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

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.cleanup.RenameJavaDocParamNameVisitor;
import org.openrewrite.java.trait.Scope;
import org.openrewrite.java.tree.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Renames a NamedVariable to the target name.
 * Prevents variables from being renamed to reserved java keywords.
 * Notes:
 * - The current version will rename variables even if a variable with `toName` is already declared in the same scope.
 */
public class RenameVariable<P> extends JavaIsoVisitor<P> {
    private final J.VariableDeclarations.NamedVariable variable;
    private final String toName;

    public RenameVariable(J.VariableDeclarations.NamedVariable variable, String toName) {
        this.variable = variable;
        this.toName = toName;
    }

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
        if (!JavaKeywordUtils.isReservedKeyword(toName) && !JavaKeywordUtils.isReservedLiteral(toName) && !StringUtils.isBlank(toName) && variable.equals(this.variable)) {
            doAfterVisit(new RenameVariableVisitor(variable, toName));
            return variable;
        }
        return super.visitVariable(variable, p);
    }

    @RequiredArgsConstructor
    private class RenameVariableVisitor extends JavaIsoVisitor<P> {
        private final J.VariableDeclarations.NamedVariable renameVariable;
        private final String newName;

        /**
         * The declaring identifier and every identifier the scope model resolves to it, by id.
         */
        private final Set<UUID> renamed = new HashSet<>();

        /**
         * Whether the variable is a field, whose name a Javadoc reference such as {@code {@link #name}} also spells.
         */
        private boolean field;

        /**
         * Whether the references were collected, which happens on the tree the visit starts from.
         */
        private boolean resolved;

        @Override
        public @Nullable J visit(@Nullable Tree tree, P p) {
            if (!resolved && tree instanceof J) {
                resolved = true;
                new JavaIsoVisitor<Set<UUID>>() {
                    @Override
                    public J.Identifier visitIdentifier(J.Identifier identifier, Set<UUID> found) {
                        Object parent = getCursor().getParentTreeCursor().getValue();
                        if (parent instanceof J.VariableDeclarations.NamedVariable && parent.equals(renameVariable) &&
                            ((J.VariableDeclarations.NamedVariable) parent).getName() == identifier) {
                            Scope.bindingOf(getCursor(), identifier).ifPresent(binding -> {
                                field = binding.getKind() == Scope.Kind.MEMBER;
                                found.add(identifier.getId());
                                binding.references().forEach(reference -> found.add(reference.<J>getValue().getId()));
                            });
                        }
                        return identifier;
                    }
                }.visit(tree, renamed, getCursor());
            }
            return super.visit(tree, p);
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier ident, P p) {
            if (getCursor().dropParentUntil(it -> it instanceof Javadoc.Parameter ||
                    it instanceof Comment ||
                    it instanceof J.ClassDeclaration ||
                    it instanceof J.MethodDeclaration ||
                    it instanceof J.VariableDeclarations ||
                    it instanceof SourceFile).getValue() instanceof Javadoc.Parameter) {
                return ident;
            }

            Cursor parent = getCursor().getParentTreeCursor();
            if (ident.getSimpleName().equals(renameVariable.getSimpleName())) {
                if (ident.getFieldType() != null && ident.getFieldType().getOwner() instanceof JavaType.FullyQualified &&
                        TypeUtils.isOfType(ident.getFieldType(), renameVariable.getVariableType())) {
                    parent.putMessage("renamed", true);
                    return ident.withFieldType(ident.getFieldType().withName(newName)).withSimpleName(newName);
                } else if (parent.getValue() instanceof J.FieldAccess &&
                        !ident.equals(((J.FieldAccess) parent.getValue()).getTarget())) {
                    if (fieldAccessTargetsVariable(parent.getValue())) {
                        if (ident.getFieldType() != null) {
                            ident = ident.withFieldType(ident.getFieldType().withName(newName));
                        }
                        parent.putMessage("renamed", true);
                        return ident.withSimpleName(newName);
                    }
                } else if (renamed.contains(ident.getId()) || field && getCursor().firstEnclosing(Javadoc.class) != null) {
                    if (parent.getValue() instanceof J.VariableDeclarations.NamedVariable) {
                        Tree variableDeclaration = parent.getParentTreeCursor().getValue();
                        J maybeParameter = getCursor().dropParentUntil(is -> is instanceof JavaSourceFile || is instanceof J.ClassDeclaration || is instanceof J.MethodDeclaration).getValue();
                        if (maybeParameter instanceof J.MethodDeclaration) {
                            J.MethodDeclaration methodDeclaration = (J.MethodDeclaration) maybeParameter;
                            if (methodDeclaration.getParameters().contains((Statement) variableDeclaration) &&
                                    methodDeclaration.getComments().stream().anyMatch(Javadoc.DocComment.class::isInstance) &&
                                    ((J.MethodDeclaration) maybeParameter).getMethodType() != null) {
                                doAfterVisit(new RenameJavaDocParamNameVisitor<>((J.MethodDeclaration) maybeParameter, renameVariable.getSimpleName(), newName));
                            }
                        }
                    }

                    if (ident.getFieldType() != null) {
                        ident = ident.withFieldType(ident.getFieldType().withName(newName));
                    }
                    parent.putMessage("renamed", true);
                    return ident.withSimpleName(newName);
                }
            }
            return super.visitIdentifier(ident, p);
        }

        @Override
        public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, P p) {
            if ("class".equals(fieldAccess.getName().getSimpleName()) &&
                    fieldAccess.getType() instanceof JavaType.Parameterized &&
                    ((JavaType.Parameterized) fieldAccess.getType()).getType() instanceof JavaType.Class) {
                return fieldAccess; // Avoid renaming `Foo` in `Foo.class`
            }
            return super.visitFieldAccess(fieldAccess, p);
        }

        @Override
        public J.MemberReference visitMemberReference(J.MemberReference memberRef, P p) {
            J.MemberReference m = super.visitMemberReference(memberRef, p);
            if (m != memberRef && m.getVariableType() != null && m.getVariableType().getName().equals(renameVariable.getSimpleName())) {
                m = m.withVariableType(m.getVariableType().withName(newName));
            }
            return m;
        }

        /**
         * FieldAccess targets the variable if its target type equals variable.Name.FieldType.Owner.
         */
        private boolean fieldAccessTargetsVariable(J.FieldAccess fieldAccess) {
            if (renameVariable.getName().getFieldType() != null &&
                    fieldAccess.getTarget().getType() != null) {
                JavaType targetType = resolveType(fieldAccess.getTarget().getType());
                JavaType.Variable variableNameFieldType = renameVariable.getName().getFieldType();
                return TypeUtils.isOfType(resolveType(variableNameFieldType.getOwner()), targetType);
            }
            return false;
        }

        private @Nullable JavaType resolveType(@Nullable JavaType type) {
            return type instanceof JavaType.Parameterized ? ((JavaType.Parameterized) type).getType() : type;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable namedVariable, P p) {
            J.VariableDeclarations.NamedVariable v = super.visitVariable(namedVariable, p);
            if (namedVariable.getSimpleName().equals(renameVariable.getSimpleName()) && v.getVariableType() != null &&
                Boolean.TRUE.equals(getCursor().pollMessage("renamed"))) {
                v = v.withVariableType(v.getVariableType().withName(newName));
            }
            return v;
        }
    }

}
