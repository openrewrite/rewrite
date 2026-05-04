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
package org.openrewrite;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Wraps an exception thrown by a recipe so that {@link ExecutionContext#getOnError()}
 * consumers can attribute the failure to the specific nested recipe and source file
 * that produced it. This is especially useful for large declarative recipes (e.g.
 * thousands of nested recipes) where the underlying stack trace alone does not
 * reveal which recipe ran into trouble.
 */
@Getter
public class RecipeError extends RuntimeException {

    /**
     * The names of the recipes from root to leaf at the time of failure.
     * The last element is the recipe that directly threw.
     */
    private final List<String> recipeStack;

    /**
     * The path of the source file being processed when the error occurred,
     * or {@code null} when the error was raised outside of a per-source context
     * (for example, during scanning recipe generation).
     */
    private final @Nullable String sourcePath;

    public RecipeError(List<String> recipeStack, @Nullable String sourcePath, Throwable cause) {
        super(cause);
        this.recipeStack = Collections.unmodifiableList(recipeStack);
        this.sourcePath = sourcePath;
    }

    /**
     * @return the leaf recipe name (the recipe that directly threw), or
     * {@code "unknown"} if the recipe stack is empty.
     */
    public String getRecipeName() {
        return recipeStack.isEmpty() ? "unknown" : recipeStack.get(recipeStack.size() - 1);
    }
}
