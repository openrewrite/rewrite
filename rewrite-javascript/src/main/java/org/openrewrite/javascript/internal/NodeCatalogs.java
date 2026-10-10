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
import lombok.AllArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;
import org.openrewrite.yaml.tree.YamlKey;

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
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

/**
 * A workspace's catalogs as its package manager reads them (pnpm from pnpm-workspace.yaml, Yarn from .yarnrc.yml, Bun
 * from the root package.json), by catalog then alias; an edit changes the document in place.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NodeCatalogs {

    /**
     * The default catalog's name here.
     */
    public static final String DEFAULT = "";

    private static final String PROTOCOL = "catalog:";
    private static final Pattern YAML_NON_STRING = Pattern.compile(
            "[-+]?(\\.[0-9][0-9_]*|[0-9][0-9_]*(\\.[0-9_]*)?)([eE][-+]?[0-9]+)?" +
            "|[-+]?0[xXbBoO][0-9a-fA-F_]+" +
            "|[-+]?\\.(inf|Inf|INF)|\\.(nan|NaN|NAN)" +
            "|~|[nN]ull|NULL" +
            "|[tT]rue|TRUE|[fF]alse|FALSE|[yY]es|YES|[nN]o|NO|[oO]n|ON|[oO]ff|OFF");
    private static final String PLAIN_SCALAR_INDICATORS = "-?:,[]{}#&*!|>'\"%@`";

    PackageManager packageManager;

    /**
     * The file the package manager reads them from, as this cycle's edits leave it.
     */
    SourceFile document;

    Map<String, Map<String, String>> catalogs;

    /**
     * pnpm's message when the default catalog is spelled both ways, which it rejects.
     */
    @Nullable
    String misconfiguration;

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
    public static @Nullable NodeCatalogs read(PackageManager pm, SourceFile document) {
        if (pm == PackageManager.Bun) {
            if (!(document instanceof Json.Document) || bunBase((Json.Document) document) == null) {
                return null;
            }
            return new NodeCatalogs(pm, document, bunCatalogs((Json.Document) document), null);
        }
        if ((pm != PackageManager.Pnpm && pm != PackageManager.YarnBerry) || !(document instanceof Yaml.Documents)) {
            return null;
        }
        Yaml.Mapping root = yamlRoot((Yaml.Documents) document);
        Map<String, Map<String, String>> catalogs = new LinkedHashMap<>();
        String misconfiguration = null;
        Yaml.Mapping catalog = root == null ? null : mapping(root, "catalog");
        Yaml.Mapping named = root == null ? null : mapping(root, "catalogs");
        if (catalog != null) {
            catalogs.put(DEFAULT, scalars(catalog));
        }
        if (named != null) {
            for (Yaml.Mapping.Entry group : named.getEntries()) {
                if (!(group.getValue() instanceof Yaml.Mapping)) {
                    continue;
                }
                String name = group.getKey().getValue();
                if (pm == PackageManager.Pnpm && "default".equals(name)) {
                    if (catalog != null) {
                        misconfiguration = "The 'default' catalog was defined multiple times. " +
                                           "Use the 'catalog' field or 'catalogs.default', but not both.";
                        continue;
                    }
                    name = DEFAULT;
                }
                catalogs.put(name, scalars((Yaml.Mapping) group.getValue()));
            }
        }
        return new NodeCatalogs(pm, document, catalogs, misconfiguration);
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
    public @Nullable String catalogOf(String value) {
        if (!value.startsWith(PROTOCOL)) {
            return null;
        }
        String name = value.substring(PROTOCOL.length());
        if (packageManager == PackageManager.Pnpm) {
            name = name.trim();
            return name.isEmpty() || "default".equals(name) ? DEFAULT : name;
        }
        return packageManager == PackageManager.Bun ? name.trim() : name;
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
     * pnpm's {@code catalogMode} as pnpm-workspace.yaml sets it: manual, prefer or strict.
     */
    public String getCatalogMode() {
        String mode = packageManager == PackageManager.Pnpm ? topLevelScalar("catalogMode") : null;
        return mode == null ? "manual" : mode;
    }

    /**
     * Whether pnpm drops the entries no importer references ({@code cleanupUnusedCatalogs}).
     */
    public boolean isCleanupUnusedCatalogs() {
        return packageManager == PackageManager.Pnpm && "true".equals(topLevelScalar("cleanupUnusedCatalogs"));
    }

    /**
     * The catalogs with the entry's specifier replaced in place, quoting kept: null when the catalog has no such entry
     * or one that is not a version range (a block scalar, another protocol), these catalogs when it already holds it.
     */
    public @Nullable NodeCatalogs withSpecifier(String catalog, String alias, String specifier) {
        String current = specifier(catalog, alias);
        if (current == null || !isVersionSpec(alias, current)) {
            return null;
        }
        if (current.equals(specifier)) {
            return this;
        }
        if (document instanceof Json.Document) {
            List<String> path = bunPath((Json.Document) document, catalog);
            return reread(PackageJsonHelper.withStringMember((Json.Document) document, path, alias, specifier));
        }
        Yaml.Documents docs = (Yaml.Documents) document;
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

    /**
     * The catalogs with {@code alias: specifier} in the catalog, its entry replaced or added where pnpm's workspace
     * manifest writer puts a new key, and the catalog created when there is none.
     */
    public NodeCatalogs withEntry(String catalog, String alias, String specifier) {
        String current = specifier(catalog, alias);
        if (specifier.equals(current)) {
            return this;
        }
        if (document instanceof Json.Document) {
            List<String> path = bunPath((Json.Document) document, catalog);
            return reread(PackageJsonHelper.withStringMember((Json.Document) document, path, alias, specifier));
        }
        Yaml.Documents docs = (Yaml.Documents) document;
        Yaml.Mapping root = yamlRoot(docs);
        if (current != null) {
            Yaml.Scalar scalar = entryScalar(root, yamlPath(catalog), alias);
            if (scalar != null && scalar.getStyle() != Yaml.Scalar.Style.LITERAL &&
                scalar.getStyle() != Yaml.Scalar.Style.FOLDED) {
                Yaml.Scalar rewritten = scalar.withValue(specifier);
                if (scalar.getStyle() == Yaml.Scalar.Style.PLAIN && !canBePlainScalar(specifier)) {
                    rewritten = rewritten.withStyle(Yaml.Scalar.Style.SINGLE_QUOTED);
                }
                return reread(replaceScalar(docs, scalar, rewritten));
            }
        }
        Yaml.Mapping base = root == null ? new Yaml.Mapping(randomId(), Markers.EMPTY, null, emptyList(), null, null,
                null) : root;
        return reread(withRoot(docs, put(base, yamlPath(catalog), 0, alias, specifier, indentUnit(base))));
    }

    /**
     * pnpm's {@code removePackagesFromWorkspaceCatalog}: the catalogs without the entries no importer's declarations
     * nor a {@code catalog:} override reference, and without a catalog left empty.
     */
    public NodeCatalogs withoutUnused(Collection<Map<String, String>> declarations) {
        if (!(document instanceof Yaml.Documents) || packageManager != PackageManager.Pnpm || declarations.isEmpty()) {
            return this;
        }
        Yaml.Documents docs = (Yaml.Documents) document;
        Yaml.Mapping root = yamlRoot(docs);
        if (root == null || mapping(root, "catalog") == null && mapping(root, "catalogs") == null) {
            return this;
        }
        Map<String, Set<String>> references = new HashMap<>();
        for (Map<String, String> declared : declarations) {
            for (Map.Entry<String, String> dependency : declared.entrySet()) {
                references.computeIfAbsent(dependency.getKey(), k -> new HashSet<>()).add(dependency.getValue());
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
                if (!(group.getValue() instanceof Yaml.Mapping)) {
                    groups.add(group);
                    continue;
                }
                Yaml.Mapping kept = retain((Yaml.Mapping) group.getValue(), references, group.getKey().getValue());
                changed |= kept != group.getValue();
                if (!kept.getEntries().isEmpty()) {
                    groups.add(kept == group.getValue() ? group : group.withValue(kept));
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

    public String print() {
        return document.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED));
    }

    private NodeCatalogs reread(SourceFile edited) {
        NodeCatalogs read = read(packageManager, edited);
        return read == null ? this : read;
    }

    private @Nullable String topLevelScalar(String key) {
        Yaml.Mapping root = document instanceof Yaml.Documents ? yamlRoot((Yaml.Documents) document) : null;
        if (root == null) {
            return null;
        }
        for (Yaml.Mapping.Entry entry : root.getEntries()) {
            if (key.equals(entry.getKey().getValue()) && entry.getValue() instanceof Yaml.Scalar) {
                return ((Yaml.Scalar) entry.getValue()).getValue();
            }
        }
        return null;
    }

    private static boolean isVersionSpec(String alias, String value) {
        try {
            return Spec.ofDependency(alias, value).isVersionSpec();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // the catalog's mapping keys: pnpm writes the default catalog where it is, `catalog` when it is neither place
    private List<String> yamlPath(String catalog) {
        if (!DEFAULT.equals(catalog)) {
            return Arrays.asList("catalogs", catalog);
        }
        Yaml.Mapping root = yamlRoot((Yaml.Documents) document);
        if (packageManager == PackageManager.Pnpm && root != null && mapping(root, "catalog") == null) {
            Yaml.Mapping named = mapping(root, "catalogs");
            if (named != null && mapping(named, "default") != null) {
                return Arrays.asList("catalogs", "default");
            }
        }
        return singletonList("catalog");
    }

    private static Yaml.@Nullable Mapping yamlRoot(Yaml.Documents docs) {
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
                return entry.getValue() instanceof Yaml.Mapping ? (Yaml.Mapping) entry.getValue() : null;
            }
        }
        return null;
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

    // `mapping` with `alias: specifier` under the keys path[depth..], each created where missing
    private static Yaml.Mapping put(Yaml.Mapping mapping, List<String> path, int depth, String alias, String specifier,
                                    String unit) {
        String indent = depth == 0 ? "" : repeat(unit, depth);
        if (depth == path.size()) {
            for (int i = 0; i < mapping.getEntries().size(); i++) {
                Yaml.Mapping.Entry entry = mapping.getEntries().get(i);
                if (alias.equals(entry.getKey().getValue())) {
                    List<Yaml.Mapping.Entry> entries = new ArrayList<>(mapping.getEntries());
                    entries.set(i, entry.withValue(scalar(" ", specifier)));
                    return mapping.withEntries(entries);
                }
            }
            return insert(mapping, newEntry(indent, alias, scalar(" ", specifier)), false);
        }
        String key = path.get(depth);
        for (int i = 0; i < mapping.getEntries().size(); i++) {
            Yaml.Mapping.Entry entry = mapping.getEntries().get(i);
            if (key.equals(entry.getKey().getValue()) && entry.getValue() instanceof Yaml.Mapping) {
                List<Yaml.Mapping.Entry> entries = new ArrayList<>(mapping.getEntries());
                entries.set(i, entry.withValue(put((Yaml.Mapping) entry.getValue(), path, depth + 1, alias,
                        specifier, unit)));
                return mapping.withEntries(entries);
            }
        }
        Yaml.Mapping child = put(new Yaml.Mapping(randomId(), Markers.EMPTY, null, emptyList(), null, null, null),
                path, depth + 1, alias, specifier, unit);
        return insert(mapping, newEntry(indent, key, child), depth == 0);
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

    // parsePkgAndParentSelector's target package of an override key: `a>b@1` -> b
    private static String overrideTarget(String selector) {
        String target = selector.substring(selector.lastIndexOf('>') + 1);
        int at = target.indexOf('@', target.startsWith("@") ? 1 : 0);
        return at < 0 ? target : target.substring(0, at);
    }

    // Bun's Package.parse: the workspaces object when it declares a catalog, else the root when it has workspaces
    private static Json.@Nullable JsonObject bunBase(Json.Document document) {
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

    private static List<String> bunPath(Json.Document document, String catalog) {
        Json.JsonObject base = bunBase(document);
        List<String> path = new ArrayList<>();
        if (base != document.getValue()) {
            path.add("workspaces");
        }
        if (DEFAULT.equals(catalog)) {
            path.add("catalog");
        } else {
            path.add("catalogs");
            path.add(catalog);
        }
        return path;
    }

    private static Map<String, Map<String, String>> bunCatalogs(Json.Document document) {
        Map<String, Map<String, String>> catalogs = new LinkedHashMap<>();
        Json.JsonObject base = bunBase(document);
        if (base == null) {
            return catalogs;
        }
        Json catalog = member(base, "catalog");
        if (catalog instanceof Json.JsonObject) {
            catalogs.put(DEFAULT, strings((Json.JsonObject) catalog));
        }
        Json named = member(base, "catalogs");
        if (named instanceof Json.JsonObject) {
            for (Json m : ((Json.JsonObject) named).getMembers()) {
                if (m instanceof Json.Member && ((Json.Member) m).getValue() instanceof Json.JsonObject) {
                    String name = string(((Json.Member) m).getKey());
                    if (name != null) {
                        catalogs.put(name, strings((Json.JsonObject) ((Json.Member) m).getValue()));
                    }
                }
            }
        }
        return catalogs;
    }

    private static @Nullable Json member(Json.JsonObject object, String key) {
        for (Json m : object.getMembers()) {
            if (m instanceof Json.Member && key.equals(string(((Json.Member) m).getKey()))) {
                return ((Json.Member) m).getValue();
            }
        }
        return null;
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

    private static @Nullable String string(@Nullable Object node) {
        if (node instanceof Json.Literal && ((Json.Literal) node).getValue() instanceof String) {
            return (String) ((Json.Literal) node).getValue();
        }
        return null;
    }

    /**
     * A dependency recipe's scan with the catalogs its projects read, read once a cycle, and the edits this cycle
     * decides on them.
     */
    public static final class Accumulator {
        public final NodeDependencyScan.Accumulator scan = new NodeDependencyScan.Accumulator();
        public final Map<Path, PackageJsonHelper.EditAndRegenerateResult> results = new LinkedHashMap<>();
        public final Set<Object> rows = new HashSet<>();

        /**
         * The manifest edit this cycle decides for each package.json, by its path.
         */
        public final Map<Path, Function<Json.Document, Json.Document>> manifestEdits = new HashMap<>();

        /**
         * The messages of the edits this cycle declines, by the package.json they leave unchanged.
         */
        public final Map<Path, Set<String>> declines = new HashMap<>();
        private final Map<Path, SourceFile> files = new HashMap<>();
        private final Map<Path, NodeCatalogs> read = new HashMap<>();
        private final Map<Path, List<UnaryOperator<NodeCatalogs>>> edits = new LinkedHashMap<>();

        /**
         * Of a lock's {@code importers}, the one regenerated last this cycle, whose lock holds every edit made before.
         */
        public @Nullable Path lastRegenerated(List<Path> importers) {
            Path last = null;
            for (Map.Entry<Path, PackageJsonHelper.EditAndRegenerateResult> result : results.entrySet()) {
                if (importers.contains(result.getKey()) && result.getValue().getRegenResult() != null) {
                    last = result.getKey();
                }
            }
            return last;
        }

        /**
         * Note a file the scan visits that holds catalogs: pnpm-workspace.yaml or .yarnrc.yml.
         */
        public void capture(SourceFile sf) {
            String name = String.valueOf(sf.getSourcePath().getFileName());
            if (sf instanceof Yaml.Documents && (name.equals(fileName(PackageManager.Pnpm)) ||
                                                name.equals(fileName(PackageManager.YarnBerry)))) {
                files.put(sf.getSourcePath(), sf);
            }
        }

        /**
         * The catalogs the package manager of {@code packageJson} reads for it, as this cycle's edits leave them.
         */
        public @Nullable NodeCatalogs of(Path packageJson) {
            PackageManager pm = packageManager(packageJson);
            Path file = pm == null ? null : file(pm, packageJson);
            if (pm == null || file == null) {
                return null;
            }
            NodeCatalogs catalogs = read.computeIfAbsent(file, f -> {
                SourceFile document = pm == PackageManager.Bun ? captured(f) : files.get(f);
                return document == null ? null : read(pm, document);
            });
            if (catalogs == null) {
                return null;
            }
            for (UnaryOperator<NodeCatalogs> edit : edits.getOrDefault(file, emptyList())) {
                catalogs = edit.apply(catalogs);
            }
            return catalogs;
        }

        /**
         * Keep an edit decided on the catalogs of {@code packageJson}; it applies to every later read of them, and
         * one that cannot apply to a later revision leaves it as it is.
         */
        public void edit(Path packageJson, Function<NodeCatalogs, @Nullable NodeCatalogs> edit) {
            NodeCatalogs catalogs = of(packageJson);
            if (catalogs != null) {
                edits.computeIfAbsent(catalogs.getPath(), k -> new ArrayList<>()).add(c -> {
                    NodeCatalogs edited = edit.apply(c);
                    return edited == null ? c : edited;
                });
            }
        }

        /**
         * The catalog files this cycle edits that a regeneration for {@code packageJson} reads, by path; Bun's root
         * manifest, as edited so far, when {@code packageJson} is another.
         */
        public Map<Path, String> editedFiles(ExecutionContext ctx, Path packageJson) {
            NodeCatalogs catalogs = of(packageJson);
            if (catalogs == null || !edits.containsKey(catalogs.getPath()) ||
                catalogs.getPath().equals(packageJson)) {
                return Collections.emptyMap();
            }
            SourceFile live = catalogs.getDocument() instanceof Json.Document ?
                    PackageJsonHelper.getLiveTree(ctx, catalogs.getPath()) : null;
            if (live instanceof Json.Document) {
                return Collections.singletonMap(catalogs.getPath(), withCatalogEdits((Json.Document) live)
                        .printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED)));
            }
            return Collections.singletonMap(catalogs.getPath(), catalogs.print());
        }

        /**
         * {@code visited} as this cycle's catalog edits leave it, when it is a catalog file other than a manifest.
         */
        public @Nullable SourceFile editedFile(SourceFile visited) {
            if (!files.containsKey(visited.getSourcePath()) || !edits.containsKey(visited.getSourcePath())) {
                return null;
            }
            PackageManager pm = String.valueOf(visited.getSourcePath().getFileName())
                    .equals(fileName(PackageManager.Pnpm)) ? PackageManager.Pnpm : PackageManager.YarnBerry;
            NodeCatalogs catalogs = read(pm, visited);
            if (catalogs == null) {
                return null;
            }
            for (UnaryOperator<NodeCatalogs> edit : edits.get(visited.getSourcePath())) {
                catalogs = edit.apply(catalogs);
            }
            return catalogs.getDocument() == visited ? null : catalogs.getDocument();
        }

        /**
         * {@code doc} with this cycle's catalog edits when it is the Bun root manifest holding them.
         */
        public Json.Document withCatalogEdits(Json.Document doc) {
            List<UnaryOperator<NodeCatalogs>> edited = edits.get(doc.getSourcePath());
            NodeCatalogs catalogs = edited == null ? null : read(PackageManager.Bun, doc);
            if (catalogs == null) {
                return doc;
            }
            for (UnaryOperator<NodeCatalogs> edit : edited) {
                catalogs = edit.apply(catalogs);
            }
            return (Json.Document) catalogs.getDocument();
        }

        /**
         * Whether this cycle edits catalogs that {@code packageJson}'s package manager reads for it.
         */
        public boolean editsCatalogsOf(Path packageJson) {
            NodeCatalogs catalogs = of(packageJson);
            return catalogs != null && edits.containsKey(catalogs.getPath());
        }

        /**
         * The declarations of every project the scan holds under {@code root}'s directory, as edited so far.
         */
        public List<Map<String, String>> declarations(ExecutionContext ctx, Path root,
                                                      Map<Path, SourceFile> edited) {
            Path dir = root.getParent();
            List<Map<String, String>> out = new ArrayList<>();
            for (Map.Entry<Path, NodeDependencyScan.ProjectState> project : scan.projects.entrySet()) {
                if (project.getValue().capturedPackageJson == null ||
                    dir != null && !project.getKey().startsWith(dir)) {
                    continue;
                }
                SourceFile current = edited.get(project.getKey());
                if (current == null) {
                    current = PackageJsonHelper.getLiveTree(ctx, project.getKey());
                }
                if (current == null) {
                    current = project.getValue().capturedPackageJson;
                }
                for (Map<String, String> scope : PackageJsonHelper.declaredDependencies(current).values()) {
                    out.add(scope);
                }
            }
            return out;
        }

        /**
         * The package manager of {@code packageJson}: its marker's, else the one a manifest at or above names in
         * {@code packageManager} or {@code devEngines.packageManager}, else the one whose catalog files it lies under.
         */
        public @Nullable PackageManager packageManager(Path packageJson) {
            NodeDependencyScan.ProjectState ps = scan.projects.get(packageJson);
            NodeResolutionResult marker = ps == null || ps.capturedPackageJson == null ? null :
                    ps.capturedPackageJson.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
            if (marker != null && marker.getPackageManager() != null) {
                return marker.getPackageManager();
            }
            for (Path dir = packageJson.getParent(); ; dir = dir.getParent()) {
                SourceFile manifest = captured(dir == null ? Paths.get("package.json") : dir.resolve("package.json"));
                PackageManager named = manifest == null ? null : named(manifest);
                if (named != null) {
                    return named;
                }
                if (dir == null) {
                    break;
                }
            }
            if (file(PackageManager.Pnpm, packageJson) != null) {
                return PackageManager.Pnpm;
            }
            if (file(PackageManager.YarnBerry, packageJson) != null) {
                return PackageManager.YarnBerry;
            }
            Path bun = file(PackageManager.Bun, packageJson);
            SourceFile root = bun == null ? null : captured(bun);
            NodeCatalogs catalogs = root == null ? null : read(PackageManager.Bun, root);
            return catalogs != null && !catalogs.getCatalogs().isEmpty() ? PackageManager.Bun : null;
        }

        // the nearest file at or above the manifest's directory that pm reads catalogs from
        private @Nullable Path file(PackageManager pm, Path packageJson) {
            String name = fileName(pm);
            if (name == null) {
                return null;
            }
            for (Path dir = packageJson.getParent(); ; dir = dir.getParent()) {
                Path candidate = dir == null ? Paths.get(name) : dir.resolve(name);
                if (pm == PackageManager.Bun) {
                    SourceFile manifest = captured(candidate);
                    if (manifest instanceof Json.Document && bunBase((Json.Document) manifest) != null) {
                        return candidate;
                    }
                } else if (files.containsKey(candidate)) {
                    return candidate;
                }
                if (dir == null) {
                    return null;
                }
            }
        }

        private @Nullable SourceFile captured(Path packageJson) {
            NodeDependencyScan.ProjectState ps = scan.projects.get(packageJson);
            return ps == null ? null : ps.capturedPackageJson;
        }

        private static @Nullable PackageManager named(SourceFile manifest) {
            if (!(manifest instanceof Json.Document) || !(((Json.Document) manifest).getValue() instanceof
                    Json.JsonObject)) {
                return null;
            }
            Json.JsonObject root = (Json.JsonObject) ((Json.Document) manifest).getValue();
            String spec = string(member(root, "packageManager"));
            Json devEngines = member(root, "devEngines");
            Json declared = devEngines instanceof Json.JsonObject ?
                    member((Json.JsonObject) devEngines, "packageManager") : null;
            if (spec == null && declared instanceof Json.JsonObject) {
                String name = string(member((Json.JsonObject) declared, "name"));
                String version = string(member((Json.JsonObject) declared, "version"));
                spec = name == null ? null : name + "@" + (version == null ? "" : version);
            }
            if (spec == null) {
                return null;
            }
            if (spec.startsWith("pnpm@")) {
                return PackageManager.Pnpm;
            }
            if (spec.startsWith("bun@")) {
                return PackageManager.Bun;
            }
            if (spec.startsWith("npm@")) {
                return PackageManager.Npm;
            }
            if (spec.startsWith("yarn@")) {
                return spec.startsWith("yarn@1.") || "yarn@1".equals(spec) ? PackageManager.YarnClassic :
                        PackageManager.YarnBerry;
            }
            return null;
        }
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
}
