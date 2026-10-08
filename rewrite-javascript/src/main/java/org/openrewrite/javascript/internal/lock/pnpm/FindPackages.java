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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Port of @pnpm/workspace.projects-reader src/index.ts {@code findWorkspaceProjectsNoCheck} and src/findPackages.ts
 * (pnpm 11.2.2), with @pnpm/lockfile.fs {@code getLockfileImporterId}, over the directories holding a package.json.
 */
final class FindPackages {

    private static final List<String> DEFAULT_IGNORE = Arrays.asList("**/node_modules/**", "**/bower_components/**",
            "**/test/**", "**/tests/**");
    private static final List<String> IGNORE = Arrays.asList("**/node_modules/**", "**/bower_components/**");

    private FindPackages() {
    }

    /**
     * {@code findWorkspaceProjectsNoCheck}: the project directories among the directories holding a package.json,
     * sorted as pnpm sorts them ({@code ""} is the root).
     */
    static List<String> findWorkspaceProjects(Collection<String> manifestDirs, List<String> patterns) {
        List<String> projects = findPackages(manifestDirs, patterns, IGNORE, true);
        projects.sort(KeySorting::lexCompare);
        return projects;
    }

    /**
     * {@code findPackages}: the directories whose package.json the patterns match, deduplicated, by directory.
     */
    static List<String> findPackages(Collection<String> manifestDirs, @Nullable List<String> patterns,
                                     @Nullable List<String> ignorePatterns, boolean includeRoot) {
        List<String> normalized = normalizePatterns(patterns == null ? Arrays.asList(".", "**") : patterns);
        List<Pattern> include = new ArrayList<>();
        List<Pattern> ignore = new ArrayList<>();
        for (String pattern : ignorePatterns == null ? DEFAULT_IGNORE : ignorePatterns) {
            ignore.add(toRegex(pattern));
        }
        for (String pattern : normalized) {
            if (pattern.startsWith("!")) {
                ignore.add(toRegex(pattern.substring(1)));
            } else {
                include.add(toRegex(pattern));
            }
        }
        List<String> paths = new ArrayList<>();
        for (String dir : manifestDirs) {
            String manifestPath = dir.isEmpty() ? "package.json" : dir + "/package.json";
            if (matchesAny(include, manifestPath) && !matchesAny(ignore, manifestPath)) {
                paths.add(dir);
            }
        }
        if (includeRoot && manifestDirs.contains("")) {
            paths.add("");
        }
        paths.sort(KeySorting::lexCompare);
        return new ArrayList<>(new LinkedHashSet<>(paths));
    }

    private static List<String> normalizePatterns(List<String> patterns) {
        List<String> normalized = new ArrayList<>();
        for (String pattern : patterns) {
            normalized.add(pattern.replaceFirst("/?\\z", "/package.{json,yaml,json5}"));
        }
        return normalized;
    }

    /**
     * {@code getLockfileImporterId}: {@code .} for the lock's own directory.
     */
    static String getLockfileImporterId(String rootDir) {
        return rootDir.isEmpty() ? "." : rootDir;
    }

    private static boolean matchesAny(List<Pattern> patterns, String path) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(path).matches()) {
                return true;
            }
        }
        return false;
    }

    static Pattern toRegex(String glob) {
        String pattern = glob.startsWith("./") ? glob.substring(2) : glob;
        if (pattern.startsWith("../") || pattern.startsWith("/")) {
            throw new CannotKnow("the projects the workspace pattern \"" + glob + "\" matches (paths outside the " +
                                 "project)");
        }
        if (pattern.matches(".*[@!+*?]\\(.*") || pattern.contains("\\")) {
            throw new CannotKnow("the projects the workspace pattern \"" + glob + "\" matches (an extglob or " +
                                 "escape, which this port of tinyglobby 0.2.16 does not match)");
        }
        StringBuilder regex = new StringBuilder();
        String[] segments = pattern.split("/", -1);
        for (int s = 0; s < segments.length; s++) {
            String segment = segments[s];
            boolean last = s == segments.length - 1;
            if ("**".equals(segment)) {
                regex.append(last ? "(?:(?!\\.)[^/]*(?:/(?!\\.)[^/]*)*)?" : "(?:(?!\\.)[^/]*/)*");
                continue;
            }
            if (!segment.startsWith(".")) {
                regex.append("(?!\\.)");
            }
            regex.append(segmentRegex(segment, glob));
            if (!last) {
                regex.append('/');
            }
        }
        return Pattern.compile(regex.toString());
    }

    private static String segmentRegex(String segment, String glob) {
        StringBuilder out = new StringBuilder();
        int braces = 0;
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            switch (c) {
                case '*':
                    out.append("[^/]*");
                    break;
                case '?':
                    out.append("[^/]");
                    break;
                case '[': {
                    int end = segment.indexOf(']', i + 1);
                    if (end < 0) {
                        out.append("\\[");
                        break;
                    }
                    String body = segment.substring(i + 1, end);
                    if (body.startsWith("!") || body.startsWith("^")) {
                        body = "^" + body.substring(1);
                    }
                    out.append('[').append(body.replace("\\", "\\\\")).append(']');
                    i = end;
                    break;
                }
                case '{':
                    braces++;
                    out.append("(?:");
                    break;
                case '}':
                    if (braces == 0) {
                        out.append("\\}");
                    } else {
                        braces--;
                        out.append(')');
                    }
                    break;
                case ',':
                    out.append(braces > 0 ? "|" : ",");
                    break;
                default:
                    out.append(Pattern.quote(String.valueOf(c)));
            }
        }
        if (braces != 0) {
            throw new CannotKnow("the projects the workspace pattern \"" + glob + "\" matches (an unbalanced brace)");
        }
        return out.toString();
    }
}
