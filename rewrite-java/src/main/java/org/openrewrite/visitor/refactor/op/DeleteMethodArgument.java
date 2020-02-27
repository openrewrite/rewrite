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

import org.openrewrite.tree.Expression;
import org.openrewrite.tree.Formatting;
import org.openrewrite.tree.J;
import org.openrewrite.visitor.refactor.AstTransform;
import org.openrewrite.visitor.refactor.ScopedRefactorVisitor;
import org.openrewrite.tree.Formatting;
import org.openrewrite.visitor.refactor.AstTransform;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.openrewrite.tree.J.randomId;
import static java.util.Collections.singletonList;

public class DeleteMethodArgument extends ScopedRefactorVisitor {
    private final int pos;

    public DeleteMethodArgument(UUID scope, int pos) {
        super(scope);
        this.pos = pos;
    }

    @Override
    public String getRuleName() {
        return "core.DeleteMethodArgument";
    }

    @Override
    public boolean isSingleRun() {
        return true;
    }

    @Override
    public List<AstTransform> visitMethodInvocation(J.MethodInvocation method) {
        return maybeTransform(method,
                method.getId().equals(scope) && method.getArgs().getArgs().stream()
                        .filter(arg -> !(arg instanceof J.Empty)).count() > pos,
                super::visitMethodInvocation,
                m -> {
                    List<Expression> args = new ArrayList<>(m.getArgs().getArgs());
                    args.remove(pos);
                    if (args.isEmpty()) {
                        args = singletonList(new J.Empty(randomId(), Formatting.EMPTY));
                    }
                    return m.withArgs(m.getArgs().withArgs(args));
                });
    }
}
