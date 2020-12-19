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
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.tree.*;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;

/**
 * This visitor will remove any imports for types that are not referenced within the compilation unit. This visitor
 * is aware of the import layout style and will correctly handle unfolding of wildcard imports if the import counts
 * drop below the configured values.
 */
public class RemoveUnusedImports extends JavaIsoRefactorVisitor {

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu) {

        ImportLayoutStyle layoutStyle = Optional.ofNullable(cu.getStyle(ImportLayoutStyle.class))
                .orElse(ImportLayoutStyle.getDefaultImportLayoutStyle());

        Map<String, Set<String>> methodsByTypeName = new StaticMethodsByType().visit(cu);
        Map<String, Set<JavaType.Class>> typesByPackage = new TypesByPackage().visit(cu);
        boolean changed = false;
        // Whenever an import statement is found to be used it should be added to this list
        // At the end this list will contain only imports which are actually used
        final List<J.Import> importsWithUsage = new ArrayList<>();

        for (J.Import anImport : cu.getImports()) {
            if (anImport.isStatic()) {
                Set<String> methods = methodsByTypeName.get(anImport.getTypeName());
                if (methods == null) {
                    changed = true;
                    continue;
                }
                if ("*".equals(anImport.getQualid().getSimpleName())) {
                    if (methods.size() < layoutStyle.getNameCountToUseStarImport()) {
                        methods.stream().sorted().forEach(method ->
                                importsWithUsage.add(anImport.withQualid(anImport.getQualid().withName(anImport.getQualid().getName()
                                        .withName(method))).withPrefix("\n"))
                        );
                        changed = true;
                    } else {
                        importsWithUsage.add(anImport);
                    }
                } else {
                    importsWithUsage.add(anImport);
                }
            } else {
                Set<JavaType.Class> types = typesByPackage.get(anImport.getPackageName());
                if (types == null) {
                    changed = true;
                    continue;
                }
                if ("*".equals(anImport.getQualid().getSimpleName())) {
                    if (types.size() < layoutStyle.getClassCountToUseStarImport()) {
                        types.stream().map(JavaType.FullyQualified::getClassName).sorted().forEach(typeClassName ->
                            importsWithUsage.add(anImport.withQualid(anImport.getQualid().withName(anImport.getQualid().getName()
                                    .withName(typeClassName))).withPrefix("\n"))
                        );
                        changed = true;
                    } else {
                        importsWithUsage.add(anImport);
                    }
                } else {
                    importsWithUsage.add(anImport);
                }
            }
        }
        return changed ? cu.withImports(importsWithUsage) : cu;
    }

    static class TypesByPackage extends AbstractJavaSourceVisitor<Map<String, Set<JavaType.Class>>> {
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

    static class StaticMethodsByType extends AbstractJavaSourceVisitor<Map<String, Set<String>>> {
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
            return new HashMap<>();
        }

        @Override
        public Map<String, Set<String>> visitMethodInvocation(J.MethodInvocation method) {
            Map<String, Set<String>> m = super.visitMethodInvocation(method);
            if (method.getSelect() == null) {
                JavaType.Method type = method.getType();
                if (type != null && type.hasFlags(Flag.Static)) {
                    Map<String, Set<String>> m2 = new HashMap<>();
                    m2.put(type.getDeclaringType().getFullyQualifiedName(), singleton(type.getName()));
                    m = reduce(m, m2);
                }
            }
            return m;
        }
    }
}
