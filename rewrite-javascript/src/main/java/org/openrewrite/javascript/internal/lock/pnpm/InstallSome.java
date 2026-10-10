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
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.semver.NodeSemver;

import java.util.List;
import java.util.Map;

/**
 * Port of @pnpm/installing.deps-installer src/install/index.ts installSome's catalogMode check (pnpm 11.2.2) for
 * {@code pnpm add <alias>@<specifier>}, between {@link ParseWantedDependencies} and {@link ResolveDependencyTree}.
 */
public final class InstallSome {

    private static final String CATALOG_PROTOCOL = "catalog:";
    private static final String DEFAULT = "default";

    private InstallSome() {
    }

    /**
     * The specifier pnpm writes into the manifest, and the catalog it adds {@code alias: specifier} to (null for
     * none).
     */
    @Value
    public static class Saved {
        String bareSpecifier;

        @Nullable
        String catalogName;
    }

    /**
     * What pnpm add saves, the workspace manifest as its reader reads it (null without one); a {@link Rejected} for
     * what pnpm itself rejects or fails on.
     */
    public static Saved add(String alias, String bareSpecifier, @Nullable Map<String, @Nullable Object> workspace) {
        Map<String, @Nullable Map<String, @Nullable Object>> catalogs;
        try {
            catalogs = Catalogs.getCatalogsFromWorkspaceManifest(workspace);
        } catch (PnpmError e) {
            throw Rejected.rejects(Reason.RESOLUTION_REQUIRED, alias, "[" + e.getCode() + "] " + e.getMessage());
        }
        Map<String, @Nullable Object> defaultCatalog = catalogs.get(DEFAULT);
        String wanted = ParseWantedDependencies.bareSpecifier(alias, bareSpecifier, defaultCatalog);
        String saveCatalogName = saveCatalogName(workspace);
        if (!"manual".equals(catalogMode(workspace))) {
            String perDepCatalogName = saveCatalogName == null ? DEFAULT : saveCatalogName;
            String catalogBareSpecifier = CATALOG_PROTOCOL + (DEFAULT.equals(perDepCatalogName) ? "" :
                    perDepCatalogName);
            Catalogs.Resolution catalog = Catalogs.resolveFromCatalog(catalogs, alias, catalogBareSpecifier);
            String catalogDepSpecifier = "found".equals(catalog.getType()) ? catalog.getSpecifier() : null;
            if (catalogDepSpecifier == null || wanted.equals(catalogBareSpecifier) || eq(alias, wanted,
                    catalogDepSpecifier)) {
                saveCatalogName = perDepCatalogName;
            } else if ("strict".equals(catalogMode(workspace))) {
                throw Rejected.rejects(Reason.RESOLUTION_REQUIRED, alias, "[ERR_PNPM_CATALOG_VERSION_MISMATCH] " +
                                                                          "Wanted dependency outside the version " +
                                                                          "range defined in catalog");
            }
        }
        String lookup = CATALOG_PROTOCOL.equals(wanted) ? null :
                ResolveDependencyTree.catalogLookup(alias, saveCatalogName, defaultCatalog);
        return lookup == null ? new Saved(wanted, null) : new Saved(lookup, saveCatalogName);
    }

    // semver.validRange of both, then semver.eq, which takes versions only
    private static boolean eq(String alias, String wanted, String catalogDepSpecifier) {
        if (NodeSemver.validRange(wanted, NodeSemver.Options.DEFAULT) == null ||
            NodeSemver.validRange(catalogDepSpecifier, NodeSemver.Options.DEFAULT) == null) {
            return false;
        }
        try {
            return NodeSemver.compare(wanted, catalogDepSpecifier, NodeSemver.Options.DEFAULT) == 0;
        } catch (IllegalArgumentException e) {
            throw Rejected.crashes(Reason.RESOLUTION_REQUIRED, alias, "TypeError: " + e.getMessage());
        }
    }

    // getConfig's catalogMode: manual when unset
    static String catalogMode(@Nullable Map<String, @Nullable Object> workspace) {
        Object mode = workspace == null || !workspace.containsKey("catalogMode") ? "manual" :
                workspace.get("catalogMode");
        return mode == null ? "null" : Js.string(mode);
    }

    static @Nullable String saveCatalogName(@Nullable Map<String, @Nullable Object> workspace) {
        Object value = workspace == null ? null : workspace.get("saveCatalogName");
        return value == null || value instanceof Map || value instanceof List ? null : Js.string(value);
    }
}
