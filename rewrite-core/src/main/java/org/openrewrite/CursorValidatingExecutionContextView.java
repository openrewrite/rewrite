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

    public boolean getValidateCursorAcyclic() {
        return getMessage(VALIDATE_CURSOR_ACYCLIC, false);
    }

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
        boolean mutationAllowed = !getMessage(VALIDATE_CTX_MUTATION, false) || key.equals(VALIDATE_CURSOR_ACYCLIC) || key.equals(VALIDATE_CTX_MUTATION)
                                  || key.equals(ExecutionContext.CURRENT_CYCLE) || key.equals(ExecutionContext.CURRENT_RECIPE) || key.equals(ExecutionContext.DATA_TABLES)
                                  || key.startsWith("org.openrewrite.maven"); // MavenExecutionContextView stores metrics
        assert mutationAllowed
                : "Recipe mutated execution context key \"" + key + "\". " +
                  "Recipes should not mutate the contents of the ExecutionContext as it allows mutable state to leak between " +
                  "recipes, opening the door for difficult to debug recipe composition errors. " +
                  "If you need to store state within the execution of a single recipe use Cursor messaging. " +
                  "If you want to pass state between recipes, use a ScanningRecipe instead.";
        super.putMessage(key, value);
    }
}
