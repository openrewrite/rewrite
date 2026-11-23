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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * js-yaml 3.13.1's {@code safeLoad} under {@code FAILSAFE_SCHEMA}, as yarn 1.22.22's bundled copy answered each text
 * (the test resources' {@code generate/bundle-rows.js} prints the rows).
 */
class JsYamlTest {

    static Stream<Arguments> documents() {
        return Stream.of(
                Arguments.of("'k': \n", "{\"k\":null}"),
                Arguments.of("'k': !!int 5\n", "throws"),
                Arguments.of("'k': [a, , b]\n", "{\"k\":[\"a\",null,\"b\"]}"),
                Arguments.of("'k': !!binary aGk=\n", "throws"),
                Arguments.of("'k':\n\t- a\n", "{\"k\":[\"a\"]}"),
                Arguments.of("'k': {a: }\n", "{\"k\":{\"a\":null}}"),
                Arguments.of("'k': !!null ''\n", "throws"),
                Arguments.of("'k': !!bool true\n", "throws"),
                Arguments.of("'k': !!str\n", "{\"k\":\"\"}"),
                Arguments.of("'k': !!seq\n", "{\"k\":[]}"),
                Arguments.of("'k': ! v\n", "{\"k\":\"v\"}"),
                Arguments.of("'a': &x {version: '1'}\n'b': *x\n",
                        "{\"a\":{\"version\":\"1\"},\"b\":{\"version\":\"1\"}}"),
                Arguments.of("'__proto__': {version: '1'}\n'b': '2'\n", "{\"b\":\"2\"}"),
                Arguments.of("'hasOwnProperty': &hasOwnProperty x\n'b': *hasOwnProperty\n", "throws"),
                Arguments.of("'1': a\n'0': b\n'x': c\n", "{\"0\":\"b\",\"1\":\"a\",\"x\":\"c\"}"),
                Arguments.of("'k': \"\\u0041\\x42\\U0001F600\"\n", "{\"k\":\"AB😀\"}"),
                Arguments.of("'k': |-\n  line1\n  line2\n", "{\"k\":\"line1\\nline2\"}"),
                Arguments.of("'k': >+\n  a\n\n", "{\"k\":\"a\\n\\n\"}"),
                Arguments.of("? [a, b]\n: c\n", "{\"a,b\":\"c\"}"),
                Arguments.of("? {a: b}\n: c\n", "{\"[object Object]\":\"c\"}"),
                Arguments.of("%YAML 2.0\n---\n'k': v\n", "throws"),
                Arguments.of("---\n'k': v\n---\n'j': w\n", "throws"),
                Arguments.of("'k': v\n'k': w\n", "throws"),
                Arguments.of("x: [a, b]\n", "{\"x\":[\"a\",\"b\"]}")
        );
    }

    @ParameterizedTest
    @MethodSource("documents")
    void loadsAsYarnsJsYamlDoes(String text, String json) {
        String answer;
        try {
            answer = Js.jsonStringify(LockfileParser.safeLoadFailsafe(text));
        } catch (JsError e) {
            answer = "throws";
        }
        assertThat(answer).isEqualTo(json);
    }

    @Test
    void anAliasIsTheObjectItsAnchorNames() {
        Map<?, ?> document = (Map<?, ?>) LockfileParser.safeLoadFailsafe("'a': &x {version: '1'}\n'b': *x\n");
        assertThat(document.get("b")).isSameAs(document.get("a"));
    }
}
