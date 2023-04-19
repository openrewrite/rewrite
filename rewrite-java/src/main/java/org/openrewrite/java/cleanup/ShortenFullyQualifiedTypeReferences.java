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

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            final Map<String, JavaType> usedTypes = new HashMap<>();

            private void ensureInitialized() {
                if (!usedTypes.isEmpty()) {
                    return;
                }
                SourceFile sourceFile = getCursor().firstEnclosing(SourceFile.class);
                if (sourceFile instanceof JavaSourceFile) {
                    JavaIsoVisitor<Map<String, JavaType>> typeCollector = new JavaIsoVisitor<Map<String, JavaType>>() {
                        @Override
                        public J.Import visitImport(J.Import impoort, Map<String, JavaType> types) {
                            if (!impoort.isStatic() && isWellFormedType(impoort.getQualid().getType())) {
                                types.put(impoort.getQualid().getSimpleName(), impoort.getQualid().getType());
                            }
                            return impoort;
                        }

                        @Override
                        public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Map<String, JavaType> types) {
                            return fieldAccess;
                        }

                        @Override
                        public J.TypeParameter visitTypeParameter(J.TypeParameter typeParam, Map<String, JavaType> types) {
                            // using `null` since we don't have access to the type here
                            types.put(((J.Identifier) typeParam.getName()).getSimpleName(), null);
                            return typeParam;
                        }

                        @Override
                        public J.Identifier visitIdentifier(J.Identifier identifier, Map<String, JavaType> types) {
                            JavaType type = identifier.getType();
                            if (type instanceof JavaType.FullyQualified && identifier.getFieldType() == null) {
                                types.put(identifier.getSimpleName(), type);
                            }
                            return identifier;
                        }
                    };
                    typeCollector.visit(sourceFile, usedTypes);
                }
            }

            @Override
            public J visitImport(J.Import impoort, ExecutionContext ctx) {
                // stop recursion
                return impoort;
            }

            @Override
            public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                JavaType type = fieldAccess.getType();
                if (fieldAccess.getName().getFieldType() == null && type instanceof JavaType.Class && ((JavaType.Class) type).getOwningClass() == null) {
                    ensureInitialized();

                    String simpleName = fieldAccess.getSimpleName();
                    if (type.equals(usedTypes.get(simpleName))) {
                        return fieldAccess.getName().withPrefix(fieldAccess.getPrefix());
                    } else if (!usedTypes.containsKey(simpleName)) {
                        maybeAddImport(((JavaType.FullyQualified) type).getFullyQualifiedName());
                        usedTypes.put(simpleName, type);
                        return fieldAccess.getName().withPrefix(fieldAccess.getPrefix());
                    }
                }
                return super.visitFieldAccess(fieldAccess, ctx);
            }
        };
    }
}
