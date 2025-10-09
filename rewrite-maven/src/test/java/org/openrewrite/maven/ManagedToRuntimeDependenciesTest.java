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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class ManagedToRuntimeDependenciesTest implements RewriteTest {

    @DocumentExample
    @Test
    void convertNonImportManagedDependencies() {
        rewriteRun(
          spec -> spec.recipe(new ManagedToRuntimeDependencies()),
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
                              <groupId>com.fasterxml.jackson</groupId>
                              <artifactId>jackson-bom</artifactId>
                              <version>2.15.2</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                          <dependency>
                              <groupId>org.slf4j</groupId>
                              <artifactId>slf4j-api</artifactId>
                              <version>1.7.36</version>
                          </dependency>
                          <dependency>
                              <groupId>commons-lang</groupId>
                              <artifactId>commons-lang</artifactId>
                              <version>2.6</version>
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
                              <groupId>com.fasterxml.jackson</groupId>
                              <artifactId>jackson-bom</artifactId>
                              <version>2.15.2</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.slf4j</groupId>
                          <artifactId>slf4j-api</artifactId>
                          <version>1.7.36</version>
                          <scope>runtime</scope>
                      </dependency>
                      <dependency>
                          <groupId>commons-lang</groupId>
                          <artifactId>commons-lang</artifactId>
                          <version>2.6</version>
                          <scope>runtime</scope>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void scopeMapping() {
        rewriteRun(
          spec -> spec.recipe(new ManagedToRuntimeDependencies()),
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
                              <groupId>junit</groupId>
                              <artifactId>junit</artifactId>
                              <version>4.12</version>
                              <scope>test</scope>
                          </dependency>
                          <dependency>
                              <groupId>javax.servlet</groupId>
                              <artifactId>servlet-api</artifactId>
                              <version>2.5</version>
                              <scope>provided</scope>
                          </dependency>
                          <dependency>
                              <groupId>org.slf4j</groupId>
                              <artifactId>slf4j-api</artifactId>
                              <version>1.7.36</version>
                              <scope>compile</scope>
                          </dependency>
                          <dependency>
                              <groupId>commons-logging</groupId>
                              <artifactId>commons-logging</artifactId>
                              <version>1.2</version>
                              <scope>runtime</scope>
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
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>junit</groupId>
                          <artifactId>junit</artifactId>
                          <version>4.12</version>
                          <scope>test</scope>
                      </dependency>
                      <dependency>
                          <groupId>javax.servlet</groupId>
                          <artifactId>servlet-api</artifactId>
                          <version>2.5</version>
                          <scope>provided</scope>
                      </dependency>
                      <dependency>
                          <groupId>org.slf4j</groupId>
                          <artifactId>slf4j-api</artifactId>
                          <version>1.7.36</version>
                          <scope>runtime</scope>
                      </dependency>
                      <dependency>
                          <groupId>commons-logging</groupId>
                          <artifactId>commons-logging</artifactId>
                          <version>1.2</version>
                          <scope>runtime</scope>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void addToExistingDependencies() {
        rewriteRun(
          spec -> spec.recipe(new ManagedToRuntimeDependencies()),
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
                              <groupId>org.slf4j</groupId>
                              <artifactId>slf4j-api</artifactId>
                              <version>1.7.36</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.springframework</groupId>
                          <artifactId>spring-core</artifactId>
                          <version>5.3.21</version>
                      </dependency>
                  </dependencies>
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
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.springframework</groupId>
                          <artifactId>spring-core</artifactId>
                          <version>5.3.21</version>
                      </dependency>
                      <dependency>
                          <groupId>org.slf4j</groupId>
                          <artifactId>slf4j-api</artifactId>
                          <version>1.7.36</version>
                          <scope>runtime</scope>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void onlyImportScopedDependencies() {
        rewriteRun(
          spec -> spec.recipe(new ManagedToRuntimeDependencies()),
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
                              <groupId>com.fasterxml.jackson</groupId>
                              <artifactId>jackson-bom</artifactId>
                              <version>2.15.2</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                          <dependency>
                              <groupId>org.springframework</groupId>
                              <artifactId>spring-framework-bom</artifactId>
                              <version>5.3.21</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void noDependencyManagement() {
        rewriteRun(
          spec -> spec.recipe(new ManagedToRuntimeDependencies()),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.springframework</groupId>
                          <artifactId>spring-core</artifactId>
                          <version>5.3.21</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void preserveOtherDependencyElements() {
        rewriteRun(
          spec -> spec.recipe(new ManagedToRuntimeDependencies()),
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
                              <groupId>org.slf4j</groupId>
                              <artifactId>slf4j-api</artifactId>
                              <version>1.7.36</version>
                              <type>jar</type>
                              <classifier>sources</classifier>
                              <optional>true</optional>
                              <exclusions>
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
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.slf4j</groupId>
                          <artifactId>slf4j-api</artifactId>
                          <version>1.7.36</version>
                          <type>jar</type>
                          <classifier>sources</classifier>
                          <optional>true</optional>
                          <scope>runtime</scope>
                          <exclusions>
                              <exclusion>
                                  <groupId>commons-logging</groupId>
                                  <artifactId>commons-logging</artifactId>
                              </exclusion>
                          </exclusions>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }
}
