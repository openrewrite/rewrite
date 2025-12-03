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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.openrewrite.test.SourceSpecs.text;

class SearchResultsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromYaml(
          //language=yml
          """
            type: specs.openrewrite.org/v1beta/recipe
            name: test.SearchMarkerScraping
            displayName: Find text and add the results to the global datatable
            description: Hello world.
            recipeList:
              - org.openrewrite.text.Find:
                  find: hi
                  caseSensitive: true
              - org.openrewrite.text.Find:
                  find: SOME OTHER TEXT
              - org.openrewrite.text.Find:
                  find: ([A-Z])\\w+
                  regex: true
                  caseSensitive: true
            """,
          "test.SearchMarkerScraping"
        );
    }

    @DocumentExample
    @Test
    void searchMarkersAreDetectedDuringRecipeRun() {
        rewriteRun(
          spec -> spec.dataTable(SearchResults.Row.class, rows -> {
                assertThat(rows).extracting(
                    SearchResults.Row::getSourcePath,
                    SearchResults.Row::getResult,
                    SearchResults.Row::getParentRecipe,
                    SearchResults.Row::getRecipe)
                  .containsExactlyInAnyOrder(
                    tuple("matched", "hi", "Find text and add the results to the global datatable", "Find text `hi`"),
                    tuple("nested/matched", "hi", "Find text and add the results to the global datatable", "Find text `hi`"),
                    tuple("matched-inside", "some other text", "Find text and add the results to the global datatable", "Find text `SOME OTHER TEXT`")
                  );
            }),
          text(
            "hi",
            "~~>hi",
            spec -> spec.path("matched")
          ),
          text(
            "hello",
            spec -> spec.path("non-matched")
          ),
          text(
            "hi",
            "~~>hi",
            spec -> spec.path("nested/matched")
          ),
          text(
            """
              we also search through entire files
              file contains some other text somewhere in the middle.
              end of file
              """,
            """
              we also search through entire files
              file contains ~~>some other text somewhere in the middle.
              end of file
              """,
            spec -> spec.path("matched-inside")
          )
        );
    }

    @Test
    void multipleMarkersAddedByRecipeReported() {
        rewriteRun(
          spec -> spec.dataTable(SearchResults.Row.class, rows -> {
              assertThat(rows).extracting(
                  SearchResults.Row::getSourcePath,
                  SearchResults.Row::getResult,
                  SearchResults.Row::getParentRecipe,
                  SearchResults.Row::getRecipe)
                .containsExactlyInAnyOrder(
                  tuple("match-capitalized-words", "We", "Find text and add the results to the global datatable", "Find text `([A-Z])\\w+`"),
                  tuple("match-capitalized-words", "SearchMarkers", "Find text and add the results to the global datatable", "Find text `([A-Z])\\w+`"),
                  tuple("match-capitalized-words", "File", "Find text and add the results to the global datatable", "Find text `([A-Z])\\w+`"),
                  tuple("match-capitalized-words", "End", "Find text and add the results to the global datatable", "Find text `([A-Z])\\w+`")
                );
          }),
          text(
            """
              We add all SearchMarkers that are found during recipe run.
              File contains capitalized words.
              End of file
              """, """
              ~~>We add all ~~>SearchMarkers that are found during recipe run.
              ~~>File contains capitalized words.
              ~~>End of file
              """,
            spec -> spec.path("match-capitalized-words")
          )
        );
    }

    @Test
    void markersOfGeneratedFilesReported() {
        rewriteRun(
          spec -> spec
            .recipeFromYaml(
              //language=yml
              """
                type: specs.openrewrite.org/v1beta/recipe
                name: test.SearchMarkerScraping
                displayName: Find text and add the results to the global datatable
                description: Hello world.
                recipeList:
                  - org.openrewrite.text.CreateTextFile:
                      fileContents: this file matches
                      relativeFileName: matched
                  - org.openrewrite.text.Find:
                      find: hi
                      caseSensitive: true
                """,
              "test.SearchMarkerScraping"
            ).dataTable(SearchResults.Row.class, rows -> {
                assertThat(rows).extracting(
                    SearchResults.Row::getSourcePath,
                    SearchResults.Row::getResult,
                    SearchResults.Row::getParentRecipe,
                    SearchResults.Row::getRecipe)
                  .containsExactlyInAnyOrder(
                    tuple("matched", "hi", "Find text and add the results to the global datatable", "Find text `hi`")
                  );
            }),
          text(
            doesNotExist(),
            "t~~>his file matches",
            spec -> spec.path("matched")
          )
        );
    }

    @Test
    void multipleMarkersAddedByDifferentRecipesReported() {
        rewriteRun(
          spec -> spec.dataTable(SearchResults.Row.class, rows -> {
                assertThat(rows).extracting(
                    SearchResults.Row::getSourcePath,
                    SearchResults.Row::getResult,
                    SearchResults.Row::getParentRecipe,
                    SearchResults.Row::getRecipe)
                  .containsExactlyInAnyOrder(
                    tuple("different-recipes-matching", "We", "Find text and add the results to the global datatable", "Find text `([A-Z])\\w+`"),
                    tuple("different-recipes-matching", "SearchMarkers", "Find text and add the results to the global datatable", "Find text `([A-Z])\\w+`"),
                    tuple("different-recipes-matching", "some other text", "Find text and add the results to the global datatable", "Find text `SOME OTHER TEXT`")
                  );
            }),
          text(
            """
              We add all SearchMarkers that are found during recipe run.
              file contains some other text somewhere in the middle resulting in 2 different recipes matches.
              """, """
              ~~>We add all ~~>SearchMarkers that are found during recipe run.
              file contains ~~>some other text somewhere in the middle resulting in 2 different recipes matches.
              """,
            spec -> spec.path("different-recipes-matching")
          )
        );
    }

    @Test
    void nestedRecipesReported() {
        rewriteRun(
          spec -> spec
            .recipeFromYaml(
              //language=yml
              """
                type: specs.openrewrite.org/v1beta/recipe
                name: test.SearchMarkerScraping
                displayName: Find text and add the results to the global datatable
                description: Hello world.
                recipeList:
                  - test.FindCapitalizedWords
                  - test.FindSomeOtherText
                ---
                type: specs.openrewrite.org/v1beta/recipe
                name: test.FindCapitalizedWords
                displayName: Find text and add the results to the global datatable
                description: Hello world.
                recipeList:
                  - org.openrewrite.text.Find:
                      find: ([A-Z])\\w+
                      regex: true
                      caseSensitive: true
                ---
                type: specs.openrewrite.org/v1beta/recipe
                name: test.FindSomeOtherText
                displayName: Find text and add the results to the global datatable
                description: Hello world.
                recipeList:
                  - org.openrewrite.text.Find:
                      find: SOME OTHER TEXT
                """,
              "test.SearchMarkerScraping"
            ).dataTable(SearchResults.Row.class, rows -> {
                assertThat(rows).extracting(
                    SearchResults.Row::getSourcePath,
                    SearchResults.Row::getResult,
                    SearchResults.Row::getParentRecipe,
                    SearchResults.Row::getRecipe)
                  .containsExactlyInAnyOrder(
                    tuple("different-recipes-matching", "We", "Find text and add the results to the global datatable", "Find text `([A-Z])\\w+`"),
                    tuple("different-recipes-matching", "We", "", "Find text and add the results to the global datatable"),
                    tuple("different-recipes-matching", "SearchMarkers", "Find text and add the results to the global datatable", "Find text `([A-Z])\\w+`"),
                    //TODO: Question for Jonathan: Should we not report both instance name and recipe name? Searching for the recipe id from the String is not always "easy"
                    tuple("different-recipes-matching", "SearchMarkers", "", "Find text and add the results to the global datatable"),
                    tuple("different-recipes-matching", "some other text", "Find text and add the results to the global datatable", "Find text `SOME OTHER TEXT`"),
                    tuple("different-recipes-matching", "some other text", "", "Find text and add the results to the global datatable")
                  );
            }),
          text(
            """
              We add all SearchMarkers that are found during recipe run.
              file contains some other text somewhere in the middle resulting in 2 different recipes matches.
              """, """
              ~~>We add all ~~>SearchMarkers that are found during recipe run.
              file contains ~~>some other text somewhere in the middle resulting in 2 different recipes matches.
              """,
            spec -> spec.path("different-recipes-matching")
          )
        );
    }
}
