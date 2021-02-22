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
package org.openrewrite.java.search;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptySet;

public class FindInheritedFields {
    private FindInheritedFields() {
    }

    public static Set<JavaType.Variable> find(J j, String clazz) {
        Set<JavaType.Variable> fields = new HashSet<>();
        new FindInheritedFieldsVisitor(clazz).visit(j, fields);
        return fields;
    }

    private static class FindInheritedFieldsVisitor extends JavaIsoVisitor<Set<JavaType.Variable>> {
        private final String fullyQualifiedName;

        public FindInheritedFieldsVisitor(String fullyQualifiedName) {
            this.fullyQualifiedName = fullyQualifiedName;
        }

        private Set<JavaType.Variable> superFields(@Nullable JavaType.Class type) {
            if (type == null || type.getSupertype() == null) {
                return emptySet();
            }
            Set<JavaType.Variable> types = new HashSet<>();
            type.getMembers().stream()
                    .filter(m -> !m.hasFlags(Flag.Private) && TypeUtils.hasElementType(m.getType(), fullyQualifiedName))
                    .forEach(types::add);
            types.addAll(superFields(type.getSupertype()));
            return types;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Set<JavaType.Variable> ctx) {
            JavaType.Class asClass = TypeUtils.asClass(classDecl.getType());
            ctx.addAll(superFields(asClass == null ? null : asClass.getSupertype()));
            return super.visitClassDeclaration(classDecl, ctx);
        }
    }
}
