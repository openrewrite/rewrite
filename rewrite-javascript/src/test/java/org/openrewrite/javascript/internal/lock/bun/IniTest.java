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
package org.openrewrite.javascript.internal.lock.bun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.HostEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ports bun's test/js/bun/ini/ini.test.ts (bun-v1.3.10): its env var cases, from {@code ini-cases.json} (written by
 * {@code generate/ini-cases.js}), and its parse cases.
 */
class IniTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static List<Arguments> envVarCases() throws IOException {
        List<Arguments> cases = new ArrayList<>();
        try (InputStream is = IniTest.class.getResourceAsStream("ini-cases.json")) {
            for (JsonNode c : MAPPER.readTree(is)) {
                cases.add(Arguments.of(c.get("name").asText(), c.get("ini").asText(),
                        MAPPER.convertValue(c.get("env"), Map.class), MAPPER.convertValue(c.get("expected"), Map.class)));
            }
        }
        return cases;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("envVarCases")
    void envVars(String name, String ini, Map<String, String> env, Map<String, Object> expected) {
        assertThat(Ini.parse(ini, HostEnvironment.of(env))).isEqualTo(expected);
    }

    @Test
    void weirdSection() {
        assertThat(parse("\n[foo\\]]\nlol = true\n")).isEqualTo(map("[foo\\]]", true, "lol", true));
    }

    @Test
    void basic() {
        assertThat(parse("\n    hello = 'friends'\n    ")).isEqualTo(map("hello", "friends"));
    }

    @Test
    void basicSections() {
        assertThat(parse("\nhello = 'friends'\n\n[foo]\nbar = 'baz'\n    "))
          .isEqualTo(map("hello", "friends", "foo", map("bar", "baz")));
    }

    @Test
    void keyAndThenSection() {
        assertThat(parse("\nfoo = 'hihihi'\n\n[foo]\nisbar = 'lol'\n    ")).isEqualTo(map("foo", "hihihi"));
    }

    @Test
    void dottedSectionsNest() {
        assertThat(parse("\n[a.b]\nwow = 'hi'\n"))
          .isEqualTo(map("a", map("b", map("wow", "hi"))));
    }

    @Test
    void duplicateProperties() {
        assertThat(parse("\nzr[] = deedee\nzr=123\nar[] = one\nar[] = three\nstr = 3\nbrr = 1\nbrr = 2\nbrr = 3\n" +
                         "brr = 3\n"))
          .isEqualTo(map("zr", Arrays.asList("deedee", "123"), "ar", Arrays.asList("one", "three"), "str", "3",
            "brr", "3"));
    }

    @Test
    void matchesNpmIni() {
        assertThat(parse("\n'{ \"what\": \"is this\" }' = seriously?\n")).isEqualTo(map("[Object object]", "seriously?"));
        assertThat(parse("\n'[1, 2, 3]' = cmon man\n")).isEqualTo(map("1,2,3", "cmon man"));
    }

    @Test
    void anUnsetVariableUnderAnUnknownEnvironmentIsNotKnown() {
        assertThatThrownBy(() -> Ini.parse("registry=${NPM_REGISTRY}\n", HostEnvironment.UNKNOWN))
          .isInstanceOf(CannotKnow.class)
          .hasMessageContaining("NPM_REGISTRY");
        assertThat(Ini.parse("registry=${REG}\n", HostEnvironment.UNKNOWN.with(Collections.singletonMap("REG",
          "https://example.com/")))).isEqualTo(map("registry", "https://example.com/"));
    }

    private static Map<String, Object> parse(String ini) {
        return Ini.parse(ini, HostEnvironment.of(Collections.emptyMap()));
    }

    private static Map<String, Object> map(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }
}
