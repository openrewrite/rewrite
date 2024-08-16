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

import org.openrewrite.Incubating;
import org.openrewrite.Recipe;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Incubating(since = "8.12.1")
public interface RecipePrinter {

    /**
     * The consumer to print the recipe tree to, for instance {@code () -> System.out::println}.
     *
     * @return the consumer
     */
    Consumer<CharSequence> consumer();

    /**
     * Print the recipe tree for a recipe to {@link #consumer()}.
     *
     * @param recipe the recipe to print
     */
    default void printTree(Recipe recipe) {
        consumer().accept(printRecipe(recipe.getDescriptor(), ""));
    }

    /**
     * Internal method to print a recipe tree. This is used recursively to print the tree. Not intended for external use.
     *
     * @param rd     the recipe descriptor
     * @param prefix the indentation prefix
     */
    default CharSequence printRecipe(RecipeDescriptor rd, String prefix) {
        List<OptionDescriptor> options = rd.getOptions();
        StringBuilder recipeString = new StringBuilder(prefix);
        recipeString.append(rd.getName());
        if (options != null && !options.isEmpty()) {
            String opts = options.stream()
                    .map(option -> {
                        if (option.getValue() != null) {
                            return String.format("%s=%s", option.getName(), option.getValue());
                        }
                        return null;
                    }).
                    filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
            if (!opts.isEmpty()) {
                recipeString.append(String.format(": {%s}", opts));
            }
        }
        recipeString.append(System.lineSeparator());

        for (RecipeDescriptor child : rd.getRecipeList()) {
            recipeString.append(printRecipe(child, prefix + "  "));
        }
        return recipeString;
    }
}
