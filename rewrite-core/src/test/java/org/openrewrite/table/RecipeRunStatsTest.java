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
package org.openrewrite.table;

import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

class RecipeRunStatsTest implements RewriteTest {

    @AllArgsConstructor
    static class RecipeWithApplicabilityTest extends Recipe {
        @Override
        public String getDisplayName() {
            return "Recipe with an applicability test";
        }

        @Override
        public String getDescription() {
            return "This recipe is a test utility which exists to exercise RecipeRunStats.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return Preconditions.check(
              new PlainTextVisitor<>() {
                  @Override
                  public PlainText visitText(PlainText text, ExecutionContext ctx) {
                      if (!"sam".equals(text.getText())) {
                          return SearchResult.found(text);
                      }
                      return text;
                  }
              },
              new PlainTextVisitor<>() {
                  @Override
                  public PlainText visitText(PlainText tree, ExecutionContext ctx) {
                      return tree.withText("sam");
                  }
              });
        }
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RecipeWithApplicabilityTest());
    }

    @DocumentExample
    @Test
    void singleRow() {
        rewriteRun(
          spec -> spec.dataTable(RecipeRunStats.Row.class, rows -> {
              assertThat(rows)
                .as("Running a single recipe on a single source should produce a single row in the RecipeRunStats table")
                .hasSize(1);
              RecipeRunStats.Row row = rows.get(0);
              assertThat(row.getRecipe()).endsWith("RecipeWithApplicabilityTest");
              assertThat(row.getSourceFiles())
                .as("Test framework will invoke the recipe once when it is expected to make a change, " +
                    "then once again when it is expected to make no change")
                .isEqualTo(2);
              assertThat(row.getEditMax()).isGreaterThan(0);
              assertThat(row.getEditTotalTime())
                .as("Cumulative time should be greater than any single visit time")
                .isGreaterThan(row.getEditMax());
          }),
          text("samuel", "sam")
        );
    }
}
