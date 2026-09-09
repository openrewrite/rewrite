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
package org.openrewrite.javascript.internal.lock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads the range a recipe re-pinned for a dependency out of the edited {@code package.json}, for the
 * yarn-classic and bun patchers whose descriptor/importer surface {@link LockEditSet.PackageEdit} does not carry.
 */
final class LockManifests {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> SCOPES = Arrays.asList(
            "dependencies", "devDependencies", "peerDependencies", "optionalDependencies");

    private LockManifests() {
    }

    /** The declared range for {@code name}, preferring {@code preferredScope}, or {@code null} if absent. */
    static @Nullable String declaredConstraint(@Nullable String packageJson, @Nullable String preferredScope, String name) {
        if (packageJson == null) {
            return null;
        }
        try {
            JsonNode root = JSON.readTree(packageJson);
            if (root == null || !root.isObject()) {
                return null;
            }
            String preferred = read(root, preferredScope, name);
            if (preferred != null) {
                return preferred;
            }
            for (String scope : SCOPES) {
                String v = read(root, scope, name);
                if (v != null) {
                    return v;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Every dependency name declared in {@code package.json} across all scopes — the roots for orphan GC. */
    static Set<String> declaredNames(@Nullable String packageJson) {
        Set<String> names = new LinkedHashSet<>();
        if (packageJson == null) {
            return names;
        }
        try {
            JsonNode root = JSON.readTree(packageJson);
            if (root == null || !root.isObject()) {
                return names;
            }
            for (String scope : SCOPES) {
                JsonNode s = root.get(scope);
                if (s != null && s.isObject()) {
                    for (Iterator<String> it = s.fieldNames(); it.hasNext(); ) {
                        names.add(it.next());
                    }
                }
            }
        } catch (Exception ignored) {
            // A malformed manifest just yields no roots; the caller keeps every block.
        }
        return names;
    }

    private static @Nullable String read(JsonNode root, @Nullable String scope, String name) {
        if (scope == null) {
            return null;
        }
        JsonNode s = root.get(scope);
        if (s != null && s.isObject()) {
            JsonNode c = s.get(name);
            if (c != null && c.isTextual()) {
                return c.asText();
            }
        }
        return null;
    }
}
