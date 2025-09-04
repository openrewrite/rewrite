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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeParentVersionTest implements RewriteTest {

    @DocumentExample
    @Test
    void nonMavenCentralRepository() {
        rewriteRun(
          spec -> spec
            .recipe(new UpgradeParentVersion(
              "org.jenkins-ci",
              "jenkins",
              "1.125",
              null,
              null))
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
                      <groupId>org.jenkins-ci</groupId>
                      <artifactId>jenkins</artifactId>
                      <version>1.124</version>
                  </parent>
                  <artifactId>example</artifactId>
                  <version>1.0.0</version>
              </project>
              """,
            """
              <project>
                  <parent>
                      <groupId>org.jenkins-ci</groupId>
                      <artifactId>jenkins</artifactId>
                      <version>1.125</version>
                  </parent>
                  <artifactId>example</artifactId>
                  <version>1.0.0</version>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1317")
    @Test
    void doesNotDowngradeVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeParentVersion(
            "org.springframework.boot",
            "spring-boot-starter-parent",
            "~1.5",
            null,
            null)),
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

    @Test
    void upgradeVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeParentVersion(
            "org.springframework.boot",
            "spring-boot-starter-parent",
            "~1.5",
            null,
            null)),
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

    @ParameterizedTest
    @ValueSource(strings = {"<relativePath />", "<relativePath></relativePath>"})
    void onlyExternalWhenActuallyExternal(String relativePathTag) {
        rewriteRun(
          spec -> spec.recipe(new UpgradeParentVersion(
            "org.springframework.boot",
            "spring-boot-starter-parent",
            "~1.5",
            null,
            true)),
          pomXml(
            """
              <project>
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                  %s
                </parent>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """.formatted(relativePathTag),
            """
              <project>
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.22.RELEASE</version>
                  %s
                </parent>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """.formatted(relativePathTag)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "<relativePath>..</relativePath>", "<relativePath>../pom.xml</relativePath>", "<relativePath>../../pom.xml</relativePath>"})
    void onlyExternalWhenNotExternal(String relativePathTag) {
        rewriteRun(
          spec -> spec.recipe(new UpgradeParentVersion(
            "org.springframework.boot",
            "spring-boot-starter-parent",
            "~1.5",
            null,
            true)),
          pomXml(
            """
              <project>
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                  %s
                </parent>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """.formatted(relativePathTag)
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
            null,
            null)),
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

    @Issue("https://github.com/openrewrite/rewrite/issues/5707")
    @Test
    void upgradeParentWithCIFriendlyVersionsAndNullProperty() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeParentVersion(
            "org.openrewrite",
            "rewrite-bom",
            "8.56.0",
            null,
            null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-bom</artifactId>
                  <version>8.55.0</version>
                </parent>
                <groupId>foo</groupId>
                <artifactId>bar</artifactId>
                <version>${revision}${sha1}${changelist}</version>
                <properties>
                  <revision>1.2.3</revision>
                  <changelist>-SNAPSHOT</changelist>
                  <sha1 />
                </properties>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-bom</artifactId>
                  <version>8.56.0</version>
                </parent>
                <groupId>foo</groupId>
                <artifactId>bar</artifactId>
                <version>${revision}${sha1}${changelist}</version>
                <properties>
                  <revision>1.2.3</revision>
                  <changelist>-SNAPSHOT</changelist>
                  <sha1 />
                </properties>
              </project>
              """
          )
        );
    }
}
