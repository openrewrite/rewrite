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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.invocation.DefaultGradle;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;
import org.gradle.util.GradleVersion;
import org.openrewrite.RecipeSerializer;
import org.openrewrite.gradle.marker.GradleProjectBuilder;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.gradle.marker.GradleSettingsBuilder;
import org.openrewrite.maven.tree.MavenRepository;

import javax.inject.Inject;
import java.io.File;

@SuppressWarnings("unused")
public class ToolingApiOpenRewriteModelPlugin implements Plugin<Project> {
    private final ToolingModelBuilderRegistry registry;

    @Inject
    public ToolingApiOpenRewriteModelPlugin(ToolingModelBuilderRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void apply(Project project) {
        registry.register(new OpenRewriteModelBuilder());
    }

    private static final ObjectMapper mapper = new RecipeSerializer().getMapper().copy().addMixIn(MavenRepository.class, MavenRepositoryMixin.class);

    private static class OpenRewriteModelBuilder implements ToolingModelBuilder {
        @Override
        public boolean canBuild(String modelName) {
            return modelName.equals(OpenRewriteModelProxy.class.getName());
        }

        @Override
        public Object buildAll(String modelName, Project project) {
            try {
                org.openrewrite.gradle.marker.GradleProject gradleProject = GradleProjectBuilder.gradleProject(project);
                byte[] gradleProjectBytes = mapper.writeValueAsBytes(gradleProject);

                byte[] gradleSettingsBytes = null;
                if (GradleVersion.current().compareTo(GradleVersion.version("4.4")) >= 0 &&
                    (new File(project.getProjectDir(), "settings.gradle").exists() ||
                     new File(project.getProjectDir(), "settings.gradle.kts").exists())) {
                    GradleSettings gradleSettings = GradleSettingsBuilder.gradleSettings(((DefaultGradle) project.getGradle()).getSettings());
                    gradleSettingsBytes = mapper.writeValueAsBytes(gradleSettings);
                }
                return new OpenRewriteModelImpl(gradleProjectBytes, gradleSettingsBytes);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize Gradle model to JSON", e);
            }
        }
    }

    interface MavenRepositoryMixin {
        @JsonProperty(access = JsonProperty.Access.READ_WRITE)
        String getUsername();

        @JsonProperty(access = JsonProperty.Access.READ_WRITE)
        String getPassword();
    }
}
