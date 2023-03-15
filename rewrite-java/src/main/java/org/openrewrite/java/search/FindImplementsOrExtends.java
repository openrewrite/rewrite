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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.marker.SearchResult;

public class FindImplementsOrExtends extends Recipe {
    private final String interfaceFullyQualifiedName;

    public FindImplementsOrExtends(String interfaceFullyQualifiedName) {
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
                        if (findInterface(impl.getType())) {
                            return SearchResult.found(classDecl);
                        }
                    }
                }

                TypeTree ext = classDecl.getExtends();
                if (ext != null) {
                    JavaType.Class jc = (JavaType.Class) ext.getType();
                    if (findInterface(jc)) {
                        return SearchResult.found(classDecl);
                    }
                }
                return classDecl;
            }
        };
    }

    private boolean findInterface(@Nullable JavaType javaType) {
        if (javaType == null) {
            return false;
        }

        if (javaType instanceof JavaType.Class) {
            JavaType.Class jc = (JavaType.Class) javaType;
            if (jc.toString().equals(interfaceFullyQualifiedName)) {
                return true;
            }

            for (JavaType.FullyQualified it : jc.getInterfaces()) {
                if (it.toString().equals(interfaceFullyQualifiedName)) {
                    return true;
                }

                if (findInterface(it)) {
                    return true;
                }
            }
            return false;
        } else if (javaType instanceof JavaType.Parameterized) {
            JavaType.Parameterized jp = (JavaType.Parameterized) javaType;
            if (jp.toString().equals(interfaceFullyQualifiedName)) {
                return true;
            }
            return findInterface(jp.getType());
        }
        return false;
    }
}
