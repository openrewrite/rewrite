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

/**
 * Test cases for {@link UpgradeDependencyVersion} against Gradle version catalogs
 * ({@code dependencyResolutionManagement { versionCatalogs { ... } } }).
 */
class UpgradeDependencyVersionVersionCatalogTest implements RewriteTest {

    /**
     * Two libraries share a single version reference. Targeting each one in turn, via two
     * separate UDV recipe runs, should still only ever touch the shared version(...) declaration
     * -- neither library's own declaration should be rewritten or detached to a literal.
     */
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

    /**
     * Same starting catalog as above, but only the recipe targeting widget-a runs. Since
     * widget-b still shares the version reference and isn't targeted by this run, widget-a gets
     * detached to its own inline literal instead of bumping the shared version(...) declaration
     * -- which is left unchanged, along with widget-b's untouched versionRef(...) declaration.
     */
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

    /**
     * A library with an inline `.version(...)` isn't shared with anything -- its literal is
     * simply upgraded in place.
     */
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

    /**
     * A library declared with the single coordinate-string form has its version embedded in the
     * same literal as the group and artifact -- upgrading it rewrites the whole string.
     */
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

    /**
     * A library declared `.withoutVersion()` has no version of its own -- its version comes from
     * a BOM/platform or transitive resolution elsewhere. That's an explicit declaration of intent
     * to leave it unmanaged here, so UDV leaves it alone even though `newVersion` is specified --
     * it does not assume `.withoutVersion()` into an inline version.
     */
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

    /**
     * A library declared via `versionRef(...)` that no other library shares can have its shared
     * version(...) declaration bumped directly -- there's no untargeted sharer to protect, so no
     * detach is needed.
     */
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

    /**
     * `groupId`/`artifactId` can be glob expressions (per UDV's own javadoc) -- a single recipe
     * run can match every current sharer of a ref at once. That's the same end state as running
     * two exact-GA recipes in sequence (see the pair of tests above), but reached in one pass:
     * the scanner must discover every matching library by testing the glob against each one, not
     * by looking up one known GA.
     */
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

    /**
     * A glob can also match multiple libraries that don't share anything with each other -- each
     * has its own non-shared {@code versionRef}. There's no sharing to reconcile here, so this is
     * just the non-shared case (see {@link #nonSharedVersionRefIsUpgradedDirectly()}) applied once
     * per matched library in a single pass.
     */
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

    /**
     * A glob can also match only *some* of a ref's sharers in one pass -- `gadget-c` shares
     * `widgetVersion` too, but "widget-*" doesn't match it. The matched libraries still detach
     * to their own literal, exactly as if each had been targeted by a separate exact-GA run; the
     * unmatched sharer and the shared declaration are untouched.
     */
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

    /**
     * A library's version can't be resolved to a literal at all -- a rich
     * {@code .version { strictly(...) } }} constraint, whose argument is a closure rather than a
     * string literal, authored directly in the catalog from the start. Targeting it directly is a
     * safe no-op rather than a crash: it matches neither the inline-version nor the versionRef
     * shape, so {@code withVersion} has nothing to rewrite. And since it never used
     * {@code versionRef(...)} to begin with, it was never a member of widget-a/widget-b's
     * sharing group in the first place, so it doesn't interfere with -- or get swept into -- their
     * own reconciliation, which proceeds and collapses normally once they agree.
     */
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

    /**
     * A library's version is a GString rather than a plain string literal --
     * {@code .version("${widgetDVersion}")} -- so it can't be resolved to a literal either, the
     * same as a rich {@code .version { strictly(...) } }} constraint. Left unchanged rather than
     * crashing.
     */
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
