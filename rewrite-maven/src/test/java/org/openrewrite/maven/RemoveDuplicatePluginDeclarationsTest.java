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

class RemoveDuplicatePluginDeclarationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveDuplicatePluginDeclarations());
    }

    @DocumentExample
    @Test
    void removeDuplicatePlugin() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.8.1</version>
                          </plugin>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-surefire-plugin</artifactId>
                              <version>2.22.2</version>
                          </plugin>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.11.0</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.8.1</version>
                          </plugin>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-surefire-plugin</artifactId>
                              <version>2.22.2</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );
    }

    @Test
    void removeDuplicatePluginWithImplicitGroupId() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.8.1</version>
                          </plugin>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.11.0</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
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

    @Test
    void removeDuplicateInPluginManagement() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <pluginManagement>
                          <plugins>
                              <plugin>
                                  <groupId>org.apache.maven.plugins</groupId>
                                  <artifactId>maven-compiler-plugin</artifactId>
                                  <version>3.8.1</version>
                              </plugin>
                              <plugin>
                                  <groupId>org.apache.maven.plugins</groupId>
                                  <artifactId>maven-compiler-plugin</artifactId>
                                  <version>3.11.0</version>
                              </plugin>
                          </plugins>
                      </pluginManagement>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <pluginManagement>
                          <plugins>
                              <plugin>
                                  <groupId>org.apache.maven.plugins</groupId>
                                  <artifactId>maven-compiler-plugin</artifactId>
                                  <version>3.8.1</version>
                              </plugin>
                          </plugins>
                      </pluginManagement>
                  </build>
              </project>
              """
          )
        );
    }

    @Test
    void removeMultipleDuplicates() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
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
                              <version>3.9.0</version>
                          </plugin>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.11.0</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
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

    @Test
    void noChangeWhenNoDuplicates() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.8.1</version>
                          </plugin>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-surefire-plugin</artifactId>
                              <version>2.22.2</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );
    }

    @Test
    void keepDifferentGroupIdsSameArtifactId() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.8.1</version>
                          </plugin>
                          <plugin>
                              <groupId>com.example</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>1.0.0</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );
    }
}
