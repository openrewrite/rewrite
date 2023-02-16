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
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.text;

public class DataTableTest implements RewriteTest {

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
        assertThat(recipe.getDataTableDescriptors().get(0).getColumns()).hasSize(2);
    }

    @JsonIgnoreType
    static class WordTable extends DataTable<WordTable.Row> {
        public WordTable(Recipe recipe) {
            super(recipe, Row.class, WordTable.class.getName(),
              "Words", "Each word in the text.");
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
