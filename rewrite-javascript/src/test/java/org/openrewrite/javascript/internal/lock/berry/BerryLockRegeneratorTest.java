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
package org.openrewrite.javascript.internal.lock.berry;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The release a project runs, against {@code yarn --version} under corepack 0.35.0 with the 4.5.3 release file as
 * the {@code yarnPath} target ({@code ybf-release-probe.sh}), and the releases after the one the package ports
 * (Yarn 4.14.0 writes {@code __metadata.version: 9} and 4.18.1 version 10 for an empty project).
 */
class BerryLockRegeneratorTest {

    private static final String YARN_PATH = "yarnPath: .yarn/releases/yarn-4.5.3.cjs\n";

    @Test
    void yarnPathWinsOverPackageManager() {
        assertThat(release("yarn@4.10.3", YARN_PATH, Collections.emptyMap())).isEqualTo("4.5.3");
        assertThat(release(null, YARN_PATH, Collections.emptyMap())).isEqualTo("4.5.3");
        assertThat(release("yarn@4.10.3", null, Collections.singletonMap("YARN_YARN_PATH",
                ".yarn/releases/yarn-4.5.3.cjs"))).isEqualTo("4.5.3");
    }

    @Test
    void ignorePathLeavesItToPackageManager() {
        assertThat(release("yarn@4.10.3", YARN_PATH + "ignorePath: true\n", Collections.emptyMap()))
                .isEqualTo("4.10.3");
        assertThat(release("yarn@4.10.3", YARN_PATH, Collections.singletonMap("YARN_IGNORE_PATH", "1")))
                .isEqualTo("4.10.3");
        assertThat(release("yarn@4.10.3", YARN_PATH, Collections.singletonMap("YARN_IGNORE_PATH", "0")))
                .isEqualTo("4.5.3");
        assertThat(release("yarn@4.10.3", null, Collections.emptyMap())).isEqualTo("4.10.3");
    }

    @Test
    void aReleaseFileWithoutAVersionIsUnknown() {
        assertThatThrownBy(() -> release("yarn@4.10.3", "yarnPath: .yarn/releases/yarn.cjs\n",
                Collections.emptyMap()))
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining(".yarn/releases/yarn.cjs");
        assertThatThrownBy(() -> release(null, null, Collections.emptyMap()))
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("the Yarn release that runs");
    }

    @Test
    void aReleaseAfterThePortedOneIsUnknown() {
        for (String version : new String[]{"4.11.0", "4.13.0", "4.14.0", "4.18.1"}) {
            assertThat(YarnRelease.of(version)).as(version).isNotNull();
            assertThatThrownBy(() -> release("yarn@" + version, null, Collections.emptyMap())).as(version)
                    .isInstanceOf(CannotKnow.class)
                    .hasMessageContaining("Yarn " + version)
                    .hasMessageContaining("ports Yarn 4.10.3");
        }
        assertThatThrownBy(() -> release("yarn@4.10.3", "yarnPath: .yarn/releases/yarn-4.14.0.cjs\n",
                Collections.emptyMap()))
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("Yarn 4.14.0");
        assertThat(release("yarn@4.0.0", null, Collections.emptyMap())).isEqualTo("4.0.0");
    }

    private static String release(@Nullable String packageManager, @Nullable String yarnrc, Map<String, String> env) {
        BerryLockRegenerator.Inputs.InputsBuilder inputs = BerryLockRegenerator.Inputs.builder()
                .manifest("", packageManager == null ? "{\"name\": \"p\"}" :
                        "{\"name\": \"p\", \"packageManager\": \"" + packageManager + "\"}")
                .env(env)
                .now(Instant.EPOCH);
        if (yarnrc != null) {
            inputs.configFile(".yarnrc.yml", yarnrc);
        }
        return new BerryLockRegenerator(inputs.build()).release().getVersion();
    }
}
