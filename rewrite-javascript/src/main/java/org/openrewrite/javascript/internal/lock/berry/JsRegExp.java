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

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A JavaScript regular expression source as V8 reads it without the {@code u} flag, as a {@link Pattern}.
 */
final class JsRegExp {

    private static final String DOT = "[^\\n\\r\\u2028\\u2029]";
    private static final String WORD = "[A-Za-z0-9_]";
    private static final String WORD_BOUNDARY = "(?:(?<=" + WORD + ")(?!" + WORD + ")|(?<!" + WORD + ")(?=" + WORD +
                                                "))";
    private static final String NOT_WORD_BOUNDARY = "(?:(?<=" + WORD + ")(?=" + WORD + ")|(?<!" + WORD + ")(?!" + WORD +
                                                    "))";
    private static final char[][] WHITESPACE = {{'\t', '\r'}, {' ', ' '}, {'\u00A0', '\u00A0'}, {'\u1680', '\u1680'},
            {'\u2000', '\u200A'}, {'\u2028', '\u2029'}, {'\u202F', '\u202F'}, {'\u205F', '\u205F'},
            {'\u3000', '\u3000'}, {'\uFEFF', '\uFEFF'}};

    private JsRegExp() {
    }

    /**
     * {@code new RegExp(source)}: null when V8 throws its SyntaxError.
     */
    static @Nullable Pattern compile(String source) {
        String java = toJava(source);
        if (java == null) {
            return null;
        }
        try {
            return Pattern.compile(java);
        } catch (PatternSyntaxException e) {
            throw new CannotKnow("how V8 reads the regular expression /" + source + "/");
        }
    }

    /**
     * A regular expression literal of the ported module's source.
     */
    static Pattern of(String source) {
        Pattern pattern = compile(source);
        if (pattern == null) {
            throw new IllegalArgumentException("V8 rejects /" + source + "/");
        }
        return pattern;
    }

    /**
     * The source as a Java regular expression, or null for a source V8 rejects.
     */
    static @Nullable String toJava(String source) {
        int captures = 0;
        boolean named = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '[') {
                int end = classEnd(source, i);
                if (end < 0) {
                    return null;
                }
                i = end - 1;
            } else if (c == '(') {
                if (!source.startsWith("?", i + 1)) {
                    captures++;
                } else if (source.startsWith("?<", i + 1) && !source.startsWith("?<=", i + 1) &&
                           !source.startsWith("?<!", i + 1)) {
                    captures++;
                    named = true;
                }
            }
        }

        StringBuilder out = new StringBuilder();
        // each open group: its capture number, or 0 for a non-capturing group or lookahead, -1 for a lookbehind
        Deque<Integer> open = new ArrayDeque<>();
        Set<Integer> closed = new HashSet<>();
        int opened = 0;
        boolean atom = false;
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            switch (c) {
                case '\\': {
                    if (i + 1 == source.length()) {
                        return null;
                    }
                    char e = source.charAt(i + 1);
                    if (e == 'b' || e == 'B') {
                        out.append(e == 'b' ? WORD_BOUNDARY : NOT_WORD_BOUNDARY);
                        atom = false;
                        i += 2;
                        break;
                    }
                    if (e >= '1' && e <= '9') {
                        int end = i + 1;
                        while (end < source.length() && Character.isDigit(source.charAt(end))) {
                            end++;
                        }
                        int n = Integer.parseInt(source.substring(i + 1, Math.min(end, i + 10)));
                        if (n <= captures) {
                            // a group that has not closed yet holds no capture, which V8 matches as empty
                            out.append(closed.contains(n) ? "(?:\\" + n + ")" : "(?:)");
                            atom = true;
                            i = end;
                            break;
                        }
                    }
                    if (e == 'k' && named) {
                        int close = source.indexOf('>', i + 3);
                        if (!source.startsWith("<", i + 2) || close < 0) {
                            return null;
                        }
                        out.append(source, i, close + 1);
                        atom = true;
                        i = close + 1;
                        break;
                    }
                    List<String> item = new ArrayList<>();
                    i = escape(source, i + 1, item, false);
                    out.append(item.get(0));
                    atom = true;
                    break;
                }
                case '[': {
                    int end = translateClass(source, i, out);
                    if (end < 0) {
                        return null;
                    }
                    i = end;
                    atom = true;
                    break;
                }
                case '(': {
                    if (source.startsWith("(?:", i) || source.startsWith("(?=", i) || source.startsWith("(?!", i)) {
                        open.push(0);
                        out.append(source, i, i + 3);
                        i += 3;
                    } else if (source.startsWith("(?<=", i) || source.startsWith("(?<!", i)) {
                        open.push(-1);
                        out.append(source, i, i + 4);
                        i += 4;
                    } else if (source.startsWith("(?<", i)) {
                        int close = source.indexOf('>', i);
                        if (close < 0) {
                            return null;
                        }
                        open.push(++opened);
                        out.append(source, i, close + 1);
                        i = close + 1;
                    } else if (source.startsWith("(?", i)) {
                        return null;
                    } else {
                        open.push(++opened);
                        out.append('(');
                        i++;
                    }
                    atom = false;
                    break;
                }
                case ')': {
                    if (open.isEmpty()) {
                        return null;
                    }
                    int group = open.pop();
                    closed.add(group);
                    atom = group >= 0;
                    out.append(')');
                    i++;
                    break;
                }
                case '|':
                case '^':
                    out.append(c);
                    atom = false;
                    i++;
                    break;
                case '$':
                    out.append("\\z");
                    atom = false;
                    i++;
                    break;
                case '.':
                    out.append(DOT);
                    atom = true;
                    i++;
                    break;
                case '*':
                case '+':
                case '?': {
                    if (!atom) {
                        return null;
                    }
                    out.append(c);
                    i = lazy(source, i + 1, out);
                    atom = false;
                    break;
                }
                case '{': {
                    int end = bracedQuantifier(source, i);
                    if (end == -2) {
                        return null;
                    }
                    if (end > 0) {
                        if (!atom) {
                            return null;
                        }
                        out.append(source, i, end);
                        i = lazy(source, end, out);
                        atom = false;
                    } else {
                        out.append("\\{");
                        atom = true;
                        i++;
                    }
                    break;
                }
                default:
                    out.append(literal(c));
                    atom = true;
                    i++;
                    break;
            }
        }
        return open.isEmpty() ? out.toString() : null;
    }

    private static int lazy(String source, int i, StringBuilder out) {
        if (i < source.length() && source.charAt(i) == '?') {
            out.append('?');
            return i + 1;
        }
        return i;
    }

    // the end of a valid {n}, {n,} or {n,m}; 0 when the brace is a literal, -2 for numbers out of order
    private static int bracedQuantifier(String source, int i) {
        int j = i + 1;
        int startMin = j;
        while (j < source.length() && Character.isDigit(source.charAt(j))) {
            j++;
        }
        if (j == startMin || j == source.length()) {
            return 0;
        }
        String min = source.substring(startMin, j);
        String max = min;
        if (source.charAt(j) == ',') {
            int startMax = ++j;
            while (j < source.length() && Character.isDigit(source.charAt(j))) {
                j++;
            }
            max = j == startMax ? null : source.substring(startMax, j);
        }
        if (j == source.length() || source.charAt(j) != '}') {
            return 0;
        }
        if (max != null && new BigInteger(min).compareTo(new BigInteger(max)) > 0) {
            return -2;
        }
        return j + 1;
    }

    private static int classEnd(String source, int i) {
        StringBuilder ignored = new StringBuilder();
        return translateClass(source, i, ignored);
    }

    // the index after the class, -1 when it is unterminated or holds a range out of order
    private static int translateClass(String source, int start, StringBuilder out) {
        int i = start + 1;
        boolean negated = i < source.length() && source.charAt(i) == '^';
        if (negated) {
            i++;
        }
        List<String> items = new ArrayList<>();
        while (true) {
            if (i >= source.length()) {
                return -1;
            }
            char c = source.charAt(i);
            if (c == ']') {
                i++;
                break;
            }
            List<String> from = new ArrayList<>();
            i = classAtom(source, i, from);
            if (i + 1 < source.length() && source.charAt(i) == '-' && source.charAt(i + 1) != ']') {
                List<String> to = new ArrayList<>();
                int next = classAtom(source, i + 1, to);
                if (from.size() == 2 && to.size() == 2) {
                    if (from.get(1).charAt(0) > to.get(1).charAt(0)) {
                        return -1;
                    }
                    items.add(from.get(0) + "-" + to.get(0));
                } else {
                    items.add(from.get(0));
                    items.add(literal('-'));
                    items.add(to.get(0));
                }
                i = next;
            } else {
                items.add(from.get(0));
            }
        }
        if (items.isEmpty()) {
            out.append(negated ? "[\\s\\S]" : "(?!)");
        } else {
            out.append(negated ? "[^" : "[");
            for (String item : items) {
                out.append(item);
            }
            out.append(']');
        }
        return i;
    }

    // one class atom: its Java form, then the character itself when it is one character
    private static int classAtom(String source, int i, List<String> atom) {
        char c = source.charAt(i);
        if (c != '\\' || i + 1 == source.length()) {
            atom.add(literal(c));
            atom.add(String.valueOf(c));
            return i + 1;
        }
        return escape(source, i + 1, atom, true);
    }

    // an escape after its backslash: its Java form, and the character when it is one
    private static int escape(String source, int i, List<String> item, boolean inClass) {
        char e = source.charAt(i);
        switch (e) {
            case 'd':
            case 'D':
            case 'w':
            case 'W':
                item.add("\\" + e);
                return i + 1;
            case 's':
                item.add(inClass ? whitespace(false) : "[" + whitespace(false) + "]");
                return i + 1;
            case 'S':
                item.add(inClass ? whitespace(true) : "[" + whitespace(true) + "]");
                return i + 1;
            case 'b':
                return character('\b', i + 1, item);
            case 'f':
                return character('\f', i + 1, item);
            case 'n':
                return character('\n', i + 1, item);
            case 'r':
                return character('\r', i + 1, item);
            case 't':
                return character('\t', i + 1, item);
            case 'v':
                return character('\u000B', i + 1, item);
            case 'c': {
                char next = i + 1 < source.length() ? source.charAt(i + 1) : 0;
                if (next >= 'a' && next <= 'z' || next >= 'A' && next <= 'Z' ||
                    inClass && (next >= '0' && next <= '9' || next == '_')) {
                    return character((char) (next % 32), i + 2, item);
                }
                return character('\\', i, item);
            }
            case 'x':
                if (hex(source, i + 1, 2)) {
                    return character((char) Integer.parseInt(source.substring(i + 1, i + 3), 16), i + 3, item);
                }
                return character('x', i + 1, item);
            case 'u':
                if (hex(source, i + 1, 4)) {
                    return character((char) Integer.parseInt(source.substring(i + 1, i + 5), 16), i + 5, item);
                }
                return character('u', i + 1, item);
            default:
                if (e >= '0' && e <= '7') {
                    int value = e - '0';
                    int j = i + 1;
                    int max = e <= '3' ? 3 : 2;
                    while (j < source.length() && j - i < max && source.charAt(j) >= '0' && source.charAt(j) <= '7') {
                        value = value * 8 + source.charAt(j) - '0';
                        j++;
                    }
                    return character((char) value, j, item);
                }
                return character(e, i + 1, item);
        }
    }

    private static int character(char c, int next, List<String> item) {
        item.add(literal(c));
        item.add(String.valueOf(c));
        return next;
    }

    private static boolean hex(String source, int i, int n) {
        if (i + n > source.length()) {
            return false;
        }
        for (int j = i; j < i + n; j++) {
            if (Character.digit(source.charAt(j), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static String literal(char c) {
        if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9') {
            return String.valueOf(c);
        }
        return String.format("\\u%04X", (int) c);
    }

    private static String whitespace(boolean complement) {
        StringBuilder out = new StringBuilder();
        if (!complement) {
            for (char[] range : WHITESPACE) {
                out.append(literal(range[0])).append('-').append(literal(range[1]));
            }
            return out.toString();
        }
        char from = 0;
        for (char[] range : WHITESPACE) {
            if (range[0] > from) {
                out.append(literal(from)).append('-').append(literal((char) (range[0] - 1)));
            }
            from = (char) (range[1] + 1);
        }
        return out.append(literal(from)).append('-').append(literal('\uFFFF')).toString();
    }
}
