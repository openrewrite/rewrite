/*
 * Copyright 2026 the original author or authors.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.RecipeSerializer;
import org.openrewrite.gradle.marker.GradleProject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("gradle4")
class Gradle4CompatibilityTest {

    @Test
    void buildsModelWithProjectDependency(@TempDir Path projectDir) throws Exception {
        // given
        writeGradle4Build(projectDir, "gradle4-compatibility", "include 'api'\n", """
          plugins {
              id 'java'
          }
          dependencies {
              implementation project(':api')
          }
          """);
        Path apiDir = Files.createDirectory(projectDir.resolve("api"));
        Files.write(apiDir.resolve("build.gradle"), "apply plugin: 'java'\n".getBytes(UTF_8));

        // when
        OpenRewriteModel model = buildModel(projectDir);

        // then
        GradleProject project = model.getGradleProject();
        assertThat(project.getName()).isEqualTo("gradle4-compatibility");
        assertThat(model.getGradleProjectsByPath()).containsOnlyKeys(":", ":api");
        assertThat(model.getGradleProjectsByPath().get(":api").getName()).isEqualTo("api");
        assertThat(requireNonNull(project.getConfiguration("compileClasspath")).getRequested())
          .singleElement().satisfies(dependency -> assertThat(dependency.getArtifactId()).isEqualTo("api"));
        assertThat(requireNonNull(project.getConfiguration("compileClasspath")).getDirectResolved())
          .singleElement().satisfies(dependency -> assertThat(dependency.getArtifactId()).isEqualTo("api"));
        assertThat(model.getGradleSettings()).isNotNull();
    }

    @Test
    void resolvesMavenCentralDependencyAndSerializes(@TempDir Path projectDir) throws Exception {
        // given
        writeGradle4Build(projectDir, "sample", "", """
          plugins {
              id 'java'
          }
          repositories {
              mavenCentral()
          }
          dependencies {
              implementation 'org.openrewrite:rewrite-java:8.56.0'
          }
          """);

        // when
        GradleProject project = buildModel(projectDir).getGradleProject();
        ObjectMapper mapper = new RecipeSerializer().getMapper();
        GradleProject roundTripped = mapper.readValue(mapper.writeValueAsBytes(project), GradleProject.class);

        // then
        assertThat(requireNonNull(project.getConfiguration("compileClasspath")).getRequested())
          .singleElement().satisfies(dependency -> assertThat(dependency.getArtifactId()).isEqualTo("rewrite-java"));
        assertThat(requireNonNull(project.getConfiguration("compileClasspath")).getDirectResolved())
          .singleElement().satisfies(dependency -> assertThat(dependency.getVersion()).isEqualTo("8.56.0"));
        assertThat(roundTripped).isEqualTo(project);
    }

    private static void writeGradle4Build(Path projectDir, String rootProjectName, String settingsBody,
                                          String buildGradle) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("org.gradle.java.home",
          requireNonNull(System.getProperty("org.openrewrite.test.gradleJavaHome")));
        try (OutputStream out = Files.newOutputStream(projectDir.resolve("gradle.properties"))) {
            properties.store(out, null);
        }
        Files.write(projectDir.resolve("settings.gradle"),
          ("rootProject.name = '" + rootProjectName + "'\n" + settingsBody).getBytes(UTF_8));
        Files.write(projectDir.resolve("build.gradle"), (buildGradle + """
          assert gradle.gradleVersion == '4.10'
          assert System.getProperty('java.specification.version') == '1.8'
          """).getBytes(UTF_8));
    }

    private static OpenRewriteModel buildModel(Path projectDir) throws IOException {
        Path gradleUserHome = Paths.get(
          requireNonNull(System.getProperty("org.openrewrite.gradle.local.gradle-user-homes")))
          .resolve("Gradle4CompatibilityTest");
        return OpenRewriteModelBuilder.forProjectDirectory(
          projectDir.toFile(), projectDir.resolve("build.gradle").toFile(), null, gradleUserHome.toFile());
    }
}
