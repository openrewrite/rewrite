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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * fill-range 2.2.4's and 7.0.1's own tests and the arguments braces passes ({@code upstream/fill-range-*}), for the
 * calls braces makes: the range's text and the options it passes.
 */
class FillRangeTest {

    private static final Set<String> BRACES_OPTIONS = new HashSet<>(Arrays.asList("expand", "nodupes",
            "keepEscaping"));

    static Stream<Arguments> calls() {
        List<Arguments> out = new ArrayList<>();
        for (FillRange.Release release : FillRange.Release.values()) {
            for (Call call : UpstreamCalls.read(resource(release))) {
                if (notPorted(release, call) == null) {
                    out.add(Arguments.of(release, call));
                }
            }
        }
        return out.stream();
    }

    private static String resource(FillRange.Release release) {
        return "fill-range-" + release.name().substring(1).replace('_', '.') + ".jsonl";
    }

    private static @Nullable String notPorted(FillRange.Release release, Call call) {
        List<@Nullable Object> args = call.getArgs();
        int strings = 0;
        while (strings < args.size() && args.get(strings) instanceof String) {
            strings++;
        }
        if (strings < 2) {
            return "bounds that are not two strings: braces passes its range's text";
        }
        if (strings > 3 || args.size() > strings + 1) {
            return "more arguments than braces passes";
        }
        Object options = args.size() > strings ? args.get(strings) : null;
        if (options instanceof Map && !((Map<?, ?>) options).containsKey("$")) {
            for (Object key : ((Map<?, ?>) options).keySet()) {
                if (release == FillRange.Release.V7_0_1 ? !BRACES_OPTIONS.contains(key) :
                        !"makeRe".equals(key) && !"silent".equals(key)) {
                    return "options." + key + ": braces does not pass it";
                }
            }
            if (Boolean.FALSE.equals(((Map<?, ?>) options).get("silent"))) {
                return "options.silent false: expand-range leaves it unset";
            }
        } else if (options != null) {
            return "a function or a number step: braces passes its range's text";
        }
        return null;
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("calls")
    void answersAsTheReleaseDoes(FillRange.Release release, Call call) {
        List<@Nullable Object> args = call.getArgs();
        String step = args.size() > 2 && args.get(2) instanceof String ? (String) args.get(2) : null;
        Object options = args.get(args.size() - 1);
        boolean makeRe = options instanceof Map && EcmaScript.truthy(((Map<?, ?>) options).get("makeRe"));
        assertThat(FillRange.fill((String) args.get(0), (String) args.get(1), step,
                new FillRange.Options(release, makeRe))).isEqualTo(call.getOut());
    }

    @Test
    void namesEveryUpstreamCallItDoesNotReplay() {
        Map<String, Integer> notPorted = new LinkedHashMap<>();
        for (FillRange.Release release : FillRange.Release.values()) {
            for (Call call : UpstreamCalls.read(resource(release))) {
                String reason = notPorted(release, call);
                if (reason != null) {
                    notPorted.merge(release + " " + reason, 1, Integer::sum);
                }
            }
        }
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("V2_2_4 a function or a number step: braces passes its range's text", 9);
        expected.put("V2_2_4 bounds that are not two strings: braces passes its range's text", 41);
        expected.put("V2_2_4 options.optimize: braces does not pass it", 4);
        expected.put("V2_2_4 options.regexPrefix: braces does not pass it", 9);
        expected.put("V2_2_4 options.silent false: expand-range leaves it unset", 5);
        expected.put("V2_2_4 options.step: braces does not pass it", 4);
        expected.put("V7_0_1 a function or a number step: braces passes its range's text", 13);
        expected.put("V7_0_1 bounds that are not two strings: braces passes its range's text", 104);
        expected.put("V7_0_1 more arguments than braces passes", 1);
        expected.put("V7_0_1 options.step: braces does not pass it", 2);
        expected.put("V7_0_1 options.strictRanges: braces does not pass it", 4);
        expected.put("V7_0_1 options.stringify: braces does not pass it", 1);
        expected.put("V7_0_1 options.toRegex: braces does not pass it", 26);
        expected.put("V7_0_1 options.transform: braces does not pass it", 1);
        assertThat(notPorted).isEqualTo(expected);
    }

    // randomatic's while (length--) never ends for a length that is not a whole number: node hangs or runs out of heap
    @Test
    void aRandomRangeOfNoWholeLengthNeverEnds() {
        for (String[] range : new String[][]{{"x", "Infinity"}, {"a", "2.5"}, {"a", "-1"}}) {
            assertThatThrownBy(() -> FillRange.fill(range[0], range[1], "?",
              new FillRange.Options(FillRange.Release.V2_2_4, false)))
              .isInstanceOf(Rejected.class)
              .satisfies(e -> assertThat(((Rejected) e).getOutcome()).isEqualTo(Rejected.Outcome.CRASH))
              .hasMessageContaining("never ends");
        }
    }
}
