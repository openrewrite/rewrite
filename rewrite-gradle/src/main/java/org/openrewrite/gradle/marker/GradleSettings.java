/*
 * Copyright 2023 the original author or authors.
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toSet;
import static org.openrewrite.Tree.randomId;

@Value
@With
@AllArgsConstructor(onConstructor_ = { @JsonCreator})
@Builder
public class GradleSettings implements Marker, Serializable {

    @Builder.Default
    UUID id = randomId();

    @Deprecated
    @Nullable
    @Builder.Default
    List<MavenRepository> pluginRepositories = emptyList();

    @Builder.Default
    List<GradlePluginDescriptor> plugins = emptyList();

    @Builder.Default
    Map<String, FeaturePreview> featurePreviews = emptyMap();

    @Builder.Default
    GradleBuildscript buildscript = GradleBuildscript.builder().build();

    // Backwards compatibility to ease convoluted release process with rewrite-gradle-tooling-model
    public GradleSettings(
            UUID id,
            List<MavenRepository> pluginRepositories,
            List<GradlePluginDescriptor> plugins,
            Map<String, FeaturePreview> featurePreviews
    ) {
        this(id, pluginRepositories, plugins, featurePreviews, null);
    }

    public GradleBuildscript getBuildscript() {
        // Temporary workaround for better compatibility with old LSTs that don't have a buildscript field yet.
        //noinspection ConstantValue
        if (buildscript == null) {
            return new GradleBuildscript(randomId(), emptyList(), emptyMap());
        }
        return buildscript;
    }

    public @Nullable Boolean isFeatureEnabled(String name) {
        return featurePreviews.get(name).getEnabled();
    }

    public Set<FeaturePreview> getActiveFeatures() {
        return featurePreviews.values().stream()
                .filter(FeaturePreview::isActive)
                .collect(toSet());
    }

    /**
     * Get a list of Maven plugin repositories.
     *
     * @return list of Maven plugin repositories
     * @deprecated Use {@link GradleBuildscript#getMavenRepositories()} instead.
     */
    @Deprecated
    public List<MavenRepository> getPluginRepositories() {
        if (buildscript != null) {
            return buildscript.getMavenRepositories();
        }
        return pluginRepositories == null ? emptyList() : pluginRepositories;
    }
}
