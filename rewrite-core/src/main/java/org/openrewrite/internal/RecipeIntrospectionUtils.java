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

import org.openrewrite.*;
import org.openrewrite.config.*;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;

@SuppressWarnings("unused")
public class RecipeIntrospectionUtils {
    @Nullable
    public static TreeVisitor<?, ExecutionContext> recipeApplicableTest(Recipe recipe) {
        try {
            Method getVisitor = recipe.getClass().getDeclaredMethod("getApplicableTest");
            getVisitor.setAccessible(true);
            //noinspection unchecked
            return (TreeVisitor<?, ExecutionContext>) getVisitor.invoke(recipe);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
            return null;
        }
    }

    @Nullable
    public static TreeVisitor<?, ExecutionContext> recipeSingleSourceApplicableTest(Recipe recipe) {
        try {
            Method getVisitor = recipe.getClass().getDeclaredMethod("getSingleSourceApplicableTest");
            getVisitor.setAccessible(true);
            //noinspection unchecked
            return (TreeVisitor<?, ExecutionContext>) getVisitor.invoke(recipe);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
            return null;
        }
    }

    public static TreeVisitor<?, ExecutionContext> recipeVisitor(Recipe recipe) {
        try {
            Method getVisitor = ReflectionUtils.findMethod(recipe.getClass(), "getVisitor");
            if (getVisitor == null) {
                throw new RecipeIntrospectionException("Recipe " + recipe.getName() + " does not implement getVisitor()");
            }
            getVisitor.setAccessible(true);
            //noinspection unchecked
            return (TreeVisitor<?, ExecutionContext>) getVisitor.invoke(recipe);
        } catch (InvocationTargetException | IllegalAccessException t) {
            throw new RecipeIntrospectionException("Unable to invoke getVisitor() on " + recipe.getClass().getName(), t);
        }
    }

    public static List<SourceFile> recipeVisit(Recipe recipe, List<SourceFile> before, ExecutionContext ctx) {
        try {
            Method visit = ReflectionUtils.findMethod(recipe.getClass(), "visit", List.class, ExecutionContext.class);
            if (visit == null) {
                throw new RecipeIntrospectionException("Recipe " + recipe.getClass().getName() + " does not implement visit(List<SourceFile>, ExecutionContext)");
            }
            visit.setAccessible(true);
            //noinspection unchecked
            return (List<SourceFile>) visit.invoke(recipe, before, ctx);
        } catch (InvocationTargetException | IllegalAccessException t) {
            throw new RecipeIntrospectionException("Unable to invoke visit(List<SourceFile>, ExecutionContext) on " + recipe.getClass().getName(), t);
        }
    }

    public static DataTableDescriptor dataTableDescriptorFromDataTable(DataTable<?> dataTable) {
        return new DataTableDescriptor(dataTable.getName(), dataTable.getDisplayName(), dataTable.getDescription(),
                getColumnDescriptors(dataTable));
    }

    public static RecipeDescriptor recipeDescriptorFromDeclarativeRecipe(DeclarativeRecipe recipe, URI source) {
        List<RecipeDescriptor> recipeList = new ArrayList<>();
        for (Recipe childRecipe : recipe.getRecipeList()) {
            recipeList.add(recipeDescriptorFromRecipe(childRecipe));
        }
        //noinspection deprecation
        return new RecipeDescriptor(recipe.getName(), recipe.getDisplayName(), recipe.getDescription(),
                recipe.getTags(), recipe.getEstimatedEffortPerOccurrence(),
                emptyList(), recipe.getLanguages(), recipeList, recipe.getDataTableDescriptors(), source);
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
                for (Annotation annotation : constructor.getAnnotations()) {
                    if ("com.fasterxml.jackson.annotation.JsonCreator".equals(annotation.annotationType().getName())) {
                        return constructor;
                    }
                }
            }
            throw new RecipeIntrospectionException("Unable to locate primary constructor for Recipe " + recipeClass);
        }
    }

    @Nullable
    public static Constructor<?> getZeroArgsConstructor(Class<?> recipeClass) {
        Constructor<?>[] constructors = recipeClass.getConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() == 0) {
                return constructor;
            }
        }
        return null;
    }

    public static RecipeDescriptor recipeDescriptorFromRecipe(Recipe recipe) {
        List<OptionDescriptor> options = getOptionsDescriptors(recipe);
        List<RecipeDescriptor> recipeList = new ArrayList<>();
        for (Recipe next : recipe.getRecipeList()) {
            recipeList.add(recipeDescriptorFromRecipe(next));
        }
        URI recipeSource;
        try {
            recipeSource = recipe.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        //noinspection deprecation
        return new RecipeDescriptor(recipe.getName(), recipe.getDisplayName(),
                recipe.getDescription(), recipe.getTags(), recipe.getEstimatedEffortPerOccurrence(),
                options, recipe.getLanguages(), recipeList, recipe.getDataTableDescriptors(), recipeSource);
    }

    public static Recipe constructRecipe(Class<?> recipeClass) {
        return construct(recipeClass);
    }

    private static <V> V construct(Class<?> clazz) {
        Constructor<?> primaryConstructor = getZeroArgsConstructor(clazz);
        if (primaryConstructor == null) {
            primaryConstructor = getPrimaryConstructor(clazz);
        }
        Object[] constructorArgs = new Object[primaryConstructor.getParameterCount()];
        for (int i = 0; i < primaryConstructor.getParameters().length; i++) {
            java.lang.reflect.Parameter param = primaryConstructor.getParameters()[i];
            if (param.getType().isPrimitive()) {
                constructorArgs[i] = getPrimitiveDefault(param.getType());
            } else if (param.getType().equals(String.class)) {
                constructorArgs[i] = "";
            } else if (Enum.class.isAssignableFrom(param.getType())) {
                try {
                    Object[] values = (Object[]) param.getType().getMethod("values").invoke(null);
                    constructorArgs[i] = values[0];
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } else if (List.class.isAssignableFrom(param.getType())) {
                constructorArgs[i] = emptyList();
            } else {
                constructorArgs[i] = null;
            }
        }
        primaryConstructor.setAccessible(true);
        try {
            //noinspection unchecked
            return (V) primaryConstructor.newInstance(constructorArgs);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            // Should never happen
            throw getRecipeIntrospectionException(clazz, e);
        }
    }

    @NonNull
    private static RecipeIntrospectionException getRecipeIntrospectionException(Class<?> recipeClass, ReflectiveOperationException e) {
        return new RecipeIntrospectionException("Unable to call primary constructor for Recipe " + recipeClass, e);
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

    private static List<ColumnDescriptor> getColumnDescriptors(DataTable<?> dataTable) {
        List<ColumnDescriptor> columns = new ArrayList<>();

        for (Field field : dataTable.getType().getDeclaredFields()) {
            field.setAccessible(true);
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                Object fieldValue;
                columns.add(new ColumnDescriptor(field.getName(),
                        field.getType().getSimpleName(),
                        column.displayName(),
                        column.description()));
            }
        }
        return columns;
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
