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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.javascript.internal.lock.Ssri;
import org.openrewrite.javascript.internal.lock.V8Date;
import org.openrewrite.javascript.internal.lock.WhatwgUrl;
import org.openrewrite.javascript.semver.NodeSemver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of @pnpm/resolving.npm-resolver src/index.ts {@code resolveNpm} (pnpm 11.2.2) with whichVersionIsPinned,
 * workspacePrefToNpm, @pnpm/workspace.range-resolver and semver-utils 1.1.4's {@code parseRange}.
 */
final class NpmResolver {

    static final String MINIMUM_RELEASE_AGE_VIOLATION_CODE = "MINIMUM_RELEASE_AGE_VIOLATION";

    private static final Pattern SEMVER_RANGE = Pattern.compile(
            "\\s*((\\|\\||-)|(((?:(?:~?[<>]?)|\\^?)=?)\\s*(v)?([0-9]+)(\\.(x|\\*|[0-9]+))?(\\.(x|\\*|[0-9]+))?" +
            "(([-+])([a-zA-Z0-9.-]+))?))\\s*");

    private final RegistryMetadata registry;
    private final Map<String, String> registries;
    private final Map<String, String> namedRegistries;
    private final @Nullable Object saveWorkspaceProtocol;
    private final boolean ignoreMissingTimeField;
    private final boolean fullMetadata;
    private final Instant now;
    private final Map<String, Map<String, @Nullable Object>> metaCache = new LinkedHashMap<>();

    /**
     * The release whose resolver this is.
     */
    private final PnpmRelease release;

    /**
     * {@code strictPublishedByCheck}, minimumReleaseAge under minimumReleaseAgeStrict, which the picker reads before
     * 11.1.3 ({@link PnpmRelease#checksTheReleaseAgeAsAPolicy}).
     */
    private final boolean strictPublishedByCheck;

    /**
     * @param namedRegistries the aliases {@link #mergeNamedRegistries} answers
     * @param now             the clock {@code trustPolicyIgnoreAfter} counts from
     */
    NpmResolver(RegistryMetadata registry, Map<String, String> registries, Map<String, String> namedRegistries,
                @Nullable Object saveWorkspaceProtocol, boolean ignoreMissingTimeField, boolean fullMetadata,
                Instant now) {
        this(registry, registries, namedRegistries, saveWorkspaceProtocol, ignoreMissingTimeField, fullMetadata, now,
                PnpmRelease.PORTED, false);
    }

    NpmResolver(RegistryMetadata registry, Map<String, String> registries, Map<String, String> namedRegistries,
                @Nullable Object saveWorkspaceProtocol, boolean ignoreMissingTimeField, boolean fullMetadata,
                Instant now, PnpmRelease release, boolean strictPublishedByCheck) {
        this.registry = registry;
        this.registries = registries;
        this.namedRegistries = namedRegistries;
        this.saveWorkspaceProtocol = saveWorkspaceProtocol;
        this.ignoreMissingTimeField = ignoreMissingTimeField;
        this.fullMetadata = fullMetadata;
        this.now = now;
        this.release = release;
        this.strictPublishedByCheck = strictPublishedByCheck;
    }

    PnpmRelease getRelease() {
        return release;
    }

    /**
     * {@code mergeNamedRegistries}: the built-in {@code gh} alias under the configured ones, each an http(s) URL.
     */
    static Map<String, String> mergeNamedRegistries(@Nullable Map<String, @Nullable Object> userDefined) {
        Map<String, String> merged = new LinkedHashMap<>(PickPackageFromMeta.BUILTIN_NAMED_REGISTRIES);
        if (userDefined == null) {
            return merged;
        }
        for (String alias : Js.keys(userDefined)) {
            Object url = userDefined.get(alias);
            if (!(url instanceof String) || !isValidHttpUrl((String) url)) {
                throw new PnpmError("INVALID_NAMED_REGISTRY_URL", "The named registry alias '" + alias +
                        "' is mapped to '" + Js.string(url) + "', which is not a valid http(s) URL.",
                        "Provide a URL that starts with http:// or https://, e.g. https://npm.pkg.example.com/");
            }
            merged.put(alias, (String) url);
        }
        return merged;
    }

    private static boolean isValidHttpUrl(String url) {
        WhatwgUrl parsed = WhatwgUrl.tryParse(url);
        return parsed != null && ("http:".equals(parsed.getProtocol()) || "https:".equals(parsed.getProtocol()));
    }

    /**
     * {@code ResolveResult}: the id, resolution and manifest one wanted dependency resolved to.
     */
    static final class ResolveResult {
        String id = "";
        @Nullable Map<String, @Nullable Object> manifest;
        Map<String, @Nullable Object> resolution = new LinkedHashMap<>();
        String resolvedVia = "";
        @Nullable String latest;
        @Nullable String publishedAt;
        @Nullable String normalizedBareSpecifier;
        @Nullable String alias;
        ResolveDependencies.@Nullable PolicyViolation policyViolation;
    }

    @Nullable
    ResolveResult resolveNpm(ResolveDependencies.WantedDependency wantedDependency,
                             PackageRequester.RequestPackageOptions opts,
                             PreferredVersions.@Nullable ByName preferredVersions) {
        String defaultTag = opts.defaultTag == null ? "latest" : opts.defaultTag;
        String alias = wantedDependency.getAlias();
        String bareSpecifier = wantedDependency.getBareSpecifier();
        String registryUrl = alias != null && !alias.isEmpty() ?
                PickRegistryForPackage.pickRegistryForPackage(registries, alias, bareSpecifier, release) :
                registries.get("default");
        if (bareSpecifier != null && bareSpecifier.startsWith("workspace:")) {
            if (bareSpecifier.startsWith("workspace:.")) {
                return null;
            }
            ResolveResult resolvedFromWorkspace = tryResolveFromWorkspace(wantedDependency, defaultTag, registryUrl,
                    opts);
            if (resolvedFromWorkspace != null) {
                return resolvedFromWorkspace;
            }
        }
        Map<String, Map<String, Verification.Project>> workspacePackages = opts.alwaysTryWorkspacePackages ?
                opts.workspacePackages : null;
        PickPackageFromMeta.RegistryPackageSpec spec = bareSpecifier != null && !bareSpecifier.isEmpty() ?
                PickPackageFromMeta.parseBareSpecifier(bareSpecifier, alias, defaultTag, registryUrl, release) :
                new PickPackageFromMeta.RegistryPackageSpec("tag", String.valueOf(alias), defaultTag, null);
        if (spec == null) {
            return null;
        }
        PickPackage.PickerOptions pickerOpts = PickPackage.PickerOptions.builder()
                .preferredVersionSelectors(preferredVersions == null ? null : preferredVersions.get(spec.getName()))
                .publishedBy(opts.publishedBy)
                .publishedByExclude(opts.publishedByExclude)
                .pickLowestVersion(opts.pickLowestVersion)
                .includeLatestTag("latest".equals(opts.update))
                .ignoreMissingTimeField(ignoreMissingTimeField)
                .strictPublishedByCheck(strictPublishedByCheck)
                .release(release)
                .build();
        PickPackage.PickResult pickResult;
        try {
            pickResult = PickPackage.pickPackage(metaCache, registry, spec, pickerOpts, registryUrl,
                    wantedDependency.isOptional(), fullMetadata);
        } catch (PnpmError e) {
            if (workspacePackages != null && opts.projectDir != null) {
                try {
                    return tryResolveFromWorkspacePackages(workspacePackages, spec, wantedDependency, opts, false);
                } catch (PnpmError workspaceErr) {
                    // the registry's error is the one pnpm reports, but from 11.10.0 after a 404 that no version of
                    // the workspace matches (PnpmRelease#reportsTheWorkspaceVersionsTheRegistryLacks)
                    if ("ERR_PNPM_FETCH_404".equals(e.getCode()) &&
                        "ERR_PNPM_NO_MATCHING_VERSION_INSIDE_WORKSPACE".equals(workspaceErr.getCode()) &&
                        release.reportsTheWorkspaceVersionsTheRegistryLacks()) {
                        throw workspaceErr;
                    }
                }
            }
            throw e;
        }
        Map<String, @Nullable Object> pickedPackage = pickResult.getPickedPackage();
        Map<String, @Nullable Object> meta = pickResult.getMeta();
        if (pickedPackage == null) {
            if (workspacePackages != null && opts.projectDir != null) {
                try {
                    return tryResolveFromWorkspacePackages(workspacePackages, spec, wantedDependency, opts, false);
                } catch (PnpmError workspaceErr) {
                    // pnpm reports that no version matches, from 11.10.0 that none of the workspace does
                    // (PnpmRelease#reportsTheWorkspaceVersionsTheRegistryLacks)
                    if ("ERR_PNPM_NO_MATCHING_VERSION_INSIDE_WORKSPACE".equals(workspaceErr.getCode()) &&
                        release.reportsTheWorkspaceVersionsTheRegistryLacks()) {
                        throw workspaceErr;
                    }
                }
            }
            String dep = alias != null && !alias.isEmpty() ?
                    alias + "@" + (bareSpecifier == null ? "" : bareSpecifier) : String.valueOf(bareSpecifier);
            if (opts.publishedBy != null && !release.checksTheReleaseAgeAsAPolicy()) {
                PnpmError immature = noMatureMatchingVersion(wantedDependency, dep, spec, meta, registryUrl,
                        preferredVersions);
                if (immature != null) {
                    throw immature;
                }
            }
            throw new PnpmError("NO_MATCHING_VERSION", "No matching version found for " + dep +
                                                       " while fetching it from " + registryUrl);
        }
        if ("no-downgrade".equals(opts.trustPolicy)) {
            TrustChecks.failIfTrustDowngraded(meta, Js.string(pickedPackage.get("version")), opts.trustPolicyExclude,
                    opts.trustPolicyIgnoreAfter, now, release);
        }
        String pickedName = Js.string(pickedPackage.get("name"));
        String pickedVersion = Js.string(pickedPackage.get("version"));
        Map<String, Verification.Project> workspacePkgsMatchingName = workspacePackages == null ? null :
                workspacePackages.get(pickedName);
        if (workspacePkgsMatchingName != null && opts.projectDir != null) {
            Verification.Project matchedPkg = workspacePkgsMatchingName.get(pickedVersion);
            if (matchedPkg != null) {
                ResolveResult local = resolveFromLocalPackage(matchedPkg, spec, wantedDependency, opts);
                local.latest = latestOf(meta);
                return local;
            }
            String localVersion = pickMatchingLocalVersionOrNull(workspacePkgsMatchingName, spec);
            if (localVersion != null &&
                (NodeSemver.compare(localVersion, pickedVersion, NodeSemver.Options.DEFAULT) > 0 ||
                 opts.preferWorkspacePackages)) {
                ResolveResult local = resolveFromLocalPackage(workspacePkgsMatchingName.get(localVersion), spec,
                        wantedDependency, opts);
                local.latest = latestOf(meta);
                return local;
            }
        }
        ResolveResult result = new ResolveResult();
        result.id = pickedName + "@" + pickedVersion;
        Map<String, @Nullable Object> dist = ResolveDependencies.asMap(pickedPackage.get("dist"));
        String integrity = getIntegrity(dist);
        if (integrity != null) {
            result.resolution.put("integrity", integrity);
        }
        result.resolution.put("tarball", normalizeRegistryUrl(Js.string(dist.get("tarball"))));
        if (opts.calcSpecifier) {
            result.normalizedBareSpecifier = spec.getNormalizedBareSpecifier() != null ?
                    spec.getNormalizedBareSpecifier() :
                    calcSpecifier(wantedDependency, spec, pickedVersion, opts.pinnedVersion);
        }
        result.manifest = pickedPackage;
        result.latest = latestOf(meta);
        result.resolvedVia = "npm-registry";
        Object time = meta.get("time");
        Object publishedAt = time instanceof Map ? ((Map<?, ?>) time).get(pickedVersion) : null;
        result.publishedAt = publishedAt == null ? null : Js.string(publishedAt);
        result.policyViolation = policyViolation(detectMinReleaseAgeViolation(pickedName, pickedVersion,
                result.publishedAt, opts.publishedBy, opts.publishedByExclude));
        return result;
    }

    // NoMatchingVersionError before 11.1.3 under a release-age cutoff, where the range picks a version without it: its
    // code is NO_MATURE_MATCHING_VERSION, and it names that version where the packument has times; null where the
    // range picks none, and the error is NO_MATCHING_VERSION as from 11.1.3
    private @Nullable PnpmError noMatureMatchingVersion(ResolveDependencies.WantedDependency wantedDependency,
                                                        String dep, PickPackageFromMeta.RegistryPackageSpec spec,
                                                        Map<String, @Nullable Object> meta, String registryUrl,
                                                        PreferredVersions.@Nullable ByName preferred) {
        String immatureVersion = PickPackageFromMeta.pickVersionByVersionRange(
                new PickPackageFromMeta.VersionRangeOptions(meta, spec.getFetchSpec(),
                        preferred == null ? null : preferred.get(spec.getName()), release));
        if (immatureVersion == null) {
            return null;
        }
        Object time = meta.get("time");
        if (Js.truthy(time)) {
            Object releasedAt = time instanceof Map ? ((Map<?, ?>) time).get(immatureVersion) : null;
            double released = V8Date.dateParse(Js.string(releasedAt == null ? Js.UNDEFINED : releasedAt));
            String releaseAgeText = formatTimeAgo(released);
            if (releaseAgeText == null) {
                releaseAgeText = "just now";
            }
            // before 11.1.0 (pnpm/pnpm#11532) the age is told in minutes, hours or days
            // (PnpmRelease#tellsAReleaseAgeInItsLargestWholeUnit), asked where the two tell it otherwise
            String inMinutesHoursOrDays = formatTimeAgoInMinutesHoursOrDays(released);
            if (!inMinutesHoursOrDays.equals(releaseAgeText) && !release.tellsAReleaseAgeInItsLargestWholeUnit()) {
                releaseAgeText = inMinutesHoursOrDays;
            }
            String pkgName = Js.truthy(wantedDependency.getAlias()) ? wantedDependency.getAlias() :
                    Js.string(meta.get("name"));
            return new PnpmError("NO_MATURE_MATCHING_VERSION", "Version " + immatureVersion + " (released " +
                    releaseAgeText + ") of " + pkgName + " does not meet the minimumReleaseAge constraint");
        }
        return new PnpmError("NO_MATURE_MATCHING_VERSION", "No matching version found for " + dep +
                                                           " while fetching it from " + registryUrl);
    }

    // formatTimeAgo of 11.1.2's npm-resolver, the run's clock as Date.now()
    private @Nullable String formatTimeAgo(double ts) {
        if (Double.isNaN(ts)) {
            return null;
        }
        double diffMs = now.toEpochMilli() - ts;
        if (diffMs < 0) {
            return null;
        }
        long diffSec = (long) Math.floor(diffMs / 1e3);
        long diffMin = diffSec / 60;
        long diffHour = diffMin / 60;
        long diffDay = diffHour / 24;
        long diffMonth = diffDay / 30;
        long diffYear = diffDay / 365;
        if (diffYear > 0) {
            return diffYear + " year" + (diffYear == 1 ? "" : "s") + " ago";
        }
        if (diffMonth > 0) {
            return diffMonth + " month" + (diffMonth == 1 ? "" : "s") + " ago";
        }
        if (diffDay > 0) {
            return diffDay + " day" + (diffDay == 1 ? "" : "s") + " ago";
        }
        if (diffHour > 0) {
            return diffHour + " hour" + (diffHour == 1 ? "" : "s") + " ago";
        }
        if (diffMin > 0) {
            return diffMin + " minute" + (diffMin == 1 ? "" : "s") + " ago";
        }
        return "a few seconds ago";
    }

    // formatTimeAgo of npm-resolver before 11.1.0: "just now" under a minute and for a date ahead, then minutes under
    // 90 of them, hours under 48 and days; "NaN minutes ago" for a date that does not parse
    private String formatTimeAgoInMinutesHoursOrDays(double ts) {
        double diffMs = now.toEpochMilli() - ts;
        if (diffMs < 60 * 1e3) {
            return "just now";
        }
        double diffMinutes = Math.floor(diffMs / (60 * 1e3));
        double diffHours = Math.floor(diffMs / (60 * 60 * 1e3));
        double diffDays = Math.floor(diffMs / (24 * 60 * 60 * 1e3));
        if (diffHours >= 48) {
            return Js.numberToString(diffDays) + " day" + (diffDays == 1 ? "" : "s") + " ago";
        }
        if (diffMinutes >= 90) {
            return Js.numberToString(diffHours) + " hour" + (diffHours == 1 ? "" : "s") + " ago";
        }
        return Js.numberToString(diffMinutes) + " minute" + (diffMinutes == 1 ? "" : "s") + " ago";
    }

    @Nullable
    ResolveResult resolveJsr(ResolveDependencies.WantedDependency wantedDependency,
                             PackageRequester.RequestPackageOptions opts,
                             PreferredVersions.@Nullable ByName preferredVersions) {
        if (!Js.truthy(wantedDependency.getBareSpecifier())) {
            return null;
        }
        PickPackageFromMeta.JsrRegistryPackageSpec spec = PickPackageFromMeta.parseJsrSpecifierToRegistryPackageSpec(
                wantedDependency.getBareSpecifier(), wantedDependency.getAlias(),
                opts.defaultTag == null ? "latest" : opts.defaultTag, release);
        if (spec == null) {
            return null;
        }
        Function<String, @Nullable Object> publishedByExclude = opts.publishedByExclude;
        boolean optional = wantedDependency.isOptional();
        // before 11.1.0 the jsr resolver passes pickPackage neither the release-age exclusions nor the dependency's
        // optional (PnpmRelease#picksAJsrPackageWithItsExclusionsAndOptionality), asked where either reaches the
        // pick: an exclusion of the package under a cutoff, or an optional dependency without full metadata
        boolean excluded = opts.publishedBy != null && publishedByExclude != null &&
                           Js.truthy(publishedByExclude.apply(spec.getSpec().getName()));
        if ((excluded || optional && !fullMetadata) && !release.picksAJsrPackageWithItsExclusionsAndOptionality()) {
            publishedByExclude = null;
            optional = false;
        }
        ResolveResult picked = pickFromSimpleRegistry(wantedDependency, opts, preferredVersions, spec.getSpec(),
                registries.get("@jsr"), publishedByExclude, optional);
        if (opts.calcSpecifier) {
            picked.normalizedBareSpecifier = calcPrefixedSpecifier("jsr:", spec.getJsrPkgName(), wantedDependency,
                    versionOf(picked), opts.pinnedVersion);
        }
        picked.resolvedVia = "jsr-registry";
        picked.alias = spec.getJsrPkgName();
        return picked;
    }

    @Nullable
    ResolveResult resolveFromNamedRegistry(ResolveDependencies.WantedDependency wantedDependency,
                                           PackageRequester.RequestPackageOptions opts,
                                           PreferredVersions.@Nullable ByName preferredVersions) {
        if (!Js.truthy(wantedDependency.getBareSpecifier())) {
            return null;
        }
        PickPackageFromMeta.NamedRegistryPackageSpec spec =
                PickPackageFromMeta.parseNamedRegistrySpecifierToRegistryPackageSpec(
                        wantedDependency.getBareSpecifier(), namedRegistries.keySet(), wantedDependency.getAlias(),
                        opts.defaultTag == null ? "latest" : opts.defaultTag, release);
        if (spec == null) {
            return null;
        }
        String registryUrl = namedRegistries.get(spec.getRegistryName());
        if (!Js.truthy(registryUrl)) {
            return null;
        }
        ResolveResult picked = pickFromSimpleRegistry(wantedDependency, opts, preferredVersions, spec.getSpec(),
                registryUrl, opts.publishedByExclude, wantedDependency.isOptional());
        if (opts.calcSpecifier) {
            picked.normalizedBareSpecifier = calcPrefixedSpecifier(spec.getRegistryName() + ":",
                    spec.getSpec().getName(), wantedDependency, versionOf(picked), opts.pinnedVersion);
        }
        picked.resolvedVia = "named-registry";
        picked.alias = spec.getSpec().getName();
        return picked;
    }

    private static String versionOf(ResolveResult picked) {
        return Js.string(picked.manifest == null ? null : picked.manifest.get("version"));
    }

    // the pick of the jsr and named-registry resolvers from one registry: no workspace or trust check; the
    // publishedByExclude and optional pickPackage takes, which before 11.1.0 the jsr resolver passes none of
    private ResolveResult pickFromSimpleRegistry(ResolveDependencies.WantedDependency wantedDependency,
                                                 PackageRequester.RequestPackageOptions opts,
                                                 PreferredVersions.@Nullable ByName preferredVersions,
                                                 PickPackageFromMeta.RegistryPackageSpec spec, String registryUrl,
                                                 @Nullable Function<String, @Nullable Object> publishedByExclude,
                                                 boolean optional) {
        PickPackage.PickerOptions pickerOpts = PickPackage.PickerOptions.builder()
                .preferredVersionSelectors(preferredVersions == null ? null : preferredVersions.get(spec.getName()))
                .publishedBy(opts.publishedBy)
                .publishedByExclude(publishedByExclude)
                .pickLowestVersion(opts.pickLowestVersion)
                .includeLatestTag("latest".equals(opts.update))
                .ignoreMissingTimeField(ignoreMissingTimeField)
                .strictPublishedByCheck(strictPublishedByCheck)
                .release(release)
                .build();
        PickPackage.PickResult pickResult = PickPackage.pickPackage(metaCache, registry, spec, pickerOpts,
                registryUrl, optional, fullMetadata);
        Map<String, @Nullable Object> pickedPackage = pickResult.getPickedPackage();
        Map<String, @Nullable Object> meta = pickResult.getMeta();
        if (pickedPackage == null) {
            String alias = wantedDependency.getAlias();
            String bareSpecifier = wantedDependency.getBareSpecifier();
            String dep = alias != null && !alias.isEmpty() ?
                    alias + "@" + (bareSpecifier == null ? "" : bareSpecifier) : String.valueOf(bareSpecifier);
            throw new PnpmError("NO_MATCHING_VERSION", "No matching version found for " + dep +
                                                       " while fetching it from " + registryUrl);
        }
        String pickedName = Js.string(pickedPackage.get("name"));
        String pickedVersion = Js.string(pickedPackage.get("version"));
        ResolveResult result = new ResolveResult();
        result.id = pickedName + "@" + pickedVersion;
        Map<String, @Nullable Object> dist = ResolveDependencies.asMap(pickedPackage.get("dist"));
        String integrity = getIntegrity(dist);
        if (integrity != null) {
            result.resolution.put("integrity", integrity);
        }
        result.resolution.put("tarball", normalizeRegistryUrl(Js.string(dist.get("tarball"))));
        result.manifest = pickedPackage;
        result.latest = latestOf(meta);
        Object time = meta.get("time");
        Object publishedAt = time instanceof Map ? ((Map<?, ?>) time).get(pickedVersion) : null;
        result.publishedAt = publishedAt == null ? null : Js.string(publishedAt);
        result.policyViolation = policyViolation(detectMinReleaseAgeViolation(pickedName, pickedVersion,
                result.publishedAt, opts.publishedBy, opts.publishedByExclude));
        return result;
    }

    String calcPrefixedSpecifier(String prefix, String pkgName,
                                 ResolveDependencies.WantedDependency wantedDependency, String version,
                                 @Nullable String defaultPinnedVersion) {
        String range = calcRange(version, wantedDependency, defaultPinnedVersion);
        String alias = wantedDependency.getAlias();
        if (alias == null || alias.isEmpty() || pkgName.equals(alias)) {
            return prefix + range;
        }
        return prefix + pkgName + "@" + range;
    }

    private static @Nullable String latestOf(Map<String, @Nullable Object> meta) {
        Object latest = ResolveDependencies.asMap(meta.get("dist-tags")).get("latest");
        return latest == null ? null : Js.string(latest);
    }

    private @Nullable ResolveResult tryResolveFromWorkspace(ResolveDependencies.WantedDependency wantedDependency,
                                                            String defaultTag, String registryUrl,
                                                            PackageRequester.RequestPackageOptions opts) {
        String bareSpecifier = workspacePrefToNpm(wantedDependency.getBareSpecifier());
        PickPackageFromMeta.RegistryPackageSpec spec = PickPackageFromMeta.parseBareSpecifier(bareSpecifier,
                wantedDependency.getAlias(), defaultTag, registryUrl, release);
        if (spec == null) {
            throw new JsError(JsError.ERROR, "Invalid workspace: spec (" + wantedDependency.getBareSpecifier() + ")");
        }
        if (opts.workspacePackages == null) {
            throw new JsError(JsError.ERROR,
                    "Cannot resolve package from workspace because opts.workspacePackages is not defined");
        }
        if (opts.projectDir == null) {
            throw new JsError(JsError.ERROR,
                    "Cannot resolve package from workspace because opts.projectDir is not defined");
        }
        return tryResolveFromWorkspacePackages(opts.workspacePackages, spec, wantedDependency, opts,
                opts.update != null);
    }

    private ResolveResult tryResolveFromWorkspacePackages(Map<String, Map<String, Verification.Project>> packages,
                                                          PickPackageFromMeta.RegistryPackageSpec spec,
                                                          ResolveDependencies.WantedDependency wantedDependency,
                                                          PackageRequester.RequestPackageOptions opts,
                                                          boolean update) {
        Map<String, Verification.Project> matchingName = packages.get(spec.getName());
        String projectDir = opts.projectDir == null ? "" : opts.projectDir;
        if (matchingName == null) {
            throw new PnpmError("WORKSPACE_PKG_NOT_FOUND", "In " + displayDir(projectDir) + ": \"" + spec.getName() +
                                                           "@" + wantedDependency.getBareSpecifier() +
                                                           "\" is in the dependencies but no package named \"" +
                                                           spec.getName() + "\" is present in the workspace");
        }
        String localVersion = pickMatchingLocalVersionOrNull(matchingName, update ?
                new PickPackageFromMeta.RegistryPackageSpec("range", spec.getName(), "*", null) : spec);
        if (localVersion == null) {
            List<String> available = new ArrayList<>(matchingName.keySet());
            NodeSemver.rsort(available, NodeSemver.Options.DEFAULT);
            throw new PnpmError("NO_MATCHING_VERSION_INSIDE_WORKSPACE", "In " + displayDir(projectDir) +
                    ": No matching version found for " + wantedDependency.getAlias() + "@" +
                    wantedDependency.getBareSpecifier() + " inside the workspace" +
                    (available.isEmpty() ? "" : ". Available versions: " + String.join(", ", available)));
        }
        return resolveFromLocalPackage(matchingName.get(localVersion), spec, wantedDependency, opts);
    }

    private static String displayDir(String projectDir) {
        return projectDir.isEmpty() ? "" : projectDir;
    }

    private @Nullable String pickMatchingLocalVersionOrNull(Map<String, Verification.Project> versions,
                                                            PickPackageFromMeta.RegistryPackageSpec spec) {
        List<String> localVersions = new ArrayList<>(versions.keySet());
        switch (spec.getType()) {
            case "tag":
                return NodeSemver.maxSatisfying(localVersions, "*", NodeSemver.Options.INCLUDE_PRERELEASE);
            case "version":
                return versions.containsKey(spec.getFetchSpec()) ? spec.getFetchSpec() : null;
            case "range":
                return resolveWorkspaceRange(spec.getFetchSpec(), localVersions, release);
            default:
                return null;
        }
    }

    // @pnpm/workspace.range-resolver, over the node-semver the release bundles (any of them reads * alike)
    static @Nullable String resolveWorkspaceRange(String range, List<String> versions, PnpmRelease release) {
        if ("*".equals(range) || "^".equals(range) || "~".equals(range) || range.isEmpty()) {
            return NodeSemver.maxSatisfying(versions, "*", NodeSemver.Options.INCLUDE_PRERELEASE);
        }
        return release.underSemver(semver -> NodeSemver.maxSatisfying(versions, range,
                NodeSemver.Options.LOOSE.release(semver)));
    }

    private ResolveResult resolveFromLocalPackage(Verification.Project localPackage,
                                                  PickPackageFromMeta.RegistryPackageSpec spec,
                                                  ResolveDependencies.WantedDependency wantedDependency,
                                                  PackageRequester.RequestPackageOptions opts) {
        boolean hardLinkLocalPackages = opts.injectWorkspacePackages ||
                                        Boolean.TRUE.equals(wantedDependency.getInjected());
        String localPackageDir = resolveLocalPackageDir(localPackage);
        String directory;
        String id;
        if (hardLinkLocalPackages) {
            directory = RootRelativePath.relative(opts.lockfileDir == null ? "" : opts.lockfileDir, localPackageDir);
            id = "file:" + directory;
        } else {
            directory = localPackageDir;
            id = "link:" + RootRelativePath.relative(opts.projectDir == null ? "" : opts.projectDir, localPackageDir);
        }
        ResolveResult result = new ResolveResult();
        result.id = id;
        result.manifest = ReadPackageHook.deepCopy(localPackage.getManifest());
        result.resolution.put("directory", directory);
        result.resolution.put("type", "directory");
        result.resolvedVia = "workspace";
        if (opts.calcSpecifier) {
            result.normalizedBareSpecifier = spec.getNormalizedBareSpecifier() != null ?
                    spec.getNormalizedBareSpecifier() :
                    calcSpecifierForWorkspaceDep(wantedDependency, spec,
                            Js.string(localPackage.getManifest().get("version")), opts.pinnedVersion);
        }
        return result;
    }

    private static String resolveLocalPackageDir(Verification.Project localPackage) {
        Map<String, @Nullable Object> publishConfig = ResolveDependencies.asMap(
                localPackage.getManifest().get("publishConfig"));
        if (publishConfig.get("directory") == null || Boolean.FALSE.equals(publishConfig.get("linkDirectory"))) {
            return localPackage.getRootDir();
        }
        return RootRelativePath.join(localPackage.getRootDir(), Js.string(publishConfig.get("directory")));
    }

    private String calcSpecifierForWorkspaceDep(ResolveDependencies.WantedDependency wantedDependency,
                                                PickPackageFromMeta.RegistryPackageSpec spec, String version,
                                                @Nullable String defaultPinnedVersion) {
        boolean save = Js.truthy(saveWorkspaceProtocol);
        if (!save && !wantedDependency.getBareSpecifier().startsWith("workspace:")) {
            return calcSpecifier(wantedDependency, spec, version, defaultPinnedVersion);
        }
        String alias = wantedDependency.getAlias();
        String prefix = alias == null || alias.isEmpty() || spec.getName().equals(alias) ? "workspace:" :
                "workspace:" + spec.getName() + "@";
        if ("rolling".equals(saveWorkspaceProtocol)) {
            String specifier = wantedDependency.getPrevSpecifier() != null ? wantedDependency.getPrevSpecifier() :
                    wantedDependency.getBareSpecifier();
            if (specifier != null && !specifier.isEmpty()) {
                if (specifier.equals(prefix + "*") || specifier.equals(prefix + "^") ||
                    specifier.equals(prefix + "~")) {
                    return specifier;
                }
                String pinned = whichVersionIsPinned(specifier, release);
                if ("major".equals(pinned)) {
                    return prefix + "^";
                } else if ("minor".equals(pinned)) {
                    return prefix + "~";
                } else if ("patch".equals(pinned) || "none".equals(pinned)) {
                    return prefix + "*";
                }
            }
            return prefix + "^";
        }
        List<Object> prerelease = NodeSemver.valid(version, NodeSemver.Options.DEFAULT) == null ? null :
                NodeSemver.prerelease(version, NodeSemver.Options.DEFAULT);
        if (prerelease != null && !prerelease.isEmpty()) {
            return prefix + version;
        }
        String pinnedVersion = wantedDependency.getPrevSpecifier() != null ?
                whichVersionIsPinned(wantedDependency.getPrevSpecifier(), release) : null;
        return prefix + createVersionSpec(version, pinnedVersion != null ? pinnedVersion : defaultPinnedVersion);
    }

    String calcSpecifier(ResolveDependencies.WantedDependency wantedDependency,
                         PickPackageFromMeta.RegistryPackageSpec spec, String version,
                         @Nullable String defaultPinnedVersion) {
        String prev = wantedDependency.getPrevSpecifier();
        if (prev != null && !prev.isEmpty() && prev.equals(wantedDependency.getBareSpecifier()) &&
            "tag".equals(Verification.versionSelectorType(prev, release))) {
            return prev;
        }
        String range = calcRange(version, wantedDependency, defaultPinnedVersion);
        String alias = wantedDependency.getAlias();
        if (alias == null || alias.isEmpty() || spec.getName().equals(alias)) {
            return range;
        }
        return "npm:" + spec.getName() + "@" + range;
    }

    private String calcRange(String version, ResolveDependencies.WantedDependency wantedDependency,
                             @Nullable String defaultPinnedVersion) {
        if (NodeSemver.valid(version, NodeSemver.Options.DEFAULT) != null) {
            List<Object> prerelease = NodeSemver.prerelease(version, NodeSemver.Options.DEFAULT);
            if (prerelease != null && !prerelease.isEmpty()) {
                return version;
            }
        }
        String pinnedVersion = null;
        if (wantedDependency.getPrevSpecifier() != null && !wantedDependency.getPrevSpecifier().isEmpty()) {
            pinnedVersion = whichVersionIsPinned(wantedDependency.getPrevSpecifier(), release);
        }
        if (pinnedVersion == null && wantedDependency.getBareSpecifier() != null &&
            !wantedDependency.getBareSpecifier().isEmpty()) {
            pinnedVersion = whichVersionIsPinned(wantedDependency.getBareSpecifier(), release);
        }
        if (pinnedVersion == null) {
            pinnedVersion = defaultPinnedVersion;
        }
        return createVersionSpec(version, pinnedVersion);
    }

    static String createVersionSpec(String version, @Nullable String pinnedVersion) {
        switch (pinnedVersion == null ? "major" : pinnedVersion) {
            case "none":
            case "major":
                return "^" + version;
            case "minor":
                return "~" + version;
            case "patch":
                return version;
            default:
                throw new PnpmError("BAD_PINNED_VERSION", "Cannot pin '" + pinnedVersion + "'");
        }
    }

    /**
     * whichVersionIsPinned.ts over semver-utils' {@code parseRange}.
     */
    /**
     * {@link #whichVersionIsPinned(String)} as {@code release} reads it: from 11.8.0 a {@code catalog:} specifier pins
     * nothing ({@link PnpmRelease#takesNoPinFromACatalogSpecifier}); before, its catalog's name reads as a version.
     * From 11.14.0 a version prefixed with {@code =} pins as the version does
     * ({@link PnpmRelease#pinsAVersionPrefixedWithEqualsAsTheVersion}); before, it pins nothing.
     */
    static @Nullable String whichVersionIsPinned(String spec, PnpmRelease release) {
        String pinned = whichVersionIsPinned(spec);
        String pinnedFrom11140 = whichVersionIsPinned(spec, true);
        if (spec.startsWith("catalog:") && (pinned != null || pinnedFrom11140 != null) &&
            release.takesNoPinFromACatalogSpecifier()) {
            return null;
        }
        return Objects.equals(pinned, pinnedFrom11140) || !release.pinsAVersionPrefixedWithEqualsAsTheVersion() ?
                pinned : pinnedFrom11140;
    }

    static @Nullable String whichVersionIsPinned(String spec) {
        return whichVersionIsPinned(spec, false);
    }

    // whichVersionIsPinned.ts, where 11.14.0 takes an = operator as none
    private static @Nullable String whichVersionIsPinned(String spec, boolean equalsAsNoOperator) {
        int colonIndex = spec.indexOf(':');
        if (colonIndex != -1) {
            spec = spec.substring(colonIndex + 1);
        }
        int index = spec.lastIndexOf('@');
        if (index != -1) {
            spec = spec.substring(index + 1);
        }
        if ("*".equals(spec)) {
            return "none";
        }
        List<String[]> parsedRange = new ArrayList<>();
        Matcher m = SEMVER_RANGE.matcher(spec);
        while (m.find()) {
            String operator = m.group(4) != null && !m.group(4).isEmpty() ? m.group(4) : m.group(2);
            parsedRange.add(new String[]{operator, m.group(6), m.group(8), m.group(10)});
        }
        if (parsedRange.size() != 1) {
            return null;
        }
        String[] versionObject = parsedRange.get(0);
        if (versionObject[0] == null || equalsAsNoOperator && "=".equals(versionObject[0])) {
            if (versionObject[3] != null && !versionObject[3].isEmpty()) {
                return "patch";
            }
            if (versionObject[2] != null && !versionObject[2].isEmpty()) {
                return "minor";
            }
            if (versionObject[1] != null && !versionObject[1].isEmpty()) {
                return "major";
            }
            return null;
        }
        switch (versionObject[0]) {
            case "~":
                return "minor";
            case "^":
                return "major";
            default:
                return null;
        }
    }

    /**
     * workspacePrefToNpm.ts with @pnpm/workspace.spec-parser's {@code WorkspaceSpec.parse}.
     */
    static String workspacePrefToNpm(String workspaceBareSpecifier) {
        Matcher m = WORKSPACE_SPEC.matcher(workspaceBareSpecifier);
        if (!m.matches()) {
            throw new JsError(JsError.ERROR, "Invalid workspace spec: " + workspaceBareSpecifier);
        }
        String alias = m.group(1);
        String version = m.group(2);
        String versionPart = "^".equals(version) || "~".equals(version) || version.isEmpty() ? "*" : version;
        return alias != null ? "npm:" + alias + "@" + versionPart : versionPart;
    }

    private static final Pattern WORKSPACE_SPEC = Pattern.compile("^workspace:(?:([^._/][^@]*)@)?(.*)$",
            Pattern.DOTALL);

    private static @Nullable String getIntegrity(Map<String, @Nullable Object> dist) {
        if (Js.truthy(dist.get("integrity"))) {
            return Js.string(dist.get("integrity"));
        }
        if (!Js.truthy(dist.get("shasum"))) {
            return null;
        }
        Ssri.Integrity integrity = Ssri.fromHex(Js.string(dist.get("shasum")), "sha1", Ssri.Release.V13_0_1);
        if (integrity == null) {
            throw new PnpmError("INVALID_TARBALL_INTEGRITY", "Tarball \"" + Js.string(dist.get("tarball")) +
                                                             "\" has invalid shasum specified in its metadata: " +
                                                             Js.string(dist.get("shasum")));
        }
        return integrity.toString();
    }

    static String normalizeRegistryUrl(String url) {
        WhatwgUrl parsed = WhatwgUrl.tryParse(url);
        return parsed == null ? url : parsed.toString();
    }

    // the violation a release reports from 11.1.3 on (PnpmRelease#checksTheReleaseAgeAsAPolicy); before, none
    private ResolveDependencies.@Nullable PolicyViolation policyViolation(
            ResolveDependencies.@Nullable PolicyViolation violation) {
        return violation != null && release.checksTheReleaseAgeAsAPolicy() ? violation : null;
    }

    static ResolveDependencies.@Nullable PolicyViolation detectMinReleaseAgeViolation(
            String name, String version, @Nullable String publishedAt, @Nullable Instant publishedBy,
            @Nullable Function<String, @Nullable Object> publishedByExclude) {
        if (publishedBy == null || publishedAt == null || publishedAt.isEmpty()) {
            return null;
        }
        Object excludeResult = publishedByExclude == null ? null : publishedByExclude.apply(name);
        if (Boolean.TRUE.equals(excludeResult)) {
            return null;
        }
        if (excludeResult instanceof List && ((List<?>) excludeResult).contains(version)) {
            return null;
        }
        double ts = V8Date.dateParse(publishedAt);
        if (Double.isNaN(ts) || ts <= publishedBy.toEpochMilli()) {
            return null;
        }
        return new ResolveDependencies.PolicyViolation(name, version, MINIMUM_RELEASE_AGE_VIOLATION_CODE,
                "was published at " + V8Date.toISOString(ts) + ", within the minimumReleaseAge cutoff (" +
                V8Date.toISOString(publishedBy.toEpochMilli()) + ")");
    }
}
