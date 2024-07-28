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
package org.openrewrite.java.internal;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;

import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.newSetFromMap;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class TypesInUse {
    private final JavaSourceFile cu;
    private final Set<JavaType> typesInUse;
    private final Set<JavaType.Method> declaredMethods;
    private final Set<JavaType.Method> usedMethods;
    private final Set<JavaType.Variable> variables;

    public static TypesInUse build(JavaSourceFile cu) {
        FindTypesInUse findTypesInUse = new FindTypesInUse();
        findTypesInUse.visit(cu, 0);
        return new TypesInUse(cu,
                findTypesInUse.getTypes(),
                findTypesInUse.getDeclaredMethods(),
                findTypesInUse.getUsedMethods(),
                findTypesInUse.getVariables());
    }

    @Getter
    public static class FindTypesInUse extends JavaIsoVisitor<Integer> {
        private final Set<JavaType> types = newSetFromMap(new IdentityHashMap<>());
        private final Set<JavaType.Method> declaredMethods = newSetFromMap(new IdentityHashMap<>());
        private final Set<JavaType.Method> usedMethods = newSetFromMap(new IdentityHashMap<>());
        private final Set<JavaType.Variable> variables = newSetFromMap(new IdentityHashMap<>());

        @Override
        public J.Import visitImport(J.Import _import, Integer p) {
            return _import;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
            Object parent = Objects.requireNonNull(getCursor().getParent()).getValue();
            if (parent instanceof J.ClassDeclaration) {
                // skip type of class
                return identifier;
            } else if (parent instanceof J.MethodDeclaration && ((J.MethodDeclaration) parent).getName() == identifier) {
                // skip method name
                return identifier;
            }
            return super.visitIdentifier(identifier, p);
        }

        @Override
        public @Nullable JavaType visitType(@Nullable JavaType javaType, Integer p) {
            if (javaType != null && !(javaType instanceof JavaType.Unknown)) {
                Cursor cursor = getCursor();
                if (javaType instanceof JavaType.Variable) {
                    variables.add((JavaType.Variable) javaType);
                } else if (javaType instanceof JavaType.Method) {
                    if (cursor.getValue() instanceof J.MethodDeclaration) {
                        declaredMethods.add((JavaType.Method) javaType);
                    } else {
                        usedMethods.add((JavaType.Method) javaType);
                    }
                } else if (!(cursor.getValue() instanceof J.ClassDeclaration) && !(cursor.getValue() instanceof J.Lambda)) {
                    // ignore type representing class declaration itself and inferred lambda types
                    types.add(javaType);
                }
            }
            return javaType;
        }
    }
}
