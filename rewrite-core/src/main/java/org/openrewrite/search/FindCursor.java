package org.openrewrite.search;

import org.openrewrite.Cursor;
import org.openrewrite.SourceVisitor;
import org.openrewrite.Tree;

public class FindCursor extends SourceVisitor<Cursor> {
    private final Tree t;

    public FindCursor(Tree t) {
        this.t = t;
        setCursoringOn();
    }

    @Override
    public Cursor defaultTo(Tree t) {
        return null;
    }

    @Override
    public Cursor visitTree(Tree tree) {
        if (tree != null && t.getId().equals(tree.getId())) {
            return getCursor();
        }

        return super.visitTree(tree);
    }
}
