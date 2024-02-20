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
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.search.FindGradleProject;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Markup;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.semver.Semver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddDirectDependencyToUpgradeTransitiveVersion extends Recipe {

    @EqualsAndHashCode.Exclude
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "com.fasterxml.jackson*")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "jackson-module*")
    String artifactId;

    @Option(displayName = "Version",
            description = "An exact version number or node-style semver selector used to select the version number. " +
                          "You can also use `latest.release` for the latest available version and `latest.patch` if " +
                          "the current version is a valid semantic version. For more details, you can look at the documentation " +
                          "page of [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors). " +
                          "Defaults to `latest.release`.",
            example = "29.X",
            required = false)
    @Nullable
    String version;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                          "Setting 'newVersion' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Override
    public String getDisplayName() {
        return "Upgrade transitive Gradle dependencies";
    }

    @Override
    public String getDescription() {
        return "Upgrades the version of a transitive dependency in a Gradle build file. " +
               "There are many ways to do this in Gradle, so the mechanism for upgrading a " +
               "transitive dependency must be considered carefully depending on your style " +
               "of dependency management.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (version != null) {
            validated = validated.and(Semver.validate(version, versionPattern));
        }
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        DependencyMatcher dependencyMatcher = new DependencyMatcher(groupId, artifactId, null);
        return Preconditions.check(new FindGradleProject(FindGradleProject.SearchCriteria.Marker), new GroovyVisitor<ExecutionContext>() {
            GradleProject gradleProject;

            @Override
            public J visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                gradleProject = cu.getMarkers().findFirst(GradleProject.class)
                        .orElseThrow(() -> new IllegalStateException("Unable to find GradleProject marker."));

                Map<GroupArtifact, List<GradleDependencyConfiguration>> toUpdate = new HashMap<>();

                DependencyVersionSelector versionSelector = new DependencyVersionSelector(metadataFailures, gradleProject);
                for (GradleDependencyConfiguration configuration : gradleProject.getConfigurations()) {
                    for (ResolvedDependency resolvedDependency : configuration.getResolved()) {
                        if (resolvedDependency.getDepth() > 0 &&
                            dependencyMatcher.matches(resolvedDependency.getGroupId(), resolvedDependency.getArtifactId(), resolvedDependency.getVersion())) {

                            try {
                                String selected = versionSelector.select(resolvedDependency.getGav(), configuration.getName(),
                                        version, versionPattern, ctx);
                                if (!resolvedDependency.getVersion().equals(selected)) {
                                    toUpdate.merge(new GroupArtifact(groupId, artifactId), singletonList(configuration), (existing, update) -> {
                                        List<GradleDependencyConfiguration> all = ListUtils.concatAll(existing, update);
                                        all.removeIf(c -> {
                                            for (GradleDependencyConfiguration config : all) {
                                                if (c.allExtendsFrom().contains(config)) {
                                                    return true;
                                                }
                                            }
                                            return false;
                                        });
                                        return all;
                                    });
                                }
                            } catch (MavenDownloadingException e) {
                                return Markup.warn(cu, e);
                            }
                        }
                    }
                }

                return super.visitCompilationUnit(cu, ctx);
            }
        });
    }
}
