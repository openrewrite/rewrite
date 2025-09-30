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
import org.openrewrite.java.tree.*;

import java.util.Stack;

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
        private final Stack<Tree> currentNameScope = new Stack<>();
        private final J.VariableDeclarations.NamedVariable renameVariable;
        private final String newName;

        @Override
        public J.Block visitBlock(J.Block block, P p) {
            // A variable name scope is owned by the ClassDeclaration regardless of what order it is declared.
            // Variables declared in a child block are owned by the corresponding block until the block is exited.
            if (getCursor().getParent() != null && getCursor().getParent().getValue() instanceof J.ClassDeclaration) {
                boolean isClassScope = false;
                for (Statement statement : block.getStatements()) {
                    if (statement instanceof J.VariableDeclarations) {
                        if (((J.VariableDeclarations) statement).getVariables().contains(renameVariable)) {
                            isClassScope = true;
                            break;
                        }
                    }
                }

                if (isClassScope) {
                    currentNameScope.add(block);
                }
            }
            return super.visitBlock(block, p);
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
                } else if (currentNameScope.size() == 1 && isVariableName(parent.getValue(), ident)) {
                    if (parent.getValue() instanceof J.VariableDeclarations.NamedVariable) {
                        Tree variableDeclaration = parent.getParentTreeCursor().getValue();
                        J maybeParameter = getCursor().dropParentUntil(is -> is instanceof JavaSourceFile || is instanceof J.ClassDeclaration || is instanceof J.MethodDeclaration).getValue();
                        if (maybeParameter instanceof J.MethodDeclaration) {
                            J.MethodDeclaration methodDeclaration = (J.MethodDeclaration) maybeParameter;
                            if (methodDeclaration.getParameters().contains((Statement) variableDeclaration) &&
                                    methodDeclaration.getComments().stream().anyMatch(it -> it instanceof Javadoc.DocComment) &&
                                    ((J.MethodDeclaration) maybeParameter).getMethodType() != null) {
                                doAfterVisit(new RenameJavaDocParamNameVisitor<>((J.MethodDeclaration) maybeParameter, renameVariable.getSimpleName(), newName));
                            }
                        }
                    }

                    // The size of the stack will be 1 if the identifier is in the right scope.
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
            if (fieldAccess.getType() instanceof JavaType.Parameterized &&
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

        private boolean isVariableName(Object value, J.Identifier ident) {
            if (value instanceof J.MethodInvocation) {
                J.MethodInvocation m = (J.MethodInvocation) value;
                return m.getName() != ident;
            } else if (value instanceof J.MethodDeclaration) {
                J.MethodDeclaration m = (J.MethodDeclaration) value;
                return m.getName() != ident;
            } else if (value instanceof J.NewClass) {
                J.NewClass m = (J.NewClass) value;
                return m.getClazz() != ident;
            } else if (value instanceof J.NewArray) {
                J.NewArray a = (J.NewArray) value;
                return a.getTypeExpression() != ident;
            } else if (value instanceof J.VariableDeclarations) {
                J.VariableDeclarations v = (J.VariableDeclarations) value;
                return ident != v.getTypeExpression();
            } else return !(value instanceof J.ParameterizedType);
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
            boolean nameEquals = namedVariable.getSimpleName().equals(renameVariable.getSimpleName());
            if (nameEquals) {
                Cursor parentScope = getCursorToParentScope(getCursor());
                // The target variable was found and was not declared in a class declaration block.
                if (currentNameScope.isEmpty()) {
                    if (namedVariable.equals(renameVariable)) {
                        currentNameScope.add(parentScope.getValue());
                    }
                } else {
                    // A variable has been declared and created a new name scope.
                    if (!parentScope.getValue().equals(currentNameScope.peek()) && getCursor().isScopeInPath(currentNameScope.peek())) {
                        currentNameScope.add(parentScope.getValue());
                    }
                }
            }

            J.VariableDeclarations.NamedVariable v = super.visitVariable(namedVariable, p);
            if (nameEquals && v.getVariableType() != null && Boolean.TRUE.equals(getCursor().pollMessage("renamed"))) {
                v = v.withVariableType(v.getVariableType().withName(newName));
            }
            return v;
        }

        @Override
        public J.Try.Catch visitCatch(J.Try.Catch _catch, P p) {
            // Check if the try added a new scope to the stack,
            // postVisit doesn't happen until after both the try and catch are processed.
            maybeChangeNameScope(getCursorToParentScope(getCursor()).getValue());
            return super.visitCatch(_catch, p);
        }

        @Override
        public @Nullable J postVisit(J tree, P p) {
            maybeChangeNameScope(tree);
            return super.postVisit(tree, p);
        }

        /**
         * Used to check if the name scope has changed.
         * Pops the stack if the tree element is at the top of the stack.
         */
        private void maybeChangeNameScope(Tree tree) {
            if (!currentNameScope.isEmpty() && currentNameScope.peek().equals(tree)) {
                currentNameScope.pop();
            }
        }

        /**
         * Returns either the current block or a J.Type that may create a reference to a variable.
         * I.E. for(int target = 0; target < N; target++) creates a new name scope for `target`.
         * The name scope in the next J.Block `{}` cannot create new variables with the name `target`.
         * <p>
         * J.* types that may only reference an existing name and do not create a new name scope are excluded.
         */
        private Cursor getCursorToParentScope(Cursor cursor) {
            return cursor.dropParentUntil(is ->
                    is instanceof JavaSourceFile ||
                            is instanceof J.ClassDeclaration ||
                            is instanceof J.MethodDeclaration ||
                            is instanceof J.Block ||
                            is instanceof J.ForLoop ||
                            is instanceof J.ForEachLoop ||
                            is instanceof J.Case ||
                            is instanceof J.Try ||
                            is instanceof J.Try.Catch ||
                            is instanceof J.Lambda);
        }
    }

}
