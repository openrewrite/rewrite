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
package org.openrewrite;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OptionGroupTest implements RewriteTest {

    @Value
    public static class AnOptionGroup {
        @Option(displayName = "Option within a nested class", description = "Option")
        String aNestedOption;
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class ARecipe extends Recipe {

        @Option(displayName = "An option", description = "Option")
        String anOption;

        @OptionGroup
        AnOptionGroup aGroup;

        @Override
        public String getDisplayName() {
            return "A Recipe";
        }

        @Override
        public String getDescription() {
            return "A Recipe Description";
        }
    }

    @Test
    public void nestedOption() {
        ARecipe recipe = new ARecipe(
                "An option",
                new AnOptionGroup("A nested option")
        );
        RecipeDescriptor descriptor = recipe.getDescriptor();
        assertEquals(2, descriptor.getOptions().size(), "Option count");
    }

    @Test
    void loadingNested() {
        Environment env = Environment.builder()
                .load(new YamlResourceLoader(new ByteArrayInputStream(
                        //language=yml
                        """
                                type: specs.openrewrite.org/v1beta/recipe
                                name: test.LoadNestedRecipe
                                displayName: Load Nested Recipe
                                recipeList:
                                    - org.openrewrite.OptionGroupTest$ARecipe:
                                        anOption: Hello!
                                        aNestedOption: World!
                                """.getBytes()
                ), URI.create("rewrite.yml"), new Properties()))
                .build();

        Collection<RecipeDescriptor> yamlRecipeDescriptors = env.listRecipeDescriptors();
        assertThat(yamlRecipeDescriptors).hasSize(1);
        RecipeDescriptor descriptor = yamlRecipeDescriptors.iterator().next();
        Collection<RecipeDescriptor> recipeDescriptorList = descriptor.getRecipeList();
        assertThat(recipeDescriptorList).hasSize(1);
        RecipeDescriptor recipeDescriptor = recipeDescriptorList.iterator().next();
        assertThat(recipeDescriptor.getOptions()).hasSize(2);

        List<Recipe> recipes = env.listRecipes();
        assertThat(recipes).hasSize(1);
        Recipe loadRecipe = recipes.iterator().next();
        assertThat(loadRecipe.getRecipeList()).hasSize(1);
        Recipe aRecipeUntyped = loadRecipe.getRecipeList().iterator().next();
        assertThat(aRecipeUntyped).isExactlyInstanceOf(ARecipe.class);
        ARecipe aRecipe = (ARecipe) aRecipeUntyped;
        assertThat(aRecipe.getAnOption()).isEqualTo("Hello!");
        assertThat(aRecipe.getAGroup().getANestedOption()).isEqualTo("World!");
    }

}
