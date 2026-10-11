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
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.javascript.internal.lock.Spec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of @pnpm/workspace.projects-graph src/index.ts (pnpm 11.2.2).
 */
final class ProjectsGraph {

    // two places for the workspace, whose own location the engine does not know
    private static final String[] PLACEMENTS = {"/", "/workspace"};

    private final Map<String, Verification.Project> projectMap = new LinkedHashMap<>();
    private final List<Unmatched> unmatched = new ArrayList<>();
    private final List<String> unknown = new ArrayList<>();

    private ProjectsGraph() {
    }

    @Value
    static class Unmatched {
        String pkgName;
        String range;
    }

    @Value
    static class Result {
        /**
         * Each project's directory with the directories of the projects it depends on.
         */
        Map<String, List<String>> graph;

        List<Unmatched> unmatched;

        /**
         * The dependencies whose edge the engine cannot know: a directory outside the workspace, which may be one
         * of its projects.
         */
        List<String> unknown;
    }

    static Result createProjectsGraph(List<Verification.Project> projects, boolean ignoreDevDeps,
                                      @Nullable Boolean linkWorkspacePackages) {
        ProjectsGraph projectsGraph = new ProjectsGraph();
        for (Verification.Project project : projects) {
            projectsGraph.projectMap.put(project.getRootDir(), project);
        }
        Map<String, List<String>> graph = new LinkedHashMap<>();
        for (Verification.Project project : projectsGraph.projectMap.values()) {
            graph.put(project.getRootDir(), projectsGraph.createNode(project, ignoreDevDeps, linkWorkspacePackages));
        }
        return new Result(graph, projectsGraph.unmatched, projectsGraph.unknown);
    }

    @SuppressWarnings("unchecked")
    private List<String> createNode(Verification.Project project, boolean ignoreDevDeps,
                                    @Nullable Boolean linkWorkspacePackages) {
        Map<String, @Nullable Object> manifest = project.getManifest();
        Map<String, @Nullable Object> dependencies = new LinkedHashMap<>();
        for (String field : new String[]{"peerDependencies", "devDependencies", "optionalDependencies",
                "dependencies"}) {
            Object deps = manifest.get(field);
            if (deps instanceof Map && !(ignoreDevDeps && "devDependencies".equals(field))) {
                for (String key : Js.keys((Map<String, @Nullable Object>) deps)) {
                    dependencies.put(key, ((Map<String, @Nullable Object>) deps).get(key));
                }
            }
        }
        List<String> edges = new ArrayList<>();
        for (String key : Js.keys(dependencies)) {
            String edge = edge(project, key, dependencies.get(key), linkWorkspacePackages);
            if (edge != null) {
                edges.add(edge);
            }
        }
        return edges;
    }

    private @Nullable String edge(Verification.Project project, String depName, @Nullable Object raw,
                                  @Nullable Boolean linkWorkspacePackages) {
        if (!(raw instanceof String)) {
            throw new JsError(JsError.ERROR, raw == null || raw == Js.UNDEFINED ?
                    "Cannot read properties of " + Js.string(raw) + " (reading 'startsWith')" :
                    "rawSpec.startsWith is not a function");
        }
        String rawSpec = (String) raw;
        boolean isWorkspaceSpec = rawSpec.startsWith("workspace:");
        String dir;
        Spec.Type type;
        try {
            if (isWorkspaceSpec) {
                PickPackageFromMeta.RegistryPackageSpec parsed = PickPackageFromMeta.parseBareSpecifier(
                        NpmResolver.workspacePrefToNpm(rawSpec), depName, "latest", "");
                if (parsed == null) {
                    return null;
                }
                rawSpec = parsed.getFetchSpec();
                depName = parsed.getName();
            }
            type = Spec.resolve(depName, rawSpec, PosixPath.join(PLACEMENTS[0], project.getRootDir()),
                    Spec.Release.PNPM_2_0_0).getType();
            dir = type == Spec.Type.DIRECTORY ? directory(depName, rawSpec, project.getRootDir()) : null;
        } catch (IllegalArgumentException | JsError | PnpmError e) {
            return null;
        }
        if (type == Spec.Type.DIRECTORY) {
            Verification.Project found = dir == null ? null : projectMap.get(dir);
            return found == null ? null : found.getRootDir();
        }
        if (type != Spec.Type.VERSION && type != Spec.Type.RANGE) {
            return null;
        }
        List<Verification.Project> candidates = new ArrayList<>();
        for (Verification.Project candidate : projectMap.values()) {
            if (depName.equals(candidate.getManifest().get("name"))) {
                candidates.add(candidate);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        List<@Nullable Object> versions = new ArrayList<>();
        for (Verification.Project candidate : candidates) {
            if (Js.truthy(candidate.getManifest().get("version"))) {
                versions.add(candidate.getManifest().get("version"));
            }
        }
        if (Boolean.FALSE.equals(linkWorkspacePackages) && !isWorkspaceSpec) {
            unmatched.add(new Unmatched(depName, rawSpec));
            return null;
        }
        if (isWorkspaceSpec && versions.isEmpty()) {
            return candidates.get(0).getRootDir();
        }
        Object matched = rawSpec;
        if (!versions.contains(rawSpec)) {
            List<String> strings = new ArrayList<>();
            for (Object version : versions) {
                if (!(version instanceof String)) {
                    throw new JsError(JsError.ERROR,
                            "Invalid version. Must be a string. Got type \"" + Js.typeOf(version) + "\".");
                }
                strings.add((String) version);
            }
            matched = NpmResolver.resolveWorkspaceRange(rawSpec, strings);
            if (matched == null) {
                unmatched.add(new Unmatched(depName, rawSpec));
                return null;
            }
        }
        for (Verification.Project candidate : candidates) {
            if (matched.equals(candidate.getManifest().get("version"))) {
                return candidate.getRootDir();
            }
        }
        return null;
    }

    // path.resolve(project.rootDir, spec.fetchSpec) relative to the workspace, the same wherever the workspace lies, or
    // null for a path that leaves it (unknown)
    private @Nullable String directory(String depName, String rawSpec, String rootDir) {
        String dir = null;
        for (String root : PLACEMENTS) {
            String where = PosixPath.join(root, rootDir);
            String fetchSpec = Spec.resolve(depName, rawSpec, where, Spec.Release.PNPM_2_0_0).getFetchSpec();
            String relative = PosixPath.relative(root, PosixPath.resolve(where, String.valueOf(fetchSpec)));
            if (RootRelativePath.climbsAboveRoot(relative) || dir != null && !dir.equals(relative)) {
                unknown.add(rawSpec);
                return null;
            }
            dir = relative;
        }
        return dir;
    }
}
