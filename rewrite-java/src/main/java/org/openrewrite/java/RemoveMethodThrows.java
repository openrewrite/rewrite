/*
 * Copyright 2025 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.java.search.DeclaresMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.NameTree;

import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveMethodThrows extends Recipe {

    @Option(displayName = "Method pattern",
            example = "com.example.MyClass myMethod(..)")
    String methodPattern;

    @Option(displayName = "Exception type",
            description = "Fully qualified name of the exception to remove (e.g. `java.io.IOException`).",
            example = "java.io.IOException",
            required = false)
    @Nullable
    String exceptionType;

    @Option(displayName = "Match overriden methods",
            description = "Whether to match overridden forms of the method on subclasses of typeMatcher. Default is true.",
            required = false)
    @Nullable
    Boolean matchOverrides;

    @Option(displayName = "Remove all throws declarations",
            description = "Remove all throws declarations. Default is false.",
            required = false)
    @Nullable
    Boolean removeAll;

    @Override
    public String getDisplayName() {
        return "Remove a specific exception from a method's throws clause";
    }

    @Override
    public String getDescription() {
        return "Remove a specific exception from a method's throws clause.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> v = super.validate()
                .and(MethodMatcher.validate(methodPattern));

        // Cross-field validation: exceptionType is required unless removeAll is true
        if ((Boolean.FALSE.equals(removeAll) || removeAll == null) && exceptionType == null) {
            v = v.and(Validated.invalid(
                    "exceptionType",
                    exceptionType,
                    "exceptionType must be provided when removeAll is false"));
        }

        return v;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        boolean varMatchOverrides = matchOverrides != null ? matchOverrides : true;
        boolean varRemoveAll = removeAll != null ? removeAll : false;
        MethodMatcher methodMatcher = new MethodMatcher(methodPattern, varMatchOverrides);
        return Preconditions.check(new DeclaresMethod<>(methodMatcher), new JavaIsoVisitor<ExecutionContext>() {

                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                        J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                        J.ClassDeclaration cd = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);

                        if (methodMatcher.matches(m, cd)) {

                            if (m.getThrows() == null) {
                                return m; // no throws to modify
                            }

                            if (varRemoveAll) {
                                // get list of throws to maybeRemoveImport later
                                List<String> throwsTypes = m.getThrows().stream()
                                        .filter(t -> t.getType() != null)
                                        .map(t -> t.getType().toString())
                                        .collect(Collectors.toList());
                                throwsTypes.forEach(this::maybeRemoveImport);
                                // Remove the entire throws clause
                                return m.withThrows(null);
                            }

                            if (exceptionType == null) {
                                throw new IllegalStateException("exceptionType should never be null here");
                            }

                            List<NameTree> updatedThrows = m.getThrows().stream()
                                    .filter(t -> {
                                        // Keep only exception types that are not the target
                                        String fqn = t.getType() != null ? t.getType().toString() : null;
                                        return fqn == null || !fqn.equals(exceptionType);
                                    })
                                    .collect(Collectors.toList());

                            maybeRemoveImport(exceptionType);

                            if (updatedThrows.isEmpty()) {
                                // Remove the entire throws clause
                                return m.withThrows(null);
                            }

                            // Replace with filtered throws list
                            return m.withThrows(updatedThrows);
                        }
                        return m;
                    }

                }
        );
    }
}
