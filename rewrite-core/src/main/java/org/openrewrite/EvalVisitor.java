package org.openrewrite;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.internal.lang.Nullable;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public abstract class EvalVisitor<T extends Tree> implements TreeVisitor<T, EvalContext> {
    private static final boolean IS_DEBUGGING = System.getProperty("org.openrewrite.debug") != null ||
            ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

    private boolean cursored = IS_DEBUGGING;

    private final ThreadLocal<Cursor> cursor = new ThreadLocal<>();
    private final ThreadLocal<List<Task<? extends T>>> andThen = new ThreadLocal<>();

    /**
     * @return Other visitors that are run after this one.
     */
    public List<Task<? extends T>> onNext() {
        return andThen.get();
    }

    /**
     * Used to build up pipelines of visitors.
     *
     * @param visitor The visitor to run after this visitor.
     */
    protected void doOnNext(EvalVisitor<T> visitor) {
        doOnNext(visitor, null);
    }

    /**
     * Used to build up pipelines of visitors.
     *
     * @param visitor The visitor to run after this visitor.
     * @param cursor The cursor to start the next visitor at.
     */
    protected void doOnNext(EvalVisitor<T> visitor, @Nullable Cursor cursor) {
        andThen.get().add(new Task<>(visitor, cursor));
    }

    protected final void setCursoringOn() {
        this.cursored = true;
    }

    public void next() {
    }

    public boolean isIdempotent() {
        return true;
    }

    public final Cursor getCursor() {
        if (cursor.get() == null) {
            throw new IllegalStateException("Cursoring is not enabled for this visitor. " +
                    "Call setCursoringOn() in the visitor's constructor to enable.");
        }
        return cursor.get();
    }

    @Nullable
    public T visitEach(@Nullable T tree, EvalContext ctx) {
        return defaultValue(tree, ctx);
    }

    public final T scan(Tree tree, EvalContext ctx, Cursor cursor) {
        this.cursor.set(cursor);
        return visit(tree, ctx);
    }

    @Nullable
    public T visit(@Nullable Tree tree, EvalContext ctx) {
        if (tree == null) {
            return defaultValue(null, ctx);
        }

        if (cursored) {
            cursor.set(new Cursor(cursor.get(), tree));
        }

        T t = tree.accept(this, ctx);

        if (cursored) {
            cursor.set(cursor.get().getParent());
        }

        return t;
    }

    @Nullable
    @Override
    public T defaultValue(@Nullable Tree tree, EvalContext ctx) {
        //noinspection unchecked
        return (T) tree;
    }

    public Iterable<Tag> getTags() {
        return Tags.empty();
    }

    public Validated validate() {
        return Validated.none();
    }

    public String getName() {
        return getClass().getName();
    }

    protected <T2 extends Tree> T2 eval(T2 t, EvalContext ctx, BiFunction<T2, EvalContext, Tree> callSuper) {
        //noinspection unchecked
        return (T2) callSuper.apply(t, ctx);
    }

    protected <T2 extends T> T2 eval(@Nullable Tree tree, EvalContext ctx) {
        //noinspection unchecked
        return (T2) visit(tree, ctx);
    }

    protected <T2 extends T> List<T2> eval(@Nullable List<T2> trees, EvalContext ctx) {
        if (trees == null) {
            return null;
        }

        List<T2> mutatedTrees = new ArrayList<>(trees.size());
        boolean changed = false;
        for (T2 tree : trees) {
            @SuppressWarnings("unchecked") T2 mutated = (T2) visit(tree, ctx);
            if (mutated != tree || mutated == null) {
                changed = true;
            }
            if (mutated != null) {
                mutatedTrees.add(mutated);
            }
        }

        return changed ? mutatedTrees : trees;
    }

    public static class Task<T> {
        private final EvalVisitor<? extends T> next;

        @Nullable
        private final Cursor startingCursor;

        public Task(EvalVisitor<? extends T> next, @Nullable Cursor startingCursor) {
            this.next = next;
            this.startingCursor = startingCursor;
        }

        public EvalVisitor<? extends T> getNext() {
            return next;
        }

        @Nullable
        public Cursor getStartingCursor() {
            return startingCursor;
        }
    }
}
