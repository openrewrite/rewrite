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
        return UvLockMarkers.clauseTruth(PyRange.parse(requiresPython), var, op, value);
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
        String[] parseError = new String[1];
        Toml.Document pyproject = parseToml(pyprojectContent, parseError);
        if (pyproject == null) {
            String detail = "Edited pyproject.toml could not be parsed as TOML";
            if (parseError[0] != null && !parseError[0].isEmpty()) {
                detail += ": " + parseError[0];
            }
            return Result.failure(new Failure(Reason.MALFORMED_MANIFEST, null, null, detail));
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

    private static Toml.@Nullable Document parseToml(String content, String[] parseError) {
        Throwable[] firstError = new Throwable[1];
        SourceFile parsed = new TomlParser()
                .parse(new InMemoryExecutionContext(t -> {
                    if (firstError[0] == null) {
                        firstError[0] = t;
                    }
                }), content)
                .findFirst()
                .orElse(null);
        if (firstError[0] != null) {
            parseError[0] = firstError[0].getMessage();
            return null;
        }
        return parsed instanceof Toml.Document ? (Toml.Document) parsed : null;
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

    /** A directly-declared dependency the manifest adds; gets a new [[package]] entry and a root edge. */
    private static final class Addition {
        final String canonicalName;
        final @Nullable String devGroup;
        /** uv's recorded marker form for the root edge, or null for an unconditional add. */
        final @Nullable String marker;

        Addition(String canonicalName, @Nullable String devGroup, @Nullable String marker) {
            this.canonicalName = canonicalName;
            this.devGroup = devGroup;
            this.marker = marker;
        }
    }

    private static final class Run {
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
        // greedy-forward cascade state (ADR 0010 T1): packages a moved requirement forces to move,
        // and a metadata cache so gathering a target's constraints re-reads each dependent once
        private final Deque<String> cascadeQueue = new ArrayDeque<>();
        private final Map<String, CoreMetadata> metadataCache = new HashMap<>();
        // T2 add state: directly-declared additions from the manifest, and transitive packages
        // their closures pull in (not yet in the lock)
        private final List<Addition> additions = new ArrayList<>();
        private final Deque<String> additionQueue = new ArrayDeque<>();
        // packages resolved under a markered direct add: the whole closure inherits the root
        // marker, so a marker-gated transitive edge would need root/edge marker intersection
        // (fail loud). Unconditional transitive edges keep the closure identical to an
        // unmarkered add, which is byte-identical to uv (verified: python-dateutil -> six).
        private final Set<String> markeredAddClosure = new HashSet<>();

        Run(Toml.Document pyproject, UvLock lock, ExecutionContext ctx) {
            this.pyproject = pyproject;
            this.lock = lock;
            this.http = HttpSenderExecutionContextView.view(ctx).getHttpSender();
            this.simpleClient = new SimpleIndexClient(http);
            this.flatClient = new FlatIndexClient(http);
            this.indexes = UvIndexDiscovery.discover(ctx, pyproject, null, Environment.SYSTEM);
            this.indexPins = UvIndexDiscovery.sourceIndexPins(pyproject);
            this.nonRegistrySources = UvLockMetadataBuilder.readNonRegistrySources(pyproject);
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

            Toml.Table project = UvLockToml.findTable(pyproject, "project");
            if (project == null) {
                throw new EngineFailure(Reason.MALFORMED_MANIFEST, null,
                        "pyproject.toml has no [project] table");
            }
            String projectName = UvLockToml.literalString(project, "name");
            if (projectName == null ||
                    !Pep508Requirement.canonicalize(projectName).equals(Pep508Requirement.canonicalize(root.getName()))) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, projectName,
                        "The [project] name does not match the lock's root package " + root.getName());
            }

            String newRequiresPython = normalizeRequiresPython(UvLockToml.literalString(project, "requires-python"));
            boolean pythonChanged = !newRequiresPython.equals(lock.getRequiresPython());
            range = PyRange.parse(newRequiresPython);
            if (pythonChanged) {
                checkPythonBounds(PyRange.parse(lock.getRequiresPython() != null ? lock.getRequiresPython() : ""));
            }

            UvLockMetadata newMetadata = new UvLockMetadataBuilder(
                    pyproject, oldMetadata, indexes, indexPins, nonRegistrySources).build(project);
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
            Set<String> resolved = new HashSet<>();
            for (Change change : changes) {
                resolved.add(change.canonicalName);
                if (applyChange(change)) {
                    rewritten.add(change.canonicalName);
                    structural = true;
                }
            }
            for (Addition addition : additions) {
                resolved.add(addition.canonicalName);
                resolveAddition(addition.canonicalName, addition);
                rewritten.add(addition.canonicalName);
                structural = true;
            }
            // drain cascades and transitive additions the closures pull in (ADR 0010 T1+T2)
            while (!cascadeQueue.isEmpty() || !additionQueue.isEmpty()) {
                if (!cascadeQueue.isEmpty()) {
                    String target = cascadeQueue.poll();
                    if (!resolved.add(target)) {
                        verifyCascadeSatisfied(target);
                        continue;
                    }
                    Change cascade = new Change(target);
                    cascade.constraints.addAll(gatherConstraints(target));
                    List<String> pins = indexPins.get(target);
                    cascade.pinnedIndexName = pins != null ? pins.get(0) : null;
                    if (applyChange(cascade)) {
                        rewritten.add(target);
                        structural = true;
                    }
                } else {
                    String target = additionQueue.poll();
                    if (!resolved.add(target)) {
                        verifyCascadeSatisfied(target);
                        continue;
                    }
                    resolveAddition(target, null);
                    rewritten.add(target);
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
            String version = UvLockToml.literalString(project, "version");
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
                    // T2 add (ADR 0010): resolve the new package and its closure. Extras, pinned
                    // indexes, and optional-dependency groups on the added declaration are deferred
                    // to a later increment.
                    if (newReqs.size() > 1 || added.getIndex() != null ||
                            (added.getExtras() != null && !added.getExtras().isEmpty()) || UvLockRequirements.extraGroupOf(added) != null) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, added.getName(),
                                "Adding a dependency with extras or a pinned index is not yet " +
                                        "supported by the native engine");
                    }
                    // A python-version-gated add introduces a new fork boundary uv records as
                    // lock-level resolution-markers even when the package resolves to one version;
                    // that is marker-space resolution, so fail loud (added.getMarker() is uv's form).
                    if (added.getMarker() != null && UvLockMarkers.referencesPythonVersion(added.getMarker())) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, added.getName(),
                                "Adding a dependency gated on the Python version alters the lock's fork " +
                                        "structure, which the native engine does not support");
                    }
                    additions.add(new Addition(added.getName(), devGroup, added.getMarker()));
                    continue;
                }
                if (UvLockRequirements.rendered(oldReqs).equals(UvLockRequirements.rendered(newReqs))) {
                    continue;
                }
                if (e.getKey().endsWith(UvLockRequirements.MULTI_EXTRA)) {
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
                if (!Objects.equals(oldReq.getDirectory(), newReq.getDirectory())) {
                    throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, newReq.getName(),
                            "Re-targeting between registry and directory sources is not supported");
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
                    String extraGroup = devGroup == null ? UvLockRequirements.extraGroupOf(req) : null;
                    if (UvLockRequirements.MULTI_EXTRA.equals(extraGroup)) {
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
                    String key = req.getName() + '\0' + UvLockRequirements.extraGroupOf(req);
                    List<UvLockRequirement> reqs = byKey.computeIfAbsent(key, k -> new ArrayList<>());
                    reqs.add(req);
                }
            }
            return byKey;
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
            PyRange oldRange = PyRange.parse(lock.getRequiresPython() != null ? lock.getRequiresPython() : "");
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
                // url/git-pinned artifacts are chosen explicitly, not by Python tag; never filter them
                if (pkg.getSource().getType() == UvLockSource.Type.REGISTRY && pkg.getWheels() != null) {
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
                                        "with requires-python " + range.describe() +
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
                List<String[]> clauses = UvLockMarkers.parseSimpleAndChain(edge.getMarker());
                if (clauses == null) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                            "Cannot re-evaluate dependency marker \"" + edge.getMarker() +
                                    "\" under the new requires-python");
                }
                String simplified = UvLockMarkers.simplifyClauses(range, clauses);
                if (UvLockMarkers.DROP_EDGE.equals(simplified)) {
                    continue;
                }
                kept.add(Objects.equals(simplified, edge.getMarker()) ? edge : edge.withMarker(simplified));
            }
            return kept.isEmpty() ? null : kept;
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
            packages.set(index, resolveEntry(change, pinned, pkg));
            return true;
        }

        /**
         * Resolves {@code change.canonicalName} against its index over the change's constraints
         * and returns a fully built registry entry (version, source, edges, artifacts).
         * {@code base} supplies fields to preserve for a rewrite; a fresh add passes a name-only
         * package with {@code pinned == null}. Cascades and additions the closure needs are queued
         * as a side effect of {@link #buildEdges}.
         */
        private UvLockPackage resolveEntry(Change change, @Nullable PythonVersion pinned, UvLockPackage base) {
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
                        explainNoVersion(byVersion, change, listing.index.getIndex().getUrl())));
            }
            if (wouldFork(byVersion, change, selected)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, change.canonicalName,
                        "Resolving " + change.canonicalName + " would fork it across the requires-python range " +
                                "(uv locks multiple versions gated by python markers); marker-space resolution " +
                                "is not supported by the native engine");
            }
            List<PackageFile> files = byVersion.get(selected);
            CoreMetadata metadata = fetchMetadata(change.canonicalName, listing.index, files);
            List<UvLockDependency> edges = buildEdges(change.canonicalName, metadata, base);

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
            return base.toBuilder()
                    .version(selected.toString())
                    .source(UvLockSource.registry(listing.index.getIndex().getUrl()))
                    .dependencies(edges.isEmpty() ? null : edges)
                    .sdist(sdistFile != null ? toArtifact(change.canonicalName, listing.index, sdistFile) : null)
                    .wheels(wheels.isEmpty() ? null : wheels)
                    .build();
        }

        /**
         * Resolves an added package (direct or transitive) as a fresh registry entry and inserts
         * it in uv's alphabetical package order. Direct manifest additions additionally get a root
         * dependency edge.
         */
        private void resolveAddition(String name, @Nullable Addition direct) {
            if (direct != null && direct.marker != null) {
                markeredAddClosure.add(name);
            }
            Change select = new Change(name);
            select.constraints.addAll(gatherConstraints(name));
            List<String> pins = indexPins.get(name);
            select.pinnedIndexName = pins != null ? pins.get(0) : null;
            UvLockPackage created = resolveEntry(select, null, UvLockPackage.builder().name(name).build());
            insertSorted(created);
            if (direct != null) {
                addRootEdge(direct);
            }
        }

        private void insertSorted(UvLockPackage pkg) {
            int pos = 0;
            while (pos < packages.size() && packages.get(pos).getName().compareTo(pkg.getName()) < 0) {
                pos++;
            }
            packages.add(pos, pkg);
            rootIndex = findRoot();
        }

        private void addRootEdge(Addition addition) {
            UvLockPackage root = packages.get(rootIndex);
            UvLockPackage.UvLockPackageBuilder builder = root.toBuilder();
            UvLockDependency edge = new UvLockDependency(addition.canonicalName, null, null, null, addition.marker);
            if (addition.devGroup != null) {
                Map<String, List<UvLockDependency>> groups = root.getDevDependencies() != null ?
                        new LinkedHashMap<>(root.getDevDependencies()) : new LinkedHashMap<>();
                groups.put(addition.devGroup, addEdgeSorted(groups.get(addition.devGroup), edge));
                builder.devDependencies(groups);
            } else {
                builder.dependencies(addEdgeSorted(root.getDependencies(), edge));
            }
            packages.set(rootIndex, builder.build());
        }

        private static List<UvLockDependency> addEdgeSorted(@Nullable List<UvLockDependency> edges, UvLockDependency edge) {
            List<UvLockDependency> updated = edges != null ? new ArrayList<>(edges) : new ArrayList<>();
            int pos = 0;
            while (pos < updated.size() && updated.get(pos).getName().compareTo(edge.getName()) < 0) {
                pos++;
            }
            updated.add(pos, edge);
            return updated;
        }

        /**
         * Every constraint on {@code target} across the whole lock: each registry dependent's
         * recorded requirement (from its metadata) plus any direct declaration on the root.
         * uv keeps a package at the highest version satisfying <em>all</em> of these, so
         * gathering them completely is what makes a cascade choice reproduce uv's.
         */
        private List<PythonVersionSpecifierSet> gatherConstraints(String target) {
            List<PythonVersionSpecifierSet> constraints = new ArrayList<>();
            for (UvLockPackage q : packages) {
                if (q.getSource().getType() != UvLockSource.Type.REGISTRY || !dependsOn(q, target)) {
                    continue;
                }
                CoreMetadata meta = metadataFor(q);
                if (meta == null) {
                    continue;
                }
                for (String raw : meta.getRequiresDist()) {
                    Pep508Requirement req = Pep508Requirement.parse(raw);
                    if (req == null || !req.getCanonicalName().equals(target)) {
                        continue;
                    }
                    PythonVersionSpecifierSet specs = req.getSpecifiers();
                    if (specs != null && !specs.isMatchAll()) {
                        constraints.add(specs);
                    }
                }
            }
            UvLockMetadata rootMeta = packages.get(rootIndex).getMetadata();
            if (rootMeta != null && rootMeta.getRequiresDist() != null) {
                for (UvLockRequirement req : rootMeta.getRequiresDist()) {
                    if (req.getName().equals(target) && req.getSpecifier() != null) {
                        PythonVersionSpecifierSet specs = PythonVersionSpecifierSet.parse(req.getSpecifier());
                        if (specs != null) {
                            constraints.add(specs);
                        }
                    }
                }
            }
            return constraints;
        }

        private static boolean dependsOn(UvLockPackage pkg, String target) {
            for (List<UvLockDependency> edges : allEdgeLists(pkg)) {
                for (UvLockDependency edge : edges) {
                    if (edge.getName().equals(target)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private @Nullable CoreMetadata metadataFor(UvLockPackage pkg) {
            String key = pkg.getName() + '@' + pkg.getVersion();
            if (metadataCache.containsKey(key)) {
                return metadataCache.get(key);
            }
            CoreMetadata meta = null;
            PythonVersion version = PythonVersion.parse(pkg.getVersion());
            if (version != null) {
                List<String> pins = indexPins.get(pkg.getName());
                Listing listing = fetchListing(pkg.getName(), pins != null ? pins.get(0) : null);
                if (!listing.index.isFlat()) {
                    List<PackageFile> files = groupByVersion(listing.listing, pkg.getName()).get(version);
                    if (files != null && !files.isEmpty()) {
                        meta = fetchMetadata(pkg.getName(), listing.index, files);
                    }
                }
            }
            metadataCache.put(key, meta);
            return meta;
        }

        /**
         * A cascade rediscovered an already-settled package: safe only if its settled version
         * still satisfies the now-complete constraint set. Otherwise uv would backtrack, which
         * the greedy engine does not.
         */
        private void verifyCascadeSatisfied(String target) {
            int index = findSinglePackage(target);
            if (index < 0) {
                return;
            }
            PythonVersion version = PythonVersion.parse(packages.get(index).getVersion());
            if (version == null) {
                return;
            }
            for (PythonVersionSpecifierSet constraint : gatherConstraints(target)) {
                if (!constraint.contains(version, true)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, target,
                            "Updating " + target + " to satisfy a new requirement would require revising an " +
                                    "already-updated package (backtracking), which the native engine does not support");
                }
            }
        }

        private void checkNoUvResolutionOverrides(String pkg) {
            Toml.Table uvTable = UvLockToml.findTable(pyproject, "tool.uv");
            if (uvTable != null) {
                for (Toml value : uvTable.getValues()) {
                    if (value instanceof Toml.KeyValue) {
                        String key = UvLockToml.keyName((Toml.KeyValue) value);
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
         * uv forks a package -- locking multiple versions gated by python markers -- when a
         * version newer than the one admissible at the requires-python floor is itself
         * admissible for a higher part of the range. The greedy engine locks a single version,
         * so this must be detected and failed loud rather than emit a lock uv would disagree with.
         */
        private boolean wouldFork(Map<PythonVersion, List<PackageFile>> byVersion, Change change, PythonVersion selected) {
            if (range.lower == null) {
                return false;
            }
            for (Map.Entry<PythonVersion, List<PackageFile>> candidate : byVersion.entrySet()) {
                PythonVersion version = candidate.getKey();
                if (version.compareTo(selected) <= 0) {
                    continue;
                }
                boolean satisfies = true;
                for (PythonVersionSpecifierSet constraint : change.constraints) {
                    if (!constraint.contains(version)) {
                        satisfies = false;
                        break;
                    }
                }
                if (!satisfies || !installable(candidate.getValue())) {
                    continue;
                }
                // selectVersion skipped this newer version because its requires-python floor
                // excludes the lock's floor; if that floor still lands inside the range, uv would
                // lock it for the upper sub-range and fork
                for (PackageFile file : candidate.getValue()) {
                    String requires = file.getRequiresPython();
                    if (requires == null) {
                        continue;
                    }
                    PyRange fileRange = PyRange.parse(requires);
                    if (fileRange.lower != null && fileRange.lower.compareTo(range.lower) > 0 &&
                            range.withinUpperBound(fileRange.lower)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static boolean installable(List<PackageFile> files) {
            for (PackageFile file : files) {
                DistFilename dist = DistFilename.parse(file.getFilename());
                if (!file.isYanked() && dist != null && dist.getType() != DistFilename.Type.OTHER) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Explain why {@link #selectVersion} found nothing, distinguishing "the index
         * has no version matching the constraints" (a lagging mirror is the usual cause)
         * from a genuine requires-python exclusion or an all-yanked release.
         */
        private String explainNoVersion(Map<PythonVersion, List<PackageFile>> byVersion, Change change, String indexUrl) {
            List<PythonVersion> matching = new ArrayList<>();
            for (PythonVersion version : byVersion.keySet()) {
                if (satisfiesAll(change.constraints, version)) {
                    matching.add(version);
                }
            }
            if (matching.isEmpty()) {
                return "No version matching the declared constraints is available at " + indexUrl +
                        " (the index may lag PyPI or not mirror this release)";
            }
            if (range.lower != null) {
                for (PythonVersion version : matching) {
                    if (!versionAdmitsPython(byVersion.get(version))) {
                        return "The declared constraints match " + version + " at " + indexUrl +
                                ", but its requires-python excludes python " + range.lower;
                    }
                }
            }
            return "All versions matching the declared constraints at " + indexUrl +
                    " are yanked or have no installable distribution";
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
                if (rawMarker != null && UvLockMarkers.referencesExtraVariable(rawMarker)) {
                    checkExtraGatedRequirementDroppable(pkg, oldPkg, raw);
                    continue;
                }
                String marker = edgeMarker(pkg, requirement.getCanonicalName(), rawMarker, oldEdges);
                if (UvLockMarkers.DROP_EDGE.equals(marker)) {
                    continue;
                }
                if (marker != null && markeredAddClosure.contains(pkg)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                            "Adding a conditional dependency whose closure has a marker-gated edge (" +
                                    requirement.getCanonicalName() + ") requires marker-space resolution, " +
                                    "which the native engine does not support");
                }
                String target = requirement.getCanonicalName();
                if (!seen.add(target)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                            "Multiple requirements on " + target + " need marker-aware merging");
                }
                int targetIndex = findSinglePackage(target);
                if (targetIndex < 0) {
                    // T2 (ADR 0010): the requirement pulls in a package not yet in the lock; queue it
                    // as a transitive addition and emit the version-agnostic edge
                    if (!requirement.getExtras().isEmpty()) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                                "Adding transitive " + target + " with extras is not yet supported by the native engine");
                    }
                    additionQueue.add(target);
                    if (markeredAddClosure.contains(pkg)) {
                        markeredAddClosure.add(target);
                    }
                    edges.add(new UvLockDependency(target, null, null, null, marker));
                    continue;
                }
                UvLockPackage locked = packages.get(targetIndex);
                PythonVersion lockedVersion = PythonVersion.parse(locked.getVersion());
                if (requirement.getSpecifiers() != null && (lockedVersion == null ||
                        !requirement.getSpecifiers().contains(lockedVersion, true))) {
                    // greedy-forward cascade (ADR 0010 T1): the new version needs a newer <target>
                    // than the pin; queue it to move and still emit the version-agnostic edge
                    cascadeQueue.add(target);
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
            String normalized = UvLockMarkers.recordMarker(rawMarker, null);
            if (normalized != null) {
                List<String[]> clauses = UvLockMarkers.parseSimpleAndChain(normalized);
                return clauses != null ? UvLockMarkers.simplifyClauses(range, clauses) : normalized;
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
                                " -> " + range.describe() + ": " + String.join(", ", violations)));
            }
        }


    }
}
