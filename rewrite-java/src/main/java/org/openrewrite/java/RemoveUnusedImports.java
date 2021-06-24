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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.*;

import java.util.*;

/**
 * This recipe will remove any imports for types that are not referenced within the compilation unit. This recipe
 * is aware of the import layout style and will correctly handle unfolding of wildcard imports if the import counts
 * drop below the configured values.
 */
public class RemoveUnusedImports extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove unused imports";
    }

    @Override
    public String getDescription() {
        return "Remove imports for types that are not referenced.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveUnusedImportsVisitor();
    }

    private static class RemoveUnusedImportsVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            ImportLayoutStyle layoutStyle = Optional.ofNullable(cu.getStyle(ImportLayoutStyle.class))
                    .orElse(IntelliJ.importLayout());

            Map<String, Set<String>> methodsAndFieldsByTypeName = new HashMap<>();
            Map<String, Set<JavaType.FullyQualified>> typesByPackage = new HashMap<>();

            for (JavaType javaType : cu.getTypesInUse().keySet()) {
                if(javaType instanceof JavaType.Variable) {
                    JavaType.Variable variable = (JavaType.Variable) javaType;
                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(variable.getType());
                    if(fq != null) {
                        methodsAndFieldsByTypeName.computeIfAbsent(fq.getFullyQualifiedName(), f -> new HashSet<>())
                            .add(variable.getName());
                    }
                } else if(javaType instanceof JavaType.Method) {
                    JavaType.Method method = (JavaType.Method) javaType;
                    if(method.hasFlags(Flag.Static)) {
                        methodsAndFieldsByTypeName.computeIfAbsent(method.getDeclaringType().getFullyQualifiedName(), t -> new HashSet<>())
                                .add(method.getName());
                    }
                } else if(javaType instanceof JavaType.FullyQualified) {
                    JavaType.FullyQualified fullyQualified = (JavaType.FullyQualified) javaType;
                    typesByPackage.computeIfAbsent(fullyQualified.getPackageName(), f -> new HashSet<>())
                            .add(fullyQualified);
                }
            }

            boolean changed = false;

            // Whenever an import statement is found to be used it should be added to this list
            // At the end this list will contain only imports which are actually used
            final List<JRightPadded<J.Import>> importsWithUsage = new ArrayList<>();

            for (JRightPadded<J.Import> anImport : cu.getPadding().getImports()) {
                J.Import elem = anImport.getElement();
                J.FieldAccess qualid = elem.getQualid();
                J.Identifier name = qualid.getName();

                if (anImport.getElement().isStatic()) {
                    Set<String> methodsAndFields = methodsAndFieldsByTypeName.get(anImport.getElement().getTypeName());
                    if (methodsAndFields == null) {
                        changed = true;
                        continue;
                    }

                    if ("*".equals(qualid.getSimpleName())) {
                        if (methodsAndFields.size() < layoutStyle.getNameCountToUseStarImport()) {
                            methodsAndFields.stream().sorted().forEach(method ->
                                    importsWithUsage.add(anImport.withElement(elem.withQualid(qualid.withName(name.withName(method)))))
                            );
                            changed = true;
                        } else {
                            importsWithUsage.add(anImport);
                        }
                    } else if(methodsAndFields.contains(qualid.getSimpleName())) {
                        importsWithUsage.add(anImport);
                    } else {
                        changed = true;
                    }
                } else {
                    Set<JavaType.FullyQualified> types = typesByPackage.get(anImport.getElement().getPackageName());
                    if (types == null) {
                        changed = true;
                        continue;
                    }
                    if ("*".equals(anImport.getElement().getQualid().getSimpleName())) {
                        if (types.size() < layoutStyle.getClassCountToUseStarImport()) {
                            List<String> toSort = new ArrayList<>();
                            for (JavaType.FullyQualified type : types) {
                                String typeClassName = type.getClassName();
                                toSort.add(typeClassName);
                            }
                            toSort.sort(null);
                            List<JRightPadded<J.Import>> unfoldedWildcardImports = new ArrayList<>(toSort.size());
                            for (String typeClassName : toSort) {
                                JRightPadded<J.Import> importJRightPadded = anImport.withElement(
                                        new J.Import(
                                                Tree.randomId(),
                                                elem.getPrefix(),
                                                elem.getMarkers(),
                                                elem.getPadding().getStatic(),
                                                elem.getQualid().withName(
                                                        name.withName(typeClassName)
                                                )
                                        )
                                );
                                unfoldedWildcardImports.add(importJRightPadded);
                            }

                            importsWithUsage.addAll(ListUtils.map(unfoldedWildcardImports, (index, paddedImport) -> {
                                if (index != 0) {
                                    paddedImport = paddedImport.withElement(
                                            paddedImport.getElement().withPrefix(
                                                    Space.format("\n")
                                            )
                                    );
                                }
                                return paddedImport;
                            }));

                            changed = true;
                        } else {
                            importsWithUsage.add(anImport);
                        }
                    } else if (types.stream()
                            .filter(c -> anImport.getElement().isFromType(c.getFullyQualifiedName()))
                            .findAny().isPresent()) {
                        importsWithUsage.add(anImport);
                    } else {
                        changed = true;
                    }
                }
            }

            if (changed) {
                cu = cu.getPadding().withImports(importsWithUsage);
                if (!cu.getImports().isEmpty()) {
                    cu = autoFormat(cu, cu.getImports().get(cu.getImports().size() - 1), ctx, getCursor());
                } else if (!cu.getClasses().isEmpty()) {
                    cu = autoFormat(cu, cu.getClasses().get(0).getName(), ctx, getCursor());
                }
                cu = (J.CompilationUnit) new OrderImports.OrderImportsVisitor<ExecutionContext>(false).visit(cu, ctx);
            }

            return cu;
        }
    }
}
