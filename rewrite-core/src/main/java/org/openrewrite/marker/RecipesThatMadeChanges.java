/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.marker;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.Incubating;
import org.openrewrite.Recipe;
import org.openrewrite.config.RecipeDescriptor;

import java.util.*;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@With
public class RecipesThatMadeChanges implements Marker {
    @EqualsAndHashCode.Include
    UUID id;

    Collection<Stack<Recipe>> recipes;

    /**
     * Return a list of recipes that have made changes as a hierarchy of descriptors.
     * The method transforms the flat, stack-based representation into descriptors where children are grouped under their common parents.
     */
    @Incubating(since = "7.22.0")
    public List<RecipeDescriptor> recipeDescriptors() {
        List<RecipeDescriptor> recipesToDisplay = new ArrayList<>();

        for (Stack<Recipe> currentStack : recipes) {
            // The first recipe is an Environment.CompositeRecipe and should not be included in the list of RecipeDescriptors
            Recipe root = currentStack.get(1);
            RecipeDescriptor rootDescriptor = root.getDescriptor().withRecipeList(new ArrayList<>());

            RecipeDescriptor index;
            if (!recipesToDisplay.contains(rootDescriptor)) {
                recipesToDisplay.add(rootDescriptor);
                index = rootDescriptor;
            } else {
                index = recipesToDisplay.get(recipesToDisplay.indexOf(rootDescriptor));
            }

            for (int i = 2; i < currentStack.size(); i++) {
                RecipeDescriptor nextDescriptor = currentStack.get(i).getDescriptor().withRecipeList(new ArrayList<>());
                if (!index.getRecipeList().contains(nextDescriptor)) {
                    index.getRecipeList().add(nextDescriptor);
                    index = nextDescriptor;
                } else {
                    index = index.getRecipeList().get(index.getRecipeList().indexOf(nextDescriptor));
                }
            }
        }
        return recipesToDisplay;
    }
}
