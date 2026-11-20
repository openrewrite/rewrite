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
package org.openrewrite.groovy.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeTree;

public class MinimumViableSpacingVisitor<P> extends org.openrewrite.java.format.MinimumViableSpacingVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    public MinimumViableSpacingVisitor(@Nullable Tree stopAfter) {
        super(stopAfter);
        this.stopAfter = stopAfter;
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
        J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, p);

        // Groovy's `def`/`var` and destructuring declarations carry an empty (name-less) type expression.
        // Since it renders nothing, the superclass' "space before the type" and "space before the first
        // variable" collapse onto the same spot and double up. Keep at most a single separating space,
        // and only when a modifier or annotation precedes it.
        TypeTree typeExpression = v.getTypeExpression();
        if (typeExpression instanceof J.Identifier && ((J.Identifier) typeExpression).getSimpleName().isEmpty()) {
            boolean needsLeadingSpace = !v.getLeadingAnnotations().isEmpty() || !v.getModifiers().isEmpty();
            v = v.withTypeExpression(typeExpression.withPrefix(
                    typeExpression.getPrefix().withWhitespace(needsLeadingSpace ? " " : "")));
            if (!v.getVariables().isEmpty()) {
                v = v.withVariables(Space.formatFirstPrefix(v.getVariables(),
                        v.getVariables().get(0).getPrefix().withWhitespace("")));
            }
        }
        return v;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, P p) {
        J.MethodInvocation m = super.visitMethodInvocation(method, p);
        return m.withArguments(ListUtils.mapFirst(m.getArguments(), first -> {
            if (first.getMarkers().findFirst(OmitParentheses.class).isPresent()) {
                return first.getPrefix().getWhitespace().isEmpty() ?
                        first.withPrefix(first.getPrefix().withWhitespace(" ")) :
                        first;
            }
            return first;
        }));
    }

    @Override
    public @Nullable J postVisit(J tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(JavaSourceFile.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Override
    public @Nullable J visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (J) tree;
        }
        return super.visit(tree, p);
    }
}
