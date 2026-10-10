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

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A JavaScript regular expression source as V8 reads it, without the {@code u} flag (ECMAScript Annex B) or with it,
 * translated to a {@link Pattern} source over the same strings, and {@code String.prototype.replace} over it.
 */
public final class V8RegExp {

    /**
     * JavaScript's {@code .} as a Java character class: Java's also leaves out {@code \u0085}.
     */
    public static final String DOT = "[^\\n\\r\\u2028\\u2029]";
    private static final String WORD = "[A-Za-z0-9_]";
    private static final String WORD_BOUNDARY = "(?:(?<=" + WORD + ")(?!" + WORD + ")|(?<!" + WORD + ")(?=" + WORD +
                                                "))";
    private static final String NOT_WORD_BOUNDARY = "(?:(?<=" + WORD + ")(?=" + WORD + ")|(?<!" + WORD + ")(?!" + WORD +
                                                    "))";
    private static final char[][] WHITESPACE = {{'\t', '\r'}, {' ', ' '}, {'\u00A0', '\u00A0'}, {'\u1680', '\u1680'},
            {'\u2000', '\u200A'}, {'\u2028', '\u2029'}, {'\u202F', '\u202F'}, {'\u205F', '\u205F'},
            {'\u3000', '\u3000'}, {'\uFEFF', '\uFEFF'}};
    private static final String HEX_DIGITS = "0123456789abcdefABCDEF";
    private static final String SYNTAX_CHARACTERS = "^$\\.*+?()[]{}|/";
    private static final Set<String> GENERAL_CATEGORIES = new HashSet<>(Arrays.asList("L", "Lu", "Ll", "Lt", "Lm",
            "Lo", "M", "Mn", "Mc", "Me", "N", "Nd", "Nl", "No", "P", "Pc", "Pd", "Ps", "Pe", "Pi", "Pf", "Po", "S",
            "Sm", "Sc", "Sk", "So", "Z", "Zs", "Zl", "Zp", "C", "Cc", "Cf", "Cs", "Co", "Cn"));
    private static final int LOOKAHEAD = -2;

    private V8RegExp() {
    }

    /**
     * {@code new RegExp(source)}: null when V8 throws its SyntaxError; cannot-know where Java reads the pattern apart
     * and for what the translation leaves out (modifiers, a group name beyond ASCII).
     */
    public static @Nullable Pattern compile(String source) {
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
     * {@code new RegExp(source, "u")}, Java matching code points as V8 then does; a property names a General_Category.
     * @throws JsError V8's SyntaxError; CannotKnow for what the translation leaves out (scripts, modifiers)
     */
    public static Pattern compileUnicode(String source) {
        String[] reason = new String[1];
        String java = toJava(source, true, reason);
        if (java == null) {
            if (reason[0] == null) {
                throw new CannotKnow("how V8 reads the regular expression /" + source + "/u");
            }
            throw new JsError(JsError.SYNTAX_ERROR, "Invalid regular expression: /" + source + "/u: " + reason[0]);
        }
        try {
            return Pattern.compile(java);
        } catch (PatternSyntaxException e) {
            throw new CannotKnow("how V8 reads the regular expression /" + source + "/u");
        }
    }

    /**
     * A regular expression literal of a ported module's source.
     */
    public static Pattern of(String source) {
        Pattern pattern = compile(source);
        if (pattern == null) {
            throw new IllegalArgumentException("V8 rejects /" + source + "/");
        }
        return pattern;
    }

    /**
     * {@code String.prototype.replace} with a regular expression and a replacement string, its {@code $} patterns
     * included.
     */
    public static String replace(String s, Pattern re, boolean global, String replacement) {
        Map<String, Integer> names = namedGroups(re.pattern());
        return replace(s, re, global, m -> substitution(s, m, replacement, names));
    }

    /**
     * {@code String.prototype.replace} with a regular expression and a replacement function of the match.
     */
    public static String replace(String s, Pattern re, boolean global, Function<Matcher, String> replacer) {
        Matcher m = re.matcher(s);
        StringBuilder out = new StringBuilder();
        int last = 0;
        while (m.find()) {
            out.append(s, last, m.start()).append(replacer.apply(m));
            last = m.end();
            if (!global) {
                break;
            }
        }
        return out.append(s.substring(last)).toString();
    }

    /**
     * {@code String.prototype.replace} of the first occurrence of a string.
     */
    public static String replace(String s, String search, String replacement) {
        int at = s.indexOf(search);
        if (at < 0) {
            return s;
        }
        Matcher m = Pattern.compile(Pattern.quote(search)).matcher(s);
        m.find(at);
        return s.substring(0, at) + substitution(s, m, replacement, Collections.emptyMap()) +
               s.substring(at + search.length());
    }

    // the capture number of each named group of a translated source
    private static Map<String, Integer> namedGroups(String java) {
        Map<String, Integer> names = new HashMap<>();
        int captures = 0;
        boolean inClass = false;
        for (int i = 0; i < java.length(); i++) {
            char c = java.charAt(i);
            if (c == '\\') {
                i++;
            } else if (inClass) {
                inClass = c != ']';
            } else if (c == '[') {
                inClass = true;
            } else if (c == '(' && !java.startsWith("?", i + 1)) {
                captures++;
            } else if (c == '(' && java.startsWith("?<", i + 1) && !java.startsWith("?<=", i + 1) &&
                       !java.startsWith("?<!", i + 1)) {
                names.put(java.substring(i + 3, java.indexOf('>', i)), ++captures);
            }
        }
        return names;
    }

    // GetSubstitution
    private static String substitution(String s, Matcher m, String replacement, Map<String, Integer> names) {
        StringBuilder out = new StringBuilder();
        int groups = m.groupCount();
        for (int i = 0; i < replacement.length(); i++) {
            char c = replacement.charAt(i);
            if (c != '$' || i + 1 == replacement.length()) {
                out.append(c);
                continue;
            }
            char next = replacement.charAt(i + 1);
            if (next == '$') {
                out.append('$');
                i++;
            } else if (next == '&') {
                out.append(m.group());
                i++;
            } else if (next == '`') {
                out.append(s, 0, m.start());
                i++;
            } else if (next == '\'') {
                out.append(s.substring(m.end()));
                i++;
            } else if (next == '<' && !names.isEmpty() && replacement.indexOf('>', i + 2) >= 0) {
                int close = replacement.indexOf('>', i + 2);
                Integer group = names.get(replacement.substring(i + 2, close));
                out.append(group == null ? "" : group(m, group));
                i = close;
            } else if (next >= '0' && next <= '9') {
                int one = next - '0';
                char after = i + 2 < replacement.length() ? replacement.charAt(i + 2) : ' ';
                int two = after >= '0' && after <= '9' ? one * 10 + after - '0' : -1;
                if (two >= 1 && two <= groups) {
                    out.append(group(m, two));
                    i += 2;
                } else if (one >= 1 && one <= groups) {
                    out.append(group(m, one));
                    i++;
                } else {
                    out.append(c);
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String group(Matcher m, int n) {
        String g = m.group(n);
        return g == null ? "" : g;
    }

    /**
     * The source as a Java regular expression, or null for a source V8 rejects; cannot-know as {@link #compile}.
     */
    public static @Nullable String toJava(String source) {
        return toJava(source, false, new String[1]);
    }

    // with the u flag, reason[0] is V8's message for a source it rejects, or null where the translation cannot say
    private static @Nullable String toJava(String source, boolean unicode, String[] reason) {
        int captures = 0;
        boolean named = false;
        Map<String, Integer> names = new HashMap<>();
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '[') {
                int end = unicode ? classExtent(source, i) : classEnd(source, i);
                if (end < 0) {
                    if (unicode) {
                        break;
                    }
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
                    int close = source.indexOf('>', i + 3);
                    if (close > 0) {
                        names.putIfAbsent(source.substring(i + 3, close), captures);
                    }
                }
            }
        }

        StringBuilder out = new StringBuilder();
        // each open group: its capture number, 0 for a non-capturing group, LOOKAHEAD or -1 for a lookbehind
        Deque<Integer> open = new ArrayDeque<>();
        Set<Integer> closed = new HashSet<>();
        int opened = 0;
        boolean atom = false;
        boolean afterLookaround = false;
        // V8 reports a reference to no group once the whole source has parsed; a modifier group is not translated
        boolean unresolved = false;
        boolean modifiers = false;
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            boolean lookaround = false;
            switch (c) {
                case '\\': {
                    if (i + 1 == source.length()) {
                        reason[0] = "\\ at end of pattern";
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
                        if (unicode) {
                            reason[0] = "Invalid escape";
                            return null;
                        }
                    }
                    if (e == 'k' && (unicode || named)) {
                        if (!source.startsWith("<", i + 2)) {
                            reason[0] = "Invalid named reference";
                            return null;
                        }
                        int close = groupNameEnd(source, i + 3, reason);
                        if (close < 0) {
                            return null;
                        }
                        Integer group = names.get(source.substring(i + 3, close));
                        unresolved |= group == null;
                        out.append(group != null && closed.contains(group) ? "(?:\\" + group + ")" : "(?:)");
                        atom = true;
                        i = close + 1;
                        break;
                    }
                    List<String> item = new ArrayList<>();
                    i = escape(source, i + 1, item, false, unicode, named, reason);
                    if (i < 0) {
                        return null;
                    }
                    out.append(item.get(0));
                    atom = true;
                    break;
                }
                case '[': {
                    int end = translateClass(source, i, out, unicode, named, reason);
                    if (end < 0) {
                        return null;
                    }
                    i = end;
                    atom = true;
                    break;
                }
                case '(': {
                    if (source.startsWith("(?:", i)) {
                        open.push(0);
                        out.append(source, i, i + 3);
                        i += 3;
                    } else if (source.startsWith("(?=", i) || source.startsWith("(?!", i)) {
                        open.push(LOOKAHEAD);
                        out.append(source, i, i + 3);
                        i += 3;
                    } else if (source.startsWith("(?<=", i) || source.startsWith("(?<!", i)) {
                        open.push(-1);
                        out.append(source, i, i + 4);
                        i += 4;
                    } else if (source.startsWith("(?<", i)) {
                        int close = groupNameEnd(source, i + 3, reason);
                        if (close < 0) {
                            return null;
                        }
                        open.push(++opened);
                        out.append(source, i, close + 1);
                        i = close + 1;
                    } else if (source.startsWith("(?", i)) {
                        int colon = modifiers(source, i + 2, reason);
                        if (colon < 0) {
                            return null;
                        }
                        modifiers = true;
                        open.push(0);
                        out.append("(?:");
                        i = colon + 1;
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
                        reason[0] = "Unmatched ')'";
                        return null;
                    }
                    int group = open.pop();
                    closed.add(group);
                    atom = group >= 0 || group == LOOKAHEAD && !unicode;
                    lookaround = group < 0;
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
                        reason[0] = afterLookaround ? "Invalid quantifier" : "Nothing to repeat";
                        return null;
                    }
                    out.append(c);
                    i = lazy(source, i + 1, out);
                    atom = false;
                    break;
                }
                case '{': {
                    int end = bracedQuantifier(source, i);
                    // a lookaround that the u flag (or a lookbehind) leaves unquantifiable still takes a quantifier
                    boolean quantifies = atom || afterLookaround;
                    if (end != 0 && !quantifies) {
                        reason[0] = "Nothing to repeat";
                        return null;
                    }
                    if (end == -2) {
                        reason[0] = "numbers out of order in {} quantifier";
                        return null;
                    }
                    if (end > 0) {
                        if (!atom) {
                            reason[0] = "Invalid quantifier";
                            return null;
                        }
                        out.append(source, i, end);
                        i = lazy(source, end, out);
                        atom = false;
                    } else if (unicode) {
                        reason[0] = quantifies ? "Incomplete quantifier" : "Lone quantifier brackets";
                        return null;
                    } else {
                        out.append("\\{");
                        atom = true;
                        i++;
                    }
                    break;
                }
                case '}':
                case ']':
                    if (unicode) {
                        reason[0] = "Lone quantifier brackets";
                        return null;
                    }
                    out.append(literal(c));
                    atom = true;
                    i++;
                    break;
                default:
                    out.append(literal(c));
                    atom = true;
                    i++;
                    break;
            }
            afterLookaround = lookaround;
        }
        if (!open.isEmpty()) {
            reason[0] = "Unterminated group";
            return null;
        }
        if (unresolved) {
            reason[0] = "Invalid named capture referenced";
            return null;
        }
        if (modifiers) {
            throw new CannotKnow("how V8 reads the modifiers of the regular expression /" + source + "/");
        }
        return out.toString();
    }

    // the index of the '>' after a group name that starts at i, or -1 where V8 rejects the name
    private static int groupNameEnd(String source, int i, String[] reason) {
        for (int j = i; j < source.length(); j++) {
            char c = source.charAt(j);
            if (c == '>' && j > i) {
                return j;
            }
            if (c >= 0x80 || c == '\\') {
                throw new CannotKnow("how V8 reads the group name in the regular expression /" + source + "/");
            }
            boolean identifier = c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c == '_' || c == '$' ||
                                 j > i && c >= '0' && c <= '9';
            if (!identifier) {
                break;
            }
        }
        reason[0] = "Invalid capture group name";
        return -1;
    }

    // a modifier group's flags after its "(?": the index of its ':', or -1 with V8's message
    private static int modifiers(String source, int i, String[] reason) {
        boolean dash = false;
        String seen = "";
        for (int j = i; j < source.length(); j++) {
            char c = source.charAt(j);
            if (c == '-') {
                if (dash) {
                    reason[0] = "Multiple dashes in flag group";
                    return -1;
                }
                dash = true;
            } else if (c == 'i' || c == 'm' || c == 's') {
                if (seen.indexOf(c) >= 0) {
                    reason[0] = "Repeated flag in flag group";
                    return -1;
                }
                seen += c;
            } else if (c == ':') {
                if (dash && seen.isEmpty()) {
                    reason[0] = "Invalid flag group";
                    return -1;
                }
                return j;
            } else {
                break;
            }
        }
        reason[0] = "Invalid group";
        return -1;
    }

    // the index after the class that opens at i, or -1 when nothing closes it
    private static int classExtent(String source, int i) {
        for (int j = i + 1; j < source.length(); j++) {
            char c = source.charAt(j);
            if (c == '\\') {
                j++;
            } else if (c == ']') {
                return j + 1;
            }
        }
        return -1;
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
        return translateClass(source, i, ignored, false, false, new String[1]);
    }

    // the index after the class, -1 when it is unterminated or holds a range out of order
    private static int translateClass(String source, int start, StringBuilder out, boolean unicode, boolean named,
                                      String[] reason) {
        int i = start + 1;
        boolean negated = i < source.length() && source.charAt(i) == '^';
        if (negated) {
            i++;
        }
        List<String> items = new ArrayList<>();
        while (true) {
            if (i >= source.length()) {
                reason[0] = "Unterminated character class";
                return -1;
            }
            char c = source.charAt(i);
            if (c == ']') {
                i++;
                break;
            }
            List<String> from = new ArrayList<>();
            i = classAtom(source, i, from, unicode, named, reason);
            if (i < 0) {
                return -1;
            }
            if (i + 1 < source.length() && source.charAt(i) == '-' && source.charAt(i + 1) != ']') {
                List<String> to = new ArrayList<>();
                int next = classAtom(source, i + 1, to, unicode, named, reason);
                if (next < 0) {
                    return -1;
                }
                if (from.size() == 2 && to.size() == 2) {
                    boolean outOfOrder = unicode ? from.get(1).codePointAt(0) > to.get(1).codePointAt(0) :
                            from.get(1).charAt(0) > to.get(1).charAt(0);
                    if (outOfOrder) {
                        reason[0] = "Range out of order in character class";
                        return -1;
                    }
                    items.add(from.get(0) + "-" + to.get(0));
                } else if (unicode) {
                    reason[0] = "Invalid character class";
                    return -1;
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

    // one class atom: its Java form, then the character itself when it is one character (a code point with the u flag)
    private static int classAtom(String source, int i, List<String> atom, boolean unicode, boolean named,
                                 String[] reason) {
        char c = source.charAt(i);
        if (unicode && Character.isHighSurrogate(c) && i + 1 < source.length() &&
            Character.isLowSurrogate(source.charAt(i + 1))) {
            atom.add(literal(c) + literal(source.charAt(i + 1)));
            atom.add(source.substring(i, i + 2));
            return i + 2;
        }
        if (c != '\\' || i + 1 == source.length()) {
            atom.add(literal(c));
            atom.add(String.valueOf(c));
            return i + 1;
        }
        return escape(source, i + 1, atom, true, unicode, named, reason);
    }

    // an escape after its backslash: its Java form, and the character when it is one; -1 where V8 rejects it
    private static int escape(String source, int i, List<String> item, boolean inClass, boolean unicode,
                              boolean named, String[] reason) {
        char e = source.charAt(i);
        if (unicode) {
            int next = unicodeEscape(source, i, item, inClass, reason);
            if (next != 0) {
                return next;
            }
        } else if (e == 'k' && named) {
            reason[0] = "Invalid escape";
            return -1;
        }
        switch (e) {
            case 'd':
            case 'D':
            case 'w':
            case 'W':
                item.add("\\" + e);
                return i + 1;
            case 's':
                item.add(inClass ? whitespace(false, unicode) : "[" + whitespace(false, unicode) + "]");
                return i + 1;
            case 'S':
                item.add(inClass ? whitespace(true, unicode) : "[" + whitespace(true, unicode) + "]");
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

    // what the u flag reads otherwise: 0 to read the escape as without it, -1 where V8 rejects it
    private static int unicodeEscape(String source, int i, List<String> item, boolean inClass, String[] reason) {
        char e = source.charAt(i);
        switch (e) {
            case 'd':
            case 'D':
            case 'w':
            case 'W':
            case 's':
            case 'S':
            case 'b':
            case 'f':
            case 'n':
            case 'r':
            case 't':
            case 'v':
                return 0;
            case 'c': {
                char next = i + 1 < source.length() ? source.charAt(i + 1) : 0;
                if (next >= 'a' && next <= 'z' || next >= 'A' && next <= 'Z') {
                    return 0;
                }
                reason[0] = "Invalid Unicode escape";
                return -1;
            }
            case 'x':
                if (hex(source, i + 1, 2)) {
                    return 0;
                }
                reason[0] = "Invalid escape";
                return -1;
            case 'u':
                return unicodeCodePoint(source, i, item, reason);
            case 'p':
            case 'P':
                return property(source, i, item, inClass, reason);
            case '0':
                if (i + 1 < source.length() && source.charAt(i + 1) >= '0' && source.charAt(i + 1) <= '9') {
                    reason[0] = "Invalid decimal escape";
                    return -1;
                }
                return character('\0', i + 1, item);
            default:
                if (e >= '1' && e <= '7') {
                    reason[0] = "Invalid decimal escape";
                    return -1;
                }
                if (SYNTAX_CHARACTERS.indexOf(e) >= 0 || inClass && e == '-') {
                    return character(e, i + 1, item);
                }
                reason[0] = "Invalid escape";
                return -1;
        }
    }

    // \\uXXXX, a surrogate pair of two, or \\u{X...} as one code point
    private static int unicodeCodePoint(String source, int i, List<String> item, String[] reason) {
        if (source.startsWith("{", i + 1)) {
            int close = source.indexOf('}', i + 2);
            String digits = close < 0 ? "" : source.substring(i + 2, close);
            if (!digits.isEmpty() && hex(digits, 0, digits.length()) &&
                new BigInteger(digits, 16).compareTo(BigInteger.valueOf(Character.MAX_CODE_POINT)) <= 0) {
                return codePoint(Integer.parseInt(digits, 16), close + 1, item);
            }
            reason[0] = "Invalid Unicode escape";
            return -1;
        }
        if (!hex(source, i + 1, 4)) {
            reason[0] = "Invalid Unicode escape";
            return -1;
        }
        char c = (char) Integer.parseInt(source.substring(i + 1, i + 5), 16);
        if (Character.isHighSurrogate(c) && source.startsWith("\\u", i + 5) && hex(source, i + 7, 4)) {
            char low = (char) Integer.parseInt(source.substring(i + 7, i + 11), 16);
            if (Character.isLowSurrogate(low)) {
                return codePoint(Character.toCodePoint(c, low), i + 11, item);
            }
        }
        return character(c, i + 5, item);
    }

    private static int codePoint(int cp, int next, List<String> item) {
        String s = new String(Character.toChars(cp));
        item.add(s.length() == 1 ? literal(s.charAt(0)) : literal(s.charAt(0)) + literal(s.charAt(1)));
        item.add(s);
        return next;
    }

    // \\p{name} or \\P{name} for a General_Category value, which Java names alike
    private static int property(String source, int i, List<String> item, boolean inClass, String[] reason) {
        int close = source.startsWith("{", i + 1) ? source.indexOf('}', i + 2) : -1;
        if (close < 0) {
            reason[0] = inClass ? "Invalid property name in character class" : "Invalid property name";
            return -1;
        }
        String name = source.substring(i + 2, close);
        String value = name.startsWith("General_Category=") ? name.substring("General_Category=".length()) :
                name.startsWith("gc=") ? name.substring("gc=".length()) : name;
        if (!GENERAL_CATEGORIES.contains(value)) {
            throw new CannotKnow("how V8 reads the property \\" + source.charAt(i) + "{" + name + "}");
        }
        item.add("\\" + source.charAt(i) + "{" + value + "}");
        return close + 1;
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
            if (HEX_DIGITS.indexOf(source.charAt(j)) < 0) {
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

    // \s or \S as class items; \S's code points go past U+FFFF with the u flag
    private static String whitespace(boolean complement, boolean unicode) {
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
        out.append(literal(from)).append('-').append(literal('\uFFFF'));
        return unicode ? out.append("\\x{10000}-\\x{10FFFF}").toString() : out.toString();
    }
}
