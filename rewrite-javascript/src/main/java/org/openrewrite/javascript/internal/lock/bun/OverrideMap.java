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
 * Port of bun's src/install/lockfile/OverrideMap.zig (bun 1.3.10): one flat map from a package name hash to the
 * dependency every edge of that name resolves with, read from {@code overrides} or, failing that,
 * {@code resolutions}.
 */
final class OverrideMap {

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

    void parseAppend(Lockfile lockfile, Package rootPackage, Log log, Json.Expr expr) {
        Json.Expr overrides = expr.get("overrides");
        if (overrides != null) {
            parseFromOverrides(lockfile, rootPackage, log, overrides);
            return;
        }
        Json.Expr resolutions = expr.get("resolutions");
        if (resolutions != null) {
            parseFromResolutions(lockfile, rootPackage, log, resolutions);
        }
    }

    void parseFromOverrides(Lockfile lockfile, Package rootPackage, Log log, Json.Expr expr) {
        if (!expr.isObject()) {
            log.addWarning("\"overrides\" must be an object");
            throw new IllegalArgumentException("Invalid");
        }
        for (Json.Property prop : expr.properties()) {
            String k = prop.key.asString();
            if (k == null || k.isEmpty()) {
                log.addWarning("Missing overridden package name");
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
                    log.addWarning("Bun currently does not support nested \"overrides\"");
                    continue;
                }
                if (!dot.isString()) {
                    log.addWarning("Invalid override value for \"" + k + "\"");
                    continue;
                }
                if (valueExpr.properties().size() > 1) {
                    log.addWarning("Bun currently does not support nested \"overrides\"");
                }
                value = dot;
            } else {
                log.addWarning("Invalid override value for \"" + k + "\"");
                continue;
            }
            String versionStr = value.asString();
            if (versionStr.startsWith("patch:")) {
                log.addWarning("Bun currently does not support patched package \"overrides\"");
                continue;
            }
            Dependency version = parseOverrideValue("override", lockfile, rootPackage, log, k, versionStr);
            if (version != null) {
                map.put(nameHash, version);
            }
        }
    }

    void parseFromResolutions(Lockfile lockfile, Package rootPackage, Log log, Json.Expr expr) {
        if (!expr.isObject()) {
            log.addWarning("\"resolutions\" must be an object with string values");
            return;
        }
        for (Json.Property prop : expr.properties()) {
            String k = prop.key.asString();
            if (k.startsWith("**/")) {
                k = k.substring(3);
            }
            if (k.isEmpty()) {
                log.addWarning("Missing resolution package name");
                continue;
            }
            Json.Expr value = prop.value;
            if (!value.isString()) {
                log.addWarning("Expected string value for resolution \"" + k + "\"");
                continue;
            }
            if (k.charAt(0) == '@') {
                int firstSlash = k.indexOf('/');
                if (firstSlash < 0) {
                    log.addWarning("Invalid package name \"" + k + "\"");
                    continue;
                }
                if (k.indexOf('/', firstSlash + 1) >= 0) {
                    log.addWarning("Bun currently does not support nested \"resolutions\"");
                    continue;
                }
            } else if (k.indexOf('/') >= 0) {
                log.addWarning("Bun currently does not support nested \"resolutions\"");
                continue;
            }
            String versionStr = value.asString();
            if (versionStr.startsWith("patch:")) {
                log.addWarning("Bun currently does not support patched package \"resolutions\"");
                continue;
            }
            Dependency version = parseOverrideValue("resolution", lockfile, rootPackage, log, k, versionStr);
            if (version != null) {
                map.put(Wyhash11.stringHash(k), version);
            }
        }
    }

    /**
     * A {@code $name} value is the root's own dependency of that name, the whole dependency: the lock writes that
     * dependency's name and literal for the key.
     */
    static @Nullable Dependency parseOverrideValue(String field, Lockfile lockfile, Package rootPackage, Log log,
                                                   String key, String value) {
        if (value.isEmpty()) {
            log.addWarning("Missing " + field + " value");
            return null;
        }
        if (value.charAt(0) == '$') {
            String refName = value.substring(1);
            for (Dependency dep : lockfile.dependenciesOf(rootPackage)) {
                if (dep.name.equals(refName)) {
                    return dep;
                }
            }
            log.addWarning("Could not resolve " + field + " \"" + value + "\" (you need \"" + refName +
                           "\" in your dependencies)");
            return null;
        }
        long nameHash = Wyhash11.stringHash(key);
        DependencyVersion version = DependencyVersion.parse(key, nameHash, value);
        if (version == null) {
            log.addWarning("Invalid " + field + " value \"" + value + "\"");
            return null;
        }
        return new Dependency(nameHash, key, version, Behavior.NONE);
    }
}
