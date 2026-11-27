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
package org.openrewrite.json.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class JsonStringTest {

    @Test
    void quoteEscapesWhatStandardJsonRequires() {
        assertThat(JsonString.quote("\"\\\b\f\n\r\t\u0001é"))
          .isEqualTo("\"\\\"\\\\\\b\\f\\n\\r\\t\\u0001é\"");
    }

    @Test
    void decodeKeepsAnEscapeWithoutItsHexDigitsAsWritten() {
        assertThat(JsonString.decode("\"\\x4\\u12\"")).isEqualTo("\\x4\\u12");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
      lone high mid-string | a\\ud800b           | a\\ud800b
      lone high at end     | a\\ud800            | a\\ud800
      lone low             | \\udfaa             | \\udfaa
      two highs in a row   | \\ud83d\\ud83d\\ude00 | \\ud83d😀
      reversed pair        | \\ude00\\ud83d       | \\ude00\\ud83d
      upper case hex       | \\uD800             | \\ud800
      """)
    void decodeKeepsAnUnpairedSurrogateAsItsEscape(String description, String escapes, String decoded) {
        assertThat(JsonString.decode("\"" + escapes + "\"")).isEqualTo(decoded);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\\ud83d\\ude00", "\\uD83D\\uDE00", "\\ud83d\\\n\\ude00"})
    void decodeJoinsASurrogatePair(String escapes) {
        assertThat(JsonString.decode("\"" + escapes + "\"")).isEqualTo("😀");
    }
}
