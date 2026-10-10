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
import org.openrewrite.javascript.internal.lock.SpecUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * pnpm 11.2.2's pnpm/test/packageManagerCheck.test.ts through a lock-only install, with the environment's form of
 * {@code --pm-on-fail}; less the commands other than install, the CLI flags and the warnings pnpm prints.
 */
class MainTest {

    private static final String LOCK = "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: true\n" +
                                       "  excludeLinksFromLockfile: false\n\nimporters:\n\n  .:\n    dependencies:\n" +
                                       "      wrappy:\n        specifier: 1.0.2\n        version: 1.0.2\n\n" +
                                       "packages:\n\n  wrappy@1.0.2:\n    resolution: {integrity: sha512-" +
                                       "l4Sp/DRseor9wL6EvV2+TuQn63dMkPjZ/sp9XkghTEbV9KlPS1xUsZ3u7/IQO4wxtcFB4bgpQPRc" +
                                       "R3QCvezPcQ==}" +
                                       "\n\nsnapshots:\n\n  wrappy@1.0.2: {}\n";

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

    private static String manifest(String fields) {
        return "{\"name\":\"project\",\"version\":\"1.0.0\",\"dependencies\":{\"wrappy\":\"1.0.2\"}," + fields + "}";
    }

    private static String devEngines(String packageManager) {
        return "\"devEngines\":{\"packageManager\":" + packageManager + "}";
    }

    private static Install.Result install(String manifest, @Nullable String workspace, String... env) {
        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("pnpm_config_minimum_release_age", "0");
        for (int i = 0; i < env.length; i += 2) {
            environment.put(env[i], env[i + 1]);
        }
        PnpmLockRegenerator.Inputs.InputsBuilder inputs = PnpmLockRegenerator.Inputs.builder()
          .lock(LOCK)
          .manifest("", manifest)
          .env(environment)
          .now(Instant.parse("2026-10-08T00:00:00Z"));
        if (workspace != null) {
            inputs.file("pnpm-workspace.yaml", workspace);
        }
        return new PnpmLockRegenerator(inputs.build(), NO_REGISTRY).install();
    }

    private static void installs(String manifest, String... env) {
        assertThat(install(manifest, null, env).getWritten()).isEqualTo(LOCK);
    }

    private static void rejects(String manifest, String code, String message, String... env) {
        assertThatThrownBy(() -> install(manifest, null, env))
          .isInstanceOfSatisfying(PnpmError.class, e -> assertThat(e.getCode()).isEqualTo(code))
          .hasMessageContaining(message);
    }

    @Test
    void installShouldFailIfTheUsedPnpmVersionDoesNotSatisfyThePnpmVersionSpecifiedInEngines() {
        assertThatThrownBy(() -> install(manifest("\"engines\":{\"pnpm\":\"99999\"}"), null))
          .isInstanceOfSatisfying(PackageIsInstallable.UnsupportedEngineError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_UNSUPPORTED_ENGINE");
              assertThat(e.getWanted()).containsEntry("pnpm", "99999");
          });
    }

    @Test
    void installShouldNotFailIfTheUsedPnpmVersionDoesNotSatisfyThePnpmVersionSpecifiedInPackageManager() {
        installs(manifest("\"packageManager\":\"pnpm@0.0.0\""), "pnpm_config_pm_on_fail", "ignore");
        rejects(manifest("\"packageManager\":\"pnpm@0.0.0\""), "ERR_PNPM_BAD_PM_VERSION",
          "This project is configured to use 0.0.0 of pnpm. Your current pnpm is", "pnpm_config_pm_on_fail", "error");
    }

    @Test
    void installShouldFailIfTheProjectRequiresADifferentPackageManager() {
        rejects(manifest("\"packageManager\":\"yarn@4.0.0\""), "ERR_PNPM_OTHER_PM_EXPECTED",
          "This project is configured to use yarn");
        installs(manifest("\"packageManager\":\"yarn@4.0.0\""), "pnpm_config_pm_on_fail", "warn");
    }

    @Test
    void installShouldNotFailForPackageManagerFieldWithHash() {
        installs(manifest("\"packageManager\":\"pnpm@" + Main.PNPM_VERSION + "+sha256.123456789\""));
    }

    @Test
    void installShouldNotFailForPackageManagerFieldWithUrl() {
        installs(manifest("\"packageManager\":\"pnpm@https://github.com/pnpm/pnpm\""));
    }

    @Test
    void devEnginesPackageManagerWithOnFailErrorShouldFailOnVersionMismatch() {
        rejects(manifest(devEngines("{\"name\":\"pnpm\",\"version\":\"0.0.1\",\"onFail\":\"error\"}")),
          "ERR_PNPM_BAD_PM_VERSION", "This project is configured to use 0.0.1 of pnpm");
    }

    @Test
    void devEnginesPackageManagerWithOnFailWarnShouldWarnOnVersionMismatch() {
        installs(manifest(devEngines("{\"name\":\"pnpm\",\"version\":\"0.0.1\",\"onFail\":\"warn\"}")));
    }

    @Test
    void devEnginesPackageManagerWithOnFailIgnoreShouldNotCheckVersion() {
        installs(manifest(devEngines("{\"name\":\"pnpm\",\"version\":\"0.0.1\",\"onFail\":\"ignore\"}")));
    }

    @Test
    void devEnginesPackageManagerDefaultsToOnFailDownload() {
        rejects(manifest(devEngines("{\"name\":\"pnpm\",\"version\":\"0.0.1\"}")), "ERR_PNPM_BAD_PM_VERSION",
          "does not switch versions when running under corepack", "COREPACK_ROOT", "/fake/corepack");
        assertThatThrownBy(() -> install(manifest(devEngines("{\"name\":\"pnpm\",\"version\":\"0.0.1\"}")), null))
          .isInstanceOfSatisfying(CannotKnow.class, e -> assertThat(e.getSubject()).contains("0.0.1"));
    }

    @Test
    void devEnginesPackageManagerWithADifferentPmNameShouldFailWithOnFailError() {
        rejects(manifest(devEngines("{\"name\":\"yarn\",\"version\":\">=4.0.0\",\"onFail\":\"error\"}")),
          "ERR_PNPM_OTHER_PM_EXPECTED", "This project is configured to use yarn");
    }

    @Test
    void devEnginesPackageManagerArraySelectsThePnpmEntry() {
        rejects(manifest(devEngines("[{\"name\":\"yarn\",\"version\":\">=4.0.0\",\"onFail\":\"ignore\"}," +
                                    "{\"name\":\"pnpm\",\"version\":\"0.0.1\",\"onFail\":\"error\"}]")),
          "ERR_PNPM_BAD_PM_VERSION", "This project is configured to use 0.0.1 of pnpm");
    }

    @Test
    void devEnginesPackageManagerArrayDefaultsOnFailToIgnoreForNonLastElements() {
        installs(manifest(devEngines("[{\"name\":\"pnpm\",\"version\":\"" + Main.PNPM_VERSION + "\"}," +
                                     "{\"name\":\"yarn\",\"version\":\">=4.0.0\"}]")));
    }

    @Test
    void devEnginesPackageManagerTakesPrecedenceOverPackageManagerField() {
        rejects(manifest("\"packageManager\":\"pnpm@" + Main.PNPM_VERSION + "\"," +
                         devEngines("{\"name\":\"pnpm\",\"version\":\"0.0.1\",\"onFail\":\"error\"}")),
          "ERR_PNPM_BAD_PM_VERSION", "This project is configured to use 0.0.1 of pnpm");
    }

    @Test
    void noWarningWhenPackageManagerAndDevEnginesPackageManagerSpecifyTheSameExactVersion() {
        installs(manifest("\"packageManager\":\"pnpm@1.2.3\"," +
                          devEngines("{\"name\":\"pnpm\",\"version\":\"1.2.3\",\"onFail\":\"ignore\"}")));
    }

    @Test
    void warnsWhenPackageManagerSpecifiesADifferentPackageManagerFromDevEnginesPackageManager() {
        installs(manifest("\"packageManager\":\"yarn@1.2.3\"," +
                          devEngines("{\"name\":\"pnpm\",\"version\":\"1.2.3\",\"onFail\":\"ignore\"}")));
    }

    @Test
    void warnsWhenPackageManagerVersionDoesNotMatchTheDevEnginesPackageManagerVersionStringExactly() {
        installs(manifest("\"packageManager\":\"pnpm@1.2.3\"," +
                          devEngines("{\"name\":\"pnpm\",\"version\":\">=1.0.0\",\"onFail\":\"ignore\"}")));
    }

    @Test
    void pmOnFailIgnoreViaEnvVarBypassesTheDevEnginesPackageManagerCheck() {
        installs(manifest(devEngines("{\"name\":\"pnpm\",\"version\":\"0.0.1\",\"onFail\":\"error\"}")),
          "pnpm_config_pm_on_fail", "ignore");
    }

    @Test
    void devEnginesPackageManagerCheckRunsEvenWhenPnpmIsInvokedViaCorepack() {
        installs(manifest(devEngines("{\"name\":\"pnpm\",\"version\":\"0.0.1\",\"onFail\":\"warn\"}")),
          "COREPACK_ROOT", "/fake/corepack");
    }

    @Test
    void devEnginesPackageManagerOnFailDownloadSurfacesARegularErrorUnderCorepackInsteadOfSwitchingVersions() {
        assertThatThrownBy(() -> install(manifest(devEngines("{\"name\":\"pnpm\",\"version\":\"0.0.1\"," +
                                                             "\"onFail\":\"download\"}")), null,
          "COREPACK_ROOT", "/fake/corepack"))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_BAD_PM_VERSION");
              assertThat(e.getHint()).contains("invoke pnpm directly");
          })
          .hasMessageContaining("This project is configured to use 0.0.1 of pnpm")
          .hasMessageContaining("Corepack invoked pnpm")
          .hasMessageContaining("does not switch versions when running under corepack");
    }

    @Test
    void pmOnFailIgnoreSetInPnpmWorkspaceYamlBypassesTheDevEnginesPackageManagerCheck() {
        assertThat(install(manifest(devEngines("{\"name\":\"pnpm\",\"version\":\"0.0.1\",\"onFail\":\"error\"}")),
          "pmOnFail: ignore\n").getWritten()).isEqualTo(LOCK);
    }

    /**
     * Upstream's release-brittle case: the range admits the running release, which syncEnvLockfile then resolves
     * into packageManagerDependencies from the registry.
     */
    @Test
    void devEnginesPackageManagerWithVersionRangeShouldMatchCurrentVersion() {
        assertThatThrownBy(() -> install(manifest(devEngines("{\"name\":\"pnpm\",\"version\":\">=1.0.0\"," +
                                                             "\"onFail\":\"error\"}")), null))
          .hasMessageContaining("fetched https://registry.npmjs.org/pnpm");
    }

    /**
     * Recorded with corepack 0.35.0: it starts devEngines' 10.34.6 and refuses a range, where pnpm run directly
     * installs with 11.2.2; with the environment unknown the host does not say which runs.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "{\"name\":\"pnpm\",\"version\":\"10.34.6\",\"onFail\":\"ignore\"}|corepack starts pnpm 10.34.6 and pnpm run " +
      "directly installs with 11.2.2",
      "{\"name\":\"pnpm\",\"version\":\">=1.0.0\",\"onFail\":\"error\"}|corepack refuses to start pnpm (Invalid " +
      "package manager specification in package.json (pnpm@>=1.0.0); expected a semver version",
    })
    void corepackMustPickTheReleasePnpmRunDirectlyInstallsWith(String packageManager, String subject) {
        PnpmLockRegenerator.Inputs inputs = PnpmLockRegenerator.Inputs.builder()
          .lock(LOCK)
          .manifest("", manifest(devEngines(packageManager)))
          .now(Instant.parse("2026-10-08T00:00:00Z"))
          .build();
        assertThatThrownBy(() -> new PnpmLockRegenerator(inputs, NO_REGISTRY).install())
          .isInstanceOfSatisfying(CannotKnow.class, e -> assertThat(e.getSubject()).contains(subject));
    }

    @Test
    void theReleaseIsTheOnePnpmRunDirectlyInstallsWithWhereCorepackAgrees() {
        PnpmOptions.WantedPackageManager ported = new PnpmOptions.WantedPackageManager("pnpm", "11.2.2", "download",
          false);
        assertThat(Main.release(ported, () -> new SpecUtils.Descriptor("pnpm", "11.2.2"))).isEqualTo("11.2.2");
        assertThat(Main.release(null, () -> null)).isEqualTo(Main.PNPM_VERSION);
        assertThat(Main.release(ported.withName("yarn"), () -> fail("asked corepack"))).isEqualTo(Main.PNPM_VERSION);
        assertThat(Main.release(ported.withVersion("10.34.6"), () -> new SpecUtils.Descriptor("pnpm", "10.34.6")))
          .isEqualTo("10.34.6");
    }
}
