/*
 * Copyright 2025 the original author or authors.
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

import static org.openrewrite.test.SourceSpecs.text;

class AddToGitignoreTest implements RewriteTest {

    @DocumentExample
    @Test
    void createGitignoreWhenNoneExists() {
        rewriteRun(
          spec -> spec.recipe(new AddToGitignore("""
            *.tmp
            .DS_Store
            target/""", null)),
          text(
            doesNotExist(),
            """
              *.tmp
              .DS_Store
              target/
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void addEntriesToExistingGitignore() {
        rewriteRun(
          spec -> spec.recipe(new AddToGitignore("""
            *.log
            build/""", null)),
          text(
            """
              *.tmp
              .DS_Store
              """,
            """
              *.tmp
              .DS_Store
              
              *.log
              build/
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void doNotDuplicateExistingEntries() {
        rewriteRun(
          spec -> spec.recipe(new AddToGitignore("""
            *.tmp
            *.log""", null)),
          text(
            """
              *.tmp
              .DS_Store
              *.log
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void handleEntriesWithDifferentFormats() {
        rewriteRun(
          spec -> spec.recipe(new AddToGitignore("""
            /build
            target/""", null)),
          text(
            """
              build/
              /target
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void preserveCommentsAndAddNewEntries() {
        rewriteRun(
          spec -> spec.recipe(new AddToGitignore("""
            # IDE files
            .idea/
            *.iml""", null)),
          text(
            """
              # Build artifacts
              target/
              build/
              """,
            """
              # Build artifacts
              target/
              build/
              
              # IDE files
              .idea/
              *.iml
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void handleMultilineInput() {
        rewriteRun(
          spec -> spec.recipe(new AddToGitignore("""
            *.log
            *.tmp
            
            # OS files
            .DS_Store
            Thumbs.db""", null)),
          text(
            """
              target/
              """,
            """
              target/
              
              *.log
              *.tmp
              # OS files
              .DS_Store
              Thumbs.db
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void handleWindowsLineEndings() {
        rewriteRun(
          spec -> spec.recipe(new AddToGitignore("*.log", null)),
          text(
            "*.tmp\r\n.DS_Store\r\n",
            "*.tmp\r\n.DS_Store\r\n\r\n*.log",
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void handleNestedGitignore() {
        // With default behavior (null pattern), targets root .gitignore
        // Should create root .gitignore and not modify nested one
        rewriteRun(
          spec -> spec.recipe(new AddToGitignore("local.properties", null)),
          text(
            doesNotExist(),
            """
              local.properties
              """,
            spec -> spec.path(".gitignore")
          ),
          text(
            """
              *.class
              """,
            spec -> spec.path("src/.gitignore")
          )
        );
    }

    @Test
    void handleNestedGitignoreWithExplicitPath() {
        // When explicitly targeting nested .gitignore, should update it
        rewriteRun(
          spec -> spec.recipe(new AddToGitignore("local.properties", "src/.gitignore")),
          text(
            """
              *.class
              """,
            """
              *.class
              
              local.properties
              """,
            spec -> spec.path("src/.gitignore")
          )
        );
    }

    @Test
    void normalizeAndDeduplicateEntries() {
        rewriteRun(
          spec -> spec.recipe(new AddToGitignore("""
            /build/
            build
            /build""", null)),
          text(
            """
              target/
              """,
            """
              target/
              
              /build/
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void handleNegationPatterns() {
        rewriteRun(
          spec -> spec.recipe(new AddToGitignore("""
            *.log
            !important.log""", null)),
          text(
            """
              *.tmp
              """,
            """
              *.tmp
              
              *.log
              !important.log
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void doNotAddDuplicateNegations() {
        rewriteRun(
          spec -> spec.recipe(new AddToGitignore("!important.log", null)),
          text(
            """
              *.log
              !important.log
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void emptyLinesInInput() {
        rewriteRun(
          spec -> spec.recipe(new AddToGitignore("""
            
            
            *.log
            
            *.tmp
            
            """, null)),
          text(
            """
              target/
              """,
            """
              target/
              
              *.log
              *.tmp
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void doNotChangeWhenEntriesExistInDifferentOrder() {
        rewriteRun(
          spec -> spec.recipe(new AddToGitignore("""
            *.log
            target/
            .DS_Store
            build/""", null)),
          text(
            """
              # Build directories
              build/
              target/
              
              # Logs
              *.log
              
              # OS files
              .DS_Store
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void rootOnlyTargetingByDefault() {
        AddToGitignore recipe = new AddToGitignore("*.log", null);
        rewriteRun(
          spec -> spec.recipe(recipe),
          text(
            """
              *.tmp
              """,
            """
              *.tmp
              
              *.log
              """,
            spec -> spec.path(".gitignore")
          ),
          text(
            """
              *.class
              """,
            spec -> spec.path("src/.gitignore")
          )
        );
    }

    @Test
    void targetSpecificSubdirectory() {
        AddToGitignore recipe = new AddToGitignore("*.log", "src/.gitignore");
        rewriteRun(
          spec -> spec.recipe(recipe),
          text(
            """
              *.tmp
              """,
            spec -> spec.path(".gitignore")
          ),
          text(
            """
              *.class
              """,
            """
              *.class
              
              *.log
              """,
            spec -> spec.path("src/.gitignore")
          )
        );
    }

    @Test
    void targetAllGitignoreFiles() {
        AddToGitignore recipe = new AddToGitignore("*.log", "**/.gitignore");
        rewriteRun(
          spec -> spec.recipe(recipe),
          text(
            """
              *.tmp
              """,
            """
              *.tmp
              
              *.log
              """,
            spec -> spec.path(".gitignore")
          ),
          text(
            """
              *.class
              """,
            """
              *.class
              
              *.log
              """,
            spec -> spec.path("src/.gitignore")
          ),
          text(
            """
              node_modules/
              """,
            """
              node_modules/
              
              *.log
              """,
            spec -> spec.path("docs/.gitignore")
          )
        );
    }

    @Test
    void createGitignoreInSpecificLocation() {
        AddToGitignore recipe = new AddToGitignore("*.log", "src/.gitignore");
        rewriteRun(
          spec -> spec.recipe(recipe),
          text(
            doesNotExist(),
            """
              *.log
              """,
            spec -> spec.path("src/.gitignore")
          )
        );
    }

    @Test
    void createRootGitignoreWhenPatternIsWildcard() {
        AddToGitignore recipe = new AddToGitignore("*.log", "**/.gitignore");
        rewriteRun(
          spec -> spec.recipe(recipe),
          text(
            doesNotExist(),
            """
              *.log
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void doNotAddSuperfluousEntries() {
        rewriteRun(
          spec -> spec.recipe(new AddToGitignore("foo.tmp", null)),
          text(
            """
              *.tmp
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void addEntryToFileEndingWithNewline() {
        rewriteRun(
          spec -> spec.recipe(new AddToGitignore("*.log", null)),
          text(
            "*.tmp\n.DS_Store\n",
            "*.tmp\n.DS_Store\n\n*.log\n",
            spec -> spec.path(".gitignore").noTrim()
          )
        );
    }

    @Test
    void addEntryToFileEndingWithWindowsNewline() {
        rewriteRun(
          spec -> spec.recipe(new AddToGitignore("*.log", null)),
          text(
            "*.tmp\r\n.DS_Store\r\n",
            "*.tmp\r\n.DS_Store\r\n\r\n*.log\r\n",
            spec -> spec.path(".gitignore").noTrim()
          )
        );
    }

    @Test
    void doNotAddSuperfluousEntriesWithMultiplePatterns() {
        rewriteRun(
          spec -> spec.recipe(new AddToGitignore("""
            foo.tmp
            build/bin
            test.log""", null)),
          text(
            """
              *.tmp
              build/
              *.log
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }

    @Test
    void addNonSuperfluousEntries() {
        rewriteRun(
          spec -> spec.recipe(new AddToGitignore("""
            foo.txt
            *.tmp""", null)),
          text(
            """
              *.log
              """,
            """
              *.log
              
              foo.txt
              *.tmp
              """,
            spec -> spec.path(".gitignore")
          )
        );
    }
}
