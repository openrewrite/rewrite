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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;
import org.openrewrite.json.JsonIsoVisitor;
import org.openrewrite.json.JsonParser;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonKey;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.json.tree.Space;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

/**
 * Patches a {@code bun.lock} (JSONC). Bun's text lock round-trips byte-for-byte through the rewrite-json LST
 * (trailing commas, blank lines between entries, compact tuple arrays all preserved), so the patch is surgical
 * {@link Json.Literal} replacement: for each moving package the {@code packages} tuple's locator
 * ({@code "name@ver"}, element 0) and integrity (element 3) are rewritten and the {@code workspaces[dir]}
 * declared constraint re-pinned. Bun stores integrity only — no {@code resolved} URL — so element 1 and the
 * dependency metadata (element 2) are left untouched.
 */
public final class BunLockPatcher implements LockPatcher {

    @Override
    public String patch(LockEditSet edits) {
        String content = edits.getExistingLockContent();
        if (content == null) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "no bun lock content");
        }
        Json.Document document = parse(content, edits.getLockPath());
        if (!(document.getValue() instanceof Json.JsonObject)) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "bun.lock root is not an object");
        }

        boolean anyRemoval = false;
        for (PackageEdit edit : edits.getEdits()) {
            anyRemoval |= edit.getNewVersion() == null;
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

        Json.Document patched = (Json.Document) new BunVisitor(edits.getEdits(), edits.getEditedPackageJson())
                .visitNonNull(document, 0);
        return patched.printAll();
    }

    /** Rewrite the top-level {@code packages} object, dropping {@code dropKeys} and preserving bun's blank-line layout. */
    private static Json.JsonObject dropPackagesMembers(Json.JsonObject root, Set<String> dropKeys) {
        List<JsonRightPadded<Json>> top = new ArrayList<>(root.getPadding().getMembers());
        for (int i = 0; i < top.size(); i++) {
            Json el = top.get(i).getElement();
            if (el instanceof Json.Member && "packages".equals(literalKey(((Json.Member) el).getKey())) &&
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
            if (el instanceof Json.Member && dropKeys.contains(literalKey(((Json.Member) el).getKey()))) {
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
        Json.JsonObject packages = objectMember(root, "packages");
        if (packages == null) {
            return emptySet();
        }
        for (Json member : packages.getMembers()) {
            if (member instanceof Json.Member) {
                String key = literalKey(((Json.Member) member).getKey());
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
                String key = literalKey(((Json.Member) member).getKey());
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
        Json.JsonObject workspaces = objectMember(root, "workspaces");
        if (workspaces != null) {
            for (Json member : workspaces.getMembers()) {
                if (!(member instanceof Json.Member) || !(((Json.Member) member).getValue() instanceof Json.JsonObject)) {
                    continue;
                }
                Json.JsonObject importer = (Json.JsonObject) ((Json.Member) member).getValue();
                for (String scope : Arrays.asList("dependencies", "devDependencies", "optionalDependencies", "peerDependencies")) {
                    for (String name : memberKeys(objectMember(importer, scope))) {
                        if (!removedNames.contains(name) && reachable.add(name)) {
                            queue.add(name);
                        }
                    }
                }
            }
        }
        while (!queue.isEmpty()) {
            Json.JsonObject metadata = tupleMetadata(arrayMember(packages, queue.poll()));
            for (String scope : Arrays.asList("dependencies", "optionalDependencies", "peerDependencies")) {
                for (String name : memberKeys(objectMember(metadata, scope))) {
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

    private static Json.Document parse(String content, @Nullable Path lockPath) {
        Path path = lockPath == null ? Paths.get("bun.lock") : lockPath;
        Parser.Input input = Parser.Input.fromString(path, content);
        SourceFile sf = JsonParser.builder().build()
                .parseInputs(singletonList(input), null, new InMemoryExecutionContext())
                .findFirst()
                .orElseThrow(() -> new EngineFailure(Reason.MALFORMED_LOCK, null, "empty bun lock"));
        if (!(sf instanceof Json.Document)) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "bun.lock is not valid JSONC");
        }
        return (Json.Document) sf;
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
            String key = literalKey(m.getKey());
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

    private static Json.@Nullable JsonObject objectMember(Json.@Nullable JsonObject obj, String key) {
        if (obj == null) {
            return null;
        }
        for (Json member : obj.getMembers()) {
            if (member instanceof Json.Member && key.equals(literalKey(((Json.Member) member).getKey())) &&
                    ((Json.Member) member).getValue() instanceof Json.JsonObject) {
                return (Json.JsonObject) ((Json.Member) member).getValue();
            }
        }
        return null;
    }

    private static Json.@Nullable Array arrayMember(Json.JsonObject obj, String key) {
        for (Json member : obj.getMembers()) {
            if (member instanceof Json.Member && key.equals(literalKey(((Json.Member) member).getKey())) &&
                    ((Json.Member) member).getValue() instanceof Json.Array) {
                return (Json.Array) ((Json.Member) member).getValue();
            }
        }
        return null;
    }

    private static List<String> memberKeys(Json.@Nullable JsonObject obj) {
        List<String> keys = new ArrayList<>();
        if (obj != null) {
            for (Json member : obj.getMembers()) {
                if (member instanceof Json.Member) {
                    String key = literalKey(((Json.Member) member).getKey());
                    if (key != null) {
                        keys.add(key);
                    }
                }
            }
        }
        return keys;
    }

    private static @Nullable String literalKey(JsonKey key) {
        if (key instanceof Json.Literal) {
            Object v = ((Json.Literal) key).getValue();
            return v == null ? null : v.toString();
        }
        return null;
    }

    /** True when the cursor's member sits at {@code workspaces.<importerKey>.<scope>.<name>}. */
    private static boolean isWorkspaceConstraint(Cursor cursor, String importerKey) {
        return hasAncestorMemberKey(cursor, "workspaces") && hasAncestorMemberKey(cursor, importerKey);
    }

    private static boolean hasAncestorMemberKey(Cursor cursor, String key) {
        Cursor c = cursor.getParent();
        while (c != null) {
            Object value = c.getValue();
            if (value instanceof Json.Member && key.equals(literalKey(((Json.Member) value).getKey()))) {
                return true;
            }
            c = c.getParent();
        }
        return false;
    }
}
