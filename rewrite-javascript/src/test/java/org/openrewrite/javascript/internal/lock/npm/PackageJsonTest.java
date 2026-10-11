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
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.json;

class PackageJsonTest {

    // @npmcli/package-json 8.0.0 normalize.js, which npm 12.0.0 bundles: the overrides step leaves the field absent or
    // an object with keys ('"overrides" was removed (must be an object)', 'Empty "overrides" was removed'). Read off
    // the source: npm 11.21.0 and 12.0.0 write the same lock over a project without dependencies for each of these.
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "[\"is-number@7.0.0\"] | false",
      "\"$foo\" | false",
      "null | false",
      "{} | false",
      "[] | false",
      "1 | false",
      "true | false",
      "{\"is-number\":\"7.0.0\"} | true"
    })
    void theOverridesStepKeepsOnlyAnObjectWithKeysFrom12(String overrides, boolean kept) {
        String text = "{\"name\":\"a\",\"version\":\"1.0.0\",\"overrides\":" + overrides + "}";
        for (String release : new String[]{"12.0.0", "12.2.0"}) {
            assertThat(PackageJson.normalize(text, NpmRelease.of(release)).getContent().containsKey("overrides"))
              .isEqualTo(kept);
            assertThat(PackageJson.syncNormalize(json(text), NpmRelease.of(release)).containsKey("overrides"))
              .isEqualTo(kept);
        }
        assertThat(PackageJson.normalize(text, NpmRelease.of("11.21.0")).getContent()).containsKey("overrides");
        assertThat(PackageJson.syncNormalize(json(text), NpmRelease.of("11.21.0"))).containsKey("overrides");
    }
}
