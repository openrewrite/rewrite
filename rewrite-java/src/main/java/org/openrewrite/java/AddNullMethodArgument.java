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
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.EMPTY;

/**
 * This recipe finds method invocations matching a method pattern and uses a zero-based argument index to determine
 * which argument is added with a null value.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class AddNullMethodArgument extends Recipe {

    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method invocations.",
            example = "com.yourorg.A foo(int, int)")
    String methodPattern;

    /**
     * A zero-based index that indicates which argument will be added as null to the method invocation.
     */
    @Option(displayName = "Argument index",
            description = "A zero-based index that indicates which argument will be added as null to the method invocation.",
            example = "0")
    int argumentIndex;

    @Option(displayName = "Parameter type",
            description = "The type of the parameter that we add the argument for.",
            example = "java.lang.String")
    String parameterType;

    @Option(displayName = "Parameter name",
            description = "The name of the parameter that we add the argument for.",
            required = false,
            example = "name")
    @Nullable String parameterName;

    @Option(displayName = "Explicit cast",
            description = "Explicitly cast the argument to the parameter type. Useful if the method is overridden with another type.",
            required = false,
            example = "true")
    @Nullable Boolean explicitCast;

    @Override
    public String getInstanceNameSuffix() {
        return String.format("%d in methods `%s`", argumentIndex, methodPattern);
    }

    @Override
    public String getDisplayName() {
        return "Add a `null` method argument";
    }

    @Override
    public String getDescription() {
        return "Add a `null` argument to method invocations.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(methodPattern), new AddNullMethodArgumentVisitor(new MethodMatcher(methodPattern)));
    }

    private class AddNullMethodArgumentVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher;

        public AddNullMethodArgumentVisitor(MethodMatcher methodMatcher) {
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

                Expression nullLiteral = new J.Literal(randomId(), args.isEmpty() ? Space.EMPTY : Space.SINGLE_SPACE, Markers.EMPTY, "null", "null", null, JavaType.Primitive.Null);
                if (explicitCast == Boolean.TRUE) {
                    nullLiteral = new J.TypeCast(randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                            new J.ControlParentheses<>(randomId(), EMPTY, Markers.EMPTY,
                                    new JRightPadded<>(TypeTree.build(parameterType), EMPTY, Markers.EMPTY)),
                            nullLiteral);
                }
                m = m.withArguments(ListUtils.insert(args, nullLiteral, argumentIndex));

                JavaType.Method methodType = m.getMethodType();
                if (methodType != null) {
                    m = m.withMethodType(methodType
                            .withParameterNames(ListUtils.insert(methodType.getParameterNames(),
                                    parameterName == null ? "arg" + argumentIndex : parameterName, argumentIndex))
                            .withParameterTypes(ListUtils.insert(methodType.getParameterTypes(),
                                    JavaType.buildType(parameterType), argumentIndex)));
                    if (m instanceof J.MethodInvocation && ((J.MethodInvocation) m).getName().getType() != null) {
                        m = ((J.MethodInvocation) m).withName(((J.MethodInvocation) m).getName().withType(m.getMethodType()));
                    }
                }
            }
            return m;
        }

    }
}
