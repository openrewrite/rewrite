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
 * braces 1.8.5's and 3.0.2's own tests and the inputs where their sources differ ({@code upstream/braces-*}), for the
 * options each release's caller can pass; 3.0.2 ignores {@code optimize} and {@code minimatch}.
 */
class BracesTest {

    private static final Set<String> OPTIONS_1_8_5 = new HashSet<>(Arrays.asList("makeRe", "nodupes"));
    private static final Set<String> OPTIONS_3_0_2 = new HashSet<>(Arrays.asList("expand", "nodupes", "keepEscaping",
            "optimize", "minimatch"));

    static Stream<Arguments> calls() {
        List<Arguments> out = new ArrayList<>();
        for (Braces.Release release : Braces.Release.values()) {
            for (Call call : UpstreamCalls.read(resource(release))) {
                if (notPorted(release, call) == null) {
                    out.add(Arguments.of(release, call));
                }
            }
        }
        return out.stream();
    }

    private static String resource(Braces.Release release) {
        return release == Braces.Release.V1_8_5 ? "braces-1.8.5.jsonl" : "braces-3.0.2.jsonl.gz";
    }

    private static @Nullable String notPorted(Braces.Release release, Call call) {
        boolean v3 = release == Braces.Release.V3_0_2;
        if (!"braces".equals(call.getFn()) && !(v3 && "expand".equals(call.getFn()))) {
            return "braces." + call.getFn() + ": micromatch asks braces() for an expansion";
        }
        Object options = call.arg(1);
        if (options == EcmaScript.UNDEFINED) {
            return v3 && "braces".equals(call.getFn()) ? "braces() without expand: micromatch passes expand" : null;
        }
        if (!(options instanceof Map) || ((Map<?, ?>) options).containsKey("$")) {
            return "a function for options: micromatch passes an object";
        }
        for (Object key : ((Map<?, ?>) options).keySet()) {
            if (!(v3 ? OPTIONS_3_0_2 : OPTIONS_1_8_5).contains(key)) {
                return "options." + key + ": micromatch does not pass it";
            }
        }
        if (v3 && "braces".equals(call.getFn()) && !Boolean.TRUE.equals(((Map<?, ?>) options).get("expand"))) {
            return "braces() without expand: micromatch passes expand";
        }
        return null;
    }

    private static List<String> answer(Braces.Release release, Call call) {
        Map<?, ?> options = call.arg(1) instanceof Map ? (Map<?, ?>) call.arg(1) : new LinkedHashMap<>();
        Object nodupes = options.containsKey("nodupes") ? options.get("nodupes") : null;
        Braces.Options opts = Braces.Options.builder(release)
                .nodupes(nodupes == null ? null : EcmaScript.truthy(nodupes))
                .makeRe(EcmaScript.truthy(options.get("makeRe")))
                .keepEscaping(EcmaScript.truthy(options.get("keepEscaping")))
                .build();
        String input = (String) call.arg(0);
        return "expand".equals(call.getFn()) ? Braces.expand(input, opts) : new Braces().braces(input, opts);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("calls")
    void answersAsTheReleaseDoes(Braces.Release release, Call call) {
        Map<String, @Nullable Object> threw = call.getThrew();
        if (threw != null) {
            assertThatThrownBy(() -> answer(release, call))
              .hasToString(threw.get("name") + ": " + threw.get("message"));
        } else {
            assertThat(answer(release, call)).isEqualTo(call.getOut());
        }
    }

    @Test
    void namesEveryUpstreamCallItDoesNotReplay() {
        Map<String, Integer> notPorted = new LinkedHashMap<>();
        for (Braces.Release release : Braces.Release.values()) {
            for (Call call : UpstreamCalls.read(resource(release))) {
                String reason = notPorted(release, call);
                if (reason != null) {
                    notPorted.merge(release + " " + reason, 1, Integer::sum);
                }
            }
        }
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("V1_8_5 a function for options: micromatch passes an object", 3);
        expected.put("V1_8_5 options.bash: micromatch does not pass it", 17);
        expected.put("V1_8_5 options.strict: micromatch does not pass it", 1);
        expected.put("V3_0_2 braces.compile: micromatch asks braces() for an expansion", 214);
        expected.put("V3_0_2 options.keepQuotes: micromatch does not pass it", 8);
        assertThat(notPorted).isEqualTo(expected);
    }
}
