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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.Assertions.settingsGradleKts;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class UpgradeDependencyVersionVersionCatalogTest implements RewriteTest {

    @Test
    void sequentialRecipesTargetingBothSharersOnlyUpdateTheSharedVersionReference() {
        rewriteRun(
          spec -> spec.recipes(
            new UpgradeDependencyVersion("com.acme", "widget-a", "2.0", null),
            new UpgradeDependencyVersion("com.acme", "widget-b", "2.0", null)
          ),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widgetVersion', '1.0')
                          library('widgetA', 'com.acme', 'widget-a').versionRef('widgetVersion')
                          library('widgetB', 'com.acme', 'widget-b').versionRef('widgetVersion')
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widgetVersion', '2.0')
                          library('widgetA', 'com.acme', 'widget-a').versionRef('widgetVersion')
                          library('widgetB', 'com.acme', 'widget-b').versionRef('widgetVersion')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinSequentialRecipesTargetingBothSharersOnlyUpdateTheSharedVersionReference() {
        rewriteRun(
          spec -> spec.recipes(
            new UpgradeDependencyVersion("com.acme", "widget-a", "2.0", null),
            new UpgradeDependencyVersion("com.acme", "widget-b", "2.0", null)
          ),
          settingsGradleKts(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          version("widgetVersion", "1.0")
                          library("widgetA", "com.acme", "widget-a").versionRef("widgetVersion")
                          library("widgetB", "com.acme", "widget-b").versionRef("widgetVersion")
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          version("widgetVersion", "2.0")
                          library("widgetA", "com.acme", "widget-a").versionRef("widgetVersion")
                          library("widgetB", "com.acme", "widget-b").versionRef("widgetVersion")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void singleRecipeTargetingOneSharerDetachesInsteadOfUpdatingTheSharedReference() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-a", "2.0", null)),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widgetVersion', '1.0')
                          library('widgetA', 'com.acme', 'widget-a').versionRef('widgetVersion')
                          library('widgetB', 'com.acme', 'widget-b').versionRef('widgetVersion')
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widgetVersion', '1.0')
                          library('widgetA', 'com.acme', 'widget-a').version('2.0')
                          library('widgetB', 'com.acme', 'widget-b').versionRef('widgetVersion')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinSingleRecipeTargetingOneSharerDetachesInsteadOfUpdatingTheSharedReference() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-a", "2.0", null)),
          settingsGradleKts(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          version("widgetVersion", "1.0")
                          library("widgetA", "com.acme", "widget-a").versionRef("widgetVersion")
                          library("widgetB", "com.acme", "widget-b").versionRef("widgetVersion")
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          version("widgetVersion", "1.0")
                          library("widgetA", "com.acme", "widget-a").version("2.0")
                          library("widgetB", "com.acme", "widget-b").versionRef("widgetVersion")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void inlineVersionLibraryIsUpgradedDirectly() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "acme-core", "2.0", null)),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          library('acmeCoreLib', 'com.acme', 'acme-core').version('1.0')
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          library('acmeCoreLib', 'com.acme', 'acme-core').version('2.0')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinInlineVersionLibraryIsUpgradedDirectly() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "acme-core", "2.0", null)),
          settingsGradleKts(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          library("acmeCoreLib", "com.acme", "acme-core").version("1.0")
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          library("acmeCoreLib", "com.acme", "acme-core").version("2.0")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void singleStringCoordinateLibraryIsUpgradedDirectly() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget", "2.0", null)),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          library('acmeWidgetLib', 'com.acme:widget:1.0')
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          library('acmeWidgetLib', 'com.acme:widget:2.0')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinSingleStringCoordinateLibraryIsUpgradedDirectly() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget", "2.0", null)),
          settingsGradleKts(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          library("acmeWidgetLib", "com.acme:widget:1.0")
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          library("acmeWidgetLib", "com.acme:widget:2.0")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void libraryWithoutVersionIsLeftUnmanaged() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "acme-tool", "2.0", null)),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          library('acmeToolLib', 'com.acme', 'acme-tool').withoutVersion()
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinLibraryWithoutVersionIsLeftUnmanaged() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "acme-tool", "2.0", null)),
          settingsGradleKts(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          library("acmeToolLib", "com.acme", "acme-tool").withoutVersion()
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void nonSharedVersionRefIsUpgradedDirectly() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "acme-gadget", "2.0", null)),
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
                      libs {
                          version('acmeGadgetVersion', '2.0')
                          library('acmeGadgetLib', 'com.acme', 'acme-gadget').versionRef('acmeGadgetVersion')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinNonSharedVersionRefIsUpgradedDirectly() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "acme-gadget", "2.0", null)),
          settingsGradleKts(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          version("acmeGadgetVersion", "1.0")
                          library("acmeGadgetLib", "com.acme", "acme-gadget").versionRef("acmeGadgetVersion")
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          version("acmeGadgetVersion", "2.0")
                          library("acmeGadgetLib", "com.acme", "acme-gadget").versionRef("acmeGadgetVersion")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void wildcardGroupAndArtifactUpgradesAllMatchingSharersInOnePass() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-*", "2.0", null)),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widgetVersion', '1.0')
                          library('widgetA', 'com.acme', 'widget-a').versionRef('widgetVersion')
                          library('widgetB', 'com.acme', 'widget-b').versionRef('widgetVersion')
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widgetVersion', '2.0')
                          library('widgetA', 'com.acme', 'widget-a').versionRef('widgetVersion')
                          library('widgetB', 'com.acme', 'widget-b').versionRef('widgetVersion')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinWildcardGroupAndArtifactUpgradesAllMatchingSharersInOnePass() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-*", "2.0", null)),
          settingsGradleKts(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          version("widgetVersion", "1.0")
                          library("widgetA", "com.acme", "widget-a").versionRef("widgetVersion")
                          library("widgetB", "com.acme", "widget-b").versionRef("widgetVersion")
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          version("widgetVersion", "2.0")
                          library("widgetA", "com.acme", "widget-a").versionRef("widgetVersion")
                          library("widgetB", "com.acme", "widget-b").versionRef("widgetVersion")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void wildcardGroupAndArtifactUpgradesMultipleNonSharedLibrariesInOnePass() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-*", "2.0", null)),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widgetAVersion', '1.0')
                          library('widgetA', 'com.acme', 'widget-a').versionRef('widgetAVersion')
                          version('widgetBVersion', '1.0')
                          library('widgetB', 'com.acme', 'widget-b').versionRef('widgetBVersion')
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widgetAVersion', '2.0')
                          library('widgetA', 'com.acme', 'widget-a').versionRef('widgetAVersion')
                          version('widgetBVersion', '2.0')
                          library('widgetB', 'com.acme', 'widget-b').versionRef('widgetBVersion')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinWildcardGroupAndArtifactUpgradesMultipleNonSharedLibrariesInOnePass() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-*", "2.0", null)),
          settingsGradleKts(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          version("widgetAVersion", "1.0")
                          library("widgetA", "com.acme", "widget-a").versionRef("widgetAVersion")
                          version("widgetBVersion", "1.0")
                          library("widgetB", "com.acme", "widget-b").versionRef("widgetBVersion")
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          version("widgetAVersion", "2.0")
                          library("widgetA", "com.acme", "widget-a").versionRef("widgetAVersion")
                          version("widgetBVersion", "2.0")
                          library("widgetB", "com.acme", "widget-b").versionRef("widgetBVersion")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void wildcardGroupAndArtifactDetachesWhenAnUnmatchedSharerRemains() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-*", "2.0", null)),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widgetVersion', '1.0')
                          library('widgetA', 'com.acme', 'widget-a').versionRef('widgetVersion')
                          library('widgetB', 'com.acme', 'widget-b').versionRef('widgetVersion')
                          library('gadgetC', 'com.acme', 'gadget-c').versionRef('widgetVersion')
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widgetVersion', '1.0')
                          library('widgetA', 'com.acme', 'widget-a').version('2.0')
                          library('widgetB', 'com.acme', 'widget-b').version('2.0')
                          library('gadgetC', 'com.acme', 'gadget-c').versionRef('widgetVersion')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinWildcardGroupAndArtifactDetachesWhenAnUnmatchedSharerRemains() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-*", "2.0", null)),
          settingsGradleKts(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          version("widgetVersion", "1.0")
                          library("widgetA", "com.acme", "widget-a").versionRef("widgetVersion")
                          library("widgetB", "com.acme", "widget-b").versionRef("widgetVersion")
                          library("gadgetC", "com.acme", "gadget-c").versionRef("widgetVersion")
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          version("widgetVersion", "1.0")
                          library("widgetA", "com.acme", "widget-a").version("2.0")
                          library("widgetB", "com.acme", "widget-b").version("2.0")
                          library("gadgetC", "com.acme", "gadget-c").versionRef("widgetVersion")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void libraryWithUnresolvableVersionConstraintIsLeftUnchanged() {
        rewriteRun(
          spec -> spec.recipes(
            new UpgradeDependencyVersion("com.acme", "widget-a", "2.0", null),
            new UpgradeDependencyVersion("com.acme", "widget-b", "2.0", null),
            new UpgradeDependencyVersion("com.acme", "widget-c", "2.0", null)
          ),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widgetVersion', '1.0')
                          library('widgetA', 'com.acme', 'widget-a').versionRef('widgetVersion')
                          library('widgetB', 'com.acme', 'widget-b').versionRef('widgetVersion')
                          library('widgetC', 'com.acme', 'widget-c').version { strictly('1.0') }
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widgetVersion', '2.0')
                          library('widgetA', 'com.acme', 'widget-a').versionRef('widgetVersion')
                          library('widgetB', 'com.acme', 'widget-b').versionRef('widgetVersion')
                          library('widgetC', 'com.acme', 'widget-c').version { strictly('1.0') }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void libraryWithUnresolvableVersionConstraintIsLeftUnchangedWithWildcard() {
        rewriteRun(
          spec -> spec.recipes(
            new UpgradeDependencyVersion("com.acme", "widget-*", "2.0", null)
          ),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widgetVersion', '1.0')
                          library('widgetA', 'com.acme', 'widget-a').versionRef('widgetVersion')
                          library('widgetB', 'com.acme', 'widget-b').versionRef('widgetVersion')
                          library('widgetC', 'com.acme', 'widget-c').version { strictly('1.0') }
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widgetVersion', '2.0')
                          library('widgetA', 'com.acme', 'widget-a').versionRef('widgetVersion')
                          library('widgetB', 'com.acme', 'widget-b').versionRef('widgetVersion')
                          library('widgetC', 'com.acme', 'widget-c').version { strictly('1.0') }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void libraryWithInterpolatedVersionIsLeftUnchanged() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-d", "2.0", null)),
          settingsGradle(
            """
              def widgetDVersion = '1.0'

              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          library('widgetD', 'com.acme', 'widget-d').version("${widgetDVersion}")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void symbolicVersionResolvedWhenCatalogAppliedFromSeparateFileWithRepoInBuildGradle() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(new UpgradeDependencyVersion("com.google.guava", "guava", "30.x", "-jre")),
          settingsGradle(
            """
              rootProject.name = 'catalog-applied-file'
              apply from: './gradle/versions.gradle'
              """
          ),
          buildGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          library('guava', 'com.google.guava', 'guava').version('29.0-jre')
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          library('guava', 'com.google.guava', 'guava').version('30.1.1-jre')
                      }
                  }
              }
              """,
            spec1 -> spec1.path("gradle/versions.gradle")
          ),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation libs.guava
              }
              """
          )
        );
    }

    @Test
    void kotlinSymbolicVersionResolvedWhenCatalogAppliedFromSeparateFileWithRepoInBuildGradle() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(new UpgradeDependencyVersion("com.google.guava", "guava", "30.x", "-jre")),
          settingsGradleKts(
            """
              rootProject.name = "catalog-applied-file"
              apply(from = "./gradle/versions.gradle.kts")
              """
          ),
          buildGradleKts(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          library("guava", "com.google.guava", "guava").version("29.0-jre")
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          library("guava", "com.google.guava", "guava").version("30.1.1-jre")
                      }
                  }
              }
              """,
            spec1 -> spec1.path("gradle/versions.gradle.kts")
          ),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(libs.guava)
              }
              """
          )
        );
    }
}
