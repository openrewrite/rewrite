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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.yaml.internal.StringUtils.quoteIfNeeded;

class StringUtilsTest {

    @ParameterizedTest
    @MethodSource
    void alreadyQuotedPassthrough(String input, String expected) {
        assertThat(quoteIfNeeded(input)).isEqualTo(expected);
    }

    static Stream<Arguments> alreadyQuotedPassthrough() {
        return Stream.of(
                Arguments.of("'hello'", "'hello'"),
                Arguments.of("\"hello\"", "\"hello\""),
                Arguments.of("''", "''")
        );
    }

    @ParameterizedTest
    @MethodSource
    void noQuotingNeeded(String input, String expected) {
        assertThat(quoteIfNeeded(input)).isEqualTo(expected);
    }

    static Stream<Arguments> noQuotingNeeded() {
        return Stream.of(
                Arguments.of("hello", "hello"),
                Arguments.of("hello world", "hello world"),
                Arguments.of("some-key", "some-key"),
                Arguments.of("path/to/file", "path/to/file"),
                Arguments.of("1.0.0", "1.0.0"),
                Arguments.of("truefalse", "truefalse"),
                Arguments.of("https://example.com", "https://example.com")
        );
    }

    @Test
    void emptyStringQuoted() {
        assertThat(quoteIfNeeded("")).isEqualTo("''");
    }

    @ParameterizedTest
    @MethodSource
    void validYamlTypedValuesUnchanged(String input, String expected) {
        assertThat(quoteIfNeeded(input)).isEqualTo(expected);
    }

    static Stream<Arguments> validYamlTypedValuesUnchanged() {
        return Stream.of(
                Arguments.of("null", "null"),
                Arguments.of("true", "true"),
                Arguments.of("false", "false"),
                Arguments.of("YES", "YES"),
                Arguments.of("123", "123"),
                Arguments.of("1.23", "1.23")
        );
    }

    @ParameterizedTest
    @MethodSource
    void indicatorCharactersQuoted(String input, String expected) {
        assertThat(quoteIfNeeded(input)).isEqualTo(expected);
    }

    static Stream<Arguments> indicatorCharactersQuoted() {
        return Stream.of(
                Arguments.of("~", "~"),
                Arguments.of("- item", "'- item'"),
                Arguments.of("-42", "'-42'"),
                Arguments.of("# comment", "'# comment'"),
                Arguments.of("*alias", "'*alias'"),
                Arguments.of("&anchor", "'&anchor'"),
                Arguments.of("[list]", "'[list]'"),
                Arguments.of("{map}", "'{map}'"),
                Arguments.of("!tag", "'!tag'"),
                Arguments.of("%directive", "'%directive'")
        );
    }

    @ParameterizedTest
    @MethodSource
    void dangerousMidStringPatternsQuoted(String input, String expected) {
        assertThat(quoteIfNeeded(input)).isEqualTo(expected);
    }

    static Stream<Arguments> dangerousMidStringPatternsQuoted() {
        return Stream.of(
                Arguments.of("key: value", "'key: value'"),
                Arguments.of("TODO: Follow this link https://example.com/page",
                        "'TODO: Follow this link https://example.com/page'"),
                Arguments.of("before # after", "'before # after'")
        );
    }

    @ParameterizedTest
    @MethodSource
    void documentMarkersQuoted(String input, String expected) {
        assertThat(quoteIfNeeded(input)).isEqualTo(expected);
    }

    static Stream<Arguments> documentMarkersQuoted() {
        return Stream.of(
                Arguments.of("---", "'---'"),
                Arguments.of("...", "'...'")
        );
    }

    @ParameterizedTest
    @MethodSource
    void leadingTrailingWhitespaceQuoted(String input, String expected) {
        assertThat(quoteIfNeeded(input)).isEqualTo(expected);
    }

    static Stream<Arguments> leadingTrailingWhitespaceQuoted() {
        return Stream.of(
                Arguments.of(" hello", "' hello'"),
                Arguments.of("hello ", "'hello '"),
                Arguments.of("\thello", "\"\\thello\"")
        );
    }

    @ParameterizedTest
    @MethodSource
    void doubleQuoteCases(String input, String expected) {
        assertThat(quoteIfNeeded(input)).isEqualTo(expected);
    }

    static Stream<Arguments> doubleQuoteCases() {
        return Stream.of(
                // Contains ': ' (needs quoting) AND single quote (forces double quotes)
                Arguments.of("it's: a test", "\"it's: a test\""),
                Arguments.of("line1\nline2", "\"line1\\nline2\""),
                Arguments.of("col1\tcol2", "\"col1\\tcol2\""),
                Arguments.of("it's", "it's"),
                Arguments.of("line1\rline2", "\"line1\\rline2\""),
                Arguments.of("abc\0def", "\"abc\\0def\""),
                Arguments.of("path\\to", "path\\to"),
                Arguments.of("say \"hello\"", "say \"hello\"")
        );
    }
}
