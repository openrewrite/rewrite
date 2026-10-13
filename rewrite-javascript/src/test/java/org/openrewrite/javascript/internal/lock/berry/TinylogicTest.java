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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * tinylogic 2.0.0's index.test.js, then structUtils' condition pattern over tokens true for os=darwin and cpu=arm64:
 * node v24.19.0's answers, its SyntaxError messages included.
 */
class TinylogicTest {

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"true;true", "false;false", "true | false;true", "true & false;false",
            "true ^ false;true", "false ^ false;false", "false & true | true;true", "!true;false", "!false;true",
            "!true | true;true", "!true & false;false"})
    void parse(String query, boolean result) {
        assertThat(Tinylogic.parse(query, Pattern.compile("[a-z]+"), "true"::equals)).isEqualTo(result);
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"os=darwin;true", "os=darwin & cpu=arm64;true", "os=linux | os=darwin;true",
            "!os=win32 & os=darwin;true", "(os=darwin | os=linux) & (cpu=x64 | cpu=arm64);true", "xos=darwin;false",
            "os=darwin & !cpu=x64;true", "os=darwin^cpu=arm64;false", "' os=darwin';true", "os=darwin&cpu=arm64;true",
            "os=darwin | os=linux & cpu=x64;false", "!!os=darwin;true"})
    void conditions(String condition, boolean result) {
        assertThat(conditions(condition)).isEqualTo(result);
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "'os=darwin ';Expected \"&\", \"^\", or \"|\" but end of input found.",
            "'';Expected \"!\", \"(\", or [^ \\t\\n\\r()!|&\\^] but end of input found.",
            "os=darwin x;Expected \"&\", \"^\", or \"|\" but \"x\" found.",
            "os=darwin &;Expected \"!\", \"(\", or [^ \\t\\n\\r()!|&\\^] but end of input found.",
            "os=Darwin;Expected [^ \\t\\n\\r()!|&\\^] but end of input found.",
            "(os=darwin;Expected \"&\", \")\", \"^\", \"|\", or [^ \\t\\n\\r()!|&\\^] but end of input found."})
    void rejects(String condition, String message) {
        assertThatThrownBy(() -> conditions(condition))
                .isInstanceOf(JsError.class)
                .hasToString("SyntaxError: " + message);
    }

    private static boolean conditions(String condition) {
        Pattern pattern = Pattern.compile("(os|cpu|libc)=([a-z0-9_-]+)");
        return Tinylogic.parse(condition, pattern, token -> "os=darwin".equals(token) || "cpu=arm64".equals(token));
    }
}
