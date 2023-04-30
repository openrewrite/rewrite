/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.*;

public class NoPrimitiveWrappersForToStringOrCompareTo extends Recipe {
    private static final MethodMatcher NUMBER_TO_STRING_MATCHER = new MethodMatcher("java.lang.Number toString()", true);
    private static final MethodMatcher BOOLEAN_TO_STRING_MATCHER = new MethodMatcher("java.lang.Boolean toString()", true);

    private static final MethodMatcher NUMBER_COMPARE_TO_MATCHER = new MethodMatcher("java.lang.Number compareTo(..)", true);
    private static final MethodMatcher BOOLEAN_COMPARE_TO_MATCHER = new MethodMatcher("java.lang.Boolean compareTo(..)", true);

    @Override
    public String getDisplayName() {
        return "No primitive wrappers for #toString() or #compareTo(..)";
    }

    @Override
    public String getDescription() {
        return "Primitive wrappers should not be instantiated only for `#toString()` or `#compareTo(..)` invocations.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1158");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(
                        new UsesMethod<>(NUMBER_COMPARE_TO_MATCHER),
                        new UsesMethod<>(NUMBER_TO_STRING_MATCHER),
                        new UsesMethod<>(BOOLEAN_COMPARE_TO_MATCHER),
                        new UsesMethod<>(BOOLEAN_TO_STRING_MATCHER)
                ),
                new NoPrimitiveWrapperVisitor()
        );
    }

    private static class NoPrimitiveWrapperVisitor extends JavaIsoVisitor<ExecutionContext> {

        private static final MethodMatcher VALUE_OF_NUMBER_MATCHER = new MethodMatcher("java.lang.Number valueOf(*)", true);
        private static final MethodMatcher VALUE_OF_BOOLEAN_MATCHER = new MethodMatcher("java.lang.Boolean valueOf(*)", true);

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
            JavaType.Class clazz = mi.getMethodType() != null ? TypeUtils.asClass(mi.getMethodType().getDeclaringType()) : null;
            if (clazz != null && "java.lang".equals(clazz.getPackageName())) {
                if (NUMBER_TO_STRING_MATCHER.matches(mi) || BOOLEAN_TO_STRING_MATCHER.matches(mi)) {
                    Expression arg = null;
                    if (mi.getSelect() instanceof J.NewClass) {
                        arg = getSingleArg(((J.NewClass) mi.getSelect()).getArguments());
                    } else if (mi.getSelect() instanceof J.MethodInvocation) {
                        J.MethodInvocation selectMethod = (J.MethodInvocation) mi.getSelect();
                        if (VALUE_OF_NUMBER_MATCHER.matches(selectMethod) || VALUE_OF_BOOLEAN_MATCHER.matches(selectMethod)) {
                            arg = getSingleArg(selectMethod.getArguments());
                        }
                    }
                    if (arg != null && !TypeUtils.isString(arg.getType()) && mi.getSelect() != null) {
                        JavaType.FullyQualified fq = mi.getMethodType().getDeclaringType();
                        mi = mi.withSelect(new J.Identifier(UUID.randomUUID(), mi.getSelect().getPrefix(), Markers.EMPTY, fq.getClassName(), fq, null));
                        //noinspection ArraysAsListWithZeroOrOneArgument
                        mi = mi.withArguments(Arrays.asList(arg));
                    }
                } else if (NUMBER_COMPARE_TO_MATCHER.matches(mi) || BOOLEAN_COMPARE_TO_MATCHER.matches(mi)) {
                    Expression arg = null;
                    if (mi.getSelect() instanceof J.NewClass) {
                        arg = getSingleArg(((J.NewClass) mi.getSelect()).getArguments());
                    } else if (mi.getSelect() instanceof J.MethodInvocation) {
                        J.MethodInvocation selectMethod = (J.MethodInvocation) mi.getSelect();
                        if (VALUE_OF_NUMBER_MATCHER.matches(selectMethod) || VALUE_OF_BOOLEAN_MATCHER.matches(selectMethod)) {
                            arg = getSingleArg(selectMethod.getArguments());
                        }
                    }

                    if (arg != null && !TypeUtils.isString(arg.getType()) && mi.getSelect() != null) {
                        JavaType.FullyQualified fq = mi.getMethodType().getDeclaringType();
                        mi = mi.withSelect(new J.Identifier(UUID.randomUUID(), mi.getSelect().getPrefix(), Markers.EMPTY, fq.getClassName(), fq, null));
                        mi = mi.withArguments(ListUtils.concat(arg, mi.getArguments()));
                        mi = maybeAutoFormat(mi, mi.withName(mi.getName().withSimpleName("compare")), executionContext);
                    }
                }
            }
            return mi;
        }

        @Nullable
        private Expression getSingleArg(@Nullable List<Expression> args) {
            if (args != null && args.size() == 1 && !(args.get(0) instanceof J.Empty)) {
                return args.get(0);
            }
            return null;
        }
    }
}
