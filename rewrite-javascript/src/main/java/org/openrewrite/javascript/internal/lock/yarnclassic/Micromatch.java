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
import org.openrewrite.javascript.internal.lock.Braces;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.IsExtglob;
import org.openrewrite.javascript.internal.lock.IsGlob;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.V8RegExp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.javascript.internal.lock.V8RegExp.DOT;

/**
 * Port of micromatch 2.3.11 (index.js, lib/expand.js, lib/glob.js, lib/utils.js, lib/chars.js) with arr-diff 2.0.0
 * on POSIX paths, as yarn 1.22.22 bundles them, for calls with no options.
 */
final class Micromatch {

    private static final String QMARK = "[^/]";
    private static final String STAR = QMARK + "*?";
    private static final String NODOT = "(?!\\.)(?=.)";
    private static final String DOTFILE_GLOB = "(?:\\/|^)\\.{1,2}($|\\/)";
    private static final String DOTFILES = "(?!" + DOTFILE_GLOB + ")(?=.)";
    private static final String TWO_STAR_DOT = "(?:(?!" + DOTFILE_GLOB + ").)*?";

    private static final Pattern PATH_CHARS = Pattern.compile("([/.])");
    private static final Pattern PLUS = Pattern.compile("(\\+)(?!\\()");
    private static final Pattern BRACE_DOT = Pattern.compile("[{,]\\.");
    private static final Pattern EMPTY_BRACKETS = Pattern.compile("\\[]");
    private static final Pattern DRIVE = Pattern.compile("^(\\w):([\\\\/]+?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STARS = Pattern.compile("\\*{2,}");
    private static final Pattern WORD_STAR = Pattern.compile("(\\w+)\\*(?!/)");
    private static final Pattern GLOBSTAR_STAR_WORD = Pattern.compile("\\*\\*/\\*(\\w)");
    private static final Pattern GLOBSTAR_ANY = Pattern.compile("\\*\\*/(" + V8RegExp.DOT + ")");
    private static final Pattern GLOBSTAR_COMMA = Pattern.compile(",\\*\\*|\\*\\*,");
    private static final Pattern TRAILING_SLASH_STAR = Pattern.compile("/\\*\\z");
    private static final Pattern TRAILING_STAR = Pattern.compile("(?!/)\\*\\z");
    private static final Pattern PARTIAL_STAR = Pattern.compile("([^/]+)\\*");
    private static final Pattern QMARKS = Pattern.compile("\\?+");
    private static final Pattern DOT_WORD = Pattern.compile("\\.([*\\w]+)");
    private static final Pattern NOT_SLASH_CLASS = Pattern.compile("\\[\\^[\\\\/]+]");
    private static final Pattern SLASHES = Pattern.compile("/+");
    private static final Pattern FIVE_DIGITS = Pattern.compile("[0-9]{5}");
    private static final Pattern SPELLED_ID = Pattern.compile("([0-9]{0,4})__");
    private static final Pattern BACKSLASHES = Pattern.compile("\\\\{2,}");
    private static final Pattern LEADING_CLASS_OR_WILDCARD = Pattern.compile("^[\\[?*]");
    private static final Pattern ESCAPED = Pattern.compile("[\"\\\\](['\"]?[^\"'\\\\]['\"]?)");
    private static final Pattern LETTER = Pattern.compile("[a-z]", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNESCAPE = Pattern.compile("__([A-Z]+)_([A-Z]+)__");
    private static final Pattern NEGATED_CLASS = Pattern.compile("\\[\\^([^\\]]*?)]");
    private static final Pattern OPENING = Pattern.compile("[{(\\[]");
    private static final Pattern CLOSING = Pattern.compile("[})\\]]");

    private static final Map<String, String> ESC = new HashMap<>();
    private static final Map<String, String> UNESC = new HashMap<>();
    private static final Map<String, String> TEMP = new HashMap<>();

    static {
        String[][] chars = {{"?", "QMRK"}, {"@", "AMPE"}, {"!", "EXCL"}, {"+", "PLUS"}, {"*", "STAR"},
                {",", "COMMA"}, {"(", "LTPAREN"}, {")", "RTPAREN"}, {"[", "LTBRACK"}, {"]", "RTBRACK"}};
        for (String[] ch : chars) {
            ESC.put(ch[0], "__UNESC_" + ch[1] + "__");
            UNESC.put("__UNESC_" + ch[1] + "__", "\\" + ch[0]);
            TEMP.put("__TEMP_" + ch[1] + "__", ch[0]);
        }
    }

    private static final Pattern MATCH_NOTHING = Objects.requireNonNull(V8RegExp.compile("$^"));

    private final ParseGlob parseGlob = new ParseGlob();
    private final Braces braces = new Braces();

    /**
     * {@code micromatch(files, patterns)}: the files a pattern matches, less those a {@code !} pattern does.
     * @throws JsError micromatch's TypeError for a pattern that is not a string.
     */
    static List<String> micromatch(List<String> files, @Nullable Object patterns) {
        if (!Js.truthy(patterns)) {
            return new ArrayList<>();
        }
        Micromatch mm = new Micromatch();
        if (!(patterns instanceof List)) {
            return mm.match(files, patterns);
        }
        List<String> omit = new ArrayList<>();
        List<String> keep = new ArrayList<>();
        for (Object glob : (List<?>) patterns) {
            if (glob instanceof String && ((String) glob).startsWith("!")) {
                omit.addAll(mm.match(files, ((String) glob).substring(1)));
            } else {
                keep.addAll(mm.match(files, glob));
            }
        }
        return diff(keep, omit);
    }

    List<String> match(List<String> files, @Nullable Object pattern) {
        boolean negate = false;
        if (pattern instanceof String && ((String) pattern).startsWith("!")) {
            negate = true;
            pattern = ((String) pattern).substring(1);
        }
        Predicate<String> isMatch = matcher(pattern);
        List<String> res = new ArrayList<>();
        for (String file : files) {
            if (isMatch.test(file)) {
                res.add(file);
            }
        }
        return negate ? diff(files, res) : res;
    }

    Predicate<String> matcher(@Nullable Object pattern) {
        if (!(pattern instanceof String)) {
            throw new JsError(JsError.TYPE_ERROR, "micromatch.matcher(): pattern should be a string, regex, or " +
                                                  "function.");
        }
        String glob = (String) pattern;
        if (!IsGlob.isGlob(glob, IsGlob.Release.V2_0_1)) {
            return glob::equals;
        }
        String source = makeRe(glob);
        Pattern re = V8RegExp.compile(source);
        List<String> ids = braces.preservedIds(source);
        return fp -> {
            boolean matches = find(re, fp);
            if (!ids.isEmpty() && !sameForEveryDraw(source, ids, fp, matches)) {
                throw new CannotKnow("whether " + fp + " matches " + glob + ", whose regex holds an id " +
                                     "micromatch 2.3.11 draws at random");
            }
            return matches;
        };
    }

    private static boolean find(@Nullable Pattern re, String fp) {
        return (re == null ? MATCH_NOTHING : re).matcher(fp).find();
    }

    /**
     * Whether a path matches alike for every id preserve can draw ({@code Math.random().toString().slice(2, 7)}),
     * trying one id for each way an id can decide a match ({@link #draws}).
     */
    private static boolean sameForEveryDraw(String source, List<String> ids, String fp, boolean matches) {
        List<List<String>> draws = new ArrayList<>();
        for (String id : ids) {
            draws.add(draws(source, "__ID" + id + "__", fp));
        }
        Pattern token = Pattern.compile("__ID(" + String.join("|", ids) + ")__");
        int[] draw = new int[ids.size()];
        do {
            Matcher m = token.matcher(source);
            StringBuffer drawn = new StringBuffer();
            while (m.find()) {
                int i = ids.indexOf(m.group(1));
                m.appendReplacement(drawn, Matcher.quoteReplacement("__ID" + draws.get(i).get(draw[i]) + "__"));
            }
            m.appendTail(drawn);
            if (find(V8RegExp.compile(drawn.toString()), fp) != matches) {
                return false;
            }
        } while (next(draw, draws));
        return true;
    }

    /**
     * The ids that decide a match differently: a drawn id is a literal the path may spell after {@code __ID} (each it
     * spells, one it does not), and inside a class only which of the path's digits it holds counts.
     */
    private static List<String> draws(String source, String token, String fp) {
        Set<String> spelled = new LinkedHashSet<>();
        for (int at = fp.indexOf("__ID"); at >= 0; at = fp.indexOf("__ID", at + 1)) {
            Matcher id = SPELLED_ID.matcher(fp.substring(at + 4));
            if (id.lookingAt() && (id.group(1).length() == 5 || isShortId(id.group(1)))) {
                spelled.add(id.group(1));
            }
            if (fp.length() >= at + 9 && FIVE_DIGITS.matcher(fp.substring(at + 4, at + 9)).matches()) {
                spelled.add(fp.substring(at + 4, at + 9));
            }
        }
        StringBuilder digits = new StringBuilder();
        char lacked = 0;
        for (char c = '0'; c <= '9'; c++) {
            if (fp.indexOf(c) >= 0) {
                digits.append(c);
            } else if (lacked == 0) {
                lacked = c;
            }
        }
        List<String> draws = new ArrayList<>(spelled);
        if (inClass(source, token)) {
            for (int set = 0; set < 1 << digits.length(); set++) {
                if (Integer.bitCount(set) <= 5) {
                    StringBuilder held = new StringBuilder();
                    for (int i = 0; i < digits.length(); i++) {
                        if ((set & 1 << i) != 0) {
                            held.append(digits.charAt(i));
                        }
                    }
                    addUnspelled(draws, spelled, held.toString(), lacked == 0 ? held.toString() : held.toString() +
                                                                                              lacked);
                }
            }
            if (!spelled.contains("")) {
                draws.add("");
            }
        } else {
            addUnspelled(draws, spelled, "", "0123456789");
        }
        return draws;
    }

    /**
     * Whether a Math.random() value V8 can draw, a multiple of 2^-52, prints as {@code 0.<digits>} with four decimals
     * or fewer (3342 values; none prints in exponent form): preserve's id is then shorter than five digits.
     */
    static boolean isShortId(String digits) {
        if (digits.isEmpty()) {
            return true;
        }
        double x = Double.parseDouble("0." + digits);
        double lattice = Math.scalb(x, 52);
        return lattice == Math.rint(lattice) && ("0." + digits).equals(Js.numberToString(x));
    }

    // five digits of the pool that hold every digit of `held`, unlike any the path spells
    private static void addUnspelled(List<String> draws, Set<String> spelled, String held, String pool) {
        if (pool.isEmpty()) {
            return;
        }
        int[] at = new int[5];
        do {
            StringBuilder id = new StringBuilder();
            for (int i : at) {
                id.append(pool.charAt(i));
            }
            String s = id.toString();
            boolean holdsAll = true;
            for (int i = 0; i < held.length(); i++) {
                holdsAll &= s.indexOf(held.charAt(i)) >= 0;
            }
            if (holdsAll && !spelled.contains(s)) {
                draws.add(s);
                return;
            }
        } while (next(at, pool.length()));
    }

    // whether the token sits inside a character class of the source, as V8 reads `[` and `]`
    private static boolean inClass(String source, String token) {
        boolean inClass = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (inClass && (source.startsWith(token, i) || c == '\\' && source.startsWith(token, i + 1))) {
                return true;
            }
            if (c == '\\') {
                i++;
            } else if (!inClass && c == '[') {
                inClass = true;
            } else if (inClass && c == ']') {
                inClass = false;
            }
        }
        return false;
    }

    private static boolean next(int[] draw, List<List<String>> draws) {
        for (int i = draw.length - 1; i >= 0; i--) {
            if (++draw[i] < draws.get(i).size()) {
                return true;
            }
            draw[i] = 0;
        }
        return false;
    }

    private static boolean next(int[] draw, int base) {
        for (int i = draw.length - 1; i >= 0; i--) {
            if (++draw[i] < base) {
                return true;
            }
            draw[i] = 0;
        }
        return false;
    }

    /**
     * The source of the RegExp micromatch builds for a glob, as {@code toRegex} hands it to {@code new RegExp}.
     */
    String makeRe(String glob) {
        Options opts = new Options();
        Glob parsed = expand(glob, opts);
        opts.negated = opts.negated || parsed.negated;
        return wrapGlob(parsed.pattern, opts.negated);
    }

    private static String wrapGlob(String glob, boolean negate) {
        glob = "(?:" + glob + ")$";
        if (negate) {
            return "^(?!^" + glob + ").*$";
        }
        return "^" + glob;
    }

    // the options expand reads and sets on the object toRegex makes for each glob
    private static final class Options {
        boolean dot;
        boolean makeRe;
        boolean negated;
    }

    // lib/glob.js
    private static final class Glob {
        String pattern;
        final String orig;
        boolean negated;

        Glob(String pattern) {
            this.pattern = pattern;
            this.orig = pattern;
            if (pattern.startsWith("!")) {
                this.pattern = pattern.substring(1);
                this.negated = true;
            }
        }

        void replace(String a, String b, boolean escape) {
            pattern = Js.splitJoin(pattern, a, escape ? esc(b) : b);
        }

        void replace(Pattern a, boolean global, String b, boolean escape) {
            pattern = V8RegExp.replace(pattern, a, global, escape ? esc(b) : b);
        }

        void replace(Pattern a, boolean global, Function<Matcher, String> b) {
            pattern = V8RegExp.replace(pattern, a, global, b);
        }

        void escape(String str) {
            pattern = V8RegExp.replace(str, ESCAPED, true, m -> {
                String ch = ESC.get(m.group(1));
                if (ch != null) {
                    return ch;
                }
                if (LETTER.matcher(m.group()).find()) {
                    return Js.splitJoin(m.group(), "\\", "");
                }
                return m.group();
            });
        }

        void unescape(String str) {
            pattern = V8RegExp.replace(str, UNESCAPE, true, m -> {
                Map<String, String> chars = "ESC".equals(m.group(1)) ? ESC : "UNESC".equals(m.group(1)) ? UNESC :
                        "TEMP".equals(m.group(1)) ? TEMP : null;
                if (chars == null) {
                    throw new JsError(JsError.TYPE_ERROR, "Cannot read properties of undefined (reading '" +
                                                          m.group() + "')");
                }
                String ch = chars.get(m.group());
                return ch == null ? "undefined" : ch;
            });
            pattern = unesc(pattern);
        }

        private static String esc(String str) {
            return Js.splitJoin(Js.splitJoin(str, "?", "%~"), "*", "%%");
        }

        private static String unesc(String str) {
            return Js.splitJoin(Js.splitJoin(str, "%~", "?"), "%%", "*");
        }
    }

    // lib/expand.js
    private Glob expand(String pattern, Options opts) {
        Glob glob = new Glob(pattern);
        opts.makeRe = true;

        if (!IsGlob.isGlob(pattern, IsGlob.Release.V2_0_1)) {
            glob.pattern = V8RegExp.replace(glob.pattern, PATH_CHARS, true, m -> "\\" + m.group(1));
            return glob;
        }

        glob.pattern = V8RegExp.replace(glob.pattern, PLUS, true, m -> "\\" + m.group(1));
        glob.pattern = Js.splitJoin(glob.pattern, "$", "\\$");

        if (".*".equals(glob.pattern)) {
            return literal(pattern, "\\." + STAR);
        }
        if ("*".equals(glob.pattern)) {
            return literal(pattern, oneStar(opts.dot));
        }

        ParseGlob.Tokens tok = parseGlob.parseGlob(glob.pattern);

        if (tok.dotfile) {
            opts.dot = true;
        }
        if (tok.dotdir) {
            opts.dot = true;
        }
        if (BRACE_DOT.matcher(glob.pattern).find()) {
            opts.makeRe = false;
            opts.dot = true;
        }
        opts.negated = glob.negated;

        if (glob.pattern.startsWith(".") && !glob.pattern.startsWith("./")) {
            glob.pattern = "\\" + glob.pattern;
        }

        if (tok.braces) {
            Matcher a = OPENING.matcher(glob.pattern);
            Matcher b = CLOSING.matcher(glob.pattern);
            int opening = 0;
            while (a.find()) {
                opening++;
            }
            int closing = 0;
            while (b.find()) {
                closing++;
            }
            if (opening > 0 && closing > 0 && opening != closing) {
                opts.makeRe = false;
            }
            Braces.Options braceOptions = Braces.Options.builder(Braces.Release.V1_8_5).makeRe(opts.makeRe).build();
            glob.pattern = String.join("|", braces.braces(glob.pattern, braceOptions));
        }

        if (tok.extglob && IsExtglob.isExtglob(glob.pattern, IsExtglob.Release.V1_0_0)) {
            glob.pattern = Extglob.extglob(glob.pattern);
        }

        if (tok.brackets) {
            glob.pattern = ExpandBrackets.brackets(glob.pattern);
        }

        String leading = opts.dot ? DOTFILES : NODOT;
        glob.replace("[!", "[^", false);
        glob.replace("(?", "(%~", false);
        glob.replace(EMPTY_BRACKETS, false, "\\[\\]", false);
        glob.replace("/[", "/" + leading + "[", true);
        glob.replace("/?", "/" + leading + "[^/]", true);
        glob.replace("/.", "/(?=.)\\.", true);

        glob.replace(DRIVE, true, "(?=.)$1:$2", true);

        if (glob.pattern.contains("[^")) {
            glob.pattern = negateSlash(glob.pattern);
        }

        if ("**".equals(glob.pattern)) {
            glob.pattern = globstar(opts.dot);
        } else {
            glob.pattern = balance(glob.pattern);
            glob.escape(glob.pattern);

            if (tok.globstar) {
                glob.pattern = collapse(glob.pattern, "/**");
                glob.pattern = collapse(glob.pattern, "**/");
                glob.replace("/**/", "(?:/" + globstar(opts.dot) + "/|/)", true);
                glob.replace(STARS, true, "**", false);

                glob.replace(WORD_STAR, true, "$1[^/]*?", true);
                glob.replace(GLOBSTAR_STAR_WORD, true, globstar(opts.dot) + "\\/" + leading(opts) + "[^/]*?$1",
                        true);

                if (!opts.dot) {
                    glob.replace(GLOBSTAR_ANY, true, "(?:**\\/|)$1", false);
                }

                if (!tok.dirname.isEmpty() || GLOBSTAR_COMMA.matcher(glob.orig).find()) {
                    glob.replace("**", globstar(opts.dot), true);
                }
            }

            glob.replace(TRAILING_SLASH_STAR, false, "\\/" + oneStar(opts.dot), true);
            glob.replace(TRAILING_STAR, false, STAR, true);
            glob.replace(PARTIAL_STAR, false, "$1" + oneStar(true), true);
            glob.replace("*", oneStar(opts.dot), true);
            glob.replace("?.", "?\\.", true);
            glob.replace("?:", "?:", true);

            glob.replace(QMARKS, true, m -> m.group().length() == 1 ? QMARK :
                    QMARK + "{" + m.group().length() + "}");

            glob.replace(DOT_WORD, true, "\\.$1", false);
            glob.replace(NOT_SLASH_CLASS, true, QMARK, false);
            glob.replace(SLASHES, true, "\\/", false);
            glob.replace(BACKSLASHES, true, "\\", false);
        }

        glob.unescape(glob.pattern);
        glob.replace("__UNESC_STAR__", "*", false);

        glob.replace("?.", "?\\.", false);

        glob.replace("[^\\/]", QMARK, false);

        if (glob.pattern.length() > 1 && LEADING_CLASS_OR_WILDCARD.matcher(glob.pattern).find()) {
            glob.pattern = leading(opts) + glob.pattern;
        }

        return glob;
    }

    private static String leading(Options opts) {
        return opts.dot ? DOTFILES : NODOT;
    }

    // expand's early return for `.*` and `*`, which carries no negation
    private static Glob literal(String orig, String pattern) {
        Glob glob = new Glob(orig);
        glob.pattern = pattern;
        glob.negated = false;
        return glob;
    }

    private static String collapse(String str, String ch) {
        String[] res = Js.splitString(str, ch);
        boolean isFirst = res[0].isEmpty();
        boolean isLast = res[res.length - 1].isEmpty();
        List<String> kept = new ArrayList<>();
        if (isFirst) {
            kept.add("");
        }
        for (String part : res) {
            if (!part.isEmpty()) {
                kept.add(part);
            }
        }
        if (isLast) {
            kept.add("");
        }
        return String.join(ch, kept);
    }

    private static String negateSlash(String str) {
        return V8RegExp.replace(str, NEGATED_CLASS, true, m -> {
            String inner = m.group(1);
            if (!inner.contains("/")) {
                inner = "\\/" + inner;
            }
            return "[^" + inner + "]";
        });
    }

    // escapes every bracket when the pattern holds unequal numbers of [ and ]
    private static String balance(String str) {
        String[] aarr = Js.splitString(str, "[");
        int alen = String.join("", aarr).length();
        int blen = Js.splitJoin(str, "]", "").length();
        if (alen != blen) {
            str = String.join("\\[", aarr);
            return Js.splitJoin(str, "]", "\\]");
        }
        return str;
    }

    private static String oneStar(boolean dotfile) {
        return dotfile ? "(?!" + DOTFILE_GLOB + ")(?=.)" + STAR : NODOT + STAR;
    }

    private static String globstar(boolean dotfile) {
        if (dotfile) {
            return TWO_STAR_DOT;
        }
        return "(?:(?!(?:\\/|^)\\.).)*?";
    }

    // arr-diff 2.0.0
    private static List<String> diff(List<String> arr, List<String> other) {
        List<String> res = new ArrayList<>();
        for (String item : arr) {
            if (!other.contains(item)) {
                res.add(item);
            }
        }
        return res;
    }
}
