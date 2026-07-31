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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;
import org.openrewrite.javascript.internal.lock.LockEditSet.WriteThroughMetadata;
import org.openrewrite.json.JsonParser;
import org.openrewrite.json.internal.JsonPrinter;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonKey;
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

/**
 * Byte-exact {@link LockPatcher} for npm {@code package-lock.json} (lockfileVersion 2 and 3, full
 * workspace support). It parses the captured lock with the byte-lossless rewrite-json LST and rewrites
 * only the entries the {@link LockEditSet} names, preserving every other byte.
 * <p>
 * Per {@link PackageEdit} the edit sites are:
 * <ol>
 *   <li>{@code packages["node_modules/<name>"]} — {@code version}/{@code resolved}/{@code integrity}
 *       (plus write-through {@code license}/{@code deprecated}) on a resolved bump;</li>
 *   <li>the importer's declared constraint at {@code packages["<importerDir or ''>"].<scope>["<name>"]},
 *       taken verbatim from the edited {@code package.json};</li>
 *   <li>for lockfileVersion 2 only, the legacy {@code dependencies["<name>"]} tree entry
 *       ({@code version}/{@code resolved}/{@code integrity}; {@code requires} is closure-unchanged).</li>
 * </ol>
 * A removal drops the {@code node_modules} entry, its importer scope member, the v2 legacy entry, and
 * any transitive entry that becomes orphaned. Anything the format cannot express byte-exactly fails loud.
 */
public final class NpmLockPatcher implements LockPatcher {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final List<String> IMPORTER_SCOPES = Arrays.asList(
            "dependencies", "devDependencies", "peerDependencies", "optionalDependencies");

    @Override
    public String patch(LockEditSet edits) {
        Json.Document doc = parse(edits.getExistingLockContent());
        if (!(doc.getValue() instanceof Json.JsonObject)) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "package-lock.json root is not an object");
        }
        Json.JsonObject root = (Json.JsonObject) doc.getValue();

        int lockfileVersion = lockfileVersion(root);
        if (lockfileVersion != 2 && lockfileVersion != 3) {
            throw new EngineFailure(Reason.UNSUPPORTED_LOCKFILE_VERSION, null,
                    "package-lock.json lockfileVersion " + lockfileVersion + " is not supported (need 2 or 3)");
        }
        if (getObjectMember(root, "packages") == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_LOCKFILE_VERSION, null,
                    "package-lock.json has no packages map (v1 is not supported)");
        }

        JsonNode editedManifest = parseManifest(edits.getEditedPackageJson());

        // A relocate-nest (an upgrade that pushes the old version down) copies a pre-edit entry, so it runs
        // before the bumps mutate it. The v2 legacy nested tree is captured now but inserted after the bumps,
        // so the bump's own fork guard (applyLegacyTree) still sees a flat legacy tree.
        List<LegacyNest> legacyNests = new ArrayList<>();
        for (PackageEdit edit : edits.getEdits()) {
            if (edit.getNestedUnder() != null && !edit.isAdded()) {
                if (lockfileVersion == 2) {
                    legacyNests.add(captureLegacyNest(root, edit));
                }
                root = relocatePackagesEntry(root, edit);
            }
        }

        List<PackageEdit> removals = new ArrayList<>();
        for (PackageEdit edit : edits.getEdits()) {
            if (edit.getNestedUnder() != null) {
                continue; // both nest kinds run in their own pass
            }
            if (edit.isPromoted()) {
                root = applyPromotion(root, lockfileVersion, editedManifest, edit);
                continue;
            }
            if (edit.getNewVersion() == null) {
                removals.add(edit);
                continue;
            }
            if (edit.isAdded()) {
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
            if (edit.getNestedUnder() != null && edit.isAdded()) {
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

        // Site (1): the hoisted package placement, only when the resolved version actually moves.
        if (placementMoves) {
            Json.JsonObject packages = requirePackages(root);
            Json.JsonObject entry = getObjectMember(packages, "node_modules/" + name);
            if (entry == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        "no packages entry for node_modules/" + name);
            }
            requireRegistryEntry(name, edit, entry);
            entry = setStringField(entry, "version", edit.getNewVersion());
            entry = setStringField(entry, "resolved", edit.getNewResolved());
            entry = setStringField(entry, "integrity", edit.getNewIntegrity());
            entry = applyWriteThrough(name, packages, entry, edit.getWriteThroughMetadata());
            // A cascade bump changes the entry's own dependency edge constraints (unchanged edges are left
            // byte-identical). An added edge reshapes and fails loud; a dropped edge orphan-prunes when the
            // edit allows it (the GC pass below drops whatever it leaves unreachable).
            entry = reconcileConstraintMap(name, entry, "dependencies", edit.getNewDependencies(), edit.isPrunesOrphans());
            packages = putMember(packages, "node_modules/" + name, entry);
            root = putMember(root, "packages", packages);
        }

        // Site (2): the importer's declared constraint mirror.
        root = applyImporterConstraint(root, editedManifest, edit);

        // Site (3): the v2 legacy dependencies tree second writer.
        if (lockfileVersion == 2 && placementMoves) {
            root = applyLegacyTree(root, edit);
        }
        return root;
    }

    private Json.JsonObject applyImporterConstraint(Json.JsonObject root, @Nullable JsonNode editedManifest,
                                                    PackageEdit edit) {
        String newConstraint = lookupConstraint(editedManifest, edit.getScope(), edit.getName());
        if (newConstraint == null) {
            return root;
        }
        String importerKey = edit.getImporterDir() == null ? "" : edit.getImporterDir();
        Json.JsonObject packages = requirePackages(root);
        Json.JsonObject importer = getObjectMember(packages, importerKey);
        if (importer == null) {
            return root;
        }
        Json.JsonObject scope = getObjectMember(importer, edit.getScope());
        if (scope == null || getMember(scope, edit.getName()) == null) {
            return root;
        }
        scope = setStringField(scope, edit.getName(), newConstraint);
        importer = putMember(importer, edit.getScope(), scope);
        packages = putMember(packages, importerKey, importer);
        return putMember(root, "packages", packages);
    }

    private Json.JsonObject applyLegacyTree(Json.JsonObject root, PackageEdit edit) {
        String name = edit.getName();
        Json.JsonObject legacy = getObjectMember(root, "dependencies");
        if (legacy == null) {
            return root;
        }
        if (hasNestedOccurrence(legacy, name)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " appears nested in the v2 legacy dependencies tree (fork/dedupe)");
        }
        Json.JsonObject entry = getObjectMember(legacy, name);
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
        entry = setStringField(entry, "version", edit.getNewVersion());
        entry = setStringField(entry, "resolved", edit.getNewResolved());
        entry = setStringField(entry, "integrity", edit.getNewIntegrity());
        // The v2 legacy tree mirrors a dependent's edges under `requires`; a cascade re-pins them, an
        // orphan-prune drops them.
        entry = reconcileConstraintMap(name, entry, "requires", edit.getNewDependencies(), edit.isPrunesOrphans());
        legacy = putMember(legacy, name, entry);
        return putMember(root, "dependencies", legacy);
    }

    /**
     * Re-pin the constraint values of an entry's {@code dependencies}/{@code requires} map to a moved
     * version's edges. Only changed values are rewritten (unchanged edges stay byte-identical); an edge
     * added or removed by the upgrade reshapes the tree and fails loud (deferred).
     */
    private Json.JsonObject reconcileConstraintMap(String name, Json.JsonObject entry, String mapKey,
                                                   @Nullable Map<String, String> newDeps, boolean allowDrops) {
        Map<String, String> deps = newDeps == null ? Collections.emptyMap() : newDeps;
        if (deps.isEmpty() && !allowDrops) {
            return entry; // no constraint reconciliation requested (Phase A bump / leaf mover)
        }
        Json.JsonObject map = getObjectMember(entry, mapKey);
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
        return changed || allowDrops ? putMember(entry, mapKey, map) : entry;
    }

    private static Set<String> memberKeys(Json.JsonObject obj) {
        Set<String> keys = new LinkedHashSet<>();
        for (Json member : obj.getMembers()) {
            if (member instanceof Json.Member) {
                String key = memberKey((Json.Member) member);
                if (key != null) {
                    keys.add(key);
                }
            }
        }
        return keys;
    }

    // --- leaf add (Phase B increment 1) ----------------------------------

    /**
     * Insert a brand-new leaf (its object/array metadata already vetted by the engine): a {@code packages}
     * entry, the importer's declared constraint (creating the scope object when absent), and — for
     * lockfileVersion 2 — the minimal legacy tree entry (still {@code version}/{@code resolved}/{@code
     * integrity} only). Each insert lands at npm's own sort position ({@link NpmKeyOrder}) with byte-exact
     * whitespace.
     */
    private Json.JsonObject applyAdd(Json.JsonObject root, int lockfileVersion,
                                     @Nullable JsonNode editedManifest, PackageEdit edit) {
        String name = edit.getName();
        Json.JsonObject packages = requirePackages(root);
        requireFlatPlacement(packages);

        String entryKey = "node_modules/" + name;
        if (getMember(packages, entryKey) != null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + " is already placed in node_modules");
        }
        if (edit.getNewResolved() == null || edit.getNewIntegrity() == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, name, name + " has no registry locator");
        }

        // Site (1): the hoisted package entry.
        String entryText = leafEntryText(packages, edit);
        packages = graftSorted(packages, entryKey, entryText, true);
        root = putMember(root, "packages", packages);

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
     * stays, so only the importer edge is written (creating the scope object when absent). A dev→prod promotion
     * additionally clears {@code "dev": true} on the leaf entry; the v2 legacy tree marks dev in two places, so
     * clearing it there is not yet verified and fails loud.
     */
    private Json.JsonObject applyPromotion(Json.JsonObject root, int lockfileVersion,
                                           @Nullable JsonNode editedManifest, PackageEdit edit) {
        root = insertImporterConstraint(root, editedManifest, edit);
        if (edit.isClearDev()) {
            if (lockfileVersion == 2) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, edit.getName(),
                        "clearing dev on a lockfileVersion 2 promotion (legacy tree) is not yet supported");
            }
            Json.JsonObject packages = requirePackages(root);
            String entryKey = "node_modules/" + edit.getName();
            Json.JsonObject entry = getObjectMember(packages, entryKey);
            if (entry != null) {
                entry = removeMembers(entry, Collections.singleton("dev"));
                packages = putMember(packages, entryKey, entry);
                root = putMember(root, "packages", packages);
            }
        }
        return root;
    }

    /**
     * The leaf entry object source, members in npm's serialization order: object-valued members after
     * scalar/array-valued ones, {@code swKeyOrder} keys first within a group, then ICU {@code localeCompare}.
     * Object/array metadata values are pretty-printed at npm's indentation ({@link #renderNode}).
     */
    private String leafEntryText(Json.JsonObject packages, PackageEdit edit) {
        String fieldWs = nestedMemberWhitespace(packages);
        String closeWs = memberWhitespace(packages);
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
        if ("devDependencies".equals(edit.getScope())) {
            fields.add(new EntryField("dev", "true", false));
        }
        WriteThroughMetadata wt = edit.getWriteThroughMetadata();
        if (wt != null) {
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
        if (node.isObject()) {
            if (node.size() == 0) {
                return "{}";
            }
            String inner = indent + unit;
            List<String> keys = new ArrayList<>();
            node.fieldNames().forEachRemaining(keys::add);
            keys.sort((a, b) -> {
                boolean ao = node.get(a).isObject();
                boolean bo = node.get(b).isObject();
                return ao != bo ? (ao ? 1 : -1) : NpmKeyOrder.compareKeys(a, b);
            });
            List<String> members = new ArrayList<>();
            for (String k : keys) {
                members.add("\n" + inner + jsonEncode(k) + ": " + renderNode(node.get(k), inner, unit));
            }
            return "{" + String.join(",", members) + "\n" + indent + "}";
        }
        if (node.isArray()) {
            if (node.size() == 0) {
                return "[]";
            }
            String inner = indent + unit;
            List<String> elements = new ArrayList<>();
            for (JsonNode el : node) {
                elements.add("\n" + inner + renderNode(el, inner, unit));
            }
            return "[" + String.join(",", elements) + "\n" + indent + "]";
        }
        return scalarSource(node);
    }

    /** The indentation (no newline) of an object member's prefix whitespace. */
    private static String indentOf(String ws) {
        int nl = ws.lastIndexOf('\n');
        return nl < 0 ? ws : ws.substring(nl + 1);
    }

    private static String scalarSource(JsonNode node) {
        try {
            return JSON.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "could not JSON-encode value: " + node);
        }
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
        Json.JsonObject importer = getObjectMember(packages, importerKey);
        if (importer == null) {
            return root;
        }
        Json.JsonObject scope = getObjectMember(importer, edit.getScope());
        if (scope != null) {
            // Existing scope: insert the single scalar constraint, sorted.
            scope = graftSorted(scope, edit.getName(), jsonEncode(newConstraint), false);
            importer = putMember(importer, edit.getScope(), scope);
        } else {
            // First dependency of this scope: create the whole scope object with one member.
            String fieldWs = nestedMemberWhitespace(importer);
            String closeWs = memberWhitespace(importer);
            if (fieldWs == null) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, edit.getName(), "cannot derive importer indentation");
            }
            String scopeText = "{" + field(fieldWs, edit.getName(), jsonEncode(newConstraint)) + closeWs + "}";
            importer = graftSorted(importer, edit.getScope(), scopeText, true);
        }
        packages = putMember(packages, importerKey, importer);
        return putMember(root, "packages", packages);
    }

    private Json.JsonObject insertLegacyEntry(Json.JsonObject root, PackageEdit edit) {
        Json.JsonObject legacy = getObjectMember(root, "dependencies");
        if (legacy == null) {
            return root;
        }
        // The v2 legacy tree marks dev entries with "dev": true; its byte-exact form for a dev closure
        // add is not yet verified, so defer rather than emit a maybe-wrong entry.
        if ("devDependencies".equals(edit.getScope())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, edit.getName(),
                    "adding a devDependency closure to a lockfileVersion 2 lock is not yet supported");
        }
        String fieldWs = nestedMemberWhitespace(legacy);
        String closeWs = memberWhitespace(legacy);
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
        return putMember(root, "dependencies", legacy);
    }

    // --- reverse-dependent nest (Phase B I5) -----------------------------

    /**
     * Relocate the pre-edit {@code node_modules/<name>} entry to {@code node_modules/<dependent>/node_modules/
     * <name>} byte-for-byte — npm nests the old version there when the top-level slot goes to the new one. The
     * value sits at the same depth, so its bytes are reused verbatim; only the key and sort position change.
     */
    private Json.JsonObject relocatePackagesEntry(Json.JsonObject root, PackageEdit edit) {
        String name = edit.getName();
        Json.JsonObject packages = requirePackages(root);
        Json.Member top = getMember(packages, "node_modules/" + name);
        if (top == null || !(top.getValue() instanceof Json.JsonObject)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, "no node_modules/" + name + " entry to nest");
        }
        String nestedKey = "node_modules/" + edit.getNestedUnder() + "/node_modules/" + name;
        if (getMember(packages, nestedKey) != null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, nestedKey + " already present (fork exists)");
        }
        packages = graftSorted(packages, nestedKey, objectSource(top.getValue()), true);
        return putMember(root, "packages", packages);
    }

    /**
     * Insert a brand-new nested leaf at {@code node_modules/<dependent>/node_modules/<name>} (I5 add-nest):
     * a closure member an incompatible top-level pin excludes. Serialized like a top-level leaf add
     * ({@link #leafEntryText}) but at the nested key and with no importer edge. v2's deeper legacy nesting is
     * not yet verified, so it defers.
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
        if (getMember(packages, nestedKey) != null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, nestedKey + " already present (fork exists)");
        }
        packages = graftSorted(packages, nestedKey, leafEntryText(packages, edit), true);
        return putMember(root, "packages", packages);
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
     * Insert the v2 legacy nested entry {@code dependencies.<dependent>.dependencies.<name>} (minimal
     * {@code version}/{@code resolved}/{@code integrity}) at its deeper indentation. Captured from the pre-bump
     * top-level legacy entry so it holds the old version even though it lands after the bump has moved it.
     */
    private LegacyNest captureLegacyNest(Json.JsonObject root, PackageEdit edit) {
        Json.JsonObject legacy = getObjectMember(root, "dependencies");
        Json.JsonObject entry = legacy == null ? null : getObjectMember(legacy, edit.getName());
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
        Json.JsonObject legacy = getObjectMember(root, "dependencies");
        Json.JsonObject dependent = legacy == null ? null : getObjectMember(legacy, nest.dependent);
        if (dependent == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, nest.name,
                    "no legacy entry for " + nest.dependent + " to nest under");
        }
        if (getObjectMember(dependent, "dependencies") != null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, nest.name,
                    nest.dependent + " already has a nested legacy dependencies tree; merge not yet supported");
        }
        String fieldWs = nestedMemberWhitespace(legacy);
        String closeWs = memberWhitespace(legacy);
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
        legacy = putMember(legacy, nest.dependent, dependent);
        return putMember(root, "dependencies", legacy);
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

    /** Reject a lock with any nested {@code node_modules} placement; a flat tree is required to insert soundly. */
    private void requireFlatPlacement(Json.JsonObject packages) {
        for (Json member : packages.getMembers()) {
            if (member instanceof Json.Member) {
                String key = memberKey((Json.Member) member);
                if (key != null && key.indexOf("node_modules/") != key.lastIndexOf("node_modules/")) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                            "cannot add into a lock with nested node_modules placements: " + key);
                }
            }
        }
    }

    private static String field(String fieldWs, String key, String valueSource) {
        return fieldWs + jsonEncode(key) + ": " + valueSource;
    }

    /**
     * Splice a new member (built from source text so its inner whitespace is exact) into {@code obj} at
     * npm's sort position. The new member reuses a sibling's newline+indent prefix; when it becomes the
     * new last member it inherits the previous last member's pre-brace {@code after}.
     */
    private Json.JsonObject graftSorted(Json.JsonObject obj, String key, String valueSource, boolean valueIsObject) {
        String prefixWs = memberWhitespace(obj);
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
            String k = memberKey(m);
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
    private static @Nullable String memberWhitespace(Json.JsonObject obj) {
        for (Json member : obj.getMembers()) {
            if (member instanceof Json.Member) {
                return ((Json.Member) member).getPrefix().getWhitespace();
            }
        }
        return null;
    }

    /** The newline+indent prefix one level deeper, read from a sibling's nested object member. */
    private static @Nullable String nestedMemberWhitespace(Json.JsonObject obj) {
        for (Json member : obj.getMembers()) {
            if (member instanceof Json.Member && ((Json.Member) member).getValue() instanceof Json.JsonObject) {
                String ws = memberWhitespace((Json.JsonObject) ((Json.Member) member).getValue());
                if (ws != null) {
                    return ws;
                }
            }
        }
        return null;
    }

    /** Parse a single member from a throwaway wrapper so its internal whitespace round-trips byte-exact. */
    private static Json.Member parseMember(String memberSource) {
        Json.Document doc = parse("{" + memberSource + "\n}");
        if (doc.getValue() instanceof Json.JsonObject) {
            for (Json member : ((Json.JsonObject) doc.getValue()).getMembers()) {
                if (member instanceof Json.Member) {
                    return (Json.Member) member;
                }
            }
        }
        throw new EngineFailure(Reason.MALFORMED_LOCK, null, "could not construct new lock member");
    }

    private Json.JsonObject applyWriteThrough(String name, Json.JsonObject packages, Json.JsonObject entry,
                                              @Nullable WriteThroughMetadata wt) {
        if (wt == null) {
            return entry;
        }
        if (wt.getBin() != null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " bin metadata changed; native write-through is not supported");
        }
        if (wt.isEnginesChanged()) {
            entry = writeThroughEngines(name, packages, entry, wt.getEngines());
        }
        if (wt.isFundingChanged()) {
            entry = writeThroughObjectMember(name, packages, entry, "funding", wt.getFunding());
        }
        if (wt.getLicense() != null) {
            if (getMember(entry, "license") == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + " gained a license field");
            }
            entry = setStringField(entry, "license", wt.getLicense());
        }
        if (wt.getDeprecated() != null) {
            if (getMember(entry, "deprecated") == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, name + " gained a deprecated field");
            }
            entry = setStringField(entry, "deprecated", wt.getDeprecated());
        }
        return entry;
    }

    /** Add, replace, or remove the entry's {@code engines} object at npm's field position (byte-exact). */
    private Json.JsonObject writeThroughEngines(String name, Json.JsonObject packages, Json.JsonObject entry,
                                                @Nullable Map<String, String> engines) {
        JsonNode value = (engines == null || engines.isEmpty()) ? null : JSON.valueToTree(engines);
        return writeThroughObjectMember(name, packages, entry, "engines", value);
    }

    /** Add, replace, or remove an object-valued entry member at npm's field position (byte-exact). */
    private Json.JsonObject writeThroughObjectMember(String name, Json.JsonObject packages, Json.JsonObject entry,
                                                     String key, @Nullable JsonNode value) {
        entry = removeMembers(entry, Collections.singleton(key));
        if (value == null) {
            return entry; // member removed on upgrade
        }
        String fieldWs = nestedMemberWhitespace(packages);
        String closeWs = memberWhitespace(packages);
        if (fieldWs == null || closeWs == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, name, "cannot derive entry indentation for " + key);
        }
        String keyIndent = indentOf(fieldWs);
        String unit = keyIndent.length() > indentOf(closeWs).length() ?
                keyIndent.substring(indentOf(closeWs).length()) : "  ";
        return graftSorted(entry, key, renderNode(value, keyIndent, unit), true);
    }

    private void requireRegistryEntry(String name, PackageEdit edit, Json.JsonObject entry) {
        Json.Member link = getMember(entry, "link");
        if (link != null && "true".equals(literalSource(link.getValue()))) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, name, name + " is a workspace link entry");
        }
        if (getMember(entry, "resolved") == null || getMember(entry, "integrity") == null) {
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
                String key = memberKey((Json.Member) member);
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
            Json.JsonObject importer = getObjectMember(packages, importerKey);
            if (importer != null) {
                Json.JsonObject scope = getObjectMember(importer, removal.getScope());
                if (scope != null && getMember(scope, removal.getName()) != null) {
                    Json.JsonObject trimmed = removeMembers(scope, Collections.singleton(removal.getName()));
                    if (isEmptyObject(trimmed)) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, removal.getName(),
                                "removing " + removal.getName() + " empties the " + removal.getScope() + " scope");
                    }
                    importer = putMember(importer, removal.getScope(), trimmed);
                    packages = putMember(packages, importerKey, importer);
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
            String key = memberKey((Json.Member) member);
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
        root = putMember(root, "packages", packages);

        if (lockfileVersion == 2) {
            Json.JsonObject legacy = getObjectMember(root, "dependencies");
            if (legacy != null) {
                Set<String> legacyDrop = new LinkedHashSet<>(removedNames);
                for (String key : orphanKeys) {
                    legacyDrop.add(key.substring("node_modules/".length()));
                }
                root = putMember(root, "dependencies", removeMembers(legacy, legacyDrop));
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
            String key = memberKey((Json.Member) member);
            if (key == null || key.contains("node_modules/")) {
                continue; // only importer entries seed the roots
            }
            Json.JsonObject importer = (Json.JsonObject) ((Json.Member) member).getValue();
            for (String scope : IMPORTER_SCOPES) {
                enqueueNames(getObjectMember(importer, scope), reachable, queue);
            }
        }
        while (!queue.isEmpty()) {
            String name = queue.poll();
            Json.JsonObject entry = getObjectMember(packages, "node_modules/" + name);
            if (entry == null) {
                continue;
            }
            enqueueNames(getObjectMember(entry, "dependencies"), reachable, queue);
            enqueueNames(getObjectMember(entry, "optionalDependencies"), reachable, queue);
            enqueueNames(getObjectMember(entry, "peerDependencies"), reachable, queue);
        }
        return reachable;
    }

    private void enqueueNames(Json.@Nullable JsonObject scope, Set<String> reachable, Deque<String> queue) {
        if (scope == null) {
            return;
        }
        for (Json member : scope.getMembers()) {
            if (member instanceof Json.Member) {
                String name = memberKey((Json.Member) member);
                if (name != null && reachable.add(name)) {
                    queue.add(name);
                }
            }
        }
    }

    // --- orphan GC after an edge-dropping bump ---------------------------

    /**
     * Drop every installed {@code packages} entry the bump left unreachable. Reachability is npm's own
     * hoisting resolution ({@link #resolveFrom}) over the full tree — importer roots seed it, and each
     * reachable entry's dependency edges resolve against its own install path — so a transitive still
     * required elsewhere (top-level or nested) survives. A pruned name that also lives at another placement
     * could re-hoist on removal, so it fails loud rather than risk a non-byte-exact tree.
     */
    private Json.JsonObject gcOrphansAfterBump(Json.JsonObject root, int lockfileVersion) {
        Json.JsonObject packages = requirePackages(root);
        Set<String> reachable = reachableInstalledKeys(packages);

        Map<String, Integer> placements = new LinkedHashMap<>();
        for (Json member : packages.getMembers()) {
            if (member instanceof Json.Member) {
                String key = memberKey((Json.Member) member);
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
            String key = memberKey((Json.Member) member);
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
        root = putMember(root, "packages", packages);

        if (lockfileVersion == 2) {
            Json.JsonObject legacy = getObjectMember(root, "dependencies");
            if (legacy != null) {
                for (String name : orphanNames) {
                    if (hasNestedOccurrence(legacy, name)) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                                "orphan-pruning " + name + " from a nested v2 legacy tree is not yet supported");
                    }
                }
                root = putMember(root, "dependencies", removeMembers(legacy, orphanNames));
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
                String key = memberKey((Json.Member) member);
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
            String key = memberKey((Json.Member) member);
            if (key == null || key.contains("node_modules/")) {
                continue; // only importer roots seed the walk
            }
            Json.JsonObject importer = (Json.JsonObject) ((Json.Member) member).getValue();
            for (String scope : IMPORTER_SCOPES) {
                enqueueResolved(keys, key, getObjectMember(importer, scope), reachable, queue);
            }
        }
        while (!queue.isEmpty()) {
            String key = queue.poll();
            Json.JsonObject entry = getObjectMember(packages, key);
            if (entry == null) {
                continue;
            }
            enqueueResolved(keys, key, getObjectMember(entry, "dependencies"), reachable, queue);
            enqueueResolved(keys, key, getObjectMember(entry, "optionalDependencies"), reachable, queue);
            enqueueResolved(keys, key, getObjectMember(entry, "peerDependencies"), reachable, queue);
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
            String name = memberKey((Json.Member) member);
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
     * npm's hoisting resolution: from the package installed at {@code fromKey}, resolve {@code name} by
     * checking its own {@code node_modules} then walking up each ancestor's, returning the matching entry
     * key (or {@code null} when unresolved — e.g. an optional/peer edge left unplaced).
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
        Json.JsonObject packages = getObjectMember(root, "packages");
        if (packages == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "package-lock.json has no packages map");
        }
        return packages;
    }

    private static Json.Document parse(String content) {
        JsonParser parser = new JsonParser();
        Parser.Input input = Parser.Input.fromString(content);
        SourceFile parsed = parser.parseInputs(Collections.singletonList(input), null,
                        new InMemoryExecutionContext())
                .findFirst().orElse(null);
        if (!(parsed instanceof Json.Document)) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "package-lock.json is not valid JSON");
        }
        return (Json.Document) parsed;
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
        Json.Member member = getMember(root, "lockfileVersion");
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
        Json.Member link = getMember(entry, "link");
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
        Json.Member member = getMember(obj, field);
        if (member == null) {
            return obj;
        }
        Space prefix = member.getValue().getPrefix();
        Json.Literal literal = new Json.Literal(Tree.randomId(), prefix, Markers.EMPTY, jsonEncode(value), value);
        return putMember(obj, field, literal);
    }

    /** Jackson-escaped quoted JSON string literal — registry {@code license}/{@code deprecated} may contain {@code "}/{@code \}/newlines. */
    private static String jsonEncode(String value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "could not JSON-encode value: " + value);
        }
    }

    /** Replace the value of member {@code key} in place, keeping its position and surrounding padding. */
    private static Json.JsonObject putMember(Json.JsonObject obj, String key, JsonValue newValue) {
        List<JsonRightPadded<Json>> members = new ArrayList<>(obj.getPadding().getMembers());
        for (int i = 0; i < members.size(); i++) {
            Json element = members.get(i).getElement();
            if (element instanceof Json.Member && key.equals(memberKey((Json.Member) element))) {
                Json.Member updated = ((Json.Member) element).withValue(newValue);
                members.set(i, members.get(i).withElement(updated));
                return obj.getPadding().withMembers(members);
            }
        }
        return obj;
    }

    /** Drop the named members, moving the closing-brace whitespace onto the new last member (byte-exact). */
    private static Json.JsonObject removeMembers(Json.JsonObject obj, Set<String> keys) {
        List<JsonRightPadded<Json>> original = obj.getPadding().getMembers();
        List<JsonRightPadded<Json>> kept = new ArrayList<>();
        boolean removed = false;
        for (JsonRightPadded<Json> rp : original) {
            Json element = rp.getElement();
            if (element instanceof Json.Member && keys.contains(memberKey((Json.Member) element))) {
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
            Json.JsonObject nested = getObjectMember((Json.JsonObject) ((Json.Member) member).getValue(), "dependencies");
            if (nested != null && (getMember(nested, name) != null || hasNestedOccurrence(nested, name))) {
                return true;
            }
        }
        return false;
    }

    private static Json.@Nullable JsonObject getObjectMember(Json.JsonObject obj, String key) {
        Json.Member member = getMember(obj, key);
        return member != null && member.getValue() instanceof Json.JsonObject ? (Json.JsonObject) member.getValue() : null;
    }

    private static @Nullable String stringField(Json.JsonObject obj, String key) {
        Json.Member member = getMember(obj, key);
        return member == null ? null : literalValue(member.getValue());
    }

    private static Json.@Nullable Member getMember(Json.JsonObject obj, String key) {
        for (Json member : obj.getMembers()) {
            if (member instanceof Json.Member && key.equals(memberKey((Json.Member) member))) {
                return (Json.Member) member;
            }
        }
        return null;
    }

    private static @Nullable String memberKey(Json.Member member) {
        JsonKey key = member.getKey();
        return key instanceof Json.Literal ? literalValue((Json.Literal) key) : null;
    }

    private static @Nullable String literalValue(@Nullable JsonValue value) {
        if (value instanceof Json.Literal) {
            Object v = ((Json.Literal) value).getValue();
            return v == null ? null : v.toString();
        }
        return null;
    }

    private static @Nullable String literalSource(@Nullable JsonValue value) {
        return value instanceof Json.Literal ? ((Json.Literal) value).getSource() : null;
    }
}
