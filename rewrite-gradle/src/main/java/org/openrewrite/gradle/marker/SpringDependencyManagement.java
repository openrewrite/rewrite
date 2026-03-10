/*
 * Copyright 2025 the original author or authors.
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

import static java.util.Collections.emptyMap;

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
    Map<String, DependencyManagement> configurationDependencyManagement = emptyMap();

    @JsonCreator
    public SpringDependencyManagement(boolean overriddenByDependencies, @Nullable DependencyManagement globalDependencyManagement, Map<String, DependencyManagement> configurationDependencyManagement) {
        this.overriddenByDependencies = overriddenByDependencies;
        this.globalDependencyManagement = globalDependencyManagement;
        this.configurationDependencyManagement = configurationDependencyManagement;
    }

    @Nullable
    public String getManagedVersion(@Nullable String configuration, String group, String name) {
        String key = createKey(group, name);
        // Configuration-specific management takes precedence over global, with global as fallback
        if (configuration != null && configurationDependencyManagement.containsKey(configuration)) {
            String version = configurationDependencyManagement.get(configuration).getManagedVersion(key, overriddenByDependencies);
            if (version != null) {
                return version;
            }
        }
        if (globalDependencyManagement != null) {
            return globalDependencyManagement.getManagedVersion(key, overriddenByDependencies);
        }
        return null;
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

            DependencyManagement globalDm = null;
            if (globalDependencyManagement != null) {
                versionsField = globalDependencyManagement.getClass().getDeclaredField("versions");
                versionsField.setAccessible(true);
                explicitVersionsField = globalDependencyManagement.getClass().getDeclaredField("explicitVersions");
                explicitVersionsField.setAccessible(true);
                importedBomsField = globalDependencyManagement.getClass().getDeclaredField("importedBoms");
                importedBomsField.setAccessible(true);
                globalDm = getDependencyManagement(globalDependencyManagement, versionsField, explicitVersionsField, importedBomsField);
            }

            Map<String, DependencyManagement> configDmMap = emptyMap();
            if (configurationDependencyManagement instanceof Map && !((Map<?, ?>) configurationDependencyManagement).isEmpty()) {
                configDmMap = new HashMap<>();
                // The Spring plugin's configurationDependencyManagement map uses Configuration objects as keys,
                // not Strings. We need to reflectively call getName() to get the configuration name.
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) configurationDependencyManagement).entrySet()) {
                    Object configKey = entry.getKey();
                    Method getNameMethod = configKey.getClass().getMethod("getName");
                    String configName = (String) getNameMethod.invoke(configKey);
                    Object value = entry.getValue();
                    if (versionsField == null) {
                        versionsField = value.getClass().getDeclaredField("versions");
                        versionsField.setAccessible(true);
                        explicitVersionsField = value.getClass().getDeclaredField("explicitVersions");
                        explicitVersionsField.setAccessible(true);
                        importedBomsField = value.getClass().getDeclaredField("importedBoms");
                        importedBomsField.setAccessible(true);
                    }
                    configDmMap.put(configName, getDependencyManagement(value, versionsField, explicitVersionsField, importedBomsField));
                }
            }

            if (globalDm == null && configDmMap.isEmpty()) {
                return null;
            }
            return new SpringDependencyManagement(overriddenByDependencies, globalDm, configDmMap);
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
