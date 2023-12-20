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

import lombok.Value;
import org.openrewrite.Incubating;
import org.openrewrite.Recipe;
import org.openrewrite.config.RecipeDescriptor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@Incubating(since = "8.11.3")
public interface RecipeTreePrinter {

    RecipeTreePrinter DEFAULT = new SystemOutRecipeTreePrinter();

    Consumer<String> consumeRecipeDescriptor(String prefix);

    /**
     * Print the recipe tree for a recipe.
     *
     * @param recipe the
     */
    default void printTree(Recipe recipe) {
        printRecipe(recipe.getDescriptor(), "");
    }

    /**
     * @return a set of recipe names to skip when printing the recipe tree.
     */
    default Set<String> getRecipesToSkipChildren() {
        return Collections.emptySet();
    }

    /**
     * Print the recipe tree for a recipe descriptor.
     *
     * @param rd     the recipe descriptor
     * @param prefix the indentation prefix
     */
    default void printRecipe(RecipeDescriptor rd, String prefix) {
        Set<String> recipesToSkipChildren = getRecipesToSkipChildren();
        if (recipesToSkipChildren.contains(rd.getName())) {
            return;
        }

        rd.print(consumeRecipeDescriptor(prefix));

        for (RecipeDescriptor child : rd.getRecipeList()) {
            printRecipe(child, prefix + "  ");
        }
    }

    @Value
    class SystemOutRecipeTreePrinter implements RecipeTreePrinter {

        Set<String> recipesToSkipChildren = new HashSet<>();

        @Override
        public Consumer<String> consumeRecipeDescriptor(String prefix) {
            return s -> {
                System.out.print(prefix);
                System.out.println(s);
            };
        }
    }
}
