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
package org.openrewrite.javascript.internal.lock.berry;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;

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

/**
 * Replays every recorded edit {@link BerryScenarios} finds through {@link YarnBerryLockRegenerator} and asserts its
 * {@code expect} line; {@code REWRITE_LOCK_YARN_BERRY_SCENARIO_RESULTS} names a file for one line per edit.
 */
class YarnBerryScenarioTest {

    static final String RESULTS_ENV = "REWRITE_LOCK_YARN_BERRY_SCENARIO_RESULTS";

    private static final List<String> results = Collections.synchronizedList(new ArrayList<>());

    /**
     * One install Yarn recorded: the lock it started from, the phase whose files it read, and what it wrote.
     */
    @Value
    static class Edit {
        BerryScenarios.Scenario scenario;
        String label;
        String start;
        BerryScenarios.Phase phase;

        @Nullable
        String after;

        @Nullable
        String expect;

        @Override
        public String toString() {
            return label;
        }
    }

    static Stream<Arguments> edits() {
        List<Arguments> out = new ArrayList<>();
        for (BerryScenarios.Scenario scenario : BerryScenarios.discover()) {
            String before = scenario.file("before");
            String seed = scenario.file("lock-seed");
            String recorded = scenario.file("recorded-with");
            if (seed != null && before != null && recorded != null && recorded.contains("before exit=0")) {
                Edit edit = new Edit(scenario, scenario.getLabel() + "/lock-seed", seed, BerryScenarios.Phase.BEFORE,
                        before, "exact");
                out.add(Arguments.of(edit.getLabel(), edit));
            }
            String after = scenario.file("after");
            if (before != null && (after != null || scenario.expect() != null)) {
                Edit edit = new Edit(scenario, scenario.getLabel(), before, BerryScenarios.Phase.AFTER, after,
                        scenario.expect());
                out.add(Arguments.of(edit.getLabel(), edit));
            }
        }
        return out.stream();
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

    static String kind(@Nullable String expect) {
        if (expect == null || expect.isEmpty()) {
            return "exact";
        }
        return expect.split("\\s+")[0];
    }

    private static void record(String label, String outcome, String detail) {
        results.add(label + "\t" + outcome + "\t" + detail.replace('\n', ' ').replace('\t', ' '));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("edits")
    void regeneratesWhatYarnWrote(String name, Edit edit) {
        BerryScenarios.Scenario scenario = edit.getScenario();
        String expect = edit.getExpect();
        String kind = kind(expect);
        String after = edit.getAfter();
        BerryScenarios.ScenarioRegistry http = scenario.registry();
        String label = edit.getLabel();

        Result result = new YarnBerryLockRegenerator(scenario.inputs(edit.getPhase(), edit.getStart()), http)
                .regenerate();
        Failure failure = result.getFailure();
        String thrown = failure == null ? String.valueOf(result.getErrorMessage()) :
                failure.getReason() + ": " + failure.getDetail();

        if (kind.equals("exact") && after != null) {
            if (!result.isSuccess()) {
                failed(scenario, label, http, "failed", thrown, " expected Yarn's lock but regeneration failed: " +
                                                                thrown);
            }
            if (!after.equals(result.getLockFileContent())) {
                String actual = String.valueOf(result.getLockFileContent());
                failed(scenario, label, http, "mismatch", firstDifference(after, actual), "");
                assertThat(result.getLockFileContent()).as(label + http.describe()).isEqualTo(after);
            }
            record(label, "exact", http.describe().trim());
            return;
        }
        if (result.isSuccess()) {
            String written = after == null ? "" : after.equals(result.getLockFileContent()) ? " (wrote Yarn's after)" :
                    " (" + firstDifference(after, String.valueOf(result.getLockFileContent())) + ")";
            failed(scenario, label, http, "unexpected-success", "expected " + expect + written,
                    " expected \"" + expect + "\" but regeneration succeeded" + written);
        }
        String rest = expect == null ? "" : expect.substring(kind.length()).trim();
        String detail = failure.getDetail();
        boolean ok;
        switch (kind) {
            case "cannot-know":
                ok = failure.getKind() == Kind.CANNOT_KNOW && namesEvery(detail, rest);
                break;
            case "malformed":
                ok = failure.getReason() == Reason.MALFORMED_LOCK || failure.getReason() == Reason.MALFORMED_MANIFEST;
                break;
            case "pm-rejects":
                // a package the registry does not have is the one route that is not recorded
                ok = failure.getKind() == Kind.PM_REJECTS && detail.contains(rest) &&
                     (http.missing.isEmpty() || failure.getReason() == Reason.PACKAGE_NOT_FOUND);
                break;
            case "pm-fails":
                ok = failure.getKind() == Kind.PM_FAILS && detail.contains(rest) && http.missing.isEmpty();
                break;
            case "second-install-rejects":
                ok = failure.getKind() == Kind.SECOND_INSTALL;
                break;
            default:
                ok = false;
                break;
        }
        if (!ok) {
            failed(scenario, label, http, "wrong-failure", "expected " + expect + " got " + thrown,
                    " expected \"" + expect + "\" but got " + thrown);
        }
        record(label, "expected-failure", thrown + http.describe());
    }

    // every package, spec or url the expect line names
    private static boolean namesEvery(String detail, String what) {
        for (String token : what.split("\\s+")) {
            String t = token.replaceAll("^[(]+", "").replaceAll("[),;:]+$", "");
            if ((t.indexOf('@') > 0 || t.contains("://")) && !detail.contains(t)) {
                return false;
            }
        }
        return true;
    }

    // a replay the stub could not serve is skipped, every other departure from the recording fails
    private static void failed(BerryScenarios.Scenario scenario, String label, BerryScenarios.ScenarioRegistry http,
                               String outcome, String detail, String message) {
        if (!http.missing.isEmpty() && !scenario.has("recorded-with") && scenario.getHttpCache() == null) {
            record(label, "skipped", "not served offline:" + http.describe());
            assumeTrue(false, label + " cannot be replayed offline" + http.describe());
        }
        record(label, outcome, detail + http.describe());
        if (!message.isEmpty()) {
            fail(label + message + http.describe());
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
