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
    // What each composite was found to misorder, so that every test that runs it fails the same way
    private static final Map<String, String> CHECKED = new ConcurrentHashMap<>();

    private CompositeOrder() {
    }

    static void assertOrdered(Recipe recipe) {
        List<Step> steps = new ArrayList<>();
        flatten(recipe, null, 0, Collections.emptySet(), steps, Collections.newSetFromMap(new IdentityHashMap<>()));
        if (steps.size() < 2) {
            return;
        }
        StringJoiner signature = new StringJoiner(",", recipe.getName() + "[", "]");
        for (Step step : steps) {
            signature.add(step.toString());
        }
        String found = CHECKED.computeIfAbsent(signature.toString(), s -> misordered(steps));
        if (!found.isEmpty()) {
            fail("Order the steps of a composite so that a step runs before the steps that rename what it looks for. " +
                 "In " + recipe.getName() + ", these steps look for names that steps before them already renamed, " +
                 "so they never change what they were written for:\n" + found +
                 "Move each step above the step that renames what it looks for, or remove it if that step already " +
                 "does what it should. If the order is meant, add spec.recipeQualityValidation(" +
                 "RecipeQualityValidation.builder().orderStepsForEarlierChanges(false).build()).");
        }
    }

    private static String misordered(List<Step> steps) {
        StringBuilder found = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            String looksFor = step.renaming.oldName();
            String renamed = looksFor;
            int renamedBy = -1;
            for (int j = 0; j < i; j++) {
                Step earlier = steps.get(j);
                if (earlier.applies(step)) {
                    String next = earlier.renaming.rename(renamed);
                    if (!same(next, renamed)) {
                        renamed = next;
                        if (renamedBy < 0) {
                            renamedBy = j;
                        }
                    }
                }
            }
            if (renamedBy < 0 || !step.ownerUnderTest()) {
                continue;
            }
            String endsUp = through(steps, i + 1, steps.size(), step, renamed);
            // had the step run just before the step that renamed what it looks for, that step and the ones after it
            // would have renamed what it wrote
            String meant = through(steps, i + 1, steps.size(), step,
                    through(steps, renamedBy, i, step, step.renaming.rename(looksFor)));
            if (!same(endsUp, meant)) {
                found.append("  ").append(step).append(": ").append(steps.get(renamedBy)).append(" renamed `")
                        .append(looksFor).append("` to `").append(renamed)
                        .append("` first, so code that named it ends up as `").append(endsUp).append("`, not `")
                        .append(meant).append("`\n");
            }
        }
        return found.toString();
    }

    /**
     * What {@code name} becomes once the steps from {@code from} up to {@code to} have run.
     */
    private static String through(List<Step> steps, int from, int to, Step step, String name) {
        String renamed = name;
        for (int j = from; j < to; j++) {
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
        @SuppressWarnings("deprecation") // only the descriptor knows which file a declarative recipe came from
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
