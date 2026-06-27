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
package org.openrewrite.golang.internal.modgraph;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A small, allocation-light go.mod reader for the resolver. It extracts only
 * what Minimal Version Selection needs — the module path, the {@code go}
 * directive, the {@code require} set (with {@code // indirect} flags), and the
 * main module's {@code replace} directives — from both the project go.mod and
 * the (many) dependency go.mod files fetched from the proxy. It is line-oriented
 * and tolerant: anything it does not recognize is skipped, matching how the Go
 * toolchain's modfile loader treats unknown directives for resolution purposes.
 */
public final class GoModFile {

    @Nullable String modulePath;
    @Nullable String goVersion;
    final List<Require> requires = new ArrayList<>();
    final List<Replace> replaces = new ArrayList<>();

    public @Nullable String modulePath() {
        return modulePath;
    }

    public @Nullable String goVersion() {
        return goVersion;
    }

    public List<Require> requires() {
        return requires;
    }

    public List<Replace> replaces() {
        return replaces;
    }

    public static final class Require {
        public final String path;
        public final String version;
        public final boolean indirect;

        Require(String path, String version, boolean indirect) {
            this.path = path;
            this.version = version;
            this.indirect = indirect;
        }
    }

    public static final class Replace {
        public final String oldPath;
        public final @Nullable String oldVersion;
        public final String newPath;
        public final @Nullable String newVersion;

        Replace(String oldPath, @Nullable String oldVersion, String newPath, @Nullable String newVersion) {
            this.oldPath = oldPath;
            this.oldVersion = oldVersion;
            this.newPath = newPath;
            this.newVersion = newVersion;
        }
    }

    public static GoModFile parse(String content) {
        GoModFile mf = new GoModFile();
        String block = null; // "require" or "replace" while inside a ( ... ) block
        for (String raw : content.split("\n", -1)) {
            String line = raw;
            boolean indirect = stripComment(line).indirect;
            line = stripComment(line).text.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (block != null) {
                if (line.equals(")")) {
                    block = null;
                    continue;
                }
                if (block.equals("require")) {
                    mf.addRequire(tokens(line), indirect);
                } else if (block.equals("replace")) {
                    mf.addReplace(tokens(line));
                }
                continue;
            }
            List<String> t = tokens(line);
            if (t.isEmpty()) {
                continue;
            }
            switch (t.get(0)) {
                case "module":
                    if (t.size() > 1) {
                        mf.modulePath = unquote(t.get(1));
                    }
                    break;
                case "go":
                    if (t.size() > 1) {
                        mf.goVersion = t.get(1);
                    }
                    break;
                case "require":
                    if (t.size() == 2 && t.get(1).equals("(")) {
                        block = "require";
                    } else {
                        mf.addRequire(t.subList(1, t.size()), indirect);
                    }
                    break;
                case "replace":
                    if (t.size() == 2 && t.get(1).equals("(")) {
                        block = "replace";
                    } else {
                        mf.addReplace(t.subList(1, t.size()));
                    }
                    break;
                default:
                    // exclude / retract / toolchain etc. — not needed for resolution
                    break;
            }
        }
        return mf;
    }

    private void addRequire(List<String> t, boolean indirect) {
        if (t.size() >= 2) {
            requires.add(new Require(unquote(t.get(0)), unquote(t.get(1)), indirect));
        }
    }

    // Forms: "old => new ver" | "old => new" | "old ver => new ver" | "old ver => new"
    private void addReplace(List<String> t) {
        int arrow = t.indexOf("=>");
        if (arrow < 0) {
            return;
        }
        List<String> lhs = t.subList(0, arrow);
        List<String> rhs = t.subList(arrow + 1, t.size());
        if (lhs.isEmpty() || rhs.isEmpty()) {
            return;
        }
        String oldPath = unquote(lhs.get(0));
        String oldVersion = lhs.size() > 1 ? unquote(lhs.get(1)) : null;
        String newPath = unquote(rhs.get(0));
        String newVersion = rhs.size() > 1 ? unquote(rhs.get(1)) : null;
        replaces.add(new Replace(oldPath, oldVersion, newPath, newVersion));
    }

    private static final class Stripped {
        final String text;
        final boolean indirect;

        Stripped(String text, boolean indirect) {
            this.text = text;
            this.indirect = indirect;
        }
    }

    private static Stripped stripComment(String line) {
        int c = line.indexOf("//");
        if (c < 0) {
            return new Stripped(line, false);
        }
        boolean indirect = line.substring(c).replace("//", "").trim().equals("indirect");
        return new Stripped(line.substring(0, c), indirect);
    }

    private static List<String> tokens(String line) {
        List<String> out = new ArrayList<>();
        for (String tok : line.trim().split("\\s+")) {
            if (!tok.isEmpty()) {
                out.add(tok);
            }
        }
        return out;
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && (s.charAt(0) == '"' || s.charAt(0) == '`') &&
            s.charAt(s.length() - 1) == s.charAt(0)) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
