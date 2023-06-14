package org.openrewrite.java.recipes;


import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.SourceSpecs.text;

class RecipeChainExecutorTest implements RewriteTest {

    @Test
    void yamlApplicabilityWithSingleSource() {
        @Language("yaml")
        String yamlRecipe = """
          ---
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.ApplicabilityExactlyOnce
          displayName: Applicability test runs once for the whole recipe list
          description: >
            An applicability test should be run once and if a match is found, all recipes in the list should be run.
            So if one of the recipes in the list makes a change which would cause the applicability test to no longer match,
            subsequent recipes in the list should still execute.
            
            Given a text file containing the number "1", running this recipe should result in a file which contains "3".
            If the applicability test is incorrectly applied to individual recipes in the list, the (incorrect) result would be "2".
          recipeList:
            - org.openrewrite.java.recipes.RecipeChainExecutor:
                preCondition:
                  - org.openrewrite.text.FindAndReplace:
                      find: "1"
                      replace: "A"
                recipes:
                  - org.openrewrite.text.ChangeText:
                      toText: "2"
                  - org.openrewrite.text.ChangeText:
                      toText: "3"
          """;
        rewriteRun(
          spec -> spec.recipeFromYaml(yamlRecipe, "org.openrewrite.ApplicabilityExactlyOnce"),
          text("1", "3"),
          text("2")
        );
    }

}

