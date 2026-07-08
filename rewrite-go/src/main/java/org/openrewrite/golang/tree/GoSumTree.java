/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.golang.tree;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.golang.GoSumVisitor;

/**
 * Node family for the {@code go.sum} LST: {@link GoSum} and its lines.
 * <p>
 * Mirrors {@link GoModTree}: extending {@link Tree} gives every node
 * {@code Tree}'s {@code @c} polymorphic type id so the elements survive
 * {@code .lst} (de)serialization, and the {@code accept} dispatch routes
 * traversal to a {@link GoSumVisitor}. Printing remains delegated to the Go
 * RPC server.
 */
public interface GoSumTree extends Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> @Nullable R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptGoSum(v.adapt(GoSumVisitor.class), p);
    }

    default <P> @Nullable GoSumTree acceptGoSum(GoSumVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(GoSumVisitor.class);
    }
}
