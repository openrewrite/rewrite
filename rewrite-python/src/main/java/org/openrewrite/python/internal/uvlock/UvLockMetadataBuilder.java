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
package org.openrewrite.python.internal.uvlock;

import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.LockFileRegeneration.Reason;
import org.openrewrite.python.internal.index.UvIndex;
import org.openrewrite.python.internal.pep440.PythonVersionSpecifier;
import org.openrewrite.python.internal.pep440.PythonVersionSpecifierSet;
import org.openrewrite.python.internal.pep508.Marker;
import org.openrewrite.python.internal.pep508.Pep508Requirement;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import static java.util.Collections.emptyList;
import static java.util.Collections.sort;

/**
 * Rebuilds the root package's {@code [package.metadata]} (requires-dist, provides-extras, and
 * requires-dev) from the edited {@code pyproject.toml}, reproducing uv's recorded normalization.
 * The result is what {@link UvLockEngine} diffs against the lock's recorded metadata.
 */
final class UvLockMetadataBuilder {

    private final Toml.Document pyproject;
    private final UvLockMetadata oldMetadata;
    private final List<UvIndex> indexes;
    private final Map<String, List<String>> indexPins;
    private final Set<String> nonRegistrySources;

    UvLockMetadataBuilder(Toml.Document pyproject, UvLockMetadata oldMetadata, List<UvIndex> indexes,
                          Map<String, List<String>> indexPins, Set<String> nonRegistrySources) {
        this.pyproject = pyproject;
        this.oldMetadata = oldMetadata;
        this.indexes = indexes;
        this.indexPins = indexPins;
        this.nonRegistrySources = nonRegistrySources;
    }

    /** A dependency declared in the manifest, before it is resolved into a lock requirement. */
    private static final class Declared {
        final String canonicalName;
        final Pep508Requirement requirement;
        final @Nullable String rawMarker;
        final @Nullable String extraGroup;
        final @Nullable String devGroup;

        Declared(Pep508Requirement requirement, @Nullable String rawMarker,
                 @Nullable String extraGroup, @Nullable String devGroup) {
            this.canonicalName = requirement.getCanonicalName();
            this.requirement = requirement;
            this.rawMarker = rawMarker;
            this.extraGroup = extraGroup;
            this.devGroup = devGroup;
        }
    }

    UvLockMetadata build(Toml.Table project) {
        return buildMetadata(readDeclarations(project), readProvidesExtras());
    }

    /**
     * Package names whose {@code [tool.uv.sources]} entry pins something other than a
     * named index (git/path/workspace/editable).
     */
    static Set<String> readNonRegistrySources(Toml.Document pyproject) {
        Set<String> nonRegistry = new HashSet<>();
        Toml.Table sources = UvLockToml.findTable(pyproject, "tool.uv.sources");
        for (Toml sourceValue : sources != null ? sources.getValues() : Collections.<Toml>emptyList()) {
            if (sourceValue instanceof Toml.KeyValue) {
                Toml.KeyValue kv = (Toml.KeyValue) sourceValue;
                String pkg = UvLockToml.keyName(kv);
                if (pkg != null && !UvLockToml.hasIndexKey(kv.getValue())) {
                    nonRegistry.add(Pep508Requirement.canonicalize(pkg));
                }
            }
        }
        for (TomlValue value : pyproject.getValues()) {
            if (!(value instanceof Toml.Table)) {
                continue;
            }
            Toml.Table table = (Toml.Table) value;
            String name = UvLockToml.tableName(table);
            if (name != null && name.startsWith("tool.uv.sources.")) {
                String pkg = name.substring("tool.uv.sources.".length());
                if (UvLockToml.literalString(table, "index") == null) {
                    nonRegistry.add(Pep508Requirement.canonicalize(UvLockToml.unquote(pkg)));
                }
            }
        }
        return nonRegistry;
    }

    private List<Declared> readDeclarations(Toml.Table project) {
        List<Declared> declared = new ArrayList<>();
        for (String spec : UvLockToml.stringArray(project, "dependencies")) {
            declared.add(toDeclared(spec, null, null));
        }
        Toml.Table optional = UvLockToml.findTable(pyproject, "project.optional-dependencies");
        if (optional != null) {
            for (Toml value : optional.getValues()) {
                if (!(value instanceof Toml.KeyValue)) {
                    continue;
                }
                Toml.KeyValue kv = (Toml.KeyValue) value;
                String extra = UvLockToml.keyName(kv);
                if (extra == null || !(kv.getValue() instanceof Toml.Array)) {
                    throw new EngineFailure(Reason.MALFORMED_MANIFEST, null,
                            "Unreadable [project.optional-dependencies] entry");
                }
                for (String spec : UvLockToml.stringArrayValues((Toml.Array) kv.getValue(), "optional-dependencies")) {
                    declared.add(toDeclared(spec, Pep508Requirement.canonicalize(extra), null));
                }
            }
        }
        Toml.Table groups = UvLockToml.findTable(pyproject, "dependency-groups");
        if (groups != null) {
            for (Map.Entry<String, List<String>> group : UvLockToml.dependencyGroups(groups).entrySet()) {
                for (String spec : group.getValue()) {
                    declared.add(toDeclared(spec, null, group.getKey()));
                }
            }
        }
        // uv locks the legacy [tool.uv] dev-dependencies list as the dev group
        Toml.Table uv = UvLockToml.findTable(pyproject, "tool.uv");
        if (uv != null) {
            for (String spec : UvLockToml.stringArray(uv, "dev-dependencies")) {
                declared.add(toDeclared(spec, null, "dev"));
            }
        }
        return declared;
    }

    private Declared toDeclared(String spec, @Nullable String extraGroup, @Nullable String devGroup) {
        Pep508Requirement requirement = Pep508Requirement.parse(spec);
        if (requirement == null) {
            throw new EngineFailure(Reason.MALFORMED_MANIFEST, null,
                    "Unparseable dependency declaration: " + spec);
        }
        if (requirement.getUrl() != null && !requirement.getUrl().startsWith("git+")) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, requirement.getCanonicalName(),
                    "Direct URL dependencies are not supported by the native engine: " + spec);
        }
        int semi = spec.indexOf(';');
        String rawMarker = requirement.getMarker() != null && semi >= 0 ?
                spec.substring(semi + 1).trim() : null;
        return new Declared(requirement, rawMarker, extraGroup, devGroup);
    }

    private List<String> readProvidesExtras() {
        Toml.Table optional = UvLockToml.findTable(pyproject, "project.optional-dependencies");
        if (optional == null) {
            return emptyList();
        }
        List<String> extras = new ArrayList<>();
        for (Toml value : optional.getValues()) {
            if (value instanceof Toml.KeyValue) {
                String extra = UvLockToml.keyName((Toml.KeyValue) value);
                if (extra != null) {
                    extras.add(Pep508Requirement.canonicalize(extra));
                }
            }
        }
        return extras;
    }

    private UvLockMetadata buildMetadata(List<Declared> declared, List<String> providesExtras) {
        List<UvLockRequirement> requiresDist = new ArrayList<>();
        Map<String, List<UvLockRequirement>> requiresDev = new TreeMap<>();
        for (Declared decl : declared) {
            UvLockRequirement requirement = buildRequirement(decl);
            if (decl.devGroup != null) {
                List<UvLockRequirement> group = requiresDev.computeIfAbsent(decl.devGroup,
                        k -> new ArrayList<>());
                group.add(requirement);
            } else {
                requiresDist.add(requirement);
            }
        }
        // Declared dev groups record into requires-dev even when empty
        Toml.Table groups = UvLockToml.findTable(pyproject, "dependency-groups");
        if (groups != null) {
            for (Toml value : groups.getValues()) {
                if (value instanceof Toml.KeyValue) {
                    String group = UvLockToml.keyName((Toml.KeyValue) value);
                    if (group != null) {
                        requiresDev.computeIfAbsent(Pep508Requirement.canonicalize(group), k -> new ArrayList<>());
                    }
                }
            }
        }
        Toml.Table uv = UvLockToml.findTable(pyproject, "tool.uv");
        if (uv != null && UvLockToml.hasKey(uv, "dev-dependencies")) {
            requiresDev.computeIfAbsent("dev", k -> new ArrayList<>());
        }
        requiresDist.sort(Comparator.comparing(UvLockRequirement::getName));
        for (List<UvLockRequirement> group : requiresDev.values()) {
            group.sort(Comparator.comparing(UvLockRequirement::getName));
        }
        stabilizeDuplicates(requiresDist, oldMetadata.getRequiresDist());
        for (Map.Entry<String, List<UvLockRequirement>> group : requiresDev.entrySet()) {
            stabilizeDuplicates(group.getValue(), oldMetadata.getRequiresDev() != null ?
                    oldMetadata.getRequiresDev().get(group.getKey()) : null);
        }
        return new UvLockMetadata(
                requiresDist.isEmpty() ? null : requiresDist,
                providesExtras.isEmpty() ? null : providesExtras,
                requiresDev.isEmpty() ? null : new LinkedHashMap<>(requiresDev));
    }

    /**
     * uv's tie-break for same-name (marker-differentiated) entries is not declaration
     * order; when a run of duplicates matches the old recording as a multiset, keep the
     * old order so an untouched forked declaration round-trips byte-identically.
     */
    private static void stabilizeDuplicates(List<UvLockRequirement> entries,
                                            @Nullable List<UvLockRequirement> oldSection) {
        if (oldSection == null) {
            return;
        }
        int i = 0;
        while (i < entries.size()) {
            int end = i + 1;
            while (end < entries.size() && entries.get(end).getName().equals(entries.get(i).getName())) {
                end++;
            }
            if (end - i > 1) {
                List<UvLockRequirement> oldRun = new ArrayList<>();
                for (UvLockRequirement old : oldSection) {
                    if (old.getName().equals(entries.get(i).getName())) {
                        oldRun.add(old);
                    }
                }
                List<String> newRendered = UvLockRequirements.rendered(entries.subList(i, end));
                List<String> oldRendered = UvLockRequirements.rendered(oldRun);
                List<String> newSorted = new ArrayList<>(newRendered);
                List<String> oldSorted = new ArrayList<>(oldRendered);
                sort(newSorted);
                sort(oldSorted);
                if (newSorted.equals(oldSorted)) {
                    for (int j = 0; j < oldRun.size(); j++) {
                        entries.set(i + j, oldRun.get(j));
                    }
                }
            }
            i = end;
        }
    }

    private UvLockRequirement buildRequirement(Declared decl) {
        String url = decl.requirement.getUrl();
        List<String> extras = null;
        if (!decl.requirement.getExtras().isEmpty()) {
            extras = new ArrayList<>();
            for (String extra : decl.requirement.getExtras()) {
                extras.add(Pep508Requirement.canonicalize(extra));
            }
        }
        if (url != null) {
            String git = recordedGit(url);
            UvLockRequirement old = findOldRequirement(decl);
            if (old != null && old.getGit() != null && (git == null ? sameGit(old.getGit(), url) : git.equals(old.getGit()))) {
                return withDeclaredExtrasAndMarker(old, extras, decl);
            }
            if (git == null) {
                throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, decl.canonicalName,
                        "Recording the git dependency " + url + " is not supported by the native engine");
            }
            return new UvLockRequirement(decl.canonicalName, extras, null, resolveMetadataMarker(decl), null, null,
                    null, git, null, null);
        }
        if (nonRegistrySources.contains(decl.canonicalName)) {
            UvLockRequirement old = findOldRequirement(decl);
            if (old == null) {
                throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, decl.canonicalName,
                        "Adding a git/path/editable dependency is not supported by the native engine");
            }
            return withDeclaredExtrasAndMarker(old, extras, decl);
        }
        List<String> pins = indexPins.get(decl.canonicalName);
        if (pins != null && new HashSet<>(pins).size() > 1) {
            UvLockRequirement old = findOldRequirement(decl);
            if (old != null) {
                return old;
            }
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, decl.canonicalName,
                    "Marker-gated multi-index pins are not supported by the native engine");
        }
        String specifier = normalizeSpecifier(decl);
        String index = pins == null ? null : declaredIndexUrl(pins.get(0), decl.canonicalName);
        String marker = resolveMetadataMarker(decl);
        return new UvLockRequirement(decl.canonicalName, extras, null, marker, specifier, index, null, null, null, null);
    }

    // a source the lock records keeps its recorded form, while the declaration's extras and marker still count
    private UvLockRequirement withDeclaredExtrasAndMarker(UvLockRequirement old, @Nullable List<String> extras,
                                                          Declared decl) {
        String marker = resolveMetadataMarker(decl);
        return Objects.equals(extras, old.getExtras()) && Objects.equals(marker, old.getMarker()) ? old :
                old.withExtras(extras).withMarker(marker);
    }

    private @Nullable String normalizeSpecifier(Declared decl) {
        return normalizeSpecifier(decl.requirement);
    }

    /** uv's requires-dist rendering of a requirement's specifier set, or null when it accepts any version. */
    static @Nullable String normalizeSpecifier(Pep508Requirement requirement) {
        PythonVersionSpecifierSet specifiers = requirement.getSpecifiers();
        if (specifiers == null || specifiers.isMatchAll()) {
            return null;
        }
        StringBuilder joined = new StringBuilder();
        for (PythonVersionSpecifier spec : specifiers.getSpecifiers()) {
            if (joined.length() > 0) {
                joined.append(',');
            }
            joined.append(spec.getOperator()).append(spec.getVersion());
        }
        try {
            return UvLockNormalization.normalizeRequiresDistSpecifier(joined.toString());
        } catch (IllegalArgumentException e) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, requirement.getCanonicalName(),
                    "Cannot normalize version specifier " + joined + ": " + e.getMessage());
        }
    }

    private String declaredIndexUrl(String indexName, String pkg) {
        for (UvIndex index : indexes) {
            if (index.isNamed() && indexName.equals(index.getIndex().getName())) {
                return index.getIndex().getUrl();
            }
        }
        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                "Index " + indexName + " pinned via [tool.uv.sources] is not declared in configuration");
    }

    /**
     * uv's recorded marker form for a declaration. When the declared marker is outside
     * the normalization subset the empirical catalog covers, fall back to the old
     * recorded form when it is provably the same declaration; otherwise fail.
     */
    private @Nullable String resolveMetadataMarker(Declared decl) {
        String extraClause = decl.extraGroup != null ? "extra == '" + decl.extraGroup + "'" : null;
        if (decl.rawMarker == null) {
            return extraClause;
        }
        String normalized = UvLockMarkers.recordMarker(decl.rawMarker, extraClause);
        if (normalized != null) {
            return normalized;
        }
        UvLockRequirement old = findOldRequirement(decl);
        if (old != null && old.getMarker() != null && markersLooselyEqual(decl, old.getMarker())) {
            return old.getMarker();
        }
        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, decl.canonicalName,
                "Cannot reproduce uv's recorded form of marker \"" + decl.rawMarker + "\"; " +
                        "marker normalization outside the verified subset requires resolution");
    }

    /**
     * Whether the declared marker (plus any extra gate) is semantically the marker uv
     * recorded, modulo spacing/quoting, so the recorded form can be copied verbatim.
     */
    private boolean markersLooselyEqual(Declared decl, String recorded) {
        String declaredText = decl.extraGroup != null ?
                "(" + decl.rawMarker + ") and extra == '" + decl.extraGroup + "'" : decl.rawMarker;
        Marker declaredMarker = Marker.parse(declaredText);
        Marker recordedMarker = Marker.parse(recorded);
        return declaredMarker != null && declaredMarker.equals(recordedMarker);
    }

    /**
     * uv's recorded form of a {@code git+} URL: the repository URL with its revision and subdirectory as query
     * parameters. Null when it carries both, whose order uv's form leaves open here.
     */
    static @Nullable String recordedGit(String url) {
        String rest = url.substring("git+".length());
        String subdirectory = null;
        int hash = rest.indexOf('#');
        if (hash >= 0) {
            for (String part : rest.substring(hash + 1).split("&")) {
                if (part.startsWith("subdirectory=")) {
                    subdirectory = part.substring("subdirectory=".length());
                } else if (!part.startsWith("egg=")) {
                    return null;
                }
            }
            rest = rest.substring(0, hash);
        }
        String rev = null;
        int at = rest.lastIndexOf('@');
        int hostEnd = rest.indexOf('/', rest.indexOf("://") + 3);
        if (at > hostEnd && hostEnd > 0) {
            rev = rest.substring(at + 1);
            rest = rest.substring(0, at);
            // uv percent-encodes a revision's slashes, a form not reproduced here
            if (rev.indexOf('/') >= 0) {
                return null;
            }
        }
        if (rev != null && subdirectory != null) {
            return null;
        }
        return rev != null ? rest + "?rev=" + rev : subdirectory != null ? rest + "?subdirectory=" + subdirectory : rest;
    }

    // the recorded form names the same repository, revision and subdirectory, in whichever order
    private static boolean sameGit(String recorded, String url) {
        String rest = url.substring("git+".length());
        int hash = rest.indexOf('#');
        String base = hash >= 0 ? rest.substring(0, hash) : rest;
        int at = base.lastIndexOf('@');
        String repository = at > base.indexOf("://") + 3 && base.indexOf('/', at) < 0 ? base.substring(0, at) : base;
        int query = recorded.indexOf('?');
        if (!(query >= 0 ? recorded.substring(0, query) : recorded).equals(repository)) {
            return false;
        }
        Set<String> params = new HashSet<>(Arrays.asList((query >= 0 ? recorded.substring(query + 1) : "").split("&")));
        if (at > base.indexOf("://") + 3 && base.indexOf('/', at) < 0) {
            if (!params.remove("rev=" + base.substring(at + 1))) {
                return false;
            }
        }
        if (hash >= 0) {
            for (String part : rest.substring(hash + 1).split("&")) {
                if (part.startsWith("subdirectory=") && !params.remove(part)) {
                    return false;
                }
            }
        }
        params.remove("");
        return params.isEmpty();
    }

    private @Nullable UvLockRequirement findOldRequirement(Declared decl) {
        List<UvLockRequirement> section = decl.devGroup != null ?
                (oldMetadata.getRequiresDev() != null ? oldMetadata.getRequiresDev().get(decl.devGroup) : null) :
                oldMetadata.getRequiresDist();
        if (section == null) {
            return null;
        }
        for (UvLockRequirement req : section) {
            if (req.getName().equals(decl.canonicalName) &&
                    Objects.equals(decl.extraGroup, UvLockRequirements.extraGroupOf(req))) {
                return req;
            }
        }
        return null;
    }
}
