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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

public class NoFinalizer extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove `finalize()` method declarations.";
    }

    @Override
    public String getDescription() {
        return "Finalizers are problematic, and their use can lead to performance issues, deadlocks, hangs, and other unnecessary behavior.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new NoFinalizerVisitor();
    }

    private static class NoFinalizerVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final MethodMatcher FINALIZER = new MethodMatcher("java.lang.Object finalize()");

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            if (FINALIZER.matches(method, getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class))) {
                return null;
            }
            return super.visitMethodDeclaration(method, ctx);
        }
    }

}
