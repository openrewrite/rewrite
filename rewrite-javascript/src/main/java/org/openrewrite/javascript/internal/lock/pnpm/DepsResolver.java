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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencyTree.ResolvedDirectDependency;
import org.openrewrite.javascript.internal.lock.pnpm.ResolvePeers.DependenciesGraphNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Port of @pnpm/installing.deps-resolver src/index.ts {@code resolveDependencies} (pnpm 11.2.2): the tree, its peers,
 * the importers rebuilt from the manifests, and the lock updated from the graph.
 */
final class DepsResolver {

    private DepsResolver() {
    }

    @Value
    static class Options {
        boolean autoInstallPeers;
        boolean dedupePeerDependents;
        boolean dedupePeers;
        boolean dedupeInjectedDeps;
        boolean excludeLinksFromLockfile;
        boolean lockfileIncludeTarballUrl;
        boolean resolvePeersFromWorkspaceRoot;
        int peersSuffixMaxLength;
        Map<String, String> registries;
        boolean allowUnusedPatches;
        Consumer<List<ResolveDependencies.PolicyViolation>> handleResolutionPolicyViolations;
    }

    @Value
    static class Result {
        LockfileObject newLockfile;
        Map<String, ResolvePeers.PeerDependencyIssues> peerDependencyIssuesByProjects;
        List<ResolveDependencies.PolicyViolation> resolutionPolicyViolations;
    }

    static Result resolveDependencies(List<ResolveDependencyTree.Importer> importers,
                                      ResolveDependencies.ResolutionContext ctx,
                                      PreferredVersions.ByName preferredVersions, Options opts) {
        ResolveDependencyTree.Result tree = ResolveDependencyTree.resolveDependencyTree(importers, ctx,
                preferredVersions);
        LockfileObject wantedLockfile = ctx.getWantedLockfile();
        if (!tree.getResolutionPolicyViolations().isEmpty()) {
            opts.getHandleResolutionPolicyViolations().accept(tree.getResolutionPolicyViolations());
        }
        if (ctx.getPatchedDependencies() != null &&
            (ctx.isForceFullResolution() || wantedLockfile.getPackages() == null ||
             wantedLockfile.getPackages().isEmpty()) &&
            wantedLockfile.getImporters().size() == importers.size()) {
            PatchingConfig.verifyPatches(ctx.getPatchedDependencies(), ctx.getAppliedPatches(),
                    opts.isAllowUnusedPatches());
        }

        List<ResolvePeers.ProjectToResolve> projectsToLink = new ArrayList<>();
        for (ResolveDependencyTree.Importer project : importers) {
            ResolveDependencyTree.ResolvedImporter resolvedImporter = tree.getResolvedImporters().get(project.getId());
            List<ResolvePeers.TopParent> topParents = new ArrayList<>();
            for (ResolvedDirectDependency linked : resolvedImporter.getLinkedDependencies()) {
                String directory = Js.string(linked.getResolution().get("directory"));
                String target = !opts.isExcludeLinksFromLockfile() ||
                                !RootRelativePath.climbsAboveRoot(RootRelativePath.normalize(directory)) ? directory :
                        RootRelativePath.join(RootRelativePath.join(project.getRootDir(), "node_modules"),
                                linked.getAlias());
                topParents.add(new ResolvePeers.TopParent(linked.getAlias(), Js.string(linked.getVersion()), null,
                        ResolveDependencies.createNodeIdForLinkedLocalPkg(ctx.getLockfileDir(), target)));
            }
            // 11.5.2: what the manifest declares or the install adds, and what the install asks for anew
            Set<String> declaredDirectDependencies = new LinkedHashSet<>(Js.keys(
                    PreferredVersions.getAllDependenciesFromManifest(project.getManifest())));
            Set<String> explicitlyRequestedDirectDependencies = new LinkedHashSet<>();
            for (ResolveDependencies.WantedDependency wanted : project.getWantedDependencies()) {
                String alias = wanted.getAlias();
                if (alias == null) {
                    continue;
                }
                if (wanted.isNew()) {
                    declaredDirectDependencies.add(alias);
                }
                if (wanted.isNew() || wanted.isUpdateSpec() || wanted.getPrevSpecifier() != null &&
                                                               !wanted.getPrevSpecifier().equals(wanted.getBareSpecifier())) {
                    explicitlyRequestedDirectDependencies.add(alias);
                }
            }
            projectsToLink.add(new ResolvePeers.ProjectToResolve(project.getId(), project.getRootDir(),
                    resolvedImporter.getDirectNodeIdsByAlias(), topParents, declaredDirectDependencies,
                    explicitlyRequestedDirectDependencies, resolvedImporter.getHoistedPeerProviderNodeIds()));
        }

        Set<String> workspaceProjectIds = new LinkedHashSet<>();
        for (ResolveDependencyTree.Importer importer : importers) {
            workspaceProjectIds.add(importer.getId());
        }
        workspaceProjectIds.addAll(wantedLockfile.getImporters().keySet());
        ResolvePeers.Result peers = ResolvePeers.resolvePeers(tree.getAllPeerDepNames(), projectsToLink,
                tree.getDependenciesTree(), opts.isResolvePeersFromWorkspaceRoot(), opts.isDedupePeerDependents(),
                opts.isDedupePeers(), opts.isDedupeInjectedDeps(), opts.getPeersSuffixMaxLength(),
                ctx.getLockfileDir(), tree.getResolvedImporters(), workspaceProjectIds, ctx.getRelease());
        if (treeHasLockedPeerContexts(tree.getDependenciesTree()) &&
            ctx.getRelease().prefersThePeerContextsTheLockRecords()) {
            // 11.5.2: again, from the paths the first pass gave the nodes
            peers = ResolvePeers.resolvePeers(tree.getAllPeerDepNames(), projectsToLink,
                    tree.getDependenciesTree(), opts.isResolvePeersFromWorkspaceRoot(),
                    opts.isDedupePeerDependents(), opts.isDedupePeers(), opts.isDedupeInjectedDeps(),
                    opts.getPeersSuffixMaxLength(), ctx.getLockfileDir(), tree.getResolvedImporters(),
                    workspaceProjectIds, ctx.getRelease(), peers.getPathsByNodeId());
        }
        Map<String, DependenciesGraphNode> dependenciesGraph = peers.getDependenciesGraph();

        for (ResolveDependencyTree.Importer project : importers) {
            ResolveDependencyTree.ResolvedImporter resolvedImporter = tree.getResolvedImporters().get(project.getId());
            Map<String, @Nullable Object> updatedManifest = project.getManifest();
            if (opts.isAutoInstallPeers()) {
                Map<String, @Nullable Object> peerDependencies =
                        ResolveDependencies.asMap(updatedManifest.get("peerDependencies"));
                if (updatedManifest.get("peerDependencies") != null) {
                    Map<String, @Nullable Object> allDeps =
                            PreferredVersions.getAllDependenciesFromManifest(updatedManifest);
                    for (String peerName : Js.keys(peerDependencies)) {
                        if (Js.truthy(allDeps.get(peerName))) {
                            continue;
                        }
                        @SuppressWarnings("unchecked")
                        Map<String, @Nullable Object> dependencies =
                                updatedManifest.get("dependencies") instanceof Map ?
                                        (Map<String, @Nullable Object>) updatedManifest.get("dependencies") : null;
                        if (dependencies == null) {
                            dependencies = new LinkedHashMap<>();
                            updatedManifest.put("dependencies", dependencies);
                        }
                        dependencies.put(peerName, peerDependencies.get(peerName));
                    }
                }
            }
            ProjectSnapshot projectSnapshot = wantedLockfile.getImporters().get(project.getId());
            ProjectSnapshot newSnapshot = addDirectDependenciesToLockfile(updatedManifest, projectSnapshot,
                    resolvedImporter.getLinkedDependencies(), resolvedImporter.getDirectDependencies(),
                    opts.isExcludeLinksFromLockfile());
            wantedLockfile.getImporters().put(project.getId(), newSnapshot);

            for (Map.Entry<String, String> dep : peers.getDependenciesByProjectId().get(project.getId()).entrySet()) {
                if (project.getManifest().get("dependenciesMeta") != null) {
                    newSnapshot.setDependenciesMeta(project.getManifest().get("dependenciesMeta"));
                }
                String alias = dep.getKey();
                String depPath = dep.getValue();
                DependenciesGraphNode depNode = dependenciesGraph.get(depPath);
                String ref = UpdateLockfile.depPathToRef(depPath, alias, depNode.getResolvedPackage().getName());
                if (truthy(newSnapshot.getDependencies(), alias)) {
                    newSnapshot.getDependencies().put(alias, ref);
                } else if (truthy(newSnapshot.getDevDependencies(), alias)) {
                    newSnapshot.getDevDependencies().put(alias, ref);
                } else if (truthy(newSnapshot.getOptionalDependencies(), alias)) {
                    newSnapshot.getOptionalDependencies().put(alias, ref);
                }
            }
        }

        LockfileObject newLockfile = UpdateLockfile.updateLockfile(dependenciesGraph, wantedLockfile,
                opts.getRegistries(), opts.isLockfileIncludeTarballUrl(), ctx.getRelease());
        if (tree.getTime() != null) {
            Map<String, @Nullable Object> time = new LinkedHashMap<>();
            Object previous = wantedLockfile.getTime();
            if (previous instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, @Nullable Object> before = (Map<String, @Nullable Object>) previous;
                time.putAll(before);
            }
            time.putAll(tree.getTime());
            newLockfile.set("time", time);
        }
        List<ResolvedDirectDependency> allDirect = new ArrayList<>();
        for (ResolveDependencyTree.ResolvedImporter resolved : tree.getResolvedImporters().values()) {
            allDirect.addAll(resolved.getDirectDependencies());
        }
        newLockfile.set("catalogs", UpdateLockfile.getCatalogSnapshots(allDirect));
        return new Result(newLockfile, peers.getPeerDependencyIssuesByProjects(),
                tree.getResolutionPolicyViolations());
    }

    // treeHasLockedPeerContexts (11.5.2)
    private static boolean treeHasLockedPeerContexts(
            Map<ResolveDependencies.NodeId, ResolveDependencies.DependenciesTreeNode> dependenciesTree) {
        for (ResolveDependencies.DependenciesTreeNode node : dependenciesTree.values()) {
            if (node.getLockedPeerContext() != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean truthy(@Nullable Map<String, @Nullable String> refs, String alias) {
        return refs != null && Js.truthy(refs.get(alias));
    }

    private static ProjectSnapshot addDirectDependenciesToLockfile(Map<String, @Nullable Object> newManifest,
                                                                   ProjectSnapshot projectSnapshot,
                                                                   List<ResolvedDirectDependency> linkedPackages,
                                                                   List<ResolvedDirectDependency> directDependencies,
                                                                   boolean excludeLinksFromLockfile) {
        ProjectSnapshot newProjectSnapshot = new ProjectSnapshot();
        newProjectSnapshot.setDependencies(new LinkedHashMap<>());
        newProjectSnapshot.setDevDependencies(new LinkedHashMap<>());
        newProjectSnapshot.setOptionalDependencies(new LinkedHashMap<>());
        Object publishConfig = newManifest.get("publishConfig");
        if (publishConfig instanceof Map && Js.truthy(((Map<?, ?>) publishConfig).get("directory"))) {
            newProjectSnapshot.setPublishDirectory(((Map<?, ?>) publishConfig).get("directory"));
        }
        for (ResolvedDirectDependency linkedPkg : linkedPackages) {
            newProjectSnapshot.getSpecifiers().put(linkedPkg.getAlias(),
                    getSpecFromPackageManifest(newManifest, linkedPkg.getAlias()));
        }
        Map<String, ResolvedDirectDependency> directDependenciesByAlias = new LinkedHashMap<>();
        for (ResolvedDirectDependency directDependency : directDependencies) {
            directDependenciesByAlias.put(directDependency.getAlias(), directDependency);
        }
        Set<String> allDeps = new LinkedHashSet<>(Js.keys(
                PreferredVersions.getAllDependenciesFromManifest(newManifest)));
        for (String alias : allDeps) {
            ResolvedDirectDependency dep = directDependenciesByAlias.get(alias);
            String spec = dep == null ? null : getSpecFromPackageManifest(newManifest, dep.getAlias());
            if (dep != null && (!excludeLinksFromLockfile || !dep.isLinkedDependency() ||
                                spec.startsWith("workspace:"))) {
                String ref = UpdateLockfile.depPathToRef(dep.getPkgId(), dep.getAlias(), Js.string(dep.getName()));
                if (dep.isDev()) {
                    newProjectSnapshot.getDevDependencies().put(dep.getAlias(), ref);
                } else if (dep.isOptional()) {
                    newProjectSnapshot.getOptionalDependencies().put(dep.getAlias(), ref);
                } else {
                    newProjectSnapshot.getDependencies().put(dep.getAlias(), ref);
                }
                newProjectSnapshot.getSpecifiers().put(dep.getAlias(), spec);
            } else if (Js.truthy(projectSnapshot.getSpecifiers().get(alias))) {
                newProjectSnapshot.getSpecifiers().put(alias, projectSnapshot.getSpecifiers().get(alias));
                if (truthy(projectSnapshot.getDependencies(), alias)) {
                    newProjectSnapshot.getDependencies().put(alias, projectSnapshot.getDependencies().get(alias));
                } else if (truthy(projectSnapshot.getOptionalDependencies(), alias)) {
                    newProjectSnapshot.getOptionalDependencies().put(alias,
                            projectSnapshot.getOptionalDependencies().get(alias));
                } else if (truthy(projectSnapshot.getDevDependencies(), alias)) {
                    newProjectSnapshot.getDevDependencies().put(alias,
                            projectSnapshot.getDevDependencies().get(alias));
                }
            }
        }
        alignDependencyTypes(newManifest, newProjectSnapshot);
        return newProjectSnapshot;
    }

    private static void alignDependencyTypes(Map<String, @Nullable Object> manifest, ProjectSnapshot projectSnapshot) {
        Map<String, String> depTypesOfAliases = new LinkedHashMap<>();
        for (String depType : ProjectSnapshot.DEPENDENCIES_FIELDS) {
            if (manifest.get(depType) == null) {
                continue;
            }
            for (String alias : Js.ownKeys(manifest.get(depType))) {
                depTypesOfAliases.putIfAbsent(alias, depType);
            }
        }
        for (String depType : ProjectSnapshot.DEPENDENCIES_FIELDS) {
            Map<String, @Nullable String> refs = projectSnapshot.field(depType);
            if (refs == null) {
                continue;
            }
            for (String alias : new ArrayList<>(Js.keys(refs))) {
                String target = depTypesOfAliases.get(alias);
                if (target == null || depType.equals(target)) {
                    continue;
                }
                projectSnapshot.field(target).put(alias, refs.get(alias));
                refs.remove(alias);
            }
        }
    }

    static String getSpecFromPackageManifest(Map<String, @Nullable Object> manifest, String depName) {
        for (String field : new String[]{"optionalDependencies", "dependencies", "devDependencies",
                "peerDependencies"}) {
            Object spec = ResolveDependencies.asMap(manifest.get(field)).get(depName);
            if (spec != null) {
                return Js.string(spec);
            }
        }
        return "";
    }
}
