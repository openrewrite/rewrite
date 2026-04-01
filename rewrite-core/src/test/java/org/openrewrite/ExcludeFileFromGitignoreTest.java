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
    void addNegationIfFolderNameMatch() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("test/"))),
          text(
            """
              test
              """,
            """
              test
              !/test/
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
    void addNegationIfNestedFolderNameMatch() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("test/file.yaml"))),
          text(
            """
              test
              """,
            """
              test
              !/test/file.yaml
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
              # comment

              file.yaml
              """,
            """
              # comment

              test.yml
              !/directory/test.yml
              # comment

              file.yaml
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
              /directory/*
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
              /directory/*
              !/directory/nested/
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void ignoreDeeplyNestedDirectory() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/deeply/nested/"))),
          text(
            """
              /directory/
              """,
            """
              /directory/*
              !/directory/deeply/
              /directory/deeply/*
              !/directory/deeply/nested/
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void ignoreDeeplyNestedFile() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/deeply/nested/file.txt"))),
          text(
            """
              /directory/
              """,
            """
              /directory/*
              !/directory/deeply/
              /directory/deeply/*
              !/directory/deeply/nested/
              /directory/deeply/nested/*
              !/directory/deeply/nested/file.txt
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
              /yet-another-nested/*
              !/yet-another-nested/test.yml
              """,
            spec -> spec.path("directory/nested/.gitignore")
          )
        );
    }

    @Test
    void ignoreWildcardedDirectoryAtStart() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/nested/", "files/nesting/deeply/file.txt"))),
          text(
            """
              /**/nested/
              /**/nesting/
              """,
            """
              /**/nested/
              !/directory/nested/
              /**/nesting/*
              !/files/nesting/
              /files/nesting/*
              !/files/nesting/deeply/
              /files/nesting/deeply/*
              !/files/nesting/deeply/file.txt
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void ignoreWildcardedDirectoryAtEnd() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/nested/", "files/nested"))),
          text(
            """
              /directory/**/
              /files/**/
              """,
            """
              /directory/**/
              !/directory/nested/
              /files/**/
              !/files/nested
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void ignoreWildcardedFile() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/test.yml", "directory/other.txt", "/directory/nested/application.yaml"))),
          text(
            """
              /test.*
              /*.txt
              /nested/*.yaml
              """,
            """
              /test.*
              !/test.yml
              /*.txt
              !/other.txt
              /nested/*.yaml
              !/nested/application.yaml
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
              otherfile.yml
              nested/test.yml
              !/nested/test.yml
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
              # comment 1
              test.yml
              /yet-another-file.yml
              # comment 2
              /otherfile.yml
              # comment 3
              end-of-file/file.yml
              """,
            """
              # comment 1
              test.yml
              !/test.yml
              /yet-another-file.yml
              # comment 2
              # comment 3
              end-of-file/file.yml
              !/end-of-file/file.yml
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void wrongExactNegationPositionedBeforeIgnoreMovesToCorrectPosition() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/test.yml"))),
          text(
            """
              !/directory/test.yml
              /directory/*
              """,
            """
              /directory/*
              !/directory/test.yml
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void wrongNonExactNegationPositionedBeforeIgnoreDoesNotGetChanged() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("directory/test.yml"))),
          text(
            """
              !directory/test.yml
              /directory/*
              """,
            """
              !directory/test.yml
              /directory/*
              !/directory/test.yml
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void whenMultipleFilesInTheSameDirectoryTheyDoNotGetOverwritten() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("gradlew", "gradlew.bat","gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties"))),
          text(
            """
              gradlew
              gradlew.bat
              /gradle/
              """,
            """
              gradlew
              !/gradlew
              gradlew.bat
              !/gradlew.bat
              /gradle/*
              !/gradle/wrapper/
              /gradle/wrapper/*
              !/gradle/wrapper/gradle-wrapper.properties
              !/gradle/wrapper/gradle-wrapper.jar
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void multiWildcardNoDirectoryEntries() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties"))),
          text(
            """
              /gradle/**
              """,
                """
              /gradle/**
              !/gradle/wrapper/
              /gradle/wrapper/**
              !/gradle/wrapper/gradle-wrapper.properties
              !/gradle/wrapper/gradle-wrapper.jar
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void noActionOnWrappedWildcardPath() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("gradle/nested/files/test.txt"))),
          text(
            """
              /gradle/**/files/
              gradle/**/files
              gradle/**/files/
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void noActionOnMultiWildcardEntriesOnly() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeFileFromGitignore(List.of("gradlew", "gradlew.bat","gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties"))),
          text(
            """
              **/gradlew
              gradlew.bat
              /gradle/**
              """,
            """
              **/gradlew
              !/gradlew
              gradlew.bat
              !/gradlew.bat
              /gradle/**
              !/gradle/wrapper/
              /gradle/wrapper/**
              !/gradle/wrapper/gradle-wrapper.properties
              !/gradle/wrapper/gradle-wrapper.jar
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }
}
