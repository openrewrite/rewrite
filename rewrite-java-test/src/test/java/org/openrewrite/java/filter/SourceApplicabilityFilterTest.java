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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.search.HasSourceSet;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Properties;

import static org.openrewrite.java.Assertions.*;

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

    private static FindMethods createFindMethods() {
        return new FindMethods("java.io.PrintStream println(..)", true, "none");
    }

    @Nested
    class AllSourceTest implements RewriteTest {
        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipe(new CompositeRecipe()
              .doNext(new SourceApplicabilityFilter(SourceApplicabilityFilter.Target.ALL_SOURCE))
              .doNext(createFindMethods()));
        }

        @Test
        void justProduction() {
            rewriteRun(
              srcMainJava(java(PRODUCTION_INITIAL, PRODUCTION_SEARCH_RESULT))
            );
        }

        @Test
        void justTest() {
            rewriteRun(
              srcTestJava(java(TEST_INITIAL, TEST_SEARCH_RESULT))
            );
        }

        @Test
        void productionAndTest() {
            rewriteRun(
              srcMainJava(java(PRODUCTION_INITIAL, PRODUCTION_SEARCH_RESULT)),
              srcTestJava(java(TEST_INITIAL, TEST_SEARCH_RESULT))
            );
        }
    }

    @Nested
    class AllSourceIfDetectedInNonTestTest implements RewriteTest {

        private static CompositeRecipe createMainRecipe() {
            return (CompositeRecipe) new CompositeRecipe()
              .addApplicableTest(new HasSourceSet("main").getVisitor())
              .addApplicableTest(createFindMethods().getVisitor())
              .doNext(createFindMethods());
        }

        private static CompositeRecipe createTestRecipe() {
            return (CompositeRecipe) new CompositeRecipe()
              .addSingleSourceApplicableTest(new HasSourceSet("test").getVisitor())
              .addSingleSourceApplicableTest(createFindMethods().getVisitor())
              .doNext(createFindMethods());
        }

//        @Override
//        public void defaults(RecipeSpec spec) {
//            spec.recipe(new CompositeRecipe()
//              .addApplicableTest(getVisitor(createMainRecipe()))
//              .addApplicableTest(getVisitor(createTestRecipe()))
//              .doNext(createMainRecipe())
//              .doNext(createTestRecipe()));
//        }


        @Override
        public void defaults(RecipeSpec spec) {
            String yamlRecipe = """
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
""";
            spec.recipe(Environment.builder()
              .scanRuntimeClasspath()
              .load(
                new YamlResourceLoader(
                  new ByteArrayInputStream(yamlRecipe.getBytes(StandardCharsets.UTF_8)),
                  Paths.get("applicability.yml").toUri(),
                  new Properties()))
              .build()
              .activateRecipes("com.example.SecurityModifyRecipe"));
        }

        private static TreeVisitor<?, ExecutionContext> getVisitor(Recipe recipe) {
            try {
                Method getVisitor = Recipe.class.getDeclaredMethod("getVisitor");
                getVisitor.setAccessible(true);
                //noinspection unchecked
                return (TreeVisitor<?, ExecutionContext>) getVisitor.invoke(recipe);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        void justProduction() {
            rewriteRun(
              srcMainJava(java(PRODUCTION_INITIAL, PRODUCTION_SEARCH_RESULT))
            );
        }

        @Test
        void justTest() {
            rewriteRun(
              srcTestJava(java(TEST_INITIAL))
            );
        }

        @Test
        void productionAndTest() {
            rewriteRun(
              srcMainJava(java(PRODUCTION_INITIAL, PRODUCTION_SEARCH_RESULT)),
              srcTestJava(java(TEST_INITIAL, TEST_SEARCH_RESULT))
            );
        }
    }

    @Nested
    class NonTestSourceTest implements RewriteTest {
        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipe(new CompositeRecipe()
              .doNext(new SourceApplicabilityFilter(SourceApplicabilityFilter.Target.NON_TEST_SOURCE))
              .doNext(createFindMethods()));
        }

        @Test
        void justProduction() {
            rewriteRun(
              srcMainJava(java(PRODUCTION_INITIAL, PRODUCTION_SEARCH_RESULT))
            );
        }

        @Test
        void justTest() {
            rewriteRun(
              srcTestJava(java(TEST_INITIAL))
            );
        }

        @Test
        void productionAndTest() {
            rewriteRun(
              srcMainJava(java(PRODUCTION_INITIAL, PRODUCTION_SEARCH_RESULT)),
              srcTestJava(java(TEST_INITIAL))
            );
        }
    }

}
