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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.internal.lock.EcmaScript;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;
import org.openrewrite.yaml.tree.YamlKey;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

/**
 * A workspace's catalogs as its package manager reads them (pnpm from pnpm-workspace.yaml, Yarn from .yarnrc.yml, Bun
 * from the root package.json), by catalog then alias; an edit changes the document in place.
 */
@Getter
public abstract class NodeCatalogs {

    private static final String PROTOCOL = "catalog:";
    private static final Pattern YAML_NON_STRING = Pattern.compile(
            "[-+]?(\\.[0-9][0-9_]*|[0-9][0-9_]*(\\.[0-9_]*)?)([eE][-+]?[0-9]+)?" +
            "|[-+]?0[xXbBoO][0-9a-fA-F_]+" +
            "|[-+]?\\.(inf|Inf|INF)|\\.(nan|NaN|NAN)" +
            "|~|[nN]ull|NULL" +
            "|[tT]rue|TRUE|[fF]alse|FALSE|[yY]es|YES|[nN]o|NO|[oO]n|ON|[oO]ff|OFF");
    private static final String PLAIN_SCALAR_INDICATORS = "-?:,[]{}#&*!|>'\"%@`";

    /**
     * The file the package manager reads them from.
     */
    private final SourceFile document;

    private final Map<String, Map<String, String>> catalogs;

    private NodeCatalogs(SourceFile document, Map<String, Map<String, String>> catalogs) {
        this.document = document;
        this.catalogs = catalogs;
    }

    /**
     * The name of the file {@code pm} reads catalogs from, in the workspace root; null for one without catalogs.
     */
    public static @Nullable String fileName(@Nullable PackageManager pm) {
        if (pm == PackageManager.Pnpm) {
            return "pnpm-workspace.yaml";
        }
        if (pm == PackageManager.YarnBerry) {
            return ".yarnrc.yml";
        }
        return pm == PackageManager.Bun ? "package.json" : null;
    }

    /**
     * The catalogs {@code document} declares by {@code pm}'s rules; null when it is not the file {@code pm} reads them
     * from, or a Bun package.json without workspaces.
     */
    public static @Nullable NodeCatalogs read(@Nullable PackageManager pm, SourceFile document) {
        if (pm == PackageManager.Pnpm && document instanceof Yaml.Documents) {
            return new Pnpm((Yaml.Documents) document);
        }
        if (pm == PackageManager.YarnBerry && document instanceof Yaml.Documents) {
            return new YarnBerry((Yaml.Documents) document);
        }
        if (pm == PackageManager.Bun && document instanceof Json.Document &&
            Bun.base((Json.Document) document) != null) {
            return new Bun((Json.Document) document);
        }
        return null;
    }

    /**
     * What a declared dependency value is ({@link Spec#ofDependency}); null for one npa rejects outright.
     */
    public static @Nullable Spec classify(String name, String value) {
        try {
            return Spec.ofDependency(name, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Path getPath() {
        return document.getSourcePath();
    }

    /**
     * The catalog a dependency value names, by the package manager's rules; null when it is no catalog reference.
     */
    public abstract @Nullable String catalogOf(String value);

    /**
     * The specifier the catalog's entry for {@code alias} holds; null when there is none.
     */
    public @Nullable String specifier(String catalog, String alias) {
        Map<String, String> entries = catalogs.get(catalog);
        return entries == null ? null : entries.get(alias);
    }

    /**
     * The specifier a dependency on {@code alias} declared as {@code value} resolves to through these catalogs; null
     * when the value is no catalog reference or the catalog has no such entry.
     */
    public @Nullable String resolve(String alias, String value) {
        String catalog = catalogOf(value);
        return catalog == null ? null : specifier(catalog, alias);
    }

    /**
     * The catalogs with the entry's specifier replaced in place, quoting kept: null when the catalog has no such entry
     * or one that is not a version range (a block scalar, another protocol), these catalogs when it already holds it.
     */
    public abstract @Nullable NodeCatalogs withSpecifier(String catalog, String alias, String specifier);

    /**
     * The catalogs with {@code alias: specifier} in the catalog, its entry replaced or added where pnpm's workspace
     * manifest writer puts a new key, the catalog created when there is none; null when it cannot be added in place.
     */
    public abstract @Nullable NodeCatalogs withEntry(String catalog, String alias, String specifier);

    public String print() {
        return printed(document);
    }

    /**
     * Whether a catalog is written in YAML's flow style, which is neither read nor edited here.
     */
    public boolean isFlowStyle() {
        return false;
    }

    abstract NodeCatalogs reread(SourceFile edited);

    private static boolean isVersionSpec(String alias, String value) {
        Spec spec = classify(alias, value);
        return spec != null && spec.isVersionSpec();
    }

    /**
     * pnpm's catalogs from pnpm-workspace.yaml, keyed as pnpm keys them ({@code default} for {@code catalog} and
     * {@code catalogs.default}), with the settings {@code pnpm add} and {@code pnpm remove} read beside them.
     */
    public static final class Pnpm extends YamlCatalogs {

        public enum CatalogMode {
            MANUAL, PREFER, STRICT
        }

        private static final String DEFAULT = "default";

        Pnpm(Yaml.Documents document) {
            super(document, read(yamlRoot(document)));
        }

        private static Map<String, Map<String, String>> read(Yaml.@Nullable Mapping root) {
            Map<String, Map<String, String>> catalogs = new LinkedHashMap<>();
            Yaml.Mapping catalog = root == null ? null : mapping(root, "catalog");
            Yaml.Mapping named = root == null ? null : mapping(root, "catalogs");
            if (catalog != null) {
                catalogs.put(DEFAULT, scalars(catalog));
            }
            if (named != null) {
                for (Yaml.Mapping.Entry group : named.getEntries()) {
                    Yaml.Mapping entries = asMapping(group.getValue());
                    if (entries != null && !(catalog != null && DEFAULT.equals(group.getKey().getValue()))) {
                        catalogs.put(group.getKey().getValue(), scalars(entries));
                    }
                }
            }
            return catalogs;
        }

        @Override
        public @Nullable String catalogOf(String value) {
            if (!value.startsWith(PROTOCOL)) {
                return null;
            }
            String name = EcmaScript.trim(value.substring(PROTOCOL.length()));
            return name.isEmpty() ? DEFAULT : name;
        }

        /**
         * {@code catalogMode}: manual when unset or {@code manual}, strict when {@code strict}; any other value lets
         * pnpm add a version outside the catalog, as prefer does.
         */
        public CatalogMode getCatalogMode() {
            Yaml.Block mode = setting("catalogMode");
            if (mode == null || mode instanceof Yaml.Scalar && "manual".equals(value((Yaml.Scalar) mode))) {
                return CatalogMode.MANUAL;
            }
            return mode instanceof Yaml.Scalar && "strict".equals(value((Yaml.Scalar) mode)) ? CatalogMode.STRICT :
                    CatalogMode.PREFER;
        }

        /**
         * Whether {@code cleanupUnusedCatalogs} is a value JavaScript holds true.
         */
        public boolean isCleanupUnusedCatalogs() {
            Yaml.Block cleanup = setting("cleanupUnusedCatalogs");
            if (cleanup == null) {
                return false;
            }
            return !(cleanup instanceof Yaml.Scalar) || EcmaScript.truthy(value((Yaml.Scalar) cleanup));
        }

        /**
         * {@code saveCatalogName} as pnpm reads it into a string; null when unset or null.
         */
        public @Nullable String getSaveCatalogName() {
            Yaml.Block name = setting("saveCatalogName");
            Object value = name instanceof Yaml.Scalar ? value((Yaml.Scalar) name) : null;
            return value == null ? null : EcmaScript.string(value);
        }

        /**
         * pnpm's {@code removePackagesFromWorkspaceCatalog}: without the entries no project's dependencies nor a
         * {@code catalog:} override reference, and without a catalog left empty; null when that is no edit in place.
         */
        public @Nullable NodeCatalogs withoutUnused(Collection<Map<String, Map<String, String>>> projects) {
            Yaml.Documents docs = (Yaml.Documents) getDocument();
            Yaml.Mapping root = yamlRoot(docs);
            if (projects.isEmpty() || root == null) {
                return this;
            }
            if (isFlowStyle()) {
                return null;
            }
            if (mapping(root, "catalog") == null && mapping(root, "catalogs") == null) {
                return this;
            }
            Map<String, Set<String>> references = new HashMap<>();
            for (Map<String, Map<String, String>> project : projects) {
                for (Map<String, String> scope : project.values()) {
                    for (Map.Entry<String, String> dependency : scope.entrySet()) {
                        references.computeIfAbsent(dependency.getKey(), k -> new HashSet<>())
                                .add(dependency.getValue());
                    }
                }
            }
            Yaml.Mapping overrides = mapping(root, "overrides");
            if (overrides != null) {
                for (Map.Entry<String, String> override : scalars(overrides).entrySet()) {
                    if (override.getValue().startsWith(PROTOCOL)) {
                        references.computeIfAbsent(overrideTarget(override.getKey()), k -> new HashSet<>())
                                .add(override.getValue());
                    }
                }
            }
            Yaml.Mapping cleaned = root;
            Yaml.Mapping catalog = mapping(root, "catalog");
            if (catalog != null) {
                Yaml.Mapping kept = retain(catalog, references, null);
                cleaned = kept.getEntries().isEmpty() ? without(cleaned, "catalog") : with(cleaned, "catalog", kept);
            }
            Yaml.Mapping named = mapping(cleaned, "catalogs");
            if (named != null) {
                List<Yaml.Mapping.Entry> groups = new ArrayList<>();
                boolean changed = false;
                for (Yaml.Mapping.Entry group : named.getEntries()) {
                    Yaml.Mapping entries = asMapping(group.getValue());
                    if (entries == null) {
                        groups.add(group);
                        continue;
                    }
                    Yaml.Mapping kept = retain(entries, references, group.getKey().getValue());
                    changed |= kept != entries || kept.getEntries().isEmpty();
                    if (!kept.getEntries().isEmpty()) {
                        groups.add(kept == entries ? group : group.withValue(kept));
                    }
                }
                if (groups.isEmpty()) {
                    cleaned = without(cleaned, "catalogs");
                } else if (changed) {
                    cleaned = with(cleaned, "catalogs", named.withEntries(firstPrefix(named.getEntries(), groups)));
                }
            }
            return cleaned == root ? this : reread(withRoot(docs, cleaned));
        }

        @Override
        NodeCatalogs reread(SourceFile edited) {
            return new Pnpm((Yaml.Documents) edited);
        }

        // pnpm writes the default catalog where it is, `catalog` when it is neither place
        @Override
        List<String> yamlPath(String catalog) {
            if (!DEFAULT.equals(catalog)) {
                return Arrays.asList("catalogs", catalog);
            }
            Yaml.Mapping root = yamlRoot((Yaml.Documents) getDocument());
            if (root != null && mapping(root, "catalog") == null) {
                Yaml.Mapping named = mapping(root, "catalogs");
                if (named != null && mapping(named, DEFAULT) != null) {
                    return Arrays.asList("catalogs", DEFAULT);
                }
            }
            return singletonList("catalog");
        }

        private Yaml.@Nullable Block setting(String key) {
            Yaml.Mapping root = yamlRoot((Yaml.Documents) getDocument());
            if (root != null) {
                for (Yaml.Mapping.Entry entry : root.getEntries()) {
                    if (key.equals(entry.getKey().getValue())) {
                        return entry.getValue();
                    }
                }
            }
            return null;
        }

        // the entries of `catalog` (name null) or `catalogs.<name>` a reference keeps
        private static Yaml.Mapping retain(Yaml.Mapping catalog, Map<String, Set<String>> references,
                                           @Nullable String name) {
            List<Yaml.Mapping.Entry> kept = new ArrayList<>();
            for (Yaml.Mapping.Entry entry : catalog.getEntries()) {
                Set<String> referenced = references.getOrDefault(entry.getKey().getValue(), Collections.emptySet());
                if (referenced.contains(PROTOCOL) || name != null && referenced.contains(PROTOCOL + name)) {
                    kept.add(entry);
                }
            }
            return kept.size() == catalog.getEntries().size() ? catalog :
                    catalog.withEntries(firstPrefix(catalog.getEntries(), kept));
        }

        // parsePkgAndParentSelector's target package of an override key: `a>b@1` -> b
        private static String overrideTarget(String selector) {
            String target = selector.substring(selector.lastIndexOf('>') + 1);
            int at = target.indexOf('@', target.startsWith("@") ? 1 : 0);
            return at < 0 ? target : target.substring(0, at);
        }
    }

    /**
     * Yarn's catalogs from .yarnrc.yml: {@code catalog} for {@code catalog:}, {@code catalogs.<name>} otherwise.
     */
    public static final class YarnBerry extends YamlCatalogs {

        YarnBerry(Yaml.Documents document) {
            super(document, read(yamlRoot(document)));
        }

        private static Map<String, Map<String, String>> read(Yaml.@Nullable Mapping root) {
            Map<String, Map<String, String>> catalogs = new LinkedHashMap<>();
            Yaml.Mapping catalog = root == null ? null : mapping(root, "catalog");
            Yaml.Mapping named = root == null ? null : mapping(root, "catalogs");
            if (catalog != null) {
                catalogs.put("", scalars(catalog));
            }
            if (named != null) {
                for (Yaml.Mapping.Entry group : named.getEntries()) {
                    Yaml.Mapping entries = asMapping(group.getValue());
                    if (entries != null && !group.getKey().getValue().isEmpty()) {
                        catalogs.put(group.getKey().getValue(), scalars(entries));
                    }
                }
            }
            return catalogs;
        }

        @Override
        public @Nullable String catalogOf(String value) {
            return value.startsWith(PROTOCOL) ? value.substring(PROTOCOL.length()) : null;
        }

        @Override
        NodeCatalogs reread(SourceFile edited) {
            return new YarnBerry((Yaml.Documents) edited);
        }

        @Override
        List<String> yamlPath(String catalog) {
            return catalog.isEmpty() ? singletonList("catalog") : Arrays.asList("catalogs", catalog);
        }
    }

    /**
     * Bun's catalogs from the root package.json: {@code workspaces.catalog(s)} when the workspaces object holds one,
     * the top-level {@code catalog(s)} otherwise.
     */
    public static final class Bun extends NodeCatalogs {

        Bun(Json.Document document) {
            super(document, read(document));
        }

        private static Map<String, Map<String, String>> read(Json.Document document) {
            Map<String, Map<String, String>> catalogs = new LinkedHashMap<>();
            Json.JsonObject base = base(document);
            if (base == null) {
                return catalogs;
            }
            Json catalog = member(base, "catalog");
            if (catalog instanceof Json.JsonObject) {
                catalogs.put("", strings((Json.JsonObject) catalog));
            }
            Json named = member(base, "catalogs");
            if (named instanceof Json.JsonObject) {
                for (Json m : ((Json.JsonObject) named).getMembers()) {
                    if (m instanceof Json.Member && ((Json.Member) m).getValue() instanceof Json.JsonObject) {
                        String name = string(((Json.Member) m).getKey());
                        if (name != null && !name.isEmpty()) {
                            catalogs.put(name, strings((Json.JsonObject) ((Json.Member) m).getValue()));
                        }
                    }
                }
            }
            return catalogs;
        }

        @Override
        public @Nullable String catalogOf(String value) {
            return value.startsWith(PROTOCOL) ? value.substring(PROTOCOL.length()).trim() : null;
        }

        @Override
        public @Nullable NodeCatalogs withSpecifier(String catalog, String alias, String specifier) {
            String current = specifier(catalog, alias);
            if (current == null || !isVersionSpec(alias, current)) {
                return null;
            }
            return current.equals(specifier) ? this : withEntry(catalog, alias, specifier);
        }

        @Override
        public NodeCatalogs withEntry(String catalog, String alias, String specifier) {
            if (specifier.equals(specifier(catalog, alias))) {
                return this;
            }
            Json.Document document = (Json.Document) getDocument();
            return reread(PackageJsonHelper.withStringMember(document, path(document, catalog), alias, specifier));
        }

        @Override
        NodeCatalogs reread(SourceFile edited) {
            return new Bun((Json.Document) edited);
        }

        // Bun's Package.parse: the workspaces object when it declares a catalog, else the root when it has workspaces
        static Json.@Nullable JsonObject base(Json.Document document) {
            if (!(document.getValue() instanceof Json.JsonObject)) {
                return null;
            }
            Json.JsonObject root = (Json.JsonObject) document.getValue();
            Json workspaces = member(root, "workspaces");
            if (workspaces == null) {
                return null;
            }
            if (workspaces instanceof Json.JsonObject && (member((Json.JsonObject) workspaces, "catalog") != null ||
                                                          member((Json.JsonObject) workspaces, "catalogs") != null)) {
                return (Json.JsonObject) workspaces;
            }
            return root;
        }

        private static List<String> path(Json.Document document, String catalog) {
            List<String> path = new ArrayList<>();
            if (base(document) != document.getValue()) {
                path.add("workspaces");
            }
            if (catalog.isEmpty()) {
                path.add("catalog");
            } else {
                path.add("catalogs");
                path.add(catalog);
            }
            return path;
        }

        private static Map<String, String> strings(Json.JsonObject object) {
            Map<String, String> out = new LinkedHashMap<>();
            for (Json m : object.getMembers()) {
                if (m instanceof Json.Member) {
                    String key = string(((Json.Member) m).getKey());
                    String value = string(((Json.Member) m).getValue());
                    if (key != null && value != null) {
                        out.put(key, value);
                    }
                }
            }
            return out;
        }
    }

    /**
     * Catalogs in a YAML file, edited one scalar or one key at a time.
     */
    abstract static class YamlCatalogs extends NodeCatalogs {

        private final boolean flowStyle;

        YamlCatalogs(Yaml.Documents document, Map<String, Map<String, String>> catalogs) {
            super(document, catalogs);
            this.flowStyle = flowStyle(yamlRoot(document));
        }

        /**
         * The keys leading from the document's root to the catalog's mapping.
         */
        abstract List<String> yamlPath(String catalog);

        @Override
        public boolean isFlowStyle() {
            return flowStyle;
        }

        @Override
        public @Nullable NodeCatalogs withSpecifier(String catalog, String alias, String specifier) {
            String current = specifier(catalog, alias);
            if (flowStyle || current == null || !isVersionSpec(alias, current)) {
                return null;
            }
            return current.equals(specifier) ? this : replaced(catalog, alias, specifier);
        }

        @Override
        public @Nullable NodeCatalogs withEntry(String catalog, String alias, String specifier) {
            String current = specifier(catalog, alias);
            if (flowStyle) {
                return null;
            }
            if (specifier.equals(current)) {
                return this;
            }
            if (current != null) {
                return replaced(catalog, alias, specifier);
            }
            Yaml.Documents docs = (Yaml.Documents) getDocument();
            Yaml.Mapping root = yamlRoot(docs);
            if (root == null && (docs.getDocuments().isEmpty() ||
                                 !(docs.getDocuments().get(0).getBlock() instanceof Yaml.Scalar) ||
                                 !((Yaml.Scalar) docs.getDocuments().get(0).getBlock()).getValue().isEmpty())) {
                return null;
            }
            Yaml.Mapping base = root == null ?
                    new Yaml.Mapping(randomId(), Markers.EMPTY, null, emptyList(), null, null, null) : root;
            Yaml.Mapping edited = put(base, yamlPath(catalog), 0, alias, specifier, indentUnit(base));
            return edited == null ? null : reread(withRoot(docs, edited));
        }

        private @Nullable NodeCatalogs replaced(String catalog, String alias, String specifier) {
            Yaml.Documents docs = (Yaml.Documents) getDocument();
            Yaml.Scalar scalar = entryScalar(yamlRoot(docs), yamlPath(catalog), alias);
            if (scalar == null || scalar.getStyle() == Yaml.Scalar.Style.LITERAL ||
                scalar.getStyle() == Yaml.Scalar.Style.FOLDED) {
                return null;
            }
            Yaml.Scalar rewritten = scalar.withValue(specifier);
            if (scalar.getStyle() == Yaml.Scalar.Style.PLAIN && !canBePlainScalar(specifier)) {
                rewritten = rewritten.withStyle(Yaml.Scalar.Style.SINGLE_QUOTED);
            }
            return reread(replaceScalar(docs, scalar, rewritten));
        }

        // the YAML parser reads a flow mapping as one scalar
        private static boolean flowStyle(Yaml.@Nullable Mapping root) {
            List<Yaml.Block> values = new ArrayList<>();
            for (Yaml.Mapping.Entry entry : root == null ? Collections.<Yaml.Mapping.Entry>emptyList() :
                    root.getEntries()) {
                if ("catalog".equals(entry.getKey().getValue()) || "catalogs".equals(entry.getKey().getValue())) {
                    values.add(entry.getValue());
                    if (entry.getValue() instanceof Yaml.Mapping && "catalogs".equals(entry.getKey().getValue())) {
                        for (Yaml.Mapping.Entry group : ((Yaml.Mapping) entry.getValue()).getEntries()) {
                            values.add(group.getValue());
                        }
                    }
                }
            }
            for (Yaml.Block value : values) {
                if (value instanceof Yaml.Mapping && ((Yaml.Mapping) value).getOpeningBracePrefix() != null &&
                    !((Yaml.Mapping) value).getEntries().isEmpty() ||
                    value instanceof Yaml.Scalar && ((Yaml.Scalar) value).getStyle() == Yaml.Scalar.Style.PLAIN &&
                    ((Yaml.Scalar) value).getValue().startsWith("{") && !emptyFlow(value)) {
                    return true;
                }
            }
            return false;
        }
    }

    static Yaml.@Nullable Mapping yamlRoot(Yaml.Documents docs) {
        if (docs.getDocuments().isEmpty() || !(docs.getDocuments().get(0).getBlock() instanceof Yaml.Mapping)) {
            return null;
        }
        return (Yaml.Mapping) docs.getDocuments().get(0).getBlock();
    }

    private static SourceFile withRoot(Yaml.Documents docs, Yaml.Mapping root) {
        if (docs.getDocuments().isEmpty()) {
            return docs;
        }
        List<Yaml.Document> documents = new ArrayList<>(docs.getDocuments());
        documents.set(0, documents.get(0).withBlock(root));
        return docs.withDocuments(documents);
    }

    private static Yaml.@Nullable Mapping mapping(Yaml.Mapping mapping, String key) {
        for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
            if (key.equals(entry.getKey().getValue())) {
                return asMapping(entry.getValue());
            }
        }
        return null;
    }

    // a mapping, or `{}`, which the YAML parser reads as a scalar
    private static Yaml.@Nullable Mapping asMapping(Yaml.@Nullable Block value) {
        if (value instanceof Yaml.Mapping) {
            return (Yaml.Mapping) value;
        }
        return emptyFlow(value) ? new Yaml.Mapping(randomId(), Markers.EMPTY, null, emptyList(), null, null, null) :
                null;
    }

    private static boolean emptyFlow(Yaml.@Nullable Block value) {
        return value instanceof Yaml.Scalar && ((Yaml.Scalar) value).getStyle() == Yaml.Scalar.Style.PLAIN &&
               "{}".equals(((Yaml.Scalar) value).getValue().replaceAll("\\s", ""));
    }

    private static Map<String, String> scalars(Yaml.Mapping mapping) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
            if (entry.getValue() instanceof Yaml.Scalar) {
                out.put(entry.getKey().getValue(), ((Yaml.Scalar) entry.getValue()).getValue());
            }
        }
        return out;
    }

    private static Yaml.@Nullable Scalar entryScalar(Yaml.@Nullable Mapping root, List<String> path, String alias) {
        Yaml.Mapping at = root;
        for (String key : path) {
            at = at == null ? null : mapping(at, key);
        }
        if (at == null) {
            return null;
        }
        for (Yaml.Mapping.Entry entry : at.getEntries()) {
            if (alias.equals(entry.getKey().getValue()) && entry.getValue() instanceof Yaml.Scalar) {
                return (Yaml.Scalar) entry.getValue();
            }
        }
        return null;
    }

    private static SourceFile replaceScalar(Yaml.Documents docs, Yaml.Scalar from, Yaml.Scalar to) {
        return (SourceFile) new YamlIsoVisitor<Integer>() {
            @Override
            public Yaml.Scalar visitScalar(Yaml.Scalar scalar, Integer p) {
                return scalar == from ? to : scalar;
            }
        }.visitNonNull(docs, 0);
    }

    // `mapping` with `alias: specifier` under the keys path[depth..], each created where missing; null where a key on
    // the path holds no block mapping
    private static Yaml.@Nullable Mapping put(Yaml.Mapping mapping, List<String> path, int depth, String alias,
                                              String specifier, String unit) {
        String indent = depth == 0 ? "" : repeat(unit, depth);
        if (depth == path.size()) {
            for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
                if (alias.equals(entry.getKey().getValue())) {
                    return null;
                }
            }
            return insert(mapping, newEntry(indent, alias, scalar(" ", specifier)), false);
        }
        String key = path.get(depth);
        for (int i = 0; i < mapping.getEntries().size(); i++) {
            Yaml.Mapping.Entry entry = mapping.getEntries().get(i);
            if (key.equals(entry.getKey().getValue())) {
                if (!(entry.getValue() instanceof Yaml.Mapping)) {
                    return null;
                }
                Yaml.Mapping child = put((Yaml.Mapping) entry.getValue(), path, depth + 1, alias, specifier, unit);
                if (child == null) {
                    return null;
                }
                List<Yaml.Mapping.Entry> entries = new ArrayList<>(mapping.getEntries());
                entries.set(i, entry.withValue(child));
                return mapping.withEntries(entries);
            }
        }
        Yaml.Mapping child = put(new Yaml.Mapping(randomId(), Markers.EMPTY, null, emptyList(), null, null, null),
                path, depth + 1, alias, specifier, unit);
        return child == null ? null : insert(mapping, newEntry(indent, key, child), depth == 0);
    }

    // where pnpm's reorderRecursive puts a new key: sorted in when the keys are sorted (packages first at the top
    // level), else last; at the top level propagateBlankLinesToNewPairs decides its blank line
    private static Yaml.Mapping insert(Yaml.Mapping mapping, Yaml.Mapping.Entry entry, boolean topLevel) {
        List<Yaml.Mapping.Entry> entries = new ArrayList<>(mapping.getEntries());
        List<String> keys = new ArrayList<>();
        for (Yaml.Mapping.Entry e : entries) {
            keys.add(e.getKey().getValue());
        }
        String key = entry.getKey().getValue();
        int at = entries.size();
        if (sorted(keys)) {
            at = !keys.isEmpty() && "packages".equals(keys.get(0)) && !"packages".equals(key) ? 1 : 0;
            while (at < keys.size() && keys.get(at).compareTo(key) < 0) {
                at++;
            }
        }
        if (topLevel && entries.isEmpty()) {
            entry = entry.withPrefix("");
        } else if (topLevel) {
            boolean blankStyle = entries.size() > 1;
            for (int i = 1; i < entries.size() && blankStyle; i++) {
                blankStyle = blank(entries.get(i));
            }
            Yaml.Mapping.Entry next = at < entries.size() ? entries.get(at) : null;
            boolean blank = blankStyle || next != null && at > 0 && blank(next) ||
                            next == null && at > 1 && blank(entries.get(at - 1));
            if (at == 0) {
                Yaml.Mapping.Entry first = entries.get(0);
                entry = entry.withPrefix(first.getPrefix());
                entries.set(0, first.withPrefix(blankStyle ? "\n\n" : "\n"));
            } else {
                entry = entry.withPrefix(blank ? "\n\n" : "\n");
            }
        }
        entries.add(at, entry);
        return mapping.withEntries(entries);
    }

    private static boolean blank(Yaml.Mapping.Entry entry) {
        return entry.getPrefix().startsWith("\n\n");
    }

    // pnpm's detectKeyLayout: alphabetical after an optional leading `packages`, by lexCompare (code units)
    private static boolean sorted(List<String> keys) {
        int start = !keys.isEmpty() && "packages".equals(keys.get(0)) ? 1 : 0;
        for (int i = start + 1; i < keys.size(); i++) {
            if (keys.get(i - 1).compareTo(keys.get(i)) > 0) {
                return false;
            }
        }
        return true;
    }

    private static Yaml.Mapping.Entry newEntry(String indent, String key, Yaml.Block value) {
        Yaml.Scalar.Style style = canBePlainScalar(key) ? Yaml.Scalar.Style.PLAIN : Yaml.Scalar.Style.SINGLE_QUOTED;
        YamlKey yamlKey = new Yaml.Scalar(randomId(), "", Markers.EMPTY, style, null, null, key);
        return new Yaml.Mapping.Entry(randomId(), "\n" + indent, Markers.EMPTY, yamlKey, "", value);
    }

    private static Yaml.Scalar scalar(String prefix, String value) {
        return new Yaml.Scalar(randomId(), prefix, Markers.EMPTY, canBePlainScalar(value) ? Yaml.Scalar.Style.PLAIN :
                Yaml.Scalar.Style.SINGLE_QUOTED, null, null, value);
    }

    private static String indentUnit(Yaml.Mapping root) {
        for (Yaml.Mapping.Entry entry : root.getEntries()) {
            if (entry.getValue() instanceof Yaml.Mapping && !((Yaml.Mapping) entry.getValue()).getEntries().isEmpty()) {
                String prefix = ((Yaml.Mapping) entry.getValue()).getEntries().get(0).getPrefix();
                String indent = prefix.substring(prefix.lastIndexOf('\n') + 1);
                if (!indent.isEmpty() && indent.trim().isEmpty()) {
                    return indent;
                }
            }
        }
        return "  ";
    }

    private static String repeat(String unit, int times) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < times; i++) {
            out.append(unit);
        }
        return out.toString();
    }

    // the first remaining entry takes the prefix of the first original one
    private static List<Yaml.Mapping.Entry> firstPrefix(List<Yaml.Mapping.Entry> original,
                                                        List<Yaml.Mapping.Entry> kept) {
        if (kept.isEmpty() || original.isEmpty() || kept.get(0) == original.get(0)) {
            return kept;
        }
        List<Yaml.Mapping.Entry> out = new ArrayList<>(kept);
        out.set(0, kept.get(0).withPrefix(original.get(0).getPrefix()));
        return out;
    }

    private static Yaml.Mapping with(Yaml.Mapping mapping, String key, Yaml.Mapping value) {
        List<Yaml.Mapping.Entry> entries = new ArrayList<>(mapping.getEntries());
        for (int i = 0; i < entries.size(); i++) {
            if (key.equals(entries.get(i).getKey().getValue())) {
                if (entries.get(i).getValue() == value) {
                    return mapping;
                }
                entries.set(i, entries.get(i).withValue(value));
            }
        }
        return mapping.withEntries(entries);
    }

    private static Yaml.Mapping without(Yaml.Mapping mapping, String key) {
        List<Yaml.Mapping.Entry> kept = new ArrayList<>();
        for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
            if (!key.equals(entry.getKey().getValue())) {
                kept.add(entry);
            }
        }
        return mapping.withEntries(firstPrefix(mapping.getEntries(), kept));
    }

    // a plain scalar as js-yaml's core schema reads it (null, boolean, number or string), a quoted one as a string
    private static @Nullable Object value(Yaml.Scalar scalar) {
        String text = scalar.getValue();
        if (scalar.getStyle() != Yaml.Scalar.Style.PLAIN) {
            return text;
        }
        switch (text) {
            case "":
            case "~":
            case "null":
            case "Null":
            case "NULL":
                return null;
            case "true":
            case "True":
            case "TRUE":
                return true;
            case "false":
            case "False":
            case "FALSE":
                return false;
            default:
                Double number = yamlNumber(text);
                return number == null ? text : number;
        }
    }

    private static final Pattern YAML_INT = Pattern.compile(
            "[-+]?(?:0b[01_]*[01][01_]*|0x[0-9a-fA-F_]*[0-9a-fA-F][0-9a-fA-F_]*|0o[0-7_]*[0-7][0-7_]*" +
            "|0|0?[0-9][0-9_]*)");
    private static final Pattern YAML_FLOAT = Pattern.compile(
            "[-+]?[0-9][0-9_]*(?:\\.[0-9_]*)?(?:[eE][-+]?[0-9]+)?|\\.[0-9_]+(?:[eE][-+]?[0-9]+)?" +
            "|[-+]?\\.(?:inf|Inf|INF)|\\.(?:nan|NaN|NAN)");

    private static @Nullable Double yamlNumber(String text) {
        if (text.endsWith("_")) {
            return null;
        }
        String value = text.replace("_", "");
        double sign = value.startsWith("-") ? -1 : 1;
        String digits = value.startsWith("-") || value.startsWith("+") ? value.substring(1) : value;
        if (YAML_INT.matcher(text).matches()) {
            int radix = digits.startsWith("0b") ? 2 : digits.startsWith("0x") ? 16 : digits.startsWith("0o") ? 8 : 10;
            return sign * new BigInteger(radix == 10 ? digits : digits.substring(2), radix).doubleValue();
        }
        if (!YAML_FLOAT.matcher(text).matches()) {
            return null;
        }
        String lower = digits.toLowerCase();
        if (".inf".equals(lower)) {
            return sign * Double.POSITIVE_INFINITY;
        }
        if (".nan".equals(lower)) {
            return Double.NaN;
        }
        return sign * Double.parseDouble(lower.startsWith(".") ? "0" + lower : lower);
    }

    static @Nullable Json member(Json.JsonObject object, String key) {
        for (Json m : object.getMembers()) {
            if (m instanceof Json.Member && key.equals(string(((Json.Member) m).getKey()))) {
                return ((Json.Member) m).getValue();
            }
        }
        return null;
    }

    static @Nullable String string(@Nullable Object node) {
        if (node instanceof Json.Literal && ((Json.Literal) node).getValue() instanceof String) {
            return (String) ((Json.Literal) node).getValue();
        }
        return null;
    }

    /**
     * Whether YAML reads {@code value} written without quotes as this string.
     */
    static boolean canBePlainScalar(String value) {
        if (value.isEmpty() || "---".equals(value) || "...".equals(value)) {
            return false;
        }
        if (PLAIN_SCALAR_INDICATORS.indexOf(value.charAt(0)) >= 0 || !value.equals(value.trim())) {
            return false;
        }
        if (value.contains(": ") || value.contains(" #") || value.endsWith(":") ||
            YAML_NON_STRING.matcher(value).matches()) {
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

    private static String printed(SourceFile document) {
        return document.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED));
    }
}
