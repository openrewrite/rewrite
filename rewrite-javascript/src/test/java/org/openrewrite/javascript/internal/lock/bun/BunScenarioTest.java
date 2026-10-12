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
package org.openrewrite.javascript.internal.lock.bun;

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
import org.openrewrite.javascript.internal.lock.LockScenario;
import org.openrewrite.javascript.internal.lock.LockScenario.Phase;
import org.openrewrite.javascript.internal.lock.LockScenarioTest;
import org.openrewrite.javascript.internal.lock.RecordedRegistry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Replays every recorded edit {@link BunScenarios} finds through {@link BunLockRegenerator} and asserts its
 * {@code expect} line; {@code REWRITE_LOCK_BUN_SCENARIO_RESULTS} names a file for one line per edit. Every scenario is
 * replayed as each other Bun release too: its {@code releases} file names the releases that write its recorded
 * outcome, and each {@code release/<first>/} another outcome with its own {@code releases}; the first and last release
 * of each range are replayed ({@code REWRITE_LOCK_EVERY_RELEASE}, {@code REWRITE_LOCK_RELEASES}).
 */
class BunScenarioTest extends LockScenarioTest {

    BunScenarioTest() {
        super("REWRITE_LOCK_BUN_SCENARIO_RESULTS", "bun");
    }

    /**
     * One edit bun recorded: the lock it started from, the release it ran as, what it wrote, and what a second install
     * over that rewrote it to.
     */
    @Value
    static class Edit {
        BunScenarios.Scenario scenario;
        String label;
        String start;

        @Nullable
        String after;

        @Nullable
        String expect;

        /**
         * The lock the second install over {@link #after} wrote, where it changed it.
         */
        @Nullable
        String again;

        /**
         * The release it is replayed as; null for the one that recorded the scenario.
         */
        @Nullable
        String release;

        /**
         * What that release asked the registry for in the phases replayed, where its outcome keeps it
         * ({@code release/<first>/requests.log}); null for the scenario's own outcome.
         */
        @Nullable
        String requests;

        @Override
        public String toString() {
            return label;
        }
    }

    static Stream<Arguments> edits() {
        List<Arguments> out = new ArrayList<>();
        for (BunScenarios.Scenario scenario : BunScenarios.discover()) {
            if (scenario.isEdit()) {
                Edit edit = recorded(scenario, null, scenario.getLabel());
                out.add(Arguments.of(edit.getLabel(), edit));
            }
        }
        return out.stream();
    }

    static Stream<Arguments> releaseEdits() {
        List<Arguments> out = new ArrayList<>();
        for (BunScenarios.Scenario scenario : BunScenarios.discover()) {
            if (!scenario.isEdit()) {
                continue;
            }
            for (LockScenario.Replay replay : scenario.releases()) {
                String label = scenario.getLabel() + "@" + replay.getRelease();
                List<Edit> edits = new ArrayList<>();
                if (replay.getOutcome().equals(scenario.getDir())) {
                    edits.add(recorded(scenario, replay.getRelease(), label));
                } else {
                    edits.addAll(outcome(scenario, replay.getRelease(), replay.getOutcome(), label));
                }
                for (Edit edit : edits) {
                    out.add(Arguments.of(edit.getLabel(), edit));
                }
            }
        }
        return out.stream();
    }

    // the scenario's own edit over its before
    private static Edit recorded(BunScenarios.Scenario scenario, @Nullable String release, String label) {
        return new Edit(scenario, label, Objects.requireNonNull(scenario.file("before")), scenario.file("after"),
                scenario.expect(), scenario.file("again"), release, null);
    }

    // another release's outcome: the edit over the lock that release wrote first, and the edit over the scenario's own
    // before (cross) where it starts from another lock or ends otherwise
    private static List<Edit> outcome(BunScenarios.Scenario scenario, String release, Path outcome, String label) {
        List<Edit> edits = new ArrayList<>();
        String before = BunScenarios.Scenario.read(outcome, "before");
        String requests = BunScenarios.Scenario.read(outcome, "requests.log");
        Edit edit = null;
        if (before != null) {
            edit = new Edit(scenario, label, before, BunScenarios.Scenario.read(outcome, "after"),
                    trimmed(BunScenarios.Scenario.read(outcome, "expect")),
                    BunScenarios.Scenario.read(outcome, "after-again"), release, requests);
            edits.add(edit);
        }
        Edit cross = new Edit(scenario, label + "/cross", Objects.requireNonNull(scenario.file("before")),
                BunScenarios.Scenario.read(outcome, "cross"),
                trimmed(BunScenarios.Scenario.read(outcome, "expect-cross")),
                BunScenarios.Scenario.read(outcome, "cross-again"), release, requests);
        if (edit == null || !(edit.getStart().equals(cross.getStart()) &&
                              Objects.equals(edit.getAfter(), cross.getAfter()) &&
                              Objects.equals(edit.getExpect(), cross.getExpect()))) {
            edits.add(cross);
        }
        return edits;
    }

    private static @Nullable String trimmed(@Nullable String expect) {
        return expect == null ? null : expect.trim();
    }

    /**
     * The registry as the scenario recorded it: {@code http/<name>}, or {@code http/<name>@full}, whatever its url; a
     * downloaded fixture's empty versions are filled from its {@code http/<name>-<version>} documents. It holds what
     * the edit replayed over it wrote and the second install's rewrite of that, which a second install's failure is
     * held to, and answers only for the hosts bun asked ({@link BunScenarios.Scenario#asked}): those of the release
     * replayed where its outcome keeps its requests.
     */
    static final class ScenarioRegistry extends RecordedRegistry<BunScenarios.Scenario> implements ManifestSource {
        private final @Nullable String after;
        private final @Nullable String again;
        private final @Nullable String requests;

        ScenarioRegistry(BunScenarios.Scenario scenario) {
            this(scenario, scenario.file("after"), scenario.file("again"), null);
        }

        ScenarioRegistry(BunScenarios.Scenario scenario, @Nullable String after, @Nullable String again,
                         @Nullable String requests) {
            super(scenario);
            this.after = after;
            this.again = again;
            this.requests = requests;
        }

        @Override
        public @Nullable String packument(String registry, String name, boolean extended) {
            String file = extended ? name + "@full" : name;
            if (!scenario.asked(registry, requests)) {
                notRecorded(BunScenarios.Scenario.origin(registry) + "/" + file);
                return null;
            }
            String mirror = mirror(registry, "npmjs");
            String body = mirror == null ? null : scenario.file(mirror + "/" + file);
            if (body == null && mirror != null) {
                body = scenario.file(mirror + "/" + file.replace("/", "%2f"));
            }
            if (body == null) {
                body = scenario.http(file);
            }
            if (body == null) {
                notRecorded(file);
                return null;
            }
            return withRecordedVersions(name, body);
        }
    }

    static BunLockRegenerator.Inputs inputs(BunScenarios.Scenario scenario) {
        return inputs(scenario, Objects.requireNonNull(scenario.file("before")), null);
    }

    static BunLockRegenerator.Inputs inputs(BunScenarios.Scenario scenario, String start, @Nullable String release) {
        Map<String, String> patchesAbove = new TreeMap<>();
        scenario.filesAbove(Phase.AFTER).forEach((path, text) -> {
            if (path.endsWith(".patch")) {
                patchesAbove.put(path, text);
            }
        });
        return scenario.inputs(Phase.AFTER, start)
          .files(scenario.tree(Phase.AFTER))
          .files(patchesAbove)
          .paths(scenario.paths(Phase.AFTER, start))
          .host(scenario.host())
          .release(release == null ? BunRelease.PORTED : BunRelease.of(release))
          .build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("edits")
    void regeneratesWhatBunWrote(String name, Edit edit) {
        replay(edit);
    }

    /**
     * Every scenario as each other Bun release installs it: the releases its {@code releases} files name write the
     * recorded outcome, or the one under {@code release/}.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("releaseEdits")
    void regeneratesWhatEachReleaseWrote(String name, Edit edit) {
        replay(edit);
    }

    private void replay(Edit edit) {
        BunScenarios.Scenario scenario = edit.getScenario();
        Expect expect = Expect.of(edit.getExpect());
        skipUnlessReplayed(edit.getLabel(), scenario, expect);
        ScenarioRegistry http = new ScenarioRegistry(scenario, edit.getAfter(), edit.getAgain(), edit.getRequests());
        Result result = new BunLockRegenerator(inputs(scenario, edit.getStart(), edit.getRelease()), http)
          .regenerate();
        check(edit.getLabel(), expect, edit.getAfter(), result, http);
    }

    @Override
    protected boolean names(Expect expect, Failure failure, RecordedRegistry<?> http) {
        String rest = expect.getRest();
        switch (expect.getKind()) {
            case "exact":
                return failure.getKind() == Kind.PM_REJECTS && failure.getDetail().startsWith("bun deletes the lock");
            case "cannot-know":
                return failure.getKind() == Kind.CANNOT_KNOW && failure.getDetail().contains(rest);
            case "malformed":
                return failure.getReason() == Reason.MALFORMED_LOCK;
            case "pm-rejects":
            case "pm-fails":
                return failure.getKind() == (expect.getKind().equals("pm-rejects") ? Kind.PM_REJECTS : Kind.PM_FAILS) &&
                       failure.getDetail().contains(rest.replaceFirst("^error: ", "")) && http.missing.isEmpty();
            case "second-install-rejects":
                return failure.getKind() == Kind.SECOND_INSTALL && rewritesAsBunDid(failure, (ScenarioRegistry) http);
            case "unstable":
                return failure.getKind() == Kind.SECOND_INSTALL;
            default:
                return false;
        }
    }

    // with bun's after and its own rewrite of it recorded, the lock written is bun's and the second install over it
    // rewrites it as bun did
    private static boolean rewritesAsBunDid(Failure failure, ScenarioRegistry http) {
        String after = http.after;
        String again = http.again;
        if (after == null || again == null) {
            return true;
        }
        if (!after.equals(failure.getWritten())) {
            return false;
        }
        String[] a = after.split("\n", -1);
        String[] b = again.split("\n", -1);
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            String x = i < a.length ? a[i] : "";
            String y = i < b.length ? b[i] : "";
            if (!x.equals(y)) {
                return failure.getDetail().endsWith(" would rewrite it: line " + (i + 1) + " \"" + x.trim() +
                                                    "\" becomes \"" + y.trim() + "\"");
            }
        }
        return false;
    }
}
