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
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
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
import org.openrewrite.maven.MavenDownloadingExceptions;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.LatestPatch;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeDependencyVersion extends Recipe {

    private static final String VERSION_VARIABLE_KEY = "VERSION_VARIABLE";
    private static final String NEW_VERSION_KEY = "NEW_VERSION";

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
        return "Upgrade Gradle dependency versions";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Upgrade the version of a dependency in a build.gradle file. " +
                "Supports updating dependency declarations of various forms:\n" +
                "* `String` notation:  `implementation \"group:artifact:version\"` \n" +
                "* `Map` notation: `implementation group: 'group', name: 'artifact', version: 'version'`\n" +
                "Can update version numbers which are defined earlier in the same file in variable declarations.";
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
        MethodMatcher dependencyDsl = new MethodMatcher("DependencyHandlerSpec *(..)");
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
                Set<GroupArtifactVersion> versionUpdates = getCursor().getMessage(NEW_VERSION_KEY);
                if(versionUpdates != null) {
                    Optional<GradleProject> maybeGp = cu.getMarkers()
                            .findFirst(GradleProject.class);
                    if(!maybeGp.isPresent()) {
                        return cu;
                    }
                    GradleProject newGp = maybeGp.get();
                    for (GroupArtifactVersion gav : versionUpdates) {
                        newGp = replaceVersion(newGp, ctx, gav);
                    }
                    cu = cu.withMarkers(cu.getMarkers().removeByType(GradleProject.class).add(newGp));
                }
                return cu;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (dependencyDsl.matches(m)) {
                    List<Expression> depArgs = m.getArguments();
                    if(depArgs.get(0) instanceof G.GString) {
                        G.GString gString = (G.GString) depArgs.get(0);
                        List<J> strings = gString.getStrings();
                        if(strings.size() != 2 || !(strings.get(0) instanceof J.Literal) || !(strings.get(1) instanceof G.GString.Value)) {
                            return m;
                        }
                        G.GString.Value versionValue = (G.GString.Value) strings.get(1);
                        if(!(versionValue.getTree() instanceof J.Identifier)) {
                            return m;
                        }
                        String versionVariableName = ((J.Identifier) versionValue.getTree()).getSimpleName();
                        getCursor().putMessageOnFirstEnclosing(JavaSourceFile.class, VERSION_VARIABLE_KEY, versionVariableName);

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
                                    return m;
                                }
                                if(!version.startsWith("$")) {
                                    try {
                                        String newVersion = findNewerVersion(groupId, artifactId, version, versionComparator,
                                                gradleProject, ctx);
                                        if (newVersion == null || version.equals(newVersion)) {
                                            return m;
                                        }
                                        getCursor().dropParentUntil(p -> p instanceof SourceFile)
                                                .computeMessageIfAbsent(NEW_VERSION_KEY, it -> new LinkedHashSet<GroupArtifactVersion>())
                                                .add(new GroupArtifactVersion(groupId, artifactId, newVersion));

                                        return m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                                            J.Literal literal = (J.Literal) arg;
                                            String newGav = groupId + ":" + artifactId + ":" + newVersion;
                                            return literal
                                                    .withValue(newGav)
                                                    .withValueSource(requireNonNull(literal.getValueSource()).replace(gav, newGav));
                                        }));
                                    } catch (MavenDownloadingException e) {
                                        return e.warn(m);
                                    }
                                }
                            }
                        }
                    } else if (depArgs.size() == 3 && depArgs.get(0) instanceof G.MapEntry
                            && depArgs.get(1) instanceof G.MapEntry
                            && depArgs.get(2) instanceof G.MapEntry) {
                        Expression groupValue = ((G.MapEntry) depArgs.get(0)).getValue();
                        Expression artifactValue = ((G.MapEntry) depArgs.get(1)).getValue();
                        if(!(groupValue instanceof J.Literal) || !(artifactValue instanceof J.Literal)) {
                            return m;
                        }
                        J.Literal groupLiteral = (J.Literal) groupValue;
                        J.Literal artifactLiteral = (J.Literal) artifactValue;
                        if(!groupId.equals(groupLiteral.getValue()) || !artifactId.equals(artifactLiteral.getValue())) {
                            return m;
                        }
                        G.MapEntry versionEntry = (G.MapEntry) depArgs.get(2);
                        Expression versionExp = versionEntry.getValue();
                        if(versionExp instanceof J.Literal && ((J.Literal) versionExp).getValue() instanceof String) {
                            GradleProject gradleProject = getCursor().firstEnclosingOrThrow(JavaSourceFile.class)
                                    .getMarkers()
                                    .findFirst(GradleProject.class)
                                    .orElseThrow(() -> new IllegalArgumentException("Gradle files are expected to have a GradleProject marker."));
                            J.Literal versionLiteral = (J.Literal) versionExp;
                            String version = (String) versionLiteral.getValue();
                            if(version.startsWith("$")) {
                                return m;
                            }
                            String newVersion;
                            try {
                                newVersion = findNewerVersion(groupId, artifactId, version, versionComparator,
                                        gradleProject, ctx);
                            } catch (MavenDownloadingException e) {
                                return e.warn(m);
                            }
                            if (newVersion == null || version.equals(newVersion)) {
                                return m;
                            }
                            List<Expression> newArgs = new ArrayList<>(3);
                            newArgs.add(depArgs.get(0));
                            newArgs.add(depArgs.get(1));
                            newArgs.add(versionEntry.withValue(
                                    versionLiteral
                                            .withValueSource(requireNonNull(versionLiteral.getValueSource()).replace(version, newVersion))
                                            .withValue(newVersion)));

                            return m.withArguments(newArgs);
                        } else if(versionExp instanceof J.Identifier) {
                            String versionVariableName = ((J.Identifier) versionExp).getSimpleName();
                            getCursor().putMessageOnFirstEnclosing(JavaSourceFile.class, VERSION_VARIABLE_KEY, versionVariableName);
                        }
                    }
                }
                return m;
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
                getCursor().dropParentUntil(p -> p instanceof SourceFile)
                        .computeMessageIfAbsent(NEW_VERSION_KEY, m -> new LinkedHashSet<GroupArtifactVersion>())
                        .add(new GroupArtifactVersion(groupId, artifactId, newVersion));

                J.Literal newVersionLiteral = initializer.withValue(newVersion)
                        .withValueSource("'" + newVersion + "'");
                v = v.withInitializer(newVersionLiteral);
            } catch (MavenDownloadingException e) {
                return e.warn(v);
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

    static GradleProject replaceVersion(GradleProject gp, ExecutionContext ctx, GroupArtifactVersion gav) {
        try {
            if(gav.getGroupId() == null || gav.getArtifactId() == null) {
                return gp;
            }
            MavenPomDownloader mpd = new MavenPomDownloader(emptyMap(), ctx, null, null);
            Pom pom = mpd.download(gav, null, null, gp.getMavenRepositories());
            ResolvedPom resolvedPom = pom.resolve(emptyList(), mpd, gp.getMavenRepositories(), ctx);
            ResolvedGroupArtifactVersion resolvedGav = resolvedPom.getGav();
            List<ResolvedDependency> transitiveDependencies = resolvedPom.resolveDependencies(Scope.Runtime, mpd, ctx);
            Map<String, GradleDependencyConfiguration> nameToConfiguration = gp.getNameToConfiguration();
            Map<String, GradleDependencyConfiguration> newNameToConfiguration = new HashMap<>(nameToConfiguration.size());
            boolean anyChanged = false;
            for (GradleDependencyConfiguration gdc : nameToConfiguration.values()) {
                GradleDependencyConfiguration newGdc = gdc;
                newGdc = newGdc.withRequested(ListUtils.map(gdc.getRequested(), requested -> {
                    if (!Objects.equals(requested.getGroupId(), gav.getGroupId()) || !Objects.equals(requested.getArtifactId(), gav.getArtifactId())) {
                        return requested;
                    }
                    return requested.withGav(gav);
                }));
                newGdc = newGdc.withResolved(ListUtils.map(gdc.getResolved(), resolved -> {
                    if (!Objects.equals(resolved.getGroupId(), resolvedGav.getGroupId()) || !Objects.equals(resolved.getArtifactId(), resolvedGav.getArtifactId())) {
                        return resolved;
                    }
                    return resolved.withGav(resolvedGav)
                            .withDependencies(transitiveDependencies);
                }));
                anyChanged |= newGdc != gdc;
                newNameToConfiguration.put(newGdc.getName(), newGdc);
            }
            if(anyChanged) {
                gp = gp.withNameToConfiguration(newNameToConfiguration);
            }
        } catch (MavenDownloadingException | MavenDownloadingExceptions  e) {
            return gp;
        }
        return gp;
    }
}
