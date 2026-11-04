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
package org.openrewrite.csharp.rpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.rpc.internal.EngineInstall;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code dotnet tool install} is not safe to run concurrently into one {@code --tool-path}.
 * Parallel Gradle test forks (one JVM per core) each lazily install the same fresh daily
 * snapshot, so the first install of a version races and intermittently fails with
 * "Directory not empty". {@link CSharpRewriteRpc.Builder#installTool} installs it once,
 * holding a {@link java.nio.channels.FileLock} that is honored across processes.
 * <p>
 * This is exercised with real subprocesses because a {@code FileLock} is per-JVM — two
 * lock attempts from one JVM throw {@code OverlappingFileLockException} instead of
 * blocking, so cross-process serialization cannot be reproduced in a single JVM.
 */
class InstallToolPathConcurrencyTest {

    private static final int PROCESSES = 4;
    private static final String VERSION = "1.2.3-snapshot.test";

    @Test
    void concurrentInstallsIntoSameToolPathAreSerialized(@TempDir Path tempDir) throws Exception {
        // given
        Path toolsDir = tempDir.resolve("rewrite-tools");
        Path toolExecutable = toolsDir.resolve(VERSION).resolve(CSharpRewriteRpc.Builder.TOOL_COMMAND);
        Path installs = tempDir.resolve("installs");
        Path barrier = tempDir.resolve("go");

        // when
        List<Process> processes = new ArrayList<>();
        for (int i = 0; i < PROCESSES; i++) {
            processes.add(new ProcessBuilder(
                    Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp", System.getProperty("java.class.path"),
                    Racer.class.getName(),
                    toolsDir.toString(),
                    installs.toString(),
                    barrier.toString())
                    .redirectErrorStream(true)
                    .start());
        }
        // Release all racers at once to maximize overlap on the first (uncached) install.
        Thread.sleep(500);
        Files.createFile(barrier);

        List<Integer> exitCodes = new ArrayList<>();
        for (Process p : processes) {
            assertThat(p.waitFor(60, SECONDS)).as("subprocess timed out").isTrue();
            exitCodes.add(p.exitValue());
        }

        // then
        assertThat(exitCodes)
                .as("every racing install must succeed; %d indicates a detected concurrent install",
                        Racer.EXIT_CONCURRENT)
                .containsOnly(0);
        assertThat(toolExecutable).exists();
        assertThat(EngineInstall.isInstalled(toolExecutable.getParent(), VERSION)).isTrue();
        assertThat(Files.readAllLines(installs)).as("installed once, the others using that install").hasSize(1);
    }

    @Test
    void aToolPathACrashedInstallLeftIsInstalledAgainRatherThanRun(@TempDir Path tempDir) throws Exception {
        // given: the executable is there, but the install never finished
        Path toolsDir = tempDir.resolve("rewrite-tools");
        Path left = Files.createDirectories(toolsDir.resolve(VERSION));
        Files.createFile(left.resolve(CSharpRewriteRpc.Builder.TOOL_COMMAND));
        Files.createDirectories(left.resolve(".store/partial"));

        // when
        Path toolPath = CSharpRewriteRpc.Builder.installTool(toolsDir, VERSION,
                directory -> Files.createFile(directory.resolve(CSharpRewriteRpc.Builder.TOOL_COMMAND)));

        // then
        assertThat(toolPath).isEqualTo(left.toAbsolutePath().normalize());
        assertThat(toolPath.resolve(".store")).doesNotExist();
        assertThat(EngineInstall.isInstalled(toolPath, VERSION)).isTrue();
    }

    @Test
    void anInstallThatLeavesNoToolIsNotTakenForInstalled(@TempDir Path tempDir) {
        Path toolsDir = tempDir.resolve("rewrite-tools");

        assertThatThrownBy(() -> CSharpRewriteRpc.Builder.installTool(toolsDir, VERSION, directory -> {
        })).hasMessageContaining("left no " + CSharpRewriteRpc.Builder.TOOL_COMMAND);
        assertThat(toolsDir.resolve(VERSION)).doesNotExist();
    }

    /**
     * Stands in for one Gradle test fork: it calls the production install path with a fake
     * install that flags any overlap and records that it ran. With the file lock the installs
     * are serialized (and the re-check skips redundant installs); without it the racers overlap
     * and the loser exits {@link #EXIT_CONCURRENT}.
     */
    public static class Racer {
        static final int EXIT_CONCURRENT = 2;

        public static void main(String[] args) throws Exception {
            Path toolsDir = Paths.get(args[0]);
            Path installs = Paths.get(args[1]);
            Path barrier = Paths.get(args[2]);

            long deadline = System.nanoTime() + SECONDS.toNanos(30);
            while (!Files.exists(barrier)) {
                if (System.nanoTime() > deadline) {
                    System.exit(3);
                }
                Thread.sleep(10);
            }

            try {
                CSharpRewriteRpc.Builder.installTool(toolsDir, VERSION, toolPath -> fakeInstall(toolsDir, toolPath, installs));
            } catch (ConcurrentInstall e) {
                System.exit(EXIT_CONCURRENT);
            }
            System.exit(0);
        }

        private static void fakeInstall(Path toolsDir, Path toolPath, Path installs) {
            Path inProgress = toolsDir.resolve("INSTALLING");
            try {
                // CREATE_NEW semantics: throws if another install is in flight.
                Files.createFile(inProgress);
            } catch (FileAlreadyExistsException e) {
                throw new ConcurrentInstall();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                Files.write(installs, "installed\n".getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                Thread.sleep(500);
                Files.createFile(toolPath.resolve(CSharpRewriteRpc.Builder.TOOL_COMMAND));
                Files.delete(inProgress);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        private static class ConcurrentInstall extends RuntimeException {
        }
    }
}
