/*
 * Copyright 2026 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedManagedDependency;
import org.openrewrite.semver.Semver;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

import static org.openrewrite.internal.StringUtils.matchesGlob;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveBomManagedDirectDependencies extends Recipe {

    @Option(displayName = "BOM group pattern",
            description = "Group ID glob pattern for BOMs to consider. " +
                          "For example, `org.springframework.boot` to match Spring Boot BOMs.",
            example = "org.springframework.boot")
    String bomGroupPattern;

    @Option(displayName = "BOM artifact pattern",
            description = "Artifact ID glob pattern for BOMs to consider. " +
                          "For example, `*-dependencies` to match Spring Boot's BOM.",
            example = "*-dependencies",
            required = false)
    @Nullable
    String bomArtifactPattern;

    @Option(displayName = "Dependency group pattern",
            description = "Group ID glob pattern for dependencies to check against BOM. " +
                          "Use `*` to match all dependencies.",
            example = "*",
            required = false)
    @Nullable
    String dependencyGroupPattern;

    @Option(displayName = "Dependency artifact pattern",
            description = "Artifact ID glob pattern for dependencies to check against BOM. " +
                          "Use `*` to match all dependencies.",
            example = "*",
            required = false)
    @Nullable
    String dependencyArtifactPattern;

    @Override
    public String getDisplayName() {
        return "Remove direct dependencies that are managed by a BOM with incompatible versions";
    }

    @Override
    public String getDescription() {
        return "Removes directly declared dependencies when they have a version that is incompatible with " +
               "the version managed by an imported BOM. This is useful during framework upgrades (e.g., Spring Boot) " +
               "where transitive dependencies receive major version bumps and explicitly declared older versions " +
               "should be removed to use the BOM-managed versions instead.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String depGroupPattern = dependencyGroupPattern != null ? dependencyGroupPattern : "*";
        String depArtifactPattern = dependencyArtifactPattern != null ? dependencyArtifactPattern : "*";
        String bomArtPattern = bomArtifactPattern != null ? bomArtifactPattern : "*";

        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Document d = super.visitDocument(document, ctx);
                if (d != document) {
                    d = (Xml.Document) new RemoveEmptyDependenciesTags().visitNonNull(d, ctx);
                }
                return d;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isDependencyTag()) {
                    ResolvedDependency dependency = findDependency(tag);
                    if (dependency != null) {
                        String declaredVersion = dependency.getRequested().getVersion();
                        if (declaredVersion == null) {
                            return super.visitTag(tag, ctx);
                        }

                        // Check if dependency matches the patterns
                        if (!matchesGlob(dependency.getGroupId(), depGroupPattern) ||
                            !matchesGlob(dependency.getArtifactId(), depArtifactPattern)) {
                            return super.visitTag(tag, ctx);
                        }

                        // Check if dependency has exclusions - skip removal if so
                        if (tag.getChild("exclusions").isPresent()) {
                            return super.visitTag(tag, ctx);
                        }

                        // Find the managed version from a matching BOM
                        ResolvedManagedDependency managedDep = findManagedDependencyFromMatchingBom(
                                dependency.getGroupId(), dependency.getArtifactId());

                        if (managedDep != null && managedDep.getVersion() != null) {
                            String managedVersion = managedDep.getVersion();
                            String resolvedDeclaredVersion = getResolutionResult().getPom().getValue(declaredVersion);

                            if (resolvedDeclaredVersion != null && hasDifferentMajorVersion(resolvedDeclaredVersion, managedVersion)) {
                                doAfterVisit(new RemoveContentVisitor<>(tag, true, true));
                                maybeUpdateModel();
                            }
                        }
                    }
                }
                return super.visitTag(tag, ctx);
            }

            private @Nullable ResolvedManagedDependency findManagedDependencyFromMatchingBom(String groupId, String artifactId) {
                for (ResolvedManagedDependency managed : getResolutionResult().getPom().getDependencyManagement()) {
                    if (managed.getGroupId().equals(groupId) && managed.getArtifactId().equals(artifactId)) {
                        ResolvedGroupArtifactVersion bomGav = managed.getBomGav();
                        if (bomGav != null) {
                            // Check if the BOM matches our patterns
                            if (matchesGlob(bomGav.getGroupId(), bomGroupPattern) &&
                                matchesGlob(bomGav.getArtifactId(), bomArtPattern)) {
                                return managed;
                            }
                        }
                    }
                }
                return null;
            }

            private boolean hasDifferentMajorVersion(String declaredVersion, String managedVersion) {
                String declaredMajor = Semver.majorVersion(declaredVersion);
                String managedMajor = Semver.majorVersion(managedVersion);
                return !declaredMajor.equals(managedMajor);
            }
        };
    }

    private static class RemoveEmptyDependenciesTags extends MavenIsoVisitor<ExecutionContext> {
        @Override
        public Xml.@Nullable Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            if (t != null && "dependencies".equals(t.getName()) && (t.getContent() == null || t.getContent().isEmpty())) {
                return null;
            }
            return t;
        }
    }
}
