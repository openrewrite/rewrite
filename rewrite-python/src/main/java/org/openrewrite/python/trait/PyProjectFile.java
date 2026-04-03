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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Value
public class PyProjectFile implements PythonDependencyFile {

    Cursor cursor;
    PythonResolutionResult marker;

    @Override
    public PyProjectFile withUpgradedVersions(Map<String, String> upgrades, @Nullable String scope, @Nullable String groupName) {
        Toml.Document doc = (Toml.Document) getTree();
        Toml.Document result = (Toml.Document) new TomlIsoVisitor<Map<String, String>>() {
            @Override
            public Toml.Literal visitLiteral(Toml.Literal literal, Map<String, String> u) {
                if (!isInsideTargetArray(getCursor(), scope, groupName)) {
                    return literal;
                }

                String spec = literal.getValue().toString();
                String packageName = PyProjectHelper.extractPackageName(spec);
                if (packageName == null) {
                    return literal;
                }

                String normalizedName = PythonResolutionResult.normalizeName(packageName);
                String fixVersion = u.get(normalizedName);
                if (fixVersion == null) {
                    return literal;
                }

                String newSpec = PythonDependencyFile.rewritePep508Spec(spec, packageName, fixVersion);
                if (!newSpec.equals(spec)) {
                    return literal.withSource("\"" + newSpec + "\"").withValue(newSpec);
                }
                return literal;
            }
        }.visitNonNull(doc, upgrades);
        if (result != doc) {
            PythonResolutionResult updatedMarker = PythonDependencyFile.updateResolvedVersions(marker, upgrades);
            result = result.withMarkers(result.getMarkers()
                    .removeByType(PythonResolutionResult.class)
                    .addIfAbsent(updatedMarker));
            return new PyProjectFile(new Cursor(cursor.getParentOrThrow(), result), updatedMarker);
        }
        return this;
    }

    @Override
    public PyProjectFile withAddedDependencies(Map<String, String> additions, @Nullable String scope, @Nullable String groupName) {
        Toml.Document doc = (Toml.Document) getTree();
        Toml.Document original = doc;
        for (Map.Entry<String, String> entry : additions.entrySet()) {
            if (PyProjectHelper.findDependencyInScope(marker, entry.getKey(), scope, groupName) == null) {
                String pep508 = entry.getKey() + PyProjectHelper.normalizeVersionConstraint(entry.getValue());
                doc = addDependencyToArray(doc, pep508, scope, groupName);
            }
        }
        if (doc != original) {
            PythonResolutionResult updatedMarker = PythonDependencyFile.updateResolvedVersions(marker, additions);
            doc = doc.withMarkers(doc.getMarkers()
                    .removeByType(PythonResolutionResult.class)
                    .addIfAbsent(updatedMarker));
            return new PyProjectFile(new Cursor(cursor.getParentOrThrow(), doc), updatedMarker);
        }
        return this;
    }

    private static Toml.Document addDependencyToArray(Toml.Document d, String pep508,
                                                       @Nullable String scope, @Nullable String groupName) {
        return (Toml.Document) new TomlIsoVisitor<Integer>() {
            @Override
            public Toml.Array visitArray(Toml.Array array, Integer p) {
                Toml.Array a = super.visitArray(array, p);
                if (!PyProjectHelper.isInsideDependencyArray(getCursor(), scope, groupName)) {
                    return a;
                }

                Toml.Literal newLiteral = new Toml.Literal(
                        Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                        TomlType.Primitive.String, "\"" + pep508 + "\"", pep508);

                List<TomlRightPadded<Toml>> existingPadded = a.getPadding().getValues();
                List<TomlRightPadded<Toml>> newPadded = new ArrayList<>();

                boolean isEmpty = existingPadded.size() == 1 &&
                        existingPadded.get(0).getElement() instanceof Toml.Empty;
                if (existingPadded.isEmpty() || isEmpty) {
                    newPadded.add(new TomlRightPadded<>(newLiteral, Space.EMPTY, Markers.EMPTY));
                } else {
                    TomlRightPadded<Toml> lastPadded = existingPadded.get(existingPadded.size() - 1);
                    boolean hasTrailingComma = lastPadded.getElement() instanceof Toml.Empty;

                    if (hasTrailingComma) {
                        int lastRealIdx = existingPadded.size() - 2;
                        Toml lastRealElement = existingPadded.get(lastRealIdx).getElement();
                        Toml.Literal formatted = newLiteral.withPrefix(lastRealElement.getPrefix());
                        for (int i = 0; i <= lastRealIdx; i++) {
                            newPadded.add(existingPadded.get(i));
                        }
                        newPadded.add(new TomlRightPadded<>(formatted, Space.EMPTY, Markers.EMPTY));
                        newPadded.add(lastPadded);
                    } else {
                        Toml lastElement = lastPadded.getElement();
                        Space newPrefix = lastElement.getPrefix().getWhitespace().contains("\n") ?
                                lastElement.getPrefix() :
                                Space.SINGLE_SPACE;
                        Toml.Literal formatted = newLiteral.withPrefix(newPrefix);
                        for (int i = 0; i < existingPadded.size() - 1; i++) {
                            newPadded.add(existingPadded.get(i));
                        }
                        newPadded.add(lastPadded.withAfter(Space.EMPTY));
                        newPadded.add(new TomlRightPadded<>(formatted, lastPadded.getAfter(), Markers.EMPTY));
                    }
                }

                return a.getPadding().withValues(newPadded);
            }
        }.visitNonNull(d, 0);
    }

    @Override
    public PyProjectFile withDependencySearchMarkers(Map<String, String> packageMessages, ExecutionContext ctx) {
        Toml.Document doc = (Toml.Document) getTree();
        Toml.Document result = (Toml.Document) new TomlIsoVisitor<Map<String, String>>() {
            @Override
            public Toml.Literal visitLiteral(Toml.Literal literal, Map<String, String> msgs) {
                if (!isInsideTargetArray(getCursor(), null, null)) {
                    return literal;
                }

                String spec = literal.getValue().toString();
                String packageName = PyProjectHelper.extractPackageName(spec);
                if (packageName == null) {
                    return literal;
                }

                String normalizedName = PythonResolutionResult.normalizeName(packageName);
                String message = msgs.get(normalizedName);
                if (message != null) {
                    return SearchResult.found(literal, message);
                }
                return literal;
            }
        }.visitNonNull(doc, packageMessages);
        if (result != doc) {
            return new PyProjectFile(new Cursor(cursor.getParentOrThrow(), result), marker);
        }
        return this;
    }

    private static boolean isInsideTargetArray(Cursor cursor, @Nullable String scope, @Nullable String groupName) {
        Cursor c = cursor;
        while (c != null) {
            if (c.getValue() instanceof Toml.Array) {
                return PyProjectHelper.isInsideDependencyArray(c, scope, groupName);
            }
            c = c.getParent();
        }
        return false;
    }

    public static class Matcher extends SimpleTraitMatcher<PyProjectFile> {
        @Override
        protected @Nullable PyProjectFile test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof Toml.Document) {
                Toml.Document doc = (Toml.Document) value;
                if (doc.getSourcePath().toString().endsWith("pyproject.toml")) {
                    PythonResolutionResult marker = doc.getMarkers()
                            .findFirst(PythonResolutionResult.class).orElse(null);
                    if (marker != null) {
                        return new PyProjectFile(cursor, marker);
                    }
                }
            }
            return null;
        }
    }
}
