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
import org.openrewrite.Cursor;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;
import org.openrewrite.json.JsonIsoVisitor;
import org.openrewrite.json.internal.JsonPrinter;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.json.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptySet;
import static org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit.Kind.*;

/**
 * Patches a {@code bun.lock} (JSONC). Bun's text lock round-trips byte-for-byte through the rewrite-json LST,
 * so the patch is surgical {@link Json.Literal} replacement: each moving package's {@code packages} tuple
 * locator ({@code "name@ver"}, element 0) and integrity (element 3) are rewritten and its {@code workspaces[dir]}
 * constraint re-pinned. Bun stores integrity only (no {@code resolved} URL), so element 1 and the metadata (2) stay.
 */
public final class BunLockPatcher implements LockPatcher {

    @Override
    public String patch(LockEditSet edits) {
        String content = edits.getExistingLockContent();
        if (content == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "no bun lock content");
        }
        Json.Document document = LockJson.parse(content, edits.getLockPath());
        if (!(document.getValue() instanceof Json.JsonObject)) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "bun.lock root is not an object");
        }

        boolean anyRemoval = false;
        List<PackageEdit> adds = new ArrayList<>();
        List<PackageEdit> rewrites = new ArrayList<>();
        List<PackageEdit> relocateNests = new ArrayList<>();
        List<PackageEdit> freshNests = new ArrayList<>();
        for (PackageEdit edit : edits.getEdits()) {
            if (edit.getNestedUnder() != null) {
                (edit.getKind() == ADD ? freshNests : relocateNests).add(edit);
            } else if (edit.getNewVersion() == null) {
                anyRemoval = true;
                rewrites.add(edit);
            } else if (edit.getKind() == ADD) {
                adds.add(edit);
            } else {
                rewrites.add(edit);
            }
        }

        // A relocate-nest (an upgrade pushing the old version down) copies the moving package's pre-bump
        // tuple, so capture it before the visitor rewrites that tuple to the new version.
        Map<PackageEdit, Json.Array> nestedTuples = new LinkedHashMap<>();
        for (PackageEdit nest : relocateNests) {
            Json.Array tuple = LockJson.arrayMember(LockJson.objectMember((Json.JsonObject) document.getValue(), "packages"), nest.getName());
            if (tuple == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, nest.getName(),
                        "no bun tuple to nest for " + nest.getName());
            }
            nestedTuples.put(nest, tuple);
        }

        if (anyRemoval) {
            // Drop the removed roots and their orphaned transitives from `packages` structurally (with a
            // first-member prefix fixup) — a visitor null-return would leave a stray blank line behind.
            Json.JsonObject root = (Json.JsonObject) document.getValue();
            Set<String> orphanKeys = orphanPackageKeys(root, edits.getEdits());
            if (!orphanKeys.isEmpty()) {
                document = document.withValue(dropPackagesMembers(root, orphanKeys));
            }
        }

        if (!adds.isEmpty()) {
            document = document.withValue(
                    applyAdds((Json.JsonObject) document.getValue(), adds, edits.getEditedPackageJson()));
        }

        Json.Document patched = (Json.Document) new BunVisitor(rewrites, edits.getEditedPackageJson())
                .visitNonNull(document, 0);

        if (!relocateNests.isEmpty()) {
            patched = patched.withValue(applyNests((Json.JsonObject) patched.getValue(), relocateNests, nestedTuples));
        }
        if (!freshNests.isEmpty()) {
            patched = patched.withValue(applyFreshNests((Json.JsonObject) patched.getValue(), freshNests));
        }
        return patched.printAll();
    }

    // --- leaf / clean-closure add ----------------------------------

    /**
     * Insert each add's {@code packages} tuple and, for a declared root, its {@code workspaces[dir].<scope>}
     * constraint (both ASCII-sorted). A transitive has no importer edge, so only a declared root re-pins.
     */
    private static Json.JsonObject applyAdds(Json.JsonObject root, List<PackageEdit> adds,
                                             @Nullable String editedPackageJson) {
        root = insertPackageTuples(root, adds);
        return insertWorkspaceConstraints(root, adds, editedPackageJson);
    }

    private static Json.JsonObject insertPackageTuples(Json.JsonObject root, List<PackageEdit> adds) {
        Json.JsonObject packages = LockJson.objectMember(root, "packages");
        if (packages == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "bun.lock has no packages map");
        }
        String firstWs = LockJson.memberWhitespace(packages);
        if (firstWs == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "bun.lock packages map is empty");
        }
        // A nested placement (parent/name) needs the hoisting model; the flat sorted insert cannot honour it.
        for (Json member : packages.getMembers()) {
            if (member instanceof Json.Member) {
                String key = LockJson.literal(((Json.Member) member).getKey());
                if (key != null && key.indexOf('/') >= 0) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                            "cannot add into a bun.lock with nested placements: " + key);
                }
            }
        }

        List<JsonRightPadded<Json>> members = new ArrayList<>(packages.getPadding().getMembers());
        for (PackageEdit add : adds) {
            members = insertSorted(members, buildPackageMember(add));
        }
        // bun separates entries with a blank line: the first keeps its newline+indent, the rest gain one.
        members = normalizePrefixes(members, firstWs, "\n" + firstWs);
        return LockJson.replaceValue(root, "packages", packages.getPadding().withMembers(members));
    }

    private static Json.JsonObject insertWorkspaceConstraints(Json.JsonObject root, List<PackageEdit> adds,
                                                              @Nullable String editedPackageJson) {
        Json.JsonObject workspaces = LockJson.objectMember(root, "workspaces");
        if (workspaces == null) {
            return root;
        }
        for (PackageEdit add : adds) {
            String constraint = LockManifests.declaredConstraint(editedPackageJson, add.getScope(), add.getName());
            if (constraint == null) {
                continue; // a transitive: absent from the edited package.json, so no importer edge
            }
            String importerKey = add.getImporterDir() == null ? "" : add.getImporterDir();
            Json.JsonObject importer = LockJson.objectMember(workspaces, importerKey);
            if (importer == null) {
                continue;
            }
            Json.JsonObject scope = LockJson.objectMember(importer, add.getScope());
            if (scope == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, add.getName(),
                        "adding a new " + add.getScope() + " scope to bun.lock is not yet supported");
            }
            String ws = LockJson.memberWhitespace(scope);
            if (ws == null) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, add.getName(), "cannot derive workspace indentation");
            }
            List<JsonRightPadded<Json>> members =
                    insertSorted(new ArrayList<>(scope.getPadding().getMembers()), buildLiteralMember(add.getName(), constraint));
            members = normalizePrefixes(members, ws, ws);
            scope = scope.getPadding().withMembers(members);
            importer = LockJson.replaceValue(importer, add.getScope(), scope);
            workspaces = LockJson.replaceValue(workspaces, importerKey, importer);
        }
        return LockJson.replaceValue(root, "workspaces", workspaces);
    }

    // --- reverse-dependent nest ---------------------------------

    /**
     * Insert each nested copy as a {@code "<dependent>/<name>"} tuple relocated byte-for-byte from the pre-bump
     * top-level entry (kept for the reverse-dependent whose constraint excludes the new version); bun places
     * nested entries after all top-level ones.
     */
    private static Json.JsonObject applyNests(Json.JsonObject root, List<PackageEdit> nests,
                                              Map<PackageEdit, Json.Array> nestedTuples) {
        Json.JsonObject packages = LockJson.objectMember(root, "packages");
        if (packages == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "bun.lock has no packages map");
        }
        String firstWs = LockJson.memberWhitespace(packages);
        if (firstWs == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "bun.lock packages map is empty");
        }
        List<JsonRightPadded<Json>> members = new ArrayList<>(packages.getPadding().getMembers());
        for (PackageEdit nest : nests) {
            String key = nest.getNestedUnder() + "/" + nest.getName();
            if (hasMemberKey(members, key)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, nest.getName(), key + " already present (fork exists)");
            }
            Json.Member member = parseMember(quote(key) + ": " + tupleSource(nestedTuples.get(nest)));
            members = insertNestedLast(members, member);
        }
        members = normalizePrefixes(members, firstWs, "\n" + firstWs);
        return LockJson.replaceValue(root, "packages", packages.getPadding().withMembers(members));
    }

    /**
     * Insert each fresh nested add (a new closure member a frozen top-level pin excludes) as a
     * {@code "<dependent>/<name>"} tuple built from the resolved leaf, after all top-level entries.
     */
    private static Json.JsonObject applyFreshNests(Json.JsonObject root, List<PackageEdit> nests) {
        Json.JsonObject packages = LockJson.objectMember(root, "packages");
        if (packages == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "bun.lock has no packages map");
        }
        String firstWs = LockJson.memberWhitespace(packages);
        if (firstWs == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "bun.lock packages map is empty");
        }
        List<JsonRightPadded<Json>> members = new ArrayList<>(packages.getPadding().getMembers());
        for (PackageEdit nest : nests) {
            String key = nest.getNestedUnder() + "/" + nest.getName();
            if (hasMemberKey(members, key)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, nest.getName(), key + " already present (fork exists)");
            }
            members = insertNestedLast(members, buildNestedPackageMember(key, nest));
        }
        members = normalizePrefixes(members, firstWs, "\n" + firstWs);
        return LockJson.replaceValue(root, "packages", packages.getPadding().withMembers(members));
    }

    /** Build a nested {@code "<parent>/<name>": ["<name>@<ver>", "", <metadata>, "<sri>"]} tuple member from source text. */
    private static Json.Member buildNestedPackageMember(String key, PackageEdit add) {
        String integrity = add.getNewIntegrity();
        if (integrity == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, add.getName(), add.getName() + " has no integrity");
        }
        String locator = add.getName() + "@" + add.getNewVersion();
        String tuple = "[" + quote(locator) + ", " + quote("") + ", " +
                renderMetadata(add.getNewDependencies()) + ", " + quote(integrity) + "]";
        return parseMember(quote(key) + ": " + tuple);
    }

    /** A tuple printed as source with its leading prefix stripped, so it re-grafts cleanly under a fresh key. */
    private static String tupleSource(Json.Array tuple) {
        String printed = tuple.print(new JsonPrinter<Integer>());
        int i = 0;
        while (i < printed.length() && Character.isWhitespace(printed.charAt(i))) {
            i++;
        }
        return printed.substring(i);
    }

    /** Append after the last real member (bun places nested {@code parent/name} entries after all top-level ones). */
    private static List<JsonRightPadded<Json>> insertNestedLast(List<JsonRightPadded<Json>> members, Json.Member member) {
        int idx = members.size();
        for (int i = members.size() - 1; i >= 0; i--) {
            if (members.get(i).getElement() instanceof Json.Member) {
                idx = i + 1;
                break;
            }
        }
        members.add(idx, new JsonRightPadded<>(member, Space.EMPTY, Markers.EMPTY));
        return members;
    }

    private static boolean hasMemberKey(List<JsonRightPadded<Json>> members, String key) {
        for (JsonRightPadded<Json> rp : members) {
            if (rp.getElement() instanceof Json.Member && key.equals(LockJson.literal(((Json.Member) rp.getElement()).getKey()))) {
                return true;
            }
        }
        return false;
    }

    /** Build the {@code "<name>": ["<name>@<ver>", "", <metadata>, "<sri>"]} tuple member from exact source text. */
    private static Json.Member buildPackageMember(PackageEdit add) {
        String integrity = add.getNewIntegrity();
        if (integrity == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, add.getName(), add.getName() + " has no integrity");
        }
        String locator = add.getName() + "@" + add.getNewVersion();
        String tuple = "[" + quote(locator) + ", " + quote("") + ", " +
                renderMetadata(add.getNewDependencies()) + ", " + quote(integrity) + "]";
        return parseMember(quote(add.getName()) + ": " + tuple);
    }

    private static Json.Member buildLiteralMember(String name, String value) {
        return parseMember(quote(name) + ": " + quote(value));
    }

    /** bun's compact single-line metadata: {@code {}} or {@code { "dependencies": { "<dep>": "<range>", … } }}. */
    private static String renderMetadata(@Nullable Map<String, String> deps) {
        if (deps == null || deps.isEmpty()) {
            return "{}";
        }
        return "{ " + quote("dependencies") + ": " + renderInlineMap(deps) + " }";
    }

    private static String renderInlineMap(Map<String, String> map) {
        List<String> keys = new ArrayList<>(map.keySet());
        keys.sort(null); // bun sorts the dependency map by name (ASCII)
        StringBuilder sb = new StringBuilder("{ ");
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(quote(keys.get(i))).append(": ").append(quote(map.get(keys.get(i))));
        }
        return sb.append(" }").toString();
    }

    /** Splice {@code member} at its ASCII-sorted key position, before any trailing comma placeholder ({@link Json.Empty}). */
    private static List<JsonRightPadded<Json>> insertSorted(List<JsonRightPadded<Json>> members, Json.Member member) {
        String newKey = LockJson.literal(member.getKey());
        int idx = members.size();
        for (int i = 0; i < members.size(); i++) {
            Json el = members.get(i).getElement();
            if (!(el instanceof Json.Member)) {
                idx = i; // insert a real member before the trailing comma placeholder
                break;
            }
            String k = LockJson.literal(((Json.Member) el).getKey());
            if (k != null && newKey != null && k.compareTo(newKey) > 0) {
                idx = i;
                break;
            }
        }
        members.add(idx, new JsonRightPadded<>(member, Space.EMPTY, Markers.EMPTY));
        return members;
    }

    /** Set the first real member's prefix to {@code firstWs} and every following one to {@code restWs} (byte-exact whitespace). */
    private static List<JsonRightPadded<Json>> normalizePrefixes(List<JsonRightPadded<Json>> members,
                                                                 String firstWs, String restWs) {
        boolean first = true;
        for (int i = 0; i < members.size(); i++) {
            Json el = members.get(i).getElement();
            if (!(el instanceof Json.Member)) {
                continue; // the trailing comma placeholder keeps its own prefix
            }
            if (!el.getPrefix().getComments().isEmpty()) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null, "bun.lock has comments; add not supported");
            }
            String target = first ? firstWs : restWs;
            if (!el.getPrefix().getWhitespace().equals(target)) {
                members.set(i, members.get(i).withElement(el.withPrefix(el.getPrefix().withWhitespace(target))));
            }
            first = false;
        }
        return members;
    }

    /** Parse a single member from a throwaway wrapper so its hand-crafted inner bytes round-trip exactly. */
    private static Json.Member parseMember(String memberSource) {
        Json.Document doc = LockJson.parse("{" + memberSource + "}", null);
        if (doc.getValue() instanceof Json.JsonObject) {
            for (Json member : ((Json.JsonObject) doc.getValue()).getMembers()) {
                if (member instanceof Json.Member) {
                    return (Json.Member) member;
                }
            }
        }
        throw new EngineFailure(Reason.MALFORMED_LOCK, null, "could not construct bun lock member");
    }

    /** bun package names, versions, ranges and SRI integrity never contain {@code "}/{@code \\}, so a plain quote suffices. */
    private static String quote(String value) {
        return "\"" + value + "\"";
    }

    /** Rewrite the top-level {@code packages} object, dropping {@code dropKeys} and preserving bun's blank-line layout. */
    private static Json.JsonObject dropPackagesMembers(Json.JsonObject root, Set<String> dropKeys) {
        List<JsonRightPadded<Json>> top = new ArrayList<>(root.getPadding().getMembers());
        for (int i = 0; i < top.size(); i++) {
            Json el = top.get(i).getElement();
            if (el instanceof Json.Member && "packages".equals(LockJson.literal(((Json.Member) el).getKey())) &&
                    ((Json.Member) el).getValue() instanceof Json.JsonObject) {
                Json.JsonObject trimmed = dropMembersWithFixup((Json.JsonObject) ((Json.Member) el).getValue(), dropKeys);
                top.set(i, top.get(i).withElement(((Json.Member) el).withValue(trimmed)));
                return root.getPadding().withMembers(top);
            }
        }
        return root;
    }

    private static Json.JsonObject dropMembersWithFixup(Json.JsonObject obj, Set<String> dropKeys) {
        List<JsonRightPadded<Json>> original = obj.getPadding().getMembers();
        Space firstPrefix = null;
        for (JsonRightPadded<Json> rp : original) {
            if (rp.getElement() instanceof Json.Member) {
                firstPrefix = rp.getElement().getPrefix();
                break;
            }
        }
        List<JsonRightPadded<Json>> kept = new ArrayList<>();
        boolean removed = false;
        for (JsonRightPadded<Json> rp : original) {
            Json el = rp.getElement();
            if (el instanceof Json.Member && dropKeys.contains(LockJson.literal(((Json.Member) el).getKey()))) {
                removed = true;
                continue;
            }
            kept.add(rp);
        }
        if (!removed) {
            return obj;
        }
        // The new first entry inherits the original first entry's prefix, so a removed leading entry doesn't
        // leave its successor carrying an extra blank line.
        if (firstPrefix != null) {
            for (int i = 0; i < kept.size(); i++) {
                Json el = kept.get(i).getElement();
                if (el instanceof Json.Member) {
                    if (!el.getPrefix().equals(firstPrefix)) {
                        kept.set(i, kept.get(i).withElement(el.withPrefix(firstPrefix)));
                    }
                    break;
                }
            }
        }
        return obj.getPadding().withMembers(kept);
    }

    /**
     * The package-map keys to drop for a removal: the removed roots plus every transitive no longer reachable
     * from a surviving workspace dependency (mirrors what a real {@code bun install} produces for a non-leaf
     * removal). Nested placements ({@code parent/name} keys) need the hoisting model — fail loud instead.
     */
    private static Set<String> orphanPackageKeys(Json.JsonObject root, List<PackageEdit> edits) {
        Json.JsonObject packages = LockJson.objectMember(root, "packages");
        if (packages == null) {
            return emptySet();
        }
        for (Json member : packages.getMembers()) {
            if (member instanceof Json.Member) {
                String key = LockJson.literal(((Json.Member) member).getKey());
                if (key != null && key.indexOf('/') >= 0) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                            "cannot remove from a bun.lock with nested placements: " + key);
                }
            }
        }
        Set<String> removedNames = new LinkedHashSet<>();
        for (PackageEdit edit : edits) {
            if (edit.getNewVersion() == null) {
                removedNames.add(edit.getName());
            }
        }
        Set<String> reachable = reachableNames(root, packages, removedNames);
        Set<String> drop = new LinkedHashSet<>();
        for (Json member : packages.getMembers()) {
            if (member instanceof Json.Member) {
                String key = LockJson.literal(((Json.Member) member).getKey());
                if (key != null && !reachable.contains(key)) {
                    drop.add(key);
                }
            }
        }
        return drop;
    }

    private static Set<String> reachableNames(Json.JsonObject root, Json.JsonObject packages, Set<String> removedNames) {
        Set<String> reachable = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        Json.JsonObject workspaces = LockJson.objectMember(root, "workspaces");
        if (workspaces != null) {
            for (Json member : workspaces.getMembers()) {
                if (!(member instanceof Json.Member) || !(((Json.Member) member).getValue() instanceof Json.JsonObject)) {
                    continue;
                }
                Json.JsonObject importer = (Json.JsonObject) ((Json.Member) member).getValue();
                for (String scope : Arrays.asList("dependencies", "devDependencies", "optionalDependencies", "peerDependencies")) {
                    for (String name : memberKeys(LockJson.objectMember(importer, scope))) {
                        if (!removedNames.contains(name) && reachable.add(name)) {
                            queue.add(name);
                        }
                    }
                }
            }
        }
        while (!queue.isEmpty()) {
            Json.JsonObject metadata = tupleMetadata(LockJson.arrayMember(packages, queue.poll()));
            for (String scope : Arrays.asList("dependencies", "optionalDependencies", "peerDependencies")) {
                for (String name : memberKeys(LockJson.objectMember(metadata, scope))) {
                    if (reachable.add(name)) {
                        queue.add(name);
                    }
                }
            }
        }
        return reachable;
    }

    /** The metadata object (tuple element 2) of a {@code packages} entry's {@code ["name@ver", "", {…}, "sri"]} array. */
    private static Json.@Nullable JsonObject tupleMetadata(Json.@Nullable Array tuple) {
        if (tuple == null || tuple.getValues().size() < 3 || !(tuple.getValues().get(2) instanceof Json.JsonObject)) {
            return null;
        }
        return (Json.JsonObject) tuple.getValues().get(2);
    }

    private static final class BunVisitor extends JsonIsoVisitor<Integer> {
        private final List<PackageEdit> edits;
        private final @Nullable String editedPackageJson;

        private BunVisitor(List<PackageEdit> edits, @Nullable String editedPackageJson) {
            this.edits = edits;
            this.editedPackageJson = editedPackageJson;
        }

        @Override
        public Json.@Nullable Member visitMember(Json.Member member, Integer p) {
            Json.Member m = super.visitMember(member, p);
            String key = LockJson.literal(m.getKey());
            if (key == null) {
                return m;
            }
            // Root/workspace-member declared constraint: workspaces[dir].<scope>.<name>
            for (PackageEdit edit : edits) {
                if (key.equals(edit.getName()) && m.getValue() instanceof Json.Literal &&
                        isWorkspaceConstraint(getCursor(), importerKey(edit))) {
                    if (edit.getNewVersion() == null) {
                        return null;
                    }
                    String constraint = LockManifests.declaredConstraint(editedPackageJson, edit.getScope(), edit.getName());
                    if (constraint != null) {
                        Json.Literal lit = (Json.Literal) m.getValue();
                        return m.withValue(lit.withSource('"' + constraint + '"').withValue(constraint));
                    }
                }
            }
            return m;
        }

        @Override
        public Json.Array visitArray(Json.Array array, Integer p) {
            Json.Array a = super.visitArray(array, p);
            List<JsonValue> values = a.getValues();
            if (values.size() < 4 || !(values.get(0) instanceof Json.Literal)) {
                return a;
            }
            Object first = ((Json.Literal) values.get(0)).getValue();
            for (PackageEdit edit : edits) {
                if (edit.getNewVersion() == null) {
                    continue;
                }
                if (locator(edit, edit.getOldVersion()).equals(first)) {
                    List<JsonValue> updated = new ArrayList<>(values);
                    String newLocator = locator(edit, edit.getNewVersion());
                    updated.set(0, ((Json.Literal) values.get(0)).withSource('"' + newLocator + '"').withValue(newLocator));
                    if (edit.getNewIntegrity() != null && values.get(3) instanceof Json.Literal) {
                        String integrity = edit.getNewIntegrity();
                        updated.set(3, ((Json.Literal) values.get(3)).withSource('"' + integrity + '"').withValue(integrity));
                    }
                    return a.withValues(updated);
                }
            }
            return a;
        }

        private static String locator(PackageEdit edit, @Nullable String version) {
            return edit.getName() + "@" + version;
        }

        private static String importerKey(PackageEdit edit) {
            return edit.getImporterDir() == null ? "" : edit.getImporterDir();
        }
    }

    private static List<String> memberKeys(Json.@Nullable JsonObject obj) {
        List<String> keys = new ArrayList<>();
        if (obj != null) {
            for (Json member : obj.getMembers()) {
                if (member instanceof Json.Member) {
                    String key = LockJson.literal(((Json.Member) member).getKey());
                    if (key != null) {
                        keys.add(key);
                    }
                }
            }
        }
        return keys;
    }

    /** True when the cursor's member sits at {@code workspaces.<importerKey>.<scope>.<name>}. */
    private static boolean isWorkspaceConstraint(Cursor cursor, String importerKey) {
        return hasAncestorMemberKey(cursor, "workspaces") && hasAncestorMemberKey(cursor, importerKey);
    }

    private static boolean hasAncestorMemberKey(Cursor cursor, String key) {
        Cursor c = cursor.getParent();
        while (c != null) {
            Object value = c.getValue();
            if (value instanceof Json.Member && key.equals(LockJson.literal(((Json.Member) value).getKey()))) {
                return true;
            }
            c = c.getParent();
        }
        return false;
    }
}
