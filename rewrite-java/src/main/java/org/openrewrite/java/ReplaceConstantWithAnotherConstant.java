/*
 * Copyright 2023 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceConstantWithAnotherConstant extends Recipe {

    @Option(displayName = "Fully qualified name of the constant to replace",
            example = "org.springframework.http.MediaType.APPLICATION_JSON_VALUE")
    String existingFullyQualifiedConstantName;

    @Option(displayName = "Fully qualified name of the constant to use in place of existing constant",
            example = "org.springframework.http.MediaType.APPLICATION_JSON_VALUE")
    String fullyQualifiedConstantName;

    @Override
    public String getDisplayName() {
        return "Replace constant with another constant";
    }

    @Override
    public String getDescription() {
        return "Replace a constant with another constant, adding/removing import on class if needed.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(existingFullyQualifiedConstantName.substring(0, existingFullyQualifiedConstantName.lastIndexOf('.')), false),
                new ReplaceConstantWithAnotherConstantVisitor(existingFullyQualifiedConstantName, fullyQualifiedConstantName));
    }

    private static class ReplaceConstantWithAnotherConstantVisitor extends JavaVisitor<ExecutionContext> {

        private final String existingOwningType;
        private final String constantName;
        private final JavaType.FullyQualified newOwningType;
        private final String newConstantName;

        public ReplaceConstantWithAnotherConstantVisitor(String existingFullyQualifiedConstantName, String fullyQualifiedConstantName) {
            this.existingOwningType = existingFullyQualifiedConstantName.substring(0, existingFullyQualifiedConstantName.lastIndexOf('.'));
            this.constantName = existingFullyQualifiedConstantName.substring(existingFullyQualifiedConstantName.lastIndexOf('.') + 1);
            this.newOwningType = JavaType.ShallowClass.build(fullyQualifiedConstantName.substring(0, fullyQualifiedConstantName.lastIndexOf('.')));
            this.newConstantName = fullyQualifiedConstantName.substring(fullyQualifiedConstantName.lastIndexOf('.') + 1);
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            JavaType.Variable fieldType = fieldAccess.getName().getFieldType();
            if (isConstant(fieldType)) {
                return replaceFieldAccess(fieldAccess, fieldType);
            }
            return super.visitFieldAccess(fieldAccess, ctx);
        }

        @Override
        public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
            JavaType.Variable fieldType = ident.getFieldType();
            if (isConstant(fieldType) && !isVariableDeclaration()) {
                return replaceFieldAccess(ident, fieldType);
            }
            return super.visitIdentifier(ident, ctx);
        }

        private J replaceFieldAccess(Expression expression, JavaType.Variable fieldType) {
            JavaType owner = fieldType.getOwner();
            while (owner instanceof JavaType.FullyQualified) {
                maybeRemoveImport(((JavaType.FullyQualified) owner).getFullyQualifiedName());
                owner = ((JavaType.FullyQualified) owner).getOwningClass();
            }

            if (expression instanceof J.Identifier) {
                maybeAddImport(newOwningType.getFullyQualifiedName(), newConstantName, false);
                J.Identifier identifier = (J.Identifier) expression;
                return identifier
                        .withSimpleName(newConstantName)
                        .withFieldType(fieldType.withOwner(newOwningType).withName(newConstantName));
            } else if (expression instanceof J.FieldAccess) {
                maybeAddImport(newOwningType.getFullyQualifiedName(), false);
                J.FieldAccess fieldAccess = (J.FieldAccess) expression;
                Expression target = fieldAccess.getTarget();
                J.Identifier name = fieldAccess.getName();
                if (target instanceof J.Identifier) {
                    target = ((J.Identifier) target).withType(newOwningType).withSimpleName(newOwningType.getClassName());
                    name = name
                            .withFieldType(fieldType.withOwner(newOwningType).withName(newConstantName))
                            .withSimpleName(newConstantName);
                } else {
                    target = (((J.FieldAccess) target).getName()).withType(newOwningType).withSimpleName(newOwningType.getClassName());
                    name = name
                            .withFieldType(fieldType.withOwner(newOwningType).withName(newConstantName))
                            .withSimpleName(newConstantName);
                }
                return fieldAccess
                        .withTarget(target)
                        .withName(name);
            }
            return expression;
        }

        private boolean isConstant(@Nullable JavaType.Variable varType) {
            return varType != null && TypeUtils.isOfClassType(varType.getOwner(), existingOwningType) &&
                    varType.getName().equals(constantName);
        }

        private boolean isVariableDeclaration() {
            Cursor maybeVariable = getCursor().dropParentUntil(is -> is instanceof J.VariableDeclarations || is instanceof J.CompilationUnit);
            if (!(maybeVariable.getValue() instanceof J.VariableDeclarations)) {
                return false;
            }
            JavaType.Variable variableType = ((J.VariableDeclarations) maybeVariable.getValue()).getVariables().get(0).getVariableType();
            if (variableType == null) {
                return true;
            }

            JavaType.FullyQualified ownerFqn = TypeUtils.asFullyQualified(variableType.getOwner());
            if (ownerFqn == null) {
                return true;
            }

            return constantName.equals(((J.VariableDeclarations) maybeVariable.getValue()).getVariables().get(0).getSimpleName()) &&
                    existingOwningType.equals(ownerFqn.getFullyQualifiedName());
        }
    }
}
