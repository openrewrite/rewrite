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
                  .as("This example runs a list of two recipes on a single source and expects to produce 3 " +
                      "rows in the SourcesFileResults table, one is for the declarative recipe, another two are for two " +
                      "recipes in the list.")
                  .hasSize(3);

                SourcesFileResults.Row row0 = rows.get(0);
                assertThat(row0.getRecipe()).isEqualTo("org.openrewrite.AppendAndChangeText");
                SourcesFileResults.Row row1 = rows.get(1);
                assertThat(row1.getRecipe()).isEqualTo("org.openrewrite.text.AppendToTextFile");
                SourcesFileResults.Row row2 = rows.get(2);
                assertThat(row2.getRecipe()).isEqualTo("org.openrewrite.text.FindAndReplace");
            }),
          text(
            "0",
            "1 -> 2",
            spec -> spec.path("file.txt").noTrim()
          )
        );
    }
}