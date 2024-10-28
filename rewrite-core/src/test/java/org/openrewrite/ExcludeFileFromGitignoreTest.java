/*
 * Copyright 2024 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.test.SourceSpecs.text;

class ExcludeFileFromGitignoreTest implements RewriteTest {

    @DocumentExample
    @Test
    void removesEntryIfExactPathMatch() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("test.yml"))),
          text(
            """
              /test.yml
              """,
            """
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void addNegationIfFileNameMatch() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("test.yml"))),
          text(
            """
              test.yml
              """,
            """
              test.yml
              !/test.yml
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void addNegationIfNestedFileNameMatch() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/test.yml"))),
          text(
            """
              test.yml
              """,
            """
              test.yml
              !/directory/test.yml
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void commentsAndEmptyLinesUntouched() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/test.yml"))),
          text(
            """
              # comment
              
              test.yml
              """,
            """
              # comment
              
              test.yml
              !/directory/test.yml
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void looksInNestedGitignoreFiles() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/test.yml"))),
          text(
            """
              test.yml
              """,
            spec -> spec.path(".gitignore")
          ),
          text(
            """
              test.yml
              """,
            """
              test.yml
              !/test.yml
              """,
            spec -> spec.path("directory/.gitignore")
          )
        );
    }

    @Test
    void removesInNestedGitignoreFiles() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/test.yml"))),
          text(
            """
              """,
            spec -> spec.path(".gitignore")
          ),
          text(
            """
              /test.yml
              """,
            """
              """,
            spec -> spec.path("directory/.gitignore")
          )
        );
    }

    @Test
    void recursivelyLooksInNestedGitignoreFiles() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/test.yml"))),
          text(
            """
              test.yml
              """,
            """
              test.yml
              !/directory/test.yml
              """,
            spec -> spec.path(".gitignore")
          ),
          text(
            """
              /test.yml
              """,
            """
              """,
            spec -> spec.path("directory/.gitignore")
          )
        );
    }

    @Test
    void nothingToRemoveIfPathNotInGitignore() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/test.yml"))),
          text(
            """
              otherfile.yml
              otherdirectory/test.yml
              """,
            spec -> spec.path(".gitignore")
          ),
          text(
            """
              otherfile.yml
              """,
            spec -> spec.path("directory/.gitignore")
          )
        );
    }

    @Test
    void multiplePaths() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of(
            "directory/test.yml",
            "otherdirectory/otherfile.yml",
            "directory/nested/not-ignored.yml"))),
          text(
            """
              test.yml
              /otherdirectory/otherfile.yml
              """,
            """
              test.yml
              !/directory/test.yml
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void negateFileFromIgnoredDirectory() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/test.yml"))),
          text(
            """
              /directory/
              """,
            """
              /directory/
              !/directory/test.yml
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void ignoredExactDirectories() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/"))),
          text(
            """
              /directory/
              """,
            """
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void ignoredDirectories() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/"))),
          text(
            """
              directory/
              """,
            """
              directory/
              !/directory/
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void ignoreNestedDirectory() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/nested/"))),
          text(
            """
              /directory/
              """,
            """
              /directory/
              !/directory/nested/
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void ignoreNestedDirectoryWithMultipleGitignoreFiles() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/nested/yet-another-nested/test.yml"))),
          text(
            """
              otherfile.yml
              """,
            spec -> spec.path(".gitignore")
          ),
          text(
            """
              /yet-another-nested/
              """,
            """
              /yet-another-nested/
              !/yet-another-nested/test.yml
              """,
            spec -> spec.path("directory/nested/.gitignore")
          )
        );
    }

    @Test
    void ignoreWildcardedDirectory() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/nested/"))),
          text(
            """
              /**/nested/
              """,
            """
              /**/nested/
              !/directory/nested/
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void ignoreWildcardedFile() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/test.yml", "directory/other.txt"))),
          text(
            """
              /test.*
              /*.txt
              """,
            """
              /test.*
              !/test.yml
              /*.txt
              !/other.txt
              """,
            spec -> spec.path("directory/.gitignore")
          )
        );
    }

    @Test
    void excludedPathsOnlyGetAddedOnce() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("nested/test.yml"))),
          text(
            """
              test.yml
              otherfile.yml
              nested/test.yml
              """,
            """
              test.yml
              !/nested/test.yml
              otherfile.yml
              nested/test.yml
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void newRulesGetAddedBesidesExistingRules() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("/test.yml", "/otherfile.yml", "end-of-file/file.yml"))),
          text(
            """
              # comment
              test.yml
              /yet-another-file.yml
              # comment
              /otherfile.yml
              # comment
              end-of-file/file.yml
              """,
            """
              # comment
              test.yml
              !/test.yml
              /yet-another-file.yml
              # comment
              # comment
              end-of-file/file.yml
              !/end-of-file/file.yml
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }
}
