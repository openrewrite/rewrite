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

import static java.util.stream.Collectors.joining;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@With
public class RecipesThatMadeChanges implements Marker {
    @EqualsAndHashCode.Include
    UUID id;

    Collection<Stack<Recipe>> recipes;

    /**
     * A list of recipe names and their associated options which have made changes to the AST
     * @param indent string prepended to sub recipes for logging a hierarchical view
     */
    @Incubating(since = "7.22.0")
    public List<String> recipeNamesAndOptionsThatMadeChanges(String indent) {
        List<RecipeDescriptor> recipesToDisplay = new ArrayList<>();

        for (Stack<Recipe> currentStack : recipes) {

            Recipe root = currentStack.get(1);
            RecipeDescriptor rootDescriptor = root.getDescriptor().withRecipeList(new ArrayList<>());

            RecipeDescriptor index;
            if (!recipesToDisplay.contains(rootDescriptor)) {
                recipesToDisplay.add(rootDescriptor);
                index = rootDescriptor;
            } else {
                index = recipesToDisplay.get(recipesToDisplay.indexOf(rootDescriptor));
            }

            for (int i = 1; i < currentStack.size(); i++) {
                Recipe child = currentStack.get(i);
                if (child == root) {
                    continue;
                }
                RecipeDescriptor childDescriptor = child.getDescriptor().withRecipeList(new ArrayList<>());
                if (!index.getRecipeList().contains(childDescriptor)) {
                    index.getRecipeList().add(childDescriptor);
                    index = childDescriptor;
                } else {
                    index = index.getRecipeList().get(index.getRecipeList().indexOf(childDescriptor));
                }
            }
        }
        String prefix = "" + indent;
        List<String> recipeNamesAndOptions = new ArrayList<>();
        for (RecipeDescriptor recipeDescriptor : recipesToDisplay) {
            addRecipeNameAndOptions(recipeDescriptor, prefix, recipeNamesAndOptions);
            prefix = prefix + indent;
        }
        return recipeNamesAndOptions;
    }

    private void addRecipeNameAndOptions(RecipeDescriptor rd, String prefix, List<String> recipeNamesAndOptions) {
        StringBuilder recipeString = new StringBuilder(prefix + rd.getName());
        if (!rd.getOptions().isEmpty()) {
            String opts = rd.getOptions().stream().map(option -> {
                        if (option.getValue() != null) {
                            return option.getName() + "=" + option.getValue();
                        }
                        return null;
                    }
            ).filter(Objects::nonNull).collect(joining(", "));
            if (!opts.isEmpty()) {
                recipeString.append(": {").append(opts).append("}");
            }
        }
        recipeNamesAndOptions .add(recipeString.toString());
        if (rd.getRecipeList() != null) {
            for (RecipeDescriptor rchild : rd.getRecipeList()) {
                addRecipeNameAndOptions(rchild, prefix + "    ", recipeNamesAndOptions );
            }
        }
    }
}
