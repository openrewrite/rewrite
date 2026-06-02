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

import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.LargeSourceSet;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.SourceFile;

import java.util.Collection;
import java.util.List;

/**
 * Strategy used by {@link RewriteTest#rewriteRun} to execute a recipe over a set of
 * source files. The default {@link #IN_PROCESS} runner invokes
 * {@link Recipe#run(LargeSourceSet, ExecutionContext, int, int)} in the test JVM.
 * Alternative runners can stand up an out-of-process backend (e.g. the moderne-cli
 * via the {@code active.recipe} hook) and translate its output back into a
 * {@link RecipeRun}, letting the same test source exercise multiple execution paths.
 * <p>
 * Select a runner per-test via {@link RecipeSpec#runner(RewriteRunner)} or per-class
 * by overriding {@link RewriteTest#runner()}.
 */
public interface RewriteRunner {

    /**
     * A ready-to-use no-override instance for {@link RewriteTest#runner()} defaults.
     * Behaves as plain in-process execution via the default {@link #run} below.
     */
    RewriteRunner IN_PROCESS = new RewriteRunner() {};

    /**
     * Default in-JVM behavior. Out-of-process runners override this.
     */
    default RecipeRun run(Recipe recipe, Context context) {
        return recipe.run(context.getSources(), context.getExecutionContext(),
                context.getCycles(), context.getExpectedCyclesThatMakeChanges() + 1);
    }

    /**
     * Everything a runner might want from the surrounding {@link RewriteTest}
     * invocation. New runners can ignore most of this and just delegate to
     * {@link Recipe#run}; out-of-process runners need the source specs (to re-parse
     * mutated files using the same parsers) and the two recipe specs (to honor
     * test-level options like execution-context customizers).
     */
    @Value
    class Context {
        LargeSourceSet sources;

        /**
         * The parsed source files in their original (pre-recipe) form. Out-of-process
         * runners need these to stage the project on disk before invoking the backend.
         */
        List<SourceFile> parsedSources;

        ExecutionContext executionContext;
        int cycles;
        int expectedCyclesThatMakeChanges;
        Collection<SourceSpec<?>> sourceSpecs;
        RecipeSpec testClassSpec;
        RecipeSpec testMethodSpec;
    }
}
