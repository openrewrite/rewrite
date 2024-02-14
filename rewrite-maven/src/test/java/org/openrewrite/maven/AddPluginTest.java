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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class AddPluginTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddPlugin("org.openrewrite.maven", "rewrite-maven-plugin", "100.0", null, null, null, null));
    }

    @DocumentExample
    @Test
    void addPluginWithConfiguration() {
        rewriteRun(
          spec -> spec.recipe(new AddPlugin("org.openrewrite.maven", "rewrite-maven-plugin", "100.0",
            "<configuration>\n<activeRecipes>\n<recipe>io.moderne.FindTest</recipe>\n</activeRecipes>\n</configuration>",
            null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>100.0</version>
                      <configuration>
                        <activeRecipes>
                          <recipe>io.moderne.FindTest</recipe>
                        </activeRecipes>
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
    void addPluginWithDependencies() {
        rewriteRun(
          spec -> spec.recipe(new AddPlugin("org.openrewrite.maven", "rewrite-maven-plugin", "100.0", null,
            """
                  <dependencies>
                      <dependency>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>java-8-migration</artifactId>
                          <version>1.0.0</version>
                      </dependency>
                  </dependencies>
              """, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>100.0</version>
                      <dependencies>
                        <dependency>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>java-8-migration</artifactId>
                          <version>1.0.0</version>
                        </dependency>
                      </dependencies>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void addPluginWithExecutions() {
        rewriteRun(
          spec -> spec.recipe(new AddPlugin("org.openrewrite.maven", "rewrite-maven-plugin", "100.0", null, null,
            """
                  <executions>
                    <execution>
                      <id>xjc</id>
                      <goals>
                        <goal>xjc</goal>
                      </goals>
                      <configuration>
                        <sources>
                          <source>/src/main/resources/countries.xsd</source>
                          <source>/src/main/resources/countries1.xsd</source>
                        </sources>
                        <outputDirectory>/src/main/generated-java</outputDirectory>
                      </configuration>
                    </execution>
                  </executions>
              """, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>100.0</version>
                      <executions>
                        <execution>
                          <id>xjc</id>
                          <goals>
                            <goal>xjc</goal>
                          </goals>
                          <configuration>
                            <sources>
                              <source>/src/main/resources/countries.xsd</source>
                              <source>/src/main/resources/countries1.xsd</source>
                            </sources>
                            <outputDirectory>/src/main/generated-java</outputDirectory>
                          </configuration>
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
    void addPlugin() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>100.0</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void updatePluginVersion() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>99.0</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>100.0</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void addPluginWithoutVersion() {
        rewriteRun(
          spec -> spec.recipe(new AddPlugin("org.openrewrite.maven", "rewrite-maven-plugin", null, null, null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void addPluginWithMatchingFilePattern() {
        rewriteRun(
          spec -> spec.recipe(new AddPlugin("org.openrewrite.maven", "rewrite-maven-plugin", null, null, null, null, "dir/pom.xml")),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            spec -> spec.path("dir/pom.xml")
          )
        );
    }

    @Test
    void addPluginWithNonMatchingFilePattern() {
        rewriteRun(
          spec -> spec.recipe(new AddPlugin("org.openrewrite.maven", "rewrite-maven-plugin", null, null, null, null, "dir/pom.xml")),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            spec -> spec.path("pom.xml")
          )
        );
    }

    @Test
    void addPluginWithExistingPlugin() {
        rewriteRun(
          spec -> spec.recipe(new AddPlugin("org.springframework.boot", "spring-boot-maven-plugin", "3.1.5", null, null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>3.1.5</version>
                </parent>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-maven-plugin</artifactId>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            spec -> spec.path("pom.xml")
          )
        );
    }
}
