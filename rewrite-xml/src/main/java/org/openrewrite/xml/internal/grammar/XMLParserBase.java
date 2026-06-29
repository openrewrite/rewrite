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
package org.openrewrite.xml.internal.grammar;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Hand-written superclass for the generated {@link XMLParser} (wired via the {@code superClass} grammar option).
 * Holds the state needed by the void-element semantic predicate in {@code XMLParser.g4} so that the grammar
 * itself stays free of target-specific (Java) members and can also be generated for other targets.
 */
public abstract class XMLParserBase extends Parser {

    /**
     * When {@code true}, HTML <a href="https://developer.mozilla.org/en-US/docs/Glossary/Void_element">void
     * elements</a> (e.g. {@code <br>}, {@code <img>}) may be written without a self-closing slash. Set by the
     * parser driver for HTML-like sources (.jsp, .html, ...). Defaults to {@code false} so that strict XML
     * parsing is unaffected.
     */
    public boolean htmlMode = false;

    private static final Set<String> VOID_ELEMENTS = new HashSet<>(Arrays.asList(
            "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr"));

    protected XMLParserBase(TokenStream input) {
        super(input);
    }

    public boolean isVoidElement(@Nullable String name) {
        return htmlMode && name != null && VOID_ELEMENTS.contains(name.toLowerCase());
    }
}
