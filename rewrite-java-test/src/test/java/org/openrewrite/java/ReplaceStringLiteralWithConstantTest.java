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

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class ReplaceStringLiteralWithConstantTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true));
    }

    @Test
    void shouldReadValuesInOrderProperties() {
        ReplaceStringLiteralWithConstant recipe = (ReplaceStringLiteralWithConstant) RecipeSpec.defaults().recipeFromYaml("""
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.ReplaceStringLiteralWithConstantList
            recipeList:
                - org.openrewrite.java.ReplaceStringLiteralWithConstant:
                    literalValue: %s
                    fullyQualifiedConstantName: %s
            """.formatted("default", EXAMPLE_STRING_FQN),
          "org.openrewrite.ReplaceStringLiteralWithConstantList"
        ).getRecipe().getRecipeList().get(0).getRecipeList().get(0);
        assertThat(recipe.getFullyQualifiedConstantName()).isEqualTo(EXAMPLE_STRING_FQN);
        assertThat(recipe.getLiteralValue()).isEqualTo("default");
    }

    @Test
    void shouldReadValuesInvertedOrderProperties() {
        ReplaceStringLiteralWithConstant recipe = (ReplaceStringLiteralWithConstant) RecipeSpec.defaults().recipeFromYaml("""
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.ReplaceStringLiteralWithConstantList
            recipeList:
                - org.openrewrite.java.ReplaceStringLiteralWithConstant:
                    fullyQualifiedConstantName: %s
                    literalValue: %s
            """.formatted(EXAMPLE_STRING_FQN, "default"),
          "org.openrewrite.ReplaceStringLiteralWithConstantList"
        ).getRecipe().getRecipeList().get(0).getRecipeList().get(0);
        assertThat(recipe.getFullyQualifiedConstantName()).isEqualTo(EXAMPLE_STRING_FQN);
        assertThat(recipe.getLiteralValue()).isEqualTo("default");
    }

    @Test
    void shouldUseConstantValueAsDefaultLiteralValue() {
        ReplaceStringLiteralWithConstant recipe = (ReplaceStringLiteralWithConstant) RecipeSpec.defaults().recipeFromYaml("""
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.ReplaceStringLiteralWithConstantList
            recipeList:
                - org.openrewrite.java.ReplaceStringLiteralWithConstant:
                    fullyQualifiedConstantName: %s
            """.formatted(EXAMPLE_STRING_FQN),
          "org.openrewrite.ReplaceStringLiteralWithConstantList"
        ).getRecipe().getRecipeList().get(0).getRecipeList().get(0);
        assertThat(recipe.getFullyQualifiedConstantName()).isEqualTo(EXAMPLE_STRING_FQN);
        assertThat(recipe.getLiteralValue()).isEqualTo(EXAMPLE_STRING_CONSTANT);
    }

    @Test
    void doNothingIfStringLiteralNotFound() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant("Hello World!", EXAMPLE_STRING_FQN)),
          java("""
            package org.openrewrite.java;
                            
            class Test {
                String s = "FooBar";
            }
            """
          )
        );
    }

    @Test
    void doNotReplaceDefinition() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant("Hello World!", EXAMPLE_STRING_FQN)),
          java(
            """
              package org.openrewrite.java;
                          
              class ReplaceStringLiteralWithConstantTest {
                  static final String EXAMPLE_STRING_CONSTANT = "Hello World!";
              }
              """
          ));
    }

    @Test
    void shouldNotAddImportWhenUnnecessary() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant("Hello World!", EXAMPLE_STRING_FQN)),
          java(
            """
              package org.openrewrite.java;
                              
              class Test {
                  Object o = "Hello World!";
              }
              """,
            """                
              package org.openrewrite.java;
                          
              class Test {
                  Object o = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
              }
              """
          )
        );
    }

    @Test
    void replaceStringLiteralWithConstant() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant("Hello World!", EXAMPLE_STRING_FQN)),
          java(
            """
              class Test {
                  Object o = "Hello World!";
              }
              """,
            """                
              import org.openrewrite.java.ReplaceStringLiteralWithConstantTest;
                          
              class Test {
                  Object o = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
              }
              """
          )
        );
    }

    @Test
    void replaceLiteralWithUserDefinedConstant() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant("newValue", EXAMPLE_STRING_FQN)),
          java(
            """
              package com.abc;
                          
              class A {
                  String v = "newValue";
                  private String method() {
                      return "newValue";
                  }
              }
              """,
            """
              package com.abc;
                          
              import org.openrewrite.java.ReplaceStringLiteralWithConstantTest;
                          
              class A {
                  String v = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
                  private String method() {
                      return ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceStringLiteralWithConstantValueWhenLiteralValueIsNotConfigured() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant(EXAMPLE_STRING_FQN)),
          java(
            """ 
              class Test {
                  Object o = "Hello World!";
              }
              """,
            """
              import org.openrewrite.java.ReplaceStringLiteralWithConstantTest;
                          
              class Test {
                  Object o = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
              }
              """
          )
        );
    }

    @Test
    void replaceStringLiteralWithConstantValueWhenLiteralValueIsConfiguredNull() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant(null, EXAMPLE_STRING_FQN)),
          java(
            """
              class Test {
                  Object o = "Hello World!";
              }
              """,
            """
              import org.openrewrite.java.ReplaceStringLiteralWithConstantTest;
                          
              class Test {
                  Object o = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
              }
              """
          )
        );
    }

    @Test
    void replaceStringLiteralWithLiteralValueWhenLiteralValueIsConfiguredEmpty() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant("", EXAMPLE_STRING_FQN)),
          java(
            """
              class Test {
                  Object o = "";
              }
              """,
            """
              import org.openrewrite.java.ReplaceStringLiteralWithConstantTest;
                          
              class Test {
                  Object o = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
              }
              """
          )
        );
    }

    @Test
    void replaceStringLiteralWithLiteralValueWhenLiteralValueIsConfiguredBlank() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceStringLiteralWithConstant(" ", EXAMPLE_STRING_FQN)),
          java(
            """                
              class Test {
                  Object o = " ";
              }
              """,
            """
              import org.openrewrite.java.ReplaceStringLiteralWithConstantTest;
                          
              class Test {
                  Object o = ReplaceStringLiteralWithConstantTest.EXAMPLE_STRING_CONSTANT;
              }
              """
          )
        );
    }

    public static String EXAMPLE_STRING_FQN = ReplaceStringLiteralWithConstantTest.class.getName() + ".EXAMPLE_STRING_CONSTANT";
    public static String EXAMPLE_STRING_CONSTANT = "Hello World!";
}
