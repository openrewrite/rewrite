package org.openrewrite.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Recipe;

import java.util.HashMap;
import java.util.Map;

public class RecipeLoader {
    @Nullable
    private final ClassLoader classLoader;

    @Getter
    private final ObjectMapper mapper;

    public RecipeLoader(@Nullable ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.mapper = ObjectMappers.propertyBasedMapper(classLoader);
    }

    public Recipe load(String recipeName, @Nullable Map<String, Object> recipeArgs) {
        if (recipeArgs == null || recipeArgs.isEmpty()) {
            try {
                // first try an explicitly-declared zero-arg constructor
                return (Recipe) Class.forName(recipeName, true,
                                classLoader == null ? this.getClass().getClassLoader() : classLoader)
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (ReflectiveOperationException e) {
                return instantiateRecipe(recipeName, new HashMap<>());
            }
        }
        return instantiateRecipe(recipeName, recipeArgs);
    }

    private Recipe instantiateRecipe(String recipeName, Map<String, Object> args) throws IllegalArgumentException {
        Map<Object, Object> withJsonType = new HashMap<>(args);
        withJsonType.put("@c", recipeName);
        return mapper.convertValue(withJsonType, Recipe.class);
    }
}
