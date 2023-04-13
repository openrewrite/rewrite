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
import org.openrewrite.gradle.search.FindGradleProject;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
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
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeLiteralDependencyVersion extends Recipe {

    private static final String VERSION_VARIABLE_KEY = "VERSION_VARIABLE_KEY";

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

    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new FindGradleProject(FindGradleProject.SearchCriteria.Marker).getVisitor();
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher dependency = new MethodMatcher("DependencyHandlerSpec *(..)");
        VersionComparator versionComparator = requireNonNull(Semver.validate(newVersion, versionPattern).getValue());
        return new GroovyVisitor<ExecutionContext>() {

            @Override
            public J visitJavaSourceFile(JavaSourceFile sourceFile, ExecutionContext ctx) {
                JavaSourceFile cu = (JavaSourceFile) super.visitJavaSourceFile(sourceFile, ctx);
                String variableName = getCursor().getMessage(VERSION_VARIABLE_KEY);
                if(variableName != null) {
                    Optional<GradleProject> maybeGp = cu.getMarkers()
                            .findFirst(GradleProject.class);
                    if(!maybeGp.isPresent()) {
                        return cu;
                    }

                    cu = (JavaSourceFile) new UpdateVariable(variableName, versionComparator, maybeGp.get()).visitNonNull(cu, ctx);
                }

                return cu;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (dependency.matches(method)) {
                    List<Expression> depArgs = method.getArguments();
                    if(depArgs.get(0) instanceof G.GString) {
                        G.GString gString = (G.GString) depArgs.get(0);

                    } else if (depArgs.get(0) instanceof J.Literal) {
                        String gav = (String) ((J.Literal) depArgs.get(0)).getValue();
                        assert gav != null;
                        String[] parts = gav.split(":");
                        if (gav.length() >= 2) {
                            if (StringUtils.matchesGlob(parts[0], groupId) &&
                                StringUtils.matchesGlob(parts[1], artifactId)) {

                                GradleProject gradleProject = getCursor().firstEnclosingOrThrow(JavaSourceFile.class)
                                        .getMarkers()
                                        .findFirst(GradleProject.class)
                                        .orElseThrow(() -> new IllegalArgumentException("Gradle files are expected to have a GradleProject marker."));

                                String version = parts[2];
                                if(version == null) {
                                    return method;
                                }
                                if(!version.startsWith("$")) {
                                    try {
                                        String newVersion = findNewerVersion(groupId, artifactId, version, versionComparator,
                                                gradleProject, ctx);
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
        };
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private class UpdateVariable extends GroovyIsoVisitor<ExecutionContext> {
        String versionVariableName;
        VersionComparator versionComparator;
        GradleProject gradleProject;
        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
            J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, ctx);
            if(!versionVariableName.equals((v.getSimpleName()))) {
                return v;
            }
            if(!(v.getInitializer() instanceof J.Literal)) {
                return v;
            }
            J.Literal initializer = (J.Literal) v.getInitializer();
            if(initializer.getType() != JavaType.Primitive.String) {
                return v;
            }
            String version = (String) initializer.getValue();
            if(version == null) {
                return v;
            }

            try {
                String newVersion = findNewerVersion(groupId, artifactId, version, versionComparator, gradleProject, ctx);
                if(newVersion == null) {
                    return v;
                }
                J.Literal newVersionLiteral = initializer.withValue(newVersion)
                        .withValueSource("'" + newVersion + "'");
                v = v.withInitializer(newVersionLiteral);
            } catch (MavenDownloadingException e) {
                return e.warn(v);
            }
            return v;
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext executionContext) {
            J.Assignment v = super.visitAssignment(assignment, executionContext);
            if(!(assignment.getAssignment() instanceof J.Identifier)) {
                return v;
            }
            J.Identifier id = (J.Identifier) assignment.getAssignment();
            if(!versionVariableName.equals((id.getSimpleName()))) {
                return v;
            }
            return v;
        }
    }

    @Nullable
    private String findNewerVersion(String groupId, String artifactId, String version, VersionComparator versionComparator,
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
}
