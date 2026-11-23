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
package org.openrewrite.python.rpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.rpc.internal.EngineInstall;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Every Gradle test fork starts its own Python RPC, so the first time a build needs a version of the engine, several
 * JVMs bootstrap it into one {@code pipPackagesPath} at once. {@code pip install --target} into one directory from
 * several processes fails ("Directory not empty"), so {@link PythonRewriteRpc.Builder#installEngine} must install it
 * once and have the others use that install. These run the bootstrap from several processes and threads at once, with
 * a stand-in for pip that fails, as pip does, when another install writes at the same time.
 */
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "pip's stand-in is a shell script")
@Timeout(value = 60, unit = SECONDS)
class PythonEngineInstallConcurrencyTest {

    private static final String VERSION = "8.94.0.dev20261026000000";
    private static final int PROCESSES = 3;
    private static final int THREADS = 3;

    @TempDir
    Path tempDir;

    @Test
    void severalBootstrapsAtOnceInstallTheEngineOnceAndAllUseIt() throws Exception {
        // given
        Path python = pip(0);
        Path packages = tempDir.resolve("packages");
        Path barrier = tempDir.resolve("go");

        // when: forks, each a JVM, and threads of this one, all released together
        List<Process> forks = new ArrayList<>();
        for (int i = 0; i < PROCESSES; i++) {
            forks.add(new ProcessBuilder(
                    Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp", System.getProperty("java.class.path"),
                    Fork.class.getName(), python.toString(), packages.toString(), barrier.toString())
                    .redirectErrorStream(true)
                    .start());
        }
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        List<Path> installed = new CopyOnWriteArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            Thread thread = new Thread(() -> {
                try {
                    awaitBarrier(barrier);
                    installed.add(PythonRewriteRpc.builder().installEngine(python, packages, VERSION));
                } catch (Throwable t) {
                    failures.add(t);
                }
            });
            thread.start();
            threads.add(thread);
        }
        Thread.sleep(500);
        Files.createFile(barrier);

        // then
        for (Process fork : forks) {
            assertThat(fork.waitFor(45, SECONDS)).as("fork timed out").isTrue();
            assertThat(fork.exitValue()).as(new String(readAll(fork), StandardCharsets.UTF_8)).isZero();
        }
        for (Thread thread : threads) {
            thread.join(SECONDS.toMillis(45));
        }
        assertThat(failures).isEmpty();
        Path version = packages.resolve(VERSION).toAbsolutePath().normalize();
        assertThat(installed).hasSize(THREADS).containsOnly(version);
        assertThat(Files.readAllLines(tempDir.resolve("installs")))
                .as("pip ran once, and every other bootstrap used what it installed").hasSize(1);
        assertThat(version.resolve("rewrite/__init__.py")).exists();
        assertThat(EngineInstall.isInstalled(version, VERSION)).isTrue();
        try (Stream<Path> entries = Files.list(packages)) {
            assertThat(entries.map(entry -> entry.getFileName().toString()))
                    .as("nothing a temporary install left")
                    .containsExactlyInAnyOrder(VERSION, VERSION + ".lock");
        }
    }

    @Test
    void aDirectoryARacedOrCrashedInstallLeftIsInstalledAgainRatherThanUsed() throws Exception {
        // given: what pip left when another install raced it, or the bootstrap was killed: no completion marker
        Path python = pip(0);
        Path packages = tempDir.resolve("packages");
        Path left = Files.createDirectories(packages.resolve(VERSION).resolve("rewrite"));
        Files.write(left.resolve("half-written.py"), new byte[0]);
        Files.write(packages.resolve(VERSION).resolve(".openrewrite-version"), VERSION.getBytes(StandardCharsets.UTF_8));

        // when
        Path version = PythonRewriteRpc.builder().installEngine(python, packages, VERSION);

        // then
        assertThat(Files.readAllLines(tempDir.resolve("installs"))).hasSize(1);
        assertThat(version.resolve("rewrite/__init__.py")).exists();
        assertThat(version.resolve("rewrite/half-written.py")).doesNotExist();
        assertThat(EngineInstall.isInstalled(version, VERSION)).isTrue();
    }

    @Test
    void aFailedInstallLeavesNothingTakenForInstalled() throws Exception {
        Path python = pip(1);
        Path packages = tempDir.resolve("packages");

        assertThatThrownBy(() -> PythonRewriteRpc.builder().installEngine(python, packages, VERSION))
                .hasMessageContaining("Failed to install openrewrite==" + VERSION + " (pip exited with code 1)")
                .hasMessageContaining("no matching distribution");
        assertThat(packages.resolve(VERSION)).doesNotExist();
        try (Stream<Path> entries = Files.list(packages)) {
            assertThat(entries.map(entry -> entry.getFileName().toString())).containsExactly(VERSION + ".lock");
        }
    }

    /**
     * A stand-in for the interpreter's {@code -m pip install --target=DIR SPEC}: it installs a {@code rewrite} package
     * into DIR, slowly, and fails, as pip does, when another install is writing at the same time; or it exits
     * {@code exitCode}, without installing anything, when that is not 0. Every install it makes is recorded.
     */
    private Path pip(int exitCode) throws IOException {
        Path state = tempDir.toAbsolutePath();
        Path script = state.resolve("python");
        Files.write(script, ("#!/bin/sh\n" +
                             "target=\n" +
                             "for arg in \"$@\"; do case \"$arg\" in --target=*) target=\"${arg#--target=}\";; esac; done\n" +
                             "[ -n \"$target\" ] || exit 64\n" +
                             "if [ " + exitCode + " -ne 0 ]; then echo 'ERROR: no matching distribution'; exit " + exitCode + "; fi\n" +
                             "mkdir '" + state + "/installing' 2>/dev/null || { echo 'Directory not empty'; exit 2; }\n" +
                             "echo \"$target\" >> '" + state + "/installs'\n" +
                             "sleep 1\n" +
                             "mkdir -p \"$target/rewrite\" && : > \"$target/rewrite/__init__.py\"\n" +
                             "rmdir '" + state + "/installing'\n").getBytes(StandardCharsets.UTF_8));
        assertThat(script.toFile().setExecutable(true)).isTrue();
        return script;
    }

    private static void awaitBarrier(Path barrier) throws InterruptedException {
        long deadline = System.nanoTime() + SECONDS.toNanos(30);
        while (!Files.exists(barrier)) {
            if (System.nanoTime() > deadline) {
                throw new IllegalStateException("never released");
            }
            Thread.sleep(10);
        }
    }

    private static byte[] readAll(Process process) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = process.getInputStream().read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    /**
     * Stands in for one Gradle test fork: it bootstraps the engine as the RPC's start does, and exits 0 when it got an
     * installed engine.
     */
    public static class Fork {
        public static void main(String[] args) throws Exception {
            Path python = Paths.get(args[0]);
            Path packages = Paths.get(args[1]);
            awaitBarrier(Paths.get(args[2]));
            Path version = PythonRewriteRpc.builder().installEngine(python, packages, VERSION);
            System.exit(EngineInstall.isInstalled(version, VERSION) ? 0 : 1);
        }
    }
}
