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
import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.Type;
import com.netflix.rewrite.tree.visitor.refactor.AstTransform;
import com.netflix.rewrite.tree.visitor.refactor.ScopedRefactorVisitor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.netflix.rewrite.tree.Formatting.EMPTY;
import static com.netflix.rewrite.tree.Tr.randomId;

public class ChangeMethodTargetToStatic extends ScopedRefactorVisitor {
    private final String clazz;

    public ChangeMethodTargetToStatic(UUID scope, String clazz) {
        super(scope);
        this.clazz = clazz;
    }

    @Override
    public String getRuleName() {
        return "change-method-target";
    }

    @Override
    public List<AstTransform> visitMethodInvocation(Tr.MethodInvocation method) {
        return transformIfScoped(method,
                super::visitMethodInvocation,
                m -> {
                    var classType = Type.Class.build(clazz);
                    Tr.MethodInvocation transformedMethodInvocation = m.withSelect(
                            Tr.Ident.build(randomId(), classType.getClassName(), classType,
                                    m.getSelect() == null ? EMPTY : m.getSelect().getFormatting()));

                    Type.Method transformedType = null;
                    if (m.getType() != null) {
                        maybeRemoveImport(m.getType().getDeclaringType());
                        transformedType = m.getType().withDeclaringType(classType);
                        if (!m.getType().hasFlags(Flag.Static)) {
                            Set<Flag> flags = new LinkedHashSet<>(m.getType().getFlags());
                            flags.add(Flag.Static);
                            transformedType = transformedType.withFlags(flags);
                        }
                    }

                    return transformedMethodInvocation.withType(transformedType);
                }
        );
    }

    @Override
    public List<AstTransform> visitEnd() {
        maybeAddImport(clazz);
        return super.visitEnd();
    }
}
