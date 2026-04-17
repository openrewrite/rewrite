/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.yaml.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.yaml.internal.StringUtils.quoteIfNeeded;

class StringUtilsTest {

    // Already-quoted passthrough

    @Test
    void singleQuotedPassthrough() {
        assertThat(quoteIfNeeded("'hello'")).isEqualTo("'hello'");
    }

    @Test
    void doubleQuotedPassthrough() {
        assertThat(quoteIfNeeded("\"hello\"")).isEqualTo("\"hello\"");
    }

    @Test
    void emptyQuotedPassthrough() {
        assertThat(quoteIfNeeded("''")).isEqualTo("''");
    }

    // No quoting needed

    @Test
    void plainStringUnchanged() {
        assertThat(quoteIfNeeded("hello")).isEqualTo("hello");
    }

    @Test
    void plainStringWithSpacesUnchanged() {
        assertThat(quoteIfNeeded("hello world")).isEqualTo("hello world");
    }

    @Test
    void plainStringWithHyphensUnchanged() {
        assertThat(quoteIfNeeded("some-key")).isEqualTo("some-key");
    }

    @Test
    void pathUnchanged() {
        assertThat(quoteIfNeeded("path/to/file")).isEqualTo("path/to/file");
    }

    @Test
    void versionLikeStringUnchanged() {
        assertThat(quoteIfNeeded("1.0.0")).isEqualTo("1.0.0");
    }

    @Test
    void nonReservedWordUnchanged() {
        assertThat(quoteIfNeeded("truefalse")).isEqualTo("truefalse");
    }

    @Test
    void urlUnchanged() {
        assertThat(quoteIfNeeded("https://example.com")).isEqualTo("https://example.com");
    }

    // Empty string

    @Test
    void emptyStringQuoted() {
        assertThat(quoteIfNeeded("")).isEqualTo("''");
    }

    // Valid YAML typed values left unchanged

    @Test
    void nullUnchanged() {
        assertThat(quoteIfNeeded("null")).isEqualTo("null");
    }

    @Test
    void trueUnchanged() {
        assertThat(quoteIfNeeded("true")).isEqualTo("true");
    }

    @Test
    void falseUnchanged() {
        assertThat(quoteIfNeeded("false")).isEqualTo("false");
    }

    @Test
    void yesUnchanged() {
        assertThat(quoteIfNeeded("YES")).isEqualTo("YES");
    }

    @Test
    void integerUnchanged() {
        assertThat(quoteIfNeeded("123")).isEqualTo("123");
    }

    @Test
    void floatUnchanged() {
        assertThat(quoteIfNeeded("1.23")).isEqualTo("1.23");
    }

    // Indicator characters at start

    @Test
    void tildeUnchanged() {
        assertThat(quoteIfNeeded("~")).isEqualTo("~");
    }

    @Test
    void dashSpaceQuoted() {
        assertThat(quoteIfNeeded("- item")).isEqualTo("'- item'");
    }

    @Test
    void negativeNumberQuotedAsIndicator() {
        assertThat(quoteIfNeeded("-42")).isEqualTo("'-42'");
    }

    @Test
    void hashQuoted() {
        assertThat(quoteIfNeeded("# comment")).isEqualTo("'# comment'");
    }

    @Test
    void asteriskQuoted() {
        assertThat(quoteIfNeeded("*alias")).isEqualTo("'*alias'");
    }

    @Test
    void ampersandQuoted() {
        assertThat(quoteIfNeeded("&anchor")).isEqualTo("'&anchor'");
    }

    @Test
    void bracketQuoted() {
        assertThat(quoteIfNeeded("[list]")).isEqualTo("'[list]'");
    }

    @Test
    void braceQuoted() {
        assertThat(quoteIfNeeded("{map}")).isEqualTo("'{map}'");
    }

    @Test
    void exclamationQuoted() {
        assertThat(quoteIfNeeded("!tag")).isEqualTo("'!tag'");
    }

    @Test
    void percentQuoted() {
        assertThat(quoteIfNeeded("%directive")).isEqualTo("'%directive'");
    }

    // Dangerous mid-string patterns

    @Test
    void colonSpaceQuoted() {
        assertThat(quoteIfNeeded("key: value")).isEqualTo("'key: value'");
    }

    @Test
    void colonSpaceInTextWithUrlQuoted() {
        assertThat(quoteIfNeeded("TODO: Follow this link https://example.com/page"))
                .isEqualTo("'TODO: Follow this link https://example.com/page'");
    }

    @Test
    void spaceHashQuoted() {
        assertThat(quoteIfNeeded("before # after")).isEqualTo("'before # after'");
    }

    // Document markers

    @Test
    void documentStartMarkerQuoted() {
        assertThat(quoteIfNeeded("---")).isEqualTo("'---'");
    }

    @Test
    void documentEndMarkerQuoted() {
        assertThat(quoteIfNeeded("...")).isEqualTo("'...'");
    }

    // Leading/trailing whitespace

    @Test
    void leadingSpaceQuoted() {
        assertThat(quoteIfNeeded(" hello")).isEqualTo("' hello'");
    }

    @Test
    void trailingSpaceQuoted() {
        assertThat(quoteIfNeeded("hello ")).isEqualTo("'hello '");
    }

    @Test
    void leadingTabQuoted() {
        assertThat(quoteIfNeeded("\thello")).isEqualTo("\"\\thello\"");
    }

    // Double-quote cases

    @Test
    void singleQuoteInValueUsesDoubleQuotes() {
        // Contains ': ' (needs quoting) AND single quote (forces double quotes)
        assertThat(quoteIfNeeded("it's: a test")).isEqualTo("\"it's: a test\"");
    }

    @Test
    void newlineUsesDoubleQuotes() {
        assertThat(quoteIfNeeded("line1\nline2")).isEqualTo("\"line1\\nline2\"");
    }

    @Test
    void tabUsesDoubleQuotes() {
        assertThat(quoteIfNeeded("col1\tcol2")).isEqualTo("\"col1\\tcol2\"");
    }

    @Test
    void plainStringWithSingleQuoteMidStringUnchanged() {
        assertThat(quoteIfNeeded("it's")).isEqualTo("it's");
    }

    @Test
    void carriageReturnUsesDoubleQuotes() {
        assertThat(quoteIfNeeded("line1\rline2")).isEqualTo("\"line1\\rline2\"");
    }

    @Test
    void nullCharUsesDoubleQuotes() {
        assertThat(quoteIfNeeded("abc\0def")).isEqualTo("\"abc\\0def\"");
    }

    @Test
    void backslashInDoubleQuotesEscaped() {
        assertThat(quoteIfNeeded("path\\to")).isEqualTo("path\\to");
    }

    @Test
    void doubleQuoteCharInValueEscaped() {
        assertThat(quoteIfNeeded("say \"hello\"")).isEqualTo("say \"hello\"");
    }
}
