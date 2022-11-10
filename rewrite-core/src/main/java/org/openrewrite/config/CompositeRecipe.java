/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.config;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * A recipe that exists only to wrap other recipes.
 * Anonymous recipe classes aren't serializable/deserializable so use this, or another named type, instead
 */
class CompositeRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return getName();
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        Duration total = Duration.ofMinutes(0);
        for (Recipe recipe : getRecipeList()) {
            if (recipe.getEstimatedEffortPerOccurrence() != null) {
                total = total.plus(recipe.getEstimatedEffortPerOccurrence());
            }
        }

        if (total.getSeconds() == 0) {
            return Duration.ofMinutes(5);
        }

        return total;
    }

    @Override
    public List<TreeVisitor<?, ExecutionContext>> getSingleSourceApplicableTests() {
        List<TreeVisitor<?, ExecutionContext>> tests = new ArrayList<>(super.getSingleSourceApplicableTests());
        for (Recipe recipe : getRecipeList()) {
            tests.addAll(recipe.getSingleSourceApplicableTests());
        }
        return tests;
    }

    @Override
    public List<TreeVisitor<?, ExecutionContext>> getApplicableTests() {
        List<TreeVisitor<?, ExecutionContext>> tests = new ArrayList<>(super.getApplicableTests());
        for (Recipe recipe : getRecipeList()) {
            tests.addAll(recipe.getApplicableTests());
        }
        return tests;
    }
}
