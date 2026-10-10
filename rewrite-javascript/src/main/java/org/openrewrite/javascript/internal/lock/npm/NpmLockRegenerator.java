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
package org.openrewrite.javascript.internal.lock.npm;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.openrewrite.javascript.internal.lock.LocaleCompare;
import org.openrewrite.javascript.internal.lock.LockOnlyInstall;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.SharedError;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/arborist lib/arborist/index.js and reify.js (npm 11.17.0) for {@code npm install
 * --package-lock-only}; the lock written must come back unchanged from a second install, or that install is reported.
 */
public final class NpmLockRegenerator {

    /**
     * What the engine is given for an npm project.
     */
    @Value
    @Builder
    public static class Inputs {
        /**
         * The package-lock.json; null when the project has none.
         */
        @Nullable
        String lock;

        @Nullable
        String yarnLock;

        /**
         * The project's manifests as edited, by location: {@code ""} the root, {@code packages/a} a workspace
         * member. A member the host did not supply keeps its lock entry.
         */
        @Singular
        Map<String, String> manifests;

        /**
         * The project's own .npmrc; the host's user and global npmrc are not an input.
         */
        @Nullable
        String npmrc;

        /**
         * The files the project holds relative to the lock's directory, when the host says; an .npmrc among them the
         * host did not read is unknown.
         */
        @Nullable
        Set<String> paths;

        /**
         * The environment npm runs in; null when the host does not know it.
         */
        @Nullable
        Map<String, String> env;

        @Builder.Default
        Host host = Host.UNKNOWN;

        /**
         * The clock {@code min-release-age} counts from.
         */
        @NonNull
        Instant now;

        /**
         * The name of the directory the project is in, when the host knows it: a root manifest without a name
         * is named after it.
         */
        @Nullable
        String directoryName;

        /**
         * The directory npm runs in, the edited manifest's, relative to the lock's.
         */
        @Builder.Default
        String runsIn = "";

        /**
         * The package.json files under each directory above the one npm runs in that holds one, nearest first, keyed
         * by where npm runs as seen from there ({@code packages/a}); each by location, its own at "", null unread.
         */
        @Singular
        Map<String, Map<String, @Nullable String>> parentTrees;
    }

    private static final LockOnlyInstall INSTALL = LockOnlyInstall.NPM;

    private final Inputs inputs;
    private final PackumentSource registry;

    public NpmLockRegenerator(Inputs inputs, PackumentSource registry) {
        this.inputs = inputs;
        this.registry = registry;
    }

    public Result regenerate() {
        NpmOptions options;
        String path;
        String written;
        try {
            int localPrefix = loadLocalPrefix(inputs);
            int lockDir = inputs.getRunsIn().isEmpty() ? 0 : inputs.getRunsIn().split("/").length;
            if (localPrefix > lockDir) {
                StringBuilder up = new StringBuilder("..");
                for (int i = localPrefix - lockDir; i > 1; i--) {
                    up.append("/..");
                }
                throw new CannotKnow("the lock npm writes: it installs at the workspace root " + up +
                                     ", whose workspaces list this project, and writes that root's lock");
            }
            if (localPrefix < lockDir) {
                throw new CannotKnow("the lock npm writes: run in " + inputs.getRunsIn() + " it installs " +
                                     localPrefix + " directories above it, not at this lock");
            }
            if (inputs.getPaths() != null && inputs.getPaths().contains("npm-shrinkwrap.json")) {
                throw new CannotKnow("the lock npm writes: it reads and writes the npm-shrinkwrap.json beside this " +
                                     "lock in its place");
            }
            if (inputs.getNpmrc() == null && inputs.getPaths() != null && inputs.getPaths().contains(".npmrc")) {
                throw new CannotKnow(".npmrc (its contents were not read)");
            }
            if (inputs.getYarnLock() == null && inputs.getPaths() != null && inputs.getPaths().contains("yarn.lock")) {
                throw new CannotKnow("yarn.lock, which npm reads and rewrites beside its lock (its contents were " +
                                     "not read)");
            }
            options = NpmOptions.fromNpmrc(inputs.getNpmrc(), HostEnvironment.of(inputs.getEnv()));
            if (options.isGlobal()) {
                throw new NpmRejection("ESHRINKWRAPGLOBAL", "cannot generate lockfile for global packages", null);
            }
            VirtualTreeLoader.ProjectFiles files = projectFiles(truthy(inputs.getLock()) ?
                new LockFile(inputs.getLock()) : null);
            path = projectPath(files);
            IdealTreeBuilder arb = arborist(path, files, options);
            arb.buildIdealTree();
            if (!options.isSave() || options.isDryRun()) {
                return Result.success(Objects.requireNonNull(inputs.getLock()));
            }
            written = saveIdealTree(arb, options);
        } catch (NodeRegistryException e) {
            throw e;
        } catch (RuntimeException e) {
            return Result.failure(failure(e));
        }
        return INSTALL.verify(written, lock -> {
            IdealTreeBuilder again = arborist(path, projectFiles(new LockFile(lock)), options);
            again.buildIdealTree();
            return saveIdealTree(again, options);
        }, NpmLockRegenerator::failure);
    }

    /**
     * Where npm run in the project's directory installs and writes its lock; a walk that fails, which
     * {@link #regenerate()} reports as it walks first, stops here.
     */
    public static InstallRoot installRoot(Inputs inputs) {
        try {
            return new InstallRoot(loadLocalPrefix(inputs), false);
        } catch (RuntimeException e) {
            return InstallRoot.STOPS_HERE;
        }
    }

    /**
     * @npmcli/config {@code loadLocalPrefix}: how many directories above the one it runs in npm installs, at the
     * nearest whose workspaces map it, writing that directory's lock; 0 when it installs there.
     */
    private static int loadLocalPrefix(Inputs inputs) {
        String manifest = inputs.getManifests().get(inputs.getRunsIn());
        if (manifest == null) {
            return 0;
        }
        for (Map.Entry<String, Map<String, @Nullable String>> parent : inputs.getParentTrees().entrySet()) {
            VirtualTreeLoader.ProjectFiles files = parentFiles(parent.getKey(), manifest, parent.getValue());
            Map<String, Object> pkg;
            try {
                PackageJson own = files.manifest("");
                if (own == null) {
                    continue;
                }
                pkg = own.getContent();
            } catch (PackageJson.NeedsPackageFiles e) {
                pkg = e.getContent();
            } catch (CannotKnow e) {
                throw e;
            } catch (RuntimeException e) {
                continue;
            }
            if (!truthy(pkg.get("workspaces"))) {
                continue;
            }
            if (MapWorkspaces.mapWorkspaces("/", pkg, files).containsValue("/" + parent.getKey())) {
                return parent.getKey().split("/").length;
            }
        }
        return 0;
    }

    private static VirtualTreeLoader.ProjectFiles parentFiles(String runsIn, String manifest,
                                                              Map<String, @Nullable String> tree) {
        VirtualTreeLoader.ProjectFiles.ProjectFilesBuilder files = VirtualTreeLoader.ProjectFiles.builder()
                .manifest(runsIn, manifest);
        for (Map.Entry<String, @Nullable String> held : tree.entrySet()) {
            if (held.getKey().equals(runsIn)) {
                continue;
            }
            if (held.getValue() == null) {
                files.unread(held.getKey());
            } else {
                files.manifest(held.getKey(), held.getValue());
            }
        }
        return files.build();
    }

    /**
     * index.js {@code #setWorkspaces}: the members the node's {@code workspaces} patterns name among the
     * project's directories become its workspace edges.
     */
    static Node setWorkspaces(Node node, VirtualTreeLoader.ProjectFiles files) {
        Map<String, String> workspaces = MapWorkspaces.mapWorkspaces(node.getPath(), node.getPackage(), files);
        if (!workspaces.isEmpty()) {
            node.setWorkspaces(workspaces);
        }
        return node;
    }

    private VirtualTreeLoader.ProjectFiles projectFiles(@Nullable LockFile lock) {
        return projectFiles(lock, inputs.getYarnLock(), inputs.getManifests());
    }

    /**
     * The project's files, the lock's own entries standing in for the root and for link targets outside node_modules
     * the host did not supply, as arborist reads a project without a package.json.
     */
    @SuppressWarnings("unchecked")
    static VirtualTreeLoader.ProjectFiles projectFiles(@Nullable LockFile lock, @Nullable String yarnLock,
                                                       Map<String, String> manifests) {
        VirtualTreeLoader.ProjectFiles.ProjectFilesBuilder files = VirtualTreeLoader.ProjectFiles.builder()
                .lock(lock)
                .yarnLock(yarnLock)
                .manifests(manifests);
        Object data = lockData(lock);
        Object packages = data instanceof Map ? Js.get(data, "packages") : null;
        if (packages instanceof Map) {
            for (Map.Entry<String, @Nullable Object> entry : Js.entries(packages)) {
                Object meta = entry.getValue();
                if (!(meta instanceof Map)) {
                    continue;
                }
                Object resolved = Js.get(meta, "resolved");
                if (entry.getKey().isEmpty()) {
                    if (!manifests.containsKey("")) {
                        files.parsedManifest("", (Map<String, Object>) meta);
                    }
                } else if (Js.strictEquals(Js.get(meta, "link"), true) && resolved instanceof String &&
                           !((String) resolved).contains("node_modules/") && !manifests.containsKey(resolved)) {
                    Object target = Js.get(packages, (String) resolved);
                    if (target instanceof Map) {
                        files.parsedManifest((String) resolved, (Map<String, Object>) target);
                    }
                }
            }
        }
        return files.build();
    }

    private IdealTreeBuilder arborist(String path, VirtualTreeLoader.ProjectFiles files, NpmOptions options) {
        return new IdealTreeBuilder(path, files, options, registry, inputs.getHost(), inputs.getNow());
    }

    // reify.js #saveIdealTree: the lock takes the indent of the root package.json, two spaces when it has none
    private static String saveIdealTree(IdealTreeBuilder arb, NpmOptions options) {
        PackageJson rootManifest = arb.getRootManifest();
        Object format = rootManifest == null || rootManifest.getIndent() == null ? "  " : rootManifest.getIndent();
        boolean formatPackageLock = options.isFormatPackageLock();
        return Objects.requireNonNull(arb.buildIdealTree().getMeta())
                .toString(formatPackageLock && truthy(format) ? format : formatPackageLock);
    }

    /**
     * The root takes the directory's name when its manifest has none (node.js: nameFromFolder). Without the
     * host's word, the lock's own top-level name stands in: npm wrote it from that directory.
     */
    private String projectPath(VirtualTreeLoader.ProjectFiles files) {
        String directoryName = inputs.getDirectoryName();
        if (directoryName == null) {
            Object data = lockData(files.getLock());
            Object lockName = data instanceof Map ? Js.get(data, "name") : null;
            if (lockName instanceof String && !((String) lockName).isEmpty()) {
                directoryName = (String) lockName;
            } else if (data instanceof Map && !truthy(rootName(files))) {
                throw new CannotKnow("the project directory name (package.json has no name; npm names the lock " +
                                     "after the directory it runs in)");
            } else {
                // a named root never reads it; a lock that is missing or malformed is refused before any name is
                // written
                directoryName = "project";
            }
        }
        return "/" + directoryName;
    }

    // a lock that does not parse is the builder's to refuse
    private static @Nullable Object lockData(@Nullable LockFile lock) {
        if (lock == null) {
            return null;
        }
        try {
            return lock.parsed().getValue();
        } catch (RuntimeException e) {
            return null;
        }
    }

    // a manifest that does not parse is the builder's to reject
    private static Object rootName(VirtualTreeLoader.ProjectFiles files) {
        try {
            PackageJson manifest = files.manifest("");
            return manifest == null ? Js.UNDEFINED : Js.get(manifest.getContent(), "name");
        } catch (RuntimeException e) {
            return true;
        }
    }

    private static Failure failure(RuntimeException e) {
        return INSTALL.failure(outcome(e));
    }

    private static RuntimeException outcome(RuntimeException e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (e instanceof PlaceDep.CouldNotResolve) {
            PlaceDep.PeerConflict conflict = ((PlaceDep.CouldNotResolve) e).getConflict();
            return Rejected.coded(Reason.PEER_CONFLICT, conflict.getEdge().getName(), "ERESOLVE",
                    ExplainEresolve.explain(conflict.getExplanation(), 4));
        }
        if (e instanceof CannotKnow) {
            return e;
        }
        if (e instanceof PackageJson.NeedsPackageFiles) {
            return new CannotKnow("the package's files: " + message);
        }
        if (e instanceof LocaleCompare.UnknownOrder) {
            return new CannotKnow("the order npm writes the keys in: " + message);
        }
        if (e instanceof MalformedLock) {
            return Rejected.discardsLock(message);
        }
        if (e instanceof MalformedManifest) {
            return Rejected.crashes(Reason.MALFORMED_MANIFEST, null, "it cannot apply the manifest: " + message);
        }
        if (e instanceof JsonParseEvenBetterErrors.JsonParseException) {
            return Rejected.coded(Reason.MALFORMED_MANIFEST, null, "EJSONPARSE", message);
        }
        String code = e instanceof NpmError ? ((NpmError) e).getCode() :
                e instanceof Spec.InvalidSpecException ? ((Spec.InvalidSpecException) e).getCode() : null;
        if (code == null) {
            // the port raises JavaScript's errors as these two; any other is a fault of the port, not npm's
            if (!(e instanceof IllegalArgumentException) && !(e instanceof IllegalStateException)) {
                throw e;
            }
            return Rejected.crashes(Reason.RESOLUTION_REQUIRED, packageOf(e), message);
        }
        return Rejected.coded(reasonFor(code), packageOf(e), code, message);
    }

    private static Reason reasonFor(String code) {
        switch (code) {
            case "E404":
                return SharedError.PACKAGE_NOT_FOUND.getReason();
            case "ETARGET":
                return SharedError.NO_MATCHING_VERSION.getReason();
            case "EBADPLATFORM":
                return SharedError.UNSUPPORTED_PLATFORM.getReason();
            case "EBADENGINE":
                return SharedError.UNSUPPORTED_ENGINE.getReason();
            case "ENOVERSIONS":
            case "E403":
                return Reason.VERSION_NOT_FOUND;
            case "EUNSUPPORTEDPROTOCOL":
            case "EALLOWGIT":
            case "EALLOWFILE":
            case "EALLOWDIRECTORY":
            case "EALLOWREMOTE":
                return Reason.UNSUPPORTED_ENTRY_TYPE;
            case "EOVERRIDE":
            case "EJSONPARSE":
            case "EDUPLICATEWORKSPACE":
            case "EINVALIDTAGNAME":
            case "EINCOMPLETEMANIFEST":
                return Reason.MALFORMED_MANIFEST;
            default:
                return Reason.RESOLUTION_REQUIRED;
        }
    }

    private static @Nullable String packageOf(RuntimeException e) {
        if (e instanceof NpmRejection) {
            return ((NpmRejection) e).getPkg();
        }
        if (e instanceof PickManifest.PickFailure) {
            Object name = ((PickManifest.PickFailure) e).getName();
            return name == null ? null : Js.string(name);
        }
        return null;
    }
}
