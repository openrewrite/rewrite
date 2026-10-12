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
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.function.Supplier;

/**
 * Port of pnpm/src syncEnvLockfile.ts (pnpm 11.2.2).
 */
final class SyncEnvLockfile {

    private SyncEnvLockfile() {
    }

    /**
     * The release pnpm records into packageManagerDependencies with {@code resolvePackageManagerIntegrities}, the one
     * that runs; null where it records nothing. Before 11.1.3 it records only over an env document whose pnpm entry
     * the wanted range does not satisfy ({@link PnpmRelease#recordsTheReleaseAnEnvDocumentLacks}).
     */
    static @Nullable String syncEnvLockfile(PnpmOptions.WantedPackageManager pm,
                                            Supplier<@Nullable EnvLockfile> envLockfile, PnpmRelease running) {
        if (!"pnpm".equals(pm.getName()) || pm.getVersion() == null || !SwitchCliVersion.shouldPersistLockfile(pm)) {
            return null;
        }
        String version = Js.string(pm.getVersion());
        String current = running.version();
        if (!running.underSemver(semver -> NodeSemver.satisfies(current, version,
                NodeSemver.Options.INCLUDE_PRERELEASE.release(semver)))) {
            return null;
        }
        EnvLockfile locked = envLockfile.get();
        Object lockedVersion = EnvInstaller.lockedVersion(locked == null ? null : locked.packageManagerDependencies(),
                "pnpm");
        boolean hasLockedVersion = lockedVersion != null && lockedVersion != Js.UNDEFINED;
        if ((locked == null || !hasLockedVersion) && !running.recordsTheReleaseAnEnvDocumentLacks()) {
            return null;
        }
        if (hasLockedVersion && running.underSemver(semver -> NodeSemver.satisfies(lockedVersion, version,
                NodeSemver.Options.INCLUDE_PRERELEASE.release(semver)))) {
            return null;
        }
        return current;
    }

    /**
     * {@link #syncEnvLockfile(PnpmOptions.WantedPackageManager, Supplier, PnpmRelease)} with pnpm 11.2.2 running.
     */
    static @Nullable String syncEnvLockfile(PnpmOptions.WantedPackageManager pm,
                                            Supplier<@Nullable EnvLockfile> envLockfile) {
        return syncEnvLockfile(pm, envLockfile, PnpmRelease.PORTED);
    }
}
