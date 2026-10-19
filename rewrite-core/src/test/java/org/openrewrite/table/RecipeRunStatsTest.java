/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.CreateTextFile;
import org.openrewrite.text.FindAndReplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.table.SourceFileResultsTest.builtWithGradle8;
import static org.openrewrite.table.SourceFileResultsTest.recordGradleVersion;
import static org.openrewrite.test.SourceSpecs.text;

class RecipeRunStatsTest implements RewriteTest {

    @DocumentExample
    @Test
    void countsOnlyFilesWhoseTextChanged() {
        rewriteRun(
          spec -> spec
            .recipes(recordGradleVersion("9.8.1"), new FindAndReplace("Hi", "Hello", null, null, null, null, null, null))
            .dataTable(RecipeRunStats.Row.class, rows ->
              assertThat(rows).extracting(RecipeRunStats.Row::getSourceFilesChanged).containsOnly(1)),
          text("Hi", "Hello", spec -> builtWithGradle8(spec.path("hi.txt"))),
          // Only its marker changes
          text("Bye", spec -> builtWithGradle8(spec.path("bye.txt")))
        );
    }

    @Test
    void countsCreatedFiles() {
        rewriteRun(
          spec -> spec
            .recipe(new CreateTextFile("foo", "manifest.txt", false))
            .dataTable(RecipeRunStats.Row.class, rows ->
              assertThat(rows).extracting(RecipeRunStats.Row::getSourceFilesChanged).containsOnly(1)),
          text(null, "foo", spec -> spec.path("manifest.txt"))
        );
    }
}
