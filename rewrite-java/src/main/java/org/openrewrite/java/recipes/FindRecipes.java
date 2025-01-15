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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.table.RewriteRecipeSource;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.search.FindProperty;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class FindRecipes extends Recipe {
    RewriteRecipeSource recipeSource = new RewriteRecipeSource(this);

    @Override
    public String getDisplayName() {
        return "Find OpenRewrite recipes";
    }

    @Override
    public String getDescription() {
        return "This recipe finds all OpenRewrite recipes, primarily to produce a data table that is being used " +
               "to experiment with fine-tuning a large language model to produce more recipes.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> findImperativeRecipes = findImperativeRecipes();
        TreeVisitor<?, ExecutionContext> findRefasterRecipes = findRefasterRecipes();
        TreeVisitor<?, ExecutionContext> findYamlRecipes = findYamlRecipes();
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree preVisit(@NonNull Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                tree = findImperativeRecipes.visit(tree, ctx);
                tree = findRefasterRecipes.visit(tree, ctx);
                tree = findYamlRecipes.visit(tree, ctx);
                return tree;
            }
        };
    }

    private TreeVisitor<?, ExecutionContext> findRefasterRecipes() {
        String recipeDescriptor = "org.openrewrite.java.template.RecipeDescriptor";
        AnnotationMatcher annotationMatcher = new AnnotationMatcher("@" + recipeDescriptor);
        return Preconditions.check(new UsesType<>(recipeDescriptor, false), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                for (J.Annotation annotation : cd.getLeadingAnnotations()) {
                    if (annotationMatcher.matches(annotation)) {
                        String name = null;
                        String description = null;
                        for (Expression argument : annotation.getArguments()) {
                            if (argument instanceof J.Assignment) {
                                J.Assignment assignment = (J.Assignment) argument;
                                if (assignment.getVariable() instanceof J.Identifier) {
                                    String simpleName = ((J.Identifier) assignment.getVariable()).getSimpleName();
                                    if ("name".equals(simpleName)) {
                                        if (assignment.getAssignment() instanceof J.Literal) {
                                            name = (String) ((J.Literal) assignment.getAssignment()).getValue();
                                        }
                                    } else if ("description".equals(simpleName)) {
                                        if (assignment.getAssignment() instanceof J.Literal) {
                                            description = (String) ((J.Literal) assignment.getAssignment()).getValue();
                                        }
                                    }
                                }
                            }
                        }
                        if (name != null && description != null) {
                            recipeSource.insertRow(ctx, new RewriteRecipeSource.Row(
                                    name,
                                    description,
                                    RewriteRecipeSource.RecipeType.Refaster,
                                    cd.printTrimmed(getCursor()),
                                    "[]"
                            ));
                            return SearchResult.found(cd);
                        }
                    }
                }
                return cd;
            }
        });
    }

    private TreeVisitor<?, ExecutionContext> findImperativeRecipes() {
        MethodMatcher getDisplayName = new MethodMatcher("org.openrewrite.Recipe getDisplayName()", true);
        MethodMatcher getDescription = new MethodMatcher("org.openrewrite.Recipe getDescription()", true);
        AnnotationMatcher optionAnnotation = new AnnotationMatcher("@org.openrewrite.Option");
        return Preconditions.check(new UsesType<>("org.openrewrite.Recipe", false), new JavaIsoVisitor<ExecutionContext>() {
            final List<J.VariableDeclarations> options = new ArrayList<>();

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                if (TypeUtils.isAssignableTo("org.openrewrite.Recipe", classDecl.getType())) {
                    recipeSource.insertRow(ctx, new RewriteRecipeSource.Row(
                            getCursor().getMessage("displayName"),
                            getCursor().getMessage("description"),
                            RewriteRecipeSource.RecipeType.Java,
                            getCursor().firstEnclosingOrThrow(J.CompilationUnit.class).printAllTrimmed(),
                            convertOptionsToJSON(options)
                    ));
                    return classDecl.withName(SearchResult.found(classDecl.getName()));
                }
                return cd;
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                if (multiVariable.getLeadingAnnotations().stream().anyMatch(optionAnnotation::matches)) {
                    options.add(multiVariable);
                }
                return super.visitVariableDeclarations(multiVariable, ctx);
            }

            @Override
            public J.Return visitReturn(J.Return aReturn, ExecutionContext ctx) {
                J j = getCursor().dropParentUntil(it -> it instanceof J.MethodDeclaration || it instanceof J.ClassDeclaration).getValue();
                if (j instanceof J.MethodDeclaration) {
                    J.MethodDeclaration method = (J.MethodDeclaration) j;
                    if (getDisplayName.matches(method.getMethodType()) && aReturn.getExpression() instanceof J.Literal) {
                        getCursor().putMessageOnFirstEnclosing(J.ClassDeclaration.class, "displayName",
                                requireNonNull(((J.Literal) aReturn.getExpression()).getValue()));
                    }
                    if (getDescription.matches(method.getMethodType()) && aReturn.getExpression() instanceof J.Literal) {
                        getCursor().putMessageOnFirstEnclosing(J.ClassDeclaration.class, "description",
                                requireNonNull(((J.Literal) aReturn.getExpression()).getValue()));
                    }
                }

                return super.visitReturn(aReturn, ctx);
            }

            private String convertOptionsToJSON(List<J.VariableDeclarations> options) {
                ArrayNode optionsArray = JsonNodeFactory.instance.arrayNode();
                for (J.VariableDeclarations option : options) {
                    ObjectNode optionNode = optionsArray.addObject();
                    optionNode.put("name", option.getVariables().get(0).getSimpleName());
                    mapOptionAnnotation(option.getLeadingAnnotations(), optionNode);
                }
                return optionsArray.toString();
            }

            private void mapOptionAnnotation(List<J.Annotation> leadingAnnotations, ObjectNode optionNode) {
                for (J.Annotation annotation : leadingAnnotations) {
                    if (optionAnnotation.matches(annotation) && annotation.getArguments() != null) {
                        for (Expression argument : annotation.getArguments()) {
                            if (argument instanceof J.Assignment) {
                                J.Assignment assignment = (J.Assignment) argument;
                                if (assignment.getVariable() instanceof J.Identifier) {
                                    J.Identifier identifier = (J.Identifier) assignment.getVariable();
                                    if (assignment.getAssignment() instanceof J.Literal) {
                                        optionNode.set(identifier.getSimpleName(), mapValue(((J.Literal) assignment.getAssignment()).getValue()));
                                    } else if (assignment.getAssignment() instanceof J.NewArray) {
                                        J.NewArray newArray = (J.NewArray) assignment.getAssignment();
                                        if (newArray.getInitializer() != null) {
                                            ArrayNode valuesArray = optionNode.putArray(identifier.getSimpleName());
                                            for (Expression expression : newArray.getInitializer()) {
                                                if (expression instanceof J.Literal) {
                                                    valuesArray.add(mapValue(((J.Literal) expression).getValue()));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }

            private ValueNode mapValue(@Nullable Object value) {
                if (value instanceof String) {
                    return JsonNodeFactory.instance.textNode((String) value);
                } else if (value instanceof Boolean) {
                    return JsonNodeFactory.instance.booleanNode((Boolean) value);
                } else if (value instanceof Integer) {
                    return JsonNodeFactory.instance.numberNode((Integer) value);
                } else if (value == null) {
                    return JsonNodeFactory.instance.nullNode();
                }
                throw new IllegalArgumentException(String.valueOf(value));
            }
        });
    }

    private TreeVisitor<?, ExecutionContext> findYamlRecipes() {
        return Preconditions.check(
                new FindProperty("type", false, "specs.openrewrite.org/v1beta/recipe"),
                new YamlIsoVisitor<ExecutionContext>() {
                    @Override
                    public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext ctx) {
                        Yaml.Document doc = super.visitDocument(document, ctx);

                        if ("specs.openrewrite.org/v1beta/recipe".equals(extractValue(doc, "type"))) {
                            String displayName = extractValue(doc, "displayName");
                            String description = extractValue(doc, "description");
                            if (displayName != null && description != null) {
                                recipeSource.insertRow(ctx, new RewriteRecipeSource.Row(
                                        displayName,
                                        description,
                                        RewriteRecipeSource.RecipeType.Yaml,
                                        doc.withPrefix("").printTrimmed(getCursor()),
                                        "[]"
                                ));
                                return SearchResult.found(doc);
                            }
                        }
                        return doc;
                    }

                    private @Nullable String extractValue(Yaml yaml, String key) {
                        Set<Yaml.Block> blocks = FindProperty.find(yaml, key, false);
                        if (!blocks.isEmpty()) {
                            Yaml.Block first = blocks.iterator().next();
                            if (first instanceof Yaml.Scalar) {
                                return ((Yaml.Scalar) first).getValue();
                            }
                        }
                        return null;
                    }
                }
        );
    }
}
