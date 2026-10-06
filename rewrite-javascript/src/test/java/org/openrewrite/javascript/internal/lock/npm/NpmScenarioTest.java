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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Replays every recorded edit {@link NpmScenarios} finds through {@link NpmLockRegenerator} against a stub
 * registry served from the scenario's {@code http/}, and asserts what the scenario's {@code expect} line says:
 * {@code exact} (the lock written equals npm's byte for byte; the default when {@code after} is present),
 * {@code peer-conflict}, {@code npm-rejects <code>}, {@code cannot-know}, {@code malformed} or {@code unstable}.
 * When {@code REWRITE_LOCK_SCENARIO_RESULTS} names a file, one line per edit is written there: the outcome and
 * the first difference.
 */
class NpmScenarioTest {

    static final String RESULTS_ENV = "REWRITE_LOCK_SCENARIO_RESULTS";

    private static final List<String> results = Collections.synchronizedList(new ArrayList<>());

    static Stream<Arguments> edits() {
        return IdealTreeBuilderTest.edits();
    }

    @AfterAll
    static void writeResults() {
        String file = System.getenv(RESULTS_ENV);
        if (file == null || file.isEmpty()) {
            return;
        }
        try {
            List<String> sorted = new ArrayList<>(results);
            Collections.sort(sorted);
            Files.write(Paths.get(file), (String.join("\n", sorted) + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static NpmLockRegenerator.Inputs inputs(NpmScenarios.Edit edit) {
        NpmScenarios.Scenario scenario = edit.getScenario();
        String npmrc = scenario.npmrc() == null ? "" : scenario.npmrc() + "\n";
        Instant recordedAt = scenario.recordedAt();
        return NpmLockRegenerator.Inputs.builder()
          .lock(edit.getStart().getText())
          .yarnLock(edit.getInputs().getYarnLock())
          .manifests(edit.manifests())
          .npmrc(npmrc + scenario.argsAfter())
          .host(IdealTreeBuilderTest.RECORDED_ON.toBuilder()
            .nodeVersion(scenario.nodeVersion())
            .now(recordedAt != null ? recordedAt : Instant.now())
            .build())
          .build();
    }

    /**
     * The bytes npm wrote for the edit. A fixture recorded by another npm than the one ported carries npm
     * 11.17.0's rewrite of its lock beside it; that is the expectation then.
     */
    private static String expectedLock(NpmScenarios.Edit edit, @Nullable String expect) {
        NpmScenarios.Lock result = edit.getResult();
        String rewritten = result == null || expect != null ? null : result.rewritten();
        return rewritten != null ? rewritten : result.getText();
    }

    private static void record(String label, String outcome, String detail) {
        results.add(label + "\t" + outcome + "\t" + detail.replace('\n', ' ').replace('\t', ' '));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("edits")
    void regeneratesWhatNpmWrote(String name, NpmScenarios.Edit edit) {
        NpmScenarios.Scenario scenario = edit.getScenario();
        String expect = scenario.expect();
        String kind = IdealTreeBuilderTest.kind(expect);
        String[] words = expect == null ? new String[0] : expect.split("\\s+");
        IdealTreeBuilderTest.ScenarioRegistry http = new IdealTreeBuilderTest.ScenarioRegistry(scenario);
        String label = scenario.getLabel() + "/" + edit.getStart().getFileName();

        if (kind.equals("exact") && edit.getResult() == null) {
            record(label, "skipped", "no after and no expect");
            return;
        }
        Result result = new NpmLockRegenerator(inputs(edit), IdealTreeBuilderTest.registry(http)).regenerate();
        Failure failure = result.getFailure();
        String thrown = failure == null ? String.valueOf(result.getErrorMessage()) :
          failure.getReason() + ": " + failure.getDetail();

        if (kind.equals("exact")) {
            if (!result.isSuccess()) {
                record(label, "failed", thrown + missing(http));
                fail(label + " expected npm's lock but regeneration failed: " + thrown + missing(http));
            }
            String expected = expectedLock(edit, expect);
            if (!expected.equals(result.getLockFileContent())) {
                record(label, "mismatch", firstDifference(expected, result.getLockFileContent()) + missing(http));
                assertThat(result.getLockFileContent()).as(label + missing(http)).isEqualTo(expected);
            }
            record(label, "exact", "");
            return;
        }
        if (result.isSuccess()) {
            record(label, "unexpected-success", "expected " + expect);
            fail(label + " expected \"" + expect + "\" but regeneration succeeded" + missing(http));
        }
        boolean ok;
        switch (kind) {
            case "peer-conflict":
                ok = failure.getReason() == Reason.PEER_CONFLICT &&
                     failure.getDetail().startsWith(NpmLockRegenerator.SECOND_INSTALL) &&
                     failure.getDetail().contains(words[1]) && failure.getDetail().contains(words[2]);
                break;
            case "npm-rejects":
                // a crash is npm's own code failing (no error code): the port fails there too, not always the same way
                ok = "crash".equals(words[1]) ? failure.getDetail().startsWith(NpmLockRegenerator.NPM_FAILS) ||
                                                failure.getDetail().startsWith(NpmLockRegenerator.NPM_REJECTS) :
                  failure.getDetail().startsWith(NpmLockRegenerator.NPM_REJECTS + " (" + words[1] + ")");
                break;
            case "cannot-know":
                ok = failure.getDetail().startsWith(NpmLockRegenerator.CANNOT_KNOW);
                break;
            case "malformed":
                ok = failure.getReason() == Reason.MALFORMED_LOCK ||
                     failure.getReason() == Reason.MALFORMED_MANIFEST && failure.getDetail().contains(malformedKey(expect));
                break;
            case "unstable":
                ok = failure.getDetail().startsWith(NpmLockRegenerator.SECOND_INSTALL);
                break;
            default:
                ok = false;
                break;
        }
        if (!ok) {
            record(label, "wrong-failure", "expected " + expect + " got " + thrown + missing(http));
            fail(label + " expected \"" + expect + "\" but got " + thrown + missing(http));
        }
        record(label, "expected-failure", thrown);
    }

    // "malformed override key <key>" or "malformed override reference <$ref> reached from <name>"
    private static String malformedKey(String expect) {
        String[] words = expect.split("\\s+");
        return expect.startsWith("malformed override reference ") ? words[3] : words[words.length - 1];
    }

    private static String firstDifference(String expected, String actual) {
        String[] e = expected.split("\n", -1);
        String[] a = actual.split("\n", -1);
        for (int i = 0; i < Math.max(e.length, a.length); i++) {
            String x = i < e.length ? e[i] : "";
            String y = i < a.length ? a[i] : "";
            if (!x.equals(y)) {
                return "line " + (i + 1) + " expected \"" + x.trim() + "\" actual \"" + y.trim() + "\"";
            }
        }
        return "differs";
    }

    private static String missing(IdealTreeBuilderTest.ScenarioRegistry http) {
        return http.missing.isEmpty() ? "" : " [routes not recorded: " + String.join(", ", http.missing) + "]";
    }
}
