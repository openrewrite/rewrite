/*
 * Copyright 2026 the original author or authors.
 *
 * Moderne Proprietary. Only for use by Moderne customers under the terms of a commercial contract.
 */
package org.openrewrite.python.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.python.internal.PyProjectHelper;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.tree.Space;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlRightPadded;
import org.openrewrite.toml.tree.TomlType;
import org.openrewrite.trait.SimpleTraitMatcher;

import java.util.*;

import static org.openrewrite.internal.ListUtils.concat;
import static org.openrewrite.internal.ListUtils.map;

/**
 * Trait implementation for Pipfile dependency files.
 * Pipfile uses key-value tables: {@code [packages]} for production and
 * {@code [dev-packages]} for development dependencies.
 */
@Value
public class PipfileFile implements PythonDependencyFile {

    Cursor cursor;
    PythonResolutionResult marker;

    @Override
    public PipfileFile withUpgradedVersions(Map<String, String> upgrades, @Nullable String scope, @Nullable String groupName) {
        Toml.Document doc = (Toml.Document) getTree();
        Toml.Document result = (Toml.Document) new TomlIsoVisitor<Map<String, String>>() {
            @Override
            public Toml.KeyValue visitKeyValue(Toml.KeyValue keyValue, Map<String, String> u) {
                Toml.KeyValue kv = super.visitKeyValue(keyValue, u);
                if (!isInsideTargetTable(getCursor(), scope)) {
                    return kv;
                }
                if (!(kv.getKey() instanceof Toml.Identifier)) {
                    return kv;
                }
                String pkgName = ((Toml.Identifier) kv.getKey()).getName();
                String newVersion = PythonDependencyFile.getByNormalizedName(u, pkgName);
                if (newVersion == null) {
                    return kv;
                }
                return updateKeyValueVersion(kv, newVersion);
            }
        }.visitNonNull(doc, upgrades, cursor);
        if (result != doc) {
            PythonResolutionResult updatedMarker = PythonDependencyFile.updateResolvedVersions(marker, upgrades);
            result = result.withMarkers(result.getMarkers().setByType(updatedMarker));
            return new PipfileFile(new Cursor(cursor.getParentOrThrow(), result), updatedMarker);
        }
        return this;
    }

    @Override
    public PipfileFile withAddedDependencies(Map<String, String> additions, @Nullable String scope, @Nullable String groupName) {
        String tableName = resolveTableName(scope);
        Toml.Document doc = (Toml.Document) getTree();
        Toml.Document original = doc;
        for (Map.Entry<String, String> entry : additions.entrySet()) {
            String normalizedName = PythonResolutionResult.normalizeName(entry.getKey());
            if (!hasDependencyInTable(doc, tableName, normalizedName)) {
                doc = addToTable(doc, tableName, entry.getKey(), entry.getValue());
            }
        }
        if (doc != original) {
            PythonResolutionResult updatedMarker = PythonDependencyFile.updateResolvedVersions(marker, additions);
            doc = doc.withMarkers(doc.getMarkers().setByType(updatedMarker));
            return new PipfileFile(new Cursor(cursor.getParentOrThrow(), doc), updatedMarker);
        }
        return this;
    }

    @Override
    public PipfileFile withRemovedDependencies(Set<String> packageNames, @Nullable String scope, @Nullable String groupName) {
        Set<String> normalizedNames = new HashSet<>();
        for (String name : packageNames) {
            normalizedNames.add(PythonResolutionResult.normalizeName(name));
        }
        Toml.Document doc = (Toml.Document) getTree();
        Toml.Document result = (Toml.Document) new TomlIsoVisitor<Set<String>>() {
            @Override
            public Toml.Table visitTable(Toml.Table table, Set<String> names) {
                Toml.Table t = super.visitTable(table, names);
                if (!isTargetTable(t, scope)) {
                    return t;
                }
                List<Toml> newValues = map(t.getValues(), value -> {
                    if (value instanceof Toml.KeyValue) {
                        Toml.KeyValue kv = (Toml.KeyValue) value;
                        if (kv.getKey() instanceof Toml.Identifier) {
                            String keyName = ((Toml.Identifier) kv.getKey()).getName();
                            if (names.contains(PythonResolutionResult.normalizeName(keyName))) {
                                return null;
                            }
                        }
                    }
                    return value;
                });
                return t.withValues(newValues);
            }
        }.visitNonNull(doc, normalizedNames, cursor);
        if (result != doc) {
            return new PipfileFile(new Cursor(cursor.getParentOrThrow(), result), marker);
        }
        return this;
    }

    @Override
    public PipfileFile withChangedDependency(String oldPackageName, String newPackageName, @Nullable String newVersion) {
        String normalizedOld = PythonResolutionResult.normalizeName(oldPackageName);
        Toml.Document doc = (Toml.Document) getTree();
        Toml.Document result = (Toml.Document) new TomlIsoVisitor<Integer>() {
            @Override
            public Toml.KeyValue visitKeyValue(Toml.KeyValue keyValue, Integer p) {
                Toml.KeyValue kv = super.visitKeyValue(keyValue, p);
                if (!(kv.getKey() instanceof Toml.Identifier)) {
                    return kv;
                }
                String keyName = ((Toml.Identifier) kv.getKey()).getName();
                if (!PythonResolutionResult.normalizeName(keyName).equals(normalizedOld)) {
                    return kv;
                }
                // Check we're inside [packages] or [dev-packages]
                if (!isInsideTargetTable(getCursor(), null)) {
                    return kv;
                }
                Toml.Identifier newKey = ((Toml.Identifier) kv.getKey())
                        .withName(newPackageName)
                        .withSource(newPackageName);
                kv = kv.getPadding().withKey(kv.getPadding().getKey().withElement(newKey));
                if (newVersion != null) {
                    kv = updateKeyValueVersion(kv, newVersion);
                }
                return kv;
            }
        }.visitNonNull(doc, 0, cursor);
        if (result != doc) {
            return new PipfileFile(new Cursor(cursor.getParentOrThrow(), result), marker);
        }
        return this;
    }

    @Override
    public PipfileFile withPinnedTransitiveDependencies(Map<String, String> pins) {
        // Pipfile has no constraint mechanism — add to [packages]
        return withAddedDependencies(pins, "packages", null);
    }

    @Override
    public PipfileFile withDependencySearchMarkers(Map<String, String> packageMessages, ExecutionContext ctx) {
        Toml.Document doc = (Toml.Document) getTree();
        Toml.Document result = (Toml.Document) new TomlIsoVisitor<Map<String, String>>() {
            @Override
            public Toml.KeyValue visitKeyValue(Toml.KeyValue keyValue, Map<String, String> msgs) {
                Toml.KeyValue kv = super.visitKeyValue(keyValue, msgs);
                if (!isInsideTargetTable(getCursor(), null)) {
                    return kv;
                }
                if (!(kv.getKey() instanceof Toml.Identifier)) {
                    return kv;
                }
                String pkgName = ((Toml.Identifier) kv.getKey()).getName();
                String message = PythonDependencyFile.getByNormalizedName(msgs, pkgName);
                if (message != null) {
                    return SearchResult.found(kv, message);
                }
                return kv;
            }
        }.visitNonNull(doc, packageMessages, cursor);
        if (result != doc) {
            return new PipfileFile(new Cursor(cursor.getParentOrThrow(), result), marker);
        }
        return this;
    }

    // region Helpers

    /**
     * Resolve the target table name from the scope parameter.
     * {@code null} defaults to "packages".
     */
    private static String resolveTableName(@Nullable String scope) {
        if (scope == null || scope.isEmpty() || "packages".equals(scope)) {
            return "packages";
        }
        return scope;
    }

    /**
     * Check if a table matches the target scope.
     * When scope is null, matches both "packages" and "dev-packages".
     */
    private static boolean isTargetTable(Toml.Table table, @Nullable String scope) {
        if (table.getName() == null) {
            return false;
        }
        String name = table.getName().getName();
        if (scope == null) {
            return "packages".equals(name) || "dev-packages".equals(name);
        }
        return resolveTableName(scope).equals(name);
    }

    /**
     * Check if the cursor is inside a target Pipfile table.
     * When scope is null, matches both "packages" and "dev-packages".
     */
    private static boolean isInsideTargetTable(Cursor cursor, @Nullable String scope) {
        Toml.Table table = cursor.firstEnclosing(Toml.Table.class);
        return table != null && isTargetTable(table, scope);
    }

    private static boolean hasDependencyInTable(Toml.Document doc, String tableName, String normalizedName) {
        for (Toml value : doc.getValues()) {
            if (value instanceof Toml.Table) {
                Toml.Table table = (Toml.Table) value;
                if (table.getName() != null && tableName.equals(table.getName().getName())) {
                    for (Toml entry : table.getValues()) {
                        if (entry instanceof Toml.KeyValue) {
                            Toml.KeyValue kv = (Toml.KeyValue) entry;
                            if (kv.getKey() instanceof Toml.Identifier &&
                                    PythonResolutionResult.normalizeName(
                                            ((Toml.Identifier) kv.getKey()).getName()).equals(normalizedName)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static Toml.Document addToTable(Toml.Document doc, String tableName, String packageName, String version) {
        String normalizedVersion = PyProjectHelper.normalizeVersionConstraint(version);
        return (Toml.Document) new TomlIsoVisitor<Integer>() {
            @Override
            public Toml.Table visitTable(Toml.Table table, Integer p) {
                Toml.Table t = super.visitTable(table, p);
                if (t.getName() == null || !tableName.equals(t.getName().getName())) {
                    return t;
                }

                Toml.Identifier key = new Toml.Identifier(
                        Tree.randomId(), Space.EMPTY, Markers.EMPTY, packageName, packageName);
                Toml.Literal value = new Toml.Literal(
                        Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                        TomlType.Primitive.String, "\"" + normalizedVersion + "\"", normalizedVersion);
                Toml.KeyValue newKv = new Toml.KeyValue(
                        Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                        new TomlRightPadded<>(key, Space.SINGLE_SPACE, Markers.EMPTY),
                        value);

                List<Toml> values = t.getValues();
                Space entryPrefix = !values.isEmpty()
                        ? values.get(values.size() - 1).getPrefix()
                        : Space.format("\n");
                newKv = newKv.withPrefix(entryPrefix);

                return t.withValues(concat(values, newKv));
            }
        }.visitNonNull(doc, 0);
    }

    /**
     * Update the version in a key-value pair, handling both simple literals
     * ({@code requests = ">=2.28.0"}) and inline tables
     * ({@code django = {version = ">=3.2", extras = ["postgres"]}}).
     */
    private static Toml.KeyValue updateKeyValueVersion(Toml.KeyValue kv, String newVersion) {
        String normalizedVersion = PyProjectHelper.normalizeVersionConstraint(newVersion);
        if (kv.getValue() instanceof Toml.Literal) {
            Toml.Literal literal = (Toml.Literal) kv.getValue();
            return kv.withValue(literal.withSource("\"" + normalizedVersion + "\"").withValue(normalizedVersion));
        }
        if (kv.getValue() instanceof Toml.Table) {
            // Inline table: update the "version" key inside
            Toml.Table inlineTable = (Toml.Table) kv.getValue();
            return kv.withValue(inlineTable.withValues(map(inlineTable.getValues(), inner -> {
                if (inner instanceof Toml.KeyValue) {
                    Toml.KeyValue innerKv = (Toml.KeyValue) inner;
                    if (innerKv.getKey() instanceof Toml.Identifier &&
                            "version".equals(((Toml.Identifier) innerKv.getKey()).getName()) &&
                            innerKv.getValue() instanceof Toml.Literal) {
                        Toml.Literal literal = (Toml.Literal) innerKv.getValue();
                        return innerKv.withValue(
                                literal.withSource("\"" + normalizedVersion + "\"").withValue(normalizedVersion));
                    }
                }
                return inner;
            })));
        }
        return kv;
    }

    // endregion

    public static class Matcher extends SimpleTraitMatcher<PipfileFile> {
        @Override
        protected @Nullable PipfileFile test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof Toml.Document) {
                Toml.Document doc = (Toml.Document) value;
                if (doc.getSourcePath().endsWith("Pipfile")) {
                    PythonResolutionResult marker = doc.getMarkers()
                            .findFirst(PythonResolutionResult.class).orElse(null);
                    if (marker != null) {
                        return new PipfileFile(cursor, marker);
                    }
                }
            }
            return null;
        }
    }
}
