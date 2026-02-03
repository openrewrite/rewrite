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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
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

    @Nested
    class IsMultiComponentLiteralsTest {

        private J.Literal stringLiteral(String value) {
            return new J.Literal(randomId(), Space.EMPTY, Markers.EMPTY, value, "\"" + value + "\"", null, JavaType.Primitive.String);
        }

        private J.Literal intLiteral(int value) {
            return new J.Literal(randomId(), Space.EMPTY, Markers.EMPTY, value, String.valueOf(value), null, JavaType.Primitive.Int);
        }

        @Test
        void twoStringLiterals() {
            List<Expression> args = Arrays.asList(stringLiteral("com.example"), stringLiteral("mylib"));
            assertThat(GradleDependency.isMultiComponentLiterals(args)).isTrue();
        }

        @Test
        void threeStringLiterals() {
            List<Expression> args = Arrays.asList(stringLiteral("com.example"), stringLiteral("mylib"), stringLiteral("1.0"));
            assertThat(GradleDependency.isMultiComponentLiterals(args)).isTrue();
        }

        @Test
        void fourStringLiterals() {
            List<Expression> args = Arrays.asList(stringLiteral("com.example"), stringLiteral("mylib"), stringLiteral("1.0"), stringLiteral("sources"));
            assertThat(GradleDependency.isMultiComponentLiterals(args)).isTrue();
        }

        @Test
        void singleArgIsNotMultiComponent() {
            List<Expression> args = Collections.singletonList(stringLiteral("com.example"));
            assertThat(GradleDependency.isMultiComponentLiterals(args)).isFalse();
        }

        @Test
        void fiveArgsIsNotMultiComponent() {
            List<Expression> args = Arrays.asList(
              stringLiteral("a"), stringLiteral("b"), stringLiteral("c"), stringLiteral("d"), stringLiteral("e"));
            assertThat(GradleDependency.isMultiComponentLiterals(args)).isFalse();
        }

        @Test
        void colonSeparatedIsNotMultiComponent() {
            List<Expression> args = Arrays.asList(stringLiteral("com.example:mylib:1.0"), stringLiteral("extra"));
            assertThat(GradleDependency.isMultiComponentLiterals(args)).isFalse();
        }

        @Test
        void nonLiteralArgIsNotMultiComponent() {
            J.Identifier variable = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, Collections.emptyList(), "version", null, null);
            List<Expression> args = Arrays.asList(stringLiteral("com.example"), stringLiteral("mylib"), variable);
            assertThat(GradleDependency.isMultiComponentLiterals(args)).isFalse();
        }

        @Test
        void nonStringLiteralIsNotMultiComponent() {
            List<Expression> args = Arrays.asList(stringLiteral("com.example"), intLiteral(42));
            assertThat(GradleDependency.isMultiComponentLiterals(args)).isFalse();
        }

        @Test
        void emptyListIsNotMultiComponent() {
            assertThat(GradleDependency.isMultiComponentLiterals(Collections.emptyList())).isFalse();
        }
    }

    @Nested
    class ParseMultiComponentLiteralsTest {

        private J.Literal stringLiteral(String value) {
            return new J.Literal(randomId(), Space.EMPTY, Markers.EMPTY, value, "\"" + value + "\"", null, JavaType.Primitive.String);
        }

        @Test
        void twoArgs() {
            List<Expression> args = Arrays.asList(stringLiteral("com.example"), stringLiteral("mylib"));
            Dependency dep = GradleDependency.parseMultiComponentLiterals(args);
            assertThat(dep).isNotNull();
            assertThat(dep.getGroupId()).isEqualTo("com.example");
            assertThat(dep.getArtifactId()).isEqualTo("mylib");
            assertThat(dep.getVersion()).isNull();
            assertThat(dep.getClassifier()).isNull();
        }

        @Test
        void threeArgs() {
            List<Expression> args = Arrays.asList(stringLiteral("com.example"), stringLiteral("mylib"), stringLiteral("1.0"));
            Dependency dep = GradleDependency.parseMultiComponentLiterals(args);
            assertThat(dep).isNotNull();
            assertThat(dep.getGroupId()).isEqualTo("com.example");
            assertThat(dep.getArtifactId()).isEqualTo("mylib");
            assertThat(dep.getVersion()).isEqualTo("1.0");
            assertThat(dep.getClassifier()).isNull();
        }

        @Test
        void fourArgs() {
            List<Expression> args = Arrays.asList(stringLiteral("com.example"), stringLiteral("mylib"), stringLiteral("1.0"), stringLiteral("sources"));
            Dependency dep = GradleDependency.parseMultiComponentLiterals(args);
            assertThat(dep).isNotNull();
            assertThat(dep.getGroupId()).isEqualTo("com.example");
            assertThat(dep.getArtifactId()).isEqualTo("mylib");
            assertThat(dep.getVersion()).isEqualTo("1.0");
            assertThat(dep.getClassifier()).isEqualTo("sources");
        }

        @Test
        void emptyGroupReturnsNull() {
            List<Expression> args = Arrays.asList(stringLiteral(""), stringLiteral("mylib"));
            assertThat(GradleDependency.parseMultiComponentLiterals(args)).isNull();
        }

        @Test
        void emptyArtifactReturnsNull() {
            List<Expression> args = Arrays.asList(stringLiteral("com.example"), stringLiteral(""));
            assertThat(GradleDependency.parseMultiComponentLiterals(args)).isNull();
        }
    }
}
