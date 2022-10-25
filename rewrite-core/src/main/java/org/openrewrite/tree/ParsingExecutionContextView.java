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

import org.openrewrite.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.quark.Quark;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.openrewrite.internal.lang.Nullable;

import java.nio.charset.Charset;

public class ParsingExecutionContextView extends DelegatingExecutionContext {
    private static final String PARSING_LISTENER = "org.openrewrite.core.parsingListener";

    private static final String CHARSET = "org.openrewrite.parser.charset";

    private static final String PARSING_FAILURES = "org.openrewrite.core.parsingFailures";

    public ParsingExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static ParsingExecutionContextView view(ExecutionContext ctx) {
        if(ctx instanceof ParsingExecutionContextView) {
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

    @SuppressWarnings("UnusedReturnValue")
    public ParsingExecutionContextView parseFailure(Path path, Parser<?> parser, Throwable t) {
        if(path.isAbsolute()) {
            throw new RuntimeException("Relative paths only");
        }
        putMessageInCollection(PARSING_FAILURES,
                new Quark(Tree.randomId(), path, Markers.EMPTY.addIfAbsent(ParseExceptionResult.build(parser, t)), null, null),
                ArrayList::new);
        return this;
    }

    @SuppressWarnings("unused")
    public List<Quark> getParseFailures() {
        return getMessage(PARSING_FAILURES, Collections.emptyList());
    }

    @SuppressWarnings("unused")
    public List<Quark> pollParseFailures() {
        return pollMessage(PARSING_FAILURES, Collections.emptyList());
    }

    public ParsingExecutionContextView setCharset(@Nullable Charset charset) {
        putMessage(CHARSET, charset);
        return this;
    }

    @Nullable
    public Charset getCharset() {
        return getMessage(CHARSET);
    }
}
