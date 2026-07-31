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
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static java.util.Collections.singletonList;

/**
 * Byte-exact patcher for a Yarn Berry {@code yarn.lock} (the {@code __metadata:}-headed YAML format). The file
 * round-trips byte-identical through the rewrite-yaml LST, so rewriting only the named entries preserves every
 * other byte. A registry dependency's {@code checksum} — long the blocker for berry — is reproduced by
 * {@link BerryZipChecksum} and threaded in on the edit; the engine derives it before this patcher runs.
 * <p>
 * Supported: an in-place version bump (rewrite the entry's descriptor key, {@code version}, {@code resolution}
 * and {@code checksum} plus the importer range) and adding a dependency plus its runtime closure (a fresh entry
 * per member at its sorted position). A cascade (changed {@code dependencies} map), removal, merged descriptor
 * key, fork, scoped add, or workspaces all fail loud.
 */
public final class YarnBerryLockPatcher implements LockPatcher {

    private static final List<String> IMPORTER_SCOPES =
            Arrays.asList("dependencies", "devDependencies", "optionalDependencies", "peerDependencies");

    @Override
    public String patch(LockEditSet edits) {
        Yaml.Documents docs = parse(edits.getExistingLockContent(), edits.getLockPath());
        List<Yaml.Document> documents = docs.getDocuments();
        if (documents.isEmpty() || !(documents.get(0).getBlock() instanceof Yaml.Mapping)) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, firstName(edits), "yarn berry lock root is not a mapping");
        }
        Yaml.Document document = documents.get(0);
        Yaml.Mapping root = (Yaml.Mapping) document.getBlock();

        List<PackageEdit> adds = new ArrayList<>();
        for (PackageEdit edit : edits.getEdits()) {
            if (edit.getKind() == PackageEdit.Kind.ADD) {
                adds.add(edit);
            } else if (edit.getKind() == PackageEdit.Kind.BUMP) {
                root = applyBump(root, edit, edits.getEditedPackageJson());
            } else if (edit.getKind() == PackageEdit.Kind.FORCED_MOVE) {
                root = applyForcedMove(root, edit);
            } else {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, edit.getName(),
                        "yarn berry does not support " + edit.getKind() + " yet");
            }
        }
        if (!adds.isEmpty()) {
            root = applyAdds(root, adds, edits.getEditedPackageJson());
        }

        List<Yaml.Document> newDocuments = new ArrayList<>(documents);
        newDocuments.set(0, document.withBlock(root));
        return docs.withDocuments(newDocuments).printAll();
    }

    private Yaml.Mapping applyAdds(Yaml.Mapping root, List<PackageEdit> adds, @Nullable String editedPackageJson) {
        for (PackageEdit edit : adds) {
            if (edit.getName().startsWith("@")) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, edit.getName(),
                        "scoped berry adds defer until the sort comparator is validated");
            }
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
                body.append("    ").append(dep.getKey()).append(": \"npm:").append(dep.getValue()).append("\"\n");
            }
        }
        body.append("  checksum: ").append(edit.getNewBerryChecksum()).append('\n');
        body.append("  languageName: node\n");
        body.append("  linkType: hard\n");
        return parseGraftEntry(body.toString(), descriptor);
    }

    private Yaml.Mapping insertImporterEdge(Yaml.Mapping root, PackageEdit edit, String constraint) {
        String name = edit.getName();
        Yaml.Mapping.Entry importer = singleImporter(root, name);
        if (importer == null) {
            return root;
        }
        Yaml.Mapping importerBody = (Yaml.Mapping) importer.getValue();
        Yaml.Mapping.Entry scopeEntry = findEntry(importerBody, edit.getScope());
        if (scopeEntry == null || !(scopeEntry.getValue() instanceof Yaml.Mapping)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    "adding the first " + edit.getScope() + " to a berry importer is deferred");
        }
        Yaml.Mapping deps = (Yaml.Mapping) scopeEntry.getValue();
        Yaml.Mapping.Entry depEntry = parseGraftEntry(
                edit.getScope() + ":\n  " + name + ": \"npm:" + constraint + "\"\n", edit.getScope(), name);
        deps = insertEntrySorted(deps, depEntry, name, 0);
        importerBody = replaceEntry(importerBody, edit.getScope(), scopeEntry.withValue(deps));
        return replaceEntry(root, keyOf(importer), importer.withValue(importerBody));
    }

    private Yaml.Mapping applyBump(Yaml.Mapping root, PackageEdit edit, @Nullable String editedPackageJson) {
        String name = edit.getName();
        if (edit.getNewVersion() == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, "yarn berry removal is deferred");
        }
        if (edit.getOldConstraint() == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, name, "missing old constraint for " + name);
        }
        if (edit.getNewBerryChecksum() == null) {
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
        // The engine proved every non-dependency surface (peer/optional/engines/…) byte-identical, so rewrite only
        // the resolution triple, the importer range, and — for a cascade — the changed dependency constraints.
        body = setScalar(body, "version", edit.getNewVersion(), name);
        body = setScalar(body, "resolution", name + "@npm:" + edit.getNewVersion(), name);
        body = setScalar(body, "checksum", edit.getNewBerryChecksum(), name);
        body = rewriteDependencies(body, edit.getNewDependencies(), name);
        root = replaceEntry(root, oldDescriptor, renameKey(entry, newDescriptor).withValue(body));

        return repinImporter(root, name, newConstraint);
    }

    /** Re-head a cascade-forced transitive's descriptor to its new range and rewrite its resolution triple. */
    private Yaml.Mapping applyForcedMove(Yaml.Mapping root, PackageEdit edit) {
        String name = edit.getName();
        if (edit.getNewBerryChecksum() == null) {
            throw new EngineFailure(Reason.CHECKSUM_UNAVAILABLE, name, "no reproduced checksum for " + name);
        }
        if (edit.getOldConstraint() == null || edit.getNewConstraint() == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, name, "missing descriptor range for moved " + name);
        }
        String oldDescriptor = name + "@npm:" + edit.getOldConstraint();
        Yaml.Mapping.Entry entry = findSingleDescriptor(root, oldDescriptor, name);
        Yaml.Mapping body = (Yaml.Mapping) entry.getValue();
        body = setScalar(body, "version", edit.getNewVersion(), name);
        body = setScalar(body, "resolution", name + "@npm:" + edit.getNewVersion(), name);
        body = setScalar(body, "checksum", edit.getNewBerryChecksum(), name);
        return replaceEntry(root, oldDescriptor,
                renameKey(entry, name + "@npm:" + edit.getNewConstraint()).withValue(body));
    }

    /** Re-pin each already-present dependency constraint in the bumped entry to the new manifest's {@code npm:} range. */
    private static Yaml.Mapping rewriteDependencies(Yaml.Mapping body, @Nullable Map<String, String> newDeps, String name) {
        if (newDeps == null || newDeps.isEmpty()) {
            return body;
        }
        Yaml.Mapping.Entry depsEntry = findEntry(body, "dependencies");
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
            deps = setScalar(deps, depName, "npm:" + range, name);
        }
        return replaceEntry(body, "dependencies", depsEntry.withValue(deps));
    }

    private static List<String> depNames(Yaml.Mapping deps) {
        List<String> names = new ArrayList<>();
        for (Yaml.Mapping.Entry entry : deps.getEntries()) {
            String key = keyOf(entry);
            if (key != null) {
                names.add(key);
            }
        }
        return names;
    }

    /** Re-pin the single workspace importer's declared range on {@code name} to {@code npm:<newConstraint>}. */
    private Yaml.Mapping repinImporter(Yaml.Mapping root, String name, String newConstraint) {
        Yaml.Mapping.Entry importer = singleImporter(root, name);
        if (importer == null) {
            return root;
        }
        Yaml.Mapping importerBody = (Yaml.Mapping) importer.getValue();
        for (String scope : IMPORTER_SCOPES) {
            Yaml.Mapping.Entry scopeEntry = findEntry(importerBody, scope);
            if (scopeEntry != null && scopeEntry.getValue() instanceof Yaml.Mapping &&
                    findEntry((Yaml.Mapping) scopeEntry.getValue(), name) != null) {
                Yaml.Mapping newScope = setScalar((Yaml.Mapping) scopeEntry.getValue(), name, "npm:" + newConstraint, name);
                importerBody = replaceEntry(importerBody, scope, scopeEntry.withValue(newScope));
            }
        }
        return replaceEntry(root, keyOf(importer), importer.withValue(importerBody));
    }

    /** The lone {@code @workspace:} importer entry, or {@code null} if none; multiple importers defer. */
    private static Yaml.Mapping.@Nullable Entry singleImporter(Yaml.Mapping root, String name) {
        Yaml.Mapping.Entry importer = null;
        for (Yaml.Mapping.Entry e : root.getEntries()) {
            String key = keyOf(e);
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
            String k = keyOf(entries.get(i));
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

    private Yaml.Mapping.Entry parseGraftEntry(String synthetic, String... path) {
        Yaml.Documents docs = parse(synthetic, null);
        if (docs.getDocuments().isEmpty() || !(docs.getDocuments().get(0).getBlock() instanceof Yaml.Mapping)) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "could not construct berry lock entry");
        }
        Yaml.Mapping mapping = (Yaml.Mapping) docs.getDocuments().get(0).getBlock();
        for (int i = 0; i < path.length - 1; i++) {
            Yaml.Mapping.Entry entry = findEntry(mapping, path[i]);
            if (entry == null || !(entry.getValue() instanceof Yaml.Mapping)) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, null, "could not navigate synthetic entry at " + path[i]);
            }
            mapping = (Yaml.Mapping) entry.getValue();
        }
        Yaml.Mapping.Entry leaf = findEntry(mapping, path[path.length - 1]);
        if (leaf == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "could not construct berry entry " + path[path.length - 1]);
        }
        return leaf;
    }

    /** Find the entry whose key is exactly {@code descriptor}; fail loud on a merged (comma-joined) key. */
    private static Yaml.Mapping.Entry findSingleDescriptor(Yaml.Mapping root, String descriptor, String name) {
        for (Yaml.Mapping.Entry entry : root.getEntries()) {
            String key = keyOf(entry);
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
        throw new EngineFailure(Reason.MALFORMED_LOCK, name, "no berry entry for " + descriptor);
    }

    // --- YAML LST helpers (mirror PnpmLockPatcher; a shared LockYaml can absorb these) ------------------

    private static Yaml.Documents parse(String content, @Nullable Path lockPath) {
        Path path = lockPath == null ? Paths.get("yarn.lock") : lockPath;
        SourceFile source;
        try {
            ExecutionContext ctx = new InMemoryExecutionContext();
            Parser.Input input = Parser.Input.fromString(path, content);
            source = new YamlParser().parseInputs(singletonList(input), null, ctx).findFirst().orElse(null);
        } catch (RuntimeException e) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "unparseable yarn.lock: " + e.getMessage());
        }
        if (!(source instanceof Yaml.Documents)) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "yarn.lock could not be parsed as YAML");
        }
        return (Yaml.Documents) source;
    }

    private static @Nullable String keyOf(Yaml.Mapping.Entry entry) {
        return entry.getKey() instanceof Yaml.Scalar ? ((Yaml.Scalar) entry.getKey()).getValue() : null;
    }

    private static Yaml.Mapping.@Nullable Entry findEntry(Yaml.Mapping mapping, String key) {
        for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
            if (key.equals(keyOf(entry))) {
                return entry;
            }
        }
        return null;
    }

    private static Yaml.Mapping replaceEntry(Yaml.Mapping mapping, @Nullable String key, Yaml.Mapping.Entry replacement) {
        List<Yaml.Mapping.Entry> entries = new ArrayList<>(mapping.getEntries());
        for (int i = 0; i < entries.size(); i++) {
            if (key != null && key.equals(keyOf(entries.get(i)))) {
                entries.set(i, replacement);
                return mapping.withEntries(entries);
            }
        }
        return mapping;
    }

    private static Yaml.Mapping.Entry renameKey(Yaml.Mapping.Entry entry, String newKey) {
        return entry.withKey(((Yaml.Scalar) entry.getKey()).withValue(newKey));
    }

    private static Yaml.Mapping setScalar(Yaml.Mapping mapping, String key, String value, String name) {
        Yaml.Mapping.Entry entry = findEntry(mapping, key);
        if (entry == null || !(entry.getValue() instanceof Yaml.Scalar)) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, name, "expected scalar entry '" + key + "' for " + name);
        }
        return replaceEntry(mapping, key, entry.withValue(((Yaml.Scalar) entry.getValue()).withValue(value)));
    }

    private static @Nullable String firstName(LockEditSet edits) {
        return edits.getEdits().isEmpty() ? null : edits.getEdits().get(0).getName();
    }
}
