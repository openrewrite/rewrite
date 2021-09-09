/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.polyglot;

import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;

import java.util.UUID;

public class PolyglotTree implements Tree {

    @Nullable
    private final Tree delegate;

    public PolyglotTree(@Nullable Tree delegate) {
        this.delegate = delegate;
    }

    @Override
    public UUID getId() {
        return delegate != null ? delegate.getId() : UUID.randomUUID();
    }

    @Override
    public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v instanceof PolyglotVisitor;
    }

    @Override
    public <R extends Tree, P> @Nullable R accept(TreeVisitor<R, P> v, P p) {
        return Tree.super.accept(v, p);
    }

    @Override
    public <P> String print(TreePrinter<P> printer, P p) {
        return new PolyglotPrinter<P>(printer).print(this, p);
    }

}
