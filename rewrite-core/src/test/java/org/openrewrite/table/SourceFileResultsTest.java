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
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Recipe;
import org.openrewrite.RenameFile;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.text.ChangeText;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.other;
import static org.openrewrite.test.SourceSpecs.text;

class SourceFileResultsTest implements RewriteTest {

    @DocumentExample
    @Test
    void hierarchical() {
        rewriteRun(
          spec -> spec
            .recipeFromYaml(
                //language=yml
                """
                  type: specs.openrewrite.org/v1beta/recipe
                  name: test.ChangeTextToHello
                  displayName: Change text to hello
                  description: Hello world.
                  recipeList:
                    - org.openrewrite.text.ChangeText:
                        toText: Hello!
                  """,
              "test.ChangeTextToHello"
            ).dataTable(SourcesFileResults.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                assertThat(rows.stream().map(SourcesFileResults.Row::getParentRecipe))
                  .containsExactly("test.ChangeTextToHello");
                assertThat(rows.stream().map(SourcesFileResults.Row::getRecipe))
                  .containsExactly("org.openrewrite.text.ChangeText");
            }),
          text(
            "Hi",
            "Hello!"
          )
        );
    }

    @Test
    void deepHierarchyRecordsAllLevels() {
        // Our Sankey visualization of recipe execution relies on the data table recording all levels of the hierarchy,
        // not just the leaf recipes. This test ensures that the data table includes rows for both the parent and leaf
        // recipes, even when the parent recipe doesn't directly perform any transformations (i.e., it has zero
        // estimated time saving).
        rewriteRun(
          spec -> spec
            .recipeFromYaml(
                //language=yml
                """
                  type: specs.openrewrite.org/v1beta/recipe
                  name: test.GrandParent
                  displayName: Grand parent recipe
                  description: Three-level hierarchy.
                  recipeList:
                    - test.Parent
                  ---
                  type: specs.openrewrite.org/v1beta/recipe
                  name: test.Parent
                  displayName: Parent recipe
                  description: Middle level.
                  recipeList:
                    - org.openrewrite.text.ChangeText:
                        toText: Hello!
                  """,
              "test.GrandParent"
            ).dataTable(SourcesFileResults.Row.class, rows -> {
                // Leaf recipe row + one hierarchy row for the parent recipe.
                // The root recipe (GrandParent) doesn't get its own row but appears
                // as the parent in production runs where a synthetic root wraps it.
                assertThat(rows).hasSize(2);
                assertThat(rows.get(0).getParentRecipe()).isEqualTo("test.Parent");
                assertThat(rows.get(0).getRecipe()).isEqualTo("org.openrewrite.text.ChangeText");
                // Hierarchy row with zero effort so the column remains summable
                assertThat(rows.get(1).getRecipe()).isEqualTo("test.Parent");
                assertThat(rows.get(1).getEstimatedTimeSaving()).isEqualTo(0L);
            }),
          text(
            "Hi",
            "Hello!"
          )
        );
    }

    @Test
    void noRowForAnEditThatOnlyChangesAMarker() {
        rewriteRun(
          spec -> spec
            .recipe(recordGradleVersion("9.8.1"))
            // The marker is a change to the tree, so the cycle made one
            .expectedCyclesThatMakeChanges(1)
            .afterRecipe(run -> assertThat(run.getDataTableStore().getDataTables())
              .noneMatch(dt -> dt.getName().equals(SourcesFileResults.class.getName()))),
          // The file prints the same, so it has no diff
          text("Hi", SourceFileResultsTest::builtWithGradle8)
        );
    }

    @Test
    void onlyTheRecipeThatChangedTheFileHasARow() {
        rewriteRun(
          spec -> spec
            .recipes(recordGradleVersion("9.8.1"), new ChangeText("Hello!"))
            .dataTable(SourcesFileResults.Row.class, rows ->
              assertThat(rows).extracting(SourcesFileResults.Row::getRecipe)
                .containsExactly("org.openrewrite.text.ChangeText")),
          text(
            "Hi",
            "Hello!",
            SourceFileResultsTest::builtWithGradle8
          )
        );
    }

    @Test
    void searchResultOnAQuarkHasARow() {
        // A quark is compared by its path alone, so it is the search result that keeps the row
        rewriteRun(
          spec -> spec
            .recipe(new FindSourceFiles("**/*.jks"))
            .dataTable(SourcesFileResults.Row.class, rows ->
              assertThat(rows).extracting(SourcesFileResults.Row::getSourcePath)
                .containsExactly("keystore.jks")),
          other(
            "this text will not be read because this is a quark",
            "~~>⚛⚛⚛ The contents of this file are not visible. ⚛⚛⚛",
            spec -> spec.path("keystore.jks")
          )
        );
    }

    @Test
    void renameThatKeepsTheTextHasARow() {
        rewriteRun(
          spec -> spec
            .recipe(new RenameFile("hello.txt", "goodbye.txt"))
            .dataTable(SourcesFileResults.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                assertThat(rows.getFirst().getSourcePath()).isEqualTo("hello.txt");
                assertThat(rows.getFirst().getAfterSourcePath()).isEqualTo("goodbye.txt");
                assertThat(rows.getFirst().getEstimatedTimeSaving()).isEqualTo(300L);
            }),
          text(
            "Hi",
            "Hi",
            spec -> spec
              .path("hello.txt")
              .afterRecipe(pt -> assertThat(pt.getSourcePath()).isEqualTo(Path.of("goodbye.txt")))
          )
        );
    }

    static void builtWithGradle8(SourceSpec<PlainText> spec) {
        spec.markers(new BuildTool(randomId(), BuildTool.Type.Gradle, "8.14.3"));
    }

    /**
     * Records a Gradle version in the {@link BuildTool} marker, as a Gradle upgrade does, which changes the tree but
     * not what the file prints.
     */
    static Recipe recordGradleVersion(String version) {
        return toRecipe(() -> new PlainTextVisitor<>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                if (text.getMarkers().findFirst(BuildTool.class).filter(tool -> version.equals(tool.getVersion())).isPresent()) {
                    return text;
                }
                return text.withMarkers(text.getMarkers().setByType(new BuildTool(randomId(), BuildTool.Type.Gradle, version)));
            }
        });
    }
}
