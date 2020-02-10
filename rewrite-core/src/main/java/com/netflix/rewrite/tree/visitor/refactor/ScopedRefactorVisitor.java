package com.netflix.rewrite.tree.visitor.refactor;

import com.netflix.rewrite.tree.Tree;

import java.util.Spliterators;
import java.util.UUID;

import static java.util.stream.StreamSupport.stream;

public abstract class ScopedRefactorVisitor extends RefactorVisitor {
    protected final UUID scope;

    public ScopedRefactorVisitor(UUID scope) {
        this.scope = scope;
    }

    protected boolean isInScope(Tree t) {
        return t.getId().equals(scope) ||
                stream(Spliterators.spliteratorUnknownSize(getCursor().getPath(), 0), false)
                    .anyMatch(p -> p.getId().equals(scope));
    }
}
