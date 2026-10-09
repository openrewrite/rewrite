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
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.npm.ScenarioReplay.ScenarioRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.openrewrite.javascript.internal.lock.npm.ScenarioReplay.missing;

/**
 * Replays every recorded edit {@link NpmScenarios} finds through {@link NpmLockRegenerator} and asserts its
 * {@code expect} line; {@code REWRITE_LOCK_SCENARIO_RESULTS} names a file for one line per edit.
 */
class NpmScenarioTest {

    static final String RESULTS_ENV = "REWRITE_LOCK_SCENARIO_RESULTS";

    private static final List<String> results = Collections.synchronizedList(new ArrayList<>());

    static Stream<Arguments> edits() {
        return ScenarioReplay.edits();
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
        String kind = ScenarioReplay.kind(expect);
        String[] words = expect == null ? new String[0] : expect.split("\\s+");
        ScenarioRegistry http = new ScenarioRegistry(scenario);
        String label = scenario.getLabel() + "/" + edit.getStart().getFileName();

        if (kind.equals("exact") && edit.getResult() == null) {
            record(label, "skipped", "no after and no expect");
            return;
        }
        Result result = new NpmLockRegenerator(ScenarioReplay.inputs(edit), ScenarioReplay.registry(http)).regenerate();
        Failure failure = result.getFailure();
        String thrown = failure == null ? String.valueOf(result.getErrorMessage()) :
          failure.getReason() + ": " + failure.getDetail();

        if (kind.equals("exact")) {
            if (!result.isSuccess()) {
                failed(label, http, "failed", thrown, " expected npm's lock but regeneration failed: " + thrown);
            }
            String expected = expectedLock(edit, expect);
            if (!expected.equals(result.getLockFileContent())) {
                failed(label, http, "mismatch", firstDifference(expected, result.getLockFileContent()), "");
                assertThat(result.getLockFileContent()).as(label + missing(http)).isEqualTo(expected);
            }
            record(label, "exact", missing(http).trim());
            return;
        }
        if (result.isSuccess()) {
            failed(label, http, "unexpected-success", "expected " + expect,
              " expected \"" + expect + "\" but regeneration succeeded");
        }
        boolean ok;
        switch (kind) {
            case "peer-conflict":
                ok = failure.getReason() == Reason.PEER_CONFLICT &&
                     failure.getKind() == Kind.SECOND_INSTALL &&
                     failure.getDetail().contains(words[1]) && failure.getDetail().contains(words[2]);
                break;
            case "npm-rejects":
                // a crash is npm's own code failing (no error code): the port fails there too, not always the same way;
                // a route the stub lacked may be the failure's cause instead, unless npm's own answer was the 404
                ok = ("crash".equals(words[1]) ? failure.getKind() == Kind.PM_FAILS ||
                                                 failure.getKind() == Kind.PM_REJECTS :
                  failure.getKind() == Kind.PM_REJECTS && failure.getDetail().contains("(" + words[1] + ")")) &&
                     (http.missing.isEmpty() || "E404".equals(words[1]));
                break;
            case "cannot-know":
                ok = failure.getKind() == Kind.CANNOT_KNOW;
                break;
            case "malformed":
                ok = failure.getReason() == Reason.MALFORMED_LOCK ||
                     failure.getReason() == Reason.MALFORMED_MANIFEST &&
                     failure.getDetail().contains(ScenarioReplay.malformedKey(expect));
                break;
            case "unstable":
                ok = failure.getKind() == Kind.SECOND_INSTALL;
                break;
            default:
                ok = false;
                break;
        }
        if (!ok) {
            failed(label, http, "wrong-failure", "expected " + expect + " got " + thrown,
              " expected \"" + expect + "\" but got " + thrown);
        }
        record(label, "expected-failure", thrown + missing(http));
    }

    // a replay the stub could not serve is skipped, every other departure from the recording fails
    private static void failed(String label, ScenarioRegistry http, String outcome, String detail, String message) {
        if (http.unservedElsewhereRecording()) {
            record(label, "skipped", "not served offline:" + missing(http));
            assumeTrue(false, label + " cannot be replayed offline" + missing(http));
        }
        record(label, outcome, detail + missing(http));
        if (!message.isEmpty()) {
            fail(label + message + missing(http));
        }
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
}
