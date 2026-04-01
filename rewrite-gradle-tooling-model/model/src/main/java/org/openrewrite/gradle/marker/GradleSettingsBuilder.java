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

import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.FeaturePreviews;
import org.gradle.initialization.DefaultSettings;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.internal.service.UnknownServiceException;
import org.gradle.util.GradleVersion;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.tree.MavenRepository;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static java.util.Collections.emptyMap;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.gradle.marker.GradleProjectBuilder.mapRepositories;

public final class GradleSettingsBuilder {
    static MavenRepository GRADLE_PLUGIN_PORTAL = MavenRepository.builder()
            .id("Gradle Central Plugin Repository")
            .uri("https://plugins.gradle.org/m2")
            .releases(true)
            .snapshots(true)
            .build();

    private GradleSettingsBuilder() {
    }

    public static GradleSettings gradleSettings(Settings settings) {
        Set<MavenRepository> pluginRepositories = new HashSet<>();
        pluginRepositories.addAll(mapRepositories(settings.getPluginManagement().getRepositories()));
        pluginRepositories.addAll(mapRepositories(settings.getBuildscript().getRepositories()));
        if (pluginRepositories.isEmpty()) {
            pluginRepositories.add(GRADLE_PLUGIN_PORTAL);
        }

        return new GradleSettings(
                randomId(),
                null,
                GradleProjectBuilder.pluginDescriptors(settings.getPluginManager()),
                featurePreviews((DefaultSettings) settings),
                new GradleBuildscript(
                        randomId(),
                        new ArrayList<>(pluginRepositories),
                        GradleProjectBuilder.dependencyConfigurations(settings.getBuildscript().getConfigurations())
                )
        );
    }

    private static Map<String, FeaturePreview> featurePreviews(DefaultSettings settings) {
        if (GradleVersion.current().compareTo(GradleVersion.version("4.6")) < 0) {
            return emptyMap();
        }

        Map<String, FeaturePreview> featurePreviews = new HashMap<>();
        if (GradleVersion.current().compareTo(GradleVersion.version("8.0")) < 0) {
            FeaturePreviews gradleFeaturePreviews = getService(settings, FeaturePreviews.class);
            if (gradleFeaturePreviews != null) {
                try {
                    Method method = gradleFeaturePreviews.getClass().getDeclaredMethod("isFeatureEnabled", FeaturePreviews.Feature.class);
                    FeaturePreviews.Feature[] gradleFeatures = FeaturePreviews.Feature.values();
                    for (FeaturePreviews.Feature feature : gradleFeatures) {
                        Boolean enabled = (Boolean) method.invoke(gradleFeaturePreviews, feature);
                        featurePreviews.put(feature.name(), new FeaturePreview(feature.name(), feature.isActive(), enabled));
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    // ignore
                }
            }
        } else {
            try {
                Class<?> reflectiveFeaturePreviewFetcher = Class.forName("org.openrewrite.gradle.marker.ReflectiveFeaturePreviewFetcher");
                Method getPreviewsMethod = reflectiveFeaturePreviewFetcher.getMethod("getPreviews", DefaultSettings.class);
                //noinspection unchecked
                featurePreviews.putAll((Map<String, FeaturePreview>) getPreviewsMethod.invoke(null, settings));
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                return featurePreviews;
            }
        }
        return featurePreviews;
    }

    private static <T> @Nullable T getService(DefaultSettings settings, @SuppressWarnings("SameParameterValue") Class<T> serviceType) {
        try {
            Method services = settings.getClass().getDeclaredMethod("getServices");
            services.setAccessible(true);
            ServiceRegistry serviceRegistry = (ServiceRegistry) services.invoke(settings);
            return serviceRegistry.get(serviceType);
        } catch (UnknownServiceException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }
}
