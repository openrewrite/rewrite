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
import org.openrewrite.python.internal.PyProjectHelper;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;

import java.util.*;

import static org.openrewrite.internal.ListUtils.map;

/**
 * Trait for Python dependency files (pyproject.toml, requirements.txt, etc.).
 * Use {@link org.openrewrite.python.internal.PyProjectHelper#extractPackageName(String)}
 * for PEP 508 package name extraction.
 */

public interface PythonDependencyFile extends Trait<SourceFile> {

    PythonResolutionResult getMarker();

    /**
     * Upgrade version constraints for dependencies in the specified scope.
     *
     * @param upgrades  package name → new version
     * @param scope     the TOML scope, or {@code null} for the default ({@code [project].dependencies})
     * @param groupName required for {@code "project.optional-dependencies"} or {@code "dependency-groups"}
     */
    PythonDependencyFile withUpgradedVersions(Map<String, String> upgrades, @Nullable String scope, @Nullable String groupName);

    /**
     * Add dependencies to the specified scope.
     *
     * @param additions package name → version constraint (e.g. {@code "2.0"} or {@code ">=2.0"})
     * @param scope     the TOML scope (e.g. {@code "project.optional-dependencies"},
     *                  {@code "dependency-groups"}), or {@code null} for the default
     *                  ({@code [project].dependencies})
     * @param groupName required when scope is {@code "project.optional-dependencies"}
     *                  or {@code "dependency-groups"}, otherwise {@code null}
     */
    PythonDependencyFile withAddedDependencies(Map<String, String> additions, @Nullable String scope, @Nullable String groupName);

    /**
     * Pin transitive dependencies using the strategy appropriate for this file's
     * package manager. For pyproject.toml: uv uses {@code [tool.uv].constraint-dependencies},
     * PDM uses {@code [tool.pdm.overrides]}, and other managers add a direct dependency.
     * For requirements.txt: appends the dependency.
     *
     * @param pins package name → version constraint
     */
    PythonDependencyFile withPinnedTransitiveDependencies(Map<String, String> pins);

    /**
     * Remove dependencies from the specified scope.
     *
     * @param packageNames package names to remove
     * @param scope        the TOML scope, or {@code null} for the default ({@code [project].dependencies})
     * @param groupName    required for {@code "project.optional-dependencies"} or {@code "dependency-groups"}
     */
    PythonDependencyFile withRemovedDependencies(Set<String> packageNames, @Nullable String scope, @Nullable String groupName);

    /**
     * Change a dependency to a different package, searching all scopes.
     *
     * @param oldPackageName the current package name
     * @param newPackageName the new package name
     * @param newVersion     optional new version constraint, or {@code null} to preserve the original
     */
    PythonDependencyFile withChangedDependency(String oldPackageName, String newPackageName, @Nullable String newVersion);

    /**
     * Add search result markers for vulnerable dependencies.
     *
     * @param packageMessages package name → vulnerability description message
     */
    PythonDependencyFile withDependencySearchMarkers(Map<String, String> packageMessages, ExecutionContext ctx);

    /**
     * Post-process the modified source file, e.g. regenerate lock files.
     * Called by recipes after a trait method modifies the tree.
     * The default implementation returns the tree unchanged.
     *
     * @param ctx the execution context
     * @return the post-processed source file
     */
    default SourceFile afterModification(ExecutionContext ctx) {
        return getTree();
    }

    /**
     * Rewrite a PEP 508 dependency spec with a new version constraint.
     * Preserves extras and environment markers. The version is normalized
     * via {@link PyProjectHelper#normalizeVersionConstraint(String)},
     * so both {@code "2.31.0"} and {@code ">=2.31.0"} are accepted.
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

        sb.append(PyProjectHelper.normalizeVersionConstraint(newVersion));

        // Preserve environment markers (everything after ';')
        int semiIdx = spec.indexOf(';', nameEnd);
        if (semiIdx >= 0) {
            sb.append(spec.substring(semiIdx));
        }

        return sb.toString();
    }

    /**
     * Look up a value in a map by normalizing the lookup key per PEP 503.
     * This allows callers to pass non-normalized package names.
     */
    static @Nullable String getByNormalizedName(Map<String, String> map, String name) {
        String normalized = PythonResolutionResult.normalizeName(name);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (PythonResolutionResult.normalizeName(entry.getKey()).equals(normalized)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Update the resolved dependency versions in a marker to reflect version changes.
     * Returns the same marker if no changes were needed.
     */
    static PythonResolutionResult updateResolvedVersions(
            PythonResolutionResult marker, Map<String, String> versionUpdates) {
        List<PythonResolutionResult.ResolvedDependency> resolved = marker.getResolvedDependencies();
        List<PythonResolutionResult.ResolvedDependency> updated = map(resolved, dep -> {
            String normalizedName = PythonResolutionResult.normalizeName(dep.getName());
            String newVersion = versionUpdates.get(normalizedName);
            if (newVersion != null && !newVersion.equals(dep.getVersion())) {
                return dep.withVersion(newVersion);
            }
            return dep;
        });
        return updated == resolved ? marker : marker.withResolvedDependencies(updated);
    }

    class Matcher extends SimpleTraitMatcher<PythonDependencyFile> {
        private final RequirementsFile.Matcher reqMatcher = new RequirementsFile.Matcher();
        private final PyProjectFile.Matcher pyprojectMatcher = new PyProjectFile.Matcher();
        private final PipfileFile.Matcher pipfileMatcher = new PipfileFile.Matcher();

        @Override
        protected @Nullable PythonDependencyFile test(Cursor cursor) {
            PythonDependencyFile r = reqMatcher.test(cursor);
            if (r != null) {
                return r;
            }
            r = pyprojectMatcher.test(cursor);
            if (r != null) {
                return r;
            }
            return pipfileMatcher.test(cursor);
        }
    }
}
