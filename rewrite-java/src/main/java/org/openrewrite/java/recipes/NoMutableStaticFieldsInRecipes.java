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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

public class NoMutableStaticFieldsInRecipes extends Recipe {
    @Override
    public String getDisplayName() {
        return "Recipe classes should not have mutable `static` fields";
    }

    @Override
    public String getDescription() {
        return "Remove mutable static fields from Recipe classes to discourage their use.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesType<>("org.openrewrite.Recipe", true),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                        J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                        if (TypeUtils.isAssignableTo("org.openrewrite.Recipe", cd.getType())) {
                            return cd.withBody(cd.getBody().withStatements(ListUtils.map(cd.getBody().getStatements(), stmt -> {
                                        if (stmt instanceof J.VariableDeclarations) {
                                            J.VariableDeclarations field = (J.VariableDeclarations) stmt;
                                            if (field.hasModifier(J.Modifier.Type.Static) && !field.hasModifier(J.Modifier.Type.Final)) {
                                                // We want to discourage the use of mutable static fields in recipes,
                                                // so rather than make them immutable, we'll just remove the field.
                                                // Any fields that were intended as constants should be made final.
                                                return null;
                                            }
                                        }
                                        return stmt;
                                    })
                            ));
                        }
                        return cd;
                    }
                }
        );
    }
}
