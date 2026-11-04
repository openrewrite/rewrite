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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of @pnpm/engine.runtime.bun-resolver src/index.ts (pnpm 11.2.2): a {@code bun} dependency of
 * {@code runtime:} resolved from the npm registry's bun package and the GitHub release's SHASUMS256.txt.
 */
final class BunRuntimeResolver {

    private static final Pattern ASSET = Pattern.compile("^bun-([^-.]+)-([^-.]+)(-musl)?\\.zip$");

    private BunRuntimeResolver() {
    }

    /**
     * {@code resolveBunRuntime}: null for any dependency but a {@code bun} of {@code runtime:}.
     */
    static NpmResolver.@Nullable ResolveResult resolveBunRuntime(RegistryMetadata fetch, NpmResolver resolveFromNpm,
                                                                ResolveDependencies.WantedDependency wantedDependency,
                                                                ResolveDependencies.@Nullable InfoFromLockfile current,
                                                                @Nullable String update, @Nullable String hostOs) {
        String bareSpecifier = wantedDependency.getBareSpecifier();
        if (!"bun".equals(wantedDependency.getAlias()) || bareSpecifier == null ||
            !bareSpecifier.startsWith("runtime:")) {
            return null;
        }
        NpmResolver.ResolveResult result = new NpmResolver.ResolveResult();
        result.resolvedVia = "github.com/oven-sh/bun";
        if (current != null && update == null) {
            result.id = current.getPkgId();
            result.resolution = current.getResolution();
            return result;
        }
        String versionSpec = NodeRuntimeResolver.runtimeSpec(bareSpecifier.substring("runtime:".length()),
                resolveFromNpm.getRelease());
        NpmResolver.ResolveResult npmResolution = resolveFromNpm.resolveNpm(
                new ResolveDependencies.WantedDependency(wantedDependency.getAlias(), versionSpec,
                        wantedDependency.isDev(), wantedDependency.isOptional()),
                new PackageRequester.RequestPackageOptions(), null);
        if (npmResolution == null) {
            throw new PnpmError("BUN_RESOLUTION_FAILURE", "Could not resolve Bun version specified as " +
                                                          versionSpec);
        }
        String version = Js.string(npmResolution.manifest == null ? null : npmResolution.manifest.get("version"));
        List<Map<String, @Nullable Object>> assets = readBunAssets(fetch, version);
        assets.sort((asset1, asset2) -> KeySorting.lexCompare(DenoRuntimeResolver.url(asset1),
                DenoRuntimeResolver.url(asset2)));
        result.id = "bun@runtime:" + version;
        result.normalizedBareSpecifier = "runtime:" + versionSpec;
        Map<String, @Nullable Object> manifest = new LinkedHashMap<>();
        manifest.put("name", "bun");
        manifest.put("version", version);
        manifest.put("bin", getBunBinLocationForCurrentOS(hostOs));
        result.manifest = manifest;
        result.resolution.put("type", "variations");
        result.resolution.put("variants", new ArrayList<Object>(assets));
        return result;
    }

    private static List<Map<String, @Nullable Object>> readBunAssets(RegistryMetadata fetch, String version) {
        String integritiesFileUrl = "https://github.com/oven-sh/bun/releases/download/bun-v" + version +
                                    "/SHASUMS256.txt";
        List<Map<String, @Nullable Object>> assets = new ArrayList<>();
        for (ShasumsFile.Item item : ShasumsFile.fetchShasumsFile(fetch, integritiesFileUrl)) {
            String fileName = item.getFileName() == null ? "undefined" : item.getFileName();
            Matcher match = ASSET.matcher(fileName);
            if (!match.matches()) {
                continue;
            }
            String platform = "windows".equals(match.group(1)) ? "win32" : match.group(1);
            String arch = "aarch64".equals(match.group(2)) ? "arm64" : match.group(2);
            Map<String, @Nullable Object> resolution = new LinkedHashMap<>();
            resolution.put("type", "binary");
            resolution.put("archive", "zip");
            resolution.put("bin", getBunBinLocationForCurrentOS(platform));
            resolution.put("integrity", item.getIntegrity());
            resolution.put("url", "https://github.com/oven-sh/bun/releases/download/bun-v" + version + "/" +
                                  fileName);
            resolution.put("prefix", fileName.replaceFirst("\\.zip$", ""));
            Map<String, @Nullable Object> target = new LinkedHashMap<>();
            target.put("os", platform);
            target.put("cpu", arch);
            if (match.group(3) != null) {
                target.put("libc", "musl");
            }
            List<Object> targets = new ArrayList<>();
            targets.add(target);
            Map<String, @Nullable Object> asset = new LinkedHashMap<>();
            asset.put("targets", targets);
            asset.put("resolution", resolution);
            assets.add(asset);
        }
        return assets;
    }

    private static String getBunBinLocationForCurrentOS(@Nullable String platform) {
        return "win32".equals(platform) ? "bun.exe" : "bun";
    }
}
