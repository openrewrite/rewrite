/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.maven.parity.corpus;

import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.internal.ResolutionEngineSelector;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The <em>integrated</em> perf gate (DESIGN §6): both engines now live behind one facade on one classpath, so the honest
 * benchmark is MavenParser end-to-end in a single JVM — {@code ctx engine=legacy} vs {@code engine=maven} over an
 * identical reactor input set. Supersedes {@code spike/benchmark}'s split-JVM OLD/NEW design, which predated
 * integration and measured the raw (unadapted) pipeline.
 * <p>
 * Warm iterations are zero-download: each engine mode is primed once through {@link RecordingHttpSender} RECORD, then
 * every measured iteration replays from the store with no network. Metrics per (corpus, tier, engine): warm full-reactor
 * median (≥5 iters after 2 warm-ups, fresh {@link MavenPomCache} each), a warm-cache re-resolution loop, and a
 * peak-ish used-heap note. Emits a {@code RESULT} line per mode; the ratio table is assembled from those.
 * <p>
 * Args: {@code <reactorDirName> <cap|full>}. Sysprops: {@code bench.warmups} (2), {@code bench.iters} (5),
 * {@code bench.loop} (50).
 */
public class IntegratedBenchmark {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: IntegratedBenchmark <reactorDirName> <cap|full>");
            System.exit(2);
        }
        maybeStartFdWatch();
        String reactorName = args[0];
        int cap = "full".equalsIgnoreCase(args[1]) ? -1 : Integer.parseInt(args[1]);
        int warmups = Integer.getInteger("bench.warmups", 2);
        int iters = Integer.getInteger("bench.iters", 5);
        int loop = Integer.getInteger("bench.loop", 50);

        Path reactorDir = CorpusPaths.reactors().resolve(reactorName);
        List<Path> all = reactorPoms(reactorDir);
        List<Path> selected = selectTier(all);
        if (cap > 0 && selected.size() > cap) {
            selected = capTier(all, cap);
        }
        Path leaf = pinnedLeaf(selected);

        System.out.println("[bench] " + reactorName + " tier=" + (cap < 0 ? "full" : cap) +
                " modules=" + selected.size() + " leaf=" + reactorDir.relativize(leaf) +
                " warmups=" + warmups + " iters=" + iters + " loop=" + loop);
        System.out.println("[wiring] ResolutionEngineSelector from " +
                ResolutionEngineSelector.class.getProtectionDomain().getCodeSource().getLocation());

        for (String engine : new String[]{"legacy", "maven"}) {
            try {
                runMode(engine, reactorName, cap, selected, reactorDir, leaf, warmups, iters, loop);
            } catch (Throwable t) {
                System.out.println("RESULT\tcorpus=" + reactorName + "\ttier=" + (cap < 0 ? "full" : cap) +
                        "\tmodules=" + selected.size() + "\tengine=" + engine + "\tSTATUS=UNAVAILABLE\treason=" +
                        firstLine(String.valueOf(t)));
                System.err.println("[bench] " + engine + " unavailable: " + t);
            }
        }
        System.out.println("[bench] done " + reactorName + " tier=" + (cap < 0 ? "full" : cap));
    }

    private static void runMode(String engine, String reactorName, int cap, List<Path> selected, Path reactorDir,
                                Path leaf, int warmups, int iters, int loop) {
        // Prime once from the network into the shared store (subsequent iterations replay with zero download).
        HttpSender realSender = new HttpUrlConnectionSender(Duration.ofSeconds(10), Duration.ofSeconds(60));
        long primeStart = System.currentTimeMillis();
        parse(selected, reactorDir, engine, RecordingHttpSender.record(CorpusPaths.store(), realSender), fresh());
        System.out.println("[bench] " + engine + " primed in " + (System.currentTimeMillis() - primeStart) + "ms");

        // Warm full-reactor median: fresh pom cache per iteration, zero network (replay).
        List<Long> warm = new ArrayList<>();
        long peakHeap = 0;
        long fdBefore = openFds();
        String profile = null;
        for (int i = 0; i < warmups + iters; i++) {
            MavenPomCache cache = fresh();
            boolean lastMeasured = i == warmups + iters - 1;
            if (lastMeasured) {
                org.openrewrite.maven.internal.engine.EngineProfiler.reset();
            }
            long t0 = System.nanoTime();
            parse(selected, reactorDir, engine, RecordingHttpSender.replay(CorpusPaths.store()), cache);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            if (i >= warmups) {
                warm.add(ms);
            }
            if (lastMeasured && org.openrewrite.maven.internal.engine.EngineProfiler.ENABLED) {
                profile = org.openrewrite.maven.internal.engine.EngineProfiler.report(selected.size());
            }
            peakHeap = Math.max(peakHeap, usedHeapMb());
        }
        long fdAfter = openFds();

        // Warm-cache re-resolution loop, FRESH ctx per iteration: one shared warm pom cache, but a new ExecutionContext
        // each parse (engine handle + DataPool/model cache rebuilt each iteration). Every remote descriptor is a bytes
        // cache hit, so this isolates the cost of reconstructing the engine's per-ctx state.
        MavenPomCache shared = fresh();
        parse(selected, reactorDir, engine, RecordingHttpSender.replay(CorpusPaths.store()), shared); // warm the cache
        List<Long> loopTimes = new ArrayList<>();
        for (int i = 0; i < loop; i++) {
            long t0 = System.nanoTime();
            parse(selected, reactorDir, engine, RecordingHttpSender.replay(CorpusPaths.store()), shared);
            loopTimes.add((System.nanoTime() - t0) / 1_000_000);
        }

        // Warm-cache re-resolution loop, SHARED ctx (the actual UpdateMavenModel steady state — a recipe run reuses one
        // ExecutionContext across every resolution cycle, so the engine's RepositorySystem, session, and DataPool/model
        // cache stay warm). Legacy is ctx-insensitive (its cache is the shared MavenPomCache), so this is a fair mirror.
        MavenPomCache sharedCtxCache = fresh();
        ExecutionContext loopCtx = newCtx(engine, RecordingHttpSender.replay(CorpusPaths.store()), sharedCtxCache);
        parseOn(loopCtx, selected, reactorDir); // warm the ctx
        List<Long> loopCtxTimes = new ArrayList<>();
        String loopProfile = null;
        for (int i = 0; i < loop; i++) {
            boolean lastLoop = i == loop - 1;
            if (lastLoop) {
                org.openrewrite.maven.internal.engine.EngineProfiler.reset();
            }
            long t0 = System.nanoTime();
            parseOn(loopCtx, selected, reactorDir);
            loopCtxTimes.add((System.nanoTime() - t0) / 1_000_000);
            if (lastLoop && org.openrewrite.maven.internal.engine.EngineProfiler.ENABLED) {
                loopProfile = org.openrewrite.maven.internal.engine.EngineProfiler.report(selected.size());
            }
        }

        double warmMedian = median(warm);
        double loopMedian = median(loopTimes);
        double loopCtxMedian = median(loopCtxTimes);
        System.out.println("RESULT\tcorpus=" + reactorName + "\ttier=" + (cap < 0 ? "full" : cap) +
                "\tmodules=" + selected.size() + "\tengine=" + engine +
                "\tSTATUS=OK\twarmMedianMs=" + fmt(warmMedian) +
                "\twarmMsPerModule=" + fmt(warmMedian / selected.size()) +
                "\tloopMedianMs=" + fmt(loopMedian) +
                "\tloopMsPerModule=" + fmt(loopMedian / selected.size()) +
                "\tloopCtxMedianMs=" + fmt(loopCtxMedian) +
                "\tloopCtxMsPerModule=" + fmt(loopCtxMedian / selected.size()) +
                "\tpeakHeapMb=" + peakHeap +
                "\tfdBefore=" + fdBefore + "\tfdAfter=" + fdAfter +
                "\twarm=" + warm + "\tloop=" + loopMedian);
        if (profile != null) {
            System.out.println("PROFILE\tengine=" + engine + "\t" + profile);
        }
        if (loopProfile != null) {
            System.out.println("PROFILE-LOOP\tengine=" + engine + "\t" + loopProfile);
        }
    }

    // Diagnostic: when -Dbench.fdwatch=N, a daemon polls open FDs and, on crossing N, dumps a basename histogram of
    // this process's open files (lsof) then exits — captures exactly what is leaking just before the OS cap.
    private static void maybeStartFdWatch() {
        int threshold = Integer.getInteger("bench.fdwatch", -1);
        if (threshold <= 0) {
            return;
        }
        Thread t = new Thread(() -> {
            String pid = String.valueOf(ProcessHandle.current().pid());
            while (true) {
                long fds = openFds();
                if (fds >= threshold) {
                    try {
                        System.out.println("[fdwatch] FD=" + fds + " >= " + threshold + " — dumping lsof");
                        Process p = new ProcessBuilder("lsof", "-p", pid).redirectErrorStream(true).start();
                        java.util.Map<String, Integer> hist = new java.util.HashMap<>();
                        try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
                            String line;
                            while ((line = r.readLine()) != null) {
                                String[] parts = line.trim().split("\\s+");
                                String name = parts.length > 0 ? parts[parts.length - 1] : line;
                                String key = name.replaceAll("[0-9]+", "N");
                                if (key.length() > 60) {
                                    key = key.substring(key.length() - 60);
                                }
                                hist.merge(key, 1, Integer::sum);
                            }
                        }
                        hist.entrySet().stream()
                                .sorted((a, b) -> b.getValue() - a.getValue())
                                .limit(30)
                                .forEach(e -> System.out.println("[fdwatch] " + e.getValue() + "\t" + e.getKey()));
                    } catch (Exception e) {
                        System.out.println("[fdwatch] dump failed: " + e);
                    }
                    Runtime.getRuntime().halt(7);
                }
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "fdwatch");
        t.setDaemon(true);
        t.start();
    }

    // Open file-descriptor count (Unix), for the FD-leak gate; -1 where unavailable.
    private static long openFds() {
        try {
            java.lang.management.OperatingSystemMXBean os = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            if (os instanceof com.sun.management.UnixOperatingSystemMXBean) {
                return ((com.sun.management.UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount();
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }

    private static void parse(List<Path> poms, Path relativeTo, String engine, HttpSender sender, MavenPomCache cache) {
        parseOn(newCtx(engine, sender, cache), poms, relativeTo);
    }

    private static ExecutionContext newCtx(String engine, HttpSender sender, MavenPomCache cache) {
        ExecutionContext ctx = new InMemoryExecutionContext(t -> { /* errors surface as ParseExceptionResult markers */ });
        ctx.putMessage(ResolutionEngineSelector.ENGINE_KEY, engine);
        HttpSenderExecutionContextView.view(ctx).setHttpSender(sender);
        MavenExecutionContextView mavenCtx = MavenExecutionContextView.view(ctx);
        mavenCtx.setPomCache(cache);
        mavenCtx.setAddLocalRepository(false);
        return ctx;
    }

    private static void parseOn(ExecutionContext ctx, List<Path> poms, Path relativeTo) {
        List<Parser.Input> inputs = poms.stream().map(Parser.Input::fromFile).collect(Collectors.toList());
        List<SourceFile> parsed = MavenParser.builder().build().parseInputs(inputs, relativeTo, ctx)
                .collect(Collectors.toList());
        if (parsed.isEmpty()) {
            throw new IllegalStateException("no source files parsed");
        }
    }

    private static MavenPomCache fresh() {
        return new InMemoryMavenPomCache();
    }

    private static long usedHeapMb() {
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) / (1024 * 1024);
    }

    private static double median(List<Long> values) {
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        if (n == 0) {
            return 0;
        }
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    // All pom-packaging modules are always included (parents/BOMs/aggregators — the shared-parent path); this is the
    // full self-consistent reactor.
    private static List<Path> selectTier(List<Path> all) {
        return all;
    }

    // Cap rule (baseline-compatible): all pom-packaging modules always in, remaining artifact modules fill the cap in
    // path order. Caps are strict supersets, keeping the reactor self-consistent (every selected module's parent/BOM is
    // present).
    private static List<Path> capTier(List<Path> all, int cap) {
        Set<Path> result = new LinkedHashSet<>();
        for (Path p : all) {
            if ("pom".equals(packaging(p))) {
                result.add(p);
            }
        }
        for (Path p : all) {
            if (result.size() >= cap) {
                break;
            }
            result.add(p);
        }
        List<Path> out = new ArrayList<>(result);
        Collections.sort(out);
        return out;
    }

    private static Path pinnedLeaf(List<Path> selected) {
        Path leaf = null;
        for (Path p : selected) {
            if (!"pom".equals(packaging(p))) {
                leaf = p; // last artifact module in path order — deterministic across engines
            }
        }
        return leaf == null ? selected.get(selected.size() - 1) : leaf;
    }

    private static String packaging(Path pom) {
        try {
            String xml = new String(Files.readAllBytes(pom), java.nio.charset.StandardCharsets.UTF_8);
            int i = xml.indexOf("<packaging>");
            if (i < 0) {
                return "jar";
            }
            int j = xml.indexOf("</packaging>", i);
            return xml.substring(i + "<packaging>".length(), j).trim();
        } catch (Exception e) {
            return "jar";
        }
    }

    private static List<Path> reactorPoms(Path reactorDir) throws java.io.IOException {
        if (!Files.isDirectory(reactorDir)) {
            throw new java.io.IOException("Reactor not present: " + reactorDir);
        }
        try (Stream<Path> walk = Files.walk(reactorDir)) {
            return walk.filter(p -> "pom.xml".equals(p.getFileName().toString()))
                    .filter(p -> {
                        String rel = reactorDir.relativize(p).toString();
                        return !rel.contains("src/") && !rel.contains("target/") && !rel.startsWith(".");
                    })
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private static String firstLine(String s) {
        int nl = s.indexOf('\n');
        return nl < 0 ? s : s.substring(0, nl);
    }

    private static String fmt(double d) {
        return String.format(java.util.Locale.ROOT, "%.1f", d);
    }
}
