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
package org.openrewrite.python;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.python.rpc.PythonRewriteRpc;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

/**
 * Measurement harness, not a correctness test: parse and print exercise opposite
 * directions of the wire, so one run measures both peers.
 */
@Timeout(value = 1800, unit = TimeUnit.SECONDS)
class PythonRpcThroughputTest {

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
     * Summed across threads because a send runs on a traversal thread. Both counters
     * are cumulative per thread, so the difference of two readings is the work between.
     */
    static long cpuNanos() {
        ThreadMXBean t = ManagementFactory.getThreadMXBean();
        long total = 0;
        for (long id : t.getAllThreadIds()) {
            long c = t.getThreadCpuTime(id);
            if (c > 0) {
                total += c;
            }
        }
        return total;
    }

    static long allocated() {
        long total = 0;
        for (long id : ManagementFactory.getThreadMXBean().getAllThreadIds()) {
            long b = ALLOC.getThreadAllocatedBytes(id);
            if (b > 0) {
                total += b;
            }
        }
        return total;
    }

    @BeforeEach
    void before() {
        // Tracing is deliberately off: it logs every batch, which is the thing being timed.
        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder()
                .log(tempDir.resolve("python-rpc.log")));
    }

    @AfterEach
    void after() {
        PythonRewriteRpc.shutdownCurrent();
        PythonRewriteRpc.setFactory(PythonRewriteRpc.builder());
    }

    @Test
    void parseThenPrintOwnSources() {
        Path project = Paths.get("rewrite").toAbsolutePath();
        List<String> exclusions = List.of("**/.venv/**", "**/build/**", "**/__pycache__/**");

        List<SourceFile> files = parse(project, exclusions, "parse");
        // Each cycle resets and refetches the same trees, so the early ones are warmup.
        for (int cycle = 0; cycle < 6; cycle++) {
            printAfterReset(files, "cycle " + cycle);
        }
    }

    List<SourceFile> parse(Path project, List<String> exclusions, String label) {
        long cpu = cpuNanos(), alloc = allocated(), nanos = System.nanoTime();
        List<SourceFile> files = PythonRewriteRpc.getOrStart()
                .parseProject(project, exclusions, new InMemoryExecutionContext(Throwable::printStackTrace))
                .collect(toList());
        report("PARSE", label, files.size(), nanos, cpu, alloc, 0);
        return files;
    }

    void printAfterReset(List<SourceFile> files, String label) {
        // Reset drops the peer's view of every tree, so each print that follows makes
        // the peer fetch it back, which is what exercises its receive path.
        PythonRewriteRpc.getOrStart().reset();

        long cpu = cpuNanos(), alloc = allocated(), nanos = System.nanoTime();
        long chars = 0;
        for (SourceFile f : files) {
            chars += PythonRewriteRpc.getOrStart().print(f).length();
        }
        report("PRINT", label, files.size(), nanos, cpu, alloc, chars);
    }

    static void report(String phase, String label, int files, long startNanos, long startCpu, long startAlloc, long chars) {
        System.out.printf("%s  %-10s %4d files  wall %,6d ms  jvmCpu %,6d ms  %,15d bytes%s%n",
                phase, label, files,
                (System.nanoTime() - startNanos) / 1_000_000,
                (cpuNanos() - startCpu) / 1_000_000,
                allocated() - startAlloc,
                chars == 0 ? "" : String.format("  %,d chars", chars));
    }
}
