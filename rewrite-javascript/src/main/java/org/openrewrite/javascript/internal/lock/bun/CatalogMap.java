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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of bun's src/install/lockfile/CatalogMap.zig (bun 1.3.10): the default catalog and the named groups, each
 * from a dependency name to its dependency.
 */
final class CatalogMap {

    /**
     * The catalog group a dependency's version names, as Dependency.Version.parse reads a catalog reference; {@code ""}
     * the default one, null for no catalog reference.
     */
    static @Nullable String groupOf(String dependency) {
        if (DependencyVersion.Tag.infer(dependency) != DependencyVersion.Tag.CATALOG) {
            return null;
        }
        return Strings.trim(dependency.substring("catalog:".length()), Strings.WHITESPACE_CHARS);
    }

    LinkedHashMap<String, Dependency> defaultMap = new LinkedHashMap<>();
    LinkedHashMap<String, LinkedHashMap<String, Dependency>> groups = new LinkedHashMap<>();

    boolean hasAny() {
        return !defaultMap.isEmpty() || !groups.isEmpty();
    }

    /**
     * The catalog entry of a dependency, as the release looks it up.
     */
    @Nullable
    Dependency get(String catalogName, String depName, BunRelease release) {
        if (release.looksUpTheDefaultCatalogUnderBothNames()) {
            // locate: the default catalog under both spellings, the one the name matches first
            if (catalogName.isEmpty()) {
                Dependency found = defaultMap.get(depName);
                return found != null ? found : inGroup("default", depName);
            }
            Dependency found = inGroup(catalogName, depName);
            return found != null || !"default".equals(catalogName) ? found : defaultMap.get(depName);
        }
        return get(catalogName, depName);
    }

    private @Nullable Dependency inGroup(String catalogName, String depName) {
        LinkedHashMap<String, Dependency> group = groups.get(catalogName);
        return group == null ? null : group.get(depName);
    }

    @Nullable
    Dependency get(String catalogName, String depName) {
        if (catalogName.isEmpty()) {
            if (defaultMap.isEmpty()) {
                return null;
            }
            return defaultMap.get(depName);
        }
        LinkedHashMap<String, Dependency> group = groups.get(catalogName);
        if (group == null || group.isEmpty()) {
            return null;
        }
        return group.get(depName);
    }

    /**
     * {@code resolve_range} (from 1.4.0): the version a {@code catalog:} dependency's entry names, else, without an
     * entry or for any other dependency, the dependency's own.
     */
    DependencyVersion resolveRange(Dependency dep) {
        if (dep.version.tag != DependencyVersion.Tag.CATALOG) {
            return dep.version;
        }
        Dependency entry = get(dep.version.string(), dep.name);
        return entry == null ? dep.version : entry.version;
    }

    /**
     * {@code strip_reference} (from 1.4.0): only the root and its workspaces reference catalogs, so that elsewhere a
     * {@code catalog:} dependency is left unresolvable, keeping its literal, and a {@code catalog:} peer becomes an
     * optional peer of {@code *}, binding to whatever the importer provides.
     */
    static void stripReference(Dependency dep) {
        if (dep.version.tag != DependencyVersion.Tag.CATALOG) {
            return;
        }
        String literal = dep.version.literal;
        if (!dep.behavior.isPeer()) {
            dep.version = new DependencyVersion(DependencyVersion.Tag.UNINITIALIZED, literal, null);
            return;
        }
        dep.behavior = dep.behavior.add(Behavior.OPTIONAL);
        dep.version = new DependencyVersion(DependencyVersion.Tag.NPM, literal,
                new DependencyVersion.NpmInfo(dep.name, SemverQuery.parse("*"), false));
    }

    LinkedHashMap<String, Dependency> getOrPutGroup(String catalogName) {
        if (catalogName.isEmpty()) {
            return defaultMap;
        }
        return groups.computeIfAbsent(catalogName, k -> new LinkedHashMap<>());
    }

    boolean parseAppend(Log log, Json.Expr expr, BunRelease release) {
        boolean foundAny = parseGroups(log, expr, release);
        Json.Expr catalogs = expr.get("catalogs");
        Json.Expr defaultGroup = catalogs != null && catalogs.isObject() ? catalogs.get("default") : null;
        if (release.rejectsAnEntryOfBothDefaultCatalogs() && !defaultMap.isEmpty() && defaultGroup != null &&
            defaultGroup.isObject()) {
            boolean conflict = false;
            for (Json.Property item : defaultGroup.properties()) {
                String depName = item.key.asString();
                if (defaultMap.containsKey(depName)) {
                    log.addError("\"" + depName + "\" is defined in both \"catalog\" and \"catalogs.default\"; " +
                                 "keep one of them");
                    conflict = true;
                }
            }
            if (conflict) {
                throw new IllegalArgumentException("InstallFailed");
            }
        }
        return foundAny;
    }

    boolean parseAppend(Log log, Json.Expr expr) {
        return parseGroups(log, expr, BunRelease.PORTED);
    }

    private boolean parseGroups(Log log, Json.Expr expr, BunRelease release) {
        boolean foundAny = false;
        Json.Expr defaultCatalog = expr.get("catalog");
        if (defaultCatalog != null) {
            LinkedHashMap<String, Dependency> group = getOrPutGroup("");
            foundAny = true;
            if (defaultCatalog.isObject()) {
                putEntries(log, group, defaultCatalog, release);
            }
        }
        Json.Expr catalogs = expr.get("catalogs");
        if (catalogs != null) {
            foundAny = true;
            if (catalogs.isObject()) {
                for (Json.Property catalog : catalogs.properties()) {
                    LinkedHashMap<String, Dependency> group = getOrPutGroup(catalog.key.asString());
                    if (catalog.value.isObject()) {
                        putEntries(log, group, catalog.value, release);
                    }
                }
            }
        }
        return foundAny;
    }

    private static void putEntries(Log log, LinkedHashMap<String, Dependency> group, Json.Expr obj,
                                   BunRelease release) {
        for (Json.Property item : obj.properties()) {
            String depName = item.key.asString();
            long depNameHash = Wyhash11.stringHash(depName);
            String versionStr = item.value.asString();
            if (versionStr == null) {
                continue;
            }
            DependencyVersion version = DependencyVersion.parse(depName, depNameHash, versionStr, release);
            if (version == null) {
                log.addError("Invalid dependency version");
                continue;
            }
            if (group.containsKey(depName)) {
                log.addError("Duplicate catalog");
                continue;
            }
            group.put(depName, new Dependency(depNameHash, depName, version, Behavior.NONE));
        }
    }

    void sort() {
        defaultMap = sortedByName(defaultMap);
        LinkedHashMap<String, LinkedHashMap<String, Dependency>> sortedGroups = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashMap<String, Dependency>> group : groups.entrySet()) {
            sortedGroups.put(group.getKey(), sortedByName(group.getValue()));
        }
        List<String> names = new ArrayList<>(sortedGroups.keySet());
        names.sort(Strings::order);
        groups = new LinkedHashMap<>();
        for (String name : names) {
            groups.put(name, sortedGroups.get(name));
        }
    }

    private static LinkedHashMap<String, Dependency> sortedByName(Map<String, Dependency> deps) {
        List<Map.Entry<String, Dependency>> entries = new ArrayList<>(deps.entrySet());
        entries.sort((l, r) -> Strings.order(l.getValue().name, r.getValue().name));
        LinkedHashMap<String, Dependency> out = new LinkedHashMap<>();
        for (Map.Entry<String, Dependency> e : entries) {
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    CatalogMap cloneMap(BunRelease release) {
        CatalogMap out = new CatalogMap();
        for (Map.Entry<String, Dependency> e : defaultMap.entrySet()) {
            out.defaultMap.put(e.getKey(), e.getValue().cloneDependency(release));
        }
        for (Map.Entry<String, LinkedHashMap<String, Dependency>> group : groups.entrySet()) {
            LinkedHashMap<String, Dependency> newGroup = new LinkedHashMap<>();
            for (Map.Entry<String, Dependency> e : group.getValue().entrySet()) {
                newGroup.put(e.getKey(), e.getValue().cloneDependency(release));
            }
            out.groups.put(group.getKey(), newGroup);
        }
        return out;
    }
}
