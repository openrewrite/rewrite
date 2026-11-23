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
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of @pnpm/patching.config (pnpm 11.2.2): groupPatchedDependencies, getPatchInfo, allPatchKeys and verifyPatches.
 */
final class PatchingConfig {

    private PatchingConfig() {
    }

    /**
     * {@code ExtendedPatchInfo}: a patch's hash and the {@code patchedDependencies} key that names it.
     */
    @Value
    static class PatchInfo {
        String hash;
        String key;
    }

    @Value
    static class PatchGroupRangeItem {
        String version;
        PatchInfo patch;
    }

    /**
     * {@code PatchGroup}: one package's patches by exact version, by range, and for every version.
     */
    static final class PatchGroup {
        final Map<String, PatchInfo> exact = new LinkedHashMap<>();
        final List<PatchGroupRangeItem> range = new ArrayList<>();
        @Nullable PatchInfo all;
    }

    static Map<String, PatchGroup> groupPatchedDependencies(Map<String, String> patchedDependencies) {
        return groupPatchedDependencies(patchedDependencies, PnpmRelease.PORTED);
    }

    static Map<String, PatchGroup> groupPatchedDependencies(Map<String, String> patchedDependencies,
                                                            PnpmRelease release) {
        Map<String, PatchGroup> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : patchedDependencies.entrySet()) {
            String key = entry.getKey();
            PatchInfo info = new PatchInfo(entry.getValue(), key);
            DepPath.DependencyPath parsed = DepPath.parse(key);
            String name = parsed.getName();
            if (Js.truthy(name) && Js.truthy(parsed.getVersion())) {
                result.computeIfAbsent(name, n -> new PatchGroup()).exact.put(parsed.getVersion(), info);
                continue;
            }
            String nonSemverVersion = parsed.getNonSemverVersion();
            if (Js.truthy(name) && Js.truthy(nonSemverVersion)) {
                if (ResolveDependencies.validRange(nonSemverVersion, release) == null) {
                    throw new PnpmError("PATCH_NON_SEMVER_RANGE", nonSemverVersion +
                                                                  " is not a valid semantic version range.");
                }
                PatchGroup group = result.computeIfAbsent(name, n -> new PatchGroup());
                if ("*".equals(nonSemverVersion.trim())) {
                    group.all = info;
                } else {
                    group.range.add(new PatchGroupRangeItem(nonSemverVersion, info));
                }
                continue;
            }
            result.computeIfAbsent(key, n -> new PatchGroup()).all = info;
        }
        return result;
    }

    static @Nullable PatchInfo getPatchInfo(@Nullable Map<String, PatchGroup> patchFileGroups, String pkgName,
                                            String pkgVersion) {
        return getPatchInfo(patchFileGroups, pkgName, pkgVersion, PnpmRelease.PORTED);
    }

    /**
     * {@link #getPatchInfo(Map, String, String)} over the node-semver {@code release} bundles.
     */
    static @Nullable PatchInfo getPatchInfo(@Nullable Map<String, PatchGroup> patchFileGroups, String pkgName,
                                            String pkgVersion, PnpmRelease release) {
        PatchGroup group = patchFileGroups == null ? null : patchFileGroups.get(pkgName);
        if (group == null) {
            return null;
        }
        PatchInfo exactVersion = group.exact.get(pkgVersion);
        if (exactVersion != null) {
            return exactVersion;
        }
        List<PatchGroupRangeItem> satisfied = new ArrayList<>();
        for (PatchGroupRangeItem item : group.range) {
            if (satisfies(pkgVersion, item.getVersion(), release)) {
                satisfied.add(item);
            }
        }
        if (satisfied.size() > 1) {
            List<String> versions = new ArrayList<>();
            for (PatchGroupRangeItem item : satisfied) {
                versions.add(item.getVersion());
            }
            String pkgId = pkgName + "@" + pkgVersion;
            throw new PnpmError("PATCH_KEY_CONFLICT", "Unable to choose between " + satisfied.size() +
                    " version ranges to patch " + pkgId + ": " + String.join(", ", versions),
                    "Explicitly set the exact version (" + pkgId + ") to resolve conflict");
        }
        if (satisfied.size() == 1) {
            return satisfied.get(0).getPatch();
        }
        return group.all;
    }

    static List<String> allPatchKeys(Map<String, PatchGroup> patchedDependencies) {
        List<String> keys = new ArrayList<>();
        for (PatchGroup group : patchedDependencies.values()) {
            for (PatchInfo patch : group.exact.values()) {
                keys.add(patch.getKey());
            }
            for (PatchGroupRangeItem item : group.range) {
                keys.add(item.getPatch().getKey());
            }
            if (group.all != null) {
                keys.add(group.all.getKey());
            }
        }
        return keys;
    }

    static void verifyPatches(Map<String, PatchGroup> patchedDependencies, Set<String> appliedPatches,
                              boolean allowUnusedPatches) {
        List<String> unusedPatches = new ArrayList<>();
        for (String patchKey : allPatchKeys(patchedDependencies)) {
            if (!appliedPatches.contains(patchKey)) {
                unusedPatches.add(patchKey);
            }
        }
        if (unusedPatches.isEmpty() || allowUnusedPatches) {
            return;
        }
        throw new PnpmError("UNUSED_PATCH", "The following patches were not used: " +
                                            String.join(", ", unusedPatches),
                "Either remove them from \"patchedDependencies\" or update them to match packages in your " +
                "dependencies.");
    }

    private static boolean satisfies(String version, String range, PnpmRelease release) {
        try {
            return release.underSemver(semver -> NodeSemver.satisfies(version, range,
                    NodeSemver.Options.DEFAULT.release(semver)));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
