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

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Port of @pnpm/catalogs.config, @pnpm/catalogs.protocol-parser and @pnpm/catalogs.resolver (pnpm 11.2.2): the
 * catalogs pnpm-workspace.yaml defines, by catalog name then alias, and the {@code catalog:} protocol over them.
 */
final class Catalogs {

    private static final String CATALOG_PROTOCOL = "catalog:";

    private Catalogs() {
    }

    /**
     * {@code resolveFromCatalog}'s result: found (a specifier), a misconfiguration (an error to rethrow), or unused.
     */
    @Value
    static class Resolution {
        String type;

        @Nullable
        String catalogName;

        @Nullable
        String specifier;

        @Nullable
        PnpmError error;

        static Resolution unused() {
            return new Resolution("unused", null, null, null);
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Map<String, @Nullable Object>> getCatalogsFromWorkspaceManifest(
            @Nullable Map<String, @Nullable Object> workspaceManifest) {
        Map<String, @Nullable Map<String, @Nullable Object>> catalogs = new LinkedHashMap<>();
        if (workspaceManifest == null) {
            return catalogs;
        }
        Object catalog = workspaceManifest.get("catalog");
        Object named = workspaceManifest.get("catalogs");
        if (catalog != null && named instanceof Map && ((Map<String, Object>) named).get("default") != null) {
            throw new PnpmError("INVALID_CATALOGS_CONFIGURATION",
                    "The 'default' catalog was defined multiple times. " +
                    "Use the 'catalog' field or 'catalogs.default', but not both.");
        }
        catalogs.put("default", (Map<String, @Nullable Object>) catalog);
        if (named instanceof Map) {
            for (String name : Js.keys((Map<String, Object>) named)) {
                catalogs.put(name, (Map<String, @Nullable Object>) ((Map<String, Object>) named).get(name));
            }
        }
        return catalogs;
    }

    /**
     * {@code parseCatalogProtocol}: the catalog a {@code catalog:} specifier names, null for any other specifier.
     */
    static @Nullable String parseCatalogProtocol(String bareSpecifier) {
        if (!bareSpecifier.startsWith(CATALOG_PROTOCOL)) {
            return null;
        }
        String catalogNameRaw = Js.trim(bareSpecifier.substring(CATALOG_PROTOCOL.length()));
        return catalogNameRaw.isEmpty() ? "default" : catalogNameRaw;
    }

    static Resolution resolveFromCatalog(Map<String, @Nullable Map<String, @Nullable Object>> catalogs,
                                         String alias, String bareSpecifier) {
        String catalogName = parseCatalogProtocol(bareSpecifier);
        if (catalogName == null) {
            return Resolution.unused();
        }
        Map<String, @Nullable Object> catalog = catalogs.get(catalogName);
        Object catalogLookup = catalog == null ? null : catalog.get(alias);
        if (catalogLookup == null) {
            return new Resolution("misconfiguration", catalogName, null, new PnpmError(
                    "CATALOG_ENTRY_NOT_FOUND_FOR_SPEC",
                    "No catalog entry '" + alias + "' was found for catalog '" + catalogName + "'."));
        }
        String lookup = (String) catalogLookup;
        if (parseCatalogProtocol(lookup) != null) {
            return new Resolution("misconfiguration", catalogName, null, new PnpmError(
                    "CATALOG_ENTRY_INVALID_RECURSIVE_DEFINITION",
                    "Found invalid catalog entry using the catalog protocol recursively. The entry for '" + alias +
                    "' in catalog '" + catalogName + "' is invalid."));
        }
        String protocolOfLookup = lookup.split(":", -1)[0];
        if ("workspace".equals(protocolOfLookup)) {
            return new Resolution("misconfiguration", catalogName, null, new PnpmError(
                    "CATALOG_ENTRY_INVALID_WORKSPACE_SPEC",
                    "The workspace protocol cannot be used as a catalog value. The entry for '" + alias +
                    "' in catalog '" + catalogName + "' is invalid."));
        }
        if (Arrays.asList("link", "file").contains(protocolOfLookup)) {
            return new Resolution("misconfiguration", catalogName, null, new PnpmError(
                    "CATALOG_ENTRY_INVALID_SPEC",
                    "The entry for '" + alias + "' in catalog '" + catalogName + "' declares a dependency using the '" +
                    protocolOfLookup + "' protocol. " +
                    "This is not yet supported, but may be in a future version of pnpm."));
        }
        return new Resolution("found", catalogName, lookup, null);
    }
}
