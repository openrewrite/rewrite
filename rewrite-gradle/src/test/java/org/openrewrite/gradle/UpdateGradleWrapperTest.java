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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.remote.Remote;
import org.openrewrite.remote.RemoteArchive;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.text.PlainText;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.gradle.util.GradleWrapper.*;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.test.SourceSpecs.*;

@SuppressWarnings("UnusedProperty")
class UpdateGradleWrapperTest implements RewriteTest {
    private final UnaryOperator<@Nullable String> notEmpty = actual -> {
        assertThat(actual).isNotNull();
        return actual + "\n";
    };

    // Gradle wrapper script text for 7.4.2
    private static final String GRADLEW_TEXT = StringUtils.readFully(UpdateGradleWrapperTest.class.getResourceAsStream("gradlew-7.4.2"));
    private static final String GRADLEW_BAT_TEXT = StringUtils.readFully(UpdateGradleWrapperTest.class.getResourceAsStream("gradlew-7.4.2.bat"));

    private final SourceSpecs gradlew = text("", spec -> spec.path(WRAPPER_SCRIPT_LOCATION).after(notEmpty));
    private final SourceSpecs gradlewBat = text("", spec -> spec.path(WRAPPER_BATCH_LOCATION).after(notEmpty));
    private final SourceSpecs gradleWrapperJarQuark = other("", spec -> spec.path(WRAPPER_JAR_LOCATION));
    private final String wrapperJarChecksum = "29e49b10984e585d8118b7d0bc452f944e386458df27371b49b4ac1dec4b7fda";

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpdateGradleWrapper("7.4.2", null, null, null, null))
          .beforeRecipe(withToolingApi());
    }

    @DocumentExample("Update existing Gradle wrapper")
    @Test
    void updateWrapper() {
        rewriteRun(
          spec -> spec.allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "7.4")))
            .afterRecipe(run -> {
                var gradleSh = result(run, PlainText.class, "gradlew");
                assertThat(gradleSh.getSourcePath()).isEqualTo(WRAPPER_SCRIPT_LOCATION);
                assertThat(gradleSh.getText()).isEqualTo(GRADLEW_TEXT);
                //noinspection DataFlowIssue
                assertThat(gradleSh.getFileAttributes()).isNotNull();
                assertThat(gradleSh.getFileAttributes().isReadable()).isTrue();
                assertThat(gradleSh.getFileAttributes().isExecutable()).isTrue();

                var gradleBat = result(run, PlainText.class, "gradlew.bat");
                assertThat(gradleBat.getSourcePath()).isEqualTo(WRAPPER_BATCH_LOCATION);
                assertThat(gradleBat.getText()).isEqualTo(GRADLEW_BAT_TEXT);

                var gradleWrapperJar = result(run, RemoteArchive.class, "gradle-wrapper.jar");
                assertThat(gradleWrapperJar.getSourcePath()).isEqualTo(WRAPPER_JAR_LOCATION);
                assertThat(gradleWrapperJar.getUri()).isEqualTo(URI.create("https://services.gradle.org/distributions/gradle-7.4.2-bin.zip"));
                assertThat(isValidWrapperJar(gradleWrapperJar)).as("Wrapper jar is not valid").isTrue();
            }),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-7.4-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-7.4.2-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              distributionSha256Sum=29e49b10984e585d8118b7d0bc452f944e386458df27371b49b4ac1dec4b7fda
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
              .afterRecipe(gradleWrapperProperties ->
                assertThat(gradleWrapperProperties.getMarkers().findFirst(BuildTool.class)).hasValueSatisfying(buildTool -> {
                    assertThat(buildTool.getType()).isEqualTo(BuildTool.Type.Gradle);
                    assertThat(buildTool.getVersion()).isEqualTo("7.4.2");
                }))
          ),
          gradlew,
          gradlewBat,
          gradleWrapperJarQuark
        );
    }

    @Test
    void addGradleWrapper() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(1).afterRecipe(run -> {
              assertThat(run.getChangeset().getAllResults()).hasSize(4);

              var gradleSh = result(run, PlainText.class, "gradlew");
              assertThat(gradleSh.getSourcePath()).isEqualTo(WRAPPER_SCRIPT_LOCATION);
              assertThat(gradleSh.getText()).isEqualTo(GRADLEW_TEXT);
              //noinspection DataFlowIssue
              assertThat(gradleSh.getFileAttributes()).isNotNull();
              assertThat(gradleSh.getFileAttributes().isReadable()).isTrue();
              assertThat(gradleSh.getFileAttributes().isExecutable()).isTrue();

              var gradleBat = result(run, PlainText.class, "gradlew.bat");
              assertThat(gradleBat.getSourcePath()).isEqualTo(WRAPPER_BATCH_LOCATION);
              assertThat(gradleBat.getText()).isEqualTo(GRADLEW_BAT_TEXT);

              var gradleWrapperProperties = result(run, Properties.File.class, "gradle-wrapper.properties");
              assertThat(gradleWrapperProperties.getSourcePath()).isEqualTo(WRAPPER_PROPERTIES_LOCATION);

              var gradleWrapperJar = result(run, RemoteArchive.class, "gradle-wrapper.jar");
              assertThat(gradleWrapperJar.getSourcePath()).isEqualTo(WRAPPER_JAR_LOCATION);
              assertThat(gradleWrapperJar.getUri()).isEqualTo(URI.create("https://services.gradle.org/distributions/gradle-7.4.2-bin.zip"));
              assertThat(isValidWrapperJar(gradleWrapperJar)).as("Wrapper jar is not valid").isTrue();
          }),
          buildGradle(
            """
              plugins {
                  id "java"
              }
              """
          )
        );
    }

    @Test
    void updateVersionAndDistribution() {
        rewriteRun(
          spec -> spec.allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "7.4")))
            .afterRecipe(run -> {
                var gradleSh = result(run, PlainText.class, "gradlew");
                assertThat(gradleSh.getText()).isNotBlank();

                var gradleBat = result(run, PlainText.class, "gradlew.bat");
                assertThat(gradleBat.getText()).isNotBlank();

                var gradleWrapperJar = result(run, RemoteArchive.class, "gradle-wrapper.jar");
                assertThat(PathUtils.equalIgnoringSeparators(gradleWrapperJar.getSourcePath(), WRAPPER_JAR_LOCATION)).isTrue();
                assertThat(gradleWrapperJar.getUri()).isEqualTo(URI.create("https://services.gradle.org/distributions/gradle-7.4.2-bin.zip"));
                assertThat(isValidWrapperJar(gradleWrapperJar)).as("Wrapper jar is not valid").isTrue();
            }),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-7.4-all.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-7.4.2-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              distributionSha256Sum=29e49b10984e585d8118b7d0bc452f944e386458df27371b49b4ac1dec4b7fda
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
          ),
          gradlew,
          gradlewBat,
          gradleWrapperJarQuark
        );
    }

    @Test
    void updateChecksumAlreadySet() {
        rewriteRun(
          spec -> spec.allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "7.4")))
            .afterRecipe(run -> {
                var gradleWrapperJar = result(run, RemoteArchive.class, "gradle-wrapper.jar");
                assertThat(PathUtils.equalIgnoringSeparators(gradleWrapperJar.getSourcePath(), WRAPPER_JAR_LOCATION)).isTrue();
                assertThat(gradleWrapperJar.getUri()).isEqualTo(URI.create("https://services.gradle.org/distributions/gradle-7.4.2-bin.zip"));
                assertThat(isValidWrapperJar(gradleWrapperJar)).as("Wrapper jar is not valid").isTrue();
            }),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-7.4-all.zip
              distributionSha256Sum=cd5c2958a107ee7f0722004a12d0f8559b4564c34daad7df06cffd4d12a426d0
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-7.4.2-bin.zip
              distributionSha256Sum=29e49b10984e585d8118b7d0bc452f944e386458df27371b49b4ac1dec4b7fda
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
          ),
          gradlew,
          gradlewBat,
          gradleWrapperJarQuark
        );
    }

    @Test
    void dontAddMissingWrapper() {
        rewriteRun(
          spec -> spec.recipe(new UpdateGradleWrapper("7.x", null, Boolean.FALSE, null, null))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "7.4")))
            .afterRecipe(run -> assertThat(run.getChangeset().getAllResults()).isEmpty())
        );
    }

    @Test
    void updateMultipleWrappers() {
        rewriteRun(
          spec -> spec.allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "7.4")))
            .recipe(new UpdateGradleWrapper("7.4.2", null, Boolean.FALSE, null, null)),
          dir("example1",
            properties(
              """
                distributionBase=GRADLE_USER_HOME
                distributionPath=wrapper/dists
                distributionUrl=https\\://services.gradle.org/distributions/gradle-7.4-all.zip
                distributionSha256Sum=cd5c2958a107ee7f0722004a12d0f8559b4564c34daad7df06cffd4d12a426d0
                zipStoreBase=GRADLE_USER_HOME
                zipStorePath=wrapper/dists
                """,
              """
                distributionBase=GRADLE_USER_HOME
                distributionPath=wrapper/dists
                distributionUrl=https\\://services.gradle.org/distributions/gradle-7.4.2-bin.zip
                distributionSha256Sum=29e49b10984e585d8118b7d0bc452f944e386458df27371b49b4ac1dec4b7fda
                zipStoreBase=GRADLE_USER_HOME
                zipStorePath=wrapper/dists
                """,
              spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
            ),
            gradlew,
            gradlewBat,
            gradleWrapperJarQuark
          ),
          dir("example2",
            properties(
              """
                distributionBase=GRADLE_USER_HOME
                distributionPath=wrapper/dists
                distributionUrl=https\\://services.gradle.org/distributions/gradle-7.4-all.zip
                distributionSha256Sum=cd5c2958a107ee7f0722004a12d0f8559b4564c34daad7df06cffd4d12a426d0
                zipStoreBase=GRADLE_USER_HOME
                zipStorePath=wrapper/dists
                """,
              """
                distributionBase=GRADLE_USER_HOME
                distributionPath=wrapper/dists
                distributionUrl=https\\://services.gradle.org/distributions/gradle-7.4.2-bin.zip
                distributionSha256Sum=29e49b10984e585d8118b7d0bc452f944e386458df27371b49b4ac1dec4b7fda
                zipStoreBase=GRADLE_USER_HOME
                zipStorePath=wrapper/dists
                """,
              spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
            ),
            gradlew,
            gradlewBat,
            gradleWrapperJarQuark
          )
        );
    }

    /**
     * In Gradle 6.6+, the wrapper jar moved from `gradle-{version}/lib/plugins/gradle-plugins-{version}.jar!gradle-wrapper.jar`
     * to `gradle-{version}/lib/gradle-wrapper-{version}.jar!gradle-wrapper.jar`.
     */
    @Test
    void olderThan6_6() {
        rewriteRun(
          spec -> spec.recipe(new UpdateGradleWrapper("5.6.4", null, null, null, null))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "4.0")))
            .afterRecipe(run -> {
                var gradleSh = result(run, PlainText.class, "gradlew");
                assertThat(gradleSh.getText()).isNotBlank();

                var gradleBat = result(run, PlainText.class, "gradlew.bat");
                assertThat(gradleBat.getText()).isNotBlank();

                var gradleWrapperJar = result(run, RemoteArchive.class, "gradle-wrapper.jar");
                assertThat(PathUtils.equalIgnoringSeparators(gradleWrapperJar.getSourcePath(), WRAPPER_JAR_LOCATION)).isTrue();
                assertThat(gradleWrapperJar.getUri()).isEqualTo(URI.create("https://services.gradle.org/distributions/gradle-5.6.4-bin.zip"));
                assertThat(isValidWrapperJar(gradleWrapperJar)).as("Wrapper jar is not valid").isTrue();
            }),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-4.0-all.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-5.6.4-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              distributionSha256Sum=1f3067073041bc44554d0efe5d402a33bc3d3c93cc39ab684f308586d732a80d
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
          ),
          gradlew,
          gradlewBat,
          gradleWrapperJarQuark
        );
    }

    @Test
    void doNotDowngrade() {
        rewriteRun(
          spec -> spec.allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "7.5"))),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-7.5-all.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
          ),
          text("", spec -> spec.path("gradlew")),
          text("", spec -> spec.path("gradlew.bat")),
          other("", spec -> spec.path("gradle/wrapper/gradle-wrapper.jar"))
        );
    }

    @Test
    void allowUpdatingDistributionTypeWhenSameVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpdateGradleWrapper("5.6.x", "bin", null, null, null))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "5.6.4")))
            .afterRecipe(run -> {
                var gradleSh = result(run, PlainText.class, "gradlew");
                assertThat(gradleSh.getText()).isNotBlank();

                var gradleBat = result(run, PlainText.class, "gradlew.bat");
                assertThat(gradleBat.getText()).isNotBlank();

                var gradleWrapperJar = result(run, RemoteArchive.class, "gradle-wrapper.jar");
                assertThat(PathUtils.equalIgnoringSeparators(gradleWrapperJar.getSourcePath(), WRAPPER_JAR_LOCATION)).isTrue();
                assertThat(gradleWrapperJar.getUri()).isEqualTo(URI.create("https://services.gradle.org/distributions/gradle-5.6.4-bin.zip"));
                assertThat(isValidWrapperJar(gradleWrapperJar)).as("Wrapper jar is not valid").isTrue();
            }),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-5.6.4-all.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-5.6.4-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              distributionSha256Sum=1f3067073041bc44554d0efe5d402a33bc3d3c93cc39ab684f308586d732a80d
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
          ),
          gradlew,
          gradlewBat,
          gradleWrapperJarQuark
        );
    }

    @Test
    void defaultsToLatestRelease() {
        rewriteRun(
          spec -> spec.recipe(new UpdateGradleWrapper(null, null, null, null, null))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "7.4")))
            .afterRecipe(run -> {
                var gradleSh = result(run, PlainText.class, "gradlew");
                assertThat(gradleSh.getSourcePath()).isEqualTo(WRAPPER_SCRIPT_LOCATION);
                assertThat(gradleSh.getText()).isNotBlank();
                //noinspection DataFlowIssue
                assertThat(gradleSh.getFileAttributes()).isNotNull();
                assertThat(gradleSh.getFileAttributes().isReadable()).isTrue();
                assertThat(gradleSh.getFileAttributes().isExecutable()).isTrue();

                var gradleBat = result(run, PlainText.class, "gradlew.bat");
                assertThat(gradleBat.getSourcePath()).isEqualTo(WRAPPER_BATCH_LOCATION);
                assertThat(gradleBat.getText()).isNotBlank();

                var gradleWrapperJar = result(run, RemoteArchive.class, "gradle-wrapper.jar");
                assertThat(gradleWrapperJar.getSourcePath()).isEqualTo(WRAPPER_JAR_LOCATION);
                //noinspection OptionalGetWithoutIsPresent
                BuildTool buildTool = gradleWrapperJar.getMarkers().findFirst(BuildTool.class).get();
                assertThat(buildTool.getVersion()).isNotEqualTo("7.4");
                assertThat(gradleWrapperJar.getUri()).isEqualTo(URI.create("https://services.gradle.org/distributions/gradle-" + buildTool.getVersion() + "-bin.zip"));
                assertThat(isValidWrapperJar(gradleWrapperJar)).as("Wrapper jar is not valid").isTrue();
            }),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-7.4-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
              .after(after -> {
                  Matcher versionMatcher = Pattern.compile("gradle-(.*?)-bin.zip").matcher(after);
                  assertThat(versionMatcher.find()).isTrue();
                  String gradleVersion = versionMatcher.group(1);
                  assertThat(gradleVersion).isNotEqualTo("7.4");

                  Matcher checksumMatcher = Pattern.compile("distributionSha256Sum=(.*)").matcher(after);
                  assertThat(checksumMatcher.find()).isTrue();
                  String checksum = checksumMatcher.group(1);
                  assertThat(checksum).isNotBlank();

                  return """
                    distributionBase=GRADLE_USER_HOME
                    distributionPath=wrapper/dists
                    distributionUrl=https\\://services.gradle.org/distributions/gradle-%s-bin.zip
                    zipStoreBase=GRADLE_USER_HOME
                    zipStorePath=wrapper/dists
                    distributionSha256Sum=%s
                    """.formatted(gradleVersion, checksum);
              })
          ),
          gradlew,
          gradlewBat,
          gradleWrapperJarQuark
        );
    }

    @Test
    void skipWorkIfUpdatedEarlier() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
              """
                  type: specs.openrewrite.org/v1beta/recipe
                  name: org.openrewrite.gradle.MultipleWrapperUpdates
                  displayName: Multiple wrapper updates
                  description: Multiple wrapper updates.
                  recipeList:
                    - org.openrewrite.gradle.UpdateGradleWrapper:
                        version: 7.6.3
                        addIfMissing: false
                    - org.openrewrite.gradle.UpdateGradleWrapper:
                        version: 6.9.4
                        addIfMissing: false
                """,
              "org.openrewrite.gradle.MultipleWrapperUpdates")
            .cycles(1)
            .expectedCyclesThatMakeChanges(1)
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "5.6.4")))
            .afterRecipe(run -> {
                var gradleSh = result(run, PlainText.class, "gradlew");
                assertThat(gradleSh.getSourcePath()).isEqualTo(WRAPPER_SCRIPT_LOCATION);
                assertThat(gradleSh.getText()).isNotBlank();
                //noinspection DataFlowIssue
                assertThat(gradleSh.getFileAttributes()).isNotNull();
                assertThat(gradleSh.getFileAttributes().isReadable()).isTrue();
                assertThat(gradleSh.getFileAttributes().isExecutable()).isTrue();

                var gradleBat = result(run, PlainText.class, "gradlew.bat");
                assertThat(gradleBat.getSourcePath()).isEqualTo(WRAPPER_BATCH_LOCATION);
                assertThat(gradleBat.getText()).isNotBlank();

                var gradleWrapperJar = result(run, RemoteArchive.class, "gradle-wrapper.jar");
                assertThat(gradleWrapperJar.getSourcePath()).isEqualTo(WRAPPER_JAR_LOCATION);
                //noinspection OptionalGetWithoutIsPresent
                BuildTool buildTool = gradleWrapperJar.getMarkers().findFirst(BuildTool.class).get();
                assertThat(buildTool.getVersion()).isEqualTo("7.6.3");
                assertThat(gradleWrapperJar.getUri()).isEqualTo(URI.create("https://services.gradle.org/distributions/gradle-" + buildTool.getVersion() + "-bin.zip"));
                assertThat(isValidWrapperJar(gradleWrapperJar)).as("Wrapper jar is not valid").isTrue();
            }),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-5.6.4-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-7.6.3-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              distributionSha256Sum=740c2e472ee4326c33bf75a5c9f5cd1e69ecf3f9b580f6e236c86d1f3d98cfac
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
          ),
          gradlew,
          gradlewBat,
          gradleWrapperJarQuark
        );
    }

    @Test
    void getExactVersionWithPatternFromGradleServices() {
        rewriteRun(
          spec -> spec.recipe(new UpdateGradleWrapper("8.0.x", null, null, null, null))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "7.4"))),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-7.4-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
              .after(after -> {
                  Matcher checksumMatcher = Pattern.compile("distributionSha256Sum=(.*)").matcher(after);
                  assertThat(checksumMatcher.find()).isTrue();
                  String checksum = checksumMatcher.group(1);
                  assertThat(checksum).isNotBlank();

                  // language=properties
                  return """
                    distributionBase=GRADLE_USER_HOME
                    distributionPath=wrapper/dists
                    distributionUrl=https\\://services.gradle.org/distributions/gradle-8.0.2-bin.zip
                    zipStoreBase=GRADLE_USER_HOME
                    zipStorePath=wrapper/dists
                    distributionSha256Sum=%s
                    """.formatted(checksum);
              })
          ),
          gradlew,
          gradlewBat,
          gradleWrapperJarQuark
        );
    }

    @Test
    void preferExistingDistributionSourceWhenServicesGradleOrgUnavailable() {
        HttpSender unhelpfulSender = request -> {
            if (request.getUrl().toString().contains("services.gradle.org")) {
                throw new RuntimeException("I'm sorry Dave, I'm afraid I can't do that.");
            }
            if (request.getUrl().toString().contains("company.com/repo")) {
                InputStream zipInputStream = UpdateGradleWrapperTest.class.getClassLoader().getResourceAsStream("gradle-8.10-bin.zip");
                return new HttpSender.Response(200, zipInputStream, () -> {
                });
            }
            return new HttpUrlConnectionSender().send(request);
        };
        HttpSenderExecutionContextView ctx = HttpSenderExecutionContextView.view(new InMemoryExecutionContext())
          .setHttpSender(unhelpfulSender)
          .setLargeFileHttpSender(unhelpfulSender);
        rewriteRun(
          spec -> spec.recipe(new UpdateGradleWrapper("8.10", "bin", false, null, null))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "7.4")))
            .executionContext(ctx),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://company.com/repo/gradle-8.8-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
              .after(after -> {
                  Matcher distUrlMatcher = Pattern.compile("distributionUrl=(.*/gradle-(.*)-bin.zip)").matcher(after);
                  assertThat(distUrlMatcher.find()).as(after).isTrue();
                  assertThat(distUrlMatcher.group(1)).startsWith("https\\://company.com/repo");
                  assertThat(distUrlMatcher.group(2)).isEqualTo("8.10");
                  return after;
              })
          ),
          gradlew,
          gradlewBat,
          gradleWrapperJarQuark
        );
    }

    @Test
    void addWrapperWithCustomDistributionUri() {
        HttpSender customDistributionHost = request -> {
            if (request.getUrl().toString().contains("company.com")) {
                return new HttpSender.Response(200, UpdateGradleWrapperTest.class.getClassLoader().getResourceAsStream("gradle-8.10-bin.zip"), () -> {
                });
            }
            return new HttpUrlConnectionSender().send(request);
        };
        HttpSenderExecutionContextView ctx = HttpSenderExecutionContextView.view(new InMemoryExecutionContext())
          .setHttpSender(customDistributionHost)
          .setLargeFileHttpSender(customDistributionHost);
        rewriteRun(
          spec -> spec
            .recipe(new UpdateGradleWrapper(null, null, null, "https://company.com/repo/gradle-8.10-bin.zip", null))
            .expectedCyclesThatMakeChanges(1)
            .executionContext(ctx)
            .afterRecipe(run -> {
                assertThat(run.getChangeset().getAllResults()).hasSize(4);

                var gradleSh = result(run, PlainText.class, "gradlew");
                assertThat(gradleSh.getSourcePath()).isEqualTo(WRAPPER_SCRIPT_LOCATION);
                //noinspection DataFlowIssue
                assertThat(gradleSh.getFileAttributes()).isNotNull();
                assertThat(gradleSh.getFileAttributes().isReadable()).isTrue();
                assertThat(gradleSh.getFileAttributes().isExecutable()).isTrue();

                var gradleBat = result(run, PlainText.class, "gradlew.bat");
                assertThat(gradleBat.getSourcePath()).isEqualTo(WRAPPER_BATCH_LOCATION);

                var gradleWrapperProperties = result(run, Properties.File.class, "gradle-wrapper.properties");
                assertThat(gradleWrapperProperties.getSourcePath()).isEqualTo(WRAPPER_PROPERTIES_LOCATION);

                var gradleWrapperJar = result(run, RemoteArchive.class, "gradle-wrapper.jar");
                assertThat(gradleWrapperJar.getSourcePath()).isEqualTo(WRAPPER_JAR_LOCATION);
                assertThat(gradleWrapperJar.getUri()).isEqualTo(URI.create("https://company.com/repo/gradle-8.10-bin.zip"));
                assertThat(isValidWrapperJar(gradleWrapperJar)).as("Wrapper jar is not valid").isTrue();
            }),
          buildGradle(
            """
              plugins {
                  id "java"
              }
              """
          )
        );
    }

    @Test
    void addWrapperWithCustomDistributionUriAndDistributionChecksum() {
        HttpSender customDistributionHost = request -> {
            if (request.getUrl().toString().contains("company.com")) {
                return new HttpSender.Response(200, UpdateGradleWrapperTest.class.getClassLoader().getResourceAsStream("gradle-8.10-bin.zip"), () -> {
                });
            }
            return new HttpUrlConnectionSender().send(request);
        };
        HttpSenderExecutionContextView ctx = HttpSenderExecutionContextView.view(new InMemoryExecutionContext())
          .setHttpSender(customDistributionHost)
          .setLargeFileHttpSender(customDistributionHost);
        rewriteRun(
          spec -> spec
            .recipe(new UpdateGradleWrapper(null, null, null, "https://company.com/repo/gradle-8.10-bin.zip", wrapperJarChecksum))
            .expectedCyclesThatMakeChanges(1)
            .executionContext(ctx)
            .afterRecipe(run -> {
                var gradleWrapperProperties = result(run, Properties.File.class, "gradle-wrapper.properties");
                assertThat(gradleWrapperProperties.getSourcePath()).isEqualTo(WRAPPER_PROPERTIES_LOCATION);
                assertThat(gradleWrapperProperties.getContent().stream()
                  .filter(Properties.Entry.class::isInstance)
                  .map(Properties.Entry.class::cast)
                  .anyMatch(prop -> "distributionSha256Sum".equals(prop.getKey()) && wrapperJarChecksum.equals(prop.getValue().getText()))).isTrue();
            }),
          buildGradle(
            """
              plugins {
                  id "java"
              }
              """
          )
        );
    }

    @Test
    void migrateToCustomDistributionUri() {
        HttpSender customDistributionHost = request -> {
            if (request.getUrl().toString().contains("company.com")) {
                return new HttpSender.Response(200, UpdateGradleWrapperTest.class.getClassLoader().getResourceAsStream("gradle-8.10-bin.zip"), () -> {
                });
            }
            return new HttpUrlConnectionSender().send(request);
        };
        HttpSenderExecutionContextView ctx = HttpSenderExecutionContextView.view(new InMemoryExecutionContext())
          .setHttpSender(customDistributionHost)
          .setLargeFileHttpSender(customDistributionHost);
        rewriteRun(
          spec -> spec.recipe(new UpdateGradleWrapper(null, null, null, "https://company.com/repo/gradle-8.10-bin.zip", null))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "7.4")))
            .executionContext(ctx),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-5.6.4-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://company.com/repo/gradle-8.10-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
          ),
          gradlew,
          gradlewBat,
          gradleWrapperJarQuark
        );
    }

    @Test
    void removeShaDuringMigrationToCustomDistributionUri() {
        HttpSender customDistributionHost = request -> {
            if (request.getUrl().toString().contains("company.com")) {
                return new HttpSender.Response(200, UpdateGradleWrapperTest.class.getClassLoader().getResourceAsStream("gradle-8.10-bin.zip"), () -> {
                });
            }
            return new HttpUrlConnectionSender().send(request);
        };
        HttpSenderExecutionContextView ctx = HttpSenderExecutionContextView.view(new InMemoryExecutionContext())
          .setHttpSender(customDistributionHost)
          .setLargeFileHttpSender(customDistributionHost);
        rewriteRun(
          spec -> spec.recipe(new UpdateGradleWrapper(null, null, null, "https://company.com/repo/gradle-8.10-bin.zip", null))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "7.4")))
            .executionContext(ctx),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-7.4.2-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              distributionSha256Sum=29e49b10984e585d8118b7d0bc452f944e386458df27371b49b4ac1dec4b7fda
              """,
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://company.com/repo/gradle-8.10-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
          ),
          gradlew,
          gradlewBat,
          gradleWrapperJarQuark
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2651")
    @Test
    void updateWithCustomDistributionUri() {
        HttpSender customDistributionHost = request -> {
            if (request.getUrl().toString().contains("company.com")) {
                return new HttpSender.Response(200, UpdateGradleWrapperTest.class.getClassLoader().getResourceAsStream("gradle-8.10-bin.zip"), () -> {
                });
            }
            return new HttpUrlConnectionSender().send(request);
        };
        HttpSenderExecutionContextView ctx = HttpSenderExecutionContextView.view(new InMemoryExecutionContext())
          .setHttpSender(customDistributionHost)
          .setLargeFileHttpSender(customDistributionHost);
        rewriteRun(
          spec -> spec.recipe(new UpdateGradleWrapper("8.10", null, null, null, null))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "7.4")))
            .executionContext(ctx),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://company.com/repo/gradle-8.2-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://company.com/repo/gradle-8.10-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
          ),
          gradlew,
          gradlewBat,
          gradleWrapperJarQuark
        );
    }

    @Test
    void servicesGradleOrgUnavailableForCustomDistributionUri() {
        HttpSender unhelpfulSender = request -> {
            if (request.getUrl().toString().contains("services.gradle.org")) {
                throw new RuntimeException("I'm sorry Dave, I'm afraid I can't do that.");
            }
            if (request.getUrl().toString().contains("company.com")) {
                return new HttpSender.Response(200, UpdateGradleWrapperTest.class.getClassLoader().getResourceAsStream("gradle-8.10-bin.zip"), () -> {
                });
            }
            return new HttpUrlConnectionSender().send(request);
        };
        HttpSenderExecutionContextView ctx = HttpSenderExecutionContextView.view(new InMemoryExecutionContext())
          .setHttpSender(unhelpfulSender)
          .setLargeFileHttpSender(unhelpfulSender);
        rewriteRun(
          spec -> spec.recipe(new UpdateGradleWrapper("8.10", null, null, null, null))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "7.4")))
            .executionContext(ctx),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://company.com/repo/gradle-8.2-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://company.com/repo/gradle-8.10-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
          ),
          gradlew,
          gradlewBat,
          gradleWrapperJarQuark
        );
    }

    @Test
    void updateWrapperInSubDirectory() {
        rewriteRun(
          spec -> spec.allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "7.4")))
            .afterRecipe(run -> {
                Path subdir = Path.of("subdir");
                var gradleSh = result(run, PlainText.class, "gradlew");
                assertThat(gradleSh.getSourcePath()).isEqualTo(subdir.resolve(WRAPPER_SCRIPT_LOCATION));
                assertThat(gradleSh.getText()).isEqualTo(GRADLEW_TEXT);
                //noinspection DataFlowIssue
                assertThat(gradleSh.getFileAttributes()).isNotNull();
                assertThat(gradleSh.getFileAttributes().isReadable()).isTrue();
                assertThat(gradleSh.getFileAttributes().isExecutable()).isTrue();

                var gradleBat = result(run, PlainText.class, "gradlew.bat");
                assertThat(gradleBat.getSourcePath()).isEqualTo(subdir.resolve(WRAPPER_BATCH_LOCATION));
                assertThat(gradleBat.getText()).isEqualTo(GRADLEW_BAT_TEXT);

                var gradleWrapperJar = result(run, RemoteArchive.class, "gradle-wrapper.jar");
                assertThat(gradleWrapperJar.getSourcePath()).isEqualTo(subdir.resolve(WRAPPER_JAR_LOCATION));
                assertThat(gradleWrapperJar.getUri()).isEqualTo(URI.create("https://services.gradle.org/distributions/gradle-7.4.2-bin.zip"));
                assertThat(isValidWrapperJar(gradleWrapperJar)).as("Wrapper jar is not valid").isTrue();
            }),
          dir("subdir",
            properties(
              """
                distributionBase=GRADLE_USER_HOME
                distributionPath=wrapper/dists
                distributionUrl=https\\://services.gradle.org/distributions/gradle-7.4-bin.zip
                zipStoreBase=GRADLE_USER_HOME
                zipStorePath=wrapper/dists
                """,
              """
                distributionBase=GRADLE_USER_HOME
                distributionPath=wrapper/dists
                distributionUrl=https\\://services.gradle.org/distributions/gradle-7.4.2-bin.zip
                zipStoreBase=GRADLE_USER_HOME
                zipStorePath=wrapper/dists
                distributionSha256Sum=29e49b10984e585d8118b7d0bc452f944e386458df27371b49b4ac1dec4b7fda
                """,
              spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
                .afterRecipe(gradleWrapperProperties ->
                  assertThat(gradleWrapperProperties.getMarkers().findFirst(BuildTool.class)).hasValueSatisfying(buildTool -> {
                      assertThat(buildTool.getType()).isEqualTo(BuildTool.Type.Gradle);
                      assertThat(buildTool.getVersion()).isEqualTo("7.4.2");
                  }))
            ),
            gradlew,
            gradlewBat,
            gradleWrapperJarQuark
          )
        );
    }

    @Test
    void failRecipeIfBothVersionAndDistributionUriAreProvided() {
        assertThat(new UpdateGradleWrapper("7.4.2", "bin", false, "https://company.com/repo/gradle-7.4.2-bin.zip", null).validate().isInvalid()).isTrue();
    }

    @Test
    void supportsSemverAgainstJFrogArtifactory() {
        HttpSender customDistributionHost = request -> {
            String url = request.getUrl().toString();
            if ("https://artifactory.company.com/artifactory/api/storage/gradle-distributions".equals(url)) {
                return new HttpSender.Response(200, new ByteArrayInputStream("""
                  {
                    "repo" : "gradle-distributions",
                    "path" : "/",
                    "created" : "2025-08-25T07:12:28.801Z",
                    "createdBy" : "admin",
                    "lastModified" : "2025-08-25T07:12:28.801Z",
                    "modifiedBy" : "admin",
                    "lastUpdated" : "2025-08-25T07:12:28.801Z",
                    "children" : [ {
                      "uri" : "/gradle-8.10-bin.zip",
                      "folder" : false
                    }, {
                      "uri" : "/gradle-8.8-all.zip",
                      "folder" : false
                    }, {
                      "uri" : "/gradle-8.8-bin.zip",
                      "folder" : false
                    } ],
                    "uri" : "https://company.com/artifactory/api/storage/gradle-distributions"
                  }
                  """.getBytes(StandardCharsets.UTF_8)), () -> {
                });
            } else if ("https://artifactory.company.com/artifactory/gradle-distributions/gradle-8.8-all.zip".equals(url)) {
                return new HttpSender.Response(200, UpdateGradleWrapperTest.class.getClassLoader().getResourceAsStream("gradle-8.8-all.zip"), () -> {
                });
            }
            return new HttpUrlConnectionSender().send(request);
        };
        HttpSenderExecutionContextView ctx = HttpSenderExecutionContextView.view(new InMemoryExecutionContext())
          .setHttpSender(customDistributionHost)
          .setLargeFileHttpSender(customDistributionHost);
        rewriteRun(
          spec -> spec.recipe(new UpdateGradleWrapper("8.x", "all", null, null, null))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "8.2")))
            .executionContext(ctx),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://artifactory.company.com/artifactory/gradle-distributions/gradle-8.2-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://artifactory.company.com/artifactory/gradle-distributions/gradle-8.8-all.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
          ),
          gradlew,
          gradlewBat,
          gradleWrapperJarQuark
        );
    }

    @Test
    void supportsSemverAgainstJFrogArtifactoryWithPath() {
        HttpSender customDistributionHost = request -> {
            String url = request.getUrl().toString();
            if ("https://company.com/artifactory/api/storage/gradle-distributions/gradle/distributions".equals(url)) {
                return new HttpSender.Response(200, new ByteArrayInputStream("""
                  {
                    "repo" : "gradle-distributions",
                    "path" : "/gradle/distributions",
                    "created" : "2025-08-25T07:12:28.801Z",
                    "createdBy" : "admin",
                    "lastModified" : "2025-08-25T07:12:28.801Z",
                    "modifiedBy" : "admin",
                    "lastUpdated" : "2025-08-25T07:12:28.801Z",
                    "children" : [ {
                      "uri" : "/gradle-8.10-bin.zip",
                      "folder" : false
                    }, {
                      "uri" : "/gradle-8.8-all.zip",
                      "folder" : false
                    }, {
                      "uri" : "/gradle-8.8-bin.zip",
                      "folder" : false
                    } ],
                    "uri" : "https://company.com/artifactory/api/storage/gradle-distributions/gradle/distributions"
                  }
                  """.getBytes(StandardCharsets.UTF_8)), () -> {
                });
            } else if ("https://company.com/artifactory/gradle-distributions/gradle/distributions/gradle-8.10-bin.zip".equals(url)) {
                return new HttpSender.Response(200, UpdateGradleWrapperTest.class.getClassLoader().getResourceAsStream("gradle-8.10-bin.zip"), () -> {
                });
            }
            return new HttpUrlConnectionSender().send(request);
        };
        HttpSenderExecutionContextView ctx = HttpSenderExecutionContextView.view(new InMemoryExecutionContext())
          .setHttpSender(customDistributionHost)
          .setLargeFileHttpSender(customDistributionHost);
        rewriteRun(
          spec -> spec.recipe(new UpdateGradleWrapper("8.x", null, null, null, null))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "8.2")))
            .executionContext(ctx),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://company.com/artifactory/gradle-distributions/gradle/distributions/gradle-8.2-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://company.com/artifactory/gradle-distributions/gradle/distributions/gradle-8.10-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
          ),
          gradlew,
          gradlewBat,
          gradleWrapperJarQuark
        );
    }

    @Test
    void supportsSemverAgainstJFrogArtifactoryHtmlResponse() {
        HttpSender customDistributionHost = request -> {
            String url = request.getUrl().toString();
            if ("https://artifactory.company.com/artifactory/api/storage/gradle-distributions".equals(url)) {
                return new HttpSender.Response(200, new ByteArrayInputStream("""
                  <!DOCTYPE html>
                  <html>
                  <head><meta name="robots" content="noindex" />
                  <title>Index of gradle-distributions/</title>
                  </head>
                  <body>
                  <h1>Index of gradle-distributions/</h1>
                  <pre>Name                       Last modified      Size</pre><hr/>
                  <pre><a href="distributions">distributions</a>->                 -    -
                  <a href="gradle-8.6-bin.zip">gradle-8.6-bin.zip</a>          02-Feb-2024 16:58  126.64 MB
                  <a href="gradle-8.7-bin.zip">gradle-8.7-bin.zip</a>          22-Mar-2024 16:05  127.97 MB
                  <a href="gradle-8.8-bin.zip">gradle-8.8-bin.zip</a>          31-May-2024 21:58  131.64 MB
                  <a href="gradle-8.8-all.zip">gradle-8.8-all.zip</a>          31-May-2024 21:58  141.64 MB
                  <a href="gradle-8.9-bin.zip">gradle-8.9-bin.zip</a>          11-Jul-2024 14:51  129.81 MB
                  <a href="gradle-8.10-bin.zip">gradle-8.10-bin.zip</a>          11-Jul-2024 14:51  159.81 MB
                  </pre>
                  </body>
                  </html>
                  """.getBytes(StandardCharsets.UTF_8)), Map.of("Content-Type", List.of("text/html")),
                  () -> {
                  });

            } else if ("https://artifactory.company.com/artifactory/gradle-distributions/gradle-8.8-all.zip".equals(url)) {
                return new HttpSender.Response(200, UpdateGradleWrapperTest.class.getClassLoader().getResourceAsStream("gradle-8.8-all.zip"), () -> {
                });
            }
            return new HttpUrlConnectionSender().send(request);
        };
        HttpSenderExecutionContextView ctx = HttpSenderExecutionContextView.view(new InMemoryExecutionContext())
          .setHttpSender(customDistributionHost)
          .setLargeFileHttpSender(customDistributionHost);
        rewriteRun(
          spec -> spec.recipe(new UpdateGradleWrapper("8.x", "all", null, null, null))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "8.2")))
            .executionContext(ctx),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://artifactory.company.com/artifactory/gradle-distributions/gradle-8.2-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://artifactory.company.com/artifactory/gradle-distributions/gradle-8.8-all.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
          ),
          gradlew,
          gradlewBat,
          gradleWrapperJarQuark
        );
    }

    @Test
    void supportsSemverAgainstJFrogArtifactoryHtmlResponseWithPath() {
        HttpSender customDistributionHost = request -> {
            String url = request.getUrl().toString();
            if ("https://company.com/artifactory/api/storage/gradle-distributions/gradle/distributions".equals(url)) {
                return new HttpSender.Response(200, new ByteArrayInputStream("""
                  <!DOCTYPE html>
                  <html>
                  <head><meta name="robots" content="noindex" />
                  <title>Index of gradle-distributions/</title>
                  </head>
                  <body>
                  <h1>Index of gradle-distributions/</h1>
                  <pre>Name                       Last modified      Size</pre><hr/>
                  <pre><a href="distributions">distributions</a>->                 -    -
                  <a href="gradle-8.6-bin.zip">gradle-8.6-bin.zip</a>          02-Feb-2024 16:58  126.64 MB
                  <a href="gradle-8.7-bin.zip">gradle-8.7-bin.zip</a>          22-Mar-2024 16:05  127.97 MB
                  <a href="gradle-8.8-bin.zip">gradle-8.8-bin.zip</a>          31-May-2024 21:58  131.64 MB
                  <a href="gradle-8.8-all.zip">gradle-8.8-all.zip</a>          31-May-2024 21:58  141.64 MB
                  <a href="gradle-8.9-bin.zip">gradle-8.9-bin.zip</a>          11-Jul-2024 14:51  129.81 MB
                  <a href="gradle-8.10-bin.zip">gradle-8.10-bin.zip</a>          11-Jul-2024 14:51  159.81 MB
                  </pre>
                  </body>
                  </html>
                  """.getBytes(StandardCharsets.UTF_8)), Map.of("Content-Type", List.of("text/html")),
                  () -> {
                  });
            } else if ("https://company.com/artifactory/gradle-distributions/gradle/distributions/gradle-8.10-bin.zip".equals(url)) {
                return new HttpSender.Response(200, UpdateGradleWrapperTest.class.getClassLoader().getResourceAsStream("gradle-8.10-bin.zip"), () -> {
                });
            }
            return new HttpUrlConnectionSender().send(request);
        };
        HttpSenderExecutionContextView ctx = HttpSenderExecutionContextView.view(new InMemoryExecutionContext())
          .setHttpSender(customDistributionHost)
          .setLargeFileHttpSender(customDistributionHost);
        rewriteRun(
          spec -> spec.recipe(new UpdateGradleWrapper("8.x", null, null, null, null))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "8.2")))
            .executionContext(ctx),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://company.com/artifactory/gradle-distributions/gradle/distributions/gradle-8.2-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://company.com/artifactory/gradle-distributions/gradle/distributions/gradle-8.10-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
          ),
          gradlew,
          gradlewBat,
          gradleWrapperJarQuark
        );
    }

    private <S extends SourceFile> S result(RecipeRun run, Class<S> clazz, String endsWith) {
        return run.getChangeset().getAllResults().stream()
          .map(Result::getAfter)
          .filter(Objects::nonNull)
          .filter(r -> r.getSourcePath().endsWith(endsWith))
          .findFirst()
          .map(clazz::cast)
          .orElseThrow();
    }

    private boolean isValidWrapperJar(Remote gradleWrapperJar) {
        try {
            Path testWrapperJar = Files.createTempFile("gradle-wrapper", "jar");
            ExecutionContext ctx = new InMemoryExecutionContext();
            HttpSenderExecutionContextView.view(ctx).setHttpSender(new HttpUrlConnectionSender(Duration.ofSeconds(5), Duration.ofSeconds(5)));
            try (InputStream is = gradleWrapperJar.getInputStream(ctx)) {
                Files.copy(is, testWrapperJar, StandardCopyOption.REPLACE_EXISTING);
                try (FileSystem fs = FileSystems.newFileSystem(testWrapperJar)) {
                    return Files.exists(fs.getPath("org/gradle/cli/CommandLineParser.class"));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
