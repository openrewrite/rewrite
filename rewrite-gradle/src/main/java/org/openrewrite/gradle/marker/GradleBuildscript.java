/*
 * Copyright 2024 the original author or authors.
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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.tree.MavenRepository;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.openrewrite.Tree.randomId;

@Value
@With
@Builder
@AllArgsConstructor
public class GradleBuildscript implements Serializable {

    @Builder.Default
    UUID id = randomId();

    @Builder.Default
    List<MavenRepository> mavenRepositories = emptyList();

    @Builder.Default
    Map<String, GradleDependencyConfiguration> nameToConfiguration = emptyMap();

    public @Nullable GradleDependencyConfiguration getConfiguration(String name) {
        return nameToConfiguration.get(name);
    }

    public List<GradleDependencyConfiguration> getConfigurations() {
        return new ArrayList<>(nameToConfiguration.values());
    }

    /**
     * Typically buildscripts have only the "classpath" configuration, but it is technically possible to declare more.
     */
    public GradleBuildscript mapConfigurations(
            Function<GradleDependencyConfiguration, @Nullable GradleDependencyConfiguration> mapping,
            ExecutionContext ctx) {
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

        Map<String, GradleDependencyConfiguration> updatedNameToConfiguration = GradleDependencyConfiguration.updateExtendsFrom(updatedConfigurations, untouchedConfigurations);
        GradleBuildscript result = new GradleBuildscript(
                id,
                mavenRepositories,
                updatedNameToConfiguration
        );

        updatedConfigurations.values().stream()
                .flatMap(it -> GradleProject.configurationsExtendingFrom(it, updatedNameToConfiguration, true).stream()
                        .map(GradleDependencyConfiguration::getName))
                .map(untouchedConfigurations::get)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(needsUpdate -> needsUpdate.markForReResolution(getMavenRepositories(), ctx));

        return result;
    }
}
