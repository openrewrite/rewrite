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
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.semver.NodeSemver;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of @pnpm/installing.env-installer (pnpm 11.2.2) as a lock-only install runs it, of installConfigDeps only the
 * checks its normalizeFromLockfile makes of the env document.
 */
final class EnvInstaller {

    private final RegistryMetadata registry;
    private final Instant now;
    private final Host host;

    EnvInstaller(RegistryMetadata registry, Instant now, Host host) {
        this.registry = registry;
        this.now = now;
        this.host = host;
    }

    /**
     * {@code resolveAndInstallConfigDeps}: the old forms migrate into the lock's env document, the others not locked
     * at their specifier resolve from the registry, and the document is written when either changed it.
     */
    @SuppressWarnings("unchecked")
    void resolveAndInstallConfigDeps(Map<String, @Nullable Object> configDeps, EnvLockfile.OnDisk lockFile,
                                     PnpmOptions options) {
        PnpmRelease release = options.getRelease();
        Map<String, String> registries = options.getRegistries();
        EnvLockfile read = lockFile.readEnvLockfile();
        EnvLockfile envLockfile = read != null ? read : EnvLockfile.createEnvLockfile();
        Map<String, @Nullable Object> lockfileConfigDeps = envLockfile.configDependencies();
        Map<String, String> depsToResolve = new LinkedHashMap<>();
        boolean lockfileChanged = false;
        for (String name : Js.keys(configDeps)) {
            Object value = configDeps.get(name);
            if (value == null || value instanceof Map || value instanceof List) {
                if (!Js.truthy(lockfileConfigDeps.get(name))) {
                    String registry = PickRegistryForPackage.pickRegistryForPackage(registries, name, null);
                    if (value == null) {
                        throw new JsError(JsError.ERROR, "Cannot read properties of null (reading 'integrity')");
                    }
                    Object spec = value instanceof Map ? ((Map<String, @Nullable Object>) value).get("integrity") :
                            null;
                    String[] parsed = parseIntegrity(name, spec);
                    Object tarball = value instanceof Map ? ((Map<String, @Nullable Object>) value).get("tarball") :
                            null;
                    migrate(envLockfile, name, parsed, tarball == null || tarball == Js.UNDEFINED ?
                            LockfileUtils.getNpmTarballUrl(name, parsed[0], registry) : tarball, registry, release);
                    lockfileChanged = true;
                }
                continue;
            }
            if (!(value instanceof String)) {
                throw new CannotKnow("what pnpm makes of configDependencies." + name + " (" + Js.typeOf(value) +
                                     ", where pnpm reads a string or an object)");
            }
            String specifier = (String) value;
            if (specifier.contains("+")) {
                if (!Js.truthy(lockfileConfigDeps.get(name))) {
                    String registry = PickRegistryForPackage.pickRegistryForPackage(registries, name, null);
                    String[] parsed = parseIntegrity(name, specifier);
                    migrate(envLockfile, name, parsed, LockfileUtils.getNpmTarballUrl(name, parsed[0], registry),
                            registry, release);
                    lockfileChanged = true;
                }
                continue;
            }
            Object existing = lockfileConfigDeps.get(name);
            if (Js.truthy(existing) && existing instanceof Map &&
                specifier.equals(((Map<String, @Nullable Object>) existing).get("specifier"))) {
                String pkgKey = name + "@" + Js.string(((Map<String, @Nullable Object>) existing).get("version"));
                if (Js.truthy(envLockfile.packages().get(pkgKey))) {
                    continue;
                }
            }
            depsToResolve.put(name, specifier);
        }
        if (depsToResolve.isEmpty()) {
            if (lockfileChanged) {
                verifyEnvLockfile(envLockfile, release);
                lockFile.writeEnvLockfile(envLockfile);
            }
            normalizeFromLockfile(envLockfile, release);
            return;
        }
        NpmResolver resolveFromNpm = new NpmResolver(registry, registries,
                CreateNewStoreController.namedRegistries(options), null, true, false, now, release, false);
        for (Map.Entry<String, String> dep : depsToResolve.entrySet()) {
            String name = dep.getKey();
            String specifier = dep.getValue();
            NpmResolver.ResolveResult resolution = resolveFromNpm(resolveFromNpm, name, specifier, false);
            if (resolution == null || !(resolution.resolution.get("integrity") instanceof String) ||
                ((String) resolution.resolution.get("integrity")).isEmpty()) {
                throw new PnpmError("BAD_CONFIG_DEP", "Cannot resolve " + name + "@" + specifier +
                                                      " as a configuration dependency because it has no integrity");
            }
            Map<String, @Nullable Object> manifest = resolution.manifest == null ? new LinkedHashMap<>() :
                    resolution.manifest;
            String version = Js.string(manifest.get("version"));
            String registry = PickRegistryForPackage.pickRegistryForPackage(registries, name, null);
            String pkgKey = name + "@" + version;
            Map<String, @Nullable Object> locked = new LinkedHashMap<>();
            locked.put("specifier", specifier);
            locked.put("version", version);
            lockfileConfigDeps.put(name, locked);
            Map<String, @Nullable Object> entry = new LinkedHashMap<>();
            entry.put("resolution", LockfileUtils.toLockfileResolution(name, version, resolution.resolution, registry,
                    null, release));
            envLockfile.packages().put(pkgKey, entry);
            // the config dependency's optionalDependencies, from 11.2.0
            Map<String, @Nullable Object> optionalSubdeps = Js.truthy(manifest.get("optionalDependencies")) &&
                                                            release.resolvesTheOptionalDependenciesOfAConfigDependency() ?
                    resolveOptionalSubdeps(name, manifest, envLockfile, registries, resolveFromNpm, release) : null;
            Map<String, @Nullable Object> snapshot = new LinkedHashMap<>();
            if (optionalSubdeps != null) {
                snapshot.put("optionalDependencies", optionalSubdeps);
            }
            envLockfile.snapshots().put(pkgKey, snapshot);
        }
        PruneEnvLockfile.pruneEnvLockfile(envLockfile);
        verifyEnvLockfile(envLockfile, release);
        lockFile.writeEnvLockfile(envLockfile);
        normalizeFromLockfile(envLockfile, release);
    }

    private static void migrate(EnvLockfile envLockfile, String name, String[] parsed, Object tarball,
                                String registry, PnpmRelease release) {
        String version = parsed[0];
        String pkgKey = name + "@" + version;
        Map<String, @Nullable Object> locked = new LinkedHashMap<>();
        locked.put("specifier", version);
        locked.put("version", version);
        envLockfile.configDependencies().put(name, locked);
        Map<String, @Nullable Object> resolution = new LinkedHashMap<>();
        resolution.put("integrity", parsed[1]);
        resolution.put("tarball", tarball);
        Map<String, @Nullable Object> pkg = new LinkedHashMap<>();
        pkg.put("resolution", LockfileUtils.toLockfileResolution(name, version, resolution, registry, null,
                release));
        envLockfile.packages().put(pkgKey, pkg);
        envLockfile.snapshots().put(pkgKey, new LinkedHashMap<String, @Nullable Object>());
    }

    // resolveOptionalSubdeps.ts (11.2.0): the config dependency's optional dependencies, each an exact version; null
    // for none
    private static @Nullable Map<String, @Nullable Object> resolveOptionalSubdeps(
            String parentName, Map<String, @Nullable Object> parentManifest, EnvLockfile envLockfile,
            Map<String, String> registries, NpmResolver resolveFromNpm, PnpmRelease release) {
        Map<String, @Nullable Object> optionalDeps =
                ResolveDependencies.asMap(parentManifest.get("optionalDependencies"));
        if (Js.keys(optionalDeps).isEmpty()) {
            return null;
        }
        Map<String, @Nullable Object> resolved = new LinkedHashMap<>();
        for (String subdepName : Js.keys(optionalDeps)) {
            String subdepSpec = Js.string(optionalDeps.get(subdepName));
            if (NodeSemver.valid(subdepSpec, NodeSemver.Options.DEFAULT) == null) {
                throw new PnpmError("CONFIG_DEP_OPTIONAL_NOT_EXACT", "Cannot install \"" + subdepName + "@" +
                        subdepSpec + "\" as an optionalDependency of config dependency \"" + parentName +
                        "\": only exact versions are supported (got \"" + subdepSpec + "\")");
            }
            NpmResolver.ResolveResult resolution;
            try {
                resolution = resolveFromNpm(resolveFromNpm, subdepName, subdepSpec, true);
            } catch (PnpmError e) {
                if ("ERR_PNPM_TRUST_DOWNGRADE".equals(e.getCode())) {
                    throw e;
                }
                continue;
            } catch (JsError e) {
                continue;
            }
            if (resolution == null || !(resolution.resolution.get("integrity") instanceof String) ||
                ((String) resolution.resolution.get("integrity")).isEmpty() || resolution.manifest == null) {
                throw new PnpmError("BAD_CONFIG_DEP", "Cannot resolve optionalDependency \"" + subdepName +
                        "\" of config dependency \"" + parentName + "\" because it has no integrity");
            }
            String subdepVersion = Js.string(resolution.manifest.get("version"));
            String subdepKey = subdepName + "@" + subdepVersion;
            Map<String, @Nullable Object> entry = new LinkedHashMap<>();
            entry.put("resolution", LockfileUtils.toLockfileResolution(subdepName, subdepVersion,
                    resolution.resolution, PickRegistryForPackage.pickRegistryForPackage(registries, subdepName,
                            null), null, release));
            for (String field : new String[]{"os", "cpu", "libc"}) {
                Object value = resolution.manifest.get(field);
                if (value instanceof List && !((List<?>) value).isEmpty() ||
                    value instanceof String && !((String) value).isEmpty()) {
                    entry.put(field, value);
                }
            }
            envLockfile.packages().put(subdepKey, entry);
            if (envLockfile.snapshots().get(subdepKey) == null) {
                Map<String, @Nullable Object> snapshot = new LinkedHashMap<>();
                // empty before 11.2.1
                if (release.marksAConfigDependencysOptionalDependenciesOptional()) {
                    snapshot.put("optional", true);
                }
                envLockfile.snapshots().put(subdepKey, snapshot);
            }
            resolved.put(subdepName, subdepVersion);
        }
        return resolved.isEmpty() ? null : resolved;
    }

    private static NpmResolver.@Nullable ResolveResult resolveFromNpm(NpmResolver resolver, String name,
                                                                      String specifier, boolean optional) {
        ResolveDependencies.WantedDependency wanted = new ResolveDependencies.WantedDependency(name, specifier, false,
                optional);
        PackageRequester.RequestPackageOptions opts = new PackageRequester.RequestPackageOptions();
        opts.lockfileDir = "";
        opts.projectDir = "";
        return resolver.resolveNpm(wanted, opts, PreferredVersions.ByName.of(new LinkedHashMap<>()));
    }

    /**
     * {@code parseIntegrity}: the version and integrity of {@code <version>+<integrity>}.
     */
    static String[] parseIntegrity(String pkgName, @Nullable Object pkgSpec) {
        if (pkgSpec == null || pkgSpec == Js.UNDEFINED) {
            throw new JsError(JsError.ERROR,
                    "Cannot read properties of " + Js.string(pkgSpec) + " (reading 'indexOf')");
        }
        if (!(pkgSpec instanceof String)) {
            throw new CannotKnow("what pnpm makes of the integrity of config dependency " + pkgName + " (" +
                                 Js.typeOf(pkgSpec) + ", where pnpm reads a string)");
        }
        String spec = (String) pkgSpec;
        int sepIndex = spec.indexOf('+');
        if (sepIndex == -1) {
            throw new PnpmError("CONFIG_DEP_NO_INTEGRITY", "Your config dependency called \"" + pkgName +
                                                           "\" doesn't have an integrity checksum",
                    "Integrity checksum should be inlined in the version specifier. For example:\n\n" +
                    "pnpm-workspace.yaml:\nconfigDependencies:\n  my-config: \"1.0.0+sha512-" +
                    "Xg0tn4HcfTijTwfDwYlvVCl43V6h4KyVVX2aEm4qdO/PC6L2YvzLHFdmxhoeSA3eslcE6+ZVXHgWwopXYLNq4Q==\"\n");
        }
        return new String[]{spec.substring(0, sepIndex), spec.substring(sepIndex + 1)};
    }

    /**
     * {@code isPackageManagerResolved}: the env document holds pnpm and @pnpm/exe at {@code pnpmVersion}.
     */
    static boolean isPackageManagerResolved(@Nullable EnvLockfile envLockfile, String pnpmVersion) {
        if (envLockfile == null) {
            return false;
        }
        Map<String, @Nullable Object> pmDeps = envLockfile.packageManagerDependencies();
        return pmDeps != null && pnpmVersion.equals(lockedVersion(pmDeps, "pnpm")) &&
               pnpmVersion.equals(lockedVersion(pmDeps, "@pnpm/exe"));
    }

    @SuppressWarnings("unchecked")
    static @Nullable Object lockedVersion(@Nullable Map<String, @Nullable Object> dependencies, String name) {
        Object dependency = dependencies == null ? null : dependencies.get(name);
        return dependency instanceof Map ? ((Map<String, @Nullable Object>) dependency).get("version") : null;
    }

    /**
     * {@code resolvePackageManagerIntegrities}: pnpm, @pnpm/exe and their dependencies into the env document's
     * packageManagerDependencies with the caller's store controller, written into the lock when {@code save}.
     */
    EnvLockfile resolvePackageManagerIntegrities(String pnpmVersion, @Nullable EnvLockfile envLockfile, boolean save,
                                                 EnvLockfile.OnDisk lockFile, NpmResolver storeController,
                                                 PnpmOptions options) {
        EnvLockfile lockfileToUpdate = envLockfile != null ? envLockfile : save ? lockFile.readEnvLockfile() : null;
        if (lockfileToUpdate == null) {
            lockfileToUpdate = EnvLockfile.createEnvLockfile();
        }
        if (isPackageManagerResolved(lockfileToUpdate, pnpmVersion)) {
            return lockfileToUpdate;
        }
        Map<String, @Nullable Object> dependencies = new LinkedHashMap<>();
        dependencies.put("pnpm", pnpmVersion);
        dependencies.put("@pnpm/exe", pnpmVersion);
        Map<String, @Nullable Object> manifest = new LinkedHashMap<>();
        manifest.put("dependencies", dependencies);
        LockfileObject lockfile = resolveManifestDependencies(manifest, storeController, options);
        if (lockfile.getPackages() == null) {
            return lockfileToUpdate;
        }
        ProjectSnapshot importer = lockfile.getImporters().get(".");
        Map<String, @Nullable Object> packageManagerDependencies = new LinkedHashMap<>();
        if (importer.getDependencies() != null) {
            for (Map.Entry<String, @Nullable String> dependency : importer.getDependencies().entrySet()) {
                Map<String, @Nullable Object> locked = new LinkedHashMap<>();
                locked.put("specifier", importer.getSpecifiers().get(dependency.getKey()));
                locked.put("version", dependency.getValue());
                packageManagerDependencies.put(dependency.getKey(), locked);
            }
        }
        lockfileToUpdate.importer().put("packageManagerDependencies", packageManagerDependencies);
        LockfileObject merged = PruneEnvLockfile.convertToLockfileEnvObject(lockfileToUpdate);
        if (merged.getPackages() == null) {
            merged.setPackages(new LinkedHashMap<>());
        }
        merged.getPackages().putAll(lockfile.getPackages());
        PruneEnvLockfile.replacePrunedEntries(lockfileToUpdate, merged);
        if (save) {
            verifyEnvLockfile(lockfileToUpdate, options.getRelease());
            lockFile.writeEnvLockfile(lockfileToUpdate);
        }
        return lockfileToUpdate;
    }

    // resolveManifestDependencies.ts: the lock deps-resolver writes for one manifest over an empty lock
    private LockfileObject resolveManifestDependencies(Map<String, @Nullable Object> manifest,
                                                       NpmResolver storeController, PnpmOptions options) {
        LockfileObject emptyLockfile = new LockfileObject();
        emptyLockfile.set("lockfileVersion", LockfileObject.LOCKFILE_VERSION);
        emptyLockfile.getImporters().put(".", new ProjectSnapshot());
        List<ResolveDependencies.WantedDependency> wantedDependencies =
                ResolveDependencyTree.getWantedDependencies(manifest, false, options.getRelease());
        for (ResolveDependencies.WantedDependency dep : wantedDependencies) {
            dep.setUpdateSpec(true);
        }
        Map<String, String> registries = options.getRegistries();
        PackageRequester requester = new PackageRequester(storeController, new LinkedHashMap<>(), host,
                options.isTrue("engineStrict"), options.isTrue("force"), options.getNodeVersion(), null, registry,
                options.getNodeDownloadMirrors());
        ResolveDependencies.ResolutionContext ctx = new ResolveDependencies.ResolutionContext(emptyLockfile, "",
                requester);
        ctx.setRelease(options.getRelease());
        ctx.setForceFullResolution(true);
        ctx.setRegistries(registries);
        ctx.setNamedRegistryPrefixes(options.getNamedRegistryPrefixes());
        ctx.setWorkspacePackages(new LinkedHashMap<>());
        DepsResolver.Options resolverOptions = new DepsResolver.Options(false, false, false, false, false, false,
                false, 1000, registries, true, violations -> {
            throw new PnpmError("RESOLUTION_POLICY_VIOLATIONS_UNHANDLED", violations.size() +
                    " resolution-policy " + (violations.size() == 1 ? "violation was" : "violations were") +
                    " produced but no handleResolutionPolicyViolations callback was wired to react to them.");
        });
        List<ResolveDependencyTree.Importer> importers = Collections.singletonList(
                new ResolveDependencyTree.Importer(".", "", manifest, wantedDependencies));
        return DepsResolver.resolveDependencies(importers, ctx, PreferredVersions.ByName.of(new LinkedHashMap<>()),
                resolverOptions).getNewLockfile();
    }

    /**
     * verifyEnvLockfile.ts of 11.8.0, before each write of the env document and each install of its config
     * dependencies ({@link PnpmRelease#verifiesTheEnvDocumentsConfigDependencies}): their names and those of their
     * optional dependencies are package names, and each version exact; asked where the check fails.
     */
    static void verifyEnvLockfile(EnvLockfile envLockfile, PnpmRelease release) {
        try {
            verifyEnvLockfile(envLockfile);
        } catch (PnpmError | JsError e) {
            if (release.verifiesTheEnvDocumentsConfigDependencies()) {
                throw e;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void verifyEnvLockfile(EnvLockfile envLockfile) {
        Object configDeps = envLockfile.importer().get("configDependencies");
        ResolveDependencies.assertValidDependencyAliases(configDeps, "The configDependencies in pnpm-lock.yaml");
        if (!(configDeps instanceof Map)) {
            return;
        }
        for (String name : Js.keys((Map<String, @Nullable Object>) configDeps)) {
            Object entry = ((Map<String, @Nullable Object>) configDeps).get(name);
            if (entry == null || entry == Js.UNDEFINED) {
                throw new JsError(JsError.TYPE_ERROR, "Cannot read properties of " + Js.string(entry) +
                                                      " (reading 'version')");
            }
            Object version = entry instanceof Map ? ((Map<String, @Nullable Object>) entry).get("version") : null;
            assertValidConfigDepVersion(name, version);
            Object snapshot = envLockfile.snapshots().get(name + "@" + Js.string(version == null ? Js.UNDEFINED :
                    version));
            Object optionalDeps = snapshot instanceof Map ?
                    ((Map<String, @Nullable Object>) snapshot).get("optionalDependencies") : null;
            if (optionalDeps == null || optionalDeps == Js.UNDEFINED) {
                continue;
            }
            ResolveDependencies.assertValidDependencyAliases(optionalDeps, "The optionalDependencies of config " +
                    "dependency \"" + name + "\" in pnpm-lock.yaml");
            if (optionalDeps instanceof Map) {
                for (String subdepName : Js.keys((Map<String, @Nullable Object>) optionalDeps)) {
                    assertValidConfigDepVersion(subdepName, ((Map<String, @Nullable Object>) optionalDeps)
                            .get(subdepName));
                }
            }
        }
    }

    private static void assertValidConfigDepVersion(String name, @Nullable Object version) {
        if (!(version instanceof String) || NodeSemver.valid((String) version, NodeSemver.Options.DEFAULT) == null) {
            throw new PnpmError("INVALID_CONFIG_DEP_VERSION", "The config dependency \"" + name + "\" has an " +
                    "invalid version \"" + Js.string(version == null ? Js.UNDEFINED : version) + "\"",
                    "A config dependency version must be an exact semver version.");
        }
    }

    // installConfigDeps' normalizeFromLockfile: every locked config dependency and, from 11.2.0, optional
    // subdependency must have its packages entry and integrity
    @SuppressWarnings("unchecked")
    private static void normalizeFromLockfile(EnvLockfile lockfile, PnpmRelease release) {
        verifyEnvLockfile(lockfile, release);
        Map<String, @Nullable Object> configDeps = lockfile.configDependencies();
        for (String pkgName : Js.keys(configDeps)) {
            Object entry = configDeps.get(pkgName);
            if (entry == null || entry == Js.UNDEFINED) {
                throw new JsError(JsError.ERROR,
                        "Cannot destructure property 'version' of '" + Js.string(entry) + "' as it is " +
                                  Js.string(entry) + ".");
            }
            Object version = entry instanceof Map ? ((Map<String, @Nullable Object>) entry).get("version") : null;
            String pkgKey = pkgName + "@" + Js.string(version == null ? Js.UNDEFINED : version);
            requireIntegrity(lockfile, pkgKey, "referenced from importers['.'].configDependencies");
            Object snapshot = lockfile.snapshots().get(pkgKey);
            Object optionalDependencies = snapshot instanceof Map ?
                    ((Map<String, @Nullable Object>) snapshot).get("optionalDependencies") : null;
            if (Js.truthy(optionalDependencies) && release.resolvesTheOptionalDependenciesOfAConfigDependency()) {
                if (!(optionalDependencies instanceof Map)) {
                    throw new CannotKnow("what pnpm makes of the optionalDependencies of config dependency " +
                                         pkgName + " (" + Js.typeOf(optionalDependencies) +
                                         ", where pnpm reads an object)");
                }
                Map<String, @Nullable Object> optional = (Map<String, @Nullable Object>) optionalDependencies;
                for (String subdepName : Js.keys(optional)) {
                    requireIntegrity(lockfile, subdepName + "@" + Js.string(optional.get(subdepName)),
                            "referenced from optionalDependencies of config dependency \"" + pkgName + "\"");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void requireIntegrity(EnvLockfile lockfile, String pkgKey, String referencedFrom) {
        Object pkgInfo = lockfile.packages().get(pkgKey);
        if (!Js.truthy(pkgInfo)) {
            throw new PnpmError("ENV_LOCKFILE_CORRUPTED", "pnpm-lock.yaml is corrupted or incomplete: missing " +
                                                          "packages entry for \"" + pkgKey + "\" " + referencedFrom);
        }
        Object resolution = pkgInfo instanceof Map ? ((Map<String, @Nullable Object>) pkgInfo).get("resolution") : null;
        if (resolution == null || resolution == Js.UNDEFINED) {
            throw new JsError(JsError.ERROR,
                    "Cannot read properties of " + Js.string(resolution) + " (reading 'integrity')");
        }
        Object integrity = resolution instanceof Map ?
                ((Map<String, @Nullable Object>) resolution).get("integrity") : null;
        if (!Js.truthy(integrity)) {
            throw new PnpmError("ENV_LOCKFILE_CORRUPTED", "pnpm-lock.yaml is corrupted or incomplete: missing " +
                                                          "integrity for \"" + pkgKey + "\"");
        }
    }
}
