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
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Flag;
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
        findTypesInUse.visit(cu, cu);
        return new TypesInUse(cu,
                findTypesInUse.getTypes(),
                findTypesInUse.getDeclaredMethods(),
                findTypesInUse.getUsedMethods(),
                findTypesInUse.getVariables());
    }

    @Getter
    public static class FindTypesInUse extends JavaIsoVisitor<JavaSourceFile> {
        private final Set<JavaType> types = newSetFromMap(new IdentityHashMap<>());
        private final Set<JavaType.Method> declaredMethods = newSetFromMap(new IdentityHashMap<>());
        private final Set<JavaType.Method> usedMethods = newSetFromMap(new IdentityHashMap<>());
        private final Set<JavaType.Variable> variables = newSetFromMap(new IdentityHashMap<>());

        @Override
        public J.Import visitImport(J.Import _import, JavaSourceFile cu) {
            return _import;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, JavaSourceFile cu) {
            Object parent = Objects.requireNonNull(getCursor().getParent()).getValue();
            if (parent instanceof J.ClassDeclaration) {
                // skip type of class
                return identifier;
            } else if (parent instanceof J.MethodDeclaration && ((J.MethodDeclaration) parent).getName() == identifier) {
                // skip method name
                return identifier;
            }
            return super.visitIdentifier(identifier, cu);
        }

        @Override
        public @Nullable JavaType visitType(@Nullable JavaType javaType, JavaSourceFile cu) {
            if (javaType != null && !(javaType instanceof JavaType.Unknown)) {
                Cursor cursor = getCursor();
                if (javaType instanceof JavaType.Variable) {
                    JavaType.Variable jType = (JavaType.Variable) javaType;
                    variables.add(jType);
                    if (jType.getOwner() != null && jType.getOwner() instanceof JavaType.Class) {
                        JavaType.Class owner = (JavaType.Class) jType.getOwner();
                        String ownerPackage;
                        if (owner.getFullyQualifiedName().contains(".")) {
                            ownerPackage = owner.getFullyQualifiedName().substring(0, owner.getFullyQualifiedName().lastIndexOf("."));
                        } else {
                            ownerPackage = owner.getFullyQualifiedName();
                        }

                        // If we're accessing a variable that has the static flag and is not owned by the
                        // CompilationUnit we are visiting we should add the owning class of the variable as a used type
                        if (jType.getFlags().contains(Flag.Static)
                            && cu.getPackageDeclaration() != null && !ownerPackage.equals(cu.getPackageDeclaration().getPackageName())) {
                            types.add(jType.getOwner());
                        }
                    }
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
