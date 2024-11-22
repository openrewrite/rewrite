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
import org.openrewrite.marker.Marker;
import org.openrewrite.maven.tree.MavenRepository;

import java.io.Serializable;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.openrewrite.Tree.randomId;


/**
 * Contains metadata about a Gradle Project. Queried from Gradle itself when the OpenRewrite build plugin runs.
 * Not automatically available on LSTs that aren't parsed through a Gradle plugin, so tests won't automatically have
 * access to this metadata.
 */
@SuppressWarnings("unused")
@Value
@AllArgsConstructor(onConstructor_ = { @JsonCreator })
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

    @With
    @Deprecated
    @Nullable
    List<MavenRepository> mavenPluginRepositories;

    @Builder.Default
    Map<String, GradleDependencyConfiguration> nameToConfiguration = emptyMap();

    @Builder.Default
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
        List<GradleDependencyConfiguration> result = new ArrayList<>();
        for (GradleDependencyConfiguration configuration : nameToConfiguration.values()) {
            if (configuration.equals(parentConfiguration)) {
                continue;
            }
            for (GradleDependencyConfiguration extendsFrom : configuration.getExtendsFrom()) {
                if (extendsFrom.equals(parentConfiguration)) {
                    result.add(configuration);
                    if (transitive) {
                        result.addAll(configurationsExtendingFrom(configuration, true));
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
}
