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
import org.openrewrite.Incubating;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

@Incubating(since = "7.10.0")
public class IndexOfReplaceableByContains extends Recipe {
    private static final MethodMatcher STRING_INDEX_MATCHER = new MethodMatcher("java.lang.String indexOf(String)");
    private static final MethodMatcher LIST_INDEX_MATCHER = new MethodMatcher("java.util.List indexOf(Object)");

    @Override
    public String getDisplayName() {
        return "`indexOf()` replaceable by `contains()`";
    }

    @Override
    public String getDescription() {
        return "Checking if a value is included in a `String` or `List` using `indexOf(value)>-1` or `indexOf(value)>=0` can be replaced with `contains(value)`.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new IndexOfReplaceableByContainsVisitor();
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                doAfterVisit(new UsesMethod<>(STRING_INDEX_MATCHER));
                doAfterVisit(new UsesMethod<>(LIST_INDEX_MATCHER));
                return cu;
            }
        };
    }

    private static class IndexOfReplaceableByContainsVisitor extends JavaVisitor<ExecutionContext> {
        private final JavaTemplate stringContains = JavaTemplate.builder(this::getCursor, "#{any(java.lang.String)}.contains(#{any(java.lang.String)})").build();
        private final JavaTemplate listContains = JavaTemplate.builder(this::getCursor, "#{any(java.util.List)}.contains(#{any(java.lang.Object)})").build();

        @Override
        public J visitBinary(J.Binary binary, ExecutionContext ctx) {
            J j = super.visitBinary(binary, ctx);
            J.Binary asBinary = (J.Binary) j;
            if (asBinary.getLeft() instanceof J.MethodInvocation) {
                J.MethodInvocation mi = ((J.MethodInvocation) asBinary.getLeft());
                if (STRING_INDEX_MATCHER.matches(mi) || LIST_INDEX_MATCHER.matches(mi)) {
                    if (asBinary.getRight() instanceof J.Literal) {
                        String valueSource = ((J.Literal) asBinary.getRight()).getValueSource();
                        boolean isGreaterThanNegativeOne = asBinary.getOperator() == J.Binary.Type.GreaterThan && "-1".equals(valueSource);
                        boolean isGreaterThanOrEqualToZero = asBinary.getOperator() == J.Binary.Type.GreaterThanOrEqual && "0".equals(valueSource);
                        if (isGreaterThanNegativeOne || isGreaterThanOrEqualToZero) {
                            j = mi.withTemplate(STRING_INDEX_MATCHER.matches(mi) ? stringContains : listContains,
                                    mi.getCoordinates().replace(), mi.getSelect(), mi.getArguments().get(0)).withPrefix(asBinary.getPrefix());
                        }
                    }
                }
            }
            return j;
        }
    }

}
