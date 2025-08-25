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
package org.openrewrite.gradle.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;

class FindRepositoryTest implements RewriteTest {
    @DocumentExample
    @Test
    void repositoryByUrl() {
        rewriteRun(
          spec -> spec.recipe(new FindRepository(null, "https://central.sonatype.com/repository/maven-snapshots", null)),
          buildGradle(
            """
              buildscript {
                repositories {
                  maven { url = "https://central.sonatype.com/repository/maven-snapshots" }
                  maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
                  maven { setUrl("https://central.sonatype.com/repository/maven-snapshots") }
                  maven { setUrl(uri("https://central.sonatype.com/repository/maven-snapshots")) }
                }
              }

              repositories {
                maven { url = "https://central.sonatype.com/repository/maven-snapshots" }
                maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
                maven { setUrl("https://central.sonatype.com/repository/maven-snapshots") }
                maven { setUrl(uri("https://central.sonatype.com/repository/maven-snapshots")) }
              }
              """,
            """
              buildscript {
                repositories {
                  /*~~>*/maven { url = "https://central.sonatype.com/repository/maven-snapshots" }
                  /*~~>*/maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
                  /*~~>*/maven { setUrl("https://central.sonatype.com/repository/maven-snapshots") }
                  /*~~>*/maven { setUrl(uri("https://central.sonatype.com/repository/maven-snapshots")) }
                }
              }

              repositories {
                /*~~>*/maven { url = "https://central.sonatype.com/repository/maven-snapshots" }
                /*~~>*/maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
                /*~~>*/maven { setUrl("https://central.sonatype.com/repository/maven-snapshots") }
                /*~~>*/maven { setUrl(uri("https://central.sonatype.com/repository/maven-snapshots")) }
              }
              """
          ),
          settingsGradle(
            """
              pluginManagement {
                repositories {
                  maven { url = "https://central.sonatype.com/repository/maven-snapshots" }
                  maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
                  maven { setUrl("https://central.sonatype.com/repository/maven-snapshots") }
                  maven { setUrl(uri("https://central.sonatype.com/repository/maven-snapshots")) }
                }
              }
              """,
            """
              pluginManagement {
                repositories {
                  /*~~>*/maven { url = "https://central.sonatype.com/repository/maven-snapshots" }
                  /*~~>*/maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
                  /*~~>*/maven { setUrl("https://central.sonatype.com/repository/maven-snapshots") }
                  /*~~>*/maven { setUrl(uri("https://central.sonatype.com/repository/maven-snapshots")) }
                }
              }
              """
          )
        );
    }

    @Test
    void repositoryByUrlAndPurposeProject() {
        rewriteRun(
          spec -> spec.recipe(new FindRepository(null, "https://central.sonatype.com/repository/maven-snapshots", FindRepository.Purpose.Project)),
          buildGradle(
            """
              buildscript {
                repositories {
                  maven { url = "https://central.sonatype.com/repository/maven-snapshots" }
                }
              }

              repositories {
                maven { url = "https://central.sonatype.com/repository/maven-snapshots" }
              }
              """,
            """
              buildscript {
                repositories {
                  maven { url = "https://central.sonatype.com/repository/maven-snapshots" }
                }
              }

              repositories {
                /*~~>*/maven { url = "https://central.sonatype.com/repository/maven-snapshots" }
              }
              """
          )
        );
    }

    @Test
    void repositoryByUrlAndPurposePlugin() {
        rewriteRun(
          spec -> spec.recipe(new FindRepository(null, "https://central.sonatype.com/repository/maven-snapshots", FindRepository.Purpose.Plugin)),
          buildGradle(
            """
              buildscript {
                repositories {
                  maven { url = "https://central.sonatype.com/repository/maven-snapshots" }
                }
              }

              repositories {
                maven { url = "https://central.sonatype.com/repository/maven-snapshots" }
              }
              """,
            """
              buildscript {
                repositories {
                  /*~~>*/maven { url = "https://central.sonatype.com/repository/maven-snapshots" }
                }
              }

              repositories {
                maven { url = "https://central.sonatype.com/repository/maven-snapshots" }
              }
              """
          ),
          settingsGradle(
            """
              pluginManagement {
                repositories {
                  maven { url = "https://central.sonatype.com/repository/maven-snapshots" }
                }
              }
              """,
            """
              pluginManagement {
                repositories {
                  /*~~>*/maven { url = "https://central.sonatype.com/repository/maven-snapshots" }
                }
              }
              """
          )
        );
    }

    @Test
    void groovyStringRepository() {
        rewriteRun(
          spec -> spec.recipe(new FindRepository(null, "${NEXUS_URL}/content/repositories/snapshots", null)),
          settingsGradle(
            """
              pluginManagement {
                repositories {
                  maven { url = "${NEXUS_URL}/content/repositories/snapshots" }
                }
              }
              """,
            """
              pluginManagement {
                repositories {
                  /*~~>*/maven { url = "${NEXUS_URL}/content/repositories/snapshots" }
                }
              }
              """
          )
        );
    }

    @Test
    void repositoryByType() {
        rewriteRun(
          spec -> spec.recipe(new FindRepository("mavenCentral", null, null)),
          buildGradle(
            """
              repositories {
                mavenCentral()
              }
              """,
            """
              repositories {
                /*~~>*/mavenCentral()
              }
              """
          )
        );
    }
}
