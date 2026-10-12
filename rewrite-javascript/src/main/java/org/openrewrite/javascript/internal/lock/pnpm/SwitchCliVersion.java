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
        if (lockFile == null && persistLockfile &&
            NodeSemver.satisfies(current, version, NodeSemver.Options.INCLUDE_PRERELEASE)) {
            PnpmError failure = stores.storeControllerFailure(options);
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
        if (!Js.truthy(pmVersion) || !NodeSemver.satisfies(pmVersion, version, NodeSemver.Options.INCLUDE_PRERELEASE)) {
            storeToUse = stores.createStoreController(options);
            if (!NodeSemver.satisfies(current, version, NodeSemver.Options.INCLUDE_PRERELEASE) && !follows(version)) {
                throw switchesTo(version);
            }
            EnvLockfile resolved = envInstaller.resolvePackageManagerIntegrities(version, envLockfile,
                    persistLockfile, rootLockFile, storeToUse, options);
            pmVersion = EnvInstaller.lockedVersion(resolved.packageManagerDependencies(), "pnpm");
            if (!Js.truthy(pmVersion)) {
                // pnpm warns that it cannot resolve the release and goes on
                return new Switched(current, null);
            }
        } else if (!EnvInstaller.isPackageManagerResolved(envLockfile, Js.string(pmVersion))) {
            storeToUse = stores.createStoreController(options);
            if (!(current.equals(pmVersion) || follows(Js.string(pmVersion)))) {
                throw switchesTo(Js.string(pmVersion));
            }
            envInstaller.resolvePackageManagerIntegrities(Js.string(pmVersion), envLockfile, persistLockfile,
                    rootLockFile, storeToUse, options);
        }
        if (current.equals(pmVersion)) {
            return new Switched(current, null);
        }
        if (follows(Js.string(pmVersion))) {
            // a root lock the inputs do not hold may pin another release of the range than the one resolved here
            return new Switched(persistLockfile && lockFile == null ? version : Js.string(pmVersion), null);
        }
        if (storeToUse == null) {
            stores.createStoreController(options);
        }
        throw switchesTo(Js.string(pmVersion));
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
