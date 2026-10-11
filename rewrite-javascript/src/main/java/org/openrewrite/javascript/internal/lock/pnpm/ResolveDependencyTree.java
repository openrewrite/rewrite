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

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.DependenciesTreeNode;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.NodeId;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.PkgAddress;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.ResolutionContext;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.WantedDependency;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of @pnpm/installing.deps-resolver src/resolveDependencyTree.ts, src/toResolveImporter.ts,
 * src/getWantedDependencies.ts and src/validatePeerDependencies.ts (pnpm 11.2.2).
 */
final class ResolveDependencyTree {

    /**
     * The catalog lookup resolveDependencyTree attaches to a direct dependency saved to {@code saveCatalogName}: its
     * {@code userSpecifiedBareSpecifier}, or null where the default catalog already holds the alias or there is none.
     */
    static @Nullable String catalogLookup(String alias, @Nullable String saveCatalogName,
                                          @Nullable Map<String, @Nullable Object> defaultCatalog) {
        if (saveCatalogName == null || defaultCatalog != null && defaultCatalog.get(alias) != null) {
            return null;
        }
        return "catalog:" + ("default".equals(saveCatalogName) ? "" : saveCatalogName);
    }

    private ResolveDependencyTree() {
    }

    /**
     * One project to resolve: its importer id, directory and manifest, and the dependencies the install wants.
     */
    @Getter
    static final class Importer {
        private final String id;
        private final String rootDir;
        private final Map<String, @Nullable Object> manifest;
        private final List<WantedDependency> wantedDependencies;

        Importer(String id, String rootDir, Map<String, @Nullable Object> manifest,
                 List<WantedDependency> wantedDependencies) {
            this.id = id;
            this.rootDir = rootDir;
            this.manifest = manifest;
            this.wantedDependencies = wantedDependencies;
        }
    }

    /**
     * {@code ResolvedDirectDependency}, or a {@code LinkedDependency} when {@link PkgAddress#isLinkedDependency}.
     */
    @Value
    static class ResolvedDirectDependency {
        String alias;
        boolean optional;
        boolean dev;
        Map<String, @Nullable Object> resolution;
        String pkgId;

        @Nullable
        String version;

        @Nullable
        String name;

        ResolveDependencies.@Nullable CatalogLookup catalogLookup;

        @Nullable
        String normalizedBareSpecifier;

        boolean linkedDependency;
    }

    @Value
    static class ResolvedImporter {
        List<ResolvedDirectDependency> directDependencies;
        Map<String, NodeId> directNodeIdsByAlias;
        List<ResolvedDirectDependency> linkedDependencies;
    }

    @Value
    static class Result {
        Map<NodeId, DependenciesTreeNode> dependenciesTree;
        Map<String, ResolvedImporter> resolvedImporters;

        @Nullable
        Map<String, String> time;

        Set<String> allPeerDepNames;
        List<ResolveDependencies.PolicyViolation> resolutionPolicyViolations;
    }

    /**
     * getWantedDependencies.ts: the manifest's dependency fields (peers first under auto-install), each with its
     * group.
     */
    static List<WantedDependency> getWantedDependencies(Map<String, @Nullable Object> pkg, boolean autoInstallPeers) {
        Map<String, @Nullable Object> dependencies = ResolveDependencies.asMap(pkg.get("dependencies"));
        Map<String, @Nullable Object> devDependencies = ResolveDependencies.asMap(pkg.get("devDependencies"));
        Map<String, @Nullable Object> optionalDependencies =
                ResolveDependencies.asMap(pkg.get("optionalDependencies"));
        Map<String, @Nullable Object> peerDependencies = ResolveDependencies.asMap(pkg.get("peerDependencies"));
        Map<String, @Nullable Object> dependenciesMeta = ResolveDependencies.asMap(pkg.get("dependenciesMeta"));
        Map<String, @Nullable Object> depsToInstall = new LinkedHashMap<>();
        depsToInstall.putAll(devDependencies);
        depsToInstall.putAll(dependencies);
        depsToInstall.putAll(optionalDependencies);
        depsToInstall = Js.ordered(depsToInstall);
        if (autoInstallPeers) {
            Map<String, @Nullable Object> withPeers = new LinkedHashMap<>(peerDependencies);
            withPeers.putAll(depsToInstall);
            depsToInstall = Js.ordered(withPeers);
        }
        List<WantedDependency> wanted = new ArrayList<>();
        for (String alias : Js.keys(depsToInstall)) {
            String depType;
            if (optionalDependencies.get(alias) != null) {
                depType = "optional";
            } else if (dependencies.get(alias) != null) {
                depType = "prod";
            } else if (devDependencies.get(alias) != null) {
                depType = "dev";
            } else if (peerDependencies.get(alias) != null) {
                depType = "prod";
            } else {
                depType = null;
            }
            String bareSpecifier = Js.string(depsToInstall.get(alias));
            WantedDependency dependency = new WantedDependency(alias, bareSpecifier, "dev".equals(depType),
                    "optional".equals(depType));
            Object meta = dependenciesMeta.get(alias);
            if (meta instanceof Map && ((Map<?, ?>) meta).get("injected") instanceof Boolean) {
                dependency.setInjected((Boolean) ((Map<?, ?>) meta).get("injected"));
            }
            dependency.setPrevSpecifier(bareSpecifier);
            wanted.add(dependency);
        }
        return wanted;
    }

    /**
     * validatePeerDependencies.ts: each of a project's peer ranges is a semver range, a workspace: or a catalog: spec.
     */
    static void validatePeerDependencies(String rootDir, Map<String, @Nullable Object> manifest) {
        Map<String, @Nullable Object> peerDependencies = ResolveDependencies.asMap(manifest.get("peerDependencies"));
        Object name = manifest.get("name");
        String projectId = name != null ? Js.string(name) : rootDir;
        for (String depName : Js.keys(peerDependencies)) {
            String version = Js.string(peerDependencies.get(depName));
            if (!ReadPackageHook.isValidPeerRange(version)) {
                throw new PnpmError("INVALID_PEER_DEPENDENCY_SPECIFICATION", "The peerDependencies field named '" +
                        depName + "' of package '" + projectId + "' has an invalid value: '" + version + "'",
                        "The values in peerDependencies should be either a valid semver range, a `workspace:` spec, " +
                        "or a `catalog:` spec");
            }
        }
    }

    static Result resolveDependencyTree(List<Importer> importers, ResolutionContext ctx,
                                        PreferredVersions.ByName preferredVersions) {
        List<ResolveDependencies.ImporterToResolve> resolveArgs = new ArrayList<>();
        for (Importer importer : importers) {
            validatePeerDependencies(importer.getRootDir(), importer.getManifest());
            List<WantedDependency> wantedDependencies = new ArrayList<>();
            for (WantedDependency dep : importer.getWantedDependencies()) {
                String spec = dep.getBareSpecifier();
                dep.setUpdateDepth(spec.startsWith("file:") && spec.endsWith(".tgz") ? 0 : -1);
                wantedDependencies.add(dep);
            }
            ProjectSnapshot projectSnapshot = ctx.getWantedLockfile().getImporters().get(importer.getId());
            boolean proceed = ".".equals(importer.getId());
            for (WantedDependency dep : wantedDependencies) {
                proceed |= dep.isNew();
            }
            ResolveDependencies.ParentPkg parentPkg = new ResolveDependencies.ParentPkg(NodeId.of(importer.getId()),
                    true, importer.getRootDir(), false, importer.getId(), null);
            ResolveDependencies.ResolvedDependenciesOptions options =
                    new ResolveDependencies.ResolvedDependenciesOptions(parentPkg);
            options.setCurrentDepth(0);
            List<String> parentIds = new ArrayList<>();
            parentIds.add(importer.getId());
            options.setParentIds(parentIds);
            options.setProceed(proceed);
            Map<String, @Nullable String> resolvedDependencies = new LinkedHashMap<>();
            if (projectSnapshot.getDependencies() != null) {
                resolvedDependencies.putAll(projectSnapshot.getDependencies());
            }
            if (projectSnapshot.getDevDependencies() != null) {
                resolvedDependencies.putAll(projectSnapshot.getDevDependencies());
            }
            if (projectSnapshot.getOptionalDependencies() != null) {
                resolvedDependencies.putAll(projectSnapshot.getOptionalDependencies());
            }
            options.setResolvedDependencies(resolvedDependencies);
            options.setUpdateDepth(-1);
            options.setPrefix(importer.getRootDir());
            ResolveDependencies.ParentPkgAliases parentPkgAliases = new ResolveDependencies.ParentPkgAliases();
            for (WantedDependency dep : wantedDependencies) {
                if (Js.truthy(dep.getAlias())) {
                    parentPkgAliases.put(dep.getAlias(), Boolean.TRUE);
                }
            }
            ResolveDependencies.ImporterToResolve arg = new ResolveDependencies.ImporterToResolve(preferredVersions,
                    parentPkgAliases, wantedDependencies, options);
            arg.setUpdatePackageManifest(false);
            resolveArgs.add(arg);
        }
        ResolveDependencies.RootResult root = ctx.getLoop().await(
                ResolveDependencies.resolveRootDependencies(ctx, resolveArgs));

        for (ResolveDependencies.PendingNode pendingNode : ctx.getPendingNodes()) {
            DependenciesTreeNode node = new DependenciesTreeNode(pendingNode.getResolvedPackage(),
                    pendingNode.getDepth());
            node.setInstallable(pendingNode.isInstallable());
            String id = pendingNode.getResolvedPackage().getId();
            node.setLazyChildren(() -> buildTree(ctx, id, pendingNode.getParentIds(),
                    ctx.getChildrenByParentId().get(id), pendingNode.getDepth() + 1, pendingNode.isInstallable()));
            ctx.getDependenciesTree().put(pendingNode.getNodeId(), node);
        }

        Map<String, ResolvedImporter> resolvedImporters = new LinkedHashMap<>();
        for (int i = 0; i < importers.size(); i++) {
            Importer importer = importers.get(i);
            List<PkgAddress> directDeps = dedupeSameAliasDirectDeps(root.getPkgAddressesByImporters().get(i),
                    importer.getWantedDependencies());
            List<ResolvedDirectDependency> directDependencies = new ArrayList<>();
            List<ResolvedDirectDependency> linkedDependencies = new ArrayList<>();
            Map<String, NodeId> directNodeIdsByAlias = new LinkedHashMap<>();
            for (PkgAddress dep : directDeps) {
                if (dep.isLinkedDependency()) {
                    ResolvedDirectDependency linked = new ResolvedDirectDependency(dep.getAlias(), dep.isOptional(),
                            dep.isDev(), dep.getResolution() == null ? new LinkedHashMap<>() : dep.getResolution(),
                            dep.getPkgId(), dep.getVersion(), dep.getName(), dep.getCatalogLookup(),
                            dep.getNormalizedBareSpecifier(), true);
                    directDependencies.add(linked);
                    linkedDependencies.add(linked);
                    continue;
                }
                ResolveDependencies.ResolvedPackage resolvedPackage =
                        ctx.getDependenciesTree().get(dep.getNodeId()).getResolvedPackage();
                directDependencies.add(new ResolvedDirectDependency(dep.getAlias(), resolvedPackage.isOptional(),
                        resolvedPackage.isDev(), resolvedPackage.getResolution(), resolvedPackage.getId(),
                        resolvedPackage.getVersion(), resolvedPackage.getName(), dep.getCatalogLookup(),
                        dep.getNormalizedBareSpecifier(), false));
                directNodeIdsByAlias.put(dep.getAlias(), dep.getNodeId());
            }
            resolvedImporters.put(importer.getId(), new ResolvedImporter(directDependencies, directNodeIdsByAlias,
                    linkedDependencies));
        }
        return new Result(ctx.getDependenciesTree(), resolvedImporters, root.getTime(), ctx.getAllPeerDepNames(),
                ctx.getResolutionPolicyViolations());
    }

    private static Map<String, NodeId> buildTree(ResolutionContext ctx, String parentId, List<String> parentIds,
                                                 @Nullable List<ResolveDependencies.ChildId> children, int depth,
                                                 boolean installable) {
        Map<String, NodeId> childrenNodeIds = new LinkedHashMap<>();
        if (children == null) {
            return childrenNodeIds;
        }
        boolean installableSoFar = installable;
        for (ResolveDependencies.ChildId child : children) {
            String alias = child.getAlias();
            String id = child.getId();
            if (id.startsWith("link:")) {
                childrenNodeIds.put(alias, NodeId.of(id));
                continue;
            }
            if (ResolveDependencies.parentIdsContainSequence(parentIds, parentId, id) || parentId.equals(id)) {
                continue;
            }
            ResolveDependencies.ResolvedPackage resolved = ctx.getResolvedPkgsById().get(id);
            if (resolved.isLeaf()) {
                childrenNodeIds.put(alias, NodeId.of(id));
                continue;
            }
            NodeId childNodeId = ctx.nextNodeId();
            childrenNodeIds.put(alias, childNodeId);
            installableSoFar = installableSoFar || !ctx.getSkipped().contains(id);
            boolean childInstallable = installableSoFar;
            List<String> childParentIds = new ArrayList<>(parentIds);
            childParentIds.add(id);
            DependenciesTreeNode node = new DependenciesTreeNode(resolved, depth);
            node.setInstallable(childInstallable);
            node.setLazyChildren(() -> buildTree(ctx, id, childParentIds, ctx.getChildrenByParentId().get(id),
                    depth + 1, childInstallable));
            ctx.getDependenciesTree().put(childNodeId, node);
        }
        return childrenNodeIds;
    }

    private static List<PkgAddress> dedupeSameAliasDirectDeps(List<PkgAddress> directDeps,
                                                              List<WantedDependency> wantedDependencies) {
        Map<String, PkgAddress> deps = new LinkedHashMap<>();
        for (PkgAddress directDep : directDeps) {
            String alias = directDep.getAlias();
            if (!deps.containsKey(alias)) {
                deps.put(alias, directDep);
            } else {
                for (WantedDependency wanted : wantedDependencies) {
                    boolean same = Js.truthy(wanted.getAlias()) ? wanted.getAlias().equals(alias) :
                            wanted.getBareSpecifier().equals(directDep.getNormalizedBareSpecifier());
                    if (same) {
                        if (wanted.isNew()) {
                            deps.put(alias, directDep);
                        }
                        break;
                    }
                }
            }
        }
        return new ArrayList<>(deps.values());
    }
}
