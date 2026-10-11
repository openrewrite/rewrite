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
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of @pnpm/engine.runtime.node-resolver (pnpm 11.2.2): a {@code node} dependency of
 * {@code runtime:} resolved from the Node.js release index and each release's SHASUMS256.txt.
 */
final class NodeRuntimeResolver {

    static final String DEFAULT_NODE_MIRROR_BASE_URL = "https://nodejs.org/download/release/";
    static final String UNOFFICIAL_NODE_MIRROR_BASE_URL = "https://unofficial-builds.nodejs.org/download/release/";

    private static final List<String> RELEASE_CHANNELS = Arrays.asList("nightly", "rc", "test", "v8-canary",
            "release");
    private static final Pattern STABLE = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");
    private static final Pattern PRERELEASE_CHANNEL =
            Pattern.compile("^\\d+\\.\\d+\\.\\d+-(nightly|rc|test|v8-canary)");

    private NodeRuntimeResolver() {
    }

    /**
     * {@code resolveNodeRuntime}: null for any dependency but a {@code node} of {@code runtime:}.
     */
    static NpmResolver.@Nullable ResolveResult resolveNodeRuntime(RegistryMetadata fetch,
                                                                 ResolveDependencies.WantedDependency wantedDependency,
                                                                 ResolveDependencies.@Nullable InfoFromLockfile current,
                                                                 @Nullable String update,
                                                                 @Nullable Map<String, @Nullable Object> mirrors,
                                                                 @Nullable String hostOs) {
        String bareSpecifier = wantedDependency.getBareSpecifier();
        if (!"node".equals(wantedDependency.getAlias()) || bareSpecifier == null ||
            !bareSpecifier.startsWith("runtime:")) {
            return null;
        }
        NpmResolver.ResolveResult result = new NpmResolver.ResolveResult();
        result.resolvedVia = "nodejs.org";
        if (current != null && current.getResolution() != null && update == null) {
            result.id = current.getPkgId();
            result.resolution = current.getResolution();
            return result;
        }
        String versionSpec = bareSpecifier.substring("runtime:".length());
        String[] parsed = parseNodeSpecifier(versionSpec);
        String nodeMirrorBaseUrl = getNodeMirror(mirrors, parsed[0]);
        String version = resolveNodeVersion(fetch, parsed[1], nodeMirrorBaseUrl);
        if (version == null) {
            throw new PnpmError("NODEJS_VERSION_NOT_FOUND", "Could not find a Node.js version that satisfies " +
                                                            versionSpec);
        }
        List<Object> variants = readNodeAssets(fetch, nodeMirrorBaseUrl, version);
        String range = version.equals(versionSpec) ? version : "^" + version;
        result.id = "node@runtime:" + version;
        result.normalizedBareSpecifier = "runtime:" + range;
        Map<String, @Nullable Object> manifest = new LinkedHashMap<>();
        manifest.put("name", "node");
        manifest.put("version", version);
        manifest.put("bin", nodeBins(hostOs));
        result.manifest = manifest;
        result.resolution.put("type", "variations");
        result.resolution.put("variants", variants);
        return result;
    }

    private static List<Object> readNodeAssets(RegistryMetadata fetch, String nodeMirrorBaseUrl, String version) {
        List<Object> assets = readNodeAssetsFromMirror(fetch, nodeMirrorBaseUrl, version, false);
        if (DEFAULT_NODE_MIRROR_BASE_URL.equals(nodeMirrorBaseUrl)) {
            try {
                assets.addAll(readNodeAssetsFromMirror(fetch, UNOFFICIAL_NODE_MIRROR_BASE_URL, version, true));
            } catch (PnpmError e) {
                // musl builds are missing for some releases
            }
        }
        return assets;
    }

    private static List<Object> readNodeAssetsFromMirror(RegistryMetadata fetch, String nodeMirrorBaseUrl,
                                                         String version, boolean muslOnly) {
        String integritiesFileUrl = nodeMirrorBaseUrl + "v" + version + "/SHASUMS256.txt";
        Pattern pattern = Pattern.compile("^node-v" + Pattern.quote(version) +
                                          "-([^-.]+)-([^.-]+)(-musl)?\\.(?:tar\\.gz|zip)$");
        List<Object> assets = new ArrayList<>();
        for (ShasumsFile.Item item : ShasumsFile.fetchShasumsFile(fetch, integritiesFileUrl)) {
            String fileName = item.getFileName() == null ? "undefined" : item.getFileName();
            Matcher match = pattern.matcher(fileName);
            if (!match.matches()) {
                continue;
            }
            String platform = match.group(1);
            String arch = match.group(2);
            if ("win".equals(platform)) {
                platform = "win32";
            }
            boolean isMusl = match.group(3) != null;
            if (muslOnly && !isMusl) {
                continue;
            }
            boolean windows = "win32".equals(platform);
            String basename = "node-v" + version + "-" + (windows ? "win" : platform) + "-" +
                              getNormalizedArch(platform, arch, version) + (isMusl ? "-musl" : "");
            String extname = windows ? ".zip" : ".tar.gz";
            Map<String, @Nullable Object> resolution = new LinkedHashMap<>();
            resolution.put("type", "binary");
            resolution.put("archive", ".zip".equals(extname) ? "zip" : "tarball");
            resolution.put("bin", nodeBins(platform));
            resolution.put("integrity", item.getIntegrity());
            resolution.put("url", nodeMirrorBaseUrl + "v" + version + "/" + basename + extname);
            if (".zip".equals(extname)) {
                resolution.put("prefix", basename);
            }
            Map<String, @Nullable Object> target = new LinkedHashMap<>();
            target.put("os", platform);
            target.put("cpu", arch);
            if (isMusl) {
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

    private static @Nullable String resolveNodeVersion(RegistryMetadata fetch, String versionSpec,
                                                       String nodeMirrorBaseUrl) {
        String url = nodeMirrorBaseUrl + "index.json";
        String index = fetch.document(url);
        Object parsed = index == null ? null : Manifests.parseValue(index, url);
        if (!(parsed instanceof List)) {
            throw new JsError(JsError.ERROR, "the Node.js release index " + url + " is not a JSON array");
        }
        List<String> all = new ArrayList<>();
        List<Object> lts = new ArrayList<>();
        for (Object entry : (List<?>) parsed) {
            Map<String, @Nullable Object> release = ResolveDependencies.asMap(entry);
            all.add(Js.string(release.get("version")).substring(1));
            lts.add(release.get("lts"));
        }
        if ("latest".equals(versionSpec)) {
            return all.isEmpty() ? null : all.get(0);
        }
        List<String> versions = new ArrayList<>();
        String versionRange = versionSpec;
        String selector = Verification.versionSelectorType(versionSpec);
        if ("lts".equals(versionSpec) || "tag".equals(selector)) {
            for (int i = 0; i < all.size(); i++) {
                Object l = lts.get(i);
                boolean wanted = "lts".equals(versionSpec) ? !Boolean.FALSE.equals(l) :
                        l instanceof String && ((String) l).toLowerCase().equals(versionSpec.toLowerCase());
                if (wanted) {
                    versions.add(all.get(i));
                }
            }
            versionRange = "*";
        } else {
            versions.addAll(all);
        }
        return NodeSemver.maxSatisfying(versions, versionRange, NodeSemver.Options.LOOSE_INCLUDE_PRERELEASE);
    }

    // {releaseChannel, versionSpecifier}
    static String[] parseNodeSpecifier(String specifier) {
        if (specifier.contains("/")) {
            String[] parts = specifier.split("/", -1);
            if (!RELEASE_CHANNELS.contains(parts[0])) {
                throw new PnpmError("INVALID_NODE_RELEASE_CHANNEL", "\"" + parts[0] +
                                                                    "\" is not a valid Node.js release channel");
            }
            return new String[]{parts[0], parts[1]};
        }
        Matcher prerelease = PRERELEASE_CHANNEL.matcher(specifier);
        if (prerelease.find()) {
            return new String[]{prerelease.group(1), specifier};
        }
        if (STABLE.matcher(specifier).matches()) {
            return new String[]{"release", specifier};
        }
        if (RELEASE_CHANNELS.contains(specifier)) {
            return new String[]{specifier, "latest"};
        }
        if ("lts".equals(specifier) || "latest".equals(specifier)) {
            return new String[]{"release", specifier};
        }
        return new String[]{"release", specifier};
    }

    static String getNodeMirror(@Nullable Map<String, @Nullable Object> mirrors, String releaseChannel) {
        Object mirror = mirrors == null ? null : mirrors.get(releaseChannel);
        String nodeMirror = mirror != null ? Js.string(mirror) : "https://nodejs.org/download/" + releaseChannel + "/";
        return nodeMirror.endsWith("/") ? nodeMirror : nodeMirror + "/";
    }

    static String getNormalizedArch(String platform, String arch, String nodeVersion) {
        String major = nodeVersion.split("\\.", -1)[0];
        if ("darwin".equals(platform) && "arm64".equals(arch) && major.matches("\\d+") &&
            Integer.parseInt(major) < 16) {
            return "x64";
        }
        if ("win32".equals(platform) && "ia32".equals(arch)) {
            return "x86";
        }
        if ("arm".equals(arch)) {
            return "armv7l";
        }
        return arch;
    }

    private static Map<String, @Nullable Object> nodeBins(@Nullable String platform) {
        Map<String, @Nullable Object> bins = new LinkedHashMap<>();
        bins.put("node", "win32".equals(platform) ? "node.exe" : "bin/node");
        return bins;
    }
}
