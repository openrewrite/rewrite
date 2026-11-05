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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class OpenRewriteModelBuilderTest {

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

    @Test
    void buildsInTheGivenGradleUserHome(@TempDir Path tmp) throws IOException {
        // The home's daemon holds files in it, so the home can't be a @TempDir. The build cleans up the homes directory.
        String homes = System.getProperty("org.openrewrite.gradle.local.gradle-user-homes");
        assumeTrue(homes != null, "Run by a build that names a directory for Gradle user homes");
        Path gradleUserHome = Paths.get(homes).resolve("OpenRewriteModelBuilderTest-" + UUID.randomUUID());
        Path project = Files.createDirectories(tmp.resolve("project"));
        Files.write(project.resolve("settings.gradle"), "rootProject.name = 'sample'\n".getBytes(UTF_8));
        Files.write(project.resolve("build.gradle"), new byte[0]);

        OpenRewriteModel model = OpenRewriteModelBuilder.forProjectDirectory(project.toFile(), null, null, gradleUserHome.toFile());

        assertThat(model.getGradleProject().getName()).isEqualTo("sample");
        assertThat(gradleUserHome.resolve("caches")).isDirectory();
    }
}
