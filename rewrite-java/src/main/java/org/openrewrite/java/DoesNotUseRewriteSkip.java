/*
 * Copyright 2022 the original author or authors.
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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.Set;

public class DoesNotUseRewriteSkip extends Recipe {
    @Override
    public String getDisplayName() {
        return "Uses `@RewriteSkip` annotation";
    }

    @Override
    public String getDescription() {
        return "The annotation provides a mechanism to skip a whole source file from consideration";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            final UsesRewriteSkipVisitor usesRewriteSkip = new UsesRewriteSkipVisitor();

            @Override
            public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext ctx) {
                JavaSourceFile c = cu;
                if (c == usesRewriteSkip.visit(c, ctx)) {
                    // if this source file is NOT skipped, then the recipe is applicable
                    c = SearchResult.found(c);
                }
                return c;
            }
        };
    }

    /**
     * Marks when {@code RewriteSkip} is configured to skip this source file. The applicable test
     * passes when the inverse is true -- when no {@code RewriteSkip} is configured to skip the source file.
     */
    private static class UsesRewriteSkipVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext ctx) {
            JavaSourceFile c = cu;
            if (c.getPackageDeclaration() != null) {
                Set<J.Annotation> skips = FindAnnotations.find(c.getPackageDeclaration(), "@org.openrewrite.java.RewriteSkip");
                for (J.Annotation skip : skips) {
                    //noinspection ConstantConditions
                    c = c.withPackageDeclaration((J.Package) visit(c.getPackageDeclaration(), ctx, getCursor()));
                    if (skip.getArguments() == null || skip.getArguments().isEmpty()) {
                        // this annotation skips all recipes
                        c = SearchResult.found(c);
                    }
                }
            }
            return c;
        }

        @Override
        public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
            J.Literal l = literal;
            if (literal.getType() == JavaType.Primitive.String) {
                assert literal.getValue() != null;
                Recipe currentRecipe = ctx.getCurrentRecipe();
                if (literal.getValue().toString().equals(currentRecipe.getClass().getName())) {
                    l = SearchResult.found(l);
                }
            }
            return l;
        }

        @Override
        public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            J.FieldAccess f = fieldAccess;
            if (f.getSimpleName().equals("class")) {
                Recipe currentRecipe = ctx.getCurrentRecipe();
                if (TypeUtils.isOfClassType(f.getTarget().getType(), currentRecipe.getClass().getName())) {
                    f = SearchResult.found(f);
                }
            }
            return f;
        }
    }
}
