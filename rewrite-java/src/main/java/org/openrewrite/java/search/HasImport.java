package org.openrewrite.java.search;

import lombok.RequiredArgsConstructor;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaSourceVisitor;
import org.openrewrite.java.tree.J;

@RequiredArgsConstructor
public class HasImport extends JavaSourceVisitor<Boolean> {
    private final String clazz;

    @Override
    public Boolean defaultTo(Tree t) {
        return false;
    }

    @Override
    public Boolean visitImport(J.Import impoort) {
        return impoort.isFromType(clazz);
    }
}
