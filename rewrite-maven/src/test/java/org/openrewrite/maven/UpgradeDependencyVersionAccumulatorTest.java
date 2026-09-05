/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every source file writes to the one accumulator, and a runtime that visits source files in parallel
 * therefore writes to it from several threads at once. The {@code versionExistence} verdicts are written
 * during the edit phase, where the value is produced by a network call, so the window is as wide as a
 * download.
 * <p>
 * This exercises the writes the recipe actually makes. Backed by plain {@code HashMap} and {@code HashSet}
 * they silently lose entries or throw, and a single-threaded test run — which is how the recipe is exercised
 * everywhere else — cannot show either.
 */
class UpgradeDependencyVersionAccumulatorTest {

    private static final int THREADS = 8;
    private static final int WRITES_PER_THREAD = 500;
    private static final int EXPECTED = THREADS * WRITES_PER_THREAD;

    private static final UpgradeDependencyVersion.PropertyKey SHARED =
            new UpgradeDependencyVersion.PropertyKey(Paths.get("pom.xml"), "shared.version");

    @Test
    void toleratesConcurrentWriters() throws Exception {
        UpgradeDependencyVersion.Accumulator acc = new UpgradeDependencyVersion.Accumulator();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startTogether = new CountDownLatch(1);
        List<Future<?>> writers = new ArrayList<>();
        try {
            for (int t = 0; t < THREADS; t++) {
                int thread = t;
                writers.add(pool.submit(() -> {
                    startTogether.await();
                    for (int i = 0; i < WRITES_PER_THREAD; i++) {
                        GroupArtifact ga = new GroupArtifact("com.example.g" + thread, "a" + i);
                        acc.getProjectArtifacts().add(ga);
                        acc.getPomProperties().add(new UpgradeDependencyVersion.PomProperty(
                                Paths.get(thread + "/" + i + "/pom.xml"), "jackson.version", "2.15.2"));
                        acc.getPropertyConsumers()
                                .computeIfAbsent(SHARED, key -> ConcurrentHashMap.newKeySet())
                                .add(ga);
                        acc.getVersionExistence().putIfAbsent(
                                new GroupArtifactVersion(ga.getGroupId(), ga.getArtifactId(), "2.15.2"), true);
                    }
                    return null;
                }));
            }
            startTogether.countDown();
            for (Future<?> writer : writers) {
                writer.get(60, SECONDS);
            }
        } finally {
            pool.shutdownNow();
            assertThat(pool.awaitTermination(60, SECONDS)).isTrue();
        }

        assertThat(acc.getProjectArtifacts()).hasSize(EXPECTED);
        assertThat(acc.getPomProperties()).hasSize(EXPECTED);
        assertThat(acc.getPropertyConsumers().get(SHARED)).hasSize(EXPECTED);
        assertThat(acc.getVersionExistence()).hasSize(EXPECTED);
    }
}
