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

        private Set<JavaType.Variable> superFields(@Nullable JavaType.FullyQualified type) {
            if (type == null || type.getSupertype() == null) {
                return emptySet();
            }
            Set<JavaType.Variable> types = new HashSet<>();
            for (JavaType.Variable m : type.getMembers()) {
                if (!m.hasFlags(Flag.Private) && hasElementTypeAssignable(m.getType(), fullyQualifiedName)) {
                    types.add(m);
                }
            }
            types.addAll(superFields(type.getSupertype()));
            return types;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Set<JavaType.Variable> ctx) {
            ctx.addAll(superFields(classDecl.getType() == null ? null : classDecl.getType().getSupertype()));
            return super.visitClassDeclaration(classDecl, ctx);
        }
    }

    private static boolean hasElementTypeAssignable(@Nullable JavaType type, String fullyQualifiedName) {
        if (type instanceof JavaType.Array) {
            return hasElementTypeAssignable(((JavaType.Array) type).getElemType(), fullyQualifiedName);
        } else if (type instanceof JavaType.Class) {
            return TypeUtils.isAssignableTo(JavaType.ShallowClass.build(fullyQualifiedName), type);
        } else if (type instanceof JavaType.GenericTypeVariable) {
            JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) type;
            for (JavaType bound : generic.getBounds()) {
                if (hasElementTypeAssignable(bound, fullyQualifiedName)) {
                    return true;
                }
            }
        } else if(type instanceof JavaType.Parameterized) {
            JavaType.Parameterized parameterized = (JavaType.Parameterized) type;
            return hasElementTypeAssignable(parameterized.getType(), fullyQualifiedName);
        }
        return false;
    }
}
