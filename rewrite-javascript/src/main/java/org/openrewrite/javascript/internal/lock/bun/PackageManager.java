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
package org.openrewrite.javascript.internal.lock.bun;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.RootRelativePath;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openrewrite.javascript.internal.lock.bun.Package.INVALID_PACKAGE_ID;

/**
 * Port of bun's src/install/PackageManager (bun 1.3.10) for a lock-only install: PackageManagerEnqueue.zig,
 * runTasks.zig, processDependencyList.zig, PackageManagerResolution.zig and resolvers/folder_resolver.zig.
 */
final class PackageManager {

    /**
     * What resolving a dependency needs that only a download, a clone or the host's disk could tell.
     */
    @Getter

    static final class ResolvedPackageResult {
        final Package pkg;
        final boolean isFirstTime;

        ResolvedPackageResult(Package pkg, boolean isFirstTime) {
            this.pkg = pkg;
            this.isFirstTime = isFirstTime;
        }
    }

    private static final class ResolveError extends Exception {
        enum Kind {
            DIST_TAG_NOT_FOUND, NO_MATCHING_VERSION, TOO_RECENT_VERSION
        }

        final Kind kind;

        ResolveError(Kind kind) {
            super(kind.name(), null, false, false);
            this.kind = kind;
        }
    }

    private static final class NetworkTask {
        final String name;
        final boolean extended;

        NetworkTask(String name, boolean extended) {
            this.name = name;
            this.extended = extended;
        }
    }

    Lockfile lockfile;
    final BunOptions options;
    final Log log;
    private final Map<String, String> projectManifests;
    private final ManifestSource registry;
    private final double nowMs;

    private final Map<Long, DependencyVersion> knownNpmAliases = new HashMap<>();
    private final Map<Long, PackageManifest> manifests = new HashMap<>();
    private final Map<String, Boolean> networkDedupeMap = new HashMap<>();
    private final Map<String, List<Integer>> taskQueue = new HashMap<>();
    private final ArrayDeque<NetworkTask> networkTasks = new ArrayDeque<>();
    private final ArrayDeque<int[]> dependencyListQueue = new ArrayDeque<>();
    final ArrayDeque<Integer> peerDependencies = new ArrayDeque<>();
    private final Map<String, Integer> folders = new HashMap<>();

    /**
     * An edge resolved from its packument: its spec and the version {@code findBestVersion} picked before the dedupe.
     */
    static final class Resolved {
        final int dependencyId;
        final DependencyVersion version;
        final PackageManifest manifest;
        final PackageManifest.FindResult pick;
        final Resolution resolution;

        Resolved(int dependencyId, DependencyVersion version, PackageManifest manifest,
                 PackageManifest.FindResult pick) {
            this.dependencyId = dependencyId;
            this.version = version;
            this.manifest = manifest;
            this.pick = pick;
            this.resolution = Resolution.npm(pick.version, pick.packageVersion.tarballUrl);
        }
    }

    /**
     * The edges resolved from a packument before the peers are.
     */
    final List<Resolved> firstPass = new ArrayList<>();
    private boolean peersStarted;

    /**
     * The names that had a package when the peers began, and the required peers of the other names the peers ran.
     */
    private @Nullable Set<Long> namesBeforePeers;
    private final Map<Integer, DependencyVersion> peersOfNewNames = new LinkedHashMap<>();

    /**
     * The peers queued before any manifest arrived, whose place at the head of the peer queue no timing changes.
     */
    final Set<Integer> peersQueuedFirst = new HashSet<>();
    private boolean manifestArrived;

    PackageManager(Lockfile lockfile, BunOptions options, Log log, Map<String, String> projectManifests,
                   ManifestSource registry, double nowMs) {
        this.lockfile = lockfile;
        this.options = options;
        this.log = log;
        this.projectManifests = projectManifests;
        this.registry = registry;
        this.nowMs = nowMs;
    }

    int pendingTaskCount() {
        return networkTasks.size();
    }

    /**
     * What {@code Dependency.parse} records when it parses an {@code npm:} alias with the package manager at hand.
     */
    void registerAliases(Lockfile from, Package pkg) {
        for (Dependency dep : from.dependenciesOf(pkg)) {
            if (dep.version.tag == DependencyVersion.Tag.NPM && dep.version.npm().isAlias) {
                knownNpmAliases.put(dep.nameHash, dep.version);
            }
        }
    }

    void enqueueDependencyWithMain(int id, Dependency dependency, int resolution, boolean installPeer) {
        if (dependency.behavior.isOptionalPeer()) {
            return;
        }

        String name = dependency.realname();
        long nameHash;
        switch (dependency.version.tag) {
            case DIST_TAG:
            case GIT:
            case GITHUB:
            case NPM:
            case TARBALL:
            case WORKSPACE:
                nameHash = Wyhash11.stringHash(name);
                break;
            default:
                nameHash = dependency.nameHash;
                break;
        }

        DependencyVersion version = null;
        if (dependency.version.tag == DependencyVersion.Tag.NPM) {
            DependencyVersion aliased = knownNpmAliases.get(nameHash);
            if (aliased != null) {
                SemverQuery.Group group = dependency.version.npm().version;
                lists:
                for (SemverQuery.List queries = aliased.npm().version.head; queries != null; queries = queries.next) {
                    for (SemverQuery query = queries.head; query != null; query = query.next) {
                        if (group.satisfies(query.range.left.version) || group.satisfies(query.range.right.version)) {
                            name = aliased.npm().name;
                            nameHash = Wyhash11.stringHash(name);
                            version = aliased;
                            break lists;
                        }
                    }
                }
            }
        }
        if (version == null &&
            !dependency.behavior.isWorkspace() &&
            (dependency.version.tag != DependencyVersion.Tag.NPM || !dependency.version.npm().isAlias)) {
            DependencyVersion override = lockfile.overrides.get(nameHash);
            if (override != null) {
                name = replacementName(name, override);
                nameHash = replacementNameHash(nameHash, override);
                version = override;
                if (override.tag == DependencyVersion.Tag.CATALOG) {
                    Dependency catalogDep = lockfile.catalogs.get(override.string(), name);
                    if (catalogDep != null) {
                        name = replacementName(name, catalogDep.version);
                        nameHash = replacementNameHash(nameHash, catalogDep.version);
                        version = catalogDep.version;
                    }
                }
            } else if (dependency.version.tag == DependencyVersion.Tag.CATALOG) {
                Dependency catalogDep = lockfile.catalogs.get(dependency.version.string(), name);
                if (catalogDep != null) {
                    name = replacementName(name, catalogDep.version);
                    nameHash = replacementNameHash(nameHash, catalogDep.version);
                    version = catalogDep.version;
                }
            }
        }
        if (version == null) {
            version = dependency.version;
        }
        if (installPeer && dependency.behavior.isPeer() && version.tag == DependencyVersion.Tag.NPM &&
            namesBeforePeers != null && !namesBeforePeers.contains(nameHash)) {
            peersOfNewNames.putIfAbsent(id, version);
        }

        switch (version.tag) {
            case DIST_TAG:
            case FOLDER:
            case NPM: {
                ResolvedPackageResult result;
                try {
                    result = getOrPutResolvedPackage(nameHash, name, dependency, version, dependency.behavior, id,
                            resolution, installPeer);
                } catch (ResolveError e) {
                    if (dependency.behavior.isRequired()) {
                        log.addError(resolveErrorMessage(e.kind, name, version));
                    }
                    return;
                }
                if (result != null) {
                    if (result.isFirstTime && result.pkg.dependenciesLen > 0) {
                        dependencyListQueue.add(new int[]{result.pkg.dependenciesOff, result.pkg.dependenciesLen});
                    }
                } else if (version.tag.isNPM()) {
                    if (!dependency.behavior.isPeer() || installPeer) {
                        if (!hasCreatedNetworkTask(name, dependency.behavior.isRequired())) {
                            networkTasks.add(new NetworkTask(name, options.getMinimumReleaseAgeMs() != null));
                        }
                    } else {
                        queuePeer(id);
                        return;
                    }
                    taskQueue.computeIfAbsent(name, k -> new ArrayList<>()).add(id);
                }
                return;
            }
            case GIT:
            case GITHUB:
            case TARBALL: {
                Resolution res;
                if (version.tag == DependencyVersion.Tag.TARBALL) {
                    DependencyVersion.TarballInfo tarball = version.tarball();
                    res = Resolution.of(tarball.remote ? Resolution.Tag.REMOTE_TARBALL : Resolution.Tag.LOCAL_TARBALL,
                            tarball.uri);
                } else {
                    res = Resolution.repository(version.tag == DependencyVersion.Tag.GIT ? Resolution.Tag.GIT :
                            Resolution.Tag.GITHUB, version.repository());
                }
                Integer pkgId = lockfile.getPackageID(nameHash, null, res);
                if (pkgId != null) {
                    assignResolution(id, pkgId);
                    return;
                }
                if (dependency.behavior.isPeer() && !installPeer) {
                    queuePeer(id);
                    return;
                }
                throw new CannotKnow((version.tag == DependencyVersion.Tag.TARBALL ? "tarball contents of " :
                        "git ref of ") + dependency.name + "@" + version.literal);
            }
            case SYMLINK:
            case WORKSPACE: {
                ResolvedPackageResult result;
                try {
                    result = getOrPutResolvedPackage(nameHash, name, dependency, version, dependency.behavior, id,
                            resolution, installPeer);
                } catch (ResolveError e) {
                    throw new IllegalStateException(e);
                }
                if (result != null) {
                    if (result.isFirstTime && result.pkg.dependenciesLen > 0) {
                        dependencyListQueue.add(new int[]{result.pkg.dependenciesOff, result.pkg.dependenciesLen});
                    }
                } else if (dependency.behavior.isRequired()) {
                    log.addError(version.tag == DependencyVersion.Tag.WORKSPACE ?
                            "Workspace dependency \"" + name + "\" not found" :
                            "Package \"" + name + "\" is not linked");
                }
                return;
            }
            default:
        }
    }

    private static String resolveErrorMessage(ResolveError.Kind kind, String name, DependencyVersion version) {
        switch (kind) {
            case DIST_TAG_NOT_FOUND:
                return "Package \"" + name + "\" with tag \"" + version.distTag().tag +
                       "\" not found, but package exists";
            case NO_MATCHING_VERSION:
                return "No version matching \"" + version.literal + "\" found for specifier \"" + name +
                       "\" (but package exists)";
            default:
                return version.tag == DependencyVersion.Tag.DIST_TAG ?
                        "Package \"" + name + "\" with tag \"" + version.distTag().tag +
                        "\" not found (all versions blocked by minimum-release-age)" :
                        "No version matching \"" + name + "\" found for specifier \"" + version.literal +
                        "\" (blocked by minimum-release-age)";
        }
    }

    // git, github and tarball specs have no name until their package.json is read
    private static String replacementName(String original, DependencyVersion replacement) {
        switch (replacement.tag) {
            case DIST_TAG:
                return replacement.distTag().name;
            case NPM:
                return replacement.npm().name;
            case GIT:
            case GITHUB:
                return replacement.repository().packageName;
            case TARBALL:
                return replacement.tarball().packageName;
            default:
                return original;
        }
    }

    private static long replacementNameHash(long original, DependencyVersion replacement) {
        switch (replacement.tag) {
            case DIST_TAG:
                return Wyhash11.stringHash(replacement.distTag().name);
            case NPM:
                return Wyhash11.stringHash(replacement.npm().name);
            default:
                return original;
        }
    }

    private boolean hasCreatedNetworkTask(String taskId, boolean isRequired) {
        Boolean existing = networkDedupeMap.get(taskId);
        networkDedupeMap.put(taskId, existing == null ? isRequired : existing || isRequired);
        return existing != null;
    }

    private @Nullable ResolvedPackageResult getOrPutResolvedPackageWithFindResult(
            long nameHash, String name, DependencyVersion version, int dependencyId, Behavior behavior,
            PackageManifest manifest, PackageManifest.FindResult findResult, boolean installPeer) {
        if (!peersStarted) {
            firstPass.add(new Resolved(dependencyId, version, manifest, findResult));
        }
        Integer id = lockfile.getPackageID(nameHash, version,
                Resolution.npm(findResult.version, findResult.packageVersion.tarballUrl));
        if (id != null) {
            assignResolution(dependencyId, id);
            return new ResolvedPackageResult(lockfile.packages.get(id), false);
        } else if (behavior.isPeer() && !installPeer) {
            return null;
        }

        Package pkg = lockfile.appendPackage(Package.fromNPM(lockfile, manifest, findResult.version,
                findResult.packageVersion, Features.NPM));
        registerAliases(lockfile, pkg);
        assignResolution(dependencyId, pkg.meta.id);
        return new ResolvedPackageResult(pkg, true);
    }

    private @Nullable ResolvedPackageResult getOrPutResolvedPackage(long nameHash, String name, Dependency dependency,
                                                                    DependencyVersion version, Behavior behavior,
                                                                    int dependencyId, int resolution,
                                                                    boolean installPeer) throws ResolveError {
        if (installPeer && behavior.isPeer()) {
            List<Integer> index = lockfile.packageIndex.get(nameHash);
            if (index != null) {
                for (int existingId : index) {
                    if (existingId < lockfile.packages.size() &&
                        resolutionSatisfiesDependency(lockfile.packages.get(existingId).resolution, version)) {
                        assignResolution(dependencyId, existingId);
                        return new ResolvedPackageResult(lockfile.packages.get(existingId), false);
                    }
                }
                int first = index.get(0);
                if (first < lockfile.packages.size()) {
                    Resolution.Tag resTag = lockfile.packages.get(first).resolution.tag;
                    DependencyVersion.Tag verTag = version.tag;
                    if (resTag == Resolution.Tag.NPM && verTag == DependencyVersion.Tag.NPM ||
                        resTag == Resolution.Tag.GIT && verTag == DependencyVersion.Tag.GIT ||
                        resTag == Resolution.Tag.GITHUB && verTag == DependencyVersion.Tag.GITHUB) {
                        assignResolution(dependencyId, first);
                        return new ResolvedPackageResult(lockfile.packages.get(first), false);
                    }
                }
            }
        }

        if (resolution < lockfile.packages.size()) {
            return new ResolvedPackageResult(lockfile.packages.get(resolution), false);
        }

        switch (version.tag) {
            case NPM:
            case DIST_TAG: {
                if (version.tag == DependencyVersion.Tag.NPM) {
                    String workspacePath = lockfile.workspacePaths.isEmpty() ? null :
                            lockfile.workspacePaths.get(nameHash);
                    SemverVersion workspaceVersion = lockfile.workspaceVersions.get(nameHash);
                    if (options.isLinkWorkspacePackages() &&
                        (workspaceVersion != null && version.npm().version.satisfies(workspaceVersion) ||
                         workspacePath != null && version.npm().version.isStar())) {
                        ResolvedPackageResult workspace = rootWorkspace(nameHash, dependencyId);
                        if (workspace != null) {
                            return workspace;
                        }
                    }
                }

                PackageManifest manifest = manifests.get(nameHash);
                if (manifest == null) {
                    return null;
                }

                PackageManifest.FindVersionResult versionResult = version.tag == DependencyVersion.Tag.DIST_TAG ?
                        manifest.findByDistTagWithFilter(version.distTag().tag, options.getMinimumReleaseAgeMs(),
                                options.getMinimumReleaseAgeExcludes(), nowMs) :
                        manifest.findBestVersionWithFilter(version.npm().version, options.getMinimumReleaseAgeMs(),
                                options.getMinimumReleaseAgeExcludes(), nowMs);
                if (versionResult.error == PackageManifest.FindVersionResult.Error.TOO_RECENT ||
                    versionResult.error == PackageManifest.FindVersionResult.Error.ALL_VERSIONS_TOO_RECENT) {
                    throw new ResolveError(ResolveError.Kind.TOO_RECENT_VERSION);
                }
                PackageManifest.FindResult findResult = versionResult.result;
                if (findResult == null) {
                    if (version.tag == DependencyVersion.Tag.DIST_TAG && !lockfile.workspacePaths.isEmpty() &&
                        lockfile.workspacePaths.get(nameHash) != null) {
                        ResolvedPackageResult workspace = rootWorkspace(nameHash, dependencyId);
                        if (workspace != null) {
                            return workspace;
                        }
                    }
                    if (behavior.isPeer()) {
                        return null;
                    }
                    throw new ResolveError(version.tag == DependencyVersion.Tag.NPM ?
                            ResolveError.Kind.NO_MATCHING_VERSION : ResolveError.Kind.DIST_TAG_NOT_FOUND);
                }
                return getOrPutResolvedPackageWithFindResult(nameHash, name, version, dependencyId, behavior,
                        manifest, findResult, installPeer);
            }
            case FOLDER: {
                if (lockfile.isWorkspaceDependency(dependencyId)) {
                    int[] res = folderResolution(Resolution.Tag.FOLDER, version, version.string(), dependency);
                    assignResolution(dependencyId, res[0]);
                    return new ResolvedPackageResult(lockfile.packages.get(res[0]), res[1] == 1);
                }
                Package pkg = new Package();
                pkg.name = name;
                pkg.nameHash = nameHash;
                pkg.resolution = Resolution.of(Resolution.Tag.FOLDER, version.string());
                pkg.scripts.filled = true;
                pkg.meta.hasInstallScript = false;
                pkg.dependenciesOff = lockfile.dependencies.size();
                pkg = lockfile.appendPackage(pkg);
                assignResolution(dependencyId, pkg.meta.id);
                return new ResolvedPackageResult(lockfile.packages.get(pkg.meta.id), true);
            }
            case WORKSPACE: {
                String workspacePath = lockfile.workspacePaths.get(nameHash);
                int[] res = folderResolution(Resolution.Tag.WORKSPACE, version,
                        workspacePath != null ? workspacePath : version.string(), dependency);
                assignResolution(dependencyId, res[0]);
                return new ResolvedPackageResult(lockfile.packages.get(res[0]), res[1] == 1);
            }
            case SYMLINK:
                throw new CannotKnow("linked package " + dependency.name + "@" + version.literal +
                                     " (bun reads it from the global link directory)");
            default:
                return null;
        }
    }

    private @Nullable ResolvedPackageResult rootWorkspace(long nameHash, int dependencyId) {
        Package root = lockfile.rootPackage();
        if (root == null) {
            return null;
        }
        for (int i = root.dependenciesOff; i < root.dependenciesEnd(); i++) {
            Dependency rootDep = lockfile.dependencies.get(i);
            int workspacePackageId = lockfile.resolutions.get(i);
            if (workspacePackageId != INVALID_PACKAGE_ID && rootDep.version.tag == DependencyVersion.Tag.WORKSPACE &&
                rootDep.nameHash == nameHash) {
                assignResolution(dependencyId, workspacePackageId);
                return new ResolvedPackageResult(lockfile.packages.get(workspacePackageId), false);
            }
        }
        return null;
    }

    /**
     * {@code FolderResolution.getOrPut} for a folder or workspace relative to the project: the package id read from its
     * package.json, replacing one resolved there, and 1 when it was read now.
     */
    private int[] folderResolution(Resolution.Tag tag, DependencyVersion version, String path, Dependency dependency) {
        String rel = RootRelativePath.normalize(path.replace('\\', '/'));
        Integer known = folders.get(rel);
        if (known != null) {
            return new int[]{known, 0};
        }
        String source = projectManifests.get(rel);
        if (source == null) {
            throw new CannotKnow((tag == Resolution.Tag.WORKSPACE ? "workspace member manifest of " :
                    "directory manifest of ") + dependency.name + "@" + version.literal + " (" +
                                 (rel.isEmpty() ? "." : rel) + "/package.json is not among the project's files)");
        }
        Json.Expr json = Json.parse(source);
        Package pkg = new Package();
        pkg.parseWithJSON(lockfile, options, log, rel, json,
                tag == Resolution.Tag.WORKSPACE ? Features.WORKSPACE : Features.FOLDER, projectManifests,
                Resolution.of(tag, rel));
        registerAliases(lockfile, pkg);
        Integer existingId = lockfile.getPackageID(pkg.nameHash, version, pkg.resolution);
        int id;
        if (existingId != null) {
            pkg.meta.id = existingId;
            lockfile.packages.set(existingId, pkg);
            id = existingId;
        } else {
            id = lockfile.appendPackage(pkg).meta.id;
        }
        folders.put(rel, id);
        return new int[]{id, 1};
    }

    private boolean resolutionSatisfiesDependency(Resolution resolution, DependencyVersion dependency) {
        if (resolution.tag == Resolution.Tag.NPM && dependency.tag == DependencyVersion.Tag.NPM) {
            return dependency.npm().version.satisfies(resolution.version);
        }
        if (resolution.tag == Resolution.Tag.GIT && dependency.tag == DependencyVersion.Tag.GIT ||
            resolution.tag == Resolution.Tag.GITHUB && dependency.tag == DependencyVersion.Tag.GITHUB) {
            return resolution.repository.eql(dependency.repository());
        }
        return false;
    }

    void assignResolution(int dependencyId, int packageId) {
        lockfile.resolutions.set(dependencyId, packageId);
        Dependency dep = lockfile.dependencies.get(dependencyId);
        if (dep.name.isEmpty() || dep.name.equals(dep.version.literal)) {
            Package pkg = lockfile.packages.get(packageId);
            dep.name = pkg.name;
            dep.nameHash = pkg.nameHash;
        }
    }

    void enqueueDependencyList(int off, int len) {
        List<Dependency> deps = lockfile.dependencies;
        int begin = off;
        int end = off + len;
        if (end - begin > 1 && deps.get(0).behavior.isPeer()) {
            int peerI = 0;
            Dependency peer = deps.get(peerI);
            while (peer.behavior.isPeer()) {
                int depI = end - 1;
                Dependency dep = deps.get(depI);
                while (!dep.behavior.isPeer()) {
                    if (!dep.behavior.isDev() && peer.nameHash == dep.nameHash) {
                        deps.set(peerI, deps.get(begin).copy());
                        begin++;
                        break;
                    }
                    depI--;
                    dep = deps.get(depI);
                }
                peerI++;
                if (peerI == end) {
                    break;
                }
                peer = deps.get(peerI);
            }
        }
        for (int i = begin; i < end; i++) {
            enqueueDependencyWithMain(i, deps.get(i), lockfile.resolutions.get(i), false);
        }
        drainDependencyList();
    }

    void drainDependencyList() {
        int[] dependenciesList;
        while ((dependenciesList = dependencyListQueue.poll()) != null) {
            int end = dependenciesList[0] + dependenciesList[1];
            for (int i = dependenciesList[0]; i < end; i++) {
                enqueueDependencyWithMain(i, lockfile.dependencies.get(i), lockfile.resolutions.get(i), false);
            }
        }
    }

    /**
     * One manifest arrives: the edges waiting for it are resolved, then whatever they queued.
     */
    void runTasks(boolean installPeer) {
        NetworkTask task = networkTasks.poll();
        if (task != null) {
            manifestArrived = true;
            String scope = options.scopeForPackageName(task.name);
            String text = registry.packument(scope, task.name, task.extended);
            if (text == null) {
                if (networkDedupeMap.getOrDefault(task.name, true)) {
                    log.addError("GET " + scope + task.name + " - 404");
                }
            } else {
                PackageManifest manifest = PackageManifest.parse(log, text, task.name, task.extended);
                if (manifest == null) {
                    log.addError("parsing package manifest for " + task.name);
                } else {
                    manifests.put(Wyhash11.stringHash(manifest.name), manifest);
                    List<Integer> dependencyList = taskQueue.remove(task.name);
                    if (dependencyList != null) {
                        processDependencyList(dependencyList, installPeer);
                    }
                }
            }
        }
        drainDependencyList();
    }

    private void processDependencyList(List<Integer> dependencyList, boolean installPeer) {
        for (int dependencyId : dependencyList) {
            enqueueDependencyWithMain(dependencyId, lockfile.dependencies.get(dependencyId),
                    lockfile.resolutions.get(dependencyId), installPeer);
        }
    }

    void processPeerDependencyList() {
        peersStarted = true;
        Integer peerDependencyId;
        while ((peerDependencyId = peerDependencies.poll()) != null) {
            enqueueDependencyWithMain(peerDependencyId, lockfile.dependencies.get(peerDependencyId),
                    lockfile.resolutions.get(peerDependencyId), true);
        }
    }

    /**
     * {@code installWithManager}'s wait for everything but the peers, then for the peers ({@code packagesBefore}: the
     * package count when enqueueing began); cannot-know where the first wait depends on manifest arrival order.
     */
    void resolve(int packagesBefore) {
        if (pendingTaskCount() == 0 && peerDependencies.isEmpty()) {
            return;
        }
        while (pendingTaskCount() > 0) {
            drainDependencyList();
            runTasks(false);
        }
        List<String> arrivalOrder = manifestArrivalOrder(packagesBefore);
        if (!arrivalOrder.isEmpty()) {
            throw new CannotKnow(String.join("; ", arrivalOrder));
        }
        peersStarted = true;
        namesBeforePeers = new HashSet<>(lockfile.packageIndex.keySet());
        if (!peerDependencies.isEmpty()) {
            processPeerDependencyList();
            drainDependencyList();
        }
        if (pendingTaskCount() > 0) {
            do {
                processPeerDependencyList();
                drainDependencyList();
                runTasks(true);
            } while (!peerDependencies.isEmpty() || pendingTaskCount() > 0);
        }
        List<String> peerOrder = peerArrivalOrder(peerPicks());
        if (!peerOrder.isEmpty()) {
            throw new CannotKnow(String.join("; ", peerOrder));
        }
    }

    private List<Resolved> peerPicks() {
        List<Resolved> picks = new ArrayList<>();
        for (Map.Entry<Integer, DependencyVersion> peer : peersOfNewNames.entrySet()) {
            DependencyVersion version = peer.getValue();
            PackageManifest manifest = manifests.get(Wyhash11.stringHash(version.npm().name));
            PackageManifest.FindResult pick = manifest == null ? null :
                    manifest.findBestVersionWithFilter(version.npm().version, options.getMinimumReleaseAgeMs(),
                            options.getMinimumReleaseAgeExcludes(), nowMs).result;
            if (pick != null) {
                picks.add(new Resolved(peer.getKey(), version, manifest, pick));
            }
        }
        return picks;
    }

    /**
     * The dedupe in {@code getOrPutResolvedPackageWithFindResult} that network timing decides: one line per name whose
     * result depends on the order its first-pass edges ran ({@code packagesBefore}: the count when the install began).
     */
    List<String> manifestArrivalOrder(int packagesBefore) {
        Map<String, List<Resolved>> byName = new LinkedHashMap<>();
        for (Resolved edge : firstPass) {
            if (!lockfile.dependencies.get(edge.dependencyId).behavior.isPeer()) {
                byName.computeIfAbsent(edge.manifest.name, k -> new ArrayList<>()).add(edge);
            }
        }
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, List<Resolved>> entry : byName.entrySet()) {
            List<Resolved> edges = entry.getValue();
            pairs:
            for (int i = 0; i < edges.size(); i++) {
                for (int j = 0; j < edges.size(); j++) {
                    if (i != j && dependsOnOrder(packagesBefore, edges.get(i), edges.get(j))) {
                        Resolved a = edges.get(Math.min(i, j));
                        Resolved b = edges.get(Math.max(i, j));
                        out.add("manifest arrival order: " + entry.getKey() + " for " + requirer(a) + " (" +
                                a.version.literal + ") and " + requirer(b) + " (" + b.version.literal +
                                ") resolved in one install");
                        break pairs;
                    }
                }
            }
        }
        return out;
    }

    private void queuePeer(int id) {
        if (!manifestArrived) {
            peersQueuedFirst.add(id);
        }
        peerDependencies.add(id);
    }

    /**
     * One line per name whose required peers nothing satisfied when the peers began and whose picks differ, unless the
     * first was queued before any manifest arrived: the order the requirers' manifests arrived in decides the pick.
     */
    List<String> peerArrivalOrder(List<Resolved> peers) {
        Map<String, List<Resolved>> byName = new LinkedHashMap<>();
        for (Resolved edge : peers) {
            byName.computeIfAbsent(edge.manifest.name, k -> new ArrayList<>()).add(edge);
        }
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, List<Resolved>> entry : byName.entrySet()) {
            List<Resolved> edges = entry.getValue();
            for (int i = 1; i < edges.size() && !peersQueuedFirst.contains(edges.get(0).dependencyId); i++) {
                if (!edges.get(i).resolution.eql(edges.get(0).resolution)) {
                    Resolved a = edges.get(0);
                    Resolved b = edges.get(i);
                    out.add("manifest arrival order: peer " + entry.getKey() + " for " + requirer(a) + " (" +
                            a.version.literal + ") and " + requirer(b) + " (" + b.version.literal +
                            ") installed in one install");
                    break;
                }
            }
        }
        return out;
    }

    // run first, `edge` takes the first pre-existing package it accepts (getPackageID walks highest first) or appends
    // its pick; run after `other` appended its pick, it takes that pick when it accepts it and it walks earlier
    private boolean dependsOnOrder(int packagesBefore, Resolved edge, Resolved other) {
        if (existing(packagesBefore, other) != null || !accepts(edge, other.resolution)) {
            return false;
        }
        Resolution first = existing(packagesBefore, edge);
        return first == null ? !other.resolution.eql(edge.resolution) : other.resolution.order(first) > 0;
    }

    @Nullable
    private Resolution existing(int packagesBefore, Resolved edge) {
        List<Integer> ids = lockfile.packageIndex.get(Wyhash11.stringHash(edge.manifest.name));
        if (ids != null) {
            for (int id : ids) {
                if (id < packagesBefore && accepts(edge, lockfile.packages.get(id).resolution)) {
                    return lockfile.packages.get(id).resolution;
                }
            }
        }
        return null;
    }

    private static boolean accepts(Resolved edge, Resolution candidate) {
        return candidate.eql(edge.resolution) ||
               candidate.tag == Resolution.Tag.NPM && edge.version.tag == DependencyVersion.Tag.NPM &&
               edge.version.npm().version.satisfies(candidate.version);
    }

    private String requirer(Resolved edge) {
        for (Package pkg : lockfile.packages) {
            if (pkg.containsDependency(edge.dependencyId)) {
                return pkg.resolution.tag == Resolution.Tag.ROOT ? pkg.name : pkg.name + "@" + pkg.resolution;
            }
        }
        return lockfile.dependencies.get(edge.dependencyId).name;
    }

    /**
     * {@code verifyResolutions}: each required dependency that did not resolve, as bun reports it.
     */
    static List<String> verifyResolutions(Lockfile lockfile, BunOptions options) {
        List<String> failed = new ArrayList<>();
        int end = lockfile.packages.size();
        for (Package parent : lockfile.packages) {
            boolean local = parent.resolution.tag == Resolution.Tag.ROOT ||
                            parent.resolution.tag == Resolution.Tag.WORKSPACE ||
                            parent.resolution.tag == Resolution.Tag.FOLDER;
            for (int i = parent.dependenciesOff; i < parent.dependenciesEnd(); i++) {
                if (lockfile.resolutions.get(i) < end) {
                    continue;
                }
                Dependency failedDep = lockfile.dependencies.get(i);
                if (failedDep.behavior.isPeer()) {
                    continue;
                }
                if (failedDep.behavior.optional() || !isEnabled(failedDep.behavior, local, options)) {
                    continue;
                }
                String literal = failedDep.version.literal;
                failed.add(failedDep.name.isEmpty() || failedDep.name.equals(literal) ? literal :
                        failedDep.name + "@" + literal);
            }
        }
        return failed;
    }

    private static boolean isEnabled(Behavior behavior, boolean local, BunOptions options) {
        if (local) {
            return behavior.isProd() ||
                   options.isLocalOptionalDependencies() && behavior.isOptional() ||
                   options.isLocalDevDependencies() && behavior.isDev() ||
                   options.isLocalPeerDependencies() && behavior.isPeer() ||
                   behavior.isWorkspace();
        }
        return behavior.isProd() ||
               options.isRemoteOptionalDependencies() && behavior.isOptional() ||
               options.isRemotePeerDependencies() && behavior.isPeer();
    }
}
