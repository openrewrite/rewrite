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
        for (int i = 0; i < warmups + iters; i++) {
            MavenPomCache cache = fresh();
            long t0 = System.nanoTime();
            parse(selected, reactorDir, engine, RecordingHttpSender.replay(CorpusPaths.store()), cache);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            if (i >= warmups) {
                warm.add(ms);
            }
            peakHeap = Math.max(peakHeap, usedHeapMb());
        }

        // Warm-cache re-resolution loop: one shared warm pom cache, re-resolve the reactor repeatedly (the
        // UpdateMavenModel steady state — every remote descriptor is a cache hit). Reported per iteration.
        MavenPomCache shared = fresh();
        parse(selected, reactorDir, engine, RecordingHttpSender.replay(CorpusPaths.store()), shared); // warm the cache
        List<Long> loopTimes = new ArrayList<>();
        for (int i = 0; i < loop; i++) {
            long t0 = System.nanoTime();
            parse(selected, reactorDir, engine, RecordingHttpSender.replay(CorpusPaths.store()), shared);
            loopTimes.add((System.nanoTime() - t0) / 1_000_000);
        }

        double warmMedian = median(warm);
        double loopMedian = median(loopTimes);
        System.out.println("RESULT\tcorpus=" + reactorName + "\ttier=" + (cap < 0 ? "full" : cap) +
                "\tmodules=" + selected.size() + "\tengine=" + engine +
                "\tSTATUS=OK\twarmMedianMs=" + fmt(warmMedian) +
                "\twarmMsPerModule=" + fmt(warmMedian / selected.size()) +
                "\tloopMedianMs=" + fmt(loopMedian) +
                "\tloopMsPerModule=" + fmt(loopMedian / selected.size()) +
                "\tpeakHeapMb=" + peakHeap +
                "\twarm=" + warm + "\tloop=" + loopMedian);
    }

    private static void parse(List<Path> poms, Path relativeTo, String engine, HttpSender sender, MavenPomCache cache) {
        ExecutionContext ctx = new InMemoryExecutionContext(t -> { /* errors surface as ParseExceptionResult markers */ });
        ctx.putMessage(ResolutionEngineSelector.ENGINE_KEY, engine);
        HttpSenderExecutionContextView.view(ctx).setHttpSender(sender);
        MavenExecutionContextView mavenCtx = MavenExecutionContextView.view(ctx);
        mavenCtx.setPomCache(cache);
        mavenCtx.setAddLocalRepository(false);
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
