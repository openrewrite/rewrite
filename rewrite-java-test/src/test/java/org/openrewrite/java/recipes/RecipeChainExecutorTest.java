/*
 * Copyright 2023 the original author or authors.
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

