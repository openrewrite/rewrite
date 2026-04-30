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
                if (scope != null && !isInsideTargetArray(getCursor(), scope, groupName)) {
                    return literal;
                }

                String spec = literal.getValue().toString();
                String packageName = PyProjectHelper.extractPackageName(spec);
                if (packageName == null) {
                    return literal;
                }

                String fixVersion = PythonDependencyFile.getByNormalizedName(u, packageName);
                if (fixVersion == null) {
                    return literal;
                }

                String newSpec = PythonDependencyFile.rewritePep508Spec(spec, packageName, fixVersion);
                if (!newSpec.equals(spec)) {
                    return literal.withSource("\"" + newSpec + "\"").withValue(newSpec);
                }
                return literal;
            }
        }.visitNonNull(doc, upgrades, cursor);
        if (result != doc) {
            PythonResolutionResult updatedMarker = PythonDependencyFile.updateResolvedVersions(marker, upgrades);
            result = result.withMarkers(result.getMarkers().setByType(updatedMarker));
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
            doc = doc.withMarkers(doc.getMarkers().setByType(updatedMarker));
            return new PyProjectFile(new Cursor(cursor.getParentOrThrow(), doc), updatedMarker);
        }
        return this;
    }

    @Override
    public PyProjectFile withRemovedDependencies(Set<String> packageNames, @Nullable String scope, @Nullable String groupName) {
        Set<String> normalizedNames = new HashSet<>();
        for (String name : packageNames) {
            normalizedNames.add(PythonResolutionResult.normalizeName(name));
        }
        Toml.Document doc = (Toml.Document) getTree();
        Toml.Document result = (Toml.Document) new TomlIsoVisitor<Set<String>>() {
            @Override
            public Toml.Array visitArray(Toml.Array array, Set<String> names) {
                Toml.Array a = super.visitArray(array, names);
                if (scope != null && !PyProjectHelper.isInsideDependencyArray(getCursor(), scope, groupName)) {
                    return a;
                }

                List<TomlRightPadded<Toml>> existingPadded = a.getPadding().getValues();
                List<TomlRightPadded<Toml>> newPadded = new ArrayList<>();
                boolean found = false;
                int removedIdx = -1;

                for (int i = 0; i < existingPadded.size(); i++) {
                    TomlRightPadded<Toml> padded = existingPadded.get(i);
                    Toml element = padded.getElement();

                    if (element instanceof Toml.Literal) {
                        Object val = ((Toml.Literal) element).getValue();
                        if (val instanceof String) {
                            String depName = PyProjectHelper.extractPackageName((String) val);
                            if (depName != null && names.contains(PythonResolutionResult.normalizeName(depName))) {
                                if (!found) {
                                    removedIdx = i;
                                }
                                found = true;
                                continue;
                            }
                        }
                    }
                    newPadded.add(padded);
                }

                if (!found) {
                    return a;
                }

                if (removedIdx == 0 && !newPadded.isEmpty()) {
                    TomlRightPadded<Toml> first = newPadded.get(0);
                    if (!(first.getElement() instanceof Toml.Empty)) {
                        Space originalPrefix = existingPadded.get(removedIdx).getElement().getPrefix();
                        newPadded.set(0, first.map(el -> el.withPrefix(originalPrefix)));
                    }
                }

                return a.getPadding().withValues(newPadded);
            }
        }.visitNonNull(doc, normalizedNames, cursor);
        if (result != doc) {
            return new PyProjectFile(new Cursor(cursor.getParentOrThrow(), result), marker);
        }
        return this;
    }

    @Override
    public PyProjectFile withChangedDependency(String oldPackageName, String newPackageName, @Nullable String newVersion, @Nullable String scope, @Nullable String groupName) {
        String normalizedOld = PythonResolutionResult.normalizeName(oldPackageName);
        Toml.Document doc = (Toml.Document) getTree();
        Toml.Document result = (Toml.Document) new TomlIsoVisitor<Integer>() {
            @Override
            public Toml.Literal visitLiteral(Toml.Literal literal, Integer p) {
                if (scope != null && !isInsideTargetArray(getCursor(), scope, groupName)) {
                    return literal;
                }
                if (literal.getType() != TomlType.Primitive.String) {
                    return literal;
                }
                Object val = literal.getValue();
                if (!(val instanceof String)) {
                    return literal;
                }
                String spec = (String) val;
                String depName = PyProjectHelper.extractPackageName(spec);
                if (depName == null || !PythonResolutionResult.normalizeName(depName).equals(normalizedOld)) {
                    return literal;
                }

                String newSpec = buildChangedSpec(spec, depName, newPackageName, newVersion);
                return literal.withSource("\"" + newSpec + "\"").withValue(newSpec);
            }
        }.visitNonNull(doc, 0, cursor);
        if (result != doc) {
            return new PyProjectFile(new Cursor(cursor.getParentOrThrow(), result), marker);
        }
        return this;
    }

    private static String buildChangedSpec(String oldSpec, String oldName, String newName, @Nullable String newVer) {
        StringBuilder sb = new StringBuilder(newName);

        // Preserve extras
        int extrasStart = oldSpec.indexOf('[', oldName.length());
        int extrasEnd = oldSpec.indexOf(']', oldName.length());
        if (extrasStart >= 0 && extrasEnd > extrasStart) {
            sb.append(oldSpec, extrasStart, extrasEnd + 1);
        }

        if (newVer != null) {
            sb.append(PyProjectHelper.normalizeVersionConstraint(newVer));
        } else {
            // Preserve original version constraint
            String remaining = oldSpec.substring(oldName.length()).trim();
            if (remaining.startsWith("[")) {
                int end = remaining.indexOf(']');
                if (end >= 0) {
                    remaining = remaining.substring(end + 1).trim();
                }
            }
            int markerIdx = remaining.indexOf(';');
            String versionPart = markerIdx >= 0 ? remaining.substring(0, markerIdx).trim() : remaining.trim();
            if (!versionPart.isEmpty()) {
                sb.append(versionPart);
            }
        }

        // Preserve environment markers
        int semiIdx = oldSpec.indexOf(';');
        if (semiIdx >= 0) {
            sb.append("; ").append(oldSpec.substring(semiIdx + 1).trim());
        }

        return sb.toString();
    }

    @Override
    public PyProjectFile withPinnedTransitiveDependencies(Map<String, String> pins, @Nullable String scope, @Nullable String groupName) {
        PythonResolutionResult.PackageManager pm = marker.getPackageManager();
        if (pm == PythonResolutionResult.PackageManager.Uv) {
            return pinViaArrayScope(pins, "tool.uv.constraint-dependencies");
        } else if (pm == PythonResolutionResult.PackageManager.Pdm) {
            return pinViaPdmOverrides(pins);
        } else {
            // Fallback: add as direct dependency
            return withAddedDependencies(pins, scope, groupName);
        }
    }

    private PyProjectFile pinViaArrayScope(Map<String, String> pins, String scope) {
        PyProjectFile current = this;
        for (Map.Entry<String, String> entry : pins.entrySet()) {
            PythonResolutionResult.Dependency existing = PyProjectHelper.findDependencyInScope(
                    current.marker, entry.getKey(), scope, null);
            if (existing == null) {
                current = current.withAddedDependencies(
                        Collections.singletonMap(entry.getKey(), entry.getValue()), scope, null);
            } else {
                current = current.withUpgradedVersions(
                        Collections.singletonMap(entry.getKey(), entry.getValue()), scope, null);
            }
        }
        return current;
    }

    private PyProjectFile pinViaPdmOverrides(Map<String, String> pins) {
        Toml.Document doc = (Toml.Document) getTree();
        Toml.Document updated = doc;
        for (Map.Entry<String, String> entry : pins.entrySet()) {
            PythonResolutionResult.Dependency existing = PyProjectHelper.findDependencyInScope(
                    marker, entry.getKey(), "tool.pdm.overrides", null);
            if (existing == null) {
                updated = addPdmOverride(updated, entry.getKey(), entry.getValue());
            } else {
                updated = upgradePdmOverride(updated, entry.getKey(), entry.getValue());
            }
        }
        if (updated != doc) {
            return new PyProjectFile(new Cursor(cursor.getParentOrThrow(), updated), marker);
        }
        return this;
    }

    private static Toml.Document addPdmOverride(Toml.Document doc, String packageName, String version) {
        String normalizedVersion = PyProjectHelper.normalizeVersionConstraint(version);
        return (Toml.Document) new TomlIsoVisitor<Integer>() {
            @Override
            public Toml.Table visitTable(Toml.Table table, Integer p) {
                Toml.Table t = super.visitTable(table, p);
                if (t.getName() == null || !"tool.pdm.overrides".equals(t.getName().getName())) {
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

    private static Toml.Document upgradePdmOverride(Toml.Document doc, String packageName, String version) {
        String normalizedName = PythonResolutionResult.normalizeName(packageName);
        String normalizedVersion = PyProjectHelper.normalizeVersionConstraint(version);
        return (Toml.Document) new TomlIsoVisitor<Integer>() {
            @Override
            public Toml.KeyValue visitKeyValue(Toml.KeyValue keyValue, Integer p) {
                Toml.KeyValue kv = super.visitKeyValue(keyValue, p);
                if (!PyProjectHelper.isInsidePdmOverridesTable(getCursor())) {
                    return kv;
                }
                if (!(kv.getKey() instanceof Toml.Identifier)) {
                    return kv;
                }
                String keyName = ((Toml.Identifier) kv.getKey()).getName();
                if (!PythonResolutionResult.normalizeName(keyName).equals(normalizedName)) {
                    return kv;
                }
                if (!(kv.getValue() instanceof Toml.Literal)) {
                    return kv;
                }
                Toml.Literal literal = (Toml.Literal) kv.getValue();
                if (normalizedVersion.equals(literal.getValue())) {
                    return kv;
                }
                return kv.withValue(literal.withSource("\"" + normalizedVersion + "\"").withValue(normalizedVersion));
            }
        }.visitNonNull(doc, 0);
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
    public PyProjectFile withDependencySearchMarkers(Map<String, String> packageMessages, @Nullable String scope, @Nullable String groupName, ExecutionContext ctx) {
        Toml.Document doc = (Toml.Document) getTree();
        Toml.Document result = (Toml.Document) new TomlIsoVisitor<Map<String, String>>() {
            @Override
            public Toml.Literal visitLiteral(Toml.Literal literal, Map<String, String> msgs) {
                if (scope != null && !isInsideTargetArray(getCursor(), scope, groupName)) {
                    return literal;
                }
                if (literal.getType() != TomlType.Primitive.String) {
                    return literal;
                }
                Object val = literal.getValue();
                if (!(val instanceof String)) {
                    return literal;
                }

                String spec = (String) val;
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
        }.visitNonNull(doc, packageMessages, cursor);
        if (result != doc) {
            return new PyProjectFile(new Cursor(cursor.getParentOrThrow(), result), marker);
        }
        return this;
    }

    private static boolean isInsideTargetArray(Cursor cursor, @Nullable String scope, @Nullable String groupName) {
        try {
            Cursor arrayCursor = cursor.dropParentUntil(v -> v instanceof Toml.Array);
            return PyProjectHelper.isInsideDependencyArray(arrayCursor, scope, groupName);
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public static class Matcher extends SimpleTraitMatcher<PyProjectFile> {
        @Override
        protected @Nullable PyProjectFile test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof Toml.Document) {
                Toml.Document doc = (Toml.Document) value;
                if (doc.getSourcePath().endsWith("pyproject.toml")) {
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
