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

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Port of picomatch lib/picomatch.js, lib/utils.js and lib/constants.js over paths with forward slashes: 2.3.1, which
 * fast-glob 3.3.2 and micromatch 4.0.5 call in Yarn 4.10.3, and 4.0.4, which tinyglobby 0.2.16 calls in pnpm 11.2.2.
 */
public final class Picomatch {

    public enum Release {
        V2_3_1,
        V4_0_4
    }

    /**
     * The release, and the {@code dot} and {@code posix} options; {@code posix} makes {@code [!} a negated class.
     */
    @Value
    public static class Options {
        Release release;
        boolean dot;
        boolean posix;
    }

    static final int MAX_LENGTH = 1024 * 64;
    static final int DEFAULT_MAX_EXTGLOB_RECURSION = 0;

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

    private static final String REGEX_SPECIAL_CHARS = "-*+?.^${}(|)[]";
    private static final Pattern NEVER = V8RegExp.of("$^");

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
     * {@code picomatch(globs, options)}: a matcher that is true when any of the globs matches.
     * @throws JsError picomatch's TypeError for an empty glob, and {@link #makeRe}'s
     */
    public static Predicate<String> matcher(List<String> globs, Options options) {
        List<Predicate<String>> fns = new ArrayList<>();
        for (String glob : globs) {
            fns.add(matcher(glob, options));
        }
        return str -> {
            for (Predicate<String> isMatch : fns) {
                if (isMatch.test(str)) {
                    return true;
                }
            }
            return false;
        };
    }

    public static Predicate<String> matcher(String glob, Options options) {
        if (glob.isEmpty()) {
            throw new JsError(JsError.TYPE_ERROR, "Expected pattern to be a non-empty string");
        }
        Pattern regex = makeRe(glob, options);
        return input -> test(input, regex, glob);
    }

    // picomatch.test without a format: the input as it is, or the regex
    private static boolean test(String input, Pattern regex, String glob) {
        if (input.isEmpty()) {
            return false;
        }
        return input.equals(glob) || regex.matcher(input).find();
    }

    /**
     * {@code picomatch.makeRe(input, options)}.
     * @throws JsError for an empty input, one over 65536 characters, and 2.3.1's for an Object.prototype member
     */
    public static Pattern makeRe(String input, Options options) {
        return toRegex(source(input, options));
    }

    /**
     * The source {@code compileRe} gives {@code new RegExp}.
     */
    public static String source(String input, Options options) {
        if (input.isEmpty()) {
            throw new JsError(JsError.TYPE_ERROR, "Expected a non-empty string");
        }
        String output = null;
        boolean negated = false;
        if (input.charAt(0) == '.' || input.charAt(0) == '*') {
            output = PicomatchParse.fastpaths(input, options);
        }
        if (output == null || output.isEmpty()) {
            PicomatchParse.State state = PicomatchParse.parse(input, options, true);
            output = state.output;
            negated = state.negated;
        }
        String source = "^(?:" + output + ")$";
        return negated ? "^(?!" + source + ").*$" : source;
    }

    // new RegExp(source); V8's SyntaxError gives /$^/, which matches nothing but the empty string
    private static Pattern toRegex(String source) {
        String java = V8RegExp.toJava(source);
        if (java == null) {
            return NEVER;
        }
        try {
            return Pattern.compile(java);
        } catch (PatternSyntaxException e) {
            throw new CannotKnow("how V8 reads the regular expression /" + source + "/");
        }
    }

    /**
     * {@code REPLACEMENTS[input]}: 2.3.1's object inherits from {@code Object.prototype}, 4.0.4's does not.
     */
    static @Nullable String replacement(String input, Release release) {
        String replaced = REPLACEMENTS.get(input);
        return replaced != null || release != Release.V2_3_1 ? replaced : EcmaScript.objectPrototypeMember(input);
    }

    /**
     * {@code POSIX_REGEX_SOURCE[name]}, an object that inherits from {@code Object.prototype} as in
     * {@link #replacement}.
     */
    static @Nullable String posixRegexSource(String name, Release release) {
        String source = POSIX_REGEX_SOURCE.get(name);
        return source != null || release != Release.V2_3_1 ? source : EcmaScript.objectPrototypeMember(name);
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
