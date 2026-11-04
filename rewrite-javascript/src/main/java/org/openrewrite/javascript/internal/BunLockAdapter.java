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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Converts a {@code bun.lock} JSONC string into the npm {@code package-lock.json} v3
 * shape consumed by {@link LockFileParser}.
 * <p>
 * bun.lock entries are arrays: {@code [name@version, url, metadata, integrity]}.
 * Path keys are bare names for top-level deps and {@code "parent/child"} for
 * nested deps. The adapter rewrites those into npm-style {@code node_modules/<name>}
 * and {@code node_modules/parent/node_modules/child} keys.
 */
public final class BunLockAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            .configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true);

    private BunLockAdapter() {}

    public static String toNpmV3(String bunLockJsonc) {
        JsonNode bun;
        try {
            bun = MAPPER.readTree(bunLockJsonc);
        } catch (IOException e) {
            throw new RuntimeException("malformed bun.lock JSONC: " + e.getMessage(), e);
        }
        JsonNode bunPackages = bun.get("packages");
        if (bunPackages == null || !bunPackages.isObject()) {
            throw new RuntimeException("bun.lock is missing the `packages` map");
        }

        ObjectNode root = MAPPER.createObjectNode();
        root.put("lockfileVersion", 3);
        ObjectNode packages = root.putObject("packages");
        packages.putObject(""); // root placeholder

        Iterator<Map.Entry<String, JsonNode>> it = bunPackages.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (!value.isArray() || value.size() < 1) continue;

            String nameAtVersion = value.get(0).asText("");
            int atIdx = nameAtVersion.lastIndexOf('@');
            if (atIdx <= 0) continue;
            String name = nameAtVersion.substring(0, atIdx);
            String version = nameAtVersion.substring(atIdx + 1);

            JsonNode metadata = value.size() > 2 ? value.get(2) : null;

            ObjectNode npmEntry = packages.putObject(npmPathFor(key, name));
            npmEntry.put("version", version);
            if (metadata != null && metadata.isObject()) {
                copyIfPresent(metadata, npmEntry, "dependencies");
                copyIfPresent(metadata, npmEntry, "devDependencies");
                copyIfPresent(metadata, npmEntry, "peerDependencies");
                copyIfPresent(metadata, npmEntry, "optionalDependencies");
            }
        }

        try {
            return MAPPER.writeValueAsString(root);
        } catch (IOException e) {
            throw new RuntimeException("failed to serialize npm v3: " + e.getMessage(), e);
        }
    }

    private static String npmPathFor(String bunKey, String name) {
        if (bunKey.contains("/") && !bunKey.startsWith("@")) {
            // Nested: "parent/child" -> "node_modules/parent/node_modules/child"
            StringBuilder sb = new StringBuilder();
            for (String part : bunKey.split("/")) {
                if (sb.length() > 0) sb.append('/');
                sb.append("node_modules/").append(part);
            }
            return sb.toString();
        }
        return "node_modules/" + name;
    }

    private static void copyIfPresent(JsonNode src, ObjectNode dst, String key) {
        JsonNode v = src.get(key);
        if (v != null && v.isObject() && v.size() > 0) {
            dst.set(key, v);
        }
    }
}
