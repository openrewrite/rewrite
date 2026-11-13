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

import org.gradle.tooling.GradleConnectionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.gradle.DependencyVersionSelector;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.MavenRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OpenRewriteModelBuilderTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void centralMirrorPreservesPluginPortal(boolean explicitPluginPortal, @TempDir Path projectDir) throws Exception {
        // The test process may also have a mirror configured, whose script uses the same local variable names.
        String init = OpenRewriteModelBuilder.generateInitScriptFromManifest() + "\n({ ->\n" +
          OpenRewriteModelBuilder.mirrorScriptSnippet("https://central-mirror.example/repository", "user", "password") + "\n})()\n";
        Files.write(projectDir.resolve("settings.gradle"), (explicitPluginPortal ?
          "pluginManagement { repositories { gradlePluginPortal() } }" : "").getBytes(StandardCharsets.UTF_8));
        Files.write(projectDir.resolve("build.gradle"),
          "plugins { id 'java' }\nrepositories { mavenCentral() }".getBytes(StandardCharsets.UTF_8));

        OpenRewriteModel model = OpenRewriteModelBuilder.forProjectDirectory(projectDir.toFile(), null, init);

        assertThat(model.getGradleProject().getMavenRepositories())
          .extracting(MavenRepository::getUri)
          .isNotEmpty()
          .doesNotContain("https://repo.maven.apache.org/maven2/");
        assertThat(model.getGradleProject().getBuildscript().getMavenRepositories())
          .extracting(MavenRepository::getUri)
          .containsExactly("https://plugins.gradle.org/m2");
        // Boot 2.1 plugin markers were published only to the plugin portal, not Maven Central.
        assertThat(new DependencyVersionSelector(null, model.getGradleProject(), null).select(
          new GroupArtifact("org.springframework.boot", "org.springframework.boot.gradle.plugin"),
          "classpath", "2.1.x", null, new InMemoryExecutionContext()))
          .matches("2\\.1\\.\\d+\\.RELEASE");
    }

    @Test
    void defaultInitScriptLoadsThePluginAtTheModelsOwnVersion() throws IOException {
        assertThat(OpenRewriteModelBuilder.defaultInitScript("8.98.0-SNAPSHOT"))
          .contains("classpath 'org.openrewrite.gradle.tooling:plugin:8.98.0-SNAPSHOT'")
          .doesNotContain("latest.integration");
        // A model loaded from a classes directory has no version to match
        assertThat(OpenRewriteModelBuilder.defaultInitScript(null))
          .contains("classpath 'org.openrewrite.gradle.tooling:plugin:latest.integration'");
    }

    @Test
    void onlyFailingToFindThePluginNamesTheCredentialsForItsRepository() {
        GradleConnectionException missingPlugin = new GradleConnectionException(
          "Could not fetch model of type 'OpenRewriteModelProxy' using Gradle distribution",
          new RuntimeException("Could not find org.openrewrite.gradle.tooling:plugin:8.98.0-SNAPSHOT."));
        assertThat(OpenRewriteModelBuilder.explainMissingPlugin(missingPlugin, "8.98.0-SNAPSHOT"))
          .hasMessageContaining("codegenomeUsername")
          .hasMessageContaining("ORG_GRADLE_PROJECT_")
          .hasMessageContaining("CODEGENOME_USERNAME")
          .hasCause(missingPlugin);

        GradleConnectionException brokenBuildScript = new GradleConnectionException(
          "Could not fetch model of type 'OpenRewriteModelProxy' using Gradle distribution",
          new RuntimeException("Could not compile build file 'build.gradle'."));
        assertThat(OpenRewriteModelBuilder.explainMissingPlugin(brokenBuildScript, "8.98.0-SNAPSHOT"))
          .isSameAs(brokenBuildScript);
    }
}
