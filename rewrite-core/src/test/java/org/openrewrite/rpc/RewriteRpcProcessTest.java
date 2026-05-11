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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

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
