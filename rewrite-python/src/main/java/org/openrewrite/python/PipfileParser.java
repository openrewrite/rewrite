/*
 * Copyright 2026 the original author or authors.
 *
 * Moderne Proprietary. Only for use by Moderne customers under the terms of a commercial contract.
 */
package org.openrewrite.python;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.python.internal.PyProjectHelper;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.PackageManager;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.openrewrite.Tree.randomId;

/**
 * Parser for Pipfile files that delegates to {@link TomlParser} and attaches a
 * {@link PythonResolutionResult} marker with dependency metadata.
 */
public class PipfileParser implements Parser {

    private final TomlParser tomlParser = new TomlParser();

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return tomlParser.parseInputs(sources, relativeTo, ctx).map(sf -> {
            if (!(sf instanceof Toml.Document)) {
                return sf;
            }
            Toml.Document doc = (Toml.Document) sf;
            PythonResolutionResult marker = createMarker(doc);
            if (marker == null) {
                return sf;
            }
            return doc.withMarkers(doc.getMarkers().addIfAbsent(marker));
        });
    }

    static @Nullable PythonResolutionResult createMarker(Toml.Document doc) {
        Map<String, Toml.Table> tables = indexTables(doc);

        Toml.Table packagesTable = tables.get("packages");
        Toml.Table devPackagesTable = tables.get("dev-packages");

        // A Pipfile should have at least one dependency section
        if (packagesTable == null && devPackagesTable == null) {
            return null;
        }

        List<Dependency> dependencies = parseDependencyTable(packagesTable);

        Map<String, List<Dependency>> optionalDependencies = new LinkedHashMap<>();
        List<Dependency> devDeps = parseDependencyTable(devPackagesTable);
        if (!devDeps.isEmpty()) {
            optionalDependencies.put("dev-packages", devDeps);
        }

        Toml.Table requiresTable = tables.get("requires");
        String requiresPython = requiresTable != null ? getStringValue(requiresTable, "python_version") : null;

        return new PythonResolutionResult(
                randomId(),
                null,
                null,
                null,
                null,
                doc.getSourcePath().toString(),
                requiresPython,
                null,
                Collections.emptyList(),
                dependencies,
                optionalDependencies,
                Collections.emptyMap(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                PackageManager.Pipenv,
                null
        );
    }

    private static List<Dependency> parseDependencyTable(Toml.@Nullable Table table) {
        if (table == null) {
            return Collections.emptyList();
        }
        List<Dependency> deps = new ArrayList<>();
        for (Toml value : table.getValues()) {
            if (!(value instanceof Toml.KeyValue)) {
                continue;
            }
            Toml.KeyValue kv = (Toml.KeyValue) value;
            if (!(kv.getKey() instanceof Toml.Identifier)) {
                continue;
            }
            String name = ((Toml.Identifier) kv.getKey()).getName();
            String versionConstraint = extractVersion(kv.getValue());
            if ("*".equals(versionConstraint)) {
                versionConstraint = null;
            }
            deps.add(new Dependency(name, versionConstraint, null, null, null));
        }
        return deps;
    }

    private static @Nullable String extractVersion(Toml value) {
        if (value instanceof Toml.Literal) {
            Object v = ((Toml.Literal) value).getValue();
            return v instanceof String ? (String) v : null;
        }
        if (value instanceof Toml.Table) {
            // Inline table: {version = ">=3.2", ...}
            for (Toml inner : ((Toml.Table) value).getValues()) {
                if (inner instanceof Toml.KeyValue) {
                    Toml.KeyValue innerKv = (Toml.KeyValue) inner;
                    if (innerKv.getKey() instanceof Toml.Identifier &&
                            "version".equals(((Toml.Identifier) innerKv.getKey()).getName())) {
                        return extractVersion(innerKv.getValue());
                    }
                }
            }
        }
        return null;
    }

    private static @Nullable String getStringValue(Toml.Table table, String key) {
        for (Toml value : table.getValues()) {
            if (value instanceof Toml.KeyValue) {
                Toml.KeyValue kv = (Toml.KeyValue) value;
                if (kv.getKey() instanceof Toml.Identifier &&
                        key.equals(((Toml.Identifier) kv.getKey()).getName()) &&
                        kv.getValue() instanceof Toml.Literal) {
                    Object v = ((Toml.Literal) kv.getValue()).getValue();
                    return v instanceof String ? (String) v : null;
                }
            }
        }
        return null;
    }

    private static Map<String, Toml.Table> indexTables(Toml.Document doc) {
        Map<String, Toml.Table> tables = new LinkedHashMap<>();
        for (Toml value : doc.getValues()) {
            if (value instanceof Toml.Table) {
                Toml.Table table = (Toml.Table) value;
                if (table.getName() != null) {
                    tables.put(table.getName().getName(), table);
                }
            }
        }
        return tables;
    }

    @Override
    public boolean accept(Path path) {
        return "Pipfile".equals(path.getFileName().toString());
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("Pipfile");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {

        Builder() {
            super(Toml.Document.class);
        }

        @Override
        public PipfileParser build() {
            return new PipfileParser();
        }

        @Override
        public String getDslName() {
            return "Pipfile";
        }
    }
}
