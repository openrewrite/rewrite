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
 * Port of bun's src/install/lockfile/OverrideMap.zig (bun 1.3.10): one flat map from a name hash to the dependency
 * every edge of that name resolves with, from {@code overrides}, else {@code resolutions}.
 */
final class OverrideMap {

    /**
     * The field of the root manifest parseAppend reads overrides from: {@code overrides} when it has one, else
     * {@code resolutions} when it has that, else null.
     */
    static @Nullable String readsFrom(String rootManifest) {
        Json.Expr expr;
        try {
            expr = Json.parse(rootManifest);
        } catch (RuntimeException e) {
            return null;
        }
        return expr.get("overrides") != null ? "overrides" : expr.get("resolutions") != null ? "resolutions" : null;
    }

    final LinkedHashMap<Long, Dependency> map = new LinkedHashMap<>();

    @Nullable
    DependencyVersion get(long nameHash) {
        if (map.isEmpty()) {
            return null;
        }
        Dependency dep = map.get(nameHash);
        return dep == null ? null : dep.version;
    }

    void sort() {
        List<Map.Entry<Long, Dependency>> entries = new ArrayList<>(map.entrySet());
        entries.sort((l, r) -> Strings.order(l.getValue().name, r.getValue().name));
        map.clear();
        for (Map.Entry<Long, Dependency> e : entries) {
            map.put(e.getKey(), e.getValue());
        }
    }

    OverrideMap cloneMap() {
        OverrideMap out = new OverrideMap();
        for (Map.Entry<Long, Dependency> e : map.entrySet()) {
            out.map.put(e.getKey(), e.getValue().cloneDependency());
        }
        return out;
    }

    void parseAppend(Lockfile lockfile, Package rootPackage, Json.Expr expr) {
        Json.Expr overrides = expr.get("overrides");
        if (overrides != null) {
            parseFromOverrides(lockfile, rootPackage, overrides);
            return;
        }
        Json.Expr resolutions = expr.get("resolutions");
        if (resolutions != null) {
            parseFromResolutions(lockfile, rootPackage, resolutions);
        }
    }

    void parseFromOverrides(Lockfile lockfile, Package rootPackage, Json.Expr expr) {
        if (!expr.isObject()) {
            throw new IllegalArgumentException("Invalid");
        }
        for (Json.Property prop : expr.properties()) {
            String k = prop.key.asString();
            if (k == null || k.isEmpty()) {
                continue;
            }
            long nameHash = Wyhash11.stringHash(k);
            Json.Expr valueExpr = prop.value;
            Json.Expr value;
            if (valueExpr.isString()) {
                value = valueExpr;
            } else if (valueExpr.isObject()) {
                Json.Expr dot = valueExpr.get(".");
                if (dot == null) {
                    continue;
                }
                if (!dot.isString()) {
                    continue;
                }
                value = dot;
            } else {
                continue;
            }
            String versionStr = value.asString();
            if (versionStr.startsWith("patch:")) {
                continue;
            }
            Dependency version = parseOverrideValue(lockfile, rootPackage, k, versionStr);
            if (version != null) {
                map.put(nameHash, version);
            }
        }
    }

    void parseFromResolutions(Lockfile lockfile, Package rootPackage, Json.Expr expr) {
        if (!expr.isObject()) {
            return;
        }
        for (Json.Property prop : expr.properties()) {
            String k = prop.key.asString();
            if (k.startsWith("**/")) {
                k = k.substring(3);
            }
            if (k.isEmpty()) {
                continue;
            }
            Json.Expr value = prop.value;
            if (!value.isString()) {
                continue;
            }
            if (k.charAt(0) == '@') {
                int firstSlash = k.indexOf('/');
                if (firstSlash < 0) {
                    continue;
                }
                if (k.indexOf('/', firstSlash + 1) >= 0) {
                    continue;
                }
            } else if (k.indexOf('/') >= 0) {
                continue;
            }
            String versionStr = value.asString();
            if (versionStr.startsWith("patch:")) {
                continue;
            }
            Dependency version = parseOverrideValue(lockfile, rootPackage, k, versionStr);
            if (version != null) {
                map.put(Wyhash11.stringHash(k), version);
            }
        }
    }

    /**
     * A {@code $name} value is the root's own dependency of that name, the whole dependency: the lock writes that
     * dependency's name and literal for the key.
     */
    static @Nullable Dependency parseOverrideValue(Lockfile lockfile, Package rootPackage, String key, String value) {
        if (value.isEmpty()) {
            return null;
        }
        if (value.charAt(0) == '$') {
            String refName = value.substring(1);
            for (Dependency dep : lockfile.dependenciesOf(rootPackage)) {
                if (dep.name.equals(refName)) {
                    return dep;
                }
            }
            return null;
        }
        long nameHash = Wyhash11.stringHash(key);
        DependencyVersion version = DependencyVersion.parse(key, nameHash, value);
        if (version == null) {
            return null;
        }
        return new Dependency(nameHash, key, version, Behavior.NONE);
    }
}
