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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.javascript.NodeExecutionContextView;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.LockFileRegeneration;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The shared, package-manager-agnostic orchestrator for native lock regeneration. It diffs the
 * pre-edit and post-edit {@code package.json} to scope the change to the declared dependencies the
 * recipe actually touched, proves each moving dependency leaves the resolved closure unchanged (the
 * strict layout whitelist), resolves the target version with minimal-update semantics against the
 * registry, and hands a proven {@link LockEditSet} to the format's {@link LockPatcher}.
 * <p>
 * Everything outside the whitelist fails loud (emitting no lock and a structured {@link Failure}); a
 * shell-out fallback is deliberately absent (it would lose registry credentials — the failure this
 * initiative exists to kill). The only non-fail-loud tolerance is the write-through metadata tier
 * ({@code engines}/{@code license}/{@code deprecated}/{@code bin}), which a real bump patches without
 * reshaping the tree.
 * <p>
 * Reads over the raw lock and the two manifests are read-only inspection (Jackson for JSON, SnakeYAML
 * for pnpm's YAML) — never the lossy {@code LockFileParser}/adapter normalization, which the proof
 * must not trust.
 */
public final class NativeLockEngine {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Constraint protocols the native engine cannot re-pin without repo/filesystem access. */
    private static final List<String> UNSUPPORTED_PROTOCOLS = Arrays.asList(
            "git:", "git+", "github:", "file:", "link:", "portal:", "workspace:", "http://", "https://");

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
            // The manifest changed but no declared dependency did — the edit is an override/resolution
            // or some other field the native engine cannot prove leaves the closure unchanged.
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "change is outside declared dependencies (e.g. overrides/resolutions) and requires resolution");
        }

        Path lockPath = lockPath(pm, packageJsonPath);
        NodeRegistries registries = RegistryDiscovery.discover(ctx, marker, Environment.SYSTEM);
        NpmRegistryClient client = NodeExecutionContextView.view(ctx).getRegistryClient();

        List<LockEditSet.PackageEdit> edits = new ArrayList<>();
        for (DepChange change : changes) {
            edits.add(resolveEdit(pm, change, existingLock, registries, client));
        }

        LockEditSet editSet = new LockEditSet(existingLock, lockPath, pm, editedPackageJson, edits);

        LockPatcher patcher = patcherFor(pm);
        if (patcher == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "no native patcher for " + pm + " yet");
        }
        return Result.success(patcher.patch(editSet));
    }

    private static LockEditSet.PackageEdit resolveEdit(PackageManager pm, DepChange change, String existingLock,
                                                       NodeRegistries registries, NpmRegistryClient client) {
        String name = change.name;

        if (change.oldConstraint == null) {
            // Added dependency — a new node in the closure needs the hoisting-aware resolver (Phase B).
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, "adding " + name + " requires resolution");
        }

        Set<String> lockedVersions = findLockedVersions(pm, existingLock, name);

        if (change.newConstraint == null) {
            // Removal — the patcher drops the entry and its orphans; keystone has no patcher yet.
            String oldVersion = lockedVersions.isEmpty() ? "" : lockedVersions.iterator().next();
            return LockEditSet.PackageEdit.builder()
                    .name(name)
                    .oldVersion(oldVersion)
                    .newVersion(null)
                    .scope(change.scope)
                    .oldConstraint(change.oldConstraint)
                    .build();
        }

        if (isUnsupportedProtocol(change.newConstraint)) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, name,
                    name + " uses an unsupported entry type: " + change.newConstraint);
        }

        if (lockedVersions.isEmpty()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, "no locked entry for " + name);
        }
        if (lockedVersions.size() > 1) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " is present at multiple versions (fork/peer-duplicated): " + lockedVersions);
        }
        String oldVersion = lockedVersions.iterator().next();

        String targetVersion = resolveTarget(client, registries, name, oldVersion, change.newConstraint);

        LockEditSet.PackageEdit.PackageEditBuilder edit = LockEditSet.PackageEdit.builder()
                .name(name)
                .oldVersion(oldVersion)
                .newVersion(targetVersion)
                .scope(change.scope)
                .oldConstraint(change.oldConstraint);

        if (targetVersion.equals(oldVersion)) {
            // Constraint-only widening: the resolved version does not move, so the package entry keeps
            // its resolved/integrity and only the importer's declared constraint is re-pinned.
            return edit.build();
        }

        NodeRegistry registry = registries.registryFor(name);
        VersionManifest oldManifest = client.getManifest(registry, name, oldVersion);
        VersionManifest newManifest = client.getManifest(registry, name, targetVersion);

        proveClosureUnchanged(name, oldManifest, newManifest);

        VersionManifest.Dist dist = newManifest.getDist();
        return edit
                .newResolved(dist == null ? null : dist.getTarball())
                .newIntegrity(dist == null ? null : dist.getIntegrity())
                .newShasum(dist == null ? null : dist.getShasum())
                .newDependencies(newManifest.getDependencies())
                .newOptionalDependencies(newManifest.getOptionalDependencies())
                .writeThroughMetadata(writeThrough(oldManifest, newManifest))
                .build();
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
     * The strict layout whitelist. The two manifests must agree on every closure-affecting surface;
     * only the write-through tier (engines/license/deprecated/bin) may differ.
     */
    private static void proveClosureUnchanged(String name, VersionManifest oldM, VersionManifest newM) {
        requireEqual(name, "dependencies", oldM.getDependencies(), newM.getDependencies());
        requireEqual(name, "peerDependencies", oldM.getPeerDependencies(), newM.getPeerDependencies());
        requireEqual(name, "peerDependenciesMeta", oldM.getPeerDependenciesMeta(), newM.getPeerDependenciesMeta());
        requireEqual(name, "optionalDependencies", oldM.getOptionalDependencies(), newM.getOptionalDependencies());
        requireEqual(name, "os", oldM.getOs(), newM.getOs());
        requireEqual(name, "cpu", oldM.getCpu(), newM.getCpu());
        requireEqual(name, "libc", oldM.getLibc(), newM.getLibc());
        requireEqual(name, "bundleDependencies", oldM.getBundleDependencies(), newM.getBundleDependencies());
        if (!Objects.equals(bool(oldM.getHasInstallScript()), bool(newM.getHasInstallScript()))) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + " hasInstallScript changed");
        }
        // Note: peer-provider and dedupe-reshuffle detection require the full hoisting model (Phase B).
        // The single-locked-version guard plus the dependency-set-unchanged requirement above reject the
        // common triggers; a full check lands with the hoisting-aware resolver.
    }

    private static void requireEqual(String name, String surface, @Nullable Object a, @Nullable Object b) {
        if (!Objects.equals(normalize(a), normalize(b))) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + " " + surface + " changed");
        }
    }

    private static LockEditSet.@Nullable WriteThroughMetadata writeThrough(VersionManifest oldM, VersionManifest newM) {
        LockEditSet.WriteThroughMetadata.WriteThroughMetadataBuilder b = LockEditSet.WriteThroughMetadata.builder();
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

    // --- raw lock inspection (read-only) ---------------------------------

    private static Set<String> findLockedVersions(PackageManager pm, String lock, String name) {
        switch (pm) {
            case Npm:
            case Bun:
                return findLockedVersionsJson(lock, name);
            case Pnpm:
                return findLockedVersionsPnpm(lock, name);
            case YarnClassic:
            case YarnBerry:
                return findLockedVersionsYarn(lock, name);
            default:
                return Collections.emptySet();
        }
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
