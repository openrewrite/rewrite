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

import org.openrewrite.tree.Flag;
import org.openrewrite.tree.J;
import org.openrewrite.tree.Type;
import org.openrewrite.visitor.refactor.AstTransform;
import org.openrewrite.visitor.refactor.ScopedRefactorVisitor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.openrewrite.tree.Formatting.EMPTY;
import static org.openrewrite.tree.J.randomId;

public class ChangeMethodTargetToStatic extends ScopedRefactorVisitor {
    private final String clazz;

    public ChangeMethodTargetToStatic(J.MethodInvocation scope, String clazz) {
        super(scope.getId());
        this.clazz = clazz;
    }

    @Override
    public String getRuleName() {
        return "core.ChangeMethodTarget{to=" + clazz + "}";
    }

    @Override
    public List<AstTransform> visitMethodInvocation(J.MethodInvocation method) {
        return transformIfScoped(method,
                super::visitMethodInvocation,
                m -> {
                    var classType = Type.Class.build(clazz);
                    J.MethodInvocation transformedMethodInvocation = m.withSelect(
                            J.Ident.build(randomId(), classType.getClassName(), classType,
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
