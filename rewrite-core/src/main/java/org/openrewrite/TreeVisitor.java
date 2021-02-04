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
 * TreeVisitors are invoked immediately after visiting SourceFile
 *
 * @param <T>
 * @param <P>
 */
public abstract class TreeVisitor<T extends Tree, P> {
    private static final boolean IS_DEBUGGING = System.getProperty("org.openrewrite.debug") != null ||
            ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

    protected boolean cursored = IS_DEBUGGING;

    private final ThreadLocal<Cursor> cursor = new ThreadLocal<>();
    private final ThreadLocal<List<TreeVisitor<T, P>>> afterVisit = new ThreadLocal<>();

    protected final void setCursoringOn() {
        this.cursored = true;
        cursor.set(new Cursor(null, "root"));
    }

    protected void setCursor(@Nullable Cursor cursor) {
        this.cursor.set(cursor);
    }

    // ephemeral do once after visit (sourceFile)
    protected void doAfterVisit(TreeVisitor<T, P> visitor) {
        afterVisit.get().add(visitor);
    }

    // ephemeral do once after visit
    protected void doAfterVisit(Recipe visitor) {
        //noinspection unchecked
        afterVisit.get().add((TreeVisitor<T, P>) visitor.getVisitor());
    }

    protected List<TreeVisitor<T, P>> getAfterVisit() {
        return afterVisit.get();
    }

    public final Cursor getCursor() {
        if (cursor.get() == null) {
            throw new IllegalStateException("Cursoring is not enabled for this visitor. " +
                    "Call setCursoringOn() in the visitor's constructor to enable.");
        }
        return cursor.get();
    }

    @Nullable
    public T preVisit(T tree, P p) {
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

        @SuppressWarnings("unchecked") T t = preVisit((T) tree, p);
        if (t == null) {
            afterVisit.remove();
            return defaultValue(null, p);
        }

        t = t.accept(this, p);

        if (cursored) {
            cursor.set(cursor.get().getParent());
        }

        if (topLevel) {
            for (TreeVisitor<T, P> v : afterVisit.get()) {
                t = v.visit(t, p);
            }
            afterVisit.remove();
        }

        return t;
    }

    @Nullable
    public T defaultValue(@Nullable Tree tree, P p) {
        //noinspection unchecked
        return (T) tree;
    }

    @Incubating(since = "7.0.0")
    protected <T2 extends Tree> T2 visitAndCast(T2 t, P p, BiFunction<T2, P, Tree> callSuper) {
        //noinspection unchecked
        return (T2) callSuper.apply(t, p);
    }

    @Incubating(since = "7.0.0")
    @Nullable
    protected <T2 extends T> T2 visitAndCast(@Nullable Tree tree, P p) {
        //noinspection unchecked
        return (T2) visit(tree, p);
    }
}
