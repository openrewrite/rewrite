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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.marker.RecipesThatMadeChanges;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.FindAndReplace;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

class RecipeListTest implements RewriteTest {

    @DocumentExample
    @Test
    void declarativeRecipeInCode() {
        rewriteRun(
          specs -> specs.recipe(new FormalHello("jon", "jonathan"))
            .expectedCyclesThatMakeChanges(1).cycles(1),
          text(
            "hi jon",
            "hello jonathan",
            spec -> spec.afterRecipe(txt -> {
                Optional<Stream<String>> recipeNames = txt.getMarkers().findFirst(RecipesThatMadeChanges.class)
                  .map(recipes -> recipes.getRecipes().stream()
                    .map(stack -> stack.stream().map(Recipe::getDescriptor).map(RecipeDescriptor::getName)
                      .collect(Collectors.joining("->")))
                  );

                assertThat(recipeNames).isPresent();
                assertThat(recipeNames.get()).containsExactly(
                  "org.openrewrite.FormalHello->org.openrewrite.text.FindAndReplace",
                  "org.openrewrite.FormalHello->org.openrewrite.FormalHello$1"
                );
            })
          )
        );
    }
}

@EqualsAndHashCode(callSuper = false)
@Value
class FormalHello extends Recipe {
    @Option(example = "TODO Provide a usage example for the docs", displayName = "Before name",
            description = "The name of a person being greeted")
    String beforeName;

    @Option(example = "TODO Provide a usage example for the docs", displayName = "After name",
            description = "The more formal name of the person.")
    String afterName;

    @Override
    public String getDisplayName() {
        return "Formal hello";
    }

    @Override
    public String getDescription() {
        return "Be formal. Be cool.";
    }

    @Override
    public void buildRecipeList(RecipeList recipes) {
        recipes
          // TODO would these large option-set recipes
          //  benefit from builders?
          .recipe(new FindAndReplace(
            "hi", "hello", null, false, null,
            null, null, null)
          )
          .recipe(
            "Say my name, say my name",
            "It's late and I'm making bad jokes.",
            new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    return text.withText(text.getText().replace(beforeName, afterName));
                }
            }
          );
    }
}
