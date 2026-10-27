/*
 * Copyright 2024 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.openrewrite.scheduling.WorkingDirectoryExecutionContextView;

import static org.openrewrite.Recipe.PANIC;

public class CursorValidatingExecutionContextView extends DelegatingExecutionContext {
    private static final String VALIDATE_CURSOR_ACYCLIC = "org.openrewrite.CursorValidatingExecutionContextView.ValidateCursorAcyclic";
    private static final String VALIDATE_CTX_MUTATION = "org.openrewrite.CursorValidatingExecutionContextView.ValidateExecutionContextImmutability";

    public CursorValidatingExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static CursorValidatingExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof CursorValidatingExecutionContextView) {
            return (CursorValidatingExecutionContextView) ctx;
        }
        return new CursorValidatingExecutionContextView(ctx);
    }

    /**
     * @deprecated A cursor whose parent points to the same element fails an assertion in tests, so this has no effect.
     */
    @Deprecated
    public boolean getValidateCursorAcyclic() {
        return getMessage(VALIDATE_CURSOR_ACYCLIC, false);
    }

    /**
     * @deprecated A cursor whose parent points to the same element fails an assertion in tests, so this has no effect.
     */
    @Deprecated
    @SuppressWarnings("UnusedReturnValue")
    public CursorValidatingExecutionContextView setValidateCursorAcyclic(boolean validateCursorAcyclic) {
        putMessage(VALIDATE_CURSOR_ACYCLIC, validateCursorAcyclic);
        return this;
    }

    public CursorValidatingExecutionContextView setValidateImmutableExecutionContext(boolean allowExecutionContextMutation) {
        putMessage(VALIDATE_CTX_MUTATION, allowExecutionContextMutation);
        return this;
    }

    @Override
    public void putMessage(String key, @Nullable Object value) {
        boolean mutationAllowed =
                !getMessage(VALIDATE_CTX_MUTATION, false) ||
                VALIDATE_CURSOR_ACYCLIC.equals(key) ||
                VALIDATE_CTX_MUTATION.equals(key) ||
                PANIC.equals(key) ||
                ExecutionContext.CURRENT_CYCLE.equals(key) ||
                ExecutionContext.CURRENT_RECIPE.equals(key) ||
                DataTableExecutionContextView.DATA_TABLE_STORE.equals(key) ||
                WorkingDirectoryExecutionContextView.WORKING_DIRECTORY_ROOT.equals(key) ||
                ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT.equals(key) ||
                key.startsWith("org.openrewrite.maven") // MavenExecutionContextView stores metrics
                || key.startsWith("io.moderne"); // We ought to know what we're doing
        assert mutationAllowed : mutationMessage(key);
        super.putMessage(key, value);
    }

    private String mutationMessage(String key) {
        Recipe recipe = getMessage(ExecutionContext.CURRENT_RECIPE);
        return (recipe == null ? "A recipe" : recipe.getName()) + " put \"" + key + "\" on the ExecutionContext, " +
               "which every recipe and source file in a run shares, so what it keeps there leaks into the others. " +
               "Keep state that spans source files in a ScanningRecipe's accumulator, and state within one source " +
               "file in Cursor messages, with getCursor().putMessage(..) or putMessageOnFirstEnclosing(..). A recipe " +
               "that must change the ExecutionContext opts out with " +
               "RecipeQualityValidation.builder().executionContextUnchanged(false).";
    }
}
