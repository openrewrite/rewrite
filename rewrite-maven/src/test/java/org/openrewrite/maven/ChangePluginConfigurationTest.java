/*
 * Copyright 2021 the original author or authors.
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

class ChangePluginConfigurationTest implements RewriteTest {
    @DocumentExample
    @Test
    void removeConfiguration() {
        rewriteRun(
          spec -> spec.recipe(new ChangePluginConfiguration("org.openrewrite.maven", "rewrite-maven-plugin", null)),
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <configuration>
                                  <activeRecipes>
                                      <recipe>org.openrewrite.java.cleanup.UnnecessaryThrows</recipe>
                                  </activeRecipes>
                              </configuration>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );
    }

    @Test
    void addConfiguration() {
        rewriteRun(
          spec -> spec.recipe(new ChangePluginConfiguration(
            "org.openrewrite.maven",
            "rewrite-maven-plugin",
            "<activeRecipes>\n<recipe>org.openrewrite.java.cleanup.UnnecessaryThrows</recipe>\n</activeRecipes>")),
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <configuration>
                                  <activeRecipes>
                                      <recipe>org.openrewrite.java.cleanup.UnnecessaryThrows</recipe>
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
    void replaceConfiguration() {
        rewriteRun(
          spec -> spec.recipe(new ChangePluginConfiguration(
            "org.openrewrite.maven",
            "rewrite-maven-plugin",
            "<activeRecipes>\n<recipe>org.openrewrite.java.cleanup.UnnecessaryThrows</recipe>\n</activeRecipes>")),
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <configuration />
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <configuration>
                                  <activeRecipes>
                                      <recipe>org.openrewrite.java.cleanup.UnnecessaryThrows</recipe>
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
    void addConfigurationWithMavenPropertyFromYaml() {
        rewriteRun(
          spec -> spec.recipeFromYaml("""
            type: specs.openrewrite.org/v1beta/recipe
            name: com.yourorg.ChangePluginConfigurationExample
            displayName: Change Maven plugin configuration example
            description: Add targetJdk configuration with Maven property reference.
            recipeList:
              - org.openrewrite.maven.ChangePluginConfiguration:
                  groupId: org.openrewrite.maven
                  artifactId: rewrite-maven-plugin
                  configuration: "<targetJdk>\\${java.version}</targetJdk>"
            """,
            "com.yourorg.ChangePluginConfigurationExample"),
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <configuration>
                                  <targetJdk>${java.version}</targetJdk>
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
    void addConfigurationWithMavenProperty() {
        rewriteRun(
          spec -> spec.recipe(new ChangePluginConfiguration(
            "org.openrewrite.maven",
            "rewrite-maven-plugin",
            "<targetJdk>${java.version}</targetJdk>")),
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <configuration>
                                  <targetJdk>${java.version}</targetJdk>
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
    void transformConfigurationNoOpWhenConfigurationMissing() {
        rewriteRun(
          spec -> spec.recipe(new ChangePluginConfiguration(
            "org.openrewrite.maven",
            "rewrite-maven-plugin",
            null)),
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );
    }
}
