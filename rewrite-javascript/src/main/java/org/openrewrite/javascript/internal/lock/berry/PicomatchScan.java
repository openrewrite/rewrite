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

import java.util.ArrayList;
import java.util.List;

/**
 * Port of picomatch 2.3.1 lib/scan.js for the {@code parts} fast-glob 3.3.2 reads ({@code parts: true}).
 */
final class PicomatchScan {

    private static final int NAN = -1;

    private final String str;
    private int index = -1;
    private int prev = NAN;
    private int code = NAN;

    private PicomatchScan(String input) {
        this.str = input;
    }

    static List<String> parts(String input) {
        return new PicomatchScan(input).scan();
    }

    private List<String> scan() {
        int length = str.length() - 1;
        List<Integer> slashes = new ArrayList<>();
        int start = 0;
        boolean isGlob = false;
        boolean braceEscaped = false;
        boolean finished = false;
        int braces = 0;

        while (index < length) {
            code = advance();

            if (code == '\\') {
                code = advance();
                if (code == '{') {
                    braceEscaped = true;
                }
                continue;
            }

            if (braceEscaped || code == '{') {
                braces++;
                while (index < length && (code = advance()) > 0) {
                    if (code == '\\') {
                        advance();
                        continue;
                    }
                    if (code == '{') {
                        braces++;
                        continue;
                    }
                    if (!braceEscaped && code == '.' && (code = advance()) == '.') {
                        isGlob = true;
                        finished = true;
                        continue;
                    }
                    if (!braceEscaped && code == ',') {
                        isGlob = true;
                        finished = true;
                        continue;
                    }
                    if (code == '}') {
                        braces--;
                        if (braces == 0) {
                            braceEscaped = false;
                            finished = true;
                            break;
                        }
                    }
                }
                continue;
            }

            if (code == '/') {
                slashes.add(index);
                if (finished) {
                    continue;
                }
                if (prev == '.' && index == start + 1) {
                    start += 2;
                }
                continue;
            }

            boolean isExtglobChar = code == '+' || code == '@' || code == '*' || code == '?' || code == '!';
            if (isExtglobChar && peek() == '(') {
                isGlob = true;
                finished = true;
                while (index < length && (code = advance()) > 0) {
                    if (code == '\\') {
                        code = advance();
                        continue;
                    }
                    if (code == ')') {
                        break;
                    }
                }
                continue;
            }

            if (code == '*' || code == '?') {
                isGlob = true;
                finished = true;
                continue;
            }

            if (code == '[') {
                int next;
                while (index < length && (next = advance()) > 0) {
                    if (next == '\\') {
                        advance();
                        continue;
                    }
                    if (next == ']') {
                        isGlob = true;
                        finished = true;
                        break;
                    }
                }
                continue;
            }

            if (code == '!' && index == start) {
                start++;
                continue;
            }

            if (code == '(') {
                isGlob = true;
                while (index < length && (code = advance()) > 0) {
                    if (code == '(') {
                        code = advance();
                        continue;
                    }
                    if (code == ')') {
                        finished = true;
                        break;
                    }
                }
                continue;
            }

            if (isGlob) {
                finished = true;
            }
        }

        List<String> parts = new ArrayList<>();
        int prevIndex = 0;
        for (int idx = 0; idx < slashes.size(); idx++) {
            int n = prevIndex != 0 ? prevIndex + 1 : start;
            int i = slashes.get(idx);
            String value = n < i ? str.substring(n, i) : "";
            if (idx != 0 || !value.isEmpty()) {
                parts.add(value);
            }
            prevIndex = i;
        }
        if (prevIndex != 0 && prevIndex + 1 < str.length()) {
            parts.add(str.substring(prevIndex + 1));
        }
        return parts;
    }

    private int peek() {
        return index + 1 < str.length() ? str.charAt(index + 1) : NAN;
    }

    private int advance() {
        prev = code;
        index++;
        return index < str.length() ? str.charAt(index) : NAN;
    }
}
