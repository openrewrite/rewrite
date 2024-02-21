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

import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.PathUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ParsingExecutionContextView extends DelegatingExecutionContext {
    private static final String PARSING_LISTENER = "org.openrewrite.core.parsingListener";
    private static final String INCLUDE_PATTERNS = "org.openrewrite.parser.%s.includePatterns";
    private static final String EXCLUDE_PATTERNS = "org.openrewrite.parser.%s.excludePatterns";

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

    @Nullable
    public Charset getCharset() {
        return getMessage(CHARSET);
    }

    public <T extends Parser> ParsingExecutionContextView includePattern(T parser, String filePattern) {
        putMessageInSet(String.format(INCLUDE_PATTERNS, parser.getDslName()), filePattern);
        return this;
    }

    public <T extends Parser> Set<String> getIncludePattern(T parser) {
        return getPatternsFromSystemProperty(String.format(INCLUDE_PATTERNS, parser.getDslName()));
    }

    public <T extends Parser> ParsingExecutionContextView setIncludePattern(T parser, @Nullable Set<String> filePattern) {
        putMessage(String.format(INCLUDE_PATTERNS, parser.getDslName()), filePattern);
        return this;
    }

    public <T extends Parser> ParsingExecutionContextView excludePattern(T parser, String filePattern) {
        putMessageInSet(String.format(EXCLUDE_PATTERNS, parser.getDslName()), filePattern);
        return this;
    }

    public <T extends Parser> Set<String> getExcludePattern(T parser) {
        return getPatternsFromSystemProperty(String.format(EXCLUDE_PATTERNS, parser.getDslName()));
    }

    public <T extends Parser> ParsingExecutionContextView setExcludePattern(T parser, @Nullable Set<String> filePattern) {
        putMessage(String.format(EXCLUDE_PATTERNS, parser.getDslName()), filePattern);
        return this;
    }

    public <T extends Parser> boolean accepts(T parser, Path path) {
        for (String pattern : getExcludePattern(parser)) {
            if (PathUtils.matchesGlob(path, pattern)) {
                return false;
            }
        }

        return parser.accept(path) || getIncludePattern(parser).stream().anyMatch(pattern -> PathUtils.matchesGlob(path, pattern));
    }

    private Set<String> getPatternsFromSystemProperty(String key) {
        Set<String> included = getMessage(key, Collections.emptySet());
        if (included == Collections.EMPTY_SET) {
            String property = System.getProperty(key, null);
            if (StringUtils.isNotEmpty(property)) {
                included = new HashSet<>(Arrays.asList(property.split(",")));
                putMessage(key, included);
            }
        }
        return included;
    }
}
