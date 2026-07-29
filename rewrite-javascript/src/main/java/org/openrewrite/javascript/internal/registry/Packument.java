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
package org.openrewrite.javascript.internal.registry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A full registry packument ({@code GET registry/name}). Version manifests are kept
 * as raw {@link ObjectNode}s: lock entries copy manifest fields verbatim, so any
 * remodeling would risk losing the exact shapes npm preserves (funding arrays,
 * string-or-object bin, etc.).
 */
public final class Packument {
    private final ObjectNode root;

    Packument(ObjectNode root) {
        this.root = root;
    }

    public @Nullable String latestTag() {
        JsonNode distTags = root.get("dist-tags");
        if (distTags != null && distTags.hasNonNull("latest")) {
            return distTags.get("latest").asText();
        }
        return null;
    }

    public Map<String, ObjectNode> versions() {
        Map<String, ObjectNode> out = new LinkedHashMap<>();
        JsonNode versions = root.get("versions");
        if (versions != null && versions.isObject()) {
            versions.fields().forEachRemaining(e -> {
                if (e.getValue() instanceof ObjectNode) {
                    out.put(e.getKey(), (ObjectNode) e.getValue());
                }
            });
        }
        return out;
    }

    public @Nullable ObjectNode version(String version) {
        JsonNode versions = root.get("versions");
        if (versions == null || !versions.isObject()) {
            return null;
        }
        JsonNode v = versions.get(version);
        return v instanceof ObjectNode ? (ObjectNode) v : null;
    }
}
