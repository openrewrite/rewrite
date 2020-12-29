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
package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CompositeEvalVisitor extends EvalVisitor<Tree> {
    private final List<EvalVisitor<? extends Tree>> delegates = new ArrayList<>();

    public void addVisitor(EvalVisitor<? extends Tree> visitor) {
        delegates.add(visitor);
    }

    @Override
    public Validated validate() {
        return delegates.stream()
                .map(EvalVisitor::validate)
                .reduce(Validated.none(), (validation, acc) -> acc.and(validation));
    }

    @Nullable
    @Override
    public Tree visit(@Nullable Tree tree, EvalContext ctx) {
        return tree;
    }

    public void extendsFrom(CompositeEvalVisitor delegate) {
        delegates.add(0, delegate);
    }

    @Override
    public Tree defaultValue(Tree t, EvalContext ctx) {
        return delegates.stream()
                .map(d -> d instanceof CompositeEvalVisitor ?
                        ((CompositeEvalVisitor) d).defaultValue(t, ctx) :
                        d.defaultValue(t, ctx))
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    private final List<EvalVisitor<? extends Tree>> andThen = new ArrayList<>();
}
