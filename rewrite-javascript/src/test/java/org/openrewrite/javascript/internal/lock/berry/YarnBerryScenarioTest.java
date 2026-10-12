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
import org.openrewrite.javascript.internal.lock.LockScenario.Replay;
import org.openrewrite.javascript.internal.lock.LockScenarioTest;
import org.openrewrite.javascript.internal.lock.RecordedRegistry;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Replays every recorded edit {@link BerryScenarios} finds through {@link YarnBerryLockRegenerator} and asserts its
 * {@code expect} line, and the {@code .yarnrc.yml} the install writes; {@code REWRITE_LOCK_YARN_BERRY_SCENARIO_RESULTS}
 * names a file for one line per edit. Every scenario is replayed as each other Yarn release too: its {@code releases}
 * file names the releases that write its recorded outcome, and each {@code release/<first>/} another outcome with its
 * own {@code releases}; the first and last release of each range are replayed ({@code REWRITE_LOCK_EVERY_RELEASE},
 * {@code REWRITE_LOCK_RELEASES}).
 */
class YarnBerryScenarioTest extends LockScenarioTest {

    YarnBerryScenarioTest() {
        super("REWRITE_LOCK_YARN_BERRY_SCENARIO_RESULTS", "Yarn");
    }

    /**
     * One install Yarn recorded: the lock it started from, the phase whose files it read, the {@code .yarnrc.yml} it
     * started with, the release it ran as, and what it wrote.
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

        @Nullable
        String yarnrc;

        /**
         * The {@code .yarnrc.yml} the install wrote; null where it wrote none.
         */
        @Nullable
        String written;

        /**
         * Whether the recording kept what Yarn wrote besides the lock, so that {@link #written} says.
         */
        boolean writtenKept;

        /**
         * The release it is replayed as; null for the one the scenario's own files pick.
         */
        @Nullable
        String release;

        Instant now;

        @Override
        public String toString() {
            return label;
        }
    }

    static Stream<Arguments> edits() {
        List<Arguments> out = new ArrayList<>();
        for (BerryScenarios.Scenario scenario : BerryScenarios.discover()) {
            for (Edit edit : recorded(scenario, null, scenario.getLabel())) {
                out.add(Arguments.of(edit.getLabel(), edit));
            }
        }
        return out.stream();
    }

    static Stream<Arguments> releaseEdits() {
        List<Arguments> out = new ArrayList<>();
        for (BerryScenarios.Scenario scenario : BerryScenarios.discover()) {
            for (Replay replay : scenario.releases()) {
                String label = scenario.getLabel() + "@" + replay.getRelease();
                List<Edit> edits = replay.getOutcome().equals(scenario.getDir()) ?
                        recorded(scenario, replay.getRelease(), label) :
                        outcome(scenario, replay.getRelease(), replay.getOutcome(), label);
                for (Edit edit : edits) {
                    out.add(Arguments.of(edit.getLabel(), edit));
                }
            }
        }
        return out.stream();
    }

    // the scenario's own edits: the first phase over its seed, and the edit over its before
    private static List<Edit> recorded(BerryScenarios.Scenario scenario, @Nullable String release, String label) {
        List<Edit> edits = new ArrayList<>();
        String before = scenario.file("before");
        String seed = scenario.file("lock-seed");
        String recorded = scenario.file("recorded-with");
        boolean kept = scenario.keepsWrittenFiles();
        Instant now = scenario.now();
        if (seed != null && before != null && recorded != null && recorded.contains("before exit=0")) {
            edits.add(new Edit(scenario, label + "/lock-seed", seed, Phase.BEFORE, before, "exact",
                    scenario.yarnrcGiven(Phase.BEFORE, null), scenario.file("yarnrc-before.written"), kept, release,
                    now));
        }
        String after = scenario.file("after");
        if (before != null && (after != null || scenario.expect() != null)) {
            edits.add(new Edit(scenario, label, before, Phase.AFTER, after, scenario.expect(),
                    scenario.yarnrcGiven(Phase.AFTER, scenario.file("yarnrc-before.written")),
                    scenario.file("yarnrc.written"), kept, release, now));
        }
        return edits;
    }

    // another release's outcome: the first phase over the scenario's seed, the edit over the lock that release wrote
    // first, and the edit over the scenario's own before (cross)
    private static List<Edit> outcome(BerryScenarios.Scenario scenario, String release, Path outcome, String label) {
        List<Edit> edits = new ArrayList<>();
        Instant now = scenario.now(outcome);
        String seed = scenario.file("lock-seed");
        String before = BerryScenarios.Scenario.read(outcome, "before");
        String beforeWritten = BerryScenarios.Scenario.read(outcome, "yarnrc-before.written");
        if (seed != null && (before != null || BerryScenarios.Scenario.read(outcome, "expect-before") != null)) {
            edits.add(new Edit(scenario, label + "/lock-seed", seed, Phase.BEFORE, before,
                    expect(outcome, "expect-before"), scenario.yarnrcGiven(Phase.BEFORE, null), beforeWritten, true,
                    release, now));
        }
        String start = scenario.has("before-given") ? scenario.file("before") : before;
        String after = BerryScenarios.Scenario.read(outcome, "after");
        if (start != null && (after != null || BerryScenarios.Scenario.read(outcome, "expect") != null)) {
            edits.add(new Edit(scenario, label, start, Phase.AFTER, after, expect(outcome, "expect"),
                    scenario.yarnrcGiven(Phase.AFTER, scenario.has("before-given") ?
                            scenario.file("yarnrc-before.written") : beforeWritten),
                    BerryScenarios.Scenario.read(outcome, "yarnrc.written"), true, release, now));
        }
        String recordedBefore = scenario.file("before");
        String cross = BerryScenarios.Scenario.read(outcome, "cross");
        if (recordedBefore != null && !scenario.has("before-given") &&
            (cross != null || BerryScenarios.Scenario.read(outcome, "expect-cross") != null)) {
            edits.add(new Edit(scenario, label + "/cross", recordedBefore, Phase.AFTER, cross,
                    expect(outcome, "expect-cross"),
                    scenario.yarnrcGiven(Phase.AFTER, scenario.file("yarnrc-before.written")),
                    BerryScenarios.Scenario.read(outcome, "cross.yarnrc.written"), true, release, now));
        }
        return edits;
    }

    private static @Nullable String expect(Path outcome, String file) {
        String expect = BerryScenarios.Scenario.read(outcome, file);
        return expect == null ? null : expect.trim();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("edits")
    void regeneratesWhatYarnWrote(String name, Edit edit) {
        replay(edit);
    }

    /**
     * Every scenario as each other Yarn release installs it: the releases its {@code releases} files name write the
     * recorded outcome, or the one under {@code release/}.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("releaseEdits")
    void regeneratesWhatEachReleaseWrote(String name, Edit edit) {
        replay(edit);
    }

    private void replay(Edit edit) {
        BerryScenarios.Scenario scenario = edit.getScenario();
        Expect expect = Expect.of(edit.getExpect());
        skipUnlessReplayed(edit.getLabel(), scenario, expect);
        BerryScenarios.ScenarioRegistry http = scenario.registry();
        Result result = new YarnBerryLockRegenerator(scenario.installInputs(edit.getPhase(), edit.getStart(),
                edit.getYarnrc(), edit.getRelease(), edit.getNow()), http).regenerate();
        String yarnrc = result.getFiles().get(Configuration.DEFAULT_RC_FILENAME);
        boolean exact = expect.isExact() || Expect.EXACT_OR.equals(expect.getKind()) && result.isSuccess();
        if (exact && edit.isWrittenKept() && edit.getAfter() != null && result.isSuccess() &&
            Objects.equals(edit.getAfter(), result.getLockFileContent()) && !Objects.equals(edit.getWritten(), yarnrc)) {
            failed(edit.getLabel(), http, "mismatch", ".yarnrc.yml " + (edit.getWritten() == null ?
                            "written where Yarn wrote none" : yarnrc == null ? "not written" :
                            firstDifference(edit.getWritten(), yarnrc)),
                    " wrote another .yarnrc.yml than Yarn");
        }
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
