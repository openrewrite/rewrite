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

import org.openrewrite.ExecutionContext;
import org.openrewrite.javascript.NodeExecutionContextView;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.berry.RegistrySource;
import org.openrewrite.javascript.internal.lock.berry.YarnBerryLockRegenerator;
import org.openrewrite.javascript.internal.lock.bun.BunLockRegenerator;
import org.openrewrite.javascript.internal.lock.bun.ManifestSource;
import org.openrewrite.javascript.internal.lock.npm.Host;
import org.openrewrite.javascript.internal.lock.npm.NpmLockRegenerator;
import org.openrewrite.javascript.internal.lock.npm.PackumentSource;
import org.openrewrite.javascript.internal.lock.npm.RegistryPackuments;
import org.openrewrite.javascript.internal.lock.pnpm.PnpmLockRegenerator;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator;
import org.openrewrite.javascript.internal.registry.Environment;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;
import org.openrewrite.javascript.internal.registry.RegistryDiscovery;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.time.Instant;

/**
 * Builds the project's package manager's inputs from the {@link Request} and runs the port of that package manager's
 * own install: {@link NpmLockRegenerator}, {@link BunLockRegenerator}, {@link YarnClassicLockRegenerator},
 * {@link YarnBerryLockRegenerator} or {@link PnpmLockRegenerator}. There is no shell-out (it would leak registry
 * credentials).
 */
public final class NativeLockEngine {

    private NativeLockEngine() {
    }

    public static Result regenerate(PackageManager pm, Request request, ExecutionContext ctx) {
        try {
            switch (pm) {
                case Npm:
                    return regenerateNpm(request, ctx);
                case Bun:
                    return regenerateBun(request, ctx);
                case YarnClassic:
                    return regenerateYarnClassic(request, ctx);
                case YarnBerry:
                    return regenerateYarnBerry(request, ctx);
                case Pnpm:
                    return regeneratePnpm(request, ctx);
                default:
                    return Result.failure(new Failure(Reason.RESOLUTION_REQUIRED, null,
                            "no native lock regeneration for " + pm));
            }
        } catch (NodeRegistryException nre) {
            return Result.failure(toFailure(nre));
        }
    }

    /**
     * How many directories above the edited manifest's the workspace root is whose lock the package manager writes
     * when run in the manifest's directory, found by the package manager's own walk, when regenerating that root's
     * lock reproduces it; else 0. Yarn Berry's project is the nearest directory with a yarn.lock, the manifest's own.
     */
    public static int installRoot(PackageManager pm, Request request) {
        if (pm == PackageManager.Pnpm) {
            return PnpmLockRegenerator.installRoot(PnpmInputs.from(request, Host.UNKNOWN, Instant.EPOCH,
                    PnpmInputs.NO_REGISTRY));
        }
        try {
            switch (pm) {
                case Npm:
                    return NpmLockRegenerator.loadLocalPrefix(NpmInputs.from(request, Host.UNKNOWN, Instant.EPOCH));
                case Bun:
                    return BunLockRegenerator.findWorkspaceRoot(BunInputs.from(request, Host.UNKNOWN, Instant.EPOCH));
                case YarnClassic:
                    return YarnClassicLockRegenerator.installRoot(YarnClassicInputs.from(request, Host.UNKNOWN,
                            Instant.EPOCH));
                default:
                    return 0;
            }
        } catch (RuntimeException e) {
            // these walk only from a manifest with its own lock, whose regeneration reports this
            return 0;
        }
    }

    private static Result regenerateNpm(Request request, ExecutionContext ctx) {
        NodeExecutionContextView view = NodeExecutionContextView.view(ctx);
        NodeRegistries registries = RegistryDiscovery.discover(ctx, request.getMarker(), Environment.SYSTEM);
        PackumentSource packuments = new RegistryPackuments(registries, view.getRegistryClient());
        return new NpmLockRegenerator(NpmInputs.from(request, host(view), Instant.now()), packuments).regenerate();
    }

    private static Result regenerateBun(Request request, ExecutionContext ctx) {
        NodeExecutionContextView view = NodeExecutionContextView.view(ctx);
        NodeRegistries registries = RegistryDiscovery.discover(ctx, request.getMarker(), Environment.SYSTEM);
        ManifestSource manifests = new BunRegistryManifests(registries, view.getRegistryClient());
        return new BunLockRegenerator(BunInputs.from(request, host(view), Instant.now()), manifests).regenerate();
    }

    private static Result regenerateYarnClassic(Request request, ExecutionContext ctx) {
        NodeExecutionContextView view = NodeExecutionContextView.view(ctx);
        NodeRegistries registries = RegistryDiscovery.discover(ctx, request.getMarker(), Environment.SYSTEM);
        YarnClassicRegistryPackuments packuments = new YarnClassicRegistryPackuments(registries,
                view.getRegistryClient());
        return new YarnClassicLockRegenerator(YarnClassicInputs.from(request, host(view), Instant.now()), packuments)
                .regenerate();
    }

    private static Result regenerateYarnBerry(Request request, ExecutionContext ctx) {
        NodeExecutionContextView view = NodeExecutionContextView.view(ctx);
        NodeRegistries registries = RegistryDiscovery.discover(ctx, request.getMarker(), Environment.SYSTEM);
        RegistrySource registry = new YarnBerryRegistrySource(registries, view.getRegistryClient());
        return new YarnBerryLockRegenerator(YarnBerryInputs.from(request, host(view), Instant.now()), registry)
                .regenerate();
    }

    private static Result regeneratePnpm(Request request, ExecutionContext ctx) {
        NodeExecutionContextView view = NodeExecutionContextView.view(ctx);
        NodeRegistries registries = RegistryDiscovery.discover(ctx, request.getMarker(), Environment.SYSTEM);
        PnpmRegistryMetadata registry = new PnpmRegistryMetadata(registries, view.getRegistryClient());
        return new PnpmLockRegenerator(PnpmInputs.from(request, host(view), Instant.now(), registry)).regenerate();
    }

    private static Host host(NodeExecutionContextView view) {
        return Host.builder()
                .nodeVersion(view.getNodeVersion())
                .os(view.getOs())
                .cpu(view.getCpu())
                .libc(view.getLibc())
                .build();
    }

    private static Failure toFailure(NodeRegistryException e) {
        Reason reason;
        switch (e.getReason()) {
            case AUTH_FAILED:
                reason = Reason.AUTH_FAILED;
                break;
            case PACKAGE_NOT_FOUND:
                reason = Reason.PACKAGE_NOT_FOUND;
                break;
            case VERSION_NOT_FOUND:
                reason = Reason.VERSION_NOT_FOUND;
                break;
            case MALFORMED_MANIFEST:
                reason = Reason.MALFORMED_MANIFEST;
                break;
            case UNREACHABLE:
            default:
                reason = Reason.REGISTRY_UNREACHABLE;
                break;
        }
        return new Failure(reason, e.getPackageName(), e.getMessage());
    }
}
