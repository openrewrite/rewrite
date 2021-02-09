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
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveUnusedImportsVisitor();
    }

    private static class RemoveUnusedImportsVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {

            ImportLayoutStyle layoutStyle = Optional.ofNullable(cu.getStyle(ImportLayoutStyle.class))
                    .orElse(IntelliJ.importLayout());

            Map<String, Set<String>> methodsByTypeName = new HashMap<>();
            new StaticMethodsByType().visit(cu, methodsByTypeName);

            Map<String, Set<JavaType.Class>> typesByPackage = new HashMap<>();
            new TypesByPackage().visit(cu, typesByPackage);

            boolean changed = false;
            // Whenever an import statement is found to be used it should be added to this list
            // At the end this list will contain only imports which are actually used
            final List<JRightPadded<J.Import>> importsWithUsage = new ArrayList<>();

            for (JRightPadded<J.Import> anImport : cu.getPadding().getImports()) {
                J.Import elem = anImport.getElement();
                J.FieldAccess qualid = elem.getQualid();
                J.Identifier name = qualid.getName();

                if (anImport.getElement().isStatic()) {
                    Set<String> methods = methodsByTypeName.get(anImport.getElement().getTypeName());
                    if (methods == null) {
                        changed = true;
                        continue;
                    }

                    if ("*".equals(qualid.getSimpleName())) {
                        if (methods.size() < layoutStyle.getNameCountToUseStarImport()) {
                            methods.stream().sorted().forEach(method ->
                                    importsWithUsage.add(anImport.withElement(elem.withQualid(qualid.withName(name.withName(method)))))
                            );
                            changed = true;
                        } else {
                            importsWithUsage.add(anImport);
                        }
                    } else {
                        importsWithUsage.add(anImport);
                    }
                } else {
                    Set<JavaType.Class> types = typesByPackage.get(anImport.getElement().getPackageName());
                    if (types == null) {
                        changed = true;
                        continue;
                    }
                    if ("*".equals(anImport.getElement().getQualid().getSimpleName())) {
                        if (types.size() < layoutStyle.getClassCountToUseStarImport()) {
                            types.stream().map(JavaType.FullyQualified::getClassName).sorted().forEach(typeClassName ->
                                    importsWithUsage.add(anImport.withElement(elem.withQualid(qualid.withName(name
                                            .withName(typeClassName))).withPrefix(Space.format("\n"))))
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
            cu = changed ? cu.getPadding().withImports(importsWithUsage) : cu;
            if (cu.getImports().isEmpty()) {
                cu = cu.withClasses(
                        ListUtils.mapFirst(cu.getClasses(),
                                cl -> cl.withPrefix(
                                        cl.getPrefix().withWhitespace(
                                                cl.getPrefix().getWhitespace().replace("\n\n", "")
                                        )
                                )
                        )
                );
            } else {
                if (cu.getPackageDeclaration() == null) {
                    cu = cu.getPadding().withImports(ListUtils.mapFirst(cu.getPadding().getImports(), i -> i.withElement(i.getElement().withPrefix(Space.EMPTY))));
                }
            }
            return cu;
        }

        private static class TypesByPackage extends JavaIsoVisitor<Map<String, Set<JavaType.Class>>> {
            TypesByPackage() {
                setCursoringOn();
            }

            @Override
            public <N extends NameTree> N visitTypeName(N name, Map<String, Set<JavaType.Class>> ctx) {
                if (getCursor().firstEnclosing(J.Import.class) == null) {
                    JavaType.Class clazz = TypeUtils.asClass(name.getType());
                    if (clazz != null) {
                        ctx.computeIfAbsent(clazz.getPackageName(), t -> new HashSet<>()).add(clazz);
                    }
                }
                return super.visitTypeName(name, ctx);
            }
        }

        private static class StaticMethodsByType extends JavaIsoVisitor<Map<String, Set<String>>> {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Map<String, Set<String>> ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (method.getSelect() == null) {
                    JavaType.Method type = method.getType();
                    if (type != null && type.hasFlags(Flag.Static)) {
                        ctx.computeIfAbsent(type.getDeclaringType().getFullyQualifiedName(), t -> new HashSet<>())
                                .add(type.getName());
                    }
                }
                return m;
            }
        }
    }
}
