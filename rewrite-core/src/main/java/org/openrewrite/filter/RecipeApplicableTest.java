/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.filter;

import org.openrewrite.*;

/**
 * A {@link Recipe#getApplicableTests()} test to be applied to {@link Recipe} executions.
 * </p>
 * <b>Note: Heavily Incubating</b>
 * </p>
 * </p>
 * To use this class properly, use {@link #addToExecutionContext}.
 */
@Incubating(since = "7.36.0")
@FunctionalInterface
public interface RecipeApplicableTest {

    /**
     * The test should implement the specification described on {@link Recipe#getApplicableTest()}.
     *
     * @param recipe The current {@link Recipe} the {@link Recipe#getApplicableTest()} is being applied to.
     *               This will be the same as {@link ExecutionContext#getCurrentRecipe()}.
     * @return A tree visitor that performs an applicability test.
     */
    TreeVisitor<?, ExecutionContext> getTest(Recipe recipe);

    /**
     * Adds a {@link RecipeApplicableTest} to the {@link ExecutionContext} so the {@link RecipeScheduler} will
     * configure all <i>future</i> {@link Recipe} executions.
     * </p>
     * This allows any {@link Recipe} to influence the behavior/applicability of all future {@link Recipe} executions.
     * </p>
     * This method is best invoked within the {@link Recipe#visit} method.
     */
    static void addToExecutionContext(ExecutionContext ctx, RecipeApplicableTest test) {
        ctx.putMessageInSet(RecipeApplicableTest.class.getSimpleName(), test);
    }
}
