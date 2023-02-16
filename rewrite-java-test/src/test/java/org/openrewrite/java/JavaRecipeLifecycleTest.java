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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.Recipe;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.search.HasSourceSet;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;

public class JavaRecipeLifecycleTest {
    @Issue("https://github.com/openrewrite/rewrite/discussions/2849")
    interface SourceSetFilteringTest extends RewriteTest {
        @Language("java")
        String MAIN_INITIAL = """
          class Main {
            Main() {
              System.out.println("Hello World!");
            }
          }
          """;

        @Language("java")
        String MAIN_SEARCH_RESULT = """
          class Main {
            Main() {
              /*~~>*/System.out.println("Hello World!");
            }
          }
          """;

        @Language("java")
        String TEST_INITIAL = """
          class Test {
            Test() {
              System.out.println("Hello World!");
            }
          }
          """;

        @Language("java")
        String TEST_SEARCH_RESULT = """
          class Test {
            Test() {
              /*~~>*/System.out.println("Hello World!");
            }
          }
          """;

        @Test
        default void justProduction() {
            rewriteRun(
              srcMainJava(java(MAIN_INITIAL, MAIN_SEARCH_RESULT))
            );
        }

        @Test
        default void justTest() {
            rewriteRun(
              srcTestJava(java(TEST_INITIAL))
            );
        }

        @Test
        default void productionAndTest() {
            rewriteRun(
              srcMainJava(java(MAIN_INITIAL, MAIN_SEARCH_RESULT)),
              srcTestJava(java(TEST_INITIAL, TEST_SEARCH_RESULT))
            );
        }
    }

    @Nested
    class FromYamlSourceSetFilteringTest implements SourceSetFilteringTest {
        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipeFromYaml("""
                ---
                type: specs.openrewrite.org/v1beta/recipe
                name: com.example.SecurityModifyRecipe
                displayName: Do all the things for both
                description: Do all the things for both.
                applicability:
                  anySource:
                  - org.openrewrite.java.search.HasSourceSet:
                      sourceSet: "main"
                  - org.openrewrite.java.search.FindMethods:
                      methodPattern: "java.io.PrintStream println(..)"
                recipeList:
                  - org.openrewrite.java.search.FindMethods:
                      methodPattern: "java.io.PrintStream println(..)"
                """,
              "com.example.SecurityModifyRecipe"
            );
        }
    }

    @Nested
    class FromRecipeObjectsSourceSetFilteringTest implements SourceSetFilteringTest {

        private static FindMethods createFindMethods() {
            return new FindMethods("java.io.PrintStream println(..)", true, "none");
        }

        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipe(
              Recipe.noop()
                .addApplicableTest(new HasSourceSet("main").getVisitor())
                .addApplicableTest(createFindMethods().getVisitor())
                .doNext(createFindMethods())
            );
        }
    }

}
