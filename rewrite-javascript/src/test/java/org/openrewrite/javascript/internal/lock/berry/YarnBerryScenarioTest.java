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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.Expect;
import org.openrewrite.javascript.internal.lock.LockScenario.Phase;
import org.openrewrite.javascript.internal.lock.LockScenarioTest;
import org.openrewrite.javascript.internal.lock.RecordedRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Replays every recorded edit {@link BerryScenarios} finds through {@link YarnBerryLockRegenerator} and asserts its
 * {@code expect} line; {@code REWRITE_LOCK_YARN_BERRY_SCENARIO_RESULTS} names a file for one line per edit.
 */
class YarnBerryScenarioTest extends LockScenarioTest {

    YarnBerryScenarioTest() {
        super("REWRITE_LOCK_YARN_BERRY_SCENARIO_RESULTS", "Yarn");
    }

    /**
     * One install Yarn recorded: the lock it started from, the phase whose files it read, and what it wrote.
     */
    @Value
    static class Edit {
        BerryScenarios.Scenario scenario;
        String label;
        String start;
        Phase phase;

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
                Edit edit = new Edit(scenario, scenario.getLabel() + "/lock-seed", seed, Phase.BEFORE,
                        before, "exact");
                out.add(Arguments.of(edit.getLabel(), edit));
            }
            String after = scenario.file("after");
            if (before != null && (after != null || scenario.expect() != null)) {
                Edit edit = new Edit(scenario, scenario.getLabel(), before, Phase.AFTER, after,
                        scenario.expect());
                out.add(Arguments.of(edit.getLabel(), edit));
            }
        }
        return out.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("edits")
    void regeneratesWhatYarnWrote(String name, Edit edit) {
        BerryScenarios.Scenario scenario = edit.getScenario();
        Expect expect = Expect.of(edit.getExpect());
        skipUnlessReplayed(edit.getLabel(), scenario, expect);
        BerryScenarios.ScenarioRegistry http = scenario.registry();
        Result result = new YarnBerryLockRegenerator(scenario.inputs(edit.getPhase(), edit.getStart()), http)
                .regenerate();
        check(edit.getLabel(), expect, edit.getAfter(), result, http);
    }

    @Override
    protected boolean names(Expect expect, Failure failure, RecordedRegistry<?> http) {
        String rest = expect.getRest();
        String detail = failure.getDetail();
        switch (expect.getKind()) {
            case "cannot-know":
                return failure.getKind() == Kind.CANNOT_KNOW && namesEvery(detail, rest);
            case "malformed":
                return failure.getReason() == Reason.MALFORMED_LOCK || failure.getReason() == Reason.MALFORMED_MANIFEST;
            case "pm-rejects":
                // a package the registry does not have is the one route that is not recorded
                return failure.getKind() == Kind.PM_REJECTS && detail.contains(rest) &&
                       (http.missing.isEmpty() || failure.getReason() == Reason.PACKAGE_NOT_FOUND);
            case "pm-fails":
                return failure.getKind() == Kind.PM_FAILS && detail.contains(rest) && http.missing.isEmpty();
            case "second-install-rejects":
                return failure.getKind() == Kind.SECOND_INSTALL;
            default:
                return false;
        }
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
}
