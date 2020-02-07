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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@AllArgsConstructor
public class ChangeFieldName extends RefactorVisitor {
    String name;

    @Override
    protected String getRuleName() {
        return "change-field-name";
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public List<AstTransform> visitMultiVariable(Tr.VariableDecls multiVariable) {
        if(multiVariable.getVars().size() > 1) {
            // change field name is not supported on multi-variable declarations
            return emptyList();
        }

        var v = multiVariable.getVars().stream().findAny().get();
        return v.getSimpleName().equals(name) ?
                emptyList() :
                transform(multiVariable, mv -> mv.withVars(singletonList(v.withName(v.getName().withName(name)))));
    }
}
