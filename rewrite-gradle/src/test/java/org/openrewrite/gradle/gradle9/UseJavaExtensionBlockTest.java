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
package org.openrewrite.gradle.gradle9;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class UseJavaExtensionBlockTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseJavaExtensionBlock());
    }

    @DocumentExample
    @Test
    void wrapsAndNormalizesBothCompatibilityAssignments() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              sourceCompatibility = '17'
              targetCompatibility = '17'
              """,
            """
              plugins {
                  id 'java'
              }

              java {
                  sourceCompatibility = JavaVersion.VERSION_17
                  targetCompatibility = JavaVersion.VERSION_17
              }
              """
          )
        );
    }

    @Test
    void mirrorsTargetWhenOnlySourceIsDeclared() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              sourceCompatibility = '17'
              """,
            """
              plugins {
                  id 'java'
              }

              java {
                  sourceCompatibility = JavaVersion.VERSION_17
                  targetCompatibility = JavaVersion.VERSION_17
              }
              """
          )
        );
    }

    @Test
    void mirrorsSourceWhenOnlyTargetIsDeclared() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              targetCompatibility = '17'
              """,
            """
              plugins {
                  id 'java'
              }

              java {
                  sourceCompatibility = JavaVersion.VERSION_17
                  targetCompatibility = JavaVersion.VERSION_17
              }
              """
          )
        );
    }

    @Test
    void doesNotMirrorWhenCounterpartAlreadyInJavaBlock() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              sourceCompatibility = '17'

              java {
                  targetCompatibility = '11'
              }
              """,
            """
              plugins {
                  id 'java'
              }

              java {
                  targetCompatibility = '11'
                  sourceCompatibility = JavaVersion.VERSION_17
              }
              """
          )
        );
    }

    @Test
    void mergesIntoExistingJavaBlock() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              sourceCompatibility = '17'
              targetCompatibility = '17'

              java {
                  withSourcesJar()
              }
              """,
            """
              plugins {
                  id 'java'
              }

              java {
                  withSourcesJar()
                  sourceCompatibility = JavaVersion.VERSION_17
                  targetCompatibility = JavaVersion.VERSION_17
              }
              """
          )
        );
    }

    @Test
    void keepsExistingValueWhenAlsoSetAtTopLevel() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              java {
                  sourceCompatibility = '11'
              }

              sourceCompatibility = '17'
              """,
            """
              plugins {
                  id 'java'
              }

              java {
                  sourceCompatibility = '11'
                  targetCompatibility = JavaVersion.VERSION_17
              }
              """
          )
        );
    }

    @Test
    void normalizesEnumValuesUnchanged() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              sourceCompatibility = JavaVersion.VERSION_17
              targetCompatibility = JavaVersion.VERSION_17
              """,
            """
              plugins {
                  id 'java'
              }

              java {
                  sourceCompatibility = JavaVersion.VERSION_17
                  targetCompatibility = JavaVersion.VERSION_17
              }
              """
          )
        );
    }

    @Test
    void normalizesNumericValues() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              sourceCompatibility = 17
              targetCompatibility = 17
              """,
            """
              plugins {
                  id 'java'
              }

              java {
                  sourceCompatibility = JavaVersion.VERSION_17
                  targetCompatibility = JavaVersion.VERSION_17
              }
              """
          )
        );
    }

    @Test
    void normalizesJava8DoubleValues() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              sourceCompatibility = 1.8
              """,
            """
              plugins {
                  id 'java'
              }

              java {
                  sourceCompatibility = JavaVersion.VERSION_1_8
                  targetCompatibility = JavaVersion.VERSION_1_8
              }
              """
          )
        );
    }

    @Test
    void preservesUnresolvableVariableReference() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              ext {
                  javaVersion = JavaVersion.VERSION_17
              }

              sourceCompatibility = javaVersion
              """,
            """
              plugins {
                  id 'java'
              }

              ext {
                  javaVersion = JavaVersion.VERSION_17
              }

              java {
                  sourceCompatibility = javaVersion
                  targetCompatibility = javaVersion
              }
              """
          )
        );
    }

    @Nested
    class InsideSubprojectsBlock {
        @Test
        void movesBothCompatibilityIntoJavaBlock() {
            rewriteRun(
              buildGradle(
                """
                  subprojects {
                      sourceCompatibility = 11
                      targetCompatibility = 11
                  }
                  """,
                """
                  subprojects {
                      java {
                          sourceCompatibility = JavaVersion.VERSION_11
                          targetCompatibility = JavaVersion.VERSION_11
                      }
                  }
                  """
              )
            );
        }

        @Test
        void mirrorsWhenOnlySourceDeclaredInSubprojects() {
            rewriteRun(
              buildGradle(
                """
                  subprojects {
                      sourceCompatibility = 11
                  }
                  """,
                """
                  subprojects {
                      java {
                          sourceCompatibility = JavaVersion.VERSION_11
                          targetCompatibility = JavaVersion.VERSION_11
                      }
                  }
                  """
              )
            );
        }

        @Test
        void mergesIntoExistingJavaBlockInSubprojects() {
            rewriteRun(
              buildGradle(
                """
                  subprojects {
                      sourceCompatibility = 11
                      targetCompatibility = 11
                      java {
                          withSourcesJar()
                      }
                  }
                  """,
                """
                  subprojects {
                      java {
                          withSourcesJar()
                          sourceCompatibility = JavaVersion.VERSION_11
                          targetCompatibility = JavaVersion.VERSION_11
                      }
                  }
                  """
              )
            );
        }

        @Test
        void leavesSurroundingStatementsUntouched() {
            rewriteRun(
              buildGradle(
                """
                  subprojects {
                      apply plugin: 'java'

                      sourceCompatibility = 11
                      targetCompatibility = 11

                      repositories {
                          mavenCentral()
                      }
                  }
                  """,
                """
                  subprojects {
                      apply plugin: 'java'

                      repositories {
                          mavenCentral()
                      }

                      java {
                          sourceCompatibility = JavaVersion.VERSION_11
                          targetCompatibility = JavaVersion.VERSION_11
                      }
                  }
                  """
              )
            );
        }

        @Test
        void handlesAllprojectsBlock() {
            rewriteRun(
              buildGradle(
                """
                  allprojects {
                      sourceCompatibility = 11
                      targetCompatibility = 11
                  }
                  """,
                """
                  allprojects {
                      java {
                          sourceCompatibility = JavaVersion.VERSION_11
                          targetCompatibility = JavaVersion.VERSION_11
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class NoChange {
        @Test
        void alreadyInJavaBlock() {
            rewriteRun(
              buildGradle(
                """
                  plugins {
                      id 'java'
                  }

                  java {
                      sourceCompatibility = '17'
                      targetCompatibility = '17'
                  }
                  """
              )
            );
        }

        @Test
        void noCompatibilityProperties() {
            rewriteRun(
              buildGradle(
                """
                  plugins {
                      id 'java'
                  }

                  repositories {
                      mavenCentral()
                  }
                  """
              )
            );
        }

        @Test
        void kotlinDsl() {
            rewriteRun(
              buildGradleKts(
                """
                  plugins {
                      `java`
                  }

                  java {
                      sourceCompatibility = JavaVersion.VERSION_17
                      targetCompatibility = JavaVersion.VERSION_17
                  }
                  """
              )
            );
        }
    }
}
