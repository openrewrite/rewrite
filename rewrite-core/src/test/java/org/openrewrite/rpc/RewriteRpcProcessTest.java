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
import java.util.concurrent.TimeUnit;

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
}
