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
package org.openrewrite.maven.internal.engine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenDownloadingExceptions;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.engine.MavenEngine;
import org.openrewrite.maven.engine.SessionConfig;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Model;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Repository;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.RepositoryPolicy;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystemSession;
import org.openrewrite.maven.internal.MavenParsingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.ResolutionEngineSelector;
import org.openrewrite.maven.internal.ResolutionEngineSelector.Engine;
import org.openrewrite.maven.internal.parity.ResolutionDiff;
import org.openrewrite.maven.internal.parity.ResolutionSnapshot;
import org.openrewrite.maven.internal.parity.SnapshotNormalizer;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedManagedDependency;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.maven.tree.Scope;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * Routes effective-POM construction between the legacy resolver and the shaded Maven engine, per the dev/CI
 * {@link ResolutionEngineSelector}. LEGACY runs the caller's existing path untouched; MAVEN builds the effective pom on
 * {@link EngineEffectivePom} + {@link EffectivePomMapper}; SHADOW runs both, diffs the {@code $.pom} snapshot section with
 * the ledgered {@code parity/masks.txt} applied, throws an {@link AssertionError} on any unexplained difference, and
 * returns the legacy result (shadow must never change behavior). Dependency resolution stays legacy in every mode
 * (Phase 3).
 */
public final class MavenEngineResolution {

    private static final String ENGINE_HANDLE_KEY = "org.openrewrite.maven.internal.engine.handle";
    private static final String COLLECTOR_HANDLE_KEY = "org.openrewrite.maven.internal.engine.collector";
    private static final String EFFECTIVE_MEMO_KEY = "org.openrewrite.maven.internal.engine.effectiveMemo";

    // The legacy resolver resolves imported BOMs and parents through nested Pom.resolve calls (ResolvedPom line ~941);
    // those are internal to the legacy algorithm and have no engine equivalent (the engine imports BOMs inside Maven's
    // ModelBuilder). Only the outermost facade call dispatches; re-entrant calls run the legacy path directly.
    private static final ThreadLocal<Boolean> DISPATCHING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    // Dependency-graph analogue of DISPATCHING: the legacy per-scope pass (and the engine's own internal resolves,
    // plus the shadow legacy re-run) must not re-enter the graph facade and double-collect/double-compare. Only the
    // outermost dependencyGraph/dependencyGraphScope call dispatches.
    private static final ThreadLocal<Boolean> DISPATCHING_DEPS = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private MavenEngineResolution() {
    }

    /** A legacy effective-pom computation, deferred so MAVEN mode never runs it. */
    @FunctionalInterface
    public interface LegacyResolution {
        ResolvedPom resolve() throws MavenDownloadingException;
    }

    /** An action run with the engine suppressed (legacy-only); see {@link #withoutEngine}. */
    @FunctionalInterface
    public interface SuppressedAction<T, E extends Throwable> {
        T run() throws E;
    }

    /**
     * Runs {@code action} with engine dispatch suppressed, so every nested {@link #effectivePom} call takes the legacy
     * path. Dependency resolution — and the imported-BOM/parent effective-poms it builds internally — stays 100% legacy
     * in every mode (Phase 2 routes only the project effective pom through the engine); callers wrap that pass with this.
     */
    public static <T, E extends Throwable> T withoutEngine(SuppressedAction<T, E> action) throws E {
        boolean previous = DISPATCHING.get();
        DISPATCHING.set(Boolean.TRUE);
        try {
            return action.run();
        } finally {
            DISPATCHING.set(previous);
        }
    }

    public static ResolvedPom effectivePom(Pom requested, Iterable<String> activeProfiles,
                                           MavenPomDownloader downloader, ExecutionContext ctx,
                                           LegacyResolution legacy) throws MavenDownloadingException {
        Engine engine = ResolutionEngineSelector.select(ctx);
        if (engine == Engine.LEGACY || DISPATCHING.get()) {
            return legacy.resolve();
        }
        DISPATCHING.set(Boolean.TRUE);
        try {
            if (engine == Engine.MAVEN) {
                return engineEffectivePom(requested, activeProfiles, downloader, ctx);
            }
            // SHADOW: run both, diff, return legacy (never change behavior).
            Attempt legacyAttempt = attempt(legacy::resolve);
            Attempt engineAttempt = attempt(() -> engineEffectivePom(requested, activeProfiles, downloader, ctx));
            assertShadowParity(requested, toList(activeProfiles), PomXmlRegistry.injectedProperties(ctx), legacyAttempt, engineAttempt);
            if (legacyAttempt.failure != null) {
                throw legacyAttempt.rethrow();
            }
            return requireNonNull(legacyAttempt.result);
        } finally {
            DISPATCHING.set(Boolean.FALSE);
        }
    }

    private static ResolvedPom engineEffectivePom(Pom requested, Iterable<String> activeProfiles,
                                                  MavenPomDownloader downloader, ExecutionContext ctx) throws MavenDownloadingException {
        return buildEngineEffective(requested, toList(activeProfiles), downloader, ctx).pom;
    }

    // The engine's effective pom plus the shaded Maven Model and settings the dependency collector needs. The effective
    // model already had every scope's direct deps DM-injected by model building; the collector seeds a single verbose
    // collect from it (DESIGN §4.2). Model builds are served warm from the bytes cache, so rebuilding here (rather than
    // stashing the Model on the frozen ResolvedPom) stays cheap.
    private static EngineEffective buildEngineEffective(Pom requested, List<String> profiles,
                                                        MavenPomDownloader downloader, ExecutionContext ctx) throws MavenDownloadingException {
        MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
        Map<String, String> injected = PomXmlRegistry.injectedProperties(ctx);

        byte[] xml = PomXmlRegistry.get(ctx, requested);
        if (xml == null) {
            // No registry bytes match this requested pom: a synthetic Pom.builder() graph (rewrite-gradle), or a recipe
            // that mutated the requested pom and re-resolved without refreshing the registry. Print from the converter.
            xml = new PomToModelConverter().toXml(requested);
        }
        xml = ensureModelVersion(xml);

        // Effective-model memo (DESIGN §6): the mapped effective result is a pure function of the requested pom's bytes,
        // the active profiles, and the reactor epoch. Keying on all three lets repeated builds of the same module skip
        // ModelBuilder entirely — the resolution facade and the dependency-graph facade each build the same module's
        // effective pom, and a steady-state re-resolution loop rebuilds every module every cycle. The global epoch (bumped
        // by every UpdateMavenModel re-resolution) invalidates the whole memo when any reactor member mutates, so a mutated
        // parent is never served stale to an inheriting sibling; remote-only builds share one epoch and stay warm.
        Cache<String, EngineEffective> memo = effectiveMemo(ctx);
        String memoKey = effectiveMemoKey(PomXmlRegistry.epoch(ctx), profiles, requested, xml);
        EngineEffective memoized = memo.getIfPresent(memoKey);
        if (memoized != null) {
            if (EngineProfiler.ENABLED) {
                EngineProfiler.effectiveMemoHits.incrementAndGet();
            }
            return memoized;
        }
        if (EngineProfiler.ENABLED) {
            EngineProfiler.effectiveMemoMisses.incrementAndGet();
        }

        ReactorWorkspace reactor = new ReactorWorkspace(
                downloader.getProjectPoms(), PomXmlRegistry.pathSource(ctx), PomXmlRegistry.epoch(ctx));

        MavenSettings settings = downloader.getMavenSettings() != null ? downloader.getMavenSettings() : mctx.getSettings();
        SettingsBridge bridge = new SettingsBridge(mctx, settings, profiles);
        List<MavenRepository> requestRepositories = bridge.requestRepositories(
                requested.getEffectiveRepositories(),
                SettingsBridge.addLocalRepository(mctx),
                SettingsBridge.addCentralRepository(mctx),
                requested.getProperties());
        EffectiveSettings effectiveSettings = bridge.effectiveSettings(injected);

        EngineHandle handle = handle(ctx);
        // Root project build: a fresh per-build model cache (null store) so its own servedBy is complete — the mapper
        // reads this build's attribution directly, not an accumulated one.
        EngineEffectivePom service = new EngineEffectivePom(
                handle.engine.getRepositorySystem(), handle.session, requestRepositories, null);
        EngineModelBuildingOutcome outcome = service.build(xml, requested, effectiveSettings, reactor, ctx);
        if (!outcome.isSuccess()) {
            throw rethrow(outcome.getFailure());
        }
        BomGavAttributor attributor = new BomGavAttributor(service, effectiveSettings, reactor, ctx, mctx.getPomCache());
        EffectivePomMapper mapper = new EffectivePomMapper(mctx.getPomCache(), attributor, reactor);
        ResolvedPom pom = mapper.map(outcome, requested, profiles, injected);
        Model effectiveModel = requireNonNull(outcome.getResult()).getEffectiveModel();
        // The collect resolves transitives through the parent-merged effective repositories (declared order), so a
        // pom that inherits a repository from its parent attributes bytes to it exactly as legacy does — legacy's
        // download walks the resolved pom's parent-merged repositories, not the requested pom's own list (NEW-3: spark
        // declares gcs-maven-central-mirror before central in its parent, so bytes are attributed to the GCS mirror).
        List<MavenRepository> collectRepositories = bridge.requestRepositories(
                modelRepositories(effectiveModel), SettingsBridge.addLocalRepository(mctx),
                SettingsBridge.addCentralRepository(mctx), requested.getProperties());
        EngineEffective built = new EngineEffective(pom, effectiveModel, requestRepositories, collectRepositories, effectiveSettings, reactor);
        memo.put(memoKey, built);
        return built;
    }

    @SuppressWarnings("unchecked")
    private static Cache<String, EngineEffective> effectiveMemo(ExecutionContext ctx) {
        return ctx.computeMessageIfAbsent(EFFECTIVE_MEMO_KEY,
                k -> Caffeine.newBuilder().maximumSize(4096).recordStats().<String, EngineEffective>build());
    }

    /** Memo hit/miss stats for the perf-correctness gate ({@code EngineReResolutionTest}). */
    static CacheStats effectiveMemoStats(ExecutionContext ctx) {
        return effectiveMemo(ctx).stats();
    }

    // The memo identity: reactor epoch + active profiles + the requested pom's gav and exact bytes. The gav keeps distinct
    // modules apart at one epoch; the byte length + hash guards the rare synthetic graph that shares a gav, so a memo hit
    // only ever returns a result built from byte-identical input under the same profiles and epoch.
    private static String effectiveMemoKey(int epoch, List<String> profiles, Pom requested, byte[] xml) {
        return epoch + "|" + String.join(",", profiles) + "|" + requested.getGav() + "|" +
                xml.length + ":" + Arrays.hashCode(xml);
    }

    // The parent-merged declared repositories the effective model carries, in declaration order (the request universe
    // the collect resolves and attributes bytes through). Unlike EffectivePomMapper's projected list this is unfiltered
    // by super-POM provenance — it is a resolution input, not output shape.
    private static List<MavenRepository> modelRepositories(Model effectiveModel) {
        List<MavenRepository> repositories = new ArrayList<>();
        for (Repository repo : effectiveModel.getRepositories()) {
            // Drop the canonical Maven Central (super-POM injected, or a declared central pointing at it): it is re-added
            // by the addCentralRepository seam, and letting it into the request universe would clobber a ctx-injected
            // repo sharing the id 'central' (e.g. a mock/mirror central).
            if (isCanonicalCentral(repo.getUrl())) {
                continue;
            }
            repositories.add(new MavenRepository(repo.getId(), repo.getUrl(),
                    enabled(repo.getReleases()), enabled(repo.getSnapshots()), false, null, null, null, null));
        }
        return repositories;
    }

    private static boolean isCanonicalCentral(@Nullable String url) {
        if (url == null) {
            return false;
        }
        String normalized = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return normalized.equals(MavenRepository.MAVEN_CENTRAL.getUri());
    }

    private static @Nullable String enabled(@Nullable RepositoryPolicy policy) {
        return policy == null ? null : policy.getEnabled();
    }

    private static final class EngineEffective {
        final ResolvedPom pom;
        final Model effectiveModel;
        final List<MavenRepository> requestRepositories;
        final List<MavenRepository> collectRepositories;
        final EffectiveSettings effectiveSettings;
        final ReactorWorkspace reactor;

        EngineEffective(ResolvedPom pom, Model effectiveModel, List<MavenRepository> requestRepositories,
                        List<MavenRepository> collectRepositories, EffectiveSettings effectiveSettings, ReactorWorkspace reactor) {
            this.pom = pom;
            this.effectiveModel = effectiveModel;
            this.requestRepositories = requestRepositories;
            this.collectRepositories = collectRepositories;
            this.effectiveSettings = effectiveSettings;
            this.reactor = reactor;
        }
    }

    // --- dependency-graph routing ---------------------------------------------------------------------------------

    /** A legacy all-scopes dependency resolution, deferred so MAVEN mode never runs it. */
    @FunctionalInterface
    public interface LegacyDependencyResolution {
        Map<Scope, List<ResolvedDependency>> resolve() throws MavenDownloadingExceptions;
    }

    /** A legacy single-scope dependency resolution (the external per-scope entry point). */
    @FunctionalInterface
    public interface LegacyScopeResolution {
        List<ResolvedDependency> resolve() throws MavenDownloadingExceptions;
    }

    /**
     * Routes the all-scopes dependency graph. LEGACY runs the caller's per-scope legacy pass untouched; MAVEN runs a
     * single verbose collect and projects all four scopes ({@link EngineDependencyCollector} + {@link DependencyGraphMapper});
     * SHADOW runs both, diffs the full {@code pom}+{@code scopes}+{@code errors} snapshot with the ledgered masks, throws
     * on any unexplained difference, and returns the legacy result. Direct-dependency failures preserve the
     * {@code partialResult} contract (L-P0-004) on either path.
     */
    public static Map<Scope, List<ResolvedDependency>> dependencyGraph(
            ResolvedPom legacyPom, Iterable<String> activeProfiles, MavenPomDownloader downloader, ExecutionContext ctx,
            LegacyDependencyResolution legacy) throws MavenDownloadingExceptions {
        Engine engine = ResolutionEngineSelector.select(ctx);
        if (engine == Engine.LEGACY || DISPATCHING_DEPS.get()) {
            return legacy.resolve();
        }
        DISPATCHING_DEPS.set(Boolean.TRUE);
        try {
            Pom requested = legacyPom.getRequested();
            List<String> profiles = toList(activeProfiles);
            if (engine == Engine.MAVEN) {
                EngineGraph graph;
                try {
                    graph = attemptEngineGraph(legacyPom, profiles, downloader, ctx);
                } catch (MavenDownloadingException e) {
                    throw MavenDownloadingExceptions.append(null, e);
                }
                if (graph.failure != null) {
                    throw graph.failure;
                }
                return graph.dependencies;
            }
            // SHADOW: run both, diff, return legacy (never change behavior).
            DepAttempt legacyAttempt = attemptDeps(legacy);
            EngineGraph engineGraph;
            try {
                engineGraph = attemptEngineGraph(legacyPom, profiles, downloader, ctx);
            } catch (MavenDownloadingException | MavenParsingException e) {
                // The engine's effective-pom rebuild threw. When Maven is stricter than rewrite's lenient parser
                // (parent-packaging, self-parent, expression-cycle, ...) the effective-pom shadow already ledgered the
                // outcome and returned legacy; there is no engine graph to compare, so mirror that here.
                String category = strictnessCategory(e.getMessage());
                if (category != null && isOutcomeMaskLedgered(category)) {
                    // Ledgered Maven-stricter outcome. Return legacy's outcome — which may itself be a (deferred)
                    // failure, e.g. a missing dependency version legacy tolerates at model build then fails at download.
                    if (legacyAttempt.failure != null) {
                        throw legacyAttempt.failure;
                    }
                    return requireNonNull(legacyAttempt.result);
                }
                throw new AssertionError("Shadow dependency resolution: engine effective-pom rebuild failed for " +
                        requested.getGav() + ": " + firstLine(e.getMessage()), e);
            }
            assertShadowDependencyParity(legacyPom, profiles, legacyAttempt, engineGraph);
            if (legacyAttempt.failure != null) {
                throw legacyAttempt.failure;
            }
            return requireNonNull(legacyAttempt.result);
        } finally {
            DISPATCHING_DEPS.set(Boolean.FALSE);
        }
    }

    /**
     * Routes a single scope's projection for external callers (rewrite-gradle). MAVEN projects just this scope from the
     * one verbose collect; LEGACY and SHADOW defer to the legacy pass (the all-scopes {@link #dependencyGraph} carries the
     * shadow census). Re-entrant calls under an active all-scopes dispatch run legacy directly.
     */
    public static List<ResolvedDependency> dependencyGraphScope(
            ResolvedPom pom, Scope scope, MavenPomDownloader downloader, ExecutionContext ctx,
            LegacyScopeResolution legacy) throws MavenDownloadingExceptions {
        Engine engine = ResolutionEngineSelector.select(ctx);
        if (engine != Engine.MAVEN || DISPATCHING_DEPS.get()) {
            return legacy.resolve();
        }
        DISPATCHING_DEPS.set(Boolean.TRUE);
        try {
            EngineGraph graph;
            try {
                graph = attemptEngineGraph(pom, toList(pom.getActiveProfiles()), downloader, ctx);
            } catch (MavenDownloadingException e) {
                throw MavenDownloadingExceptions.append(null, e);
            }
            if (graph.dependencies.containsKey(scope)) {
                return graph.dependencies.get(scope);
            }
            if (graph.failure != null) {
                throw graph.failure;
            }
            return new ArrayList<>();
        } finally {
            DISPATCHING_DEPS.set(Boolean.FALSE);
        }
    }

    // Builds the engine effective model, runs one verbose collect, and projects all four scopes. A direct-dependency
    // failure is captured (with the partial projection) rather than thrown, so shadow can compare both outcomes.
    private static EngineGraph attemptEngineGraph(ResolvedPom callerPom, List<String> profiles,
                                                  MavenPomDownloader downloader, ExecutionContext ctx) throws MavenDownloadingException {
        Pom requested = callerPom.getRequested();
        long t0 = EngineProfiler.ENABLED ? System.nanoTime() : 0;
        EngineEffective effective = buildEngineEffective(requested, profiles, downloader, ctx);
        if (EngineProfiler.ENABLED) {
            EngineProfiler.effectiveNanos.addAndGet(System.nanoTime() - t0);
        }
        // ResolvedPom.resolve's no-change contract: a value-unchanged re-resolution returns the caller's instance, whose
        // requested/requestedBom thread the PREVIOUS requested pom's declarations (UpdateMavenModel swaps a re-mapped
        // requested onto it). Thread against the caller's pom whenever the engine's threading inputs are value-equal, so
        // consumers' reference lookups — and the shadow's requestedRef — see exactly what legacy leaves in place.
        ResolvedPom enginePom = threadingEqual(effective.pom, callerPom) ? callerPom : effective.pom;
        CollectorHandle handle = collectorHandle(ctx);
        long tCollect = EngineProfiler.ENABLED ? System.nanoTime() : 0;
        EngineCollectOutcome outcome = handle.collector.collect(effective.effectiveModel, requested,
                effective.collectRepositories, effective.effectiveSettings, effective.reactor, handle.scratch, ctx);
        if (EngineProfiler.ENABLED) {
            EngineProfiler.collectNanos.addAndGet(System.nanoTime() - tCollect);
        }
        DependencyGraphMapper mapper = new DependencyGraphMapper(MavenExecutionContextView.view(ctx).getPomCache());
        long tProject = EngineProfiler.ENABLED ? System.nanoTime() : 0;
        try {
            EngineGraph g = new EngineGraph(enginePom, mapper.map(outcome, enginePom, requested, ctx), null);
            if (EngineProfiler.ENABLED) {
                EngineProfiler.projectNanos.addAndGet(System.nanoTime() - tProject);
            }
            return g;
        } catch (MavenDownloadingExceptions e) {
            Map<Scope, List<ResolvedDependency>> partial = e.getPartialResult() == null ?
                    new LinkedHashMap<>() : e.getPartialResult().getDependencies();
            return new EngineGraph(enginePom, partial, e);
        }
    }

    // The dependency-threading inputs (requested dependencies + managed coordinates) compared by value; managed entries
    // align by g:a:classifier:type (unique on both sides) and ignore the resolved repository spelling and the threaded
    // requested/requestedBom instances, so a value-unchanged re-resolution reuses the caller's pom exactly as legacy does.
    private static boolean threadingEqual(ResolvedPom engine, ResolvedPom caller) {
        if (!engine.getRequestedDependencies().equals(caller.getRequestedDependencies())) {
            return false;
        }
        List<ResolvedManagedDependency> a = engine.getDependencyManagement();
        List<ResolvedManagedDependency> b = caller.getDependencyManagement();
        if (a.size() != b.size()) {
            return false;
        }
        Map<String, ResolvedManagedDependency> byKey = new HashMap<>(b.size() * 2);
        for (ResolvedManagedDependency dm : b) {
            byKey.put(dmKey(dm), dm);
        }
        for (ResolvedManagedDependency dm : a) {
            ResolvedManagedDependency other = byKey.get(dmKey(dm));
            if (other == null ||
                    !Objects.equals(dm.getVersion(), other.getVersion()) ||
                    dm.getScope() != other.getScope() ||
                    !Objects.equals(dm.getExclusions(), other.getExclusions())) {
                return false;
            }
        }
        return true;
    }

    private static String dmKey(ResolvedManagedDependency dm) {
        return dm.getGroupId() + ':' + dm.getArtifactId() + ':' +
                (dm.getClassifier() == null ? "" : dm.getClassifier()) + ':' +
                (dm.getType() == null ? "jar" : dm.getType());
    }

    private static final class EngineGraph {
        final ResolvedPom pom;
        final Map<Scope, List<ResolvedDependency>> dependencies;
        final @Nullable MavenDownloadingExceptions failure;

        EngineGraph(ResolvedPom pom, Map<Scope, List<ResolvedDependency>> dependencies, @Nullable MavenDownloadingExceptions failure) {
            this.pom = pom;
            this.dependencies = dependencies;
            this.failure = failure;
        }
    }

    // A legacy dependency attempt: the resolved scopes, or the failure carrying the partial projection. Both feed the
    // shadow snapshot so a failing resolution still compares its resolvable scopes and its error set.
    private static final class DepAttempt {
        final @Nullable Map<Scope, List<ResolvedDependency>> result;
        final @Nullable MavenDownloadingExceptions failure;
        final Map<Scope, List<ResolvedDependency>> forSnapshot;

        DepAttempt(@Nullable Map<Scope, List<ResolvedDependency>> result, @Nullable MavenDownloadingExceptions failure,
                   Map<Scope, List<ResolvedDependency>> forSnapshot) {
            this.result = result;
            this.failure = failure;
            this.forSnapshot = forSnapshot;
        }
    }

    private static DepAttempt attemptDeps(LegacyDependencyResolution legacy) throws MavenDownloadingExceptions {
        try {
            Map<Scope, List<ResolvedDependency>> resolved = legacy.resolve();
            return new DepAttempt(resolved, null, resolved);
        } catch (MavenDownloadingExceptions e) {
            Map<Scope, List<ResolvedDependency>> partial = e.getPartialResult() == null ?
                    new LinkedHashMap<>() : e.getPartialResult().getDependencies();
            return new DepAttempt(null, e, partial);
        }
    }

    static void assertShadowDependencyParity(ResolvedPom legacyPom, List<String> profiles,
                                             DepAttempt legacy, EngineGraph engine) {
        Pom requested = legacyPom.getRequested();
        SnapshotNormalizer normalizer = requested.getSourcePath() != null && requested.getSourcePath().getParent() != null ?
                new SnapshotNormalizer(requested.getSourcePath().getParent()) : new SnapshotNormalizer();
        // Each side threads requestedRef against its own pom's requested dependencies, so snapshot legacy deps with the
        // legacy pom and engine deps with the engine pom. The pom section then diffs exactly as the effective-pom shadow
        // (same ledgered masks); scopes/errors carry the dependency comparison.
        ResolutionSnapshot legacySnapshot = dependencySnapshot(legacyPom, legacy.forSnapshot, legacy.failure, profiles, normalizer);
        ResolutionSnapshot engineSnapshot = dependencySnapshot(engine.pom, engine.dependencies, engine.failure, profiles, normalizer);

        List<ResolutionDiff.Entry> entries = new ArrayList<>();
        for (ResolutionDiff.Entry entry : ResolutionDiff.between(legacySnapshot, engineSnapshot).getEntries()) {
            String path = entry.getPath();
            if (path.startsWith("$.pom") || path.startsWith("$.scopes") || path.startsWith("$.errors")) {
                entries.add(entry);
            }
        }
        ResolutionDiff diff = new ResolutionDiff(entries).masked(SnapshotNormalizer.loadMasks());
        if (!diff.isEmpty()) {
            throw new AssertionError("Shadow dependency resolution mismatch for " + requested.getGav() +
                    " (legacy=left, engine=right):\n" + diff.render());
        }
    }

    private static ResolutionSnapshot dependencySnapshot(ResolvedPom pom, Map<Scope, List<ResolvedDependency>> dependencies,
                                                         @Nullable MavenDownloadingExceptions failure, List<String> profiles,
                                                         SnapshotNormalizer normalizer) {
        MavenResolutionResult wrapper = new MavenResolutionResult(
                UUID.randomUUID(), null, pom, emptyList(), null, dependencies, null, profiles, emptyMap());
        List<Throwable> errors = new ArrayList<>();
        if (failure != null) {
            errors.addAll(failure.getExceptions());
        }
        return ResolutionSnapshot.of(wrapper, errors, emptyList(), normalizer, null);
    }

    private static CollectorHandle collectorHandle(ExecutionContext ctx) {
        return ctx.computeMessageIfAbsent(COLLECTOR_HANDLE_KEY, k -> {
            try {
                long t0 = EngineProfiler.ENABLED ? System.nanoTime() : 0;
                Path scratch = Files.createTempDirectory("rewrite-engine-collect-");
                scratch.toFile().deleteOnExit();
                CollectorHandle h = new CollectorHandle(new EngineDependencyCollector(), scratch);
                if (EngineProfiler.ENABLED) {
                    EngineProfiler.bootstrapNanos.addAndGet(System.nanoTime() - t0);
                }
                return h;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    // Bootstrapped once per ExecutionContext; its RepositorySystem + shared RepositoryCache keep re-collects warm across
    // a reactor's modules. Dev/CI-only, so the collector (Closeable) is intentionally not closed — it dies with the ctx.
    private static final class CollectorHandle {
        final EngineDependencyCollector collector;
        final Path scratch;

        CollectorHandle(EngineDependencyCollector collector, Path scratch) {
            this.collector = collector;
            this.scratch = scratch;
        }
    }

    // Rewrite's RawPom parses poms with no explicit <modelVersion> (it defaults to 4.0.0); Maven's ModelBuilder rejects
    // them. Default it so the engine reads the same lenient model rewrite's parser does (KEEP_REWRITE, L-P2-C-003).
    static byte[] ensureModelVersion(byte[] xml) {
        String s = new String(xml, StandardCharsets.UTF_8);
        if (s.contains("<modelVersion")) {
            return xml;
        }
        int projectOpen = s.indexOf("<project");
        if (projectOpen < 0) {
            return xml;
        }
        int tagClose = s.indexOf('>', projectOpen);
        if (tagClose < 0 || s.charAt(tagClose - 1) == '/') {
            return xml;
        }
        return (s.substring(0, tagClose + 1) + "<modelVersion>4.0.0</modelVersion>" + s.substring(tagClose + 1))
                .getBytes(StandardCharsets.UTF_8);
    }

    private static MavenDownloadingException rethrow(@Nullable Throwable failure) {
        if (failure instanceof MavenDownloadingException) {
            return (MavenDownloadingException) failure;
        }
        if (failure instanceof RuntimeException) {
            throw (RuntimeException) failure;
        }
        throw new IllegalStateException("Engine effective-pom build failed", failure);
    }

    // --- shadow comparison (package-visible for direct unit testing) ---------------------------------------------

    static final class Attempt {
        final @Nullable ResolvedPom result;
        final @Nullable Throwable failure;

        Attempt(@Nullable ResolvedPom result, @Nullable Throwable failure) {
            this.result = result;
            this.failure = failure;
        }

        MavenDownloadingException rethrow() {
            if (failure instanceof MavenDownloadingException) {
                return (MavenDownloadingException) failure;
            }
            if (failure instanceof RuntimeException) {
                throw (RuntimeException) failure;
            }
            throw new IllegalStateException("Resolution failed", failure);
        }
    }

    @FunctionalInterface
    private interface Resolution {
        ResolvedPom resolve() throws MavenDownloadingException;
    }

    // Both legitimate resolution-failure signals are captured so shadow reports an outcome comparison rather than
    // crashing: the legacy path throws MavenDownloadingException, and the engine may additionally throw
    // MavenParsingException (validation/cycle) as an unchecked exception. Other throwables are real bugs and propagate.
    private static Attempt attempt(Resolution resolution) {
        try {
            return new Attempt(resolution.resolve(), null);
        } catch (MavenDownloadingException | MavenParsingException e) {
            return new Attempt(null, e);
        }
    }

    static void assertShadowParity(Pom requested, List<String> profiles, Map<String, String> injected,
                                   Attempt legacy, Attempt engine) {
        if (legacy.failure != null || engine.failure != null) {
            // One threw and the other did not: an outcome mismatch. Legacy-resolves/engine-fails where the engine is
            // Maven-identically stricter than rewrite's lenient parser (cycles, self-parent, non-pom parent/aggregator
            // packaging, a genuinely missing dependency version) is a known class that flips at Phase 5; a ledgered
            // outcome mask lets shadow return legacy's result rather than failing. Anything else is unexplained.
            if (legacy.failure == null || engine.failure == null) {
                String category = classifyEngineStrictness(legacy, engine);
                if (category != null && isOutcomeMaskLedgered(category)) {
                    return;
                }
                throw new AssertionError("Shadow resolution outcome mismatch for " + requested.getGav() + ":\n" +
                        "  legacy: " + describe(legacy) + "\n" +
                        "  engine: " + describe(engine));
            }
            return;
        }
        ResolutionDiff diff = pomDiff(requested, profiles, injected,
                requireNonNull(legacy.result), requireNonNull(engine.result))
                .masked(SnapshotNormalizer.loadMasks());
        if (!diff.isEmpty()) {
            throw new AssertionError("Shadow resolution $.pom mismatch for " + requested.getGav() +
                    " (legacy=left, engine=right):\n" + diff.render());
        }
    }

    private static ResolutionDiff pomDiff(Pom requested, List<String> profiles, Map<String, String> injected,
                                          ResolvedPom legacy, ResolvedPom engine) {
        SnapshotNormalizer normalizer = requested.getSourcePath() != null && requested.getSourcePath().getParent() != null ?
                new SnapshotNormalizer(requested.getSourcePath().getParent()) : new SnapshotNormalizer();
        ResolutionSnapshot legacySnapshot = pomSnapshot(legacy, profiles, injected, normalizer);
        ResolutionSnapshot engineSnapshot = pomSnapshot(engine, profiles, injected, normalizer);
        List<ResolutionDiff.Entry> pomEntries = new ArrayList<>();
        for (ResolutionDiff.Entry entry : ResolutionDiff.between(legacySnapshot, engineSnapshot).getEntries()) {
            if (entry.getPath().startsWith("$.pom")) {
                pomEntries.add(entry);
            }
        }
        return new ResolutionDiff(pomEntries);
    }

    private static ResolutionSnapshot pomSnapshot(ResolvedPom pom, List<String> profiles, Map<String, String> injected,
                                                  SnapshotNormalizer normalizer) {
        MavenResolutionResult wrapper = new MavenResolutionResult(
                UUID.randomUUID(), null, pom, emptyList(), null, new LinkedHashMap<>(), null, profiles, injected);
        return ResolutionSnapshot.of(wrapper, emptyList(), emptyList(), normalizer, null);
    }

    // Classifies a legacy-resolves/engine-throws mismatch into an engine-strictness category (or null if it is not a
    // known Maven-stricter-than-rewrite case and must surface). The reverse direction is never masked.
    static @Nullable String classifyEngineStrictness(Attempt legacy, Attempt engine) {
        if (legacy.failure != null || engine.failure == null) {
            return null;
        }
        return strictnessCategory(engine.failure.getMessage());
    }

    // The engine-strictness category of a thrown message (or null if not a known Maven-stricter-than-rewrite case).
    static @Nullable String strictnessCategory(@Nullable String message) {
        if (message == null) {
            return null;
        }
        if (message.contains("cannot have the same groupId:artifactId as the project")) {
            return "self-parent";
        }
        if (message.contains("recursive expression cycle")) {
            return "expression-cycle";
        }
        if (message.contains("Invalid packaging for parent POM")) {
            return "parent-packaging";
        }
        if (message.contains("Aggregator projects require 'pom' as packaging")) {
            return "aggregator-packaging";
        }
        if ((message.contains("dependencies.dependency.version") && message.contains("is missing")) ||
                message.contains("'version' is missing")) {
            // Maven rejects a dependency (or transitive descriptor) with no resolvable version at model build; legacy
            // tolerates it and defers to download. Same ALIGN_TO_MAVEN class whether reported as the full path or bare.
            return "missing-dependency-version";
        }
        // Any system-scope <systemPath> validation Maven enforces but rewrite tolerates: absent, or non-absolute
        // (e.g. ${project.basedir}-relative). Legacy resolves; the engine throws exactly where Maven does.
        if (message.contains("dependencies.dependency.systemPath")) {
            return "system-scope-missing-path";
        }
        return null;
    }

    private static boolean isOutcomeMaskLedgered(String category) {
        String token = "outcome:" + category;
        for (SnapshotNormalizer.Mask mask : SnapshotNormalizer.loadMasks()) {
            if (token.equals(mask.getJsonPathPrefix())) {
                return true;
            }
        }
        return false;
    }

    private static String describe(Attempt attempt) {
        if (attempt.failure != null) {
            return "threw " + attempt.failure.getClass().getSimpleName() + ": " +
                    firstLine(attempt.failure.getMessage());
        }
        return "resolved " + requireNonNull(attempt.result).getGav();
    }

    private static String firstLine(@Nullable String message) {
        if (message == null) {
            return "<null>";
        }
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }

    // --- engine lifecycle ----------------------------------------------------------------------------------------

    /** Bootstrapped once per {@link ExecutionContext} (DESIGN §2 "cached on the ExecutionContext"). Dev/CI-only. */
    private static final class EngineHandle {
        final MavenEngine engine;
        final RepositorySystemSession session;

        EngineHandle(MavenEngine engine, RepositorySystemSession session) {
            this.engine = engine;
            this.session = session;
        }
    }

    private static EngineHandle handle(ExecutionContext ctx) {
        return ctx.computeMessageIfAbsent(ENGINE_HANDLE_KEY, k -> {
            long t0 = EngineProfiler.ENABLED ? System.nanoTime() : 0;
            EngineHandle h = newHandle(ctx);
            if (EngineProfiler.ENABLED) {
                EngineProfiler.bootstrapNanos.addAndGet(System.nanoTime() - t0);
            }
            return h;
        });
    }

    private static EngineHandle newHandle(ExecutionContext ctx) {
        try {
            Path scratch = Files.createTempDirectory("rewrite-engine-");
            scratch.toFile().deleteOnExit();
            MavenEngine engine = new MavenEngine();
            RepositorySystemSession session = engine.newSession(scratch.resolve("lrm"),
                    SessionConfig.forSender(HttpSenderExecutionContextView.view(ctx).getHttpSender()));
            return new EngineHandle(engine, session);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> toList(Iterable<String> iterable) {
        List<String> list = new ArrayList<>();
        for (String s : iterable) {
            list.add(s);
        }
        return list;
    }

    private static <T> T requireNonNull(@Nullable T value) {
        if (value == null) {
            throw new IllegalStateException("Expected a non-null resolution result");
        }
        return value;
    }
}
