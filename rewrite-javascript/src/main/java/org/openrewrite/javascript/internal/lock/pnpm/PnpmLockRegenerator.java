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

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.JsYaml;
import org.openrewrite.javascript.internal.lock.LockOnlyInstall;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.SharedError;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Port of pnpm 11.2.2's pnpm/src main.ts for {@code pnpm install --lockfile-only}: {@link Main}, the install
 * command, then @pnpm/installing.commands' {@link InstallDeps}.
 */
public final class PnpmLockRegenerator {

    private static final LockOnlyInstall INSTALL = LockOnlyInstall.PNPM;

    /**
     * What the engine is given for a pnpm project.
     */
    @Value
    @Builder(toBuilder = true)
    public static class Inputs {
        /**
         * The pnpm-lock.yaml; null when the project has none.
         */
        @Nullable
        String lock;

        /**
         * The lock's directory relative to the root: {@code ""}, or a workspace member's own lock's.
         */
        @Builder.Default
        String lockDir = "";

        /**
         * The directory pnpm runs in relative to the root, the edited manifest's; null for the lock's.
         */
        @Nullable
        String dir;

        /**
         * The project's manifests by location: {@code ""} the root, {@code packages/a} a workspace member.
         */
        @Singular
        Map<String, String> manifests;

        /**
         * The project's package-manager files by path relative to the root: pnpm-workspace.yaml, .npmrc, patch
         * files, a pnpmfile, the other projects' locks, and the package.json of a {@code file:} directory.
         */
        @Singular
        Map<String, String> files;

        /**
         * The process environment pnpm runs in; null when the host does not know it.
         */
        @Nullable
        Map<String, String> env;

        /**
         * The clock {@code minimumReleaseAge} counts from; null for the walk, which reads none.
         */
        @Nullable
        Instant now;

        @Builder.Default
        Host host = Host.UNKNOWN;

        /**
         * The user's .npmrc, the host's registries, which pnpm reads under the project's; null for none.
         */
        @Nullable
        String userNpmrc;

        /**
         * The files the project holds relative to its root, when the host says.
         */
        @Nullable
        Set<String> paths;

        /**
         * The files the host read directly in each directory above the root, by path from it ({@code
         * ../package.json}), and those it holds there, when it says: what corepack's walk from where pnpm runs reads.
         */
        @Builder.Default
        Map<String, String> filesAbove = Collections.emptyMap();

        @Nullable
        Set<String> pathsAbove;

        boolean holds(String path) {
            return files.containsKey(path) || paths != null && paths.contains(path);
        }

        // declared so that javadoc, which reads the source before Lombok, finds it
        public static class InputsBuilder {
        }
    }

    /**
     * workspace.project-manifest-reader's other manifest names, which it reads where there is no package.json.
     */
    public static final List<String> OTHER_MANIFEST_NAMES = Collections.unmodifiableList(Arrays.asList("package.json5",
            "package.yaml"));

    private final Inputs inputs;
    private final RegistryMetadata registry;
    private final EnvLockfile.OnDisk lockFile;
    private final EnvInstaller envInstaller;
    private final CreateNewStoreController stores;
    private final Instant now;
    private final Install lockInstall;

    public PnpmLockRegenerator(Inputs inputs, RegistryMetadata registry) {
        this.inputs = inputs;
        this.registry = registry;
        this.lockFile = new EnvLockfile.OnDisk(inputs.getLock());
        this.now = requireNonNull(inputs.getNow(), "now");
        this.envInstaller = new EnvInstaller(registry, now, inputs.getHost());
        this.stores = new CreateNewStoreController(registry, now);
        this.lockInstall = new Install(inputs, inputs.getLockDir(), lockFile, registry, stores, now);
    }

    /**
     * The lock pnpm writes for the edited manifests over the project's lock, or the failure, after a second install
     * over the written lock changed nothing.
     */
    public Result regenerate() {
        if (inputs.getLock() == null) {
            return Result.failure(INSTALL.failure(Rejected.discardsLock("the project has no " +
                                                                        LockfileReader.WANTED_LOCKFILE)));
        }
        String written;
        try {
            String dir = inputs.getDir();
            if (dir != null && !dir.equals(inputs.getLockDir())) {
                String into = Main.installsInto(inputs, dir);
                if (!into.equals(inputs.getLockDir())) {
                    throw new CannotKnow("which lock pnpm writes for this edit (run in " + dir + " it installs " +
                                         "into " + lockfilePath(into) + ", not " + lockfilePath(inputs.getLockDir()) +
                                         ")");
                }
            }
            written = writeLock();
        } catch (RuntimeException e) {
            return Result.failure(failure(e));
        }
        return INSTALL.verify(written, again ->
                new PnpmLockRegenerator(inputs.toBuilder().lock(again).build(), registry)
                .writeLock(), PnpmLockRegenerator::failure);
    }

    /**
     * Where pnpm installs the lock's project: {@link Main#installRoot}.
     */
    public static InstallRoot installRoot(Inputs inputs) {
        return Main.installRoot(inputs);
    }

    private static String lockfilePath(String dir) {
        return dir.isEmpty() ? LockfileReader.WANTED_LOCKFILE : dir + "/" + LockfileReader.WANTED_LOCKFILE;
    }

    /**
     * One {@code pnpm install --lockfile-only} over the lock: the lock it writes.
     */
    String writeLock() {
        Install.Result install = install();
        return install.getResolution() == null ? install.getWritten() : lockInstall.installInContext(install);
    }

    /**
     * main.ts up to the command, then the install command up to the resolution of the lock.
     */
    Install.Result install() {
        Main main = new Main(inputs, envInstaller, stores, lockFile);
        PnpmOptions options = main.config();
        boolean workspace = options.getWorkspacePackagePatterns() != null;
        String lockfileDir = workspace ? options.getLockfileDir(inputs.getLockDir()) : null;
        main.checkWantedPackageManager(options);
        Main.FilterResults filterResults;
        try {
            main.installConfigDepsAndLoadHooks(options, lockfileDir);
            filterResults = workspace ? main.filterProjectsFromDir(options) : null;
            // install.ts' handler, before installDeps
            if (options.isTrue("global")) {
                throw new PnpmError("GLOBAL_INSTALL_NOT_SUPPORTED", "\"pnpm install -g\" is not supported. " +
                                                                    "Use \"pnpm add -g <pkg>\" to install global " +
                                                                    "packages.");
            }
        } catch (RuntimeException e) {
            throw main.beforeNextStoreController(e);
        }
        return new InstallDeps(inputs, registry, stores, now, lockInstall).installDeps(options,
                filterResults == null ? null : filterResults.getAllProjects(),
                filterResults == null ? null : filterResults.getAllProjectsGraph());
    }

    private static Failure failure(RuntimeException e) {
        return INSTALL.failure(outcome(e));
    }

    // pnpm's own errors carry an ERR_PNPM_ code, though main.ts's printError logs none; an error without one crashes
    private static RuntimeException outcome(RuntimeException e) {
        if (e instanceof LockfileReader.IgnoredLockfile) {
            return Rejected.discardsLock(String.valueOf(e.getMessage()));
        }
        if (e instanceof ReportPeerDependencyIssues.PeerDependencyIssuesError) {
            return Rejected.rejects(Reason.PEER_CONFLICT,
                    ((ReportPeerDependencyIssues.PeerDependencyIssuesError) e).firstPeerName(),
                    "[ERR_PNPM_PEER_DEP_ISSUES] " + e.getMessage());
        }
        if (e instanceof PnpmError) {
            String code = ((PnpmError) e).getCode();
            return Rejected.rejects(reasonFor(code), "[" + code + "] " + e.getMessage());
        }
        if (e instanceof JsError) {
            String code = ((JsError) e).getCode();
            Reason reason = "ERR_INVALID_PACKAGE_CONFIG".equals(code) ? Reason.MALFORMED_MANIFEST :
                    Reason.RESOLUTION_REQUIRED;
            return Rejected.crashes(reason, null, (code == null ? "" : "[" + code + "] ") + Js.message(e));
        }
        // the lock and the env document catch their own; what reaches here is pnpm-workspace.yaml's
        if (e instanceof JsYaml.YamlException) {
            return Rejected.crashes(Reason.MALFORMED_MANIFEST, null, String.valueOf(e.getMessage()));
        }
        if (e instanceof LockfileFormatConverters.TypeError) {
            return Rejected.crashes(Reason.MALFORMED_LOCK, null, "TypeError: " + e.getMessage());
        }
        return e;
    }

    private static Reason reasonFor(String code) {
        switch (code) {
            case "ERR_PNPM_NO_MATCHING_VERSION":
                return SharedError.NO_MATCHING_VERSION.getReason();
            case "ERR_PNPM_FETCH_404":
                return SharedError.PACKAGE_NOT_FOUND.getReason();
            case "ERR_PNPM_UNSUPPORTED_ENGINE":
                return SharedError.UNSUPPORTED_ENGINE.getReason();
            case "ERR_PNPM_UNSUPPORTED_PLATFORM":
                return SharedError.UNSUPPORTED_PLATFORM.getReason();
            case "ERR_PNPM_NO_MATURE_MATCHING_VERSION":
            case "ERR_PNPM_NO_MATCHING_VERSION_INSIDE_WORKSPACE":
            case "ERR_PNPM_NODEJS_VERSION_NOT_FOUND":
                return Reason.VERSION_NOT_FOUND;
            case "ERR_PNPM_WORKSPACE_PKG_NOT_FOUND":
                return Reason.PACKAGE_NOT_FOUND;
            case "ERR_PNPM_JSON_PARSE":
            case "ERR_PNPM_JSON5_PARSE":
            case "ERR_PNPM_INVALID_PEER_DEPENDENCY_SPECIFICATION":
            case "ERR_PNPM_CATALOG_ENTRY_NOT_FOUND_FOR_SPEC":
                return Reason.MALFORMED_MANIFEST;
            case "ERR_PNPM_BROKEN_LOCKFILE":
            case "ERR_PNPM_ENV_LOCKFILE_CORRUPTED":
                return Reason.MALFORMED_LOCK;
            default:
                return Reason.RESOLUTION_REQUIRED;
        }
    }
}
