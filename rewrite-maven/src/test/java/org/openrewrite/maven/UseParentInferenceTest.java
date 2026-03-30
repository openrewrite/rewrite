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

class UseParentInferenceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseParentInference());
    }

    @DocumentExample
    @Test
    void useParentInferenceWithDefaultRelativePath() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>
              </project>
              """,
            spec -> spec.path("pom.xml")
          ),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0.0</version>
                      <relativePath>..</relativePath>
                  </parent>
                  <artifactId>child</artifactId>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent/>
                  <artifactId>child</artifactId>
              </project>
              """,
            spec -> spec.path("child/pom.xml")
          )
        );
    }

    @Test
    void noChangeWithImplicitRelativePath() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>
              </project>
              """,
            spec -> spec.path("pom.xml")
          ),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0.0</version>
                  </parent>
                  <artifactId>child</artifactId>
              </project>
              """,
            spec -> spec.path("child/pom.xml")
          )
        );
    }

    @Test
    void preserveNonDefaultRelativePath() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>
              </project>
              """,
            spec -> spec.path("parent/pom.xml")
          ),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0.0</version>
                      <relativePath>../../parent</relativePath>
                  </parent>
                  <artifactId>child</artifactId>
              </project>
              """,
            spec -> spec.path("modules/child/pom.xml")
          )
        );
    }

    @Test
    void preserveEmptyRelativePath() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>3.0.0</version>
                      <relativePath/>
                  </parent>
                  <artifactId>child</artifactId>
              </project>
              """
          )
        );
    }

    @Test
    void noChangeWhenNoParent() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
              </project>
              """
          )
        );
    }

    @Test
    void useParentInferenceWithOnlyGroupIdAndArtifactId() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>
              </project>
              """,
            spec -> spec.path("pom.xml")
          ),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0.0</version>
                      <relativePath>..</relativePath>
                  </parent>
                  <artifactId>child</artifactId>
                  <version>2.0.0</version>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent/>
                  <artifactId>child</artifactId>
                  <version>2.0.0</version>
              </project>
              """,
            spec -> spec.path("child/pom.xml")
          )
        );
    }

    @Test
    void useParentInferenceWithComments() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>
              </project>
              """,
            spec -> spec.path("pom.xml")
          ),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <!-- Parent POM -->
                  <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0.0</version>
                      <relativePath>..</relativePath>
                  </parent>
                  <artifactId>child</artifactId>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <!-- Parent POM -->
                  <parent/>
                  <artifactId>child</artifactId>
              </project>
              """,
            spec -> spec.path("child/pom.xml")
          )
        );
    }
}
