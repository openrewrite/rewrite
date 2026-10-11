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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.UpstreamCalls.Call;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.openrewrite.javascript.internal.lock.EcmaScript.HOLE;
import static org.openrewrite.javascript.internal.lock.EcmaScript.UNDEFINED;

/**
 * What Node v24.19.0 answers for the same expressions, the built-ins' rows from {@code generate/node-builtins.js}.
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

    static Stream<Call> builtins() {
        return UpstreamCalls.read(BUILTINS).stream().filter(EcmaScriptTest::knows);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builtins")
    void answersAsNodeDoes(Call call) {
        Map<String, @Nullable Object> threw = call.getThrew();
        if (threw != null) {
            assertThatThrownBy(() -> answer(call)).isInstanceOf(JsError.class)
              .hasToString(threw.get("name") + ": " + threw.get("message"));
            return;
        }
        assertThat(answer(call)).isEqualTo(call.getOut());
    }

    @Test
    void everyBuiltinRowIsReplayedSomewhere() {
        Map<String, Integer> cannotKnow = new LinkedHashMap<>();
        List<String> elsewhere = Arrays.asList("encodeURIComponent", "decodeURIComponent", "decodeURI", "replace",
          "match", "toISOString", "utc", "timeClip");
        for (Call call : UpstreamCalls.read(BUILTINS)) {
            if (elsewhere.contains(call.getFn())) {
                continue;
            }
            assertThat(call.getFn()).isIn("parseInt", "slice", "repeat", "split", "toInt32", "isInteger", "toNumber",
              "numberToString", "trim", "trimStart", "entries", "jsonStringify");
            if (!knows(call)) {
                cannotKnow.merge(call.getFn(), 1, Integer::sum);
            }
        }
        // a radix of 36 over more than 53 bits, which an x64 build of V8 rounds otherwise than this arm64 one did
        assertThat(cannotKnow).containsExactly(entry("parseInt", 4));
    }

    private static boolean knows(Call call) {
        try {
            return answer(call) != NOT_HERE;
        } catch (CannotKnow e) {
            return false;
        } catch (JsError e) {
            return true;
        }
    }

    static final String BUILTINS = "node-24.19.0-builtins.jsonl";

    private static final Object NOT_HERE = new Object();

    private static @Nullable Object answer(Call call) {
        switch (call.getFn()) {
            case "parseInt":
                return EcmaScript.parseInt(text(call, 0), call.has(1) ? (int) number(call, 1) : 0);
            case "slice":
                return call.has(2) ? EcmaScript.slice(text(call, 0), (int) number(call, 1), (int) number(call, 2)) :
                  EcmaScript.slice(text(call, 0), (int) number(call, 1));
            case "repeat":
                return EcmaScript.repeat(text(call, 0), (int) number(call, 1));
            case "split":
                return Arrays.asList(EcmaScript.splitString(text(call, 0), text(call, 1)));
            case "toInt32":
                return (double) EcmaScript.toInt32(number(call, 0));
            case "isInteger":
                return EcmaScript.isInteger(number(call, 0));
            case "toNumber":
                return EcmaScript.toNumber(text(call, 0));
            case "numberToString":
                return EcmaScript.numberToString(number(call, 0));
            case "trim":
                return EcmaScript.trim(text(call, 0));
            case "trimStart":
                return EcmaScript.trimStart(text(call, 0));
            case "entries":
                return entries(call);
            case "jsonStringify": {
                String json = EcmaScript.jsonStringify(stringified(call), REPLACERS.get(text(call, 3)), call.arg(4));
                return json == null ? UNDEFINED : json;
            }
            default:
                return NOT_HERE;
        }
    }

    private static final Map<String, EcmaScript.@Nullable JsonReplacer> REPLACERS = new LinkedHashMap<>();

    static {
        REPLACERS.put("none", null);
        REPLACERS.put("dropB", (k, v) -> "b".equals(k) ? UNDEFINED : v);
        REPLACERS.put("wrapRoot", (k, v) -> k.isEmpty() ? object(singletonList(Arrays.asList("wrapped", v))) : v);
        REPLACERS.put("numbersAsStrings", (k, v) -> v instanceof Number ? EcmaScript.string(v) : v);
        REPLACERS.put("keyed", (k, v) -> v instanceof String ? k + "=" + v : v);
        REPLACERS.put("undefinedAsNull", (k, v) -> v == UNDEFINED ? null : v);
    }

    // the value node-builtins.js builds from the row: as given, from pairs, an array with holes, or a cycle
    @SuppressWarnings("unchecked")
    private static @Nullable Object stringified(Call call) {
        Object value = call.arg(1);
        switch (text(call, 0)) {
            case "object":
                return object((List<?>) Objects.requireNonNull(value));
            case "sparse": {
                List<@Nullable Object> array = new ArrayList<>();
                for (int i = 0; i < (int) number(call, 1); i++) {
                    array.add(HOLE);
                }
                for (Object pair : (List<?>) Objects.requireNonNull(call.arg(2))) {
                    array.set((int) (double) (Double) ((List<?>) pair).get(0), ((List<?>) pair).get(1));
                }
                return array;
            }
            case "cycle": {
                List<?> path = (List<?>) Objects.requireNonNull(value);
                List<Object> chain = new ArrayList<>();
                chain.add(container(path.get(0)));
                for (int i = 0; i < path.size(); i++) {
                    Object next = i + 1 < path.size() ? container(path.get(i + 1)) :
                      chain.get((int) number(call, 2));
                    Object holder = chain.get(i);
                    if (holder instanceof List) {
                        List<Object> array = (List<Object>) holder;
                        int index = (int) (double) (Double) path.get(i);
                        while (array.size() <= index) {
                            array.add(HOLE);
                        }
                        array.set(index, next);
                    } else {
                        ((Map<String, Object>) holder).put((String) path.get(i), next);
                    }
                    chain.add(next);
                }
                return chain.get(0);
            }
            default:
                return value;
        }
    }

    private static Object container(@Nullable Object key) {
        return key instanceof Double ? new ArrayList<>() : new LinkedHashMap<>();
    }

    private static Map<String, @Nullable Object> object(List<?> pairs) {
        Map<String, @Nullable Object> object = new LinkedHashMap<>();
        for (Object pair : pairs) {
            object.put((String) ((List<?>) pair).get(0), ((List<?>) pair).get(1));
        }
        return object;
    }

    private static List<List<@Nullable Object>> entries(Call call) {
        Object value = call.arg(1);
        if ("object".equals(call.arg(0))) {
            value = object((List<?>) Objects.requireNonNull(value));
        }
        List<List<@Nullable Object>> out = new ArrayList<>();
        for (Map.Entry<String, @Nullable Object> entry : EcmaScript.entries(value)) {
            out.add(Arrays.asList(entry.getKey(), entry.getValue()));
        }
        return out;
    }

    private static String text(Call call, int i) {
        return (String) Objects.requireNonNull(call.arg(i));
    }

    private static double number(Call call, int i) {
        return (Double) Objects.requireNonNull(call.arg(i));
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

    @Test
    void entriesLeaveOutAHole() {
        assertThat(EcmaScript.entries(Arrays.asList(HOLE, "x"))).containsExactly(entry("1", "x"));
    }
}
