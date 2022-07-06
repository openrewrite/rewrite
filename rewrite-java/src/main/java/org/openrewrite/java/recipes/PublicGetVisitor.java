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
package org.openrewrite.java.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;

import static org.openrewrite.java.tree.J.Modifier.Type.Protected;
import static org.openrewrite.java.tree.J.Modifier.Type.Public;

public class PublicGetVisitor extends Recipe {
    private static final MethodMatcher getVisitor = new MethodMatcher("org.openrewrite.Recipe getVisitor()", true);

    @Override
    public String getDisplayName() {
        return "Make all `Recipe#getVisitor()` methods public";
    }

    @Override
    public String getDescription() {
        return "It would be a breaking API change to increase the visibility of the method by default, " +
                "but any recipe can increase visibility of the `Recipe` class. Making them public makes " +
                "recipes easier to use in other recipes in unexpected ways.";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitJavaSourceFile(JavaSourceFile cu, ExecutionContext ctx) {
                for (JavaType.Method type : cu.getTypesInUse().getDeclaredMethods()) {
                    if (getVisitor.matches(type)) {
                        return cu.withMarkers(cu.getMarkers().searchResult());
                    }
                }
                return cu;
            }
        };
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                if (getVisitor.matches(method, getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class)) &&
                        J.Modifier.hasModifier(m.getModifiers(), Protected)) {
                    m = m.withModifiers(ListUtils.map(m.getModifiers(), mod ->
                            mod.getType() == Protected ? mod.withType(Public) : mod));
                }
                return m;
            }
        };
    }
}
