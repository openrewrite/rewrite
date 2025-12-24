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

import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

public class DeclaresType<P> extends JavaIsoVisitor<P> {
    private final String type;
    private final boolean includeSubtypes;

    public DeclaresType(String type) {
        this(type, false);
    }

    public DeclaresType(String type, boolean includeSubtypes) {
        this.type = type;
        this.includeSubtypes = includeSubtypes;
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        if (classDecl.getType() != null) {
            if (includeSubtypes && TypeUtils.isAssignableTo(type, classDecl.getType()) ||
                    TypeUtils.isOfClassType(classDecl.getType(), type)) {
                return SearchResult.found(classDecl);
            }
        }
        return super.visitClassDeclaration(classDecl, p);
    }
}
