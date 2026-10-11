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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.arr;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;

/**
 * Port of json-stringify-nice test/basic.js with its tap snapshots, and JSON.stringify's own rendering
 * of the values a lock holds.
 */
class JsonStringifyNiceTest {

    @Test
    void basicSortingOperationWithDefaultTwoSpaceIndent() {
        assertThat(JsonStringifyNice.stringify(obj(
          "c", 1, "ch", 2, "d", 3, "z", 1, "y", "z",
          "obj", obj("a", obj(), "b", "x"),
          "a", obj("b", 1, "a", 2),
          "yy", "a"))).isEqualTo(
          "{\n" +
          "  \"c\": 1,\n" +
          "  \"ch\": 2,\n" +
          "  \"d\": 3,\n" +
          "  \"y\": \"z\",\n" +
          "  \"yy\": \"a\",\n" +
          "  \"z\": 1,\n" +
          "  \"a\": {\n" +
          "    \"a\": 2,\n" +
          "    \"b\": 1\n" +
          "  },\n" +
          "  \"obj\": {\n" +
          "    \"b\": \"x\",\n" +
          "    \"a\": {}\n" +
          "  }\n" +
          "}\n");
    }

    @Test
    void throwsSameErrorOnCyclesAsJsonStringify() {
        Map<String, Object> b = obj();
        Map<String, Object> a = obj("b", b);
        b.put("c", a);
        Map<String, Object> cycle = obj("a", a);
        String builtin = "TypeError: Converting circular structure to JSON\n" +
                         "    --> starting at object with constructor 'Object'\n" +
                         "    |     property 'b' -> object with constructor 'Object'\n" +
                         "    --- property 'c' closes the circle";
        assertThatThrownBy(() -> Js.jsonStringify(cycle)).hasToString(builtin);
        assertThatThrownBy(() -> JsonStringifyNice.stringify(cycle)).hasToString(builtin);
    }

    private static Map<String, Object> spacesObj() {
        return obj("z", 1, "y", "z", "obj", obj("a", obj(), "b", "x"), "a", obj("b", 1, "a", 2), "yy", "a");
    }

    @Test
    void spacesCanBeSetTab() {
        assertThat(JsonStringifyNice.stringify(spacesObj(), Collections.emptyList(), "\t")).isEqualTo(
          "{\n\t\"y\": \"z\",\n\t\"yy\": \"a\",\n\t\"z\": 1,\n\t\"a\": {\n\t\t\"a\": 2,\n\t\t\"b\": 1\n\t},\n" +
          "\t\"obj\": {\n\t\t\"b\": \"x\",\n\t\t\"a\": {}\n\t}\n}\n");
    }

    @Test
    void spacesCanBeSetSpaceFace() {
        assertThat(JsonStringifyNice.stringify(spacesObj(), Collections.emptyList(), " ^_^ ")).isEqualTo(
          "{\n ^_^ \"y\": \"z\",\n ^_^ \"yy\": \"a\",\n ^_^ \"z\": 1,\n ^_^ \"a\": {\n ^_^  ^_^ \"a\": 2,\n" +
          " ^_^  ^_^ \"b\": 1\n ^_^ },\n ^_^ \"obj\": {\n ^_^  ^_^ \"b\": \"x\",\n ^_^  ^_^ \"a\": {}\n ^_^ }\n}\n");
    }

    @Test
    void spacesCanBeSetTheNumberThree() {
        assertThat(JsonStringifyNice.stringify(spacesObj(), Collections.emptyList(), 3)).isEqualTo(
          "{\n   \"y\": \"z\",\n   \"yy\": \"a\",\n   \"z\": 1,\n   \"a\": {\n      \"a\": 2,\n      \"b\": 1\n   " +
          "},\n   \"obj\": {\n      \"b\": \"x\",\n      \"a\": {}\n   }\n}\n");
    }

    @Test
    void spacesCanBeSetEmptyStringAndFalse() {
        String minified = "{\"y\":\"z\",\"yy\":\"a\",\"z\":1,\"a\":{\"a\":2,\"b\":1},\"obj\":{\"b\":\"x\",\"a\":{}}}";
        assertThat(JsonStringifyNice.stringify(spacesObj(), Collections.emptyList(), "")).isEqualTo(minified);
        assertThat(JsonStringifyNice.stringify(spacesObj(), Collections.emptyList(), false)).isEqualTo(minified);
        assertThat(JsonStringifyNice.stringify(spacesObj(), Collections.emptyList(), 0)).isEqualTo(minified);
        assertThat(JsonStringifyNice.stringify(spacesObj(), Collections.emptyList(), null)).isEqualTo(minified);
    }

    // the replacer-function case is not ported: shrinkwrap.js passes only a key list

    @Test
    void sortKeysExplicitlyWithAPreferenceList() {
        Map<String, Object> o = obj("z", 1, "y", "z", "obj", obj("a", obj(), "b", "x"),
          "a", obj("b", 1, "a", obj("nested", true)), "yy", "a");
        assertThat(JsonStringifyNice.stringify(o, Arrays.asList("obj", "z", "yy"), 2)).isEqualTo(
          "{\n  \"z\": 1,\n  \"yy\": \"a\",\n  \"y\": \"z\",\n  \"obj\": {\n    \"b\": \"x\",\n    \"a\": {}\n  },\n" +
          "  \"a\": {\n    \"b\": 1,\n    \"a\": {\n      \"nested\": true\n    }\n  }\n}\n");
    }

    // npm 11.17.0's bundled json-stringify-nice 1.1.4, run over an own __proto__ as JSON.parse makes one
    @Test
    void anOwnProtoKeyIsLeftOut() {
        assertThat(JsonStringifyNice.stringify(obj("__proto__", false, "a b", 1))).isEqualTo("{\n  \"a b\": 1\n}\n");
        assertThat(JsonStringifyNice.stringify(obj("a", obj("__proto__", obj("x", 1)), "b", 2))).isEqualTo(
          "{\n  \"b\": 2,\n  \"a\": {}\n}\n");
    }

    @Test
    void dontBeConfusedByEmptyStringsOrOtherFalseyValues() {
        assertThat(JsonStringifyNice.stringify(obj("z", "", "y", null, "w", false, "x", 0))).isEqualTo(
          "{\n  \"w\": false,\n  \"x\": 0,\n  \"y\": null,\n  \"z\": \"\"\n}\n");
    }

    @Test
    void arraysSortBeforeObjectsAndKeepTheirOrder() {
        assertThat(JsonStringifyNice.stringify(obj("b", obj("x", 1), "a", arr(3, obj("z", 1, "a", 2), arr()), "c",
                                                   arr())))
          .isEqualTo("{\n  \"a\": [\n    3,\n    {\n      \"a\": 2,\n      \"z\": 1\n    },\n    []\n  ],\n  \"c\": " +
                     "[],\n  \"b\": {\n    \"x\": 1\n  }\n}\n");
    }

    @Test
    void indentLongerThanTenCharactersIsCut() {
        assertThat(JsonStringifyNice.stringify(obj("a", 1), Collections.emptyList(), "123456789012"))
          .isEqualTo("{\n1234567890\"a\": 1\n}\n");
        assertThat(JsonStringifyNice.stringify(obj("a", 1), Collections.emptyList(), 12))
          .isEqualTo("{\n          \"a\": 1\n}\n");
    }

    @Test
    void stringsEscapeAsJsonStringifyDoes() {
        assertThat(JsonStringifyNice.stringify(obj("k\"\\\n\t\u0001\u007f\u2028", "\ud83d\udc15 \ud800"),
          Collections.emptyList(), 0))
          .isEqualTo("{\"k\\\"\\\\\\n\\t\\u0001\u007f\u2028\":\"\ud83d\udc15 \\ud800\"}");
    }

    @ParameterizedTest
    @CsvSource({
      "3, 3", "3.0, 3", "-0.0, 0", "0.1, 0.1", "1e21, 1e+21", "1e20, 100000000000000000000", "1.5e-7, 1.5e-7",
      "0.000001, 0.000001", "123456789012345680000, 123456789012345680000", "2.5e-5, 0.000025",
      "1.7976931348623157e308, 1.7976931348623157e+308", "5e-324, 5e-324", "100, 100", "-42, -42",
      "12345678901234567890, 12345678901234567000",
      // powers of two: the nearest shorter decimal falls outside the asymmetric interval while a farther one reads back
      "5.6843418860808015e-14, 5.684341886080802e-14", "5.9604644775390625e-8, 5.960464477539063e-8",
      "-5.9604644775390625e-8, -5.960464477539063e-8", "2.98023223876953125e-8, 2.9802322387695312e-8",
      "36028797018963968, 36028797018963970", "1152921504606846976, 1152921504606847000",
      "1.1805916207174113e21, 1.1805916207174113e+21", "8.673617379884035e-19, 8.673617379884035e-19",
      "9223372036854775808, 9223372036854776000", "18446744073709551616, 18446744073709552000",
      "1e-323, 1e-323", "2.2250738585072014e-308, 2.2250738585072014e-308",
      "1.2676506002282294e30, 1.2676506002282294e+30", "7.888609052210118e-31, 7.888609052210118e-31",
      "1e23, 1e+23", "8.41e21, 8.41e+21", "9007199254740993, 9007199254740992", "2.5e-324, 5e-324",
      "4.35e-321, 4.35e-321", "123e-20, 1.23e-18", "0.1e1, 1", "1e22, 1e+22", "100000000000000000000000, 1e+23"
    })
    void numbersRenderAsJsonStringifyDoes(String json, String expected) {
        Object value = JsonParseEvenBetterErrors.parse(json).getValue();
        assertThat(JsonStringifyNice.stringify(value, Collections.emptyList(), 0)).isEqualTo(expected);
    }
}
