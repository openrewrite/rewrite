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

import lombok.AccessLevel;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.EcmaScript;
import org.openrewrite.javascript.internal.lock.ManifestRules;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.internal.lock.YamlDocument;
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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * A workspace's catalogs as its package manager's port reads them (pnpm from pnpm-workspace.yaml, Yarn from
 * .yarnrc.yml, Bun from the root package.json), by catalog then alias; an edit changes the document in place.
 */
@Getter
public abstract class NodeCatalogs {

    /**
     * The file the package manager reads them from.
     */
    private final SourceFile document;

    @Getter(AccessLevel.PACKAGE)
    private final ManifestRules rules;

    private final Map<String, Map<String, String>> catalogs;

    // why the package manager reads no catalogs from the file
    @Getter(AccessLevel.NONE)
    private final @Nullable String rejection;

    private NodeCatalogs(SourceFile document, ManifestRules rules) {
        this.document = document;
        this.rules = rules;
        Map<String, Map<String, String>> read = Collections.emptyMap();
        String rejected = null;
        try {
            read = rules.catalogs(PackageJsonHelper.printed(document));
        } catch (Rejected | CannotKnow e) {
            rejected = e.getMessage();
        }
        this.catalogs = read;
        this.rejection = rejected;
    }

    /**
     * The name of the file {@code pm} reads catalogs from, in the workspace root; null for one without catalogs.
     */
    public static @Nullable String fileName(@Nullable PackageManager pm) {
        return LockFileRegeneration.manifestRules(pm).catalogFile();
    }

    /**
     * The catalogs {@code document} declares by {@code pm}'s rules; null when it is not the file {@code pm} reads them
     * from, or a Bun package.json without workspaces.
     */
    public static @Nullable NodeCatalogs read(@Nullable PackageManager pm, SourceFile document) {
        return read(pm, document, null);
    }

    /**
     * As {@link #read(PackageManager, SourceFile)}, pnpm's written as the release {@code rootManifest} names (the
     * ported one when it names none) writes the file.
     */
    public static @Nullable NodeCatalogs read(@Nullable PackageManager pm, SourceFile document,
                                              @Nullable String rootManifest) {
        return read(LockFileRegeneration.manifestRules(pm), document, rootManifest);
    }

    /**
     * The catalogs {@code document} declares by {@code rules}; null when it is not a file they read catalogs from.
     */
    public static @Nullable NodeCatalogs read(ManifestRules rules, SourceFile document, @Nullable String rootManifest) {
        if (rules.catalogFile() == null || rules.catalogsAt(PackageJsonHelper.printed(document)) == null) {
            return null;
        }
        if (document instanceof Json.Document) {
            return new Bun((Json.Document) document, rules);
        }
        if (!(document instanceof Yaml.Documents)) {
            return null;
        }
        return rules.writesCatalogFile() ? new Pnpm((Yaml.Documents) document, rules, rootManifest, null) :
                new YarnBerry((Yaml.Documents) document, rules);
    }

    public Path getPath() {
        return document.getSourcePath();
    }

    /**
     * The catalog a dependency value names, by the package manager's rules; null when it is no catalog reference.
     */
    public @Nullable String catalogOf(String value) {
        return rules.catalogOf(value);
    }

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
     * The catalogs with the entry's specifier replaced, quoting kept: null when the catalog has no such entry or one
     * that is not a version range (a block scalar, another protocol), these catalogs when it already holds it.
     */
    public abstract @Nullable NodeCatalogs withSpecifier(String catalog, String alias, String specifier);

    /**
     * The catalogs with {@code alias: specifier} in the catalog, its entry replaced or added where pnpm's workspace
     * manifest writer puts a new key, the catalog created when there is none; null when it cannot be written.
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
     * Why these catalogs are not edited: the package manager rejects the file, or the release that writes it is not
     * one whose writes are recorded; null when they are.
     */
    public @Nullable String declined() {
        return rejection;
    }

    /**
     * The workspace settings the package manager reads from this file beside its catalogs; null for none.
     */
    public @Nullable Map<String, @Nullable Object> getSettings() {
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

        Pnpm(Yaml.Documents document, ManifestRules rules, @Nullable String rootManifest,
             @Nullable PnpmRelease release) {
            super(document, rules);
            this.rootManifest = rootManifest;
            this.release = release;
        }

        /**
         * This file for the release {@code rootManifest}, a revision of the root's package.json, names.
         */
        public Pnpm forRoot(String rootManifest) {
            return rootManifest.equals(this.rootManifest) ? this : sharingSettings(rootManifest, null);
        }

        /**
         * This file for {@code release}, read already from the root and this file.
         */
        Pnpm forRelease(PnpmRelease release) {
            return release == this.release ? this : sharingSettings(rootManifest, release);
        }

        private Pnpm sharingSettings(@Nullable String rootManifest, @Nullable PnpmRelease release) {
            Pnpm other = new Pnpm((Yaml.Documents) getDocument(), getRules(), rootManifest, release);
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
                release = requireNonNull(PnpmRelease.of(getRules(), rootManifest == null ? "{}" : rootManifest,
                        getSettings()));
            }
            return release;
        }

        @Override
        public @Nullable String declined() {
            String declined = getRelease().declined();
            return declined != null ? declined : super.declined();
        }

        /**
         * Whether an install by the release the root names cleans unused catalog entries up: the setting is one
         * JavaScript holds true, and the release has it.
         */
        public boolean cleansUp() {
            return isCleanupUnusedCatalogs() && getRelease().cleansUpUnusedCatalogs();
        }

        /**
         * pnpm-workspace.yaml as {@code text} (none when null), read by {@code rules}, for {@code release}.
         */
        static Pnpm parse(@Nullable String text, ManifestRules rules, PnpmRelease release) {
            SourceFile parsed = YamlParser.builder().build().parse(new InMemoryExecutionContext(),
                    text == null ? "" : text).findFirst().orElseThrow(IllegalStateException::new);
            if (!(parsed instanceof Yaml.Documents)) {
                throw new IllegalArgumentException("pnpm-workspace.yaml is not YAML pnpm reads");
            }
            return new Pnpm((Yaml.Documents) parsed.withSourcePath(Paths.get(requireNonNull(rules.catalogFile()))),
                    rules, null, release);
        }

        /**
         * pnpm-workspace.yaml as pnpm's workspace-manifest-reader reads it, once; null when empty or
         * {@link #isRejected}.
         */
        @Override
        public @Nullable Map<String, @Nullable Object> getSettings() {
            if (!settingsRead) {
                settingsRead = true;
                try {
                    settings = getRules().workspaceSettings(print());
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

        /**
         * The file with the override {@code key: value} as pnpm writes it, an existing value replaced; this one when
         * it holds that value. An {@link IllegalArgumentException} names why it is not written.
         */
        public NodeCatalogs withOverride(String key, String value) {
            return written(manifest -> {
                Object overrides = manifest.get("overrides");
                if (overrides == null) {
                    overrides = new LinkedHashMap<String, @Nullable Object>();
                    manifest.put("overrides", overrides);
                }
                if (!(overrides instanceof Map)) {
                    throw new IllegalArgumentException("pnpm-workspace.yaml's overrides is not a mapping");
                }
                return !value.equals(asMap(overrides).put(key, value));
            });
        }

        /**
         * Whether {@code cleanupUnusedCatalogs} is a value JavaScript holds true.
         */
        public boolean isCleanupUnusedCatalogs() {
            Map<String, @Nullable Object> read = getSettings();
            return read != null && EcmaScript.truthy(read.get("cleanupUnusedCatalogs"));
        }

        /**
         * The file without what {@link WorkspaceManifestWriter#cleanupUnusedCatalogs} removes over {@code projects}
         * (null when unread); null when declined, an {@link IllegalArgumentException} when not written as pnpm does.
         */
        public @Nullable NodeCatalogs withoutUnused(Map<String, @Nullable Map<String, @Nullable Object>> projects) {
            if (declined() != null) {
                return null;
            }
            Map<String, @Nullable Object> read = getSettings();
            if (read == null || yamlRoot((Yaml.Documents) getDocument()) == null ||
                !getRelease().cleansUpUnusedCatalogs()) {
                return this;
            }
            Map<String, @Nullable Object> cleaned = copied(read);
            if (!getRelease().cleanupKeepsOverrideReferences()) {
                cleaned.remove("overrides");
            }
            if (!WorkspaceManifestWriter.cleanupUnusedCatalogs(cleaned, projects)) {
                return this;
            }
            if (isFlowStyle()) {
                throw new IllegalArgumentException("its flow-style catalogs are not read");
            }
            return written(manifest -> {
                retain(manifest, "catalog", cleaned.get("catalog"));
                Object named = manifest.get("catalogs");
                if (named instanceof Map) {
                    Object kept = cleaned.get("catalogs");
                    for (String group : new ArrayList<>(asMap(named).keySet())) {
                        retain(asMap(named), group, kept instanceof Map ? asMap(kept).get(group) : null);
                    }
                }
                retain(manifest, "catalogs", cleaned.get("catalogs"));
                return true;
            });
        }

        private static void retain(Map<String, @Nullable Object> map, String key, @Nullable Object kept) {
            Object value = map.get(key);
            if (!(value instanceof Map)) {
                return;
            }
            if (kept instanceof Map) {
                asMap(value).keySet().retainAll(asMap(kept).keySet());
            } else {
                map.remove(key);
            }
        }

        private static Map<String, @Nullable Object> copied(Map<String, @Nullable Object> read) {
            Map<String, @Nullable Object> copy = new LinkedHashMap<>(read);
            for (String key : Arrays.asList("catalog", "catalogs")) {
                Object value = copy.get(key);
                if (value instanceof Map) {
                    Map<String, @Nullable Object> level = new LinkedHashMap<>(asMap(value));
                    if ("catalogs".equals(key)) {
                        level.replaceAll((name, group) -> group instanceof Map ? new LinkedHashMap<>(asMap(group)) :
                                group);
                    }
                    copy.put(key, level);
                }
            }
            return copy;
        }

        // the release read once for the root: an edit leaving pmOnFail as it was leaves it
        @Override
        NodeCatalogs reread(SourceFile edited) {
            Pnpm next = new Pnpm((Yaml.Documents) edited, getRules(), rootManifest, null);
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

        // pnpm 9 never writes the file, so its entry is the recipe's own edit
        @Override
        @Nullable
        NodeCatalogs replaced(List<String> path, String key, String value) {
            if (getRelease().writer() == null) {
                return super.replaced(path, key, value);
            }
            return inserted(path, key, value);
        }

        // addCatalogs
        @Override
        @Nullable
        NodeCatalogs inserted(List<String> path, String key, String value) {
            if (getRelease().writer() == null) {
                return null;
            }
            try {
                return written(manifest -> {
                    Map<String, @Nullable Object> level = manifest;
                    for (String name : path) {
                        Object child = level.get(name);
                        if (child == null) {
                            child = new LinkedHashMap<String, @Nullable Object>();
                            level.put(name, child);
                        }
                        if (!(child instanceof Map)) {
                            throw new IllegalArgumentException("pnpm-workspace.yaml's " + name + " is not a mapping");
                        }
                        level = asMap(child);
                    }
                    if (level.get(key) != null && !(level.get(key) instanceof String)) {
                        throw new IllegalArgumentException("pnpm-workspace.yaml's " + key + " holds no scalar");
                    }
                    return !value.equals(level.put(key, value));
                });
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        /**
         * updateWorkspaceManifest: the manifest pnpm's yaml reads from the file, as {@code edit} leaves it, patched
         * into the document and printed; this file when the edit changes nothing.
         */
        private NodeCatalogs written(Edit edit) {
            String declined = declined();
            if (declined != null) {
                throw new IllegalArgumentException(declined);
            }
            if (isRejected()) {
                throw new IllegalArgumentException("pnpm-workspace.yaml is not YAML pnpm reads: " +
                                                   requireNonNull(rejection).getMessage(), rejection);
            }
            WorkspaceManifestWriter writer = getRelease().writer();
            if (writer == null) {
                throw new IllegalArgumentException(getRelease() + " never writes pnpm-workspace.yaml");
            }
            YamlDocument document = YamlDocument.parse((Yaml.Documents) getDocument());
            Object read = document.toJs();
            if (read != null && !(read instanceof Map)) {
                throw new IllegalArgumentException("pnpm-workspace.yaml is not a mapping");
            }
            Map<String, @Nullable Object> manifest = read == null ? new LinkedHashMap<>() : asMap(read);
            Object original = keyOrder(manifest);
            if (!edit.apply(manifest)) {
                return this;
            }
            if (manifest.isEmpty()) {
                throw new IllegalArgumentException("pnpm would delete pnpm-workspace.yaml, which it leaves empty");
            }
            writer.write(document, original, manifest);
            return reread(PackageJsonHelper.reparseYaml((Yaml.Documents) getDocument(), document.toString()));
        }

        // captureKeyOrder
        private static @Nullable Object keyOrder(@Nullable Object value) {
            if (!(value instanceof Map)) {
                return null;
            }
            Map<String, @Nullable Object> order = new LinkedHashMap<>();
            for (Map.Entry<String, @Nullable Object> entry : asMap(value).entrySet()) {
                order.put(entry.getKey(), keyOrder(entry.getValue()));
            }
            return order;
        }

        @SuppressWarnings("unchecked")
        private static Map<String, @Nullable Object> asMap(Object value) {
            return (Map<String, @Nullable Object>) value;
        }

        private interface Edit {
            /**
             * Whether it changed the manifest pnpm read.
             */
            boolean apply(Map<String, @Nullable Object> manifest);
        }
    }

    /**
     * Yarn's catalogs from .yarnrc.yml: {@code catalog} for {@code catalog:}, {@code catalogs.<name>} otherwise.
     */
    public static final class YarnBerry extends YamlCatalogs {

        YarnBerry(Yaml.Documents document, ManifestRules rules) {
            super(document, rules);
        }

        @Override
        NodeCatalogs reread(SourceFile edited) {
            return new YarnBerry((Yaml.Documents) edited, getRules());
        }

        @Override
        List<String> yamlPath(String catalog) {
            return catalog.isEmpty() ? singletonList("catalog") : Arrays.asList("catalogs", catalog);
        }

        // Yarn writes no catalog entry: the recipe's own edit puts the pair where pnpm 11's writer would, its quoting
        // too, and leaves the rest of the text as it is
        @Override
        @Nullable
        NodeCatalogs inserted(List<String> path, String key, String value) {
            Yaml.Documents docs = (Yaml.Documents) getDocument();
            Yaml.Mapping root = yamlRoot(docs);
            if (root == null || docs.getDocuments().size() != 1) {
                return null;
            }
            Yaml.Document doc = docs.getDocuments().get(0);
            String[] following = {doc.getEnd().getPrefix()};
            Yaml.Mapping edited;
            try {
                edited = insertedInto(root, "", path, key, YamlDocument.scalar(value), following,
                        print().contains("\r\n") ? "\r\n" : "\n");
            } catch (IllegalArgumentException e) {
                return null;
            }
            return edited == null ? null : reread(docs.withDocuments(singletonList(doc.withBlock(edited)
                    .withEnd(doc.getEnd().withPrefix(following[0])))));
        }

        // following[0] is the text after the mapping's last value, whose first line a pair appended after it takes
        private Yaml.@Nullable Mapping insertedInto(Yaml.Mapping mapping, String indent, List<String> path, String key,
                                                    String value, String[] following, String lineBreak) {
            List<Yaml.Mapping.Entry> entries = new ArrayList<>(mapping.getEntries());
            List<String> keys = new ArrayList<>();
            for (Yaml.Mapping.Entry entry : entries) {
                keys.add(entry.getKey().getValue());
            }
            String name = path.isEmpty() ? key : path.get(0);
            int at = keys.indexOf(name);
            if (!path.isEmpty() && at >= 0) {
                Yaml.Block child = entries.get(at).getValue();
                if (!(child instanceof Yaml.Mapping) || ((Yaml.Mapping) child).getOpeningBracePrefix() != null ||
                    ((Yaml.Mapping) child).getEntries().isEmpty()) {
                    return null;
                }
                String prefix = ((Yaml.Mapping) child).getEntries().get(0).getPrefix();
                String[] next = at + 1 < entries.size() ? new String[]{entries.get(at + 1).getPrefix()} : following;
                Yaml.Mapping edited = insertedInto((Yaml.Mapping) child, prefix.substring(prefix.lastIndexOf('\n') + 1),
                        path.subList(1, path.size()), key, value, next, lineBreak);
                if (edited == null) {
                    return null;
                }
                entries.set(at, entries.get(at).withValue(edited));
                if (at + 1 < entries.size()) {
                    entries.set(at + 1, entries.get(at + 1).withPrefix(next[0]));
                }
                return mapping.withEntries(entries);
            }
            if (at >= 0) {
                return null;
            }
            String unit = indentUnit(yamlRoot((Yaml.Documents) getDocument()));
            StringBuilder text = new StringBuilder();
            String nested = indent;
            for (int i = 0; i < path.size(); i++) {
                text.append(i == 0 ? "" : nested).append(path.get(i)).append(':').append(lineBreak);
                nested += unit;
            }
            text.append(path.isEmpty() ? "" : nested).append(YamlDocument.scalar(key)).append(": ")
                    .append(value).append(lineBreak);
            Yaml.Mapping parsed = yamlRoot((Yaml.Documents) YamlParser.builder().build().parse(text.toString())
                    .findFirst().orElseThrow(IllegalStateException::new));
            Yaml.Mapping.Entry entry = requireNonNull(parsed).getEntries().get(0);
            at = new WorkspaceManifestWriter(11).insertionIndex(keys, name);
            if (at < entries.size()) {
                String prefix = entries.get(at).getPrefix();
                int cut = lineBreakAt(prefix);
                entries.set(at, entries.get(at).withPrefix(cut < 0 ? lineBreak + indent : prefix.substring(cut)));
                entry = entry.withPrefix(cut < 0 ? prefix : prefix.substring(0, cut) + lineBreak + indent);
            } else {
                int cut = lineBreakAt(following[0]);
                entry = entry.withPrefix((cut < 0 ? following[0] : following[0].substring(0, cut)) + lineBreak +
                                         indent);
                following[0] = cut < 0 ? "" : following[0].substring(cut);
            }
            entries.add(at, entry);
            return mapping.withEntries(entries);
        }

        private static int lineBreakAt(String text) {
            int nl = text.indexOf('\n');
            return nl > 0 && text.charAt(nl - 1) == '\r' ? nl - 1 : nl;
        }
    }

    /**
     * Bun's catalogs from the root package.json: {@code workspaces.catalog(s)} when the workspaces object holds one,
     * the top-level {@code catalog(s)} otherwise.
     */
    public static final class Bun extends NodeCatalogs {

        Bun(Json.Document document, ManifestRules rules) {
            super(document, rules);
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
            return reread(PackageJsonHelper.withStringMember(document, path(catalog), alias, specifier));
        }

        @Override
        NodeCatalogs reread(SourceFile edited) {
            return new Bun((Json.Document) edited, getRules());
        }

        private List<String> path(String catalog) {
            List<String> path = new ArrayList<>(requireNonNull(getRules().catalogsAt(print())));
            if (catalog.isEmpty()) {
                path.add("catalog");
            } else {
                path.add("catalogs");
                path.add(catalog);
            }
            return path;
        }
    }

    /**
     * Catalogs in a YAML file, an entry's scalar replaced in place, quoting kept, unless the package manager writes
     * the file itself.
     */
    abstract static class YamlCatalogs extends NodeCatalogs {

        private final boolean flowStyle;

        YamlCatalogs(Yaml.Documents document, ManifestRules rules) {
            super(document, rules);
            this.flowStyle = flowStyle(yamlRoot(document));
        }

        /**
         * The keys leading from the document's root to the catalog's mapping.
         */
        abstract List<String> yamlPath(String catalog);

        /**
         * The file with a new pair {@code key: value} under the keys {@code path}, each created where missing; null
         * when it is not written.
         */
        abstract @Nullable NodeCatalogs inserted(List<String> path, String key, String value);

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
            if (current.equals(specifier)) {
                return this;
            }
            return inBlockScalar(catalog, alias) ? null : replaced(yamlPath(catalog), alias, specifier);
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
            if (current == null) {
                return inserted(yamlPath(catalog), alias, specifier);
            }
            return inBlockScalar(catalog, alias) ? null : replaced(yamlPath(catalog), alias, specifier);
        }

        // an entry is one scalar edited in place, which a block scalar is not
        private boolean inBlockScalar(String catalog, String alias) {
            Yaml.Scalar scalar = entryScalar(yamlRoot((Yaml.Documents) getDocument()), yamlPath(catalog), alias);
            return scalar == null || scalar.getStyle() == Yaml.Scalar.Style.LITERAL ||
                   scalar.getStyle() == Yaml.Scalar.Style.FOLDED;
        }

        /**
         * The file with the scalar at {@code path} then {@code key} given {@code value}, its quotes kept; null for a
         * block scalar or a value its quotes cannot hold.
         */
        @Nullable
        NodeCatalogs replaced(List<String> path, String key, String value) {
            Yaml.Documents docs = (Yaml.Documents) getDocument();
            Yaml.Scalar scalar = entryScalar(yamlRoot(docs), path, key);
            if (scalar == null) {
                return null;
            }
            Yaml.Scalar rewritten;
            try {
                YamlDocument.scalar(value);
                rewritten = scalar.getStyle() == Yaml.Scalar.Style.SINGLE_QUOTED ?
                        scalar.withValue(value.replace("'", "''")) :
                        scalar.getStyle() == Yaml.Scalar.Style.DOUBLE_QUOTED ?
                                scalar.withValue(YamlDocument.doubleQuoted(value)
                                        .substring(1, YamlDocument.doubleQuoted(value).length() - 1)) :
                                plain(scalar, YamlDocument.scalar(value));
            } catch (IllegalArgumentException e) {
                return null;
            }
            return reread((SourceFile) new YamlIsoVisitor<Integer>() {
                @Override
                public Yaml.Scalar visitScalar(Yaml.Scalar s, Integer p) {
                    return s == scalar ? rewritten : s;
                }
            }.visitNonNull(docs, 0));
        }

        // a plain scalar given the text yaml writes a new one with, in quotes when it needs them
        private static Yaml.Scalar plain(Yaml.Scalar scalar, String written) {
            Yaml.Scalar.Style style = written.startsWith("'") ? Yaml.Scalar.Style.SINGLE_QUOTED :
                    written.startsWith("\"") ? Yaml.Scalar.Style.DOUBLE_QUOTED : Yaml.Scalar.Style.PLAIN;
            return scalar.withStyle(style).withValue(style == Yaml.Scalar.Style.PLAIN ? written :
                    written.substring(1, written.length() - 1));
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
        return emptyFlow(value) ? new Yaml.Mapping(Tree.randomId(), Markers.EMPTY, null, emptyList(), null, null,
                null) : null;
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

    private static String indentUnit(Yaml.@Nullable Mapping root) {
        for (Yaml.Mapping.Entry entry : root == null ? Collections.<Yaml.Mapping.Entry>emptyList() :
                root.getEntries()) {
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
