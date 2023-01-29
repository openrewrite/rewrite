/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class RemoveCallsToObjectFinalize extends Recipe {

    private static final MethodMatcher OBJECT_FINALIZE = new MethodMatcher("java.lang.Object finalize()");

    @Override
    public String getDisplayName() {
        return "Remove `Object.finalize()` invocations";
    }

    @Override
    public String getDescription() {
        return "Removes calls to `Object.finalize()`. This method is called during garbage collection and calling it manually is misleading.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1111");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(OBJECT_FINALIZE);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext context) {
                J.MethodInvocation invocation = super.visitMethodInvocation(method, context);

                if (invocation.getMethodType() != null && "finalize".equals(invocation.getMethodType().getName())
                        &&(invocation.getMethodType().getDeclaringType().getSupertype() != null && Object.class.getName().equals(invocation.getMethodType().getDeclaringType().getSupertype().getFullyQualifiedName()))) {
                    return null;
                }
                return invocation;
            }
        };
    }
}
