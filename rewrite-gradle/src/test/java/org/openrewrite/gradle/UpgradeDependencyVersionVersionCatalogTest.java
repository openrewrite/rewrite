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
import static org.openrewrite.properties.Assertions.properties;

class UpgradeDependencyVersionVersionCatalogTest implements RewriteTest {

    @Test
    void sequentialRecipesDetachAllButTheLastReferrer() {
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
    void kotlinSequentialRecipesDetachAllButTheLastReferrer() {
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
    void pluginReferrerKeepsTheSharedVersionFromMoving() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-*", "2.0", null)),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widgetVersion', '1.0')
                          library('widgetA', 'com.acme', 'widget-a').versionRef('widgetVersion')
                          plugin('widget', 'com.acme.widget').versionRef('widgetVersion')
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
                          plugin('widget', 'com.acme.widget').versionRef('widgetVersion')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinPluginReferrerKeepsTheSharedVersionFromMoving() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-*", "2.0", null)),
          settingsGradleKts(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          version("widgetVersion", "1.0")
                          library("widgetA", "com.acme", "widget-a").versionRef("widgetVersion")
                          plugin("widget", "com.acme.widget").versionRef("widgetVersion")
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
                          plugin("widget", "com.acme.widget").versionRef("widgetVersion")
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
    void libraryWithVersionConstraintIsUpgradedInPlace() {
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
                          library('widgetA', 'com.acme', 'widget-a').version('2.0')
                          library('widgetB', 'com.acme', 'widget-b').versionRef('widgetVersion')
                          library('widgetC', 'com.acme', 'widget-c').version { strictly('2.0') }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void libraryWithVersionConstraintIsUpgradedInPlaceWithWildcard() {
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
                          library('widgetC', 'com.acme', 'widget-c').version { strictly('2.0') }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinLibraryWithVersionConstraintIsUpgradedInPlace() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-c", "2.0", null)),
          settingsGradleKts(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          library("widgetC", "com.acme", "widget-c").version { strictly("1.0") }
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          library("widgetC", "com.acme", "widget-c").version { strictly("2.0") }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void libraryWithVersionConstraintKeepsItsOtherCalls() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-c", "2.0", null)),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          library('widgetC', 'com.acme', 'widget-c').version {
                              require('1.0')
                              reject('1.1', '1.2')
                          }
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          library('widgetC', 'com.acme', 'widget-c').version {
                              require('2.0')
                              reject('1.1', '1.2')
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void versionConstraintDeclarationSharedByEveryReferrerIsUpgradedInPlace() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-*", "2.0", null)),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widgetVersion') { require('1.0') }
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
                          version('widgetVersion') { require('2.0') }
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
    void libraryAlreadyAtTheNewVersionConstraintIsLeftAlone() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-c", "2.0", null)),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          library('widgetC', 'com.acme', 'widget-c').version { strictly('2.0') }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void libraryWithInterpolatedVersionIsUpgraded() {
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
              """,
            """
              def widgetDVersion = '2.0'

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
    void libraryWithPartiallyInterpolatedVersionIsLeftUnchanged() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-d", "2.0", null)),
          settingsGradle(
            """
              def widgetDMinor = '0'

              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          library('widgetD', 'com.acme', 'widget-d').version("1.${widgetDMinor}")
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

    @Test
    void versionCatalogProducerBlockIsUpgraded() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-a", "2.0", null)),
          buildGradle(
            """
              apply plugin: 'version-catalog'

              catalog {
                  versionCatalog {
                      version('widgetVersion', '1.0')
                      library('widgetA', 'com.acme', 'widget-a').versionRef('widgetVersion')
                      plugin('widgetPlugin', 'com.acme.widget').versionRef('widgetVersion')
                  }
              }
              """,
            """
              apply plugin: 'version-catalog'

              catalog {
                  versionCatalog {
                      version('widgetVersion', '1.0')
                      library('widgetA', 'com.acme', 'widget-a').version('2.0')
                      plugin('widgetPlugin', 'com.acme.widget').versionRef('widgetVersion')
                  }
              }
              """
          )
        );
    }

    @Test
    void versionCatalogProducerBlockSharedVersionIsUpgradedInPlace() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-*", "2.0", null)),
          buildGradle(
            """
              apply plugin: 'version-catalog'

              catalog {
                  versionCatalog {
                      version('widgetVersion', '1.0')
                      library('widgetA', 'com.acme', 'widget-a').versionRef('widgetVersion')
                      library('widgetB', 'com.acme', 'widget-b').versionRef('widgetVersion')
                  }
              }
              """,
            """
              apply plugin: 'version-catalog'

              catalog {
                  versionCatalog {
                      version('widgetVersion', '2.0')
                      library('widgetA', 'com.acme', 'widget-a').versionRef('widgetVersion')
                      library('widgetB', 'com.acme', 'widget-b').versionRef('widgetVersion')
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinVersionCatalogProducerBlockIsUpgraded() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "acme-core", "2.0", null)),
          buildGradleKts(
            """
              plugins {
                  `version-catalog`
              }

              catalog {
                  versionCatalog {
                      library("acmeCoreLib", "com.acme", "acme-core").version("1.0")
                  }
              }
              """,
            """
              plugins {
                  `version-catalog`
              }

              catalog {
                  versionCatalog {
                      library("acmeCoreLib", "com.acme", "acme-core").version("2.0")
                  }
              }
              """
          )
        );
    }

    @Test
    void entriesInEveryVersionCatalogBlockAreUpgraded() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-*", "2.0", null)),
          buildGradle(
            """
              apply plugin: 'version-catalog'

              catalog {
                  versionCatalog {
                      library('widgetA', 'com.acme', 'widget-a').version('1.0')
                  }
                  versionCatalog {
                      version('widgetBVersion', '1.0')
                      library('widgetB', 'com.acme', 'widget-b').versionRef('widgetBVersion')
                  }
              }
              """,
            """
              apply plugin: 'version-catalog'

              catalog {
                  versionCatalog {
                      library('widgetA', 'com.acme', 'widget-a').version('2.0')
                  }
                  versionCatalog {
                      version('widgetBVersion', '2.0')
                      library('widgetB', 'com.acme', 'widget-b').versionRef('widgetBVersion')
                  }
              }
              """
          )
        );
    }

    @Test
    void versionRefIsResolvedAcrossVersionCatalogBlocksOfOneCatalog() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-a", "2.0", null)),
          buildGradle(
            """
              apply plugin: 'version-catalog'

              catalog {
                  versionCatalog {
                      version('shared', '1.0')
                      library('widgetA', 'com.acme', 'widget-a').versionRef('shared')
                  }
                  versionCatalog {
                      library('widgetB', 'com.acme', 'widget-b').versionRef('shared')
                  }
              }
              """,
            """
              apply plugin: 'version-catalog'

              catalog {
                  versionCatalog {
                      version('shared', '1.0')
                      library('widgetA', 'com.acme', 'widget-a').version('2.0')
                  }
                  versionCatalog {
                      library('widgetB', 'com.acme', 'widget-b').versionRef('shared')
                  }
              }
              """
          )
        );
    }

    @Test
    void catalogConfiguredThroughAProjectBlockIsUpgraded() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-a", "2.0", null)),
          settingsGradle("include 'catalog'"),
          buildGradle(
            """
              project(':catalog') {
                  apply plugin: 'version-catalog'

                  catalog {
                      versionCatalog {
                          library('widgetA', 'com.acme', 'widget-a').version('1.0')
                      }
                  }
              }
              """,
            """
              project(':catalog') {
                  apply plugin: 'version-catalog'

                  catalog {
                      versionCatalog {
                          library('widgetA', 'com.acme', 'widget-a').version('2.0')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void versionHeldInAVariableIsUpgraded() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-*", "2.0", null)),
          settingsGradle(
            """
              ext {
                  widgetAVersion = '1.0'
              }

              def widgetBVersion = '1.0'

              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widgetB', widgetBVersion)
                          library('widgetA', 'com.acme', 'widget-a').version(widgetAVersion)
                          library('widgetB', 'com.acme', 'widget-b').versionRef('widgetB')
                      }
                  }
              }
              """,
            """
              ext {
                  widgetAVersion = '2.0'
              }

              def widgetBVersion = '2.0'

              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widgetB', widgetBVersion)
                          library('widgetA', 'com.acme', 'widget-a').version(widgetAVersion)
                          library('widgetB', 'com.acme', 'widget-b').versionRef('widgetB')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinVersionHeldInAVariableIsUpgraded() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-*", "2.0", null)),
          settingsGradleKts(
            """
              val widgetAVersion = "1.0"
              val widgetBVersion = "1.0"

              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          library("widgetA", "com.acme", "widget-a").version(widgetAVersion)
                          library("widgetB", "com.acme", "widget-b").version("$widgetBVersion")
                      }
                  }
              }
              """,
            """
              val widgetAVersion = "2.0"
              val widgetBVersion = "2.0"

              dependencyResolutionManagement {
                  versionCatalogs {
                      create("libs") {
                          library("widgetA", "com.acme", "widget-a").version(widgetAVersion)
                          library("widgetB", "com.acme", "widget-b").version("$widgetBVersion")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void versionHeldInGradlePropertiesIsUpgraded() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-a", "2.0", null)),
          properties(
            """
              widgetVersion=1.0
              """,
            """
              widgetVersion=2.0
              """,
            spec -> spec.path("gradle.properties")
          ),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widget', widgetVersion)
                          library('widgetA', 'com.acme', 'widget-a').versionRef('widget')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void pluginSharingTheVariableDoesNotHoldTheUpgradeBack() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-a", "2.0", null)),
          settingsGradle(
            """
              def widgetVersion = '1.0'

              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widget', widgetVersion)
                          library('widgetA', 'com.acme', 'widget-a').versionRef('widget')
                          plugin('widgetPlugin', 'com.acme.widget').versionRef('widget')
                      }
                  }
              }
              """,
            """
              def widgetVersion = '2.0'

              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('widget', widgetVersion)
                          library('widgetA', 'com.acme', 'widget-a').versionRef('widget')
                          plugin('widgetPlugin', 'com.acme.widget').versionRef('widget')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void sharedVariableMovesWhenTheUntargetedNeighbourIsPublishedAtTheNewVersion() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(new UpgradeDependencyVersion("org.apache.tomcat.embed", "tomcat-embed-core", "10.0.27", null)),
          settingsGradle(
            """
              rootProject.name = 'catalog-variable'
              apply from: './gradle/versions.gradle'
              """
          ),
          buildGradle(
            """
              def tomcatVersion = '10.0.0'

              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('tomcat', tomcatVersion)
                          library('tomcatCore', 'org.apache.tomcat.embed', 'tomcat-embed-core').versionRef('tomcat')
                          library('tomcatEl', 'org.apache.tomcat.embed', 'tomcat-embed-el').versionRef('tomcat')
                      }
                  }
              }
              """,
            """
              def tomcatVersion = '10.0.27'

              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('tomcat', tomcatVersion)
                          library('tomcatCore', 'org.apache.tomcat.embed', 'tomcat-embed-core').versionRef('tomcat')
                          library('tomcatEl', 'org.apache.tomcat.embed', 'tomcat-embed-el').versionRef('tomcat')
                      }
                  }
              }
              """,
            spec -> spec.path("gradle/versions.gradle")
          ),
          buildGradle(
            """
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
    void sharedVariableIsLeftAloneWhenTheUntargetedNeighbourHasNoSuchVersion() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(new UpgradeDependencyVersion("org.apache.tomcat.embed", "tomcat-embed-core", "10.0.27", null)),
          settingsGradle(
            """
              rootProject.name = 'catalog-variable'
              apply from: './gradle/versions.gradle'
              """
          ),
          buildGradle(
            """
              def tomcatVersion = '10.0.0'

              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('tomcat', tomcatVersion)
                          library('tomcatCore', 'org.apache.tomcat.embed', 'tomcat-embed-core').versionRef('tomcat')
                          library('guava', 'com.google.guava', 'guava').versionRef('tomcat')
                      }
                  }
              }
              """,
            spec -> spec.path("gradle/versions.gradle")
          ),
          buildGradle(
            """
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
    void latestPatchUpgradesAVersionHeldInAVariable() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(new UpgradeDependencyVersion("org.apache.tomcat.embed", "tomcat-embed-core", "latest.patch", null)),
          settingsGradle(
            """
              rootProject.name = 'catalog-variable'
              apply from: './gradle/versions.gradle'
              """
          ),
          buildGradle(
            """
              def tomcatVersion = '10.0.0'

              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('tomcat', tomcatVersion)
                          library('tomcatEmbedCore', 'org.apache.tomcat.embed', 'tomcat-embed-core').versionRef('tomcat')
                      }
                  }
              }
              """,
            """
              def tomcatVersion = '10.0.27'

              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('tomcat', tomcatVersion)
                          library('tomcatEmbedCore', 'org.apache.tomcat.embed', 'tomcat-embed-core').versionRef('tomcat')
                      }
                  }
              }
              """,
            spec -> spec.path("gradle/versions.gradle")
          ),
          buildGradle(
            """
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
    void producerBlockVersionHeldInAnExtPropertyIsUpgraded() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(new UpgradeDependencyVersion("com.acme", "widget-a", "2.0", null)),
          buildGradle(
            """
              buildscript {
                  ext {
                      widgetVersion = '1.0'
                  }
                  repositories {
                      mavenCentral()
                  }
              }

              apply plugin: 'version-catalog'

              catalog {
                  versionCatalog {
                      version('widget', widgetVersion)
                      library('widgetA', 'com.acme', 'widget-a').versionRef('widget')
                      plugin('widgetPlugin', 'com.acme.widget').versionRef('widget')
                  }
              }
              """,
            """
              buildscript {
                  ext {
                      widgetVersion = '2.0'
                  }
                  repositories {
                      mavenCentral()
                  }
              }

              apply plugin: 'version-catalog'

              catalog {
                  versionCatalog {
                      version('widget', widgetVersion)
                      library('widgetA', 'com.acme', 'widget-a').versionRef('widget')
                      plugin('widgetPlugin', 'com.acme.widget').versionRef('widget')
                  }
              }
              """
          )
        );
    }
}
