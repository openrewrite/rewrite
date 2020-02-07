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
package com.netflix.rewrite.tree.visitor.refactor;

import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.Tree;
import com.netflix.rewrite.tree.visitor.CursorAstVisitor;

import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public abstract class RefactorVisitor extends CursorAstVisitor<List<AstTransform>> {
    @Override
    public List<AstTransform> defaultTo(Tree t) {
        return emptyList();
    }

    protected abstract String getRuleName();

    @SuppressWarnings("unchecked")
    protected List<AstTransform> deleteStatementFromParentBlock(Tree t) {
        return transform(getCursor().getParentOrThrow().getTree(),
                containing -> {
                    Tr.Block<Tree> block = (Tr.Block<Tree>) containing;
                    return block
                            .withStatements(block.getStatements().stream()
                                    .filter(s -> s != t)
                                    .collect(toList())
                            );
                }
        );
    }

    protected <U extends Tree> List<AstTransform> transform(U target, Function<U, U> mutation) {
        return transform(target, getRuleName(), mutation);
    }

    @SuppressWarnings("unchecked")
    protected <U extends Tree> List<AstTransform> transform(U target, String name, Function<U, U> mutation) {
        return singletonList(new AstTransform(target.getId(), name, (Class<Tree>) target.getClass(), t -> mutation.apply((U) t)));
    }
}
