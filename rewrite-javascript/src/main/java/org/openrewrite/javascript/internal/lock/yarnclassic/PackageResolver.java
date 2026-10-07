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
import org.openrewrite.semver.NodeSemver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of yarn src/package-resolver.js (yarn 1.22.22): the resolver's state, a map from every pattern of the run to
 * its manifest and from each package name to its patterns, and the lookups a request makes in it.
 */
final class PackageResolver {

    final YarnClassicOptions config;
    final Lockfile lockfile;
    final ResolutionMap resolutionMap;

    final Map<String, List<String>> patternsByPackage = new LinkedHashMap<>();
    final Set<String> fetchingPatterns = new LinkedHashSet<>();
    final Map<String, Manifest> patterns = new LinkedHashMap<>();
    final Set<String> usedRegistries = new LinkedHashSet<>();
    final List<DelayedResolve> delayedResolveQueue = new ArrayList<>();
    boolean flat;
    boolean frozen;
    @Nullable
    WorkspaceLayout workspaceLayout;

    @Value
    static class DelayedResolve {
        PackageRequest req;
        Manifest info;
    }

    PackageResolver(YarnClassicOptions config, Lockfile lockfile) {
        this(config, lockfile, new ResolutionMap(config));
    }

    PackageResolver(YarnClassicOptions config, Lockfile lockfile, ResolutionMap resolutionMap) {
        this.config = config;
        this.lockfile = lockfile;
        this.resolutionMap = resolutionMap;
    }

    boolean isNewPattern(String pattern) {
        return patterns.get(pattern).fresh;
    }

    void updateManifest(PackageReference ref, Manifest newPkg) {
        Manifest oldPkg = patterns.get(ref.patterns.get(0));
        newPkg.reference = ref;
        newPkg.remote = ref.remote;
        newPkg.put("name", oldPkg.getName());
        newPkg.fresh = oldPkg.fresh;
        newPkg.put("prebuiltVariants", oldPkg.get("prebuiltVariants"));
        for (String pattern : ref.patterns) {
            patterns.put(pattern, newPkg);
        }
    }

    void updateManifests(List<Manifest> newPkgs) {
        for (Manifest newPkg : newPkgs) {
            if (newPkg.reference != null) {
                for (String pattern : newPkg.reference.patterns) {
                    Manifest oldPkg = patterns.get(pattern);
                    newPkg.put("prebuiltVariants", oldPkg.get("prebuiltVariants"));
                    patterns.put(pattern, newPkg);
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
            throw new JsError(JsError.INVARIANT_VIOLATION, "expected manifest");
        }
        return manifest;
    }

    @Nullable
    Manifest getExactVersionMatch(String name, String version, @Nullable Manifest manifest) {
        List<String> patterns = patternsByPackage.get(name);
        if (patterns == null) {
            return null;
        }
        for (String pattern : patterns) {
            Manifest info = getStrictResolvedPattern(pattern);
            if (version.equals(info.getVersion())) {
                return info;
            }
        }
        if (manifest != null && ExoticResolvers.getExoticResolver(version) != null) {
            List<Manifest> resolved = new ArrayList<>();
            for (String pattern : patterns) {
                resolved.add(getStrictResolvedPattern(pattern));
            }
            return exoticRangeMatch(resolved, manifest);
        }
        return null;
    }

    /**
     * The manifest of the highest version already resolved for the name that satisfies the range.
     */
    @Nullable
    Manifest getHighestRangeVersionMatch(String name, String range, @Nullable Manifest manifest) {
        List<String> patterns = patternsByPackage.get(name);
        if (patterns == null) {
            return null;
        }
        List<String> versionNumbers = new ArrayList<>();
        List<Manifest> resolvedPatterns = new ArrayList<>();
        for (String pattern : patterns) {
            Manifest info = getStrictResolvedPattern(pattern);
            versionNumbers.add(Js.string(info.getVersion()));
            resolvedPatterns.add(info);
        }
        String maxValidRange = NodeSemver.maxSatisfying(versionNumbers, range, YarnClassicOptions.SEMVER);
        if (maxValidRange == null) {
            return manifest != null && ExoticResolvers.getExoticResolver(range) != null ?
                    exoticRangeMatch(resolvedPatterns, manifest) : null;
        }
        return resolvedPatterns.get(versionNumbers.indexOf(maxValidRange));
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
        return NodeSemver.validRange(range, YarnClassicOptions.SEMVER) != null &&
               version instanceof String && NodeSemver.valid((String) version, YarnClassicOptions.SEMVER) != null &&
               ExoticResolvers.getExoticResolver(range) == null &&
               hasVersion &&
               !NodeSemver.satisfies((String) version, range, YarnClassicOptions.SEMVER);
    }

    void reportPackageWithExistingVersion(PackageRequest req, Manifest info) {
        delayedResolveQueue.add(new DelayedResolve(req, info));
    }
}
