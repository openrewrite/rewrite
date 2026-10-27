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
package org.openrewrite.javascript.internal.lock.pnpm;

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
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.LockScenario;
import org.openrewrite.javascript.internal.lock.LockScenario.Phase;
import org.openrewrite.javascript.internal.lock.LockScenarioTest;
import org.openrewrite.javascript.internal.lock.RecordedRegistry;
import org.openrewrite.javascript.internal.lock.RootRelativePath;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.fail;

/**
 * Replays every edit {@link PnpmScenarios} finds through {@link PnpmLockRegenerator} with the edit phase's inputs and
 * asserts the scenario's {@code expect} line, {@code exact} when it has none and {@code after} is present. Every
 * scenario is replayed as each other pnpm release too, run by it as the release that built the LST: its
 * {@code releases} file names the releases that write its recorded outcome, and each {@code release/<first>/} another
 * outcome with its own {@code releases}; the first and last release of each range are replayed
 * ({@code REWRITE_LOCK_EVERY_RELEASE}, {@code REWRITE_LOCK_RELEASES}).
 */
class PnpmScenarioTest extends LockScenarioTest {

    private static final Pattern NODE_VERSION = Pattern.compile("node (v\\S+?),");
    private static final Pattern PLATFORM = Pattern.compile("platform os=(\\S*) cpu=(\\S*) libc=(\\S*)");
    private static final Pattern RECORDED_WITH = Pattern.compile("^pnpm (\\d+\\.\\d+\\.\\d+),");
    private static final Pattern RECORDED_ERROR = Pattern.compile("(?m)^\\[(?:ERROR|ERR_PNPM_[A-Z0-9_]+)] (.+)$");

    PnpmScenarioTest() {
        super("REWRITE_LOCK_PNPM_SCENARIO_RESULTS", "pnpm");
    }

    /**
     * One edit pnpm recorded as one release ran it: the lock it started from beside the other projects' locks, what
     * that release wrote, and its expect line.
     */
    @Value
    static class Edit {
        PnpmScenarios.Scenario scenario;
        String label;
        String start;
        Map<String, String> otherLocks;

        @Nullable
        String after;

        @Nullable
        String expect;

        /**
         * The release that ran it, as the release that built the LST.
         */
        String release;

        @Override
        public String toString() {
            return label;
        }
    }

    static Stream<Arguments> edits() {
        List<Arguments> out = new ArrayList<>();
        for (PnpmScenarios.Scenario scenario : PnpmScenarios.discover()) {
            if (isEdit(scenario)) {
                out.add(Arguments.of(scenario.getLabel(), scenario));
            }
        }
        return out.stream();
    }

    private static boolean isEdit(PnpmScenarios.Scenario scenario) {
        return scenario.has("before") && (scenario.has("after") || scenario.expect() != null);
    }

    static Stream<Arguments> releaseEdits() {
        List<Arguments> out = new ArrayList<>();
        for (PnpmScenarios.Scenario scenario : PnpmScenarios.discover()) {
            if (!isEdit(scenario)) {
                continue;
            }
            for (LockScenario.Replay replay : scenario.releaseReplays(PnpmReleases.all(), PnpmReleases.PROVEN,
                    recordedRelease(scenario))) {
                for (Edit edit : edits(scenario, replay, scenario.getLabel() + "@" + replay.getRelease())) {
                    out.add(Arguments.of(edit.getLabel(), edit));
                }
            }
        }
        return out.stream();
    }

    /**
     * The release that recorded the scenario, which its own outcome is the control of: 11.2.2 where the recorder's
     * {@code pnpm --version} failed and names none.
     */
    static String recordedRelease(PnpmScenarios.Scenario scenario) {
        String recordedWith = scenario.recordedWith();
        Matcher m = RECORDED_WITH.matcher(recordedWith == null ? "" : recordedWith);
        return m.find() ? m.group(1) : Main.PNPM_VERSION;
    }

    /**
     * The expect line the scenario's own edit is held to: its own, but for a recording a release outside
     * {@link PnpmReleases#PROVEN} made, which the engine cannot know the lock of, whatever that release wrote. The
     * recording is held to its own line again once the engine reproduces that release.
     */
    static @Nullable String expected(PnpmScenarios.Scenario scenario) {
        String release = recordedRelease(scenario);
        return PnpmReleases.isProven(release) ? scenario.expect() : "cannot-know the lock pnpm " + release + " writes";
    }

    // the scenario's own edit as another release, or another outcome: the edit over the lock that release wrote first,
    // and the edit over the scenario's own before (cross) where it starts from another lock or ends otherwise
    private static List<Edit> edits(PnpmScenarios.Scenario scenario, LockScenario.Replay replay, String label) {
        String release = replay.getRelease();
        String before = requireNonNull(scenario.file("before"));
        Map<String, String> otherLocks = scenario.otherLocks(Phase.BEFORE);
        List<Edit> edits = new ArrayList<>();
        Path outcome = replay.getOutcome();
        if (outcome.equals(scenario.getDir())) {
            edits.add(new Edit(scenario, label, before, otherLocks, scenario.file("after"), asRelease(scenario),
                    release));
            return edits;
        }
        String own = LockScenario.read(outcome, "before");
        Edit edit = null;
        if (own != null) {
            edit = new Edit(scenario, label, own, otherLocks(scenario, outcome),
                    LockScenario.read(outcome, "after"), trimmed(LockScenario.read(outcome, "expect")), release);
            edits.add(edit);
        }
        Edit cross = new Edit(scenario, label + "/cross", before, otherLocks, LockScenario.read(outcome, "cross"),
                trimmed(LockScenario.read(outcome, "expect-cross")), release);
        if (edit == null || !(edit.getStart().equals(cross.getStart()) &&
                              edit.getOtherLocks().equals(cross.getOtherLocks()) &&
                              Objects.equals(edit.getAfter(), cross.getAfter()) &&
                              Objects.equals(edit.getExpect(), cross.getExpect()))) {
            edits.add(cross);
        }
        return edits;
    }

    // the other projects' locks the release wrote beside its own before, where the scenario keeps every project's
    private static Map<String, String> otherLocks(PnpmScenarios.Scenario scenario, Path outcome) {
        Map<String, String> out = new TreeMap<>();
        Path locks = outcome.resolve("probes").resolve("before-locks");
        if (!Files.isDirectory(locks)) {
            return scenario.otherLocks(Phase.BEFORE);
        }
        try (Stream<Path> walk = Files.walk(locks)) {
            walk.filter(Files::isRegularFile).forEach(p ->
              out.put(locks.relativize(p).toString().replace('\\', '/'), LockScenario.read(p)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        out.remove(RootRelativePath.join(scenario.lockDir(), LockfileReader.WANTED_LOCKFILE));
        return out;
    }

    private static @Nullable String trimmed(@Nullable String expect) {
        return expect == null ? null : expect.trim();
    }

    /**
     * The scenario's own expect line for a replay of its recorded outcome as a release that built the LST. A
     * cannot-know about the release corepack starts is the host's environment, which the release settles: it answered
     * {@code --version} as pnpm run directly, as the recording ran (corepack would start the release the project
     * names). Such a release is held to what the recording ended with: its lock, or the error its edit's log names.
     */
    static @Nullable String asRelease(PnpmScenarios.Scenario scenario) {
        String expect = scenario.expect();
        if (expect == null || !"cannot-know".equals(Expect.of(expect).getKind()) ||
            !expect.startsWith("cannot-know the pnpm release that installs the project: corepack")) {
            return expect;
        }
        if (scenario.has("after")) {
            return null;
        }
        String log = String.valueOf(scenario.file("pnpm-after.log"));
        Matcher error = RECORDED_ERROR.matcher(log);
        return error.find() ? "pm-rejects " + error.group(1).trim() : expect;
    }

    static PnpmLockRegenerator.Inputs inputs(PnpmScenarios.Scenario scenario, String lock) {
        return inputs(scenario, lock, scenario.otherLocks(Phase.BEFORE), null);
    }

    /**
     * The edit phase's inputs over {@code lock} beside {@code otherLocks}, run by {@code release} as the release that
     * built the LST; without one, as the recording's host recorded none.
     */
    static PnpmLockRegenerator.Inputs inputs(PnpmScenarios.Scenario scenario, String lock,
                                             Map<String, String> otherLocks, @Nullable String release) {
        Map<String, String> files = new LinkedHashMap<>(scenario.tree(Phase.AFTER));
        String workspace = scenario.workspaceManifest(Phase.AFTER);
        if (workspace != null) {
            files.put("pnpm-workspace.yaml", workspace);
        }
        String npmrc = scenario.npmrc(Phase.AFTER);
        if (npmrc != null) {
            files.put(".npmrc", npmrc);
        }
        files.putAll(otherLocks);
        PnpmScenarios.Scenario recording = recording(scenario);
        Map<String, String> manifests = scenario.manifests(Phase.AFTER, lock);
        PnpmLockRegenerator.Inputs.InputsBuilder inputs = PnpmLockRegenerator.Inputs.builder()
          .lock(lock)
          .lockDir(scenario.lockDir())
          .dir(scenario.runDir() != null ? scenario.runDir() : scenario.lockDir())
          .manifests(manifests)
          .files(files)
          .paths(PnpmScenarios.paths(files, manifests, scenario.lockDir()))
          .userNpmrc(recording.userNpmrc())
          .now(recording.phaseTime("after"))
          .host(host(recording.recordedWith()))
          .release(release == null ? null : PnpmRelease.of(release));
        if (recording.recordedWith() == null) {
            // a downloaded fixture records no clock and no publish times
            inputs.env(Collections.singletonMap("pnpm_config_minimum_release_age", "0"));
        } else if (scenario.env() != null) {
            inputs.env(scenario.env());
        }
        return inputs.build();
    }

    private static PnpmScenarios.Scenario recording(PnpmScenarios.Scenario scenario) {
        return scenario.getReRecording() != null ? scenario.getReRecording() : scenario;
    }

    static Host host(@Nullable String recordedWith) {
        if (recordedWith == null) {
            return Host.UNKNOWN;
        }
        Matcher node = NODE_VERSION.matcher(recordedWith);
        Matcher platform = PLATFORM.matcher(recordedWith);
        boolean hasPlatform = platform.find();
        return Host.builder()
          .nodeVersion(node.find() ? node.group(1) : null)
          .os(hasPlatform ? platform.group(1) : "darwin")
          .cpu(hasPlatform ? platform.group(2) : "arm64")
          .libc(hasPlatform && !platform.group(3).isEmpty() ? platform.group(3) : null)
          .build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("edits")
    void regeneratesWhatPnpmWrote(String name, PnpmScenarios.Scenario scenario) {
        Expect expect = Expect.of(expected(scenario));
        skipUnlessReplayed(name, scenario, expect);
        ScenarioRegistry http = new ScenarioRegistry(recording(scenario), "after");
        Result result;
        try {
            result = new PnpmLockRegenerator(inputs(scenario, scenario.file("before")), http).regenerate();
        } catch (ScenarioRegistry.RouteNotRecorded e) {
            record(name, "failed", e.getMessage());
            fail(name + ": " + e.getMessage());
            return;
        }
        check(name, expect, scenario.file("after"), result, http);
    }

    /**
     * Every scenario as each other pnpm release ran it: the releases its {@code releases} files name write the
     * recorded outcome, or the one under {@code release/}.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("releaseEdits")
    void regeneratesWhatEachReleaseWrote(String name, Edit edit) {
        PnpmScenarios.Scenario scenario = edit.getScenario();
        Expect expect = Expect.of(edit.getExpect());
        skipUnlessReplayed(name, scenario, expect);
        ScenarioRegistry http = new ScenarioRegistry(recording(scenario), "after");
        Result result;
        try {
            result = new PnpmLockRegenerator(inputs(scenario, edit.getStart(), edit.getOtherLocks(),
                    edit.getRelease()), http).regenerate();
        } catch (ScenarioRegistry.RouteNotRecorded e) {
            record(name, "failed", e.getMessage());
            fail(name + ": " + e.getMessage());
            return;
        }
        check(name, expect, edit.getAfter(), result, http);
    }

    @Override
    protected boolean names(Expect expect, Failure failure, RecordedRegistry<?> http) {
        String rest = expect.getRest();
        String detail = failure.getDetail();
        String code = rest.isEmpty() ? "" : rest.split("\\s+")[0];
        switch (expect.getKind()) {
            case "cannot-know":
                return failure.getKind() == Kind.CANNOT_KNOW && namesEvery(detail, rest);
            case "malformed":
                return failure.getReason() == Reason.MALFORMED_LOCK;
            case "pm-rejects":
                return failure.getKind() == Kind.PM_REJECTS && detail.contains(rest);
            case "pm-fails":
                return failure.getKind() == Kind.PM_FAILS && detail.contains(rest);
            case "second-install-rejects":
                return failure.getKind() == Kind.SECOND_INSTALL && detail.contains(code) && namesEvery(detail, rest);
            default:
                return false;
        }
    }

    // every package, spec or url the expect line names, before its parenthesis
    private static boolean namesEvery(String detail, String what) {
        int paren = what.indexOf(" (");
        String subject = paren < 0 ? what : what.substring(0, paren);
        for (String token : subject.split("\\s+")) {
            String t = token.replaceAll("^[(]+", "").replaceAll("[),;:]+$", "");
            if ((t.indexOf('@') > 0 || t.contains("://") || t.contains(":")) && !detail.contains(t)) {
                return false;
            }
        }
        return true;
    }
}
