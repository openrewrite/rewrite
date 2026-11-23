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
package org.openrewrite.javascript.internal.lock;

import java.util.regex.Pattern;

/**
 * Port of is-glob over its is-extglob: 2.0.1 with 1.0.0, which yarn 1.22.22 bundles for micromatch 2.3.11, and 4.0.3
 * with 2.1.1 under its default strict check, which glob-parent 5.1.2 calls in Yarn 4.10.3's fast-glob.
 */
public final class IsGlob {

    public enum Release {
        V2_0_1,
        V4_0_3
    }

    private static final Pattern GLOB_CHARS_2_0_1 = V8RegExp.of("[*!?{}(|)[\\]]");

    private IsGlob() {
    }

    public static boolean isGlob(String str, Release release) {
        if (release == Release.V2_0_1) {
            return GLOB_CHARS_2_0_1.matcher(str).find() || IsExtglob.isExtglob(str, IsExtglob.Release.V1_0_0);
        }
        if (str.isEmpty()) {
            return false;
        }
        if (IsExtglob.isExtglob(str, IsExtglob.Release.V2_1_1)) {
            return true;
        }
        return strictCheck(str);
    }

    private static boolean strictCheck(String str) {
        if (str.charAt(0) == '!') {
            return true;
        }
        int index = 0;
        int pipeIndex = -2;
        int closeSquareIndex = -2;
        int closeCurlyIndex = -2;
        int closeParenIndex = -2;
        int backSlashIndex = -2;
        while (index < str.length()) {
            char c = str.charAt(index);
            if (c == '*') {
                return true;
            }

            if (at(str, index + 1) == '?' && "].+)".indexOf(c) >= 0) {
                return true;
            }

            if (closeSquareIndex != -1 && c == '[' && at(str, index + 1) != ']') {
                if (closeSquareIndex < index) {
                    closeSquareIndex = str.indexOf(']', index);
                }
                if (closeSquareIndex > index) {
                    if (backSlashIndex == -1 || backSlashIndex > closeSquareIndex) {
                        return true;
                    }
                    backSlashIndex = str.indexOf('\\', index);
                    if (backSlashIndex == -1 || backSlashIndex > closeSquareIndex) {
                        return true;
                    }
                }
            }

            if (closeCurlyIndex != -1 && c == '{' && at(str, index + 1) != '}') {
                closeCurlyIndex = str.indexOf('}', index);
                if (closeCurlyIndex > index) {
                    backSlashIndex = str.indexOf('\\', index);
                    if (backSlashIndex == -1 || backSlashIndex > closeCurlyIndex) {
                        return true;
                    }
                }
            }

            if (closeParenIndex != -1 && c == '(' && at(str, index + 1) == '?' &&
                ":!=".indexOf(at(str, index + 2)) >= 0 && at(str, index + 3) != ')') {
                closeParenIndex = str.indexOf(')', index);
                if (closeParenIndex > index) {
                    backSlashIndex = str.indexOf('\\', index);
                    if (backSlashIndex == -1 || backSlashIndex > closeParenIndex) {
                        return true;
                    }
                }
            }

            if (pipeIndex != -1 && c == '(' && at(str, index + 1) != '|') {
                if (pipeIndex < index) {
                    pipeIndex = str.indexOf('|', index);
                }
                if (pipeIndex != -1 && at(str, pipeIndex + 1) != ')') {
                    closeParenIndex = str.indexOf(')', pipeIndex);
                    if (closeParenIndex > pipeIndex) {
                        backSlashIndex = str.indexOf('\\', pipeIndex);
                        if (backSlashIndex == -1 || backSlashIndex > closeParenIndex) {
                            return true;
                        }
                    }
                }
            }

            if (c == '\\') {
                int open = at(str, index + 1);
                index += 2;
                int close = open == '{' ? '}' : open == '(' ? ')' : open == '[' ? ']' : -1;
                if (close != -1) {
                    int n = str.indexOf(close, index);
                    if (n != -1) {
                        index = n + 1;
                    }
                }
                if (at(str, index) == '!') {
                    return true;
                }
            } else {
                index++;
            }
        }
        return false;
    }

    private static int at(String str, int index) {
        return index < str.length() ? str.charAt(index) : -1;
    }
}
