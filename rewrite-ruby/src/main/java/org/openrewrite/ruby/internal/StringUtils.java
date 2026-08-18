/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.ruby.internal;

public final class StringUtils {

    private StringUtils() {
    }

    public static String endSymbol(String beginDelimiter) {
        if (beginDelimiter.equals(":")) {
            return "";
        } else if (beginDelimiter.startsWith(":")) {
            return endDelimiter(beginDelimiter.substring(1));
        }
        return endDelimiter(beginDelimiter);
    }

    public static String endDelimiter(String beginDelimiter) {
        if (beginDelimiter.startsWith("%")) {
            char t = beginDelimiter.charAt(1);
            if (t == 'q' || t == 'Q' || t == 's' || t == 'i' || t == 'I' ||
                t == 'w' || t == 'W' || t == 'r' || t == 'x') {
                return matchBeginDelimiter(beginDelimiter.charAt(2), beginDelimiter.substring(2));
            }
            // for the %[foo bar baz] case
            return matchBeginDelimiter(beginDelimiter.charAt(1), beginDelimiter.substring(1));
        }
        if (beginDelimiter.equals("?")) {
            return ""; // the character literal case and the bare symbol case
        }
        if (beginDelimiter.length() == 1) {
            return matchBeginDelimiter(beginDelimiter.charAt(0), beginDelimiter);
        }
        return beginDelimiter;
    }

    private static String matchBeginDelimiter(char c, String orElse) {
        switch (c) {
            case '[':
                return "]";
            case '(':
                return ")";
            case '{':
                return "}";
            case '<':
                return ">";
            default:
                return orElse;
        }
    }
}
