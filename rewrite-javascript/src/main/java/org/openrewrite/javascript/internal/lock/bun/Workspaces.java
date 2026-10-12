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
package org.openrewrite.javascript.internal.lock.bun;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.RootRelativePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of bun's src/install/lockfile/Package/WorkspaceMap.zig {@code processNamesArray} (bun 1.3.10), over the
 * project's manifests the host supplies, by location, as the release that installs reads them ({@link BunRelease}).
 */
final class Workspaces {

    static final class Entry {
        final String name;
        final @Nullable String version;

        Entry(String name, @Nullable String version) {
            this.name = name;
            this.version = version;
        }
    }

    private static final String[] IGNORED_PATHS = {"node_modules", ".git", "CMakeFiles"};

    private final BunRelease release;

    final LinkedHashMap<String, Entry> map = new LinkedHashMap<>();

    Workspaces(BunRelease release) {
        this.release = release;
    }

    /**
     * @param manifests the project's manifests by location relative to the root ({@code ""} the root itself).
     */
    int processNamesArray(Log log, List<Json.Expr> arr, Map<String, String> manifests) {
        if (arr.isEmpty()) {
            return 0;
        }
        int origErrors = log.errors.size();
        List<String> workspaceGlobs = new ArrayList<>();
        for (Json.Expr item : arr) {
            String inputPath = item.asString();
            if (inputPath == null) {
                log.addError("Workspaces expects an array of strings");
                throw new IllegalArgumentException("InvalidPackageJSON");
            }
            if (inputPath.isEmpty() || ".".equals(inputPath) || "./".equals(inputPath) || ".\\".equals(inputPath)) {
                continue;
            }
            if (Glob.detectGlobSyntax(inputPath, release)) {
                workspaceGlobs.add(inputPath);
                continue;
            }
            String location = RootRelativePath.normalize(inputPath.replace('\\', '/'));
            if (location.isEmpty()) {
                continue;
            }
            String manifest = manifests.get(location);
            if (manifest == null) {
                log.addError("Workspace not found \"" + inputPath + "\"");
                continue;
            }
            Entry entry = processWorkspaceName(log, manifest, inputPath);
            if (entry == null || entry.name.isEmpty()) {
                continue;
            }
            insert(location, entry);
        }

        for (int i = 0; i < workspaceGlobs.size(); i++) {
            String userPattern = workspaceGlobs.get(i);
            if (!release.walksDotComponentsAsWritten()) {
                for (String component : userPattern.replace('\\', '/').split("/")) {
                    if (component.length() == 2 && component.charAt(0) == '.' && !"..".equals(component)) {
                        throw new CannotKnow("the directories bun " + release + " walks for the workspaces pattern " +
                                             userPattern + ", whose " + component + " its glob walker takes for ..");
                    }
                }
            }
            String globPattern = userPattern.isEmpty() ? "package.json" : RootRelativePath.join(
                    userPattern.replace('\\', '/'), "package.json");
            List<Component> components = Component.build(globPattern);
            if (!components.isEmpty() && components.get(0).hint == Hint.DOT_BACK) {
                throw new CannotKnow("the directories above the root bun " + release + " walks for the workspaces " +
                                     "pattern " + userPattern);
            }
            for (String location : manifests.keySet()) {
                if (location.isEmpty()) {
                    continue;
                }
                if (!walks(components, (location + "/package.json").split("/"), 0, 0)) {
                    continue;
                }
                boolean negated = false;
                for (String nextPattern : release.excludesWorkspacesALaterPatternNegates() ?
                        workspaceGlobs.subList(i + 1, workspaceGlobs.size()) : Collections.<String>emptyList()) {
                    if (Glob.match(nextPattern, location, release) == Glob.MatchResult.NEGATE_NO_MATCH) {
                        negated = true;
                        break;
                    }
                }
                if (negated) {
                    continue;
                }
                Entry entry = processWorkspaceName(log, manifests.get(location), location);
                if (entry == null || entry.name.isEmpty()) {
                    continue;
                }
                insert(location, entry);
            }
        }

        if (origErrors != log.errors.size()) {
            throw new IllegalArgumentException("InstallFailed");
        }
        List<Map.Entry<String, Entry>> entries = new ArrayList<>(map.entrySet());
        entries.sort((a, b) -> Strings.order(a.getValue().name, b.getValue().name));
        map.clear();
        for (Map.Entry<String, Entry> e : entries) {
            map.put(e.getKey(), e.getValue());
        }
        return map.size();
    }

    /**
     * A pattern component as src/glob/GlobWalker.zig {@code makeComponent} tells it.
     */
    enum Hint {
        NONE, DOT, DOT_BACK, LITERAL, SINGLE, DOUBLE, WILDCARD_FILEPATH
    }

    static final class Component {
        final String text;
        final Hint hint;

        private Component(String text, Hint hint) {
            this.text = text;
            this.hint = hint;
        }

        // buildPatternComponents on POSIX: the pattern's non-empty parts between slashes; a pattern ending in a slash
        // is never built here, as every one ends in package.json
        static List<Component> build(String pattern) {
            List<Component> components = new ArrayList<>();
            int start = 0;
            for (int i = 0; i <= pattern.length(); i++) {
                if (i == pattern.length() || pattern.charAt(i) == '/') {
                    if (i > start) {
                        components.add(make(pattern, start, i));
                    }
                    start = i + 1;
                }
            }
            return components;
        }

        private static Component make(String pattern, int start, int end) {
            String text = pattern.substring(start, end);
            if (".".equals(text)) {
                return new Component(text, Hint.DOT);
            }
            if ("..".equals(text)) {
                return new Component(text, Hint.DOT_BACK);
            }
            if (!hasSpecialSyntax(text)) {
                return new Component(text, Hint.LITERAL);
            }
            if (text.length() == 1) {
                return new Component(text, text.charAt(0) == '*' ? Hint.SINGLE : Hint.NONE);
            }
            if ("**".equals(text)) {
                return new Component(text, Hint.DOUBLE);
            }
            // the rest of the whole pattern, not of the component, is checked for other glob syntax
            if (text.charAt(0) == '*' && text.charAt(1) == '.' && start + 2 < pattern.length()) {
                for (int i = start + 2; i < pattern.length(); i++) {
                    char c = pattern.charAt(i);
                    if (c == '[' || c == '{' || c == '?' || c == '*') {
                        return new Component(text, Hint.NONE);
                    }
                }
                return new Component(text, Hint.WILDCARD_FILEPATH);
            }
            return new Component(text, Hint.NONE);
        }

        private static boolean hasSpecialSyntax(String text) {
            for (int i = 0; i < text.length(); i++) {
                if ("*[{?!".indexOf(text.charAt(i)) >= 0) {
                    return true;
                }
            }
            return false;
        }
    }

    // GlobWalker's iteration (dot, absolute and follow_symlinks off, only_files on) down a path of the project's: at
    // the directory of its first k segments with component idx, whether it yields the file the path ends in
    private boolean walks(List<Component> components, String[] segments, int k, int idx) {
        int last = components.size() - 1;
        // skipSpecialComponents: the root's . and .. are where the walk starts, and a run of ** is its last
        while (idx < components.size() && components.get(idx).hint == Hint.DOT) {
            idx++;
        }
        while (idx < last && components.get(idx).hint == Hint.DOUBLE && components.get(idx + 1).hint == Hint.DOUBLE) {
            idx++;
        }
        if (idx > last) {
            return false;
        }
        Component pattern = components.get(idx);
        boolean isLast = idx == last;
        if (isLast && pattern.hint == Hint.LITERAL) {
            // the last literal component is opened in the directory itself, whatever its name
            return k == segments.length - 1 && segments[k].equals(pattern.text);
        }
        String entry = segments[k];
        Component next = isLast ? null : components.get(idx + 1);
        if (k == segments.length - 1) {
            // matchPatternFile
            if (!isLast) {
                return pattern.hint == Hint.DOUBLE && idx + 1 == last && next.hint != Hint.DOUBLE &&
                       matchPatternImpl(next, entry);
            }
            return matchPatternImpl(pattern, entry);
        }
        int bump = matchPatternDir(pattern, next, entry, idx, last);
        if (bump < 0) {
            return false;
        }
        if (bump == 2) {
            return walks(components, segments, k + 1, idx + 2) || walks(components, segments, k + 1, idx);
        }
        return walks(components, segments, k + 1, idx + bump);
    }

    // matchPatternDir: how many components the walk moves on into the directory, -1 where it does not go in; before
    // 1.4.0 never into a hidden one, from 1.4.0 into one a component names, and by ** alone into none
    private int matchPatternDir(Component pattern, @Nullable Component next, String entry, int idx, int last) {
        boolean hidden = entry.startsWith(".");
        if (hidden && !release.matchesHiddenEntriesThePatternNames()) {
            return -1;
        }
        if (ignored(entry)) {
            return -1;
        }
        boolean isLast = idx == last;
        if (pattern.hint == Hint.DOUBLE) {
            if (!isLast && matchPatternImpl(next, entry)) {
                if (idx + 1 == last) {
                    return hidden ? -1 : 0;
                }
                return 2;
            }
            return hidden ? -1 : 0;
        }
        return matchPatternImpl(pattern, entry) && !isLast ? 1 : -1;
    }

    // matchPatternImpl: a hidden entry matches only a component that itself starts with . (from 1.4.0), and none
    // before
    private boolean matchPatternImpl(@Nullable Component pattern, String filepath) {
        if (pattern == null) {
            return false;
        }
        if (filepath.startsWith(".") &&
            (!release.matchesHiddenEntriesThePatternNames() || !pattern.text.startsWith("."))) {
            return false;
        }
        if (ignored(filepath)) {
            return false;
        }
        switch (pattern.hint) {
            case DOUBLE:
            case SINGLE:
                return true;
            case WILDCARD_FILEPATH:
                return filepath.endsWith(pattern.text.substring(1));
            case LITERAL:
                return pattern.text.equals(filepath);
            default:
                return Glob.match(pattern.text, filepath, release).matches();
        }
    }

    private static boolean ignored(String name) {
        for (String ignoredPath : IGNORED_PATHS) {
            if (ignoredPath.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private void insert(String key, Entry value) {
        map.put(key, value);
    }

    private @Nullable Entry processWorkspaceName(Log log, String manifest, String inputPath) {
        Json.Expr workspaceJson;
        try {
            workspaceJson = Json.parse(manifest, release);
        } catch (Json.SyntaxError e) {
            log.addError(e.error + " reading package.json for workspace package \"" + inputPath + "\"");
            return null;
        }
        Json.Expr nameExpr = workspaceJson.get("name");
        String name = nameExpr == null ? null : nameExpr.asString();
        if (name == null) {
            log.addError("Missing \"name\" from package.json in " + inputPath);
            return null;
        }
        Json.Expr versionExpr = workspaceJson.get("version");
        return new Entry(name, versionExpr == null ? null : versionExpr.asString());
    }
}
