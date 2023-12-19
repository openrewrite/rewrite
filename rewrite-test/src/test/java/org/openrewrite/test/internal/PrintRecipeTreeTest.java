package org.openrewrite.test.internal;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.java.recipes.SelectRecipeExamples;
import org.openrewrite.test.RewriteTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class PrintRecipeTreeTest implements RewriteTest {

    private final StringBuilder sb = new StringBuilder();

    @Override
    public boolean printTree() {
        return true;
    }

    @Override
    public Consumer<String> consumeRecipeTree() {
        return s -> sb.append(s).append(System.lineSeparator());
    }

    @BeforeEach
    void beforeEach() {
        sb.setLength(0);
    }

    @Test
    void printRecipeTreeForSimpleRecipe() {
        rewriteRun(
          spec -> spec.recipe(new SelectRecipeExamples())
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
          spec -> spec.recipe(recipe)
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
}

@Value
@EqualsAndHashCode(callSuper = true)
class TestRecipe extends Recipe {

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
