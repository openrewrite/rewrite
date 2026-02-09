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
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts dependency metadata from a parsed pyproject.toml AST (Toml.Document).
 */
public class PythonDependencyParser {

    /**
     * Create a PythonResolutionResult marker from a parsed Toml.Document.
     *
     * @param doc        the parsed pyproject.toml document
     * @param relativeTo the base path (used as the path field in the marker)
     * @return the marker, or null if the document has no [project] section
     */
    public static @Nullable PythonResolutionResult createMarker(Toml.Document doc, @Nullable Path relativeTo) {
        Map<String, Toml.Table> tables = indexTables(doc);

        Toml.Table projectTable = tables.get("project");

        String name = getStringValue(projectTable, "name");
        String version = getStringValue(projectTable, "version");
        String description = getStringValue(projectTable, "description");
        String requiresPython = getStringValue(projectTable, "requires-python");

        Toml.Table buildSystemTable = tables.get("build-system");
        String buildBackend = getStringValue(buildSystemTable, "build-backend");
        List<Dependency> buildRequires = getDependencyList(buildSystemTable, "requires");

        List<Dependency> dependencies = getDependencyList(projectTable, "dependencies");
        Map<String, List<Dependency>> optionalDependencies = getOptionalDependencies(tables);

        String path = doc.getSourcePath().toString();

        return new PythonResolutionResult(
                UUID.randomUUID(),
                name,
                version,
                description,
                path,
                requiresPython,
                buildBackend,
                buildRequires,
                dependencies,
                optionalDependencies,
                Collections.emptyList(),
                null,
                null
        );
    }

    private static Map<String, Toml.Table> indexTables(Toml.Document doc) {
        Map<String, Toml.Table> tables = new LinkedHashMap<>();
        for (TomlValue value : doc.getValues()) {
            if (value instanceof Toml.Table) {
                Toml.Table table = (Toml.Table) value;
                Toml.Identifier nameId = table.getName();
                if (nameId != null) {
                    tables.put(nameId.getName(), table);
                }
            }
        }
        return tables;
    }

    static @Nullable String getStringValue(Toml.@Nullable Table table, String key) {
        if (table == null) {
            return null;
        }
        for (Toml value : table.getValues()) {
            if (value instanceof Toml.KeyValue) {
                Toml.KeyValue kv = (Toml.KeyValue) value;
                if (kv.getKey() instanceof Toml.Identifier &&
                    key.equals(((Toml.Identifier) kv.getKey()).getName())) {
                    if (kv.getValue() instanceof Toml.Literal) {
                        Object val = ((Toml.Literal) kv.getValue()).getValue();
                        return val instanceof String ? (String) val : null;
                    }
                }
            }
        }
        return null;
    }

    static List<Dependency> getDependencyList(Toml.@Nullable Table table, String key) {
        if (table == null) {
            return Collections.emptyList();
        }
        for (Toml value : table.getValues()) {
            if (value instanceof Toml.KeyValue) {
                Toml.KeyValue kv = (Toml.KeyValue) value;
                if (kv.getKey() instanceof Toml.Identifier &&
                    key.equals(((Toml.Identifier) kv.getKey()).getName())) {
                    if (kv.getValue() instanceof Toml.Array) {
                        return parseDependencyArray((Toml.Array) kv.getValue());
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private static List<Dependency> parseDependencyArray(Toml.Array array) {
        List<Dependency> deps = new ArrayList<>();
        for (Toml item : array.getValues()) {
            if (item instanceof Toml.Literal) {
                Object val = ((Toml.Literal) item).getValue();
                if (val instanceof String) {
                    Dependency dep = parsePep508((String) val);
                    if (dep != null) {
                        deps.add(dep);
                    }
                }
            }
        }
        return deps;
    }

    private static Map<String, List<Dependency>> getOptionalDependencies(Map<String, Toml.Table> tables) {
        // Optional dependencies can be in a [project.optional-dependencies] table
        Toml.Table optDepsTable = tables.get("project.optional-dependencies");
        if (optDepsTable != null) {
            return parseOptionalDependenciesFromTable(optDepsTable);
        }

        // Or as a sub-table within [project]
        Toml.Table projectTable = tables.get("project");
        if (projectTable != null) {
            for (Toml value : projectTable.getValues()) {
                if (value instanceof Toml.KeyValue) {
                    Toml.KeyValue kv = (Toml.KeyValue) value;
                    if (kv.getKey() instanceof Toml.Identifier &&
                        "optional-dependencies".equals(((Toml.Identifier) kv.getKey()).getName())) {
                        if (kv.getValue() instanceof Toml.Table) {
                            return parseOptionalDependenciesFromTable((Toml.Table) kv.getValue());
                        }
                    }
                }
            }
        }

        return Collections.emptyMap();
    }

    private static Map<String, List<Dependency>> parseOptionalDependenciesFromTable(Toml.Table table) {
        Map<String, List<Dependency>> result = new LinkedHashMap<>();
        for (Toml value : table.getValues()) {
            if (value instanceof Toml.KeyValue) {
                Toml.KeyValue kv = (Toml.KeyValue) value;
                if (kv.getKey() instanceof Toml.Identifier && kv.getValue() instanceof Toml.Array) {
                    String groupName = ((Toml.Identifier) kv.getKey()).getName();
                    List<Dependency> deps = parseDependencyArray((Toml.Array) kv.getValue());
                    result.put(groupName, deps);
                }
            }
        }
        return result;
    }

    // PEP 508 parsing: name[extras](version_constraint);marker
    // Examples:
    //   requests
    //   requests>=2.28.0
    //   requests[security]>=2.28.0
    //   requests>=2.28.0; python_version>='3.8'
    //   requests[security,socks]>=2.28.0,<3.0; python_version>='3.8'

    private static final Pattern PEP_508_PATTERN = Pattern.compile(
            "^\\s*" +
            "([A-Za-z0-9](?:[A-Za-z0-9._-]*[A-Za-z0-9])?)" +  // name
            "(?:\\s*\\[([^\\]]+)\\])?" +                         // extras (optional)
            "(?:\\s*([^;]+?))?" +                                // version constraint (optional)
            "(?:\\s*;\\s*(.+))?" +                               // marker (optional)
            "\\s*$"
    );

    static @Nullable Dependency parsePep508(String spec) {
        Matcher m = PEP_508_PATTERN.matcher(spec);
        if (!m.matches()) {
            return null;
        }

        String name = m.group(1);
        String extrasStr = m.group(2);
        String versionConstraint = m.group(3);
        String marker = m.group(4);

        List<String> extras = null;
        if (extrasStr != null) {
            extras = new ArrayList<>();
            for (String extra : extrasStr.split(",")) {
                extras.add(extra.trim());
            }
        }

        if (versionConstraint != null) {
            versionConstraint = versionConstraint.trim();
            if (versionConstraint.isEmpty()) {
                versionConstraint = null;
            }
        }

        if (marker != null) {
            marker = marker.trim();
            if (marker.isEmpty()) {
                marker = null;
            }
        }

        return new Dependency(name, versionConstraint, extras, marker, null);
    }
}
