/*
 * Copyright 2026 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Recipe;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.config.RecipePreconditions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.newSetFromMap;
import static org.assertj.core.api.Assertions.fail;

/**
 * Finds the {@link TreeVisitor}s that recipes keep in static fields. A visitor keeps its cursor and other state while
 * it visits, so one that every thread shares breaks when source files are visited in parallel.
 */
final class StaticVisitors {

    private static final ClassValue<List<Field>> STATIC_FIELDS = new ClassValue<List<Field>>() {
        @Override
        protected List<Field> computeValue(Class<?> type) {
            List<Field> fields = new ArrayList<>();
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && !field.isSynthetic() && mayHoldVisitor(field.getType())) {
                    try {
                        field.setAccessible(true);
                        fields.add(field);
                    } catch (RuntimeException ignored) {
                        // a field that can't be read can't be checked
                    }
                }
            }
            return fields;
        }
    };

    private StaticVisitors() {
    }

    static void assertNoneKept(Recipe recipe) {
        Set<Class<?>> classes = new LinkedHashSet<>();
        collectRecipeClasses(recipe, classes, newSetFromMap(new IdentityHashMap<>()));
        Set<String> found = new LinkedHashSet<>();
        for (Class<?> c : classes) {
            for (Field field : STATIC_FIELDS.get(c)) {
                Object value;
                try {
                    value = field.get(null);
                } catch (IllegalAccessException | RuntimeException | LinkageError ignored) {
                    continue;
                }
                String path = AddedMarkers.typeName(c) + "." + field.getName();
                for (Map.Entry<String, TreeVisitor<?, ?>> visitor : visitors(path, value).entrySet()) {
                    found.add("  " + visitor.getKey() + " -> " + AddedMarkers.typeName(visitor.getValue().getClass()));
                }
            }
        }
        if (!found.isEmpty()) {
            fail("A recipe should create its visitors in getVisitor() and keep none in static fields. A visitor " +
                 "keeps its cursor and other state while it visits, so one that every thread shares fails when " +
                 "source files are visited in parallel, often with a NullPointerException on its cursor. These " +
                 "static fields keep visitors:\n" + String.join("\n", found) + "\n" +
                 "Create each visitor, including a precondition's, where it is used, and keep in static fields only " +
                 "what it is built from, such as MethodMatchers, AnnotationMatchers and type names. If the field " +
                 "is meant, add spec.recipeQualityValidation(" +
                 "RecipeQualityValidation.builder().noStaticVisitors(false).build()).");
        }
    }

    private static void collectRecipeClasses(Recipe recipe, Set<Class<?>> classes, Set<Recipe> seen) {
        Recipe r = recipe;
        while (r instanceof Recipe.DelegatingRecipe) {
            r = ((Recipe.DelegatingRecipe) r).getDelegate();
        }
        if (!seen.add(r)) {
            return;
        }
        if (!(r instanceof AdHocRecipe) && !(r instanceof AdHocScanningRecipe)) {
            for (Class<?> c = r.getClass(); c != Recipe.class && c != ScanningRecipe.class && !isJdk(c);
                 c = c.getSuperclass()) {
                collectNested(c, classes);
            }
        }
        if (r instanceof RecipePreconditions) {
            for (Recipe precondition : ((RecipePreconditions) r).getPreconditions()) {
                collectRecipeClasses(precondition, classes, seen);
            }
        }
        for (Recipe child : r.getRecipeList()) {
            collectRecipeClasses(child, classes, seen);
        }
    }

    private static void collectNested(Class<?> c, Set<Class<?>> classes) {
        if (!classes.add(c)) {
            return;
        }
        try {
            for (Class<?> nested : c.getDeclaredClasses()) {
                collectNested(nested, classes);
            }
        } catch (RuntimeException | LinkageError ignored) {
            // a class whose nested classes can't be loaded is checked on its own
        }
    }

    /**
     * The visitors a static field keeps, directly or in an array or a JDK collection, map, Optional or
     * AtomicReference, by the path to each.
     */
    private static Map<String, TreeVisitor<?, ?>> visitors(String path, @Nullable Object value) {
        Map<String, TreeVisitor<?, ?>> found = new LinkedHashMap<>();
        if (value instanceof TreeVisitor) {
            found.put(path, (TreeVisitor<?, ?>) value);
        } else if (value instanceof Object[]) {
            Object[] elements = (Object[]) value;
            for (int i = 0; i < elements.length; i++) {
                addIfVisitor(found, path + "[" + i + "]", elements[i]);
            }
        } else if (value instanceof Collection && isJdk(value.getClass())) {
            int i = 0;
            for (Object element : (Collection<?>) value) {
                addIfVisitor(found, path + "[" + i++ + "]", element);
            }
        } else if (value instanceof Map && isJdk(value.getClass())) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                addIfVisitor(found, path + "[" + entry.getKey() + "]", entry.getValue());
            }
        } else if (value instanceof Optional) {
            addIfVisitor(found, path + ".get()", ((Optional<?>) value).orElse(null));
        } else if (value instanceof AtomicReference) {
            addIfVisitor(found, path + ".get()", ((AtomicReference<?>) value).get());
        }
        return found;
    }

    private static void addIfVisitor(Map<String, TreeVisitor<?, ?>> found, String path, @Nullable Object value) {
        if (value instanceof TreeVisitor) {
            found.put(path, (TreeVisitor<?, ?>) value);
        }
    }

    private static boolean mayHoldVisitor(Class<?> type) {
        return TreeVisitor.class.isAssignableFrom(type) || type.isAssignableFrom(TreeVisitor.class) ||
               type.isArray() && !type.getComponentType().isPrimitive() ||
               Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type) ||
               Optional.class.equals(type) || AtomicReference.class.isAssignableFrom(type);
    }

    private static boolean isJdk(Class<?> type) {
        String name = type.getName();
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.") ||
               name.startsWith("sun.") || name.startsWith("com.sun.");
    }
}
