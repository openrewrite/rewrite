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
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.LockScenario.Phase;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/** pnpm 11.2.2's {@code install --lockfile-only} under what it reads before resolving, as recorded on 2026-10-08. */
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

        @Override
        public @Nullable String document(String url) {
            return fail("fetched " + url);
        }
    };

    private static Install.Result install(@Nullable String workspace, @Nullable String npmrc,
                                                       String... env) {
        Map<String, String> files = new LinkedHashMap<>();
        if (npmrc != null) {
            files.put(".npmrc", npmrc);
        }
        return install(LOCK, MANIFEST, workspace, files, env);
    }

    private static Install.Result install(String lock, String manifest, @Nullable String workspace,
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
          .build(), NO_REGISTRY).install();
    }

    private static Install.Result withManifest(String manifest, String... env) {
        return install(LOCK, manifest, null, Collections.emptyMap(), env);
    }

    // the edit a recorded scenario makes: its after phase's inputs over its before lock
    private static Install.Result edit(String name) {
        PnpmScenarios.Scenario scenario = scenario(name);
        Map<String, String> files = new LinkedHashMap<>(scenario.tree(Phase.AFTER));
        String workspace = scenario.workspaceManifest(Phase.AFTER);
        if (workspace != null) {
            files.put("pnpm-workspace.yaml", workspace);
        }
        String before = scenario.file("before");
        return new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .lock(before)
          .manifests(scenario.manifests(Phase.AFTER, before))
          .files(files)
          .now(scenario.phaseTime("after"))
          .build(), new ScenarioRegistry(scenario, "after")).install();
    }

    private static PnpmScenarios.Scenario scenario(String name) {
        return PnpmScenarios.discover().stream()
          .filter(s -> s.getName().equals(name))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("no scenario " + name));
    }

    private static final String WITH = "{\"name\":\"p\",\"version\":\"1.0.0\",\"dependencies\":{\"wrappy\":\"1.0.2\"},";

    /**
     * pnpm installs every project of a workspace whose projects keep their own locks in the same run, so another
     * project's lock the host holds without its contents is unknown.
     */
    @Test
    void anotherProjectsLockTheHostDidNotReadIsCannotKnow() {
        PnpmScenarios.Scenario scenario = scenario("open-ws-unshared-other-project-resolves");
        PnpmLockRegenerator.Inputs given = PnpmScenarioTest.inputs(scenario, scenario.file("before"));
        Map<String, String> files = new LinkedHashMap<>(given.getFiles());
        assertThat(files.remove("packages/b/pnpm-lock.yaml")).isNotNull();

        Result result = new PnpmLockRegenerator(given.toBuilder().clearFiles().files(files).build(),
          new ScenarioRegistry(scenario, "after")).regenerate();

        assertThat(result.getFailure()).isNotNull();
        assertThat(result.getFailure().getKind()).isEqualTo(Kind.CANNOT_KNOW);
        assertThat(result.getFailure().getDetail()).contains("packages/b/pnpm-lock.yaml (its contents were not read)");
    }

    @Test
    void anAddedDevEnginesRuntimeIsAnEditPnpmResolves() {
        Install.Result install = edit("small-dev-engines-runtime-add");
        assertThat(install.getResolution()).isEqualTo("projects not up to date with the lock");
    }

    @Test
    void runtimeOnFailIgnoreDropsTheRuntimeDependency() {
        assertThat(edit("small-runtime-on-fail-ignore").getResolution())
          .isEqualTo("projects not up to date with the lock");
    }

    /**
     * pnpm 11.2.2 over small-config-dependency-add's after: unchanged, or resolving a changed or ranged specifier.
     */
    @Test
    void configDependenciesOverALockThatRecordsThem() {
        String lock = scenario("small-config-dependency-add").file("after");
        assertThat(install(lock, MANIFEST, null, Collections.emptyMap()).getWritten()).isEqualTo(lock);
        assertThat(install(lock, MANIFEST, "configDependencies: {}", Collections.emptyMap()).getWritten())
          .isEqualTo(lock);
        assertThatThrownBy(() -> install(lock, MANIFEST, "configDependencies:\n  is-positive: 3.0.0",
          Collections.emptyMap())).hasMessageContaining("fetched https://registry.npmjs.org/is-positive");
        assertThatThrownBy(() -> install(lock, MANIFEST, "configDependencies:\n  is-positive: ^3.0.0",
          Collections.emptyMap())).hasMessageContaining("fetched https://registry.npmjs.org/is-positive");
    }

    @Test
    void aPluginConfigDependencyRunsItsPnpmfile() {
        String lock = scenario("small-config-dependency-add").file("after");
        assertThatThrownBy(() -> install(lock, MANIFEST, "configDependencies:\n  is-positive: 3.1.0\n" +
                                                         "  '@pnpm/plugin-x': 1.0.0", Collections.emptyMap()))
          .hasMessageContaining("fetched https://registry.npmjs.org/@pnpm/plugin-x");
        String withPlugin = lock.replace("    configDependencies:\n", "    configDependencies:\n" +
          "      '@pnpm/plugin-x':\n        specifier: 1.0.0\n        version: 1.0.0\n")
          .replace("packages:\n\n  is-positive@3.1.0:", "packages:\n\n  '@pnpm/plugin-x@1.0.0':\n" +
            "    resolution: {integrity: sha512-AAAA}\n\n  is-positive@3.1.0:")
          .replace("snapshots:\n\n  is-positive@3.1.0: {}", "snapshots:\n\n  '@pnpm/plugin-x@1.0.0': {}\n\n" +
            "  is-positive@3.1.0: {}");
        assertThatThrownBy(() -> install(withPlugin, MANIFEST, "configDependencies:\n  is-positive: 3.1.0\n" +
                                                               "  '@pnpm/plugin-x': 1.0.0", Collections.emptyMap()))
          .isInstanceOfSatisfying(CannotKnow.class, e -> assertThat(e.getSubject()).contains("@pnpm/plugin-x"));
    }

    @Test
    void aPnpmfileSettingRunsThatFile() {
        assertThatThrownBy(() -> install("pnpmfile: hooks/custom.cjs", null))
          .isInstanceOfSatisfying(CannotKnow.class, e -> assertThat(e.getSubject()).contains("hooks/custom.cjs"));
        assertThatThrownBy(() -> install("globalPnpmfile: /srv/u/.pnpmfile.cjs", null))
          .isInstanceOf(CannotKnow.class);
        assertThat(install("pnpmfile: hooks/missing.cjs\nignorePnpmfile: true", null).getWritten()).isEqualTo(LOCK);
        assertThat(install(LOCK, MANIFEST, "ignorePnpmfile: true",
          Collections.singletonMap(".pnpmfile.cjs", "module.exports = {}")).getWritten()).isEqualTo(LOCK);
    }

    /**
     * Installing each project into its own lock, recursive.ts loads the pnpmfiles setting from the project's directory;
     * a shared install never reads it. The recorded scenarios pin both; these are the files the host may not show.
     */
    @Test
    void thePnpmfilesSettingOfAProjectInstalledOnItsOwn() {
        String unshared = "packages:\n  - '.'\nsharedWorkspaceLockfile: false\n";
        assertThatThrownBy(() -> install(unshared + "pnpmfiles: [hooks/x.cjs]", null))
          .isInstanceOfSatisfying(CannotKnow.class, e -> assertThat(e.getSubject()).contains("whether the pnpmfile"));
        assertThatThrownBy(() -> installHolding(unshared + "pnpmfiles: [hooks/x.cjs]", "hooks/x.cjs"))
          .isInstanceOfSatisfying(CannotKnow.class, e -> assertThat(e.getSubject()).contains("what hooks/x.cjs does"));
        assertThatThrownBy(() -> installHolding(unshared + "pnpmfiles: [hooks/x.cjs, hooks/y.cjs]", "hooks/y.cjs"))
          .isInstanceOf(CannotKnow.class);
        assertThatThrownBy(() -> installHolding(unshared + "pnpmfiles: [hooks/x.cjs]"))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_PNPMFILE_NOT_FOUND"))
          .hasMessage("pnpmfile at \"hooks/x.cjs\" is not found");
        assertThat(installHolding(unshared + "pnpmfiles: [.pnpmfile.cjs]").getWritten()).isEqualTo(LOCK);
        assertThat(installHolding(unshared + "pnpmfiles: [hooks/x.cjs]\nignorePnpmfile: true").getWritten())
          .isEqualTo(LOCK);
        assertThat(installHolding("packages:\n  - '.'\npnpmfiles: [hooks/x.cjs]").getWritten()).isEqualTo(LOCK);
        assertThatThrownBy(() -> installHolding(unshared + "pnpmfiles: hooks/x.cjs"))
          .isInstanceOf(CannotKnow.class);
        assertThatThrownBy(() -> installHolding(unshared + "pnpmfiles: [../x.cjs]")).isInstanceOf(CannotKnow.class);
    }

    /**
     * recursive.ts spreads a project's packageConfigs over its install: overrides that are a Date (a YAML timestamp)
     * replace the workspace's with none.
     */
    @Test
    void projectOverridesThatAreADate() {
        String unshared = "packages:\n  - '.'\nsharedWorkspaceLockfile: false\noverrides: {wrappy: 1.0.1}\n";
        assertThat(install(unshared + "packageConfigs: {p: {overrides: 2026-01-01}}", null).getWritten())
          .isEqualTo(LOCK);
        assertThat(install(unshared, null).getResolution()).isEqualTo("overrides");
    }

    @Test
    void virtualStoreOnlyNeedsAModulesDirectory() {
        assertThatThrownBy(() -> install("virtualStoreOnly: true\nenableModulesDir: false", null))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_CONFIG_CONFLICT_VIRTUAL_STORE_ONLY_WITH_NO_MODULES_DIR"));
        assertThat(install("virtualStoreOnly: true", null).getWritten()).isEqualTo(LOCK);
        assertThat(install("enableModulesDir: false", null).getWritten()).isEqualTo(LOCK);
    }

    /**
     * pnpm stops at a workspace manifest by another of the names it looks for, nearest first.
     */
    @Test
    void aMisspeltWorkspaceManifest() {
        assertThatThrownBy(() -> installHolding(null, "pnpm-workspace.yml"))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_BAD_WORKSPACE_MANIFEST_NAME"))
          .hasMessage("The workspace manifest file should be named \"pnpm-workspace.yaml\". File found: " +
                      "pnpm-workspace.yml");
        assertThat(installHolding("packages:\n  - '.'\n", "pnpm-workspace.yml").getWritten()).isEqualTo(LOCK);
    }

    /**
     * A project whose manifest is package.yaml or package.json5, which the host does not read.
     */
    @Test
    void aProjectWithAnotherManifestName() {
        assertThatThrownBy(() -> installHolding("packages:\n  - '.'\n  - 'packages/*'\n", "packages/b/package.yaml"))
          .isInstanceOfSatisfying(CannotKnow.class,
            e -> assertThat(e.getSubject()).contains("packages/b/package.yaml"));
        assertThat(installHolding("packages:\n  - '.'\n", "packages/b/package.json5").getWritten()).isEqualTo(LOCK);
        assertThatThrownBy(() -> installHolding("packages:\n  - '.'\n", "package.json5"))
          .isInstanceOf(CannotKnow.class);
        assertThat(installHolding(null, "package.yaml").getWritten()).isEqualTo(LOCK);
    }

    private static Install.Result installHolding(@Nullable String workspace, String... paths) {
        Map<String, String> files = new LinkedHashMap<>();
        Set<String> held = new LinkedHashSet<>(Arrays.asList("package.json", "pnpm-lock.yaml"));
        held.addAll(Arrays.asList(paths));
        if (workspace != null) {
            files.put("pnpm-workspace.yaml", workspace);
            held.add("pnpm-workspace.yaml");
        }
        return new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .lock(LOCK)
          .manifest("", MANIFEST)
          .files(files)
          .paths(held)
          .env(Collections.singletonMap("pnpm_config_minimum_release_age", "0"))
          .now(Instant.parse("2026-10-08T00:00:00Z"))
          .build(), NO_REGISTRY).install();
    }

    /**
     * getConfig copies every camelCase key of pnpm-workspace.yaml into the options the install reads: one that changes
     * the lock or the outcome and that the port does not read is cannot-know.
     */
    @ParameterizedTest
    @ValueSource(strings = {"update: true", "updatePackageManifest: true", "latest: true", "dedupe: true",
      "fixLockfile: true", "forceFullResolution: true", "resolutionOnly: true", "frozenLockfile: true",
      "frozenLockfileIfExists: true", "ignorePackageManifest: true", "saveLockfile: false",
      "pruneLockfileImporters: false", "workspace: true", "ignoredPackages: [x]", "rootProjectManifestDir: x",
      "packageManager: {name: pnpm}", "wantedPackageManager: {name: pnpm}", "dir: x",
      "workspacePackagePatterns: [x]", "production: true", "only: dev", "dev: true", "optional: false",
      "recursiveInstall: false", "workspaceRoot: true", "filter: [x]", "filterProd: [x]", "offline: true",
      "preferOffline: true", "lockfileDir: x", "lockfileDir: /abs", "lockfileDir: 5"})
    void anInstallOptionTheWorkspaceManifestSetsIsCannotKnow(String setting) {
        assertThatThrownBy(() -> install("packages:\n  - '.'\n" + setting, null)).isInstanceOf(CannotKnow.class);
    }

    /**
     * Those options set to the value a lock-only install takes when they are not set.
     */
    @ParameterizedTest
    @ValueSource(strings = {"update: false", "latest: false", "dedupe: false", "fixLockfile: false",
      "forceFullResolution: false", "resolutionOnly: false", "frozenLockfile: false", "frozenLockfileIfExists: false",
      "ignorePackageManifest: false", "saveLockfile: true", "pruneLockfileImporters: true", "workspace: false",
      "pruneDirectDependencies: false", "updateToLatest: false", "updatePackageManifest: false"})
    void anInstallOptionSetAsIfUnsetChangesNothing(String setting) {
        assertThat(install("packages:\n  - '.'\n" + setting, null).getWritten()).isEqualTo(LOCK);
    }

    /**
     * Settings that do not reach the lock, and a name no typed code on the install path reads.
     */
    @ParameterizedTest
    @ValueSource(strings = {"nodeLinker: hoisted", "dedupeDirectDeps: true", "depth: 5", "sort: false",
      "hoistPattern: ['*']", "shamefullyHoist: true", "modulesDir: nm", "allowBuilds: {x: true}", "runtime: false",
      "ci: true", "optimisticRepeatInstall: false", "lockfileDir: .", "registry: https://r.example/",
      "includeWorkspaceRoot: true", "notASetting: 1"})
    void aSettingThatDoesNotReachTheLockChangesNothing(String setting) {
        assertThat(install("packages:\n  - '.'\n" + setting, null).getWritten()).isEqualTo(LOCK);
    }

    /**
     * An agent server resolves the install; under trustPolicy no-downgrade pnpm 11.2.2 refuses it.
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
     * The pnpm release a packageManager field or devEngines.packageManager names, as pnpm 11.2.2 handles each.
     */
    @Test
    void anotherPnpmReleaseTakesTheInstallOver() {
        assertThatThrownBy(() -> new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .lock(LOCK)
          .manifest("", WITH + "\"packageManager\":\"pnpm@9.15.0\"}")
          .env(Collections.singletonMap("pnpm_config_minimum_release_age", "0"))
          .now(Instant.parse("2026-10-08T00:00:00Z"))
          .build(), NO_REGISTRY).install())
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
        assertThatThrownBy(() -> withManifest(WITH + "\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\"," +
                                              "\"version\":\">=12\",\"onFail\":\"error\"}}}"))
          .isInstanceOfSatisfying(PnpmError.class, e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_BAD_PM_VERSION"))
          .hasMessage("This project is configured to use >=12 of pnpm. Your current pnpm is v11.2.2");
        assertThat(withManifest(WITH + "\"devEngines\":{\"packageManager\":{\"name\":\"pnpm\",\"version\":\">=12\"," +
                                "\"onFail\":\"warn\"}}}").getWritten()).isEqualTo(LOCK);
    }

    /**
     * NPM_CONFIG_WORKSPACE_DIR names the workspace pnpm reads; the inputs are the one found from the lock up.
     */
    @Test
    void theWorkspaceTheEnvironmentNames() {
        assertThat(install("packages:\n  - '.'\n", null, "NPM_CONFIG_WORKSPACE_DIR", ".").getWritten())
          .isEqualTo(LOCK);
        assertThatThrownBy(() -> install("packages:\n  - '.'\n", null, "NPM_CONFIG_WORKSPACE_DIR", "sub"))
          .isInstanceOf(CannotKnow.class);
    }

    @Test
    void minimumReleaseAgeZeroVerifiesNothing() {
        Install.Result install = install(null, null);
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

    /**
     * config/reader checks the project .npmrc's credentials before it reads the root manifest (index.ts:317-328, 402),
     * whose engines.runtime conversion raises a TypeError here.
     */
    @Test
    void theNpmrcCredentialsBeforeTheRootManifest() {
        String manifest = WITH + "\"engines\":{\"runtime\":[null]}}";
        assertThatThrownBy(() -> install(LOCK, manifest, null, Collections.singletonMap(".npmrc",
          "//registry.npmjs.org/:tokenHelper=/bin/echo")))
          .hasMessage("tokenHelper must not be configured in project-level .npmrc");
        assertThatThrownBy(() -> install(LOCK, manifest, null, Collections.emptyMap()))
          .isInstanceOfSatisfying(JsError.class, e -> assertThat(e.getMessage()).contains("reading 'name'"));
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

    // getConfig throws a TypeError for it in every pnpm 11 release
    @Test
    void aRegistryVariablePnpmCannotUse() {
        assertThatThrownBy(() -> install(null, null, "pnpm_config_registry", "null"))
          .isInstanceOfSatisfying(JsError.class, e -> assertThat(e.getName()).isEqualTo("TypeError"))
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
          .build(), NO_REGISTRY).install())
          .isInstanceOfSatisfying(JsError.class, e -> assertThat(e.getCode()).isEqualTo("ERR_INVALID_PACKAGE_CONFIG"));
    }
}
