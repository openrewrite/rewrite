/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.LatestIntegration;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.openrewrite.internal.StringUtils.matchesGlob;

/**
 * Removes direct dependencies that are also available transitively from other dependencies,
 * when the direct version is older than or equal to the transitive version.
 * <p>
 * This is useful during dependency upgrades (like Spring Boot migrations) where a dependency
 * that was previously declared directly is now pulled in transitively by the upgraded
 * dependency, potentially with an incompatible version.
 * <p>
 * Note: The transitive dependency information is obtained by downloading and parsing
 * the POMs of other direct dependencies in the project.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveRedundantDirectDependency extends Recipe {

    @Option(displayName = "Group",
            description = "Group glob expression pattern used to match dependencies that should be considered for removal. " +
                          "Group is the first part of a dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "org.apache.tomcat.embed",
            required = false)
    @Nullable
    String groupPattern;

    @Option(displayName = "Artifact",
            description = "Artifact glob expression pattern used to match dependencies that should be considered for removal. " +
                          "Artifact is the second part of a dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "tomcat-embed-*",
            required = false)
    @Nullable
    String artifactPattern;

    @Option(displayName = "Only if transitive version is ...",
            description = "Only remove the direct dependency if the transitive version has the specified comparative relationship " +
                          "to the direct version. For example, `gte` will only remove the direct dependency if the transitive " +
                          "version is the same or newer. Default `gte`.",
            valid = {"ANY", "EQ", "LT", "LTE", "GT", "GTE"},
            required = false)
    @Nullable
    Comparator onlyIfTransitiveVersionIs;

    @Option(displayName = "Except",
            description = "Accepts a list of GAVs. Dependencies matching a GAV will be ignored by this recipe. " +
                          "GAV versions are ignored if provided.",
            example = "com.jcraft:jsch",
            required = false)
    @Nullable
    List<String> except;

    public enum Comparator {
        ANY,
        EQ,
        LT,
        LTE,
        GT,
        GTE
    }

    @Override
    public String getDisplayName() {
        return "Remove redundant direct dependencies";
    }

    @Override
    public String getDescription() {
        return "Removes direct dependencies that are also available transitively from other dependencies, " +
               "when the transitive version meets the specified version criteria (default: same or newer). " +
               "This is useful during dependency upgrades where a previously direct dependency becomes " +
               "available transitively with a compatible or newer version.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = Validated.none();
        if (except != null) {
            for (int i = 0; i < except.size(); i++) {
                final String retainDep = except.get(i);
                validated = validated.and(Validated.test(
                        String.format("except[%d]", i),
                        "did not look like a two-or-three-part GAV",
                        retainDep,
                        maybeGav -> {
                            final int gavParts = maybeGav.split(":").length;
                            return gavParts == 2 || gavParts == 3;
                        }));
            }
        }
        return validated;
    }

    private Comparator determineComparator() {
        return onlyIfTransitiveVersionIs != null ? onlyIfTransitiveVersionIs : Comparator.GTE;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Comparator comparator = determineComparator();
        return Preconditions.check(new IsBuildGradle<>(), new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof JavaSourceFile)) {
                    return tree;
                }

                JavaSourceFile sourceFile = (JavaSourceFile) tree;
                Optional<GradleProject> maybeGp = sourceFile.getMarkers().findFirst(GradleProject.class);
                if (!maybeGp.isPresent()) {
                    return sourceFile;
                }

                GradleProject gradleProject = maybeGp.get();

                // Find redundant direct dependencies
                Set<String> toRemove = findRedundantDirectDependencies(gradleProject, comparator, ctx);

                if (toRemove.isEmpty()) {
                    return sourceFile;
                }

                Tree result = sourceFile;
                for (String ga : toRemove) {
                    String[] parts = ga.split(":");
                    result = new RemoveDependency(parts[0], parts[1], null)
                            .getVisitor()
                            .visitNonNull(result, ctx);
                }

                return result;
            }

            private Set<String> findRedundantDirectDependencies(GradleProject gradleProject, Comparator comparator, ExecutionContext ctx) {
                Set<String> toRemove = new LinkedHashSet<>();

                // Get repositories for downloading POMs
                List<MavenRepository> repositories = gradleProject.getMavenRepositories();

                // Collect all direct dependencies across all configurations
                // We need both the resolved dependencies AND the requested (declared) dependencies
                // because Gradle may resolve to a different version than what was declared
                Map<String, ResolvedDependency> directDeps = new HashMap<>();
                Map<String, String> declaredVersions = new HashMap<>(); // GA -> declared version
                for (GradleDependencyConfiguration config : gradleProject.getConfigurations()) {
                    List<ResolvedDependency> directResolved = config.getDirectResolved();
                    if (directResolved == null) continue;
                    for (ResolvedDependency dep : directResolved) {
                        String ga = dep.getGroupId() + ":" + dep.getArtifactId();
                        directDeps.put(ga, dep);
                        // Look up the declared version from the requested dependencies
                        Dependency requested = config.findRequestedDependency(dep.getGroupId(), dep.getArtifactId());
                        if (requested != null && requested.getVersion() != null) {
                            declaredVersions.put(ga, requested.getVersion());
                        }
                    }
                }

                // For each direct dependency, check if any OTHER direct dependency declares it transitively
                MavenPomDownloader downloader = new MavenPomDownloader(emptyMap(), ctx);

                for (ResolvedDependency direct : directDeps.values()) {
                    String ga = direct.getGroupId() + ":" + direct.getArtifactId();

                    // Skip if not matching patterns
                    if (!matchesGroup(direct) || !matchesArtifact(direct) || !isNotExcepted(direct)) {
                        continue;
                    }

                    // Get the DECLARED version (what user wrote in build.gradle), not the resolved version
                    String directVersion = declaredVersions.getOrDefault(ga, direct.getVersion());

                    // Check if this dependency is declared as transitive by any OTHER direct dependency
                    for (ResolvedDependency otherDirect : directDeps.values()) {
                        if (otherDirect == direct) continue;

                        // Download the POM of the other direct dependency and check its dependencies
                        try {
                            GroupArtifactVersion otherGav = new GroupArtifactVersion(
                                    otherDirect.getGroupId(),
                                    otherDirect.getArtifactId(),
                                    otherDirect.getVersion()
                            );
                            Pom otherPom = downloader.download(otherGav, null, null, repositories);
                            ResolvedPom resolvedOtherPom = otherPom.resolve(emptyList(), downloader, repositories, ctx);

                            // Check if otherPom declares our direct dependency
                            String transitiveVersion = findDeclaredDependencyVersion(
                                    resolvedOtherPom,
                                    direct.getGroupId(),
                                    direct.getArtifactId()
                            );

                            if (transitiveVersion != null) {
                                // Found it! Compare versions using DECLARED version, not resolved
                                if (matchesComparator(directVersion, transitiveVersion, comparator)) {
                                    toRemove.add(ga);
                                    break; // No need to check other direct deps
                                }
                            }
                        } catch (MavenDownloadingException e) {
                            // Could not download POM, skip this check
                        }
                    }
                }

                return toRemove;
            }

            private @Nullable String findDeclaredDependencyVersion(ResolvedPom pom, String groupId, String artifactId) {
                // Check direct dependencies
                for (Dependency dep : pom.getRequestedDependencies()) {
                    if (groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId())) {
                        String version = dep.getVersion();
                        // Version might be null (managed) or a property, try to resolve it
                        if (version != null) {
                            return version;
                        }
                        // Try dependency management
                        for (ResolvedManagedDependency managed : pom.getDependencyManagement()) {
                            if (groupId.equals(managed.getGroupId()) && artifactId.equals(managed.getArtifactId())) {
                                return managed.getVersion();
                            }
                        }
                    }
                }

                // Also check dependency management directly (for BOM-managed dependencies)
                for (ResolvedManagedDependency managed : pom.getDependencyManagement()) {
                    if (groupId.equals(managed.getGroupId()) && artifactId.equals(managed.getArtifactId())) {
                        return managed.getVersion();
                    }
                }

                return null;
            }

            private boolean matchesComparator(String directVersion, String transitiveVersion, Comparator comparator) {
                if (comparator == Comparator.ANY) {
                    return true;
                }

                int comparison = new LatestIntegration(null).compare(null, transitiveVersion, directVersion);

                // comparison > 0 means transitive > direct
                // comparison < 0 means transitive < direct
                // comparison == 0 means transitive == direct

                switch (comparator) {
                    case GT:
                        return comparison > 0;
                    case GTE:
                        return comparison >= 0;
                    case LT:
                        return comparison < 0;
                    case LTE:
                        return comparison <= 0;
                    case EQ:
                        return comparison == 0;
                    default:
                        return false;
                }
            }

            private boolean matchesGroup(ResolvedDependency d) {
                return groupPattern == null || groupPattern.isEmpty() || matchesGlob(d.getGroupId(), groupPattern);
            }

            private boolean matchesArtifact(ResolvedDependency d) {
                return artifactPattern == null || artifactPattern.isEmpty() || matchesGlob(d.getArtifactId(), artifactPattern);
            }

            private boolean isNotExcepted(ResolvedDependency d) {
                if (except == null) {
                    return true;
                }
                for (String gav : except) {
                    String[] split = gav.split(":");
                    String exceptedGroupId = split[0];
                    String exceptedArtifactId = split[1];
                    if (matchesGlob(d.getGroupId(), exceptedGroupId) &&
                        matchesGlob(d.getArtifactId(), exceptedArtifactId)) {
                        return false;
                    }
                }
                return true;
            }
        });
    }
}
