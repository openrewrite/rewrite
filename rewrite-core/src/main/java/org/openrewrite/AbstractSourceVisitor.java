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

import java.lang.management.ManagementFactory;
import java.util.List;

public abstract class AbstractSourceVisitor<R> implements SourceVisitor<R> {
    private static final boolean IS_DEBUGGING = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

    private boolean cursored = IS_DEBUGGING;

    private final ThreadLocal<Cursor> cursor = new ThreadLocal<>();

    protected void setCursoringOn() {
        this.cursored = true;
    }

    protected final R visitAfter(R r, @Nullable Tree tree) {
        return tree == null ? r : reduce(r, visit(tree));
    }

    protected final R visitAfter(R r, @Nullable List<? extends Tree> trees) {
        return reduce(r, visit(trees));
    }

    @Override
    public Cursor getCursor() {
        if (cursor.get() == null) {
            throw new IllegalStateException("Cursoring is not enabled for this visitor. " +
                    "Call setCursoringOn() in the visitor's constructor to enable.");
        }
        return cursor.get();
    }

    public final R visit(@Nullable Tree tree) {
        if (tree == null) {
            return defaultTo(null);
        }

        if (cursored) {
            cursor.set(new Cursor(cursor.get(), tree));
        }

        R t = reduce(tree.accept(this), visitTree(tree));

        if (cursored) {
            cursor.set(cursor.get().getParent());
        }

        return t;
    }
}
