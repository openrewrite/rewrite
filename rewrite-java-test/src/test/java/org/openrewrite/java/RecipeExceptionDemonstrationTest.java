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
package org.openrewrite.java;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.SourceSpecs.text;

class RecipeExceptionDemonstrationTest implements RewriteTest {

    @Test
    void getVisitorOnMatchingMethod() {
        rewriteRun(
          spec -> spec
            .recipe(new RecipeExceptionDemonstration("java.util.List add(..)", null, null,
              null, null, null, null))
            .afterRecipe(run -> assertThat(run.getResults().get(0).getRecipes().iterator().next()).isNotNull())
            .executionContext(new InMemoryExecutionContext()),
          java(
            """
              import java.util.*;
              class Test {
                  void test(List<Integer> list) {
                      list.add(42);
                  }
              }
              """,
            """
              import java.util.*;
              class Test {
                  void test(List<Integer> list) {
                      /*~~(Demonstrating an exception thrown on a matching method.)~~>*/list.add(42);
                  }
              }
              """
          )
        );
    }

    @Test
    void applicableTest() {
        rewriteRun(
          spec -> spec
            .recipe(new RecipeExceptionDemonstration(null, null, null,
              true, null, null, null))
            .afterRecipe(run ->
              assertThat(run.getResults().get(0).getRecipes().iterator().next()).isNotNull())
            .executionContext(new InMemoryExecutionContext()),
          java(
            """
              import java.util.*;
              class Test {
                  void test(List<Integer> list) {
                      list.add(42);
                  }
              }
              """
          ),
          text(
            null,
            "~~(Throwing on the project-level applicable test.)~~>" +
            "Rewrite encountered an uncaught recipe error in org.openrewrite.java.RecipeExceptionDemonstration.",
            spec -> spec.path("recipe-exception-1.txt")
          )
        );
    }

    @Disabled(value = "The exception thrown in getSingleSourceApplicableTest() is caught by RecipeScheduler, so disable this.")
    @Test
    void singleSourceApplicableTest() {
        rewriteRun(
          spec -> spec
            .recipe(new RecipeExceptionDemonstration(null, null, null,
              null, null, true, null))
            .afterRecipe(run ->
              assertThat(run.getResults().get(0).getRecipes().iterator().next()).isNotNull())
            .executionContext(new InMemoryExecutionContext()),
          java(
            """
              import java.util.*;
              class Test {
                  void test(List<Integer> list) {
                      list.add(42);
                  }
              }
              """,
            """
              /*~~(Demonstrating an exception thrown on the single-source applicable test.)~~>*/import java.util.*;
              class Test {
                  void test(List<Integer> list) {
                      list.add(42);
                  }
              }
              """
          )
        );
    }

    @Test
    void applicableTestVisitor() {
        rewriteRun(
          spec ->
            spec
              .recipe(new RecipeExceptionDemonstration(null, null, null,
                null, true, null, null))
              .afterRecipe(run ->
                assertThat(run.getResults().get(0).getRecipes().iterator().next()).isNotNull())
              .executionContext(new InMemoryExecutionContext()),
          java(
            """
              import java.util.*;
              class Test {
                  void test(List<Integer> list) {
                      list.add(42);
                  }
              }
              """,
            """
              /*~~(Throwing on the project-level applicable test.)~~>*/import java.util.*;
              class Test {
                  void test(List<Integer> list) {
                      list.add(42);
                  }
              }
              """
          )
        );
    }

    @Test
    void visitAllVisitor() {
        rewriteRun(
          spec ->
            spec
              .recipe(new RecipeExceptionDemonstration(null, null, true,
                null, null, null, null))
              .afterRecipe(run ->
                assertThat(run.getResults().get(0).getRecipes().iterator().next()).isNotNull())
              .executionContext(new InMemoryExecutionContext()),
          java(
            """
              import java.util.*;
              class Test {
                  void test(List<Integer> list) {
                      list.add(42);
                  }
              }
              """,
            """
              /*~~(Demonstrating an exception thrown in the recipe's `visit(List<SourceFile>, ExecutionContext)` method.)~~>*/import java.util.*;
              class Test {
                  void test(List<Integer> list) {
                      list.add(42);
                  }
              }
              """
          )
        );
    }

    @Test
    void visitAll() {
        rewriteRun(
          spec -> spec
            .recipe(new RecipeExceptionDemonstration(null, true, null,
              null, null, null, null))
            .afterRecipe(run ->
              assertThat(run.getResults().get(0).getRecipes().iterator().next()).isNotNull())
            .executionContext(new InMemoryExecutionContext()),
          java(
            """
              import java.util.*;
              class Test {
                  void test(List<Integer> list) {
                      list.add(42);
                  }
              }
              """
          ),
          text(
            null,
            "~~(Demonstrating an exception thrown in the recipe's `visit(List<SourceFile>, ExecutionContext)` method.)~~>" +
            "Rewrite encountered an uncaught recipe error in org.openrewrite.java.RecipeExceptionDemonstration.",
            spec -> spec.path("recipe-exception-1.txt")
          )
        );
    }
}
