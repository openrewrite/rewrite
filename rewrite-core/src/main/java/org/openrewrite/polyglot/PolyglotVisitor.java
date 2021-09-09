package org.openrewrite.polyglot;

import org.graalvm.polyglot.Value;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;

import static org.openrewrite.polyglot.PolyglotUtils.invokeMember;

public class PolyglotVisitor<T> extends TreeVisitor<PolyglotTree, T> {

    private final Value value;
    private final TreeVisitor<? extends Tree, T> delegate;

    public PolyglotVisitor(Value value, TreeVisitor<? extends Tree, T> delegate) {
        this.value = value;
        this.delegate = delegate;
    }

    @Override
    public @Nullable PolyglotTree visit(@Nullable Tree tree, T ctx) {
        return invokeMember(value, "visit", new PolyglotTree(tree), ctx)
                .map(v -> v.as(PolyglotTree.class))
                .orElseGet(() -> new PolyglotTree(delegate.visit(tree, ctx)));
    }

}
