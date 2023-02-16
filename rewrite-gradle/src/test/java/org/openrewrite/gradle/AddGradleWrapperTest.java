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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.PathUtils;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.remote.Remote;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.util.GradleWrapper.WRAPPER_JAR_LOCATION;
import static org.openrewrite.gradle.util.GradleWrapperTest.mockGradleServices;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.test.SourceSpecs.dir;
import static org.openrewrite.test.SourceSpecs.other;

@SuppressWarnings("UnusedProperty")
class AddGradleWrapperTest implements RewriteTest {


    private <S extends SourceFile> S result(RecipeRun run, Class<S> clazz, String endsWith) {
        return run.getResults().stream()
          .map(Result::getAfter)
          .filter(Objects::nonNull)
          .filter(r -> r.getSourcePath().endsWith(endsWith))
          .findFirst()
          .map(clazz::cast)
          .orElseThrow();
    }

    @ParameterizedTest
    @ValueSource(strings = {"latest.release","  "})
    @NullSource
    void addWrapperWithReleaseTag(String desiredVersion) {
        mockGradleServices(mockWebServer -> {
            String expectedUrl = "http://%s:%d/distributions/gradle-7.6-bin.zip".formatted(mockWebServer.getHostName(), mockWebServer.getPort());
            rewriteRun(
              spec ->spec
                  .recipe(
                    //language=yaml
                    new ByteArrayInputStream(
                      """
                        ---
                        type: specs.openrewrite.org/v1beta/recipe
                        name: org.openrewrite.test.AddGradleWrapper
                        displayName: Adds a Gradle wrapper
                        description: Add latest release of the gradle wrapper
                        recipeList:
                          - org.openrewrite.gradle.AddGradleWrapper:
                              version: %s
                              repositoryUrl: http://%s:%d/versions/all
                        """
                        .formatted(desiredVersion, mockWebServer.getHostName(), mockWebServer.getPort())
                        .getBytes()
                    ),
                    "org.openrewrite.test.AddGradleWrapper"
                  )
                  .afterRecipe(run -> {

                      validateGradlewFiles(run);

                      var gradleWrapperJar = result(run, Remote.class, "gradle-wrapper.jar");
                      assertThat(PathUtils.equalIgnoringSeparators(gradleWrapperJar.getSourcePath(), WRAPPER_JAR_LOCATION)).isTrue();

                      assertThat(gradleWrapperJar.getUri()).isEqualTo(URI.create(expectedUrl));

                  }),

              buildGradle(""),

              dir(
                "gradle/wrapper",
                properties(null, createWrapperPropertyFile(expectedUrl,"7ba68c54029790ab444b39d7e293d3236b2632631fb5f2e012bb28b4ff669e4b"), spec -> spec.path(Paths.get("gradle-wrapper.properties")))
              ));
        });
    }

    private static String createWrapperPropertyFile(String url, String hash) {
        return """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=%s
              distributionSha256Sum=%s
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
          """.formatted(url.replace("://", "\\://"), hash);
    }


    private void validateGradlewFiles(RecipeRun run) {
        var gradleSh = result(run, PlainText.class, "gradlew");
        assertThat(gradleSh.getText()).isNotBlank();
        assertThat(gradleSh.getFileAttributes()).isNotNull();
        assertThat(gradleSh.getFileAttributes().isExecutable()).isTrue();

        var gradleBat = result(run, PlainText.class, "gradlew.bat");
        assertThat(gradleBat.getText()).isNotBlank();
        assertThat(gradleBat.getFileAttributes()).isNotNull();
        assertThat(gradleBat.getFileAttributes().isExecutable()).isTrue();
    }

    @Test
    void addWrapperWhenIncomplete() {
        mockGradleServices(mockWebServer -> {
            rewriteRun(
              spec -> spec
                .recipe(
                  //language=yaml
                  new ByteArrayInputStream(
                    """
                      ---
                      type: specs.openrewrite.org/v1beta/recipe
                      name: org.openrewrite.test.AddGradleWrapper
                      displayName: Adds a Gradle wrapper
                      description: Add latest release of the gradle wrapper
                      recipeList:
                        - org.openrewrite.gradle.AddGradleWrapper:
                            version: %s
                            repositoryUrl: http://%s:%d/versions/all
                      """
                      .formatted("7.6", mockWebServer.getHostName(), mockWebServer.getPort())
                      .getBytes()
                  ),
                  "org.openrewrite.test.AddGradleWrapper"
                )
                .afterRecipe(run -> {
                  var gradleWrapperJar = result(run, Remote.class, "gradle-wrapper.jar");
                  assertThat(PathUtils.equalIgnoringSeparators(gradleWrapperJar.getSourcePath(), WRAPPER_JAR_LOCATION)).isTrue();
                  assertThat(gradleWrapperJar.getUri()).isEqualTo(
                    URI.create("http://%s:%d/distributions/gradle-7.6-bin.zip"
                      .formatted(mockWebServer.getHostName(), mockWebServer.getPort())));
              }).expectedCyclesThatMakeChanges(1),
              other("", spec -> spec.path("gradlew")),
              other("", spec -> spec.path("gradlew.bat")),
              other("", spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")),
              buildGradle("")
            );
        });
    }

    @Disabled
    @Test
    void addWrapperToGradleKotlin() {
        mockGradleServices(mockWebServer -> {
            rewriteRun(
              spec -> spec
                .recipe(
                  //language=yaml
                  new ByteArrayInputStream(
                    """
                      ---
                      type: specs.openrewrite.org/v1beta/recipe
                      name: org.openrewrite.test.AddGradleWrapper
                      displayName: Adds a Gradle wrapper
                      description: Add latest release of the gradle wrapper
                      recipeList:
                        - org.openrewrite.gradle.AddGradleWrapper:
                            version: %s
                            repositoryUrl: http://%s:%d/versions/all
                      """
                      .formatted("7.6", mockWebServer.getHostName(), mockWebServer.getPort())
                      .getBytes()
                  ),
                  "org.openrewrite.test.AddGradleWrapper"
                )
                .afterRecipe(run -> assertThat(run.getResults()).isNotEmpty())
                .expectedCyclesThatMakeChanges(1),
              other("pretend this is a kotlin build file", spec -> spec.path("build.gradle.kts"))
            );
        });
    }

    @Test
    void dontAddWrapperToMavenProject() {
        rewriteRun(
          spec -> spec.afterRecipe(run -> assertThat(run.getResults()).isEmpty()),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
              </project>
              """
          )
        );
    }
}
