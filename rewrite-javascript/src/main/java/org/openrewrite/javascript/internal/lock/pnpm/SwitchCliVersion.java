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
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Port of pnpm/src switchCliVersion.ts and shouldPersistLockfile.ts (pnpm 11.2.2): the release that installs. Where
 * the host does not say which release runs, 11.2.2 does, and the engine follows it to the release it hands the
 * install to when that is one the engine reproduces ({@link Main#runsAsPorted}), with what the ported modules do from
 * that release on. Where the release that built the LST runs, a hand-over to another is cannot-know: that release
 * runs over what the first left behind (its env document among it), which no recording holds.
 */
final class SwitchCliVersion {

    /**
     * What the switch leaves for the install that follows.
     */
    @Value
    static class Switched {
        /**
         * The release that installs: the one pnpm hands the install to, this one where it does not switch; the range
         * the project names where the inputs do not say which release of it the root's lock pins.
         */
        String release;

        /**
         * The failure of a store controller pnpm creates unless the root's lock records the release, which pnpm ends
         * with either way; null when it creates none or the inputs say what that lock holds.
         */
        @Nullable
        PnpmError unlessLocked;
    }

    private final EnvInstaller envInstaller;
    private final CreateNewStoreController stores;

    /**
     * The release that runs, the one that built the LST; null where the host recorded none.
     */
    private final @Nullable PnpmRelease runs;

    SwitchCliVersion(EnvInstaller envInstaller, CreateNewStoreController stores) {
        this(envInstaller, stores, null);
    }

    SwitchCliVersion(EnvInstaller envInstaller, CreateNewStoreController stores, @Nullable PnpmRelease runs) {
        this.envInstaller = envInstaller;
        this.stores = stores;
        this.runs = runs;
    }

    // @pnpm/cli.meta's packageManager.version of the release that runs
    private String current() {
        return runs == null ? Main.PNPM_VERSION : runs.version();
    }

    // whether the engine follows pnpm to version, a release or a range it would hand the install to: only where the
    // host does not say which release runs, and every release it names is one the engine reproduces
    private boolean follows(String version) {
        return runs == null && Main.runsAsPorted(version);
    }

    /**
     * The switch over the lock file of rootProjectManifestDir, null when the inputs do not say what it holds.
     */
    Switched switchCliVersion(PnpmOptions.WantedPackageManager pm, PnpmOptions options,
                              EnvLockfile.@Nullable OnDisk lockFile) {
        String current = current();
        if (!"pnpm".equals(pm.getName()) || pm.getVersion() == null) {
            return new Switched(current, null);
        }
        String version = Js.string(pm.getVersion());
        boolean persistLockfile = shouldPersistLockfile(pm);
        if (!persistLockfile && (version.equals(current) || follows(version))) {
            return new Switched(version, null);
        }
        // the store controller and the resolution of pnpm itself take the release's bootstrap config
        Supplier<PnpmOptions> bootstrap = () -> options.packageManagerBootstrap(running());
        if (lockFile == null && persistLockfile &&
            satisfies(current, version)) {
            PnpmError failure = stores.storeControllerFailure(bootstrap.get());
            if (failure == null && !(version.equals(current) || follows(version))) {
                throw new CannotKnow("which pnpm release the workspace's " + LockfileReader.WANTED_LOCKFILE +
                                     " pins for " + version + " (pnpm runs that release)");
            }
            return new Switched(version, failure);
        }
        EnvLockfile.OnDisk rootLockFile = lockFile == null ? new EnvLockfile.OnDisk(null) : lockFile;
        EnvLockfile envLockfile = persistLockfile ? rootLockFile.readEnvLockfile() : null;
        Object pmVersion = EnvInstaller.lockedVersion(
                envLockfile == null ? null : envLockfile.packageManagerDependencies(), "pnpm");
        NpmResolver storeToUse = null;
        // the release pnpm hands over to is the locked one, else one the registry has in range: when that cannot be
        // the release that runs, resolving it only names another
        if (!Js.truthy(pmVersion) || !satisfies(pmVersion, version)) {
            storeToUse = stores.createStoreController(bootstrap.get());
            if (!satisfies(current, version) && !follows(version) && !mayNameARefusedRelease(version)) {
                throw switchesTo(version);
            }
            envLockfile = envInstaller.resolvePackageManagerIntegrities(version, envLockfile,
                    persistLockfile, rootLockFile, storeToUse, bootstrap.get());
            pmVersion = EnvInstaller.lockedVersion(envLockfile.packageManagerDependencies(), "pnpm");
            if (!Js.truthy(pmVersion)) {
                // pnpm warns that it cannot resolve the release and goes on
                return new Switched(current, null);
            }
        } else if (!EnvInstaller.isPackageManagerResolved(envLockfile, Js.string(pmVersion))) {
            storeToUse = stores.createStoreController(bootstrap.get());
            if (!(current.equals(pmVersion) || follows(Js.string(pmVersion)) ||
                  refusesAsBroken(Js.string(pmVersion)))) {
                throw switchesTo(Js.string(pmVersion));
            }
            envLockfile = envInstaller.resolvePackageManagerIntegrities(Js.string(pmVersion), envLockfile,
                    persistLockfile, rootLockFile, storeToUse, bootstrap.get());
        }
        if (current.equals(pmVersion)) {
            return new Switched(current, null);
        }
        if (follows(Js.string(pmVersion))) {
            // a root lock the inputs do not hold may pin another release of the range than the one resolved here
            return new Switched(persistLockfile && lockFile == null ? version : Js.string(pmVersion), null);
        }
        // from 11.14.0 a broken release is refused, its env document saved already where the release was resolved
        // (PnpmRelease#refusesToSwitchToABrokenRelease)
        if (refusesAsBroken(Js.string(pmVersion))) {
            throw new PnpmError("BROKEN_PNPM_RELEASE", "pnpm v" + pmVersion + " is a broken release and cannot " +
                    "be installed", "Its \"@pnpm/exe\" build shipped without a binary and does not run. Even where " +
                    "it does run, pinning it would break everyone on the project who uses \"@pnpm/exe\", because " +
                    "the pin is shared. Choose another version, or run \"pnpm self-update latest\".");
        }
        // from 11.5.3 the env document's packageManagerDependencies must be registry packages that record only an
        // integrity, ahead of the store controller (PnpmRelease#checksTheEnvDocumentBeforeSwitching)
        if (envLockfile != null && running().checksTheEnvDocumentBeforeSwitching()) {
            assertPackageManagerLockfileUsesRegistryResolutions(envLockfile);
        }
        if (storeToUse == null) {
            stores.createStoreController(bootstrap.get());
        }
        throw switchesTo(Js.string(pmVersion));
    }

    // the release that runs: the one that built the LST, else pnpm's own
    private PnpmRelease running() {
        return runs == null ? PnpmRelease.PORTED : runs;
    }

    // engine/pm installPnpm.ts' BROKEN_RELEASES (11.14.0, the same through 11.16.0)
    private static final List<String> BROKEN_RELEASES = Arrays.asList("11.12.0", "11.13.0");

    // assertReleaseIsInstallable: whether the release that runs refuses to hand the install to version as a broken
    // release, from 11.14.0 (PnpmRelease#refusesToSwitchToABrokenRelease), asked only of a broken one
    private boolean refusesAsBroken(String version) {
        return BROKEN_RELEASES.contains(version) && running().refusesToSwitchToABrokenRelease();
    }

    // whether the range may resolve to a release the one that runs refuses as broken, which only resolving it tells
    private boolean mayNameARefusedRelease(String range) {
        for (String broken : BROKEN_RELEASES) {
            if (satisfies(broken, range)) {
                return running().refusesToSwitchToABrokenRelease();
            }
        }
        return false;
    }

    // semver.satisfies(version, range, { includePrerelease: true }) of the node-semver the release that runs bundles
    private boolean satisfies(Object version, String range) {
        return running().underSemver(semver -> NodeSemver.satisfies(version, range,
                NodeSemver.Options.INCLUDE_PRERELEASE.release(semver)));
    }

    /**
     * pnpm/src packageManagerLockfile.ts (11.5.3): each package the env document's packageManagerDependencies reach is
     * a registry package whose resolution is an integrity alone.
     */
    static void assertPackageManagerLockfileUsesRegistryResolutions(EnvLockfile envLockfile) {
        Map<String, @Nullable Object> packageManagerDependencies = envLockfile.packageManagerDependencies();
        if (packageManagerDependencies == null) {
            throw new PnpmError("INVALID_PACKAGE_MANAGER_LOCKFILE",
                    "The packageManager dependencies were not found in pnpm-lock.yaml");
        }
        Set<String> visited = new HashSet<>();
        for (String name : Js.keys(packageManagerDependencies)) {
            Object dependency = packageManagerDependencies.get(name);
            Object reference = dependency instanceof Map ? ((Map<?, ?>) dependency).get("version") : null;
            if (!(reference instanceof String)) {
                throw new CannotKnow("what pnpm makes of the env document's packageManagerDependencies." + name +
                                     " (no version string)");
            }
            String depPath = DepPath.refToRelative((String) reference, name);
            if (depPath == null) {
                throw invalidPackageManagerLockfile(name);
            }
            assertRegistryPackageManagerDependency(envLockfile, depPath, visited);
        }
    }

    @SuppressWarnings("unchecked")
    private static void assertRegistryPackageManagerDependency(EnvLockfile envLockfile, String depPath,
                                                               Set<String> visited) {
        if (!visited.add(depPath)) {
            return;
        }
        Object packageInfo = envLockfile.packages().get(DepPath.removeSuffix(depPath));
        Object snapshot = envLockfile.snapshots().get(depPath);
        if (packageInfo == null || snapshot == null) {
            throw invalidPackageManagerLockfile(depPath);
        }
        if (!(packageInfo instanceof Map) || !(snapshot instanceof Map)) {
            throw new CannotKnow("what pnpm makes of the env document's " + depPath + " (no mapping)");
        }
        Map<String, @Nullable Object> info = (Map<String, @Nullable Object>) packageInfo;
        DepPath.DependencyPath parsed = DepPath.parse(depPath);
        if (parsed.getName() == null || parsed.getVersion() == null || parsed.getNonSemverVersion() != null ||
            info.get("id") != null ||
            info.get("name") != null && !Js.strictEquals(info.get("name"), parsed.getName()) ||
            info.get("version") != null && !Js.strictEquals(info.get("version"), parsed.getVersion())) {
            throw invalidPackageManagerLockfile(depPath);
        }
        Object resolution = info.get("resolution");
        if (!(resolution instanceof Map) || ((Map<String, @Nullable Object>) resolution).size() != 1 ||
            !(((Map<String, @Nullable Object>) resolution).get("integrity") instanceof String) ||
            ((String) ((Map<String, @Nullable Object>) resolution).get("integrity")).isEmpty()) {
            throw invalidPackageManagerLockfile(depPath);
        }
        Map<String, @Nullable Object> children = new LinkedHashMap<>();
        for (String field : new String[]{"dependencies", "optionalDependencies"}) {
            Object deps = ((Map<String, @Nullable Object>) snapshot).get(field);
            if (deps instanceof Map) {
                children.putAll((Map<String, @Nullable Object>) deps);
            }
        }
        for (Map.Entry<String, @Nullable Object> child : children.entrySet()) {
            if (!(child.getValue() instanceof String)) {
                throw new CannotKnow("what pnpm makes of the env document's " + depPath + " " + child.getKey() +
                                     " (no reference string)");
            }
            String nextDepPath = DepPath.refToRelative((String) child.getValue(), child.getKey());
            if (nextDepPath == null) {
                throw invalidPackageManagerLockfile(depPath);
            }
            assertRegistryPackageManagerDependency(envLockfile, nextDepPath, visited);
        }
    }

    private static PnpmError invalidPackageManagerLockfile(String depPath) {
        return new PnpmError("INVALID_PACKAGE_MANAGER_LOCKFILE", "The packageManager dependency \"" + depPath +
                "\" in pnpm-lock.yaml must use a registry package path and an integrity-only resolution");
    }

    /**
     * The hand-over to {@code release} the engine does not follow.
     */
    CannotKnow switchesTo(String release) {
        if (runs != null) {
            return new CannotKnow("the pnpm release that installs the project: pnpm " + release + ", which pnpm " +
                                  runs + " downloads and hands the install over to");
        }
        return new CannotKnow("the lock pnpm " + release + " writes (pnpm " + Main.PNPM_VERSION + " hands the " +
                              "install to the release the project names, and the engine reproduces pnpm " +
                              PnpmReleases.ranges(PnpmReleases.proven()) + ")");
    }

    static boolean shouldPersistLockfile(PnpmOptions.WantedPackageManager pm) {
        if (pm.isFromDevEngines()) {
            return true;
        }
        if (pm.getVersion() == null) {
            return false;
        }
        String valid = NodeSemver.valid(Js.string(pm.getVersion()), NodeSemver.Options.DEFAULT);
        return valid != null && Long.parseLong(valid.substring(0, valid.indexOf('.'))) >= 12;
    }
}
