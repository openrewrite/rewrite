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

import org.openrewrite.Tree;
import org.openrewrite.java.tree.*;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;

/**
 * Assumes imports are ordered. Only meant to be used by {@link OrderImports}.
 */
class RemoveUnusedImports extends JavaRefactorVisitor {
    private final int classCountToUseStarImport;
    private final int nameCountToUseStarImport;

    RemoveUnusedImports(int classCountToUseStarImport, int nameCountToUseStarImport) {
        this.classCountToUseStarImport = classCountToUseStarImport;
        this.nameCountToUseStarImport = nameCountToUseStarImport;
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu) {
        Map<String, Set<JavaType.Class>> typesByPackage = new TypesByPackage().visit(cu);
        Map<String, Set<String>> methodsByTypeName = new MethodsByType().visit(cu);

        boolean changed = false;
        final List<J.Import> importsWithoutUnused = new ArrayList<>();

        for (J.Import anImport : cu.getImports()) {
            if (anImport.isStatic()) {
                Set<String> methods = methodsByTypeName.get(anImport.getTypeName());
                if (methods == null) {
                    changed = true;
                    continue;
                }
                if ("*".equals(anImport.getQualid().getSimpleName())) {
                    if (methods.size() < nameCountToUseStarImport) {
                        methods.stream().sorted().forEach(method -> {
                            importsWithoutUnused.add(anImport.withQualid(anImport.getQualid().withName(anImport.getQualid().getName()
                                    .withName(method))));
                        });
                        changed = true;
                    } else {
                        importsWithoutUnused.add(anImport);
                    }
                } else {
                    importsWithoutUnused.add(anImport);
                }
            } else {
                Set<JavaType.Class> types = typesByPackage.get(anImport.getPackageName());
                if (types == null) {
                    changed = true;
                    continue;
                }
                if ("*".equals(anImport.getQualid().getSimpleName())) {
                    if (types.size() < classCountToUseStarImport) {
                        types.stream().map(JavaType.FullyQualified::getClassName).sorted().forEach(typeClassName -> {
                            importsWithoutUnused.add(anImport.withQualid(anImport.getQualid().withName(anImport.getQualid().getName()
                                    .withName(typeClassName))));
                        });
                        changed = true;
                    } else {
                        importsWithoutUnused.add(anImport);
                    }
                } else {
                    importsWithoutUnused.add(anImport);
                }
            }
        }

        return changed ? cu.withImports(importsWithoutUnused) : cu;
    }

    static class TypesByPackage extends JavaSourceVisitor<Map<String, Set<JavaType.Class>>> {
        TypesByPackage() {
            setCursoringOn();
        }

        @Override
        public Map<String, Set<JavaType.Class>> reduce(Map<String, Set<JavaType.Class>> r1,
                                                       Map<String, Set<JavaType.Class>> r2) {
            if (r1.isEmpty()) {
                return r2;
            }

            for (Map.Entry<String, Set<JavaType.Class>> r2Entry : r2.entrySet()) {
                r1.compute(r2Entry.getKey(), (pkg, types) -> Stream.concat(
                        types == null ? Stream.empty() : types.stream(),
                        r2Entry.getValue().stream()
                ).collect(toSet()));
            }

            return r1;
        }

        @Override
        public Map<String, Set<JavaType.Class>> defaultTo(Tree t) {
            return emptyMap();
        }

        @Override
        public Map<String, Set<JavaType.Class>> visitTypeName(NameTree name) {
            if (getCursor().firstEnclosing(J.Import.class) == null) {
                JavaType.Class clazz = TypeUtils.asClass(name.getType());
                if (clazz != null) {
                    Map<String, Set<JavaType.Class>> typeByPackage = new HashMap<>();
                    typeByPackage.put(clazz.getPackageName(), singleton(clazz));
                    return typeByPackage;
                }
            }
            return super.visitTypeName(name);
        }
    }

    static class MethodsByType extends JavaSourceVisitor<Map<String, Set<String>>> {
        @Override
        public Map<String, Set<String>> reduce(Map<String, Set<String>> r1,
                                               Map<String, Set<String>> r2) {
            if (r1.isEmpty()) {
                return r2;
            }

            for (Map.Entry<String, Set<String>> r2Entry : r2.entrySet()) {
                r1.compute(r2Entry.getKey(), (pkg, types) -> Stream.concat(
                        types == null ? Stream.empty() : types.stream(),
                        r2Entry.getValue().stream()
                ).collect(toSet()));
            }

            return r1;
        }

        @Override
        public Map<String, Set<String>> defaultTo(Tree t) {
            return emptyMap();
        }

        @Override
        public Map<String, Set<String>> visitMethodInvocation(J.MethodInvocation method) {
            if (method.getSelect() == null) {
                JavaType.Method type = method.getType();
                if (type != null && type.hasFlags(Flag.Static)) {
                    Map<String, Set<String>> typeByPackage = new HashMap<>();
                    typeByPackage.put(type.getDeclaringType().getFullyQualifiedName(), singleton(type.getName()));
                    return typeByPackage;
                }
            }
            return super.visitMethodInvocation(method);
        }
    }
}
