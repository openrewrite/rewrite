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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Port of fast-glob 3.3.2 for {@code fastGlob(patterns, {cwd, onlyDirectories: true, ignore})} as Workspace.ts calls
 * it on a POSIX host: the tasks (managers/tasks.js, utils/pattern.js) and the async provider's readers with their
 * deep and entry filters (providers/), over the project's directories.
 */
final class FastGlob {

    private static final List<String> IGNORE = Arrays.asList("**/node_modules", "**/.git", "**/.yarn");

    private static final Pattern COMMON_GLOB_SYMBOLS_RE = JsRegExp.of("[*?]|^!");
    private static final Pattern REGEX_CHARACTER_CLASS_SYMBOLS_RE = JsRegExp.of("\\[[^[]*]");
    private static final Pattern REGEX_GROUP_SYMBOLS_RE = JsRegExp.of("(?:^|[^!*+?@])\\([^(]*\\|[^|]*\\)");
    private static final Pattern GLOB_EXTENSION_SYMBOLS_RE = JsRegExp.of("[!*+?@]\\([^(]*\\)");
    private static final Pattern BRACE_EXPANSION_SEPARATORS_RE = JsRegExp.of(",|\\.\\.");
    private static final Pattern DOUBLE_SLASH_RE = JsRegExp.of("(?!^)\\/{2,}");
    private static final Pattern BRACES = JsRegExp.of("\\{.*\\}");

    private enum Kind {
        DIRECTORY, FILE, ABSENT
    }

    private static final class Task {
        final String base;
        final boolean dynamic;
        final List<String> positive;
        final List<String> negative;
        final List<String> patterns = new ArrayList<>();

        Task(String base, boolean dynamic, List<String> positive, List<String> negative) {
            this.base = base;
            this.dynamic = dynamic;
            this.positive = positive;
            this.negative = negative;
            patterns.addAll(positive);
            for (String pattern : negative) {
                patterns.add("!" + pattern);
            }
        }
    }

    private final Set<String> directories = new HashSet<>();
    private final Set<String> files = new HashSet<>();
    private final Map<String, Set<String>> children = new HashMap<>();

    /**
     * @param manifestDirectories the directories that hold a package.json, relative to the project's root
     *                            ({@code .} the root)
     */
    FastGlob(Collection<String> manifestDirectories) {
        directories.add(".");
        for (String directory : manifestDirectories) {
            String path = normalize(directory);
            files.add(join(path, "package.json"));
            while (!".".equals(path) && directories.add(path)) {
                String parent = parent(path);
                children.computeIfAbsent(parent, p -> new TreeSet<>()).add(path.substring(path.lastIndexOf('/') + 1));
                path = parent;
            }
        }
    }

    /**
     * The directories the patterns match under {@code cwd}, spelled as fast-glob reports them.
     *
     * @param cwd the directory the patterns are relative to, from the project's root
     * @throws JsError    what fast-glob, micromatch or the file system throws
     * @throws CannotKnow for a pattern that reaches outside the project, or two tasks that race to report one
     *                    directory under two spellings
     */
    List<String> glob(List<String> source, String cwd) {
        for (String pattern : source) {
            if (pattern.isEmpty()) {
                throw new JsError(JsError.TYPE_ERROR, "Patterns must be a string (non empty) or an array of strings");
            }
        }
        List<Reader> readers = new ArrayList<>();
        for (Task task : generate(source)) {
            readers.add(new Reader(task, cwd));
        }
        List<List<String>> works = new ArrayList<>();
        for (Reader reader : readers) {
            works.add(reader.read());
        }
        Map<String, String> reported = new LinkedHashMap<>();
        for (List<String> work : works) {
            for (String path : work) {
                String other = reported.putIfAbsent(removeLeadingDotSegment(path), path);
                if (other != null && !other.equals(path)) {
                    throw new CannotKnow("the order of the workspaces " + cwd + " lists (fast-glob's tasks race to " +
                                         "report " + other + " or " + path + ")");
                }
            }
        }
        return new ArrayList<>(reported.values());
    }

    private static List<Task> generate(List<String> input) {
        List<String> patterns = processPatterns(input);
        List<String> positive = new ArrayList<>();
        List<String> negative = new ArrayList<>();
        for (String pattern : patterns) {
            if (isNegativePattern(pattern)) {
                negative.add(pattern.substring(1));
            } else {
                positive.add(pattern);
            }
        }
        for (String pattern : processPatterns(IGNORE)) {
            negative.add(isNegativePattern(pattern) ? pattern.substring(1) : pattern);
        }
        List<String> staticPatterns = new ArrayList<>();
        List<String> dynamicPatterns = new ArrayList<>();
        for (String pattern : positive) {
            (isDynamicPattern(pattern) ? dynamicPatterns : staticPatterns).add(pattern);
        }
        List<Task> tasks = convertPatternsToTasks(staticPatterns, negative, false);
        tasks.addAll(convertPatternsToTasks(dynamicPatterns, negative, true));
        return tasks;
    }

    private static List<String> processPatterns(List<String> input) {
        List<String> patterns = new ArrayList<>();
        for (String pattern : input) {
            List<String> expanded = new ArrayList<>(braces(pattern));
            expanded.sort(Comparator.comparingInt(String::length));
            for (String p : expanded) {
                if (!p.isEmpty()) {
                    patterns.add(p);
                }
            }
        }
        List<String> out = new ArrayList<>();
        for (String pattern : patterns) {
            out.add(DOUBLE_SLASH_RE.matcher(pattern).replaceAll("/"));
        }
        return out;
    }

    /**
     * micromatch 4.0.5's {@code braces(pattern, {expand: true, nodupes: true, keepEscaping: true})}.
     */
    static List<String> braces(String pattern) {
        return BRACES.matcher(pattern).find() ? Braces.braces(pattern) : Collections.singletonList(pattern);
    }

    private static List<Task> convertPatternsToTasks(List<String> positive, List<String> negative, boolean dynamic) {
        List<String> outside = new ArrayList<>();
        List<String> inside = new ArrayList<>();
        for (String pattern : positive) {
            (pattern.startsWith("..") || pattern.startsWith("./..") ? outside : inside).add(pattern);
        }
        Map<String, List<String>> outsideGroup = groupPatternsByBaseDirectory(outside);
        Map<String, List<String>> insideGroup = groupPatternsByBaseDirectory(inside);
        List<Task> tasks = convertPatternGroupsToTasks(outsideGroup, negative, dynamic);
        if (insideGroup.containsKey(".")) {
            tasks.add(new Task(".", dynamic, inside, negative));
        } else {
            tasks.addAll(convertPatternGroupsToTasks(insideGroup, negative, dynamic));
        }
        return tasks;
    }

    // a base Object.prototype already names is found by `in`, and has no push
    private static Map<String, List<String>> groupPatternsByBaseDirectory(List<String> patterns) {
        Map<String, List<String>> group = new LinkedHashMap<>();
        for (String pattern : patterns) {
            String base = GlobParent.globParent(pattern);
            if (Picomatch.objectPrototypeMember(base) != null) {
                throw new JsError(JsError.TYPE_ERROR, "collection[base].push is not a function");
            }
            group.computeIfAbsent(base, b -> new ArrayList<>()).add(pattern);
        }
        return group;
    }

    private static List<Task> convertPatternGroupsToTasks(Map<String, List<String>> positive, List<String> negative,
                                                          boolean dynamic) {
        List<Task> tasks = new ArrayList<>();
        for (String base : Js.keys(positive)) {
            tasks.add(new Task(base, dynamic, positive.get(base), negative));
        }
        return tasks;
    }

    static boolean isDynamicPattern(String pattern) {
        if (pattern.isEmpty()) {
            return false;
        }
        if (pattern.contains("\\")) {
            return true;
        }
        if (COMMON_GLOB_SYMBOLS_RE.matcher(pattern).find() || REGEX_CHARACTER_CLASS_SYMBOLS_RE.matcher(pattern).find() ||
            REGEX_GROUP_SYMBOLS_RE.matcher(pattern).find() || GLOB_EXTENSION_SYMBOLS_RE.matcher(pattern).find()) {
            return true;
        }
        int openingBraceIndex = pattern.indexOf('{');
        if (openingBraceIndex == -1) {
            return false;
        }
        int closingBraceIndex = pattern.indexOf('}', openingBraceIndex + 1);
        if (closingBraceIndex == -1) {
            return false;
        }
        return BRACE_EXPANSION_SEPARATORS_RE.matcher(pattern.substring(openingBraceIndex, closingBraceIndex)).find();
    }

    private static boolean isNegativePattern(String pattern) {
        return pattern.startsWith("!") && (pattern.length() < 2 || pattern.charAt(1) != '(');
    }

    private static boolean isAffectDepthOfReadingPattern(String pattern) {
        return pattern.endsWith("/**") || !isDynamicPattern(GlobParent.basename(pattern));
    }

    private static List<String> getPatternParts(String pattern) {
        List<String> parts = PicomatchScan.parts(pattern);
        if (parts.isEmpty()) {
            parts = new ArrayList<>(Collections.singletonList(pattern));
        }
        if (parts.get(0).startsWith("/")) {
            parts.set(0, parts.get(0).substring(1));
            parts.add(0, "");
        }
        return parts;
    }

    private static String removeLeadingDotSegment(String entry) {
        return entry.startsWith("./") || entry.startsWith(".\\") ? entry.substring(2) : entry;
    }

    private static List<Pattern> makeRe(List<String> patterns, boolean dot) {
        List<Pattern> out = new ArrayList<>();
        for (String pattern : patterns) {
            out.add(Picomatch.makeRe(pattern, dot));
        }
        return out;
    }

    private static boolean matchAny(String entry, List<Pattern> patternsRe) {
        for (Pattern patternRe : patternsRe) {
            if (test(patternRe, entry)) {
                return true;
            }
        }
        return false;
    }

    // a Java pattern reads a surrogate pair as one character where V8 reads two
    private static boolean test(Pattern pattern, String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isSurrogate(s.charAt(i))) {
                throw new CannotKnow("how V8 matches a workspace pattern against " + s + " (a character outside " +
                                     "the Basic Multilingual Plane)");
            }
        }
        return pattern.matcher(s).find();
    }

    private final class Reader {
        private final Task task;
        private final String cwd;
        private final PartialMatcher matcher;
        private final List<Pattern> deepNegativeRe;
        private final List<Pattern> positiveRe;
        private final List<Pattern> negativeRe;

        Reader(Task task, String cwd) {
            this.task = task;
            this.cwd = cwd;
            this.matcher = new PartialMatcher(task.positive);
            List<String> affectDepthOfReading = new ArrayList<>();
            for (String pattern : task.negative) {
                if (isAffectDepthOfReadingPattern(pattern)) {
                    affectDepthOfReading.add(pattern);
                }
            }
            this.deepNegativeRe = makeRe(affectDepthOfReading, false);
            this.positiveRe = makeRe(task.positive, false);
            this.negativeRe = makeRe(task.negative, true);
        }

        List<String> read() {
            return task.dynamic ? dynamic() : statics();
        }

        private List<String> statics() {
            List<String> entries = new ArrayList<>();
            Set<String> index = new HashSet<>();
            for (String pattern : task.patterns) {
                Kind kind = stat(resolve(cwd, pattern));
                if (kind != Kind.ABSENT && entryFilter(pattern, kind == Kind.DIRECTORY, index)) {
                    entries.add(pattern);
                }
            }
            return entries;
        }

        private List<String> dynamic() {
            String root = resolve(cwd, task.base);
            Kind kind = stat(root);
            if (kind == Kind.ABSENT) {
                return Collections.emptyList();
            }
            if (kind == Kind.FILE) {
                throw new JsError(JsError.ERROR, "ENOTDIR: not a directory, scandir '" + root + "'");
            }
            List<String> entries = new ArrayList<>();
            Set<String> index = new HashSet<>();
            Deque<String[]> queue = new ArrayDeque<>();
            queue.add(new String[]{root, ".".equals(task.base) ? "" : task.base});
            while (!queue.isEmpty()) {
                String[] item = queue.poll();
                for (String name : children.getOrDefault(item[0], Collections.emptySet())) {
                    String base = item[1];
                    String path = base.isEmpty() ? name : base.endsWith("/") ? base + name : base + "/" + name;
                    if (entryFilter(path, true, index)) {
                        entries.add(path);
                    }
                    if (deepFilter(path)) {
                        queue.add(new String[]{join(item[0], name), path});
                    }
                }
            }
            return entries;
        }

        private boolean deepFilter(String entryPath) {
            String filepath = removeLeadingDotSegment(entryPath);
            if (!matcher.match(filepath)) {
                return false;
            }
            return !matchAny(filepath, deepNegativeRe);
        }

        private boolean entryFilter(String entryPath, boolean isDirectory, Set<String> index) {
            String filepath = removeLeadingDotSegment(entryPath);
            if (index.contains(filepath) || !isDirectory) {
                return false;
            }
            boolean isMatched = isMatchToPatterns(filepath, positiveRe) && !isMatchToPatterns(filepath, negativeRe);
            if (isMatched) {
                index.add(filepath);
            }
            return isMatched;
        }

        private boolean isMatchToPatterns(String filepath, List<Pattern> patternsRe) {
            return matchAny(filepath, patternsRe) || matchAny(filepath + "/", patternsRe);
        }
    }

    private static final class PartialMatcher {
        private final List<PatternInfo> storage = new ArrayList<>();

        PartialMatcher(List<String> patterns) {
            for (String pattern : patterns) {
                List<Segment> segments = new ArrayList<>();
                for (String part : getPatternParts(pattern)) {
                    boolean dynamic = isDynamicPattern(part);
                    segments.add(new Segment(dynamic, part, dynamic ? Picomatch.makeRe(part, false) : null));
                }
                int firstSection = segments.size();
                for (int i = segments.size() - 1; i >= 0; i--) {
                    if (segments.get(i).dynamic && segments.get(i).pattern.contains("**")) {
                        firstSection = i;
                    }
                }
                storage.add(new PatternInfo(firstSection == segments.size(), segments, firstSection));
            }
        }

        boolean match(String filepath) {
            String[] parts = filepath.split("/", -1);
            int levels = parts.length;
            for (PatternInfo pattern : storage) {
                if (pattern.complete && pattern.segments.size() <= levels) {
                    continue;
                }
                if (!pattern.complete && levels > pattern.firstSection) {
                    return true;
                }
                boolean match = true;
                for (int index = 0; index < parts.length && match; index++) {
                    Segment segment = pattern.segments.get(index);
                    match = segment.patternRe != null ? test(segment.patternRe, parts[index]) :
                            segment.pattern.equals(parts[index]);
                }
                if (match) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class Segment {
        final boolean dynamic;
        final String pattern;
        final @Nullable Pattern patternRe;

        Segment(boolean dynamic, String pattern, @Nullable Pattern patternRe) {
            this.dynamic = dynamic;
            this.pattern = pattern;
            this.patternRe = patternRe;
        }
    }

    private static final class PatternInfo {
        final boolean complete;
        final List<Segment> segments;

        /**
         * The length of the first section, the segments before the first globstar.
         */
        final int firstSection;

        PatternInfo(boolean complete, List<Segment> segments, int firstSection) {
            this.complete = complete;
            this.segments = segments;
            this.firstSection = firstSection;
        }
    }

    /**
     * {@code path.resolve(cwd, path)} relative to the project's root.
     *
     * @throws CannotKnow for a path outside the project
     */
    static String resolve(String cwd, String path) {
        if (path.startsWith("/")) {
            throw new CannotKnow("the directories the workspace pattern " + path + " matches (an absolute path)");
        }
        String resolved = normalize(".".equals(cwd) ? path : cwd + "/" + path);
        if ("..".equals(resolved) || resolved.startsWith("../")) {
            throw new CannotKnow("the directories the workspace pattern " + path + " matches (outside the project)");
        }
        return resolved;
    }

    /**
     * {@code xfs.existsPromise} of a path relative to the project's root.
     */
    boolean exists(String path) {
        String normalized = normalize(path);
        return directories.contains(normalized) || files.contains(normalized);
    }

    private Kind stat(String path) {
        if (".".equals(path)) {
            return Kind.DIRECTORY;
        }
        int slash = -1;
        while ((slash = path.indexOf('/', slash + 1)) != -1) {
            String prefix = path.substring(0, slash);
            if (files.contains(prefix)) {
                throw new JsError(JsError.ERROR, "ENOTDIR: not a directory, lstat '" + path + "'");
            }
            if (!directories.contains(prefix)) {
                return Kind.ABSENT;
            }
        }
        return directories.contains(path) ? Kind.DIRECTORY : files.contains(path) ? Kind.FILE : Kind.ABSENT;
    }

    private static String normalize(String path) {
        String normalized = PortablePath.normalize(path);
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String parent(String path) {
        int slash = path.lastIndexOf('/');
        return slash == -1 ? "." : path.substring(0, slash);
    }

    private static String join(String directory, String name) {
        return ".".equals(directory) ? name : directory + "/" + name;
    }
}
