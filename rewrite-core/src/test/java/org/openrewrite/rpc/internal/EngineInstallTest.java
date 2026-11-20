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
package org.openrewrite.rpc.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EngineInstallTest {

    private static final String VERSION = "1.2.3";

    @TempDir
    Path tempDir;

    @Test
    void installsOnceAndMarksTheInstallComplete() throws Exception {
        Path target = tempDir.resolve("engines").resolve(VERSION);
        AtomicInteger installs = new AtomicInteger();

        Path installed = EngineInstall.ensureInstalled(target, VERSION, directory -> {
            installs.incrementAndGet();
            Files.write(directory.resolve("engine"), "ok".getBytes(StandardCharsets.UTF_8));
        });
        Path again = EngineInstall.ensureInstalled(target, VERSION, directory -> installs.incrementAndGet());

        assertThat(installed).isEqualTo(target.toAbsolutePath().normalize()).isEqualTo(again);
        assertThat(installs).hasValue(1);
        assertThat(installed.resolve("engine")).hasContent("ok");
        assertThat(installed.resolve(EngineInstall.MARKER)).hasContent(VERSION);
        assertThat(EngineInstall.isInstalled(installed, VERSION)).isTrue();
        assertThat(EngineInstall.isInstalled(installed, "1.2.4")).isFalse();
    }

    @Test
    void aDirectoryWithoutTheMarkerIsInstalledAgainRatherThanUsed() throws Exception {
        // What a crashed, killed or raced install leaves: some of the files, and no marker.
        Path target = Files.createDirectories(tempDir.resolve("engines").resolve(VERSION));
        Files.write(target.resolve("half-written"), new byte[0]);

        Path installed = EngineInstall.ensureInstalled(target, VERSION,
                directory -> Files.write(directory.resolve("engine"), new byte[0]));

        assertThat(installed.resolve("engine")).exists();
        assertThat(installed.resolve("half-written")).doesNotExist();
        assertThat(EngineInstall.isInstalled(installed, VERSION)).isTrue();
    }

    @Test
    void aMarkerOfAnotherVersionIsNotThisVersionInstalled() throws Exception {
        Path target = Files.createDirectories(tempDir.resolve("engines").resolve(VERSION));
        Files.write(target.resolve(EngineInstall.MARKER), "1.2.2".getBytes(StandardCharsets.UTF_8));
        AtomicInteger installs = new AtomicInteger();

        EngineInstall.ensureInstalled(target, VERSION, directory -> installs.incrementAndGet());

        assertThat(installs).hasValue(1);
        assertThat(target.resolve(EngineInstall.MARKER)).hasContent(VERSION);
    }

    @Test
    void aFailedInstallLeavesNothingBehindAndTheNextInstallsAgain() throws Exception {
        Path engines = tempDir.resolve("engines");
        Path target = engines.resolve(VERSION);

        assertThatThrownBy(() -> EngineInstall.ensureInstalled(target, VERSION, directory -> {
            Files.write(directory.resolve("part"), new byte[0]);
            throw new IllegalStateException("pip exited with code 1");
        })).isInstanceOf(IllegalStateException.class).hasMessage("pip exited with code 1");

        assertThat(target).doesNotExist();
        assertThat(entries(engines)).containsExactly(VERSION + ".lock");

        EngineInstall.ensureInstalled(target, VERSION, directory -> Files.write(directory.resolve("engine"), new byte[0]));
        assertThat(EngineInstall.isInstalled(target, VERSION)).isTrue();
    }

    @Test
    void whatAnInstallThatNeverFinishedLeftBesideItsTargetIsRemoved() throws Exception {
        Path engines = Files.createDirectories(tempDir.resolve("engines"));
        Files.createDirectories(engines.resolve("." + VERSION + ".installing-123/rewrite"));
        Files.createDirectories(engines.resolve("." + VERSION + "1.installing-456"));

        EngineInstall.ensureInstalled(engines.resolve(VERSION), VERSION, directory -> {
        });

        assertThat(entries(engines)).containsExactlyInAnyOrder(VERSION, VERSION + ".lock",
                "." + VERSION + "1.installing-456");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void theInstallIsAsReadableAsAnyDirectoryMadeBesideIt() throws Exception {
        // An install made as one user, as when an image is built, may be read as another.
        Path engines = Files.createDirectories(tempDir.resolve("engines"));

        Path installed = EngineInstall.ensureInstalled(engines.resolve(VERSION), VERSION, directory -> {
        });

        assertThat(Files.getPosixFilePermissions(installed))
                .isEqualTo(Files.getPosixFilePermissions(Files.createDirectory(engines.resolve("plain"))));
    }

    @Test
    void threadsInstallingAtOnceInstallOnceAndAllUseIt() throws Exception {
        Path target = tempDir.resolve("engines").resolve(VERSION);
        int threads = 8;
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger installing = new AtomicInteger();
        AtomicInteger installs = new AtomicInteger();
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        List<Path> installed = new CopyOnWriteArrayList<>();
        List<Thread> started = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread thread = new Thread(() -> {
                try {
                    go.await();
                    installed.add(EngineInstall.ensureInstalled(target, VERSION, directory -> {
                        if (installing.incrementAndGet() != 1) {
                            throw new IllegalStateException("two installs at once");
                        }
                        installs.incrementAndGet();
                        Thread.sleep(200);
                        Files.write(directory.resolve("engine"), new byte[0]);
                        installing.decrementAndGet();
                    }));
                } catch (Throwable t) {
                    failures.add(t);
                }
            });
            thread.start();
            started.add(thread);
        }
        go.countDown();
        for (Thread thread : started) {
            thread.join(SECONDS.toMillis(30));
        }

        assertThat(failures).isEmpty();
        assertThat(installs).hasValue(1);
        assertThat(installed).hasSize(threads).containsOnly(target.toAbsolutePath().normalize());
    }

    private static List<String> entries(Path directory) throws IOException {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.map(entry -> entry.getFileName().toString()).collect(Collectors.toList());
        }
    }
}
