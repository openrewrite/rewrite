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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.test.RewriteTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.openrewrite.maven.Assertions.pomXml;

class ChangePluginConfigurationTest implements RewriteTest {

    @Language("xml")
    private static String xslt;

    @BeforeAll
    static void setup() {
        xslt = StringUtils.readFully(ChangePluginConfigurationTest.class
          .getResourceAsStream("/changePlugin.xslt"));

        assertFalse(StringUtils.isBlank(xslt));
    }

    @DocumentExample
    @Test
    void removeConfiguration() {
        rewriteRun(
          spec -> spec.recipe(new ChangePluginConfiguration("org.openrewrite.maven", "rewrite-maven-plugin", null, null, null)),
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
            "<activeRecipes>\n<recipe>org.openrewrite.java.cleanup.UnnecessaryThrows</recipe>\n</activeRecipes>",
            null,
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
            "<activeRecipes>\n<recipe>org.openrewrite.java.cleanup.UnnecessaryThrows</recipe>\n</activeRecipes>",
            null,
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
    void transformConfigurationFromInlineTransformation() {
        rewriteRun(
          spec -> spec.recipe(new ChangePluginConfiguration(
            "org.openrewrite.maven",
            "rewrite-maven-plugin",
            null,
            xslt,
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
                              <configuration>
                                  <activeRecipes>
                                      <activeRecipe>org.openrewrite.java.cleanup.UnnecessaryThrows</activeRecipe>
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
    void transformConfigurationFromClasspathResource() {
        rewriteRun(
          spec -> spec.recipe(new ChangePluginConfiguration(
            "org.openrewrite.maven",
            "rewrite-maven-plugin",
            null,
            null,
            "/changePlugin.xslt")),
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
                              <configuration>
                                  <activeRecipes>
                                      <activeRecipe>org.openrewrite.java.cleanup.UnnecessaryThrows</activeRecipe>
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
    void transformConfigurationNoOpWhenConfigurationMissing() {
        rewriteRun(
          spec -> spec.recipe(new ChangePluginConfiguration(
            "org.openrewrite.maven",
            "rewrite-maven-plugin",
            null,
            null,
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
