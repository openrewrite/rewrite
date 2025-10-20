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
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.util.List;

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
    void descriptor() {
        Recipe recipe = toRecipe();
        new WordTable(recipe);
        assertThat(recipe.getDataTableDescriptors()).hasSize(4);
        assertThat(recipe.getDataTableDescriptors().getFirst().getColumns()).hasSize(2);
    }

    @Test
    void multipleRecipesWriteToSameDataTable() {
        rewriteRun(
          spec -> spec
            .recipe(new CompositeRecipe(List.of(
              toRecipe(r -> new PlainTextVisitor<>() {
                  final WordTable wordTable = new WordTable(r);

                  @Override
                  public PlainText visitText(PlainText text, ExecutionContext ctx) {
                      wordTable.insertRow(ctx, new WordTable.Row(0, "first"));
                      return text;
                  }
              }),
              toRecipe(r -> new PlainTextVisitor<>() {
                  final WordTable wordTable = new WordTable(r);

                  @Override
                  public PlainText visitText(PlainText text, ExecutionContext ctx) {
                      wordTable.insertRow(ctx, new WordTable.Row(1, "second"));
                      return text;
                  }
              })
            )))
            .dataTable(WordTable.Row.class, rows -> assertThat(rows.stream().map(WordTable.Row::getText))
              .containsExactlyInAnyOrder("first", "second")),
          text("test")
        );
    }

    @Test
    void recipeUsedInPreconditionDoesNotEmitDataTableRows() {
        Recipe preconditionRecipe = toRecipe(r -> new PlainTextVisitor<>() {
            final WordTable wordTable = new WordTable(r);

            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                // This row should NOT be emitted because it's in a precondition
                wordTable.insertRow(ctx, new WordTable.Row(0, "precondition"));
                return text.withText("modified");
            }
        });

        rewriteRun(
          spec -> spec
            .recipe(toRecipe(r -> Preconditions.check(
              preconditionRecipe,
              new PlainTextVisitor<>() {
                  final WordTable wordTable = new WordTable(r);

                  @Override
                  public PlainText visitText(PlainText text, ExecutionContext ctx) {
                      wordTable.insertRow(ctx, new WordTable.Row(1, "main"));
                      return text.withText("changed");
                  }
              }
            )))
            .dataTable(WordTable.Row.class, rows -> assertThat(rows.stream().map(WordTable.Row::getText))
              // Only "main" should be present, not "precondition"
              .containsExactly("main")),
          text("test", "changed")
        );
    }

    @JsonIgnoreType
    static class WordTable extends DataTable<WordTable.Row> {
        public WordTable(Recipe recipe) {
            super(recipe, "Words", "Each word in the text.");
        }

        @Value
        static class Row {
            @Column(displayName = "Position", description = "The index position of the word in the text.")
            int position;

            @Column(displayName = "Text", description = "The text of the word.")
            String text;
        }
    }
}
