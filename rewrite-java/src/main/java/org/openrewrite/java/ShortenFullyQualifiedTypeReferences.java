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
package org.openrewrite.java;

import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.DefaultJavaTypeSignatureBuilder;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.*;

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
    public JavaVisitor<ExecutionContext> getVisitor() {
        // This wrapper is necessary so that the "correct" implementation is used when this recipe is used declaratively
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    return ((JavaSourceFile) tree).service(ImportService.class).shortenFullyQualifiedTypeReferencesIn((J) tree).visit(tree, ctx);
                }
                return (J) tree;
            }
        };
    }

    /**
     * Returns a visitor which replaces all fully qualified references in the given subtree with simple names and adds 
     * corresponding import statements.
     * <p>
     * For compatibility with other Java-based languages it is recommended to use this as a service via
     * {@link ImportService#shortenFullyQualifiedTypeReferencesIn(J)}, as that will dispatch to the correct
     * implementation for the language.
     * 
     * @see ImportService#shortenFullyQualifiedTypeReferencesIn(J)
     * @see JavaVisitor#service(Class) 
     */
    public static <J2 extends J> JavaVisitor<ExecutionContext> modifyOnly(J2 subtree) {
        return getVisitor(subtree);
    }

    @NonNull
    private static JavaVisitor<ExecutionContext> getVisitor(@Nullable J scope) {
        return new JavaVisitor<ExecutionContext>() {
            final Map<String, JavaType> usedTypes = new HashMap<>();
            final JavaTypeSignatureBuilder signatureBuilder = new DefaultJavaTypeSignatureBuilder();

            boolean modify = scope == null;

            private void ensureInitialized() {
                if (!usedTypes.isEmpty()) {
                    return;
                }
                SourceFile sourceFile = getCursor().firstEnclosing(SourceFile.class);
                if (sourceFile instanceof JavaSourceFile) {
                    JavaIsoVisitor<Map<String, JavaType>> typeCollector = new JavaIsoVisitor<Map<String, JavaType>>() {
                        @Override
                        public J.Import visitImport(J.Import import_, Map<String, JavaType> types) {
                            if (!import_.isStatic() && isWellFormedType(import_.getQualid().getType())) {
                                types.put(import_.getQualid().getSimpleName(), import_.getQualid().getType());
                            }
                            return import_;
                        }

                        @Override
                        public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Map<String, JavaType> types) {
                            if (fieldAccess.getTarget() instanceof J.Identifier) {
                                visitIdentifier((J.Identifier) fieldAccess.getTarget(), types);
                            } else if (fieldAccess.getTarget() instanceof J.FieldAccess) {
                                visitFieldAccess((J.FieldAccess) fieldAccess.getTarget(), types);
                            }
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
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                @SuppressWarnings("DataFlowIssue")
                boolean subtreeRoot = !modify && (scope.equals(tree) || scope.isScope(tree));
                if (subtreeRoot) {
                    modify = true;
                }
                try {
                    return super.visit(tree, ctx);
                } finally {
                    if (subtreeRoot) {
                        modify = false;
                    }
                }
            }

            @Override
            public J visitImport(J.Import import_, ExecutionContext ctx) {
                // stop recursion
                return import_;
            }

            @Override
            public Space visitSpace(Space space, Space.Location loc, ExecutionContext ctx) {
                // stop recursion into Javadoc comments
                return space;
            }

            @Override
            public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                if (!modify) {
                    return super.visitFieldAccess(fieldAccess, ctx);
                }

                JavaType type = fieldAccess.getType();
                if (fieldAccess.getName().getFieldType() == null && type instanceof JavaType.Class && ((JavaType.Class) type).getOwningClass() == null) {
                    ensureInitialized();

                    String simpleName = fieldAccess.getSimpleName();
                    JavaType usedType = usedTypes.get(simpleName);
                    if (type == usedType || signatureBuilder.signature(type).equals(signatureBuilder.signature(usedType))) {
                        return !fieldAccess.getPrefix().isEmpty() ? fieldAccess.getName().withPrefix(fieldAccess.getPrefix()) : fieldAccess.getName();
                    } else if (!usedTypes.containsKey(simpleName)) {
                        String fullyQualifiedName = ((JavaType.FullyQualified) type).getFullyQualifiedName();
                        if (!fullyQualifiedName.startsWith("java.lang.")) {
                            maybeAddImport(fullyQualifiedName);
                            usedTypes.put(simpleName, type);
                            if (!fieldAccess.getName().getAnnotations().isEmpty()) {
                                return fieldAccess.getName().withAnnotations(ListUtils.map(fieldAccess.getName().getAnnotations(), (i, a) -> {
                                    if (i == 0) {
                                        return a.withPrefix(fieldAccess.getPrefix());
                                    }
                                    return a;
                                }));
                            }
                            return fieldAccess.getName().withPrefix(fieldAccess.getPrefix());
                        }
                    }
                }
                return super.visitFieldAccess(fieldAccess, ctx);
            }
        };
    }
}
