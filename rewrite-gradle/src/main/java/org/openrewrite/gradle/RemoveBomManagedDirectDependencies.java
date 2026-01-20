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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleDependencies;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.gradle.trait.GradleMultiDependency;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.trait.Trait;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.semver.Semver;

import java.util.*;

import static java.util.Collections.emptyList;
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
               "the version managed by an imported BOM (platform or enforcedPlatform). This is useful during " +
               "framework upgrades (e.g., Spring Boot) where transitive dependencies receive major version " +
               "bumps and explicitly declared older versions should be removed to use the BOM-managed versions instead.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String depGroupPattern = dependencyGroupPattern != null ? dependencyGroupPattern : "*";
        String depArtifactPattern = dependencyArtifactPattern != null ? dependencyArtifactPattern : "*";
        String bomArtPattern = bomArtifactPattern != null ? bomArtifactPattern : "*";

        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @SuppressWarnings("NotNullFieldNotInitialized")
            GradleProject gp;
            final Map<String, List<ResolvedPom>> platforms = new HashMap<>();
            final Set<Statement> statementsToRemove = new HashSet<>();

            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    Optional<GradleProject> maybeGp = tree.getMarkers().findFirst(GradleProject.class);
                    if (!maybeGp.isPresent()) {
                        return (J) tree;
                    }

                    gp = maybeGp.get();

                    // Collect platform dependencies
                    collectPlatforms(tree, ctx);

                    // Identify dependencies to remove
                    if (!platforms.isEmpty()) {
                        new IdentifyIncompatibleDependencies().visit(tree, ctx);
                    }

                    // Remove identified statements
                    if (!statementsToRemove.isEmpty()) {
                        tree = new RemoveStatements().visitNonNull(tree, ctx);
                    }
                }
                return super.visit(tree, ctx);
            }

            private void collectPlatforms(Tree tree, ExecutionContext ctx) {
                MavenPomDownloader mpd = new MavenPomDownloader(ctx);
                GradleMultiDependency.matcher()
                        .asVisitor(gmd -> gmd.map(gradleDependency -> {
                            if (gradleDependency.isPlatform()) {
                                GroupArtifactVersion gav = gradleDependency.getGav();
                                if (matchesGlob(gav.getGroupId(), bomGroupPattern) && matchesGlob(gav.getArtifactId(), bomArtPattern)) {
                                    try {
                                        ResolvedPom platformPom = mpd.download(gav, null, null, gp.getMavenRepositories())
                                                .resolve(emptyList(), mpd, ctx);
                                        platforms.computeIfAbsent(gradleDependency.getConfigurationName(), k -> new ArrayList<>())
                                                .add(platformPom);
                                    } catch (MavenDownloadingException ignored) {
                                    }
                                }
                            }
                            return gradleDependency.getTree();
                        }))
                        .visit(tree, ctx);
            }

            class IdentifyIncompatibleDependencies extends JavaIsoVisitor<ExecutionContext> {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                    Optional<GradleDependency> optDep = new GradleDependency.Matcher().get(getCursor());
                    if (!optDep.isPresent()) {
                        return m;
                    }

                    GradleDependency gradleDependency = optDep.get();

                    // Skip platform dependencies
                    if (gradleDependency.isPlatform()) {
                        return m;
                    }

                    ResolvedDependency dep = gradleDependency.getResolvedDependency();

                    // Check patterns
                    if (!matchesGlob(dep.getGroupId(), depGroupPattern) ||
                        !matchesGlob(dep.getArtifactId(), depArtifactPattern)) {
                        return m;
                    }

                    // Get declared version - use the version as declared in the build file
                    String declaredVersion = gradleDependency.getDeclaredVersion();
                    if (declaredVersion == null || declaredVersion.isEmpty()) {
                        return m;
                    }

                    // Find managed version from platforms
                    String configName = gradleDependency.getConfigurationName();
                    String managedVersion = findManagedVersion(dep.getGroupId(), dep.getArtifactId(),
                            dep.getRequested().getClassifier(), configName);

                    if (managedVersion != null && hasDifferentMajorVersion(declaredVersion, managedVersion)) {
                        statementsToRemove.add(m);
                    }

                    return m;
                }
            }

            private @Nullable String findManagedVersion(String groupId, String artifactId, @Nullable String classifier, String configName) {
                // Check direct configuration
                List<ResolvedPom> configPlatforms = platforms.get(configName);
                if (configPlatforms != null) {
                    for (ResolvedPom platform : configPlatforms) {
                        String managedVersion = platform.getManagedVersion(groupId, artifactId, null, classifier);
                        if (managedVersion != null) {
                            return managedVersion;
                        }
                    }
                }

                // Check extended configurations
                GradleDependencyConfiguration gdc = gp.getConfiguration(configName);
                if (gdc != null) {
                    for (GradleDependencyConfiguration extendedConfig : gdc.allExtendsFrom()) {
                        List<ResolvedPom> extendedPlatforms = platforms.get(extendedConfig.getName());
                        if (extendedPlatforms != null) {
                            for (ResolvedPom platform : extendedPlatforms) {
                                String managedVersion = platform.getManagedVersion(groupId, artifactId, null, classifier);
                                if (managedVersion != null) {
                                    return managedVersion;
                                }
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

            class RemoveStatements extends JavaVisitor<ExecutionContext> {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                    return new GradleDependencies.Matcher().get(getCursor())
                            .map(dependencies -> dependencies.filterStatements(s -> !statementsToRemove.contains(s)))
                            .map(Trait::getTree)
                            .orElse(m);
                }
            }
        });
    }
}
