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
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.semver.NodeSemver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Port of @pnpm/workspace.projects-graph src/index.ts (pnpm 11.2.2), with the parts of @pnpm/npm-package-arg 2.0.0
 * and hosted-git-info 4.1.0 that type a dependency.
 */
final class ProjectsGraph {

    private static final Pattern IS_URL = Pattern.compile("^(?:git[+])?[a-z]+:", Pattern.CASE_INSENSITIVE);
    private static final Pattern IS_FILENAME = Pattern.compile("[.](?:tgz|tar.gz|tar)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern IS_FILESPEC = Pattern.compile("^(?:[.]|~[/]|[/]|[a-zA-Z]:)");
    private static final Pattern IS_FILE_PROTOCOL = Pattern.compile("^file:", Pattern.CASE_INSENSITIVE);
    // what encodeURIComponent escapes
    private static final Pattern URI_ENCODED = Pattern.compile("[^A-Za-z0-9\\-_.!~*'()]");

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

    // npa's result as far as createNode reads it
    @Value
    private static class Spec {
        String type;

        @Nullable
        String fetchSpec;
    }

    private static final class NpaError extends RuntimeException {
        NpaError(String message) {
            super(message);
        }
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
            throw new JsError(JsError.ERROR, raw == null || raw == JsYaml.UNDEFINED ?
                    "Cannot read properties of " + Js.string(raw) + " (reading 'startsWith')" :
                    "rawSpec.startsWith is not a function");
        }
        String rawSpec = (String) raw;
        boolean isWorkspaceSpec = rawSpec.startsWith("workspace:");
        Spec spec;
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
            spec = resolve(depName, rawSpec, project.getRootDir());
        } catch (NpaError | JsError | PnpmError e) {
            return null;
        }
        if ("directory".equals(spec.getType())) {
            Verification.Project found = spec.getFetchSpec() == null ? null : projectMap.get(spec.getFetchSpec());
            return found == null ? null : found.getRootDir();
        }
        if (!"version".equals(spec.getType()) && !"range".equals(spec.getType())) {
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
                    throw new JsError(JsError.ERROR, "Invalid version. Must be a string. Got type \"" + Js.typeOf(version) + "\".");
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

    // npa.js: the argument split into a name and a spec
    private Spec npa(String arg, String where) {
        int nameEndsAt = arg.startsWith("@") ? arg.substring(1).indexOf('@') + 1 : arg.indexOf('@');
        String namePart = nameEndsAt > 0 ? arg.substring(0, nameEndsAt) : arg;
        String name = null;
        String spec = null;
        if (IS_URL.matcher(arg).find()) {
            spec = arg;
        } else if (!namePart.startsWith("@") && (namePart.contains("/") || IS_FILENAME.matcher(namePart).find())) {
            spec = arg;
        } else if (nameEndsAt > 0) {
            name = namePart;
            spec = arg.substring(nameEndsAt + 1);
        } else if (ParseOverrides.validForOldPackages(arg)) {
            name = arg;
        } else {
            spec = arg;
        }
        return resolve(name, spec, where);
    }

    // npa.js resolve
    private Spec resolve(@Nullable String name, @Nullable String spec, String where) {
        if (name != null && !name.isEmpty() && !ParseOverrides.validForOldPackages(name)) {
            throw new NpaError("Invalid package name \"" + name + "\"");
        }
        if (spec == null || spec.isEmpty()) {
            return fromRegistry("");
        }
        if (IS_FILESPEC.matcher(spec).find() || IS_FILE_PROTOCOL.matcher(spec).find()) {
            return fromFile(spec, where);
        }
        if (spec.startsWith("npm:")) {
            return npa(spec.substring(4), where);
        }
        if (isHostedGit(spec, where)) {
            return new Spec("git", null);
        }
        if (IS_URL.matcher(spec).find()) {
            return new Spec("url", null);
        }
        if (spec.contains("/") || IS_FILENAME.matcher(spec).find()) {
            return fromFile(spec, where);
        }
        return fromRegistry(spec);
    }

    private Spec fromFile(String rawSpec, String where) {
        String type = IS_FILENAME.matcher(rawSpec).find() ? "file" : "directory";
        String spec = rawSpec.replace('\\', '/')
                .replaceFirst("(?i)^file:[/]*([A-Za-z]:)", "$1")
                .replaceFirst("^file:(?:[/]*([~./]))?", "$1");
        String fetchSpec = spec.startsWith("~/") || spec.startsWith("/") || spec.matches("^[A-Za-z]:.*") ? null :
                RootRelativePath.join(where, spec);
        if ("directory".equals(type) && (fetchSpec == null || RootRelativePath.climbsAboveRoot(fetchSpec))) {
            unknown.add(rawSpec);
            return new Spec(type, null);
        }
        return new Spec(type, fetchSpec);
    }

    private static Spec fromRegistry(String rawSpec) {
        String spec = rawSpec.isEmpty() ? "latest" : rawSpec;
        if (NodeSemver.valid(spec, NodeSemver.Options.LOOSE) != null) {
            return new Spec("version", spec);
        }
        if (NodeSemver.validRange(spec, NodeSemver.Options.LOOSE) != null) {
            return new Spec("range", spec);
        }
        if (URI_ENCODED.matcher(spec).find()) {
            throw new NpaError("Invalid tag name \"" + spec + "\"");
        }
        return new Spec("tag", spec);
    }

    // hosted-git-info 4.1.0 fromUrl(spec) != null, for the specs resolve reaches it with; a spec that names a user
    // before its host is a repository only on a host hosted-git-info knows
    private boolean isHostedGit(String spec, String where) {
        if (isGitHubShorthand(spec)) {
            return true;
        }
        if (spec.indexOf(':') < 0 && spec.indexOf('@') > -1 && spec.contains("/") &&
            projectMap.containsKey(RootRelativePath.join(where, spec))) {
            unknown.add(spec);
            return true;
        }
        return false;
    }

    private static boolean isGitHubShorthand(String arg) {
        int firstHash = arg.indexOf('#');
        int firstSlash = arg.indexOf('/');
        int secondSlash = arg.indexOf('/', firstSlash + 1);
        int firstColon = arg.indexOf(':');
        int firstSpace = -1;
        for (int i = 0; i < arg.length(); i++) {
            if (Js.trim(String.valueOf(arg.charAt(i))).isEmpty()) {
                firstSpace = i;
                break;
            }
        }
        int firstAt = arg.indexOf('@');
        boolean spaceOnlyAfterHash = firstSpace == -1 || firstHash > -1 && firstSpace > firstHash;
        boolean atOnlyAfterHash = firstAt == -1 || firstHash > -1 && firstAt > firstHash;
        boolean colonOnlyAfterHash = firstColon == -1 || firstHash > -1 && firstColon > firstHash;
        boolean secondSlashOnlyAfterHash = secondSlash == -1 || firstHash > -1 && secondSlash > firstHash;
        boolean hasSlash = firstSlash > 0;
        boolean doesNotEndWithSlash = firstHash > -1 ? firstHash == 0 || arg.charAt(firstHash - 1) != '/' :
                !arg.endsWith("/");
        boolean doesNotStartWithDot = !arg.startsWith(".");
        return spaceOnlyAfterHash && hasSlash && doesNotEndWithSlash && doesNotStartWithDot && atOnlyAfterHash &&
               colonOnlyAfterHash && secondSlashOnlyAfterHash;
    }
}
