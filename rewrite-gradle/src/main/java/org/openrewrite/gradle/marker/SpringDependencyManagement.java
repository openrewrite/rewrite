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
import lombok.*;
import lombok.experimental.NonFinal;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.ExtensionAware;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.gradle.attributes.Category;
import org.openrewrite.gradle.attributes.ProjectAttribute;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenDownloadingExceptions;
import org.openrewrite.maven.attributes.Attributed;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.Semver;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@SuppressWarnings("unused")
@Value
@With
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
@Builder
public class SpringDependencyManagement implements Serializable {

    /**
     * Whether dependency management should be overridden by versions declared on a
     * project's dependencies. The default is {@code true}.
     */
    @Builder.Default
    boolean overriddenByDependencies = true;

    @Nullable
    @Builder.Default
    DependencyManagement globalDependencyManagement = null;

    @Builder.Default
    @With
    Map<String, DependencyManagement> configurationDependencyManagement = new HashMap<>();

    @Nullable
    public String getManagedVersion(@Nullable String configuration, String group, String name) {
        String key = createKey(group, name);
        if (globalDependencyManagement != null) {
            return globalDependencyManagement.getManagedVersion(key, overriddenByDependencies);
        }
        if (!configurationDependencyManagement.containsKey(configuration)) {
            return null;
        }
        return configurationDependencyManagement.get(configuration).getManagedVersion(key, overriddenByDependencies);
    }

    private String createKey(String group, String name) {
        return group + ":" + name;
    }

    @Value
    @With
    @AllArgsConstructor(onConstructor_ = {@JsonCreator})
    @Builder
    static class DependencyManagement implements Serializable {
        /**
         * A map of the managed versions from imported boms. The key-value pairs in the map have the form {@code group:name = version}.
         */
        Map<String, String> implicitVersions;

        /**
         * A map of the managed versions from dependencies in the {@code dependencies} block. The key-value pairs in the map have the form {@code group:name = version}.
         */
        Map<String, String> explicitVersions;

        List<GroupArtifactVersion> importedBoms;

        String getManagedVersion(String key, boolean overriddenByDependencies) {
            if (!overriddenByDependencies) {
                return implicitVersions.containsKey(key) ? implicitVersions.get(key) : explicitVersions.get(key);
            }
            return explicitVersions.containsKey(key) ? explicitVersions.get(key) : implicitVersions.get(key);
        }
    }

    /**
     * If the Spring {@code io.spring.dependency-management} plugin is applied, reflectively access its {@code DependencyManagementExtension} to get the dependency management.
     */
    @SuppressWarnings("unchecked")
    static @Nullable SpringDependencyManagement springDependencyManagement(Project project) {
        try {
            Object extension = project.getExtensions().findByName("dependencyManagement");
            if (extension == null) {
                return null;
            }

            Field dependencyManagementSettingsField = extension.getClass().getDeclaredField("dependencyManagementSettings");
            dependencyManagementSettingsField.setAccessible(true);
            Object dependencyManagementSettings = dependencyManagementSettingsField.get(extension);
            Method isOverriddenByDependencies = dependencyManagementSettings.getClass().getDeclaredMethod("isOverriddenByDependencies");
            isOverriddenByDependencies.setAccessible(true);
            boolean overriddenByDependencies = (boolean) isOverriddenByDependencies.invoke(dependencyManagementSettings);

            Field dependencyManagementContainerField = extension.getClass().getDeclaredField("dependencyManagementContainer");
            dependencyManagementContainerField.setAccessible(true);

            Object dependencyManagementContainer = dependencyManagementContainerField.get(extension);
            if (dependencyManagementContainer == null) {
                return null;
            }

            Field globalDependencyManagementField = dependencyManagementContainer.getClass().getDeclaredField("globalDependencyManagement");
            globalDependencyManagementField.setAccessible(true);
            Field configurationDependencyManagementField = dependencyManagementContainer.getClass().getDeclaredField("configurationDependencyManagement");
            configurationDependencyManagementField.setAccessible(true);

            Object globalDependencyManagement = globalDependencyManagementField.get(dependencyManagementContainer);
            Object configurationDependencyManagement = configurationDependencyManagementField.get(dependencyManagementContainer);

            if (globalDependencyManagement == null && configurationDependencyManagement == null) {
                return null;
            }

            Field versionsField = null;
            Field explicitVersionsField = null;
            Field importedBomsField = null;
            if (globalDependencyManagement != null) {
                versionsField = globalDependencyManagement.getClass().getDeclaredField("versions");
                versionsField.setAccessible(true);
                explicitVersionsField = globalDependencyManagement.getClass().getDeclaredField("explicitVersions");
                explicitVersionsField.setAccessible(true);
                importedBomsField = globalDependencyManagement.getClass().getDeclaredField("importedBoms");
                importedBomsField.setAccessible(true);
                return new SpringDependencyManagement(overriddenByDependencies, getDependencyManagement(globalDependencyManagement, versionsField, explicitVersionsField, importedBomsField), new HashMap<>());
            } else if (configurationDependencyManagement instanceof Map) {
                Map<String, DependencyManagement> configurationDependencyManagementMap = new HashMap<>();
                for (Map.Entry<String, Object> entry : ((Map<String, Object>) configurationDependencyManagement).entrySet()) {
                    if (versionsField == null) {
                        versionsField = entry.getValue().getClass().getDeclaredField("versions");
                        versionsField.setAccessible(true);
                        explicitVersionsField = entry.getValue().getClass().getDeclaredField("explicitVersions");
                        explicitVersionsField.setAccessible(true);
                        importedBomsField = entry.getValue().getClass().getDeclaredField("importedBoms");
                        importedBomsField.setAccessible(true);
                    }
                    configurationDependencyManagementMap.put(entry.getKey(), getDependencyManagement(entry.getValue(), versionsField, explicitVersionsField, importedBomsField));
                }

                return new SpringDependencyManagement(overriddenByDependencies, null, configurationDependencyManagementMap);
            }
            return null;
        } catch (Exception e) {
            // Plugin not on classpath or API changed -- silently fall back
            return null;
        }
    }

    private static DependencyManagement getDependencyManagement(Object dependencyManagement, Field versionsField, Field explicitVersionsField, Field importedBomsField) throws IllegalAccessException, NoSuchFieldException {
        Map<String, String> implicitVersions = (Map<String, String>) versionsField.get(dependencyManagement);
        Map<String, String> explicitVersions = (Map<String, String>) explicitVersionsField.get(dependencyManagement);
        explicitVersions.forEach((key, __) -> implicitVersions.remove(key));

        List<GroupArtifactVersion> importedBoms = new ArrayList<>();
        Object maybeImportedBoms = importedBomsField.get(dependencyManagement);
        if (maybeImportedBoms instanceof List) {
            for (Object importedBom : ((List<?>) maybeImportedBoms)) {
                Field coordinatesField = importedBom.getClass().getDeclaredField("coordinates");
                coordinatesField.setAccessible(true);
                Object coordinates = coordinatesField.get(importedBom);

                Field groupField = coordinates.getClass().getDeclaredField("groupId");
                groupField.setAccessible(true);
                Field artifactIdField = coordinates.getClass().getDeclaredField("artifactId");
                artifactIdField.setAccessible(true);
                Field versionField = coordinates.getClass().getDeclaredField("version");
                versionField.setAccessible(true);

                String group = (String) groupField.get(coordinates);
                String artifactId = (String) artifactIdField.get(coordinates);
                String version = (String) versionField.get(coordinates);
                importedBoms.add(new GroupArtifactVersion(group, artifactId, version));
            }
        }

        return new DependencyManagement(implicitVersions, explicitVersions, importedBoms);
    }
}
