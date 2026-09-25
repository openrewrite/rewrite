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
package org.openrewrite.javascript.internal;

import lombok.Value;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.tree.Json;
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyMap;

/**
 * pnpm 9.5+ and Yarn 4.10+ let a workspace declare one version per package in a catalog and have every
 * member reference it from its {@code package.json} as {@code catalog:} or {@code catalog:<name>}. Both
 * spell catalogs the same way, pnpm in {@code pnpm-workspace.yaml} and Yarn in {@code .yarnrc.yml}, so
 * one reader serves both.
 */
@UtilityClass
public class NodeCatalogs {

    /** The default catalog has no name; {@code catalog:strict} names one. */
    public static final String DEFAULT_CATALOG = "";

    /** A catalog entry's coordinates within a workspace file. */
    @Value
    public static class CatalogEntry {
        String catalogName;
        String packageName;
    }

    private static final String CATALOG_PROTOCOL = "catalog:";
    private static final String DEFAULT_CATALOG_KEY = "catalog";
    private static final String NAMED_CATALOGS_KEY = "catalogs";

    private static final String PNPM_WORKSPACE_FILE = "pnpm-workspace.yaml";
    private static final String YARN_WORKSPACE_FILE = ".yarnrc.yml";

    private static final Set<String> WORKSPACE_FILE_NAMES =
            new LinkedHashSet<>(Arrays.asList(PNPM_WORKSPACE_FILE, YARN_WORKSPACE_FILE));

    /** True when the basename is a workspace file that can declare catalogs. */
    public static boolean isWorkspaceFile(String basename) {
        return WORKSPACE_FILE_NAMES.contains(basename);
    }

    /**
     * The workspace file a manager keeps its catalogs in, or null for a manager that has none. A
     * repository can contain both files, so the manager decides which one is read rather than whichever
     * happens to be present.
     */
    public static @Nullable String workspaceFileFor(@Nullable PackageManager pm) {
        if (pm == PackageManager.Pnpm) {
            return PNPM_WORKSPACE_FILE;
        }
        return pm == PackageManager.YarnBerry ? YARN_WORKSPACE_FILE : null;
    }

    /**
     * The catalog a manifest's version position refers to, or null when it holds something else. Returns
     * {@link #DEFAULT_CATALOG} for the bare {@code catalog:} and the name for {@code catalog:<name>}.
     */
    public static @Nullable String catalogReference(@Nullable String value) {
        return value == null || !value.startsWith(CATALOG_PROTOCOL) ?
                null :
                value.substring(CATALOG_PROTOCOL.length());
    }

    /**
     * Every catalog reference a manifest makes, as package name to catalog name. Read from the JSON
     * rather than from {@code NodeResolutionResult}, so a member that never resolved still counts as a
     * consumer of the entry it references.
     */
    public static Map<String, String> catalogReferences(SourceFile packageJson) {
        if (!(packageJson instanceof Json.Document) ||
                !(((Json.Document) packageJson).getValue() instanceof Json.JsonObject)) {
            return emptyMap();
        }
        Map<String, String> references = new LinkedHashMap<>();
        for (Json rootMember : ((Json.JsonObject) ((Json.Document) packageJson).getValue()).getMembers()) {
            if (!(rootMember instanceof Json.Member)) continue;
            Json.Member scope = (Json.Member) rootMember;
            String scopeKey = PackageJsonHelper.literalString(scope.getKey());
            if (scopeKey == null || !PackageJsonHelper.isDeclaredScope(scopeKey) ||
                    !(scope.getValue() instanceof Json.JsonObject)) {
                continue;
            }
            for (Json child : ((Json.JsonObject) scope.getValue()).getMembers()) {
                if (!(child instanceof Json.Member)) continue;
                Json.Member dependency = (Json.Member) child;
                String name = PackageJsonHelper.literalString(dependency.getKey());
                String catalog = catalogReference(PackageJsonHelper.literalString(dependency.getValue()));
                if (name != null && catalog != null) {
                    references.put(name, catalog);
                }
            }
        }
        return references;
    }

    /** The version a catalog declares for a package, or null when the catalog or the entry is absent. */
    public static @Nullable String findEntry(SourceFile workspaceFile, String catalogName, String packageName) {
        if (!(workspaceFile instanceof Yaml.Documents)) {
            return null;
        }
        for (Yaml.Document document : ((Yaml.Documents) workspaceFile).getDocuments()) {
            if (!(document.getBlock() instanceof Yaml.Mapping)) continue;
            Yaml.Mapping catalog = catalogMapping((Yaml.Mapping) document.getBlock(), catalogName);
            if (catalog == null) continue;
            for (Yaml.Mapping.Entry entry : catalog.getEntries()) {
                if (packageName.equals(entry.getKey().getValue()) && entry.getValue() instanceof Yaml.Scalar) {
                    return ((Yaml.Scalar) entry.getValue()).getValue();
                }
            }
        }
        return null;
    }

    /**
     * Set a catalog entry's version, returning {@code workspaceFile} unchanged when the entry is absent or
     * already holds {@code newVersion}. Rewriting the scalar in place keeps its quoting style, so
     * {@code '~1.4.1'} becomes {@code '~1.5.0'} rather than losing its quotes.
     */
    public static SourceFile updateEntry(SourceFile workspaceFile, String catalogName, String packageName,
                                         String newVersion) {
        if (!(workspaceFile instanceof Yaml.Documents)) {
            return workspaceFile;
        }
        Yaml.Documents documents = (Yaml.Documents) workspaceFile;
        List<Yaml.Document> updated = new ArrayList<>(documents.getDocuments());
        boolean changed = false;
        for (int i = 0; i < updated.size(); i++) {
            Yaml.Document document = updated.get(i);
            if (!(document.getBlock() instanceof Yaml.Mapping)) continue;
            Yaml.Mapping root = (Yaml.Mapping) document.getBlock();
            UnaryOperator<Yaml.Mapping> setVersion = catalog -> withVersion(catalog, packageName, newVersion);
            Yaml.Mapping rewritten = DEFAULT_CATALOG.equals(catalogName) ?
                    withMapping(root, DEFAULT_CATALOG_KEY, setVersion) :
                    withMapping(root, NAMED_CATALOGS_KEY, catalogs -> withMapping(catalogs, catalogName, setVersion));
            if (rewritten != root) {
                updated.set(i, document.withBlock(rewritten));
                changed = true;
            }
        }
        return changed ? documents.withDocuments(updated) : workspaceFile;
    }

    private static Yaml.@Nullable Mapping catalogMapping(Yaml.Mapping root, String catalogName) {
        if (DEFAULT_CATALOG.equals(catalogName)) {
            return childMapping(root, DEFAULT_CATALOG_KEY);
        }
        Yaml.Mapping catalogs = childMapping(root, NAMED_CATALOGS_KEY);
        return catalogs == null ? null : childMapping(catalogs, catalogName);
    }

    private static Yaml.@Nullable Mapping childMapping(Yaml.Mapping mapping, String key) {
        for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
            if (key.equals(entry.getKey().getValue()) && entry.getValue() instanceof Yaml.Mapping) {
                return (Yaml.Mapping) entry.getValue();
            }
        }
        return null;
    }

    /** Apply {@code f} to the mapping under {@code key}, returning {@code mapping} itself when nothing changed. */
    private static Yaml.Mapping withMapping(Yaml.Mapping mapping, String key, UnaryOperator<Yaml.Mapping> f) {
        List<Yaml.Mapping.Entry> entries = new ArrayList<>(mapping.getEntries());
        for (int i = 0; i < entries.size(); i++) {
            Yaml.Mapping.Entry entry = entries.get(i);
            if (!key.equals(entry.getKey().getValue()) || !(entry.getValue() instanceof Yaml.Mapping)) continue;
            Yaml.Mapping child = (Yaml.Mapping) entry.getValue();
            Yaml.Mapping rewritten = f.apply(child);
            if (rewritten == child) {
                return mapping;
            }
            entries.set(i, entry.withValue(rewritten));
            return mapping.withEntries(entries);
        }
        return mapping;
    }

    private static Yaml.Mapping withVersion(Yaml.Mapping catalog, String packageName, String newVersion) {
        List<Yaml.Mapping.Entry> entries = new ArrayList<>(catalog.getEntries());
        for (int i = 0; i < entries.size(); i++) {
            Yaml.Mapping.Entry entry = entries.get(i);
            if (!packageName.equals(entry.getKey().getValue()) || !(entry.getValue() instanceof Yaml.Scalar)) continue;
            Yaml.Scalar version = (Yaml.Scalar) entry.getValue();
            if (newVersion.equals(version.getValue())) {
                return catalog;
            }
            entries.set(i, entry.withValue(version.withValue(newVersion)));
            return catalog.withEntries(entries);
        }
        return catalog;
    }
}
