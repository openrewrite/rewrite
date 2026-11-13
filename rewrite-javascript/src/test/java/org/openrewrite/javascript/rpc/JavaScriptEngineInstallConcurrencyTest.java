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
package org.openrewrite.javascript.rpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.rpc.internal.EngineInstall;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Every Gradle test fork starts its own JavaScript RPC, and npx installs the engine into one cache directory on first
 * use, so several installing it at once leave it half-written. {@link JavaScriptRewriteRpc.Builder#installEngine} has
 * one of them install it while the others wait. These run it from several processes and threads at once, with a
 * stand-in for npx that fails when another install is under way at the same time.
 */
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "npx's stand-in is a shell script")
@Timeout(value = 60, unit = SECONDS)
class JavaScriptEngineInstallConcurrencyTest {

    private static final String VERSION = "8.94.0-20261026-000000";
    private static final int PROCESSES = 2;
    private static final int THREADS = 3;

    @TempDir
    Path tempDir;

    @Test
    void severalStartsAtOnceInstallTheEngineOnce() throws Exception {
        // given
        Path npx = npx(0);
        Path cache = tempDir.resolve("npm-cache");
        Path barrier = tempDir.resolve("go");

        // when
        List<Process> forks = new ArrayList<>();
        for (int i = 0; i < PROCESSES; i++) {
            forks.add(new ProcessBuilder(
                    Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                    "-cp", System.getProperty("java.class.path"),
                    Fork.class.getName(), npx.toString(), cache.toString(), barrier.toString())
                    .redirectErrorStream(true)
                    .start());
        }
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            Thread thread = new Thread(() -> {
                try {
                    awaitBarrier(barrier);
                    JavaScriptRewriteRpc.builder().installEngine(npx, VERSION, environment(cache));
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
        assertThat(Files.readAllLines(tempDir.resolve("installs")))
                .as("npx installed it once, as the RPC itself would run it, with nothing but its help")
                .containsExactly("--yes --package=@openrewrite/rewrite@" + VERSION + " rewrite-rpc --help");
        assertThat(EngineInstall.isInstalled(cache.resolve("_openrewrite/javascript").resolve(VERSION), VERSION))
                .isTrue();
    }

    @Test
    void aFailedInstallIsNotRecordedAndTheNextStartTriesAgain() throws Exception {
        Path cache = tempDir.resolve("npm-cache");

        assertThatThrownBy(() -> JavaScriptRewriteRpc.builder().installEngine(npx(1), VERSION, environment(cache)))
                .hasMessageContaining("Could not install @openrewrite/rewrite@" + VERSION)
                .hasMessageContaining("npm error notarget");
        assertThat(cache.resolve("_openrewrite/javascript").resolve(VERSION)).doesNotExist();

        JavaScriptRewriteRpc.builder().installEngine(npx(0), VERSION, environment(cache));
        assertThat(EngineInstall.isInstalled(cache.resolve("_openrewrite/javascript").resolve(VERSION), VERSION))
                .isTrue();
    }

    @Test
    void theInstallIsRecordedBesideTheCacheNpmUses() {
        assertThat(JavaScriptRewriteRpc.Builder.npmCache(Collections.singletonMap("NPM_CONFIG_CACHE", "/elsewhere")))
                .isEqualTo(Paths.get("/elsewhere"));
        assertThat(JavaScriptRewriteRpc.Builder.npmCache(Collections.singletonMap("npm_config_cache", "/lower")))
                .isEqualTo(Paths.get("/lower"));
    }

    private static Map<String, String> environment(Path cache) {
        return Collections.singletonMap("npm_config_cache", cache.toString());
    }

    /**
     * A stand-in for npx that installs slowly and fails when another install is under way at the same time, as npx's do
     * when they share a cache; or that exits {@code exitCode} as npm does for a version it cannot find.
     */
    private Path npx(int exitCode) throws IOException {
        Path state = tempDir.toAbsolutePath();
        Path script = state.resolve("npx-" + exitCode);
        Files.write(script, ("#!/bin/sh\n" +
                             "if [ " + exitCode + " -ne 0 ]; then echo 'npm error notarget'; exit " + exitCode + "; fi\n" +
                             "mkdir '" + state + "/installing' 2>/dev/null || { echo 'half-written'; exit 2; }\n" +
                             "echo \"$*\" >> '" + state + "/installs'\n" +
                             "sleep 1\n" +
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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = process.getInputStream().read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    /**
     * Stands in for one Gradle test fork, starting an RPC as far as installing its engine.
     */
    public static class Fork {
        public static void main(String[] args) throws Exception {
            Path npx = Paths.get(args[0]);
            Path cache = Paths.get(args[1]);
            awaitBarrier(Paths.get(args[2]));
            JavaScriptRewriteRpc.builder().installEngine(npx, VERSION, environment(cache));
            System.exit(0);
        }
    }
}
