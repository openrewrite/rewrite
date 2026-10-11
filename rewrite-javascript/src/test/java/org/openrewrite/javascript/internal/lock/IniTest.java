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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.UpstreamCalls.Call;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ini 1.3.5's and 6.0.0's own tests and the inputs where their sources differ ({@code upstream/ini-*.jsonl}).
 */
class IniTest {

    static Stream<Arguments> calls() {
        List<Arguments> out = new ArrayList<>();
        for (Ini.Release release : Ini.Release.values()) {
            for (Call call : UpstreamCalls.read("ini-" + version(release) + ".jsonl")) {
                if (ported(release, call)) {
                    out.add(Arguments.of(release, call));
                }
            }
        }
        return out.stream();
    }

    private static String version(Ini.Release release) {
        return release.name().substring(1).replace('_', '.');
    }

    // decode without options, unsafe, and 6.0.0's encode of a flat map of strings (the host's npmrc)
    private static boolean ported(Ini.Release release, Call call) {
        switch (call.getFn()) {
            case "decode":
            case "unsafe":
                return call.getArgs().size() == 1;
            case "encode":
                return release == Ini.Release.V6_0_0 && call.getArgs().size() == 1 && call.arg(0) instanceof Map &&
                       ((Map<?, ?>) call.arg(0)).values().stream().allMatch(v -> v instanceof String);
            default:
                return false;
        }
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("calls")
    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    void answersAsTheReleaseDoes(Ini.Release release, Call call) {
        switch (call.getFn()) {
            case "decode":
                String text = (String) call.arg(0);
                if (call.getThrew() != null) {
                    assertThatThrownBy(() -> Ini.decode(text, release))
                      .hasMessage((String) call.getThrew().get("message"));
                } else {
                    assertThat(UpstreamCalls.shape(UpstreamCalls.normalize(Ini.decode(text, release),
                      v -> v instanceof Ini.Section))).isEqualTo(UpstreamCalls.shape(call.getOut()));
                }
                break;
            case "unsafe":
                assertThat(Ini.unsafe((String) call.arg(0))).isEqualTo(call.getOut());
                break;
            default:
                Map<String, @Nullable String> properties = new LinkedHashMap<>();
                ((Map<String, Object>) call.arg(0)).forEach((k, v) -> properties.put(k, (String) v));
                assertThat(Ini.encode(properties)).isEqualTo(call.getOut());
        }
    }

    @Test
    void namesEveryUpstreamCallItDoesNotReplay() {
        Map<String, Integer> notPorted = new LinkedHashMap<>();
        for (Ini.Release release : Ini.Release.values()) {
            for (Call call : UpstreamCalls.read("ini-" + version(release) + ".jsonl")) {
                if (!ported(release, call)) {
                    notPorted.merge(version(release) + " " + call.getFn() + (call.getArgs().size() > 1 ?
                      " with options" : ""), 1, Integer::sum);
                }
            }
        }
        Map<String, Integer> expected = new LinkedHashMap<>();
        // yarn never writes an ini file
        expected.put("1.3.5 encode", 7);
        expected.put("1.3.5 encode with options", 3);
        // bracketedArray: false, which neither npm nor read-ini-file passes
        expected.put("6.0.0 decode with options", 1);
        // sections, arrays and non-string values: the host's npmrc is a flat map of strings
        expected.put("6.0.0 encode", 8);
        expected.put("6.0.0 encode with options", 9);
        assertThat(notPorted).isEqualTo(expected);
    }
}
