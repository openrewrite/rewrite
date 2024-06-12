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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class AddPluginRepositoryTest implements RewriteTest {

    @DocumentExample
    @Test
    void addSimpleRepo() {
        rewriteRun(
          spec -> spec.recipe(new AddPluginRepository("myRepo", "http://myrepo.maven.com/repo", null, null,
            null, null, null,
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
                <pluginRepositories>
                  <pluginRepository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                  </pluginRepository>
                </pluginRepositories>
              </project>
              """
          )
        );
    }

    @Test
    void updateExistingRepo() {
        rewriteRun(
          spec -> spec.recipe(new AddPluginRepository("myRepo", "http://myrepo.maven.com/repo", "bb", null,
            null, null, null,
            null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <pluginRepositories>
                  <pluginRepository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                    <name>qq</name>
                  </pluginRepository>
                </pluginRepositories>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <pluginRepositories>
                  <pluginRepository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                    <name>bb</name>
                  </pluginRepository>
                </pluginRepositories>
              </project>
              """
          )
        );
    }

    @Test
    void doNotRemoveRepoName() {
        rewriteRun(
          spec -> spec.recipe(new AddPluginRepository("myRepo", "http://myrepo.maven.com/repo", null, null,
            null, null, null,
            null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <pluginRepositories>
                  <pluginRepository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                    <name>qq</name>
                  </pluginRepository>
                </pluginRepositories>
              </project>
              """
          )
        );
    }

    @Test
    void removeSnapshots() {
        rewriteRun(
          spec -> spec.recipe(new AddPluginRepository("myRepo", "http://myrepo.maven.com/repo", null, null,
            null, null, null,
            null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <pluginRepositories>
                  <pluginRepository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                    <snapshots>
                        <enabled>true</enabled>
                    </snapshots>
                  </pluginRepository>
                </pluginRepositories>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <pluginRepositories>
                  <pluginRepository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                  </pluginRepository>
                </pluginRepositories>
              </project>
              """
          )
        );
    }

    @Test
    void updateSnapshots1() {
        rewriteRun(
          spec -> spec.recipe(new AddPluginRepository("myRepo", "http://myrepo.maven.com/repo", null, null,
            false, "whatever", null,
            null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <pluginRepositories>
                  <pluginRepository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                    <snapshots>
                      <enabled>true</enabled>
                    </snapshots>
                  </pluginRepository>
                </pluginRepositories>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <pluginRepositories>
                  <pluginRepository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                    <snapshots>
                      <enabled>false</enabled>
                      <checksumPolicy>whatever</checksumPolicy>
                    </snapshots>
                  </pluginRepository>
                </pluginRepositories>
              </project>
              """
          )
        );
    }

    @Test
    void updateSnapshots2() {
        rewriteRun(
          spec -> spec.recipe(new AddPluginRepository("myRepo", "http://myrepo.maven.com/repo", null, null,
            null, "whatever", null,
            null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <pluginRepositories>
                  <pluginRepository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                    <snapshots>
                      <enabled>true</enabled>
                    </snapshots>
                  </pluginRepository>
                </pluginRepositories>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <pluginRepositories>
                  <pluginRepository>
                    <id>myRepo</id>
                    <url>http://myrepo.maven.com/repo</url>
                    <snapshots>
                      <checksumPolicy>whatever</checksumPolicy>
                    </snapshots>
                  </pluginRepository>
                </pluginRepositories>
              </project>
              """
          )
        );
    }

    @Test
    void noIdMatch1SameSnapshots() {
        rewriteRun(
          spec -> spec.recipe(new AddPluginRepository("myRepo", "http://myrepo.maven.com/repo", null, null,
            true, null, null,
            null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <pluginRepositories>
                  <pluginRepository>
                    <id>myRepo-X</id>
                    <url>http://myrepo.maven.com/repo</url>
                    <snapshots>
                      <enabled>true</enabled>
                    </snapshots>
                  </pluginRepository>
                </pluginRepositories>
              </project>
              """
          )
        );
    }
}
