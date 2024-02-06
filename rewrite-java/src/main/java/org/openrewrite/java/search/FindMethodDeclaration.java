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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindMethodDeclaration extends Recipe {

    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method invocations.",
            example = "java.util.List add(..)")
    String methodPattern;

    @Option(displayName = "Match on overrides",
            description = "When enabled, find methods that are overrides of the method pattern.",
            required = false)
    @Nullable
    Boolean matchOverrides;

    @Override
    public String getDisplayName() {
        return "Find method declaration";
    }

    @Override
    public String getDescription() {
        return "Locates the declaration of a method.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new DeclaresMethod<>(methodPattern, matchOverrides), new JavaIsoVisitor<ExecutionContext>() {
            final MethodMatcher m = new MethodMatcher(methodPattern, matchOverrides);

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
                J.ClassDeclaration cd = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (cd == null) {
                    return md;
                }
                if (m.matches(md, cd)) {
                    md = SearchResult.found(md);
                }
                return md;
            }
        });
    }
}
