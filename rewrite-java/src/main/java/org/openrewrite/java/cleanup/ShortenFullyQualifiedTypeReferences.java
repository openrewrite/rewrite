/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.openrewrite.java.tree.TypeUtils.isWellFormedType;

public class ShortenFullyQualifiedTypeReferences extends Recipe {

    @Override
    public String getDisplayName() {
        return "Add imports for fully qualified references to types";
    }

    @Override
    public String getDescription() {
        return "Any fully qualified references to Java types will be replaced with corresponding simple "
                + "names and import statements, provided that it doesn't result in "
                + "any conflicts with other imports or types declared in the local compilation unit.";
    }

    @Override
    public @Nullable Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            final Set<String> localTypes = new HashSet<>();
            final Map<String, JavaType> importedTypes = new HashMap<>();
            final Map<String, JavaType> staticallyImportedMembers = new HashMap<>();

            @Override
            public @Nullable J visitSourceFile(SourceFile sourceFile, ExecutionContext ctx) {
                if (sourceFile instanceof J.CompilationUnit) {
                    J.CompilationUnit cu = (J.CompilationUnit) sourceFile;
                    JavaIsoVisitor<Set<String>> typeCollector = new JavaIsoVisitor<Set<String>>() {
                        @Override
                        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Set<String> types) {
                            types.add(classDecl.getSimpleName());
                            return super.visitClassDeclaration(classDecl, types);
                        }
                    };
                    typeCollector.visit(cu, localTypes);
                }
                return super.visitSourceFile(sourceFile, ctx);
            }

            @Override
            public J visitImport(J.Import impoort, ExecutionContext ctx) {
                if (impoort.isStatic() && isWellFormedType(impoort.getQualid().getType())) {
                    staticallyImportedMembers.put(impoort.getQualid().getSimpleName(), impoort.getQualid().getType());
                } else if (!impoort.isStatic() && isWellFormedType(impoort.getQualid().getType())) {
                    importedTypes.put(impoort.getQualid().getSimpleName(), impoort.getQualid().getType());
                }
                return impoort;
            }

            @Override
            public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                JavaType type = fieldAccess.getType();
                if (type instanceof JavaType.Class) {
                    String simpleName = fieldAccess.getSimpleName();
                    if (type.equals(importedTypes.get(simpleName))) {
                        return fieldAccess.getName().withPrefix(fieldAccess.getPrefix());
                    } else if (!importedTypes.containsKey(simpleName) && !localTypes.contains(simpleName)) {
                        maybeAddImport(((JavaType.FullyQualified) type).getFullyQualifiedName());
                        importedTypes.put(simpleName, type);
                        return fieldAccess.getName().withPrefix(fieldAccess.getPrefix());
                    }
                }
                return super.visitFieldAccess(fieldAccess, ctx);
            }
        };
    }
}
