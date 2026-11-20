/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.test.internal;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/**
 * Recipes under test, declared outside any test class so that the checks that skip test scaffolding check them.
 */
final class RenameFooToBarRecipes {

    private RenameFooToBarRecipes() {
    }

    /**
     * Renames calls to {@code foo()} to {@code bar()}, keeping the call's prefix.
     */
    public static class RenamesFooToBar extends Recipe {
        @Override
        public String getDisplayName() {
            return "Rename `foo()` to `bar()`";
        }

        @Override
        public String getDescription() {
            return "Renames calls to `foo()` to `bar()`.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new JavaIsoVisitor<>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                    return "foo".equals(m.getSimpleName()) ? m.withName(m.getName().withSimpleName("bar")) : m;
                }
            };
        }
    }

    /**
     * Renames calls to {@code foo()} to {@code bar()} and formats the renamed call, which gives its line the
     * indentation of a file formatted from scratch.
     */
    public static class ReformatsWhatItRenames extends Recipe {
        @Override
        public String getDisplayName() {
            return "Rename `foo()` to `bar()` and format it";
        }

        @Override
        public String getDescription() {
            return "Renames calls to `foo()` to `bar()` and formats the renamed call.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new JavaIsoVisitor<>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                    return "foo".equals(m.getSimpleName()) ?
                      autoFormat(m.withName(m.getName().withSimpleName("bar")), ctx) : m;
                }
            };
        }
    }
}
