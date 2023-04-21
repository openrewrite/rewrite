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
package org.openrewrite.java;

import org.openrewrite.ExecutionContext;
import org.openrewrite.config.RecipeExample;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Extract recipe examples from a test file which are annotated with @DocumentExample
 * Output is the content of the yaml file to present examples
 * Format is like:
 * <pre>
 *               type: specs.openrewrite.org/v1beta/example
 *               recipeName: test.ChangeTextToHello
 *               examples:
 *                 - description: "Change World to Hello in a text file"
 *                   before: "World"
 *                   after: "Hello!"
 *                   language: "text"
 *                 - description: "Change World to Hello in a java file"
 *                   before: |
 *                     public class A {
 *                         void method() {
 *                             System.out.println("World");
 *                         }
 *                     }
 *                   after: |
 *                     public class A {
 *                         void method() {
 *                             System.out.println("Hello!");
 *                         }
 *                     }
 *                   language: "java"
 * </pre>
 */
public class ExamplesExtractor extends JavaIsoVisitor<ExecutionContext> {

    private static final AnnotationMatcher TEST_ANNOTATION_MATCHER = new AnnotationMatcher("@org.junit.jupiter.api.Test");
    private static final AnnotationMatcher DOCUMENT_EXAMPLE_ANNOTATION_MATCHER = new AnnotationMatcher("@org.openrewrite.internal.DocumentExample");

    private static final MethodMatcher REWRITE_RUN_METHOD_MATCHER_WITH_SPEC =
        new MethodMatcher("org.openrewrite.test.RewriteTest rewriteRun(java.util.function.Consumer, org.openrewrite.test.SourceSpecs[])");
    private static final MethodMatcher REWRITE_RUN_METHOD_MATCHER =
        new MethodMatcher("org.openrewrite.test.RewriteTest rewriteRun(org.openrewrite.test.SourceSpecs[])");
    private static final MethodMatcher RECIPE_METHOD_MATCHER = new MethodMatcher("org.openrewrite.test.RecipeSpec recipe(org.openrewrite.Recipe)");

    private static final MethodMatcher JAVA_METHOD_MATCHER = new MethodMatcher("org.openrewrite.java.Assertions java(..)");
    private static final MethodMatcher BUILD_GRADLE_METHOD_MATCHER = new MethodMatcher("org.openrewrite.gradle.Assertions buildGradle(..)");
    private static final MethodMatcher POM_XML_METHOD_MATCHER = new MethodMatcher("org.openrewrite.maven.Assertions pomXml(..)");

    private static final MethodMatcher DEFAULT_METHOD_MATCHER = new MethodMatcher("org.openrewrite.test.RewriteTest defaults(org.openrewrite.test.RecipeSpec)");
    private static final MethodMatcher SPEC_RECIPE_METHOD_MATCHER = new MethodMatcher("org.openrewrite.test.RecipeSpec recipe(org.openrewrite.Recipe)");

    private String recipeType;
    private String defaultRecipeName;
    private String recipeName;
    private List<RecipeExample> recipeExamples;
    private String exampleDescription;

    protected ExamplesExtractor() {
        recipeType = "specs.openrewrite.org/v1beta/example";
        recipeExamples = new ArrayList<>();
    }

    /**
     * print the recipe example yaml.
     */
    public String printRecipeExampleYaml() {
        String name = (recipeName != null && !recipeName.isEmpty()) ? recipeName : defaultRecipeName;
        return new YamlPrinter().print(recipeType, name, recipeExamples);
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
        if ( method.getMethodType().getDeclaringType().getInterfaces().get(0).getFullyQualifiedName().equals("org.openrewrite.test.RewriteTest") &&
             method.getName().getSimpleName().equals("defaults")) {
            defaultRecipeName = findDefaultRecipeName(method);
            return method;
        }

        List<J.Annotation> annotations = method.getLeadingAnnotations();
        if (!hasAnnotation(annotations, TEST_ANNOTATION_MATCHER) &&
            !hasAnnotation(annotations, DOCUMENT_EXAMPLE_ANNOTATION_MATCHER)) {
            return method;
        }

        new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext executionContext) {
                if (DOCUMENT_EXAMPLE_ANNOTATION_MATCHER.matches(annotation)) {
                    List<Expression> args = annotation.getArguments();
                    if (args.size() == 1 && args.get(0) instanceof J.Assignment) {
                        J.Assignment assignment = (J.Assignment) args.get(0);
                        if (assignment.getAssignment() instanceof J.Literal) {
                            exampleDescription = (String) ((J.Literal) assignment.getAssignment()).getValue();
                        }
                    }
                }
                return annotation;
            }
        }.visit(method, ctx);

        return super.visitMethodDeclaration(method, ctx);
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
        List<Expression> args = method.getArguments();
        RecipeExample example = null;
        if (REWRITE_RUN_METHOD_MATCHER_WITH_SPEC.matches(method)) {
            String maybeName = findRecipeNameFromSpecParam(args.get(0));
            if (maybeName != null) {
                recipeName = maybeName;
            }
            example = extractRecipeExample(args.get(1));

        } else if (REWRITE_RUN_METHOD_MATCHER.matches(method)) {
            example = extractRecipeExample(args.get(0));
        }

        if (example != null) {
            example.setDescription(exampleDescription);
            this.recipeExamples.add(example);
        }
        return method;
    }

    private static boolean hasAnnotation( List<J.Annotation> annotations, AnnotationMatcher matcher) {
        return annotations.stream().anyMatch(matcher::matches);
    }

    public static class YamlPrinter {
        String print(String recipeType,
                     String recipeName,
                     List<RecipeExample> examples) {
            if (recipeName == null ||
                recipeName.isEmpty() ||
                examples.isEmpty()
                ) {
                return "";
            }

            StringBuilder output = new StringBuilder();
            output.append("type: ").append(recipeType).append("\n");
            output.append("recipeName: ").append(recipeName).append("\n");
            output.append("examples:\n");
            for (RecipeExample example : examples) {
                output.append("- description: \"").append(example.getDescription()).append("\"\n");
                output.append("  before: |\n");
                output.append(indentTextBlock(example.getBefore()));
                output.append("  after: |\n");
                output.append(indentTextBlock(example.getAfter()));
                output.append("  language: \"").append(example.getLanguage()).append("\"\n");
            }
            return output.toString();
        }

        private String indentTextBlock(String text) {
            String str = "    " + text.replace("\n", "\n    ").trim();
            if (!str.endsWith("\n")) {
                str = str + "\n";
            }
            return str;
        }
    }

    @Nullable
    private String findRecipeNameFromSpecParam(Expression arg) {
        return new JavaIsoVisitor<List<String>>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method,
                                                            List<String> recipeNames) {
                if (RECIPE_METHOD_MATCHER.matches(method)) {
                    new JavaIsoVisitor<List<String>>() {
                        @Override
                        public J.NewClass visitNewClass(J.NewClass newClass, List<String> recipeNames) {
                            if (TypeUtils.isAssignableTo("org.openrewrite.Recipe", newClass.getType())) {
                                JavaType type = newClass.getType();
                                if (type instanceof JavaType.Class) {
                                    JavaType.Class tc = (JavaType.Class) type;
                                    recipeNames.add(tc.getFullyQualifiedName());
                                }
                            }
                            return newClass;
                        }
                    }.visit(method, recipeNames);
                    return method;
                }
                return method;
            }
        }.reduce(arg, new ArrayList<>()).stream().findFirst().orElse(null);
    }

    @Nullable
    private String findDefaultRecipeName(J.MethodDeclaration defaultsMethod) {
        return new JavaIsoVisitor<List<String>>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, List<String> strings) {
                if (SPEC_RECIPE_METHOD_MATCHER.matches(method)) {
                    new JavaIsoVisitor<List<String>>() {
                        @Override
                        public J.NewClass visitNewClass(J.NewClass newClass, List<String> recipeNames) {
                            if (TypeUtils.isAssignableTo("org.openrewrite.Recipe", newClass.getType())) {
                                JavaType type = newClass.getType();
                                if (type instanceof JavaType.Class) {
                                    JavaType.Class tc = (JavaType.Class) type;
                                    recipeNames.add(tc.getFullyQualifiedName());
                                }
                            }
                            return newClass;
                        }
                    }.visit(defaultsMethod, strings);
                }
                return method;
            }
        }.reduce(defaultsMethod, new ArrayList<>()).stream().findFirst().orElse(null);
    }

    @Nullable
    private RecipeExample extractRecipeExample(Expression sourceSpecArg) {
        RecipeExample recipeExample = new RecipeExample();

        new JavaIsoVisitor<RecipeExample>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method,
                                                            RecipeExample recipeExample) {
                if (JAVA_METHOD_MATCHER.matches(method)) {
                    recipeExample.setLanguage("java");
                } else if (BUILD_GRADLE_METHOD_MATCHER.matches(method)) {
                    recipeExample.setLanguage("groovy");
                } else if (POM_XML_METHOD_MATCHER.matches(method)) {
                    recipeExample.setLanguage("xml");
                } else {
                    return method;
                }

                List<Expression> args = method.getArguments();

                // arg0 is always `before`. arg1 is optional to be `after`, to adjust if code changed
                J.Literal before = !args.isEmpty() ? (args.get(0) instanceof J.Literal ? (J.Literal) args.get(0) : null) : null;
                J.Literal after = args.size() > 1? (args.get(1) instanceof J.Literal ? (J.Literal) args.get(1) : null) : null;

                if (before != null) {
                    recipeExample.setBefore((String) before.getValue());
                }

                if (after != null) {
                    recipeExample.setAfter((String) after.getValue());
                }
                return method;
            }
        }.visit(sourceSpecArg, recipeExample);

        if (recipeExample.getLanguage() != null && !recipeExample.getLanguage().isEmpty()) {
            return recipeExample;
        }
        return null;
    }
}
