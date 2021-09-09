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
