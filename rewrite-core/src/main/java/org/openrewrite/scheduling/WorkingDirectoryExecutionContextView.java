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
package org.openrewrite.scheduling;

import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Incubating;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Recipes that need to write (or download somehow and read) resources from disk as part of their
 * operation can use this view to receive a safe directory to use. This directory will not be created
 * unless requested by a recipe, and will be deleted by {@link org.openrewrite.RecipeScheduler} at the
 * end of each cycle. Each recipe in a recipe list will get its own directory to use so there is no
 * cross-contamination of the directory between recipes.
 */
@Incubating(since = "8.12.0")
public class WorkingDirectoryExecutionContextView extends DelegatingExecutionContext {
    public static final String WORKING_DIRECTORY_ROOT = "org.openrewrite.scheduling.workingDirectory";

    private WorkingDirectoryExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static WorkingDirectoryExecutionContextView view(ExecutionContext ctx) {
        return new WorkingDirectoryExecutionContextView(ctx);
    }

    /**
     * This should not be called from recipes, but only from tools that are creating instantiating
     * recipe runs directly. These tools should select a directory that are appropriate to their
     * context and ensure they are cleaned up.
     *
     * @param path The root directory from which individual recipe(+cycle) working directories will
     *             be created.
     */
    public void setRoot(Path path) {
        if (getMessage(CURRENT_CYCLE) != null) {
            throw new IllegalStateException("The root working directory cannot be set once " +
                                            "recipe execution has begun.");
        }
        putMessage(WORKING_DIRECTORY_ROOT, path);
    }

    /**
     * @return A working directory that a recipe may write to. Created only when a recipe
     * requests it by calling this method, and deleted at the end of the recipe cycle.
     */
    public Path getWorkingDirectory() {
        try {
            Path root = getMessage(WORKING_DIRECTORY_ROOT);
            if (root == null) {
                root = Files.createTempDirectory("rewrite-work");
                putMessage(WORKING_DIRECTORY_ROOT, root);
            }
            RecipeRunCycle<?> cycle = getCycleDetails();
            return Files.createDirectories(root.resolve("cycle" + cycle.getCycle() + "_" +
                                                        "recipe" + cycle.getRecipePosition()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
