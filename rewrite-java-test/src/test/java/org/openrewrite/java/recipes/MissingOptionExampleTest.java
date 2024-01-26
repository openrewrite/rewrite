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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MissingOptionExampleTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MissingOptionExample())
          .parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()));
    }

    @DocumentExample
    @Test
    void lacksExample() {
        rewriteRun(
          java(
            """
              import org.openrewrite.Option;
              import org.openrewrite.Recipe;

              class SomeRecipe extends Recipe {
                  @Option(displayName = "Test", description = "Test")
                  private String test;
                            
                  @Override
                  public String getDisplayName() {
                      return "Find missing `@Option` `example` values";
                  }
                  @Override
                  public String getDescription() {
                      return "Find `@Option` annotations that are missing `example` values.";
                  }
              }
              """,
            """
              import org.openrewrite.Option;
              import org.openrewrite.Recipe;

              class SomeRecipe extends Recipe {
                  /*~~(Missing example value for documentation)~~>*/@Option(displayName = "Test", description = "Test")
                  private String test;
                            
                  @Override
                  public String getDisplayName() {
                      return "Find missing `@Option` `example` values";
                  }
                  @Override
                  public String getDescription() {
                      return "Find `@Option` annotations that are missing `example` values.";
                  }
              }
              """
          )
        );
    }

    @Test
    void hasExampleAlready() {
        rewriteRun(
          java(
            """
              import org.openrewrite.Option;
              import org.openrewrite.Recipe;

              class SomeRecipe extends Recipe {
                  @Option(displayName = "Test", description = "Test", example = "true")
                  private boolean test = true;
                            
                  @Override
                  public String getDisplayName() {
                      return "Find missing `@Option` `example` values";
                  }
                  @Override
                  public String getDescription() {
                      return "Find `@Option` annotations that are missing `example` values.";
                  }
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "boolean",
      "Boolean",
      "int",
      "Integer",
      "long",
      "Long"
    })
    void skipBoolean(String type) {
        rewriteRun(
          java(
            """
              import org.openrewrite.Option;
              import org.openrewrite.Recipe;

              class SomeRecipe extends Recipe {
                  @Option(displayName = "Test", description = "Test")
                  private %s test;
                            
                  @Override
                  public String getDisplayName() {
                      return "Find missing `@Option` `example` values";
                  }
                  @Override
                  public String getDescription() {
                      return "Find `@Option` annotations that are missing `example` values.";
                  }
              }
              """.formatted(type)
          )
        );
    }

    @Test
    void skipValidOptions() {
        rewriteRun(
          java(
            """
              import org.openrewrite.Option;
              import org.openrewrite.Recipe;

              class SomeRecipe extends Recipe {
                  @Option(displayName = "Test", description = "Test", valid = {"foo", "bar"})
                  private String test;
                            
                  @Override
                  public String getDisplayName() {
                      return "Find missing `@Option` `example` values";
                  }
                  @Override
                  public String getDescription() {
                      return "Find `@Option` annotations that are missing `example` values.";
                  }
              }
              """
          )
        );
    }
}
