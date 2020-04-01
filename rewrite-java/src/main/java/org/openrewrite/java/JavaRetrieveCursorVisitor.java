package org.openrewrite.java;

import lombok.Getter;
import org.openrewrite.Cursor;
import org.openrewrite.ScopedVisitorSupport;
import org.openrewrite.Tree;

import java.util.UUID;

public class JavaRetrieveCursorVisitor extends JavaSourceVisitor<Cursor> implements ScopedVisitorSupport {
    @Getter
    private final UUID scope;

    public JavaRetrieveCursorVisitor(UUID scope) {
        this.scope = scope;
    }

    @Override
    public boolean isCursored() {
        return true;
    }

    @Override
    public Cursor defaultTo(Tree t) {
        return null;
    }

    @Override
    public Cursor visitTree(Tree tree) {
        if (tree.getId().equals(getScope()))
            return getCursor();
        return super.visitTree(tree);
    }
}