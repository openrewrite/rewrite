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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Port of bun's src/install/npm.zig {@code PackageManifest} and {@code PackageVersion} (bun 1.3.10), with
 * {@code findBestVersion} and the minimum release age filter, as the release that installs reads a packument
 * ({@link BunRelease}; npm.rs for 1.4.x).
 */
final class PackageManifest {

    /**
     * {@code PackageVersion}: what bun keeps of one version of the packument.
     */
    static final class PackageVersion {
        Integrity integrity = Integrity.UNKNOWN;

        /**
         * Names and specs alternating, in the packument's order; the optional peers moved to the front of the peers.
         */
        List<String> dependencies = Collections.emptyList();
        List<String> optionalDependencies = Collections.emptyList();
        List<String> peerDependencies = Collections.emptyList();
        int nonOptionalPeerDependenciesStart;

        /**
         * Name hashes of the bundled dependencies; null when every dependency is bundled.
         */
        @Nullable List<Long> bundledDependencies = Collections.emptyList();
        Bin bin = Bin.NONE;
        String tarballUrl = "";
        int os = Negatable.OPERATING_SYSTEM.allValue;
        int cpu = Negatable.ARCHITECTURE.allValue;
        int libc = Negatable.NONE;
        boolean hasInstallScript;
        double publishTimestampMs;

        boolean allDependenciesBundled() {
            return bundledDependencies == null;
        }
    }

    static final class FindResult {
        final SemverVersion version;
        final PackageVersion packageVersion;

        FindResult(SemverVersion version, PackageVersion packageVersion) {
            this.version = version;
            this.packageVersion = packageVersion;
        }
    }

    /**
     * {@code FindVersionResult}: a version found, one found while newer ones were too recent, or an error.
     */
    static final class FindVersionResult {
        enum Error {
            NOT_FOUND, TOO_RECENT, ALL_VERSIONS_TOO_RECENT
        }

        final @Nullable FindResult result;
        final @Nullable SemverVersion newestFiltered;
        final @Nullable Error error;
        final boolean withFilter;

        private FindVersionResult(@Nullable FindResult result, @Nullable SemverVersion newestFiltered,
                                  @Nullable Error error, boolean withFilter) {
            this.result = result;
            this.newestFiltered = newestFiltered;
            this.error = error;
            this.withFilter = withFilter;
        }

        static FindVersionResult found(FindResult result) {
            return new FindVersionResult(result, null, null, false);
        }

        static FindVersionResult foundWithFilter(FindResult result, @Nullable SemverVersion newestFiltered) {
            return new FindVersionResult(result, newestFiltered, null, true);
        }

        static FindVersionResult error(Error error) {
            return new FindVersionResult(null, null, error, false);
        }
    }

    private static final double SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000.0;

    final String name;
    /**
     * The release that parsed the packument, which orders its versions ({@link SemverVersion#orderPre}).
     */
    final BunRelease release;
    final List<SemverVersion> releaseVersions = new ArrayList<>();
    final List<PackageVersion> releasePackages = new ArrayList<>();
    final List<SemverVersion> prereleaseVersions = new ArrayList<>();
    final List<PackageVersion> prereleasePackages = new ArrayList<>();
    final List<String> distTags = new ArrayList<>();
    final List<SemverVersion> distTagVersions = new ArrayList<>();
    boolean hasExtendedManifest;

    private PackageManifest(String name, BunRelease release) {
        this.name = name;
        this.release = release;
    }

    @Nullable
    FindResult findByVersion(SemverVersion version) {
        List<SemverVersion> keys = version.hasPre() ? prereleaseVersions : releaseVersions;
        List<PackageVersion> values = version.hasPre() ? prereleasePackages : releasePackages;
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).eql(version)) {
                return new FindResult(keys.get(i), values.get(i));
            }
        }
        return null;
    }

    @Nullable
    FindResult findByDistTag(String tag) {
        for (int i = 0; i < distTags.size(); i++) {
            if (distTags.get(i).equals(tag)) {
                return findByVersion(distTagVersions.get(i));
            }
        }
        return null;
    }

    boolean shouldExcludeFromAgeFilter(@Nullable List<String> exclusions) {
        return exclusions != null && exclusions.contains(name);
    }

    static boolean isPackageVersionTooRecent(PackageVersion packageVersion, double minimumReleaseAgeMs, double nowMs) {
        return packageVersion.publishTimestampMs > nowMs - minimumReleaseAgeMs;
    }

    @Nullable
    FindResult findBestVersion(SemverQuery.Group group) {
        SemverRange.Comparator left = group.head.head.range.left;
        if (left.op == SemverRange.Op.EQL) {
            return findByVersion(left.version);
        }
        FindResult latest = findByDistTag("latest");
        if (latest != null && group.satisfies(latest.version)) {
            if ((group.flags & SemverQuery.Group.FLAG_PRE) != 0) {
                if (left.version.order(latest.version, release) == 0) {
                    return latest;
                }
            } else {
                return latest;
            }
        }
        for (int i = releaseVersions.size(); i > 0; i--) {
            SemverVersion version = releaseVersions.get(i - 1);
            if (group.satisfies(version)) {
                return new FindResult(version, releasePackages.get(i - 1));
            }
        }
        if ((group.flags & SemverQuery.Group.FLAG_PRE) != 0) {
            for (int i = prereleaseVersions.size(); i > 0; i--) {
                SemverVersion version = prereleaseVersions.get(i - 1);
                if (group.satisfies(version)) {
                    return new FindResult(version, prereleasePackages.get(i - 1));
                }
            }
        }
        return null;
    }

    /**
     * @param nowMs bun's start time, from which a version's age is measured.
     */
    FindVersionResult findBestVersionWithFilter(SemverQuery.Group group, @Nullable Double minimumReleaseAgeMs,
                                                @Nullable List<String> exclusions, double nowMs) {
        Double minAgeMs = minimumReleaseAgeMs != null && !shouldExcludeFromAgeFilter(exclusions) ?
                minimumReleaseAgeMs : null;
        if (minAgeMs == null) {
            FindResult result = findBestVersion(group);
            return result != null ? FindVersionResult.found(result) :
                    FindVersionResult.error(FindVersionResult.Error.NOT_FOUND);
        }
        SemverRange.Comparator left = group.head.head.range.left;
        SemverVersion[] newestFiltered = {null};
        if (left.op == SemverRange.Op.EQL) {
            FindResult result = findByVersion(left.version);
            if (result != null) {
                if (isPackageVersionTooRecent(result.packageVersion, minAgeMs, nowMs)) {
                    return FindVersionResult.error(FindVersionResult.Error.TOO_RECENT);
                }
                return FindVersionResult.found(result);
            }
            return FindVersionResult.error(FindVersionResult.Error.NOT_FOUND);
        }
        FindResult latest = findByDistTag("latest");
        if (latest != null && group.satisfies(latest.version)) {
            if (isPackageVersionTooRecent(latest.packageVersion, minAgeMs, nowMs)) {
                newestFiltered[0] = latest.version;
            }
            if (newestFiltered[0] == null) {
                if ((group.flags & SemverQuery.Group.FLAG_PRE) != 0) {
                    if (left.version.order(latest.version, release) == 0) {
                        return FindVersionResult.found(latest);
                    }
                } else {
                    return FindVersionResult.found(latest);
                }
            }
        }
        FindVersionResult result = searchVersionList(releaseVersions, releasePackages, group, minAgeMs, newestFiltered,
                nowMs);
        if (result != null) {
            return result;
        }
        if ((group.flags & SemverQuery.Group.FLAG_PRE) != 0) {
            result = searchVersionList(prereleaseVersions, prereleasePackages, group, minAgeMs, newestFiltered, nowMs);
            if (result != null) {
                return result;
            }
        }
        if (newestFiltered[0] != null) {
            return FindVersionResult.error(FindVersionResult.Error.ALL_VERSIONS_TOO_RECENT);
        }
        return FindVersionResult.error(FindVersionResult.Error.NOT_FOUND);
    }

    private static @Nullable FindVersionResult searchVersionList(List<SemverVersion> versions,
                                                                 List<PackageVersion> packages, SemverQuery.Group group,
                                                                 double minimumReleaseAgeMs,
                                                                 SemverVersion[] newestFiltered, double nowMs) {
        PackageVersion prevPackageBlockedFromAge = null;
        FindResult bestVersion = null;
        double stabilityWindowMs = Math.min(minimumReleaseAgeMs, SEVEN_DAYS_MS);
        for (int i = versions.size(); i > 0; ) {
            i--;
            SemverVersion version = versions.get(i);
            if (!group.satisfies(version)) {
                continue;
            }
            PackageVersion pkg = packages.get(i);
            if (isPackageVersionTooRecent(pkg, minimumReleaseAgeMs, nowMs)) {
                if (newestFiltered[0] == null) {
                    newestFiltered[0] = version;
                }
                prevPackageBlockedFromAge = pkg;
            } else if (prevPackageBlockedFromAge != null) {
                if (pkg.publishTimestampMs < nowMs - (minimumReleaseAgeMs + SEVEN_DAYS_MS)) {
                    if (bestVersion == null) {
                        bestVersion = new FindResult(version, pkg);
                    }
                    break;
                }
                boolean isStable = prevPackageBlockedFromAge.publishTimestampMs - pkg.publishTimestampMs >=
                                   stabilityWindowMs;
                if (isStable) {
                    bestVersion = new FindResult(version, pkg);
                    break;
                }
                if (bestVersion == null) {
                    bestVersion = new FindResult(version, pkg);
                }
                prevPackageBlockedFromAge = pkg;
            } else {
                return FindVersionResult.found(new FindResult(version, pkg));
            }
        }
        if (bestVersion != null) {
            return newestFiltered[0] != null ? FindVersionResult.foundWithFilter(bestVersion, newestFiltered[0]) :
                    FindVersionResult.found(bestVersion);
        }
        return null;
    }

    FindVersionResult findByDistTagWithFilter(String tag, @Nullable Double minimumReleaseAgeMs,
                                              @Nullable List<String> exclusions, double nowMs) {
        FindResult distResult = findByDistTag(tag);
        if (distResult == null) {
            return FindVersionResult.error(FindVersionResult.Error.NOT_FOUND);
        }
        Double minAgeMs = minimumReleaseAgeMs != null && !shouldExcludeFromAgeFilter(exclusions) ?
                minimumReleaseAgeMs : null;
        if (minAgeMs == null) {
            return FindVersionResult.found(distResult);
        }
        double stabilityWindowMs = Math.min(minAgeMs, SEVEN_DAYS_MS);
        if (!isPackageVersionTooRecent(distResult.packageVersion, minAgeMs, nowMs)) {
            return FindVersionResult.found(distResult);
        }
        SemverVersion latestVersion = distResult.version;
        boolean isPrerelease = latestVersion.hasPre();
        String latestVersionTagBeforeDot = null;
        if (isPrerelease) {
            int dot = latestVersion.pre.indexOf('.');
            latestVersionTagBeforeDot = dot >= 0 ? latestVersion.pre.substring(0, dot) : latestVersion.pre;
        }
        List<SemverVersion> versions = isPrerelease ? prereleaseVersions : releaseVersions;
        List<PackageVersion> packages = isPrerelease ? prereleasePackages : releasePackages;
        FindResult bestVersion = null;
        PackageVersion prevPackageBlockedFromAge = distResult.packageVersion;
        for (int i = versions.size(); i > 0; i--) {
            SemverVersion version = versions.get(i - 1);
            PackageVersion pkg = packages.get(i - 1);
            if (version.order(latestVersion, release) > 0) {
                continue;
            }
            if (latestVersionTagBeforeDot != null) {
                int dot = version.pre.indexOf('.');
                String actualTag = dot >= 0 ? version.pre.substring(0, dot) : version.pre;
                if (!actualTag.equals(latestVersionTagBeforeDot)) {
                    continue;
                }
            }
            if (isPackageVersionTooRecent(pkg, minAgeMs, nowMs)) {
                prevPackageBlockedFromAge = pkg;
                continue;
            }
            if (prevPackageBlockedFromAge != null) {
                if (pkg.publishTimestampMs < nowMs - (minAgeMs + SEVEN_DAYS_MS)) {
                    FindResult found = bestVersion != null ? bestVersion : new FindResult(version, pkg);
                    return FindVersionResult.foundWithFilter(found, distResult.version);
                }
                boolean isStable = prevPackageBlockedFromAge.publishTimestampMs - pkg.publishTimestampMs >=
                                   stabilityWindowMs;
                if (isStable) {
                    return FindVersionResult.foundWithFilter(new FindResult(version, pkg), distResult.version);
                }
                if (bestVersion == null) {
                    bestVersion = new FindResult(version, pkg);
                }
                prevPackageBlockedFromAge = pkg;
                continue;
            }
            bestVersion = new FindResult(version, pkg);
            break;
        }
        if (bestVersion != null) {
            return FindVersionResult.foundWithFilter(bestVersion, distResult.version);
        }
        return FindVersionResult.error(FindVersionResult.Error.ALL_VERSIONS_TOO_RECENT);
    }

    /**
     * {@code parse}: an abbreviated packument (or a full one, for the minimum release age), named after the
     * dependency it was fetched for; null when it does not parse or reports an error.
     */
    static @Nullable PackageManifest parse(Log log, String jsonBuffer, String expectedName,
                                           boolean isExtendedManifest) {
        return parse(BunRelease.PORTED, log, jsonBuffer, expectedName, isExtendedManifest);
    }

    /**
     * {@code parse} as {@code release} reads a packument.
     */
    static @Nullable PackageManifest parse(BunRelease release, Log log, String jsonBuffer, String expectedName,
                                           boolean isExtendedManifest) {
        Json.Expr json;
        try {
            json = Json.parse(jsonBuffer);
        } catch (Json.SyntaxError e) {
            log.addError(e.getMessage());
            return null;
        }
        Json.Expr errorExpr = json.get("error");
        if (errorExpr != null && errorExpr.asString() != null) {
            log.addError("npm error: " + errorExpr.asString());
            return null;
        }
        Json.Expr modified = json.get("modified");
        if (modified != null && modified.asString() == null) {
            return null;
        }

        PackageManifest result = new PackageManifest(expectedName, release);
        ManifestBuffer buffer = new ManifestBuffer(release, expectedName);
        buffer.count(expectedName);
        buffer.append(expectedName);
        Json.Expr versionsExpr = json.get("versions");
        if (versionsExpr != null && versionsExpr.isObject()) {
            Json.Expr timeObj = json.get("time");
            for (Json.Property prop : versionsExpr.properties()) {
                String versionName = prop.key.asString();
                SemverVersion.ParseResult parsedVersion = SemverVersion.parse(versionName, release);
                if (!parsedVersion.valid) {
                    if (!release.skipsAVersionKeyThatIsNoVersionSilently()) {
                        log.addError("Failed to parse dependency " + versionName);
                    }
                    continue;
                }
                buffer.count(versionName);
                if (parsedVersion.version.min().hasPre() || parsedVersion.version.min().hasBuild()) {
                    buffer.append(versionName);
                }
                Json.Expr value = prop.value;
                Set<String> bundledDepsSet = new LinkedHashSet<>();
                boolean bundleAllDeps = false;
                Json.Expr bundledDepsExpr = value.get("bundleDependencies");
                if (bundledDepsExpr == null) {
                    bundledDepsExpr = value.get("bundledDependencies");
                }
                if (bundledDepsExpr != null) {
                    if (bundledDepsExpr.kind == Json.Kind.BOOLEAN) {
                        bundleAllDeps = Boolean.TRUE.equals(bundledDepsExpr.asBool());
                    } else if (bundledDepsExpr.isArray()) {
                        for (Json.Expr bundledDep : bundledDepsExpr.items()) {
                            if (bundledDep.asString() != null) {
                                bundledDepsSet.add(bundledDep.asString());
                            }
                        }
                    }
                }

                PackageVersion packageVersion = new PackageVersion();
                Json.Expr cpu = value.get("cpu");
                if (cpu != null) {
                    packageVersion.cpu = Negatable.fromJson(Negatable.ARCHITECTURE, cpu);
                }
                Json.Expr os = value.get("os");
                if (os != null) {
                    packageVersion.os = Negatable.fromJson(Negatable.OPERATING_SYSTEM, os);
                }
                Json.Expr libc = value.get("libc");
                if (libc != null) {
                    packageVersion.libc = Negatable.fromJson(Negatable.LIBC, libc);
                }
                Json.Expr hasInstallScript = value.get("hasInstallScript");
                if (hasInstallScript != null && hasInstallScript.kind == Json.Kind.BOOLEAN) {
                    packageVersion.hasInstallScript = Boolean.TRUE.equals(hasInstallScript.asBool());
                }
                packageVersion.bin = parseBin(value);
                buffer.appendAll(packageVersion.bin.value);
                buffer.leaveUncountedTheDirectoryOfAnEmptyBin(value, versionName);

                Json.Expr dist = value.get("dist");
                if (dist != null && dist.isObject()) {
                    Json.Expr tarball = dist.get("tarball");
                    if (tarball != null && tarball.isString()) {
                        buffer.count(tarball.asString());
                    }
                    if (tarball != null && tarball.isString() && !tarball.asString().isEmpty()) {
                        packageVersion.tarballUrl = tarball.asString();
                        buffer.append(packageVersion.tarballUrl);
                    }
                    boolean done = false;
                    Json.Expr integrity = dist.get("integrity");
                    if (integrity != null && integrity.asString() != null) {
                        packageVersion.integrity = Integrity.parse(integrity.asString(), release);
                        done = packageVersion.integrity.tag.isSupported();
                    }
                    if (!done) {
                        Json.Expr shasum = dist.get("shasum");
                        if (shasum != null && shasum.asString() != null) {
                            try {
                                packageVersion.integrity = Integrity.parseSHASum(shasum.asString(), release);
                            } catch (IllegalArgumentException e) {
                                packageVersion.integrity = Integrity.UNKNOWN;
                            }
                        }
                    }
                }

                boolean impliesMetaPeers = release.impliesOptionalPeersOfPeerDependenciesMeta();
                String[] groups = {"dependencies", "optionalDependencies", "peerDependencies"};
                for (String group : groups) {
                    Json.Expr versionedDeps = value.get(group);
                    boolean isPeer = "peerDependencies".equals(group);
                    List<Json.Property> items = versionedDeps != null && versionedDeps.isObject() ?
                            versionedDeps.properties() : Collections.<Json.Property>emptyList();
                    if (impliesMetaPeers) {
                        // a group runs with its items, the peers also for the names peerDependenciesMeta lists alone
                        Json.Expr meta = isPeer ? value.get("peerDependenciesMeta") : null;
                        boolean hasMetaOnlyPeers = meta != null && meta.isObject() && !meta.properties().isEmpty();
                        if (items.isEmpty() && !hasMetaOnlyPeers) {
                            continue;
                        }
                    } else if (versionedDeps == null || !versionedDeps.isObject()) {
                        continue;
                    }
                    // bun keeps the bundled names found in the last group it reads (from 1.3.14 the last but the peers)
                    List<Long> bundled = new ArrayList<>();
                    List<Long> optionalPeerDepNames = new ArrayList<>();
                    if (isPeer) {
                        Json.Expr meta = value.get("peerDependenciesMeta");
                        if (meta != null && meta.isObject()) {
                            for (Json.Property metaProp : meta.properties()) {
                                Json.Expr optional = metaProp.value.get("optional");
                                if (optional != null && Boolean.TRUE.equals(optional.asBool())) {
                                    optionalPeerDepNames.add(Wyhash11.stringHash(metaProp.key.asString()));
                                }
                            }
                        }
                    }
                    List<String> names = new ArrayList<>();
                    List<String> versions = new ArrayList<>();
                    int nonOptionalPeerDependencyOffset = 0;
                    for (Json.Property item : items) {
                        String nameStr = item.key.asString();
                        String versionStr = item.value.asString();
                        buffer.count(nameStr);
                        buffer.count(versionStr);
                        if (nameStr == null || versionStr == null) {
                            continue;
                        }
                        names.add(nameStr);
                        versions.add(versionStr);
                        int i = names.size() - 1;
                        if (!bundleAllDeps && bundledDepsSet.remove(nameStr)) {
                            bundled.add(Wyhash11.stringHash(nameStr));
                        }
                        if (isPeer && optionalPeerDepNames.contains(Wyhash11.stringHash(nameStr))) {
                            if (nonOptionalPeerDependencyOffset != i) {
                                String currentName = names.get(i);
                                names.set(i, names.get(nonOptionalPeerDependencyOffset));
                                names.set(nonOptionalPeerDependencyOffset, currentName);
                                String currentVersion = versions.get(i);
                                versions.set(i, versions.get(nonOptionalPeerDependencyOffset));
                                versions.set(nonOptionalPeerDependencyOffset, currentVersion);
                            }
                            nonOptionalPeerDependencyOffset++;
                        }
                    }
                    if (isPeer && impliesMetaPeers) {
                        // a name peerDependenciesMeta marks optional and the peers do not list is an optional peer
                        // of "*", moved to the optional peers at the front
                        Json.Expr meta = value.get("peerDependenciesMeta");
                        if (meta != null && meta.isObject()) {
                            outer:
                            for (Json.Property metaProp : meta.properties()) {
                                Json.Expr optional = metaProp.value.get("optional");
                                if (optional == null || !Boolean.TRUE.equals(optional.asBool())) {
                                    continue;
                                }
                                String metaKey = metaProp.key.asString();
                                long metaHash = Wyhash11.stringHash(metaKey);
                                for (String existing : names) {
                                    if (Wyhash11.stringHash(existing) == metaHash) {
                                        continue outer;
                                    }
                                }
                                names.add(metaKey);
                                versions.add("*");
                                int i = names.size() - 1;
                                if (nonOptionalPeerDependencyOffset != i) {
                                    Collections.swap(names, i, nonOptionalPeerDependencyOffset);
                                    Collections.swap(versions, i, nonOptionalPeerDependencyOffset);
                                }
                                nonOptionalPeerDependencyOffset++;
                            }
                        }
                    }
                    List<String> pairs = new ArrayList<>(names.size() * 2);
                    for (int i = 0; i < names.size(); i++) {
                        pairs.add(names.get(i));
                        pairs.add(versions.get(i));
                    }
                    buffer.appendAll(pairs);
                    if ("dependencies".equals(group)) {
                        packageVersion.dependencies = pairs;
                    } else if ("optionalDependencies".equals(group)) {
                        packageVersion.optionalDependencies = pairs;
                    } else {
                        packageVersion.peerDependencies = pairs;
                        packageVersion.nonOptionalPeerDependenciesStart = nonOptionalPeerDependencyOffset;
                    }
                    if (!(isPeer && impliesMetaPeers)) {
                        packageVersion.bundledDependencies = bundleAllDeps ? null : new ArrayList<>(bundled);
                    }
                }

                if (timeObj != null) {
                    Json.Expr publishTime = timeObj.get(versionName);
                    if (publishTime != null && publishTime.asString() != null) {
                        Double time = parseES5Date(publishTime.asString());
                        if (time != null) {
                            packageVersion.publishTimestampMs = time;
                        }
                    }
                }

                if (!parsedVersion.version.min().hasPre()) {
                    result.releaseVersions.add(parsedVersion.version.min());
                    result.releasePackages.add(packageVersion);
                } else {
                    result.prereleaseVersions.add(parsedVersion.version.min());
                    result.prereleasePackages.add(packageVersion);
                }
            }
        }

        Json.Expr distTagsExpr = json.get("dist-tags");
        if (distTagsExpr != null && distTagsExpr.isObject()) {
            for (Json.Property tag : distTagsExpr.properties()) {
                String key = tag.key.asString();
                String versionName = tag.value.asString();
                if (versionName == null) {
                    continue;
                }
                buffer.append(key);
                buffer.append(versionName);
                result.distTags.add(key);
                result.distTagVersions.add(SemverVersion.parse(versionName, release).version.min());
            }
        }
        buffer.requireItHoldsWhatItLeftUncounted();

        sortByVersion(result.releaseVersions, result.releasePackages, release);
        sortByVersion(result.prereleaseVersions, result.prereleasePackages, release);
        result.hasExtendedManifest = isExtendedManifest;
        return result;
    }

    /**
     * The string buffer 1.4's manifest parser fills ({@code semver::Builder}), as far as a release's port of it tells
     * apart: its counting pass sums the strings it cannot inline (the registry's ETag and Last-Modified headers too,
     * which the engine is not given), and the build pass appends into twice that and more.
     */
    private static final class ManifestBuffer {
        // the slack the buffer has besides twice what its counting pass counts: (cap + cap % 64 + 64) * 2
        private static final long SLACK = 128;

        private final BunRelease release;
        private final String name;

        /**
         * At most what the counting pass counts: the bytes of the expected name, the versions, their tarballs and
         * their dependencies' names and specs that it does not inline.
         */
        private long counted;

        /**
         * The bytes of each {@code directories.bin} the counting pass leaves out beside an empty {@code bin}.
         */
        private long uncounted;

        private @Nullable String firstUncounted;

        ManifestBuffer(BunRelease release, String name) {
            this.release = release;
            this.name = name;
        }

        void count(@Nullable String s) {
            if (s != null) {
                int length = s.getBytes(StandardCharsets.UTF_8).length;
                if (length > 8) {
                    counted += length;
                }
            }
        }

        /**
         * A string the build pass appends, which 1.4.0 misplaces where it is 8 bytes whose last is not ASCII
         * ({@link BunRelease#misplacesAnEightByteNonAsciiManifestString()}).
         */
        void append(String s) {
            if (release.misplacesAnEightByteNonAsciiManifestString() && endsAnEightByteStringOutOfLine(s)) {
                throw new CannotKnow("what bun " + release + " reads back of the string \"" + s + "\" in " + name +
                                     "'s packument: its manifest string builder takes these 8 bytes, the last not " +
                                     "ASCII, for a string it inlines, and keeps the offset of where they stood in " +
                                     "the packument, which reads whatever its buffer holds there");
            }
        }

        void appendAll(List<String> strings) {
            for (String s : strings) {
                append(s);
            }
        }

        /**
         * A version whose {@code bin} is {@code ""} has its {@code directories.bin} appended but not counted where the
         * release leaves it out ({@link BunRelease#leavesAnEmptyBinsDirectoryUncounted()}).
         */
        void leaveUncountedTheDirectoryOfAnEmptyBin(Json.Expr version, String versionName) {
            if (!release.leavesAnEmptyBinsDirectoryUncounted()) {
                return;
            }
            Json.Expr bin = version.get("bin");
            if (bin == null || !bin.isString() || !bin.asString().isEmpty()) {
                return;
            }
            Json.Expr directories = version.get("directories");
            Json.Expr directoriesBin = directories == null || !directories.isObject() ? null : directories.get("bin");
            String directory = directoriesBin == null ? null : directoriesBin.asString();
            if (directory == null) {
                return;
            }
            int length = directory.getBytes(StandardCharsets.UTF_8).length;
            if (length > 8 || endsAnEightByteStringOutOfLine(directory)) {
                uncounted += length;
                if (firstUncounted == null) {
                    firstUncounted = versionName;
                }
            }
        }

        /**
         * The build pass panics ({@code range end index out of range}) where what the counting pass left out does not
         * fit the buffer's slack, which ends past the counted bytes and {@link #SLACK}: within that it cannot panic,
         * beyond it whether it does turns on the response's headers as well.
         */
        void requireItHoldsWhatItLeftUncounted() {
            if (uncounted > counted + SLACK) {
                throw new CannotKnow("whether bun " + release + " holds the directories.bin of " + name + "@" +
                                     firstUncounted + " in its manifest buffer: beside an empty bin its counting " +
                                     "pass leaves the path out, and where the buffer cannot hold it bun panics; the " +
                                     "registry's ETag and Last-Modified headers size the buffer too");
            }
        }

        // String::can_inline does not hold 8 bytes whose last has its high bit set
        private static boolean endsAnEightByteStringOutOfLine(String s) {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            return bytes.length == 8 && (bytes[7] & 0x80) != 0;
        }
    }

    private static void sortByVersion(List<SemverVersion> versions, List<PackageVersion> packages,
                                      BunRelease release) {
        List<Integer> indices = new ArrayList<>(versions.size());
        for (int i = 0; i < versions.size(); i++) {
            indices.add(i);
        }
        indices.sort((l, r) -> versions.get(l).order(versions.get(r), release));
        List<SemverVersion> sortedVersions = new ArrayList<>(versions.size());
        List<PackageVersion> sortedPackages = new ArrayList<>(packages.size());
        for (int i : indices) {
            sortedVersions.add(versions.get(i));
            sortedPackages.add(packages.get(i));
        }
        versions.clear();
        versions.addAll(sortedVersions);
        packages.clear();
        packages.addAll(sortedPackages);
    }

    private static Bin parseBin(Json.Expr value) {
        Json.Expr bin = value.get("bin");
        if (bin != null) {
            if (bin.isObject()) {
                List<Json.Property> props = bin.properties();
                if (props.isEmpty()) {
                    return Bin.NONE;
                }
                List<String> values = new ArrayList<>();
                for (Json.Property prop : props) {
                    String key = prop.key.asString();
                    String v = prop.value.asString();
                    if (key == null || v == null) {
                        return Bin.NONE;
                    }
                    values.add(key);
                    values.add(v);
                }
                return new Bin(props.size() == 1 ? Bin.Tag.NAMED_FILE : Bin.Tag.MAP, values);
            }
            String str = bin.asString();
            if (str != null && !str.isEmpty()) {
                return new Bin(Bin.Tag.FILE, Collections.singletonList(str));
            }
        }
        return dirBin(value);
    }

    private static Bin dirBin(Json.Expr value) {
        Json.Expr dirs = value.get("directories");
        if (dirs != null) {
            Json.Expr binProp = dirs.get("bin");
            String str = binProp == null ? null : binProp.asString();
            if (str != null && !str.isEmpty()) {
                return new Bin(Bin.Tag.DIR, Collections.singletonList(str));
            }
        }
        return Bin.NONE;
    }

    // WTF::parseES5Date: the ISO 8601 date-time strings registries write
    private static @Nullable Double parseES5Date(String s) {
        try {
            return (double) OffsetDateTime.parse(s).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
