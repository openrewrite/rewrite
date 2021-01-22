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
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.concurrent.atomic.AtomicReference;

public class ChangeFieldName<P> extends JavaIsoProcessor<P> {
    private final JavaType.Class classType;
    private final String hasName;
    private final String toName;

    public ChangeFieldName(JavaType.Class classType, String hasName, String toName) {
        this.classType = classType;
        this.hasName = hasName;
        this.toName = toName;
        setCursoringOn();
    }

    @Override
    public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable, P p) {
        J.VariableDecls.NamedVar v = super.visitVariable(variable, p);
        J.ClassDecl enclosingClass = getCursor().firstEnclosingOrThrow(J.ClassDecl.class);
        if (variable.isField(getCursor()) && matchesClass(enclosingClass.getType()) &&
                variable.getSimpleName().equals(hasName)) {
            v = v.withName(v.getName().withName(toName));
        }
        return v;
    }

    @Override
    public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, P p) {
        J.FieldAccess f = super.visitFieldAccess(fieldAccess, p);
        if (matchesClass(fieldAccess.getTarget().getType()) &&
                fieldAccess.getSimpleName().equals(hasName)) {
            f = f.withName(f.getName().withElem(f.getName().getElem().withName(toName)));
        }
        return f;
    }

    @Override
    public J.Ident visitIdentifier(J.Ident ident, P p) {
        J.Ident i = super.visitIdentifier(ident, p);
        if (ident.getSimpleName().equals(hasName) && isFieldReference(ident)) {
            i = i.withName(toName);
        }
        return i;
    }

    private boolean matchesClass(@Nullable JavaType test) {
        JavaType.Class testClassType = TypeUtils.asClass(test);
        return testClassType != null && testClassType.getFullyQualifiedName().equals(classType.getFullyQualifiedName());
    }

    private boolean isFieldReference(J.Ident ident) {
        AtomicReference<Cursor> nearest = new AtomicReference<>();
        new FindVariableDefinition(ident, getCursor()).visit(getCursor().firstEnclosing(J.CompilationUnit.class), nearest);
        return nearest.get() != null && nearest.get()
                .getParentOrThrow() // maybe J.VariableDecls
                .getParentOrThrow() // maybe J.Block
                .getParentOrThrow() // maybe J.ClassDecl
                .getValue() instanceof J.ClassDecl;
    }

    private static class FindVariableDefinition extends JavaIsoProcessor<AtomicReference<Cursor>> {
        private final J.Ident ident;
        private final Cursor referenceScope;

        public FindVariableDefinition(J.Ident ident, Cursor referenceScope) {
            this.ident = ident;
            this.referenceScope = referenceScope;
            setCursoringOn();
        }

        @Override
        public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable, AtomicReference<Cursor> ctx) {
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
