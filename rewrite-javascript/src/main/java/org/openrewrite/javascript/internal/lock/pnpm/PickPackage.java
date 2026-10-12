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

import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.V8Date;
import org.openrewrite.javascript.internal.lock.WhatwgUrl;
import org.openrewrite.javascript.semver.NodeSemver;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Port of @pnpm/resolving.npm-resolver src/pickPackage.ts (pnpm 11.2.2), the pickers it runs over a
 * packument.
 */
final class PickPackage {

    private PickPackage() {
    }

    @Value
    @Builder(toBuilder = true)
    @With
    static class PickerOptions {
        @Nullable
        Map<String, PreferredVersions.Selector> preferredVersionSelectors;

        @Nullable
        Instant publishedBy;

        @Nullable
        Function<String, @Nullable Object> publishedByExclude;

        boolean pickLowestVersion;

        boolean includeLatestTag;

        boolean ignoreMissingTimeField;

        /**
         * minimumReleaseAge under minimumReleaseAgeStrict, which before 11.1.3 leaves an immature range unpicked
         * ({@link PnpmRelease#checksTheReleaseAgeAsAPolicy}).
         */
        boolean strictPublishedByCheck;

        @Builder.Default
        PnpmRelease release = PnpmRelease.PORTED;
    }

    private static @Nullable Map<String, @Nullable Object> runPicker(
            PickerOptions pickerOpts, PickPackageFromMeta.RegistryPackageSpec spec,
            Function<PickPackageFromMeta.RegistryPackageSpec, @Nullable Map<String, @Nullable Object>> pickOne) {
        Map<String, @Nullable Object> currentPkg = pickOne.apply(spec);
        if (!pickerOpts.isIncludeLatestTag()) {
            return currentPkg;
        }
        Map<String, @Nullable Object> latestPkg = pickOne.apply(new PickPackageFromMeta.RegistryPackageSpec("tag",
                spec.getName(), "latest", spec.getNormalizedBareSpecifier()));
        return pickMax(latestPkg, currentPkg);
    }

    private static @Nullable Map<String, @Nullable Object> pickMax(@Nullable Map<String, @Nullable Object> a,
                                                                  @Nullable Map<String, @Nullable Object> b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return NodeSemver.compare(a.get("version"), b.get("version"), NodeSemver.Options.DEFAULT) < 0 ? b : a;
    }

    private static @Nullable Map<String, @Nullable Object> pickHighest(
            @Nullable Map<String, PreferredVersions.Selector> preferredVersionSelectors, @Nullable Instant publishedBy,
            @Nullable Function<String, @Nullable Object> publishedByExclude, Map<String, @Nullable Object> meta,
            PickPackageFromMeta.RegistryPackageSpec spec, PnpmRelease release) {
        return PickPackageFromMeta.pickPackageFromMeta(PickPackageFromMeta::pickVersionByVersionRange,
                preferredVersionSelectors, publishedBy, publishedByExclude, meta, spec, release);
    }

    private static @Nullable Map<String, @Nullable Object> pickLowest(
            @Nullable Map<String, PreferredVersions.Selector> preferredVersionSelectors, @Nullable Instant publishedBy,
            @Nullable Function<String, @Nullable Object> publishedByExclude, Map<String, @Nullable Object> meta,
            PickPackageFromMeta.RegistryPackageSpec spec, PnpmRelease release) {
        return PickPackageFromMeta.pickPackageFromMeta(PickPackageFromMeta::pickLowestVersionByVersionRange,
                preferredVersionSelectors, publishedBy, publishedByExclude, meta, spec, release);
    }

    static @Nullable Map<String, @Nullable Object> pickRespectingMinReleaseAge(
            PickerOptions pickerOpts, PickPackageFromMeta.RegistryPackageSpec spec,
            Map<String, @Nullable Object> meta) {
        return runPicker(pickerOpts, spec, targetSpec -> {
            // from 11.22.0 the lowest mature version where the resolution picks the lowest
            Map<String, @Nullable Object> highest = pickerOpts.isPickLowestVersion() &&
                                                    pickerOpts.getRelease().picksTheLowestMatureVersion() ?
                    pickLowest(pickerOpts.getPreferredVersionSelectors(), pickerOpts.getPublishedBy(),
                            pickerOpts.getPublishedByExclude(), meta, targetSpec, pickerOpts.getRelease()) :
                    pickHighest(pickerOpts.getPreferredVersionSelectors(), pickerOpts.getPublishedBy(),
                            pickerOpts.getPublishedByExclude(), meta, targetSpec, pickerOpts.getRelease());
            // before 11.1.3 a strict check leaves an immature range unpicked
            if (highest != null || pickerOpts.isStrictPublishedByCheck() &&
                                   !pickerOpts.getRelease().checksTheReleaseAgeAsAPolicy()) {
                return highest;
            }
            return pickLowest(pickerOpts.getPreferredVersionSelectors(), null, null, meta, targetSpec,
                    pickerOpts.getRelease());
        });
    }

    static @Nullable Map<String, @Nullable Object> pickIgnoringReleaseAge(
            PickerOptions pickerOpts, PickPackageFromMeta.RegistryPackageSpec spec,
            Map<String, @Nullable Object> meta) {
        return runPicker(pickerOpts, spec, targetSpec -> pickerOpts.isPickLowestVersion() ?
                pickLowest(pickerOpts.getPreferredVersionSelectors(), pickerOpts.getPublishedBy(),
                        pickerOpts.getPublishedByExclude(), meta, targetSpec, pickerOpts.getRelease()) :
                pickHighest(pickerOpts.getPreferredVersionSelectors(), pickerOpts.getPublishedBy(),
                        pickerOpts.getPublishedByExclude(), meta, targetSpec, pickerOpts.getRelease()));
    }

    static @Nullable Map<String, @Nullable Object> pickMatchingVersionFast(
            PickerOptions pickerOpts, PickPackageFromMeta.RegistryPackageSpec spec,
            Map<String, @Nullable Object> meta) {
        return pickerOpts.getPublishedBy() != null ? pickRespectingMinReleaseAge(pickerOpts, spec, meta) :
                pickIgnoringReleaseAge(pickerOpts, spec, meta);
    }

    /**
     * {@code pickPackage}'s result: the packument read and the version it selected.
     */
    @Value
    static class PickResult {
        Map<String, @Nullable Object> meta;

        @Nullable
        Map<String, @Nullable Object> pickedPackage;
    }

    /**
     * {@code pickPackage} of a lock-only install: the run's cache of each packument (full for an optional dependency),
     * the full one fetched when the abbreviated one says the package changed after the release-age cutoff.
     */
    static PickResult pickPackage(Map<String, Map<String, @Nullable Object>> metaCache, RegistryMetadata registry,
                                  PickPackageFromMeta.RegistryPackageSpec spec, PickerOptions pickerOpts,
                                  String registryUrl, boolean optional, boolean filterMetadata) {
        String name = spec.getName();
        if (name.contains("/") && name.charAt(0) != '@') {
            throw new PnpmError("INVALID_PACKAGE_NAME",
                    "Package name " + name + " is invalid, it should have a @scope");
        }
        boolean fullMetadata = optional || filterMetadata;
        PnpmRelease release = pickerOpts.getRelease();
        // from 11.9.0 the packuments the run holds are keyed by registry too, a filtered one apart
        // (PnpmRelease#keysThePackumentCacheByRegistry); the run keeps both keys, the release asked where they differ
        String nameKey = fullMetadata ? name + ":full" : name;
        String registryKey = getPkgMetaCacheKey(registryUrl, name, fullMetadata, filterMetadata);
        Map<String, @Nullable Object> heldByName = metaCache.get(nameKey);
        Map<String, @Nullable Object> heldByRegistry = metaCache.get(registryKey);
        Map<String, @Nullable Object> cached = heldByName == heldByRegistry ||
                                               !release.keysThePackumentCacheByRegistry() ? heldByName :
                heldByRegistry;
        CacheKeys cacheKey = new CacheKeys(nameKey, registryKey);
        if (cached != null) {
            // from 11.1.2 a held packument changed after the cutoff is fetched again in full
            if (!needsUpgradeForReleaseAge(spec, pickerOpts, cached) ||
                !release.upgradesAHeldPackumentForTheReleaseAge()) {
                return new PickResult(cached, pickMatchingVersionFinal(pickerOpts, spec, cached));
            }
            Map<String, @Nullable Object> upgraded = fetch(registry, registryUrl, spec.getName(), true, release);
            cacheKey.put(metaCache, upgraded);
            return new PickResult(upgraded, pickMatchingVersionFinal(pickerOpts, spec, upgraded));
        }
        Map<String, @Nullable Object> meta = fetch(registry, registryUrl, name, fullMetadata, release);
        if (pickerOpts.getPublishedBy() != null && !fullMetadata && meta.get("time") == null &&
            !excluded(pickerOpts, name)) {
            Instant modified = modifiedDate(meta);
            // before 11.2.0 a packument modified at the cutoff is fetched in full too
            if (modified == null || modified.isAfter(pickerOpts.getPublishedBy()) ||
                modified.equals(pickerOpts.getPublishedBy()) &&
                !release.takesAPackumentModifiedAtTheCutoffAsMature()) {
                meta = fetch(registry, registryUrl, name, true, release);
            }
        }
        if (filterMetadata) {
            meta = clearMeta(meta);
        }
        cacheKey.put(metaCache, meta);
        return new PickResult(meta, pickMatchingVersionFinal(pickerOpts, spec, meta));
    }

    // a packument's two cache keys: 11.2.2's name and 11.9.0's registry and name
    @Value
    private static class CacheKeys {
        String byName;
        String byRegistry;

        void put(Map<String, Map<String, @Nullable Object>> metaCache, Map<String, @Nullable Object> meta) {
            metaCache.put(byName, meta);
            metaCache.put(byRegistry, meta);
        }
    }

    // getPkgMetaCacheKey (11.9.0)
    static String getPkgMetaCacheKey(String registry, String pkgName, boolean fullMetadata, boolean filterMetadata) {
        String key = canonicalizeRegistry(registry) + "\0" + pkgName;
        if (!fullMetadata) {
            return key;
        }
        return filterMetadata ? key + ":full:filtered" : key + ":full";
    }

    private static String canonicalizeRegistry(String registry) {
        try {
            WhatwgUrl parsed = WhatwgUrl.parse(registry);
            String pathname = parsed.getPathname().endsWith("/") ? parsed.getPathname() : parsed.getPathname() + "/";
            return parsed.getOrigin() + pathname;
        } catch (IllegalArgumentException e) {
            return registry;
        }
    }

    // maybeUpgradeAbbreviatedMetaForReleaseAge's test: whether it fetches the held packument again in full
    private static boolean needsUpgradeForReleaseAge(PickPackageFromMeta.RegistryPackageSpec spec,
                                                     PickerOptions pickerOpts, Map<String, @Nullable Object> meta) {
        if (pickerOpts.getPublishedBy() == null || meta.get("time") != null || excluded(pickerOpts, spec.getName())) {
            return false;
        }
        Instant modified = modifiedDate(meta);
        // before 11.2.0 a packument modified at the cutoff is upgraded too
        return modified == null || !(modified.isBefore(pickerOpts.getPublishedBy()) ||
                                     modified.equals(pickerOpts.getPublishedBy()) &&
                                     pickerOpts.getRelease().takesAPackumentModifiedAtTheCutoffAsMature());
    }

    private static final String[] VERSION_FIELDS = {"name", "version", "bin", "directories", "devDependencies",
            "optionalDependencies", "dependencies", "peerDependencies", "dist", "engines", "peerDependenciesMeta",
            "cpu", "os", "libc", "deprecated", "bundleDependencies", "bundledDependencies", "hasInstallScript",
            "_npmUser"};

    // clearMeta: the abbreviated document's fields of each version, with libc
    private static Map<String, @Nullable Object> clearMeta(Map<String, @Nullable Object> pkg) {
        Map<String, @Nullable Object> versions = new LinkedHashMap<>();
        Map<String, @Nullable Object> all = ResolveDependencies.asMap(pkg.get("versions"));
        for (String version : Js.keys(all)) {
            Map<String, @Nullable Object> info = ResolveDependencies.asMap(all.get(version));
            Map<String, @Nullable Object> picked = new LinkedHashMap<>();
            for (String field : VERSION_FIELDS) {
                if (info.containsKey(field)) {
                    picked.put(field, info.get(field));
                }
            }
            versions.put(version, picked);
        }
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        out.put("name", pkg.get("name"));
        out.put("dist-tags", pkg.get("dist-tags"));
        out.put("versions", versions);
        out.put("time", pkg.get("time"));
        out.put("modified", pkg.get("modified"));
        return out;
    }

    private static boolean excluded(PickerOptions pickerOpts, String name) {
        return pickerOpts.getPublishedByExclude() != null &&
               Boolean.TRUE.equals(pickerOpts.getPublishedByExclude().apply(name));
    }

    private static @Nullable Instant modifiedDate(Map<String, @Nullable Object> meta) {
        Object modified = meta.get("modified");
        if (!Js.truthy(modified)) {
            return null;
        }
        double time = V8Date.dateParse(Js.string(modified));
        return Double.isNaN(time) ? null : Instant.ofEpochMilli((long) time);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable Object> fetch(RegistryMetadata registry, String registryUrl, String name,
                                                       boolean fullMetadata, PnpmRelease release) {
        String text = registry.packument(registryUrl, name, fullMetadata);
        if (text == null) {
            throw new PnpmError("FETCH_404", "GET " + PnpmError.requestUrl(registryUrl + encodeName(name), release) +
                                             ": Not Found - 404",
                    name + " is not in the npm registry, or you have no permission to fetch it.");
        }
        Object meta = Manifests.parseValue(text, registryUrl + name);
        if (!(meta instanceof Map)) {
            throw new CannotKnow("what pnpm makes of the packument of " + name + " (JSON, but not an object)");
        }
        return (Map<String, @Nullable Object>) meta;
    }

    // the registry path of a name, a scoped name's slash escaped
    private static String encodeName(String name) {
        return name.startsWith("@") ? name.replace("/", "%2F") : name;
    }

    /**
     * {@code pickMatchingVersionFinal}: without {@code time} in the packument and with
     * {@code minimumReleaseAgeIgnoreMissingTime}, the age policy is skipped (pnpm warns).
     */
    static @Nullable Map<String, @Nullable Object> pickMatchingVersionFinal(
            PickerOptions pickerOpts, PickPackageFromMeta.RegistryPackageSpec spec,
            Map<String, @Nullable Object> meta) {
        try {
            return pickMatchingVersionFast(pickerOpts, spec, meta);
        } catch (PnpmError e) {
            if (pickerOpts.isIgnoreMissingTimeField() && "ERR_PNPM_MISSING_TIME".equals(e.getCode())) {
                return pickMatchingVersionFast(pickerOpts.withPublishedBy(null).withPublishedByExclude(null), spec,
                        meta);
            }
            throw e;
        }
    }
}
