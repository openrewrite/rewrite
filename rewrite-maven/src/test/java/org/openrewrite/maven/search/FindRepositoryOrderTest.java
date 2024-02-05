/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.maven.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.table.MavenRepositoryOrder;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class FindRepositoryOrderTest implements RewriteTest {

    @Test
    void findRepositoryOrder() {
        var ctx = MavenExecutionContextView.view(new InMemoryExecutionContext());
        ctx.setMavenSettings(MavenSettings.parse(new Parser.Input(Paths.get("settings.xml"), () -> new ByteArrayInputStream(
          //language=xml
          """
            <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
                <activeProfiles>
                    <activeProfile>
                        repo
                    </activeProfile>
                </activeProfiles>
                <profiles>
                    <profile>
                        <id>repo</id>
                        <repositories>
                            <repository>
                                <id>spring-milestones</id>
                                <name>Spring Milestones</name>
                                <url>https://repo.spring.io/milestone</url>
                            </repository>
                        </repositories>
                    </profile>
                </profiles>
            </settings>
            """.getBytes()
        )), ctx));

        rewriteRun(
          spec -> spec
            .recipe(new FindRepositoryOrder())
            .executionContext(ctx)
            .dataTable(MavenRepositoryOrder.Row.class, failures ->
              assertThat(failures.stream().map(MavenRepositoryOrder.Row::getUri).distinct())
                .containsExactlyInAnyOrder(
                  "https://myrepo.maven.com/repo",
                  "https://repo.spring.io/milestone")),
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
                <repositories>
                  <repository>
                    <id>myRepo</id>
                    <url>https://myrepo.maven.com/repo</url>
                  </repository>
                </repositories>
              </project>
              """,
            """
              <!--~~(https://myrepo.maven.com/repo
              https://repo.spring.io/milestone)~~>--><project>
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>1.5.12.RELEASE</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>myRepo</id>
                    <url>https://myrepo.maven.com/repo</url>
                  </repository>
                </repositories>
              </project>
              """
          )
        );
    }
}
