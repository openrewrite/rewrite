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
            PickPackageFromMeta.RegistryPackageSpec spec) {
        return PickPackageFromMeta.pickPackageFromMeta(PickPackageFromMeta::pickVersionByVersionRange,
                preferredVersionSelectors, publishedBy, publishedByExclude, meta, spec);
    }

    private static @Nullable Map<String, @Nullable Object> pickLowest(
            @Nullable Map<String, PreferredVersions.Selector> preferredVersionSelectors, @Nullable Instant publishedBy,
            @Nullable Function<String, @Nullable Object> publishedByExclude, Map<String, @Nullable Object> meta,
            PickPackageFromMeta.RegistryPackageSpec spec) {
        return PickPackageFromMeta.pickPackageFromMeta(PickPackageFromMeta::pickLowestVersionByVersionRange,
                preferredVersionSelectors, publishedBy, publishedByExclude, meta, spec);
    }

    static @Nullable Map<String, @Nullable Object> pickRespectingMinReleaseAge(
            PickerOptions pickerOpts, PickPackageFromMeta.RegistryPackageSpec spec,
            Map<String, @Nullable Object> meta) {
        return runPicker(pickerOpts, spec, targetSpec -> {
            Map<String, @Nullable Object> highest = pickHighest(pickerOpts.getPreferredVersionSelectors(),
                    pickerOpts.getPublishedBy(), pickerOpts.getPublishedByExclude(), meta, targetSpec);
            if (highest != null) {
                return highest;
            }
            return pickLowest(pickerOpts.getPreferredVersionSelectors(), null, null, meta, targetSpec);
        });
    }

    static @Nullable Map<String, @Nullable Object> pickIgnoringReleaseAge(
            PickerOptions pickerOpts, PickPackageFromMeta.RegistryPackageSpec spec,
            Map<String, @Nullable Object> meta) {
        return runPicker(pickerOpts, spec, targetSpec -> pickerOpts.isPickLowestVersion() ?
                pickLowest(pickerOpts.getPreferredVersionSelectors(), pickerOpts.getPublishedBy(),
                        pickerOpts.getPublishedByExclude(), meta, targetSpec) :
                pickHighest(pickerOpts.getPreferredVersionSelectors(), pickerOpts.getPublishedBy(),
                        pickerOpts.getPublishedByExclude(), meta, targetSpec));
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
        String cacheKey = fullMetadata ? name + ":full" : name;
        Map<String, @Nullable Object> cached = metaCache.get(cacheKey);
        if (cached != null) {
            Map<String, @Nullable Object> upgraded = maybeUpgradeForReleaseAge(registry, spec, pickerOpts, registryUrl,
                    cached);
            if (upgraded != cached) {
                metaCache.put(cacheKey, upgraded);
            }
            return new PickResult(upgraded, pickMatchingVersionFinal(pickerOpts, spec, upgraded));
        }
        Map<String, @Nullable Object> meta = fetch(registry, registryUrl, name, fullMetadata);
        if (pickerOpts.getPublishedBy() != null && !fullMetadata && meta.get("time") == null &&
            !excluded(pickerOpts, name)) {
            Instant modified = modifiedDate(meta);
            if (modified == null || modified.isAfter(pickerOpts.getPublishedBy())) {
                meta = fetch(registry, registryUrl, name, true);
            }
        }
        if (filterMetadata) {
            meta = clearMeta(meta);
        }
        metaCache.put(cacheKey, meta);
        return new PickResult(meta, pickMatchingVersionFinal(pickerOpts, spec, meta));
    }

    private static Map<String, @Nullable Object> maybeUpgradeForReleaseAge(
            RegistryMetadata registry, PickPackageFromMeta.RegistryPackageSpec spec, PickerOptions pickerOpts,
            String registryUrl, Map<String, @Nullable Object> meta) {
        if (pickerOpts.getPublishedBy() == null || meta.get("time") != null || excluded(pickerOpts, spec.getName())) {
            return meta;
        }
        Instant modified = modifiedDate(meta);
        if (modified != null && !modified.isAfter(pickerOpts.getPublishedBy())) {
            return meta;
        }
        return fetch(registry, registryUrl, spec.getName(), true);
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
                                                       boolean fullMetadata) {
        String text = registry.packument(registryUrl, name, fullMetadata);
        if (text == null) {
            throw new PnpmError("FETCH_404", "GET " + registryUrl + encodeName(name) + ": Not Found - 404",
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
