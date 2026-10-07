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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An install that changes nothing, for every bun.lock {@link BunScenarios} finds, with the manifests and config of
 * the phase that wrote it: the lock loaded into the model, its edges resolved by walking the keys, the tree
 * hoisted again, the root diffed against the manifests, the lockfile cleaned, and the whole file written. The
 * bytes are the lock's own, or what bun itself writes on that install ({@link BunScenarios.Lock#rewritten()});
 * a lock bun ignores fails to load with bun's error.
 */
class BunLockRoundTripTest {

    static Stream<Arguments> locks() {
        List<Arguments> out = new ArrayList<>();
        for (BunScenarios.Scenario scenario : BunScenarios.discover()) {
            for (BunScenarios.Lock lock : scenario.locks()) {
                out.add(Arguments.of(lock.toString(), lock));
            }
        }
        return out.stream();
    }

    static BunLockRegenerator.PendingInstall install(BunScenarios.Lock lock) {
        return BunLockRegenerator.installWithManager(BunLockRegenerator.Inputs.builder()
          .lock(lock.getText())
          .manifests(lock.manifests())
          .npmrc(lock.npmrc())
          .bunfig(lock.bunfig())
          .now(lock.getScenario().now())
          .build(), null);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("locks")
    void writesBackWhatBunWrites(String name, BunScenarios.Lock lock) {
        BunLockRegenerator.PendingInstall install = install(lock);
        String loadError = lock.loadError();
        if (loadError != null) {
            TextLockfileReader.ParseError error = install.getLoadResult().getError();
            assertThat(error).as("bun ignores the lock: " + loadError).isNotNull();
            // bun prints a multi-line log entry under one "error:"; the recording keeps its first line
            String firstLine = error.getMessage().split("\n", 2)[0];
            assertThat(error.error + ": " + firstLine).isEqualTo(loadError);
            return;
        }
        assertThat(install.getLoadResult().getError()).isNull();
        if (lock.resolvesAgain()) {
            assertThat(install.getEnqueued()).as("bun resolves on an install that changes nothing").isNotEmpty();
            return;
        }
        assertThat(install.hasPendingWork()).as("dependencies left to resolve").isFalse();
        assertThat(install.getEnqueued()).as("dependencies enqueued").isEmpty();
        String rewritten = lock.rewritten();
        String expected = lock.deletedAgain() ? null : rewritten != null ? rewritten : lock.getText();
        assertThat(install.save()).isEqualTo(expected);
    }
}
