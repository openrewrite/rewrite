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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.Expect;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.LockScenario;
import org.openrewrite.javascript.internal.lock.LockScenario.Phase;
import org.openrewrite.javascript.internal.lock.LockScenario.Replay;
import org.openrewrite.javascript.internal.lock.LockScenarioTest;
import org.openrewrite.javascript.internal.lock.RecordedRegistry;
import org.openrewrite.javascript.internal.lock.RegistrySource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Replays every recorded edit {@link YarnClassicScenarios} finds through {@link YarnClassicLockRegenerator} and asserts
 * its {@code expect} line; {@code REWRITE_LOCK_YARN_CLASSIC_SCENARIO_RESULTS} names a file for one line per edit.
 * Every scenario is replayed as each other Yarn 1 release too: its {@code releases} file names the releases that write
 * its recorded outcome, and each {@code release/<first>/} another outcome with its own {@code releases}; the first and
 * last release of each range are replayed ({@code REWRITE_LOCK_EVERY_RELEASE}, {@code REWRITE_LOCK_RELEASES}).
 */
class YarnClassicScenarioTest extends LockScenarioTest {

    private static final Pattern SYNTAX_ERROR_HEAD = Pattern.compile("^SyntaxError: (\\.\\./)*package\\.json:");

    YarnClassicScenarioTest() {
        super("REWRITE_LOCK_YARN_CLASSIC_SCENARIO_RESULTS", "yarn");
    }

    /**
     * One install of the edit: the lock it started from, what it wrote, its expect line, and the release it ran as,
     * which the host's {@code NodeRuntime} marker names; null for the recording, which the engine replays without one.
     */
    @Value
    static class Edit {
        YarnClassicScenarios.Scenario scenario;
        String label;
        String start;

        @Nullable
        String after;

        @Nullable
        String expect;

        @Nullable
        YarnClassicRelease release;

        @Override
        public String toString() {
            return label;
        }
    }

    static Stream<Arguments> edits() {
        List<Arguments> out = new ArrayList<>();
        for (YarnClassicScenarios.Scenario scenario : YarnClassicScenarios.discover()) {
            if (scenario.isEdit()) {
                out.add(Arguments.of(scenario.getLabel(), scenario));
            }
        }
        return out.stream();
    }

    static Stream<Arguments> releaseEdits() {
        List<Arguments> out = new ArrayList<>();
        for (YarnClassicScenarios.Scenario scenario : YarnClassicScenarios.discover()) {
            if (!scenario.isEdit()) {
                continue;
            }
            for (Replay replay : scenario.releases()) {
                String label = scenario.getLabel() + "@" + replay.getRelease();
                YarnClassicRelease release = YarnClassicRelease.of(replay.getRelease());
                for (Edit edit : replay.getOutcome().equals(scenario.getDir()) ?
                        singletonList(new Edit(scenario, label, requireNonNull(scenario.file("before")),
                                scenario.file("after"), asRelease(scenario.expect()), release)) :
                        outcome(scenario, release, replay.getOutcome(), label)) {
                    out.add(Arguments.of(edit.getLabel(), edit));
                }
            }
        }
        return out.stream();
    }

    // what the engine cannot know of the recording, replayed without the release that built the LST, it may know as
    // that release (a yarn-path hands the install to it), and what the recording's release discards as malformed
    // another may read (a Berry lock before 1.19.0): the lock byte for byte where it writes one
    private static @Nullable String asRelease(@Nullable String expect) {
        String kind = Expect.of(expect).getKind();
        return expect != null && ("cannot-know".equals(kind) || "malformed".equals(kind)) ?
                Expect.EXACT_OR + " " + expect : expect;
    }

    // another release's outcome: the edit over the lock that release wrote first (over the given one where the
    // recording was given its start), and the edit over the scenario's own before (cross)
    private static List<Edit> outcome(YarnClassicScenarios.Scenario scenario, YarnClassicRelease release,
                                      Path outcome, String label) {
        List<Edit> edits = new ArrayList<>();
        String start = scenario.isGiven() ? scenario.file("before") : LockScenario.read(outcome, "before");
        String after = LockScenario.read(outcome, "after");
        String expect = expect(outcome, "expect");
        if (start != null && (after != null || expect != null)) {
            edits.add(new Edit(scenario, label, start, after, expect, release));
        }
        String cross = LockScenario.read(outcome, "cross");
        String expectCross = expect(outcome, "expect-cross");
        if (!scenario.isGiven() && (cross != null || expectCross != null)) {
            edits.add(new Edit(scenario, label + "/cross", requireNonNull(scenario.file("before")), cross,
                    expectCross, release));
        }
        return edits;
    }

    private static @Nullable String expect(Path outcome, String file) {
        String expect = LockScenario.read(outcome, file);
        return expect == null ? null : expect.trim();
    }

    /**
     * The registry as the scenario recorded it ({@code http/<name>}, {@code http/<name>-<version>.tgz}, a 404 where its
     * {@code registry-config} said); a downloaded fixture's empty versions come from its {@code <name>-<version>}.
     */
    static final class ScenarioRegistry extends RecordedRegistry<YarnClassicScenarios.Scenario>
            implements RegistrySource {
        ScenarioRegistry(YarnClassicScenarios.Scenario scenario) {
            super(scenario);
        }

        @Override
        public @Nullable String packument(String registry, String name, boolean full, @Nullable String authorization) {
            String mirror = mirror(registry, "npmjs", "yarnpkg");
            String body = mirror == null ? scenario.http(name) : scenario.file(mirror + "/" + name);
            if (body == null) {
                notRecorded(name);
                return null;
            }
            return withRecordedVersions(name, body);
        }

        @Override
        public byte @Nullable [] tarball(String url, String name, String version, @Nullable String authorization) {
            if (scenario.notFound(url)) {
                return null;
            }
            String file = name + "-" + version + ".tgz";
            String mirror = mirror(url, "npmjs", "yarnpkg");
            byte[] tgz = mirror == null ? scenario.tarball(file) : scenario.bytes(mirror + "/" + file);
            if (tgz == null) {
                notRecorded(file + " " + url);
            }
            return tgz;
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("edits")
    void regeneratesWhatYarnWrote(String name, YarnClassicScenarios.Scenario scenario) {
        replay(new Edit(scenario, scenario.getLabel(), requireNonNull(scenario.file("before")), scenario.file("after"),
                scenario.expect(), null));
    }

    /**
     * Every scenario as each other Yarn 1 release installs it: the releases its {@code releases} files name write the
     * recorded outcome, or the one under {@code release/}.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("releaseEdits")
    void regeneratesWhatEachReleaseWrote(String name, Edit edit) {
        replay(edit);
    }

    private void replay(Edit edit) {
        YarnClassicScenarios.Scenario scenario = edit.getScenario();
        Expect expect = Expect.of(edit.getExpect());
        skipUnlessReplayed(edit.getLabel(), scenario, expect);
        ScenarioRegistry http = new ScenarioRegistry(scenario);
        Result result = new YarnClassicLockRegenerator(scenario.inputs(Phase.AFTER, edit.getStart()).toBuilder()
                .release(edit.getRelease()).build(), http).regenerate();
        check(edit.getLabel(), expect, edit.getAfter(), result, http);
    }

    /**
     * yarn-enable-lockfile-versions writes the Node version into the lock's header, which the engine cannot know from
     * a host that does not say, unless a lockfile flag keeps yarn from writing the lock at all.
     */
    @ParameterizedTest
    @CsvSource({
            "small-lockfile-versions-header, the Node version yarn-enable-lockfile-versions writes",
            "open-opt-pure-lockfile-lockfile-versions,"
    })
    void readsTheNodeVersionOnlyWhereYarnWritesTheLock(String name, @Nullable String cannotKnow) {
        YarnClassicScenarios.Scenario scenario = YarnClassicScenarios.discover().stream()
                .filter(s -> s.getName().equals(name)).findFirst().orElseThrow(IllegalStateException::new);
        Host withoutNode = scenario.host().toBuilder().nodeVersion(null).build();
        YarnClassicLockRegenerator.Inputs inputs = scenario.inputs(Phase.AFTER,
                scenario.file("before")).toBuilder().host(withoutNode).build();
        Result result = new YarnClassicLockRegenerator(inputs, new ScenarioRegistry(scenario)).regenerate();
        if (cannotKnow == null) {
            assertThat(result.getLockFileContent()).isEqualTo(scenario.file("after"));
        } else {
            assertThat(requireNonNull(result.getFailure()).getDetail()).isEqualTo("the engine cannot know " +
                                                                                  cannotKnow);
        }
    }

    @Override
    protected boolean names(Expect expect, Failure failure, RecordedRegistry<?> http) {
        String rest = expect.getRest();
        String detail = failure.getDetail();
        switch (expect.getKind()) {
            case "cannot-know":
                return failure.getKind() == Kind.CANNOT_KNOW && namesEvery(detail, rest) && http.missing.isEmpty();
            case "malformed":
                return failure.getReason() == Reason.MALFORMED_LOCK || failure.getReason() == Reason.MALFORMED_MANIFEST;
            case "pm-rejects":
                // a package the registry does not have is the one route that is not recorded, whatever words a
                // release reports it in
                return failure.getKind() == Kind.PM_REJECTS && saysWhatYarnSaid(detail, rest) &&
                       (http.missing.isEmpty() || failure.getReason() == Reason.PACKAGE_NOT_FOUND &&
                        http.missing.stream().allMatch(detail::contains));
            case "pm-fails":
                return failure.getKind() == Kind.PM_FAILS && saysWhatYarnSaid(detail, rest) && http.missing.isEmpty();
            case "second-install-rejects":
            case "unstable":
                return failure.getKind() == Kind.SECOND_INSTALL;
            default:
                return false;
        }
    }

    // the head of yarn's error line; V8's JSON.parse wording is not ported, only the error and the file it names
    private static boolean saysWhatYarnSaid(String detail, String message) {
        String head = message.endsWith(".") ? message.substring(0, message.length() - 1) : message;
        Matcher syntaxError = SYNTAX_ERROR_HEAD.matcher(head);
        if (syntaxError.find()) {
            return detail.contains(syntaxError.group());
        }
        return detail.contains(head);
    }

    // every package pattern, spec or url the expect line names
    private static boolean namesEvery(String detail, String what) {
        for (String token : what.split("\\s+")) {
            String t = token.replaceAll("^[(]+", "").replaceAll("[),;:]+$", "");
            if ((t.indexOf('@') > 0 || t.contains("://") || t.matches("^[a-z]+:[^/].*")) && !detail.contains(t)) {
                return false;
            }
        }
        return true;
    }
}
