/*
 * Copyright 2024 the original author or authors.
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


@SuppressWarnings("GroovyAssignabilityCheck")
class DependencyConstraintToRuleTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new DependencyConstraintToRule());
    }

    @DocumentExample
    @Test
    void newResolutionStrategyBlock() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              dependencies {
                  constraints {
                      implementation('com.fasterxml.jackson.core:jackson-core:2.12.5') {
                          because 'CVE-2024-BAD'
                      }
                  }
                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-core') {
                          details.useVersion('2.12.5')
                          details.because('CVE-2024-BAD')
                      }
                  }
              }
              dependencies {
                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """
          )
        );
    }

    @Test
    void buildscript() {
        rewriteRun(
          buildGradle(
            """
              buildscript {
                  repositories { mavenCentral() }
                  dependencies {
                      constraints {
                          implementation('com.fasterxml.jackson.core:jackson-core:2.12.5') {
                              because 'CVE-2024-BAD'
                          }
                      }
                      classpath 'org.openrewrite:rewrite-java:7.0.0'
                  }
              }
              """
          )
        );
    }

    @Test
    void updateExistingResolutionStrategyBlock() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              configurations.all {
                    resolutionStrategy.eachDependency { d ->
                        if (d.requested.group == 'com.whatever' && d.requested.name == 'doesntmatter') {
                            d.useVersion('1.2.3')
                        } else {
                            // foo
                        }
                    }
              }
              dependencies {
                  constraints {
                      implementation('com.fasterxml.jackson.core:jackson-core:2.12.5')
                  }
                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              configurations.all {
                    resolutionStrategy.eachDependency { d ->
                        if (d.requested.group == 'com.whatever' && d.requested.name == 'doesntmatter') {
                            d.useVersion('1.2.3')
                        } else if (d.requested.group == 'com.fasterxml.jackson.core' && d.requested.name == 'jackson-core') {
                            d.useVersion('2.12.5')
                        } else {
                            // foo
                        }
                    }
              }
              dependencies {
                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """
          )
        );
    }

    @Test
    void leaveDifficultConstraintAlone() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              dependencies {
                  constraints {
                      implementation('com.fasterxml.jackson.core:jackson-core') {
                          // This is hard to convert to a resolution strategy block so better to do nothing
                          version {
                                strictly '[2.12.4, 2.12.5['
                                reject '2.12.3'
                          }
                      }
                      implementation('org.yaml:snakeyaml:2.2')
                  }
                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'org.yaml' && details.requested.name == 'snakeyaml') {
                          details.useVersion('2.2')
                      }
                  }
              }
              dependencies {
                  constraints {
                      implementation('com.fasterxml.jackson.core:jackson-core') {
                          // This is hard to convert to a resolution strategy block so better to do nothing
                          version {
                                strictly '[2.12.4, 2.12.5['
                                reject '2.12.3'
                          }
                      }
                  }
                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """
          )
        );
    }
}
