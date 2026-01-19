/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class RemoveManagedDependencyTest implements RewriteTest {

    @DocumentExample
    @Test
    void removeManagedDependency() {
        rewriteRun(
          spec -> spec.recipe(new RemoveManagedDependency(
            "javax.activation",
            "javax.activation-api",
            null
          )),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>javax.activation</groupId>
                              <artifactId>javax.activation-api</artifactId>
                              <version>1.2.0</version>
                          </dependency>
                          <dependency>
                              <groupId>jakarta.activation</groupId>
                              <artifactId>jakarta.activation-api</artifactId>
                              <version>1.2.1</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>jakarta.activation</groupId>
                              <artifactId>jakarta.activation-api</artifactId>
                              <version>1.2.1</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void removeManagedDependencyWithScopeNone() {
        rewriteRun(
          spec -> spec.recipe(new RemoveManagedDependency(
            "javax.activation",
            "javax.activation-api",
            null
          )),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>javax.activation</groupId>
                              <artifactId>javax.activation-api</artifactId>
                              <version>1.2.0</version>
                              <scope>test</scope>
                          </dependency>
                          <dependency>
                              <groupId>jakarta.activation</groupId>
                              <artifactId>jakarta.activation-api</artifactId>
                              <version>1.2.1</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>jakarta.activation</groupId>
                              <artifactId>jakarta.activation-api</artifactId>
                              <version>1.2.1</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void removeManagedDependencyWithScopeMatching() {
        rewriteRun(
          spec -> spec.recipe(new RemoveManagedDependency(
            "javax.activation",
            "javax.activation-api",
            "test"
          )),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>javax.activation</groupId>
                              <artifactId>javax.activation-api</artifactId>
                              <version>1.2.0</version>
                              <scope>test</scope>
                          </dependency>
                          <dependency>
                              <groupId>jakarta.activation</groupId>
                              <artifactId>jakarta.activation-api</artifactId>
                              <version>1.2.1</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>jakarta.activation</groupId>
                              <artifactId>jakarta.activation-api</artifactId>
                              <version>1.2.1</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void removeManagedDependencyWithScopeNonMatching() {
        rewriteRun(
          spec -> spec.recipe(new RemoveManagedDependency(
            "javax.activation",
            "javax.activation-api",
            "compile"
          )),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>javax.activation</groupId>
                              <artifactId>javax.activation-api</artifactId>
                              <version>1.2.0</version>
                              <scope>test</scope>
                          </dependency>
                          <dependency>
                              <groupId>jakarta.activation</groupId>
                              <artifactId>jakarta.activation-api</artifactId>
                              <version>1.2.1</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void removeBomImport() {
        rewriteRun(
          spec -> spec.recipe(new RemoveManagedDependency(
            "io.micrometer",
            "micrometer-bom",
            null
          )),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>io.micrometer</groupId>
                              <artifactId>micrometer-bom</artifactId>
                              <version>1.9.9</version>
                              <scope>import</scope>
                              <type>bom</type>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
              </project>
              """
          )
        );
    }
}
