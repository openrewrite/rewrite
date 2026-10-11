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
import org.openrewrite.javascript.internal.lock.RootRelativePath;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of bun's src/install/lockfile/Package/WorkspaceMap.zig {@code processNamesArray} (bun 1.3.10), over the
 * project's manifests the host supplies, by location.
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

    final LinkedHashMap<String, Entry> map = new LinkedHashMap<>();

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
            if (Glob.detectGlobSyntax(inputPath)) {
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
            String globPattern = userPattern.isEmpty() ? "package.json" : RootRelativePath.join(
                    userPattern.replace('\\', '/'), "package.json");
            for (String location : manifests.keySet()) {
                if (location.isEmpty() || ignored(location)) {
                    continue;
                }
                String matchedPath = location + "/package.json";
                if (!walkerMatches(globPattern, matchedPath)) {
                    continue;
                }
                boolean negated = false;
                for (String nextPattern : workspaceGlobs.subList(i + 1, workspaceGlobs.size())) {
                    if (Glob.match(nextPattern, location) == Glob.MatchResult.NEGATE_NO_MATCH) {
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

    // the glob walker matches files under the root, hidden directories and node_modules excluded
    private static boolean walkerMatches(String pattern, String path) {
        for (String segment : path.split("/")) {
            if (segment.startsWith(".") && !pattern.contains("/.") && !pattern.startsWith(".")) {
                return false;
            }
        }
        return Glob.match(pattern, path).matches();
    }

    private static boolean ignored(String location) {
        for (String segment : location.split("/")) {
            for (String ignoredPath : IGNORED_PATHS) {
                if (ignoredPath.equals(segment)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void insert(String key, Entry value) {
        map.put(key, value);
    }

    private static @Nullable Entry processWorkspaceName(Log log, String manifest, String inputPath) {
        Json.Expr workspaceJson;
        try {
            workspaceJson = Json.parse(manifest);
        } catch (Json.SyntaxError e) {
            log.addError("ParserError reading package.json for workspace package \"" + inputPath + "\"");
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
