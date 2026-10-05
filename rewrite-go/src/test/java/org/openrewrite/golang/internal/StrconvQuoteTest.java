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
package org.openrewrite.golang.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StrconvQuoteTest {

    private static String codePoints(int... codePoints) {
        return new String(codePoints, 0, codePoints.length);
    }

    @Test
    void escapesWhatGoDoesNotPrint() {
        assertThat(StrconvQuote.quote("json:\"a\" back\\slash")).isEqualTo("\"json:\\\"a\\\" back\\\\slash\"");
        assertThat(StrconvQuote.quote(codePoints(0x07, '\b', '\f', '\n', '\r', '\t', 0x0b, 0x00, 0x7f)))
                .isEqualTo("\"\\a\\b\\f\\n\\r\\t\\v\\x00\\x7f\"");
        // no-break space, soft hyphen, line separator, private use
        assertThat(StrconvQuote.quote(codePoints(0xa0, 0xad, 0x2028, 0xe000)))
                .isEqualTo("\"\\u00a0\\u00ad\\u2028\\ue000\"");
        assertThat(StrconvQuote.quote(codePoints(0xe0001, 0x10ffff))).isEqualTo("\"\\U000e0001\\U0010ffff\"");
    }

    @Test
    void leavesWhatGoPrints() {
        String printable = codePoints(' ', 0xe9, 0x436, 0x8a9e, 0xfffd, 0x1f600);
        assertThat(StrconvQuote.quote(printable)).isEqualTo('"' + printable + '"');
    }

    /**
     * Unicode 16 made U+1C89 a letter, which a JVM built on it reports and the ranges of Unicode 15 do not.
     */
    @Test
    void doesNotAskTheJvmWhatIsPrintable() {
        assertThat(StrconvQuote.quote(codePoints(0x1c89))).isEqualTo("\"\\u1c89\"");
    }
}
