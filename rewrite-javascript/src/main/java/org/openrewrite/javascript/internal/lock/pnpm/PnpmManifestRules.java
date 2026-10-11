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
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.DependencyPathSegment;
import org.openrewrite.javascript.internal.lock.EcmaScript;
import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.openrewrite.javascript.internal.lock.LockOnlyInstall;
import org.openrewrite.javascript.internal.lock.ManifestRules;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.javascript.internal.lock.SpecUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * pnpm's reading of the files the dependency recipes edit: overrides where the release reads them, in its selector
 * syntax, the catalogs and settings of pnpm-workspace.yaml, the release that installs, and pnpm add's save.
 */
public final class PnpmManifestRules implements ManifestRules {

    public static final PnpmManifestRules RULES = new PnpmManifestRules();

    private PnpmManifestRules() {
    }

    // pnpm 9 reads package.json's overrides only, 10 instead of pnpm-workspace.yaml's when it holds any
    // (override-pnpm9-reads-pnpm-field, override-pnpm10-recipe-beside-resolutions), 11 pnpm-workspace.yaml's only
    @Override
    public OverridePlace overridePlace(String rootManifest, @Nullable Map<String, @Nullable Object> workspace,
                                       int release) {
        Map<String, @Nullable Object> root = parsed(rootManifest);
        boolean pnpmField = root.get("pnpm") != null;
        Object pnpm = root.get("pnpm");
        boolean holdsOverrides = holdsMembers(root.get("resolutions")) ||
                                 pnpm instanceof Map && holdsMembers(((Map<?, ?>) pnpm).get("overrides"));
        if (release == 9 || release == 10 && (holdsOverrides || !(workspace != null &&
                                                                    holdsMembers(workspace.get("overrides"))) &&
                                                                  pnpmField)) {
            // {...resolutions, ...pnpm?.overrides}; a null pnpm field: override-pnpm10-recipe-null-pnpm-*
            return !pnpmField && root.containsKey("pnpm") && holdsMembers(root.get("resolutions")) ?
                    OverridePlace.RESOLUTIONS : OverridePlace.PNPM_FIELD;
        }
        return OverridePlace.WORKSPACE_MANIFEST;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable Object> parsed(String rootManifest) {
        try {
            Object parsed = EcmaScript.jsonParse(rootManifest);
            return parsed instanceof Map ? (Map<String, @Nullable Object>) parsed : emptyMap();
        } catch (RuntimeException e) {
            return emptyMap();
        }
    }

    private static boolean holdsMembers(@Nullable Object value) {
        return value instanceof Map && !((Map<?, ?>) value).isEmpty();
    }

    // the selector of the whole path, which pnpm reads with one parent only
    @Override
    public @Nullable String overrideDeclined(String packageName, List<DependencyPathSegment> path) {
        StringBuilder selector = new StringBuilder();
        for (DependencyPathSegment segment : path) {
            selector.append(segment(segment)).append('>');
        }
        selector.append(packageName);
        String rejected = ParseOverrides.rejects(selector.toString());
        return rejected == null ? null : "pnpm rejects the override selector " + selector + ": " + rejected;
    }

    // one parent, a>b (override-recipe-path)
    @Override
    public String overrideKey(String packageName, List<DependencyPathSegment> path, List<String> existing) {
        return path.isEmpty() ? packageName : segment(path.get(path.size() - 1)) + ">" + packageName;
    }

    @Override
    public @Nullable String outranked(String packageName, List<String> keys,
                                      Map<String, @Nullable Object> resolutions) {
        List<String> before = new ArrayList<>();
        for (String key : keys) {
            int at = key.lastIndexOf('>');
            if (at > 0 && packageName.equals(nameOf(key.substring(at + 1)))) {
                before.add(key);
            }
        }
        return before.isEmpty() ? null : "pnpm applies " + String.join(", ", before) +
                                         " before a global override of " + packageName;
    }

    // a selector's package name, without the range after its name's @
    private static String nameOf(String selector) {
        int at = selector.indexOf('@', selector.startsWith("@") ? 1 : 0);
        return at < 0 ? selector : selector.substring(0, at);
    }

    private static String segment(DependencyPathSegment segment) {
        return segment.getVersion() == null ? segment.getName() : segment.getName() + "@" + segment.getVersion();
    }

    @Override
    public String catalogFile() {
        return WorkspaceRootFinder.WORKSPACE_MANIFEST_FILENAME;
    }

    @Override
    public List<String> catalogsAt(String text) {
        return emptyList();
    }

    // getCatalogsFromWorkspaceManifest over the file as its reader reads it
    @Override
    public Map<String, Map<String, String>> catalogs(String text) {
        Map<String, @Nullable Map<String, @Nullable Object>> read;
        try {
            read = Catalogs.getCatalogsFromWorkspaceManifest(WorkspaceManifest.readWorkspaceManifest(text));
        } catch (PnpmError e) {
            throw Rejected.coded(Reason.MALFORMED_MANIFEST, null, e.getCode(), e.getCode() + " " + e.getMessage());
        }
        Map<String, Map<String, String>> catalogs = new LinkedHashMap<>();
        for (Map.Entry<String, @Nullable Map<String, @Nullable Object>> catalog : read.entrySet()) {
            if (catalog.getValue() == null) {
                continue;
            }
            Map<String, String> entries = new LinkedHashMap<>();
            for (Map.Entry<String, @Nullable Object> entry : catalog.getValue().entrySet()) {
                if (entry.getValue() instanceof String) {
                    entries.put(entry.getKey(), (String) entry.getValue());
                }
            }
            catalogs.put(catalog.getKey(), entries);
        }
        return catalogs;
    }

    // pnpm add and update rewrite pnpm-workspace.yaml through the workspace manifest writer
    @Override
    public boolean writesCatalogFile() {
        return true;
    }

    @Override
    public @Nullable String catalogOf(String value) {
        return Catalogs.parseCatalogProtocol(value);
    }

    @Override
    public String workspaceSettingsFile() {
        return WorkspaceRootFinder.WORKSPACE_MANIFEST_FILENAME;
    }

    // the root-finder's find-up, misspellings included, which getConfig rejects
    @Override
    public @Nullable String workspaceRoot(Predicate<String> holds, String dir) {
        String found = WorkspaceRootFinder.findWorkspaceManifest(holds, dir);
        if (found == null) {
            return null;
        }
        String expected = WorkspaceRootFinder.WORKSPACE_MANIFEST_FILENAME;
        if (!expected.equals(found.substring(found.lastIndexOf('/') + 1))) {
            String code = "ERR_PNPM_BAD_WORKSPACE_MANIFEST_NAME";
            throw Rejected.coded(Reason.MALFORMED_MANIFEST, null, code,
                    "pnpm finds " + found + " where it reads " + expected + " (" + code + ")");
        }
        return RootRelativePath.dirname(found);
    }

    @Override
    public @Nullable Map<String, @Nullable Object> workspaceSettings(@Nullable String text) {
        return WorkspaceManifest.readWorkspaceManifest(text);
    }

    // Main.release, corepack's findProjectSpec asked from the root; a manifest pnpm cannot read is no CannotKnow
    @Override
    public String release(String rootManifest, @Nullable Map<String, @Nullable Object> workspace) {
        PnpmOptions.WantedPackageManager wanted;
        try {
            wanted = PnpmOptions.wantedPackageManager(rootManifest, workspace);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        return Main.release(wanted, () -> SpecUtils.findProjectSpec("", 0, dir -> dir.isEmpty() ? rootManifest : null,
                path -> false, "pnpm", HostEnvironment.UNKNOWN));
    }

    @Override
    public Saved added(String alias, String specifier, @Nullable Map<String, @Nullable Object> workspace) {
        Install.Saved saved;
        try {
            saved = Install.installSome(alias, specifier, workspace);
        } catch (Rejected e) {
            throw Rejected.rejects(e.getReason(), e.getPackageName(), LockOnlyInstall.PNPM.failure(e).getDetail());
        }
        return new Saved(saved.getBareSpecifier(), saved.getCatalogName());
    }

    @Override
    public List<String> sortedOnAdd(String field) {
        return DEPENDENCY_FIELDS;
    }

    @Override
    public boolean keepsEmptyFieldsOnAdd() {
        return false;
    }
}
