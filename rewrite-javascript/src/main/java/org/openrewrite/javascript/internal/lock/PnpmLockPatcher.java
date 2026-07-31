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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;
import org.openrewrite.javascript.internal.lock.LockEditSet.WriteThroughMetadata;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import static java.util.Collections.singletonList;
import static org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit.Kind.*;

/**
 * Byte-exact patcher for {@code pnpm-lock.yaml} (lockfileVersion 6 and 9). The file round-trips
 * byte-identical through the rewrite-yaml LST, so rewriting only the named entries preserves every other
 * byte. This parser sees {@code resolution: {integrity: …}} and {@code engines: {…}} as single opaque
 * flow-map scalars, so integrity/engines are rewritten as substrings inside that one scalar. Any edit that
 * would move a peer suffix, retarget a by-version reference, or fork a version shared across importers is
 * closure-changing and fails loud, as does lockfileVersion &lt; 6.
 */
public final class PnpmLockPatcher implements LockPatcher {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Dependency scopes as they appear in the lockfile's importer mappings. */
    private static final List<String> LOCK_SCOPES =
            Arrays.asList("dependencies", "devDependencies", "optionalDependencies");

    /** Dependency scopes read from a {@code package.json} when re-pinning importer specifiers. */
    private static final List<String> MANIFEST_SCOPES =
            Arrays.asList("dependencies", "devDependencies", "optionalDependencies", "peerDependencies");

    private static final List<String> UNSUPPORTED_ENTRY_PREFIXES = Arrays.asList(
            "link:", "file:", "workspace:", "git:", "git+", "github:", "portal:", "http://", "https://");

    @Override
    public String patch(LockEditSet edits) {
        Yaml.Documents docs = parse(edits.getExistingLockContent(), edits.getLockPath());
        List<Yaml.Document> documents = docs.getDocuments();
        if (documents.isEmpty() || !(documents.get(0).getBlock() instanceof Yaml.Mapping)) {
            throw fail(Reason.MALFORMED_LOCK, firstName(edits), "pnpm-lock.yaml root is not a mapping");
        }
        Yaml.Document document = documents.get(0);
        Yaml.Mapping root = (Yaml.Mapping) document.getBlock();

        int major = lockfileMajor(root, firstName(edits));
        if (major < 6) {
            throw fail(Reason.UNSUPPORTED_LOCKFILE_VERSION, firstName(edits),
                    "pnpm lockfileVersion below 6 is not supported for native regeneration");
        }

        Map<String, String> newConstraints = declaredConstraints(edits.getEditedPackageJson());
        Map<String, String> addedVersions = addedVersions(edits.getEdits());

        boolean anyRemoval = false;
        boolean anyPrune = false;
        for (PackageEdit edit : edits.getEdits()) {
            if (edit.getKind() == ADD) {
                root = applyAdd(root, edit, major, newConstraints, addedVersions);
                continue;
            }
            if (edit.getKind() == CONTENT_FORK) {
                root = applyContentFork(root, edit, major, newConstraints);
                continue;
            }
            if (edit.getKind() == FORCED_MOVE) {
                root = applyForcedMove(root, edit, major);
                continue;
            }
            if (edit.isPrunesOrphans() && major < 9) {
                throw fail(Reason.RESOLUTION_REQUIRED, edit.getName(),
                        "orphan-prune on a pnpm bump below lockfileVersion 9 is not yet supported");
            }
            preCheck(root, edit, major);
            root = applyEdit(root, edit, major, newConstraints);
            anyRemoval |= edit.getNewVersion() == null;
            anyPrune |= edit.isPrunesOrphans();
        }
        if (anyRemoval || anyPrune) {
            root = gcOrphans(root, major);
        }

        List<Yaml.Document> newDocuments = new ArrayList<>(documents);
        newDocuments.set(0, document.withBlock(root));
        return docs.withDocuments(newDocuments).printAll();
    }

    // --- fail-loud structural pre-checks ----------------------------

    private void preCheck(Yaml.Mapping root, PackageEdit edit, int major) {
        if (edit.getNewVersion() == null) {
            return; // removal: the reference scan runs while dropping the keys
        }
        String name = edit.getName();
        String oldV = edit.getOldVersion();

        for (String version : importerVersionsForDep(root, edit)) {
            if (isUnsupportedEntry(version)) {
                throw fail(Reason.UNSUPPORTED_ENTRY_TYPE, name,
                        name + " resolves to an unsupported entry type: " + version);
            }
        }

        // Peer suffix (provider or dependent).
        String peerToken = "(" + name + "@";
        for (String key : allKeysAndVersions(root, major)) {
            if (key.contains(peerToken)) {
                throw fail(Reason.RESOLUTION_REQUIRED, name,
                        name + " is a peer-dependency provider; resolution required");
            }
        }
        if (bumpedHasPeerSuffix(root, edit, major)) {
            throw fail(Reason.RESOLUTION_REQUIRED, name, name + " is peer-dependent; resolution required");
        }

        // Dedupe collision / shared fork.
        String newVersion = edit.getNewVersion();
        if (keys(section(root, "packages")).contains(packageKey(name, newVersion, major)) ||
                (major >= 9 && keys(section(root, "snapshots")).contains(name + "@" + newVersion))) {
            throw fail(Reason.RESOLUTION_REQUIRED, name,
                    name + "@" + newVersion + " already present; dedupe required");
        }
        if (sharedByOtherImporter(root, edit)) {
            throw fail(Reason.RESOLUTION_REQUIRED, name,
                    name + "@" + oldV + " is shared by another importer; resolution required");
        }

        // By-version reference from another package's dependency block.
        if (referencedByOther(root, edit, major)) {
            throw fail(Reason.RESOLUTION_REQUIRED, name,
                    name + "@" + oldV + " is referenced by another package; resolution required");
        }
    }

    private List<String> allKeysAndVersions(Yaml.Mapping root, int major) {
        List<String> out = new ArrayList<>(keys(section(root, "packages")));
        if (major >= 9) {
            out.addAll(keys(section(root, "snapshots")));
        }
        out.addAll(importerVersionValues(root));
        return out;
    }

    private boolean bumpedHasPeerSuffix(Yaml.Mapping root, PackageEdit edit, int major) {
        String base = edit.getName() + "@" + edit.getOldVersion();
        List<String> keys = major >= 9 ? keys(section(root, "snapshots")) : keys(section(root, "packages"));
        String prefix = major >= 9 ? base + "(" : "/" + base + "(";
        for (String key : keys) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        for (String version : importerVersionsForDep(root, edit)) {
            if (version.indexOf('(') >= 0) {
                return true;
            }
        }
        return false;
    }

    private boolean sharedByOtherImporter(Yaml.Mapping root, PackageEdit edit) {
        Yaml.Mapping importers = section(root, "importers");
        if (importers == null) {
            return false; // single-package v6: only one importer
        }
        String editedDir = importerDir(edit);
        for (Yaml.Mapping.Entry importer : importers.getEntries()) {
            if (editedDir.equals(keyOf(importer)) || !(importer.getValue() instanceof Yaml.Mapping)) {
                continue;
            }
            String version = depVersion((Yaml.Mapping) importer.getValue(), edit.getName());
            if (version != null && stripSuffix(version).equals(edit.getOldVersion())) {
                return true;
            }
        }
        return false;
    }

    private boolean referencedByOther(Yaml.Mapping root, PackageEdit edit, int major) {
        String name = edit.getName();
        String oldV = edit.getOldVersion();
        Yaml.Mapping graph = major >= 9 ? section(root, "snapshots") : section(root, "packages");
        if (graph == null) {
            return false;
        }
        String ownKey = major >= 9 ? name + "@" + oldV : "/" + name + "@" + oldV;
        for (Yaml.Mapping.Entry entry : graph.getEntries()) {
            String key = keyOf(entry);
            if (key == null || ownKey.equals(key) || key.startsWith(ownKey + "(")) {
                continue;
            }
            if (dependsOnByVersion(entry.getValue(), name, oldV)) {
                return true;
            }
        }
        return false;
    }

    private static boolean dependsOnByVersion(Yaml.Block body, String name, String oldV) {
        if (!(body instanceof Yaml.Mapping)) {
            return false;
        }
        Yaml.Mapping mapping = (Yaml.Mapping) body;
        for (String depScope : Arrays.asList("dependencies", "optionalDependencies")) {
            Yaml.Mapping.Entry scope = findEntry(mapping, depScope);
            if (scope != null && scope.getValue() instanceof Yaml.Mapping) {
                Yaml.Mapping.Entry dep = findEntry((Yaml.Mapping) scope.getValue(), name);
                if (dep != null && dep.getValue() instanceof Yaml.Scalar) {
                    String version = ((Yaml.Scalar) dep.getValue()).getValue();
                    if (version.equals(oldV) || version.startsWith(oldV + "(")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // --- edit application -------------------------------------------

    private Yaml.Mapping applyEdit(Yaml.Mapping root, PackageEdit edit, int major, Map<String, String> newConstraints) {
        boolean removal = edit.getNewVersion() == null;
        if (!removal) {
            requireSupportedWriteThrough(edit);
        }
        root = patchImporter(root, edit, removal, newConstraints);
        root = patchPackages(root, edit, major, removal);
        if (major >= 9) {
            root = patchSnapshots(root, edit, removal);
        }
        return root;
    }

    private Yaml.Mapping patchImporter(Yaml.Mapping root, PackageEdit edit, boolean removal,
                                       Map<String, String> newConstraints) {
        Yaml.Mapping importers = section(root, "importers");
        if (importers == null) {
            return patchScopes(root, edit, removal, newConstraints); // single-package v6
        }
        String dir = importerDir(edit);
        Yaml.Mapping.Entry importer = findEntry(importers, dir);
        if (importer == null || !(importer.getValue() instanceof Yaml.Mapping)) {
            if (removal) {
                return root;
            }
            throw fail(Reason.MALFORMED_LOCK, edit.getName(), "no importer '" + dir + "'");
        }
        Yaml.Mapping patchedScopes = patchScopes((Yaml.Mapping) importer.getValue(), edit, removal, newConstraints);
        Yaml.Mapping patchedImporters = replaceEntryValue(importers, dir, patchedScopes);
        return replaceEntryValue(root, "importers", patchedImporters);
    }

    private Yaml.Mapping patchScopes(Yaml.Mapping scopes, PackageEdit edit, boolean removal,
                                     Map<String, String> newConstraints) {
        for (String scope : LOCK_SCOPES) {
            Yaml.Mapping.Entry scopeEntry = findEntry(scopes, scope);
            if (scopeEntry == null || !(scopeEntry.getValue() instanceof Yaml.Mapping)) {
                continue;
            }
            Yaml.Mapping deps = (Yaml.Mapping) scopeEntry.getValue();
            Yaml.Mapping.Entry dep = findEntry(deps, edit.getName());
            if (dep == null) {
                continue;
            }
            if (removal) {
                return replaceEntryValue(scopes, scope, removeEntries(deps, edit.getName()));
            }
            if (!(dep.getValue() instanceof Yaml.Mapping)) {
                throw fail(Reason.MALFORMED_LOCK, edit.getName(), "importer entry is not a mapping");
            }
            Yaml.Mapping body = (Yaml.Mapping) dep.getValue();
            String newSpecifier = newConstraints.get(edit.getName());
            if (newSpecifier != null) {
                body = setScalar(body, "specifier", newSpecifier);
            }
            body = setScalar(body, "version", edit.getNewVersion());
            Yaml.Mapping patchedDeps = replaceEntryValue(deps, edit.getName(), body);
            return replaceEntryValue(scopes, scope, patchedDeps);
        }
        if (!removal) {
            throw fail(Reason.MALFORMED_LOCK, edit.getName(), "no importer entry for " + edit.getName());
        }
        return scopes;
    }

    private Yaml.Mapping patchPackages(Yaml.Mapping root, PackageEdit edit, int major, boolean removal) {
        Yaml.Mapping packages = section(root, "packages");
        if (packages == null) {
            throw fail(Reason.MALFORMED_LOCK, edit.getName(), "pnpm-lock.yaml has no packages section");
        }
        String oldKey = packageKey(edit.getName(), edit.getOldVersion(), major);
        if (removal) {
            if (referencedByOther(root, edit, major)) {
                return root; // still needed transitively — keep the entry
            }
            return replaceEntryValue(root, "packages", removeEntries(packages, oldKey));
        }
        Yaml.Mapping.Entry pkg = findEntry(packages, oldKey);
        if (pkg == null) {
            throw fail(Reason.MALFORMED_LOCK, edit.getName(), "no packages entry " + oldKey);
        }
        Yaml.Mapping.Entry renamed = renameKey(pkg, packageKey(edit.getName(), edit.getNewVersion(), major));
        if (renamed.getValue() instanceof Yaml.Mapping) {
            Yaml.Mapping body = (Yaml.Mapping) renamed.getValue();
            body = editResolution(body, edit);
            body = editEngines(body, edit);
            renamed = renamed.withValue(body);
        }
        return replaceEntryValue(root, "packages", replaceEntry(packages, oldKey, renamed));
    }

    private Yaml.Mapping patchSnapshots(Yaml.Mapping root, PackageEdit edit, boolean removal) {
        Yaml.Mapping snapshots = section(root, "snapshots");
        if (snapshots == null) {
            return root;
        }
        String oldKey = edit.getName() + "@" + edit.getOldVersion();
        if (removal) {
            if (referencedByOther(root, edit, 9)) {
                return root;
            }
            return replaceEntryValue(root, "snapshots", removeEntries(snapshots, oldKey));
        }
        Yaml.Mapping.Entry snapshot = findEntry(snapshots, oldKey);
        if (snapshot == null) {
            throw fail(Reason.MALFORMED_LOCK, edit.getName(), "no snapshots entry " + oldKey);
        }
        Yaml.Mapping.Entry renamed = renameKey(snapshot, edit.getName() + "@" + edit.getNewVersion());
        if (edit.isPrunesOrphans()) {
            renamed = pruneSnapshotEdges(renamed, edit);
        }
        return replaceEntryValue(root, "snapshots", replaceEntry(snapshots, oldKey, renamed));
    }

    /**
     * Drop the {@code dependencies}/{@code optionalDependencies} edges the bumped version no longer declares; a
     * snapshot emptied of all edges becomes {@code {}}, and one carrying any other field fails loud.
     */
    private Yaml.Mapping.Entry pruneSnapshotEdges(Yaml.Mapping.Entry snapshot, PackageEdit edit) {
        if (!(snapshot.getValue() instanceof Yaml.Mapping)) {
            return snapshot;
        }
        Set<String> keep = edit.getNewDependencies() == null ?
                Collections.emptySet() : edit.getNewDependencies().keySet();
        String newKey = edit.getName() + "@" + edit.getNewVersion();
        List<Yaml.Mapping.Entry> scopes = new ArrayList<>();
        for (Yaml.Mapping.Entry scopeEntry : ((Yaml.Mapping) snapshot.getValue()).getEntries()) {
            String scope = keyOf(scopeEntry);
            if (!"dependencies".equals(scope) && !"optionalDependencies".equals(scope)) {
                throw fail(Reason.RESOLUTION_REQUIRED, edit.getName(),
                        edit.getName() + " snapshot carries " + scope + "; orphan-prune not yet supported");
            }
            if (!(scopeEntry.getValue() instanceof Yaml.Mapping)) {
                scopes.add(scopeEntry);
                continue;
            }
            List<Yaml.Mapping.Entry> survivors = new ArrayList<>();
            for (Yaml.Mapping.Entry dep : ((Yaml.Mapping) scopeEntry.getValue()).getEntries()) {
                if (keep.contains(keyOf(dep))) {
                    survivors.add(dep);
                }
            }
            if (!survivors.isEmpty()) {
                scopes.add(scopeEntry.withValue(((Yaml.Mapping) scopeEntry.getValue()).withEntries(survivors)));
            }
        }
        if (scopes.isEmpty()) {
            // Keep the renamed entry (its key prefix carries the blank-line separator) and swap only the value
            // to an empty flow mapping {}, borrowed from a parsed template.
            return snapshot.withValue(parseGraftEntry("snapshots:\n  " + newKey + ": {}\n", "snapshots", newKey).getValue());
        }
        return snapshot.withValue(((Yaml.Mapping) snapshot.getValue()).withEntries(scopes));
    }

    private Yaml.Mapping editResolution(Yaml.Mapping body, PackageEdit edit) {
        if (edit.getNewIntegrity() == null) {
            return body;
        }
        Yaml.Mapping.Entry entry = findEntry(body, "resolution");
        if (entry == null || !(entry.getValue() instanceof Yaml.Scalar)) {
            // A block-style resolution map is not the flow-scalar this patcher rewrites integrity inside;
            // silently keeping the OLD integrity would emit a lock a real install rejects.
            throw fail(Reason.MALFORMED_LOCK, edit.getName(),
                    edit.getName() + " resolution is not a flow-scalar; cannot rewrite integrity");
        }
        Yaml.Scalar scalar = (Yaml.Scalar) entry.getValue();
        String value = replaceToken(scalar.getValue(), "integrity", edit.getNewIntegrity());
        if (edit.getNewResolved() != null && value.contains("tarball:")) {
            value = replaceToken(value, "tarball", edit.getNewResolved());
        }
        return replaceEntry(body, "resolution", entry.withValue(scalar.withValue(value)));
    }

    private Yaml.Mapping editEngines(Yaml.Mapping body, PackageEdit edit) {
        WriteThroughMetadata metadata = edit.getWriteThroughMetadata();
        if (metadata == null || metadata.getEngines() == null) {
            return body;
        }
        Yaml.Mapping.Entry entry = findEntry(body, "engines");
        if (entry == null || !(entry.getValue() instanceof Yaml.Scalar)) {
            return body; // engines absent in the raw entry — cannot synthesize the flow map safely
        }
        Yaml.Scalar scalar = (Yaml.Scalar) entry.getValue();
        return replaceEntry(body, "engines", entry.withValue(scalar.withValue(renderEngines(metadata.getEngines()))));
    }

    /** pnpm writes {@code engines} through, but {@code license}/{@code deprecated}/{@code bin} deltas are not modeled, so fail loud rather than silently drop them. */
    private void requireSupportedWriteThrough(PackageEdit edit) {
        WriteThroughMetadata wt = edit.getWriteThroughMetadata();
        if (wt == null) {
            return;
        }
        String changed = wt.getLicense() != null ? "license" :
                wt.getDeprecated() != null ? "deprecated" :
                        wt.getBin() != null ? "bin" : null;
        if (changed != null) {
            throw fail(Reason.RESOLUTION_REQUIRED, edit.getName(),
                    edit.getName() + " " + changed + " metadata changed; native write-through is not supported for pnpm");
        }
    }

    // --- cascade move ------------------------------------------

    /**
     * Apply a transitive forced to move by a direct-dependency bump (v9 only). pnpm keys by resolved version,
     * so the move renames {@code packages}/{@code snapshots} entries {@code <dep>@<old>} to {@code @<new>} and
     * retargets every snapshot reference. The engine has already proven the move is private to the bumped root,
     * so no importer edge or other package needs reconciling.
     */
    private Yaml.Mapping applyForcedMove(Yaml.Mapping root, PackageEdit edit, int major) {
        if (major < 9) {
            throw fail(Reason.RESOLUTION_REQUIRED, edit.getName(),
                    "a pnpm cascade move below lockfileVersion 9 is not yet supported");
        }
        requireSupportedWriteThrough(edit);
        root = patchPackages(root, edit, major, false);
        root = patchSnapshots(root, edit, false);
        return retargetSnapshotReferences(root, edit.getName(), edit.getOldVersion(), edit.getNewVersion());
    }

    /** Rewrite every snapshot's resolved reference to {@code dep@oldVersion} so it points at {@code newVersion}. */
    private Yaml.Mapping retargetSnapshotReferences(Yaml.Mapping root, String dep, String oldVersion, String newVersion) {
        Yaml.Mapping snapshots = section(root, "snapshots");
        if (snapshots == null) {
            return root;
        }
        List<Yaml.Mapping.Entry> entries = new ArrayList<>(snapshots.getEntries());
        boolean changed = false;
        for (int i = 0; i < entries.size(); i++) {
            Yaml.Mapping.Entry entry = entries.get(i);
            if (!(entry.getValue() instanceof Yaml.Mapping)) {
                continue;
            }
            Yaml.Mapping body = (Yaml.Mapping) entry.getValue();
            Yaml.Mapping retargeted = retargetDepReference(body, dep, oldVersion, newVersion);
            if (retargeted != body) {
                entries.set(i, entry.withValue(retargeted));
                changed = true;
            }
        }
        return changed ? replaceEntryValue(root, "snapshots", snapshots.withEntries(entries)) : root;
    }

    private Yaml.Mapping retargetDepReference(Yaml.Mapping body, String dep, String oldVersion, String newVersion) {
        Yaml.Mapping result = body;
        for (String scope : Arrays.asList("dependencies", "optionalDependencies")) {
            Yaml.Mapping.Entry scopeEntry = findEntry(result, scope);
            if (scopeEntry == null || !(scopeEntry.getValue() instanceof Yaml.Mapping)) {
                continue;
            }
            Yaml.Mapping deps = (Yaml.Mapping) scopeEntry.getValue();
            Yaml.Mapping.Entry ref = findEntry(deps, dep);
            if (ref == null || !(ref.getValue() instanceof Yaml.Scalar) ||
                    !oldVersion.equals(((Yaml.Scalar) ref.getValue()).getValue())) {
                continue;
            }
            Yaml.Mapping patchedDeps = setScalar(deps, dep, newVersion);
            result = replaceEntryValue(result, scope, patchedDeps);
        }
        return result;
    }

    // --- content-fork ---------------------------------------

    /**
     * pnpm's reverse-dependent fork (v9 only): add the new version's {@code packages}+{@code snapshots} content
     * and retarget only the importer edge, leaving the old version's entries in place for the reverse-dependent
     * that still resolves to it. Unlike a normal bump it renames nothing.
     */
    private Yaml.Mapping applyContentFork(Yaml.Mapping root, PackageEdit edit, int major, Map<String, String> newConstraints) {
        if (major < 9) {
            throw fail(Reason.RESOLUTION_REQUIRED, edit.getName(),
                    "a pnpm content-fork below lockfileVersion 9 is not yet supported");
        }
        if (edit.getNewIntegrity() == null) {
            throw fail(Reason.UNSUPPORTED_ENTRY_TYPE, edit.getName(), edit.getName() + " has no registry integrity");
        }
        requireSupportedWriteThrough(edit);
        String key = edit.getName() + "@" + edit.getNewVersion();

        Yaml.Mapping packages = section(root, "packages");
        if (packages == null) {
            throw fail(Reason.MALFORMED_LOCK, edit.getName(), "pnpm-lock.yaml has no packages section");
        }
        if (findEntry(packages, key) != null) {
            throw fail(Reason.RESOLUTION_REQUIRED, edit.getName(), key + " already present; dedupe required");
        }
        packages = insertEntrySorted(packages, buildPackagesEntry(edit, key), key);
        root = replaceEntryValue(root, "packages", packages);

        Yaml.Mapping snapshots = section(root, "snapshots");
        if (snapshots == null) {
            throw fail(Reason.MALFORMED_LOCK, edit.getName(), "pnpm-lock.yaml has no snapshots section");
        }
        if (findEntry(snapshots, key) != null) {
            throw fail(Reason.RESOLUTION_REQUIRED, edit.getName(), key + " already present in snapshots; dedupe required");
        }
        snapshots = insertEntrySorted(snapshots, buildSnapshotEntry(edit, key, Collections.emptyMap()), key);
        root = replaceEntryValue(root, "snapshots", snapshots);

        return patchImporter(root, edit, false, newConstraints);
    }

    // --- leaf / clean-closure add ------------------

    /**
     * Insert a brand-new closure member (v9 only): a {@code packages} entry, a {@code snapshots} entry keyed by
     * resolved version, and (for a declared direct dependency) the importer edge. Each lands at pnpm's
     * lexicographic sort position with byte-exact whitespace; a collision or unmodeled surface fails loud.
     */
    private Yaml.Mapping applyAdd(Yaml.Mapping root, PackageEdit edit, int major,
                                  Map<String, String> newConstraints, Map<String, String> addedVersions) {
        if (major < 9) {
            throw fail(Reason.RESOLUTION_REQUIRED, edit.getName(),
                    "adding to a pnpm lockfileVersion below 9 is not yet supported");
        }
        if (edit.getNewIntegrity() == null) {
            throw fail(Reason.UNSUPPORTED_ENTRY_TYPE, edit.getName(), edit.getName() + " has no registry integrity");
        }
        String key = edit.getName() + "@" + edit.getNewVersion();

        Yaml.Mapping packages = section(root, "packages");
        if (packages == null) {
            throw fail(Reason.MALFORMED_LOCK, edit.getName(), "pnpm-lock.yaml has no packages section");
        }
        if (findEntry(packages, key) != null) {
            throw fail(Reason.RESOLUTION_REQUIRED, edit.getName(), key + " already present; dedupe required");
        }
        packages = insertEntrySorted(packages, buildPackagesEntry(edit, key), key);
        root = replaceEntryValue(root, "packages", packages);

        Yaml.Mapping snapshots = section(root, "snapshots");
        if (snapshots == null) {
            throw fail(Reason.MALFORMED_LOCK, edit.getName(), "pnpm-lock.yaml has no snapshots section");
        }
        if (findEntry(snapshots, key) != null) {
            throw fail(Reason.RESOLUTION_REQUIRED, edit.getName(), key + " already present in snapshots; dedupe required");
        }
        snapshots = insertEntrySorted(snapshots, buildSnapshotEntry(edit, key, addedVersions), key);
        root = replaceEntryValue(root, "snapshots", snapshots);

        String specifier = newConstraints.get(edit.getName());
        if (specifier != null) {
            root = insertImporterEdge(root, edit, specifier);
        }
        return root;
    }

    private Yaml.Mapping.Entry buildPackagesEntry(PackageEdit edit, String key) {
        StringBuilder body = new StringBuilder("packages:\n  ").append(key).append(":\n")
                .append("    resolution: {integrity: ").append(edit.getNewIntegrity()).append('}');
        WriteThroughMetadata wt = edit.getWriteThroughMetadata();
        if (wt != null && wt.getEngines() != null) {
            requireQuotableEngines(edit.getName(), wt.getEngines());
            body.append("\n    engines: ").append(renderEngines(wt.getEngines()));
        }
        body.append('\n');
        return parseGraftEntry(body.toString(), "packages", key);
    }

    private Yaml.Mapping.Entry buildSnapshotEntry(PackageEdit edit, String key, Map<String, String> addedVersions) {
        Map<String, String> deps = edit.getNewDependencies();
        if (deps == null || deps.isEmpty()) {
            return parseGraftEntry("snapshots:\n  " + key + ": {}\n", "snapshots", key);
        }
        List<String> names = new ArrayList<>(deps.keySet());
        Collections.sort(names);
        StringBuilder body = new StringBuilder("snapshots:\n  ").append(key).append(":\n    dependencies:");
        for (String dep : names) {
            String resolved = addedVersions.get(dep);
            if (resolved == null) {
                throw fail(Reason.RESOLUTION_REQUIRED, edit.getName(),
                        edit.getName() + " depends on " + dep + " which is not part of the added closure");
            }
            body.append("\n      ").append(dep).append(": ").append(resolved);
        }
        body.append('\n');
        return parseGraftEntry(body.toString(), "snapshots", key);
    }

    private Yaml.Mapping insertImporterEdge(Yaml.Mapping root, PackageEdit edit, String specifier) {
        requirePlainSpecifier(edit.getName(), specifier);
        Yaml.Mapping importers = section(root, "importers");
        if (importers == null) {
            throw fail(Reason.RESOLUTION_REQUIRED, edit.getName(), "cannot add an importer edge to a single-package lock");
        }
        String dir = importerDir(edit);
        Yaml.Mapping.Entry importer = findEntry(importers, dir);
        if (importer == null || !(importer.getValue() instanceof Yaml.Mapping)) {
            throw fail(Reason.MALFORMED_LOCK, edit.getName(), "no importer '" + dir + "'");
        }
        Yaml.Mapping importerBody = (Yaml.Mapping) importer.getValue();
        String scope = edit.getScope();
        Yaml.Mapping.Entry scopeEntry = findEntry(importerBody, scope);
        if (scopeEntry == null || !(scopeEntry.getValue() instanceof Yaml.Mapping)) {
            throw fail(Reason.RESOLUTION_REQUIRED, edit.getName(),
                    "adding the first " + scope + " to importer '" + dir + "' is not yet supported");
        }
        Yaml.Mapping deps = (Yaml.Mapping) scopeEntry.getValue();
        if (findEntry(deps, edit.getName()) != null) {
            throw fail(Reason.RESOLUTION_REQUIRED, edit.getName(), edit.getName() + " is already declared in " + scope);
        }
        String body = "importers:\n  " + dir + ":\n    " + scope + ":\n      " + edit.getName() +
                ":\n        specifier: " + specifier + "\n        version: " + edit.getNewVersion() + '\n';
        Yaml.Mapping.Entry depEntry = parseGraftEntry(body, "importers", dir, scope, edit.getName());
        deps = insertEntrySorted(deps, depEntry, edit.getName());
        importerBody = replaceEntryValue(importerBody, scope, deps);
        return replaceEntryValue(root, "importers", replaceEntryValue(importers, dir, importerBody));
    }

    /** Build an entry from a synthetic mini-lock so its internal formatting round-trips byte-exact. */
    private Yaml.Mapping.Entry parseGraftEntry(String synthetic, String... path) {
        Yaml.Documents docs = parse(synthetic, null);
        if (docs.getDocuments().isEmpty() || !(docs.getDocuments().get(0).getBlock() instanceof Yaml.Mapping)) {
            throw fail(Reason.MALFORMED_LOCK, null, "could not construct pnpm lock entry");
        }
        Yaml.Mapping mapping = (Yaml.Mapping) docs.getDocuments().get(0).getBlock();
        for (int i = 0; i < path.length - 1; i++) {
            Yaml.Mapping.Entry entry = findEntry(mapping, path[i]);
            if (entry == null || !(entry.getValue() instanceof Yaml.Mapping)) {
                throw fail(Reason.MALFORMED_LOCK, null, "could not navigate synthetic lock entry at " + path[i]);
            }
            mapping = (Yaml.Mapping) entry.getValue();
        }
        Yaml.Mapping.Entry leaf = findEntry(mapping, path[path.length - 1]);
        if (leaf == null) {
            throw fail(Reason.MALFORMED_LOCK, null, "could not construct pnpm lock entry " + path[path.length - 1]);
        }
        return leaf;
    }

    /**
     * Splice {@code newEntry} into {@code mapping} at its lexicographic key position, reusing an existing
     * sibling's prefix (pnpm's entries share a uniform blank-line/indent prefix).
     */
    private static Yaml.Mapping insertEntrySorted(Yaml.Mapping mapping, Yaml.Mapping.Entry newEntry, String newKey) {
        List<Yaml.Mapping.Entry> entries = mapping.getEntries();
        if (entries.isEmpty()) {
            throw fail(Reason.MALFORMED_LOCK, null, "cannot derive entry indentation for insert");
        }
        Yaml.Mapping.Entry placed = newEntry.withPrefix(entries.get(0).getPrefix());
        int idx = 0;
        while (idx < entries.size()) {
            String k = keyOf(entries.get(idx));
            if (k != null && k.compareTo(newKey) > 0) {
                break;
            }
            idx++;
        }
        List<Yaml.Mapping.Entry> out = new ArrayList<>(entries.size() + 1);
        out.addAll(entries.subList(0, idx));
        out.add(placed);
        out.addAll(entries.subList(idx, entries.size()));
        return mapping.withEntries(out);
    }

    private static Map<String, String> addedVersions(List<PackageEdit> edits) {
        Map<String, String> versions = new LinkedHashMap<>();
        for (PackageEdit edit : edits) {
            if (edit.getKind() == ADD && edit.getNewVersion() != null) {
                versions.put(edit.getName(), edit.getNewVersion());
            }
        }
        return versions;
    }

    /** A specifier pnpm leaves unquoted (caret/tilde/exact/plain ranges); a quote-requiring range defers. */
    private static void requirePlainSpecifier(String name, String specifier) {
        if (!isPlainYamlScalar(specifier)) {
            throw fail(Reason.RESOLUTION_REQUIRED, name,
                    name + " specifier '" + specifier + "' needs YAML quoting; native pnpm add is not yet supported");
        }
    }

    /** pnpm single-quotes engine ranges; a value it would leave bare would be over-quoted by the renderer. */
    private static void requireQuotableEngines(String name, Map<String, String> engines) {
        for (String value : engines.values()) {
            if (isPlainYamlScalar(value)) {
                throw fail(Reason.RESOLUTION_REQUIRED, name,
                        name + " engine constraint '" + value + "' is not single-quoted by pnpm; native add deferred");
            }
        }
    }

    /** Whether {@code s} is a YAML plain scalar pnpm would emit unquoted (no leading indicator, no {@code :}/{@code #}). */
    private static boolean isPlainYamlScalar(String s) {
        if (s.isEmpty()) {
            return false;
        }
        if ("!&*?|>%@`\"'#,[]{}:- ".indexOf(s.charAt(0)) >= 0) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ':' || c == '#' || c == '\n' || c == '\t') {
                return false;
            }
        }
        return true;
    }

    // --- orphan GC after removals ----------------------------------------

    /**
     * Drop every {@code packages}/{@code snapshots} entry no longer reachable from an importer root after a
     * removal (a removed non-leaf takes its private transitives with it). Runs only for a removal, so a pure
     * bump stays byte-identical.
     */
    private Yaml.Mapping gcOrphans(Yaml.Mapping root, int major) {
        if (major >= 9) {
            Yaml.Mapping snapshots = section(root, "snapshots");
            if (snapshots == null) {
                return root;
            }
            Set<String> reachable = reachableKeys(root, snapshots, "");
            root = replaceEntryValue(root, "snapshots", retainEntries(snapshots, reachable, false));
            Yaml.Mapping packages = section(root, "packages");
            if (packages != null) {
                Set<String> reachableBases = new LinkedHashSet<>();
                for (String key : reachable) {
                    reachableBases.add(stripSuffix(key));
                }
                root = replaceEntryValue(root, "packages", retainEntries(packages, reachableBases, true));
            }
            return root;
        }
        Yaml.Mapping packages = section(root, "packages");
        if (packages == null) {
            return root;
        }
        Set<String> reachable = reachableKeys(root, packages, "/");
        return replaceEntryValue(root, "packages", retainEntries(packages, reachable, false));
    }

    private Set<String> reachableKeys(Yaml.Mapping root, Yaml.Mapping graph, String prefix) {
        Set<String> reachable = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        Yaml.Mapping importers = section(root, "importers");
        List<Yaml.Block> roots = new ArrayList<>();
        if (importers != null) {
            for (Yaml.Mapping.Entry importer : importers.getEntries()) {
                roots.add(importer.getValue());
            }
        } else {
            roots.add(root);
        }
        for (Yaml.Block scopes : roots) {
            for (String key : importerDepKeys(scopes, prefix)) {
                if (reachable.add(key)) {
                    queue.add(key);
                }
            }
        }
        while (!queue.isEmpty()) {
            Yaml.Mapping.Entry entry = findEntry(graph, queue.poll());
            if (entry == null || !(entry.getValue() instanceof Yaml.Mapping)) {
                continue;
            }
            for (String childKey : graphDepKeys((Yaml.Mapping) entry.getValue(), prefix)) {
                if (reachable.add(childKey)) {
                    queue.add(childKey);
                }
            }
        }
        return reachable;
    }

    private static List<String> importerDepKeys(Yaml.Block scopesBlock, String prefix) {
        List<String> out = new ArrayList<>();
        if (!(scopesBlock instanceof Yaml.Mapping)) {
            return out;
        }
        for (String scope : LOCK_SCOPES) {
            Yaml.Mapping.Entry scopeEntry = findEntry((Yaml.Mapping) scopesBlock, scope);
            if (scopeEntry != null && scopeEntry.getValue() instanceof Yaml.Mapping) {
                for (Yaml.Mapping.Entry dep : ((Yaml.Mapping) scopeEntry.getValue()).getEntries()) {
                    String name = keyOf(dep);
                    if (name != null && dep.getValue() instanceof Yaml.Mapping) {
                        Yaml.Mapping.Entry version = findEntry((Yaml.Mapping) dep.getValue(), "version");
                        if (version != null && version.getValue() instanceof Yaml.Scalar) {
                            out.add(prefix + name + "@" + ((Yaml.Scalar) version.getValue()).getValue());
                        }
                    }
                }
            }
        }
        return out;
    }

    private static List<String> graphDepKeys(Yaml.Mapping body, String prefix) {
        List<String> out = new ArrayList<>();
        for (String depScope : Arrays.asList("dependencies", "optionalDependencies")) {
            Yaml.Mapping.Entry scope = findEntry(body, depScope);
            if (scope != null && scope.getValue() instanceof Yaml.Mapping) {
                for (Yaml.Mapping.Entry dep : ((Yaml.Mapping) scope.getValue()).getEntries()) {
                    String name = keyOf(dep);
                    if (name != null && dep.getValue() instanceof Yaml.Scalar) {
                        out.add(prefix + name + "@" + ((Yaml.Scalar) dep.getValue()).getValue());
                    }
                }
            }
        }
        return out;
    }

    private static Yaml.Mapping retainEntries(Yaml.Mapping mapping, Set<String> keep, boolean byBase) {
        List<Yaml.Mapping.Entry> entries = new ArrayList<>();
        for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
            String key = keyOf(entry);
            String probe = key == null ? null : (byBase ? stripSuffix(key) : key);
            if (probe != null && keep.contains(probe)) {
                entries.add(entry);
            }
        }
        return mapping.withEntries(entries);
    }

    // --- reads over importer mappings ------------------------------------

    private List<String> importerVersionsForDep(Yaml.Mapping root, PackageEdit edit) {
        List<String> out = new ArrayList<>();
        Yaml.Mapping scopes = importerScopesFor(root, edit);
        if (scopes != null) {
            String version = depVersion(scopes, edit.getName());
            if (version != null) {
                out.add(version);
            }
        }
        return out;
    }

    private Yaml.@Nullable Mapping importerScopesFor(Yaml.Mapping root, PackageEdit edit) {
        Yaml.Mapping importers = section(root, "importers");
        if (importers == null) {
            return root;
        }
        Yaml.Mapping.Entry importer = findEntry(importers, importerDir(edit));
        return importer != null && importer.getValue() instanceof Yaml.Mapping ? (Yaml.Mapping) importer.getValue() : null;
    }

    private List<String> importerVersionValues(Yaml.Mapping root) {
        List<String> out = new ArrayList<>();
        Yaml.Mapping importers = section(root, "importers");
        if (importers != null) {
            for (Yaml.Mapping.Entry importer : importers.getEntries()) {
                collectScopeVersions(importer.getValue(), out);
            }
        } else {
            collectScopeVersions(root, out);
        }
        return out;
    }

    private static void collectScopeVersions(Yaml.Block scopesBlock, List<String> out) {
        if (!(scopesBlock instanceof Yaml.Mapping)) {
            return;
        }
        Yaml.Mapping scopes = (Yaml.Mapping) scopesBlock;
        for (String scope : LOCK_SCOPES) {
            Yaml.Mapping.Entry scopeEntry = findEntry(scopes, scope);
            if (scopeEntry != null && scopeEntry.getValue() instanceof Yaml.Mapping) {
                for (Yaml.Mapping.Entry dep : ((Yaml.Mapping) scopeEntry.getValue()).getEntries()) {
                    if (dep.getValue() instanceof Yaml.Mapping) {
                        Yaml.Mapping.Entry version = findEntry((Yaml.Mapping) dep.getValue(), "version");
                        if (version != null && version.getValue() instanceof Yaml.Scalar) {
                            out.add(((Yaml.Scalar) version.getValue()).getValue());
                        }
                    }
                }
            }
        }
    }

    private static @Nullable String depVersion(Yaml.Mapping scopes, String name) {
        for (String scope : LOCK_SCOPES) {
            Yaml.Mapping.Entry scopeEntry = findEntry(scopes, scope);
            if (scopeEntry != null && scopeEntry.getValue() instanceof Yaml.Mapping) {
                Yaml.Mapping.Entry dep = findEntry((Yaml.Mapping) scopeEntry.getValue(), name);
                if (dep != null && dep.getValue() instanceof Yaml.Mapping) {
                    Yaml.Mapping.Entry version = findEntry((Yaml.Mapping) dep.getValue(), "version");
                    if (version != null && version.getValue() instanceof Yaml.Scalar) {
                        return ((Yaml.Scalar) version.getValue()).getValue();
                    }
                }
            }
        }
        return null;
    }

    // --- LST mapping helpers ---------------------------------------------

    private static Yaml.@Nullable Mapping section(Yaml.Mapping root, String name) {
        Yaml.Mapping.Entry entry = findEntry(root, name);
        return entry != null && entry.getValue() instanceof Yaml.Mapping ? (Yaml.Mapping) entry.getValue() : null;
    }

    private static List<String> keys(Yaml.@Nullable Mapping mapping) {
        List<String> out = new ArrayList<>();
        if (mapping != null) {
            for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
                String key = keyOf(entry);
                if (key != null) {
                    out.add(key);
                }
            }
        }
        return out;
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

    private static Yaml.Mapping replaceEntry(Yaml.Mapping mapping, String key, Yaml.Mapping.Entry replacement) {
        List<Yaml.Mapping.Entry> entries = new ArrayList<>(mapping.getEntries());
        for (int i = 0; i < entries.size(); i++) {
            if (key.equals(keyOf(entries.get(i)))) {
                entries.set(i, replacement);
                return mapping.withEntries(entries);
            }
        }
        return mapping;
    }

    private static Yaml.Mapping replaceEntryValue(Yaml.Mapping mapping, String key, Yaml.Block value) {
        List<Yaml.Mapping.Entry> entries = new ArrayList<>(mapping.getEntries());
        for (int i = 0; i < entries.size(); i++) {
            Yaml.Mapping.Entry entry = entries.get(i);
            if (key.equals(keyOf(entry))) {
                entries.set(i, entry.withValue(value));
                return mapping.withEntries(entries);
            }
        }
        return mapping;
    }

    private static Yaml.Mapping removeEntries(Yaml.Mapping mapping, String key) {
        List<Yaml.Mapping.Entry> entries = new ArrayList<>();
        for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
            if (!key.equals(keyOf(entry))) {
                entries.add(entry);
            }
        }
        return mapping.withEntries(entries);
    }

    private static Yaml.Mapping.Entry renameKey(Yaml.Mapping.Entry entry, String newKey) {
        if (!(entry.getKey() instanceof Yaml.Scalar)) {
            throw fail(Reason.MALFORMED_LOCK, null, "cannot rename a non-scalar key");
        }
        return entry.withKey(((Yaml.Scalar) entry.getKey()).withValue(newKey));
    }

    private static Yaml.Mapping setScalar(Yaml.Mapping mapping, String key, String value) {
        Yaml.Mapping.Entry entry = findEntry(mapping, key);
        if (entry == null || !(entry.getValue() instanceof Yaml.Scalar)) {
            throw fail(Reason.MALFORMED_LOCK, null, "expected scalar entry '" + key + "'");
        }
        return replaceEntry(mapping, key, entry.withValue(((Yaml.Scalar) entry.getValue()).withValue(value)));
    }

    // --- small utilities -------------------------------------------------

    private static String packageKey(String name, String version, int major) {
        String base = name + "@" + version;
        return major >= 9 ? base : "/" + base;
    }

    private static String importerDir(PackageEdit edit) {
        return edit.getImporterDir() == null ? "." : edit.getImporterDir();
    }

    private static String stripSuffix(String version) {
        int paren = version.indexOf('(');
        return paren >= 0 ? version.substring(0, paren) : version;
    }

    private static boolean isUnsupportedEntry(String version) {
        for (String prefix : UNSUPPORTED_ENTRY_PREFIXES) {
            if (version.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String replaceToken(String flowMap, String token, String value) {
        return flowMap.replaceFirst(token + ":\\s*[^,}]+", Matcher.quoteReplacement(token + ": " + value));
    }

    private static String renderEngines(Map<String, String> engines) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> engine : engines.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(engine.getKey()).append(": '").append(engine.getValue()).append('\'');
        }
        return sb.append('}').toString();
    }

    private Map<String, String> declaredConstraints(@Nullable String packageJson) {
        Map<String, String> out = new LinkedHashMap<>();
        if (packageJson == null) {
            return out;
        }
        try {
            JsonNode root = JSON.readTree(packageJson);
            if (root == null || !root.isObject()) {
                return out;
            }
            for (String scope : MANIFEST_SCOPES) {
                JsonNode node = root.get(scope);
                if (node != null && node.isObject()) {
                    for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                        Map.Entry<String, JsonNode> field = it.next();
                        if (!out.containsKey(field.getKey())) {
                            out.put(field.getKey(), field.getValue().asText());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Best-effort: a missing/unreadable specifier just leaves the importer specifier untouched.
        }
        return out;
    }

    private static int lockfileMajor(Yaml.Mapping root, @Nullable String pkgForFailure) {
        Yaml.Mapping.Entry entry = findEntry(root, "lockfileVersion");
        if (entry == null || !(entry.getValue() instanceof Yaml.Scalar)) {
            throw fail(Reason.MALFORMED_LOCK, pkgForFailure, "pnpm-lock.yaml has no lockfileVersion");
        }
        String raw = ((Yaml.Scalar) entry.getValue()).getValue().trim();
        int dot = raw.indexOf('.');
        String head = dot >= 0 ? raw.substring(0, dot) : raw;
        try {
            return Integer.parseInt(head.trim());
        } catch (NumberFormatException nfe) {
            throw fail(Reason.MALFORMED_LOCK, pkgForFailure, "unrecognised lockfileVersion: " + raw);
        }
    }

    private Yaml.Documents parse(String content, @Nullable Path lockPath) {
        Path path = lockPath == null ? Paths.get("pnpm-lock.yaml") : lockPath;
        SourceFile source;
        try {
            ExecutionContext ctx = new InMemoryExecutionContext();
            Parser.Input input = Parser.Input.fromString(path, content);
            source = new YamlParser().parseInputs(singletonList(input), null, ctx).findFirst().orElse(null);
        } catch (RuntimeException e) {
            throw fail(Reason.MALFORMED_LOCK, null, "unparseable pnpm-lock.yaml: " + e.getMessage());
        }
        if (!(source instanceof Yaml.Documents)) {
            throw fail(Reason.MALFORMED_LOCK, null, "pnpm-lock.yaml could not be parsed as YAML");
        }
        return (Yaml.Documents) source;
    }

    private static @Nullable String firstName(LockEditSet edits) {
        return edits.getEdits().isEmpty() ? null : edits.getEdits().get(0).getName();
    }

    private static EngineFailure fail(Reason reason, @Nullable String packageName, String detail) {
        return new EngineFailure(reason, packageName, detail);
    }
}
