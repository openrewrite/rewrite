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
import org.openrewrite.gradle.internal.ManagedDependencyVersions;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markup;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.Version;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

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
            if (gdc.findRequestedDependency(groupId, artifactId) != null) {
                return sourceFile;
            }

            isKotlinDsl = sourceFile instanceof K.CompilationUnit;
            String resolvedVersion = null;
            String managedVersion = null;
            if (version != null) {
                if (version.startsWith("$")) {
                    resolvedVersion = version;
                } else {
                    String managed = dependencyModifier == null ?
                            ManagedDependencyVersions.of(sourceFile, gradleProject, ctx).getManagedVersion(configuration, groupId, artifactId, classifier) :
                            null;
                    VersionComparator versionComparator = Semver.validate(version, versionPattern).getValue();
                    if (managed == null || versionComparator == null || !versionComparator.isValid(null, managed)) {
                        try {
                            resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                    .select(new GroupArtifact(groupId, artifactId), configuration, version, versionPattern, ctx);
                        } catch (MavenDownloadingException e) {
                            return (J) e.warn(tree);
                        }
                    }
                    // A managed version as new as the selected one makes an explicit version redundant
                    if (managed != null && (resolvedVersion == null ||
                            new Version(managed).compareTo(new Version(resolvedVersion)) >= 0)) {
                        managedVersion = managed;
                        resolvedVersion = null;
                    }
                }
            }

            sourceFile = (JavaSourceFile) new org.openrewrite.gradle.internal.AddDependencyVisitor(configuration, groupId, artifactId, resolvedVersion, classifier, extension, insertPredicate, dependencyModifier, isKotlinDsl)
                    .visitNonNull(sourceFile, ctx);

            if (sourceFile != tree) {
                String versionWithPattern = managedVersion != null ? managedVersion :
                        StringUtils.isBlank(resolvedVersion) || resolvedVersion.startsWith("$") ? null : resolvedVersion;
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

    public enum DependencyModifier {
        PLATFORM,
        ENFORCED_PLATFORM;
    }
}
