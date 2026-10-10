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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.UpstreamCalls.Call;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code Date.parse} in Node v24.19.0 over the ISO form, NaN where V8's ISO parser refuses; {@code toISOString} and
 * {@code Date.UTC} as Node answers them ({@code generate/node-builtins.js}).
 */
class V8DateTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "1970-01-01|0",
      "1970-02-29|5097600000",
      "0301-02-30|-52663392000000",
      "1970-04-31|10368000000",
      "0865-06-06T24:00+00:00|-34856784000000",
      "1970-01-01T24:00:00Z|86400000",
      "2020-01-01T00:00+19:00|1577768400000",
      "2020-01-01T00:00+23:59|1577750460000",
      "2020-01-01T00:00+24:00|NaN",
      "1970-13-01|NaN",
      "1970-01-32|NaN",
      "2020-01-01T25:00Z|NaN",
      "+275760-09-13T00:00:00.001Z|NaN",
      "2026-10-08T12:34:56.789Z|1791462896789"
    })
    void parsesAsNode(String text, double expected) {
        assertThat(V8Date.parse(text)).isEqualTo(Double.valueOf(expected));
    }

    static Stream<Call> calls() {
        return UpstreamCalls.read(EcmaScriptTest.BUILTINS).stream()
          .filter(call -> "toISOString".equals(call.getFn()) || "utc".equals(call.getFn()) ||
                          "timeClip".equals(call.getFn()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("calls")
    void answersAsNodeDoes(Call call) {
        Map<String, @Nullable Object> threw = call.getThrew();
        if (threw != null) {
            assertThatThrownBy(() -> V8Date.toISOString(number(call, 0))).isInstanceOf(JsError.class)
              .hasToString(threw.get("name") + ": " + threw.get("message"));
        } else if ("toISOString".equals(call.getFn())) {
            assertThat(V8Date.toISOString(number(call, 0))).isEqualTo(call.getOut());
        } else if ("timeClip".equals(call.getFn())) {
            assertThat(V8Date.timeClip(number(call, 0))).isEqualTo(call.getOut());
        } else {
            assertThat(V8Date.utc(number(call, 0), number(call, 1), number(call, 2), number(call, 3), number(call, 4),
              number(call, 5), number(call, 6))).isEqualTo(call.getOut());
        }
    }

    private static double number(Call call, int i) {
        return (Double) Objects.requireNonNull(call.arg(i));
    }
}
