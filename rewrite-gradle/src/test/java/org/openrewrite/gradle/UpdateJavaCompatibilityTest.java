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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

class ChangeJavaCompatibilityTest implements RewriteTest {
    @Nested
    @DisplayName("When requested version is an integer")
    class NewInteger extends BaseTest {
        NewInteger() {
            super("8");
        }
    }

    @Nested
    @DisplayName("When requested version is a double")
    class NewDouble extends BaseTest {
        NewDouble() {
            super("1.8");
        }
    }

    @Nested
    @DisplayName("When requested version is a string (double quotes)")
    class NewStringDoubleQuoted extends BaseTest {
        NewStringDoubleQuoted() {
            super("\"1.8\"");
        }
    }

    @Nested
    @DisplayName("When requested version is a string (single quotes -tmp)")
    class NewStringSingleQuoted extends BaseTest {
        NewStringSingleQuoted() {
            super("'1.8'");
        }
    }

    @Nested
    @DisplayName("When requested version is an enum (shorthand)")
    class NewEnumShorthand extends BaseTest {
        NewEnumShorthand() {
            super("VERSION_1_8");
        }
    }

    @Nested
    @DisplayName("When requested version is an enum")
    class NewEnum extends BaseTest {
        NewEnum() {
            super("JavaVersion.VERSION_1_8");
        }
    }
}

@SuppressWarnings("GroovyUnusedAssignment")
abstract class BaseTest implements RewriteTest {
    private final String newVersion;

    BaseTest(String newVersion) {
        this.newVersion = newVersion;
    }

    @Test
    void changeSourceCompatibility() {
        rewriteRun(
          spec -> spec.recipe(new ChangeJavaCompatibility(newVersion, "source")),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              sourceCompatibility = 7
              """,
            """
              plugins {
                  id 'java'
              }
              
              sourceCompatibility = %s
              """.formatted(coerce(newVersion))
          )
        );
    }

    @Test
    void changeTargetCompatibility() {
        rewriteRun(
          spec -> spec.recipe(new ChangeJavaCompatibility(newVersion, "target")),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              targetCompatibility = '7'
              """,
            """
              plugins {
                  id 'java'
              }
              
              targetCompatibility = %s
              """.formatted(coerce(newVersion))
          )
        );
    }

    @Test
    void changeSetSourceCompatibility() {
        rewriteRun(
          spec -> spec.recipe(new ChangeJavaCompatibility(newVersion, "source")),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              setSourceCompatibility "7"
              """,
            """
              plugins {
                  id 'java'
              }
              
              setSourceCompatibility %s
              """.formatted(coerce(newVersion))
          )
        );
    }

    @Test
    void changeSetTargetCompatibility() {
        rewriteRun(
          spec -> spec.recipe(new ChangeJavaCompatibility(newVersion, "target")),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              setTargetCompatibility 1.7
              """,
            """
              plugins {
                  id 'java'
              }
              
              setTargetCompatibility %s
              """.formatted(coerce(newVersion))
          )
        );
    }

    @Test
    void changeSourceCompatibilityJavaPluginExtension() {
        rewriteRun(
          spec -> spec.recipe(new ChangeJavaCompatibility(newVersion, "source")),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              java {
                  sourceCompatibility = "1.7"
              }
              """,
            """
              plugins {
                  id 'java'
              }
              
              java {
                  sourceCompatibility = %s
              }
              """.formatted(coerce(newVersion))
          )
        );
    }

    @Test
    void changeTargetCompatibilityJavaPluginExtension() {
        rewriteRun(
          spec -> spec.recipe(new ChangeJavaCompatibility(newVersion, "target")),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              java {
                  targetCompatibility = '1.7'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              
              java {
                  targetCompatibility = %s
              }
              """.formatted(coerce(newVersion))
          )
        );
    }

    @Test
    void changeSourceCompatibilityCompileJava() {
        rewriteRun(
          spec -> spec.recipe(new ChangeJavaCompatibility(newVersion, "source")),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              compileJava {
                  sourceCompatibility = JavaVersion.VERSION_1_7
              }
              """,
            """
              plugins {
                  id 'java'
              }
              
              compileJava {
                  sourceCompatibility = %s
              }
              """.formatted(coerce(newVersion))
          )
        );
    }

    @Test
    void changeTargetCompatibilityCompileJava() {
        rewriteRun(
          spec -> spec.recipe(new ChangeJavaCompatibility(newVersion, "target")),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              compileJava {
                  targetCompatibility = "1.7"
              }
              """,
            """
              plugins {
                  id 'java'
              }
              
              compileJava {
                  targetCompatibility = %s
              }
              """.formatted(coerce(newVersion))
          )
        );
    }

    @Test
    void changeSourceCompatibilityTasksNamedCompileJava() {
        rewriteRun(
          spec -> spec.recipe(new ChangeJavaCompatibility(newVersion, "source")),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              tasks.named("compileJava") {
                  sourceCompatibility = "1.7"
              }
              """,
            """
              plugins {
                  id 'java'
              }
              
              tasks.named("compileJava") {
                  sourceCompatibility = %s
              }
              """.formatted(coerce(newVersion))
          )
        );
    }

    @Test
    void changeTargetCompatibilityTasksNamedCompileJava() {
        rewriteRun(
          spec -> spec.recipe(new ChangeJavaCompatibility(newVersion, "target")),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              tasks.named("compileJava") {
                  targetCompatibility = "1.7"
              }
              """,
            """
              plugins {
                  id 'java'
              }
              
              tasks.named("compileJava") {
                  targetCompatibility = %s
              }
              """.formatted(coerce(newVersion))
          )
        );
    }

    @Test
    void changeSourceCompatibilityTasksWithTypeJavaCompile() {
        rewriteRun(
          spec -> spec.recipe(new ChangeJavaCompatibility(newVersion, "source")),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              tasks.withType(JavaCompile).configureEach {
                  sourceCompatibility = "1.7"
              }
              """,
            """
              plugins {
                  id 'java'
              }
              
              tasks.withType(JavaCompile).configureEach {
                  sourceCompatibility = %s
              }
              """.formatted(coerce(newVersion))
          )
        );
    }

    @Test
    void changeTargetCompatibilityTasksWithTypeJavaCompile() {
        rewriteRun(
          spec -> spec.recipe(new ChangeJavaCompatibility(newVersion, "target")),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              tasks.withType(JavaCompile).configureEach {
                  targetCompatibility = "1.7"
              }
              """,
            """
              plugins {
                  id 'java'
              }
              
              tasks.withType(JavaCompile).configureEach {
                  targetCompatibility = %s
              }
              """.formatted(coerce(newVersion))
          )
        );
    }

    @Test
    void changeSourceCompatibilityJavaPluginExtensionFieldAccess() {
        rewriteRun(
          spec -> spec.recipe(new ChangeJavaCompatibility(newVersion, "source")),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              java.sourceCompatibility = "1.7"
              """,
            """
              plugins {
                  id 'java'
              }
              
              java.sourceCompatibility = %s
              """.formatted(coerce(newVersion))
          )
        );
    }

    @Test
    void changeTargetCompatibilityJavaPluginExtensionFieldAccess() {
        rewriteRun(
          spec -> spec.recipe(new ChangeJavaCompatibility(newVersion, "target")),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              
              java.targetCompatibility = "1.7"
              """,
            """
              plugins {
                  id 'java'
              }
              
              java.targetCompatibility = %s
              """.formatted(coerce(newVersion))
          )
        );
    }

    private String coerce(String version) {
        return version.startsWith("VERSION_") ? "JavaVersion." + version : version;
    }
}
