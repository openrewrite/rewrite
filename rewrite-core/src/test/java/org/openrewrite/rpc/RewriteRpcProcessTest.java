/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.rpc;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class RewriteRpcProcessTest {

    /**
     * Forks a JVM that spawns a long-running child via {@link RewriteRpcProcess} and
     * exits without calling {@link RewriteRpcProcess#shutdown()}. Asserts that the
     * spawned child is no longer alive once the forked JVM exits, confirming that
     * the JVM-exit shutdown hook on {@code RewriteRpcProcess} actually killed it.
     * <p>
     * Without the hook this test fails: on Unix the orphan would be reparented to
     * init and remain alive; on Windows it would simply continue running.
     */
    @Test
    void shutdownHookKillsChildOnJvmExit() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                System.getProperty("java.home") + "/bin/java",
                "-cp", System.getProperty("java.class.path"),
                ForkedJvmEntryPoint.class.getName());
        pb.redirectErrorStream(true);
        Process forked = pb.start();

        long childPid = -1;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(forked.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("RPC_PID=")) {
                    childPid = Long.parseLong(line.substring("RPC_PID=".length()).trim());
                }
            }
        }
        assertThat(forked.waitFor(15, TimeUnit.SECONDS))
                .as("forked JVM should exit on its own")
                .isTrue();
        assertThat(childPid).as("forked JVM should have printed an RPC_PID").isPositive();

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (isAlive(childPid) && System.nanoTime() < deadline) {
            //noinspection BusyWait
            Thread.sleep(50);
        }

        boolean stillAlive = isAlive(childPid);
        if (stillAlive) {
            // Don't leak a stray sleep if the assertion is about to fail.
            ProcessHandle.of(childPid).ifPresent(ProcessHandle::destroyForcibly);
        }
        assertThat(stillAlive)
                .as("child PID %s should be dead after forked JVM exit (shutdown hook)", childPid)
                .isFalse();
    }

    private static boolean isAlive(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    /**
     * After {@link RewriteRpcProcess#shutdown()} returns, the {@code rpc-stderr-drain}
     * thread must have completed; otherwise the parent-side handle on the stderr
     * redirect log file outlives {@code shutdown()}, which on Windows blocks
     * {@code @TempDir} cleanup and same-path reopens (no FILE_SHARE_DELETE).
     */
    @Test
    void shutdownWaitsForStderrDrainToComplete() throws Exception {
        int parallel = 16;
        List<Path> logs = new ArrayList<>();
        List<RewriteRpcProcess> processes = new ArrayList<>();
        List<Thread> drainThreads = new ArrayList<>();
        try {
            // given: many subprocesses flooding stderr in parallel — competing
            // for CPU and disk widens the race window between "subprocess exits"
            // and "drain thread closes its OutputStream"
            for (int i = 0; i < parallel; i++) {
                Path log = Files.createTempFile("rpc-stderr-drain-test-" + i, ".log");
                logs.add(log);
                RewriteRpcProcess process = new RewriteRpcProcess(
                        System.getProperty("java.home") + "/bin/java",
                        "-cp", System.getProperty("java.class.path"),
                        StderrFlooderEntryPoint.class.getName());
                process.setStderrRedirect(log);
                process.start();
                processes.add(process);
            }
            Thread.sleep(500);
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if ("rpc-stderr-drain".equals(t.getName())) {
                    drainThreads.add(t);
                }
            }
            assertThat(drainThreads)
                    .as("one drain thread per subprocess should have been started")
                    .hasSize(parallel);

            // when: each subprocess is shut down in turn
            for (RewriteRpcProcess process : processes) {
                process.shutdown();
            }

            // then: every drain thread must have closed its OutputStream (i.e.
            // released the log file handle) before its shutdown() returned
            List<Thread> leaked = drainThreads.stream().filter(Thread::isAlive).collect(toList());
            assertThat(leaked)
                    .as("rpc-stderr-drain threads should be joined before shutdown() returns; " +
                            "otherwise the parent-side log handle outlives shutdown() and breaks " +
                            "log-file deletion/reuse on Windows. Still-alive drain threads: %s",
                            leaked)
                    .isEmpty();
        } finally {
            for (Path log : logs) {
                Files.deleteIfExists(log);
            }
        }
    }

    /**
     * If the subprocess binary cannot be exec'd (e.g. missing from PATH), the spawn thread
     * dies with an {@link java.io.IOException} but {@code start()} previously busy-waited
     * on {@code process != null} forever — see the Moderne CLI hang when {@code rewrite-go-rpc}
     * is not installed. {@code start()} must observe the dead spawn thread and surface the
     * failure instead of hanging.
     */
    @Test
    void startFailsFastWhenBinaryMissing() {
        // given: a command pointing at a binary that does not exist anywhere
        String missing = "definitely-no-such-binary-7a3f9e2c";
        RewriteRpcProcess process = new RewriteRpcProcess(missing);

        // when / then: start() must surface the failure within a bounded time, not hang
        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThatThrownBy(process::start)
                        .isInstanceOf(UncheckedIOException.class)
                        .hasMessageContaining(missing));
    }

    /**
     * When the subprocess dies, its stderr is the only thing that explains why. The drain
     * thread has to consume stderr eagerly to keep the subprocess off a full pipe, so
     * unless a tail is retained the diagnostic is read and discarded and the failure
     * surfaces as a bare exit code — see the recurring {@code RPC process shut down early
     * with exit code 217/1/127} failures in rewrite-static-analysis CI, which had no
     * attributable cause for exactly this reason.
     */
    @Test
    void livenessCheckReportsSubprocessStderr() throws Exception {
        // given: a subprocess that reports a startup failure on stderr and exits non-zero
        RewriteRpcProcess process = new RewriteRpcProcess(
                System.getProperty("java.home") + "/bin/java",
                "-cp", System.getProperty("java.class.path"),
                StderrThenExitEntryPoint.class.getName());
        try {
            process.start();

            // when: it has exited
            assertThat(underlyingProcess(process).waitFor(30, TimeUnit.SECONDS))
                    .as("subprocess should exit on its own")
                    .isTrue();

            // then: the liveness check explains why, not just that
            assertThat(process.getLivenessCheck())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("exit code 217")
                    .hasMessageContaining(StderrThenExitEntryPoint.MESSAGE);
        } finally {
            process.shutdown();
        }
    }

    /**
     * The retained stderr is bounded, so a subprocess that logs steadily can't be held in
     * memory in full. The bytes worth keeping are the last ones — a stack trace or fatal
     * error lands at the end — so eviction must drop the head, not truncate the tail.
     */
    @Test
    void livenessCheckRetainsTailOfOversizeStderr() throws Exception {
        // given: a subprocess emitting far more stderr than the retained tail holds
        RewriteRpcProcess process = new RewriteRpcProcess(
                System.getProperty("java.home") + "/bin/java",
                "-cp", System.getProperty("java.class.path"),
                OversizeStderrEntryPoint.class.getName());
        try {
            process.start();

            // when: it has exited
            assertThat(underlyingProcess(process).waitFor(30, TimeUnit.SECONDS))
                    .as("subprocess should exit on its own")
                    .isTrue();

            // then: the last thing it said survives, and the first thing was evicted
            assertThat(process.getLivenessCheck())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(OversizeStderrEntryPoint.TAIL)
                    .hasMessageNotContaining(OversizeStderrEntryPoint.HEAD);
        } finally {
            process.shutdown();
        }
    }

    /**
     * The retained tail only reaches the caller through {@link RewriteRpcProcess#getLivenessCheck()},
     * so it is no help when the subprocess logs heavily before dying, or misbehaves without exiting
     * at all. {@code REWRITE_RPC_STDERR=console} mirrors the whole stream to the parent's stderr,
     * which is settable in CI without touching the repo under test.
     */
    @Test
    void mirrorsSubprocessStderrToConsoleWhenEnvVarSet() throws Exception {
        assertThat(runForkedSpawner("console"))
                .as("REWRITE_RPC_STDERR=console should mirror the subprocess's stderr")
                .contains(StderrThenExitEntryPoint.MESSAGE);
    }

    /**
     * Negative control for {@link #mirrorsSubprocessStderrToConsoleWhenEnvVarSet()} — mirroring is
     * strictly opt-in, so an unset variable must leave the parent's stderr untouched. Without this
     * the positive test would still pass if stderr were mirrored unconditionally.
     */
    @Test
    void doesNotMirrorSubprocessStderrByDefault() throws Exception {
        assertThat(runForkedSpawner(null))
                .as("stderr mirroring should be opt-in")
                .doesNotContain(StderrThenExitEntryPoint.MESSAGE);
    }

    /**
     * Runs {@link SpawnerEntryPoint} in a forked JVM, optionally with {@code REWRITE_RPC_STDERR}
     * set, and returns everything that JVM wrote to stdout and stderr.
     */
    private static String runForkedSpawner(@Nullable String stderrEnv) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                System.getProperty("java.home") + "/bin/java",
                "-cp", System.getProperty("java.class.path"),
                SpawnerEntryPoint.class.getName());
        if (stderrEnv == null) {
            pb.environment().remove("REWRITE_RPC_STDERR");
        } else {
            pb.environment().put("REWRITE_RPC_STDERR", stderrEnv);
        }
        pb.redirectErrorStream(true);
        Process forked = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(forked.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }
        assertThat(forked.waitFor(30, TimeUnit.SECONDS))
                .as("forked JVM should exit on its own")
                .isTrue();
        return output.toString();
    }

    /**
     * Runs in the forked JVM. Spawns {@link StderrThenExitEntryPoint} via
     * {@link RewriteRpcProcess} and waits for it to exit, so whatever the drain thread
     * did with its stderr has landed by the time the forked JVM exits.
     */
    public static class SpawnerEntryPoint {
        public static void main(String[] args) throws Exception {
            RewriteRpcProcess proc = new RewriteRpcProcess(
                    System.getProperty("java.home") + "/bin/java",
                    "-cp", System.getProperty("java.class.path"),
                    StderrThenExitEntryPoint.class.getName());
            proc.start();

            Field f = RewriteRpcProcess.class.getDeclaredField("process");
            f.setAccessible(true);
            ((Process) f.get(proc)).waitFor(20, TimeUnit.SECONDS);

            proc.shutdown();
        }
    }

    private static Process underlyingProcess(RewriteRpcProcess process) throws Exception {
        Field f = RewriteRpcProcess.class.getDeclaredField("process");
        f.setAccessible(true);
        return (Process) f.get(process);
    }

    /**
     * Forked entry point that writes a diagnostic to stderr and exits with the same code
     * observed in CI. Uses {@code halt} so no shutdown hook can add output after it.
     */
    public static class StderrThenExitEntryPoint {
        static final String MESSAGE = "rpc-server: failed to start, ENOENT";

        public static void main(String[] args) {
            System.err.println(MESSAGE);
            System.err.flush();
            Runtime.getRuntime().halt(217);
        }
    }

    /**
     * Forked entry point that brackets far more than the retained tail of filler between
     * two markers, so the test can tell which end of the buffer survived eviction.
     */
    public static class OversizeStderrEntryPoint {
        static final String HEAD = "HEAD-MARKER-should-be-evicted";
        static final String TAIL = "TAIL-MARKER-should-survive";

        public static void main(String[] args) {
            System.err.println(HEAD);
            byte[] junk = new byte[8192];
            Arrays.fill(junk, (byte) 'x');
            String filler = new String(junk, StandardCharsets.UTF_8);
            for (int i = 0; i < 8; i++) {
                System.err.print(filler);
            }
            System.err.println(TAIL);
            System.err.flush();
            Runtime.getRuntime().halt(1);
        }
    }

    /**
     * Runs in the forked JVM. Spawns a long-running child via {@link RewriteRpcProcess},
     * prints its PID, and returns from {@code main} so the JVM exits without an explicit
     * {@code shutdown()} call.
     */
    public static class ForkedJvmEntryPoint {
        public static void main(String[] args) throws Exception {
            RewriteRpcProcess proc = new RewriteRpcProcess("sleep", "30");
            proc.start();

            Field f = RewriteRpcProcess.class.getDeclaredField("process");
            f.setAccessible(true);
            Process underlying = (Process) f.get(proc);

            System.out.println("RPC_PID=" + underlying.pid());
            System.out.flush();
        }
    }

    /**
     * Forked entry point that continuously writes to stderr until killed.
     * The producer outruns any reasonable drain, so the kernel pipe buffer
     * stays full and the drain thread has guaranteed pending work when the
     * test calls {@link RewriteRpcProcess#shutdown()}.
     */
    public static class StderrFlooderEntryPoint {
        public static void main(String[] args) throws Exception {
            byte[] junk = new byte[8192];
            Arrays.fill(junk, (byte) 'x');
            while (true) {
                System.err.write(junk);
                System.err.flush();
            }
        }
    }
}
