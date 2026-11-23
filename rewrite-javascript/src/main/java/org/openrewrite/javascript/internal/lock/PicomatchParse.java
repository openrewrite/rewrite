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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.javascript.internal.lock.EcmaScript.trim;
import static org.openrewrite.javascript.internal.lock.Picomatch.*;

/**
 * Port of picomatch lib/parse.js, 2.3.1 and 4.0.4: 4.0.4 merges adjacent text from the value and guards repeated
 * extglobs; 2.3.1 reads its tables through {@code Object.prototype}.
 */
final class PicomatchParse {

    private static final Pattern REGEX_NON_SPECIAL_CHARS = V8RegExp.of("^[^@![\\].,$*+?^{}()|\\\\/]+");
    private static final Pattern NO_FAST_PATH = V8RegExp.of("(^[*!]|[/()[\\]{}\"])");
    private static final Pattern BACKSLASHES = V8RegExp.of("^\\\\+");
    private static final Pattern ALL_BACKSLASHES = V8RegExp.of("\\\\+");
    private static final Pattern CLOSING_PARENS = V8RegExp.of("^\\)+$");
    private static final Pattern EXTENSION = V8RegExp.of("^\\.[^\\\\/.]+$");
    private static final Pattern GROUP_START = V8RegExp.of("^\\([^?]");
    private static final Pattern LOOKAROUND = V8RegExp.of("[!=<:]");
    private static final Pattern LOOKBEHIND = V8RegExp.of("<([!=]|\\w+>)");
    private static final Pattern EXTENSION_SUFFIX = V8RegExp.of("^(.*?)\\.(\\w+)$");
    private static final Pattern SIMPLE_AT_EXTGLOB = V8RegExp.of("^@\\([^\\\\()[\\]{}|]+\\)$");
    private static final Pattern ESCAPED_CHAR = V8RegExp.of("\\\\(.)");
    private static final Pattern STARS_AND_QMARKS = V8RegExp.of("^[*?]+$");

    private PicomatchParse() {
    }

    static final class Token {
        String type;
        String value;
        @Nullable String output;
        @Nullable Token prev;
        boolean extglob;
        boolean star;
        boolean posix;
        boolean comma;
        boolean dots;
        int outputIndex;
        int tokensIndex;

        Token(String type, String value) {
            this(type, value, null);
        }

        Token(String type, String value, @Nullable String output) {
            this.type = type;
            this.value = value;
            this.output = output;
        }
    }

    private static final class Extglob {
        final String type;
        final String open;
        String close;
        String inner = "";
        @Nullable Token prev;
        int parens;
        String output = "";
        int startIndex;
        int tokensIndex;

        Extglob(String type, String open, String close) {
            this.type = type;
            this.open = open;
            this.close = close;
        }

        static Extglob of(String value) {
            switch (value) {
                case "!":
                    return new Extglob("negate", "(?:(?!(?:", "))" + STAR + ")");
                case "?":
                    return new Extglob("qmark", "(?:", ")?");
                case "+":
                    return new Extglob("plus", "(?:", ")+");
                case "*":
                    return new Extglob("star", "(?:", ")*");
                default:
                    return new Extglob("at", "(?:", ")");
            }
        }
    }

    static final class State {
        int index = -1;
        int start;
        String output = "";
        boolean backtrack;
        boolean negated;
        int brackets;
        int braces;
        int parens;
        int quotes;
        final List<Token> tokens = new ArrayList<>();
    }

    /**
     * @throws JsError the SyntaxError for an input over 65536 characters, and 2.3.1's TypeError for an input
     *                     naming a member of {@code Object.prototype}
     */
    static State parse(String input, Options options, boolean fastpaths) {
        return new Parser(input, options, fastpaths).parse();
    }

    static @Nullable String fastpaths(String input, Options options) {
        if (input.length() > MAX_LENGTH) {
            throw new JsError(JsError.SYNTAX_ERROR, "Input length: " + input.length() +
                                                              ", exceeds maximum allowed length: " + MAX_LENGTH);
        }
        boolean dot = options.isDot();
        String replaced = replacement(input, options.getRelease());
        if (replaced != null) {
            input = replaced;
        }
        String nodot = dot ? NO_DOTS : NO_DOT;
        String slashDot = dot ? NO_DOTS_SLASH : NO_DOT;
        String output = input.startsWith("./") ? input.substring(2) : input;
        String source = create(output, dot, nodot, slashDot);
        if (source != null) {
            source += SLASH_LITERAL + "?";
        }
        return source;
    }

    private static @Nullable String create(String str, boolean dot, String nodot, String slashDot) {
        switch (str) {
            case "*":
                return nodot + ONE_CHAR + STAR;
            case ".*":
                return DOT_LITERAL + ONE_CHAR + STAR;
            case "*.*":
                return nodot + STAR + DOT_LITERAL + ONE_CHAR + STAR;
            case "*/*":
                return nodot + STAR + SLASH_LITERAL + ONE_CHAR + slashDot + STAR;
            case "**":
                return nodot + globstar(dot);
            case "**/*":
                return "(?:" + nodot + globstar(dot) + SLASH_LITERAL + ")?" + slashDot + ONE_CHAR + STAR;
            case "**/*.*":
                return "(?:" + nodot + globstar(dot) + SLASH_LITERAL + ")?" + slashDot + STAR + DOT_LITERAL +
                       ONE_CHAR + STAR;
            case "**/.*":
                return "(?:" + nodot + globstar(dot) + SLASH_LITERAL + ")?" + DOT_LITERAL + ONE_CHAR + STAR;
            default: {
                Matcher match = EXTENSION_SUFFIX.matcher(str);
                if (!match.find()) {
                    return null;
                }
                String source = create(match.group(1), dot, nodot, slashDot);
                if (source == null || source.isEmpty()) {
                    return null;
                }
                return source + DOT_LITERAL + match.group(2);
            }
        }
    }

    private static String globstar(boolean dot) {
        return "(?:(?:(?!" + START_ANCHOR + (dot ? DOTS_SLASH : DOT_LITERAL) + ").)*?)";
    }

    private static boolean test(Pattern pattern, @Nullable String s) {
        return pattern.matcher(s == null ? "undefined" : s).find();
    }

    // JavaScript's string concatenation, where an unset output reads "undefined"
    private static String concat(@Nullable String a, String b) {
        return (a == null ? "undefined" : a) + b;
    }

    private static List<String> splitTopLevel(String input) {
        List<String> parts = new ArrayList<>();
        int bracket = 0;
        int paren = 0;
        int quote = 0;
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (escaped) {
                value.append(ch);
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                value.append(ch);
                escaped = true;
                continue;
            }
            if (ch == '"') {
                quote = quote == 1 ? 0 : 1;
                value.append(ch);
                continue;
            }
            if (quote == 0) {
                if (ch == '[') {
                    bracket++;
                } else if (ch == ']' && bracket > 0) {
                    bracket--;
                } else if (bracket == 0) {
                    if (ch == '(') {
                        paren++;
                    } else if (ch == ')' && paren > 0) {
                        paren--;
                    } else if (ch == '|' && paren == 0) {
                        parts.add(value.toString());
                        value.setLength(0);
                        continue;
                    }
                }
            }
            value.append(ch);
        }
        parts.add(value.toString());
        return parts;
    }

    private static boolean isPlainBranch(String branch) {
        boolean escaped = false;
        for (int i = 0; i < branch.length(); i++) {
            char ch = branch.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if ("?*+@!()[]{}".indexOf(ch) >= 0) {
                return false;
            }
        }
        return true;
    }

    private static @Nullable String normalizeSimpleBranch(String branch) {
        String value = trim(branch);
        while (SIMPLE_AT_EXTGLOB.matcher(value).find()) {
            value = value.substring(2, value.length() - 1);
        }
        if (!isPlainBranch(value)) {
            return null;
        }
        return ESCAPED_CHAR.matcher(value).replaceAll("$1");
    }

    private static boolean hasRepeatedCharPrefixOverlap(List<String> branches) {
        List<String> values = new ArrayList<>();
        for (String branch : branches) {
            String normalized = normalizeSimpleBranch(branch);
            if (normalized != null && !normalized.isEmpty()) {
                values.add(normalized);
            }
        }
        for (int i = 0; i < values.size(); i++) {
            for (int j = i + 1; j < values.size(); j++) {
                String a = values.get(i);
                String b = values.get(j);
                char c = a.charAt(0);
                if (!onlyChar(a, c) || !onlyChar(b, c)) {
                    continue;
                }
                if (a.equals(b) || a.startsWith(b) || b.startsWith(a)) {
                    return true;
                }
            }
        }
        return false;
    }

    // s === c.repeat(s.length)
    private static boolean onlyChar(String s, char c) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != c) {
                return false;
            }
        }
        return true;
    }

    // {type, body, end} of a `+(` or `*(` extglob at the start of the pattern
    private static @Nullable String[] parseRepeatedExtglob(String pattern, boolean requireEnd) {
        if (pattern.length() < 2 || (pattern.charAt(0) != '+' && pattern.charAt(0) != '*') ||
            pattern.charAt(1) != '(') {
            return null;
        }
        int bracket = 0;
        int paren = 0;
        int quote = 0;
        boolean escaped = false;
        for (int i = 1; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                quote = quote == 1 ? 0 : 1;
                continue;
            }
            if (quote == 1) {
                continue;
            }
            if (ch == '[') {
                bracket++;
                continue;
            }
            if (ch == ']' && bracket > 0) {
                bracket--;
                continue;
            }
            if (bracket > 0) {
                continue;
            }
            if (ch == '(') {
                paren++;
                continue;
            }
            if (ch == ')') {
                paren--;
                if (paren == 0) {
                    if (requireEnd && i != pattern.length() - 1) {
                        return null;
                    }
                    return new String[]{String.valueOf(pattern.charAt(0)), pattern.substring(2, i),
                            String.valueOf(i)};
                }
            }
        }
        return null;
    }

    private static @Nullable String getStarExtglobSequenceOutput(String pattern) {
        int index = 0;
        List<String> chars = new ArrayList<>();
        while (index < pattern.length()) {
            String[] match = parseRepeatedExtglob(pattern.substring(index), false);
            if (match == null || !"*".equals(match[0])) {
                return null;
            }
            List<String> branches = splitTopLevel(match[1]);
            if (branches.size() != 1) {
                return null;
            }
            String branch = normalizeSimpleBranch(trim(branches.get(0)));
            if (branch == null || branch.length() != 1) {
                return null;
            }
            chars.add(branch);
            index += Integer.parseInt(match[2]) + 1;
        }
        if (chars.isEmpty()) {
            return null;
        }
        StringBuilder source = new StringBuilder();
        if (chars.size() == 1) {
            source.append(escapeRegex(chars.get(0)));
        } else {
            source.append('[');
            for (String c : chars) {
                source.append(escapeRegex(c));
            }
            source.append(']');
        }
        return source + "*";
    }

    private static int repeatedExtglobRecursion(String pattern) {
        int depth = 0;
        String value = trim(pattern);
        String[] match = parseRepeatedExtglob(value, true);
        while (match != null) {
            depth++;
            value = trim(match[1]);
            match = parseRepeatedExtglob(value, true);
        }
        return depth;
    }

    private static final class RepeatedExtglob {
        final boolean risky;
        final @Nullable String safeOutput;

        RepeatedExtglob(boolean risky, @Nullable String safeOutput) {
            this.risky = risky;
            this.safeOutput = safeOutput;
        }
    }

    private static RepeatedExtglob analyzeRepeatedExtglob(String body) {
        List<String> branches = new ArrayList<>();
        for (String branch : splitTopLevel(body)) {
            branches.add(trim(branch));
        }
        if (branches.size() > 1) {
            for (String branch : branches) {
                if (branch.isEmpty() || STARS_AND_QMARKS.matcher(branch).find()) {
                    return new RepeatedExtglob(true, null);
                }
            }
            if (hasRepeatedCharPrefixOverlap(branches)) {
                return new RepeatedExtglob(true, null);
            }
        }
        for (String branch : branches) {
            String safeOutput = getStarExtglobSequenceOutput(branch);
            if (safeOutput != null && !safeOutput.isEmpty()) {
                return new RepeatedExtglob(true, safeOutput);
            }
            if (repeatedExtglobRecursion(branch) > DEFAULT_MAX_EXTGLOB_RECURSION) {
                return new RepeatedExtglob(true, null);
            }
        }
        return new RepeatedExtglob(false, null);
    }

    private static final class Parser {
        private final Options options;
        private final boolean dot;
        private final boolean fastpaths;
        private final State state = new State();
        private final List<Token> tokens = state.tokens;
        private final Token bos = new Token("bos", "", "");
        private final List<Extglob> extglobs = new ArrayList<>();
        private final List<Token> braces = new ArrayList<>();
        private final List<String> stack = new ArrayList<>();
        private final String nodot;
        private final String qmarkNoDot;
        private String input;
        private int len;
        private Token prev = bos;
        private String value = "";

        Parser(String input, Options options, boolean fastpaths) {
            this.options = options;
            this.dot = options.isDot();
            this.fastpaths = fastpaths;
            this.nodot = dot ? "" : NO_DOT;
            this.qmarkNoDot = dot ? QMARK : QMARK_NO_DOT;
            String replaced = replacement(input, options.getRelease());
            if (replaced != null && options.getRelease() == Release.V2_3_1 &&
                EcmaScript.objectPrototypeMember(input) != null) {
                throw new JsError(JsError.TYPE_ERROR, "output.startsWith is not a function");
            }
            this.input = replaced != null ? replaced : input;
            this.len = this.input.length();
        }

        State parse() {
            if (len > MAX_LENGTH) {
                throw new JsError(JsError.SYNTAX_ERROR, "Input length: " + len +
                                                                  ", exceeds maximum allowed length: " + MAX_LENGTH);
            }
            tokens.add(bos);
            if (input.startsWith("./")) {
                input = input.substring(2);
            }
            len = input.length();

            if (fastpaths && !test(NO_FAST_PATH, input)) {
                state.output = wrapOutput(fastPathOutput(), state.negated);
                return state;
            }

            while (!eos()) {
                value = advance();

                if ("\u0000".equals(value)) {
                    continue;
                }

                if ("\\".equals(value)) {
                    String next = peek(1);
                    if ("/".equals(next)) {
                        continue;
                    }
                    if (".".equals(next) || ";".equals(next)) {
                        continue;
                    }
                    if (next == null) {
                        value += "\\";
                        push(new Token("text", value));
                        continue;
                    }
                    Matcher match = BACKSLASHES.matcher(remaining());
                    if (match.find() && match.group().length() > 2) {
                        int slashes = match.group().length();
                        state.index += slashes;
                        if (slashes % 2 != 0) {
                            value += "\\";
                        }
                    }
                    value += advance();
                    if (state.brackets == 0) {
                        push(new Token("text", value));
                        continue;
                    }
                }

                if (state.brackets > 0 && (!"]".equals(value) || "[".equals(prev.value) || "[^".equals(prev.value))) {
                    if (":".equals(value)) {
                        String inner = prev.value.substring(1);
                        if (inner.contains("[")) {
                            prev.posix = true;
                            if (inner.contains(":")) {
                                int idx = prev.value.lastIndexOf('[');
                                String pre = prev.value.substring(0, idx);
                                String rest = prev.value.substring(Math.min(idx + 2, prev.value.length()));
                                String posix = posixRegexSource(rest, options.getRelease());
                                if (posix != null) {
                                    prev.value = pre + posix;
                                    state.backtrack = true;
                                    advance();
                                    if ((bos.output == null || bos.output.isEmpty()) && tokens.indexOf(prev) == 1) {
                                        bos.output = ONE_CHAR;
                                    }
                                    continue;
                                }
                            }
                        }
                    }
                    if (("[".equals(value) && !":".equals(peek(1))) || ("-".equals(value) && "]".equals(peek(1)))) {
                        value = "\\" + value;
                    }
                    if ("]".equals(value) && ("[".equals(prev.value) || "[^".equals(prev.value))) {
                        value = "\\" + value;
                    }
                    if (options.isPosix() && "!".equals(value) && "[".equals(prev.value)) {
                        value = "^";
                    }
                    prev.value += value;
                    append(value, null);
                    continue;
                }

                if (state.quotes == 1 && !"\"".equals(value)) {
                    value = escapeRegex(value);
                    prev.value += value;
                    append(value, null);
                    continue;
                }

                if ("\"".equals(value)) {
                    state.quotes = state.quotes == 1 ? 0 : 1;
                    continue;
                }

                if ("(".equals(value)) {
                    increment("parens");
                    push(new Token("paren", value));
                    continue;
                }

                if (")".equals(value)) {
                    Extglob extglob = extglobs.isEmpty() ? null : extglobs.get(extglobs.size() - 1);
                    if (extglob != null && state.parens == extglob.parens + 1) {
                        extglobClose(extglobs.remove(extglobs.size() - 1));
                        continue;
                    }
                    push(new Token("paren", value, state.parens != 0 ? ")" : "\\)"));
                    decrement("parens");
                    continue;
                }

                if ("[".equals(value)) {
                    if (!remaining().contains("]")) {
                        value = "\\" + value;
                    } else {
                        increment("brackets");
                    }
                    push(new Token("bracket", value));
                    continue;
                }

                if ("]".equals(value)) {
                    if ("bracket".equals(prev.type) && prev.value.length() == 1) {
                        push(new Token("text", value, "\\" + value));
                        continue;
                    }
                    if (state.brackets == 0) {
                        push(new Token("text", value, "\\" + value));
                        continue;
                    }
                    decrement("brackets");
                    String prevValue = prev.value.substring(1);
                    if (!prev.posix && prevValue.startsWith("^") && !prevValue.contains("/")) {
                        value = "/" + value;
                    }
                    prev.value += value;
                    append(value, null);
                    if (hasRegexChars(prevValue)) {
                        continue;
                    }
                    String escaped = escapeRegex(prev.value);
                    state.output = EcmaScript.slice(state.output, 0, -prev.value.length());
                    prev.value = "(?:" + escaped + "|" + prev.value + ")";
                    state.output += prev.value;
                    continue;
                }

                if ("{".equals(value)) {
                    increment("braces");
                    Token open = new Token("brace", value, "(");
                    open.outputIndex = state.output.length();
                    open.tokensIndex = tokens.size();
                    braces.add(open);
                    push(open);
                    continue;
                }

                if ("}".equals(value)) {
                    Token brace = braces.isEmpty() ? null : braces.get(braces.size() - 1);
                    if (brace == null) {
                        push(new Token("text", value, value));
                        continue;
                    }
                    String output = ")";
                    if (brace.dots) {
                        List<Token> arr = new ArrayList<>(tokens);
                        List<String> range = new ArrayList<>();
                        for (int i = arr.size() - 1; i >= 0; i--) {
                            tokens.remove(tokens.size() - 1);
                            if ("brace".equals(arr.get(i).type)) {
                                break;
                            }
                            if (!"dots".equals(arr.get(i).type)) {
                                range.add(0, arr.get(i).value);
                            }
                        }
                        output = expandRange(range);
                        state.backtrack = true;
                    }
                    if (!brace.comma && !brace.dots) {
                        String out = state.output.substring(0, Math.min(brace.outputIndex, state.output.length()));
                        List<Token> toks = new ArrayList<>(tokens.subList(Math.min(brace.tokensIndex, tokens.size()),
                                tokens.size()));
                        brace.value = brace.output = "\\{";
                        value = output = "\\}";
                        state.output = out;
                        for (Token t : toks) {
                            state.output += t.output != null && !t.output.isEmpty() ? t.output : t.value;
                        }
                    }
                    push(new Token("brace", value, output));
                    decrement("braces");
                    braces.remove(braces.size() - 1);
                    continue;
                }

                if ("|".equals(value)) {
                    push(new Token("text", value));
                    continue;
                }

                if (",".equals(value)) {
                    String output = value;
                    Token brace = braces.isEmpty() ? null : braces.get(braces.size() - 1);
                    if (brace != null && !stack.isEmpty() && "braces".equals(stack.get(stack.size() - 1))) {
                        brace.comma = true;
                        output = "|";
                    }
                    push(new Token("comma", value, output));
                    continue;
                }

                if ("/".equals(value)) {
                    if ("dot".equals(prev.type) && state.index == state.start + 1) {
                        state.start = state.index + 1;
                        state.output = "";
                        tokens.remove(tokens.size() - 1);
                        prev = bos;
                        continue;
                    }
                    push(new Token("slash", value, SLASH_LITERAL));
                    continue;
                }

                if (".".equals(value)) {
                    if (state.braces > 0 && "dot".equals(prev.type)) {
                        if (".".equals(prev.value)) {
                            prev.output = DOT_LITERAL;
                        }
                        Token brace = braces.get(braces.size() - 1);
                        prev.type = "dots";
                        prev.output = concat(prev.output, value);
                        prev.value += value;
                        brace.dots = true;
                        continue;
                    }
                    if (state.braces + state.parens == 0 && !"bos".equals(prev.type) && !"slash".equals(prev.type)) {
                        push(new Token("text", value, DOT_LITERAL));
                        continue;
                    }
                    push(new Token("dot", value, DOT_LITERAL));
                    continue;
                }

                if ("?".equals(value)) {
                    boolean isGroup = "(".equals(prev.value);
                    if (!isGroup && "(".equals(peek(1)) && !"?".equals(peek(2))) {
                        extglobOpen("qmark", value);
                        continue;
                    }
                    if ("paren".equals(prev.type)) {
                        String next = peek(1);
                        String output = value;
                        if (("(".equals(prev.value) && !test(LOOKAROUND, next)) ||
                            ("<".equals(next) && !test(LOOKBEHIND, remaining()))) {
                            output = "\\" + value;
                        }
                        push(new Token("text", value, output));
                        continue;
                    }
                    if (!dot && ("slash".equals(prev.type) || "bos".equals(prev.type))) {
                        push(new Token("qmark", value, QMARK_NO_DOT));
                        continue;
                    }
                    push(new Token("qmark", value, QMARK));
                    continue;
                }

                if ("!".equals(value)) {
                    if ("(".equals(peek(1))) {
                        if (!"?".equals(peek(2)) || !test(LOOKAROUND, peek(3))) {
                            extglobOpen("negate", value);
                            continue;
                        }
                    }
                    if (state.index == 0) {
                        negate();
                        continue;
                    }
                }

                if ("+".equals(value)) {
                    if ("(".equals(peek(1)) && !"?".equals(peek(2))) {
                        extglobOpen("plus", value);
                        continue;
                    }
                    if ("(".equals(prev.value)) {
                        push(new Token("plus", value, PLUS_LITERAL));
                        continue;
                    }
                    if ("bracket".equals(prev.type) || "paren".equals(prev.type) || "brace".equals(prev.type) ||
                        state.parens > 0) {
                        push(new Token("plus", value));
                        continue;
                    }
                    push(new Token("plus", PLUS_LITERAL));
                    continue;
                }

                if ("@".equals(value)) {
                    if ("(".equals(peek(1)) && !"?".equals(peek(2))) {
                        Token at = new Token("at", value, "");
                        at.extglob = true;
                        push(at);
                        continue;
                    }
                    push(new Token("text", value));
                    continue;
                }

                if (!"*".equals(value)) {
                    if ("$".equals(value) || "^".equals(value)) {
                        value = "\\" + value;
                    }
                    Matcher match = REGEX_NON_SPECIAL_CHARS.matcher(remaining());
                    if (match.find()) {
                        value += match.group();
                        state.index += match.group().length();
                    }
                    push(new Token("text", value));
                    continue;
                }

                if ("globstar".equals(prev.type) || prev.star) {
                    prev.type = "star";
                    prev.star = true;
                    prev.value += value;
                    prev.output = STAR;
                    state.backtrack = true;
                    continue;
                }

                String rest = remaining();
                if (test(GROUP_START, rest)) {
                    extglobOpen("star", value);
                    continue;
                }

                if ("star".equals(prev.type)) {
                    Token prior = prev.prev;
                    Token before = prior.prev;
                    boolean isStart = "slash".equals(prior.type) || "bos".equals(prior.type);
                    boolean afterStar = before != null && ("star".equals(before.type) ||
                                                           "globstar".equals(before.type));
                    boolean isBrace = state.braces > 0 && ("comma".equals(prior.type) || "brace".equals(prior.type));
                    boolean isExtglob = !extglobs.isEmpty() && ("pipe".equals(prior.type) ||
                                                                "paren".equals(prior.type));
                    if (!isStart && !"paren".equals(prior.type) && !isBrace && !isExtglob) {
                        push(new Token("star", value, ""));
                        continue;
                    }

                    while (rest.startsWith("/**")) {
                        String after = charAt(state.index + 4);
                        if (after != null && !"/".equals(after)) {
                            break;
                        }
                        rest = rest.substring(3);
                        state.index += 3;
                    }

                    if ("bos".equals(prior.type) && eos()) {
                        prev.type = "globstar";
                        prev.value += value;
                        prev.output = globstar(dot);
                        state.output = prev.output;
                        continue;
                    }

                    if ("slash".equals(prior.type) && !"bos".equals(prior.prev.type) && !afterStar && eos()) {
                        state.output = EcmaScript.slice(state.output, 0, -(prior.output + prev.output).length());
                        prior.output = "(?:" + prior.output;
                        prev.type = "globstar";
                        prev.output = globstar(dot) + "|$)";
                        prev.value += value;
                        state.output += prior.output + prev.output;
                        continue;
                    }

                    if ("slash".equals(prior.type) && !"bos".equals(prior.prev.type) && rest.startsWith("/")) {
                        String end = rest.length() > 1 ? "|$" : "";
                        state.output = EcmaScript.slice(state.output, 0, -(prior.output + prev.output).length());
                        prior.output = "(?:" + prior.output;
                        prev.type = "globstar";
                        prev.output = globstar(dot) + SLASH_LITERAL + "|" + SLASH_LITERAL + end + ")";
                        prev.value += value;
                        state.output += prior.output + prev.output;
                        advance();
                        push(new Token("slash", "/", ""));
                        continue;
                    }

                    if ("bos".equals(prior.type) && rest.startsWith("/")) {
                        prev.type = "globstar";
                        prev.value += value;
                        prev.output = "(?:^|" + SLASH_LITERAL + "|" + globstar(dot) + SLASH_LITERAL + ")";
                        state.output = prev.output;
                        advance();
                        push(new Token("slash", "/", ""));
                        continue;
                    }

                    state.output = EcmaScript.slice(state.output, 0, -prev.output.length());
                    prev.type = "globstar";
                    prev.output = globstar(dot);
                    prev.value += value;
                    state.output += prev.output;
                    continue;
                }

                Token token = new Token("star", value, STAR);
                if (state.index == state.start || "slash".equals(prev.type) || "dot".equals(prev.type)) {
                    String guard = "dot".equals(prev.type) ? NO_DOT_SLASH : dot ? NO_DOTS_SLASH : nodot;
                    state.output += guard;
                    prev.output = concat(prev.output, guard);
                    if (!"*".equals(peek(1))) {
                        state.output += ONE_CHAR;
                        prev.output = concat(prev.output, ONE_CHAR);
                    }
                }
                push(token);
            }

            while (state.brackets > 0) {
                state.output = escapeLast(state.output, '[', -1);
                decrement("brackets");
            }
            while (state.parens > 0) {
                state.output = escapeLast(state.output, '(', -1);
                decrement("parens");
            }
            while (state.braces > 0) {
                state.output = escapeLast(state.output, '{', -1);
                decrement("braces");
            }

            if ("star".equals(prev.type) || "bracket".equals(prev.type)) {
                push(new Token("maybe_slash", "", SLASH_LITERAL + "?"));
            }

            if (state.backtrack) {
                StringBuilder output = new StringBuilder();
                for (Token token : tokens) {
                    output.append(token.output != null ? token.output : token.value);
                }
                state.output = output.toString();
            }
            return state;
        }

        // input.replace(REGEX_SPECIAL_CHARS_BACKREF, ...) over UTF-16 units, then its backslash runs
        private String fastPathOutput() {
            boolean backslashes = false;
            StringBuilder output = new StringBuilder();
            int i = 0;
            while (i < input.length()) {
                char c = input.charAt(i);
                String esc;
                char first;
                int j;
                if (c == '\\' && i + 1 < input.length() && !isWordChar(input.charAt(i + 1))) {
                    esc = "\\";
                    first = input.charAt(i + 1);
                    j = i + 2;
                } else if (!isWordChar(c)) {
                    esc = "";
                    first = c;
                    j = i + 1;
                } else {
                    output.append(c);
                    i++;
                    continue;
                }
                int restStart = j;
                while (j < input.length() && input.charAt(j) == first) {
                    j++;
                }
                String rest = input.substring(restStart, j);
                String m = input.substring(i, j);
                int chars = 1 + rest.length();
                if (first == '\\') {
                    backslashes = true;
                    output.append(m);
                } else if (first == '?') {
                    if (!esc.isEmpty()) {
                        output.append(esc).append(first).append(EcmaScript.repeat(QMARK, rest.length()));
                    } else if (i == 0) {
                        output.append(qmarkNoDot).append(EcmaScript.repeat(QMARK, rest.length()));
                    } else {
                        output.append(EcmaScript.repeat(QMARK, chars));
                    }
                } else if (first == '.') {
                    output.append(EcmaScript.repeat(DOT_LITERAL, chars));
                } else if (first == '*') {
                    if (!esc.isEmpty()) {
                        output.append(esc).append(first).append(rest.isEmpty() ? "" : STAR);
                    } else {
                        output.append(STAR);
                    }
                } else {
                    output.append(esc.isEmpty() ? "\\" + m : m);
                }
                i = j;
            }
            String out = output.toString();
            if (backslashes) {
                Matcher m = ALL_BACKSLASHES.matcher(out);
                StringBuilder replaced = new StringBuilder();
                int last = 0;
                while (m.find()) {
                    replaced.append(out, last, m.start()).append(m.group().length() % 2 == 0 ? "\\\\" : "\\");
                    last = m.end();
                }
                out = replaced.append(out.substring(last)).toString();
            }
            return out;
        }

        private boolean eos() {
            return state.index == len - 1;
        }

        private @Nullable String peek(int n) {
            return charAt(state.index + n);
        }

        private @Nullable String charAt(int i) {
            return i >= 0 && i < len ? String.valueOf(input.charAt(i)) : null;
        }

        private String advance() {
            String c = charAt(++state.index);
            return c == null ? "" : c;
        }

        private String remaining() {
            return input.substring(Math.min(state.index + 1, len));
        }

        private void append(String value, @Nullable String output) {
            state.output += output != null ? output : value;
        }

        private void negate() {
            int count = 1;
            while ("!".equals(peek(1)) && (!"(".equals(peek(2)) || "?".equals(peek(3)))) {
                advance();
                state.start++;
                count++;
            }
            if (count % 2 == 0) {
                return;
            }
            state.negated = true;
            state.start++;
        }

        private void increment(String type) {
            add(type, 1);
            stack.add(type);
        }

        private void decrement(String type) {
            add(type, -1);
            if (!stack.isEmpty()) {
                stack.remove(stack.size() - 1);
            }
        }

        private void add(String type, int n) {
            switch (type) {
                case "parens":
                    state.parens += n;
                    break;
                case "brackets":
                    state.brackets += n;
                    break;
                default:
                    state.braces += n;
            }
        }

        private void push(Token tok) {
            if ("globstar".equals(prev.type)) {
                boolean isBrace = state.braces > 0 && ("comma".equals(tok.type) || "brace".equals(tok.type));
                boolean isExtglob = tok.extglob || (!extglobs.isEmpty() && ("pipe".equals(tok.type) ||
                                                                            "paren".equals(tok.type)));
                if (!"slash".equals(tok.type) && !"paren".equals(tok.type) && !isBrace && !isExtglob) {
                    state.output = EcmaScript.slice(state.output, 0, -prev.output.length());
                    prev.type = "star";
                    prev.value = "*";
                    prev.output = STAR;
                    state.output += prev.output;
                }
            }

            if (!extglobs.isEmpty() && !"paren".equals(tok.type)) {
                extglobs.get(extglobs.size() - 1).inner += tok.value;
            }

            if (!tok.value.isEmpty() || (tok.output != null && !tok.output.isEmpty())) {
                append(tok.value, tok.output);
            }
            if ("text".equals(prev.type) && "text".equals(tok.type)) {
                if (options.getRelease() == Release.V2_3_1) {
                    prev.value += tok.value;
                    prev.output = (prev.output == null ? "" : prev.output) + tok.value;
                } else {
                    prev.output = (prev.output == null || prev.output.isEmpty() ? prev.value : prev.output) +
                                  tok.value;
                    prev.value += tok.value;
                }
                return;
            }

            tok.prev = prev;
            tokens.add(tok);
            prev = tok;
        }

        private void extglobOpen(String type, String value) {
            Extglob token = Extglob.of(value);
            token.prev = prev;
            token.parens = state.parens;
            token.output = state.output;
            token.startIndex = state.index;
            token.tokensIndex = tokens.size();
            String output = token.open;

            increment("parens");
            push(new Token(type, value, state.output.isEmpty() ? ONE_CHAR : ""));
            Token paren = new Token("paren", advance(), output);
            paren.extglob = true;
            push(paren);
            extglobs.add(token);
        }

        private void extglobClose(Extglob token) {
            String literal = input.substring(token.startIndex, Math.min(state.index + 1, len));
            String body = token.startIndex + 2 < state.index ? input.substring(token.startIndex + 2, state.index) : "";
            RepeatedExtglob analysis = options.getRelease() == Release.V2_3_1 ? new RepeatedExtglob(false, null) :
                    analyzeRepeatedExtglob(body);
            if (("plus".equals(token.type) || "star".equals(token.type)) && analysis.risky) {
                String safeOutput = analysis.safeOutput != null ?
                        (token.output.isEmpty() ? ONE_CHAR : "") + analysis.safeOutput : null;
                Token open = tokens.get(token.tokensIndex);
                open.type = "text";
                open.value = literal;
                open.output = safeOutput != null && !safeOutput.isEmpty() ? safeOutput : escapeRegex(literal);
                for (int i = token.tokensIndex + 1; i < tokens.size(); i++) {
                    tokens.get(i).value = "";
                    tokens.get(i).output = "";
                }
                state.output = token.output + open.output;
                state.backtrack = true;
                Token paren = new Token("paren", value, "");
                paren.extglob = true;
                push(paren);
                decrement("parens");
                return;
            }
            String output = token.close;
            if ("negate".equals(token.type)) {
                String extglobStar = STAR;
                if (token.inner.length() > 1 && token.inner.contains("/")) {
                    extglobStar = globstar(dot);
                }
                if (!extglobStar.equals(STAR) || eos() || test(CLOSING_PARENS, remaining())) {
                    output = token.close = ")$))" + extglobStar;
                }
                String rest = remaining();
                if (token.inner.contains("*") && !rest.isEmpty() && test(EXTENSION, rest)) {
                    String expression = new Parser(rest, options, false).parse().output;
                    output = token.close = ")" + expression + ")" + extglobStar + ")";
                }
            }
            Token paren = new Token("paren", value, output);
            paren.extglob = true;
            push(paren);
            decrement("parens");
        }

        private static String expandRange(List<String> args) {
            List<String> sorted = new ArrayList<>(args);
            Collections.sort(sorted);
            String value = "[" + String.join("-", sorted) + "]";
            if (V8RegExp.toJava(value) != null) {
                return value;
            }
            List<String> escaped = new ArrayList<>();
            for (String v : sorted) {
                escaped.add(escapeRegex(v));
            }
            return String.join("..", escaped);
        }

        private static boolean isWordChar(char c) {
            return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
        }
    }
}
