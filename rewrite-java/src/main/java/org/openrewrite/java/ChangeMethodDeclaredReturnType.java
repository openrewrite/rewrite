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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeTree;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeMethodDeclaredReturnType extends Recipe {
    @Option(displayName = "Method pattern",
        description = MethodMatcher.METHOD_PATTERN_DESCRIPTION,
        example = "org.mockito.Matchers anyVararg()")
    String methodPattern;

    @Option(displayName = "New method declared return type",
        description = "The fully qualified new return type of the method declared/definition.",
        example = "long")
    String newReturnType;

    @Override
    public String getDisplayName() {
        return "Change method definition return type";
    }

    @Override
    public String getDescription() {
        return "Changes the return type of the method definition.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, false);

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                if (methodMatcher.matches(m.getMethodType())) {
                    m = m.withReturnTypeExpression(TypeTree.build(newReturnType));
                    //As the call to the following method don't work, then we should most probably use visitor: AddImport() !!
                    // maybeAddImport(newReturnType);
                    return autoFormat(m, ctx);
                }
                return m;
            }
        };
    }
}