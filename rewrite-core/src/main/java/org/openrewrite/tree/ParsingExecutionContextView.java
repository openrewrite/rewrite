/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.tree;

import org.jspecify.annotations.Nullable;
import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;

import java.nio.charset.Charset;

public class ParsingExecutionContextView extends DelegatingExecutionContext {
    private static final String PARSING_LISTENER = "org.openrewrite.core.parsingListener";

    private static final String CHARSET = "org.openrewrite.parser.charset";

    public ParsingExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static ParsingExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof ParsingExecutionContextView) {
            return (ParsingExecutionContextView) ctx;
        }
        return new ParsingExecutionContextView(ctx);
    }

    public ParsingExecutionContextView setParsingListener(ParsingEventListener listener) {
        putMessage(PARSING_LISTENER, listener);
        return this;
    }

    public ParsingEventListener getParsingListener() {
        return getMessage(PARSING_LISTENER, ParsingEventListener.NOOP);
    }

    public ParsingExecutionContextView setCharset(@Nullable Charset charset) {
        putMessage(CHARSET, charset);
        return this;
    }

    public @Nullable Charset getCharset() {
        return getMessage(CHARSET);
    }
}
