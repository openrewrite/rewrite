/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.JavaType.Primitive;

/**
 * This recipe finds method invocations matching a method pattern and uses a zero-based argument index to determine
 * which argument is added with a literal value.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class AddLiteralMethodArgument extends Recipe {

    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern",
            description = MethodMatcher.METHOD_PATTERN_INVOCATIONS_DESCRIPTION,
            example = "com.yourorg.A foo(int, int)")
    String methodPattern;

    /**
     * A zero-based index indicating where to insert the new argument.
     */
    @Option(displayName = "Argument index",
            description = "A zero-based index indicating where to insert the new argument.",
            example = "0")
    int argumentIndex;

    @Option(displayName = "Literal",
            description = "The literal value to add as an argument.",
            example = "abc")
    Object literal;

    @Option(displayName = "Parameter type",
            description = "The parameter type for the added argument. Defaults to `String`.",
            required = false,
            example = "String",
            valid = {"String", "int", "short", "long", "float", "double", "boolean", "char"})
    @Nullable
    String primitiveType;

    @Override
    public String getInstanceNameSuffix() {
        return String.format("%d in methods `%s`", argumentIndex, methodPattern);
    }

    String displayName = "Add a literal method argument";

    String description = "Add a literal `String` or primitive argument to method invocations.";

    @Override
    public Validated<Object> validate() {
        return super.validate().and(MethodMatcher.validate(methodPattern));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(methodPattern), new AddLiteralMethodArgumentVisitor(new MethodMatcher(methodPattern)));
    }

    private class AddLiteralMethodArgumentVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher;

        public AddLiteralMethodArgumentVisitor(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            return (J.MethodInvocation) visitMethodCall(m);
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            J.NewClass n = super.visitNewClass(newClass, ctx);
            return (J.NewClass) visitMethodCall(n);
        }

        private MethodCall visitMethodCall(MethodCall methodCall) {
            MethodCall m = methodCall;
            List<Expression> originalArgs = m.getArguments();
            if (methodMatcher.matches(m) && (long) originalArgs.size() >= argumentIndex) {
                List<Expression> args = new ArrayList<>(originalArgs);

                if (args.size() == 1 && args.get(0) instanceof J.Empty) {
                    args.remove(0);
                }
                JavaType.Primitive primitive;
                String valueSource;
                Object value;
                JavaType.Primitive literalType;
                if (StringUtils.isBlank(primitiveType) || "string".equalsIgnoreCase(primitiveType)) {
                    primitive = Primitive.String;
                    valueSource = String.format("\"%s\"", getLiteral());
                    value = String.valueOf(getLiteral());
                    literalType = primitive;
                } else if("char".equalsIgnoreCase(primitiveType)){
                    primitive = Primitive.Char;
                    valueSource = String.format("'%s'", getLiteral());
                    String c = String.valueOf(getLiteral());
                    value = c.length() == 1 ? (Object) c.charAt(0) : getLiteral();
                    literalType = primitive;
                }else {
                    primitive = Primitive.fromKeyword(primitiveType.toLowerCase());
                    valueSource = String.valueOf(getLiteral());
                    // the parser gives the literal the value and type its text has, whatever the parameter's type
                    Object[] parsed = parseLiteral(valueSource);
                    value = parsed == null ? getLiteral() : parsed[0];
                    literalType = parsed == null ? primitive : (JavaType.Primitive) parsed[1];
                }

                Expression literal = new J.Literal(randomId(), args.isEmpty() ? Space.EMPTY : Space.SINGLE_SPACE, Markers.EMPTY, value, valueSource, null, literalType);
                m = m.withArguments(ListUtils.insert(args, literal, argumentIndex));

                JavaType.Method methodType = m.getMethodType();
                if (methodType != null) {
                    m = m.withMethodType(methodType
                            .withParameterNames(ListUtils.insert(methodType.getParameterNames(), "arg" + argumentIndex, argumentIndex))
                            .withParameterTypes(ListUtils.insert(methodType.getParameterTypes(), primitive, argumentIndex)));
                    if (m instanceof J.MethodInvocation && ((J.MethodInvocation) m).getName().getType() != null) {
                        m = ((J.MethodInvocation) m).withName(((J.MethodInvocation) m).getName().withType(m.getMethodType()));
                    }
                }
            }
            return m;
        }

        /**
         * The value and type of a boolean or numeric literal written as {@code source}, or null if it isn't one.
         */
        private Object @Nullable [] parseLiteral(String source) {
            String s = source.replace("_", "");
            if ("true".equals(s) || "false".equals(s)) {
                return new Object[]{Boolean.parseBoolean(s), Primitive.Boolean};
            }
            if (s.isEmpty()) {
                return null;
            }
            char last = Character.toLowerCase(s.charAt(s.length() - 1));
            boolean hex = s.startsWith("0x") || s.startsWith("0X") || s.startsWith("-0x") || s.startsWith("-0X");
            try {
                if (last == 'l') {
                    return new Object[]{Long.decode(s.substring(0, s.length() - 1)), Primitive.Long};
                } else if (!hex && last == 'f') {
                    return new Object[]{Float.parseFloat(s), Primitive.Float};
                } else if (!hex && (last == 'd' || s.contains(".") || s.contains("e") || s.contains("E"))) {
                    return new Object[]{Double.parseDouble(s), Primitive.Double};
                }
                return new Object[]{Integer.decode(s), Primitive.Int};
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
