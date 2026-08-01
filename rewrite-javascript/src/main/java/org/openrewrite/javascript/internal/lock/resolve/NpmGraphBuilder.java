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
package org.openrewrite.javascript.internal.lock.resolve;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openrewrite.javascript.internal.lock.EngineFailure;
import org.openrewrite.javascript.internal.registry.VersionManifest;
import org.openrewrite.semver.NodeSemver;

import java.util.*;

import static org.openrewrite.javascript.internal.LockFileRegeneration.Reason.RESOLUTION_REQUIRED;

/**
 * Builds the {@link ResolutionGraph} for the npm resolution of a clean closure: every package resolves to a
 * single version (the highest satisfying every requirer), with no fork, peer, or optional dependency. Anything
 * beyond that clean case fails loud — the fork/peer slices layer on top later. Version and constraint decisions
 * are delegated entirely to node-semver.
 */
public final class NpmGraphBuilder {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> ROOT_SCOPES = Arrays.asList("dependencies", "devDependencies");

    private final Registry registry;

    public NpmGraphBuilder(Registry registry) {
        this.registry = registry;
    }

    public ResolutionGraph build(Map<String, String> importerManifests) {
        Map<String, String> resolved = new LinkedHashMap<>();   // name -> chosen single version
        Deque<Requirement> work = new ArrayDeque<>();
        List<ImporterDecl> declared = new ArrayList<>();

        for (Map.Entry<String, String> e : importerManifests.entrySet()) {
            ImporterDecl decl = new ImporterDecl(e.getKey(), declaredScopes(e.getValue()));
            declared.add(decl);
            for (Map<String, String> scope : decl.scopes.values()) {
                for (Map.Entry<String, String> dep : scope.entrySet()) {
                    work.add(new Requirement(dep.getKey(), dep.getValue()));
                }
            }
        }

        while (!work.isEmpty()) {
            Requirement req = work.poll();
            String cur = resolved.get(req.name);
            if (cur != null) {
                if (!NodeSemver.satisfies(cur, req.range)) {
                    // Reconciling to a version satisfying both requirers, or forking, is a later slice.
                    throw new EngineFailure(RESOLUTION_REQUIRED, req.name,
                            req.name + " required at " + req.range + " but already resolved to " + cur + " (fork/upgrade)");
                }
                continue;
            }
            String version = NodeSemver.maxSatisfying(registry.versions(req.name), req.range);
            if (version == null) {
                throw new EngineFailure(RESOLUTION_REQUIRED, req.name, "no version of " + req.name + " satisfies " + req.range);
            }
            VersionManifest manifest = registry.manifest(req.name, version);
            requireCleanLeaf(manifest);
            resolved.put(req.name, version);
            if (manifest.getDependencies() != null) {
                for (Map.Entry<String, String> dep : manifest.getDependencies().entrySet()) {
                    work.add(new Requirement(dep.getKey(), dep.getValue()));
                }
            }
        }

        Map<String, ResolvedNode> nodes = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : resolved.entrySet()) {
            VersionManifest manifest = registry.manifest(e.getKey(), e.getValue());
            nodes.put(ResolutionGraph.key(e.getKey(), e.getValue()), new ResolvedNode(manifest, edgesOf(manifest, resolved)));
        }

        List<ResolutionGraph.Importer> importers = new ArrayList<>();
        for (ImporterDecl decl : declared) {
            Map<String, String> importerResolved = new LinkedHashMap<>();
            for (Map<String, String> scope : decl.scopes.values()) {
                for (String name : scope.keySet()) {
                    importerResolved.put(name, resolved.get(name));
                }
            }
            importers.add(new ResolutionGraph.Importer(decl.dir, decl.scopes, importerResolved));
        }
        return new ResolutionGraph(importers, nodes);
    }

    private static Map<String, String> edgesOf(VersionManifest manifest, Map<String, String> resolved) {
        Map<String, String> edges = new LinkedHashMap<>();
        if (manifest.getDependencies() != null) {
            for (String dep : manifest.getDependencies().keySet()) {
                edges.put(dep, resolved.get(dep));
            }
        }
        return edges;
    }

    private static void requireCleanLeaf(VersionManifest manifest) {
        if (notEmpty(manifest.getPeerDependencies()) || notEmpty(manifest.getOptionalDependencies())) {
            throw new EngineFailure(RESOLUTION_REQUIRED, manifest.getName(),
                    manifest.getName() + " declares peer/optional dependencies (not yet resolved)");
        }
    }

    private static boolean notEmpty(Map<String, String> m) {
        return m != null && !m.isEmpty();
    }

    private static Map<String, Map<String, String>> declaredScopes(String manifestJson) {
        Map<String, Map<String, String>> scopes = new LinkedHashMap<>();
        try {
            JsonNode root = JSON.readTree(manifestJson);
            for (String scope : ROOT_SCOPES) {
                JsonNode node = root.get(scope);
                if (node != null && node.isObject()) {
                    Map<String, String> deps = new LinkedHashMap<>();
                    node.fields().forEachRemaining(f -> deps.put(f.getKey(), f.getValue().asText()));
                    if (!deps.isEmpty()) {
                        scopes.put(scope, deps);
                    }
                }
            }
        } catch (Exception e) {
            throw new EngineFailure(RESOLUTION_REQUIRED, null, "could not parse importer manifest");
        }
        return scopes;
    }

    private static final class Requirement {
        final String name;
        final String range;

        Requirement(String name, String range) {
            this.name = name;
            this.range = range;
        }
    }

    private static final class ImporterDecl {
        final String dir;
        final Map<String, Map<String, String>> scopes;

        ImporterDecl(String dir, Map<String, Map<String, String>> scopes) {
            this.dir = dir;
            this.scopes = scopes;
        }
    }
}
