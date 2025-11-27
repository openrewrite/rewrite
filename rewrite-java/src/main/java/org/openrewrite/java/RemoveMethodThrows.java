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

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveMethodThrows extends Recipe {

    @Option(displayName = "Method pattern",
            example = "com.example.MyClass myMethod(..)")
    String methodPattern;

    @Option(displayName = "Exception type pattern",
            description = "A type pattern that is used to find matching exception to remove. Use `*` to match all.",
            example = "java.io.IOException")
    String exceptionTypePattern;

    @Option(displayName = "Match overriden methods",
            description = "Whether to match overridden forms of the method on subclasses of typeMatcher. Default is true.",
            required = false)
    @Nullable
    Boolean matchOverrides;

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
        return super.validate().and(MethodMatcher.validate(methodPattern));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        boolean varMatchOverrides = matchOverrides != null ? matchOverrides : true;
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

                            TypeMatcher typeMatcher = new TypeMatcher(exceptionTypePattern);

                            List<NameTree> updatedThrows = new ArrayList<>();
                            for (NameTree nameTree : m.getThrows()) {
                                if (nameTree.getType() != null) {
                                    if (typeMatcher.matches(nameTree.getType())) {
                                        maybeRemoveImport(nameTree.getType().toString());
                                    } else {
                                        updatedThrows.add(nameTree);
                                    }
                                }
                            }

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
