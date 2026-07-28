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
package org.openrewrite.python.internal;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.python.internal.pdmlock.PdmLockEngine;
import org.openrewrite.python.internal.pipfilelock.PipenvLockEngine;
import org.openrewrite.python.internal.poetrylock.PoetryLockEngine;
import org.openrewrite.python.internal.uvlock.UvLockEngine;
import org.openrewrite.python.marker.PythonResolutionResult.PackageManager;
import org.openrewrite.python.table.PythonLockRegenerationFailures;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * Regenerates a lock file from an edited dependencies file. Both {@code uv}
 * ({@link #UV}) and {@code pipenv} ({@link #PIPENV}) projects regenerate their lock
 * natively, without executing the package manager: the existing lock is surgically
 * updated by consulting the project's package index over the network.
 */
public abstract class LockFileRegeneration {

    public static final LockFileRegeneration UV = new NativeUv();

    public static final LockFileRegeneration PIPENV = new NativePipenv();

    public static final LockFileRegeneration POETRY = new NativePoetry();

    public static final LockFileRegeneration PDM = new NativePdm();

    public static @Nullable LockFileRegeneration forPackageManager(@Nullable PackageManager pm) {
        if (pm == null) {
            return null;
        }
        switch (pm) {
            case Uv:
                return UV;
            case Pipenv:
                return PIPENV;
            case Poetry:
                return POETRY;
            case Pdm:
                return PDM;
            default:
                return null;
        }
    }

    public enum Reason {
        INDEX_UNREACHABLE,
        AUTH_FAILED,
        PACKAGE_NOT_FOUND,
        DYNAMIC_SDIST_METADATA,
        PIN_EXCLUDED_BY_PYTHON,
        UNSUPPORTED_ENTRY_TYPE,
        RESOLUTION_REQUIRED,
        RESOLUTION_CONFLICT,
        HASH_UNAVAILABLE,
        MALFORMED_MANIFEST,
        MALFORMED_LOCK
    }

    @Value
    public static class Failure {
        Reason reason;
        @Nullable String packageName;
        @Nullable String indexUrl;
        String detail;
    }

    @Value
    public static class Result {
        boolean success;
        @Nullable String lockFileContent;
        @Nullable String errorMessage;
        @Nullable Failure failure;

        /**
         * Notes accompanying a successful regeneration, e.g. orphaned transitive
         * entries retained after a removal.
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
     * Insert a data table row describing a failed regeneration, mapping the
     * structured {@link Failure} when present and falling back to the plain
     * error message (and the recipe's target package) otherwise.
     */
    public static void insertFailureRow(ExecutionContext ctx, PythonLockRegenerationFailures table,
                                        Path depsPath, Result result, @Nullable String fallbackPackageName) {
        Failure failure = result.getFailure();
        table.insertRow(ctx, new PythonLockRegenerationFailures.Row(
                depsPath.toString(),
                failure != null && failure.getPackageName() != null ? failure.getPackageName() : fallbackPackageName,
                failure != null ? failure.getReason().toString() : null,
                failure != null ? failure.getDetail() : String.valueOf(result.getErrorMessage())));
    }

    public final Result regenerate(String dependenciesContent, @Nullable String existingLockContent, ExecutionContext ctx) {
        return regenerate(dependenciesContent, null, existingLockContent, Collections.emptyMap(), ctx);
    }

    public final Result regenerate(String dependenciesContent, @Nullable String originalDependenciesContent,
                                   @Nullable String existingLockContent, ExecutionContext ctx) {
        return regenerate(dependenciesContent, originalDependenciesContent, existingLockContent,
                Collections.emptyMap(), ctx);
    }

    /**
     * Regenerate the lock file from the given dependencies content.
     * When an existing lock file is provided a minimal update is performed
     * rather than re-resolving every dependency from scratch.
     *
     * @param dependenciesContent         the dependencies-file content to lock
     * @param originalDependenciesContent the pre-edit dependencies-file content, or {@code null}.
     *                                    When provided, engines that support it reconcile only the
     *                                    packages the edit actually changed, leaving pre-existing
     *                                    drift in untouched packages as-is instead of aborting
     * @param existingLockContent         the current lock file content, or {@code null}
     * @param environment                 ignored; retained for API compatibility with the
     *                                    former shell-out implementation. Environment-style
     *                                    proxy/index configuration has no effect on the
     *                                    native engines; use {@code ctx} instead
     * @param ctx                         supplies the HTTP transport (proxy included) via
     *                                    {@link org.openrewrite.HttpSenderExecutionContextView}
     *                                    and host-configured package indexes/credentials via
     *                                    {@link org.openrewrite.python.PythonExecutionContextView}
     * @return a result containing the new lock file content, or a failure
     */
    public abstract Result regenerate(String dependenciesContent, @Nullable String originalDependenciesContent,
                                      @Nullable String existingLockContent, Map<String, String> environment,
                                      ExecutionContext ctx);

    private static final class NativePipenv extends LockFileRegeneration {
        @Override
        public Result regenerate(String dependenciesContent, @Nullable String originalDependenciesContent,
                                 @Nullable String existingLockContent, Map<String, String> environment,
                                 ExecutionContext ctx) {
            return PipenvLockEngine.regenerate(dependenciesContent, originalDependenciesContent,
                    existingLockContent, ctx);
        }
    }

    private static final class NativeUv extends LockFileRegeneration {
        @Override
        public Result regenerate(String dependenciesContent, @Nullable String originalDependenciesContent,
                                 @Nullable String existingLockContent, Map<String, String> environment,
                                 ExecutionContext ctx) {
            return UvLockEngine.regenerate(dependenciesContent, existingLockContent, ctx);
        }
    }

    private static final class NativePoetry extends LockFileRegeneration {
        @Override
        public Result regenerate(String dependenciesContent, @Nullable String originalDependenciesContent,
                                 @Nullable String existingLockContent, Map<String, String> environment,
                                 ExecutionContext ctx) {
            return PoetryLockEngine.regenerate(dependenciesContent, originalDependenciesContent,
                    existingLockContent, ctx);
        }
    }

    private static final class NativePdm extends LockFileRegeneration {
        @Override
        public Result regenerate(String dependenciesContent, @Nullable String originalDependenciesContent,
                                 @Nullable String existingLockContent, Map<String, String> environment,
                                 ExecutionContext ctx) {
            return PdmLockEngine.regenerate(dependenciesContent, originalDependenciesContent,
                    existingLockContent, ctx);
        }
    }
}
