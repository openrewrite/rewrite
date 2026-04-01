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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

@AllArgsConstructor
public class FullyQualifyTypeReference<P> extends JavaVisitor<P> {
    private final JavaType.FullyQualified typeToFullyQualify;

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
            return identifier.withSimpleName(typeToFullyQualify.getFullyQualifiedName());
        }
        return super.visitIdentifier(identifier, p);
    }
}
