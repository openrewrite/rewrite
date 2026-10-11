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
 * Bun's reading of the files the dependency recipes edit: OverrideMap's field, the root package.json's catalogs as
 * Package.parse reads them, and its add, which sorts the field it saves to only.
 */
public final class BunManifestRules implements ManifestRules {

    public static final BunManifestRules RULES = new BunManifestRules();

    private BunManifestRules() {
    }

    @Override
    public OverridePlace overridePlace(String rootManifest, @Nullable Map<String, @Nullable Object> workspace,
                                       int release) {
        return "resolutions".equals(OverrideMap.readsFrom(rootManifest)) ? OverridePlace.RESOLUTIONS :
                OverridePlace.OVERRIDES;
    }

    // OverrideMap is flat (override-nested)
    @Override
    public @Nullable String overrideDeclined(String packageName, List<DependencyPathSegment> path) {
        return path.isEmpty() ? null : "bun does not support nested overrides";
    }

    @Override
    public String catalogFile() {
        return "package.json";
    }

    // Package.parse: the workspaces value when it holds a catalog or catalogs, else the root when it has workspaces
    @Override
    public @Nullable List<String> catalogsAt(String text) {
        Json.Expr json = parsed(text);
        Json.Expr workspaces = json == null ? null : json.get("workspaces");
        if (workspaces == null) {
            return null;
        }
        return workspaces.get("catalog") != null || workspaces.get("catalogs") != null ?
                singletonList("workspaces") : emptyList();
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
        return CatalogMap.groupOf(value);
    }

    @Override
    public List<String> sortedOnAdd(String field) {
        return singletonList(field);
    }
}
