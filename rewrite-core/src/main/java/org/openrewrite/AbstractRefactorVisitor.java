package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class AbstractRefactorVisitor<T extends Tree> extends AbstractSourceVisitor<T> implements RefactorVisitor<T> {
    private final ThreadLocal<List<RefactorVisitor<T>>> andThen = new ThreadLocal<>();

    public AbstractRefactorVisitor() {
        andThen.set(new ArrayList<>());
    }

    /**
     * Used to build up pipelines of visitors.
     *
     * @return Other visitors that are run after this one.
     */
    public List<RefactorVisitor<T>> andThen() {
        return andThen.get();
    }

    /**
     * Used to build up pipelines of visitors.
     *
     * @param visitor The visitor to run after this visitor.
     */
    protected void andThen(RefactorVisitor<T> visitor) {
        andThen.get().add(visitor);
    }

    @SuppressWarnings("unchecked")
    protected <T1 extends Tree> T1 refactor(T1 t, Function<T1, Tree> callSuper) {
        return (T1) callSuper.apply(t);
    }

    @SuppressWarnings("unchecked")
    protected <T1 extends Tree> T1 refactor(@Nullable Tree tree) {
        return (T1) visit(tree);
    }

    protected <T1 extends Tree> List<T1> refactor(@Nullable List<T1> trees) {
        if(trees == null) {
            return null;
        }

        List<T1> mutatedTrees = new ArrayList<>(trees.size());
        boolean changed = false;
        for (T1 tree : trees) {
            T1 mutated = refactor(tree);
            if(mutated != tree) {
                changed = true;
            }
            mutatedTrees.add(mutated);
        }

        return changed ? mutatedTrees : trees;
    }

    public void next() {
        synchronized (this) {
            if (andThen.get() != null) {
                andThen.get().clear();
            } else {
                andThen.set(new ArrayList<>());
            }
        }
    }
}
