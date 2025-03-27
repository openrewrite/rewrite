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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;

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
        private final JavaType.FullyQualified existingOwningTypeFqn;
        private final JavaType.FullyQualified newOwningType;
        private final JavaType.FullyQualified newTargetType;
        private final String newConstantName;

        public ReplaceConstantWithAnotherConstantVisitor(String existingFullyQualifiedConstantName, String fullyQualifiedConstantName) {
            this.existingOwningType = existingFullyQualifiedConstantName.substring(0, existingFullyQualifiedConstantName.lastIndexOf('.'));
            this.constantName = existingFullyQualifiedConstantName.substring(existingFullyQualifiedConstantName.lastIndexOf('.') + 1);
            this.existingOwningTypeFqn = JavaType.ShallowClass.build(existingOwningType);
            this.newOwningType = JavaType.ShallowClass.build(fullyQualifiedConstantName.substring(0, fullyQualifiedConstantName.lastIndexOf('.')));
            this.newTargetType = JavaType.ShallowClass.build(fullyQualifiedConstantName);
            this.newConstantName = fullyQualifiedConstantName.substring(fullyQualifiedConstantName.lastIndexOf('.') + 1);
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            JavaType.Variable fieldType = fieldAccess.getName().getFieldType();
            if (isConstant(fieldType)) {
                JavaSourceFile sf = getCursor().firstEnclosing(JavaSourceFile.class);
                return replaceFieldAccess(fieldAccess, fieldType, hasNoConflictingImport(sf));
            }
            return super.visitFieldAccess(fieldAccess, ctx);
        }

        @Override
        public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
            JavaType.Variable fieldType = ident.getFieldType();
            if (isConstant(fieldType)) {
                JavaSourceFile sf = getCursor().firstEnclosing(JavaSourceFile.class);
                return replaceFieldAccess(ident, fieldType, hasNoConflictingImport(sf));
            }
            return super.visitIdentifier(ident, ctx);
        }

        private J replaceFieldAccess(Expression expression, JavaType.Variable fieldType, boolean hasNoConflictingImport) {
            JavaType owner = fieldType.getOwner();
            while (owner instanceof JavaType.FullyQualified) {
                maybeRemoveImport(((JavaType.FullyQualified) owner).getFullyQualifiedName());
                owner = ((JavaType.FullyQualified) owner).getOwningClass();
            }

            if (expression instanceof J.Identifier) {
                String realName = hasNoConflictingImport ? newConstantName : newTargetType.getFullyQualifiedName();
                if (hasNoConflictingImport) {
                    maybeAddImport(newOwningType.getFullyQualifiedName(), newConstantName, false);
                }
                J.Identifier identifier = (J.Identifier) expression;
                return identifier
                        .withSimpleName(realName)
                        .withFieldType(fieldType.withOwner(newOwningType).withName(realName));
            } else if (expression instanceof J.FieldAccess) {
                if (hasNoConflictingImport) {
                    maybeAddImport(newOwningType.getFullyQualifiedName(), false);
                }
                J.FieldAccess fieldAccess = (J.FieldAccess) expression;
                Expression target = fieldAccess.getTarget();
                J.Identifier name = fieldAccess.getName();
                String realName = hasNoConflictingImport ? newOwningType.getClassName() : newOwningType.getFullyQualifiedName();
                if (target instanceof J.Identifier) {
                    target = ((J.Identifier) target).withType(newOwningType).withSimpleName(realName);
                    name = name
                            .withFieldType(fieldType.withOwner(newOwningType).withName(newConstantName))
                            .withSimpleName(newConstantName);
                } else {
                    target = (((J.FieldAccess) target).getName()).withType(newOwningType).withSimpleName(realName);
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

        private boolean isConstant(JavaType.@Nullable Variable varType) {
            return varType != null && TypeUtils.isOfClassType(varType.getOwner(), existingOwningType) &&
                    varType.getName().equals(constantName);
        }

        private boolean hasNoConflictingImport(@Nullable JavaSourceFile sf) {
            return hasNoConflictingImport(sf, newOwningType) && hasNoConflictingImport(sf, newTargetType);
        }

        private boolean hasNoConflictingImport(@Nullable JavaSourceFile sf, JavaType.FullyQualified targetType) {
            if (sf == null || targetType == null) {
                return true;
            }

            for (J.Import anImport : sf.getImports()) {
                if (anImport.isStatic()) {
                    JavaType.FullyQualified fqn = TypeUtils.asFullyQualified(anImport.getQualid().getTarget().getType());
                    if (anImport.getQualid().getSimpleName().equals(newConstantName) && !TypeUtils.isOfClassType(fqn, newOwningType.getFullyQualifiedName())) {
                        if (!anImport.getQualid().getSimpleName().equals(constantName) || !TypeUtils.isOfClassType(fqn, existingOwningTypeFqn.getFullyQualifiedName())) {
                            return false;
                        }
                    }
                } else {
                    JavaType.FullyQualified currType = TypeUtils.asFullyQualified(anImport.getQualid().getType());
                    if (currType != null &&
                            !TypeUtils.isOfType(currType, targetType) &&
                            currType.getClassName().equals(targetType.getClassName())) {
                        return false;
                    }
                }

            }
            return true;
        }
    }
}
