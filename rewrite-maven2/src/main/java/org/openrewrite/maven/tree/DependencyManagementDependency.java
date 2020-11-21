package org.openrewrite.maven.tree;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;

/**
 * Dependency management sections contain a combination of single dependency definitions and imports of
 * BOMs and their dependency management sections/properties.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@c")
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface DependencyManagementDependency {
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    class Defined implements DependencyManagementDependency,
            DependencyDescriptor {
        String groupId;
        String artifactId;
        String version;

        @Nullable
        Scope scope;

        @Nullable
        String classifier;

        Set<GroupArtifact> exclusions;

        @JsonIgnore
        @Override
        public List<DependencyDescriptor> getDependencies() {
            return Collections.singletonList(this);
        }

        @Override
        public Map<String, String> getProperties() {
            return emptyMap();
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    class Imported implements DependencyManagementDependency {
        Pom maven;

        @JsonIgnore
        @Override
        public List<DependencyDescriptor> getDependencies() {
            return maven.getEffectiveDependencyManagement().getDependencies().stream()
                    .flatMap(dep -> dep.getDependencies().stream())
                    .collect(Collectors.toList());
        }

        @JsonIgnore
        @Override
        public Map<String, String> getProperties() {
            // FIXME should be active properties by profile as well? also parent properties?
            return maven.getProperties();
        }
    }

    /**
     * @return A list of managed dependencies in order of precedence.
     */
    List<DependencyDescriptor> getDependencies();

    /**
     * @return A map of properties inherited from import-scope BOMs defined as
     * dependencyManagement dependencies.
     */
    Map<String, String> getProperties();
}
