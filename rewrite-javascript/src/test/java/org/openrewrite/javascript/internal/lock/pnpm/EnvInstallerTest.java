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

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.json;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.strings;

/**
 * pnpm 11.2.2's installing/env-installer/test/resolveAndInstallConfigDeps.test.ts up to the registry, less the
 * frozenLockfile cases (a lock-only install is never frozen), the logger's events and the store's contents.
 */
class EnvInstallerTest {

    private static final Map<String, String> REGISTRIES = Collections.singletonMap("default",
      "http://localhost:7769/");
    private static final String FOO_INTEGRITY = "sha512-foo100";

    private static EnvLockfile lockfileWithFoo(String version) {
        EnvLockfile lockfile = EnvLockfile.createEnvLockfile();
        lockfile.configDependencies().put("@pnpm.e2e/foo", obj("{'specifier':'" + version + "','version':'" +
                                                               version + "'}"));
        lockfile.packages().put("@pnpm.e2e/foo@" + version, obj("{'resolution':{'integrity':'" + FOO_INTEGRITY +
                                                                "'}}"));
        lockfile.snapshots().put("@pnpm.e2e/foo@" + version, obj("{}"));
        return lockfile;
    }

    @Test
    void resolvesConfigDepWhenNoEnvLockfileExists() {
        EnvInstaller.ConfigDepsResult result = EnvInstaller.resolveAndInstallConfigDeps(
          obj("{'@pnpm.e2e/foo':'100.0.0'}"), EnvLockfile.createEnvLockfile(), REGISTRIES);
        assertThat(result.getDepsToResolve()).isEqualTo(strings("{'@pnpm.e2e/foo':'100.0.0'}"));
        assertThat(result.isLockfileChanged()).isFalse();
    }

    @Test
    void resolvesNewlyAddedConfigDepWhenEnvLockfileAlreadyHasOtherDeps() {
        EnvInstaller.ConfigDepsResult result = EnvInstaller.resolveAndInstallConfigDeps(
          obj("{'@pnpm.e2e/foo':'100.0.0','@pnpm.e2e/bar':'100.0.0'}"), lockfileWithFoo("100.0.0"), REGISTRIES);
        assertThat(result.getDepsToResolve()).isEqualTo(strings("{'@pnpm.e2e/bar':'100.0.0'}"));
    }

    @Test
    void skipsResolutionWhenAllDepsAreAlreadyInEnvLockfile() {
        EnvInstaller.ConfigDepsResult result = EnvInstaller.resolveAndInstallConfigDeps(
          obj("{'@pnpm.e2e/foo':'100.0.0'}"), lockfileWithFoo("100.0.0"), REGISTRIES);
        assertThat(result.getDepsToResolve()).isEmpty();
        assertThat(result.isLockfileChanged()).isFalse();
    }

    @Test
    void reResolvesWhenConfigDepVersionChangesInPnpmWorkspaceYaml() {
        EnvInstaller.ConfigDepsResult result = EnvInstaller.resolveAndInstallConfigDeps(
          obj("{'@pnpm.e2e/foo':'100.1.0'}"), lockfileWithFoo("100.0.0"), REGISTRIES);
        assertThat(result.getDepsToResolve()).isEqualTo(strings("{'@pnpm.e2e/foo':'100.1.0'}"));
    }

    @Test
    void handlesOldFormatConfigDepsViaMigrationPath() {
        EnvLockfile lockfile = EnvLockfile.createEnvLockfile();
        EnvInstaller.ConfigDepsResult result = EnvInstaller.resolveAndInstallConfigDeps(
          obj("{'@pnpm.e2e/foo':'100.0.0+" + FOO_INTEGRITY + "'}"), lockfile, REGISTRIES);
        assertThat(result.isLockfileChanged()).isTrue();
        assertThat(result.getDepsToResolve()).isEmpty();
        assertThat(json(lockfile.getDocument())).isEqualTo(
          "{'lockfileVersion':'9.0','importers':{'.':{'configDependencies':{'@pnpm.e2e/foo':{'specifier':'100.0.0'," +
          "'version':'100.0.0'}}}},'packages':{'@pnpm.e2e/foo@100.0.0':{'resolution':{'integrity':'" +
          FOO_INTEGRITY + "'}}},'snapshots':{'@pnpm.e2e/foo@100.0.0':{}}}");
    }

    @Test
    void handlesMixedOldFormatAndNewFormatConfigDepsTogether() {
        EnvLockfile lockfile = EnvLockfile.createEnvLockfile();
        EnvInstaller.ConfigDepsResult result = EnvInstaller.resolveAndInstallConfigDeps(
          obj("{'@pnpm.e2e/foo':'100.0.0+" + FOO_INTEGRITY + "','@pnpm.e2e/bar':'100.0.0'}"), lockfile, REGISTRIES);
        assertThat(result.isLockfileChanged()).isTrue();
        assertThat(result.getDepsToResolve()).isEqualTo(strings("{'@pnpm.e2e/bar':'100.0.0'}"));
        assertThat(lockfile.packages()).containsOnlyKeys("@pnpm.e2e/foo@100.0.0");
    }

    /**
     * small-config-dependency-object-form, as pnpm 11.2.2 rejected it on 2026-10-08.
     */
    @Test
    void anObjectFormWithoutTheVersionInItsIntegrity() {
        String tarball = "https://registry.npmjs.org/is-positive/-/is-positive-3.1.0.tgz";
        Map<String, @Nullable Object> configDeps = obj("{'is-positive':{'integrity':'sha512-8ND1','tarball':'" +
                                                       tarball + "'}}");
        assertThatThrownBy(() -> EnvInstaller.resolveAndInstallConfigDeps(configDeps, EnvLockfile.createEnvLockfile(),
          REGISTRIES))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_CONFIG_DEP_NO_INTEGRITY"))
          .hasMessage("Your config dependency called \"is-positive\" doesn't have an integrity checksum");
    }

    /**
     * As pnpm 11.2.2 rejected it on 2026-10-08: the env document names a version its packages do not hold.
     */
    @Test
    void aLockedConfigDependencyWithoutItsPackagesEntry() {
        EnvLockfile lockfile = lockfileWithFoo("100.0.0");
        lockfile.packages().put("@pnpm.e2e/foo@99.0.0", lockfile.packages().remove("@pnpm.e2e/foo@100.0.0"));
        assertThatThrownBy(() -> EnvInstaller.resolveAndInstallConfigDeps(obj("{}"), lockfile, REGISTRIES))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_ENV_LOCKFILE_CORRUPTED"))
          .hasMessage("pnpm-lock.yaml is corrupted or incomplete: missing packages entry for " +
                      "\"@pnpm.e2e/foo@100.0.0\" referenced from importers['.'].configDependencies");
    }
}
