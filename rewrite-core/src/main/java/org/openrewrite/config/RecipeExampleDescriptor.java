package org.openrewrite.config;

import lombok.Value;
import org.openrewrite.internal.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Value
public class RecipeExampleDescriptor {
    String recipe;
    String before;
    String after;
    String testClassName;
    String testMethodName;
    List<RecipeExampleParameterDescriptor> parameters;

    @Value
    public static class RecipeExampleParameterDescriptor {
        String name;
        String type;
        String value;
    }

    @Nullable
    public static RecipeExampleDescriptor fromProperties(Properties props) {
        Object recipeName = props.get("recipe");
        Object before = props.get("before");
        Object after = props.get("after");
        if(recipeName == null || before == null || after == null) {
            // We're probably looking at some other properties file and not a Recipe Example
            return null;
        }

        Set<String> recipeOptionNames = props.keySet().stream()
                .map(Object::toString)
                .filter(it -> it.startsWith("recipeOption."))
                .map(it -> {
                    int startIndex = 13; // index of character just after "recipeOption."
                    int endIndex = it.indexOf('.', startIndex);
                    if(endIndex == -1) {
                        endIndex = it.length();
                    }
                    return it.substring(startIndex, endIndex);
                })
                .collect(toSet());

        List<RecipeExampleParameterDescriptor> params = new ArrayList<>();
        for(String recipeOptionName : recipeOptionNames) {
            Object type = props.get("recipeOption." + recipeOptionName + ".type");
            Object value = props.get("recipeOption." + recipeOptionName);
            if(type == null || value == null) {
                // Some kind of problem with this example, just ignore it in favor of other examples that did work correctly
                return null;
            }

            params.add(new RecipeExampleParameterDescriptor(
                    recipeOptionName,
                    type.toString(),
                    value.toString()
            ));
        }

        return new RecipeExampleDescriptor(
                recipeName.toString(),
                before.toString(),
                after.toString(),
                props.get("testClassName").toString(),
                props.get("testMethodName").toString(),
                params
        );
    }
}
