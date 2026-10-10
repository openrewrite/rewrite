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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.Expect;
import org.openrewrite.javascript.internal.lock.LockScenarioTest;
import org.openrewrite.javascript.internal.lock.RecordedRegistry;
import org.openrewrite.javascript.internal.lock.npm.ScenarioReplay.ScenarioRegistry;

import java.util.stream.Stream;

/**
 * Replays every recorded edit {@link NpmScenarios} finds through {@link NpmLockRegenerator} and asserts its
 * {@code expect} line; {@code REWRITE_LOCK_SCENARIO_RESULTS} names a file for one line per edit.
 */
class NpmScenarioTest extends LockScenarioTest {

    NpmScenarioTest() {
        super("REWRITE_LOCK_SCENARIO_RESULTS", "npm");
    }

    static Stream<Arguments> edits() {
        return ScenarioReplay.edits();
    }

    /**
     * The bytes npm wrote for the edit. A fixture recorded by another npm than the one ported carries npm
     * 11.17.0's rewrite of its lock beside it; that is the expectation then.
     */
    private static @Nullable String expectedLock(NpmScenarios.Edit edit, @Nullable String expect) {
        NpmScenarios.Lock result = edit.getResult();
        if (result == null) {
            return null;
        }
        String rewritten = expect != null ? null : result.rewritten();
        return rewritten != null ? rewritten : result.getText();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("edits")
    void regeneratesWhatNpmWrote(String name, NpmScenarios.Edit edit) {
        NpmScenarios.Scenario scenario = edit.getScenario();
        Expect expect = Expect.of(scenario.expect());
        String label = scenario.getLabel() + "/" + edit.getStart().getFileName();
        skipUnlessReplayed(label, scenario, expect);
        if (expect.isExact() && edit.getResult() == null) {
            record(label, "skipped", "no after and no expect");
            return;
        }
        ScenarioRegistry http = new ScenarioRegistry(scenario);
        Result result = new NpmLockRegenerator(ScenarioReplay.inputs(edit), ScenarioReplay.registry(http)).regenerate();
        String yarnLock = edit.getResult() != null && "after".equals(edit.getResult().getFileName()) &&
                          scenario.has("after-yarn") ? scenario.file("after-yarn") : null;
        if (expect.isExact() && yarnLock != null && result.isSuccess() &&
            !yarnLock.equals(result.getFiles().get("yarn.lock"))) {
            failed(label, http, "mismatch", "yarn.lock " + firstDifference(yarnLock,
                    String.valueOf(result.getFiles().get("yarn.lock"))), " wrote another yarn.lock than npm");
        }
        check(label, expect, expectedLock(edit, scenario.expect()), result, http);
    }

    @Override
    protected boolean names(Expect expect, Failure failure, RecordedRegistry<?> http) {
        String[] words = expect.words();
        switch (expect.getKind()) {
            case "peer-conflict":
                return failure.getReason() == Reason.PEER_CONFLICT && failure.getKind() == Kind.SECOND_INSTALL &&
                       failure.getDetail().contains(words[1]) && failure.getDetail().contains(words[2]);
            case "npm-rejects":
                // a route the stub lacked may be the failure's cause instead, unless npm's own answer was the 404
                return failure.getKind() == Kind.PM_REJECTS && failure.getDetail().contains("(" + words[1] + ")") &&
                       (http.missing.isEmpty() || "E404".equals(words[1]));
            case "pm-fails":
                // an error without a code of npm's own: the port fails there too, not always with npm's words
                return failure.getKind() == Kind.PM_FAILS && http.missing.isEmpty();
            case "cannot-know":
                return failure.getKind() == Kind.CANNOT_KNOW;
            case "malformed":
                return failure.getReason() == Reason.MALFORMED_LOCK && failure.getKind() == Kind.PM_REJECTS ||
                       failure.getReason() == Reason.MALFORMED_MANIFEST && failure.getKind() == Kind.PM_FAILS &&
                       failure.getDetail().contains(ScenarioReplay.malformedKey(String.valueOf(expect.getLine())));
            case "unstable":
                return failure.getKind() == Kind.SECOND_INSTALL;
            default:
                return false;
        }
    }
}
