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
import org.openrewrite.RenamingRecipe;
import org.openrewrite.Singleton;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.config.DeclarativeRecipe;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.fail;

/**
 * Finds a step of a composite recipe that looks for a name an earlier step already renamed, so that it never changes
 * what it was written for.
 */
final class CompositeOrder {
    private static final Set<String> CHECKED = ConcurrentHashMap.newKeySet();

    private CompositeOrder() {
    }

    static void assertOrdered(Recipe recipe) {
        List<Step> steps = new ArrayList<>();
        flatten(recipe, null, 0, Collections.emptySet(), steps, Collections.newSetFromMap(new IdentityHashMap<>()));
        StringJoiner signature = new StringJoiner(",", recipe.getName() + "[", "]");
        for (Step step : steps) {
            signature.add(step.toString());
        }
        if (steps.size() < 2 || !CHECKED.add(signature.toString())) {
            return;
        }
        StringBuilder found = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            String looksFor = step.renaming.oldName();
            String renamed = looksFor;
            Step renamedBy = null;
            for (int j = 0; j < i; j++) {
                Step earlier = steps.get(j);
                if (earlier.applies(step)) {
                    String next = earlier.renaming.rename(renamed);
                    if (!same(next, renamed)) {
                        renamed = next;
                        if (renamedBy == null) {
                            renamedBy = earlier;
                        }
                    }
                }
            }
            if (renamedBy == null || !step.ownerUnderTest()) {
                continue;
            }
            String endsUp = after(steps, i, step, renamed);
            String meant = after(steps, i, step, step.renaming.rename(looksFor));
            if (!same(endsUp, meant)) {
                found.append("  ").append(step).append(": ").append(renamedBy).append(" renamed `").append(looksFor)
                        .append("` to `").append(renamed).append("` first, so code that named it ends up as `")
                        .append(endsUp).append("`, not `").append(meant).append("`\n");
            }
        }
        if (found.length() > 0) {
            fail("Order the steps of a composite so that a step runs before the steps that rename what it looks for. " +
                 "In " + recipe.getName() + ", these steps look for names that steps before them already renamed, " +
                 "so they never change what they were written for:\n" + found +
                 "Move each step above the step that renames what it looks for, or remove it if that step already " +
                 "does what it should. If the order is meant, add spec.recipeQualityValidation(" +
                 "RecipeQualityValidation.builder().orderStepsForEarlierChanges(false).build()).");
        }
    }

    /**
     * What {@code name} becomes once the steps after the one at {@code index} have run.
     */
    private static String after(List<Step> steps, int index, Step step, String name) {
        String renamed = name;
        for (int j = index + 1; j < steps.size(); j++) {
            if (steps.get(j).applies(step)) {
                renamed = steps.get(j).renaming.rename(renamed);
            }
        }
        return renamed;
    }

    private static boolean same(String a, String b) {
        return a.replace('$', '.').equals(b.replace('$', '.'));
    }

    /**
     * The renaming steps of a recipe in the order they run, each with the composite that lists it and the
     * preconditions that limit where it runs.
     */
    private static void flatten(Recipe recipe, @Nullable Recipe owner, int index, Set<String> gates, List<Step> steps,
                                Set<Recipe> path) {
        Recipe r = recipe;
        while (r instanceof Recipe.DelegatingRecipe) {
            r = ((Recipe.DelegatingRecipe) r).getDelegate();
        }
        if (!path.add(r)) {
            return;
        }
        Set<String> limitedBy = gates;
        if (r instanceof DeclarativeRecipe) {
            for (Recipe precondition : ((DeclarativeRecipe) r).getPreconditions()) {
                if (!(precondition instanceof Singleton)) {
                    limitedBy = new HashSet<>(gates);
                    limitedBy.add(r.getName());
                    break;
                }
            }
        }
        if (r instanceof RenamingRecipe && owner != null) {
            steps.add(new Step((RenamingRecipe) r, r, owner, index, limitedBy));
        }
        List<Recipe> children = r.getRecipeList();
        for (int i = 0; i < children.size(); i++) {
            flatten(children.get(i), r, i + 1, limitedBy, steps, path);
        }
        path.remove(r);
    }

    private static final class Step {
        final RenamingRecipe renaming;
        final Recipe recipe;
        final Recipe owner;
        final int index;
        final Set<String> gates;

        Step(RenamingRecipe renaming, Recipe recipe, Recipe owner, int index, Set<String> gates) {
            this.renaming = renaming;
            this.recipe = recipe;
            this.owner = owner;
            this.index = index;
            this.gates = gates;
        }

        /**
         * Whether this step renames names of the kind {@code later} looks for wherever {@code later} runs.
         */
        boolean applies(Step later) {
            return renaming.kindOfName().equals(later.renaming.kindOfName()) && later.gates.containsAll(gates);
        }

        /**
         * Whether the composite that lists this step comes from the module under test, not from a dependency or from
         * the recipes a test lists itself.
         */
        boolean ownerUnderTest() {
            if (owner instanceof CompositeRecipe) {
                return false;
            }
            URI source = owner.getDescriptor().getSource();
            return source != null && !"jar".equals(source.getScheme()) &&
                   (source.getPath() == null || !source.getPath().endsWith(".jar"));
        }

        @Override
        public String toString() {
            String name = recipe.getName();
            return name.substring(name.lastIndexOf('.') + 1) + " `" + renaming.oldName() + "` to `" +
                   renaming.rename(renaming.oldName()) + "` (step " + index + " of " + owner.getName() + ")";
        }
    }
}
