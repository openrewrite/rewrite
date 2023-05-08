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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.remote.Remote;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.text.PlainText;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.*;
import java.time.Duration;
import java.util.Objects;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.util.GradleWrapper.*;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.test.SourceSpecs.other;
import static org.openrewrite.test.SourceSpecs.text;

@SuppressWarnings("UnusedProperty")
class UpdateGradleWrapperTest implements RewriteTest {
    private final UnaryOperator<@Nullable String> notEmpty = actual -> {
        assertThat(actual).isNotNull();
        return actual + "\n";
    };

    private final SourceSpecs gradlew = text("", spec -> spec.path(WRAPPER_SCRIPT_LOCATION).after(notEmpty));
    private final SourceSpecs gradlewBat = text("", spec -> spec.path(WRAPPER_BATCH_LOCATION).after(notEmpty));
    private final SourceSpecs gradleWrapperJarQuark = other("", spec -> spec.path(WRAPPER_JAR_LOCATION));

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpdateGradleWrapper("7.4.2", null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"gradle\\wrapper\\gradle-wrapper.properties", "gradle/wrapper/gradle-wrapper.properties"})
    void updateVersionAndDistribution(String osPath) {
        rewriteRun(
          spec -> spec.allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "7.4")))
            .afterRecipe(run -> {
              var gradleSh = result(run, PlainText.class, "gradlew");
              assertThat(gradleSh.getText()).isNotBlank();

              var gradleBat = result(run, PlainText.class, "gradlew.bat");
              assertThat(gradleBat.getText()).isNotBlank();

              var gradleWrapperJar = result(run, Remote.class, "gradle-wrapper.jar");
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
            spec -> spec.path(osPath)
          ),
          gradlew,
          gradlewBat,
          gradleWrapperJarQuark
        );
    }

    @DocumentExample
    @Test
    void updateChecksumAlreadySet() {
        rewriteRun(
          spec -> spec.allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "7.4")))
            .afterRecipe(run -> {
              var gradleWrapperJar = result(run, Remote.class, "gradle-wrapper.jar");
              assertThat(PathUtils.equalIgnoringSeparators(gradleWrapperJar.getSourcePath(), WRAPPER_JAR_LOCATION)).isTrue();
              assertThat(gradleWrapperJar.getUri()).isEqualTo(URI.create("https://services.gradle.org/distributions/gradle-7.4.2-bin.zip"));
              assertThat(isValidWrapperJar(gradleWrapperJar)).as("Wrapper jar is not valid").isTrue();
              var gradleWrapperProperties = result(run, Properties.File.class, "gradle-wrapper.properties");
              assertThat(gradleWrapperProperties.getMarkers().findFirst(BuildTool.class)).hasValueSatisfying(buildTool -> {
                  assertThat(buildTool.getType()).isEqualTo(BuildTool.Type.Gradle);
                  assertThat(buildTool.getVersion()).isEqualTo("7.4.2");
              });
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
          )
        );
    }

    private <S extends SourceFile> S result(RecipeRun run, Class<S> clazz, String endsWith) {
        return run.getResults().stream()
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
            try (InputStream is = gradleWrapperJar.getInputStream(new HttpUrlConnectionSender(Duration.ofSeconds(5), Duration.ofSeconds(5)))) {
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
