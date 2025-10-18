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

class MigrateToMaven4Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/maven.yml", "org.openrewrite.maven.MigrateToMaven4");
    }

    @DocumentExample
    @Test
    void comprehensiveMigration() {
        rewriteRun(
          pomXml(
            """
              <project xmlns="http://maven.apache.org/POM/4.0.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>

                  <modules>
                      <module>child-a</module>
                      <module>child-b</module>
                  </modules>

                  <properties>
                      <project.root>${executionRootDirectory}</project.root>
                      <multi.root>${multiModuleProjectDirectory}</multi.root>
                      <my.version>${version}</my.version>
                      <my.basedir>${basedir}</my.basedir>
                  </properties>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.8.1</version>
                          </plugin>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.8.1</version>
                          </plugin>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-antrun-plugin</artifactId>
                              <executions>
                                  <execution>
                                      <id>pre-clean-task</id>
                                      <phase>pre-clean</phase>
                                      <goals>
                                          <goal>run</goal>
                                      </goals>
                                  </execution>
                                  <execution>
                                      <id>pre-integration-test-task</id>
                                      <phase>pre-integration-test</phase>
                                      <goals>
                                          <goal>run</goal>
                                      </goals>
                                  </execution>
                                  <execution>
                                      <id>post-integration-test-task</id>
                                      <phase>post-integration-test</phase>
                                      <goals>
                                          <goal>run</goal>
                                      </goals>
                                  </execution>
                              </executions>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project xmlns="http://maven.apache.org/POM/4.1.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.1.0 http://maven.apache.org/xsd/maven-4.1.0.xsd">
                  <modelVersion>4.1.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>

                  <subprojects>
                      <subproject>child-a</subproject>
                      <subproject>child-b</subproject>
                  </subprojects>

                  <properties>
                      <project.root>${session.rootDirectory}</project.root>
                      <multi.root>${project.rootDirectory}</multi.root>
                      <my.version>${project.version}</my.version>
                      <my.basedir>${project.basedir}</my.basedir>
                  </properties>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.8.1</version>
                          </plugin>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-antrun-plugin</artifactId>
                              <executions>
                                  <execution>
                                      <id>pre-clean-task</id>
                                      <phase>before:clean</phase>
                                      <goals>
                                          <goal>run</goal>
                                      </goals>
                                  </execution>
                                  <execution>
                                      <id>pre-integration-test-task</id>
                                      <phase>before:integration-test</phase>
                                      <goals>
                                          <goal>run</goal>
                                      </goals>
                                  </execution>
                                  <execution>
                                      <id>post-integration-test-task</id>
                                      <phase>after:integration-test</phase>
                                      <goals>
                                          <goal>run</goal>
                                      </goals>
                                  </execution>
                              </executions>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );
    }

    @Test
    void migrateAllLifecyclePhases() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-antrun-plugin</artifactId>
                              <executions>
                                  <execution>
                                      <id>pre-clean</id>
                                      <phase>pre-clean</phase>
                                      <goals><goal>run</goal></goals>
                                  </execution>
                                  <execution>
                                      <id>post-clean</id>
                                      <phase>post-clean</phase>
                                      <goals><goal>run</goal></goals>
                                  </execution>
                                  <execution>
                                      <id>pre-site</id>
                                      <phase>pre-site</phase>
                                      <goals><goal>run</goal></goals>
                                  </execution>
                                  <execution>
                                      <id>post-site</id>
                                      <phase>post-site</phase>
                                      <goals><goal>run</goal></goals>
                                  </execution>
                              </executions>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.1.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-antrun-plugin</artifactId>
                              <executions>
                                  <execution>
                                      <id>pre-clean</id>
                                      <phase>before:clean</phase>
                                      <goals><goal>run</goal></goals>
                                  </execution>
                                  <execution>
                                      <id>post-clean</id>
                                      <phase>after:clean</phase>
                                      <goals><goal>run</goal></goals>
                                  </execution>
                                  <execution>
                                      <id>pre-site</id>
                                      <phase>before:site</phase>
                                      <goals><goal>run</goal></goals>
                                  </execution>
                                  <execution>
                                      <id>post-site</id>
                                      <phase>after:site</phase>
                                      <goals><goal>run</goal></goals>
                                  </execution>
                              </executions>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );
    }

    @Test
    void noChangesWhenAlreadyMaven4() {
        rewriteRun(
          pomXml(
            """
              <project xmlns="http://maven.apache.org/POM/4.1.0">
                  <modelVersion>4.1.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>

                  <properties>
                      <app.root>${session.rootDirectory}</app.root>
                      <my.version>${project.version}</my.version>
                  </properties>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.8.1</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );
    }
}
