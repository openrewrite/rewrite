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
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Builds the package manager's inputs from the {@link Request} and runs the port of its own install: a table of the
 * five ports, each answering which files its package manager reads and where it installs.
 */
public final class NativeLockEngine {

    private static final Map<PackageManager, Port> PORTS = new EnumMap<>(PackageManager.class);

    static {
        PORTS.put(PackageManager.Npm, new Port(NpmInputs::reads, NpmInputs::readsOnlyBesideLock, name -> false, false,
                NpmInputs::installRoot, NativeLockEngine::regenerateNpm));
        PORTS.put(PackageManager.Bun, new Port(BunInputs::reads, BunInputs::readsOnlyBesideLock, name -> false, false,
                BunInputs::installRoot, NativeLockEngine::regenerateBun));
        PORTS.put(PackageManager.YarnClassic, new Port(YarnClassicInputs::reads, name -> false, name -> false, false,
                YarnClassicInputs::installRoot, NativeLockEngine::regenerateYarnClassic));
        PORTS.put(PackageManager.YarnBerry, new Port(YarnBerryInputs::reads, name -> false, name -> false, false,
                YarnBerryInputs::installRoot, NativeLockEngine::regenerateYarnBerry));
        PORTS.put(PackageManager.Pnpm, new Port(PnpmInputs::reads, name -> false, PnpmInputs::readsAsManifest,
                PnpmInputs.readsOtherLocks(), PnpmInputs::installRoot, NativeLockEngine::regeneratePnpm));
    }

    private NativeLockEngine() {
    }

    public static Result regenerate(PackageManager pm, Request request, ExecutionContext ctx) {
        Port port = PORTS.get(pm);
        if (port == null) {
            return Result.failure(LockOnlyInstall.cannotKnow(new CannotKnow("the lock " + pm + " writes")));
        }
        try {
            return port.regenerate.apply(request, ctx);
        } catch (NodeRegistryException nre) {
            return Result.failure(LockOnlyInstall.failure(nre));
        }
    }

    /**
     * Where the package manager installs when run in the edited manifest's directory, by its own walk: npm's
     * localPrefix, Bun's workspace root, Yarn classic's findWorkspaceRoot, Berry's project, pnpm's workspace dir.
     */
    public static InstallRoot installRoot(PackageManager pm, Request request) {
        Port port = PORTS.get(pm);
        return port == null ? InstallRoot.STOPS_HERE : port.installRoot.apply(LockDirectory.of(request));
    }

    /**
     * Whether the package manager reads the contents of a project file of this name.
     */
    public static boolean reads(PackageManager pm, String fileName) {
        Port port = PORTS.get(pm);
        return port != null && port.reads.test(fileName);
    }

    /**
     * Whether the package manager reads a file of this name only from its lock's directory.
     */
    public static boolean readsOnlyBesideLock(PackageManager pm, String fileName) {
        Port port = PORTS.get(pm);
        return port != null && port.readsOnlyBesideLock.test(fileName);
    }

    /**
     * Whether the package manager reads a project's manifest from a file of this name, as it reads a package.json.
     */
    public static boolean readsAsManifest(PackageManager pm, String fileName) {
        Port port = PORTS.get(pm);
        return port != null && port.readsAsManifest.test(fileName);
    }

    /**
     * Whether the package manager's install also reads the locks of the workspace's other projects.
     */
    public static boolean readsOtherLocks(PackageManager pm) {
        Port port = PORTS.get(pm);
        return port != null && port.readsOtherLocks;
    }

    // what the dispatch asks one package manager's port
    private static final class Port {
        final Predicate<String> reads;
        final Predicate<String> readsOnlyBesideLock;
        final Predicate<String> readsAsManifest;
        final boolean readsOtherLocks;
        final Function<LockDirectory, InstallRoot> installRoot;
        final BiFunction<Request, ExecutionContext, Result> regenerate;

        Port(Predicate<String> reads, Predicate<String> readsOnlyBesideLock, Predicate<String> readsAsManifest,
             boolean readsOtherLocks, Function<LockDirectory, InstallRoot> installRoot,
             BiFunction<Request, ExecutionContext, Result> regenerate) {
            this.reads = reads;
            this.readsOnlyBesideLock = readsOnlyBesideLock;
            this.readsAsManifest = readsAsManifest;
            this.readsOtherLocks = readsOtherLocks;
            this.installRoot = installRoot;
            this.regenerate = regenerate;
        }
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
