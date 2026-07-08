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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.engine.MavenEngine;
import org.openrewrite.maven.engine.SessionConfig;
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
import org.openrewrite.maven.tree.ResolvedPom;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.util.Collections.emptyList;

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

    // The legacy resolver resolves imported BOMs and parents through nested Pom.resolve calls (ResolvedPom line ~941);
    // those are internal to the legacy algorithm and have no engine equivalent (the engine imports BOMs inside Maven's
    // ModelBuilder). Only the outermost facade call dispatches; re-entrant calls run the legacy path directly.
    private static final ThreadLocal<Boolean> DISPATCHING = ThreadLocal.withInitial(() -> Boolean.FALSE);

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
        MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
        List<String> profiles = toList(activeProfiles);
        Map<String, String> injected = PomXmlRegistry.injectedProperties(ctx);

        byte[] xml = PomXmlRegistry.get(ctx, requested);
        if (xml == null) {
            // No registry bytes match this requested pom: a synthetic Pom.builder() graph (rewrite-gradle), or a recipe
            // that mutated the requested pom and re-resolved without refreshing the registry. Print from the converter.
            xml = new PomToModelConverter().toXml(requested);
        }
        xml = ensureModelVersion(xml);

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
        EngineEffectivePom service = new EngineEffectivePom(
                handle.engine.getRepositorySystem(), handle.session, requestRepositories, handle.materializeDir);
        EngineModelBuildingOutcome outcome = service.build(xml, requested, effectiveSettings, reactor, ctx);
        if (!outcome.isSuccess()) {
            throw rethrow(outcome.getFailure());
        }
        BomGavAttributor attributor = new BomGavAttributor(service, effectiveSettings, reactor, ctx, mctx.getPomCache());
        EffectivePomMapper mapper = new EffectivePomMapper(mctx.getPomCache(), attributor, reactor);
        return mapper.map(outcome, requested, profiles, injected);
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
        String message = engine.failure.getMessage();
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
        if (message.contains("dependencies.dependency.version") && message.contains("is missing")) {
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
        final Path materializeDir;

        EngineHandle(MavenEngine engine, RepositorySystemSession session, Path materializeDir) {
            this.engine = engine;
            this.session = session;
            this.materializeDir = materializeDir;
        }
    }

    private static EngineHandle handle(ExecutionContext ctx) {
        return ctx.computeMessageIfAbsent(ENGINE_HANDLE_KEY, k -> newHandle(ctx));
    }

    private static EngineHandle newHandle(ExecutionContext ctx) {
        try {
            Path scratch = Files.createTempDirectory("rewrite-engine-");
            scratch.toFile().deleteOnExit();
            MavenEngine engine = new MavenEngine();
            RepositorySystemSession session = engine.newSession(scratch.resolve("lrm"),
                    SessionConfig.forSender(HttpSenderExecutionContextView.view(ctx).getHttpSender()));
            return new EngineHandle(engine, session, scratch.resolve("materialize"));
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
