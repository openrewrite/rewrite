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
package org.openrewrite.javascript.internal.lock.berry;

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.JsYaml;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Yarn's packages/yarnpkg-parsers/tests/syml.test.js, every case.
 */
class SymlTest {

    @Test
    void shouldNotConfuseOldStyleValuesWithNewStyleKeys() {
        assertThat(Syml.parseSyml("# yarn lockfile v1\nfoo \"C:\\\\foobar\"\n"))
                .isEqualTo(map("foo", "C:\\foobar"));
    }

    @Test
    void shouldParseNewStyleObjects() {
        assertThat(Syml.parseSyml("foo:\n  bar: true\n  baz: \"quux\"\n"))
                .isEqualTo(map("foo", map("bar", "true", "baz", "quux")));
    }

    @Test
    void shouldParseOldStyleObjects() {
        assertThat(Syml.parseSyml("# yarn lockfile v1\nfoo:\n  bar true\n  baz \"quux\"\n"))
                .isEqualTo(map("foo", map("bar", "true", "baz", "quux")));
    }

    @Test
    void shouldMergeDuplicates() {
        assertThat(Syml.parseSyml("\n      \"lodash@npm:^4.17.20\":\n        version: 4.17.20\n\n" +
                                  "      \"lodash@npm:^4.17.20\":\n        version: 4.17.20\n      "))
                .isEqualTo(map("lodash@npm:^4.17.20", map("version", "4.17.20")));
    }

    @Test
    void stringifiesAnObject() {
        assertThat(Syml.stringifySyml(map("foo", map("bar", "true", "baz", "quux"))))
                .isEqualTo("foo:\n  bar: true\n  baz: quux\n");
    }

    @Test
    void stringifiesAnObjectWithALongKeyWithYaml12Spec() {
        String longKey = new String(new char[1025]).replace('\0', 'a');
        assertThat(Syml.stringifySyml(map(longKey, map("bar", "true", "baz", "quux"))))
                .isEqualTo("? " + longKey + "\n:\n  bar: true\n  baz: quux\n");
        assertThat(Syml.stringifySyml(map(longKey, map(longKey, "quux", "baz", "quux"))))
                .isEqualTo("? " + longKey + "\n:\n  ? " + longKey + "\n  : quux\n  baz: quux\n");
        assertThat(Syml.stringifySyml(map(longKey, map(longKey, map("aa", "quux", longKey, "zip"), "baz", "zax"))))
                .isEqualTo("? " + longKey + "\n:\n  ? " + longKey + "\n  :\n    aa: quux\n    ? " + longKey +
                           "\n    : zip\n  baz: zax\n");
    }

    @Test
    void quotesWhatIsNotASimpleString() {
        assertThat(Syml.stringifySyml(map("ms@npm:^2.1.0", map("version", "2.1.2", "peer", ">= 16.8.0 || ^17",
                "star", "*", "bang", "!os=win32 & libc=glibc", "conditions", "os=linux & cpu=x64"))))
                .isEqualTo("\"ms@npm:^2.1.0\":\n  version: 2.1.2\n  bang: \"!os=win32 & libc=glibc\"\n" +
                           "  conditions: os=linux & cpu=x64\n  peer: \">= 16.8.0 || ^17\"\n  star: \"*\"\n");
    }

    @Test
    void dropsUndefinedAndMappingsOfOnlyUndefined() {
        assertThat(Syml.stringifySyml(map("a", map("x", Syml.UNDEFINED), "b", map("y", "1", "z", Syml.UNDEFINED))))
                .isEqualTo("b:\n  y: 1\n");
        assertThat(Syml.stringifySyml(map("a", Syml.UNDEFINED))).isEmpty();
    }

    @Test
    void readsEveryScalarAsAString() {
        assertThat(Syml.parseSyml("__metadata:\n  version: 8\n  flag: true\n  none:\n"))
                .isEqualTo(map("__metadata", map("version", "8", "flag", "true", "none", null)));
    }

    @Test
    void rejectsWhatJsYamlRejects() {
        assertThatThrownBy(() -> Syml.parseSyml("a:\n  b: 1\n c d\n"))
                .isInstanceOf(JsYaml.YamlException.class);
        assertThatThrownBy(() -> Syml.parseSyml("foo"))
                .hasMessage("Expected an indexed object, got a string instead. Does your file follow Yaml's rules?");
    }

    private static Map<String, Object> map(Object... keysAndValues) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            out.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return out;
    }
}
