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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.UriComponent;
import org.openrewrite.javascript.internal.lock.V8Date;
import org.openrewrite.javascript.internal.lock.ValidateNpmPackageName;
import org.openrewrite.javascript.internal.lock.WhatwgUrl;
import org.openrewrite.javascript.semver.NodeSemver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Port of @pnpm/resolving.npm-resolver src/pickPackageFromMeta.ts and src/parseBareSpecifier.ts (pnpm 11.2.2), with
 * @pnpm/resolving.registry.pkg-metadata-filter and parse-npm-tarball-url 5.0.0.
 */
final class PickPackageFromMeta {

    private PickPackageFromMeta() {
    }

    /**
     * {@code RegistryPackageSpec}: a {@code tag}, {@code version} or {@code range} of a package name.
     */
    @Value
    static class RegistryPackageSpec {
        String type;
        String name;
        String fetchSpec;

        @Nullable
        String normalizedBareSpecifier;
    }

    /**
     * {@code pickVersionByVersionRange}'s and {@code pickLowestVersionByVersionRange}'s arguments.
     */
    @Value
    static class VersionRangeOptions {
        Map<String, @Nullable Object> meta;
        String versionRange;

        @Nullable
        Map<String, PreferredVersions.Selector> preferredVersionSelectors;

        /**
         * The release whose node-semver reads the range.
         */
        PnpmRelease release;

        // semver's range functions under the release's node-semver, the loose options npm-resolver passes
        boolean satisfies(String version) {
            return release.underSemver(semver -> NodeSemver.satisfies(version, versionRange,
                    NodeSemver.Options.LOOSE.release(semver)));
        }

        @Nullable
        String maxSatisfying(List<String> versions) {
            return release.underSemver(semver -> NodeSemver.maxSatisfying(versions, versionRange,
                    NodeSemver.Options.LOOSE.release(semver)));
        }

        @Nullable
        String minSatisfying(List<String> versions) {
            return release.underSemver(semver -> NodeSemver.minSatisfying(versions, versionRange,
                    NodeSemver.Options.LOOSE.release(semver)));
        }
    }

    // semver.validRange(range) under the release's node-semver
    private static boolean validRange(String range, PnpmRelease release) {
        return release.underSemver(semver -> NodeSemver.validRange(range,
                NodeSemver.Options.DEFAULT.release(semver))) != null;
    }

    /**
     * {@code parseBareSpecifier} of the npm resolver for a registry spec ({@code npm:} aliases, versions, ranges,
     * tags, a tarball URL on the registry); null for any other specifier.
     */
    static @Nullable RegistryPackageSpec parseBareSpecifier(String bareSpecifier, @Nullable String alias,
                                                            String defaultTag, String registry) {
        return parseBareSpecifier(bareSpecifier, alias, defaultTag, registry, PnpmRelease.PORTED);
    }

    /**
     * {@link #parseBareSpecifier(String, String, String, String)} over the node-semver {@code release} bundles.
     */
    static @Nullable RegistryPackageSpec parseBareSpecifier(String bareSpecifier, @Nullable String alias,
                                                            String defaultTag, String registry,
                                                            PnpmRelease release) {
        String name = alias;
        if (bareSpecifier.startsWith("npm:")) {
            bareSpecifier = bareSpecifier.substring(4);
            // before 11.1.0 (pnpm/pnpm#11324) a range after npm: is taken for a package name, as anything without an
            // @ past its first character (PnpmRelease#takesTheAliasAsTheNameOfAnNpmRange); the two read every such
            // range otherwise
            if (alias != null && !alias.isEmpty() &&
                validRange(bareSpecifier, release) && release.takesTheAliasAsTheNameOfAnNpmRange()) {
                name = alias;
            } else {
                int index = bareSpecifier.lastIndexOf('@');
                if (index < 1) {
                    name = bareSpecifier;
                    bareSpecifier = defaultTag;
                } else {
                    name = bareSpecifier.substring(0, index);
                    bareSpecifier = bareSpecifier.substring(index + 1);
                }
            }
        }
        if (name != null && !name.isEmpty()) {
            RegistryPackageSpec spec = selectorSpec(name, bareSpecifier, release);
            if (spec != null) {
                return spec;
            }
        }
        if (bareSpecifier.startsWith(registry)) {
            String[] pkg = parseNpmTarballUrl(bareSpecifier);
            if (pkg != null) {
                return new RegistryPackageSpec("version", pkg[0], pkg[1], bareSpecifier);
            }
        }
        return null;
    }

    // version-selector-type's {type, normalized} for a package name
    private static @Nullable RegistryPackageSpec selectorSpec(String name, String selector, PnpmRelease release) {
        String type = Verification.versionSelectorType(selector, release);
        if (type == null) {
            return null;
        }
        String normalized = "version".equals(type) ? NodeSemver.valid(selector, NodeSemver.Options.LOOSE) :
                "range".equals(type) ? release.underSemver(semver -> NodeSemver.validRange(selector,
                        NodeSemver.Options.LOOSE.release(semver))) : selector;
        return new RegistryPackageSpec(type, name, normalized, null);
    }

    static final Map<String, String> BUILTIN_NAMED_REGISTRIES =
            Collections.singletonMap("gh", "https://npm.pkg.github.com/");

    @Value
    static class JsrRegistryPackageSpec {
        RegistryPackageSpec spec;
        String jsrPkgName;
    }

    @Value
    static class NamedRegistryPackageSpec {
        RegistryPackageSpec spec;
        String registryName;
    }

    static @Nullable JsrRegistryPackageSpec parseJsrSpecifierToRegistryPackageSpec(String rawSpecifier,
                                                                                   @Nullable String alias,
                                                                                   String defaultTag,
                                                                                   PnpmRelease release) {
        JsrSpecifierParser.JsrSpec spec = JsrSpecifierParser.parseJsrSpecifier(rawSpecifier, alias, release);
        if (spec == null || !Js.truthy(spec.getNpmPkgName())) {
            return null;
        }
        RegistryPackageSpec selector = selectorSpec(spec.getNpmPkgName(),
                spec.getVersionSelector() != null ? spec.getVersionSelector() : defaultTag, release);
        return selector == null ? null : new JsrRegistryPackageSpec(selector, spec.getJsrPkgName());
    }

    static @Nullable NamedRegistryPackageSpec parseNamedRegistrySpecifierToRegistryPackageSpec(
            String rawSpecifier, Set<String> knownRegistryNames, @Nullable String packageAlias, String defaultTag,
            PnpmRelease release) {
        int colon = rawSpecifier.indexOf(':');
        if (colon <= 0) {
            return null;
        }
        String registryName = rawSpecifier.substring(0, colon);
        if (!knownRegistryNames.contains(registryName)) {
            return null;
        }
        String body = rawSpecifier.substring(colon + 1);
        String pkgName;
        String versionSelector = null;
        boolean scopeWithoutName = false;
        if (ResolveDependencies.validRange(body, release) != null) {
            if (!Js.truthy(packageAlias)) {
                return null;
            }
            pkgName = packageAlias;
            versionSelector = body;
        } else if (!body.isEmpty() && body.charAt(0) == '@') {
            int index = body.lastIndexOf('@');
            if (index == 0) {
                pkgName = body;
            } else {
                pkgName = body.substring(0, index);
                versionSelector = body.substring(index + 1);
            }
            scopeWithoutName = pkgName.indexOf('/') == -1 || pkgName.endsWith("/");
        } else if (packageAlias != null && packageAlias.startsWith("@")) {
            pkgName = packageAlias;
            versionSelector = body;
        } else {
            int index = body.lastIndexOf('@');
            if (index < 1) {
                pkgName = body;
            } else {
                pkgName = body.substring(0, index);
                versionSelector = body.substring(index + 1);
            }
            if (pkgName.isEmpty()) {
                return null;
            }
        }
        // from 11.10.0 any form's name validate-npm-package-name 7.0.2 rejects is invalid; before, only a scope's
        // without a name (PnpmRelease#validatesANamedRegistryPackageName), asked where the two differ
        boolean invalidName = !ValidateNpmPackageName.validForOldPackages(pkgName,
                ValidateNpmPackageName.Release.V7_0_2);
        if (invalidName == scopeWithoutName ? invalidName :
                release.validatesANamedRegistryPackageName() ? invalidName : scopeWithoutName) {
            throw new PnpmError("INVALID_NAMED_REGISTRY_PACKAGE_NAME", "The package name '" + pkgName +
                    "' in named registry '" + registryName + ":' is invalid");
        }
        RegistryPackageSpec selector = selectorSpec(pkgName, versionSelector != null ? versionSelector : defaultTag,
                release);
        return selector == null ? null : new NamedRegistryPackageSpec(selector, registryName);
    }

    // parse-npm-tarball-url 5.0.0: the name and version a registry tarball URL names, null for another URL
    private static String @Nullable [] parseNpmTarballUrl(String url) {
        WhatwgUrl parsed = WhatwgUrl.tryParse(url);
        if (parsed == null) {
            throw new JsError(JsError.TYPE_ERROR, "Invalid URL");
        }
        String path = parsed.getPathname();
        if (path.isEmpty() || parsed.getHost().isEmpty()) {
            return null;
        }
        String[] parts = path.split("/-/", -1);
        if (parts.length != 2) {
            return null;
        }
        String name;
        try {
            name = parts[0].isEmpty() ? "" : UriComponent.decodeURIComponent(parts[0].substring(1));
        } catch (IllegalArgumentException e) {
            throw new JsError(JsError.URI_ERROR, "URI malformed");
        }
        if (name.isEmpty()) {
            return null;
        }
        String pathWithNoExtension = parts[1].replaceFirst("\\.tgz$", "");
        int scopelessNameLength = name.length() - (name.indexOf('/') + 1);
        String version = scopelessNameLength + 1 <= pathWithNoExtension.length() ?
                pathWithNoExtension.substring(scopelessNameLength + 1) : "";
        if (NodeSemver.valid(version, NodeSemver.Options.LOOSE) == null) {
            return null;
        }
        return new String[]{name, version};
    }

    /**
     * {@code pickPackageFromMeta}: the version manifest the spec selects, null when none does;
     * {@code publishedByExclude} answers true, the versions it trusts, or null.
     */
    static @Nullable Map<String, @Nullable Object> pickPackageFromMeta(
            Function<VersionRangeOptions, @Nullable String> pickVersionByVersionRange,
            @Nullable Map<String, PreferredVersions.Selector> preferredVersionSelectors,
            @Nullable Instant publishedBy, @Nullable Function<String, @Nullable Object> publishedByExclude,
            Map<String, @Nullable Object> meta, RegistryPackageSpec spec) {
        return pickPackageFromMeta(pickVersionByVersionRange, preferredVersionSelectors, publishedBy,
                publishedByExclude, meta, spec, PnpmRelease.PORTED);
    }

    /**
     * {@link #pickPackageFromMeta(Function, Map, Instant, Function, Map, RegistryPackageSpec)} as {@code release}
     * runs it: before 11.2.0 a packument without times modified at the cutoff has none
     * ({@link PnpmRelease#takesAPackumentModifiedAtTheCutoffAsMature}).
     */
    @SuppressWarnings("unchecked")
    static @Nullable Map<String, @Nullable Object> pickPackageFromMeta(
            Function<VersionRangeOptions, @Nullable String> pickVersionByVersionRange,
            @Nullable Map<String, PreferredVersions.Selector> preferredVersionSelectors,
            @Nullable Instant publishedBy, @Nullable Function<String, @Nullable Object> publishedByExclude,
            Map<String, @Nullable Object> meta, RegistryPackageSpec spec, PnpmRelease release) {
        if (publishedBy != null) {
            Object excludeResult = publishedByExclude == null ? null :
                    publishedByExclude.apply(Js.string(meta.get("name")));
            if (!Boolean.TRUE.equals(excludeResult)) {
                if (meta.get("time") != null) {
                    assertMetaHasTime(meta);
                    meta = filterPkgMetadataByPublishDate(meta, publishedBy,
                            excludeResult instanceof List ? (List<String>) excludeResult : null);
                } else {
                    Instant modifiedDate = parseModifiedDate(meta.get("modified"));
                    if (modifiedDate == null || modifiedDate.isAfter(publishedBy) ||
                        modifiedDate.equals(publishedBy) && !release.takesAPackumentModifiedAtTheCutoffAsMature()) {
                        assertMetaHasTime(meta);
                    }
                }
            }
        }
        Object versions = meta.get("versions");
        if ((!(versions instanceof Map) || ((Map<?, ?>) versions).isEmpty()) && publishedBy == null) {
            Object time = meta.get("time");
            Object unpublished = time instanceof Map ? ((Map<String, Object>) time).get("unpublished") : null;
            Object unpublishedVersions = unpublished instanceof Map ?
                    ((Map<String, Object>) unpublished).get("versions") : null;
            if (unpublishedVersions instanceof List && !((List<?>) unpublishedVersions).isEmpty()) {
                throw new PnpmError("UNPUBLISHED_PKG", "No versions available for " + spec.getName() +
                                                       " because it was unpublished");
            }
            throw new PnpmError("NO_VERSIONS", "No versions available for " + spec.getName() +
                                               ". The package may be unpublished.");
        }
        try {
            String version;
            switch (spec.getType()) {
                case "version":
                    version = spec.getFetchSpec();
                    break;
                case "tag": {
                    Object tag = ((Map<String, Object>) meta.get("dist-tags")).get(spec.getFetchSpec());
                    version = tag == null ? null : Js.string(tag);
                    break;
                }
                default:
                    version = pickVersionByVersionRange.apply(new VersionRangeOptions(meta, spec.getFetchSpec(),
                            preferredVersionSelectors, release));
            }
            if (version == null || version.isEmpty()) {
                return null;
            }
            Object manifest = ((Map<String, Object>) meta.get("versions")).get(version);
            if (manifest instanceof Map && Js.truthy(meta.get("name"))) {
                ((Map<String, Object>) manifest).put("name", meta.get("name"));
            }
            return (Map<String, @Nullable Object>) manifest;
        } catch (PnpmError | CannotKnow e) {
            throw e;
        } catch (RuntimeException e) {
            throw new PnpmError("MALFORMED_METADATA", "Received malformed metadata for \"" + spec.getName() + "\"",
                    "This might mean that the package was unpublished from the registry");
        }
    }

    static void assertMetaHasTime(Map<String, @Nullable Object> meta) {
        if (meta.get("time") == null) {
            throw new PnpmError("MISSING_TIME", "The metadata of " + Js.string(meta.get("name")) +
                                                " is missing the \"time\" field");
        }
    }

    private static @Nullable Instant parseModifiedDate(@Nullable Object modified) {
        if (!Js.truthy(modified)) {
            return null;
        }
        double time = V8Date.dateParse(Js.string(modified));
        return Double.isNaN(time) ? null : Instant.ofEpochMilli((long) time);
    }

    private static boolean semverSatisfiesLoose(@Nullable String version, String range, PnpmRelease release) {
        if (version == null) {
            return false;
        }
        try {
            return release.underSemver(semver -> NodeSemver.satisfies(version, range,
                    NodeSemver.Options.LOOSE.release(semver)));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> versionKeys(Map<String, @Nullable Object> meta) {
        Object versions = meta.get("versions");
        return versions instanceof Map ? new ArrayList<>(Js.keys((Map<String, Object>) versions)) : new ArrayList<>();
    }

    static @Nullable String pickLowestVersionByVersionRange(VersionRangeOptions options) {
        Map<String, PreferredVersions.Selector> selectors = options.getPreferredVersionSelectors();
        if (selectors != null && !selectors.isEmpty()) {
            for (List<String> preferredVersions : prioritizePreferredVersions(options.getMeta(),
                    options.getVersionRange(), selectors, options.getRelease())) {
                String preferredVersion = options.minSatisfying(preferredVersions);
                if (preferredVersion != null) {
                    return preferredVersion;
                }
            }
        }
        List<String> versions = versionKeys(options.getMeta());
        if ("*".equals(options.getVersionRange())) {
            // semver.compare is consistent, so any stable sort gives V8's order
            versions.sort((a, b) -> NodeSemver.compare(a, b, NodeSemver.Options.DEFAULT));
            return versions.isEmpty() ? null : versions.get(0);
        }
        return options.minSatisfying(versions);
    }

    @SuppressWarnings("unchecked")
    static @Nullable String pickVersionByVersionRange(VersionRangeOptions options) {
        Map<String, @Nullable Object> meta = options.getMeta();
        String versionRange = options.getVersionRange();
        Object latestValue = ((Map<String, Object>) meta.get("dist-tags")).get("latest");
        String latest = latestValue == null ? null : Js.string(latestValue);
        Map<String, PreferredVersions.Selector> selectors = options.getPreferredVersionSelectors();
        if (selectors != null && !selectors.isEmpty()) {
            for (List<String> preferredVersions : prioritizePreferredVersions(meta, versionRange, selectors,
                    options.getRelease())) {
                if (latest != null && preferredVersions.contains(latest) &&
                    semverSatisfiesLoose(latest, versionRange, options.getRelease())) {
                    return latest;
                }
                String preferredVersion = options.maxSatisfying(preferredVersions);
                if (preferredVersion != null) {
                    return preferredVersion;
                }
            }
        }
        List<String> versions = versionKeys(meta);
        if (Js.truthy(latest) && ("*".equals(versionRange) ||
                                  semverSatisfiesLoose(latest, versionRange, options.getRelease()))) {
            return latest;
        }
        String maxVersion = options.maxSatisfying(versions);
        Map<String, Object> versionManifests = (Map<String, Object>) meta.get("versions");
        if (maxVersion != null && Js.truthy(deprecated(versionManifests.get(maxVersion))) && versions.size() > 1) {
            List<String> nonDeprecatedVersions = new ArrayList<>();
            for (String version : versions) {
                Object versionMeta = versionManifests.get(version);
                if (!Js.truthy(deprecated(versionMeta))) {
                    Object v = versionMeta instanceof Map ? ((Map<String, Object>) versionMeta).get("version") : null;
                    nonDeprecatedVersions.add(Js.string(v == null ? Js.UNDEFINED : v));
                }
            }
            String maxNonDeprecatedVersion = options.maxSatisfying(nonDeprecatedVersions);
            if (maxNonDeprecatedVersion != null) {
                return maxNonDeprecatedVersion;
            }
        }
        return maxVersion;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Object deprecated(@Nullable Object versionMeta) {
        return versionMeta instanceof Map ? ((Map<String, Object>) versionMeta).get("deprecated") : null;
    }

    @SuppressWarnings("unchecked")
    static List<List<String>> prioritizePreferredVersions(
            Map<String, @Nullable Object> meta, String versionRange,
            Map<String, PreferredVersions.Selector> preferredVerSelectors, PnpmRelease release) {
        Map<String, Long> versionsPrioritizer = new LinkedHashMap<>();
        for (String version : versionKeys(meta)) {
            if (semverSatisfiesLoose(version, versionRange, release)) {
                add(versionsPrioritizer, version, 0);
            }
        }
        for (String preferredSelector : Js.keys(preferredVerSelectors)) {
            PreferredVersions.Selector selector = preferredVerSelectors.get(preferredSelector);
            long weight = selector.getWeight() == null ? 1 : selector.getWeight();
            if (preferredSelector.equals(versionRange)) {
                continue;
            }
            switch (selector.getSelectorType()) {
                case "tag": {
                    Object version = ((Map<String, Object>) meta.get("dist-tags")).get(preferredSelector);
                    add(versionsPrioritizer, version == null ? "undefined" : Js.string(version), weight);
                    break;
                }
                case "range":
                    for (String version : versionKeys(meta)) {
                        if (semverSatisfiesLoose(version, preferredSelector, release)) {
                            add(versionsPrioritizer, version, weight);
                        }
                    }
                    break;
                case "version": {
                    Object versions = meta.get("versions");
                    if (versions instanceof Map && Js.truthy(((Map<String, Object>) versions).get(preferredSelector))) {
                        add(versionsPrioritizer, preferredSelector, weight);
                    }
                    break;
                }
                default:
                    break;
            }
        }
        Map<Long, List<String>> versionsByWeight = new LinkedHashMap<>();
        for (String version : Js.keys(versionsPrioritizer)) {
            versionsByWeight.computeIfAbsent(versionsPrioritizer.get(version), w -> new ArrayList<>()).add(version);
        }
        List<Long> weights = new ArrayList<>(versionsByWeight.keySet());
        weights.sort((a, b) -> Long.compare(b, a));
        List<List<String>> byPriority = new ArrayList<>();
        for (Long weight : weights) {
            byPriority.add(versionsByWeight.get(weight));
        }
        return byPriority;
    }

    // PreferredVersionsPrioritizer.add: a falsy (zero) weight is replaced, not added to
    private static void add(Map<String, Long> preferredVersions, String version, long weight) {
        Long existing = preferredVersions.get(version);
        if (existing == null || existing == 0) {
            preferredVersions.put(version, weight);
        } else {
            preferredVersions.put(version, existing + weight);
        }
    }

    /**
     * @pnpm/resolving.registry.pkg-metadata-filter {@code filterPkgMetadataByPublishDate}: the versions published at
     * or before the cutoff (or trusted), each dist-tag kept or moved to the best version left.
     */
    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> filterPkgMetadataByPublishDate(Map<String, @Nullable Object> pkgDoc,
                                                                       Instant publishedBy,
                                                                       @Nullable List<String> trustedVersions) {
        Map<String, Object> versions = pkgDoc.get("versions") instanceof Map ?
                (Map<String, Object>) pkgDoc.get("versions") : new LinkedHashMap<>();
        Map<String, Object> time = (Map<String, Object>) pkgDoc.get("time");
        Map<String, @Nullable Object> versionsWithinDate = new LinkedHashMap<>();
        for (String version : Js.keys(versions)) {
            Object timeStr = time.get(version);
            boolean withinDate = Js.truthy(timeStr) && !Double.isNaN(V8Date.dateParse(Js.string(timeStr))) &&
                                 V8Date.dateParse(Js.string(timeStr)) <= publishedBy.toEpochMilli();
            if (withinDate || trustedVersions != null && trustedVersions.contains(version)) {
                versionsWithinDate.put(version, versions.get(version));
            }
        }
        Map<String, @Nullable Object> distTagsWithinDate = new LinkedHashMap<>();
        Map<String, Object> allDistTags = pkgDoc.get("dist-tags") instanceof Map ?
                (Map<String, Object>) pkgDoc.get("dist-tags") : new LinkedHashMap<>();
        for (String tag : Js.keys(allDistTags)) {
            String distTagVersion = Js.string(allDistTags.get(tag));
            if (Js.truthy(versionsWithinDate.get(distTagVersion))) {
                distTagsWithinDate.put(tag, distTagVersion);
                continue;
            }
            String original = NodeSemver.valid(distTagVersion, NodeSemver.Options.LOOSE);
            if (original == null) {
                continue;
            }
            boolean originalIsPrerelease = original.contains("-");
            long originalMajor = Long.parseLong(original.substring(0, original.indexOf('.')));
            String bestVersion = null;
            for (String candidate : Js.keys(versionsWithinDate)) {
                String parsed = NodeSemver.valid(candidate, NodeSemver.Options.LOOSE);
                if (parsed == null ||
                    !"latest".equals(tag) &&
                    Long.parseLong(parsed.substring(0, parsed.indexOf('.'))) != originalMajor ||
                    parsed.contains("-") != originalIsPrerelease) {
                    continue;
                }
                if (bestVersion == null) {
                    bestVersion = candidate;
                } else {
                    boolean candidateIsDeprecated = deprecated(versions.get(candidate)) != null;
                    boolean bestVersionIsDeprecated = deprecated(versions.get(bestVersion)) != null;
                    if (NodeSemver.compare(candidate, bestVersion, NodeSemver.Options.LOOSE) > 0 &&
                        bestVersionIsDeprecated == candidateIsDeprecated ||
                        bestVersionIsDeprecated && !candidateIsDeprecated) {
                        bestVersion = candidate;
                    }
                }
            }
            if (bestVersion != null) {
                distTagsWithinDate.put(tag, bestVersion);
            }
        }
        Map<String, @Nullable Object> filtered = new LinkedHashMap<>(pkgDoc);
        filtered.put("versions", versionsWithinDate);
        filtered.put("dist-tags", distTagsWithinDate);
        return filtered;
    }
}
