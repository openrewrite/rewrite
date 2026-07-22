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
package org.openrewrite.python.internal.poetrylock;

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
import org.openrewrite.python.internal.PyprojectData;
import org.openrewrite.python.internal.index.*;
import org.openrewrite.python.internal.metadata.CoreMetadata;
import org.openrewrite.python.internal.metadata.LazyWheelMetadataReader;
import org.openrewrite.python.internal.metadata.Pep658MetadataFetcher;
import org.openrewrite.python.internal.metadata.SdistMetadataReader;
import org.openrewrite.python.internal.pep440.PythonVersion;
import org.openrewrite.python.internal.pep440.PythonVersionSpecifierSet;
import org.openrewrite.python.internal.pep508.Pep508Requirement;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;

import java.util.*;

/**
 * Regenerates poetry.lock natively from an edited pyproject.toml. Scope (v1): a surgical version
 * change of one or more direct dependencies that resolve to a leaf package (no dependency edges and
 * no extras), where the closure is provably unchanged. Everything else — adding or removing a
 * dependency, changing a dependency with its own edges/extras, non-registry sources, or a Python
 * bound change — fails loud with {@link Reason#RESOLUTION_REQUIRED} so a real relock never disagrees.
 */
public final class PoetryLockEngine {

    private PoetryLockEngine() {
    }

    public static Result regenerate(String pyprojectContent, @Nullable String oldLockContent, ExecutionContext ctx) {
        return regenerate(pyprojectContent, null, oldLockContent, ctx);
    }

    public static Result regenerate(String pyprojectContent, @Nullable String originalPyprojectContent,
                                    @Nullable String oldLockContent, ExecutionContext ctx) {
        if (oldLockContent == null) {
            return Result.failure(new Failure(Reason.RESOLUTION_REQUIRED, null, null,
                    "No existing poetry.lock; locking from scratch requires full dependency resolution, " +
                            "which the native engine does not support"));
        }
        PoetryLock lock;
        try {
            lock = PoetryLockReader.parse(oldLockContent);
        } catch (PoetryLockFormatException e) {
            return Result.failure(new Failure(Reason.MALFORMED_LOCK, null, null,
                    "Existing poetry.lock could not be parsed: " + e.getMessage()));
        }
        Toml.Document pyproject = parseToml(pyprojectContent);
        if (pyproject == null) {
            return Result.failure(new Failure(Reason.MALFORMED_MANIFEST, null, null,
                    "Edited pyproject.toml could not be parsed as TOML"));
        }
        // Best-effort: a null or unparseable original falls back to whole-pyproject reconciliation.
        Toml.Document originalPyproject = originalPyprojectContent == null ? null : parseToml(originalPyprojectContent);
        try {
            return new Run(pyproject, originalPyproject, lock, ctx).regenerate();
        } catch (PythonIndexException e) {
            return Result.failure(indexFailure(null, e));
        } catch (PoetryLockFormatException e) {
            return Result.failure(new Failure(Reason.MALFORMED_LOCK, null, null,
                    "Regenerated lock could not be emitted in poetry's format: " + e.getMessage()));
        }
    }

    private static Toml.@Nullable Document parseToml(String content) {
        SourceFile parsed = new TomlParser()
                .parse(new InMemoryExecutionContext(), content)
                .findFirst()
                .orElse(null);
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

    private static final class Change {
        final int packageIndex;
        final String canonicalName;
        final PythonVersionSpecifierSet constraint;

        Change(int packageIndex, String canonicalName, PythonVersionSpecifierSet constraint) {
            this.packageIndex = packageIndex;
            this.canonicalName = canonicalName;
            this.constraint = constraint;
        }
    }

    private static final class EngineFailure extends RuntimeException {
        final Failure failure;

        EngineFailure(Reason reason, @Nullable String packageName, String detail) {
            super(detail);
            this.failure = new Failure(reason, packageName, null, detail);
        }
    }

    private static final class Run {
        private final Toml.Document pyproject;
        private final Toml.@Nullable Document originalPyproject;
        private final PoetryLock lock;
        private final HttpSender http;
        private final SimpleIndexClient simpleClient;
        private final FlatIndexClient flatClient;
        private final List<PythonPackageIndex> indexes;
        private final @Nullable PythonVersion pythonLowerBound;

        private List<PoetryLockPackage> packages;

        Run(Toml.Document pyproject, Toml.@Nullable Document originalPyproject, PoetryLock lock, ExecutionContext ctx) {
            this.pyproject = pyproject;
            this.originalPyproject = originalPyproject;
            this.lock = lock;
            this.http = HttpSenderExecutionContextView.view(ctx).getHttpSender();
            this.simpleClient = new SimpleIndexClient(http);
            this.flatClient = new FlatIndexClient(http);
            this.indexes = IndexDiscovery.discover(ctx, null, null, Environment.SYSTEM);
            this.pythonLowerBound = lowerBound(lock.getPythonVersions());
        }

        Result regenerate() {
            try {
                return run();
            } catch (EngineFailure e) {
                return Result.failure(e.failure);
            }
        }

        private Result run() {
            packages = new ArrayList<>(lock.getPackages());
            String newHash = PoetryContentHash.hash(pyproject);

            Map<String, @Nullable PythonVersionSpecifierSet> declared = collectDirectDeps(pyproject);
            Map<String, @Nullable PythonVersionSpecifierSet> originalDeclared =
                    originalPyproject == null ? null : collectDirectDeps(originalPyproject);
            Set<String> changedPackages = changedNames(originalDeclared, declared);

            Map<String, Integer> byName = new HashMap<>();
            for (int i = 0; i < packages.size(); i++) {
                byName.put(Pep508Requirement.canonicalize(packages.get(i).getName()), i);
            }

            List<Change> changes = new ArrayList<>();
            for (Map.Entry<String, @Nullable PythonVersionSpecifierSet> dep : declared.entrySet()) {
                // Leave pre-existing drift in a dependency the recipe never touched exactly as-is.
                if (changedPackages != null && !changedPackages.contains(dep.getKey())) {
                    continue;
                }
                Integer idx = byName.get(dep.getKey());
                if (idx == null) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep.getKey(),
                            "Declared dependency " + dep.getKey() + " is not in the existing lock; " +
                                    "adding a dependency requires resolution, which the native engine does not support");
                }
                PythonVersionSpecifierSet constraint = dep.getValue();
                if (constraint == null) {
                    continue;
                }
                PythonVersion locked = PythonVersion.parse(packages.get(idx).getVersion());
                if (locked == null || !constraint.contains(locked, true)) {
                    changes.add(new Change(idx, dep.getKey(), constraint));
                }
            }

            // Tolerate orphans that already existed under the original declarations; only fail loud
            // on packages this edit orphaned (a removal or closure-shrinking change).
            Set<String> tolerated = originalDeclared == null ?
                    Collections.emptySet() : orphansUnder(originalDeclared.keySet(), byName);
            requireNoOrphans(declared.keySet(), byName, tolerated);

            for (Change change : changes) {
                applyLeafChange(change);
            }

            PoetryLock updated = lock.withPackages(packages).withContentHash(newHash);
            return Result.success(PoetryLockWriter.write(updated));
        }

        private static @Nullable Set<String> changedNames(
                @Nullable Map<String, @Nullable PythonVersionSpecifierSet> original,
                Map<String, @Nullable PythonVersionSpecifierSet> edited) {
            if (original == null) {
                return null;
            }
            Set<String> changed = new HashSet<>();
            Set<String> names = new HashSet<>(original.keySet());
            names.addAll(edited.keySet());
            for (String name : names) {
                if (!Objects.equals(original.get(name), edited.get(name))) {
                    changed.add(name);
                }
            }
            return changed;
        }

        /**
         * Every locked package must be reachable by name from a declared direct dependency, so a
         * pure version change leaves no orphan. A removed dependency (or one pruned by an upgrade)
         * shows up as an orphan and defers to a real relock.
         */
        private void requireNoOrphans(Set<String> roots, Map<String, Integer> byName, Set<String> tolerated) {
            Set<String> reachable = reachableFrom(roots, byName);
            for (PoetryLockPackage pkg : packages) {
                String canonical = Pep508Requirement.canonicalize(pkg.getName());
                if (!reachable.contains(canonical) && !tolerated.contains(canonical)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, pkg.getName(),
                            "Locked package " + pkg.getName() + " is not reachable from the declared " +
                                    "dependencies; pruning or adding a dependency requires resolution");
                }
            }
        }

        private Set<String> orphansUnder(Set<String> roots, Map<String, Integer> byName) {
            Set<String> reachable = reachableFrom(roots, byName);
            Set<String> orphans = new HashSet<>();
            for (PoetryLockPackage pkg : packages) {
                String canonical = Pep508Requirement.canonicalize(pkg.getName());
                if (!reachable.contains(canonical)) {
                    orphans.add(canonical);
                }
            }
            return orphans;
        }

        private Set<String> reachableFrom(Set<String> roots, Map<String, Integer> byName) {
            Set<String> reachable = new HashSet<>();
            Deque<String> queue = new ArrayDeque<>();
            for (String root : roots) {
                if (byName.containsKey(root) && reachable.add(root)) {
                    queue.add(root);
                }
            }
            while (!queue.isEmpty()) {
                PoetryLockPackage pkg = packages.get(byName.get(queue.poll()));
                if (pkg.getDependencies() == null) {
                    continue;
                }
                for (PoetryLockDependency edge : pkg.getDependencies()) {
                    String canonical = Pep508Requirement.canonicalize(edge.getName());
                    if (byName.containsKey(canonical) && reachable.add(canonical)) {
                        queue.add(canonical);
                    }
                }
            }
            return reachable;
        }

        private void applyLeafChange(Change change) {
            PoetryLockPackage oldPkg = packages.get(change.packageIndex);
            if (oldPkg.getDependencies() != null || oldPkg.getExtras() != null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, change.canonicalName,
                        "Changing " + change.canonicalName + ", which has its own dependencies or extras, " +
                                "requires resolution; the native engine only changes leaf packages");
            }
            if (oldPkg.getSource() != null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, change.canonicalName,
                        "Changing the non-registry (source) package " + change.canonicalName + " is not supported");
            }

            Listing listing = fetchListing(change.canonicalName);
            Map<PythonVersion, List<PackageFile>> byVersion = groupByVersion(listing.listing, change.canonicalName);
            PythonVersion target = selectVersion(byVersion, change.constraint);
            if (target == null) {
                throw new EngineFailure(Reason.RESOLUTION_CONFLICT, change.canonicalName,
                        "No version of " + change.canonicalName + " matching the declared constraint is " +
                                "available at " + listing.index.getUrl());
            }

            List<PackageFile> files = byVersion.get(target);
            CoreMetadata metadata = fetchMetadata(change.canonicalName, listing.index, files);
            if (!metadata.getRequiresDist().isEmpty() || !metadata.getProvidesExtra().isEmpty()) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, change.canonicalName,
                        "Version " + target + " of " + change.canonicalName + " declares dependencies or extras; " +
                                "the native engine only regenerates leaf packages");
            }

            List<PoetryLockFile> lockFiles = new ArrayList<>();
            for (PackageFile file : files) {
                lockFiles.add(new PoetryLockFile(file.getFilename(), "sha256:" + sha256(change.canonicalName, listing.index, file)));
            }
            lockFiles.sort(Comparator.comparing(PoetryLockFile::getFile));

            String description = metadata.getSummary() != null ? metadata.getSummary() : oldPkg.getDescription();
            // poetry records python-versions verbatim from the index's per-file requires-python
            String pythonVersions = "*";
            for (PackageFile file : files) {
                if (file.getRequiresPython() != null) {
                    pythonVersions = file.getRequiresPython();
                    break;
                }
            }

            packages.set(change.packageIndex, oldPkg
                    .withVersion(target.toString())
                    .withDescription(description)
                    .withPythonVersions(pythonVersions)
                    .withFiles(lockFiles));
        }

        // ---- index access ----

        private static final class Listing {
            final PythonPackageIndex index;
            final PackageListing listing;

            Listing(PythonPackageIndex index, PackageListing listing) {
                this.index = index;
                this.listing = listing;
            }
        }

        private Listing fetchListing(String canonicalName) {
            PythonIndexException notFound = null;
            for (PythonPackageIndex index : indexes) {
                try {
                    PackageListing listing = simpleClient.listFiles(index, canonicalName);
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
            throw new EngineFailure(Reason.PACKAGE_NOT_FOUND, canonicalName, "No configured index lists " + canonicalName);
        }

        private Map<PythonVersion, List<PackageFile>> groupByVersion(PackageListing listing, String pkg) {
            Map<PythonVersion, List<PackageFile>> byVersion = new LinkedHashMap<>();
            for (PackageFile file : listing.getFiles()) {
                if (file.isYanked()) {
                    continue;
                }
                DistFilename dist = DistFilename.parse(file.getFilename());
                if (dist == null || dist.getType() == DistFilename.Type.OTHER ||
                        !pkg.equals(Pep508Requirement.canonicalize(dist.getDistribution()))) {
                    continue;
                }
                PythonVersion version = PythonVersion.parse(dist.getVersion());
                if (version != null) {
                    byVersion.computeIfAbsent(version, k -> new ArrayList<>()).add(file);
                }
            }
            return byVersion;
        }

        private @Nullable PythonVersion selectVersion(Map<PythonVersion, List<PackageFile>> byVersion,
                                                      PythonVersionSpecifierSet constraint) {
            PythonVersion best = null;
            for (Map.Entry<PythonVersion, List<PackageFile>> candidate : byVersion.entrySet()) {
                PythonVersion version = candidate.getKey();
                if (!constraint.contains(version, true) || !versionAdmitsPython(candidate.getValue())) {
                    continue;
                }
                if (best == null || version.compareTo(best) > 0) {
                    best = version;
                }
            }
            return best;
        }

        private boolean versionAdmitsPython(List<PackageFile> files) {
            if (pythonLowerBound == null) {
                return true;
            }
            for (PackageFile file : files) {
                String requires = file.getRequiresPython();
                if (requires == null) {
                    return true;
                }
                PythonVersionSpecifierSet set = PythonVersionSpecifierSet.parse(requires);
                if (set == null || set.contains(pythonLowerBound, true)) {
                    return true;
                }
            }
            return false;
        }

        private CoreMetadata fetchMetadata(String pkg, PythonPackageIndex index, List<PackageFile> files) {
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
            }
            if (sdist == null) {
                throw new EngineFailure(Reason.INDEX_UNREACHABLE, pkg,
                        "Could not fetch dependency metadata for " + pkg);
            }
            CoreMetadata metadata = SdistMetadataReader.read(http, sdist.getUrl());
            if (metadata == null || !metadata.hasStaticRequiresDist()) {
                throw new EngineFailure(Reason.DYNAMIC_SDIST_METADATA, pkg,
                        "Sdist metadata of " + sdist.getFilename() + " does not declare static Requires-Dist (PEP 643)");
            }
            return metadata;
        }

        private String sha256(String pkg, PythonPackageIndex index, PackageFile file) {
            if (file.getSha256() != null) {
                return file.getSha256();
            }
            try (HttpSender.Response response = http.send(http.get(file.getUrl()).build())) {
                if (response.isSuccessful()) {
                    return Hashing.sha256Hex(response.getBodyAsBytes());
                }
            } catch (Exception ignored) {
                // fall through to the failure below
            }
            throw new EngineFailure(Reason.HASH_UNAVAILABLE, pkg,
                    "The index lists " + file.getFilename() + " without a sha256 digest, and it could not be downloaded to hash");
        }

        // ---- manifest reading ----

        @SuppressWarnings("unchecked")
        private Map<String, @Nullable PythonVersionSpecifierSet> collectDirectDeps(Toml.Document pyproject) {
            Map<String, Object> root = PyprojectData.toNestedMap(pyproject);
            Map<String, @Nullable PythonVersionSpecifierSet> deps = new LinkedHashMap<>();

            Map<String, Object> project = asMap(root.get("project"));
            addPep508List(deps, project.get("dependencies"));
            for (Object group : asMap(project.get("optional-dependencies")).values()) {
                addPep508List(deps, group);
            }
            for (Object group : asMap(root.get("dependency-groups")).values()) {
                addPep508List(deps, group);
            }

            Map<String, Object> toolPoetry = asMap(mapGet(asMap(root.get("tool")), "poetry"));
            addPoetryDeps(deps, asMap(toolPoetry.get("dependencies")));
            for (Object group : asMap(toolPoetry.get("group")).values()) {
                addPoetryDeps(deps, asMap(asMap(group).get("dependencies")));
            }
            return deps;
        }

        private void addPep508List(Map<String, @Nullable PythonVersionSpecifierSet> deps, @Nullable Object list) {
            if (!(list instanceof List)) {
                return;
            }
            for (Object item : (List<?>) list) {
                if (!(item instanceof String)) {
                    continue;
                }
                Pep508Requirement req = Pep508Requirement.parse((String) item);
                if (req != null) {
                    deps.put(req.getCanonicalName(), req.getSpecifiers());
                }
            }
        }

        private void addPoetryDeps(Map<String, @Nullable PythonVersionSpecifierSet> deps, Map<String, Object> table) {
            for (Map.Entry<String, Object> e : table.entrySet()) {
                if ("python".equals(e.getKey())) {
                    continue;
                }
                String canonical = Pep508Requirement.canonicalize(e.getKey());
                String constraint = poetryConstraintString(e.getValue());
                deps.put(canonical, constraint == null ? null : PoetryConstraint.toSpecifierSet(constraint));
            }
        }

        @SuppressWarnings("unchecked")
        private @Nullable String poetryConstraintString(@Nullable Object value) {
            if (value instanceof String) {
                return (String) value;
            }
            if (value instanceof Map) {
                Object version = ((Map<String, Object>) value).get("version");
                return version instanceof String ? (String) version : null;
            }
            return null;
        }

        private @Nullable Object mapGet(Map<String, Object> map, String key) {
            return map.get(key);
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> asMap(@Nullable Object value) {
            return value instanceof Map ? (Map<String, Object>) value : Collections.emptyMap();
        }
    }

    /**
     * The lower Python bound of a poetry {@code python-versions} constraint (caret/tilde/{@code >=}),
     * used to skip a candidate version whose Requires-Python would exclude the project.
     */
    static @Nullable PythonVersion lowerBound(@Nullable String pythonVersions) {
        if (pythonVersions == null) {
            return null;
        }
        String s = pythonVersions.trim();
        if (s.startsWith("^") || s.startsWith("~")) {
            return PythonVersion.parse(s.substring(1).trim());
        }
        if (s.startsWith(">=")) {
            return PythonVersion.parse(s.substring(2).trim());
        }
        if (s.startsWith(">")) {
            return PythonVersion.parse(s.substring(1).trim());
        }
        return null;
    }
}
