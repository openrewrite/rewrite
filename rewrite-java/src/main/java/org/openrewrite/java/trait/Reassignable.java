/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

/**
 * A syntactic check that an expression could be the target of a plain assignment
 * {@code target = ...}. Matches a non-final local {@link J.Identifier} declared
 * in an enclosing block, or a {@link J.FieldAccess} on a non-final field whose
 * target is a bare {@link J.Identifier}.
 */
@Incubating(since = "8.68.0")
@Value
public class Reassignable implements Trait<Expression> {
    Cursor cursor;

    public static class Matcher extends SimpleTraitMatcher<Reassignable> {

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<Reassignable, P> visitor) {
            return new JavaVisitor<P>() {
                @Override
                public J visitIdentifier(J.Identifier ident, P p) {
                    Reassignable r = test(getCursor());
                    return r != null ?
                            (J) visitor.visit(r, p) :
                            super.visitIdentifier(ident, p);
                }

                @Override
                public J visitFieldAccess(J.FieldAccess fieldAccess, P p) {
                    Reassignable r = test(getCursor());
                    return r != null ?
                            (J) visitor.visit(r, p) :
                            super.visitFieldAccess(fieldAccess, p);
                }
            };
        }

        @Override
        protected @Nullable Reassignable test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof J.Identifier && isReassignableLocalIdentifier(cursor, (J.Identifier) value)) {
                return new Reassignable(cursor);
            }
            if (value instanceof J.FieldAccess && isReassignableFieldAccess((J.FieldAccess) value)) {
                return new Reassignable(cursor);
            }
            return null;
        }

        private static boolean isReassignableLocalIdentifier(Cursor cursor, J.Identifier ident) {
            // Skip identifiers that appear as the name-part of a larger construct
            // (field-access field name, method-invocation name, declaration name, etc.).
            Object parent = cursor.getParentTreeCursor().getValue();
            if (parent instanceof J.FieldAccess && ((J.FieldAccess) parent).getName() == ident) {
                return false;
            }
            if (parent instanceof J.MethodInvocation && ((J.MethodInvocation) parent).getName() == ident) {
                return false;
            }
            if (parent instanceof J.VariableDeclarations.NamedVariable &&
                    ((J.VariableDeclarations.NamedVariable) parent).getName() == ident) {
                return false;
            }

            if (ident.getFieldType() != null && ident.getFieldType().hasFlags(Flag.Final)) {
                return false;
            }
            String name = ident.getSimpleName();
            Cursor c = cursor.getParent();
            while (c != null) {
                Object v = c.getValue();
                if (v instanceof J.Block) {
                    for (Statement stmt : ((J.Block) v).getStatements()) {
                        if (stmt instanceof J.VariableDeclarations) {
                            J.VariableDeclarations vd = (J.VariableDeclarations) stmt;
                            for (J.VariableDeclarations.NamedVariable nv : vd.getVariables()) {
                                if (name.equals(nv.getName().getSimpleName()) &&
                                        TypeUtils.isOfType(nv.getName().getType(), ident.getType())) {
                                    return !vd.hasModifier(J.Modifier.Type.Final);
                                }
                            }
                        }
                    }
                }
                if (v instanceof J.MethodDeclaration) {
                    for (Statement p : ((J.MethodDeclaration) v).getParameters()) {
                        if (p instanceof J.VariableDeclarations) {
                            for (J.VariableDeclarations.NamedVariable nv : ((J.VariableDeclarations) p).getVariables()) {
                                if (name.equals(nv.getName().getSimpleName())) {
                                    return false;
                                }
                            }
                        }
                    }
                    return false;
                }
                c = c.getParent();
            }
            return false;
        }

        private static boolean isReassignableFieldAccess(J.FieldAccess fa) {
            if (!(fa.getTarget() instanceof J.Identifier)) {
                return false;
            }
            JavaType.Variable v = fa.getName().getFieldType();
            return v != null && !v.hasFlags(Flag.Final);
        }
    }
}
