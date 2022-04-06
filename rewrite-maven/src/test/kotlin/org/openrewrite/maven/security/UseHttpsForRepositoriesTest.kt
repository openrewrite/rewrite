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
@file:Suppress("HttpUrlsUsage")

package org.openrewrite.maven.security

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.maven.MavenRecipeTest

class UseHttpsForRepositoriesTest : MavenRecipeTest {

    override val recipe: Recipe
        get() = UseHttpsForRepositories()

    @Test
    fun replaceHttpInRepositoryBlock() = assertChanged(
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>

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
        after = """
            <project>
              <modelVersion>4.0.0</modelVersion>

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

    @Test
    fun replaceHttpInDistributionManagementBlock() = assertChanged(
        before = """
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
        after = """
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

    @Test
    fun replaceHttpInPluginRepositoriesBlock() = assertChanged(
        before = """
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
        after = """
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

    @Test
    fun replaceHttpInRepositoryBlockFromProperties() = assertChanged(
        before = """
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
                  <url>${'$'}{my-repo-url}</url>
                </repository>
              </repositories>
            </project>
        """,
        after = """
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
                  <url>${'$'}{my-repo-url}</url>
                </repository>
              </repositories>
            </project>
        """
    )
}
