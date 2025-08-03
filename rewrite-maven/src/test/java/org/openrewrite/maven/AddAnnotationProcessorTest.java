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
    void addAnnotationProcessor() {
        rewriteRun(
          spec -> spec.recipe(new AddAnnotationProcessor(
            "org.projectlombok",
            "lombok-mapstruct-binding",
            "0.2.0"
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
                              <artifactId>maven-compiler-plugin</artifactId>
                              <configuration>
                                  <annotationProcessorPaths>
                                      <path>
                                          <groupId>org.mapstruct</groupId>
                                          <artifactId>mapstruct-processor</artifactId>
                                      </path>
                                      <path>
                                          <groupId>org.projectlombok</groupId>
                                          <artifactId>lombok</artifactId>
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
                              <artifactId>maven-compiler-plugin</artifactId>
                              <configuration>
                                  <annotationProcessorPaths>
                                      <path>
                                          <groupId>org.mapstruct</groupId>
                                          <artifactId>mapstruct-processor</artifactId>
                                      </path>
                                      <path>
                                          <groupId>org.projectlombok</groupId>
                                          <artifactId>lombok</artifactId>
                                      </path>
                                      <path>
                                          <groupId>org.projectlombok</groupId>
                                          <artifactId>lombok-mapstruct-binding</artifactId>
                                          <version>0.2.0</version>
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

    @Test
    void shouldUpdateProcessorVersionAlreadyPresent() {
        rewriteRun(
          spec -> spec.recipe(new AddAnnotationProcessor(
            "org.projectlombok",
            "lombok-mapstruct-binding",
            "0.2.0"
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
                              <configuration>
                                  <annotationProcessorPaths>
                                      <path>
                                          <groupId>org.mapstruct</groupId>
                                          <artifactId>mapstruct-processor</artifactId>
                                      </path>
                                      <path>
                                          <groupId>org.projectlombok</groupId>
                                          <artifactId>lombok</artifactId>
                                      </path>
                                      <path>
                                          <groupId>org.projectlombok</groupId>
                                          <artifactId>lombok-mapstruct-binding</artifactId>
                                          <version>0.1.0</version>
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
                              <configuration>
                                  <annotationProcessorPaths>
                                      <path>
                                          <groupId>org.mapstruct</groupId>
                                          <artifactId>mapstruct-processor</artifactId>
                                      </path>
                                      <path>
                                          <groupId>org.projectlombok</groupId>
                                          <artifactId>lombok</artifactId>
                                      </path>
                                      <path>
                                          <groupId>org.projectlombok</groupId>
                                          <artifactId>lombok-mapstruct-binding</artifactId>
                                          <version>0.2.0</version>
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

    @Test
    void addAnnotationWithOlderVersionAsMavenProperty() {
        rewriteRun(
          spec -> spec.recipe(new AddAnnotationProcessor(
            "org.projectlombok",
            "lombok-mapstruct-binding",
            "0.2.0"
          )),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                      <version.lombok.mapstruct.binding>0.1.0</version.lombok.mapstruct.binding>
                  </properties>
                  <build>
                      <plugins>
                          <plugin>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <configuration>
                                  <annotationProcessorPaths>
                                      <path>
                                          <groupId>org.projectlombok</groupId>
                                          <artifactId>lombok-mapstruct-binding</artifactId>
                                          <version>${version.lombok.mapstruct.binding}</version>
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
                  <properties>
                      <version.lombok.mapstruct.binding>0.2.0</version.lombok.mapstruct.binding>
                  </properties>
                  <build>
                      <plugins>
                          <plugin>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <configuration>
                                  <annotationProcessorPaths>
                                      <path>
                                          <groupId>org.projectlombok</groupId>
                                          <artifactId>lombok-mapstruct-binding</artifactId>
                                          <version>${version.lombok.mapstruct.binding}</version>
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

    @Test
    void addAnnotationWithSameVersionAsMavenProperty() {
        rewriteRun(
          spec -> spec.recipe(new AddAnnotationProcessor(
            "org.projectlombok",
            "lombok-mapstruct-binding",
            "0.2.0"
          )),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                      <version.lombok.mapstruct.binding>0.2.0</version.lombok.mapstruct.binding>
                  </properties>
                  <build>
                      <plugins>
                          <plugin>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <configuration>
                                  <annotationProcessorPaths>
                                      <path>
                                          <groupId>org.projectlombok</groupId>
                                          <artifactId>lombok-mapstruct-binding</artifactId>
                                          <version>${version.lombok.mapstruct.binding}</version>
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
