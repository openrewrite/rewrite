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
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parses uv.lock files (TOML format) to extract resolved dependency information.
 */
public class UvLockParser {

    /**
     * Find and parse a uv.lock file, walking up from the given directory.
     *
     * @param pyprojectDir the directory containing pyproject.toml
     * @param boundary     the boundary to stop searching at (typically relativeTo)
     * @return list of resolved dependencies, or empty list if no uv.lock found
     */
    public static List<ResolvedDependency> findAndParse(Path pyprojectDir, @Nullable Path boundary) {
        Path lockFile = findLockFile(pyprojectDir, boundary);
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
            Path lockFile = dir.resolve("uv.lock");
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
     * Parse uv.lock content into a list of resolved dependencies.
     */
    static List<ResolvedDependency> parse(String content) {
        TomlParser parser = new TomlParser();
        Parser.Input input = Parser.Input.fromString(
                Paths.get("uv.lock"),
                content
        );
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input),
                null,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());

        if (parsed.isEmpty() || !(parsed.get(0) instanceof Toml.Document)) {
            return Collections.emptyList();
        }

        Toml.Document doc = (Toml.Document) parsed.get(0);
        return extractPackages(doc);
    }

    /**
     * Two-pass extraction:
     * 1. Create all ResolvedDependency objects (without transitive deps linked)
     * 2. Link transitive dependencies by looking up names in the resolved map
     * <p>
     * Python resolution is flat (one version per package), so each dependency
     * name maps to exactly one ResolvedDependency.
     */
    private static List<ResolvedDependency> extractPackages(Toml.Document doc) {
        // Pass 1: Create all resolved entries and collect raw dependency names per package
        List<ResolvedDependency> resolved = new ArrayList<>();
        Map<String, ResolvedDependency> byNormalizedName = new LinkedHashMap<>();
        Map<String, List<String>> rawDepsPerPackage = new LinkedHashMap<>();

        for (TomlValue value : doc.getValues()) {
            if (!(value instanceof Toml.Table)) {
                continue;
            }
            Toml.Table table = (Toml.Table) value;
            if (table.getName() == null || !"package".equals(table.getName().getName())) {
                continue;
            }

            String name = PythonDependencyParser.getStringValue(table, "name");
            String version = PythonDependencyParser.getStringValue(table, "version");
            if (name == null || version == null) {
                continue;
            }

            String source = extractSource(table);
            List<String> depNames = extractDependencyNames(table);

            ResolvedDependency entry = new ResolvedDependency(name, version, source, null);
            resolved.add(entry);
            byNormalizedName.put(PythonResolutionResult.normalizeName(name), entry);
            if (!depNames.isEmpty()) {
                rawDepsPerPackage.put(PythonResolutionResult.normalizeName(name), depNames);
            }
        }

        // Pass 2: Link transitive dependencies
        List<ResolvedDependency> linked = new ArrayList<>(resolved.size());
        for (ResolvedDependency entry : resolved) {
            String normalizedName = PythonResolutionResult.normalizeName(entry.getName());
            List<String> depNames = rawDepsPerPackage.get(normalizedName);
            if (depNames != null) {
                List<ResolvedDependency> deps = new ArrayList<>();
                for (String depName : depNames) {
                    ResolvedDependency dep = byNormalizedName.get(PythonResolutionResult.normalizeName(depName));
                    if (dep != null) {
                        deps.add(dep);
                    }
                }
                linked.add(entry.withDependencies(deps.isEmpty() ? null : deps));
            } else {
                linked.add(entry);
            }
        }

        return linked;
    }

    /**
     * Extract the source URL/path from a [[package]] table.
     * uv.lock uses inline tables for source with various keys:
     * <ul>
     *   <li>{@code { registry = "https://pypi.org/simple" }} — PyPI or custom index</li>
     *   <li>{@code { editable = "." }} — local editable install</li>
     *   <li>{@code { virtual = "." }} — virtual workspace member</li>
     *   <li>{@code { path = "packages/foo" }} — local path dependency</li>
     *   <li>{@code { git = "https://..." }} — git dependency</li>
     * </ul>
     */
    private static @Nullable String extractSource(Toml.Table packageTable) {
        for (Toml value : packageTable.getValues()) {
            if (!(value instanceof Toml.KeyValue)) {
                continue;
            }
            Toml.KeyValue kv = (Toml.KeyValue) value;
            if (!(kv.getKey() instanceof Toml.Identifier) ||
                    !"source".equals(((Toml.Identifier) kv.getKey()).getName())) {
                continue;
            }
            if (kv.getValue() instanceof Toml.Table) {
                Toml.Table sourceTable = (Toml.Table) kv.getValue();
                // Try each known source type in order of likelihood
                String[] sourceKeys = {"registry", "editable", "virtual", "path", "git"};
                for (String key : sourceKeys) {
                    String val = PythonDependencyParser.getStringValue(sourceTable, key);
                    if (val != null) {
                        return val;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Extract dependency names from a [[package]] table.
     * Dependencies in uv.lock look like:
     * <pre>
     * dependencies = [
     *     { name = "certifi", specifier = ">=2017.4.17" },
     * ]
     * </pre>
     * We only need the names for linking to other ResolvedDependency entries.
     */
    private static List<String> extractDependencyNames(Toml.Table packageTable) {
        for (Toml value : packageTable.getValues()) {
            if (!(value instanceof Toml.KeyValue)) {
                continue;
            }
            Toml.KeyValue kv = (Toml.KeyValue) value;
            if (!(kv.getKey() instanceof Toml.Identifier) ||
                    !"dependencies".equals(((Toml.Identifier) kv.getKey()).getName())) {
                continue;
            }
            if (kv.getValue() instanceof Toml.Array) {
                List<String> names = new ArrayList<>();
                for (Toml item : ((Toml.Array) kv.getValue()).getValues()) {
                    if (item instanceof Toml.Table) {
                        String name = PythonDependencyParser.getStringValue((Toml.Table) item, "name");
                        if (name != null) {
                            names.add(name);
                        }
                    }
                }
                return names;
            }
        }
        return Collections.emptyList();
    }
}
