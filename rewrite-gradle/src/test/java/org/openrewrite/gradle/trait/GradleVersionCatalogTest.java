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
package org.openrewrite.gradle.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.*;

class GradleVersionCatalogTest implements RewriteTest {

    @Test
    void capturesOriginalVersionReferencesOnCatalogRoot() {
        // The marker only prints under verbose printing, as it must stay invisible in normal recipe output
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
                    new GradleVersionCatalog.Matcher().asVisitor(catalog -> catalog.withOriginalVersionReferencesMarker().getTree())))
                  .markerPrinter(PrintOutputCapture.MarkerPrinter.VERBOSE),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('springBootVersion', '3.5.15')
                          library('springBootStarterWeb', 'org.springframework.boot', 'spring-boot-starter-web').versionRef('springBootVersion')
                          library('springBootStarterWebflux', 'org.springframework.boot', 'spring-boot-starter-webflux').versionRef('springBootVersion')
                          library('acmeCoreLib', 'com.acme', 'acme-core').version('1.0.0')
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      /*~~(springBootVersion->3.5.15@[org.springframework.boot:spring-boot-starter-web, org.springframework.boot:spring-boot-starter-webflux])~~>*/libs {
                          version('springBootVersion', '3.5.15')
                          library('springBootStarterWeb', 'org.springframework.boot', 'spring-boot-starter-web').versionRef('springBootVersion')
                          library('springBootStarterWebflux', 'org.springframework.boot', 'spring-boot-starter-webflux').versionRef('springBootVersion')
                          library('acmeCoreLib', 'com.acme', 'acme-core').version('1.0.0')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void matchesInlineInSettingsGradle() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new GradleVersionCatalog.Matcher().asVisitor(catalog -> SearchResult.found(catalog.getTree())))),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('lombokVersion', '1.18.30')
                          library('projectLombok', 'org.projectlombok', 'lombok').versionRef('lombokVersion')
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      /*~~>*/libs {
                          version('lombokVersion', '1.18.30')
                          library('projectLombok', 'org.projectlombok', 'lombok').versionRef('lombokVersion')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void matchesWhenSplitIntoASeparateGradleFile() {
        // Mirrors the `apply from: './gradle/versions.gradle'` pattern
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new GradleVersionCatalog.Matcher().asVisitor(catalog -> SearchResult.found(catalog.getTree())))),
          buildGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('lombokVersion', '1.18.30')
                          library('projectLombok', 'org.projectlombok', 'lombok').versionRef('lombokVersion')
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      /*~~>*/libs {
                          version('lombokVersion', '1.18.30')
                          library('projectLombok', 'org.projectlombok', 'lombok').versionRef('lombokVersion')
                      }
                  }
              }
              """,
            spec1 -> spec1.path("gradle/versions.gradle")
          )
        );
    }

    @Test
    void matchesOnlyRequestedCatalogName() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new GradleVersionCatalog.Matcher().catalogName("testLibs").asVisitor(catalog -> SearchResult.found(catalog.getTree())))),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('lombokVersion', '1.18.30')
                      }
                      testLibs {
                          version('junitVersion', '5.10.0')
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('lombokVersion', '1.18.30')
                      }
                      /*~~>*/testLibs {
                          version('junitVersion', '5.10.0')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void getVersionResolvesInlineVersion() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new GradleVersionCatalog.Matcher().asVisitor(catalog -> SearchResult.found(catalog.getTree(),
              catalog.getVersion(new GroupArtifact("com.acme", "acme-core")))))),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          library('acmeCoreLib', 'com.acme', 'acme-core').version('1.0.0')
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      /*~~(1.0.0)~~>*/libs {
                          library('acmeCoreLib', 'com.acme', 'acme-core').version('1.0.0')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void getVersionResolvesThroughVersionRef() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new GradleVersionCatalog.Matcher().asVisitor(catalog -> SearchResult.found(catalog.getTree(),
              catalog.getVersion(new GroupArtifact("com.acme", "acme-gadget")))))),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('acmeGadgetVersion', '1.0')
                          library('acmeGadgetLib', 'com.acme', 'acme-gadget').versionRef('acmeGadgetVersion')
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      /*~~(1.0)~~>*/libs {
                          version('acmeGadgetVersion', '1.0')
                          library('acmeGadgetLib', 'com.acme', 'acme-gadget').versionRef('acmeGadgetVersion')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void getVersionReturnsNullForLibraryWithoutVersion() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new GradleVersionCatalog.Matcher().asVisitor(catalog -> SearchResult.found(catalog.getTree(),
              "version=" + catalog.getVersion(new GroupArtifact("com.acme", "acme-tool")))))),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          library('acmeToolLib', 'com.acme', 'acme-tool').withoutVersion()
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      /*~~(version=null)~~>*/libs {
                          library('acmeToolLib', 'com.acme', 'acme-tool').withoutVersion()
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void getVersionReturnsNullWhenLibraryNotFound() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new GradleVersionCatalog.Matcher().asVisitor(catalog -> SearchResult.found(catalog.getTree(),
              "version=" + catalog.getVersion(new GroupArtifact("com.acme", "does-not-exist")))))),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          library('acmeCoreLib', 'com.acme', 'acme-core').version('1.0.0')
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      /*~~(version=null)~~>*/libs {
                          library('acmeCoreLib', 'com.acme', 'acme-core').version('1.0.0')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinMatchesInlineInSettingsGradle() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new GradleVersionCatalog.Matcher().asVisitor(catalog -> SearchResult.found(catalog.getTree())))),
          settingsGradleKts(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          version("lombokVersion", "1.18.30")
                          library("projectLombok", "org.projectlombok", "lombok").versionRef("lombokVersion")
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      /*~~>*/create("libs") {
                          version("lombokVersion", "1.18.30")
                          library("projectLombok", "org.projectlombok", "lombok").versionRef("lombokVersion")
                      }
                  }
              }
              """
          )
        );
    }
}
