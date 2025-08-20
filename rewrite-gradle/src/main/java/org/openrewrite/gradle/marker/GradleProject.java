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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markup;
import org.openrewrite.maven.tree.*;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toSet;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.gradle.marker.GradleDependencyConfiguration.updateExtendsFrom;

/**
 * Contains metadata about a Gradle Project. Queried from Gradle itself when the OpenRewrite build plugin runs.
 * Not automatically available on LSTs that aren't parsed through a Gradle plugin, so tests won't automatically have
 * access to this metadata.
 */
@SuppressWarnings("unused")
@Value
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
@Builder
public class GradleProject implements Marker, Serializable {

    @With
    @Builder.Default
    UUID id = randomId();

    @With
    @Nullable
    String group;

    @With
    @Builder.Default
    String name = "";

    @With
    @Nullable
    String version;

    @With
    @Builder.Default
    String path = "";

    @With
    @Builder.Default
    List<GradlePluginDescriptor> plugins = emptyList();

    @With
    @Builder.Default
    List<MavenRepository> mavenRepositories = emptyList();

    @SuppressWarnings("DeprecatedIsStillUsed")
    @With
    @Deprecated
    @Nullable
    List<MavenRepository> mavenPluginRepositories;

    @Builder.Default
    Map<String, GradleDependencyConfiguration> nameToConfiguration = emptyMap();

    @Builder.Default
    @With
    GradleBuildscript buildscript = new GradleBuildscript(randomId(), emptyList(), emptyMap());

    public GradleBuildscript getBuildscript() {
        // Temporary workaround for better compatibility with old LSTs that don't have a buildscript field yet.
        //noinspection ConstantValue
        if (buildscript == null) {
            return new GradleBuildscript(randomId(), emptyList(), emptyMap());
        }
        return buildscript;
    }

    /**
     * Get a list of Maven plugin repositories.
     *
     * @return list of Maven plugin repositories
     * @deprecated Use {@link GradleBuildscript#getMavenRepositories()} instead.
     */
    @Deprecated
    public List<MavenRepository> getMavenPluginRepositories() {
        //noinspection ConstantValue
        if (buildscript != null) {
            return buildscript.getMavenRepositories();
        }
        return mavenPluginRepositories == null ? emptyList() : mavenPluginRepositories;
    }

    public @Nullable GradleDependencyConfiguration getConfiguration(String name) {
        return nameToConfiguration.get(name);
    }

    public List<GradleDependencyConfiguration> getConfigurations() {
        return new ArrayList<>(nameToConfiguration.values());
    }

    /**
     * List the configurations which extend from the given configuration.
     * Assuming a hierarchy like:
     * <pre>
     *     implementation
     *     |> compileClasspath
     *     |> runtimeClasspath
     *     |> testImplementation
     *        |> testCompileClasspath
     *        |> testRuntimeClasspath
     * </pre>
     * <p>
     * When querying "implementation" with transitive is false this function will return [compileClasspath, runtimeClasspath, testImplementation].
     * When transitive is true this function will also return [testCompileClasspath, testRuntimeClasspath].
     */
    public List<GradleDependencyConfiguration> configurationsExtendingFrom(
            GradleDependencyConfiguration parentConfiguration,
            boolean transitive
    ) {
        return configurationsExtendingFrom(parentConfiguration, nameToConfiguration, transitive);
    }

    /**
     * List the configurations which extend from the given configuration.
     * Assuming a hierarchy like:
     * <pre>
     *     implementation
     *     |> compileClasspath
     *     |> runtimeClasspath
     *     |> testImplementation
     *        |> testCompileClasspath
     *        |> testRuntimeClasspath
     * </pre>
     * <p>
     * When querying "implementation" with transitive is false this function will return [compileClasspath, runtimeClasspath, testImplementation].
     * When transitive is true this function will also return [testCompileClasspath, testRuntimeClasspath].
     */
    public static List<GradleDependencyConfiguration> configurationsExtendingFrom(GradleDependencyConfiguration parentConfiguration, Map<String, GradleDependencyConfiguration> nameToConfiguration, boolean transitive) {
        List<GradleDependencyConfiguration> result = new ArrayList<>();
        for (GradleDependencyConfiguration configuration : nameToConfiguration.values()) {
            if (configuration == parentConfiguration) {
                continue;
            }
            for (GradleDependencyConfiguration extendsFrom : configuration.getExtendsFrom()) {
                if (extendsFrom.getName().equals(parentConfiguration.getName())) {
                    result.add(configuration);
                    if (transitive) {
                        result.addAll(configurationsExtendingFrom(configuration, nameToConfiguration,true));
                    }
                }
            }
        }
        return result;
    }

    public GradleProject withNameToConfiguration(Map<String, GradleDependencyConfiguration> nameToConfiguration) {
        Map<String, GradleDependencyConfiguration> configurations = new HashMap<>(nameToConfiguration);
        for (GradleDependencyConfiguration gdc : configurations.values()) {
            List<GradleDependencyConfiguration> extendsFromList = new ArrayList<>(gdc.getExtendsFrom());
            boolean changed = false;
            for (int i = 0; i < extendsFromList.size(); i++) {
                GradleDependencyConfiguration extendsFrom = extendsFromList.get(i);
                if (configurations.get(extendsFrom.getName()) != extendsFrom) {
                    extendsFromList.set(i, configurations.get(extendsFrom.getName()));
                    changed = true;
                }
            }
            if (changed) {
                configurations.put(gdc.getName(), gdc.withExtendsFrom(extendsFromList));
            }
        }

        return new GradleProject(
                id,
                group,
                name,
                version,
                path,
                plugins,
                mavenRepositories,
                mavenPluginRepositories,
                configurations,
                buildscript
        );
    }


    public GradleProject removeDirectDependencies(Collection<GroupArtifact> gas, ExecutionContext ctx) {
        return mapConfigurations(
                conf -> conf.removeDirectDependencies(gas, getMavenRepositories(), ctx),
                ctx
        );
    }

    /**
     * Upgrade the specified dependency within all configurations.
     */
    public GradleProject upgradeDirectDependencyVersions(Collection<GroupArtifactVersion> gavs, ExecutionContext ctx) {
        return mapConfigurations(
                conf -> conf.upgradeDirectDependencies(gavs, getMavenRepositories(), ctx),
                ctx
        );
    }

    /**
     * Upgrade the specified dependency within the specified configuration and all configurations which extend from that configuration.
     */
    public GradleProject upgradeDirectDependencyVersion(String configuration, GroupArtifactVersion gav, ExecutionContext ctx) {
        return mapConfiguration(
                configuration,
                conf -> conf.upgradeDirectDependency(gav, getMavenRepositories(), ctx),
                ctx);
    }

    public GradleProject addOrUpdateConstraints(Map<String, ? extends Collection<GroupArtifactVersion>> configurationNameToConstraints, ExecutionContext ctx) {
        return mapConfigurations(
                it -> {
                    Collection<GroupArtifactVersion> gavs = configurationNameToConstraints.get(it.getName());
                    if (gavs != null && !gavs.isEmpty()) {
                        for (GroupArtifactVersion gav : gavs) {
                            it = it.addOrUpdateConstraint(gav, mavenRepositories, ctx);
                        }
                    }
                    return it;
                },
                ctx
        );
    }

    /**
     * Applies the specified mapping function to the named configuration and all configurations which extend from it.
     * @param configuration name of the configuration to apply the mapping function to
     * @param mapping mapping function which is expected to either return a new configuration with modifications or the original configuration unchanged
     * @return a GradleProject marker with updated configurations, or the original GradleProject marker if no updates were made.
     */
    public GradleProject mapConfiguration(
            String configuration,
            Function<GradleDependencyConfiguration, @Nullable GradleDependencyConfiguration> mapping,
            ExecutionContext ctx
    ) {
        Set<String> extendingFrom = configurationsExtendingFrom(nameToConfiguration.get(configuration), true).stream()
                .map(GradleDependencyConfiguration::getName)
                .collect(toSet());
        return mapConfigurations(it -> {
            if (configuration.equals(it.getName()) || extendingFrom.contains(it.getName())) {
                return mapping.apply(it);
            }
            return it;
        }, ctx);
    }

    public GradleProject mapConfigurations(
            Function<GradleDependencyConfiguration, @Nullable GradleDependencyConfiguration> mapping,
            ExecutionContext ctx
    ) {
        Map<String, GradleDependencyConfiguration> updatedConfigurations = new HashMap<>(nameToConfiguration.size());
        Map<String, GradleDependencyConfiguration> untouchedConfigurations = new HashMap<>(nameToConfiguration.size());
        for (GradleDependencyConfiguration configuration : getConfigurations()) {
            GradleDependencyConfiguration mapped = mapping.apply(configuration);
            if (mapped == configuration) {
                // Defensively copy the original configurations so that there's no mutation of the original objects' extendsFrom
                untouchedConfigurations.put(configuration.getName(), configuration.clone());
            } else if (mapped != null) {
                updatedConfigurations.put(mapped.getName(), mapped);
            }
        }
        if (updatedConfigurations.isEmpty()) {
            return this;
        }

        GradleProject result = new GradleProject(
                id,
                group,
                name,
                version,
                path,
                plugins,
                mavenRepositories,
                mavenPluginRepositories,
                updateExtendsFrom(updatedConfigurations, untouchedConfigurations),
                buildscript
        );

        // All configurations extending from a mutated configuration must be marked as requiring re-resolution to propagate heritable changes
        updatedConfigurations.values().stream()
                .flatMap(it -> result.configurationsExtendingFrom(it, true).stream()
                        .map(GradleDependencyConfiguration::getName))
                .map(untouchedConfigurations::get)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(needsUpdate -> needsUpdate.markForReResolution(getMavenRepositories(), ctx));

        return result;
    }

    /**
     * Upgrade the specified dependency within all configurations.
     */
    public GradleProject upgradeBuildscriptDirectDependencyVersions(Collection<GroupArtifactVersion> gavs, ExecutionContext ctx) {
        return mapBuildscriptConfigurations(
                conf -> conf.upgradeDirectDependencies(gavs, buildscript.getMavenRepositories(), ctx),
                ctx
        );
    }

    /**
     * Apply a transformation to the configurations of the buildscript.
     * Typically, buildscripts have only the "classpath" configuration, but it is technically possible to declare more.
     */
    public GradleProject mapBuildscriptConfigurations(
            Function<GradleDependencyConfiguration, @Nullable GradleDependencyConfiguration> mapping,
            ExecutionContext ctx) {
        return withBuildscript(buildscript.mapConfigurations(mapping, ctx));
    }

    /**
     * If any configurations within this GradleProject marker have reported dependency resolution failure attach
     * the type and message to the specified tree as a Warning marker.
     * This can be used to ensure that model-updating errors are not hidden.
     * This does not always mean that OpenRewrite attempted to resolve a dependency and failed, sometimes this comes
     * directly from Gradle at parse time.
     *
     * @param tree an LST element to be used to report a dependency resolution failure.
     * @return Either the tree with a warning marker or the original tree if there are no errors to report
     */
    public <T extends Tree> T maybeWarn(T tree) {
        for (GradleDependencyConfiguration conf : nameToConfiguration.values()) {
            if (conf.getExceptionType() != null) {
                return Markup.warn(tree, new IllegalStateException(
                        conf.getName() + " reported a non-fatal dependency resolution problem which may cause inaccuracies: " +
                        conf.getExceptionType() + " " + conf.getMessage()));
            }
        }
        return tree;
    }
}
