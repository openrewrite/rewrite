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
 * pnpm-lock.yaml has explicit root-level top-level deps:
 * <pre>
 *   dependencies:
 *     lodash:
 *       specifier: ^4.17.21
 *       version: 4.17.21
 *   packages:
 *     /lodash@4.17.21:
 *       resolution: {...}
 * </pre>
 * Top-level packages (those listed at root) get {@code node_modules/&lt;name&gt;}
 * paths; everything else in the {@code packages} map goes to a synthetic nested
 * path so it's preserved in {@code all} but not in {@code topLevel}.
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

        // Collect the names of top-level deps from the four root scopes.
        Set<String> topLevelNames = new LinkedHashSet<>();
        collectScopeNames(rootMap.get("dependencies"), topLevelNames);
        collectScopeNames(rootMap.get("devDependencies"), topLevelNames);
        collectScopeNames(rootMap.get("peerDependencies"), topLevelNames);
        collectScopeNames(rootMap.get("optionalDependencies"), topLevelNames);

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
            NameVersion nv = parsePackageKey(entry.getKey());
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

    @SuppressWarnings("unchecked")
    private static void collectScopeNames(Object scope, Set<String> sink) {
        if (scope instanceof Map) {
            sink.addAll(((Map<String, Object>) scope).keySet());
        }
    }

    /**
     * Parse a pnpm package key like {@code /lodash@4.17.21}, {@code /@types/node@20.0.0},
     * or {@code /react-redux@8.0.5(react@18.2.0)}. Returns null for unrecognized shapes.
     */
    static NameVersion parsePackageKey(String key) {
        if (key == null || !key.startsWith("/")) {
            return null;
        }
        // Strip the leading '/'.
        String body = key.substring(1);
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
