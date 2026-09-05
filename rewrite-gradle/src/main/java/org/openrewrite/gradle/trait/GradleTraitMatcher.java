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
package org.openrewrite.gradle.trait;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;

import java.util.Optional;

public abstract class GradleTraitMatcher<U extends Trait<?>> extends SimpleTraitMatcher<U> {
    protected @Nullable GradleProject getGradleProject(Cursor cursor) {
        SourceFile sourceFile = cursor.firstEnclosing(SourceFile.class);
        if (sourceFile == null) {
            return null;
        }

        Optional<GradleProject> maybeGp = sourceFile.getMarkers().findFirst(GradleProject.class);
        return maybeGp.orElse(null);
    }

    protected boolean withinBlock(Cursor cursor, String name) {
        Cursor parentCursor = cursor.getParent();
        while (parentCursor != null) {
            if (parentCursor.getValue() instanceof J.MethodInvocation) {
                J.MethodInvocation m = parentCursor.getValue();
                if (m.getSimpleName().equals(name)) {
                    return true;
                }
            }
            parentCursor = parentCursor.getParent();
        }

        return false;
    }

    /**
     * @return {@code true} if the cursor's tree is itself a statement in an enclosing block,
     * rather than a nested expression such as the receiver of a chained method call.
     */
    protected boolean isTopLevelStatement(Cursor cursor) {
        Cursor parent = cursor.getParentTreeCursor();
        if (parent.getValue() instanceof J.Return) {
            // Groovy closures implicitly return their last expression through a synthetic Return
            parent = parent.getParentTreeCursor();
        }
        return !parent.isRoot() && parent.getValue() instanceof J.Block;
    }

    protected static J.@Nullable MethodInvocation asChainedInvocation(J.MethodInvocation m) {
        return m.getSelect() instanceof J.MethodInvocation ? (J.MethodInvocation) m.getSelect() : null;
    }

    /**
     * @return the string value of {@code m}'s argument at {@code index}, or {@code null} if
     * there's no such argument or it isn't a string literal.
     */
    protected static @Nullable String literalArgument(J.MethodInvocation m, int index) {
        if (index < m.getArguments().size()) {
            Expression argument = m.getArguments().get(index);
            if (argument instanceof J.Literal && ((J.Literal) argument).getValue() instanceof String) {
                return (String) ((J.Literal) argument).getValue();
            }
        }
        return null;
    }
}
