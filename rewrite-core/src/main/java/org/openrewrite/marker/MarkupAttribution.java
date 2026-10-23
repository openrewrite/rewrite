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
package org.openrewrite.marker;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeScheduler;
import org.openrewrite.scheduling.RecipeRunCycle;

final class MarkupAttribution {
    private MarkupAttribution() {
    }

    static Markup attribute(Markup markup) {
        if (markup.getRecipeName() != null) {
            return markup;
        }
        // Not CURRENT_RECIPE: putting a message on the context counts as a change by the running recipe
        ExecutionContext ctx = RecipeScheduler.currentContext();
        RecipeRunCycle<?> cycle = ctx == null ? null : ctx.getMessage(ExecutionContext.CURRENT_CYCLE);
        Recipe recipe = cycle == null ? null : cycle.getCurrentRecipe();
        return recipe == null ? markup : markup.withRecipeName(recipe.getName()).withRecipeInstanceName(recipe.getInstanceName());
    }
}
