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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import org.openrewrite.javascript.marker.NodeResolutionResult.ResolvedDependency;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses an npm {@code package-lock.json} v3-format string into structured
 * {@link ResolvedDependency} instances. The Bun lock format is reduced
 * to the same npm v3 shape upstream by {@link BunLockAdapter}, so this
 * parser handles both PMs.
 */
final class LockFileParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LockFileParser() {}

    @Value
    public static class ParseResult {
        List<ResolvedDependency> all;
        Map<String, ResolvedDependency> topLevel;
    }

    public static ParseResult parse(String npmV3Json) {
        JsonNode root;
        try {
            root = MAPPER.readTree(npmV3Json);
        } catch (IOException e) {
            throw new RuntimeException("malformed lock JSON: " + e.getMessage(), e);
        }
        JsonNode packages = root.get("packages");
        if (packages == null || !packages.isObject()) {
            throw new RuntimeException("lock file is missing the `packages` map");
        }

        List<ResolvedDependency> all = new ArrayList<>();
        Map<String, ResolvedDependency> topLevel = new LinkedHashMap<>();

        packages.fields().forEachRemaining(entry -> {
            String pathKey = entry.getKey();
            if (pathKey.isEmpty()) {
                return; // root entry
            }
            String name = nameFromPathKey(pathKey);
            if (name == null) {
                return;
            }
            JsonNode body = entry.getValue();
            String version = body.path("version").asText(null);
            ResolvedDependency dep = new ResolvedDependency(
                    name, version,
                    null, null, null, null,
                    null, null);
            all.add(dep);
            if (isTopLevel(pathKey)) {
                topLevel.put(name, dep);
            }
        });
        return new ParseResult(all, topLevel);
    }

    /**
     * Extracts the package name from a lock-file path key.
     * <p>
     * The name is whatever comes after the LAST {@code node_modules/} segment,
     * which handles top-level deps, nested deps, and scoped packages uniformly.
     */
    private static String nameFromPathKey(String pathKey) {
        int marker = pathKey.lastIndexOf("node_modules/");
        if (marker < 0) {
            return null;
        }
        String tail = pathKey.substring(marker + "node_modules/".length());
        return tail.isEmpty() ? null : tail;
    }

    private static boolean isTopLevel(String pathKey) {
        // Top-level entries have exactly one "node_modules/" segment.
        return pathKey.indexOf("node_modules/") == 0
                && pathKey.indexOf("/node_modules/", "node_modules/".length()) < 0;
    }
}
