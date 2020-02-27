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

import org.openrewrite.tree.J;
import org.openrewrite.tree.Type;
import org.openrewrite.tree.TypeUtils;
import org.openrewrite.visitor.refactor.AstTransform;
import org.openrewrite.visitor.refactor.ScopedRefactorVisitor;
import org.openrewrite.visitor.refactor.AstTransform;

import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

public class ChangeFieldType extends ScopedRefactorVisitor {
    private final String targetType;

    public ChangeFieldType(UUID scope, String targetType) {
        super(scope);
        this.targetType = targetType;
    }

    @Override
    public String getRuleName() {
        return "core.ChangeFieldType{to=" + targetType + "}";
    }

    @Override
    public List<AstTransform> visitMultiVariable(J.VariableDecls multiVariable) {
        Type.Class originalType = multiVariable.getTypeAsClass();

        return maybeTransform(multiVariable,
                multiVariable.getId().equals(scope) &&
                        originalType != null &&
                        !originalType.getFullyQualifiedName().equals(targetType),
                super::visitMultiVariable,
                mv -> {
                    Type.Class type = Type.Class.build(targetType);
                    maybeRemoveImport(originalType);

                    return mv.withTypeExpr(mv.getTypeExpr() == null ? null : J.Ident.build(mv.getTypeExpr().getId(),
                            type.getClassName(), type, mv.getTypeExpr().getFormatting()))
                            .withVars(mv.getVars().stream().map(var -> {
                                Type.Class varType = TypeUtils.asClass(var.getType());
                                if (varType != null && !varType.equals(type)) {
                                    return var.withType(type).withName(var.getName().withType(type));
                                }
                                return var;
                            }).collect(toList()));
                }
        );
    }

    @Override
    public List<AstTransform> visitEnd() {
        maybeAddImport(targetType);
        return super.visitEnd();
    }
}
