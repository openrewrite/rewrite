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
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;
import org.openrewrite.javascript.internal.registry.VersionManifest;

import java.util.*;

import static org.openrewrite.javascript.internal.lock.YarnLock.unwrap;

/**
 * Diffs a freshly resolved {@link ResolutionGraph} against the existing classic {@code yarn.lock} (v1) and
 * expresses the difference as {@link PackageEdit}s for {@link YarnClassicLockPatcher} — so untouched blocks keep
 * their bytes and only what the resolution actually changed is rewritten. yarn v1 is flat: one block per resolved
 * {@code name@version}, headed by every {@code name@range} selector that resolves to it. Nodes match blocks of the
 * same name version-by-version (a lone leftover pair is an in-place move); a matched block whose selector set
 * gained a range merges it in place, unmatched graph nodes become sorted block inserts, and unmatched lock blocks
 * are removals. yarn records no peer surface at all, so a satisfied-peer closure diffs like a peer-free one. A
 * difference the patcher cannot express byte-exactly — a merged header that must split, a block gaining a
 * dependency line, an {@code optionalDependencies} section — fails loud rather than guess.
 */
final class YarnClassicLockDiff {

    /** The scope precedence {@code LockManifests.declaredConstraint} uses when the patcher re-reads the manifest. */
    private static final List<String> MANIFEST_SCOPES = Arrays.asList(
            "dependencies", "devDependencies", "peerDependencies", "optionalDependencies");

    private YarnClassicLockDiff() {
    }

    static List<PackageEdit> diff(ResolutionGraph graph, String existingLock) {
        ResolutionGraph.Importer root = singleRootImporter(graph);
        requireNoAliases(graph);
        Lock lock = Lock.parse(existingLock);
        Map<String, Set<String>> selectors = selectorsByNode(graph, root);

        Map<String, List<String>> nodesByName = new LinkedHashMap<>();
        for (String nodeKey : graph.getNodes().keySet()) {
            nodesByName.computeIfAbsent(nameOf(nodeKey), k -> new ArrayList<>()).add(nodeKey);
        }
        Set<String> names = new LinkedHashSet<>(nodesByName.keySet());
        names.addAll(lock.blocksByName.keySet());

        List<PackageEdit> edits = new ArrayList<>();
        List<String> fresh = new ArrayList<>();
        List<Block> orphaned = new ArrayList<>();
        for (String name : names) {
            matchName(graph, root, selectors, name,
                    nodesByName.getOrDefault(name, Collections.emptyList()),
                    lock.blocksByName.getOrDefault(name, Collections.emptyList()),
                    edits, fresh, orphaned);
        }
        for (String nodeKey : fresh) {
            edits.add(addEdit(graph, root, selectors, nodeKey, fresh));
        }
        for (Block block : orphaned) {
            for (String range : block.ranges) {
                edits.add(PackageEdit.builder()
                        .name(block.name)
                        .oldVersion(block.version)
                        .newVersion(null)
                        .oldConstraint(range)
                        .scope("dependencies")
                        .importerDir(null)
                        .build());
            }
        }
        return edits;
    }

    /**
     * Bind one name's resolved versions to its installed blocks: exact version matches first, then a lone
     * leftover pair binds as an in-place move; leftover nodes place fresh, leftover blocks are removals.
     */
    private static void matchName(ResolutionGraph graph, ResolutionGraph.Importer root,
                                  Map<String, Set<String>> selectors, String name,
                                  List<String> nodeKeys, List<Block> lockBlocks,
                                  List<PackageEdit> edits, List<String> fresh, List<Block> orphaned) {
        List<String> nodes = new ArrayList<>(nodeKeys);
        List<Block> blocks = new ArrayList<>(lockBlocks);

        for (Iterator<String> it = nodes.iterator(); it.hasNext(); ) {
            String nodeKey = it.next();
            String version = versionOf(nodeKey);
            List<Block> matches = new ArrayList<>();
            for (Block block : blocks) {
                if (version.equals(block.version)) {
                    matches.add(block);
                }
            }
            if (matches.size() > 1) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        name + "@" + version + " heads more than one lock block");
            }
            if (matches.size() == 1) {
                PackageEdit edit = boundEdit(graph, root, selectors, name, nodeKey, matches.get(0));
                if (edit != null) {
                    edits.add(edit);
                }
                blocks.remove(matches.get(0));
                it.remove();
            }
        }
        if (nodes.size() == 1 && blocks.size() == 1) {
            edits.add(moveEdit(graph, root, selectors, name, nodes.get(0), blocks.get(0)));
            return;
        }
        if (!nodes.isEmpty() && !blocks.isEmpty()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " resolves to versions whose lock blocks cannot be matched unambiguously");
        }
        fresh.addAll(nodes);
        orphaned.addAll(blocks);
    }

    /**
     * The edit for a node whose block already holds its version: nothing when the selector set matches; a
     * selector-merge when the block only gained a range; a header re-pin when a lone selector was replaced.
     * A merged header that must drop a selector needs splitting, which defers.
     */
    private static @Nullable PackageEdit boundEdit(ResolutionGraph graph, ResolutionGraph.Importer root,
                                                   Map<String, Set<String>> selectors, String name,
                                                   String nodeKey, Block block) {
        Set<String> expected = expectedSelectors(selectors, name, nodeKey);
        Set<String> installed = new LinkedHashSet<>(block.ranges);
        if (expected.equals(installed)) {
            return null;
        }
        Set<String> lost = new LinkedHashSet<>(installed);
        lost.removeAll(expected);
        Set<String> gained = new LinkedHashSet<>(expected);
        gained.removeAll(installed);
        String version = versionOf(nodeKey);
        String scope = declaringScope(root, name);

        if (lost.isEmpty()) {
            if (gained.size() > 1) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        name + " gains more than one selector; resolution required");
            }
            String range = gained.iterator().next();
            String patcherRange = patcherDeclaredRange(root, scope, name);
            if (patcherRange != null && !patcherRange.equals(range)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        name + " gains a selector the edited manifest does not declare; resolution required");
            }
            return PackageEdit.builder()
                    .name(name)
                    .oldVersion(block.version)
                    .newVersion(version)
                    .newConstraint(range)
                    .scope(scope)
                    .importerDir(null)
                    .kind(PackageEdit.Kind.PROMOTION)
                    .build();
        }
        if (installed.size() == 1 && expected.size() == 1) {
            return PackageEdit.builder()
                    .name(name)
                    .oldVersion(block.version)
                    .newVersion(version)
                    .oldConstraint(block.ranges.get(0))
                    .newConstraint(expected.iterator().next())
                    .scope(scope)
                    .importerDir(null)
                    .kind(PackageEdit.Kind.FORCED_MOVE)
                    .build();
        }
        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                name + " keeps a merged header that must drop a selector; resolution required");
    }

    /**
     * A lone unmatched node/block pair of one name moves in place: a bump when the root declares it, a
     * forced move for a transitive. The patcher rewrites only a single-selector header, never adds a
     * dependency line, and has no verified {@code optionalDependencies} serialization — those defer.
     */
    private static PackageEdit moveEdit(ResolutionGraph graph, ResolutionGraph.Importer root,
                                        Map<String, Set<String>> selectors, String name,
                                        String nodeKey, Block block) {
        Set<String> expected = expectedSelectors(selectors, name, nodeKey);
        if (block.ranges.size() != 1 || expected.size() != 1) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " moves on a merged selector list; resolution required");
        }
        ResolvedNode node = graph.getNodes().get(nodeKey);
        VersionManifest m = node.getManifest();
        if (block.hasOptionalSection || notEmpty(m.getOptionalDependencies())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " has an optionalDependencies section, which is not yet patched");
        }
        Set<String> newDeps = m.getDependencies() == null ? Collections.emptySet() : m.getDependencies().keySet();
        if (!block.depNames.containsAll(newDeps)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " gains a dependency edge on upgrade, which is not yet patched");
        }

        String version = versionOf(nodeKey);
        String newRange = expected.iterator().next();
        String scope = declaringScope(root, name);
        boolean rootDeclared = version.equals(root.getResolved().get(name)) && declaredRange(root, name) != null;
        if (rootDeclared && !newRange.equals(patcherDeclaredRange(root, scope, name))) {
            // applyEdit re-reads the new selector from the edited manifest; it must agree with the resolution.
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " moves under a selector the edited manifest does not declare; resolution required");
        }
        VersionManifest.Dist dist = requireLocator(m);
        return PackageEdit.builder()
                .name(name)
                .oldVersion(block.version)
                .newVersion(version)
                .newResolved(dist.getTarball())
                .newIntegrity(dist.getIntegrity())
                .newShasum(dist.getShasum())
                .newDependencies(notEmpty(m.getDependencies()) ? m.getDependencies() : null)
                .oldConstraint(block.ranges.get(0))
                .newConstraint(rootDeclared ? null : newRange)
                .scope(scope)
                .importerDir(null)
                .kind(rootDeclared ? PackageEdit.Kind.BUMP : PackageEdit.Kind.FORCED_MOVE)
                .prunesOrphans(!newDeps.containsAll(block.depNames))
                .build();
    }

    /**
     * A fresh node inserts as a new sorted block. The patcher derives the header from the edited manifest's
     * declared range plus the sibling inserts' dependency ranges, so that derivation must land exactly on the
     * resolved selector set; any other requirer shape defers.
     */
    private static PackageEdit addEdit(ResolutionGraph graph, ResolutionGraph.Importer root,
                                       Map<String, Set<String>> selectors, String nodeKey, List<String> fresh) {
        String name = nameOf(nodeKey);
        VersionManifest m = graph.getNodes().get(nodeKey).getManifest();
        if (notEmpty(m.getOptionalDependencies())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + "@" + m.getVersion() + " declares optionalDependencies, which is not yet patched");
        }
        Set<String> expected = expectedSelectors(selectors, name, nodeKey);
        String scope = declaringScope(root, name);
        Set<String> derivable = new LinkedHashSet<>();
        String declared = patcherDeclaredRange(root, scope, name);
        if (declared != null) {
            derivable.add(declared);
        }
        for (String sibling : fresh) {
            Map<String, String> deps = graph.getNodes().get(sibling).getManifest().getDependencies();
            if (deps != null && deps.containsKey(name)) {
                derivable.add(deps.get(name));
            }
        }
        if (!derivable.equals(expected)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + "'s block header cannot be derived from the edit; resolution required");
        }
        VersionManifest.Dist dist = requireLocator(m);
        return PackageEdit.builder()
                .name(name)
                .oldVersion("")
                .newVersion(m.getVersion())
                .newResolved(dist.getTarball())
                .newIntegrity(dist.getIntegrity())
                .newShasum(dist.getShasum())
                .newDependencies(notEmpty(m.getDependencies()) ? m.getDependencies() : null)
                .scope(scope)
                .importerDir(null)
                .kind(PackageEdit.Kind.ADD)
                .build();
    }

    // --- selectors ---------------------------------------------------------

    /**
     * Map each resolved node to the ranges that resolve to it: the root's declared range (yarn installs no
     * peers, so {@code peerDependencies} contributes none) plus every requirer's dependency or
     * optional-dependency range whose edge resolves to this node.
     */
    private static Map<String, Set<String>> selectorsByNode(ResolutionGraph graph, ResolutionGraph.Importer root) {
        Map<String, Set<String>> byNode = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> scope : root.getDeclared().entrySet()) {
            if ("peerDependencies".equals(scope.getKey())) {
                continue;
            }
            for (Map.Entry<String, String> dep : scope.getValue().entrySet()) {
                String version = root.getResolved().get(dep.getKey());
                if (version != null) {
                    add(byNode, dep.getKey(), version, dep.getValue());
                }
            }
        }
        for (ResolvedNode node : graph.getNodes().values()) {
            Map<String, String> deps = node.getManifest().getDependencies();
            Map<String, String> optional = node.getManifest().getOptionalDependencies();
            for (Map.Entry<String, String> edge : node.getResolvedEdges().entrySet()) {
                String range = deps == null ? null : deps.get(edge.getKey());
                if (range == null && optional != null) {
                    range = optional.get(edge.getKey());
                }
                if (range != null) {
                    add(byNode, edge.getKey(), edge.getValue(), range);
                }
            }
        }
        return byNode;
    }

    private static void add(Map<String, Set<String>> byNode, String name, String version, String range) {
        byNode.computeIfAbsent(ResolutionGraph.key(name, version), k -> new LinkedHashSet<>()).add(range);
    }

    private static Set<String> expectedSelectors(Map<String, Set<String>> selectors, String name, String nodeKey) {
        Set<String> expected = selectors.get(nodeKey);
        if (expected == null || expected.isEmpty()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    nodeKey + " resolved but no selector requires it");
        }
        return expected;
    }

    // --- root declarations -------------------------------------------------

    /** The non-peer scope the root declares {@code name} in, defaulting to {@code dependencies} for a transitive. */
    private static String declaringScope(ResolutionGraph.Importer root, String name) {
        for (Map.Entry<String, Map<String, String>> scope : root.getDeclared().entrySet()) {
            if (scope.getValue().containsKey(name) && !"peerDependencies".equals(scope.getKey())) {
                return scope.getKey();
            }
        }
        return "dependencies";
    }

    private static @Nullable String declaredRange(ResolutionGraph.Importer root, String name) {
        for (Map.Entry<String, Map<String, String>> scope : root.getDeclared().entrySet()) {
            if (scope.getValue().containsKey(name) && !"peerDependencies".equals(scope.getKey())) {
                return scope.getValue().get(name);
            }
        }
        return null;
    }

    /** The range {@code LockManifests.declaredConstraint} yields the patcher for {@code name}, or {@code null}. */
    private static @Nullable String patcherDeclaredRange(ResolutionGraph.Importer root, String preferredScope,
                                                         String name) {
        Map<String, String> preferred = root.getDeclared().get(preferredScope);
        if (preferred != null && preferred.containsKey(name)) {
            return preferred.get(name);
        }
        for (String scope : MANIFEST_SCOPES) {
            Map<String, String> deps = root.getDeclared().get(scope);
            if (deps != null && deps.containsKey(name)) {
                return deps.get(name);
            }
        }
        return null;
    }

    // --- guards ------------------------------------------------------------

    private static ResolutionGraph.Importer singleRootImporter(ResolutionGraph graph) {
        if (graph.getImporters().size() != 1 || !graph.getImporters().get(0).getDir().isEmpty()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "only a single root importer can be diffed against the lock");
        }
        return graph.getImporters().get(0);
    }

    private static void requireNoAliases(ResolutionGraph graph) {
        for (Map.Entry<String, ResolvedNode> e : graph.getNodes().entrySet()) {
            String slot = nameOf(e.getKey());
            String realName = e.getValue().getManifest().getName();
            if (realName != null && !slot.equals(realName)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slot,
                        slot + " aliases " + realName + "; yarn alias blocks are not yet patched");
            }
        }
    }

    private static VersionManifest.Dist requireLocator(VersionManifest m) {
        VersionManifest.Dist dist = m.getDist();
        if (dist == null || dist.getTarball() == null || dist.getShasum() == null || dist.getIntegrity() == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, m.getName(),
                    m.getName() + "@" + m.getVersion() + " has no registry locator (tarball/shasum/integrity)");
        }
        return dist;
    }

    private static boolean notEmpty(@Nullable Map<String, String> m) {
        return m != null && !m.isEmpty();
    }

    // --- node keys ---------------------------------------------------------

    private static String nameOf(String nodeKey) {
        return nodeKey.substring(0, nodeKey.lastIndexOf('@'));
    }

    private static String versionOf(String nodeKey) {
        return nodeKey.substring(nodeKey.lastIndexOf('@') + 1);
    }

    // --- lock model --------------------------------------------------------

    /** One installed block: its selector ranges (header order), version, and dependency-section names. */
    private static final class Block {
        final String name;
        final List<String> ranges;
        final String version;
        final Set<String> depNames;
        final boolean hasOptionalSection;

        Block(String name, List<String> ranges, String version, Set<String> depNames, boolean hasOptionalSection) {
            this.name = name;
            this.ranges = ranges;
            this.version = version;
            this.depNames = depNames;
            this.hasOptionalSection = hasOptionalSection;
        }
    }

    /** The existing lock's blocks, grouped by package name; parsed once, structure only. */
    private static final class Lock {
        final Map<String, List<Block>> blocksByName = new LinkedHashMap<>();

        static Lock parse(String content) {
            if (!content.contains("# yarn lockfile v1")) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, null, "not a yarn v1 lockfile");
            }
            Lock lock = new Lock();
            List<String> headerTokens = null;
            String version = null;
            Set<String> depNames = new LinkedHashSet<>();
            boolean optionalSection = false;
            boolean inDeps = false;
            for (String line : content.split("\n", -1)) {
                if (!line.isEmpty() && !Character.isWhitespace(line.charAt(0)) && line.charAt(0) != '#' &&
                        line.endsWith(":")) {
                    lock.flush(headerTokens, version, depNames, optionalSection);
                    headerTokens = YarnClassicLockPatcher.splitSelectors(line.substring(0, line.length() - 1));
                    version = null;
                    depNames = new LinkedHashSet<>();
                    optionalSection = false;
                    inDeps = false;
                } else if (headerTokens != null) {
                    if (line.equals("  dependencies:") || line.equals("  optionalDependencies:")) {
                        inDeps = true;
                        optionalSection |= line.contains("optional");
                    } else if (inDeps && line.startsWith("    ")) {
                        depNames.add(unwrap(line.trim().split("\\s+", 2)[0]));
                    } else {
                        inDeps = false;
                        String trimmed = line.trim();
                        if (trimmed.startsWith("version ")) {
                            version = unwrap(trimmed.substring("version ".length()).trim());
                        }
                    }
                }
            }
            lock.flush(headerTokens, version, depNames, optionalSection);
            return lock;
        }

        private void flush(@Nullable List<String> headerTokens, @Nullable String version,
                           Set<String> depNames, boolean optionalSection) {
            if (headerTokens == null) {
                return;
            }
            String name = null;
            List<String> ranges = new ArrayList<>();
            for (String token : headerTokens) {
                String selector = unwrap(token);
                int at = selector.lastIndexOf('@');
                String selectorName = at > 0 ? selector.substring(0, at) : selector;
                if (name == null) {
                    name = selectorName;
                } else if (!name.equals(selectorName)) {
                    throw new EngineFailure(Reason.MALFORMED_LOCK, name,
                            "a lock block merges selectors of different packages");
                }
                ranges.add(at > 0 ? selector.substring(at + 1) : "");
            }
            if (version == null) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, name, name + "'s lock block has no version");
            }
            blocksByName.computeIfAbsent(name, k -> new ArrayList<>())
                    .add(new Block(name, ranges, version, depNames, optionalSection));
        }
    }
}
