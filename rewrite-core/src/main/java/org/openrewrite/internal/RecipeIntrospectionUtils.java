/*
 * Copyright 2020 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.config.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;

public class RecipeIntrospectionUtils {

    public static RecipeDescriptor recipeDescriptorFromRecipeClass(Class<?> recipeClass) {
        List<OptionDescriptor> options = getOptionDescriptors(recipeClass);
        Recipe recipe = constructRecipe(recipeClass);
        RecipeDescriptor recipeDescriptor = new RecipeDescriptor(recipeClass.getName(), recipe.getDisplayName(),
                recipe.getDescription(),
                recipe.getTags(),
                options,
                recipe.getLanguages(),
                emptyList());
        List<RecipeDescriptor> recipeList = new ArrayList<>();
        for (Recipe next : recipe.getRecipeList()) {
            recipeList.add(recipeDescriptorFromRecipe(next));
        }
        return recipeDescriptor.withRecipeList(recipeList);
    }

    public static RecipeDescriptor recipeDescriptorFromDeclarativeRecipe(DeclarativeRecipe recipe) {
        List<RecipeDescriptor> recipeList = new ArrayList<>();
        for (Recipe childRecipe : recipe.getRecipeList()) {
            recipeList.add(recipeDescriptorFromRecipe(childRecipe));
        }
        return new RecipeDescriptor(recipe.getName(), recipe.getDisplayName(), recipe.getDescription(),
                recipe.getTags(), emptyList(), recipe.getLanguages(), recipeList);
    }

    public static Constructor<?> getPrimaryConstructor(Class<?> recipeClass) {
        Constructor<?>[] constructors = recipeClass.getConstructors();
        if (constructors.length == 0) {
            // kotlin object declarations have no constructors at all
            throw new RecipeIntrospectionException("Unable to locate primary constructor for Recipe " + recipeClass);
        } else if (recipeClass.getConstructors().length == 1) {
            return recipeClass.getConstructors()[0];
        } else {
            for (Constructor<?> constructor : constructors) {
                if (constructor.isAnnotationPresent(JsonCreator.class)) {
                    return constructor;
                }
            }
            throw new RecipeIntrospectionException("Unable to locate primary constructor for Recipe " + recipeClass);
        }
    }

    public static RecipeDescriptor recipeDescriptorFromRecipe(Recipe recipe) {
        List<OptionDescriptor> options = getOptionsDescriptors(recipe);
        List<RecipeDescriptor> recipeList = new ArrayList<>();
        for (Recipe next : recipe.getRecipeList()) {
            recipeList.add(recipeDescriptorFromRecipe(next));
        }
        return new RecipeDescriptor(recipe.getName(), recipe.getDisplayName(), recipe.getDescription(),
                recipe.getTags(), options, recipe.getLanguages(), recipeList);
    }

    private static Recipe constructRecipe(Class<?> recipeClass) {
        Constructor<?> primaryConstructor = getPrimaryConstructor(recipeClass);
        Object[] constructorArgs = new Object[primaryConstructor.getParameterCount()];
        for (int i = 0; i < primaryConstructor.getParameters().length; i++) {
            java.lang.reflect.Parameter param = primaryConstructor.getParameters()[i];
            if (param.getType().isPrimitive()) {
                constructorArgs[i] = getPrimitiveDefault(param.getType());
            } else {
                constructorArgs[i] = null;
            }
        }
        primaryConstructor.setAccessible(true);
        try {
            return (Recipe) primaryConstructor.newInstance(constructorArgs);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            // Should never happen
            throw new RecipeIntrospectionException("Unable to call primary constructor for Recipe " + recipeClass, e);
        }
    }

    private static List<OptionDescriptor> getOptionDescriptors(Class<?> recipeClass) {
        List<OptionDescriptor> options = new ArrayList<>();

        for (Field field : recipeClass.getDeclaredFields()) {
            Option option = field.getAnnotation(Option.class);
            if (option != null) {
                options.add(new OptionDescriptor(field.getName(),
                        field.getType().getSimpleName(),
                        option.displayName(),
                        option.description(),
                        option.example().isEmpty() ? null : option.example(),
                        option.valid().length == 1 && option.valid()[0].isEmpty() ? null : Arrays.asList(option.valid()),
                        option.required(),
                        null));
            }
        }
        return options;
    }

    private static List<OptionDescriptor> getOptionsDescriptors(Recipe recipe) {
        List<OptionDescriptor> options = new ArrayList<>();

        for (Field field : recipe.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Option option = field.getAnnotation(Option.class);
            if (option != null) {
                Object fieldValue;
                try {
                    fieldValue = field.get(recipe);
                } catch (IllegalAccessException e) {
                    throw new RecipeIntrospectionException("Error getting recipe option value, recipe: " +
                            recipe.getClass().getName() + ", option: " + field.getName(), e);
                }
                options.add(new OptionDescriptor(field.getName(),
                        field.getType().getSimpleName(),
                        option.displayName(),
                        option.description(),
                        option.example().isEmpty() ? null : option.example(),
                        option.valid().length == 1 && option.valid()[0].isEmpty() ? null : Arrays.asList(option.valid()),
                        option.required(),
                        fieldValue));
            }
        }
        return options;
    }

    private static Object getPrimitiveDefault(Class<?> t) {
        if (t.equals(byte.class)) {
            return (byte) 0;
        } else if (t.equals(short.class)) {
            return (short) 0;
        } else if (t.equals(int.class)) {
            return 0;
        } else if (t.equals(long.class)) {
            return 0L;
        } else if (t.equals(float.class)) {
            return 0.0f;
        } else if (t.equals(double.class)) {
            return 0.0d;
        } else if (t.equals(char.class)) {
            return '\u0000';
        } else if (t.equals(boolean.class)) {
            return false;
        } else {
            throw new RecipeIntrospectionException(t.getCanonicalName() + " is not a supported primitive type");
        }
    }
}
