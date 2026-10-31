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

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markup;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.Version;

import java.util.function.Predicate;

@RequiredArgsConstructor
public class AddDependencyVisitor extends JavaIsoVisitor<ExecutionContext> {
    private final String groupId;
    private final String artifactId;

    @Nullable
    private final String version;

    @Nullable
    private final String versionPattern;

    private final String configuration;

    @Nullable
    private final String classifier;

    @Nullable
    private final String extension;

    @Nullable
    private final MavenMetadataFailures metadataFailures;

    @Nullable
    private String resolvedVersion;

    @Nullable
    private final Predicate<Cursor> insertPredicate;

    @Nullable
    private final DependencyModifier dependencyModifier;

    @Nullable
    private transient GradleProject gradleProject;

    private transient boolean isKotlinDsl;

    @Override
    public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
        if (tree instanceof JavaSourceFile) {
            JavaSourceFile sourceFile = (JavaSourceFile) tree;
            gradleProject = sourceFile.getMarkers().findFirst(GradleProject.class).orElse(null);
            if (gradleProject == null) {
                return sourceFile;
            }

            GradleDependencyConfiguration gdc = gradleProject.getConfiguration(configuration);
            if (gdc == null) {
                return Markup.warn(sourceFile, new IllegalStateException(
                        "GradleProject has no configuration named '" + configuration +
                                "'. Available configurations: " + gradleProject.getNameToConfiguration().keySet()));
            }
            isKotlinDsl = sourceFile instanceof K.CompilationUnit;
            if (version != null) {
                if (version.startsWith("$")) {
                    resolvedVersion = version;
                } else {
                    try {
                        resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                .select(new GroupArtifact(groupId, artifactId), configuration, version, versionPattern, ctx);
                    } catch (MavenDownloadingException e) {
                        return (J) e.warn(tree);
                    }
                }
            }

            // Check if the dependency already exists on this configuration or a related one
            // that feeds into the same resolution classpath (e.g. implementation vs api both
            // feed into compileClasspath). This avoids adding duplicate declarations and instead
            // upgrades the existing one if the requested version is newer.
            GradleDependencyConfiguration existingConfig = findConfigurationWithDependency(gdc);
            if (existingConfig != null) {
                if (resolvedVersion != null && !resolvedVersion.startsWith("$")) {
                    String existingVersion = resolveExistingVersion(existingConfig);
                    if (existingVersion != null &&
                            new Version(resolvedVersion).compareTo(new Version(existingVersion)) > 0) {
                        String upgradeVersion = resolvedVersion;
                        String existingConfigName = existingConfig.getName();
                        sourceFile = (JavaSourceFile) new GradleDependency.Matcher()
                                .configuration(existingConfigName)
                                .groupId(groupId)
                                .artifactId(artifactId)
                                .asVisitor(dep -> dep.withDeclaredVersion(upgradeVersion).getTree())
                                .visitNonNull(sourceFile, ctx);
                        if (sourceFile != tree) {
                            sourceFile = org.openrewrite.gradle.internal.AddDependencyVisitor.addDependency(
                                    sourceFile,
                                    gradleProject.getConfiguration(existingConfigName),
                                    new GroupArtifactVersion(groupId, artifactId, resolvedVersion),
                                    classifier,
                                    ctx
                            );
                        }
                    }
                }
                return sourceFile;
            }

            sourceFile = (JavaSourceFile) new org.openrewrite.gradle.internal.AddDependencyVisitor(configuration, groupId, artifactId, resolvedVersion, classifier, extension, insertPredicate, dependencyModifier, isKotlinDsl)
                    .visitNonNull(sourceFile, ctx);

            if (sourceFile != tree) {
                String versionWithPattern = StringUtils.isBlank(resolvedVersion) || resolvedVersion.startsWith("$") ? null : resolvedVersion;
                sourceFile = org.openrewrite.gradle.internal.AddDependencyVisitor.addDependency(
                        sourceFile,
                        gradleProject.getConfiguration(configuration),
                        new GroupArtifactVersion(groupId, artifactId, versionWithPattern),
                        classifier,
                        ctx
                );
            }

            return sourceFile;
        }
        return (J) tree;
    }

    /**
     * Find the configuration where the dependency is already declared. Checks the target
     * configuration first, then sibling declaration configurations that share resolution
     * classpaths (e.g. api and implementation both feed into compileClasspath).
     */
    private @Nullable GradleDependencyConfiguration findConfigurationWithDependency(GradleDependencyConfiguration targetConfig) {
        // Check exact configuration first
        if (targetConfig.findRequestedDependency(groupId, artifactId) != null) {
            return targetConfig;
        }
        // Check sibling configurations: find resolvable configs that extend from the target,
        // then look at what other declaration configs also feed into those same resolvable configs
        for (GradleDependencyConfiguration resolvable : gradleProject.configurationsExtendingFrom(targetConfig, true)) {
            if (!resolvable.isCanBeResolved()) {
                continue;
            }
            // Check all declaration configs that feed into this resolvable config
            for (GradleDependencyConfiguration sibling : gradleProject.getNameToConfiguration().values()) {
                if (sibling == targetConfig || sibling.isCanBeResolved()) {
                    continue;
                }
                // Check if this sibling feeds into the same resolvable config
                if (gradleProject.configurationsExtendingFrom(sibling, true).contains(resolvable) &&
                        sibling.findRequestedDependency(groupId, artifactId) != null) {
                    return sibling;
                }
            }
        }
        return null;
    }

    /**
     * Resolve the existing version of the dependency from the given configuration.
     * First checks the requested dependency version, then falls back to resolved
     * dependencies in resolvable extending configurations.
     */
    private @Nullable String resolveExistingVersion(GradleDependencyConfiguration existingConfig) {
        Dependency existingDep = existingConfig.findRequestedDependency(groupId, artifactId);
        if (existingDep != null && existingDep.getVersion() != null) {
            return existingDep.getVersion();
        }
        for (GradleDependencyConfiguration ext : gradleProject.configurationsExtendingFrom(existingConfig, true)) {
            if (ext.isCanBeResolved()) {
                org.openrewrite.maven.tree.ResolvedDependency rd = ext.findResolvedDependency(groupId, artifactId);
                if (rd != null) {
                    return rd.getVersion();
                }
            }
        }
        return null;
    }

    public enum DependencyModifier {
        PLATFORM,
        ENFORCED_PLATFORM;
    }
}
