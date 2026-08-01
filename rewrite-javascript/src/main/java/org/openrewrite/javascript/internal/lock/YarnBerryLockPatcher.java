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
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Byte-exact patcher for a Yarn Berry {@code yarn.lock} (the {@code __metadata:}-headed YAML format). The file
 * round-trips byte-identical through the rewrite-yaml LST, so rewriting only the named entries preserves every
 * other byte. A registry dependency's {@code checksum} — long the blocker for berry — is reproduced by
 * {@link BerryZipChecksum} and threaded in on the edit; the engine derives it before this patcher runs.
 * <p>
 * Supported: an in-place version bump (rewrite the entry's descriptor key, {@code version}, {@code resolution}
 * and {@code checksum} plus the importer range) and adding a dependency plus its runtime closure (a fresh entry
 * per member at its sorted position; scoped members are supported, their {@code @scope/name} keys sorting by
 * plain string like any other and quoted for YAML). A multi-importer workspace bump re-pins only the member
 * importer the edit targets. A merged descriptor key or fork fails loud.
 */
public final class YarnBerryLockPatcher implements LockPatcher {

    private static final List<String> IMPORTER_SCOPES =
            Arrays.asList("dependencies", "devDependencies", "optionalDependencies", "peerDependencies");

    /** Package names that lost a requiring edge this patch (a dropped bump edge, or a removed importer dep). */
    private final Set<String> droppedTargets = new LinkedHashSet<>();

    @Override
    public String patch(LockEditSet edits) {
        Yaml.Documents docs = LockYaml.parse(edits.getExistingLockContent(), edits.getLockPath());
        List<Yaml.Document> documents = docs.getDocuments();
        if (documents.isEmpty() || !(documents.get(0).getBlock() instanceof Yaml.Mapping)) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, firstName(edits), "yarn berry lock root is not a mapping");
        }
        Yaml.Document document = documents.get(0);
        Yaml.Mapping root = (Yaml.Mapping) document.getBlock();

        droppedTargets.clear();
        List<PackageEdit> adds = new ArrayList<>();
        for (PackageEdit edit : edits.getEdits()) {
            if (edit.getKind() == PackageEdit.Kind.ADD) {
                adds.add(edit);
            } else if (edit.getKind() == PackageEdit.Kind.BUMP && edit.getNewVersion() == null) {
                root = removeImporterEdge(root, edit);
                droppedTargets.add(edit.getName());
            } else if (edit.getKind() == PackageEdit.Kind.BUMP) {
                root = applyBump(root, edit, edits.getEditedPackageJson());
            } else if (edit.getKind() == PackageEdit.Kind.FORCED_MOVE) {
                root = applyForcedMove(root, edit);
            } else if (edit.getKind() == PackageEdit.Kind.PROMOTION) {
                root = applyPromotion(root, edit);
            } else {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, edit.getName(),
                        "yarn berry does not support " + edit.getKind() + " yet");
            }
        }
        if (!adds.isEmpty()) {
            root = applyAdds(root, adds, edits.getEditedPackageJson());
        }
        if (!droppedTargets.isEmpty()) {
            root = gcOrphans(root);
        }

        List<Yaml.Document> newDocuments = new ArrayList<>(documents);
        newDocuments.set(0, document.withBlock(root));
        return docs.withDocuments(newDocuments).printAll();
    }

    private Yaml.Mapping applyAdds(Yaml.Mapping root, List<PackageEdit> adds, @Nullable String editedPackageJson) {
        // A scoped key sorts by plain string like any other (@ < letters, __metadata pinned first) and its
        // YAML-reserved leading @ is quoted, so both a scoped leaf and a scoped clean closure are byte-exact;
        // the engine already defers a merge/fork/peer, and a multi-range merged descriptor fails loud below.
        for (PackageEdit edit : adds) {
            if (edit.getNewBerryChecksum() == null) {
                throw new EngineFailure(Reason.CHECKSUM_UNAVAILABLE, edit.getName(),
                        "no reproduced checksum for " + edit.getName());
            }
            String descriptor = descriptorFor(edit, adds, editedPackageJson);
            root = insertEntrySorted(root, buildEntry(descriptor, edit), descriptor, 1);

            // Only a package.json-declared member writes an importer edge; transitive closure members do not.
            String declared = LockManifests.declaredConstraint(editedPackageJson, edit.getScope(), edit.getName());
            if (declared != null) {
                root = insertImporterEdge(root, edit, declared);
            }
        }
        return root;
    }

    /** The merged {@code name@npm:range} descriptor: every range that resolves to this member, sorted. */
    private static String descriptorFor(PackageEdit edit, List<PackageEdit> adds, @Nullable String editedPackageJson) {
        String name = edit.getName();
        Set<String> ranges = new TreeSet<>();
        String declared = LockManifests.declaredConstraint(editedPackageJson, edit.getScope(), name);
        if (declared != null) {
            ranges.add(declared);
        }
        for (PackageEdit other : adds) {
            Map<String, String> deps = other.getNewDependencies();
            if (deps != null && deps.containsKey(name)) {
                ranges.add(deps.get(name));
            }
        }
        if (ranges.isEmpty()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, "no declaring range found for added " + name);
        }
        if (ranges.size() > 1) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " is required at multiple ranges within the added closure; merged berry descriptors deferred");
        }
        return name + "@npm:" + ranges.iterator().next();
    }

    /** Build a fresh registry entry from a synthetic snippet, then graft it under its own descriptor key. */
    private Yaml.Mapping.Entry buildEntry(String descriptor, PackageEdit edit) {
        StringBuilder body = new StringBuilder();
        body.append('"').append(descriptor).append("\":\n");
        body.append("  version: ").append(edit.getNewVersion()).append('\n');
        body.append("  resolution: \"").append(edit.getName()).append("@npm:").append(edit.getNewVersion()).append("\"\n");
        Map<String, String> deps = edit.getNewDependencies();
        if (deps != null && !deps.isEmpty()) {
            body.append("  dependencies:\n");
            for (Map.Entry<String, String> dep : new TreeMap<>(deps).entrySet()) {
                String key = dep.getKey().startsWith("@") ? "\"" + dep.getKey() + "\"" : dep.getKey();
                body.append("    ").append(key).append(": \"npm:").append(dep.getValue()).append("\"\n");
            }
        }
        body.append("  checksum: ").append(edit.getNewBerryChecksum()).append('\n');
        body.append("  languageName: node\n");
        body.append("  linkType: hard\n");
        return LockYaml.graft(body.toString(), descriptor);
    }

    private Yaml.Mapping insertImporterEdge(Yaml.Mapping root, PackageEdit edit, String constraint) {
        String name = edit.getName();
        Yaml.Mapping.Entry importer = singleImporter(root, name);
        if (importer == null) {
            return root;
        }
        Yaml.Mapping importerBody = (Yaml.Mapping) importer.getValue();
        Yaml.Mapping.Entry scopeEntry = LockYaml.findEntry(importerBody, edit.getScope());
        if (scopeEntry == null || !(scopeEntry.getValue() instanceof Yaml.Mapping)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    "adding the first " + edit.getScope() + " to a berry importer is deferred");
        }
        Yaml.Mapping deps = (Yaml.Mapping) scopeEntry.getValue();
        // A scoped key must be quoted in YAML (@ is a reserved indicator); yarn quotes it too.
        String depKey = name.startsWith("@") ? "\"" + name + "\"" : name;
        Yaml.Mapping.Entry depEntry = LockYaml.graft(
                edit.getScope() + ":\n  " + depKey + ": \"npm:" + constraint + "\"\n", edit.getScope(), name);
        deps = insertEntrySorted(deps, depEntry, name, 0);
        importerBody = LockYaml.replaceEntry(importerBody, edit.getScope(), scopeEntry.withValue(deps));
        return LockYaml.replaceEntry(root, LockYaml.keyOf(importer), importer.withValue(importerBody));
    }

    private Yaml.Mapping applyBump(Yaml.Mapping root, PackageEdit edit, @Nullable String editedPackageJson) {
        String name = edit.getName();
        if (edit.getNewVersion() == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, "yarn berry removal is deferred");
        }
        if (edit.getOldConstraint() == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, "missing old constraint for " + name);
        }
        // A constraint-only widening keeps the resolved version (and its checksum); only the descriptor and the
        // importer range move. A version move rewrites the resolution triple and needs a reproduced checksum.
        boolean widening = edit.getNewVersion().equals(edit.getOldVersion());
        if (!widening && edit.getNewBerryChecksum() == null) {
            throw new EngineFailure(Reason.CHECKSUM_UNAVAILABLE, name, "no reproduced checksum for " + name);
        }

        String newConstraint = LockManifests.declaredConstraint(editedPackageJson, edit.getScope(), name);
        if (newConstraint == null) {
            newConstraint = edit.getOldConstraint();
        }
        String oldDescriptor = name + "@npm:" + edit.getOldConstraint();
        String newDescriptor = name + "@npm:" + newConstraint;

        Yaml.Mapping.Entry entry = findSingleDescriptor(root, oldDescriptor, name);
        Yaml.Mapping body = (Yaml.Mapping) entry.getValue();
        if (!widening) {
            // The engine proved every non-dependency surface byte-identical, so rewrite only the resolution
            // triple, and — for a cascade — the changed dependency constraints.
            body = LockYaml.setScalar(body, "version", edit.getNewVersion());
            body = LockYaml.setScalar(body, "resolution", name + "@npm:" + edit.getNewVersion());
            body = LockYaml.setScalar(body, "checksum", edit.getNewBerryChecksum());
            body = rewriteDependencies(body, edit.getNewDependencies(), name);
            if (edit.isPrunesOrphans()) {
                recordDroppedEdges(body, edit.getNewDependencies());
                body = pruneDependencies(body, edit.getNewDependencies());
            }
        }
        root = LockYaml.replaceEntry(root, oldDescriptor, LockYaml.renameKey(entry, newDescriptor).withValue(body));

        return repinImporter(root, edit.getImporterDir(), name, newConstraint);
    }

    /** Record which of the bumped entry's dependencies the new manifest dropped, as orphan candidates for the GC. */
    private void recordDroppedEdges(Yaml.Mapping body, @Nullable Map<String, String> newDeps) {
        Yaml.Mapping.Entry depsEntry = LockYaml.findEntry(body, "dependencies");
        if (depsEntry == null || !(depsEntry.getValue() instanceof Yaml.Mapping)) {
            return;
        }
        Set<String> keep = newDeps == null ? Collections.emptySet() : newDeps.keySet();
        for (Yaml.Mapping.Entry dep : ((Yaml.Mapping) depsEntry.getValue()).getEntries()) {
            String name = LockYaml.keyOf(dep);
            if (name != null && !keep.contains(name)) {
                droppedTargets.add(name);
            }
        }
    }

    /** Drop the dropped edges from the bumped entry's {@code dependencies:} map, removing the map if it empties. */
    private static Yaml.Mapping pruneDependencies(Yaml.Mapping body, @Nullable Map<String, String> newDeps) {
        Yaml.Mapping.Entry depsEntry = LockYaml.findEntry(body, "dependencies");
        if (depsEntry == null || !(depsEntry.getValue() instanceof Yaml.Mapping)) {
            return body;
        }
        Set<String> keep = newDeps == null ? Collections.emptySet() : newDeps.keySet();
        List<Yaml.Mapping.Entry> survivors = new ArrayList<>();
        for (Yaml.Mapping.Entry dep : ((Yaml.Mapping) depsEntry.getValue()).getEntries()) {
            if (keep.contains(LockYaml.keyOf(dep))) {
                survivors.add(dep);
            }
        }
        if (survivors.isEmpty()) {
            List<Yaml.Mapping.Entry> kept = new ArrayList<>();
            for (Yaml.Mapping.Entry e : body.getEntries()) {
                if (!"dependencies".equals(LockYaml.keyOf(e))) {
                    kept.add(e);
                }
            }
            return body.withEntries(kept);
        }
        return LockYaml.replaceEntry(body, "dependencies",
                depsEntry.withValue(((Yaml.Mapping) depsEntry.getValue()).withEntries(survivors)));
    }

    /** Drop the removed dependency's edge from the workspace importer; the GC then reaps its private subtree. */
    private Yaml.Mapping removeImporterEdge(Yaml.Mapping root, PackageEdit edit) {
        String name = edit.getName();
        Yaml.Mapping.Entry importer = singleImporter(root, name);
        if (importer == null) {
            return root;
        }
        Yaml.Mapping importerBody = (Yaml.Mapping) importer.getValue();
        for (String scope : IMPORTER_SCOPES) {
            Yaml.Mapping.Entry scopeEntry = LockYaml.findEntry(importerBody, scope);
            if (scopeEntry == null || !(scopeEntry.getValue() instanceof Yaml.Mapping) ||
                    LockYaml.findEntry((Yaml.Mapping) scopeEntry.getValue(), name) == null) {
                continue;
            }
            List<Yaml.Mapping.Entry> survivors = new ArrayList<>();
            for (Yaml.Mapping.Entry dep : ((Yaml.Mapping) scopeEntry.getValue()).getEntries()) {
                if (!name.equals(LockYaml.keyOf(dep))) {
                    survivors.add(dep);
                }
            }
            if (survivors.isEmpty()) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        "removing the last " + scope + " from a berry importer is not yet supported");
            }
            importerBody = LockYaml.replaceEntry(importerBody, scope,
                    scopeEntry.withValue(((Yaml.Mapping) scopeEntry.getValue()).withEntries(survivors)));
        }
        return LockYaml.replaceEntry(root, LockYaml.keyOf(importer), importer.withValue(importerBody));
    }

    /**
     * Reap only the subtree the dropped edges orphaned: from each dropped target, remove its entry iff no surviving
     * entry still requires it and it is not an importer root, then recurse into its own dependencies. A full
     * reachability sweep would wrongly reap peer-provided packages (yarn.lock records no peer-satisfaction edge).
     */
    private Yaml.Mapping gcOrphans(Yaml.Mapping root) {
        Set<String> roots = new LinkedHashSet<>(importerDepNames(root));
        Set<String> removed = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>(droppedTargets);
        Set<String> seen = new LinkedHashSet<>();
        while (!queue.isEmpty()) {
            String name = queue.poll();
            if (!seen.add(name) || roots.contains(name)) {
                continue;
            }
            Yaml.Mapping.Entry entry = registryEntry(root, name);
            if (entry == null || referencedByOther(root, name, removed)) {
                continue;
            }
            removed.add(LockYaml.keyOf(entry));
            queue.addAll(entryDepNames((Yaml.Mapping) entry.getValue()));
        }
        List<Yaml.Mapping.Entry> kept = new ArrayList<>();
        for (Yaml.Mapping.Entry e : root.getEntries()) {
            if (!removed.contains(LockYaml.keyOf(e))) {
                kept.add(e);
            }
        }
        return root.withEntries(kept);
    }

    /** True when an entry (not being removed, not {@code name}'s own) requires {@code name} in a dependency scope. */
    private static boolean referencedByOther(Yaml.Mapping root, String name, Set<String> removedKeys) {
        for (Yaml.Mapping.Entry e : root.getEntries()) {
            String key = LockYaml.keyOf(e);
            if (key == null || "__metadata".equals(key) || removedKeys.contains(key) ||
                    berryEntryName(key).equals(name) || !(e.getValue() instanceof Yaml.Mapping)) {
                continue;
            }
            if (entryDepNames((Yaml.Mapping) e.getValue()).contains(name)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> importerDepNames(Yaml.Mapping root) {
        Yaml.Mapping.Entry importer = singleImporter(root, "");
        return importer == null ? new ArrayList<>() : entryDepNames((Yaml.Mapping) importer.getValue());
    }

    /** The dependency names declared across an entry's dependency scopes. */
    private static List<String> entryDepNames(Yaml.Mapping entry) {
        List<String> names = new ArrayList<>();
        for (String scope : IMPORTER_SCOPES) {
            Yaml.Mapping.Entry scopeEntry = LockYaml.findEntry(entry, scope);
            if (scopeEntry != null && scopeEntry.getValue() instanceof Yaml.Mapping) {
                for (Yaml.Mapping.Entry dep : ((Yaml.Mapping) scopeEntry.getValue()).getEntries()) {
                    String name = LockYaml.keyOf(dep);
                    if (name != null) {
                        names.add(name);
                    }
                }
            }
        }
        return names;
    }

    /** The package name a descriptor key heads (before {@code @npm:}); the key itself for a non-registry entry. */
    private static String berryEntryName(String key) {
        String first = key.split(",")[0].trim();
        int at = first.indexOf("@npm:");
        return at > 0 ? first.substring(0, at) : first;
    }

    /** Promote an already-present transitive: merge its declared descriptor into the entry key and add the importer edge. */
    private Yaml.Mapping applyPromotion(Yaml.Mapping root, PackageEdit edit) {
        String name = edit.getName();
        String constraint = edit.getNewConstraint();
        if (constraint == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, "no declared constraint for promoted " + name);
        }
        Yaml.Mapping.Entry entry = registryEntry(root, name);
        if (entry == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, "no berry entry for " + name);
        }
        String key = LockYaml.keyOf(entry);
        Set<String> descriptors = new TreeSet<>();
        for (String descriptor : key.split(",")) {
            descriptors.add(descriptor.trim());
        }
        if (descriptors.add(name + "@npm:" + constraint)) {
            root = LockYaml.replaceEntry(root, key,
                    LockYaml.renameKey(entry, String.join(", ", descriptors)).withValue(entry.getValue()));
        }
        return insertImporterEdge(root, edit, constraint);
    }

    /** The lone registry entry heading {@code name} (its key holds an {@code @npm:} descriptor for it). */
    private static Yaml.Mapping.@Nullable Entry registryEntry(Yaml.Mapping root, String name) {
        for (Yaml.Mapping.Entry e : root.getEntries()) {
            String key = LockYaml.keyOf(e);
            if (key != null && key.contains("@npm:") && berryEntryName(key).equals(name)) {
                return e;
            }
        }
        return null;
    }

    /** Re-head a cascade-forced transitive's descriptor to its new range and rewrite its resolution triple. */
    private Yaml.Mapping applyForcedMove(Yaml.Mapping root, PackageEdit edit) {
        String name = edit.getName();
        if (edit.getNewBerryChecksum() == null) {
            throw new EngineFailure(Reason.CHECKSUM_UNAVAILABLE, name, "no reproduced checksum for " + name);
        }
        if (edit.getOldConstraint() == null || edit.getNewConstraint() == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, "missing descriptor range for moved " + name);
        }
        String oldDescriptor = name + "@npm:" + edit.getOldConstraint();
        Yaml.Mapping.Entry entry = findSingleDescriptor(root, oldDescriptor, name);
        Yaml.Mapping body = (Yaml.Mapping) entry.getValue();
        body = LockYaml.setScalar(body, "version", edit.getNewVersion());
        body = LockYaml.setScalar(body, "resolution", name + "@npm:" + edit.getNewVersion());
        body = LockYaml.setScalar(body, "checksum", edit.getNewBerryChecksum());
        return LockYaml.replaceEntry(root, oldDescriptor,
                LockYaml.renameKey(entry, name + "@npm:" + edit.getNewConstraint()).withValue(body));
    }

    /** Re-pin each already-present dependency constraint in the bumped entry to the new manifest's {@code npm:} range. */
    private static Yaml.Mapping rewriteDependencies(Yaml.Mapping body, @Nullable Map<String, String> newDeps, String name) {
        if (newDeps == null || newDeps.isEmpty()) {
            return body;
        }
        Yaml.Mapping.Entry depsEntry = LockYaml.findEntry(body, "dependencies");
        if (depsEntry == null || !(depsEntry.getValue() instanceof Yaml.Mapping)) {
            return body;
        }
        Yaml.Mapping deps = (Yaml.Mapping) depsEntry.getValue();
        for (String depName : depNames(deps)) {
            String range = newDeps.get(depName);
            if (range == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        name + " no longer depends on " + depName + " (orphan prune) not yet supported for berry");
            }
            deps = LockYaml.setScalar(deps, depName, "npm:" + range);
        }
        return LockYaml.replaceEntry(body, "dependencies", depsEntry.withValue(deps));
    }

    private static List<String> depNames(Yaml.Mapping deps) {
        List<String> names = new ArrayList<>();
        for (Yaml.Mapping.Entry entry : deps.getEntries()) {
            String key = LockYaml.keyOf(entry);
            if (key != null) {
                names.add(key);
            }
        }
        return names;
    }

    /** Re-pin the declared range on {@code name} to {@code npm:<newConstraint>} in the edit's workspace importer. */
    private Yaml.Mapping repinImporter(Yaml.Mapping root, @Nullable String importerDir, String name, String newConstraint) {
        Yaml.Mapping.Entry importer = importerFor(root, importerDir);
        if (importer == null) {
            return root;
        }
        Yaml.Mapping importerBody = (Yaml.Mapping) importer.getValue();
        for (String scope : IMPORTER_SCOPES) {
            Yaml.Mapping.Entry scopeEntry = LockYaml.findEntry(importerBody, scope);
            if (scopeEntry != null && scopeEntry.getValue() instanceof Yaml.Mapping &&
                    LockYaml.findEntry((Yaml.Mapping) scopeEntry.getValue(), name) != null) {
                Yaml.Mapping newScope = LockYaml.setScalar((Yaml.Mapping) scopeEntry.getValue(), name, "npm:" + newConstraint);
                importerBody = LockYaml.replaceEntry(importerBody, scope, scopeEntry.withValue(newScope));
            }
        }
        return LockYaml.replaceEntry(root, LockYaml.keyOf(importer), importer.withValue(importerBody));
    }

    /**
     * The workspace importer keyed {@code @workspace:<dir>} ({@code .} for the root), located by the edit's importer
     * directory so a member-declared bump re-pins only that member. Falls back to the sole importer when unkeyed.
     */
    private static Yaml.Mapping.@Nullable Entry importerFor(Yaml.Mapping root, @Nullable String importerDir) {
        String target = importerDir == null ? "." : importerDir;
        Yaml.Mapping.Entry only = null;
        int count = 0;
        for (Yaml.Mapping.Entry e : root.getEntries()) {
            String key = LockYaml.keyOf(e);
            if (key == null || !key.contains("@workspace:") || !(e.getValue() instanceof Yaml.Mapping)) {
                continue;
            }
            count++;
            only = e;
            if (target.equals(workspaceDir(key))) {
                return e;
            }
        }
        return (importerDir == null && count == 1) ? only : null;
    }

    /** The path after {@code @workspace:} in an importer descriptor key. */
    private static String workspaceDir(String key) {
        String first = key.split(",")[0].trim();
        int idx = first.indexOf("@workspace:");
        return idx < 0 ? first : first.substring(idx + "@workspace:".length());
    }

    /** The lone {@code @workspace:} importer entry, or {@code null} if none; multiple importers defer. */
    private static Yaml.Mapping.@Nullable Entry singleImporter(Yaml.Mapping root, String name) {
        Yaml.Mapping.Entry importer = null;
        for (Yaml.Mapping.Entry e : root.getEntries()) {
            String key = LockYaml.keyOf(e);
            if (key != null && key.contains("@workspace:") && e.getValue() instanceof Yaml.Mapping) {
                if (importer != null) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                            "yarn berry workspaces (multiple importers) are deferred");
                }
                importer = e;
            }
        }
        return importer;
    }

    /**
     * Splice {@code newEntry} at its plain-string key position, copying a sibling's prefix. {@code firstSortable}
     * is 1 at the lock root (berry pins {@code __metadata} first) and 0 for a nested map. Scoped keys are already
     * deferred, so a plain string compare holds.
     */
    private static Yaml.Mapping insertEntrySorted(Yaml.Mapping mapping, Yaml.Mapping.Entry newEntry, String newKey,
                                                  int firstSortable) {
        List<Yaml.Mapping.Entry> entries = mapping.getEntries();
        Yaml.Mapping.Entry placed = newEntry.withPrefix(entries.get(entries.size() - 1).getPrefix());
        int idx = entries.size();
        for (int i = firstSortable; i < entries.size(); i++) {
            String k = LockYaml.keyOf(entries.get(i));
            if (k != null && k.compareTo(newKey) > 0) {
                idx = i;
                break;
            }
        }
        List<Yaml.Mapping.Entry> out = new ArrayList<>(entries.size() + 1);
        out.addAll(entries.subList(0, idx));
        out.add(placed);
        out.addAll(entries.subList(idx, entries.size()));
        return mapping.withEntries(out);
    }

    /** Find the entry whose key is exactly {@code descriptor}; fail loud on a merged (comma-joined) key. */
    private static Yaml.Mapping.Entry findSingleDescriptor(Yaml.Mapping root, String descriptor, String name) {
        for (Yaml.Mapping.Entry entry : root.getEntries()) {
            String key = LockYaml.keyOf(entry);
            if (key == null || !(entry.getValue() instanceof Yaml.Mapping)) {
                continue;
            }
            List<String> descriptors = new ArrayList<>();
            for (String part : key.split(",")) {
                descriptors.add(part.trim());
            }
            if (descriptors.contains(descriptor)) {
                if (descriptors.size() > 1) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                            name + " shares a merged descriptor key (" + key + "); resolution required");
                }
                return entry;
            }
        }
        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, "no berry entry for " + descriptor);
    }

    private static @Nullable String firstName(LockEditSet edits) {
        return edits.getEdits().isEmpty() ? null : edits.getEdits().get(0).getName();
    }
}
