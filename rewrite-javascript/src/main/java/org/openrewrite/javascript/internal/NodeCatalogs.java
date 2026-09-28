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
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;


/**
 * pnpm 9.5+ and Yarn 4.10+ let a workspace declare one version per package in a catalog and have every
 * member reference it from its {@code package.json} as {@code catalog:} or {@code catalog:<name>}. Both
 * spell catalogs the same way, pnpm in {@code pnpm-workspace.yaml} and Yarn in {@code .yarnrc.yml}, so
 * one reader serves both.
 */
@UtilityClass
public class NodeCatalogs {

    /** The default catalog has no name; {@code catalog:stable} names one. */
    public static final String DEFAULT_CATALOG = "";

    @Value
    public static class CatalogEntry {
        String catalogName;
        String packageName;
    }

    private static final String CATALOG_PROTOCOL = "catalog:";
    private static final String DEFAULT_CATALOG_KEY = "catalog";
    private static final String NAMED_CATALOGS_KEY = "catalogs";

    /** Plain scalars YAML resolves to something other than a string; `2` and `2.0` are valid npm ranges. */
    private static final Pattern YAML_NON_STRING = Pattern.compile(
            "[-+]?(\\.[0-9][0-9_]*|[0-9][0-9_]*(\\.[0-9_]*)?)([eE][-+]?[0-9]+)?" +
                    "|[-+]?0[xXbBoO][0-9a-fA-F_]+" +
                    "|[-+]?\\.(inf|Inf|INF)|\\.(nan|NaN|NAN)" +
                    "|~|[nN]ull|NULL" +
                    "|[tT]rue|TRUE|[fF]alse|FALSE|[yY]es|YES|[nN]o|NO|[oO]n|ON|[oO]ff|OFF");

    private static final String PLAIN_SCALAR_INDICATORS = "-?:,[]{}#&*!|>'\"%@`";

    private static final String PNPM_WORKSPACE_FILE = "pnpm-workspace.yaml";
    private static final String YARN_WORKSPACE_FILE = ".yarnrc.yml";

    public static boolean isWorkspaceFile(String basename) {
        return PNPM_WORKSPACE_FILE.equals(basename) || YARN_WORKSPACE_FILE.equals(basename);
    }

    /** A repository can contain both files, so the manager decides which is read, not whichever exists. */
    public static @Nullable String workspaceFileFor(@Nullable PackageManager pm) {
        if (pm == PackageManager.Pnpm) {
            return PNPM_WORKSPACE_FILE;
        }
        return pm == PackageManager.YarnBerry ? YARN_WORKSPACE_FILE : null;
    }

    /** The catalog a version position refers to, {@link #DEFAULT_CATALOG} for a bare {@code catalog:}. */
    public static @Nullable String catalogReference(@Nullable String value) {
        return CATALOG_PROTOCOL.equals(PackageJsonHelper.dependencySpecifierProtocol(value)) ?
                value.substring(CATALOG_PROTOCOL.length()) :
                null;
    }

    /**
     * Three answers in one, because callers need to tell them apart: null when the catalog declares no
     * such entry, {@code workspaceFile} itself when it does but already holds {@code newVersion}, and a
     * rewritten file otherwise. Rewriting the scalar in place is what preserves its quoting style.
     */
    public static @Nullable SourceFile updateEntry(SourceFile workspaceFile, String catalogName,
                                                   String packageName, String newVersion) {
        if (!(workspaceFile instanceof Yaml.Documents)) {
            return null;
        }
        Yaml.Documents documents = (Yaml.Documents) workspaceFile;
        List<Yaml.Document> updated = new ArrayList<>(documents.getDocuments());
        boolean[] found = {false};
        boolean changed = false;
        for (int i = 0; i < updated.size(); i++) {
            Yaml.Document document = updated.get(i);
            if (!(document.getBlock() instanceof Yaml.Mapping)) continue;
            Yaml.Mapping root = (Yaml.Mapping) document.getBlock();
            UnaryOperator<Yaml.Mapping> setVersion = catalog -> withVersion(catalog, packageName, newVersion, found);
            Yaml.Mapping rewritten = DEFAULT_CATALOG.equals(catalogName) ?
                    withMapping(root, DEFAULT_CATALOG_KEY, setVersion) :
                    withMapping(root, NAMED_CATALOGS_KEY, catalogs -> withMapping(catalogs, catalogName, setVersion));
            if (rewritten != root) {
                updated.set(i, document.withBlock(rewritten));
                changed = true;
            }
        }
        if (!found[0]) {
            return null;
        }
        return changed ? documents.withDocuments(updated) : workspaceFile;
    }

    /** Returns {@code mapping} itself when nothing changed, so callers can compare by identity. */
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

    private static Yaml.Mapping withVersion(Yaml.Mapping catalog, String packageName, String newVersion,
                                            boolean[] found) {
        List<Yaml.Mapping.Entry> entries = new ArrayList<>(catalog.getEntries());
        for (int i = 0; i < entries.size(); i++) {
            Yaml.Mapping.Entry entry = entries.get(i);
            if (!packageName.equals(entry.getKey().getValue()) || !(entry.getValue() instanceof Yaml.Scalar)) continue;
            found[0] = true;
            Yaml.Scalar version = (Yaml.Scalar) entry.getValue();
            if (newVersion.equals(version.getValue())) {
                return catalog;
            }
            Yaml.Scalar.Style style = version.getStyle();
            if (style == Yaml.Scalar.Style.LITERAL || style == Yaml.Scalar.Style.FOLDED) {
                // `withValue` cannot rewrite a block scalar's body without clobbering its envelope.
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
     * An npm range can open with a YAML indicator (`>=2.0.0` reads as a folded scalar, `*` as an alias) or
     * resolve to another type (`2` as an integer), so an entry that was unquoted cannot always stay that
     * way. rewrite-yaml has this rule too, but only in its `internal` package, which is not API.
     */
    static boolean canBePlainScalar(String value) {
        if (value.isEmpty() || "---".equals(value) || "...".equals(value)) {
            return false;
        }
        if (PLAIN_SCALAR_INDICATORS.indexOf(value.charAt(0)) >= 0 || !value.equals(value.trim())) {
            return false;
        }
        // `: ` opens a mapping value and ` #` a comment, wherever they appear.
        if (value.contains(": ") || value.contains(" #") || YAML_NON_STRING.matcher(value).matches()) {
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
