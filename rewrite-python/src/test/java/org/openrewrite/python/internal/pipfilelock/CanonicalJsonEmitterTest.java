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
package org.openrewrite.python.internal.pipfilelock;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.python.internal.pipfilelock.Fixtures.resource;

/**
 * All expected values in this class were recorded from Python 3.14 via
 * {@code src/test/resources/pipfilelock/oracle.py compact}, i.e.
 * {@code json.dumps(obj, sort_keys=True, separators=(",", ":"))}.
 */
class CanonicalJsonEmitterTest {

    @Test
    void tortureCorpusMatchesPythonJsonDumps() {
        Map<String, Object> torture = PipfileLockWriter.read(resource("torture.input"));
        assertThat(CanonicalJsonEmitter.emit(torture)).isEqualTo(resource("torture.expected.compact"));
    }

    @Test
    void keysSortByCodePointNotUtf16CodeUnit() {
        // Python sorts str by code point: U+FFFD before U+1F600; String.compareTo would reverse them
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("\uD83D\uDE00", 2L);
        map.put("\uFFFD", 1L);
        assertThat(CanonicalJsonEmitter.emit(map)).isEqualTo("{\"\\ufffd\":1,\"\\ud83d\\ude00\":2}");
    }

    @Test
    void scalars() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("b", true);
        map.put("a", null);
        map.put("n", -3L);
        map.put("big", new BigInteger("123456789012345678901234567890"));
        map.put("s", "x/y");
        assertThat(CanonicalJsonEmitter.emit(map))
          .isEqualTo("{\"a\":null,\"b\":true,\"big\":123456789012345678901234567890,\"n\":-3,\"s\":\"x/y\"}");
    }

    @Test
    void nestedContainersSortAtEveryLevel() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("z", asList());
        nested.put("y", new LinkedHashMap<>());
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("b", 1L);
        inner.put("a", 2L);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("nested", nested);
        map.put("list", asList(inner, asList(true, false, null)));
        assertThat(CanonicalJsonEmitter.emit(map))
          .isEqualTo("{\"list\":[{\"a\":2,\"b\":1},[true,false,null]],\"nested\":{\"y\":{},\"z\":[]}}");
    }

    @Test
    void loneSurrogateEscapesLikePython() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("k", "\uD800");
        assertThat(CanonicalJsonEmitter.emit(map)).isEqualTo("{\"k\":\"\\ud800\"}");
    }

    @Test
    void rejectsFloats() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("f", 1.5d);
        assertThatThrownBy(() -> CanonicalJsonEmitter.emit(map))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonStringKeys() {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put(1L, "v");
        assertThatThrownBy(() -> CanonicalJsonEmitter.emit(map))
          .isInstanceOf(IllegalArgumentException.class);
    }
}
