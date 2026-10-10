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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

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

    /**
     * Why these catalogs are not edited for the package manager release that writes the file; null when they are.
     */
    public @Nullable String declined() {
        return null;
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
        private @Nullable PnpmRelease release;

        Pnpm(Yaml.Documents document, @Nullable String rootManifest) {
            super(document, read(yamlRoot(document)));
            this.rootManifest = rootManifest;
        }

        /**
         * This file for the release {@code rootManifest}, a revision of the root's package.json, names.
         */
        public Pnpm forRoot(String rootManifest) {
            if (rootManifest.equals(this.rootManifest)) {
                return this;
            }
            Pnpm other = new Pnpm((Yaml.Documents) getDocument(), rootManifest);
            other.settings = getSettings();
            other.rejection = rejection;
            other.settingsRead = true;
            return other;
        }

        /**
         * The release that writes this file, the ported one when the root names none.
         */
        public PnpmRelease getRelease() {
            if (release == null) {
                release = PnpmRelease.of(rootManifest == null ? "{}" : rootManifest, getSettings());
            }
            return release;
        }

        @Override
        public @Nullable String declined() {
            return getRelease().declined();
        }

        /**
         * Whether an install by the release the root names cleans unused catalog entries up: the setting is one
         * JavaScript holds true, and the release has it.
         */
        public boolean cleansUp() {
            return isCleanupUnusedCatalogs() && getRelease().cleansUpUnusedCatalogs();
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

        @Override
        @Nullable
        WorkspaceManifestWriter writer() {
            return getRelease().writer();
        }

        // pnpm's writer patches the yaml document it read and prints all of it again
        @Override
        boolean rerenders() {
            return true;
        }

        @Override
        boolean sortsEveryLevel() {
            return getRelease().sortsEveryLevel();
        }

        /**
         * The file with the override {@code key: value} where pnpm's writer puts it, an existing value replaced in
         * place; this one when it holds that value. An {@link IllegalArgumentException} names why not in place.
         */
        public NodeCatalogs withOverride(String key, String value) {
            String declined = declined();
            if (declined != null) {
                throw new IllegalArgumentException(declined);
            }
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
            if (writer() == null) {
                throw new IllegalArgumentException(getRelease() + " never writes pnpm-workspace.yaml");
            }
            Yaml.Scalar existing = entryScalar(root, singletonList("overrides"), key);
            if (existing != null) {
                if (value.equals(unescaped(existing))) {
                    return this;
                }
                return laidOut(Layout.of((Yaml.Documents) replaceScalar(docs, existing, restyled(existing, value))));
            }
            Layout edited = Layout.of(docs).with(singletonList("overrides"), key, scalar(" ", value),
                    requireNonNull(writer()), root == null ? "  " : indentUnit(root));
            if (edited == null) {
                throw new IllegalArgumentException("pnpm-workspace.yaml's overrides is not a block mapping");
            }
            return laidOut(edited);
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
                                                              ((Yaml.Scalar) value).getValue().startsWith("{") &&
                                                              !emptyFlow(value));
            if (inline || value instanceof Yaml.Alias ||
                value instanceof Yaml.Mapping && (((Yaml.Mapping) value).getOpeningBracePrefix() != null &&
                                                  !((Yaml.Mapping) value).getEntries().isEmpty() ||
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
         * {@code projects} by directory (null where the recipes do not read one); null when not in place or declined.
         */
        @SuppressWarnings("unchecked")
        public @Nullable NodeCatalogs withoutUnused(Map<String, @Nullable Map<String, @Nullable Object>> projects) {
            if (declined() != null) {
                return null;
            }
            Map<String, @Nullable Object> read = getSettings();
            Yaml.Documents docs = (Yaml.Documents) getDocument();
            Yaml.Mapping root = yamlRoot(docs);
            if (read == null || root == null || !getRelease().cleansUpUnusedCatalogs()) {
                return this;
            }
            Map<String, @Nullable Object> cleaned = new LinkedHashMap<>(read);
            if (!getRelease().cleanupKeepsOverrideReferences()) {
                cleaned.remove("overrides");
            }
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
            try {
                Layout layout = Layout.of(docs);
                Layout edited = layout;
                if (mapping(root, "catalog") != null) {
                    Object kept = cleaned.get("catalog");
                    edited = kept instanceof Map ? edited.retaining(singletonList("catalog"),
                            ((Map<?, ?>) kept).keySet()) : edited.without(singletonList("catalog"));
                }
                Yaml.Mapping named = mapping(root, "catalogs");
                if (named != null) {
                    Object kept = cleaned.get("catalogs");
                    if (!(kept instanceof Map)) {
                        edited = edited.without(singletonList("catalogs"));
                    } else {
                        for (Yaml.Mapping.Entry group : named.getEntries()) {
                            Object keptGroup = ((Map<?, ?>) kept).get(group.getKey().getValue());
                            List<String> path = Arrays.asList("catalogs", group.getKey().getValue());
                            if (asMapping(group.getValue()) != null) {
                                edited = keptGroup instanceof Map ? edited.retaining(path,
                                        ((Map<?, ?>) keptGroup).keySet()) : edited.without(path);
                            }
                        }
                    }
                }
                return edited == layout ? this : laidOut(edited);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        // the release read once for the root: an edit leaving pmOnFail as it was leaves it
        @Override
        NodeCatalogs reread(SourceFile edited) {
            Pnpm next = new Pnpm((Yaml.Documents) edited, rootManifest);
            if (release != null && Objects.equals(pmOnFail(next.getSettings()), pmOnFail(getSettings()))) {
                next.release = release;
            }
            return next;
        }

        private static @Nullable Object pmOnFail(@Nullable Map<String, @Nullable Object> settings) {
            return settings == null ? null : settings.get("pmOnFail");
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

        // Yarn writes no catalog entry; the recipes place a new one as pnpm 11's writer would
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
         * Where a new key goes and its blank line; null when nothing writes a new key into the file.
         */
        abstract @Nullable WorkspaceManifestWriter writer();

        /**
         * Whether a write prints the whole document again as the yaml library pnpm writes with does.
         */
        boolean rerenders() {
            return false;
        }

        /**
         * Whether the writer sorts every level of the document it writes.
         */
        boolean sortsEveryLevel() {
            return false;
        }

        // the catalogs of a laid out revision as the writer leaves the whole file; an IllegalArgumentException names
        // a layout whose comments are not placed here as the writer places them
        NodeCatalogs laidOut(Layout layout) {
            return reread((sortsEveryLevel() ? layout.sorted(requireNonNull(writer())) : layout).write(rerenders()));
        }

        /**
         * The catalogs of an edited revision, as the writer leaves the whole file; null when it is not laid out here.
         */
        @Nullable
        NodeCatalogs written(SourceFile edited) {
            if (!rerenders()) {
                return reread(edited);
            }
            try {
                return laidOut(Layout.of((Yaml.Documents) edited));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        @Override
        public boolean isFlowStyle() {
            return flowStyle;
        }

        @Override
        public @Nullable NodeCatalogs withSpecifier(String catalog, String alias, String specifier) {
            String current = specifier(catalog, alias);
            if (flowStyle || declined() != null || current == null || !isVersionSpec(alias, current)) {
                return null;
            }
            return current.equals(specifier) ? this : replaced(catalog, alias, specifier);
        }

        @Override
        public @Nullable NodeCatalogs withEntry(String catalog, String alias, String specifier) {
            String current = specifier(catalog, alias);
            if (flowStyle || declined() != null) {
                return null;
            }
            if (specifier.equals(current)) {
                return this;
            }
            if (current != null) {
                return replaced(catalog, alias, specifier);
            }
            WorkspaceManifestWriter writer = writer();
            if (writer == null) {
                return null;
            }
            Yaml.Mapping root = yamlRoot((Yaml.Documents) getDocument());
            try {
                Layout edited = Layout.of((Yaml.Documents) getDocument()).with(yamlPath(catalog), alias,
                        scalar(" ", specifier), writer, root == null ? "  " : indentUnit(root));
                return edited == null ? null : laidOut(edited);
            } catch (IllegalArgumentException e) {
                return null;
            }
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
            return written(replaceScalar(docs, scalar, rewritten));
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

    private static Yaml.Mapping newMapping() {
        return new Yaml.Mapping(randomId(), Markers.EMPTY, null, emptyList(), null, null, null);
    }

    /**
     * A YAML file's block mappings with the text pnpm's yaml library attaches to each pair: the lines before a key, the
     * comment after a value, a level's trailing lines; what opens a nested level stays where it opens.
     */
    static final class Layout {
        private final Yaml.Documents docs;
        private final String prelude;
        private final Level root;
        private final String trailing;

        private Layout(Yaml.Documents docs, String prelude, Level root, String trailing) {
            this.docs = docs;
            this.prelude = prelude;
            this.root = root;
            this.trailing = trailing;
        }

        /**
         * The layout of {@code docs}; an {@link IllegalArgumentException} for one whose comments it does not place.
         */
        static Layout of(Yaml.Documents docs) {
            if (docs.getDocuments().size() != 1 || docs.getDocuments().get(0).isExplicit() ||
                docs.getDocuments().get(0).getEnd().isExplicit()) {
                throw new IllegalArgumentException("pnpm-workspace.yaml has document markers");
            }
            if (PackageJsonHelper.printed(docs).indexOf('\r') >= 0) {
                throw new IllegalArgumentException("pnpm-workspace.yaml has CR line ends");
            }
            Yaml.Document doc = docs.getDocuments().get(0);
            String after = doc.getEnd().getPrefix();
            Yaml.Block block = doc.getBlock();
            if (block instanceof Yaml.Mapping && ((Yaml.Mapping) block).getOpeningBracePrefix() == null &&
                ((Yaml.Mapping) block).getEntries().isEmpty() ||
                block instanceof Yaml.Scalar && ((Yaml.Scalar) block).getValue().isEmpty() && isNull(block)) {
                return new Layout(docs, doc.getPrefix() + block.getPrefix(), new Level("", "", emptyList(), ""),
                        after);
            }
            if (!isBlockMapping(block)) {
                throw new IllegalArgumentException("pnpm-workspace.yaml is not a block mapping");
            }
            Yaml.Mapping mapping = (Yaml.Mapping) block;
            String prelude = doc.getPrefix() + mapping.getPrefix() + mapping.getEntries().get(0).getPrefix();
            // composer.ts decorate: a comment before the first key without a blank line after it is the key's
            boolean glued = hasComment(prelude) && !blankAfterComments(prelude);
            Read read = read(mapping, after, true, glued ? prelude : null);
            return new Layout(docs, glued ? "" : prelude, read.level, read.rest);
        }

        /**
         * This layout with {@code key: value} under the keys {@code path}, each created where missing, a new pair
         * where {@code writer} puts it; null where a key on the path holds neither a block mapping nor null.
         */
        @Nullable
        Layout with(List<String> path, String key, Yaml.Block value, WorkspaceManifestWriter writer, String unit) {
            Level edited = put(root, path, 0, key, value, writer, unit);
            return edited == null ? null : new Layout(docs, prelude, edited, trailing);
        }

        /**
         * This layout without the pair at {@code path}; this one when there is none.
         */
        Layout without(List<String> path) {
            String key = path.get(path.size() - 1);
            return edited(path.subList(0, path.size() - 1), level -> level.retaining(k -> !k.equals(key)));
        }

        /**
         * This layout with only the pairs of {@code keep} at the level {@code path} names; this one when it holds no
         * other.
         */
        Layout retaining(List<String> path, Set<?> keep) {
            return edited(path, level -> level.retaining(keep::contains));
        }

        /**
         * This layout with every level's pairs in {@code writer}'s order, as pnpm 10's sortKeysByPriority leaves the
         * object patchDocument then gives the document.
         */
        Layout sorted(WorkspaceManifestWriter writer) {
            Level sorted = root.sorted(writer);
            return sorted == root ? this : new Layout(docs, prelude, sorted, trailing);
        }

        /**
         * The document as the writer leaves it; {@code rerendered} when it prints all of it again as pnpm's yaml does:
         * a key line's comment on a line of its own, no pair whose value is null or {}, no blank line at either end.
         */
        Yaml.Documents write(boolean rerendered) {
            Level level = rerendered ? root.withoutNulls() : root;
            if (level.pairs.isEmpty()) {
                throw new IllegalArgumentException("pnpm-workspace.yaml would be left without a key");
            }
            Yaml.Document doc = docs.getDocuments().get(0);
            String[] after = {""};
            Yaml.Mapping mapping = write(isBlockMapping(doc.getBlock()) ? (Yaml.Mapping) doc.getBlock() : newMapping(),
                    level, true, rerendered, after);
            String opening = rerendered && !hasComment(prelude) ? "" : prelude;
            if (hasComment(opening) && !blankAfterComments(opening)) {
                opening += "\n";
            }
            String first = level.pairs.get(0).lead;
            String end = hasComment(trailing) && !trailing.startsWith("\n") ? "\n" + trailing :
                    rerendered && !hasComment(trailing) ? "" : trailing;
            return docs.withDocuments(singletonList(doc.withPrefix(first == null ? opening : opening + first)
                    .withBlock(mapping.withPrefix("")).withEnd(doc.getEnd().withPrefix(after[0] + end))));
        }

        private Layout edited(List<String> path, Function<Level, Level> edit) {
            Level edited = edit(root, path, 0, edit);
            return edited == root ? this : new Layout(docs, prelude, edited, trailing);
        }

        private static Level edit(Level level, List<String> path, int depth, Function<Level, Level> edit) {
            if (depth == path.size()) {
                return edit.apply(level);
            }
            int at = level.index(path.get(depth));
            Level child = at < 0 ? null : level.pairs.get(at).child;
            if (child == null) {
                return level;
            }
            Level edited = edit(child, path, depth + 1, edit);
            return edited == child ? level : level.with(at, level.pairs.get(at).withChild(edited));
        }

        private static @Nullable Level put(Level level, List<String> path, int depth, String key, Yaml.Block value,
                                           WorkspaceManifestWriter writer, String unit) {
            if (depth == path.size()) {
                return level.index(key) >= 0 ? null :
                        insert(level, new Pair(newEntry("", key, value), null, null, "", ""), writer, depth == 0);
            }
            String name = path.get(depth);
            int at = level.index(name);
            String indent = repeat(unit, depth + 1);
            if (at < 0) {
                Level child = put(new Level(indent, "\n" + indent, emptyList(), ""), path, depth + 1, key, value,
                        writer, unit);
                return child == null ? null : insert(level, new Pair(newEntry("", name, newMapping()), null, child,
                        "", ""), writer, depth == 0);
            }
            Pair pair = level.pairs.get(at);
            Level child = pair.child;
            if (child == null && (!isNull(pair.entry.getValue()) || pair.tail == null || hasComment(pair.tail))) {
                return null;
            }
            Level edited = put(child == null ? new Level(indent, "\n" + indent, emptyList(), "") : child, path,
                    depth + 1, key, value, writer, unit);
            return edited == null ? null : level.with(at, new Pair(child == null ?
                    pair.entry.withValue(newMapping()) : pair.entry, pair.lead, edited, "", pair.trail));
        }

        // pnpm's writer: the key among the level's by its order, at the top level with pnpm 11's blank lines
        private static Level insert(Level level, Pair pair, WorkspaceManifestWriter writer, boolean top) {
            int at = writer.insertionIndex(level.keys(), pair.key());
            List<Pair> pairs = new ArrayList<>(level.pairs);
            Pair inserted = pair;
            if (top && !pairs.isEmpty()) {
                List<Boolean> blankBefore = new ArrayList<>();
                for (int i = 0; i < pairs.size(); i++) {
                    String lead = pairs.get(i).lead;
                    blankBefore.add(i > 0 && lead != null && lead.startsWith("\n"));
                }
                if (at == 0) {
                    Pair first = pairs.get(0);
                    pairs.set(0, first.withLead((writer.blankLineStyle(blankBefore) ? "\n" : "") +
                                                (first.lead == null ? "" : first.lead)));
                } else if (writer.blankBefore(blankBefore, at)) {
                    inserted = pair.withLead("\n");
                }
            }
            pairs.add(at, inserted);
            return level.withPairs(pairs);
        }

        private static Read read(Yaml.Mapping mapping, String after, boolean top, @Nullable String firstLead) {
            List<Yaml.Mapping.Entry> entries = mapping.getEntries();
            String opening = top ? "" : entries.get(0).getPrefix();
            String indent = lastLine(opening);
            if (!indent.trim().isEmpty() || mapping.getAnchor() != null || mapping.getTag() != null) {
                throw new IllegalArgumentException("pnpm-workspace.yaml has a mapping it does not lay out");
            }
            List<Pair> pairs = new ArrayList<>();
            String lead = firstLead;
            String remainder = "";
            for (int i = 0; i < entries.size(); i++) {
                Yaml.Mapping.Entry entry = entries.get(i);
                requireLaidOut(entry);
                String next = i + 1 < entries.size() ? entries.get(i + 1).getPrefix() : after;
                Yaml.Block value = entry.getValue();
                if (isBlockMapping(value)) {
                    Read child = read((Yaml.Mapping) value, next, false, null);
                    pairs.add(new Pair(entry, lead, child.level, "", ""));
                    remainder = child.rest;
                } else if (blockScalar(value)) {
                    pairs.add(new Pair(entry, lead, null, null, ""));
                    remainder = next;
                } else {
                    int nl = next.indexOf('\n');
                    String tail = nl < 0 ? next : next.substring(0, nl);
                    remainder = nl < 0 ? "" : next.substring(nl + 1);
                    String trail = "";
                    int items = value instanceof Yaml.Sequence && ((Yaml.Sequence) value).getOpeningBracketPrefix() ==
                                                                  null ? lastLine(((Yaml.Sequence) value).getEntries()
                            .get(0).getPrefix()).length() : -1;
                    if (items > 0 && commentIndented(remainder, items)) {
                        trail = fullLines(remainder);
                        remainder = lastLine(remainder);
                    }
                    // parser.ts atIndentedComment: a comment indented below a value is that value's
                    if (commentIndented(remainder, Math.max(indent.length(), items) + 1)) {
                        throw new IllegalArgumentException("pnpm-workspace.yaml has a comment below " +
                                                           entry.getKey().getValue() + " indented under its value");
                    }
                    pairs.add(new Pair(entry, lead, null, tail, trail));
                }
                lead = remainder;
            }
            // parser.ts pop: a level keeps the comments after its last pair that are indented as deep as it
            if (!top && commentIndented(remainder, indent.length())) {
                return new Read(new Level(indent, opening, pairs, fullLines(remainder)), lastLine(remainder));
            }
            return new Read(new Level(indent, opening, pairs, ""), remainder);
        }

        private static void requireLaidOut(Yaml.Mapping.Entry entry) {
            Yaml.Block value = entry.getValue();
            boolean scalarKey = entry.getKey() instanceof Yaml.Scalar &&
                                ((Yaml.Scalar) entry.getKey()).getStyle() != Yaml.Scalar.Style.LITERAL &&
                                ((Yaml.Scalar) entry.getKey()).getStyle() != Yaml.Scalar.Style.FOLDED &&
                                ((Yaml.Scalar) entry.getKey()).getAnchor() == null &&
                                ((Yaml.Scalar) entry.getKey()).getTag() == null;
            boolean laidOut = value instanceof Yaml.Scalar ? (blockScalar(value) ||
                                                              ((Yaml.Scalar) value).getValue().indexOf('\n') < 0) &&
                                                             ((Yaml.Scalar) value).getAnchor() == null &&
                                                             ((Yaml.Scalar) value).getTag() == null :
                    value instanceof Yaml.Mapping ? ((Yaml.Mapping) value).getAnchor() == null &&
                                                    ((Yaml.Mapping) value).getTag() == null :
                            value instanceof Yaml.Sequence && ((Yaml.Sequence) value).getAnchor() == null &&
                            ((Yaml.Sequence) value).getTag() == null &&
                            (((Yaml.Sequence) value).getOpeningBracketPrefix() != null ||
                             !((Yaml.Sequence) value).getEntries().isEmpty());
            if (!scalarKey || !laidOut) {
                throw new IllegalArgumentException("pnpm-workspace.yaml has a value it does not lay out: " +
                                                   entry.getKey().getValue());
            }
        }

        private static Yaml.Mapping write(Yaml.Mapping mapping, Level level, boolean top, boolean rerendered,
                                          String[] after) {
            List<Yaml.Mapping.Entry> entries = new ArrayList<>();
            String previous = "";
            for (int k = 0; k < level.pairs.size(); k++) {
                Pair pair = level.pairs.get(k);
                String prefix = k > 0 ? previous + (pair.lead == null ? level.indent : pair.lead) :
                        top ? "" : opening(level, pair.lead, rerendered);
                Yaml.Mapping.Entry entry = pair.entry.withPrefix(prefix);
                if (pair.child != null) {
                    String[] childAfter = {""};
                    entry = entry.withValue(write((Yaml.Mapping) entry.getValue(), pair.child, false, rerendered,
                            childAfter));
                    previous = childAfter[0];
                } else {
                    previous = pair.tail == null ? pair.trail : pair.tail + "\n" + pair.trail;
                }
                entries.add(entry);
            }
            after[0] = previous + level.trail;
            return mapping.withEntries(entries);
        }

        // where a nested level opens, then the lines of the pair now first; after a blank line yaml keeps the indent
        private static String opening(Level level, @Nullable String lead, boolean rerendered) {
            String opening = level.opening;
            int nl = opening.indexOf('\n');
            if (rerendered && nl > 0 && opening.substring(0, nl).trim().startsWith("#")) {
                opening = "\n" + level.indent + opening.substring(0, nl).trim() + opening.substring(nl);
            }
            if (lead == null) {
                return opening;
            }
            return lead.startsWith("\n") ? opening + lead : fullLines(opening) + lead;
        }

        private static boolean isBlockMapping(Yaml.@Nullable Block block) {
            return block instanceof Yaml.Mapping && ((Yaml.Mapping) block).getOpeningBracePrefix() == null &&
                   !((Yaml.Mapping) block).getEntries().isEmpty();
        }

        // a literal or folded scalar, whose text ends its last line
        private static boolean blockScalar(Yaml.Block value) {
            return value instanceof Yaml.Scalar && (((Yaml.Scalar) value).getStyle() == Yaml.Scalar.Style.LITERAL ||
                                                    ((Yaml.Scalar) value).getStyle() == Yaml.Scalar.Style.FOLDED);
        }

        private static boolean isNull(Yaml.Block value) {
            return value instanceof Yaml.Scalar && ((Yaml.Scalar) value).getStyle() == Yaml.Scalar.Style.PLAIN &&
                   Arrays.asList("", "~", "null", "Null", "NULL").contains(((Yaml.Scalar) value).getValue());
        }

        private static boolean hasComment(String text) {
            for (String line : text.split("\n", -1)) {
                if (line.trim().startsWith("#")) {
                    return true;
                }
            }
            return false;
        }

        // whether a line of white space follows the text's last comment
        private static boolean blankAfterComments(String text) {
            int last = -1;
            String[] lines = text.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].trim().startsWith("#")) {
                    last = i;
                }
            }
            return last >= 0 && last + 2 < lines.length;
        }

        // whether a full line of the text is a comment indented by at least `columns`
        private static boolean commentIndented(String text, int columns) {
            for (String line : fullLines(text).split("\n", -1)) {
                if (line.trim().startsWith("#") && line.indexOf('#') >= columns) {
                    return true;
                }
            }
            return lastLine(text).trim().startsWith("#") && lastLine(text).indexOf('#') >= columns;
        }

        private static String fullLines(String text) {
            return text.substring(0, text.lastIndexOf('\n') + 1);
        }

        private static String lastLine(String text) {
            return text.substring(text.lastIndexOf('\n') + 1);
        }

        private static final class Read {
            final Level level;
            final String rest;

            Read(Level level, String rest) {
                this.level = level;
                this.rest = rest;
            }
        }

        // one block mapping: its pairs, the text it opens with (nested levels) and its trailing lines
        private static final class Level {
            final String indent;
            final String opening;
            final List<Pair> pairs;
            final String trail;

            Level(String indent, String opening, List<Pair> pairs, String trail) {
                this.indent = indent;
                this.opening = opening;
                this.pairs = pairs;
                this.trail = trail;
            }

            int index(String key) {
                for (int i = 0; i < pairs.size(); i++) {
                    if (key.equals(pairs.get(i).key())) {
                        return i;
                    }
                }
                return -1;
            }

            List<String> keys() {
                List<String> keys = new ArrayList<>();
                for (Pair pair : pairs) {
                    keys.add(pair.key());
                }
                return keys;
            }

            Level withPairs(List<Pair> pairs) {
                return new Level(indent, opening, pairs, trail);
            }

            Level with(int at, Pair pair) {
                List<Pair> edited = new ArrayList<>(pairs);
                edited.set(at, pair);
                return withPairs(edited);
            }

            Level retaining(Predicate<String> keep) {
                List<Pair> kept = new ArrayList<>();
                for (Pair pair : pairs) {
                    if (keep.test(pair.key())) {
                        kept.add(pair);
                    }
                }
                return kept.size() == pairs.size() ? this : withPairs(kept);
            }

            Level sorted(WorkspaceManifestWriter writer) {
                List<Pair> sorted = new ArrayList<>();
                List<String> keys = new ArrayList<>();
                boolean changed = false;
                for (Pair pair : pairs) {
                    Pair p = pair;
                    if (pair.child != null) {
                        Level child = pair.child.sorted(writer);
                        if (child != pair.child) {
                            p = pair.withChild(child);
                            changed = true;
                        }
                    } else if (holdsMapping(pair.entry.getValue())) {
                        throw new IllegalArgumentException("pnpm-workspace.yaml has a flow mapping or a sequence of " +
                                                           "mappings, whose keys pnpm 10 sorts in a document it " +
                                                           "rewrites");
                    }
                    int at = writer.insertionIndex(keys, p.key());
                    keys.add(at, p.key());
                    sorted.add(at, p);
                    changed |= at != sorted.size() - 1;
                }
                return changed ? withPairs(sorted) : this;
            }

            Level withoutNulls() {
                List<Pair> kept = new ArrayList<>();
                boolean changed = false;
                for (Pair pair : pairs) {
                    if (pair.child != null) {
                        Level child = pair.child.withoutNulls();
                        if (child.pairs.isEmpty()) {
                            throw new IllegalArgumentException("pnpm-workspace.yaml has " + pair.key() + " holding " +
                                                               "nulls only, which pnpm writes as {}");
                        }
                        kept.add(child == pair.child ? pair : pair.withChild(child));
                        changed |= child != pair.child;
                    } else if (isNull(pair.entry.getValue()) || emptyFlow(pair.entry.getValue()) ||
                               pair.entry.getValue() instanceof Yaml.Mapping &&
                               ((Yaml.Mapping) pair.entry.getValue()).getEntries().isEmpty()) {
                        changed = true;
                    } else {
                        kept.add(pair);
                    }
                }
                return changed ? withPairs(kept) : this;
            }

            private static boolean holdsMapping(Yaml.Block value) {
                if (value instanceof Yaml.Sequence) {
                    for (Yaml.Sequence.Entry entry : ((Yaml.Sequence) value).getEntries()) {
                        if (entry.getBlock() instanceof Yaml.Mapping) {
                            return true;
                        }
                    }
                    return false;
                }
                return value instanceof Yaml.Mapping && !((Yaml.Mapping) value).getEntries().isEmpty() ||
                       value instanceof Yaml.Scalar && ((Yaml.Scalar) value).getStyle() == Yaml.Scalar.Style.PLAIN &&
                       ((Yaml.Scalar) value).getValue().startsWith("{") && !emptyFlow(value);
            }
        }

        // a key and its value with the lines before it, and the rest of its value's last line (the comment after it;
        // null after a block scalar) or its nested level; a sequence's own trailing lines
        private static final class Pair {
            final Yaml.Mapping.Entry entry;
            final @Nullable String lead;
            final @Nullable Level child;
            final @Nullable String tail;
            final String trail;

            Pair(Yaml.Mapping.Entry entry, @Nullable String lead, @Nullable Level child, @Nullable String tail,
                 String trail) {
                this.entry = entry;
                this.lead = lead;
                this.child = child;
                this.tail = tail;
                this.trail = trail;
            }

            String key() {
                return entry.getKey().getValue();
            }

            Pair withLead(@Nullable String lead) {
                return new Pair(entry, lead, child, tail, trail);
            }

            Pair withChild(Level child) {
                return new Pair(entry, lead, child, tail, trail);
            }
        }
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
