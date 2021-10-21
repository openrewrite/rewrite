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
import org.openrewrite.java.tree.JavaType.FullyQualified;
import org.openrewrite.marker.SearchResult;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FindTypesImplementing extends Recipe {
    private final List<FullyQualified> interfaces;

    public FindTypesImplementing(List<FullyQualified> interfaces) {
        this.interfaces = interfaces;
    }

    private final UUID id = Tree.randomId();

    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                J.ClassDeclaration a = super.visitClassDeclaration(classDecl, executionContext);
                if(classDecl.getKind() == J.ClassDeclaration.Kind.Type.Class &&
                implementsInterface(classDecl)) {
                    a = a.withMarkers(a.getMarkers().addIfAbsent(new SearchResult(FindTypesImplementing.this.id, FindTypesImplementing.this)));
                }
                return a;
            }
        };
    }

    private boolean implementsInterface(J.ClassDeclaration classDecl) {
        if(classDecl.getImplements() == null) {
            return false;
        }

        List<String> fqns = interfaces.stream().map(fqn -> fqn.getFullyQualifiedName()).collect(Collectors.toList());

        return classDecl.getType().getInterfaces().stream()
                .filter(fqn -> !fqns.contains(fqn.getFullyQualifiedName()))
                .findFirst()
                .isEmpty();
    }

    @Override
    public String getDisplayName() {
        return "Find types implementing ";
    }
}
