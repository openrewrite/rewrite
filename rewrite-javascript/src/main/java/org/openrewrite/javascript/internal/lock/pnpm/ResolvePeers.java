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
package org.openrewrite.javascript.internal.lock.pnpm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.ValidateNpmPackageName;
import org.openrewrite.javascript.internal.lock.YarnpkgSemverUtils;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.DependenciesTreeNode;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.MissingPeerInfo;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.NodeId;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.PeerDependency;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.ResolvedPackage;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Port of @pnpm/installing.deps-resolver src/resolvePeers.ts, src/mergePeers.ts and src/linkPathToPeerVersion.ts
 * (pnpm 11.2.2).
 */
final class ResolvePeers {

    private ResolvePeers() {
    }

    /**
     * {@code DependenciesGraphNode}: a {@link ResolvedPackage} at a dependency path.
     */
    @Getter
    @Setter
    static final class DependenciesGraphNode {
        private final String depPath;
        private final ResolvedPackage resolvedPackage;
        private int depth;
        private Map<String, NodeId> childrenNodeIds = new LinkedHashMap<>();
        private Map<String, String> children = new LinkedHashMap<>();
        private Map<String, PeerDependency> peerDependencies = new LinkedHashMap<>();
        private Set<String> transitivePeerDependencies = new LinkedHashSet<>();
        private Set<String> resolvedPeerNames = new LinkedHashSet<>();
        private boolean installable;
        private boolean isPure;
        private boolean optional;

        DependenciesGraphNode(String depPath, ResolvedPackage resolvedPackage) {
            this.depPath = depPath;
            this.resolvedPackage = resolvedPackage;
            this.optional = resolvedPackage.isOptional();
        }
    }

    /**
     * A project's direct dependencies by alias, and the linked packages above them.
     */
    @Value
    @AllArgsConstructor
    static class ProjectToResolve {
        String id;
        String rootDir;
        Map<String, NodeId> directNodeIdsByAlias;
        List<TopParent> topParents;

        /**
         * From 11.5.2: the dependencies the manifest declares and those the install adds, and those it adds, updates
         * or changes the specifier of, which win over the peer contexts the lock records.
         */
        @Nullable
        Set<String> declaredDirectDependencies;

        @Nullable
        Set<String> explicitlyRequestedDirectDependencies;

        /**
         * From 11.11.0: the direct dependencies' nodes that are peer providers attached for reuse, resolved after the
         * project's own ({@link PnpmRelease#resolvesHoistedPeerProvidersAfterOwnDependencies}).
         */
        @Nullable
        Set<NodeId> hoistedPeerProviderNodeIds;

        ProjectToResolve(String id, String rootDir, Map<String, NodeId> directNodeIdsByAlias,
                         List<TopParent> topParents) {
            this(id, rootDir, directNodeIdsByAlias, topParents, null, null);
        }

        ProjectToResolve(String id, String rootDir, Map<String, NodeId> directNodeIdsByAlias,
                         List<TopParent> topParents, @Nullable Set<String> declaredDirectDependencies,
                         @Nullable Set<String> explicitlyRequestedDirectDependencies) {
            this(id, rootDir, directNodeIdsByAlias, topParents, declaredDirectDependencies,
                    explicitlyRequestedDirectDependencies, null);
        }
    }

    @Value
    static class TopParent {
        String name;
        String version;

        @Nullable
        String alias;

        @Nullable
        NodeId linkedDir;
    }

    @Value
    static class Result {
        Map<String, DependenciesGraphNode> dependenciesGraph;
        Map<String, Map<String, String>> dependenciesByProjectId;
        Map<String, PeerDependencyIssues> peerDependencyIssuesByProjects;

        /**
         * The dep path of each node, which 11.5.2's second pass starts from.
         */
        Map<NodeId, String> pathsByNodeId;
    }

    @Value
    static class ParentPackage {
        String name;
        String version;
    }

    @Value
    static class MissingPeerIssue {
        List<ParentPackage> parents;
        boolean optional;
        String wantedRange;
    }

    @Value
    static class BadPeerIssue {
        String foundVersion;
        List<ParentPackage> resolvedFrom;
        List<ParentPackage> parents;
        boolean optional;
        String wantedRange;
    }

    /**
     * The peers a project's packages miss or get at a version their range does not admit, with mergePeers' view of
     * the missing ones.
     */
    @Getter
    static final class PeerDependencyIssues {
        private final Map<String, List<BadPeerIssue>> bad;
        private final Map<String, List<MissingPeerIssue>> missing;
        private final List<String> conflicts;
        private final Map<String, String> intersections;

        PeerDependencyIssues() {
            this(new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>(), new LinkedHashMap<>());
        }

        PeerDependencyIssues(Map<String, List<BadPeerIssue>> bad, Map<String, List<MissingPeerIssue>> missing,
                             List<String> conflicts, Map<String, String> intersections) {
            this.bad = bad;
            this.missing = missing;
            this.conflicts = conflicts;
            this.intersections = intersections;
        }
    }

    /**
     * A JavaScript promise of a dependency path: settled once, its reactions queued.
     */
    static final class Deferred {
        private final Deque<Runnable> microtasks;
        private @Nullable String value;
        private final List<Consumer<String>> reactions = new ArrayList<>();

        Deferred(Deque<Runnable> microtasks) {
            this.microtasks = microtasks;
        }

        void resolve(String depPath) {
            if (value != null) {
                return;
            }
            value = depPath;
            for (Consumer<String> reaction : reactions) {
                microtasks.add(() -> reaction.accept(depPath));
            }
            reactions.clear();
        }

        void then(Consumer<String> reaction) {
            if (value != null) {
                String v = value;
                microtasks.add(() -> reaction.accept(v));
            } else {
                reactions.add(reaction);
            }
        }
    }

    private static final class ParentRef {
        final String version;
        final int depth;
        final @Nullable NodeId nodeId;
        final @Nullable String alias;
        int occurrence;
        final List<NodeId> parentNodeIds;

        ParentRef(String version, int depth, @Nullable NodeId nodeId, @Nullable String alias, int occurrence,
                  List<NodeId> parentNodeIds) {
            this.version = version;
            this.depth = depth;
            this.nodeId = nodeId;
            this.alias = alias;
            this.occurrence = occurrence;
            this.parentNodeIds = parentNodeIds;
        }

        ParentRef copy() {
            return new ParentRef(version, depth, nodeId, alias, occurrence, parentNodeIds);
        }
    }

    private static final class ParentPkgInfo {
        @Nullable String pkgIdWithPatchHash;
        @Nullable String version;
        @Nullable Integer depth;
        @Nullable Integer occurrence;
    }

    @Value
    private static class ParentPkgNode {
        String alias;
        DependenciesTreeNode node;
        NodeId nodeId;
        List<NodeId> parentNodeIds;
    }

    @Value
    private static class PendingPeer {
        String alias;
        NodeId nodeId;
    }

    /**
     * {@code PeersResolution}: the peers resolved for the children, by alias, and those they miss.
     */
    @Value
    private static class PeersResolution {
        Map<String, NodeId> resolvedPeers;
        Map<String, MissingPeerInfo> missingPeers;
    }

    private static final class PeersCacheItem {
        final Deferred depPath;
        final Map<String, NodeId> resolvedPeers;
        final Map<String, MissingPeerInfo> missingPeers;

        // the node that resolved it (11.12.0's ownerNodeId)
        final NodeId ownerNodeId;

        PeersCacheItem(Deferred depPath, Map<String, NodeId> resolvedPeers, Map<String, MissingPeerInfo> missingPeers,
                       NodeId ownerNodeId) {
            this.depPath = depPath;
            this.resolvedPeers = resolvedPeers;
            this.missingPeers = missingPeers;
            this.ownerNodeId = ownerNodeId;
        }
    }

    private static final class Ctx {
        final Set<String> allPeerDepNames;
        final Map<NodeId, Map<String, ParentPkgInfo>> parentPkgsOfNode;
        final Map<NodeId, DependenciesTreeNode> dependenciesTree;
        final Map<String, DependenciesGraphNode> depGraph;
        final Map<NodeId, String> pathsByNodeId;
        final Map<NodeId, Deferred> pathsByNodeIdPromises;
        final Map<String, Set<String>> depPathsByPkgId;
        final Map<String, List<PeersCacheItem>> peersCache;
        final PeerDependencyIssues peerDependencyIssues;
        final Set<String> purePkgs;
        final boolean dedupePeers;
        final int peersSuffixMaxLength;
        final Deque<Runnable> microtasks;
        final List<NodeId> parentNodeIds;
        final List<String> parentDepPathsChain;
        final int[] pending;
        final PnpmRelease release;

        // 11.5.2: the first node of each dep path the lock records, the paths the first pass gave the nodes (null on
        // the first pass), and the providers of the project's peers whose choice must win over the lock's
        final Map<String, NodeId> nodeIdsByPreviousDepPath;
        final @Nullable Map<NodeId, String> resolvedPeerProviderPaths;
        final List<CurrentProviderSource> currentProviderSources;

        // 11.12.0: which nodes wait on which peers' paths, whose peers cache a node took, and the nodes whose
        // path a cycle settled as name@version
        final Map<NodeId, Set<NodeId>> awaitedPeerNodeIdsByNodeId = new LinkedHashMap<>();
        final Map<NodeId, NodeId> peersCacheOwnerByNodeId = new LinkedHashMap<>();
        final Set<NodeId> cycleBrokenNodeIds = new LinkedHashSet<>();
        @Nullable Ctx run;

        // 11.11.0: the nodes the project's walk gave their parents' context, what pnpm's parentPkgsOfNode of the
        // project holds (the port shares one of them across projects)
        Set<NodeId> projectNodeIds = new LinkedHashSet<>();

        Ctx(Set<String> allPeerDepNames, Map<NodeId, Map<String, ParentPkgInfo>> parentPkgsOfNode,
            Map<NodeId, DependenciesTreeNode> dependenciesTree, Map<String, DependenciesGraphNode> depGraph,
            Map<NodeId, String> pathsByNodeId, Map<NodeId, Deferred> pathsByNodeIdPromises,
            Map<String, Set<String>> depPathsByPkgId, Map<String, List<PeersCacheItem>> peersCache,
            PeerDependencyIssues peerDependencyIssues, Set<String> purePkgs, boolean dedupePeers,
            int peersSuffixMaxLength, Deque<Runnable> microtasks, List<NodeId> parentNodeIds,
            List<String> parentDepPathsChain, int[] pending, PnpmRelease release,
            Map<String, NodeId> nodeIdsByPreviousDepPath, @Nullable Map<NodeId, String> resolvedPeerProviderPaths,
            List<CurrentProviderSource> currentProviderSources) {
            this.allPeerDepNames = allPeerDepNames;
            this.parentPkgsOfNode = parentPkgsOfNode;
            this.dependenciesTree = dependenciesTree;
            this.depGraph = depGraph;
            this.pathsByNodeId = pathsByNodeId;
            this.pathsByNodeIdPromises = pathsByNodeIdPromises;
            this.depPathsByPkgId = depPathsByPkgId;
            this.peersCache = peersCache;
            this.peerDependencyIssues = peerDependencyIssues;
            this.purePkgs = purePkgs;
            this.dedupePeers = dedupePeers;
            this.peersSuffixMaxLength = peersSuffixMaxLength;
            this.microtasks = microtasks;
            this.parentNodeIds = parentNodeIds;
            this.parentDepPathsChain = parentDepPathsChain;
            this.pending = pending;
            this.release = release;
            this.nodeIdsByPreviousDepPath = nodeIdsByPreviousDepPath;
            this.resolvedPeerProviderPaths = resolvedPeerProviderPaths;
            this.currentProviderSources = currentProviderSources;
        }

        Ctx with(List<NodeId> parentNodeIds, List<String> parentDepPathsChain) {
            Ctx copy = new Ctx(allPeerDepNames, parentPkgsOfNode, dependenciesTree, depGraph, pathsByNodeId,
                    pathsByNodeIdPromises, depPathsByPkgId, peersCache, peerDependencyIssues, purePkgs, dedupePeers,
                    peersSuffixMaxLength, microtasks, parentNodeIds, parentDepPathsChain, pending, release,
                    nodeIdsByPreviousDepPath, resolvedPeerProviderPaths, currentProviderSources);
            copy.run = shared();
            copy.projectNodeIds = projectNodeIds;
            return copy;
        }

        Ctx forProject(PeerDependencyIssues issues, List<CurrentProviderSource> sources) {
            Ctx copy = new Ctx(allPeerDepNames, parentPkgsOfNode, dependenciesTree, depGraph, pathsByNodeId,
                    pathsByNodeIdPromises, depPathsByPkgId, peersCache, issues, purePkgs, dedupePeers,
                    peersSuffixMaxLength, microtasks, parentNodeIds, parentDepPathsChain, pending, release,
                    nodeIdsByPreviousDepPath, resolvedPeerProviderPaths, sources);
            copy.run = shared();
            return copy;
        }

        // the context of the whole run, which holds what 11.12.0 tracks across it
        Ctx shared() {
            return run == null ? this : run;
        }

        void drain() {
            Runnable task;
            while ((task = microtasks.poll()) != null) {
                task.run();
            }
        }
    }

    /**
     * {@code currentProviderSources}' entry (11.5.2): a project's direct dependencies, those its manifest declares,
     * and those the install asks for anew.
     */
    @Value
    private static class CurrentProviderSource {
        Map<String, NodeId> directNodeIdsByAlias;
        Set<String> declaredDirectDependencies;
        Set<String> explicitlyRequestedDirectDependencies;
    }

    private static final class NodeResolution {
        final Map<String, NodeId> resolvedPeers;
        final Map<String, MissingPeerInfo> missingPeers;
        final @Nullable Consumer<List<List<String>>> calculateDepPath;

        NodeResolution(Map<String, NodeId> resolvedPeers, Map<String, MissingPeerInfo> missingPeers,
                       @Nullable Consumer<List<List<String>>> calculateDepPath) {
            this.resolvedPeers = resolvedPeers;
            this.missingPeers = missingPeers;
            this.calculateDepPath = calculateDepPath;
        }
    }

    static Result resolvePeers(Set<String> allPeerDepNames, List<ProjectToResolve> projects,
                               Map<NodeId, DependenciesTreeNode> dependenciesTree,
                               boolean resolvePeersFromWorkspaceRoot,
                               boolean dedupePeerDependents, boolean dedupePeers, boolean dedupeInjectedDeps,
                               int peersSuffixMaxLength, String lockfileDir,
                               Map<String, ResolveDependencyTree.ResolvedImporter> resolvedImporters,
                               Set<String> workspaceProjectIds) {
        return resolvePeers(allPeerDepNames, projects, dependenciesTree, resolvePeersFromWorkspaceRoot,
                dedupePeerDependents, dedupePeers, dedupeInjectedDeps, peersSuffixMaxLength, lockfileDir,
                resolvedImporters, workspaceProjectIds, PnpmRelease.PORTED);
    }

    static Result resolvePeers(Set<String> allPeerDepNames, List<ProjectToResolve> projects,
                               Map<NodeId, DependenciesTreeNode> dependenciesTree,
                               boolean resolvePeersFromWorkspaceRoot,
                               boolean dedupePeerDependents, boolean dedupePeers, boolean dedupeInjectedDeps,
                               int peersSuffixMaxLength, String lockfileDir,
                               Map<String, ResolveDependencyTree.ResolvedImporter> resolvedImporters,
                               Set<String> workspaceProjectIds, PnpmRelease release) {
        return resolvePeers(allPeerDepNames, projects, dependenciesTree, resolvePeersFromWorkspaceRoot,
                dedupePeerDependents, dedupePeers, dedupeInjectedDeps, peersSuffixMaxLength, lockfileDir,
                resolvedImporters, workspaceProjectIds, release, null);
    }

    /**
     * The peers of every node, a second pass of 11.5.2 on taking the paths the first gave the nodes
     * ({@code resolvedPeerProviderPaths}), so that the peer contexts the lock records are kept where they still hold.
     */
    static Result resolvePeers(Set<String> allPeerDepNames, List<ProjectToResolve> projects,
                               Map<NodeId, DependenciesTreeNode> dependenciesTree,
                               boolean resolvePeersFromWorkspaceRoot,
                               boolean dedupePeerDependents, boolean dedupePeers, boolean dedupeInjectedDeps,
                               int peersSuffixMaxLength, String lockfileDir,
                               Map<String, ResolveDependencyTree.ResolvedImporter> resolvedImporters,
                               Set<String> workspaceProjectIds, PnpmRelease release,
                               @Nullable Map<NodeId, String> resolvedPeerProviderPaths) {
        Map<String, DependenciesGraphNode> depGraph = new LinkedHashMap<>();
        Map<NodeId, String> pathsByNodeId = new LinkedHashMap<>();
        Map<NodeId, Deferred> pathsByNodeIdPromises = new LinkedHashMap<>();
        Map<String, Set<String>> depPathsByPkgId = new LinkedHashMap<>();
        Map<String, NodeId> nodeIdsByPreviousDepPath = resolvedPeerProviderPaths == null ? new LinkedHashMap<>() :
                getNodeIdsByPreviousDepPath(dependenciesTree);
        ProjectToResolve workspaceRootProject = null;
        if (resolvePeersFromWorkspaceRoot && projects.size() > 1) {
            for (ProjectToResolve project : projects) {
                if (".".equals(project.getId())) {
                    workspaceRootProject = project;
                    break;
                }
            }
        }
        Map<String, ParentRef> rootPkgsByName = resolvePeersFromWorkspaceRoot ?
                getRootPkgsByName(dependenciesTree, projects) : new LinkedHashMap<>();
        Map<String, PeerDependencyIssues> peerDependencyIssuesByProjects = new LinkedHashMap<>();
        Deque<Runnable> microtasks = new ArrayDeque<>();
        int[] pending = {0};
        Ctx base = new Ctx(allPeerDepNames, new LinkedHashMap<>(), dependenciesTree, depGraph, pathsByNodeId,
                pathsByNodeIdPromises, depPathsByPkgId, new LinkedHashMap<>(), new PeerDependencyIssues(),
                new LinkedHashSet<>(), dedupePeers, peersSuffixMaxLength, microtasks, new ArrayList<>(),
                new ArrayList<>(), pending, release, nodeIdsByPreviousDepPath, resolvedPeerProviderPaths,
                Collections.emptyList());
        for (ProjectToResolve project : projects) {
            List<CurrentProviderSource> currentProviderSources = new ArrayList<>();
            currentProviderSources.add(currentProviderSource(project));
            if (workspaceRootProject != null && !workspaceRootProject.getId().equals(project.getId())) {
                currentProviderSources.add(currentProviderSource(workspaceRootProject));
            }
            PeerDependencyIssues peerDependencyIssues = new PeerDependencyIssues();
            Map<String, ParentRef> merged = new LinkedHashMap<>(rootPkgsByName);
            merged.putAll(createPkgsByName(dependenciesTree, project.getDirectNodeIdsByAlias(),
                    project.getTopParents()));
            Map<String, ParentRef> pkgsByName = new LinkedHashMap<>();
            for (Map.Entry<String, ParentRef> entry : merged.entrySet()) {
                if (allPeerDepNames.contains(entry.getKey())) {
                    pkgsByName.put(entry.getKey(), entry.getValue());
                }
            }
            for (ParentRef ref : pkgsByName.values()) {
                if (ref.nodeId != null && !pathsByNodeIdPromises.containsKey(ref.nodeId)) {
                    pathsByNodeIdPromises.put(ref.nodeId, new Deferred(microtasks));
                }
            }
            Ctx ctx = base.forProject(peerDependencyIssues, currentProviderSources);
            // from 11.11.0 the peer providers attached to the project for reuse are resolved after its own direct
            // dependencies, and only those their walk did not reach (pnpm/pnpm#4993); before, with them at once
            // (PnpmRelease#resolvesHoistedPeerProvidersAfterOwnDependencies)
            Map<String, NodeId> ownDirectChildren = new LinkedHashMap<>();
            Map<String, NodeId> hoistedProviderChildren = new LinkedHashMap<>();
            Set<NodeId> hoistedPeerProviderNodeIds = project.getHoistedPeerProviderNodeIds();
            for (Map.Entry<String, NodeId> direct : project.getDirectNodeIdsByAlias().entrySet()) {
                (hoistedPeerProviderNodeIds != null && hoistedPeerProviderNodeIds.contains(direct.getValue()) ?
                        hoistedProviderChildren : ownDirectChildren).put(direct.getKey(), direct.getValue());
            }
            if (hoistedProviderChildren.isEmpty() || !release.resolvesHoistedPeerProvidersAfterOwnDependencies()) {
                resolvePeersOfChildren(new LinkedHashMap<>(project.getDirectNodeIdsByAlias()), pkgsByName, ctx);
                ctx.drain();
            } else {
                resolvePeersOfChildren(ownDirectChildren, pkgsByName, ctx);
                ctx.drain();
                Map<String, NodeId> prunedProviderChildren = new LinkedHashMap<>();
                for (Map.Entry<String, NodeId> provider : hoistedProviderChildren.entrySet()) {
                    if (!ctx.projectNodeIds.contains(provider.getValue())) {
                        prunedProviderChildren.put(provider.getKey(), provider.getValue());
                    }
                }
                if (!prunedProviderChildren.isEmpty()) {
                    resolvePeersOfChildren(prunedProviderChildren, pkgsByName, ctx);
                    ctx.drain();
                }
            }
            if (!peerDependencyIssues.getBad().isEmpty() || !peerDependencyIssues.getMissing().isEmpty()) {
                peerDependencyIssuesByProjects.put(project.getId(), mergePeers(peerDependencyIssues));
            }
        }
        base.drain();
        if (pending[0] != 0 && breakDepPathAwaitCycles(base, dependenciesTree) && release.breaksPeerPathAwaitCycles()) {
            base.drain();
        }
        if (pending[0] != 0) {
            throw new IllegalStateException("peers of " + pending[0] + " dependency paths never settled");
        }

        for (DependenciesGraphNode node : depGraph.values()) {
            Map<String, String> children = new LinkedHashMap<>();
            for (Map.Entry<String, NodeId> child : node.getChildrenNodeIds().entrySet()) {
                String path = pathsByNodeId.get(child.getValue());
                children.put(child.getKey(), path != null ? path : String.valueOf(child.getValue().getId()));
            }
            node.setChildren(children);
            node.setChildrenNodeIds(new LinkedHashMap<>());
        }

        Map<String, Map<String, String>> dependenciesByProjectId = new LinkedHashMap<>();
        for (ProjectToResolve project : projects) {
            Map<String, String> deps = new LinkedHashMap<>();
            for (Map.Entry<String, NodeId> direct : project.getDirectNodeIdsByAlias().entrySet()) {
                deps.put(direct.getKey(), pathsByNodeId.get(direct.getValue()));
            }
            dependenciesByProjectId.put(project.getId(), deps);
        }
        if (dedupeInjectedDeps) {
            DedupeInjectedDeps.dedupeInjectedDeps(dependenciesByProjectId, projects, depGraph, pathsByNodeId,
                    lockfileDir, resolvedImporters, workspaceProjectIds, release);
        }
        if (dedupePeerDependents) {
            List<Set<String>> duplicates = new ArrayList<>();
            for (Set<String> depPaths : depPathsByPkgId.values()) {
                if (depPaths.size() > 1) {
                    duplicates.add(depPaths);
                }
            }
            Map<String, String> allDepPathsMap = deduplicateAll(depGraph, duplicates, release);
            for (ProjectToResolve project : projects) {
                Map<String, String> deps = dependenciesByProjectId.get(project.getId());
                for (Map.Entry<String, String> dep : deps.entrySet()) {
                    String mapped = allDepPathsMap.get(dep.getValue());
                    if (mapped != null) {
                        dep.setValue(mapped);
                    }
                }
            }
        }
        return new Result(depGraph, dependenciesByProjectId, peerDependencyIssuesByProjects, pathsByNodeId);
    }

    private static int nodeDepsCount(DependenciesGraphNode node) {
        return node.getChildren().size() + node.getResolvedPeerNames().size();
    }

    static Map<String, String> deduplicateAll(Map<String, DependenciesGraphNode> depGraph,
                                              List<Set<String>> duplicates, PnpmRelease release) {
        Map<String, String> depPathsMap = new LinkedHashMap<>();
        List<Set<String>> remainingDuplicates = deduplicateDepPaths(duplicates, depGraph, depPathsMap, release);
        if (remainingDuplicates.size() == duplicates.size()) {
            return depPathsMap;
        }
        for (DependenciesGraphNode node : depGraph.values()) {
            for (Map.Entry<String, String> child : node.getChildren().entrySet()) {
                String mapped = depPathsMap.get(child.getValue());
                if (mapped != null) {
                    child.setValue(mapped);
                }
            }
        }
        if (!depPathsMap.isEmpty()) {
            Map<String, String> all = new LinkedHashMap<>(depPathsMap);
            all.putAll(deduplicateAll(depGraph, remainingDuplicates, release));
            return all;
        }
        return depPathsMap;
    }

    private static List<Set<String>> deduplicateDepPaths(List<Set<String>> duplicates,
                                                         Map<String, DependenciesGraphNode> depGraph,
                                                         Map<String, String> depPathsMap, PnpmRelease release) {
        Comparator<String> depCountSorter = (a, b) ->
                nodeDepsCount(depGraph.get(a)) - nodeDepsCount(depGraph.get(b));
        List<Set<String>> remainingDuplicates = new ArrayList<>();
        for (Set<String> depPaths : duplicates) {
            Set<String> unresolvedDepPaths = new LinkedHashSet<>(depPaths);
            List<String> currentDepPaths = sortByDepCount(new ArrayList<>(depPaths), depCountSorter, release);
            while (!currentDepPaths.isEmpty()) {
                String depPath1 = currentDepPaths.remove(currentDepPaths.size() - 1);
                List<String> nextDepPaths = new ArrayList<>();
                while (!currentDepPaths.isEmpty()) {
                    String depPath2 = currentDepPaths.remove(currentDepPaths.size() - 1);
                    if (isCompatibleAndHasMoreDeps(depGraph, depPath1, depPath2)) {
                        depPathsMap.put(depPath2, depPath1);
                        unresolvedDepPaths.remove(depPath1);
                        unresolvedDepPaths.remove(depPath2);
                    } else {
                        nextDepPaths.add(depPath2);
                    }
                }
                nextDepPaths.addAll(currentDepPaths);
                currentDepPaths = sortByDepCount(nextDepPaths, depCountSorter, release);
            }
            if (!unresolvedDepPaths.isEmpty()) {
                remainingDuplicates.add(unresolvedDepPaths);
            }
        }
        return remainingDuplicates;
    }

    // a stable sort by the count of deps; from 11.5.3 a tie goes by dep path
    // (PnpmRelease#breaksPeerDedupeTiesByDepPath), asked where the two orders differ
    private static List<String> sortByDepCount(List<String> depPaths, Comparator<String> depCountSorter,
                                               PnpmRelease release) {
        List<String> byDepPath = new ArrayList<>(depPaths);
        byDepPath.sort(depCountSorter.thenComparing(Comparator.naturalOrder()));
        depPaths.sort(depCountSorter);
        return depPaths.equals(byDepPath) || !release.breaksPeerDedupeTiesByDepPath() ? depPaths : byDepPath;
    }

    // depPathCompatibility.ts' isCompatibleAndHasMoreDeps, which dedupeInjectedDeps asks too from 11.13.0
    static boolean isCompatibleAndHasMoreDeps(Map<String, DependenciesGraphNode> depGraph, String depPath1,
                                              String depPath2) {
        DependenciesGraphNode node1 = depGraph.get(depPath1);
        DependenciesGraphNode node2 = depGraph.get(depPath2);
        if (nodeDepsCount(node1) < nodeDepsCount(node2)) {
            return false;
        }
        Set<String> node1DepPaths = new LinkedHashSet<>(node1.getChildren().values());
        for (String depPath : node2.getChildren().values()) {
            if (!node1DepPaths.contains(depPath)) {
                return false;
            }
        }
        for (String name : node2.getResolvedPeerNames()) {
            if (!node1.getResolvedPeerNames().contains(name)) {
                return false;
            }
        }
        return true;
    }

    // 11.5.2's second pass: each peer the lock records the node with takes the provider it recorded, where that
    // provider kept its dep path, has no peers of its own, satisfies the range and no current choice must win
    @SuppressWarnings("ConstantConditions")
    private static Map<String, ParentRef> keepLockedPeerContext(DependenciesTreeNode node,
                                                               ResolvedPackage resolvedPackage,
                                                               Map<String, ParentRef> parentPkgs,
                                                               Map<String, ParentRef> parentParentPkgs,
                                                               List<NodeId> parentNodeIds,
                                                               Ctx ctx) {
        for (Map.Entry<String, String> locked : node.getLockedPeerContext().entrySet()) {
            String peerName = locked.getKey();
            String previousPeerDepPath = locked.getValue();
            NodeId peerNodeId = ctx.nodeIdsByPreviousDepPath.get(previousPeerDepPath);
            PeerDependency peerDependency = resolvedPackage.getPeerDependencies().get(peerName);
            if (peerNodeId == null || peerDependency == null) {
                continue;
            }
            if (!previousPeerDepPath.equals(ctx.resolvedPeerProviderPaths.get(peerNodeId))) {
                continue;
            }
            if (DepPath.indexOfDepPathSuffix(previousPeerDepPath).getPeersIndex() != -1) {
                continue;
            }
            String currentPeerDepPath = ctx.pathsByNodeId.get(peerNodeId);
            if (currentPeerDepPath != null && !currentPeerDepPath.equals(previousPeerDepPath)) {
                continue;
            }
            if (hasCurrentPeerProviderThatMustWin(peerName, parentPkgs, ctx)) {
                continue;
            }
            ParentRef lockedPeer = toPkgByName(Collections.singletonList(new ParentPkgNode(peerName,
                    ctx.dependenciesTree.get(peerNodeId), peerNodeId, parentNodeIds))).get(peerName);
            if (!satisfiesPeerRange(lockedPeer.version, peerDependency.getVersion(), ctx.release)) {
                continue;
            }
            Deferred peerPathPromise = ctx.pathsByNodeIdPromises.get(peerNodeId);
            if (peerPathPromise == null) {
                peerPathPromise = new Deferred(ctx.microtasks);
                ctx.pathsByNodeIdPromises.put(peerNodeId, peerPathPromise);
            }
            ctx.pathsByNodeId.put(peerNodeId, previousPeerDepPath);
            peerPathPromise.resolve(previousPeerDepPath);
            // a childless node shares its parent's map: from 11.7.0 the locked peer goes into a copy, out of its
            // siblings' scope (PnpmRelease#keepsALockedPeerInItsOwnScope); before, into the shared map
            if (parentPkgs == parentParentPkgs && ctx.release.keepsALockedPeerInItsOwnScope()) {
                parentPkgs = new LinkedHashMap<>(parentParentPkgs);
            }
            parentPkgs.put(peerName, lockedPeer);
        }
        return parentPkgs;
    }

    /**
     * breakDepPathAwaitCycles (11.12.0): each node whose peer path waits, through peers' paths or the peers cache it
     * took, in a cycle of nodes that wait on each other gets its {@code name@version}. Whether there is such a cycle;
     * its paths are resolved only where the release breaks them, the caller asking it once there is one.
     */
    private static boolean breakDepPathAwaitCycles(Ctx run, Map<NodeId, DependenciesTreeNode> dependenciesTree) {
        Set<NodeId> awaitingNodeIds = new LinkedHashSet<>(run.awaitedPeerNodeIdsByNodeId.keySet());
        awaitingNodeIds.addAll(run.peersCacheOwnerByNodeId.keySet());
        Map<NodeId, String> keysByNodeId = new LinkedHashMap<>();
        Map<String, NodeId> nodeIdsByKey = new LinkedHashMap<>();
        Map<String, List<String>> graph = new LinkedHashMap<>();
        for (NodeId nodeId : awaitingNodeIds) {
            if (run.pathsByNodeId.containsKey(nodeId) || run.cycleBrokenNodeIds.contains(nodeId)) {
                continue;
            }
            NodeId cacheOwnerNodeId = run.peersCacheOwnerByNodeId.get(nodeId);
            Set<NodeId> awaitedNodeIds = run.awaitedPeerNodeIdsByNodeId.get(nodeId);
            if (awaitedNodeIds == null && cacheOwnerNodeId != null) {
                awaitedNodeIds = run.awaitedPeerNodeIdsByNodeId.get(cacheOwnerNodeId);
            }
            if (awaitedNodeIds == null) {
                continue;
            }
            List<String> liveTargets = new ArrayList<>();
            for (NodeId awaitedNodeId : awaitedNodeIds) {
                if (!run.pathsByNodeId.containsKey(awaitedNodeId) && !run.cycleBrokenNodeIds.contains(awaitedNodeId)) {
                    liveTargets.add(keyOf(awaitedNodeId, keysByNodeId, nodeIdsByKey));
                }
            }
            if (!liveTargets.isEmpty()) {
                graph.put(keyOf(nodeId, keysByNodeId, nodeIdsByKey), liveTargets);
            }
        }
        if (graph.isEmpty()) {
            return false;
        }
        Set<String> inCycles = new LinkedHashSet<>();
        for (List<String> cycle : GraphCycles.cycles(graph)) {
            inCycles.addAll(cycle);
        }
        if (inCycles.isEmpty() || !run.release.breaksPeerPathAwaitCycles()) {
            return !inCycles.isEmpty();
        }
        for (String key : inCycles) {
            NodeId nodeId = nodeIdsByKey.get(key);
            ResolvedPackage resolvedPackage = dependenciesTree.get(nodeId).getResolvedPackage();
            run.cycleBrokenNodeIds.add(nodeId);
            Deferred deferred = run.pathsByNodeIdPromises.get(nodeId);
            if (deferred != null) {
                deferred.resolve(resolvedPackage.getName() + "@" + resolvedPackage.getVersion());
            }
        }
        return true;
    }

    private static String keyOf(NodeId nodeId, Map<NodeId, String> keysByNodeId, Map<String, NodeId> nodeIdsByKey) {
        String key = keysByNodeId.get(nodeId);
        if (key == null) {
            key = String.valueOf(keysByNodeId.size());
            keysByNodeId.put(nodeId, key);
            nodeIdsByKey.put(key, nodeId);
        }
        return key;
    }

    private static CurrentProviderSource currentProviderSource(ProjectToResolve project) {
        return new CurrentProviderSource(project.getDirectNodeIdsByAlias(),
                project.getDeclaredDirectDependencies() == null ? Collections.emptySet() :
                        project.getDeclaredDirectDependencies(),
                project.getExplicitlyRequestedDirectDependencies() == null ? Collections.emptySet() :
                        project.getExplicitlyRequestedDirectDependencies());
    }

    // getNodeIdsByPreviousDepPath (11.5.2): the first node of the tree each dep path the lock records names
    private static Map<String, NodeId> getNodeIdsByPreviousDepPath(Map<NodeId, DependenciesTreeNode> tree) {
        Map<String, NodeId> nodeIdsByPreviousDepPath = new LinkedHashMap<>();
        for (Map.Entry<NodeId, DependenciesTreeNode> entry : tree.entrySet()) {
            String previousDepPath = entry.getValue().getPreviousDepPath();
            if (previousDepPath != null && !nodeIdsByPreviousDepPath.containsKey(previousDepPath)) {
                nodeIdsByPreviousDepPath.put(previousDepPath, entry.getKey());
            }
        }
        return nodeIdsByPreviousDepPath;
    }

    /**
     * Whether a node's own peer provider must win over the one the lock records (11.5.2): a direct dependency of the
     * project (or of the workspace root) that is aliased, asked for anew, or declared without a lock record, or a
     * child whose provider the manifests changed under one of the node's parents.
     */
    private static boolean hasCurrentPeerProviderThatMustWin(String peerName, Map<String, ParentRef> parentPkgs,
                                                             Ctx ctx) {
        ParentRef parent = parentPkgs.get(peerName);
        NodeId peerNodeId = parent == null ? null : parent.nodeId;
        if (peerNodeId == null) {
            return false;
        }
        for (CurrentProviderSource source : ctx.currentProviderSources) {
            for (Map.Entry<String, NodeId> direct : source.getDirectNodeIdsByAlias().entrySet()) {
                if (!peerNodeId.equals(direct.getValue())) {
                    continue;
                }
                String alias = direct.getKey();
                DependenciesTreeNode peerNode = ctx.dependenciesTree.get(peerNodeId);
                if (!alias.equals(peerName) || source.getExplicitlyRequestedDirectDependencies().contains(alias) ||
                    source.getDeclaredDirectDependencies().contains(alias) &&
                    (peerNode == null || peerNode.getPreviousDepPath() == null)) {
                    return true;
                }
            }
        }
        for (NodeId parentNodeId : ctx.parentNodeIds) {
            DependenciesTreeNode parentNode = ctx.dependenciesTree.get(parentNodeId);
            if (parentNode == null || parentNode.getDependencyNamesWhoseCurrentProviderMustWin() == null) {
                continue;
            }
            Map<String, NodeId> children = parentNode.children();
            for (String alias : parentNode.getDependencyNamesWhoseCurrentProviderMustWin()) {
                if (peerNodeId.equals(children.get(alias))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Map<String, ParentRef> getRootPkgsByName(Map<NodeId, DependenciesTreeNode> dependenciesTree,
                                                            List<ProjectToResolve> projects) {
        if (projects.size() <= 1) {
            return new LinkedHashMap<>();
        }
        for (ProjectToResolve project : projects) {
            if (".".equals(project.getId())) {
                return createPkgsByName(dependenciesTree, project.getDirectNodeIdsByAlias(),
                        project.getTopParents());
            }
        }
        return new LinkedHashMap<>();
    }

    private static Map<String, ParentRef> createPkgsByName(Map<NodeId, DependenciesTreeNode> dependenciesTree,
                                                           Map<String, NodeId> directNodeIdsByAlias,
                                                           List<TopParent> topParents) {
        List<ParentPkgNode> nodes = new ArrayList<>();
        for (Map.Entry<String, NodeId> direct : directNodeIdsByAlias.entrySet()) {
            nodes.add(new ParentPkgNode(direct.getKey(), dependenciesTree.get(direct.getValue()), direct.getValue(),
                    Collections.emptyList()));
        }
        Map<String, ParentRef> parentRefs = toPkgByName(nodes);
        for (TopParent topParent : topParents) {
            ParentRef pkg = new ParentRef(topParent.getVersion(), 0, topParent.getLinkedDir(), topParent.getAlias(),
                    0, Collections.emptyList());
            updateParentRefs(parentRefs, topParent.getName(), pkg);
            if (topParent.getAlias() != null && !topParent.getAlias().equals(topParent.getName())) {
                updateParentRefs(parentRefs, topParent.getAlias(), pkg);
            }
        }
        return parentRefs;
    }

    private static Map<String, ParentRef> toPkgByName(List<ParentPkgNode> nodes) {
        Map<String, ParentRef> pkgsByName = new LinkedHashMap<>();
        for (ParentPkgNode entry : nodes) {
            String alias = entry.getAlias();
            DependenciesTreeNode node = entry.getNode();
            ParentRef pkg = new ParentRef(node.getResolvedPackage().getVersion(), node.getDepth(), entry.getNodeId(),
                    alias, 0, entry.getParentNodeIds());
            updateParentRefs(pkgsByName, alias, pkg);
            if (!alias.equals(node.getResolvedPackage().getName())) {
                updateParentRefs(pkgsByName, node.getResolvedPackage().getName(), pkg);
            }
        }
        return pkgsByName;
    }

    private static void updateParentRefs(Map<String, ParentRef> parentRefs, String newAlias, ParentRef pkg) {
        ParentRef existing = parentRefs.get(newAlias);
        if (existing != null) {
            boolean existingHasAlias = existing.alias != null && !existing.alias.equals(newAlias);
            if (!existingHasAlias) {
                return;
            }
            boolean newHasAlias = pkg.alias != null && !pkg.alias.equals(newAlias);
            if (newHasAlias && NodeSemver.compare(existing.version, pkg.version, NodeSemver.Options.DEFAULT) >= 0) {
                return;
            }
        }
        parentRefs.put(newAlias, pkg);
    }

    private static NodeResolution resolvePeersOfNode(String currentAlias, NodeId nodeId,
                                                     Map<String, ParentRef> parentParentPkgs, Ctx ctx) {
        DependenciesTreeNode node = ctx.dependenciesTree.get(nodeId);
        if (node.getDepth() == -1) {
            return new NodeResolution(new LinkedHashMap<>(), new LinkedHashMap<>(), null);
        }
        ResolvedPackage resolvedPackage = node.getResolvedPackage();
        String pkgIdWithPatchHash = resolvedPackage.getPkgIdWithPatchHash();
        if (ctx.purePkgs.contains(pkgIdWithPatchHash) &&
            ctx.depGraph.get(pkgIdWithPatchHash).getDepth() <= node.getDepth() &&
            resolvedPackage.getPeerDependencies().isEmpty()) {
            ctx.pathsByNodeId.put(nodeId, pkgIdWithPatchHash);
            ctx.pathsByNodeIdPromises.get(nodeId).resolve(pkgIdWithPatchHash);
            return new NodeResolution(new LinkedHashMap<>(), new LinkedHashMap<>(), null);
        }
        Map<String, NodeId> children = node.children();
        List<NodeId> parentNodeIds = new ArrayList<>(ctx.parentNodeIds);
        parentNodeIds.add(nodeId);
        Map<String, ParentRef> parentPkgs;
        if (children.isEmpty()) {
            parentPkgs = parentParentPkgs;
        } else {
            parentPkgs = new LinkedHashMap<>(parentParentPkgs);
            List<ParentPkgNode> parentPkgNodes = new ArrayList<>();
            for (Map.Entry<String, NodeId> child : children.entrySet()) {
                DependenciesTreeNode childNode = ctx.dependenciesTree.get(child.getValue());
                // from 11.8.0 an npm-aliased child provides a peer of its real name too
                // (PnpmRelease#providesPeersUnderAnAliasedChildsName)
                String realName = childNode.getResolvedPackage().getName();
                if (ctx.allPeerDepNames.contains(child.getKey()) ||
                    !child.getKey().equals(realName) && ctx.allPeerDepNames.contains(realName) &&
                    ctx.release.providesPeersUnderAnAliasedChildsName()) {
                    parentPkgNodes.add(new ParentPkgNode(child.getKey(), childNode, child.getValue(), parentNodeIds));
                }
            }
            Map<String, ParentRef> newParentPkgs = toPkgByName(parentPkgNodes);
            for (Map.Entry<String, ParentRef> entry : newParentPkgs.entrySet()) {
                ParentRef current = parentPkgs.get(entry.getKey());
                if (current != null) {
                    if (!parentPkgsMatch(ctx.dependenciesTree, current, entry.getValue()) ||
                        inheritedParentPkgBreaksPeerDiamond(ctx, parentPkgs, current, entry.getValue(), children) &&
                        ctx.release.replacesAnInheritedParentThatBreaksAPeerDiamond()) {
                        entry.getValue().occurrence = current.occurrence + 1;
                        parentPkgs.put(entry.getKey(), entry.getValue());
                    }
                } else {
                    parentPkgs.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (node.getLockedPeerContext() != null && ctx.resolvedPeerProviderPaths != null) {
            parentPkgs = keepLockedPeerContext(node, resolvedPackage, parentPkgs, parentParentPkgs, parentNodeIds,
                    ctx);
        }
        PeersCacheItem hit = findHit(ctx, parentPkgs, pkgIdWithPatchHash);
        if (hit != null) {
            for (Map.Entry<String, MissingPeerInfo> missing : hit.missingPeers.entrySet()) {
                ctx.peerDependencyIssues.getMissing().computeIfAbsent(missing.getKey(), k -> new ArrayList<>())
                        .add(new MissingPeerIssue(getLocationFromParentNodeIds(ctx.dependenciesTree, parentNodeIds),
                                missing.getValue().isOptional(), missing.getValue().getRange()));
            }
            ctx.shared().peersCacheOwnerByNodeId.put(nodeId, hit.ownerNodeId);
            ctx.pending[0]++;
            hit.depPath.then(depPath -> {
                ctx.pathsByNodeId.put(nodeId, depPath);
                DependenciesGraphNode graphNode = ctx.depGraph.get(depPath);
                graphNode.setDepth(Math.min(graphNode.getDepth(), node.getDepth()));
                ctx.pathsByNodeIdPromises.get(nodeId).resolve(depPath);
                ctx.pending[0]--;
            });
            return new NodeResolution(hit.resolvedPeers, hit.missingPeers, null);
        }

        List<String> chain = ctx.parentDepPathsChain;
        if (!chain.contains(pkgIdWithPatchHash)) {
            chain = new ArrayList<>(chain);
            chain.add(pkgIdWithPatchHash);
        }
        PeersResolution childrenResult = resolvePeersOfChildren(children, parentPkgs, ctx.with(parentNodeIds, chain));
        ctx.drain();
        Map<String, NodeId> allResolvedPeers = childrenResult.getResolvedPeers();
        Map<String, MissingPeerInfo> missingPeersOfChildren = childrenResult.getMissingPeers();

        Map<String, NodeId> resolvedPeers = new LinkedHashMap<>();
        Map<String, MissingPeerInfo> missingPeers = new LinkedHashMap<>();
        if (!resolvedPackage.getPeerDependencies().isEmpty()) {
            resolvePeers(ctx, resolvedPackage, parentPkgs, parentNodeIds, resolvedPeers, missingPeers);
        }
        allResolvedPeers.putAll(resolvedPeers);
        allResolvedPeers.remove(resolvedPackage.getName());

        Map<String, MissingPeerInfo> allMissingPeers = new LinkedHashMap<>(missingPeersOfChildren);
        allMissingPeers.putAll(missingPeers);

        PeersCacheItem cache = null;
        boolean isPure = allResolvedPeers.isEmpty() && allMissingPeers.isEmpty();
        // from 11.8.0 a package reached again through a cycle, its children cut, is neither pure nor cached
        // (PnpmRelease#leavesACycleReentryUncached)
        boolean resolvedThroughCycle = ctx.parentDepPathsChain.contains(pkgIdWithPatchHash) &&
                                       ctx.release.leavesACycleReentryUncached();
        if (!resolvedThroughCycle && isPure) {
            ctx.purePkgs.add(pkgIdWithPatchHash);
        } else if (!resolvedThroughCycle) {
            cache = new PeersCacheItem(new Deferred(ctx.microtasks), allResolvedPeers, allMissingPeers, nodeId);
            ctx.peersCache.computeIfAbsent(pkgIdWithPatchHash, k -> new ArrayList<>()).add(cache);
        }
        PeersCacheItem cacheItem = cache;
        Consumer<String> addDepPathToGraph = depPath -> addDepPathToGraph(ctx, cacheItem, nodeId, node, depPath,
                allResolvedPeers, missingPeersOfChildren, resolvedPeers, children, isPure);

        Consumer<List<List<String>>> calculateDepPathIfNeeded = null;
        if (allResolvedPeers.isEmpty()) {
            addDepPathToGraph.accept(pkgIdWithPatchHash);
        } else {
            List<DepPath.PeerId> peerIds = new ArrayList<>();
            List<PendingPeer> pendingPeers = new ArrayList<>();
            for (Map.Entry<String, NodeId> peer : allResolvedPeers.entrySet()) {
                DepPath.PeerId peerId = peerNodeIdToPeerId(peer.getKey(), peer.getValue(), ctx);
                if (peerId != null) {
                    peerIds.add(peerId);
                } else {
                    pendingPeers.add(new PendingPeer(peer.getKey(), peer.getValue()));
                }
            }
            if (pendingPeers.isEmpty()) {
                addDepPathToGraph.accept(pkgIdWithPatchHash +
                                         DepPath.createPeerDepGraphHash(peerIds, ctx.peersSuffixMaxLength));
            } else {
                calculateDepPathIfNeeded = cycles -> calculateDepPath(ctx, nodeId, currentAlias, pkgIdWithPatchHash,
                        peerIds, pendingPeers, cycles, addDepPathToGraph);
            }
        }
        return new NodeResolution(allResolvedPeers, allMissingPeers, calculateDepPathIfNeeded);
    }

    private static void calculateDepPath(Ctx ctx, NodeId nodeId, String currentAlias, String pkgIdWithPatchHash,
                                         List<DepPath.PeerId> peerIds, List<PendingPeer> pendingPeerNodes,
                                         List<List<String>> cycles, Consumer<String> addDepPathToGraph) {
        Set<String> cyclicPeerAliases = new LinkedHashSet<>();
        Set<String> pendingPeerAliases = new HashSet<>();
        for (PendingPeer pendingPeer : pendingPeerNodes) {
            pendingPeerAliases.add(pendingPeer.getAlias());
        }
        for (List<String> cycle : cycles) {
            if (cycle.contains(currentAlias) ||
                !Collections.disjoint(cycle, pendingPeerAliases) && ctx.release.cutsCyclesOfPendingPeers()) {
                cyclicPeerAliases.addAll(cycle);
            }
        }
        DepPath.PeerId[] ids = new DepPath.PeerId[pendingPeerNodes.size()];
        List<Deferred> awaited = new ArrayList<>();
        List<Integer> awaitedAt = new ArrayList<>();
        for (int i = 0; i < pendingPeerNodes.size(); i++) {
            String alias = pendingPeerNodes.get(i).getAlias();
            NodeId peerNodeId = pendingPeerNodes.get(i).getNodeId();
            if (cyclicPeerAliases.contains(alias)) {
                ResolvedPackage peer = ctx.dependenciesTree.get(peerNodeId).getResolvedPackage();
                String id = peer.getName() + "@" + peer.getVersion();
                ctx.shared().cycleBrokenNodeIds.add(peerNodeId);
                Deferred deferred = ctx.pathsByNodeIdPromises.get(peerNodeId);
                if (deferred != null) {
                    deferred.resolve(id);
                }
                ids[i] = DepPath.PeerId.of(id);
                continue;
            }
            if (ctx.dedupePeers) {
                DependenciesTreeNode peerNode = ctx.dependenciesTree.get(peerNodeId);
                if (peerNode != null) {
                    ids[i] = DepPath.PeerId.of(peerNode.getResolvedPackage().getName(),
                            peerNode.getResolvedPackage().getVersion());
                    continue;
                }
            }
            ctx.shared().awaitedPeerNodeIdsByNodeId.computeIfAbsent(nodeId, k -> new LinkedHashSet<>())
                    .add(peerNodeId);
            awaited.add(ctx.pathsByNodeIdPromises.get(peerNodeId));
            awaitedAt.add(i);
        }
        ctx.pending[0]++;
        Runnable finish = () -> {
            List<DepPath.PeerId> all = new ArrayList<>(peerIds);
            Collections.addAll(all, ids);
            addDepPathToGraph.accept(pkgIdWithPatchHash + DepPath.createPeerDepGraphHash(all,
                    ctx.peersSuffixMaxLength));
            ctx.pending[0]--;
        };
        if (awaited.isEmpty()) {
            ctx.microtasks.add(finish);
            return;
        }
        int[] remaining = {awaited.size()};
        for (int k = 0; k < awaited.size(); k++) {
            int at = awaitedAt.get(k);
            awaited.get(k).then(depPath -> {
                ids[at] = DepPath.PeerId.of(depPath);
                if (--remaining[0] == 0) {
                    ctx.microtasks.add(finish);
                }
            });
        }
    }

    private static void addDepPathToGraph(Ctx ctx, @Nullable PeersCacheItem cache, NodeId nodeId,
                                          DependenciesTreeNode node, String depPath,
                                          Map<String, NodeId> allResolvedPeers,
                                          Map<String, MissingPeerInfo> missingPeersOfChildren,
                                          Map<String, NodeId> resolvedPeers, Map<String, NodeId> children,
                                          boolean isPure) {
        ResolvedPackage resolvedPackage = node.getResolvedPackage();
        if (cache != null) {
            cache.depPath.resolve(depPath);
        }
        ctx.pathsByNodeId.put(nodeId, depPath);
        ctx.pathsByNodeIdPromises.get(nodeId).resolve(depPath);
        ctx.depPathsByPkgId.computeIfAbsent(resolvedPackage.getPkgIdWithPatchHash(), k -> new LinkedHashSet<>())
                .add(depPath);
        Map<String, PeerDependency> peerDependencies = new LinkedHashMap<>(resolvedPackage.getPeerDependencies());
        DependenciesGraphNode existing = ctx.depGraph.get(depPath);
        if (existing == null || existing.getDepth() > node.getDepth()) {
            // from 11.11.0 the package's directory is safeJoinModulesDir's, which refuses a name
            // validate-npm-package-name 7.0.2 rejects (ERR_PNPM_INVALID_DEPENDENCY_NAME), one it accepts staying under
            // the virtual store; the error names the virtual store's absolute path, which no input holds
            // (PnpmRelease#placesAPackageOnlyUnderAValidName)
            if (!ValidateNpmPackageName.validForOldPackages(resolvedPackage.getName(),
                    ValidateNpmPackageName.Release.V7_0_2) && ctx.release.placesAPackageOnlyUnderAValidName()) {
                throw new CannotKnow("the virtual store directory under which pnpm refuses to place " + depPath +
                                     " with the invalid alias " + Js.jsonStringify(resolvedPackage.getName()) +
                                     " (ERR_PNPM_INVALID_DEPENDENCY_NAME names its absolute path)");
            }
            Set<String> transitivePeerDependencies = new LinkedHashSet<>();
            for (String unknownPeer : allResolvedPeers.keySet()) {
                if (!peerDependencies.containsKey(unknownPeer)) {
                    transitivePeerDependencies.add(unknownPeer);
                }
            }
            for (String unknownPeer : missingPeersOfChildren.keySet()) {
                if (!peerDependencies.containsKey(unknownPeer)) {
                    transitivePeerDependencies.add(unknownPeer);
                }
            }
            DependenciesGraphNode graphNode = new DependenciesGraphNode(depPath, resolvedPackage);
            Map<String, NodeId> childrenNodeIds = getPreviouslyResolvedChildren(ctx,
                    resolvedPackage.getPkgIdWithPatchHash());
            childrenNodeIds.putAll(children);
            childrenNodeIds.putAll(resolvedPeers);
            graphNode.setChildrenNodeIds(childrenNodeIds);
            graphNode.setDepth(node.getDepth());
            graphNode.setInstallable(node.isInstallable());
            graphNode.setPure(isPure);
            graphNode.setPeerDependencies(peerDependencies);
            graphNode.setTransitivePeerDependencies(transitivePeerDependencies);
            graphNode.setResolvedPeerNames(new LinkedHashSet<>(allResolvedPeers.keySet()));
            ctx.depGraph.put(depPath, graphNode);
        }
    }

    private static boolean parentPkgsMatch(Map<NodeId, DependenciesTreeNode> dependenciesTree, ParentRef current,
                                           ParentRef newParent) {
        if (!current.version.equals(newParent.version) || !Objects.equals(current.alias, newParent.alias)) {
            return false;
        }
        DependenciesTreeNode currentNode = current.nodeId == null ? null : dependenciesTree.get(current.nodeId);
        if (currentNode == null) {
            return true;
        }
        DependenciesTreeNode newNode = newParent.nodeId == null ? null : dependenciesTree.get(newParent.nodeId);
        if (newNode == null) {
            return true;
        }
        return currentNode.getResolvedPackage().getName().equals(newNode.getResolvedPackage().getName());
    }

    /**
     * Whether the parent package a node inherits must give way to its own child of that name, because the
     * inherited one was resolved against other versions of the peers that the own child and one of the node's
     * children share, as resolvePeers.ts's {@code inheritedParentPkgBreaksPeerDiamond} decides from pnpm
     * 11.5.1 on.
     */
    private static boolean inheritedParentPkgBreaksPeerDiamond(Ctx ctx, Map<String, ParentRef> parentPkgs,
                                                               ParentRef inheritedParentPkg,
                                                               ParentRef ownChildParentPkg,
                                                               Map<String, NodeId> children) {
        if (inheritedParentPkg.nodeId == null || ownChildParentPkg.nodeId == null) {
            return false;
        }
        if (inheritedParentPkg.nodeId.equals(ownChildParentPkg.nodeId)) {
            return false;
        }
        Map<String, ParentPkgInfo> inheritedContext = ctx.parentPkgsOfNode.get(inheritedParentPkg.nodeId);
        if (inheritedContext == null) {
            return false;
        }
        DependenciesTreeNode parentNode = ctx.dependenciesTree.get(ownChildParentPkg.nodeId);
        ResolvedPackage parentPkg = parentNode == null ? null : parentNode.getResolvedPackage();
        if (parentPkg == null) {
            return false;
        }
        Set<String> conflictingPeers = new LinkedHashSet<>();
        for (String peerName : parentPkg.getPeerDependencies().keySet()) {
            if (!ctx.allPeerDepNames.contains(peerName)) {
                continue;
            }
            ParentPkgInfo inheritedPeer = inheritedContext.get(peerName);
            ParentRef currentPeer = parentPkgs.get(peerName);
            if (inheritedPeer == null || currentPeer == null) {
                continue;
            }
            if (parentPeerDiffers(ctx.dependenciesTree, currentPeer, inheritedPeer)) {
                conflictingPeers.add(peerName);
            }
        }
        if (conflictingPeers.isEmpty()) {
            return false;
        }
        for (NodeId childNodeId : children.values()) {
            DependenciesTreeNode child = ctx.dependenciesTree.get(childNodeId);
            Map<String, PeerDependency> childPeerDependencies = child == null || child.getResolvedPackage() == null ?
                    null : child.getResolvedPackage().getPeerDependencies();
            if (childPeerDependencies == null || childPeerDependencies.get(parentPkg.getName()) == null) {
                continue;
            }
            for (String peerName : conflictingPeers) {
                if (childPeerDependencies.get(peerName) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean parentPeerDiffers(Map<NodeId, DependenciesTreeNode> dependenciesTree,
                                             ParentRef currentPeer, ParentPkgInfo inheritedPeer) {
        if (inheritedPeer.pkgIdWithPatchHash != null) {
            if (currentPeer.nodeId == null || currentPeer.nodeId.isLink()) {
                return true;
            }
            DependenciesTreeNode current = dependenciesTree.get(currentPeer.nodeId);
            String currentPkgId = current == null || current.getResolvedPackage() == null ? null :
                    current.getResolvedPackage().getPkgIdWithPatchHash();
            return !inheritedPeer.pkgIdWithPatchHash.equals(currentPkgId);
        }
        return !currentPeer.version.equals(inheritedPeer.version);
    }

    private static @Nullable PeersCacheItem findHit(Ctx ctx, Map<String, ParentRef> parentPkgs,
                                                    String pkgIdWithPatchHash) {
        List<PeersCacheItem> cacheItems = ctx.peersCache.get(pkgIdWithPatchHash);
        if (cacheItems == null) {
            return null;
        }
        for (PeersCacheItem cache : cacheItems) {
            if (matches(ctx, parentPkgs, cache)) {
                return cache;
            }
        }
        return null;
    }

    private static boolean matches(Ctx ctx, Map<String, ParentRef> parentPkgs, PeersCacheItem cache) {
        for (Map.Entry<String, NodeId> peer : cache.resolvedPeers.entrySet()) {
            NodeId cachedNodeId = peer.getValue();
            ParentRef parent = parentPkgs.get(peer.getKey());
            NodeId parentPkgNodeId = parent == null ? null : parent.nodeId;
            if ((parentPkgNodeId != null) != (cachedNodeId != null)) {
                return false;
            }
            if (Objects.equals(parentPkgNodeId, cachedNodeId)) {
                continue;
            }
            if (parentPkgNodeId == null) {
                return false;
            }
            if (ctx.pathsByNodeId.containsKey(cachedNodeId) &&
                ctx.pathsByNodeId.get(cachedNodeId).equals(ctx.pathsByNodeId.get(parentPkgNodeId))) {
                continue;
            }
            if (!ctx.dependenciesTree.containsKey(parentPkgNodeId) && parentPkgNodeId.isLink()) {
                return false;
            }
            String parentPkgId = ctx.dependenciesTree.get(parentPkgNodeId).getResolvedPackage().getPkgIdWithPatchHash();
            String cachedPkgId = ctx.dependenciesTree.get(cachedNodeId).getResolvedPackage().getPkgIdWithPatchHash();
            if (!parentPkgId.equals(cachedPkgId)) {
                return false;
            }
            if (!ctx.purePkgs.contains(parentPkgId) && !parentPackagesMatch(ctx, cachedNodeId, parentPkgNodeId)) {
                return false;
            }
        }
        for (String missingPeer : cache.missingPeers.keySet()) {
            if (parentPkgs.get(missingPeer) != null) {
                return false;
            }
        }
        return true;
    }

    private static boolean parentPackagesMatch(Ctx ctx, NodeId cachedNodeId, NodeId checkedNodeId) {
        Map<String, ParentPkgInfo> cachedParentPkgs = ctx.parentPkgsOfNode.get(cachedNodeId);
        if (cachedParentPkgs == null) {
            return false;
        }
        Map<String, ParentPkgInfo> checkedParentPkgs = ctx.parentPkgsOfNode.get(checkedNodeId);
        if (checkedParentPkgs == null) {
            return false;
        }
        if (cachedParentPkgs.size() != checkedParentPkgs.size()) {
            return false;
        }
        int maxDepth = 0;
        for (ParentPkgInfo info : checkedParentPkgs.values()) {
            maxDepth = Math.max(info.depth == null ? 0 : info.depth, maxDepth);
        }
        boolean peerDepsAreNotShadowed = parentPkgsHaveSingleOccurrence(cachedParentPkgs) &&
                                         parentPkgsHaveSingleOccurrence(checkedParentPkgs);
        for (Map.Entry<String, ParentPkgInfo> entry : cachedParentPkgs.entrySet()) {
            ParentPkgInfo checked = checkedParentPkgs.get(entry.getKey());
            if (checked == null) {
                return false;
            }
            String version = entry.getValue().version;
            String pkgId = entry.getValue().pkgIdWithPatchHash;
            if (Js.truthy(version) && Js.truthy(checked.version)) {
                if (!version.equals(checked.version)) {
                    return false;
                }
                continue;
            }
            boolean ok = pkgId != null && pkgId.equals(checked.pkgIdWithPatchHash) &&
                         (peerDepsAreNotShadowed || checked.depth != null && checked.depth == maxDepth ||
                          ctx.purePkgs.contains(pkgId));
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static boolean parentPkgsHaveSingleOccurrence(Map<String, ParentPkgInfo> parentPkgs) {
        for (ParentPkgInfo info : parentPkgs.values()) {
            if (info.occurrence != null && info.occurrence != 0) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, NodeId> getPreviouslyResolvedChildren(Ctx ctx, String currentDepPath) {
        Map<String, NodeId> allChildren = new LinkedHashMap<>();
        if (currentDepPath.isEmpty() || !ctx.parentDepPathsChain.contains(currentDepPath)) {
            return allChildren;
        }
        for (int i = ctx.parentNodeIds.size() - 1; i >= 0; i--) {
            DependenciesTreeNode parentNode = ctx.dependenciesTree.get(ctx.parentNodeIds.get(i));
            if (parentNode.getResolvedPackage().getPkgIdWithPatchHash().equals(currentDepPath)) {
                allChildren.putAll(parentNode.children());
            }
        }
        return allChildren;
    }

    private static PeersResolution resolvePeersOfChildren(Map<String, NodeId> children,
                                                          Map<String, ParentRef> parentPkgs, Ctx ctx) {
        Map<String, NodeId> allResolvedPeers = new LinkedHashMap<>();
        Map<String, MissingPeerInfo> allMissingPeers = new LinkedHashMap<>();
        List<Map.Entry<String, NodeId>> repeated = new ArrayList<>();
        List<Map.Entry<String, NodeId>> notRepeated = new ArrayList<>();
        for (Map.Entry<String, NodeId> child : children.entrySet()) {
            (parentPkgs.get(child.getKey()) != null ? repeated : notRepeated).add(child);
        }
        Set<NodeId> nodeIds = new LinkedHashSet<>();
        for (Map.Entry<String, NodeId> child : repeated) {
            nodeIds.add(child.getValue());
        }
        for (Map.Entry<String, NodeId> child : notRepeated) {
            nodeIds.add(child.getValue());
        }
        Map<NodeId, String> aliasByNodeId = new LinkedHashMap<>();
        for (Map.Entry<String, NodeId> child : children.entrySet()) {
            aliasByNodeId.put(child.getValue(), child.getKey());
        }
        for (NodeId nodeId : nodeIds) {
            ctx.pathsByNodeIdPromises.computeIfAbsent(nodeId, k -> new Deferred(ctx.microtasks));
        }
        List<Consumer<List<List<String>>>> calculateDepPaths = new ArrayList<>();
        Map<String, List<String>> graph = new LinkedHashMap<>();
        Map<String, ParentPkgInfo> parentDepPaths = new LinkedHashMap<>();
        for (Map.Entry<String, ParentRef> entry : parentPkgs.entrySet()) {
            if (!ctx.allPeerDepNames.contains(entry.getKey())) {
                continue;
            }
            ParentRef parentPkg = entry.getValue();
            ParentPkgInfo info = new ParentPkgInfo();
            if (parentPkg.nodeId != null && !parentPkg.nodeId.isLink()) {
                info.pkgIdWithPatchHash = ctx.dependenciesTree.get(parentPkg.nodeId).getResolvedPackage()
                        .getPkgIdWithPatchHash();
                info.depth = parentPkg.depth;
                info.occurrence = parentPkg.occurrence;
            } else {
                info.version = parentPkg.version;
            }
            parentDepPaths.put(entry.getKey(), info);
        }
        for (NodeId childNodeId : nodeIds) {
            ctx.parentPkgsOfNode.put(childNodeId, parentDepPaths);
            ctx.projectNodeIds.add(childNodeId);
        }
        for (NodeId childNodeId : nodeIds) {
            String currentAlias = aliasByNodeId.get(childNodeId);
            NodeResolution resolution = resolvePeersOfNode(currentAlias, childNodeId, parentPkgs, ctx);
            ctx.drain();
            if (resolution.calculateDepPath != null) {
                calculateDepPaths.add(resolution.calculateDepPath);
            }
            List<String> edges = new ArrayList<>();
            for (Map.Entry<String, NodeId> peer : resolution.resolvedPeers.entrySet()) {
                allResolvedPeers.put(peer.getKey(), peer.getValue());
                edges.add(peer.getKey());
            }
            addEdgesToGraph(graph, currentAlias, edges);
            DependenciesTreeNode node = ctx.dependenciesTree.get(childNodeId);
            if (!currentAlias.equals(node.getResolvedPackage().getName())) {
                addEdgesToGraph(graph, node.getResolvedPackage().getName(), edges);
            }
            allMissingPeers.putAll(resolution.missingPeers);
        }
        if (!calculateDepPaths.isEmpty()) {
            List<List<String>> cycles = GraphCycles.cycles(graph);
            for (Consumer<List<List<String>>> calculateDepPath : calculateDepPaths) {
                calculateDepPath.accept(cycles);
            }
        }
        Map<String, NodeId> unknownResolvedPeersOfChildren = new LinkedHashMap<>();
        for (Map.Entry<String, NodeId> peer : allResolvedPeers.entrySet()) {
            if (children.get(peer.getKey()) == null) {
                unknownResolvedPeersOfChildren.put(peer.getKey(), peer.getValue());
            }
        }
        return new PeersResolution(unknownResolvedPeersOfChildren, allMissingPeers);
    }

    private static void addEdgesToGraph(Map<String, List<String>> graph, String pkgName, List<String> edges) {
        List<String> existing = graph.get(pkgName);
        if (existing == null) {
            graph.put(pkgName, edges);
        } else {
            existing.addAll(edges);
        }
    }

    private static void resolvePeers(Ctx ctx, ResolvedPackage resolvedPackage, Map<String, ParentRef> parentPkgs,
                                     List<NodeId> parentNodeIds, Map<String, NodeId> resolvedPeers,
                                     Map<String, MissingPeerInfo> missingPeers) {
        for (Map.Entry<String, PeerDependency> peer : resolvedPackage.getPeerDependencies().entrySet()) {
            String peerName = peer.getKey();
            String version = peer.getValue().getVersion();
            // the wanted range an issue records, 11.2.2's; 11.14.0's getPeerVersionRange gives the one it reads
            // (satisfiesPeerRange), but mergePeers alone reads this one, of whose conflicts and intersections the
            // report checks only that they are empty
            String peerVersionRange = version.startsWith("workspace:") ? version.substring("workspace:".length()) :
                    version;
            ParentRef resolved = parentPkgs.get(peerName);
            boolean optionalPeer = Boolean.TRUE.equals(peer.getValue().getOptional());
            if (resolved == null) {
                missingPeers.put(peerName, new MissingPeerInfo(version, optionalPeer));
                ctx.peerDependencyIssues.getMissing().computeIfAbsent(peerName, k -> new ArrayList<>())
                        .add(new MissingPeerIssue(getLocationFromParentNodeIds(ctx.dependenciesTree, parentNodeIds),
                                optionalPeer, peerVersionRange));
                continue;
            }
            if (!satisfiesPeerRange(resolved.version, version, ctx.release)) {
                List<ParentPackage> peerLocation = resolved.nodeId == null ? new ArrayList<>() :
                        getLocationFromParentNodeIds(ctx.dependenciesTree, resolved.parentNodeIds);
                ctx.peerDependencyIssues.getBad().computeIfAbsent(peerName, k -> new ArrayList<>())
                        .add(new BadPeerIssue(resolved.version, peerLocation,
                                getLocationFromParentNodeIds(ctx.dependenciesTree, parentNodeIds), optionalPeer,
                                peerVersionRange));
            }
            if (resolved.nodeId != null) {
                resolvedPeers.put(peerName, resolved.nodeId);
            }
        }
    }

    // whether version satisfies the range of the peer spec, prereleases included: 11.2.2's range is the spec less a
    // leading workspace:, 11.14.0's getPeerVersionRange's (PnpmRelease#readsAPeerRangeOutOfItsSpecifier), asked where
    // the two answer otherwise
    private static boolean satisfiesPeerRange(String version, String peerSpec, PnpmRelease release) {
        String before = peerSpec.startsWith("workspace:") ? peerSpec.substring("workspace:".length()) : peerSpec;
        String after = ReadPackageHook.getPeerVersionRange(peerSpec, release);
        boolean satisfiesBefore = release.underSemver(semver -> YarnpkgSemverUtils.satisfiesWithPrereleases(version,
                before, true, semver));
        if (before.equals(after)) {
            return satisfiesBefore;
        }
        boolean satisfiesAfter = release.underSemver(semver -> YarnpkgSemverUtils.satisfiesWithPrereleases(version,
                after, true, semver));
        return satisfiesBefore == satisfiesAfter || !release.readsAPeerRangeOutOfItsSpecifier() ? satisfiesBefore :
                satisfiesAfter;
    }

    private static List<ParentPackage> getLocationFromParentNodeIds(Map<NodeId, DependenciesTreeNode> dependenciesTree,
                                                                    List<NodeId> parentNodeIds) {
        List<ParentPackage> parents = new ArrayList<>();
        for (NodeId nodeId : parentNodeIds) {
            ResolvedPackage pkg = dependenciesTree.get(nodeId).getResolvedPackage();
            parents.add(new ParentPackage(pkg.getName(), pkg.getVersion()));
        }
        return parents;
    }

    // mergePeers.ts over a project's missing peers
    private static PeerDependencyIssues mergePeers(PeerDependencyIssues issues) {
        List<String> conflicts = new ArrayList<>();
        Map<String, String> intersections = new LinkedHashMap<>();
        for (Map.Entry<String, List<MissingPeerIssue>> peer : issues.getMissing().entrySet()) {
            List<MissingPeerIssue> ranges = peer.getValue();
            boolean allOptional = true;
            for (MissingPeerIssue range : ranges) {
                allOptional &= range.isOptional();
            }
            if (allOptional) {
                continue;
            }
            if (ranges.size() == 1) {
                intersections.put(peer.getKey(), ranges.get(0).getWantedRange());
                continue;
            }
            List<String> wantedRanges = new ArrayList<>();
            for (MissingPeerIssue range : ranges) {
                wantedRanges.add(range.getWantedRange());
            }
            String intersection = ResolveDependencies.safeIntersect(wantedRanges);
            if (intersection == null) {
                conflicts.add(peer.getKey());
            } else {
                intersections.put(peer.getKey(), intersection);
            }
        }
        return new PeerDependencyIssues(issues.getBad(), issues.getMissing(), conflicts, intersections);
    }

    private static DepPath.@Nullable PeerId peerNodeIdToPeerId(String alias, NodeId peerNodeId, Ctx ctx) {
        if (peerNodeId.isLink()) {
            return DepPath.PeerId.of(alias, linkPathToPeerVersion(((String) peerNodeId.getId()).substring(5)));
        }
        if (ctx.dedupePeers) {
            DependenciesTreeNode peerNode = ctx.dependenciesTree.get(peerNodeId);
            if (peerNode != null) {
                return DepPath.PeerId.of(peerNode.getResolvedPackage().getName(),
                        peerNode.getResolvedPackage().getVersion());
            }
        }
        String depPath = ctx.pathsByNodeId.get(peerNodeId);
        return depPath == null ? null : DepPath.PeerId.of(depPath);
    }

    /**
     * linkPathToPeerVersion.ts: filenamify v4's rendering of a {@code link:} path.
     */
    static String linkPathToPeerVersion(String relPath) {
        int i = 0;
        while (i < relPath.length() && relPath.charAt(i) == '.') {
            i++;
        }
        StringBuilder out = new StringBuilder();
        boolean lastWasPlus = true;
        for (; i < relPath.length(); i++) {
            char c = relPath.charAt(i);
            boolean replace = c < 32 || c == '"' || c == '*' || c == '+' || c == '/' || c == ':' || c == '<' ||
                              c == '>' || c == '?' || c == '\\' || c == '|';
            if (replace) {
                if (!lastWasPlus) {
                    out.append('+');
                    lastWasPlus = true;
                }
            } else {
                out.append(c);
                lastWasPlus = false;
            }
        }
        int end = out.length();
        while (end > 0 && (out.charAt(end - 1) == '+' || out.charAt(end - 1) == '.')) {
            end--;
        }
        if (end > 0) {
            return out.substring(0, end);
        }
        return relPath.isEmpty() ? "" : "+";
    }
}
