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
import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.berry.RegistrySource;
import org.openrewrite.javascript.internal.lock.berry.YarnBerryLockRegenerator;
import org.openrewrite.javascript.internal.lock.bun.BunLockRegenerator;
import org.openrewrite.javascript.internal.lock.bun.ManifestSource;
import org.openrewrite.javascript.internal.lock.npm.NpmLockRegenerator;
import org.openrewrite.javascript.internal.lock.npm.PackumentSource;
import org.openrewrite.javascript.internal.lock.pnpm.PnpmLockRegenerator;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator;
import org.openrewrite.javascript.internal.registry.Environment;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;
import org.openrewrite.javascript.internal.registry.RegistryDiscovery;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.time.Instant;

/**
 * Builds the package manager's inputs from the {@link Request} and runs the port of its own install.
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
                    return Result.failure(LockOnlyInstall.NPM.failure(new CannotKnow("the lock " + pm + " writes")));
            }
        } catch (NodeRegistryException nre) {
            return Result.failure(LockOnlyInstall.failure(nre));
        }
    }

    /**
     * Where the package manager installs when run in the edited manifest's directory, by its own walk: npm's
     * localPrefix, Bun's workspace root, Yarn classic's findWorkspaceRoot, Berry's project, pnpm's workspace dir.
     */
    public static InstallRoot installRoot(PackageManager pm, Request request) {
        LockDirectory dir = LockDirectory.of(request);
        switch (pm) {
            case Npm:
                return NpmInputs.installRoot(dir);
            case Bun:
                return BunInputs.installRoot(dir);
            case YarnClassic:
                return YarnClassicLockRegenerator.installRoot(YarnClassicInputs.from(dir, Host.UNKNOWN, Instant.EPOCH));
            case YarnBerry:
                return YarnBerryLockRegenerator.installRoot(YarnBerryInputs.holds(dir), dir.depth());
            case Pnpm:
                return PnpmLockRegenerator.installRoot(PnpmInputs.from(dir, Host.UNKNOWN, Instant.EPOCH));
            default:
                return InstallRoot.STOPS_HERE;
        }
    }

    /**
     * Whether the package manager reads the contents of a project file of this name.
     */
    public static boolean reads(PackageManager pm, String fileName) {
        switch (pm) {
            case Npm:
                return NpmInputs.readsOnlyBesideLock(fileName);
            case Bun:
                return BunInputs.reads(fileName);
            case YarnClassic:
                return YarnClassicInputs.reads(fileName);
            case YarnBerry:
                return YarnBerryInputs.reads(fileName);
            case Pnpm:
                return PnpmInputs.reads(fileName);
            default:
                return false;
        }
    }

    /**
     * Whether the package manager reads a file of this name only from its lock's directory.
     */
    public static boolean readsOnlyBesideLock(PackageManager pm, String fileName) {
        switch (pm) {
            case Npm:
                return NpmInputs.readsOnlyBesideLock(fileName);
            case Bun:
                return BunInputs.readsOnlyBesideLock(fileName);
            default:
                return false;
        }
    }

    /**
     * Whether the package manager reads a project's manifest from a file of this name, as it reads a package.json.
     */
    public static boolean readsAsManifest(PackageManager pm, String fileName) {
        return pm == PackageManager.Pnpm && PnpmLockRegenerator.OTHER_MANIFEST_NAMES.contains(fileName);
    }

    /**
     * Whether the package manager's install also reads the locks of the workspace's other projects: pnpm's of a
     * workspace whose projects keep their own, which it installs in the same run.
     */
    public static boolean readsOtherLocks(PackageManager pm) {
        return pm == PackageManager.Pnpm;
    }

    private static Result regenerateNpm(Request request, ExecutionContext ctx) {
        NodeExecutionContextView view = NodeExecutionContextView.view(ctx);
        NodeRegistries registries = RegistryDiscovery.discover(ctx, request.getMarker(), Environment.SYSTEM);
        PackumentSource packuments = new NpmRegistryPackuments(registries, view.getRegistryClient());
        return new NpmLockRegenerator(NpmInputs.from(LockDirectory.of(request), host(view), Instant.now()), packuments)
                .regenerate();
    }

    private static Result regenerateBun(Request request, ExecutionContext ctx) {
        NodeExecutionContextView view = NodeExecutionContextView.view(ctx);
        NodeRegistries registries = RegistryDiscovery.discover(ctx, request.getMarker(), Environment.SYSTEM);
        ManifestSource manifests = new BunRegistryManifests(registries, view.getRegistryClient());
        return new BunLockRegenerator(BunInputs.from(LockDirectory.of(request), host(view), Instant.now()), manifests)
                .regenerate();
    }

    private static Result regenerateYarnClassic(Request request, ExecutionContext ctx) {
        NodeExecutionContextView view = NodeExecutionContextView.view(ctx);
        NodeRegistries registries = RegistryDiscovery.discover(ctx, request.getMarker(), Environment.SYSTEM);
        YarnClassicRegistryPackuments packuments = new YarnClassicRegistryPackuments(registries,
                view.getRegistryClient());
        return new YarnClassicLockRegenerator(YarnClassicInputs.from(LockDirectory.of(request), host(view),
                Instant.now()), packuments)
                .regenerate();
    }

    private static Result regenerateYarnBerry(Request request, ExecutionContext ctx) {
        NodeExecutionContextView view = NodeExecutionContextView.view(ctx);
        NodeRegistries registries = RegistryDiscovery.discover(ctx, request.getMarker(), Environment.SYSTEM);
        RegistrySource registry = new YarnBerryRegistrySource(registries, view.getRegistryClient());
        return new YarnBerryLockRegenerator(YarnBerryInputs.from(LockDirectory.of(request), host(view),
                Instant.now()), registry)
                .regenerate();
    }

    private static Result regeneratePnpm(Request request, ExecutionContext ctx) {
        NodeExecutionContextView view = NodeExecutionContextView.view(ctx);
        NodeRegistries registries = RegistryDiscovery.discover(ctx, request.getMarker(), Environment.SYSTEM);
        PnpmRegistryMetadata registry = new PnpmRegistryMetadata(registries, view.getRegistryClient());
        return new PnpmLockRegenerator(PnpmInputs.from(LockDirectory.of(request), host(view), Instant.now()),
                registry).regenerate();
    }

    private static Host host(NodeExecutionContextView view) {
        return Host.builder()
                .nodeVersion(view.getNodeVersion())
                .os(view.getOs())
                .cpu(view.getCpu())
                .libc(view.getLibc())
                .build();
    }
}
