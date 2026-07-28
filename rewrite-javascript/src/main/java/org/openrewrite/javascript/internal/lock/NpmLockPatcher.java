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
import org.jspecify.annotations.Nullable;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;
import org.openrewrite.javascript.internal.lock.LockEditSet.WriteThroughMetadata;
import org.openrewrite.json.JsonParser;
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
import java.util.LinkedHashSet;
import java.util.List;
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

        List<PackageEdit> removals = new ArrayList<>();
        for (PackageEdit edit : edits.getEdits()) {
            if (edit.getNewVersion() == null) {
                removals.add(edit);
                continue;
            }
            root = applyBump(root, lockfileVersion, editedManifest, edit);
        }
        if (!removals.isEmpty()) {
            root = applyRemovals(root, lockfileVersion, removals);
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
            entry = applyWriteThrough(name, entry, edit.getWriteThroughMetadata());
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
        legacy = putMember(legacy, name, entry);
        return putMember(root, "dependencies", legacy);
    }

    private Json.JsonObject applyWriteThrough(String name, Json.JsonObject entry, @Nullable WriteThroughMetadata wt) {
        if (wt == null) {
            return entry;
        }
        if (wt.getEngines() != null || wt.getBin() != null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " engines/bin metadata changed; native write-through is not supported");
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
