package org.openrewrite.maven.internal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.Maven;

import java.net.URI;
import java.util.*;

import static java.util.Collections.singletonList;

/**
 * A value object deserialized directly from POM XML
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public class RawPom {
    @Nullable
    Parent parent;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Nullable
    String groupId;

    @EqualsAndHashCode.Include
    @ToString.Include
    String artifactId;

    @EqualsAndHashCode.Include
    @ToString.Include
    @Nullable
    String version;

    @Nullable
    String packaging;

    @Nullable
    List<Dependency> dependencies;

    @Nullable
    DependencyManagement dependencyManagement;

    @Nullable
    Map<String, String> properties;

    @Nullable
    List<Repository> repositories;

    @Nullable
    List<License> licenses;

    @Nullable
    List<Profile> profiles;

    @JsonIgnore
    public Map<String, String> getActiveProperties(URI containingPomUri) {
        Map<String, String> activeProperties = new HashMap<>();

        if (properties != null) {
            activeProperties.putAll(properties);
        }

        if (profiles != null) {
            for (RawPom.Profile profile : profiles) {
                if (profile.isActive(containingPomUri) && profile.getProperties() != null) {
                    activeProperties.putAll(profile.getProperties());
                }
            }
        }

        return activeProperties;
    }

    @JsonIgnore
    public List<Dependency> getActiveDependencies(URI containingPomUri) {
        List<Dependency> activeDependencies = new ArrayList<>();

        if (dependencies != null) {
            activeDependencies.addAll(dependencies);
        }

        if (profiles != null) {
            profiles.stream().filter(p -> p.isActive(containingPomUri)).forEach(profile -> {
                if (profile.dependencies != null) {
                    activeDependencies.addAll(profile.dependencies);
                }
            });
        }

        return activeDependencies;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Dependency {
        String groupId;
        String artifactId;

        @Nullable
        String version;

        @Nullable
        String scope;

        @Nullable
        String type;

        @Nullable
        String classifier;

        @Nullable
        Boolean optional;

        @Nullable
        Set<GroupArtifact> exclusions;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class DependencyManagement {
        List<Dependency> dependencies;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Repository {
        @With
        String url;

        @Nullable
        ArtifactPolicy releases;

        @Nullable
        ArtifactPolicy snapshots;

        public boolean acceptsVersion(String version) {
            if (version.endsWith("-SNAPSHOT")) {
                return snapshots != null && snapshots.isEnabled();
            } else if (url.equals("https://repo.spring.io/milestone")) {
                // special case this repository since it will be so commonly used
                return version.matches(".*(M|RC)\\d+$");
            }
            return releases != null && releases.isEnabled();
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Getter
    public static class ArtifactPolicy {
        boolean enabled;

        public ArtifactPolicy(@Nullable Boolean enabled) {
            this.enabled = enabled == null || enabled;
        }

        public ArtifactPolicy() {
            this(true);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Parent {
        String groupId;
        String artifactId;
        String version;

        @Nullable
        String relativePath;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class License {
        String name;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Profile {
        @Nullable
        ProfileActivation activation;

        @Nullable
        Map<String, String> properties;

        @Nullable
        List<Dependency> dependencies;

        @JsonIgnore
        public boolean isActive(URI containingPomUri) {
            return activation != null && activation.isActive(containingPomUri);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class ProfileActivation {
        @Nullable
        String jdk;

        @Nullable
        Map<String, String> property;

        @JsonIgnore
        public boolean isActive(@Nullable URI containingPomUri) {
            return isActiveByJdk(containingPomUri) || isActiveByProperty();
        }

        @JsonIgnore
        private boolean isActiveByJdk(@Nullable URI containingPomUri) {
            if (jdk == null) {
                return false;
            }
            String version = System.getProperty("java.version");
            RequestedVersion requestedVersion = new RequestedVersion(containingPomUri, new GroupArtifact("", ""),
                    null, jdk);

            if (requestedVersion.isDynamic() || requestedVersion.isRange()) {
                return requestedVersion.selectFrom(singletonList(version)) != null;
            }

            //noinspection ConstantConditions
            return version.startsWith(requestedVersion.nearestVersion());
        }

        @JsonIgnore
        private boolean isActiveByProperty() {
            return property != null && !property.isEmpty() &&
                    property.entrySet().stream().allMatch(prop -> prop.getValue().equals(System.getenv(prop.getKey())));
        }
    }

    @Nullable
    public String getGroupId() {
        return groupId == null && parent != null ? parent.getGroupId() : groupId;
    }

    @Nullable
    public String getVersion() {
        return version == null && parent != null ? parent.getVersion() : version;
    }
}
