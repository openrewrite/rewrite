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
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.RootRelativePath;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of @pnpm/lockfile.settings-checker src/getOutdatedLockfileSetting.ts and src/calcPatchHashes.ts
 * (pnpm 11.2.2): which lock-level setting differs from the configuration, forcing a full resolution.
 */
final class SettingsChecker {

    private SettingsChecker() {
    }

    /**
     * What the configuration says for each setting the lock records.
     */
    @Value
    @Builder
    static class Settings {
        Map<String, @Nullable Map<String, @Nullable Object>> catalogs;

        @Nullable
        Map<String, @Nullable Object> overrides;

        @Nullable
        String packageExtensionsChecksum;

        @Nullable
        Map<String, @Nullable Object> patchedDependencies;

        @Nullable
        List<@Nullable Object> ignoredOptionalDependencies;

        @Nullable
        Object autoInstallPeers;

        @Nullable
        Object dedupePeers;

        @Nullable
        Object excludeLinksFromLockfile;

        @Nullable
        Object peersSuffixMaxLength;

        @Nullable
        String pnpmfileChecksum;

        @Nullable
        Object injectWorkspacePackages;
    }

    /**
     * {@code getOutdatedLockfileSetting}: the name of the first setting that differs, null when none does.
     */
    static @Nullable String getOutdatedLockfileSetting(LockfileObject lockfile, Settings settings) {
        return getOutdatedLockfileSetting(lockfile, settings, PnpmRelease.PORTED);
    }

    /**
     * {@link #getOutdatedLockfileSetting(LockfileObject, Settings)} as {@code release} compares the catalogs'
     * specifiers ({@link Verification#specifiersAreEqual}).
     */
    @SuppressWarnings("unchecked")
    static @Nullable String getOutdatedLockfileSetting(LockfileObject lockfile, Settings settings,
                                                       PnpmRelease release) {
        if (!Verification.allCatalogsAreUpToDate(settings.getCatalogs(), lockfile.getCatalogs(), release)) {
            return "catalogs";
        }
        if (!Js.equals(orEmptyObject(lockfile.getOverrides()), orEmptyObject(settings.getOverrides()))) {
            return "overrides";
        }
        if (!Js.strictEquals(undefinedToNull(lockfile.getPackageExtensionsChecksum()),
                settings.getPackageExtensionsChecksum())) {
            return "packageExtensionsChecksum";
        }
        if (!Js.equals(sorted(lockfile.getIgnoredOptionalDependencies()),
                sorted(settings.getIgnoredOptionalDependencies()))) {
            return "ignoredOptionalDependencies";
        }
        if (!Js.equals(orEmptyObject(lockfile.getPatchedDependencies()),
                orEmptyObject(settings.getPatchedDependencies()))) {
            return "patchedDependencies";
        }
        Object lockSettings = lockfile.getSettings();
        Object autoInstallPeers = setting(lockSettings, "autoInstallPeers");
        if (autoInstallPeers != null && !Js.strictEquals(autoInstallPeers, settings.getAutoInstallPeers())) {
            return "settings.autoInstallPeers";
        }
        if (Js.truthy(setting(lockSettings, "dedupePeers")) != Js.truthy(settings.getDedupePeers())) {
            return "settings.dedupePeers";
        }
        Object excludeLinks = setting(lockSettings, "excludeLinksFromLockfile");
        if (excludeLinks != null && !Js.strictEquals(excludeLinks, settings.getExcludeLinksFromLockfile())) {
            return "settings.excludeLinksFromLockfile";
        }
        Object peersSuffixMaxLength = setting(lockSettings, "peersSuffixMaxLength");
        if (peersSuffixMaxLength != null &&
            !Js.strictEquals(peersSuffixMaxLength, settings.getPeersSuffixMaxLength()) ||
            peersSuffixMaxLength == null && !Js.strictEquals(settings.getPeersSuffixMaxLength(), 1000.0)) {
            return "settings.peersSuffixMaxLength";
        }
        if (!Js.strictEquals(undefinedToNull(lockfile.getPnpmfileChecksum()), settings.getPnpmfileChecksum())) {
            return "pnpmfileChecksum";
        }
        if (Js.truthy(setting(lockSettings, "injectWorkspacePackages")) !=
            Js.truthy(settings.getInjectWorkspacePackages())) {
            return "settings.injectWorkspacePackages";
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Object setting(@Nullable Object settings, String name) {
        if (!(settings instanceof Map)) {
            return null;
        }
        return undefinedToNull(((Map<String, @Nullable Object>) settings).get(name));
    }

    private static Object orEmptyObject(@Nullable Object value) {
        return value == null || value == Js.UNDEFINED ? new LinkedHashMap<String, Object>() : value;
    }

    private static @Nullable Object undefinedToNull(@Nullable Object value) {
        return value == Js.UNDEFINED ? null : value;
    }

    // value?.sort() ?? []: the array sorted in place by its elements' strings
    @SuppressWarnings("unchecked")
    private static Object sorted(@Nullable Object value) {
        if (value == null || value == Js.UNDEFINED) {
            return new ArrayList<>();
        }
        if (!(value instanceof List)) {
            throw new CannotKnow("what pnpm makes of ignoredOptionalDependencies (" + Js.typeOf(value) +
                                 ", where pnpm writes an array)");
        }
        List<@Nullable Object> list = (List<@Nullable Object>) value;
        list.sort((a, b) -> Js.string(a).compareTo(Js.string(b)));
        return list;
    }

    /**
     * {@code calcPatchHashes}: each patch file's sha256 (hex), CRLF read as LF, from the files by path relative to the
     * lock's directory.
     */
    static Map<String, @Nullable Object> calcPatchHashes(Map<String, String> patches, Map<String, String> files) {
        Map<String, @Nullable Object> hashes = new LinkedHashMap<>();
        for (Map.Entry<String, String> patch : patches.entrySet()) {
            String path = RootRelativePath.normalize(patch.getValue());
            String content = RootRelativePath.climbsAboveRoot(path) ? null : files.get(path);
            if (content == null) {
                throw new CannotKnow("the patch file " + patch.getValue() + " (not among the files the host gave)");
            }
            hashes.put(patch.getKey(), DepPath.createHexHash(content.replace("\r\n", "\n")));
        }
        return hashes;
    }
}
