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
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A family's scenario test: each recorded edit regenerated through its port, byte for byte the package manager's lock
 * or the failure its expect line names.
 */
public abstract class LockScenarioTest extends ScenarioTest {

    private final String packageManager;

    /**
     * @param packageManager as the failure messages name it: {@code npm}, {@code Yarn}, ...
     */
    protected LockScenarioTest(String resultsEnv, String packageManager) {
        super(resultsEnv);
        this.packageManager = packageManager;
    }

    /**
     * Whether the failure is the one the expect line names.
     */
    protected abstract boolean names(Expect expect, Failure failure, RecordedRegistry<?> http);

    /**
     * The verdict on a regeneration: byte for byte the lock the package manager wrote, or the failure the expect line
     * names. A replay the registry could not serve is skipped; every other departure from the recording fails.
     */
    protected void check(String label, Expect expect, @Nullable String expected, Result result,
                         RecordedRegistry<?> http) {
        Failure failure = result.getFailure();
        String thrown = failure == null ? String.valueOf(result.getErrorMessage()) :
                failure.getReason() + ": " + failure.getDetail();
        boolean exact = expect.isExact();
        if (Expect.EXACT_OR.equals(expect.getKind())) {
            exact = result.isSuccess();
            expect = Expect.of(expect.getRest());
        }
        if (exact && expected != null) {
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
        if (!names(expect, requireNonNull(failure), http)) {
            failed(label, http, "wrong-failure", "expected " + expect + " got " + thrown,
                    " expected \"" + expect + "\" but got " + thrown);
        }
        if ("unstable".equals(expect.getKind()) && expected != null && failure.getWritten() != null &&
            !expected.equals(failure.getWritten())) {
            // the lock the second install changes is the one the package manager wrote first
            failed(label, http, "mismatch", firstDifference(expected, failure.getWritten()),
                    " wrote another first lock than " + packageManager + " before its second install changes it");
        }
        record(label, "expected-failure", thrown + http.describe());
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
