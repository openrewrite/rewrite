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
import org.openrewrite.javascript.internal.lock.Host;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.json;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;

/**
 * pnpm 11.2.2's installing/env-installer/test/resolveAndInstallConfigDeps.test.ts over the env document, less the
 * frozenLockfile cases (a lock-only install is never frozen), the logger's events and the store's contents.
 */
class EnvInstallerTest {

    private static final String REGISTRY = "http://localhost:7769/";
    private static final PnpmOptions OPTIONS = PnpmOptions.load(null, "registry=" + REGISTRY, null,
      Collections.emptyMap());

    // the registry mock's @pnpm.e2e/foo 100.0.0 and 100.1.0 and @pnpm.e2e/bar 100.0.0, as each test may fetch them
    private static final class E2eRegistry implements RegistryMetadata {
        final List<String> serves;
        final List<String> fetched = new ArrayList<>();

        E2eRegistry(String... serves) {
            this.serves = Arrays.asList(serves);
        }

        @Override
        public @Nullable String packument(String registry, String name, boolean fullMetadata) {
            if (!serves.contains(name)) {
                return fail("fetched " + registry + name);
            }
            fetched.add(name);
            StringBuilder versions = new StringBuilder();
            for (String version : "@pnpm.e2e/foo".equals(name) ? new String[]{"100.0.0", "100.1.0"} :
              new String[]{"100.0.0"}) {
                if (versions.length() > 0) {
                    versions.append(',');
                }
                versions.append('"').append(version).append("\":{\"name\":\"").append(name)
                  .append("\",\"version\":\"").append(version).append("\",\"dist\":{\"integrity\":\"")
                  .append(integrity(name, version)).append("\",\"tarball\":\"").append(registry).append(name)
                  .append("/-/").append(name.substring(name.indexOf('/') + 1)).append('-').append(version)
                  .append(".tgz\"}}");
            }
            return "{\"name\":\"" + name + "\",\"dist-tags\":{\"latest\":\"100.0.0\"},\"versions\":{" + versions +
                   "}}";
        }

        @Override
        public @Nullable String attestations(String registry, String name, String version) {
            return fail("fetched the attestations of " + name + "@" + version);
        }

        @Override
        public @Nullable String document(String url) {
            return fail("fetched " + url);
        }
    }

    private static String integrity(String name, String version) {
        return "sha512-" + name.substring(name.indexOf('/') + 1) + version.replace(".", "");
    }

    private static EnvLockfile lockfileWithFoo(String version) {
        EnvLockfile lockfile = EnvLockfile.createEnvLockfile();
        lockfile.configDependencies().put("@pnpm.e2e/foo", obj("{'specifier':'" + version + "','version':'" +
                                                               version + "'}"));
        lockfile.packages().put("@pnpm.e2e/foo@" + version, obj("{'resolution':{'integrity':'" +
                                                                integrity("@pnpm.e2e/foo", version) + "'}}"));
        lockfile.snapshots().put("@pnpm.e2e/foo@" + version, obj("{}"));
        return lockfile;
    }

    private static EnvLockfile.OnDisk onDisk(@Nullable EnvLockfile envLockfile) {
        return new EnvLockfile.OnDisk(envLockfile == null ? null : EnvLockfile.writeEnvLockfile(null, envLockfile));
    }

    private static EnvLockfile install(Map<String, @Nullable Object> configDeps, EnvLockfile.OnDisk lockFile,
                                       RegistryMetadata registry) {
        new EnvInstaller(registry, Instant.parse("2026-10-08T00:00:00Z"), Host.UNKNOWN)
          .resolveAndInstallConfigDeps(configDeps, lockFile, OPTIONS);
        EnvLockfile written = EnvLockfile.readEnvLockfile(lockFile.text());
        assertThat(written).isNotNull();
        return written;
    }

    @Test
    void resolvesAndInstallsConfigDepWhenNoEnvLockfileExists() {
        EnvLockfile envLockfile = install(obj("{'@pnpm.e2e/foo':'100.0.0'}"), onDisk(null),
          new E2eRegistry("@pnpm.e2e/foo"));
        assertThat(json(envLockfile.configDependencies().get("@pnpm.e2e/foo")))
          .isEqualTo("{'specifier':'100.0.0','version':'100.0.0'}");
        assertThat(json(envLockfile.packages().get("@pnpm.e2e/foo@100.0.0")))
          .isEqualTo("{'resolution':{'integrity':'" + integrity("@pnpm.e2e/foo", "100.0.0") + "'}}");
    }

    @Test
    void resolvesNewlyAddedConfigDepWhenEnvLockfileAlreadyHasOtherDeps() {
        E2eRegistry registry = new E2eRegistry("@pnpm.e2e/bar");
        EnvLockfile envLockfile = install(obj("{'@pnpm.e2e/foo':'100.0.0','@pnpm.e2e/bar':'100.0.0'}"),
          onDisk(lockfileWithFoo("100.0.0")), registry);
        assertThat(envLockfile.configDependencies()).containsKeys("@pnpm.e2e/foo", "@pnpm.e2e/bar");
        assertThat(registry.fetched).containsExactly("@pnpm.e2e/bar");
    }

    @Test
    void skipsResolutionWhenAllDepsAreAlreadyInEnvLockfile() {
        EnvLockfile.OnDisk lockFile = onDisk(lockfileWithFoo("100.0.0"));
        String before = lockFile.text();
        install(obj("{'@pnpm.e2e/foo':'100.0.0'}"), lockFile, new E2eRegistry());
        assertThat(lockFile.text()).isSameAs(before);
    }

    @Test
    void reResolvesAndReinstallsWhenConfigDepVersionChangesInPnpmWorkspaceYaml() {
        EnvLockfile.OnDisk lockFile = onDisk(lockfileWithFoo("100.0.0"));
        install(obj("{'@pnpm.e2e/foo':'100.0.0'}"), lockFile, new E2eRegistry());
        EnvLockfile envLockfile = install(obj("{'@pnpm.e2e/foo':'100.1.0'}"), lockFile,
          new E2eRegistry("@pnpm.e2e/foo"));
        assertThat(json(envLockfile.configDependencies().get("@pnpm.e2e/foo")))
          .isEqualTo("{'specifier':'100.1.0','version':'100.1.0'}");
        assertThat(json(envLockfile.packages().get("@pnpm.e2e/foo@100.1.0")))
          .isEqualTo("{'resolution':{'integrity':'" + integrity("@pnpm.e2e/foo", "100.1.0") + "'}}");
        assertThat(envLockfile.packages()).doesNotContainKey("@pnpm.e2e/foo@100.0.0");
    }

    @Test
    void handlesOldFormatConfigDepsViaMigrationPath() {
        EnvLockfile envLockfile = install(obj("{'@pnpm.e2e/foo':'100.0.0+" + integrity("@pnpm.e2e/foo", "100.0.0") +
                                              "'}"), onDisk(null), new E2eRegistry());
        assertThat(json(envLockfile.getDocument())).isEqualTo(
          "{'lockfileVersion':'9.0','importers':{'.':{'configDependencies':{'@pnpm.e2e/foo':{'specifier':'100.0.0'," +
          "'version':'100.0.0'}}}},'packages':{'@pnpm.e2e/foo@100.0.0':{'resolution':{'integrity':'" +
          integrity("@pnpm.e2e/foo", "100.0.0") + "'}}},'snapshots':{'@pnpm.e2e/foo@100.0.0':{}}}");
    }

    @Test
    void handlesMixedOldFormatAndNewFormatConfigDepsTogether() {
        E2eRegistry registry = new E2eRegistry("@pnpm.e2e/bar");
        EnvLockfile envLockfile = install(obj("{'@pnpm.e2e/foo':'100.0.0+" + integrity("@pnpm.e2e/foo",
          "100.0.0") + "','@pnpm.e2e/bar':'100.0.0'}"), onDisk(null), registry);
        assertThat(envLockfile.configDependencies()).containsKeys("@pnpm.e2e/foo", "@pnpm.e2e/bar");
        assertThat(envLockfile.packages()).containsOnlyKeys("@pnpm.e2e/bar@100.0.0", "@pnpm.e2e/foo@100.0.0");
        assertThat(registry.fetched).containsExactly("@pnpm.e2e/bar");
    }

    /**
     * small-config-dependency-object-form, as pnpm 11.2.2 rejected it on 2026-10-08.
     */
    @Test
    void anObjectFormWithoutTheVersionInItsIntegrity() {
        String tarball = "https://registry.npmjs.org/is-positive/-/is-positive-3.1.0.tgz";
        Map<String, @Nullable Object> configDeps = obj("{'is-positive':{'integrity':'sha512-8ND1','tarball':'" +
                                                       tarball + "'}}");
        assertThatThrownBy(() -> install(configDeps, onDisk(null), new E2eRegistry()))
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
        assertThatThrownBy(() -> install(obj("{}"), onDisk(lockfile), new E2eRegistry()))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_ENV_LOCKFILE_CORRUPTED"))
          .hasMessage("pnpm-lock.yaml is corrupted or incomplete: missing packages entry for " +
                      "\"@pnpm.e2e/foo@100.0.0\" referenced from importers['.'].configDependencies");
    }
}
