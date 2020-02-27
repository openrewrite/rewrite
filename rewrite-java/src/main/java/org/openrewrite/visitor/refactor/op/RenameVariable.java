/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.visitor.refactor.op;

import org.openrewrite.tree.Cursor;
import org.openrewrite.tree.J;
import org.openrewrite.visitor.RetrieveCursorVisitor;
import org.openrewrite.visitor.refactor.AstTransform;
import org.openrewrite.visitor.refactor.ScopedRefactorVisitor;
import org.openrewrite.visitor.RetrieveCursorVisitor;
import org.openrewrite.visitor.refactor.AstTransform;

import java.util.List;

public class RenameVariable extends ScopedRefactorVisitor {
    private final String toName;

    private Cursor scopeCursor;
    private String scopeVariableName;

    public RenameVariable(J.VariableDecls.NamedVar scope, String toName) {
        super(scope.getId());
        this.toName = toName;
    }

    @Override
    public List<AstTransform> visitCompilationUnit(J.CompilationUnit cu) {
        scopeCursor = new RetrieveCursorVisitor(scope).visit(cu);
        scopeVariableName = ((J.VariableDecls.NamedVar) scopeCursor.getTree()).getSimpleName();
        return super.visitCompilationUnit(cu);
    }

    @Override
    public List<AstTransform> visitIdentifier(J.Ident ident) {
        return maybeTransform(ident,
                ident.getSimpleName().equals(scopeVariableName) &&
                        scopeCursor.isInSameNameScope(getCursor()),
                super::visitIdentifier,
                i -> i.withName(toName));
    }
}
