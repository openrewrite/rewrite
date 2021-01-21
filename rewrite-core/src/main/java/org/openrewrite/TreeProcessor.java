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

/**
 * Abstract {@link TreeVisitor} for processing {@link Tree elements}
 *
 * Always returns input type T
 * provides Parameterizable P input which is mutable allowing context to be shared
 *
 * postProcessing via afterVisit for conditionally chaining other operations with the expectation is that after
 * TreeProcessors are invoked immediately after visiting SourceFile
 *
 * @param <T>
 * @param <P>
 */
public abstract class TreeProcessor<T extends Tree, P> implements TreeVisitor<T, P> {
    private static final boolean IS_DEBUGGING = System.getProperty("org.openrewrite.debug") != null ||
            ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

    private boolean cursored = IS_DEBUGGING;

    private final ThreadLocal<Cursor> cursor = new ThreadLocal<>();
    private final ThreadLocal<List<TreeProcessor<T, P>>> afterVisit = new ThreadLocal<>();

    protected final void setCursoringOn() {
        this.cursored = true;
    }

    protected void rebaseCursor(Tree t) {
        this.cursor.set(new Cursor(getCursor().getParent(), t));
    }

    // ephemeral do once after visit (sourceFile)
    protected void doAfterVisit(TreeProcessor<T, P> visitor) {
        afterVisit.get().add(visitor);
    }

    // ephemeral do once after visit
    protected void doAfterVisit(Recipe visitor) {
        //noinspection unchecked
        afterVisit.get().add((TreeProcessor<T, P>) visitor.getProcessor());
    }

    protected List<TreeProcessor<T, P>> getAfterVisit() {
        return afterVisit.get();
    }

    public final Cursor getCursor() {
        if (cursor.get() == null) {
            throw new IllegalStateException("Cursoring is not enabled for this processor. " +
                    "Call setCursoringOn() in the processor's constructor to enable.");
        }
        return cursor.get();
    }

    @Nullable
    public T visitEach(T tree, P p) {
        return defaultValue(tree, p);
    }

    @Nullable
    public T visit(@Nullable Tree tree, P p, Cursor parent) {
        this.cursor.set(parent);
        return visit(tree, p);
    }

    @Nullable
    public T visit(@Nullable Tree tree, P p) {
        if (tree == null) {
            return defaultValue(null, p);
        }

        boolean topLevel = false;
        if (afterVisit.get() == null) {
            topLevel = true;
            afterVisit.set(new ArrayList<>());
        }

        if (cursored) {
            cursor.set(new Cursor(cursor.get(), tree));
        }

        @SuppressWarnings("unchecked") T t = visitEach((T) tree, p);
        if (t == null) {
            afterVisit.remove();
            return defaultValue(null, p);
        }

        t = t.accept(this, p);

        if (cursored) {
            cursor.set(cursor.get().getParent());
        }

        if (topLevel) {
            for (TreeProcessor<T, P> v : afterVisit.get()) {
                t = v.visit(t, p);
            }
            afterVisit.remove();
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

    @Nullable
    protected <T2 extends T> T2 call(@Nullable Tree tree, P p) {
        //noinspection unchecked
        return (T2) visit(tree, p);
    }

    @Nullable
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
