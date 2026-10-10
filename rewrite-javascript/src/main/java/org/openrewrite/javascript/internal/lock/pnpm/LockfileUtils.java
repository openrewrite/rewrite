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
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.WhatwgUrl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Port of @pnpm/lockfile.utils src/toLockfileResolution.ts (pnpm 11.2.2) with get-npm-tarball-url 2.1.0.
 */
final class LockfileUtils {

    private LockfileUtils() {
    }

    /**
     * The resolution a lock records: the integrity alone where the tarball URL follows from the registry.
     */
    static Map<String, @Nullable Object> toLockfileResolution(String name, String version,
                                                              Map<String, @Nullable Object> resolution,
                                                              String registry, boolean lockfileIncludeTarballUrl) {
        Object integrity = resolution.get("integrity");
        boolean typed = resolution.containsKey("type") && resolution.get("type") != Js.UNDEFINED;
        if (typed || !Js.truthy(integrity)) {
            return resolution;
        }
        Object tarball = resolution.get("tarball");
        if (tarball == null || tarball == Js.UNDEFINED) {
            Map<String, @Nullable Object> out = new LinkedHashMap<>();
            out.put("integrity", integrity);
            return out;
        }
        if (!(tarball instanceof String)) {
            throw new CannotKnow("what pnpm makes of the tarball " + Js.string(tarball) + " (" + Js.typeOf(tarball) +
                                 ", where pnpm reads a string)");
        }
        String url = (String) tarball;
        boolean gitHosted = Boolean.TRUE.equals(resolution.get("gitHosted")) || isGitHostedTarballUrl(url);
        if (lockfileIncludeTarballUrl || url.startsWith("file:") || gitHosted) {
            return preservingGitHosted(integrity, url, gitHosted);
        }
        String expectedTarball = getNpmTarballUrl(name, version, registry);
        String actualTarball = url.replace("%2f", "/");
        if (!Objects.equals(removeProtocol(expectedTarball), removeProtocol(actualTarball))) {
            return preservingGitHosted(integrity, url, gitHosted);
        }
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        out.put("integrity", integrity);
        return out;
    }

    /**
     * {@code pkgSnapshotToResolution}: the resolution a snapshot records, its tarball URL filled in for a registry
     * package.
     */
    static Map<String, @Nullable Object> pkgSnapshotToResolution(String depPath, PackageSnapshot pkgSnapshot,
                                                                 Map<String, String> registries) {
        Map<String, @Nullable Object> resolution = pkgSnapshot.getResolution();
        if (resolution == null) {
            resolution = new LinkedHashMap<>();
        }
        Object tarball = resolution.get("tarball");
        if (Js.truthy(resolution.get("type")) || tarball instanceof String && ((String) tarball).startsWith("file:") ||
            Boolean.TRUE.equals(resolution.get("gitHosted"))) {
            return resolution;
        }
        DepPath.DependencyPath parsed = DepPath.parse(depPath);
        String nonSemverVersion = parsed.getNonSemverVersion();
        if (nonSemverVersion != null && nonSemverVersion.startsWith("file:")) {
            Map<String, @Nullable Object> out = new LinkedHashMap<>(resolution);
            out.put("tarball", nonSemverVersion);
            return out;
        }
        String name = parsed.getName();
        String version = pkgSnapshot.getVersion() != null ? pkgSnapshot.getVersion() : parsed.getVersion();
        String registry = "";
        if (name != null && !name.isEmpty() && name.charAt(0) == '@') {
            String scoped = registries.get(name.split("/", -1)[0]);
            registry = scoped == null ? "" : scoped;
        }
        if (registry.isEmpty()) {
            registry = registries.get("default");
        }
        String url;
        if (!Js.truthy(tarball)) {
            if (name == null || version == null) {
                throw new JsError(JsError.ERROR, "Couldn't get tarball URL from dependency path " + depPath);
            }
            url = getNpmTarballUrl(name, version, registry);
        } else {
            WhatwgUrl parsedUrl = WhatwgUrl.tryParse(Js.string(tarball), registry.endsWith("/") ? registry :
                    registry + "/");
            if (parsedUrl == null) {
                throw new JsError(JsError.TYPE_ERROR, "Invalid URL");
            }
            url = parsedUrl.toString();
        }
        Map<String, @Nullable Object> out = new LinkedHashMap<>(resolution);
        out.put("tarball", url);
        return out;
    }

    /**
     * get-npm-tarball-url: the registry's URL of a version's tarball, build metadata dropped.
     */
    static String getNpmTarballUrl(String pkgName, String pkgVersion, @Nullable String registry) {
        String base = registry != null && !registry.isEmpty() ?
                registry.endsWith("/") ? registry : registry + "/" :
                "https://registry.npmjs.org/";
        int plus = pkgVersion.indexOf('+');
        String version = plus == -1 ? pkgVersion : pkgVersion.substring(0, plus);
        String scopelessName = !pkgName.isEmpty() && pkgName.charAt(0) == '@' ? scopelessName(pkgName) : pkgName;
        return base + pkgName + "/-/" + scopelessName + "-" + version + ".tgz";
    }

    // name.split('/')[1]
    private static String scopelessName(String name) {
        String[] parts = name.split("/", -1);
        return parts.length > 1 ? parts[1] : "undefined";
    }

    private static Map<String, @Nullable Object> preservingGitHosted(Object integrity, String tarball,
                                                                     boolean gitHosted) {
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        out.put("integrity", integrity);
        out.put("tarball", tarball);
        if (gitHosted) {
            out.put("gitHosted", true);
        }
        return out;
    }

    private static boolean isGitHostedTarballUrl(String url) {
        return (url.startsWith("https://codeload.github.com/") || url.startsWith("https://bitbucket.org/") ||
                url.startsWith("https://gitlab.com/")) && url.contains("tar.gz");
    }

    // url.split('://')[1]
    private static @Nullable String removeProtocol(String url) {
        int at = url.indexOf("://");
        if (at == -1) {
            return null;
        }
        int next = url.indexOf("://", at + 3);
        return next == -1 ? url.substring(at + 3) : url.substring(at + 3, next);
    }
}
