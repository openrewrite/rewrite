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
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class ChangeExclusionTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/2933")
    @DocumentExample
    @Test
    void changeExclusionsWithGlob() {
        rewriteRun(
          spec -> spec.recipe(new ChangeExclusion(
            "org.springframework",
            "spring-web*",
            "org.springframework.boot",
            "spring-boot-starter-web"
          )),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-databind</artifactId>
                              <version>2.13.0</version>
                              <exclusions>
                                  <exclusion>
                                      <groupId>org.springframework</groupId>
                                      <artifactId>spring-web</artifactId>
                                  </exclusion>
                                  <exclusion>
                                      <groupId>commons-logging</groupId>
                                      <artifactId>commons-logging</artifactId>
                                  </exclusion>
                              </exclusions>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-databind</artifactId>
                              <version>2.13.0</version>
                              <exclusions>
                                  <exclusion>
                                      <groupId>org.springframework.boot</groupId>
                                      <artifactId>spring-boot-starter-web</artifactId>
                                  </exclusion>
                                  <exclusion>
                                      <groupId>commons-logging</groupId>
                                      <artifactId>commons-logging</artifactId>
                                  </exclusion>
                              </exclusions>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void changeExclusionGroupIdOnly() {
        rewriteRun(
          spec -> spec.recipe(new ChangeExclusion(
            "org.springframework",
            "spring-core",
            "org.springframework.boot",
            null
          )),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-databind</artifactId>
                              <version>2.13.0</version>
                              <exclusions>
                                  <exclusion>
                                      <groupId>org.springframework</groupId>
                                      <artifactId>spring-core</artifactId>
                                  </exclusion>
                              </exclusions>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-databind</artifactId>
                              <version>2.13.0</version>
                              <exclusions>
                                  <exclusion>
                                      <groupId>org.springframework.boot</groupId>
                                      <artifactId>spring-core</artifactId>
                                  </exclusion>
                              </exclusions>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void changeExclusionArtifactIdOnly() {
        rewriteRun(
          spec -> spec.recipe(new ChangeExclusion(
            "org.springframework",
            "spring-core",
            null,
            "spring-context"
          )),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-databind</artifactId>
                              <version>2.13.0</version>
                              <exclusions>
                                  <exclusion>
                                      <groupId>org.springframework</groupId>
                                      <artifactId>spring-core</artifactId>
                                  </exclusion>
                              </exclusions>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-databind</artifactId>
                              <version>2.13.0</version>
                              <exclusions>
                                  <exclusion>
                                      <groupId>org.springframework</groupId>
                                      <artifactId>spring-context</artifactId>
                                  </exclusion>
                              </exclusions>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void changeExclusionBothGroupIdAndArtifactId() {
        rewriteRun(
          spec -> spec.recipe(new ChangeExclusion(
            "javax.servlet",
            "servlet-api",
            "jakarta.servlet",
            "jakarta.servlet-api"
          )),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-databind</artifactId>
                              <version>2.13.0</version>
                              <exclusions>
                                  <exclusion>
                                      <groupId>javax.servlet</groupId>
                                      <artifactId>servlet-api</artifactId>
                                  </exclusion>
                              </exclusions>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-databind</artifactId>
                              <version>2.13.0</version>
                              <exclusions>
                                  <exclusion>
                                      <groupId>jakarta.servlet</groupId>
                                      <artifactId>jakarta.servlet-api</artifactId>
                                  </exclusion>
                              </exclusions>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void doesNotChangeNonMatchingExclusions() {
        rewriteRun(
          spec -> spec.recipe(new ChangeExclusion(
            "org.springframework",
            "spring-core",
            "org.springframework.boot",
            "spring-boot-starter"
          )),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-databind</artifactId>
                              <version>2.13.0</version>
                              <exclusions>
                                  <exclusion>
                                      <groupId>org.springframework</groupId>
                                      <artifactId>spring-beans</artifactId>
                                  </exclusion>
                                  <exclusion>
                                      <groupId>commons-logging</groupId>
                                      <artifactId>commons-logging</artifactId>
                                  </exclusion>
                              </exclusions>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void changeMultipleMatchingExclusions() {
        rewriteRun(
          spec -> spec.recipe(new ChangeExclusion(
            "org.springframework",
            "spring-*",
            "org.springframework.boot",
            null
          )),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-databind</artifactId>
                              <version>2.13.0</version>
                              <exclusions>
                                  <exclusion>
                                      <groupId>org.springframework</groupId>
                                      <artifactId>spring-core</artifactId>
                                  </exclusion>
                                  <exclusion>
                                      <groupId>org.springframework</groupId>
                                      <artifactId>spring-beans</artifactId>
                                  </exclusion>
                                  <exclusion>
                                      <groupId>commons-logging</groupId>
                                      <artifactId>commons-logging</artifactId>
                                  </exclusion>
                              </exclusions>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-databind</artifactId>
                              <version>2.13.0</version>
                              <exclusions>
                                  <exclusion>
                                      <groupId>org.springframework.boot</groupId>
                                      <artifactId>spring-core</artifactId>
                                  </exclusion>
                                  <exclusion>
                                      <groupId>org.springframework.boot</groupId>
                                      <artifactId>spring-beans</artifactId>
                                  </exclusion>
                                  <exclusion>
                                      <groupId>commons-logging</groupId>
                                      <artifactId>commons-logging</artifactId>
                                  </exclusion>
                              </exclusions>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }
}
