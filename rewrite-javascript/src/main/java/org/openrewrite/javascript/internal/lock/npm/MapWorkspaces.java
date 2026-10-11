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

import java.util.AbstractMap;
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
     * The @npmcli/map-workspaces release whose answers these are: 5.0.3, which npm 11.17.0 bundles and every release
     * from 3.0.6 reads as here; 3.0.4, which applies the patterns one after another, a negated one taking away what
     * the ones before it found, as every release from 2.0.2 does; 2.0.1, which refuses a name two paths share as soon
     * as a pattern finds the second; 2.0.0, which also reads a backslash in a pattern as glob's escape, as 1.0.4 does;
     * or 1.0.3, whose refusal of a name two paths share names neither.
     */
    enum Release {
        V1_0_3, V2_0_0, V2_0_1, V3_0_4, V5_0_3
    }

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

    private MapWorkspaces() {
    }

    // glob's ignore option as 3.0.4 passes it
    private static final Minimatch.Options IGNORE_3_0_4 =
            Minimatch.Options.builder(Minimatch.Release.V10_2_5).dot(true).nocomment(true).nonegate(true).build();

    // minimatch 10.2.5 with the brace-expansion the npm release bundles beside it
    private static Minimatch.Options.OptionsBuilder minimatch(NpmRelease release) {
        return Minimatch.Options.builder(Minimatch.Release.V10_2_5).braceExpansion(release::braceExpansion);
    }

    // a declaration without a pattern, for which no release of the library finds a member: the same reading of
    // workspaces.packages that every release starts with
    private static boolean declaresNone(@Nullable Object workspaces) {
        Object packages = Js.get(workspaces, "packages");
        Object declaration = packages instanceof List ? packages : workspaces;
        return declaration instanceof List && ((List<?>) declaration).isEmpty();
    }

    private static final class Patterns {
        final List<String> patterns = new ArrayList<>();
        final List<String> negatedPatterns = new ArrayList<>();
    }

    private static Patterns appendNegatedPatterns(List<?> allPatterns, NpmRelease release) {
        Minimatch.Options options = minimatch(release).build();
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
                    if (Minimatch.minimatch(pattern, out.negatedPatterns.get(i), options)) {
                        out.negatedPatterns.remove(i);
                    }
                }
                out.patterns.add(pattern);
            }
        }
        for (String negated : out.negatedPatterns) {
            for (String pattern : Minimatch.match(new ArrayList<>(out.patterns), negated, options)) {
                out.patterns.remove(pattern);
            }
        }
        return out;
    }

    private static Patterns getPatterns(@Nullable Object workspaces, NpmRelease release) {
        Object packages = Js.get(workspaces, "packages");
        Object declaration = packages instanceof List ? packages : workspaces;
        if (!(declaration instanceof List)) {
            throw new WorkspacesException("workspaces config expects an Array", "EWORKSPACESCONFIG");
        }
        return appendNegatedPatterns((List<?>) declaration, release);
    }

    private static Object getPackageName(@Nullable Object pkg, String pathname) {
        Object name = Js.get(pkg, "name");
        return truthy(name) ? name : Node.nameFromFolder(pathname);
    }

    /**
     * {@code mapWorkspaces({cwd, pkg})} over the locations holding a package.json ({@code ""} the root): package name
     * to absolute path, in the order the patterns list them, as the npm {@code release} bundles the library.
     */
    static Map<Object, String> mapWorkspaces(String cwd, Map<String, Object> pkg,
            VirtualTreeLoader.ProjectFiles files, NpmRelease release) {
        Object workspaces = pkg.containsKey("workspaces") ? pkg.get("workspaces") : new ArrayList<>();
        if (declaresNone(workspaces)) {
            return new LinkedHashMap<>();
        }
        if (release.mapWorkspaces() != Release.V5_0_3) {
            return mapWorkspaces3_0_4(cwd, workspaces, files, release.mapWorkspaces());
        }
        Patterns p = getPatterns(workspaces, release);
        Map<Object, String> results = new LinkedHashMap<>();
        if (p.patterns.isEmpty() && p.negatedPatterns.isEmpty()) {
            return results;
        }

        Minimatch.Options options = minimatch(release).build();
        Minimatch.Options partial = minimatch(release).partial(true).windowsPathsNoEscape(true).build();
        Minimatch.Options ignoring = minimatch(release).dot(true).nocomment(true).nonegate(true).build();
        List<String> ignore = new ArrayList<>();
        ignore.add("**/node_modules/**");
        ignore.addAll(p.negatedPatterns);
        List<String> matches = new ArrayList<>();
        for (String location : files.manifestLocations()) {
            String dir = location.isEmpty() ? "." : location;
            if (ignored(dir, ignore, ignoring)) {
                continue;
            }
            for (String pattern : p.patterns) {
                if (Minimatch.minimatch(dir, globPattern(pattern), options)) {
                    matches.add(dir);
                    break;
                }
            }
        }
        matches.sort(LocaleCompare::en);

        List<String> orderedMatches = new ArrayList<>();
        for (String pattern : p.patterns) {
            for (String m : matches) {
                if (Minimatch.minimatch(m, pattern, partial)) {
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

    // 3.0.4's appendNegatedPatterns: every pattern in its place, only a leading / stripped
    private static List<Map.Entry<String, Boolean>> patterns3_0_4(@Nullable Object workspaces) {
        Object packages = Js.get(workspaces, "packages");
        Object declaration = packages instanceof List ? packages : workspaces;
        if (!(declaration instanceof List)) {
            throw new WorkspacesException("workspaces config expects an Array", "EWORKSPACESCONFIG");
        }
        List<Map.Entry<String, Boolean>> results = new ArrayList<>();
        for (Object raw : (List<?>) declaration) {
            if (!(raw instanceof String)) {
                Js.get(raw, "match");
                throw new IllegalArgumentException("pattern.match is not a function");
            }
            String pattern = (String) raw;
            int excl = 0;
            while (excl < pattern.length() && pattern.charAt(excl) == '!') {
                excl++;
            }
            pattern = pattern.substring(excl).replaceFirst("^/+", "");
            results.add(new AbstractMap.SimpleEntry<>(pattern, excl % 2 == 1));
        }
        return results;
    }

    // glob(getGlobPattern(pattern)): a leading ./ and a trailing / name nothing, and . and a lone ** find the
    // directory itself; before 2.0.1 a backslash stays glob's escape
    private static boolean globs3_0_4(String dir, String pattern, Release release) {
        String glob = release == Release.V1_0_3 || release == Release.V2_0_0 ? pattern : pattern.replace('\\', '/');
        while (glob.startsWith("./")) {
            glob = glob.substring(2);
        }
        while (glob.endsWith("/")) {
            glob = glob.substring(0, glob.length() - 1);
        }
        if (".".equals(dir)) {
            return glob.isEmpty() || ".".equals(glob) || "**".equals(glob);
        }
        return !glob.isEmpty() && Minimatch.minimatch(dir, glob, Minimatch.Release.V10_2_5);
    }

    private static Map<Object, String> mapWorkspaces3_0_4(String cwd, @Nullable Object workspaces,
            VirtualTreeLoader.ProjectFiles files, Release release) {
        List<Map.Entry<String, Boolean>> patterns = patterns3_0_4(workspaces);
        Map<Object, String> results = new LinkedHashMap<>();
        if (patterns.isEmpty()) {
            return results;
        }
        List<String> ignore = new ArrayList<>();
        ignore.add("**/node_modules/**");

        Map<Object, Set<String>> seen = new LinkedHashMap<>();
        // before 2.0.2: the pathname each name was first found at, and the results by pathname
        Map<Object, String> firstSeen = new LinkedHashMap<>();
        Map<String, Object> byPathname = new LinkedHashMap<>();
        boolean refusesAtOnce = release == Release.V1_0_3 || release == Release.V2_0_0 || release == Release.V2_0_1;
        for (Map.Entry<String, Boolean> item : patterns) {
            List<String> matches = new ArrayList<>();
            for (String location : files.manifestLocations()) {
                String dir = location.isEmpty() ? "." : location;
                if (!ignored(dir, ignore, IGNORE_3_0_4) && globs3_0_4(dir, item.getKey(), release)) {
                    matches.add(dir);
                }
            }
            matches.sort(LocaleCompare::en);

            for (String match : matches) {
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
                if (refusesAtOnce) {
                    if (item.getValue()) {
                        byPathname.remove(path);
                        continue;
                    }
                    String prior = firstSeen.get(name);
                    if (prior != null && !prior.equals(path)) {
                        throw new WorkspacesException(release == Release.V1_0_3 ?
                                "must not have multiple workspaces with the same name" : String.join("\n",
                                "must not have multiple workspaces with the same name",
                                "package '" + Js.string(name) + "' has conflicts in the following paths:",
                                "    " + prior, "    " + path), "EDUPLICATEWORKSPACE");
                    }
                    firstSeen.put(name, path);
                    byPathname.put(path, name);
                    continue;
                }
                Set<String> seenPackagePathnames = seen.computeIfAbsent(name, k -> new LinkedHashSet<>());
                if (item.getValue()) {
                    seenPackagePathnames.remove(path);
                } else {
                    seenPackagePathnames.add(path);
                }
            }
        }

        if (refusesAtOnce) {
            // reverseResultMap
            for (Map.Entry<String, Object> entry : byPathname.entrySet()) {
                results.put(entry.getValue(), entry.getKey());
            }
            return results;
        }

        List<String> errors = new ArrayList<>();
        errors.add("must not have multiple workspaces with the same name");
        for (Map.Entry<Object, Set<String>> entry : seen.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            if (entry.getValue().size() > 1) {
                errors.add("package '" + Js.string(entry.getKey()) + "' has conflicts in the following paths:");
                for (String pathname : entry.getValue()) {
                    errors.add("    " + pathname);
                }
            } else {
                results.put(entry.getKey(), entry.getValue().iterator().next());
            }
        }
        if (errors.size() > 1) {
            throw new WorkspacesException(String.join("\n", errors), "EDUPLICATEWORKSPACE");
        }
        return results;
    }

    private static Map<String, String> virtual3_0_4(String cwd, Object packages, @Nullable Object workspaces) {
        Map<String, String> results = new LinkedHashMap<>();
        List<Map.Entry<String, Boolean>> patterns = patterns3_0_4(workspaces);
        if (patterns.isEmpty()) {
            return results;
        }
        patterns.add(new AbstractMap.SimpleEntry<>("**/node_modules/**", true));

        Map<String, String> byPath = new LinkedHashMap<>();
        for (String packageKey : Js.keys(packages)) {
            if (packageKey.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, Boolean> item : patterns) {
                if (Minimatch.minimatch(packageKey, item.getKey(), Minimatch.Release.V10_2_5)) {
                    String pathname = PosixPath.join(cwd, packageKey);
                    if (item.getValue()) {
                        byPath.remove(pathname);
                    } else {
                        byPath.put(pathname, Js.string(getPackageName(Js.get(packages, packageKey), pathname)));
                    }
                }
            }
        }
        for (Map.Entry<String, String> e : byPath.entrySet()) {
            results.put(e.getValue(), e.getKey());
        }
        return results;
    }

    // glob's Ignore: a pattern matches the directory or its trailing-slash form, and one ending in ** ignores children
    private static boolean ignored(String dir, List<String> ignore, Minimatch.Options options) {
        for (String pattern : ignore) {
            Minimatch mm = new Minimatch(pattern, options);
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
     * entries describe, as the npm {@code release} bundles the library.
     */
    static Map<String, String> virtual(String cwd, Map<String, Object> lockfile, NpmRelease release) {
        Object packages = truthy(lockfile.get("packages")) ? lockfile.get("packages") : new LinkedHashMap<>();
        Object root = Js.get(packages, "");
        Object workspaces = root instanceof Map && ((Map<?, ?>) root).containsKey("workspaces") ?
                ((Map<?, ?>) root).get("workspaces") : new ArrayList<>();
        if (declaresNone(workspaces)) {
            return new LinkedHashMap<>();
        }
        if (release.mapWorkspaces() != Release.V5_0_3) {
            // virtual reads alike from 2.0.0 to 3.0.4
            return virtual3_0_4(cwd, packages, workspaces);
        }
        Map<String, String> results = new LinkedHashMap<>();
        Patterns p = getPatterns(workspaces, release);
        if (p.patterns.isEmpty() && p.negatedPatterns.isEmpty()) {
            return results;
        }
        p.negatedPatterns.add("**/node_modules/**");

        Minimatch.Options options = minimatch(release).build();
        List<String> packageKeys = new ArrayList<>(Js.keys(packages));
        for (String pattern : p.negatedPatterns) {
            for (String key : Minimatch.match(new ArrayList<>(packageKeys), pattern, options)) {
                packageKeys.remove(key);
            }
        }

        Map<String, String> byPath = new LinkedHashMap<>();
        for (String pattern : p.patterns) {
            for (String key : Minimatch.match(packageKeys, pattern, options)) {
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
