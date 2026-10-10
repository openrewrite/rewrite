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
package org.openrewrite.javascript.internal.lock.npm;

import lombok.Builder;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.openrewrite.javascript.internal.lock.JsError;

/** Port of minimatch 10.2.5 on POSIX paths, without extglobs, which no workspace pattern uses. */
final class Minimatch {

    @Value
    @Builder
    static class Options {
        static final Options DEFAULT = Options.builder().build();

        boolean dot;
        boolean partial;
        boolean nocomment;
        boolean nonegate;
        boolean windowsPathsNoEscape;
    }

    /**
     * A pattern with syntax this port does not implement.
     */
    static final class UnsupportedPattern extends IllegalArgumentException {
        UnsupportedPattern(String pattern, String what) {
            super("The workspace pattern " + Js.quote(pattern) + " uses " + what + ", which is not supported");
        }
    }

    private static final Object GLOBSTAR = new Object() {
        @Override
        public String toString() {
            return "**";
        }
    };

    private static final int MAX_GLOBSTAR_RECURSION = 200;
    private static final Pattern SLASHES = Pattern.compile("/+");
    private static final Pattern HAS_BRACE = Pattern.compile("\\{(?:(?!\\{).)*}");
    private static final Pattern EXTGLOB = Pattern.compile("[!?+*@]\\(");
    private static final Pattern RE_SPECIALS = Pattern.compile("[-\\[\\]{}()*+?.,\\\\^$|#\\s]");
    private static final String STAR = "[^/]*?";
    private static final String STAR_NO_EMPTY = "[^/]+?";
    private static final String QMARK = "[^/]";
    private static final String START_NO_DOT = "(?!\\.)";
    private static final String START_NO_TRAVERSAL = "(?!(?:^|/)\\.\\.?(?:$|/))";

    private final Options options;
    private String pattern;
    private boolean negate;
    private boolean comment;
    private boolean empty;
    private final List<List<Object>> set = new ArrayList<>();

    Minimatch(String pattern, Options options) {
        this.options = options;
        this.pattern = options.isWindowsPathsNoEscape() ? pattern.replace('\\', '/') : pattern;
        make();
    }

    static boolean minimatch(String path, String pattern, Options options) {
        return new Minimatch(pattern, options).match(path);
    }

    static List<String> match(List<String> list, String pattern, Options options) {
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
        Set<String> globSet = new LinkedHashSet<>(braceExpand(pattern));
        for (String glob : globSet) {
            List<String> parts = levelOneOptimize(slashSplit(glob));
            List<Object> compiled = new ArrayList<>();
            for (String part : parts) {
                compiled.add(parse(part));
            }
            set.add(compiled);
        }
    }

    private static List<String> braceExpand(String pattern) {
        if (!HAS_BRACE.matcher(pattern).find()) {
            return Arrays.asList(pattern);
        }
        return BraceExpansion.expand(pattern);
    }

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
        return out.isEmpty() ? new ArrayList<>(Arrays.asList("")) : out;
    }

    private void parseNegate() {
        if (options.isNonegate()) {
            return;
        }
        int negateOffset = 0;
        boolean negate = false;
        for (int i = 0; i < pattern.length() && pattern.charAt(i) == '!'; i++) {
            negate = !negate;
            negateOffset++;
        }
        if (negateOffset > 0) {
            pattern = pattern.substring(negateOffset);
        }
        this.negate = negate;
    }

    private static List<String> slashSplit(String p) {
        return new ArrayList<>(Arrays.asList(SLASHES.split(p, -1)));
    }

    private Object parse(String part) {
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
                re.append(RE_SPECIALS.matcher(String.valueOf(c)).matches() ? "\\" : "").append(c);
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
                re.append(noEmpty && "*".equals(part) ? STAR_NO_EMPTY : STAR);
                hasMagic = true;
                continue;
            }
            if (c == '?') {
                re.append(QMARK);
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
        return RE_SPECIALS.matcher(s).replaceAll("\\\\$0");
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

    boolean match(String f) {
        return match(f, options.isPartial());
    }

    boolean match(String f, boolean partial) {
        if (comment) {
            return false;
        }
        if (empty) {
            return f.isEmpty();
        }
        if ("/".equals(f) && partial) {
            return true;
        }
        List<String> ff = slashSplit(f);
        for (List<Object> pattern : set) {
            if (matchOne(ff, pattern, partial)) {
                return !negate;
            }
        }
        return negate;
    }

    boolean matchOne(List<String> file, List<Object> pattern, boolean partial) {
        if (pattern.contains(GLOBSTAR)) {
            return matchGlobstar(file, pattern, partial, 0, 0);
        }
        return matchOne(file, pattern, partial, 0, 0);
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
            if (!matchOne(fileHead, head, partial, 0, 0)) {
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
            if (matchOne(file, tail, partial, tailStart, 0)) {
                fileTailMatch = tail.size();
            } else {
                if (!file.get(file.size() - 1).isEmpty() || fileIndex + tail.size() == file.size()) {
                    return false;
                }
                tailStart--;
                if (!matchOne(file, tail, partial, tailStart, 0)) {
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
            boolean m = matchOne(slice(file, 0, fileIndex + body.size()), body, partial, fileIndex, 0);
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

    private boolean matchOne(List<String> file, List<Object> pattern, boolean partial, int fileIndex,
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
