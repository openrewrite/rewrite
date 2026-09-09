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
package org.openrewrite.python.internal.poetrylock;

import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.CanonicalJson;
import org.openrewrite.python.internal.Hashing;
import org.openrewrite.python.internal.PyprojectData;
import org.openrewrite.toml.tree.Toml;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes {@code [metadata].content-hash} with poetry's exact algorithm
 * ({@code Locker._get_content_hash}, {@code poetry/packages/locker.py}): the SHA-256 of
 * {@code json.dumps(relevant_content, sort_keys=True).encode()} over a relevant slice of the
 * pyproject file, covering both the legacy {@code [tool.poetry]} and the modern PEP 621
 * {@code [project]} + {@code [dependency-groups]} layouts.
 */
public final class PoetryContentHash {

    private static final List<String> LEGACY_KEYS =
            Arrays.asList("dependencies", "source", "extras", "dev-dependencies");
    private static final List<String> RELEVANT_KEYS =
            Arrays.asList("dependencies", "source", "extras", "dev-dependencies", "group");
    private static final List<String> RELEVANT_PROJECT_KEYS =
            Arrays.asList("requires-python", "dependencies", "optional-dependencies");

    private PoetryContentHash() {
    }

    public static String hash(Toml.Document pyproject) {
        Map<String, Object> root = PyprojectData.toNestedMap(pyproject);
        Map<String, Object> project = asMap(root.get("project"));
        Map<String, Object> groupContent = asMap(root.get("dependency-groups"));
        Map<String, Object> toolPoetry = asMap(mapGet(asMap(root.get("tool")), "poetry"));

        Map<String, Object> relevantProject = new LinkedHashMap<>();
        for (String key : RELEVANT_PROJECT_KEYS) {
            if (project.containsKey(key)) {
                relevantProject.put(key, project.get(key));
            }
        }

        Map<String, Object> relevantPoetry = new LinkedHashMap<>();
        for (String key : RELEVANT_KEYS) {
            Object data = toolPoetry.get(key);
            if (data == null) {
                boolean legacy = LEGACY_KEYS.contains(key);
                if (!legacy || !relevantProject.isEmpty() || !groupContent.isEmpty()) {
                    continue;
                }
            }
            relevantPoetry.put(key, data);
        }

        Object relevant;
        if (!relevantProject.isEmpty() || !groupContent.isEmpty()) {
            Map<String, Object> combined = new LinkedHashMap<>();
            if (!relevantProject.isEmpty()) {
                combined.put("project", relevantProject);
            }
            if (!groupContent.isEmpty()) {
                combined.put("dependency-groups", groupContent);
            }
            Map<String, Object> tool = new LinkedHashMap<>();
            tool.put("poetry", relevantPoetry);
            combined.put("tool", tool);
            relevant = combined;
        } else {
            relevant = relevantPoetry;
        }

        return Hashing.sha256Hex(CanonicalJson.emit(relevant, true).getBytes(StandardCharsets.UTF_8));
    }

    private static @Nullable Object mapGet(Map<String, Object> map, String key) {
        return map.get(key);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(@Nullable Object value) {
        return value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<>();
    }
}
