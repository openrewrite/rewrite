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
import org.junit.jupiter.params.provider.ValueSource;
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

    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");

    private static void switchCliVersion(PnpmOptions.WantedPackageManager pm, String lock) {
        switchCliVersion(pm, lock, null, "");
    }

    // a member's lock (lockDir other than the root) reads the root's lock, which the inputs do not hold here
    private static @Nullable PnpmError switchCliVersion(PnpmOptions.WantedPackageManager pm, String lock,
                                                        @Nullable String workspaceManifest, String lockDir) {
        return new SwitchCliVersion(new EnvInstaller(NO_REGISTRY, NOW, Host.UNKNOWN),
          new CreateNewStoreController(NO_REGISTRY, NOW))
          .switchCliVersion(pm, options(workspaceManifest), lockDir.isEmpty() ? new EnvLockfile.OnDisk(lock) : null);
    }

    private static PnpmOptions options(@Nullable String workspaceManifest) {
        return PnpmOptions.load(workspaceManifest, null, null, Collections.emptyMap());
    }

    /**
     * The release pnpm 11.2.2 hands the install to, when it is no pnpm 11 release, is cannot-know before the registry
     * is asked for it: an exact pin, a range without 11.2.2, a locked release the range admits, or a member's range.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"9.15.0|false||", "10.18.3|false||", "^10.0.0|true||", ">=12|true||",
      "^10.0.0|true|10.18.3|", "^10.0.0|true||packages/a"})
    void anotherReleaseIsCannotKnowBeforeAnyResolution(String version, boolean fromDevEngines,
                                                       @Nullable String locked, @Nullable String lockDir) {
        assertThatThrownBy(() -> switchCliVersion(pm(version, fromDevEngines),
          locked == null ? MAIN_DOCUMENT : envDocument(locked), null, lockDir == null ? "" : lockDir))
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
     * Another release from 11.0.6 runs as 11.2.2: a pin needs no switch, and a member's range of them none either.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"11.3.0|false|", "11.28.5|false|", "^11.0.6|true|packages/a"})
    void aPnpm11ReleaseRunsAsThePortedOne(String version, boolean fromDevEngines, @Nullable String lockDir) {
        assertThat(switchCliVersion(pm(version, fromDevEngines), MAIN_DOCUMENT, null, lockDir == null ? "" : lockDir))
          .isNull();
    }

    /**
     * A pnpm 11 release the range locks is resolved as 11.2.2 would be resolved: the registry decides.
     */
    @Test
    void aLockedPnpm11ReleaseIsResolved() {
        assertThatThrownBy(() -> switchCliVersion(pm("^11.0.0", true), envDocument("11.3.0")))
          .hasMessageContaining("fetched https://registry.npmjs.org/pnpm");
    }

    private static final String BAD_NAMED_REGISTRY = "namedRegistries:\n  foo: ftp://example.com/\n";

    /**
     * Recorded with pnpm 11.2.2 for a pinned other release, a devEngines range and a member's devEngines range, each
     * beside a bad named registry: every way to another release checks first.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"10.18.3|false||", "^10.0.0|true||", "^11.0.0|true|11.3.0|",
      "^10.0.0|true|10.18.3|", "^10.0.0|true||packages/a"})
    void theStoreControllerBeforeAnotherReleaseChecksTheNamedRegistries(String version, boolean fromDevEngines,
                                                                       @Nullable String locked,
                                                                       @Nullable String lockDir) {
        assertThatThrownBy(() -> switchCliVersion(pm(version, fromDevEngines),
          locked == null ? MAIN_DOCUMENT : envDocument(locked), BAD_NAMED_REGISTRY, lockDir == null ? "" : lockDir))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_NAMED_REGISTRY_URL"));
    }

    @Test
    void theRunningReleaseNamedExactlyCreatesNoStoreController() {
        switchCliVersion(pm("11.2.2", false), MAIN_DOCUMENT, BAD_NAMED_REGISTRY, "");
    }

    /**
     * A member's own lock: the workspace root's env document decides whether switchCliVersion creates a store
     * controller; installDeps creates one either way (open-ws-unshared-member-dev-engines-pm-locked-, -unlocked-).
     */
    @ParameterizedTest
    @ValueSource(strings = {"11.2.2", "^11.0.0"})
    void aMembersStoreControllerFailsAtTheNextOne(String version) {
        PnpmError unlessLocked = switchCliVersion(pm(version, true), MAIN_DOCUMENT, BAD_NAMED_REGISTRY, "packages/a");
        assertThat(unlessLocked).isNotNull();
        assertThatThrownBy(() -> new CreateNewStoreController(NO_REGISTRY, NOW)
          .createStoreController(options(BAD_NAMED_REGISTRY)))
          .isInstanceOfSatisfying(PnpmError.class, next -> {
              assertThat(next.getCode()).isEqualTo(unlessLocked.getCode())
                .isEqualTo("ERR_PNPM_INVALID_NAMED_REGISTRY_URL");
              assertThat(member(BAD_NAMED_REGISTRY, null).beforeNextStoreController(next)).isSameAs(next);
          });
    }

    /**
     * open-ws-unshared-member-dev-engines-pm-locked-bad-named-registry-missing-pnpmfile and its -unlocked- twin: what
     * fails before installDeps' store controller fails first only when the root's lock records the release.
     */
    @Test
    void aFailureBeforeTheNextStoreControllerIsEitherOne() {
        PnpmError missing = new PnpmError("PNPMFILE_NOT_FOUND", "pnpmfile at \"missing.cjs\" is not found");
        assertThat(member(BAD_NAMED_REGISTRY, null).beforeNextStoreController(missing))
          .isInstanceOfSatisfying(CannotKnow.class, e -> assertThat(e.getSubject())
            .contains("ERR_PNPM_INVALID_NAMED_REGISTRY_URL", "ftp://example.com/", "ERR_PNPM_PNPMFILE_NOT_FOUND"));
        assertThat(member(null, null).beforeNextStoreController(missing)).isSameAs(missing);
    }

    /**
     * The root's lock the inputs hold decides: locked and resolved, no store controller; unlocked, the store
     * controller pnpm creates, whose failure is pnpm's (the -locked- and -unlocked- missing-pnpmfile recordings).
     */
    @Test
    void aMembersSwitchReadsTheRootsLockTheInputsHold() {
        assertThatThrownBy(() -> member(BAD_NAMED_REGISTRY, MAIN_DOCUMENT))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_INVALID_NAMED_REGISTRY_URL"));
        PnpmError missing = new PnpmError("PNPMFILE_NOT_FOUND", "pnpmfile at \"missing.cjs\" is not found");
        String resolved = "---\nlockfileVersion: '9.0'\n\nimporters:\n\n  .:\n    configDependencies: {}\n" +
                          "    packageManagerDependencies:\n      '@pnpm/exe':\n        specifier: 11.2.2\n" +
                          "        version: 11.2.2\n      pnpm:\n        specifier: 11.2.2\n" +
                          "        version: 11.2.2\n\n" +
                          "packages: {}\n\nsnapshots: {}\n\n---\n" + MAIN_DOCUMENT;
        assertThat(member(BAD_NAMED_REGISTRY, resolved).beforeNextStoreController(missing)).isSameAs(missing);
    }

    /**
     * A member's lock where the root's lock records no release: switchCliVersion, or syncEnvLockfile under onFail
     * error, resolves pnpm's integrities into the root's env document, which asks the registry.
     */
    @ParameterizedTest
    @ValueSource(strings = {"download", "error"})
    void aMembersPackageManagerCheckResolvesIntoTheRootsLock(String onFail) {
        assertThatThrownBy(() -> member(null, MAIN_DOCUMENT, onFail))
          .hasMessageContaining("fetched https://registry.npmjs.org/pnpm");
    }

    private static Main member(@Nullable String workspaceSettings, @Nullable String rootLock) {
        return member(workspaceSettings, rootLock, "download");
    }

    // packages/a's lock under a root pinning pnpm 11.2.2 in devEngines, after the package manager check
    private static Main member(@Nullable String workspaceSettings, @Nullable String rootLock, String onFail) {
        PnpmLockRegenerator.Inputs.InputsBuilder inputs = PnpmLockRegenerator.Inputs.builder()
          .lock(MAIN_DOCUMENT)
          .lockDir("packages/a")
          .manifest("", "{\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\",\"version\":\"11.2.2\"," +
                        "\"onFail\":\"" + onFail + "\"}}}")
          .manifest("packages/a", "{\"name\":\"a\"}")
          .file("pnpm-workspace.yaml", "packages:\n  - packages/*\nsharedWorkspaceLockfile: false\n" +
                                       (workspaceSettings == null ? "" : workspaceSettings))
          .now(NOW);
        if (rootLock != null) {
            inputs.file("pnpm-lock.yaml", rootLock);
        }
        PnpmLockRegenerator.Inputs built = inputs.build();
        EnvLockfile.OnDisk lockFile = new EnvLockfile.OnDisk(MAIN_DOCUMENT);
        Main main = new Main(built, new EnvInstaller(NO_REGISTRY, NOW, Host.UNKNOWN),
          new CreateNewStoreController(NO_REGISTRY, NOW), lockFile);
        main.checkWantedPackageManager(main.config());
        return main;
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
