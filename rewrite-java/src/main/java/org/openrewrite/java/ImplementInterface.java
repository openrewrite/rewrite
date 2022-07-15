/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java;

import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.format;

public class ImplementInterface<P> extends JavaIsoVisitor<P> {
    private final J.ClassDeclaration scope;
    private final JavaType.FullyQualified interfaceType;

    public ImplementInterface(J.ClassDeclaration scope, JavaType.FullyQualified interfaceType) {
        this.scope = scope;
        this.interfaceType = interfaceType;
    }

    public ImplementInterface(J.ClassDeclaration scope, String interfaze) {
        this(scope, JavaType.ShallowClass.build(interfaze));
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);
        if (c.isScope(scope) && (c.getImplements() == null || c.getImplements().stream()
                .noneMatch(f -> TypeUtils.isAssignableTo(f.getType(), interfaceType)))) {

            if (!classDecl.getSimpleName().equals(interfaceType.getClassName())) {
                maybeAddImport(interfaceType);
            }

            TypeTree impl = TypeTree.build(classDecl.getSimpleName().equals(interfaceType.getClassName()) ?
                    interfaceType.getFullyQualifiedName() : interfaceType.getClassName())
                    .withType(interfaceType)
                    .withPrefix(format(" "));

            c = c.withImplements(ListUtils.concat(c.getImplements(), impl));

            JContainer<TypeTree> anImplements = c.getPadding().getImplements();
            assert anImplements != null;
            if (anImplements.getBefore().getWhitespace().isEmpty()) {
                c = c.getPadding().withImplements(anImplements.withBefore(Space.format(" ")));
            }
        }

        return c;
    }
}
