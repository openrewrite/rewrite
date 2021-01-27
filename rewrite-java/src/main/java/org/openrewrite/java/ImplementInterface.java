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

import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.format;

public class ImplementInterface<P> extends JavaIsoVisitor<P> {
    private final J.ClassDecl scope;
    private final JavaType.FullyQualified interfaceType;

    public ImplementInterface(J.ClassDecl scope, JavaType.FullyQualified interfaceType) {
        this.scope = scope;
        this.interfaceType = interfaceType;
    }

    public ImplementInterface(J.ClassDecl scope, String interfaze) {
        this(scope, JavaType.Class.build(interfaze));
    }

    @Override
    public J.ClassDecl visitClassDecl(J.ClassDecl classDecl, P p) {
        J.ClassDecl c = super.visitClassDecl(classDecl, p);
        if (c.isScope(scope) && (c.getImplements() == null ||
                c.getImplements().getElem().stream().noneMatch(f -> interfaceType.equals(f.getElem().getType())))) {
            maybeAddImport(interfaceType);

            J.Ident implementElement = J.Ident.build(
                    randomId(),
                    format(" "),
                    Markers.EMPTY,
                    interfaceType.getClassName(),
                    interfaceType
            );

            JContainer<TypeTree> implementsClause = c.getImplements();
            if (implementsClause == null) {
                implementsClause = JContainer.build(format(" "), emptyList(), Markers.EMPTY);
            }
            c = c.withImplements(implementsClause.insertElement(0, implementElement));
        }

        return c;
    }
}
