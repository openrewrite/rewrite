/*
 * Copyright 2022 the original author or authors.
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

import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.util.Iterator;

public class UseForEachRemoveInsteadOfSetRemoveAll extends Recipe {
    @Override
    public String getDisplayName() {
        return "Replace `java.util.Set#removeAll(java.util.Collection)` with `java.util.Collection#forEach(Set::remove)`";
    }

    @Override
    public String getDescription() {
        return "Using `java.util.Collection#forEach(Set::remove)` rather than `java.util.Set#removeAll(java.util.Collection)` may improve performance due to a possible O(n^2) complexity.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher removeAll = new MethodMatcher("java.util.Set removeAll(java.util.Collection)");
        return Preconditions.check(new UsesMethod<>(removeAll), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
                if (removeAll.matches(mi) && !returnValueIsUsed()) {
                    mi = mi.withTemplate(JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "#{any(java.util.Collection)}.forEach(#{any(java.util.Set)}::remove)").build(),
                            mi.getCoordinates().replace(), mi.getArguments().get(0), mi.getSelect());
                }
                return mi;
            }

            private boolean returnValueIsUsed() {
                Iterator<Cursor> cIterator = getCursor().getPathAsCursors();
                while (cIterator.hasNext()) {
                    Cursor p = cIterator.next();
                    if (p.getValue() instanceof J.ClassDeclaration
                        || p.getValue() instanceof J.Block
                        || p.getValue() instanceof J.Lambda) {
                        return false;
                    } else if (p.getValue() instanceof J.ControlParentheses
                               || p.getValue() instanceof J.Return
                               || p.getValue() instanceof J.VariableDeclarations
                               || p.getValue() instanceof J.Assignment) {
                        return true;
                    }
                }
                return true;
            }
        });
    }
}
