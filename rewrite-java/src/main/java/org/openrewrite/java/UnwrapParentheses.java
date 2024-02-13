/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java;

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

public class UnwrapParentheses<P> extends JavaVisitor<P> {
    private final J.Parentheses<?> scope;

    public UnwrapParentheses(J.Parentheses<?> scope) {
        this.scope = scope;
    }

    @Override
    public <T extends J> J visitParentheses(J.Parentheses<T> parens, P p) {
        if (scope.isScope(parens) && isUnwrappable(getCursor())) {
            J tree = parens.getTree().withPrefix(parens.getPrefix());
            if (tree.getPrefix().isEmpty()) {
                Object parent = getCursor().getParentOrThrow().getValue();
                if (parent instanceof J.Return || parent instanceof J.Throw) {
                    tree = tree.withPrefix(Space.SINGLE_SPACE);
                }
            }
            return tree;
        }
        return super.visitParentheses(parens, p);
    }

    public static boolean isUnwrappable(Cursor parensScope) {
        if (!(parensScope.getValue() instanceof J.Parentheses)) {
            return false;
        }
        Tree parent = parensScope.getParentTreeCursor().getValue();
        if (parent instanceof J.If ||
            parent instanceof J.Switch ||
            parent instanceof J.Synchronized ||
            parent instanceof J.Try.Catch ||
            parent instanceof J.TypeCast ||
            parent instanceof J.WhileLoop) {
            return false;
        } else if (parent instanceof J.DoWhileLoop) {
            return !(parensScope.getValue() == ((J.DoWhileLoop) parent).getWhileCondition());
        } else if (parent instanceof J.Unary) {
            J innerJ = ((J.Parentheses<?>) parensScope.getValue()).getTree();
            return !(innerJ instanceof J.Assignment) &&
                   !(innerJ instanceof J.Binary) &&
                   !(innerJ instanceof J.Ternary) &&
                   !(innerJ instanceof J.InstanceOf);
        }
        return true;
    }
}

