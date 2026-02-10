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
import org.openrewrite.marker.Markup;
import org.openrewrite.python.internal.PythonDependencyParser;
import org.openrewrite.python.internal.UvLockRegeneration;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlType;

import java.util.*;

/**
 * Change the version constraint for a dependency in {@code [project].dependencies} in pyproject.toml.
 * When uv is available, the uv.lock file is regenerated to reflect the change.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class ChangeDependencyVersion extends ScanningRecipe<ChangeDependencyVersion.Accumulator> {

    @Option(displayName = "Package name",
            description = "The PyPI package name to update.",
            example = "requests")
    String packageName;

    @Option(displayName = "New version",
            description = "The new PEP 508 version constraint (e.g., `>=2.31.0`).",
            example = ">=2.31.0")
    String newVersion;

    @Override
    public String getDisplayName() {
        return "Change Python dependency version";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", packageName, newVersion);
    }

    @Override
    public String getDescription() {
        return "Change the version constraint for a dependency in [project].dependencies in pyproject.toml. " +
                "When uv is available, the uv.lock file is regenerated.";
    }

    static class Accumulator {
        final Set<String> projectsToUpdate = new HashSet<>();
        final Map<String, String> updatedLockFiles = new HashMap<>();
        final Set<String> failedProjects = new HashSet<>();
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

                // Check if the dependency exists and has a different version
                PythonResolutionResult.Dependency dep = marker.findDependency(packageName);
                if (dep == null) {
                    return document;
                }

                // Skip if the version constraint already matches
                if (newVersion.equals(dep.getVersionConstraint())) {
                    return document;
                }

                acc.projectsToUpdate.add(document.getSourcePath().toString());
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
                    return changeVersionInPyproject(document, ctx, acc);
                }

                if (sourcePath.endsWith("uv.lock")) {
                    String pyprojectPath = correspondingPyprojectPath(sourcePath);
                    String newContent = acc.updatedLockFiles.get(pyprojectPath);
                    if (newContent != null) {
                        return reparseToml(document, newContent);
                    }
                }

                return document;
            }
        };
    }

    private Toml.Document changeVersionInPyproject(Toml.Document document, ExecutionContext ctx, Accumulator acc) {
        String normalizedName = PythonResolutionResult.normalizeName(packageName);
        String sourcePath = document.getSourcePath().toString();

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

                // Check if we're inside [project].dependencies array
                if (!isInsideProjectDependencies()) {
                    return l;
                }

                // Check if this literal matches the package we're looking for
                String spec = (String) val;
                String depName = extractPackageName(spec);
                if (!PythonResolutionResult.normalizeName(depName).equals(normalizedName)) {
                    return l;
                }

                // Build new PEP 508 string preserving extras and markers
                String newSpec = buildNewSpec(spec, depName);
                return l.withSource("\"" + newSpec + "\"").withValue(newSpec);
            }

            private boolean isInsideProjectDependencies() {
                // Walk up the cursor to verify we're inside [project].dependencies
                Cursor c = getCursor();
                boolean inArray = false;
                boolean inDependencies = false;
                boolean inProject = false;

                while (c != null) {
                    Object value = c.getValue();
                    if (value instanceof Toml.Array) {
                        inArray = true;
                    } else if (value instanceof Toml.KeyValue && inArray) {
                        Toml.KeyValue kv = (Toml.KeyValue) value;
                        if (kv.getKey() instanceof Toml.Identifier &&
                                "dependencies".equals(((Toml.Identifier) kv.getKey()).getName())) {
                            inDependencies = true;
                        }
                    } else if (value instanceof Toml.Table && inDependencies) {
                        Toml.Table table = (Toml.Table) value;
                        if (table.getName() != null && "project".equals(table.getName().getName())) {
                            inProject = true;
                            break;
                        }
                    }
                    c = c.getParent();
                }
                return inProject;
            }

            private String buildNewSpec(String oldSpec, String depName) {
                // Parse extras and markers from old spec
                String extras = extractExtras(oldSpec);
                String marker = extractMarker(oldSpec);

                StringBuilder sb = new StringBuilder(depName);
                if (extras != null) {
                    sb.append('[').append(extras).append(']');
                }
                sb.append(newVersion);
                if (marker != null) {
                    sb.append("; ").append(marker);
                }
                return sb.toString();
            }
        }.visitNonNull(document, ctx);

        if (updated != document) {
            // Regenerate lock file
            String pyprojectContent = updated.printAll();
            UvLockRegeneration.Result lockResult = UvLockRegeneration.regenerate(pyprojectContent);
            if (lockResult.isSuccess()) {
                acc.updatedLockFiles.put(sourcePath, lockResult.getLockFileContent());
            } else {
                acc.failedProjects.add(sourcePath);
                updated = Markup.warn(updated, new RuntimeException(
                        "uv lock regeneration failed: " + lockResult.getErrorMessage()));
            }

            // Update the marker
            PythonResolutionResult marker = updated.getMarkers()
                    .findFirst(PythonResolutionResult.class).orElse(null);
            if (marker != null) {
                PythonResolutionResult newMarker = PythonDependencyParser.createMarker(updated, null);
                if (newMarker != null) {
                    updated = updated.withMarkers(updated.getMarkers()
                            .removeByType(PythonResolutionResult.class)
                            .addIfAbsent(newMarker.withId(marker.getId())));
                }
            }
        }

        return updated;
    }

    static String extractPackageName(String pep508Spec) {
        String trimmed = pep508Spec.trim();
        int end = 0;
        while (end < trimmed.length()) {
            char c = trimmed.charAt(end);
            if (c == '[' || c == '>' || c == '<' || c == '=' || c == '!' || c == '~' || c == ';' || c == ' ') {
                break;
            }
            end++;
        }
        return trimmed.substring(0, end).trim();
    }

    static @Nullable String extractExtras(String pep508Spec) {
        int start = pep508Spec.indexOf('[');
        int end = pep508Spec.indexOf(']');
        if (start >= 0 && end > start) {
            return pep508Spec.substring(start + 1, end);
        }
        return null;
    }

    static @Nullable String extractMarker(String pep508Spec) {
        int idx = pep508Spec.indexOf(';');
        if (idx >= 0) {
            String marker = pep508Spec.substring(idx + 1).trim();
            return marker.isEmpty() ? null : marker;
        }
        return null;
    }

    private static String correspondingPyprojectPath(String uvLockPath) {
        if (uvLockPath.contains("/")) {
            return uvLockPath.substring(0, uvLockPath.lastIndexOf('/') + 1) + "pyproject.toml";
        }
        return "pyproject.toml";
    }

    private static Toml.Document reparseToml(Toml.Document original, String newContent) {
        TomlParser parser = new TomlParser();
        Parser.Input input = Parser.Input.fromString(original.getSourcePath(), newContent);
        List<SourceFile> parsed = new ArrayList<>();
        parser.parseInputs(Collections.singletonList(input), null,
                new InMemoryExecutionContext(Throwable::printStackTrace)).forEach(parsed::add);
        if (!parsed.isEmpty() && parsed.get(0) instanceof Toml.Document) {
            Toml.Document newDoc = (Toml.Document) parsed.get(0);
            return newDoc.withId(original.getId())
                    .withMarkers(original.getMarkers());
        }
        return original;
    }
}
