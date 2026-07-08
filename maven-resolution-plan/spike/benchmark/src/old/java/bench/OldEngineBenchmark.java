package bench;

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
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.tree.ParseError;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OLD engine benchmark: rewrite-maven's own resolution ({@link MavenParser} + {@link InMemoryMavenPomCache}).
 * Scenarios: cold full reactor, warm full reactor (median of measured iterations), warm single-module re-resolution.
 */
public class OldEngineBenchmark {

    static final int WARM_WARMUP = 2;
    static final int WARM_MEASURED = 7;
    static final int LOOP_WARMUP = 3;
    static final int LOOP_MEASURED = 50;

    public static void main(String[] args) throws Exception {
        Path corpus = Paths.get(System.getProperty("bench.corpus"));
        Path outDir = Paths.get(System.getProperty("bench.out"));
        int maxModules = Integer.getInteger("bench.maxModules", 0);
        String leafName = System.getProperty("bench.leaf", "maven-core");
        boolean skipCold = Boolean.getBoolean("bench.skipCold");
        List<Path> poms = Corpus.selectSubtree(corpus, maxModules);
        MavenParser parser = MavenParser.builder().build();

        StringBuilder rpt = new StringBuilder();
        log(rpt, "OLD ENGINE (rewrite-maven)");
        log(rpt, "reactor poms(selected)=" + poms.size() + "  cap=" + (maxModules <= 0 ? "full" : maxModules)
                + "  skipCold=" + skipCold);

        Path coldLocal = localRepo("old");
        long coldWall = -1, coldReq = -1;
        int coldNodes = -1, coldCompile = -1, failures = -1;

        // ---------------- COLD (single live-network shot; skipped for very large reactors) ----------------
        if (!skipCold) {
            InMemoryMavenPomCache coldCache = new InMemoryMavenPomCache();
            RecordingHttpSender coldSender = recorder();
            long t0 = System.nanoTime();
            List<SourceFile> coldParsed = parse(parser, poms, corpus, ctx(coldCache, coldLocal, coldSender));
            coldWall = System.nanoTime() - t0;
            coldReq = coldSender.count();
            coldNodes = countResolvedDeps(coldParsed);
            coldCompile = countScope(coldParsed, Scope.Compile);
            failures = countFailures(coldParsed);
            log(rpt, String.format("COLD  wall=%.1f ms  requests=%d  resolvedDeps(all scopes)=%d  compileScope=%d  parseFailures=%d",
                    Stat.ms(coldWall), coldReq, coldNodes, coldCompile, failures));
        } else {
            log(rpt, "COLD  skipped (bench.skipCold): warm-up primes caches over live network instead");
        }

        // ---------------- WARM (shared warm pom cache, fresh ctx each iteration) ----------------
        InMemoryMavenPomCache warmCache = new InMemoryMavenPomCache();
        for (int i = 0; i < WARM_WARMUP; i++) {
            parse(parser, poms, corpus, ctx(warmCache, coldLocal, recorder()));
            log(rpt, "WARM  warmup " + (i + 1) + "/" + WARM_WARMUP + " done");
        }
        long[] warm = new long[WARM_MEASURED];
        RecordingHttpSender warmSender = null;
        List<SourceFile> lastWarm = null;
        for (int i = 0; i < WARM_MEASURED; i++) {
            warmSender = recorder();
            ExecutionContext c = ctx(warmCache, coldLocal, warmSender);
            long s = System.nanoTime();
            lastWarm = parse(parser, poms, corpus, c);
            warm[i] = System.nanoTime() - s;
        }
        log(rpt, "WARM  " + Stat.summary(warm) + "  requests(last iter)=" + (warmSender == null ? -1 : warmSender.count()));
        // Graph nodes are resolution-structural (identical warm/cold); derive from warm when cold was skipped.
        if (coldNodes < 0 && lastWarm != null) {
            coldNodes = countResolvedDeps(lastWarm);
            coldCompile = countScope(lastWarm, Scope.Compile);
            failures = countFailures(lastWarm);
            log(rpt, String.format("WARM  resolvedDeps(all scopes)=%d  compileScope=%d  parseFailures=%d",
                    coldNodes, coldCompile, failures));
        }

        // ---------------- RE-RESOLUTION LOOP (one leaf module, warm cache, repeated) ----------------
        Path leaf = Corpus.leaf(poms, leafName);
        List<Path> one = java.util.Collections.singletonList(leaf);
        for (int i = 0; i < LOOP_WARMUP; i++) {
            parse(parser, one, corpus, ctx(warmCache, coldLocal, recorder()));
        }
        long[] loop = new long[LOOP_MEASURED];
        for (int i = 0; i < LOOP_MEASURED; i++) {
            ExecutionContext c = ctx(warmCache, coldLocal, recorder());
            long s = System.nanoTime();
            parse(parser, one, corpus, c);
            loop[i] = System.nanoTime() - s;
        }
        log(rpt, "LOOP  module=" + corpus.relativize(leaf) + "  perIteration " + Stat.summary(loop));

        // Machine-readable summary line for RESULTS aggregation.
        log(rpt, String.format("RESULT old modules=%d cold_wall_ms=%.1f cold_req=%d warm_median_ms=%.1f warm_min_ms=%.1f warm_max_ms=%.1f loop_median_ms=%.3f nodes_all=%d nodes_compile=%d",
                poms.size(), Stat.ms(coldWall), coldReq, Stat.ms(Stat.median(warm)), Stat.ms(Stat.min(warm)), Stat.ms(Stat.max(warm)), Stat.ms(Stat.median(loop)), coldNodes, coldCompile));

        Files.write(outDir.resolve("old-results.txt"), rpt.toString().getBytes(StandardCharsets.UTF_8));
    }

    static List<SourceFile> parse(MavenParser parser, List<Path> poms, Path relativeTo, ExecutionContext ctx) {
        List<Parser.Input> inputs = poms.stream().map(Parser.Input::fromFile).collect(Collectors.toList());
        return parser.parseInputs(inputs, relativeTo, ctx).collect(Collectors.toList());
    }

    static ExecutionContext ctx(MavenPomCache cache, Path localRepo, HttpSender sender) {
        ExecutionContext ctx = new InMemoryExecutionContext(t -> { /* count via markers, swallow */ });
        MavenExecutionContextView mv = MavenExecutionContextView.view(ctx);
        mv.setPomCache(cache);
        // Point at an empty temp local repo so COLD is genuinely cold (never the user's ~/.m2).
        mv.setLocalRepository(MavenRepository.MAVEN_LOCAL_DEFAULT.withUri(localRepo.toUri().toString()));
        HttpSenderExecutionContextView.view(ctx).setHttpSender(sender);
        return ctx;
    }

    static RecordingHttpSender recorder() {
        return new RecordingHttpSender(new HttpUrlConnectionSender(Duration.ofSeconds(30), Duration.ofSeconds(60)));
    }

    /** A persistent local repo (reused across runs) if bench.localRepo is set, else a fresh temp repo. */
    static Path localRepo(String prefix) throws java.io.IOException {
        String configured = System.getProperty("bench.localRepo", "");
        return configured.isEmpty()
                ? Files.createTempDirectory(prefix + "-local")
                : Files.createDirectories(Paths.get(configured));
    }

    static int countResolvedDeps(List<SourceFile> parsed) {
        int n = 0;
        for (SourceFile sf : parsed) {
            MavenResolutionResult mrr = sf.getMarkers().findFirst(MavenResolutionResult.class).orElse(null);
            if (mrr != null) {
                for (Map.Entry<Scope, List<ResolvedDependency>> e : mrr.getDependencies().entrySet()) {
                    n += e.getValue().size();
                }
            }
        }
        return n;
    }

    static int countScope(List<SourceFile> parsed, Scope scope) {
        int n = 0;
        for (SourceFile sf : parsed) {
            MavenResolutionResult mrr = sf.getMarkers().findFirst(MavenResolutionResult.class).orElse(null);
            if (mrr != null) {
                List<ResolvedDependency> l = mrr.getDependencies().get(scope);
                if (l != null) {
                    n += l.size();
                }
            }
        }
        return n;
    }

    static int countFailures(List<SourceFile> parsed) {
        int n = 0;
        for (SourceFile sf : parsed) {
            if (sf instanceof ParseError || sf.getMarkers().findFirst(org.openrewrite.ParseExceptionResult.class).isPresent()) {
                n++;
            }
        }
        return n;
    }

    static void log(StringBuilder rpt, String line) {
        rpt.append(line).append('\n');
        System.out.println(line); // live per-phase progress (long large-reactor runs must not go silent)
        System.out.flush();
    }
}
