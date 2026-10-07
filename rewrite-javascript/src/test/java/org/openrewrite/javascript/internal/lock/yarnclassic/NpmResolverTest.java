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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The version a range picks from a packument. The recording {@code open-dist-tag-latest-missing} (yarn 1.22.22, a
 * registry serving left-pad with no dist-tags and 1.2.0 as its last version key) locked {@code left-pad@latest} at
 * 1.2.0: a {@code latest} range the dist-tags do not name takes the last version listed, not the highest.
 */
class NpmResolverTest {

    @SuppressWarnings("unchecked")
    private static final Map<String, Object> NO_LATEST_TAG = (Map<String, Object>) Js.jsonParse(
            "{\"name\": \"left-pad\", \"dist-tags\": {}, \"versions\": {" +
            "\"1.1.3\": {\"name\": \"left-pad\", \"version\": \"1.1.3\"}, " +
            "\"1.3.0\": {\"name\": \"left-pad\", \"version\": \"1.3.0\"}, " +
            "\"1.2.0\": {\"name\": \"left-pad\", \"version\": \"1.2.0\"}}}");

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "latest, 1.2.0",
            "^1.1.0, 1.3.0",
            "1.1.x, 1.1.3",
    })
    void picksAVersionWithoutALatestTag(String range, String version) {
        Map<String, Object> picked = NpmResolver.findVersionInRegistryResponse(YarnClassicOptions.defaults(),
                "left-pad", range, NO_LATEST_TAG);
        assertThat(picked.get("version")).isEqualTo(version);
    }
}
