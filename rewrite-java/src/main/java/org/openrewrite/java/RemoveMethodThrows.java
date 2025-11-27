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
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.search.DeclaresMethod;
import org.openrewrite.java.tree.J;

@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveMethodThrows extends Recipe {


    @Option(displayName = "Method pattern",
            description = MethodMatcher.METHOD_PATTERN_INVOCATIONS_DESCRIPTION,
            example = "java.util.List add(..)")
    String methodPattern;

    @Option(displayName = "Match on overrides",
            description = "When enabled, match methods that are overrides of the method pattern. Default is true.",
            required = false)
    @Nullable
    Boolean matchOverrides;

    @Option(displayName = "Exception type pattern",
            description = "A type pattern that is used to find matching exception to remove. Use `*` to match all.",
            example = "java.io.IOException")
    String exceptionTypePattern;

    @Override
    public String getDisplayName() {
        return "Remove elements from a method declaration `throws` clause";
    }

    @Override
    public String getDescription() {
        return "Remove specific, or all exceptions from a method declaration `throws` clause.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(MethodMatcher.validate(methodPattern));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher methodMatcher = new MethodMatcher(methodPattern, matchOverrides != null ? matchOverrides : true);
        TypeMatcher typeMatcher = new TypeMatcher(exceptionTypePattern, true);
        return Preconditions.check(new DeclaresMethod<>(methodMatcher), new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                        J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                        J.ClassDeclaration cd = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
                        if (methodMatcher.matches(m, cd) && m.getThrows() != null) {
                            return m.withThrows(ListUtils.map(m.getThrows(), nt -> {
                                if (typeMatcher.matches(nt.getType())) {
                                    maybeRemoveImport(nt.getType().toString());
                                    return null;
                                }
                                return nt;
                            }));
                        }
                        return m;
                    }
                }
        );
    }
}
