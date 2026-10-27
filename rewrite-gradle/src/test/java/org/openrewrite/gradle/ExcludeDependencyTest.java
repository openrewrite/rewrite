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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.SourceFile;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class ExcludeDependencyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new ExcludeDependency("commons-logging", "commons-logging", null));
    }

    @DocumentExample
    @Test
    void attachesExcludeToDeclarationThatTransitivelyIncludesTarget() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude group: 'commons-logging', module: 'commons-logging'
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(resolved(cu, "compileClasspath"))
                  .doesNotContain("commons-logging:commons-logging")
                  .contains("org.apache.httpcomponents:httpcore", "commons-codec:commons-codec");
                assertThat(resolved(cu, "runtimeClasspath")).doesNotContain("commons-logging:commons-logging");
            })
          )
        );
    }

    @Test
    void appendsExcludeToExistingClosure() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude group: 'org.apache.httpcomponents', module: 'httpcore'
                  }
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude group: 'org.apache.httpcomponents', module: 'httpcore'
                      exclude group: 'commons-logging', module: 'commons-logging'
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(resolved(cu, "compileClasspath"))
                  .as("the pre-existing httpcore exclude must survive re-resolution")
                  .doesNotContain("commons-logging:commons-logging", "org.apache.httpcomponents:httpcore")
                  .contains("commons-codec:commons-codec");
            })
          )
        );
    }

    @Test
    void idempotentWhenExcludeAlreadyPresent() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude group: 'commons-logging', module: 'commons-logging'
                  }
              }
              """
          )
        );
    }

    @Test
    void idempotentWhenGroupOnlyExcludeCoversTarget() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude group: 'commons-logging'
                  }
              }
              """
          )
        );
    }

    @Test
    void idempotentWhenModuleOnlyExcludeCoversTarget() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude module: 'commons-logging'
                  }
              }
              """
          )
        );
    }

    @Test
    void idempotentWhenTransitiveFalse() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      transitive = false
                  }
              }
              """
          )
        );
    }

    @Test
    void noOpWhenTargetModuleNotTransitivelyPresent() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'com.google.guava:guava:30.1.1-jre'
              }
              """
          )
        );
    }

    @Test
    void noOpOnDirectDeclarationOfExcludedTarget() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'commons-logging:commons-logging:1.2'
              }
              """
          )
        );
    }

    @Test
    void attachesToOtherDeclarationsEvenWhenTargetAlsoDeclaredDirectly() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'commons-logging:commons-logging:1.2'
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'commons-logging:commons-logging:1.2'
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude group: 'commons-logging', module: 'commons-logging'
                  }
              }
              """
          )
        );
    }

    @Test
    void configurationFilterLimitsWhichDeclarationsAreTouched() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeDependency("commons-logging", "commons-logging", "api")),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  api 'org.apache.httpcomponents:httpclient:4.5.13'
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  api('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude group: 'commons-logging', module: 'commons-logging'
                  }
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(requestedExclusions(cu, "api", "httpclient"))
                  .contains(new GroupArtifact("commons-logging", "commons-logging"));
                assertThat(resolved(cu, "compileClasspath"))
                  .as("the implementation declaration still brings commons-logging in")
                  .contains("commons-logging:commons-logging");
            })
          )
        );
    }

    @Test
    void noOpInBuildscriptClasspath() {
        rewriteRun(
          buildGradle(
            """
              buildscript {
                  repositories {
                      mavenCentral()
                  }
                  dependencies {
                      classpath 'org.apache.httpcomponents:httpclient:4.5.13'
                  }
              }

              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }
              """
          )
        );
    }

    @Test
    void noOpInSubprojectsAndAllprojects() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              subprojects {
                  dependencies {
                      implementation 'org.apache.httpcomponents:httpclient:4.5.13'
                  }
              }

              allprojects {
                  dependencies {
                      implementation 'org.apache.httpcomponents:httpclient:4.5.13'
                  }
              }
              """
          )
        );
    }

    @Test
    void mapNotationDeclarationGroovy() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation group: 'org.apache.httpcomponents', name: 'httpclient', version: '4.5.13'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(group: 'org.apache.httpcomponents', name: 'httpclient', version: '4.5.13') {
                      exclude group: 'commons-logging', module: 'commons-logging'
                  }
              }
              """
          )
        );
    }

    @Test
    void matchesTwoSpaceIndentation() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                id 'java-library'
              }

              repositories {
                mavenCentral()
              }

              dependencies {
                implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """,
            """
              plugins {
                id 'java-library'
              }

              repositories {
                mavenCentral()
              }

              dependencies {
                implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                  exclude group: 'commons-logging', module: 'commons-logging'
                }
              }
              """
          )
        );
    }

    @Test
    void matchesTabIndentation() {
        rewriteRun(
          buildGradle(
            """
              plugins {
              \tid 'java-library'
              }

              repositories {
              \tmavenCentral()
              }

              dependencies {
              \timplementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """,
            """
              plugins {
              \tid 'java-library'
              }

              repositories {
              \tmavenCentral()
              }

              dependencies {
              \timplementation('org.apache.httpcomponents:httpclient:4.5.13') {
              \t\texclude group: 'commons-logging', module: 'commons-logging'
              \t}
              }
              """
          )
        );
    }

    @Test
    void matchesIndentationOfNestedDependenciesBlock() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              if (true) {
                  dependencies {
                      implementation 'org.apache.httpcomponents:httpclient:4.5.13'
                  }
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              if (true) {
                  dependencies {
                      implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                          exclude group: 'commons-logging', module: 'commons-logging'
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void fillsEmptyExistingClosure() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {}
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude group: 'commons-logging', module: 'commons-logging'
                  }
              }
              """
          )
        );
    }

    // ------------- Kotlin DSL -------------

    @Test
    void kotlinDslAttachesExcludeToDeclaration() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13")
              }
              """,
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13") {
                      exclude(group = "commons-logging", module = "commons-logging")
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(resolved(cu, "compileClasspath"))
                  .doesNotContain("commons-logging:commons-logging")
                  .contains("org.apache.httpcomponents:httpcore", "commons-codec:commons-codec");
                assertThat(resolved(cu, "runtimeClasspath")).doesNotContain("commons-logging:commons-logging");
            })
          )
        );
    }

    @Test
    void kotlinDslMatchesTwoSpaceIndentation() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                `java-library`
              }

              repositories {
                mavenCentral()
              }

              dependencies {
                implementation("org.apache.httpcomponents:httpclient:4.5.13")
              }
              """,
            """
              plugins {
                `java-library`
              }

              repositories {
                mavenCentral()
              }

              dependencies {
                implementation("org.apache.httpcomponents:httpclient:4.5.13") {
                  exclude(group = "commons-logging", module = "commons-logging")
                }
              }
              """
          )
        );
    }

    @Test
    void kotlinDslAppendsExcludeToExistingClosure() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13") {
                      exclude(group = "org.apache.httpcomponents", module = "httpcore")
                  }
              }
              """,
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13") {
                      exclude(group = "org.apache.httpcomponents", module = "httpcore")
                      exclude(group = "commons-logging", module = "commons-logging")
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinDslIdempotentWhenExcludeAlreadyPresent() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13") {
                      exclude(group = "commons-logging", module = "commons-logging")
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinDslIdempotentWhenIsTransitiveFalse() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13") {
                      isTransitive = false
                  }
              }
              """
          )
        );
    }

    @Test
    void groupOnlyInputEmitsGroupOnlyExclude() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeDependency("commons-logging", null, null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude group: 'commons-logging'
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(resolved(cu, "compileClasspath")).doesNotContain("commons-logging:commons-logging");
            })
          )
        );
    }

    @Test
    void moduleOnlyInputEmitsModuleOnlyExclude() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeDependency("*", "commons-logging", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude module: 'commons-logging'
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(resolved(cu, "compileClasspath")).doesNotContain("commons-logging:commons-logging");
            })
          )
        );
    }

    @Test
    void kotlinDslGroupOnlyInputEmitsGroupOnlyExclude() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeDependency("commons-logging", null, null)),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13")
              }
              """,
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13") {
                      exclude(group = "commons-logging")
                  }
              }
              """
          )
        );
    }

    @Test
    void secondExcludeSeesTransitivesAfterFirstChangesModel() {
        rewriteRun(
          spec -> spec.recipes(
            new ExcludeDependency("commons-logging", "commons-logging", null),
            new ExcludeDependency("commons-codec", "commons-codec", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude group: 'commons-logging', module: 'commons-logging'
                      exclude group: 'commons-codec', module: 'commons-codec'
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(resolved(cu, "compileClasspath"))
              .doesNotContain("commons-logging:commons-logging", "commons-codec:commons-codec")
              .contains("org.apache.httpcomponents:httpcore"))
          )
        );
    }

    @Test
    void validationFailsWhenBothCoordinatesAreOmitted() {
        assertThat(new ExcludeDependency(null, null, null).validate().isValid()).isFalse();
        assertThat(new ExcludeDependency("*", "*", null).validate().isValid()).isFalse();
        assertThat(new ExcludeDependency("", "", null).validate().isValid()).isFalse();
    }

    @Test
    void validationFailsOnPartialWildcards() {
        assertThat(new ExcludeDependency("commons-*", "commons-logging", null).validate().isValid()).isFalse();
        assertThat(new ExcludeDependency("commons-logging", "commons-*", null).validate().isValid()).isFalse();
        assertThat(new ExcludeDependency("*", "commons-logging", null).validate().isValid()).isTrue();
        assertThat(new ExcludeDependency("commons-logging", "*", null).validate().isValid()).isTrue();
    }

    @Test
    void validationFailsOnCharactersOutsideCoordinateSyntax() {
        assertThat(new ExcludeDependency("commons-logging'", "commons-logging", null).validate().isValid()).isFalse();
        assertThat(new ExcludeDependency("commons-logging", "commons\"logging", null).validate().isValid()).isFalse();
        assertThat(new ExcludeDependency("${group}", "commons-logging", null).validate().isValid()).isFalse();
        assertThat(new ExcludeDependency("org.apache_commons-1", "commons.logging", null).validate().isValid()).isTrue();
    }

    @Test
    void kotlinDslConfigurationFilter() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeDependency("commons-logging", "commons-logging", "api")),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  api("org.apache.httpcomponents:httpclient:4.5.13")
                  implementation("org.apache.httpcomponents:httpclient:4.5.13")
              }
              """,
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  api("org.apache.httpcomponents:httpclient:4.5.13") {
                      exclude(group = "commons-logging", module = "commons-logging")
                  }
                  implementation("org.apache.httpcomponents:httpclient:4.5.13")
              }
              """
          )
        );
    }

    private static List<String> resolved(SourceFile cu, String configuration) {
        GradleDependencyConfiguration conf = cu.getMarkers().findFirst(GradleProject.class)
          .orElseThrow(IllegalStateException::new)
          .getConfiguration(configuration);
        assertThat(conf).isNotNull();
        return conf.getResolved().stream()
          .map(d -> d.getGroupId() + ":" + d.getArtifactId())
          .collect(toList());
    }

    private static List<GroupArtifact> requestedExclusions(SourceFile cu, String configuration, String artifactId) {
        GradleDependencyConfiguration conf = cu.getMarkers().findFirst(GradleProject.class)
          .orElseThrow(IllegalStateException::new)
          .getConfiguration(configuration);
        assertThat(conf).isNotNull();
        return conf.getRequested().stream()
          .filter(d -> artifactId.equals(d.getArtifactId()))
          .flatMap(d -> d.getExclusions().stream())
          .collect(toList());
    }
}
