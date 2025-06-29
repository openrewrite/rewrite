/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Recipe;
import org.openrewrite.config.RecipeIntrospectionException;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;

public class RecipeLoader {

    private final @Nullable ClassLoader classLoader;
    private @Nullable ObjectMapper mapper;

    public RecipeLoader(@Nullable ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public Recipe load(String recipeName, @Nullable Map<String, Object> recipeArgs) {
        try {
            Class<?> recipeClass = Class.forName(recipeName, true,
                    classLoader == null ? this.getClass().getClassLoader() : classLoader);
            return RecipeIntrospectionUtils.constructRecipe(recipeClass, recipeArgs == null ? emptyMap() : recipeArgs);
        } catch (ReflectiveOperationException | RecipeIntrospectionException | IllegalArgumentException e) {
            return instantiateRecipeUsingJackson(recipeName, recipeArgs == null ? emptyMap() : recipeArgs);
        }
    }

    public Recipe load(Class<?> recipeClass, @Nullable Map<String, Object> recipeArgs) {
        try {
            return RecipeIntrospectionUtils.constructRecipe(recipeClass, recipeArgs == null ? emptyMap() : recipeArgs);
        } catch (RecipeIntrospectionException | IllegalArgumentException e) {
            return instantiateRecipeUsingJackson(recipeClass.getName(), recipeArgs == null ? emptyMap() : recipeArgs);
        }
    }

    public ObjectMapper getMapper() {
        return mapper != null ? mapper : (mapper = ObjectMappers.propertyBasedMapper(classLoader));
    }

    private Recipe instantiateRecipeUsingJackson(String recipeName, Map<String, Object> args) throws IllegalArgumentException {
        Map<Object, Object> withJsonType = new HashMap<>(args);
        withJsonType.put("@c", recipeName);
        return getMapper().convertValue(withJsonType, Recipe.class);
    }
}
