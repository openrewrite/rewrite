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
import org.openrewrite.python.RequirementsTxtParser;
import org.openrewrite.python.internal.PyProjectHelper;
import org.openrewrite.text.Find;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.text.PlainText;
import org.openrewrite.trait.SimpleTraitMatcher;

import java.util.*;
import java.util.regex.Pattern;

@Value
public class RequirementsFile implements PythonDependencyFile {
    private static final RequirementsTxtParser PARSER = new RequirementsTxtParser();
    private static final Pattern SCOPE_PATTERN = Pattern.compile("requirements(?:-([\\w-]+))?\\.(?:txt|in)");

    Cursor cursor;
    PythonResolutionResult marker;

    /**
     * Check whether this file matches the given scope.
     * <ul>
     *   <li>{@code null} → matches all requirements files</li>
     *   <li>{@code ""} (empty) → matches only {@code requirements.txt} / {@code requirements.in}</li>
     *   <li>{@code "dev"} → matches only {@code requirements-dev.txt} / {@code requirements-dev.in}</li>
     * </ul>
     */
    private boolean matchesScope(@Nullable String scope) {
        if (scope == null) {
            return true;
        }
        String filename = getTree().getSourcePath().getFileName().toString();
        java.util.regex.Matcher m = SCOPE_PATTERN.matcher(filename);
        if (!m.matches()) {
            return false;
        }
        String fileSuffix = m.group(1); // null for requirements.txt, "dev" for requirements-dev.txt
        if (scope.isEmpty()) {
            return fileSuffix == null;
        }
        return scope.equals(fileSuffix);
    }

    @Override
    public RequirementsFile withUpgradedVersions(Map<String, String> upgrades, @Nullable String scope, @Nullable String groupName) {
        if (!matchesScope(scope)) {
            return this;
        }
        PlainText pt = (PlainText) getTree();
        String text = pt.getText();
        String[] lines = text.split("\n", -1);
        boolean changed = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("-")) {
                continue;
            }

            String packageName = PyProjectHelper.extractPackageName(trimmed);
            if (packageName == null) {
                continue;
            }

            String fixVersion = PythonDependencyFile.getByNormalizedName(upgrades, packageName);
            if (fixVersion == null) {
                continue;
            }

            String newSpec = PythonDependencyFile.rewritePep508Spec(trimmed, packageName, fixVersion);
            if (!newSpec.equals(trimmed)) {
                // Preserve leading whitespace from the original line
                int leadingWs = 0;
                while (leadingWs < line.length() && Character.isWhitespace(line.charAt(leadingWs))) {
                    leadingWs++;
                }
                lines[i] = line.substring(0, leadingWs) + newSpec;
                changed = true;
            }
        }

        if (changed) {
            PythonResolutionResult updatedMarker = PythonDependencyFile.updateResolvedVersions(marker, upgrades);
            PlainText newPt = pt.withText(String.join("\n", lines));
            newPt = newPt.withMarkers(newPt.getMarkers().setByType(updatedMarker));
            return new RequirementsFile(new Cursor(cursor.getParentOrThrow(), newPt), updatedMarker);
        }
        return this;
    }

    @Override
    public RequirementsFile withAddedDependencies(Map<String, String> additions, @Nullable String scope, @Nullable String groupName) {
        if (!matchesScope(scope)) {
            return this;
        }
        PlainText pt = (PlainText) getTree();
        String text = pt.getText();
        String[] lines = text.split("\n", -1);

        Set<String> existingPackages = new HashSet<>();
        for (String line : lines) {
            String pkg = PyProjectHelper.extractPackageName(line.trim());
            if (pkg != null) {
                existingPackages.add(PythonResolutionResult.normalizeName(pkg));
            }
        }

        StringBuilder sb = new StringBuilder(text);
        boolean changed = false;
        for (Map.Entry<String, String> entry : additions.entrySet()) {
            if (!existingPackages.contains(PythonResolutionResult.normalizeName(entry.getKey()))) {
                sb.append("\n").append(entry.getKey()).append(PyProjectHelper.normalizeVersionConstraint(entry.getValue()));
                changed = true;
            }
        }

        if (changed) {
            PythonResolutionResult updatedMarker = PythonDependencyFile.updateResolvedVersions(marker, additions);
            PlainText newPt = pt.withText(sb.toString());
            newPt = newPt.withMarkers(newPt.getMarkers().setByType(updatedMarker));
            return new RequirementsFile(new Cursor(cursor.getParentOrThrow(), newPt), updatedMarker);
        }
        return this;
    }

    @Override
    public RequirementsFile withRemovedDependencies(Set<String> packageNames, @Nullable String scope, @Nullable String groupName) {
        if (!matchesScope(scope)) {
            return this;
        }
        Set<String> normalizedNames = new HashSet<>();
        for (String name : packageNames) {
            normalizedNames.add(PythonResolutionResult.normalizeName(name));
        }
        PlainText pt = (PlainText) getTree();
        String[] lines = pt.getText().split("\n", -1);
        List<String> kept = new ArrayList<>();
        boolean changed = false;

        for (String line : lines) {
            String pkg = PyProjectHelper.extractPackageName(line.trim());
            if (pkg != null && normalizedNames.contains(PythonResolutionResult.normalizeName(pkg))) {
                changed = true;
            } else {
                kept.add(line);
            }
        }

        if (changed) {
            PlainText newPt = pt.withText(String.join("\n", kept));
            return new RequirementsFile(new Cursor(cursor.getParentOrThrow(), newPt), marker);
        }
        return this;
    }

    @Override
    public RequirementsFile withChangedDependency(String oldPackageName, String newPackageName, @Nullable String newVersion) {
        PlainText pt = (PlainText) getTree();
        String[] lines = pt.getText().split("\n", -1);
        boolean changed = false;
        String normalizedOld = PythonResolutionResult.normalizeName(oldPackageName);

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            String pkg = PyProjectHelper.extractPackageName(trimmed);
            if (pkg != null && PythonResolutionResult.normalizeName(pkg).equals(normalizedOld)) {
                // Preserve leading whitespace
                int leadingWs = 0;
                while (leadingWs < lines[i].length() && Character.isWhitespace(lines[i].charAt(leadingWs))) {
                    leadingWs++;
                }
                String newSpec;
                if (newVersion != null) {
                    newSpec = newPackageName + PyProjectHelper.normalizeVersionConstraint(newVersion);
                } else {
                    // Replace just the name, keep the rest
                    newSpec = newPackageName + trimmed.substring(pkg.length());
                }
                lines[i] = lines[i].substring(0, leadingWs) + newSpec;
                changed = true;
            }
        }

        if (changed) {
            PlainText newPt = pt.withText(String.join("\n", lines));
            return new RequirementsFile(new Cursor(cursor.getParentOrThrow(), newPt), marker);
        }
        return this;
    }

    @Override
    public RequirementsFile withPinnedTransitiveDependencies(Map<String, String> pins) {
        return withAddedDependencies(pins, null, null);
    }

    @Override
    public RequirementsFile withDependencySearchMarkers(Map<String, String> packageMessages, ExecutionContext ctx) {
        PlainText result = (PlainText) getTree();
        for (Map.Entry<String, String> entry : packageMessages.entrySet()) {
            Find find = new Find(entry.getKey(), null, false, null, null, null, null, null);
            result = (PlainText) find.getVisitor().visitNonNull(result, ctx);
        }
        if (result != getTree()) {
            return new RequirementsFile(new Cursor(cursor.getParentOrThrow(), result), marker);
        }
        return this;
    }

    public static class Matcher extends SimpleTraitMatcher<RequirementsFile> {
        @Override
        protected @Nullable RequirementsFile test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof PlainText) {
                PlainText pt = (PlainText) value;
                if (PARSER.accept(pt.getSourcePath())) {
                    PythonResolutionResult marker = pt.getMarkers()
                            .findFirst(PythonResolutionResult.class).orElse(null);
                    if (marker != null) {
                        return new RequirementsFile(cursor, marker);
                    }
                }
            }
            return null;
        }
    }
}
