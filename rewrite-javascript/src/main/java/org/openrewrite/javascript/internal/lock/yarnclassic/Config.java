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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.LocaleCompare;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator.Inputs;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.singletonList;

/**
 * Port of yarn src/config.js (yarn 1.22.22): {@code init}, the workspace root and its members, and the manifests it
 * reads.
 */
@Getter
final class Config {

    private final Inputs inputs;

    /**
     * {@code workspaceRootFolder} as directories above the project's: 0 the project's own, -1 none.
     */
    private int workspaceRoot = -1;

    /**
     * The manifest of {@code lockfileFolder}, read once {@link #init} found it.
     */
    private @Nullable Manifest lockfileFolderManifest;

    private boolean focus;

    /**
     * The name of the workspace a {@code --focus} install runs for: the manifest's where yarn runs.
     */
    private @Nullable Object focusedWorkspaceName;

    Config(Inputs inputs) {
        this.inputs = inputs;
    }

    /**
     * {@code init}: the workspace root from where yarn runs up, the focus check, the registries' config, the manifest
     * of the lock's directory, the workspaces check.
     */
    YarnClassicOptions init(Map<String, @Nullable Object> rc, List<String> warnings) {
        boolean looseSemver = YarnClassicOptions.looseSemver(rc);
        requireWorkspaceRootAbove(looseSemver);
        workspaceRoot = findWorkspaceRoot(looseSemver, warnings);
        focus = YarnClassicOptions.focus(rc);
        if (focus && (workspaceRoot < 0 || workspaceRoot == 0 && inputs.getCwd().isEmpty())) {
            throw new MessageError("This command can only be run inside an individual workspace.");
        }
        if (workspaceRoot < 0 && !inputs.getCwd().isEmpty()) {
            throw new CannotKnow("the lock yarn writes: run in " + inputs.getCwd() + ", which no workspaces list, it " +
                                 "makes that directory its lockfileFolder and writes its yarn.lock, not this lock");
        }
        if (focus) {
            focusedWorkspaceName = readRootManifest(looseSemver, warnings).getName();
        }
        YarnClassicOptions config = YarnClassicOptions.load(rc, inputs.configFilesFromCwd(), inputs.getEnv());
        String up = YarnClassicOptions.up(Math.max(workspaceRoot, 0));
        String lockfileFolder = up.isEmpty() ? "" : up.substring(0, up.length() - 1);
        String manifest = up.isEmpty() ? inputs.getManifests().get("") :
                inputs.getConfigFiles().get(up + "package.json");
        lockfileFolderManifest = manifest == null ? null :
                tryManifest(lockfileFolder, manifest, looseSemver, false, lockfileFolder, warnings);
        if (workspaceRoot >= 0 && !config.isWorkspacesEnabled()) {
            throw new MessageError("Your project root defines workspaces but the feature is disabled in your Yarn " +
                                   "config. Please check \"workspaces-experimental\" in your .yarnrc file.");
        }
        return config;
    }

    // readRootManifest: the manifest where yarn runs, which yarn requires
    private Manifest readRootManifest(boolean looseSemver, List<String> warnings) {
        String cwd = inputs.getCwd();
        String text = inputs.getManifests().get(cwd);
        if (text == null) {
            if (inputs.getPaths() != null && !inputs.getPaths().contains(cwd + "/package.json")) {
                throw new MessageError("Couldn't find a package.json file in \"" + cwd + "\"", "ENOENT");
            }
            throw new CannotKnow(cwd + "/package.json (its contents were not read)");
        }
        return tryManifest(cwd, text, looseSemver, true, "", warnings);
    }

    // findWorkspaceRoot starts where yarn runs: a manifest with workspaces below this lock's stops it there
    private void requireWorkspaceRootAbove(boolean looseSemver) {
        Set<String> paths = inputs.getPaths();
        for (String dir = inputs.getCwd(); !dir.isEmpty(); dir = dir.contains("/") ?
                dir.substring(0, dir.lastIndexOf('/')) : "") {
            String file = dir + "/package.json";
            String text = inputs.getManifests().containsKey(dir) ? inputs.getManifests().get(dir) :
                    inputs.getConfigFiles().get(file);
            if (text == null) {
                if (paths != null && paths.contains(file)) {
                    throw new CannotKnow(file + " (its contents were not read)");
                }
                continue;
            }
            Map<String, @Nullable Object> ws = extractWorkspaces(tryManifest(dir, text, looseSemver, true, null,
                    new ArrayList<>()));
            if (ws != null && Js.truthy(ws.get("packages"))) {
                throw new CannotKnow("the lock yarn writes: run in " + inputs.getCwd() + " it stops at the " +
                                     "workspaces of " + file + ", not at this lock");
            }
        }
    }

    /**
     * {@code findWorkspaceRoot}: how many directories above the project's the nearest manifest with a workspaces
     * list is when it is where yarn runs or that list matches the path from it to there, 0 the project's, else -1.
     */
    int findWorkspaceRoot(boolean looseSemver, List<String> warnings) {
        String cwd = inputs.getCwd();
        for (int depth = 0; depth <= inputs.reach(); depth++) {
            Manifest manifest = findManifest(depth, looseSemver, warnings);
            Map<String, @Nullable Object> ws = manifest == null ? null : extractWorkspaces(manifest);
            if (ws != null && Js.truthy(ws.get("packages"))) {
                String relativePath = depth == 0 ? cwd : cwd.isEmpty() ? inputs.relativePath(depth) :
                        inputs.relativePath(depth) + "/" + cwd;
                return relativePath.isEmpty() ||
                       !Micromatch.micromatch(singletonList(relativePath), ws.get("packages")).isEmpty() ? depth : -1;
            }
        }
        return -1;
    }

    // findManifest: the directory's package.json, else its yarn.json
    private @Nullable Manifest findManifest(int depth, boolean looseSemver, List<String> warnings) {
        String up = YarnClassicOptions.up(depth);
        String text = depth == 0 ? inputs.getManifests().get("") : inputs.getConfigFiles().get(up + "package.json");
        if (text != null) {
            return tryManifest(depth == 0 ? "" : up.substring(0, up.length() - 1), text, looseSemver, true, null,
                    warnings);
        }
        Set<String> paths = inputs.getPaths();
        for (String name : depth == 0 ? singletonList("yarn.json") : Arrays.asList("package.json", "yarn.json")) {
            if (paths != null && paths.contains(up + name)) {
                throw new CannotKnow(up + name + " (its contents were not read)");
            }
        }
        return null;
    }

    /**
     * {@code tryManifest}: the manifest read, its registry and location set, then normalized.
     */
    static Manifest tryManifest(String location, String text, boolean looseSemver, boolean isRoot,
                                @Nullable String lockfileFolder, List<String> warnings) {
        String file = location.isEmpty() ? "package.json" : location + "/package.json";
        Manifest manifest = manifest(Fs.readJson(file, text), "_registry");
        manifest.put("_registry", NpmResolver.NPM_REGISTRY_ID);
        manifest.put("_loc", file);
        return NormalizeManifest.normalizeManifest(manifest, location, lockfileFolder, isRoot, looseSemver, warnings);
    }

    /**
     * What {@link #readManifest} finds in a directory that holds no manifest.
     */
    static final Object NO_MANIFEST = new Object();

    /**
     * {@code readManifest} of a directory a fetch extracted into yarn's cache: its .yarn-metadata.json's manifest as
     * it is, else its package.json or yarn.json normalized, else {@link #NO_MANIFEST}.
     */
    static @Nullable Object readManifest(TarFs dest, String dir, Object registry, boolean looseSemver,
                                         List<String> warnings) {
        TarFs.Node metadata = dest.get(".yarn-metadata.json");
        if (metadata != null) {
            Object json = Fs.readJson(PosixPath.join(dir, ".yarn-metadata.json"), text(metadata));
            if (json == null) {
                throw new JsError(JsError.TYPE_ERROR, "Cannot read properties of null (reading 'manifest')");
            }
            if (json instanceof Map && ((Map<?, ?>) json).containsKey("manifest")) {
                Object manifest = ((Map<?, ?>) json).get("manifest");
                return Js.truthy(manifest) ? manifest : NO_MANIFEST;
            }
        }
        List<String> names = "yarn".equals(registry) ? Arrays.asList("yarn.json", "package.json") :
                Arrays.asList("package.json", "yarn.json");
        for (String filename : names) {
            TarFs.Node file = dest.get(filename);
            if (file == null) {
                continue;
            }
            String loc = PosixPath.join(dir, filename);
            Manifest manifest = manifest(Fs.readJson(loc, text(file)), "_registry");
            manifest.put("_registry", "yarn.json".equals(filename) ? "yarn" : NpmResolver.NPM_REGISTRY_ID);
            manifest.put("_loc", loc);
            return NormalizeManifest.normalizeManifest(manifest, null, "", false, looseSemver, warnings);
        }
        return NO_MANIFEST;
    }

    // readFile: utf8 with CRLF as LF
    private static String text(TarFs.Node node) {
        if (node.directory) {
            throw new JsError(JsError.ERROR, "EISDIR: illegal operation on a directory, read");
        }
        return new String(node.content(), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    // an object, or an array with its indices; a property set on any other value is a TypeError
    @SuppressWarnings("unchecked")
    static Manifest manifest(@Nullable Object json, String property) {
        if (json instanceof Map) {
            return new Manifest((Map<String, @Nullable Object>) json);
        }
        if (json instanceof List) {
            Map<String, @Nullable Object> object = new LinkedHashMap<>();
            Js.assign(object, json);
            return new Manifest(object);
        }
        if (json == null) {
            throw new JsError(JsError.TYPE_ERROR, "Cannot set properties of null (setting '" + property + "')");
        }
        throw new JsError(JsError.TYPE_ERROR, "Cannot create property '" + property + "' on " + Js.typeOf(json) +
                                              " '" + Js.string(json) + "'");
    }

    /**
     * Whether a manifest makes its directory a workspace root, as {@code findWorkspaceRoot} reads it: a
     * {@code workspaces} array, even an empty one, or an object with one.
     */
    static boolean isWorkspaceRoot(Manifest manifest) {
        Map<String, @Nullable Object> ws = extractWorkspaces(manifest);
        return ws != null && Js.truthy(ws.get("packages"));
    }

    @SuppressWarnings("unchecked")
    static @Nullable Map<String, @Nullable Object> extractWorkspaces(Manifest manifest) {
        Object workspaces = manifest.get("workspaces");
        if (!Js.truthy(workspaces)) {
            return null;
        }
        if (workspaces instanceof List) {
            Map<String, @Nullable Object> ws = new LinkedHashMap<>();
            ws.put("packages", workspaces);
            return ws;
        }
        if (workspaces instanceof Map) {
            Map<String, @Nullable Object> ws = (Map<String, @Nullable Object>) workspaces;
            if ((Js.truthy(ws.get("packages")) && ws.get("packages") instanceof List) ||
                (Js.truthy(ws.get("nohoist")) && ws.get("nohoist") instanceof List)) {
                return ws;
            }
        }
        return null;
    }

    /**
     * {@code getWorkspaces}: the root's workspaces config once it is eligible.
     * @throws MessageError members or a nohoist list in a root that is not private, when {@code shouldThrow}.
     */
    static @Nullable Map<String, @Nullable Object> getWorkspaces(Manifest manifest, boolean shouldThrow,
                                                                   YarnClassicOptions config,
                                                                   List<String> warnings) {
        if (!config.isWorkspacesEnabled()) {
            return null;
        }
        Map<String, @Nullable Object> ws = extractWorkspaces(manifest);
        if (ws == null) {
            return null;
        }
        Map<String, @Nullable Object> wsCopy = new LinkedHashMap<>(ws);
        List<String> warned = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        boolean isPrivate = Js.truthy(manifest.get("private"));
        Object packages = wsCopy.get("packages");
        if (Js.truthy(packages) && Js.length(packages) > 0 && !isPrivate) {
            errors.add("Workspaces can only be enabled in private projects.");
            wsCopy = null;
        }
        if (wsCopy != null && Js.truthy(wsCopy.get("nohoist")) && Js.length(wsCopy.get("nohoist")) > 0) {
            if (!config.isWorkspacesNohoistEnabled()) {
                warned.add(Lang.format("$0 defines nohoist but the feature is disabled in your Yarn config " +
                                       "(\"workspaces-nohoist-experimental\" in .yarnrc file)", manifest.getName()));
                wsCopy.put("nohoist", null);
            } else if (!isPrivate) {
                errors.add(Lang.format("nohoist config is ignored in $0 because it is not a private package. If " +
                                       "you think nohoist should be allowed in public packages, please submit an " +
                                       "issue for your use case.", manifest.getName()));
                wsCopy.put("nohoist", null);
            }
        }
        if (!errors.isEmpty() && shouldThrow) {
            throw new MessageError(String.join("\n", errors));
        }
        List<String> msg = new ArrayList<>(errors);
        msg.addAll(warned);
        if (!msg.isEmpty()) {
            warnings.add(String.join("\n", msg));
        }
        return wsCopy;
    }

    /**
     * {@code resolveWorkspaces}: the members whose package.json a {@code packages} glob finds, in the order glob lists
     * them, outside any node_modules.
     */
    Map<String, WorkspaceLayout.Workspace> resolveWorkspaces(Manifest root, YarnClassicOptions config,
                                                             List<String> warnings) {
        Map<String, WorkspaceLayout.Workspace> workspaces = new LinkedHashMap<>();
        if (!config.isWorkspacesEnabled()) {
            return workspaces;
        }
        Set<String> files = new LinkedHashSet<>();
        for (Object pattern : workspacePatterns(root, config, warnings)) {
            files.addAll(glob((String) pattern));
        }
        for (String file : files) {
            String loc = Objects.requireNonNull(ProjectFiles.location(PosixPath.dirname(file)));
            Manifest manifest = tryManifest(loc, Objects.requireNonNull(inputs.getManifests().get(loc)),
                    config.isLooseSemver(), false, "", warnings);
            if (!Js.truthy(manifest.getName())) {
                warnings.add(Lang.format("Missing name in workspace at $0, ignoring.", loc));
                continue;
            }
            if (!Js.truthy(manifest.getVersion())) {
                warnings.add(Lang.format("Missing version in workspace at $0, ignoring.", loc));
                continue;
            }
            String name = Js.string(manifest.getName());
            if (workspaces.containsKey(name)) {
                throw new MessageError(Lang.format("There are more than one workspace with name $0", name));
            }
            workspaces.put(name, new WorkspaceLayout.Workspace(loc, manifest));
        }
        return workspaces;
    }

    /**
     * {@code resolveWorkspaces} up to its glob, which calls replace on every pattern before the first glob starts.
     */
    static List<?> workspacePatterns(Manifest root, YarnClassicOptions config, List<String> warnings) {
        Map<String, @Nullable Object> ws = getWorkspaces(root, true, config, warnings);
        Object patterns = ws != null && Js.truthy(ws.get("packages")) ? ws.get("packages") : new ArrayList<>();
        if (!(patterns instanceof List)) {
            throw new MessageError("The workspaces field in package.json must be an array.");
        }
        for (Object pattern : (List<?>) patterns) {
            if (!(pattern instanceof String)) {
                throw new JsError(JsError.TYPE_ERROR, pattern == null ?
                        "Cannot read properties of null (reading 'replace')" : "pattern.replace is not a function");
            }
        }
        return (List<?>) patterns;
    }

    /**
     * glob 7.1.2 of {@code <pattern>/+(package.json|yarn.json)} over the project's files, without those under the
     * pattern's node_modules, sorted as glob sorts and spelled with the pattern's leading {@code ./}.
     */
    private List<String> glob(String pattern) {
        String base = pattern.replaceFirst("/?\\z", "");
        String prefix = "";
        while (base.startsWith("./")) {
            base = base.substring(2);
            prefix += "./";
        }
        String include = base + "/+(package.json|yarn.json)";
        String ignore = base + "/node_modules/**/+(package.json|yarn.json)";
        Set<String> files = new LinkedHashSet<>();
        for (String loc : inputs.getManifests().keySet()) {
            if (!loc.isEmpty()) {
                files.add(loc + "/package.json");
            }
        }
        if (inputs.getPaths() != null) {
            for (String path : inputs.getPaths()) {
                if (path.endsWith("/package.json") || path.endsWith("/yarn.json")) {
                    files.add(path);
                }
            }
        }
        List<String> found = new ArrayList<>();
        for (String file : files) {
            if (Minimatch.minimatch(file, include) && !Minimatch.minimatch(file, ignore)) {
                String loc = file.substring(0, file.lastIndexOf('/'));
                if (file.endsWith("/yarn.json") || !inputs.getManifests().containsKey(loc)) {
                    throw new CannotKnow("the workspace member " + file + " (the host did not give its contents)");
                }
                found.add(file);
            }
        }
        found.sort(LocaleCompare::en);
        List<String> spelled = new ArrayList<>();
        for (String file : found) {
            spelled.add(prefix + file);
        }
        return spelled;
    }
}
