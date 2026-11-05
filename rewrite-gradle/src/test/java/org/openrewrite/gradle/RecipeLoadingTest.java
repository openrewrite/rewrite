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
package org.openrewrite.gradle;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RewriteTest;

import java.util.List;

class RecipeLoadingTest implements RewriteTest {

    private static final String PACKAGE = "org.openrewrite.gradle";

    @Test
    void recipesMakeNoChangeWithoutSources() {
        List<Recipe> recipes = Environment.builder()
                .scanRuntimeClasspath(PACKAGE)
                .build()
                .listRecipes();
        SoftAssertions softly = new SoftAssertions();
        for (Recipe recipe : recipes) {
            // Building blocks that require options cannot be instantiated without them, so the
            // environment's copy never validates; only recipes a user could run as-is are checked.
            if (recipe.getName().startsWith(PACKAGE) && recipe.validate().isValid()) {
                softly.assertThatCode(() -> rewriteRun(spec -> spec.recipe(recipe)))
                        .as(recipe.getName())
                        .doesNotThrowAnyException();
            }
        }
        softly.assertAll();
    }
}
