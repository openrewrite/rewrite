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
package org.openrewrite.maven.security;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class UseHttpsForRepositoriesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseHttpsForRepositories());
    }

    @Test
    void replaceHttpInRepositoryBlock() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>my-repo</id>
                    <url>http://repo.example.com/repo</url>
                  </repository>
                </repositories>
              </project>
              """,
            """
              <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>my-repo</id>
                    <url>https://repo.example.com/repo</url>
                  </repository>
                </repositories>
              </project>
              """
          )
        );
    }

    @Test
    void replaceHttpInDistributionManagementBlock() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <distributionManagement>
                  <repository>
                    <id>my-repo</id>
                    <url>http://repo.example.com/repo</url>
                  </repository>
                  <snapshotRepository>
                    <id>my-snapshot-repo</id>
                    <url>http://repo.example.com/repo</url>
                  </snapshotRepository>
                </distributionManagement>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <distributionManagement>
                  <repository>
                    <id>my-repo</id>
                    <url>https://repo.example.com/repo</url>
                  </repository>
                  <snapshotRepository>
                    <id>my-snapshot-repo</id>
                    <url>https://repo.example.com/repo</url>
                  </snapshotRepository>
                </distributionManagement>
              </project>
              """
          )
        );
    }

    @Test
    void replaceHttpInPluginRepositoriesBlock() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <pluginRepositories>
                  <pluginRepository>
                    <id>my-repo</id>
                    <url>http://repo.example.com/repo</url>
                  </pluginRepository>
                </pluginRepositories>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <pluginRepositories>
                  <pluginRepository>
                    <id>my-repo</id>
                    <url>https://repo.example.com/repo</url>
                  </pluginRepository>
                </pluginRepositories>
              </project>
              """
          )
        );
    }

    @Test
    void replaceHttpInRepositoryBlockFromProperties() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <my-repo-url>http://repo.example.com/repo</my-repo-url>
                </properties>
                <repositories>
                  <repository>
                    <id>my-repo</id>
                    <url>${my-repo-url}</url>
                  </repository>
                </repositories>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <my-repo-url>https://repo.example.com/repo</my-repo-url>
                </properties>
                <repositories>
                  <repository>
                    <id>my-repo</id>
                    <url>${my-repo-url}</url>
                  </repository>
                </repositories>
              </project>
              """
          )
        );
    }
}
