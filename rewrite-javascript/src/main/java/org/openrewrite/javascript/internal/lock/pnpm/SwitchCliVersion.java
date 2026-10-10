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

import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.semver.NodeSemver;

/**
 * Port of pnpm/src switchCliVersion.ts and shouldPersistLockfile.ts (pnpm 11.2.2): the release that installs, which
 * the engine follows only when it is the ported one.
 */
final class SwitchCliVersion {

    private final EnvInstaller envInstaller;
    private final CreateNewStoreController stores;
    private final EnvLockfile.OnDisk lockFile;
    private final String lockDir;

    SwitchCliVersion(EnvInstaller envInstaller, CreateNewStoreController stores, EnvLockfile.OnDisk lockFile,
                     String lockDir) {
        this.envInstaller = envInstaller;
        this.stores = stores;
        this.lockFile = lockFile;
        this.lockDir = lockDir;
    }

    void switchCliVersion(PnpmOptions.WantedPackageManager pm, PnpmOptions options, String rootProjectManifestDir) {
        if (!"pnpm".equals(pm.getName()) || pm.getVersion() == null) {
            return;
        }
        String version = Js.string(pm.getVersion());
        boolean persistLockfile = shouldPersistLockfile(pm);
        if (!persistLockfile && Main.PNPM_VERSION.equals(version)) {
            return;
        }
        // readEnvLockfile(rootProjectManifestDir): another lock's env document, which the inputs do not hold; only when
        // it records the running release resolved does pnpm go on without a store controller
        boolean ownEnvDocument = lockDir.equals(rootProjectManifestDir);
        if (!ownEnvDocument && persistLockfile &&
            NodeSemver.satisfies(Main.PNPM_VERSION, version, NodeSemver.Options.INCLUDE_PRERELEASE)) {
            if (stores.createStoreControllerUnlessLocked(options) && !Main.PNPM_VERSION.equals(version)) {
                throw new CannotKnow("which pnpm release the workspace's " + LockfileReader.WANTED_LOCKFILE +
                                     " pins for " + version + " (pnpm runs that release)");
            }
            return;
        }
        boolean save = persistLockfile && ownEnvDocument;
        EnvLockfile envLockfile = save ? lockFile.readEnvLockfile() : null;
        Object pmVersion = EnvInstaller.lockedVersion(
                envLockfile == null ? null : envLockfile.packageManagerDependencies(), "pnpm");
        NpmResolver storeToUse = null;
        // the release pnpm hands over to is the locked one, else one the registry has in range: when that cannot be
        // the ported release, resolving it only names another
        if (!Js.truthy(pmVersion) || !NodeSemver.satisfies(pmVersion, version, NodeSemver.Options.INCLUDE_PRERELEASE)) {
            storeToUse = stores.createStoreController(options);
            if (!NodeSemver.satisfies(Main.PNPM_VERSION, version, NodeSemver.Options.INCLUDE_PRERELEASE)) {
                throw switchesTo(version);
            }
            EnvLockfile resolved = envInstaller.resolvePackageManagerIntegrities(version, envLockfile, save,
                    lockFile, storeToUse, options);
            pmVersion = EnvInstaller.lockedVersion(resolved.packageManagerDependencies(), "pnpm");
            if (!Js.truthy(pmVersion)) {
                // pnpm warns that it cannot resolve the release and goes on
                return;
            }
        } else if (!EnvInstaller.isPackageManagerResolved(envLockfile, Js.string(pmVersion))) {
            storeToUse = stores.createStoreController(options);
            if (!Main.PNPM_VERSION.equals(Js.string(pmVersion))) {
                throw switchesTo(Js.string(pmVersion));
            }
            envInstaller.resolvePackageManagerIntegrities(Js.string(pmVersion), envLockfile, save, lockFile,
                    storeToUse, options);
        }
        if (Main.PNPM_VERSION.equals(Js.string(pmVersion))) {
            return;
        }
        if (storeToUse == null) {
            stores.createStoreController(options);
        }
        throw switchesTo(Js.string(pmVersion));
    }

    static CannotKnow switchesTo(String release) {
        return new CannotKnow("the lock pnpm " + release + " writes (pnpm " + Main.PNPM_VERSION + " hands the " +
                              "install to the release the project names)");
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
