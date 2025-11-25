package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.SourceSpecs.text;

class PreventIfAlreadyRanTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromYaml(
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
              - org.openrewrite.PreventIfAlreadyRan:
                  fqrn: org.openrewrite.text.AppendToTextFile
            recipeList:
              - org.openrewrite.text.AppendToTextFile:
                  relativeFileName: "a/file.txt"
                  content: "second"
                  existingFileStrategy: Merge
            """,
            "org.openrewrite.test.AppendFirstNotSecond")
          .cycles(1)
          .expectedCyclesThatMakeChanges(1);
    }

    @Test
    void recipePreventsSecondInstance() {
        rewriteRun(
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