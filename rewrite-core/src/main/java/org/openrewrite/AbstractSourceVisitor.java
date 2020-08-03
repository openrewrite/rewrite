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
