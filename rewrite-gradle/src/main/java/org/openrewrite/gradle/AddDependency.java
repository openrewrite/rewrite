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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.search.FindJVMTestSuites;
import org.openrewrite.gradle.trait.JvmTestSuite;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.semver.Semver;

import java.util.*;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddDependency extends ScanningRecipe<AddDependency.Scanned> {

    @EqualsAndHashCode.Exclude
    MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate 'com.google.guava:guava:VERSION'.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate 'com.google.guava:guava:VERSION'",
            example = "guava")
    String artifactId;

    @Option(displayName = "Version",
            description = "An exact version number or node-style semver selector used to select the version number. " +
                    "You can also use `latest.release` for the latest available version and `latest.patch` if " +
                    "the current version is a valid semantic version. For more details, you can look at the documentation " +
                    "page of [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors).",
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
            example = "org.junit.jupiter.api.*",
            required = false)
    @Nullable
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

    @Option(displayName = "Accept transitive",
            description = "Default false. If enabled, the dependency will not be added if it is already on the classpath as a transitive dependency.",
            example = "true",
            required = false)
    @Nullable
    Boolean acceptTransitive;

    @Override
    public String getDisplayName() {
        return "Add Gradle dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s:%s`", groupId, artifactId, version);
    }

    @Override
    public String getDescription() {
        return "Add a gradle dependency to a `build.gradle` file in the correct configuration based on where it is used.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (version != null) {
            validated = validated.and(Semver.validate(version, versionPattern));
        }
        return validated;
    }

    public static class Scanned {
        Map<JavaProject, Boolean> usingType = new HashMap<>();
        Map<JavaProject, Set<String>> configurationsByProject = new HashMap<>();
    }

    @Override
    public Scanned getInitialValue(ExecutionContext ctx) {
        return new Scanned();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Scanned acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {

            @Nullable
            UsesType<ExecutionContext> usesType = null;

            private boolean usesType(SourceFile sourceFile, ExecutionContext ctx) {
                if (onlyIfUsing == null) {
                    return true;
                }
                if (usesType == null) {
                    usesType = new UsesType<>(onlyIfUsing, true);
                }
                return usesType.isAcceptable(sourceFile, ctx) && usesType.visit(sourceFile, ctx) != sourceFile;
            }

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                sourceFile.getMarkers().findFirst(JavaProject.class).ifPresent(javaProject -> {
                    acc.usingType.compute(javaProject, (jp, usingType) -> Boolean.TRUE.equals(usingType) || usesType(sourceFile, ctx));

                    Set<String> configurations = acc.configurationsByProject.computeIfAbsent(javaProject, ignored -> new HashSet<>());
                    sourceFile.getMarkers().findFirst(JavaSourceSet.class).ifPresent(sourceSet ->
                            configurations.add("main".equals(sourceSet.getName()) ? "implementation" : sourceSet.getName() + "Implementation"));
                });
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Scanned acc) {
        return Preconditions.check(!acc.configurationsByProject.isEmpty(),
                Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {

                    @Override
                    public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                        if (!(tree instanceof JavaSourceFile)) {
                            return (J) tree;
                        }
                        JavaSourceFile s = (JavaSourceFile) tree;
                        Optional<JavaProject> maybeJp = s.getMarkers().findFirst(JavaProject.class);
                        Optional<GradleProject> maybeGp = s.getMarkers().findFirst(GradleProject.class);
                        if (!maybeJp.isPresent() ||
                                (onlyIfUsing != null && !acc.usingType.getOrDefault(maybeJp.get(), false)) || !acc.configurationsByProject.containsKey(maybeJp.get()) ||
                                !maybeGp.isPresent()) {
                            return s;
                        }

                        JavaProject jp = maybeJp.get();
                        GradleProject gp = maybeGp.get();

                        Set<String> resolvedConfigurations = StringUtils.isBlank(configuration) ?
                                acc.configurationsByProject.getOrDefault(jp, new HashSet<>()) :
                                new HashSet<>(singletonList(configuration));
                        if (resolvedConfigurations.isEmpty()) {
                            resolvedConfigurations.add("implementation");
                        }

                        GradleConfigurationFilter gradleConfigurationFilter = new GradleConfigurationFilter(gp, resolvedConfigurations);
                        gradleConfigurationFilter.removeTransitiveConfigurations();
                        gradleConfigurationFilter.removeConfigurationsContainingDependency(new GroupArtifact(groupId, artifactId));
                        gradleConfigurationFilter.removeConfigurationsContainingTransitiveDependency(new GroupArtifact(groupId, artifactId));
                        resolvedConfigurations = gradleConfigurationFilter.getFilteredConfigurations();

                        if (resolvedConfigurations.isEmpty()) {
                            return s;
                        }

                        Set<JvmTestSuite> jvmTestSuites = FindJVMTestSuites.jvmTestSuites(s);
                        for (String resolvedConfiguration : resolvedConfigurations) {
                            JvmTestSuite jvmTestSuite = maybeJvmTestSuite(resolvedConfiguration, jvmTestSuites);
                            if (jvmTestSuite != null) {
                                s = (JavaSourceFile) jvmTestSuite.addDependency(resolvedConfiguration, groupId, artifactId, version, versionPattern, classifier, extension, metadataFailures, null, ctx)
                                        .visitNonNull(s, ctx);
                            } else {
                                s = (JavaSourceFile) new AddDependencyVisitor(groupId, artifactId, version, versionPattern, resolvedConfiguration,
                                        classifier, extension, metadataFailures, this::isTopLevel, null).visitNonNull(s, ctx);
                            }
                        }

                        return s;
                    }

                    private boolean isTopLevel(Cursor cursor) {
                        if (cursor.getValue() instanceof J.Block) {
                            return cursor.getParentOrThrow().getValue() instanceof JavaSourceFile;
                        }
                        return cursor.getParentOrThrow().firstEnclosing(J.MethodInvocation.class) == null;
                    }

                    private @Nullable JvmTestSuite maybeJvmTestSuite(String configuration, Set<JvmTestSuite> jvmTestSuites) {
                        for (JvmTestSuite jvmTestSuite : jvmTestSuites) {
                            if (jvmTestSuite.isAcceptable(configuration)) {
                                return jvmTestSuite;
                            }
                        }
                        return null;
                    }
                })
        );
    }
}
