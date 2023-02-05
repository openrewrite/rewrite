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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.function.Supplier;

import static org.openrewrite.test.SourceSpecs.text;

class AppendToTextFileTest implements RewriteTest {

    @Test
    void createsFileIfNeeded() {
        rewriteRun(
          spec -> spec.recipe(new AppendToTextFile("file.txt", "content", "preamble", true, "leave")),
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
        Supplier<Recipe> r = () -> new AppendToTextFile("file.txt", "content", "preamble", true, "leave");
        rewriteRun(
          spec -> spec.recipe(r.get().doNext(r.get())),
          text(
            null,
            """
              preamble
              content
              content
              """,
            SourceSpec::noTrim
          )
        );
    }

    @Test
    void replacesFileIfRequested() {
        rewriteRun(
          spec -> spec.recipe(new AppendToTextFile("file.txt", "content", "preamble", true, "replace")),
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
    void continuesFileIfRequested() {
        rewriteRun(
          spec -> spec.recipe(new AppendToTextFile("file.txt", "content", "preamble", true, "continue")),
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
          spec -> spec.recipe(new AppendToTextFile("file.txt", "content", "preamble", true, "leave")),
          text("existing", spec -> spec.path("file.txt"))
        );
    }

    @Test
    void multipleInstancesCanAppend() {
        rewriteRun(
          spec -> spec.recipe(
            new AppendToTextFile("file.txt", "content", "preamble", true, "replace")
              .doNext(new AppendToTextFile("file.txt", "content", "preamble", true, "replace"))),
          text(
            "existing",
            """
              preamble
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
          spec -> spec.recipe(new AppendToTextFile("file.txt", "content", null, true, "replace")),
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
          spec -> spec.recipe(new AppendToTextFile("file1.txt", "content1", "preamble1", true, "replace")
            .doNext(new AppendToTextFile("file2.txt", "content2", "preamble2", true, "replace"))),
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

    // This test is just to demonstrate RewriteRun defect, it's expected to fail since new file `file1.txt` generated but pass.
    // todo, kunli, to be removed after this PR.
    @Disabled("Expected to be failed since new file generated")
    @Test
    void demonstrateRewriteRunDefect() {
        rewriteRun(
          spec -> spec.recipe(new AppendToTextFile("file1.txt", "content1", "preamble1", true, "replace")
            .doNext(new AppendToTextFile("file2.txt", "content2", "preamble2", true, "replace"))),
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
    void changeAndCreatedFilesIfNeeded() {
        rewriteRun(
          spec -> spec.recipe(new AppendToTextFile("file1.txt", "content1", "preamble1", true, "replace")
            .doNext(new AppendToTextFile("file2.txt", "content2", "preamble2", true, "replace"))),
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
          spec -> spec.recipe(
            new AppendToTextFile("file1.txt", "content1", "preamble1", true, "replace")
              .doNext(new AppendToTextFile("file2.txt", "content2", "preamble2", true, "replace"))
              .doNext(new AppendToTextFile("file1.txt", "content1", "preamble1", true, "replace"))
              .doNext(new AppendToTextFile("file2.txt", "content2", "preamble2", true, "replace"))),
          text(
            "existing1",
            """
              preamble1
              content1
              content1
              """,
            spec -> spec.path("file1.txt").noTrim()
          ),
          text(
            "existing2",
            """
              preamble2
              content2
              content2
              """,
            spec -> spec.path("file2.txt").noTrim()
          )
        );
    }
}
