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
import org.openrewrite.javascript.internal.lock.EcmaScript;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.YamlDocument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of @pnpm/workspace.workspace-manifest-writer src/index.ts (pnpm 11.2.2, the release's writer otherwise): where
 * it puts a new key and its blank line.
 */
public final class WorkspaceManifestWriter {

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
     * The rest of updateWorkspaceManifest ({@code original} is captureKeyOrder's): pnpm 10's sortKeysByPriority or 11's
     * reorderRecursive, patchDocument, then for 11 propagateBlankLinesToNewPairs.
     */
    public void write(YamlDocument document, @Nullable Object original, Map<String, @Nullable Object> manifest) {
        if (release < 11) {
            document.patch(sorted(manifest));
            return;
        }
        document.patch(reordered(original, manifest));
        propagateBlankLinesToNewPairs(document, original instanceof Map ? EcmaScript.keys(asMap(original)) :
                Collections.emptyList());
    }

    private @Nullable Object sorted(@Nullable Object value) {
        if (value instanceof List) {
            List<@Nullable Object> items = new ArrayList<>();
            for (Object item : (List<?>) value) {
                items.add(sorted(item));
            }
            return items;
        }
        if (!(value instanceof Map)) {
            return value;
        }
        List<String> keys = new ArrayList<>();
        for (String key : EcmaScript.keys(asMap(value))) {
            keys.add(insertionIndex(keys, key), key);
        }
        Map<String, @Nullable Object> result = new LinkedHashMap<>();
        for (String key : keys) {
            result.put(key, sorted(asMap(value).get(key)));
        }
        return result;
    }

    private @Nullable Object reordered(@Nullable Object original, @Nullable Object current) {
        if (!(current instanceof Map)) {
            return current;
        }
        Map<String, @Nullable Object> order = original instanceof Map ? asMap(original) : new LinkedHashMap<>();
        List<String> keys = new ArrayList<>();
        for (String key : EcmaScript.keys(order)) {
            if (asMap(current).containsKey(key)) {
                keys.add(key);
            }
        }
        for (String key : EcmaScript.keys(asMap(current))) {
            if (!order.containsKey(key)) {
                keys.add(insertionIndex(keys, key), key);
            }
        }
        Map<String, @Nullable Object> result = new LinkedHashMap<>();
        for (String key : keys) {
            result.put(key, reordered(order.get(key), asMap(current).get(key)));
        }
        return result;
    }

    /**
     * {@code propagateBlankLinesToNewPairs}: a top-level pair the document did not hold before gets a blank line where
     * the document separates its pairs by them, or where its neighbour has one.
     */
    void propagateBlankLinesToNewPairs(YamlDocument document, List<String> originalKeys) {
        List<String> items = document.keys();
        Set<String> original = new HashSet<>(originalKeys);
        String originalFirst = originalKeys.isEmpty() ? null : originalKeys.get(0);
        int nonFirst = 0;
        int nonFirstWithBlank = 0;
        for (int i = 0; i < items.size(); i++) {
            if (original.contains(items.get(i)) && !items.get(i).equals(originalFirst)) {
                nonFirst++;
                nonFirstWithBlank += document.blankBefore(i) ? 1 : 0;
            }
        }
        boolean blankLineStyle = nonFirst > 0 && nonFirstWithBlank == nonFirst;
        for (int i = 1; i < items.size(); i++) {
            if (document.blankBefore(i) || !blankLineStyle && original.contains(items.get(i))) {
                continue;
            }
            boolean last = i + 1 == items.size();
            if (blankLineStyle || !last && document.blankBefore(i + 1) || last && document.blankBefore(i - 1)) {
                document.blankLineBefore(i);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable Object> asMap(Object value) {
        return (Map<String, @Nullable Object>) value;
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
