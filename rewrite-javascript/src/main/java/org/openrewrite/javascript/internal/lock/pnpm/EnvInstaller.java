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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of @pnpm/installing.env-installer (pnpm 11.2.2) as far as the env document goes without the registry.
 */
final class EnvInstaller {

    private EnvInstaller() {
    }

    @Value
    static class ConfigDepsResult {
        boolean lockfileChanged;

        /**
         * The config dependencies pnpm resolves from the registry, name to specifier, in its order.
         */
        Map<String, String> depsToResolve;
    }

    /**
     * {@code resolveAndInstallConfigDeps} up to the registry: the old forms migrate into {@code envLockfile}, and
     * with nothing to resolve the locked config dependencies are checked as {@code installConfigDeps} reads them.
     */
    @SuppressWarnings("unchecked")
    static ConfigDepsResult resolveAndInstallConfigDeps(Map<String, @Nullable Object> configDeps,
                                                        EnvLockfile envLockfile, Map<String, String> registries) {
        Map<String, @Nullable Object> lockfileConfigDeps = envLockfile.configDependencies();
        Map<String, String> depsToResolve = new LinkedHashMap<>();
        boolean lockfileChanged = false;
        for (String name : Js.keys(configDeps)) {
            Object value = configDeps.get(name);
            if (value == null || value instanceof Map || value instanceof List) {
                if (!Js.truthy(lockfileConfigDeps.get(name))) {
                    String registry = PickRegistryForPackage.pickRegistryForPackage(registries, name, null);
                    if (value == null) {
                        throw new JsError("Cannot read properties of null (reading 'integrity')");
                    }
                    Object spec = value instanceof Map ? ((Map<String, @Nullable Object>) value).get("integrity") :
                            null;
                    String[] parsed = parseIntegrity(name, spec);
                    Object tarball = value instanceof Map ? ((Map<String, @Nullable Object>) value).get("tarball") :
                            null;
                    migrate(envLockfile, name, parsed, tarball == null || tarball == JsYaml.UNDEFINED ?
                            LockfileUtils.getNpmTarballUrl(name, parsed[0], registry) : tarball, registry);
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
                            registry);
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
            normalizeFromLockfile(envLockfile);
        }
        return new ConfigDepsResult(lockfileChanged, depsToResolve);
    }

    private static void migrate(EnvLockfile envLockfile, String name, String[] parsed, Object tarball,
                                String registry) {
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
        pkg.put("resolution", LockfileUtils.toLockfileResolution(name, version, resolution, registry, false));
        envLockfile.packages().put(pkgKey, pkg);
        envLockfile.snapshots().put(pkgKey, new LinkedHashMap<String, @Nullable Object>());
    }

    /**
     * {@code parseIntegrity}: the version and integrity of {@code <version>+<integrity>}.
     */
    static String[] parseIntegrity(String pkgName, @Nullable Object pkgSpec) {
        if (pkgSpec == null || pkgSpec == JsYaml.UNDEFINED) {
            throw new JsError("Cannot read properties of " + Js.string(pkgSpec) + " (reading 'indexOf')");
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

    // installConfigDeps' normalizeFromLockfile: every locked config dependency and optional subdependency must have
    // its packages entry and integrity
    @SuppressWarnings("unchecked")
    private static void normalizeFromLockfile(EnvLockfile lockfile) {
        Map<String, @Nullable Object> configDeps = lockfile.configDependencies();
        for (String pkgName : Js.keys(configDeps)) {
            Object entry = configDeps.get(pkgName);
            if (entry == null || entry == JsYaml.UNDEFINED) {
                throw new JsError("Cannot destructure property 'version' of '" + Js.string(entry) + "' as it is " +
                                  Js.string(entry) + ".");
            }
            Object version = entry instanceof Map ? ((Map<String, @Nullable Object>) entry).get("version") : null;
            String pkgKey = pkgName + "@" + Js.string(version == null ? JsYaml.UNDEFINED : version);
            requireIntegrity(lockfile, pkgKey, "referenced from importers['.'].configDependencies");
            Object snapshot = lockfile.snapshots().get(pkgKey);
            Object optionalDependencies = snapshot instanceof Map ?
                    ((Map<String, @Nullable Object>) snapshot).get("optionalDependencies") : null;
            if (Js.truthy(optionalDependencies)) {
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
        if (resolution == null || resolution == JsYaml.UNDEFINED) {
            throw new JsError("Cannot read properties of " + Js.string(resolution) + " (reading 'integrity')");
        }
        Object integrity = resolution instanceof Map ?
                ((Map<String, @Nullable Object>) resolution).get("integrity") : null;
        if (!Js.truthy(integrity)) {
            throw new PnpmError("ENV_LOCKFILE_CORRUPTED", "pnpm-lock.yaml is corrupted or incomplete: missing " +
                                                          "integrity for \"" + pkgKey + "\"");
        }
    }
}
