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
package org.openrewrite.javascript.internal.lock.bun;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.DependencyPathSegment;
import org.openrewrite.javascript.internal.lock.ManifestRules;
import org.openrewrite.javascript.internal.lock.Rejected;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Bun's reading of the files the dependency recipes edit, as the release that installs reads them
 * ({@link #installedAs}): OverrideMap's field and the rules it reads (flat before 1.4.0), the root package.json's
 * catalogs as Package.parse reads them (none before 1.2.14, the workspaces object's alone before 1.2.19), and its add,
 * which sorts the field it saves to only.
 */
public final class BunManifestRules implements ManifestRules {

    /**
     * The rules of {@link BunRelease#PORTED}, the release that installs where the LST records none.
     */
    public static final BunManifestRules RULES = new BunManifestRules(BunRelease.PORTED, null);

    private final @Nullable BunRelease release;

    /**
     * Why the release that installs is not known, where {@link #release} is null.
     */
    private final @Nullable String unknown;

    private BunManifestRules(@Nullable BunRelease release, @Nullable String unknown) {
        this.release = release;
        this.unknown = unknown;
    }

    static BunManifestRules of(BunRelease release) {
        return new BunManifestRules(release, null);
    }

    // the release the NodeRuntime marker names, as the lock regeneration installs it (BunInputs)
    @Override
    public ManifestRules installedAs(@Nullable String version) {
        try {
            return of(BunReleases.installedAs(version));
        } catch (CannotKnow e) {
            return new BunManifestRules(null, e.getMessage());
        }
    }

    @Override
    public OverridePlace overridePlace(String rootManifest, @Nullable Map<String, @Nullable Object> workspace,
                                       int release) {
        return "resolutions".equals(OverrideMap.readsFrom(rootManifest)) ? OverridePlace.RESOLUTIONS :
                OverridePlace.OVERRIDES;
    }

    // OverrideMap is flat (override-nested) until 1.4.0, which reads what the recipe writes one parent deep and warns
    // of the rest (resolutions-nested-path): a nested object in overrides, a Yarn path in resolutions
    @Override
    public @Nullable String overrideDeclined(String packageName, String newVersion, List<DependencyPathSegment> path,
                                             OverridePlace place) {
        if (path.isEmpty()) {
            return null;
        }
        if (release == null) {
            return unknown;
        }
        if (!release.readsScopedOverrides()) {
            return "bun does not support nested overrides";
        }
        StringBuilder written = new StringBuilder("{");
        if (place == OverridePlace.RESOLUTIONS) {
            written.append("\"resolutions\":{");
            Strings.appendJsonString(written, overrideKey(packageName, path, emptyList()), true);
        } else {
            written.append("\"overrides\":");
            for (DependencyPathSegment segment : path) {
                written.append('{');
                Strings.appendJsonString(written, segment(segment), true);
                written.append(':');
            }
            written.append('{');
            Strings.appendJsonString(written, packageName, true);
        }
        written.append(':');
        Strings.appendJsonString(written, newVersion, true);
        for (int i = place == OverridePlace.RESOLUTIONS ? 0 : path.size(); i >= 0; i--) {
            written.append('}');
        }
        return OverrideMap.warning(release, written.append('}').toString());
    }

    // override_selector.rs's Yarn path, parent/child, read with each package's range
    @Override
    public String overrideKey(String packageName, List<DependencyPathSegment> path, List<String> existing) {
        StringBuilder key = new StringBuilder();
        for (DependencyPathSegment segment : path) {
            key.append(segment(segment)).append('/');
        }
        return key.append(packageName).toString();
    }

    private static String segment(DependencyPathSegment segment) {
        return segment.getVersion() == null ? segment.getName() : segment.getName() + "@" + segment.getVersion();
    }

    // none before 1.2.14, whose catalog: is a git spec (BunRelease.parsesCatalogSpecs)
    @Override
    public @Nullable String catalogFile() {
        return readsCatalogs() ? "package.json" : null;
    }

    // Package.parse: the workspaces value when it holds a catalog or catalogs, else the root when it has workspaces;
    // before 1.2.19 the workspaces object alone (BunRelease.readsTopLevelCatalogs)
    @Override
    public @Nullable List<String> catalogsAt(String text) {
        if (!readsCatalogs()) {
            return null;
        }
        Json.Expr json = parsed(text);
        Json.Expr workspaces = json == null ? null : json.get("workspaces");
        if (workspaces == null) {
            return null;
        }
        if (release != null && !release.readsTopLevelCatalogs()) {
            return workspaces.isObject() ? singletonList("workspaces") : null;
        }
        return workspaces.get("catalog") != null || workspaces.get("catalogs") != null ?
                singletonList("workspaces") : emptyList();
    }

    // where the release is not known, as 1.3.10 reads them
    private boolean readsCatalogs() {
        return release == null || release.parsesCatalogSpecs();
    }

    @Override
    public Map<String, Map<String, String>> catalogs(String text) {
        List<String> at = catalogsAt(text);
        if (at == null) {
            return Collections.emptyMap();
        }
        Json.Expr base = Json.parse(text);
        for (String key : at) {
            base = base.get(key);
        }
        Log log = new Log();
        CatalogMap catalogs = new CatalogMap();
        catalogs.parseAppend(log, base);
        if (log.hasErrors()) {
            throw Rejected.rejects(Reason.MALFORMED_MANIFEST, "bun rejects the root package.json's catalogs: " +
                                                              String.join(", ", log.errors));
        }
        Map<String, Map<String, String>> read = new LinkedHashMap<>();
        if (base.get("catalog") != null || !catalogs.defaultMap.isEmpty()) {
            read.put("", specifiers(catalogs.defaultMap));
        }
        for (Map.Entry<String, LinkedHashMap<String, Dependency>> group : catalogs.groups.entrySet()) {
            read.put(group.getKey(), specifiers(group.getValue()));
        }
        return read;
    }

    private static Map<String, String> specifiers(Map<String, Dependency> group) {
        Map<String, String> specifiers = new LinkedHashMap<>();
        for (Map.Entry<String, Dependency> entry : group.entrySet()) {
            specifiers.put(entry.getKey(), entry.getValue().version.literal);
        }
        return specifiers;
    }

    private static Json.@Nullable Expr parsed(String text) {
        try {
            return Json.parse(text);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public @Nullable String catalogOf(String value) {
        return readsCatalogs() ? CatalogMap.groupOf(value) : null;
    }

    @Override
    public List<String> sortedOnAdd(String field) {
        return singletonList(field);
    }
}
