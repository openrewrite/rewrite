package org.openrewrite.table;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;


class SourcesFileResultsTest implements RewriteTest {
    @Test
    void twoResultsOnly() {
        @Language("yaml")
        String yamlRecipe = """
          ---
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.AppendAndChangeText
          displayName: Append and then change text
          recipeList:
              - org.openrewrite.text.AppendToTextFile:
                  relativeFileName: "file.txt"
                  content: " -> 2"
                  preamble: "preamble"
                  appendNewline : false
                  existingFileStrategy: "continue"
              - org.openrewrite.text.FindAndReplace:
                  find: "0"
                  replace: "1"
          """;
        rewriteRun(
          spec -> spec.recipeFromYaml(yamlRecipe, "org.openrewrite.AppendAndChangeText")
            .dataTable(SourcesFileResults.Row.class, rows -> {
                assertThat(rows)
                  .as("Running a list of two recipes on a single source should produce 2 rows in the SourcesFileResults table")
                  .hasSize(3);

                SourcesFileResults.Row row0 = rows.get(0);
                assertThat(row0.getRecipe()).isEqualTo("org.openrewrite.AppendAndChangeText");
                SourcesFileResults.Row row1 = rows.get(1);
                assertThat(row1.getRecipe()).isEqualTo("org.openrewrite.text.AppendToTextFile");
                SourcesFileResults.Row row2 = rows.get(2);
                assertThat(row2.getRecipe()).isEqualTo("org.openrewrite.text.FindAndReplace");
            }),
            // .dataTable(),
          text(
            "0",
            "1 -> 2",
            spec -> spec.path("file.txt").noTrim()
          )
        );
    }
}
