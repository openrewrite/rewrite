/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.coldfusion.style;

import lombok.Value;
import lombok.With;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

/**
 * Where script breaks its lines: each statement on its own line, and the braces and keywords around blocks. Tags
 * keep the lines they are written on, since the line breaks between them can be part of a template's output.
 */
@Value
@With
public class WrappingAndBracesStyle implements Style {

    /**
     * Whether a block written on one line, as {@code if (x) { return; }}, stays on it.
     */
    Boolean keepSimpleBlocksInOneLine;

    Braces braces;
    KeywordsAfterBlock keywordsAfterBlock;

    /**
     * Whether a <code>{</code> starts a line of its own rather than ending the line before it.
     */
    @Value
    @With
    public static class Braces {
        /**
         * The body of a component.
         */
        Boolean componentOnNewLine;

        /**
         * The body of a function.
         */
        Boolean functionOnNewLine;

        /**
         * The body of {@code if}, {@code else}, a loop, {@code switch}, {@code try}, {@code catch}, {@code finally}
         * and a tag written as a statement, as {@code lock}.
         */
        Boolean statementOnNewLine;
    }

    /**
     * Whether a keyword that follows a block's <code>}</code> starts a line of its own.
     */
    @Value
    @With
    public static class KeywordsAfterBlock {
        Boolean elseOnNewLine;

        /**
         * {@code catch} and {@code finally}.
         */
        Boolean catchOnNewLine;

        /**
         * The {@code while} of {@code do {} while (x)}.
         */
        Boolean whileOnNewLine;
    }

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(CFFormat.wrappingAndBraces(), this);
    }
}
