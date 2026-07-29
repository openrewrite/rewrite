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
package org.openrewrite.javascript.internal.npmlock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;
import org.openrewrite.javascript.internal.registry.NpmRegistryConfig;
import org.openrewrite.javascript.internal.registry.NpmRegistryException;
import org.openrewrite.javascript.internal.registry.Packument;
import org.openrewrite.javascript.internal.semver.NpmRange;
import org.openrewrite.javascript.internal.npmlock.NpmLockTree.Edge;
import org.openrewrite.javascript.internal.npmlock.NpmLockTree.Flags;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Native {@code package-lock.json} regeneration: applies a recipe's manifest edit to
 * the lock without executing npm, consulting the project's registry only for the
 * packages the edit actually moves.
 *
 * <p>The lock's root {@code ""} entry mirrors the manifest npm last resolved, so
 * diffing the edited manifest against it yields the edit set for free. The engine
 * handles the provable subset — a version move whose closure stays satisfied by the
 * existing tree, removals with an orphan sweep, top-level additions that fit without
 * placement conflicts, range edits the current pin already satisfies, and flat
 * {@code overrides} retargeting a single copy — and fails loudly with a structured
 * {@link Failure} on everything else (cascading placement, peer conflicts, workspace
 * locks, non-registry specs, lockfileVersion &lt; 3). On failure the old lock is left
 * byte-untouched and the stale pin is caught by {@code npm ci} in CI.
 *
 * <p>Dependency flags ({@code dev}/{@code optional}/{@code devOptional}/{@code peer})
 * are recomputed with arborist's own fixed-point algorithm, and applied only where
 * the edit changed an entry's color — pre-existing drift elsewhere in the file is
 * preserved byte-for-byte, never "fixed" as a side effect.
 */
public final class NpmLockEngine {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String[] DEP_SCOPES = {
            "dependencies", "devDependencies", "optionalDependencies", "peerDependencies"};

    private NpmLockEngine() {
    }

    public static Result regenerate(String packageJsonContent,
                                    @Nullable String originalPackageJsonContent,
                                    String lockContent,
                                    @Nullable String npmrcContent,
                                    ExecutionContext ctx) {
        try {
            return new Run(packageJsonContent, originalPackageJsonContent, lockContent, npmrcContent, ctx).run();
        } catch (EngineFailure e) {
            return Result.failure(e.getFailure());
        } catch (NpmRegistryException e) {
            return Result.failure(new Failure(mapRegistryReason(e.getReason()), null, e.getRegistryUrl(), e.getMessage()));
        }
    }

    private static Reason mapRegistryReason(NpmRegistryException.Reason reason) {
        switch (reason) {
            case AUTH_FAILED:
                return Reason.AUTH_FAILED;
            case NOT_FOUND:
                return Reason.PACKAGE_NOT_FOUND;
            case CONFIG:
                return Reason.MALFORMED_MANIFEST;
            default:
                return Reason.REGISTRY_UNREACHABLE;
        }
    }

    private static class Run {
        private final ObjectNode manifest;
        private final @Nullable ObjectNode originalManifest;
        private final String lockContent;
        private final String indent;
        private final String eol;
        private final ObjectNode lock;
        private final ObjectNode packages;
        private final NpmLockTree tree;
        private final NpmRegistryClient registry;
        private final Map<String, NpmRange> overrides;
        private final Set<String> rebuiltPaths = new LinkedHashSet<>();

        Run(String packageJsonContent, @Nullable String originalPackageJsonContent,
            String lockContent, @Nullable String npmrcContent, ExecutionContext ctx) {
            this.manifest = parseObject(packageJsonContent, Reason.MALFORMED_MANIFEST, "package.json");
            this.originalManifest = originalPackageJsonContent == null ? null
                    : parseObject(originalPackageJsonContent, Reason.MALFORMED_MANIFEST, "original package.json");
            this.lockContent = lockContent;
            this.indent = NpmLockWriter.detectIndent(lockContent);
            this.eol = NpmLockWriter.detectEol(lockContent);
            this.lock = parseObject(lockContent, Reason.MALFORMED_LOCK, "package-lock.json");
            JsonNode packagesNode = lock.get("packages");
            if (!(packagesNode instanceof ObjectNode)) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, null, "lock file is missing the `packages` map");
            }
            this.packages = (ObjectNode) packagesNode;
            this.tree = new NpmLockTree(packages);
            HttpSender http = HttpSenderExecutionContextView.view(ctx).getHttpSender();
            this.registry = new NpmRegistryClient(http,
                    new NpmRegistryConfig(NpmRegistryConfig.parseNpmrc(npmrcContent), ctx));
            this.overrides = parseOverrides();
        }

        private static ObjectNode parseObject(String content, Reason reason, String what) {
            try {
                JsonNode node = MAPPER.readTree(content);
                if (node instanceof ObjectNode) {
                    return (ObjectNode) node;
                }
                throw new EngineFailure(reason, null, what + " is not a JSON object");
            } catch (EngineFailure e) {
                throw e;
            } catch (Exception e) {
                throw new EngineFailure(reason, null, "malformed " + what + ": " + e.getMessage());
            }
        }

        Result run() {
            guardSupported();

            Map<String, Flags> oldFlags = tree.calcDepFlags();
            Set<String> editedNames = editedNames();
            applyOverrideEdits();

            for (String name : editedNames) {
                applyDependencyEdit(name);
            }
            syncRootEntry(editedNames);
            syncTopLevel();

            Map<String, Flags> newFlags = tree.calcDepFlags();
            sweepOrphans(oldFlags, newFlags);
            reconcileFlags(oldFlags, newFlags);

            String out = NpmLockWriter.write(lock, indent, eol);
            return Result.success(out);
        }

        // --- Guards --------------------------------------------------------

        private void guardSupported() {
            int lockfileVersion = lock.path("lockfileVersion").asInt(-1);
            if (lockfileVersion != 3) {
                throw new EngineFailure(Reason.UNSUPPORTED_LOCK_VERSION, null,
                        "lockfileVersion " + (lockfileVersion < 0 ? "(absent)" : lockfileVersion) +
                                " is not supported natively; only v3 (npm 9+) locks are. A CI `npm install` " +
                                "will upgrade the lock format");
            }
            if (manifest.has("workspaces") || root().has("workspaces")) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                        "workspace projects resolve across multiple manifests");
            }
            for (String path : tree.paths()) {
                ObjectNode entry = tree.entry(path);
                if (entry != null && entry.path("link").asBoolean(false)) {
                    throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, NpmLockTree.nameOf(path),
                            "symlinked (`link: true`) entries require filesystem resolution");
                }
            }
            String roundTrip = NpmLockWriter.write(lock, indent, eol);
            if (!roundTrip.equals(lockContent)) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, null,
                        "lock file is not in canonical npm form (hand-edited or written by an older npm?); " +
                                "refusing to rewrite it wholesale");
            }
        }

        private ObjectNode root() {
            ObjectNode root = tree.entry("");
            if (root == null) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, null, "lock file has no root \"\" entry");
            }
            return root;
        }

        // --- Edit-set computation ------------------------------------------

        /**
         * Names whose manifest declaration disagrees with the lock's recorded root
         * entry. The lock root is the baseline (not the pre-edit manifest) because
         * regeneration always starts from the lock captured at scan time: a prior
         * recipe's edit in the same run must be reconciled too, or it would be lost
         * when this recipe's output replaces the lock.
         */
        private Set<String> editedNames() {
            ObjectNode baseline = root();
            Set<String> names = new LinkedHashSet<>();
            for (String scope : DEP_SCOPES) {
                JsonNode before = baseline.path(scope);
                JsonNode after = manifest.path(scope);
                before.fieldNames().forEachRemaining(n -> {
                    if (!Objects.equals(before.path(n).asText(null), after.path(n).asText(null))) {
                        names.add(n);
                    }
                });
                after.fieldNames().forEachRemaining(n -> {
                    if (!Objects.equals(before.path(n).asText(null), after.path(n).asText(null))) {
                        names.add(n);
                    }
                });
            }
            return names;
        }

        private Map<String, NpmRange> parseOverrides() {
            Map<String, NpmRange> parsed = new LinkedHashMap<>();
            JsonNode overridesNode = manifest.path("overrides");
            if (overridesNode.isMissingNode()) {
                return parsed;
            }
            if (!overridesNode.isObject()) {
                throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, null, "non-object `overrides`");
            }
            overridesNode.fields().forEachRemaining(e -> {
                String key = e.getKey();
                JsonNode value = e.getValue();
                if (!value.isTextual() || key.contains("@") && key.lastIndexOf('@') > 0 || value.asText().startsWith("$")) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, key,
                            "only flat `\"package\": \"range\"` overrides are supported natively");
                }
                NpmRange range = NpmRange.parse(value.asText());
                if (range == null) {
                    throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, key,
                            "override target `" + value.asText() + "` is not a semver range");
                }
                parsed.put(key, range);
            });
            return parsed;
        }

        /**
         * An override whose value changed since the pre-edit manifest (the
         * {@code UpgradeTransitiveDependencyVersion} path) is a no-op when every copy
         * of the package already satisfies it. When it would actually move a pin, npm
         * re-places the overridden copies (nesting them under their dependents, as the
         * recorded {@code override} fixture shows) — a placement decision this engine
         * refuses to guess.
         */
        private void applyOverrideEdits() {
            JsonNode before = originalManifest != null ? originalManifest.path("overrides")
                    : MAPPER.createObjectNode();
            for (Map.Entry<String, NpmRange> override : overrides.entrySet()) {
                String name = override.getKey();
                if (originalManifest != null &&
                        Objects.equals(before.path(name).asText(null),
                                manifest.path("overrides").path(name).asText(null))) {
                    continue;
                }
                for (String path : tree.paths()) {
                    if (!name.equals(NpmLockTree.nameOf(path))) {
                        continue;
                    }
                    ObjectNode entry = tree.entry(path);
                    String pinned = entry == null ? null : entry.path("version").asText(null);
                    if (pinned == null || !override.getValue().satisfies(pinned)) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                                "override `" + name + "` -> `" + override.getValue() +
                                        "` moves " + name + "@" + pinned + " at " + path +
                                        "; npm re-places overridden copies, which requires full resolution");
                    }
                }
            }
        }

        // --- Edit application ----------------------------------------------

        private void applyDependencyEdit(String name) {
            String newRange = declaredRange(manifest, name);
            if (newRange == null) {
                return; // removal: the root-entry sync and orphan sweep complete it
            }
            NpmRange range = NpmRange.parse(effectiveRange(name, newRange));
            if (range == null) {
                throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, name,
                        "`" + newRange + "` is not a registry semver range; git, file, alias and " +
                                "tarball dependencies are not resolved natively");
            }
            String location = tree.resolve("", name);
            if (location == null) {
                addEntry(name, range);
                return;
            }
            ObjectNode entry = tree.entry(location);
            if (entry == null) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, name, "unresolvable entry at " + location);
            }
            if (entry.has("name") && !name.equals(entry.path("name").asText())) {
                throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, name,
                        "aliased package at " + location);
            }
            String pinned = entry.path("version").asText(null);
            if (pinned != null && range.satisfies(pinned)) {
                return; // npm keeps a still-valid pin; only the recorded ranges change
            }
            moveEntry(location, name, range);
        }

        private @Nullable String declaredRange(ObjectNode source, String name) {
            for (String scope : DEP_SCOPES) {
                JsonNode range = source.path(scope).get(name);
                if (range != null) {
                    return range.asText();
                }
            }
            return null;
        }

        private String effectiveRange(String name, String declared) {
            NpmRange override = overrides.get(name);
            return override != null ? override.toString() : declared;
        }

        /** Re-resolve the entry at {@code location} to the best version for {@code range}. */
        private void moveEntry(String location, String name, NpmRange range) {
            guardNoBundledChildren(location, name);
            Packument packument = registry.packument(name);
            String chosen = NpmRange.pickVersion(packument.versions().keySet(), packument.latestTag(), range);
            if (chosen == null) {
                throw new EngineFailure(Reason.VERSION_NOT_FOUND, name,
                        "no published version of " + name + " satisfies `" + range + "`");
            }
            ObjectNode current = tree.entry(location);
            if (current != null && chosen.equals(current.path("version").asText(null))) {
                return;
            }
            ObjectNode versionManifest = packument.version(chosen);
            if (versionManifest == null) {
                throw new EngineFailure(Reason.VERSION_NOT_FOUND, name, name + "@" + chosen + " has no manifest");
            }
            ObjectNode entry = buildEntry(name, versionManifest);
            packages.set(location, entry);
            rebuiltPaths.add(location);
            verifyClosure(location, name, chosen);
            verifyDependents(location, name, chosen);
        }

        /** Add {@code name} as a new top-level entry, npm's maximally-hoisted placement. */
        private void addEntry(String name, NpmRange range) {
            Packument packument = registry.packument(name);
            String chosen = NpmRange.pickVersion(packument.versions().keySet(), packument.latestTag(), range);
            if (chosen == null) {
                throw new EngineFailure(Reason.VERSION_NOT_FOUND, name,
                        "no published version of " + name + " satisfies `" + range + "`");
            }
            ObjectNode versionManifest = packument.version(chosen);
            if (versionManifest == null) {
                throw new EngineFailure(Reason.VERSION_NOT_FOUND, name, name + "@" + chosen + " has no manifest");
            }
            String location = "node_modules/" + name;
            packages.set(location, buildEntry(name, versionManifest));
            rebuiltPaths.add(location);
            verifyClosure(location, name, chosen);
        }

        private void guardNoBundledChildren(String location, String name) {
            String prefix = location + "/node_modules/";
            for (String path : tree.paths()) {
                if (path.startsWith(prefix)) {
                    ObjectNode child = tree.entry(path);
                    if (child != null && child.path("inBundle").asBoolean(false)) {
                        throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, name,
                                name + " ships bundled dependencies, whose content is only " +
                                        "knowable from the tarball");
                    }
                }
            }
        }

        /**
         * The Tier-0 proof: every requirement of the moved/added entry must already be
         * satisfied by the tree visible from its location. Anything else would require
         * placing new packages or moving pins npm chose — full resolution territory.
         */
        private void verifyClosure(String location, String name, String version) {
            for (Edge edge : tree.edges(location)) {
                NpmRange edgeRange = NpmRange.parse(effectiveRange(edge.name, edge.range));
                if (edgeRange == null) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                            name + "@" + version + " depends on " + edge.name + "@`" + edge.range +
                                    "`, which is not a registry semver range");
                }
                String target = tree.resolve(location, edge.name);
                if (target == null) {
                    if (edge.optional) {
                        continue;
                    }
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                            name + "@" + version + " requires " + edge.name + "@`" + edge.range +
                                    "`, which is not in the tree; installing it needs full resolution");
                }
                ObjectNode targetEntry = tree.entry(target);
                String targetVersion = targetEntry == null ? null : targetEntry.path("version").asText(null);
                if (targetEntry != null && targetEntry.has("name")) {
                    throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, name,
                            edge.name + " resolves to an aliased package at " + target);
                }
                if (targetVersion == null || !edgeRange.satisfies(targetVersion)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                            name + "@" + version + " requires " + edge.name + "@`" + edge.range +
                                    "` but the tree pins " + edge.name + "@" + targetVersion +
                                    (edge.peer ? " (peer dependency conflict)" : "; the cascade needs full resolution"));
                }
            }
        }

        /**
         * The reverse proof: every entry that resolves this location for its own
         * requirement must still accept the new version. Where it would not, npm nests
         * a fresh copy for that dependent — a placement decision we refuse to guess.
         */
        private void verifyDependents(String location, String name, String version) {
            for (String path : tree.paths()) {
                if (path.equals(location)) {
                    continue;
                }
                for (Edge edge : tree.edges(path)) {
                    if (!edge.name.equals(name) || !location.equals(tree.resolve(path, edge.name))) {
                        continue;
                    }
                    NpmRange edgeRange = NpmRange.parse(effectiveRange(edge.name, edge.range));
                    if (edgeRange == null || !edgeRange.satisfies(version)) {
                        if (path.isEmpty()) {
                            continue; // the root's own edge is the edit being applied
                        }
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                                NpmLockTree.nameOf(path) + " requires " + name + "@`" + edge.range +
                                        "`, which " + name + "@" + version + " no longer satisfies; " +
                                        "npm would nest a second copy");
                    }
                }
            }
        }

        // --- Entry construction --------------------------------------------

        /**
         * The manifest fields npm copies into a lock entry ({@code pkgMetaKeys} in
         * arborist's shrinkwrap.js), with its quirks: falsy values and empty objects
         * are skipped, and a license object collapses to its {@code type}.
         */
        private static final String[] PKG_META_KEYS = {
                "version", "dependencies", "peerDependencies", "peerDependenciesMeta",
                "optionalDependencies", "bundleDependencies", "acceptDependencies",
                "funding", "engines", "os", "cpu", "libc", "license", "bin", "deprecated", "workspaces"};

        private ObjectNode buildEntry(String name, ObjectNode versionManifest) {
            if (truthy(versionManifest.get("bundleDependencies")) || truthy(versionManifest.get("bundledDependencies"))) {
                throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, name,
                        name + " bundles dependencies inside its tarball");
            }
            ObjectNode entry = MAPPER.createObjectNode();
            for (String key : PKG_META_KEYS) {
                JsonNode value = versionManifest.get(key);
                if (value == null || !truthy(value) || (value.isObject() && value.isEmpty())) {
                    continue;
                }
                if ("license".equals(key) && value.isObject() && value.hasNonNull("type")) {
                    entry.set("license", value.get("type"));
                } else if ("funding".equals(key) && value.isTextual()) {
                    entry.putObject("funding").put("url", value.textValue());
                } else if ("bin".equals(key) && value.isTextual()) {
                    String binName = versionManifest.path("name").asText(name);
                    entry.putObject("bin").put(binName, value.textValue());
                } else {
                    entry.set(key, value.deepCopy());
                }
            }
            JsonNode scripts = versionManifest.path("scripts");
            if (versionManifest.path("hasInstallScript").asBoolean(false) ||
                    scripts.has("install") || scripts.has("preinstall") || scripts.has("postinstall")) {
                entry.put("hasInstallScript", true);
            }
            JsonNode dist = versionManifest.path("dist");
            String tarball = dist.path("tarball").asText(null);
            if (tarball == null) {
                throw new EngineFailure(Reason.INTEGRITY_UNAVAILABLE, name,
                        "registry supplies no tarball URL for " + name);
            }
            entry.put("resolved", tarball);
            String integrity = dist.path("integrity").asText(null);
            if (integrity == null) {
                String shasum = dist.path("shasum").asText(null);
                if (shasum == null) {
                    throw new EngineFailure(Reason.INTEGRITY_UNAVAILABLE, name,
                            "registry supplies neither integrity nor shasum for " + name);
                }
                integrity = "sha1-" + Base64.getEncoder().encodeToString(hexToBytes(shasum));
            }
            entry.put("integrity", integrity);
            return entry;
        }

        private static boolean truthy(@Nullable JsonNode node) {
            if (node == null || node.isNull() || node.isMissingNode()) {
                return false;
            }
            if (node.isBoolean()) {
                return node.booleanValue();
            }
            if (node.isTextual()) {
                return !node.textValue().isEmpty();
            }
            if (node.isNumber()) {
                return node.doubleValue() != 0;
            }
            return true;
        }

        private static byte[] hexToBytes(String hex) {
            byte[] out = new byte[hex.length() / 2];
            for (int i = 0; i < out.length; i++) {
                out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
            return out;
        }

        // --- Root entry and top-level sync ---------------------------------

        /**
         * Mirror the manifest's declaration of each edited name into the root entry's
         * dependency blocks. Only edited names are touched: unrelated drift between the
         * manifest and the recorded root entry is preserved as-is.
         */
        private void syncRootEntry(Set<String> editedNames) {
            ObjectNode root = root();
            for (String scope : DEP_SCOPES) {
                for (String name : editedNames) {
                    JsonNode declared = manifest.path(scope).get(name);
                    JsonNode block = root.get(scope);
                    if (declared != null) {
                        ObjectNode target = block instanceof ObjectNode ? (ObjectNode) block
                                : root.putObject(scope);
                        target.set(name, declared.deepCopy());
                    } else if (block instanceof ObjectNode) {
                        ((ObjectNode) block).remove(name);
                        if (block.isEmpty()) {
                            root.remove(scope);
                        }
                    }
                }
            }
        }

        private void syncTopLevel() {
            JsonNode name = manifest.get("name");
            if (name != null && name.isTextual()) {
                lock.set("name", name.deepCopy());
                root().set("name", name.deepCopy());
            }
            JsonNode version = manifest.get("version");
            if (version != null && version.isTextual()) {
                lock.set("version", version.deepCopy());
                root().set("version", version.deepCopy());
            }
        }

        // --- Sweep and flags -----------------------------------------------

        /**
         * Remove entries the edit orphaned. Entries that were already unreachable
         * before the edit are left in place — that drift predates the recipe.
         */
        private void sweepOrphans(Map<String, Flags> oldFlags, Map<String, Flags> newFlags) {
            for (String path : tree.paths()) {
                if (!path.isEmpty() && oldFlags.containsKey(path) && !newFlags.containsKey(path)) {
                    packages.remove(path);
                }
            }
        }

        /**
         * Apply recomputed dep flags only where the edit changed an entry's color. When
         * a color change lands on an entry whose recorded flags already disagreed with
         * the recomputed before-state, our model of the tree cannot be trusted for it.
         */
        private void reconcileFlags(Map<String, Flags> oldFlags, Map<String, Flags> newFlags) {
            for (String path : tree.paths()) {
                if (path.isEmpty()) {
                    continue;
                }
                ObjectNode entry = tree.entry(path);
                Flags after = newFlags.get(path);
                if (entry == null || after == null) {
                    continue;
                }
                Flags before = oldFlags.get(path);
                if (before == null || rebuiltPaths.contains(path)) {
                    NpmLockTree.applyFlags(entry, after);
                } else if (!sameColor(before, after)) {
                    if (!NpmLockTree.sameFlags(entry, before)) {
                        throw new EngineFailure(Reason.MALFORMED_LOCK, NpmLockTree.nameOf(path),
                                "recorded dev/optional flags of " + path + " disagree with the " +
                                        "recorded dependency graph");
                    }
                    NpmLockTree.applyFlags(entry, after);
                }
            }
        }

        private static boolean sameColor(Flags a, Flags b) {
            return a.dev == b.dev && a.optional == b.optional &&
                    a.devOptional == b.devOptional && a.peer == b.peer;
        }
    }
}
