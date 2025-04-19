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
package org.openrewrite;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.junit.jupiter.api.Test;
import org.openrewrite.table.TextMatches;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.text;

class DataTableTest implements RewriteTest {

    @Test
    void dataTable() {
        rewriteRun(
          spec -> spec
            .recipe(toRecipe(r -> new PlainTextVisitor<>() {
                final WordTable wordTable = new WordTable(r);

                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    int i = 0;
                    for (String s : text.getText().split(" ")) {
                        wordTable.insertRow(ctx, new WordTable.Row(i++, s));
                    }
                    return text;
                }
            }))
            .dataTableAsCsv(WordTable.class.getName(), """
              position,text
              0,hello
              1,world
              """
            )
            .dataTable(WordTable.Row.class, rows -> assertThat(rows.stream().map(WordTable.Row::getText))
              .containsExactly("hello", "world")),
          text("hello world")
        );
    }

    @Test
    void noRowsFromPrecondition() {
        rewriteRun(
          spec -> spec
            .recipe(toRecipe(r -> Preconditions.check(toRecipe(r2 -> new PlainTextVisitor<>() {
                final WordTable wordTable = new WordTable(r2);

                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    int i = 0;
                    for (String s : text.getText().split(" ")) {
                        wordTable.insertRow(ctx, new WordTable.Row(i++, s));
                    }
                    return text;
                }
            }), TreeVisitor.noop())))
            .afterRecipe(run -> {
                assertThat(run.getDataTables().keySet()).noneMatch(dt -> dt instanceof WordTable);
            }),
          text("hello world")
        );
    }

    @Test
    void noRowsFromDeclarativePrecondition() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
              """
                ---
                type: specs.openrewrite.org/v1beta/recipe
                name: test.recipe
                displayName: Recipe producing data table as precondition
                description: .
                preconditions:
                 - org.openrewrite.text.Find:
                     find: hello
                recipeList:
                  - org.openrewrite.Recipe$Noop
                """,
              "test.recipe"
            )
            .afterRecipe(run -> {
                assertThat(run.getDataTables().keySet()).noneMatch(dt -> dt instanceof TextMatches);
            }),
          text("hello world")
        );
    }

    @Test
    void rowsFromDeclarativeRecipe() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
              """
                ---
                type: specs.openrewrite.org/v1beta/recipe
                name: test.recipe
                displayName: Recipe producing data table in recipe list
                description: .
                recipeList:
                  - org.openrewrite.text.Find:
                      find: hello
                """,
              "test.recipe"
            )
            .afterRecipe(run -> {
                assertThat(run.getDataTables().keySet()).anyMatch(dt -> dt instanceof TextMatches);
            }),
          text("hello world", "~~>hello world")
        );
    }

    @Test
    void rowsFromSecondaryDeclarativeRecipe() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
              """
                ---
                type: specs.openrewrite.org/v1beta/recipe
                name: test.recipe
                displayName: Top-level recipe
                description: .
                recipeList:
                  - test.recipe2
                ---
                type: specs.openrewrite.org/v1beta/recipe
                name: test.recipe2
                displayName: Recipe producing data table
                description: .
                recipeList:
                  - org.openrewrite.text.Find:
                      find: hello
                """,
              "test.recipe"
            )
            .afterRecipe(run -> {
                assertThat(run.getDataTables().keySet()).anyMatch(dt -> dt instanceof TextMatches);
            }),
          text("hello world", "~~>hello world")
        );
    }

    @Test
    void rowsFromDeclarativeRecipeWithPrecondition() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
              """
                ---
                type: specs.openrewrite.org/v1beta/recipe
                name: test.recipe
                displayName: Recipe producing data table as precondition and in recipe list
                description: .
                preconditions:
                 - org.openrewrite.text.Find:
                     find: hello
                recipeList:
                  - org.openrewrite.text.Find:
                      find: hello
                """,
              "test.recipe"
            )
            .afterRecipe(run -> {
                assertThat(run.getDataTables().keySet()).anyMatch(dt -> dt instanceof TextMatches);
            }),
          text("hello world", "~~>hello world")
        );
    }

    @Test
    void descriptor() {
        Recipe recipe = toRecipe();
        new WordTable(recipe);

        assertThat(recipe.getDataTableDescriptors()).hasSize(4);
        assertThat(recipe.getDataTableDescriptors().get(0).getColumns()).hasSize(2);
    }

    @JsonIgnoreType
    static class WordTable extends DataTable<WordTable.Row> {
        public WordTable(Recipe recipe) {
            super(recipe, "Words", "Each word in the text.");
        }

        static class Row {
            @Column(displayName = "Position", description = "The index position of the word in the text.")
            private int position;

            @Column(displayName = "Text", description = "The text of the word.")
            private String text;

            public Row() {
            }

            public Row(int position, String text) {
                this.position = position;
                this.text = text;
            }

            public int getPosition() {
                return position;
            }

            public String getText() {
                return text;
            }
        }
    }
}
