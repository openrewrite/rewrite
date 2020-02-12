package com.netflix.rewrite.tree.visitor.refactor;

import com.netflix.rewrite.internal.lang.Nullable;
import com.netflix.rewrite.tree.Tree;
import lombok.RequiredArgsConstructor;

import java.util.Spliterators;
import java.util.UUID;

import static java.util.stream.StreamSupport.stream;

@RequiredArgsConstructor
public abstract class ScopedRefactorVisitor extends RefactorVisitor {
    protected final UUID scope;

    protected boolean isInScope(@Nullable Tree t) {
        return (t != null && t.getId().equals(scope)) ||
                stream(Spliterators.spliteratorUnknownSize(getCursor().getPath(), 0), false)
                    .anyMatch(p -> p.getId().equals(scope));
    }
}
