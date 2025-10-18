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
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class ReplaceModulesWithSubprojectsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceModulesWithSubprojects());
    }

    @DocumentExample
    @Test
    void replaceModulesWithSubprojects() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>
                  <modules>
                      <module>module-a</module>
                      <module>module-b</module>
                  </modules>
              </project>
              """,
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>
                  <subprojects>
                      <subproject>module-a</subproject>
                      <subproject>module-b</subproject>
                  </subprojects>
              </project>
              """
          )
        );
    }

    @Test
    void replaceSingleModule() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>
                  <modules>
                      <module>my-module</module>
                  </modules>
              </project>
              """,
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>
                  <subprojects>
                      <subproject>my-module</subproject>
                  </subprojects>
              </project>
              """
          )
        );
    }

    @Test
    void replaceManyModules() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>
                  <modules>
                      <module>core</module>
                      <module>api</module>
                      <module>impl</module>
                      <module>tests</module>
                  </modules>
              </project>
              """,
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>
                  <subprojects>
                      <subproject>core</subproject>
                      <subproject>api</subproject>
                      <subproject>impl</subproject>
                      <subproject>tests</subproject>
                  </subprojects>
              </project>
              """
          )
        );
    }

    @Test
    void replaceModulesWithRelativePaths() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>
                  <modules>
                      <module>../sibling-module</module>
                      <module>child/nested-module</module>
                  </modules>
              </project>
              """,
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>
                  <subprojects>
                      <subproject>../sibling-module</subproject>
                      <subproject>child/nested-module</subproject>
                  </subprojects>
              </project>
              """
          )
        );
    }

    @Test
    void noChangeWhenNoModules() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <packaging>jar</packaging>
              </project>
              """
          )
        );
    }

    @Test
    void preservesFormatting() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>

                  <!-- Multi-module configuration -->
                  <modules>
                      <!-- Core module -->
                      <module>core</module>
                      <!-- API module -->
                      <module>api</module>
                  </modules>
              </project>
              """,
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>

                  <!-- Multi-module configuration -->
                  <subprojects>
                      <!-- Core module -->
                      <subproject>core</subproject>
                      <!-- API module -->
                      <subproject>api</subproject>
                  </subprojects>
              </project>
              """
          )
        );
    }
}
