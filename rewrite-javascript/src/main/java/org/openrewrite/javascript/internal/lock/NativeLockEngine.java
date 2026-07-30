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
import org.openrewrite.javascript.NodeExecutionContextView;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.LockFileRegeneration;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
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

    // Bun's lock is JSONC (trailing commas, // comments); npm's is strict JSON — this mapper reads both.
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
            edits.addAll(resolveEdit(pm, change, existingLock, registries, client));
        }

        LockEditSet editSet = new LockEditSet(existingLock, lockPath, pm, editedPackageJson, edits);

        LockPatcher patcher = patcherFor(pm);
        if (patcher == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "no native patcher for " + pm + " yet");
        }
        return Result.success(patcher.patch(editSet));
    }

    private static List<LockEditSet.PackageEdit> resolveEdit(PackageManager pm, DepChange change, String existingLock,
                                                             NodeRegistries registries, NpmRegistryClient client) {
        String name = change.name;

        if (change.oldConstraint == null) {
            // Added dependency (Phase B). The added direct dep plus its resolved runtime closure is
            // hoisted against the existing tree; any placement that would move/nest/fork fails loud.
            return resolveClosureAdd(pm, change, existingLock, registries, client);
        }

        Set<String> lockedVersions = findLockedVersions(pm, existingLock, name);
        String importerDir = findImporterDir(pm, existingLock, name, change.scope, change.oldConstraint);

        if (change.newConstraint == null) {
            // Removal — the patcher drops the entry and its orphans; keystone has no patcher yet.
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
                .oldConstraint(change.oldConstraint)
                .importerDir(importerDir);

        if (targetVersion.equals(oldVersion)) {
            // Constraint-only widening: the resolved version does not move, so the package entry keeps
            // its resolved/integrity and only the importer's declared constraint is re-pinned.
            return Collections.singletonList(edit.build());
        }

        // A reverse-dependent whose recorded constraint excludes the new version keeps the old version
        // nested under itself (Phase B I5); the safe single-leaf slice resolves it, everything else defers.
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

        // Every closure surface but `dependencies` must be unchanged; a `dependencies` delta no longer
        // fails loud outright — it seeds the Phase B I3 greedy-forward cascade below.
        proveNonDependencySurfacesUnchanged(name, oldManifest, newManifest);

        VersionManifest.Dist dist = newManifest.getDist();
        // A dropped `dependencies` edge (present in the old manifest, gone in the new) orphan-prunes rather than
        // fails loud: the patcher removes the edge and GCs whatever it leaves unreachable. npm only so far.
        boolean prunesOrphans = pm == PackageManager.Npm && dropsDependencyEdge(oldManifest, newManifest);
        LockEditSet.PackageEdit rootEdit = edit
                .newResolved(dist == null ? null : dist.getTarball())
                .newIntegrity(dist == null ? null : dist.getIntegrity())
                .newShasum(dist == null ? null : dist.getShasum())
                .newDependencies(newManifest.getDependencies())
                .newOptionalDependencies(newManifest.getOptionalDependencies())
                .writeThroughMetadata(writeThrough(oldManifest, newManifest))
                .prunesOrphans(prunesOrphans)
                .build();

        List<LockEditSet.PackageEdit> edits = new ArrayList<>();
        edits.add(rootEdit);
        edits.addAll(nestEdits);
        if (!dependenciesEqual(oldManifest, newManifest)) {
            if (pm == PackageManager.Npm) {
                edits.addAll(cascadeForcedMoves(name, oldManifest, newManifest, existingLock, registries, client));
            } else if (pm == PackageManager.Pnpm) {
                edits.addAll(cascadeForcedMovesPnpm(name, oldVersion, oldManifest, newManifest, existingLock, registries, client));
            } else {
                // Only the npm and pnpm patchers can reshape a changed closure so far; other formats defer.
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + " dependencies changed");
            }
        }
        return edits;
    }

    /**
     * Phase B increment 3: a direct-dependency bump whose new version's {@code dependencies} edges changed
     * such that a currently-locked transitive no longer satisfies and must move. It runs the same
     * greedy-forward, keep-pins contract as the closure add: seed from the bumped dep's changed edges,
     * resolve each forced transitive over the <b>union of all live constraints</b> on it (substituting the
     * bumped dep's new constraint for its stale lock entry), and <b>fail loud the instant a move would
     * reshape</b> — a sibling/reverse-dependent that rejects the new version (union unsatisfiable → npm
     * would fork/nest), a mover whose own dependencies also change (a second cascade wave / backtrack), a
     * brand-new transitive the bump introduces (add-during-bump), or a dropped edge (orphan pruning).
     */
    private static List<LockEditSet.PackageEdit> cascadeForcedMoves(String rootName, VersionManifest rootOld,
                                                                    VersionManifest rootNew, String existingLock,
                                                                    NodeRegistries registries, NpmRegistryClient client) {
        Map<String, String> newDeps = rootNew.getDependencies() == null ?
                Collections.emptyMap() : rootNew.getDependencies();

        // A dropped edge (present in oldDeps, gone from newDeps) is handled by the patcher's orphan GC, flagged
        // on the root edit via prunesOrphans — not here. This loop only re-resolves edges the new version keeps.
        // Each edge resolves over the ACTUAL installed tree via npm's hoisting walk (the bumped package's own
        // node_modules first, then up), so an edge already satisfied by a nested copy is a no-op — not a new add.
        Map<String, Object> installed = installedPackagesNpm(existingLock);
        String rootKey = "node_modules/" + rootName;
        List<LockEditSet.PackageEdit> moves = new ArrayList<>();
        for (Map.Entry<String, String> e : newDeps.entrySet()) {
            String dep = e.getKey();
            String constraint = e.getValue();
            String resolvedKey = NpmLockPatcher.resolveFrom(installed.keySet(), rootKey, dep);
            String cur = installedVersion(installed, resolvedKey);
            if (cur == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                        "upgrading " + rootName + " introduces new transitive " + dep +
                                " (add-during-bump) not yet supported");
            }
            if (isUnsupportedProtocol(constraint) || !NodeSemver.validRange(constraint)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                        dep + " is constrained by an unresolvable range: " + constraint);
            }
            if (!NodeSemver.satisfies(cur, constraint)) {
                if (!resolvedKey.equals("node_modules/" + dep)) {
                    // A nested copy that no longer satisfies would move within its own subtree; the mover path
                    // assumes a top-level entry, so defer rather than risk a non-byte-exact tree.
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                            "moving nested " + dep + " to satisfy the upgraded " + rootName +
                                    " constraint is not yet supported");
                }
                moves.add(resolveForcedMove(rootName, dep, cur, newDeps, existingLock, registries, client));
            }
        }
        return moves;
    }

    /** Resolve and emit the move of a single forced transitive, failing loud on any reshape. */
    private static LockEditSet.PackageEdit resolveForcedMove(String rootName, String dep, String oldVersion,
                                                             Map<String, String> rootNewDeps, String existingLock,
                                                             NodeRegistries registries, NpmRegistryClient client) {
        // A transitive that is also directly declared would need its importer declaration reconciled too.
        if (importerDeclaresNpm(existingLock, dep)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                    dep + " is directly declared; moving it via cascade is not yet supported");
        }

        Set<String> union = liveConstraintsNpm(existingLock, dep, rootName, rootNewDeps);
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
        // A mover must itself be a clean-closure bump; a change to its own edges is a deeper wave.
        proveNonDependencySurfacesUnchanged(dep, oldManifest, newManifest);
        if (!dependenciesEqual(oldManifest, newManifest)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, dep,
                    "moving " + dep + " to " + target + " changes its own dependencies " +
                            "(multi-level cascade) not yet supported");
        }

        VersionManifest.Dist dist = newManifest.getDist();
        if (dist == null || dist.getTarball() == null || dist.getIntegrity() == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, dep,
                    dep + "@" + target + " has no registry tarball/integrity");
        }
        return LockEditSet.PackageEdit.builder()
                .name(dep)
                .oldVersion(oldVersion)
                .newVersion(target)
                .newResolved(dist.getTarball())
                .newIntegrity(dist.getIntegrity())
                .newShasum(dist.getShasum())
                .newDependencies(newManifest.getDependencies())
                .newOptionalDependencies(newManifest.getOptionalDependencies())
                .writeThroughMetadata(writeThrough(oldManifest, newManifest))
                .scope("dependencies")
                .importerDir(null)
                .forcedMove(true)
                .build();
    }

    /**
     * Every recorded constraint on {@code dep} across the lock's installed entries, substituting the
     * bumped root's <b>new</b> constraint for its stale lock entry. The intersection over this set is the
     * reverse-edge safety check: if no version satisfies all, npm would fork/nest rather than move.
     */
    private static Set<String> liveConstraintsNpm(String lock, String dep, String rootName,
                                                  Map<String, String> rootNewDeps) {
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
                if (key.substring(nm + prefix.length()).equals(rootName)) {
                    continue; // the bumped root: substituted with its new constraint below
                }
                Map<?, ?> entry = (Map<?, ?>) e.getValue();
                addConstraintOn(constraints, entry.get("dependencies"), dep);
                addConstraintOn(constraints, entry.get("optionalDependencies"), dep);
                addConstraintOn(constraints, entry.get("peerDependencies"), dep);
            }
        }
        String rootConstraint = rootNewDeps.get(dep);
        if (rootConstraint != null) {
            constraints.add(rootConstraint);
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
     * Phase B cascade for pnpm: the same greedy-forward, keep-pins contract as {@link #cascadeForcedMoves}
     * (npm), seeded from the bumped dep's changed {@code dependencies} edges. pnpm's lock records resolved
     * versions in {@code snapshots.<root>@<v>.dependencies}, not constraints — so the transitive's current
     * version is read from the bumped root's own snapshot, and the reverse-edge safety is a
     * <b>single-requirer</b> check (a shared transitive, whose other requirers' ranges are not in the lock,
     * fails loud rather than risk a fork). The version resolution ({@link #maxSatisfyingAll}) is PM-agnostic;
     * only the lock reads differ from npm.
     */
    private static List<LockEditSet.PackageEdit> cascadeForcedMovesPnpm(String rootName, String rootOldVersion,
                                                                        VersionManifest rootOld, VersionManifest rootNew,
                                                                        String existingLock, NodeRegistries registries,
                                                                        NpmRegistryClient client) {
        Map<String, String> oldDeps = rootOld.getDependencies() == null ?
                Collections.emptyMap() : rootOld.getDependencies();
        Map<String, String> newDeps = rootNew.getDependencies() == null ?
                Collections.emptyMap() : rootNew.getDependencies();

        for (String dep : oldDeps.keySet()) {
            if (!newDeps.containsKey(dep)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, rootName,
                        "upgrading " + rootName + " drops the dependency edge to " + dep +
                                " (orphan pruning) not yet supported");
            }
        }

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
        // Reverse-edge safety: pnpm records resolved versions, not ranges, so a shared transitive's other
        // requirers cannot be proven to accept the new version from the lock alone. Only a transitive private
        // to the bumped root moves; any other referrer defers (npm would dedupe/fork against that range).
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
                .writeThroughMetadata(writeThrough(oldManifest, newManifest))
                .scope("dependencies")
                .importerDir(null)
                .forcedMove(true)
                .build();
    }

    /**
     * Phase B increment 5 (pnpm): a direct-dependency bump a reverse-dependent excludes. pnpm is
     * content-addressed and never nests, so it <b>content-forks</b> — the old version stays for the
     * reverse-dependent and the new version is added as fresh {@code packages}+{@code snapshots} content, with
     * only the importer edge retargeted. Because a pnpm lock records resolved versions (not ranges), the
     * reverse-dependent's acceptance of the new version cannot be read from the lock; its manifest is fetched to
     * prove its constraint <b>excludes</b> the target (else pnpm would dedupe it up, not fork). The safe slice is
     * a single referrer keeping a leaf; anything else fails loud.
     *
     * @return the one content-fork edit when it applies, or {@code null} when the moved dep is not kept by any
     * reverse-dependent (a plain rename bump proceeds instead).
     */
    private static @Nullable List<LockEditSet.PackageEdit> planContentForkPnpm(String name, String oldVersion,
                                                                               String targetVersion, DepChange change,
                                                                               String lock, NodeRegistries registries,
                                                                               NpmRegistryClient client) {
        Set<String> referrers = referrersPnpm(lock, name, oldVersion, " ");
        if (referrers.isEmpty()) {
            return null; // not retained by any reverse-dependent — a normal rename bump, not a fork
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

        // The referrer must genuinely EXCLUDE the new version — otherwise pnpm dedupes it up rather than forking.
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
                .writeThroughMetadata(pnpmLeafMetadata(newManifest))
                .scope(change.scope)
                .importerDir(findImporterDir(PackageManager.Pnpm, lock, name, change.scope, change.oldConstraint))
                .contentFork(true)
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

    /** The snapshot keys — other than {@code ownerKey} — that reference {@code dep@oldVersion} as a resolved dep. */
    private static Set<String> referrersPnpm(String lock, String dep, String oldVersion, String ownerKey) {
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
            if (key.equals(ownerKey) || !(e.getValue() instanceof Map)) {
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
     * Phase B increment 2: a direct dependency the recipe added, plus its runtime closure. The added
     * dep is resolved ({@code maxSatisfying}/dist-tag over the packument), then its
     * {@code dependencies}/{@code optionalDependencies} are walked transitively; each package in the
     * closure either hoists to a fresh top-level {@code node_modules/<name>} entry (absent from the lock)
     * or dedups to an already-satisfying top-level pin.
     * <p>
     * This keeps Phase A/B's accuracy-by-construction contract: every existing pin stays fixed, a
     * decision opens only for a genuinely-new package, and the resolver <b>fails loud the instant a
     * placement would move, nest, or fork an already-placed package</b> — a top-level pin the new
     * requirement cannot satisfy (npm would nest → fork/cascade, deferred to I3/I5), a closure member
     * needed at two incompatible versions, a reverse-dependent whose recorded constraint excludes it, or
     * any object-metadata surface not yet verified byte-exact. Non-npm formats still defer.
     */
    private static List<LockEditSet.PackageEdit> resolveClosureAdd(PackageManager pm, DepChange change,
                                                                   String existingLock, NodeRegistries registries,
                                                                   NpmRegistryClient client) {
        String rootName = change.name;
        String rootConstraint = Objects.requireNonNull(change.newConstraint);

        if (isUnsupportedProtocol(rootConstraint)) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, rootName,
                    rootName + " uses an unsupported entry type: " + rootConstraint);
        }
        if (pm == PackageManager.Pnpm) {
            // pnpm is content-addressed: placement is mechanical (one packages+snapshots entry per closure
            // member), but every closure member must be brand-new (no dedupe/conflict) and peer-free.
            return resolveClosureAddPnpm(change, rootName, rootConstraint, existingLock, registries, client);
        }
        if (pm == PackageManager.Bun) {
            // bun hoists like npm: the closure resolves identically, one packages tuple per member, failing
            // loud on any conflict/nest (bun's parent/name fork keys).
            return resolveClosureAddBun(change, rootName, rootConstraint, existingLock, registries, client);
        }
        if (pm == PackageManager.YarnClassic) {
            // yarn.lock lists one block per resolved (name, version); placement is not hoisted, so every
            // closure member must be brand-new (no merge/second-block for an existing name).
            return resolveClosureAddYarn(change, rootName, rootConstraint, existingLock, registries, client);
        }
        // Only the npm patcher can insert closure entries so far; other formats defer.
        if (pm != PackageManager.Npm) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, rootName, "adding " + rootName + " requires resolution");
        }

        Map<String, String> existingTopLevel = topLevelVersionsNpm(existingLock);
        boolean dev = "devDependencies".equals(change.scope);

        Map<String, Placement> placed = new LinkedHashMap<>();
        Map<String, NestedPlacement> nested = new LinkedHashMap<>();
        Deque<Requirement> queue = new ArrayDeque<>();
        queue.add(new Requirement(rootName, rootConstraint, dev));

        while (!queue.isEmpty()) {
            Requirement req = queue.poll();

            String existingVersion = existingTopLevel.get(req.name);
            if (existingVersion != null) {
                // Dedup to the already-placed pin, or nest the required version under its parent where an
                // incompatible version holds the top slot (I5 add-nest, leaf-under-a-fresh-top-level slice).
                if (existingSatisfies(existingVersion, req.constraint)) {
                    continue;
                }
                nestUnderParent(req, existingVersion, placed, nested, existingLock, registries, client);
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

            // A stray reverse-dependent whose recorded constraint excludes the resolved version → fail loud.
            proveReverseDependentsAccept(pm, req.name, version, version, existingLock);

            placed.put(req.name, new Placement(version, manifest, req.dev));

            enqueueDeps(queue, manifest.getDependencies(), req.dev, req.name);
            enqueueDeps(queue, manifest.getOptionalDependencies(), req.dev, req.name);
        }

        List<LockEditSet.PackageEdit> edits = new ArrayList<>();
        for (Map.Entry<String, Placement> e : placed.entrySet()) {
            Placement p = e.getValue();
            VersionManifest.Dist dist = Objects.requireNonNull(p.manifest.getDist());
            // Only the root (the declared dependency) writes an importer constraint; the patcher no-ops
            // the transitives because they are absent from the edited package.json. Scope carries the
            // dev-ness so a dev-rooted closure emits "dev": true on every fresh entry.
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
                    .added(true)
                    .writeThroughMetadata(leafMetadata(p.manifest))
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
                    .added(true)
                    .nestedUnder(np.parent)
                    .writeThroughMetadata(leafMetadata(np.manifest))
                    .build());
        }
        return edits;
    }

    /**
     * A closure member whose required version an incompatible top-level pin excludes: npm nests it at
     * {@code node_modules/<parent>/node_modules/<name>} (I5 add-nest). Only the safe slice resolves — the
     * requiring package is a freshly-placed top-level entry, some requirer pins the top-level version
     * <b>exactly</b> (so it provably cannot move up to satisfy the new edge — otherwise npm cascades rather
     * than forks), and the nested version is itself a leaf. Anything else fails loud (npm would reshape).
     */
    private static void nestUnderParent(Requirement req, String existingVersion, Map<String, Placement> placed,
                                        Map<String, NestedPlacement> nested, String existingLock,
                                        NodeRegistries registries, NpmRegistryClient client) {
        // A cascade (the top-level entry moves up to satisfy the new edge) is a different, deferred class;
        // only an exact pin on the current version proves the top slot is frozen, forcing the fork.
        if (req.parent == null || !placed.containsKey(req.parent) ||
                !topLevelPinnedNpm(existingLock, req.name, existingVersion)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, req.name, req.name + " is already placed at " +
                    existingVersion + " which does not satisfy " + req.constraint + " (npm would nest/move it; deferred)");
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
        requireEmittableClosureMember(req.name, manifest);
        VersionManifest.Dist dist = manifest.getDist();
        if (dist == null || dist.getTarball() == null || dist.getIntegrity() == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, req.name,
                    req.name + "@" + version + " has no registry tarball/integrity");
        }
        nested.put(nestKey, new NestedPlacement(req.parent, req.name, version, manifest, req.dev));
    }

    /**
     * Phase B increment 4 (pnpm): a direct dependency the recipe added, plus its runtime closure, into a
     * pnpm-lock.yaml v9. pnpm is non-hoisted/content-addressed, so placement is mechanical — one
     * {@code packages}+{@code snapshots} entry per closure member keyed by its resolved version. The version
     * resolution reuses the shared worklist ({@code resolveAddedVersion} over the packument, transitive walk).
     * <p>
     * The pnpm-specific contract, tighter than npm's: every closure member must be <b>brand-new</b> (its name
     * absent from the lock) so no snapshot references an existing entry and no dedupe/fork decision opens, and
     * <b>no member may declare peers or optionalDependencies</b> — pnpm encodes those as peer-suffix keys the
     * mechanical placement does not model. Any collision, peer, optional, or unsupported metadata fails loud.
     */
    private static List<LockEditSet.PackageEdit> resolveClosureAddPnpm(DepChange change, String rootName,
                                                                       String rootConstraint, String existingLock,
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
                    .importerDir(null)
                    .added(true)
                    .writeThroughMetadata(pnpmLeafMetadata(p.manifest))
                    .build());
        }
        return edits;
    }

    /**
     * Phase B (bun): a direct dependency the recipe added, plus its runtime closure, into a {@code bun.lock}.
     * bun hoists like npm, so the closure resolution is identical to {@link #resolveClosureAdd} — every member
     * hoists to a fresh top-level {@code packages["<name>"]} tuple or dedups to an already-satisfying pin, and
     * any placement that would move/nest/fork an existing entry (bun's {@code parent/name} keys) fails loud.
     * <p>
     * bun records only the integrity in the tuple (no tarball URL), and the emittable gate is tighter than
     * npm's: only a deps-or-empty tuple ({@code {}} / {@code { "dependencies": {…} }}) is byte-verified, so any
     * {@code bin}/{@code os}/{@code cpu}/{@code peer}/{@code optional} metadata or scoped name defers.
     */
    private static List<LockEditSet.PackageEdit> resolveClosureAddBun(DepChange change, String rootName,
                                                                      String rootConstraint, String existingLock,
                                                                      NodeRegistries registries, NpmRegistryClient client) {
        Map<String, String> existingTopLevel = topLevelVersionsBun(existingLock);
        boolean dev = "devDependencies".equals(change.scope);

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
                nestUnderParentBun(req, existingVersion, placed, nested, existingLock, registries, client);
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

            // A stray reverse-dependent whose recorded constraint excludes the resolved version → fail loud.
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
                    .added(true)
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
                    .added(true)
                    .nestedUnder(np.parent)
                    .build());
        }
        return edits;
    }

    /** The bun analogue of {@link #nestUnderParent}: a fresh leaf nests at {@code "<parent>/<name>"} when a frozen top pin excludes it. */
    private static void nestUnderParentBun(Requirement req, String existingVersion, Map<String, Placement> placed,
                                           Map<String, NestedPlacement> nested, String existingLock,
                                           NodeRegistries registries, NpmRegistryClient client) {
        if (req.parent == null || !placed.containsKey(req.parent) ||
                !topLevelPinnedBun(existingLock, req.name, existingVersion)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, req.name, req.name + " is already placed at " +
                    existingVersion + " which does not satisfy " + req.constraint + " (bun would nest/move it; deferred)");
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
        requireEmittableBunClosureMember(req.name, manifest);
        VersionManifest.Dist dist = manifest.getDist();
        if (dist == null || dist.getIntegrity() == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, req.name, req.name + "@" + version + " has no registry integrity");
        }
        nested.put(nestKey, new NestedPlacement(req.parent, req.name, version, manifest, req.dev));
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
     * Phase B (yarn-classic): a direct dependency the recipe added, plus its runtime closure, into a
     * {@code yarn.lock} v1. yarn is not hoisted — it lists one {@code name@range:} block per resolved
     * {@code (name, version)} — so placement is mechanical (the patcher inserts each block at its
     * {@code sortAlpha} position), but the block header is the set of every declared/transitive range that
     * resolves to the version. The version resolution reuses the shared worklist ({@code resolveAddedVersion}
     * over the packument, transitive walk).
     * <p>
     * The yarn-specific contract: every closure member must be <b>brand-new</b> (its name absent from every
     * existing block) so no header merge or second-version block opens, and <b>no member may declare peers or
     * optionalDependencies</b> — yarn resolves those into further blocks the clean placement does not model.
     * Any collision, peer, or optional fails loud; other block-irrelevant metadata (engines/os/cpu/bin) is not
     * recorded in a yarn.lock block, so it needs no gate.
     */
    private static List<LockEditSet.PackageEdit> resolveClosureAddYarn(DepChange change, String rootName,
                                                                       String rootConstraint, String existingLock,
                                                                       NodeRegistries registries, NpmRegistryClient client) {
        Set<String> existingNames = existingYarnNames(existingLock);
        boolean dev = "devDependencies".equals(change.scope);

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
                    .added(true)
                    .build());
        }
        return edits;
    }

    /**
     * A closure member the yarn add patcher can insert byte-exactly. A yarn.lock block carries only
     * {@code version}/{@code resolved}/{@code integrity}/{@code dependencies}, so engines/os/cpu/bin never
     * appear and need no gate; a peer or optionalDependency, however, resolves into further blocks the clean
     * placement does not model — so either defers the whole add.
     */
    private static void requireEmittableYarnClosureMember(String name, VersionManifest m) {
        if (notEmpty(m.getOptionalDependencies())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    "adding " + name + " pulls in optionalDependencies not yet supported for yarn adds");
        }
        if (notEmpty(m.getPeerDependencies()) || nonEmptyObject(m.getPeerDependenciesMeta())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    "adding " + name + " declares peerDependencies not yet supported for yarn adds");
        }
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
     * A closure member whose bun tuple the patcher can insert byte-exactly: a metadata object of {@code {}} or
     * {@code { "dependencies": {…} }} only. bun records {@code optionalDependencies}/{@code peerDependencies}/
     * {@code bin}/{@code os}/{@code cpu}/{@code libc} into that object and auto-installs non-optional peers,
     * none of which the deps-or-empty placement models — so any of them, a scoped name, or unverified metadata
     * defers the whole add.
     */
    private static void requireEmittableBunClosureMember(String name, VersionManifest m) {
        if (name.indexOf('/') >= 0 || name.startsWith("@")) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    "adding scoped package " + name + " is not yet supported for bun adds");
        }
        if (notEmpty(m.getOptionalDependencies())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    "adding " + name + " pulls in optionalDependencies not yet supported for bun adds");
        }
        if (notEmpty(m.getPeerDependencies()) || nonEmptyObject(m.getPeerDependenciesMeta())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    "adding " + name + " declares peerDependencies not yet supported for bun adds");
        }
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
    private static LockEditSet.@Nullable WriteThroughMetadata pnpmLeafMetadata(VersionManifest m) {
        if (!notEmpty(m.getEngines())) {
            return null;
        }
        return LockEditSet.WriteThroughMetadata.builder().engines(m.getEngines()).build();
    }

    /**
     * A closure member the pnpm add patcher can insert byte-exactly: {@code resolution} + optional
     * {@code engines} only. Any peer (pnpm's suffix-key surface), optionalDependencies, or object/array
     * metadata not yet verified byte-exact defers the whole add.
     */
    private static void requireEmittablePnpmClosureMember(String name, VersionManifest m) {
        if (notEmpty(m.getOptionalDependencies())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    "adding " + name + " pulls in optionalDependencies not yet supported for pnpm adds");
        }
        if (notEmpty(m.getPeerDependencies()) || nonEmptyObject(m.getPeerDependenciesMeta())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    "adding " + name + " declares peerDependencies (pnpm suffix keys) not yet supported for pnpm adds");
        }
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
     * A closure member whose lock entry the npm patcher can insert byte-exactly. Its {@code dependencies}
     * are handled (walked + recorded as the entry's dependency map / v2 {@code requires}); the byte-exact
     * metadata tier — the scalar {@code dev}/{@code deprecated}/{@code license}/{@code hasInstallScript},
     * the {@code os}/{@code cpu}/{@code libc} arrays, an {@code engines} object, an object-form {@code bin},
     * a string-form {@code funding}, and now the {@code peerDependencies}/{@code peerDependenciesMeta} maps
     * (recorded verbatim) — is written through.
     * <p>
     * A peer marked optional (via {@code peerDependenciesMeta[x].optional}) is <b>skipped</b>: npm does not
     * auto-install it, so the closure walk never enqueues it; the entry still records the maps verbatim.
     * A <b>non-optional</b> peer npm auto-installs (placed top-level with a {@code peer: true} marker whose
     * reachability propagation needs the full hoisting model) fails loud — the I5-adjacent boundary. So do
     * {@code optionalDependencies} (installed + {@code optional}-marked), a string {@code bin} or non-string
     * {@code funding} npm reshapes, or bundled/shrinkwrap/accept/workspaces (exhaustive-or-fail).
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

    private static LockEditSet.@Nullable WriteThroughMetadata leafMetadata(VersionManifest m) {
        boolean peers = notEmpty(m.getPeerDependencies()) || nonEmptyObject(m.getPeerDependenciesMeta());
        boolean any = m.getLicenseString() != null || m.getDeprecated() != null || notEmpty(m.getEngines()) ||
                notEmpty(m.getOs()) || notEmpty(m.getCpu()) || notEmpty(m.getLibc()) ||
                bool(m.getHasInstallScript()) || m.getBin() != null || m.getFunding() != null || peers;
        if (!any) {
            return null;
        }
        return LockEditSet.WriteThroughMetadata.builder()
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
     * The strict layout whitelist for a bump, minus the {@code dependencies} surface (which I3's cascade
     * handles). The two manifests must agree on every OTHER closure-affecting surface; only the
     * write-through tier (engines/license/deprecated/bin) may differ.
     */
    private static void proveNonDependencySurfacesUnchanged(String name, VersionManifest oldM, VersionManifest newM) {
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
        // The single-locked-version guard plus the cascade's reverse-edge check reject the common triggers.
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
     * Phase B increment 5: a direct-dependency bump whose new version a reverse-dependent's recorded
     * constraint excludes. npm keeps the new version at the top-level slot (the direct dep wins) and nests
     * the old version under the reverse-dependent ({@code node_modules/<dependent>/node_modules/<name>}, plus
     * the v2 legacy tree). This plans only the accuracy-safe slice — a single top-level reverse-dependent, a
     * constraint still resolving to the currently-locked version, and a leaf being nested — so the patcher
     * relocates the pre-edit entry byte-for-byte. Anything wider (multiple nesters, a newer in-range nested
     * version, a non-leaf, a dev/link entry) is where npm reshapes further, so it fails loud.
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
                .nestedUnder(dependentName)
                .build());
    }

    /** Installed lock entries whose recorded constraint on {@code name} excludes {@code target}, keyed by their packages key. */
    private static Map<String, String> conflictingReverseDependentsNpm(String lock, String name, String target) {
        Map<String, String> conflicts = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : packagesMap(lock).entrySet()) {
            // Importer entries are the user's package.json, re-pinned by the patcher — never a nest trigger.
            if (!e.getKey().contains("node_modules/") || !(e.getValue() instanceof Map)) {
                continue;
            }
            String c = firstExcludingConstraint((Map<?, ?>) e.getValue(), name, target);
            if (c != null) {
                conflicts.put(e.getKey(), c);
            }
        }
        return conflicts;
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
     * The bun analogue of {@link #planReverseDependentNestsNpm}: bun hoists like npm, so the same single-leaf
     * slice applies — the old version relocates to a {@code "<dependent>/<name>"} tuple. The reads differ (bun's
     * {@code packages} is keyed by name with {@code ["name@ver", "", metadata, sri]} tuples).
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
     * A closure-unchanged proof only inspects the moving package's own manifest; it says nothing about the
     * OTHER locked entries that depend on it. If a reverse-dependent's recorded constraint excludes the new
     * version, re-pinning would emit a lock a real install rejects — fail loud instead.
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
            default:
                // yarn merges selectors into shared headers; the single-locked-version guard covers the common case.
                break;
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

    private static LockEditSet.@Nullable WriteThroughMetadata writeThrough(VersionManifest oldM, VersionManifest newM) {
        LockEditSet.WriteThroughMetadata.WriteThroughMetadataBuilder b = LockEditSet.WriteThroughMetadata.builder();
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
    private static @Nullable String findImporterDir(PackageManager pm, String lock, String name, String scope,
                                                    @Nullable String oldConstraint) {
        switch (pm) {
            case Npm:
            case Bun:
                return findImporterDirJson(pm, lock, name, oldConstraint);
            case Pnpm:
                return findImporterDirPnpm(lock, name, oldConstraint);
            default:
                return null; // yarn has no importer concept in its lock
        }
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
                            return null; // ambiguous — fall back to the root importer
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
