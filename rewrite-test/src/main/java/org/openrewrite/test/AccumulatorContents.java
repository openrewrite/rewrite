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

import org.openrewrite.*;

import java.util.*;

import static java.util.Collections.newSetFromMap;
import static org.assertj.core.api.Assertions.fail;

/// Finds the [Heavyweight] objects, such as trees, that ScanningRecipe accumulators keep. An accumulator lives from the
/// first source file scanned until the cycle ends.
final class AccumulatorContents {

    /// The scanning recipes that ran in this cycle, in the order they first ran.
    private final List<ScanningRecipe<?>> scanningRecipes = new ArrayList<>();
    private final Set<Recipe> ran = newSetFromMap(new IdentityHashMap<>());

    private final Heavyweights heavyweights = new Heavyweights();

    void startCycle() {
        scanningRecipes.clear();
        ran.clear();
    }

    void ran(Recipe recipe) {
        if (recipe instanceof ScanningRecipe && ran.add(recipe)) {
            scanningRecipes.add((ScanningRecipe<?>) recipe);
        }
    }

    /// Walks the accumulator of each scanning recipe that ran in this cycle so far.
    void check(Cursor rootCursor, ExecutionContext ctx, String when) {
        Set<Object> walked = newSetFromMap(new IdentityHashMap<>());
        for (ScanningRecipe<?> recipe : scanningRecipes) {
            Object acc = recipe.getAccumulator(rootCursor, ctx);
            if (acc != null && walked.add(acc)) {
                try {
                    Heavyweights.Walk walk = heavyweights.walk(recipe.getName(), when, false);
                    walk.from(acc, AddedMarkers.typeName(acc.getClass()));
                    walk.drain();
                } catch (RuntimeException ignored) {
                    // an accumulator the walk can't read is left alone
                }
            }
        }
    }

    void assertNoneFound() {
        if (!heavyweights.isEmpty()) {
            fail("A ScanningRecipe's accumulator should keep the values the recipe needs, such as coordinates, " +
                 "property values, paths and ids, and no Heavyweight objects, such as trees, cursors, visitors and " +
                 "build models:\n" + heavyweights.report() +
                 "An accumulator stays in memory from the first source file scanned until the cycle ends, so what " +
                 "it keeps for each source file adds up over a large repository. A MavenResolutionResult or " +
                 "GradleProject can reach the model of a whole build, and a JavaSourceSet a whole classpath, while " +
                 "small markers such as JavaProject are fine to keep. A recipe that must keep a Heavyweight object " +
                 "opts out with RecipeQualityValidation.builder().lightweightAccumulators(false).");
        }
    }
}
