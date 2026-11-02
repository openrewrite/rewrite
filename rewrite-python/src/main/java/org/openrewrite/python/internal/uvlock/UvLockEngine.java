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
import org.openrewrite.python.internal.uvlock.marker.MarkerTree;
import org.openrewrite.python.internal.uvlock.marker.MarkerParser;
import org.openrewrite.python.internal.uvlock.marker.MarkerInterner;
import org.openrewrite.python.internal.uvlock.marker.Ranges;
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

import java.time.Instant;
import java.time.Duration;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.sort;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * Native {@code uv.lock} regeneration with minimal-update semantics (ADR 0009): the old
 * lock is the resolution state, the edited pyproject.toml is diffed against the root
 * package's recorded {@code [package.metadata]} (uv's {@code --check} oracle), and only
 * the affected entries are rewritten in uv's own byte-exact emission style. Any edit
 * whose result cannot be proven correct fails loud with a structured {@link Failure}.
 */
public final class UvLockEngine {

    /** The moment a relative exclude-newer counts back from, when set on the context: uv's "now" while it locked. */
    static final String CURRENT_TIME = "org.openrewrite.python.uvlock.currentTime";

    private static final Pattern UPLOAD_TIME = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(?:\\.(\\d+))?Z");

    private UvLockEngine() {
    }

    /** A relative span counts back from {@code now} in UTC, an absolute exclude-newer is its own instant. */
    static @Nullable Instant excludeNewerCutoff(@Nullable UvLockOptions options, Instant now) {
        if (options == null || options.getExcludeNewer() == null) {
            return null;
        }
        String span = options.getExcludeNewerSpan();
        if (span == null) {
            return Instant.parse(options.getExcludeNewer());
        }
        return span.contains("T") ?
                now.minus(Duration.parse(span)) :
                now.atZone(ZoneOffset.UTC).minus(Period.parse(span)).toInstant();
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
        /** A package new to the lock that only another package's requirement brings in. */
        boolean transitiveAddition;
        /** A git or URL declaration the manifest now takes from the registry instead. */
        boolean retargetToRegistry;

        Change(String canonicalName) {
            this.canonicalName = canonicalName;
        }
    }

    private static final class Removal {
        final String canonicalName;
        final @Nullable String extraGroup;
        final @Nullable String devGroup;
        /** The markers of the package's declarations that remain, or null when none does. */
        final @Nullable List<@Nullable String> remainingMarkers;

        Removal(String canonicalName, @Nullable String extraGroup, @Nullable String devGroup) {
            this(canonicalName, extraGroup, devGroup, null);
        }

        Removal(String canonicalName, @Nullable String extraGroup, @Nullable String devGroup,
                @Nullable List<@Nullable String> remainingMarkers) {
            this.canonicalName = canonicalName;
            this.extraGroup = extraGroup;
            this.devGroup = devGroup;
            this.remainingMarkers = remainingMarkers;
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
        // a configured release rewrites a changed lock below revision 3 at its own revision, upload times included
        private boolean migrating;

        private List<UvLockPackage> packages;
        private int rootIndex;
        private UvLockMetadata oldMetadata;
        private PyRange range;
        /**
         * Per queued addition, the lowest Python its incoming edges reach it from, absent once an edge reaches it
         * from the range's own floor. Selection admits a version from there, as uv resolves it within that fork.
         */
        private final Map<String, PythonVersion> additionFloors = new HashMap<>();
        private final Set<String> unflooredAdditions = new HashSet<>();
        private @Nullable PythonVersion selectionFloor;
        /** New packages taken below their newest to keep locked versions, with the locked packages that blocked. */
        private final Map<String, Set<String>> backedOff = new LinkedHashMap<>();
        /** Locked targets a marker-gated requirement moves in only part of the range, split once the drain settles. */
        private final Set<String> pendingForkMoves = new LinkedHashSet<>();
        /** An entry for the forks below a requires-python split, which the caller adds beside the one it gets. */
        private @Nullable UvLockPackage lowerForkEntry;
        /** The lock's forks as this run's requirements split them, in uv's split order, or null until one does. */
        private @Nullable List<String> forks;
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
        // [tool.uv] override-dependencies, by canonical name: each replaces every requirement on its package
        private final Map<String, Pep508Requirement> overrideDependencies = new TreeMap<>();

        // uv lists only the files uploaded before this moment
        private final @Nullable Instant excludeNewerCutoff;

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
            Instant now = ctx.getMessage(CURRENT_TIME);
            this.excludeNewerCutoff = excludeNewerCutoff(lock.getOptions(), now != null ? now : Instant.now());
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
            // environments may also come from uv.toml or the command line, which the engine cannot see
            if (!sameEnvironments(declaredEnvironments(), lock.getSupportedMarkers())) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                        "The manifest's [tool.uv] environments differ from the lock's supported-markers");
            }
            Integer writeRevision = configuredRevision();
            migrating = writeRevision != null && (lock.getRevision() == null || lock.getRevision() < 3);
            packages = new ArrayList<>(lock.getPackages());
            rootIndex = findRoot();
            UvLockPackage root = packages.get(rootIndex);
            oldMetadata = root.getMetadata() != null ? root.getMetadata() :
                    UvLockMetadata.builder().build();
            readConstraintDependencies();
            readOverrideDependencies();

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
            // uv leaves a lock alone while its metadata satisfies the manifest, whatever form it records,
            // and writes the metadata fresh once anything else in the lock changes
            UvLockMetadata freshMetadata = newMetadata;
            if (satisfies(oldMetadata, newMetadata)) {
                newMetadata = oldMetadata;
            }
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
                UvLockManifest manifest = (lock.getManifest() != null ? lock.getManifest() : new UvLockManifest(null, null, null, null))
                        .withConstraints(constraintDependencies.isEmpty() ? null : new ArrayList<>(constraintDependencies.values()));
                updated = updated.withManifest(manifest.getMembers() == null && manifest.getConstraints() == null &&
                        manifest.getOverrides() == null && manifest.getBuildConstraints() == null ? null : manifest);
                structural = true;
                for (String name : constrainedLockedNames(constraintDependencies.values())) {
                    // dependents that no longer accept the constrained version are revised after it moves
                    Change change = new Change(name);
                    change.constraints.addAll(gatherConstraints(name, dependent -> false));
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
            // drain cascades and transitive additions the closures pull in (ADR 0010 T1+T2), then revise dependents
            boolean revised = false;
            do {
            while (!cascadeQueue.isEmpty() || !additionQueue.isEmpty()) {
                if (!cascadeQueue.isEmpty()) {
                    String target = cascadeQueue.poll();
                    if (!resolved.add(target)) {
                        structural |= revisitSettled(target, rewritten);
                        continue;
                    }
                    if (pendingForkMoves.contains(target)) {
                        continue;
                    }
                    if (entriesNamed(target) > 1) {
                        requireEveryForkSatisfied(target);
                        continue;
                    }
                    List<String> forcing = forksForcingMove(target);
                    if (forcing != null) {
                        splitAcrossForks(target, forcing);
                        rewritten.add(target);
                        structural = true;
                        continue;
                    }
                    Change cascade = new Change(target);
                    cascade.constraints.addAll(gatherConstraints(target));
                    List<String> pins = indexPins.get(target);
                    cascade.pinnedIndexName = pins != null ? pins.get(0) : null;
                    int targetIndex = findSinglePackage(target);
                    boolean registry = targetIndex >= 0 &&
                            packages.get(targetIndex).getSource().getType() == UvLockSource.Type.REGISTRY;
                    if (registry && !anyVersionSatisfies(target, cascade.pinnedIndexName, cascade.constraints)) {
                        // dependents the run left alone are revised afterwards to accept what this selects
                        cascade.constraints.clear();
                        cascade.constraints.addAll(gatherConstraints(target, rewritten::contains));
                    }
                    if (applyChange(cascade)) {
                        rewritten.add(target);
                        structural = true;
                    }
                } else {
                    String target = additionQueue.poll();
                    if (!resolved.add(target)) {
                        structural |= revisitSettled(target, rewritten);
                        continue;
                    }
                    selectionFloor = unflooredAdditions.contains(target) ? null : additionFloors.get(target);
                    try {
                        resolveAddition(target, null);
                    } finally {
                        selectionFloor = null;
                    }
                    rewritten.add(target);
                    structural = true;
                }
            }
                revised = applyForkMove(rewritten) || reconsiderBackoff() || reviseDependent(rewritten, resolved);
                structural |= revised;
            } while (revised);
            qualifyEdgesIntoSplitPackages();
            if (structural) {
                dropUnreachable();
                requireNoForkLeftWithOneEntry();
                requireSourcedEntriesStillDeclared();
            }
            verifyDependents(rewritten);
            addProxyForks(rewritten);
            List<String> addedForks = convergentForks(rewritten);
            if (addedForks != null) {
                refineEntryForks(addedForks);
                updated = updated.withPackages(packages).withResolutionMarkers(addedForks);
            }
            if (structural && !writesCurrentMarkerForms()) {
                prunePlatformWheels(rewritten);
            }
            if (pythonChanged && lowerBoundIncreased()) {
                validatePinsAgainstPython(rewritten);
            }

            Object defaultGroups = declaredDefaultGroups();
            // a lock recording default groups no longer configured no longer satisfies the manifest
            if (packages.get(rootIndex).getDefaultGroups() != null &&
                    !packages.get(rootIndex).getDefaultGroups().equals(defaultGroups)) {
                packages.set(rootIndex, packages.get(rootIndex).withDefaultGroups(defaultGroups));
            }
            updated = updated.withPackages(packages);
            String written = UvLockWriter.write(updated);
            String original = UvLockWriter.write(lock);
            if (written.equals(original)) {
                return Result.success(written);
            }
            if (newMetadata != freshMetadata) {
                packages.set(rootIndex, packages.get(rootIndex).withMetadata(metadataEmpty ? null : freshMetadata));
                updated = updated.withPackages(packages);
            }
            if (writesCurrentMarkerForms()) {
                refreshArtifacts();
                prunePlatformWheels(rewritten);
                updated = updated.withPackages(packages);
            }
            if (migrating) {
                // a lock that satisfies the manifest stays as it is, so only a changed one is pruned whole
                if (!structural && !writesCurrentMarkerForms()) {
                    prunePlatformWheels(rewritten);
                }
                packages = rewrittenWhole(packages);
                updated = updated.withPackages(packages).withResolutionMarkers(inSolveOrder(updated.getResolutionMarkers()));
            }
            qualifyEdgesIntoSplitPackages();
            updated = updated.withPackages(packages);
            if (writesReachabilityMarkers()) {
                if (lock.getRequiredMarkers() != null) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                            "Changing a lock with required environments ([tool.uv] required-environments) is not " +
                                    "supported by the native engine");
                }
                updated = updated.withPackages(UvLockReachability.simplify(packages, rootIndex, range,
                        lock.getSupportedMarkers()));
            }
            if (writesCurrentMarkerForms()) {
                updated = updated.withPackages(inUvOrder(inCurrentMarkerForms(updated.getPackages())))
                        .withResolutionMarkers(currentForms(null, inSolveOrder(updated.getResolutionMarkers())));
                // a relative exclude-newer is written as its span beside the sentinel timestamp
                if (updated.getOptions() != null && updated.getOptions().getExcludeNewerSpan() != null) {
                    updated = updated.withOptions(updated.getOptions().withExcludeNewer(UvLockOptions.RELATIVE_EXCLUDE_NEWER_TIMESTAMP));
                }
            }
            // uv restamps a lock it changed with its own revision
            // from revision 5 on, uv records the project's default groups on its entry
            if (writeRevision != null && writeRevision >= 5) {
                List<UvLockPackage> withGroups = new ArrayList<>(updated.getPackages());
                int at = findRoot(withGroups);
                withGroups.set(at, withGroups.get(at).withDefaultGroups(defaultGroups));
                updated = updated.withPackages(withGroups);
            }
            if (writeRevision != null && !writeRevision.equals(lock.getRevision())) {
                updated = updated.withRevision(writeRevision);
            }
            return Result.success(UvLockWriter.write(updated));
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

        // uv renders every marker it writes from its marker algebra, which the port reproduces as of 0.13.4
        private boolean writesCurrentMarkerForms() {
            PythonVersion version = uvVersion != null ? PythonVersion.parse(uvVersion) : null;
            return version != null && version.compareTo(PythonVersion.parse("0.13.4")) >= 0;
        }

        // uv builds every lock with its packages sorted by id: name, then version with none first
        /**
         * A registry entry records the index as configured, so a lock written against
         * {@code https://pypi.org/simple/} records {@code https://pypi.org/simple} once anything changes. An index
         * the manifest does not configure keeps its recorded URL, since only its own configuration names it.
         */
        private UvLockSource asConfigured(UvLockSource source) {
            if (source.getType() != UvLockSource.Type.REGISTRY || source.getValue() == null) {
                return source;
            }
            String recorded = source.getValue();
            String match = null;
            for (UvIndex index : indexes) {
                String configured = index.getIndex().getUrl();
                if (configured.equals(recorded)) {
                    return source;
                }
                if (match == null && withoutTrailingSlash(configured).equals(withoutTrailingSlash(recorded))) {
                    match = configured;
                }
            }
            return match == null ? source : UvLockSource.registry(match);
        }

        private static String withoutTrailingSlash(String url) {
            return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        }

        private List<UvLockPackage> inUvOrder(List<UvLockPackage> pkgs) {
            List<UvLockPackage> sorted = new ArrayList<>(pkgs);
            sorted.sort(Comparator.comparing(UvLockPackage::getName).thenComparing(pkg ->
                    pkg.getVersion() == null ? null : PythonVersion.parse(pkg.getVersion()), Comparator.nullsFirst(Comparator.naturalOrder())));
            return sorted;
        }

        private List<UvLockPackage> inCurrentMarkerForms(List<UvLockPackage> pkgs) {
            List<UvLockPackage> out = new ArrayList<>(pkgs.size());
            for (UvLockPackage pkg : withRecordedMarkers(pkgs)) {
                UvLockPackage.UvLockPackageBuilder b = pkg.toBuilder()
                        .source(asConfigured(pkg.getSource()))
                        .resolutionMarkers(inSolveOrder(currentForms(pkg.getName(), pkg.getResolutionMarkers())));
                UvLockMetadata metadata = pkg.getMetadata();
                if (metadata != null) {
                    Map<String, List<UvLockRequirement>> dev = null;
                    if (metadata.getRequiresDev() != null) {
                        dev = new LinkedHashMap<>();
                        for (Map.Entry<String, List<UvLockRequirement>> group : metadata.getRequiresDev().entrySet()) {
                            dev.put(group.getKey(), requirementsInCurrentForms(pkg.getName(), group.getValue()));
                        }
                    }
                    b.metadata(metadata.withRequiresDist(requirementsInCurrentForms(pkg.getName(), metadata.getRequiresDist()))
                            .withRequiresDev(dev));
                }
                out.add(b.build());
            }
            return out;
        }

        private @Nullable List<UvLockRequirement> requirementsInCurrentForms(String pkg, @Nullable List<UvLockRequirement> reqs) {
            if (reqs == null) {
                return null;
            }
            List<UvLockRequirement> out = new ArrayList<>(reqs.size());
            for (UvLockRequirement req : reqs) {
                out.add(req.getMarker() == null ? req : req.withMarker(currentForm(pkg, req.getMarker())));
            }
            return out;
        }

        private @Nullable List<String> currentForms(@Nullable String pkg, @Nullable List<String> markers) {
            if (markers == null) {
                return null;
            }
            List<String> out = new ArrayList<>(markers.size());
            for (String marker : markers) {
                out.add(currentForm(pkg, marker));
            }
            return out;
        }

        private String currentForm(@Nullable String pkg, String marker) {
            String current = UvLockMarkers.recordMarker(marker, null);
            if (current == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                        "Rewriting the lock needs uv's current form of the marker \"" + marker + "\"");
            }
            return current;
        }

        /**
         * What a release writing the lock whole changes in entries the edit left alone: edge markers in
         * its recorded form, wheels the Python range rules out dropped, and the upload times the index lists.
         */
        private List<UvLockPackage> rewrittenWhole(List<UvLockPackage> pkgs) {
            List<UvLockPackage> out = new ArrayList<>(pkgs.size());
            for (UvLockPackage pkg : withRecordedMarkers(pkgs)) {
                UvLockPackage.UvLockPackageBuilder b = pkg.toBuilder()
                        .resolutionMarkers(inSolveOrder(pkg.getResolutionMarkers()));
                if (pkg.getSource().getType() == UvLockSource.Type.REGISTRY && pkg.getWheels() != null) {
                    List<UvLockArtifact> wheels = new ArrayList<>();
                    for (UvLockArtifact wheel : pkg.getWheels()) {
                        String filename = wheelFilename(wheel);
                        if (filename == null || wheelAdmittedByRange(filename)) {
                            wheels.add(wheel);
                        }
                    }
                    b.wheels(wheels.isEmpty() ? null : wheels);
                }
                out.add(b.build());
            }
            return withUploadTimes(out);
        }

        private List<UvLockPackage> withRecordedMarkers(List<UvLockPackage> pkgs) {
            List<UvLockPackage> out = new ArrayList<>(pkgs.size());
            for (UvLockPackage pkg : pkgs) {
                UvLockPackage.UvLockPackageBuilder b = pkg.toBuilder().dependencies(recorded(pkg.getName(), pkg.getDependencies()));
                if (pkg.getOptionalDependencies() != null) {
                    Map<String, List<UvLockDependency>> groups = new LinkedHashMap<>();
                    pkg.getOptionalDependencies().forEach((k, v) -> groups.put(k, recorded(pkg.getName(), v)));
                    b.optionalDependencies(groups);
                }
                if (pkg.getDevDependencies() != null) {
                    Map<String, List<UvLockDependency>> groups = new LinkedHashMap<>();
                    pkg.getDevDependencies().forEach((k, v) -> groups.put(k, recorded(pkg.getName(), v)));
                    b.devDependencies(groups);
                }
                out.add(b.build());
            }
            return out;
        }

        private @Nullable List<UvLockDependency> recorded(String pkg, @Nullable List<UvLockDependency> edges) {
            if (edges == null) {
                return null;
            }
            List<UvLockDependency> out = new ArrayList<>(edges.size());
            for (UvLockDependency edge : edges) {
                if (edge.getMarker() == null) {
                    out.add(edge);
                    continue;
                }
                String marker = UvLockMarkers.recordMarkerAsRecorded(edge.getMarker());
                if (marker == null) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                            "Rewriting the lock whole needs uv's current form of the marker \"" + edge.getMarker() + "\"");
                }
                out.add(edge.withMarker(marker));
            }
            return out;
        }

        /**
         * uv solves forks with higher Python lower bounds first and records them in solve order. Forks with the
         * same lower bound keep the order they are recorded in, and a fork the engine cannot bound keeps the
         * whole order.
         */
        private @Nullable List<String> inSolveOrder(@Nullable List<String> markers) {
            if (markers == null || markers.size() < 2) {
                return markers;
            }
            Map<String, PythonVersion> lower = new HashMap<>();
            for (String marker : markers) {
                List<List<String[]>> terms = UvLockMarkers.parseDisjunction(marker);
                if (terms == null) {
                    return markers;
                }
                PythonVersion least = null;
                boolean unbounded = false;
                for (List<String[]> term : terms) {
                    PyRange r = new PyRange();
                    for (String[] c : term) {
                        String[] lowered = UvLockMarkers.lowerVersionClause(c[0], c[1], c[2]);
                        if (lowered != null && !"!=".equals(lowered[1])) {
                            PyRange both = r.intersect(PyRange.parse(lowered[1] + lowered[2]));
                            r = both != null ? both : r;
                        }
                    }
                    if (r.lower == null) {
                        unbounded = true;
                    } else if (least == null || r.lower.compareTo(least) < 0) {
                        least = r.lower;
                    }
                }
                lower.put(marker, unbounded ? null : least);
            }
            List<String> sorted = new ArrayList<>(markers);
            sorted.sort((a, b) -> {
                PythonVersion la = lower.get(a);
                PythonVersion lb = lower.get(b);
                return la == null ? (lb == null ? 0 : 1) : lb == null ? -1 : lb.compareTo(la);
            });
            return sorted;
        }

        /**
         * The Python lower bound an and-chain marker implies through its Python version clauses, or null when it
         * implies none. A disjunction that names the Python version fails loud.
         */
        private static @Nullable PythonVersion impliedPythonLower(String marker) {
            List<String[]> clauses = UvLockMarkers.parseSimpleAndChain(marker);
            if (clauses == null) {
                if (UvLockMarkers.referencesPythonVersion(marker)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                            "The marker \"" + marker + "\" may fork the resolution in a way the native engine " +
                                    "cannot place");
                }
                return null;
            }
            PyRange r = new PyRange();
            for (String[] c : clauses) {
                String[] lowered = UvLockMarkers.lowerVersionClause(c[0], c[1], c[2]);
                if (lowered != null && !"!=".equals(lowered[1])) {
                    PyRange both = r.intersect(PyRange.parse(lowered[1] + lowered[2]));
                    r = both != null ? both : r;
                }
            }
            return r.lower;
        }

        // the Python range a fork marker made only of Python version clauses spans, or null for any other marker
        private static @Nullable PyRange forkRange(String marker) {
            List<String[]> clauses = UvLockMarkers.parseSimpleAndChain(marker);
            if (clauses == null) {
                return null;
            }
            PyRange r = new PyRange();
            for (String[] c : clauses) {
                String[] lowered = UvLockMarkers.lowerVersionClause(c[0], c[1], c[2]);
                if (lowered == null) {
                    return null;
                }
                if (!"!=".equals(lowered[1])) {
                    PyRange both = r.intersect(PyRange.parse(lowered[1] + lowered[2]));
                    r = both != null ? both : r;
                }
            }
            return r;
        }

        /** Every registry artifact without an upload time gets the one its index lists, where it lists one. */
        private List<UvLockPackage> withUploadTimes(List<UvLockPackage> pkgs) {
            List<UvLockPackage> out = new ArrayList<>(pkgs.size());
            for (UvLockPackage pkg : pkgs) {
                if (pkg.getSource().getType() != UvLockSource.Type.REGISTRY || !lacksUploadTime(pkg)) {
                    out.add(pkg);
                    continue;
                }
                List<String> pins = indexPins.get(pkg.getName());
                Map<String, PackageFile> byFilename = new HashMap<>();
                for (PackageFile file : fetchListing(pkg.getName(), pins != null ? pins.get(0) : null).listing.getFiles()) {
                    byFilename.put(file.getFilename(), file);
                }
                List<UvLockArtifact> wheels = null;
                if (pkg.getWheels() != null) {
                    wheels = new ArrayList<>();
                    for (UvLockArtifact wheel : pkg.getWheels()) {
                        wheels.add(withUploadTime(pkg, wheel, byFilename));
                    }
                }
                out.add(pkg.toBuilder()
                        .sdist(pkg.getSdist() != null ? withUploadTime(pkg, pkg.getSdist(), byFilename) : null)
                        .wheels(wheels)
                        .build());
            }
            return out;
        }

        private static boolean lacksUploadTime(UvLockPackage pkg) {
            if (pkg.getSdist() != null && pkg.getSdist().getUploadTime() == null) {
                return true;
            }
            for (UvLockArtifact wheel : pkg.getWheels() != null ? pkg.getWheels() : Collections.<UvLockArtifact>emptyList()) {
                if (wheel.getUploadTime() == null) {
                    return true;
                }
            }
            return false;
        }

        private UvLockArtifact withUploadTime(UvLockPackage pkg, UvLockArtifact artifact, Map<String, PackageFile> byFilename) {
            if (artifact.getUploadTime() != null || artifact.getUrl() == null) {
                return artifact;
            }
            PackageFile file = byFilename.get(artifact.getUrl().substring(artifact.getUrl().lastIndexOf('/') + 1));
            if (file == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg.getName(),
                        "Rewriting the lock whole needs " + artifact.getUrl() + ", which the index no longer lists");
            }
            // uv records an upload time only where the index gives one
            String uploadTime = formatUploadTime(file.getUploadTime());
            return uploadTime == null ? artifact : artifact.withUploadTime(uploadTime);
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

        /**
         * uv's satisfies check: requirements compared after normalization, and provides-extras as a set,
         * or not at all in a lock from before the field existed (version 1 revision 1).
         */
        private boolean satisfies(UvLockMetadata recorded, UvLockMetadata declared) {
            if (!requirementKeys(recorded.getRequiresDist()).equals(requirementKeys(declared.getRequiresDist()))) {
                return false;
            }
            Map<String, List<UvLockRequirement>> recordedDev = recorded.getRequiresDev() != null ? recorded.getRequiresDev() : emptyMap();
            Map<String, List<UvLockRequirement>> declaredDev = declared.getRequiresDev() != null ? declared.getRequiresDev() : emptyMap();
            if (!recordedDev.keySet().equals(declaredDev.keySet())) {
                return false;
            }
            for (String group : recordedDev.keySet()) {
                if (!requirementKeys(recordedDev.get(group)).equals(requirementKeys(declaredDev.get(group)))) {
                    return false;
                }
            }
            boolean providesExtrasTracked = lock.getRevision() != null && lock.getRevision() >= 1;
            return !providesExtrasTracked || new HashSet<>(orEmpty(recorded.getProvidesExtras()))
                    .equals(new HashSet<>(orEmpty(declared.getProvidesExtras())));
        }

        private static List<String> orEmpty(@Nullable List<String> list) {
            return list != null ? list : emptyList();
        }

        private static Map<String, Integer> requirementKeys(@Nullable List<UvLockRequirement> requirements) {
            Map<String, Integer> keys = new HashMap<>();
            for (UvLockRequirement r : requirements != null ? requirements : Collections.<UvLockRequirement>emptyList()) {
                List<String> extras = new ArrayList<>(orEmpty(r.getExtras()));
                sort(extras);
                String key = r.getName() + '|' + extras + '|' + normalizedMarker(r.getMarker()) + '|' + r.getIndex() + '|' + r.getUrl() + '|' +
                        r.getGit() + '|' + r.getDirectory() + '|' + r.getEditable() + '|' + specifierKey(r.getSpecifier());
                keys.merge(key, 1, Integer::sum);
            }
            return keys;
        }

        // an older uv's recorded form and the current one denote the same marker
        private static @Nullable String normalizedMarker(@Nullable String marker) {
            if (marker == null) {
                return null;
            }
            String recorded = UvLockMarkers.recordMarker(marker, null);
            return recorded != null ? recorded : marker;
        }

        // versions compare with trailing zeros ignored, except under ~= and a star where they are significant
        private static String specifierKey(@Nullable String specifier) {
            if (specifier == null) {
                return "";
            }
            List<String> parts = new ArrayList<>();
            for (String part : specifier.split(",")) {
                Matcher m = Pattern.compile("\\s*(~=|===|==|!=|<=|>=|<|>)\\s*(\\d+(?:\\.\\d+)*)\\s*").matcher(part);
                if (m.matches() && !"~=".equals(m.group(1))) {
                    parts.add(m.group(1) + m.group(2).replaceAll("(\\.0)+$", ""));
                } else {
                    parts.add(part.trim());
                }
            }
            sort(parts);
            return String.join(",", parts);
        }

        /** {@code [tool.uv] default-groups} as uv records it: {@code "all"}, or the sorted normalized names, never {@code ["dev"]}. */
        private @Nullable List<String> declaredEnvironments() {
            Toml.Table uv = UvLockToml.findTable(pyproject, "tool.uv");
            if (uv == null || !UvLockToml.hasKey(uv, "environments")) {
                return null;
            }
            String single = UvLockToml.literalString(uv, "environments");
            return single != null ? singletonList(single) : UvLockToml.stringArray(uv, "environments");
        }

        private static boolean sameEnvironments(@Nullable List<String> declared, @Nullable List<String> recorded) {
            if (declared == null || recorded == null) {
                return declared == recorded;
            }
            try {
                MarkerInterner interner = new MarkerInterner();
                MarkerTree a = MarkerTree.trueIn(interner).negate();
                for (String marker : declared) {
                    a = a.or(MarkerTree.parse(interner, marker));
                }
                MarkerTree b = MarkerTree.trueIn(interner).negate();
                for (String marker : recorded) {
                    b = b.or(MarkerTree.parse(interner, marker));
                }
                return a.and(b.negate()).isFalse() && b.and(a.negate()).isFalse();
            } catch (MarkerParser.MarkerSyntaxException e) {
                return false;
            }
        }

        private @Nullable Object declaredDefaultGroups() {
            Toml.Table uv = UvLockToml.findTable(pyproject, "tool.uv");
            if (uv == null || !UvLockToml.hasKey(uv, "default-groups")) {
                return null;
            }
            if ("all".equals(UvLockToml.literalString(uv, "default-groups"))) {
                return "all";
            }
            TreeSet<String> groups = new TreeSet<>();
            for (String group : UvLockToml.stringArray(uv, "default-groups")) {
                groups.add(Pep508Requirement.canonicalize(group));
            }
            return groups.equals(Collections.singleton("dev")) ? null : new ArrayList<>(groups);
        }

        private int findRoot() {
            return findRoot(packages);
        }

        private static int findRoot(List<UvLockPackage> packages) {
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
                    if (nonRegistrySources.contains(added.getName()) || added.getGit() != null ||
                            added.getUrl() != null || added.getDirectory() != null || added.getEditable() != null) {
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
                if (oldReqs.size() > newReqs.size() && UvLockRequirements.extraGroupOf(newReqs.get(0)) == null &&
                        UvLockRequirements.rendered(oldReqs).containsAll(UvLockRequirements.rendered(newReqs))) {
                    List<@Nullable String> remaining = new ArrayList<>();
                    for (UvLockRequirement req : newReqs) {
                        remaining.add(req.getMarker());
                    }
                    removals.add(new Removal(newReqs.get(0).getName(), null, devGroup, remaining));
                    continue;
                }
                if (oldReqs.size() != 1 || newReqs.size() != 1) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, newReqs.get(0).getName(),
                            "Editing marker-differentiated duplicate declarations is not supported");
                }
                UvLockRequirement oldReq = oldReqs.get(0);
                UvLockRequirement newReq = newReqs.get(0);
                if (!Objects.equals(normalizedMarker(oldReq.getMarker()), normalizedMarker(newReq.getMarker()))) {
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
                boolean wasDirect = oldReq.getGit() != null || oldReq.getUrl() != null;
                boolean isDirect = newReq.getGit() != null || newReq.getUrl() != null;
                if (isDirect && !(Objects.equals(oldReq.getGit(), newReq.getGit()) &&
                        Objects.equals(oldReq.getUrl(), newReq.getUrl()))) {
                    throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, newReq.getName(),
                            "Re-targeting a declaration to a git or URL source is not supported");
                }
                Change change = changes.get(newReq.getName());
                if (change == null) {
                    change = new Change(newReq.getName());
                    changes.put(newReq.getName(), change);
                }
                change.retargetToRegistry |= wasDirect && !isDirect;
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
                    // generic tags support every Python of their major version from the tag upward
                    if (major >= lowerMajor && (minor < 0 || minorStartsBelowUpper(major, minor))) {
                        return true;
                    }
                } else if (abi3) {
                    // uv keeps stable-ABI wheels whatever the range's upper bound
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
            UvLockSource.Type type = pkg.getSource().getType();
            if (change.retargetToRegistry && (type == UvLockSource.Type.GIT || type == UvLockSource.Type.URL)) {
                // the root edge goes first: its extras are what activate the new entry's groups
                updateRootEdgeExtras(change);
                packages.set(index, resolveEntry(change, null,
                        UvLockPackage.builder().name(change.canonicalName).build()));
                addLowerForkEntry();
                return true;
            }
            if (type != UvLockSource.Type.REGISTRY) {
                throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, change.canonicalName,
                        "Editing a " + type.getKey() + "-sourced package is not supported");
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
            addLowerForkEntry();
            return true;
        }

        private void addLowerForkEntry() {
            if (lowerForkEntry != null) {
                packages.add(lowerForkEntry);
                lowerForkEntry = null;
            }
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
            checkNoUvResolutionOverrides(change.canonicalName);
            UvLockOptions options = lock.getOptions();
            if (options != null && (options.getResolutionMode() != null || options.getPrereleaseMode() != null ||
                    options.getForkStrategy() != null)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, change.canonicalName,
                        "Version selection under a non-default [options] resolution, prerelease or fork setting is not supported");
            }
            if (lock.getRequiredMarkers() != null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, change.canonicalName,
                        "Resolving " + change.canonicalName + " in a lock with required environments " +
                                "([tool.uv] required-environments) is not supported by the native engine");
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
            if (change.transitiveAddition) {
                selected = keepingLockedVersions(byVersion, change, listing.index, selected);
            }
            if (wouldFork(byVersion, change, selected)) {
                return splitOnRequiresPython(change, base, listing, byVersion, selected);
            }
            return buildEntry(change, base, listing, byVersion, selected);
        }

        /**
         * uv forks the range where the newest acceptable version needs a newer Python, and resolves each side on
         * its own. The newest version for the forks from that floor up, with the lower selection set aside for the
         * forks below. Only one split, on a lock not forked yet, is reproduced.
         */
        private UvLockPackage splitOnRequiresPython(Change change, UvLockPackage base, Listing listing,
                                                    Map<PythonVersion, List<PackageFile>> byVersion,
                                                    PythonVersion lower) {
            PythonVersion upper = null;
            for (Map.Entry<PythonVersion, List<PackageFile>> e : byVersion.entrySet()) {
                PythonVersion v = e.getKey();
                if (!v.isPrerelease() && satisfiesAll(change.constraints, v) && installable(e.getValue()) &&
                        (upper == null || v.compareTo(upper) > 0)) {
                    upper = v;
                }
            }
            PythonVersion floor = upper == null ? null : requiredPythonFloor(byVersion.get(upper));
            if (floor == null || forks != null || lock.getResolutionMarkers() != null || lowerForkEntry != null ||
                    !range.withinUpperBound(floor)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, change.canonicalName,
                        "Resolving " + change.canonicalName + " would fork it across the requires-python range " +
                                "(uv locks multiple versions gated by python markers); marker-space resolution " +
                                "is not supported by the native engine");
            }
            String above = "python_full_version >= '" + floor + "'";
            splitForks(above, false);
            List<String> split = requireNonNull(forks);
            String below = split.get(0).equals(above) ? split.get(1) : split.get(0);
            // whether uv moves a locked package or backtracks this one depends on its decision order
            if (!keepsLockedVersions(fetchMetadata(change.canonicalName, listing.index, byVersion.get(upper))) ||
                    !keepsLockedVersions(fetchMetadata(change.canonicalName, listing.index, byVersion.get(lower)))) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, change.canonicalName,
                        "Resolving " + change.canonicalName + " forks it across the requires-python range into " +
                                "versions that would move a locked package, which the native engine does not decide");
            }
            lowerForkEntry = buildEntry(change, base, listing, byVersion, lower)
                    .withResolutionMarkers(singletonList(below));
            return buildEntry(change, base, listing, byVersion, upper).withResolutionMarkers(singletonList(above));
        }

        private UvLockPackage buildEntry(Change change, UvLockPackage base, Listing listing,
                                         Map<PythonVersion, List<PackageFile>> byVersion, PythonVersion selected) {
            List<PackageFile> files = byVersion.get(selected);
            CoreMetadata metadata = fetchMetadata(change.canonicalName, listing.index, files);
            Set<String> activated = activatedExtras(change.canonicalName);
            requireActivatedExtrasGated(change.canonicalName, selected.toString(), metadata, activated);
            ResolvedEdges edges = buildEdges(change.canonicalName, metadata, base, activated, true);

            UvLockPackage withArtifacts = withArtifacts(base.toBuilder()
                    .version(selected.toString())
                    .source(UvLockSource.registry(listing.index.getIndex().getUrl()))
                    .dependencies(edges.base.isEmpty() ? null : edges.base)
                    .optionalDependencies(edges.groups.isEmpty() ? null : edges.groups)
                    .build(), listing, files);
            if (withArtifacts == null) {
                throw new EngineFailure(new Failure(Reason.RESOLUTION_CONFLICT, change.canonicalName,
                        listing.index.getIndex().getUrl(), "No installable distribution for " + selected));
            }
            return withArtifacts;
        }

        /** The entry with the sdist and wheels uv records from these listed files, or null when none installs. */
        private @Nullable UvLockPackage withArtifacts(UvLockPackage pkg, Listing listing, List<PackageFile> files) {
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
                return null;
            }
            // uv records wheels in filename order, whatever order the index lists them in
            wheelFiles.sort(Comparator.comparing(PackageFile::getFilename));
            List<UvLockArtifact> wheels = new ArrayList<>();
            for (PackageFile wheel : wheelFiles) {
                wheels.add(toArtifact(pkg.getName(), listing.index, wheel));
            }
            return pkg.toBuilder()
                    .sdist(sdistFile != null ? toArtifact(pkg.getName(), listing.index, sdistFile) : null)
                    .wheels(wheels.isEmpty() ? null : wheels)
                    .build();
        }

        /**
         * Every registry entry with the files its index lists for the locked version now. uv reads them anew whenever
         * it relocks, so files uploaded since the lock was written appear. An entry from an index the manifest does
         * not configure keeps its files, as that index came from its author's own configuration.
         */
        private void refreshArtifacts() {
            for (int i = 0; i < packages.size(); i++) {
                UvLockPackage pkg = packages.get(i);
                if (pkg.getSource().getType() != UvLockSource.Type.REGISTRY || pkg.getVersion() == null) {
                    continue;
                }
                List<String> pins = indexPins.get(pkg.getName());
                Listing listing = fetchListing(pkg.getName(), pins != null ? pins.get(0) : null);
                if (!listing.index.getIndex().getUrl().equals(pkg.getSource().getValue())) {
                    continue;
                }
                PythonVersion version = PythonVersion.parse(pkg.getVersion());
                List<PackageFile> files = version == null ? null : groupByVersion(listing.listing, pkg.getName()).get(version);
                UvLockPackage refreshed = files == null ? null : withArtifacts(pkg, listing, files);
                if (refreshed == null) {
                    throw new EngineFailure(new Failure(Reason.RESOLUTION_REQUIRED, pkg.getName(),
                            listing.index.getIndex().getUrl(), pkg.getName() + " " + pkg.getVersion() +
                            " is no longer listed with an installable file, so uv would resolve it again"));
                }
                packages.set(i, refreshed);
            }
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

        /**
         * Moves one untouched dependent that no longer accepts a moved package to the newest of its versions that does,
         * as uv's resolver backtracks to one. Reports whether it moved one.
         */
        private boolean reviseDependent(Set<String> rewritten, Set<String> resolved) {
            Map<String, Set<String>> live = liveExtras();
            for (String name : new ArrayList<>(rewritten)) {
                if (entriesNamed(name) > 1) {
                    continue; // split across forks, where each fork's requirements chose its entry
                }
                int index = findSinglePackage(name);
                PythonVersion version = index < 0 ? null : PythonVersion.parse(packages.get(index).getVersion());
                if (version == null) {
                    continue;
                }
                List<UvLockPackage> violated = new ArrayList<>();
                forEachDependentRequirement(name, live, (dependent, raw, requirement) -> {
                    if (!rewritten.contains(dependent.getName()) && requirement.getSpecifiers() != null &&
                            !requirement.getSpecifiers().contains(version, true)) {
                        violated.add(dependent);
                    }
                });
                for (UvLockPackage dependent : violated) {
                    PythonVersion revision = revisionAccepting(dependent, name, version, live);
                    if (revision == null) {
                        continue;
                    }
                    Change change = new Change(dependent.getName());
                    change.constraints.add(requireNonNull(PythonVersionSpecifierSet.parse("==" + revision)));
                    List<String> pins = indexPins.get(dependent.getName());
                    change.pinnedIndexName = pins != null ? pins.get(0) : null;
                    resolved.add(dependent.getName());
                    if (applyChange(change)) {
                        rewritten.add(dependent.getName());
                        return true;
                    }
                }
            }
            return false;
        }

        /** The newest of the dependent's versions, within a budget, whose requirement on the package accepts it. */
        private @Nullable PythonVersion revisionAccepting(UvLockPackage dependent, String name, PythonVersion version,
                                                          Map<String, Set<String>> live) {
            List<String> pins = indexPins.get(dependent.getName());
            Listing listing = fetchListing(dependent.getName(), pins != null ? pins.get(0) : null);
            Map<PythonVersion, List<PackageFile>> byVersion = groupByVersion(listing.listing, dependent.getName());
            List<PythonVersionSpecifierSet> constraints = gatherConstraints(dependent.getName());
            // uv takes a pre-release where no final release meets the constraints
            boolean prereleases = byVersion.keySet().stream()
                    .noneMatch(v -> !v.isPrerelease() && satisfiesAll(constraints, v));
            int tried = 0;
            for (PythonVersion candidate : new TreeSet<>(byVersion.keySet()).descendingSet()) {
                if (!satisfiesAll(constraints, candidate) || !versionAdmitsPython(byVersion.get(candidate)) ||
                        candidate.isPrerelease() && !prereleases &&
                                !candidate.equals(PythonVersion.parse(dependent.getVersion()))) {
                    continue;
                }
                if (++tried > 20) {
                    return null;
                }
                CoreMetadata metadata = fetchMetadata(dependent.getName(), listing.index, byVersion.get(candidate));
                boolean accepts = true;
                for (String raw : overridden(metadata.getRequiresDist())) {
                    Pep508Requirement requirement = Pep508Requirement.parse(raw);
                    if (requirement == null || !requirement.getCanonicalName().equals(name) ||
                            requirement.getSpecifiers() == null) {
                        continue;
                    }
                    int semi = raw.indexOf(';');
                    String rawMarker = requirement.getMarker() != null && semi >= 0 ?
                            raw.substring(semi + 1).trim() : null;
                    if ((rawMarker == null || requirementCanFire(dependent, rawMarker, live)) &&
                            !requirement.getSpecifiers().contains(version, true)) {
                        accepts = false;
                    }
                }
                if (accepts) {
                    return candidate;
                }
            }
            return null;
        }

        /**
         * A [tool.uv.sources] entry applies only to the project's own declaration, so a git, URL or path package
         * still reached once that declaration goes is resolved from the registry by uv, which needs requirements
         * the engine may not have.
         */
        private void requireSourcedEntriesStillDeclared() {
            Set<String> declared = new HashSet<>();
            UvLockPackage root = packages.get(rootIndex);
            for (UvLockDependency edge : root.getDependencies() != null ? root.getDependencies() :
                    Collections.<UvLockDependency>emptyList()) {
                declared.add(edge.getName());
            }
            for (Map<String, List<UvLockDependency>> groups : Arrays.asList(root.getDevDependencies(),
                    root.getOptionalDependencies())) {
                if (groups != null) {
                    groups.values().forEach(edges -> edges.forEach(edge -> declared.add(edge.getName())));
                }
            }
            Set<String> wasDeclared = new HashSet<>();
            UvLockPackage originalRoot = lock.getPackages().get(findRoot(lock.getPackages()));
            for (UvLockDependency edge : originalRoot.getDependencies() != null ? originalRoot.getDependencies() :
                    Collections.<UvLockDependency>emptyList()) {
                wasDeclared.add(edge.getName());
            }
            for (Map<String, List<UvLockDependency>> groups : Arrays.asList(originalRoot.getDevDependencies(),
                    originalRoot.getOptionalDependencies())) {
                if (groups != null) {
                    groups.values().forEach(edges -> edges.forEach(edge -> wasDeclared.add(edge.getName())));
                }
            }
            for (int i = 0; i < packages.size(); i++) {
                UvLockPackage pkg = packages.get(i);
                UvLockSource.Type type = pkg.getSource().getType();
                if (i != rootIndex && type != UvLockSource.Type.REGISTRY &&
                        wasDeclared.contains(pkg.getName()) && !declared.contains(pkg.getName())) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg.getName(),
                            "Removing the declaration of " + type.getKey() + "-sourced " + pkg.getName() +
                                    " leaves it required by another package, which uv resolves from the registry");
                }
            }
        }

        // a package the lock holds once per fork that keeps only one entry would need its forks rewritten
        private void requireNoForkLeftWithOneEntry() {
            Map<String, Integer> before = new HashMap<>();
            for (UvLockPackage pkg : lock.getPackages()) {
                before.merge(pkg.getName(), 1, Integer::sum);
            }
            for (Map.Entry<String, Integer> e : before.entrySet()) {
                if (e.getValue() > 1 && entriesNamed(e.getKey()) == 1 &&
                        packages.get(findSinglePackage(e.getKey())).getResolutionMarkers() != null) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, e.getKey(),
                            "The edit leaves one of " + e.getKey() + "'s fork-duplicated entries, whose forks uv " +
                                    "would rewrite");
                }
            }
        }

        // an untouched dependent the new version no longer satisfies is one uv would revise, which the engine does not
        private void verifyDependents(Set<String> rewritten) {
            Map<String, Set<String>> live = liveExtras();
            verifyLocalDependents(rewritten, live);
            for (String name : rewritten) {
                if (entriesNamed(name) > 1) {
                    continue;
                }
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

        /**
         * A moved package's path and editable dependents keep their requirements in the lock's metadata. A git or
         * URL dependent's requirements are not in the lock, so nothing shows it still accepts the moved version.
         */
        private void verifyLocalDependents(Set<String> rewritten, Map<String, Set<String>> live) {
            Map<String, String> lockedVersions = new HashMap<>();
            for (UvLockPackage pkg : lock.getPackages()) {
                lockedVersions.merge(pkg.getName(), String.valueOf(pkg.getVersion()), (a, b) -> a);
            }
            for (String name : rewritten) {
                if (entriesNamed(name) != 1) {
                    continue;
                }
                String version = packages.get(findSinglePackage(name)).getVersion();
                PythonVersion moved = PythonVersion.parse(version);
                if (moved == null || !lockedVersions.containsKey(name) || lockedVersions.get(name).equals(version)) {
                    continue;
                }
                for (int i = 0; i < packages.size(); i++) {
                    UvLockPackage q = packages.get(i);
                    if (i == rootIndex || q.getSource().getType() == UvLockSource.Type.REGISTRY ||
                            !live.containsKey(q.getName()) || !dependsOn(q, name)) {
                        continue;
                    }
                    if (q.getMetadata() == null) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                                q.getName() + " is " + q.getSource().getType().getKey() + "-sourced, so the lock " +
                                        "does not record whether it accepts " + name + " " + version);
                    }
                    for (UvLockRequirement req : q.getMetadata().getRequiresDist() != null ?
                            q.getMetadata().getRequiresDist() : Collections.<UvLockRequirement>emptyList()) {
                        PythonVersionSpecifierSet specs = req.getName().equals(name) && req.getSpecifier() != null ?
                                PythonVersionSpecifierSet.parse(req.getSpecifier()) : null;
                        if (specs != null && !specs.contains(moved, true)) {
                            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                                    q.getName() + " requires " + name + req.getSpecifier() + ", which " + version +
                                            " does not satisfy");
                        }
                    }
                }
            }
        }

        /**
         * uv writes every package's wheels less the ones its reachability rules out. Before uv 0.13.4 an entry
         * the run did not resolve keeps the wheels it has, so one whose platform-scoped reachability changed
         * may be owed wheels back from the index, which fails loud. From 0.13.4 every entry is refreshed first.
         */
        private void prunePlatformWheels(Set<String> rewritten) {
            boolean any = false;
            for (UvLockPackage pkg : packages) {
                any |= hasPlatformWheel(pkg);
            }
            if (!any) {
                return;
            }
            // a whole-lock rewrite judges reachability by the markers it is about to write
            UvLockReachability now = UvLockReachability.of(migrating ? withRecordedMarkers(packages) : packages, rootIndex,
                    range, lock.getSupportedMarkers());
            if (now == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                        "Pruning platform wheels by reachability is not supported for this lock's markers");
            }
            UvLockReachability before = null;
            Map<String, Integer> beforeIndex = new HashMap<>();
            List<UvLockPackage> original = lock.getPackages();
            for (int i = 0; i < original.size(); i++) {
                beforeIndex.put(original.get(i).getName() + '@' + original.get(i).getVersion(), i);
            }
            for (int i = 0; i < packages.size(); i++) {
                UvLockPackage pkg = packages.get(i);
                if (!hasPlatformWheel(pkg)) {
                    continue;
                }
                Integer was = beforeIndex.get(pkg.getName() + '@' + pkg.getVersion());
                if (!rewritten.contains(pkg.getName()) && was != null && !writesCurrentMarkerForms()) {
                    if (before == null) {
                        before = UvLockReachability.of(migrating ? withRecordedMarkers(original) : original, findRoot(original), PyRange.parse(
                                lock.getRequiresPython() != null ? lock.getRequiresPython() : ""), lock.getSupportedMarkers());
                    }
                    if (before == null || (before.platformScoped(was) && !before.reachKey(was).equals(now.reachKey(i)))) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg.getName(),
                                "The platforms " + pkg.getName() + " is reachable on changed, so uv would take its wheels " +
                                        "from the index again, which the native engine does not reproduce");
                    }
                }
                List<UvLockArtifact> kept = new ArrayList<>();
                for (UvLockArtifact wheel : pkg.getWheels()) {
                    String filename = wheelFilename(wheel);
                    if (filename == null || !now.wheelUnreachable(i, filename)) {
                        kept.add(wheel);
                    }
                }
                if (kept.size() != pkg.getWheels().size()) {
                    packages.set(i, pkg.withWheels(kept.isEmpty() ? null : kept));
                }
            }
        }

        private static boolean hasPlatformWheel(UvLockPackage pkg) {
            for (UvLockArtifact wheel : pkg.getWheels() != null ? pkg.getWheels() : Collections.<UvLockArtifact>emptyList()) {
                String filename = wheelFilename(wheel);
                if (filename != null) {
                    String base = filename.substring(0, filename.length() - ".whl".length());
                    if (!"any".equals(base.substring(base.lastIndexOf('-') + 1))) {
                        return true;
                    }
                }
            }
            return false;
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
                for (String raw : overridden(meta.getRequiresDist())) {
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
            select.transitiveAddition = direct == null;
            insertSorted(resolveEntry(select, null, UvLockPackage.builder().name(name).build()));
            if (lowerForkEntry != null) {
                insertSorted(lowerForkEntry);
                lowerForkEntry = null;
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
        /**
         * In a lock forked by its resolution-markers, uv resolves each fork with the locked version preferred, so a
         * package moves only in the forks where a requirement the locked version fails applies. Those forks, or null
         * when the package moves in every fork or the lock has none.
         */
        private @Nullable List<String> forksForcingMove(String target) {
            List<String> forks = lock.getResolutionMarkers();
            int index = findSinglePackage(target);
            PythonVersion locked = index < 0 ? null : PythonVersion.parse(packages.get(index).getVersion());
            if (forks == null || forks.size() < 2 || locked == null) {
                return null;
            }
            Set<String> forcing = new LinkedHashSet<>();
            boolean[] everywhere = {false};
            Map<String, Set<String>> live = liveExtras();
            forEachDependentRequirement(target, live, (dependent, raw, req) -> {
                if (req.getSpecifiers() == null || req.getSpecifiers().contains(locked, true)) {
                    return;
                }
                if (req.getMarker() == null) {
                    everywhere[0] = true;
                    return;
                }
                forcing.addAll(forksWhere(raw.substring(raw.indexOf(';') + 1),
                        live.getOrDefault(dependent.getName(), Collections.emptySet())));
            });
            PythonVersionSpecifierSet constraint = constraintSpecifiers(target);
            if (everywhere[0] || forcing.isEmpty() || forcing.containsAll(forksReaching(target, forks)) ||
                    constraint != null && !constraint.contains(locked, true)) {
                return null;
            }
            List<String> ordered = new ArrayList<>();
            for (String fork : forks) {
                if (forcing.contains(fork)) {
                    ordered.add(fork);
                }
            }
            return ordered;
        }

        /**
         * The forks some live requirement on the package reaches. A package moved in all of them moves as one
         * entry, as the forks it no longer reaches keep nothing of it.
         */
        private Set<String> forksReaching(String target, List<String> forkList) {
            Set<String> reached = new LinkedHashSet<>();
            Map<String, Set<String>> live = liveExtras();
            forEachDependentRequirement(target, live, (dependent, raw, req) ->
                    reached.addAll(req.getMarker() == null ? forkList : forksWhere(raw.substring(raw.indexOf(';') + 1),
                            live.getOrDefault(dependent.getName(), Collections.emptySet()), forkList)));
            for (int i = 0; i < packages.size(); i++) {
                UvLockPackage q = packages.get(i);
                if (i == rootIndex || q.getSource().getType() == UvLockSource.Type.REGISTRY ||
                        !live.containsKey(q.getName()) || !dependsOn(q, target)) {
                    continue;
                }
                reached.addAll(forkList);
            }
            UvLockMetadata rootMeta = packages.get(rootIndex).getMetadata();
            List<UvLockRequirement> declared = new ArrayList<>();
            if (rootMeta != null && rootMeta.getRequiresDist() != null) {
                declared.addAll(rootMeta.getRequiresDist());
            }
            if (rootMeta != null && rootMeta.getRequiresDev() != null) {
                rootMeta.getRequiresDev().values().forEach(declared::addAll);
            }
            for (UvLockRequirement req : declared) {
                if (req.getName().equals(target)) {
                    reached.addAll(req.getMarker() == null ? forkList :
                            forksWhere(req.getMarker(), Collections.emptySet(), forkList));
                }
            }
            return reached;
        }

        // the forks a requirement's marker is not disjoint from, with the dependent's active extras taken as true
        private Set<String> forksWhere(String marker, Set<String> activeExtras) {
            return forksWhere(marker, activeExtras, requireNonNull(lock.getResolutionMarkers()));
        }

        private Set<String> forksWhere(String marker, Set<String> activeExtras, List<String> forkList) {
            Set<String> out = new LinkedHashSet<>();
            try {
                MarkerInterner interner = new MarkerInterner();
                MarkerTree tree = MarkerTree.parse(interner, marker).simplifyExtrasWith(activeExtras::contains);
                for (String fork : forkList) {
                    if (!tree.isDisjoint(MarkerTree.parse(interner, fork))) {
                        out.add(fork);
                    }
                }
                return out;
            } catch (MarkerParser.MarkerSyntaxException e) {
                return new LinkedHashSet<>(forkList);
            }
        }

        // a package held once per fork stays while each fork's version meets the requirements reaching that fork
        private void requireEveryForkSatisfied(String target) {
            Map<String, Set<String>> live = liveExtras();
            forEachDependentRequirement(target, live, (dependent, raw, req) -> {
                String marker = req.getMarker() != null ? raw.substring(raw.indexOf(';') + 1).trim() : null;
                requireForksSatisfied(target, req.getSpecifiers(), marker,
                        live.getOrDefault(dependent.getName(), Collections.emptySet()));
            });
            UvLockMetadata rootMeta = packages.get(rootIndex).getMetadata();
            List<UvLockRequirement> declared = new ArrayList<>();
            if (rootMeta != null && rootMeta.getRequiresDist() != null) {
                declared.addAll(rootMeta.getRequiresDist());
            }
            if (rootMeta != null && rootMeta.getRequiresDev() != null) {
                rootMeta.getRequiresDev().values().forEach(declared::addAll);
            }
            for (UvLockRequirement req : declared) {
                if (req.getName().equals(target) && req.getSpecifier() != null) {
                    requireForksSatisfied(target, PythonVersionSpecifierSet.parse(req.getSpecifier()),
                            req.getMarker(), null);
                }
            }
            PythonVersionSpecifierSet constraint = constraintSpecifiers(target);
            requireForksSatisfied(target, constraint, null, null);
        }

        private void requireForksSatisfied(String target, @Nullable PythonVersionSpecifierSet specifiers,
                                           @Nullable String marker, @Nullable Collection<String> activeExtras) {
            if (specifiers == null) {
                return;
            }
            for (UvLockPackage pkg : packages) {
                PythonVersion version = pkg.getName().equals(target) ? PythonVersion.parse(pkg.getVersion()) : null;
                List<String> forks = pkg.getResolutionMarkers();
                if (version == null || specifiers.contains(version, true) ||
                        marker != null && forks != null && !reachesForks(marker, activeExtras, forks)) {
                    continue;
                }
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, target,
                        "Editing a fork-duplicated package is not supported by the native engine");
            }
        }

        private boolean reachesForks(String marker, @Nullable Collection<String> activeExtras, List<String> forks) {
            Set<String> reached = forksWhere(marker,
                    activeExtras != null ? new HashSet<>(activeExtras) : Collections.<String>emptySet());
            return forks.stream().anyMatch(reached::contains);
        }

        /** The package at a new version in the forcing forks, beside the locked version in the others. */
        private void splitAcrossForks(String target, List<String> forcing) {
            splitAcrossForks(target, forcing, requireNonNull(lock.getResolutionMarkers()));
        }

        private void splitAcrossForks(String target, List<String> forcing, List<String> allForks) {
            int index = singlePackageIndex(target);
            UvLockPackage old = packages.get(index);
            if (old.getResolutionMarkers() != null || old.getSource().getType() != UvLockSource.Type.REGISTRY) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, target,
                        "Splitting " + target + " across the lock's forks is supported for a single registry entry " +
                                "only");
            }
            Change change = new Change(target);
            change.constraints.addAll(gatherConstraints(target));
            List<String> pins = indexPins.get(target);
            change.pinnedIndexName = pins != null ? pins.get(0) : null;
            UvLockPackage moved = resolveEntry(change, PythonVersion.parse(old.getVersion()), old);
            List<String> others = new ArrayList<>(allForks);
            others.removeAll(forcing);
            packages.set(index, old.withResolutionMarkers(others));
            packages.add(moved.withResolutionMarkers(forcing));
        }

        /**
         * Every unqualified edge into a name the lock holds several entries of becomes one edge per entry, its marker
         * the edge's own and its entry's forks, as uv writes edges into fork-duplicated packages.
         */
        private void qualifyEdgesIntoSplitPackages() {
            Map<String, List<UvLockPackage>> byName = new LinkedHashMap<>();
            for (UvLockPackage pkg : packages) {
                byName.computeIfAbsent(pkg.getName(), k -> new ArrayList<>()).add(pkg);
            }
            byName.values().removeIf(entries -> entries.size() < 2 ||
                    entries.stream().anyMatch(e -> e.getResolutionMarkers() == null));
            // uv lists the edges into one package's entries by version, as it orders packages
            for (List<UvLockPackage> entries : byName.values()) {
                entries.sort(Comparator.comparing(e -> PythonVersion.parse(e.getVersion()),
                        Comparator.nullsFirst(Comparator.naturalOrder())));
            }
            if (byName.isEmpty()) {
                return;
            }
            for (int i = 0; i < packages.size(); i++) {
                UvLockPackage pkg = packages.get(i);
                UvLockPackage.UvLockPackageBuilder b = pkg.toBuilder()
                        .dependencies(qualified(pkg.getDependencies(), byName));
                if (pkg.getOptionalDependencies() != null) {
                    Map<String, List<UvLockDependency>> groups = new LinkedHashMap<>();
                    pkg.getOptionalDependencies().forEach((k, v) -> groups.put(k, qualified(v, byName)));
                    b.optionalDependencies(groups);
                }
                if (pkg.getDevDependencies() != null) {
                    Map<String, List<UvLockDependency>> groups = new LinkedHashMap<>();
                    pkg.getDevDependencies().forEach((k, v) -> groups.put(k, qualified(v, byName)));
                    b.devDependencies(groups);
                }
                packages.set(i, b.build());
            }
        }

        private @Nullable List<UvLockDependency> qualified(@Nullable List<UvLockDependency> edges,
                                                           Map<String, List<UvLockPackage>> split) {
            if (edges == null) {
                return null;
            }
            List<UvLockDependency> out = new ArrayList<>();
            for (UvLockDependency edge : edges) {
                List<UvLockPackage> entries = split.get(edge.getName());
                if (entries == null || edge.getVersion() != null) {
                    out.add(edge);
                    continue;
                }
                for (UvLockPackage entry : entries) {
                    try {
                        MarkerInterner interner = new MarkerInterner();
                        MarkerTree marker = edge.getMarker() == null ? MarkerTree.trueIn(interner) :
                                MarkerTree.parse(interner, edge.getMarker());
                        MarkerTree forks = MarkerTree.trueIn(interner).negate();
                        for (String fork : requireNonNull(entry.getResolutionMarkers())) {
                            forks = forks.or(MarkerTree.parse(interner, fork));
                        }
                        MarkerTree both = marker.and(forks);
                        if (!both.isFalse()) {
                            out.add(edge.withVersion(entry.getVersion()).withSource(entry.getSource())
                                    .withMarker(both.tryToString()));
                        }
                    } catch (MarkerParser.MarkerSyntaxException e) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, edge.getName(),
                                "Qualifying edges into " + edge.getName() + " needs the marker \"" +
                                        edge.getMarker() + "\"");
                    }
                }
            }
            return out;
        }

        private boolean anyVersionSatisfies(String target, @Nullable String pinnedIndex,
                                            List<PythonVersionSpecifierSet> constraints) {
            Map<PythonVersion, List<PackageFile>> byVersion =
                    groupByVersion(fetchListing(target, pinnedIndex).listing, target);
            for (Map.Entry<PythonVersion, List<PackageFile>> e : byVersion.entrySet()) {
                if (satisfiesAll(constraints, e.getKey()) && versionAdmitsPython(e.getValue())) {
                    return true;
                }
            }
            return false;
        }

        private List<PythonVersionSpecifierSet> gatherConstraints(String target) {
            return gatherConstraints(target, dependent -> true);
        }

        /** The requirements on the target from the root, the constraints, and the dependents the predicate accepts. */
        private List<PythonVersionSpecifierSet> gatherConstraints(String target, Predicate<String> fromDependent) {
            List<PythonVersionSpecifierSet> constraints = new ArrayList<>();
            Pep508Requirement override = overrideDependencies.get(target);
            if (override != null) {
                if (override.getSpecifiers() != null && !override.getSpecifiers().isMatchAll()) {
                    constraints.add(override.getSpecifiers());
                }
                PythonVersionSpecifierSet constraint = constraintSpecifiers(target);
                if (constraint != null) {
                    constraints.add(constraint);
                }
                return constraints;
            }
            Map<String, Set<String>> live = liveExtras();
            forEachDependentRequirement(target, live, (dependent, raw, req) -> {
                PythonVersionSpecifierSet specs = req.getSpecifiers();
                if (specs != null && !specs.isMatchAll() && fromDependent.test(dependent.getName())) {
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
         * A cascade rediscovered an already-settled package. Its settled version must still meet the root's
         * requirements, the constraints, and the dependents the run moved, since those do not move again.
         * Dependents the run left alone are revised afterwards, and a pending fork move is settled later.
         * An extra the new edge activates on it gets its group.
         */
        private boolean revisitSettled(String target, Set<String> rewritten) {
            if (pendingForkMoves.contains(target)) {
                return false;
            }
            int index = findSinglePackage(target);
            if (index < 0) {
                return false;
            }
            PythonVersion version = PythonVersion.parse(packages.get(index).getVersion());
            if (version == null) {
                return false;
            }
            for (PythonVersionSpecifierSet constraint : gatherConstraints(target, rewritten::contains)) {
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
                        if ("required-environments".equals(key)) {
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

        // only one plain override per package, as uv records it under [manifest] overrides, is reproduced
        private void readOverrideDependencies() {
            Toml.Table uv = UvLockToml.findTable(pyproject, "tool.uv");
            List<UvLockRequirement> declared = new ArrayList<>();
            List<String> specs = uv == null ? Collections.<String>emptyList() : UvLockToml.stringArray(uv, "override-dependencies");
            for (String spec : specs) {
                Pep508Requirement requirement = Pep508Requirement.parse(spec);
                if (requirement == null) {
                    throw new EngineFailure(Reason.MALFORMED_MANIFEST, null,
                            "Unparseable override-dependencies entry: " + spec);
                }
                String name = requirement.getCanonicalName();
                if (requirement.getMarker() != null || requirement.getUrl() != null ||
                        overrideDependencies.containsKey(name)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                            "Only one plain override per package is supported under [tool.uv] " +
                                    "override-dependencies: " + spec);
                }
                overrideDependencies.put(name, requirement);
                List<String> extras = requirement.getExtras().isEmpty() ? null :
                        new ArrayList<>(requirement.getExtras());
                declared.add(new UvLockRequirement(name, extras, null, null,
                        UvLockMetadataBuilder.normalizeSpecifier(requirement), null, null, null, null, null));
            }
            List<UvLockRequirement> recorded = lock.getManifest() != null && lock.getManifest().getOverrides() != null ?
                    lock.getManifest().getOverrides() : emptyList();
            if (!new HashSet<>(declared).equals(new HashSet<>(recorded))) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                        "Changing [tool.uv] override-dependencies is not supported by the native engine");
            }
        }

        // the requirements with each one on an overridden package replaced by the override under its own marker
        private List<String> overridden(List<String> requiresDist) {
            if (overrideDependencies.isEmpty()) {
                return requiresDist;
            }
            List<String> out = new ArrayList<>(requiresDist.size());
            for (String raw : requiresDist) {
                Pep508Requirement requirement = Pep508Requirement.parse(raw);
                Pep508Requirement override = requirement == null ? null :
                        overrideDependencies.get(requirement.getCanonicalName());
                if (override == null) {
                    out.add(raw);
                    continue;
                }
                StringBuilder replaced = new StringBuilder(override.getName());
                if (!override.getExtras().isEmpty()) {
                    replaced.append('[').append(String.join(",", override.getExtras())).append(']');
                }
                if (override.getSpecifiers() != null) {
                    replaced.append(UvLockMetadataBuilder.normalizeSpecifier(override));
                }
                int semi = raw.indexOf(';');
                if (requirement.getMarker() != null && semi >= 0) {
                    replaced.append("; ").append(raw.substring(semi + 1).trim());
                }
                out.add(replaced.toString());
            }
            return out;
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

        private int entriesNamed(String canonicalName) {
            int count = 0;
            for (UvLockPackage pkg : packages) {
                count += pkg.getName().equals(canonicalName) ? 1 : 0;
            }
            return count;
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
                if (version == null || excludedAsNewer(pkg, file)) {
                    continue;
                }
                List<PackageFile> files = byVersion.computeIfAbsent(version, k -> new ArrayList<>());
                files.add(file);
            }
            return byVersion;
        }

        // a file without an upload time is excluded too, as uv cannot place it before the cutoff
        private boolean excludedAsNewer(String pkg, PackageFile file) {
            Instant cutoff = excludeNewerCutoff;
            Map<String, Object> perPackage = lock.getOptions() != null ? lock.getOptions().getExcludeNewerPackage() : null;
            Object override = perPackage != null ? perPackage.get(pkg) : null;
            if (Boolean.FALSE.equals(override)) {
                return false;
            } else if (override instanceof String) {
                cutoff = Instant.parse((String) override);
            }
            if (cutoff == null) {
                return false;
            }
            if (file.getUploadTime() == null) {
                return true;
            }
            return Instant.parse(file.getUploadTime()).toEpochMilli() >= cutoff.toEpochMilli();
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
        /**
         * uv decides a package the lock already holds before one a requirement newly brings in, so a new
         * package's version that would move a locked one loses to an older version that keeps it. The newest
         * of the few candidates whose requirements every locked version meets, or the selection when none does.
         */
        private PythonVersion keepingLockedVersions(Map<PythonVersion, List<PackageFile>> byVersion, Change change,
                                                    UvIndex index, PythonVersion selected) {
            int tried = 0;
            Set<String> blockers = new HashSet<>();
            for (PythonVersion candidate : new TreeSet<>(byVersion.keySet()).headSet(selected, true).descendingSet()) {
                if (candidate.isPrerelease() || !satisfiesAll(change.constraints, candidate) ||
                        !installable(byVersion.get(candidate)) || !versionAdmitsPython(byVersion.get(candidate))) {
                    continue;
                }
                if (++tried > 10) {
                    break;
                }
                Set<String> moves =
                        lockedBlockers(fetchMetadata(change.canonicalName, index, byVersion.get(candidate)));
                if (moves.isEmpty()) {
                    if (!candidate.equals(selected)) {
                        backedOff.put(change.canonicalName, blockers);
                    }
                    return candidate;
                }
                blockers.addAll(moves);
            }
            return selected;
        }

        private boolean keepsLockedVersions(CoreMetadata metadata) {
            return lockedBlockers(metadata).isEmpty();
        }

        /**
         * A new package backed off to keep locked versions is taken at its newest once every locked package
         * that blocked the newer ones has moved anyway. Reports whether it took one.
         */
        private boolean reconsiderBackoff() {
            Map<String, String> original = new HashMap<>();
            for (UvLockPackage pkg : lock.getPackages()) {
                original.merge(pkg.getName(), String.valueOf(pkg.getVersion()), (a, b) -> a);
            }
            for (Iterator<Map.Entry<String, Set<String>>> it = backedOff.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Set<String>> e = it.next();
                boolean allMoved = true;
                for (String blocker : e.getValue()) {
                    int index = entriesNamed(blocker) == 1 ? findSinglePackage(blocker) : -1;
                    allMoved &= index >= 0 &&
                            !String.valueOf(packages.get(index).getVersion()).equals(original.get(blocker));
                }
                int index = findSinglePackage(e.getKey());
                if (!allMoved || index < 0) {
                    continue;
                }
                it.remove();
                Change change = new Change(e.getKey());
                change.constraints.addAll(gatherConstraints(e.getKey()));
                List<String> pins = indexPins.get(e.getKey());
                change.pinnedIndexName = pins != null ? pins.get(0) : null;
                packages.set(index, resolveEntry(change, null, UvLockPackage.builder().name(e.getKey()).build()));
                addLowerForkEntry();
                return true;
            }
            return false;
        }

        // the locked packages whose versions the requirements would move
        private Set<String> lockedBlockers(CoreMetadata metadata) {
            Set<String> blockers = new HashSet<>();
            for (String raw : overridden(metadata.getRequiresDist())) {
                Pep508Requirement requirement = Pep508Requirement.parse(raw);
                int semi = raw.indexOf(';');
                if (requirement == null || requirement.getSpecifiers() == null ||
                        semi >= 0 && UvLockMarkers.referencesExtraVariable(raw.substring(semi + 1))) {
                    continue;
                }
                if (semi >= 0) {
                    String recorded = UvLockMarkers.recordMarker(raw.substring(semi + 1).trim(), null);
                    List<String[]> clauses = recorded != null ? UvLockMarkers.parseSimpleAndChain(recorded) : null;
                    if (clauses != null &&
                            UvLockMarkers.DROP_EDGE.equals(UvLockMarkers.simplifyClauses(range, clauses))) {
                        continue;
                    }
                }
                String target = requirement.getCanonicalName();
                if (entriesNamed(target) != 1) {
                    continue;
                }
                PythonVersion locked = PythonVersion.parse(packages.get(findSinglePackage(target)).getVersion());
                if (locked != null && !requirement.getSpecifiers().contains(locked, true)) {
                    blockers.add(target);
                }
            }
            return blockers;
        }

        private boolean wouldFork(Map<PythonVersion, List<PackageFile>> byVersion, Change change, PythonVersion selected) {
            PythonVersion floor = selectionFloor != null ? selectionFloor : range.lower;
            if (floor == null) {
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
                    if (fileRange.lower != null && fileRange.lower.compareTo(floor) > 0 &&
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
            return admitsPythonFrom(files, selectionFloor != null ? selectionFloor : range.lower);
        }

        private static boolean admitsPythonFrom(List<PackageFile> files, @Nullable PythonVersion lower) {
            if (lower == null) {
                return true;
            }
            for (PackageFile file : files) {
                String requires = file.getRequiresPython();
                if (requires == null) {
                    return true;
                }
                PythonVersionSpecifierSet set = PythonVersionSpecifierSet.parse(requires);
                if (set == null || set.contains(lower, true)) {
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
            Map<String, List<@Nullable String>> splits = withBase ? convergingSplits(metadata) : emptyMap();
            for (List<@Nullable String> markers : splits.values()) {
                for (String marker : markers) {
                    if (marker != null) {
                        splitForks(marker, false);
                    }
                }
            }
            for (String raw : overridden(metadata.getRequiresDist())) {
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
                    UvLockDependency edge = edgeTo(pkg, requirement, marker);
                    if (group == null && splits.containsKey(target)) {
                        noteForkMove(target, requirement, marker);
                    }
                    List<UvLockDependency> placed = group == null ? edges.base :
                            edges.groups.computeIfAbsent(group, k -> new ArrayList<>());
                    if (!seen.add(group + '\0' + target)) {
                        mergeEdge(pkg, placed, edge);
                        continue;
                    }
                    placed.add(edge);
                }
            }

            edges.base.sort(Comparator.comparing(UvLockDependency::getName));
            for (List<UvLockDependency> entries : edges.groups.values()) {
                entries.sort(Comparator.comparing(UvLockDependency::getName));
            }
            return edges;
        }

        /**
         * Targets named by several firing base requirements under differing markers, each mapped to those markers,
         * null for an unmarked one. uv forks the resolution on them unless the lock's forks already split there.
         */
        private Map<String, List<@Nullable String>> convergingSplits(CoreMetadata metadata) {
            Map<String, List<@Nullable String>> byTarget = new LinkedHashMap<>();
            for (String raw : overridden(metadata.getRequiresDist())) {
                Pep508Requirement requirement = Pep508Requirement.parse(raw);
                if (requirement == null) {
                    continue;
                }
                int semi = raw.indexOf(';');
                String marker = null;
                if (requirement.getMarker() != null && semi >= 0) {
                    String rawMarker = raw.substring(semi + 1).trim();
                    if (UvLockMarkers.referencesExtraVariable(rawMarker)) {
                        continue;
                    }
                    String recorded = UvLockMarkers.recordMarker(rawMarker, null);
                    List<String[]> clauses = recorded != null ? UvLockMarkers.parseSimpleAndChain(recorded) : null;
                    marker = clauses != null ? UvLockMarkers.simplifyClauses(range, clauses) : recorded;
                    if (UvLockMarkers.DROP_EDGE.equals(marker)) {
                        continue;
                    }
                }
                byTarget.computeIfAbsent(requirement.getCanonicalName(), k -> new ArrayList<>()).add(marker);
            }
            byTarget.values().removeIf(markers -> new HashSet<>(markers).size() < 2);
            return byTarget;
        }

        /**
         * Splits each fork on the marker and its negation where both sides stay within the Python range. A lone
         * requirement splits only the forks whose Python lower bound its marker raises, as uv forks on it.
         */
        private void splitForks(String marker, boolean onlyWhereItRaisesPython) {
            try {
                MarkerInterner interner = new MarkerInterner();
                Ranges.Bound<PythonVersion> lower = range.lower == null ? Ranges.Bound.unbounded() :
                        range.lowerExclusive ? Ranges.Bound.excluded(range.lower) : Ranges.Bound.included(range.lower);
                Ranges.Bound<PythonVersion> upper = range.upper == null ? Ranges.Bound.unbounded() :
                        range.upperInclusive ? Ranges.Bound.included(range.upper) : Ranges.Bound.excluded(range.upper);
                PythonVersion raisedTo = null;
                if (onlyWhereItRaisesPython) {
                    raisedTo = impliedPythonLower(marker);
                    if (raisedTo == null || range.lower != null && raisedTo.compareTo(range.lower) <= 0) {
                        return;
                    }
                }
                MarkerTree split = MarkerTree.parse(interner, marker);
                List<String> current = forks != null ? forks : lock.getResolutionMarkers() != null ?
                        lock.getResolutionMarkers() : singletonList(null);
                List<String> next = new ArrayList<>();
                for (String fork : current) {
                    MarkerTree f = fork == null ? MarkerTree.trueIn(interner) : MarkerTree.parse(interner, fork);
                    PythonVersion forkLower = fork == null ? range.lower : impliedPythonLower(fork);
                    if (raisedTo != null && forkLower != null && raisedTo.compareTo(forkLower) <= 0) {
                        next.add(fork);
                        continue;
                    }
                    MarkerTree in = f.and(split).simplifyPythonVersions(lower, upper);
                    MarkerTree out = f.and(split.negate()).simplifyPythonVersions(lower, upper);
                    if (in.isFalse() || out.isFalse()) {
                        next.add(fork);
                    } else {
                        next.add(requireNonNull(in.tryToString()));
                        next.add(requireNonNull(out.tryToString()));
                    }
                }
                if (!next.equals(current)) {
                    forks = next;
                }
            } catch (MarkerParser.MarkerSyntaxException e) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                        "Splitting the resolution on the marker \"" + marker + "\" is not supported");
            }
        }

        // uv prefers the locked version in each fork, so a requirement it fails moves it only where that applies
        private void noteForkMove(String target, Pep508Requirement requirement, @Nullable String marker) {
            if (entriesNamed(target) != 1) {
                return;
            }
            PythonVersion locked = PythonVersion.parse(packages.get(findSinglePackage(target)).getVersion());
            if (locked == null) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, target,
                        "Locked version of " + target + " is not PEP 440");
            }
            if (marker != null && requirement.getSpecifiers() != null &&
                    !requirement.getSpecifiers().contains(locked, true)) {
                pendingForkMoves.add(target);
            }
        }

        /**
         * Moves one pending target to a new version in the forks some requirement its locked version fails reaches,
         * keeping the locked version elsewhere, or as one entry when every fork moves it. Reports whether it moved one.
         */
        private boolean applyForkMove(Set<String> rewritten) {
            Iterator<String> pending = pendingForkMoves.iterator();
            if (!pending.hasNext()) {
                return false;
            }
            String target = pending.next();
            pending.remove();
            List<String> all = forks != null ? inSolveOrder(forks) : lock.getResolutionMarkers();
            if (all == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, target,
                        "Requirements on " + target + " move it in part of the range without forking it");
            }
            PythonVersion locked =
                    requireNonNull(PythonVersion.parse(packages.get(singlePackageIndex(target)).getVersion()));
            Set<String> reached = new LinkedHashSet<>();
            PythonVersionSpecifierSet constraint = constraintSpecifiers(target);
            if (constraint != null && !constraint.contains(locked, true)) {
                reached.addAll(all);
            }
            Map<String, Set<String>> live = liveExtras();
            forEachDependentRequirement(target, live, (dependent, raw, req) -> {
                if (req.getSpecifiers() == null || req.getSpecifiers().contains(locked, true)) {
                    return;
                }
                reached.addAll(req.getMarker() == null ? all : forksWhere(raw.substring(raw.indexOf(';') + 1),
                        live.getOrDefault(dependent.getName(), Collections.emptySet()), all));
            });
            List<String> forcing = new ArrayList<>();
            for (String fork : all) {
                if (reached.contains(fork)) {
                    forcing.add(fork);
                }
            }
            if (forcing.isEmpty()) {
                return false;
            }
            if (forcing.containsAll(forksReaching(target, all))) {
                Change change = new Change(target);
                change.constraints.addAll(gatherConstraints(target));
                List<String> pins = indexPins.get(target);
                change.pinnedIndexName = pins != null ? pins.get(0) : null;
                applyChange(change);
            } else {
                splitAcrossForks(target, forcing, all);
            }
            rewritten.add(target);
            return true;
        }

        /**
         * uv resolves a requirement under a marker as a package of its own, which first tries the locked version
         * and then the newest one its specifiers allow. Once a run moves the package, the locked version no longer
         * fits, and a newest version needing a higher Python than the range allows forks the resolution there.
         */
        private void addProxyForks(Set<String> rewritten) {
            Map<String, Set<String>> live = liveExtras();
            for (String name : rewritten) {
                if (entriesNamed(name) != 1) {
                    continue;
                }
                int index = findSinglePackage(name);
                UvLockPackage before = null;
                for (UvLockPackage pkg : lock.getPackages()) {
                    before = pkg.getName().equals(name) ? pkg : before;
                }
                PythonVersion moved = index < 0 ? null : PythonVersion.parse(packages.get(index).getVersion());
                PythonVersion locked = before == null ? null : PythonVersion.parse(before.getVersion());
                if (moved == null || locked == null || moved.equals(locked)) {
                    continue;
                }
                List<String> pins = indexPins.get(name);
                Map<PythonVersion, List<PackageFile>> byVersion =
                        groupByVersion(fetchListing(name, pins != null ? pins.get(0) : null).listing, name);
                forEachDependentRequirement(name, live, (dependent, raw, req) -> {
                    if (req.getMarker() == null) {
                        return;
                    }
                    PythonVersion newest = null;
                    for (Map.Entry<PythonVersion, List<PackageFile>> e : byVersion.entrySet()) {
                        PythonVersion v = e.getKey();
                        if (!v.equals(locked) && !v.isPrerelease() &&
                                e.getValue().stream().anyMatch(f -> !f.isYanked()) &&
                                (req.getSpecifiers() == null || req.getSpecifiers().contains(v, true)) &&
                                (newest == null || v.compareTo(newest) > 0)) {
                            newest = v;
                        }
                    }
                    if (newest == null || newest.equals(moved) || versionAdmitsPython(byVersion.get(newest))) {
                        return;
                    }
                    PythonVersion floor = requiredPythonFloor(byVersion.get(newest));
                    if (floor == null) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                                name + " " + newest + " forks the resolution on a requires-python the engine " +
                                        "cannot place");
                    }
                    splitForks("python_full_version >= '" + floor + "'", false);
                });
            }
        }

        // the lowest Python a version's files require, when they all name the same plain lower bound
        private static @Nullable PythonVersion requiredPythonFloor(List<PackageFile> files) {
            PythonVersion floor = null;
            for (PackageFile file : files) {
                PyRange r = file.getRequiresPython() == null ? null : PyRange.parse(file.getRequiresPython());
                if (r == null || r.lower == null || floor != null && !floor.equals(r.lower)) {
                    return null;
                }
                floor = r.lower;
            }
            return floor;
        }

        /**
         * The forks a converging split adds to an unforked lock without duplicating any package, or null for none.
         * That holds only while no package the run moved would take a newer version under a fork's narrower range.
         */
        /**
         * An entry recorded under a fork the run has since split holds for each new fork that falls inside it,
         * which is how uv writes the entry once the resolution's forks are finer than its own.
         */
        private void refineEntryForks(List<String> forks) {
            MarkerInterner interner = new MarkerInterner();
            for (int i = 0; i < packages.size(); i++) {
                List<String> recorded = packages.get(i).getResolutionMarkers();
                if (recorded == null || forks.containsAll(recorded)) {
                    continue;
                }
                List<String> refined = new ArrayList<>();
                try {
                    MarkerTree covered = MarkerTree.trueIn(interner).negate();
                    for (String marker : recorded) {
                        covered = covered.or(MarkerTree.parse(interner, marker));
                    }
                    for (String fork : forks) {
                        if (MarkerTree.parse(interner, fork).and(covered.negate()).isFalse()) {
                            refined.add(fork);
                        }
                    }
                } catch (MarkerParser.MarkerSyntaxException e) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, packages.get(i).getName(),
                            "Re-expressing " + packages.get(i).getName() + "'s forks needs its recorded markers");
                }
                if (refined.isEmpty()) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, packages.get(i).getName(),
                            "The run's forks leave " + packages.get(i).getName() + " no fork of its own");
                }
                packages.set(i, packages.get(i).withResolutionMarkers(refined));
            }
        }

        private @Nullable List<String> convergentForks(Set<String> rewritten) {
            if (forks == null) {
                return null;
            }
            for (String name : rewritten) {
                if (entriesNamed(name) != 1) {
                    continue;
                }
                int index = findSinglePackage(name);
                PythonVersion version = index < 0 ? null : PythonVersion.parse(packages.get(index).getVersion());
                if (version == null || packages.get(index).getSource().getType() != UvLockSource.Type.REGISTRY) {
                    continue;
                }
                List<String> pins = indexPins.get(name);
                Map<PythonVersion, List<PackageFile>> byVersion =
                        groupByVersion(fetchListing(name, pins != null ? pins.get(0) : null).listing, name);
                List<PythonVersionSpecifierSet> constraints = gatherConstraints(name);
                for (Map.Entry<PythonVersion, List<PackageFile>> e : byVersion.entrySet()) {
                    if (e.getKey().compareTo(version) <= 0 || e.getKey().isPrerelease() ||
                            !satisfiesAll(constraints, e.getKey()) || versionAdmitsPython(e.getValue())) {
                        continue;
                    }
                    for (String fork : forks) {
                        PyRange r = forkRange(fork);
                        if (r != null && r.lower != null && admitsPythonFrom(e.getValue(), r.lower)) {
                            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                                    name + " " + e.getKey() + " fits the fork " + fork +
                                            " but not the whole Python range");
                        }
                    }
                }
            }
            return inSolveOrder(forks);
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
                List<String[]> byExtra = placementsByExtra(rawMarker);
                if (byExtra != null) {
                    return byExtra;
                }
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                        "Extra-gated requirement marker \"" + rawMarker + "\" is not a disjunction of and-chains with " +
                                "one extra == clause each; placing it in optional-dependencies groups is not supported by " +
                                "the native engine");
            }
            return placements;
        }

        private static final Pattern EXTRA_EQUALS = Pattern.compile("extra\\s*==\\s*['\"]([^'\"]+)['\"]");

        /**
         * The marker split into one placement per extra it names, each the rest of the marker with that extra
         * true and the others false. Null when the marker holds with no extra or compares extras otherwise.
         */
        private @Nullable List<String[]> placementsByExtra(String rawMarker) {
            Set<String> extras = new TreeSet<>();
            Matcher m = EXTRA_EQUALS.matcher(rawMarker);
            while (m.find()) {
                extras.add(Pep508Requirement.canonicalize(m.group(1)));
            }
            if (extras.isEmpty() || rawMarker.replaceAll(EXTRA_EQUALS.pattern(), "").contains("extra")) {
                return null;
            }
            try {
                MarkerTree tree = MarkerTree.parse(new MarkerInterner(), rawMarker);
                if (!tree.simplifyNotExtrasWith(x -> true).isFalse()) {
                    return null;
                }
                List<String[]> placements = new ArrayList<>();
                for (String extra : extras) {
                    MarkerTree rest = tree.simplifyExtrasWith(x -> Pep508Requirement.canonicalize(x).equals(extra))
                            .simplifyNotExtrasWith(x -> !Pep508Requirement.canonicalize(x).equals(extra));
                    if (!rest.isFalse()) {
                        placements.add(new String[]{extra, rest.tryToString()});
                    }
                }
                return placements;
            } catch (MarkerParser.MarkerSyntaxException e) {
                return null;
            }
        }

        private static String quoteMarkerValue(String value) {
            return value.indexOf('\'') < 0 ? "'" + value + "'" : "\"" + value + "\"";
        }

        // uv records one edge for several requirements on a package, under the union of their markers
        private void mergeEdge(String pkg, List<UvLockDependency> placed, UvLockDependency edge) {
            for (int i = 0; i < placed.size(); i++) {
                UvLockDependency prior = placed.get(i);
                if (!prior.getName().equals(edge.getName())) {
                    continue;
                }
                if (!Objects.equals(prior.getExtra(), edge.getExtra())) {
                    break;
                }
                if (prior.getMarker() == null || edge.getMarker() == null) {
                    placed.set(i, prior.withMarker(null));
                    return;
                }
                try {
                    MarkerInterner interner = new MarkerInterner();
                    MarkerTree union = MarkerTree.parse(interner, prior.getMarker())
                            .or(MarkerTree.parse(interner, edge.getMarker()));
                    placed.set(i, prior.withMarker(union.isTrue() ? null : union.tryToString()));
                    return;
                } catch (MarkerParser.MarkerSyntaxException e) {
                    break;
                }
            }
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg,
                    "Multiple requirements on " + edge.getName() + " need marker-aware merging");
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
            if (entriesNamed(target) > 1) {
                requireForksSatisfied(target, requirement.getSpecifiers(), marker, extras);
                return new UvLockDependency(target, null, null, extras, marker);
            }
            int targetIndex = findSinglePackage(target);
            if (targetIndex < 0) {
                PythonVersion floor = marker == null ? null : impliedPythonLower(marker);
                if (floor == null || range.lower != null && floor.compareTo(range.lower) <= 0) {
                    unflooredAdditions.add(target);
                } else {
                    additionFloors.merge(target, floor, (a, b) -> a.compareTo(b) <= 0 ? a : b);
                }
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
            for (String raw : overridden(metadata.getRequiresDist())) {
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
                if (simplified != null && !UvLockMarkers.DROP_EDGE.equals(simplified)) {
                    splitForks(simplified, true);
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
            String uploadTime = lock.expectsUploadTime() || migrating ? formatUploadTime(file.getUploadTime()) : null;
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
            UvLockPackage root = packages.get(rootIndex);
            UvLockPackage.UvLockPackageBuilder builder = root.toBuilder();
            if (removal.devGroup != null) {
                builder.devDependencies(removeGroupEdge(root.getDevDependencies(), removal.devGroup, removal));
            } else if (removal.extraGroup != null) {
                builder.optionalDependencies(
                        removeGroupEdge(root.getOptionalDependencies(), removal.extraGroup, removal));
            } else {
                List<UvLockDependency> edges = removeEdge(root.getDependencies(), removal);
                builder.dependencies(edges != null && edges.isEmpty() ? null : edges);
            }
            packages.set(rootIndex, builder.build());
        }

        private @Nullable Map<String, List<UvLockDependency>> removeGroupEdge(
                @Nullable Map<String, List<UvLockDependency>> groups, String group, Removal removal) {
            if (groups == null) {
                return null;
            }
            Map<String, List<UvLockDependency>> updated = new LinkedHashMap<>(groups);
            List<UvLockDependency> edges = removeEdge(updated.get(group), removal);
            if (edges == null || edges.isEmpty()) {
                updated.remove(group);
            } else {
                updated.put(group, edges);
            }
            return updated.isEmpty() ? null : updated;
        }

        private @Nullable List<UvLockDependency> removeEdge(@Nullable List<UvLockDependency> edges, Removal removal) {
            if (edges == null) {
                return null;
            }
            String name = removal.canonicalName;
            List<UvLockDependency> updated = new ArrayList<>();
            for (UvLockDependency edge : edges) {
                if (edge.getName().equals(name) && removal.remainingMarkers != null) {
                    updated.add(narrowed(edge, removal.remainingMarkers));
                    continue;
                }
                if (edge.getName().equals(name)) {
                    continue;
                }
                updated.add(edge);
            }
            return updated;
        }

        // the edge as the remaining declarations require it, refused where they no longer reach it, as uv then reshapes the forks
        private UvLockDependency narrowed(UvLockDependency edge, List<@Nullable String> remainingMarkers) {
            try {
                MarkerInterner interner = new MarkerInterner();
                MarkerTree declared = MarkerTree.trueIn(interner).negate();
                for (String marker : remainingMarkers) {
                    declared = declared.or(marker == null ? MarkerTree.trueIn(interner) :
                            MarkerTree.parse(interner, marker));
                }
                MarkerTree marker = (edge.getMarker() == null ? MarkerTree.trueIn(interner) :
                        MarkerTree.parse(interner, edge.getMarker())).and(declared);
                if (marker.isFalse()) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, edge.getName(),
                            "Removing the declarations that reach " + edge.getName() + " " + edge.getVersion() +
                                    " changes the lock's forks, which the native engine does not reproduce");
                }
                return edge.withMarker(marker.isTrue() ? null : marker.tryToString());
            } catch (MarkerParser.MarkerSyntaxException e) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, edge.getName(),
                        "Narrowing the edge into " + edge.getName() + " needs the marker \"" + edge.getMarker() + "\"");
            }
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
