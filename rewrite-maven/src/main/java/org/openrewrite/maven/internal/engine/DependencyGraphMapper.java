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
        Index index = new Index(outcome.getServedBy(), outcome.getDeclaredDependencies(), pomCache);
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
            result.put(scope, project(outcome.getRoot(), scope, requestedDependencies, engineResolvedPom, index));
        }

        if (exceptions != null) {
            throw exceptions.setPartialResult(partial(engineResolvedPom, result));
        }
        return result;
    }

    // ---- per-scope projection ----

    private List<ResolvedDependency> project(@Nullable DependencyNode root, Scope scope,
                                             List<Dependency> requestedDependencies, ResolvedPom rootPom, Index index) {
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
                seeds.putIfAbsent(gact(child), new Frame(child, Scope.Compile, null, matchRequested(child, requestedDependencies, rootPom)));
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
                List<Pom> declaringLineage = index.lineageFor(node.gav);
                for (DependencyNode child : winners(frame.aether)) {
                    // The child's scope as DECLARED in its containing pom (legacy ResolvedPom.getDependencyScope) — not
                    // aether's scope, which the JavaScopeDeriver has already promoted (a compile dependency of a
                    // test/provided parent becomes test/provided). Legacy propagates every root as Compile, so a
                    // test-scoped direct dependency's compile transitives stay compile-reachable and populate the
                    // Test/Provided classpath (verified against `mvn dependency:tree`).
                    Scope childScope = childScope(child, declaringLineage, rootPom);
                    if (childScope.isInClasspathOf(frame.propagationScope)) {
                        next.putIfAbsent(gact(child), new Frame(child, childScope, node, transitiveRequested(child, declaringLineage)));
                    }
                }
            }
            level = new ArrayList<>(next.values());
            depth++;
        }

        new ExclusionAttributor(index::declaredDepsFor).attribute(roots);

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
        // Effective exclusions (requested + dependencyManagement-sourced) prune the accumulated set; the requested-only
        // subset drives legacy's attribution walk (which ancestor an effective exclusion is reported on).
        node.ownExclusions = toGroupArtifacts(frame.aether.getDependency() == null ? null :
                frame.aether.getDependency().getExclusions());
        node.requestedExclusions = (frame.aether.getManagedBits() & DependencyNode.MANAGED_EXCLUSIONS) != 0 ?
                toGroupArtifacts(DependencyManagerUtils.getPremanagedExclusions(frame.aether)) : node.ownExclusions;
        return node;
    }

    private static List<GroupArtifact> toGroupArtifacts(@Nullable Collection<org.openrewrite.maven.engine.shaded.org.eclipse.aether.graph.Exclusion> exclusions) {
        if (exclusions == null || exclusions.isEmpty()) {
            return emptyList();
        }
        List<GroupArtifact> out = new ArrayList<>(exclusions.size());
        for (org.openrewrite.maven.engine.shaded.org.eclipse.aether.graph.Exclusion e : exclusions) {
            out.add(new GroupArtifact(e.getGroupId(), e.getArtifactId()));
        }
        return out;
    }

    // The original declared Dependency instance from the requesting pom, so getResolvedDependency stays reference-exact.
    // Duplicate direct declarations resolve last-declaration-wins, mirroring legacy's g:a-keyed rootDependencies map
    // (a later put replacing an earlier), so a doubly-declared coordinate threads its LAST declared instance.
    private @Nullable Dependency matchRequested(DependencyNode node, List<Dependency> requestedDependencies, ResolvedPom rootPom) {
        Artifact artifact = node.getArtifact();
        String classifier = emptyToNull(artifact.getClassifier());
        Dependency exact = null;
        Dependency byGa = null;
        // Interpolate each requested coordinate against the root pom's effective properties (groupId/artifactId can both
        // be ${...}, possibly inherited), so the depth-0 requested threads the original declared instance — the identity
        // getResolvedDependency relies on — rather than a value-equal synthetic.
        for (Dependency dependency : requestedDependencies) {
            if (!artifact.getArtifactId().equals(rootPom.getValue(dependency.getArtifactId())) ||
                    !artifact.getGroupId().equals(rootPom.getValue(dependency.getGroupId()))) {
                continue;
            }
            byGa = dependency;
            if (Objects.equals(emptyToNull(rootPom.getValue(dependency.getClassifier())), classifier)) {
                exact = dependency;
            }
        }
        if (exact != null) {
            return exact;
        }
        return byGa != null ? byGa : synthetic(artifact);
    }

    // A transitive node's requested coordinates match what the legacy BFS threads as ResolvedDependency.requested: the
    // declaring pom's *parent-merged* declared version (legacy's getRequestedDependencies merges ancestor-declared
    // <dependencies>, child-wins), interpolated through the lineage's properties exactly as ResolvedPom.getValue —
    // null when declared without a version (management supplied it), the literal when a placeholder cannot resolve.
    // Fall back to the aether (pre-management) version only when no lineage pom declares the coordinate at all.
    private static Dependency transitiveRequested(DependencyNode node, List<Pom> declaringLineage) {
        Artifact artifact = node.getArtifact();
        Dependency declared = matchDeclared(declaringLineage, artifact);
        String version;
        if (declared != null) {
            version = declared.getVersion();
            if (version != null && version.contains("${")) {
                version = interpolate(declaringLineage, version);
            }
        } else {
            version = artifact.getBaseVersion();
            if ((node.getManagedBits() & DependencyNode.MANAGED_VERSION) != 0) {
                String premanaged = DependencyManagerUtils.getPremanagedVersion(node);
                if (premanaged != null) {
                    version = premanaged;
                }
            }
        }
        return Dependency.builder()
                .gav(new GroupArtifactVersion(artifact.getGroupId(), artifact.getArtifactId(), version))
                .classifier(emptyToNull(artifact.getClassifier()))
                .type(artifact.getExtension())
                .build();
    }

    // The declaring instance for a coordinate: the declaring pom's own <dependencies> first, then each ancestor's
    // (child-wins, mirroring legacy's parent-merged getRequestedDependencies).
    private static @Nullable Dependency matchDeclared(List<Pom> declaringLineage, Artifact artifact) {
        String classifier = emptyToNull(artifact.getClassifier());
        for (Pom pom : declaringLineage) {
            Dependency byGa = null;      // exact interpolated g:a match, classifier not yet confirmed
            Dependency byArtifact = null; // artifactId (+classifier) match, groupId a placeholder we could not resolve
            for (Dependency d : pom.getDependencies()) {
                if (!artifact.getArtifactId().equals(d.getArtifactId())) {
                    continue;
                }
                boolean classifierMatches = Objects.equals(emptyToNull(d.getClassifier()), classifier);
                if (groupIdMatches(declaringLineage, d.getGroupId(), artifact.getGroupId())) {
                    if (classifierMatches) {
                        return d;
                    }
                    if (byGa == null) {
                        byGa = d;
                    }
                } else if (classifierMatches && byArtifact == null) {
                    byArtifact = d;
                }
            }
            if (byGa != null || byArtifact != null) {
                return byGa != null ? byGa : byArtifact;
            }
        }
        return null;
    }

    // A declared groupId may be inherited (null → the containing pom's) or an uninterpolated placeholder like
    // ${project.groupId}; resolve those against the declaring lineage before comparing, so the raw declared version
    // still threads onto the child (legacy interpolates the coordinate but keeps the requested version raw).
    private static boolean groupIdMatches(List<Pom> declaringLineage, @Nullable String declaredGroupId, String artifactGroupId) {
        if (declaredGroupId == null || artifactGroupId.equals(declaredGroupId)) {
            return true;
        }
        return declaredGroupId.contains("${") && artifactGroupId.equals(interpolate(declaringLineage, declaredGroupId));
    }

    // Mirrors ResolvedPom.getValue over the declaring pom's raw-lineage property merge (child-first, first-wins):
    // placeholders resolve recursively; an unresolvable placeholder stays literal, exactly as legacy threads it.
    private static @Nullable String interpolate(List<Pom> lineage, String value) {
        return ResolvedPom.placeholderHelper.replacePlaceholders(value, key -> lineageProperty(lineage, key));
    }

    private static @Nullable String lineageProperty(List<Pom> lineage, String key) {
        if (lineage.isEmpty()) {
            return null;
        }
        Pom pom = lineage.get(0);
        switch (key) {
            case "groupId":
            case "project.groupId":
            case "pom.groupId":
                return pom.getGroupId();
            case "project.parent.groupId":
            case "parent.groupId":
                return pom.getParent() == null ? null : pom.getParent().getGroupId();
            case "artifactId":
            case "project.artifactId":
            case "pom.artifactId":
                return pom.getArtifactId();
            case "project.parent.artifactId":
            case "parent.artifactId":
                return pom.getParent() == null ? null : pom.getParent().getArtifactId();
            case "version":
            case "project.version":
            case "pom.version":
                return pom.getVersion();
            case "project.parent.version":
            case "parent.version":
                return pom.getParent() == null ? null : pom.getParent().getVersion();
            default:
                for (Pom p : lineage) {
                    String v = p.getProperties().get(key);
                    if (v != null) {
                        return v;
                    }
                }
                return null;
        }
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

    // Legacy ResolvedPom.getDependencyScope: the child's scope declared in its containing pom (else Compile), then the
    // root pom's managed scope only when that would not promote the dependency into a classpath it was not already in.
    private static Scope childScope(DependencyNode child, List<Pom> declaringLineage, ResolvedPom rootPom) {
        Artifact a = child.getArtifact();
        Dependency declared = matchDeclared(declaringLineage, a);
        Scope inContaining = declared != null && declared.getScope() != null ?
                Scope.fromName(declared.getScope()) : Scope.Compile;
        Scope inProject = rootPom.getManagedScope(a.getGroupId(), a.getArtifactId(), a.getExtension(),
                emptyToNull(a.getClassifier()));
        Scope legacyScope = inProject == null ? inContaining :
                (inContaining.isInClasspathOf(inProject) ? inProject : inContaining);
        // Maven selects a coordinate's effective scope as the widest (most-visible) across every path that reaches its
        // winning version; aether records that selection as the winner edge's scope. When the raw declared scope in the
        // containing pom is NARROWER than what Maven kept (e.g. grpc-core declares error_prone_annotations `runtime`
        // while grpc-api declares it `compile`, so Maven keeps it `compile`), trust aether's wider scope so the
        // coordinate lands in the classpaths Maven does. When aether instead PROMOTED it narrower — a compile child of a
        // test/provided parent that its JavaScopeDeriver moved to test/provided — the declared scope is wider and wins,
        // preserving the test-closure population (L-P3-D-001).
        Scope aetherScope = scopeOf(child);
        return Scope.maxPrecedence(aetherScope, legacyScope) == aetherScope && aetherScope != legacyScope ?
                aetherScope : legacyScope;
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
        List<GroupArtifact> ownExclusions = emptyList();      // requested + managed — prunes the accumulated set
        List<GroupArtifact> requestedExclusions = emptyList(); // requested only — drives the attribution walk
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
        private final Map<GroupArtifactVersion, List<GroupArtifact>> declaredDependencies;
        private final MavenPomCache pomCache;

        Index(Map<ResolvedGroupArtifactVersion, MavenRepository> servedBy,
              Map<GroupArtifactVersion, List<GroupArtifact>> declaredDependencies, MavenPomCache pomCache) {
            this.declaredDependencies = declaredDependencies;
            this.pomCache = pomCache;
            for (Map.Entry<ResolvedGroupArtifactVersion, MavenRepository> entry : servedBy.entrySet()) {
                ResolvedGroupArtifactVersion key = entry.getKey();
                GroupArtifactVersion gav = new GroupArtifactVersion(key.getGroupId(), key.getArtifactId(), key.getVersion());
                repositoryByGav.putIfAbsent(gav, entry.getValue());
                keyByGav.putIfAbsent(gav, key);
            }
        }

        // The node's effective (parent-merged) declared dependencies captured at descriptor-read time; falls back to the
        // cached pom's own <dependencies> when the descriptor was never read (e.g. a purely reactor-local pom).
        List<GroupArtifact> declaredDepsFor(GroupArtifactVersion gav) {
            List<GroupArtifact> declared = declaredDependencies.get(gav);
            if (declared != null) {
                return declared;
            }
            Pom pom = pomFor(gav);
            if (pom == null) {
                return emptyList();
            }
            List<GroupArtifact> out = new ArrayList<>(pom.getDependencies().size());
            for (Dependency d : pom.getDependencies()) {
                out.add(new GroupArtifact(d.getGroupId() == null ? "" : d.getGroupId(), d.getArtifactId()));
            }
            return out;
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

        // The raw pom plus its ancestors (child-first), resolved through the collect's servedBy attribution; a parent
        // gav's ${...} coordinates are interpolated against the chain gathered so far. Cycle-guarded.
        List<Pom> lineageFor(GroupArtifactVersion gav) {
            List<Pom> lineage = new ArrayList<>(4);
            Set<GroupArtifactVersion> visited = new HashSet<>();
            for (Pom pom = pomFor(gav); pom != null; ) {
                GroupArtifactVersion id = new GroupArtifactVersion(pom.getGroupId(), pom.getArtifactId(), pom.getVersion());
                if (!visited.add(id)) {
                    break;
                }
                lineage.add(pom);
                Parent parent = pom.getParent();
                if (parent == null) {
                    break;
                }
                GroupArtifactVersion parentGav = parent.getGav();
                String parentGroupId = resolveCoordinate(lineage, parentGav.getGroupId());
                String parentVersion = resolveCoordinate(lineage, parentGav.getVersion());
                if (parentGroupId == null || parentVersion == null) {
                    break;
                }
                pom = pomFor(new GroupArtifactVersion(parentGroupId, parentGav.getArtifactId(), parentVersion));
            }
            return lineage;
        }

        private static @Nullable String resolveCoordinate(List<Pom> lineage, @Nullable String value) {
            if (value == null || !value.contains("${")) {
                return value;
            }
            String resolved = interpolate(lineage, value);
            return resolved == null || resolved.contains("${") ? null : resolved;
        }

        List<License> licensesFor(GroupArtifactVersion gav) {
            Pom pom = pomFor(gav);
            return pom == null ? emptyList() : pom.getLicenses();
        }
    }
}
