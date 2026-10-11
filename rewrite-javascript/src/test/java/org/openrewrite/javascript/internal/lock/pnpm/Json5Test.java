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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.EcmaScript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * json5 2.2.3's own test/parse.js and test/errors.js inputs, with what its parse answers ({@code
 * pnpm/differential/json5.jsonl}, written by {@code pnpm/generate/json5.js rows}).
 */
class Json5Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @SuppressWarnings("unchecked")
    static Stream<Arguments> rows() {
        List<Arguments> out = new ArrayList<>();
        InputStream in = Json5Test.class.getClassLoader().getResourceAsStream("pnpm/differential/json5.jsonl");
        assertThat(in).isNotNull();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Map<String, Object> row = MAPPER.readValue(line, LinkedHashMap.class);
                out.add(Arguments.of(row.get("text"), row));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rows")
    void parsesAsJson5Does(String text, Map<String, Object> row) {
        if (row.containsKey("error")) {
            assertThatThrownBy(() -> Json5.parse(text))
              .isInstanceOfSatisfying(Json5.SyntaxError.class, e -> {
                  assertThat(e.getMessage()).isEqualTo(row.get("error"));
                  assertThat(e.getLineNumber()).isEqualTo(row.get("line"));
                  assertThat(e.getColumnNumber()).isEqualTo(row.get("column"));
              });
            return;
        }
        Object parsed = Json5.parse(text);
        assertThat(parsed).isEqualTo(decode(row.get("value")));
        assertThat(keyOrder(parsed)).isEqualTo(keyOrder(decode(row.get("value"))));
    }

    @Test
    void readsAPackageJson5AsPnpmDoes() {
        assertThat(Manifests.readJson5("\uFEFF// c\n{name: 'c', dependencies: {once: '1.4.0',},}", "c/package.json5"))
          .containsEntry("name", "c");
        assertThatThrownBy(() -> Manifests.readJson5("{version: 1.4.0}", "c/package.json5"))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_JSON5_PARSE");
              assertThat(e.getMessage()).isEqualTo("JSON5: invalid character '.' at 1:14 in c/package.json5");
          });
    }

    // each object's keys in the order JavaScript enumerates them, which Map.equals ignores
    @SuppressWarnings("unchecked")
    private static @Nullable Object keyOrder(@Nullable Object value) {
        List<@Nullable Object> order = new ArrayList<>();
        if (value instanceof Map) {
            for (String key : EcmaScript.keys((Map<String, ?>) value)) {
                order.add(key);
                order.add(keyOrder(((Map<String, ?>) value).get(key)));
            }
            return order;
        }
        if (value instanceof List) {
            for (Object item : (List<Object>) value) {
                order.add(keyOrder(item));
            }
            return order;
        }
        return null;
    }

    // the generator writes a number JSON cannot hold as {"$number": "NaN" | "Infinity" | "-Infinity" | "-0"}
    @SuppressWarnings("unchecked")
    private static @Nullable Object decode(@Nullable Object value) {
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            if (map.size() == 1 && map.get("$number") instanceof String) {
                return Double.parseDouble((String) map.get("$number"));
            }
            Map<String, @Nullable Object> out = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                out.put(entry.getKey(), decode(entry.getValue()));
            }
            return EcmaScript.jsonValue(out);
        }
        if (value instanceof List) {
            List<@Nullable Object> out = new ArrayList<>();
            for (Object item : (List<Object>) value) {
                out.add(decode(item));
            }
            return out;
        }
        return EcmaScript.jsonValue(value);
    }
}
