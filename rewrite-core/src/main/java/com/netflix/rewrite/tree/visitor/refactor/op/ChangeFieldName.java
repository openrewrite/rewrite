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
package com.netflix.rewrite.tree.visitor.refactor.op;

import com.netflix.rewrite.internal.lang.Nullable;
import com.netflix.rewrite.tree.*;
import com.netflix.rewrite.tree.visitor.CursorAstVisitor;
import com.netflix.rewrite.tree.visitor.refactor.AstTransform;
import com.netflix.rewrite.tree.visitor.refactor.RefactorVisitor;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ChangeFieldName extends RefactorVisitor {
    private final Type.Class classType;
    private final String hasName;
    private final String toName;

    @Override
    public String getRuleName() {
        return "change-field-name";
    }

    @Override
    public List<AstTransform> visitVariable(Tr.VariableDecls.NamedVar variable) {
        boolean isField = getCursor()
                .getParentOrThrow() // Tr.VariableDecls
                .getParentOrThrow() // Tr.Block
                .getParentOrThrow() // maybe Tr.ClassDecl
                .getTree() instanceof Tr.ClassDecl;

        return maybeTransform(isField &&
                        matchesClass(getCursor().enclosingClass().getType()) &&
                        variable.getSimpleName().equals(hasName),
                super.visitVariable(variable),
                transform(variable, v -> v.withName(v.getName().withName(toName)))
        );
    }

    @Override
    public List<AstTransform> visitFieldAccess(Tr.FieldAccess fieldAccess) {
        return maybeTransform(matchesClass(fieldAccess.getTarget().getType()) &&
                        fieldAccess.getSimpleName().equals(hasName),
                super.visitFieldAccess(fieldAccess),
                transform(fieldAccess, fa -> fa.withName(fa.getName().withName(toName)))
        );
    }

    @Override
    public List<AstTransform> visitIdentifier(Tr.Ident ident) {
        return maybeTransform(ident.getSimpleName().equals(hasName) && isFieldReference(ident),
                super.visitIdentifier(ident),
                transform(ident, i -> i.withName(toName))
        );
    }

    private boolean matchesClass(@Nullable Type test) {
        Type.Class testClassType = TypeUtils.asClass(test);
        return testClassType != null && testClassType.getFullyQualifiedName().equals(classType.getFullyQualifiedName());
    }

    private boolean isFieldReference(Tr.Ident ident) {
        Cursor nearest = new FindVariableDefinition(ident, getCursor()).visit(getCursor().enclosingCompilationUnit());
        return nearest != null && nearest
                .getParentOrThrow() // maybe Tr.VariableDecls
                .getParentOrThrow() // maybe Tr.Block
                .getParentOrThrow() // maybe Tr.ClassDecl
                .getTree() instanceof Tr.ClassDecl;
    }

    @RequiredArgsConstructor
    private static class FindVariableDefinition extends CursorAstVisitor<Cursor> {
        private final Tr.Ident ident;
        private final Cursor referenceScope;

        @Override
        public Cursor defaultTo(Tree t) {
            return null;
        }

        @Override
        public Cursor visitVariable(Tr.VariableDecls.NamedVar variable) {
            return variable.getSimpleName().equalsIgnoreCase(ident.getSimpleName()) && getCursor().isInSameNameScope(referenceScope) ?
                    getCursor() :
                    super.visitVariable(variable);
        }

        @Override
        public Cursor reduce(Cursor r1, Cursor r2) {
            if(r1 == null) {
                return r2;
            }

            if(r2 == null) {
                return r1;
            }

            // the definition will be the "closest" cursor, i.e. the one with the longest path in the same name scope
            return r1.getPathAsStream().count() > r2.getPathAsStream().count() ? r1 : r2;
        }
    }
}
