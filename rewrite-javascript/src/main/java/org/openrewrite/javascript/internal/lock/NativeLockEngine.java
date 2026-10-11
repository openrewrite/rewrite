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
package org.openrewrite.javascript.internal.lock;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.javascript.NodeExecutionContextView;
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.time.Instant;

/**
 * Builds the host's side of a regeneration once and runs the port of the package manager's own install over the
 * request seen from its lock's directory.
 */
public final class NativeLockEngine {

    private NativeLockEngine() {
    }

    /**
     * The port of {@code pm}'s install; null for a package manager no port stands for.
     */
    public static @Nullable Port port(@Nullable PackageManager pm) {
        if (pm == null) {
            return null;
        }
        switch (pm) {
            case Npm:
                return NpmInputs.PORT;
            case Bun:
                return BunInputs.PORT;
            case YarnClassic:
                return YarnClassicInputs.PORT;
            case YarnBerry:
                return YarnBerryInputs.PORT;
            case Pnpm:
                return PnpmInputs.PORT;
            default:
                return null;
        }
    }

    public static Result regenerate(Port port, Request request, ExecutionContext ctx) {
        try {
            return port.regenerate(LockDirectory.of(request), HostInputs.of(ctx, request.getMarker(), Instant.now()),
                    NodeExecutionContextView.view(ctx).getRegistryClient());
        } catch (NodeRegistryException nre) {
            return Result.failure(LockOnlyInstall.failure(nre));
        }
    }
}
