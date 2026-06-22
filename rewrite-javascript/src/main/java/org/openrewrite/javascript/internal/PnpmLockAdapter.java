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
package org.openrewrite.javascript.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Converts a {@code pnpm-lock.yaml} into the npm v3 shape consumed by
 * {@link LockFileParser}.
 * <p>
 * Three on-disk formats are supported:
 * <ul>
 *   <li><b>lockfileVersion 5.x</b> (pnpm 7.x and earlier) lists top-level deps at the root and
 *   keys packages with a leading slash but a <em>slash</em> separating name and version
 *   ({@code /lodash/4.17.21}), with the peer-dep suffix introduced by {@code _}.</li>
 *   <li><b>lockfileVersion 6.0</b> (pnpm 8.x) lists top-level deps at the root and
 *   keys packages with a leading slash, with dependencies inlined:
 *   <pre>
 *   dependencies:
 *     lodash:
 *       specifier: ^4.17.21
 *       version: 4.17.21
 *   packages:
 *     /lodash@4.17.21:
 *       resolution: {...}
 *   </pre></li>
 *   <li><b>lockfileVersion 9.0</b> (pnpm 9.x/10.x) moves top-level deps under
 *   {@code importers} and splits package metadata (in {@code packages}, keyed
 *   <em>without</em> a leading slash) from the dependency graph (in {@code snapshots}):
 *   <pre>
 *   importers:
 *     .:
 *       dependencies:
 *         lodash: {specifier: ^4.17.21, version: 4.17.21}
 *   packages:
 *     lodash@4.17.21:
 *       resolution: {...}
 *   snapshots:
 *     lodash@4.17.21: {}
 *   </pre></li>
 * </ul>
 * Top-level packages (those listed at the root or under {@code importers}) get
 * {@code node_modules/&lt;name&gt;} paths; everything else in the {@code packages} map
 * goes to a synthetic nested path so it's preserved in {@code all} but not in
 * {@code topLevel}.
 * <p>
 * pnpm encodes peer-dep variants in its package keys: {@code /foo@1.0.0(react@18.0.0)}.
 * The peer-dep suffix is stripped during name/version extraction.
 */
public final class PnpmLockAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PnpmLockAdapter() {}

    @SuppressWarnings("unchecked")
    public static String toNpmV3(String pnpmLockYaml) {
        Object loaded;
        try {
            loaded = new Yaml().load(pnpmLockYaml);
        } catch (RuntimeException e) {
            throw new RuntimeException("malformed pnpm-lock.yaml: " + e.getMessage(), e);
        }
        if (!(loaded instanceof Map)) {
            throw new RuntimeException("pnpm-lock.yaml root is not a YAML map");
        }
        Map<String, Object> rootMap = (Map<String, Object>) loaded;

        // Collect the names of top-level deps. lockfileVersion 6.0 lists them at the root;
        // 9.0 nests them under `importers.<dir>`. Reading both keeps the adapter format-agnostic.
        Set<String> topLevelNames = new LinkedHashSet<>();
        collectAllScopeNames(rootMap, topLevelNames);
        Object importers = rootMap.get("importers");
        if (importers instanceof Map) {
            for (Object importer : ((Map<String, Object>) importers).values()) {
                if (importer instanceof Map) {
                    collectAllScopeNames((Map<String, Object>) importer, topLevelNames);
                }
            }
        }

        // lockfileVersion 9.0 moves the dependency graph out of `packages` and into `snapshots`,
        // keyed by the same package key. Older formats inline dependencies in each package body.
        Object snapshotsObj = rootMap.get("snapshots");
        Map<String, Object> snapshots = snapshotsObj instanceof Map ? (Map<String, Object>) snapshotsObj : null;

        // pnpm 7 and earlier (lockfileVersion 5.x) separate name and version with a slash
        // (`/is-even/1.0.0`) rather than the `@` used from 6.0 onward, so they need a different key parser.
        boolean legacyKeys = lockfileVersion(rootMap.get("lockfileVersion")) < 6;

        ObjectNode root = MAPPER.createObjectNode();
        root.put("lockfileVersion", 3);
        ObjectNode packages = root.putObject("packages");
        packages.putObject("");

        Object packagesObj = rootMap.get("packages");
        if (!(packagesObj instanceof Map)) {
            try {
                return MAPPER.writeValueAsString(root);
            } catch (IOException e) {
                throw new RuntimeException("failed to serialize npm v3: " + e.getMessage(), e);
            }
        }

        Set<String> seenTopLevel = new HashSet<>();
        int[] nestedCounter = {0};

        for (Map.Entry<String, Object> entry : ((Map<String, Object>) packagesObj).entrySet()) {
            if (!(entry.getValue() instanceof Map)) {
                continue;
            }
            NameVersion nv = legacyKeys ? parseLegacyPackageKey(entry.getKey()) : parsePackageKey(entry.getKey());
            if (nv == null) {
                continue;
            }
            Map<String, Object> body = (Map<String, Object>) entry.getValue();

            String path;
            if (topLevelNames.contains(nv.name) && seenTopLevel.add(nv.name)) {
                path = "node_modules/" + nv.name;
            } else {
                path = "node_modules/.pnpm/" + (nestedCounter[0]++) + "/node_modules/" + nv.name;
            }

            ObjectNode npmEntry = packages.putObject(path);
            npmEntry.put("version", nv.version);

            Object deps = body.get("dependencies");
            if (!(deps instanceof Map) && snapshots != null) {
                Object snapshot = snapshots.get(entry.getKey());
                if (snapshot instanceof Map) {
                    deps = ((Map<String, Object>) snapshot).get("dependencies");
                }
            }
            if (deps instanceof Map && !((Map<?, ?>) deps).isEmpty()) {
                ObjectNode depsNode = npmEntry.putObject("dependencies");
                for (Map.Entry<String, Object> d : ((Map<String, Object>) deps).entrySet()) {
                    depsNode.put(d.getKey(), asString(d.getValue()));
                }
            }
        }

        try {
            return MAPPER.writeValueAsString(root);
        } catch (IOException e) {
            throw new RuntimeException("failed to serialize npm v3: " + e.getMessage(), e);
        }
    }

    private static void collectAllScopeNames(Map<String, Object> source, Set<String> sink) {
        collectScopeNames(source.get("dependencies"), sink);
        collectScopeNames(source.get("devDependencies"), sink);
        collectScopeNames(source.get("peerDependencies"), sink);
        collectScopeNames(source.get("optionalDependencies"), sink);
    }

    @SuppressWarnings("unchecked")
    private static void collectScopeNames(Object scope, Set<String> sink) {
        if (scope instanceof Map) {
            sink.addAll(((Map<String, Object>) scope).keySet());
        }
    }

    /**
     * Parse a pnpm package key like {@code /lodash@4.17.21}, {@code /@types/node@20.0.0},
     * or {@code /react-redux@8.0.5(react@18.2.0)}. The leading slash is present in
     * lockfileVersion 6.0 and absent in 9.0, so it is optional. Returns null for
     * unrecognized shapes.
     */
    static NameVersion parsePackageKey(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        // Strip the leading '/' when present (lockfileVersion 6.0).
        String body = key.startsWith("/") ? key.substring(1) : key;
        // Strip the trailing peer-dep suffix '(...)' if present.
        int parenStart = body.indexOf('(');
        if (parenStart >= 0) {
            body = body.substring(0, parenStart);
        }
        // Split at the last '@' (handles scoped names which have '@' at index 0).
        int searchFrom = body.startsWith("@") ? 1 : 0;
        int atIdx = body.lastIndexOf('@');
        if (atIdx <= searchFrom - 1 || atIdx == body.length() - 1) {
            return null;
        }
        // Edge case: scoped name without an @version suffix (shouldn't happen in real lockfiles).
        if (body.startsWith("@") && atIdx == 0) {
            return null;
        }
        String name = body.substring(0, atIdx);
        String version = body.substring(atIdx + 1);
        return new NameVersion(name, version);
    }

    /**
     * Parse a legacy pnpm package key (lockfileVersion 5.x and earlier) like
     * {@code /lodash/4.17.21}, {@code /@types/node/20.0.0}, or
     * {@code /react-redux/8.0.5_react@18.2.0}, where name and version are separated by a
     * slash and the peer-dep suffix is introduced with {@code _}. Returns null for
     * unrecognized shapes.
     */
    static NameVersion parseLegacyPackageKey(String key) {
        if (key == null || !key.startsWith("/")) {
            return null;
        }
        String body = key.substring(1);
        // Name and version are separated by the last '/'; everything before it is the
        // (possibly scoped) name.
        int lastSlash = body.lastIndexOf('/');
        if (lastSlash <= 0 || lastSlash == body.length() - 1) {
            return null;
        }
        String name = body.substring(0, lastSlash);
        String version = body.substring(lastSlash + 1);
        // Strip the peer-dep suffix '_...' from the version segment.
        int underscore = version.indexOf('_');
        if (underscore >= 0) {
            version = version.substring(0, underscore);
        }
        return version.isEmpty() ? null : new NameVersion(name, version);
    }

    /**
     * Reads the {@code lockfileVersion} field, which snakeyaml surfaces either as a number
     * (e.g. {@code 5.4}) or a quoted string (e.g. {@code '6.0'}). Unknown values default to a
     * modern version so the {@code @}-style key parser is used.
     */
    private static double lockfileVersion(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                // Fall through to the modern default.
            }
        }
        return 6;
    }

    private static String asString(Object o) {
        return o == null ? "" : o.toString();
    }

    static final class NameVersion {
        final String name;
        final String version;

        NameVersion(String name, String version) {
            this.name = name;
            this.version = version;
        }
    }
}
