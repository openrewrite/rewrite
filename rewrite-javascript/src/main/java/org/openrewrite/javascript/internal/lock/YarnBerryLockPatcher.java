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

import static java.util.Collections.singletonList;

/**
 * Byte-exact patcher for a Yarn Berry {@code yarn.lock} (the {@code __metadata:}-headed YAML format). The file
 * round-trips byte-identical through the rewrite-yaml LST, so rewriting only the named entries preserves every
 * other byte. A registry dependency's {@code checksum} — long the blocker for berry — is reproduced by
 * {@link BerryZipChecksum} and threaded in on the edit; the engine derives it before this patcher runs.
 * <p>
 * Only an in-place version bump is supported so far: the package entry's descriptor key, {@code version},
 * {@code resolution} and {@code checksum} plus the importer's declared range. Anything that reshapes the
 * closure (a changed {@code dependencies} map, an add, a removal, a merged descriptor key, a fork) fails loud.
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

        for (PackageEdit edit : edits.getEdits()) {
            if (edit.getKind() != PackageEdit.Kind.BUMP) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, edit.getName(),
                        "yarn berry only supports in-place version bumps so far (" + edit.getKind() + " deferred)");
            }
            root = applyBump(root, edit, edits.getEditedPackageJson());
        }

        List<Yaml.Document> newDocuments = new ArrayList<>(documents);
        newDocuments.set(0, document.withBlock(root));
        return docs.withDocuments(newDocuments).printAll();
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
        // A bump whose closure is unchanged rewrites only these four; the engine already proved everything else
        // (dependencies, peer/optional surfaces) is byte-identical, so leave the rest of the entry alone.
        body = setScalar(body, "version", edit.getNewVersion(), name);
        body = setScalar(body, "resolution", name + "@npm:" + edit.getNewVersion(), name);
        body = setScalar(body, "checksum", edit.getNewBerryChecksum(), name);
        root = replaceEntry(root, oldDescriptor, renameKey(entry, newDescriptor).withValue(body));

        return repinImporter(root, name, newConstraint);
    }

    /** Re-pin the single workspace importer's declared range on {@code name} to {@code npm:<newConstraint>}. */
    private Yaml.Mapping repinImporter(Yaml.Mapping root, String name, String newConstraint) {
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
