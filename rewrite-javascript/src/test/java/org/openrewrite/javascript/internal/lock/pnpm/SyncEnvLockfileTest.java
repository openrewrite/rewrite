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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SyncEnvLockfileTest {

    /**
     * pnpm 11.2.2's pnpm/src/syncEnvLockfile.test.ts: the version pnpm records into packageManagerDependencies, null
     * where it records nothing. Its first case, no wanted package manager, is every install without one.
     */
    @Test
    void syncEnvLockfile() {
        assertThat(SyncEnvLockfile.syncEnvLockfile(new PnpmOptions.WantedPackageManager("yarn", "4.0.0", null,
          true), () -> null)).isNull();
        assertThat(SyncEnvLockfile.syncEnvLockfile(pm("11.0.0", false), () -> staleEnvLockfile("9.0.0"))).isNull();
        assertThat(SyncEnvLockfile.syncEnvLockfile(pm("0.0.1", true), () -> staleEnvLockfile("9.0.0"))).isNull();
        assertThat(SyncEnvLockfile.syncEnvLockfile(pm("11.2.2", true), () -> null)).isEqualTo("11.2.2");
        assertThat(SyncEnvLockfile.syncEnvLockfile(pm("11.2.2", true), () -> EnvLockfile.readEnvLockfile(
          "---\nlockfileVersion: '9.0'\nimporters:\n  '.':\n    configDependencies: {}\npackages: {}\n" +
          "snapshots: {}\n\n---\n"))).isEqualTo("11.2.2");
        assertThat(SyncEnvLockfile.syncEnvLockfile(pm("11.2.2", true), () -> staleEnvLockfile("11.2.2"))).isNull();
        assertThat(SyncEnvLockfile.syncEnvLockfile(pm("11.2.2", true), () -> staleEnvLockfile("9.0.0")))
          .isEqualTo("11.2.2");
    }

    private static PnpmOptions.WantedPackageManager pm(@Nullable String version, boolean fromDevEngines) {
        return new PnpmOptions.WantedPackageManager("pnpm", version, "download", fromDevEngines);
    }

    private static @Nullable EnvLockfile staleEnvLockfile(String pnpmVersion) {
        return EnvLockfile.readEnvLockfile("---\nlockfileVersion: '9.0'\nimporters:\n  '.':\n" +
                                           "    configDependencies: {}\n    packageManagerDependencies:\n" +
                                           "      pnpm:\n        specifier: " + pnpmVersion + "\n" +
                                           "        version: " + pnpmVersion + "\n" +
                                           "packages: {}\nsnapshots: {}\n\n---\n");
    }
}
