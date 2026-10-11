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
import org.openrewrite.javascript.internal.lock.JsYaml;

import java.util.List;
import java.util.Map;

/**
 * Port of @pnpm/workspace.workspace-manifest-reader src/index.ts and src/catalogs.ts
 * (pnpm 11.2.2).
 */
final class WorkspaceManifest {

    private WorkspaceManifest() {
    }

    /**
     * {@code readWorkspaceManifest}: null for an absent or empty file; pnpm's INVALID_WORKSPACE_CONFIGURATION for one
     * it rejects, a {@link JsYaml.YamlException} for one that is not YAML.
     */
    static @Nullable Map<String, @Nullable Object> readWorkspaceManifest(@Nullable String text) {
        if (text == null) {
            return null;
        }
        Object manifest = JsYaml.load(text);
        return validateWorkspaceManifest(manifest);
    }

    @SuppressWarnings("unchecked")
    static @Nullable Map<String, @Nullable Object> validateWorkspaceManifest(@Nullable Object manifest) {
        if (manifest == null || manifest == Js.UNDEFINED) {
            return null;
        }
        if (manifest instanceof List) {
            throw invalid("Expected object but found - array");
        }
        if (!(manifest instanceof Map)) {
            throw invalid("Expected object but found - " + Js.typeOf(manifest));
        }
        Map<String, @Nullable Object> map = (Map<String, @Nullable Object>) manifest;
        if (map.isEmpty()) {
            return map;
        }
        assertValidWorkspaceManifestPackages(map);
        assertValidWorkspaceManifestCatalog(map);
        assertValidWorkspaceManifestCatalogs(map);
        return map;
    }

    private static void assertValidWorkspaceManifestPackages(Map<String, @Nullable Object> manifest) {
        Object packages = manifest.get("packages");
        if (!Js.truthy(packages)) {
            return;
        }
        if (!(packages instanceof List)) {
            throw invalid("packages field is not an array");
        }
        for (Object pkg : (List<?>) packages) {
            if (!Js.truthy(pkg)) {
                throw invalid("Missing or empty package");
            }
            if (!(pkg instanceof String)) {
                throw invalid("Invalid package type - " + Js.typeOf(pkg));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void assertValidWorkspaceManifestCatalog(Map<String, @Nullable Object> manifest) {
        Object catalog = manifest.get("catalog");
        if (catalog == null) {
            return;
        }
        if (catalog instanceof List) {
            throw invalid("Expected catalog field to be an object, but found - array");
        }
        if (!(catalog instanceof Map)) {
            throw invalid("Expected catalog field to be an object, but found - " + Js.typeOf(catalog));
        }
        for (Map.Entry<String, @Nullable Object> entry : ((Map<String, @Nullable Object>) catalog).entrySet()) {
            if (!(entry.getValue() instanceof String)) {
                throw invalid("Invalid catalog entry for " + entry.getKey() + ". Expected string, but found: " +
                              Js.typeOf(entry.getValue()));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void assertValidWorkspaceManifestCatalogs(Map<String, @Nullable Object> manifest) {
        Object catalogs = manifest.get("catalogs");
        if (catalogs == null) {
            return;
        }
        if (catalogs instanceof List) {
            throw invalid("Expected catalogs field to be an object, but found - array");
        }
        if (!(catalogs instanceof Map)) {
            throw invalid("Expected catalogs field to be an object, but found - " + Js.typeOf(catalogs));
        }
        for (String catalogName : Js.keys((Map<String, @Nullable Object>) catalogs)) {
            Object catalog = ((Map<String, @Nullable Object>) catalogs).get(catalogName);
            if (catalog instanceof List) {
                throw invalid("Expected named catalog " + catalogName + " to be an object, but found - array");
            }
            if (catalog == null) {
                throw invalid("Expected named catalog " + catalogName + " to be an object, but found - null");
            }
            if (!(catalog instanceof Map)) {
                throw invalid("Expected named catalog " + catalogName + " to be an object, but found - " +
                              Js.typeOf(catalog));
            }
            for (Map.Entry<String, @Nullable Object> entry : ((Map<String, @Nullable Object>) catalog).entrySet()) {
                if (!(entry.getValue() instanceof String)) {
                    throw invalid("Catalog '" + catalogName + "' has invalid entry '" + entry.getKey() +
                                  "'. Expected string specifier, but found: " + Js.typeOf(entry.getValue()));
                }
            }
        }
    }

    private static PnpmError invalid(String message) {
        return new PnpmError("INVALID_WORKSPACE_CONFIGURATION", message);
    }
}
