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

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TreeBuilder;
import org.openrewrite.java.tree.TypeUtils;

public final class ChangeFieldName {
    private ChangeFieldName() {
    }

    public static class Scoped extends JavaIsoRefactorVisitor {
        private final JavaType.Class classType;
        private final String hasName;
        private final String toName;

        public Scoped(JavaType.Class classType, String hasName, String toName) {
            this.classType = classType;
            this.hasName = hasName;
            this.toName = toName;
            setCursoringOn();
        }

        @Override
        public Iterable<Tag> getTags() {
            return Tags.of("class.type", classType.getFullyQualifiedName(),
                    "has.name", hasName, "to.name", toName);
        }

        @Override
        public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable) {
            J.VariableDecls.NamedVar v = super.visitVariable(variable);
            if (variable.isField(getCursor()) && matchesClass(enclosingClass().getType()) &&
                    variable.getSimpleName().equals(hasName)) {
                v = v.withName(v.getName().withName(toName));
            }
            return v;
        }

        @Override
        public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess) {
            J.FieldAccess f = super.visitFieldAccess(fieldAccess);
            if (matchesClass(fieldAccess.getTarget().getType()) &&
                    fieldAccess.getSimpleName().equals(hasName)) {
                f = f.withName(f.getName().withName(toName));
            }
            return f;
        }

        @Override
        public J.Ident visitIdentifier(J.Ident ident) {
            J.Ident i = super.visitIdentifier(ident);
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
            Cursor nearest = new FindVariableDefinition(ident, getCursor()).visit(enclosingCompilationUnit());
            return nearest != null && nearest
                    .getParentOrThrow() // maybe J.VariableDecls
                    .getParentOrThrow() // maybe J.Block
                    .getParentOrThrow() // maybe J.ClassDecl
                    .getTree() instanceof J.ClassDecl;
        }

        private static class FindVariableDefinition extends AbstractJavaSourceVisitor<Cursor> {
            private final J.Ident ident;
            private final Cursor referenceScope;

            public FindVariableDefinition(J.Ident ident, Cursor referenceScope) {
                this.ident = ident;
                this.referenceScope = referenceScope;
                setCursoringOn();
            }

            @Override
            public Cursor defaultTo(Tree t) {
                return null;
            }

            @Override
            public Cursor visitVariable(J.VariableDecls.NamedVar variable) {
                return variable.getSimpleName().equalsIgnoreCase(ident.getSimpleName()) && isInSameNameScope(referenceScope) ?
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
}
