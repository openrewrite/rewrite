/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.gradle.trait;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.gradle.ExcludeDependency;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class GradleDependencyTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(RewriteTest.toRecipe(() -> new GradleDependency.Matcher().asVisitor(dep ->
            SearchResult.found(dep.getTree(), dep.getResolvedDependency().getGav().toString()))));
    }

    @DocumentExample
    @Test
    void literal() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation "com.google.guava:guava:28.2-jre"
              }
              """,
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  /*~~(com.google.guava:guava:28.2-jre)~~>*/implementation "com.google.guava:guava:28.2-jre"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      //"api",
      "implementation",
      "compileOnly",
      "runtimeOnly",
      "testImplementation",
      "testCompileOnly",
      "testRuntimeOnly",
    })
    void methods(String method) {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  %s "com.google.guava:guava:28.2-jre"
              }
              """.formatted(method),
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  /*~~(com.google.guava:guava:28.2-jre)~~>*/%s "com.google.guava:guava:28.2-jre"
              }
              """.formatted(method)
          )
        );
    }

    @Disabled("Need additional plugins to test these methods")
    @ParameterizedTest
    @ValueSource(strings = {
      // Android
      "debugImplementation",
      "releaseImplementation",
      "androidTestImplementation",
      "featureImplementation",
      // Kotlin
      "annotationProcessor",
      "kapt",
      "ksp"
    })
    void methodsFromPlugins(String method) {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  %s "com.google.guava:guava:28.2-jre"
              }
              """.formatted(method),
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  /*~~(com.google.guava:guava:28.2-jre)~~>*/%s "com.google.guava:guava:28.2-jre"
              }
              """.formatted(method)
          )
        );
    }

    @Disabled("Requires at most Java 15")
    @ParameterizedTest
    @ValueSource(strings = {
      "compile", // deprecated
      "runtime", // deprecated
      "testCompile", // deprecated
      "testRuntime" // deprecated
    })
    void decprecatedMethods(String method) {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi("6.9.4")),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  %s "com.google.guava:guava:28.2-jre"
              }
              """.formatted(method),
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  /*~~(com.google.guava:guava:28.2-jre)~~>*/%s "com.google.guava:guava:28.2-jre"
              }
              """.formatted(method)
          )
        );
    }

    @Test
    void resolvedDependencyWithTransitivesAfterReResolution() {
        rewriteRun(
          spec -> spec
            .beforeRecipe(withToolingApi())
            .recipes(
              new ExcludeDependency("commons-logging", "commons-logging", null),
              // marks once, as a later cycle sees the cached transitives through getResolvedDependency() too
              RewriteTest.toRecipe(() -> new GradleDependency.Matcher().asVisitor(dep ->
                dep.getTree().getMarkers().findFirst(SearchResult.class).isPresent() ? dep.getTree() :
                SearchResult.found(dep.getTree(), String.format("shallow: %s, with transitives: %s",
                  dep.getResolvedDependency().findDependency("commons-codec", "commons-codec") != null,
                  dep.getResolvedDependencyWithTransitives().findDependency("commons-codec", "commons-codec") != null))))),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation "org.apache.httpcomponents:httpclient:4.5.13"
              }
              """,
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  /*~~(shallow: false, with transitives: true)~~>*/implementation("org.apache.httpcomponents:httpclient:4.5.13") {
                      exclude group: 'commons-logging', module: 'commons-logging'
                  }
              }
              """
          )
        );
    }

    @Test
    void groovyString() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  def version = "28.2-jre"
                  implementation "com.google.guava:guava:${version}"
              }
              """,
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  def version = "28.2-jre"
                  /*~~(com.google.guava:guava:28.2-jre)~~>*/implementation "com.google.guava:guava:${version}"
              }
              """
          )
        );
    }

    @Test
    void groovyMapEntry() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation group: "com.google.guava", name: "guava", version: "28.2-jre"
              }
              """,
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  /*~~(com.google.guava:guava:28.2-jre)~~>*/implementation group: "com.google.guava", name: "guava", version: "28.2-jre"
              }
              """
          )
        );
    }

    @Test
    void platform() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(platform("com.google.guava:guava:28.2-jre"))
              }
              """,
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  /*~~(com.google.guava:guava:28.2-jre)~~>*/implementation(platform("com.google.guava:guava:28.2-jre"))
              }
              """
          )
        );
    }

    @Test
    void enforcedPlatform() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(enforcedPlatform("com.google.guava:guava:28.2-jre"))
              }
              """,
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  /*~~(com.google.guava:guava:28.2-jre)~~>*/implementation(enforcedPlatform("com.google.guava:guava:28.2-jre"))
              }
              """
          )
        );
    }

    @Test
    void multiComponentLiterals() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("com.google.guava", "guava", "28.2-jre")
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
                  /*~~(com.google.guava:guava:28.2-jre)~~>*/implementation("com.google.guava", "guava", "28.2-jre")
              }
              """
          )
        );
    }

    @Test
    void withDeclaredGroupIdPreservesGString() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(RewriteTest.toRecipe(() -> new GradleDependency.Matcher()
              .groupId("com.google.guava").artifactId("guava")
              .asVisitor(dep -> dep.withDeclaredGroupId("org.example").getTree()))),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  def version = "28.2-jre"
                  implementation "com.google.guava:guava:${version}"
              }
              """,
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  def version = "28.2-jre"
                  implementation "org.example:guava:${version}"
              }
              """
          )
        );
    }

    @Test
    void withDeclaredArtifactIdPreservesGString() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(RewriteTest.toRecipe(() -> new GradleDependency.Matcher()
              .groupId("com.google.guava").artifactId("guava")
              .asVisitor(dep -> dep.withDeclaredArtifactId("banana").getTree()))),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  def version = "28.2-jre"
                  implementation "com.google.guava:guava:${version}"
              }
              """,
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  def version = "28.2-jre"
                  implementation "com.google.guava:banana:${version}"
              }
              """
          )
        );
    }

    @Test
    void withDeclaredVersionCollapsesGString() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(RewriteTest.toRecipe(() -> new GradleDependency.Matcher()
              .groupId("com.google.guava").artifactId("guava")
              .asVisitor(dep -> dep.withDeclaredVersion("29.0-jre").getTree()))),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  def version = "28.2-jre"
                  implementation "com.google.guava:guava:${version}"
              }
              """,
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  def version = "28.2-jre"
                  implementation "com.google.guava:guava:29.0-jre"
              }
              """
          )
        );
    }

    @Test
    void withDeclaredGroupIdPreservesKotlinStringTemplate() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(RewriteTest.toRecipe(() -> new GradleDependency.Matcher()
              .groupId("com.google.guava").artifactId("guava")
              .asVisitor(dep -> dep.withDeclaredGroupId("org.example").getTree()))),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  val version = "28.2-jre"
                  implementation("com.google.guava:guava:${version}")
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
                  val version = "28.2-jre"
                  implementation("org.example:guava:${version}")
              }
              """
          )
        );
    }

    @Test
    void withDeclaredArtifactIdPreservesKotlinStringTemplate() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(RewriteTest.toRecipe(() -> new GradleDependency.Matcher()
              .groupId("com.google.guava").artifactId("guava")
              .asVisitor(dep -> dep.withDeclaredArtifactId("banana").getTree()))),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  val version = "28.2-jre"
                  implementation("com.google.guava:guava:${version}")
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
                  val version = "28.2-jre"
                  implementation("com.google.guava:banana:${version}")
              }
              """
          )
        );
    }

    @Test
    void withDeclaredVersionCollapsesKotlinStringTemplate() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(RewriteTest.toRecipe(() -> new GradleDependency.Matcher()
              .groupId("com.google.guava").artifactId("guava")
              .asVisitor(dep -> dep.withDeclaredVersion("29.0-jre").getTree()))),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  val version = "28.2-jre"
                  implementation("com.google.guava:guava:${version}")
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
                  val version = "28.2-jre"
                  implementation("com.google.guava:guava:29.0-jre")
              }
              """
          )
        );
    }

    @Test
    void withDeclaredCoordinatesKeepTrailingClosureOfGString() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(RewriteTest.toRecipe(() -> new GradleDependency.Matcher()
              .groupId("com.google.guava").artifactId("guava")
              .asVisitor(dep -> dep.withDeclaredGroupId("org.example").withDeclaredArtifactId("banana")
                .withDeclaredVersion("29.0-jre").getTree()))),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  def version = "28.2-jre"
                  implementation("com.google.guava:guava:${version}") {
                      exclude group: "com.google.code.findbugs"
                  }
              }
              """,
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  def version = "28.2-jre"
                  implementation("org.example:banana:29.0-jre") {
                      exclude group: "com.google.code.findbugs"
                  }
              }
              """
          )
        );
    }

    @Test
    void withDeclaredCoordinatesKeepTrailingLambdaOfKotlinStringTemplate() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(RewriteTest.toRecipe(() -> new GradleDependency.Matcher()
              .groupId("com.google.guava").artifactId("guava")
              .asVisitor(dep -> dep.withDeclaredGroupId("org.example").withDeclaredArtifactId("banana")
                .withDeclaredVersion("29.0-jre").getTree()))),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  val version = "28.2-jre"
                  implementation("com.google.guava:guava:${version}") {
                      exclude(group = "com.google.code.findbugs")
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
                  val version = "28.2-jre"
                  implementation("org.example:banana:29.0-jre") {
                      exclude(group = "com.google.code.findbugs")
                  }
              }
              """
          )
        );
    }

    @Test
    void withDeclaredVersionAddsMapEntryBeforeTrailingClosure() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(RewriteTest.toRecipe(() -> new GradleDependency.Matcher()
              .groupId("com.google.guava").artifactId("guava")
              .asVisitor(dep -> dep.withDeclaredVersion("29.0-jre").getTree()))),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(group: "com.google.guava", name: "guava") {
                      exclude group: "com.google.code.findbugs"
                  }
              }
              """,
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(group: "com.google.guava", name: "guava", version: "29.0-jre") {
                      exclude group: "com.google.code.findbugs"
                  }
              }
              """
          )
        );
    }

    @Test
    void withDeclaredVersionAddsNamedArgumentBeforeTrailingLambda() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(RewriteTest.toRecipe(() -> new GradleDependency.Matcher()
              .groupId("com.google.guava").artifactId("guava")
              .asVisitor(dep -> dep.withDeclaredVersion("29.0-jre").getTree()))),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(group = "com.google.guava", name = "guava") {
                      exclude(group = "com.google.code.findbugs")
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
                  implementation(group = "com.google.guava", name = "guava", version = "29.0-jre") {
                      exclude(group = "com.google.code.findbugs")
                  }
              }
              """
          )
        );
    }

    @Test
    void removeVersionKeepsTrailingClosureOfMapEntries() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(RewriteTest.toRecipe(() -> new GradleDependency.Matcher()
              .groupId("com.google.guava").artifactId("guava")
              .asVisitor(dep -> dep.removeVersion().getTree()))),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(group: "com.google.guava", name: "guava", version: "28.2-jre") {
                      exclude group: "com.google.code.findbugs"
                  }
              }
              """,
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(group: "com.google.guava", name: "guava") {
                      exclude group: "com.google.code.findbugs"
                  }
              }
              """
          )
        );
    }

    @Test
    void multiComponentLiteralsTwoArgs() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("com.google.guava", "guava")
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
                  /*~~(com.google.guava:guava:)~~>*/implementation("com.google.guava", "guava")
              }
              """
          )
        );
    }

}
