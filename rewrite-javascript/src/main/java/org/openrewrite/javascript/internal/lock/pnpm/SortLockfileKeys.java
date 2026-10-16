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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of @pnpm/lockfile.fs src/sortLockfileKeys.ts (pnpm 11.2.2), over the lock in its file form.
 */
final class SortLockfileKeys {

    private static final List<String> ORDERED = Arrays.asList("resolution", "id", "name", "version", "engines", "cpu",
            "os", "libc", "deprecated", "hasBin", "prepare", "requiresBuild", "bundleDependencies", "peerDependencies",
            "peerDependenciesMeta", "dependencies", "optionalDependencies", "transitivePeerDependencies", "dev",
            "optional");

    private static final List<String> ROOT_KEYS = Arrays.asList("lockfileVersion", "settings", "catalogs",
            "overrides", "packageExtensionsChecksum", "pnpmfileChecksum", "patchedDependencies", "importers",
            "packages");

    static final Map<String, Integer> ORDERED_KEYS = new HashMap<>();
    static final Map<String, Integer> ROOT_KEYS_ORDER = new HashMap<>();

    static {
        for (int i = 0; i < ORDERED.size(); i++) {
            ORDERED_KEYS.put(ORDERED.get(i), i + 1);
        }
        for (int i = 0; i < ROOT_KEYS.size(); i++) {
            ROOT_KEYS_ORDER.put(ROOT_KEYS.get(i), i);
        }
    }

    private SortLockfileKeys() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> sortLockfileKeys(Map<String, @Nullable Object> lockfile) {
        Object importers = lockfile.get("importers");
        if (importers instanceof Map) {
            Map<String, @Nullable Object> sorted = KeySorting.sortDirectKeys((Map<String, @Nullable Object>) importers);
            for (String importerId : Js.keys(sorted)) {
                sorted.put(importerId, sortByPriority(ROOT_KEYS_ORDER, sorted.get(importerId)));
            }
            lockfile.put("importers", sorted);
        }
        for (String section : new String[]{"packages", "snapshots"}) {
            Object entries = lockfile.get(section);
            if (entries instanceof Map) {
                Map<String, @Nullable Object> sorted =
                        KeySorting.sortDirectKeys((Map<String, @Nullable Object>) entries);
                for (String pkgId : Js.keys(sorted)) {
                    sorted.put(pkgId, sortByPriority(ORDERED_KEYS, sorted.get(pkgId)));
                }
                lockfile.put(section, sorted);
            }
        }
        Object catalogs = lockfile.get("catalogs");
        if (lockfile.containsKey("catalogs") && catalogs instanceof Map) {
            Map<String, @Nullable Object> sorted = KeySorting.sortDirectKeys((Map<String, @Nullable Object>) catalogs);
            for (String catalogName : Js.keys(sorted)) {
                Object catalog = sorted.get(catalogName);
                sorted.put(catalogName, catalog instanceof Map ?
                        KeySorting.sortDeepKeys((Map<String, @Nullable Object>) catalog) : catalog);
            }
            lockfile.put("catalogs", sorted);
        }
        for (String key : new String[]{"time", "patchedDependencies"}) {
            Object value = lockfile.get(key);
            if (lockfile.containsKey(key) && value instanceof Map) {
                lockfile.put(key, KeySorting.sortDirectKeys((Map<String, @Nullable Object>) value));
            }
        }
        return KeySorting.sortKeysByPriority(ROOT_KEYS_ORDER, false, lockfile);
    }

    // the file form holds an object for every importer and entry
    @SuppressWarnings("unchecked")
    private static Object sortByPriority(Map<String, Integer> priority, @Nullable Object value) {
        return KeySorting.sortKeysByPriority(priority, true, (Map<String, @Nullable Object>) value);
    }
}
