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
package org.openrewrite.python;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.python.internal.PyProjectHelper;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.tree.Space;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlRightPadded;
import org.openrewrite.toml.tree.TomlType;

import java.util.*;

import static org.openrewrite.Tree.randomId;

/**
 * Pin a transitive dependency version by adding or upgrading a constraint in the
 * appropriate tool-specific section. The strategy depends on the detected package manager:
 * <ul>
 *   <li><b>uv</b>: uses {@code [tool.uv].constraint-dependencies}</li>
 *   <li><b>PDM</b>: uses {@code [tool.pdm.overrides]}</li>
 *   <li><b>Other/unknown</b>: adds as a direct dependency in {@code [project].dependencies}</li>
 * </ul>
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeTransitiveDependencyVersion extends ScanningRecipe<UpgradeTransitiveDependencyVersion.Accumulator> {

    @Option(displayName = "Package name",
            description = "The PyPI package name of the transitive dependency to pin.",
            example = "certifi")
    String packageName;

    @Option(displayName = "Version",
            description = "The PEP 508 version constraint (e.g., `>=2023.7.22`).",
            example = ">=2023.7.22")
    String version;

    @Override
    public String getDisplayName() {
        return "Upgrade transitive Python dependency version";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", packageName, version);
    }

    @Override
    public String getDescription() {
        return "Pin a transitive dependency version using the appropriate strategy for the " +
                "detected package manager: uv uses `[tool.uv].constraint-dependencies`, " +
                "PDM uses `[tool.pdm.overrides]`, and other managers add a direct dependency.";
    }

    enum Action {
        NONE,
        ADD_CONSTRAINT,
        UPGRADE_CONSTRAINT,
        ADD_PDM_OVERRIDE,
        UPGRADE_PDM_OVERRIDE,
        ADD_DIRECT_DEPENDENCY
    }

    static class Accumulator {
        final Set<String> projectsToUpdate = new HashSet<>();
        final Map<String, String> updatedLockFiles = new HashMap<>();
        final Map<String, Action> actions = new HashMap<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public Toml.Document visitDocument(Toml.Document document, ExecutionContext ctx) {
                if (!document.getSourcePath().toString().endsWith("pyproject.toml")) {
                    return document;
                }
                Optional<PythonResolutionResult> resolution = document.getMarkers()
                        .findFirst(PythonResolutionResult.class);
                if (!resolution.isPresent()) {
                    return document;
                }

                PythonResolutionResult marker = resolution.get();
                String sourcePath = document.getSourcePath().toString();

                // Skip if this is a direct dependency
                if (marker.findDependency(packageName) != null) {
                    return document;
                }

                PythonResolutionResult.PackageManager pm = marker.getPackageManager();
                Action action = null;

                if (pm == PythonResolutionResult.PackageManager.Uv) {
                    // Uv: require resolved deps, use constraint-dependencies
                    if (marker.getResolvedDependency(packageName) == null) {
                        return document;
                    }
                    Dependency existing = PyProjectHelper.findDependencyInScope(
                            marker, packageName, "tool.uv.constraint-dependencies", null);
                    if (existing == null) {
                        action = Action.ADD_CONSTRAINT;
                    } else if (!PyProjectHelper.normalizeVersionConstraint(version).equals(existing.getVersionConstraint())) {
                        action = Action.UPGRADE_CONSTRAINT;
                    }
                } else if (pm == PythonResolutionResult.PackageManager.Pdm) {
                    // PDM: use tool.pdm.overrides
                    Dependency existing = PyProjectHelper.findDependencyInScope(
                            marker, packageName, "tool.pdm.overrides", null);
                    if (existing == null) {
                        action = Action.ADD_PDM_OVERRIDE;
                    } else if (!PyProjectHelper.normalizeVersionConstraint(version).equals(existing.getVersionConstraint())) {
                        action = Action.UPGRADE_PDM_OVERRIDE;
                    }
                } else {
                    // Fallback: add as direct dependency
                    action = Action.ADD_DIRECT_DEPENDENCY;
                }

                if (action == null) {
                    return document;
                }

                acc.actions.put(sourcePath, action);
                acc.projectsToUpdate.add(sourcePath);
                return document;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public Toml.Document visitDocument(Toml.Document document, ExecutionContext ctx) {
                String sourcePath = document.getSourcePath().toString();

                if (sourcePath.endsWith("pyproject.toml") && acc.projectsToUpdate.contains(sourcePath)) {
                    Action action = acc.actions.get(sourcePath);
                    if (action == Action.ADD_CONSTRAINT) {
                        return addToArray(document, ctx, acc, "tool.uv.constraint-dependencies");
                    } else if (action == Action.UPGRADE_CONSTRAINT) {
                        return upgradeConstraint(document, ctx, acc);
                    } else if (action == Action.ADD_PDM_OVERRIDE) {
                        return addPdmOverride(document, ctx, acc);
                    } else if (action == Action.UPGRADE_PDM_OVERRIDE) {
                        return upgradePdmOverride(document, ctx, acc);
                    } else if (action == Action.ADD_DIRECT_DEPENDENCY) {
                        return addToArray(document, ctx, acc, null);
                    }
                }

                if (sourcePath.endsWith("uv.lock")) {
                    String pyprojectPath = PyProjectHelper.correspondingPyprojectPath(sourcePath);
                    String newContent = acc.updatedLockFiles.get(pyprojectPath);
                    if (newContent != null) {
                        return PyProjectHelper.reparseToml(document, newContent);
                    }
                }

                return document;
            }
        };
    }

    private Toml.Document addToArray(Toml.Document document, ExecutionContext ctx, Accumulator acc, @Nullable String scope) {
        String normalizedVersion = PyProjectHelper.normalizeVersionConstraint(version);
        String pep508 = packageName + normalizedVersion;

        Toml.Document updated = (Toml.Document) new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public Toml.Array visitArray(Toml.Array array, ExecutionContext ctx) {
                Toml.Array a = super.visitArray(array, ctx);

                if (!PyProjectHelper.isInsideDependencyArray(getCursor(), scope, null)) {
                    return a;
                }

                Toml.Literal newLiteral = new Toml.Literal(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        TomlType.Primitive.String,
                        "\"" + pep508 + "\"",
                        pep508
                );

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
                        Toml.Literal formattedLiteral = newLiteral.withPrefix(lastRealElement.getPrefix());

                        for (int i = 0; i <= lastRealIdx; i++) {
                            newPadded.add(existingPadded.get(i));
                        }
                        newPadded.add(new TomlRightPadded<>(formattedLiteral, Space.EMPTY, Markers.EMPTY));
                        newPadded.add(lastPadded);
                    } else {
                        Toml lastElement = lastPadded.getElement();
                        Space newPrefix = lastElement.getPrefix().getWhitespace().contains("\n")
                                ? lastElement.getPrefix()
                                : Space.SINGLE_SPACE;
                        Toml.Literal formattedLiteral = newLiteral.withPrefix(newPrefix);

                        for (int i = 0; i < existingPadded.size() - 1; i++) {
                            newPadded.add(existingPadded.get(i));
                        }
                        newPadded.add(lastPadded.withAfter(Space.EMPTY));
                        newPadded.add(new TomlRightPadded<>(formattedLiteral, lastPadded.getAfter(), Markers.EMPTY));
                    }
                }

                return a.getPadding().withValues(newPadded);
            }
        }.visitNonNull(document, ctx);

        if (updated != document) {
            updated = PyProjectHelper.regenerateLockAndRefreshMarker(updated, acc.updatedLockFiles);
        }

        return updated;
    }

    private Toml.Document upgradeConstraint(Toml.Document document, ExecutionContext ctx, Accumulator acc) {
        String normalizedName = PythonResolutionResult.normalizeName(packageName);

        Toml.Document updated = (Toml.Document) new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public Toml.Literal visitLiteral(Toml.Literal literal, ExecutionContext ctx) {
                Toml.Literal l = super.visitLiteral(literal, ctx);
                if (l.getType() != TomlType.Primitive.String) {
                    return l;
                }

                Object val = l.getValue();
                if (!(val instanceof String)) {
                    return l;
                }

                // Check if we're inside [tool.uv].constraint-dependencies
                if (!isInsideConstraintDependencies()) {
                    return l;
                }

                String spec = (String) val;
                String depName = PyProjectHelper.extractPackageName(spec);
                if (depName == null || !PythonResolutionResult.normalizeName(depName).equals(normalizedName)) {
                    return l;
                }

                String extras = UpgradeDependencyVersion.extractExtras(spec);
                String marker = UpgradeDependencyVersion.extractMarker(spec);

                StringBuilder sb = new StringBuilder(depName);
                if (extras != null) {
                    sb.append('[').append(extras).append(']');
                }
                sb.append(PyProjectHelper.normalizeVersionConstraint(version));
                if (marker != null) {
                    sb.append("; ").append(marker);
                }

                String newSpec = sb.toString();
                return l.withSource("\"" + newSpec + "\"").withValue(newSpec);
            }

            private boolean isInsideConstraintDependencies() {
                Cursor c = getCursor();
                while (c != null) {
                    if (c.getValue() instanceof Toml.Array) {
                        return PyProjectHelper.isInsideDependencyArray(c, "tool.uv.constraint-dependencies", null);
                    }
                    c = c.getParent();
                }
                return false;
            }
        }.visitNonNull(document, ctx);

        if (updated != document) {
            updated = PyProjectHelper.regenerateLockAndRefreshMarker(updated, acc.updatedLockFiles);
        }

        return updated;
    }

    private Toml.Document addPdmOverride(Toml.Document document, ExecutionContext ctx, Accumulator acc) {
        Toml.Document updated = (Toml.Document) new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public Toml.Table visitTable(Toml.Table table, ExecutionContext ctx) {
                Toml.Table t = super.visitTable(table, ctx);
                if (t.getName() == null || !"tool.pdm.overrides".equals(t.getName().getName())) {
                    return t;
                }

                // Build a new KeyValue: packageName = "version"
                String normalizedVersion = PyProjectHelper.normalizeVersionConstraint(version);
                Toml.Identifier key = new Toml.Identifier(
                        randomId(), Space.EMPTY, Markers.EMPTY, packageName, packageName);
                Toml.Literal value = new Toml.Literal(
                        randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                        TomlType.Primitive.String, "\"" + normalizedVersion + "\"", normalizedVersion);
                Toml.KeyValue newKv = new Toml.KeyValue(
                        randomId(), Space.EMPTY, Markers.EMPTY,
                        new TomlRightPadded<>(key, Space.SINGLE_SPACE, Markers.EMPTY),
                        value);

                // Determine prefix for new entry
                List<Toml> values = t.getValues();
                Space entryPrefix;
                if (!values.isEmpty()) {
                    entryPrefix = values.get(values.size() - 1).getPrefix();
                } else {
                    entryPrefix = Space.format("\n");
                }
                newKv = newKv.withPrefix(entryPrefix);

                List<Toml> newValues = new ArrayList<>(values);
                newValues.add(newKv);
                return t.withValues(newValues);
            }
        }.visitNonNull(document, ctx);

        if (updated != document) {
            updated = PyProjectHelper.regenerateLockAndRefreshMarker(updated, acc.updatedLockFiles);
        }

        return updated;
    }

    private Toml.Document upgradePdmOverride(Toml.Document document, ExecutionContext ctx, Accumulator acc) {
        String normalizedName = PythonResolutionResult.normalizeName(packageName);

        Toml.Document updated = (Toml.Document) new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public Toml.KeyValue visitKeyValue(Toml.KeyValue keyValue, ExecutionContext ctx) {
                Toml.KeyValue kv = super.visitKeyValue(keyValue, ctx);

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
                String normalizedVersion = PyProjectHelper.normalizeVersionConstraint(version);
                return kv.withValue(literal.withSource("\"" + normalizedVersion + "\"").withValue(normalizedVersion));
            }
        }.visitNonNull(document, ctx);

        if (updated != document) {
            updated = PyProjectHelper.regenerateLockAndRefreshMarker(updated, acc.updatedLockFiles);
        }

        return updated;
    }
}
