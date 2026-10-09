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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A family's scenario test: each recorded edit replayed through its port and held to its expect line, one result line
 * per edit in the file the family's results variable names, which a corpus run tallies.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class LockScenarioTest {

    private final String resultsEnv;
    private final String packageManager;
    private final List<String> results = Collections.synchronizedList(new ArrayList<>());

    /**
     * @param packageManager as the failure messages name it: {@code npm}, {@code Yarn}, ...
     */
    protected LockScenarioTest(String resultsEnv, String packageManager) {
        this.resultsEnv = resultsEnv;
        this.packageManager = packageManager;
    }

    /**
     * Whether the failure is the one an expect line of a kind other than {@code pm-fails} names.
     */
    protected abstract boolean names(Expect expect, Failure failure, RecordedRegistry http);

    @AfterAll
    void writeResults() {
        String file = System.getenv(resultsEnv);
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

    protected void record(String label, String outcome, String detail) {
        results.add(label + "\t" + outcome + "\t" + detail.replace('\n', ' ').replace('\t', ' '));
    }

    /**
     * Skips an edit before its replay: one a re-recording supersedes, or one its expect line says the engine does not
     * reproduce, with the reason.
     */
    protected void skipUnlessReplayed(String label, LockScenario scenario, Expect expect) {
        LockScenario supersededBy = scenario.getSupersededBy();
        if (supersededBy != null) {
            record(label, "skipped", "superseded by " + supersededBy.getName());
            assumeTrue(false, label + " is superseded by " + supersededBy.getName());
        }
        if (Expect.NOT_REPRODUCED.equals(expect.getKind())) {
            record(label, "skipped", "not reproduced: " + expect.getRest());
            assumeTrue(false, label + " is not reproduced: " + expect.getRest());
        }
    }

    /**
     * The verdict on a regeneration: byte for byte the lock the package manager wrote, or the failure the expect line
     * names. A replay the registry could not serve is skipped; every other departure from the recording fails.
     */
    protected void check(String label, Expect expect, @Nullable String expected, Result result, RecordedRegistry http) {
        Failure failure = result.getFailure();
        String thrown = failure == null ? String.valueOf(result.getErrorMessage()) :
                failure.getReason() + ": " + failure.getDetail();
        if (expect.isExact() && expected != null) {
            if (!result.isSuccess()) {
                failed(label, http, "failed", thrown, " expected " + packageManager +
                                                      "'s lock but regeneration failed: " + thrown);
            }
            if (!expected.equals(result.getLockFileContent())) {
                failed(label, http, "mismatch", firstDifference(expected, String.valueOf(result.getLockFileContent())),
                        "");
                assertThat(result.getLockFileContent()).as(label + http.describe()).isEqualTo(expected);
            }
            record(label, "exact", http.describe().trim());
            return;
        }
        if (result.isSuccess()) {
            String written = expected == null ? "" : expected.equals(result.getLockFileContent()) ?
                    " (wrote " + packageManager + "'s after)" :
                    " (" + firstDifference(expected, String.valueOf(result.getLockFileContent())) + ")";
            failed(label, http, "unexpected-success", "expected " + expect + written,
                    " expected \"" + expect + "\" but regeneration succeeded" + written);
        }
        Failure failed = requireNonNull(failure);
        boolean ok = Expect.PM_FAILS.equals(expect.getKind()) ? failed.getKind() == Kind.PM_FAILS :
                names(expect, failed, http);
        if (!ok) {
            failed(label, http, "wrong-failure", "expected " + expect + " got " + thrown,
                    " expected \"" + expect + "\" but got " + thrown);
        }
        record(label, "expected-failure", thrown + http.describe());
    }

    /**
     * Records a departure from the recording and fails with {@code message} when it has one; a replay the stub could
     * not serve is skipped instead.
     */
    protected void failed(String label, RecordedRegistry http, String outcome, String detail, String message) {
        if (http.unservedElsewhereRecording()) {
            record(label, "skipped", "not served offline:" + http.describe());
            assumeTrue(false, label + " cannot be replayed offline" + http.describe());
        }
        record(label, outcome, detail + http.describe());
        if (!message.isEmpty()) {
            fail(label + message + http.describe());
        }
    }

    public static String firstDifference(String expected, String actual) {
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
