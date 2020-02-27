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

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.tree.*;
import org.openrewrite.tree.*;
import org.openrewrite.visitor.CursorAstVisitor;
import org.openrewrite.visitor.refactor.AstTransform;
import org.openrewrite.visitor.refactor.RefactorVisitor;
import lombok.RequiredArgsConstructor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.visitor.CursorAstVisitor;
import org.openrewrite.visitor.refactor.AstTransform;
import org.slf4j.helpers.MessageFormatter;

import java.util.List;

@RequiredArgsConstructor
public class ChangeFieldName extends RefactorVisitor {
    private final Type.Class classType;
    private final String hasName;
    private final String toName;

    @Override
    public String getRuleName() {
        return MessageFormatter.arrayFormat("core.ChangeFieldName{classType={},whenName={},toName={}}",
                new String[]{classType.getFullyQualifiedName(), hasName, toName}).toString();
    }

    @Override
    public List<AstTransform> visitVariable(J.VariableDecls.NamedVar variable) {
        boolean isField = getCursor()
                .getParentOrThrow() // J.VariableDecls
                .getParentOrThrow() // J.Block
                .getParentOrThrow() // maybe J.ClassDecl
                .getTree() instanceof J.ClassDecl;

        return maybeTransform(variable,
                isField &&
                        matchesClass(getCursor().enclosingClass().getType()) &&
                        variable.getSimpleName().equals(hasName),
                super::visitVariable,
                v -> v.withName(v.getName().withName(toName))
        );
    }

    @Override
    public List<AstTransform> visitFieldAccess(J.FieldAccess fieldAccess) {
        return maybeTransform(fieldAccess,
                matchesClass(fieldAccess.getTarget().getType()) &&
                        fieldAccess.getSimpleName().equals(hasName),
                super::visitFieldAccess,
                fa -> fa.withName(fa.getName().withName(toName))
        );
    }

    @Override
    public List<AstTransform> visitIdentifier(J.Ident ident) {
        return maybeTransform(ident,
                ident.getSimpleName().equals(hasName) && isFieldReference(ident),
                super::visitIdentifier,
                i -> i.withName(toName)
        );
    }

    private boolean matchesClass(@Nullable Type test) {
        Type.Class testClassType = TypeUtils.asClass(test);
        return testClassType != null && testClassType.getFullyQualifiedName().equals(classType.getFullyQualifiedName());
    }

    private boolean isFieldReference(J.Ident ident) {
        Cursor nearest = new FindVariableDefinition(ident, getCursor()).visit(getCursor().enclosingCompilationUnit());
        return nearest != null && nearest
                .getParentOrThrow() // maybe J.VariableDecls
                .getParentOrThrow() // maybe J.Block
                .getParentOrThrow() // maybe J.ClassDecl
                .getTree() instanceof J.ClassDecl;
    }

    @RequiredArgsConstructor
    private static class FindVariableDefinition extends CursorAstVisitor<Cursor> {
        private final J.Ident ident;
        private final Cursor referenceScope;

        @Override
        public Cursor defaultTo(Tree t) {
            return null;
        }

        @Override
        public Cursor visitVariable(J.VariableDecls.NamedVar variable) {
            return variable.getSimpleName().equalsIgnoreCase(ident.getSimpleName()) && getCursor().isInSameNameScope(referenceScope) ?
                    getCursor() :
                    super.visitVariable(variable);
        }

        @Override
        public Cursor reduce(Cursor r1, Cursor r2) {
            if (r1 == null) {
                return r2;
            }

            if (r2 == null) {
                return r1;
            }

            // the definition will be the "closest" cursor, i.e. the one with the longest path in the same name scope
            return r1.getPathAsStream().count() > r2.getPathAsStream().count() ? r1 : r2;
        }
    }
}
