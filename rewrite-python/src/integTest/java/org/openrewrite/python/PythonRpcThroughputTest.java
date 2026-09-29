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
     * Per thread, because a send runs on a traversal thread and both counters are
     * cumulative per thread. Kept as a snapshot so {@link #since} can tell a thread
     * that started mid-phase from one that was already running.
     */
    static Map<Long, Long> cpuNanos() {
        ThreadMXBean t = ManagementFactory.getThreadMXBean();
        Map<Long, Long> byThread = new HashMap<>();
        for (long id : t.getAllThreadIds()) {
            long c = t.getThreadCpuTime(id);
            if (c > 0) {
                byThread.put(id, c);
            }
        }
        return byThread;
    }

    static Map<Long, Long> allocated() {
        Map<Long, Long> byThread = new HashMap<>();
        for (long id : ManagementFactory.getThreadMXBean().getAllThreadIds()) {
            long b = ALLOC.getThreadAllocatedBytes(id);
            if (b > 0) {
                byThread.put(id, b);
            }
        }
        return byThread;
    }

    /**
     * Work recorded against the threads alive at the end, counting one that started
     * in between from zero. A thread that exits mid-phase takes its own total with
     * it: the counters are unreadable once it is gone, so this is a lower bound.
     */
    static long since(Map<Long, Long> before, Map<Long, Long> after) {
        long total = 0;
        for (Map.Entry<Long, Long> e : after.entrySet()) {
            total += e.getValue() - before.getOrDefault(e.getKey(), 0L);
        }
        return total;
    }

    @BeforeEach
    void before() {
        // Tracing is deliberately off: it logs every batch, which is the thing being timed.
        PythonRewriteRpc.Builder builder = PythonRewriteRpc.builder()
                .log(tempDir.resolve("python-rpc.log"));
        String exe = System.getenv("REWRITE_PY_EXE");
        if (exe != null) {
            builder = builder.pythonPath(java.nio.file.Paths.get(exe));
        }
        System.out.println("PYEXE " + (exe == null ? "<default>" : exe));
        PythonRewriteRpc.setFactory(builder);
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
        Map<Long, Long> cpu = cpuNanos(), alloc = allocated();
        long nanos = System.nanoTime();
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

        Map<Long, Long> cpu = cpuNanos(), alloc = allocated();
        long nanos = System.nanoTime();
        long chars = 0;
        for (SourceFile f : files) {
            chars += PythonRewriteRpc.getOrStart().print(f).length();
        }
        report("PRINT", label, files.size(), nanos, cpu, alloc, chars);
    }

    static void report(String phase, String label, int files, long startNanos,
                       Map<Long, Long> startCpu, Map<Long, Long> startAlloc, long chars) {
        System.out.printf("%s  %-10s %4d files  wall %,6d ms  jvmCpu %,6d ms  %,15d bytes%s%n",
                phase, label, files,
                (System.nanoTime() - startNanos) / 1_000_000,
                since(startCpu, cpuNanos()) / 1_000_000,
                since(startAlloc, allocated()),
                chars == 0 ? "" : String.format("  %,d chars", chars));
    }
}
