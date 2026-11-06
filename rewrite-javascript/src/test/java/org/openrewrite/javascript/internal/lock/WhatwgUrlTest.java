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
package org.openrewrite.javascript.internal.lock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Parity with Node's URL (v24.19.0) over the URL shapes npm specs take, as dumped by
 * npm-arborist/generate/url-cases.js.
 */
class WhatwgUrlTest {

    static Stream<Arguments> cases() {
        try (InputStream is = WhatwgUrlTest.class.getResourceAsStream("/npm-arborist/url-cases")) {
            List<Arguments> rows = new ArrayList<>();
            for (JsonNode row : new ObjectMapper().readTree(is)) {
                rows.add(Arguments.of(row.get("input").asText(), row.get("base").isNull() ? null :
                        row.get("base").asText(), row));
            }
            return rows.stream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ParameterizedTest(name = "{0} against {1}")
    @MethodSource("cases")
    void parsesAsNodeDoes(String input, @Nullable String base, JsonNode expected) {
        if (expected.has("invalid")) {
            assertThatThrownBy(() -> WhatwgUrl.parse(input, base)).isInstanceOf(IllegalArgumentException.class);
            return;
        }
        WhatwgUrl url = WhatwgUrl.parse(input, base);
        assertThat(url.getHref()).as("href").isEqualTo(expected.get("href").asText());
        assertThat(url.getProtocol()).as("protocol").isEqualTo(expected.get("protocol").asText());
        assertThat(url.getUsername()).as("username").isEqualTo(expected.get("username").asText());
        assertThat(url.getPassword()).as("password").isEqualTo(expected.get("password").asText());
        assertThat(url.getHost()).as("host").isEqualTo(expected.get("host").asText());
        assertThat(url.getHostname()).as("hostname").isEqualTo(expected.get("hostname").asText());
        assertThat(url.getPort()).as("port").isEqualTo(expected.get("port").asText());
        assertThat(url.getPathname()).as("pathname").isEqualTo(expected.get("pathname").asText());
        assertThat(url.getSearch()).as("search").isEqualTo(expected.get("search").asText());
        assertThat(url.getHash()).as("hash").isEqualTo(expected.get("hash").asText());
    }
}
