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
import com.netflix.rewrite.tree.visitor.refactor.AstTransform;
import com.netflix.rewrite.tree.visitor.refactor.RefactorVisitor;
import lombok.AllArgsConstructor;

import java.util.List;

import static com.netflix.rewrite.tree.Tr.randomId;
import static java.util.Collections.*;

@AllArgsConstructor
public class ChangeFieldName extends RefactorVisitor<Tr.VariableDecls> {
    String name;

    @Override
    protected String getRuleName() {
        return "change-field-name";
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public List<AstTransform<Tr.VariableDecls>> visitMultiVariable(Tr.VariableDecls multiVariable) {
        if(multiVariable.getVars().size() > 1) {
            // change field name is not supported on multi-variable declarations
            return emptyList();
        }

        var v = multiVariable.getVars().stream().findAny().get();
        return v.getSimpleName().equals(name) ?
                emptyList() :
                transform(mv -> mv.withVars(singletonList(v.withName(
                        Tr.Ident.build(randomId(), name, v.getName().getType(), v.getName().getFormatting())))));
    }
}
