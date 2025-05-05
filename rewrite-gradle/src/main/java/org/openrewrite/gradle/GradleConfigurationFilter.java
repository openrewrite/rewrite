package org.openrewrite.gradle;

import lombok.Getter;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.maven.tree.GroupArtifact;

import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class GradleConfigurationFilter {
    private final GradleProject gradleProject;

    @Getter
    private final Set<String> filteredConfigurations;

    public GradleConfigurationFilter(GradleProject gradleProject, Set<String> configurations) {
        this.gradleProject = gradleProject;
        this.filteredConfigurations = new HashSet<>(configurations);
    }

    public void removeTransitiveConfigurations() {
        Set<String> tmpConfigurations = new HashSet<>(filteredConfigurations);
        for (String tmpConfiguration : tmpConfigurations) {
            GradleDependencyConfiguration gdc = requireNonNull((gradleProject.getConfiguration(tmpConfiguration)));
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

    public void removeTransitiveConfigurationsContainingDependency(GroupArtifact dependency) {
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
