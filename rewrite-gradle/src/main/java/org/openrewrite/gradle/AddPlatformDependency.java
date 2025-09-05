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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.semver.Semver;

import java.util.*;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static org.openrewrite.gradle.AddDependencyVisitor.DependencyModifier.ENFORCED_PLATFORM;
import static org.openrewrite.gradle.AddDependencyVisitor.DependencyModifier.PLATFORM;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddPlatformDependency extends ScanningRecipe<AddPlatformDependency.Scanned> {

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
                    "is used when adding a new, as yet unused, dependency.",
            example = "implementation",
            required = false)
    @Nullable
    String configuration;

    @Option(displayName = "Enforced",
            description = "Used to determine whether the platform dependency should be enforcedPlatform.",
            example = "true",
            required = false)
    @Nullable
    Boolean enforced;

    @Override
    public String getDisplayName() {
        return "Add Gradle platform dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s:%s`", groupId, artifactId, version);
    }

    @Override
    public String getDescription() {
        return "Add a gradle platform dependency to a `build.gradle` file in the correct configuration based on where it is used.";
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
        Map<JavaProject, Set<String>> configurationsByProject = new HashMap<>();
    }

    @Override
    public Scanned getInitialValue(ExecutionContext ctx) {
        return new Scanned();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Scanned acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                sourceFile.getMarkers().findFirst(JavaProject.class).ifPresent(javaProject -> {
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
                        if (!maybeJp.isPresent() || !acc.configurationsByProject.containsKey(maybeJp.get()) || !maybeGp.isPresent()) {
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
                        resolvedConfigurations = gradleConfigurationFilter.getFilteredConfigurations();

                        if (resolvedConfigurations.isEmpty()) {
                            return s;
                        }

                        Set<JvmTestSuite> jvmTestSuites = FindJVMTestSuites.jvmTestSuites(s);
                        AddDependencyVisitor.DependencyModifier modifier = TRUE.equals(enforced) ? ENFORCED_PLATFORM : PLATFORM;
                        for (String resolvedConfiguration : resolvedConfigurations) {
                            JvmTestSuite jvmTestSuite = maybeJvmTestSuite(resolvedConfiguration, jvmTestSuites);
                            if (jvmTestSuite != null) {
                                s = (JavaSourceFile) jvmTestSuite.addDependency(resolvedConfiguration, groupId, artifactId, version, versionPattern,
                                                null, null, metadataFailures, modifier, ctx).visitNonNull(s, ctx);
                            } else {
                                s = (JavaSourceFile) new AddDependencyVisitor(groupId, artifactId, version, versionPattern, resolvedConfiguration,
                                        null, null, metadataFailures, this::isTopLevel, modifier).visitNonNull(s, ctx);
                            }
                        }

                        return s;
                    }

                    private boolean isTopLevel(Cursor cursor) {
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
                }));
    }
}
