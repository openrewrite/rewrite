/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.gradle.gradle8;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class JacocoReportDeprecationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new JacocoReportDeprecations());
    }

    @DocumentExample
    @Test
    void deprecationsInNormalSyntax() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml.enabled = false
                      csv.enabled = true
                      html.enabled = false

                      xml.destination = layout.buildDirectory.dir('jacocoXml')
                      csv.destination = layout.buildDirectory.dir('jacocoCsv')
                      html.destination = layout.buildDirectory.dir('jacocoHtml')
                  }
              }
              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml.required = false
                      csv.required = true
                      html.required = false

                      xml.outputLocation = layout.buildDirectory.dir('jacocoXml')
                      csv.outputLocation = layout.buildDirectory.dir('jacocoCsv')
                      html.outputLocation = layout.buildDirectory.dir('jacocoHtml')
                  }
              }
              """
          )
        );
    }

    @Test
    void enabledDeprecatedInCollapsedSyntax() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport.reports.xml.enabled = false
              jacocoTestReport.reports.csv.enabled = true
              jacocoTestReport.reports.html.enabled = false

              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport.reports.xml.required = false
              jacocoTestReport.reports.csv.required = true
              jacocoTestReport.reports.html.required = false

              """
          )
        );
    }

    @Test
    void enabledDeprecatedInElapsedSyntax() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml {
                          enabled = false
                      }
                      csv {
                          enabled = false
                      }
                      html {
                          enabled = false
                      }
                  }
              }

              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml {
                          required = false
                      }
                      csv {
                          required = false
                      }
                      html {
                          required = false
                      }
                  }
              }

              """
          )
        );
    }

    @Test
    void enabledDeprecatedInMixedSyntax() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml.enabled = false
                      csv.enabled = false
                      html {
                          enabled = false
                      }
                  }
              }

              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml.required = false
                      csv.required = false
                      html {
                          required = false
                      }
                  }
              }

              """
          )
        );
    }

    @Test
    void enabledDeprecatedInSemiCollapsedSyntax() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports.xml.enabled = false
                  reports.csv.enabled = false
                  reports.html.enabled = false
              }

              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports.xml.required = false
                  reports.csv.required = false
                  reports.html.required = false
              }

              """
          )
        );
    }

    @Test
    void enabledInAnotherExtensionNotTouched() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }

              tasks.register("example", JavaCompile) {
                   xml.enabled = false
                   jacocoTestReport.reports.html.enabled = false
              }

              """
          )
        );
    }

    @Test
    void destinationDeprecatedInCollapsedSyntax() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport.reports.xml.destination = layout.buildDirectory.dir('jacocoXml')
              jacocoTestReport.reports.csv.destination = layout.buildDirectory.dir('jacocoCsv')
              jacocoTestReport.reports.html.destination = layout.buildDirectory.dir('jacocoHtml')

              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport.reports.xml.outputLocation = layout.buildDirectory.dir('jacocoXml')
              jacocoTestReport.reports.csv.outputLocation = layout.buildDirectory.dir('jacocoCsv')
              jacocoTestReport.reports.html.outputLocation = layout.buildDirectory.dir('jacocoHtml')

              """
          )
        );
    }

    @Test
    void destinationDeprecatedInElapsedSyntax() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml {
                          destination = layout.buildDirectory.dir('jacocoXml')
                      }
                      csv {
                          destination = layout.buildDirectory.dir('jacocoCsv')
                      }
                      html {
                          destination = layout.buildDirectory.dir('jacocoHtml')
                      }
                  }
              }
              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml {
                          outputLocation = layout.buildDirectory.dir('jacocoXml')
                      }
                      csv {
                          outputLocation = layout.buildDirectory.dir('jacocoCsv')
                      }
                      html {
                          outputLocation = layout.buildDirectory.dir('jacocoHtml')
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void destinationDeprecatedInMixedSyntax() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml.destination = layout.buildDirectory.dir('jacocoXml')
                      csv.destination = layout.buildDirectory.dir('jacocoCsv')
                      html {
                          destination = layout.buildDirectory.dir('jacocoHtml')
                      }
                  }
              }

              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml.outputLocation = layout.buildDirectory.dir('jacocoXml')
                      csv.outputLocation = layout.buildDirectory.dir('jacocoCsv')
                      html {
                          outputLocation = layout.buildDirectory.dir('jacocoHtml')
                      }
                  }
              }

              """
          )
        );
    }

    @Test
    void destinationDeprecatedInSemiCollapsedSyntax() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports.xml.destination = layout.buildDirectory.dir('jacocoXml')
                  reports.csv.destination = layout.buildDirectory.dir('jacocoCsv')
                  reports.html.destination = layout.buildDirectory.dir('jacocoHtml')
              }

              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports.xml.outputLocation = layout.buildDirectory.dir('jacocoXml')
                  reports.csv.outputLocation = layout.buildDirectory.dir('jacocoCsv')
                  reports.html.outputLocation = layout.buildDirectory.dir('jacocoHtml')
              }

              """
          )
        );
    }

    @Test
    void destinationInAnotherExtensionNotTouched() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }

              tasks.register("example", JavaCompile) {
                   xml.destination = false
                   jacocoTestReport.reports.html.destination = layout.buildDirectory.dir('jacocoHtml')
              }

              """
          )
        );
    }

    @Test
    void alsoWorkOnKotlinDsl() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  java
                  jacoco
              }

              tasks.jacocoTestReport {
                  reports {
                      xml.isEnabled = false
                      html.isEnabled = true
                      html.destination = layout.buildDirectory.dir("jacocoHtml")
                  }
              }
              """,
            """
              plugins {
                  java
                  jacoco
              }

              tasks.jacocoTestReport {
                  reports {
                      xml.required = false
                      html.required = true
                      html.outputLocation = layout.buildDirectory.dir("jacocoHtml")
                  }
              }
              """
          )
        );
    }
}
