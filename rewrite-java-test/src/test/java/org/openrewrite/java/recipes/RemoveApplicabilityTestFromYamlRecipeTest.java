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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

public class RemoveApplicabilityTestFromYamlRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveApplicabilityTestFromYamlRecipe());
    }

    @DocumentExample("Comment out applicability from yaml recipes as it's no longer supported and could require user to transform it to java.")
    @Test
    void commentOutAnySourceApplicabilityTest() {
        //language=yaml
        rewriteRun(
          yaml(
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.java.testing.mockito.AnyToNullable
              displayName: x
              description: x.
              tags:
                - testing
              applicability:
                anySource:
                  - org.openrewrite.java.testing.mockito.UsesMockitoAll
                singleSource:
                  - org.openrewrite.java.testing.mockito.UsesMockitoAll
              recipeList:
                - org.openrewrite.java.Recipe1
                - org.openrewrite.java.Recipe2
              """,
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.java.testing.mockito.AnyToNullable
              displayName: x
              description: x.
              tags:
                - testing
              # Applicability tests are no longer supported for yaml recipes, please remove or require migrating the recipe to Java code
                # anySource:
                #   - org.openrewrite.java.testing.mockito.UsesMockitoAll
              recipeList:
                - org.openrewrite.java.Recipe1
                - org.openrewrite.java.Recipe2
              preconditions:
                - org.openrewrite.java.testing.mockito.UsesMockitoAll
              """
          )
        );
    }

    @Test
    void migrateSingleSourceApplicability() {
        //language=yaml
        rewriteRun(
          yaml(
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.java.testing.mockito.AnyToNullable
              displayName: x
              description: x.
              tags:
                - testing
              applicability:
                singleSource:
                  - org.openrewrite.java.testing.mockito.UsesMockitoAll
              recipeList:
                - org.openrewrite.java.Recipe1
                - org.openrewrite.java.Recipe2
              """,
            """
              type: specs.openrewrite.org/v1beta/recipe
              name: org.openrewrite.java.testing.mockito.AnyToNullable
              displayName: x
              description: x.
              tags:
                - testing
              recipeList:
                - org.openrewrite.java.Recipe1
                - org.openrewrite.java.Recipe2
              preconditions:
                - org.openrewrite.java.testing.mockito.UsesMockitoAll
              """
          )
        );
    }
}
