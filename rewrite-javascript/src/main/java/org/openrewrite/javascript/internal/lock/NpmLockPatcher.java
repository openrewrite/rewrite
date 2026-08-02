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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;
import org.openrewrite.javascript.internal.lock.LockEditSet.EntryMetadata;
import org.openrewrite.json.internal.JsonPrinter;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.json.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit.Kind.*;

/**
 * Byte-exact {@link LockPatcher} for npm {@code package-lock.json} (lockfileVersion 2 and 3, full workspace
 * support). It parses the captured lock with the byte-lossless rewrite-json LST and rewrites only the entries
 * the {@link LockEditSet} names: the {@code packages} entry, the importer's declared constraint, and (v2 only)
 * the legacy {@code dependencies} tree. A removal also drops orphaned transitives; anything the format cannot
 * express byte-exactly fails loud.
 */
public final class NpmLockPatcher implements LockPatcher {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final List<String> IMPORTER_SCOPES = Arrays.asList(
            "dependencies", "devDependencies", "peerDependencies", "optionalDependencies");

    @Override
    public String patch(LockEditSet edits) {
        Json.Document doc = LockJson.parse(edits.getExistingLockContent(), null);
        if (!(doc.getValue() instanceof Json.JsonObject)) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "package-lock.json root is not an object");
        }
        Json.JsonObject root = (Json.JsonObject) doc.getValue();

        int lockfileVersion = lockfileVersion(root);
        if (lockfileVersion != 2 && lockfileVersion != 3) {
            throw new EngineFailure(Reason.UNSUPPORTED_LOCKFILE_VERSION, null,
                    "package-lock.json lockfileVersion " + lockfileVersion + " is not supported (need 2 or 3)");
        }
        if (LockJson.objectMember(root, "packages") == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_LOCKFILE_VERSION, null,
                    "package-lock.json has no packages map (v1 is not supported)");
        }

        JsonNode editedManifest = parseManifest(edits.getEditedPackageJson());

        // A relocate-nest copies a pre-edit entry, so it runs before the bumps mutate it; the v2 legacy nest is
        // captured now but inserted after the bumps, so applyLegacyTree's fork guard still sees a flat tree.
        List<LegacyNest> legacyNests = new ArrayList<>();
        for (PackageEdit edit : edits.getEdits()) {
            if (edit.getKind() == REVERSE_NEST) {
                if (lockfileVersion == 2) {
                    legacyNests.add(captureLegacyNest(root, edit));
                }
                root = relocatePackagesEntry(root, edit);
            }
        }

        List<PackageEdit> removals = new ArrayList<>();
        for (PackageEdit edit : edits.getEdits()) {
            if (edit.getNestedUnder() != null && (edit.getKind() == ADD || edit.getKind() == REVERSE_NEST)) {
                continue; // nested adds and relocations run in their own pass
            }
            if (edit.getKind() == PROMOTION) {
                root = applyPromotion(root, lockfileVersion, editedManifest, edit);
                continue;
            }
            if (edit.getNewVersion() == null) {
                removals.add(edit);
                continue;
            }
            if (edit.getKind() == ADD) {
                root = applyAdd(root, lockfileVersion, editedManifest, edit);
                continue;
            }
            root = applyBump(root, lockfileVersion, editedManifest, edit);
        }
        if (!removals.isEmpty()) {
            root = applyRemovals(root, lockfileVersion, removals);
        }
        // An orphan-prune bump dropped a dependency edge; GC every installed entry it left unreachable.
        boolean prunes = false;
        for (PackageEdit edit : edits.getEdits()) {
            if (edit.isPrunesOrphans()) {
                prunes = true;
                break;
            }
        }
        if (prunes) {
            root = gcOrphansAfterBump(root, lockfileVersion);
        }
        // A fresh nested add (a new closure member an incompatible top-level pin excludes) inserts after the
        // top-level adds, so applyAdd's flat-placement check never sees the nest being created.
        for (PackageEdit edit : edits.getEdits()) {
            if (edit.getKind() == ADD && edit.getNestedUnder() != null) {
                root = applyNestedAdd(root, lockfileVersion, edit);
            }
        }
        for (LegacyNest nest : legacyNests) {
            root = insertLegacyNest(root, nest);
        }

        return doc.withValue(root).printAll();
    }

    // --- version bump -----------------------------------------------------

    private Json.JsonObject applyBump(Json.JsonObject root, int lockfileVersion,
                                      @Nullable JsonNode editedManifest, PackageEdit edit) {
        String name = edit.getName();
        boolean placementMoves = !edit.getNewVersion().equals(edit.getOldVersion());
        EntryMetadata md = edit.getMetadata();
        boolean flagsOnly = !placementMoves && md != null && md.isFlagsChanged();

        // Site (1): the installed package placement, when the resolved version moves or only its flags change.
        if (placementMoves || flagsOnly) {
            String entryKey = installedKey(edit);
            Json.JsonObject packages = requirePackages(root);
            Json.JsonObject entry = LockJson.objectMember(packages, entryKey);
            if (entry == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        "no packages entry for " + entryKey);
            }
            if (placementMoves) {
                requireRegistryEntry(name, edit, entry);
                entry = setStringField(entry, "version", edit.getNewVersion());
                entry = setStringField(entry, "resolved", edit.getNewResolved());
                entry = setStringField(entry, "integrity", edit.getNewIntegrity());
            }
            entry = applyMetadata(name, packages, entry, md);
            if (placementMoves) {
                if (edit.isAddsDependencyEdges()) {
                    // add-during-bump: the entry gains dependency edges whose subtrees are placed as fresh ADDs; graft
                    // the full new dependencies map at npm's field position (v3 only; the engine gates v2 out).
                    Map<String, String> deps = edit.getNewDependencies();
                    entry = writeObjectMember(name, packages, entry, "dependencies",
                            deps == null ? null : JSON.valueToTree(deps));
                } else {
                    // A cascade bump re-pins the entry's own dependency edges (unchanged ones stay byte-identical); an
                    // added edge fails loud, a dropped edge orphan-prunes when the edit allows it.
                    entry = reconcileConstraintMap(name, entry, "dependencies", edit.getNewDependencies(), edit.isPrunesOrphans());
                }
            }
            packages = LockJson.replaceValue(packages, entryKey, entry);
            root = LockJson.replaceValue(root, "packages", packages);
        }

        // Site (2): the importer's declared constraint mirror.
        root = applyImporterConstraint(root, editedManifest, edit);

        // Site (3): the v2 legacy dependencies tree second writer.
        if (lockfileVersion == 2 && (placementMoves || flagsOnly)) {
            if (edit.getNestedUnder() != null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        "bumping a nested entry in a lockfileVersion 2 lock is not yet supported");
            }
            root = applyLegacyTree(root, edit);
        }
        return root;
    }

    /** The {@code packages} key this edit rewrites: top-level or nested one level under a dependent. */
    private static String installedKey(PackageEdit edit) {
        return (edit.getNestedUnder() == null ? "" : "node_modules/" + edit.getNestedUnder() + "/") +
                "node_modules/" + edit.getName();
    }

    private Json.JsonObject applyImporterConstraint(Json.JsonObject root, @Nullable JsonNode editedManifest,
                                                    PackageEdit edit) {
        String newConstraint = lookupConstraint(editedManifest, edit.getScope(), edit.getName());
        if (newConstraint == null) {
            return root;
        }
        String importerKey = edit.getImporterDir() == null ? "" : edit.getImporterDir();
        Json.JsonObject packages = requirePackages(root);
        Json.JsonObject importer = LockJson.objectMember(packages, importerKey);
        if (importer == null) {
            return root;
        }
        Json.JsonObject scope = LockJson.objectMember(importer, edit.getScope());
        if (scope == null || LockJson.member(scope, edit.getName()) == null) {
            return root;
        }
        scope = setStringField(scope, edit.getName(), newConstraint);
        importer = LockJson.replaceValue(importer, edit.getScope(), scope);
        packages = LockJson.replaceValue(packages, importerKey, importer);
        return LockJson.replaceValue(root, "packages", packages);
    }

    private Json.JsonObject applyLegacyTree(Json.JsonObject root, PackageEdit edit) {
        String name = edit.getName();
        Json.JsonObject legacy = LockJson.objectMember(root, "dependencies");
        if (legacy == null) {
            return root;
        }
        if (hasNestedOccurrence(legacy, name)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " appears nested in the v2 legacy dependencies tree (fork/dedupe)");
        }
        Json.JsonObject entry = LockJson.objectMember(legacy, name);
        if (entry == null) {
            return root;
        }
        String legacyVersion = stringField(entry, "version");
        if (legacyVersion != null && (legacyVersion.startsWith("file:") || legacyVersion.startsWith("link:"))) {
            return root; // workspace self-link, never a registry bump
        }
        if (legacyVersion != null && !legacyVersion.equals(edit.getOldVersion())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " v2 legacy entry version " + legacyVersion + " != locked " + edit.getOldVersion());
        }
        if (!edit.getNewVersion().equals(edit.getOldVersion())) {
            entry = setStringField(entry, "version", edit.getNewVersion());
            entry = setStringField(entry, "resolved", edit.getNewResolved());
            entry = setStringField(entry, "integrity", edit.getNewIntegrity());
            if (edit.isAddsDependencyEdges()) {
                // add-during-bump: the legacy mirror gains a `requires` map (its edges' subtrees added as fresh entries);
                // graft the full new map at npm's field position.
                Map<String, String> deps = edit.getNewDependencies();
                entry = writeObjectMember(name, legacy, entry, "requires", deps == null ? null : JSON.valueToTree(deps));
            } else {
                // The v2 legacy tree mirrors a dependent's edges under `requires`; a cascade re-pins them, an
                // orphan-prune drops them.
                entry = reconcileConstraintMap(name, entry, "requires", edit.getNewDependencies(), edit.isPrunesOrphans());
            }
        }
        EntryMetadata md = edit.getMetadata();
        if (md != null && md.isFlagsChanged()) {
            entry = writeLegacyFlags(name, entry, md);
        }
        legacy = LockJson.replaceValue(legacy, name, entry);
        return LockJson.replaceValue(root, "dependencies", legacy);
    }

    /**
     * Exact-set the v2 legacy entry's {@code dev}/{@code optional} flags. The legacy serialization of
     * {@code devOptional}/{@code peer} is not verified, so any involvement defers.
     */
    private Json.JsonObject writeLegacyFlags(String name, Json.JsonObject entry, EntryMetadata md) {
        if (Boolean.TRUE.equals(md.getDevOptional()) || Boolean.TRUE.equals(md.getPeer()) ||
                LockJson.member(entry, "devOptional") != null || LockJson.member(entry, "peer") != null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " devOptional/peer flags in a lockfileVersion 2 legacy tree are not yet supported");
        }
        entry = writeFlag(entry, "dev", md.getDev());
        return writeFlag(entry, "optional", md.getOptional());
    }

    /**
     * Re-pin an entry's {@code dependencies}/{@code requires} constraint values to a moved version's edges; only
     * changed values are rewritten, and an edge added or removed by the upgrade reshapes the tree and fails loud.
     */
    private Json.JsonObject reconcileConstraintMap(String name, Json.JsonObject entry, String mapKey,
                                                   @Nullable Map<String, String> newDeps, boolean allowDrops) {
        Map<String, String> deps = newDeps == null ? Collections.emptyMap() : newDeps;
        if (deps.isEmpty() && !allowDrops) {
            return entry; // no constraint reconciliation requested (plain bump / leaf mover)
        }
        Json.JsonObject map = LockJson.objectMember(entry, mapKey);
        if (map == null) {
            if (deps.isEmpty()) {
                return entry; // orphan-prune of an entry with no such map to begin with
            }
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " gained a " + mapKey + " map on upgrade (not yet supported)");
        }
        Set<String> mapKeys = memberKeys(map);
        if (allowDrops) {
            // An orphan-prune may only DROP edges the new version no longer declares; a fresh edge is an
            // add-during-bump (deferred, fail loud).
            for (String k : deps.keySet()) {
                if (!mapKeys.contains(k)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                            name + " added a " + mapKey + " edge to " + k + " on upgrade (not yet supported)");
                }
            }
            Set<String> dropped = new LinkedHashSet<>();
            for (String k : mapKeys) {
                if (!deps.containsKey(k)) {
                    dropped.add(k);
                }
            }
            if (dropped.equals(mapKeys)) {
                return removeMembers(entry, Collections.singleton(mapKey)); // every edge dropped: drop the map
            }
            if (!dropped.isEmpty()) {
                map = removeMembers(map, dropped);
            }
        } else if (!mapKeys.equals(deps.keySet())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " added/removed a " + mapKey + " edge on upgrade (not yet supported)");
        }
        boolean changed = false;
        for (Map.Entry<String, String> e : deps.entrySet()) {
            if (!e.getValue().equals(stringField(map, e.getKey()))) {
                map = setStringField(map, e.getKey(), e.getValue());
                changed = true;
            }
        }
        return changed || allowDrops ? LockJson.replaceValue(entry, mapKey, map) : entry;
    }

    private static Set<String> memberKeys(Json.JsonObject obj) {
        Set<String> keys = new LinkedHashSet<>();
        for (Json member : obj.getMembers()) {
            if (member instanceof Json.Member) {
                String key = LockJson.memberKey((Json.Member) member);
                if (key != null) {
                    keys.add(key);
                }
            }
        }
        return keys;
    }

    // --- leaf add ----------------------------------

    /**
     * Insert a brand-new leaf: a {@code packages} entry, the importer's declared constraint (creating the scope
     * object when absent), and (v2 only) the minimal legacy tree entry. Each insert lands at npm's own sort
     * position ({@link NpmKeyOrder}) with byte-exact whitespace.
     */
    private Json.JsonObject applyAdd(Json.JsonObject root, int lockfileVersion,
                                     @Nullable JsonNode editedManifest, PackageEdit edit) {
        String name = edit.getName();
        Json.JsonObject packages = requirePackages(root);
        requireAddNotEntangled(packages, name);

        String entryKey = "node_modules/" + name;
        if (LockJson.member(packages, entryKey) != null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + " is already placed in node_modules");
        }
        if (edit.getNewResolved() == null || edit.getNewIntegrity() == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, name, name + " has no registry locator");
        }

        // Site (1): the hoisted package entry.
        String entryText = leafEntryText(packages, edit);
        packages = graftSorted(packages, entryKey, entryText, true);
        root = LockJson.replaceValue(root, "packages", packages);

        // Site (2): the importer's declared constraint.
        root = insertImporterConstraint(root, editedManifest, edit);

        // Site (3): the v2 legacy dependencies tree.
        if (lockfileVersion == 2) {
            root = insertLegacyEntry(root, edit);
        }
        return root;
    }

    /**
     * Promote an already-installed transitive to a declared dependency: the {@code node_modules/<name>} entry
     * stays, so only the importer edge is written. A dev→prod promotion also rewrites the entry's flags (both
     * trees on v2) through the edit's metadata.
     */
    private Json.JsonObject applyPromotion(Json.JsonObject root, int lockfileVersion,
                                           @Nullable JsonNode editedManifest, PackageEdit edit) {
        root = insertImporterConstraint(root, editedManifest, edit);
        EntryMetadata md = edit.getMetadata();
        if (md != null && md.isFlagsChanged()) {
            Json.JsonObject packages = requirePackages(root);
            String entryKey = "node_modules/" + edit.getName();
            Json.JsonObject entry = LockJson.objectMember(packages, entryKey);
            if (entry != null) {
                entry = applyMetadata(edit.getName(), packages, entry, md);
                packages = LockJson.replaceValue(packages, entryKey, entry);
                root = LockJson.replaceValue(root, "packages", packages);
            }
            if (lockfileVersion == 2) {
                Json.JsonObject legacy = LockJson.objectMember(root, "dependencies");
                Json.JsonObject legacyEntry = legacy == null ? null : LockJson.objectMember(legacy, edit.getName());
                if (legacyEntry != null) {
                    legacyEntry = writeLegacyFlags(edit.getName(), legacyEntry, md);
                    legacy = LockJson.replaceValue(legacy, edit.getName(), legacyEntry);
                    root = LockJson.replaceValue(root, "dependencies", legacy);
                }
            }
        }
        return root;
    }

    /**
     * The leaf entry object source in npm's serialization order: object-valued members after scalar/array-valued
     * ones, then {@link NpmKeyOrder} within a group. Metadata values are pretty-printed via {@link #renderNode}.
     */
    private String leafEntryText(Json.JsonObject packages, PackageEdit edit) {
        String fieldWs = nestedMemberWhitespace(packages);
        String closeWs = LockJson.memberWhitespace(packages);
        if (fieldWs == null || closeWs == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, edit.getName(), "cannot derive entry indentation");
        }
        String keyIndent = indentOf(fieldWs);
        String unit = keyIndent.length() > indentOf(closeWs).length() ?
                keyIndent.substring(indentOf(closeWs).length()) : "  ";

        List<EntryField> fields = new ArrayList<>();
        fields.add(new EntryField("version", jsonEncode(edit.getNewVersion()), false));
        fields.add(new EntryField("resolved", jsonEncode(edit.getNewResolved()), false));
        fields.add(new EntryField("integrity", jsonEncode(edit.getNewIntegrity()), false));
        EntryMetadata wt = edit.getMetadata();
        if (wt != null) {
            if (Boolean.TRUE.equals(wt.getDev())) {
                fields.add(new EntryField("dev", "true", false));
            }
            if (Boolean.TRUE.equals(wt.getOptional())) {
                fields.add(new EntryField("optional", "true", false));
            }
            if (Boolean.TRUE.equals(wt.getDevOptional())) {
                fields.add(new EntryField("devOptional", "true", false));
            }
            if (Boolean.TRUE.equals(wt.getPeer())) {
                fields.add(new EntryField("peer", "true", false));
            }
            if (wt.getDeprecated() != null) {
                fields.add(new EntryField("deprecated", jsonEncode(wt.getDeprecated()), false));
            }
            if (Boolean.TRUE.equals(wt.getHasInstallScript())) {
                fields.add(new EntryField("hasInstallScript", "true", false));
            }
            if (wt.getLicense() != null) {
                fields.add(new EntryField("license", jsonEncode(wt.getLicense()), false));
            }
            addMetadataField(fields, "os", wt.getOs(), keyIndent, unit);
            addMetadataField(fields, "cpu", wt.getCpu(), keyIndent, unit);
            addMetadataField(fields, "libc", wt.getLibc(), keyIndent, unit);
            addMetadataField(fields, "engines", wt.getEngines(), keyIndent, unit);
            addMetadataField(fields, "bin", wt.getBin(), keyIndent, unit);
            addMetadataField(fields, "funding", wt.getFunding(), keyIndent, unit);
            addMetadataField(fields, "peerDependencies", wt.getPeerDependencies(), keyIndent, unit);
            addMetadataField(fields, "peerDependenciesMeta", wt.getPeerDependenciesMeta(), keyIndent, unit);
        }
        // A closure member records its dependency edges as constraints (the resolved versions live
        // implicitly via placement); npm keeps them under the entry's `dependencies` map.
        if (edit.getNewDependencies() != null && !edit.getNewDependencies().isEmpty()) {
            addMetadataField(fields, "dependencies", edit.getNewDependencies(), keyIndent, unit);
        }
        fields.sort((a, b) -> a.object != b.object ? (a.object ? 1 : -1) : NpmKeyOrder.compareKeys(a.key, b.key));

        List<String> rendered = new ArrayList<>();
        for (EntryField f : fields) {
            rendered.add(field(fieldWs, f.key, f.value));
        }
        return "{" + String.join(",", rendered) + closeWs + "}";
    }

    private void addMetadataField(List<EntryField> fields, String key, @Nullable Object value,
                                  String keyIndent, String unit) {
        if (value == null) {
            return;
        }
        JsonNode node = value instanceof JsonNode ? (JsonNode) value : JSON.valueToTree(value);
        fields.add(new EntryField(key, renderNode(node, keyIndent, unit), node.isObject()));
    }

    /** Pretty-print a JSON value exactly as npm's {@code json-stringify-nice} does at {@code indent}. */
    private static String renderNode(JsonNode node, String indent, String unit) {
        return NpmJson.render(node, indent, unit);
    }

    /** The indentation (no newline) of an object member's prefix whitespace. */
    private static String indentOf(String ws) {
        int nl = ws.lastIndexOf('\n');
        return nl < 0 ? ws : ws.substring(nl + 1);
    }

    /** A pending entry member: its key, source value, and whether the value is a JSON object (sorts last). */
    private static final class EntryField {
        final String key;
        final String value;
        final boolean object;

        EntryField(String key, String value, boolean object) {
            this.key = key;
            this.value = value;
            this.object = object;
        }
    }

    private Json.JsonObject insertImporterConstraint(Json.JsonObject root, @Nullable JsonNode editedManifest,
                                                     PackageEdit edit) {
        String newConstraint = lookupConstraint(editedManifest, edit.getScope(), edit.getName());
        if (newConstraint == null) {
            return root;
        }
        String importerKey = edit.getImporterDir() == null ? "" : edit.getImporterDir();
        Json.JsonObject packages = requirePackages(root);
        Json.JsonObject importer = LockJson.objectMember(packages, importerKey);
        if (importer == null) {
            return root;
        }
        Json.JsonObject scope = LockJson.objectMember(importer, edit.getScope());
        if (scope != null) {
            // Existing scope: insert the single scalar constraint, sorted.
            scope = graftSorted(scope, edit.getName(), jsonEncode(newConstraint), false);
            importer = LockJson.replaceValue(importer, edit.getScope(), scope);
        } else {
            // First dependency of this scope: create the whole scope object with one member.
            String fieldWs = nestedMemberWhitespace(importer);
            String closeWs = LockJson.memberWhitespace(importer);
            if (fieldWs == null) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, edit.getName(), "cannot derive importer indentation");
            }
            String scopeText = "{" + field(fieldWs, edit.getName(), jsonEncode(newConstraint)) + closeWs + "}";
            importer = graftSorted(importer, edit.getScope(), scopeText, true);
        }
        packages = LockJson.replaceValue(packages, importerKey, importer);
        return LockJson.replaceValue(root, "packages", packages);
    }

    private Json.JsonObject insertLegacyEntry(Json.JsonObject root, PackageEdit edit) {
        Json.JsonObject legacy = LockJson.objectMember(root, "dependencies");
        if (legacy == null) {
            return root;
        }
        // The v2 legacy tree marks dev entries with "dev": true; its byte-exact form for a dev closure
        // add is not yet verified, so defer rather than emit a maybe-wrong entry.
        EntryMetadata md = edit.getMetadata();
        if ("devDependencies".equals(edit.getScope()) || (md != null && Boolean.TRUE.equals(md.getDev()))) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, edit.getName(),
                    "adding a devDependency closure to a lockfileVersion 2 lock is not yet supported");
        }
        String fieldWs = nestedMemberWhitespace(legacy);
        String closeWs = LockJson.memberWhitespace(legacy);
        if (fieldWs == null || closeWs == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, edit.getName(), "cannot derive legacy tree indentation");
        }
        List<String> fields = new ArrayList<>();
        fields.add(field(fieldWs, "version", jsonEncode(edit.getNewVersion())));
        fields.add(field(fieldWs, "resolved", jsonEncode(edit.getNewResolved())));
        fields.add(field(fieldWs, "integrity", jsonEncode(edit.getNewIntegrity())));
        // A dependent records its edges under `requires` (constraints); a leaf transitive has none.
        if (edit.getNewDependencies() != null && !edit.getNewDependencies().isEmpty()) {
            String keyIndent = indentOf(fieldWs);
            String closeIndent = indentOf(closeWs);
            String unit = keyIndent.length() > closeIndent.length() ? keyIndent.substring(closeIndent.length()) : "  ";
            String requires = renderNode(JSON.valueToTree(edit.getNewDependencies()), keyIndent, unit);
            fields.add(field(fieldWs, "requires", requires));
        }
        String entryText = "{" + String.join(",", fields) + closeWs + "}";
        legacy = graftSorted(legacy, edit.getName(), entryText, true);
        return LockJson.replaceValue(root, "dependencies", legacy);
    }

    // --- reverse-dependent nest -----------------------------

    /**
     * Relocate the pre-edit {@code node_modules/<name>} entry to {@code node_modules/<dependent>/node_modules/<name>}
     * byte-for-byte; npm nests the old version there when the top-level slot goes to the new one. The value sits at
     * the same depth, so only the key and sort position change.
     */
    private Json.JsonObject relocatePackagesEntry(Json.JsonObject root, PackageEdit edit) {
        String name = edit.getName();
        Json.JsonObject packages = requirePackages(root);
        Json.Member top = LockJson.member(packages, "node_modules/" + name);
        if (top == null || !(top.getValue() instanceof Json.JsonObject)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, "no node_modules/" + name + " entry to nest");
        }
        String nestedKey = "node_modules/" + edit.getNestedUnder() + "/node_modules/" + name;
        if (LockJson.member(packages, nestedKey) != null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, nestedKey + " already present (fork exists)");
        }
        packages = graftSorted(packages, nestedKey, objectSource(top.getValue()), true);
        return LockJson.replaceValue(root, "packages", packages);
    }

    /**
     * Insert a brand-new nested leaf at {@code node_modules/<dependent>/node_modules/<name>}: a closure member an
     * incompatible top-level pin excludes. Serialized like a top-level leaf add ({@link #leafEntryText}) but at the
     * nested key with no importer edge; v2's deeper legacy nesting fails loud.
     */
    private Json.JsonObject applyNestedAdd(Json.JsonObject root, int lockfileVersion, PackageEdit edit) {
        if (lockfileVersion == 2) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, edit.getName(),
                    "nesting an added dependency into a lockfileVersion 2 lock is not yet supported");
        }
        String name = edit.getName();
        if (edit.getNewResolved() == null || edit.getNewIntegrity() == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, name, name + " has no registry locator");
        }
        Json.JsonObject packages = requirePackages(root);
        String nestedKey = "node_modules/" + edit.getNestedUnder() + "/node_modules/" + name;
        if (LockJson.member(packages, nestedKey) != null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, nestedKey + " already present (fork exists)");
        }
        packages = graftSorted(packages, nestedKey, leafEntryText(packages, edit), true);
        return LockJson.replaceValue(root, "packages", packages);
    }

    /** A member value printed as source with its leading prefix stripped, ready to re-graft under a fresh key. */
    private static String objectSource(JsonValue value) {
        String printed = value.print(new JsonPrinter<Integer>());
        int i = 0;
        while (i < printed.length() && Character.isWhitespace(printed.charAt(i))) {
            i++;
        }
        return printed.substring(i);
    }

    /**
     * Capture the pre-bump top-level legacy entry (minimal {@code version}/{@code resolved}/{@code integrity}) so
     * the v2 nested entry {@code dependencies.<dependent>.dependencies.<name>} holds the old version even though it
     * lands after the bump has moved its sibling.
     */
    private LegacyNest captureLegacyNest(Json.JsonObject root, PackageEdit edit) {
        Json.JsonObject legacy = LockJson.objectMember(root, "dependencies");
        Json.JsonObject entry = legacy == null ? null : LockJson.objectMember(legacy, edit.getName());
        if (entry == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, edit.getName(),
                    "no legacy dependencies entry to nest for " + edit.getName());
        }
        String version = stringField(entry, "version");
        String resolved = stringField(entry, "resolved");
        String integrity = stringField(entry, "integrity");
        if (version == null || resolved == null || integrity == null ||
                !LEGACY_MINIMAL_KEYS.containsAll(memberKeys(entry))) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, edit.getName(),
                    edit.getName() + " legacy entry is not a minimal version/resolved/integrity leaf; nesting deferred");
        }
        return new LegacyNest(edit.getNestedUnder(), edit.getName(), version, resolved, integrity);
    }

    private Json.JsonObject insertLegacyNest(Json.JsonObject root, LegacyNest nest) {
        Json.JsonObject legacy = LockJson.objectMember(root, "dependencies");
        Json.JsonObject dependent = legacy == null ? null : LockJson.objectMember(legacy, nest.dependent);
        if (dependent == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, nest.name,
                    "no legacy entry for " + nest.dependent + " to nest under");
        }
        if (LockJson.objectMember(dependent, "dependencies") != null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, nest.name,
                    nest.dependent + " already has a nested legacy dependencies tree; merge not yet supported");
        }
        String fieldWs = nestedMemberWhitespace(legacy);
        String closeWs = LockJson.memberWhitespace(legacy);
        if (fieldWs == null || closeWs == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, nest.name, "cannot derive legacy nest indentation");
        }
        String keyIndent = indentOf(fieldWs);
        String unit = keyIndent.length() > indentOf(closeWs).length() ?
                keyIndent.substring(indentOf(closeWs).length()) : "  ";
        ObjectNode child = JSON.createObjectNode();
        ObjectNode inner = child.putObject(nest.name);
        inner.put("version", nest.version);
        inner.put("resolved", nest.resolved);
        inner.put("integrity", nest.integrity);
        dependent = graftSorted(dependent, "dependencies", renderNode(child, keyIndent, unit), true);
        legacy = LockJson.replaceValue(legacy, nest.dependent, dependent);
        return LockJson.replaceValue(root, "dependencies", legacy);
    }

    private static final Set<String> LEGACY_MINIMAL_KEYS =
            new LinkedHashSet<>(Arrays.asList("version", "resolved", "integrity"));

    /** The captured old-version values for a v2 legacy nested entry, rendered after the bump moves its sibling. */
    private static final class LegacyNest {
        final String dependent;
        final String name;
        final String version;
        final String resolved;
        final String integrity;

        LegacyNest(String dependent, String name, String version, String resolved, String integrity) {
            this.dependent = dependent;
            this.name = name;
            this.version = version;
            this.resolved = resolved;
            this.integrity = integrity;
        }
    }

    /**
     * A brand-new top-level add is byte-exact only when its name is disjoint from every existing fork; a name that
     * already participates in a nested placement could make npm re-hoist or reshape that fork, so only such a name
     * collision fails loud.
     */
    private void requireAddNotEntangled(Json.JsonObject packages, String name) {
        for (Json member : packages.getMembers()) {
            if (!(member instanceof Json.Member)) {
                continue;
            }
            String key = LockJson.memberKey((Json.Member) member);
            if (key == null || !key.startsWith("node_modules/") ||
                    key.indexOf("node_modules/") == key.lastIndexOf("node_modules/")) {
                continue; // importer or flat placement
            }
            // A nested key node_modules/<a>/node_modules/<b>[/node_modules/<c>]: every embedded (possibly scoped)
            // name is entangled with the fork. Split on the separator so scoped names stay intact.
            for (String nestedName : key.substring("node_modules/".length()).split("/node_modules/")) {
                if (nestedName.equals(name)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                            "cannot add " + name + "; it participates in a nested placement (" + key + ")");
                }
            }
        }
    }

    private static String field(String fieldWs, String key, String valueSource) {
        return fieldWs + jsonEncode(key) + ": " + valueSource;
    }

    /**
     * Splice a new member (built from source text so its inner whitespace is exact) into {@code obj} at npm's sort
     * position, reusing a sibling's newline+indent prefix; a new last member inherits the previous last member's
     * pre-brace {@code after}.
     */
    private Json.JsonObject graftSorted(Json.JsonObject obj, String key, String valueSource, boolean valueIsObject) {
        String prefixWs = LockJson.memberWhitespace(obj);
        if (prefixWs == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "cannot insert into an object with no members");
        }
        Json.Member newMember = parseMember(prefixWs + jsonEncode(key) + ": " + valueSource);

        List<JsonRightPadded<Json>> members = new ArrayList<>(obj.getPadding().getMembers());
        int idx = sortedIndex(members, key, valueIsObject);
        JsonRightPadded<Json> newRp = new JsonRightPadded<>(newMember, Space.EMPTY, Markers.EMPTY);
        if (idx == members.size()) {
            int lastReal = lastMemberIndex(members);
            if (lastReal >= 0) {
                JsonRightPadded<Json> prevLast = members.get(lastReal);
                newRp = newRp.withAfter(prevLast.getAfter());
                members.set(lastReal, prevLast.withAfter(Space.EMPTY));
            }
            members.add(newRp);
        } else {
            members.add(idx, newRp);
        }
        return obj.getPadding().withMembers(members);
    }

    private int sortedIndex(List<JsonRightPadded<Json>> members, String newKey, boolean newIsObject) {
        int newGroup = newIsObject ? 1 : 0;
        for (int i = 0; i < members.size(); i++) {
            Json el = members.get(i).getElement();
            if (!(el instanceof Json.Member)) {
                continue;
            }
            Json.Member m = (Json.Member) el;
            String k = LockJson.memberKey(m);
            if (k == null) {
                continue;
            }
            int group = m.getValue() instanceof Json.JsonObject ? 1 : 0;
            if (group > newGroup || (group == newGroup && NpmKeyOrder.compareKeys(k, newKey) > 0)) {
                return i;
            }
        }
        return members.size();
    }

    private static int lastMemberIndex(List<JsonRightPadded<Json>> members) {
        for (int i = members.size() - 1; i >= 0; i--) {
            if (members.get(i).getElement() instanceof Json.Member) {
                return i;
            }
        }
        return -1;
    }

    /** The newline+indent prefix of {@code obj}'s first member, or {@code null} if it has none. */
    /** The newline+indent prefix one level deeper, read from a sibling's nested object member. */
    private static @Nullable String nestedMemberWhitespace(Json.JsonObject obj) {
        for (Json member : obj.getMembers()) {
            if (member instanceof Json.Member && ((Json.Member) member).getValue() instanceof Json.JsonObject) {
                String ws = LockJson.memberWhitespace((Json.JsonObject) ((Json.Member) member).getValue());
                if (ws != null) {
                    return ws;
                }
            }
        }
        return null;
    }

    /** Parse a single member from a throwaway wrapper so its internal whitespace round-trips byte-exact. */
    private static Json.Member parseMember(String memberSource) {
        Json.Document doc = LockJson.parse("{" + memberSource + "\n}", null);
        if (doc.getValue() instanceof Json.JsonObject) {
            for (Json member : ((Json.JsonObject) doc.getValue()).getMembers()) {
                if (member instanceof Json.Member) {
                    return (Json.Member) member;
                }
            }
        }
        throw new EngineFailure(Reason.MALFORMED_LOCK, null, "could not construct new lock member");
    }

    private Json.JsonObject applyMetadata(String name, Json.JsonObject packages, Json.JsonObject entry,
                                              @Nullable EntryMetadata wt) {
        if (wt == null) {
            return entry;
        }
        if (wt.isFlagsChanged()) {
            entry = writeFlag(entry, "dev", wt.getDev());
            entry = writeFlag(entry, "optional", wt.getOptional());
            entry = writeFlag(entry, "devOptional", wt.getDevOptional());
            entry = writeFlag(entry, "peer", wt.getPeer());
        }
        if (wt.getBin() != null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " bin metadata changed; native write-through is not supported");
        }
        if (wt.isEnginesChanged()) {
            entry = writeEngines(name, packages, entry, wt.getEngines());
        }
        if (wt.isFundingChanged()) {
            entry = writeObjectMember(name, packages, entry, "funding", wt.getFunding());
        }
        if (wt.isPeerDependenciesChanged()) {
            Map<String, String> peers = wt.getPeerDependencies();
            JsonNode value = (peers == null || peers.isEmpty()) ? null : JSON.valueToTree(peers);
            entry = writeObjectMember(name, packages, entry, "peerDependencies", value);
        }
        if (wt.isPeerDependenciesMetaChanged()) {
            entry = writeObjectMember(name, packages, entry, "peerDependenciesMeta", wt.getPeerDependenciesMeta());
        }
        if (wt.getLicense() != null) {
            if (LockJson.member(entry, "license") == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + " gained a license field");
            }
            entry = setStringField(entry, "license", wt.getLicense());
        }
        if (wt.getDeprecated() != null) {
            if (LockJson.member(entry, "deprecated") == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + " gained a deprecated field");
            }
            entry = setStringField(entry, "deprecated", wt.getDeprecated());
        }
        return entry;
    }

    /** Ensure a boolean flag member is present ({@code true}) or absent; untouched bytes when already right. */
    private Json.JsonObject writeFlag(Json.JsonObject entry, String key, @Nullable Boolean value) {
        boolean want = Boolean.TRUE.equals(value);
        if (want == (LockJson.member(entry, key) != null)) {
            return entry;
        }
        return want ? graftSorted(entry, key, "true", false) : removeMembers(entry, Collections.singleton(key));
    }

    /** Add, replace, or remove the entry's {@code engines} object at npm's field position (byte-exact). */
    private Json.JsonObject writeEngines(String name, Json.JsonObject packages, Json.JsonObject entry,
                                                @Nullable Map<String, String> engines) {
        JsonNode value = (engines == null || engines.isEmpty()) ? null : JSON.valueToTree(engines);
        return writeObjectMember(name, packages, entry, "engines", value);
    }

    /** Add, replace, or remove an object-valued entry member at npm's field position (byte-exact). */
    private Json.JsonObject writeObjectMember(String name, Json.JsonObject packages, Json.JsonObject entry,
                                                     String key, @Nullable JsonNode value) {
        entry = removeMembers(entry, Collections.singleton(key));
        if (value == null) {
            return entry; // member removed on upgrade
        }
        String fieldWs = nestedMemberWhitespace(packages);
        String closeWs = LockJson.memberWhitespace(packages);
        if (fieldWs == null || closeWs == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, name, "cannot derive entry indentation for " + key);
        }
        String keyIndent = indentOf(fieldWs);
        String unit = keyIndent.length() > indentOf(closeWs).length() ?
                keyIndent.substring(indentOf(closeWs).length()) : "  ";
        return graftSorted(entry, key, renderNode(value, keyIndent, unit), true);
    }

    private void requireRegistryEntry(String name, PackageEdit edit, Json.JsonObject entry) {
        Json.Member link = LockJson.member(entry, "link");
        if (link != null && "true".equals(literalSource(link.getValue()))) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, name, name + " is a workspace link entry");
        }
        if (LockJson.member(entry, "resolved") == null || LockJson.member(entry, "integrity") == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, name,
                    name + " has no resolved/integrity (not a registry entry)");
        }
        if (edit.getNewResolved() == null || edit.getNewIntegrity() == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, name,
                    name + " has no registry locator for " + edit.getNewVersion());
        }
    }

    // --- removals + orphan GC --------------------------------------------

    private Json.JsonObject applyRemovals(Json.JsonObject root, int lockfileVersion, List<PackageEdit> removals) {
        Json.JsonObject packages = requirePackages(root);

        // GC-by-name is only sound for a flat, hoisted tree; reject nested/forked placements.
        for (Json member : packages.getMembers()) {
            if (member instanceof Json.Member) {
                String key = LockJson.memberKey((Json.Member) member);
                if (key != null && key.indexOf("node_modules/") != key.lastIndexOf("node_modules/")) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                            "cannot remove from a lock with nested node_modules placements: " + key);
                }
            }
        }

        Set<String> removedNames = new LinkedHashSet<>();
        for (PackageEdit removal : removals) {
            removedNames.add(removal.getName());
            String importerKey = removal.getImporterDir() == null ? "" : removal.getImporterDir();
            Json.JsonObject importer = LockJson.objectMember(packages, importerKey);
            if (importer != null) {
                Json.JsonObject scope = LockJson.objectMember(importer, removal.getScope());
                if (scope != null && LockJson.member(scope, removal.getName()) != null) {
                    Json.JsonObject trimmed = removeMembers(scope, Collections.singleton(removal.getName()));
                    if (isEmptyObject(trimmed)) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, removal.getName(),
                                "removing " + removal.getName() + " empties the " + removal.getScope() + " scope");
                    }
                    importer = LockJson.replaceValue(importer, removal.getScope(), trimmed);
                    packages = LockJson.replaceValue(packages, importerKey, importer);
                }
            }
        }

        Set<String> removedKeys = new LinkedHashSet<>();
        for (String name : removedNames) {
            removedKeys.add("node_modules/" + name);
        }
        packages = removeMembers(packages, removedKeys);

        // Drop transitive entries orphaned by the removal.
        Set<String> reachable = reachableNames(packages);
        Set<String> orphanKeys = new LinkedHashSet<>();
        for (Json member : packages.getMembers()) {
            if (!(member instanceof Json.Member)) {
                continue;
            }
            String key = LockJson.memberKey((Json.Member) member);
            if (key == null || !key.startsWith("node_modules/")) {
                continue; // importer entry
            }
            Json.JsonObject entry = (Json.JsonObject) ((Json.Member) member).getValue();
            if (isLink(entry)) {
                continue; // workspace symlink
            }
            if (!reachable.contains(key.substring("node_modules/".length()))) {
                orphanKeys.add(key);
            }
        }
        packages = removeMembers(packages, orphanKeys);
        root = LockJson.replaceValue(root, "packages", packages);

        if (lockfileVersion == 2) {
            Json.JsonObject legacy = LockJson.objectMember(root, "dependencies");
            if (legacy != null) {
                Set<String> legacyDrop = new LinkedHashSet<>(removedNames);
                for (String key : orphanKeys) {
                    legacyDrop.add(key.substring("node_modules/".length()));
                }
                root = LockJson.replaceValue(root, "dependencies", removeMembers(legacy, legacyDrop));
            }
        }
        return root;
    }

    private Set<String> reachableNames(Json.JsonObject packages) {
        Set<String> reachable = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        for (Json member : packages.getMembers()) {
            if (!(member instanceof Json.Member)) {
                continue;
            }
            String key = LockJson.memberKey((Json.Member) member);
            if (key == null || key.contains("node_modules/")) {
                continue; // only importer entries seed the roots
            }
            Json.JsonObject importer = (Json.JsonObject) ((Json.Member) member).getValue();
            for (String scope : IMPORTER_SCOPES) {
                enqueueNames(LockJson.objectMember(importer, scope), reachable, queue);
            }
        }
        while (!queue.isEmpty()) {
            String name = queue.poll();
            Json.JsonObject entry = LockJson.objectMember(packages, "node_modules/" + name);
            if (entry == null) {
                continue;
            }
            enqueueNames(LockJson.objectMember(entry, "dependencies"), reachable, queue);
            enqueueNames(LockJson.objectMember(entry, "optionalDependencies"), reachable, queue);
            enqueueNames(LockJson.objectMember(entry, "peerDependencies"), reachable, queue);
        }
        return reachable;
    }

    private void enqueueNames(Json.@Nullable JsonObject scope, Set<String> reachable, Deque<String> queue) {
        if (scope == null) {
            return;
        }
        for (Json member : scope.getMembers()) {
            if (member instanceof Json.Member) {
                String name = LockJson.memberKey((Json.Member) member);
                if (name != null && reachable.add(name)) {
                    queue.add(name);
                }
            }
        }
    }

    // --- orphan GC after an edge-dropping bump ---------------------------

    /**
     * Drop every installed {@code packages} entry the bump left unreachable, reachability being npm's own hoisting
     * resolution ({@link #resolveFrom}) so a transitive still required elsewhere survives. A pruned name that also
     * lives at another placement could re-hoist on removal, so it fails loud.
     */
    private Json.JsonObject gcOrphansAfterBump(Json.JsonObject root, int lockfileVersion) {
        Json.JsonObject packages = requirePackages(root);
        Set<String> reachable = reachableInstalledKeys(packages);

        Map<String, Integer> placements = new LinkedHashMap<>();
        for (Json member : packages.getMembers()) {
            if (member instanceof Json.Member) {
                String key = LockJson.memberKey((Json.Member) member);
                if (key != null && key.contains("node_modules/")) {
                    placements.merge(installedName(key), 1, Integer::sum);
                }
            }
        }

        Set<String> orphanKeys = new LinkedHashSet<>();
        Set<String> orphanNames = new LinkedHashSet<>();
        for (Json member : packages.getMembers()) {
            if (!(member instanceof Json.Member)) {
                continue;
            }
            String key = LockJson.memberKey((Json.Member) member);
            if (key == null || !key.contains("node_modules/")) {
                continue; // importer entry
            }
            Json.JsonObject entry = (Json.JsonObject) ((Json.Member) member).getValue();
            if (isLink(entry) || reachable.contains(key)) {
                continue;
            }
            String name = installedName(key);
            if (placements.getOrDefault(name, 0) > 1) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        "orphan-pruning " + name + " with a duplicate placement may re-hoist; deferred");
            }
            orphanKeys.add(key);
            orphanNames.add(name);
        }
        if (orphanKeys.isEmpty()) {
            return root;
        }
        packages = removeMembers(packages, orphanKeys);
        root = LockJson.replaceValue(root, "packages", packages);

        if (lockfileVersion == 2) {
            Json.JsonObject legacy = LockJson.objectMember(root, "dependencies");
            if (legacy != null) {
                for (String name : orphanNames) {
                    if (hasNestedOccurrence(legacy, name)) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                                "orphan-pruning " + name + " from a nested v2 legacy tree is not yet supported");
                    }
                }
                root = LockJson.replaceValue(root, "dependencies", removeMembers(legacy, orphanNames));
            }
        }
        return root;
    }

    /** The bare package name of an installed {@code node_modules/...} key (its last path segment). */
    private static String installedName(String key) {
        return key.substring(key.lastIndexOf("node_modules/") + "node_modules/".length());
    }

    /** Installed entry keys reachable from an importer root via npm's hoisting resolution. */
    private Set<String> reachableInstalledKeys(Json.JsonObject packages) {
        Set<String> keys = new LinkedHashSet<>();
        for (Json member : packages.getMembers()) {
            if (member instanceof Json.Member) {
                String key = LockJson.memberKey((Json.Member) member);
                if (key != null) {
                    keys.add(key);
                }
            }
        }
        Set<String> reachable = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        for (Json member : packages.getMembers()) {
            if (!(member instanceof Json.Member)) {
                continue;
            }
            String key = LockJson.memberKey((Json.Member) member);
            if (key == null || key.contains("node_modules/")) {
                continue; // only importer roots seed the walk
            }
            Json.JsonObject importer = (Json.JsonObject) ((Json.Member) member).getValue();
            for (String scope : IMPORTER_SCOPES) {
                enqueueResolved(keys, key, LockJson.objectMember(importer, scope), reachable, queue);
            }
        }
        while (!queue.isEmpty()) {
            String key = queue.poll();
            Json.JsonObject entry = LockJson.objectMember(packages, key);
            if (entry == null) {
                continue;
            }
            enqueueResolved(keys, key, LockJson.objectMember(entry, "dependencies"), reachable, queue);
            enqueueResolved(keys, key, LockJson.objectMember(entry, "optionalDependencies"), reachable, queue);
            enqueueResolved(keys, key, LockJson.objectMember(entry, "peerDependencies"), reachable, queue);
        }
        return reachable;
    }

    private void enqueueResolved(Set<String> keys, String fromKey, Json.@Nullable JsonObject scope,
                                 Set<String> reachable, Deque<String> queue) {
        if (scope == null) {
            return;
        }
        for (Json member : scope.getMembers()) {
            if (!(member instanceof Json.Member)) {
                continue;
            }
            String name = LockJson.memberKey((Json.Member) member);
            if (name == null) {
                continue;
            }
            String resolved = resolveFrom(keys, fromKey, name);
            if (resolved != null && reachable.add(resolved)) {
                queue.add(resolved);
            }
        }
    }

    /**
     * npm's hoisting resolution: from the package at {@code fromKey}, resolve {@code name} by checking its own
     * {@code node_modules} then walking up each ancestor's, returning the matching entry key (or {@code null} when
     * unresolved, e.g. an optional/peer edge left unplaced).
     */
    static @Nullable String resolveFrom(Set<String> keys, String fromKey, String name) {
        String prefix = fromKey.isEmpty() ? "" : fromKey + "/";
        while (true) {
            String candidate = prefix + "node_modules/" + name;
            if (keys.contains(candidate)) {
                return candidate;
            }
            if (prefix.isEmpty()) {
                return null;
            }
            String trimmed = prefix.substring(0, prefix.length() - 1);
            int nm = trimmed.lastIndexOf("node_modules/");
            prefix = nm < 0 ? "" : trimmed.substring(0, nm);
        }
    }

    // --- LST navigation + mutation ---------------------------------------

    private Json.JsonObject requirePackages(Json.JsonObject root) {
        Json.JsonObject packages = LockJson.objectMember(root, "packages");
        if (packages == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "package-lock.json has no packages map");
        }
        return packages;
    }

    private @Nullable JsonNode parseManifest(@Nullable String manifest) {
        if (manifest == null) {
            return null;
        }
        try {
            return JSON.readTree(manifest);
        } catch (Exception e) {
            return null;
        }
    }

    private static @Nullable String lookupConstraint(@Nullable JsonNode manifest, String scope, String name) {
        if (manifest == null) {
            return null;
        }
        JsonNode scopeNode = manifest.get(scope);
        if (scopeNode == null) {
            return null;
        }
        JsonNode value = scopeNode.get(name);
        return value == null ? null : value.asText();
    }

    private int lockfileVersion(Json.JsonObject root) {
        Json.Member member = LockJson.member(root, "lockfileVersion");
        if (member == null || !(member.getValue() instanceof Json.Literal)) {
            throw new EngineFailure(Reason.UNSUPPORTED_LOCKFILE_VERSION, null,
                    "package-lock.json has no lockfileVersion");
        }
        String source = ((Json.Literal) member.getValue()).getSource().trim();
        try {
            return Integer.parseInt(source);
        } catch (NumberFormatException e) {
            throw new EngineFailure(Reason.UNSUPPORTED_LOCKFILE_VERSION, null,
                    "unrecognised lockfileVersion: " + source);
        }
    }

    private static boolean isLink(Json.JsonObject entry) {
        Json.Member link = LockJson.member(entry, "link");
        return link != null && "true".equals(literalSource(link.getValue()));
    }

    private static boolean isEmptyObject(Json.JsonObject obj) {
        for (Json member : obj.getMembers()) {
            if (member instanceof Json.Member) {
                return false;
            }
        }
        return true;
    }

    /** Replace the string value of member {@code field}, preserving the old value's prefix (byte-exact whitespace). */
    private static Json.JsonObject setStringField(Json.JsonObject obj, String field, String value) {
        Json.Member member = LockJson.member(obj, field);
        if (member == null) {
            return obj;
        }
        Space prefix = member.getValue().getPrefix();
        Json.Literal literal = new Json.Literal(Tree.randomId(), prefix, Markers.EMPTY, jsonEncode(value), value);
        return LockJson.replaceValue(obj, field, literal);
    }

    /** Jackson-escaped quoted JSON string literal — registry {@code license}/{@code deprecated} may contain {@code "}/{@code \}/newlines. */
    private static String jsonEncode(String value) {
        return NpmJson.jsonEncode(value);
    }

    /** Replace the value of member {@code key} in place, keeping its position and surrounding padding. */
    /** Drop the named members, moving the closing-brace whitespace onto the new last member (byte-exact). */
    private static Json.JsonObject removeMembers(Json.JsonObject obj, Set<String> keys) {
        List<JsonRightPadded<Json>> original = obj.getPadding().getMembers();
        List<JsonRightPadded<Json>> kept = new ArrayList<>();
        boolean removed = false;
        for (JsonRightPadded<Json> rp : original) {
            Json element = rp.getElement();
            if (element instanceof Json.Member && keys.contains(LockJson.memberKey((Json.Member) element))) {
                removed = true;
                continue;
            }
            kept.add(rp);
        }
        if (!removed) {
            return obj;
        }
        if (!kept.isEmpty()) {
            JsonRightPadded<Json> originalLast = original.get(original.size() - 1);
            int last = kept.size() - 1;
            kept.set(last, kept.get(last).withAfter(originalLast.getAfter()));
        }
        return obj.getPadding().withMembers(kept);
    }

    private static boolean hasNestedOccurrence(Json.JsonObject depsTree, String name) {
        for (Json member : depsTree.getMembers()) {
            if (!(member instanceof Json.Member) || !(((Json.Member) member).getValue() instanceof Json.JsonObject)) {
                continue;
            }
            Json.JsonObject nested = LockJson.objectMember((Json.JsonObject) ((Json.Member) member).getValue(), "dependencies");
            if (nested != null && (LockJson.member(nested, name) != null || hasNestedOccurrence(nested, name))) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable String stringField(Json.JsonObject obj, String key) {
        Json.Member member = LockJson.member(obj, key);
        return member == null ? null : LockJson.literal(member.getValue());
    }

    private static @Nullable String literalSource(@Nullable JsonValue value) {
        return value instanceof Json.Literal ? ((Json.Literal) value).getSource() : null;
    }
}
