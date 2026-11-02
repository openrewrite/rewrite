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

/**
 * is-glob 2.0.1's and 4.0.3's own tests and the inputs where their sources differ ({@code upstream/is-glob-*}).
 */
class IsGlobTest {

    static Stream<Arguments> calls() {
        List<Arguments> out = new ArrayList<>();
        for (IsGlob.Release release : IsGlob.Release.values()) {
            for (Call call : UpstreamCalls.read(resource(release))) {
                if (notPorted(call) == null) {
                    out.add(Arguments.of(release, call));
                }
            }
        }
        return out.stream();
    }

    private static String resource(IsGlob.Release release) {
        return "is-glob-" + release.name().substring(1).replace('_', '.') + ".jsonl";
    }

    static @Nullable String notPorted(Call call) {
        if (!(call.arg(0) instanceof String)) {
            return "a value that is not a string: every caller passes one";
        }
        return call.has(1) ? "options.strict false: glob-parent 5.1.2 asks the strict check" : null;
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("calls")
    void answersAsTheReleaseDoes(IsGlob.Release release, Call call) {
        assertThat(IsGlob.isGlob((String) call.arg(0), release)).isEqualTo(call.getOut());
    }

    @Test
    void namesEveryUpstreamCallItDoesNotReplay() {
        Map<String, Integer> notPorted = new LinkedHashMap<>();
        for (IsGlob.Release release : IsGlob.Release.values()) {
            for (Call call : UpstreamCalls.read(resource(release))) {
                String reason = notPorted(call);
                if (reason != null) {
                    notPorted.merge(release + " " + reason, 1, Integer::sum);
                }
            }
        }
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("V2_0_1 a value that is not a string: every caller passes one", 4);
        expected.put("V4_0_3 a value that is not a string: every caller passes one", 4);
        expected.put("V4_0_3 options.strict false: glob-parent 5.1.2 asks the strict check", 22);
        assertThat(notPorted).isEqualTo(expected);
    }
}
