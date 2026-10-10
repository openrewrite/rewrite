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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An install that changes nothing, for every bun.lock {@link BunScenarios} finds: the lock's own bytes, what bun itself
 * writes on that install ({@link BunScenarios.Lock#rewritten()}), or for a lock bun ignores, bun's error.
 */
class BunLockRoundTripTest {

    private static final ManifestSource NO_REQUEST = (registry, name, extended) -> {
        throw new AssertionError("bun resolves nothing on an install that changes nothing: " + name);
    };

    static Stream<Arguments> locks() {
        List<Arguments> out = new ArrayList<>();
        for (BunScenarios.Scenario scenario : BunScenarios.discover()) {
            for (BunScenarios.Lock lock : scenario.locks()) {
                out.add(Arguments.of(lock.toString(), lock));
            }
        }
        return out.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("locks")
    void writesBackWhatBunWrites(String name, BunScenarios.Lock lock) {
        BunScenarios.Scenario scenario = lock.getScenario();
        BunLockRegenerator.Inputs inputs = lock.inputs()
          .files(patches(scenario.tree(lock.getPhase())))
          .files(patches(scenario.filesAbove(lock.getPhase())))
          .build();
        String loadError = lock.loadError();
        boolean resolves = loadError == null && lock.resolvesAgain();
        Result result = new BunLockRegenerator(inputs, resolves ?
          new BunScenarioTest.ScenarioRegistry(scenario) : NO_REQUEST).regenerate();
        if (loadError != null) {
            assertThat(result.getFailure()).as("bun ignores the lock: " + loadError).isNotNull();
            assertThat(result.getFailure().getKind()).isEqualTo(Kind.PM_REJECTS);
            assertThat(result.getFailure().getDetail()).endsWith(": " + loadError);
        } else if (lock.deletedAgain()) {
            assertThat(result.getFailure()).as("bun deletes the lock").isNotNull();
            assertThat(result.getFailure().getDetail()).startsWith("bun deletes the lock");
        } else {
            String rewritten = lock.rewritten();
            assertThat(result.getErrorMessage()).isNull();
            assertThat(result.getLockFileContent()).isEqualTo(rewritten != null ? rewritten : lock.getText());
        }
    }

    // the lock's patch files, which an install reads; the phase's .env files are the scenario test's
    private static Map<String, String> patches(Map<String, String> tree) {
        Map<String, String> patches = new TreeMap<>();
        for (Map.Entry<String, String> file : tree.entrySet()) {
            if (file.getKey().endsWith(".patch")) {
                patches.put(file.getKey(), file.getValue());
            }
        }
        return patches;
    }
}
