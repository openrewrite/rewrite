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
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Port of @pnpm/workspace.workspace-manifest-writer src/index.ts (pnpm 11.2.2, the release's writer otherwise): where
 * it puts a new key and its blank line, and how its yaml writes a new scalar ({@code singleQuote: true}).
 */
public final class WorkspaceManifestWriter {

    private static final Pattern PLAIN_UNSAFE = Pattern.compile(
            "^[\\n\\t ,\\[\\]{}#&*!|>'\"%@`]|^[?-]$|^[?-][ \\t]|[\\n:][ \\t]|[ \\t]\\n|[\\n\\t ]#|[\\n\\t :]$");
    private static final Pattern CORE_SCHEMA = Pattern.compile(
            "^(?:~|[Nn]ull|NULL)?$|^(?:[Tt]rue|TRUE|[Ff]alse|FALSE)$|^0o[0-7]+$|^[-+]?[0-9]+$|^0x[0-9a-fA-F]+$|" +
            "^(?:[-+]?\\.(?:inf|Inf|INF)|\\.nan|\\.NaN|\\.NAN)$|" +
            "^[-+]?(?:\\.[0-9]+|[0-9]+(?:\\.[0-9]*)?)[eE][-+]?[0-9]+$|^[-+]?(?:\\.[0-9]+|[0-9]+\\.[0-9]*)$");

    private static final List<String> DEPENDENCY_TYPES = Arrays.asList("dependencies", "devDependencies",
            "optionalDependencies", "peerDependencies");

    private final int release;

    /**
     * The writer of pnpm {@code release} (a major): 11 keeps an unsorted level's order, 10 sorts every level.
     */
    public WorkspaceManifestWriter(int release) {
        this.release = release;
    }

    /**
     * {@code reorderRecursive}: the index among a level's {@code keys} a new {@code key} takes, sorted in where the
     * keys are sorted (packages first or not) and last otherwise; pnpm 10 sorts every level, packages first.
     */
    public int insertionIndex(List<String> keys, String key) {
        if (release >= 11 && !sorted(keys)) {
            return keys.size();
        }
        boolean packagesFirst = release < 11 || keys.isEmpty() || "packages".equals(keys.get(0));
        int at = 0;
        for (String existing : keys) {
            if (compare(existing, key, packagesFirst) < 0) {
                at++;
            }
        }
        return at;
    }

    /**
     * {@code propagateBlankLinesToNewPairs}: whether a new top-level pair inserted at {@code at} gets a blank line
     * before it, given whether each existing pair has one.
     */
    public boolean blankBefore(List<Boolean> blankBefore, int at) {
        if (release < 11 || at == 0) {
            return false;
        }
        return blankLineStyle(blankBefore) ||
               (at < blankBefore.size() ? blankBefore.get(at) : blankBefore.get(at - 1));
    }

    /**
     * Whether the document separates its top-level pairs by blank lines, read from every pair after the first, so
     * that a pair a new one displaces from the front gets one too.
     */
    public boolean blankLineStyle(List<Boolean> blankBefore) {
        if (release < 11 || blankBefore.size() < 2) {
            return false;
        }
        for (int i = 1; i < blankBefore.size(); i++) {
            if (!blankBefore.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * How the yaml library writes {@code value} as a new scalar: plain where it reads back as that string, else
     * single quoted, double quoted when it holds only a single quote; refused where it would need an escape.
     */
    public static String scalar(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c >= 0x7f && c <= 0x9f || c == 0x2028 || c == 0x2029) {
                throw new IllegalArgumentException("pnpm-workspace.yaml would need an escape for " + value);
            }
        }
        if (PLAIN_UNSAFE.matcher(value).find() || CORE_SCHEMA.matcher(value).find()) {
            return value.contains("'") && !value.contains("\"") ? doubleQuoted(value) :
                    "'" + value.replace("'", "''") + "'";
        }
        return value;
    }

    /**
     * updateWorkspaceManifest's {@code cleanupUnusedCatalogs} over installDeps' allProjects, those of {@code projects}
     * (by directory, null unread) the manifest's packages match; true when it removed anything from {@code manifest}.
     */
    public static boolean cleanupUnusedCatalogs(Map<String, @Nullable Object> manifest,
                                                Map<String, @Nullable Map<String, @Nullable Object>> projects) {
        List<Map<String, @Nullable Object>> allProjects = new ArrayList<>();
        for (String dir : FindPackages.workspaceProjects(manifest, projects.keySet())) {
            Map<String, @Nullable Object> project = projects.get(dir);
            if (project == null) {
                throw CannotKnow.unread(dir.isEmpty() ? "the root project's manifest" : dir + "'s manifest");
            }
            allProjects.add(project);
        }
        return removePackagesFromWorkspaceCatalog(manifest, allProjects);
    }

    @SuppressWarnings("unchecked")
    static boolean removePackagesFromWorkspaceCatalog(Map<String, @Nullable Object> manifest,
                                                      List<Map<String, @Nullable Object>> packagesJson) {
        boolean shouldBeUpdated = false;
        if (packagesJson.isEmpty() || manifest.get("catalog") == null && manifest.get("catalogs") == null) {
            return shouldBeUpdated;
        }
        Map<String, Set<Object>> packageReferences = new HashMap<>();
        for (Map<String, @Nullable Object> pkg : packagesJson) {
            for (String type : DEPENDENCY_TYPES) {
                Object deps = pkg.get(type);
                if (!Js.truthy(deps) || !(deps instanceof Map)) {
                    continue;
                }
                for (String pkgName : Js.keys(deps)) {
                    addPackageReference(packageReferences, pkgName, ((Map<String, ?>) deps).get(pkgName));
                }
            }
        }
        Object overrides = manifest.get("overrides");
        if (overrides instanceof Map) {
            for (String selector : Js.keys(overrides)) {
                Object version = ((Map<String, ?>) overrides).get(selector);
                if (!(version instanceof String)) {
                    throw new JsError(JsError.TYPE_ERROR, "version.startsWith is not a function");
                }
                if (!((String) version).startsWith("catalog:")) {
                    continue;
                }
                String pkgName;
                try {
                    pkgName = ParseOverrides.parsePkgAndParentSelector(selector)[1].getName();
                } catch (PnpmError e) {
                    continue;
                }
                addPackageReference(packageReferences, pkgName, version);
            }
        }
        Object catalog = manifest.get("catalog");
        if (Js.truthy(catalog) && catalog instanceof Map) {
            for (String pkg : Js.keys(catalog)) {
                Set<Object> references = packageReferences.get(pkg);
                if (references == null || !references.contains("catalog:")) {
                    ((Map<String, ?>) catalog).remove(pkg);
                    shouldBeUpdated = true;
                }
            }
            if (((Map<?, ?>) catalog).isEmpty()) {
                manifest.remove("catalog");
                shouldBeUpdated = true;
            }
        }
        Object catalogs = manifest.get("catalogs");
        if (Js.truthy(catalogs) && catalogs instanceof Map) {
            List<String> catalogsToRemove = new ArrayList<>();
            for (String catalogName : Js.keys(catalogs)) {
                Object named = ((Map<String, ?>) catalogs).get(catalogName);
                if (!Js.truthy(named) || !(named instanceof Map)) {
                    continue;
                }
                for (String pkg : Js.keys(named)) {
                    Set<Object> references = packageReferences.get(pkg);
                    if (references == null || !references.contains("catalog:" + catalogName) &&
                                              !references.contains("catalog:")) {
                        ((Map<String, ?>) named).remove(pkg);
                        shouldBeUpdated = true;
                    }
                }
                if (((Map<?, ?>) named).isEmpty()) {
                    catalogsToRemove.add(catalogName);
                    shouldBeUpdated = true;
                }
            }
            for (String catalogName : catalogsToRemove) {
                ((Map<String, ?>) catalogs).remove(catalogName);
            }
            if (((Map<?, ?>) catalogs).isEmpty()) {
                manifest.remove("catalogs");
                shouldBeUpdated = true;
            }
        }
        return shouldBeUpdated;
    }

    private static void addPackageReference(Map<String, Set<Object>> packageReferences, String pkgName,
                                            @Nullable Object version) {
        packageReferences.computeIfAbsent(pkgName, k -> new HashSet<>()).add(version == null ? "undefined" : version);
    }

    public static String doubleQuoted(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    // detectKeyLayout: alphabetical after an optional leading `packages`, by lexCompare
    private static boolean sorted(List<String> keys) {
        int start = !keys.isEmpty() && "packages".equals(keys.get(0)) ? 1 : 0;
        for (int i = start + 1; i < keys.size(); i++) {
            if (KeySorting.lexCompare(keys.get(i - 1), keys.get(i)) > 0) {
                return false;
            }
        }
        return true;
    }

    private static int compare(String left, String right, boolean packagesFirst) {
        if (packagesFirst && "packages".equals(left) != "packages".equals(right)) {
            return "packages".equals(left) ? -1 : 1;
        }
        return KeySorting.lexCompare(left, right);
    }
}
