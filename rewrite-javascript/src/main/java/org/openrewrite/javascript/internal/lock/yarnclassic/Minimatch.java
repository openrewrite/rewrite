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

import lombok.Builder;
import lombok.Value;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Port of minimatch 3.0.4 on POSIX paths, as yarn 1.22.22 bundles it.
 */
final class Minimatch {

    @Value
    @Builder
    static class Options {
        static final Options DEFAULT = Options.builder().build();

        boolean dot;
        boolean nocase;
        boolean nocomment;
        boolean nonegate;
        boolean nobrace;
        boolean noglobstar;
        boolean noext;
        boolean matchBase;
        boolean flipNegate;
    }

    private static final Object GLOBSTAR = new Object();
    private static final Object FALSE = new Object();

    private static final String QMARK = "[^/]";
    private static final String STAR = QMARK + "*?";
    private static final String RE_SPECIALS = "().*{}+?[]^$\\!";
    private static final Pattern SLASH_SPLIT = Pattern.compile("/+");
    private static final Pattern HAS_BRACES = Pattern.compile("\\{" + Js.DOT + "*}");
    private static final Pattern PIPE_IN_TAIL = Pattern.compile("((?:\\\\{2}){0,64})(\\\\?)\\|");
    private static final Pattern CLOSE_AFTER = Pattern.compile("\\)[+*?]?");
    private static final Pattern GLOB_UNESCAPE = Pattern.compile("\\\\(" + Js.DOT + ")");

    private static final class PatternListEntry {
        final char type;
        final int start;
        final int reStart;
        final String open;
        final String close;
        int reEnd;

        PatternListEntry(char type, int start, int reStart) {
            this.type = type;
            this.start = start;
            this.reStart = reStart;
            this.open = type == '!' ? "(?:(?!(?:" : "(?:";
            switch (type) {
                case '!':
                    this.close = "))[^/]*?)";
                    break;
                case '?':
                    this.close = ")?";
                    break;
                case '+':
                    this.close = ")+";
                    break;
                case '*':
                    this.close = ")*";
                    break;
                default:
                    this.close = ")";
            }
        }
    }

    private final Options options;
    private String pattern;
    private boolean negate;
    private boolean comment;
    private boolean empty;
    private final List<List<Object>> set = new ArrayList<>();

    static boolean minimatch(String p, String pattern) {
        return minimatch(p, pattern, Options.DEFAULT);
    }

    static boolean minimatch(String p, String pattern, Options options) {
        if (!options.nocomment && pattern.startsWith("#")) {
            return false;
        }
        if (Js.trim(pattern).isEmpty()) {
            return p.isEmpty();
        }
        return new Minimatch(pattern, options).match(p);
    }

    Minimatch(String pattern, Options options) {
        this.options = options;
        this.pattern = Js.trim(pattern);
        make();
    }

    private void make() {
        if (!options.nocomment && pattern.startsWith("#")) {
            comment = true;
            return;
        }
        if (pattern.isEmpty()) {
            empty = true;
            return;
        }
        parseNegate();
        for (String glob : braceExpand()) {
            List<Object> parts = new ArrayList<>();
            boolean failed = false;
            for (String s : SLASH_SPLIT.split(glob, -1)) {
                Object parsed = parse(s, false);
                failed |= parsed == FALSE;
                parts.add(parsed);
            }
            if (!failed) {
                set.add(parts);
            }
        }
    }

    private void parseNegate() {
        if (options.nonegate) {
            return;
        }
        int negateOffset = 0;
        boolean neg = false;
        for (int i = 0; i < pattern.length() && pattern.charAt(i) == '!'; i++) {
            neg = !neg;
            negateOffset++;
        }
        if (negateOffset > 0) {
            pattern = pattern.substring(negateOffset);
        }
        negate = neg;
    }

    private List<String> braceExpand() {
        if (options.nobrace || !HAS_BRACES.matcher(pattern).find()) {
            return Collections.singletonList(pattern);
        }
        return BraceExpansion.expand(pattern);
    }

    /**
     * One path segment: {@link #GLOBSTAR}, a literal string, a compiled regular expression, or {@link #FALSE} for
     * a segment holding a slash. Under {@code isSub}, the source and whether it has magic.
     */
    private Object parse(String pattern, boolean isSub) {
        if (!options.noglobstar && "**".equals(pattern)) {
            return GLOBSTAR;
        }
        if (pattern.isEmpty()) {
            return "";
        }
        StringBuilder re = new StringBuilder();
        boolean[] hasMagic = {options.nocase};
        boolean escaping = false;
        Deque<PatternListEntry> patternListStack = new ArrayDeque<>();
        List<PatternListEntry> negativeLists = new ArrayList<>();
        char[] stateChar = {0};
        boolean inClass = false;
        int reClassStart = -1;
        int classStart = -1;
        String patternStart = pattern.charAt(0) == '.' ? "" : options.dot ?
                "(?!(?:^|\\/)\\.{1,2}(?:$|\\/))" : "(?!\\.)";

        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (escaping && RE_SPECIALS.indexOf(c) >= 0) {
                re.append('\\').append(c);
                escaping = false;
                continue;
            }
            switch (c) {
                case '/':
                    return FALSE;
                case '\\':
                    clearStateChar(stateChar, re, hasMagic);
                    escaping = true;
                    continue;
                case '?':
                case '*':
                case '+':
                case '@':
                case '!':
                    if (inClass) {
                        if (c == '!' && i == classStart + 1) {
                            c = '^';
                        }
                        re.append(c);
                        continue;
                    }
                    clearStateChar(stateChar, re, hasMagic);
                    stateChar[0] = c;
                    if (options.noext) {
                        clearStateChar(stateChar, re, hasMagic);
                    }
                    continue;
                case '(':
                    if (inClass) {
                        re.append('(');
                        continue;
                    }
                    if (stateChar[0] == 0) {
                        re.append("\\(");
                        continue;
                    }
                    patternListStack.push(new PatternListEntry(stateChar[0], i - 1, re.length()));
                    re.append(stateChar[0] == '!' ? "(?:(?!(?:" : "(?:");
                    stateChar[0] = 0;
                    continue;
                case ')':
                    if (inClass || patternListStack.isEmpty()) {
                        re.append("\\)");
                        continue;
                    }
                    clearStateChar(stateChar, re, hasMagic);
                    hasMagic[0] = true;
                    PatternListEntry pl = patternListStack.pop();
                    re.append(pl.close);
                    if (pl.type == '!') {
                        negativeLists.add(pl);
                    }
                    pl.reEnd = re.length();
                    continue;
                case '|':
                    if (inClass || patternListStack.isEmpty() || escaping) {
                        re.append("\\|");
                        escaping = false;
                        continue;
                    }
                    clearStateChar(stateChar, re, hasMagic);
                    re.append('|');
                    continue;
                case '[':
                    clearStateChar(stateChar, re, hasMagic);
                    if (inClass) {
                        re.append('\\').append(c);
                        continue;
                    }
                    inClass = true;
                    classStart = i;
                    reClassStart = re.length();
                    re.append(c);
                    continue;
                case ']':
                    if (i == classStart + 1 || !inClass) {
                        re.append('\\').append(c);
                        escaping = false;
                        continue;
                    }
                    String cs = pattern.substring(classStart + 1, i);
                    if (!validClass(cs)) {
                        Object[] sp = (Object[]) parse(cs, true);
                        re.setLength(reClassStart);
                        re.append("\\[").append(sp[0]).append("\\]");
                        hasMagic[0] = hasMagic[0] || (Boolean) sp[1];
                        inClass = false;
                        continue;
                    }
                    hasMagic[0] = true;
                    inClass = false;
                    re.append(c);
                    continue;
                default:
                    clearStateChar(stateChar, re, hasMagic);
                    if (escaping) {
                        escaping = false;
                    } else if (RE_SPECIALS.indexOf(c) >= 0 && !(c == '^' && inClass)) {
                        re.append('\\');
                    }
                    re.append(c);
            }
        }

        if (inClass) {
            String cs = pattern.substring(classStart + 1);
            Object[] sp = (Object[]) parse(cs, true);
            re.setLength(reClassStart);
            re.append("\\[").append(sp[0]);
            hasMagic[0] = hasMagic[0] || (Boolean) sp[1];
        }

        for (PatternListEntry pl = patternListStack.poll(); pl != null; pl = patternListStack.poll()) {
            String tail = re.substring(pl.reStart + pl.open.length());
            Matcher m = PIPE_IN_TAIL.matcher(tail);
            StringBuffer replaced = new StringBuffer();
            while (m.find()) {
                String g1 = m.group(1);
                String g2 = m.group(2).isEmpty() ? "\\" : m.group(2);
                m.appendReplacement(replaced, Matcher.quoteReplacement(g1 + g1 + g2 + "|"));
            }
            m.appendTail(replaced);
            String t = pl.type == '*' ? STAR : pl.type == '?' ? QMARK : "\\" + pl.type;
            hasMagic[0] = true;
            String head = re.substring(0, pl.reStart);
            re.setLength(0);
            re.append(head).append(t).append("\\(").append(replaced);
        }

        clearStateChar(stateChar, re, hasMagic);
        if (escaping) {
            re.append("\\\\");
        }

        boolean addPatternStart = re.length() > 0 && "[(.".indexOf(re.charAt(0)) >= 0;

        for (int n = negativeLists.size() - 1; n > -1; n--) {
            PatternListEntry nl = negativeLists.get(n);
            String s = re.toString();
            String nlBefore = s.substring(0, nl.reStart);
            String nlFirst = s.substring(nl.reStart, nl.reEnd - 8);
            String nlLast = s.substring(nl.reEnd - 8, nl.reEnd);
            String nlAfter = s.substring(nl.reEnd);
            nlLast += nlAfter;
            int openParensBefore = nlBefore.split("\\(", -1).length - 1;
            String cleanAfter = nlAfter;
            for (int i = 0; i < openParensBefore; i++) {
                cleanAfter = CLOSE_AFTER.matcher(cleanAfter).replaceFirst("");
            }
            nlAfter = cleanAfter;
            String dollar = nlAfter.isEmpty() && !isSub ? "$" : "";
            re.setLength(0);
            re.append(nlBefore).append(nlFirst).append(nlAfter).append(dollar).append(nlLast);
        }

        String src = re.toString();
        if (!src.isEmpty() && hasMagic[0]) {
            src = "(?=.)" + src;
        }
        if (addPatternStart) {
            src = patternStart + src;
        }
        if (isSub) {
            return new Object[]{src, hasMagic[0]};
        }
        if (!hasMagic[0]) {
            return GLOB_UNESCAPE.matcher(pattern).replaceAll("$1");
        }
        try {
            return Pattern.compile("^" + javaRegex(src) + "\\z", options.nocase ?
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0);
        } catch (PatternSyntaxException e) {
            return Pattern.compile("\\z.");
        }
    }

    private void clearStateChar(char[] stateChar, StringBuilder re, boolean[] hasMagic) {
        if (stateChar[0] != 0) {
            switch (stateChar[0]) {
                case '*':
                    re.append(STAR);
                    hasMagic[0] = true;
                    break;
                case '?':
                    re.append(QMARK);
                    hasMagic[0] = true;
                    break;
                default:
                    re.append('\\').append(stateChar[0]);
                    break;
            }
            stateChar[0] = 0;
        }
    }

    // RegExp('[' + cs + ']') compiles
    private static boolean validClass(String cs) {
        try {
            Pattern.compile(javaRegex("[" + cs + "]"));
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    /**
     * A JavaScript regular expression's source as Java reads it: {@code $} is the end of the input, {@code .}
     * leaves out JavaScript's line terminators only, and {@code [} and {@code &} are literal in a class.
     */
    static String javaRegex(String js) {
        StringBuilder out = new StringBuilder();
        boolean inClass = false;
        for (int i = 0; i < js.length(); i++) {
            char c = js.charAt(i);
            if (c == '\\' && i + 1 < js.length()) {
                out.append(c).append(js.charAt(++i));
                continue;
            }
            if (inClass) {
                if (c == ']') {
                    inClass = false;
                    out.append(c);
                } else if (c == '[' || c == '&') {
                    out.append('\\').append(c);
                } else {
                    out.append(c);
                }
                continue;
            }
            if (c == '[') {
                if (js.startsWith("[]", i)) {
                    out.append("(?!)");
                    i++;
                } else if (js.startsWith("[^]", i)) {
                    out.append("[\\s\\S]");
                    i += 2;
                } else {
                    inClass = true;
                    out.append(c);
                    if (i + 1 < js.length() && js.charAt(i + 1) == '^') {
                        out.append('^');
                        i++;
                    }
                }
            } else if (c == '$') {
                out.append("\\z");
            } else if (c == '.') {
                out.append(Js.DOT);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    boolean match(String f) {
        if (comment) {
            return false;
        }
        if (empty) {
            return f.isEmpty();
        }
        List<String> file = Arrays.asList(SLASH_SPLIT.split(f, -1));
        String filename = null;
        for (int i = file.size() - 1; i >= 0; i--) {
            filename = file.get(i);
            if (!filename.isEmpty()) {
                break;
            }
        }
        for (List<Object> pattern : set) {
            List<String> target = file;
            if (options.matchBase && pattern.size() == 1) {
                target = Collections.singletonList(filename);
            }
            if (matchOne(target, pattern)) {
                return options.flipNegate || !negate;
            }
        }
        return !options.flipNegate && negate;
    }

    private boolean matchOne(List<String> file, List<Object> pattern) {
        int fi = 0;
        int pi = 0;
        int fl = file.size();
        int pl = pattern.size();
        for (; fi < fl && pi < pl; fi++, pi++) {
            Object p = pattern.get(pi);
            String f = file.get(fi);
            if (p == FALSE) {
                return false;
            }
            if (p == GLOBSTAR) {
                int fr = fi;
                int pr = pi + 1;
                if (pr == pl) {
                    for (; fi < fl; fi++) {
                        String rest = file.get(fi);
                        if (".".equals(rest) || "..".equals(rest) || (!options.dot && rest.startsWith("."))) {
                            return false;
                        }
                    }
                    return true;
                }
                while (fr < fl) {
                    String swallowee = file.get(fr);
                    if (matchOne(file.subList(fr, fl), pattern.subList(pr, pl))) {
                        return true;
                    }
                    if (".".equals(swallowee) || "..".equals(swallowee) ||
                        (!options.dot && swallowee.startsWith("."))) {
                        break;
                    }
                    fr++;
                }
                return false;
            }
            boolean hit;
            if (p instanceof String) {
                hit = options.nocase ? f.equalsIgnoreCase((String) p) : f.equals(p);
            } else {
                hit = ((Pattern) p).matcher(f).find();
            }
            if (!hit) {
                return false;
            }
        }
        if (fi == fl && pi == pl) {
            return true;
        }
        if (fi == fl) {
            return false;
        }
        if (pi == pl) {
            return fi == fl - 1 && file.get(fi).isEmpty();
        }
        throw new IllegalStateException("wtf?");
    }
}
