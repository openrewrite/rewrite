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
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class NoToStringOnStringType extends Recipe {
    private static final MethodMatcher TO_STRING = new MethodMatcher("java.lang.String toString()");

    @Override
    public String getDisplayName() {
        return "No #toString() on String";
    }

    @Override
    public String getDescription() {
        return "Removes`#toString()` from Strings.";
    }

    @Nullable
    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext executionContext) {
                doAfterVisit(new UsesMethod<>("java.lang.String toString()"));
                return cu;
            }
        };
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1858");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
                if (TO_STRING.matches(mi)) {
                    return mi.withTemplate(JavaTemplate.builder(this::getCursor, "#{any(java.lang.String)}").build(),
                            mi.getCoordinates().replace(),
                            mi.getSelect());
                }
                return mi;
            }
        };
    }
}
