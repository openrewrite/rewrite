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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.properties.Assertions.properties;

class SyncGradleExtPropertiesWithBomTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SyncGradleExtPropertiesWithBom(
                "org.springframework.boot",
                "spring-boot-dependencies",
                "3.4.0",
                false
        ));
    }

    @DocumentExample
    @Test
    void updatesExtSubscriptPropertyWhenBomVersionIsHigher() {
        rewriteRun(
          //language=gradle
          buildGradle(
            """
              ext['jackson-bom.version'] = '2.14.0'
              """,
            """
              ext['jackson-bom.version'] = '2.18.1'
              """
          )
        );
    }

    @Test
    void updatesExtSetMethodPropertyWhenBomVersionIsHigher() {
        rewriteRun(
          //language=gradle
          buildGradle(
            """
              ext.set('jackson-bom.version', '2.14.0')
              """,
            """
              ext.set('jackson-bom.version', '2.18.1')
              """
          )
        );
    }

    @Test
    void updatesExtBlockAssignmentWhenBomVersionIsHigher() {
        rewriteRun(
          //language=gradle
          buildGradle(
            """
              ext {
                  set('jackson-bom.version', '2.14.0')
              }
              """,
            """
              ext {
                  set('jackson-bom.version', '2.18.1')
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenExtVersionIsHigherThanBom() {
        rewriteRun(
          //language=gradle
          buildGradle(
            """
              ext['jackson-bom.version'] = '99.0.0'
              """
          )
        );
    }

    @Test
    void noChangeWhenExtVersionEqualsBomWithoutCleanup() {
        rewriteRun(
          //language=gradle
          buildGradle(
            """
              ext['jackson-bom.version'] = '2.18.1'
              """
          )
        );
    }

    @Test
    void removesExtPropertyWhenBomVersionIsHigherAndRemoveEnabled() {
        rewriteRun(
          spec -> spec.recipe(new SyncGradleExtPropertiesWithBom(
                  "org.springframework.boot",
                  "spring-boot-dependencies",
                  "3.4.0",
                  true
          )),
          //language=gradle
          buildGradle(
            """
              ext['jackson-bom.version'] = '2.14.0'
              """,
            """
              """
          )
        );
    }

    @Test
    void ignoresExtPropertyNotInBom() {
        rewriteRun(
          //language=gradle
          buildGradle(
            """
              ext['my.custom.property'] = '1.0.0'
              """
          )
        );
    }

    @Test
    void resolvesBomVersionSelector() {
        rewriteRun(
          spec -> spec.recipe(new SyncGradleExtPropertiesWithBom(
                  "org.springframework.boot",
                  "spring-boot-dependencies",
                  "3.4.x",
                  true
          )),
          //language=gradle
          buildGradle(
            """
              ext['jackson-bom.version'] = '2.14.0'
              """,
            """
              """
          )
        );
    }

    @Test
    void removesExtPropertyEqualToBomWhenRemoveEnabled() {
        rewriteRun(
          spec -> spec.recipe(new SyncGradleExtPropertiesWithBom(
                  "org.springframework.boot",
                  "spring-boot-dependencies",
                  "3.4.0",
                  true
          )),
          //language=gradle
          buildGradle(
            """
              ext['jackson-bom.version'] = '2.18.1'
              """,
            """
              """
          )
        );
    }

    @Test
    void removesExtPropertyReferringToGradleProperty() {
        rewriteRun(
          spec -> spec.recipe(new SyncGradleExtPropertiesWithBom(
                  "org.springframework.boot",
                  "spring-boot-dependencies",
                  "3.4.0",
                  true
          )),
          properties(
            """
              springVersion=6.0.13
              springDataBomVersion=2022.0.11
              """,
            spec -> spec.path("gradle.properties")
          ),
          //language=gradle
          buildGradle(
            """
              description = 'Spring Test Framework for Apache Geode'

              // Define dependency version overrides.
              ext['spring-framework.version'] = "$springVersion"
              ext['spring-data-bom.version'] = "${springDataBomVersion}"

              group = 'org.springframework.data'
              """,
            """
              description = 'Spring Test Framework for Apache Geode'

              group = 'org.springframework.data'
              """
          )
        );
    }

    @Test
    void removesExtPropertyReferringToRootGradlePropertyFromSubproject() {
        rewriteRun(
          spec -> spec.recipe(new SyncGradleExtPropertiesWithBom(
                  "org.springframework.boot",
                  "spring-boot-dependencies",
                  "3.4.0",
                  true
          )),
          properties(
            "springVersion=6.0.13",
            spec -> spec.path("gradle.properties")
          ),
          //language=gradle
          buildGradle(
            """
              ext.set('spring-framework.version', springVersion)
              """,
            """
              """,
            spec -> spec.path("samples/web/build.gradle")
          )
        );
    }

    @Test
    void keepsExtPropertyReferringToVariableShadowingGradleProperty() {
        rewriteRun(
          spec -> spec.recipe(new SyncGradleExtPropertiesWithBom(
                  "org.springframework.boot",
                  "spring-boot-dependencies",
                  "3.4.0",
                  true
          )),
          properties(
            """
              springVersion=6.0.13
              jacksonVersion=2.14.0
              """,
            spec -> spec.path("gradle.properties")
          ),
          //language=gradle
          buildGradle(
            """
              def springVersion = '99.0.0'
              ext {
                  jacksonVersion = '99.0.0'
              }
              ext['spring-framework.version'] = "$springVersion"
              ext['jackson-bom.version'] = "$jacksonVersion"
              """
          )
        );
    }

    @Test
    void keepsExtPropertyReferringToHigherGradleProperty() {
        rewriteRun(
          spec -> spec.recipe(new SyncGradleExtPropertiesWithBom(
                  "org.springframework.boot",
                  "spring-boot-dependencies",
                  "3.4.0",
                  true
          )),
          properties(
            "springVersion=99.0.0",
            spec -> spec.path("gradle.properties")
          ),
          //language=gradle
          buildGradle(
            """
              ext['spring-framework.version'] = "$springVersion"
              ext['jackson-bom.version'] = "$unknownVersion"
              """
          )
        );
    }

    @Test
    void removesOverridesTheBomMeetsAndKeepsOneAboveIt() {
        rewriteRun(
          spec -> spec.recipe(new SyncGradleExtPropertiesWithBom(
                  "org.springframework.boot",
                  "spring-boot-dependencies",
                  "3.4.0",
                  true
          )),
          //language=gradle
          buildGradle(
            """
              group = 'com.example'
              ext['tomcat.version'] = '10.1.20' // below what the BOM manages
              ext['jackson-bom.version'] = '2.18.1' // equal to it
              ext['spring-framework.version'] = '6.2.99' // above it
              version = '1.0'
              """,
            """
              group = 'com.example'
              ext['spring-framework.version'] = '6.2.99' // above it
              version = '1.0'
              """
          )
        );
    }

    @Test
    void removesOverrideBelowBomAcrossMajorVersion() {
        rewriteRun(
          spec -> spec.recipe(new SyncGradleExtPropertiesWithBom(
                  "org.springframework.boot",
                  "spring-boot-dependencies",
                  "4.0.x",
                  true
          )),
          //language=gradle
          buildGradle(
            """
              group = 'com.example'
              ext['tomcat.version'] = '10.1.59' // CVE fixed
              ext['spring-framework.version'] = '7.1.0-SNAPSHOT' // waiting on 7.1
              """,
            """
              group = 'com.example'
              ext['spring-framework.version'] = '7.1.0-SNAPSHOT' // waiting on 7.1
              """
          )
        );
    }

    @Test
    void removesCommentOnTheLineOfRemovedOverride() {
        rewriteRun(
          spec -> spec.recipe(new SyncGradleExtPropertiesWithBom(
                  "org.springframework.boot",
                  "spring-boot-dependencies",
                  "3.4.0",
                  true
          )),
          //language=gradle
          buildGradle(
            """
              group = 'com.example'
              ext['spring-framework.version'] = '6.0.13' // pinned for CVE
              ext {
                  // Jackson
                  set('jackson-bom.version', '2.14.0') /* pinned */ // until 2.15
              }
              ext.set('tomcat.version', '10.1.20') // last line
              """,
            """
              group = 'com.example'
              ext {
              }
              """
          )
        );
    }

    @Test
    void raisesRatherThanRemovesOverrideTheScriptReads() {
        rewriteRun(
          spec -> spec.recipe(new SyncGradleExtPropertiesWithBom(
                  "org.springframework.boot",
                  "spring-boot-dependencies",
                  "3.4.0",
                  true
          )),
          //language=gradle
          buildGradle(
            """
              ext['mockito.version'] = '4.11.0'
              ext['jackson-bom.version'] = '2.18.1'
              ext['tomcat.version'] = '10.1.20'
              def mockito = ext['mockito.version']
              println project.property('jackson-bom.version')
              println "Tomcat ${ext.get('tomcat.version')}"
              """,
            """
              ext['mockito.version'] = '5.14.2'
              ext['jackson-bom.version'] = '2.18.1'
              ext['tomcat.version'] = '10.1.33'
              def mockito = ext['mockito.version']
              println project.property('jackson-bom.version')
              println "Tomcat ${ext.get('tomcat.version')}"
              """
          )
        );
    }

    @Test
    void keepsOverrideReferringToGradlePropertyWhenTheScriptReadsIt() {
        rewriteRun(
          spec -> spec.recipe(new SyncGradleExtPropertiesWithBom(
                  "org.springframework.boot",
                  "spring-boot-dependencies",
                  "3.4.0",
                  true
          )),
          properties(
            "springVersion=6.0.13",
            spec -> spec.path("gradle.properties")
          ),
          //language=gradle
          buildGradle(
            """
              ext['spring-framework.version'] = "$springVersion"
              println project.property('spring-framework.version')
              """
          )
        );
    }

    @Test
    void keepsOverrideReadFromAnotherScript() {
        rewriteRun(
          spec -> spec.recipe(new SyncGradleExtPropertiesWithBom(
                  "org.springframework.boot",
                  "spring-boot-dependencies",
                  "3.4.0",
                  true
          )),
          //language=gradle
          buildGradle(
            """
              ext['jackson-bom.version'] = '2.18.1'
              ext['tomcat.version'] = '10.1.20'
              """,
            """
              ext['jackson-bom.version'] = '2.18.1'
              """
          ),
          //language=gradle
          buildGradle(
            """
              def jackson = rootProject.ext['jackson-bom.version']
              """,
            spec -> spec.path("samples/web/build.gradle")
          )
        );
    }

    @Test
    void kotlinDsl() {
        rewriteRun(
          spec -> spec.recipe(new SyncGradleExtPropertiesWithBom(
                  "org.springframework.boot",
                  "spring-boot-dependencies",
                  "3.4.0",
                  true
          )),
          //language=kotlin
          buildGradleKts(
            """
              group = "com.example" // the group
              extra["tomcat.version"] = "10.1.20" // pinned for CVE
              extra["mockito.version"] = "4.11.0"
              val mockito = extra["mockito.version"] // read
              extra["jackson-bom.version"] = "2.18.1" // last line
              """,
            """
              group = "com.example" // the group
              extra["mockito.version"] = "5.14.2"
              val mockito = extra["mockito.version"] // read
              """
          )
        );
    }

    @Test
    void keepsCommentOnTheLineBeforeRemovedOverride() {
        rewriteRun(
          spec -> spec.recipe(new SyncGradleExtPropertiesWithBom(
                  "org.springframework.boot",
                  "spring-boot-dependencies",
                  "3.4.0",
                  true
          )),
          //language=gradle
          buildGradle(
            """
              group = 'com.example' // the group
              ext['tomcat.version'] = '10.1.20' // below
              ext['jackson-bom.version'] = '2.18.1' // equal
              version = '1.0'
              ext { // pins
                  set('spring-framework.version', '6.2.99') // above
                  set('mockito.version', '4.11.0') // below
              }
              """,
            """
              group = 'com.example' // the group
              version = '1.0'
              ext { // pins
                  set('spring-framework.version', '6.2.99') // above
              }
              """
          )
        );
    }

    @Test
    void keepsOverrideReadAsQuotedProperty() {
        rewriteRun(
          spec -> spec.recipe(new SyncGradleExtPropertiesWithBom(
                  "org.springframework.boot",
                  "spring-boot-dependencies",
                  "3.4.0",
                  true
          )),
          //language=gradle
          buildGradle(
            """
              ext['tomcat.version'] = '10.1.20'
              ext['mockito.version'] = '4.11.0'
              println ext.'tomcat.version'
              println "Mockito ${project.ext."mockito.version"}"
              """,
            """
              ext['tomcat.version'] = '10.1.33'
              ext['mockito.version'] = '5.14.2'
              println ext.'tomcat.version'
              println "Mockito ${project.ext."mockito.version"}"
              """
          )
        );
    }

    @Test
    void keepsFileHeaderAboveRemovedFirstOverride() {
        rewriteRun(
          spec -> spec.recipe(new SyncGradleExtPropertiesWithBom(
                  "org.springframework.boot",
                  "spring-boot-dependencies",
                  "3.4.0",
                  true
          )),
          //language=gradle
          buildGradle(
            """
              /*
               * License
               */
              ext['tomcat.version'] = '10.1.20'
              group = 'com.example'
              """,
            """
              /*
               * License
               */
              group = 'com.example'
              """
          ),
          //language=kotlin
          buildGradleKts(
            """
              // License

              // pinned for CVE
              extra["tomcat.version"] = "10.1.20"
              group = "com.example"
              """,
            """
              // License

              group = "com.example"
              """
          )
        );
    }

    @Test
    void keepsCommentSetApartFromRemovedOverride() {
        rewriteRun(
          spec -> spec.recipe(new SyncGradleExtPropertiesWithBom(
                  "org.springframework.boot",
                  "spring-boot-dependencies",
                  "3.4.0",
                  true
          )),
          //language=gradle
          buildGradle(
            """
              group = 'com.example' // the group

              // Versions

              // pinned for CVE
              ext['tomcat.version'] = '10.1.20'
              version = '1.0'
              subprojects {
                  // Versions

                  // pinned for CVE
                  ext['mockito.version'] = '4.11.0'
              }
              """,
            """
              group = 'com.example' // the group

              // Versions

              version = '1.0'
              subprojects {
                  // Versions

              }
              """
          )
        );
    }
}
