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
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencies.NodeId;
import org.openrewrite.javascript.internal.lock.pnpm.ResolveDependencyTree.ResolvedDirectDependency;
import org.openrewrite.javascript.internal.lock.pnpm.ResolvePeers.DependenciesGraphNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Port of @pnpm/installing.deps-resolver src/dedupeInjectedDeps.ts (pnpm 11.2.2): an injected workspace project whose
 * dependencies are those its project resolves becomes a link to it.
 */
final class DedupeInjectedDeps {

    private DedupeInjectedDeps() {
    }

    static void dedupeInjectedDeps(Map<String, Map<String, String>> dependenciesByProjectId,
                                   List<ResolvePeers.ProjectToResolve> projects,
                                   Map<String, DependenciesGraphNode> depGraph, Map<NodeId, String> pathsByNodeId,
                                   String lockfileDir,
                                   Map<String, ResolveDependencyTree.ResolvedImporter> resolvedImporters,
                                   Set<String> workspaceProjectIds, PnpmRelease release) {
        Map<String, Map<String, String[]>> injectedDepsByProjects = new LinkedHashMap<>();
        for (ResolvePeers.ProjectToResolve project : projects) {
            for (Map.Entry<String, NodeId> direct : project.getDirectNodeIdsByAlias().entrySet()) {
                String depPath = pathsByNodeId.get(direct.getValue());
                String id = depGraph.get(depPath).getResolvedPackage().getId();
                if (!id.startsWith("file:")) {
                    continue;
                }
                String projectId = id.substring(5);
                if (workspaceProjectIds.contains(projectId)) {
                    injectedDepsByProjects.computeIfAbsent(project.getId(), k -> new LinkedHashMap<>())
                            .put(direct.getKey(), new String[]{depPath, projectId});
                }
            }
        }
        Map<String, Map<String, String>> dedupeMap = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String[]>> injected : injectedDepsByProjects.entrySet()) {
            Map<String, String> deduped = new LinkedHashMap<>();
            for (Map.Entry<String, String[]> dep : injected.getValue().entrySet()) {
                DependenciesGraphNode node = depGraph.get(dep.getValue()[0]);
                Map<String, String> children = node.getChildren();
                Map<String, String> targetProjectDeps = dependenciesByProjectId.get(dep.getValue()[1]);
                if (targetProjectDeps == null) {
                    // a target the install did not resolve: from 11.8.0 deduped by a dep path without peers,
                    // children or not (PnpmRelease#dedupesAnUnresolvedInjectedTargetByItsPeers); before, by having
                    // no children
                    boolean withoutPeers = node.getResolvedPackage().getPkgIdWithPatchHash().equals(dep.getValue()[0]);
                    if (withoutPeers == children.isEmpty() ? withoutPeers :
                            release.dedupesAnUnresolvedInjectedTargetByItsPeers() ? withoutPeers : children.isEmpty()) {
                        deduped.put(dep.getKey(), dep.getValue()[1]);
                    }
                    continue;
                }
                // every child its target project's own; from 11.13.0 also one of the same package that the target's
                // is compatible with and has as many dependencies as
                // (PnpmRelease#dedupesAnInjectedChildTheTargetCovers)
                boolean isSubset = true;
                boolean isCoveredSubset = true;
                for (Map.Entry<String, String> child : children.entrySet()) {
                    String target = targetProjectDeps == null ? null : targetProjectDeps.get(child.getKey());
                    boolean same = child.getValue().equals(target);
                    isSubset &= same;
                    isCoveredSubset &= same || coversInjectedChild(depGraph, target, child.getValue());
                }
                if (isSubset == isCoveredSubset ? isSubset :
                        release.dedupesAnInjectedChildTheTargetCovers() ? isCoveredSubset : isSubset) {
                    deduped.put(dep.getKey(), dep.getValue()[1]);
                }
            }
            dedupeMap.put(injected.getKey(), deduped);
        }
        for (Map.Entry<String, Map<String, String>> entry : dedupeMap.entrySet()) {
            String id = entry.getKey();
            ResolveDependencyTree.ResolvedImporter importer = resolvedImporters.get(id);
            for (Map.Entry<String, String> alias : entry.getValue().entrySet()) {
                dependenciesByProjectId.get(id).remove(alias.getKey());
                List<ResolvedDirectDependency> directDependencies = importer.getDirectDependencies();
                int index = -1;
                for (int i = 0; i < directDependencies.size(); i++) {
                    if (directDependencies.get(i).getAlias().equals(alias.getKey())) {
                        index = i;
                        break;
                    }
                }
                ResolvedDirectDependency prev = directDependencies.get(index);
                Map<String, @Nullable Object> resolution = new LinkedHashMap<>();
                resolution.put("type", "directory");
                resolution.put("directory", RootRelativePath.join(lockfileDir, alias.getValue()));
                ResolvedDirectDependency linkedDep = new ResolvedDirectDependency(prev.getAlias(), prev.isOptional(),
                        prev.isDev(), resolution, "link:" + RootRelativePath.relative(id, alias.getValue()),
                        prev.getVersion(), prev.getName(), prev.getCatalogLookup(), prev.getNormalizedBareSpecifier(),
                        true);
                directDependencies.set(index, linkedDep);
                importer.getLinkedDependencies().add(linkedDep);
            }
        }
    }

    // getDedupeMap's check of a child that is not the target project's own (11.13.0): the target's child of that alias
    // is a node of the same package that is compatible with the injected one and has as many dependencies
    private static boolean coversInjectedChild(Map<String, DependenciesGraphNode> depGraph,
                                               @Nullable String targetDepPath, String depPath) {
        if (targetDepPath == null) {
            return false;
        }
        DependenciesGraphNode targetNode = depGraph.get(targetDepPath);
        DependenciesGraphNode injectedChildNode = depGraph.get(depPath);
        if (targetNode == null || injectedChildNode == null) {
            return false;
        }
        if (!Objects.equals(targetNode.getResolvedPackage().getPkgIdWithPatchHash(),
                injectedChildNode.getResolvedPackage().getPkgIdWithPatchHash())) {
            return false;
        }
        return ResolvePeers.isCompatibleAndHasMoreDeps(depGraph, targetDepPath, depPath);
    }
}
