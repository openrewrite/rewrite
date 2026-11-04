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
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class OpenRewriteModelBuilderTest {

    @Test
    void defaultInitScriptLoadsThePluginAtTheModelsOwnVersion() throws IOException {
        assertThat(OpenRewriteModelBuilder.defaultInitScript("8.98.0-SNAPSHOT", null))
          .contains("classpath 'org.openrewrite.gradle.tooling:plugin:8.98.0-SNAPSHOT'")
          .doesNotContain("latest.integration");
        // A model loaded from a classes directory has no version to match
        assertThat(OpenRewriteModelBuilder.defaultInitScript(null, null))
          .contains("classpath 'org.openrewrite.gradle.tooling:plugin:latest.integration'");
    }

    @Test
    void defaultInitScriptLoadsThePluginFromTheRepositoryOnDiskTheModelCameFrom(@TempDir Path repository) throws IOException {
        Path modelJar = repository.resolve("org/openrewrite/gradle/tooling/model/8.98.0/model-8.98.0.jar");
        assertThat(OpenRewriteModelBuilder.defaultInitScript("8.98.0", modelJar))
          .contains("mavenLocal()\n        maven { url = uri('" + repository.toUri() + "') }")
          .contains("classpath 'org.openrewrite.gradle.tooling:plugin:8.98.0'");
    }

    @Test
    void aModelJarOutsideAMavenRepositoryAddsNoRepository(@TempDir Path dir) throws IOException {
        Path cached = dir.resolve("modules-2/files-2.1/org.openrewrite.gradle.tooling/model/8.98.0/0a1b2c/model-8.98.0.jar");
        assertThat(OpenRewriteModelBuilder.repositoryOnDisk(cached, "8.98.0")).isNull();
        Path otherVersion = dir.resolve("org/openrewrite/gradle/tooling/model/8.97.0/model-8.97.0.jar");
        assertThat(OpenRewriteModelBuilder.repositoryOnDisk(otherVersion, "8.98.0")).isNull();
        assertThat(OpenRewriteModelBuilder.repositoryOnDisk(Paths.get("model-8.98.0.jar"), "8.98.0")).isNull();
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
