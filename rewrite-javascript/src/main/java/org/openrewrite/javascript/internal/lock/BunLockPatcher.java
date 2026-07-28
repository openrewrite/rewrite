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
import org.openrewrite.json.tree.JsonValue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
        Json.Document patched = (Json.Document) new BunVisitor(edits.getEdits(), edits.getEditedPackageJson())
                .visitNonNull(document, 0);
        return patched.printAll();
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
            // Removal of a packages entry: drop the whole tuple member.
            if (m.getValue() instanceof Json.Array && hasAncestorMemberKey(getCursor(), "packages")) {
                for (PackageEdit edit : edits) {
                    if (edit.getNewVersion() == null && locatorMatches((Json.Array) m.getValue(), locator(edit, edit.getOldVersion()))) {
                        return null;
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

        private static boolean locatorMatches(Json.Array array, String locator) {
            List<JsonValue> values = array.getValues();
            return !values.isEmpty() && values.get(0) instanceof Json.Literal &&
                    locator.equals(((Json.Literal) values.get(0)).getValue());
        }

        private static String importerKey(PackageEdit edit) {
            return edit.getImporterDir() == null ? "" : edit.getImporterDir();
        }
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
