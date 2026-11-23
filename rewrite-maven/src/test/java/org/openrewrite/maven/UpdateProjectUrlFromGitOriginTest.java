/*
 * Copyright 2026 the original author or authors.
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.UUID;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class UpdateProjectUrlFromGitOriginTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpdateProjectUrlFromGitOrigin());
    }

    @DocumentExample
    @Test
    void updatesProjectUrl() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <url>https://old-server.example.com/org/sub-group/repo</url>
                <organization>
                  <name>My Company</name>
                  <url>https://www.example.com/</url>
                </organization>
                <scm>
                  <developerConnection>scm:git:${project.url}.git</developerConnection>
                  <url>${project.url}</url>
                </scm>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <url>https://new-server.example.com/group/sub-group/repo</url>
                <organization>
                  <name>My Company</name>
                  <url>https://www.example.com/</url>
                </organization>
                <scm>
                  <developerConnection>scm:git:${project.url}.git</developerConnection>
                  <url>${project.url}</url>
                </scm>
              </project>
              """,
            spec -> spec.markers(gitProvenance("git@new-server.example.com:group/sub-group/repo.git"))
          )
        );
    }

    @Test
    void sshPortIsNotCarriedIntoProjectUrl() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <url>https://old-server.example.com/org/repo</url>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <url>https://new-server.example.com/username/repo</url>
              </project>
              """,
            spec -> spec.markers(gitProvenance("ssh://git@new-server.example.com:7999/username/repo.git"))
          )
        );
    }

    @Test
    void scmProjectUrlReferencesAreKept() {
        rewriteRun(
          spec -> spec.recipes(new UpdateScmFromGitOrigin(null), new UpdateProjectUrlFromGitOrigin()),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <url>https://old-server.example.com/org/repo</url>
                <scm>
                  <developerConnection>scm:git:${project.url}.git</developerConnection>
                  <url>${project.url}</url>
                </scm>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <url>https://new-server.example.com/username/repo</url>
                <scm>
                  <developerConnection>scm:git:${project.url}.git</developerConnection>
                  <url>${project.url}</url>
                </scm>
              </project>
              """,
            spec -> spec.markers(gitProvenance("git@new-server.example.com:username/repo.git"))
          )
        );
    }

    @Test
    void childModuleUrlIsUnchanged() {
        rewriteRun(
          mavenProject("parent",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app-parent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <url>https://old-server.example.com/org/repo</url>
                  <modules>
                    <module>module1</module>
                  </modules>
                </project>
                """,
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app-parent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <url>https://new-server.example.com/username/repo</url>
                  <modules>
                    <module>module1</module>
                  </modules>
                </project>
                """,
              spec -> spec.markers(gitProvenance("https://new-server.example.com/username/repo.git"))
            ),
            mavenProject("module1",
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app-parent</artifactId>
                      <version>1</version>
                    </parent>
                    <artifactId>module1</artifactId>
                    <url>https://old-server.example.com/org/repo/module1</url>
                  </project>
                  """,
                spec -> spec.markers(gitProvenance("https://new-server.example.com/username/repo.git"))
              ))
          )
        );
    }

    @Nested
    class NoChange {

        @Test
        void propertyUrlIsUnchanged() {
            rewriteRun(
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                      <site.url>https://old-server.example.com/org/repo</site.url>
                    </properties>
                    <url>${site.url}</url>
                  </project>
                  """,
                spec -> spec.markers(gitProvenance("https://new-server.example.com/username/repo.git"))
              )
            );
        }

        @Test
        void urlAlreadyMatches() {
            rewriteRun(
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <url>https://new-server.example.com/username/repo</url>
                  </project>
                  """,
                spec -> spec.markers(gitProvenance("git@new-server.example.com:username/repo.git"))
              )
            );
        }

        @Test
        void noGitOrigin() {
            rewriteRun(
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <url>https://old-server.example.com/org/repo</url>
                  </project>
                  """,
                spec -> spec.markers(gitProvenance(null))
              )
            );
        }

        @Test
        void noProjectUrl() {
            rewriteRun(
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
                  """,
                spec -> spec.markers(gitProvenance("https://new-server.example.com/username/repo.git"))
              )
            );
        }
    }

    private GitProvenance gitProvenance(String origin) {
        return new GitProvenance(UUID.randomUUID(), origin, null, null, null, null, null);
    }
}
