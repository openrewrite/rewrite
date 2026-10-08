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
import org.openrewrite.semver.NodeSemver;

import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

/**
 * Port of @pnpm/resolving.npm-resolver src/pickPackage.ts (pnpm 11.2.2), the pickers {@code pickPackage} runs over a
 * packument once it has one: {@code minimumReleaseAge} respected (the highest mature version, else the lowest of
 * any age) or not ({@code resolutionMode}'s lowest or highest), with the {@code latest} tag as a second candidate.
 * Fetching the packument (abbreviated, or full when the age policy needs {@code time}) is the resolver's.
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
