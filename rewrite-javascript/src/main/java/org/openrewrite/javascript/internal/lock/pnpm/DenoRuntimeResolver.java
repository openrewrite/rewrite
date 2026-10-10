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

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of @pnpm/engine.runtime.deno-resolver src/index.ts (pnpm 11.2.2): a {@code deno} dependency of
 * {@code runtime:} resolved from the npm registry's deno package and the GitHub release's checksums.
 */
final class DenoRuntimeResolver {

    private static final Pattern ASSET_REGEX = Pattern.compile(
            "^deno-(aarch64|x86_64)-(apple-darwin|unknown-linux-gnu|pc-windows-msvc)\\.zip\\.sha256sum$");
    private static final Pattern SHA256 = Pattern.compile("([a-f0-9]{64})", Pattern.CASE_INSENSITIVE);

    private DenoRuntimeResolver() {
    }

    /**
     * {@code resolveDenoRuntime}: null for any dependency but a {@code deno} of {@code runtime:}.
     */
    @SuppressWarnings("unchecked")
    static NpmResolver.@Nullable ResolveResult resolveDenoRuntime(RegistryMetadata fetch, NpmResolver resolveFromNpm,
                                                                 ResolveDependencies.WantedDependency wantedDependency,
                                                                 ResolveDependencies.@Nullable InfoFromLockfile current,
                                                                 @Nullable String update, @Nullable String hostOs) {
        String bareSpecifier = wantedDependency.getBareSpecifier();
        if (!"deno".equals(wantedDependency.getAlias()) || bareSpecifier == null ||
            !bareSpecifier.startsWith("runtime:")) {
            return null;
        }
        NpmResolver.ResolveResult result = new NpmResolver.ResolveResult();
        result.resolvedVia = "github.com/denoland/deno";
        if (current != null && update == null) {
            result.id = current.getPkgId();
            result.resolution = current.getResolution();
            return result;
        }
        String versionSpec = bareSpecifier.substring("runtime:".length());
        NpmResolver.ResolveResult npmResolution = resolveFromNpm.resolveNpm(
                new ResolveDependencies.WantedDependency(wantedDependency.getAlias(), versionSpec,
                        wantedDependency.isDev(), wantedDependency.isOptional()),
                new PackageRequester.RequestPackageOptions(), null);
        if (npmResolution == null) {
            throw new PnpmError("DENO_RESOLUTION_FAILURE", "Could not resolve Deno version specified as " +
                                                           versionSpec);
        }
        String version = Js.string(npmResolution.manifest == null ? null : npmResolution.manifest.get("version"));
        String releaseUrl = "https://api.github.com/repos/denoland/deno/releases/tags/v" + version;
        String release = fetch.document(releaseUrl);
        Object data = release == null ? null : Manifests.parseValue(release, releaseUrl);
        Object assetsData = data instanceof Map ? ((Map<String, @Nullable Object>) data).get("assets") : null;
        if (assetsData == null || assetsData == Js.UNDEFINED) {
            throw new PnpmError("DENO_MISSING_ASSETS", "No assets found for Deno v" + version);
        }
        if (!(assetsData instanceof List)) {
            throw new JsError(JsError.ERROR, "data.assets.map is not a function");
        }
        List<Map<String, @Nullable Object>> assets = new ArrayList<>();
        for (Object asset : (List<?>) assetsData) {
            Map<String, @Nullable Object> a = ResolveDependencies.asMap(asset);
            List<Object> targets = parseAssetName(Js.string(a.get("name")));
            if (targets == null) {
                continue;
            }
            String downloadUrl = Js.string(a.get("browser_download_url"));
            String sha256 = fetchSha256(fetch, downloadUrl);
            Map<String, @Nullable Object> resolution = new LinkedHashMap<>();
            resolution.put("type", "binary");
            resolution.put("url", downloadUrl.replaceFirst("\\.sha256sum$", ""));
            resolution.put("integrity", "sha256-" + Base64.getEncoder().encodeToString(ShasumsFile.hex(sha256)));
            resolution.put("bin", getDenoBinLocationForCurrentOS(
                    Js.string(((Map<String, @Nullable Object>) targets.get(0)).get("os"))));
            resolution.put("archive", "zip");
            Map<String, @Nullable Object> platformAsset = new LinkedHashMap<>();
            platformAsset.put("targets", targets);
            platformAsset.put("resolution", resolution);
            assets.add(platformAsset);
        }
        assets.sort((asset1, asset2) -> KeySorting.lexCompare(url(asset1), url(asset2)));
        result.id = "deno@runtime:" + version;
        result.normalizedBareSpecifier = "runtime:" + versionSpec;
        Map<String, @Nullable Object> manifest = new LinkedHashMap<>();
        manifest.put("name", "deno");
        manifest.put("version", version);
        manifest.put("bin", getDenoBinLocationForCurrentOS(hostOs));
        result.manifest = manifest;
        result.resolution.put("type", "variations");
        result.resolution.put("variants", new ArrayList<Object>(assets));
        return result;
    }

    @SuppressWarnings("unchecked")
    static String url(Map<String, @Nullable Object> asset) {
        return Js.string(((Map<String, @Nullable Object>) asset.get("resolution")).get("url"));
    }

    static @Nullable List<Object> parseAssetName(String name) {
        Matcher m = ASSET_REGEX.matcher(name);
        if (!m.matches()) {
            return null;
        }
        String os = "apple-darwin".equals(m.group(2)) ? "darwin" :
                "unknown-linux-gnu".equals(m.group(2)) ? "linux" : "win32";
        String cpu = "aarch64".equals(m.group(1)) ? "arm64" : "x64";
        List<Object> targets = new ArrayList<>();
        targets.add(target(os, cpu));
        if ("win32".equals(os) && "x64".equals(cpu)) {
            targets.add(target("win32", "arm64"));
        }
        return targets;
    }

    private static Map<String, @Nullable Object> target(String os, String cpu) {
        Map<String, @Nullable Object> target = new LinkedHashMap<>();
        target.put("os", os);
        target.put("cpu", cpu);
        return target;
    }

    private static String getDenoBinLocationForCurrentOS(@Nullable String platform) {
        return "win32".equals(platform) ? "deno.exe" : "deno";
    }

    private static String fetchSha256(RegistryMetadata fetch, String url) {
        String txt = fetch.document(url);
        if (txt == null) {
            throw new PnpmError("DENO_GITHUB_FAILURE", "Failed to GET sha256 at " + url);
        }
        Matcher m = SHA256.matcher(txt);
        if (!m.find()) {
            throw new PnpmError("DENO_PARSE_HASH", "No SHA256 in " + url);
        }
        return m.group(1).toLowerCase();
    }
}
