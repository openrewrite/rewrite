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
import org.openrewrite.java.JavaParser;
import org.openrewrite.table.RewriteRecipeSource;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

public class FindRecipesTest implements RewriteTest {

    @Test
    void findRecipes() {
        rewriteRun(
          spec -> spec
            .recipe(new FindRecipes())
            .parser(JavaParser.fromJavaVersion()
              .classpath(JavaParser.runtimeClasspath()))
            .expectedCyclesThatMakeChanges(1)
            .cycles(1)
            .dataTable(RewriteRecipeSource.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                RewriteRecipeSource.Row row = rows.get(0);
                assertThat(row.getDisplayName()).isEqualTo("My recipe");
                assertThat(row.getDescription()).isEqualTo("This is my recipe.");
            }),
          java(
            """
              import org.openrewrite.internal.lang.NonNullApi;
              import org.openrewrite.Recipe;
              
              @NonNullApi
              class MyRecipe extends Recipe {
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
              import org.openrewrite.internal.lang.NonNullApi;
              import org.openrewrite.Recipe;
              
              @NonNullApi
              class /*~~>*/MyRecipe extends Recipe {
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
}
