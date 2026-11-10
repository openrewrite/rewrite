/*
 * Copyright 2020 the original author or authors.
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

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.search.DeclaresMethod;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * This recipe finds method invocations matching the given method pattern and reorders the arguments based on the ordered
 * array of parameter names.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class ReorderMethodArguments extends Recipe {

    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern",
            description = MethodMatcher.METHOD_PATTERN_DESCRIPTION,
            example = "com.yourorg.A foo(String, Integer, Integer)")
    String methodPattern;

    /**
     * An array of parameter names that indicates the new order in which those arguments should be arranged.
     */
    @Option(displayName = "New parameter names",
            description = "An array of parameter names that indicates the new order in which those arguments should be arranged.",
            example = "[foo, bar, baz]")
    String[] newParameterNames;

    /**
     * If the original method signature is not type-attributed, this is an optional list that indicates the original order
     * in which the arguments were arranged.
     */
    @Option(displayName = "Old parameter names",
            description = "If the original method signature is not type-attributed, this is an optional list that indicates the original order in which the arguments were arranged.",
            required = false,
            example = "[baz, bar, foo]")
    String @Nullable [] oldParameterNames;

    @Option(displayName = "Ignore type definition",
            description = "When set to `true` the definition of the old type will be left untouched. " +
                    "This is useful when you're replacing usage of a class but don't want to rename it.",
            required = false)
    @Nullable
    Boolean ignoreDefinition;

    @Option(displayName = "Match on overrides",
            description = "When enabled, find methods that are overrides of the method pattern.",
            required = false)
    @Nullable
    Boolean matchOverrides;

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
    }

    String displayName = "Reorder method arguments";

    String description = "Reorder method arguments into the specified order.";

    @Override
    public Validated<Object> validate() {
        return super.validate().and(MethodMatcher.validate(methodPattern));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new JavaVisitor<ExecutionContext>() {
            @Override
            public J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                    if (Boolean.TRUE.equals(ignoreDefinition)) {
                        J j = new DeclaresMethod<>(methodPattern, true).visitNonNull(cu, ctx);
                        if (cu != j) {
                            return cu;
                        }
                    }
                    return new UsesMethod<>(methodPattern).visitNonNull(cu, ctx);
                }
                return super.visit(tree, ctx);
            }
        }, new ReorderMethodArgumentsVisitor(new MethodMatcher(methodPattern, matchOverrides)));
    }

    private class ReorderMethodArgumentsVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher;

        private ReorderMethodArgumentsVisitor(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            return (J.MethodInvocation) visitMethodCall(super.visitMethodInvocation(method, ctx));
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            return (J.NewClass) visitMethodCall(super.visitNewClass(newClass, ctx));
        }

        private MethodCall visitMethodCall(MethodCall m) {
            if (methodMatcher.matches(m) && m.getMethodType() != null) {
                @SuppressWarnings("ConstantConditions") List<String> paramNames =
                        oldParameterNames == null || oldParameterNames.length == 0 ?
                                m.getMethodType().getParameterNames() :
                                asList(oldParameterNames);

                List<JRightPadded<Expression>> originalArgs = getPaddedArguments(m);

                // the reordered call invokes a method that declares these parameters in the new order
                List<JavaType> paramTypes = m.getMethodType().getParameterTypes();
                int resolvedParamCount = paramTypes.size();

                int i = 0;
                List<JRightPadded<Expression>> reordered = new ArrayList<>(originalArgs.size());
                List<String> reorderedNames = new ArrayList<>(originalArgs.size());
                List<JavaType> reorderedTypes = new ArrayList<>(originalArgs.size());
                List<Space> formattings = new ArrayList<>(originalArgs.size());
                List<Space> rightFormattings = new ArrayList<>(originalArgs.size());

                // a single argument in the varargs position is a varargs element unless it is the array itself
                boolean varargsElements = originalArgs.size() > resolvedParamCount ||
                        resolvedParamCount > 0 && originalArgs.size() == resolvedParamCount &&
                        m.getMethodType().hasFlags(Flag.Varargs) &&
                        !(originalArgs.get(resolvedParamCount - 1).getElement().getType() instanceof JavaType.Array);

                for (String name : newParameterNames) {
                    int fromPos = paramNames.indexOf(name);
                    if (varargsElements && fromPos == resolvedParamCount - 1) {
                        // this is a varargs argument
                        List<JRightPadded<Expression>> varargs = originalArgs.subList(fromPos, originalArgs.size());
                        reordered.addAll(varargs);
                        if (name.equals(newParameterNames[newParameterNames.length - 1])) {
                            // still last, so the reordered method takes them as varargs too
                            reorderedNames.add(name);
                            reorderedTypes.add(paramTypes.get(fromPos));
                        } else {
                            JavaType.Array varargsType = TypeUtils.asArray(paramTypes.get(fromPos));
                            for (int j = 0; j < varargs.size(); j++) {
                                reorderedNames.add(name + j);
                                reorderedTypes.add(varargsType == null ? varargs.get(j).getElement().getType() :
                                        varargsType.getElemType());
                            }
                        }
                        for (JRightPadded<Expression> exp : originalArgs.subList(i, (i++) + varargs.size())) {
                            formattings.add(exp.getElement().getPrefix());
                            rightFormattings.add(exp.getAfter());
                        }
                    } else if (fromPos >= 0 && originalArgs.size() > fromPos) {
                        JRightPadded<Expression> originalArg = originalArgs.get(fromPos);
                        reordered.add(originalArg);
                        reorderedNames.add(name);
                        reorderedTypes.add(fromPos < resolvedParamCount ? paramTypes.get(fromPos) :
                                originalArg.getElement().getType());
                        formattings.add(originalArgs.get(i).getElement().getPrefix());
                        rightFormattings.add(originalArgs.get(i++).getAfter());
                    }
                }

                boolean changed = false;
                i = 0;
                for (JRightPadded<Expression> expression : reordered) {
                    final int index = i;
                    reordered.set(i, expression
                            .map(e -> e.withPrefix(formattings.get(index)))
                            .withAfter(rightFormattings.get(index)));
                    if (reordered.get(i) != originalArgs.get(i)) {
                        changed = true;
                    }
                    i++;
                }

                if (changed) {
                    JavaType.Method mt = m.getMethodType()
                            .withParameterNames(reorderedNames)
                            .withParameterTypes(reorderedTypes);
                    if (mt.hasFlags(Flag.Varargs) &&
                        paramNames.indexOf(newParameterNames[newParameterNames.length - 1]) != resolvedParamCount - 1) {
                        // the varargs parameter moves off the last position
                        Set<Flag> flags = EnumSet.copyOf(mt.getFlags());
                        flags.remove(Flag.Varargs);
                        mt = mt.withFlags(flags);
                    }
                    m = withPaddedArguments(m, reordered)
                            .withMethodType(mt);
                    if (m instanceof J.MethodInvocation && ((J.MethodInvocation) m).getName().getType() != null) {
                        m = ((J.MethodInvocation) m).withName(((J.MethodInvocation) m).getName().withType(mt));
                    }
                }
            }
            return m;
        }

        private List<JRightPadded<Expression>> getPaddedArguments(MethodCall m) {
            if (m instanceof J.MethodInvocation) {
                return ((J.MethodInvocation) m).getPadding().getArguments().getPadding().getElements();
            } else if (m instanceof J.NewClass) {
                return ((J.NewClass) m).getPadding().getArguments().getPadding().getElements();
            } else {
                throw new IllegalArgumentException("Unknown MethodCall type");
            }
        }

        private MethodCall withPaddedArguments(MethodCall m, List<JRightPadded<Expression>> reordered) {
            if (m instanceof J.MethodInvocation) {
                J.MethodInvocation mi = (J.MethodInvocation) m;
                return mi.getPadding().withArguments(
                        mi.getPadding().getArguments().getPadding().withElements(reordered)
                );
            } else if (m instanceof J.NewClass) {
                J.NewClass nc = (J.NewClass) m;
                return nc.getPadding().withArguments(
                        nc.getPadding().getArguments().getPadding().withElements(reordered)
                );
            } else {
                throw new IllegalArgumentException("Unknown MethodCall type");
            }
        }
    }
}
