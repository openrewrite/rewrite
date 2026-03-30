/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.gradle;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.maven.tree.GroupArtifact;

import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
class GradleConfigurationFilter {
    private final GradleProject gradleProject;

    @Getter
    private final Set<String> filteredConfigurations;

    public void removeTransitiveConfigurations() {
        Set<String> tmpConfigurations = new HashSet<>(filteredConfigurations);
        for (String tmpConfiguration : tmpConfigurations) {
            GradleDependencyConfiguration gdc = requireNonNull(gradleProject.getConfiguration(tmpConfiguration));
            for (GradleDependencyConfiguration transitive : gradleProject.configurationsExtendingFrom(gdc, true)) {
                filteredConfigurations.remove(transitive.getName());
            }
        }
    }

    public void removeConfigurationsContainingDependency(GroupArtifact dependency) {
        Set<String> tmpConfigurations = new HashSet<>(filteredConfigurations);
        for (String tmpConfiguration : tmpConfigurations) {
            GradleDependencyConfiguration gdc = gradleProject.getConfiguration(tmpConfiguration);
            if (gdc == null || gdc.findRequestedDependency(dependency.getGroupId(), dependency.getArtifactId()) != null) {
                filteredConfigurations.remove(tmpConfiguration);
            }
        }
    }

    public void removeConfigurationsContainingTransitiveDependency(GroupArtifact dependency) {
        Set<String> tmpConfigurations = new HashSet<>(filteredConfigurations);
        for (String tmpConfiguration : tmpConfigurations) {
            GradleDependencyConfiguration gdc = requireNonNull(gradleProject.getConfiguration(tmpConfiguration));
            for (GradleDependencyConfiguration transitive : gradleProject.configurationsExtendingFrom(gdc, true)) {
                if (transitive.findResolvedDependency(dependency.getGroupId(), dependency.getArtifactId()) != null) {
                    filteredConfigurations.remove(tmpConfiguration);
                }
            }
        }
    }
}
