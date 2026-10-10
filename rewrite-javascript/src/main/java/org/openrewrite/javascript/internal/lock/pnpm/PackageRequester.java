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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.javascript.internal.lock.WhatwgUrl;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Port of @pnpm/installing.package-requester's {@code resolveAndFetch} (pnpm 11.2.2) over
 * @pnpm/resolving.default-resolver's chain.
 */
final class PackageRequester {

    private static final Set<String> GIT_PROTOCOLS = new LinkedHashSet<>(Arrays.asList("git", "git+http",
            "git+https", "git+rsync", "git+ftp", "git+file", "git+ssh", "ssh"));
    private static final Pattern HOSTED_SHORTCUT = Pattern.compile("^(github|gitlab|bitbucket|gist):.+");
    private static final Pattern GITHUB_SHORTHAND = Pattern.compile("^[^@:/\\s.][^@:/\\s]*/[^@:/\\s]+(#.*)?$");
    private static final Pattern GIT_URL_SUFFIX = Pattern.compile("\\.git(?:#|$)");
    private static final Pattern IS_FILENAME = Pattern.compile("\\.(?:tgz|tar.gz|tar)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern IS_FILESPEC = Pattern.compile("^(?:[./]|~/|[a-z]:)", Pattern.CASE_INSENSITIVE);

    private final NpmResolver npmResolver;

    /**
     * The project's files by path from the lock's directory: every manifest given and the package-manager files.
     */
    private final Map<String, String> files;

    private final Host host;
    private final boolean engineStrict;
    private final boolean force;

    /**
     * The {@code nodeVersion} setting as pnpm read it; null for the host's.
     */
    private final @Nullable Object nodeVersion;

    private final @Nullable Map<String, @Nullable Object> supportedArchitectures;
    private final RegistryMetadata fetch;
    private final @Nullable Map<String, @Nullable Object> nodeDownloadMirrors;

    PackageRequester(NpmResolver npmResolver, Map<String, String> files, Host host, boolean engineStrict,
                     boolean force, @Nullable Object nodeVersion,
                     @Nullable Map<String, @Nullable Object> supportedArchitectures, RegistryMetadata fetch,
                     @Nullable Map<String, @Nullable Object> nodeDownloadMirrors) {
        this.npmResolver = npmResolver;
        this.files = files;
        this.host = host;
        this.engineStrict = engineStrict;
        this.force = force;
        this.nodeVersion = nodeVersion;
        this.supportedArchitectures = supportedArchitectures;
        this.fetch = fetch;
        this.nodeDownloadMirrors = nodeDownloadMirrors;
    }

    /**
     * {@code RequestPackageOptions}.
     */
    static final class RequestPackageOptions {
        boolean alwaysTryWorkspacePackages;
        ResolveDependencies.@Nullable InfoFromLockfile currentPkg;
        @Nullable String currentPkgPublishedAt;
        @Nullable String defaultTag;
        @Nullable Instant publishedBy;
        @Nullable Function<String, @Nullable Object> publishedByExclude;
        boolean pickLowestVersion;
        @Nullable String lockfileDir;
        PreferredVersions.@Nullable ByName preferredVersions;
        boolean preferWorkspacePackages;
        @Nullable String projectDir;
        @Nullable String trustPolicy;
        @Nullable Function<String, Object> trustPolicyExclude;
        @Nullable Double trustPolicyIgnoreAfter;
        @Nullable String update;
        @Nullable Map<String, Map<String, Verification.Project>> workspacePackages;
        boolean injectWorkspacePackages;
        boolean calcSpecifier;
        @Nullable String pinnedVersion;
    }

    /**
     * {@code PackageResponse.body}.
     */
    @Getter
    static final class PackageResponse {
        private final String id;
        private final boolean isLocal;
        private final @Nullable Boolean isInstallable;
        private final @Nullable Map<String, @Nullable Object> manifest;
        private final @Nullable String normalizedBareSpecifier;
        private final Map<String, @Nullable Object> resolution;
        private final @Nullable String resolvedVia;
        private final boolean updated;
        private final @Nullable String publishedAt;
        private final @Nullable String alias;
        private final ResolveDependencies.@Nullable PolicyViolation policyViolation;

        /**
         * Why the package's manifest is unknown: fetching it would read a tarball or repository the engine is not
         * given.
         */
        private @Nullable CannotKnow unknownManifest;

        PackageResponse(NpmResolver.ResolveResult result, boolean isLocal, @Nullable Boolean isInstallable,
                        @Nullable Map<String, @Nullable Object> manifest, boolean updated) {
            this.id = result.id;
            this.isLocal = isLocal;
            this.isInstallable = isInstallable;
            this.manifest = manifest;
            this.normalizedBareSpecifier = result.normalizedBareSpecifier;
            this.resolution = result.resolution;
            this.resolvedVia = result.resolvedVia;
            this.updated = updated;
            this.publishedAt = result.publishedAt;
            this.alias = result.alias;
            this.policyViolation = result.policyViolation;
        }
    }

    PackageResponse requestPackage(ResolveDependencies.WantedDependency wantedDependency,
                                   RequestPackageOptions options) {
        ResolveDependencies.InfoFromLockfile currentPkg = options.currentPkg;
        Map<String, @Nullable Object> resolution = currentPkg == null ? null : currentPkg.getResolution();
        String pkgId = currentPkg == null ? null : currentPkg.getPkgId();
        PreferredVersions.ByName preferredVersions = options.preferredVersions;
        if (resolution != null && options.update == null && currentPkg.getName() != null &&
            currentPkg.getVersion() != null) {
            Map<String, Map<String, PreferredVersions.Selector>> own = preferredVersions == null ?
                    new LinkedHashMap<>() : preferredVersions.spreadOwn();
            Map<String, PreferredVersions.Selector> selectors = new LinkedHashMap<>();
            selectors.put(currentPkg.getVersion(), PreferredVersions.Selector.of("version"));
            own.put(currentPkg.getName(), selectors);
            preferredVersions = PreferredVersions.ByName.of(own);
        }
        boolean withCurrent = pkgId != null && resolution != null;
        NpmResolver.ResolveResult resolveResult = resolve(wantedDependency, options, preferredVersions,
                withCurrent ? currentPkg : null);

        Map<String, @Nullable Object> manifest = resolveResult.manifest;
        Object previousIntegrity = resolution == null ? null : resolution.get("integrity");
        Object newIntegrity = resolveResult.resolution.get("integrity");
        boolean integrityChanged = previousIntegrity != null && newIntegrity != null &&
                                   !previousIntegrity.equals(newIntegrity);
        boolean updated = !resolveResult.id.equals(pkgId) || resolution == null || integrityChanged;
        String id = resolveResult.id;

        if ("directory".equals(resolveResult.resolution.get("type")) && !id.startsWith("file:")) {
            if (manifest == null) {
                throw new JsError(JsError.ERROR, "Couldn't read package.json of local dependency " +
                                  (Js.truthy(wantedDependency.getAlias()) ? wantedDependency.getAlias() + "@" : "") +
                                  wantedDependency.getBareSpecifier());
            }
            return new PackageResponse(resolveResult, true, null, manifest, updated);
        }
        CannotKnow unknown = null;
        if (manifest == null) {
            try {
                manifest = manifestFromFetch(wantedDependency, resolveResult, currentPkg);
            } catch (CannotKnow e) {
                unknown = e;
            }
        }
        Boolean isInstallable = manifest == null ? null : packageIsInstallable(id, manifest,
                wantedDependency.isOptional());
        PackageResponse response = new PackageResponse(resolveResult, false, isInstallable, manifest, updated);
        response.unknownManifest = unknown;
        return response;
    }

    // what fetching the package would read: the directory's package.json, else the lock's own record of a package
    // whose tarball it carries, else nothing the engine is given
    private Map<String, @Nullable Object> manifestFromFetch(ResolveDependencies.WantedDependency wantedDependency,
                                                            NpmResolver.ResolveResult resolveResult,
                                                            ResolveDependencies.@Nullable InfoFromLockfile currentPkg) {
        Map<String, @Nullable Object> resolution = resolveResult.resolution;
        if ("directory".equals(resolution.get("type"))) {
            String dir = RootRelativePath.normalize(Js.string(resolution.get("directory")));
            Map<String, @Nullable Object> manifest = readManifest(dir);
            if (manifest == null) {
                throw new CannotKnow("the package.json of " + (dir.isEmpty() ? "." : dir) + " (" +
                                     wantedDependency.getBareSpecifier() + ")");
            }
            return manifest;
        }
        PackageSnapshot snapshot = currentPkg == null ? null : currentPkg.getDependencyLockfile();
        if (snapshot != null && resolveResult.id.equals(currentPkg.getPkgId())) {
            if (snapshot.getDependencies() != null && !snapshot.getDependencies().isEmpty() ||
                snapshot.getOptionalDependencies() != null && !snapshot.getOptionalDependencies().isEmpty()) {
                throw new CannotKnow("the manifest of " + resolveResult.id + " (pnpm reads its dependencies from " +
                                     "the package's tarball)");
            }
            Map<String, @Nullable Object> locked = currentPkg.getResolution();
            if (resolution.get("integrity") == null && resolution.get("type") == null && locked != null &&
                locked.get("integrity") != null) {
                resolution.put("integrity", locked.get("integrity"));
            }
            Map<String, @Nullable Object> manifest = new LinkedHashMap<>();
            manifest.put("name", currentPkg.getName());
            manifest.put("version", currentPkg.getVersion());
            putIfPresent(manifest, "engines", snapshot.getEngines());
            putIfPresent(manifest, "cpu", snapshot.getCpu());
            putIfPresent(manifest, "os", snapshot.getOs());
            putIfPresent(manifest, "libc", snapshot.getLibc());
            putIfPresent(manifest, "deprecated", snapshot.getDeprecated());
            putIfPresent(manifest, "bundledDependencies", snapshot.getBundledDependencies());
            putIfPresent(manifest, "peerDependencies", snapshot.getPeerDependencies());
            putIfPresent(manifest, "peerDependenciesMeta", snapshot.getPeerDependenciesMeta());
            return manifest;
        }
        if ("git-repository".equals(resolveResult.resolvedVia)) {
            throw new CannotKnow("git commit and manifest of " + wantedDependency.getBareSpecifier());
        }
        throw new CannotKnow("tarball " + Js.string(resolution.get("tarball")) + " (its integrity needs the tarball)");
    }

    private static void putIfPresent(Map<String, @Nullable Object> manifest, String key, @Nullable Object value) {
        if (value != null && value != Js.UNDEFINED) {
            manifest.put(key, value);
        }
    }

    private NpmResolver.ResolveResult resolve(ResolveDependencies.WantedDependency wantedDependency,
                                              RequestPackageOptions options,
                                              PreferredVersions.@Nullable ByName preferredVersions,
                                              ResolveDependencies.@Nullable InfoFromLockfile currentPkg) {
        NpmResolver.ResolveResult result = npmResolver.resolveNpm(wantedDependency, options, preferredVersions);
        if (result != null) {
            return result;
        }
        result = npmResolver.resolveJsr(wantedDependency, options, preferredVersions);
        if (result != null) {
            return result;
        }
        String bareSpecifier = wantedDependency.getBareSpecifier();
        if (bareSpecifier != null && !bareSpecifier.isEmpty()) {
            result = resolveFromGit(bareSpecifier, options, currentPkg);
            if (result == null) {
                result = resolveFromTarball(bareSpecifier);
            }
            if (result == null) {
                result = resolveFromLocalScheme(wantedDependency, options, currentPkg);
            }
            if (result != null) {
                return result;
            }
        }
        result = NodeRuntimeResolver.resolveNodeRuntime(fetch, wantedDependency, currentPkg, options.update,
                nodeDownloadMirrors, host.getOs());
        if (result != null) {
            return result;
        }
        result = DenoRuntimeResolver.resolveDenoRuntime(fetch, npmResolver, wantedDependency, currentPkg,
                options.update, host.getOs());
        if (result != null) {
            return result;
        }
        result = BunRuntimeResolver.resolveBunRuntime(fetch, npmResolver, wantedDependency, currentPkg,
                options.update, host.getOs());
        if (result != null) {
            return result;
        }
        result = npmResolver.resolveFromNamedRegistry(wantedDependency, options, preferredVersions);
        if (result != null) {
            return result;
        }
        if (bareSpecifier != null && !bareSpecifier.isEmpty()) {
            result = resolveFromLocalPath(wantedDependency, options, currentPkg);
            if (result != null) {
                return result;
            }
        }
        String specifier = (Js.truthy(wantedDependency.getAlias()) ? wantedDependency.getAlias() + "@" : "") +
                           (bareSpecifier == null ? "" : bareSpecifier);
        throw new PnpmError("SPEC_NOT_SUPPORTED_BY_ANY_RESOLVER", (specifier.isEmpty() ? "" : "\"" + specifier + "\"") +
                                                                  " isn't supported by any available resolver.");
    }

    // the git resolver: a locked package is carried as the lock records it; a new one needs the repository
    private static NpmResolver.@Nullable ResolveResult resolveFromGit(
            String bareSpecifier, RequestPackageOptions options,
            ResolveDependencies.@Nullable InfoFromLockfile currentPkg) {
        if (!isGitSpec(bareSpecifier)) {
            return null;
        }
        if (currentPkg != null && options.update == null && currentPkg.getResolution() != null) {
            Map<String, @Nullable Object> current = currentPkg.getResolution();
            if ("git".equals(current.get("type")) || Js.truthy(current.get("tarball"))) {
                NpmResolver.ResolveResult result = new NpmResolver.ResolveResult();
                result.id = currentPkg.getPkgId();
                result.resolution = current;
                result.resolvedVia = "git-repository";
                return result;
            }
        }
        throw new CannotKnow("git commit and manifest of " + bareSpecifier);
    }

    private static boolean isGitSpec(String bareSpecifier) {
        if (HOSTED_SHORTCUT.matcher(bareSpecifier).matches() || GITHUB_SHORTHAND.matcher(bareSpecifier).matches()) {
            return true;
        }
        int colon = bareSpecifier.indexOf(':');
        if (colon == -1) {
            return false;
        }
        String protocol = bareSpecifier.substring(0, colon);
        if (GIT_PROTOCOLS.contains(protocol.toLowerCase())) {
            return true;
        }
        if (("http".equals(protocol) || "https".equals(protocol)) &&
            GIT_URL_SUFFIX.matcher(bareSpecifier).find()) {
            return true;
        }
        WhatwgUrl url = WhatwgUrl.tryParse(bareSpecifier);
        return url != null && ("https:".equals(url.getProtocol()) || "http:".equals(url.getProtocol())) &&
               Arrays.asList("github.com", "gitlab.com", "bitbucket.org", "gist.github.com").contains(url.getHost()) &&
               !IS_FILENAME.matcher(url.getPathname()).find();
    }

    // the tarball resolver: the URL is the id; its integrity is known only from a lock that carries it
    private static NpmResolver.@Nullable ResolveResult resolveFromTarball(String bareSpecifier) {
        if (!bareSpecifier.startsWith("http:") && !bareSpecifier.startsWith("https:")) {
            return null;
        }
        WhatwgUrl url = WhatwgUrl.tryParse(bareSpecifier);
        if (url == null) {
            throw new JsError(JsError.TYPE_ERROR, "Invalid URL");
        }
        NpmResolver.ResolveResult result = new NpmResolver.ResolveResult();
        result.id = url.toString();
        result.normalizedBareSpecifier = url.toString();
        result.resolution.put("tarball", url.toString());
        result.resolvedVia = "url";
        return result;
    }

    private NpmResolver.@Nullable ResolveResult resolveFromLocalScheme(
            ResolveDependencies.WantedDependency wd, RequestPackageOptions options,
            ResolveDependencies.@Nullable InfoFromLockfile currentPkg) {
        String bareSpecifier = wd.getBareSpecifier();
        String type;
        if (bareSpecifier.startsWith("link:") || bareSpecifier.startsWith("workspace:")) {
            type = "directory";
        } else if (bareSpecifier.startsWith("file:")) {
            type = IS_FILENAME.matcher(bareSpecifier).find() ? "file" : "directory";
        } else if (bareSpecifier.startsWith("path:")) {
            throw new PnpmError("PATH_IS_UNSUPPORTED_PROTOCOL", "Local dependencies via `path:` protocol are not " +
                                                                "supported. Use the `link:` protocol for folder " +
                                                                "dependencies and `file:` for local tarballs");
        } else {
            return null;
        }
        return resolveLocal(wd, options, currentPkg, type);
    }

    private NpmResolver.@Nullable ResolveResult resolveFromLocalPath(
            ResolveDependencies.WantedDependency wd, RequestPackageOptions options,
            ResolveDependencies.@Nullable InfoFromLockfile currentPkg) {
        String bareSpecifier = wd.getBareSpecifier();
        if (bareSpecifier.endsWith(".tgz") || bareSpecifier.endsWith(".tar.gz") || bareSpecifier.endsWith(".tar") ||
            bareSpecifier.contains("/") || IS_FILESPEC.matcher(bareSpecifier).find()) {
            return resolveLocal(wd, options, currentPkg, IS_FILENAME.matcher(bareSpecifier).find() ? "file" :
                    "directory");
        }
        return null;
    }

    // local-resolver fromLocal and resolveSpec, the project's directories relative to the lock's
    private NpmResolver.ResolveResult resolveLocal(ResolveDependencies.WantedDependency wd,
                                                   RequestPackageOptions options,
                                                   ResolveDependencies.@Nullable InfoFromLockfile currentPkg,
                                                   String type) {
        String bareSpecifier = wd.getBareSpecifier();
        String spec = bareSpecifier.replace('\\', '/')
                .replaceFirst("^(?i:file|link|workspace):/*([A-Za-z]:)", "$1")
                .replaceFirst("^(?:file|link|workspace):(?:/*([~./]))?", "$1");
        String protocol;
        if (bareSpecifier.startsWith("file:")) {
            protocol = "file:";
        } else if (bareSpecifier.startsWith("link:")) {
            protocol = "link:";
        } else {
            protocol = "directory".equals(type) && !Boolean.TRUE.equals(wd.getInjected()) ? "link:" : "file:";
        }
        if (spec.startsWith("~/") || spec.startsWith("/") || spec.matches("^[A-Za-z]:.*")) {
            throw new CannotKnow("the directory " + bareSpecifier + " names (a path outside the project)");
        }
        String projectDir = options.projectDir == null ? "" : options.projectDir;
        String lockfileDir = options.lockfileDir == null ? "" : options.lockfileDir;
        String fetchSpec = RootRelativePath.join(projectDir, spec);
        String normalizedBareSpecifier = protocol + RootRelativePath.relative(projectDir, fetchSpec);
        boolean injected = "file:".equals(protocol);
        String dependencyPath = injected ? RootRelativePath.relative(lockfileDir, fetchSpec) : fetchSpec;
        String id = !injected && ("directory".equals(type) || projectDir.equals(lockfileDir)) ?
                protocol + RootRelativePath.relative(projectDir, fetchSpec) :
                protocol + RootRelativePath.relative(lockfileDir, fetchSpec);
        NpmResolver.ResolveResult result = new NpmResolver.ResolveResult();
        result.resolvedVia = "local-filesystem";
        if ("file".equals(type)) {
            throw new CannotKnow("tarball " + id + " (pnpm re-hashes it on every install)");
        }
        if (currentPkg != null && currentPkg.getResolution() != null && options.update == null) {
            result.id = currentPkg.getPkgId();
            result.resolution = currentPkg.getResolution();
            return result;
        }
        Map<String, @Nullable Object> manifest = readManifest(fetchSpec);
        if (manifest == null) {
            throw new CannotKnow("the package.json of " + (fetchSpec.isEmpty() ? "." : fetchSpec) + " (" +
                                 bareSpecifier + ")");
        }
        result.id = id;
        result.manifest = manifest;
        result.normalizedBareSpecifier = normalizedBareSpecifier;
        result.resolution.put("directory", dependencyPath);
        result.resolution.put("type", "directory");
        return result;
    }

    private @Nullable Map<String, @Nullable Object> readManifest(String dir) {
        String path = dir.isEmpty() ? "package.json" : dir + "/package.json";
        String text = files.get(path);
        return text == null ? null : Manifests.readProjectManifest(text, path);
    }

    private @Nullable Boolean packageIsInstallable(String pkgId, Map<String, @Nullable Object> manifest,
                                                   boolean optional) {
        return force ? Boolean.TRUE : PackageIsInstallable.packageIsInstallable(pkgId, manifest, engineStrict,
                nodeVersion, optional, supportedArchitectures, host);
    }
}
