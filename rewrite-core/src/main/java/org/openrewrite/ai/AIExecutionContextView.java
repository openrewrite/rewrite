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
package org.openrewrite.ai;

import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;

import static java.util.Objects.requireNonNull;

public class AIExecutionContextView extends DelegatingExecutionContext {
    private static final String OPENAPI_TOKEN = "org.openrewrite.ai.openapi.token";

    public AIExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static AIExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof AIExecutionContextView) {
            return (AIExecutionContextView) ctx;
        }
        return new AIExecutionContextView(ctx);
    }

    public AIExecutionContextView setOpenApiToken(String token) {
        putMessage(OPENAPI_TOKEN, token);
        return this;
    }

    public String getOpenapiToken() {
        return requireNonNull(getMessage(OPENAPI_TOKEN));
    }
}
