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
package org.openrewrite.javascript.internal.lock.berry;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code querystring.escape} answers from node v24.19.0 ({@code null}: it throws URIError [ERR_INVALID_URI]).
 */
class QueryStringTest {

    static Stream<Arguments> escapes() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of("~!*()'", "~!*()'"),
                Arguments.of("a b&c=d", "a%20b%26c%3Dd"),
                Arguments.of("😀", "%F0%9F%98%80"),
                Arguments.of("\ud800@", "%F0%90%81%80"),
                Arguments.of("\udc00@x", "%F0%90%81%80x"),
                Arguments.of("\ude00\ud83d", "%F2%90%80%BD"),
                Arguments.of("p\ud800@workspace:.", "p%F0%90%81%80workspace%3A."),
                Arguments.of("\ud800", null),
                Arguments.of("\udc00", null),
                Arguments.of("x\ud83d", null));
    }

    @ParameterizedTest
    @MethodSource("escapes")
    void escapesAsNodeDoes(String input, @Nullable String escaped) {
        if (escaped == null) {
            assertThatThrownBy(() -> QueryString.escape(input))
                    .isInstanceOf(JsError.class)
                    .hasToString("URIError [ERR_INVALID_URI]: URI malformed");
        } else {
            assertThat(QueryString.escape(input)).isEqualTo(escaped);
        }
    }
}
