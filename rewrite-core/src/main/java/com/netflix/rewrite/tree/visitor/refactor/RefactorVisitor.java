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

import com.netflix.rewrite.tree.Tree;
import com.netflix.rewrite.tree.visitor.CursorAstVisitor;

import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public abstract class RefactorVisitor<T extends Tree> extends CursorAstVisitor<List<AstTransform<T>>> {
    @Override
    public List<AstTransform<T>> defaultTo(Tree t) {
        return emptyList();
    }

    protected abstract String getRuleName();

    protected List<AstTransform<T>> transform(Function<T, T> mutation) {
        return transform(getRuleName(), mutation);
    }

    @SuppressWarnings("unchecked")
    protected List<AstTransform<T>> transform(String name, Function<T, T> mutation) {
        return transform((T) getCursor().getTree(), name, mutation);
    }

    protected List<AstTransform<T>> transform(T target, Function<T, T> mutation) {
        return transform(target, getRuleName(), mutation);
    }

    @SuppressWarnings("unchecked")
    protected List<AstTransform<T>> transform(T target, String name, Function<T, T> mutation) {
        return singletonList(new AstTransform<>(target.getId(), name, (Class<T>) target.getClass(), mutation));
    }
}
