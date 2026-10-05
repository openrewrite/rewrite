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
package org.openrewrite.javascript.internal.lock.npm;

/**
 * {@code Intl.Collator('en').compare}, which npm sorts with through @isaacs/string-locale-compare, for
 * ASCII strings: whitespace, then punctuation, then digits, then letters, with case only breaking ties.
 */
final class LocaleCompare {

    private static final String PRIMARY_ORDER =
            "\t\n\u000B\f\r _-,;:!?.'\"()[]{}@*/\\&#%`^+<=>|~$0123456789abcdefghijklmnopqrstuvwxyz";

    // 0 for the control characters collation ignores
    private static final int[] PRIMARY = new int[128];

    static {
        for (int i = 0; i < PRIMARY_ORDER.length(); i++) {
            char c = PRIMARY_ORDER.charAt(i);
            PRIMARY[c] = i + 1;
            PRIMARY[Character.toUpperCase(c)] = i + 1;
        }
    }

    private LocaleCompare() {
    }

    /**
     * @throws IllegalArgumentException for a character outside ASCII, whose order needs the CLDR tables.
     */
    static int en(String a, String b) {
        int primary = compare(a, b, false);
        return primary != 0 ? primary : compare(a, b, true);
    }

    private static int compare(String a, String b, boolean tertiary) {
        int i = 0;
        int j = 0;
        while (true) {
            i = skipIgnorable(a, i);
            j = skipIgnorable(b, j);
            if (i == a.length() || j == b.length()) {
                return i < a.length() ? 1 : j < b.length() ? -1 : 0;
            }
            char ca = a.charAt(i++);
            char cb = b.charAt(j++);
            int d = tertiary ?
                    Boolean.compare(Character.isUpperCase(ca), Character.isUpperCase(cb)) :
                    Integer.compare(PRIMARY[ca], PRIMARY[cb]);
            if (d != 0) {
                return d;
            }
        }
    }

    private static int skipIgnorable(String s, int i) {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c >= 128) {
                throw new IllegalArgumentException("Cannot collate the non-ASCII character in \"" + s + "\"");
            }
            if (PRIMARY[c] != 0) {
                break;
            }
            i++;
        }
        return i;
    }
}
