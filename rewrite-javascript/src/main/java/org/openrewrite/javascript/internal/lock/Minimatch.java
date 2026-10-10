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

import lombok.Builder;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Port of minimatch on POSIX paths: 3.0.4, which yarn 1.22.22 bundles, and 10.2.5, which npm 11.17.0 bundles,
 * without 10.2.5's extglobs, which no workspace pattern uses.
 */
public final class Minimatch {

    public enum Release {
        V3_0_4,
        V10_2_5
    }

    /**
     * The release and the options its port reads: 3.0.4's {@code nocase}, {@code nobrace}, {@code noglobstar},
     * {@code noext}, {@code matchBase} and {@code flipNegate}, 10.2.5's {@code partial} and {@code windowsPathsNoEscape}.
     */
    @Value
    @Builder(builderMethodName = "hiddenBuilder")
    public static class Options {
        Release release;
        boolean dot;
        boolean nocomment;
        boolean nonegate;
        boolean nocase;
        boolean nobrace;
        boolean noglobstar;
        boolean noext;
        boolean matchBase;
        boolean flipNegate;
        boolean partial;
        boolean windowsPathsNoEscape;

        public static OptionsBuilder builder(Release release) {
            return hiddenBuilder().release(release);
        }

        public static Options of(Release release) {
            return builder(release).build();
        }
    }

    /**
     * A 10.2.5 pattern with syntax this port does not implement.
     */
    public static final class UnsupportedPattern extends IllegalArgumentException {
        UnsupportedPattern(String pattern, String what) {
            super("The workspace pattern " + EcmaScript.quote(pattern) + " uses " + what + ", which is not supported");
        }
    }

    private static final Object GLOBSTAR = new Object() {
        @Override
        public String toString() {
            return "**";
        }
    };
    private static final Object FALSE = new Object();

    private static final int MAX_PATTERN_LENGTH = 1024 * 64;
    private static final Pattern SLASHES = Pattern.compile("/+");
    private static final Pattern NEVER = V8RegExp.of("$.");

    private final Options options;
    private String pattern;
    private boolean negate;
    private boolean comment;
    private boolean empty;
    private final List<List<Object>> set = new ArrayList<>();

    /**
     * @throws JsError the TypeError of a pattern over 65536 characters (3.0.4: a path segment over that)
     */
    public Minimatch(String pattern, Options options) {
        this.options = options;
        if (options.getRelease() == Release.V3_0_4) {
            this.pattern = EcmaScript.trim(pattern);
        } else {
            assertValidPattern(pattern);
            this.pattern = options.isWindowsPathsNoEscape() ? pattern.replace('\\', '/') : pattern;
        }
        make();
    }

    private static void assertValidPattern(String pattern) {
        if (pattern.length() > MAX_PATTERN_LENGTH) {
            throw new JsError(JsError.TYPE_ERROR, "pattern is too long");
        }
    }

    public static boolean minimatch(String path, String pattern, Options options) {
        if (options.getRelease() == Release.V3_0_4) {
            if (!options.isNocomment() && pattern.startsWith("#")) {
                return false;
            }
            if (EcmaScript.trim(pattern).isEmpty()) {
                return path.isEmpty();
            }
        }
        return new Minimatch(pattern, options).match(path);
    }

    public static boolean minimatch(String path, String pattern, Release release) {
        return minimatch(path, pattern, Options.of(release));
    }

    public static List<String> match(List<String> list, String pattern, Release release) {
        return match(list, pattern, Options.of(release));
    }

    public static List<String> match(List<String> list, String pattern, Options options) {
        Minimatch mm = new Minimatch(pattern, options);
        List<String> out = new ArrayList<>();
        for (String f : list) {
            if (mm.match(f)) {
                out.add(f);
            }
        }
        return out;
    }

    private void make() {
        if (!options.isNocomment() && pattern.startsWith("#")) {
            comment = true;
            return;
        }
        if (pattern.isEmpty()) {
            empty = true;
            return;
        }
        parseNegate();
        if (options.getRelease() == Release.V3_0_4) {
            for (String glob : braceExpand()) {
                List<Object> parts = new ArrayList<>();
                boolean failed = false;
                for (String s : SLASHES.split(glob, -1)) {
                    Object parsed = parse3(s, false);
                    failed |= parsed == FALSE;
                    parts.add(parsed);
                }
                if (!failed) {
                    set.add(parts);
                }
            }
            return;
        }
        Set<String> globSet = new LinkedHashSet<>(braceExpand());
        for (String glob : globSet) {
            List<String> parts = levelOneOptimize(slashSplit(glob));
            List<Object> compiled = new ArrayList<>();
            for (String part : parts) {
                compiled.add(parse10(part));
            }
            set.add(compiled);
        }
    }

    private void parseNegate() {
        if (options.isNonegate()) {
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

    private static final Pattern HAS_BRACES_3_0_4 = V8RegExp.of("\\{.*\\}");
    private static final Pattern HAS_BRACE_10_2_5 = V8RegExp.of("\\{(?:(?!\\{).)*\\}");

    private List<String> braceExpand() {
        if (options.getRelease() == Release.V3_0_4) {
            if (options.isNobrace() || !HAS_BRACES_3_0_4.matcher(pattern).find()) {
                return Collections.singletonList(pattern);
            }
            return BraceExpansion.expand(pattern, BraceExpansion.Release.V1_1_11);
        }
        if (!HAS_BRACE_10_2_5.matcher(pattern).find()) {
            return Collections.singletonList(pattern);
        }
        return BraceExpansion.expand(pattern, BraceExpansion.Release.V5_0_6);
    }

    public boolean match(String f) {
        if (comment) {
            return false;
        }
        if (empty) {
            return f.isEmpty();
        }
        if (options.getRelease() == Release.V3_0_4) {
            return match3(f);
        }
        return match10(f, options.isPartial());
    }

    // 3.0.4

    private static final String QMARK_3 = "[^/]";
    private static final String STAR_3 = QMARK_3 + "*?";
    private static final String RE_SPECIALS_3 = "().*{}+?[]^$\\!";
    private static final Pattern PIPE_IN_TAIL = V8RegExp.of("((?:\\\\{2}){0,64})(\\\\?)\\|");
    private static final Pattern CLOSE_AFTER = V8RegExp.of("\\)[+*?]?");
    private static final Pattern GLOB_UNESCAPE = V8RegExp.of("\\\\(.)");

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

    /**
     * One path segment: {@link #GLOBSTAR}, a literal string, a compiled regular expression, or {@link #FALSE} for
     * a segment holding a slash. Under {@code isSub}, the source and whether it has magic.
     */
    private Object parse3(String pattern, boolean isSub) {
        assertValidPattern(pattern);
        if (!options.isNoglobstar() && "**".equals(pattern)) {
            return GLOBSTAR;
        }
        if (pattern.isEmpty()) {
            return "";
        }
        StringBuilder re = new StringBuilder();
        boolean[] hasMagic = {options.isNocase()};
        boolean escaping = false;
        Deque<PatternListEntry> patternListStack = new ArrayDeque<>();
        List<PatternListEntry> negativeLists = new ArrayList<>();
        char[] stateChar = {0};
        boolean inClass = false;
        int reClassStart = -1;
        int classStart = -1;
        String patternStart = pattern.charAt(0) == '.' ? "" : options.isDot() ?
                "(?!(?:^|\\/)\\.{1,2}(?:$|\\/))" : "(?!\\.)";

        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (escaping && RE_SPECIALS_3.indexOf(c) >= 0) {
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
                    if (options.isNoext()) {
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
                    // new RegExp('[' + cs + ']') throws
                    if (V8RegExp.toJava("[" + cs + "]") == null) {
                        Object[] sp = subParse(cs);
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
                    } else if (RE_SPECIALS_3.indexOf(c) >= 0 && !(c == '^' && inClass)) {
                        re.append('\\');
                    }
                    re.append(c);
            }
        }

        if (inClass) {
            String cs = pattern.substring(classStart + 1);
            Object[] sp = subParse(cs);
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
            String t = pl.type == '*' ? STAR_3 : pl.type == '?' ? QMARK_3 : "\\" + pl.type;
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
        // new RegExp('^' + src + '$', flags), whose SyntaxError gives /$./
        String java = V8RegExp.toJava("^" + src + "$");
        if (java == null) {
            return NEVER;
        }
        try {
            return Pattern.compile(java, options.isNocase() ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0);
        } catch (PatternSyntaxException e) {
            throw new CannotKnow("how V8 reads the regular expression /^" + src + "$/");
        }
    }

    // this.parse(cs, SUBPARSE): an empty class gives '', whose [0] and [1] are undefined
    private Object[] subParse(String cs) {
        Object sp = parse3(cs, true);
        return sp instanceof Object[] ? (Object[]) sp : new Object[]{"undefined", false};
    }

    private void clearStateChar(char[] stateChar, StringBuilder re, boolean[] hasMagic) {
        if (stateChar[0] != 0) {
            switch (stateChar[0]) {
                case '*':
                    re.append(STAR_3);
                    hasMagic[0] = true;
                    break;
                case '?':
                    re.append(QMARK_3);
                    hasMagic[0] = true;
                    break;
                default:
                    re.append('\\').append(stateChar[0]);
                    break;
            }
            stateChar[0] = 0;
        }
    }

    private boolean match3(String f) {
        List<String> file = Arrays.asList(SLASHES.split(f, -1));
        String filename = null;
        for (int i = file.size() - 1; i >= 0; i--) {
            filename = file.get(i);
            if (!filename.isEmpty()) {
                break;
            }
        }
        for (List<Object> pattern : set) {
            List<String> target = file;
            if (options.isMatchBase() && pattern.size() == 1) {
                target = Collections.singletonList(filename);
            }
            if (matchOne3(target, pattern)) {
                return options.isFlipNegate() || !negate;
            }
        }
        return !options.isFlipNegate() && negate;
    }

    private boolean matchOne3(List<String> file, List<Object> pattern) {
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
                        if (".".equals(rest) || "..".equals(rest) || (!options.isDot() && rest.startsWith("."))) {
                            return false;
                        }
                    }
                    return true;
                }
                while (fr < fl) {
                    String swallowee = file.get(fr);
                    if (matchOne3(file.subList(fr, fl), pattern.subList(pr, pl))) {
                        return true;
                    }
                    if (".".equals(swallowee) || "..".equals(swallowee) ||
                        (!options.isDot() && swallowee.startsWith("."))) {
                        break;
                    }
                    fr++;
                }
                return false;
            }
            boolean hit;
            if (p instanceof String) {
                hit = options.isNocase() ? f.equalsIgnoreCase((String) p) : f.equals(p);
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
        throw new JsError(JsError.ERROR, "wtf?");
    }

    // 10.2.5

    private static final int MAX_GLOBSTAR_RECURSION = 200;
    private static final Pattern EXTGLOB = Pattern.compile("[!?+*@]\\(");
    private static final Pattern RE_SPECIALS_10 = Pattern.compile("[-\\[\\]{}()*+?.,\\\\^$|#\\s]");
    private static final String STAR_10 = "[^/]*?";
    private static final String STAR_NO_EMPTY = "[^/]+?";
    private static final String QMARK_10 = "[^/]";
    private static final String START_NO_DOT = "(?!\\.)";
    private static final String START_NO_TRAVERSAL = "(?!(?:^|/)\\.\\.?(?:$|/))";

    private static List<String> levelOneOptimize(List<String> parts) {
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            String prev = out.isEmpty() ? null : out.get(out.size() - 1);
            if ("**".equals(part) && "**".equals(prev)) {
                continue;
            }
            if ("..".equals(part) && prev != null && !prev.isEmpty() && !"..".equals(prev) && !".".equals(prev) &&
                !"**".equals(prev)) {
                out.remove(out.size() - 1);
                continue;
            }
            out.add(part);
        }
        return out.isEmpty() ? new ArrayList<>(Collections.singletonList("")) : out;
    }

    private static List<String> slashSplit(String p) {
        return new ArrayList<>(Arrays.asList(SLASHES.split(p, -1)));
    }

    private Object parse10(String part) {
        if ("**".equals(part)) {
            return GLOBSTAR;
        }
        if (part.isEmpty()) {
            return "";
        }
        if (EXTGLOB.matcher(part).find()) {
            throw new UnsupportedPattern(pattern, "an extglob");
        }
        boolean noEmpty = true;
        boolean escaping = false;
        boolean hasMagic = false;
        StringBuilder re = new StringBuilder();
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            if (escaping) {
                escaping = false;
                re.append(RE_SPECIALS_10.matcher(String.valueOf(c)).matches() ? "\\" : "").append(c);
                continue;
            }
            if (c == '\\') {
                if (i == part.length() - 1) {
                    re.append("\\\\");
                } else {
                    escaping = true;
                }
                continue;
            }
            if (c == '[') {
                ParsedClass cls = parseClass(part, i);
                if (cls.consumed > 0) {
                    re.append(cls.src);
                    i += cls.consumed - 1;
                    hasMagic |= cls.magic;
                    continue;
                }
            }
            if (c == '*') {
                re.append(noEmpty && "*".equals(part) ? STAR_NO_EMPTY : STAR_10);
                hasMagic = true;
                continue;
            }
            if (c == '?') {
                re.append(QMARK_10);
                hasMagic = true;
                continue;
            }
            re.append(regExpEscape(String.valueOf(c)));
        }
        String src = re.toString();
        if (!hasMagic) {
            return unescape(src);
        }
        String start = "";
        boolean dotTravAllowed = ".".equals(part) || "..".equals(part);
        if (!dotTravAllowed) {
            boolean needNoTrav = (options.isDot() && isPatternStart(src, 0)) ||
                                 (src.startsWith("\\.") && isPatternStart(src, 2)) ||
                                 (src.startsWith("\\.\\.") && isPatternStart(src, 4));
            boolean needNoDot = !options.isDot() && isPatternStart(src, 0);
            start = needNoTrav ? START_NO_TRAVERSAL : needNoDot ? START_NO_DOT : "";
        }
        return Pattern.compile("^" + start + src + "$");
    }

    private static boolean isPatternStart(String src, int at) {
        return at < src.length() && (src.charAt(at) == '[' || src.charAt(at) == '.');
    }

    private static String regExpEscape(String s) {
        return RE_SPECIALS_10.matcher(s).replaceAll("\\\\$0");
    }

    private static String unescape(String s) {
        return s.replaceAll("((?!\\\\).|^)\\[([^/\\\\])]", "$1$2").replaceAll("\\\\([^/])", "$1");
    }

    private static final class ParsedClass {
        final String src;
        final int consumed;
        final boolean magic;

        ParsedClass(String src, int consumed, boolean magic) {
            this.src = src;
            this.consumed = consumed;
            this.magic = magic;
        }
    }

    private static final String[][] POSIX_CLASSES = {
            {"[:alnum:]", "\\p{L}\\p{Nl}\\p{Nd}", "+"},
            {"[:alpha:]", "\\p{L}\\p{Nl}", "+"},
            {"[:ascii:]", "\\x00-\\x7f", "+"},
            {"[:blank:]", "\\p{Zs}\\t", "+"},
            {"[:cntrl:]", "\\p{Cc}", "+"},
            {"[:digit:]", "\\p{Nd}", "+"},
            {"[:graph:]", "\\p{Z}\\p{C}", "-"},
            {"[:lower:]", "\\p{Ll}", "+"},
            {"[:print:]", "\\p{C}", "+"},
            {"[:punct:]", "\\p{P}", "+"},
            {"[:space:]", "\\p{Z}\\t\\r\\n\\x0B\\f", "+"},
            {"[:upper:]", "\\p{Lu}", "+"},
            {"[:word:]", "\\p{L}\\p{Nl}\\p{Nd}\\p{Pc}", "+"},
            {"[:xdigit:]", "A-Fa-f0-9", "+"},
    };

    private static String braceEscape(String s) {
        return s.replaceAll("[\\[\\]\\\\-]", "\\\\$0");
    }

    private static ParsedClass parseClass(String glob, int pos) {
        List<String> ranges = new ArrayList<>();
        List<String> negs = new ArrayList<>();
        int i = pos + 1;
        boolean sawStart = false;
        boolean escaping = false;
        boolean negate = false;
        int endPos = pos;
        String rangeStart = "";
        outer:
        while (i < glob.length()) {
            char c = glob.charAt(i);
            if ((c == '!' || c == '^') && i == pos + 1) {
                negate = true;
                i++;
                continue;
            }
            if (c == ']' && sawStart && !escaping) {
                endPos = i + 1;
                break;
            }
            sawStart = true;
            if (c == '\\' && !escaping) {
                escaping = true;
                i++;
                continue;
            }
            if (c == '[' && !escaping) {
                for (String[] cls : POSIX_CLASSES) {
                    if (glob.startsWith(cls[0], i)) {
                        if (!rangeStart.isEmpty()) {
                            return new ParsedClass("$.", glob.length() - pos, true);
                        }
                        i += cls[0].length();
                        if ("-".equals(cls[2])) {
                            negs.add(cls[1]);
                        } else {
                            ranges.add(cls[1]);
                        }
                        continue outer;
                    }
                }
            }
            escaping = false;
            String ch = String.valueOf(c);
            if (!rangeStart.isEmpty()) {
                if (ch.compareTo(rangeStart) > 0) {
                    ranges.add(braceEscape(rangeStart) + "-" + braceEscape(ch));
                } else if (ch.equals(rangeStart)) {
                    ranges.add(braceEscape(ch));
                }
                rangeStart = "";
                i++;
                continue;
            }
            if (glob.startsWith("-]", i + 1)) {
                ranges.add(braceEscape(ch + "-"));
                i += 2;
                continue;
            }
            if (glob.startsWith("-", i + 1)) {
                rangeStart = ch;
                i += 2;
                continue;
            }
            ranges.add(braceEscape(ch));
            i++;
        }
        if (endPos < i) {
            return new ParsedClass("", 0, false);
        }
        if (ranges.isEmpty() && negs.isEmpty()) {
            return new ParsedClass("$.", glob.length() - pos, true);
        }
        if (negs.isEmpty() && ranges.size() == 1 && ranges.get(0).matches("\\\\?.") && !negate) {
            String r = ranges.get(0).length() == 2 ? ranges.get(0).substring(1) : ranges.get(0);
            return new ParsedClass(regExpEscape(r), endPos - pos, false);
        }
        String sranges = "[" + (negate ? "^" : "") + String.join("", ranges) + "]";
        String snegs = "[" + (negate ? "" : "^") + String.join("", negs) + "]";
        String comb = !ranges.isEmpty() && !negs.isEmpty() ? "(" + sranges + "|" + snegs + ")" :
                !ranges.isEmpty() ? sranges : snegs;
        return new ParsedClass(comb, endPos - pos, true);
    }

    private boolean match10(String f, boolean partial) {
        if ("/".equals(f) && partial) {
            return true;
        }
        List<String> ff = slashSplit(f);
        for (List<Object> pattern : set) {
            if (matchOne10(ff, pattern, partial)) {
                return !negate;
            }
        }
        return negate;
    }

    private boolean matchOne10(List<String> file, List<Object> pattern, boolean partial) {
        if (pattern.contains(GLOBSTAR)) {
            return matchGlobstar(file, pattern, partial, 0, 0);
        }
        return matchOne10(file, pattern, partial, 0, 0);
    }

    private static <T> List<T> slice(List<T> list, int from, int to) {
        int end = Math.min(list.size(), to);
        return list.subList(Math.min(from, end), end);
    }

    private boolean badDot(String f) {
        return ".".equals(f) || "..".equals(f) || (!options.isDot() && f.startsWith("."));
    }

    private boolean matchGlobstar(List<String> file, List<Object> pattern, boolean partial, int fileIndex,
                                  int patternIndex) {
        int firstgs = patternIndex;
        while (pattern.get(firstgs) != GLOBSTAR) {
            firstgs++;
        }
        int lastgs = pattern.lastIndexOf(GLOBSTAR);
        List<Object> head = slice(pattern, patternIndex, firstgs);
        List<Object> body = partial ? slice(pattern, firstgs + 1, pattern.size()) : slice(pattern, firstgs + 1, lastgs);
        List<Object> tail = partial ? new ArrayList<>() : slice(pattern, lastgs + 1, pattern.size());

        if (!head.isEmpty()) {
            List<String> fileHead = slice(file, fileIndex, fileIndex + head.size());
            if (!matchOne10(fileHead, head, partial, 0, 0)) {
                return false;
            }
            fileIndex += head.size();
        }

        int fileTailMatch = 0;
        if (!tail.isEmpty()) {
            if (tail.size() + fileIndex > file.size()) {
                return false;
            }
            int tailStart = file.size() - tail.size();
            if (matchOne10(file, tail, partial, tailStart, 0)) {
                fileTailMatch = tail.size();
            } else {
                if (!file.get(file.size() - 1).isEmpty() || fileIndex + tail.size() == file.size()) {
                    return false;
                }
                tailStart--;
                if (!matchOne10(file, tail, partial, tailStart, 0)) {
                    return false;
                }
                fileTailMatch = tail.size() + 1;
            }
        }

        if (body.isEmpty()) {
            boolean sawSome = fileTailMatch != 0;
            for (int i = fileIndex; i < file.size() - fileTailMatch; i++) {
                sawSome = true;
                if (badDot(file.get(i))) {
                    return false;
                }
            }
            return partial || sawSome;
        }

        List<List<Object>> bodySegments = new ArrayList<>();
        List<Integer> nonGsPartsSums = new ArrayList<>();
        List<Object> currentBody = new ArrayList<>();
        bodySegments.add(currentBody);
        int nonGsParts = 0;
        nonGsPartsSums.add(0);
        for (Object b : body) {
            if (b == GLOBSTAR) {
                nonGsPartsSums.add(nonGsParts);
                currentBody = new ArrayList<>();
                bodySegments.add(currentBody);
            } else {
                currentBody.add(b);
                nonGsParts++;
            }
        }
        int[] after = new int[bodySegments.size()];
        int i = bodySegments.size() - 1;
        int fileLength = file.size() - fileTailMatch;
        for (int s = 0; s < bodySegments.size(); s++) {
            after[s] = fileLength - (nonGsPartsSums.get(i--) + bodySegments.get(s).size());
        }
        return Boolean.TRUE.equals(matchGlobStarBodySections(file, bodySegments, after, fileIndex, 0, partial, 0,
                                                             fileTailMatch != 0));
    }

    private @Nullable Boolean matchGlobStarBodySections(List<String> file, List<List<Object>> bodySegments, int[] after,
                                                       int fileIndex, int bodyIndex, boolean partial, int globStarDepth,
                                                       boolean sawTail) {
        if (bodyIndex >= bodySegments.size()) {
            for (int i = fileIndex; i < file.size(); i++) {
                sawTail = true;
                if (badDot(file.get(i))) {
                    return false;
                }
            }
            return sawTail;
        }
        List<Object> body = bodySegments.get(bodyIndex);
        int last = after[bodyIndex];
        while (fileIndex <= last) {
            boolean m = matchOne10(slice(file, 0, fileIndex + body.size()), body, partial, fileIndex, 0);
            if (m && globStarDepth < MAX_GLOBSTAR_RECURSION) {
                Boolean sub = matchGlobStarBodySections(file, bodySegments, after, fileIndex + body.size(),
                        bodyIndex + 1, partial, globStarDepth + 1, sawTail);
                if (!Boolean.FALSE.equals(sub)) {
                    return sub;
                }
            }
            if (fileIndex < file.size() && badDot(file.get(fileIndex))) {
                return false;
            }
            fileIndex++;
        }
        return partial ? Boolean.TRUE : null;
    }

    private boolean matchOne10(List<String> file, List<Object> pattern, boolean partial, int fileIndex,
                               int patternIndex) {
        int fi = fileIndex;
        int pi = patternIndex;
        int fl = file.size();
        int pl = pattern.size();
        for (; fi < fl && pi < pl; fi++, pi++) {
            Object p = pattern.get(pi);
            String f = file.get(fi);
            if (p == GLOBSTAR) {
                return false;
            }
            boolean hit = p instanceof String ? f.equals(p) : ((Pattern) p).matcher(f).find();
            if (!hit) {
                return false;
            }
        }
        if (fi == fl && pi == pl) {
            return true;
        } else if (fi == fl) {
            return partial;
        } else if (pi == pl) {
            return fi == fl - 1 && file.get(fi).isEmpty();
        }
        throw new JsError(JsError.ERROR, "wtf?");
    }
}
