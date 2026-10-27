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
package org.openrewrite.csharp.rpc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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

import static java.util.stream.Collectors.toList;

/**
 * Measurement harness, not a correctness test: parse and print exercise opposite
 * directions of the wire, so one run measures both peers.
 */
// This module's `check` runs integTest, and the tag is what keeps a measurement
// off that path; `-PincludeSlow` opts back in.
@Tag("slow")
@Timeout(value = 1800, unit = TimeUnit.SECONDS)
class CSharpRpcThroughputTest {

    static final com.sun.management.ThreadMXBean ALLOC =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    static {
        ThreadMXBean t = ManagementFactory.getThreadMXBean();
        if (t.isThreadCpuTimeSupported() && !t.isThreadCpuTimeEnabled()) {
            t.setThreadCpuTimeEnabled(true);
        }
    }

    /**
     * Per thread because a send runs on a traversal thread and both counters are cumulative
     * per thread. Kept keyed so that a thread which ends during a phase, and so is absent from
     * the later reading, does not subtract its whole total from the difference.
     */
    static Map<Long, Long> cpuNanos() {
        ThreadMXBean t = ManagementFactory.getThreadMXBean();
        Map<Long, Long> perThread = new HashMap<>();
        for (long id : t.getAllThreadIds()) {
            long c = t.getThreadCpuTime(id);
            if (c > 0) {
                perThread.put(id, c);
            }
        }
        return perThread;
    }

    static Map<Long, Long> allocated() {
        Map<Long, Long> perThread = new HashMap<>();
        for (long id : ManagementFactory.getThreadMXBean().getAllThreadIds()) {
            long b = ALLOC.getThreadAllocatedBytes(id);
            if (b > 0) {
                perThread.put(id, b);
            }
        }
        return perThread;
    }

    /** Only threads in the later reading contribute, each net of what it had already accrued. */
    static long since(Map<Long, Long> start, Map<Long, Long> end) {
        long total = 0;
        for (Map.Entry<Long, Long> e : end.entrySet()) {
            total += e.getValue() - start.getOrDefault(e.getKey(), 0L);
        }
        return total;
    }

    @BeforeEach
    void before() {
        CSharpRewriteRpc.setFactory(CSharpRewriteRpc.builder()
                .csharpServerEntry(serverEntry())
                .log(Paths.get(System.getProperty("java.io.tmpdir"), "csharp-rpc-throughput.log")));
    }

    @AfterEach
    void after() {
        CSharpRewriteRpc.shutdownCurrent();
    }

    /**
     * The module is the working directory under Gradle and the repository root when a
     * run is launched from there, so the entry is looked up under both.
     */
    static Path serverEntry() {
        Path base = Paths.get(System.getProperty("user.dir"));
        for (Path candidate : List.of(base.resolve("csharp"), base.resolve("rewrite-csharp/csharp"))) {
            Path csproj = candidate.resolve("OpenRewrite.Tool/OpenRewrite.Tool.csproj");
            if (csproj.toFile().exists()) {
                return csproj.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException("Could not find the C# Rewrite project");
    }

    static Path solution() {
        Path base = Paths.get(System.getProperty("user.dir"));
        for (Path candidate : List.of(base.resolve("csharp"), base.resolve("rewrite-csharp/csharp"))) {
            Path sln = candidate.resolve("OpenRewrite.sln");
            if (sln.toFile().exists()) {
                return sln.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException("Could not find OpenRewrite.sln");
    }

    @Test
    void parseThenPrintOwnSources() {
        List<SourceFile> files = parse(solution(), "parse 0");
        for (int p = 1; p < 4; p++) {
            CSharpRewriteRpc.getOrStart().reset();
            files = parse(solution(), "parse " + p);
        }
        // Each cycle resets and refetches the same trees, so the early ones are warmup and the
        // rest are repetitions within one process, which is what makes a single run comparable.
        for (int cycle = 0; cycle < 12; cycle++) {
            printAfterReset(files, "cycle " + cycle);
        }
    }

    List<SourceFile> parse(Path sln, String label) {
        Map<Long, Long> cpu = cpuNanos(), alloc = allocated();
        long nanos = System.nanoTime();
        List<SourceFile> files = CSharpRewriteRpc.getOrStart()
                .parseSolution(sln, sln.getParent(), new InMemoryExecutionContext(Throwable::printStackTrace))
                .collect(toList());
        report("PARSE", label, files.size(), nanos, cpu, alloc, 0);
        return files;
    }

    void printAfterReset(List<SourceFile> files, String label) {
        // Reset drops the peer's view of every tree, so each print that follows makes
        // the peer fetch it back, which is what exercises its receive path.
        CSharpRewriteRpc.getOrStart().reset();

        Map<Long, Long> cpu = cpuNanos(), alloc = allocated();
        long nanos = System.nanoTime();
        long chars = 0;
        for (SourceFile f : files) {
            chars += CSharpRewriteRpc.getOrStart().print(f).length();
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
