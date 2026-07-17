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
package org.openrewrite.python.internal.pipfilelock;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.python.PythonPackageIndex;
import org.openrewrite.python.internal.Hashing;
import org.openrewrite.python.internal.LockFileRegeneration.Failure;
import org.openrewrite.python.internal.LockFileRegeneration.Reason;
import org.openrewrite.python.internal.LockFileRegeneration.Result;
import org.openrewrite.python.internal.index.DistFilename;
import org.openrewrite.python.internal.index.Environment;
import org.openrewrite.python.internal.index.IndexDiscovery;
import org.openrewrite.python.internal.index.PackageFile;
import org.openrewrite.python.internal.index.PackageListing;
import org.openrewrite.python.internal.index.PypiJsonApi;
import org.openrewrite.python.internal.index.PythonIndexException;
import org.openrewrite.python.internal.index.SimpleIndexClient;
import org.openrewrite.python.internal.metadata.CoreMetadata;
import org.openrewrite.python.internal.metadata.LazyWheelMetadataReader;
import org.openrewrite.python.internal.metadata.Pep658MetadataFetcher;
import org.openrewrite.python.internal.metadata.SdistMetadataReader;
import org.openrewrite.python.internal.pep440.PythonVersion;
import org.openrewrite.python.internal.pep440.PythonVersionSpecifier;
import org.openrewrite.python.internal.pep440.PythonVersionSpecifierSet;
import org.openrewrite.python.internal.pep508.Marker;
import org.openrewrite.python.internal.pep508.MarkerEnvironment;
import org.openrewrite.python.internal.pep508.Pep508Requirement;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * Native {@code Pipfile.lock} regeneration with minimal-update semantics (ADR 0009,
 * "Engine orchestration"): the old lock is the old state, the edited Pipfile is diffed
 * against it, and only the edited packages are rewritten. Any edit whose result cannot
 * be proven correct fails loud with a structured {@link Failure}; no lock is emitted.
 */
public final class PipenvLockEngine {

    private static final Set<String> NON_CATEGORY_TABLES = new HashSet<>(Arrays.asList(
            "source", "requires", "scripts", "pipfile", "pipenv"));

    private static final Set<String> UNSUPPORTED_ENTRY_KEYS = new HashSet<>(Arrays.asList(
            "git", "hg", "svn", "bzr", "path", "file", "editable", "ref", "subdirectory"));

    private static final List<String> MARKER_KEYS = Arrays.asList(
            "os_name", "sys_platform", "platform_machine");

    private PipenvLockEngine() {
    }

    public static Result regenerate(String pipfileContent, @Nullable String oldLockContent, ExecutionContext ctx) {
        if (oldLockContent == null) {
            return Result.failure(new Failure(Reason.RESOLUTION_REQUIRED, null, null,
                    "No existing Pipfile.lock; locking from scratch requires full dependency resolution, " +
                            "which the native engine does not yet support"));
        }

        Map<String, Object> lock;
        try {
            lock = PipfileLockWriter.read(oldLockContent);
        } catch (IllegalArgumentException e) {
            return Result.failure(new Failure(Reason.MALFORMED_LOCK, null, null,
                    "Existing Pipfile.lock could not be parsed: " + e.getMessage()));
        }

        Toml.Document pipfile = parsePipfile(pipfileContent);
        if (pipfile == null) {
            return Result.failure(new Failure(Reason.MALFORMED_MANIFEST, null, null,
                    "Edited Pipfile could not be parsed as TOML"));
        }

        try {
            return new Run(pipfile, lock, oldLockContent, ctx).regenerate();
        } catch (PythonIndexException e) {
            return Result.failure(indexFailure(null, e));
        }
    }

    private static Toml.@Nullable Document parsePipfile(String pipfileContent) {
        // ANTLR error recovery can still yield a Document for broken TOML; treat any syntax error as malformed
        boolean[] failed = new boolean[1];
        SourceFile parsed = new TomlParser()
                .parse(new InMemoryExecutionContext(t -> failed[0] = true), pipfileContent)
                .findFirst()
                .orElse(null);
        return !failed[0] && parsed instanceof Toml.Document ? (Toml.Document) parsed : null;
    }

    private static Failure indexFailure(@Nullable String packageName, PythonIndexException e) {
        Reason reason;
        switch (e.getReason()) {
            case AUTH_FAILED:
                reason = Reason.AUTH_FAILED;
                break;
            case NOT_FOUND:
                reason = Reason.PACKAGE_NOT_FOUND;
                break;
            default:
                reason = Reason.INDEX_UNREACHABLE;
        }
        return new Failure(reason, packageName, e.getIndexUrl(), e.getMessage());
    }

    /**
     * A Pipfile dependency entry: either a bare constraint string or an inline table.
     */
    private static class PipfileEntry {
        final String keyAsWritten;
        String constraint = "*";
        List<String> extras = emptyList();
        @Nullable String markers;
        final Map<String, String> markerKeys = new LinkedHashMap<>();
        @Nullable String indexName;
        boolean inlineTable;
        boolean unsupported;

        PipfileEntry(String keyAsWritten) {
            this.keyAsWritten = keyAsWritten;
        }
    }

    private static class Edit {
        enum Kind {
            CHANGE, ATTRIBUTES, REMOVAL
        }

        final String lockCategory;
        final PipfileEntry entry;
        final @Nullable String lockKey;
        final Kind kind;

        Edit(String lockCategory, PipfileEntry entry, @Nullable String lockKey, Kind kind) {
            this.lockCategory = lockCategory;
            this.entry = entry;
            this.lockKey = lockKey;
            this.kind = kind;
        }
    }

    private static final class Run {
        private final Toml.Document pipfile;
        private final Map<String, Object> lock;
        private final String newline;
        private final HttpSender http;
        private final SimpleIndexClient client;

        private final Map<String, Object> lockMeta;
        private final @Nullable List<Map<String, Object>> lockMetaSources;
        private final List<PythonPackageIndex> indexes;
        private final MarkerEnvironment env;
        private final @Nullable PythonVersion lockPython;

        @SuppressWarnings("unchecked")
        Run(Toml.Document pipfile, Map<String, Object> lock, String oldLockContent, ExecutionContext ctx) {
            this.pipfile = pipfile;
            this.lock = lock;
            this.newline = oldLockContent.contains("\r\n") ? "\r\n" : "\n";
            this.http = HttpSenderExecutionContextView.view(ctx).getHttpSender();
            this.client = new SimpleIndexClient(http);

            Object meta = lock.get("_meta");
            this.lockMeta = meta instanceof Map ? (Map<String, Object>) meta : emptyMap();
            Object sources = lockMeta.get("sources");
            this.lockMetaSources = sources instanceof List ? (List<Map<String, Object>>) sources : null;
            this.indexes = IndexDiscovery.discover(ctx, pipfile, lockMetaSources, Environment.SYSTEM);

            Map<String, Object> pipfileRequires = tableToMap(findTable("requires"));
            Object lockRequires = lockMeta.get("requires");
            Map<String, Object> oldRequires = lockRequires instanceof Map ?
                    (Map<String, Object>) lockRequires : emptyMap();
            String pythonVersion = stringOrNull(pipfileRequires.get("python_version"));
            String pythonFullVersion = stringOrNull(pipfileRequires.get("python_full_version"));
            if (pythonVersion == null && pythonFullVersion == null) {
                pythonVersion = stringOrNull(oldRequires.get("python_version"));
                pythonFullVersion = stringOrNull(oldRequires.get("python_full_version"));
            }
            this.env = MarkerEnvironment.lockDefaults(pythonVersion, pythonFullVersion);
            this.lockPython = PythonVersion.parse(pythonVersion != null ? pythonVersion : pythonFullVersion);
        }

        Result regenerate() {
            Map<String, Map<String, PipfileEntry>> pipfileCategories = readPipfileCategories();
            if (pipfileCategories == null) {
                return Result.failure(new Failure(Reason.UNSUPPORTED_ENTRY_TYPE, null, null,
                        "Pipfile contains a dependency entry the native engine cannot represent"));
            }

            List<Edit> edits = new ArrayList<>();
            Failure diffFailure = computeEdits(pipfileCategories, edits);
            if (diffFailure != null) {
                return Result.failure(diffFailure);
            }

            Failure pinFailure = validatePinsAgainstRequires(edits);
            if (pinFailure != null) {
                return Result.failure(pinFailure);
            }

            List<String> notes = new ArrayList<>();
            for (Edit edit : edits) {
                Failure failure;
                switch (edit.kind) {
                    case REMOVAL:
                        applyRemoval(edit, pipfileCategories.get(edit.lockCategory), notes);
                        failure = null;
                        break;
                    case ATTRIBUTES:
                        failure = applyAttributes(edit);
                        break;
                    default:
                        failure = applyChange(edit);
                }
                if (failure != null) {
                    return Result.failure(failure);
                }
            }

            rebuildMeta(pipfileCategories.keySet());
            String content = PipfileLockWriter.write(lock, newline);
            return Result.success(content, notes.isEmpty() ? null : String.join("; ", notes));
        }

        /**
         * Reads [packages]/[dev-packages]/custom category tables keyed by lock category name.
         * Returns null when an entry value cannot be interpreted.
         */
        private @Nullable Map<String, Map<String, PipfileEntry>> readPipfileCategories() {
            Map<String, Map<String, PipfileEntry>> categories = new LinkedHashMap<>();
            for (TomlValue value : pipfile.getValues()) {
                if (!(value instanceof Toml.Table)) {
                    continue;
                }
                Toml.Table table = (Toml.Table) value;
                Toml.Identifier name = table.getName();
                if (name == null || NON_CATEGORY_TABLES.contains(name.getName())) {
                    continue;
                }
                String lockCategory = lockCategoryName(name.getName());
                Map<String, PipfileEntry> entries = new LinkedHashMap<>();
                for (Toml inner : table.getValues()) {
                    if (!(inner instanceof Toml.KeyValue)) {
                        continue;
                    }
                    Toml.KeyValue kv = (Toml.KeyValue) inner;
                    if (!(kv.getKey() instanceof Toml.Identifier)) {
                        return null;
                    }
                    String key = ((Toml.Identifier) kv.getKey()).getName();
                    PipfileEntry entry = readEntry(key, kv.getValue());
                    if (entry == null) {
                        return null;
                    }
                    entries.put(key, entry);
                }
                categories.put(lockCategory, entries);
            }
            return categories;
        }

        private static String lockCategoryName(String pipfileTable) {
            if ("packages".equals(pipfileTable)) {
                return "default";
            }
            if ("dev-packages".equals(pipfileTable)) {
                return "develop";
            }
            return pipfileTable;
        }

        private @Nullable PipfileEntry readEntry(String key, Toml value) {
            PipfileEntry entry = new PipfileEntry(key);
            if (value instanceof Toml.Literal) {
                Object v = ((Toml.Literal) value).getValue();
                if (!(v instanceof String)) {
                    return null;
                }
                entry.constraint = (String) v;
                return entry;
            }
            if (!(value instanceof Toml.Table)) {
                return null;
            }
            entry.inlineTable = true;
            for (Toml inner : ((Toml.Table) value).getValues()) {
                if (!(inner instanceof Toml.KeyValue)) {
                    continue;
                }
                Toml.KeyValue kv = (Toml.KeyValue) inner;
                if (!(kv.getKey() instanceof Toml.Identifier)) {
                    return null;
                }
                String innerKey = ((Toml.Identifier) kv.getKey()).getName();
                if (UNSUPPORTED_ENTRY_KEYS.contains(innerKey)) {
                    entry.unsupported = true;
                } else if ("version".equals(innerKey)) {
                    String v = literalString(kv.getValue());
                    if (v == null) {
                        return null;
                    }
                    entry.constraint = v;
                } else if ("extras".equals(innerKey) && kv.getValue() instanceof Toml.Array) {
                    List<String> extras = new ArrayList<>();
                    for (Toml element : ((Toml.Array) kv.getValue()).getValues()) {
                        String extra = literalString(element);
                        if (extra != null) {
                            extras.add(extra);
                        }
                    }
                    entry.extras = extras;
                } else if ("markers".equals(innerKey)) {
                    entry.markers = literalString(kv.getValue());
                } else if (MARKER_KEYS.contains(innerKey)) {
                    String v = literalString(kv.getValue());
                    if (v != null) {
                        entry.markerKeys.put(innerKey, v);
                    }
                } else if ("index".equals(innerKey)) {
                    entry.indexName = literalString(kv.getValue());
                }
            }
            return entry;
        }

        private @Nullable Failure computeEdits(Map<String, Map<String, PipfileEntry>> pipfileCategories,
                                               List<Edit> edits) {
            for (Map.Entry<String, Map<String, PipfileEntry>> category : pipfileCategories.entrySet()) {
                String lockCategory = category.getKey();
                Map<String, Object> lockEntries = lockCategory(lockCategory);
                Map<String, String> lockKeysByCanonical = canonicalKeys(lockEntries);

                for (PipfileEntry entry : category.getValue().values()) {
                    String canonical = Pep508Requirement.canonicalize(entry.keyAsWritten);
                    String lockKey = lockKeysByCanonical.get(canonical);
                    if (lockKey == null) {
                        if (entry.unsupported) {
                            return new Failure(Reason.UNSUPPORTED_ENTRY_TYPE, entry.keyAsWritten, null,
                                    "VCS/path/file/editable entries cannot be locked by the native engine");
                        }
                        return new Failure(Reason.RESOLUTION_REQUIRED, entry.keyAsWritten, null,
                                "Package is not in the existing lock; adding a package requires delta " +
                                        "resolution, which the native engine does not yet support");
                    }
                    Map<String, Object> lockEntry = entryMap(lockEntries.get(lockKey));
                    String pinned = pinnedVersion(lockEntry);
                    if (pinned == null) {
                        if (entry.unsupported) {
                            continue; // both sides VCS/path/file/editable: pass through
                        }
                        return new Failure(Reason.UNSUPPORTED_ENTRY_TYPE, entry.keyAsWritten, null,
                                "Existing lock entry is a VCS/path/file/editable pin; re-targeting it to a " +
                                        "version constraint is not supported by the native engine");
                    }
                    if (entry.unsupported) {
                        return new Failure(Reason.UNSUPPORTED_ENTRY_TYPE, entry.keyAsWritten, null,
                                "Changing a versioned entry to a VCS/path/file/editable entry is not " +
                                        "supported by the native engine");
                    }
                    PythonVersion pinnedVersion = PythonVersion.parse(pinned);
                    if (pinnedVersion == null) {
                        return new Failure(Reason.MALFORMED_LOCK, entry.keyAsWritten, null,
                                "Locked version " + pinned + " is not a valid PEP 440 version");
                    }
                    PythonVersionSpecifierSet constraint = parseConstraint(entry.constraint);
                    if (constraint == null) {
                        return new Failure(Reason.RESOLUTION_REQUIRED, entry.keyAsWritten, null,
                                "Unparseable version constraint: " + entry.constraint);
                    }
                    if (!constraint.contains(pinnedVersion, true)) {
                        edits.add(new Edit(lockCategory, entry, lockKey, Edit.Kind.CHANGE));
                    } else if (attributesDiffer(entry, lockEntry)) {
                        edits.add(new Edit(lockCategory, entry, lockKey, Edit.Kind.ATTRIBUTES));
                    }
                }

                // Top-level entries (the only ones carrying an "index" key) that vanished
                // from the Pipfile category were removed.
                Set<String> pipfileCanonical = new HashSet<>();
                for (String key : category.getValue().keySet()) {
                    pipfileCanonical.add(Pep508Requirement.canonicalize(key));
                }
                for (Map.Entry<String, Object> lockEntry : lockEntries.entrySet()) {
                    Map<String, Object> e = entryMap(lockEntry.getValue());
                    if (e.containsKey("index") &&
                            !pipfileCanonical.contains(Pep508Requirement.canonicalize(lockEntry.getKey()))) {
                        edits.add(new Edit(lockCategory, new PipfileEntry(lockEntry.getKey()),
                                lockEntry.getKey(), Edit.Kind.REMOVAL));
                    }
                }
            }
            return null;
        }

        /**
         * When [requires].python_version changed, every remaining pin's Requires-Python
         * (from the simple listing) must admit the new Python.
         */
        private @Nullable Failure validatePinsAgainstRequires(List<Edit> edits) {
            String newPython = stringOrNull(tableToMap(findTable("requires")).get("python_version"));
            if (newPython == null) {
                return null;
            }
            Object lockRequires = lockMeta.get("requires");
            String oldPython = lockRequires instanceof Map ?
                    stringOrNull(((Map<?, ?>) lockRequires).get("python_version")) : null;
            if (newPython.equals(oldPython)) {
                return null;
            }
            PythonVersion python = PythonVersion.parse(newPython);
            if (python == null) {
                return null;
            }

            Set<String> edited = new HashSet<>();
            for (Edit edit : edits) {
                // attribute edits keep the pinned version, so it still needs validation
                if (edit.kind != Edit.Kind.ATTRIBUTES) {
                    edited.add(edit.lockCategory + "/" + edit.lockKey);
                }
            }

            List<String> violatingPackages = new ArrayList<>();
            List<String> violations = new ArrayList<>();
            for (Map.Entry<String, Object> category : lock.entrySet()) {
                if ("_meta".equals(category.getKey()) || !(category.getValue() instanceof Map)) {
                    continue;
                }
                for (Map.Entry<String, Object> lockEntry : entryMap(category.getValue()).entrySet()) {
                    if (edited.contains(category.getKey() + "/" + lockEntry.getKey())) {
                        continue;
                    }
                    Map<String, Object> e = entryMap(lockEntry.getValue());
                    String pinned = pinnedVersion(e);
                    PythonVersion pinnedVersion = PythonVersion.parse(pinned);
                    if (pinnedVersion == null) {
                        continue; // VCS/path pins are preserved as-is
                    }
                    PackageListing listing;
                    try {
                        listing = fetchListing(lockEntry.getKey(), indexName(e));
                    } catch (PythonIndexException ex) {
                        // report the true fetch failure rather than folding it into PIN_EXCLUDED_BY_PYTHON
                        return indexFailure(lockEntry.getKey(), ex);
                    }
                    List<PackageFile> files = filesOfVersion(listing, lockEntry.getKey(), pinnedVersion);
                    if (files.isEmpty()) {
                        violatingPackages.add(lockEntry.getKey());
                        violations.add(lockEntry.getKey() + " (pinned " + pinned +
                                " no longer listed by the index; Requires-Python unverifiable)");
                    } else if (!versionAdmitsPython(files, python)) {
                        violatingPackages.add(lockEntry.getKey());
                        violations.add(lockEntry.getKey() + "==" + pinned);
                    }
                }
            }
            if (!violations.isEmpty()) {
                return new Failure(Reason.PIN_EXCLUDED_BY_PYTHON,
                        violatingPackages.size() == 1 ? violatingPackages.get(0) : null,
                        null,
                        "Pinned versions incompatible with python_version " + newPython + ": " +
                                String.join(", ", violations));
            }
            return null;
        }

        private void applyRemoval(Edit edit, Map<String, PipfileEntry> pipfileEntries,
                                  List<String> notes) {
            Map<String, Object> lockEntries = lockCategory(edit.lockCategory);
            String removedCanonical = Pep508Requirement.canonicalize(edit.lockKey);
            Map<String, PipfileEntry> declaredByCanonical = new LinkedHashMap<>();
            for (PipfileEntry declared : pipfileEntries.values()) {
                declaredByCanonical.put(Pep508Requirement.canonicalize(declared.keyAsWritten), declared);
            }

            // A retained top-level entry may still require the removed package; real pipenv
            // keeps it as a transitive dependency. The check is bounded by the Pipfile size.
            String requiredBy = null;
            String unverifiable = null;
            for (Map.Entry<String, Object> retained : lockEntries.entrySet()) {
                if (retained.getKey().equals(edit.lockKey)) {
                    continue;
                }
                PipfileEntry declared = declaredByCanonical.get(Pep508Requirement.canonicalize(retained.getKey()));
                if (declared == null) {
                    continue; // transitive entries have no reconstructible requirement set
                }
                Boolean requires = requiresRemoved(retained.getKey(), entryMap(retained.getValue()),
                        declared, removedCanonical);
                if (requires == null) {
                    unverifiable = retained.getKey();
                } else if (requires) {
                    requiredBy = retained.getKey();
                    break;
                }
            }

            if (requiredBy != null || unverifiable != null) {
                // never guess toward a subset lock: keep the entry, demoted to transitive
                Map<String, Object> kept = new LinkedHashMap<>(entryMap(lockEntries.get(edit.lockKey)));
                kept.remove("index");
                lockEntries.put(edit.lockKey, kept);
                notes.add(requiredBy != null ?
                        "DEMOTED_TO_TRANSITIVE: " + edit.lockKey + " in " + edit.lockCategory +
                                " is still required by " + requiredBy + "; kept without its index key" :
                        "ORPHANS_RETAINED: " + edit.lockKey + " in " + edit.lockCategory +
                                " was kept; requirements of " + unverifiable +
                                " could not be verified to prove it unreferenced");
                return;
            }
            lockEntries.remove(edit.lockKey);
            // Deep-transitive reverse dependencies are not reconstructible; a superset
            // lock installs correctly.
            notes.add("ORPHANS_RETAINED: removed " + edit.lockKey + " from " + edit.lockCategory +
                    "; transitive dependencies it may have introduced were retained (only direct " +
                    "requirements of remaining top-level entries were checked)");
        }

        /**
         * Whether the retained top-level entry's pinned version directly requires the
         * removed package in the lock environment; null when unverifiable.
         */
        private @Nullable Boolean requiresRemoved(String pkg, Map<String, Object> lockEntry,
                                                  PipfileEntry declared, String removedCanonical) {
            if (declared.unsupported) {
                return null; // VCS/path pins have no fetchable metadata
            }
            PythonVersion pinned = PythonVersion.parse(pinnedVersion(lockEntry));
            if (pinned == null) {
                return null;
            }
            CoreMetadata metadata;
            try {
                PythonPackageIndex index = selectIndex(
                        declared.indexName != null ? declared.indexName : indexName(lockEntry));
                PackageListing listing = client.listFiles(index, SimpleIndexClient.canonicalName(pkg));
                List<PackageFile> files = filesOfVersion(listing, pkg, pinned);
                metadata = files.isEmpty() ? null : fetchMetadata(pkg, index, files).metadata;
            } catch (PythonIndexException e) {
                metadata = null;
            }
            if (metadata == null) {
                return null;
            }
            for (String raw : metadata.getRequiresDist()) {
                Pep508Requirement requirement = Pep508Requirement.parse(raw);
                if (requirement == null) {
                    return null;
                }
                if (applies(requirement.getMarker(), declared.extras) &&
                        removedCanonical.equals(requirement.getCanonicalName())) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Applies an extras/markers/index change that leaves the pinned version in place.
         * Hashes are untouched; added extras can activate new requirements, so they
         * re-run the closure proof at the pinned version first.
         */
        private @Nullable Failure applyAttributes(Edit edit) {
            PipfileEntry entry = edit.entry;
            String pkg = entry.keyAsWritten;
            Map<String, Object> lockEntries = lockCategory(edit.lockCategory);
            Map<String, Object> oldEntry = entryMap(lockEntries.get(edit.lockKey));

            if (!recordedExtras(oldEntry).containsAll(canonicalizeAll(new HashSet<>(entry.extras)))) {
                PythonVersion pinned = PythonVersion.parse(pinnedVersion(oldEntry));
                PythonPackageIndex index;
                List<PackageFile> files;
                try {
                    index = selectIndex(entry.indexName != null ? entry.indexName : indexName(oldEntry));
                    PackageListing listing = client.listFiles(index, SimpleIndexClient.canonicalName(pkg));
                    files = filesOfVersion(listing, pkg, pinned);
                } catch (PythonIndexException e) {
                    return indexFailure(pkg, e);
                }
                MetadataResult md = fetchMetadata(pkg, index, files);
                if (md.failure != null || md.metadata == null) {
                    return md.failure;
                }
                Failure closureFailure = proveClosureUnchanged(pkg, md.metadata, entry, lockEntries);
                if (closureFailure != null) {
                    return closureFailure;
                }
            }

            Map<String, Object> newEntry = new LinkedHashMap<>(oldEntry);
            applyMarkers(newEntry, entry, null);
            applyExtras(newEntry, entry);
            applyIndex(newEntry, entry);
            lockEntries.put(edit.lockKey, newEntry);
            return null;
        }

        private @Nullable Failure applyChange(Edit edit) {
            PipfileEntry entry = edit.entry;
            String pkg = entry.keyAsWritten;
            PythonVersionSpecifierSet constraint = parseConstraint(entry.constraint);
            if (constraint == null) {
                return new Failure(Reason.RESOLUTION_REQUIRED, pkg, null,
                        "Unparseable version constraint: " + entry.constraint);
            }

            Map<String, Object> lockEntries = lockCategory(edit.lockCategory);
            Map<String, Object> oldEntry = entryMap(lockEntries.get(edit.lockKey));

            PythonPackageIndex index;
            PackageListing listing;
            try {
                String preferred = entry.indexName != null ? entry.indexName : indexName(oldEntry);
                index = selectIndex(preferred);
                listing = client.listFiles(index, SimpleIndexClient.canonicalName(pkg));
            } catch (PythonIndexException e) {
                return indexFailure(pkg, e);
            }

            Map<PythonVersion, List<PackageFile>> byVersion = groupByVersion(listing, pkg);
            PythonVersion selected = selectVersion(byVersion, constraint);
            if (selected == null) {
                return new Failure(Reason.RESOLUTION_CONFLICT, pkg, index.getUrl(),
                        "No non-yanked version satisfying " + entry.constraint +
                                (lockPython != null ? " for python " + lockPython : "") +
                                " found at " + index.getUrl());
            }
            List<PackageFile> files = byVersion.get(selected);

            MetadataResult md = fetchMetadata(pkg, index, files);
            if (md.failure != null || md.metadata == null) {
                return md.failure;
            }
            CoreMetadata metadata = md.metadata;

            Failure closureFailure = proveClosureUnchanged(pkg, metadata, entry, lockEntries);
            if (closureFailure != null) {
                return closureFailure;
            }

            String versionString = selected.toString();
            List<String> hashes = collectHashes(pkg, index, versionString, files);
            if (hashes == null) {
                return new Failure(Reason.HASH_UNAVAILABLE, pkg, index.getUrl(),
                        "The index lists a file of " + pkg + " " + versionString +
                                " without a sha256 digest, and it could not be downloaded to hash");
            }

            Map<String, Object> newEntry = new LinkedHashMap<>(oldEntry);
            newEntry.put("version", "==" + versionString);
            newEntry.put("hashes", hashes);
            String requiresPython = metadata.getRequiresPython() != null ?
                    metadata.getRequiresPython() : firstRequiresPython(files);
            applyMarkers(newEntry, entry, requiresPython);
            applyExtras(newEntry, entry);
            applyIndex(newEntry, entry);
            lockEntries.put(edit.lockKey, newEntry);
            return null;
        }

        private static class MetadataResult {
            @Nullable CoreMetadata metadata;
            @Nullable Failure failure;
        }

        /**
         * The metadata ladder (ADR 0009 §3): PEP 658 sidecar, lazy/full wheel read,
         * then sdist PKG-INFO gated on PEP 643 static metadata.
         */
        private MetadataResult fetchMetadata(String pkg, PythonPackageIndex index, List<PackageFile> files) {
            MetadataResult result = new MetadataResult();
            PackageFile wheel = null;
            PackageFile sdist = null;
            for (PackageFile file : files) {
                DistFilename dist = DistFilename.parse(file.getFilename());
                if (dist == null) {
                    continue;
                }
                if (dist.getType() == DistFilename.Type.WHEEL) {
                    if (wheel == null || (!Boolean.TRUE.equals(wheel.getCoreMetadataAvailable()) &&
                            Boolean.TRUE.equals(file.getCoreMetadataAvailable()))) {
                        wheel = file;
                    }
                } else if (dist.getType() == DistFilename.Type.SDIST && sdist == null) {
                    sdist = file;
                }
            }
            if (wheel != null) {
                CoreMetadata metadata = Pep658MetadataFetcher.fetch(http, wheel.getUrl());
                if (metadata == null) {
                    metadata = LazyWheelMetadataReader.read(http, wheel.getUrl());
                }
                if (metadata != null) {
                    // Wheel METADATA is trusted unconditionally.
                    result.metadata = metadata;
                    return result;
                }
                if (sdist == null) {
                    result.failure = new Failure(Reason.INDEX_UNREACHABLE, pkg, index.getUrl(),
                            "Could not fetch dependency metadata for " + wheel.getFilename());
                    return result;
                }
            }
            if (sdist == null) {
                result.failure = new Failure(Reason.RESOLUTION_CONFLICT, pkg, index.getUrl(),
                        "No wheel or sdist distribution available");
                return result;
            }
            CoreMetadata metadata = SdistMetadataReader.read(http, sdist.getUrl());
            if (metadata == null) {
                result.failure = new Failure(Reason.DYNAMIC_SDIST_METADATA, pkg, index.getUrl(),
                        "Could not read PKG-INFO from " + sdist.getFilename());
                return result;
            }
            if (!metadata.hasStaticRequiresDist()) {
                result.failure = new Failure(Reason.DYNAMIC_SDIST_METADATA, pkg, index.getUrl(),
                        "Sdist metadata of " + sdist.getFilename() + " does not declare static " +
                                "Requires-Dist (PEP 643); resolving would require executing a build");
                return result;
            }
            result.metadata = metadata;
            return result;
        }

        /**
         * Every requirement of the new version — after marker filtering in the lock
         * environment with the entry's requested extras — must be satisfied by an
         * existing pin in the same category, or the closure is not provably unchanged.
         */
        private @Nullable Failure proveClosureUnchanged(String pkg, CoreMetadata metadata,
                                                        PipfileEntry entry, Map<String, Object> lockEntries) {
            Map<String, String> lockKeysByCanonical = canonicalKeys(lockEntries);
            for (String raw : metadata.getRequiresDist()) {
                Pep508Requirement requirement = Pep508Requirement.parse(raw);
                if (requirement == null) {
                    return new Failure(Reason.RESOLUTION_REQUIRED, pkg, null,
                            "Unparseable requirement of the new version: " + raw);
                }
                if (!applies(requirement.getMarker(), entry.extras)) {
                    continue;
                }
                if (requirement.getUrl() != null) {
                    return new Failure(Reason.RESOLUTION_REQUIRED, pkg, null,
                            "The new version requires a direct URL dependency: " + raw);
                }
                String lockKey = lockKeysByCanonical.get(requirement.getCanonicalName());
                if (lockKey == null) {
                    return new Failure(Reason.RESOLUTION_REQUIRED, pkg, null,
                            "Requirement " + raw + " of the new version is not satisfied by the existing " +
                                    "lock; delta resolution is not yet supported by the native engine");
                }
                Map<String, Object> depEntry = entryMap(lockEntries.get(lockKey));
                if (!requirement.getExtras().isEmpty() && !recordedExtras(depEntry).containsAll(
                        canonicalizeAll(requirement.getExtras()))) {
                    return new Failure(Reason.RESOLUTION_REQUIRED, pkg, null,
                            "Requirement " + raw + " needs extras the existing pin of " + lockKey +
                                    " was not locked with; delta resolution is not yet supported");
                }
                String pinned = pinnedVersion(depEntry);
                PythonVersion pinnedVersion = PythonVersion.parse(pinned);
                if (pinnedVersion == null) {
                    if (requirement.getSpecifiers() != null && !requirement.getSpecifiers().isMatchAll()) {
                        return new Failure(Reason.RESOLUTION_REQUIRED, pkg, null,
                                "Requirement " + raw + " constrains " + lockKey +
                                        ", which is pinned to a VCS/path/file source");
                    }
                    continue;
                }
                if (requirement.getSpecifiers() != null &&
                        !requirement.getSpecifiers().contains(pinnedVersion, true)) {
                    return new Failure(Reason.RESOLUTION_REQUIRED, pkg, null,
                            "Requirement " + raw + " of the new version is not satisfied by the pinned " +
                                    lockKey + "==" + pinned + "; delta resolution is not yet supported");
                }
            }
            return null;
        }

        /**
         * Unknown marker outcomes count as required — never silently drop a requirement.
         */
        private boolean applies(@Nullable Marker marker, List<String> requestedExtras) {
            if (marker == null) {
                return true;
            }
            Boolean base = marker.evaluate(withExtra(env, ""));
            if (base == null || base) {
                return true;
            }
            for (String extra : requestedExtras) {
                Boolean result = marker.evaluate(withExtra(env, Pep508Requirement.canonicalize(extra)));
                if (result == null || result) {
                    return true;
                }
            }
            return false;
        }

        /**
         * pipenv's top-level marker composition (verified against pipenv 2026.6.2 locks):
         * an entry's own markers win outright over the Requires-Python-derived
         * python_version marker, which is synthesized only for entries declaring none.
         */
        private void applyMarkers(Map<String, Object> newEntry, PipfileEntry entry,
                                  @Nullable String requiresPython) {
            String composed = composeEntryMarkers(entry);
            if (composed != null) {
                newEntry.put("markers", composed);
                return;
            }
            String pythonMarker = markerFromSpecifier(requiresPython);
            if (pythonMarker == null) {
                return; // nothing new is known: preserve the existing markers verbatim
            }
            newEntry.put("markers", normalizeMarker(pythonMarker));
        }

        /**
         * The entry's markers string and sys_platform/platform_machine keys, normalized,
         * deduplicated, sorted, and AND-joined. Faithful to pipenv even where surprising:
         * os_name keys are dropped and an {@code or} marker joins unparenthesized.
         */
        private static @Nullable String composeEntryMarkers(PipfileEntry entry) {
            TreeSet<String> clauses = new TreeSet<>();
            if (entry.markers != null) {
                clauses.add(normalizeMarker(entry.markers));
            }
            for (Map.Entry<String, String> markerKey : entry.markerKeys.entrySet()) {
                if (!"os_name".equals(markerKey.getKey())) {
                    clauses.add(normalizeMarker(markerKey.getKey() + " " + markerKey.getValue().trim()));
                }
            }
            if (clauses.isEmpty()) {
                return null;
            }
            return normalizeMarker(String.join(" and ", clauses));
        }

        private static String normalizeMarker(String marker) {
            Marker parsed = Marker.parse(marker);
            return parsed != null ? parsed.toString() : marker.trim();
        }

        private boolean attributesDiffer(PipfileEntry entry, Map<String, Object> lockEntry) {
            if (!sortedExtras(entry).equals(recordedExtrasList(lockEntry))) {
                return true;
            }
            if (!Objects.equals(expectedIndexName(entry), indexName(lockEntry))) {
                return true;
            }
            return markersDiffer(entry, lockEntry);
        }

        private static boolean markersDiffer(PipfileEntry entry, Map<String, Object> lockEntry) {
            String composed = composeEntryMarkers(entry);
            if (composed == null) {
                return false; // recorded markers are Requires-Python-derived, not entry-declared
            }
            String recorded = stringOrNull(lockEntry.get("markers"));
            return recorded == null || !composed.equals(normalizeMarker(recorded));
        }

        private static void applyExtras(Map<String, Object> newEntry, PipfileEntry entry) {
            if (!entry.extras.isEmpty()) {
                newEntry.put("extras", sortedExtras(entry));
            } else {
                newEntry.remove("extras");
            }
        }

        private void applyIndex(Map<String, Object> newEntry, PipfileEntry entry) {
            String name = expectedIndexName(entry);
            if (name != null) {
                newEntry.put("index", name);
            } else {
                newEntry.remove("index");
            }
        }

        /**
         * pipenv records {@code index} for bare-string entries (naming the default
         * source) but for inline tables only when they carry an explicit index key.
         */
        private @Nullable String expectedIndexName(PipfileEntry entry) {
            if (entry.indexName != null) {
                return entry.indexName;
            }
            return entry.inlineTable ? null : defaultIndexName();
        }

        private @Nullable String defaultIndexName() {
            List<Map<String, Object>> sources = readPipfileSources();
            if (!sources.isEmpty()) {
                return stringOrNull(sources.get(0).get("name"));
            }
            if (lockMetaSources != null && !lockMetaSources.isEmpty()) {
                return stringOrNull(lockMetaSources.get(0).get("name"));
            }
            return "pypi";
        }

        private static List<String> sortedExtras(PipfileEntry entry) {
            return new ArrayList<>(new TreeSet<>(entry.extras));
        }

        private static List<String> recordedExtrasList(Map<String, Object> lockEntry) {
            List<String> recorded = new ArrayList<>();
            Object extras = lockEntry.get("extras");
            if (extras instanceof List) {
                for (Object extra : (List<?>) extras) {
                    if (extra instanceof String) {
                        recorded.add((String) extra);
                    }
                }
            }
            return recorded;
        }

        /**
         * pipenv's marker_from_specifier: each Requires-Python clause becomes a
         * python_version comparison, AND-ed together; runs of wildcard {@code ==}/{@code !=}
         * clauses collapse to {@code in}/{@code not in} lists (cleanup_pyspecs).
         */
        private static @Nullable String markerFromSpecifier(@Nullable String requiresPython) {
            PythonVersionSpecifierSet set = PythonVersionSpecifierSet.parse(requiresPython);
            if (set == null || set.isMatchAll()) {
                return null;
            }
            List<String> clauses = new ArrayList<>();
            List<String> inVersions = new ArrayList<>();
            List<String> notInVersions = new ArrayList<>();
            int inPosition = -1;
            int notInPosition = -1;
            for (PythonVersionSpecifier specifier : set.getSpecifiers()) {
                String version = specifier.getVersion();
                boolean wildcard = version.endsWith(".*");
                if (wildcard && "==".equals(specifier.getOperator())) {
                    if (inPosition < 0) {
                        inPosition = clauses.size();
                        clauses.add(null);
                    }
                    inVersions.add(version.substring(0, version.length() - 2));
                } else if (wildcard && "!=".equals(specifier.getOperator())) {
                    if (notInPosition < 0) {
                        notInPosition = clauses.size();
                        clauses.add(null);
                    }
                    notInVersions.add(version.substring(0, version.length() - 2));
                } else {
                    clauses.add("python_version " + specifier.getOperator() + " '" + version + "'");
                }
            }
            if (inPosition >= 0) {
                clauses.set(inPosition, "python_version in '" + String.join(", ", sortVersions(inVersions)) + "'");
            }
            if (notInPosition >= 0) {
                clauses.set(notInPosition, "python_version not in '" + String.join(", ", sortVersions(notInVersions)) + "'");
            }
            return String.join(" and ", clauses);
        }

        private static List<String> sortVersions(List<String> versions) {
            List<String> sorted = new ArrayList<>(versions);
            sorted.sort((a, b) -> {
                PythonVersion va = PythonVersion.parse(a);
                PythonVersion vb = PythonVersion.parse(b);
                return va != null && vb != null ? va.compareTo(vb) : a.compareTo(b);
            });
            return sorted;
        }

        private @Nullable List<String> collectHashes(String pkg, PythonPackageIndex index,
                                                     String version, List<PackageFile> files) {
            List<String> digests = null;
            if (PypiJsonApi.isPypi(index)) {
                digests = PypiJsonApi.sha256Digests(http, SimpleIndexClient.canonicalName(pkg), version);
            }
            if (digests == null) {
                digests = new ArrayList<>();
                for (PackageFile file : files) {
                    // pipenv downloads and hashes any listed file whose link carries no digest
                    String sha256 = file.getSha256() != null ? file.getSha256() : downloadAndHash(file.getUrl());
                    if (sha256 == null) {
                        return null;
                    }
                    digests.add(sha256);
                }
            }
            TreeSet<String> hashes = new TreeSet<>();
            for (String digest : digests) {
                hashes.add("sha256:" + digest);
            }
            return new ArrayList<>(hashes);
        }

        private @Nullable String downloadAndHash(String fileUrl) {
            try (HttpSender.Response response = http.send(http.get(fileUrl).build())) {
                if (!response.isSuccessful()) {
                    return null;
                }
                return Hashing.sha256Hex(response.getBodyAsBytes());
            } catch (Exception e) {
                return null;
            }
        }

        private PythonPackageIndex selectIndex(@Nullable String preferredName) {
            if (preferredName != null) {
                for (PythonPackageIndex index : indexes) {
                    if (preferredName.equals(index.getName())) {
                        return index;
                    }
                }
            }
            if (indexes.isEmpty()) {
                throw new PythonIndexException(PythonIndexException.Reason.UNREACHABLE, "",
                        "No package index configured");
            }
            return indexes.get(0);
        }

        /**
         * Fetches the listing for a package, trying each configured index in order when
         * no single index is pinned by name.
         */
        private PackageListing fetchListing(String pkg, @Nullable String preferredName) {
            String canonical = SimpleIndexClient.canonicalName(pkg);
            if (preferredName != null) {
                for (PythonPackageIndex index : indexes) {
                    if (preferredName.equals(index.getName())) {
                        return client.listFiles(index, canonical);
                    }
                }
            }
            PythonIndexException notFound = null;
            for (PythonPackageIndex index : indexes) {
                try {
                    return client.listFiles(index, canonical);
                } catch (PythonIndexException e) {
                    if (e.getReason() != PythonIndexException.Reason.NOT_FOUND) {
                        throw e;
                    }
                    notFound = e;
                }
            }
            throw notFound != null ? notFound :
                    new PythonIndexException(PythonIndexException.Reason.UNREACHABLE, "",
                            "No package index configured");
        }

        private Map<PythonVersion, List<PackageFile>> groupByVersion(PackageListing listing, String pkg) {
            String canonical = Pep508Requirement.canonicalize(pkg);
            Map<PythonVersion, List<PackageFile>> byVersion = new LinkedHashMap<>();
            for (PackageFile file : listing.getFiles()) {
                DistFilename dist = DistFilename.parse(file.getFilename());
                if (dist == null || !canonical.equals(Pep508Requirement.canonicalize(dist.getDistribution()))) {
                    continue;
                }
                PythonVersion version = PythonVersion.parse(dist.getVersion());
                if (version == null) {
                    continue;
                }
                List<PackageFile> files = byVersion.computeIfAbsent(version, k -> new ArrayList<>());
                files.add(file);
            }
            return byVersion;
        }

        private @Nullable PythonVersion selectVersion(Map<PythonVersion, List<PackageFile>> byVersion,
                                                      PythonVersionSpecifierSet constraint) {
            PythonVersion best = null;
            for (Map.Entry<PythonVersion, List<PackageFile>> candidate : byVersion.entrySet()) {
                PythonVersion version = candidate.getKey();
                if (!constraint.contains(version)) {
                    continue;
                }
                boolean installable = false;
                for (PackageFile file : candidate.getValue()) {
                    DistFilename dist = DistFilename.parse(file.getFilename());
                    if (!file.isYanked() && dist != null && dist.getType() != DistFilename.Type.OTHER) {
                        installable = true;
                        break;
                    }
                }
                if (!installable) {
                    continue;
                }
                if (lockPython != null && !versionAdmitsPython(candidate.getValue(), lockPython)) {
                    continue;
                }
                if (best == null || version.compareTo(best) > 0) {
                    best = version;
                }
            }
            return best;
        }

        /**
         * A version admits a Python when any of its files declares no Requires-Python
         * or declares one containing it.
         */
        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        private static boolean versionAdmitsPython(List<PackageFile> files, PythonVersion python) {
            for (PackageFile file : files) {
                String requires = file.getRequiresPython();
                if (requires == null) {
                    return true;
                }
                PythonVersionSpecifierSet set = PythonVersionSpecifierSet.parse(requires);
                if (set == null || set.contains(python, true)) {
                    return true;
                }
            }
            return false;
        }

        private static @Nullable String firstRequiresPython(List<PackageFile> files) {
            for (PackageFile file : files) {
                if (file.getRequiresPython() != null) {
                    return file.getRequiresPython();
                }
            }
            return null;
        }

        private List<PackageFile> filesOfVersion(PackageListing listing, String pkg, PythonVersion version) {
            List<PackageFile> files = groupByVersion(listing, pkg).get(version);
            return files != null ? files : Collections.emptyList();
        }

        private void rebuildMeta(Set<String> pipfileCategories) {
            List<Map<String, Object>> pipfileSources = readPipfileSources();
            String hash = PipfileLockHash.hash(pipfile,
                    pipfileSources.isEmpty() ? lockMetaSources : null,
                    PipfileLockHash.HashVariant.LEGACY);

            Map<String, Object> hashMap = new LinkedHashMap<>();
            hashMap.put("sha256", hash);
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("hash", hashMap);
            meta.put("pipfile-spec", 6L);
            meta.put("requires", tableToMap(findTable("requires")));
            if (!pipfileSources.isEmpty()) {
                meta.put("sources", pipfileSources);
            } else if (lockMetaSources != null) {
                meta.put("sources", lockMetaSources);
            } else {
                Map<String, Object> pypi = new LinkedHashMap<>();
                pypi.put("name", "pypi");
                pypi.put("url", "https://pypi.org/simple");
                pypi.put("verify_ssl", true);
                meta.put("sources", Collections.<Object>singletonList(pypi));
            }
            lock.put("_meta", meta);

            // pipenv emits every Pipfile category, even when empty.
            for (String category : pipfileCategories) {
                if (!(lock.get(category) instanceof Map)) {
                    lock.put(category, new LinkedHashMap<String, Object>());
                }
            }
        }

        /**
         * [[source]] entries as written (env placeholders unexpanded), with pipenv's
         * defaults populated: name from the URL host, verify_ssl from the URL scheme.
         */
        private List<Map<String, Object>> readPipfileSources() {
            List<Map<String, Object>> sources = new ArrayList<>();
            for (TomlValue value : pipfile.getValues()) {
                if (!(value instanceof Toml.Table)) {
                    continue;
                }
                Toml.Table table = (Toml.Table) value;
                Toml.Identifier name = table.getName();
                if (name == null || !"source".equals(name.getName())) {
                    continue;
                }
                Map<String, Object> source = tableToMap(table);
                String url = stringOrNull(source.get("url"));
                if (url == null) {
                    continue;
                }
                if (!(source.get("name") instanceof String)) {
                    source.put("name", hostOf(url));
                }
                if (!(source.get("verify_ssl") instanceof Boolean)) {
                    source.put("verify_ssl", url.startsWith("https"));
                }
                sources.add(source);
            }
            return sources;
        }

        static String hostOf(String url) {
            int scheme = url.indexOf("://");
            String rest = scheme < 0 ? url : url.substring(scheme + 3);
            int slash = rest.indexOf('/');
            if (slash >= 0) {
                rest = rest.substring(0, slash);
            }
            int at = rest.lastIndexOf('@');
            if (at >= 0) {
                rest = rest.substring(at + 1);
            }
            int colon = rest.indexOf(':');
            if (colon >= 0) {
                rest = rest.substring(0, colon);
            }
            return rest.isEmpty() ? "source" : rest;
        }

        @SuppressWarnings("SameParameterValue")
        private Toml.@Nullable Table findTable(String name) {
            for (TomlValue value : pipfile.getValues()) {
                if (value instanceof Toml.Table) {
                    Toml.Table table = (Toml.Table) value;
                    if (table.getName() != null && name.equals(table.getName().getName())) {
                        return table;
                    }
                }
            }
            return null;
        }

        private static Map<String, Object> tableToMap(Toml.@Nullable Table table) {
            Map<String, Object> map = new LinkedHashMap<>();
            if (table == null) {
                return map;
            }
            for (Toml value : table.getValues()) {
                if (!(value instanceof Toml.KeyValue)) {
                    continue;
                }
                Toml.KeyValue kv = (Toml.KeyValue) value;
                if (!(kv.getKey() instanceof Toml.Identifier)) {
                    continue;
                }
                Object v = kv.getValue() instanceof Toml.Literal ? ((Toml.Literal) kv.getValue()).getValue() : null;
                if (v instanceof String || v instanceof Boolean || v instanceof Long) {
                    map.put(((Toml.Identifier) kv.getKey()).getName(), v);
                }
            }
            return map;
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> lockCategory(String name) {
            Object category = lock.get(name);
            if (category instanceof Map) {
                return (Map<String, Object>) category;
            }
            Map<String, Object> created = new LinkedHashMap<>();
            lock.put(name, created);
            return created;
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> entryMap(@Nullable Object entry) {
            return entry instanceof Map ? (Map<String, Object>) entry : emptyMap();
        }

        private static Map<String, String> canonicalKeys(Map<String, Object> lockEntries) {
            Map<String, String> byCanonical = new LinkedHashMap<>();
            for (String key : lockEntries.keySet()) {
                byCanonical.put(Pep508Requirement.canonicalize(key), key);
            }
            return byCanonical;
        }

        private static @Nullable String pinnedVersion(Map<String, Object> lockEntry) {
            String version = stringOrNull(lockEntry.get("version"));
            if (version == null) {
                return null;
            }
            String trimmed = version.trim();
            return trimmed.startsWith("==") ? trimmed.substring(2).trim() : trimmed;
        }

        private static @Nullable String indexName(Map<String, Object> lockEntry) {
            return stringOrNull(lockEntry.get("index"));
        }

        private static @Nullable PythonVersionSpecifierSet parseConstraint(String constraint) {
            String trimmed = constraint.trim();
            if (trimmed.isEmpty() || "*".equals(trimmed)) {
                return PythonVersionSpecifierSet.parse("");
            }
            return PythonVersionSpecifierSet.parse(trimmed);
        }

        private static Set<String> recordedExtras(Map<String, Object> lockEntry) {
            Object extras = lockEntry.get("extras");
            Set<String> recorded = new HashSet<>();
            if (extras instanceof List) {
                for (Object extra : (List<?>) extras) {
                    if (extra instanceof String) {
                        recorded.add(Pep508Requirement.canonicalize((String) extra));
                    }
                }
            }
            return recorded;
        }

        private static Set<String> canonicalizeAll(Set<String> extras) {
            Set<String> canonical = new HashSet<>();
            for (String extra : extras) {
                canonical.add(Pep508Requirement.canonicalize(extra));
            }
            return canonical;
        }

        private static MarkerEnvironment withExtra(MarkerEnvironment env, String extra) {
            return MarkerEnvironment.builder()
                    .pythonVersion(env.getPythonVersion())
                    .pythonFullVersion(env.getPythonFullVersion())
                    .osName(env.getOsName())
                    .sysPlatform(env.getSysPlatform())
                    .platformMachine(env.getPlatformMachine())
                    .platformSystem(env.getPlatformSystem())
                    .platformRelease(env.getPlatformRelease())
                    .platformVersion(env.getPlatformVersion())
                    .platformPythonImplementation(env.getPlatformPythonImplementation())
                    .implementationName(env.getImplementationName())
                    .implementationVersion(env.getImplementationVersion())
                    .extra(extra)
                    .build();
        }

        private static @Nullable String stringOrNull(@Nullable Object value) {
            return value instanceof String ? (String) value : null;
        }

        private static @Nullable String literalString(Toml value) {
            return value instanceof Toml.Literal ? stringOrNull(((Toml.Literal) value).getValue()) : null;
        }
    }
}
