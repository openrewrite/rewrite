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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.SourceSpecs.text;

class RunOnceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.cycles(1).expectedCyclesThatMakeChanges(1);
    }

    @Test
    void appendOnce() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
            """
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.test.AppendBlaOnce
            displayName: Append bla once
            description: Append bla once.
            recipeList:
              - org.openrewrite.test.AppendBla
              - org.openrewrite.test.AppendBla
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.test.AppendBla
            displayName: Append bla
            description: Append bla.
            preconditions:
              - org.openrewrite.RunOnce:
                  fqrn: org.openrewrite.text.AppendToTextFile
            recipeList:
              - org.openrewrite.text.AppendToTextFile:
                  relativeFileName: "a/file.txt"
                  content: "bla"
                  existingFileStrategy: Merge
            """,
            "org.openrewrite.test.AppendBlaOnce"),
          text(
            """
            abc
            """,
            """
            abc
            bla
            
            """,
            spec -> spec.path("a/file.txt")
          )
        );
    }

    @Test
    void appendFirstNotSecond() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
            """
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.test.AppendFirstNotSecond
            displayName: Append first and not second
            description: Append first and not second.
            recipeList:
              - org.openrewrite.test.AppendFirst
              - org.openrewrite.test.AppendSecond
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.test.AppendFirst
            displayName: Append first
            description: Append first.
            recipeList:
              - org.openrewrite.text.AppendToTextFile:
                  relativeFileName: "a/file.txt"
                  content: "first"
                  existingFileStrategy: Merge
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.test.AppendSecond
            displayName: Append second
            description: Append second.
            preconditions:
              - org.openrewrite.RunOnce:
                  fqrn: org.openrewrite.text.AppendToTextFile
            recipeList:
              - org.openrewrite.text.AppendToTextFile:
                  relativeFileName: "a/file.txt"
                  content: "second"
                  existingFileStrategy: Merge
            """,
            "org.openrewrite.test.AppendFirstNotSecond"),
          text(
            """
            abc
            """,
            """
            abc
            first
            
            """,
            spec -> spec.path("a/file.txt")
          )
        );
    }
}