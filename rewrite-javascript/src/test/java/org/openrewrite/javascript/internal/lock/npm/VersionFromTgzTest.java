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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
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
}
