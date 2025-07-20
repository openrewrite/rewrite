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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.test.RewriteTest;

import java.util.UUID;

import static org.openrewrite.maven.Assertions.pomXml;

class UpdateScmFromGitOriginTest implements RewriteTest {

    @DocumentExample
    @Test
    void updatesScmFromGitOriginUsingHttps() {
        rewriteRun(
          spec -> spec.recipe(new UpdateScmFromGitOrigin()),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <scm>
                  <url>https://old-server.example.com/org/repo</url>
                  <connection>scm:git:https://old-server.example.com/org/repo.git</connection>
                  <developerConnection>scm:git:git@old-server.example.com:org/repo.git</developerConnection>
                </scm>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <scm>
                  <url>https://new-server.example.com/username/repo</url>
                  <connection>scm:git:https://new-server.example.com/username/repo.git</connection>
                  <developerConnection>scm:git:git@new-server.example.com:username/repo.git</developerConnection>
                </scm>
              </project>
              """,
            spec -> spec.markers(gitProvenance("https://new-server.example.com/username/repo.git"))
          )
        );
    }

    @Test
    void updatesScmFromGitOriginUsingHttp() {
        rewriteRun(
          spec -> spec.recipe(new UpdateScmFromGitOrigin()),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <scm>
                  <url>https://old-server.example.com/org/repo</url>
                  <connection>scm:git:https://old-server.example.com/org/repo.git</connection>
                  <developerConnection>scm:git:git@old-server.example.com:org/repo.git</developerConnection>
                </scm>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <scm>
                  <url>https://new-server.example.com/username/new-path/repo</url>
                  <connection>scm:git:https://new-server.example.com/username/new-path/repo.git</connection>
                  <developerConnection>scm:git:git@new-server.example.com:username/new-path/repo.git</developerConnection>
                </scm>
              </project>
              """,
            spec -> spec.markers(gitProvenance("http://new-server.example.com/username/new-path/repo.git"))
          )
        );
    }

    @Test
    void updatesScmFromGitOriginUsingSsh() {
        rewriteRun(
          spec -> spec.recipe(new UpdateScmFromGitOrigin()),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <scm>
                  <url>https://old-server.example.com/org/repo</url>
                  <connection>scm:git:https://old-server.example.com/org/repo.git</connection>
                  <developerConnection>scm:git:git@old-server.example.com:org/repo.git</developerConnection>
                </scm>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <scm>
                  <url>https://new-server.example.com/username/new-path/repo</url>
                  <connection>scm:git:https://new-server.example.com/username/new-path/repo.git</connection>
                  <developerConnection>scm:git:git@new-server.example.com:username/new-path/repo.git</developerConnection>
                </scm>
              </project>
              """,
            spec -> spec.markers(gitProvenance("git@new-server.example.com:username/new-path/repo.git"))
          )
        );
    }

    @Test
    void updatesScmFromGitOriginApache() {
        rewriteRun(
          spec -> spec.recipe(new UpdateScmFromGitOrigin()),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <scm>
                  <url>https://github.com/apache/hugegraph-commons</url>
                  <connection>scm:git:https://github.com/apache/hugegraph-commons.git</connection>
                  <developerConnection>scm:git:https://github.com/apache/hugegraph-commons.git</developerConnection>
                </scm>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <scm>
                  <url>https://github.com/apache/incubator-hugegraph-commons</url>
                  <connection>scm:git:https://github.com/apache/incubator-hugegraph-commons.git</connection>
                  <developerConnection>scm:git:https://github.com/apache/incubator-hugegraph-commons.git</developerConnection>
                </scm>
              </project>
              """,
            spec -> spec.markers(gitProvenance("ssh://git@github.com/apache/incubator-hugegraph-commons.git"))
          )
        );
    }

    @Test
    void updatesScmFromGitlab() {
        rewriteRun(
          spec -> spec.recipe(new UpdateScmFromGitOrigin()),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <scm>
                  <url>https://gitlab.com/old-repo/old-path</url>
                  <connection>scm:git:https://gitlab.com/old-repo/old-path.git</connection>
                  <developerConnection>scm:git:https://gitlab.com/old-repo/old-path.git</developerConnection>
                  <tag>HEAD</tag>
                </scm>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <scm>
                  <url>https://gitlab.com/new-repo/new-path</url>
                  <connection>scm:git:https://gitlab.com/new-repo/new-path.git</connection>
                  <developerConnection>scm:git:https://gitlab.com/new-repo/new-path.git</developerConnection>
                  <tag>HEAD</tag>
                </scm>
              </project>
              """,
            spec -> spec.markers(gitProvenance("https://gitlab.com/new-repo/new-path.git"))
          )
        );
    }

    @Test
    void updatesScmFromGitOriginWithScm() {
        rewriteRun(
          spec -> spec.recipe(new UpdateScmFromGitOrigin()),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <scm>
                  <connection>scm:git:https://open.med.harvard.edu/stash/scm/old-path/repo.git</connection>
                  <developerConnection>scm:git:https://open.med.harvard.edu/stash/scm/old-path/repo.git</developerConnection>
                </scm>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <scm>
                  <connection>scm:git:https://open.med.harvard.edu/stash/scm/new-path/repo.git</connection>
                  <developerConnection>scm:git:https://open.med.harvard.edu/stash/scm/new-path/repo.git</developerConnection>
                </scm>
              </project>
              """,
            spec -> spec.markers(gitProvenance("https://open.med.harvard.edu/stash/scm/new-path/repo.git"))
          )
        );
    }

    @Test
    void updatesScmFromGitOriginWithUser() {
        rewriteRun(
          spec -> spec.recipe(new UpdateScmFromGitOrigin()),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <scm>
                  <connection>scm:git:https://username@old-server.example.com/username/repository.git</connection>
                  <developerConnection>scm:git:https://username@old-server.example.com/username/repository.git</developerConnection>
                  <url>https://old-server.example.com/username/repository</url>
                </scm>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <scm>
                  <connection>scm:git:https://username@new-server.example.com/username/repository.git</connection>
                  <developerConnection>scm:git:https://username@new-server.example.com/username/repository.git</developerConnection>
                  <url>https://new-server.example.com/username/repository</url>
                </scm>
              </project>
              """,
            spec -> spec.markers(gitProvenance("https://username@new-server.example.com/username/repository.git"))
          )
        );
    }

    @Test
    void retainUrlSuffix() {
        rewriteRun(
          spec -> spec.recipe(new UpdateScmFromGitOrigin()),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <scm>
                  <connection>scm:git:https://gitbox.apache.org/repos/asf/maven-invoker.git</connection>
                  <developerConnection>scm:git:https://gitbox.apache.org/repos/asf/maven-invoker.git</developerConnection>
                  <url>https://github.com/apache/maven-invoker/tree/${project.scm.tag}</url>
                </scm>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <scm>
                  <connection>scm:git:https://github.com/apache/maven-invoker.git</connection>
                  <developerConnection>scm:git:https://github.com/apache/maven-invoker.git</developerConnection>
                  <url>https://github.com/apache/maven-invoker/tree/${project.scm.tag}</url>
                </scm>
              </project>
              """,
            spec -> spec.markers(gitProvenance("https://github.com/apache/maven-invoker.git"))
          )
        );
    }

    @Nested
    class NoChange {

        @Test
        void gitOriginIsTheSame() {
            rewriteRun(
              spec -> spec.recipe(new UpdateScmFromGitOrigin()),
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <scm>
                      <url>https://github.com/apache/roller</url>
                      <connection>scm:git:https://github.com/apache/roller.git</connection>
                      <developerConnection>scm:git:https://github.com/apache/roller.git</developerConnection>
                    </scm>
                  </project>
                  """,
                spec -> spec.markers(gitProvenance("ssh://git@github.com/apache/roller.git"))
              )
            );
        }

        @Test
        void scmIsMissing() {
            rewriteRun(
              spec -> spec.recipe(new UpdateScmFromGitOrigin()),
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
                  """,
                spec -> spec.markers(gitProvenance("git@new-server.example.com:username/repo.git"))
              )
            );
        }

        @Test
        void gitOriginIsNull() {
            rewriteRun(
              spec -> spec.recipe(new UpdateScmFromGitOrigin()),
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
                  """,
                spec -> spec.markers(gitProvenance(null))
              )
            );
        }
    }

    private GitProvenance gitProvenance(String origin) {
        return new GitProvenance(UUID.randomUUID(), origin, null, null, null, null, null);
    }
}
