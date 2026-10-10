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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * brace-expansion 1.1.11's and 5.0.6's own tests and the inputs where their sources differ, and balanced-match
 * 1.0.0's and 4.0.4's own tests ({@code upstream/brace-expansion-*}, {@code upstream/balanced-match-*}).
 */
class BraceExpansionTest {

    private static final String[][] RELEASES = {
            {"V1_1_11", "brace-expansion-1.1.11.jsonl", "balanced-match-1.0.0.jsonl"},
            {"V5_0_6", "brace-expansion-5.0.6.jsonl.gz", "balanced-match-4.0.4.jsonl"}};

    static Stream<Arguments> calls() {
        List<Arguments> out = new ArrayList<>();
        for (String[] release : RELEASES) {
            for (int file = 1; file < 3; file++) {
                for (Call call : UpstreamCalls.read(release[file])) {
                    if (notPorted(call) == null) {
                        out.add(Arguments.of(BraceExpansion.Release.valueOf(release[0]), call));
                    }
                }
            }
        }
        return out.stream();
    }

    private static @Nullable String notPorted(Call call) {
        if ("expand".equals(call.getFn())) {
            return call.has(1) && !(call.arg(1) instanceof Map && ((Map<?, ?>) call.arg(1)).isEmpty()) ?
                    "expand with a max: minimatch 10.2.5 passes none" : null;
        }
        return "{".equals(call.arg(0)) && "}".equals(call.arg(1)) ? null :
                call.getFn() + " of other delimiters: brace-expansion asks for { and } only";
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("calls")
    void answersAsTheReleaseDoes(BraceExpansion.Release release, Call call) {
        String str = (String) call.arg("expand".equals(call.getFn()) ? 0 : 2);
        switch (call.getFn()) {
            case "expand":
                List<String> expanded = BraceExpansion.expand(str, release);
                Object out = call.getOut();
                if (out instanceof Map && "strings".equals(((Map<?, ?>) out).get("$"))) {
                    assertThat(expanded).hasSize(((Number) ((Map<?, ?>) out).get("length")).intValue());
                    assertThat(sha256(EcmaScript.jsonStringify(expanded))).isEqualTo(((Map<?, ?>) out).get("sha256"));
                } else {
                    assertThat(expanded).isEqualTo(out);
                }
                break;
            case "range":
                int[] range = BraceExpansion.range(str);
                assertThat(range == null ? EcmaScript.UNDEFINED : Arrays.asList((double) range[0], (double) range[1]))
                  .isEqualTo(call.getOut());
                break;
            default:
                BraceExpansion.Balanced balanced = BraceExpansion.balanced(str);
                int[] r = BraceExpansion.range(str);
                Object written = EcmaScript.UNDEFINED;
                if (balanced != null && r != null) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("start", (double) r[0]);
                    m.put("end", (double) r[1]);
                    m.put("pre", balanced.getPre());
                    m.put("body", balanced.getBody());
                    m.put("post", balanced.getPost());
                    written = m;
                }
                assertThat(written).isEqualTo(call.getOut());
        }
    }

    private static String sha256(@Nullable String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(String.valueOf(text)
              .getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void namesEveryUpstreamCallItDoesNotReplay() {
        Map<String, Integer> notPorted = new LinkedHashMap<>();
        for (String[] release : RELEASES) {
            for (int file = 1; file < 3; file++) {
                for (Call call : UpstreamCalls.read(release[file])) {
                    String reason = notPorted(call);
                    if (reason != null) {
                        notPorted.merge(release[file].replaceAll("\\.jsonl.*", "") + " " + reason, 1, Integer::sum);
                    }
                }
            }
        }
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("balanced-match-1.0.0 balanced of other delimiters: brace-expansion asks for { and } only", 7);
        expected.put("balanced-match-4.0.4 balanced of other delimiters: brace-expansion asks for { and } only", 9);
        expected.put("brace-expansion-5.0.6 expand with a max: minimatch 10.2.5 passes none", 2);
        assertThat(notPorted).isEqualTo(expected);
    }

    // Node 24.19.0's answers: a sequence counts in doubles, and 5.0.6 takes a zero step as 1
    @Test
    void sequencesCountInDoubles() {
        for (BraceExpansion.Release release : BraceExpansion.Release.values()) {
            assertThat(BraceExpansion.expand("{18446744073709551616..18446744073709555712..4096}", release))
              .containsExactly("18446744073709552000", "18446744073709556000");
            assertThat(BraceExpansion.expand("{-0..0}", release)).containsExactly("0");
        }
        assertThat(BraceExpansion.expand("{1..3..0}", BraceExpansion.Release.V5_0_6)).containsExactly("1", "2", "3");
        assertThat(BraceExpansion.expand("{18446744073709551616..18446744073709551617}", BraceExpansion.Release.V5_0_6))
          .hasSize(100_000);
    }

    // 1.1.11 never leaves a sequence whose step does not move it: node runs out of heap
    @Test
    void aSequenceThatDoesNotMoveExhausts1_1_11sHeap() {
        for (String pattern : new String[]{"a{1..5..0}b", "{18446744073709551616..18446744073709551617}"}) {
            assertThatThrownBy(() -> BraceExpansion.expand(pattern, BraceExpansion.Release.V1_1_11))
              .isInstanceOf(Rejected.class)
              .satisfies(e -> assertThat(((Rejected) e).getOutcome()).isEqualTo(Rejected.Outcome.CRASH))
              .hasMessageContaining("JavaScript heap out of memory");
        }
    }
}
