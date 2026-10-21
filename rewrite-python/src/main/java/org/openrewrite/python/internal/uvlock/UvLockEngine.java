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
import org.openrewrite.python.PythonExecutionContextView;
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
import static java.util.Collections.sort;
import static java.util.stream.Collectors.toList;

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
        /** What live dependents require of the package, which shapes selection but may itself be about to move. */
        final List<PythonVersionSpecifierSet> dependentBounds = new ArrayList<>();
        @Nullable String pinnedIndexName;
        boolean indexChanged;
        /** Root edges whose declared extras changed, keyed by dev group ("" for [project] dependencies). */
        final Map<String, List<String>> rootEdgeExtras = new LinkedHashMap<>();

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
        final @Nullable List<String> extras;

        Addition(String canonicalName, @Nullable String devGroup, @Nullable String marker, @Nullable List<String> extras) {
            this.canonicalName = canonicalName;
            this.devGroup = devGroup;
            this.marker = marker;
            this.extras = extras;
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
        private final @Nullable String uvVersion;

        private List<UvLockPackage> packages;
        private int rootIndex;
        private UvLockMetadata oldMetadata;
        private PyRange range;
        // set when a requires-python bump collapsed a forked lock to a single environment, so the
        // top-level resolution-markers are dropped
        private boolean collapsedForks;
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
        // [tool.uv] constraint-dependencies, by canonical name: uv bounds every resolution by them
        private final Map<String, UvLockRequirement> constraintDependencies = new TreeMap<>();

        Run(Toml.Document pyproject, UvLock lock, ExecutionContext ctx) {
            this.pyproject = pyproject;
            this.lock = lock;
            this.http = HttpSenderExecutionContextView.view(ctx).getHttpSender();
            this.simpleClient = new SimpleIndexClient(http);
            this.flatClient = new FlatIndexClient(http);
            this.indexes = UvIndexDiscovery.discover(ctx, pyproject, null, Environment.SYSTEM);
            this.indexPins = UvIndexDiscovery.sourceIndexPins(pyproject);
            this.nonRegistrySources = UvLockMetadataBuilder.readNonRegistrySources(pyproject);
            this.uvVersion = PythonExecutionContextView.view(ctx).getUvVersion();
        }

        Result regenerate() {
            try {
                return run();
            } catch (EngineFailure e) {
                return Result.failure(e.failure);
            }
        }

        private Result run() {
            if (lock.getManifest() != null && lock.getManifest().getMembers() != null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                        "Workspace locks ([manifest] with members) are not supported by the native engine");
            }
            Integer writeRevision = configuredRevision();
            packages = new ArrayList<>(lock.getPackages());
            rootIndex = findRoot();
            UvLockPackage root = packages.get(rootIndex);
            oldMetadata = root.getMetadata() != null ? root.getMetadata() :
                    UvLockMetadata.builder().build();
            readConstraintDependencies();

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
                if (collapsedForks) {
                    updated = updated.withResolutionMarkers(null);
                }
                structural = true;
            }
            // cascades below bound targets only by what stays live, so the root edits come first
            for (Removal removal : removals) {
                applyRemoval(removal);
                structural = true;
            }
            if (constraintsChanged()) {
                UvLockManifest manifest = (lock.getManifest() != null ? lock.getManifest() : new UvLockManifest(null, null, null))
                        .withConstraints(constraintDependencies.isEmpty() ? null : new ArrayList<>(constraintDependencies.values()));
                updated = updated.withManifest(manifest.getConstraints() == null && manifest.getBuildConstraints() == null ?
                        null : manifest);
                structural = true;
                for (String name : constrainedLockedNames(constraintDependencies.values())) {
                    Change change = new Change(name);
                    change.constraints.addAll(gatherConstraints(name));
                    List<String> pins = indexPins.get(name);
                    change.pinnedIndexName = pins != null ? pins.get(0) : null;
                    changes.add(change);
                }
            }
            Set<String> rewritten = new HashSet<>();
            Set<String> resolved = new HashSet<>();
            for (Change change : changes) {
                if (!resolved.add(change.canonicalName)) {
                    continue;
                }
                change.dependentBounds.addAll(gatherConstraints(change.canonicalName));
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
                        structural |= revisitSettled(target);
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
                        structural |= revisitSettled(target);
                        continue;
                    }
                    resolveAddition(target, null);
                    rewritten.add(target);
                    structural = true;
                }
            }
            if (structural) {
                dropUnreachable();
            }
            verifyDependents(rewritten);
            if (pythonChanged && lowerBoundIncreased()) {
                validatePinsAgainstPython(rewritten);
            }

            updated = updated.withPackages(packages);
            String written = UvLockWriter.write(updated);
            String original = UvLockWriter.write(lock);
            if (written.equals(original)) {
                return Result.success(written);
            }
            if (writeRevision != null && (lock.getRevision() == null || lock.getRevision() < 3)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                        "uv " + uvVersion + " rewrites a lock below revision 3 in its own shape, " +
                                "which the native engine does not reproduce");
            }
            if (writesReachabilityMarkers()) {
                if (lock.getSupportedMarkers() != null || lock.getRequiredMarkers() != null) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                            "Changing a lock with restricted environments ([tool.uv] environments / " +
                                    "required-environments) is not supported by the native engine");
                }
                updated = updated.withPackages(UvLockReachability.simplify(packages, rootIndex, range));
                written = UvLockWriter.write(updated);
            }
            // uv restamps a lock it changed with its own revision
            if (writeRevision != null && !writeRevision.equals(lock.getRevision())) {
                written = UvLockWriter.write(updated.withRevision(writeRevision));
            }
            return Result.success(written);
        }

        // uv simplifies edge markers under parent reachability from 0.11.25. Without a configured release,
        // a revision-3-or-later lock is taken to be written by a release that does.
        private boolean writesReachabilityMarkers() {
            if (uvVersion != null) {
                PythonVersion version = PythonVersion.parse(uvVersion);
                return version != null && version.compareTo(PythonVersion.parse("0.11.25")) >= 0;
            }
            return lock.getRevision() != null && lock.getRevision() >= 3;
        }

        /** The revision the configured uv release writes, 5 from 0.12.22 on and 3 before, or null without a release. */
        private @Nullable Integer configuredRevision() {
            if (uvVersion == null) {
                return null;
            }
            PythonVersion version = PythonVersion.parse(uvVersion);
            if (version == null) {
                throw new EngineFailure(Reason.MALFORMED_MANIFEST, null,
                        "Configured uv version is not a valid version: " + uvVersion);
            }
            return version.compareTo(PythonVersion.parse("0.12.22")) >= 0 ? 5 : 3;
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

        private @Nullable String projectVersion(Toml.Table project, @Nullable String oldVersion) {
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
                PythonVersionSpecifierSet constraint = constraintSpecifiers(change.canonicalName);
                if (constraint != null) {
                    change.constraints.add(constraint);
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
                    // T2 add (ADR 0010): resolve the new package and its closure. A declaration under a
                    // root optional-dependencies group is deferred to a later increment.
                    if (newReqs.size() > 1 || UvLockRequirements.extraGroupOf(added) != null) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, added.getName(),
                                "Adding a dependency under a [project.optional-dependencies] group is not yet " +
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
                    additions.add(new Addition(added.getName(), devGroup, added.getMarker(), edgeExtras(added.getExtras())));
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
                if (!Objects.equals(oldReq.getExtras(), newReq.getExtras())) {
                    change.rootEdgeExtras.put(devGroup == null ? "" : devGroup, edgeExtras(newReq.getExtras()));
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

        // uv records edge extras sorted, unlike declared requires-dist extras (g3-multi-extras)
        private static @Nullable List<String> edgeExtras(@Nullable List<String> declared) {
            if (declared == null || declared.isEmpty()) {
                return null;
            }
            List<String> extras = new ArrayList<>(declared);
            sort(extras);
            return extras;
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

        // an unchanged range keeps the recorded header text, whatever spelling declares it
        private String normalizeRequiresPython(@Nullable String declared) {
            if (declared == null) {
                throw new EngineFailure(Reason.MALFORMED_MANIFEST, null,
                        "pyproject.toml [project] has no requires-python");
            }
            String normalized;
            try {
                normalized = UvLockNormalization.normalizeRequiresPython(declared);
            } catch (IllegalArgumentException e) {
                throw new EngineFailure(Reason.MALFORMED_MANIFEST, null,
                        "Unparseable requires-python '" + declared + "'. " + e.getMessage());
            }
            if (lock.getRequiresPython() != null &&
                    PyRange.parse(normalized).sameBounds(PyRange.parse(lock.getRequiresPython()))) {
                return lock.getRequiresPython();
            }
            return normalized;
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
         * marker clauses; declared metadata stays untouched. On a forked lock, only a bump
         * that lifts the floor past every fork boundary -- collapsing it to one environment --
         * is supported: the eliminated fork branches drop, the surviving entry loses its
         * resolution-markers, and its edges revert from fork-disambiguated to plain.
         */
        private void applyPythonBump() {
            if (lock.getResolutionMarkers() != null) {
                if (lock.getSupportedMarkers() != null || lock.getRequiredMarkers() != null) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                            "requires-python changes on a lock with restricted environments are not supported");
                }
                if (!collapsesToSingleEnvironment()) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                            "requires-python changes that leave the lock forked are not supported by the native engine");
                }
                collapsedForks = true;
            }
            List<UvLockPackage> kept = new ArrayList<>();
            for (UvLockPackage pkg : packages) {
                if (pkg.getResolutionMarkers() != null && allBranchesEliminated(pkg.getResolutionMarkers())) {
                    continue; // this fork branch's Python range is gone under the higher floor
                }
                UvLockPackage.UvLockPackageBuilder builder = pkg.toBuilder();
                if (pkg.getResolutionMarkers() != null) {
                    builder.resolutionMarkers(null); // collapsed to a single environment
                }
                // url/git-pinned artifacts are chosen explicitly, not by Python tag; never filter them
                if (pkg.getSource().getType() == UvLockSource.Type.REGISTRY && pkg.getWheels() != null) {
                    List<UvLockArtifact> keptWheels = new ArrayList<>();
                    for (UvLockArtifact wheel : pkg.getWheels()) {
                        String filename = wheelFilename(wheel);
                        if (filename == null || wheelAdmittedByRange(filename)) {
                            keptWheels.add(wheel);
                        }
                    }
                    if (keptWheels.isEmpty() && pkg.getSdist() == null) {
                        throw new EngineFailure(new Failure(Reason.PIN_EXCLUDED_BY_PYTHON, pkg.getName(), null,
                                pkg.getName() + "==" + pkg.getVersion() + " has no sdist and no wheel compatible " +
                                        "with requires-python " + range.describe() +
                                        "; no installable distribution would remain"));
                    }
                    builder.wheels(keptWheels.isEmpty() ? null : keptWheels);
                }
                builder.dependencies(pruneEdges(pkg.getName(), pkg.getDependencies()));
                builder.optionalDependencies(pruneEdgeGroups(pkg.getName(), pkg.getOptionalDependencies()));
                builder.devDependencies(pruneEdgeGroups(pkg.getName(), pkg.getDevDependencies()));
                kept.add(builder.build());
            }
            packages = kept;
            rootIndex = findRoot();
        }

        /**
         * A forked lock collapses to a single environment when, under the new range, exactly one
         * top-level resolution-marker branch is always true and every other is always false. A branch
         * that still straddles the range (or one this engine cannot evaluate) means the lock stays
         * forked, which is out of scope.
         */
        private boolean collapsesToSingleEnvironment() {
            int survivors = 0;
            for (String marker : lock.getResolutionMarkers()) {
                Boolean truth = branchTruth(marker);
                if (Boolean.TRUE.equals(truth)) {
                    survivors++;
                } else if (truth == null) {
                    return false;
                }
            }
            return survivors == 1;
        }

        private boolean allBranchesEliminated(List<String> markers) {
            for (String marker : markers) {
                if (!Boolean.FALSE.equals(branchTruth(marker))) {
                    return false;
                }
            }
            return true;
        }

        /** True/false when a resolution-marker branch is always/never in the new range; null when it straddles. */
        private @Nullable Boolean branchTruth(String marker) {
            List<String[]> clauses = UvLockMarkers.parseSimpleAndChain(marker);
            if (clauses == null) {
                return null;
            }
            String simplified = UvLockMarkers.simplifyClauses(range, clauses);
            if (UvLockMarkers.DROP_EDGE.equals(simplified)) {
                return Boolean.FALSE;
            }
            return simplified == null ? Boolean.TRUE : null;
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
                    kept.add(deforked(edge));
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
                kept.add(Objects.equals(simplified, edge.getMarker()) && !collapsedForks ? edge :
                        new UvLockDependency(edge.getName(), null, null, edge.getExtra(), simplified));
            }
            return kept.isEmpty() ? null : kept;
        }

        /**
         * A collapsed fork de-duplicates every package, so an edge that carried a {@code version}
         * / {@code source} to disambiguate fork copies reverts to the plain form.
         */
        private UvLockDependency deforked(UvLockDependency edge) {
            if (!collapsedForks || (edge.getVersion() == null && edge.getSource() == null)) {
                return edge;
            }
            return new UvLockDependency(edge.getName(), null, null, edge.getExtra(), edge.getMarker());
        }

        /**
         * uv's wheel filtering considers only the requires-python lower bound against the
         * wheel's Python tag (FORMAT.md §8): abi3 and generic py3 tags always stay, while
         * minor-versioned tags below the bound are dropped.
         */
        private boolean wheelAdmittedByRange(String filename) {
            if (range.lower == null && range.upper == null) {
                return true;
            }
            long[] release = range.lower != null ? range.lower.getRelease() : new long[0];
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
                    // an interpreter-specific tag covers one minor, which must meet the lower bound and start
                    // below the upper one
                    boolean aboveLower = minor < 0 ? major >= lowerMajor :
                            (major > lowerMajor || (major == lowerMajor && minor >= lowerMinor));
                    if (aboveLower && minorStartsBelowUpper(major, minor)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean minorStartsBelowUpper(long major, long minor) {
            if (range.upper == null) {
                return true;
            }
            PythonVersion start = PythonVersion.parse(major + "." + Math.max(minor, 0));
            int cmp = start == null ? -1 : start.compareTo(range.upper);
            return range.upperInclusive ? cmp <= 0 : cmp < 0;
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
            boolean extrasChanged = updateRootEdgeExtras(change);
            if (!change.indexChanged && satisfiesAll(change.constraints, pinned)) {
                // locked-version preference: the entry only changes if an extra was newly activated on it
                return ensureActivatedGroups(index) || extrasChanged;
            }
            packages.set(index, resolveEntry(change, pinned, pkg));
            return true;
        }

        // the root edge records the declaration's extras, which is what activates the package's groups
        private boolean updateRootEdgeExtras(Change change) {
            if (change.rootEdgeExtras.isEmpty()) {
                return false;
            }
            UvLockPackage root = packages.get(rootIndex);
            UvLockPackage.UvLockPackageBuilder builder = root.toBuilder();
            Map<String, List<UvLockDependency>> dev = root.getDevDependencies() != null ?
                    new LinkedHashMap<>(root.getDevDependencies()) : new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> e : change.rootEdgeExtras.entrySet()) {
                List<UvLockDependency> edges = e.getKey().isEmpty() ? root.getDependencies() : dev.get(e.getKey());
                List<UvLockDependency> updated = new ArrayList<>();
                for (UvLockDependency edge : edges != null ? edges : Collections.<UvLockDependency>emptyList()) {
                    updated.add(edge.getName().equals(change.canonicalName) ? edge.withExtra(e.getValue()) : edge);
                }
                if (e.getKey().isEmpty()) {
                    builder.dependencies(updated);
                } else {
                    dev.put(e.getKey(), updated);
                    builder.devDependencies(dev);
                }
            }
            packages.set(rootIndex, builder.build());
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
            if (lock.getOptions() != null && lock.getOptions().getExcludeNewerPackage() != null &&
                    lock.getOptions().getExcludeNewerPackage().get(change.canonicalName) instanceof String) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, change.canonicalName,
                        "Version selection under [options] exclude-newer-package is not supported");
            }
            checkNoUvResolutionOverrides(change.canonicalName);
            if (lock.getSupportedMarkers() != null || lock.getRequiredMarkers() != null) {
                // uv resolves a restricted-environments lock per environment, dropping edges gated on
                // an unsupported platform; the engine's requires-python-only simplification can't.
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, change.canonicalName,
                        "Resolving " + change.canonicalName + " in a lock with restricted environments " +
                                "([tool.uv] environments / required-environments) is not supported by the native engine");
            }

            Listing listing = fetchListing(change.canonicalName, change.pinnedIndexName);
            if (listing.index.isFlat()) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, change.canonicalName,
                        "Rewriting entries served from a flat index is not supported by the native engine");
            }
            Map<PythonVersion, List<PackageFile>> byVersion = groupByVersion(listing.listing, change.canonicalName);
            PythonVersion selected = selectUnderDependents(byVersion, change, pinned);
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
            Set<String> activated = activatedExtras(change.canonicalName);
            requireActivatedExtrasGated(change.canonicalName, selected.toString(), metadata, activated);
            ResolvedEdges edges = buildEdges(change.canonicalName, metadata, base, activated, true);

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
                        wheelAdmittedByRange(file.getFilename())) {
                    wheelFiles.add(file);
                }
            }
            if (sdistFile == null && wheelFiles.isEmpty()) {
                throw new EngineFailure(new Failure(Reason.RESOLUTION_CONFLICT, change.canonicalName,
                        listing.index.getIndex().getUrl(), "No installable distribution for " + selected));
            }
            // uv records wheels in filename order, whatever order the index lists them in
            wheelFiles.sort(Comparator.comparing(PackageFile::getFilename));
            List<UvLockArtifact> wheels = new ArrayList<>();
            for (PackageFile wheel : wheelFiles) {
                wheels.add(toArtifact(change.canonicalName, listing.index, wheel));
            }
            return base.toBuilder()
                    .version(selected.toString())
                    .source(UvLockSource.registry(listing.index.getIndex().getUrl()))
                    .dependencies(edges.base.isEmpty() ? null : edges.base)
                    .optionalDependencies(edges.groups.isEmpty() ? null : edges.groups)
                    .sdist(sdistFile != null ? toArtifact(change.canonicalName, listing.index, sdistFile) : null)
                    .wheels(wheels.isEmpty() ? null : wheels)
                    .build();
        }

        // dependents' bounds first, the declaration alone when the dependents are the ones moving (a lockstep pair)
        private @Nullable PythonVersion selectUnderDependents(Map<PythonVersion, List<PackageFile>> byVersion,
                                                              Change change, @Nullable PythonVersion pinned) {
            if (change.dependentBounds.isEmpty()) {
                return selectVersion(byVersion, change, change.constraints, pinned);
            }
            List<PythonVersionSpecifierSet> bounded = new ArrayList<>(change.constraints);
            bounded.addAll(change.dependentBounds);
            PythonVersion selected = selectVersion(byVersion, change, bounded, pinned);
            return selected != null ? selected : selectVersion(byVersion, change, change.constraints, pinned);
        }

        // an untouched dependent the new version no longer satisfies is one uv would revise, which the engine does not
        private void verifyDependents(Set<String> rewritten) {
            Map<String, Set<String>> live = liveExtras();
            for (String name : rewritten) {
                int index = findSinglePackage(name);
                if (index < 0) {
                    continue;
                }
                PythonVersion version = PythonVersion.parse(packages.get(index).getVersion());
                if (version == null) {
                    continue;
                }
                forEachDependentRequirement(name, live, (dependent, raw, requirement) -> {
                    if (!rewritten.contains(dependent.getName()) && requirement.getSpecifiers() != null &&
                            !requirement.getSpecifiers().contains(version, true)) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                                dependent.getName() + " " + dependent.getVersion() + " requires " + raw + ", which " +
                                        name + " " + version + " does not satisfy; uv would revise " + dependent.getName() +
                                        " or fail, and the native engine does neither");
                    }
                });
            }
        }

        private interface RequirementVisitor {
            void visit(UvLockPackage dependent, String raw, Pep508Requirement requirement);
        }

        // every requirement a live registry dependent places on target and whose marker can fire
        private void forEachDependentRequirement(String target, Map<String, Set<String>> live, RequirementVisitor visitor) {
            for (UvLockPackage q : packages) {
                if (q.getSource().getType() != UvLockSource.Type.REGISTRY || !live.containsKey(q.getName()) ||
                        !dependsOn(q, target)) {
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
                    int semi = raw.indexOf(';');
                    String rawMarker = req.getMarker() != null && semi >= 0 ? raw.substring(semi + 1).trim() : null;
                    if (rawMarker != null && !requirementCanFire(q, rawMarker, live)) {
                        continue;
                    }
                    visitor.visit(q, raw, req);
                }
            }
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
            if (direct != null) {
                // the root edge goes in first: its extras are what activate the new entry's groups
                addRootEdge(direct);
            }
            Change select = new Change(name);
            select.constraints.addAll(gatherConstraints(name));
            List<String> pins = indexPins.get(name);
            select.pinnedIndexName = pins != null ? pins.get(0) : null;
            insertSorted(resolveEntry(select, null, UvLockPackage.builder().name(name).build()));
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
            UvLockDependency edge = new UvLockDependency(addition.canonicalName, null, null, addition.extras, addition.marker);
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
            Map<String, Set<String>> live = liveExtras();
            forEachDependentRequirement(target, live, (dependent, raw, req) -> {
                PythonVersionSpecifierSet specs = req.getSpecifiers();
                if (specs != null && !specs.isMatchAll()) {
                    constraints.add(specs);
                }
            });
            PythonVersionSpecifierSet constraint = constraintSpecifiers(target);
            if (constraint != null) {
                constraints.add(constraint);
            }
            UvLockMetadata rootMeta = packages.get(rootIndex).getMetadata();
            if (rootMeta != null) {
                List<List<UvLockRequirement>> sections = new ArrayList<>();
                if (rootMeta.getRequiresDist() != null) {
                    sections.add(rootMeta.getRequiresDist());
                }
                if (rootMeta.getRequiresDev() != null) {
                    sections.addAll(rootMeta.getRequiresDev().values());
                }
                for (List<UvLockRequirement> section : sections) {
                    for (UvLockRequirement req : section) {
                        if (!req.getName().equals(target) || req.getSpecifier() == null) {
                            continue;
                        }
                        // every root extra and dependency group is locked, so only requires-python
                        // can rule a root declaration out
                        if (req.getMarker() != null && !requirementCanFire(packages.get(rootIndex), req.getMarker(), live)) {
                            continue;
                        }
                        PythonVersionSpecifierSet specs = PythonVersionSpecifierSet.parse(req.getSpecifier());
                        if (specs != null) {
                            constraints.add(specs);
                        }
                    }
                }
            }
            return constraints;
        }

        // a marker outside the simple and-chain subset cannot be decided and is kept, as uv's union would be
        private boolean requirementCanFire(UvLockPackage dependent, String rawMarker, Map<String, Set<String>> live) {
            List<String[]> clauses = UvLockMarkers.parseSimpleAndChain(rawMarker);
            if (clauses == null) {
                return true;
            }
            List<String[]> environmentClauses = new ArrayList<>();
            for (String[] clause : clauses) {
                if (!"extra".equals(clause[0])) {
                    environmentClauses.add(clause);
                } else if ("==".equals(clause[1]) && dependent.getSource().getType() == UvLockSource.Type.REGISTRY) {
                    if (!live.getOrDefault(dependent.getName(), Collections.emptySet())
                            .contains(Pep508Requirement.canonicalize(clause[2]))) {
                        return false;
                    }
                }
            }
            return !UvLockMarkers.DROP_EDGE.equals(UvLockMarkers.simplifyClauses(range, environmentClauses));
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
         * the greedy engine does not. An extra the new edge activates on it gets its group.
         */
        private boolean revisitSettled(String target) {
            int index = findSinglePackage(target);
            if (index < 0) {
                return false;
            }
            PythonVersion version = PythonVersion.parse(packages.get(index).getVersion());
            if (version == null) {
                return false;
            }
            for (PythonVersionSpecifierSet constraint : gatherConstraints(target)) {
                if (!constraint.contains(version, true)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, target,
                            "Updating " + target + " to satisfy a new requirement would require revising an " +
                                    "already-updated package (backtracking), which the native engine does not support");
                }
            }
            return ensureActivatedGroups(index);
        }

        /**
         * Builds the optional-dependencies groups of extras that live edges activate on a package
         * but its entry lacks, from the metadata of its locked version. Recorded groups and the
         * base edges stay untouched.
         */
        private boolean ensureActivatedGroups(int index) {
            UvLockPackage pkg = packages.get(index);
            Set<String> missing = new TreeSet<>(activatedExtras(pkg.getName()));
            if (pkg.getOptionalDependencies() != null) {
                missing.removeAll(pkg.getOptionalDependencies().keySet());
            }
            if (missing.isEmpty()) {
                return false;
            }
            CoreMetadata metadata = pkg.getSource().getType() == UvLockSource.Type.REGISTRY ? metadataFor(pkg) : null;
            if (metadata == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg.getName(),
                        "Cannot read the metadata of " + pkg.getName() + " " + pkg.getVersion() +
                                " to build its optional-dependencies groups " + missing);
            }
            requireActivatedExtrasGated(pkg.getName(), pkg.getVersion(), metadata, missing);
            Map<String, List<UvLockDependency>> groups = new TreeMap<>();
            if (pkg.getOptionalDependencies() != null) {
                groups.putAll(pkg.getOptionalDependencies());
            }
            groups.putAll(buildEdges(pkg.getName(), metadata, pkg, missing, false).groups);
            packages.set(index, pkg.toBuilder().optionalDependencies(groups).build());
            return true;
        }

        private Set<String> activatedExtras(String pkg) {
            return liveExtras().getOrDefault(pkg, Collections.emptySet());
        }

        private void checkNoUvResolutionOverrides(String pkg) {
            Toml.Table uvTable = UvLockToml.findTable(pyproject, "tool.uv");
            if (uvTable != null) {
                for (Toml value : uvTable.getValues()) {
                    if (value instanceof Toml.KeyValue) {
                        String key = UvLockToml.keyName((Toml.KeyValue) value);
                        if ("override-dependencies".equals(key) ||
                                "environments".equals(key) || "required-environments".equals(key)) {
                            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                                    "Version selection under [tool.uv] " + key + " is not supported");
                        }
                    }
                }
            }
        }

        // ---- [tool.uv] constraint-dependencies ----

        // only the plain name-and-specifier shape uv records under [manifest] constraints is reproduced
        private void readConstraintDependencies() {
            Toml.Table uv = UvLockToml.findTable(pyproject, "tool.uv");
            if (uv == null) {
                return;
            }
            for (String spec : UvLockToml.stringArray(uv, "constraint-dependencies")) {
                Pep508Requirement requirement = Pep508Requirement.parse(spec);
                if (requirement == null) {
                    throw new EngineFailure(Reason.MALFORMED_MANIFEST, null,
                            "Unparseable constraint-dependencies entry: " + spec);
                }
                String name = requirement.getCanonicalName();
                if (requirement.getMarker() != null || !requirement.getExtras().isEmpty() ||
                        requirement.getUrl() != null || constraintDependencies.containsKey(name)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                            "Only one plain version constraint per package is supported under [tool.uv] " +
                                    "constraint-dependencies: " + spec);
                }
                constraintDependencies.put(name, new UvLockRequirement(name, null, null, null,
                        UvLockMetadataBuilder.normalizeSpecifier(requirement), null, null, null, null, null));
            }
        }

        private @Nullable PythonVersionSpecifierSet constraintSpecifiers(String name) {
            UvLockRequirement constraint = constraintDependencies.get(name);
            return constraint != null && constraint.getSpecifier() != null ?
                    PythonVersionSpecifierSet.parse(constraint.getSpecifier()) : null;
        }

        // uv rewrites [manifest] constraints only when the ones on locked packages differ from those recorded
        private boolean constraintsChanged() {
            List<UvLockRequirement> recorded = lock.getManifest() != null && lock.getManifest().getConstraints() != null ?
                    lock.getManifest().getConstraints() : emptyList();
            Set<String> locked = new HashSet<>();
            for (UvLockPackage pkg : packages) {
                locked.add(pkg.getName());
            }
            Set<UvLockRequirement> before = new HashSet<>();
            for (UvLockRequirement constraint : recorded) {
                if (locked.contains(constraint.getName())) {
                    before.add(constraint);
                }
            }
            Set<UvLockRequirement> after = new HashSet<>();
            for (UvLockRequirement constraint : constraintDependencies.values()) {
                if (locked.contains(constraint.getName())) {
                    after.add(constraint);
                }
            }
            return !before.equals(after);
        }

        private List<String> constrainedLockedNames(Collection<UvLockRequirement> constraints) {
            List<String> names = new ArrayList<>();
            for (UvLockRequirement constraint : constraints) {
                if (findSinglePackage(constraint.getName()) >= 0) {
                    names.add(constraint.getName());
                }
            }
            return names;
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

        private @Nullable PythonVersion selectVersion(Map<PythonVersion, List<PackageFile>> byVersion, Change change,
                                                      List<PythonVersionSpecifierSet> constraints, PythonVersion pinned) {
            // Locked preference survives an index switch when the pinned version is listed there
            if (change.indexChanged && byVersion.containsKey(pinned) &&
                    satisfiesAll(constraints, pinned) &&
                    versionAdmitsPython(byVersion.get(pinned))) {
                return pinned;
            }
            PythonVersion best = null;
            for (Map.Entry<PythonVersion, List<PackageFile>> candidate : byVersion.entrySet()) {
                PythonVersion version = candidate.getKey();
                boolean satisfies = true;
                for (PythonVersionSpecifierSet constraint : constraints) {
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

        /** The edges one version's requires-dist yields, split into the base list and one list per activated extra. */
        private static final class ResolvedEdges {
            final List<UvLockDependency> base = new ArrayList<>();
            final Map<String, List<UvLockDependency>> groups = new TreeMap<>();
        }

        /**
         * Derives P's dependency edges from its requires-dist: the unconditional requirements
         * when {@code withBase}, plus each requirement gated on one of {@code groups} as an
         * entry of that extra's group. Targets the lock lacks, or holds at a version or without
         * an extra the requirement needs, are queued for the greedy-forward drain.
         */
        private ResolvedEdges buildEdges(String pkg, CoreMetadata metadata, UvLockPackage oldPkg,
                                         Set<String> groups, boolean withBase) {
            ResolvedEdges edges = new ResolvedEdges();
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
                List<String[]> placements = rawMarker != null && UvLockMarkers.referencesExtraVariable(rawMarker) ?
                        extraTerms(pkg, rawMarker) : Collections.singletonList(new String[]{null, rawMarker});
                String target = requirement.getCanonicalName();
                for (String[] placement : placements) {
                    String group = placement[0];
                    if (group == null ? !withBase : !groups.contains(group)) {
                        continue;
                    }
                    List<UvLockDependency> oldEdges = group == null ? oldPkg.getDependencies() :
                            oldPkg.getOptionalDependencies() != null ? oldPkg.getOptionalDependencies().get(group) : null;
                    String marker = edgeMarker(pkg, target, placement[1], oldEdges != null ? oldEdges : emptyList());
                    if (UvLockMarkers.DROP_EDGE.equals(marker)) {
                        continue;
                    }
                    if (marker != null && markeredAddClosure.contains(pkg)) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                                "Adding a conditional dependency whose closure has a marker-gated edge (" +
                                        target + ") requires marker-space resolution, " +
                                        "which the native engine does not support");
                    }
                    if (!seen.add(group + '\0' + target)) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                                "Multiple requirements on " + target + " need marker-aware merging");
                    }
                    UvLockDependency edge = edgeTo(pkg, requirement, marker);
                    (group == null ? edges.base : edges.groups.computeIfAbsent(group, k -> new ArrayList<>())).add(edge);
                }
            }
            edges.base.sort(Comparator.comparing(UvLockDependency::getName));
            for (List<UvLockDependency> entries : edges.groups.values()) {
                entries.sort(Comparator.comparing(UvLockDependency::getName));
            }
            return edges;
        }

        // each disjunct with one extra == clause goes to that extra's group, the rest of it as the entry marker
        private List<String[]> extraTerms(String pkg, String rawMarker) {
            List<List<String[]>> terms = UvLockMarkers.parseDisjunction(rawMarker);
            List<String[]> placements = new ArrayList<>();
            boolean supported = terms != null;
            for (List<String[]> term : supported ? terms : Collections.<List<String[]>>emptyList()) {
                String extra = null;
                List<String> rest = new ArrayList<>();
                for (String[] clause : term) {
                    if (!"extra".equals(clause[0])) {
                        rest.add(clause[0] + " " + clause[1] + " " + quoteMarkerValue(clause[2]));
                    } else if (extra == null && "==".equals(clause[1])) {
                        extra = Pep508Requirement.canonicalize(clause[2]);
                    } else {
                        supported = false;
                    }
                }
                if (extra == null) {
                    supported = false;
                }
                placements.add(new String[]{extra, rest.isEmpty() ? null : String.join(" and ", rest)});
            }
            if (!supported) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                        "Extra-gated requirement marker \"" + rawMarker + "\" is not a disjunction of and-chains with " +
                                "one extra == clause each; placing it in optional-dependencies groups is not supported by " +
                                "the native engine");
            }
            return placements;
        }

        private static String quoteMarkerValue(String value) {
            return value.indexOf('\'') < 0 ? "'" + value + "'" : "\"" + value + "\"";
        }

        /**
         * The edge for one requirement of P. A target the lock lacks is queued as a transitive
         * addition (ADR 0010 T2); a locked target the requirement needs newer, or with an extra
         * its entry lacks, is queued as a cascade (T1), which also builds the missing group once
         * this edge is in place. The edge itself is version-agnostic either way.
         */
        private UvLockDependency edgeTo(String pkg, Pep508Requirement requirement, @Nullable String marker) {
            String target = requirement.getCanonicalName();
            List<String> extras = null;
            if (!requirement.getExtras().isEmpty()) {
                extras = new ArrayList<>();
                for (String e : requirement.getExtras()) {
                    extras.add(Pep508Requirement.canonicalize(e));
                }
                // uv records edge extras sorted, unlike declared requires-dist extras (g3-multi-extras)
                sort(extras);
            }
            int targetIndex = findSinglePackage(target);
            if (targetIndex < 0) {
                additionQueue.add(target);
                if (markeredAddClosure.contains(pkg)) {
                    markeredAddClosure.add(target);
                }
                return new UvLockDependency(target, null, null, extras, marker);
            }
            UvLockPackage locked = packages.get(targetIndex);
            PythonVersion lockedVersion = PythonVersion.parse(locked.getVersion());
            boolean needsNewer = requirement.getSpecifiers() != null && (lockedVersion == null ||
                    !requirement.getSpecifiers().contains(lockedVersion, true));
            boolean lacksExtra = extras != null && (locked.getOptionalDependencies() == null ||
                    !locked.getOptionalDependencies().keySet().containsAll(extras));
            if (needsNewer || lacksExtra) {
                cascadeQueue.add(target);
            }
            return new UvLockDependency(target, null, null, extras, marker);
        }

        // an activated extra that gates nothing makes uv drop the extra from the edge (off14), which is not reproduced
        private void requireActivatedExtrasGated(String pkg, String version, CoreMetadata metadata, Set<String> extras) {
            if (extras.isEmpty()) {
                return;
            }
            Set<String> gated = new HashSet<>();
            for (String raw : metadata.getRequiresDist()) {
                int semi = raw.indexOf(';');
                if (semi < 0) {
                    continue;
                }
                List<List<String[]>> terms = UvLockMarkers.parseDisjunction(raw.substring(semi + 1).trim());
                for (String[] clause : terms == null ? Collections.<String[]>emptyList() :
                        terms.stream().flatMap(List::stream).collect(toList())) {
                    if ("extra".equals(clause[0]) && "==".equals(clause[1])) {
                        gated.add(Pep508Requirement.canonicalize(clause[2]));
                    }
                }
            }
            for (String extra : extras) {
                if (!gated.contains(extra)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                            pkg + " " + version + " does not gate any requirement on extra '" + extra +
                                    "', which an edge activates. The native engine cannot drop the extra from that edge.");
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
                if (clauses == null) {
                    return normalized;
                }
                String simplified = UvLockMarkers.simplifyClauses(range, clauses);
                // uv forks the resolution where a surviving clause splits the Python range
                if (simplified != null && !UvLockMarkers.DROP_EDGE.equals(simplified) &&
                        UvLockMarkers.splitsPythonRange(UvLockMarkers.parseSimpleAndChain(simplified))) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                            "Dependency marker \"" + rawMarker + "\" splits the Python range, which forks " +
                                    "the resolution");
                }
                return simplified;
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

        // each package reachable from the root, mapped to the extras some reachable edge activates on it
        private Map<String, Set<String>> liveExtras() {
            Map<String, List<UvLockPackage>> byName = new HashMap<>();
            for (UvLockPackage pkg : packages) {
                byName.computeIfAbsent(pkg.getName(), k -> new ArrayList<>()).add(pkg);
            }
            Map<String, Set<String>> live = new HashMap<>();
            List<String> frontier = new ArrayList<>();
            // only the root is a permanent seed; other editable/path packages must be reached via edges
            String rootName = packages.get(rootIndex).getName();
            live.put(rootName, new HashSet<>());
            frontier.add(rootName);
            while (!frontier.isEmpty()) {
                String name = frontier.remove(frontier.size() - 1);
                for (UvLockPackage pkg : byName.getOrDefault(name, Collections.emptyList())) {
                    for (List<UvLockDependency> edges : liveEdgeLists(pkg, live)) {
                        for (UvLockDependency edge : edges) {
                            boolean revisit = !live.containsKey(edge.getName());
                            Set<String> extras = live.computeIfAbsent(edge.getName(), k -> new HashSet<>());
                            if (edge.getExtra() != null) {
                                revisit |= extras.addAll(edge.getExtra());
                            }
                            if (revisit) {
                                frontier.add(edge.getName());
                            }
                        }
                    }
                }
            }
            return live;
        }

        private void dropUnreachable() {
            Map<String, Set<String>> liveExtras = liveExtras();
            List<UvLockPackage> kept = new ArrayList<>();
            for (UvLockPackage pkg : packages) {
                if (!liveExtras.containsKey(pkg.getName())) {
                    continue;
                }
                if (pkg.getOptionalDependencies() == null || isRoot(pkg)) {
                    kept.add(pkg);
                    continue;
                }
                Set<String> activated = liveExtras.get(pkg.getName());
                Map<String, List<UvLockDependency>> live = new LinkedHashMap<>();
                for (Map.Entry<String, List<UvLockDependency>> group : pkg.getOptionalDependencies().entrySet()) {
                    if (activated.contains(group.getKey())) {
                        live.put(group.getKey(), group.getValue());
                    }
                }
                kept.add(live.size() == pkg.getOptionalDependencies().size() ? pkg :
                        pkg.toBuilder().optionalDependencies(live.isEmpty() ? null : live).build());
            }
            packages = kept;
            rootIndex = findRoot();
        }

        private boolean isRoot(UvLockPackage pkg) {
            return pkg == packages.get(rootIndex);
        }

        // the root's declared extras are all locked
        private List<List<UvLockDependency>> liveEdgeLists(UvLockPackage pkg, Map<String, Set<String>> activatedExtras) {
            if (isRoot(pkg) || pkg.getOptionalDependencies() == null) {
                return allEdgeLists(pkg);
            }
            List<List<UvLockDependency>> lists = new ArrayList<>();
            if (pkg.getDependencies() != null) {
                lists.add(pkg.getDependencies());
            }
            Set<String> activated = activatedExtras.getOrDefault(pkg.getName(), Collections.emptySet());
            for (Map.Entry<String, List<UvLockDependency>> group : pkg.getOptionalDependencies().entrySet()) {
                if (activated.contains(group.getKey())) {
                    lists.add(group.getValue());
                }
            }
            if (pkg.getDevDependencies() != null) {
                lists.addAll(pkg.getDevDependencies().values());
            }
            return lists;
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
