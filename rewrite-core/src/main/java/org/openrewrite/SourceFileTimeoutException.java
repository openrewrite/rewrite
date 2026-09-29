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

import java.nio.file.Path;
import java.time.Duration;

/**
 * Thrown when a recipe exceeds {@link ExecutionContext#SOURCE_FILE_TIMEOUT} while scanning or editing a single
 * source file.
 */
@Incubating(since = "8.93.0")
public class SourceFileTimeoutException extends RuntimeException {
    private final Recipe recipe;
    private final Path sourcePath;

    public SourceFileTimeoutException(Recipe recipe, Path sourcePath, Duration timeout) {
        super("Recipe " + recipe.getName() + " exceeded the source file timeout of " + timeout + " on " + sourcePath + ".");
        this.recipe = recipe;
        this.sourcePath = sourcePath;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public Path getSourcePath() {
        return sourcePath;
    }
}
