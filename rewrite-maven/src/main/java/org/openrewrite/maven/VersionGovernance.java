/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.ManagedDependency;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedManagedDependency;

/**
 * Editable governance over a managed coordinate's resolved version: a {@code <dependencyManagement>} entry or an
 * imported BOM that can be changed in place within the project source set. {@link #of} returns {@code null} for
 * governance that cannot be edited there, so a caller can fall back (for example, pin the coordinate locally)
 * rather than claim an edit against a pom that never changes.
 * <p>
 * A detached value snapshot: it does not retain the {@link MavenResolutionResult} it reads, so it is safe to hold
 * across a run.
 */
@Value
public class VersionGovernance {
    public enum Kind {
        /** A plain {@code <dependencyManagement>} entry declared in an editable pom; edit it in place. */
        LOCAL_MANAGED,
        /** The version comes from an imported BOM declared in an editable pom; raise the BOM instead. */
        BOM_IMPORT
    }

    Kind kind;

    /** The effective managed version (already expanded, never a raw {@code ${...}}). */
    String managedVersion;

    /** The BOM supplying the entry. Non-null iff {@link #kind} is {@link Kind#BOM_IMPORT}. */
    @Nullable
    ResolvedGroupArtifactVersion bomGav;

    /**
     * The GAV of the project-set pom that declares the governing entry - the pom an in-place edit must land in.
     * For {@link Kind#BOM_IMPORT} this is the pom declaring the {@code <scope>import</scope>}, not the BOM itself.
     */
    ResolvedGroupArtifactVersion governingPom;

    /**
     * The editable governance for {@code ga} in {@code mrr}: {@link Kind#BOM_IMPORT} when the winning managed
     * entry comes from an imported BOM, {@link Kind#LOCAL_MANAGED} for a plain entry. {@code null} - so a caller
     * should fall back - when {@code ga} is unmanaged, or the governance is not editable: it lives only in an
     * external parent (outside the {@code parentPomIsProjectPom} chain), it comes from an active profile rather
     * than the base {@code <dependencyManagement>}, or its version is a property defined only in an external
     * parent ({@link #versionEditable}) - none of which can be changed within the source set.
     */
    public static @Nullable VersionGovernance of(MavenResolutionResult mrr, GroupArtifact ga) {
        ResolvedManagedDependency effective = null;
        for (ResolvedManagedDependency md : mrr.getPom().getDependencyManagement()) {
            if (md.getVersion() != null &&
                    ga.getGroupId().equals(md.getGroupId()) && ga.getArtifactId().equals(md.getArtifactId())) {
                effective = md;
                break;
            }
        }
        if (effective == null) {
            return null;
        }
        ResolvedGroupArtifactVersion bomGav = effective.getBomGav();
        if (bomGav != null) {
            ResolvedGroupArtifactVersion governingPom = editableDeclaration(mrr, bomGav.asGroupArtifact(), true);
            return governingPom == null ? null :
                    new VersionGovernance(Kind.BOM_IMPORT, effective.getVersion(), bomGav, governingPom);
        }
        ResolvedGroupArtifactVersion governingPom = editableDeclaration(mrr, ga, false);
        return governingPom == null ? null :
                new VersionGovernance(Kind.LOCAL_MANAGED, effective.getVersion(), null, governingPom);
    }

    /**
     * The GAV of the pom - {@code mrr} or a project-set parent - that declares an editable governing entry for
     * {@code ga}: a BOM import ({@code wantImport}) or a plain entry whose version is also editable there (see
     * {@link #versionEditable}). {@code null} when no such editable declaration exists in the source set, so the
     * governing declaration and the version it names both live in poms that can actually be changed.
     */
    private static @Nullable ResolvedGroupArtifactVersion editableDeclaration(MavenResolutionResult mrr, GroupArtifact ga, boolean wantImport) {
        MavenResolutionResult current = mrr;
        while (current != null) {
            for (ManagedDependency md : current.getPom().getRequested().getDependencyManagement()) {
                boolean kindMatches = wantImport ? md instanceof ManagedDependency.Imported : md instanceof ManagedDependency.Defined;
                if (kindMatches && ga.getGroupId().equals(md.getGroupId()) && ga.getArtifactId().equals(md.getArtifactId())) {
                    return versionEditable(mrr, md.getVersion()) ? current.getPom().getGav() : null;
                }
            }
            current = current.parentPomIsProjectPom() ? current.getParent() : null;
        }
        return null;
    }

    /**
     * Whether {@code version} - the raw version on a governing declaration - can be edited within the source set.
     * A literal can (it is a tag in a project pom). A single {@code ${property}} can only if that property is
     * declared in {@code mrr} or a project-set parent; a property defined solely in an external parent cannot be
     * changed, so the edit would no-op. A compound or absent version is treated as editable (best effort).
     */
    private static boolean versionEditable(MavenResolutionResult mrr, @Nullable String version) {
        if (version == null || !(version.startsWith("${") && version.endsWith("}"))) {
            return true;
        }
        String property = version.substring(2, version.length() - 1);
        if (property.contains("${") || property.contains("}")) {
            return true; // compound expression; not the simple single-property case this guards
        }
        MavenResolutionResult current = mrr;
        while (current != null) {
            if (current.getPom().getRequested().getProperties().containsKey(property)) {
                return true;
            }
            current = current.parentPomIsProjectPom() ? current.getParent() : null;
        }
        return false;
    }
}
