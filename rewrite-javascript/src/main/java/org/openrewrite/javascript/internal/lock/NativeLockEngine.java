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
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.resolve.LockResolver;
import org.openrewrite.javascript.internal.lock.resolve.LockResolvers;
import org.openrewrite.javascript.internal.lock.resolve.NpmGraphBuilder;
import org.openrewrite.javascript.internal.lock.resolve.NpmRegistryAdapter;
import org.openrewrite.javascript.internal.lock.resolve.Registry;
import org.openrewrite.javascript.internal.lock.resolve.ResolutionGraph;
import org.openrewrite.javascript.internal.lock.resolve.ResolveRequest;
import org.openrewrite.javascript.internal.registry.AbbreviatedPackument;
import org.openrewrite.javascript.internal.registry.Environment;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;
import org.openrewrite.javascript.internal.registry.RegistryDiscovery;
import org.openrewrite.javascript.internal.registry.VersionManifest;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.semver.NodeSemver;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit.Kind.*;

/**
 * The shared, package-manager-agnostic orchestrator for native lock regeneration. It diffs the pre-edit and
 * post-edit {@code package.json} to scope the change to the declared dependencies the recipe touched, proves
 * each moving dependency leaves the resolved closure unchanged (the strict layout whitelist), resolves the
 * target version against the registry, and hands a proven {@link LockEditSet} to the format's {@link LockPatcher}.
 * <p>
 * Everything outside the whitelist fails loud with a structured {@link Failure} and no lock; there is no
 * shell-out fallback (it would leak registry credentials). The one tolerance is the write-through metadata tier
 * ({@code engines}/{@code license}/{@code deprecated}/{@code bin}) a real bump patches without reshaping the tree.
 * Reads over the raw lock and manifests use Jackson/SnakeYAML directly, never the lossy adapter normalization.
 */
public final class NativeLockEngine {

    // Bun's lock is JSONC (trailing commas, // comments); npm's is strict JSON, so this mapper reads both.
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
     * The cannot-reshape deferrals the from-scratch {@link LockResolver} may still reproduce byte-exact. Genuine
     * input/environment failures (malformed lock/manifest, registry/auth/not-found) are deliberately absent — the
     * resolver cannot fix those, so they stay fail-loud.
     */
    private static final Set<Reason> RESOLVER_FALLBACK_REASONS =
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
        try {
            return doRegenerate(pm, editedPackageJson, originalPackageJson, existingLock, marker, packageJsonPath, ctx);
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

        // Built once and shared by both the surgical patch tier and the resolver fallback.
        NodeRegistries registries = RegistryDiscovery.discover(ctx, marker, Environment.SYSTEM);
        NpmRegistryClient client = NodeExecutionContextView.view(ctx).getRegistryClient();

        try {
            return surgicalRegenerate(pm, editedPackageJson, originalPackageJson, existingLock,
                    packageJsonPath, registries, client);
        } catch (EngineFailure surgical) {
            return resolverFallback(pm, surgical, editedPackageJson, existingLock, packageJsonPath, registries, client);
        } catch (RecipeRunException rre) {
            // A patcher failing loud from inside a rewrite-json/yaml visitor wraps its EngineFailure; unwrap it
            // so a cannot-reshape deferral still routes through the resolver fallback.
            EngineFailure surgical = engineFailureCause(rre);
            if (surgical == null) {
                throw rre;
            }
            return resolverFallback(pm, surgical, editedPackageJson, existingLock, packageJsonPath, registries, client);
        }
    }

    /** The fast surgical patch tier: diff the declared deps, prove the closure is unchanged, and patch in place. */
    private static Result surgicalRegenerate(PackageManager pm, String editedPackageJson,
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
     * The resolver fallback tier: when the surgical patch defers because it cannot reshape the closure, resolve the
     * whole closure from scratch with the package manager's {@link LockResolver}. The resolver is
     * byte-exact-or-fail-loud, so this never yields a lock a real install would disagree with; when it also defers,
     * its (deeper) failure propagates and is returned. A genuine input/environment deferral the resolver cannot fix
     * (or a package manager without a resolver) rethrows the surgical failure unchanged.
     */
    private static Result resolverFallback(PackageManager pm, EngineFailure surgical, String editedPackageJson,
                                           String existingLock, @Nullable Path packageJsonPath,
                                           NodeRegistries registries, NpmRegistryClient client) {
        LockResolver resolver = LockResolvers.forPackageManager(pm);
        boolean diffs = pm == PackageManager.Npm || pm == PackageManager.Pnpm ||
                pm == PackageManager.YarnBerry || pm == PackageManager.Bun;
        if ((!diffs && resolver == null) ||
                !RESOLVER_FALLBACK_REASONS.contains(surgical.failure.getReason())) {
            throw surgical;
        }
        // A single edited manifest resolves as a single importer. For a workspace that would drop the sibling
        // importers and write a truncated lock, so keep those deferred (return the per-dependency failure).
        if (isMultiPackageProject(pm, editedPackageJson, existingLock, packageJsonPath)) {
            throw surgical;
        }
        try {
            if (pm == PackageManager.Npm) {
                return resolveAndPatchNpm(editedPackageJson, existingLock, packageJsonPath, registries, client);
            }
            if (pm == PackageManager.Pnpm) {
                return resolveAndPatchPnpm(editedPackageJson, existingLock, packageJsonPath, registries, client);
            }
            if (pm == PackageManager.YarnBerry) {
                return resolveAndPatchYarnBerry(editedPackageJson, existingLock, packageJsonPath, registries, client);
            }
            if (pm == PackageManager.Bun) {
                return resolveAndPatchBun(editedPackageJson, existingLock, packageJsonPath, registries, client);
            }
            ResolveRequest request = new ResolveRequest(
                    Collections.singletonMap("", editedPackageJson), existingLock, registries, client);
            return Result.success(resolver.resolve(request));
        } catch (NodeRegistryException nre) {
            // The deeper closure walk hit a registry/environment error the per-dependency path did not need to
            // reach; it produced no better answer, so return that deferral unchanged (no wrong lock either way).
            throw surgical;
        }
        // An EngineFailure from the deeper attempt propagates: its reason/detail is preferred.
    }

    /**
     * Resolve the whole closure seeded by the existing lock, then patch only the difference: a locked version
     * still satisfying its range is kept (as a real incremental install would), the resolved graph is diffed
     * against the lock, and the same patcher applies the edits — untouched entries keep their bytes.
     */
    private static Result resolveAndPatchNpm(String editedPackageJson, String existingLock,
                                             @Nullable Path packageJsonPath, NodeRegistries registries,
                                             NpmRegistryClient client) {
        Registry registry = new NpmRegistryAdapter(registries, client);
        ResolutionGraph graph = new NpmGraphBuilder(registry, true, lockedVersionsNpm(existingLock))
                .build(Collections.singletonMap("", editedPackageJson));
        List<LockEditSet.PackageEdit> edits = NpmLockDiff.diff(graph, existingLock);
        LockEditSet editSet = new LockEditSet(existingLock, lockPath(PackageManager.Npm, packageJsonPath),
                PackageManager.Npm, editedPackageJson, edits);
        return Result.success(new NpmLockPatcher().patch(editSet));
    }

    /**
     * Resolve the closure seeded by the existing berry lock, diff it, and patch only the difference. Untouched
     * entries keep their recorded checksums; only fresh or moved entries reproduce theirs from the tarball.
     */
    private static Result resolveAndPatchYarnBerry(String editedPackageJson, String existingLock,
                                                   @Nullable Path packageJsonPath, NodeRegistries registries,
                                                   NpmRegistryClient client) {
        Registry registry = new NpmRegistryAdapter(registries, client);
        ResolutionGraph graph = new NpmGraphBuilder(registry, false, lockedVersionsBerry(existingLock))
                .build(Collections.singletonMap("", editedPackageJson));
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

    /** Resolve the closure seeded by the existing pnpm lock, diff it, and patch only the difference. */
    private static Result resolveAndPatchPnpm(String editedPackageJson, String existingLock,
                                              @Nullable Path packageJsonPath, NodeRegistries registries,
                                              NpmRegistryClient client) {
        Registry registry = new NpmRegistryAdapter(registries, client);
        ResolutionGraph graph = new NpmGraphBuilder(registry, false, lockedVersionsPnpm(existingLock))
                .build(Collections.singletonMap("", editedPackageJson));
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
     * The bun analogue of {@link #resolveAndPatchNpm}: resolve the closure seeded by the existing bun.lock
     * (bun keeps a satisfying locked version, like npm, but does not auto-install missing peers), diff the
     * graph against the lock, and patch only the difference.
     */
    private static Result resolveAndPatchBun(String editedPackageJson, String existingLock,
                                             @Nullable Path packageJsonPath, NodeRegistries registries,
                                             NpmRegistryClient client) {
        Registry registry = new NpmRegistryAdapter(registries, client);
        ResolutionGraph graph = new NpmGraphBuilder(registry, false, lockedVersionsBun(existingLock))
                .build(Collections.singletonMap("", editedPackageJson));
        List<LockEditSet.PackageEdit> edits = BunLockDiff.diff(graph, existingLock);
        LockEditSet editSet = new LockEditSet(existingLock, lockPath(PackageManager.Bun, packageJsonPath),
                PackageManager.Bun, editedPackageJson, edits);
        return Result.success(new BunLockPatcher().patch(editSet));
    }

    /** The versions the lock already installs, keyed by tree-slot name (an alias seeds under its slot). */
    private static Map<String, Set<String>> lockedVersionsNpm(String lock) {
        Map<String, Set<String>> locked = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : installedPackagesNpm(lock).entrySet()) {
            String key = e.getKey();
            String slot = key.substring(key.lastIndexOf("node_modules/") + "node_modules/".length());
            if (e.getValue() instanceof Map) {
                Map<?, ?> entry = (Map<?, ?>) e.getValue();
                Object version = entry.get("version");
                if (version instanceof String && !Boolean.TRUE.equals(entry.get("link"))) {
                    locked.computeIfAbsent(slot, k -> new LinkedHashSet<>()).add((String) version);
                }
            }
        }
        return locked;
    }

    /** The versions the bun.lock already installs, keyed by tree-slot name (the key's last package segment). */
    private static Map<String, Set<String>> lockedVersionsBun(String lock) {
        Map<String, Set<String>> locked = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : packagesMap(lock).entrySet()) {
            List<?> tuple = tupleOf(e.getValue());
            if (tuple != null && !tuple.isEmpty() && tuple.get(0) instanceof String) {
                String locator = (String) tuple.get(0);
                int at = locator.lastIndexOf('@');
                if (at > 0) {
                    locked.computeIfAbsent(bunSlotName(e.getKey()), k -> new LinkedHashSet<>())
                            .add(locator.substring(at + 1));
                }
            }
        }
        return locked;
    }

    /** The package name a bun {@code packages} key addresses: its last path segment, {@code @scope/}-aware. */
    private static String bunSlotName(String key) {
        int lastSlash = key.lastIndexOf('/');
        if (lastSlash < 0) {
            return key;
        }
        int prevSlash = key.lastIndexOf('/', lastSlash - 1);
        String tail = key.substring(prevSlash + 1);
        return tail.startsWith("@") ? tail : key.substring(lastSlash + 1);
    }

    /**
     * Whether the project has more than the single root importer the resolver reproduces. A workspace root manifest
     * declares {@code workspaces}; a member edit carries a multi-importer root lock (pnpm {@code importers}, npm/bun
     * workspace importer entries, berry {@code @workspace:} headers). yarn.lock is flat and cannot be told apart, so
     * a manifest in a sub-directory is treated as a member conservatively.
     */
    private static boolean isMultiPackageProject(PackageManager pm, String editedManifest, String lock,
                                                 @Nullable Path packageJsonPath) {
        try {
            if (declaresWorkspaces(editedManifest) || multiImporterLock(pm, lock)) {
                return true;
            }
        } catch (RuntimeException ignored) {
            // Unparseable here: let the resolver's own gates decide rather than misclassify.
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
            case Npm:
                return jsonImporterCount(lock, "packages", true) > 1;
            case Bun:
                return jsonImporterCount(lock, "workspaces", false) > 1;
            case Pnpm:
                return pnpmImporterKeys(lock).size() > 1;
            case YarnBerry:
                return berryWorkspaceHeaderCount(lock) > 1;
            default:
                return false; // yarn classic: flat lock, no importer sections to count
        }
    }

    /** Count importer entries in a JSON lock's {@code section} map (npm skips nested {@code node_modules/} keys). */
    private static int jsonImporterCount(String lock, String section, boolean skipNodeModules) {
        Object entries = parseJsonObject(lock, false).get(section);
        if (!(entries instanceof Map)) {
            return 0;
        }
        int count = 0;
        for (Map.Entry<?, ?> e : ((Map<?, ?>) entries).entrySet()) {
            if (e.getValue() instanceof Map && !(skipNodeModules && String.valueOf(e.getKey()).contains("node_modules/"))) {
                count++;
            }
        }
        return count;
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
                // A fresh optional or peer declaration marks flags and scopes across the tree (npm 7+ even
                // auto-installs a missing peer); only whole-closure resolution reproduces that.
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
            return Collections.singletonList(LockEditSet.PackageEdit.builder()
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

        boolean npmForkRescoped = false;
        if (pm == PackageManager.Npm && lockedVersions.size() > 1) {
            // The npm analogue: npm hoists a root-declared direct dep to the top-level node_modules/<name>, so
            // the same package forked (nested under a reverse-dependent) at another version is unambiguous at the
            // importer edge. Re-scope the bump to that top-level version; the nested forks this direct bump does
            // not touch stay byte-identical. A member/transitive fork (empty here) stays fail-loud below.
            Set<String> importerVersions = importerResolvedVersionsNpm(existingLock, name, change.oldConstraint);
            if (importerVersions.size() == 1) {
                lockedVersions = importerVersions;
                npmForkRescoped = true;
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
        if ((pm == PackageManager.YarnBerry || pm == PackageManager.Bun) &&
                importersDeclaring(pm, existingLock, name, change.oldConstraint) > 1) {
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
            return Collections.singletonList(edit.build());
        }

        if (npmForkRescoped) {
            // The re-scoped top-level bump is byte-safe only while every nested fork stays a fork; a fork whose
            // requirer would accept the bumped target dedupes up into the new top-level (a reshape). Defer those.
            requireForksStayForkedNpm(name, oldVersion, targetVersion, existingLock);
        }

        // A reverse-dependent whose recorded constraint excludes the new version keeps the old version nested
        // under itself; the safe single-leaf slice resolves it, everything else defers.
        List<LockEditSet.PackageEdit> nestEdits;
        if (pm == PackageManager.Npm) {
            nestEdits = planReverseDependentNestsNpm(name, oldVersion, targetVersion, existingLock, registries, client);
        } else if (pm == PackageManager.Bun) {
            nestEdits = planReverseDependentNestsBun(name, oldVersion, targetVersion, existingLock, registries, client);
        } else if (pm == PackageManager.Pnpm) {
            // pnpm never nests; a reverse-dependent that keeps the old version becomes a content-fork that
            // replaces the whole bump (add the new content, retain the old), so it returns early when it fires.
            List<LockEditSet.PackageEdit> fork =
                    planContentForkPnpm(name, oldVersion, targetVersion, change, existingLock, registries, client);
            if (fork != null) {
                return fork;
            }
            nestEdits = Collections.emptyList();
        } else {
            proveReverseDependentsAccept(pm, name, oldVersion, targetVersion, existingLock);
            nestEdits = Collections.emptyList();
        }

        NodeRegistry registry = registries.registryFor(name);
        VersionManifest oldManifest = client.getManifest(registry, name, oldVersion);
        VersionManifest newManifest = client.getManifest(registry, name, targetVersion);

        // Every closure surface but `dependencies` must be unchanged; a `dependencies` delta no longer fails
        // loud outright, it seeds the greedy-forward cascade below. A peer-surface delta whose already-installed
        // providers still satisfy the new ranges is written through rather than deferred (npm only).
        boolean depsEqual = dependenciesEqual(oldManifest, newManifest);
        LockEditSet.EntryMetadata surfaces =
                proveSurfacesAndWriteThrough(pm, name, oldManifest, newManifest, existingLock, depsEqual);

        VersionManifest.Dist dist = newManifest.getDist();
        // A dropped `dependencies` edge (present in the old manifest, gone in the new) orphan-prunes rather than
        // fails loud: the patcher removes the edge and GCs whatever it leaves unreachable (npm, pnpm v9, both yarns, bun).
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
            if (pm == PackageManager.Npm) {
                CascadeResult cr = cascadeForcedMoves(name, newManifest,
                        "devDependencies".equals(change.scope), existingLock, registries, client);
                edits.addAll(cr.edits);
                if (cr.rootAddsEdges) {
                    // The bumped root's entry gains new dependency edges whose subtrees were placed above;
                    // tell the patcher to graft the full new dependencies map rather than fail loud.
                    edits.set(0, rootEdit.toBuilder().addsDependencyEdges(true).build());
                }
            } else if (pm == PackageManager.Pnpm) {
                edits.addAll(cascadeForcedMovesPnpm(name, oldVersion, newManifest, existingLock, registries, client));
            } else if (pm == PackageManager.YarnBerry || pm == PackageManager.YarnClassic) {
                edits.addAll(cascadeForcedMovesYarn(pm, name, oldManifest, newManifest, existingLock, registries, client));
            } else if (pm == PackageManager.Bun) {
                edits.addAll(cascadeForcedMovesBun(name, newManifest, existingLock, registries, client));
            } else {
                // The remaining patchers cannot reshape a changed closure so far; other formats defer.
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + " dependencies changed");
            }
        }
        return edits;
    }

    /** The cascade's emitted edits, plus whether the bumped root's own {@code dependencies} map gained new edges. */
    private static final class CascadeResult {
        final List<LockEditSet.PackageEdit> edits;
        final boolean rootAddsEdges;

        CascadeResult(List<LockEditSet.PackageEdit> edits, boolean rootAddsEdges) {
            this.edits = edits;
            this.rootAddsEdges = rootAddsEdges;
        }
    }

    /**
     * A direct-dependency bump whose new {@code dependencies} edges force a currently-locked transitive to move.
     * Each forced transitive resolves over the union of all live constraints on it (the bumped dep's new
     * constraint substituted for its stale lock entry). When a moved transitive's own target ALSO changes ITS
     * {@code dependencies} (a multi-level cascade), those changed edges seed the next wave via the same worklist,
     * each wave byte-exact-or-fail-loud. A brand-new transitive the root introduces (add-during-bump) resolves and
     * hoists its own fresh subtree via {@link #placeAddedTransitivesNpm}; only the root can introduce one (a deeper
     * mover's edge set must be identical, else it already fails loud). Any move that would reshape fails loud:
     * union unsatisfiable (npm would fork/nest), an added/dropped edge mid-cascade, or a transitive re-touched
     * across waves (an earlier union missed a requirer).
     */
    private static CascadeResult cascadeForcedMoves(String rootName, VersionManifest rootNew, boolean dev,
                                                    String existingLock, NodeRegistries registries,
                                                    NpmRegistryClient client) {
        // A dropped edge is handled by the patcher's orphan GC (flagged via prunesOrphans); this loop only
        // re-resolves kept edges. Each resolves over the actual installed tree via npm's hoisting walk, so an
        // edge already satisfied by a nested copy is a no-op.
        Map<String, Object> installed = installedPackagesNpm(existingLock);

        // Every mover's new constraint map, seeded with the bumped root and grown by each cascade wave. It lets a
        // shared transitive's union substitute EVERY moved requirer's new constraint for its stale lock value.
        Map<String, Map<String, String>> movedNewDeps = new LinkedHashMap<>();
        movedNewDeps.put(rootName, rootNew.getDependencies() == null ?
                Collections.emptyMap() : rootNew.getDependencies());
        Deque<String> worklist = new ArrayDeque<>();
        worklist.add(rootName);

        List<LockEditSet.PackageEdit> moves = new ArrayList<>();
        Set<String> movedDeps = new LinkedHashSet<>();
        Map<String, String> newTransitives = new LinkedHashMap<>();
        while (!worklist.isEmpty()) {
            String moverName = worklist.poll();
            String moverKey = "node_modules/" + moverName;
            for (Map.Entry<String, String> e : movedNewDeps.get(moverName).entrySet()) {
                String dep = e.getKey();
                String constraint = e.getValue();
                String resolvedKey = NpmLockPatcher.resolveFrom(installed.keySet(), moverKey, dep);
                String cur = installedVersion(installed, resolvedKey);
                if (cur == null) {
                    // A brand-new transitive. Only the initially-bumped root can introduce one; a deeper mover's
                    // edge set must be identical (resolveForcedMove fails loud otherwise), so this never fires for
                    // a mover. Collect it; its fresh subtree is resolved+hoisted after the move waves settle.
                    if (!moverName.equals(rootName)) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                                "upgrading " + moverName + " introduces new transitive " + dep +
                                        " (add-during-bump) not yet supported");
                    }
                    newTransitives.put(dep, constraint);
                    continue;
                }
                if (isUnsupportedProtocol(constraint) || !NodeSemver.validRange(constraint)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                            dep + " is constrained by an unresolvable range: " + constraint);
                }
                if (NodeSemver.satisfies(cur, constraint)) {
                    continue; // kept edge already satisfied by the installed version (constraint re-pin only)
                }
                if (!resolvedKey.equals("node_modules/" + dep)) {
                    // A nested copy that no longer satisfies would move within its own subtree; the mover path
                    // assumes a top-level entry, so defer rather than risk a non-byte-exact tree.
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                            "moving nested " + dep + " to satisfy the upgraded " + moverName +
                                    " constraint is not yet supported");
                }
                if (!movedDeps.add(dep)) {
                    // A transitive forced by more than one wave means an earlier union missed a requirer; the
                    // second resolve would not be byte-safe. Defer.
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                            dep + " is forced to move by more than one cascade wave; deferred");
                }
                ForcedMove move = resolveForcedMove(dep, cur, movedNewDeps, existingLock, registries, client);
                moves.add(move.edit);
                if (move.cascadeDeps != null) {
                    // The moved transitive's target changes its own edges: seed the next wave.
                    movedNewDeps.put(dep, move.cascadeDeps);
                    worklist.add(dep);
                }
            }
        }
        if (!newTransitives.isEmpty()) {
            moves.addAll(placeAddedTransitivesNpm(newTransitives, dev, installed, movedNewDeps.keySet(),
                    existingLock, registries, client));
        }
        return new CascadeResult(moves, !newTransitives.isEmpty());
    }

    /**
     * Resolve and hoist the fresh subtree(s) a bump pulls into the closure (add-during-bump), reusing npm's
     * hoisting placement. Maximally conservative: every subtree member must land at a free top-level
     * {@code node_modules/<name>} slot — a name that already sits anywhere in the tree, that the cascade moves,
     * or that is required at two incompatible versions within the added subtree, all fail loud (dedupe/nest/fork
     * are left to a full resolver). Each placed member is emitted as a fresh {@code ADD}.
     */
    private static List<LockEditSet.PackageEdit> placeAddedTransitivesNpm(Map<String, String> seeds, boolean dev,
                                                                          Map<String, Object> installed,
                                                                          Set<String> cascadeNames, String existingLock,
                                                                          NodeRegistries registries,
                                                                          NpmRegistryClient client) {
        Map<String, String> existingTop = topLevelVersionsNpm(existingLock);
        Map<String, Placement> placed = new LinkedHashMap<>();
        Deque<Requirement> queue = new ArrayDeque<>();
        for (Map.Entry<String, String> e : seeds.entrySet()) {
            queue.add(new Requirement(e.getKey(), e.getValue(), dev));
        }
        while (!queue.isEmpty()) {
            Requirement req = queue.poll();
            if (cascadeNames.contains(req.name)) {
                // The added subtree needs a package the same bump is moving; the two interact, defer.
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, req.name,
                        "the added transitive subtree needs " + req.name + " which the bump also moves; deferred");
            }
            String existingVersion = existingTop.get(req.name);
            if (existingVersion != null) {
                if (existingSatisfies(existingVersion, req.constraint)) {
                    continue; // dedup to the stable top-level pin the bump does not touch
                }
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, req.name, req.name + " is installed at " +
                        existingVersion + " which does not satisfy the added " + req.constraint +
                        " (npm would nest/move it); deferred");
            }
            if (installedAnywhereNpm(installed, req.name)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, req.name,
                        req.name + " already exists nested in the tree; hoisting the added copy is not yet supported");
            }
            Placement already = placed.get(req.name);
            if (already != null) {
                if (existingSatisfies(already.version, req.constraint)) {
                    continue;
                }
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, req.name, req.name +
                        " is required at two incompatible versions within the added subtree (" +
                        already.version + " vs " + req.constraint + "); deferred");
            }

            NodeRegistry registry = registries.registryFor(req.name);
            String version = resolveAddedVersion(client, registry, req.name, req.constraint);
            VersionManifest manifest = client.getManifest(registry, req.name, version);
            requireEmittableClosureMember(req.name, manifest);
            VersionManifest.Dist dist = manifest.getDist();
            if (dist == null || dist.getTarball() == null || dist.getIntegrity() == null) {
                throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, req.name,
                        req.name + "@" + version + " has no registry tarball/integrity");
            }
            // A stray reverse-dependent whose recorded constraint excludes the resolved version fails loud.
            proveReverseDependentsAccept(PackageManager.Npm, req.name, version, version, existingLock);

            placed.put(req.name, new Placement(version, manifest, req.dev));
            enqueueDeps(queue, manifest.getDependencies(), req.dev, req.name);
            enqueueDeps(queue, manifest.getOptionalDependencies(), req.dev, req.name);
        }

        List<LockEditSet.PackageEdit> edits = new ArrayList<>();
        for (Map.Entry<String, Placement> e : placed.entrySet()) {
            Placement p = e.getValue();
            VersionManifest.Dist dist = Objects.requireNonNull(p.manifest.getDist());
            edits.add(LockEditSet.PackageEdit.builder()
                    .name(e.getKey())
                    .oldVersion("")
                    .newVersion(p.version)
                    .newResolved(dist.getTarball())
                    .newIntegrity(dist.getIntegrity())
                    .newShasum(dist.getShasum())
                    .newDependencies(notEmpty(p.manifest.getDependencies()) ? p.manifest.getDependencies() : null)
                    .scope(p.dev ? "devDependencies" : "dependencies")
                    .importerDir(null)
                    .kind(ADD)
                    .metadata(leafMetadata(p.manifest, p.dev))
                    .build());
        }
        return edits;
    }

    /** Whether {@code name} is installed at any depth ({@code node_modules/<name>} top-level or nested). */
    private static boolean installedAnywhereNpm(Map<String, Object> installed, String name) {
        String suffix = "node_modules/" + name;
        for (String key : installed.keySet()) {
            if (key.equals(suffix) || key.endsWith("/" + suffix)) {
                return true;
            }
        }
        return false;
    }

    /** A forced move's emitted edit, plus the mover's changed edges when its target seeds a deeper cascade wave. */
    private static final class ForcedMove {
        final LockEditSet.PackageEdit edit;
        final @Nullable Map<String, String> cascadeDeps;

        ForcedMove(LockEditSet.PackageEdit edit, @Nullable Map<String, String> cascadeDeps) {
            this.edit = edit;
            this.cascadeDeps = cascadeDeps;
        }
    }

    /**
     * Resolve and emit the move of a single forced transitive, failing loud on any reshape. When the target
     * changes the transitive's OWN {@code dependencies} (same edge set, changed constraints), the changed edges
     * are returned to seed the next cascade wave; an added/dropped edge reshapes the tree and fails loud.
     */
    private static ForcedMove resolveForcedMove(String dep, String oldVersion,
                                                Map<String, Map<String, String>> movedNewDeps, String existingLock,
                                                NodeRegistries registries, NpmRegistryClient client) {
        // A transitive that is also directly declared would need its importer declaration reconciled too.
        if (importerDeclaresNpm(existingLock, dep)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                    dep + " is directly declared; moving it via cascade is not yet supported");
        }

        Set<String> union = liveConstraintsNpm(existingLock, dep, movedNewDeps);
        NodeRegistry registry = registries.registryFor(dep);
        Set<String> published = client.getPackument(registry, dep).getVersions();
        String target = maxSatisfyingAll(published, union);
        if (target == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                    "no single version of " + dep + " satisfies all requirers " + union +
                            " (npm would fork/nest; deferred)");
        }
        if (target.equals(oldVersion)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                    dep + " resolves back to its locked version under the union; deferred");
        }

        VersionManifest oldManifest = client.getManifest(registry, dep, oldVersion);
        VersionManifest newManifest = client.getManifest(registry, dep, target);
        // A mover must be a clean bump on every non-`dependencies` surface; a `dependencies` delta seeds the next
        // wave, but only when the edge SET is identical (an added/dropped edge mid-cascade would place/GC a
        // subtree — npm reshapes, so it defers).
        proveNonDependencySurfacesUnchanged(dep, oldManifest, newManifest);
        Map<String, String> cascadeDeps = null;
        if (!dependenciesEqual(oldManifest, newManifest)) {
            Map<String, String> oldDeps = oldManifest.getDependencies() == null ?
                    Collections.emptyMap() : oldManifest.getDependencies();
            Map<String, String> newDeps = newManifest.getDependencies() == null ?
                    Collections.emptyMap() : newManifest.getDependencies();
            if (!oldDeps.keySet().equals(newDeps.keySet())) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                        "moving " + dep + " to " + target + " adds/drops a dependency edge " +
                                "(mid-cascade reshape) not yet supported");
            }
            cascadeDeps = newManifest.getDependencies();
        }

        VersionManifest.Dist dist = newManifest.getDist();
        if (dist == null || dist.getTarball() == null || dist.getIntegrity() == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, dep,
                    dep + "@" + target + " has no registry tarball/integrity");
        }
        LockEditSet.PackageEdit edit = LockEditSet.PackageEdit.builder()
                .name(dep)
                .oldVersion(oldVersion)
                .newVersion(target)
                .newResolved(dist.getTarball())
                .newIntegrity(dist.getIntegrity())
                .newShasum(dist.getShasum())
                .newDependencies(newManifest.getDependencies())
                .newOptionalDependencies(newManifest.getOptionalDependencies())
                .metadata(writeThrough(PackageManager.Npm, oldManifest, newManifest))
                .scope("dependencies")
                .importerDir(null)
                .kind(FORCED_MOVE)
                .build();
        return new ForcedMove(edit, cascadeDeps);
    }

    /**
     * Every recorded constraint on {@code dep} across the lock's installed entries, with each moved requirer's new
     * constraint substituted for its stale lock entry ({@code movedNewDeps} holds the bumped root plus every
     * cascade mover). If no version satisfies all of them, npm would fork/nest.
     */
    private static Set<String> liveConstraintsNpm(String lock, String dep,
                                                  Map<String, Map<String, String>> movedNewDeps) {
        Set<String> constraints = new LinkedHashSet<>();
        Map<String, Object> root = parseJsonObject(lock, false);
        Object packages = root.get("packages");
        if (packages instanceof Map) {
            String prefix = "node_modules/";
            for (Map.Entry<?, ?> e : ((Map<?, ?>) packages).entrySet()) {
                String key = String.valueOf(e.getKey());
                int nm = key.lastIndexOf(prefix);
                if (nm < 0 || !(e.getValue() instanceof Map)) {
                    continue; // importer entries: dep is not importer-declared (guarded above)
                }
                if (movedNewDeps.containsKey(key.substring(nm + prefix.length()))) {
                    continue; // a mover: its lock constraint is stale, its new one is substituted below
                }
                Map<?, ?> entry = (Map<?, ?>) e.getValue();
                addConstraintOn(constraints, entry.get("dependencies"), dep);
                addConstraintOn(constraints, entry.get("optionalDependencies"), dep);
                addConstraintOn(constraints, entry.get("peerDependencies"), dep);
            }
        }
        for (Map<String, String> newDeps : movedNewDeps.values()) {
            String rootConstraint = newDeps.get(dep);
            if (rootConstraint != null) {
                constraints.add(rootConstraint);
            }
        }
        return constraints;
    }

    private static void addConstraintOn(Set<String> constraints, @Nullable Object scopeMap, String dep) {
        if (scopeMap instanceof Map) {
            Object c = ((Map<?, ?>) scopeMap).get(dep);
            if (c instanceof String) {
                constraints.add((String) c);
            }
        }
    }

    /** The highest published version satisfying every constraint, or {@code null} if none does. */
    private static @Nullable String maxSatisfyingAll(Set<String> published, Set<String> constraints) {
        String best = null;
        for (String candidate : published) {
            boolean ok = true;
            for (String c : constraints) {
                if (!NodeSemver.satisfies(candidate, c)) {
                    ok = false;
                    break;
                }
            }
            if (ok && (best == null || NodeSemver.compare(candidate, best) > 0)) {
                best = candidate;
            }
        }
        return best;
    }

    private static boolean importerDeclaresNpm(String lock, String dep) {
        Map<String, Object> root = parseJsonObject(lock, false);
        Object packages = root.get("packages");
        if (packages instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) packages).entrySet()) {
                String key = String.valueOf(e.getKey());
                if (key.contains("node_modules/") || !(e.getValue() instanceof Map)) {
                    continue; // importer entries only
                }
                if (declaredConstraintIn((Map<?, ?>) e.getValue(), dep, DECLARED_SCOPES) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The pnpm counterpart of {@link #cascadeForcedMoves}. pnpm records resolved versions in
     * {@code snapshots.<root>@<v>.dependencies}, not constraints, so the transitive's current version is read
     * from the bumped root's own snapshot and the reverse-edge safety is a single-requirer check: a shared
     * transitive, whose other requirers' ranges are not in the lock, fails loud rather than risk a fork.
     */
    private static List<LockEditSet.PackageEdit> cascadeForcedMovesPnpm(String rootName, String rootOldVersion,
                                                                        VersionManifest rootNew, String existingLock,
                                                                        NodeRegistries registries, NpmRegistryClient client) {
        Map<String, String> newDeps = rootNew.getDependencies() == null ?
                Collections.emptyMap() : rootNew.getDependencies();

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
            if (isUnsupportedProtocol(constraint) || !NodeSemver.validRange(constraint)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                        dep + " is constrained by an unresolvable range: " + constraint);
            }
            if (!NodeSemver.satisfies(cur, constraint)) {
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
        String target = maxSatisfyingAll(published, Collections.singleton(newConstraint));
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
                .metadata(writeThrough(PackageManager.Pnpm, oldManifest, newManifest))
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
        if (refConstraint == null || !NodeSemver.validRange(refConstraint) ||
                NodeSemver.satisfies(targetVersion, refConstraint)) {
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

        return Collections.singletonList(LockEditSet.PackageEdit.builder()
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
            if ((ownerKey != null && key.equals(ownerKey)) || !(e.getValue() instanceof Map)) {
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
     * packument, then its dependencies are walked transitively; each closure member either hoists to a fresh
     * top-level {@code node_modules/<name>} entry or dedups to an already-satisfying pin. Any placement that
     * would move, nest, or fork an already-placed package fails loud (an incompatible top-level pin, a member
     * needed at two versions, an excluding reverse-dependent, or an unverified metadata surface).
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
        if (pm == PackageManager.Pnpm) {
            // pnpm is content-addressed: placement is mechanical (one packages+snapshots entry per closure
            // member), but every closure member must be brand-new (no dedupe/conflict) and peer-free.
            return resolveClosureAddPnpm(change, rootName, rootConstraint, existingLock, memberImporterDir, registries, client);
        }
        if (pm == PackageManager.Bun) {
            // bun hoists like npm: the closure resolves identically, one packages tuple per member, failing
            // loud on any conflict/nest (bun's parent/name fork keys).
            return resolveClosureAddBun(change, rootName, rootConstraint, existingLock, registries, client);
        }
        if (pm == PackageManager.YarnClassic || pm == PackageManager.YarnBerry) {
            // yarn lists one entry per resolved (name, version); placement is not hoisted, so every closure
            // member must be brand-new (no merge/second entry for an existing name). Berry adds get their
            // checksums enriched afterward; scoped members (leaf or clean closure) are byte-exact.
            return resolveClosureAddYarn(pm, change, rootName, rootConstraint, existingLock, registries, client);
        }
        // Only the npm patcher can insert closure entries so far; other formats defer.
        if (pm != PackageManager.Npm) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, rootName, "adding " + rootName + " requires resolution");
        }

        Map<String, String> existingTopLevel = topLevelVersionsNpm(existingLock);
        boolean dev = "devDependencies".equals(change.scope);

        Map<String, Placement> placed = new LinkedHashMap<>();
        Map<String, NestedPlacement> nested = new LinkedHashMap<>();
        LockEditSet.PackageEdit promotion = null;
        Deque<Requirement> queue = new ArrayDeque<>();
        queue.add(new Requirement(rootName, rootConstraint, dev));

        while (!queue.isEmpty()) {
            Requirement req = queue.poll();

            String existingVersion = existingTopLevel.get(req.name);
            if (existingVersion != null) {
                if (req.name.equals(rootName)) {
                    // Already installed as a transitive: npm reuses that install (writing only the importer edge,
                    // clearing "dev" on a dev->prod promotion) when its version equals what the constraint resolves to.
                    NodeRegistry registry = registries.registryFor(rootName);
                    String resolved = resolveAddedVersion(client, registry, rootName, rootConstraint);
                    if (!resolved.equals(existingVersion)) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, rootName, rootName + " is installed at " +
                                existingVersion + " but the added " + rootConstraint + " resolves to " + resolved +
                                " (promotion would change the version); deferred");
                    }
                    promotion = promotionEdit(rootName, change.scope, dev, existingLock);
                    continue;
                }
                // Dedup to the already-placed pin, or nest the required version under its parent where an
                // incompatible version holds the top slot.
                if (existingSatisfies(existingVersion, req.constraint)) {
                    continue;
                }
                nestUnderParent(PackageManager.Npm, req, existingVersion, placed, nested, existingLock, registries, client);
                continue;
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
            requireEmittableClosureMember(req.name, manifest);

            VersionManifest.Dist dist = manifest.getDist();
            if (dist == null || dist.getTarball() == null || dist.getIntegrity() == null) {
                throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, req.name,
                        req.name + "@" + version + " has no registry tarball/integrity");
            }

            // A stray reverse-dependent whose recorded constraint excludes the resolved version fails loud.
            proveReverseDependentsAccept(pm, req.name, version, version, existingLock);

            placed.put(req.name, new Placement(version, manifest, req.dev));

            enqueueDeps(queue, manifest.getDependencies(), req.dev, req.name);
            enqueueDeps(queue, manifest.getOptionalDependencies(), req.dev, req.name);
        }

        List<LockEditSet.PackageEdit> edits = new ArrayList<>();
        for (Map.Entry<String, Placement> e : placed.entrySet()) {
            Placement p = e.getValue();
            VersionManifest.Dist dist = Objects.requireNonNull(p.manifest.getDist());
            // Only the root writes an importer constraint; the patcher no-ops the transitives, absent from the
            // edited package.json. Scope carries dev-ness so a dev-rooted closure marks every fresh entry.
            edits.add(LockEditSet.PackageEdit.builder()
                    .name(e.getKey())
                    .oldVersion("")
                    .newVersion(p.version)
                    .newResolved(dist.getTarball())
                    .newIntegrity(dist.getIntegrity())
                    .newShasum(dist.getShasum())
                    .newDependencies(notEmpty(p.manifest.getDependencies()) ? p.manifest.getDependencies() : null)
                    .scope(p.dev ? "devDependencies" : "dependencies")
                    .importerDir(null)
                    .kind(ADD)
                    .metadata(leafMetadata(p.manifest, p.dev))
                    .build());
        }
        for (NestedPlacement np : nested.values()) {
            VersionManifest.Dist dist = Objects.requireNonNull(np.manifest.getDist());
            edits.add(LockEditSet.PackageEdit.builder()
                    .name(np.name)
                    .oldVersion("")
                    .newVersion(np.version)
                    .newResolved(dist.getTarball())
                    .newIntegrity(dist.getIntegrity())
                    .newShasum(dist.getShasum())
                    .scope(np.dev ? "devDependencies" : "dependencies")
                    .importerDir(null)
                    .kind(ADD)
                    .nestedUnder(np.parent)
                    .metadata(leafMetadata(np.manifest, np.dev))
                    .build());
        }
        if (promotion != null) {
            edits.add(promotion);
        }
        return edits;
    }

    /**
     * Build the promotion edit for an already-installed transitive: the install entry stays, only the importer
     * edge is written, and a dev->prod promotion clears the top-level {@code "dev": true}. A non-leaf dev->prod
     * promotion needs subtree dev-flag propagation and fails loud; a nested copy under another parent is an
     * independent placement and is left untouched.
     */
    private static LockEditSet.PackageEdit promotionEdit(String rootName, String scope, boolean dev, String existingLock) {
        Map<String, Object> installed = installedPackagesNpm(existingLock);
        String entryKey = "node_modules/" + rootName;
        Object entry = installed.get(entryKey);
        boolean existingDev = entry instanceof Map && Boolean.TRUE.equals(((Map<?, ?>) entry).get("dev"));
        boolean clearDev = existingDev && !dev;
        if (clearDev && entry instanceof Map) {
            Object deps = ((Map<?, ?>) entry).get("dependencies");
            if (deps instanceof Map && !((Map<?, ?>) deps).isEmpty()) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, rootName,
                        "promoting non-leaf dev transitive " + rootName + " to production needs dev-flag propagation; deferred");
            }
        }
        String version = installedVersion(installed, entryKey);
        LockEditSet.PackageEdit.PackageEditBuilder promotion = LockEditSet.PackageEdit.builder()
                .name(rootName)
                .oldVersion(version == null ? "" : version)
                .newVersion(version)
                .scope(scope)
                .importerDir(null)
                .kind(PROMOTION);
        if (clearDev) {
            // Exact-set flags: dev cleared, the entry's other flags carried unchanged.
            Map<?, ?> e = (Map<?, ?>) entry;
            promotion.metadata(LockEditSet.EntryMetadata.builder()
                    .flagsChanged(true)
                    .optional(Boolean.TRUE.equals(e.get("optional")) ? Boolean.TRUE : null)
                    .devOptional(Boolean.TRUE.equals(e.get("devOptional")) ? Boolean.TRUE : null)
                    .peer(Boolean.TRUE.equals(e.get("peer")) ? Boolean.TRUE : null)
                    .build());
        }
        return promotion.build();
    }

    /**
     * A closure member an incompatible top-level pin excludes: nest it at
     * {@code node_modules/<parent>/node_modules/<name>}. Safe slice only: the requirer is freshly placed, some
     * requirer pins the top-level version exactly (so npm/bun fork rather than cascade), and the nested version is a leaf.
     */
    private static void nestUnderParent(PackageManager pm, Requirement req, String existingVersion,
                                        Map<String, Placement> placed, Map<String, NestedPlacement> nested,
                                        String existingLock, NodeRegistries registries, NpmRegistryClient client) {
        boolean bun = pm == PackageManager.Bun;
        if (req.parent == null || !placed.containsKey(req.parent) ||
                !(bun ? topLevelPinnedBun(existingLock, req.name, existingVersion) :
                        topLevelPinnedNpm(existingLock, req.name, existingVersion))) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, req.name, req.name + " is already placed at " +
                    existingVersion + " which does not satisfy " + req.constraint +
                    " (" + (bun ? "bun" : "npm") + " would nest/move it; deferred)");
        }
        String nestKey = req.parent + "/" + req.name;
        NestedPlacement already = nested.get(nestKey);
        if (already != null) {
            if (existingSatisfies(already.version, req.constraint)) {
                return;
            }
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, req.name,
                    req.name + " is required at two incompatible versions under " + req.parent + "; deferred");
        }

        NodeRegistry registry = registries.registryFor(req.name);
        String version = resolveAddedVersion(client, registry, req.name, req.constraint);
        VersionManifest manifest = client.getManifest(registry, req.name, version);
        if (notEmpty(manifest.getDependencies()) || notEmpty(manifest.getOptionalDependencies())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, req.name,
                    "nesting " + req.name + " under " + req.parent + " pulls its own transitives (non-leaf); deferred");
        }
        if (bun) {
            requireEmittableBunClosureMember(req.name, manifest);
        } else {
            requireEmittableClosureMember(req.name, manifest);
        }
        VersionManifest.Dist dist = manifest.getDist();
        if (dist == null || dist.getIntegrity() == null || (!bun && dist.getTarball() == null)) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, req.name,
                    req.name + "@" + version + " has no registry " + (bun ? "integrity" : "tarball/integrity"));
        }
        nested.put(nestKey, new NestedPlacement(req.parent, req.name, version, manifest, req.dev));
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
            enqueueDeps(queue, manifest.getDependencies(), req.dev, req.name);
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
     * A direct dependency the recipe added, plus its runtime closure, into a {@code bun.lock}. bun hoists like
     * npm, so the closure resolution matches {@link #resolveClosureAdd}, failing loud on any placement that would
     * move/nest/fork an existing entry. bun records only the integrity (no tarball URL), and the emittable gate is
     * tighter than npm's: only a deps-or-empty tuple is byte-verified, so any bin/os/cpu/peer/optional metadata or
     * scoped name defers.
     */
    private static List<LockEditSet.PackageEdit> resolveClosureAddBun(DepChange change, String rootName,
                                                                      String rootConstraint, String existingLock,
                                                                      NodeRegistries registries, NpmRegistryClient client) {
        Map<String, String> existingTopLevel = topLevelVersionsBun(existingLock);
        boolean dev = "devDependencies".equals(change.scope);

        // Promoting a transitive to a direct dep: bun keys packages by name, so a satisfying locked version just
        // gains the importer edge (its tuple is untouched). A non-satisfying version would fork/upgrade — defer.
        String promoteVersion = existingTopLevel.get(rootName);
        if (promoteVersion != null) {
            if (!existingSatisfies(promoteVersion, rootConstraint)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, rootName, rootName + " is locked at " +
                        promoteVersion + " which excludes " + rootConstraint + " (bun would fork); deferred");
            }
            return Collections.singletonList(LockEditSet.PackageEdit.builder()
                    .name(rootName)
                    .newConstraint(rootConstraint)
                    .scope(change.scope)
                    .kind(PROMOTION)
                    .build());
        }

        Map<String, Placement> placed = new LinkedHashMap<>();
        Map<String, NestedPlacement> nested = new LinkedHashMap<>();
        Deque<Requirement> queue = new ArrayDeque<>();
        queue.add(new Requirement(rootName, rootConstraint, dev));

        while (!queue.isEmpty()) {
            Requirement req = queue.poll();

            String existingVersion = existingTopLevel.get(req.name);
            if (existingVersion != null) {
                if (existingSatisfies(existingVersion, req.constraint)) {
                    continue;
                }
                nestUnderParent(PackageManager.Bun, req, existingVersion, placed, nested, existingLock, registries, client);
                continue;
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
            requireEmittableBunClosureMember(req.name, manifest);

            VersionManifest.Dist dist = manifest.getDist();
            if (dist == null || dist.getIntegrity() == null) {
                throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, req.name,
                        req.name + "@" + version + " has no registry integrity");
            }

            // A stray reverse-dependent whose recorded constraint excludes the resolved version fails loud.
            proveReverseDependentsAccept(PackageManager.Bun, req.name, version, version, existingLock);

            placed.put(req.name, new Placement(version, manifest, req.dev));
            enqueueDeps(queue, manifest.getDependencies(), req.dev, req.name);
        }

        List<LockEditSet.PackageEdit> edits = new ArrayList<>();
        for (Map.Entry<String, Placement> e : placed.entrySet()) {
            Placement p = e.getValue();
            VersionManifest.Dist dist = Objects.requireNonNull(p.manifest.getDist());
            // Only the root (declared in package.json) writes a workspace constraint; the patcher no-ops the
            // transitives because they are absent from the edited manifest. Scope carries the dev-ness.
            edits.add(LockEditSet.PackageEdit.builder()
                    .name(e.getKey())
                    .oldVersion("")
                    .newVersion(p.version)
                    .newIntegrity(dist.getIntegrity())
                    .newDependencies(notEmpty(p.manifest.getDependencies()) ? p.manifest.getDependencies() : null)
                    .scope(p.dev ? "devDependencies" : "dependencies")
                    .importerDir(null)
                    .kind(ADD)
                    .build());
        }
        for (NestedPlacement np : nested.values()) {
            VersionManifest.Dist dist = Objects.requireNonNull(np.manifest.getDist());
            edits.add(LockEditSet.PackageEdit.builder()
                    .name(np.name)
                    .oldVersion("")
                    .newVersion(np.version)
                    .newIntegrity(dist.getIntegrity())
                    .scope(np.dev ? "devDependencies" : "dependencies")
                    .importerDir(null)
                    .kind(ADD)
                    .nestedUnder(np.parent)
                    .build());
        }
        return edits;
    }


    /** Whether a bun workspace edge or installed tuple pins {@code name} at exactly {@code version}. */
    private static boolean topLevelPinnedBun(String lock, String name, String version) {
        Map<String, Object> root = parseJsonObject(lock, false);
        Object workspaces = root.get("workspaces");
        if (workspaces instanceof Map) {
            for (Object importer : ((Map<?, ?>) workspaces).values()) {
                if (importer instanceof Map && pinsExactly((Map<?, ?>) importer, name, version)) {
                    return true;
                }
            }
        }
        for (Object value : packagesMap(lock).values()) {
            List<?> tuple = tupleOf(value);
            if (tuple != null && tuple.size() >= 3 && tuple.get(2) instanceof Map &&
                    pinsExactly((Map<?, ?>) tuple.get(2), name, version)) {
                return true;
            }
        }
        return false;
    }

    private static boolean pinsExactly(Map<?, ?> scopes, String name, String version) {
        for (String scope : DECLARED_SCOPES) {
            Object scopeMap = scopes.get(scope);
            if (scopeMap instanceof Map) {
                Object c = ((Map<?, ?>) scopeMap).get(name);
                if (c instanceof String && version.equals(((String) c).trim())) {
                    return true;
                }
            }
        }
        return false;
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
            enqueueDeps(queue, manifest.getDependencies(), req.dev, req.name);
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
        if (!NodeSemver.satisfies(locked.iterator().next(), rootConstraint)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, rootName, rootName + " is locked at " +
                    locked.iterator().next() + " which excludes " + rootConstraint + " (yarn would fork); deferred");
        }
        return Collections.singletonList(LockEditSet.PackageEdit.builder()
                .name(rootName)
                .newConstraint(rootConstraint)
                .scope(change.scope)
                .kind(PROMOTION)
                .build());
    }

    /** The pnpm/bun/yarn add gates reject any optional or peer dependency (only npm models the optional-peer skip). */
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

    /** The single top-level version of each hoisted bun package ({@code packages["<name>"]} tuple, non-nested key). */
    private static Map<String, String> topLevelVersionsBun(String lock) {
        Map<String, String> versions = new LinkedHashMap<>();
        Map<String, Object> root = parseJsonObject(lock, false);
        Object packages = root.get("packages");
        if (packages instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) packages).entrySet()) {
                String key = String.valueOf(e.getKey());
                if (isNestedBunKey(key)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                            "cannot add into a bun.lock with nested placements: " + key);
                }
                if (e.getValue() instanceof List && !((List<?>) e.getValue()).isEmpty()) {
                    String locator = String.valueOf(((List<?>) e.getValue()).get(0));
                    int at = locator.lastIndexOf('@');
                    if (at > 0) {
                        versions.put(key, locator.substring(at + 1));
                    }
                }
            }
        }
        return versions;
    }

    /** A bun {@code packages} key nests a second copy under a dependent ({@code parent/name}); a leading {@code @scope/} is not a nest. */
    private static boolean isNestedBunKey(String key) {
        int scopeEnd = key.startsWith("@") ? key.indexOf('/') : -1;
        return key.indexOf('/', scopeEnd + 1) >= 0;
    }

    /**
     * A closure member whose bun tuple the patcher can insert byte-exactly: metadata of {@code {}} or
     * {@code { "dependencies": {...} }} only. bun records optional/peer/bin/os/cpu/libc into that object and
     * auto-installs non-optional peers, none of which the placement models, so any of them defers the add.
     */
    private static void requireEmittableBunClosureMember(String name, VersionManifest m) {
        if (name.indexOf('/') >= 0 || name.startsWith("@")) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    "adding scoped package " + name + " is not yet supported for bun adds");
        }
        requireNoOptionalOrPeer(name, m, "bun");
        String metadata = unsupportedBunMetadata(m);
        if (metadata != null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    "adding " + name + " carries " + metadata + " metadata not yet supported for bun adds");
        }
    }

    private static @Nullable String unsupportedBunMetadata(VersionManifest m) {
        if (m.getBin() != null) return "bin";
        if (notEmpty(m.getOs())) return "os";
        if (notEmpty(m.getCpu())) return "cpu";
        if (notEmpty(m.getLibc())) return "libc";
        if (notEmpty(m.getBundleDependencies())) return "bundleDependencies";
        if (bool(m.getHasInstallScript())) return "hasInstallScript";
        return null;
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

    private static void enqueueDeps(Deque<Requirement> queue, @Nullable Map<String, String> deps, boolean dev,
                                    String parent) {
        if (deps == null) {
            return;
        }
        for (Map.Entry<String, String> e : deps.entrySet()) {
            queue.add(new Requirement(e.getKey(), e.getValue(), dev, parent));
        }
    }

    /** Whether an already-placed {@code version} satisfies a new {@code constraint} (proven dedup only). */
    private static boolean existingSatisfies(String version, String constraint) {
        return NodeSemver.validRange(constraint) && NodeSemver.satisfies(version, constraint);
    }

    /** Every installed {@code node_modules/...} entry (keyed by full path), the candidate set for hoisting resolution. */
    private static Map<String, Object> installedPackagesNpm(String lock) {
        Map<String, Object> installed = new LinkedHashMap<>();
        Object packages = parseJsonObject(lock, false).get("packages");
        if (packages instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) packages).entrySet()) {
                String key = String.valueOf(e.getKey());
                if (key.contains("node_modules/")) {
                    installed.put(key, e.getValue());
                }
            }
        }
        return installed;
    }

    private static @Nullable String installedVersion(Map<String, Object> installed, @Nullable String key) {
        Object entry = key == null ? null : installed.get(key);
        if (!(entry instanceof Map)) {
            return null;
        }
        Object v = ((Map<?, ?>) entry).get("version");
        return v == null ? null : String.valueOf(v);
    }

    /** The single top-level version of each hoisted package ({@code packages["node_modules/<name>"]}). */
    private static Map<String, String> topLevelVersionsNpm(String lock) {
        Map<String, String> versions = new LinkedHashMap<>();
        Map<String, Object> root = parseJsonObject(lock, false);
        Object packages = root.get("packages");
        if (packages instanceof Map) {
            String prefix = "node_modules/";
            for (Map.Entry<?, ?> e : ((Map<?, ?>) packages).entrySet()) {
                String key = String.valueOf(e.getKey());
                if (key.startsWith(prefix) && key.indexOf('/', prefix.length()) < 0 && e.getValue() instanceof Map) {
                    Object v = ((Map<?, ?>) e.getValue()).get("version");
                    if (v != null) {
                        versions.put(key.substring(prefix.length()), String.valueOf(v));
                    }
                }
            }
        }
        return versions;
    }

    /**
     * The top-level version the root importer pins for {@code name} through a declared edge matching
     * {@code oldConstraint}. npm hoists a root-declared direct dependency to {@code node_modules/<name>}, so a
     * single value means the direct dep is unambiguous at its importer even when the package is separately forked
     * (nested under a reverse-dependent) at another version. A non-root (workspace member) declaration returns
     * empty, so a member edge stays deferred rather than re-scoped to a version it may not resolve to.
     */
    private static Set<String> importerResolvedVersionsNpm(String lock, String name, @Nullable String oldConstraint) {
        Object packages = parseJsonObject(lock, false).get("packages");
        Object rootImporter = packages instanceof Map ? ((Map<?, ?>) packages).get("") : null;
        if (!(rootImporter instanceof Map)) {
            return Collections.emptySet();
        }
        String declared = declaredConstraintIn((Map<?, ?>) rootImporter, name, DECLARED_SCOPES);
        if (declared == null || (oldConstraint != null && !oldConstraint.equals(declared))) {
            return Collections.emptySet();
        }
        String topLevel = topLevelVersionsNpm(lock).get(name);
        return topLevel == null ? Collections.emptySet() : Collections.singleton(topLevel);
    }

    private static String resolveAddedVersion(NpmRegistryClient client, NodeRegistry registry,
                                              String name, String constraint) {
        AbbreviatedPackument packument = client.getPackument(registry, name);
        if (NodeSemver.validRange(constraint)) {
            String best = NodeSemver.maxSatisfying(packument.getVersions(), constraint);
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

    /**
     * A closure member whose lock entry the npm patcher can insert byte-exactly. Its dependencies are recorded as
     * the entry's dependency map (v2 {@code requires}), and the scalar/array metadata tier plus
     * {@code peerDependencies}/{@code peerDependenciesMeta} is written through verbatim. An optional peer is
     * skipped (npm does not auto-install it); a non-optional peer npm auto-installs, {@code optionalDependencies},
     * a string {@code bin} or non-string {@code funding}, or bundled/shrinkwrap/accept/workspaces all fail loud.
     */
    private static void requireEmittableClosureMember(String name, VersionManifest m) {
        if (notEmpty(m.getOptionalDependencies())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    "adding " + name + " pulls in optionalDependencies not yet supported for native adds");
        }
        Set<String> autoInstalled = nonOptionalPeers(m);
        if (!autoInstalled.isEmpty()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    "adding " + name + " declares non-optional peerDependencies " + autoInstalled +
                            " (peer auto-install) not yet supported for native adds");
        }
        String metadata = unserializableMetadata(m);
        if (metadata != null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    "adding " + name + " carries " + metadata + " metadata not yet supported for native adds");
        }
    }

    /** The peers npm would auto-install: declared in {@code peerDependencies} and not marked optional. */
    private static Set<String> nonOptionalPeers(VersionManifest m) {
        Map<String, String> peers = m.getPeerDependencies();
        if (peers == null || peers.isEmpty()) {
            return Collections.emptySet();
        }
        JsonNode meta = m.getPeerDependenciesMeta();
        Set<String> result = new LinkedHashSet<>();
        for (String peer : peers.keySet()) {
            JsonNode entry = meta == null ? null : meta.get(peer);
            if (entry == null || !entry.path("optional").asBoolean(false)) {
                result.add(peer);
            }
        }
        return result;
    }

    /** The metadata surfaces whose byte-exact npm serialization is not yet verified; each defers the add. */
    private static @Nullable String unserializableMetadata(VersionManifest m) {
        if (m.getBin() != null && !m.getBin().isObject()) return "non-object bin";
        if (m.getFunding() != null && !m.getFunding().isTextual()) return "non-string funding";
        if (notEmpty(m.getBundleDependencies())) return "bundleDependencies";
        if (bool(m.getHasShrinkwrap())) return "hasShrinkwrap";
        if (notEmpty(m.getAcceptDependencies())) return "acceptDependencies";
        if (m.getWorkspaces() != null) return "workspaces";
        return null;
    }

    private static LockEditSet.@Nullable EntryMetadata leafMetadata(VersionManifest m, boolean dev) {
        boolean peers = notEmpty(m.getPeerDependencies()) || nonEmptyObject(m.getPeerDependenciesMeta());
        boolean any = dev || m.getLicenseString() != null || m.getDeprecated() != null || notEmpty(m.getEngines()) ||
                notEmpty(m.getOs()) || notEmpty(m.getCpu()) || notEmpty(m.getLibc()) ||
                bool(m.getHasInstallScript()) || m.getBin() != null || m.getFunding() != null || peers;
        if (!any) {
            return null;
        }
        return LockEditSet.EntryMetadata.builder()
                .dev(dev ? Boolean.TRUE : null)
                .license(m.getLicenseString())
                .deprecated(m.getDeprecated())
                .engines(notEmpty(m.getEngines()) ? m.getEngines() : null)
                .os(notEmpty(m.getOs()) ? m.getOs() : null)
                .cpu(notEmpty(m.getCpu()) ? m.getCpu() : null)
                .libc(notEmpty(m.getLibc()) ? m.getLibc() : null)
                .hasInstallScript(bool(m.getHasInstallScript()) ? Boolean.TRUE : null)
                .bin(m.getBin())
                .funding(normalizeFunding(m.getFunding()))
                .peerDependencies(notEmpty(m.getPeerDependencies()) ? m.getPeerDependencies() : null)
                .peerDependenciesMeta(nonEmptyObject(m.getPeerDependenciesMeta()) ? m.getPeerDependenciesMeta() : null)
                .build();
    }

    private static boolean nonEmptyObject(@Nullable JsonNode node) {
        return node != null && node.isObject() && node.size() > 0;
    }

    /** npm records a string {@code funding} as {@code {url: <string>}}; object/array forms are gated out upstream. */
    private static @Nullable JsonNode normalizeFunding(@Nullable JsonNode funding) {
        if (funding == null) {
            return null;
        }
        return JSON.createObjectNode().set("url", funding);
    }

    private static boolean notEmpty(@Nullable Map<String, ?> map) {
        return map != null && !map.isEmpty();
    }

    private static boolean notEmpty(@Nullable List<?> list) {
        return list != null && !list.isEmpty();
    }

    private static String resolveTarget(NpmRegistryClient client, NodeRegistries registries,
                                        String name, String oldVersion, String newConstraint) {
        // Minimal-update: keep the already-locked version when it still satisfies the new range.
        if (NodeSemver.satisfies(oldVersion, newConstraint)) {
            return oldVersion;
        }
        NodeRegistry registry = registries.registryFor(name);
        Set<String> published = client.getPackument(registry, name).getVersions();
        String best = NodeSemver.maxSatisfying(published, newConstraint);
        if (best == null) {
            throw new EngineFailure(Reason.VERSION_NOT_FOUND, name,
                    "no published version of " + name + " satisfies " + newConstraint);
        }
        return best;
    }

    /**
     * The strict layout whitelist for a bump, minus the {@code dependencies} surface (the cascade handles that):
     * the two manifests must agree on every other closure-affecting surface; only the write-through tier
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

    /**
     * A direct-dependency bump's non-{@code dependencies} surfaces, returning the metadata written through instead
     * of deferred. The platform gates and {@code optionalDependencies} stay strict. A {@code peerDependencies} and/or
     * {@code peerDependenciesMeta} delta is written through — but only for npm, only for a pure-metadata bump (a
     * {@code dependencies} reshape defers), and only when the installed tree provably does not reshape: no peer
     * dropped (npm may GC an auto-installed provider), every peer the new version requires is already installed at a
     * satisfying version (an absent optional peer counts as satisfied), and no peer that flips to optional could let
     * npm GC its provider — each such provider is a root-anchored dependency npm never prunes. Anything else defers.
     */
    private static LockEditSet.@Nullable EntryMetadata proveSurfacesAndWriteThrough(
            PackageManager pm, String name, VersionManifest oldM, VersionManifest newM, String lock, boolean depsEqual) {
        provePlatformSurfacesUnchanged(name, oldM, newM);
        requireEqual(name, "optionalDependencies", oldM.getOptionalDependencies(), newM.getOptionalDependencies());

        boolean peerChanged = !Objects.equals(normalize(oldM.getPeerDependencies()), normalize(newM.getPeerDependencies()));
        boolean peerMetaChanged =
                !Objects.equals(normalize(oldM.getPeerDependenciesMeta()), normalize(newM.getPeerDependenciesMeta()));
        LockEditSet.EntryMetadata base = writeThrough(pm, oldM, newM);
        if (!peerChanged && !peerMetaChanged) {
            return base;
        }

        if (pm != PackageManager.Npm || !depsEqual || removesPeer(oldM, newM) || metaReferencesUndeclaredPeer(newM)) {
            requireEqual(name, "peerDependencies", oldM.getPeerDependencies(), newM.getPeerDependencies());
            requireEqual(name, "peerDependenciesMeta", oldM.getPeerDependenciesMeta(), newM.getPeerDependenciesMeta());
            return base;
        }
        requirePeerProvidersSatisfy(pm, name, newM, lock);
        requireOptionalFlipsAnchored(pm, name, oldM, newM, lock);

        LockEditSet.EntryMetadata.EntryMetadataBuilder b =
                base == null ? LockEditSet.EntryMetadata.builder() : base.toBuilder();
        if (peerChanged) {
            b.peerDependencies(notEmpty(newM.getPeerDependencies()) ? newM.getPeerDependencies() : null)
                    .peerDependenciesChanged(true);
        }
        if (peerMetaChanged) {
            JsonNode meta = newM.getPeerDependenciesMeta();
            b.peerDependenciesMeta(nonEmptyObject(meta) ? meta : null).peerDependenciesMetaChanged(true);
        }
        return b.build();
    }

    /**
     * A peer that flips from required to optional lets npm GC a provider it auto-installed only for that peer. The
     * cheap sound proof it survives is a root direct dependency: npm never prunes one. A flipped peer whose provider
     * is installed but not root-anchored defers (proving another anchor needs the full hoisting graph).
     */
    private static void requireOptionalFlipsAnchored(PackageManager pm, String name, VersionManifest oldM,
                                                     VersionManifest newM, String lock) {
        Map<String, String> newPeers = newM.getPeerDependencies();
        if (newPeers == null) {
            return;
        }
        Map<String, String> oldPeers = oldM.getPeerDependencies() == null ?
                Collections.emptyMap() : oldM.getPeerDependencies();
        JsonNode oldMeta = oldM.getPeerDependenciesMeta();
        JsonNode newMeta = newM.getPeerDependenciesMeta();
        Set<String> rootAnchored = rootDirectDependencyNames(lock);
        for (String peer : newPeers.keySet()) {
            boolean wasRequired = oldPeers.containsKey(peer) && !isOptionalPeer(oldMeta, peer);
            if (wasRequired && isOptionalPeer(newMeta, peer) && !rootAnchored.contains(peer) &&
                    !findLockedVersions(pm, lock, peer).isEmpty()) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + " peer " + peer +
                        " flips to optional and its provider is not a root dependency; npm may prune it");
            }
        }
    }

    /** Names the root importer declares directly; npm never prunes a top-level direct dependency. */
    private static Set<String> rootDirectDependencyNames(String lock) {
        Set<String> names = new LinkedHashSet<>();
        Object packages = parseJsonObject(lock, false).get("packages");
        if (packages instanceof Map) {
            Object root = ((Map<?, ?>) packages).get("");
            if (root instanceof Map) {
                for (String scope : Arrays.asList("dependencies", "devDependencies", "optionalDependencies")) {
                    Object deps = ((Map<?, ?>) root).get(scope);
                    if (deps instanceof Map) {
                        for (Object key : ((Map<?, ?>) deps).keySet()) {
                            names.add(String.valueOf(key));
                        }
                    }
                }
            }
        }
        return names;
    }

    /** Whether {@code peerDependenciesMeta} names a peer absent from {@code peerDependencies} (an unverified shape). */
    private static boolean metaReferencesUndeclaredPeer(VersionManifest m) {
        JsonNode meta = m.getPeerDependenciesMeta();
        if (meta == null || !meta.isObject()) {
            return false;
        }
        Set<String> declared = m.getPeerDependencies() == null ?
                Collections.emptySet() : m.getPeerDependencies().keySet();
        for (Iterator<String> it = meta.fieldNames(); it.hasNext(); ) {
            if (!declared.contains(it.next())) {
                return true;
            }
        }
        return false;
    }

    /** Whether the new version drops a {@code peerDependencies} name the old version declared. */
    private static boolean removesPeer(VersionManifest oldM, VersionManifest newM) {
        Map<String, String> oldPeers = oldM.getPeerDependencies();
        if (oldPeers == null || oldPeers.isEmpty()) {
            return false;
        }
        Map<String, String> newPeers = newM.getPeerDependencies() == null ?
                Collections.emptyMap() : newM.getPeerDependencies();
        for (String p : oldPeers.keySet()) {
            if (!newPeers.containsKey(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Every non-optional peer the new version declares must already be installed at a version its new range admits;
     * an optional peer (per the new {@code peerDependenciesMeta}) may be absent. A peer present at multiple versions
     * is ambiguous. Any auto-install or re-resolve fails loud — that hard tail is out of scope.
     */
    private static void requirePeerProvidersSatisfy(PackageManager pm, String name, VersionManifest newM, String lock) {
        Map<String, String> peers = newM.getPeerDependencies();
        if (peers == null) {
            return;
        }
        JsonNode meta = newM.getPeerDependenciesMeta();
        for (Map.Entry<String, String> e : peers.entrySet()) {
            String peer = e.getKey();
            String range = e.getValue();
            Set<String> installed = findLockedVersions(pm, lock, peer);
            if (installed.isEmpty()) {
                if (isOptionalPeer(meta, peer)) {
                    continue;
                }
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        name + " peer " + peer + " is not installed; auto-installing a peer is not yet supported");
            }
            if (installed.size() > 1) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + " peer " + peer +
                        " is present at multiple versions " + installed + "; cannot prove the graph is unchanged");
            }
            String v = installed.iterator().next();
            if (!(NodeSemver.validRange(range) && NodeSemver.satisfies(v, range))) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + " peer " + peer + "@" + v +
                        " does not satisfy the new range " + range + "; resolving a new provider is not yet supported");
            }
        }
    }

    private static boolean isOptionalPeer(@Nullable JsonNode meta, String peer) {
        if (meta == null) {
            return false;
        }
        JsonNode entry = meta.get(peer);
        return entry != null && entry.path("optional").asBoolean(false);
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
                Collections.emptyMap() : newM.getDependencies();
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

    /**
     * A direct-dependency bump whose new version a reverse-dependent's recorded constraint excludes. npm keeps the
     * new version at the top-level slot and nests the old version under the reverse-dependent
     * ({@code node_modules/<dependent>/node_modules/<name>}, plus the v2 legacy tree). Only the accuracy-safe slice
     * is planned (a single top-level reverse-dependent, the constraint still resolving to the locked version, a leaf
     * nested), so the patcher relocates the pre-edit entry byte-for-byte; anything wider fails loud.
     *
     * @return one nest edit when the safe slice applies, or an empty list when no reverse-dependent conflicts.
     */
    private static List<LockEditSet.PackageEdit> planReverseDependentNestsNpm(String name, String oldVersion,
                                                                              String targetVersion, String lock,
                                                                              NodeRegistries registries,
                                                                              NpmRegistryClient client) {
        Map<String, String> conflicts = conflictingReverseDependentsNpm(lock, name, targetVersion);
        if (conflicts.isEmpty()) {
            return Collections.emptyList();
        }
        if (conflicts.size() > 1) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " is required at excluding versions by " + conflicts.keySet() +
                            "; nesting more than one reverse-dependent is not yet supported");
        }
        Map.Entry<String, String> only = conflicts.entrySet().iterator().next();
        String dependentKey = only.getKey();
        String constraint = only.getValue();

        // The dependent must be a plain top-level install; a nested/forked dependent reshapes further.
        if (dependentKey.indexOf("node_modules/", 1) >= 0) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + "'s reverse-dependent " + dependentKey + " is itself nested; not yet supported");
        }
        String dependentName = dependentKey.substring("node_modules/".length());

        // The moved package's existing entry must be a plain registry leaf so relocating it byte-for-byte is
        // sound: no own dependencies to re-nest, a real registry locator, not a workspace link or dev-only.
        Map<String, Object> packages = packagesMap(lock);
        requireNestableLeaf(name, entryMap(packages.get("node_modules/" + name)), "nested copy of " + name);
        Map<?, ?> dependentEntry = entryMap(packages.get(dependentKey));
        if (dependentEntry != null && bool(asBoolean(dependentEntry.get("dev")))) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + "'s reverse-dependent " +
                    dependentName + " is a devDependency; dev nesting is not yet supported");
        }

        // The nested version must be exactly the currently-locked one, so the relocation reuses its bytes; a
        // newer in-range version would be a fresh resolve npm serializes into a new entry instead.
        NodeRegistry registry = registries.registryFor(name);
        Set<String> published = client.getPackument(registry, name).getVersions();
        String nested = maxSatisfyingAll(published, Collections.singleton(constraint));
        if (!oldVersion.equals(nested)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, dependentName + " requires " + name + "@" +
                    constraint + " which resolves to " + nested + ", not the locked " + oldVersion +
                    "; nesting a re-resolved version is not yet supported");
        }

        return Collections.singletonList(LockEditSet.PackageEdit.builder()
                .name(name)
                .oldVersion(oldVersion)
                .newVersion(oldVersion)
                .scope("dependencies")
                .kind(REVERSE_NEST)
                .nestedUnder(dependentName)
                .build());
    }

    /** Installed lock entries whose recorded constraint on {@code name} excludes {@code target}, keyed by their packages key. */
    private static Map<String, String> conflictingReverseDependentsNpm(String lock, String name, String target) {
        Map<String, Object> packages = packagesMap(lock);
        Map<String, String> conflicts = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : packages.entrySet()) {
            // Importer entries are the user's package.json, re-pinned by the patcher; never a nest trigger.
            if (!e.getKey().contains("node_modules/") || !(e.getValue() instanceof Map)) {
                continue;
            }
            String c = firstExcludingConstraint((Map<?, ?>) e.getValue(), name, target);
            // A dependent already served by its own pre-existing nested copy is a fork the bump leaves untouched,
            // not a new nest npm must create; skip it so a top-level bump of an already-forked dep stays clean.
            if (c != null && !nestedCopySatisfiesNpm(packages, e.getKey(), name, c)) {
                conflicts.put(e.getKey(), c);
            }
        }
        return conflicts;
    }

    /**
     * A re-scoped fork bump's dedupe guard: every reverse-dependent that currently forks {@code name} (its
     * recorded constraint excludes the old top-level version, so it holds its own nested copy) must also exclude
     * the bumped {@code target}. A requirer that now accepts the target would have npm dedupe its nest up into the
     * new top-level — a reshape the surgical bump cannot express — so any such case fails loud.
     */
    private static void requireForksStayForkedNpm(String name, String oldVersion, String target, String lock) {
        for (Map.Entry<String, Object> e : packagesMap(lock).entrySet()) {
            if (!e.getKey().contains("node_modules/") || !(e.getValue() instanceof Map)) {
                continue;
            }
            String c = firstExcludingConstraint((Map<?, ?>) e.getValue(), name, oldVersion);
            if (c != null && NodeSemver.satisfies(target, c)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + " forked under " + e.getKey() +
                        " (" + c + ") would dedupe into the bumped " + target + "; deferred");
            }
        }
    }

    /** Whether {@code dependentKey} already carries its own nested {@code node_modules/<name>} satisfying {@code constraint}. */
    private static boolean nestedCopySatisfiesNpm(Map<String, Object> packages, String dependentKey,
                                                  String name, String constraint) {
        Object nested = packages.get(dependentKey + "/node_modules/" + name);
        if (!(nested instanceof Map)) {
            return false;
        }
        Object v = ((Map<?, ?>) nested).get("version");
        return v != null && NodeSemver.validRange(constraint) && NodeSemver.satisfies(String.valueOf(v), constraint);
    }

    /** Whether some importer or installed entry pins {@code name} at exactly {@code version} (so its top slot cannot move). */
    private static boolean topLevelPinnedNpm(String lock, String name, String version) {
        for (Object value : packagesMap(lock).values()) {
            if (value instanceof Map) {
                for (String scope : DECLARED_SCOPES) {
                    Object scopeMap = ((Map<?, ?>) value).get(scope);
                    if (scopeMap instanceof Map) {
                        Object c = ((Map<?, ?>) scopeMap).get(name);
                        if (c instanceof String && version.equals(((String) c).trim())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static @Nullable String firstExcludingConstraint(Map<?, ?> entry, String name, String target) {
        for (String scope : Arrays.asList("dependencies", "optionalDependencies", "peerDependencies")) {
            Object scopeMap = entry.get(scope);
            if (scopeMap instanceof Map) {
                Object c = ((Map<?, ?>) scopeMap).get(name);
                if (c instanceof String && NodeSemver.validRange((String) c) && !NodeSemver.satisfies(target, (String) c)) {
                    return (String) c;
                }
            }
        }
        return null;
    }

    /** The moving package's existing entry must be a plain registry leaf to relocate byte-for-byte. */
    private static void requireNestableLeaf(String name, @Nullable Map<?, ?> entry, String desc) {
        if (entry == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, "no lock entry to relocate for the " + desc);
        }
        if (bool(asBoolean(entry.get("dev")))) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, desc + " is a dev entry; dev nesting is not yet supported");
        }
        if (bool(asBoolean(entry.get("link")))) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, desc + " is a workspace link; not yet supported");
        }
        if (!(entry.get("resolved") instanceof String) || !(entry.get("integrity") instanceof String)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, desc + " has no registry locator to relocate");
        }
        if (notEmptyMapValue(entry.get("dependencies")) || notEmptyMapValue(entry.get("optionalDependencies"))) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    desc + " has its own dependencies; nesting a non-leaf is not yet supported");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> packagesMap(String lock) {
        Object packages = parseJsonObject(lock, false).get("packages");
        return packages instanceof Map ? (Map<String, Object>) packages : Collections.emptyMap();
    }

    private static @Nullable Map<?, ?> entryMap(@Nullable Object value) {
        return value instanceof Map ? (Map<?, ?>) value : null;
    }

    private static boolean notEmptyMapValue(@Nullable Object value) {
        return value instanceof Map && !((Map<?, ?>) value).isEmpty();
    }

    private static @Nullable Boolean asBoolean(@Nullable Object value) {
        return value instanceof Boolean ? (Boolean) value : null;
    }

    /**
     * The bun analogue of {@link #planReverseDependentNestsNpm}: the same single-leaf slice, relocating the old
     * version to a {@code "<dependent>/<name>"} tuple. Only the lock reads differ (bun keys {@code packages} by name).
     */
    private static List<LockEditSet.PackageEdit> planReverseDependentNestsBun(String name, String oldVersion,
                                                                              String targetVersion, String lock,
                                                                              NodeRegistries registries,
                                                                              NpmRegistryClient client) {
        Map<String, String> conflicts = conflictingReverseDependentsBun(lock, name, targetVersion);
        if (conflicts.isEmpty()) {
            return Collections.emptyList();
        }
        if (conflicts.size() > 1) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " is required at excluding versions by " + conflicts.keySet() +
                            "; nesting more than one reverse-dependent is not yet supported");
        }
        Map.Entry<String, String> only = conflicts.entrySet().iterator().next();
        String dependent = only.getKey();
        requireNestableBunLeaf(name, tupleOf(packagesMap(lock).get(name)));

        NodeRegistry registry = registries.registryFor(name);
        Set<String> published = client.getPackument(registry, name).getVersions();
        String nested = maxSatisfyingAll(published, Collections.singleton(only.getValue()));
        if (!oldVersion.equals(nested)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, dependent + " requires " + name + "@" +
                    only.getValue() + " which resolves to " + nested + ", not the locked " + oldVersion +
                    "; nesting a re-resolved version is not yet supported");
        }

        return Collections.singletonList(LockEditSet.PackageEdit.builder()
                .name(name)
                .oldVersion(oldVersion)
                .newVersion(oldVersion)
                .scope("dependencies")
                .kind(REVERSE_NEST)
                .nestedUnder(dependent)
                .build());
    }

    /** Top-level bun tuples whose metadata records a constraint on {@code name} that excludes {@code target}. */
    private static Map<String, String> conflictingReverseDependentsBun(String lock, String name, String target) {
        Map<String, String> conflicts = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : packagesMap(lock).entrySet()) {
            if (isNestedBunKey(e.getKey())) {
                continue; // only plain top-level dependents can gain a nested copy
            }
            List<?> tuple = tupleOf(e.getValue());
            if (tuple != null && tuple.size() >= 3 && tuple.get(2) instanceof Map) {
                String c = firstExcludingConstraint((Map<?, ?>) tuple.get(2), name, target);
                if (c != null) {
                    conflicts.put(e.getKey(), c);
                }
            }
        }
        return conflicts;
    }

    /** The moving bun package's tuple must be a plain registry leaf (empty metadata deps, an integrity) to relocate. */
    private static void requireNestableBunLeaf(String name, @Nullable List<?> tuple) {
        if (tuple == null || tuple.size() < 4) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, "no bun tuple to relocate for " + name);
        }
        if (tuple.get(2) instanceof Map) {
            Map<?, ?> meta = (Map<?, ?>) tuple.get(2);
            if (notEmptyMapValue(meta.get("dependencies")) || notEmptyMapValue(meta.get("optionalDependencies")) ||
                    notEmptyMapValue(meta.get("peerDependencies"))) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        name + " has its own dependencies; nesting a non-leaf is not yet supported");
            }
        }
        if (!(tuple.get(3) instanceof String)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + " has no integrity to relocate");
        }
    }

    private static @Nullable List<?> tupleOf(@Nullable Object value) {
        return value instanceof List ? (List<?>) value : null;
    }

    /**
     * A closure-unchanged proof only inspects the moving package's own manifest, not the other entries that depend
     * on it. If a reverse-dependent's recorded constraint excludes the new version, re-pinning would emit a lock a
     * real install rejects, so fail loud instead.
     */
    private static void proveReverseDependentsAccept(PackageManager pm, String name, String oldVersion,
                                                     String targetVersion, String lock) {
        switch (pm) {
            case Npm:
                proveReverseDependentsNpm(name, targetVersion, lock);
                break;
            case Bun:
                proveReverseDependentsBun(name, targetVersion, lock);
                break;
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
        if (NodeSemver.validRange(range) && !NodeSemver.satisfies(targetVersion, range)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " is required by " + dependent + " at " + value + " which excludes " + targetVersion);
        }
    }

    private static void proveReverseDependentsNpm(String name, String targetVersion, String lock) {
        Map<String, Object> root = parseJsonObject(lock, false);
        Object packages = root.get("packages");
        if (packages instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) packages).entrySet()) {
                String key = String.valueOf(e.getKey());
                // Only true transitive reverse-dependents (installed packages), never importer manifests
                // (those constraints are the user's package.json, re-pinned by the patcher).
                if (!key.contains("node_modules/") || !(e.getValue() instanceof Map)) {
                    continue;
                }
                Map<?, ?> entry = (Map<?, ?>) e.getValue();
                checkReverseConstraint(name, targetVersion, key, entry.get("dependencies"));
                checkReverseConstraint(name, targetVersion, key, entry.get("optionalDependencies"));
                checkReverseConstraint(name, targetVersion, key, entry.get("peerDependencies"));
            }
        }
        checkLegacyRequires(root.get("dependencies"), name, targetVersion);
    }

    private static void checkLegacyRequires(@Nullable Object node, String name, String targetVersion) {
        if (!(node instanceof Map)) {
            return;
        }
        for (Map.Entry<?, ?> e : ((Map<?, ?>) node).entrySet()) {
            if (e.getValue() instanceof Map) {
                Map<?, ?> entry = (Map<?, ?>) e.getValue();
                checkReverseConstraint(name, targetVersion, String.valueOf(e.getKey()), entry.get("requires"));
                checkLegacyRequires(entry.get("dependencies"), name, targetVersion);
            }
        }
    }

    private static void proveReverseDependentsBun(String name, String targetVersion, String lock) {
        Map<String, Object> root = parseJsonObject(lock, false);
        Object packages = root.get("packages");
        if (!(packages instanceof Map)) {
            return;
        }
        for (Map.Entry<?, ?> e : ((Map<?, ?>) packages).entrySet()) {
            if (!(e.getValue() instanceof List)) {
                continue;
            }
            List<?> tuple = (List<?>) e.getValue();
            if (tuple.size() >= 3 && tuple.get(2) instanceof Map) {
                Map<?, ?> meta = (Map<?, ?>) tuple.get(2);
                String dependent = String.valueOf(e.getKey());
                checkReverseConstraint(name, targetVersion, dependent, meta.get("dependencies"));
                checkReverseConstraint(name, targetVersion, dependent, meta.get("optionalDependencies"));
                checkReverseConstraint(name, targetVersion, dependent, meta.get("peerDependencies"));
            }
        }
    }

    private static void checkReverseConstraint(String name, String targetVersion, String dependent,
                                               @Nullable Object constraintMap) {
        if (!(constraintMap instanceof Map)) {
            return;
        }
        Object constraint = ((Map<?, ?>) constraintMap).get(name);
        if (constraint instanceof String && NodeSemver.validRange((String) constraint) &&
                !NodeSemver.satisfies(targetVersion, (String) constraint)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " is required by " + dependent + " at " + constraint + " which excludes " + targetVersion);
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

    private static LockEditSet.@Nullable EntryMetadata writeThrough(PackageManager pm, VersionManifest oldM, VersionManifest newM) {
        LockEditSet.EntryMetadata.EntryMetadataBuilder b = LockEditSet.EntryMetadata.builder();
        boolean any = false;
        if (!Objects.equals(normalize(oldM.getEngines()), normalize(newM.getEngines()))) {
            b.engines(newM.getEngines());
            b.enginesChanged(true);
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
        // Only npm records funding in its lock; pnpm/bun/yarn omit it, so a funding delta is a no-op there.
        if (pm == PackageManager.Npm && !Objects.equals(oldM.getFunding(), newM.getFunding())) {
            JsonNode funding = newM.getFunding();
            if (funding != null && !funding.isTextual()) {
                // npm reshapes object/array funding; only the string form is byte-reproducible (as leaf adds gate).
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, newM.getName(),
                        newM.getName() + " funding changed to a non-string form; native write-through is not supported");
            }
            b.funding(normalizeFunding(funding));
            b.fundingChanged(true);
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
            case Npm:
            case Bun:
                return findImporterDirJson(pm, lock, name, oldConstraint);
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

    /** How many workspace importers declare {@code name} at exactly {@code constraint} (a shared entry when {@code > 1}). */
    private static int importersDeclaring(PackageManager pm, String lock, String name, @Nullable String constraint) {
        int count = 0;
        if (pm == PackageManager.YarnBerry) {
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
        } else {
            Object workspaces = parseJsonObject(lock, false).get("workspaces");
            if (workspaces instanceof Map) {
                for (Object importer : ((Map<?, ?>) workspaces).values()) {
                    if (importer instanceof Map) {
                        String c = declaredConstraintIn((Map<?, ?>) importer, name, DECLARED_SCOPES);
                        if (c != null && (constraint == null || constraint.equals(c))) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    private static @Nullable String findImporterDirJson(PackageManager pm, String lock, String name,
                                                        @Nullable String oldConstraint) {
        Map<String, Object> root = parseJsonObject(lock, false);
        String unique = null;
        if (pm == PackageManager.Npm) {
            Object packages = root.get("packages");
            if (packages instanceof Map) {
                for (Map.Entry<?, ?> e : ((Map<?, ?>) packages).entrySet()) {
                    String key = String.valueOf(e.getKey());
                    if (key.contains("node_modules/") || !(e.getValue() instanceof Map)) {
                        continue; // only importer entries
                    }
                    String c = declaredConstraintIn((Map<?, ?>) e.getValue(), name, DECLARED_SCOPES);
                    if (c != null && (oldConstraint == null || oldConstraint.equals(c))) {
                        if (key.isEmpty()) {
                            return null; // the root importer owns it
                        }
                        if (unique != null) {
                            return null; // ambiguous, fall back to the root importer
                        }
                        unique = key;
                    }
                }
            }
            return unique;
        }
        // bun: workspaces.<dir>.<scope>.<name>
        Object workspaces = root.get("workspaces");
        if (workspaces instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) workspaces).entrySet()) {
                String key = String.valueOf(e.getKey());
                if (!(e.getValue() instanceof Map)) {
                    continue;
                }
                String c = declaredConstraintIn((Map<?, ?>) e.getValue(), name, DECLARED_SCOPES);
                if (c != null && (oldConstraint == null || oldConstraint.equals(c))) {
                    if (key.isEmpty()) {
                        return null;
                    }
                    if (unique != null) {
                        return null;
                    }
                    unique = key;
                }
            }
        }
        return unique;
    }

    private static @Nullable String declaredConstraintIn(Map<?, ?> importer, String name, List<String> scopes) {
        for (String scope : scopes) {
            Object scopeMap = importer.get(scope);
            if (scopeMap instanceof Map && ((Map<?, ?>) scopeMap).get(name) != null) {
                return String.valueOf(((Map<?, ?>) scopeMap).get(name));
            }
        }
        return null;
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
            case Npm:
                return findLockedVersionsJson(lock, name);
            case Bun:
                return findLockedVersionsBun(lock, name);
            case Pnpm:
                return findLockedVersionsPnpm(lock, name);
            case YarnClassic:
                return findLockedVersionsYarn(lock, name);
            case YarnBerry:
                return findLockedVersionsBerry(lock, name);
            default:
                return Collections.emptySet();
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
        Map<String, String> oldDeps = rootOld.getDependencies() == null ? Collections.emptyMap() : rootOld.getDependencies();
        Map<String, String> newDeps = rootNew.getDependencies() == null ? Collections.emptyMap() : rootNew.getDependencies();
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
            if (isUnsupportedProtocol(newConstraint) || !NodeSemver.validRange(newConstraint)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep, dep + " has an unresolvable range: " + newConstraint);
            }
            String cur = locked.iterator().next();
            if (NodeSemver.satisfies(cur, newConstraint)) {
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
        String target = maxSatisfyingAll(client.getPackument(registry, dep).getVersions(), Collections.singleton(newConstraint));
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

    /**
     * A direct-dependency bump whose new {@code dependencies} edges force a currently-locked transitive to move,
     * for bun. Each kept edge that the locked version no longer satisfies re-resolves and emits a forced move; a
     * shared or directly-declared transitive, an added transitive, or a mover whose own dependencies also change
     * fails loud. A dropped edge is guarded upstream (orphan-prune is separate).
     */
    private static List<LockEditSet.PackageEdit> cascadeForcedMovesBun(String rootName, VersionManifest rootNew,
                                                                       String lock, NodeRegistries registries,
                                                                       NpmRegistryClient client) {
        Map<String, String> newDeps = rootNew.getDependencies() == null ? Collections.emptyMap() : rootNew.getDependencies();
        List<LockEditSet.PackageEdit> moves = new ArrayList<>();
        for (Map.Entry<String, String> e : newDeps.entrySet()) {
            String dep = e.getKey();
            String newConstraint = e.getValue();
            Set<String> locked = findLockedVersionsBun(lock, dep);
            if (locked.isEmpty()) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                        "upgrading " + rootName + " introduces new transitive " + dep + " (add-during-bump) not yet supported");
            }
            if (locked.size() > 1) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep, dep + " is present at multiple versions; deferred");
            }
            if (isUnsupportedProtocol(newConstraint) || !NodeSemver.validRange(newConstraint)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep, dep + " has an unresolvable range: " + newConstraint);
            }
            String cur = locked.iterator().next();
            // Still satisfied: the bumped parent's own metadata records the new range, so no separate move.
            if (NodeSemver.satisfies(cur, newConstraint)) {
                continue;
            }
            moves.add(resolveForcedMoveBun(rootName, dep, cur, newConstraint, lock, registries, client));
        }
        return moves;
    }

    /** Resolve and emit one bun forced move, failing loud on a shared/declared requirer or any deeper reshape. */
    private static LockEditSet.PackageEdit resolveForcedMoveBun(String rootName, String dep, String oldVersion,
                                                               String newConstraint, String lock,
                                                               NodeRegistries registries, NpmRegistryClient client) {
        if (bunHasOtherRequirer(lock, dep, rootName)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                    dep + " is required by more than the upgraded " + rootName + "; a shared move defers");
        }
        if (bunImporterDeclares(lock, dep)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                    dep + " is directly declared; moving it via cascade is not yet supported");
        }
        NodeRegistry registry = registries.registryFor(dep);
        String target = maxSatisfyingAll(client.getPackument(registry, dep).getVersions(), Collections.singleton(newConstraint));
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
        if (dist == null || dist.getIntegrity() == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, dep, dep + "@" + target + " has no registry integrity");
        }
        return LockEditSet.PackageEdit.builder()
                .name(dep)
                .oldVersion(oldVersion)
                .newVersion(target)
                .newIntegrity(dist.getIntegrity())
                .newDependencies(newManifest.getDependencies())
                .scope("dependencies")
                .importerDir(null)
                .kind(FORCED_MOVE)
                .build();
    }

    /** True when a package other than {@code rootName} (or a nested placement) records a constraint on {@code dep}. */
    private static boolean bunHasOtherRequirer(String lock, String dep, String rootName) {
        for (Map.Entry<String, Object> e : packagesMap(lock).entrySet()) {
            if (e.getKey().equals(rootName) || isNestedBunKey(e.getKey())) {
                continue;
            }
            List<?> tuple = tupleOf(e.getValue());
            if (tuple != null && tuple.size() >= 3 && tuple.get(2) instanceof Map) {
                Map<?, ?> meta = (Map<?, ?>) tuple.get(2);
                for (String scope : Arrays.asList("dependencies", "optionalDependencies", "peerDependencies")) {
                    Object deps = meta.get(scope);
                    if (deps instanceof Map && ((Map<?, ?>) deps).containsKey(dep)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** True when a bun workspace importer declares {@code dep} directly (so a cascade would touch its own range). */
    private static boolean bunImporterDeclares(String lock, String dep) {
        Object workspaces = parseJsonObject(lock, false).get("workspaces");
        if (!(workspaces instanceof Map)) {
            return false;
        }
        for (Object importer : ((Map<?, ?>) workspaces).values()) {
            if (!(importer instanceof Map)) {
                continue;
            }
            for (String scope : DECLARED_SCOPES) {
                Object deps = ((Map<?, ?>) importer).get(scope);
                if (deps instanceof Map && ((Map<?, ?>) deps).containsKey(dep)) {
                    return true;
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

    private static Set<String> findLockedVersionsJson(String lock, String name) {
        Map<String, Object> root = parseJsonObject(lock, false);
        Set<String> versions = new LinkedHashSet<>();
        String suffix = "node_modules/" + name;
        Object packages = root.get("packages");
        if (packages instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) packages).entrySet()) {
                String key = String.valueOf(e.getKey());
                if ((key.equals(suffix) || key.endsWith("/" + suffix)) && e.getValue() instanceof Map) {
                    Object v = ((Map<?, ?>) e.getValue()).get("version");
                    if (v != null) {
                        versions.add(String.valueOf(v));
                    }
                }
            }
        }
        // package-lock v1 (and the v2 legacy tree) keep resolved versions under `dependencies`.
        collectJsonDependencyTree(root.get("dependencies"), name, versions);
        return versions;
    }

    /** bun keys its {@code packages} by name (or {@code parent/name} when nested), so read the version off each tuple's locator. */
    private static Set<String> findLockedVersionsBun(String lock, String name) {
        Set<String> versions = new LinkedHashSet<>();
        for (Object value : packagesMap(lock).values()) {
            List<?> tuple = tupleOf(value);
            if (tuple != null && !tuple.isEmpty() && tuple.get(0) instanceof String) {
                String locator = (String) tuple.get(0);
                int at = locator.lastIndexOf('@');
                if (at > 0 && locator.substring(0, at).equals(name)) {
                    versions.add(locator.substring(at + 1));
                }
            }
        }
        return versions;
    }

    @SuppressWarnings("unchecked")
    private static void collectJsonDependencyTree(@Nullable Object node, String name, Set<String> versions) {
        if (!(node instanceof Map)) {
            return;
        }
        for (Map.Entry<?, ?> e : ((Map<?, ?>) node).entrySet()) {
            if (!(e.getValue() instanceof Map)) {
                continue;
            }
            Map<String, Object> entry = (Map<String, Object>) e.getValue();
            if (name.equals(String.valueOf(e.getKey())) && entry.get("version") != null) {
                versions.add(String.valueOf(entry.get("version")));
            }
            collectJsonDependencyTree(entry.get("dependencies"), name, versions);
        }
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
            case Npm:         return new NpmLockPatcher();
            case Pnpm:        return new PnpmLockPatcher();
            case YarnClassic: return new YarnClassicLockPatcher();
            case YarnBerry:   return new YarnBerryLockPatcher();
            case Bun:         return new BunLockPatcher();
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

    /** A pending edge in the closure walk: a package name, the requiring constraint, dev-reachability, and requirer. */
    private static final class Requirement {
        final String name;
        final String constraint;
        final boolean dev;
        final @Nullable String parent;

        Requirement(String name, String constraint, boolean dev) {
            this(name, constraint, dev, null);
        }

        Requirement(String name, String constraint, boolean dev, @Nullable String parent) {
            this.name = name;
            this.constraint = constraint;
            this.dev = dev;
            this.parent = parent;
        }
    }

    /** A closure member npm nests under a freshly-placed parent because an incompatible version holds the top slot. */
    private static final class NestedPlacement {
        final String parent;
        final String name;
        final String version;
        final VersionManifest manifest;
        final boolean dev;

        NestedPlacement(String parent, String name, String version, VersionManifest manifest, boolean dev) {
            this.parent = parent;
            this.name = name;
            this.version = version;
            this.manifest = manifest;
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
