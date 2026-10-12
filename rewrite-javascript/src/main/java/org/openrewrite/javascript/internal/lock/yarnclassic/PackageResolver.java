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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.EventLoop;
import org.openrewrite.javascript.internal.lock.EventLoop.Promise;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.RegistrySource;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of yarn src/package-resolver.js (yarn 1.22.22): the resolver's state, a map from every pattern of the run to
 * its manifest and from each package name to its patterns, and the find loop over the root requests.
 */
final class PackageResolver {

    final Config config;
    final Lockfile lockfile;
    final ResolutionMap resolutionMap;
    final EventLoop loop;
    final NpmRegistry npmRegistry;
    final ProjectFiles files;
    final Host host;

    final Map<String, List<String>> patternsByPackage = new LinkedHashMap<>();
    final Set<String> fetchingPatterns = new LinkedHashSet<>();
    final Map<String, Manifest> patterns = new LinkedHashMap<>();
    final Set<String> usedRegistries = new LinkedHashSet<>();
    final List<DelayedResolve> delayedResolveQueue = new ArrayList<>();
    boolean flat;
    boolean frozen;
    @Nullable
    WorkspaceLayout workspaceLayout;

    /**
     * config.js {@code focus} and {@code focusedWorkspaceName}.
     */
    boolean focus;
    @Nullable
    Object focusedWorkspaceName;

    int rootIndex;
    int rootMark;
    int nextCopyHash;

    /**
     * Patterns that joined an entry another pattern of the same version created first, where the other could have
     * come first and the entries differ: the bytes follow network timing.
     */
    final List<Race> races = new ArrayList<>();

    /**
     * Two entries of one version yarn may resolve in either order: the one resolved first here, which the patterns
     * share, and the other's own.
     */
    @Value
    static class Race {
        String description;
        Manifest first;
        Manifest other;

        CannotKnow cannotKnow(String why) {
            return new CannotKnow("which of " + description + " yarn resolves first: " + why);
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @Value
    static class DelayedResolve {
        PackageRequest req;
        Manifest info;
    }

    PackageResolver(Config config, Lockfile lockfile, ResolutionMap resolutionMap,
                    RegistrySource registry, ProjectFiles files, Host host) {
        this.config = config;
        this.lockfile = lockfile;
        this.resolutionMap = resolutionMap;
        this.loop = new EventLoop();
        this.npmRegistry = new NpmRegistry(config, loop, registry);
        this.files = files;
        this.host = host;
    }

    void updateManifests(List<Manifest> newPkgs) {
        requireOneManifestPerPattern(newPkgs);
        for (Manifest newPkg : newPkgs) {
            if (newPkg.reference != null) {
                for (String pattern : newPkg.reference.patterns) {
                    Manifest oldPkg = patterns.get(pattern);
                    if (config.getRelease().keepsPrebuiltVariants()) {
                        newPkg.put("prebuiltVariants", oldPkg.get("prebuiltVariants"));
                    }
                    patterns.put(pattern, newPkg);
                }
            }
        }
    }

    /**
     * package-fetcher.js's queue hands the fetched manifests over as their fetches finish, and {@code updateManifests}
     * gives each the patterns its reference holds: a pattern two fetched references hold (a resolution's, which
     * {@code resolveToResolution} gave it, and the one a later request of it made) takes the manifest of the fetch that
     * finishes last.
     */
    private void requireOneManifestPerPattern(List<Manifest> newPkgs) {
        Map<String, Manifest> claimed = new LinkedHashMap<>();
        for (Manifest newPkg : newPkgs) {
            PackageReference ref = newPkg.reference;
            if (ref == null) {
                continue;
            }
            for (String pattern : ref.patterns) {
                Manifest other = claimed.putIfAbsent(pattern, newPkg);
                if (other != null && other.reference != ref &&
                    !(fingerprint(other.remote).equals(fingerprint(newPkg.remote)) &&
                      Js.strictEquals(other.getVersion(), newPkg.getVersion()))) {
                    throw new CannotKnow("which of the fetches of " + Js.templateString(other.getName()) + "@" +
                                         Js.templateString(other.getVersion()) + " and " +
                                         Js.templateString(newPkg.getName()) + "@" +
                                         Js.templateString(newPkg.getVersion()) + " yarn finishes last: both their " +
                                         "references hold " + pattern + ", which takes the last one's manifest");
                }
            }
        }
    }

    List<String> dedupePatterns(Iterable<String> patterns) {
        List<String> deduped = new ArrayList<>();
        Set<Manifest> seen = new HashSet<>();
        for (String pattern : patterns) {
            Manifest info = getResolvedPattern(pattern);
            if (!seen.add(info)) {
                continue;
            }
            deduped.add(pattern);
        }
        return deduped;
    }

    Set<Manifest> getLevelOrderManifests(List<String> seedPatterns) {
        Set<Manifest> pkgs = new LinkedHashSet<>();
        Set<Manifest> skip = new HashSet<>();
        addLevel(seedPatterns, pkgs, skip);
        return pkgs;
    }

    private void addLevel(List<String> seedPatterns, Set<Manifest> pkgs, Set<Manifest> skip) {
        List<PackageReference> refs = new ArrayList<>();
        for (String pattern : seedPatterns) {
            Manifest pkg = getStrictResolvedPattern(pattern);
            if (skip.contains(pkg)) {
                continue;
            }
            PackageReference ref = pkg.reference;
            if (ref == null) {
                throw new JsError(Js.INVARIANT_VIOLATION, "expected reference");
            }
            refs.add(ref);
            skip.add(pkg);
            pkgs.add(pkg);
        }
        for (PackageReference ref : refs) {
            addLevel(ref.dependencies, pkgs, skip);
        }
    }

    Set<String> getAllDependencyNamesByLevelOrder(List<String> seedPatterns) {
        Set<String> names = new LinkedHashSet<>();
        for (Manifest manifest : getLevelOrderManifests(seedPatterns)) {
            names.add(Js.string(manifest.getName()));
        }
        return names;
    }

    List<Manifest> getAllInfoForPackageName(String name) {
        List<String> byName = patternsByPackage.get(name);
        return getAllInfoForPatterns(byName == null ? Collections.emptyList() : byName);
    }

    List<Manifest> getAllInfoForPatterns(List<String> patterns) {
        List<Manifest> infos = new ArrayList<>();
        Set<Manifest> seen = new HashSet<>();
        for (String pattern : patterns) {
            Manifest info = this.patterns.get(pattern);
            if (seen.add(info)) {
                infos.add(info);
            }
        }
        return infos;
    }

    List<Manifest> getManifests() {
        List<Manifest> infos = new ArrayList<>();
        Set<Manifest> seen = new HashSet<>();
        for (String pattern : Js.keys(patterns)) {
            Manifest info = patterns.get(pattern);
            if (seen.add(info)) {
                infos.add(info);
            }
        }
        return infos;
    }

    String collapseAllVersionsOfPackage(String name, String version) {
        List<String> byName = patternsByPackage.get(name);
        return collapsePackageVersions(name, version, dedupePatterns(byName == null ?
                Collections.emptyList() : byName));
    }

    String collapsePackageVersions(String name, String version, List<String> patterns) {
        PackageReference collapseToReference = null;
        Manifest collapseToManifest = null;
        String collapseToPattern = null;
        for (String pattern : patterns) {
            Manifest manifest = this.patterns.get(pattern);
            if (version.equals(manifest.getVersion())) {
                collapseToReference = manifest.reference;
                collapseToManifest = manifest;
                collapseToPattern = pattern;
                break;
            }
        }
        if (collapseToReference == null || collapseToManifest == null) {
            throw new JsError(Js.INVARIANT_VIOLATION, "Couldn't find package manifest for " + name + "@" +
                                                           version);
        }
        for (String pattern : patterns) {
            if (pattern.equals(collapseToPattern)) {
                continue;
            }
            PackageReference ref = getStrictResolvedPattern(pattern).reference;
            if (ref == null) {
                throw new JsError(Js.INVARIANT_VIOLATION, "expected package reference");
            }
            List<String> refPatterns = new ArrayList<>(ref.patterns);
            ref.prune();
            for (String refPattern : refPatterns) {
                collapseToReference.addPattern(refPattern, collapseToManifest);
            }
        }
        return collapseToPattern;
    }

    void addPattern(String pattern, Manifest info) {
        patterns.put(pattern, info);
        List<String> byName = patternsByPackage.computeIfAbsent(Js.string(info.getName()), k -> new ArrayList<>());
        if (!byName.contains(pattern)) {
            byName.add(pattern);
        }
    }

    void removePattern(String pattern) {
        Manifest pkg = patterns.get(pattern);
        if (pkg == null) {
            return;
        }
        List<String> byName = patternsByPackage.get(Js.string(pkg.getName()));
        if (byName == null) {
            return;
        }
        // splice(indexOf(pattern), 1) drops the last pattern when the pattern is not among them
        int index = byName.indexOf(pattern);
        if (index >= 0 || !byName.isEmpty()) {
            byName.remove(index < 0 ? byName.size() - 1 : index);
        }
        patterns.remove(pattern);
    }

    @Nullable
    Manifest getResolvedPattern(String pattern) {
        return patterns.get(pattern);
    }

    Manifest getStrictResolvedPattern(String pattern) {
        Manifest manifest = getResolvedPattern(pattern);
        if (manifest == null) {
            throw new JsError(Js.INVARIANT_VIOLATION, "expected manifest");
        }
        return manifest;
    }

    /**
     * The manifest already resolved for the name whose version is {@code version}; a version of no string reaches
     * here only before 1.5.0 checks it ({@code invalidPackageVersion}).
     */
    @Nullable
    Manifest getExactVersionMatch(String name, @Nullable Object version, @Nullable Manifest manifest) {
        List<String> patterns = patternsByPackage.get(name);
        if (patterns == null) {
            return null;
        }
        for (String pattern : patterns) {
            Manifest info = getStrictResolvedPattern(pattern);
            if (Js.strictEquals(info.getVersion(), version)) {
                return info;
            }
        }
        if (manifest != null && ExoticResolvers.getExoticResolver(version, config.getRelease()) != null) {
            List<Manifest> resolved = new ArrayList<>();
            for (String pattern : patterns) {
                resolved.add(getStrictResolvedPattern(pattern));
            }
            return exoticRangeMatch(resolved, manifest);
        }
        return null;
    }

    /**
     * The manifest of the highest version already resolved for the name that satisfies the range, as node-semver
     * 5.5.0's {@code maxSatisfying} over the versions as the manifests hold them.
     */
    @Nullable
    Manifest getHighestRangeVersionMatch(String name, @Nullable Object range, @Nullable Manifest manifest) {
        List<String> patterns = patternsByPackage.get(name);
        if (patterns == null) {
            return null;
        }
        List<@Nullable Object> versionNumbers = new ArrayList<>();
        List<Manifest> resolvedPatterns = new ArrayList<>();
        for (String pattern : patterns) {
            Manifest info = getStrictResolvedPattern(pattern);
            versionNumbers.add(info.getVersion());
            resolvedPatterns.add(info);
        }
        // semver's Range of no string takes no version
        Object maxValidRange = range instanceof String ? maxSatisfying(versionNumbers, (String) range) : null;
        if (maxValidRange == null) {
            return manifest != null && ExoticResolvers.getExoticResolver(range, config.getRelease()) != null ?
                    exoticRangeMatch(resolvedPatterns, manifest) : null;
        }
        return resolvedPatterns.get(versionNumbers.indexOf(maxValidRange));
    }

    private static @Nullable Object maxSatisfying(List<@Nullable Object> versions, String range) {
        try {
            Object max = null;
            for (Object v : versions) {
                if (NodeSemver.satisfies(v, range, Config.SEMVER) &&
                    (max == null || NodeSemver.compare(max, v, Config.SEMVER) < 0)) {
                    max = v;
                }
            }
            return max;
        } catch (IllegalArgumentException e) {
            throw new JsError(JsError.TYPE_ERROR, e.getMessage());
        }
    }

    @Nullable
    Manifest exoticRangeMatch(List<Manifest> resolvedPkgs, Manifest manifest) {
        PackageRemote remote = manifest.remote;
        if (!(remote != null && Js.truthy(remote.reference) && "copy".equals(remote.type))) {
            return null;
        }
        for (Manifest pkg : resolvedPkgs) {
            PackageRemote pkgRemote = pkg.remote;
            if (pkgRemote != null && remote.reference.equals(pkgRemote.reference) && "copy".equals(pkgRemote.type)) {
                manifest.remote = pkgRemote;
                return pkg;
            }
        }
        return null;
    }

    /**
     * Whether a lock entry's version no longer satisfies its own pattern's range: the entry is set aside and the
     * pattern resolves as a new one.
     */
    boolean isLockfileEntryOutdated(@Nullable Object version, String range, boolean hasVersion) {
        return NodeSemver.validRange(range, Config.SEMVER) != null &&
               version instanceof String && NodeSemver.valid((String) version, Config.SEMVER) != null &&
               ExoticResolvers.getExoticResolver(range, config.getRelease()) == null &&
               hasVersion &&
               !NodeSemver.satisfies((String) version, range, Config.SEMVER);
    }

    Promise<Void> find(DependencyRequestPattern initialReq) {
        return Promise.async(loop, () -> {
            DependencyRequestPattern req = resolveToResolution(initialReq);
            if (req == null) {
                return Promise.resolved(loop, null);
            }
            YarnClassicRelease release = config.getRelease();
            String fetchKey = req.getRegistry() + ":" + req.getPattern() +
                              (release.tellsOptionalRequestsApart() ? ":" + req.isOptional() : "");
            boolean initialFetch = !fetchingPatterns.contains(fetchKey);
            if (!initialFetch && !release.findsARepeatedRequest()) {
                return Promise.resolved(loop, null);
            }
            PackageRequest request = new PackageRequest(req, this);
            boolean fresh = false;
            if (initialFetch) {
                fetchingPatterns.add(fetchKey);
                Map<String, @Nullable Object> lockfileEntry = lockfile.getLocked(req.getPattern());
                if (lockfileEntry != null) {
                    NormalizedPattern normalized = NormalizedPattern.normalizePattern(req.getPattern());
                    if (isLockfileEntryOutdated(lockfileEntry.get("version"), normalized.getRange(),
                            normalized.isHasVersion())) {
                        removePattern(req.getPattern());
                        lockfile.removePattern(req.getPattern());
                        fresh = true;
                    }
                } else {
                    fresh = true;
                }
                request.init();
            }
            return request.find(fresh, frozen);
        });
    }

    /**
     * {@code init}: the root requests one after another, each with everything it requests, then the requests that
     * matched a version already resolved, then the resolutions that waited for their target.
     */
    void init(List<DependencyRequestPattern> deps, boolean isFlat, boolean isFrozen,
              @Nullable WorkspaceLayout workspaceLayout) {
        this.flat = isFlat;
        this.frozen = isFrozen;
        this.workspaceLayout = workspaceLayout;
        for (DependencyRequestPattern req : deps) {
            rootMark = loop.issued();
            Promise<Void> found = find(req);
            loop.run();
            rootIndex++;
            if (found.getError() != null) {
                throw found.getError();
            }
        }

        resolvePackagesWithExistingVersions();

        for (DependencyRequestPattern req : new ArrayList<>(resolutionMap.delayQueue)) {
            resolveToResolution(req);
        }

        YarnClassicRelease release = config.getRelease();
        if (release.optimizesResolutions() && (isFlat || !release.optimizesOnlyFlatResolutions())) {
            for (DependencyRequestPattern dep : deps) {
                optimizeResolutions(NormalizedPattern.normalizePattern(dep.getPattern()).getName());
            }
        }
    }

    void optimizeResolutions(String name) {
        List<String> byName = patternsByPackage.get(name);
        List<String> patterns = dedupePatterns(byName == null ? Collections.emptyList() : byName);
        List<String> collapsablePatterns = new ArrayList<>();
        for (String pattern : patterns) {
            PackageRemote remote = this.patterns.get(pattern).remote;
            if (lockfile.getLocked(pattern) == null && (remote == null || !"workspace".equals(remote.type))) {
                collapsablePatterns.add(pattern);
            }
        }
        if (collapsablePatterns.size() < 2) {
            return;
        }
        List<@Nullable Object> availableVersions = new ArrayList<>();
        for (Manifest manifest : getAllInfoForPatterns(collapsablePatterns)) {
            availableVersions.add(manifest.getVersion());
        }
        List<String> ranges = new ArrayList<>();
        for (String pattern : collapsablePatterns) {
            ranges.add(NormalizedPattern.normalizePattern(pattern).getRange());
        }
        if (!config.getRelease().collapsesToAVersionEveryRangeTakes()) {
            // semver's Range of the joined text; one that does not parse takes no version
            String combinedRange = String.join(" ", ranges);
            Object singleVersion = NodeSemver.validRange(combinedRange, Config.SEMVER) == null ? null :
                    maxSatisfying(availableVersions, combinedRange);
            if (singleVersion != null) {
                collapsePackageVersions(name, Js.string(singleVersion), collapsablePatterns);
            }
            return;
        }
        try {
            availableVersions.sort((a, b) -> NodeSemver.compare(b, a, Config.SEMVER));
        } catch (IllegalArgumentException e) {
            throw new JsError(JsError.TYPE_ERROR, e.getMessage());
        }
        for (Object version : availableVersions) {
            boolean all = true;
            for (String range : ranges) {
                all &= NodeSemver.satisfies(version, range, Config.SEMVER);
            }
            if (all) {
                collapsePackageVersions(name, Js.string(version), collapsablePatterns);
                return;
            }
        }
    }

    void reportPackageWithExistingVersion(PackageRequest req, Manifest info) {
        delayedResolveQueue.add(new DelayedResolve(req, info));
    }

    void resolvePackagesWithExistingVersions() {
        for (DelayedResolve delayed : delayedResolveQueue) {
            delayed.getReq().resolveToExistingVersion(delayed.getInfo());
        }
    }

    @Nullable
    DependencyRequestPattern resolveToResolution(DependencyRequestPattern req) {
        List<String> parentNames = req.getParentNames();
        String pattern = req.getPattern();
        YarnClassicRelease release = config.getRelease();
        if (parentNames == null || flat && release.appliesNoResolutionUnderFlat()) {
            return req;
        }
        String resolution = resolutionMap.find(pattern, parentNames);
        if (Js.truthy(resolution)) {
            Manifest resolutionManifest = release.waitsForTheTargetOfAResolution() ?
                    getResolvedPattern(resolution) : getStrictResolvedPattern(resolution);
            if (resolutionManifest != null) {
                PackageReference reference = resolutionManifest.reference;
                if (reference == null) {
                    throw new JsError(Js.INVARIANT_VIOLATION, "resolutions should have a resolved reference");
                }
                reference.patterns.add(pattern);
                addPattern(pattern, resolutionManifest);
                if (release.removesTheLockEntryOfAPatternResolvedElsewhere()) {
                    Map<String, @Nullable Object> lockManifest = lockfile.getLocked(pattern);
                    if (ResolutionMap.shouldUpdateLockfile(lockManifest, reference)) {
                        lockfile.removePattern(pattern);
                    }
                } else if (!release.keepsTheLockEntryOfAResolvedPattern() &&
                           !(release.keepsTheLockEntryOfATopLevelPattern() &&
                             resolutionMap.topLevelPatterns.contains(pattern))) {
                    lockfile.removePattern(pattern);
                }
            } else {
                resolutionMap.addToDelayQueue(req);
            }
            return null;
        }
        return req;
    }

    // an entry the request joins that differs from its own is a race unless what it waited on made it come second
    void joined(PackageRequest request, Manifest info, Manifest resolved) {
        PackageReference ref = resolved.reference;
        if (ref == null || ref.requests.isEmpty()) {
            return;
        }
        PackageRequest creator = ref.requests.get(0);
        if (creator == request || creator.root != request.root || request.io.containsAll(creator.io) ||
            fingerprint(info.remote).equals(fingerprint(resolved.remote))) {
            return;
        }
        races.add(new Race(creator.pattern + " (" + describe(creator, resolved) + ") and " + request.pattern + " (" +
                           describe(request, info) + ")", resolved, info));
    }

    /**
     * The lock text the races leave open: each shared entry takes the remote of the pattern yarn resolves first.
     */
    void requireNoRaces() {
        if (races.isEmpty()) {
            return;
        }
        List<String> each = new ArrayList<>();
        for (Race race : races) {
            each.add(race.getDescription());
        }
        throw new CannotKnow("which of " + String.join("; ", each) + " yarn resolves first: the entry they share " +
                             "follows the order the registry answers in");
    }

    private static String describe(PackageRequest request, Manifest manifest) {
        String version = Js.templateString(manifest.getVersion());
        return (manifest.locked != null ? "locked at " : "resolved anew to ") + version +
               (request.parentRequest == null ? "" : ", under " + request.parentRequest.pattern);
    }

    private static String fingerprint(@Nullable PackageRemote remote) {
        if (remote == null) {
            return "";
        }
        String integrity;
        try {
            integrity = Js.truthy(remote.integrity) ? Lockfile.serializeIntegrity(remote.integrity) : "";
        } catch (JsError e) {
            integrity = Js.string(remote.integrity);
        }
        return Lockfile.keyForRemote(remote) + " " + integrity;
    }
}
