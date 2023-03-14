/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class ChangePropertyKeyTest implements RewriteTest {
    @Test
    void propertyInDependency() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyKey("guava.version", "version.com.google.guava")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                 
                <properties>
                  <guava.version>29.0-jre</guava.version>
                </properties>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>${guava.version}</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                 
                <properties>
                  <version.com.google.guava>29.0-jre</version.com.google.guava>
                </properties>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>${version.com.google.guava}</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void propertyInDependencyManagement() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyKey("guava.version", "version.com.google.guava")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                 
                <properties>
                  <guava.version>29.0-jre</guava.version>
                </properties>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>${guava.version}</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                 
                <properties>
                  <version.com.google.guava>29.0-jre</version.com.google.guava>
                </properties>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>${version.com.google.guava}</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void propertyInProperty() {
        rewriteRun(
          spec -> spec.recipe(new ChangePropertyKey("guava.version", "version.com.google.guava")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                 
                <properties>
                  <abc>value1</abc>
                  <guava.version>29.0-jre</guava.version>
                  <def>value1</def>
                  <xyz>${guava.version}</xyz>
                </properties>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                 
                <properties>
                  <abc>value1</abc>
                  <version.com.google.guava>29.0-jre</version.com.google.guava>
                  <def>value1</def>
                  <xyz>${version.com.google.guava}</xyz>
                </properties>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }
}
