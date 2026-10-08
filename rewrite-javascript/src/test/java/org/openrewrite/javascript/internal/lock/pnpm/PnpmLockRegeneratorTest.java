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
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * pnpm 11.2.2's {@code install --lockfile-only} over a lock of wrappy 1.0.2 under configuration it reads before
 * resolving, as recorded on 2026-10-08: the project's pnpm-workspace.yaml, .npmrc and {@code pnpm_config_}
 * variables.
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
        if (workspace != null) {
            files.put("pnpm-workspace.yaml", workspace);
        }
        if (npmrc != null) {
            files.put(".npmrc", npmrc);
        }
        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("pnpm_config_minimum_release_age", "0");
        for (int i = 0; i < env.length; i += 2) {
            environment.put(env[i], env[i + 1]);
        }
        return new PnpmLockRegenerator(PnpmLockRegenerator.Inputs.builder()
          .lock(LOCK)
          .manifest("", MANIFEST)
          .files(files)
          .env(environment)
          .now(Instant.parse("2026-10-08T00:00:00Z"))
          .registry(NO_REGISTRY)
          .build()).install();
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
