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
import org.openrewrite.gradle.util.Dependency;
import org.openrewrite.gradle.util.DependencyStringNotationConverter;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenDownloadingExceptions;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.ExactVersion;
import org.openrewrite.semver.LatestPatch;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = true)
public class AddDependency extends ScanningRecipe<AddDependency.Scanned> {

    @EqualsAndHashCode.Exclude
    MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`",
            example = "guava")
    String artifactId;

    @Option(displayName = "Version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "29.X",
            required = false)
    @Nullable
    String version;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example, " +
                          "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Option(displayName = "Configuration",
            description = "A configuration to use when it is not what can be inferred from usage. Most of the time this will be left empty, but " +
                          "is used when adding a new as of yet unused dependency.",
            example = "implementation",
            required = false)
    @Nullable
    String configuration;

    @Option(displayName = "Only if using",
            description = "Used to determine if the dependency will be added and in which scope it should be placed.",
            example = "org.junit.jupiter.api.*")
    String onlyIfUsing;

    @Option(displayName = "Classifier",
            description = "A classifier to add. Commonly used to select variants of a library.",
            example = "test",
            required = false)
    @Nullable
    String classifier;

    @Option(displayName = "Extension",
            description = "The extension of the dependency to add. If omitted Gradle defaults to assuming the type is \"jar\".",
            example = "jar",
            required = false)
    @Nullable
    String extension;

    @Option(displayName = "Family pattern",
            description = "A pattern, applied to groupIds, used to determine which other dependencies should have aligned version numbers. " +
                          "Accepts '*' as a wildcard character.",
            example = "com.fasterxml.jackson*",
            required = false)
    @Nullable
    String familyPattern;

    static final String DEPENDENCY_PRESENT = "org.openrewrite.gradle.AddDependency.DEPENDENCY_PRESENT";

    @Override
    public String getDisplayName() {
        return "Add Gradle dependency";
    }

    @Override
    public String getDescription() {
        return "Add a gradle dependency to a `build.gradle` file in the correct configuration based on where it is used.";
    }

    @Override
    public Validated validate() {
        Validated validated = super.validate();
        if (version != null) {
            validated = validated.and(Semver.validate(version, versionPattern));
        }
        return validated;
    }

    static class Scanned {
        boolean usingType;
        Map<JavaProject, String> configurationByProject = new HashMap<>();
    }

    @Override
    public Scanned getInitialValue() {
        return new Scanned();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Scanned acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                sourceFile.getMarkers().findFirst(JavaProject.class).ifPresent(javaProject ->
                        sourceFile.getMarkers().findFirst(JavaSourceSet.class).ifPresent(sourceSet -> {
                            if (sourceFile != new UsesType<>(onlyIfUsing, true).visit(sourceFile, ctx)) {
                                acc.usingType = true;
                                acc.configurationByProject.compute(javaProject, (jp, configuration) -> "implementation".equals(configuration) ?
                                        configuration :
                                        "test".equals(sourceSet.getName()) ? "testImplementation" : "implementation"
                                );
                            }
                        }));
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Scanned acc) {
        return Preconditions.check(acc.usingType && !acc.configurationByProject.isEmpty(),
                Preconditions.check(new IsBuildGradle<>(), new GroovyIsoVisitor<ExecutionContext>() {
                    final MethodMatcher dependencyDslMatcher = new MethodMatcher("DependencyHandlerSpec *(..)");
                    final Pattern familyPatternCompiled = StringUtils.isBlank(familyPattern) ? null : Pattern.compile(familyPattern.replace("*", ".*"));

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                        if (dependencyDslMatcher.matches(m) && (StringUtils.isBlank(configuration) || configuration.equals(m.getSimpleName()))) {
                            if (m.getArguments().get(0) instanceof J.Literal) {
                                //noinspection ConstantConditions
                                Dependency dependency = DependencyStringNotationConverter.parse((String) ((J.Literal) m.getArguments().get(0)).getValue());
                                if (groupId.equals(dependency.getGroupId()) && artifactId.equals(dependency.getArtifactId())) {
                                    getCursor().putMessageOnFirstEnclosing(G.CompilationUnit.class, DEPENDENCY_PRESENT, true);
                                }
                            } else if (m.getArguments().get(0) instanceof G.GString) {
                                List<J> strings = ((G.GString) m.getArguments().get(0)).getStrings();
                                if (strings.size() >= 2 &&
                                        strings.get(0) instanceof J.Literal) {
                                    String dep = (String) ((J.Literal) strings.get(0)).getValue();
                                    if (dep != null) {
                                        Dependency dependency = DependencyStringNotationConverter.parse(dep);
                                        if (groupId.equals(dependency.getGroupId()) && artifactId.equals(dependency.getArtifactId())) {
                                            getCursor().putMessageOnFirstEnclosing(G.CompilationUnit.class, DEPENDENCY_PRESENT, true);
                                        }
                                    }
                                }
                            } else if (m.getArguments().get(0) instanceof G.MapEntry) {
                                G.MapEntry groupEntry = null;
                                G.MapEntry artifactEntry = null;

                                for (Expression e : m.getArguments()) {
                                    if (!(e instanceof G.MapEntry)) {
                                        continue;
                                    }
                                    G.MapEntry arg = (G.MapEntry) e;
                                    if (!(arg.getKey() instanceof J.Literal) || !(arg.getValue() instanceof J.Literal)) {
                                        continue;
                                    }
                                    J.Literal key = (J.Literal) arg.getKey();
                                    J.Literal value = (J.Literal) arg.getValue();
                                    if (!(key.getValue() instanceof String) || !(value.getValue() instanceof String)) {
                                        continue;
                                    }
                                    if ("group".equals(key.getValue())) {
                                        groupEntry = arg;
                                    } else if ("name".equals(key.getValue())) {
                                        artifactEntry = arg;
                                    }
                                }

                                if (groupEntry == null || artifactEntry == null) {
                                    return m;
                                }

                                if (groupId.equals(((J.Literal) groupEntry.getValue()).getValue())
                                    && artifactId.equals(((J.Literal) artifactEntry.getValue()).getValue())) {
                                    getCursor().putMessageOnFirstEnclosing(G.CompilationUnit.class, DEPENDENCY_PRESENT, true);
                                }
                            }
                        }

                        return m;
                    }

                    @Override
                    public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                        if (!cu.getSourcePath().toString().endsWith(".gradle") || cu.getSourcePath().getFileName().toString().equals("settings.gradle")) {
                            return cu;
                        }

                        JavaProject javaProject = getCursor().firstEnclosingOrThrow(JavaSourceFile.class)
                                .getMarkers().findFirst(JavaProject.class).orElse(null);
                        String maybeConfiguration = javaProject == null ? null :
                                acc.configurationByProject.get(javaProject);
                        if (maybeConfiguration == null) {
                            return cu;
                        }

                        G.CompilationUnit g = super.visitCompilationUnit(cu, ctx);

                        if (getCursor().getMessage(DEPENDENCY_PRESENT, false)) {
                            return g;
                        }

                        String resolvedConfiguration = StringUtils.isBlank(configuration) ? maybeConfiguration : configuration;
                        String resolvedVersion = version;
                        Optional<GradleProject> maybeGp = g.getMarkers().findFirst(GradleProject.class);
                        if (version != null && maybeGp.isPresent()) {
                            Validated versionValidation = Semver.validate(version, versionPattern);
                            if (versionValidation.isValid() && versionValidation.getValue() != null) {
                                VersionComparator versionComparator = versionValidation.getValue();
                                if(!(versionComparator instanceof ExactVersion)) {
                                    try {
                                        resolvedVersion = findNewerVersion(groupId, artifactId, version, versionComparator, maybeGp.get(), ctx);
                                    } catch (MavenDownloadingException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        }

                        g = (G.CompilationUnit) new AddDependencyVisitor(groupId, artifactId, resolvedVersion,
                                StringUtils.isBlank(versionPattern) ? null : versionPattern,
                                resolvedConfiguration,
                                StringUtils.isBlank(classifier) ? null : classifier,
                                StringUtils.isBlank(extension) ? null : extension,
                                familyPatternCompiled).visitNonNull(g, ctx, requireNonNull(getCursor().getParent()));
                        if(g != cu && maybeGp.isPresent()) {
                            String versionWithPattern = resolvedVersion + (StringUtils.isBlank(versionPattern) ? "" : versionPattern);
                            GradleProject gp = maybeGp.get();
                            GradleProject newGp = addDependency(gp,
                                    gp.getConfiguration(resolvedConfiguration),
                                    new GroupArtifactVersion(groupId, artifactId, versionWithPattern),
                                    classifier,
                                    ctx);
                            g = g.withMarkers(g.getMarkers().setByType(newGp));
                        }
                        return g;
                    }
                })
        );
    }

    /**
     * Update the dependency model, adding the specified dependency to the specified configuration and all configurations
     * which extend from it.
     *
     * @param gp marker with the current, pre-update dependency information
     * @param configuration the configuration to add the dependency to
     * @param gav the group, artifact, and version of the dependency to add
     * @param classifier the classifier of the dependency to add
     * @param ctx context which will be used to download the pom for the dependency
     * @return a copy of gp with the dependency added
     */
    static GradleProject addDependency(
            GradleProject gp,
            @Nullable GradleDependencyConfiguration configuration,
            GroupArtifactVersion gav,
            @Nullable String classifier,
            ExecutionContext ctx) {
        try {
            if(gav.getGroupId() == null || gav.getArtifactId() == null || configuration == null) {
                return gp;
            }
            MavenPomDownloader mpd = new MavenPomDownloader(emptyMap(), ctx, null,  null);
            Pom pom = mpd.download(gav, null, null, gp.getMavenRepositories());
            ResolvedPom resolvedPom = pom.resolve(emptyList(), mpd, gp.getMavenRepositories(), ctx);
            ResolvedGroupArtifactVersion resolvedGav = resolvedPom.getGav();
            List<ResolvedDependency> transitiveDependencies = resolvedPom.resolveDependencies(Scope.Runtime, mpd, ctx);
            Map<String, GradleDependencyConfiguration> nameToConfiguration = gp.getNameToConfiguration();
            Map<String, GradleDependencyConfiguration> newNameToConfiguration = new HashMap<>(nameToConfiguration.size());

            Set<GradleDependencyConfiguration> configurationsToAdd = Stream.concat(
                            Stream.of(configuration),
                            gp.configurationsExtendingFrom(configuration, true).stream())
                    .collect(Collectors.toSet());

            for(GradleDependencyConfiguration gdc : nameToConfiguration.values()) {
                if(!configurationsToAdd.contains(gdc)) {
                    newNameToConfiguration.put(gdc.getName(), gdc);
                    continue;
                }

                GradleDependencyConfiguration newGdc = gdc;
                org.openrewrite.maven.tree.Dependency newRequested = new org.openrewrite.maven.tree.Dependency(
                        gav, classifier, "jar", gdc.getName(), emptyList(), null);
                newGdc = newGdc.withRequested(ListUtils.concat(
                        ListUtils.map(gdc.getRequested(), requested -> {
                            // Remove any existing dependency with the same group and artifact id
                            if (Objects.equals(requested.getGroupId(), gav.getGroupId()) && Objects.equals(requested.getArtifactId(), gav.getArtifactId())) {
                                return null;
                            }
                            return requested;
                        }),
                        newRequested));
                if(newGdc.isCanBeResolved()) {
                    newGdc = newGdc.withResolved(ListUtils.concat(
                            ListUtils.map(gdc.getResolved(), resolved -> {
                                // Remove any existing dependency with the same group and artifact id
                                if (Objects.equals(resolved.getGroupId(), resolvedGav.getGroupId()) && Objects.equals(resolved.getArtifactId(), resolvedGav.getArtifactId())) {
                                    return null;
                                }
                                return resolved;
                            }),
                            new ResolvedDependency(null, resolvedGav, newRequested, transitiveDependencies,
                                    emptyList(), "jar",  classifier, null, 0, null)));
                }
                newNameToConfiguration.put(newGdc.getName(), newGdc);
            }
            gp = gp.withNameToConfiguration(newNameToConfiguration);
        } catch (MavenDownloadingException | MavenDownloadingExceptions | IllegalArgumentException e) {
            return gp;
        }
        return gp;
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
