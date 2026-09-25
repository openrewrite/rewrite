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

    /** Characters YAML reads as an indicator when a plain scalar opens with one. */
    private static final String PLAIN_SCALAR_INDICATORS = "-?:,[]{}#&*!|>'\"%@`";

    private static final String PNPM_WORKSPACE_FILE = "pnpm-workspace.yaml";
    private static final String YARN_WORKSPACE_FILE = ".yarnrc.yml";

    /** True when the basename is a workspace file that can declare catalogs. */
    public static boolean isWorkspaceFile(String basename) {
        return PNPM_WORKSPACE_FILE.equals(basename) || YARN_WORKSPACE_FILE.equals(basename);
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
        return CATALOG_PROTOCOL.equals(PackageJsonHelper.dependencySpecifierProtocol(value)) ?
                value.substring(CATALOG_PROTOCOL.length()) :
                null;
    }

    /**
     * Every catalog reference a manifest makes, as package name to catalog name. Read from the JSON
     * rather than from {@code NodeResolutionResult}, so a member that never resolved still counts as a
     * consumer of the entry it references.
     */
    public static Map<String, String> catalogReferences(Json.Document packageJson) {
        Map<String, String> references = new LinkedHashMap<>();
        for (Map.Entry<String, String> declared : PackageJsonHelper.declaredVersions(packageJson).entrySet()) {
            String catalog = catalogReference(declared.getValue());
            if (catalog != null) {
                references.put(declared.getKey(), catalog);
            }
        }
        return references;
    }

    /** Whether the catalog declares this package at all; the version behind it is never needed. */
    public static boolean hasEntry(Yaml.Documents workspaceFile, String catalogName, String packageName) {
        for (Yaml.Document document : workspaceFile.getDocuments()) {
            if (!(document.getBlock() instanceof Yaml.Mapping)) continue;
            Yaml.Mapping catalog = catalogMapping((Yaml.Mapping) document.getBlock(), catalogName);
            if (catalog != null && childScalar(catalog, packageName) != null) {
                return true;
            }
        }
        return false;
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

    private static Yaml.@Nullable Scalar childScalar(Yaml.Mapping mapping, String key) {
        for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
            if (key.equals(entry.getKey().getValue()) && entry.getValue() instanceof Yaml.Scalar) {
                return (Yaml.Scalar) entry.getValue();
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
            Yaml.Scalar.Style style = version.getStyle();
            if (style == Yaml.Scalar.Style.LITERAL || style == Yaml.Scalar.Style.FOLDED) {
                // `withValue` cannot rewrite a block scalar's body without clobbering its envelope, and a
                // constraint has no business being one. Decline rather than corrupt the file.
                return catalog;
            }
            Yaml.Scalar rewritten = version.withValue(newVersion);
            if (style == Yaml.Scalar.Style.PLAIN && !canBePlainScalar(newVersion)) {
                rewritten = rewritten.withStyle(Yaml.Scalar.Style.SINGLE_QUOTED);
            }
            entries.set(i, entry.withValue(rewritten));
            return catalog.withEntries(entries);
        }
        return catalog;
    }

    /**
     * Whether a value can stand unquoted where the old one did. An npm range may open with a character
     * YAML reads as an indicator, so keeping the old scalar's style would emit something that no longer
     * parses: `>=2.0.0` reads as a folded block scalar and `*` as an alias.
     * <p>
     * rewrite-yaml knows this rule too, but only in its `internal` package, which is not API and may
     * move without notice. The rule is short and stable enough to state here rather than couple to it.
     */
    private static boolean canBePlainScalar(String value) {
        if (value.isEmpty() || "---".equals(value) || "...".equals(value)) {
            return false;
        }
        if (PLAIN_SCALAR_INDICATORS.indexOf(value.charAt(0)) >= 0 || !value.equals(value.trim())) {
            return false;
        }
        // `: ` opens a mapping value and ` #` a comment, wherever they appear.
        if (value.contains(": ") || value.contains(" #")) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                return false;
            }
        }
        return true;
    }
}
