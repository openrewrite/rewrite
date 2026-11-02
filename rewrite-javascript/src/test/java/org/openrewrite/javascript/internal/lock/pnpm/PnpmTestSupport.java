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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The upstream tests' object literals as the JavaScript values the port works over: JSON (single quotes allowed for
 * readability) parsed into objects, and objects printed back as JSON with their keys in JavaScript's order.
 */
final class PnpmTestSupport {

    private PnpmTestSupport() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> obj(String json) {
        return (Map<String, @Nullable Object>) Manifests.parseValue(json.replace('\'', '"'), "test");
    }

    @SuppressWarnings("unchecked")
    static List<@Nullable Object> list(String json) {
        return (List<@Nullable Object>) Manifests.parseValue(json.replace('\'', '"'), "test");
    }

    static Map<String, String> strings(String json) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, @Nullable Object> entry : obj(json).entrySet()) {
            out.put(entry.getKey(), (String) entry.getValue());
        }
        return out;
    }

    /**
     * {@code JSON.stringify} with the keys in {@code Object.keys} order; {@code undefined} members left out.
     */
    @SuppressWarnings("unchecked")
    static String json(@Nullable Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "'" + value + "'";
        }
        if (value instanceof Map) {
            List<String> members = new ArrayList<>();
            Map<String, @Nullable Object> map = (Map<String, @Nullable Object>) value;
            for (String key : Js.keys(map)) {
                if (map.get(key) != Js.UNDEFINED) {
                    members.add("'" + key + "':" + json(map.get(key)));
                }
            }
            return "{" + String.join(",", members) + "}";
        }
        if (value instanceof List) {
            List<String> items = new ArrayList<>();
            for (Object item : (List<?>) value) {
                items.add(json(item));
            }
            return "[" + String.join(",", items) + "]";
        }
        if (value instanceof Number) {
            return Js.numberToString(((Number) value).doubleValue());
        }
        return String.valueOf(value);
    }

    /**
     * A {@link LockfileObject} from the object form the upstream tests write: importers with {@code specifiers}
     * and dependency fields, packages by dependency path with snapshot and package fields mixed.
     */
    @SuppressWarnings("unchecked")
    static LockfileObject lockfile(String json) {
        Map<String, @Nullable Object> object = obj(json);
        LockfileObject lockfile = new LockfileObject();
        for (String key : Js.keys(object)) {
            if (!"importers".equals(key) && !"packages".equals(key)) {
                lockfile.set(key, object.get(key));
            }
        }
        Map<String, @Nullable Object> importers = (Map<String, @Nullable Object>) object.get("importers");
        if (importers != null) {
            for (String id : Js.keys(importers)) {
                Map<String, @Nullable Object> from = (Map<String, @Nullable Object>) importers.get(id);
                ProjectSnapshot snapshot = new ProjectSnapshot();
                for (String field : Js.keys(from)) {
                    Object value = from.get(field);
                    if ("specifiers".equals(field)) {
                        snapshot.getSpecifiers().putAll(stringMap(value));
                    } else if ("dependenciesMeta".equals(field)) {
                        snapshot.setDependenciesMeta(value);
                    } else if ("publishDirectory".equals(field)) {
                        snapshot.setPublishDirectory(value);
                    } else {
                        snapshot.setField(field, stringMap(value));
                    }
                }
                lockfile.getImporters().put(id, snapshot);
            }
        }
        Map<String, @Nullable Object> packages = (Map<String, @Nullable Object>) object.get("packages");
        if (packages != null) {
            Map<String, @Nullable Object> file = new LinkedHashMap<>();
            Map<String, @Nullable Object> snapshots = new LinkedHashMap<>();
            for (String depPath : Js.keys(packages)) {
                snapshots.put(depPath, packages.get(depPath));
            }
            file.put("snapshots", snapshots);
            lockfile.setPackages(LockfileFormatConverters.convertToLockfileObject(file).getPackages());
        }
        return lockfile;
    }

    /**
     * The object form as the upstream tests compare it: the root keys, importers with specifiers and dependency
     * fields, packages with every field present; JSON with JavaScript's key order.
     */
    static String objectForm(LockfileObject lockfile) {
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        Map<String, @Nullable Object> importers = new LinkedHashMap<>();
        for (Map.Entry<String, ProjectSnapshot> entry : lockfile.getImporters().entrySet()) {
            Map<String, @Nullable Object> importer = new LinkedHashMap<>();
            ProjectSnapshot snapshot = entry.getValue();
            putIfPresent(importer, "dependencies", snapshot.getDependencies());
            putIfPresent(importer, "devDependencies", snapshot.getDevDependencies());
            putIfPresent(importer, "optionalDependencies", snapshot.getOptionalDependencies());
            putIfPresent(importer, "dependenciesMeta", snapshot.getDependenciesMeta());
            putIfPresent(importer, "publishDirectory", snapshot.getPublishDirectory());
            importer.put("specifiers", new LinkedHashMap<String, Object>(snapshot.getSpecifiers()));
            importers.put(entry.getKey(), importer);
        }
        out.put("importers", importers);
        for (Map.Entry<String, @Nullable Object> entry : lockfile.getRest().entrySet()) {
            out.put(entry.getKey(), entry.getValue());
        }
        if (lockfile.getPackages() != null) {
            Map<String, @Nullable Object> packages = new LinkedHashMap<>();
            for (Map.Entry<String, PackageSnapshot> entry : lockfile.getPackages().entrySet()) {
                PackageSnapshot pkg = entry.getValue();
                Map<String, @Nullable Object> p = new LinkedHashMap<>();
                putIfPresent(p, "id", pkg.getId());
                putIfPresent(p, "name", pkg.getName());
                putIfPresent(p, "version", pkg.getVersion());
                putIfPresent(p, "dependencies", pkg.getDependencies());
                putIfPresent(p, "optionalDependencies", pkg.getOptionalDependencies());
                putIfPresent(p, "transitivePeerDependencies", pkg.getTransitivePeerDependencies());
                putIfPresent(p, "optional", pkg.getOptional());
                putIfPresent(p, "resolution", pkg.getResolution());
                putIfPresent(p, "engines", pkg.getEngines());
                putIfPresent(p, "cpu", pkg.getCpu());
                putIfPresent(p, "os", pkg.getOs());
                putIfPresent(p, "libc", pkg.getLibc());
                putIfPresent(p, "deprecated", pkg.getDeprecated());
                putIfPresent(p, "hasBin", pkg.getHasBin());
                putIfPresent(p, "bundledDependencies", pkg.getBundledDependencies());
                putIfPresent(p, "peerDependencies", pkg.getPeerDependencies());
                putIfPresent(p, "peerDependenciesMeta", pkg.getPeerDependenciesMeta());
                packages.put(entry.getKey(), p);
            }
            out.put("packages", packages);
        }
        return json(KeySorting.sortDeepKeys(out));
    }

    private static void putIfPresent(Map<String, @Nullable Object> out, String key, @Nullable Object value) {
        if (value != null && value != Js.UNDEFINED) {
            out.put(key, value);
        }
    }

    /**
     * {@code json} of a JSON text, keys sorted at every depth: equal for equal objects whatever their key order.
     */
    static String sortedJson(String json) {
        return json(KeySorting.sortDeepKeys(obj(json)));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable String> stringMap(@Nullable Object value) {
        Map<String, @Nullable String> out = new LinkedHashMap<>();
        if (value instanceof Map) {
            for (Map.Entry<String, @Nullable Object> entry : ((Map<String, @Nullable Object>) value).entrySet()) {
                out.put(entry.getKey(), (String) entry.getValue());
            }
        }
        return out;
    }
}
