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
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Replays every recorded edit {@link BunScenarios} finds through {@link BunLockRegenerator} and asserts its
 * {@code expect} line; {@code REWRITE_LOCK_BUN_SCENARIO_RESULTS} names a file for one line per edit.
 */
class BunScenarioTest extends LockScenarioTest {

    BunScenarioTest() {
        super("REWRITE_LOCK_BUN_SCENARIO_RESULTS", "bun");
    }

    static Stream<Arguments> edits() {
        List<Arguments> out = new ArrayList<>();
        for (BunScenarios.Scenario scenario : BunScenarios.discover()) {
            if (scenario.isEdit()) {
                out.add(Arguments.of(scenario.getLabel(), scenario));
            }
        }
        return out.stream();
    }

    /**
     * The registry as the scenario recorded it: {@code http/<name>}, or {@code http/<name>@full}, whatever its url; a
     * downloaded fixture's empty versions are filled from its {@code http/<name>-<version>} documents.
     */
    static final class ScenarioRegistry extends RecordedRegistry<BunScenarios.Scenario> implements ManifestSource {
        ScenarioRegistry(BunScenarios.Scenario scenario) {
            super(scenario);
        }

        BunScenarios.Scenario recording() {
            return scenario;
        }

        @Override
        public @Nullable String packument(String registry, String name, boolean extended) {
            String file = extended ? name + "@full" : name;
            if (!scenario.asked(registry)) {
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
        String before = scenario.file("before");
        Map<String, String> patchesAbove = new TreeMap<>();
        scenario.filesAbove(Phase.AFTER).forEach((path, text) -> {
            if (path.endsWith(".patch")) {
                patchesAbove.put(path, text);
            }
        });
        return scenario.inputs(Phase.AFTER, before)
          .files(scenario.tree(Phase.AFTER))
          .files(patchesAbove)
          .paths(scenario.paths(Phase.AFTER, before))
          .host(scenario.host())
          .build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("edits")
    void regeneratesWhatBunWrote(String name, BunScenarios.Scenario scenario) {
        Expect expect = Expect.of(scenario.expect());
        skipUnlessReplayed(scenario.getLabel(), scenario, expect);
        ScenarioRegistry http = new ScenarioRegistry(scenario);
        Result result = new BunLockRegenerator(inputs(scenario), http).regenerate();
        check(scenario.getLabel(), expect, scenario.file("after"), result, http);
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
        String after = http.recording().file("after");
        String again = http.recording().file("again");
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
