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
import org.openrewrite.golang.GoModVisitor;

/**
 * Node family for the {@code go.mod} LST: {@link GoMod} and its statements/values.
 * <p>
 * Mirrors {@code org.openrewrite.hcl.tree.Hcl} (a standalone, non-{@code J} LST):
 * extending {@link Tree} gives every node {@code Tree}'s {@code @c} polymorphic type
 * id so the elements survive {@code .lst} (de)serialization, and the {@code accept}
 * dispatch routes traversal to a {@link GoModVisitor} just as {@code Hcl} routes to
 * {@code HclVisitor}. Printing remains delegated to the Go RPC server.
 */
public interface GoModTree extends Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> @Nullable R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptGoMod(v.adapt(GoModVisitor.class), p);
    }

    default <P> @Nullable GoModTree acceptGoMod(GoModVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(GoModVisitor.class);
    }
}
