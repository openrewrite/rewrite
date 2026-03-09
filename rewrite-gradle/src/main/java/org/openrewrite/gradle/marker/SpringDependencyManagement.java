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
import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.gradle.api.Project;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.tree.GroupArtifactVersion;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Value
@With
@Builder
public final class SpringDependencyManagement implements Serializable {

    /**
     * Whether dependency management should be overridden by versions declared on a
     * project's dependencies. The default is {@code true}.
     */
    @Builder.Default
    private final
    boolean overriddenByDependencies = true;

    @Nullable
    @Builder.Default
    private final
    DependencyManagement globalDependencyManagement = null;

    @Builder.Default
    @With
    private final
    Map<String, DependencyManagement> configurationDependencyManagement = new HashMap<>();

    @JsonCreator
    public SpringDependencyManagement(boolean overriddenByDependencies, @Nullable DependencyManagement globalDependencyManagement, Map<String, DependencyManagement> configurationDependencyManagement) {
        this.overriddenByDependencies = overriddenByDependencies;
        this.globalDependencyManagement = globalDependencyManagement;
        this.configurationDependencyManagement = configurationDependencyManagement;
    }

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
    @Builder
    static final class DependencyManagement implements Serializable {
        /**
         * A map of the managed versions from imported boms. The key-value pairs in the map have the form {@code group:name = version}.
         */
        private final Map<String, String> implicitVersions;

        /**
         * A map of the managed versions from dependencies in the {@code dependencies} block. The key-value pairs in the map have the form {@code group:name = version}.
         */
        private final Map<String, String> explicitVersions;

        private final List<GroupArtifactVersion> importedBoms;

        @JsonCreator
        public DependencyManagement(Map<String, String> implicitVersions, Map<String, String> explicitVersions, List<GroupArtifactVersion> importedBoms) {
            this.implicitVersions = implicitVersions;
            this.explicitVersions = explicitVersions;
            this.importedBoms = importedBoms;
        }

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
