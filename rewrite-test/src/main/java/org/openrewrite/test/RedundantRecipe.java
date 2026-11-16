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
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.config.DeclarativeRecipe;
import org.openrewrite.config.RecipeDescriptor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.fail;

/**
 * Finds a test that sets the recipe its class's {@code defaults(RecipeSpec)} already sets.
 */
final class RedundantRecipe {

    private RedundantRecipe() {
    }

    static void assertNotRepeated(@Nullable Recipe inTest, @Nullable Recipe inDefaults) {
        // RewriteTest#defaults sets Recipe.noop() when a test class doesn't set a recipe
        if (inTest != null && inDefaults != null && !Recipe.noop().equals(inDefaults) && same(inTest, inDefaults)) {
            fail("The test sets spec.recipe(..) to " + inTest.getName() + ", which defaults(RecipeSpec) already " +
                 "sets. Remove it from the test, so that a test which sets its own recipe stands out as testing " +
                 "a different one.");
        }
    }

    private static boolean same(Recipe a, Recipe b) {
        if (a == b) {
            return true;
        }
        if (a.getClass() != b.getClass() || Probes.isAdHoc(a)) {
            return false;
        }
        if (a instanceof DeclarativeRecipe) {
            return same(a.getDescriptor(), b.getDescriptor());
        }
        if (a instanceof CompositeRecipe) {
            List<Recipe> as = a.getRecipeList();
            List<Recipe> bs = b.getRecipeList();
            if (as.size() != bs.size()) {
                return false;
            }
            for (int i = 0; i < as.size(); i++) {
                if (!same(as.get(i), bs.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return overridesEquals(a.getClass()) ? a.equals(b) : sameFields(a, b);
    }

    /**
     * Whether the recipe compares more than its name, as {@link Recipe#equals} does, such as by its options.
     */
    private static boolean overridesEquals(Class<?> recipeClass) {
        try {
            return recipeClass.getMethod("equals", Object.class).getDeclaringClass() != Recipe.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static boolean sameFields(Recipe a, Recipe b) {
        try {
            for (Class<?> c = a.getClass(); c != Recipe.class && c != null; c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                        continue;
                    }
                    field.setAccessible(true);
                    if (!Objects.equals(field.get(a), field.get(b))) {
                        return false;
                    }
                }
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static boolean same(RecipeDescriptor a, RecipeDescriptor b) {
        return a.equals(b) && same(a.getPreconditions(), b.getPreconditions()) &&
               same(a.getRecipeList(), b.getRecipeList());
    }

    private static boolean same(List<RecipeDescriptor> as, List<RecipeDescriptor> bs) {
        if (as.size() != bs.size()) {
            return false;
        }
        for (int i = 0; i < as.size(); i++) {
            if (!same(as.get(i), bs.get(i))) {
                return false;
            }
        }
        return true;
    }
}
