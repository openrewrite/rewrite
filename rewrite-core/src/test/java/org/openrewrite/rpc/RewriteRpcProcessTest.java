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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
     * A subprocess that outlasts the grace period is force-killed by {@code shutdown()} itself,
     * which on Unix yields exit code 137. That is this method's own SIGKILL, not a crash, so
     * {@code shutdown()} must not report it — doing so failed commands whose work had already
     * completed, intermittently, whenever the machine was loaded enough to slow the subprocess'
     * SIGTERM handling past the grace period.
     */
    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "destroy() maps to TerminateProcess, which no child can delay")
    void shutdownDoesNotReportItsOwnForceKillAsFailure() throws Exception {
        Path ready = Files.createTempFile("rpc-force-kill-ready", ".marker");
        RewriteRpcProcess process = forkedProcess(SigtermIgnoringEntryPoint.class, ready);
        // Shorter than the default 5s so the test doesn't have to wait it out.
        process.setShutdownGracePeriod(Duration.ofMillis(500));
        Process underlying = null;
        try {
            process.start();
            underlying = underlyingProcess(process);
            awaitReady(ready);

            assertThatCode(process::shutdown)
                    .as("a subprocess force-killed by shutdown() is not a failure of the command")
                    .doesNotThrowAnyException();

            assertThat(underlying.isAlive()).as("subprocess should have been killed").isFalse();
            assertThat(underlying.exitValue())
                    .as("the subprocess must actually have needed the force-kill, or this test proves nothing")
                    .isEqualTo(137);
        } finally {
            if (underlying != null) {
                underlying.destroyForcibly();
            }
            Files.deleteIfExists(ready);
        }
    }

    /**
     * The complement of {@link #shutdownDoesNotReportItsOwnForceKillAsFailure()}: an exit code
     * that the subprocess produced on its own — rather than one {@code shutdown()} inflicted —
     * is still surfaced. Also covers that the stderr drain thread is joined before the throw
     * escapes, since the parent-side log handle must be released on that path too.
     */
    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "destroy() maps to TerminateProcess, so the child never runs its hook")
    void shutdownStillReportsAnExitCodeTheSubprocessChose() throws Exception {
        Path ready = Files.createTempFile("rpc-unexpected-exit-ready", ".marker");
        Path stderrLog = Files.createTempFile("rpc-unexpected-exit", ".log");
        RewriteRpcProcess process = forkedProcess(UnexpectedExitEntryPoint.class, ready);
        process.setStderrRedirect(stderrLog);
        Process underlying = null;
        try {
            process.start();
            underlying = underlyingProcess(process);
            awaitReady(ready);
            Thread drainThread = field(process, "stderrDrainThread", Thread.class);

            assertThatThrownBy(process::shutdown)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("exited with code " + UnexpectedExitEntryPoint.EXIT_CODE)
                    .hasMessageContaining(stderrLog.toString());

            assertThat(drainThread.isAlive())
                    .as("the stderr drain must be joined before shutdown() throws, or the log handle leaks")
                    .isFalse();
        } finally {
            if (underlying != null) {
                underlying.destroyForcibly();
            }
            Files.deleteIfExists(ready);
            Files.deleteIfExists(stderrLog);
        }
    }

    private static RewriteRpcProcess forkedProcess(Class<?> entryPoint, Path readyMarker) {
        return new RewriteRpcProcess(
                System.getProperty("java.home") + "/bin/java",
                "-cp", System.getProperty("java.class.path"),
                entryPoint.getName(), readyMarker.toString());
    }

    private static Process underlyingProcess(RewriteRpcProcess process) throws Exception {
        return field(process, "process", Process.class);
    }

    private static <T> T field(RewriteRpcProcess process, String name, Class<T> type) throws Exception {
        Field f = RewriteRpcProcess.class.getDeclaredField(name);
        f.setAccessible(true);
        return type.cast(f.get(process));
    }

    /**
     * Blocks until the forked JVM has written its readiness marker. Without this the test can
     * SIGTERM the child before it installs its shutdown hook, which silently turns both tests
     * into assertions about an ordinary JVM exit.
     */
    private static void awaitReady(Path marker) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (Files.size(marker) == 0) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("forked JVM never signalled readiness at " + marker);
            }
            //noinspection BusyWait
            Thread.sleep(50);
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

    /**
     * Forked entry point whose shutdown hook blocks, so the JVM cannot finish exiting on
     * SIGTERM and {@code shutdown()} has to escalate to SIGKILL.
     */
    public static class SigtermIgnoringEntryPoint {
        public static void main(String[] args) throws Exception {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(60));
                } catch (InterruptedException ignored) {
                }
            }));
            signalReady(args[0]);
            Thread.sleep(TimeUnit.SECONDS.toMillis(60));
        }
    }

    /**
     * Forked entry point that exits on SIGTERM with a code the parent has no reason to expect,
     * standing in for a subprocess that dies of its own accord as it is being shut down.
     */
    public static class UnexpectedExitEntryPoint {
        static final int EXIT_CODE = 3;

        public static void main(String[] args) throws Exception {
            // halt() rather than System.exit(), which deadlocks when called from a shutdown hook.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> Runtime.getRuntime().halt(EXIT_CODE)));
            signalReady(args[0]);
            Thread.sleep(TimeUnit.SECONDS.toMillis(60));
        }
    }

    private static void signalReady(String marker) throws IOException {
        Files.write(Paths.get(marker), "ready".getBytes());
    }
}
