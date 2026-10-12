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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Port of @pnpm/lockfile.utils src/toLockfileResolution.ts and pkgSnapshotToResolution.ts (pnpm 11.2.2) with
 * get-npm-tarball-url 2.1.0; before 11.0.8 toLockfileResolution keeps no tarball URL under
 * {@code lockfileIncludeTarballUrl: false} ({@link PnpmRelease#keepsATarballUrlTheRegistryCannotGive}), and before
 * 11.0.7 no resolution is marked git-hosted ({@link PnpmRelease#marksGitHostedTarballs}).
 */
final class LockfileUtils {

    private LockfileUtils() {
    }

    /**
     * {@link #toLockfileResolution(String, String, Map, String, Boolean, PnpmRelease)} as 11.2.2 runs it.
     */
    static Map<String, @Nullable Object> toLockfileResolution(String name, String version,
                                                              Map<String, @Nullable Object> resolution,
                                                              String registry, boolean lockfileIncludeTarballUrl) {
        return toLockfileResolution(name, version, resolution, registry, lockfileIncludeTarballUrl,
                PnpmRelease.PORTED);
    }

    /**
     * The resolution a lock records: the integrity alone where the tarball URL follows from the registry.
     * {@code lockfileIncludeTarballUrl} is null where the caller passes none (the env installer's), which the
     * releases before 11.0.8 tell from {@code false}.
     */
    static Map<String, @Nullable Object> toLockfileResolution(String name, String version,
                                                              Map<String, @Nullable Object> resolution,
                                                              String registry,
                                                              @Nullable Boolean lockfileIncludeTarballUrl,
                                                              PnpmRelease release) {
        Object integrity = resolution.get("integrity");
        boolean typed = resolution.containsKey("type") && resolution.get("type") != Js.UNDEFINED;
        if (typed || !Js.truthy(integrity)) {
            return resolution;
        }
        Map<String, @Nullable Object> lockfileResolution = toLockfileResolution(name, version, resolution, integrity,
                registry, lockfileIncludeTarballUrl, release);
        // from 11.7.0 a resolution that keeps its tarball keeps its path too (a git repository's subdirectory)
        // (PnpmRelease#keepsATarballResolutionsPath)
        Object path = resolution.get("path");
        if (path != null && path != Js.UNDEFINED && lockfileResolution.containsKey("tarball") &&
            release.keepsATarballResolutionsPath()) {
            Map<String, @Nullable Object> withPath = new LinkedHashMap<>(lockfileResolution);
            withPath.put("path", path);
            return withPath;
        }
        return lockfileResolution;
    }

    private static Map<String, @Nullable Object> toLockfileResolution(String name, String version,
                                                                      Map<String, @Nullable Object> resolution,
                                                                      Object integrity, String registry,
                                                                      @Nullable Boolean lockfileIncludeTarballUrl,
                                                                      PnpmRelease release) {
        Object tarball = resolution.get("tarball");
        // the release is asked where its resolution differs from those of the releases around it
        Outcome current = Outcome.of(() -> keepingATarballUrl(name, version, resolution, integrity, tarball,
                release, registry, lockfileIncludeTarballUrl));
        Outcome markingGitHosted = Outcome.of(() -> keepingNoTarballUrl(name, version, resolution, integrity,
                tarball, release, registry, lockfileIncludeTarballUrl, true));
        Outcome first = Outcome.of(() -> keepingNoTarballUrl(name, version, resolution, integrity, tarball, release,
                registry, lockfileIncludeTarballUrl, false));
        if (first.same(markingGitHosted) && markingGitHosted.same(current)) {
            return current.get();
        }
        if (markingGitHosted.same(current)) {
            return (release.marksGitHostedTarballs() ? current : first).get();
        }
        if (release.keepsATarballUrlTheRegistryCannotGive()) {
            return current.get();
        }
        return (first.same(markingGitHosted) || release.marksGitHostedTarballs() ? markingGitHosted : first).get();
    }

    // toLockfileResolution of 11.0.8 on, the tarball URL the registry cannot give kept (pnpm/pnpm#11276)
    private static Map<String, @Nullable Object> keepingATarballUrl(String name, String version,
                                                                    Map<String, @Nullable Object> resolution,
                                                                    Object integrity, @Nullable Object tarball,
                                                                    PnpmRelease release,
                                                                    String registry,
                                                                    @Nullable Boolean lockfileIncludeTarballUrl) {
        if (tarball == null || tarball == Js.UNDEFINED) {
            Map<String, @Nullable Object> out = new LinkedHashMap<>();
            out.put("integrity", integrity);
            return out;
        }
        String url = tarballUrl(tarball);
        boolean gitHosted = Boolean.TRUE.equals(resolution.get("gitHosted")) || isGitHostedTarballUrl(url, release);
        if (Boolean.TRUE.equals(lockfileIncludeTarballUrl) || url.startsWith("file:") || gitHosted) {
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

    // toLockfileResolution of 11.0.6 and 11.0.7: false keeps the integrity alone, and only a caller that passes no
    // lockfileIncludeTarballUrl compares the tarball with the registry's URL; 11.0.7 marks a git-hosted one
    private static Map<String, @Nullable Object> keepingNoTarballUrl(String name, String version,
                                                                     Map<String, @Nullable Object> resolution,
                                                                     Object integrity, @Nullable Object tarball,
                                                                     PnpmRelease release,
                                                                     String registry,
                                                                     @Nullable Boolean lockfileIncludeTarballUrl,
                                                                     boolean marksGitHosted) {
        boolean hasTarball = tarball != null && tarball != Js.UNDEFINED;
        String url = hasTarball ? tarballUrl(tarball) : null;
        boolean gitHostedUrl = url != null && isGitHostedTarballUrl(url, release);
        boolean gitHosted = marksGitHosted ? Boolean.TRUE.equals(resolution.get("gitHosted")) || gitHostedUrl :
                gitHostedUrl;
        if (Boolean.TRUE.equals(lockfileIncludeTarballUrl)) {
            return preservingGitHosted(integrity, url, marksGitHosted && gitHosted);
        }
        if (url != null && (url.startsWith("file:") || gitHosted)) {
            return preservingGitHosted(integrity, url, marksGitHosted && gitHosted);
        }
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        out.put("integrity", integrity);
        if (Boolean.FALSE.equals(lockfileIncludeTarballUrl)) {
            return out;
        }
        if (url == null) {
            throw new JsError(JsError.TYPE_ERROR, "Cannot read properties of undefined (reading 'replaceAll')");
        }
        String expectedTarball = getNpmTarballUrl(name, version, registry);
        if (!Objects.equals(removeProtocol(expectedTarball), removeProtocol(url.replace("%2f", "/")))) {
            return preservingGitHosted(integrity, url, marksGitHosted && gitHosted);
        }
        return out;
    }

    /**
     * A resolution, or what computing it throws.
     */
    private static final class Outcome {
        private final @Nullable Map<String, @Nullable Object> resolution;
        private final @Nullable RuntimeException error;

        private Outcome(@Nullable Map<String, @Nullable Object> resolution, @Nullable RuntimeException error) {
            this.resolution = resolution;
            this.error = error;
        }

        static Outcome of(Supplier<Map<String, @Nullable Object>> compute) {
            try {
                return new Outcome(compute.get(), null);
            } catch (RuntimeException e) {
                return new Outcome(null, e);
            }
        }

        // the same entries in the same order, or the same error
        boolean same(Outcome other) {
            if (error != null || other.error != null) {
                return error != null && other.error != null && error.getClass() == other.error.getClass() &&
                       Objects.equals(error.getMessage(), other.error.getMessage());
            }
            return new ArrayList<>(requireNonNull(resolution).entrySet())
                    .equals(new ArrayList<>(requireNonNull(other.resolution).entrySet()));
        }

        Map<String, @Nullable Object> get() {
            if (error != null) {
                throw error;
            }
            return requireNonNull(resolution);
        }
    }

    private static String tarballUrl(Object tarball) {
        if (!(tarball instanceof String)) {
            throw new CannotKnow("what pnpm makes of the tarball " + Js.string(tarball) + " (" + Js.typeOf(tarball) +
                                 ", where pnpm reads a string)");
        }
        return (String) tarball;
    }

    /**
     * {@link #pkgSnapshotToResolution(String, PackageSnapshot, Map, PnpmRelease)} as 11.2.2 runs it.
     */
    static Map<String, @Nullable Object> pkgSnapshotToResolution(String depPath, PackageSnapshot pkgSnapshot,
                                                                 Map<String, String> registries) {
        return pkgSnapshotToResolution(depPath, pkgSnapshot, registries, PnpmRelease.PORTED);
    }

    /**
     * {@code pkgSnapshotToResolution}: the resolution a snapshot records, its tarball URL filled in for a registry
     * package.
     */
    static Map<String, @Nullable Object> pkgSnapshotToResolution(String depPath, PackageSnapshot pkgSnapshot,
                                                                 Map<String, String> registries, PnpmRelease release) {
        Map<String, @Nullable Object> resolution = pkgSnapshot.getResolution();
        if (resolution == null) {
            resolution = new LinkedHashMap<>();
        }
        Object tarball = resolution.get("tarball");
        boolean fileTarball = tarball instanceof String && ((String) tarball).startsWith("file:");
        if (isNullish(resolution.get("type")) && isNullish(resolution.get("integrity")) && !fileTarball &&
            !(Boolean.TRUE.equals(resolution.get("gitHosted")) ||
              tarball instanceof String && isGitHostedTarballUrl((String) tarball, release)) &&
            release.requiresALockedTarballsIntegrity()) {
            throw new PnpmError("MISSING_TARBALL_INTEGRITY", "Cannot install package \"" + depPath + "\": its " +
                                "lockfile entry has no \"integrity\" field, so pnpm cannot verify the downloaded " +
                                "tarball.", "The lockfile may be corrupted or have been tampered with. Restore it " +
                                "from a trusted source, or delete it and re-run installation without " +
                                "--frozen-lockfile to regenerate.");
        }
        // from 11.0.7 the gitHosted flag tells a git-hosted tarball; before, its URL does
        boolean flagged = Boolean.TRUE.equals(resolution.get("gitHosted"));
        boolean gitHostedUrl = tarball instanceof String && isGitHostedTarballUrl((String) tarball, release);
        boolean gitHosted = flagged == gitHostedUrl || release.marksGitHostedTarballs() ? flagged : gitHostedUrl;
        if (Js.truthy(resolution.get("type")) || fileTarball || gitHosted) {
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

    // == null in JavaScript: null or undefined
    private static boolean isNullish(@Nullable Object value) {
        return value == null || value == Js.UNDEFINED;
    }

    // {integrity, tarball}, an undefined tarball left out as the lock's writer leaves it out
    private static Map<String, @Nullable Object> preservingGitHosted(Object integrity, @Nullable String tarball,
                                                                     boolean gitHosted) {
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        out.put("integrity", integrity);
        if (tarball != null) {
            out.put("tarball", tarball);
        }
        if (gitHosted) {
            out.put("gitHosted", true);
        }
        return out;
    }

    /**
     * {@code isGitHostedTarballUrl}, which lockfile.fs shares from 11.5.3: the URL as written, and from 11.5.3 in any
     * case (PnpmRelease#matchesGitHostedTarballUrlsInAnyCase).
     */
    static boolean isGitHostedTarballUrl(String url, PnpmRelease release) {
        return isGitHostedTarballUrl(url) ||
               isGitHostedTarballUrlInAnyCase(url) && release.matchesGitHostedTarballUrlsInAnyCase();
    }

    static boolean isGitHostedTarballUrlInAnyCase(String url) {
        return isGitHostedTarballUrl(url.toLowerCase(Locale.ROOT));
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
