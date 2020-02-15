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

import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.Type;
import com.netflix.rewrite.tree.TypeUtils;
import com.netflix.rewrite.tree.visitor.refactor.AstTransform;
import com.netflix.rewrite.tree.visitor.refactor.RefactorVisitor;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ChangeFieldName extends RefactorVisitor {
    private final Type.Class classType;
    private final String hasName;
    private final String toName;

    @Override
    public String getRuleName() {
        return "change-field-name";
    }

    @Override
    public List<AstTransform> visitVariable(Tr.VariableDecls.NamedVar variable) {
        boolean isField = getCursor()
                .getParentOrThrow() // Tr.VariableDecls
                .getParentOrThrow() // Tr.Block
                .getParentOrThrow() // maybe Tr.ClassDecl
                .getTree() instanceof Tr.ClassDecl;
        Type.Class containingClassType = TypeUtils.asClass(getCursor().enclosingClass().getType());

        return maybeTransform(isField && containingClassType == classType && variable.getSimpleName().equals(hasName),
                super.visitVariable(variable),
                transform(variable, v -> v.withName(v.getName().withName(toName)))
        );
    }

    @Override
    public List<AstTransform> visitFieldAccess(Tr.FieldAccess fieldAccess) {
        Type.Class targetType = TypeUtils.asClass(fieldAccess.getTarget().getType());
        return maybeTransform(targetType == classType && fieldAccess.getSimpleName().equals(hasName),
                super.visitFieldAccess(fieldAccess),
                transform(fieldAccess, fa -> fa.withName(fa.getName().withName(toName)))
        );
    }
}
