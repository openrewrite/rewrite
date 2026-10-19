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
package org.openrewrite.maven;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.utilities.MavenWrapper;
import org.openrewrite.properties.PropertiesParser;
import org.openrewrite.quark.Quark;
import org.openrewrite.remote.*;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.text.PlainText;

import javax.net.ssl.SSLException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.maven.utilities.MavenWrapper.*;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.test.SourceSpecs.*;

class UpdateMavenWrapperTest implements RewriteTest {
    private final UnaryOperator<@Nullable String> notEmpty = actual -> {
        assertThat(actual).isNotNull();
        return actual + "\n";
    };

    // Maven wrapper script text for 3.1.1
    private static final String MVNW_TEXT = StringUtils.readFully(UpdateMavenWrapperTest.class.getResourceAsStream("/mvnw"));
    private static final String MVNW_CMD_TEXT = StringUtils.readFully(UpdateMavenWrapperTest.class.getResourceAsStream("/mvnw.cmd"));

    private final SourceSpecs mvnw = text("", spec -> spec.path(WRAPPER_SCRIPT_LOCATION).after(notEmpty));
    private final SourceSpecs mvnwCmd = text("", spec -> spec.path(WRAPPER_BATCH_LOCATION).after(notEmpty));
    private final SourceSpecs mvnWrapperJarQuark = other("", spec -> spec.path(WRAPPER_JAR_LOCATION).after(notEmpty));

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpdateMavenWrapper("3.1.x", null, "3.8.x", null, null, Boolean.TRUE));
    }

    @DocumentExample("Add a new Maven wrapper")
    @Test
    void addMavenWrapper() {
        rewriteRun(
          spec -> spec.recipe(new UpdateMavenWrapper("3.1.x", null, "3.8.x", null, null, null))
            .afterRecipe(run -> {
                assertThat(run.getChangeset().getAllResults()).hasSize(4);

                var mvnw = result(run, PlainText.class, "mvnw");
                assertThat(mvnw.getSourcePath()).isEqualTo(WRAPPER_SCRIPT_LOCATION);
                assertThat(mvnw.getText()).isEqualTo(MVNW_TEXT);
                assertThat(mvnw.getFileAttributes()).isNotNull();
                assertThat(mvnw.getFileAttributes().isReadable()).isTrue();
                assertThat(mvnw.getFileAttributes().isWritable()).isTrue();

                var mvnwCmd = result(run, PlainText.class, "mvnw.cmd");
                assertThat(mvnwCmd.getSourcePath()).isEqualTo(WRAPPER_BATCH_LOCATION);
                assertThat(mvnwCmd.getText()).isEqualTo(MVNW_CMD_TEXT);

                var mavenWrapperJar = result(run, RemoteFile.class, "maven-wrapper.jar");
                assertThat(mavenWrapperJar.getSourcePath()).isEqualTo(WRAPPER_JAR_LOCATION);
                assertThat(mavenWrapperJar.getUri().toString()).endsWith("/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar");
                assertThat(isValidWrapperJar(mavenWrapperJar)).as("Wrapper jar is not valid").isTrue();
            }),
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
              </project>
              """
          ),
          properties(
            doesNotExist(),
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.9/apache-maven-3.8.9-bin.zip
              distributionSha256Sum=e50133ba6d4333bea8f8bae137c13198c8c90ded959466e13252b820b52cb68b
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar
              """),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
          )
        );
    }

    @Test
    void addMavenWrapperWithWrapperJarChecksumEnabled() {
        rewriteRun(
          spec -> spec.afterRecipe(run -> {
              assertThat(run.getChangeset().getAllResults()).hasSize(4);

              var mvnw = result(run, PlainText.class, "mvnw");
              assertThat(mvnw.getSourcePath()).isEqualTo(WRAPPER_SCRIPT_LOCATION);
              assertThat(mvnw.getText()).isEqualTo(MVNW_TEXT);
              assertThat(mvnw.getFileAttributes()).isNotNull();
              assertThat(mvnw.getFileAttributes().isReadable()).isTrue();
              assertThat(mvnw.getFileAttributes().isWritable()).isTrue();

              var mvnwCmd = result(run, PlainText.class, "mvnw.cmd");
              assertThat(mvnwCmd.getSourcePath()).isEqualTo(WRAPPER_BATCH_LOCATION);
              assertThat(mvnwCmd.getText()).isEqualTo(MVNW_CMD_TEXT);

              var mavenWrapperJar = result(run, RemoteFile.class, "maven-wrapper.jar");
              assertThat(mavenWrapperJar.getSourcePath()).isEqualTo(WRAPPER_JAR_LOCATION);
              assertThat(mavenWrapperJar.getUri().toString()).endsWith("/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar");
              assertThat(isValidWrapperJar(mavenWrapperJar)).as("Wrapper jar is not valid").isTrue();
          }),
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
              </project>
              """
          ),
          properties(
            doesNotExist(),
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.9/apache-maven-3.8.9-bin.zip
              distributionSha256Sum=e50133ba6d4333bea8f8bae137c13198c8c90ded959466e13252b820b52cb68b
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar
              wrapperSha256Sum=ff7f21f2ef81723377e3d42d06661c4e3af60cf4bdfb7579ac8f22051399942d
              """),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
          )
        );
    }

    @Test
    void updateWrapper() {
        rewriteRun(
          spec -> spec.recipe(new UpdateMavenWrapper("3.1.x", null, "3.8.x", null, null, null))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Maven, "3.8.0")))
            .afterRecipe(run -> {
                var mvnw = result(run, PlainText.class, "mvnw");
                assertThat(mvnw.getSourcePath()).isEqualTo(WRAPPER_SCRIPT_LOCATION);
                assertThat(mvnw.getText()).isEqualTo(MVNW_TEXT);
                assertThat(mvnw.getFileAttributes()).isNotNull();
                assertThat(mvnw.getFileAttributes().isReadable()).isTrue();
                assertThat(mvnw.getFileAttributes().isWritable()).isTrue();

                var mvnwCmd = result(run, PlainText.class, "mvnw.cmd");
                assertThat(mvnwCmd.getSourcePath()).isEqualTo(WRAPPER_BATCH_LOCATION);
                assertThat(mvnwCmd.getText()).isEqualTo(MVNW_CMD_TEXT);

                var mavenWrapperJar = result(run, RemoteFile.class, "maven-wrapper.jar");
                assertThat(mavenWrapperJar.getSourcePath()).isEqualTo(WRAPPER_JAR_LOCATION);
                assertThat(mavenWrapperJar.getUri().toString()).endsWith("/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar");
                assertThat(isValidWrapperJar(mavenWrapperJar)).as("Wrapper jar is not valid").isTrue();
            }),
          properties(
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.0/apache-maven-3.8.0-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.0/maven-wrapper-3.1.0.jar
              """),
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.9/apache-maven-3.8.9-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar
              distributionSha256Sum=e50133ba6d4333bea8f8bae137c13198c8c90ded959466e13252b820b52cb68b
              """),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
              .afterRecipe(mavenWrapperProperties ->
                assertThat(mavenWrapperProperties.getMarkers().findFirst(BuildTool.class)).hasValueSatisfying(buildTool -> {
                    assertThat(buildTool.getType()).isEqualTo(BuildTool.Type.Maven);
                    assertThat(buildTool.getVersion()).isEqualTo("3.8.9");
                }))
          ),
          mvnw,
          mvnwCmd,
          mvnWrapperJarQuark
        );
    }

    @Test
    void updateWrapperWithWrapperJarChecksumDisabledButChecksumAlreadyThere() {
        rewriteRun(
          spec -> spec.recipe(new UpdateMavenWrapper("3.1.x", null, "3.8.x", null, null, null))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Maven, "3.8.0")))
            .afterRecipe(run -> {
                var mvnw = result(run, PlainText.class, "mvnw");
                assertThat(mvnw.getSourcePath()).isEqualTo(WRAPPER_SCRIPT_LOCATION);
                assertThat(mvnw.getText()).isEqualTo(MVNW_TEXT);
                assertThat(mvnw.getFileAttributes()).isNotNull();
                assertThat(mvnw.getFileAttributes().isReadable()).isTrue();
                assertThat(mvnw.getFileAttributes().isWritable()).isTrue();

                var mvnwCmd = result(run, PlainText.class, "mvnw.cmd");
                assertThat(mvnwCmd.getSourcePath()).isEqualTo(WRAPPER_BATCH_LOCATION);
                assertThat(mvnwCmd.getText()).isEqualTo(MVNW_CMD_TEXT);

                var mavenWrapperJar = result(run, RemoteFile.class, "maven-wrapper.jar");
                assertThat(mavenWrapperJar.getSourcePath()).isEqualTo(WRAPPER_JAR_LOCATION);
                assertThat(mavenWrapperJar.getUri().toString()).endsWith("/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar");
                assertThat(isValidWrapperJar(mavenWrapperJar)).as("Wrapper jar is not valid").isTrue();
            }),
          properties(
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.1/apache-maven-3.8.1-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.0/maven-wrapper-3.1.0.jar
              distributionSha256Sum=ba1517f73c5c22cf39afa0d570c998e6e024f37c75569f5c5524a69ff00a7f1b
              wrapperSha256Sum=46b0acdfe3da08b3f40d25bd135858b6014ee62b92883768995c946a3b446bd6
              """),
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.9/apache-maven-3.8.9-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar
              distributionSha256Sum=e50133ba6d4333bea8f8bae137c13198c8c90ded959466e13252b820b52cb68b
              """),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
              .afterRecipe(mavenWrapperProperties ->
                assertThat(mavenWrapperProperties.getMarkers().findFirst(BuildTool.class)).hasValueSatisfying(buildTool -> {
                    assertThat(buildTool.getType()).isEqualTo(BuildTool.Type.Maven);
                    assertThat(buildTool.getVersion()).isEqualTo("3.8.9");
                }))
          ),
          mvnw,
          mvnwCmd,
          mvnWrapperJarQuark
        );
    }

    @Test
    void updateWrapperWithWrapperJarChecksumEnabled() {
        rewriteRun(
          spec -> spec.allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Maven, "3.8.0")))
            .afterRecipe(run -> {
                var mvnw = result(run, PlainText.class, "mvnw");
                assertThat(mvnw.getSourcePath()).isEqualTo(WRAPPER_SCRIPT_LOCATION);
                assertThat(mvnw.getText()).isEqualTo(MVNW_TEXT);
                assertThat(mvnw.getFileAttributes()).isNotNull();
                assertThat(mvnw.getFileAttributes().isReadable()).isTrue();
                assertThat(mvnw.getFileAttributes().isWritable()).isTrue();

                var mvnwCmd = result(run, PlainText.class, "mvnw.cmd");
                assertThat(mvnwCmd.getSourcePath()).isEqualTo(WRAPPER_BATCH_LOCATION);
                assertThat(mvnwCmd.getText()).isEqualTo(MVNW_CMD_TEXT);

                var mavenWrapperJar = result(run, RemoteFile.class, "maven-wrapper.jar");
                assertThat(mavenWrapperJar.getSourcePath()).isEqualTo(WRAPPER_JAR_LOCATION);
                assertThat(mavenWrapperJar.getUri().toString()).endsWith("/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar");
                assertThat(isValidWrapperJar(mavenWrapperJar)).as("Wrapper jar is not valid").isTrue();
            }),
          properties(
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.0/apache-maven-3.8.0-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.0/maven-wrapper-3.1.0.jar
              """),
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.9/apache-maven-3.8.9-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar
              distributionSha256Sum=e50133ba6d4333bea8f8bae137c13198c8c90ded959466e13252b820b52cb68b
              wrapperSha256Sum=ff7f21f2ef81723377e3d42d06661c4e3af60cf4bdfb7579ac8f22051399942d
              """),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
              .afterRecipe(mavenWrapperProperties ->
                assertThat(mavenWrapperProperties.getMarkers().findFirst(BuildTool.class)).hasValueSatisfying(buildTool -> {
                    assertThat(buildTool.getType()).isEqualTo(BuildTool.Type.Maven);
                    assertThat(buildTool.getVersion()).isEqualTo("3.8.9");
                }))
          ),
          mvnw,
          mvnwCmd,
          mvnWrapperJarQuark
        );
    }

    @Test
    void doesNotAddWrapperToNonMavenProject() {
        rewriteRun(
          spec -> spec.recipe(new UpdateMavenWrapper("3.1.x", null, "3.8.x", null, null, null))
            .afterRecipe(run ->
              assertThat(run.getChangeset().getAllResults()).isEmpty()
            ),
          text(
            """
              Some random file content
              """
          )
        );
    }

    /**
     * When multiple independent Maven projects exist in subdirectories without a root {@code pom.xml},
     * the recipe adds wrapper files at the root level. This allows all subprojects to use
     * the same wrapper from the repository root.
     */
    @Test
    void addsWrapperToRootWhenMultipleIndependentMavenProjectsExist() {
        rewriteRun(
          spec -> spec.recipe(new UpdateMavenWrapper("3.1.x", null, "3.8.x", null, null, null))
            .expectedCyclesThatMakeChanges(1)
            .afterRecipe(run -> {
                assertThat(run.getChangeset().getAllResults()).hasSize(4);

                // Verify wrapper files are added at root, not in subdirectories
                var mvnw = run.getChangeset().getAllResults().stream()
                  .map(Result::getAfter)
                  .filter(Objects::nonNull)
                  .filter(r -> r.getSourcePath().endsWith("mvnw"))
                  .findFirst();
                assertThat(mvnw).isPresent();
                assertThat(mvnw.get().getSourcePath().toString()).isEqualTo("mvnw");

                var mavenWrapperJar = run.getChangeset().getAllResults().stream()
                  .map(Result::getAfter)
                  .filter(Objects::nonNull)
                  .filter(r -> r.getSourcePath().endsWith("maven-wrapper.jar"))
                  .findFirst();
                assertThat(mavenWrapperJar).isPresent();
                assertThat(mavenWrapperJar.get().getSourcePath()).isEqualTo(Paths.get(".mvn/wrapper/maven-wrapper.jar"));

                var mvnwCmd = run.getChangeset().getAllResults().stream()
                  .map(Result::getAfter)
                  .filter(Objects::nonNull)
                  .filter(r -> r.getSourcePath().endsWith("mvnw.cmd"))
                  .findFirst();
                assertThat(mvnwCmd).isPresent();

                var properties = run.getChangeset().getAllResults().stream()
                  .map(Result::getAfter)
                  .filter(Objects::nonNull)
                  .filter(r -> r.getSourcePath().endsWith("maven-wrapper.properties"))
                  .findFirst();
                assertThat(properties).isPresent();

                // Verify subdirectories do NOT contain wrapper files
                var filesInProjectA = run.getChangeset().getAllResults().stream()
                  .map(Result::getAfter)
                  .filter(Objects::nonNull)
                  .filter(r -> r.getSourcePath().toString().startsWith("project-a/") &&
                               (r.getSourcePath().toString().contains("mvnw") ||
                                r.getSourcePath().toString().contains("maven-wrapper")))
                  .toList();
                assertThat(filesInProjectA).isEmpty();

                var filesInProjectB = run.getChangeset().getAllResults().stream()
                  .map(Result::getAfter)
                  .filter(Objects::nonNull)
                  .filter(r -> r.getSourcePath().toString().startsWith("project-b/") &&
                               (r.getSourcePath().toString().contains("mvnw") ||
                                r.getSourcePath().toString().contains("maven-wrapper")))
                  .toList();
                assertThat(filesInProjectB).isEmpty();
            }),
          dir("project-a",
            pomXml(
              """
                <project>
                  <groupId>com.example</groupId>
                  <artifactId>project-a</artifactId>
                  <version>1.0.0</version>
                </project>
                """
            )
          ),
          dir("project-b",
            pomXml(
              """
                <project>
                  <groupId>com.example</groupId>
                  <artifactId>project-b</artifactId>
                  <version>1.0.0</version>
                </project>
                """
            )
          )
        );
    }

    @Test
    void updateWrapperInSubDirectory() {
        rewriteRun(
          spec -> spec.recipe(new UpdateMavenWrapper("3.1.x", null, "3.8.x", null, null, null))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Maven, "3.8.0")))
            .afterRecipe(run -> {
                Path subdir = Path.of("subdir");
                var mvnw = result(run, PlainText.class, "mvnw");
                assertThat(mvnw.getSourcePath()).isEqualTo(subdir.resolve(WRAPPER_SCRIPT_LOCATION));
                var mavenWrapperJar = result(run, Remote.class, "maven-wrapper.jar");
                assertThat(mavenWrapperJar.getSourcePath()).isEqualTo(subdir.resolve(WRAPPER_JAR_LOCATION));
            }),
          dir("subdir",
            properties(
              withLicenseHeader("""
                distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.0/apache-maven-3.8.0-bin.zip
                wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.0/maven-wrapper-3.1.0.jar
                """),
              withLicenseHeader("""
                distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.9/apache-maven-3.8.9-bin.zip
                wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar
                distributionSha256Sum=e50133ba6d4333bea8f8bae137c13198c8c90ded959466e13252b820b52cb68b
                """),
              spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
            ),
            mvnw,
            mvnWrapperJarQuark
          )
        );
    }

    @Test
    void updateVersionUsingSourceDistribution() {
        rewriteRun(
          spec -> spec.recipe(new UpdateMavenWrapper("3.1.x", "source", "3.8.x", null, null, Boolean.TRUE))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Maven, "3.8.0")))
            .afterRecipe(run -> {
                var mvnw = result(run, PlainText.class, "mvnw");
                assertThat(mvnw.getText()).isNotBlank();

                var mvnwCmd = result(run, PlainText.class, "mvnw.cmd");
                assertThat(mvnwCmd.getText()).isNotBlank();

                assertThatThrownBy(() -> result(run, SourceFile.class, "maven-wrapper.jar"))
                  .isInstanceOf(NoSuchElementException.class)
                  .hasMessage("No value present");

                var mvnwDownloaderJava = result(run, RemoteArchive.class, "MavenWrapperDownloader.java");
                assertThat(mvnwDownloaderJava.getSourcePath()).isEqualTo(WRAPPER_DOWNLOADER_LOCATION);
                assertThat(mvnwDownloaderJava.getUri().toString()).endsWith("/org/apache/maven/wrapper/maven-wrapper-distribution/3.1.1/maven-wrapper-distribution-3.1.1-source.zip");
            }),
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
              </project>
              """,
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
              </project>
              """,
            spec -> spec.afterRecipe(pom ->
              assertThat(pom.getMarkers().findFirst(BuildTool.class)).hasValueSatisfying(buildTool -> {
                  assertThat(buildTool.getType()).isEqualTo(BuildTool.Type.Maven);
                  assertThat(buildTool.getVersion()).isEqualTo("3.8.9");
              }))
          ),
          properties(
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.0/apache-maven-3.8.0-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.0/maven-wrapper-3.1.0.jar
              """),
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.9/apache-maven-3.8.9-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar
              distributionSha256Sum=e50133ba6d4333bea8f8bae137c13198c8c90ded959466e13252b820b52cb68b
              wrapperSha256Sum=ff7f21f2ef81723377e3d42d06661c4e3af60cf4bdfb7579ac8f22051399942d
              """),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
          ),
          mvnw,
          mvnwCmd,
          other(
            "",
            null,
            spec -> spec.path(".mvn/wrapper/maven-wrapper.jar")
          )
        );
    }

    @Test
    void updateVersionUsingScriptDistribution() {
        rewriteRun(
          spec -> spec.recipe(new UpdateMavenWrapper("3.1.x", "script", "3.8.x", null, null, Boolean.TRUE))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Maven, "3.8.0")))
            .afterRecipe(run -> {
                var mvnw = result(run, PlainText.class, "mvnw");
                assertThat(mvnw.getText()).isNotBlank();

                var mvnwCmd = result(run, PlainText.class, "mvnw.cmd");
                assertThat(mvnwCmd.getText()).isNotBlank();

                assertThatThrownBy(() -> result(run, SourceFile.class, "maven-wrapper.jar"))
                  .isInstanceOf(NoSuchElementException.class)
                  .hasMessage("No value present");

                assertThatThrownBy(() -> result(run, Remote.class, "MavenWrapperDownloader.java"))
                  .isInstanceOf(NoSuchElementException.class)
                  .hasMessage("No value present");
            }),
          properties(
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.0/apache-maven-3.8.0-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.0/maven-wrapper-3.1.0.jar
              """),
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.9/apache-maven-3.8.9-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar
              distributionSha256Sum=e50133ba6d4333bea8f8bae137c13198c8c90ded959466e13252b820b52cb68b
              wrapperSha256Sum=ff7f21f2ef81723377e3d42d06661c4e3af60cf4bdfb7579ac8f22051399942d
              """),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
          ),
          mvnw,
          mvnwCmd,
          other(
            "",
            null,
            spec -> spec.path(".mvn/wrapper/maven-wrapper.jar")
          )
        );
    }

    @Test
    void updateVersionUsingOnlyScriptDistribution() {
        rewriteRun(
          spec -> spec.recipe(new UpdateMavenWrapper(null, "only-script", "3.8.x", null, null, Boolean.TRUE))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Maven, "3.8.0")))
            .afterRecipe(run -> {
                var mvnw = result(run, PlainText.class, "mvnw");
                assertThat(mvnw.getText()).isNotBlank();

                var mvnwCmd = result(run, PlainText.class, "mvnw.cmd");
                assertThat(mvnwCmd.getText()).isNotBlank();

                assertThatThrownBy(() -> result(run, Remote.class, "maven-wrapper.jar"))
                  .isInstanceOf(NoSuchElementException.class)
                  .hasMessage("No value present");

                assertThatThrownBy(() -> result(run, Remote.class, "MavenWrapperDownloader.java"))
                  .isInstanceOf(NoSuchElementException.class)
                  .hasMessage("No value present");
            }),
          properties(
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.0/apache-maven-3.8.0-bin.zip
              """),
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.9/apache-maven-3.8.9-bin.zip
              distributionSha256Sum=e50133ba6d4333bea8f8bae137c13198c8c90ded959466e13252b820b52cb68b
              """),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
          ),
          mvnw,
          mvnwCmd,
          other(
            "",
            null,
            spec -> spec.path(".mvn/wrapper/maven-wrapper.jar")
          )
        );
    }

    @Test
    void dontAddMissingWrapper() {
        rewriteRun(
          spec -> spec.recipe(new UpdateMavenWrapper("3.1.x", null, "3.8.x", null, Boolean.FALSE, Boolean.TRUE))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Maven, "3.8.0")))
            .afterRecipe(run -> assertThat(run.getChangeset().getAllResults()).isEmpty())
        );
    }

    @Test
    void updateMultipleWrappers() {
        rewriteRun(
          spec -> spec.recipe(new UpdateMavenWrapper("3.1.x", null, "3.8.x", null, Boolean.FALSE, Boolean.TRUE))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Maven, "3.8.0"))),
          dir("example1",
            properties(
              withLicenseHeader("""
                distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.0/apache-maven-3.8.0-bin.zip
                wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.0/maven-wrapper-3.1.0.jar
                """),
              withLicenseHeader("""
                distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.9/apache-maven-3.8.9-bin.zip
                wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar
                distributionSha256Sum=e50133ba6d4333bea8f8bae137c13198c8c90ded959466e13252b820b52cb68b
                wrapperSha256Sum=ff7f21f2ef81723377e3d42d06661c4e3af60cf4bdfb7579ac8f22051399942d
                """),
              spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
            ),
            // Each `dir(...)` prefixes the `SourceSpec` instances it is given in place, so these cannot be shared
            text("", spec -> spec.path(WRAPPER_SCRIPT_LOCATION).after(notEmpty)),
            text("", spec -> spec.path(WRAPPER_BATCH_LOCATION).after(notEmpty)),
            other("", spec -> spec.path(WRAPPER_JAR_LOCATION).after(notEmpty))
          ),
          dir("example2",
            properties(
              withLicenseHeader("""
                distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.0/apache-maven-3.8.0-bin.zip
                wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.0/maven-wrapper-3.1.0.jar
                """),
              withLicenseHeader("""
                distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.9/apache-maven-3.8.9-bin.zip
                wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar
                distributionSha256Sum=e50133ba6d4333bea8f8bae137c13198c8c90ded959466e13252b820b52cb68b
                wrapperSha256Sum=ff7f21f2ef81723377e3d42d06661c4e3af60cf4bdfb7579ac8f22051399942d
                """),
              spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
            ),
            text("", spec -> spec.path(WRAPPER_SCRIPT_LOCATION).after(notEmpty)),
            text("", spec -> spec.path(WRAPPER_BATCH_LOCATION).after(notEmpty)),
            other("", spec -> spec.path(WRAPPER_JAR_LOCATION).after(notEmpty))
          )
        );
    }

    @Test
    void doNotDowngrade() {
        rewriteRun(
          spec -> spec.allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Maven, "3.9.0"))),
          properties(
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.0/apache-maven-3.9.0-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
              """),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
          ),
          text("", spec -> spec.path("mvnw")),
          text("", spec -> spec.path("mvnw.cmd")),
          other("", spec -> spec.path(".mvn/wrapper/maven-wrapper.jar"))
        );
    }

    @Test
    void allowUpdatingDistributionTypeWhenSameVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpdateMavenWrapper("3.1.x", "script", "3.8.x", null, null, Boolean.TRUE))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Maven, "3.8.9")))
            .afterRecipe(run -> {
                var mvnw = result(run, PlainText.class, "mvnw");
                assertThat(mvnw.getText()).isNotBlank();

                var mvnwCmd = result(run, PlainText.class, "mvnw.cmd");
                assertThat(mvnwCmd.getText()).isNotBlank();

                assertThatThrownBy(() -> result(run, SourceFile.class, "maven-wrapper.jar"))
                  .isInstanceOf(NoSuchElementException.class)
                  .hasMessage("No value present");

                assertThatThrownBy(() -> result(run, SourceFile.class, "MavenWrapperDownloader.java"))
                  .isInstanceOf(NoSuchElementException.class)
                  .hasMessage("No value present");
            }),
          properties(
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.9/apache-maven-3.8.9-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar
              distributionSha256Sum=e50133ba6d4333bea8f8bae137c13198c8c90ded959466e13252b820b52cb68b
              wrapperSha256Sum=ff7f21f2ef81723377e3d42d06661c4e3af60cf4bdfb7579ac8f22051399942d
              """),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
          ),
          mvnw,
          mvnwCmd,
          other(
            "",
            null,
            spec -> spec.path(".mvn/wrapper/maven-wrapper.jar")
          ),
          other(
            "",
            null,
            spec -> spec.path(".mvn/wrapper/MavenWrapperDownloader.java")
          )
        );
    }

    @Test
    void defaultsToLatestRelease() {
        rewriteRun(
          spec -> spec.recipe(new UpdateMavenWrapper(null, null, null, null, null, Boolean.TRUE))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Maven, "3.8.0")))
            .afterRecipe(run -> {
                var mvnw = result(run, PlainText.class, "mvnw");
                assertThat(mvnw.getSourcePath()).isEqualTo(WRAPPER_SCRIPT_LOCATION);
                assertThat(mvnw.getText()).isNotBlank();
                assertThat(mvnw.getFileAttributes()).isNotNull();
                assertThat(mvnw.getFileAttributes().isReadable()).isTrue();
                assertThat(mvnw.getFileAttributes().isWritable()).isTrue();

                var mvnwCmd = result(run, PlainText.class, "mvnw.cmd");
                assertThat(mvnwCmd.getSourcePath()).isEqualTo(WRAPPER_BATCH_LOCATION);
                assertThat(mvnwCmd.getText()).isNotBlank();

                var mavenWrapperJar = result(run, RemoteFile.class, "maven-wrapper.jar");
                assertThat(mavenWrapperJar.getSourcePath()).isEqualTo(WRAPPER_JAR_LOCATION);
                Matcher wrapperVersionMatcher = Pattern.compile("maven-wrapper-(.*?)\\.jar").matcher(mavenWrapperJar.getUri().toString());
                assertThat(wrapperVersionMatcher.find()).isTrue();
                String wrapperVersion = wrapperVersionMatcher.group(1);
                assertThat(wrapperVersion).isNotEqualTo("3.1.1");
                assertThat(mavenWrapperJar.getUri().toString()).endsWith("/org/apache/maven/wrapper/maven-wrapper/" + wrapperVersion + "/maven-wrapper-" + wrapperVersion + ".jar");
                assertThat(isValidWrapperJar(mavenWrapperJar)).as("Wrapper jar is not valid").isTrue();
            }),
          properties(
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.0/apache-maven-3.8.0-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar
              distributionSha256Sum=e50133ba6d4333bea8f8bae137c13198c8c90ded959466e13252b820b52cb68b
              wrapperSha256Sum=ff7f21f2ef81723377e3d42d06661c4e3af60cf4bdfb7579ac8f22051399942d
              """),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
              .after(after -> {
                  Matcher distributionVersionMatcher = Pattern.compile("apache-maven-(.*?)-bin\\.zip").matcher(after);
                  assertThat(distributionVersionMatcher.find()).isTrue();
                  String mavenDistributionVersion = distributionVersionMatcher.group(1);
                  assertThat(mavenDistributionVersion).isNotEqualTo("3.8.0");

                  Matcher distributionChecksumMatcher = Pattern.compile("distributionSha256Sum=(.*)").matcher(after);
                  assertThat(distributionChecksumMatcher.find()).isTrue();
                  String distributionChecksum = distributionChecksumMatcher.group(1);
                  assertThat(distributionChecksum).isNotBlank();

                  Matcher wrapperVersionMatcher = Pattern.compile("maven-wrapper-(.*?)\\.jar").matcher(after);
                  assertThat(wrapperVersionMatcher.find()).isTrue();
                  String wrapperVersion = wrapperVersionMatcher.group(1);
                  assertThat(wrapperVersion).isNotEqualTo("3.1.1");

                  Matcher wrapperChecksumMatcher = Pattern.compile("wrapperSha256Sum=(.*)").matcher(after);
                  assertThat(wrapperChecksumMatcher.find()).isTrue();
                  String wrapperChecksum = wrapperChecksumMatcher.group(1);
                  assertThat(wrapperChecksum).isNotBlank();

                  return withLicenseHeader("""
                    distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%s/apache-maven-%s-bin.zip
                    wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/%s/maven-wrapper-%s.jar
                    distributionSha256Sum=%s
                    wrapperSha256Sum=%s
                    """.formatted(mavenDistributionVersion, mavenDistributionVersion, wrapperVersion, wrapperVersion, distributionChecksum, wrapperChecksum));
              })
          ),
          mvnw,
          mvnwCmd,
          mvnWrapperJarQuark
        );
    }

    @Test
    void skipWorkIfUpdatedEarlier() {
        rewriteRun(
          spec -> spec.recipeFromYaml(
              """
                type: specs.openrewrite.org/v1beta/recipe
                name: org.openrewrite.maven.MultipleWrapperUpdates
                displayName: Multiple wrapper updates
                description: Multiple wrapper updates.
                recipeList:
                  - org.openrewrite.maven.UpdateMavenWrapper:
                      wrapperVersion: 3.2.0
                      distributionVersion: 3.8.9
                      addIfMissing: false
                      enforceWrapperChecksumVerification: true
                  - org.openrewrite.maven.UpdateMavenWrapper:
                      wrapperVersion: 3.1.1
                      distributionVersion: 3.6.0
                      addIfMissing: false
                      enforceWrapperChecksumVerification: true
                """,
              "org.openrewrite.maven.MultipleWrapperUpdates"
            )
            .cycles(1)
            .expectedCyclesThatMakeChanges(1)
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Maven, "3.5.0")))
            .afterRecipe(run -> {
                var mvnw = result(run, PlainText.class, "mvnw");
                assertThat(mvnw.getSourcePath()).isEqualTo(WRAPPER_SCRIPT_LOCATION);
                assertThat(mvnw.getFileAttributes()).isNotNull();
                assertThat(mvnw.getFileAttributes().isReadable()).isTrue();
                assertThat(mvnw.getFileAttributes().isWritable()).isTrue();

                var mvnwCmd = result(run, PlainText.class, "mvnw.cmd");
                assertThat(mvnwCmd.getSourcePath()).isEqualTo(WRAPPER_BATCH_LOCATION);

                var mavenWrapperJar = result(run, RemoteFile.class, "maven-wrapper.jar");
                assertThat(mavenWrapperJar.getSourcePath()).isEqualTo(WRAPPER_JAR_LOCATION);
                assertThat(mavenWrapperJar.getUri().toString()).endsWith("/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar");
                assertThat(isValidWrapperJar(mavenWrapperJar)).as("Wrapper jar is not valid").isTrue();
            }),
          properties(
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.5.0/apache-maven-3.5.0-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.0/maven-wrapper-3.1.0.jar
              """),
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.9/apache-maven-3.8.9-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
              distributionSha256Sum=e50133ba6d4333bea8f8bae137c13198c8c90ded959466e13252b820b52cb68b
              wrapperSha256Sum=e63a53cfb9c4d291ebe3c2b0edacb7622bbc480326beaa5a0456e412f52f066a
              """),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
              .afterRecipe(mavenWrapperProperties ->
                assertThat(mavenWrapperProperties.getMarkers().findFirst(BuildTool.class)).hasValueSatisfying(buildTool -> {
                    assertThat(buildTool.getType()).isEqualTo(BuildTool.Type.Maven);
                    assertThat(buildTool.getVersion()).isEqualTo("3.8.9");
                }))
          ),
          mvnw,
          mvnwCmd,
          mvnWrapperJarQuark
        );
    }

    @Test
    void updateWrapperWithoutBuildToolMarker() {
        rewriteRun(
          spec -> spec.recipe(new UpdateMavenWrapper("3.1.x", null, "3.8.x", null, null, null))
            .afterRecipe(run -> {
                var mvnw = result(run, PlainText.class, "mvnw");
                assertThat(mvnw.getText()).isEqualTo(MVNW_TEXT);

                var mvnwCmd = result(run, PlainText.class, "mvnw.cmd");
                assertThat(mvnwCmd.getText()).isEqualTo(MVNW_CMD_TEXT);

                var mavenWrapperJar = result(run, RemoteFile.class, "maven-wrapper.jar");
                assertThat(mavenWrapperJar.getUri().toString()).endsWith("/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar");
                assertThat(isValidWrapperJar(mavenWrapperJar)).as("Wrapper jar is not valid").isTrue();
            }),
          properties(
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.0/apache-maven-3.8.0-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.0/maven-wrapper-3.1.0.jar
              """),
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.9/apache-maven-3.8.9-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar
              distributionSha256Sum=e50133ba6d4333bea8f8bae137c13198c8c90ded959466e13252b820b52cb68b
              """),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
              .afterRecipe(mavenWrapperProperties ->
                assertThat(mavenWrapperProperties.getMarkers().findFirst(BuildTool.class)).isEmpty())
          ),
          mvnw,
          mvnwCmd,
          mvnWrapperJarQuark
        );
    }

    @Test
    void updateWrapperWithModerneCliBuildToolMarker() {
        rewriteRun(
          spec -> spec.recipe(new UpdateMavenWrapper("3.1.x", null, "3.8.x", null, null, null))
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.ModerneCli, "3.44.0")))
            .afterRecipe(run -> {
                var mvnw = result(run, PlainText.class, "mvnw");
                assertThat(mvnw.getText()).isEqualTo(MVNW_TEXT);

                var mvnwCmd = result(run, PlainText.class, "mvnw.cmd");
                assertThat(mvnwCmd.getText()).isEqualTo(MVNW_CMD_TEXT);

                var mavenWrapperJar = result(run, RemoteFile.class, "maven-wrapper.jar");
                assertThat(mavenWrapperJar.getUri().toString()).endsWith("/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar");
                assertThat(isValidWrapperJar(mavenWrapperJar)).as("Wrapper jar is not valid").isTrue();
            }),
          properties(
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.0/apache-maven-3.8.0-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.0/maven-wrapper-3.1.0.jar
              """),
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.9/apache-maven-3.8.9-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar
              distributionSha256Sum=e50133ba6d4333bea8f8bae137c13198c8c90ded959466e13252b820b52cb68b
              """),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
              .afterRecipe(mavenWrapperProperties ->
                assertThat(mavenWrapperProperties.getMarkers().findFirst(BuildTool.class)).hasValueSatisfying(buildTool ->
                  assertThat(buildTool.getType()).isEqualTo(BuildTool.Type.ModerneCli)))
          ),
          mvnw,
          mvnwCmd,
          mvnWrapperJarQuark
        );
    }

    @Test
    void updateWrapperInSubDirectoryWithoutBuildToolMarker() {
        rewriteRun(
          spec -> spec.recipe(new UpdateMavenWrapper("3.1.x", null, "3.8.x", null, null, null))
            .afterRecipe(run -> {
                Path subdir = Path.of("subdir");
                var mvnw = result(run, PlainText.class, "mvnw");
                assertThat(mvnw.getSourcePath()).isEqualTo(subdir.resolve(WRAPPER_SCRIPT_LOCATION));
                var mavenWrapperJar = result(run, Remote.class, "maven-wrapper.jar");
                assertThat(mavenWrapperJar.getSourcePath()).isEqualTo(subdir.resolve(WRAPPER_JAR_LOCATION));
            }),
          dir("subdir",
            properties(
              withLicenseHeader("""
                distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.0/apache-maven-3.8.0-bin.zip
                wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.0/maven-wrapper-3.1.0.jar
                """),
              withLicenseHeader("""
                distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.9/apache-maven-3.8.9-bin.zip
                wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar
                distributionSha256Sum=e50133ba6d4333bea8f8bae137c13198c8c90ded959466e13252b820b52cb68b
                """),
              spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
            ),
            mvnw,
            mvnWrapperJarQuark
          )
        );
    }

    @Test
    void doNotDowngradeWithoutBuildToolMarker() {
        rewriteRun(
          properties(
            withLicenseHeader("""
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.0/apache-maven-3.9.0-bin.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
              """),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
          ),
          text("", spec -> spec.path("mvnw")),
          text("", spec -> spec.path("mvnw.cmd")),
          other("", spec -> spec.path(".mvn/wrapper/maven-wrapper.jar"))
        );
    }

    @Test
    void distributionUrlWithoutExtractableVersionAndNoMarkerMakesNoChanges() {
        rewriteRun(
          spec -> spec.recipe(new UpdateMavenWrapper("3.1.x", null, "3.8.x", null, false, null)),
          properties(
            withLicenseHeader("""
              distributionUrl=https://company.example/repo/maven-distribution.zip
              wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.0/maven-wrapper-3.1.0.jar
              """),
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
          ),
          text("", spec -> spec.path("mvnw")),
          text("", spec -> spec.path("mvnw.cmd")),
          other("", spec -> spec.path(".mvn/wrapper/maven-wrapper.jar"))
        );
    }

    @Test
    void downloadsThroughAnonymousMirrorButWritesCanonicalUrls(@TempDir Path remoteCache) throws IOException {
        try (Mirror mirror = new Mirror("9.1.1", "9.2.1", "bin", request -> true)) {
            rewriteRun(
              spec -> spec.recipe(new UpdateMavenWrapper(null, null, null, null, null, Boolean.TRUE))
                .executionContext(mirror.context(remoteCache, ""))
                .afterRecipe(run -> {
                    assertThat(result(run, RemoteFile.class, "maven-wrapper.jar").getUri())
                      .isEqualTo(mirror.server.url(mirror.wrapperJarPath()).uri());
                    assertThat(mirror.sent).allSatisfy(request -> assertThat(request.getUrl().getPort()).isEqualTo(mirror.port()));
                    assertThat(mirror.served).contains(mirror.wrapperJarPath(), mirror.wrapperDistributionPath(), mirror.distributionPath());
                }),
              mavenProject(),
              addedWrapperProperties(mirror),
              text(doesNotExist(), "mirrored mvnw", spec -> spec.path(WRAPPER_SCRIPT_LOCATION)),
              text(doesNotExist(), "mirrored mvnw.cmd", spec -> spec.path(WRAPPER_BATCH_LOCATION)),
              other(doesNotExist(), mirror.wrapperJarText(), spec -> spec.path(WRAPPER_JAR_LOCATION))
            );
        }
    }

    @Test
    void downloadsThroughMirrorWithBasicAuthentication(@TempDir Path remoteCache) throws IOException {
        try (Mirror mirror = new Mirror("9.1.2", "9.2.2", "bin", Mirror.BASIC_AUTHENTICATION)) {
            rewriteRun(
              spec -> spec.recipe(new UpdateMavenWrapper(null, null, null, null, null, Boolean.TRUE))
                .executionContext(mirror.context(remoteCache, Mirror.CREDENTIALS))
                .afterRecipe(run -> {
                    assertThat(result(run, RemoteFile.class, "maven-wrapper.jar").getUri())
                      .isEqualTo(mirror.server.url(mirror.wrapperJarPath()).uri());
                    assertThat(mirror.sent).allSatisfy(request -> assertThat(request.getUrl().getPort()).isEqualTo(mirror.port()));
                    assertThat(mirror.served).contains(mirror.wrapperJarPath(), mirror.wrapperDistributionPath(), mirror.distributionPath());
                }),
              mavenProject(),
              addedWrapperProperties(mirror),
              text(doesNotExist(), "mirrored mvnw", spec -> spec.path(WRAPPER_SCRIPT_LOCATION)),
              text(doesNotExist(), "mirrored mvnw.cmd", spec -> spec.path(WRAPPER_BATCH_LOCATION)),
              other(doesNotExist(), mirror.wrapperJarText(), spec -> spec.path(WRAPPER_JAR_LOCATION))
            );
        }
    }

    @Test
    void downloadsThroughMirrorWithHttpHeaderToken(@TempDir Path remoteCache) throws IOException {
        try (Mirror mirror = new Mirror("9.1.3", "9.2.3", "bin", Mirror.TOKEN_AUTHENTICATION)) {
            rewriteRun(
              spec -> spec.recipe(new UpdateMavenWrapper(null, null, null, null, null, Boolean.TRUE))
                .executionContext(mirror.context(remoteCache, Mirror.TOKEN))
                .afterRecipe(run -> assertThat(mirror.served)
                  .contains(mirror.wrapperJarPath(), mirror.wrapperDistributionPath(), mirror.distributionPath())),
              mavenProject(),
              addedWrapperProperties(mirror),
              text(doesNotExist(), "mirrored mvnw", spec -> spec.path(WRAPPER_SCRIPT_LOCATION)),
              text(doesNotExist(), "mirrored mvnw.cmd", spec -> spec.path(WRAPPER_BATCH_LOCATION)),
              other(doesNotExist(), mirror.wrapperJarText(), spec -> spec.path(WRAPPER_JAR_LOCATION))
            );
        }
    }

    @Test
    void addsWrapperDownloaderSourceThroughAuthenticatedMirror(@TempDir Path remoteCache) throws IOException {
        try (Mirror mirror = new Mirror("9.1.4", "9.2.4", "source", Mirror.BASIC_AUTHENTICATION)) {
            rewriteRun(
              spec -> spec.recipe(new UpdateMavenWrapper(null, "source", null, null, null, null))
                .executionContext(mirror.context(remoteCache, Mirror.CREDENTIALS)),
              mavenProject(),
              properties(
                doesNotExist(),
                withLicenseHeader("""
                  distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/9.2.4/apache-maven-9.2.4-bin.zip
                  distributionSha256Sum=%s
                  wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/9.1.4/maven-wrapper-9.1.4.jar
                  """.formatted(sha256(mirror.distribution))),
                spec -> spec.path(WRAPPER_PROPERTIES_LOCATION)
              ),
              text(doesNotExist(), "mirrored mvnw", spec -> spec.path(WRAPPER_SCRIPT_LOCATION)),
              text(doesNotExist(), "mirrored mvnw.cmd", spec -> spec.path(WRAPPER_BATCH_LOCATION)),
              other(doesNotExist(), "mirrored MavenWrapperDownloader.java", spec -> spec.path(WRAPPER_DOWNLOADER_LOCATION))
            );
        }
    }

    @ParameterizedTest
    @CsvSource({"bin,9.1.5,9.2.5", "source,9.1.15,9.2.15"})
    void remotesAreServedFromDiskCacheWhenWrittenOutWithAnotherContext(String distributionType, String wrapperVersion,
                                                                       String mavenVersion, @TempDir Path remoteCache) throws IOException {
        try (Mirror mirror = new Mirror(wrapperVersion, mavenVersion, distributionType, Mirror.BASIC_AUTHENTICATION)) {
            // given
            ExecutionContext ctx = mirror.context(remoteCache, Mirror.CREDENTIALS);
            List<Remote> remotes = new UpdateMavenWrapper(null, distributionType, null, null, null, null)
              .run(new InMemoryLargeSourceSet(List.of(mavenProject(ctx))), ctx)
              .getChangeset().getAllResults().stream()
              .map(Result::getAfter)
              .filter(Remote.class::isInstance)
              .map(Remote.class::cast)
              .toList();
            List<HttpSender.Request> sent = new CopyOnWriteArrayList<>();
            ExecutionContext writeOutContext = new InMemoryExecutionContext();
            RemoteExecutionContextView.view(writeOutContext).setArtifactCache(new LocalRemoteArtifactCache(remoteCache));
            HttpSenderExecutionContextView.view(writeOutContext).setLargeFileHttpSender(request -> {
                sent.add(request);
                throw new IllegalStateException("Unexpected request to " + request.getUrl());
            });

            // when
            List<String> written = remotes.stream().map(remote -> remote.printAll(writeOutContext)).toList();

            // then
            assertThat(written).containsExactly("bin".equals(distributionType) ?
              mirror.wrapperJarText() :
              "mirrored MavenWrapperDownloader.java");
            assertThat(sent).isEmpty();
        }
    }

    @Test
    void upToDateWrapperIsUnchangedWhenDownloadingThroughMirror(@TempDir Path remoteCache) throws IOException {
        try (Mirror mirror = new Mirror("9.1.6", "9.2.6", "bin", Mirror.BASIC_AUTHENTICATION)) {
            rewriteRun(
              spec -> spec.recipe(new UpdateMavenWrapper(null, null, null, null, Boolean.FALSE, null))
                .executionContext(mirror.context(remoteCache, Mirror.CREDENTIALS)),
              properties(
                withLicenseHeader("""
                  distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/9.2.6/apache-maven-9.2.6-bin.zip
                  distributionSha256Sum=%s
                  wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/9.1.6/maven-wrapper-9.1.6.jar
                  """.formatted(sha256(mirror.distribution))),
                spec -> spec.path(WRAPPER_PROPERTIES_LOCATION)
              ),
              text("mirrored mvnw", spec -> spec.path(WRAPPER_SCRIPT_LOCATION)),
              text("mirrored mvnw.cmd", spec -> spec.path(WRAPPER_BATCH_LOCATION))
            );
        }
    }

    @Test
    void failureToCreateWrapperIsAttemptedOncePerRun(@TempDir Path remoteCache) throws IOException {
        try (Mirror mirror = new Mirror("9.1.7", "9.2.7", "bin", request -> true)) {
            // given
            mirror.failing.add(mirror.wrapperJarPath());
            List<Throwable> errors = new CopyOnWriteArrayList<>();
            ExecutionContext ctx = mirror.context(remoteCache, "", errors::add);
            List<SourceFile> sources = List.of(
              new PropertiesParser().parse(withLicenseHeader("""
                  distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.0/apache-maven-3.8.0-bin.zip
                  wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.0/maven-wrapper-3.1.0.jar
                  """)).findFirst().orElseThrow().withSourcePath(WRAPPER_PROPERTIES_LOCATION),
              PlainText.builder().text("").sourcePath(WRAPPER_SCRIPT_LOCATION).build(),
              PlainText.builder().text("").sourcePath(WRAPPER_BATCH_LOCATION).build(),
              new Quark(Tree.randomId(), WRAPPER_JAR_LOCATION, Markers.EMPTY, null, null),
              PlainText.builder().text("readme").sourcePath(Path.of("README.md")).build(),
              PlainText.builder().text("notes").sourcePath(Path.of("NOTES.md")).build()
            );

            // when
            new UpdateMavenWrapper(null, null, null, null, null, null).run(new InMemoryLargeSourceSet(sources), ctx);

            // then
            assertThat(mirror.sent)
              .filteredOn(request -> request.getUrl().getPath().endsWith("/maven-wrapper-9.1.7.jar"))
              .hasSize(1);
            assertThat(errors).isNotEmpty();
        }
    }

    @Test
    void credentialsAreOnlySentToTheMirror(@TempDir Path remoteCache) throws IOException {
        try (Mirror mirror = new Mirror("9.1.8", "9.2.8", "bin", Mirror.BASIC_AUTHENTICATION)) {
            // given
            List<Throwable> errors = new CopyOnWriteArrayList<>();
            ExecutionContext ctx = mirror.context(remoteCache, Mirror.CREDENTIALS, errors::add);
            new UpdateMavenWrapper(null, null, null, null, null, null).run(new InMemoryLargeSourceSet(List.of(mavenProject(ctx))), ctx);
            assertThat(errors).isEmpty();
            assertThat(mirror.sent)
              .filteredOn(request -> request.getRequestHeaders().containsKey("Authorization"))
              .isNotEmpty()
              .allSatisfy(request -> {
                  assertThat(request.getUrl().getPort()).isEqualTo(mirror.port());
                  assertThat(request.getUrl().getPath()).startsWith("/maven2/");
              });
            mirror.sent.clear();

            // when
            for (String uri : List.of(
              "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/9.1.8/maven-wrapper-9.1.8.jar",
              mirror.server.url("/other/maven-wrapper-9.1.8.jar").toString(),
              mirror.url() + "-other/maven-wrapper-9.1.8.jar")) {
                try {
                    Remote.builder(WRAPPER_JAR_LOCATION).build(URI.create(uri)).getInputStream(ctx).close();
                } catch (RuntimeException ignored) {
                    // expected: every one of these is refused
                }
            }

            // then
            assertThat(mirror.sent).hasSize(3)
              .allSatisfy(request -> assertThat(request.getRequestHeaders()).doesNotContainKey("Authorization"));
        }
    }

    private static SourceSpecs mavenProject() {
        return pomXml(
          """
            <project>
              <groupId>com.example</groupId>
              <artifactId>demo</artifactId>
              <version>1.0.0</version>
            </project>
            """
        );
    }

    private static SourceFile mavenProject(ExecutionContext ctx) {
        return MavenParser.builder().build().parse(ctx,
          """
            <project>
              <groupId>com.example</groupId>
              <artifactId>demo</artifactId>
              <version>1.0.0</version>
            </project>
            """
        ).findFirst().orElseThrow();
    }

    private SourceSpecs addedWrapperProperties(Mirror mirror) {
        return properties(
          doesNotExist(),
          withLicenseHeader("""
            distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%2$s/apache-maven-%2$s-bin.zip
            distributionSha256Sum=%3$s
            wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/%1$s/maven-wrapper-%1$s.jar
            wrapperSha256Sum=%4$s
            """.formatted(mirror.wrapperVersion, mirror.mavenVersion, sha256(mirror.distribution), sha256(mirror.wrapperJar))),
          spec -> spec.path(WRAPPER_PROPERTIES_LOCATION)
        );
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * A Maven repository mirroring everything ({@code <mirrorOf>*</mirrorOf>}) that serves made-up wrapper and
     * distribution versions, so the static checksum memo of {@link MavenWrapper} can't interfere between tests.
     */
    private static final class Mirror implements AutoCloseable {
        static final Predicate<RecordedRequest> BASIC_AUTHENTICATION = request -> "Basic dXNlcjpzZWNyZXQ=".equals(request.getHeader("Authorization"));
        static final Predicate<RecordedRequest> TOKEN_AUTHENTICATION = request -> "abc".equals(request.getHeader("Private-Token"));

        @Language("xml")
        static final String CREDENTIALS = """
          <server>
            <id>corporate</id>
            <username>user</username>
            <password>secret</password>
          </server>
          """;

        @Language("xml")
        static final String TOKEN = """
          <server>
            <id>corporate</id>
            <configuration>
              <httpHeaders>
                <property>
                  <name>Private-Token</name>
                  <value>abc</value>
                </property>
              </httpHeaders>
            </configuration>
          </server>
          """;

        final String wrapperVersion;
        final String mavenVersion;
        final String distributionType;
        final byte[] wrapperJar;
        final byte[] distribution;
        final MockWebServer server = new MockWebServer();
        final Map<String, byte[]> files = new ConcurrentHashMap<>();
        final Set<String> failing = ConcurrentHashMap.newKeySet();
        final Set<String> served = ConcurrentHashMap.newKeySet();
        final List<HttpSender.Request> sent = new CopyOnWriteArrayList<>();

        Mirror(String wrapperVersion, String mavenVersion, String distributionType, Predicate<RecordedRequest> authorized) throws IOException {
            this.wrapperVersion = wrapperVersion;
            this.mavenVersion = mavenVersion;
            this.distributionType = distributionType;
            this.wrapperJar = ("mirrored maven-wrapper " + wrapperVersion).getBytes(StandardCharsets.UTF_8);
            this.distribution = ("mirrored apache-maven " + mavenVersion).getBytes(StandardCharsets.UTF_8);
            files.put("/maven2/org/apache/maven/wrapper/maven-wrapper-distribution/maven-metadata.xml",
              metadata("org.apache.maven.wrapper", "maven-wrapper-distribution", wrapperVersion));
            files.put("/maven2/org/apache/maven/apache-maven/maven-metadata.xml", metadata("org.apache.maven", "apache-maven", mavenVersion));
            files.put(wrapperJarPath(), wrapperJar);
            files.put(wrapperDistributionPath(), zip(Map.of(
              "mvnw", "mirrored mvnw",
              "mvnw.cmd", "mirrored mvnw.cmd",
              ".mvn/wrapper/MavenWrapperDownloader.java", "mirrored MavenWrapperDownloader.java")));
            files.put(distributionPath(), distribution);
            server.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    if (!authorized.test(request)) {
                        return new MockResponse().setResponseCode(401);
                    }
                    if (failing.contains(request.getPath())) {
                        return new MockResponse().setResponseCode(429);
                    }
                    byte[] body = files.get(request.getPath());
                    if (body == null) {
                        return new MockResponse().setResponseCode("GET".equals(request.getMethod()) ? 404 : 200);
                    }
                    served.add(request.getPath());
                    return new MockResponse().setBody(new Buffer().write(body));
                }
            });
            server.start();
        }

        String url() {
            return server.url("/maven2").toString();
        }

        int port() {
            return server.getPort();
        }

        String wrapperJarPath() {
            return "/maven2/org/apache/maven/wrapper/maven-wrapper/%1$s/maven-wrapper-%1$s.jar".formatted(wrapperVersion);
        }

        String wrapperJarText() {
            return new String(wrapperJar, StandardCharsets.ISO_8859_1);
        }

        String wrapperDistributionPath() {
            return "/maven2/org/apache/maven/wrapper/maven-wrapper-distribution/%1$s/maven-wrapper-distribution-%1$s-%2$s.zip"
              .formatted(wrapperVersion, distributionType);
        }

        String distributionPath() {
            return "/maven2/org/apache/maven/apache-maven/%1$s/apache-maven-%1$s-bin.zip".formatted(mavenVersion);
        }

        ExecutionContext context(Path remoteCache, @Language("xml") String server) {
            return context(remoteCache, server, t -> {
                throw new AssertionError(t);
            });
        }

        ExecutionContext context(Path remoteCache, @Language("xml") String server, Consumer<Throwable> onError) {
            InMemoryExecutionContext ctx = new InMemoryExecutionContext(onError);
            RemoteExecutionContextView.view(ctx).setArtifactCache(new LocalRemoteArtifactCache(remoteCache));
            // MockWebServer never answers the TLS handshake of the https probe that precedes the http fallback, so each
            // probe would wait out its read timeout and retries; a real plain HTTP server rejects the handshake at once
            HttpSender delegate = new HttpUrlConnectionSender();
            HttpSender httpSender = request -> {
                sent.add(request);
                if ("https".equals(request.getUrl().getProtocol())) {
                    throw new UncheckedIOException(new SSLException("Unsupported or unrecognized SSL message"));
                }
                return delegate.send(request);
            };
            HttpSenderExecutionContextView.view(ctx).setHttpSender(httpSender).setLargeFileHttpSender(httpSender);
            MavenExecutionContextView.view(ctx).setMavenSettings(MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
              //language=xml
              """
                <settings>
                  <mirrors>
                    <mirror>
                      <id>corporate</id>
                      <url>%s</url>
                      <mirrorOf>*</mirrorOf>
                    </mirror>
                  </mirrors>
                  <servers>
                    %s
                  </servers>
                </settings>
                """.formatted(url(), server)
            ), ctx));
            return ctx;
        }

        @Override
        public void close() throws IOException {
            server.close();
        }

        private static byte[] metadata(String groupId, String artifactId, String version) {
            //language=xml
            return """
              <metadata>
                <groupId>%s</groupId>
                <artifactId>%s</artifactId>
                <versioning>
                  <latest>%s</latest>
                  <release>%s</release>
                  <versions>
                    <version>%s</version>
                  </versions>
                </versioning>
              </metadata>
              """.formatted(groupId, artifactId, version, version, version).getBytes(StandardCharsets.UTF_8);
        }

        private static byte[] zip(Map<String, String> entries) throws IOException {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                for (Map.Entry<String, String> entry : entries.entrySet()) {
                    zip.putNextEntry(new ZipEntry(entry.getKey()));
                    zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                    zip.closeEntry();
                }
            }
            return bytes.toByteArray();
        }
    }

    private String withLicenseHeader(@Language("properties") String original) {
        return MavenWrapper.ASF_LICENSE_HEADER + original;
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
            Path testWrapperJar = Files.createTempFile("maven-wrapper", "jar");
            ExecutionContext ctx = new InMemoryExecutionContext();
            HttpSenderExecutionContextView.view(ctx).setHttpSender(new HttpUrlConnectionSender(Duration.ofSeconds(5), Duration.ofSeconds(5)));
            try (InputStream is = gradleWrapperJar.getInputStream(ctx)) {
                Files.copy(is, testWrapperJar, StandardCopyOption.REPLACE_EXISTING);
                try (FileSystem fs = FileSystems.newFileSystem(testWrapperJar)) {
                    return Files.exists(fs.getPath("org/apache/maven/wrapper/MavenWrapperMain.class"));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
