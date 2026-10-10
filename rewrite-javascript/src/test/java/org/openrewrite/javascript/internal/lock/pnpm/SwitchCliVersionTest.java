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

class SwitchCliVersionTest {

    /**
     * pnpm 11.2.2's pnpm/src/shouldPersistLockfile.test.ts.
     */
    @Test
    void shouldPersistLockfile() {
        for (String version : new String[]{"9.3.0", "11.0.0", "12.0.0", ">=9.0.0"}) {
            assertThat(SwitchCliVersion.shouldPersistLockfile(pm(version, true))).as(version).isTrue();
        }
        for (String version : new String[]{"9.3.0", "10.0.0", "11.0.0", "11.0.0-rc.1"}) {
            assertThat(SwitchCliVersion.shouldPersistLockfile(pm(version, false))).as(version).isFalse();
        }
        for (String version : new String[]{"12.0.0", "12.5.3", "13.0.0", "100.0.0"}) {
            assertThat(SwitchCliVersion.shouldPersistLockfile(pm(version, false))).as(version).isTrue();
        }
        for (String version : new String[]{null, "not-a-version", "^12.0.0"}) {
            assertThat(SwitchCliVersion.shouldPersistLockfile(pm(version, false))).as(version).isFalse();
        }
    }

    private static PnpmOptions.WantedPackageManager pm(@Nullable String version, boolean fromDevEngines) {
        return new PnpmOptions.WantedPackageManager("pnpm", version, "download", fromDevEngines);
    }
}
