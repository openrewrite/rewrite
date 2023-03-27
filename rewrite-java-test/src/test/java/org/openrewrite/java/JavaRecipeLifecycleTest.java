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
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.test.SourceSpecs.dir;

public class JavaRecipeLifecycleTest {
    @Issue("https://github.com/openrewrite/rewrite/discussions/2849")
    @Nested
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
        default void justMain() {
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
        default void mainAndTest() {
            rewriteRun(
              srcMainJava(java(MAIN_INITIAL, MAIN_SEARCH_RESULT)),
              srcTestJava(java(TEST_INITIAL, TEST_SEARCH_RESULT))
            );
        }
    }

    @Nested
    class FromYamlUsingHasSourceSetSourceSetFilteringTest implements SourceSetFilteringTest {
        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipeFromYaml("""
                ---
                type: specs.openrewrite.org/v1beta/recipe
                name: com.example.FilteringByMainTestSources
                displayName: Filtering Test By `main` Sources Test
                description: Only apply a `FindMethods` recipe to all sources when `main` sources are modified.
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
              "com.example.FilteringByMainTestSources"
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
                .addApplicableTest(new HasSourceSet("main"))
                .addApplicableTest(createFindMethods())
                .doNext(createFindMethods())
            );
        }
    }


    @Nested
    class FromYamlUsingIsNotTestSourceSourceSetFilteringTest implements SourceSetFilteringTest {
        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipeFromYaml("""
                ---
                type: specs.openrewrite.org/v1beta/recipe
                name: com.example.FilteringTestSources
                displayName: Filtering Test Sources Test
                description: Only apply a `FindMethods` recipe to all sources when non-test sources are modified.
                applicability:
                  anySource:
                  - org.openrewrite.java.search.IsLikelyNotTest
                  - org.openrewrite.java.search.FindMethods:
                      methodPattern: "java.io.PrintStream println(..)"
                recipeList:
                  - org.openrewrite.java.search.FindMethods:
                      methodPattern: "java.io.PrintStream println(..)"
                """,
              "com.example.FilteringTestSources"
            );
        }

        @Language("java")
        String PRODUCTION_INITIAL = """
          class Production {
            Production() {
              System.out.println("Hello World Production!");
            }
          }
          """;

        @Language("java")
        String PRODUCTION_SEARCH_RESULT = """
          class Production {
            Production() {
              /*~~>*/System.out.println("Hello World Production!");
            }
          }
          """;

        /**
         * A source set that isn't `main` or `test`, but is a source set likely used for production code.
         */
        private SourceSpecs srcProductionJava(SourceSpecs... javaSources) {
            return dir("src/production/java", spec -> sourceSet(spec, "production"), javaSources);
        }

        @Test
        void mainAndProductionAndTest() {
            rewriteRun(
              srcMainJava(java(MAIN_INITIAL, MAIN_SEARCH_RESULT)),
              srcProductionJava(java(PRODUCTION_INITIAL, PRODUCTION_SEARCH_RESULT)),
              srcTestJava(java(TEST_INITIAL, TEST_SEARCH_RESULT))
            );
        }

        @Test
        void productionAndTest() {
            rewriteRun(
              srcProductionJava(java(PRODUCTION_INITIAL, PRODUCTION_SEARCH_RESULT)),
              srcTestJava(java(TEST_INITIAL, TEST_SEARCH_RESULT))
            );
        }
    }

}
