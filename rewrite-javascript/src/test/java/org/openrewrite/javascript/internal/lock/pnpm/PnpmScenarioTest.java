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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.Expect;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.LockScenario.Phase;
import org.openrewrite.javascript.internal.lock.LockScenarioTest;
import org.openrewrite.javascript.internal.lock.RecordedRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.fail;

/**
 * Replays every edit {@link PnpmScenarios} finds through {@link PnpmLockRegenerator} with the edit phase's inputs and
 * asserts the scenario's {@code expect} line, {@code exact} when it has none and {@code after} is present.
 */
class PnpmScenarioTest extends LockScenarioTest {

    private static final Pattern NODE_VERSION = Pattern.compile("node (v\\S+?),");
    private static final Pattern PLATFORM = Pattern.compile("platform os=(\\S*) cpu=(\\S*) libc=(\\S*)");

    PnpmScenarioTest() {
        super("REWRITE_LOCK_PNPM_SCENARIO_RESULTS", "pnpm");
    }

    static Stream<Arguments> edits() {
        List<Arguments> out = new ArrayList<>();
        for (PnpmScenarios.Scenario scenario : PnpmScenarios.discover()) {
            if (scenario.has("before") && (scenario.has("after") || scenario.expect() != null)) {
                out.add(Arguments.of(scenario.getLabel(), scenario));
            }
        }
        return out.stream();
    }

    static PnpmLockRegenerator.Inputs inputs(PnpmScenarios.Scenario scenario, String lock) {
        Map<String, String> files = new LinkedHashMap<>(scenario.tree(Phase.AFTER));
        String workspace = scenario.workspaceManifest(Phase.AFTER);
        if (workspace != null) {
            files.put("pnpm-workspace.yaml", workspace);
        }
        String npmrc = scenario.npmrc(Phase.AFTER);
        if (npmrc != null) {
            files.put(".npmrc", npmrc);
        }
        files.putAll(scenario.otherLocks(Phase.BEFORE));
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
          .host(host(recording.recordedWith()));
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
        Expect expect = Expect.of(scenario.expect());
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
