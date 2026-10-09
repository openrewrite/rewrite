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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.arr;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.json;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;

/**
 * What Node v24.19.0 answers for the same expressions over {@code JSON.parse} of the same text.
 */
class JsTest {

    @Test
    void getFromObject() {
        Map<String, Object> object = json("{\"a\":1,\"b\":null,\"toString\":\"own\"}");
        assertThat(Js.get(object, "a")).isEqualTo(1);
        assertThat(Js.get(object, "b")).isNull();
        assertThat(Js.get(object, "c")).isSameAs(Js.UNDEFINED);
        assertThat(Js.get(object, "toString")).isEqualTo("own");
        assertThat(Js.get(object, "constructor")).isSameAs(Js.INHERITED);
        assertThat(Js.get(object, "__proto__")).isSameAs(Js.INHERITED);
        assertThat(Js.get(object, "length")).isSameAs(Js.UNDEFINED);
    }

    @Test
    void getFromArray() {
        assertThat(Js.get(arr("x", "y"), "1")).isEqualTo("y");
        assertThat(Js.get(arr("x", "y"), "2")).isSameAs(Js.UNDEFINED);
        assertThat(Js.get(arr("x", "y"), "01")).isSameAs(Js.UNDEFINED);
        assertThat(Js.get(arr("x", "y"), "length")).isEqualTo(2);
        assertThat(Js.get(arr("x", "y"), "map")).isSameAs(Js.INHERITED);
        assertThat(Js.get(arr("x", "y"), "hasOwnProperty")).isSameAs(Js.INHERITED);
        assertThat(Js.get(arr("x", "y"), "trim")).isSameAs(Js.UNDEFINED);
    }

    @Test
    void getFromPrimitive() {
        assertThat(Js.get("xy", "1")).isEqualTo("y");
        assertThat(Js.get("xy", "2")).isSameAs(Js.UNDEFINED);
        assertThat(Js.get("xy", "length")).isEqualTo(2);
        assertThat(Js.get("xy", "trim")).isSameAs(Js.INHERITED);
        assertThat(Js.get("xy", "map")).isSameAs(Js.UNDEFINED);
        assertThat(Js.get(5, "toFixed")).isSameAs(Js.INHERITED);
        assertThat(Js.get(5, "0")).isSameAs(Js.UNDEFINED);
        assertThat(Js.get(true, "valueOf")).isSameAs(Js.INHERITED);
        assertThat(Js.get(true, "x")).isSameAs(Js.UNDEFINED);
        assertThat(Js.get(Js.INHERITED, "optional")).isSameAs(Js.UNDEFINED);
    }

    @Test
    void getFromNullThrows() {
        assertThatThrownBy(() -> Js.get(null, "."))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Cannot read properties of null (reading '.')");
        assertThatThrownBy(() -> Js.get(Js.UNDEFINED, "x"))
          .hasMessage("Cannot read properties of undefined (reading 'x')");
    }

    @Test
    void keysOfObjectPutIntegersFirst() {
        assertThat(Js.keys(json("{\"b\":1,\"4294967295\":1,\"4294967294\":1,\"1\":1,\"01\":1,\"-1\":1,\"1.5\":1," +
                                "\"0\":1,\"1e3\":1,\"10\":1,\"9007199254740993\":1}")))
          .containsExactly("0", "1", "10", "4294967294", "b", "4294967295", "01", "-1", "1.5", "1e3",
          "9007199254740993");
    }

    @Test
    void entriesOfOtherValues() {
        assertThat(Js.entries(arr("x", null))).extracting(Map.Entry::getKey).containsExactly("0", "1");
        assertThat(Js.entries(arr("x", null))).extracting(Map.Entry::getValue).containsExactly("x", null);
        assertThat(Js.keys("ab")).containsExactly("0", "1");
        assertThat(Js.entries("a\ud83d\ude00")).extracting(Map.Entry::getValue)
                .containsExactly("a", "\ud83d", "\ude00");
        assertThat(Js.keys(5)).isEmpty();
        assertThat(Js.keys(true)).isEmpty();
        assertThat(Js.keys(obj())).isEmpty();
        assertThatThrownBy(() -> Js.keys(null)).hasMessage("Cannot convert undefined or null to object");
    }

    @Test
    void strictEquals() {
        assertThat(Js.strictEquals("a", "a")).isTrue();
        assertThat(Js.strictEquals(5, 5.0)).isTrue();
        assertThat(Js.strictEquals(5, "5")).isFalse();
        assertThat(Js.strictEquals(null, null)).isTrue();
        assertThat(Js.strictEquals(null, Js.UNDEFINED)).isFalse();
        assertThat(Js.strictEquals(true, true)).isTrue();
        assertThat(Js.strictEquals(obj(), obj())).isFalse();
        assertThat(Js.strictEquals(arr(), arr())).isFalse();
        Object same = obj();
        assertThat(Js.strictEquals(same, same)).isTrue();
    }

    @Test
    void truthy() {
        assertThat(Js.truthy(Js.UNDEFINED)).isFalse();
        assertThat(Js.truthy(Js.INHERITED)).isTrue();
        assertThat(Js.truthy(arr())).isTrue();
        assertThat(Js.truthy(obj())).isTrue();
        assertThat(Js.truthy("")).isFalse();
        assertThat(Js.truthy(0)).isFalse();
    }
}
