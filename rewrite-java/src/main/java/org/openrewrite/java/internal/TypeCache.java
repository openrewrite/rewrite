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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class TypeCache {
    private final J.CompilationUnit cu;
    private final Set<JavaType> typesInUse;
    private final Set<JavaType.Method> declaredMethods;

    public static TypeCache build(J.CompilationUnit cu) {
        Set<JavaType> types = new HashSet<JavaType>() {
            @Override
            public boolean add(@Nullable JavaType javaType) {
                if (javaType != null) {
                    return super.add(javaType);
                }
                return false;
            }
        };

        Set<JavaType.Method> declaredMethods = new HashSet<JavaType.Method>() {
            @Override
            public boolean add(@Nullable JavaType.Method javaType) {
                if (javaType != null) {
                    return super.add(javaType);
                }
                return false;
            }
        };

        new JavaIsoVisitor<Integer>() {
            @Override
            public J preVisit(J tree, Integer integer) {
                if (tree instanceof TypedTree) {
                    if (!(tree instanceof J.ClassDeclaration) &&
                            !(tree instanceof J.MethodDeclaration) &&
                            !(tree instanceof J.VariableDeclarations)) {
                        types.add(((TypedTree) tree).getType());
                    }
                }
                return tree;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration c, Integer p) {
                visitSpace(c.getPrefix(), Space.Location.ANY, p);
                for (J.Annotation annotation : c.getAllAnnotations()) {
                    visit(annotation, p);
                }
                if (c.getPadding().getTypeParameters() != null) {
                    visitContainer(c.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, p);
                }
                if (c.getPadding().getExtends() != null) {
                    visitLeftPadded(c.getPadding().getExtends(), JLeftPadded.Location.EXTENDS, p);
                }
                if (c.getPadding().getImplements() != null) {
                    visitContainer(c.getPadding().getImplements(), JContainer.Location.IMPLEMENTS, p);
                }
                visit(c.getBody(), p);
                return c;
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                types.add(identifier.getFieldType());
                return super.visitIdentifier(identifier, p);
            }

            @Override
            public J.Import visitImport(J.Import impoort, Integer p) {
                return impoort;
            }

            @Override
            public J.Package visitPackage(J.Package pkg, Integer p) {
                for (J.Annotation annotation : pkg.getAnnotations()) {
                    visit(annotation, p);
                }
                return pkg;
            }

            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, Integer p) {
                types.add(memberRef.getReferenceType());
                return super.visitMemberReference(memberRef, p);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Integer p) {
                declaredMethods.add(method.getType());
                return super.visitMethodDeclaration(method, p);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                types.add(method.getReturnType());
                return super.visitMethodInvocation(method, p);
            }
        }.visit(cu, 0);

        return new TypeCache(cu, types, declaredMethods);
    }
}
