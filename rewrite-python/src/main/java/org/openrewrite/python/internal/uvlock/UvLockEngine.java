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
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.python.internal.Hashing;
import org.openrewrite.python.internal.LockFileRegeneration.Failure;
import org.openrewrite.python.internal.LockFileRegeneration.Reason;
import org.openrewrite.python.internal.LockFileRegeneration.Result;
import org.openrewrite.python.internal.index.DistFilename;
import org.openrewrite.python.internal.index.Environment;
import org.openrewrite.python.internal.index.FlatIndexClient;
import org.openrewrite.python.internal.index.PackageFile;
import org.openrewrite.python.internal.index.PackageListing;
import org.openrewrite.python.internal.index.PythonIndexException;
import org.openrewrite.python.internal.index.SimpleIndexClient;
import org.openrewrite.python.internal.index.UvIndex;
import org.openrewrite.python.internal.index.UvIndexDiscovery;
import org.openrewrite.python.internal.metadata.CoreMetadata;
import org.openrewrite.python.internal.metadata.LazyWheelMetadataReader;
import org.openrewrite.python.internal.metadata.Pep658MetadataFetcher;
import org.openrewrite.python.internal.metadata.SdistMetadataReader;
import org.openrewrite.python.internal.pep440.PythonVersion;
import org.openrewrite.python.internal.pep440.PythonVersionSpecifier;
import org.openrewrite.python.internal.pep440.PythonVersionSpecifierSet;
import org.openrewrite.python.internal.pep508.Marker;
import org.openrewrite.python.internal.pep508.Pep508Requirement;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

/**
 * Native {@code uv.lock} regeneration with minimal-update semantics (ADR 0009): the old
 * lock is the resolution state, the edited pyproject.toml is diffed against the root
 * package's recorded {@code [package.metadata]} (uv's {@code --check} oracle), and only
 * the affected entries are rewritten in uv's own byte-exact emission style. Any edit
 * whose result cannot be proven correct fails loud with a structured {@link Failure}.
 */
public final class UvLockEngine {

    private static final Pattern UPLOAD_TIME = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(?:\\.(\\d+))?Z");

    private UvLockEngine() {
    }

    /**
     * uv records upload-time truncated to milliseconds with trailing fractional zeros
     * trimmed (verified against PyPI's microsecond PEP 700 timestamps). Unrecognized
     * shapes are omitted rather than recorded wrong.
     */
    static @Nullable String formatUploadTime(@Nullable String pep700) {
        if (pep700 == null) {
            return null;
        }
        Matcher m = UPLOAD_TIME.matcher(pep700);
        if (!m.matches()) {
            return null;
        }
        String fraction = m.group(2);
        if (fraction == null) {
            return m.group(1) + "Z";
        }
        if (fraction.length() > 3) {
            fraction = fraction.substring(0, 3);
        }
        int end = fraction.length();
        while (end > 0 && fraction.charAt(end - 1) == '0') {
            end--;
        }
        fraction = fraction.substring(0, end);
        return m.group(1) + (fraction.isEmpty() ? "" : "." + fraction) + "Z";
    }

    /**
     * Visible for tests: truth of a simple marker clause over a requires-python interval.
     */
    static @Nullable Boolean clauseTruth(String requiresPython, String var, String op, String value) {
        return Run.clauseTruth(Run.parseRange(requiresPython), var, op, value);
    }

    public static Result regenerate(String pyprojectContent, @Nullable String oldLockContent, ExecutionContext ctx) {
        if (oldLockContent == null) {
            return Result.failure(new Failure(Reason.RESOLUTION_REQUIRED, null, null,
                    "No existing uv.lock; locking from scratch requires full dependency resolution, " +
                            "which the native engine does not yet support"));
        }
        UvLock lock;
        try {
            lock = UvLockReader.parse(oldLockContent);
        } catch (UvLockFormatException e) {
            return Result.failure(new Failure(Reason.MALFORMED_LOCK, null, null,
                    "Existing uv.lock could not be parsed: " + e.getMessage()));
        }
        Toml.Document pyproject = parseToml(pyprojectContent);
        if (pyproject == null) {
            return Result.failure(new Failure(Reason.MALFORMED_MANIFEST, null, null,
                    "Edited pyproject.toml could not be parsed as TOML"));
        }
        try {
            return new Run(pyproject, lock, ctx).regenerate();
        } catch (PythonIndexException e) {
            return Result.failure(indexFailure(null, e));
        } catch (UvLockFormatException e) {
            return Result.failure(new Failure(Reason.MALFORMED_LOCK, null, null,
                    "Regenerated lock could not be emitted in uv's format: " + e.getMessage()));
        }
    }

    private static Toml.@Nullable Document parseToml(String content) {
        boolean[] failed = new boolean[1];
        SourceFile parsed = new TomlParser()
                .parse(new InMemoryExecutionContext(t -> failed[0] = true), content)
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
     * A declared dependency from the edited pyproject, tagged with the metadata section
     * it records into: requires-dist (optionally under an extra) or a dev group.
     */
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

    private static final class Change {
        final String canonicalName;
        final List<PythonVersionSpecifierSet> constraints = new ArrayList<>();
        @Nullable String pinnedIndexName;
        boolean indexChanged;

        Change(String canonicalName) {
            this.canonicalName = canonicalName;
        }
    }

    private static final class Removal {
        final String canonicalName;
        final @Nullable String extraGroup;
        final @Nullable String devGroup;

        Removal(String canonicalName, @Nullable String extraGroup, @Nullable String devGroup) {
            this.canonicalName = canonicalName;
            this.extraGroup = extraGroup;
            this.devGroup = devGroup;
        }
    }

    /**
     * The lock's requires-python interval; upper is null when unbounded.
     */
    private static final class PyRange {
        @Nullable PythonVersion lower;
        boolean lowerExclusive;
        @Nullable PythonVersion upper;
        boolean upperInclusive;
    }

    private static final class EngineFailure extends RuntimeException {
        final Failure failure;

        EngineFailure(Failure failure) {
            super(failure.getDetail());
            this.failure = failure;
        }

        EngineFailure(Reason reason, @Nullable String packageName, String detail) {
            this(new Failure(reason, packageName, null, detail));
        }
    }

    private static final class Run {
        private static final Pattern SIMPLE_CLAUSE = Pattern.compile(
                "\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*(===|==|!=|<=|>=|<|>|~=|not\\s+in|in)\\s*(['\"])([^'\"]*)\\3\\s*");
        private static final Pattern EXTRA_CLAUSE = Pattern.compile("extra == '([^']+)'");
        private static final Set<String> VERSION_VARS = new HashSet<>(Arrays.asList(
                "python_version", "python_full_version"));

        private final Toml.Document pyproject;
        private final UvLock lock;
        private final HttpSender http;
        private final SimpleIndexClient simpleClient;
        private final FlatIndexClient flatClient;
        private final List<UvIndex> indexes;
        private final Map<String, List<String>> indexPins;
        private final Set<String> nonRegistrySources;

        private List<UvLockPackage> packages;
        private int rootIndex;
        private UvLockMetadata oldMetadata;
        private PyRange range;

        Run(Toml.Document pyproject, UvLock lock, ExecutionContext ctx) {
            this.pyproject = pyproject;
            this.lock = lock;
            this.http = HttpSenderExecutionContextView.view(ctx).getHttpSender();
            this.simpleClient = new SimpleIndexClient(http);
            this.flatClient = new FlatIndexClient(http);
            this.indexes = UvIndexDiscovery.discover(ctx, pyproject, null, Environment.SYSTEM);
            this.indexPins = UvIndexDiscovery.sourceIndexPins(pyproject);
            this.nonRegistrySources = readNonRegistrySources();
        }

        Result regenerate() {
            try {
                return run();
            } catch (EngineFailure e) {
                return Result.failure(e.failure);
            }
        }

        private Result run() {
            if (lock.getManifest() != null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                        "Workspace locks ([manifest] with members) are not supported by the native engine");
            }
            packages = new ArrayList<>(lock.getPackages());
            rootIndex = findRoot();
            UvLockPackage root = packages.get(rootIndex);
            oldMetadata = root.getMetadata() != null ? root.getMetadata() :
                    UvLockMetadata.builder().build();

            Toml.Table project = findTable("project");
            if (project == null) {
                throw new EngineFailure(Reason.MALFORMED_MANIFEST, null,
                        "pyproject.toml has no [project] table");
            }
            String projectName = literalString(project, "name");
            if (projectName == null ||
                    !Pep508Requirement.canonicalize(projectName).equals(Pep508Requirement.canonicalize(root.getName()))) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, projectName,
                        "The [project] name does not match the lock's root package " + root.getName());
            }

            String newRequiresPython = normalizeRequiresPython(literalString(project, "requires-python"));
            boolean pythonChanged = !newRequiresPython.equals(lock.getRequiresPython());
            range = parseRange(newRequiresPython);
            if (pythonChanged) {
                checkPythonBounds(parseRange(lock.getRequiresPython() != null ? lock.getRequiresPython() : ""));
            }

            List<Declared> declared = readDeclarations(project);
            List<String> providesExtras = readProvidesExtras();

            UvLockMetadata newMetadata = buildMetadata(declared, providesExtras);
            boolean metadataEmpty = newMetadata.getRequiresDist() == null &&
                    newMetadata.getProvidesExtras() == null && newMetadata.getRequiresDev() == null;
            packages.set(rootIndex, root.toBuilder()
                    .version(projectVersion(project, root.getVersion()))
                    // uv omits [package.metadata] entirely when nothing is declared (s-remove-last-dep fixture)
                    .metadata(metadataEmpty ? null : newMetadata)
                    .build());

            List<Change> changes = new ArrayList<>();
            List<Removal> removals = new ArrayList<>();
            computeEdits(newMetadata, changes, removals);

            UvLock updated = lock.withRequiresPython(newRequiresPython);
            boolean structural = false;
            if (pythonChanged && lowerBoundIncreased()) {
                applyPythonBump();
                structural = true;
            }
            Set<String> rewritten = new HashSet<>();
            for (Change change : changes) {
                if (applyChange(change)) {
                    rewritten.add(change.canonicalName);
                    structural = true;
                }
            }
            for (Removal removal : removals) {
                applyRemoval(removal);
                structural = true;
            }
            if (structural) {
                dropUnreachable();
            }
            if (pythonChanged && lowerBoundIncreased()) {
                validatePinsAgainstPython(rewritten);
            }

            return Result.success(UvLockWriter.write(updated.withPackages(packages)));
        }

        // ---- manifest reading ----

        private int findRoot() {
            int found = -1;
            for (int i = 0; i < packages.size(); i++) {
                UvLockSource source = packages.get(i).getSource();
                if ((source.getType() == UvLockSource.Type.VIRTUAL || source.getType() == UvLockSource.Type.EDITABLE) &&
                        ".".equals(source.getValue())) {
                    if (found >= 0) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                                "Multiple root packages in the lock");
                    }
                    found = i;
                }
            }
            if (found < 0) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, null,
                        "No root package (source virtual/editable \".\") found in the lock");
            }
            return found;
        }

        private String projectVersion(Toml.Table project, String oldVersion) {
            String version = literalString(project, "version");
            if (version == null) {
                return oldVersion;
            }
            PythonVersion parsed = PythonVersion.parse(version);
            if (parsed == null) {
                throw new EngineFailure(Reason.MALFORMED_MANIFEST, null,
                        "The [project] version is not a valid PEP 440 version: " + version);
            }
            return parsed.toString();
        }

        private List<Declared> readDeclarations(Toml.Table project) {
            List<Declared> declared = new ArrayList<>();
            for (String spec : stringArray(project, "dependencies")) {
                declared.add(toDeclared(spec, null, null));
            }
            Toml.Table optional = findTable("project.optional-dependencies");
            if (optional != null) {
                for (Toml value : optional.getValues()) {
                    if (!(value instanceof Toml.KeyValue)) {
                        continue;
                    }
                    Toml.KeyValue kv = (Toml.KeyValue) value;
                    String extra = keyName(kv);
                    if (extra == null || !(kv.getValue() instanceof Toml.Array)) {
                        throw new EngineFailure(Reason.MALFORMED_MANIFEST, null,
                                "Unreadable [project.optional-dependencies] entry");
                    }
                    for (String spec : stringArrayValues((Toml.Array) kv.getValue(), "optional-dependencies")) {
                        declared.add(toDeclared(spec, Pep508Requirement.canonicalize(extra), null));
                    }
                }
            }
            Toml.Table groups = findTable("dependency-groups");
            if (groups != null) {
                for (Toml value : groups.getValues()) {
                    if (!(value instanceof Toml.KeyValue)) {
                        continue;
                    }
                    Toml.KeyValue kv = (Toml.KeyValue) value;
                    String group = keyName(kv);
                    if (group == null || !(kv.getValue() instanceof Toml.Array)) {
                        throw new EngineFailure(Reason.MALFORMED_MANIFEST, null,
                                "Unreadable [dependency-groups] entry");
                    }
                    for (String spec : stringArrayValues((Toml.Array) kv.getValue(), "dependency-groups")) {
                        declared.add(toDeclared(spec, null, group));
                    }
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
            if (requirement.getUrl() != null) {
                throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, requirement.getCanonicalName(),
                        "Direct URL dependencies are not supported by the native engine: " + spec);
            }
            int semi = spec.indexOf(';');
            String rawMarker = requirement.getMarker() != null && semi >= 0 ?
                    spec.substring(semi + 1).trim() : null;
            return new Declared(requirement, rawMarker, extraGroup, devGroup);
        }

        private List<String> readProvidesExtras() {
            Toml.Table optional = findTable("project.optional-dependencies");
            if (optional == null) {
                return emptyList();
            }
            List<String> extras = new ArrayList<>();
            for (Toml value : optional.getValues()) {
                if (value instanceof Toml.KeyValue) {
                    String extra = keyName((Toml.KeyValue) value);
                    if (extra != null) {
                        extras.add(Pep508Requirement.canonicalize(extra));
                    }
                }
            }
            return extras;
        }

        /**
         * Package names whose {@code [tool.uv.sources]} entry pins something other than a
         * named index (git/path/workspace/editable).
         */
        private Set<String> readNonRegistrySources() {
            Set<String> nonRegistry = new HashSet<>();
            for (TomlValue value : pyproject.getValues()) {
                if (!(value instanceof Toml.Table)) {
                    continue;
                }
                Toml.Table table = (Toml.Table) value;
                String name = tableName(table);
                if ("tool.uv.sources".equals(name)) {
                    for (Toml sourceValue : table.getValues()) {
                        if (!(sourceValue instanceof Toml.KeyValue)) {
                            continue;
                        }
                        Toml.KeyValue kv = (Toml.KeyValue) sourceValue;
                        String pkg = keyName(kv);
                        if (pkg != null && !hasIndexKey(kv.getValue())) {
                            nonRegistry.add(Pep508Requirement.canonicalize(pkg));
                        }
                    }
                } else if (name != null && name.startsWith("tool.uv.sources.")) {
                    String pkg = name.substring("tool.uv.sources.".length());
                    if (literalString(table, "index") == null) {
                        nonRegistry.add(Pep508Requirement.canonicalize(unquote(pkg)));
                    }
                }
            }
            return nonRegistry;
        }

        private static boolean hasIndexKey(Toml value) {
            if (value instanceof Toml.Table) {
                return literalString((Toml.Table) value, "index") != null;
            }
            if (value instanceof Toml.Array) {
                for (Toml element : ((Toml.Array) value).getValues()) {
                    if (!(element instanceof Toml.Table) ||
                            literalString((Toml.Table) element, "index") == null) {
                        return false;
                    }
                }
                return !((Toml.Array) value).getValues().isEmpty();
            }
            return false;
        }

        // ---- metadata rebuild ----

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
            Toml.Table groups = findTable("dependency-groups");
            if (groups != null) {
                for (Toml value : groups.getValues()) {
                    if (value instanceof Toml.KeyValue) {
                        String group = keyName((Toml.KeyValue) value);
                        if (group != null && !requiresDev.containsKey(group)) {
                            requiresDev.put(group, new ArrayList<>());
                        }
                    }
                }
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
                    List<String> newRendered = rendered(entries.subList(i, end));
                    List<String> oldRendered = rendered(oldRun);
                    List<String> newSorted = new ArrayList<>(newRendered);
                    List<String> oldSorted = new ArrayList<>(oldRendered);
                    Collections.sort(newSorted);
                    Collections.sort(oldSorted);
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
            if (nonRegistrySources.contains(decl.canonicalName)) {
                UvLockRequirement old = findOldRequirement(decl);
                if (old == null) {
                    throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, decl.canonicalName,
                            "Adding a git/path/editable dependency is not supported by the native engine");
                }
                return old;
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
            List<String> extras = null;
            if (!decl.requirement.getExtras().isEmpty()) {
                extras = new ArrayList<>();
                for (String extra : decl.requirement.getExtras()) {
                    extras.add(Pep508Requirement.canonicalize(extra));
                }
            }
            String specifier = normalizeSpecifier(decl);
            String index = pins == null ? null : declaredIndexUrl(pins.get(0), decl.canonicalName);
            String marker = resolveMetadataMarker(decl);
            return new UvLockRequirement(decl.canonicalName, extras, null, marker, specifier, index);
        }

        private @Nullable String normalizeSpecifier(Declared decl) {
            PythonVersionSpecifierSet specifiers = decl.requirement.getSpecifiers();
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
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, decl.canonicalName,
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
            String normalized = recordMarker(decl.rawMarker, extraClause);
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

        private @Nullable UvLockRequirement findOldRequirement(Declared decl) {
            List<UvLockRequirement> section = decl.devGroup != null ?
                    (oldMetadata.getRequiresDev() != null ? oldMetadata.getRequiresDev().get(decl.devGroup) : null) :
                    oldMetadata.getRequiresDist();
            if (section == null) {
                return null;
            }
            for (UvLockRequirement req : section) {
                if (req.getName().equals(decl.canonicalName) &&
                        Objects.equals(decl.extraGroup, extraGroupOf(req))) {
                    return req;
                }
            }
            return null;
        }

        /**
         * Sentinel group for markers gating on more than one extra, which cannot be
         * attributed to a single optional-dependencies group.
         */
        private static final String MULTI_EXTRA = "\0multi-extra";

        private static @Nullable String extraGroupOf(UvLockRequirement req) {
            if (req.getMarker() == null) {
                return null;
            }
            Matcher m = EXTRA_CLAUSE.matcher(req.getMarker());
            if (!m.find()) {
                return null;
            }
            String first = m.group(1);
            return m.find() ? MULTI_EXTRA : first;
        }

        // ---- diffing ----

        private void computeEdits(UvLockMetadata newMetadata, List<Change> changes, List<Removal> removals) {
            Map<String, Change> changeByName = new LinkedHashMap<>();
            diffSection(oldMetadata.getRequiresDist(), newMetadata.getRequiresDist(), null, changeByName, removals);
            Map<String, List<UvLockRequirement>> oldDev = oldMetadata.getRequiresDev() != null ?
                    oldMetadata.getRequiresDev() : new LinkedHashMap<>();
            Map<String, List<UvLockRequirement>> newDev = newMetadata.getRequiresDev() != null ?
                    newMetadata.getRequiresDev() : new LinkedHashMap<>();
            Set<String> allGroups = new HashSet<>(oldDev.keySet());
            allGroups.addAll(newDev.keySet());
            for (String group : allGroups) {
                diffSection(oldDev.get(group), newDev.get(group), group, changeByName, removals);
            }
            // a changed package must satisfy its declarations from every section, not just the edited one
            for (Change change : changeByName.values()) {
                collectConstraints(change, newMetadata.getRequiresDist());
                for (List<UvLockRequirement> group : newDev.values()) {
                    collectConstraints(change, group);
                }
            }
            changes.addAll(changeByName.values());
        }

        private void collectConstraints(Change change, @Nullable List<UvLockRequirement> section) {
            if (section == null) {
                return;
            }
            for (UvLockRequirement req : section) {
                if (!req.getName().equals(change.canonicalName) || req.getSpecifier() == null) {
                    continue;
                }
                PythonVersionSpecifierSet constraint = PythonVersionSpecifierSet.parse(req.getSpecifier());
                if (constraint == null) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, change.canonicalName,
                            "Unparseable version constraint: " + req.getSpecifier());
                }
                change.constraints.add(constraint);
            }
        }

        private void diffSection(@Nullable List<UvLockRequirement> oldSection,
                                 @Nullable List<UvLockRequirement> newSection,
                                 @Nullable String devGroup,
                                 Map<String, Change> changes, List<Removal> removals) {
            Map<String, List<UvLockRequirement>> oldByKey = byKey(oldSection);
            Map<String, List<UvLockRequirement>> newByKey = byKey(newSection);
            for (Map.Entry<String, List<UvLockRequirement>> e : newByKey.entrySet()) {
                List<UvLockRequirement> oldReqs = oldByKey.remove(e.getKey());
                List<UvLockRequirement> newReqs = e.getValue();
                if (oldReqs == null) {
                    UvLockRequirement added = newReqs.get(0);
                    if (nonRegistrySources.contains(added.getName())) {
                        throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, added.getName(),
                                "Git/path/editable dependency entries are not supported by the native engine");
                    }
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, added.getName(),
                            "Package is not in the existing lock; adding a package requires delta " +
                                    "resolution, which the native engine does not yet support");
                }
                if (rendered(oldReqs).equals(rendered(newReqs))) {
                    continue;
                }
                if (e.getKey().endsWith(MULTI_EXTRA)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, newReqs.get(0).getName(),
                            "Editing a declaration whose marker gates on multiple extras is not supported");
                }
                if (oldReqs.size() != 1 || newReqs.size() != 1) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, newReqs.get(0).getName(),
                            "Editing marker-differentiated duplicate declarations is not supported");
                }
                UvLockRequirement oldReq = oldReqs.get(0);
                UvLockRequirement newReq = newReqs.get(0);
                if (!Objects.equals(oldReq.getExtras(), newReq.getExtras())) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, newReq.getName(),
                            "Changing declared extras requires resolution, which the native engine " +
                                    "does not yet support");
                }
                if (!Objects.equals(oldReq.getMarker(), newReq.getMarker())) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, newReq.getName(),
                            "Changing a declaration's environment marker requires resolution");
                }
                if (!Objects.equals(oldReq.getEditable(), newReq.getEditable())) {
                    throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, newReq.getName(),
                            "Re-targeting between registry and editable sources is not supported");
                }
                Change change = changes.get(newReq.getName());
                if (change == null) {
                    change = new Change(newReq.getName());
                    changes.put(newReq.getName(), change);
                }
                List<String> pins = indexPins.get(newReq.getName());
                change.pinnedIndexName = pins != null ? pins.get(0) : null;
                if (!Objects.equals(oldReq.getIndex(), newReq.getIndex())) {
                    change.indexChanged = true;
                }
            }
            for (List<UvLockRequirement> removed : oldByKey.values()) {
                for (UvLockRequirement req : removed) {
                    String extraGroup = devGroup == null ? extraGroupOf(req) : null;
                    if (MULTI_EXTRA.equals(extraGroup)) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, req.getName(),
                                "Removing a declaration whose marker gates on multiple extras is not supported");
                    }
                    removals.add(new Removal(req.getName(), extraGroup, devGroup));
                }
            }
        }

        private static Map<String, List<UvLockRequirement>> byKey(@Nullable List<UvLockRequirement> section) {
            Map<String, List<UvLockRequirement>> byKey = new LinkedHashMap<>();
            if (section != null) {
                for (UvLockRequirement req : section) {
                    String key = req.getName() + '\0' + extraGroupOf(req);
                    List<UvLockRequirement> reqs = byKey.computeIfAbsent(key, k -> new ArrayList<>());
                    reqs.add(req);
                }
            }
            return byKey;
        }

        private static List<String> rendered(List<UvLockRequirement> reqs) {
            List<String> rendered = new ArrayList<>(reqs.size());
            for (UvLockRequirement req : reqs) {
                rendered.add(req.getName() + "|" + req.getExtras() + "|" + req.getEditable() + "|" +
                        req.getMarker() + "|" + req.getSpecifier() + "|" + req.getIndex());
            }
            return rendered;
        }

        // ---- requires-python ----

        private String normalizeRequiresPython(@Nullable String declared) {
            if (declared == null) {
                throw new EngineFailure(Reason.MALFORMED_MANIFEST, null,
                        "pyproject.toml [project] has no requires-python");
            }
            List<Map.Entry<PythonVersion, String>> clauses = new ArrayList<>();
            for (String raw : declared.split(",")) {
                String clause = raw.replaceAll("\\s+", "");
                if (clause.isEmpty()) {
                    continue;
                }
                PythonVersionSpecifier spec = PythonVersionSpecifier.parse(clause);
                if (spec == null) {
                    throw new EngineFailure(Reason.MALFORMED_MANIFEST, null,
                            "Unparseable requires-python clause: " + raw);
                }
                String version = spec.getVersion();
                if (version.endsWith(".*")) {
                    version = version.substring(0, version.length() - 2);
                }
                PythonVersion key = PythonVersion.parse(version);
                if (key == null) {
                    throw new EngineFailure(Reason.MALFORMED_MANIFEST, null,
                            "Unparseable requires-python clause: " + raw);
                }
                clauses.add(new java.util.AbstractMap.SimpleImmutableEntry<>(key, clause));
            }
            clauses.sort(Map.Entry.comparingByKey());
            StringBuilder b = new StringBuilder();
            for (Map.Entry<PythonVersion, String> clause : clauses) {
                if (b.length() > 0) {
                    b.append(", ");
                }
                b.append(clause.getValue());
            }
            return b.toString();
        }

        private static PyRange parseRange(String requiresPython) {
            PyRange range = new PyRange();
            PythonVersionSpecifierSet set = PythonVersionSpecifierSet.parse(requiresPython);
            if (set == null) {
                return range;
            }
            for (PythonVersionSpecifier spec : set.getSpecifiers()) {
                String op = spec.getOperator();
                String version = spec.getVersion();
                boolean wildcard = version.endsWith(".*");
                PythonVersion v = PythonVersion.parse(wildcard ? version.substring(0, version.length() - 2) : version);
                if (v == null) {
                    continue;
                }
                if (">=".equals(op) || ">".equals(op)) {
                    if (range.lower == null || v.compareTo(range.lower) > 0) {
                        range.lower = v;
                        range.lowerExclusive = ">".equals(op);
                    }
                } else if ("<".equals(op) || "<=".equals(op)) {
                    if (range.upper == null || v.compareTo(range.upper) < 0) {
                        range.upper = v;
                        range.upperInclusive = "<=".equals(op);
                    }
                } else if ("~=".equals(op) || ("==".equals(op) && wildcard)) {
                    if (range.lower == null || v.compareTo(range.lower) > 0) {
                        range.lower = v;
                        range.lowerExclusive = false;
                    }
                    PythonVersion next = bumpTrailingRelease(v);
                    if (next != null && (range.upper == null || next.compareTo(range.upper) < 0)) {
                        range.upper = next;
                        range.upperInclusive = false;
                    }
                } else if ("==".equals(op)) {
                    range.lower = v;
                    range.lowerExclusive = false;
                    range.upper = v;
                    range.upperInclusive = true;
                }
            }
            return range;
        }

        private static @Nullable PythonVersion bumpTrailingRelease(PythonVersion v) {
            long[] release = v.getRelease();
            if (release.length < 2) {
                return null;
            }
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < release.length - 1; i++) {
                if (i > 0) {
                    b.append('.');
                }
                b.append(i == release.length - 2 ? release[i] + 1 : release[i]);
            }
            return PythonVersion.parse(b.toString());
        }

        private void checkPythonBounds(PyRange oldRange) {
            if (oldRange.lower != null && (range.lower == null || range.lower.compareTo(oldRange.lower) < 0)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                        "Lowering the requires-python lower bound would need wheels re-added, " +
                                "which requires resolution");
            }
            boolean upperEqual = (oldRange.upper == null) == (range.upper == null) &&
                    (oldRange.upper == null || (oldRange.upper.compareTo(range.upper) == 0 &&
                            oldRange.upperInclusive == range.upperInclusive));
            if (!upperEqual) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                        "Changing the requires-python upper bound is not supported by the native engine");
            }
        }

        private boolean lowerBoundIncreased() {
            PyRange oldRange = parseRange(lock.getRequiresPython() != null ? lock.getRequiresPython() : "");
            if (range.lower == null) {
                return false;
            }
            return oldRange.lower == null || range.lower.compareTo(oldRange.lower) > 0;
        }

        /**
         * Empirical bump behavior (BEHAVIOR.md §3): filter wheel arrays down by the new
         * lower bound, prune edges whose markers can no longer fire, and drop always-true
         * marker clauses; declared metadata stays untouched.
         */
        private void applyPythonBump() {
            if (lock.getResolutionMarkers() != null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                        "requires-python changes on a forked lock are not supported by the native engine");
            }
            for (int i = 0; i < packages.size(); i++) {
                UvLockPackage pkg = packages.get(i);
                if (pkg.getResolutionMarkers() != null || isDuplicated(pkg.getName())) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg.getName(),
                            "requires-python changes on a forked lock are not supported by the native engine");
                }
                UvLockPackage.UvLockPackageBuilder builder = pkg.toBuilder();
                if (pkg.getWheels() != null) {
                    List<UvLockArtifact> kept = new ArrayList<>();
                    for (UvLockArtifact wheel : pkg.getWheels()) {
                        String filename = wheelFilename(wheel);
                        if (filename == null || wheelAdmittedByLowerBound(filename)) {
                            kept.add(wheel);
                        }
                    }
                    if (kept.isEmpty() && pkg.getSdist() == null) {
                        throw new EngineFailure(new Failure(Reason.PIN_EXCLUDED_BY_PYTHON, pkg.getName(), null,
                                pkg.getName() + "==" + pkg.getVersion() + " has no sdist and no wheel compatible " +
                                        "with requires-python " + rangeDescription() +
                                        "; no installable distribution would remain"));
                    }
                    builder.wheels(kept.isEmpty() ? null : kept);
                }
                builder.dependencies(pruneEdges(pkg.getName(), pkg.getDependencies()));
                builder.optionalDependencies(pruneEdgeGroups(pkg.getName(), pkg.getOptionalDependencies()));
                builder.devDependencies(pruneEdgeGroups(pkg.getName(), pkg.getDevDependencies()));
                packages.set(i, builder.build());
            }
        }

        private @Nullable Map<String, List<UvLockDependency>> pruneEdgeGroups(
                String pkg, @Nullable Map<String, List<UvLockDependency>> groups) {
            if (groups == null) {
                return null;
            }
            Map<String, List<UvLockDependency>> pruned = new LinkedHashMap<>();
            for (Map.Entry<String, List<UvLockDependency>> e : groups.entrySet()) {
                List<UvLockDependency> edges = pruneEdges(pkg, e.getValue());
                if (edges != null) {
                    pruned.put(e.getKey(), edges);
                }
            }
            return pruned.isEmpty() ? null : pruned;
        }

        private @Nullable List<UvLockDependency> pruneEdges(String pkg, @Nullable List<UvLockDependency> edges) {
            if (edges == null) {
                return null;
            }
            List<UvLockDependency> kept = new ArrayList<>();
            for (UvLockDependency edge : edges) {
                if (edge.getMarker() == null) {
                    kept.add(edge);
                    continue;
                }
                List<String[]> clauses = parseSimpleAndChain(edge.getMarker());
                if (clauses == null) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                            "Cannot re-evaluate dependency marker \"" + edge.getMarker() +
                                    "\" under the new requires-python");
                }
                String simplified = simplifyClauses(clauses);
                if (DROP_EDGE.equals(simplified)) {
                    continue;
                }
                kept.add(Objects.equals(simplified, edge.getMarker()) ? edge : edge.withMarker(simplified));
            }
            return kept.isEmpty() ? null : kept;
        }

        private static final String DROP_EDGE = "\0drop";

        /**
         * Drops always-true clauses and signals edge removal on any always-false clause;
         * null result means the whole marker became vacuous.
         */
        private @Nullable String simplifyClauses(List<String[]> clauses) {
            List<String> kept = new ArrayList<>();
            for (String[] clause : clauses) {
                Boolean truth = clauseTruth(clause);
                if (Boolean.FALSE.equals(truth)) {
                    return DROP_EDGE;
                }
                if (truth == null) {
                    kept.add(clause[0] + " " + clause[1] + " '" + clause[2] + "'");
                }
            }
            return kept.isEmpty() ? null : String.join(" and ", kept);
        }

        private @Nullable Boolean clauseTruth(String[] clause) {
            return clauseTruth(range, clause[0], clause[1], clause[2]);
        }

        /**
         * Truth of a simple marker clause over every Python in the lock's requires-python
         * range; null when it varies or cannot be decided. Deliberately still null:
         * non-version variables, unparseable values, python_version compared at micro
         * precision, wildcards under ordering operators, and ~= of a single-component
         * version. Pre-release Pythons at interval boundaries are outside the model,
         * matching the release-ordering treatment of the inequality operators.
         */
        static @Nullable Boolean clauseTruth(PyRange range, String var, String op, String value) {
            if (!VERSION_VARS.contains(var)) {
                return null;
            }
            boolean wildcard = value.endsWith(".*");
            if (wildcard && !"==".equals(op) && !"!=".equals(op)) {
                return null;
            }
            PythonVersion v = PythonVersion.parse(wildcard ? value.substring(0, value.length() - 2) : value);
            if (v == null) {
                return null;
            }
            // python_version truncates to major.minor; micro-precision comparisons diverge
            if ("python_version".equals(var) && v.getRelease().length > 2) {
                return null;
            }
            PythonVersion lower = range.lower;
            PythonVersion upper = range.upper;
            switch (op) {
                case ">=":
                    if (lower != null && lower.compareTo(v) >= 0) {
                        return true;
                    }
                    if (upper != null && (range.upperInclusive ? upper.compareTo(v) < 0 : upper.compareTo(v) <= 0)) {
                        return false;
                    }
                    return null;
                case ">":
                    if (lower != null && (range.lowerExclusive ? lower.compareTo(v) >= 0 : lower.compareTo(v) > 0)) {
                        return true;
                    }
                    if (upper != null && upper.compareTo(v) <= 0) {
                        return false;
                    }
                    return null;
                case "<":
                    if (upper != null && (range.upperInclusive ? upper.compareTo(v) < 0 : upper.compareTo(v) <= 0)) {
                        return true;
                    }
                    if (lower != null && lower.compareTo(v) >= 0) {
                        return false;
                    }
                    return null;
                case "<=":
                    if (upper != null && upper.compareTo(v) <= 0) {
                        return true;
                    }
                    if (lower != null && (range.lowerExclusive ? lower.compareTo(v) >= 0 : lower.compareTo(v) > 0)) {
                        return false;
                    }
                    return null;
                case "==":
                    return equalityTruth(range, var, v, wildcard);
                case "!=": {
                    Boolean eq = equalityTruth(range, var, v, wildcard);
                    return eq == null ? null : !eq;
                }
                case "~=": {
                    // ~=X.Y[.Z] is >=X.Y[.Z], <X.(Y+1) / <X.Y.(Z+1)-family upper
                    if (v.getRelease().length < 2) {
                        return null;
                    }
                    PythonVersion next = bumpTrailingRelease(v);
                    return next == null ? null : intervalTruth(range, v, next);
                }
                default:
                    return null;
            }
        }

        /**
         * ==/!= truth: a wildcard, or python_version's truncated equality, covers the
         * half-open interval [v, next); exact python_full_version equality is a single
         * point, decidable only when v lies outside the range or the range is that point.
         */
        private static @Nullable Boolean equalityTruth(PyRange range, String var, PythonVersion v, boolean wildcard) {
            if (wildcard || "python_version".equals(var)) {
                PythonVersion next = bumpLastComponent(v, wildcard ? v.getRelease().length : 2);
                return next == null ? null : intervalTruth(range, v, next);
            }
            if (range.lower != null && range.upper != null &&
                    range.lower.compareTo(v) == 0 && range.upper.compareTo(v) == 0 &&
                    !range.lowerExclusive && range.upperInclusive) {
                return true;
            }
            if ((range.lower != null && (range.lowerExclusive ? range.lower.compareTo(v) >= 0 : range.lower.compareTo(v) > 0)) ||
                    (range.upper != null && (range.upperInclusive ? range.upper.compareTo(v) < 0 : range.upper.compareTo(v) <= 0))) {
                return false;
            }
            return null;
        }

        /**
         * Truth of "python in [lo, hi)" over the whole range: true when the range is
         * contained, false when disjoint, null when it straddles a boundary.
         */
        private static @Nullable Boolean intervalTruth(PyRange range, PythonVersion lo, PythonVersion hi) {
            if (range.lower != null && range.lower.compareTo(lo) >= 0 &&
                    range.upper != null &&
                    (range.upperInclusive ? range.upper.compareTo(hi) < 0 : range.upper.compareTo(hi) <= 0)) {
                return true;
            }
            if (range.lower != null && range.lower.compareTo(hi) >= 0) {
                return false;
            }
            if (range.upper != null && (range.upperInclusive ? range.upper.compareTo(lo) < 0 : range.upper.compareTo(lo) <= 0)) {
                return false;
            }
            return null;
        }

        private static @Nullable PythonVersion bumpLastComponent(PythonVersion v, int components) {
            if (components < 1) {
                return null;
            }
            long[] release = v.getRelease();
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < components; i++) {
                if (i > 0) {
                    b.append('.');
                }
                long part = i < release.length ? release[i] : 0;
                b.append(i == components - 1 ? part + 1 : part);
            }
            return PythonVersion.parse(b.toString());
        }

        /**
         * uv's wheel filtering considers only the requires-python lower bound against the
         * wheel's Python tag (FORMAT.md §8): abi3 and generic py3 tags always stay, while
         * minor-versioned tags below the bound are dropped.
         */
        private boolean wheelAdmittedByLowerBound(String filename) {
            if (range.lower == null) {
                return true;
            }
            long[] release = range.lower.getRelease();
            long lowerMajor = release.length > 0 ? release[0] : 0;
            long lowerMinor = release.length > 1 ? release[1] : 0;

            String base = filename.substring(0, filename.length() - ".whl".length());
            String[] segments = base.split("-");
            if (segments.length < 5) {
                return true;
            }
            String[] pythonTags = segments[segments.length - 3].split("\\.");
            String[] abiTags = segments[segments.length - 2].split("\\.");
            boolean abi3 = false;
            for (String abiTag : abiTags) {
                if ("abi3".equals(abiTag)) {
                    abi3 = true;
                    break;
                }
            }
            for (String tag : pythonTags) {
                int digits = 0;
                while (digits < tag.length() && !Character.isDigit(tag.charAt(digits))) {
                    digits++;
                }
                if (digits == 0 || digits == tag.length()) {
                    return true; // unrecognized tag: keep rather than silently drop
                }
                String impl = tag.substring(0, digits);
                String version = tag.substring(digits);
                long major = version.charAt(0) - '0';
                long minor = version.length() > 1 ? parseLongOrZero(version.substring(1)) : -1;
                if ("py".equals(impl)) {
                    // generic tags are forward-compatible within their major version
                    if (major >= lowerMajor) {
                        return true;
                    }
                } else if (abi3) {
                    // stable-ABI wheels support every Python from their tag upward
                    return true;
                } else {
                    if (minor < 0 ? major >= lowerMajor :
                            (major > lowerMajor || (major == lowerMajor && minor >= lowerMinor))) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static long parseLongOrZero(String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        private static @Nullable String wheelFilename(UvLockArtifact wheel) {
            String ref = wheel.getUrl() != null ? wheel.getUrl() : wheel.getPath();
            if (ref == null) {
                return null;
            }
            int slash = ref.lastIndexOf('/');
            String filename = slash >= 0 ? ref.substring(slash + 1) : ref;
            return filename.endsWith(".whl") ? filename : null;
        }

        // ---- version-change edit ----

        /**
         * Rewrites P's package entry for a constraint or index change. Returns false when
         * the previously locked version still satisfies the constraints (minimal update:
         * only the metadata line changed).
         */
        private boolean applyChange(Change change) {
            int index = singlePackageIndex(change.canonicalName);
            UvLockPackage pkg = packages.get(index);
            if (pkg.getResolutionMarkers() != null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, change.canonicalName,
                        "Editing a fork-duplicated package is not supported by the native engine");
            }
            if (pkg.getSource().getType() != UvLockSource.Type.REGISTRY) {
                throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, change.canonicalName,
                        "Editing a " + pkg.getSource().getType().getKey() + "-sourced package is not supported");
            }
            if (pkg.getMetadata() != null || pkg.getDevDependencies() != null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, change.canonicalName,
                        "Editing a workspace member entry is not supported by the native engine");
            }
            PythonVersion pinned = PythonVersion.parse(pkg.getVersion());
            if (pinned == null) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, change.canonicalName,
                        "Locked version " + pkg.getVersion() + " is not a valid PEP 440 version");
            }
            if (!change.indexChanged && satisfiesAll(change.constraints, pinned)) {
                return false; // locked-version preference: only the requires-dist line changes
            }
            if (lock.getOptions() != null && lock.getOptions().getExcludeNewer() != null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, change.canonicalName,
                        "Version selection under [options] exclude-newer is not supported");
            }
            checkNoUvResolutionOverrides(change.canonicalName);

            Listing listing = fetchListing(change.canonicalName, change.pinnedIndexName);
            if (listing.index.isFlat()) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, change.canonicalName,
                        "Rewriting entries served from a flat index is not supported by the native engine");
            }
            Map<PythonVersion, List<PackageFile>> byVersion = groupByVersion(listing.listing, change.canonicalName);
            PythonVersion selected = selectVersion(byVersion, change, pinned);
            if (selected == null) {
                throw new EngineFailure(new Failure(Reason.RESOLUTION_CONFLICT, change.canonicalName,
                        listing.index.getIndex().getUrl(),
                        "No non-yanked version satisfying the declared constraints" +
                                (range.lower != null ? " for python " + range.lower : "") +
                                " found at " + listing.index.getIndex().getUrl()));
            }
            List<PackageFile> files = byVersion.get(selected);
            CoreMetadata metadata = fetchMetadata(change.canonicalName, listing.index, files);
            List<UvLockDependency> edges = buildEdges(change.canonicalName, metadata, pkg);

            PackageFile sdistFile = null;
            List<PackageFile> wheelFiles = new ArrayList<>();
            for (PackageFile file : files) {
                DistFilename dist = DistFilename.parse(file.getFilename());
                if (dist == null) {
                    continue;
                }
                if (dist.getType() == DistFilename.Type.SDIST && sdistFile == null) {
                    sdistFile = file;
                } else if (dist.getType() == DistFilename.Type.WHEEL &&
                        wheelAdmittedByLowerBound(file.getFilename())) {
                    wheelFiles.add(file);
                }
            }
            if (sdistFile == null && wheelFiles.isEmpty()) {
                throw new EngineFailure(new Failure(Reason.RESOLUTION_CONFLICT, change.canonicalName,
                        listing.index.getIndex().getUrl(), "No installable distribution for " + selected));
            }
            List<UvLockArtifact> wheels = new ArrayList<>();
            for (PackageFile wheel : wheelFiles) {
                wheels.add(toArtifact(change.canonicalName, listing.index, wheel));
            }
            packages.set(index, pkg.toBuilder()
                    .version(selected.toString())
                    .source(UvLockSource.registry(listing.index.getIndex().getUrl()))
                    .dependencies(edges.isEmpty() ? null : edges)
                    .sdist(sdistFile != null ? toArtifact(change.canonicalName, listing.index, sdistFile) : null)
                    .wheels(wheels.isEmpty() ? null : wheels)
                    .build());
            return true;
        }

        private void checkNoUvResolutionOverrides(String pkg) {
            Toml.Table uvTable = findTable("tool.uv");
            if (uvTable != null) {
                for (Toml value : uvTable.getValues()) {
                    if (value instanceof Toml.KeyValue) {
                        String key = keyName((Toml.KeyValue) value);
                        if ("constraint-dependencies".equals(key) || "override-dependencies".equals(key)) {
                            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                                    "Version selection under [tool.uv] " + key + " is not supported");
                        }
                    }
                }
            }
        }

        private static boolean satisfiesAll(List<PythonVersionSpecifierSet> constraints, PythonVersion v) {
            for (PythonVersionSpecifierSet constraint : constraints) {
                // the previously locked pin is accepted even when it is a pre-release
                if (!constraint.contains(v, true)) {
                    return false;
                }
            }
            return true;
        }

        private int singlePackageIndex(String canonicalName) {
            int found = findSinglePackage(canonicalName);
            if (found < 0) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, canonicalName,
                        "Declared package has no [[package]] entry in the lock");
            }
            return found;
        }

        private int findSinglePackage(String canonicalName) {
            int found = -1;
            for (int i = 0; i < packages.size(); i++) {
                if (packages.get(i).getName().equals(canonicalName)) {
                    if (found >= 0) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, canonicalName,
                                "Editing a fork-duplicated package is not supported by the native engine");
                    }
                    found = i;
                }
            }
            return found;
        }

        private static final class Listing {
            final UvIndex index;
            final PackageListing listing;

            Listing(UvIndex index, PackageListing listing) {
                this.index = index;
                this.listing = listing;
            }
        }

        /**
         * uv's index strategy: consult eligible indexes in configuration order, first one
         * listing the package wins.
         */
        private Listing fetchListing(String canonicalName, @Nullable String pinnedIndexName) {
            PythonIndexException notFound = null;
            for (UvIndex index : indexes) {
                if (!index.usableFor(pinnedIndexName)) {
                    continue;
                }
                if (index.isFlat()) {
                    // a package absent from a flat listing is not an error, just not there
                    PackageListing listing = flatClient.listFiles(index.getIndex(), canonicalName);
                    if (!listing.getFiles().isEmpty()) {
                        return new Listing(index, listing);
                    }
                    continue;
                }
                try {
                    PackageListing listing = simpleClient.listFiles(index.getIndex(), canonicalName);
                    if (!listing.getFiles().isEmpty()) {
                        return new Listing(index, listing);
                    }
                } catch (PythonIndexException e) {
                    if (e.getReason() != PythonIndexException.Reason.NOT_FOUND) {
                        throw e;
                    }
                    notFound = e;
                }
            }
            if (notFound != null) {
                throw notFound;
            }
            throw new EngineFailure(new Failure(Reason.PACKAGE_NOT_FOUND, canonicalName, null,
                    "No configured index lists " + canonicalName));
        }

        private Map<PythonVersion, List<PackageFile>> groupByVersion(PackageListing listing, String pkg) {
            Map<PythonVersion, List<PackageFile>> byVersion = new LinkedHashMap<>();
            for (PackageFile file : listing.getFiles()) {
                DistFilename dist = DistFilename.parse(file.getFilename());
                if (dist == null || !pkg.equals(Pep508Requirement.canonicalize(dist.getDistribution()))) {
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
                                                      Change change, PythonVersion pinned) {
            // Locked preference survives an index switch when the pinned version is listed there
            if (change.indexChanged && byVersion.containsKey(pinned) &&
                    satisfiesAll(change.constraints, pinned) &&
                    versionAdmitsPython(byVersion.get(pinned))) {
                return pinned;
            }
            PythonVersion best = null;
            for (Map.Entry<PythonVersion, List<PackageFile>> candidate : byVersion.entrySet()) {
                PythonVersion version = candidate.getKey();
                boolean satisfies = true;
                for (PythonVersionSpecifierSet constraint : change.constraints) {
                    if (!constraint.contains(version)) {
                        satisfies = false;
                        break;
                    }
                }
                if (!satisfies) {
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
                if (!installable || !versionAdmitsPython(candidate.getValue())) {
                    continue;
                }
                if (best == null || version.compareTo(best) > 0) {
                    best = version;
                }
            }
            return best;
        }

        /**
         * A version admits the lock's Python range when any of its files declares no
         * Requires-Python or one containing the range's lower bound.
         */
        private boolean versionAdmitsPython(List<PackageFile> files) {
            if (range.lower == null) {
                return true;
            }
            for (PackageFile file : files) {
                String requires = file.getRequiresPython();
                if (requires == null) {
                    return true;
                }
                PythonVersionSpecifierSet set = PythonVersionSpecifierSet.parse(requires);
                if (set == null || set.contains(range.lower, true)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * The metadata ladder (ADR 0009 §3): PEP 658 sidecar, lazy/full wheel read, then
         * sdist PKG-INFO gated on PEP 643 static metadata.
         */
        private CoreMetadata fetchMetadata(String pkg, UvIndex index, List<PackageFile> files) {
            String indexUrl = index.getIndex().getUrl();
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
                    return metadata;
                }
                if (sdist == null) {
                    throw new EngineFailure(new Failure(Reason.INDEX_UNREACHABLE, pkg, indexUrl,
                            "Could not fetch dependency metadata for " + wheel.getFilename()));
                }
            }
            if (sdist == null) {
                throw new EngineFailure(new Failure(Reason.RESOLUTION_CONFLICT, pkg, indexUrl,
                        "No wheel or sdist distribution available"));
            }
            CoreMetadata metadata = SdistMetadataReader.read(http, sdist.getUrl());
            if (metadata == null) {
                throw new EngineFailure(new Failure(Reason.DYNAMIC_SDIST_METADATA, pkg, indexUrl,
                        "Could not read PKG-INFO from " + sdist.getFilename()));
            }
            if (!metadata.hasStaticRequiresDist()) {
                throw new EngineFailure(new Failure(Reason.DYNAMIC_SDIST_METADATA, pkg, indexUrl,
                        "Sdist metadata of " + sdist.getFilename() + " does not declare static " +
                                "Requires-Dist (PEP 643); resolving would require executing a build"));
            }
            return metadata;
        }

        /**
         * Derives P's new dependency edges from its requires-dist. Every target must
         * already be locked and satisfy the requirement at its pinned version; anything
         * else needs delta resolution.
         */
        private List<UvLockDependency> buildEdges(String pkg, CoreMetadata metadata, UvLockPackage oldPkg) {
            List<UvLockDependency> oldEdges = oldPkg.getDependencies() != null ?
                    oldPkg.getDependencies() : emptyList();
            List<UvLockDependency> edges = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (String raw : metadata.getRequiresDist()) {
                Pep508Requirement requirement = Pep508Requirement.parse(raw);
                if (requirement == null) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                            "Unparseable requirement of the new version: " + raw);
                }
                if (requirement.getUrl() != null) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                            "The new version requires a direct URL dependency: " + raw);
                }
                int semi = raw.indexOf(';');
                String rawMarker = requirement.getMarker() != null && semi >= 0 ?
                        raw.substring(semi + 1).trim() : null;
                if (rawMarker != null && referencesExtraVariable(rawMarker)) {
                    checkExtraGatedRequirementDroppable(pkg, oldPkg, raw);
                    continue;
                }
                String marker = edgeMarker(pkg, requirement.getCanonicalName(), rawMarker, oldEdges);
                if (DROP_EDGE.equals(marker)) {
                    continue;
                }
                String target = requirement.getCanonicalName();
                if (!seen.add(target)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                            "Multiple requirements on " + target + " need marker-aware merging");
                }
                int targetIndex = findSinglePackage(target);
                if (targetIndex < 0) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                            "Requirement " + raw + " of the new version is not in the existing lock; " +
                                    "delta resolution is not yet supported by the native engine");
                }
                UvLockPackage locked = packages.get(targetIndex);
                PythonVersion lockedVersion = PythonVersion.parse(locked.getVersion());
                if (requirement.getSpecifiers() != null && (lockedVersion == null ||
                        !requirement.getSpecifiers().contains(lockedVersion, true))) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                            "Requirement " + raw + " of the new version is not satisfied by the pinned " +
                                    target + "==" + locked.getVersion() + "; delta resolution is not yet supported");
                }
                List<String> extra = null;
                if (!requirement.getExtras().isEmpty()) {
                    extra = new ArrayList<>();
                    for (String e : requirement.getExtras()) {
                        String canonical = Pep508Requirement.canonicalize(e);
                        if (locked.getOptionalDependencies() == null ||
                                !locked.getOptionalDependencies().containsKey(canonical)) {
                            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                                    "Requirement " + raw + " needs extra " + canonical + " that " + target +
                                            " was not locked with");
                        }
                        extra.add(canonical);
                    }
                    // uv records edge extras sorted, unlike declared requires-dist extras (g3-multi-extras)
                    Collections.sort(extra);
                }
                edges.add(new UvLockDependency(target, null, null, extra, marker));
            }
            edges.sort(Comparator.comparing(UvLockDependency::getName));
            return edges;
        }

        private static boolean referencesExtraVariable(String marker) {
            return Pattern.compile("\\bextra\\b").matcher(marker).find();
        }

        /**
         * Extra-gated requirements of P vanish from the lock unless some edge activates
         * the extra; verify no activation exists before dropping.
         */
        private void checkExtraGatedRequirementDroppable(String pkg, UvLockPackage oldPkg, String raw) {
            if (oldPkg.getOptionalDependencies() != null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                        "Rebuilding [package.optional-dependencies] groups is not supported by the native engine");
            }
            for (UvLockPackage other : packages) {
                for (List<UvLockDependency> edges : allEdgeLists(other)) {
                    for (UvLockDependency edge : edges) {
                        if (edge.getName().equals(pkg) && edge.getExtra() != null) {
                            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                                    "An edge activates an extra of " + pkg + "; rebuilding optional " +
                                            "dependency groups is not supported (" + raw + ")");
                        }
                    }
                }
            }
        }

        private @Nullable String edgeMarker(String pkg, String target, @Nullable String rawMarker,
                                            List<UvLockDependency> oldEdges) {
            if (rawMarker == null) {
                return null;
            }
            String normalized = recordMarker(rawMarker, null);
            if (normalized != null) {
                List<String[]> clauses = parseSimpleAndChain(normalized);
                return clauses != null ? simplifyClauses(clauses) : normalized;
            }
            for (UvLockDependency oldEdge : oldEdges) {
                if (oldEdge.getName().equals(target) && oldEdge.getMarker() != null &&
                        Objects.equals(Marker.parse(rawMarker), Marker.parse(oldEdge.getMarker()))) {
                    return oldEdge.getMarker();
                }
            }
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                    "Cannot reproduce uv's recorded form of dependency marker \"" + rawMarker + "\"");
        }

        private UvLockArtifact toArtifact(String pkg, UvIndex index, PackageFile file) {
            String sha256 = file.getSha256() != null ? file.getSha256() : downloadAndHash(file.getUrl());
            if (sha256 == null) {
                throw new EngineFailure(new Failure(Reason.HASH_UNAVAILABLE, pkg, index.getIndex().getUrl(),
                        "The index lists " + file.getFilename() +
                                " without a sha256 digest, and it could not be downloaded to hash"));
            }
            String uploadTime = lock.expectsUploadTime() ? formatUploadTime(file.getUploadTime()) : null;
            return new UvLockArtifact(file.getUrl(), null, "sha256:" + sha256, file.getSize(), uploadTime);
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

        // ---- removal ----

        /**
         * The lock's graph makes removal exact: drop the root's edge; the reachability
         * sweep afterwards removes anything the removal orphaned.
         */
        private void applyRemoval(Removal removal) {
            int removedIndex = findSinglePackage(removal.canonicalName);
            if (removedIndex >= 0) {
                UvLockSource.Type type = packages.get(removedIndex).getSource().getType();
                if (type != UvLockSource.Type.REGISTRY) {
                    throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, removal.canonicalName,
                            "Removing a " + type.getKey() + "-sourced dependency is not supported by the native engine");
                }
            }
            UvLockPackage root = packages.get(rootIndex);
            UvLockPackage.UvLockPackageBuilder builder = root.toBuilder();
            if (removal.devGroup != null) {
                builder.devDependencies(removeGroupEdge(root.getDevDependencies(), removal.devGroup, removal.canonicalName));
            } else if (removal.extraGroup != null) {
                builder.optionalDependencies(removeGroupEdge(root.getOptionalDependencies(), removal.extraGroup, removal.canonicalName));
            } else {
                List<UvLockDependency> edges = removeEdge(root.getDependencies(), removal.canonicalName);
                builder.dependencies(edges != null && edges.isEmpty() ? null : edges);
            }
            packages.set(rootIndex, builder.build());
        }

        private @Nullable Map<String, List<UvLockDependency>> removeGroupEdge(
                @Nullable Map<String, List<UvLockDependency>> groups, String group, String name) {
            if (groups == null) {
                return null;
            }
            Map<String, List<UvLockDependency>> updated = new LinkedHashMap<>(groups);
            List<UvLockDependency> edges = removeEdge(updated.get(group), name);
            if (edges == null || edges.isEmpty()) {
                updated.remove(group);
            } else {
                updated.put(group, edges);
            }
            return updated.isEmpty() ? null : updated;
        }

        private @Nullable List<UvLockDependency> removeEdge(@Nullable List<UvLockDependency> edges, String name) {
            if (edges == null) {
                return null;
            }
            List<UvLockDependency> updated = new ArrayList<>();
            for (UvLockDependency edge : edges) {
                if (edge.getName().equals(name)) {
                    if (edge.getVersion() != null || edge.getSource() != null) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                                "Removing a fork-disambiguated dependency edge is not supported");
                    }
                    continue;
                }
                updated.add(edge);
            }
            return updated;
        }

        private void dropUnreachable() {
            Set<String> reachable = new HashSet<>();
            List<String> frontier = new ArrayList<>();
            // only the root is a permanent seed; other editable/path packages must be reached via edges
            String rootName = packages.get(rootIndex).getName();
            reachable.add(rootName);
            frontier.add(rootName);
            while (!frontier.isEmpty()) {
                String name = frontier.remove(frontier.size() - 1);
                for (UvLockPackage pkg : packages) {
                    if (!pkg.getName().equals(name)) {
                        continue;
                    }
                    for (List<UvLockDependency> edges : allEdgeLists(pkg)) {
                        for (UvLockDependency edge : edges) {
                            if (reachable.add(edge.getName())) {
                                frontier.add(edge.getName());
                            }
                        }
                    }
                }
            }
            List<UvLockPackage> kept = new ArrayList<>();
            for (UvLockPackage pkg : packages) {
                if (reachable.contains(pkg.getName())) {
                    kept.add(pkg);
                }
            }
            packages = kept;
            rootIndex = findRoot();
        }

        private static List<List<UvLockDependency>> allEdgeLists(UvLockPackage pkg) {
            List<List<UvLockDependency>> lists = new ArrayList<>();
            if (pkg.getDependencies() != null) {
                lists.add(pkg.getDependencies());
            }
            if (pkg.getOptionalDependencies() != null) {
                lists.addAll(pkg.getOptionalDependencies().values());
            }
            if (pkg.getDevDependencies() != null) {
                lists.addAll(pkg.getDevDependencies().values());
            }
            return lists;
        }

        private boolean isDuplicated(String name) {
            int count = 0;
            for (UvLockPackage pkg : packages) {
                if (pkg.getName().equals(name)) {
                    count++;
                }
            }
            return count > 1;
        }

        // ---- python-bump pin validation ----

        /**
         * After a requires-python bump every surviving pin's Requires-Python (listing data)
         * must admit the new lower bound.
         */
        private void validatePinsAgainstPython(Set<String> rewritten) {
            List<String> violatingPackages = new ArrayList<>();
            List<String> violations = new ArrayList<>();
            for (UvLockPackage pkg : packages) {
                UvLockSource.Type type = pkg.getSource().getType();
                if (type != UvLockSource.Type.REGISTRY || rewritten.contains(pkg.getName())) {
                    continue;
                }
                PythonVersion pinned = PythonVersion.parse(pkg.getVersion());
                if (pinned == null) {
                    continue;
                }
                List<String> pins = indexPins.get(pkg.getName());
                Listing listing;
                try {
                    listing = fetchListing(pkg.getName(), pins != null ? pins.get(0) : null);
                } catch (PythonIndexException e) {
                    throw new EngineFailure(indexFailure(pkg.getName(), e));
                }
                List<PackageFile> files = groupByVersion(listing.listing, pkg.getName()).get(pinned);
                if (files == null || files.isEmpty()) {
                    violatingPackages.add(pkg.getName());
                    violations.add(pkg.getName() + " (pinned " + pkg.getVersion() +
                            " no longer listed by the index; Requires-Python unverifiable)");
                } else if (!versionAdmitsPython(files)) {
                    violatingPackages.add(pkg.getName());
                    violations.add(pkg.getName() + "==" + pkg.getVersion());
                }
            }
            if (!violations.isEmpty()) {
                throw new EngineFailure(new Failure(Reason.PIN_EXCLUDED_BY_PYTHON,
                        violatingPackages.size() == 1 ? violatingPackages.get(0) : null,
                        null,
                        "Pinned versions incompatible with requires-python " + lock.getRequiresPython() +
                                " -> " + rangeDescription() + ": " + String.join(", ", violations)));
            }
        }

        private String rangeDescription() {
            return range.lower != null ? ">=" + range.lower : "(unbounded)";
        }

        // ---- marker normalization (verified subset) ----

        /**
         * uv's recorded marker form for the cataloged subset: normalized spacing, single
         * quotes, {@code python_version} to {@code python_full_version} for order-preserving
         * comparisons, and and-chain clauses sorted by variable. Null when the marker is
         * outside the subset.
         */
        private @Nullable String recordMarker(String rawMarker, @Nullable String extraClause) {
            List<String[]> clauses = parseSimpleAndChain(rawMarker);
            if (clauses == null) {
                return null;
            }
            List<String[]> mapped = new ArrayList<>();
            for (String[] clause : clauses) {
                String var = clause[0];
                String op = clause[1];
                String value = clause[2];
                if (value.indexOf('\'') >= 0 || value.indexOf('"') >= 0) {
                    return null;
                }
                if (VERSION_VARS.contains(var)) {
                    // only >= and < survive the python_version -> python_full_version rename unchanged
                    if (!">=".equals(op) && !"<".equals(op)) {
                        return null;
                    }
                    mapped.add(new String[]{"python_full_version", op, value});
                } else if ("==".equals(op) || "!=".equals(op)) {
                    mapped.add(clause);
                } else {
                    return null;
                }
            }
            if (extraClause != null) {
                List<String[]> extra = parseSimpleAndChain(extraClause);
                if (extra == null) {
                    return null;
                }
                mapped.addAll(extra);
            }
            // stable sort: uv orders and-chains alphabetically by variable
            mapped.sort(Comparator.comparing(a -> a[0]));
            List<String> parts = new ArrayList<>(mapped.size());
            for (String[] clause : mapped) {
                parts.add(clause[0] + " " + clause[1] + " '" + clause[2] + "'");
            }
            return String.join(" and ", parts);
        }

        /**
         * Splits a marker into simple {@code var op 'value'} clauses joined by {@code and};
         * null for anything richer (or-chains, parentheses, reversed operands).
         */
        static @Nullable List<String[]> parseSimpleAndChain(String marker) {
            List<String> parts = splitTopLevelAnd(marker);
            if (parts == null) {
                return null;
            }
            List<String[]> clauses = new ArrayList<>(parts.size());
            for (String part : parts) {
                Matcher m = SIMPLE_CLAUSE.matcher(part);
                if (!m.matches()) {
                    return null;
                }
                clauses.add(new String[]{m.group(1), m.group(2), m.group(4)});
            }
            return clauses;
        }

        private static @Nullable List<String> splitTopLevelAnd(String marker) {
            if (marker.indexOf('(') >= 0 || marker.indexOf(')') >= 0) {
                return null;
            }
            List<String> parts = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            char quote = 0;
            String[] tokens = marker.split("(?<=\\s)|(?=\\s)");
            // scan word tokens, honoring quoted strings, so "and"/"or" inside values are kept
            int i = 0;
            while (i < tokens.length) {
                String token = tokens[i];
                if (quote == 0 && ("and".equals(token))) {
                    parts.add(current.toString());
                    current.setLength(0);
                    i++;
                    continue;
                }
                if (quote == 0 && "or".equals(token)) {
                    return null;
                }
                for (int c = 0; c < token.length(); c++) {
                    char ch = token.charAt(c);
                    if (quote == 0 && (ch == '\'' || ch == '"')) {
                        quote = ch;
                    } else if (quote == ch) {
                        quote = 0;
                    }
                }
                current.append(token);
                i++;
            }
            parts.add(current.toString());
            return parts;
        }

        // ---- TOML helpers ----

        private Toml.@Nullable Table findTable(String name) {
            for (TomlValue value : pyproject.getValues()) {
                if (value instanceof Toml.Table) {
                    Toml.Table table = (Toml.Table) value;
                    if (name.equals(tableName(table))) {
                        return table;
                    }
                }
            }
            return null;
        }

        private static @Nullable String tableName(Toml.Table table) {
            Toml.Identifier name = table.getName();
            return name != null ? name.getName() : null;
        }

        private static @Nullable String keyName(Toml.KeyValue kv) {
            if (kv.getKey() instanceof Toml.Identifier) {
                return unquote(((Toml.Identifier) kv.getKey()).getName());
            }
            return null;
        }

        private static String unquote(String s) {
            if (s.length() >= 2 && (s.charAt(0) == '"' || s.charAt(0) == '\'') &&
                    s.charAt(s.length() - 1) == s.charAt(0)) {
                return s.substring(1, s.length() - 1);
            }
            return s;
        }

        private static @Nullable String literalString(Toml.Table table, String key) {
            for (Toml value : table.getValues()) {
                if (value instanceof Toml.KeyValue) {
                    Toml.KeyValue kv = (Toml.KeyValue) value;
                    if (key.equals(keyName(kv)) && kv.getValue() instanceof Toml.Literal) {
                        Object v = ((Toml.Literal) kv.getValue()).getValue();
                        if (v instanceof String) {
                            return (String) v;
                        }
                    }
                }
            }
            return null;
        }

        private List<String> stringArray(Toml.Table table, String key) {
            for (Toml value : table.getValues()) {
                if (value instanceof Toml.KeyValue) {
                    Toml.KeyValue kv = (Toml.KeyValue) value;
                    if (key.equals(keyName(kv))) {
                        if (!(kv.getValue() instanceof Toml.Array)) {
                            throw new EngineFailure(Reason.MALFORMED_MANIFEST, null,
                                    "Expected an array for " + key);
                        }
                        return stringArrayValues((Toml.Array) kv.getValue(), key);
                    }
                }
            }
            return emptyList();
        }

        private List<String> stringArrayValues(Toml.Array array, String context) {
            List<String> strings = new ArrayList<>();
            for (Toml element : array.getValues()) {
                if (element instanceof Toml.Empty) {
                    continue; // trailing comma
                }
                Object v = element instanceof Toml.Literal ? ((Toml.Literal) element).getValue() : null;
                if (!(v instanceof String)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                            "Non-string entry in " + context + " (e.g. include-group) is not supported");
                }
                strings.add((String) v);
            }
            return strings;
        }
    }
}
