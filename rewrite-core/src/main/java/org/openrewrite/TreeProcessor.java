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
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public abstract class TreeProcessor<T extends Tree, P> implements TreeVisitor<T, P> {
    private static final boolean IS_DEBUGGING = System.getProperty("org.openrewrite.debug") != null ||
            ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

    private boolean cursored = IS_DEBUGGING;

    private final ThreadLocal<Cursor> cursor = new ThreadLocal<>();
    private final ThreadLocal<List<TreeProcessor<T, P>>> next = new ThreadLocal<>();

    protected final void setCursoringOn() {
        this.cursored = true;
    }

    protected void rebaseCursor(Tree t) {
        this.cursor.set(new Cursor(getCursor().getParent(), t));
    }

    protected void doAfterVisit(TreeProcessor<T, P> visitor) {
        next.get().add(visitor);
    }

    protected void doAfterVisit(Recipe visitor) {
        //noinspection unchecked
        next.get().add((TreeProcessor<T, P>) visitor.getProcessor().get());
    }

    protected List<TreeProcessor<T, P>> getAfterVisit() {
        return next.get();
    }

    public final Cursor getCursor() {
        if (cursor.get() == null) {
            throw new IllegalStateException("Cursoring is not enabled for this visitor. " +
                    "Call setCursoringOn() in the visitor's constructor to enable.");
        }
        return cursor.get();
    }

    @Nullable
    public T visitEach(T tree, P p) {
        return defaultValue(tree, p);
    }

    @Nullable
    public final T visit(@Nullable Tree tree, P p) {
        return visitInternal(tree, p);
    }

    @Nullable
    private T visitInternal(Tree tree, P p) {
        if (tree == null) {
            return defaultValue(null, p);
        }

        boolean topLevel = false;
        if(next.get() == null) {
            topLevel = true;
            next.set(new ArrayList<>());
        }

        if (cursored) {
            cursor.set(new Cursor(cursor.get(), tree));
        }

        @SuppressWarnings("unchecked") T t = visitEach((T) tree, p);
        if(t == null) {
            next.remove();
            return defaultValue(null, p);
        }

        t = t.accept(this, p);

        if (cursored) {
            cursor.set(cursor.get().getParent());
        }

        if(topLevel) {
            for (TreeProcessor<T, P> v : next.get()) {
                t = v.visit(t, p);
            }
            next.remove();
        }

        return t;
    }

    @Nullable
    @Override
    public T defaultValue(@Nullable Tree tree, P p) {
        //noinspection unchecked
        return (T) tree;
    }

    protected <T2 extends Tree> T2 call(T2 t, P p, BiFunction<T2, P, Tree> callSuper) {
        //noinspection unchecked
        return (T2) callSuper.apply(t, p);
    }

    protected <T2 extends T> T2 call(@Nullable Tree tree, P p) {
        //noinspection unchecked
        return (T2) visit(tree, p);
    }

    protected <T2 extends T> List<T2> call(@Nullable List<T2> trees, P p) {
        if (trees == null) {
            return null;
        }

        List<T2> mutatedTrees = new ArrayList<>(trees.size());
        boolean changed = false;
        for (T2 tree : trees) {
            @SuppressWarnings("unchecked") T2 mutated = (T2) visit(tree, p);
            if (mutated != tree || mutated == null) {
                changed = true;
            }
            if (mutated != null) {
                mutatedTrees.add(mutated);
            }
        }

        return changed ? mutatedTrees : trees;
    }
}
