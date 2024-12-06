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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.table.RewriteRecipeSource;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.yaml.Assertions.yaml;

class FindRecipesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindRecipes()).parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()));
    }

    @DocumentExample
    @Test
    void findRecipes() {
        rewriteRun(
          spec -> spec
            .dataTable(RewriteRecipeSource.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                RewriteRecipeSource.Row row = rows.get(0);
                assertThat(row.getDisplayName()).isEqualTo("My recipe");
                assertThat(row.getDescription()).isEqualTo("This is my recipe.");
                assertThat(row.getOptions()).isEqualTo("[{\"name\":\"methodPattern\",\"displayName\":\"Method pattern\",\"description\":\"A method pattern that is used to find matching method declarations/invocations.\",\"example\":\"org.mockito.Matchers anyVararg()\"},{\"name\":\"newAccessLevel\",\"displayName\":\"New access level\",\"description\":\"New method access level to apply to the method, like \\\"public\\\".\",\"example\":\"public\",\"valid\":[\"private\",\"protected\",\"package\",\"public\"],\"required\":false}]");
            }),
          java(
            """
              import org.openrewrite.Option;
              import org.openrewrite.internal.lang.NonNullApi;
              import org.openrewrite.Recipe;
              import org.openrewrite.internal.lang.Nullable;
              
              @NonNullApi
              class MyRecipe extends Recipe {
                @Option(displayName = "Method pattern",
                        description = "A method pattern that is used to find matching method declarations/invocations.",
                        example = "org.mockito.Matchers anyVararg()")
                String methodPattern;
              
                @Option(displayName = "New access level",
                        description = "New method access level to apply to the method, like \\"public\\".",
                        example = "public",
                        valid = {"private", "protected", "package", "public"},
                        required = false)
                String newAccessLevel;
              
                @Override
                public String getDisplayName() {
                    return "My recipe";
                }
              
                @Override
                public String getDescription() {
                    return "This is my recipe.";
                }
              }
              """,
            """
              import org.openrewrite.Option;
              import org.openrewrite.internal.lang.NonNullApi;
              import org.openrewrite.Recipe;
              import org.openrewrite.internal.lang.Nullable;
              
              @NonNullApi
              class /*~~>*/MyRecipe extends Recipe {
                @Option(displayName = "Method pattern",
                        description = "A method pattern that is used to find matching method declarations/invocations.",
                        example = "org.mockito.Matchers anyVararg()")
                String methodPattern;
              
                @Option(displayName = "New access level",
                        description = "New method access level to apply to the method, like \\"public\\".",
                        example = "public",
                        valid = {"private", "protected", "package", "public"},
                        required = false)
                String newAccessLevel;
              
                @Override
                public String getDisplayName() {
                    return "My recipe";
                }
              
                @Override
                public String getDescription() {
                    return "This is my recipe.";
                }
              }
              """
          )
        );
    }

    @Test
    void returnInLambda() {
        rewriteRun(
          java(
            """
              import java.util.function.UnaryOperator;
              
              class SomeTest {
                  private final UnaryOperator<String> notEmpty = actual -> {
                      //noinspection CodeBlock2Expr
                      return actual + "\\n";
                  };
              }
              """
          )
        );
    }

    @Test
    void findRefasterRecipe() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
              """
                package org.openrewrite.java.template;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                @Target(ElementType.TYPE)
                public @interface RecipeDescriptor {
                    String name();
                    String description();
                }
                """
            ))
            .dataTable(RewriteRecipeSource.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                RewriteRecipeSource.Row row = rows.get(0);
                assertThat(row.getDisplayName()).isEqualTo("Some refaster rule");
                assertThat(row.getDescription()).isEqualTo("This is a refaster rule.");
            }),
          java(
            """
              import org.openrewrite.java.template.RecipeDescriptor;
              
              @RecipeDescriptor(
                  name = "Some refaster rule",
                  description = "This is a refaster rule."
              )
              class SomeRefasterRule {
              }
              """,
            """
              import org.openrewrite.java.template.RecipeDescriptor;
              
              /*~~>*/@RecipeDescriptor(
                  name = "Some refaster rule",
                  description = "This is a refaster rule."
              )
              class SomeRefasterRule {
              }
              """
          )
        );
    }

    @Test
    void findYamlRecipe() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
              """
                package org.openrewrite.java.template;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                @Target(ElementType.TYPE)
                public @interface RecipeDescriptor {
                    String name();
                    String description();
                }
                """
            ))
            .dataTable(RewriteRecipeSource.Row.class, rows -> {
                assertThat(rows).hasSize(2);
                assertThat(rows.get(0).getDisplayName()).isEqualTo("Migrates to Apache Commons Lang 3.x");
                assertThat(rows.get(0).getSourceCode()).startsWith(
                  "---\ntype: specs.openrewrite.org/v1beta/recipe\nname: org.openrewrite.apache.commons.lang.UpgradeApacheCommonsLang_2_3");
                assertThat(rows.get(1).getDisplayName()).isEqualTo("Migrates to Apache POI 3.17");
                assertThat(rows.get(1).getSourceCode()).startsWith(
                  "---\ntype: specs.openrewrite.org/v1beta/recipe\nname: org.openrewrite.apache.poi.UpgradeApachePoi_3_17");
            }),
          yaml(
            """
              # Apache Commons Lang
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.apache.commons.lang.UpgradeApacheCommonsLang_2_3
              displayName: Migrates to Apache Commons Lang 3.x
              description: >-
                Migrate applications to the latest Apache Commons Lang 3.x release. This recipe modifies\s
                application's build files, and changes the package as per [the migration release notes](https://commons.apache.org/proper/commons-lang/article3_0.html).
              tags:
                - apache
                - commons
                - lang
              recipeList:
                - org.openrewrite.java.dependencies.ChangeDependency:
                    oldGroupId: commons-lang
                    oldArtifactId: commons-lang
                    newGroupId: org.apache.commons
                    newArtifactId: commons-lang3
                    newVersion: 3.x
                - org.openrewrite.java.ChangePackage:
                    oldPackageName: org.apache.commons.lang
                    newPackageName: org.apache.commons.lang3
              ---
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.apache.poi.UpgradeApachePoi_3_17
              displayName: Migrates to Apache POI 3.17
              description: Migrates to the last Apache POI 3.x release. This recipe modifies build files and makes changes to deprecated/preferred APIs that have changed between versions.
              tags:
                - apache
                - poi
              recipeList:
                - org.openrewrite.java.dependencies.ChangeDependency:
                    oldGroupId: poi
                    oldArtifactId: poi
                    newGroupId: org.apache.poi
                    newArtifactId: poi
                    newVersion: 3.x
              """,
            spec -> spec.path("rewrite.yml").after(after -> {
                assertThat(after).contains("~~>");
                return after;
            })
          )
        );
    }
}
