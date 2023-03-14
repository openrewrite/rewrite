package org.openrewrite.java.search;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.marker.SearchResult;

import java.util.Objects;

public class FindImplements extends Recipe {
    private final String interfaceFullyQualifiedName;

    public FindImplements(String interfaceFullyQualifiedName) {
        this.interfaceFullyQualifiedName = interfaceFullyQualifiedName;
    }

    @Override
    public String getDisplayName() {
        return "Find class declaration with implements";
    }

    @Override
    public String getDescription() {
        return "Find source files that contain a class declaration implementing a specific interface.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl,
                                                            ExecutionContext ctx) {
                classDecl = super.visitClassDeclaration(classDecl, ctx);
                if (classDecl.getImplements() != null) {
                    for (TypeTree impl : classDecl.getImplements()) {
                        if (interfaceFullyQualifiedName.equals(Objects.requireNonNull(impl.getType()).toString())) {
                            return SearchResult.found(classDecl);
                        }
                    }
                }
                return classDecl;
            }
        };
    }
}
