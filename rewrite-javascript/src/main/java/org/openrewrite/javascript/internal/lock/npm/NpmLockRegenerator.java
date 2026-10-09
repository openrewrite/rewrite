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
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.LocaleCompare;
import org.openrewrite.javascript.internal.lock.LockOnlyInstall;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.SharedError;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/arborist lib/arborist/index.js and reify.js (npm 11.17.0) for {@code npm install
 * --package-lock-only}: the ideal tree built over the lock, written back as the lock. The lock written is then
 * loaded and built again, as the next install would, and must come back unchanged: a second install that
 * rejects or rewrites it is reported instead.
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
         * The package.json of each directory above the project that npm walks through from where it runs, nearest
         * first, keyed by the project's location from that directory ({@code packages/a}).
         */
        @Singular
        Map<String, String> parentManifests;
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
            if (localPrefix > 0) {
                StringBuilder up = new StringBuilder("..");
                for (int i = localPrefix; i > 1; i--) {
                    up.append("/..");
                }
                throw new CannotKnow("the lock npm writes: it installs at the workspace root " + up +
                                     ", whose workspaces list this project, and writes that root's lock");
            }
            options = NpmOptions.fromNpmrc(inputs.getNpmrc());
            if (options.isGlobal()) {
                throw new NpmRejection("ESHRINKWRAPGLOBAL", "cannot generate lockfile for global packages", null);
            }
            VirtualTreeLoader.ProjectFiles files = projectFiles(truthy(inputs.getLock()) ? new LockFile(inputs.getLock()) : null);
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
     * How many directories above the project npm installs, writing that directory's lock; 0 when it installs here or
     * the walk fails, which {@link #regenerate()} reports, as it walks first.
     */
    public static int installRoot(Inputs inputs) {
        try {
            return loadLocalPrefix(inputs);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /**
     * @npmcli/config {@code loadLocalPrefix}: how many directories above the project npm installs, at the nearest
     * whose workspaces map the project, writing that directory's lock; 0 when it installs here.
     */
    private static int loadLocalPrefix(Inputs inputs) {
        String manifest = inputs.getManifests().get("");
        if (manifest == null) {
            return 0;
        }
        for (Map.Entry<String, String> parent : inputs.getParentManifests().entrySet()) {
            Map<String, Object> pkg;
            try {
                pkg = PackageJson.normalize(parent.getValue()).getContent();
            } catch (PackageJson.NeedsPackageFiles e) {
                throw e;
            } catch (RuntimeException e) {
                continue;
            }
            if (!truthy(pkg.get("workspaces"))) {
                continue;
            }
            VirtualTreeLoader.ProjectFiles project = VirtualTreeLoader.ProjectFiles.builder()
                    .manifest(parent.getKey(), manifest)
                    .build();
            if (MapWorkspaces.mapWorkspaces("/", pkg, project).containsValue("/" + parent.getKey())) {
                return parent.getKey().split("/").length;
            }
        }
        return 0;
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
     * The project's files, with the lock's own entries standing in for the root and for the link targets
     * outside node_modules that the host did not supply, as arborist reads a project without a package.json
     * from its lock.
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
                // a named root never reads it; a lock that is missing or malformed is refused before any name is written
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
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (e instanceof PlaceDep.CouldNotResolve) {
            PlaceDep.PeerConflict conflict = ((PlaceDep.CouldNotResolve) e).getConflict();
            return INSTALL.failure(Kind.PM_REJECTS, Reason.PEER_CONFLICT, conflict.getEdge().getName(), "ERESOLVE",
                    ExplainEresolve.explain(conflict.getExplanation(), 4));
        }
        if (e instanceof CannotKnow) {
            return INSTALL.failure((CannotKnow) e);
        }
        if (e instanceof NpmOptions.NeedsEnvironment) {
            return INSTALL.failure(new CannotKnow("the environment: " + message));
        }
        if (e instanceof PackageJson.NeedsPackageFiles) {
            return INSTALL.failure(new CannotKnow("the package's files: " + message));
        }
        if (e instanceof LocaleCompare.UnknownOrder) {
            return INSTALL.failure(new CannotKnow("the order npm writes the keys in: " + message));
        }
        if (e instanceof MalformedLock) {
            return INSTALL.failure(Rejected.discardsLock(message));
        }
        if (e instanceof MalformedManifest) {
            return INSTALL.fails(Reason.MALFORMED_MANIFEST, null, "it cannot apply the manifest: " + message);
        }
        if (e instanceof JsonParseEvenBetterErrors.JsonParseException) {
            return INSTALL.failure(Kind.PM_REJECTS, Reason.MALFORMED_MANIFEST, null, "EJSONPARSE", message);
        }
        String code = e instanceof NpmError ? ((NpmError) e).getCode() : null;
        if (code == null) {
            return INSTALL.fails(Reason.RESOLUTION_REQUIRED, packageOf(e), message);
        }
        return INSTALL.failure(Kind.PM_REJECTS, reasonFor(code), packageOf(e), code, message);
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
