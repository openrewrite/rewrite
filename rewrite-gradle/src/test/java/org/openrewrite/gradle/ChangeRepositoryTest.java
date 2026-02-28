/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class ChangeRepositoryTest implements RewriteTest {

    @DocumentExample
    @Test
    void namedToNamed() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("jcenter", null, "mavenCentral", null)),
          buildGradle(
            """
              repositories {
                  jcenter()
              }
              """,
            """
              repositories {
                  mavenCentral()
              }
              """
          )
        );
    }

    @Test
    void namedToNamedKts() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("jcenter", null, "mavenCentral", null)),
          buildGradleKts(
            """
              repositories {
                  jcenter()
              }
              """,
            """
              repositories {
                  mavenCentral()
              }
              """
          )
        );
    }

    @Test
    void namedToCustomMaven() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("jcenter", null, "maven", "https://nexus.example.com/releases")),
          buildGradle(
            """
              repositories {
                  jcenter()
              }
              """,
            """
              repositories {
                  maven {
                      url = "https://nexus.example.com/releases"
                  }
              }
              """
          )
        );
    }

    @Test
    void namedToCustomMavenKts() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("jcenter", null, "maven", "https://nexus.example.com/releases")),
          buildGradleKts(
            """
              repositories {
                  jcenter()
              }
              """,
            """
              repositories {
                  maven {
                      url = uri("https://nexus.example.com/releases")
                  }
              }
              """
          )
        );
    }

    @Test
    void customMavenToNamed() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("maven", "https://jcenter.bintray.com", "mavenCentral", null)),
          buildGradle(
            """
              repositories {
                  maven {
                      url = "https://jcenter.bintray.com"
                  }
              }
              """,
            """
              repositories {
                  mavenCentral()
              }
              """
          )
        );
    }

    @Test
    void customMavenToNamedKts() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("maven", "https://jcenter.bintray.com", "mavenCentral", null)),
          buildGradleKts(
            """
              repositories {
                  maven {
                      url = uri("https://jcenter.bintray.com")
                  }
              }
              """,
            """
              repositories {
                  mavenCentral()
              }
              """
          )
        );
    }

    @Test
    void changeUrl() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("maven", "https://old-nexus.example.com/releases", "maven", "https://new-nexus.example.com/releases")),
          buildGradle(
            """
              repositories {
                  maven {
                      url = "https://old-nexus.example.com/releases"
                  }
              }
              """,
            """
              repositories {
                  maven {
                      url = "https://new-nexus.example.com/releases"
                  }
              }
              """
          )
        );
    }

    @Test
    void changeUrlKts() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("maven", "https://old-nexus.example.com/releases", "maven", "https://new-nexus.example.com/releases")),
          buildGradleKts(
            """
              repositories {
                  maven {
                      url = uri("https://old-nexus.example.com/releases")
                  }
              }
              """,
            """
              repositories {
                  maven {
                      url = uri("https://new-nexus.example.com/releases")
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenAlreadyCorrect() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("jcenter", null, "mavenCentral", null)),
          buildGradle(
            """
              repositories {
                  mavenCentral()
              }
              """
          )
        );
    }

    @Test
    void onlyMatchesSpecifiedUrl() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("maven", "https://old-nexus.example.com/releases", "maven", "https://new-nexus.example.com/releases")),
          buildGradle(
            """
              repositories {
                  maven {
                      url = "https://other-repo.example.com/releases"
                  }
              }
              """
          )
        );
    }

    @Test
    void preservesOtherRepositories() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("jcenter", null, "mavenCentral", null)),
          buildGradle(
            """
              repositories {
                  jcenter()
                  mavenLocal()
              }
              """,
            """
              repositories {
                  mavenCentral()
                  mavenLocal()
              }
              """
          )
        );
    }

    @Test
    void changeUrlUsingSetUrlMethod() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("maven", "https://old-nexus.example.com/releases", "maven", "https://new-nexus.example.com/releases")),
          buildGradle(
            """
              repositories {
                  maven {
                      setUrl("https://old-nexus.example.com/releases")
                  }
              }
              """,
            """
              repositories {
                  maven {
                      setUrl("https://new-nexus.example.com/releases")
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotMatchOutsideRepositoriesBlock() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("jcenter", null, "mavenCentral", null)),
          buildGradle(
            """
              repositories {
                  mavenLocal()
              }
              """
          )
        );
    }

    @Test
    void matchByUrlOnly() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository(null, "https://old-nexus.example.com/releases", "maven", "https://new-nexus.example.com/releases")),
          buildGradle(
            """
              repositories {
                  maven {
                      url = "https://old-nexus.example.com/releases"
                  }
              }
              """,
            """
              repositories {
                  maven {
                      url = "https://new-nexus.example.com/releases"
                  }
              }
              """
          )
        );
    }

    @Test
    void newUrlOnlyKeepsType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("maven", "https://old-nexus.example.com/releases", null, "https://new-nexus.example.com/releases")),
          buildGradle(
            """
              repositories {
                  maven {
                      url = "https://old-nexus.example.com/releases"
                  }
              }
              """,
            """
              repositories {
                  maven {
                      url = "https://new-nexus.example.com/releases"
                  }
              }
              """
          )
        );
    }

    @Test
    void newUrlOnlyKeepsTypeKts() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository(null, "https://old-nexus.example.com/releases", null, "https://new-nexus.example.com/releases")),
          buildGradleKts(
            """
              repositories {
                  maven {
                      url = uri("https://old-nexus.example.com/releases")
                  }
              }
              """,
            """
              repositories {
                  maven {
                      url = uri("https://new-nexus.example.com/releases")
                  }
              }
              """
          )
        );
    }

    @Test
    void matchByUrlOnlyReplace() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository(null, "https://old-nexus.example.com/releases", "mavenCentral", null)),
          buildGradle(
            """
              repositories {
                  maven {
                      url = "https://old-nexus.example.com/releases"
                  }
              }
              """,
            """
              repositories {
                  mavenCentral()
              }
              """
          )
        );
    }

    @Test
    void preservesCredentialsWhenChangingUrl() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("maven", "https://old-nexus.example.com/releases", "maven", "https://new-nexus.example.com/releases")),
          buildGradle(
            """
              repositories {
                  maven {
                      url = "https://old-nexus.example.com/releases"
                      credentials {
                          username = findProperty("mavenUsername")
                          password = findProperty("mavenPassword")
                      }
                  }
              }
              """,
            """
              repositories {
                  maven {
                      url = "https://new-nexus.example.com/releases"
                      credentials {
                          username = findProperty("mavenUsername")
                          password = findProperty("mavenPassword")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void preservesCredentialsWhenChangingUrlKts() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("maven", "https://old-nexus.example.com/releases", "maven", "https://new-nexus.example.com/releases")),
          buildGradleKts(
            """
              repositories {
                  maven {
                      url = uri("https://old-nexus.example.com/releases")
                      credentials {
                          username = System.getenv("MAVEN_USERNAME")
                          password = System.getenv("MAVEN_PASSWORD")
                      }
                  }
              }
              """,
            """
              repositories {
                  maven {
                      url = uri("https://new-nexus.example.com/releases")
                      credentials {
                          username = System.getenv("MAVEN_USERNAME")
                          password = System.getenv("MAVEN_PASSWORD")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void preservesAuthenticationBlockWhenChangingUrl() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("maven", "https://old-nexus.example.com/releases", "maven", "https://new-nexus.example.com/releases")),
          buildGradle(
            """
              repositories {
                  maven {
                      url = "https://old-nexus.example.com/releases"
                      credentials {
                          username = findProperty("mavenUsername")
                          password = findProperty("mavenPassword")
                      }
                      authentication {
                          basic(BasicAuthentication)
                      }
                  }
              }
              """,
            """
              repositories {
                  maven {
                      url = "https://new-nexus.example.com/releases"
                      credentials {
                          username = findProperty("mavenUsername")
                          password = findProperty("mavenPassword")
                      }
                      authentication {
                          basic(BasicAuthentication)
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void preservesNameAndOtherPropertiesWhenChangingUrl() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("maven", "https://old-nexus.example.com/releases", "maven", "https://new-nexus.example.com/releases")),
          buildGradle(
            """
              repositories {
                  maven {
                      name = "myRepo"
                      url = "https://old-nexus.example.com/releases"
                      allowInsecureProtocol = true
                  }
              }
              """,
            """
              repositories {
                  maven {
                      name = "myRepo"
                      url = "https://new-nexus.example.com/releases"
                      allowInsecureProtocol = true
                  }
              }
              """
          )
        );
    }

    @Test
    void removesOldWhenTargetNamedRepoAlreadyExists() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("jcenter", null, "mavenCentral", null)),
          buildGradle(
            """
              repositories {
                  jcenter()
                  mavenCentral()
              }
              """,
            """
              repositories {
                  mavenCentral()
              }
              """
          )
        );
    }

    @Test
    void removesOldWhenTargetCustomRepoAlreadyExists() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("maven", "https://old-nexus.example.com/releases", "maven", "https://new-nexus.example.com/releases")),
          buildGradle(
            """
              repositories {
                  maven {
                      url = "https://old-nexus.example.com/releases"
                  }
                  maven {
                      url = "https://new-nexus.example.com/releases"
                  }
              }
              """,
            """
              repositories {
                  maven {
                      url = "https://new-nexus.example.com/releases"
                  }
              }
              """
          )
        );
    }

    @Test
    void removesOldWhenTargetNamedRepoAlreadyExistsKts() {
        rewriteRun(
          spec -> spec.recipe(new ChangeRepository("jcenter", null, "mavenCentral", null)),
          buildGradleKts(
            """
              repositories {
                  jcenter()
                  mavenCentral()
              }
              """,
            """
              repositories {
                  mavenCentral()
              }
              """
          )
        );
    }
}
