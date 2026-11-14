/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.gradle.internal;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleDependencyConstraint;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.SpringDependencyManagementPlugin;
import org.openrewrite.gradle.trait.GradleMultiDependency;
import org.openrewrite.gradle.trait.SpringDependencyManagementPluginEntry;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedPom;

import java.util.*;

import static java.util.Collections.emptyList;

/**
 * Versions a build script manages through a Gradle {@code platform}/{@code enforcedPlatform} or the Spring
 * dependency management plugin.
 */
public class ManagedDependencyVersions {
    private final GradleProject gradleProject;
    private final JavaSourceFile buildScript;
    private final ExecutionContext ctx;

    // Platforms by the configuration declaring them
    @Getter
    private final Map<String, List<ResolvedPom>> platforms = new HashMap<>();

    private final Map<GroupArtifact, @Nullable String> springPluginManaged = new HashMap<>();
    private boolean springPluginManagedCollected;

    @Getter
    private @Nullable MavenDownloadingException platformFailure;

    private ManagedDependencyVersions(GradleProject gradleProject, JavaSourceFile buildScript, ExecutionContext ctx) {
        this.gradleProject = gradleProject;
        this.buildScript = buildScript;
        this.ctx = ctx;
    }

    public static ManagedDependencyVersions of(JavaSourceFile buildScript, GradleProject gradleProject, ExecutionContext ctx) {
        ManagedDependencyVersions managed = new ManagedDependencyVersions(gradleProject, buildScript, ctx);
        managed.collectPlatforms(buildScript, ctx);
        return managed;
    }

    /**
     * Versions the Spring dependency management plugin manages, as read from the build script.
     */
    public Map<GroupArtifact, @Nullable String> getSpringPluginManaged() {
        if (!springPluginManagedCollected) {
            springPluginManagedCollected = true;
            collectSpringPluginManaged(buildScript, ctx);
        }
        return springPluginManaged;
    }

    /**
     * The version managed for a dependency declared in {@code configuration}, considering platforms declared in it and
     * in the configurations it extends from.
     */
    public @Nullable String getManagedVersion(String configuration, String groupId, String artifactId, @Nullable String classifier) {
        // The plugin's own state, on LSTs that record it, has placeholders resolved and includes programmatic BOM imports
        SpringDependencyManagementPlugin plugin = gradleProject.getSpringDependencyManagementPlugin();
        String springManaged = plugin != null ?
                plugin.getManagedVersions().get(groupId + ":" + artifactId) :
                getSpringPluginManaged().get(new GroupArtifact(groupId, artifactId));
        if (springManaged != null) {
            return springManaged;
        }
        List<String> configurations = new ArrayList<>();
        configurations.add(configuration);
        GradleDependencyConfiguration gdc = gradleProject.getConfiguration(configuration);
        if (gdc != null) {
            for (GradleDependencyConfiguration extended : gdc.allExtendsFrom()) {
                configurations.add(extended.getName());
            }
        }
        for (String c : configurations) {
            for (ResolvedPom platform : platforms.getOrDefault(c, emptyList())) {
                String managedVersion = platform.getManagedVersion(groupId, artifactId, null, classifier);
                if (managedVersion != null) {
                    return managedVersion;
                }
            }
        }
        return null;
    }

    private void collectSpringPluginManaged(JavaSourceFile buildScript, ExecutionContext ctx) {
        if (!SpringBomProperty.isPluginApplied(gradleProject)) {
            return;
        }
        String springBootVersion = Optional.ofNullable(getSpringBootVersionFromPlugin()).orElseGet(() ->
                Optional.ofNullable(getSpringBootVersionFromConfiguration("testRuntimeClasspath")).orElseGet(() ->
                        gradleProject.getNameToConfiguration().keySet().stream()
                                .map(this::getSpringBootVersionFromConfiguration)
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElse(null)));
        if (springBootVersion != null) {
            addSpringPluginManagedBom(new GroupArtifactVersion("org.springframework.boot", "spring-boot-dependencies", springBootVersion), ctx);
        }

        new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                new SpringDependencyManagementPluginEntry.Matcher().get(getCursor()).ifPresent(entry -> entry.getArtifacts().forEach(artifact -> {
                    if ("mavenBom".equals(method.getSimpleName())) {
                        String version = entry.getVersion();
                        if (version == null || version.contains("${")) {
                            return;
                        }
                        addSpringPluginManagedBom(new GroupArtifactVersion(entry.getGroup(), artifact, version), ctx);
                    } else {
                        springPluginManaged.put(new GroupArtifact(entry.getGroup(), artifact), entry.getVersion());
                    }
                }));
                return m;
            }
        }.visit(buildScript, ctx);
    }

    private void addSpringPluginManagedBom(GroupArtifactVersion bom, ExecutionContext ctx) {
        MavenPomDownloader mpd = new MavenPomDownloader(ctx);
        try {
            mpd.download(bom, null, null, gradleProject.getMavenRepositories())
                    .resolve(emptyList(), mpd, ctx)
                    .getDependencyManagement().stream()
                    .filter(managedVersion -> managedVersion.getVersion() != null)
                    .forEach(managedVersion -> springPluginManaged.put(managedVersion.getGav().asGroupArtifact(), managedVersion.getVersion()));
        } catch (MavenDownloadingException ignored) {
        }
    }

    private void collectPlatforms(JavaSourceFile buildScript, ExecutionContext ctx) {
        MavenPomDownloader mpd = new MavenPomDownloader(ctx);
        GradleMultiDependency.matcher()
                .asVisitor(gmd -> gmd.map(gradleDependency -> {
                    if (gradleDependency.isPlatform()) {
                        try {
                            ResolvedPom platformPom = mpd.download(gradleDependency.getGav(), null, null, gradleProject.getMavenRepositories())
                                    .resolve(emptyList(), mpd, ctx);
                            platforms.computeIfAbsent(gradleDependency.getConfigurationName(), k -> new ArrayList<>()).add(platformPom);
                        } catch (MavenDownloadingException e) {
                            if (platformFailure == null) {
                                platformFailure = e;
                            }
                        }
                    }
                    return gradleDependency.getTree();
                }))
                .visit(buildScript, ctx);
    }

    private @Nullable String getSpringBootVersionFromPlugin() {
        GradleDependencyConfiguration gdc = gradleProject.getBuildscript().getConfiguration("classpath");
        if (gdc != null) {
            for (ResolvedDependency dependency : gdc.getDirectResolvedShallow()) {
                if ("org.springframework.boot.gradle.plugin".equals(dependency.getArtifactId())) {
                    return dependency.getVersion();
                }
            }
        }
        return null;
    }

    private @Nullable String getSpringBootVersionFromConfiguration(String configuration) {
        GradleDependencyConfiguration gdc = gradleProject.getConfiguration(configuration);
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
}
