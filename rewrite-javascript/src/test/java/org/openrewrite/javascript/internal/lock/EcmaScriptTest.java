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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.EcmaScript.HOLE;
import static org.openrewrite.javascript.internal.lock.EcmaScript.UNDEFINED;

/**
 * What Node v24.19.0 answers for the same expressions.
 */
class EcmaScriptTest {

    @Test
    void stringOfPrimitives() {
        assertThat(EcmaScript.string(null)).isEqualTo("null");
        assertThat(EcmaScript.string(UNDEFINED)).isEqualTo("undefined");
        assertThat(EcmaScript.string(true)).isEqualTo("true");
        assertThat(EcmaScript.string(-0.0)).isEqualTo("0");
        assertThat(EcmaScript.string(9007199254740993L)).isEqualTo("9007199254740992");
    }

    @Test
    void stringOfNumbersIsV8s() {
        assertThat(EcmaScript.string(1e21)).isEqualTo("1e+21");
        assertThat(EcmaScript.string(123456789012345680000.0)).isEqualTo("123456789012345680000");
        assertThat(EcmaScript.string(0.0001)).isEqualTo("0.0001");
        assertThat(EcmaScript.string(1e-7)).isEqualTo("1e-7");
        assertThat(EcmaScript.string(12345678.5)).isEqualTo("12345678.5");
    }

    @Test
    void stringOfObjects() {
        assertThat(EcmaScript.string(Arrays.asList(1, Arrays.asList(2, 3), null, UNDEFINED))).isEqualTo("1,2,3,,");
        assertThat(EcmaScript.string(Arrays.asList(HOLE, 1))).isEqualTo(",1");
        assertThat(EcmaScript.string(new LinkedHashMap<>())).isEqualTo("[object Object]");
        assertThat(EcmaScript.string(new byte[]{1, (byte) 255})).isEqualTo("1,255");
    }

    @Test
    void typeOf() {
        assertThat(EcmaScript.typeOf(null)).isEqualTo("object");
        assertThat(EcmaScript.typeOf(UNDEFINED)).isEqualTo("undefined");
        assertThat(EcmaScript.typeOf(new ArrayList<>())).isEqualTo("object");
        assertThat(EcmaScript.typeOf("")).isEqualTo("string");
        assertThat(EcmaScript.typeOf(1.5)).isEqualTo("number");
        assertThat(EcmaScript.typeOf(false)).isEqualTo("boolean");
    }

    @Test
    void jsonStringify() {
        Map<String, Object> object = new LinkedHashMap<>();
        object.put("b", 1);
        object.put("2", true);
        object.put("a", Arrays.asList(1, UNDEFINED, null, "x\n"));
        object.put("u", UNDEFINED);
        assertThat(EcmaScript.jsonStringify(object)).isEqualTo("{\"2\":true,\"b\":1,\"a\":[1,null,null,\"x\\n\"]}");
        assertThat(EcmaScript.jsonStringify(UNDEFINED)).isNull();
        assertThat(EcmaScript.jsonStringify(Double.NaN)).isEqualTo("null");
        assertThat(EcmaScript.jsonStringify(Arrays.asList(HOLE, 1))).isEqualTo("[null,1]");
        assertThat(EcmaScript.jsonStringify(1e21)).isEqualTo("1e+21");
        String escaped = "\"\\" + "ud800" + (char) 0x2028 + "\"";
        assertThat(EcmaScript.jsonStringify(new String(new char[]{0xD800, 0x2028}))).isEqualTo(escaped);
    }

    @Test
    void strictEquals() {
        assertThat(EcmaScript.strictEquals(5, 5.0)).isTrue();
        assertThat(EcmaScript.strictEquals(Double.NaN, Double.NaN)).isFalse();
        assertThat(EcmaScript.strictEquals("a", "a")).isTrue();
        assertThat(EcmaScript.strictEquals(null, UNDEFINED)).isFalse();
        assertThat(EcmaScript.strictEquals(new ArrayList<>(), new ArrayList<>())).isFalse();
    }

    @Test
    void keys() {
        List<Object> withHole = Arrays.asList(HOLE, 1);
        assertThat(EcmaScript.keys((Object) withHole)).containsExactly("1");
        assertThat(EcmaScript.keys("ab")).containsExactly("0", "1");
        assertThat(EcmaScript.ownKeys(null)).isEmpty();
        assertThatThrownBy(() -> EcmaScript.keys((Object) null))
          .hasMessage("Cannot convert undefined or null to object");
    }
}
