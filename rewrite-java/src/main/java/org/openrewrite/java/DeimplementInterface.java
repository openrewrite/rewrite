/*
 * Copyright 2022 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = false)
public class DeimplementInterface<P> extends JavaIsoVisitor<P> {
    String fullyQualifiedInterfaceName;

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        maybeRemoveImport(fullyQualifiedInterfaceName);
        return super.visitClassDeclaration(classDecl, p).withImplements(ListUtils.map(classDecl.getImplements(), impl ->
                TypeUtils.isOfClassType(impl.getType(), fullyQualifiedInterfaceName) ? null : impl));
    }

    @Override
    public @Nullable J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        if (method.getMethodType() != null && method.getMethodType().isInheritedFrom(fullyQualifiedInterfaceName)) {
            //noinspection ConstantConditions
            return null;
        }
        return super.visitMethodDeclaration(method, p);
    }
}
