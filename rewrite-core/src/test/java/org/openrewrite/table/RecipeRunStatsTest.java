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
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

@AllArgsConstructor
class RecipeWithApplicabilityTest extends Recipe {
    @Getter
    final String displayName = "Recipe with an applicability test";

    @Getter
    final String description = "This recipe is a test utility which exists to exercise RecipeRunStats.";

    @Option(displayName = "New text", example = "txt")
    String newText;

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
                  return tree.withText(newText);
              }
          }
        );
    }
}

class RecipeRunStatsTest implements RewriteTest {

    @DocumentExample
    @Test
    void singleRow() {
        rewriteRun(
          spec -> spec
            .recipe(new RecipeWithApplicabilityTest("sam"))
            .dataTable(RecipeRunStats.Row.class, rows -> {
              assertThat(rows)
                .as("Running a single recipe on a single source should produce a single row in the RecipeRunStats table")
                .hasSize(1);
              RecipeRunStats.Row row = rows.getFirst();
              assertThat(row.getRecipe()).endsWith("RecipeWithApplicabilityTest");
              assertThat(row.getSourceFiles())
                .isEqualTo(1);
              assertThat(row.getEditMaxNs()).isGreaterThan(0);
              assertThat(row.getEditTotalTimeNs())
                .as("Cumulative time should be greater than any single visit time")
                .isGreaterThan(row.getEditMaxNs());
          }),
          text("samuel", "sam")
        );
    }

    @Test
    void sourceFilesCountStatsForSameRecipe() {
        rewriteRun(
          spec -> spec
            .recipeFromYaml("""
                ---
                type: specs.openrewrite.org/v1beta/recipe
                name: org.openrewrite.SeveralMethodNameChangeRecipes
                description: Test.
                recipeList:
                  - org.openrewrite.table.RecipeWithApplicabilityTest:
                      newText: sam1
                  - org.openrewrite.table.RecipeWithApplicabilityTest:
                      newText: sam2
                """,
              "org.openrewrite.SeveralMethodNameChangeRecipes")
            .dataTable(RecipeRunStats.Row.class, rows -> {
                assertThat(rows)
                  .as("Running declarative recipe with parametrized recipe a single source should produce a two rows in the RecipeRunStats table")
                  .hasSize(2);
                for (RecipeRunStats.Row row : rows) {
                    assertThat(row.getSourceFiles())
                      .as("If the same recipe runs with different parameters it shouldn't increment several times source files count")
                      .isEqualTo(1);
                }
            }).expectedCyclesThatMakeChanges(2),
          text("sem", "sam2")
        );
    }
}
