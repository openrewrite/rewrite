/*
 * Copyright 2021 the original author or authors.
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

import lombok.AllArgsConstructor;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.List;

import static java.util.Collections.emptyList;

@AllArgsConstructor
public class FullyQualifyTypeReference<P> extends JavaVisitor<P> {
    private final JavaType.FullyQualified typeToFullyQualify;

    /**
     * The fully qualified name of a type as the parser makes it of that text: a field access on each package and
     * outer type, with the type on the last name.
     */
    static Expression qualifiedName(JavaType.FullyQualified type, Space prefix, List<J.Annotation> annotations) {
        String name = type.getFullyQualifiedName();
        if (name.indexOf('.') < 0 && name.indexOf('$') < 0) {
            return new J.Identifier(Tree.randomId(), prefix, Markers.EMPTY, annotations, name, type, null);
        }
        J.FieldAccess qualified = TypeTree.build(name);
        return qualified.withPrefix(prefix)
                .withType(type)
                .withName(qualified.getName().withAnnotations(annotations).withType(type));
    }

    @Override
    public J visitFieldAccess(J.FieldAccess fieldAccess, P p) {
        if (fieldAccess.isFullyQualifiedClassReference(typeToFullyQualify.getFullyQualifiedName())) {
            return fieldAccess;
        }
        return super.visitFieldAccess(fieldAccess, p);
    }

    @Override
    public J visitIdentifier(J.Identifier identifier, P p) {
        if (identifier.getFieldType() != null) {
            return super.visitIdentifier(identifier, p);
        }
        JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(identifier.getType());
        if (fullyQualified != null && typeToFullyQualify.getFullyQualifiedName().equals(fullyQualified.getFullyQualifiedName())) {
            return qualifiedName(fullyQualified, identifier.getPrefix(), identifier.getAnnotations());
        }
        return super.visitIdentifier(identifier, p);
    }
}
