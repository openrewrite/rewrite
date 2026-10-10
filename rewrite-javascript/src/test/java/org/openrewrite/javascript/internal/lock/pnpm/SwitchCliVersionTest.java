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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;

import java.time.Instant;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

class SwitchCliVersionTest {

    private static final RegistryMetadata NO_REGISTRY = new RegistryMetadata() {
        @Override
        public @Nullable String packument(String registry, String name, boolean fullMetadata) {
            return fail("fetched " + registry + name);
        }

        @Override
        public @Nullable String attestations(String registry, String name, String version) {
            return fail("fetched the attestations of " + name + "@" + version);
        }

        @Override
        public @Nullable String document(String url) {
            return fail("fetched " + url);
        }
    };

    private static final String MAIN_DOCUMENT = "lockfileVersion: '9.0'\n\nimporters:\n\n  .: {}\n";

    private static String envDocument(String pnpmVersion) {
        return "---\nlockfileVersion: '9.0'\n\nimporters:\n\n  .:\n    configDependencies: {}\n" +
               "    packageManagerDependencies:\n      pnpm:\n        specifier: " + pnpmVersion + "\n" +
               "        version: " + pnpmVersion + "\n\npackages: {}\n\nsnapshots: {}\n\n---\n" + MAIN_DOCUMENT;
    }

    private static void switchCliVersion(PnpmOptions.WantedPackageManager pm, String lock) {
        EnvLockfile.OnDisk lockFile = new EnvLockfile.OnDisk(lock);
        new SwitchCliVersion(new EnvInstaller(NO_REGISTRY, Instant.parse("2026-10-08T00:00:00Z"), Host.UNKNOWN),
          lockFile, "").switchCliVersion(pm, PnpmOptions.load(null, null, null, Collections.emptyMap()), "");
    }

    /**
     * The release pnpm 11.2.2 hands the install to, when it cannot be 11.2.2, is cannot-know before the registry is
     * asked for it: an exact pin, a range without 11.2.2, or a locked release the range admits.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"9.15.0|false|", "10.18.3|false|", "^10.0.0|true|", ">=12|true|",
      "^11.0.0|true|11.3.0", "^10.0.0|true|10.18.3"})
    void anotherReleaseIsCannotKnowBeforeAnyResolution(String version, boolean fromDevEngines,
                                                       @Nullable String locked) {
        assertThatThrownBy(() -> switchCliVersion(pm(version, fromDevEngines),
          locked == null ? MAIN_DOCUMENT : envDocument(locked)))
          .isInstanceOfSatisfying(CannotKnow.class,
            e -> assertThat(e.getSubject()).contains(locked == null ? version : locked));
    }

    /**
     * A range that admits 11.2.2 with no release locked for it is resolved: the registry decides.
     */
    @Test
    void aRangeThatAdmitsThePortedReleaseIsResolved() {
        assertThatThrownBy(() -> switchCliVersion(pm("^11.0.0", true), MAIN_DOCUMENT))
          .hasMessageContaining("fetched https://registry.npmjs.org/pnpm");
    }

    @Test
    void thePortedReleaseNeedsNoSwitch() {
        switchCliVersion(pm("11.2.2", false), MAIN_DOCUMENT);
    }

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
