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
 * <p>
 * Always returns input type T
 * provides Parameterizable P input which is mutable allowing context to be shared
 * <p>
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

    private Cursor cursor;
    private List<TreeVisitor<T, P>> afterVisit;

    protected final void setCursoringOn() {
        this.cursored = true;
        setCursor(new Cursor(null, "root"));
    }

    protected void setCursor(@Nullable Cursor cursor) {
        this.cursor = cursor;
    }

    /**
     * Execute the visitor once after the whole source file has been visited.
     * The visitor is executed against the whole source file. This operation only happens once
     * immediately after the containing visitor visits the whole source file. A subsequent {@link Recipe}
     * cycle will not run it.
     * <p>
     * This method is ideal for one-off operations like auto-formatting, adding/removing imports, etc.
     *
     * @param visitor The visitor to run.
     */
    protected void doAfterVisit(TreeVisitor<T, P> visitor) {
        afterVisit.add(visitor);
    }

    /**
     * Execute the recipe's main visitor once after the whole source file has been visited.
     * The visitor is executed against the whole source file. This operation only happens once
     * immediately after the containing visitor visits the whole source file. A subsequent {@link Recipe}
     * cycle will not run it.
     * <p>
     * This method is ideal for one-off operations like auto-formatting, adding/removing imports, etc.
     *
     * @param recipe The recipe whose visitor to run.
     */
    @Incubating(since = "7.0.0")
    protected void doAfterVisit(Recipe recipe) {
        //noinspection unchecked
        afterVisit.add((TreeVisitor<T, P>) recipe.getVisitor());
    }

    protected List<TreeVisitor<T, P>> getAfterVisit() {
        return afterVisit;
    }

    public final Cursor getCursor() {
        if (cursor == null) {
            throw new IllegalStateException("Cursoring is not enabled for this visitor. " +
                    "Call setCursoringOn() in the visitor's constructor to enable.");
        }
        return cursor;
    }

    @Nullable
    public T preVisit(T tree, P p) {
        return defaultValue(tree, p);
    }

    @Nullable
    public T postVisit(T tree, P p) {
        return defaultValue(tree, p);
    }

    @Nullable
    public T visit(@Nullable Tree tree, P p, Cursor parent) {
        this.cursor = parent;
        return visit(tree, p);
    }

    @Nullable
    public T visit(@Nullable Tree tree, P p) {
        if (tree == null) {
            return defaultValue(null, p);
        }

        boolean topLevel = false;
        if (afterVisit == null) {
            topLevel = true;
            afterVisit = new ArrayList<>();
        }

        if (cursored) {
            setCursor(new Cursor(cursor, tree));
        }

        @SuppressWarnings("unchecked") T t = preVisit((T) tree, p);
        if (t != null) {
            t = t.accept(this, p);
        }
        if (t != null) {
            t = postVisit(t, p);
        }

        if (cursored) {
            setCursor(cursor.getParent());
        }

        if (topLevel) {
            if (t != null) {
                for (TreeVisitor<T, P> v : afterVisit) {
                    t = v.visit(t, p);
                }
            }
            afterVisit = null;
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
