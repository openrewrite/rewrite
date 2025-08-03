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

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.config.ColumnDescriptor;
import org.openrewrite.config.DataTableDescriptor;
import org.openrewrite.config.RecipeIntrospectionException;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyList;

@SuppressWarnings("unused")
public class RecipeIntrospectionUtils {

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

    public static Constructor<?> getPrimaryConstructor(Class<?> recipeClass) {
        Constructor<?>[] constructors = recipeClass.getConstructors();
        if (constructors.length == 0) {
            // kotlin object declarations have no constructors at all
            throw new RecipeIntrospectionException("Unable to locate primary constructor for Recipe " + recipeClass);
        } else if (recipeClass.getConstructors().length == 1) {
            return recipeClass.getConstructors()[0];
        } else {
            Constructor<?> constructor = findJsonCreator(constructors);
            if (constructor != null) {
                return constructor;
            }
            throw new RecipeIntrospectionException("Unable to locate primary constructor for Recipe " + recipeClass);
        }
    }

    private static @Nullable Constructor<?> findJsonCreator(Constructor<?>[] constructors) {
        for (Constructor<?> constructor : constructors) {
            for (Annotation annotation : constructor.getAnnotations()) {
                if ("com.fasterxml.jackson.annotation.JsonCreator".equals(annotation.annotationType().getName())) {
                    return constructor;
                }
            }
        }
        return null;
    }

    public static @Nullable Constructor<?> getZeroArgsConstructor(Class<?> recipeClass) {
        Constructor<?>[] constructors = recipeClass.getConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() == 0) {
                return constructor;
            }
        }
        return null;
    }

    public static Recipe constructRecipe(Class<?> recipeClass) {
        return construct(recipeClass, null);
    }

    public static Recipe constructRecipe(Class<?> recipeClass, Map<String, Object> args) {
        return construct(recipeClass, args);
    }

    private static <V> V construct(Class<?> clazz, @Nullable Map<String, Object> args) {
        Constructor<?> constructor = getConstructor(clazz, args);
        Object @Nullable[] constructorArgs = new Object[constructor.getParameterCount()];
        for (int i = 0; i < constructor.getParameters().length; i++) {
            java.lang.reflect.Parameter param = constructor.getParameters()[i];
            if (args != null && args.containsKey(param.getName())) {
                constructorArgs[i] = convert(args.get(param.getName()), param.getType());
            } else if (param.getType().isPrimitive()) {
                constructorArgs[i] = getPrimitiveDefault(param.getType());
            } else if (param.getType().equals(String.class) && isKotlin(clazz)) {
                // Default Recipe::validate is more valuable if we pass null for unconfigured Strings.
                // But, that's not safe for Kotlin non-null types, so use an empty String for those
                // (though it will sneak through default recipe validation)
                constructorArgs[i] = "";
            } else if (Enum.class.isAssignableFrom(param.getType()) && args == null) {
                try {
                    Object[] values = (Object[]) param.getType().getMethod("values").invoke(null);
                    constructorArgs[i] = values[0];
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } else if (List.class.isAssignableFrom(param.getType()) && args == null) {
                constructorArgs[i] = emptyList();
            } else {
                constructorArgs[i] = null;
            }
        }
        constructor.setAccessible(true);
        try {
            //noinspection unchecked
            return (V) constructor.newInstance(constructorArgs);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            // Should never happen
            throw getRecipeIntrospectionException(clazz, e);
        }
    }

    private static Constructor<?> getConstructor(Class<?> clazz, @Nullable Map<String, Object> args) {
        if (args == null || args.isEmpty()) {
            Constructor<?> constructor = getZeroArgsConstructor(clazz);
            if (constructor != null) {
                return constructor;
            }
            return getPrimaryConstructor(clazz);
        } else {
            Constructor<?>[] constructors = clazz.getConstructors();
            Constructor<?> jsonCreator = findJsonCreator(constructors);
            if (jsonCreator != null) {
                return jsonCreator;
            }

            int argCount = args.size();
            OUTER:
            for (Constructor<?> constructor : constructors) {
                if (constructor.getParameterCount() >= argCount) {
                    for (int i = 0; i < constructor.getParameterCount(); i++) {
                        Parameter param = constructor.getParameters()[i];
                        if (!args.containsKey(param.getName())) {
                            continue OUTER;
                        }
                        Class<?> paramType = getParamType(param);
                        if (!paramType.isInstance(args.get(param.getName()))) {
                            continue OUTER;
                        }
                    }
                    return constructor;
                }
            }
        }
        throw new RecipeIntrospectionException("Unable to locate matching constructor for Recipe " + clazz.getName());
    }

    private static Class<?> getParamType(Parameter param) {
        return param.getType().isPrimitive() ? getWrapperType(param.getType()) : param.getType();
    }

    private static Class<?> getWrapperType(Class<?> primitiveType) {
        if (primitiveType == int.class) {
            return Integer.class;
        }
        if (primitiveType == boolean.class) {
            return Boolean.class;
        }
        if (primitiveType == byte.class) {
            return Byte.class;
        }
        if (primitiveType == char.class) {
            return Character.class;
        }
        if (primitiveType == double.class) {
            return Double.class;
        }
        if (primitiveType == float.class) {
            return Float.class;
        }
        if (primitiveType == long.class) {
            return Long.class;
        }
        if (primitiveType == short.class) {
            return Short.class;
        }
        if (primitiveType == void.class) {
            return Void.class;
        }
        return primitiveType;
    }

    private static Object convert(Object o, Class<?> type) {
        if (type == String.class) {
            return Objects.toString(o, null);
        } else if (o instanceof String && type.isEnum()) {
            Object[] values = type.getEnumConstants();
            for (Object value : values) {
                if (value.toString().equalsIgnoreCase((String) o)) {
                    return value;
                }
            }
        }
        return o;
    }

    private static boolean isKotlin(Class<?> clazz) {
        for (Annotation a : clazz.getAnnotations()) {
            if ("kotlin.Metadata".equals(a.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }

    private static RecipeIntrospectionException getRecipeIntrospectionException(Class<?> recipeClass, ReflectiveOperationException e) {
        return new RecipeIntrospectionException("Unable to call primary constructor for Recipe " + recipeClass, e);
    }

    private static List<ColumnDescriptor> getColumnDescriptors(DataTable<?> dataTable) {
        List<ColumnDescriptor> columns = new ArrayList<>();

        for (Field field : dataTable.getType().getDeclaredFields()) {
            field.setAccessible(true);
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                columns.add(new ColumnDescriptor(field.getName(),
                        field.getType().getSimpleName(),
                        column.displayName(),
                        column.description()));
            }
        }
        return columns;
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
