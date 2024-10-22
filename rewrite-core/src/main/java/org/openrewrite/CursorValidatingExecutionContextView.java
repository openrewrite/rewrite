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

public class CursorValidatingExecutionContextView extends DelegatingExecutionContext {
    private static final String VALIDATE_CURSOR_ACYCLIC = "org.openrewrite.CursorValidatingExecutionContextView.ValidateCursorAcyclic";

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
}
