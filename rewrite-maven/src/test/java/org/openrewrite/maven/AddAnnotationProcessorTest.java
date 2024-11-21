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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class AddAnnotationProcessorTest implements RewriteTest {

    @DocumentExample
    @Test
    void addCommentToMavenDependency() {
        rewriteRun(
          spec -> spec.recipe(new AddAnnotationProcessor(
            "org.projectlombok",
            "lombok-mapstruct-binding"
          )),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
              
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
              
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.5.1</version>
                              <configuration>
                                  <source>9</source>
                                  <target>9</target>
                                  <annotationProcessorPaths>
                                      <path>
                                          <groupId>org.mapstruct</groupId>
                                          <artifactId>mapstruct-processor</artifactId>
                                          <version>${version.mapstruct}</version>
                                      </path>
                                      <path>
                                          <groupId>org.projectlombok</groupId>
                                          <artifactId>lombok</artifactId>
                                          <version>${version.lombok}</version>
                                      </path>
                                  </annotationProcessorPaths>
                              </configuration>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
              
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
              
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.5.1</version>
                              <configuration>
                                  <source>9</source>
                                  <target>9</target>
                                  <annotationProcessorPaths>
                                      <path>
                                          <groupId>org.mapstruct</groupId>
                                          <artifactId>mapstruct-processor</artifactId>
                                          <version>${version.mapstruct}</version>
                                      </path>
                                      <path>
                                          <groupId>org.projectlombok</groupId>
                                          <artifactId>lombok</artifactId>
                                          <version>${version.lombok}</version>
                                      </path>
                                      <path>
                                          <groupId>org.projectlombok</groupId>
                                          <artifactId>lombok-mapstruct-binding</artifactId>
                                          <version>${version.mapstruct-lombok}</version>
                                      </path>
                                  </annotationProcessorPaths>
                              </configuration>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );
    }
}
