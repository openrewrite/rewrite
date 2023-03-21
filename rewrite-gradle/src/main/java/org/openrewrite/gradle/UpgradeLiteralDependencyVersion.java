/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.semver.LatestPatch;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeLiteralDependencyVersion extends Recipe {
    @EqualsAndHashCode.Exclude
    MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "com.fasterxml.jackson*")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "jackson-module*")
    String artifactId;

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "29.X")
    String newVersion;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                          "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Override
    public String getDisplayName() {
        return "Upgrade Gradle dependencies who use a fixed literal version";
    }

    @Override
    public String getDescription() {
        return "A fixed literal version is a version that is not a variable or property or " +
               "supplied indirectly by platform BOMs or similar means. For example, `com.google.guava:guava:29.0-jre`.";
    }

    @Override
    public Validated validate() {
        Validated validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        return validated;
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new IsBuildGradle<>();
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher dependency = new MethodMatcher("DependencyHandlerSpec *(..)");
        VersionComparator versionComparator = requireNonNull(Semver.validate(newVersion, versionPattern).getValue());
        return new GroovyVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (dependency.matches(method)) {
                    List<Expression> depArgs = method.getArguments();
                    if (depArgs.get(0) instanceof J.Literal) {
                        String gav = (String) ((J.Literal) depArgs.get(0)).getValue();
                        assert gav != null;
                        String[] parts = gav.split(":");
                        if (gav.length() >= 2) {
                            if (StringUtils.matchesGlob(parts[0], groupId) &&
                                StringUtils.matchesGlob(parts[1], artifactId)) {

                                GradleProject gradleProject = getCursor().firstEnclosingOrThrow(G.CompilationUnit.class)
                                        .getMarkers()
                                        .findFirst(GradleProject.class)
                                        .orElseThrow(() -> new IllegalArgumentException("Gradle files are expected to have a GradleProject marker."));

                                String version = parts[2];
                                if (version != null && !version.startsWith("$")) {
                                    try {
                                        String newVersion = findNewerVersion(groupId, artifactId, version, gradleProject, ctx);
                                        if (newVersion == null || version.equals(newVersion)) {
                                            return method;
                                        }
                                        return method.withArguments(ListUtils.mapFirst(method.getArguments(), arg -> {
                                            J.Literal literal = (J.Literal) arg;
                                            String newGav = groupId + ":" + artifactId + ":" + newVersion;
                                            return literal
                                                    .withValue(newGav)
                                                    .withValueSource(requireNonNull(literal.getValueSource()).replace(gav, newGav));
                                        }));
                                    } catch (MavenDownloadingException e) {
                                        return e.warn(method);
                                    }
                                }
                            }
                        }
                    }
                }
                return super.visitMethodInvocation(method, ctx);
            }

            @Nullable
            private String findNewerVersion(String groupId, String artifactId, String version,
                                            GradleProject gradleProject, ExecutionContext ctx) throws MavenDownloadingException {
                // in the case of "latest.patch", a new version can only be derived if the
                // current version is a semantic version
                if (versionComparator instanceof LatestPatch && !versionComparator.isValid(version, version)) {
                    return null;
                }

                try {
                    MavenMetadata mavenMetadata = metadataFailures.insertRows(ctx, () -> downloadMetadata(groupId, artifactId, gradleProject, ctx));
                    List<String> versions = new ArrayList<>();
                    for (String v : mavenMetadata.getVersioning().getVersions()) {
                        if (versionComparator.isValid(version, v)) {
                            versions.add(v);
                        }
                    }
                    return versionComparator.upgrade(version, versions).orElse(null);
                } catch (IllegalStateException e) {
                    // this can happen when we encounter exotic versions
                    return null;
                }
            }

            public MavenMetadata downloadMetadata(String groupId, String artifactId, GradleProject gradleProject, ExecutionContext ctx) throws MavenDownloadingException {
                return new MavenPomDownloader(emptyMap(), ctx, null, null)
                        .downloadMetadata(new GroupArtifact(groupId, artifactId), null,
                                gradleProject.getMavenRepositories());
            }
        };
    }
}
