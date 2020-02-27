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
import org.openrewrite.visitor.refactor.AstTransform;
import org.openrewrite.visitor.refactor.ScopedRefactorVisitor;
import org.openrewrite.visitor.refactor.AstTransform;

import java.util.List;
import java.util.UUID;

public class ChangeMethodName extends ScopedRefactorVisitor {
    private final String name;

    public ChangeMethodName(UUID scope, String name) {
        super(scope);
        this.name = name;
    }

    @Override
    public String getRuleName() {
        return "core.ChangeMethodName{to=" + name + "}";
    }

    @Override
    public List<AstTransform> visitMethodInvocation(J.MethodInvocation method) {
        return transformIfScoped(method,
                super::visitMethodInvocation,
                m -> m.withName(m.getName().withName(name)));
    }
}
