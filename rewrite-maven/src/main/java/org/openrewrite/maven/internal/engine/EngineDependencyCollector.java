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
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.engine.HttpSenderTransporterFactory;
import org.openrewrite.maven.engine.ResolutionTimeRecorder;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Model;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.ConfigurationProperties;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.DefaultRepositoryCache;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositoryCache;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystem;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystemSession;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.artifact.Artifact;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.artifact.DefaultArtifact;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.collection.CollectRequest;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.collection.CollectResult;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.collection.DependencyCollectionException;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.graph.Dependency;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.graph.DependencyCycle;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.graph.DependencyNode;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.repository.RemoteRepository;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.repository.RepositoryPolicy;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.Pom;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 3 slice A's collection service: one verbose {@code collectDependencies} over the whole root effective model
 * (DESIGN §0 N1 — every scope's direct dependencies seeded together, per-scope trees projected downstream). The
 * {@code RepositorySystem} is {@link EngineCollectSystemSupplier}'s (its descriptor reader builds effective models on
 * Phase 2's {@link EngineEffectivePom}, its version resolver honors pinned snapshots), and the session template mirrors
 * {@code MavenEngine}'s Maven-3.9 knobs plus the verbose {@code ConflictResolver}/{@code DependencyManager} config the
 * mapper needs (winner/loser links, premanaged state).
 * <p>
 * The output {@link EngineCollectOutcome} carries the verbose root, the {@code gav → repository} attribution, the
 * dependency cycles Maven recorded and tolerated, and the failures split as Maven's tolerance profile dictates: a direct
 * dependency whose descriptor was tolerated but that rewrite must fail on becomes a {@link MavenDownloadingException};
 * transitive descriptor failures are surfaced for warn events but never fail the collect.
 */
public class EngineDependencyCollector implements Closeable {

    private final RepositorySystem system = new EngineCollectSystemSupplier().get();

    // Shared across every session so the DataPool descriptor pool survives a warm re-collect (a5 §2.4) — the second
    // collect reads its descriptors from the pool and rebuilds no models.
    private final RepositoryCache repositoryCache = new DefaultRepositoryCache();

    public EngineCollectOutcome collect(Model rootEffectiveModel, Pom requested,
                                        List<MavenRepository> requestRepositories, EffectiveSettings settings,
                                        ReactorWorkspace reactor, Path scratch, ExecutionContext ctx) {
        MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
        MavenPomCache pomCache = mctx.getPomCache();
        Path materializeDir = scratch.resolve("materialize");

        CollectContext cc = new CollectContext(system, materializeDir, pomCache, reactor, settings, ctx,
                requestRepositories, mctx.getPinnedSnapshotVersions());

        try (CloseableSession session = newSession(scratch.resolve("lrm"),
                HttpSenderExecutionContextView.view(ctx).getHttpSender(), cc)) {
            CollectRequest request = collectRequest(session, rootEffectiveModel, requestRepositories);
            DependencyNode root;
            List<MavenDownloadingException> hardFailures = new ArrayList<>();
            try {
                CollectResult result = system.collectDependencies(session, request);
                root = result.getRoot();
                return outcome(root, cc, result.getCycles(), hardFailures);
            } catch (DependencyCollectionException e) {
                // Maven failed the whole collect (an untolerated failure — e.g. an unresolvable parent/BOM of a
                // dependency). Surface it against the partial graph aether still returns.
                CollectResult partial = e.getResult();
                root = partial == null ? null : partial.getRoot();
                hardFailures.add(toDownloadingException(e, requested));
                List<DependencyCycle> cycles = partial == null ? Collections.emptyList() : partial.getCycles();
                return outcome(root, cc, cycles, hardFailures);
            }
        }
    }

    private EngineCollectOutcome outcome(@Nullable DependencyNode root, CollectContext cc, List<DependencyCycle> cycles,
                                         List<MavenDownloadingException> hardFailures) {
        Map<GroupArtifactVersion, Integer> depths = new LinkedHashMap<>();
        if (root != null) {
            recordDepths(root, 0, depths, Collections.newSetFromMap(new IdentityHashMap<>()));
        }

        List<MavenDownloadingException> directFailures = new ArrayList<>(hardFailures);
        List<GroupArtifactVersion> toleratedTransitive = new ArrayList<>();
        cc.getDescriptorFailures().forEach((gav, failure) -> {
            Integer depth = depths.get(gav);
            if (depth != null && depth == 1) {
                // A direct dependency the user declared that could not be read: Maven tolerates it during collect,
                // rewrite fails on it (KEEP_REWRITE per-file error containment), matching the legacy downloader.
                MavenDownloadingException ex =
                        new MavenDownloadingException(failure.getReason(), null, gav).setRoot(gav);
                if (!failure.getRepositoryResponses().isEmpty()) {
                    ex.setRepositoryResponses(failure.getRepositoryResponses());
                }
                directFailures.add(ex);
            } else {
                toleratedTransitive.add(gav);
            }
        });

        return new EngineCollectOutcome(root, cc.getServedBy(), cycles, directFailures, toleratedTransitive,
                cc.getDescriptorReads().get());
    }

    private CollectRequest collectRequest(RepositorySystemSession session, Model model,
                                          List<MavenRepository> requestRepositories) {
        List<Dependency> direct = new ArrayList<>();
        for (Dependency dependency : DependencyConversions.toAether(model.getDependencies(),
                session.getArtifactTypeRegistry())) {
            // A direct dependency with no resolvable version cannot be collected (a model-level problem the
            // effective-pom mapper reports); the collector seeds only what aether can descend into.
            String version = dependency.getArtifact().getVersion();
            if (version != null && !version.isEmpty()) {
                direct.add(dependency);
            }
        }
        CollectRequest request = new CollectRequest();
        request.setRootArtifact(rootArtifact(model));
        request.setDependencies(direct);
        request.setManagedDependencies(DependencyConversions.managedOf(model, session.getArtifactTypeRegistry()));
        request.setRepositories(toRemote(requestRepositories));
        return request;
    }

    private static Artifact rootArtifact(Model model) {
        String packaging = model.getPackaging();
        String extension = "pom".equals(packaging) ? "pom" : "jar";
        return new DefaultArtifact(model.getGroupId(), model.getArtifactId(), "", extension, model.getVersion());
    }

    // Mirrors MavenEngine.newSession (its Maven-3.9 session-factory template) and adds the verbose collect config and
    // the per-run CollectContext. A separate system+session from MavenEngine's because the collector needs the
    // EngineDescriptorReader wired into the RepositorySystem.
    private CloseableSession newSession(Path localRepositoryScratch, HttpSender sender, CollectContext cc) {
        return new SessionBuilderSupplier(system).get()
                .setCache(repositoryCache)
                .setConfigProperty(ConfigurationProperties.USER_AGENT, userAgent())
                .withLocalRepositoryBaseDirectories(localRepositoryScratch)
                .setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_IGNORE)
                .setUpdatePolicy(null)
                .setResolutionErrorPolicy(new SimpleResolutionErrorPolicy(0, ResolutionErrorPolicy.CACHE_NOT_FOUND))
                .setIgnoreArtifactDescriptorRepositories(false)
                .setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(true, true))
                .setConfigProperty(HttpSenderTransporterFactory.HTTP_SENDER_KEY, sender)
                .setConfigProperty(HttpSenderTransporterFactory.UNREACHABLE_HOSTS_KEY,
                        Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .setConfigProperty(HttpSenderTransporterFactory.RESOLUTION_TIME_RECORDER_KEY, ResolutionTimeRecorder.NOOP)
                // DESIGN §4.2: verbose STANDARD keeps losers with a NODE_DATA_WINNER link; verbose DependencyManager
                // records premanaged version/scope/optional/exclusions the mapper reads.
                .setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, ConflictResolver.Verbosity.STANDARD)
                .setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, Boolean.TRUE)
                .setConfigProperty(CollectContext.SESSION_KEY, cc)
                .build();
    }

    private static void recordDepths(DependencyNode node, int depth, Map<GroupArtifactVersion, Integer> depths,
                                     Set<DependencyNode> visited) {
        if (!visited.add(node)) {
            return;
        }
        Artifact artifact = node.getArtifact();
        if (artifact != null) {
            GroupArtifactVersion gav =
                    new GroupArtifactVersion(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
            depths.merge(gav, depth, Math::min);
        }
        for (DependencyNode child : node.getChildren()) {
            recordDepths(child, depth + 1, depths, visited);
        }
    }

    private MavenDownloadingException toDownloadingException(DependencyCollectionException e, Pom requested) {
        GroupArtifactVersion failedOn = new GroupArtifactVersion(
                requested.getGroupId(), requested.getArtifactId(), requested.getVersion());
        return new MavenDownloadingException(
                e.getMessage() == null ? "Dependency collection failed" : e.getMessage(), e, failedOn)
                .setRoot(failedOn);
    }

    private List<RemoteRepository> toRemote(List<MavenRepository> repositories) {
        List<RemoteRepository> remotes = new ArrayList<>(repositories.size());
        for (MavenRepository repo : repositories) {
            RemoteRepository.Builder builder = new RemoteRepository.Builder(
                    repo.getId() == null ? repo.getUri() : repo.getId(), "default", repo.getUri())
                    .setReleasePolicy(policy(repo.getReleases()))
                    .setSnapshotPolicy(policy(repo.getSnapshots()));
            if (repo.getUsername() != null && repo.getPassword() != null) {
                builder.setAuthentication(new AuthenticationBuilder()
                        .addUsername(repo.getUsername()).addPassword(repo.getPassword()).build());
            }
            remotes.add(builder.build());
        }
        return remotes;
    }

    private static RepositoryPolicy policy(@Nullable String enabled) {
        return new RepositoryPolicy(!"false".equalsIgnoreCase(enabled),
                RepositoryPolicy.UPDATE_POLICY_ALWAYS, RepositoryPolicy.CHECKSUM_POLICY_IGNORE);
    }

    private static String userAgent() {
        return "OpenRewrite-Maven-Engine (Java " + System.getProperty("java.version") + "; " +
                System.getProperty("os.name") + " " + System.getProperty("os.version") + ")";
    }

    @Override
    public void close() {
        system.shutdown();
    }
}
