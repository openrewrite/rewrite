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
package org.openrewrite.python.internal.pdmlock;

import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.CanonicalJson;
import org.openrewrite.python.internal.Hashing;
import org.openrewrite.python.internal.PyprojectData;
import org.openrewrite.python.internal.pep508.Pep508Requirement;
import org.openrewrite.toml.tree.Toml;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes {@code [metadata].content_hash} with pdm's exact algorithm
 * ({@code PyProject.content_hash}, {@code pdm/project/project_file.py}): the {@code sha256:} prefix
 * followed by the SHA-256 of {@code json.dumps(dump_data, sort_keys=True).encode("utf-8")} over a
 * relevant slice of the pyproject file.
 */
public final class PdmContentHash {

    private PdmContentHash() {
    }

    public static String hash(Toml.Document pyproject) {
        Map<String, Object> root = PyprojectData.toNestedMap(pyproject);
        Map<String, Object> project = asMap(root.get("project"));
        Map<String, Object> pdm = asMap(mapGet(asMap(root.get("tool")), "pdm"));

        Map<String, Object> dumpData = new LinkedHashMap<>();
        dumpData.put("sources", pdm.containsKey("source") ? pdm.get("source") : new ArrayList<>());
        dumpData.put("dependencies", project.containsKey("dependencies") ? project.get("dependencies") : new ArrayList<>());
        dumpData.put("dev-dependencies", devDependencies(root, pdm));
        dumpData.put("optional-dependencies", project.containsKey("optional-dependencies") ?
                project.get("optional-dependencies") : new LinkedHashMap<>());
        dumpData.put("requires-python", project.containsKey("requires-python") ? project.get("requires-python") : "");
        dumpData.put("resolution", pdm.containsKey("resolution") ? pdm.get("resolution") : new LinkedHashMap<>());

        String digest = Hashing.sha256Hex(CanonicalJson.emit(dumpData, true).getBytes(StandardCharsets.UTF_8));
        return "sha256:" + digest;
    }

    /**
     * pdm's {@code dev_dependencies}: PEP 735 {@code [dependency-groups]} merged with
     * {@code [tool.pdm.dev-dependencies]}, group names PEP 503-normalized, tool groups appended
     * to any same-named PEP 735 group.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> devDependencies(Map<String, Object> root, Map<String, Object> pdm) {
        Map<String, Object> groups = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : asMap(root.get("dependency-groups")).entrySet()) {
            groups.put(Pep508Requirement.canonicalize(e.getKey()), e.getValue());
        }
        for (Map.Entry<String, Object> e : asMap(mapGet(pdm, "dev-dependencies")).entrySet()) {
            String group = Pep508Requirement.canonicalize(e.getKey());
            List<Object> merged = new ArrayList<>();
            Object existing = groups.get(group);
            if (existing instanceof List) {
                merged.addAll((List<Object>) existing);
            }
            if (e.getValue() instanceof List) {
                merged.addAll((List<Object>) e.getValue());
            }
            groups.put(group, merged);
        }
        return groups;
    }

    private static @Nullable Object mapGet(Map<String, Object> map, String key) {
        return map.get(key);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(@Nullable Object value) {
        return value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<>();
    }
}
