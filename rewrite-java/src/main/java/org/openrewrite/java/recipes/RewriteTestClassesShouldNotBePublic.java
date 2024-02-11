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
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

// Not handled by org.openrewrite.java.testing.cleanup.TestsShouldNotBePublicTest for classes that @Override defaults()
public class RewriteTestClassesShouldNotBePublic extends Recipe {
    @Override
    public String getDisplayName() {
        return "RewriteTest classes should not be public";
    }

    @Override
    public String getDescription() {
        return "Remove the public modifier from classes that implement RewriteTest.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesType<>("org.openrewrite.test.RewriteTest", true),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                        J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                        if (TypeUtils.isAssignableTo("org.openrewrite.test.RewriteTest", cd.getType()) &&
                            cd.getKind() != J.ClassDeclaration.Kind.Type.Interface &&
                            cd.getModifiers().stream().anyMatch(mod -> mod.getType() == J.Modifier.Type.Public) &&
                            cd.getModifiers().stream().noneMatch(mod -> mod.getType() == J.Modifier.Type.Abstract) &&
                            !hasPublicStaticFieldOrMethod(cd)) {

                            // Remove public modifier and move associated comment
                            final List<Comment> modifierComments = new ArrayList<>();
                            List<J.Modifier> modifiers = ListUtils.map(cd.getModifiers(), mod -> {
                                if (mod.getType() == J.Modifier.Type.Public) {
                                    modifierComments.addAll(mod.getComments());
                                    return null;
                                }
                                // copy access level modifier comment to next modifier if it exists
                                if (!modifierComments.isEmpty()) {
                                    J.Modifier nextModifier = mod.withComments(ListUtils.concatAll(new ArrayList<>(modifierComments), mod.getComments()));
                                    modifierComments.clear();
                                    return nextModifier;
                                }
                                return mod;
                            });
                            // if no following modifier exists, add comments to method itself
                            if (!modifierComments.isEmpty()) {
                                cd = cd.withComments(ListUtils.concatAll(cd.getComments(), modifierComments));
                            }
                            cd = maybeAutoFormat(cd, cd.withModifiers(modifiers), cd.getName(), ctx, getCursor().getParentTreeCursor());
                        }
                        return cd;
                    }

                    private boolean hasPublicStaticFieldOrMethod(J.ClassDeclaration cd) {
                        if (cd.getBody().getStatements().stream()
                                .filter(J.MethodDeclaration.class::isInstance)
                                .map(J.MethodDeclaration.class::cast)
                                .anyMatch(method -> method.hasModifier(J.Modifier.Type.Public) && method.hasModifier(J.Modifier.Type.Static))) {
                            return true;
                        }
                        return cd.getBody().getStatements().stream()
                                .filter(J.VariableDeclarations.class::isInstance)
                                .map(J.VariableDeclarations.class::cast)
                                .anyMatch(field -> field.hasModifier(J.Modifier.Type.Public) && field.hasModifier(J.Modifier.Type.Static));

                    }
                }
        );
    }
}
