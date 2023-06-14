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
        description = "preCondition recipe FQN and parameters. Used for yaml recipe usage only",
        example = "org.openrewrite.text.FindAndReplace")
    Object preCondition;

    @Option(displayName = "Recipe list to run",
        description = "Recipe list to run.",
        example = "org.openrewrite.text.ChangeText")
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
            // todo, not sure whether returning null is OK here
            return null;
        }

        return Preconditions.check(
            preConditionRecipe.getVisitor(),
            new TreeVisitor<Tree, ExecutionContext>() {
                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    if (tree instanceof SourceFile) {
                        for (Recipe r : recipesToRun) {
                            tree = r.getVisitor().visit(tree, ctx);
                        }
                    }
                    return tree;
                }
            }
        );
    }
}