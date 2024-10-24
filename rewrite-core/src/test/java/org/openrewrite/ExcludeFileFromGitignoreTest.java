package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.test.SourceSpecs.text;

class ExcludeFileFromGitignoreTest implements RewriteTest {

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
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/test.yml", "otherdirectory/otherfile.yml", "directory/nested/not-ignored.yml"))),
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
    void ignoredDirectories() {
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
}