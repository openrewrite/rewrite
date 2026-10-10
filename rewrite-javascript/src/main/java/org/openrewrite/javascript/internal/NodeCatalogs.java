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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.internal.lock.EcmaScript;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.internal.lock.berry.Catalogs;
import org.openrewrite.javascript.internal.lock.bun.CatalogMap;
import org.openrewrite.javascript.internal.lock.pnpm.PnpmOptions;
import org.openrewrite.javascript.internal.lock.pnpm.WorkspaceManifest;
import org.openrewrite.javascript.internal.lock.pnpm.WorkspaceManifestWriter;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.nio.file.Paths;
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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.javascript.internal.lock.pnpm.Catalogs.parseCatalogProtocol;
import static org.openrewrite.Tree.randomId;

/**
 * A workspace's catalogs as its package manager reads them (pnpm from pnpm-workspace.yaml, Yarn from .yarnrc.yml, Bun
 * from the root package.json), by catalog then alias; an edit changes the document in place.
 */
@Getter
public abstract class NodeCatalogs {

    private static final String PROTOCOL = "catalog:";

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
        return read(pm, document, null);
    }

    /**
     * As {@link #read(PackageManager, SourceFile)}, pnpm's placed by the writer of the release {@code rootManifest}
     * names (the ported one when it names none).
     */
    public static @Nullable NodeCatalogs read(@Nullable PackageManager pm, SourceFile document,
                                              @Nullable String rootManifest) {
        if (pm == PackageManager.Pnpm && document instanceof Yaml.Documents) {
            return new Pnpm((Yaml.Documents) document, rootManifest);
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
        return PackageJsonHelper.printed(document);
    }

    /**
     * Whether a catalog is written in YAML's flow style, which is neither read nor edited here.
     */
    public boolean isFlowStyle() {
        return false;
    }

    abstract NodeCatalogs reread(SourceFile edited);

    private static boolean isVersionSpec(String alias, String value) {
        Spec spec = Spec.ofDependencyOrNull(alias, value);
        return spec != null && spec.isVersionSpec();
    }

    /**
     * pnpm's catalogs from pnpm-workspace.yaml, keyed as pnpm keys them ({@code default} for {@code catalog} and
     * {@code catalogs.default}), with the settings {@code pnpm add} and {@code pnpm remove} read beside them.
     */
    public static final class Pnpm extends YamlCatalogs {

        private static final String DEFAULT = "default";

        private final @Nullable String rootManifest;
        private @Nullable Map<String, @Nullable Object> settings;
        private boolean settingsRead;
        private @Nullable RuntimeException rejection;

        Pnpm(Yaml.Documents document, @Nullable String rootManifest) {
            super(document, read(yamlRoot(document)));
            this.rootManifest = rootManifest;
        }

        /**
         * pnpm-workspace.yaml as {@code text} (none when null), for the release {@code rootManifest} names.
         */
        static Pnpm parse(@Nullable String text, String rootManifest) {
            SourceFile parsed = YamlParser.builder().build().parse(new InMemoryExecutionContext(),
                    text == null ? "" : text).findFirst().orElseThrow(IllegalStateException::new);
            if (!(parsed instanceof Yaml.Documents)) {
                throw new IllegalArgumentException("pnpm-workspace.yaml is not YAML pnpm reads");
            }
            return new Pnpm((Yaml.Documents) parsed.withSourcePath(Paths.get("pnpm-workspace.yaml")), rootManifest);
        }

        /**
         * pnpm-workspace.yaml as pnpm's workspace-manifest-reader reads it, once; null when empty or
         * {@link #isRejected}.
         */
        public @Nullable Map<String, @Nullable Object> getSettings() {
            if (!settingsRead) {
                settingsRead = true;
                try {
                    settings = WorkspaceManifest.readWorkspaceManifest(print());
                } catch (RuntimeException e) {
                    rejection = e;
                }
            }
            return settings;
        }

        /**
         * Whether pnpm rejects the file as no workspace manifest (INVALID_WORKSPACE_CONFIGURATION, not YAML).
         */
        public boolean isRejected() {
            getSettings();
            return rejection != null;
        }

        // the release's writer; one pnpm does not write with is the ported release's
        @Override
        WorkspaceManifestWriter writer() {
            int release = PnpmOptions.releaseMajor(rootManifest == null ? "{}" : rootManifest, getSettings());
            return new WorkspaceManifestWriter(release >= 9 && release <= 11 ? release : 11);
        }

        /**
         * The file with the override {@code key: value} where pnpm's writer puts it, an existing value replaced in
         * place; this one when it holds that value. An {@link IllegalArgumentException} names why not in place.
         */
        public NodeCatalogs withOverride(WorkspaceManifestWriter writer, String key, String value) {
            if (rejection != null || isRejected()) {
                throw new IllegalArgumentException("pnpm-workspace.yaml is not YAML pnpm reads: " +
                                                   requireNonNull(rejection).getMessage(), rejection);
            }
            Yaml.Documents docs = (Yaml.Documents) getDocument();
            if (print().indexOf('\r') >= 0) {
                throw new IllegalArgumentException("pnpm-workspace.yaml has CR line ends");
            }
            if (docs.getDocuments().size() > 1 || !docs.getDocuments().isEmpty() &&
                                                  (docs.getDocuments().get(0).isExplicit() ||
                                                   docs.getDocuments().get(0).getEnd().isExplicit())) {
                throw new IllegalArgumentException("pnpm-workspace.yaml has document markers");
            }
            Yaml.Mapping root = yamlRoot(docs);
            Map<String, @Nullable Object> read = getSettings();
            if (root == null ? read != null && !read.isEmpty() : !keys(root).equals(keysOf(read))) {
                throw new IllegalArgumentException("pnpm-workspace.yaml is not a block mapping of plain keys");
            }
            Yaml.Mapping overrides = root == null ? null : blockMapping(root, "overrides");
            for (Yaml.Mapping.Entry entry : root == null ? Collections.<Yaml.Mapping.Entry>emptyList() :
                    root.getEntries()) {
                requireEditable(entry);
                if ("overrides".equals(entry.getKey().getValue()) &&
                    (overrides == null && !emptyBlock(entry.getValue()) ||
                     overrides != null && !keys(overrides).equals(keysOf(read == null ? null :
                             read.get("overrides"))))) {
                    throw new IllegalArgumentException("pnpm-workspace.yaml's overrides is not a block mapping");
                }
            }
            if (overrides != null) {
                for (Yaml.Mapping.Entry entry : overrides.getEntries()) {
                    requireEditable(entry);
                    if (key.equals(entry.getKey().getValue()) && !(entry.getValue() instanceof Yaml.Scalar)) {
                        throw new IllegalArgumentException("pnpm-workspace.yaml's override of " + key +
                                                           " is not a scalar");
                    }
                }
            }
            Yaml.Scalar existing = entryScalar(root, singletonList("overrides"), key);
            if (existing != null) {
                if (value.equals(unescaped(existing))) {
                    return this;
                }
                return reread(replaceScalar(docs, existing, restyled(existing, value)));
            }
            Yaml.Mapping base = root == null ?
                    new Yaml.Mapping(randomId(), Markers.EMPTY, null, emptyList(), null, null, null) : root;
            if (root != null && overrides == null && mapping(root, "overrides") == null && has(root, "overrides")) {
                Yaml.Mapping child = insert(new Yaml.Mapping(randomId(), Markers.EMPTY, null, emptyList(), null, null,
                        null), newEntry(indentUnit(root), key, scalar(" ", value)), false, writer);
                return reread(withRoot(docs, withValue(root, "overrides", child)));
            }
            Yaml.Mapping edited = put(base, singletonList("overrides"), 0, key, value, indentUnit(base), writer);
            if (edited == null) {
                throw new IllegalArgumentException("pnpm-workspace.yaml's overrides is not a block mapping");
            }
            return reread(withRoot(docs, edited));
        }

        // a value written inline is a plain or quoted scalar, or a block on the lines below
        private static void requireEditable(Yaml.Mapping.Entry entry) {
            Yaml.Block value = entry.getValue();
            boolean inline = value instanceof Yaml.Scalar && (((Yaml.Scalar) value).getStyle() ==
                                                              Yaml.Scalar.Style.LITERAL || ((Yaml.Scalar) value)
                                                                      .getStyle() == Yaml.Scalar.Style.FOLDED ||
                                                              ((Yaml.Scalar) value).getAnchor() != null ||
                                                              ((Yaml.Scalar) value).getTag() != null ||
                                                              ((Yaml.Scalar) value).getStyle() ==
                                                              Yaml.Scalar.Style.PLAIN &&
                                                              ((Yaml.Scalar) value).getValue().startsWith("{"));
            if (inline || value instanceof Yaml.Alias ||
                value instanceof Yaml.Mapping && (((Yaml.Mapping) value).getOpeningBracePrefix() != null ||
                                                  ((Yaml.Mapping) value).getAnchor() != null ||
                                                  ((Yaml.Mapping) value).getTag() != null) ||
                value instanceof Yaml.Sequence && (((Yaml.Sequence) value).getOpeningBracketPrefix() != null ||
                                                   ((Yaml.Sequence) value).getAnchor() != null ||
                                                   ((Yaml.Sequence) value).getTag() != null)) {
                throw new IllegalArgumentException("pnpm-workspace.yaml has a value it does not edit: " +
                                                   entry.getKey().getValue());
            }
        }

        // a key without a value, which pnpm reads as null
        private static boolean emptyBlock(Yaml.Block value) {
            return value instanceof Yaml.Scalar && ((Yaml.Scalar) value).getStyle() == Yaml.Scalar.Style.PLAIN &&
                   ((Yaml.Scalar) value).getValue().isEmpty();
        }

        private static boolean has(Yaml.Mapping mapping, String key) {
            for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
                if (key.equals(entry.getKey().getValue())) {
                    return true;
                }
            }
            return false;
        }

        private static Yaml.Mapping withValue(Yaml.Mapping mapping, String key, Yaml.Block value) {
            List<Yaml.Mapping.Entry> entries = new ArrayList<>(mapping.getEntries());
            for (int i = 0; i < entries.size(); i++) {
                if (key.equals(entries.get(i).getKey().getValue())) {
                    entries.set(i, entries.get(i).withValue(value));
                }
            }
            return mapping.withEntries(entries);
        }

        private static Yaml.@Nullable Mapping blockMapping(Yaml.Mapping mapping, String key) {
            for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
                if (key.equals(entry.getKey().getValue()) && entry.getValue() instanceof Yaml.Mapping &&
                    ((Yaml.Mapping) entry.getValue()).getOpeningBracePrefix() == null) {
                    return (Yaml.Mapping) entry.getValue();
                }
            }
            return null;
        }

        private static List<String> keys(Yaml.Mapping mapping) {
            List<String> keys = new ArrayList<>();
            for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
                if (!(entry.getKey() instanceof Yaml.Scalar) ||
                    ((Yaml.Scalar) entry.getKey()).getStyle() == Yaml.Scalar.Style.LITERAL ||
                    ((Yaml.Scalar) entry.getKey()).getStyle() == Yaml.Scalar.Style.FOLDED) {
                    throw new IllegalArgumentException("pnpm-workspace.yaml has a key it cannot read: " +
                                                       entry.getKey().getValue());
                }
                keys.add(unescaped((Yaml.Scalar) entry.getKey()));
            }
            return keys;
        }

        private static List<String> keysOf(@Nullable Object loaded) {
            if (loaded == null) {
                return emptyList();
            }
            if (!(loaded instanceof Map)) {
                throw new IllegalArgumentException("pnpm-workspace.yaml is not a mapping");
            }
            List<String> keys = new ArrayList<>();
            for (Object key : ((Map<?, ?>) loaded).keySet()) {
                keys.add(String.valueOf(key));
            }
            return keys;
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
            return parseCatalogProtocol(value);
        }

        /**
         * Whether {@code cleanupUnusedCatalogs} is a value JavaScript holds true.
         */
        public boolean isCleanupUnusedCatalogs() {
            Map<String, @Nullable Object> read = getSettings();
            return read != null && EcmaScript.truthy(read.get("cleanupUnusedCatalogs"));
        }

        /**
         * The file without what the port's {@link WorkspaceManifestWriter#cleanupUnusedCatalogs} removes over the
         * {@code projects} by directory (null where the recipes do not read one); null when not in place.
         */
        @SuppressWarnings("unchecked")
        public @Nullable NodeCatalogs withoutUnused(Map<String, @Nullable Map<String, @Nullable Object>> projects) {
            Map<String, @Nullable Object> read = getSettings();
            Yaml.Documents docs = (Yaml.Documents) getDocument();
            Yaml.Mapping root = yamlRoot(docs);
            if (read == null || root == null) {
                return this;
            }
            Map<String, @Nullable Object> cleaned = new LinkedHashMap<>(read);
            for (String key : Arrays.asList("catalog", "catalogs")) {
                if (cleaned.get(key) instanceof Map) {
                    Map<String, @Nullable Object> copy =
                            new LinkedHashMap<>((Map<String, @Nullable Object>) cleaned.get(key));
                    if ("catalogs".equals(key)) {
                        copy.replaceAll((name, group) -> group instanceof Map ?
                                new LinkedHashMap<>((Map<String, @Nullable Object>) group) : group);
                    }
                    cleaned.put(key, copy);
                }
            }
            if (!WorkspaceManifestWriter.cleanupUnusedCatalogs(cleaned, projects)) {
                return this;
            }
            if (isFlowStyle()) {
                return null;
            }
            Yaml.Mapping edited = root;
            Yaml.Mapping catalog = mapping(root, "catalog");
            if (catalog != null) {
                Object kept = cleaned.get("catalog");
                edited = kept instanceof Map ? with(edited, "catalog", retain(catalog, ((Map<?, ?>) kept).keySet())) :
                        without(edited, "catalog");
            }
            Yaml.Mapping named = mapping(edited, "catalogs");
            if (named != null) {
                Object kept = cleaned.get("catalogs");
                if (!(kept instanceof Map)) {
                    edited = without(edited, "catalogs");
                } else {
                    List<Yaml.Mapping.Entry> groups = new ArrayList<>();
                    for (Yaml.Mapping.Entry group : named.getEntries()) {
                        Yaml.Mapping entries = asMapping(group.getValue());
                        Object keptGroup = ((Map<?, ?>) kept).get(group.getKey().getValue());
                        if (entries == null) {
                            groups.add(group);
                        } else if (keptGroup instanceof Map) {
                            Yaml.Mapping retained = retain(entries, ((Map<?, ?>) keptGroup).keySet());
                            groups.add(retained == entries ? group : group.withValue(retained));
                        }
                    }
                    edited = with(edited, "catalogs", named.withEntries(firstPrefix(named.getEntries(), groups)));
                }
            }
            return edited == root ? this : reread(withRoot(docs, edited));
        }

        // the entries of a catalog's mapping whose keys pnpm keeps
        private static Yaml.Mapping retain(Yaml.Mapping catalog, Set<?> keep) {
            List<Yaml.Mapping.Entry> kept = new ArrayList<>();
            for (Yaml.Mapping.Entry entry : catalog.getEntries()) {
                if (keep.contains(entry.getKey().getValue())) {
                    kept.add(entry);
                }
            }
            return kept.size() == catalog.getEntries().size() ? catalog :
                    catalog.withEntries(firstPrefix(catalog.getEntries(), kept));
        }

        @Override
        NodeCatalogs reread(SourceFile edited) {
            return new Pnpm((Yaml.Documents) edited, rootManifest);
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
            return Catalogs.catalogOf(value);
        }

        @Override
        NodeCatalogs reread(SourceFile edited) {
            return new YarnBerry((Yaml.Documents) edited);
        }

        // .yarnrc.yml takes a new key where pnpm 11's writer would
        @Override
        WorkspaceManifestWriter writer() {
            return new WorkspaceManifestWriter(11);
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
            return CatalogMap.groupOf(value);
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

        /**
         * Where a new key goes and its blank line.
         */
        abstract WorkspaceManifestWriter writer();

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
            Yaml.Mapping edited;
            try {
                edited = put(base, yamlPath(catalog), 0, alias, specifier, indentUnit(base), writer());
            } catch (IllegalArgumentException e) {
                return null;
            }
            return edited == null ? null : reread(withRoot(docs, edited));
        }

        private @Nullable NodeCatalogs replaced(String catalog, String alias, String specifier) {
            Yaml.Documents docs = (Yaml.Documents) getDocument();
            Yaml.Scalar scalar = entryScalar(yamlRoot(docs), yamlPath(catalog), alias);
            if (scalar == null || scalar.getStyle() == Yaml.Scalar.Style.LITERAL ||
                scalar.getStyle() == Yaml.Scalar.Style.FOLDED) {
                return null;
            }
            Yaml.Scalar rewritten;
            try {
                rewritten = restyled(scalar, specifier);
            } catch (IllegalArgumentException e) {
                return null;
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

    // the document with a new root; a top-level key added first to a document without one, or last before a trailing
    // comment, is laid out as pnpm's writer leaves the text around it: a blank line between, and a final newline
    private static SourceFile withRoot(Yaml.Documents docs, Yaml.Mapping root) {
        if (docs.getDocuments().isEmpty()) {
            return docs;
        }
        Yaml.Document document = docs.getDocuments().get(0);
        Yaml.Document.End end = document.getEnd();
        Yaml.Mapping placed = root;
        boolean held = document.getBlock() instanceof Yaml.Mapping &&
                       !((Yaml.Mapping) document.getBlock()).getEntries().isEmpty();
        if (!held && !root.getEntries().isEmpty()) {
            if (!document.getPrefix().trim().isEmpty() && !document.getPrefix().endsWith("\n\n")) {
                List<Yaml.Mapping.Entry> entries = new ArrayList<>(root.getEntries());
                entries.set(0, entries.get(0).withPrefix("\n" + entries.get(0).getPrefix()));
                placed = root.withEntries(entries);
            }
            if (!end.getPrefix().endsWith("\n")) {
                end = end.withPrefix(end.getPrefix() + "\n");
            }
        } else if (held && !root.getEntries().isEmpty()) {
            String last = root.getEntries().get(root.getEntries().size() - 1).getKey().getValue();
            boolean added = true;
            for (Yaml.Mapping.Entry entry : ((Yaml.Mapping) document.getBlock()).getEntries()) {
                added &= !last.equals(entry.getKey().getValue());
            }
            if (added && !end.getPrefix().trim().isEmpty() && !end.getPrefix().startsWith("\n\n")) {
                end = end.withPrefix("\n" + end.getPrefix());
            }
        }
        List<Yaml.Document> documents = new ArrayList<>(docs.getDocuments());
        documents.set(0, document.withBlock(placed).withEnd(end));
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

    // a scalar given a new value keeps its quotes, escaped; a plain one is written as pnpm's yaml writes it
    private static Yaml.Scalar restyled(Yaml.Scalar scalar, String value) {
        switch (scalar.getStyle()) {
            case SINGLE_QUOTED:
                WorkspaceManifestWriter.scalar(value);
                return scalar.withValue(value.replace("'", "''"));
            case DOUBLE_QUOTED:
                WorkspaceManifestWriter.scalar(value);
                String quoted = WorkspaceManifestWriter.doubleQuoted(value);
                return scalar.withValue(quoted.substring(1, quoted.length() - 1));
            default:
                Yaml.Scalar written = scalar("", value);
                return scalar.withStyle(written.getStyle()).withValue(written.getValue());
        }
    }

    // a scalar's value: a quoted one's text without its escapes
    private static String unescaped(Yaml.Scalar scalar) {
        if (scalar.getStyle() == Yaml.Scalar.Style.SINGLE_QUOTED) {
            return scalar.getValue().replace("''", "'");
        }
        if (scalar.getStyle() == Yaml.Scalar.Style.DOUBLE_QUOTED) {
            return scalar.getValue().replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return scalar.getValue();
    }

    // `mapping` with `alias: specifier` under the keys path[depth..], each created where missing; null where a key on
    // the path holds no block mapping
    private static Yaml.@Nullable Mapping put(Yaml.Mapping mapping, List<String> path, int depth, String alias,
                                              String specifier, String unit, WorkspaceManifestWriter writer) {
        String indent = depth == 0 ? "" : repeat(unit, depth);
        if (depth == path.size()) {
            for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
                if (alias.equals(entry.getKey().getValue())) {
                    return null;
                }
            }
            return insert(mapping, newEntry(indent, alias, scalar(" ", specifier)), false, writer);
        }
        String key = path.get(depth);
        for (int i = 0; i < mapping.getEntries().size(); i++) {
            Yaml.Mapping.Entry entry = mapping.getEntries().get(i);
            if (key.equals(entry.getKey().getValue())) {
                if (!(entry.getValue() instanceof Yaml.Mapping)) {
                    return null;
                }
                Yaml.Mapping child = put((Yaml.Mapping) entry.getValue(), path, depth + 1, alias, specifier, unit,
                        writer);
                if (child == null) {
                    return null;
                }
                List<Yaml.Mapping.Entry> entries = new ArrayList<>(mapping.getEntries());
                entries.set(i, entry.withValue(child));
                return mapping.withEntries(entries);
            }
        }
        Yaml.Mapping child = put(new Yaml.Mapping(randomId(), Markers.EMPTY, null, emptyList(), null, null, null),
                path, depth + 1, alias, specifier, unit, writer);
        return child == null ? null : insert(mapping, newEntry(indent, key, child), depth == 0, writer);
    }

    // where pnpm's writer puts a new key and, at the top level, its blank line
    private static Yaml.Mapping insert(Yaml.Mapping mapping, Yaml.Mapping.Entry entry, boolean topLevel,
                                       WorkspaceManifestWriter writer) {
        List<Yaml.Mapping.Entry> entries = new ArrayList<>(mapping.getEntries());
        List<String> keys = new ArrayList<>();
        List<Boolean> blankBefore = new ArrayList<>();
        for (Yaml.Mapping.Entry e : entries) {
            keys.add(e.getKey().getValue());
            blankBefore.add(e.getPrefix().startsWith("\n\n"));
        }
        int at = writer.insertionIndex(keys, entry.getKey().getValue());
        if (topLevel && entries.isEmpty()) {
            entry = entry.withPrefix("");
        } else if (topLevel && at == 0) {
            Yaml.Mapping.Entry first = entries.get(0);
            entry = entry.withPrefix(first.getPrefix());
            entries.set(0, first.withPrefix(writer.blankLineStyle(blankBefore) ? "\n\n" : "\n"));
        } else if (topLevel) {
            entry = entry.withPrefix(writer.blankBefore(blankBefore, at) ? "\n\n" : "\n");
        }
        entries.add(at, entry);
        return mapping.withEntries(entries);
    }

    private static Yaml.Mapping.Entry newEntry(String indent, String key, Yaml.Block value) {
        Yaml.Scalar yamlKey = scalar("", key);
        return new Yaml.Mapping.Entry(randomId(), "\n" + indent, Markers.EMPTY, yamlKey, "", value);
    }

    // a new scalar as pnpm's yaml writes it, quotes and escapes in the printed text
    private static Yaml.Scalar scalar(String prefix, String value) {
        String written = WorkspaceManifestWriter.scalar(value);
        Yaml.Scalar.Style style = written.startsWith("'") ? Yaml.Scalar.Style.SINGLE_QUOTED :
                written.startsWith("\"") ? Yaml.Scalar.Style.DOUBLE_QUOTED : Yaml.Scalar.Style.PLAIN;
        return new Yaml.Scalar(randomId(), prefix, Markers.EMPTY, style, null, null,
                style == Yaml.Scalar.Style.PLAIN ? written : written.substring(1, written.length() - 1));
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

}
