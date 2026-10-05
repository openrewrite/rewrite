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
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 * Byte-exact keyed access and edit primitives over a YAML lockfile's LST, shared by the pnpm and yarn-berry
 * patchers (both formats round-trip byte-identical through rewrite-yaml). Each edit returns a new mapping,
 * rewriting only the named entry and leaving every other byte untouched. The JSON analogue is {@link LockJson}.
 */
final class LockYaml {

    private LockYaml() {
    }

    /** Parse a lockfile into its {@link Yaml.Documents} LST; {@code path} is attribution only, not parse-gating. */
    static Yaml.Documents parse(String content, @Nullable Path path) {
        SourceFile source;
        try {
            ExecutionContext ctx = new InMemoryExecutionContext();
            Parser.Input input = Parser.Input.fromString(path == null ? Paths.get("lock.yaml") : path, content);
            source = new YamlParser().parseInputs(singletonList(input), null, ctx).findFirst().orElse(null);
        } catch (RuntimeException e) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "unparseable lockfile: " + e.getMessage());
        }
        if (!(source instanceof Yaml.Documents)) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "lockfile could not be parsed as YAML");
        }
        return (Yaml.Documents) source;
    }

    static @Nullable String keyOf(Yaml.Mapping.Entry entry) {
        return entry.getKey() instanceof Yaml.Scalar ? ((Yaml.Scalar) entry.getKey()).getValue() : null;
    }

    static Yaml.Mapping.@Nullable Entry findEntry(Yaml.Mapping mapping, String key) {
        for (Yaml.Mapping.Entry entry : mapping.getEntries()) {
            if (key.equals(keyOf(entry))) {
                return entry;
            }
        }
        return null;
    }

    /** Replace the entry whose key equals {@code key} with {@code replacement}; a no-op if absent. */
    static Yaml.Mapping replaceEntry(Yaml.Mapping mapping, @Nullable String key, Yaml.Mapping.Entry replacement) {
        List<Yaml.Mapping.Entry> entries = new ArrayList<>(mapping.getEntries());
        for (int i = 0; i < entries.size(); i++) {
            if (key != null && key.equals(keyOf(entries.get(i)))) {
                entries.set(i, replacement);
                return mapping.withEntries(entries);
            }
        }
        return mapping;
    }

    static Yaml.Mapping.Entry renameKey(Yaml.Mapping.Entry entry, String newKey) {
        if (!(entry.getKey() instanceof Yaml.Scalar)) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "cannot rename a non-scalar key");
        }
        return entry.withKey(((Yaml.Scalar) entry.getKey()).withValue(newKey));
    }

    /** Set the scalar value of {@code mapping[key]}, preserving the scalar's quote style. */
    static Yaml.Mapping setScalar(Yaml.Mapping mapping, String key, String value) {
        Yaml.Mapping.Entry entry = findEntry(mapping, key);
        if (entry == null || !(entry.getValue() instanceof Yaml.Scalar)) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "expected scalar entry '" + key + "'");
        }
        return replaceEntry(mapping, key, entry.withValue(((Yaml.Scalar) entry.getValue()).withValue(value)));
    }

    /**
     * Parse a synthetic snippet and return the entry at {@code path} (each segment a mapping key), so a fresh
     * entry inherits the LST's exact scalar styling before being grafted into the real tree.
     */
    static Yaml.Mapping.Entry graft(String synthetic, String... path) {
        Yaml.Documents docs = parse(synthetic, null);
        if (docs.getDocuments().isEmpty() || !(docs.getDocuments().get(0).getBlock() instanceof Yaml.Mapping)) {
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "could not construct lock entry");
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
            throw new EngineFailure(Reason.MALFORMED_LOCK, null, "could not construct lock entry " + path[path.length - 1]);
        }
        return leaf;
    }
}
