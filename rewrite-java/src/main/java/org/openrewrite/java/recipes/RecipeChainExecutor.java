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
package org.openrewrite.java.recipes;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
public class RecipeChainExecutor extends Recipe {
    @Option(displayName = "preCondition recipe",
        description = "preCondition recipe FQN and parameters. Used for yaml recipe usage only.",
        example = "- org.openrewrite.text.FindAndReplace:\n" +
                  "    find: \"1\"\n" +
                  "    replace: \"A\"")
    Object preCondition;

    @Option(displayName = "Recipe list to run",
        description = "Recipe list to run. Used for yaml recipe usage only.",
        example = "- org.openrewrite.text.ChangeText:\n" +
                  "    toText: \"2\"\n" +
                  "- org.openrewrite.text.ChangeText:\n" +
                  "    toText: \"3\"")
    Object recipes;

    @Override
    public String getDisplayName() {
        return "Single source preconditioned recipe chain executor";
    }

    @Override
    public String getDescription() {
        return "For yaml recipe usage only, alternative solution of single applicable test in yaml recipe.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        ObjectMapper mapper = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
            .build()
            .registerModule(new ParameterNamesModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // load precondition recipe
        Object preconditionRecipeData = ((ArrayList<Object>) preCondition).get(0);
        Map.Entry<String, Object> preconditionRecipeNameAndConfig = ((Map<String, Object>) preconditionRecipeData).entrySet().iterator().next();
        Map<Object, Object> preconditionRecipeWithJsonType = new HashMap<>((Map<String, Object>) preconditionRecipeNameAndConfig.getValue());
        preconditionRecipeWithJsonType.put("@c", preconditionRecipeNameAndConfig.getKey());
        Recipe preConditionRecipe = mapper.convertValue(preconditionRecipeWithJsonType, Recipe.class);

        // Load recipes
        ArrayList<Object> recipeListDataList = (ArrayList<Object>) recipes;
        List<Recipe> recipesToRun = new ArrayList<>();
        for (Object recipeData : recipeListDataList) {
            Map.Entry<String, Object> recipeNameAndConfig = ((Map<String, Object>) recipeData).entrySet().iterator().next();
            Map<Object, Object> recipeWithJsonType = new HashMap<>((Map<String, Object>) recipeNameAndConfig.getValue());
            recipeWithJsonType.put("@c", recipeNameAndConfig.getKey());
            Recipe r = mapper.convertValue(recipeWithJsonType, Recipe.class);
            if (r != null) {
                recipesToRun.add(r);
            }
        }

        if (preConditionRecipe == null) {
            throw new IllegalArgumentException("Precondition has to be a valid recipe");
        }

        return Preconditions.check(
            preConditionRecipe.getVisitor(),
            new TreeVisitor<Tree, ExecutionContext>() {
                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    if (tree instanceof SourceFile) {
                        tree = runRecipes(recipesToRun, tree, ctx);
                    }
                    return tree;
                }
            }
        );
    }

    private static Tree runRecipes(List<Recipe> recipes, Tree tree, ExecutionContext ctx) {
        for (Recipe r : recipes) {
            if (!r.getRecipeList().isEmpty()) {
                tree = runRecipes(r.getRecipeList(), tree, ctx);
            } else {
                tree = r.getVisitor().visitNonNull(tree, ctx);
            }
        }
        return tree;
    }
}