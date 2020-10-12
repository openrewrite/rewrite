package org.openrewrite.java;

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TreeBuilder;

public class ChangePackageName extends JavaRefactorVisitor {
    private String newPackageName;

    public void setNewPackageName(final String newPackageName) {
        this.newPackageName = newPackageName;
    }

    @Override
    public J.Package visitPackage(J.Package pkg) {
        return pkg.withExpr(TreeBuilder.buildName(this.newPackageName).withPrefix(pkg.getPrefix().concat(" ")));
    }
}
