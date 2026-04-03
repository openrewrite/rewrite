/*
 * Copyright 2026 the original author or authors.
 *
 * Moderne Proprietary. Only for use by Moderne customers under the terms of a commercial contract.
 */
package org.openrewrite.python.trait;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Trait for Python dependency files (pyproject.toml, requirements.txt, etc.).
 * Use {@link org.openrewrite.python.internal.PyProjectHelper#extractPackageName(String)}
 * for PEP 508 package name extraction.
 */

public interface PythonDependencyFile extends Trait<SourceFile> {

    PythonResolutionResult getMarker();

    PythonDependencyFile withUpgradedVersions(Map<String, String> upgrades);

    PythonDependencyFile withAddedDependencies(Map<String, String> additions);

    /**
     * Add search result markers for vulnerable dependencies.
     *
     * @param packageMessages normalized package name → vulnerability description message
     */
    PythonDependencyFile withDependencySearchMarkers(Map<String, String> packageMessages, ExecutionContext ctx);

    /**
     * Rewrite a PEP 508 dependency spec to use a new minimum version.
     * Preserves extras and environment markers.
     */
    static String rewritePep508Spec(String spec, String packageName, String newVersion) {
        int nameEnd = packageName.length();
        StringBuilder sb = new StringBuilder(packageName);

        // Preserve extras like [security]
        if (nameEnd < spec.length() && spec.charAt(nameEnd) == '[') {
            int extrasEnd = spec.indexOf(']', nameEnd);
            if (extrasEnd >= 0) {
                extrasEnd++;
                sb.append(spec, nameEnd, extrasEnd);
                nameEnd = extrasEnd;
            }
        }

        sb.append(">=").append(newVersion);

        // Preserve environment markers (everything after ';')
        int semiIdx = spec.indexOf(';', nameEnd);
        if (semiIdx >= 0) {
            sb.append(spec.substring(semiIdx));
        }

        return sb.toString();
    }

    /**
     * Update the resolved dependency versions in a marker to reflect version changes.
     * Returns the same marker if no changes were needed.
     */
    static PythonResolutionResult updateResolvedVersions(
            PythonResolutionResult marker, Map<String, String> versionUpdates) {
        List<PythonResolutionResult.ResolvedDependency> resolved = marker.getResolvedDependencies();
        List<PythonResolutionResult.ResolvedDependency> updated = new ArrayList<>(resolved.size());
        boolean changed = false;
        for (PythonResolutionResult.ResolvedDependency dep : resolved) {
            String normalizedName = PythonResolutionResult.normalizeName(dep.getName());
            String newVersion = versionUpdates.get(normalizedName);
            if (newVersion != null && !newVersion.equals(dep.getVersion())) {
                updated.add(dep.withVersion(newVersion));
                changed = true;
            } else {
                updated.add(dep);
            }
        }
        return changed ? marker.withResolvedDependencies(updated) : marker;
    }

    class Matcher extends SimpleTraitMatcher<PythonDependencyFile> {
        private final RequirementsFile.Matcher reqMatcher = new RequirementsFile.Matcher();
        private final PyProjectFile.Matcher tomlMatcher = new PyProjectFile.Matcher();

        @Override
        protected @Nullable PythonDependencyFile test(Cursor cursor) {
            PythonDependencyFile r = reqMatcher.test(cursor);
            return r != null ? r : tomlMatcher.test(cursor);
        }
    }
}
