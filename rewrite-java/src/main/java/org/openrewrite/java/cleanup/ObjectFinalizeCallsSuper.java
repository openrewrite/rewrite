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
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ObjectFinalizeCallsSuper extends Recipe {
    private static final MethodMatcher FINALIZE_METHOD_MATCHER = new MethodMatcher("java.lang.Object finalize()", true);

    @Override
    public String getDisplayName() {
        return "Object#finalize() invokes super#finalize()";
    }

    @Override
    public String getDescription() {
        return "`Object#finalize()` methods should invoke `super.finalize()`";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1114");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                for (JavaType.Method md : cu.getDeclaredMethods()) {
                    if (FINALIZE_METHOD_MATCHER.matches(md)) {
                        return cu.withMarkers(cu.getMarkers().searchResult());
                    }
                }
                return cu;
            }
        };
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, executionContext);
                if (FINALIZE_METHOD_MATCHER.matches(md.getType()) && !hasSuperFinalizeMethodInvocation(md)) {
                    //noinspection ConstantConditions
                    md = md.withTemplate(JavaTemplate.builder(this::getCursor, "super.finalize()").build(), md.getBody().getCoordinates().lastStatement());
                }
                return md;
            }

            private boolean hasSuperFinalizeMethodInvocation(J.MethodDeclaration md) {
                AtomicBoolean hasSuperFinalize = new AtomicBoolean(Boolean.FALSE);
                new FindSuperFinalizeVisitor().visit(md, hasSuperFinalize);
                return hasSuperFinalize.get();
            }
        };
    }

    private static class FindSuperFinalizeVisitor extends JavaIsoVisitor<AtomicBoolean> {
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean exists) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, exists);
            if (FINALIZE_METHOD_MATCHER.matches(mi)) {
                exists.set(Boolean.TRUE);
            }
            return mi;
        }
    }
}
