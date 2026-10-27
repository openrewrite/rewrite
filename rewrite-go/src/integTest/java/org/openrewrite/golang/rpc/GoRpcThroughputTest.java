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
package org.openrewrite.golang.rpc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.LongUnaryOperator;

import static java.util.stream.Collectors.toList;

/**
 * Measurement harness, not a correctness test: parse and print exercise opposite
 * directions of the wire, so one run measures both peers.
 */
@Timeout(value = 1800, unit = TimeUnit.SECONDS)
class GoRpcThroughputTest {

    @TempDir
    Path tempDir;

    static final com.sun.management.ThreadMXBean ALLOC =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    static {
        ThreadMXBean t = ManagementFactory.getThreadMXBean();
        if (t.isThreadCpuTimeSupported() && !t.isThreadCpuTimeEnabled()) {
            t.setThreadCpuTimeEnabled(true);
        }
    }

    /**
     * Kept per thread because a send runs on a traversal thread, and the live set changes
     * within a phase: totalling first would let a thread that exits mid-phase subtract the
     * baseline it contributed. Both counters are cumulative per thread.
     */
    static Map<Long, Long> cpuNanos() {
        ThreadMXBean t = ManagementFactory.getThreadMXBean();
        return byThread(t.getAllThreadIds(), t::getThreadCpuTime);
    }

    static Map<Long, Long> allocated() {
        return byThread(ManagementFactory.getThreadMXBean().getAllThreadIds(), ALLOC::getThreadAllocatedBytes);
    }

    static Map<Long, Long> byThread(long[] ids, LongUnaryOperator counter) {
        Map<Long, Long> reading = new HashMap<>();
        for (long id : ids) {
            long c = counter.applyAsLong(id);
            if (c > 0) {
                reading.put(id, c);
            }
        }
        return reading;
    }

    /** Work a thread did between the two readings; one absent from the first started inside them. */
    static long since(Map<Long, Long> start, Map<Long, Long> end) {
        long total = 0;
        for (Map.Entry<Long, Long> e : end.entrySet()) {
            total += e.getValue() - start.getOrDefault(e.getKey(), 0L);
        }
        return total;
    }

    @BeforeEach
    void before() {
        // Tracing is deliberately off: it logs every batch, which is the thing being timed.
        GoRewriteRpc.setFactory(GoRewriteRpc.builder()
                .goBinaryPath(Paths.get("build/rewrite-go-rpc").toAbsolutePath())
                .log(tempDir.resolve("go-rpc.log")));
    }

    @AfterEach
    void after() {
        GoRewriteRpc.shutdownCurrent();
    }

    @Test
    void parseThenPrintOwnSources() {
        Path project = Paths.get("").toAbsolutePath();
        List<String> exclusions = List.of("**/vendor/**", "**/build/**");

        // Parsing is measured in cycles too, because Java requests a page per batch here
        // and so is the side that waits, which print does not exercise. Each cycle resets
        // first, so the trees a peer retains for one cycle do not weigh on the next.
        for (int cycle = 0; cycle < 3; cycle++) {
            GoRewriteRpc.getOrStart().reset();
            List<SourceFile> files = parse(project, exclusions, "cycle " + cycle);
            printAfterReset(files, "cycle " + cycle);
        }
    }

    List<SourceFile> parse(Path project, List<String> exclusions, String label) {
        Map<Long, Long> cpu = cpuNanos(), alloc = allocated();
        long nanos = System.nanoTime();
        List<SourceFile> files = GoRewriteRpc.getOrStart()
                .parseProject(project, exclusions, new InMemoryExecutionContext(Throwable::printStackTrace))
                .collect(toList());
        report("PARSE", label, files.size(), nanos, cpu, alloc, 0);
        return files;
    }

    void printAfterReset(List<SourceFile> files, String label) {
        // Reset drops the peer's view of every tree, so each print that follows makes
        // the peer fetch it back, which is what exercises its receive path.
        GoRewriteRpc.getOrStart().reset();

        Map<Long, Long> cpu = cpuNanos(), alloc = allocated();
        long nanos = System.nanoTime();
        long chars = 0;
        for (SourceFile f : files) {
            chars += GoRewriteRpc.getOrStart().print(f).length();
        }
        report("PRINT", label, files.size(), nanos, cpu, alloc, chars);
    }

    static void report(String phase, String label, int files, long startNanos, Map<Long, Long> startCpu, Map<Long, Long> startAlloc, long chars) {
        System.out.printf("%s  %-10s %4d files  wall %,6d ms  jvmCpu %,6d ms  %,15d bytes%s%n",
                phase, label, files,
                (System.nanoTime() - startNanos) / 1_000_000,
                since(startCpu, cpuNanos()) / 1_000_000,
                since(startAlloc, allocated()),
                chars == 0 ? "" : String.format("  %,d chars", chars));
    }
}
