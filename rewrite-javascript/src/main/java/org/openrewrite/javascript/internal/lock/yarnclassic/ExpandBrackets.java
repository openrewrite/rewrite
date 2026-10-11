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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Port of expand-brackets 0.1.5 with is-posix-bracket 0.1.1, as yarn 1.22.22 bundles them for micromatch 2.3.11.
 */
final class ExpandBrackets {

    private static final Map<String, String> POSIX = new HashMap<>();

    static {
        POSIX.put("alnum", "a-zA-Z0-9");
        POSIX.put("alpha", "a-zA-Z");
        POSIX.put("blank", " \\t");
        POSIX.put("cntrl", "\\x00-\\x1F\\x7F");
        POSIX.put("digit", "0-9");
        POSIX.put("graph", "\\x21-\\x7E");
        POSIX.put("lower", "a-z");
        POSIX.put("print", "\\x20-\\x7E");
        POSIX.put("punct", "-!\"#$%&'()\\*+,./:;<=>?@[\\]^_`{|}~");
        POSIX.put("space", " \\t\\r\\n\\v\\f");
        POSIX.put("upper", "A-Z");
        POSIX.put("word", "A-Za-z0-9_");
        POSIX.put("xdigit", "A-Fa-f0-9");
    }

    private static final Pattern IS_POSIX_BRACKET = Pattern.compile("\\[([:.=+])(?:[^\\[\\]]|)+\\1]");
    private static final Pattern PARTS = Pattern.compile("(?::]\\[:|\\[?\\[:|:]]?)");
    private static final Pattern RANGE = Pattern.compile("^\\[?\\w-\\w]?\\z");
    private static final Pattern EQUIVALENCE = Pattern.compile("\\[+=|=]+");

    private ExpandBrackets() {
    }

    static String brackets(String str) {
        if (!IS_POSIX_BRACKET.matcher(str).find()) {
            return str;
        }

        boolean negated = false;
        if (str.contains("[^")) {
            negated = true;
            str = Js.splitJoin(str, "[^", "[");
        }
        if (str.contains("[!")) {
            negated = true;
            str = Js.splitJoin(str, "[!", "[");
        }

        boolean imbalanced = Js.splitString(str, "[").length != Js.splitString(str, "]").length;

        String[] parts = PARTS.split(str, -1);
        String end = "";
        String beg = "";
        List<String> res = new ArrayList<>();

        for (int i = 1; i <= parts.length; i++) {
            String inner = parts[i - 1];
            if ("^[!".equals(inner) || "[!".equals(inner)) {
                inner = "";
                negated = true;
            }

            String prefix = negated ? "^" : "";
            String ch = posix(inner);

            if (ch != null) {
                res.add("[" + prefix + ch + "]");
            } else if (!inner.isEmpty()) {
                if (RANGE.matcher(inner).find()) {
                    if (i == parts.length) {
                        res.add("[" + prefix + inner);
                    } else if (i == 1) {
                        res.add(prefix + inner + "]");
                    } else {
                        res.add(prefix + inner);
                    }
                } else {
                    if (i == 1) {
                        beg += inner;
                    } else if (i == parts.length) {
                        end += inner;
                    } else {
                        res.add("[" + prefix + inner + "]");
                    }
                }
            }
        }

        String result = String.join("|", res);
        int rlen = res.isEmpty() ? 1 : res.size();
        if (rlen > 1) {
            result = "(?:" + result + ")";
            rlen = 1;
        }
        if (!beg.isEmpty()) {
            rlen++;
            if (beg.charAt(0) == '[') {
                if (imbalanced) {
                    beg = "\\[" + beg.substring(1);
                } else {
                    beg += "]";
                }
            }
            result = beg + result;
        }
        if (!end.isEmpty()) {
            rlen++;
            if (end.endsWith("]")) {
                if (imbalanced) {
                    end = end.substring(0, end.length() - 1) + "\\]";
                } else {
                    end = "[" + end;
                }
            }
            result += end;
        }

        if (rlen > 1) {
            result = Js.splitJoin(result, "][", "]|[");
            if (result.contains("|") && !result.contains("(?")) {
                result = "(?:" + result + ")";
            }
        }

        return EQUIVALENCE.matcher(result).replaceAll("\\\\b");
    }

    // POSIX[inner] of the object literal, whose prototype answers too
    private static @Nullable String posix(String inner) {
        String ch = POSIX.get(inner);
        if (ch != null) {
            return ch;
        }
        if (PlainObject.PROTO.equals(inner)) {
            return "[object Object]";
        }
        PlainObject.BuiltinFunction inherited = PlainObject.objectFunction(inner);
        return inherited == null ? null : inherited.toString();
    }
}
