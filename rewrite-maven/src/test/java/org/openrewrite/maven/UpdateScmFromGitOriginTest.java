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
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.test.RewriteTest;

import java.time.Instant;
import java.util.UUID;

import static org.openrewrite.maven.Assertions.pomXml;

class UpdateScmFromGitOriginTest implements RewriteTest {

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
            spec -> spec.markers(sampleGitProvenance("https://new-server.example.com/username/repo.git"))
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
                  <url>http://new-server.example.com/username/repo</url>
                  <connection>scm:git:http://new-server.example.com/username/repo.git</connection>
                  <developerConnection>scm:git:git@new-server.example.com:username/repo.git</developerConnection>
                </scm>
              </project>
              """,
            spec -> spec.markers(sampleGitProvenance("http://new-server.example.com/username/repo.git"))
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
                  <url>https://new-server.example.com/username/repo</url>
                  <connection>scm:git:git@new-server.example.com:username/repo.git</connection>
                  <developerConnection>scm:git:git@new-server.example.com:username/repo.git</developerConnection>
                </scm>
              </project>
              """,
            spec -> spec.markers(sampleGitProvenance("git@new-server.example.com:username/repo.git"))
          )
        );
    }

    @Test
    void doesNothingWhenScmIsMissing() {
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
            spec -> spec.markers(sampleGitProvenance("git@new-server.example.com:username/repo.git"))
          )
        );
    }

    private GitProvenance sampleGitProvenance(String origin) {
        return new GitProvenance(UUID.randomUUID(), origin, null, null, null, null, null);
    }
}