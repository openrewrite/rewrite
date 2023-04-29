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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.TypeMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.Iterator;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindDeprecatedClasses extends Recipe {
    @Option(displayName = "Type pattern",
            description = "A type pattern that is used to find matching classes.",
            example = "org.springframework..*",
            required = false)
    @Nullable
    String typePattern;

    @Option(displayName = "Match inherited",
            description = "When enabled, find types that inherit from a deprecated type.",
            required = false)
    @Nullable
    Boolean matchInherited;

    @Option(displayName = "Ignore deprecated scopes",
            description = "When a deprecated type is used in a deprecated method or class, ignore it.",
            required = false)
    @Nullable
    Boolean ignoreDeprecatedScopes;

    @Override
    public String getDisplayName() {
        return "Find uses of deprecated classes";
    }

    @Override
    public String getDescription() {
        return "Find uses of deprecated classes, optionally ignoring those classes that are inside deprecated scopes.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TypeMatcher typeMatcher = typePattern == null ? null : new TypeMatcher(typePattern);

        return Preconditions.check(new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                for (JavaType javaType : cu.getTypesInUse().getTypesInUse()) {
                    JavaType.FullyQualified fqn = TypeUtils.asFullyQualified(javaType);
                    if (fqn != null && (typeMatcher == null || typeMatcher.matches(fqn))) {
                        for (JavaType.FullyQualified annotation : fqn.getAnnotations()) {
                            if (TypeUtils.isOfClassType(annotation, "java.lang.Deprecated")) {
                                return SearchResult.found(cu);
                            }
                        }
                    }
                }
                return cu;
            }
        }, new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public <N extends NameTree> N visitTypeName(N nameTree, ExecutionContext executionContext) {
                if (getCursor().firstEnclosing(J.Import.class) == null) {
                    JavaType.FullyQualified fqn = TypeUtils.asFullyQualified(nameTree.getType());
                    if (fqn != null && (typeMatcher == null || typeMatcher.matches(fqn))) {
                        for (JavaType.FullyQualified annotation : fqn.getAnnotations()) {
                            if (TypeUtils.isOfClassType(annotation, "java.lang.Deprecated")) {
                                if (Boolean.TRUE.equals(ignoreDeprecatedScopes)) {
                                    Iterator<Object> cursorPath = getCursor().getPath();
                                    while (cursorPath.hasNext()) {
                                        Object ancestor = cursorPath.next();
                                        if (ancestor instanceof J.MethodDeclaration &&
                                            isDeprecated(((J.MethodDeclaration) ancestor).getAllAnnotations())) {
                                            return nameTree;
                                        }
                                        if (ancestor instanceof J.ClassDeclaration &&
                                            isDeprecated(((J.ClassDeclaration) ancestor).getAllAnnotations())) {
                                            return nameTree;
                                        }
                                    }
                                }

                                return SearchResult.found(nameTree);
                            }
                        }
                    }
                }

                return nameTree;
            }

            private boolean isDeprecated(List<J.Annotation> annotations) {
                for (J.Annotation annotation : annotations) {
                    if (TypeUtils.isOfClassType(annotation.getType(), "java.lang.Deprecated")) {
                        return true;
                    }
                }
                return false;
            }
        });
    }
}
