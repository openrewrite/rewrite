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

import org.openrewrite.javascript.internal.lock.V8RegExp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of glob-parent 5.1.2 with is-glob 4.0.3 and is-extglob 2.1.1, as fast-glob 3.3.2 calls it.
 */
final class GlobParent {

    private static final Pattern ENCLOSURE = V8RegExp.of("[\\{\\[].*[\\}\\]]$");
    private static final Pattern GLOBBY = V8RegExp.of("(^|[^\\\\])([\\{\\[]|\\([^\\)]+$)");
    private static final Pattern ESCAPED = V8RegExp.of("\\\\([\\!\\*\\?\\|\\[\\]\\(\\)\\{\\}])");
    private static final Pattern EXTGLOB = V8RegExp.of("(\\\\).|([@?!+*]\\(.*\\))");

    private GlobParent() {
    }

    static String globParent(String str) {
        if (ENCLOSURE.matcher(str).find()) {
            str += "/";
        }
        str += "a";
        do {
            str = dirname(str);
        } while (isGlob(str) || GLOBBY.matcher(str).find());

        Matcher m = ESCAPED.matcher(str);
        StringBuilder out = new StringBuilder();
        int last = 0;
        while (m.find()) {
            out.append(str, last, m.start()).append(m.group(1));
            last = m.end();
        }
        return out.append(str.substring(last)).toString();
    }

    static boolean isGlob(String str) {
        if (str.isEmpty()) {
            return false;
        }
        if (isExtglob(str)) {
            return true;
        }
        return strictCheck(str);
    }

    private static boolean isExtglob(String str) {
        while (true) {
            Matcher match = EXTGLOB.matcher(str);
            if (!match.find()) {
                return false;
            }
            if (match.group(2) != null) {
                return true;
            }
            str = str.substring(match.end());
        }
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

    /**
     * Node's {@code path.posix.dirname}.
     */
    static String dirname(String path) {
        if (path.isEmpty()) {
            return ".";
        }
        boolean hasRoot = path.charAt(0) == '/';
        int end = -1;
        boolean matchedSlash = true;
        for (int i = path.length() - 1; i >= 1; --i) {
            if (path.charAt(i) == '/') {
                if (!matchedSlash) {
                    end = i;
                    break;
                }
            } else {
                matchedSlash = false;
            }
        }
        if (end == -1) {
            return hasRoot ? "/" : ".";
        }
        if (hasRoot && end == 1) {
            return "//";
        }
        return path.substring(0, end);
    }

    /**
     * Node's {@code path.posix.basename}.
     */
    static String basename(String path) {
        int start = 0;
        int end = -1;
        boolean matchedSlash = true;
        for (int i = path.length() - 1; i >= 0; --i) {
            if (path.charAt(i) == '/') {
                if (!matchedSlash) {
                    start = i + 1;
                    break;
                }
            } else if (end == -1) {
                matchedSlash = false;
                end = i + 1;
            }
        }
        return end == -1 ? "" : path.substring(start, end);
    }
}
