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
package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.table.ParseFailures;
import org.openrewrite.table.TextMatches;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;
import org.openrewrite.text.Find;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

class RecipeRunTest implements RewriteTest {
    @DocumentExample
    @Test
    void multipleRecipeInstancesProduceSeparateScopedDataTables() {
        rewriteRun(
          recipeSpec -> recipeSpec.recipes(
              new Find("hello", null, null, null, null, null, null, null),
              new Find("world", null, null, null, null, null, null, null)
            )
            .afterRecipe(recipeRun -> {
                DataTableStore store = recipeRun.getDataTableStore();

                // Each Find instance gets its own scoped data table bucket
                long textMatchTables = store.getDataTables().stream()
                        .filter(dt -> dt.getName().equals(TextMatches.class.getName()))
                        .count();
                assertThat(textMatchTables).isGreaterThanOrEqualTo(2);
            }),
          text(
            """
              hello
              """,
            """
              ~~>hello
              """,
            spec -> spec.path("file1.txt")
          ),
          text(
            """
              world
              """,
            """
              ~~>world
              """,
            spec -> spec.path("file2.txt")
          )
        );
    }

    @Test
    void dataTableWithMultilineValue() {
        rewriteRun(
          recipeSpec -> recipeSpec.recipes(new FindParseFailures(null, null, null, null))
            .afterRecipe(recipeRun -> {
                DataTableStore store = recipeRun.getDataTableStore();
                DataTable<?> parseFailuresDt = store.getDataTables().stream()
                        .filter(dt -> dt.getName().equals(ParseFailures.class.getName()))
                        .findFirst().orElseThrow();

                // Verify the row has multiline content
                List<?> rows = store.getRows(parseFailuresDt.getName(), parseFailuresDt.getGroup())
                        .collect(java.util.stream.Collectors.toList());
                assertThat(rows).hasSize(1);
            }),
          text(
            """
              hello
              """,
            """
              ~~(multiline
              message)~~>hello
              """,
            spec -> spec.markers(new ParseExceptionResult(
              Tree.randomId(), "parserType", "exceptionType", "multiline\nmessage", null))
          )
        );
    }

    @Test
    void delegateRecipeWithOnComplete() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        rewriteRun(recipeSpec -> recipeSpec.recipe(new DelegatingRecipe()).executionContext(ctx).typeValidationOptions(TypeValidation.none()));
        assertThat(ctx.<String>getMessage("org.openrewrite.recipe.oncomplete")).isEqualTo("with delegate recipe.");
    }

    public static class DelegatingRecipe extends Recipe implements Recipe.DelegatingRecipe {

        @Override
        public String getDisplayName() {
            return "Test delegate recipe";
        }

        @Override
        public String getDescription() {
            return "Test onComplete with delegate recipe.";
        }

        @Override
        public Recipe getDelegate() {
            return new Recipe() {
                @Override
                public String getDisplayName() {
                    return "Actual recipe";
                }

                @Override
                public String getDescription() {
                    return "Actual recipe with onComplete.";
                }

                @Override
                public void onComplete(ExecutionContext ctx) {
                    ctx.putMessage("org.openrewrite.recipe.oncomplete", "with delegate recipe.");
                }
            };
        }
    }
}
