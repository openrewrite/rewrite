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
package org.openrewrite.javascript.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.javascript.internal.lock.LockDirectory;
import org.openrewrite.javascript.internal.lock.ManifestRules;
import org.openrewrite.javascript.internal.lock.NativeLockEngine;
import org.openrewrite.javascript.internal.lock.Port;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * Regenerates a JavaScript project's lock without running its package manager, through the port that
 * {@link NativeLockEngine} dispatches to; {@link #forPackageManager(PackageManager)} selects the instance.
 */
public final class LockFileRegeneration {

    public static final LockFileRegeneration NPM = new LockFileRegeneration(PackageManager.Npm);
    public static final LockFileRegeneration YARN_CLASSIC = new LockFileRegeneration(PackageManager.YarnClassic);
    public static final LockFileRegeneration YARN_BERRY = new LockFileRegeneration(PackageManager.YarnBerry);
    public static final LockFileRegeneration PNPM = new LockFileRegeneration(PackageManager.Pnpm);
    public static final LockFileRegeneration BUN = new LockFileRegeneration(PackageManager.Bun);
    public static final List<LockFileRegeneration> ALL = unmodifiableList(asList(NPM, YARN_CLASSIC, YARN_BERRY, PNPM,
            BUN));

    public static @Nullable LockFileRegeneration forPackageManager(@Nullable PackageManager pm) {
        for (LockFileRegeneration regeneration : ALL) {
            if (regeneration.pm == pm) {
                return regeneration;
            }
        }
        return null;
    }

    public enum Reason {
        REGISTRY_UNREACHABLE,
        AUTH_FAILED,
        PACKAGE_NOT_FOUND,
        VERSION_NOT_FOUND,
        CHECKSUM_UNAVAILABLE,
        RESOLUTION_REQUIRED,
        UNSUPPORTED_LOCKFILE_VERSION,
        UNSUPPORTED_ENTRY_TYPE,
        MALFORMED_LOCK,
        MALFORMED_MANIFEST,

        /**
         * A peer dependency the package manager cannot satisfy ({@code ERESOLVE}): either it rejects the edit, or
         * it would write a lock that its next install rejects. The detail names both sides.
         */
        PEER_CONFLICT
    }

    /**
     * Which of the three result rules a failure falls under.
     */
    public enum Kind {
        /**
         * The package manager reads something the engine is not given.
         */
        CANNOT_KNOW,

        /**
         * The package manager's own verdict on the edit or the lock: an error of its own, or a lock it would discard
         * and resolve from scratch, or delete.
         */
        PM_REJECTS,

        /**
         * The package manager fails on the edit with an error it does not raise on purpose.
         */
        PM_FAILS,

        /**
         * A second install over the lock written for the edit would reject or rewrite it.
         */
        SECOND_INSTALL
    }

    /**
     * The edited manifest, its lock, and what else of the project the package manager reads, by path, as the edit
     * leaves it.
     */
    @Value
    public static class Request {
        String packageJson;
        @Nullable String lock;
        @Nullable NodeResolutionResult marker;
        @Nullable Path packageJsonPath;
        Map<Path, String> manifests;
        Map<Path, String> packageManagerFiles;
        @Nullable Set<Path> projectPaths;

        // editedFiles: the files the edit changes or creates beside the manifest, which the other fields then hold
        @Builder
        private Request(String packageJson, @Nullable String lock, @Nullable NodeResolutionResult marker,
                        @Nullable Path packageJsonPath, @Singular("manifest") Map<Path, String> manifests,
                        @Singular("packageManagerFile") Map<Path, String> packageManagerFiles,
                        @Singular("editedFile") Map<Path, String> editedFiles, @Nullable Set<Path> projectPaths) {
            this.packageJson = packageJson;
            this.lock = lock;
            this.marker = marker;
            this.packageJsonPath = packageJsonPath;
            if (editedFiles.isEmpty()) {
                this.manifests = manifests;
                this.packageManagerFiles = packageManagerFiles;
                this.projectPaths = projectPaths;
                return;
            }
            Map<Path, String> editedManifests = new LinkedHashMap<>(manifests);
            Map<Path, String> files = new LinkedHashMap<>(packageManagerFiles);
            Set<Path> paths = projectPaths == null ? null : new LinkedHashSet<>(projectPaths);
            for (Map.Entry<Path, String> file : editedFiles.entrySet()) {
                if (file.getKey().equals(packageJsonPath)) {
                    continue;
                }
                if (editedManifests.containsKey(file.getKey())) {
                    editedManifests.put(file.getKey(), file.getValue());
                } else {
                    files.put(file.getKey(), file.getValue());
                }
                if (paths != null) {
                    paths.add(file.getKey());
                }
            }
            this.manifests = editedManifests;
            this.packageManagerFiles = files;
            this.projectPaths = paths;
        }
    }

    /**
     * Where the package manager run in the edited manifest's directory installs: {@code depth} directories above it.
     * When its walk stops before it picks a lock, the lock there, or the nearest, reports why.
     */
    @Value
    public static class InstallRoot {
        public static final InstallRoot HERE = new InstallRoot(0, null);
        public static final InstallRoot STOPS_HERE = new InstallRoot(0, Stop.WALK_FAILS);

        int depth;

        /**
         * Why the walk stops before it picks a lock; null when it does not.
         */
        @Nullable Stop stop;

        public static InstallRoot at(int depth) {
            return new InstallRoot(depth, null);
        }

        public static InstallRoot stops(int depth, Stop why) {
            return new InstallRoot(depth, why);
        }

        public boolean isStopped() {
            return stop != null;
        }

        public enum Stop {
            /**
             * The walk fails there, which the regeneration of that lock reports.
             */
            WALK_FAILS,

            /**
             * Where the package manager installs, it writes a file beside the lock that no regeneration writes.
             */
            WRITES_ANOTHER_FILE
        }
    }

    @Value
    @AllArgsConstructor
    public static class Failure {
        Kind kind;
        Reason reason;
        @Nullable String packageName;
        String detail;

        /**
         * The lock the install wrote, where the second install rejects or rewrites it; null otherwise.
         */
        @Nullable String written;

        public Failure(Kind kind, Reason reason, @Nullable String packageName, String detail) {
            this(kind, reason, packageName, detail, null);
        }
    }

    /**
     * The regenerated lock and the other files the install writes, or the failure that stands in their place.
     */
    @Value
    public static class Result {
        @Nullable String lockFileContent;
        @Nullable Failure failure;

        /**
         * The files the install writes besides the lock, by path from the lock's directory; none when it fails.
         */
        Map<String, String> files;

        public static Result success(String lockFileContent) {
            return new Result(lockFileContent, null, emptyMap());
        }

        public static Result success(String lockFileContent, Map<String, String> files) {
            return new Result(lockFileContent, null, unmodifiableMap(new LinkedHashMap<>(files)));
        }

        public static Result failure(Failure failure) {
            return new Result(null, failure, emptyMap());
        }

        public boolean isSuccess() {
            return failure == null;
        }

        public @Nullable String getErrorMessage() {
            if (failure == null) {
                return null;
            }
            StringBuilder message = new StringBuilder(failure.getReason().toString());
            if (failure.getPackageName() != null) {
                message.append(" [").append(failure.getPackageName()).append(']');
            }
            return message.append(": ").append(failure.getDetail()).toString();
        }
    }

    public static NodeLockRegenerationFailures.Row failureRow(Path packageJsonPath, Failure failure,
                                                              @Nullable String defaultPackageName) {
        return new NodeLockRegenerationFailures.Row(packageJsonPath.toString(),
                failure.getPackageName() != null ? failure.getPackageName() : defaultPackageName,
                failure.getKind().toString(), failure.getReason().toString(), failure.getDetail());
    }

    private final PackageManager pm;
    private final Port port;

    private LockFileRegeneration(PackageManager pm) {
        this.pm = pm;
        this.port = requireNonNull(NativeLockEngine.port(pm));
    }

    public String getLockFile() {
        return port.lockFile();
    }

    /**
     * Whether the package manager reads the contents of a project file of this name.
     */
    public boolean reads(String fileName) {
        return port.readsAsManifest(fileName) || port.reads(fileName);
    }

    /**
     * Whether it reads a project's manifest from a file of this name, as it reads a package.json.
     */
    public boolean readsAsManifest(String fileName) {
        return port.readsAsManifest(fileName);
    }

    /**
     * Whether it reads a file of this name only from its lock's directory.
     */
    public boolean readsOnlyBesideLock(String fileName) {
        return port.readsOnlyBesideLock(fileName);
    }

    /**
     * Whether its install also reads the locks of the workspace's other projects.
     */
    public boolean readsOtherLocks() {
        return port.readsOtherLocks();
    }

    /**
     * Whether its install reads and rewrites a file of this name beside its lock, which the regeneration writes too.
     */
    public boolean writesBesideLock(String fileName) {
        return port.writesBesideLock(fileName);
    }

    /**
     * @param ctx supplies the {@code HttpSender} and host-configured registries/credentials
     * @return the new lock content, or a structured failure
     */
    public Result regenerate(Request request, ExecutionContext ctx) {
        return NativeLockEngine.regenerate(port, request, ctx);
    }

    /**
     * What the package manager reads of the files the dependency recipes edit.
     */
    public ManifestRules manifestRules() {
        return port.manifestRules();
    }

    /**
     * What {@code pm} reads of the files the dependency recipes edit; no package manager's rules when null.
     */
    public static ManifestRules manifestRules(@Nullable PackageManager pm) {
        LockFileRegeneration regeneration = forPackageManager(pm);
        return regeneration == null ? ManifestRules.NONE : regeneration.manifestRules();
    }

    /**
     * The package manager's own answer to where it installs when run in the edited manifest's directory.
     */
    public InstallRoot installRoot(Request request) {
        return port.installRoot(LockDirectory.of(request));
    }
}
