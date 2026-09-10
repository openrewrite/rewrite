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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

/**
 * Measurement harness, not a correctness test: parse and print exercise opposite
 * directions of the wire, so one run measures both peers.
 */
@Timeout(value = 1800, unit = TimeUnit.SECONDS)
class JavaScriptRpcThroughputTest {

    @TempDir
    Path tempDir;

    static final com.sun.management.ThreadMXBean ALLOC =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    static {
        java.lang.management.ThreadMXBean t = ManagementFactory.getThreadMXBean();
        if (t.isThreadCpuTimeSupported() && !t.isThreadCpuTimeEnabled()) {
            t.setThreadCpuTimeEnabled(true);
        }
    }

    /**
     * Keyed by thread id because a send runs on a traversal thread, so a phase's cost is
     * spread over threads that come and go within it.
     */
    static Map<Long, Long> cpuSnapshot() {
        java.lang.management.ThreadMXBean t = ManagementFactory.getThreadMXBean();
        Map<Long, Long> snapshot = new HashMap<>();
        for (long id : t.getAllThreadIds()) {
            long c = t.getThreadCpuTime(id);
            if (c > 0) {
                snapshot.put(id, c);
            }
        }
        return snapshot;
    }

    static Map<Long, Long> allocSnapshot() {
        Map<Long, Long> snapshot = new HashMap<>();
        for (long id : ManagementFactory.getThreadMXBean().getAllThreadIds()) {
            long b = ALLOC.getThreadAllocatedBytes(id);
            if (b > 0) {
                snapshot.put(id, b);
            }
        }
        return snapshot;
    }

    /**
     * Both counters are cumulative per thread, so a phase costs each thread's growth across it.
     * Keying on the later snapshot counts a thread that started mid-phase in full and one that
     * ended mid-phase not at all, which bounds the result from below.
     */
    static long delta(Map<Long, Long> before, Map<Long, Long> after) {
        long total = 0;
        for (Map.Entry<Long, Long> e : after.entrySet()) {
            total += e.getValue() - before.getOrDefault(e.getKey(), 0L);
        }
        return total;
    }

    @BeforeEach
    void before() {
        JavaScriptRewriteRpc.setFactory(JavaScriptRewriteRpc.builder()
                .recipeInstallDir(tempDir)
                .log(tempDir.resolve("rpc.log")));
    }

    @AfterEach
    void after() {
        JavaScriptRewriteRpc.shutdownCurrent();
    }

    @Test
    void parseThenPrintOwnSources() {
        Path project = Paths.get("rewrite").toAbsolutePath();
        List<String> exclusions = List.of("**/node_modules/**", "**/dist/**", "**/build/**");

        List<SourceFile> files = parse(project, exclusions, "parse");
        // Each cycle resets and refetches the same trees, so the early ones are warmup.
        for (int cycle = 0; cycle < 6; cycle++) {
            printAfterReset(files, "cycle " + cycle);
        }
    }

    List<SourceFile> parse(Path project, List<String> exclusions, String label) {
        Map<Long, Long> cpu = cpuSnapshot(), alloc = allocSnapshot();
        long nanos = System.nanoTime();
        List<SourceFile> files = JavaScriptRewriteRpc.getOrStart()
                .parseProject(project, exclusions, new InMemoryExecutionContext(Throwable::printStackTrace))
                .collect(toList());
        report("PARSE", label, files.size(), nanos, cpu, alloc, 0);
        return files;
    }

    void printAfterReset(List<SourceFile> files, String label) {
        // Reset drops the peer's view of every tree, so each print that follows makes
        // the peer fetch it back, which is what exercises its receive path.
        JavaScriptRewriteRpc.getOrStart().reset();

        Map<Long, Long> cpu = cpuSnapshot(), alloc = allocSnapshot();
        long nanos = System.nanoTime();
        long chars = 0;
        for (SourceFile f : files) {
            chars += JavaScriptRewriteRpc.getOrStart().print(f).length();
        }
        report("PRINT", label, files.size(), nanos, cpu, alloc, chars);
    }

    static void report(String phase, String label, int files, long startNanos,
                       Map<Long, Long> startCpu, Map<Long, Long> startAlloc, long chars) {
        System.out.printf("%s  %-10s %4d files  wall %,6d ms  jvmCpu %,6d ms  %,15d bytes%s%n",
                phase, label, files,
                (System.nanoTime() - startNanos) / 1_000_000,
                delta(startCpu, cpuSnapshot()) / 1_000_000,
                delta(startAlloc, allocSnapshot()),
                chars == 0 ? "" : String.format("  %,d chars", chars));
    }
}
