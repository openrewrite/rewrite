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
import org.openrewrite.gradle.marker.GradleDependencyConstraint;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleMultiDependency;
import org.openrewrite.gradle.trait.SpringDependencyManagementPluginEntry;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.*;

import java.util.*;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;

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
            if (gdc == null || gdc.findRequestedDependency(groupId, artifactId) != null) {
                return sourceFile;
            }

            isKotlinDsl = sourceFile instanceof K.CompilationUnit;
            if (version != null) {
                if (version.startsWith("$")) {
                    resolvedVersion = version;
                } else {
                    // Check if the dependency is managed by dependency management before resolving version
                    String managedVersion = findManagedVersion(sourceFile, ctx);
                    if (managedVersion != null) {
                        // Version is managed, don't add explicit version
                        resolvedVersion = null;
                    } else {
                        try {
                            resolvedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                                    .select(new GroupArtifact(groupId, artifactId), configuration, version, versionPattern, ctx);
                        } catch (MavenDownloadingException e) {
                            return (J) e.warn(tree);
                        }
                    }
                }
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
     * Check if the dependency version is managed by Spring dependency management plugin or platform BOMs.
     *
     * @return The managed version if found, null otherwise
     */
    private @Nullable String findManagedVersion(JavaSourceFile sourceFile, ExecutionContext ctx) {
        if (gradleProject == null) {
            return null;
        }

        // Check Spring dependency management plugin
        String springManagedVersion = findSpringDependencyManagementVersion(sourceFile, ctx);
        if (springManagedVersion != null) {
            return springManagedVersion;
        }

        // Check platform dependencies
        return findPlatformManagedVersion(sourceFile, ctx);
    }

    private @Nullable String findSpringDependencyManagementVersion(JavaSourceFile sourceFile, ExecutionContext ctx) {
        if (gradleProject == null) {
            return null;
        }

        boolean hasSpringDependencyManagementPlugin = gradleProject.getPlugins().stream()
                .anyMatch(plugin -> "io.spring.dependency-management".equals(plugin.getId()));

        if (!hasSpringDependencyManagementPlugin) {
            return null;
        }

        Map<GroupArtifact, String> springPluginManagedDependencies = new HashMap<>();

        // Try to get Spring Boot version from plugin classpath or configurations
        String springBootVersion = getSpringBootVersionFromPlugin();
        if (springBootVersion == null) {
            springBootVersion = getSpringBootVersionFromConfiguration("testRuntimeClasspath");
        }
        if (springBootVersion == null) {
            for (String configName : gradleProject.getNameToConfiguration().keySet()) {
                springBootVersion = getSpringBootVersionFromConfiguration(configName);
                if (springBootVersion != null) {
                    break;
                }
            }
        }

        if (springBootVersion != null) {
            MavenPomDownloader mpd = new MavenPomDownloader(ctx);
            try {
                ResolvedPom platformPom = mpd.download(
                                new GroupArtifactVersion("org.springframework.boot", "spring-boot-dependencies", springBootVersion),
                                null, null, gradleProject.getMavenRepositories())
                        .resolve(emptyList(), mpd, ctx);
                platformPom.getDependencyManagement().stream()
                        .filter(managedVersion -> managedVersion.getVersion() != null)
                        .forEach(managedVersion -> springPluginManagedDependencies.put(
                                managedVersion.getGav().asGroupArtifact(), managedVersion.getVersion()));
            } catch (MavenDownloadingException ignored) {
            }
        }

        // Also check for mavenBom imports in dependencyManagement block
        new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                new SpringDependencyManagementPluginEntry.Matcher().get(getCursor()).ifPresent(entry ->
                        entry.getArtifacts().forEach(artifact -> {
                            if ("mavenBom".equals(method.getSimpleName())) {
                                MavenPomDownloader mpd = new MavenPomDownloader(ctx);
                                try {
                                    ResolvedPom platformPom = mpd.download(
                                                    new GroupArtifactVersion(entry.getGroup(), artifact, entry.getVersion()),
                                                    null, null, gradleProject.getMavenRepositories())
                                            .resolve(emptyList(), mpd, ctx);
                                    platformPom.getDependencyManagement().stream()
                                            .filter(managedVersion -> managedVersion.getVersion() != null)
                                            .forEach(managedVersion -> springPluginManagedDependencies.put(
                                                    managedVersion.getGav().asGroupArtifact(), managedVersion.getVersion()));
                                } catch (MavenDownloadingException ignored) {
                                }
                            } else {
                                springPluginManagedDependencies.put(new GroupArtifact(entry.getGroup(), artifact), entry.getVersion());
                            }
                        }));
                return m;
            }
        }.visit(sourceFile, ctx);

        return springPluginManagedDependencies.get(new GroupArtifact(groupId, artifactId));
    }

    private @Nullable String getSpringBootVersionFromPlugin() {
        if (gradleProject == null) {
            return null;
        }
        GradleDependencyConfiguration gdc = gradleProject.getBuildscript().getConfiguration("classpath");
        if (gdc != null) {
            for (ResolvedDependency dependency : gdc.getDirectResolved()) {
                if ("org.springframework.boot.gradle.plugin".equals(dependency.getArtifactId())) {
                    return dependency.getVersion();
                }
            }
        }
        return null;
    }

    private @Nullable String getSpringBootVersionFromConfiguration(String configurationName) {
        if (gradleProject == null) {
            return null;
        }
        GradleDependencyConfiguration gdc = gradleProject.getConfiguration(configurationName);
        if (gdc != null) {
            for (GradleDependencyConstraint constraint : gdc.getConstraints()) {
                if ("org.springframework.boot".equals(constraint.getGroupId()) && constraint.getStrictVersion() != null) {
                    return constraint.getStrictVersion();
                }
            }
            for (ResolvedDependency dependency : gdc.getResolved()) {
                if (dependency.getRequested().getVersion() == null && "org.springframework.boot".equals(dependency.getGroupId())) {
                    return dependency.getVersion();
                }
            }
        }
        return null;
    }

    private @Nullable String findPlatformManagedVersion(JavaSourceFile sourceFile, ExecutionContext ctx) {
        if (gradleProject == null) {
            return null;
        }

        Map<String, List<ResolvedPom>> platforms = new HashMap<>();

        // Find all platform dependencies
        GradleMultiDependency.matcher()
                .asVisitor(gmd -> gmd.map(gradleDependency -> {
                    if (!gradleDependency.isPlatform()) {
                        return gradleDependency.getTree();
                    }
                    GroupArtifactVersion gav = gradleDependency.getGav();
                    MavenPomDownloader mpd = new MavenPomDownloader(ctx);
                    try {
                        ResolvedPom platformPom = mpd.download(gav, null, null, gradleProject.getMavenRepositories())
                                .resolve(emptyList(), mpd, ctx);
                        platforms.computeIfAbsent(gradleDependency.getConfigurationName(), k -> new ArrayList<>())
                                .add(platformPom);
                    } catch (MavenDownloadingException ignored) {
                    }
                    return gradleDependency.getTree();
                }))
                .visit(sourceFile, ctx);

        // Check if dependency is managed by any platform in the target configuration or its ancestors
        if (platforms.containsKey(configuration)) {
            for (ResolvedPom platform : platforms.get(configuration)) {
                String managedVersion = platform.getManagedVersion(groupId, artifactId, null, classifier);
                if (managedVersion != null) {
                    return managedVersion;
                }
            }
        }

        // Also check configurations that the target configuration extends from
        GradleDependencyConfiguration gdc = gradleProject.getConfiguration(configuration);
        if (gdc != null) {
            for (GradleDependencyConfiguration parentConfig : gdc.allExtendsFrom()) {
                if (platforms.containsKey(parentConfig.getName())) {
                    for (ResolvedPom platform : platforms.get(parentConfig.getName())) {
                        String managedVersion = platform.getManagedVersion(groupId, artifactId, null, classifier);
                        if (managedVersion != null) {
                            return managedVersion;
                        }
                    }
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
