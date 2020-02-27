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
package org.openrewrite.visitor.refactor;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.tree.Cursor;
import org.openrewrite.tree.Tree;
import lombok.RequiredArgsConstructor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.tree.Cursor;
import org.openrewrite.tree.Tree;

import java.util.List;
import java.util.Spliterators;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.stream.StreamSupport.stream;

@RequiredArgsConstructor
public abstract class ScopedRefactorVisitor extends RefactorVisitor {
    protected final UUID scope;

    protected boolean isScope(@Nullable Tree t) {
        return t != null && scope.equals(t.getId());
    }

    protected boolean isInScope(@Nullable Tree t) {
        return (t != null && t.getId().equals(scope)) ||
                stream(Spliterators.spliteratorUnknownSize(getCursor().getPath(), 0), false)
                    .anyMatch(p -> p.getId().equals(scope));
    }

    protected <T extends Tree> List<AstTransform> transformIfScoped(T tree,
                                                                    Function<T, List<AstTransform>> callSuper,
                                                                    Function<T, T> mutation) {
        return maybeTransform(tree, tree.getId().equals(scope), callSuper, mutation);
    }

    protected <T extends Tree, U extends Tree> List<AstTransform> transformIfScoped(T tree,
                                                                                    Function<T, List<AstTransform>> callSuper,
                                                                                    Function<T, U> transformNestedElement,
                                                                                    Function<U, U> mutation) {
        return maybeTransform(tree, tree.getId().equals(scope), callSuper, transformNestedElement, mutation);
    }

    protected <T extends Tree> List<AstTransform> transformIfScoped(T tree,
                                                                    Function<T, List<AstTransform>> callSuper,
                                                                    BiFunction<T, Cursor, T> mutation) {
        return maybeTransform(tree, tree.getId().equals(scope), callSuper, mutation);
    }
}
