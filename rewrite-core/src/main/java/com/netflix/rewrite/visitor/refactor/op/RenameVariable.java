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
package com.netflix.rewrite.visitor.refactor.op;

import com.netflix.rewrite.tree.Cursor;
import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.visitor.RetrieveCursorVisitor;
import com.netflix.rewrite.visitor.refactor.AstTransform;
import com.netflix.rewrite.visitor.refactor.ScopedRefactorVisitor;

import java.util.List;

public class RenameVariable extends ScopedRefactorVisitor {
    private final String toName;

    private Cursor scopeCursor;
    private String scopeVariableName;

    public RenameVariable(Tr.VariableDecls.NamedVar scope, String toName) {
        super(scope.getId());
        this.toName = toName;
    }

    @Override
    public List<AstTransform> visitCompilationUnit(Tr.CompilationUnit cu) {
        scopeCursor = new RetrieveCursorVisitor(scope).visit(cu);
        scopeVariableName = ((Tr.VariableDecls.NamedVar) scopeCursor.getTree()).getSimpleName();
        return super.visitCompilationUnit(cu);
    }

    @Override
    public List<AstTransform> visitIdentifier(Tr.Ident ident) {
        return maybeTransform(ident,
                ident.getSimpleName().equals(scopeVariableName) &&
                        scopeCursor.isInSameNameScope(getCursor()),
                super::visitIdentifier,
                i -> i.withName(toName));
    }
}
