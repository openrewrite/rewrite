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
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenDownloadingExceptions;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.artifact.Artifact;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.graph.DependencyNode;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.openrewrite.maven.tree.*;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * Projects the single verbose collect ({@link EngineCollectOutcome}) into the frozen per-scope
 * {@code Map<Scope, List<ResolvedDependency>>} (DESIGN §4.2). One mediated graph, four projections:
 * <ul>
 *   <li>Per-scope root seeding matches the legacy filter ({@code dScope == scope || dScope.transitiveOf(scope) == scope},
 *       root propagation scope {@code Compile}); children are scope-filtered exactly as {@code doResolveDependencies}.</li>
 *   <li>Winner nodes only: conflict losers ({@code NODE_DATA_WINNER}) are skipped, so each node links to its single
 *       nearest-declaring parent — the legacy "already resolved → skip the second edge" shape.</li>
 *   <li>Instances are shared within a scope between the flat list and the nested {@code dependencies} graph, and are
 *       never shared across scopes (each scope projects a fresh tree).</li>
 *   <li>{@code requested} threads the original {@link Dependency} instance of the declaring pom at depth 0; transitive
 *       nodes carry the declaring pom's declared coordinates.</li>
 * </ul>
 * Direct (depth-1) failures are aggregated into {@link MavenDownloadingExceptions} legacy-shaped (deduped by root GA +
 * failed GAV, the failing scope omitted), carrying the complete model of the resolvable scopes as {@code partialResult}
 * (L-P0-004). {@code effectiveExclusions} are attributed by {@link ExclusionAttributor} as a reporting post-pass.
 */
public class DependencyGraphMapper {

    private static final Scope[] RESOLVE_SCOPES = {Scope.Compile, Scope.Runtime, Scope.Test, Scope.Provided};

    private final MavenPomCache pomCache;

    public DependencyGraphMapper(MavenPomCache pomCache) {
        this.pomCache = pomCache;
    }

    public Map<Scope, List<ResolvedDependency>> map(EngineCollectOutcome outcome, ResolvedPom engineResolvedPom,
                                                    Pom requested, ExecutionContext ctx) throws MavenDownloadingExceptions {
        Index index = new Index(outcome.getServedBy(), pomCache);
        List<Dependency> requestedDependencies = engineResolvedPom.getRequestedDependencies();

        Map<Scope, List<ResolvedDependency>> result = new LinkedHashMap<>();
        MavenDownloadingExceptions exceptions = null;
        // Mirrors MavenResolutionResult.resolveDependencies: one exception per (root GA, failed GAV) across scopes.
        Map<GroupArtifact, Set<GroupArtifactVersion>> seenFailures = new HashMap<>();

        for (Scope scope : RESOLVE_SCOPES) {
            List<MavenDownloadingException> scopeFailures = failuresForScope(outcome.getDirectFailures(), requestedDependencies, scope);
            if (!scopeFailures.isEmpty()) {
                for (MavenDownloadingException e : scopeFailures) {
                    GroupArtifactVersion root = e.getRoot();
                    GroupArtifact ga = new GroupArtifact(root.getGroupId() == null ? "" : root.getGroupId(), root.getArtifactId());
                    if (seenFailures.computeIfAbsent(ga, k -> new HashSet<>()).add(e.getFailedOn())) {
                        exceptions = MavenDownloadingExceptions.append(exceptions, e);
                    }
                }
                // A failure in a scope discards that whole scope's list, exactly as the legacy per-scope catch does.
                continue;
            }
            result.put(scope, project(outcome.getRoot(), scope, requestedDependencies, index));
        }

        if (exceptions != null) {
            throw exceptions.setPartialResult(partial(engineResolvedPom, result));
        }
        return result;
    }

    // ---- per-scope projection ----

    private List<ResolvedDependency> project(@Nullable DependencyNode root, Scope scope,
                                             List<Dependency> requestedDependencies, Index index) {
        if (root == null) {
            return new ArrayList<>();
        }
        Map<DependencyNode, Node> byNode = new IdentityHashMap<>();
        List<Node> flat = new ArrayList<>();
        List<Node> roots = new ArrayList<>();

        LinkedHashMap<GroupArtifactClassifierType, Frame> seeds = new LinkedHashMap<>();
        for (DependencyNode child : winners(root)) {
            Scope dScope = scopeOf(child);
            if (dScope == scope || dScope.transitiveOf(scope) == scope) {
                seeds.putIfAbsent(gact(child), new Frame(child, Scope.Compile, null, matchRequested(child, requestedDependencies)));
            }
        }

        List<Frame> level = new ArrayList<>(seeds.values());
        int depth = 0;
        while (!level.isEmpty()) {
            LinkedHashMap<GroupArtifactClassifierType, Frame> next = new LinkedHashMap<>();
            for (Frame frame : level) {
                Node node = byNode.get(frame.aether);
                if (node == null) {
                    node = buildNode(frame, depth, index);
                    byNode.put(frame.aether, node);
                    if (depth == 0) {
                        roots.add(node);
                    }
                }
                if (frame.parent != null) {
                    frame.parent.children.add(node);
                }
                // Membership uses the propagation scope; a node that fails it is still linked to its parent but is not
                // in the flat list and its subtree is not walked (legacy ResolvedPom.java:1157).
                if (frame.propagationScope.transitiveOf(scope) != scope) {
                    continue;
                }
                flat.add(node);
                for (DependencyNode child : winners(frame.aether)) {
                    Scope childScope = scopeOf(child);
                    if (childScope.isInClasspathOf(frame.propagationScope)) {
                        next.putIfAbsent(gact(child), new Frame(child, childScope, node, transitiveRequested(child)));
                    }
                }
            }
            level = new ArrayList<>(next.values());
            depth++;
        }

        new ExclusionAttributor(index::pomFor).attribute(roots);

        Map<Node, ResolvedDependency> resolved = new IdentityHashMap<>();
        List<ResolvedDependency> out = new ArrayList<>(flat.size());
        for (Node node : flat) {
            out.add(toResolvedDependency(node, resolved));
        }
        return out;
    }

    private static ResolvedDependency toResolvedDependency(Node node, Map<Node, ResolvedDependency> resolved) {
        ResolvedDependency existing = resolved.get(node);
        if (existing != null) {
            return existing;
        }
        List<ResolvedDependency> children = node.children.isEmpty() ? emptyList() : new ArrayList<>(node.children.size());
        ResolvedDependency rd = ResolvedDependency.builder()
                .repository(node.repository)
                .gav(node.resolvedGav)
                .requested(node.requested)
                .dependencies(children)
                .licenses(node.licenses)
                .type(node.type)
                .classifier(node.classifier)
                .optional(node.optional)
                .depth(node.depth)
                .effectiveExclusions(node.effectiveExclusions.isEmpty() ? null : node.effectiveExclusions)
                .build();
        resolved.put(node, rd);
        for (Node child : node.children) {
            children.add(toResolvedDependency(child, resolved));
        }
        return rd;
    }

    private Node buildNode(Frame frame, int depth, Index index) {
        Artifact artifact = frame.aether.getArtifact();
        GroupArtifactVersion gav = new GroupArtifactVersion(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion());
        MavenRepository repository = index.repoFor(gav);
        String repositoryUri = repository == null ? null : repository.getUri();
        // L-P0-005 reproduction: a remote pom carries datedSnapshotVersion == the resolved version (the dated form for a
        // snapshot, the plain version duplicated for a release); a project/reactor pom carries null.
        String datedSnapshotVersion = repositoryUri == null ? null : artifact.getVersion();
        ResolvedGroupArtifactVersion resolvedGav = new ResolvedGroupArtifactVersion(
                repositoryUri, artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(), datedSnapshotVersion);

        Node node = new Node();
        node.gav = gav;
        node.resolvedGav = resolvedGav;
        node.repository = repository;
        node.requested = frame.requested;
        node.type = frame.requested.getType();
        node.classifier = frame.requested.getClassifier();
        node.optional = Boolean.valueOf(frame.requested.getOptional());
        node.depth = depth;
        node.licenses = index.licensesFor(gav);
        return node;
    }

    // The original declared Dependency instance from the requesting pom, so getResolvedDependency stays reference-exact.
    private @Nullable Dependency matchRequested(DependencyNode node, List<Dependency> requestedDependencies) {
        Artifact artifact = node.getArtifact();
        String classifier = emptyToNull(artifact.getClassifier());
        Dependency byGa = null;
        for (Dependency dependency : requestedDependencies) {
            if (Objects.equals(dependency.getGroupId(), artifact.getGroupId()) &&
                    dependency.getArtifactId().equals(artifact.getArtifactId())) {
                if (Objects.equals(emptyToNull(dependency.getClassifier()), classifier)) {
                    return dependency;
                }
                if (byGa == null) {
                    byGa = dependency;
                }
            }
        }
        return byGa != null ? byGa : synthetic(artifact);
    }

    // A transitive node's requested coordinates are the declaring pom's declared version (the pre-management version when
    // the root dependencyManagement overrode it), matching what the legacy BFS threads as ResolvedDependency.requested.
    private static Dependency transitiveRequested(DependencyNode node) {
        Artifact artifact = node.getArtifact();
        String version = artifact.getBaseVersion();
        if ((node.getManagedBits() & DependencyNode.MANAGED_VERSION) != 0) {
            String premanaged = DependencyManagerUtils.getPremanagedVersion(node);
            if (premanaged != null) {
                version = premanaged;
            }
        }
        return Dependency.builder()
                .gav(new GroupArtifactVersion(artifact.getGroupId(), artifact.getArtifactId(), version))
                .classifier(emptyToNull(artifact.getClassifier()))
                .type(artifact.getExtension())
                .build();
    }

    private static Dependency synthetic(Artifact artifact) {
        return Dependency.builder()
                .gav(new GroupArtifactVersion(artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion()))
                .classifier(emptyToNull(artifact.getClassifier()))
                .type(artifact.getExtension())
                .build();
    }

    // ---- failure aggregation ----

    private List<MavenDownloadingException> failuresForScope(List<MavenDownloadingException> failures,
                                                             List<Dependency> requestedDependencies, Scope scope) {
        if (failures.isEmpty()) {
            return emptyList();
        }
        List<MavenDownloadingException> out = new ArrayList<>();
        for (MavenDownloadingException failure : failures) {
            Scope declared = declaredScope(failure.getRoot(), requestedDependencies);
            if (declared == scope || declared.transitiveOf(scope) == scope) {
                out.add(failure);
            }
        }
        return out;
    }

    private static Scope declaredScope(GroupArtifactVersion root, List<Dependency> requestedDependencies) {
        for (Dependency dependency : requestedDependencies) {
            if (Objects.equals(dependency.getGroupId(), root.getGroupId()) &&
                    dependency.getArtifactId().equals(root.getArtifactId())) {
                return Scope.fromName(dependency.getScope());
            }
        }
        return Scope.Compile;
    }

    private static MavenResolutionResult partial(ResolvedPom pom, Map<Scope, List<ResolvedDependency>> dependencies) {
        List<String> activeProfiles = new ArrayList<>();
        pom.getActiveProfiles().forEach(activeProfiles::add);
        return new MavenResolutionResult(UUID.randomUUID(), null, pom, emptyList(), null, dependencies, null,
                activeProfiles, emptyMap());
    }

    // ---- aether graph helpers ----

    private static List<DependencyNode> winners(DependencyNode node) {
        List<DependencyNode> out = new ArrayList<>(node.getChildren().size());
        for (DependencyNode child : node.getChildren()) {
            if (child.getData().get(ConflictResolver.NODE_DATA_WINNER) == null) {
                out.add(child);
            }
        }
        return out;
    }

    private static Scope scopeOf(DependencyNode node) {
        String scope = node.getDependency() == null ? null : node.getDependency().getScope();
        return scope == null || scope.isEmpty() ? Scope.Compile : Scope.fromName(scope);
    }

    private static GroupArtifactClassifierType gact(DependencyNode node) {
        Artifact artifact = node.getArtifact();
        return new GroupArtifactClassifierType(artifact.getGroupId(), artifact.getArtifactId(),
                emptyToNull(artifact.getClassifier()), artifact.getExtension());
    }

    private static @Nullable String emptyToNull(@Nullable String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    /** A mutable projection node, converted to the immutable {@link ResolvedDependency} once the tree is fully shaped. */
    static final class Node {
        GroupArtifactVersion gav;
        ResolvedGroupArtifactVersion resolvedGav;
        @Nullable MavenRepository repository;
        Dependency requested;
        @Nullable String type;
        @Nullable String classifier;
        Boolean optional;
        int depth;
        List<License> licenses = emptyList();
        final List<Node> children = new ArrayList<>();
        final List<GroupArtifact> effectiveExclusions = new ArrayList<>();
    }

    private static final class Frame {
        final DependencyNode aether;
        final Scope propagationScope;
        final @Nullable Node parent;
        final Dependency requested;

        Frame(DependencyNode aether, Scope propagationScope, @Nullable Node parent, @Nullable Dependency requested) {
            this.aether = aether;
            this.propagationScope = propagationScope;
            this.parent = parent;
            this.requested = requested == null ? synthetic(aether.getArtifact()) : requested;
        }
    }

    /** {@code gav → repository / cached Pom} lookups derived from the collect's {@code servedBy} attribution. */
    static final class Index {
        private final Map<GroupArtifactVersion, MavenRepository> repositoryByGav = new HashMap<>();
        private final Map<GroupArtifactVersion, ResolvedGroupArtifactVersion> keyByGav = new HashMap<>();
        private final MavenPomCache pomCache;

        Index(Map<ResolvedGroupArtifactVersion, MavenRepository> servedBy, MavenPomCache pomCache) {
            this.pomCache = pomCache;
            for (Map.Entry<ResolvedGroupArtifactVersion, MavenRepository> entry : servedBy.entrySet()) {
                ResolvedGroupArtifactVersion key = entry.getKey();
                GroupArtifactVersion gav = new GroupArtifactVersion(key.getGroupId(), key.getArtifactId(), key.getVersion());
                repositoryByGav.putIfAbsent(gav, entry.getValue());
                keyByGav.putIfAbsent(gav, key);
            }
        }

        @Nullable MavenRepository repoFor(GroupArtifactVersion gav) {
            MavenRepository repository = repositoryByGav.get(gav);
            // A reactor-served pom stands in with a synthetic repository; it is a project dependency (null repository).
            return repository == null || "reactor:workspace".equals(repository.getUri()) ? null : repository;
        }

        @Nullable Pom pomFor(GroupArtifactVersion gav) {
            ResolvedGroupArtifactVersion key = keyByGav.get(gav);
            if (key == null) {
                return null;
            }
            try {
                Optional<Pom> pom = pomCache.getPom(key);
                //noinspection OptionalAssignedToNull
                if (pom != null && pom.isPresent()) {
                    return pom.get();
                }
            } catch (Exception ignored) {
                // treat as absent
            }
            return null;
        }

        List<License> licensesFor(GroupArtifactVersion gav) {
            Pom pom = pomFor(gav);
            return pom == null ? emptyList() : pom.getLicenses();
        }
    }
}
