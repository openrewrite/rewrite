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
import java.util.Map;
import java.util.Set;

/**
 * Converts a yarn 2+ (Berry) {@code yarn.lock} (YAML) into the npm v3 shape
 * consumed by {@link LockFileParser}.
 * <p>
 * Berry entries look like:
 * <pre>
 *   "lodash@npm:^4.17.21":
 *     version: 4.17.21
 *     resolution: "lodash@npm:4.17.21"
 *     dependencies:
 *       foo: "npm:^2.0.0"
 *     languageName: node
 *     linkType: hard
 * </pre>
 * The package name is taken from the resolution field (or key) by stripping
 * the {@code @<protocol>:} suffix. Transitive dep values like {@code "npm:^2.0.0"}
 * are stripped of their protocol prefix to {@code ^2.0.0}.
 */
public final class YarnBerryLockAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private YarnBerryLockAdapter() {}

    @SuppressWarnings("unchecked")
    public static String toNpmV3(String yarnLockContent) {
        Object loaded;
        try {
            loaded = new Yaml().load(yarnLockContent);
        } catch (RuntimeException e) {
            throw new RuntimeException("malformed yarn.lock YAML: " + e.getMessage(), e);
        }
        if (!(loaded instanceof Map)) {
            throw new RuntimeException("yarn.lock root is not a YAML map");
        }
        Map<String, Object> rootMap = (Map<String, Object>) loaded;

        ObjectNode root = MAPPER.createObjectNode();
        root.put("lockfileVersion", 3);
        ObjectNode packages = root.putObject("packages");
        packages.putObject("");

        Set<String> seen = new HashSet<>();
        int[] dupCounter = {0};

        for (Map.Entry<String, Object> entry : rootMap.entrySet()) {
            if ("__metadata".equals(entry.getKey())) {
                continue;
            }
            // Workspace entries (the project itself and any sibling workspace packages) use the
            // "@workspace:" protocol and are not real installed dependencies; npm v3 represents the
            // root under the "" key, so they must be skipped to stay in parity with the RPC marker.
            if (entry.getKey().contains("@workspace:")) {
                continue;
            }
            if (!(entry.getValue() instanceof Map)) {
                continue;
            }
            Map<String, Object> body = (Map<String, Object>) entry.getValue();
            String resolution = asString(body.get("resolution"));
            if (resolution != null && resolution.contains("@workspace:")) {
                continue;
            }
            String version = asString(body.get("version"));
            // Prefer name from resolution (canonical); fallback to key.
            String name = extractName(resolution != null ? resolution : entry.getKey());
            if (name == null || version == null) {
                continue;
            }
            String path = seen.add(name)
                    ? "node_modules/" + name
                    : "node_modules/__dup_" + (dupCounter[0]++) + "__/node_modules/" + name;
            ObjectNode npmEntry = packages.putObject(path);
            npmEntry.put("version", version);

            Object deps = body.get("dependencies");
            if (deps instanceof Map && !((Map<?, ?>) deps).isEmpty()) {
                ObjectNode depsNode = npmEntry.putObject("dependencies");
                for (Map.Entry<String, Object> d : ((Map<String, Object>) deps).entrySet()) {
                    depsNode.put(d.getKey(), stripProtocol(asString(d.getValue())));
                }
            }
        }

        try {
            return MAPPER.writeValueAsString(root);
        } catch (IOException e) {
            throw new RuntimeException("failed to serialize npm v3: " + e.getMessage(), e);
        }
    }

    /**
     * Extract the package name from a yarn berry locator like
     * {@code "lodash@npm:4.17.21"} or {@code "@types/node@npm:^20.0.0"}.
     * The name is everything before the LAST {@code @} that's followed by a
     * non-name character (typically {@code @<protocol>:}).
     */
    private static String extractName(String locator) {
        if (locator == null || locator.isEmpty()) {
            return null;
        }
        // For scoped names starting with '@', the meaningful '@' separator is the second one.
        int searchFrom = locator.startsWith("@") ? 1 : 0;
        int atIdx = locator.indexOf('@', searchFrom);
        if (atIdx <= 0) {
            return null;
        }
        return locator.substring(0, atIdx);
    }

    /** Strip a leading {@code <protocol>:} from a yarn-berry dependency value. */
    private static String stripProtocol(String value) {
        if (value == null) {
            return "";
        }
        int colon = value.indexOf(':');
        // Only strip if the segment before the colon is a "protocol" (no slashes).
        if (colon > 0 && value.indexOf('/') > colon) {
            return value.substring(colon + 1);
        }
        if (colon > 0 && value.indexOf('/') < 0) {
            return value.substring(colon + 1);
        }
        return value;
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }
}
