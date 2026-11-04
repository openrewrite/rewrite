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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.UpstreamCalls;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.VersionFromTgz.versionFromTgz;

/**
 * Port of @npmcli/arborist test/version-from-tgz.js.
 */
class VersionFromTgzTest {

    @ParameterizedTest
    @CsvSource({
      "asdf-foo, asdf-foo-1.2.3.tgz, asdf-foo",
      "asdf-foo, /path/to/asdf-foo-1.2.3.tgz, asdf-foo",
      "asdf-foo, https://x.y/p/a/t/h/asdf-foo-1.2.3.tgz, asdf-foo",
      "@asdf/foo, https://x.y/p/a/t/h/@asdf%2ffoo/-/foo-1.2.3.tgz, @asdf/foo",
      "blorg, https://x.y/p/a/t/h/@asdf%2ffoo/-/foo-1.2.3.tgz, @asdf/foo",
      "@asdf/foo, https://x.y/p/a/t/h/@asdf%2Ffoo/-/foo-1.2.3.tgz, @asdf/foo",
      "blorg, https://x.y/p/a/t/h/@asdf%2Ffoo/-/foo-1.2.3.tgz, @asdf/foo",
      "@asdf/foo, https://x.y/p/a/t/h/@asdf/foo/-/foo-1.2.3.tgz, @asdf/foo",
      "blorg, https://x.y/p/a/t/h/@asdf/foo/-/foo-1.2.3.tgz, @asdf/foo",
      "x, https://x.y/p/a/t/h/-/@foo/$bar/-/@asdf/foo/-/foo-1.2.3.tgz, @asdf/foo",
    })
    void nameAndVersion(String name, String tgz, String expectedName) {
        assertThat(versionFromTgz(name, tgz)).isEqualTo(new VersionFromTgz.NameVersion(expectedName, "1.2.3"));
    }

    @ParameterizedTest
    @CsvSource({
      "x, https://x.y/p/a/t/h/-/@foo/$bar/-/@asdf/foo/foo-1.2.3.tgz",
      "x, https://x.y/-/-/foo-1.2.3.tgz",
      "x, https://host.com/api/v1/tar.gz/master",
      "x, /path/to/x.tgz",
      "x, /path/to/x-a3wasf.tgz",
    })
    void nothingToRead(String name, String tgz) {
        assertThat(versionFromTgz(name, tgz)).isNull();
    }

    static Stream<Arguments> releaseRows() {
        List<Arguments> out = new ArrayList<>();
        for (UpstreamCalls.Call call : UpstreamCalls.read("version-from-tgz-10.4.0-11.17.0.jsonl.gz")) {
            for (String release : new String[]{"10.4.0", "11.17.0"}) {
                out.add(Arguments.of(release, call.arg(0), call.arg(1), ((Map<?, ?>) call.getOut()).get(release)));
            }
        }
        return out.stream();
    }

    /**
     * What version-from-tgz.js answers as npm 10.4.0 bundles it, reading the URL with url.parse, and as npm 11.17.0
     * does, with WHATWG URL (rows written by generate/version-from-tgz.js).
     */
    @ParameterizedTest(name = "{0} {1} {2}")
    @MethodSource("releaseRows")
    void answersAsTheReleaseDoes(String release, String name, String tgz, @Nullable Object expected) {
        if (expected instanceof Map && ((Map<?, ?>) expected).containsKey("$")) {
            assertThatThrownBy(() -> versionFromTgz(name, tgz, NpmRelease.of(release)))
              .hasMessage((String) ((Map<?, ?>) expected).get("message"));
            return;
        }
        VersionFromTgz.NameVersion got = versionFromTgz(name, tgz, NpmRelease.of(release));
        if (expected == null) {
            assertThat(got).isNull();
        } else {
            assertThat(got).isEqualTo(new VersionFromTgz.NameVersion((String) ((Map<?, ?>) expected).get("name"),
              (String) ((Map<?, ?>) expected).get("version")));
        }
    }
}
