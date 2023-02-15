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
package org.openrewrite.java.filter;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.SourceSpecs.dir;

public class SourceApplicabilityFilterTest {
    @Language("java")
    private static final String PRODUCTION_INITIAL = """
      class Production {
        Production() {
          System.out.println("Hello World!");
        }
      }
      """;

    @Language("java")
    private static final String PRODUCTION_SEARCH_RESULT = """
      class Production {
        Production() {
          /*~~>*/System.out.println("Hello World!");
        }
      }
      """;

    @Language("java")
    private static final String TEST_INITIAL = """
      class Test {
        Test() {
          System.out.println("Hello World!");
        }
      }
      """;

    @Language("java")
    private static final String TEST_SEARCH_RESULT = """
      class Test {
        Test() {
          /*~~>*/System.out.println("Hello World!");
        }
      }
      """;

    @Nested
    class AllSourceTest implements RewriteTest {
        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipe(new CompositeRecipe()
              .doNext(new SourceApplicabilityFilter(SourceApplicabilityFilter.Target.ALL_SOURCE))
              .doNext(new FindMethods("java.io.PrintStream println(..)", true, "none")));
        }

        @Test
        void justProduction() {
            rewriteRun(
              dir("src/main/java/org/openrewrite/java/", java(PRODUCTION_INITIAL, PRODUCTION_SEARCH_RESULT))
            );
        }

        @Test
        void justTest() {
            rewriteRun(
              dir("src/test/java/org/openrewrite/java/", java(TEST_INITIAL, TEST_SEARCH_RESULT))
            );
        }

        @Test
        void productionAndTest() {
            rewriteRun(
              dir("src/main/java/org/openrewrite/java/", java(PRODUCTION_INITIAL, PRODUCTION_SEARCH_RESULT)),
              dir("src/test/java/org/openrewrite/java/", java(TEST_INITIAL, TEST_SEARCH_RESULT))
            );
        }
    }

    @Nested
    class AllSourceIfDetectedInNonTestTest implements RewriteTest {
        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipe(new CompositeRecipe()
              .doNext(new SourceApplicabilityFilter(SourceApplicabilityFilter.Target.ALL_SOURCE_IF_DETECTED_IN_NON_TEST))
              .doNext(new FindMethods("java.io.PrintStream println(..)", true, "none")));
        }

        @Test
        void justProduction() {
            rewriteRun(
              dir("src/main/java/org/openrewrite/java/", java(PRODUCTION_INITIAL, PRODUCTION_SEARCH_RESULT))
            );
        }

        @Test
        void justTest() {
            rewriteRun(
              dir("src/test/java/org/openrewrite/java/", java(TEST_INITIAL))
            );
        }

        @Test
        void productionAndTest() {
            rewriteRun(
              dir("src/main/java/org/openrewrite/java/", java(PRODUCTION_INITIAL, PRODUCTION_SEARCH_RESULT)),
              dir("src/test/java/org/openrewrite/java/", java(TEST_INITIAL, TEST_SEARCH_RESULT))
            );
        }
    }

    @Nested
    class NonTestSourceTest implements RewriteTest {
        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipe(new CompositeRecipe()
              .doNext(new SourceApplicabilityFilter(SourceApplicabilityFilter.Target.NON_TEST_SOURCE))
              .doNext(new FindMethods("java.io.PrintStream println(..)", true, "none")));
        }

        @Test
        void justProduction() {
            rewriteRun(
              dir("src/main/java/org/openrewrite/java/", java(PRODUCTION_INITIAL, PRODUCTION_SEARCH_RESULT))
            );
        }

        @Test
        void justTest() {
            rewriteRun(
              dir("src/test/java/org/openrewrite/java/", java(TEST_INITIAL))
            );
        }

        @Test
        void productionAndTest() {
            rewriteRun(
              dir("src/main/java/org/openrewrite/java/", java(PRODUCTION_INITIAL, PRODUCTION_SEARCH_RESULT)),
              dir("src/test/java/org/openrewrite/java/", java(TEST_INITIAL))
            );
        }
    }

}
