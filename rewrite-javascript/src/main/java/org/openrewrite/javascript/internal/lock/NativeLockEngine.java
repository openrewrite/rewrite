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

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.javascript.NodeExecutionContextView;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.LockFileRegeneration;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.bun.BunLockRegenerator;
import org.openrewrite.javascript.internal.lock.bun.ManifestSource;
import org.openrewrite.javascript.internal.lock.npm.Host;
import org.openrewrite.javascript.internal.lock.npm.NpmLockRegenerator;
import org.openrewrite.javascript.internal.lock.npm.PackumentSource;
import org.openrewrite.javascript.internal.lock.npm.RegistryPackuments;
import org.openrewrite.javascript.internal.registry.AbbreviatedPackument;
import org.openrewrite.javascript.internal.registry.Environment;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;
import org.openrewrite.javascript.internal.registry.RegistryDiscovery;
import org.openrewrite.javascript.internal.registry.VersionManifest;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.semver.Semver;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.*;
import static org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit.Kind.*;
import static org.openrewrite.semver.Semver.Ecosystem.NODE;

/**
 * Native lock regeneration, by package manager. npm goes through {@link NpmLockRegenerator}: the ported
 * arborist tree, loaded from the lock, rebuilt for the edited manifests and written whole. Bun goes through
 * {@link BunLockRegenerator}, the port of Bun's own install over its lockfile model. pnpm and Yarn
 * take the per-dependency flow below until each follows its own resolver: diff the pre-edit and post-edit
 * {@code package.json}, prove each moving dependency leaves the resolved closure unchanged, resolve the target
 * version against the registry, and hand a proven {@link LockEditSet} to the format's {@link LockPatcher};
 * when that proof cannot be made, the whole closure is resolved instead, seeded by the existing lock, and
 * diffed into the same edits.
 * <p>
 * Whatever a route cannot express byte-exactly fails loud with a structured {@link Failure} and no lock; there
 * is no shell-out (it would leak registry credentials).
 */
public final class NativeLockEngine {

    // lenient: trailing commas and // comments are read too
    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .build();

    /** Constraint protocols the native engine cannot re-pin without repo/filesystem access. */
    private static final List<String> UNSUPPORTED_PROTOCOLS = Arrays.asList(
            "git:", "git+", "github:", "file:", "link:", "portal:", "workspace:", "http://", "https://");

    /** Manifest dependency scopes an importer entry can declare a dependency under. */
    private static final List<String> DECLARED_SCOPES = Arrays.asList(
            "dependencies", "devDependencies", "peerDependencies", "optionalDependencies");

    /**
     * The cannot-reshape deferrals whole-closure resolution may still reproduce byte-exact. Genuine
     * input/environment failures (malformed lock/manifest, registry/auth/not-found) are deliberately absent —
     * resolving more would not fix those, so they stay fail-loud.
     */
    private static final Set<Reason> RESOLVABLE_REASONS =
            EnumSet.of(Reason.RESOLUTION_REQUIRED, Reason.CHECKSUM_UNAVAILABLE);

    private NativeLockEngine() {
    }

    public static Result regenerate(PackageManager pm,
                                    String editedPackageJson,
                                    @Nullable String originalPackageJson,
                                    @Nullable String existingLock,
                                    @Nullable NodeResolutionResult marker,
                                    @Nullable Path packageJsonPath,
                                    ExecutionContext ctx) {
        return regenerate(pm, Request.builder()
                .packageJson(editedPackageJson)
                .originalPackageJson(originalPackageJson)
                .lock(existingLock)
                .marker(marker)
                .packageJsonPath(packageJsonPath)
                .build(), ctx);
    }

    public static Result regenerate(PackageManager pm, Request request, ExecutionContext ctx) {
        try {
            if (pm == PackageManager.Npm) {
                return regenerateNpm(request, ctx);
            }
            if (pm == PackageManager.Bun) {
                return regenerateBun(request, ctx);
            }
            return doRegenerate(pm, request.getPackageJson(), request.getOriginalPackageJson(), request.getLock(),
                    request.getMarker(), request.getPackageJsonPath(), ctx);
        } catch (EngineFailure ef) {
            return Result.failure(ef.failure);
        } catch (NodeRegistryException nre) {
            return Result.failure(toFailure(nre));
        } catch (RecipeRunException rre) {
            // A patcher that fails loud from inside a rewrite-json/yaml visitor has its EngineFailure wrapped;
            // unwrap it so the deferral stays a graceful Failure rather than crashing the recipe run.
            for (Throwable cause = rre.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof EngineFailure) {
                    return Result.failure(((EngineFailure) cause).failure);
                }
            }
            throw rre;
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

    private static Host host(NodeExecutionContextView view) {
        return Host.builder()
                .nodeVersion(view.getNodeVersion())
                .os(view.getOs())
                .cpu(view.getCpu())
                .libc(view.getLibc())
                .build();
    }

    private static Result doRegenerate(PackageManager pm,
                                       String editedPackageJson,
                                       @Nullable String originalPackageJson,
                                       @Nullable String existingLock,
                                       @Nullable NodeResolutionResult marker,
                                       @Nullable Path packageJsonPath,
                                       ExecutionContext ctx) {
        if (existingLock == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "no existing lock file to update");
        }

        // Built once and shared by the per-dependency and whole-closure scopes.
        NodeRegistries registries = RegistryDiscovery.discover(ctx, marker, Environment.SYSTEM);
        NpmRegistryClient client = NodeExecutionContextView.view(ctx).getRegistryClient();

        try {
            return patchEditedDependencies(pm, editedPackageJson, originalPackageJson, existingLock,
                    packageJsonPath, registries, client);
        } catch (EngineFailure perDependency) {
            return resolveAndPatch(pm, perDependency, editedPackageJson, existingLock, packageJsonPath, registries, client);
        } catch (RecipeRunException rre) {
            // A patcher failing loud from inside a rewrite-json/yaml visitor wraps its EngineFailure; unwrap it
            // so a cannot-reshape deferral still routes to whole-closure resolution.
            EngineFailure perDependency = engineFailureCause(rre);
            if (perDependency == null) {
                throw rre;
            }
            return resolveAndPatch(pm, perDependency, editedPackageJson, existingLock, packageJsonPath, registries, client);
        }
    }

    /** The per-dependency scope: diff the declared deps, prove the closure is unchanged, and patch in place. */
    private static Result patchEditedDependencies(PackageManager pm, String editedPackageJson,
                                             @Nullable String originalPackageJson, String existingLock,
                                             @Nullable Path packageJsonPath, NodeRegistries registries,
                                             NpmRegistryClient client) {
        if (originalPackageJson == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "cannot scope the edit without the pre-edit package.json");
        }

        // pnpm lockfile-version gate before touching the network.
        if (pm == PackageManager.Pnpm) {
            requireSupportedPnpmVersion(existingLock);
        }

        List<DepChange> changes = diffDeclaredDeps(originalPackageJson, editedPackageJson);
        if (changes.isEmpty()) {
            // The manifest changed but no declared dependency did; the edit is an override/resolution
            // or some other field the native engine cannot prove leaves the closure unchanged.
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "change is outside declared dependencies (e.g. overrides/resolutions) and requires resolution");
        }

        Path lockPath = lockPath(pm, packageJsonPath);
        String memberImporterDir = addImporterDir(pm, existingLock, packageJsonPath);
        List<LockEditSet.PackageEdit> edits = new ArrayList<>();
        for (DepChange change : changes) {
            edits.addAll(resolveEdit(pm, change, existingLock, memberImporterDir, registries, client));
        }
        if (pm == PackageManager.YarnBerry) {
            edits = enrichBerryChecksums(edits, existingLock, registries, client);
        }

        LockEditSet editSet = new LockEditSet(existingLock, lockPath, pm, editedPackageJson, edits);

        LockPatcher patcher = patcherFor(pm);
        if (patcher == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "no native patcher for " + pm + " yet");
        }
        return Result.success(patcher.patch(editSet));
    }

    /**
     * The whole-closure scope: when the per-dependency proof defers because it cannot reshape the closure,
     * resolve the full closure seeded by the existing lock, diff it against the lock, and hand the same kind of
     * edits to the same patcher. Byte-exact-or-fail-loud either way, so this never yields a lock a real install
     * would disagree with; when the deeper attempt also defers, its failure propagates (its detail is closer to
     * the real blocker). A genuine input/environment deferral rethrows the per-dependency failure unchanged.
     */
    private static Result resolveAndPatch(PackageManager pm, EngineFailure perDependency, String editedPackageJson,
                                           String existingLock, @Nullable Path packageJsonPath,
                                           NodeRegistries registries, NpmRegistryClient client) {
        if (!RESOLVABLE_REASONS.contains(perDependency.failure.getReason())) {
            throw perDependency;
        }
        // A single edited manifest resolves as a single importer. For a workspace that would drop the sibling
        // importers and write a truncated lock, so keep those deferred (return the per-dependency failure).
        if (isMultiPackageProject(pm, editedPackageJson, existingLock, packageJsonPath)) {
            throw perDependency;
        }
        try {
            if (pm == PackageManager.Pnpm) {
                return resolveAndPatchPnpm(editedPackageJson, existingLock, packageJsonPath, registries, client);
            }
            if (pm == PackageManager.YarnBerry) {
                return resolveAndPatchYarnBerry(editedPackageJson, existingLock, packageJsonPath, registries, client);
            }
            return resolveAndPatchYarnClassic(editedPackageJson, existingLock, packageJsonPath, registries, client);
        } catch (NodeRegistryException nre) {
            // The deeper closure walk hit a registry/environment error the per-dependency path did not need to
            // reach; it produced no better answer, so return that deferral unchanged (no wrong lock either way).
            throw perDependency;
        }
        // An EngineFailure from the deeper attempt propagates: its reason/detail is preferred.
    }

    /**
     * Resolve the closure seeded by the existing berry lock, diff it, and patch only the difference. Untouched
     * entries keep their recorded checksums; only fresh or moved entries reproduce theirs from the tarball.
     */
    private static Result resolveAndPatchYarnBerry(String editedPackageJson, String existingLock,
                                                   @Nullable Path packageJsonPath, NodeRegistries registries,
                                                   NpmRegistryClient client) {
        Registry registry = new RegistryAdapter(registries, client);
        ResolutionGraph graph = new GraphBuilder(registry, lockedVersionsBerry(existingLock))
                .build(singletonMap("", editedPackageJson));
        List<LockEditSet.PackageEdit> edits = YarnBerryLockDiff.diff(graph, existingLock);
        for (LockEditSet.PackageEdit edit : edits) {
            if (edit.getNewResolved() != null) {
                edits = enrichBerryChecksums(edits, existingLock, registries, client);
                break;
            }
        }
        LockEditSet editSet = new LockEditSet(existingLock, lockPath(PackageManager.YarnBerry, packageJsonPath),
                PackageManager.YarnBerry, editedPackageJson, edits);
        return Result.success(new YarnBerryLockPatcher().patch(editSet));
    }

    /** The versions the berry lock already resolves, by name (each entry's {@code name@npm:version} resolution). */
    private static Map<String, Set<String>> lockedVersionsBerry(String lock) {
        Map<String, Set<String>> locked = new LinkedHashMap<>();
        Object loaded = new Yaml().load(lock);
        if (loaded instanceof Map) {
            for (Object value : ((Map<?, ?>) loaded).values()) {
                if (value instanceof Map) {
                    Object resolution = ((Map<?, ?>) value).get("resolution");
                    String res = resolution == null ? null : String.valueOf(resolution);
                    int npm = res == null ? -1 : res.lastIndexOf("@npm:");
                    if (npm > 0) {
                        locked.computeIfAbsent(res.substring(0, npm), k -> new LinkedHashSet<>())
                                .add(res.substring(npm + "@npm:".length()));
                    }
                }
            }
        }
        return locked;
    }

    /** Resolve the closure seeded by the existing yarn.lock, diff against its flat blocks, and patch only the difference. */
    private static Result resolveAndPatchYarnClassic(String editedPackageJson, String existingLock,
                                                     @Nullable Path packageJsonPath, NodeRegistries registries,
                                                     NpmRegistryClient client) {
        Registry registry = new RegistryAdapter(registries, client);
        ResolutionGraph graph = new GraphBuilder(registry, lockedVersionsYarnClassic(existingLock))
                .build(singletonMap("", editedPackageJson));
        List<LockEditSet.PackageEdit> edits = YarnClassicLockDiff.diff(graph, existingLock);
        LockEditSet editSet = new LockEditSet(existingLock, lockPath(PackageManager.YarnClassic, packageJsonPath),
                PackageManager.YarnClassic, editedPackageJson, edits);
        return Result.success(new YarnClassicLockPatcher().patch(editSet));
    }

    /** The versions the lock already resolves, scanned from every {@code name@range:} header's version line. */
    private static Map<String, Set<String>> lockedVersionsYarnClassic(String lock) {
        Map<String, Set<String>> locked = new LinkedHashMap<>();
        String name = null;
        for (String line : lock.split("\n")) {
            if (!line.isEmpty() && !Character.isWhitespace(line.charAt(0)) && line.charAt(0) != '#' &&
                    line.trim().endsWith(":")) {
                String selector = unquote(line.trim().substring(0, line.trim().length() - 1).split(",")[0].trim());
                int at = selector.lastIndexOf('@');
                name = at > 0 ? selector.substring(0, at) : null;
            } else if (name != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("version ")) {
                    locked.computeIfAbsent(name, k -> new LinkedHashSet<>())
                            .add(unquote(trimmed.substring("version ".length()).trim()));
                    name = null;
                }
            }
        }
        return locked;
    }

    /** Resolve the closure seeded by the existing pnpm lock, diff it, and patch only the difference. */
    private static Result resolveAndPatchPnpm(String editedPackageJson, String existingLock,
                                              @Nullable Path packageJsonPath, NodeRegistries registries,
                                              NpmRegistryClient client) {
        Registry registry = new RegistryAdapter(registries, client);
        ResolutionGraph graph = new GraphBuilder(registry, lockedVersionsPnpm(existingLock))
                .build(singletonMap("", editedPackageJson));
        List<LockEditSet.PackageEdit> edits = PnpmLockDiff.diff(graph, existingLock);
        LockEditSet editSet = new LockEditSet(existingLock, lockPath(PackageManager.Pnpm, packageJsonPath),
                PackageManager.Pnpm, editedPackageJson, edits);
        return Result.success(new PnpmLockPatcher().patch(editSet));
    }

    /** The versions the pnpm lock already resolves, by name (bare {@code packages} keys). */
    private static Map<String, Set<String>> lockedVersionsPnpm(String lock) {
        Map<String, Set<String>> locked = new LinkedHashMap<>();
        Object loaded = new Yaml().load(lock);
        Object packages = loaded instanceof Map ? ((Map<?, ?>) loaded).get("packages") : null;
        if (packages instanceof Map) {
            for (Object key : ((Map<?, ?>) packages).keySet()) {
                String k = String.valueOf(key);
                int at = k.lastIndexOf('@');
                if (at > 0) {
                    locked.computeIfAbsent(k.substring(0, at), x -> new LinkedHashSet<>()).add(k.substring(at + 1));
                }
            }
        }
        return locked;
    }

    /**
     * Whether the project has more than the single root importer whole-closure resolution reproduces. A workspace root manifest
     * declares {@code workspaces}; a member edit carries a multi-importer root lock (pnpm {@code importers},
     * berry {@code @workspace:} headers). yarn.lock is flat and cannot be told apart, so
     * a manifest in a sub-directory is treated as a member conservatively.
     */
    private static boolean isMultiPackageProject(PackageManager pm, String editedManifest, String lock,
                                                 @Nullable Path packageJsonPath) {
        try {
            if (declaresWorkspaces(editedManifest) || multiImporterLock(pm, lock)) {
                return true;
            }
        } catch (RuntimeException ignored) {
            // Unparseable here: let the resolution's own gates decide rather than misclassify.
        }
        if (pm == PackageManager.YarnClassic && packageJsonPath != null) {
            Path parent = packageJsonPath.getParent();
            String dir = parent == null ? "" : parent.toString().replace('\\', '/');
            return !(dir.isEmpty() || ".".equals(dir));
        }
        return false;
    }

    private static boolean declaresWorkspaces(String manifestJson) {
        Object ws = parseJsonObject(manifestJson, true).get("workspaces");
        return (ws instanceof List && !((List<?>) ws).isEmpty()) ||
                (ws instanceof Map && !((Map<?, ?>) ws).isEmpty());
    }

    private static boolean multiImporterLock(PackageManager pm, String lock) {
        switch (pm) {
            case Pnpm:
                return pnpmImporterKeys(lock).size() > 1;
            case YarnBerry:
                return berryWorkspaceHeaderCount(lock) > 1;
            default:
                return false; // yarn classic: flat lock, no importer sections to count
        }
    }

    private static int berryWorkspaceHeaderCount(String lock) {
        int count = 0;
        for (String line : lock.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.contains("@workspace:") && trimmed.endsWith("\":")) {
                count++;
            }
        }
        return count;
    }

    private static @Nullable EngineFailure engineFailureCause(Throwable t) {
        for (Throwable cause = t.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof EngineFailure) {
                return (EngineFailure) cause;
            }
        }
        return null;
    }

    private static List<LockEditSet.PackageEdit> resolveEdit(PackageManager pm, DepChange change, String existingLock,
                                                             @Nullable String memberImporterDir,
                                                             NodeRegistries registries, NpmRegistryClient client) {
        String name = change.name;

        if ((change.oldConstraint != null && change.oldConstraint.startsWith("npm:")) ||
                (change.newConstraint != null && change.newConstraint.startsWith("npm:"))) {
            // An npm:<name>@<range> alias resolves its real package under the declared slot; the per-dependency
            // path does not model that, so the whole-closure path takes over.
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " is an npm: alias and requires resolution");
        }

        if (change.oldConstraint == null) {
            if ("optionalDependencies".equals(change.scope) || "peerDependencies".equals(change.scope)) {
                // A fresh optional or peer declaration marks flags and scopes across the tree; only
                // whole-closure resolution reproduces that.
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        "adding " + name + " to " + change.scope + " requires resolution");
            }
            // The added direct dep plus its resolved runtime closure is hoisted against the existing tree;
            // any placement that would move/nest/fork fails loud.
            return resolveClosureAdd(pm, change, existingLock, memberImporterDir, registries, client);
        }

        Set<String> lockedVersions = findLockedVersions(pm, existingLock, name);
        String importerDir = findImporterDir(pm, existingLock, name, change.oldConstraint);

        if (change.newConstraint == null) {
            // Removal: the patcher drops the entry and its orphans.
            String oldVersion = lockedVersions.isEmpty() ? "" : lockedVersions.iterator().next();
            return singletonList(LockEditSet.PackageEdit.builder()
                    .name(name)
                    .oldVersion(oldVersion)
                    .newVersion(null)
                    .scope(change.scope)
                    .oldConstraint(change.oldConstraint)
                    .importerDir(importerDir)
                    .build());
        }

        if (isUnsupportedProtocol(change.newConstraint)) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, name,
                    name + " uses an unsupported entry type: " + change.newConstraint);
        }

        if (pm == PackageManager.Pnpm && lockedVersions.size() > 1) {
            // A direct dep unambiguous at its importer defers today only because the SAME package is forked
            // (nested/transitive) elsewhere at another version. Re-scope the bump to the version the importer
            // edge actually resolves; the other versions are forks this direct bump does not touch. A genuine
            // fork at the importer (the edge itself resolves to more than one version) stays fail-loud below.
            Set<String> importerVersions = importerResolvedVersionsPnpm(existingLock, name, change.oldConstraint);
            if (importerVersions.size() == 1) {
                lockedVersions = importerVersions;
            }
        }

        if (lockedVersions.isEmpty()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, "no locked entry for " + name);
        }
        if (lockedVersions.size() > 1) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " is present at multiple versions (fork/peer-duplicated): " + lockedVersions);
        }
        String oldVersion = lockedVersions.iterator().next();

        // A member-declared bump must own its registry entry alone: if a sibling workspace importer declares the
        // same dependency at the same range, they share one lock entry, so re-heading it would break the sibling.
        if (pm == PackageManager.YarnBerry &&
                importersDeclaring(existingLock, name, change.oldConstraint) > 1) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " is declared by multiple workspace importers; a per-member bump would fork (deferred)");
        }

        String targetVersion = resolveTarget(client, registries, name, oldVersion, change.newConstraint);

        if (pm == PackageManager.Pnpm && !targetVersion.equals(oldVersion)) {
            // The pnpm patcher renames the old packages/snapshots key in place; that stays byte-exact only when
            // no other version of this package sorts between old and new (else a real pnpm reorders the entries).
            requirePnpmRenameKeepsOrder(name, oldVersion, targetVersion, existingLock);
        }

        LockEditSet.PackageEdit.PackageEditBuilder edit = LockEditSet.PackageEdit.builder()
                .name(name)
                .oldVersion(oldVersion)
                .newVersion(targetVersion)
                .scope(change.scope)
                .oldConstraint(change.oldConstraint)
                .importerDir(importerDir);

        if (targetVersion.equals(oldVersion)) {
            // Constraint-only widening: the resolved version does not move, so the package entry keeps
            // its resolved/integrity and only the importer's declared constraint is re-pinned.
            return singletonList(edit.build());
        }

        // A reverse-dependent whose recorded constraint excludes the new version keeps the old version nested
        // under itself; the safe single-leaf slice resolves it, everything else defers.
        List<LockEditSet.PackageEdit> nestEdits;
        if (pm == PackageManager.Pnpm) {
            // pnpm never nests; a reverse-dependent that keeps the old version becomes a content-fork that
            // replaces the whole bump (add the new content, retain the old), so it returns early when it fires.
            List<LockEditSet.PackageEdit> fork =
                    planContentForkPnpm(name, oldVersion, targetVersion, change, existingLock, registries, client);
            if (fork != null) {
                return fork;
            }
            nestEdits = emptyList();
        } else {
            proveReverseDependentsAccept(pm, name, oldVersion, targetVersion, existingLock);
            nestEdits = emptyList();
        }

        NodeRegistry registry = registries.registryFor(name);
        VersionManifest oldManifest = client.getManifest(registry, name, oldVersion);
        VersionManifest newManifest = client.getManifest(registry, name, targetVersion);

        // Every closure surface but `dependencies` must be unchanged; a `dependencies` delta no longer fails
        // loud outright, it seeds the greedy-forward cascade below.
        boolean depsEqual = dependenciesEqual(oldManifest, newManifest);
        proveNonDependencySurfacesUnchanged(name, oldManifest, newManifest);
        LockEditSet.EntryMetadata surfaces = metadataDelta(oldManifest, newManifest);

        VersionManifest.Dist dist = newManifest.getDist();
        // A dropped `dependencies` edge (present in the old manifest, gone in the new) orphan-prunes rather than
        // fails loud: the patcher removes the edge and GCs whatever it leaves unreachable (pnpm v9, both yarns).
        boolean prunesOrphans = dropsDependencyEdge(oldManifest, newManifest);
        LockEditSet.PackageEdit rootEdit = edit
                .newResolved(dist == null ? null : dist.getTarball())
                .newIntegrity(dist == null ? null : dist.getIntegrity())
                .newShasum(dist == null ? null : dist.getShasum())
                .newDependencies(newManifest.getDependencies())
                .newOptionalDependencies(newManifest.getOptionalDependencies())
                .metadata(surfaces)
                .prunesOrphans(prunesOrphans)
                .build();

        List<LockEditSet.PackageEdit> edits = new ArrayList<>();
        edits.add(rootEdit);
        edits.addAll(nestEdits);
        if (!depsEqual) {
            if (pm == PackageManager.Pnpm) {
                edits.addAll(cascadeForcedMovesPnpm(name, oldVersion, newManifest, existingLock, registries, client));
            } else if (pm == PackageManager.YarnBerry || pm == PackageManager.YarnClassic) {
                edits.addAll(cascadeForcedMovesYarn(pm, name, oldManifest, newManifest, existingLock, registries, client));
            } else {
                // The remaining patchers cannot reshape a changed closure so far; other formats defer.
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + " dependencies changed");
            }
        }
        return edits;
    }

    /** The highest published version satisfying every constraint, or {@code null} if none does. */
    private static @Nullable String maxSatisfyingAll(Set<String> published, Set<String> constraints) {
        String best = null;
        for (String candidate : published) {
            boolean ok = true;
            for (String c : constraints) {
                if (!Semver.satisfies(candidate, c, NODE)) {
                    ok = false;
                    break;
                }
            }
            if (ok && (best == null || Semver.compare(candidate, best, NODE) > 0)) {
                best = candidate;
            }
        }
        return best;
    }

    /**
     * A direct-dependency bump whose new {@code dependencies} force a locked transitive to move. pnpm records resolved versions in
     * {@code snapshots.<root>@<v>.dependencies}, not constraints, so the transitive's current version is read
     * from the bumped root's own snapshot and the reverse-edge safety is a single-requirer check: a shared
     * transitive, whose other requirers' ranges are not in the lock, fails loud rather than risk a fork.
     */
    private static List<LockEditSet.PackageEdit> cascadeForcedMovesPnpm(String rootName, String rootOldVersion,
                                                                        VersionManifest rootNew, String existingLock,
                                                                        NodeRegistries registries, NpmRegistryClient client) {
        Map<String, String> newDeps = rootNew.getDependencies() == null ?
                emptyMap() : rootNew.getDependencies();

        // A dropped edge is handled by the patcher's snapshot prune + orphan GC (flagged via prunesOrphans);
        // this loop only re-resolves kept edges.
        Map<String, String> rootSnapshotDeps = snapshotDependenciesPnpm(existingLock, rootName + "@" + rootOldVersion);
        List<LockEditSet.PackageEdit> moves = new ArrayList<>();
        for (Map.Entry<String, String> e : newDeps.entrySet()) {
            String dep = e.getKey();
            String constraint = e.getValue();
            String cur = rootSnapshotDeps.get(dep);
            if (cur == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                        "upgrading " + rootName + " introduces new transitive " + dep +
                                " (add-during-bump) not yet supported");
            }
            if (cur.indexOf('(') >= 0) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                        dep + " is resolved with a peer suffix (" + cur + "); resolution required");
            }
            if (isUnsupportedProtocol(constraint) || !Semver.validate(constraint, null, NODE).isValid()) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                        dep + " is constrained by an unresolvable range: " + constraint);
            }
            if (!Semver.satisfies(cur, constraint, NODE)) {
                moves.add(resolveForcedMovePnpm(rootName, rootOldVersion, dep, cur, constraint,
                        existingLock, registries, client));
            }
        }
        return moves;
    }

    private static LockEditSet.PackageEdit resolveForcedMovePnpm(String rootName, String rootOldVersion, String dep,
                                                                 String oldVersion, String newConstraint,
                                                                 String existingLock, NodeRegistries registries,
                                                                 NpmRegistryClient client) {
        if (importerDeclaresPnpm(existingLock, dep)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                    dep + " is directly declared; moving it via cascade is not yet supported");
        }
        // pnpm records resolved versions, not ranges, so a shared transitive's other requirers cannot be proven
        // to accept the new version from the lock; only a transitive private to the bumped root moves.
        Set<String> otherReferrers = referrersPnpm(existingLock, dep, oldVersion, rootName + "@" + rootOldVersion);
        if (!otherReferrers.isEmpty()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                    dep + "@" + oldVersion + " is shared by " + otherReferrers +
                            "; the union of its requirers is not recorded in a pnpm lock, so the move defers");
        }

        NodeRegistry registry = registries.registryFor(dep);
        Set<String> published = client.getPackument(registry, dep).getVersions();
        String target = maxSatisfyingAll(published, singleton(newConstraint));
        if (target == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                    "no single version of " + dep + " satisfies " + newConstraint + " (deferred)");
        }
        if (target.equals(oldVersion)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                    dep + " resolves back to its locked version under the new constraint; deferred");
        }

        VersionManifest oldManifest = client.getManifest(registry, dep, oldVersion);
        VersionManifest newManifest = client.getManifest(registry, dep, target);
        // A mover must itself be a clean-closure bump; a change to its own edges is a deeper wave.
        proveNonDependencySurfacesUnchanged(dep, oldManifest, newManifest);
        if (notEmpty(newManifest.getPeerDependencies())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                    dep + " declares peerDependencies (pnpm suffix keys) not yet supported for cascade moves");
        }
        if (!dependenciesEqual(oldManifest, newManifest)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                    "moving " + dep + " to " + target + " changes its own dependencies " +
                            "(multi-level cascade) not yet supported");
        }

        VersionManifest.Dist dist = newManifest.getDist();
        if (dist == null || dist.getIntegrity() == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, dep, dep + "@" + target + " has no registry integrity");
        }
        return LockEditSet.PackageEdit.builder()
                .name(dep)
                .oldVersion(oldVersion)
                .newVersion(target)
                .newIntegrity(dist.getIntegrity())
                .newDependencies(newManifest.getDependencies())
                .metadata(metadataDelta(oldManifest, newManifest))
                .scope("dependencies")
                .importerDir(null)
                .kind(FORCED_MOVE)
                .build();
    }

    /**
     * A direct-dependency bump a reverse-dependent excludes. pnpm is content-addressed and never nests, so it
     * content-forks: the old version stays for the reverse-dependent and the new version is added as fresh
     * {@code packages}+{@code snapshots} content, with only the importer edge retargeted. The reverse-dependent's
     * range is not in the lock, so its manifest is fetched to prove it excludes the target (else pnpm would dedupe
     * it up, not fork). The safe slice is a single referrer keeping a leaf; anything else fails loud.
     *
     * @return the one content-fork edit when it applies, or {@code null} when the moved dep is not kept by any
     * reverse-dependent (a plain rename bump proceeds instead).
     */
    private static @Nullable List<LockEditSet.PackageEdit> planContentForkPnpm(String name, String oldVersion,
                                                                               String targetVersion, DepChange change,
                                                                               String lock, NodeRegistries registries,
                                                                               NpmRegistryClient client) {
        Set<String> referrers = referrersPnpm(lock, name, oldVersion, null);
        if (referrers.isEmpty()) {
            return null; // not retained by any reverse-dependent: a normal rename bump, not a fork
        }
        if (referrers.size() > 1) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + "@" + oldVersion + " is shared by " +
                    referrers + "; forking more than one reverse-dependent is not yet supported");
        }
        String referrerKey = referrers.iterator().next();
        int at = referrerKey.lastIndexOf('@');
        if (at <= 0) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, "unparseable referrer key " + referrerKey);
        }
        String refName = referrerKey.substring(0, at);
        String refVersion = referrerKey.substring(at + 1);

        // The referrer must genuinely EXCLUDE the new version, else pnpm dedupes it up rather than forking.
        // Its range is not in the lock (resolved versions only), so read it from the referrer's own manifest.
        VersionManifest refManifest = client.getManifest(registries.registryFor(refName), refName, refVersion);
        Map<String, String> refDeps = refManifest.getDependencies();
        String refConstraint = refDeps == null ? null : refDeps.get(name);
        if (refConstraint == null || !Semver.validate(refConstraint, null, NODE).isValid() ||
                Semver.satisfies(targetVersion, refConstraint, NODE)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, refName + " accepts " + name + "@" +
                    targetVersion + " (constraint " + refConstraint + "); pnpm would dedupe, not fork; deferred");
        }

        // The added version's content must be a clean leaf (no deps/peers/unsupported metadata to place).
        VersionManifest newManifest = client.getManifest(registries.registryFor(name), name, targetVersion);
        requireEmittablePnpmClosureMember(name, newManifest);
        if (notEmpty(newManifest.getDependencies())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + "@" + targetVersion +
                    " has its own dependencies; a pnpm fork of a non-leaf is not yet supported");
        }
        VersionManifest.Dist dist = newManifest.getDist();
        if (dist == null || dist.getIntegrity() == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, name, name + "@" + targetVersion + " has no registry integrity");
        }

        return singletonList(LockEditSet.PackageEdit.builder()
                .name(name)
                .oldVersion(oldVersion)
                .newVersion(targetVersion)
                .newIntegrity(dist.getIntegrity())
                .metadata(pnpmLeafMetadata(newManifest))
                .scope(change.scope)
                .importerDir(findImporterDir(PackageManager.Pnpm, lock, name, change.oldConstraint))
                .kind(CONTENT_FORK)
                .build());
    }

    /** The resolved versions the given pnpm snapshot depends on (its {@code dependencies} map values). */
    private static Map<String, String> snapshotDependenciesPnpm(String lock, String snapshotKey) {
        Map<String, String> out = new LinkedHashMap<>();
        Object loaded = new Yaml().load(lock);
        if (!(loaded instanceof Map)) {
            return out;
        }
        Object snapshots = ((Map<?, ?>) loaded).get("snapshots");
        if (!(snapshots instanceof Map)) {
            return out;
        }
        for (Map.Entry<?, ?> e : ((Map<?, ?>) snapshots).entrySet()) {
            if (!stripPnpmKey(String.valueOf(e.getKey())).equals(snapshotKey) || !(e.getValue() instanceof Map)) {
                continue;
            }
            Map<?, ?> body = (Map<?, ?>) e.getValue();
            for (String scope : Arrays.asList("dependencies", "optionalDependencies")) {
                Object deps = body.get(scope);
                if (deps instanceof Map) {
                    for (Map.Entry<?, ?> d : ((Map<?, ?>) deps).entrySet()) {
                        out.putIfAbsent(String.valueOf(d.getKey()), String.valueOf(d.getValue()));
                    }
                }
            }
        }
        return out;
    }

    private static boolean importerDeclaresPnpm(String lock, String dep) {
        Object loaded = new Yaml().load(lock);
        if (!(loaded instanceof Map)) {
            return false;
        }
        Object importers = ((Map<?, ?>) loaded).get("importers");
        List<Object> roots = new ArrayList<>();
        if (importers instanceof Map) {
            roots.addAll(((Map<?, ?>) importers).values());
        } else {
            roots.add(loaded); // single-package v6: the root mapping is the sole importer
        }
        for (Object importer : roots) {
            if (!(importer instanceof Map)) {
                continue;
            }
            for (String scope : Arrays.asList("dependencies", "devDependencies", "optionalDependencies")) {
                Object deps = ((Map<?, ?>) importer).get(scope);
                if (deps instanceof Map && ((Map<?, ?>) deps).containsKey(dep)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The snapshot keys (other than {@code ownerKey}) that reference {@code dep@oldVersion} as a resolved dep. */
    private static Set<String> referrersPnpm(String lock, String dep, String oldVersion, @Nullable String ownerKey) {
        Set<String> referrers = new LinkedHashSet<>();
        Object loaded = new Yaml().load(lock);
        if (!(loaded instanceof Map)) {
            return referrers;
        }
        Object snapshots = ((Map<?, ?>) loaded).get("snapshots");
        if (!(snapshots instanceof Map)) {
            return referrers;
        }
        for (Map.Entry<?, ?> e : ((Map<?, ?>) snapshots).entrySet()) {
            String key = stripPnpmKey(String.valueOf(e.getKey()));
            if ((key.equals(ownerKey)) || !(e.getValue() instanceof Map)) {
                continue;
            }
            Map<?, ?> body = (Map<?, ?>) e.getValue();
            for (String scope : Arrays.asList("dependencies", "optionalDependencies")) {
                Object deps = body.get(scope);
                if (deps instanceof Map) {
                    Object v = ((Map<?, ?>) deps).get(dep);
                    if (v != null) {
                        String vs = String.valueOf(v);
                        if (vs.equals(oldVersion) || vs.startsWith(oldVersion + "(")) {
                            referrers.add(key);
                        }
                    }
                }
            }
        }
        return referrers;
    }

    /**
     * A direct dependency the recipe added, plus its runtime closure. The added dep is resolved over the
     * packument, then its dependencies are walked transitively; each closure member either lands as a fresh
     * entry or dedups to an already-satisfying pin. Any placement that would move, nest, or fork an
     * already-placed package fails loud (an incompatible top-level pin, a member needed at two versions, an
     * excluding reverse-dependent, or an unverified metadata surface).
     */
    private static List<LockEditSet.PackageEdit> resolveClosureAdd(PackageManager pm, DepChange change,
                                                                   String existingLock, @Nullable String memberImporterDir,
                                                                   NodeRegistries registries, NpmRegistryClient client) {
        String rootName = change.name;
        String rootConstraint = Objects.requireNonNull(change.newConstraint);

        if (isUnsupportedProtocol(rootConstraint)) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, rootName,
                    rootName + " uses an unsupported entry type: " + rootConstraint);
        }
        switch (pm) {
            case Pnpm:
                // pnpm is content-addressed: placement is mechanical (one packages+snapshots entry per closure
                // member), but every closure member must be brand-new (no dedupe/conflict) and peer-free.
                return resolveClosureAddPnpm(change, rootName, rootConstraint, existingLock, memberImporterDir, registries, client);
            case YarnClassic:
            case YarnBerry:
                // yarn lists one entry per resolved (name, version); placement is not hoisted, so every closure
                // member must be brand-new (no merge/second entry for an existing name). Berry adds get their
                // checksums enriched afterward; scoped members (leaf or clean closure) are byte-exact.
                return resolveClosureAddYarn(pm, change, rootName, rootConstraint, existingLock, registries, client);
            default:
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, rootName, "adding " + rootName + " requires resolution");
        }
    }

    /**
     * A direct dependency the recipe added, plus its runtime closure, into a pnpm-lock.yaml v9. pnpm is
     * content-addressed, so placement is mechanical: one {@code packages}+{@code snapshots} entry per closure
     * member keyed by its resolved version. Tighter than npm's contract: every member must be brand-new (its name
     * absent from the lock) so no dedupe/fork opens, and none may declare peers or optionalDependencies (pnpm
     * encodes those as peer-suffix keys the mechanical placement does not model). Any such case fails loud.
     */
    private static List<LockEditSet.PackageEdit> resolveClosureAddPnpm(DepChange change, String rootName,
                                                                       String rootConstraint, String existingLock,
                                                                       @Nullable String memberImporterDir,
                                                                       NodeRegistries registries, NpmRegistryClient client) {
        if (pnpmLockMajor(existingLock) < 9) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, rootName,
                    "adding to a pnpm lockfileVersion below 9 is not yet supported");
        }

        Set<String> existingNames = existingPnpmNames(existingLock);
        boolean dev = "devDependencies".equals(change.scope);

        Map<String, Placement> placed = new LinkedHashMap<>();
        Deque<Requirement> queue = new ArrayDeque<>();
        queue.add(new Requirement(rootName, rootConstraint, dev));

        while (!queue.isEmpty()) {
            Requirement req = queue.poll();
            if (existingNames.contains(req.name)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, req.name, req.name +
                        " is already present in the lock; pnpm dedupe/conflict for adds is deferred");
            }
            Placement already = placed.get(req.name);
            if (already != null) {
                if (existingSatisfies(already.version, req.constraint)) {
                    continue;
                }
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, req.name, req.name +
                        " is required at two incompatible versions within the added closure (" +
                        already.version + " vs " + req.constraint + "); deferred");
            }

            NodeRegistry registry = registries.registryFor(req.name);
            String version = resolveAddedVersion(client, registry, req.name, req.constraint);
            VersionManifest manifest = client.getManifest(registry, req.name, version);
            requireEmittablePnpmClosureMember(req.name, manifest);

            VersionManifest.Dist dist = manifest.getDist();
            if (dist == null || dist.getIntegrity() == null) {
                throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, req.name,
                        req.name + "@" + version + " has no registry integrity");
            }

            placed.put(req.name, new Placement(version, manifest, req.dev));
            enqueueDeps(queue, manifest.getDependencies(), req.dev);
        }

        List<LockEditSet.PackageEdit> edits = new ArrayList<>();
        for (Map.Entry<String, Placement> e : placed.entrySet()) {
            Placement p = e.getValue();
            VersionManifest.Dist dist = Objects.requireNonNull(p.manifest.getDist());
            // Only the root (declared in package.json) writes an importer edge; the patcher no-ops the
            // transitives because they are absent from the edited manifest. Scope carries the dev-ness.
            edits.add(LockEditSet.PackageEdit.builder()
                    .name(e.getKey())
                    .oldVersion("")
                    .newVersion(p.version)
                    .newIntegrity(dist.getIntegrity())
                    .newDependencies(notEmpty(p.manifest.getDependencies()) ? p.manifest.getDependencies() : null)
                    .scope(p.dev ? "devDependencies" : "dependencies")
                    .importerDir(e.getKey().equals(rootName) ? memberImporterDir : null)
                    .kind(ADD)
                    .metadata(pnpmLeafMetadata(p.manifest))
                    .build());
        }
        return edits;
    }


    /**
     * A direct dependency the recipe added, plus its runtime closure, into a {@code yarn.lock} v1. yarn lists one
     * {@code name@range:} block per resolved {@code (name, version)}, so placement is mechanical (each block at its
     * {@code sortAlpha} position) with a header of every range that resolves to the version. Every closure member
     * must be brand-new (no header merge or second-version block) and none may declare peers or optionalDependencies
     * (yarn resolves those into further blocks the clean placement does not model); anything else fails loud.
     */
    private static List<LockEditSet.PackageEdit> resolveClosureAddYarn(PackageManager pm, DepChange change, String rootName,
                                                                       String rootConstraint, String existingLock,
                                                                       NodeRegistries registries, NpmRegistryClient client) {
        Set<String> existingNames = pm == PackageManager.YarnBerry ?
                existingBerryNames(existingLock) : existingYarnNames(existingLock);
        boolean dev = "devDependencies".equals(change.scope);

        // Promoting a transitive to a direct dep: if it is already present at a satisfying version, yarn merges the
        // new selector/descriptor into the existing entry (no new blocks). A non-satisfying version forks.
        if ((pm == PackageManager.YarnClassic || pm == PackageManager.YarnBerry) && existingNames.contains(rootName)) {
            return promoteYarn(pm, change, rootName, rootConstraint, existingLock);
        }

        Map<String, Placement> placed = new LinkedHashMap<>();
        Deque<Requirement> queue = new ArrayDeque<>();
        queue.add(new Requirement(rootName, rootConstraint, dev));

        while (!queue.isEmpty()) {
            Requirement req = queue.poll();
            if (existingNames.contains(req.name)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, req.name, req.name +
                        " is already present in the lock; yarn would merge a selector or fork a second block (deferred)");
            }
            Placement already = placed.get(req.name);
            if (already != null) {
                if (existingSatisfies(already.version, req.constraint)) {
                    continue;
                }
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, req.name, req.name +
                        " is required at two incompatible versions within the added closure (" +
                        already.version + " vs " + req.constraint + "); deferred");
            }

            NodeRegistry registry = registries.registryFor(req.name);
            String version = resolveAddedVersion(client, registry, req.name, req.constraint);
            VersionManifest manifest = client.getManifest(registry, req.name, version);
            requireEmittableYarnClosureMember(req.name, manifest);

            VersionManifest.Dist dist = manifest.getDist();
            if (dist == null || dist.getTarball() == null || dist.getShasum() == null || dist.getIntegrity() == null) {
                throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, req.name,
                        req.name + "@" + version + " has no registry tarball/shasum/integrity");
            }

            placed.put(req.name, new Placement(version, manifest, req.dev));
            enqueueDeps(queue, manifest.getDependencies(), req.dev);
        }

        List<LockEditSet.PackageEdit> edits = new ArrayList<>();
        for (Map.Entry<String, Placement> e : placed.entrySet()) {
            Placement p = e.getValue();
            VersionManifest.Dist dist = Objects.requireNonNull(p.manifest.getDist());
            // Only the root writes an importer edge in package.json; the patcher derives each block header's
            // selector ranges from the added edits' dependency maps plus the root's declared constraint.
            edits.add(LockEditSet.PackageEdit.builder()
                    .name(e.getKey())
                    .oldVersion("")
                    .newVersion(p.version)
                    .newResolved(dist.getTarball())
                    .newShasum(dist.getShasum())
                    .newIntegrity(dist.getIntegrity())
                    .newDependencies(notEmpty(p.manifest.getDependencies()) ? p.manifest.getDependencies() : null)
                    .scope(p.dev ? "devDependencies" : "dependencies")
                    .importerDir(null)
                    .kind(ADD)
                    .build());
        }
        return edits;
    }

    /** A yarn promotion: merge the declared selector/descriptor into the existing entry if its version satisfies. */
    private static List<LockEditSet.PackageEdit> promoteYarn(PackageManager pm, DepChange change, String rootName,
                                                             String rootConstraint, String existingLock) {
        Set<String> locked = findLockedVersions(pm, existingLock, rootName);
        if (locked.size() != 1) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, rootName,
                    rootName + " is present at multiple versions; promoting it may fork (deferred)");
        }
        if (!Semver.satisfies(locked.iterator().next(), rootConstraint, NODE)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, rootName, rootName + " is locked at " +
                    locked.iterator().next() + " which excludes " + rootConstraint + " (yarn would fork); deferred");
        }
        return singletonList(LockEditSet.PackageEdit.builder()
                .name(rootName)
                .newConstraint(rootConstraint)
                .scope(change.scope)
                .kind(PROMOTION)
                .build());
    }

    /** The add gates reject any optional or peer dependency: none of these placements models them. */
    private static void requireNoOptionalOrPeer(String name, VersionManifest m, String pm) {
        if (notEmpty(m.getOptionalDependencies())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    "adding " + name + " pulls in optionalDependencies not yet supported for " + pm + " adds");
        }
        if (notEmpty(m.getPeerDependencies()) || nonEmptyObject(m.getPeerDependenciesMeta())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    "adding " + name + " declares peerDependencies not yet supported for " + pm + " adds");
        }
    }

    private static void requireEmittableYarnClosureMember(String name, VersionManifest m) {
        requireNoOptionalOrPeer(name, m, "yarn");
    }

    /** Every package name that heads a block in the yarn.lock (across all merged selectors). */
    private static Set<String> existingYarnNames(String lock) {
        Set<String> names = new LinkedHashSet<>();
        for (String line : lock.split("\n")) {
            if (line.isEmpty() || Character.isWhitespace(line.charAt(0)) || line.charAt(0) == '#' || !line.endsWith(":")) {
                continue;
            }
            String header = line.substring(0, line.length() - 1);
            for (String selector : header.split(",")) {
                String s = unquote(selector.trim());
                int at = s.lastIndexOf('@');
                if (at > 0) {
                    names.add(s.substring(0, at));
                }
            }
        }
        return names;
    }

    /** The only packages-entry metadata the pnpm add patcher renders is {@code engines}; carry it, drop the rest. */
    private static LockEditSet.@Nullable EntryMetadata pnpmLeafMetadata(VersionManifest m) {
        if (!notEmpty(m.getEngines())) {
            return null;
        }
        return LockEditSet.EntryMetadata.builder().engines(m.getEngines()).build();
    }

    /**
     * A closure member the pnpm add patcher can insert byte-exactly: {@code resolution} + optional
     * {@code engines} only. Any peer (pnpm's suffix-key surface), optionalDependencies, or object/array
     * metadata not yet verified byte-exact defers the whole add.
     */
    private static void requireEmittablePnpmClosureMember(String name, VersionManifest m) {
        requireNoOptionalOrPeer(name, m, "pnpm");
        String metadata = unsupportedPnpmMetadata(m);
        if (metadata != null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    "adding " + name + " carries " + metadata + " metadata not yet supported for pnpm adds");
        }
    }

    private static @Nullable String unsupportedPnpmMetadata(VersionManifest m) {
        if (notEmpty(m.getOs())) return "os";
        if (notEmpty(m.getCpu())) return "cpu";
        if (notEmpty(m.getLibc())) return "libc";
        if (m.getDeprecated() != null) return "deprecated";
        if (m.getBin() != null) return "bin";
        if (bool(m.getHasInstallScript())) return "hasInstallScript";
        if (notEmpty(m.getBundleDependencies())) return "bundleDependencies";
        return null;
    }

    /** Every package name present in the pnpm lock's {@code packages}/{@code snapshots} sections. */
    private static Set<String> existingPnpmNames(String lock) {
        Set<String> names = new LinkedHashSet<>();
        Object loaded = new Yaml().load(lock);
        if (loaded instanceof Map) {
            Map<?, ?> root = (Map<?, ?>) loaded;
            collectPnpmNames(root.get("packages"), names);
            collectPnpmNames(root.get("snapshots"), names);
        }
        return names;
    }

    private static void collectPnpmNames(@Nullable Object node, Set<String> names) {
        if (!(node instanceof Map)) {
            return;
        }
        for (Object key : ((Map<?, ?>) node).keySet()) {
            String k = stripPnpmKey(String.valueOf(key));
            int at = k.lastIndexOf('@');
            if (at > 0) {
                names.add(k.substring(0, at));
            }
        }
    }

    private static int pnpmLockMajor(String lock) {
        Object loaded = new Yaml().load(lock);
        if (loaded instanceof Map) {
            Object v = ((Map<?, ?>) loaded).get("lockfileVersion");
            if (v != null) {
                return majorOf(String.valueOf(v));
            }
        }
        return 0;
    }

    private static void enqueueDeps(Deque<Requirement> queue, @Nullable Map<String, String> deps, boolean dev) {
        if (deps == null) {
            return;
        }
        for (Map.Entry<String, String> e : deps.entrySet()) {
            queue.add(new Requirement(e.getKey(), e.getValue(), dev));
        }
    }

    /** Whether an already-placed {@code version} satisfies a new {@code constraint} (proven dedup only). */
    private static boolean existingSatisfies(String version, String constraint) {
        return Semver.validate(constraint, null, NODE).isValid() && Semver.satisfies(version, constraint, NODE);
    }

    /** Every installed {@code node_modules/...} entry (keyed by full path), the candidate set for hoisting resolution. */

    /** The single top-level version of each hoisted package ({@code packages["node_modules/<name>"]}). */

    private static String resolveAddedVersion(NpmRegistryClient client, NodeRegistry registry,
                                              String name, String constraint) {
        AbbreviatedPackument packument = client.getPackument(registry, name);
        if (Semver.validate(constraint, null, NODE).isValid()) {
            String best = Semver.maxSatisfying(packument.getVersions(), constraint, NODE);
            if (best != null) {
                return best;
            }
        }
        String tagged = packument.getDistTags().get(constraint);
        if (tagged != null && packument.getVersions().contains(tagged)) {
            return tagged;
        }
        throw new EngineFailure(Reason.VERSION_NOT_FOUND, name,
                "no published version of " + name + " satisfies " + constraint);
    }

    /** The peers npm would auto-install: declared in {@code peerDependencies} and not marked optional. */

    /** The metadata surfaces whose byte-exact npm serialization is not yet verified; each defers the add. */

    private static boolean nonEmptyObject(@Nullable JsonNode node) {
        return node != null && node.isObject() && node.size() > 0;
    }

    /** npm records a string {@code funding} as {@code {url: <string>}}; object/array forms are gated out upstream. */

    private static boolean notEmpty(@Nullable Map<String, ?> map) {
        return map != null && !map.isEmpty();
    }

    private static boolean notEmpty(@Nullable List<?> list) {
        return list != null && !list.isEmpty();
    }

    private static String resolveTarget(NpmRegistryClient client, NodeRegistries registries,
                                        String name, String oldVersion, String newConstraint) {
        // Minimal-update: keep the already-locked version when it still satisfies the new range.
        if (Semver.satisfies(oldVersion, newConstraint, NODE)) {
            return oldVersion;
        }
        NodeRegistry registry = registries.registryFor(name);
        Set<String> published = client.getPackument(registry, name).getVersions();
        String best = Semver.maxSatisfying(published, newConstraint, NODE);
        if (best == null) {
            throw new EngineFailure(Reason.VERSION_NOT_FOUND, name,
                    "no published version of " + name + " satisfies " + newConstraint);
        }
        return best;
    }

    /**
     * The strict layout whitelist for a bump, minus the {@code dependencies} surface (the cascade handles that):
     * the two manifests must agree on every other closure-affecting surface; only non-layout entry metadata
     * (engines/license/deprecated/bin) may differ.
     */
    private static void proveNonDependencySurfacesUnchanged(String name, VersionManifest oldM, VersionManifest newM) {
        requireEqual(name, "peerDependencies", oldM.getPeerDependencies(), newM.getPeerDependencies());
        requireEqual(name, "peerDependenciesMeta", oldM.getPeerDependenciesMeta(), newM.getPeerDependenciesMeta());
        requireEqual(name, "optionalDependencies", oldM.getOptionalDependencies(), newM.getOptionalDependencies());
        provePlatformSurfacesUnchanged(name, oldM, newM);
    }

    /** The always-strict subset: platform gates and install scripts, which reshape the graph or its side effects. */
    private static void provePlatformSurfacesUnchanged(String name, VersionManifest oldM, VersionManifest newM) {
        requireEqual(name, "os", oldM.getOs(), newM.getOs());
        requireEqual(name, "cpu", oldM.getCpu(), newM.getCpu());
        requireEqual(name, "libc", oldM.getLibc(), newM.getLibc());
        requireEqual(name, "bundleDependencies", oldM.getBundleDependencies(), newM.getBundleDependencies());
        if (!Objects.equals(bool(oldM.getHasInstallScript()), bool(newM.getHasInstallScript()))) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + " hasInstallScript changed");
        }
        // Peer-provider and dedupe-reshuffle detection need the full hoisting model; the single-locked-version
        // guard plus the cascade's reverse-edge check reject the common triggers.
    }

    private static boolean dependenciesEqual(VersionManifest oldM, VersionManifest newM) {
        return Objects.equals(normalize(oldM.getDependencies()), normalize(newM.getDependencies()));
    }

    /** Whether the new version removes a {@code dependencies} edge the old version declared (orphan-prune). */
    private static boolean dropsDependencyEdge(VersionManifest oldM, VersionManifest newM) {
        Map<String, String> oldDeps = oldM.getDependencies();
        if (oldDeps == null || oldDeps.isEmpty()) {
            return false;
        }
        Map<String, String> newDeps = newM.getDependencies() == null ?
                emptyMap() : newM.getDependencies();
        for (String dep : oldDeps.keySet()) {
            if (!newDeps.containsKey(dep)) {
                return true;
            }
        }
        return false;
    }

    private static void requireEqual(String name, String surface, @Nullable Object a, @Nullable Object b) {
        if (!Objects.equals(normalize(a), normalize(b))) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + " " + surface + " changed");
        }
    }

    /** Installed lock entries whose recorded constraint on {@code name} excludes {@code target}, keyed by their packages key. */

    /** Whether {@code dependentKey} already carries its own nested {@code node_modules/<name>} satisfying {@code constraint}. */

    /** Whether some importer or installed entry pins {@code name} at exactly {@code version} (so its top slot cannot move). */

    /** The moving package's existing entry must be a plain registry leaf to relocate byte-for-byte. */

    /**
     * A closure-unchanged proof only inspects the moving package's own manifest, not the other entries that depend
     * on it. If a reverse-dependent's recorded constraint excludes the new version, re-pinning would emit a lock a
     * real install rejects, so fail loud instead.
     */
    private static void proveReverseDependentsAccept(PackageManager pm, String name, String oldVersion,
                                                     String targetVersion, String lock) {
        switch (pm) {
            case Pnpm:
                proveReverseDependentsPnpm(name, oldVersion, targetVersion, lock);
                break;
            case YarnBerry:
                proveReverseDependentsBerry(name, targetVersion, lock);
                break;
            default:
                // yarn-classic merges selectors into shared headers; the single-locked-version guard covers the common case.
                break;
        }
    }

    /** A berry entry's {@code dependencies} map records the requirer's range; a range excluding the target forks. */
    private static void proveReverseDependentsBerry(String name, String targetVersion, String lock) {
        Object loaded = new Yaml().load(lock);
        if (!(loaded instanceof Map)) {
            return;
        }
        for (Map.Entry<?, ?> e : ((Map<?, ?>) loaded).entrySet()) {
            String key = String.valueOf(e.getKey());
            // Skip the importer (its constraint is the user's package.json, re-pinned by the patcher).
            if ("__metadata".equals(key) || key.contains("@workspace:") || !(e.getValue() instanceof Map)) {
                continue;
            }
            Map<?, ?> entry = (Map<?, ?>) e.getValue();
            checkBerryReverseConstraint(name, targetVersion, key, entry.get("dependencies"));
            checkBerryReverseConstraint(name, targetVersion, key, entry.get("optionalDependencies"));
            checkBerryReverseConstraint(name, targetVersion, key, entry.get("peerDependencies"));
        }
    }

    private static void checkBerryReverseConstraint(String name, String targetVersion, String dependent,
                                                    @Nullable Object depMap) {
        if (!(depMap instanceof Map)) {
            return;
        }
        Object value = ((Map<?, ?>) depMap).get(name);
        if (!(value instanceof String) || !((String) value).startsWith("npm:")) {
            return;
        }
        String range = ((String) value).substring("npm:".length());
        if (Semver.validate(range, null, NODE).isValid() && !Semver.satisfies(targetVersion, range, NODE)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " is required by " + dependent + " at " + value + " which excludes " + targetVersion);
        }
    }

    /**
     * pnpm's {@code snapshots.*.dependencies} record RESOLVED versions, not constraints, so a reverse-dependent's
     * acceptance of the new version cannot be proven. Conservatively fail loud when the moving package is a
     * transitive dependency of another entry (not only a direct/importer dep).
     */
    private static void proveReverseDependentsPnpm(String name, String oldVersion, String targetVersion, String lock) {
        Object loaded = new Yaml().load(lock);
        if (!(loaded instanceof Map)) {
            return;
        }
        Map<?, ?> root = (Map<?, ?>) loaded;
        if (referencedAsTransitivePnpm(root.get("snapshots"), name, oldVersion) ||
                referencedAsTransitivePnpm(root.get("packages"), name, oldVersion)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " is a transitive dependency of another package; pnpm records resolved versions, so " +
                            "accepting " + targetVersion + " cannot be proven");
        }
    }

    private static boolean referencedAsTransitivePnpm(@Nullable Object graph, String name, String oldVersion) {
        if (!(graph instanceof Map)) {
            return false;
        }
        String ownBase = name + "@" + oldVersion;
        for (Map.Entry<?, ?> e : ((Map<?, ?>) graph).entrySet()) {
            if (stripPnpmKey(String.valueOf(e.getKey())).equals(ownBase) || !(e.getValue() instanceof Map)) {
                continue;
            }
            Map<?, ?> body = (Map<?, ?>) e.getValue();
            for (String scope : Arrays.asList("dependencies", "optionalDependencies")) {
                Object deps = body.get(scope);
                if (deps instanceof Map) {
                    Object v = ((Map<?, ?>) deps).get(name);
                    if (v != null) {
                        String vs = String.valueOf(v);
                        if (vs.equals(oldVersion) || vs.startsWith(oldVersion + "(")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static String stripPnpmKey(String key) {
        String k = key.startsWith("/") ? key.substring(1) : key;
        int paren = k.indexOf('(');
        return paren >= 0 ? k.substring(0, paren) : k;
    }

    private static LockEditSet.@Nullable EntryMetadata metadataDelta(VersionManifest oldM, VersionManifest newM) {
        LockEditSet.EntryMetadata.EntryMetadataBuilder b = LockEditSet.EntryMetadata.builder();
        boolean any = false;
        if (!Objects.equals(normalize(oldM.getEngines()), normalize(newM.getEngines()))) {
            b.engines(newM.getEngines());
            any = true;
        }
        if (!Objects.equals(oldM.getLicenseString(), newM.getLicenseString())) {
            b.license(newM.getLicenseString());
            any = true;
        }
        if (!Objects.equals(oldM.getDeprecated(), newM.getDeprecated())) {
            b.deprecated(newM.getDeprecated());
            any = true;
        }
        if (!Objects.equals(oldM.getBin(), newM.getBin())) {
            b.bin(newM.getBin());
            any = true;
        }
        return any ? b.build() : null;
    }

    // --- package.json diff -----------------------------------------------

    private static List<DepChange> diffDeclaredDeps(String original, String edited) {
        Map<String, DeclaredDep> before = declaredDeps(parseJsonObject(original, true));
        Map<String, DeclaredDep> after = declaredDeps(parseJsonObject(edited, true));

        List<DepChange> changes = new ArrayList<>();
        Set<String> names = new LinkedHashSet<>();
        names.addAll(before.keySet());
        names.addAll(after.keySet());
        for (String name : names) {
            DeclaredDep b = before.get(name);
            DeclaredDep a = after.get(name);
            if (b == null && a != null) {
                changes.add(new DepChange(name, a.scope, null, a.constraint));
            } else if (b != null && a == null) {
                changes.add(new DepChange(name, b.scope, b.constraint, null));
            } else if (b != null && !Objects.equals(b.constraint, a.constraint)) {
                changes.add(new DepChange(name, a.scope, b.constraint, a.constraint));
            }
        }
        return changes;
    }

    private static Map<String, DeclaredDep> declaredDeps(Map<String, Object> root) {
        Map<String, DeclaredDep> out = new LinkedHashMap<>();
        for (String scope : Arrays.asList("dependencies", "devDependencies", "peerDependencies",
                "optionalDependencies", "bundledDependencies")) {
            Object node = root.get(scope);
            if (node instanceof Map) {
                for (Map.Entry<?, ?> e : ((Map<?, ?>) node).entrySet()) {
                    String name = String.valueOf(e.getKey());
                    if (!out.containsKey(name) && e.getValue() != null) {
                        out.put(name, new DeclaredDep(scope, String.valueOf(e.getValue())));
                    }
                }
            }
        }
        return out;
    }

    // --- workspace importer resolution -----------------------------------

    /**
     * The workspace importer that declares this dependency, expressed as its directory relative to the lock
     * (e.g. {@code packages/app}), or {@code null} for the root importer. Matched off the raw lock's importer
     * entries so a workspace-member edit re-pins the member's importer, not the root's.
     */
    private static @Nullable String findImporterDir(PackageManager pm, String lock, String name,
                                                    @Nullable String oldConstraint) {
        switch (pm) {
            case Pnpm:
                return findImporterDirPnpm(lock, name, oldConstraint);
            case YarnBerry:
                return findImporterDirBerry(lock, name, oldConstraint);
            default:
                return null; // yarn-classic merges selectors into a flat file, with no importer keys
        }
    }

    /**
     * The berry workspace importer ({@code <name>@workspace:<dir>}) that declares this dependency, as its directory
     * relative to the lock, or {@code null} for the root importer ({@code @workspace:.}) or a single-package lock.
     */
    private static @Nullable String findImporterDirBerry(String lock, String name, @Nullable String oldConstraint) {
        Object loaded = new Yaml().load(lock);
        if (!(loaded instanceof Map)) {
            return null;
        }
        String unique = null;
        for (Map.Entry<?, ?> e : ((Map<?, ?>) loaded).entrySet()) {
            String key = String.valueOf(e.getKey());
            int idx = key.indexOf("@workspace:");
            if (idx < 0 || !(e.getValue() instanceof Map)) {
                continue;
            }
            String c = berryImporterConstraint((Map<?, ?>) e.getValue(), name);
            if (c != null && (oldConstraint == null || oldConstraint.equals(c))) {
                String dir = key.substring(idx + "@workspace:".length());
                if (".".equals(dir)) {
                    return null; // the root importer owns it
                }
                if (unique != null) {
                    return null; // ambiguous; the shared-importer guard fails this loud
                }
                unique = dir;
            }
        }
        return unique;
    }

    /** A berry importer records its declared range as {@code npm:<range>}; return the bare range. */
    private static @Nullable String berryImporterConstraint(Map<?, ?> importer, String name) {
        for (String scope : DECLARED_SCOPES) {
            Object scopeMap = importer.get(scope);
            if (scopeMap instanceof Map && ((Map<?, ?>) scopeMap).get(name) != null) {
                String v = String.valueOf(((Map<?, ?>) scopeMap).get(name));
                return v.startsWith("npm:") ? v.substring("npm:".length()) : v;
            }
        }
        return null;
    }

    /** How many berry workspace importers declare {@code name} at exactly {@code constraint} (a shared entry when {@code > 1}). */
    private static int importersDeclaring(String lock, String name, @Nullable String constraint) {
        int count = 0;
        Object loaded = new Yaml().load(lock);
        if (loaded instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) loaded).entrySet()) {
                if (String.valueOf(e.getKey()).contains("@workspace:") && e.getValue() instanceof Map) {
                    String c = berryImporterConstraint((Map<?, ?>) e.getValue(), name);
                    if (c != null && (constraint == null || constraint.equals(c))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static @Nullable String findImporterDirPnpm(String lock, String name, @Nullable String oldConstraint) {
        Object loaded = new Yaml().load(lock);
        if (!(loaded instanceof Map)) {
            return null;
        }
        Object importers = ((Map<?, ?>) loaded).get("importers");
        if (!(importers instanceof Map)) {
            return null; // single-package lock: only the root importer
        }
        String unique = null;
        for (Map.Entry<?, ?> e : ((Map<?, ?>) importers).entrySet()) {
            String dir = String.valueOf(e.getKey());
            if (!(e.getValue() instanceof Map)) {
                continue;
            }
            String specifier = pnpmSpecifier((Map<?, ?>) e.getValue(), name);
            if (specifier != null && (oldConstraint == null || oldConstraint.equals(specifier))) {
                if (".".equals(dir)) {
                    return null;
                }
                if (unique != null) {
                    return null;
                }
                unique = dir;
            }
        }
        return unique;
    }

    private static @Nullable String pnpmSpecifier(Map<?, ?> importer, String name) {
        for (String scope : Arrays.asList("dependencies", "devDependencies", "optionalDependencies")) {
            Object scopeMap = importer.get(scope);
            if (scopeMap instanceof Map) {
                Object dep = ((Map<?, ?>) scopeMap).get(name);
                if (dep instanceof Map && ((Map<?, ?>) dep).get("specifier") != null) {
                    return String.valueOf(((Map<?, ?>) dep).get("specifier"));
                }
            }
        }
        return null;
    }

    /**
     * The workspace importer directory that owns a newly-added dependency, derived from the edited manifest's
     * path. An add has no lock entry to match on, so the member is located by its directory being an importer
     * key (the manifest path is repo-relative, and a root lock keys its importers by member directory).
     * {@code null} for the root manifest, a standalone package, or any member whose importer cannot be located
     * exactly — the patcher then targets {@code .} or fails loud. Only pnpm carries per-member importer sections.
     */
    private static @Nullable String addImporterDir(PackageManager pm, String lock, @Nullable Path packageJsonPath) {
        if (pm != PackageManager.Pnpm || packageJsonPath == null) {
            return null;
        }
        Path parent = packageJsonPath.getParent();
        if (parent == null) {
            return null; // root manifest: the "." importer
        }
        String dir = parent.toString().replace('\\', '/');
        if (dir.isEmpty() || ".".equals(dir)) {
            return null;
        }
        return pnpmImporterKeys(lock).contains(dir) ? dir : null;
    }

    private static Set<String> pnpmImporterKeys(String lock) {
        Set<String> keys = new LinkedHashSet<>();
        Object loaded = new Yaml().load(lock);
        if (loaded instanceof Map) {
            Object importers = ((Map<?, ?>) loaded).get("importers");
            if (importers instanceof Map) {
                for (Object k : ((Map<?, ?>) importers).keySet()) {
                    keys.add(String.valueOf(k));
                }
            }
        }
        return keys;
    }

    // --- raw lock inspection (read-only) ---------------------------------

    private static Set<String> findLockedVersions(PackageManager pm, String lock, String name) {
        switch (pm) {
            case Pnpm:
                return findLockedVersionsPnpm(lock, name);
            case YarnClassic:
                return findLockedVersionsYarn(lock, name);
            case YarnBerry:
                return findLockedVersionsBerry(lock, name);
            default:
                return emptySet();
        }
    }

    /** Berry entries are keyed by {@code "<name>@<protocol>:<selector>"} (a valid YAML mapping); read their versions. */
    private static Set<String> findLockedVersionsBerry(String lock, String name) {
        Set<String> versions = new LinkedHashSet<>();
        Object loaded = new Yaml().load(lock);
        if (!(loaded instanceof Map)) {
            return versions;
        }
        for (Map.Entry<?, ?> e : ((Map<?, ?>) loaded).entrySet()) {
            if (berryKeyDeclaresName(String.valueOf(e.getKey()), name) && e.getValue() instanceof Map) {
                Object v = ((Map<?, ?>) e.getValue()).get("version");
                if (v != null) {
                    versions.add(String.valueOf(v));
                }
            }
        }
        return versions;
    }

    /** True when any comma-merged descriptor in {@code key} is {@code <name>@npm:…} (never {@code __metadata}/workspace). */
    private static boolean berryKeyDeclaresName(String key, String name) {
        if ("__metadata".equals(key)) {
            return false;
        }
        for (String descriptor : key.split(",")) {
            String d = descriptor.trim();
            if (d.startsWith(name + "@npm:")) {
                return true;
            }
        }
        return false;
    }

    /**
     * A yarn direct-dep bump whose new {@code dependencies} force a currently-locked transitive to a new version.
     * Both yarn formats are flat (the selector/descriptor is the requirer's range), so each move re-heads the
     * transitive to the new range; a shared requirer, a dropped edge, a constraint-only reshuffle, or a multi-level
     * cascade all fail loud. Only the format-specific lock reads differ between classic (text) and berry (YAML).
     */
    private static List<LockEditSet.PackageEdit> cascadeForcedMovesYarn(PackageManager pm, String rootName,
                                                                        VersionManifest rootOld, VersionManifest rootNew,
                                                                        String lock, NodeRegistries registries, NpmRegistryClient client) {
        Map<String, String> oldDeps = rootOld.getDependencies() == null ? emptyMap() : rootOld.getDependencies();
        Map<String, String> newDeps = rootNew.getDependencies() == null ? emptyMap() : rootNew.getDependencies();
        // A dropped edge is handled by the patcher's orphan GC (flagged via prunesOrphans); this loop re-resolves
        // only kept edges.
        List<LockEditSet.PackageEdit> moves = new ArrayList<>();
        for (Map.Entry<String, String> e : newDeps.entrySet()) {
            String dep = e.getKey();
            String newConstraint = e.getValue();
            Set<String> locked = findLockedVersions(pm, lock, dep);
            if (locked.isEmpty()) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                        "upgrading " + rootName + " introduces new transitive " + dep + " (add-during-bump) not yet supported");
            }
            if (locked.size() > 1) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep, dep + " is present at multiple versions; deferred");
            }
            if (isUnsupportedProtocol(newConstraint) || !Semver.validate(newConstraint, null, NODE).isValid()) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep, dep + " has an unresolvable range: " + newConstraint);
            }
            String cur = locked.iterator().next();
            if (Semver.satisfies(cur, newConstraint, NODE)) {
                if (!newConstraint.equals(oldDeps.get(dep))) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                            dep + " constraint changed without a version move (selector reselect) not yet supported");
                }
                continue;
            }
            moves.add(resolveForcedMoveYarn(pm, rootName, dep, cur, oldDeps.get(dep), newConstraint, lock, registries, client));
        }
        return moves;
    }

    /** Resolve and emit one yarn forced move, failing loud on a shared requirer or any deeper reshape. */
    private static LockEditSet.PackageEdit resolveForcedMoveYarn(PackageManager pm, String rootName, String dep, String oldVersion,
                                                                 @Nullable String oldConstraint, String newConstraint,
                                                                 String lock, NodeRegistries registries, NpmRegistryClient client) {
        if (oldConstraint == null) {
            // The transitive is locked but the upgraded root's old manifest never declared it (it was pulled in
            // via another path), so the minimal engine can't reshape this closure — defer to a real resolver.
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep, "no prior selector for moved " + dep);
        }
        if (yarnHasOtherRequirer(pm, lock, dep, rootName)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                    dep + " is required by more than the upgraded " + rootName + "; a merged selector defers");
        }
        if (pm == PackageManager.YarnBerry && importerDeclaresBerry(lock, dep)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                    dep + " is directly declared; moving it via cascade is not yet supported");
        }
        NodeRegistry registry = registries.registryFor(dep);
        String target = maxSatisfyingAll(client.getPackument(registry, dep).getVersions(), singleton(newConstraint));
        if (target == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep, "no version of " + dep + " satisfies " + newConstraint);
        }
        if (target.equals(oldVersion)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep, dep + " resolves back to its locked version; deferred");
        }
        VersionManifest oldManifest = client.getManifest(registry, dep, oldVersion);
        VersionManifest newManifest = client.getManifest(registry, dep, target);
        proveNonDependencySurfacesUnchanged(dep, oldManifest, newManifest);
        if (!dependenciesEqual(oldManifest, newManifest)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                    "moving " + dep + " to " + target + " changes its own dependencies (multi-level cascade) not yet supported");
        }
        VersionManifest.Dist dist = newManifest.getDist();
        if (dist == null || dist.getTarball() == null || dist.getIntegrity() == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, dep, dep + "@" + target + " has no registry tarball/integrity");
        }
        return LockEditSet.PackageEdit.builder()
                .name(dep)
                .oldVersion(oldVersion)
                .newVersion(target)
                .oldConstraint(oldConstraint)
                .newConstraint(newConstraint)
                .newResolved(dist.getTarball())
                .newIntegrity(dist.getIntegrity())
                .newShasum(dist.getShasum())
                .newDependencies(newManifest.getDependencies())
                .scope("dependencies")
                .importerDir(null)
                .kind(FORCED_MOVE)
                .build();
    }

    /** True when any package other than {@code rootName} also requires {@code dep} (so moving it would fork). */
    private static boolean yarnHasOtherRequirer(PackageManager pm, String lock, String dep, String rootName) {
        return pm == PackageManager.YarnBerry ?
                berryHasOtherRequirer(lock, dep, rootName) : yarnClassicHasOtherRequirer(lock, dep, rootName);
    }

    private static boolean berryHasOtherRequirer(String lock, String dep, String rootName) {
        Object loaded = new Yaml().load(lock);
        if (!(loaded instanceof Map)) {
            return false;
        }
        for (Map.Entry<?, ?> e : ((Map<?, ?>) loaded).entrySet()) {
            String key = String.valueOf(e.getKey());
            if ("__metadata".equals(key) || key.contains("@workspace:") ||
                    berryKeyDeclaresName(key, rootName) || !(e.getValue() instanceof Map)) {
                continue;
            }
            Map<?, ?> entry = (Map<?, ?>) e.getValue();
            for (String scope : Arrays.asList("dependencies", "optionalDependencies", "peerDependencies")) {
                Object deps = entry.get(scope);
                if (deps instanceof Map && ((Map<?, ?>) deps).containsKey(dep)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Scan the flat {@code yarn.lock} for a {@code dependencies:} line naming {@code dep} under any block whose
     * header is not {@code rootName}. yarn.lock is not valid YAML, so this is a header/indent line walk.
     */
    private static boolean yarnClassicHasOtherRequirer(String lock, String dep, String rootName) {
        boolean rootBlock = false;
        boolean inDependencies = false;
        for (String line : lock.split("\n")) {
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            if (!Character.isWhitespace(line.charAt(0)) && line.trim().endsWith(":")) {
                rootBlock = headerMatchesName(line, rootName);
                inDependencies = false;
            } else if (line.startsWith("  dependencies:")) {
                inDependencies = true;
            } else if (line.startsWith("  ") && !line.startsWith("    ")) {
                inDependencies = false; // a sibling field (version/resolved/…) ends the dependencies block
            } else if (inDependencies && !rootBlock && line.startsWith("    ")) {
                String depName = unquote(line.trim().split("\\s+", 2)[0]);
                if (depName.equals(dep)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** True when the workspace importer declares {@code dep} directly (so a cascade would touch its own range). */
    private static boolean importerDeclaresBerry(String lock, String dep) {
        Object loaded = new Yaml().load(lock);
        if (!(loaded instanceof Map)) {
            return false;
        }
        for (Map.Entry<?, ?> e : ((Map<?, ?>) loaded).entrySet()) {
            if (String.valueOf(e.getKey()).contains("@workspace:") && e.getValue() instanceof Map) {
                Map<?, ?> importer = (Map<?, ?>) e.getValue();
                for (String scope : DECLARED_SCOPES) {
                    Object deps = importer.get(scope);
                    if (deps instanceof Map && ((Map<?, ?>) deps).containsKey(dep)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Every registry package name that heads a berry entry (across merged descriptors); ignores importer/metadata. */
    private static Set<String> existingBerryNames(String lock) {
        Set<String> names = new LinkedHashSet<>();
        Object loaded = new Yaml().load(lock);
        if (loaded instanceof Map) {
            for (Object key : ((Map<?, ?>) loaded).keySet()) {
                for (String descriptor : String.valueOf(key).split(",")) {
                    int at = descriptor.trim().indexOf("@npm:");
                    if (at > 0) {
                        names.add(descriptor.trim().substring(0, at));
                    }
                }
            }
        }
        return names;
    }

    /** Fetch each new version's tarball and derive its berry checksum from the lock's cacheKey. */
    private static List<LockEditSet.PackageEdit> enrichBerryChecksums(List<LockEditSet.PackageEdit> edits,
                                                                      String lock, NodeRegistries registries,
                                                                      NpmRegistryClient client) {
        String cacheKey = berryCacheKey(lock);
        List<LockEditSet.PackageEdit> out = new ArrayList<>(edits.size());
        for (LockEditSet.PackageEdit edit : edits) {
            if (edit.getNewVersion() == null || edit.getNewResolved() == null) {
                out.add(edit);
                continue;
            }
            NodeRegistry registry = registries.registryFor(edit.getName());
            byte[] tarball = client.getTarball(registry, edit.getName(), edit.getNewVersion(), edit.getNewResolved());
            out.add(edit.toBuilder()
                    .newBerryChecksum(BerryZipChecksum.checksum(tarball, edit.getName(), cacheKey))
                    .build());
        }
        return out;
    }

    /** The lock's {@code __metadata.cacheKey}; only the validated {@code 10c0} zip format is reproducible so far. */
    private static String berryCacheKey(String lock) {
        String cacheKey = null;
        Object loaded = new Yaml().load(lock);
        if (loaded instanceof Map) {
            Object meta = ((Map<?, ?>) loaded).get("__metadata");
            if (meta instanceof Map) {
                Object value = ((Map<?, ?>) meta).get("cacheKey");
                cacheKey = value == null ? null : String.valueOf(value);
            }
        }
        if (!"10c0".equals(cacheKey)) {
            throw new EngineFailure(Reason.CHECKSUM_UNAVAILABLE, null,
                    "yarn berry cacheKey " + cacheKey + " is not a validated checksum format (only 10c0 so far)");
        }
        return cacheKey;
    }

    private static Set<String> findLockedVersionsPnpm(String lock, String name) {
        Object loaded = new Yaml().load(lock);
        Set<String> versions = new LinkedHashSet<>();
        if (!(loaded instanceof Map)) {
            return versions;
        }
        Map<?, ?> root = (Map<?, ?>) loaded;
        collectPnpmKeys(root.get("packages"), name, versions);
        collectPnpmKeys(root.get("snapshots"), name, versions);
        return versions;
    }

    private static void collectPnpmKeys(@Nullable Object node, String name, Set<String> versions) {
        if (!(node instanceof Map)) {
            return;
        }
        for (Object key : ((Map<?, ?>) node).keySet()) {
            String k = String.valueOf(key);
            // Strip a leading '/' (v6) and any trailing peer suffix "(peer@ver)".
            if (k.startsWith("/")) {
                k = k.substring(1);
            }
            int paren = k.indexOf('(');
            if (paren >= 0) {
                k = k.substring(0, paren);
            }
            int at = k.lastIndexOf('@');
            if (at > 0 && k.substring(0, at).equals(name)) {
                versions.add(k.substring(at + 1));
            }
        }
    }

    /**
     * The resolved versions the workspace importers pin for {@code name} through an edge whose specifier matches
     * {@code oldConstraint} (peer suffix stripped). A single value means the direct dependency is unambiguous at
     * the importer even when the package is separately forked (nested/transitive) elsewhere in the graph.
     */
    private static Set<String> importerResolvedVersionsPnpm(String lock, String name, @Nullable String oldConstraint) {
        Set<String> versions = new LinkedHashSet<>();
        Object loaded = new Yaml().load(lock);
        if (!(loaded instanceof Map)) {
            return versions;
        }
        Object importers = ((Map<?, ?>) loaded).get("importers");
        List<Object> roots = new ArrayList<>();
        if (importers instanceof Map) {
            roots.addAll(((Map<?, ?>) importers).values());
        } else {
            roots.add(loaded); // single-package v6: the root mapping is the sole importer
        }
        for (Object importer : roots) {
            if (!(importer instanceof Map)) {
                continue;
            }
            for (String scope : Arrays.asList("dependencies", "devDependencies", "optionalDependencies")) {
                Object deps = ((Map<?, ?>) importer).get(scope);
                if (!(deps instanceof Map)) {
                    continue;
                }
                Object dep = ((Map<?, ?>) deps).get(name);
                if (!(dep instanceof Map)) {
                    continue;
                }
                Object specifier = ((Map<?, ?>) dep).get("specifier");
                Object version = ((Map<?, ?>) dep).get("version");
                if (version != null && (oldConstraint == null || oldConstraint.equals(String.valueOf(specifier)))) {
                    String v = String.valueOf(version);
                    int paren = v.indexOf('(');
                    versions.add(paren >= 0 ? v.substring(0, paren) : v);
                }
            }
        }
        return versions;
    }

    /**
     * A pnpm bump renames the {@code packages}/{@code snapshots} key {@code name@old} to {@code name@new} in place.
     * That is byte-exact only when the rename does not cross another version of the same package in pnpm's
     * (lexicographic) key order; otherwise a real pnpm reorders the entries, so defer.
     */
    private static void requirePnpmRenameKeepsOrder(String name, String oldVersion, String newVersion, String lock) {
        String oldKey = name + "@" + oldVersion;
        String newKey = name + "@" + newVersion;
        String lo = oldKey.compareTo(newKey) <= 0 ? oldKey : newKey;
        String hi = oldKey.compareTo(newKey) <= 0 ? newKey : oldKey;
        Object loaded = new Yaml().load(lock);
        if (!(loaded instanceof Map)) {
            return;
        }
        Map<?, ?> root = (Map<?, ?>) loaded;
        for (String section : new String[]{"packages", "snapshots"}) {
            Object node = root.get(section);
            if (!(node instanceof Map)) {
                continue;
            }
            for (Object key : ((Map<?, ?>) node).keySet()) {
                String k = String.valueOf(key);
                String bare = k.startsWith("/") ? k.substring(1) : k;
                if (bare.equals(oldKey) || !bare.startsWith(name + "@")) {
                    continue;
                }
                if (bare.compareTo(lo) > 0 && bare.compareTo(hi) < 0) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                            newKey + " would reorder past " + bare +
                                    "; pnpm's in-place rename is not byte-exact, resolution required");
                }
            }
        }
    }

    private static Set<String> findLockedVersionsYarn(String lock, String name) {
        // yarn.lock is not valid YAML; best-effort scan of "name@range:" headers and their `version` line.
        Set<String> versions = new LinkedHashSet<>();
        String[] lines = lock.split("\n");
        boolean inMatchingBlock = false;
        for (String line : lines) {
            if (!line.isEmpty() && !Character.isWhitespace(line.charAt(0)) && line.trim().endsWith(":")) {
                inMatchingBlock = headerMatchesName(line, name);
            } else if (inMatchingBlock) {
                String trimmed = line.trim();
                if (trimmed.startsWith("version ")) {
                    versions.add(unquote(trimmed.substring("version ".length()).trim()));
                    inMatchingBlock = false;
                }
            }
        }
        return versions;
    }

    private static boolean headerMatchesName(String header, String name) {
        for (String selector : header.substring(0, header.length() - 1).split(",")) {
            String s = unquote(selector.trim());
            int at = s.lastIndexOf('@');
            if (at > 0 && s.substring(0, at).equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static void requireSupportedPnpmVersion(String lock) {
        Object loaded;
        try {
            loaded = new Yaml().load(lock);
        } catch (RuntimeException e) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "unparseable pnpm-lock.yaml: " + e.getMessage());
        }
        if (!(loaded instanceof Map)) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "pnpm-lock.yaml is not a mapping");
        }
        Object version = ((Map<?, ?>) loaded).get("lockfileVersion");
        if (version == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "pnpm-lock.yaml has no lockfileVersion");
        }
        int major = majorOf(String.valueOf(version));
        if (major < 6) {
            throw new EngineFailure(Reason.UNSUPPORTED_LOCKFILE_VERSION, null,
                    "pnpm lockfileVersion " + version + " is not supported (need v6 or v9)");
        }
    }

    // --- helpers ---------------------------------------------------------

    private static Path lockPath(PackageManager pm, @Nullable Path packageJsonPath) {
        LockFileRegeneration regen = LockFileRegeneration.forPackageManager(pm);
        String lockFile = regen == null ? "lock" : regen.getLockFile();
        return packageJsonPath == null ? Paths.get(lockFile) : packageJsonPath.resolveSibling(lockFile);
    }

    private static @Nullable LockPatcher patcherFor(PackageManager pm) {
        switch (pm) {
            case Pnpm:        return new PnpmLockPatcher();
            case YarnClassic: return new YarnClassicLockPatcher();
            case YarnBerry:   return new YarnBerryLockPatcher();
            default:          return null;
        }
    }

    private static Map<String, Object> parseJsonObject(String content, boolean manifest) {
        try {
            JsonNode root = JSON.readTree(content);
            if (root == null || !root.isObject()) {
                throw new EngineFailure(manifest ? Reason.MALFORMED_MANIFEST : Reason.MALFORMED_LOCK, null,
                        (manifest ? "package.json" : "lock file") + " is not a JSON object");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = JSON.convertValue(root, Map.class);
            return map;
        } catch (EngineFailure e) {
            throw e;
        } catch (Exception e) {
            throw new EngineFailure(manifest ? Reason.MALFORMED_MANIFEST : Reason.MALFORMED_LOCK, null,
                    "unparseable " + (manifest ? "package.json" : "lock file") + ": " + e.getMessage());
        }
    }

    private static boolean isUnsupportedProtocol(String constraint) {
        String c = constraint.trim();
        for (String protocol : UNSUPPORTED_PROTOCOLS) {
            if (c.startsWith(protocol)) {
                return true;
            }
        }
        return false;
    }

    private static int majorOf(String version) {
        String head = version.trim();
        int dot = head.indexOf('.');
        if (dot >= 0) {
            head = head.substring(0, dot);
        }
        try {
            return Integer.parseInt(head.trim());
        } catch (NumberFormatException e) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "unrecognised lockfileVersion: " + version);
        }
    }

    private static @Nullable Object normalize(@Nullable Object value) {
        if (value instanceof Map && ((Map<?, ?>) value).isEmpty()) {
            return null;
        }
        if (value instanceof List && ((List<?>) value).isEmpty()) {
            return null;
        }
        if (value instanceof JsonNode && ((JsonNode) value).isEmpty()) {
            return null;
        }
        return value;
    }

    private static boolean bool(@Nullable Boolean b) {
        return Boolean.TRUE.equals(b);
    }

    private static String unquote(String s) {
        String t = s.trim();
        if (t.length() >= 2 && t.charAt(0) == '"' && t.charAt(t.length() - 1) == '"') {
            return t.substring(1, t.length() - 1);
        }
        return t;
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

    private static final class DeclaredDep {
        final String scope;
        final String constraint;

        DeclaredDep(String scope, String constraint) {
            this.scope = scope;
            this.constraint = constraint;
        }
    }

    /** A pending edge in the closure walk: a package name, the requiring constraint, and dev-reachability. */
    private static final class Requirement {
        final String name;
        final String constraint;
        final boolean dev;

        Requirement(String name, String constraint, boolean dev) {
            this.name = name;
            this.constraint = constraint;
            this.dev = dev;
        }
    }

    /** A freshly-resolved closure member: its chosen version, manifest, and dev-reachability. */
    private static final class Placement {
        final String version;
        final VersionManifest manifest;
        final boolean dev;

        Placement(String version, VersionManifest manifest, boolean dev) {
            this.version = version;
            this.manifest = manifest;
            this.dev = dev;
        }
    }

    private static final class DepChange {
        final String name;
        final String scope;
        final @Nullable String oldConstraint;
        final @Nullable String newConstraint;

        DepChange(String name, String scope, @Nullable String oldConstraint, @Nullable String newConstraint) {
            this.name = name;
            this.scope = scope;
            this.oldConstraint = oldConstraint;
            this.newConstraint = newConstraint;
        }
    }
}
