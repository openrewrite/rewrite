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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
    void mirrorsThePluginPortalAtCodeGenomeProjectsGradlePluginsBesideMavenPublic() {
        assertThat(OpenRewriteModelBuilder.mirrorScriptSnippet(mirror("https://artifacts.codegenomeproject.org/maven-public/")::get))
          .contains("def __rewriteMirrorUrl = 'https://artifacts.codegenomeproject.org/maven-public/'")
          .contains("def __rewritePortalMirrorUrl = 'https://artifacts.codegenomeproject.org/gradle-plugins/'")
          .contains("repo.url = repo.url.host == 'plugins.gradle.org' ? __rewritePortalMirrorUrl : __rewriteMirrorUrl");
    }

    @Test
    void mirrorsThePluginPortalAtAnyOtherMirrorItself() {
        assertThat(OpenRewriteModelBuilder.mirrorScriptSnippet(mirror("https://mirror.example.com/maven")::get))
          .contains("def __rewritePortalMirrorUrl = 'https://mirror.example.com/maven'");
    }

    @Test
    void noMirrorWithoutCredentials() {
        Map<String, String> env = mirror("https://artifacts.codegenomeproject.org/maven-public/");
        env.remove("MAVEN_CENTRAL_MIRROR_PASSWORD");
        assertThat(OpenRewriteModelBuilder.mirrorScriptSnippet(env::get)).isEmpty();
    }

    private static Map<String, String> mirror(String url) {
        Map<String, String> env = new HashMap<>();
        env.put("MAVEN_CENTRAL_MIRROR_URL", url);
        env.put("MAVEN_CENTRAL_MIRROR_USERNAME", "user");
        env.put("MAVEN_CENTRAL_MIRROR_PASSWORD", "password");
        return env;
    }
}
