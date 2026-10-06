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
package org.openrewrite.javascript.internal.lock.npm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What npm 11.17.0 does with each {@code lockfile-version} an .npmrc can hold (the config layer's
 * parseField and flatten, then arborist's own check in its constructor) and with each boolean's
 * value (parseField maps four words; arborist reads any other string as truthy).
 */
class NpmOptionsTest {

    @Test
    void defaults() {
        assertThat(NpmOptions.fromNpmrc(null)).isEqualTo(NpmOptions.DEFAULT);
        assertThat(NpmOptions.fromNpmrc("")).isEqualTo(NpmOptions.DEFAULT);
        assertThat(NpmOptions.DEFAULT.getLockfileVersion()).isNull();
        assertThat(NpmOptions.DEFAULT.isLegacyPeerDeps()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
      "2, 2", "' 3 ', 3", "+2, 2", "1.5, 1", "2abc, 2", "null,", "undefined,"
    })
    void lockfileVersionNpmAccepts(String value, Integer expected) {
        assertThat(NpmOptions.fromNpmrc("lockfile-version=" + value).getLockfileVersion()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
      "4, 4", "abc, NaN", "'', ''", "false, false", "true, NaN", "0, 0", "-1, -1", "0x10, 0"
    })
    void lockfileVersionNpmRejects(String value, String reported) {
        assertThatThrownBy(() -> NpmOptions.fromNpmrc("lockfile-version=" + value))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Invalid lockfileVersion config: " + reported);
    }

    @Test
    void bareKeyIsTrue() {
        assertThatThrownBy(() -> NpmOptions.fromNpmrc("lockfile-version"))
          .hasMessage("Invalid lockfileVersion config: NaN");
        assertThat(NpmOptions.fromNpmrc("legacy-peer-deps").isLegacyPeerDeps()).isTrue();
    }

    // each value installed with npm 11.17.0: the peer chain of arb-legacy-peer-deps-add stays out when legacy
    @ParameterizedTest
    @CsvSource({
      "true, true", "false, false", "0, true", "FALSE, true", "True, true", "yes, true", "00, true", "2, true",
      "undefined, false", "null, false", "'', true", "'true ', true", "' false', false"
    })
    void booleansAreNpmConfigsTruthiness(String value, boolean expected) {
        assertThat(NpmOptions.fromNpmrc("legacy-peer-deps=" + value).isLegacyPeerDeps()).isEqualTo(expected);
        assertThat(NpmOptions.fromNpmrc("install-links=" + value).isInstallLinks()).isEqualTo(expected);
        assertThat(NpmOptions.fromNpmrc("omit-lockfile-registry-resolved=" + value).isOmitLockfileRegistryResolved())
          .isEqualTo(expected);
    }

    @Test
    void otherKeysPassOver() {
        NpmOptions options = NpmOptions.fromNpmrc("# comment\n; another\nforce=true\nlegacy-peer-deps=true\n\n" +
                                                  "install-links=false\nomit-lockfile-registry-resolved=1\n" +
                                                  "lockfile-version=3\n");
        assertThat(options.getLockfileVersion()).isEqualTo(3);
        assertThat(options.isLegacyPeerDeps()).isTrue();
        assertThat(options.isInstallLinks()).isFalse();
        assertThat(options.isOmitLockfileRegistryResolved()).isTrue();
    }
}
