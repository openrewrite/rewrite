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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

public class RemoveRepositoryTest implements RewriteTest {

    @Test
    @DisplayName("It should only remove a repo when both the id and the urls are the same")
    void removesRepoAndIdCombination() {

        rewriteRun(
          spec -> spec.recipe(new RemoveRepository("myRepo", "https://myrepo.maven.com/repo")),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>myRepo</id>
                    <url>https://myrepo.maven.com/repo</url>
                  </repository>
                  <repository>
                    <id>same_repo_different_id</id>
                    <url>https://myrepo.maven.com/repo</url>
                  </repository>
                </repositories>
                <pluginRepositories>
                  <pluginRepository>
                    <id>myRepo</id>
                    <url>https://someOtherUrl</url>
                  </pluginRepository>
                  <pluginRepository>
                    <id>identicalRepo</id>
                    <url>https://myrepo.maven.com/repo</url>
                  </pluginRepository>
                </pluginRepositories>
              </project>
              """,
            """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <repositories>
                <repository>
                  <id>same_repo_different_id</id>
                  <url>https://myrepo.maven.com/repo</url>
                </repository>
              </repositories>
              <pluginRepositories>
                <pluginRepository>
                  <id>myRepo</id>
                  <url>https://someOtherUrl</url>
                </pluginRepository>
                <pluginRepository>
                  <id>identicalRepo</id>
                  <url>https://myrepo.maven.com/repo</url>
                </pluginRepository>
              </pluginRepositories>
            </project>
            """
          )
        );
    }

    @Test
    @DisplayName("If only the url is given, any repo matching that url should be removed")
    void removeSimpleRepoWithUrlOnly() {

        rewriteRun(
          spec -> spec.recipe(new RemoveRepository(null, "https://myrepo.maven.com/repo")),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>myRepo</id>
                    <url>https://myrepo.maven.com/repo</url>
                  </repository>
                  <repository>
                    <id>same_repo_different_id</id>
                    <url>https://myrepo.maven.com/repo</url>
                  </repository>
                </repositories>
                <pluginRepositories>
                  <pluginRepository>
                    <id>myRepo</id>
                    <url>https://someOtherUrl</url>
                  </pluginRepository>
                  <pluginRepository>
                    <id>identicalRepo</id>
                    <url>https://myrepo.maven.com/repo</url>
                  </pluginRepository>
                </pluginRepositories>
              </project>
              """,
            """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <repositories>
              </repositories>
              <pluginRepositories>
                <pluginRepository>
                  <id>myRepo</id>
                  <url>https://someOtherUrl</url>
                </pluginRepository>
              </pluginRepositories>
            </project>
            """
        ));
    }

}