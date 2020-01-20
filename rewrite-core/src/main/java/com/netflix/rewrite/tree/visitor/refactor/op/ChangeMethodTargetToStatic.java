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

import com.netflix.rewrite.tree.Flag;
import com.netflix.rewrite.tree.Formatting;
import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.Type;
import com.netflix.rewrite.tree.visitor.refactor.AstTransform;
import com.netflix.rewrite.tree.visitor.refactor.RefactorVisitor;
import lombok.AllArgsConstructor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.netflix.rewrite.tree.Tr.randomId;

@AllArgsConstructor
public class ChangeMethodTargetToStatic extends RefactorVisitor<Tr.MethodInvocation> {
    String clazz;

    @Override
    protected String getRuleName() {
        return "change-method-target";
    }

    @Override
    public List<AstTransform<Tr.MethodInvocation>> visitMethodInvocation(Tr.MethodInvocation method) {
        var classType = Type.Class.build(clazz);
        return transform(m -> {
            Tr.MethodInvocation transformedMethodInvocation = m
                    .withSelect(Tr.Ident.build(randomId(), classType.getClassName(), classType, method.getSelect() == null ?
                            Formatting.EMPTY : method.getSelect().getFormatting()));

            Type.Method transformedType = null;
            if (m.getType() != null) {
                transformedType = m.getType().withDeclaringType(classType);
                if (!m.getType().hasFlags(Flag.Static)) {
                    Set<Flag> flags = new LinkedHashSet<>(m.getType().getFlags());
                    flags.add(Flag.Static);
                    transformedType = transformedType.withFlags(flags);
                }
            }

            return transformedMethodInvocation.withType(transformedType);
        });
    }
}
