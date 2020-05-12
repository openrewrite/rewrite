/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.refactor;

import org.openrewrite.Formatting;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.openrewrite.Formatting.format;
import static org.openrewrite.Tree.randomId;

public class InsertMethodArgument extends ScopedJavaRefactorVisitor {
    private final int pos;
    private final String source;

    public InsertMethodArgument(J.MethodInvocation scope, int pos, String source) {
        super(scope.getId());
        this.pos = pos;
        this.source = source;
    }

    @Override
    public String getName() {
        return "core.InsertMethodArgument";
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method) {
        if (isScope()) {
            List<Expression> modifiedArgs = method.getArgs().getArgs().stream()
                    .filter(a -> !(a instanceof J.Empty))
                    .collect(Collectors.toCollection(ArrayList::new));
            modifiedArgs.add(pos,
                    new J.UnparsedSource(randomId(),
                            source,
                            pos == 0 ?
                                    modifiedArgs.stream().findFirst().map(Tree::getFormatting).orElse(Formatting.EMPTY) :
                                    format(" ")
                    )
            );

            if (pos == 0 && modifiedArgs.size() > 1) {
                // this argument previously did not occur after a comma, and now does, so let's introduce a bit of space
                modifiedArgs.set(1, modifiedArgs.get(1).withFormatting(format(" ")));
            }

            return method.withArgs(method.getArgs().withArgs(modifiedArgs));
        }

        return super.visitMethodInvocation(method);
    }
}
