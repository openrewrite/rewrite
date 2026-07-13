package bench;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.io.DefaultModelReader;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.RepositoryCache;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * NEW engine benchmark: the raw Maven pipeline (maven-resolver 2.0.20 + maven-3.9.16 model-builder), unadapted (no
 * tree-type mapping). Per reactor pom: build the effective model with {@link DefaultModelBuilder}, then run verbose
 * dependency collection. Variant N1 = one collect per pom; variant N4 = four per-scope collects per pom.
 */
public class NewEngineBenchmark {

    static final int WARM_WARMUP = 2;
    static final int WARM_MEASURED = 7;
    static final int LOOP_WARMUP = 3;
    static final int LOOP_MEASURED = 50;

    // "test" first: its cold pass downloads everything, so its cold time is the honest N1 cold; the other three
    // per-scope passes then reuse the populated local repo. N4 = all four.
    static final String[] SCOPES = {"test", "compile", "runtime", "provided"};

    public static void main(String[] args) throws Exception {
        Path corpus = Paths.get(System.getProperty("bench.corpus"));
        Path outDir = Paths.get(System.getProperty("bench.out"));
        int maxModules = Integer.getInteger("bench.maxModules", 0);
        String leafName = System.getProperty("bench.leaf", "maven-core");
        boolean skipCold = Boolean.getBoolean("bench.skipCold");
        List<Path> poms = Corpus.selectSubtree(corpus, maxModules);

        Map<String, File> reactor = reactorPool(poms);
        RepositorySystem system = new BenchRepositorySystemSupplier().get();
        List<RemoteRepository> repos = Collections.singletonList(
                new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build());
        RecordingHttpSender sender = recorder();
        DefaultModelBuilder builder = new DefaultModelBuilderFactory().newInstance();

        StringBuilder rpt = new StringBuilder();
        log(rpt, "NEW ENGINE (maven-resolver 2.0.20 + maven-3.9.16 model-builder)");
        log(rpt, "reactor poms(selected)=" + poms.size() + "  reactor GAVs=" + reactor.size()
                + "  cap=" + (maxModules <= 0 ? "full" : maxModules) + "  skipCold=" + skipCold);

        // A single local repo shared by cold+warm: cold (if run) populates it; otherwise the warm-up primes it.
        // Persistent (reused across runs) when bench.localRepo is set, else a fresh temp repo.
        String configuredRepo = System.getProperty("bench.localRepo", "");
        Path coldLocal = configuredRepo.isEmpty()
                ? Files.createTempDirectory("new-local")
                : Files.createDirectories(Paths.get(configuredRepo));
        long coldN1 = -1, coldN4 = -1, coldReq = -1;

        // ---------------- COLD (single live-network run; test pass = N1, all four = N4; skipped for large reactors) ----------------
        if (!skipCold) {
            RepositoryCache coldRepoCache = new DefaultRepositoryCache();
            CountingModelCache coldModelCache = new CountingModelCache();
            long reqBefore = sender.count();
            Result cold = runReactor(system, repos, reactor, builder, poms, coldLocal, coldRepoCache, coldModelCache, sender);
            coldReq = sender.count() - reqBefore;
            coldN1 = cold.modelNanos + cold.collectNanos[0];
            coldN4 = cold.modelNanos + cold.totalCollectNanos();
            log(rpt, String.format("COLD  model=%.1f ms  collect[test/N1]=%.1f ms  collect[all4/N4]=%.1f ms",
                    Stat.ms(cold.modelNanos), Stat.ms(cold.collectNanos[0]), Stat.ms(cold.totalCollectNanos())));
            log(rpt, String.format("COLD  N1 wall=%.1f ms   N4 wall=%.1f ms   requests(shared)=%d   nodes[N1]=%d nodes[N4 sum]=%d  modelFail=%d collectFail=%d",
                    Stat.ms(coldN1), Stat.ms(coldN4), coldReq, cold.nodesTest, cold.nodesAllScopes, cold.modelFail, cold.collectFail));
        } else {
            log(rpt, "COLD  skipped (bench.skipCold): warm-up primes local repo + caches over live network instead");
        }

        // ---------------- WARM (shared RepositoryCache + ModelCache, populated local repo, fresh session/iter) ----------------
        RepositoryCache warmRepoCache = new DefaultRepositoryCache();
        CountingModelCache warmModelCache = new CountingModelCache();
        for (int i = 0; i < WARM_WARMUP; i++) {
            runReactor(system, repos, reactor, builder, poms, coldLocal, warmRepoCache, warmModelCache, sender);
            log(rpt, "WARM  warmup " + (i + 1) + "/" + WARM_WARMUP + " done");
        }
        long[] warmN1 = new long[WARM_MEASURED];
        long[] warmN4 = new long[WARM_MEASURED];
        long warmReqBefore = sender.count();
        Result lastWarm = null;
        for (int i = 0; i < WARM_MEASURED; i++) {
            Result r = runReactor(system, repos, reactor, builder, poms, coldLocal, warmRepoCache, warmModelCache, sender);
            warmN1[i] = r.modelNanos + r.collectNanos[0];
            warmN4[i] = r.modelNanos + r.totalCollectNanos();
            lastWarm = r;
        }
        long warmReq = sender.count() - warmReqBefore;
        log(rpt, "WARM  N1 " + Stat.summary(warmN1));
        log(rpt, "WARM  N4 " + Stat.summary(warmN4));
        log(rpt, "WARM  requests over " + WARM_MEASURED + " iterations = " + warmReq
                + "  modelFail=" + lastWarm.modelFail + " collectFail=" + lastWarm.collectFail);
        // Graph nodes are resolution-structural (identical warm/cold); use warm values for a self-consistent report.
        int graphNodesN1 = lastWarm.nodesTest;
        int graphNodesN4 = lastWarm.nodesAllScopes;

        // ---------------- clone-on-hit instrumentation (one more warm reactor build) ----------------
        long rawBefore = warmModelCache.hits("raw");
        long impBefore = warmModelCache.hits("import");
        runReactor(system, repos, reactor, builder, poms, coldLocal, warmRepoCache, warmModelCache, sender);
        long rawClones = warmModelCache.hits("raw") - rawBefore;
        long impClones = warmModelCache.hits("import") - impBefore;
        log(rpt, "MODELCACHE  per warm reactor build: RAW hits (Model.clone())=" + rawClones
                + "  IMPORT hits (DependencyManagement.clone())=" + impClones
                + "  [misses so far=" + warmModelCache.misses() + "]");

        // ---------------- RE-RESOLUTION LOOP (one leaf module, warm caches, fresh session/iter) ----------------
        Path leaf = Corpus.leaf(poms, leafName);
        List<Path> one = Collections.singletonList(leaf);
        for (int i = 0; i < LOOP_WARMUP; i++) {
            runReactor(system, repos, reactor, builder, one, coldLocal, warmRepoCache, warmModelCache, sender);
        }
        long[] loopN1 = new long[LOOP_MEASURED];
        long[] loopN4 = new long[LOOP_MEASURED];
        for (int i = 0; i < LOOP_MEASURED; i++) {
            Result r = runReactor(system, repos, reactor, builder, one, coldLocal, warmRepoCache, warmModelCache, sender);
            loopN1[i] = r.modelNanos + r.collectNanos[0];
            loopN4[i] = r.modelNanos + r.totalCollectNanos();
        }
        log(rpt, "LOOP  module=" + corpus.relativize(leaf) + "  N1 perIteration " + Stat.summary(loopN1));
        log(rpt, "LOOP  module=" + corpus.relativize(leaf) + "  N4 perIteration " + Stat.summary(loopN4));

        log(rpt, String.format("RESULT new_n1 modules=%d cold_wall_ms=%.1f cold_req=%d warm_median_ms=%.1f warm_min_ms=%.1f warm_max_ms=%.1f loop_median_ms=%.3f nodes=%d raw_clones=%d import_clones=%d",
                poms.size(), Stat.ms(coldN1), coldReq, Stat.ms(Stat.median(warmN1)), Stat.ms(Stat.min(warmN1)), Stat.ms(Stat.max(warmN1)), Stat.ms(Stat.median(loopN1)), graphNodesN1, rawClones, impClones));
        log(rpt, String.format("RESULT new_n4 modules=%d cold_wall_ms=%.1f cold_req=%d warm_median_ms=%.1f warm_min_ms=%.1f warm_max_ms=%.1f loop_median_ms=%.3f nodes=%d raw_clones=%d import_clones=%d",
                poms.size(), Stat.ms(coldN4), coldReq, Stat.ms(Stat.median(warmN4)), Stat.ms(Stat.min(warmN4)), Stat.ms(Stat.max(warmN4)), Stat.ms(Stat.median(loopN4)), graphNodesN4, rawClones, impClones));

        Files.write(outDir.resolve("new-results.txt"), rpt.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** One full pass over the given poms: build each effective model, then collect all four scope variants. */
    static Result runReactor(RepositorySystem system, List<RemoteRepository> repos, Map<String, File> reactor,
                             DefaultModelBuilder builder, List<Path> poms, Path localRepo,
                             RepositoryCache repoCache, CountingModelCache modelCache, RecordingHttpSender sender) {
        Result res = new Result();
        try (CloseableSession session = session(system, localRepo, repoCache, sender)) {
            ReactorModelResolver resolver = new ReactorModelResolver(reactor, system, session, repos);
            for (Path pomPath : poms) {
                Model eff;
                long m0 = System.nanoTime();
                try {
                    eff = buildEffective(builder, pomPath.toFile(), resolver, modelCache);
                } catch (Throwable t) {
                    res.modelFail++;
                    continue;
                }
                res.modelNanos += System.nanoTime() - m0;

                Map<String, String> managedVersions = managedVersionMap(eff);
                List<Dependency> managed = toAether(
                        eff.getDependencyManagement() == null ? Collections.emptyList()
                                : eff.getDependencyManagement().getDependencies(),
                        Collections.emptyMap());
                List<Dependency> allDirect = toAether(eff.getDependencies(), managedVersions);
                Artifact root = new DefaultArtifact(eff.getGroupId(), eff.getArtifactId(), "",
                        extFor(eff.getPackaging()), eff.getVersion());

                for (int s = 0; s < SCOPES.length; s++) {
                    List<Dependency> direct = directForScope(allDirect, SCOPES[s]);
                    long c0 = System.nanoTime();
                    try {
                        CollectResult cr = collect(system, session, repos, root, direct, managed);
                        int nodes = countNodes(cr.getRoot());
                        if (s == 0) {
                            res.nodesTest += nodes;
                        }
                        res.nodesAllScopes += nodes;
                    } catch (Throwable t) {
                        res.collectFail++;
                    }
                    res.collectNanos[s] += System.nanoTime() - c0;
                }
            }
        }
        return res;
    }

    static Model buildEffective(DefaultModelBuilder builder, File pom, ReactorModelResolver resolver,
                                CountingModelCache modelCache) throws Exception {
        DefaultModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.setModelSource(new FileModelSource(pom));
        req.setPomFile(pom);
        req.setModelResolver(resolver);
        req.setModelCache(modelCache);
        req.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        req.setLocationTracking(true);
        req.setTwoPhaseBuilding(false);
        req.setSystemProperties(copyOf(System.getProperties()));
        req.setUserProperties(new Properties());
        ModelBuildingResult r = builder.build(req);
        return r.getEffectiveModel();
    }

    static CollectResult collect(RepositorySystem system, CloseableSession session, List<RemoteRepository> repos,
                                 Artifact root, List<Dependency> direct, List<Dependency> managed) throws Exception {
        CollectRequest cr = new CollectRequest();
        cr.setRootArtifact(root);
        cr.setDependencies(direct);
        cr.setManagedDependencies(managed);
        cr.setRepositories(repos);
        return system.collectDependencies(session, cr);
    }

    static CloseableSession session(RepositorySystem system, Path localRepo, RepositoryCache repoCache, HttpSender sender) {
        return new SessionBuilderSupplier(system).get()
                .withLocalRepositoryBaseDirectories(localRepo)
                .setCache(repoCache)
                .setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, ConflictResolver.Verbosity.STANDARD)
                .setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, Boolean.TRUE)
                .setConfigProperty(HttpSenderTransporterFactory.HTTP_SENDER_KEY, sender)
                .setSystemProperties(System.getProperties())
                .build();
    }

    static Map<String, File> reactorPool(List<Path> poms) throws Exception {
        DefaultModelReader reader = new DefaultModelReader();
        Map<String, File> pool = new LinkedHashMap<>();
        for (Path p : poms) {
            Model m = reader.read(p.toFile(), null);
            String g = m.getGroupId() != null ? m.getGroupId()
                    : (m.getParent() != null ? m.getParent().getGroupId() : null);
            String v = m.getVersion() != null ? m.getVersion()
                    : (m.getParent() != null ? m.getParent().getVersion() : null);
            if (g != null && v != null && m.getArtifactId() != null) {
                pool.put(g + ":" + m.getArtifactId() + ":" + v, p.toFile());
            }
        }
        return pool;
    }

    static Map<String, String> managedVersionMap(Model eff) {
        Map<String, String> map = new LinkedHashMap<>();
        if (eff.getDependencyManagement() != null) {
            for (org.apache.maven.model.Dependency d : eff.getDependencyManagement().getDependencies()) {
                if (d.getVersion() != null) {
                    map.put(d.getGroupId() + ":" + d.getArtifactId(), d.getVersion());
                }
            }
        }
        return map;
    }

    static List<Dependency> toAether(List<org.apache.maven.model.Dependency> deps, Map<String, String> managedVersions) {
        List<Dependency> out = new ArrayList<>();
        for (org.apache.maven.model.Dependency d : deps) {
            String v = d.getVersion();
            if (v == null || v.isEmpty()) {
                v = managedVersions.get(d.getGroupId() + ":" + d.getArtifactId());
            }
            if (v == null || v.isEmpty()) {
                continue; // unresolvable without a version; unadapted pipeline skips it
            }
            String type = d.getType() == null ? "jar" : d.getType();
            String classifier = d.getClassifier() != null ? d.getClassifier() : classifierFor(type);
            Artifact art = new DefaultArtifact(d.getGroupId(), d.getArtifactId(), classifier, extFor(type), v);
            List<Exclusion> exclusions = new ArrayList<>();
            for (org.apache.maven.model.Exclusion ex : d.getExclusions()) {
                exclusions.add(new Exclusion(ex.getGroupId(), ex.getArtifactId(), "*", "*"));
            }
            String scope = d.getScope() == null ? "compile" : d.getScope();
            out.add(new Dependency(art, scope, d.isOptional(), exclusions));
        }
        return out;
    }

    /** Maven scope -> classpath visibility, mirroring how rewrite resolves each of the four scopes separately. */
    static List<Dependency> directForScope(List<Dependency> all, String targetScope) {
        List<Dependency> out = new ArrayList<>();
        for (Dependency d : all) {
            if (scopeVisible(targetScope, d.getScope())) {
                out.add(d);
            }
        }
        return out;
    }

    static boolean scopeVisible(String target, String depScope) {
        switch (target) {
            case "test":
                return true;
            case "runtime":
                return "compile".equals(depScope) || "runtime".equals(depScope);
            case "compile":
            case "provided":
                return "compile".equals(depScope) || "provided".equals(depScope) || "system".equals(depScope);
            default:
                return true;
        }
    }

    static int countNodes(DependencyNode root) {
        int n = 0;
        for (DependencyNode c : root.getChildren()) {
            n += 1 + countNodes(c);
        }
        return n;
    }

    static String extFor(String type) {
        switch (type == null ? "jar" : type) {
            case "pom":
                return "pom";
            case "war":
                return "war";
            case "ear":
                return "ear";
            case "test-jar":
            case "ejb":
            case "bundle":
            case "maven-plugin":
            case "jar":
                return "jar";
            default:
                return type;
        }
    }

    static String classifierFor(String type) {
        return "test-jar".equals(type) ? "tests" : "";
    }

    static Properties copyOf(Properties src) {
        Properties p = new Properties();
        p.putAll(src);
        return p;
    }

    static RecordingHttpSender recorder() {
        return new RecordingHttpSender(new HttpUrlConnectionSender(Duration.ofSeconds(30), Duration.ofSeconds(60)));
    }

    static void log(StringBuilder rpt, String line) {
        rpt.append(line).append('\n');
        System.out.println(line); // live per-phase progress (long large-reactor runs must not go silent)
        System.out.flush();
    }

    static final class Result {
        long modelNanos;
        final long[] collectNanos = new long[SCOPES.length];
        int nodesTest;
        int nodesAllScopes;
        int modelFail;
        int collectFail;

        long totalCollectNanos() {
            long t = 0;
            for (long v : collectNanos) {
                t += v;
            }
            return t;
        }
    }
}
