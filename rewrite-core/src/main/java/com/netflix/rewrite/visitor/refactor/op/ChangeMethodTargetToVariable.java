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
package com.netflix.rewrite.visitor.refactor.op;

import com.netflix.rewrite.internal.lang.Nullable;
import com.netflix.rewrite.tree.*;
import com.netflix.rewrite.visitor.refactor.AstTransform;
import com.netflix.rewrite.visitor.refactor.ScopedRefactorVisitor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.netflix.rewrite.tree.Tr.randomId;

public class ChangeMethodTargetToVariable extends ScopedRefactorVisitor {
    private final String varName;

    @Nullable
    private final Type.Class type;

    public ChangeMethodTargetToVariable(UUID scope, String varName, @Nullable Type.Class type) {
        super(scope);
        this.varName = varName;
        this.type = type;
    }

    @Override
    public String getRuleName() {
        return "core.ChangeMethodTarget{to=" + varName + "}";
    }

    @Override
    public List<AstTransform> visitMethodInvocation(Tr.MethodInvocation method) {
        return maybeTransform(method,
                method.getId().equals(scope),
                super::visitMethodInvocation,
                m -> {
                    Expression select = m.getSelect();

                    Type.Method methodType = null;
                    if (m.getType() != null) {
                        // if the original is a static method invocation, the import on it's type may no longer be needed
                        maybeRemoveImport(m.getType().getDeclaringType());

                        Set<Flag> flags = new LinkedHashSet<>(m.getType().getFlags());
                        flags.remove(Flag.Static);
                        methodType = m.getType().withDeclaringType(this.type).withFlags(flags);
                    }

                    return m
                            .withSelect(Tr.Ident.build(randomId(), varName, type, select == null ? Formatting.EMPTY : select.getFormatting()))
                            .withType(methodType);
                }
        );
    }
}
