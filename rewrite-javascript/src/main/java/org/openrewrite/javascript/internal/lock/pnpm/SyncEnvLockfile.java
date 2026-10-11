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
     * The release pnpm records into packageManagerDependencies with {@code resolvePackageManagerIntegrities}; null
     * where it records nothing.
     */
    static @Nullable String syncEnvLockfile(PnpmOptions.WantedPackageManager pm,
                                            Supplier<@Nullable EnvLockfile> envLockfile) {
        if (!"pnpm".equals(pm.getName()) || pm.getVersion() == null || !SwitchCliVersion.shouldPersistLockfile(pm)) {
            return null;
        }
        String version = Js.string(pm.getVersion());
        if (!NodeSemver.satisfies(Main.PNPM_VERSION, version, NodeSemver.Options.INCLUDE_PRERELEASE)) {
            return null;
        }
        EnvLockfile locked = envLockfile.get();
        Object lockedVersion = EnvInstaller.lockedVersion(locked == null ? null : locked.packageManagerDependencies(),
                "pnpm");
        if (lockedVersion != null && lockedVersion != Js.UNDEFINED &&
            NodeSemver.satisfies(lockedVersion, version, NodeSemver.Options.INCLUDE_PRERELEASE)) {
            return null;
        }
        return Main.PNPM_VERSION;
    }
}
