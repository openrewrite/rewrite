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
package org.openrewrite.python.internal.uvlock.marker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Marker behavior recorded from uv 0.13.4. The cases come from its own tests in
 * {@code crates/uv-pep508/src/marker/tree.rs} and from how it renders every distinct marker in the corpus locks,
 * as {@code lock/uv/README.md} of the lock fixtures describes.
 */
class MarkerTreeUpstreamTest {

    static Stream<Map<String, String>> rows() throws IOException {
        try (InputStream in = MarkerTreeUpstreamTest.class.getResourceAsStream("/marker/uv-0.13.4-tree-rows.json")) {
            List<Map<String, String>> rows = new ObjectMapper().readValue(in, new TypeReference<List<Map<String,
                    String>>>() {
            });
            return rows.stream();
        }
    }

    @Test
    void rendersCorpusMarkersAsUv() throws Exception {
        List<String> mismatches = new ArrayList<>();
        int rows = 0;
        try (InputStream in = MarkerTreeUpstreamTest.class.getResourceAsStream("/lock/uv/marker/uv-0.13.4-rendered-markers.tsv")) {
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                String[] cols = line.split("\t");
                rows++;
                String rendered = m(new MarkerInterner(), cols[0]).toString();
                if (!rendered.equals(cols[1])) {
                    mismatches.add(cols[0] + "\n  uv:   " + cols[1] + "\n  port: " + rendered);
                }
            }
        }
        assertThat(rows).isEqualTo(321);
        assertThat(mismatches).isEmpty();
    }

    private static MarkerTree m(MarkerInterner interner, String marker) throws MarkerParser.MarkerSyntaxException {
        return MarkerTree.parse(interner, marker);
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("rows")
    void behavesAsInUv(Map<String, String> row) throws Exception {
        MarkerInterner interner = new MarkerInterner();
        MarkerTree a = m(interner, row.get("a"));
        switch (row.get("kind")) {
            case "simplifies":
                assertThat(a).isEqualTo(m(interner, row.get("b")));
                assertThat(a.tryToString()).isEqualTo(row.get("b"));
                break;
            case "true":
                assertThat(a.isTrue()).isTrue();
                break;
            case "false":
                assertThat(a.isFalse()).isTrue();
                break;
            case "disjoint":
                assertThat(a.isDisjoint(m(interner, row.get("b"))) && m(interner, row.get("b")).isDisjoint(a)).isTrue();
                break;
            case "not_disjoint":
                assertThat(a.isDisjoint(m(interner, row.get("b"))) && m(interner,
                        row.get("b")).isDisjoint(a)).isFalse();
                break;
            case "implies":
                assertThat(a.implies(m(interner, row.get("b"))).isTrue()).isTrue();
                break;
            case "not_implies":
                assertThat(a.implies(m(interner, row.get("b"))).isTrue()).isFalse();
                break;
            default:
                throw new IllegalArgumentException(row.get("kind"));
        }
    }
}
