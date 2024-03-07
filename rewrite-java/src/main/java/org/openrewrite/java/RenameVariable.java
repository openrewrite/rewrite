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
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
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
        if (!VariableNameUtils.JavaKeywords.isReserved(toName) && !StringUtils.isBlank(toName) && variable.equals(this.variable)) {
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
                if (parent.getValue() instanceof J.FieldAccess) {
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
            } else if(value instanceof J.NewClass) {
                J.NewClass m = (J.NewClass) value;
                return m.getClazz() != ident;
            } else if(value instanceof J.NewArray) {
                J.NewArray a = (J.NewArray) value;
                return a.getTypeExpression() != ident;
            } else if(value instanceof J.VariableDeclarations) {
                J.VariableDeclarations v = (J.VariableDeclarations) value;
                return ident != v.getTypeExpression();
            } else return !(value instanceof J.ParameterizedType);
        }

        /**
         * FieldAccess targets the variable if its target is an Identifier and either
         * its target FieldType equals variable.Name.FieldType
         * or its target Type equals variable.Name.FieldType.Owner
         * or if FieldAccess targets a TypCast and either
         * its type equals variable.Name.FieldType
         * or its type equals variable.Name.FieldType.Owner.
         * In case the FieldAccess targets another FieldAccess, the target is followed
         * until it is either an Identifier or a TypeCast.
         */
        private boolean fieldAccessTargetsVariable(J.FieldAccess fieldAccess) {
            if (renameVariable.getName().getFieldType() != null) {
                Expression target = getTarget(fieldAccess);
                JavaType targetType = resolveType(target.getType());
                JavaType.Variable variableNameFieldType = renameVariable.getName().getFieldType();
                if (TypeUtils.isOfType(variableNameFieldType.getOwner(), targetType)) {
                    return true;
                }
                if (target instanceof J.TypeCast) {
                    return TypeUtils.isOfType(variableNameFieldType, targetType);
                } else if (target instanceof J.Identifier) {
                    return TypeUtils.isOfType(variableNameFieldType, ((J.Identifier) target).getFieldType());
                }
            }
            return false;
        }

        @Nullable
        private Expression getTarget(J.FieldAccess fieldAccess) {
            Expression target = fieldAccess.getTarget();
            if (target instanceof J.Identifier) {
                return target;
            }
            if (target instanceof J.FieldAccess) {
                return getTarget((J.FieldAccess) target);
            }
            if (target instanceof J.Parentheses<?>) {
                J tree = ((J.Parentheses<?>) target).getTree();
                if (tree instanceof J.TypeCast) {
                    return (J.TypeCast) tree;
                }
                return null;
            }
            return null;
        }

        @Nullable
        private JavaType resolveType(@Nullable JavaType type) {
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

        @Nullable
        @Override
        public J postVisit(J tree, P p) {
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
