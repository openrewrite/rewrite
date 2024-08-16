/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeParentVersionTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/1317")
    @Test
    void doesNotDowngradeVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeParentVersion(
            "org.springframework.boot",
            "spring-boot-starter-parent",
            "~1.5",
            null
          )),
          pomXml(
            """
              <project>
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>2.4.12</version>
                  <relativePath/>
                </parent>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @DocumentExample
    @Test
    void nonMavenCentralRepository() {
        rewriteRun(
          spec -> spec
            .recipe(new UpgradeParentVersion("org.jenkins-ci.plugins", "plugin", "4.40", null))
            .executionContext(
              MavenExecutionContextView
                .view(new InMemoryExecutionContext())
                .setRepositories(List.of(
                  MavenRepository.builder().id("jenkins").uri("https://repo.jenkins-ci.org/public/").build()
                ))
            ),
          pomXml(
            """
              <project>
                  <parent>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>plugin</artifactId>
                      <version>4.33</version>
                  </parent>
                  <artifactId>antisamy-markup-formatter</artifactId>
                  <version>1.0.0</version>
              </project>
              """,
            """
              <project>
                  <parent>
                      <groupId>org.jenkins-ci.plugins</groupId>
                      <artifactId>plugin</artifactId>
                      <version>4.40</version>
                  </parent>
                  <artifactId>antisamy-markup-formatter</artifactId>
                  <version>1.0.0</version>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeParentVersion(
            "org.springframework.boot",
            "spring-boot-starter-parent",
            "~1.5",
            null
          )),
          pomXml(
            """
              <project>
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.22.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeToExactVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeParentVersion(
            "org.springframework.boot",
            "spring-boot-starter-parent",
            "1.5.22.RELEASE",
            null
          )),
          pomXml(
            """
              <project>
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.22.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }
}
