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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.npm.Host;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Replays every edit {@link PnpmScenarios} finds through {@link PnpmLockRegenerator} with the edit phase's inputs and
 * asserts the scenario's {@code expect} line, {@code exact} when it has none and {@code after} is present.
 */
class PnpmScenarioTest {

    static final String RESULTS_ENV = "REWRITE_LOCK_PNPM_SCENARIO_RESULTS";

    private static final Pattern NODE_VERSION = Pattern.compile("node (v\\S+?),");
    private static final Pattern PLATFORM = Pattern.compile("platform os=(\\S*) cpu=(\\S*) libc=(\\S*)");

    private static final List<String> results = Collections.synchronizedList(new ArrayList<>());

    static Stream<Arguments> edits() {
        List<Arguments> out = new ArrayList<>();
        for (PnpmScenarios.Scenario scenario : PnpmScenarios.discover()) {
            if (scenario.has("before") && (scenario.has("after") || scenario.expect() != null)) {
                out.add(Arguments.of(scenario.getLabel(), scenario));
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

    static PnpmLockRegenerator.Inputs inputs(PnpmScenarios.Scenario scenario, String lock) {
        Map<String, String> files = new LinkedHashMap<>(scenario.tree(false));
        String workspace = scenario.workspaceManifest(false);
        if (workspace != null) {
            files.put("pnpm-workspace.yaml", workspace);
        }
        String npmrc = scenario.npmrc(false);
        if (npmrc != null) {
            files.put(".npmrc", npmrc);
        }
        PnpmScenarios.Scenario recording = recording(scenario);
        Map<String, String> manifests = scenario.manifests(false, lock);
        PnpmLockRegenerator.Inputs.InputsBuilder inputs = PnpmLockRegenerator.Inputs.builder()
          .lock(lock)
          .lockDir(scenario.lockDir())
          .dir(scenario.runDir() != null ? scenario.runDir() : scenario.lockDir())
          .manifests(manifests)
          .files(files)
          .paths(PnpmScenarios.paths(files, manifests, scenario.lockDir()))
          .now(recording.phaseTime("after"))
          .registry(new ScenarioRegistry(recording, "after"))
          .host(host(recording.recordedWith()));
        if (recording.recordedWith() == null) {
            // a downloaded fixture records no clock and no publish times
            inputs.env(Collections.singletonMap("pnpm_config_minimum_release_age", "0"));
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
    void regeneratesWhatPnpmWrote(String name, PnpmScenarios.Scenario scenario) {
        String expect = scenario.expect();
        String kind = kind(expect);
        String after = scenario.file("after");
        if (scenario.getSupersededBy() != null) {
            record(name, "skipped", "superseded by " + scenario.getSupersededBy().getName());
            assumeTrue(false, name + " is superseded by " + scenario.getSupersededBy().getName());
        }
        Result result;
        try {
            result = new PnpmLockRegenerator(inputs(scenario, scenario.file("before"))).regenerate();
        } catch (ScenarioRegistry.RouteNotRecorded e) {
            record(name, "failed", e.getMessage());
            fail(name + ": " + e.getMessage());
            return;
        }
        Failure failure = result.getFailure();
        String thrown = failure == null ? String.valueOf(result.getErrorMessage()) :
          failure.getReason() + ": " + failure.getDetail();

        if ("exact".equals(kind) && after != null) {
            if (!result.isSuccess()) {
                record(name, "failed", thrown);
                fail(name + " expected pnpm's lock but regeneration failed: " + thrown);
            }
            if (!after.equals(result.getLockFileContent())) {
                record(name, "mismatch", firstDifference(after, String.valueOf(result.getLockFileContent())));
                assertThat(result.getLockFileContent()).as(name).isEqualTo(after);
            }
            record(name, "exact", "");
            return;
        }
        if (result.isSuccess()) {
            String written = after == null ? "" : after.equals(result.getLockFileContent()) ? " (wrote pnpm's after)" :
              " (" + firstDifference(after, String.valueOf(result.getLockFileContent())) + ")";
            record(name, "unexpected-success", "expected " + expect + written);
            fail(name + " expected \"" + expect + "\" but regeneration succeeded" + written);
        }
        String rest = expect == null ? "" : expect.substring(kind.length()).trim();
        String detail = failure.getDetail();
        String code = rest.isEmpty() ? "" : rest.split("\\s+")[0];
        boolean ok;
        switch (kind) {
            case "cannot-know":
                ok = failure.getKind() == Kind.CANNOT_KNOW && namesEvery(detail, rest);
                break;
            case "malformed":
                ok = failure.getReason() == Reason.MALFORMED_LOCK;
                break;
            case "pm-rejects":
                ok = failure.getKind() == Kind.PM_REJECTS && detail.contains(rest);
                break;
            case "second-install-rejects":
                ok = failure.getKind() == Kind.SECOND_INSTALL && detail.contains(code) &&
                     namesEvery(detail, rest);
                break;
            default:
                ok = false;
                break;
        }
        if (!ok) {
            record(name, "wrong-failure", "expected " + expect + " got " + thrown);
            fail(name + " expected \"" + expect + "\" but got " + thrown);
        }
        record(name, "expected-failure", thrown);
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
