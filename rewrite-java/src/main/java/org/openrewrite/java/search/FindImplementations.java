/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.search;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

public class FindImplementations extends Recipe {
    private final String interfaceFullyQualifiedName;

    public FindImplementations(String interfaceFullyQualifiedName) {
        this.interfaceFullyQualifiedName = interfaceFullyQualifiedName;
    }

    @Override
    public String getDisplayName() {
        return "Find class declarations implementing an interface";
    }

    @Override
    public String getDescription() {
        return "Find source files that contain a class declaration implementing a specific interface.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl,
                                                            ExecutionContext ctx) {
                classDecl = super.visitClassDeclaration(classDecl, ctx);

                if (!TypeUtils.isOfClassType(classDecl.getType(), interfaceFullyQualifiedName) &&
                    TypeUtils.isAssignableTo(interfaceFullyQualifiedName, classDecl.getType())) {
                    return SearchResult.found(classDecl);
                }
                return classDecl;
            }
        };
    }
}
