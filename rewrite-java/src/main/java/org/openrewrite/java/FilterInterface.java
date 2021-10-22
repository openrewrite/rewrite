/*
 * Copyright 2021 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 *
 * @author: Alex Boyko
 */
package org.openrewrite.java;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

import java.util.UUID;

public class FilterInterface extends Recipe {

    private final UUID id = Tree.randomId();

    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                J.ClassDeclaration a = super.visitClassDeclaration(classDecl, executionContext);
                if(classDecl.getKind() == J.ClassDeclaration.Kind.Type.Interface) {
                    a = a.withMarkers(a.getMarkers().addIfAbsent(new SearchResult(FilterInterface.this.id, "FilterInterfaceVisitor")));
                }

                return a;
            }
        };
    }

    @Override
    public String getDisplayName() {
        return "Filter interfaces";
    }
}
