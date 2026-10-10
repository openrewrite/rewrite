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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.LocaleCompare;
import org.openrewrite.javascript.internal.lock.Minimatch;
import org.openrewrite.javascript.internal.lock.PosixPath;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/map-workspaces 5.0.3: the workspace members a root's {@code workspaces} patterns
 * name, over the project's manifests in place of its directory tree, and over the lock.
 */
final class MapWorkspaces {

    /**
     * The errors map-workspaces throws, with their codes.
     */
    @Getter
    public static final class WorkspacesException extends IllegalArgumentException implements NpmError {
        private final String code;

        WorkspacesException(String message, String code) {
            super(message);
            this.code = code;
        }

        @Override
        public String getCode() {
            return code;
        }
    }

    private static final Minimatch.Options PARTIAL =
            Minimatch.Options.builder(Minimatch.Release.V10_2_5).partial(true).windowsPathsNoEscape(true).build();
    private static final Minimatch.Options IGNORE =
            Minimatch.Options.builder(Minimatch.Release.V10_2_5).dot(true).nocomment(true).nonegate(true).build();

    private MapWorkspaces() {
    }

    private static final class Patterns {
        final List<String> patterns = new ArrayList<>();
        final List<String> negatedPatterns = new ArrayList<>();
    }

    private static Patterns appendNegatedPatterns(List<?> allPatterns) {
        Patterns out = new Patterns();
        for (Object raw : allPatterns) {
            if (!(raw instanceof String)) {
                Js.get(raw, "match");
                throw new IllegalArgumentException("pattern.match is not a function");
            }
            String pattern = (String) raw;
            int excl = 0;
            while (excl < pattern.length() && pattern.charAt(excl) == '!') {
                excl++;
            }
            pattern = pattern.substring(excl);
            pattern = pattern.replaceFirst("^\\.?/+", "");
            boolean negate = excl % 2 == 1;
            if (negate) {
                out.negatedPatterns.add(pattern);
            } else {
                for (int i = 0; i < out.negatedPatterns.size(); i++) {
                    if (Minimatch.minimatch(pattern, out.negatedPatterns.get(i), Minimatch.Release.V10_2_5)) {
                        out.negatedPatterns.remove(i);
                    }
                }
                out.patterns.add(pattern);
            }
        }
        for (String negated : out.negatedPatterns) {
            for (String pattern : Minimatch.match(new ArrayList<>(out.patterns), negated, Minimatch.Release.V10_2_5)) {
                out.patterns.remove(pattern);
            }
        }
        return out;
    }

    private static Patterns getPatterns(@Nullable Object workspaces) {
        Object packages = Js.get(workspaces, "packages");
        Object declaration = packages instanceof List ? packages : workspaces;
        if (!(declaration instanceof List)) {
            throw new WorkspacesException("workspaces config expects an Array", "EWORKSPACESCONFIG");
        }
        return appendNegatedPatterns((List<?>) declaration);
    }

    private static Object getPackageName(@Nullable Object pkg, String pathname) {
        Object name = Js.get(pkg, "name");
        return truthy(name) ? name : Node.nameFromFolder(pathname);
    }

    /**
     * {@code mapWorkspaces({cwd, pkg})} over the locations holding a package.json ({@code ""} the root): package name
     * to absolute path, in the order the patterns list them.
     */
    static Map<Object, String> mapWorkspaces(String cwd, Map<String, Object> pkg,
            VirtualTreeLoader.ProjectFiles files) {
        Object workspaces = pkg.containsKey("workspaces") ? pkg.get("workspaces") : new ArrayList<>();
        Patterns p = getPatterns(workspaces);
        Map<Object, String> results = new LinkedHashMap<>();
        if (p.patterns.isEmpty() && p.negatedPatterns.isEmpty()) {
            return results;
        }

        List<String> ignore = new ArrayList<>();
        ignore.add("**/node_modules/**");
        ignore.addAll(p.negatedPatterns);
        List<String> matches = new ArrayList<>();
        for (String location : files.manifestLocations()) {
            String dir = location.isEmpty() ? "." : location;
            if (ignored(dir, ignore)) {
                continue;
            }
            for (String pattern : p.patterns) {
                if (Minimatch.minimatch(dir, globPattern(pattern), Minimatch.Release.V10_2_5)) {
                    matches.add(dir);
                    break;
                }
            }
        }
        matches.sort(LocaleCompare::en);

        List<String> orderedMatches = new ArrayList<>();
        for (String pattern : p.patterns) {
            for (String m : matches) {
                if (Minimatch.minimatch(m, pattern, PARTIAL)) {
                    orderedMatches.add(m);
                }
            }
        }

        Map<Object, Set<String>> seen = new LinkedHashMap<>();
        for (String match : orderedMatches) {
            String location = ".".equals(match) ? "" : match;
            Map<String, Object> content;
            try {
                PackageJson manifest = files.manifest(location);
                if (manifest == null) {
                    continue;
                }
                content = manifest.getContent();
            } catch (PackageJson.NeedsPackageFiles e) {
                // the bin npm reads from the files does not name the workspace
                content = e.getContent();
            }
            String path = PosixPath.join(cwd, match);
            Object name = getPackageName(content, path);
            seen.computeIfAbsent(name, k -> new LinkedHashSet<>()).add(path);
        }

        List<String> errors = new ArrayList<>();
        errors.add("must not have multiple workspaces with the same name");
        Map<Object, String> byName = new LinkedHashMap<>();
        for (Map.Entry<Object, Set<String>> entry : seen.entrySet()) {
            if (entry.getValue().size() > 1) {
                errors.add("package '" + Js.string(entry.getKey()) + "' has conflicts in the following paths:");
                for (String pathname : entry.getValue()) {
                    errors.add("    " + pathname);
                }
            } else {
                byName.put(entry.getKey(), entry.getValue().iterator().next());
            }
        }
        if (errors.size() > 1) {
            throw new WorkspacesException(String.join("\n", errors), "EDUPLICATEWORKSPACE");
        }
        results.putAll(byName);
        return results;
    }

    private static String globPattern(String pattern) {
        return pattern.endsWith("/") ? pattern.substring(0, pattern.length() - 1) : pattern;
    }

    // glob's Ignore: a pattern matches the directory or its trailing-slash form, and one ending in ** ignores children
    private static boolean ignored(String dir, List<String> ignore) {
        for (String pattern : ignore) {
            Minimatch mm = new Minimatch(pattern, IGNORE);
            if (mm.match(dir) || mm.match(dir + "/")) {
                return true;
            }
            if (pattern.endsWith("/**") || "**".equals(pattern)) {
                String ancestor = PosixPath.dirname(dir);
                while (!".".equals(ancestor) && !ancestor.isEmpty()) {
                    if (mm.match(ancestor + "/")) {
                        return true;
                    }
                    ancestor = PosixPath.dirname(ancestor);
                }
            }
        }
        return false;
    }

    /**
     * {@code mapWorkspaces.virtual({cwd, lockfile})}: the members the lock's root entry and link
     * entries describe.
     */
    static Map<String, String> virtual(String cwd, Map<String, Object> lockfile) {
        Object packages = truthy(lockfile.get("packages")) ? lockfile.get("packages") : new LinkedHashMap<>();
        Object root = Js.get(packages, "");
        Object workspaces = root instanceof Map && ((Map<?, ?>) root).containsKey("workspaces") ?
                ((Map<?, ?>) root).get("workspaces") : new ArrayList<>();
        Map<String, String> results = new LinkedHashMap<>();
        Patterns p = getPatterns(workspaces);
        if (p.patterns.isEmpty() && p.negatedPatterns.isEmpty()) {
            return results;
        }
        p.negatedPatterns.add("**/node_modules/**");

        List<String> packageKeys = new ArrayList<>(Js.keys(packages));
        for (String pattern : p.negatedPatterns) {
            for (String key : Minimatch.match(new ArrayList<>(packageKeys), pattern, Minimatch.Release.V10_2_5)) {
                packageKeys.remove(key);
            }
        }

        Map<String, String> byPath = new LinkedHashMap<>();
        for (String pattern : p.patterns) {
            for (String key : Minimatch.match(packageKeys, pattern, Minimatch.Release.V10_2_5)) {
                String pathname = PosixPath.join(cwd, key);
                byPath.put(pathname, Js.string(getPackageName(Js.get(packages, key), pathname)));
            }
        }

        for (Map.Entry<String, String> e : byPath.entrySet()) {
            results.put(e.getValue(), e.getKey());
        }
        return results;
    }
}
