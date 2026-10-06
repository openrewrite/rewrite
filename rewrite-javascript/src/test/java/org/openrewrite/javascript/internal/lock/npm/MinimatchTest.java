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

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.fixture;

/**
 * Minimatch against the answers of the minimatch npm bundles (the minimatch-cases fixture).
 */
class MinimatchTest {

    private static final JsonNode CASES = fixture("minimatch-cases");

    static Stream<Arguments> pairs() {
        List<Arguments> out = new ArrayList<>();
        for (JsonNode pair : CASES.get("pairs")) {
            out.add(Arguments.of(pair.get("path").asText(), pair.get("pattern").asText(), pair.get("options"), pair.get("matches").asBoolean()));
        }
        return out.stream();
    }

    @ParameterizedTest(name = "{0} ~ {1} {2}")
    @MethodSource("pairs")
    void matchesAsMinimatchDoes(String path, String pattern, JsonNode options, boolean expected) {
        Minimatch.Options o = Minimatch.Options.builder()
          .dot(options.path("dot").asBoolean())
          .partial(options.path("partial").asBoolean())
          .nocomment(options.path("nocomment").asBoolean())
          .nonegate(options.path("nonegate").asBoolean())
          .windowsPathsNoEscape(options.path("windowsPathsNoEscape").asBoolean())
          .build();
        assertThat(Minimatch.minimatch(path, pattern, o)).isEqualTo(expected);
    }

    @Test
    void extglobsAreRefusedByName() {
        assertThatThrownBy(() -> Minimatch.minimatch("a/b", "a/+(b|c)", Minimatch.Options.DEFAULT))
          .isInstanceOf(Minimatch.UnsupportedPattern.class)
          .hasMessageContaining("\"a/+(b|c)\"");
    }
}
