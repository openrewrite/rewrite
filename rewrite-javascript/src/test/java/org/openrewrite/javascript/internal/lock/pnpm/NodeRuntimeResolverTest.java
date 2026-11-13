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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Port of pnpm 11.2.2's engine/runtime/node-resolver/test/parseNodeSpecifier.test.ts, getNodeMirror.test.ts and
 * normalizeArch.test.ts.
 */
class NodeRuntimeResolverTest {

    @ParameterizedTest
    @CsvSource({
      "6, 6, release",
      "16.0, 16.0, release",
      "16.0.0-rc.0, 16.0.0-rc.0, rc",
      "rc/10, 10, rc",
      "nightly, latest, nightly",
      "rc, latest, rc",
      "test, latest, test",
      "v8-canary, latest, v8-canary",
      "release, latest, release",
      "lts, lts, release",
      "latest, latest, release",
      "argon, argon, release",
      "iron, iron, release",
      "22.0.0, 22.0.0, release",
      "release/22.0.0, 22.0.0, release",
      "release/latest, latest, release",
      "release/lts, lts, release",
      "release/18, 18, release",
      "rc/18, 18, rc",
      "rc/18.0.0-rc.4, 18.0.0-rc.4, rc",
      "nightly/latest, latest, nightly",
      "24.0.0-nightly20250315d765e70802, 24.0.0-nightly20250315d765e70802, nightly",
      "22.0.0-v8-canary20250101abc, 22.0.0-v8-canary20250101abc, v8-canary"
    })
    void parseNodeSpecifier(String specifier, String versionSpecifier, String releaseChannel) {
        assertThat(NodeRuntimeResolver.parseNodeSpecifier(specifier)).containsExactly(releaseChannel,
          versionSpecifier);
    }

    @Test
    void anUnknownReleaseChannelIsRejected() {
        assertThatThrownBy(() -> NodeRuntimeResolver.parseNodeSpecifier("foo/18"))
          .hasMessage("\"foo\" is not a valid Node.js release channel");
    }

    @ParameterizedTest
    @CsvSource({
      "release, http://test.mirror.localhost/release, http://test.mirror.localhost/release/",
      "nightly, http://test.mirror.localhost/nightly, http://test.mirror.localhost/nightly/",
      "rc, http://test.mirror.localhost/rc, http://test.mirror.localhost/rc/",
      "test, http://test.mirror.localhost/test, http://test.mirror.localhost/test/",
      "v8-canary, http://test.mirror.localhost/v8-canary, http://test.mirror.localhost/v8-canary/"
    })
    void getNodeMirror(String releaseDir, String mirror, String expected) {
        assertThat(NodeRuntimeResolver.getNodeMirror(Collections.singletonMap(releaseDir, mirror), releaseDir))
          .isEqualTo(expected);
    }

    @Test
    void getNodeMirrorDefaults() {
        assertThat(NodeRuntimeResolver.getNodeMirror(Collections.emptyMap(), "release"))
          .isEqualTo("https://nodejs.org/download/release/");
        assertThat(NodeRuntimeResolver.getNodeMirror(null, "release"))
          .isEqualTo("https://nodejs.org/download/release/");
        assertThat(NodeRuntimeResolver.getNodeMirror(Collections.singletonMap("release",
          "http://test.mirror.localhost"), "release")).isEqualTo("http://test.mirror.localhost/");
    }

    @ParameterizedTest
    @CsvSource({
      "win32, ia32, '', x86",
      "linux, arm, '', armv7l",
      "linux, x64, '', x64",
      "darwin, arm64, 14.20.0, x64",
      "darwin, arm64, 16.17.0, arm64"
    })
    void getNormalizedArch(String platform, String arch, String nodeVersion, String normalizedArch) {
        assertThat(NodeRuntimeResolver.getNormalizedArch(platform, arch, nodeVersion)).isEqualTo(normalizedArch);
    }
}
