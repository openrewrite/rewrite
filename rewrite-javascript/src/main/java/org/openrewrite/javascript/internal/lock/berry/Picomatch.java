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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Port of picomatch 2.3.1 lib/picomatch.js, lib/utils.js and lib/constants.js, as fast-glob 3.3.2 calls it.
 */
final class Picomatch {

    static final int MAX_LENGTH = 1024 * 64;

    static final String DOT_LITERAL = "\\.";
    static final String PLUS_LITERAL = "\\+";
    static final String SLASH_LITERAL = "\\/";
    static final String ONE_CHAR = "(?=.)";
    static final String QMARK = "[^/]";
    static final String END_ANCHOR = "(?:" + SLASH_LITERAL + "|$)";
    static final String START_ANCHOR = "(?:^|" + SLASH_LITERAL + ")";
    static final String DOTS_SLASH = DOT_LITERAL + "{1,2}" + END_ANCHOR;
    static final String NO_DOT = "(?!" + DOT_LITERAL + ")";
    static final String NO_DOTS = "(?!" + START_ANCHOR + DOTS_SLASH + ")";
    static final String NO_DOT_SLASH = "(?!" + DOT_LITERAL + "{0,1}" + END_ANCHOR + ")";
    static final String NO_DOTS_SLASH = "(?!" + DOTS_SLASH + ")";
    static final String QMARK_NO_DOT = "[^." + SLASH_LITERAL + "]";
    static final String STAR = QMARK + "*?";

    private static final Map<String, String> POSIX_REGEX_SOURCE = new HashMap<>();
    private static final Map<String, String> REPLACEMENTS = new HashMap<>();

    // what a lookup in an object literal finds on Object.prototype, as a string
    private static final List<String> OBJECT_PROTOTYPE = Arrays.asList("__defineGetter__", "__defineSetter__",
            "hasOwnProperty", "__lookupGetter__", "__lookupSetter__", "isPrototypeOf", "propertyIsEnumerable",
            "toString", "valueOf", "toLocaleString");

    private static final String REGEX_SPECIAL_CHARS = "-*+?.^${}(|)[]";
    private static final Pattern NEVER = JsRegExp.of("$^");

    static {
        POSIX_REGEX_SOURCE.put("alnum", "a-zA-Z0-9");
        POSIX_REGEX_SOURCE.put("alpha", "a-zA-Z");
        POSIX_REGEX_SOURCE.put("ascii", "\\x00-\\x7F");
        POSIX_REGEX_SOURCE.put("blank", " \\t");
        POSIX_REGEX_SOURCE.put("cntrl", "\\x00-\\x1F\\x7F");
        POSIX_REGEX_SOURCE.put("digit", "0-9");
        POSIX_REGEX_SOURCE.put("graph", "\\x21-\\x7E");
        POSIX_REGEX_SOURCE.put("lower", "a-z");
        POSIX_REGEX_SOURCE.put("print", "\\x20-\\x7E ");
        POSIX_REGEX_SOURCE.put("punct", "\\-!\"#$%&'()\\*+,./:;<=>?@[\\]^_`{|}~");
        POSIX_REGEX_SOURCE.put("space", " \\t\\r\\n\\v\\f");
        POSIX_REGEX_SOURCE.put("upper", "A-Z");
        POSIX_REGEX_SOURCE.put("word", "A-Za-z0-9_");
        POSIX_REGEX_SOURCE.put("xdigit", "A-Fa-f0-9");

        REPLACEMENTS.put("***", "*");
        REPLACEMENTS.put("**/**", "**");
        REPLACEMENTS.put("**/**/**", "**");
    }

    private Picomatch() {
    }

    /**
     * {@code picomatch.makeRe(input, options)}; throws JsError, picomatch's TypeError for an empty input or one naming
     * a member of {@code Object.prototype}.
     */
    static Pattern makeRe(String input, boolean dot) {
        return toRegex(source(input, dot, true));
    }

    /**
     * micromatch 4.0.5 {@code isMatch(str, pattern)} under its default options.
     * @throws JsError picomatch's TypeError for an empty pattern, and {@link #makeRe}'s
     */
    static boolean isMatch(String str, String pattern) {
        if (pattern.isEmpty()) {
            throw new JsError(JsError.TYPE_ERROR, "Expected pattern to be a non-empty string");
        }
        Pattern regex = toRegex(source(pattern, false, false));
        if (str.isEmpty()) {
            return false;
        }
        return str.equals(pattern) || regex.matcher(str).find();
    }

    /**
     * The JavaScript source {@code makeRe} gives {@code new RegExp}.
     */
    static String source(String input, boolean dot, boolean posix) {
        if (input.isEmpty()) {
            throw new JsError(JsError.TYPE_ERROR, "Expected a non-empty string");
        }
        String output = null;
        boolean negated = false;
        if (input.charAt(0) == '.' || input.charAt(0) == '*') {
            output = PicomatchParse.fastpaths(input, dot);
        }
        if (output == null || output.isEmpty()) {
            PicomatchParse.State state = PicomatchParse.parse(input, dot, true, posix);
            output = state.output;
            negated = state.negated;
        }
        String source = "^(?:" + output + ")$";
        return negated ? "^(?!" + source + ").*$" : source;
    }

    // V8's SyntaxError gives the regex that matches nothing but the empty string
    private static Pattern toRegex(String source) {
        Pattern pattern = JsRegExp.compile(source);
        return pattern != null ? pattern : NEVER;
    }

    /**
     * {@code REPLACEMENTS[input]}: a replacement, a member of {@code Object.prototype} as a string, or null.
     */
    static @Nullable String replacement(String input) {
        String replaced = REPLACEMENTS.get(input);
        return replaced != null ? replaced : objectPrototypeMember(input);
    }

    /**
     * {@code POSIX_REGEX_SOURCE[name]}: a character class body, a member of {@code Object.prototype} as a string, or
     * null.
     */
    static @Nullable String posixRegexSource(String name) {
        String source = POSIX_REGEX_SOURCE.get(name);
        return source != null ? source : objectPrototypeMember(name);
    }

    /**
     * {@code key in {}}, the property a plain object inherits from {@code Object.prototype}, as {@code String()}
     * gives it; null when it inherits none.
     */
    static @Nullable String objectPrototypeMember(String key) {
        if ("__proto__".equals(key)) {
            return "[object Object]";
        }
        if ("constructor".equals(key)) {
            return "function Object() { [native code] }";
        }
        return OBJECT_PROTOTYPE.contains(key) ? "function " + key + "() { [native code] }" : null;
    }

    static boolean hasRegexChars(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (REGEX_SPECIAL_CHARS.indexOf(str.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
    }

    static String escapeRegex(String str) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (REGEX_SPECIAL_CHARS.indexOf(c) >= 0) {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }

    /**
     * {@code escapeLast(input, char, lastIdx)}, from the end when {@code lastIdx} is negative.
     */
    static String escapeLast(String input, char c, int lastIdx) {
        int idx = lastIdx < 0 ? input.lastIndexOf(c) : input.lastIndexOf(c, lastIdx);
        if (idx == -1) {
            return input;
        }
        if (idx > 0 && input.charAt(idx - 1) == '\\') {
            return escapeLast(input, c, idx - 1);
        }
        return input.substring(0, idx) + "\\" + input.substring(idx);
    }

    static String wrapOutput(String input, boolean negated) {
        String output = "^(?:" + input + ")$";
        if (negated) {
            output = "(?:^(?!" + output + ").*$)";
        }
        return output;
    }
}
