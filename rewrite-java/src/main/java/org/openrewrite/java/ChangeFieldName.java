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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.concurrent.atomic.AtomicReference;

public class ChangeFieldName<P> extends JavaIsoVisitor<P> {
    private final JavaType.Class classType;
    private final String hasName;
    private final String toName;

    public ChangeFieldName(JavaType.Class classType, String hasName, String toName) {
        this.classType = classType;
        this.hasName = hasName;
        this.toName = toName;
    }

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
        J.VariableDeclarations.NamedVariable v = variable;
        J.ClassDeclaration enclosingClass = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
        if (variable.isField(getCursor()) && matchesClass(enclosingClass.getType()) &&
                variable.getSimpleName().equals(hasName)) {
            v = v.withName(v.getName().withName(toName));
        }
        if (variable.getPadding().getInitializer() != null) {
            v = v.getPadding().withInitializer(visitLeftPadded(variable.getPadding().getInitializer(),
                    JLeftPadded.Location.VARIABLE_INITIALIZER, p));
        }
        return v;
    }

    @Override
    public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, P p) {
        J.FieldAccess f = super.visitFieldAccess(fieldAccess, p);
        if (matchesClass(fieldAccess.getTarget().getType()) &&
                fieldAccess.getSimpleName().equals(hasName)) {
            f = f.getPadding().withName(f.getPadding().getName().withElement(f.getPadding().getName().getElement().withName(toName)));
        }
        return f;
    }

    @Override
    public J.Identifier visitIdentifier(J.Identifier ident, P p) {
        J.Identifier i = super.visitIdentifier(ident, p);
        if (ident.getSimpleName().equals(hasName) && isFieldReference(ident) &&
                isThisReferenceToClassType()) {
            i = i.withName(toName);
        }
        return i;
    }

    private boolean matchesClass(@Nullable JavaType test) {
        JavaType.Class testClassType = TypeUtils.asClass(test);
        return testClassType != null && testClassType.getFullyQualifiedName().equals(classType.getFullyQualifiedName());
    }

    private boolean isThisReferenceToClassType() {
        J.FieldAccess fieldAccess = getCursor().firstEnclosing(J.FieldAccess.class);
        if (fieldAccess == null) {
            return true;
        }
        while(fieldAccess.getType() == null && fieldAccess.getTarget() instanceof J.FieldAccess) {
            fieldAccess = (J.FieldAccess) fieldAccess.getTarget();
        }
        return classType.equals(fieldAccess.getTarget().getType());
    }

    private boolean isFieldReference(J.Identifier ident) {
        AtomicReference<Cursor> nearest = new AtomicReference<>();
        new FindVariableDefinition(ident, getCursor()).visit(getCursor().firstEnclosing(J.CompilationUnit.class), nearest);
        return nearest.get() != null && nearest.get()
                .dropParentUntil(J.class::isInstance) // maybe J.VariableDecls
                .dropParentUntil(J.class::isInstance) // maybe J.Block
                .dropParentUntil(J.class::isInstance) // maybe J.ClassDecl
                .getValue() instanceof J.ClassDeclaration;
    }

    private static class FindVariableDefinition extends JavaIsoVisitor<AtomicReference<Cursor>> {
        private final J.Identifier ident;
        private final Cursor referenceScope;

        public FindVariableDefinition(J.Identifier ident, Cursor referenceScope) {
            this.ident = ident;
            this.referenceScope = referenceScope;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, AtomicReference<Cursor> ctx) {
            if (variable.getSimpleName().equalsIgnoreCase(ident.getSimpleName()) && isInSameNameScope(referenceScope)) {
                // the definition will be the "closest" cursor, i.e. the one with the longest path in the same name scope
                ctx.accumulateAndGet(getCursor(), (r1, r2) -> {
                    if (r1 == null) {
                        return r2;
                    }
                    return r1.getPathAsStream().count() > r2.getPathAsStream().count() ? r1 : r2;
                });
            }
            return super.visitVariable(variable, ctx);
        }
    }
}
