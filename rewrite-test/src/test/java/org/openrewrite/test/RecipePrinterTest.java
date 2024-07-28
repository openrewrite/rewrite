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
package org.openrewrite.test;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.java.recipes.SelectRecipeExamples;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class RecipePrinterTest implements RewriteTest {

    private StringBuilder sb;

    @BeforeEach
    void beforeEach() {
        sb = new StringBuilder();
    }

    @Test
    void printRecipeTreeForSimpleRecipe() {
        rewriteRun(
          spec -> spec
            .recipe(new SelectRecipeExamples())
            .printRecipe(() -> sb::append)
        );

        assertThat(sb.toString()).isEqualTo(SelectRecipeExamples.class.getName() + System.lineSeparator());
    }

    @Test
    void printRecipeTreeForRecipeWithNestedRecipes() {
        Recipe recipe = new CompositeRecipe(Arrays.asList(
          new TestRecipe("the option"),
          new SelectRecipeExamples(),
          new CompositeRecipe(Collections.singletonList(new SelectRecipeExamples()))
        ));
        rewriteRun(
          spec -> spec
            .recipe(recipe)
            .printRecipe(() -> sb::append)
        );

        String output = sb.toString();

        String expected = String.format(
          "%1$s%n" +
          "  %2$s: {theOption=the option}%n" +
          "  %3$s%n" +
          "  %1$s%n" +
          "    %3$s%n",
          CompositeRecipe.class.getName(), TestRecipe.class.getName(), SelectRecipeExamples.class.getName()
        );

        assertThat(output).isEqualTo(expected);
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class TestRecipe extends Recipe {

        @Option(displayName = "An option",
          description = "A sample option.",
          example = "Some text.")
        String theOption;

        @Override
        public String getDisplayName() {
            return "Test recipe";
        }

        @Override
        public String getDescription() {
            return "Test recipe.";
        }
    }
}
