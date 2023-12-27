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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.format;

public class ImplementInterface<P> extends JavaIsoVisitor<P> {
    private final J.ClassDeclaration scope;
    private final JavaType.FullyQualified interfaceType;
    private final @Nullable List<Expression> typeParameters;

    public ImplementInterface(J.ClassDeclaration scope, JavaType.FullyQualified interfaceType, @Nullable List<Expression> typeParameters) {
        this.scope = scope;
        this.interfaceType = interfaceType;
        this.typeParameters = typeParameters;
    }

    public ImplementInterface(J.ClassDeclaration scope, String interface_, @Nullable List<Expression> typeParameters) {
        this(scope, ListUtils.nullIfEmpty(typeParameters) != null ?
                new JavaType.Parameterized(null, JavaType.ShallowClass.build(interface_), typeParameters.stream().map(Expression::getType).collect(Collectors.toList()))
                : JavaType.ShallowClass.build(interface_),
                typeParameters
        );
    }

    public ImplementInterface(J.ClassDeclaration scope, JavaType.FullyQualified interfaceType) {
        this(scope, interfaceType, null);
    }

    public ImplementInterface(J.ClassDeclaration scope, String interface_) {
        this(scope, interface_, null);
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

            if (typeParameters != null && !typeParameters.isEmpty()) {
                typeParameters.stream()
                        .map(Expression::getType)
                        .map(t -> (t instanceof JavaType.FullyQualified) ? (JavaType.FullyQualified) t : null)
                        .filter(Objects::nonNull)
                        .forEach(t -> maybeAddImport(t.getFullyQualifiedName()));

                List<JRightPadded<Expression>> elements = typeParameters.stream()
                        .map(t -> new JRightPadded<>(t, Space.EMPTY, Markers.EMPTY))
                        .collect(Collectors.toList());

                J.ParameterizedType typedImpl = new J.ParameterizedType(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        interfaceType instanceof JavaType.Parameterized ? impl.withType(((JavaType.Parameterized) interfaceType).getType()) : impl,
                        JContainer.build(Space.EMPTY, elements, Markers.EMPTY),
                        interfaceType
                );

                c = c.withImplements(ListUtils.concat(c.getImplements(), typedImpl));
            } else {
                c = c.withImplements(ListUtils.concat(c.getImplements(), impl));
            }

            JContainer<TypeTree> anImplements = c.getPadding().getImplements();
            assert anImplements != null;
            if (anImplements.getBefore().getWhitespace().isEmpty()) {
                c = c.getPadding().withImplements(anImplements.withBefore(Space.format(" ")));
            }
        }

        return c;
    }
}
