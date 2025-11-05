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
package org.openrewrite.text;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.Recipe;
import org.openrewrite.test.RewriteTest;

import java.util.function.Supplier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.openrewrite.test.SourceSpecs.text;

class AppendToTextFileTest implements RewriteTest {

    @DocumentExample
    @Test
    void replacesFileIfRequested() {
        rewriteRun(
          spec -> spec.recipe(new AppendToTextFile("file.txt", "content", "preamble", true, AppendToTextFile.Strategy.Replace)),
          text(
            """
              existing
              """,
            """
              preamble
              content
              """,
            spec -> spec.path("file.txt").noTrim()
          )
        );
    }

    @Test
    void createsFileIfNeeded() {
        rewriteRun(
          spec -> spec.recipe(new AppendToTextFile("file.txt", "content", "preamble", true, AppendToTextFile.Strategy.Leave)),
          text(
            null,
            """
              preamble
              content
              """
          )
        );
    }

    @Test
    void createsFileIfNeededWithMultipleInstances() {
        Supplier<Recipe> r = () -> new AppendToTextFile("file.txt", "content", "preamble", true, AppendToTextFile.Strategy.Leave);
        rewriteRun(
          spec -> spec.recipes(r.get(), r.get()),
          text(
            null,
            """
              preamble
              content
              """,
            spec -> spec.path("file.txt")
          )
        );
    }

    @Test
    void continuesFileIfRequested() {
        rewriteRun(
          spec -> spec.recipe(new AppendToTextFile("file.txt", "content", "preamble", true, AppendToTextFile.Strategy.Continue)),
          text(
            """
              existing
              """,
            """
              existing
              content
              """,
            spec -> spec.path("file.txt").noTrim()
          )
        );
    }

    @Test
    void leavesFileIfRequested() {
        rewriteRun(
          spec -> spec.recipe(new AppendToTextFile("file.txt", "content", "preamble", true, AppendToTextFile.Strategy.Leave)),
          text("existing", spec -> spec.path("file.txt"))
        );
    }

    @Test
    void multipleInstancesCanAppend() {
        rewriteRun(
          spec -> spec.recipes(
            new AppendToTextFile("file.txt", "content", "preamble", true, AppendToTextFile.Strategy.Continue),
            new AppendToTextFile("file.txt", "content", "preamble", true, AppendToTextFile.Strategy.Continue)
          ),
          text(
            "existing",
            """
              existing
              content
              content
              """,
            spec -> spec.path("file.txt").noTrim()
          )
        );
    }

    @Test
    void noLeadingNewlineIfNoPreamble() {
        rewriteRun(
          spec -> spec.recipe(new AppendToTextFile("file.txt", "content", null, true, AppendToTextFile.Strategy.Replace)),
          text(
            """
              existing
              """,
            """
              content
              """,
            spec -> spec.path("file.txt").noTrim()
          )
        );
    }

    @Test
    void multipleFiles() {
        rewriteRun(
          spec -> spec.recipes(
            new AppendToTextFile("file1.txt", "content1", "preamble1", true, AppendToTextFile.Strategy.Replace),
            new AppendToTextFile("file2.txt", "content2", "preamble2", true, AppendToTextFile.Strategy.Replace)
          ),
          text(
            "existing1",
            """
              preamble1
              content1
              """,
            spec -> spec.path("file1.txt").noTrim()
          ),
          text(
            "existing2",
            """
              preamble2
              content2
              """,
            spec -> spec.path("file2.txt").noTrim()
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2796")
    @Test
    void missingExpectedGeneratedFiles() {
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
          rewriteRun(
            spec -> spec.recipes(
              new AppendToTextFile("file1.txt", "content1", "preamble1", true, AppendToTextFile.Strategy.Replace),
              new AppendToTextFile("file2.txt", "content2", "preamble2", true, AppendToTextFile.Strategy.Replace)
            ),
            text(
              "existing2",
              """
                preamble2
                content2
                """,
              spec -> spec.path("file2.txt").noTrim()
            )
          ));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2796")
    @Test
    void changeAndCreatedFilesIfNeeded() {
        rewriteRun(
          spec -> spec.recipes(
            new AppendToTextFile("file1.txt", "content1", "preamble1", true, AppendToTextFile.Strategy.Replace),
            new AppendToTextFile("file2.txt", "content2", "preamble2", true, AppendToTextFile.Strategy.Replace)
          ),
          text(
            "existing2",
            """
              preamble2
              content2
              """,
            spec -> spec.path("file2.txt").noTrim()
          ),
          text(
            null,
            """
              preamble1
              content1
              """
          )
        );
    }

    @Test
    void multipleInstancesOnMultipleFiles() {
        rewriteRun(
          spec -> spec.recipes(
            new AppendToTextFile("file1.txt", "content1", "preamble1", true, AppendToTextFile.Strategy.Replace),
            new AppendToTextFile("file2.txt", "content2", "preamble2", true, AppendToTextFile.Strategy.Replace),
            new AppendToTextFile("file1.txt", "content1", "preamble1", true, AppendToTextFile.Strategy.Replace),
            new AppendToTextFile("file2.txt", "content2", "preamble2", true, AppendToTextFile.Strategy.Replace)
          ),
          text(
            "existing1",
            """
              preamble1
              content1
              """,
            spec -> spec.path("file1.txt").noTrim()
          ),
          text(
            "existing2",
            """
              preamble2
              content2
              """,
            spec -> spec.path("file2.txt").noTrim()
          )
        );
    }

    @Test
    void continueStrategyWithPartialExistingContent() {
        // This test demonstrates current behavior when the file already contains
        // part of the content to be appended. The Continue strategy will append
        // the content even if it results in duplication.
        // Note: The recipe does NOT currently support intelligent merging of partial content.
        rewriteRun(
          spec -> spec.recipe(new AppendToTextFile("file.txt", "line2\nline3", null, true, AppendToTextFile.Strategy.Continue)),
          text(
            """
              line1
              line2
              """,
            """
              line1
              line2
              line2
              line3
              """,
            spec -> spec.path("file.txt").noTrim()
          )
        );
    }

    /**
     * Demonstrates the {@code Replace} strategy when partial content exists.
     * It completely replaces the file, discarding the partial existing content.
     */
    @Test
    void replaceStrategyWithPartialExistingContent() {
        rewriteRun(
          spec -> spec.recipe(new AppendToTextFile("file.txt", "line2\nline3", "# Header", true, AppendToTextFile.Strategy.Replace)),
          text(
            """
              line1
              line2
              """,
            """
              # Header
              line2
              line3
              """,
            spec -> spec.path("file.txt").noTrim()
          )
        );
    }

    /**
     * Demonstrates the {@code Leave} strategy when partial content exists.
     * The file is left unchanged, even though some of the desired content is missing.
     */
    @Test
    void leaveStrategyWithPartialExistingContent() {
        rewriteRun(
          spec -> spec.recipe(new AppendToTextFile("file.txt", "line2\nline3", null, true, AppendToTextFile.Strategy.Leave)),
          text(
            """
              line1
              line2
              """,
            spec -> spec.path("file.txt")
          )
        );
    }

    /**
     * Demonstrates the @{code Merge} strategy with smart line-based deduplication.
     * Only lines that don't already exist are appended.
     */
    @Test
    void mergeStrategyWithPartialExistingContent() {
        rewriteRun(
          spec -> spec.recipe(new AppendToTextFile("file.txt", "line2\nline3\nline4", null, true, AppendToTextFile.Strategy.Merge)),
          text(
            """
              line1
              line2
              """,
            """
              line1
              line2
              line3
              line4
              """,
            spec -> spec.path("file.txt").noTrim()
          )
        );
    }

    /**
     * When all content to be appended already exists, no changes are made.
     */
    @Test
    void mergeStrategyWithAllContentAlreadyPresent() {
        rewriteRun(
          spec -> spec.recipe(new AppendToTextFile("file.txt", "line1\nline2", null, true, AppendToTextFile.Strategy.Merge)),
          text(
            """
              line1
              line2
              """,
            spec -> spec.path("file.txt")
          )
        );
    }

    /**
     * When none of the content exists, all lines are appended.
     */
    @Test
    void mergeStrategyWithNoOverlap() {
        rewriteRun(
          spec -> spec.recipe(new AppendToTextFile("file.txt", "line3\nline4", null, true, AppendToTextFile.Strategy.Merge)),
          text(
            """
              line1
              line2
              """,
            """
              line1
              line2
              line3
              line4
              """,
            spec -> spec.path("file.txt").noTrim()
          )
        );
    }

    /**
     * Merge strategy compares lines after trimming whitespace.
     */
    @Test
    void mergeStrategyIgnoresWhitespace() {
        rewriteRun(
          spec -> spec.recipe(new AppendToTextFile("file.txt", "  line2  \nline3", null, true, AppendToTextFile.Strategy.Merge)),
          text(
            """
              line1
              line2
              """,
            """
              line1
              line2
              line3
              """,
            spec -> spec.path("file.txt").noTrim()
          )
        );
    }

    /**
     * When the file doesn't exist, Merge behaves the same as creating a new file.
     */
    @Test
    void mergeStrategyOnNewFile() {
        rewriteRun(
          spec -> spec.recipe(new AppendToTextFile("file.txt", "line1\nline2", "# Header", true, AppendToTextFile.Strategy.Merge)),
          text(
            null,
            """
              # Header
              line1
              line2
              """
          )
        );
    }
}
