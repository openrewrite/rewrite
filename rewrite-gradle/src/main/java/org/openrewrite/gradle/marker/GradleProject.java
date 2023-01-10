package org.openrewrite.gradle.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;
import org.openrewrite.maven.tree.MavenRepository;

import java.util.*;


/**
 * Contains metadata about a Gradle Project. Queried from Gradle itself when the OpenRewrite build plugin runs.
 * Not automatically available on LSTs that aren't parsed through a Gradle plugin, so tests won't automatically have
 * access to this metadata.
 */
@Value
@With
public class GradleProject implements Marker {
    UUID id;
    String name;
    String path;
    List<GradlePluginDescriptor> plugins;
    List<MavenRepository> mavenRepositories;
    Map<String, GradleDependencyConfiguration> nameToConfiguration;

    public GradleDependencyConfiguration getConfiguration(String name) {
        return nameToConfiguration.get(name);
    }

    public List<GradleDependencyConfiguration> getConfigurations() {
        return new ArrayList<>(nameToConfiguration.values());
    }
}
