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
package org.openrewrite.gradle.toolingapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.RecipeSerializer;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.internal.ListUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Value
public class OpenRewriteModel {

    private static final ObjectMapper mapper = new RecipeSerializer().getMapper();

    GradleProject gradleProject;


    org.openrewrite.gradle.marker. @Nullable GradleSettings gradleSettings;

    public static OpenRewriteModel from(OpenRewriteModelProxy proxy) {
        try {
            GradleProject project = mapper.readValue(proxy.getGradleProjectBytes(), GradleProject.class);
            GradleSettings settings = proxy.getGradleSettingsBytes() == null ? null : mapper.readValue(proxy.getGradleSettingsBytes(), GradleSettings.class);
            deduplicate(project, settings);
            return new OpenRewriteModel(project, settings);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Depending on ObjectMapper settings there may be multiple different objects representing the same information.
     */
    private static void deduplicate(GradleProject gp, @Nullable GradleSettings gs) {
        Map<String, GradleDependencyConfiguration> configurationCache = new HashMap<>();
        deduplicateConfigurations(gp.getConfigurations(), configurationCache);
        deduplicateConfigurations(gp.getBuildscript().getConfigurations(), configurationCache);
    }


    private static void deduplicateConfigurations(List<GradleDependencyConfiguration> configurations, Map<String, GradleDependencyConfiguration> cache) {
        for (GradleDependencyConfiguration conf : configurations) {
            cache.putIfAbsent(conf.getName(), conf);
        }
        for (GradleDependencyConfiguration conf : configurations) {
            List<GradleDependencyConfiguration> deduplicatedExtendsFrom = ListUtils.map(conf.getExtendsFrom(), it -> {
                assert it != null;
                return cache.getOrDefault(it.getName(), it);
            });
            conf.unsafeSetExtendsFrom(deduplicatedExtendsFrom);
        }
    }
}
