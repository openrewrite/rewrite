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
package org.openrewrite.python.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.json.JsonParser;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonKey;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parses Pipfile.lock files (JSON format) to extract resolved dependency information.
 * <p>
 * Pipfile.lock has the shape:
 * <pre>
 * {
 *   "_meta": { "sources": [...], "requires": {...} },
 *   "default": { "requests": { "version": "==2.28.0", ... } },
 *   "develop": { "pytest":   { "version": "==7.0.0",  ... } }
 * }
 * </pre>
 */
public class PipfileLockParser {

    /**
     * Find and parse a Pipfile.lock file, walking up from the given directory.
     *
     * @param pipfileDir the directory containing Pipfile
     * @param boundary   the boundary to stop searching at (typically relativeTo)
     * @return list of resolved dependencies, or empty list if no Pipfile.lock found
     */
    public static List<ResolvedDependency> findAndParse(Path pipfileDir, @Nullable Path boundary) {
        Path lockFile = findLockFile(pipfileDir, boundary);
        if (lockFile == null) {
            return Collections.emptyList();
        }
        try {
            String content = new String(Files.readAllBytes(lockFile), StandardCharsets.UTF_8);
            return parse(content);
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    static @Nullable Path findLockFile(Path startDir, @Nullable Path boundary) {
        Path dir = startDir;
        while (dir != null) {
            Path lockFile = dir.resolve("Pipfile.lock");
            if (Files.isRegularFile(lockFile)) {
                return lockFile;
            }
            if (boundary != null && dir.equals(boundary)) {
                break;
            }
            dir = dir.getParent();
        }
        return null;
    }

    /**
     * Parse Pipfile.lock content into a list of resolved dependencies.
     * Entries from {@code default} appear before entries from {@code develop}.
     */
    public static List<ResolvedDependency> parse(String content) {
        JsonParser parser = new JsonParser();
        Parser.Input input = Parser.Input.fromString(Paths.get("Pipfile.lock"), content);
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input),
                null,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());

        if (parsed.isEmpty() || !(parsed.get(0) instanceof Json.Document)) {
            return Collections.emptyList();
        }

        Json.Document doc = (Json.Document) parsed.get(0);
        if (!(doc.getValue() instanceof Json.JsonObject)) {
            return Collections.emptyList();
        }
        Json.JsonObject root = (Json.JsonObject) doc.getValue();

        String defaultRegistry = extractDefaultRegistry(root);

        List<ResolvedDependency> resolved = new ArrayList<>();
        Json.JsonObject defaults = getObject(root, "default");
        if (defaults != null) {
            collectPackages(defaults, defaultRegistry, resolved);
        }
        Json.JsonObject develop = getObject(root, "develop");
        if (develop != null) {
            collectPackages(develop, defaultRegistry, resolved);
        }
        return resolved;
    }

    private static void collectPackages(Json.JsonObject packages, @Nullable String defaultRegistry, List<ResolvedDependency> out) {
        for (Json member : packages.getMembers()) {
            if (!(member instanceof Json.Member)) {
                continue;
            }
            Json.Member m = (Json.Member) member;
            String name = keyToString(m.getKey());
            if (name == null || !(m.getValue() instanceof Json.JsonObject)) {
                continue;
            }
            Json.JsonObject spec = (Json.JsonObject) m.getValue();
            String version = stripEqualsEquals(getStringMember(spec, "version"));
            if (version == null) {
                continue;
            }
            String source = extractSource(spec, defaultRegistry);
            out.add(new ResolvedDependency(name, version, source, null));
        }
    }

    /**
     * The {@code version} field in Pipfile.lock typically appears as
     * {@code "==2.28.0"}; we strip the leading {@code ==} so the value
     * matches the convention used by uv.lock entries.
     */
    private static @Nullable String stripEqualsEquals(@Nullable String version) {
        if (version == null) {
            return null;
        }
        if (version.startsWith("==")) {
            return version.substring(2);
        }
        return version;
    }

    /**
     * Extract a source URL or path for a single package entry.
     * Pipfile.lock entries can carry {@code git}, {@code path}, {@code file}, or
     * {@code index} fields; otherwise we fall back to the default registry from {@code _meta}.
     */
    private static @Nullable String extractSource(Json.JsonObject spec, @Nullable String defaultRegistry) {
        String git = getStringMember(spec, "git");
        if (git != null) {
            return git;
        }
        String path = getStringMember(spec, "path");
        if (path != null) {
            return path;
        }
        String file = getStringMember(spec, "file");
        if (file != null) {
            return file;
        }
        // index references a named source from _meta.sources, but resolving the
        // mapping is not worth the complexity here — fall through to the default.
        return defaultRegistry;
    }

    /**
     * Read {@code _meta.sources[0].url} as the default registry, since Pipfile.lock
     * stores per-package indexes only when they differ from the default.
     */
    private static @Nullable String extractDefaultRegistry(Json.JsonObject root) {
        Json.JsonObject meta = getObject(root, "_meta");
        if (meta == null) {
            return null;
        }
        for (Json member : meta.getMembers()) {
            if (!(member instanceof Json.Member)) {
                continue;
            }
            Json.Member m = (Json.Member) member;
            if (!"sources".equals(keyToString(m.getKey()))) {
                continue;
            }
            if (!(m.getValue() instanceof Json.Array)) {
                return null;
            }
            Json.Array sources = (Json.Array) m.getValue();
            for (JsonValue entry : sources.getValues()) {
                if (entry instanceof Json.JsonObject) {
                    String url = getStringMember((Json.JsonObject) entry, "url");
                    if (url != null) {
                        return url;
                    }
                }
            }
        }
        return null;
    }

    private static Json.@Nullable JsonObject getObject(Json.JsonObject obj, String key) {
        for (Json member : obj.getMembers()) {
            if (!(member instanceof Json.Member)) {
                continue;
            }
            Json.Member m = (Json.Member) member;
            if (key.equals(keyToString(m.getKey())) && m.getValue() instanceof Json.JsonObject) {
                return (Json.JsonObject) m.getValue();
            }
        }
        return null;
    }

    private static @Nullable String getStringMember(Json.JsonObject obj, String key) {
        for (Json member : obj.getMembers()) {
            if (!(member instanceof Json.Member)) {
                continue;
            }
            Json.Member m = (Json.Member) member;
            if (key.equals(keyToString(m.getKey())) && m.getValue() instanceof Json.Literal) {
                Object v = ((Json.Literal) m.getValue()).getValue();
                return v instanceof String ? (String) v : null;
            }
        }
        return null;
    }

    private static @Nullable String keyToString(JsonKey key) {
        if (key instanceof Json.Literal) {
            Object v = ((Json.Literal) key).getValue();
            return v instanceof String ? (String) v : null;
        }
        if (key instanceof Json.Identifier) {
            return ((Json.Identifier) key).getName();
        }
        return null;
    }
}
