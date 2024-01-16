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
package org.openrewrite.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.SourceSpecs.text;

class RewriteTestTest implements RewriteTest {

    @Test
    void rejectRecipeWithNameOption() {
        assertThrows(AssertionError.class, () -> rewriteRun(
          spec -> spec.recipe(new RecipeWithNameOption("test")),
          text(
            "hello world!"
          )
        ));
    }

    @Test
    void verifyAll() {
        assertThrows(AssertionError.class, this::assertRecipesConfigure);
    }


    @Test
    void expectingChanges() {
        assertThrows(AssertionError.class, () -> rewriteRun(
          spec -> spec.recipe(toRecipe(PlainTextVisitor::new)),
          text("Hello world", "Goodbye world")
        ));
    }

    @Test
    void expectingNoChanges() {
        assertThrows(AssertionError.class, () -> rewriteRun(
            spec -> spec.recipe(toRecipe(() -> new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                    return text.withText("Hello world");
                }
            })),
            text("")
          ),
          "When the recipe makes changes but no changes are expected the test should fail");
    }

    @Test
    void unstableRecipe() {
        assertThrows(AssertionError.class, () -> rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new PlainTextVisitor<>() {
              @Override
              public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                  return text.withText(text.getText() + "Hello world");
              }
          })),
          text("", "Hello world")
        ),
          "Recipes are expected to be stable, that is to say not make further changes to their output. " +
          "The test framework enforces this by running the recipe again on its own output, asserting no further changes.");
    }

    @Test
    void stabilityRequirementOptOut() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new PlainTextVisitor<>() {
              @Override
              public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                  return text.withText(text.getText() + "Hello world");
              }
          })).recipeOutputStabilityVerification(false),
          text("", "Hello world")
        );
    }
}

@SuppressWarnings("FieldCanBeLocal")
@NonNullApi
class RecipeWithNameOption extends Recipe {
    @Option
    private final String name;

    @JsonCreator
    public RecipeWithNameOption(String name) {
        this.name = name;
    }

    @Override
    public String getDisplayName() {
        return "Recipe with name option";
    }

    @Override
    public String getDescription() {
        return "A fancy description.";
    }
}
