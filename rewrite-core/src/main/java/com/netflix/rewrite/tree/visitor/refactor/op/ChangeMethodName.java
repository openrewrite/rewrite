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

@AllArgsConstructor
public class ChangeMethodName extends RefactorVisitor<Tr.MethodInvocation> {
    String name;

    @Override
    protected String getRuleName() {
        return "change-method-name";
    }

    @Override
    public List<AstTransform<Tr.MethodInvocation>> visitMethodInvocation(Tr.MethodInvocation method) {
        return transform(m -> m.withName(m.getName().withName(name)));
    }
}
