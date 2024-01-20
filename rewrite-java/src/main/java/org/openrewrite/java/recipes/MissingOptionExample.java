/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.recipes;

import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

public class MissingOptionExample extends Recipe {
    @Override
    public String getDisplayName() {
        return "Find missing `@Option` `example` values";
    }

    @Override
    public String getDescription() {
        return "Find `@Option` annotations that are missing `example` values for documentation.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("org.openrewrite.Option", false),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext executionContext) {
                        J.Annotation an = super.visitAnnotation(annotation, executionContext);
                        if (!TypeUtils.isOfClassType(annotation.getType(), "org.openrewrite.Option") || an.getArguments() == null) {
                            return an;
                        }

                        // Skip if there is already an example value, or valid options
                        boolean hasExample = an.getArguments().stream().anyMatch(exp -> {
                            if (exp instanceof J.Assignment) {
                                Expression variable = ((J.Assignment) exp).getVariable();
                                if (variable instanceof J.Identifier) {
                                    String simpleName = ((J.Identifier) variable).getSimpleName();
                                    return "example".equals(simpleName) || "valid".equals(simpleName);
                                }
                            }
                            return false;
                        });
                        if (hasExample) {
                            return an;
                        }

                        // Skip boolean and non-String primitive fields, as examples there are trivial
                        Cursor parent = getCursor().getParent();
                        if (parent != null && parent.getValue() instanceof J.VariableDeclarations) {
                            J.VariableDeclarations variableDeclarations = parent.getValue();
                            if (variableDeclarations.getTypeExpression() != null) {
                                JavaType type = variableDeclarations.getTypeExpression().getType();
                                if (!TypeUtils.isString(type)) {
                                    if (type instanceof JavaType.Primitive ||
                                        type instanceof JavaType.FullyQualified &&
                                        "java.lang".equals(((JavaType.FullyQualified) type).getPackageName())) {
                                        return an;
                                    }
                                }
                            }
                        }

                        return SearchResult.found(an, "Missing example value for documentation");
                    }
                });
    }
}
