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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;
import static java.util.Collections.newSetFromMap;
import static org.assertj.core.api.Assertions.fail;

/// Checks what recipes keep in their fields and in the static fields of the classes nested in them. A visitor in a
/// static field is shared by every thread visiting source files in parallel, and a [org.openrewrite.Heavyweight]
/// object in any of them stays in memory for as long as the recipe, or the class, does. Recipes a composite lists
/// from dependency jars are left to the builds of the projects that own them.
final class RecipeFields {

    /// The fields, static or not, that a class of the module under test declares and that can hold a reference.
    private static final ClassValue<List<Field>> FIELDS = new ClassValue<List<Field>>() {
        @Override
        protected List<Field> computeValue(Class<?> type) {
            if (!underTest(type)) {
                return emptyList();
            }
            Field[] declared;
            try {
                declared = type.getDeclaredFields();
            } catch (RuntimeException | LinkageError ignored) {
                return emptyList();
            }
            List<Field> fields = new ArrayList<>();
            for (Field field : declared) {
                if (!field.getType().isPrimitive() && !field.isSynthetic()) {
                    try {
                        field.setAccessible(true);
                        fields.add(field);
                    } catch (RuntimeException ignored) {
                    }
                }
            }
            return fields;
        }
    };

    private final Set<Recipe> ran = newSetFromMap(new IdentityHashMap<>());

    /// Records a recipe that ran, which may be another instance than the one its composite's recipe list returns
    /// after the run.
    void ran(Recipe recipe) {
        ran.add(unwrap(recipe));
    }

    static void assertNoStaticVisitors(Recipe recipe) {
        Set<Class<?>> classes = new LinkedHashSet<>();
        for (Recipe r : recipes(recipe)) {
            collectClasses(r, classes);
        }
        Set<String> found = new LinkedHashSet<>();
        for (Class<?> c : classes) {
            for (Field field : FIELDS.get(c)) {
                if (!Modifier.isStatic(field.getModifiers()) || !mayHoldVisitor(field)) {
                    continue;
                }
                String path = AddedMarkers.typeName(c) + "." + field.getName();
                for (Map.Entry<String, TreeVisitor<?, ?>> visitor : visitors(path, read(field, null)).entrySet()) {
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

    /// Walks the fields of the recipe and of each recipe it lists or that ran, and the static fields of their classes
    /// and the classes nested in them, each class once.
    void assertLightweight(Recipe recipe) {
        Set<Recipe> recipes = newSetFromMap(new IdentityHashMap<>());
        recipes.addAll(recipes(recipe));
        recipes.addAll(ran);
        Heavyweights heavyweights = new Heavyweights();
        Set<Class<?>> walkedStatics = new HashSet<>();
        for (Recipe r : recipes) {
            Set<Class<?>> classes = new LinkedHashSet<>();
            collectClasses(r, classes);
            if (classes.isEmpty()) {
                continue;
            }
            try {
                Heavyweights.Walk walk = heavyweights.walk(r.getName(), null, true);
                String name = AddedMarkers.typeName(r.getClass());
                for (Class<?> c = r.getClass(); c != Recipe.class && c != ScanningRecipe.class; c = c.getSuperclass()) {
                    for (Field field : FIELDS.get(c)) {
                        if (!Modifier.isStatic(field.getModifiers())) {
                            walk.from(read(field, r), name + "." + field.getName());
                        }
                    }
                }
                for (Class<?> c : classes) {
                    if (walkedStatics.add(c)) {
                        for (Field field : FIELDS.get(c)) {
                            if (Modifier.isStatic(field.getModifiers())) {
                                walk.from(read(field, null), AddedMarkers.typeName(c) + "." + field.getName());
                            }
                        }
                    }
                }
                walk.drain();
            } catch (RuntimeException ignored) {
                // a recipe the walk can't read is left alone
            }
        }
        if (!heavyweights.isEmpty()) {
            fail("A recipe should keep in its fields, and in the static fields of the classes nested in it, what it " +
                 "is configured with and none of the Heavyweight objects it visits, such as source files, cursors, " +
                 "the ExecutionContext and build models:\n" + heavyweights.report() +
                 "A recipe stays in memory for the whole run, and a static field for as long as its class, so what " +
                 "they keep for each source file adds up over a large repository. Keep what is needed across source " +
                 "files in a ScanningRecipe's accumulator, as values such as paths and ids. A recipe that must keep " +
                 "a Heavyweight object in a field opts out with " +
                 "RecipeQualityValidation.builder().lightweightRecipeFields(false).");
        }
    }

    /// The recipe, the recipes it lists and their preconditions, each once.
    private static Set<Recipe> recipes(Recipe recipe) {
        Set<Recipe> recipes = newSetFromMap(new IdentityHashMap<>());
        collectRecipes(recipe, recipes);
        return recipes;
    }

    private static void collectRecipes(Recipe recipe, Set<Recipe> recipes) {
        Recipe r = unwrap(recipe);
        if (!recipes.add(r)) {
            return;
        }
        if (r instanceof RecipePreconditions) {
            for (Recipe precondition : ((RecipePreconditions) r).getPreconditions()) {
                collectRecipes(precondition, recipes);
            }
        }
        for (Recipe child : r.getRecipeList()) {
            collectRecipes(child, recipes);
        }
    }

    private static Recipe unwrap(Recipe recipe) {
        Recipe r = recipe;
        while (r instanceof Recipe.DelegatingRecipe) {
            r = ((Recipe.DelegatingRecipe) r).getDelegate();
        }
        return r;
    }

    /// The classes of the recipe up to [Recipe], and the classes nested in each.
    private static void collectClasses(Recipe recipe, Set<Class<?>> classes) {
        if (recipe instanceof AdHocRecipe || recipe instanceof AdHocScanningRecipe) {
            return;
        }
        for (Class<?> c = recipe.getClass(); c != Recipe.class && c != ScanningRecipe.class; c = c.getSuperclass()) {
            collectNested(c, classes);
        }
    }

    private static @Nullable Object read(Field field, @Nullable Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException | RuntimeException | LinkageError ignored) {
            // a field that can't be read, or whose class fails to initialize, can't be walked
            return null;
        }
    }

    private static boolean underTest(Class<?> c) {
        CodeSource source = c.getProtectionDomain().getCodeSource();
        URL location = source == null ? null : source.getLocation();
        if (location == null) {
            return false;
        }
        try {
            return CompositeOrder.fromModuleUnderTest(location.toURI());
        } catch (URISyntaxException ignored) {
            return !location.getPath().endsWith(".jar");
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
        } else if (value instanceof Collection && Heavyweights.isJdk(value.getClass())) {
            int i = 0;
            for (Object element : (Collection<?>) value) {
                addIfVisitor(found, path + "[" + i++ + "]", element);
            }
        } else if (value instanceof Map && Heavyweights.isJdk(value.getClass())) {
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

    private static boolean mayHoldVisitor(Field field) {
        Class<?> type = field.getType();
        if (type.isArray()) {
            return mayBeVisitor(type.getComponentType());
        }
        boolean map = Map.class.isAssignableFrom(type);
        if (map || Collection.class.isAssignableFrom(type) || Optional.class.equals(type) ||
            AtomicReference.class.isAssignableFrom(type)) {
            return mayBeVisitor(elementType(field, map ? 1 : 0));
        }
        return mayBeVisitor(type);
    }

    private static Type elementType(Field field, int index) {
        try {
            Type generic = field.getGenericType();
            if (generic instanceof ParameterizedType) {
                Type[] arguments = ((ParameterizedType) generic).getActualTypeArguments();
                if (index < arguments.length) {
                    return arguments[index];
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
        return Object.class;
    }

    private static boolean mayBeVisitor(Type type) {
        Type raw = type instanceof ParameterizedType ? ((ParameterizedType) type).getRawType() : type;
        if (!(raw instanceof Class)) {
            return true;
        }
        Class<?> c = (Class<?>) raw;
        return TreeVisitor.class.isAssignableFrom(c) || c.isAssignableFrom(TreeVisitor.class);
    }
}
