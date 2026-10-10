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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ports bun's test/js/bun/ini/ini.test.ts (bun-v1.3.10): its env var cases, from {@code ini-cases} (written by
 * {@code generate/ini-cases.js}), and its parse cases.
 */
class IniTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static List<Arguments> envVarCases() throws IOException {
        List<Arguments> cases = new ArrayList<>();
        try (InputStream is = IniTest.class.getResourceAsStream("ini-cases")) {
            for (JsonNode c : MAPPER.readTree(is)) {
                cases.add(Arguments.of(c.get("name").asText(), c.get("ini").asText(),
                        MAPPER.convertValue(c.get("env"), Map.class),
                          MAPPER.convertValue(c.get("expected"), Map.class)));
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
        assertThat(parse("\n'{ \"what\": \"is this\" }' = seriously?\n"))
          .isEqualTo(map("[Object object]", "seriously?"));
        assertThat(parse("\n'[1, 2, 3]' = cmon man\n")).isEqualTo(map("1,2,3", "cmon man"));
    }

    @Test
    void reallyLongInput() {
        String a = String.join("", Collections.nCopies(1024, "a"));
        assertThat(parse("\n[" + a + ".lol.this.be.long]\nwow = 'hi'\n"))
          .isEqualTo(map(a, map("lol", map("this", map("be", map("long", map("wow", "hi")))))));
    }

    @Test
    void unicode() {
        assertThat(parse("\nhi👋lol = 'lol hi 👋'\n"))
          .isEqualTo(map("hi👋lol", "lol hi 👋"));
        String facepalm = "\uD83E\uDD26\u200D\u2640\uFE0F";
        assertThat(parse("\n[😎.🫒." + facepalm + "]\nlol = 'wtf'\n    "))
          .isEqualTo(map("😎", map("🫒", map(facepalm, map("lol", "wtf")))));
    }

    /**
     * bun's {@code foo.ini}, the npm/ini fixture, and the object its test expects.
     */
    @Test
    @SuppressWarnings("unchecked")
    void bigboi() throws IOException {
        String ini;
        try (InputStream is = IniTest.class.getResourceAsStream("foo.ini")) {
            ini = new String(readAll(is), StandardCharsets.UTF_8);
        }
        Map<String, Object> expected = MAPPER.readValue("{\" xa  n          p \":\"\\\"\\r\\nyoyoyo\\r\\r\\n\"," +
          "\"[disturbing]\":\"hey you never know\",\"a\":{\"[]\":\"a square?\",\"av\":\"a val\",\"b\":{\"c\":" +
          "{\"e\":\"1\",\"j\":\"2\"}},\"cr\":[\"four\",\"eight\"],\"e\":\"{ o: p, a: { av: a val, b: { c: { e: " +
          "\\\"this [value]\\\" } } } }\",\"j\":\"\\\"{ o: \\\"p\\\", a: { av: \\\"a val\\\", b: { c: { e: \\\"this " +
          "[value]\\\" } } } }\\\"\"},\"a with spaces\":\"b  c\",\"ar\":[\"one\",\"three\",\"this is included\"]," +
          "\"b\":{},\"br\":\"warm\",\"eq\":\"eq=eq\",\"false\":false,\"null\":null,\"o\":\"p\",\"s\":\"something\"," +
          "\"s1\":\"\\\"something'\",\"s2\":\"something else\",\"s3\":\"\",\"s4\":\"\",\"s5\":\"   \",\"s6\":\" a \"," +
          "\"s7\":true,\"true\":true,\"undefined\":\"undefined\",\"x.y.z\":{\"a.b.c\":{\"a.b.c\":\"abc\"," +
          "\"nocomment\":\"this; this is not a comment\",\"noHashComment\":\"this# this is not a comment\"}," +
          "\"x.y.z\":\"xyz\"},\"zr\":[\"deedee\"]}", Map.class);
        assertThat(parse(ini)).isEqualTo(expected);
    }

    /**
     * The truncated and invalid UTF-8 cases, each text as the test builds it (the bytes read as Latin-1): no throw.
     */
    @ParameterizedTest
    @MethodSource("truncatedUtf8")
    void truncatedOrInvalidUtf8DoesNotThrow(String ini) {
        assertThat(parse(ini)).containsKey("key");
    }

    static List<String> truncatedUtf8() {
        int[][] tails = {{0x80}, {0xc0}, {0xe0}, {0xe0, 0x80}, {0xf0}, {0xf0, 0x80}, {0xf0, 0x80, 0x80}};
        List<String> cases = new ArrayList<>();
        for (int[] tail : tails) {
            cases.add("key = " + latin1(tail));
        }
        cases.add("key = \\" + latin1(new int[]{0xc0}));
        cases.add("key = \\" + latin1(new int[]{0x80}));
        return cases;
    }

    private static String latin1(int[] bytes) {
        byte[] b = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            b[i] = (byte) bytes[i];
        }
        return new String(b, StandardCharsets.ISO_8859_1);
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int n; (n = is.read(buf)) > 0; ) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
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
