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

import lombok.Value;
import lombok.With;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;
import org.openrewrite.maven.tree.MavenRepository;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Value
@With
public class GradleSettings implements Marker, Serializable {
    UUID id;
    List<MavenRepository> pluginRepositories;
    List<GradlePluginDescriptor> plugins;
    Map<String, FeaturePreview> featurePreviews;

    @Nullable
    public Boolean isFeatureEnabled(String name) {
        // Unclear how enabled status can be determined in latest gradle APIs
        return null;
    }

    public Set<FeaturePreview> getActiveFeatures() {
        return featurePreviews.values().stream()
                .filter(FeaturePreview::isActive)
                .collect(Collectors.toSet());
    }
}
