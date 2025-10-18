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

class ReplaceRemovedRootDirectoryPropertiesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceRemovedRootDirectoryProperties());
    }

    @DocumentExample
    @Test
    void replaceExecutionRootDirectory() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <properties>
                      <config.path>${executionRootDirectory}/config</config.path>
                  </properties>
              </project>
              """,
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <properties>
                      <config.path>${session.rootDirectory}/config</config.path>
                  </properties>
              </project>
              """
          )
        );
    }

    @Test
    void replaceMultiModuleProjectDirectory() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <properties>
                      <root.dir>${multiModuleProjectDirectory}</root.dir>
                  </properties>
              </project>
              """,
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <properties>
                      <root.dir>${project.rootDirectory}</root.dir>
                  </properties>
              </project>
              """
          )
        );
    }

    @Test
    void replaceBothProperties() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <properties>
                      <exec.root>${executionRootDirectory}</exec.root>
                      <multi.module>${multiModuleProjectDirectory}</multi.module>
                  </properties>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-resources-plugin</artifactId>
                              <configuration>
                                  <outputDirectory>${multiModuleProjectDirectory}/target</outputDirectory>
                              </configuration>
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
                  <properties>
                      <exec.root>${session.rootDirectory}</exec.root>
                      <multi.module>${project.rootDirectory}</multi.module>
                  </properties>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-resources-plugin</artifactId>
                              <configuration>
                                  <outputDirectory>${project.rootDirectory}/target</outputDirectory>
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
    void noChangeWhenPropertiesNotUsed() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1.0.0</version>
                  <properties>
                      <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                  </properties>
              </project>
              """
          )
        );
    }
}
