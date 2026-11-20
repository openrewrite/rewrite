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

import org.openrewrite.javascript.internal.lock.GlobParent;
import org.openrewrite.javascript.internal.lock.IsExtglob;
import org.openrewrite.javascript.internal.lock.IsGlob;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.V8RegExp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of parse-glob 3.0.4 with glob-base 0.3.0 and is-dotfile 1.0.3, as yarn 1.22.22 bundles them for micromatch
 * 2.3.11.
 */
final class ParseGlob {

    private static final Pattern ENCLOSED = Pattern.compile("\\{([^{}]*?)}|\\(([^()]*?)\\)|\\[([^\\[\\]]*?)]");
    private static final Pattern BASENAME = Pattern.compile("([^/]*)\\z");

    static final class Tokens {
        String orig = "";
        String glob = "";
        String base = "";
        String dirname = "";
        String basename = "";
        String filename = "";
        String extname = "";
        String ext = "";
        boolean isGlob;
        boolean negated;
        boolean extglob;
        boolean braces;
        boolean brackets;
        boolean globstar;
        boolean dotfile;
        boolean dotdir;
    }

    // the module's cache: looked up by the pattern as given, stored under the escaped pattern
    private final Map<String, Tokens> cache = new HashMap<>();

    Tokens parseGlob(String glob) {
        Tokens cached = cache.get(glob);
        if (cached != null) {
            return cached;
        }
        Tokens tok = new Tokens();
        tok.orig = glob;

        glob = escape(glob);

        String[] parsed = globBase(glob);
        tok.isGlob = IsGlob.isGlob(glob, IsGlob.Release.V2_0_1);
        tok.glob = parsed[1];
        tok.base = parsed[0];
        Matcher segs = BASENAME.matcher(glob);
        segs.find();

        tok.basename = segs.group(1);
        tok.dirname = tok.basename.isEmpty() ? glob : glob.replace(tok.basename, "");
        String[] basename = Js.splitString(tok.basename, ".");
        tok.filename = basename[0];
        tok.extname = String.join(".", Arrays.copyOfRange(basename, 1, basename.length));
        tok.ext = "";

        if (IsGlob.isGlob(tok.dirname, IsGlob.Release.V2_0_1) && tok.basename.isEmpty()) {
            if (!tok.glob.endsWith("/")) {
                tok.basename = tok.glob;
            }
            tok.dirname = tok.base;
        }

        if (glob.indexOf('/') == -1) {
            tok.dirname = "";
            tok.basename = tok.orig;
        }

        int dot = tok.basename.indexOf('.');
        if (dot != -1) {
            tok.filename = tok.basename.substring(0, dot);
            tok.extname = tok.basename.substring(dot);
        }

        if (tok.extname.startsWith(".")) {
            String[] exts = Js.splitString(tok.extname, ".");
            tok.ext = exts[exts.length - 1];
        }

        tok.glob = unescape(tok.glob);
        tok.dirname = unescape(tok.dirname);
        tok.basename = unescape(tok.basename);
        tok.filename = unescape(tok.filename);
        tok.extname = unescape(tok.extname);

        boolean is = !glob.isEmpty() && tok.isGlob;
        tok.negated = !glob.isEmpty() && glob.charAt(0) == '!';
        tok.extglob = !glob.isEmpty() && IsExtglob.isExtglob(glob, IsExtglob.Release.V1_0_0);
        tok.braces = is && glob.contains("{");
        tok.brackets = is && glob.contains("[:");
        tok.globstar = is && glob.contains("**");
        tok.dotfile = isDotfile(tok.basename) || isDotfile(tok.filename);
        tok.dotdir = dotdir(tok.dirname);
        cache.put(glob, tok);
        return tok;
    }

    private static boolean dotdir(String base) {
        if (base.contains("/.")) {
            return true;
        }
        return base.startsWith(".") && !base.startsWith("./");
    }

    // glob-base: the directory part that holds no glob and the rest
    private static String[] globBase(String pattern) {
        String base = GlobParent.globParent(pattern, GlobParent.Release.V2_0_0);
        boolean isGlob = IsGlob.isGlob(pattern, IsGlob.Release.V2_0_1);
        String glob;
        if (!".".equals(base)) {
            glob = pattern.substring(Math.min(base.length(), pattern.length()));
            if (glob.startsWith("/")) {
                glob = glob.substring(1);
            }
        } else {
            glob = pattern;
        }
        if (!isGlob) {
            base = pattern.endsWith("/") ? pattern : PosixPath.dirname(pattern);
            glob = !".".equals(base) ? pattern.substring(Math.min(base.length(), pattern.length())) : pattern;
        }
        if (glob.startsWith("./")) {
            glob = glob.substring(2);
        }
        if (glob.startsWith("/")) {
            glob = glob.substring(1);
        }
        return new String[]{base, glob};
    }

    static boolean isDotfile(String str) {
        if (!str.isEmpty() && str.charAt(0) == '.' && str.indexOf('/', 1) == -1) {
            return true;
        }
        int slash = str.lastIndexOf('/');
        return slash != -1 && slash + 1 < str.length() && str.charAt(slash + 1) == '.';
    }

    private static String escape(String str) {
        return V8RegExp.replace(str, ENCLOSED, true, m -> {
            String inner = m.group(1) != null ? m.group(1) : m.group(2) != null ? m.group(2) : m.group(3);
            if (inner == null || inner.isEmpty()) {
                return m.group();
            }
            return Js.splitJoin(m.group(), inner, inner.replace("/", "__SLASH__").replace(".", "__DOT__"));
        });
    }

    private static String unescape(String str) {
        return str.replace("__SLASH__", "/").replace("__DOT__", ".");
    }
}
