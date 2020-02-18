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
package com.netflix.rewrite.tree.visitor.refactor.op;

import com.netflix.rewrite.tree.Formatting;
import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.Type;
import com.netflix.rewrite.tree.visitor.refactor.AstTransform;
import com.netflix.rewrite.tree.visitor.refactor.ScopedRefactorVisitor;

import java.util.List;
import java.util.UUID;

import static com.netflix.rewrite.tree.Tr.randomId;

public class ChangeFieldType extends ScopedRefactorVisitor {
    private final String targetType;

    public ChangeFieldType(UUID scope, String targetType) {
        super(scope);
        this.targetType = targetType;
    }

    @Override
    public String getRuleName() {
        return "change-field-type";
    }

    @Override
    public List<AstTransform> visitMultiVariable(Tr.VariableDecls multiVariable) {
        Type.Class originalType = multiVariable.getTypeAsClass();

        return maybeTransform(multiVariable,
                multiVariable.getId().equals(scope) &&
                        originalType != null &&
                        !originalType.getFullyQualifiedName().equals(targetType),
                super::visitMultiVariable,
                mv -> {
                    Type.Class type = Type.Class.build(targetType);
                    maybeRemoveImport(originalType);
                    return mv.withTypeExpr(Tr.Ident.build(randomId(), type.getClassName(), type,
                            mv.getTypeExpr() == null ? Formatting.EMPTY : mv.getTypeExpr().getFormatting()));
                }
        );
    }

    @Override
    public List<AstTransform> visitEnd() {
        maybeAddImport(targetType);
        return super.visitEnd();
    }
}
