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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RecipeEqualsAndHashCodeCallSuperTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RecipeEqualsAndHashCodeCallSuper())
          .parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()));
    }

    @Test
    void recipeEqualsAndHashCodeCallSuper() {
        rewriteRun(
          java(
            """
              import org.openrewrite.Recipe;
              import lombok.EqualsAndHashCode;
              import lombok.Value;
              
              @Value
              @EqualsAndHashCode(callSuper = true)
              class MyRecipe extends Recipe {
              }
              """,
            """
              import org.openrewrite.Recipe;
              import lombok.EqualsAndHashCode;
              import lombok.Value;
              
              @Value
              @EqualsAndHashCode(callSuper = false)
              class MyRecipe extends Recipe {
              }
              """
          )
        );
    }

    @Test
    void retainExclude() {
        rewriteRun(
          java(
            """
              import org.openrewrite.Recipe;
              import lombok.EqualsAndHashCode;
              import lombok.Value;
              
              @Value
              @EqualsAndHashCode(callSuper = true, exclude = "messages")
              class MyRecipe extends Recipe {
                  String messages;
              }
              """,
            """
              import org.openrewrite.Recipe;
              import lombok.EqualsAndHashCode;
              import lombok.Value;
              
              @Value
              @EqualsAndHashCode(callSuper = false, exclude = "messages")
              class MyRecipe extends Recipe {
                  String messages;
              }
              """
          )
        );
    }

    @Test
    void skipFalse() {
        rewriteRun(
          java(
            """
              import org.openrewrite.Recipe;
              import lombok.EqualsAndHashCode;
              import lombok.Value;
              
              @Value
              @EqualsAndHashCode(callSuper = false, exclude = "messages")
              class MyRecipe extends Recipe {
                  String messages;
              }
              """
          )
        );
    }
}
