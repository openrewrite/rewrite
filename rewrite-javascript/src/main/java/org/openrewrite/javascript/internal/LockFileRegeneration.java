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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.javascript.internal.lock.NativeLockEngine;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;

import java.nio.file.Path;

/**
 * Regenerate a JavaScript project's lock file natively — without executing the package manager — by
 * dispatching to the shared {@link NativeLockEngine}. Pre-configured instances are provided for each
 * {@link PackageManager}; {@link #forPackageManager(PackageManager)} dispatches to the right one and
 * {@link #getLockFile()} names its lock file.
 */
public final class LockFileRegeneration {

    public static final LockFileRegeneration NPM = new LockFileRegeneration(PackageManager.Npm, "package-lock.json");
    public static final LockFileRegeneration YARN_CLASSIC = new LockFileRegeneration(PackageManager.YarnClassic, "yarn.lock");
    public static final LockFileRegeneration YARN_BERRY = new LockFileRegeneration(PackageManager.YarnBerry, "yarn.lock");
    public static final LockFileRegeneration PNPM = new LockFileRegeneration(PackageManager.Pnpm, "pnpm-lock.yaml");
    public static final LockFileRegeneration BUN = new LockFileRegeneration(PackageManager.Bun, "bun.lock");

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
        MALFORMED_MANIFEST
    }

    @Value
    public static class Failure {
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

        /**
         * Notes accompanying a successful regeneration, e.g. orphaned entries retained after a removal.
         */
        @Nullable String detail;

        public static Result success(String lockFileContent) {
            return new Result(true, lockFileContent, null, null, null);
        }

        public static Result success(String lockFileContent, @Nullable String detail) {
            return new Result(true, lockFileContent, null, null, detail);
        }

        public static Result failure(String errorMessage) {
            return new Result(false, null, errorMessage, null, null);
        }

        public static Result failure(Failure failure) {
            StringBuilder message = new StringBuilder(failure.getReason().toString());
            if (failure.getPackageName() != null) {
                message.append(" [").append(failure.getPackageName()).append(']');
            }
            message.append(": ").append(failure.getDetail());
            return new Result(false, null, message.toString(), failure, null);
        }
    }

    /**
     * Insert a data-table row describing a failed regeneration, mapping the structured {@link Failure}
     * when present and falling back to the plain error message (and the recipe's target package) otherwise.
     */
    public static void insertFailureRow(ExecutionContext ctx, NodeLockRegenerationFailures table,
                                        Path packageJsonPath, Result result, @Nullable String fallbackPackageName) {
        Failure failure = result.getFailure();
        table.insertRow(ctx, new NodeLockRegenerationFailures.Row(
                packageJsonPath.toString(),
                failure != null && failure.getPackageName() != null ? failure.getPackageName() : fallbackPackageName,
                failure != null ? failure.getReason().toString() : null,
                failure != null ? failure.getDetail() : String.valueOf(result.getErrorMessage())));
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
     * Regenerate the lock file natively via {@link NativeLockEngine}.
     *
     * @param packageJsonContent         the post-edit package.json content to lock
     * @param originalPackageJsonContent the pre-edit package.json, used to scope the edit to the
     *                                   dependencies the recipe actually changed; {@code null} whole-manifest reconcile
     * @param existingLockContent        the current lock content for the minimal update, or {@code null}
     * @param marker                     carries npmrc/registry config, engines, and package manager
     * @param packageJsonPath            the manifest path, for failure attribution and lock-path derivation
     * @param ctx                        supplies the {@code HttpSender} and host-configured registries/credentials
     * @return the new lock content, or a structured failure
     */
    public Result regenerate(String packageJsonContent,
                             @Nullable String originalPackageJsonContent,
                             @Nullable String existingLockContent,
                             @Nullable NodeResolutionResult marker,
                             @Nullable Path packageJsonPath,
                             ExecutionContext ctx) {
        return NativeLockEngine.regenerate(pm, packageJsonContent, originalPackageJsonContent,
                existingLockContent, marker, packageJsonPath, ctx);
    }
}
