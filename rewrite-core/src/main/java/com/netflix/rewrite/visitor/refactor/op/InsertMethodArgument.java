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

import com.netflix.rewrite.tree.Expression;
import com.netflix.rewrite.tree.Formatting;
import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.Tree;
import com.netflix.rewrite.visitor.refactor.AstTransform;
import com.netflix.rewrite.visitor.refactor.ScopedRefactorVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.netflix.rewrite.tree.Formatting.format;
import static com.netflix.rewrite.tree.Tr.randomId;

public class InsertMethodArgument extends ScopedRefactorVisitor {
    private final int pos;
    private final String source;

    public InsertMethodArgument(UUID scope, int pos, String source) {
        super(scope);
        this.pos = pos;
        this.source = source;
    }

    @Override
    public String getRuleName() {
        return "core.InsertMethodArgument";
    }

    @Override
    public boolean isSingleRun() {
        return true;
    }

    @Override
    public List<AstTransform> visitMethodInvocation(Tr.MethodInvocation method) {
        return maybeTransform(method,
                method.getId().equals(scope),
                super::visitMethodInvocation,
                m -> {
                    List<Expression> modifiedArgs = m.getArgs().getArgs().stream()
                            .filter(a -> !(a instanceof Tr.Empty))
                            .collect(Collectors.toCollection(ArrayList::new));
                    modifiedArgs.add(pos,
                            new Tr.UnparsedSource(randomId(),
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

                    return m.withArgs(m.getArgs().withArgs(modifiedArgs));
                }
        );
    }
}
