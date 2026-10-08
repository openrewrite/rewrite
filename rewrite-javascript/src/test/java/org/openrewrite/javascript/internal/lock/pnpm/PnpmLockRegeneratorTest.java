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

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.fail;

/**
 * pnpm 11.2.2's {@code install --lockfile-only} under what it reads before resolving, as recorded on 2026-10-08: the
 * wanted package manager, config dependencies, pnpmfiles, an agent, the project's settings and the edits of the
 * recorded scenarios that reach the env document or the manifest reader's conversion.
 */
class PnpmLockRegeneratorTest {

    private static final String LOCK = "lockfileVersion: '9.0'\n\nsettings:\n  autoInstallPeers: true\n" +
                                       "  excludeLinksFromLockfile: false\n\nimporters:\n\n  .:\n    dependencies:\n" +
                                       "      wrappy:\n        specifier: 1.0.2\n        version: 1.0.2\n\n" +
                                       "packages:\n\n  wrappy@1.0.2:\n    resolution: {integrity: sha512-" +
                                       "l4Sp/DRseor9wL6EvV2+TuQn63dMkPjZ/sp9XkghTEbV9KlPS1xUsZ3u7/IQO4wxtcFB4bgpQPRc" +
                                       "R3QCvezPcQ==}" +
                                       "\n\nsnapshots:\n\n  wrappy@1.0.2: {}\n";
    private static final String MANIFEST =
      "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"wrappy\":\"1.0.2\"}}";

    private static final RegistryMetadata NO_REGISTRY = new RegistryMetadata() {
        @Override
        public @Nullable String packument(String registry, String name, boolean fullMetadata) {
            return fail("fetched " + registry + name);
        }

        @Override
        public @Nullable String attestations(String registry, String name, String version) {
            return fail("fetched the attestations of " + name + "@" + version);
        }
    };

    private static PnpmLockRegenerator.Install install(@Nullable String workspace, @Nullable String npmrc,
                                                       String... env) {
        Map<String, String> files = new LinkedHashMap<>();
        if (npmrc != null) {
            files.put(".npmrc", npmrc);
        }
        return install(LOCK, MANIFEST, workspace, files, env);
    }

    private static PnpmLockRegenerator.Install install(String lock, String manifest, @Nullable String workspace,
                                                       Map<String, String> projectFiles, String... env) {
        Map<String, String> files = new LinkedHashMap<>(projectFiles);
        if (workspace != null) {
            files.put("pnpm-workspace.yaml", workspace);
        }
        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("pnpm_config_minimum_release_age", "0");
        for (int i = 0; i < env.length; i += 2) {
            environment.put(env[i], env[i + 1]);
        }
        return new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .lock(lock)
          .manifest("", manifest)
          .files(files)
          .env(environment)
          .now(Instant.parse("2026-10-08T00:00:00Z"))
          .registry(NO_REGISTRY)
          .build()).install();
    }

    private static PnpmLockRegenerator.Install withManifest(String manifest, String... env) {
        return install(LOCK, manifest, null, Collections.emptyMap(), env);
    }

    // the edit a recorded scenario makes: its after phase's inputs over its before lock
    private static PnpmLockRegenerator.Install edit(String name) {
        PnpmScenarios.Scenario scenario = scenario(name);
        Map<String, String> files = new LinkedHashMap<>(scenario.tree(false));
        String workspace = scenario.workspaceManifest(false);
        if (workspace != null) {
            files.put("pnpm-workspace.yaml", workspace);
        }
        String before = scenario.file("before");
        return new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .lock(before)
          .manifests(scenario.manifests(false, before))
          .files(files)
          .now(scenario.phaseTime("after"))
          .registry(new ScenarioRegistry(scenario, "after"))
          .build()).install();
    }

    private static PnpmScenarios.Scenario scenario(String name) {
        return PnpmScenarios.discover().stream()
          .filter(s -> s.getName().equals(name))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("no scenario " + name));
    }

    private static final String WITH = "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"wrappy\":\"1.0.2\"},";

    @Test
    void anAddedDevEnginesRuntimeIsAnEditPnpmResolves() {
        PnpmLockRegenerator.Install install = edit("small-dev-engines-runtime-add");
        assertThat(install.getResolution()).isEqualTo("projects not up to date with the lock");
    }

    @Test
    void runtimeOnFailIgnoreDropsTheRuntimeDependency() {
        assertThat(edit("small-runtime-on-fail-ignore").getResolution())
          .isEqualTo("projects not up to date with the lock");
    }

    @Test
    void anAddedConfigDependencyIsResolvedIntoTheEnvDocument() {
        PnpmLockRegenerator.Install install = edit("small-config-dependency-add");
        assertThat(install.getConfigDependenciesToResolve()).containsExactly(entry("is-positive", "3.1.0"));
        assertThat(install.getResolution()).isNull();
        assertThat(install.getWritten()).isNull();
    }

    @Test
    void aConfigDependencyInTheInlineIntegrityFormMigratesIntoTheEnvDocument() {
        assertThat(edit("small-config-dependency-legacy-integrity").getWritten())
          .isEqualTo(scenario("small-config-dependency-legacy-integrity").file("after"));
    }

    @Test
    void aConfigDependencyInTheObjectFormKeepsATarballTheRegistryDoesNotName() {
        assertThat(edit("small-config-dependency-object-form-versioned").getWritten())
          .isEqualTo(scenario("small-config-dependency-object-form-versioned").file("after"));
    }

    @Test
    void aConfigDependencyInTheObjectFormWithoutAVersion() {
        assertThatThrownBy(() -> edit("small-config-dependency-object-form"))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_CONFIG_DEP_NO_INTEGRITY"));
    }

    /**
     * fpm2-probes.sh's config-dep-* over small-config-dependency-add's after.
     */
    @Test
    void configDependenciesOverALockThatRecordsThem() {
        String lock = scenario("small-config-dependency-add").file("after");
        assertThat(install(lock, MANIFEST, null, Collections.emptyMap()).getWritten()).isEqualTo(lock);
        assertThat(install(lock, MANIFEST, "configDependencies: {}", Collections.emptyMap()).getWritten())
          .isEqualTo(lock);
        assertThat(install(lock, MANIFEST, "configDependencies:\n  is-positive: 3.0.0", Collections.emptyMap())
          .getConfigDependenciesToResolve()).containsExactly(entry("is-positive", "3.0.0"));
        assertThat(install(lock, MANIFEST, "configDependencies:\n  is-positive: ^3.0.0", Collections.emptyMap())
          .getConfigDependenciesToResolve()).containsExactly(entry("is-positive", "^3.0.0"));
    }

    @Test
    void aPluginConfigDependencyRunsItsPnpmfile() {
        String lock = scenario("small-config-dependency-add").file("after");
        assertThatThrownBy(() -> install(lock, MANIFEST, "configDependencies:\n  is-positive: 3.1.0\n" +
                                                         "  '@pnpm/plugin-x': 1.0.0", Collections.emptyMap()))
          .isInstanceOfSatisfying(CannotKnow.class, e -> assertThat(e.getSubject()).contains("@pnpm/plugin-x"));
    }

    @Test
    void aPnpmfileSettingRunsThatFile() {
        assertThatThrownBy(() -> install("pnpmfile: hooks/custom.cjs", null))
          .isInstanceOfSatisfying(CannotKnow.class, e -> assertThat(e.getSubject()).contains("hooks/custom.cjs"));
        assertThatThrownBy(() -> install("globalPnpmfile: /home/u/.pnpmfile.cjs", null))
          .isInstanceOf(CannotKnow.class);
        assertThat(install("pnpmfile: hooks/missing.cjs\nignorePnpmfile: true", null).getWritten()).isEqualTo(LOCK);
        assertThat(install(LOCK, MANIFEST, "ignorePnpmfile: true",
          Collections.singletonMap(".pnpmfile.cjs", "module.exports = {}")).getWritten()).isEqualTo(LOCK);
    }

    /**
     * agent-probe.sh (the review's) and fpm2-probes.sh's agent-trust-policy.
     */
    @Test
    void anAgentResolvesTheInstall() {
        assertThatThrownBy(() -> install("agent: http://127.0.0.1:9/", null))
          .isInstanceOfSatisfying(CannotKnow.class, e -> assertThat(e.getSubject()).contains("agent"));
        assertThatThrownBy(() -> install(null, null, "pnpm_config_agent", "http://127.0.0.1:9/"))
          .isInstanceOf(CannotKnow.class);
        assertThatThrownBy(() -> install("agent: http://127.0.0.1:9/\ntrustPolicy: no-downgrade", null))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_TRUST_POLICY_INCOMPATIBLE_WITH_AGENT"));
    }

    /**
     * fpm2-probes.sh's pm-*: the release a packageManager field or devEngines.packageManager names.
     */
    @Test
    void anotherPnpmReleaseTakesTheInstallOver() {
        assertThatThrownBy(() -> withManifest(WITH + "\"packageManager\":\"pnpm@9.15.0\"}"))
          .isInstanceOfSatisfying(CannotKnow.class, e -> assertThat(e.getSubject()).contains("9.15.0"));
        assertThat(withManifest(WITH + "\"packageManager\":\"pnpm@11.2.2+sha512.0000\"}").getWritten())
          .isEqualTo(LOCK);
        assertThat(withManifest(WITH + "\"packageManager\":\"pnpm@^9.0.0\"}").getWritten()).isEqualTo(LOCK);
        assertThat(install(LOCK, WITH + "\"packageManager\":\"pnpm@9.15.0\"}", "pmOnFail: ignore",
          Collections.emptyMap()).getWritten()).isEqualTo(LOCK);
    }

    @Test
    void anotherPackageManager() {
        assertThatThrownBy(() -> withManifest(WITH + "\"packageManager\":\"yarn@1.22.22\"}"))
          .isInstanceOfSatisfying(PnpmError.class, e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_OTHER_PM_EXPECTED"))
          .hasMessage("This project is configured to use yarn");
        assertThat(install(LOCK, WITH + "\"packageManager\":\"yarn@1.22.22\"}", "pmOnFail: warn",
          Collections.emptyMap()).getWritten()).isEqualTo(LOCK);
    }

    @Test
    void anotherPnpmReleaseUnderCorepack() {
        assertThatThrownBy(() -> withManifest(WITH + "\"packageManager\":\"pnpm@9.15.0\"}", "COREPACK_ROOT",
          "/tmp/corepack"))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_BAD_PM_VERSION");
              assertThat(e.getHint()).startsWith("Align the \"packageManager\" field");
          })
          .hasMessage("This project is configured to use 9.15.0 of pnpm. Your current pnpm is v11.2.2\n" +
                      "Corepack invoked pnpm with this version, and pnpm does not switch versions when running under " +
                      "corepack.");
    }

    @Test
    void devEnginesPackageManager() {
        PnpmLockRegenerator.Install exact = withManifest(WITH + "\"devEngines\":{\"packageManager\":{" +
                                                         "\"name\":\"pnpm\",\"version\":\"11.2.2\"}}}");
        assertThat(exact.getPackageManagerToResolve()).isEqualTo("11.2.2");
        assertThat(exact.getWritten()).isNull();
        assertThatThrownBy(() -> withManifest(WITH + "\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\"," +
                                              "\"version\":\">=12\",\"onFail\":\"error\"}}}"))
          .isInstanceOfSatisfying(PnpmError.class, e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_BAD_PM_VERSION"))
          .hasMessage("This project is configured to use >=12 of pnpm. Your current pnpm is v11.2.2");
        assertThat(withManifest(WITH + "\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\",\"version\":\">=12\"," +
                                "\"onFail\":\"warn\"}}}").getWritten()).isEqualTo(LOCK);
    }

    /**
     * pnpm 11.2.2's pnpm/src/shouldPersistLockfile.test.ts.
     */
    @Test
    void shouldPersistLockfile() {
        for (String version : new String[]{"9.3.0", "11.0.0", "12.0.0", ">=9.0.0"}) {
            assertThat(PnpmLockRegenerator.shouldPersistLockfile(pm(version, true))).as(version).isTrue();
        }
        for (String version : new String[]{"9.3.0", "10.0.0", "11.0.0", "11.0.0-rc.1"}) {
            assertThat(PnpmLockRegenerator.shouldPersistLockfile(pm(version, false))).as(version).isFalse();
        }
        for (String version : new String[]{"12.0.0", "12.5.3", "13.0.0", "100.0.0"}) {
            assertThat(PnpmLockRegenerator.shouldPersistLockfile(pm(version, false))).as(version).isTrue();
        }
        for (String version : new String[]{null, "not-a-version", "^12.0.0"}) {
            assertThat(PnpmLockRegenerator.shouldPersistLockfile(pm(version, false))).as(version).isFalse();
        }
    }

    /**
     * pnpm 11.2.2's pnpm/src/syncEnvLockfile.test.ts: the version pnpm records into packageManagerDependencies, null
     * where it records nothing. Its first case, no wanted package manager, is every other test of this class.
     */
    @Test
    void syncEnvLockfile() {
        assertThat(PnpmLockRegenerator.syncEnvLockfile(new PnpmOptions.WantedPackageManager("yarn", "4.0.0", null,
          true), () -> null)).isNull();
        assertThat(PnpmLockRegenerator.syncEnvLockfile(pm("11.0.0", false), () -> staleEnvLockfile("9.0.0")))
          .isNull();
        assertThat(PnpmLockRegenerator.syncEnvLockfile(pm("0.0.1", true), () -> staleEnvLockfile("9.0.0"))).isNull();
        assertThat(PnpmLockRegenerator.syncEnvLockfile(pm("11.2.2", true), () -> null)).isEqualTo("11.2.2");
        assertThat(PnpmLockRegenerator.syncEnvLockfile(pm("11.2.2", true), () -> EnvLockfile.readEnvLockfile(
          "---\nlockfileVersion: '9.0'\nimporters:\n  '.':\n    configDependencies: {}\npackages: {}\n" +
          "snapshots: {}\n\n---\n"))).isEqualTo("11.2.2");
        assertThat(PnpmLockRegenerator.syncEnvLockfile(pm("11.2.2", true), () -> staleEnvLockfile("11.2.2")))
          .isNull();
        assertThat(PnpmLockRegenerator.syncEnvLockfile(pm("11.2.2", true), () -> staleEnvLockfile("9.0.0")))
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

    @Test
    void minimumReleaseAgeZeroVerifiesNothing() {
        PnpmLockRegenerator.Install install = install(null, null);
        assertThat(install.getResolution()).isNull();
        assertThat(install.getWritten()).isEqualTo(LOCK);
    }

    @Test
    void lockfileFalseRejectsALockOnlyInstall() {
        assertThatThrownBy(() -> install("lockfile: false", null))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_CONFIG_CONFLICT_LOCKFILE_ONLY_WITH_NO_LOCKFILE"))
          .hasMessage("Cannot generate a pnpm-lock.yaml because lockfile is set to false");
        assertThatThrownBy(() -> install(null, null, "pnpm_config_lockfile", "false"))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_CONFIG_CONFLICT_LOCKFILE_ONLY_WITH_NO_LOCKFILE"));
    }

    @Test
    void aTokenHelperInTheProjectNpmrc() {
        assertThatThrownBy(() -> install(null, "//registry.npmjs.org/:tokenHelper=/bin/echo"))
          .hasMessage("tokenHelper must not be configured in project-level .npmrc");
    }

    @Test
    void anAuthPairPnpmCannotUse() {
        assertThatThrownBy(() -> install(null, "_auth=Zm9vYmFy"))
          .hasMessage("No separator found in the decoded form of _auth");
        assertThatThrownBy(() -> install(null, "_auth=!!!"))
          .hasMessage("Failed to decode _auth as base64");
        assertThatThrownBy(() -> install(null, "//registry.npmjs.org/:_auth=Zm9vYmFy"))
          .hasMessage("No separator found in the decoded form of _auth");
    }

    @Test
    void aRegistryVariablePnpmCannotUse() {
        assertThatThrownBy(() -> install(null, null, "pnpm_config_registry", "null"))
          .hasMessage("Unexpected type of registry, expecting a string but received null");
        assertThatThrownBy(() -> install(null, null, "pnpm_config_registry", "not-a-url"))
          .hasMessage("Invalid URL");
    }

    @Test
    void aRegistryWithoutTrailingSlash() {
        assertThat(install(null, "registry=https://registry.npmjs.org").getWritten()).isEqualTo(LOCK);
    }

    @Test
    void gitBranchLockfileNamesTheLockAfterTheBranch() {
        assertThatThrownBy(() -> install("gitBranchLockfile: true", null)).isInstanceOf(CannotKnow.class);
    }

    /**
     * small-manifest-not-json: Node's module loader reads the package.json pnpm starts beside, before pnpm runs.
     */
    @Test
    void aRootManifestThatIsNotJson() {
        assertThatThrownBy(() -> new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .lock(LOCK)
          .manifest("", "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"wrappy\":\"1.0.2\"}")
          .now(Instant.parse("2026-10-08T00:00:00Z"))
          .registry(NO_REGISTRY)
          .build()).install())
          .isInstanceOfSatisfying(JsError.class, e -> assertThat(e.getCode()).isEqualTo("ERR_INVALID_PACKAGE_CONFIG"));
    }
}
