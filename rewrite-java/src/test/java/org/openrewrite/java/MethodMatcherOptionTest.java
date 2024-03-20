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
package org.openrewrite.java;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.openrewrite.OptionGroup;
import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.config.YamlResourceLoader;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

public class MethodMatcherOptionTest {

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class MethodMatcherOptionRecipe extends Recipe {

        @OptionGroup
        @JsonUnwrapped(prefix = "a-")
        MethodMatcherOption aMethodMatcherOption;

        @OptionGroup
        @JsonUnwrapped(prefix = "b-")
        MethodMatcherOption bMethodMatcherOption;

        @OptionGroup
        @JsonUnwrapped(suffix = "-a")
        MethodMatcherOption methodMatcherOptionA;

        @Override
        public String getDisplayName() {
            return "Method Matcher Option Recipe";
        }

        @Override
        public String getDescription() {
            return "Method Matcher Option Recipe Description";
        }
    }

    @Test
    void loadingNested() {
        Environment env = Environment.builder()
                .load(new YamlResourceLoader(new ByteArrayInputStream(
                        //language=yml
                        """
                                type: specs.openrewrite.org/v1beta/recipe
                                name: test.LoadNestedMethodMatcherRecipe
                                displayName: Load Nested Recipe
                                recipeList:
                                    - org.openrewrite.java.MethodMatcherOptionTest$MethodMatcherOptionRecipe:
                                        a-methodPattern: "java.util.List add(..)"
                                        a-matchOverrides: True
                                        b-methodPattern: "java.util.Set add(..)"
                                        methodPattern-a: "java.util.Map put(..)"
                                """.getBytes()
                ), URI.create("rewrite.yml"), new Properties()))
                .build();

        Collection<RecipeDescriptor> yamlRecipeDescriptors = env.listRecipeDescriptors();
        // Sanity check
        assertThat(yamlRecipeDescriptors).hasSize(1);
        RecipeDescriptor descriptor = yamlRecipeDescriptors.iterator().next();
        Collection<RecipeDescriptor> recipeDescriptorList = descriptor.getRecipeList();
        // Sanity check
        assertThat(recipeDescriptorList).hasSize(1);
        RecipeDescriptor recipeDescriptor = recipeDescriptorList.iterator().next();

        assertThat(recipeDescriptor.getOptions()).hasSize(6);
        assertThat(recipeDescriptor.getOptions().stream().map(OptionDescriptor::getName)).contains(
                "a-methodPattern",
                "a-matchOverrides",
                "b-methodPattern",
                "b-matchOverrides",
                "methodPattern-a",
                "matchOverrides-a"
        );

        List<Recipe> recipes = env.listRecipes();
        assertThat(recipes).hasSize(1);
        Recipe loadRecipe = recipes.iterator().next();
        assertThat(loadRecipe.getRecipeList()).hasSize(1);
        Recipe aRecipeUntyped = loadRecipe.getRecipeList().iterator().next();
        assertThat(aRecipeUntyped).isExactlyInstanceOf(MethodMatcherOptionRecipe.class);
        MethodMatcherOptionRecipe methodMatcherOptionRecipe = (MethodMatcherOptionRecipe) aRecipeUntyped;
        assertThatNoException().isThrownBy(() -> methodMatcherOptionRecipe.getAMethodMatcherOption().methodMatcher());
        assertThatNoException().isThrownBy(() -> methodMatcherOptionRecipe.getBMethodMatcherOption().methodMatcher());
        assertThatNoException().isThrownBy(() -> methodMatcherOptionRecipe.getMethodMatcherOptionA().methodMatcher());
    }

}
