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
package org.openrewrite.gradle.marker;

import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.HasAttributes;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.plugins.PluginManagerInternal;
import org.gradle.api.plugins.PluginManager;
import org.gradle.invocation.DefaultGradle;
import org.gradle.plugin.use.PluginId;
import org.gradle.util.GradleVersion;
import org.jspecify.annotations.Nullable;
import org.openrewrite.gradle.attributes.Category;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.gradle.marker.GradleSettingsBuilder.GRADLE_PLUGIN_PORTAL;

public final class GradleProjectBuilder {

    private GradleProjectBuilder() {
    }

    public static GradleProject gradleProject(Project project) {
        Set<MavenRepository> pluginRepositories = new HashSet<>();
        if (GradleVersion.current().compareTo(GradleVersion.version("4.4")) >= 0) {
            Settings settings = ((DefaultGradle) project.getGradle()).getSettings();
            pluginRepositories.addAll(mapRepositories(settings.getPluginManagement().getRepositories()));
            pluginRepositories.addAll(mapRepositories(settings.getBuildscript().getRepositories()));
        }
        List<ArtifactRepository> repositories = new ArrayList<>(project.getRepositories());
        if (GradleVersion.current().compareTo(GradleVersion.version("6.8")) >= 0) {
            Settings settings = ((DefaultGradle) project.getGradle()).getSettings();
            //noinspection UnstableApiUsage
            repositories.addAll(settings.getDependencyResolutionManagement().getRepositories());
        }
        pluginRepositories.addAll(mapRepositories(project.getBuildscript().getRepositories()));
        if (pluginRepositories.isEmpty()) {
            pluginRepositories.add(GRADLE_PLUGIN_PORTAL);
        }

        return new GradleProject(randomId(),
                project.getGroup().toString(),
                project.getName(),
                project.getVersion().toString(),
                project.getPath(),
                GradleProjectBuilder.pluginDescriptors(project.getPluginManager()),
                mapRepositories(repositories),
                null,
                GradleProjectBuilder.dependencyConfigurations(project.getConfigurations()),
                new GradleBuildscript(
                        randomId(),
                        new ArrayList<>(pluginRepositories),
                        GradleProjectBuilder.dependencyConfigurations(project.getBuildscript().getConfigurations())
                ));
    }

    static List<MavenRepository> mapRepositories(List<ArtifactRepository> repositories) {
        return repositories.stream()
                .filter(MavenArtifactRepository.class::isInstance)
                .map(MavenArtifactRepository.class::cast)
                .map(repo -> MavenRepository.builder()
                        .id(repo.getName())
                        .uri(repo.getUrl().toString())
                        .releases(true)
                        .snapshots(true)
                        .build())
                .collect(toList());
    }

    public static List<GradlePluginDescriptor> pluginDescriptors(@Nullable PluginManager pluginManager) {
        if (pluginManager instanceof PluginManagerInternal) {
            return pluginDescriptors((PluginManagerInternal) pluginManager);
        }
        return emptyList();
    }

    public static List<GradlePluginDescriptor> pluginDescriptors(PluginManagerInternal pluginManager) {
        return pluginManager.getPluginContainer().stream()
                .map(plugin -> new GradlePluginDescriptor(
                        plugin.getClass().getName(),
                        pluginIdForClass(pluginManager, plugin.getClass())))
                .collect(toList());
    }

    private static @Nullable String pluginIdForClass(PluginManagerInternal pluginManager, Class<?> pluginClass) {
        try {
            Method findPluginIdForClass = PluginManagerInternal.class.getMethod("findPluginIdForClass", Class.class);
            //noinspection unchecked
            Optional<PluginId> maybePluginId = (Optional<PluginId>) findPluginIdForClass.invoke(pluginManager, pluginClass);
            return maybePluginId.map(PluginId::getId).orElse(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // On old versions of gradle that don't have this method, returning null is fine
        }
        return null;
    }

    private static boolean isCanBeDeclared(Configuration configuration) {
        try {
            Method isCanBeDeclared = Configuration.class.getMethod("isCanBeDeclared");
            return (boolean) isCanBeDeclared.invoke(configuration);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // On old versions of gradle that don't have this method will fall back on a heuristic
            // Commonly not-for-declaration configurations include "compileClasspath", "TestRuntimeClasspath"
            // But _not_ included is the buildscript "classpath" configuration
            return !configuration.getName().endsWith("Classpath");
        }
    }

    private static final Map<GroupArtifact, GroupArtifact> groupArtifactCache = new ConcurrentHashMap<>();

    private static GroupArtifact groupArtifact(org.openrewrite.maven.tree.Dependency dep) {
        //noinspection ConstantConditions
        return groupArtifactCache.computeIfAbsent(new GroupArtifact(dep.getGroupId(), dep.getArtifactId()), it -> it);
    }

    private static GroupArtifact groupArtifact(ResolvedDependency dep) {
        return groupArtifactCache.computeIfAbsent(new GroupArtifact(dep.getModuleGroup(), dep.getModuleName()), it -> it);
    }

    private static final Map<GroupArtifactVersion, GroupArtifactVersion> groupArtifactVersionCache = new ConcurrentHashMap<>();

    private static GroupArtifactVersion groupArtifactVersion(ResolvedDependency dep) {
        return groupArtifactVersionCache.computeIfAbsent(
                new GroupArtifactVersion(dep.getModuleGroup(), dep.getModuleName(), unspecifiedToNull(dep.getModuleVersion())),
                it -> it);
    }

    private static GroupArtifactVersion groupArtifactVersion(Dependency dep) {
        return groupArtifactVersionCache.computeIfAbsent(
                new GroupArtifactVersion(dep.getGroup(), dep.getName(), unspecifiedToNull(dep.getVersion())), it -> it);
    }

    private static final Map<ResolvedGroupArtifactVersion, ResolvedGroupArtifactVersion> resolvedGroupArtifactVersionCache = new ConcurrentHashMap<>();

    private static ResolvedGroupArtifactVersion resolvedGroupArtifactVersion(ResolvedDependency dep) {
        return resolvedGroupArtifactVersionCache.computeIfAbsent(new ResolvedGroupArtifactVersion(
                        null, dep.getModuleGroup(), dep.getModuleName(), dep.getModuleVersion(), null),
                it -> it);
    }

    /**
     * Some Gradle dependency functions will have the String "unspecified" to indicate a missing value.
     * Rewrite's dependency API represents these missing things as "null"
     */
    private static @Nullable String unspecifiedToNull(@Nullable String maybeUnspecified) {
        if ("unspecified".equals(maybeUnspecified)) {
            return null;
        }
        return maybeUnspecified;
    }

    static Map<String, GradleDependencyConfiguration> dependencyConfigurations(ConfigurationContainer configurationContainer) {
        Map<String, GradleDependencyConfiguration> results = new HashMap<>();
        List<Configuration> configurations = new ArrayList<>(configurationContainer);
        for (Configuration conf : configurations) {
            try {
                List<org.openrewrite.maven.tree.Dependency> requested = conf.getAllDependencies().stream()
                        .map(dep -> dependency(dep, conf))
                        .collect(toList());

                List<org.openrewrite.maven.tree.ResolvedDependency> resolved;
                Map<GroupArtifact, org.openrewrite.maven.tree.Dependency> gaToRequested = requested.stream()
                        .collect(toMap(GradleProjectBuilder::groupArtifact, dep -> dep, (a, b) -> a));
                String exceptionType = null;
                String exceptionMessage = null;
                // Archives and default are redundant with other configurations
                // Newer versions of gradle display warnings with long stack traces when attempting to resolve them
                // Some Scala plugin we don't care about creates configurations that, for some unknown reason, are difficult to resolve
                if (conf.isCanBeResolved() && !"archives".equals(conf.getName()) && !"default".equals(conf.getName()) && !conf.getName().startsWith("incrementalScalaAnalysis")) {
                    ResolvedConfiguration resolvedConf = conf.getResolvedConfiguration();
                    if (resolvedConf.hasError()) {
                        try {
                            resolvedConf.rethrowFailure();
                        } catch (ResolveException e) {
                            exceptionType = e.getClass().getName();
                            exceptionMessage = e.getMessage();
                        }
                    }
                    Map<GroupArtifact, ResolvedDependency> gaToResolved = resolvedConf.getFirstLevelModuleDependencies().stream()
                            .collect(toMap(GradleProjectBuilder::groupArtifact, dep -> dep, (a, b) -> a));
                    resolved = resolved(gaToRequested, gaToResolved);
                } else {
                    resolved = emptyList();
                }
                GradleDependencyConfiguration dc = new GradleDependencyConfiguration(conf.getName(), conf.getDescription(),
                        conf.isTransitive(), conf.isCanBeResolved(), conf.isCanBeConsumed(), isCanBeDeclared(conf), emptyList(), requested, resolved, exceptionType, exceptionMessage, constraints(configurationContainer, conf), attributes(conf));
                results.put(conf.getName(), dc);
            } catch (Exception e) {
                GradleDependencyConfiguration dc = new GradleDependencyConfiguration(conf.getName(), conf.getDescription(),
                        conf.isTransitive(), conf.isCanBeResolved(), conf.isCanBeConsumed(), isCanBeDeclared(conf), emptyList(), emptyList(), emptyList(), e.getClass().getName(), e.getMessage(), constraints(configurationContainer, conf), attributes(conf));
                results.put(conf.getName(), dc);
            }
        }

        // Record the relationships between dependency configurations
        for (Configuration conf : configurations) {
            if (conf.getExtendsFrom().isEmpty()) {
                continue;
            }
            GradleDependencyConfiguration dc = results.get(conf.getName());
            if (dc != null) {
                List<GradleDependencyConfiguration> extendsFrom = conf.getExtendsFrom().stream()
                        .map(it -> results.get(it.getName()))
                        .collect(toList());
                dc.unsafeSetExtendsFrom(extendsFrom);
            }
        }
        return results;
    }

    private static List<org.openrewrite.gradle.marker.GradleDependencyConstraint> constraints(ConfigurationContainer configurations, Configuration conf) {
        // Discover the results of other resolution strategy manipulation
        // Model them as synthetic constraints so we have knowledge of them for later GradleProject updates
        Set<GradleDependencyConstraint> inferredConstraints = new HashSet<>();
        if (conf.isCanBeResolved()) {
            try {
                // If conf has already been resolved it is an error to attach a new resolutionStrategy to it
                // So create a new configuration which can inherit everything we're interested in and resolve that
                String name = conf.getName() + "Inheritor";
                if (configurations.findByName(name) != null) {
                    // I'd rather use an anonymous, detached configuration but those are deprecated and don't extendFrom() correctly
                    name = name + new Random().nextInt();
                }
                Configuration inheritor = configurations.create(name);
                inheritor.extendsFrom(conf);
                inheritor.getResolutionStrategy().eachDependency(details -> {
                    ModuleVersionSelector target = details.getTarget();
                    if (!details.getRequested().equals(target)) {
                        inferredConstraints.add(GradleDependencyConstraint.builder()
                                .groupId(target.getGroup())
                                .artifactId(target.getName())
                                .strictVersion(target.getVersion())
                                .build());
                    }
                });
                inheritor.resolve();
            } catch (Exception e) {
                // this is more of a nice-to-have than an essential
            }
        }
        Set<GradleDependencyConstraint> configuredConstraints = conf.getDependencyConstraints().stream()
                .map(constraint -> new GradleDependencyConstraint(
                        constraint.getGroup(),
                        constraint.getName(),
                        constraint.getVersionConstraint().getRequiredVersion(),
                        constraint.getVersionConstraint().getPreferredVersion(),
                        constraint.getVersionConstraint().getStrictVersion(),
                        constraint.getVersionConstraint().getBranch(),
                        constraint.getReason(),
                        constraint.getVersionConstraint().getRejectedVersions()))
                .collect(toSet());

        return GradleDependencyConfiguration.merge(inferredConstraints, configuredConstraints);
    }

    private static final Map<GroupArtifactVersion, org.openrewrite.maven.tree.Dependency>
            requestedCache = new ConcurrentHashMap<>();

    private static org.openrewrite.maven.tree.Dependency dependency(Dependency dep, Configuration configuration) {
        GroupArtifactVersion gav = groupArtifactVersion(dep);
        return requestedCache.computeIfAbsent(gav, it -> {
                    Map<String, String> attributes = attributes(dep);

                    String type = "jar";
                    if (Optional.ofNullable(Category.from(attributes.get(Category.key())))
                            .filter(cat -> cat == Category.REGULAR_PLATFORM || cat == Category.ENFORCED_PLATFORM)
                            .isPresent()) {
                        type = "pom";
                    }

                    return org.openrewrite.maven.tree.Dependency.builder()
                            .gav(gav)
                            .type(type)
                            .scope(configuration.getName())
                            .exclusions(emptyList())
                            .attributes(attributes)
                            .build();
                }
        );
    }

    private static Map<String, String> attributes(Object maybeAttributed) {
        if (!(maybeAttributed instanceof HasAttributes)) {
            return emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        HasAttributes attributed = (HasAttributes) maybeAttributed;
        for (Attribute<?> attribute : attributed.getAttributes().keySet()) {
            Object attr = attributed.getAttributes().getAttribute(attribute);
            result.put(attribute.getName(), String.valueOf(attr));
        }
        if (maybeAttributed instanceof ProjectDependency) {
            result.put("org.gradle.api.artifacts.ProjectDependency", projectPath((ProjectDependency) maybeAttributed));
        }
        return result;
    }

    /**
     * Reflectively retrieve the project path for compatibility with a wide range of Gradle versions.
     */
    private static String projectPath(ProjectDependency pd) {
        try {
            // ProjectDependency.getPath introduced in gradle 8.11
            Method getPath = ProjectDependency.class.getMethod("getPath");
            return (String) getPath.invoke(pd);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // ProjectDependency.getDependencyProject() scheduled for removal in Gradle 9.0
            try {
                Method getDependencyProject = ProjectDependency.class.getMethod("getDependencyProject");
                return ((Project) getDependencyProject.invoke(pd)).getPath();
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                // All supported versions of Gradle have getPath(), getDependencyProject(), or both, so this hopefully never happens
                throw new IllegalStateException(e);
            }
        }
    }

    private static List<org.openrewrite.maven.tree.ResolvedDependency> resolved(
            Map<GroupArtifact, org.openrewrite.maven.tree.Dependency> gaToRequested,
            Map<GroupArtifact, ResolvedDependency> gaToResolved) {
        Map<org.openrewrite.maven.tree.ResolvedGroupArtifactVersion, org.openrewrite.maven.tree.ResolvedDependency>
                resolvedCache = new HashMap<>();
        return gaToResolved.entrySet().stream()
                .map(entry -> {
                    GroupArtifact ga = entry.getKey();
                    ResolvedDependency resolved = entry.getValue();

                    // Gradle knows which repository it got a dependency from, but haven't been able to find where that info lives
                    ResolvedGroupArtifactVersion resolvedGav = resolvedGroupArtifactVersion(resolved);
                    org.openrewrite.maven.tree.ResolvedDependency resolvedDependency = resolvedCache.get(resolvedGav);
                    if (resolvedDependency == null) {
                        org.openrewrite.maven.tree.Dependency requested = gaToRequested.getOrDefault(ga, dependency(resolved));
                        resolvedDependency = org.openrewrite.maven.tree.ResolvedDependency.builder()
                                .gav(resolvedGav)
                                // There may not be a requested entry if a dependency substitution rule took effect
                                // the DependencyHandler has the substitution mapping buried inside it, but not exposed publicly
                                .requested(requested)
                                .dependencies(resolved.getChildren().stream()
                                        .map(child -> resolved(child, 1, resolvedCache))
                                        .collect(toList()))
                                .licenses(emptyList())
                                .type(requested.getType())
                                .depth(0)
                                .build();
                        resolvedCache.put(resolvedGav, resolvedDependency);
                    }
                    return resolvedDependency;
                })
                .collect(toList());
    }

    /**
     * When there is a resolved dependency that cannot be matched up with a requested dependency, construct a requested
     * dependency corresponding to the exact version which was resolved. This isn't strictly accurate, but there is no
     * obvious way to access the resolution of transitive dependencies to figure out what versions are requested during
     * the resolution process.
     */
    private static org.openrewrite.maven.tree.Dependency dependency(ResolvedDependency dep) {
        GroupArtifactVersion gav = groupArtifactVersion(dep);
        return requestedCache.computeIfAbsent(gav, it -> {
            // Synthesize a Category attribute if this is a BOM
            String type = "jar";
            Map<String, String> attributes = emptyMap();
            // Both enforcedPlatform() and platform() appear the same in this context, so assume platform()
            if (dep.getConfiguration().startsWith("platform-")) {
                attributes = singletonMap(Category.key(), "platform");
                type = "pom";
            }

            return org.openrewrite.maven.tree.Dependency.builder()
                    .gav(gav)
                    // platform() dependencies are effectively BOMs, so their jar isn't actually used
                    .type(type)
                    .attributes(attributes)
                    .scope(dep.getConfiguration())
                    .exclusions(emptyList())
                    .build();
        });
    }

    private static org.openrewrite.maven.tree.ResolvedDependency resolved(
            ResolvedDependency dep, int depth,
            Map<org.openrewrite.maven.tree.ResolvedGroupArtifactVersion, org.openrewrite.maven.tree.ResolvedDependency> resolvedCache
    ) {
        ResolvedGroupArtifactVersion resolvedGav = resolvedGroupArtifactVersion(dep);
        org.openrewrite.maven.tree.ResolvedDependency resolvedDependency = resolvedCache.get(resolvedGav);
        if (resolvedDependency == null) {
            List<org.openrewrite.maven.tree.ResolvedDependency> dependencies = new ArrayList<>();
            org.openrewrite.maven.tree.Dependency requested = dependency(dep);
            resolvedDependency = org.openrewrite.maven.tree.ResolvedDependency.builder()
                    .gav(resolvedGav)
                    .requested(requested)
                    .dependencies(dependencies)
                    .licenses(emptyList())
                    .type(requested.getType())
                    .depth(depth)
                    .build();
            //we add a temporal resolved dependency in the cache to avoid stackoverflow with dependencies that have cycles
            resolvedCache.put(resolvedGav, resolvedDependency);
            dep.getChildren().forEach(child -> dependencies.add(resolved(child, depth + 1, resolvedCache)));
        }
        return resolvedDependency;
    }

    @SuppressWarnings("unused")
    public static void clearCaches() {
        requestedCache.clear();
        groupArtifactCache.clear();
        groupArtifactVersionCache.clear();
        resolvedGroupArtifactVersionCache.clear();
    }
}
