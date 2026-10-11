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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.PeerDependency;
import org.openrewrite.javascript.internal.lock.pnpm.ResolvePeers.DependenciesGraphNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of @pnpm/installing.deps-resolver src/updateLockfile.ts, src/depPathToRef.ts and src/getCatalogSnapshots.ts,
 * with @pnpm/lockfile.pruner's {@code pruneSharedLockfile} (pnpm 11.2.2).
 */
final class UpdateLockfile {

    private UpdateLockfile() {
    }

    static String depPathToRef(String depPath, String alias, String realName) {
        if (alias.equals(realName) && depPath.startsWith(realName + "@")) {
            return depPath.substring(realName.length() + 1);
        }
        return depPath;
    }

    static LockfileObject updateLockfile(Map<String, DependenciesGraphNode> dependenciesGraph,
                                         LockfileObject lockfile, Map<String, String> registries,
                                         boolean lockfileIncludeTarballUrl) {
        if (lockfile.getPackages() == null) {
            lockfile.setPackages(new LinkedHashMap<>());
        }
        for (Map.Entry<String, DependenciesGraphNode> entry : dependenciesGraph.entrySet()) {
            DependenciesGraphNode depNode = entry.getValue();
            Map<String, String> updatedOptionalDeps = new LinkedHashMap<>();
            Map<String, String> updatedDeps = new LinkedHashMap<>();
            for (Map.Entry<String, String> child : depNode.getChildren().entrySet()) {
                PeerDependency peer = depNode.getPeerDependencies().get(child.getKey());
                boolean optional = depNode.getResolvedPackage().getOptionalDependencies().contains(child.getKey()) ||
                                   peer != null && Boolean.TRUE.equals(peer.getOptional());
                (optional ? updatedOptionalDeps : updatedDeps).put(child.getKey(), child.getValue());
            }
            String registry = DepPath.getRegistryByPackageName(registries, depNode.getResolvedPackage().getName());
            lockfile.getPackages().put(entry.getKey(), toLockfileDependency(depNode, entry.getKey(), registry,
                    updatedDeps, updatedOptionalDeps, dependenciesGraph, lockfileIncludeTarballUrl));
        }
        return pruneSharedLockfile(lockfile, dependenciesGraph);
    }

    private static PackageSnapshot toLockfileDependency(DependenciesGraphNode pkg, String depPath, String registry,
                                                        Map<String, String> updatedDeps,
                                                        Map<String, String> updatedOptionalDeps,
                                                        Map<String, DependenciesGraphNode> depGraph,
                                                        boolean lockfileIncludeTarballUrl) {
        ResolveDependencies.ResolvedPackage resolved = pkg.getResolvedPackage();
        Map<String, @Nullable Object> lockfileResolution = LockfileUtils.toLockfileResolution(resolved.getName(),
                resolved.getVersion(), resolved.getResolution(), registry, lockfileIncludeTarballUrl);
        Map<String, @Nullable String> newResolvedDeps = updateResolvedDeps(updatedDeps, depGraph);
        Map<String, @Nullable String> newResolvedOptionalDeps = updateResolvedDeps(updatedOptionalDeps, depGraph);
        PackageSnapshot result = new PackageSnapshot();
        result.setResolution(lockfileResolution);
        if (depPath.contains(":") && Js.truthy(resolved.getVersion()) &&
            !"directory".equals(lockfileResolution.get("type"))) {
            result.setVersion(resolved.getVersion());
        }
        if (!newResolvedDeps.isEmpty()) {
            result.setDependencies(newResolvedDeps);
        }
        if (!newResolvedOptionalDeps.isEmpty()) {
            result.setOptionalDependencies(newResolvedOptionalDeps);
        }
        if (pkg.isOptional()) {
            result.setOptional(true);
        }
        if (!pkg.getTransitivePeerDependencies().isEmpty()) {
            List<String> transitive = new ArrayList<>(pkg.getTransitivePeerDependencies());
            Collections.sort(transitive);
            result.setTransitivePeerDependencies(transitive);
        }
        if (!pkg.getPeerDependencies().isEmpty()) {
            Map<String, @Nullable Object> peerPkgs = new LinkedHashMap<>();
            Map<String, @Nullable Object> meta = new LinkedHashMap<>();
            for (Map.Entry<String, PeerDependency> peer : pkg.getPeerDependencies().entrySet()) {
                peerPkgs.put(peer.getKey(), peer.getValue().getVersion());
                if (Boolean.TRUE.equals(peer.getValue().getOptional())) {
                    Map<String, @Nullable Object> optional = new LinkedHashMap<>();
                    optional.put("optional", true);
                    meta.put(peer.getKey(), optional);
                }
            }
            result.setPeerDependencies(peerPkgs);
            if (!meta.isEmpty()) {
                result.setPeerDependenciesMeta(meta);
            }
        }
        Map<String, @Nullable Object> info = resolved.getAdditionalInfo();
        Object engines = info.get("engines");
        if (engines != null && engines != Js.UNDEFINED) {
            Map<String, @Nullable Object> lockEngines = null;
            for (Map.Entry<String, @Nullable Object> engine : Js.entries(engines)) {
                if ("*".equals(engine.getValue())) {
                    continue;
                }
                if (lockEngines == null) {
                    lockEngines = new LinkedHashMap<>();
                }
                lockEngines.put(engine.getKey(), engine.getValue());
            }
            if (lockEngines != null) {
                result.setEngines(lockEngines);
            }
        }
        if (present(info.get("cpu"))) {
            result.setCpu(info.get("cpu"));
        }
        if (present(info.get("os"))) {
            result.setOs(info.get("os"));
        }
        if (present(info.get("libc"))) {
            result.setLibc(info.get("libc"));
        }
        Object bundled = info.get("bundledDependencies");
        Object bundle = info.get("bundleDependencies");
        if (bundled instanceof List || Boolean.TRUE.equals(bundled)) {
            result.setBundledDependencies(bundled);
        } else if (bundle instanceof List || Boolean.TRUE.equals(bundle)) {
            result.setBundledDependencies(bundle);
        }
        if (Js.truthy(info.get("deprecated"))) {
            result.setDeprecated(info.get("deprecated"));
        }
        if (resolved.isHasBin()) {
            result.setHasBin(true);
        }
        if (resolved.getPatch() != null) {
            result.setPatched(true);
        }
        return result;
    }

    private static boolean present(@Nullable Object value) {
        return value != null && value != Js.UNDEFINED;
    }

    private static Map<String, @Nullable String> updateResolvedDeps(Map<String, String> updatedDeps,
                                                                    Map<String, DependenciesGraphNode> depGraph) {
        Map<String, @Nullable String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> dep : updatedDeps.entrySet()) {
            String depPath = dep.getValue();
            if (depPath.startsWith("link:")) {
                out.put(dep.getKey(), depPath);
                continue;
            }
            DependenciesGraphNode depNode = depGraph.get(depPath);
            out.put(dep.getKey(), depPathToRef(depPath, dep.getKey(), depNode.getResolvedPackage().getName()));
        }
        return out;
    }

    /**
     * @pnpm/lockfile.pruner {@code pruneSharedLockfile}: the packages reachable from the importers, each marked
     * optional when reached only through optional edges.
     */
    static LockfileObject pruneSharedLockfile(LockfileObject lockfile,
                                              @Nullable Map<String, DependenciesGraphNode> dependenciesGraph) {
        Map<String, PackageSnapshot> copied = new LinkedHashMap<>();
        if (lockfile.getPackages() != null) {
            List<String> devDepPaths = new ArrayList<>();
            List<String> optionalDepPaths = new ArrayList<>();
            List<String> prodDepPaths = new ArrayList<>();
            for (ProjectSnapshot importer : lockfile.getImporters().values()) {
                devDepPaths.addAll(resolvedDepsToDepPaths(importer.getDevDependencies()));
            }
            for (ProjectSnapshot importer : lockfile.getImporters().values()) {
                optionalDepPaths.addAll(resolvedDepsToDepPaths(importer.getOptionalDependencies()));
            }
            for (ProjectSnapshot importer : lockfile.getImporters().values()) {
                prodDepPaths.addAll(resolvedDepsToDepPaths(importer.getDependencies()));
            }
            Set<String> nonOptional = new LinkedHashSet<>();
            Set<String> walked = new LinkedHashSet<>();
            copyDependencySubGraph(copied, nonOptional, lockfile.getPackages(), walked, dependenciesGraph,
                    devDepPaths, false);
            copyDependencySubGraph(copied, nonOptional, lockfile.getPackages(), walked, dependenciesGraph,
                    optionalDepPaths, true);
            copyDependencySubGraph(copied, nonOptional, lockfile.getPackages(), walked, dependenciesGraph,
                    prodDepPaths, false);
        }
        lockfile.setPackages(copied.isEmpty() ? null : copied);
        return lockfile;
    }

    private static List<String> resolvedDepsToDepPaths(@Nullable Map<String, @Nullable String> deps) {
        List<String> out = new ArrayList<>();
        if (deps == null) {
            return out;
        }
        for (Map.Entry<String, @Nullable String> dep : deps.entrySet()) {
            String depPath = dep.getValue() == null ? null : DepPath.refToRelative(dep.getValue(), dep.getKey());
            if (depPath != null) {
                out.add(depPath);
            }
        }
        return out;
    }

    private static void copyDependencySubGraph(Map<String, PackageSnapshot> copied, Set<String> nonOptional,
                                               Map<String, PackageSnapshot> originalPackages, Set<String> walked,
                                               @Nullable Map<String, DependenciesGraphNode> dependenciesGraph,
                                               List<String> depPaths, boolean optional) {
        for (String depPath : depPaths) {
            String key = depPath + ":" + optional;
            if (!walked.add(key)) {
                continue;
            }
            PackageSnapshot depLockfile = originalPackages.get(depPath);
            if (depLockfile == null) {
                continue;
            }
            copied.put(depPath, depLockfile);
            DependenciesGraphNode graphNode = dependenciesGraph == null ? null : dependenciesGraph.get(depPath);
            if (optional && !nonOptional.contains(depPath)) {
                depLockfile.setOptional(true);
                if (graphNode != null) {
                    graphNode.setOptional(true);
                }
            } else {
                nonOptional.add(depPath);
                depLockfile.setOptional(null);
                if (graphNode != null) {
                    graphNode.setOptional(false);
                }
            }
            copyDependencySubGraph(copied, nonOptional, originalPackages, walked, dependenciesGraph,
                    resolvedDepsToDepPaths(depLockfile.getDependencies()), optional);
            copyDependencySubGraph(copied, nonOptional, originalPackages, walked, dependenciesGraph,
                    resolvedDepsToDepPaths(depLockfile.getOptionalDependencies()), true);
        }
    }

    /**
     * getCatalogSnapshots.ts: the catalog entries the importers' direct dependencies resolved through.
     */
    static Map<String, @Nullable Object> getCatalogSnapshots(
            List<ResolveDependencyTree.ResolvedDirectDependency> resolvedDirectDeps) {
        Map<String, @Nullable Object> catalogSnapshots = new LinkedHashMap<>();
        for (ResolveDependencyTree.ResolvedDirectDependency dep : resolvedDirectDeps) {
            if (dep.getCatalogLookup() == null) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, @Nullable Object> snapshot = (Map<String, @Nullable Object>) catalogSnapshots
                    .computeIfAbsent(dep.getCatalogLookup().getCatalogName(), k -> new LinkedHashMap<>());
            Map<String, @Nullable Object> entry = new LinkedHashMap<>();
            entry.put("specifier", dep.getCatalogLookup().getSpecifier());
            entry.put("version", dep.getVersion() == null ? Js.UNDEFINED : dep.getVersion());
            snapshot.put(dep.getAlias(), entry);
        }
        return catalogSnapshots;
    }
}
