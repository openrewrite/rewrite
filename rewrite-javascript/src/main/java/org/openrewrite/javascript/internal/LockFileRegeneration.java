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

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.javascript.internal.lock.NativeLockEngine;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * Regenerates a JavaScript project's lock without running its package manager, through the port that
 * {@link NativeLockEngine} dispatches to; {@link #forPackageManager(PackageManager)} selects the instance.
 */
public final class LockFileRegeneration {

    public static final LockFileRegeneration NPM = new LockFileRegeneration(PackageManager.Npm, "package-lock.json");
    public static final LockFileRegeneration YARN_CLASSIC = new LockFileRegeneration(PackageManager.YarnClassic, "yar" +
            "n.lock");
    public static final LockFileRegeneration YARN_BERRY = new LockFileRegeneration(PackageManager.YarnBerry, "yarn.lo" +
            "ck");
    public static final LockFileRegeneration PNPM = new LockFileRegeneration(PackageManager.Pnpm, "pnpm-lock.yaml");
    public static final LockFileRegeneration BUN = new LockFileRegeneration(PackageManager.Bun, "bun.lock");
    public static final List<LockFileRegeneration> ALL = unmodifiableList(asList(NPM, YARN_CLASSIC, YARN_BERRY, PNPM,
            BUN));

    public static @Nullable LockFileRegeneration forPackageManager(@Nullable PackageManager pm) {
        if (pm == null) {
            return null;
        }
        switch (pm) {
            case Npm:         return NPM;
            case YarnClassic: return YARN_CLASSIC;
            case YarnBerry:   return YARN_BERRY;
            case Pnpm:        return PNPM;
            case Bun:         return BUN;
            default:          return null;
        }
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
         * The package manager rejects the edit with an error of its own.
         */
        PM_REJECTS,

        /**
         * The package manager fails on the edit with an error it does not raise on purpose.
         */
        PM_FAILS,

        /**
         * The package manager would discard the lock and resolve everything from scratch.
         */
        DISCARDS_LOCK,

        /**
         * The package manager deletes the lock.
         */
        DELETES_LOCK,

        /**
         * A second install over the lock written for the edit would reject or rewrite it.
         */
        SECOND_INSTALL
    }

    /**
     * The edited manifest, its lock, and what else of the project the package manager reads, by path.
     */
    @Value
    @Builder
    public static class Request {
        String packageJson;
        @Nullable String lock;
        @Nullable NodeResolutionResult marker;
        @Nullable Path packageJsonPath;
        @Singular("manifest")
        Map<Path, String> manifests;
        @Singular("packageManagerFile")
        Map<Path, String> packageManagerFiles;
        @Nullable Set<Path> projectPaths;
    }

    /**
     * Where the package manager run in the edited manifest's directory installs: {@code depth} directories above it.
     * When its walk stops before it picks a lock, the lock there, or the nearest, reports why.
     */
    @Value
    public static class InstallRoot {
        public static final InstallRoot HERE = new InstallRoot(0, false);
        public static final InstallRoot STOPS_HERE = new InstallRoot(0, true);

        int depth;
        boolean stopped;
    }

    @Value
    public static class Failure {
        Kind kind;
        Reason reason;
        @Nullable String packageName;
        String detail;
    }

    @Value
    public static class Result {
        boolean success;
        @Nullable String lockFileContent;
        @Nullable String errorMessage;
        @Nullable Failure failure;

        public static Result success(String lockFileContent) {
            return new Result(true, lockFileContent, null, null);
        }

        public static Result failure(Failure failure) {
            StringBuilder message = new StringBuilder(failure.getReason().toString());
            if (failure.getPackageName() != null) {
                message.append(" [").append(failure.getPackageName()).append(']');
            }
            message.append(": ").append(failure.getDetail());
            return new Result(false, null, message.toString(), failure);
        }
    }

    /**
     * Insert a data-table row for a failed regeneration, naming the recipe's target package when the failure names
     * none.
     */
    public static void insertFailureRow(ExecutionContext ctx, NodeLockRegenerationFailures table,
                                        Path packageJsonPath, Result result, @Nullable String defaultPackageName) {
        Failure failure = requireNonNull(result.getFailure());
        table.insertRow(ctx, new NodeLockRegenerationFailures.Row(
                packageJsonPath.toString(),
                failure.getPackageName() != null ? failure.getPackageName() : defaultPackageName,
                failure.getReason().toString(),
                failure.getDetail()));
    }

    private final PackageManager pm;
    private final String lockFile;

    private LockFileRegeneration(PackageManager pm, String lockFile) {
        this.pm = pm;
        this.lockFile = lockFile;
    }

    public String getLockFile() {
        return lockFile;
    }

    /**
     * Whether the package manager reads the contents of a project file of this name.
     */
    public boolean reads(String fileName) {
        return NativeLockEngine.reads(pm, fileName);
    }

    /**
     * Whether it reads a file of this name only from its lock's directory.
     */
    public boolean readsOnlyBesideLock(String fileName) {
        return NativeLockEngine.readsOnlyBesideLock(pm, fileName);
    }

    /**
     * @param ctx supplies the {@code HttpSender} and host-configured registries/credentials
     * @return the new lock content, or a structured failure
     */
    public Result regenerate(Request request, ExecutionContext ctx) {
        return NativeLockEngine.regenerate(pm, request, ctx);
    }

    /**
     * The package manager's own answer to where it installs when run in the edited manifest's directory.
     */
    public InstallRoot installRoot(Request request) {
        return NativeLockEngine.installRoot(pm, request);
    }
}
