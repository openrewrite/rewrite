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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.RecipeSerializer;
import org.openrewrite.gradle.attributes.Category;
import org.openrewrite.gradle.attributes.ProjectAttribute;
import org.openrewrite.gradle.toolingapi.OpenRewriteModel;
import org.openrewrite.gradle.toolingapi.OpenRewriteModelBuilder;
import org.openrewrite.maven.tree.ResolvedDependency;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * When running these in an IDE you should enable delegation to Gradle for best results.
 * Also uncomment embedded(true) in OpenRewriteModelBuilder and you should be able to hit breakpoints.
 */
class GradleProjectTest {
    public static GradleVersion gradleVersion = System.getProperty("org.openrewrite.test.gradleVersion") == null ?
      GradleVersion.current() :
      GradleVersion.version(System.getProperty("org.openrewrite.test.gradleVersion"));

    public static boolean gradleOlderThan8() {
        return gradleVersion.compareTo(GradleVersion.version("8.0")) < 0;
    }

    static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    @SuppressWarnings("NotNullFieldNotInitialized")
    @Nested
    class gradle4Compatibility {
        @TempDir
        static Path dir;

        @SuppressWarnings("NotNullFieldNotInitialized")
        static GradleProject gradleProject;

        //language=groovy
        static String buildGradle = """
          plugins{
              id 'java'
          }

          repositories{
              mavenCentral()
          }

          dependencies{
              implementation 'org.openrewrite:rewrite-java:8.56.0'
          }
          """;

        //language=groovy
        static String settingsGradle = """
          rootProject.name = "sample"
          """;


        @BeforeAll
        static void gradleProject() throws IOException {
            try (InputStream is = new ByteArrayInputStream(buildGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(dir.resolve("build.gradle"), readAllBytes(is));
            }

            try (InputStream is = new ByteArrayInputStream(settingsGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(dir.resolve("settings.gradle"), readAllBytes(is));
            }

            OpenRewriteModel model = OpenRewriteModelBuilder.forProjectDirectory(dir.toFile(), dir.resolve("build.gradle").toFile());
            gradleProject = model.getGradleProject();
        }

        @Test
        void requestedCorrespondsDirectlyToResolved() {
            assertThat(requireNonNull(gradleProject.getConfiguration("compileClasspath")).getRequested())
              .hasSize(1);
            assertThat(requireNonNull(gradleProject.getConfiguration("compileClasspath")).getDirectResolved())
              .hasSize(1);
        }

        @Test
        void serializable() throws IOException {
            ObjectMapper m = new RecipeSerializer().getMapper();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            m.writeValue(baos, gradleProject);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            GradleProject roundTripped = m.readValue(bais, GradleProject.class);
            assertThat(roundTripped).isEqualTo(gradleProject);
        }

    }


    @SuppressWarnings("NotNullFieldNotInitialized")
    @Nested
    @DisabledIf("org.openrewrite.gradle.marker.GradleProjectTest#gradleOlderThan8")
    class singleProject {
        @TempDir
        static Path dir;

        static GradleProject gradleProject;

        //language=groovy
        static String buildGradle = """
          plugins{
              id 'java'
          }

          repositories{
              mavenCentral()
          }

          configurations.all {
              resolutionStrategy.eachDependency { details ->
                  if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                      details.useVersion('2.18.3')
                      details.because('CVE-2025-BAD')
                  }
              }
          }

          dependencies{
              constraints {
                  implementation('com.fasterxml.jackson.core:jackson-core:2.18.4') {
                      because 'CVE-2024-BAD'
                  }
              }

              implementation platform('org.openrewrite:rewrite-bom:8.56.0')
              implementation 'org.openrewrite:rewrite-java'
              implementation 'org.openrewrite.recipe:rewrite-java-dependencies:1.35.+'
          }
          """;

        //language=groovy
        static String settingsGradle = """
          rootProject.name = "sample"
          """;

        @BeforeAll
        static void gradleProject() throws IOException {
            try (InputStream is = new ByteArrayInputStream(buildGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(dir.resolve("build.gradle"), readAllBytes(is));
            }

            try (InputStream is = new ByteArrayInputStream(settingsGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(dir.resolve("settings.gradle"), readAllBytes(is));
            }

            OpenRewriteModel model = OpenRewriteModelBuilder.forProjectDirectory(dir.toFile(), dir.resolve("build.gradle").toFile());
            gradleProject = model.getGradleProject();
        }

        @Test
        void resolutionStrategy() {
            GradleDependencyConfiguration runtimeClasspath = requireNonNull(gradleProject.getConfiguration("runtimeClasspath"));
            assertThat(runtimeClasspath.getAllConstraints())
              .anyMatch(it -> "jackson-databind".equals(it.getArtifactId()) && "2.18.3".equals(it.getStrictVersion()));
            assertThat(runtimeClasspath.getResolved())
              .anyMatch(it -> "jackson-databind".equals(it.getArtifactId()) && "2.18.3".equals(it.getVersion()));
        }

        @Test
        void sameConfigurationIsReferentiallySame() {
            GradleDependencyConfiguration implementation = requireNonNull(gradleProject.getConfiguration("implementation"));
            GradleDependencyConfiguration runtimeClasspath = requireNonNull(gradleProject.getConfiguration("runtimeClasspath"));
            assertThat(runtimeClasspath.getExtendsFrom()).anyMatch(it -> it == implementation);
        }

        @Test
        void plusDependency() {
            GradleDependencyConfiguration runtimeClasspath = requireNonNull(gradleProject.getConfiguration("runtimeClasspath"));
            assertThat(runtimeClasspath.getRequested())
              .anyMatch(it -> "rewrite-java-dependencies".equals(it.getArtifactId()) && "1.35.+".equals(it.getVersion()));
            assertThat(runtimeClasspath.getDirectResolved())
              .anyMatch(it -> "rewrite-java-dependencies".equals(it.getArtifactId()) && "1.35.2".equals(it.getVersion()));
        }

        @Test
        void transitiveDependencies() {
            Map<Integer, Integer> dependenciesByDepth = new HashMap<>();
            for (GradleDependencyConfiguration configuration : gradleProject.getConfigurations()) {
                for (ResolvedDependency resolvedDependency : configuration.getResolved()) {
                    dependenciesByDepth.merge(resolvedDependency.getDepth(), 1, Integer::sum);
                }
            }
            assertThat(dependenciesByDepth).containsKeys(0, 1);
        }

        @Test
        void requestedCorrespondsDirectlyToResolved() {
            assertThat(requireNonNull(gradleProject.getConfiguration("compileClasspath")).getRequested())
              .hasSize(3);
            assertThat(requireNonNull(gradleProject.getConfiguration("compileClasspath")).getDirectResolved())
              .hasSize(3);
        }

        @Test
        void serializable() throws IOException {
            ObjectMapper m = new RecipeSerializer().getMapper();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            m.writeValue(baos, gradleProject);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            GradleProject roundTripped = m.readValue(bais, GradleProject.class);
            assertThat(roundTripped).isEqualTo(gradleProject);
        }

        @Test
        void transitiveDependencyConstraint() {
            GradleDependencyConfiguration runtimeClasspath = requireNonNull(gradleProject.getConfiguration("runtimeClasspath"));

            assertThat(runtimeClasspath)
              .extracting(GradleDependencyConfiguration::getAllConstraints)
              .asInstanceOf(InstanceOfAssertFactories.list(GradleDependencyConstraint.class))
              .as("runtime classpath should have inherited the implementation constraint on jackson-core:2.18.4")
              .anyMatch(constraint -> "com.fasterxml.jackson.core".equals(constraint.getGroupId()) && "jackson-core".equals(constraint.getArtifactId()) && "2.18.4".equals(constraint.getRequiredVersion()));

            assertThat(runtimeClasspath)
              .extracting(GradleDependencyConfiguration::getResolved)
              .asInstanceOf(InstanceOfAssertFactories.list(ResolvedDependency.class))
              .as("Constraint should have set the version of transitive jackson-core to 2.18.4")
              .anyMatch(dep -> "com.fasterxml.jackson.core".equals(dep.getGroupId()) && "jackson-core".equals(dep.getArtifactId()) && "2.18.4".equals(dep.getVersion()));
        }

        @Test
        void bom() {
            GradleDependencyConfiguration runtimeClasspath = requireNonNull(gradleProject.getConfiguration("runtimeClasspath"));
            assertThat(runtimeClasspath.getRequested())
              .as("rewrite-bom should be marked as a platform() dependency")
              .anyMatch(it -> it.findAttribute(Category.class)
                                .filter(Category.REGULAR_PLATFORM::equals)
                                .isPresent() &&
                              "rewrite-bom".equals(it.getArtifactId()) &&
                              "pom".equals(it.getType()));
            assertThat(runtimeClasspath.getDirectResolved())
              .filteredOn(it -> "rewrite-bom".equals(it.getArtifactId()))
              .singleElement()
              .extracting(ResolvedDependency::getType)
              .as("platform() dependencies should have type \"pom\" to help indicate that they aren't jars going onto a classpath")
              .isEqualTo("pom");
        }
    }

    @SuppressWarnings("NotNullFieldNotInitialized")
    @Nested
    class multiProject {
        @TempDir
        static Path dir;

        static GradleProject rootGradleProject;
        static GradleProject aGradleProject;
        static GradleProject bGradleProject;

        //language=groovy
        static String aBuildGradle = """
          plugins{
              id 'java'
          }

          repositories{
              mavenCentral()
          }

          dependencies{
              implementation 'org.openrewrite:rewrite-java:8.57.0'
              testImplementation("org.projectlombok:lombok:latest.release")
          }
          """;

        //language=groovy
        static String bBuildGradle = """
          plugins{
              id 'java'
          }

          repositories{
              mavenCentral()
          }

          dependencies{
              implementation project(":a")
              implementation 'org.openrewrite:rewrite-java:8.56.0'
          }
          """;

        //language=groovy
        static String settingsGradle = """
          rootProject.name = "sample"
          include("a")
          include("b")
          """;

        @BeforeAll
        static void gradleProject() throws IOException {
            try (InputStream is = new ByteArrayInputStream("\n".getBytes(StandardCharsets.UTF_8))) {
                Files.write(dir.resolve("build.gradle"), readAllBytes(is));
            }

            try (InputStream is = new ByteArrayInputStream(settingsGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(dir.resolve("settings.gradle"), readAllBytes(is));
            }

            Path aDir = dir.resolve("a");
            Files.createDirectory(aDir);
            try (InputStream is = new ByteArrayInputStream(aBuildGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(aDir.resolve("build.gradle"), readAllBytes(is));
            }

            Path bDir = dir.resolve("b");
            Files.createDirectory(bDir);
            try (InputStream is = new ByteArrayInputStream(bBuildGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(bDir.resolve("build.gradle"), readAllBytes(is));
            }


            OpenRewriteModel rootModel = OpenRewriteModelBuilder.forProjectDirectory(dir.toFile(), dir.resolve("build.gradle").toFile());
            rootGradleProject = rootModel.getGradleProject();
            OpenRewriteModel aModel = OpenRewriteModelBuilder.forProjectDirectory(aDir.toFile(), aDir.resolve("build.gradle").toFile());
            aGradleProject = aModel.getGradleProject();
            OpenRewriteModel bModel = OpenRewriteModelBuilder.forProjectDirectory(bDir.toFile(), bDir.resolve("build.gradle").toFile());
            bGradleProject = bModel.getGradleProject();
        }

        @Test
        void projectDependencyHasAttribute() {
            assertThat(requireNonNull(bGradleProject.getConfiguration("compileClasspath")).getRequested())
              .anyMatch(dep -> dep.findAttribute(ProjectAttribute.class).isPresent() && "a".equals(dep.getGav().getArtifactId()));
        }
    }
}
