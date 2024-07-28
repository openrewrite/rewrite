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
package org.openrewrite.maven.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class EffectiveMavenRepositoriesTest implements RewriteTest {

    @SuppressWarnings("ConstantConditions")
    //language=xml
    private static final MavenSettings SPRING_MILESTONES_SETTINGS = MavenSettings.parse(Parser.Input.fromString("""
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
      """), new InMemoryExecutionContext());

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new EffectiveMavenRepositories(true));
    }

    @DocumentExample
    @Test
    void emptyRepositories() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <!--~~(https://repo.maven.apache.org/maven2)~~>--><project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void repositoryInPom() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>spring-milestone</id>
                    <url>https://repo.spring.io/milestone</url>
                    </repository>
                </repositories>
              </project>
              """,
            """
              <!--~~(https://repo.spring.io/milestone
              https://repo.maven.apache.org/maven2)~~>--><project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <repositories>
                  <repository>
                    <id>spring-milestone</id>
                    <url>https://repo.spring.io/milestone</url>
                    </repository>
                </repositories>
              </project>
              """
          )
        );
    }

    @Test
    void fromExecutionContextSettings() {
        rewriteRun(
          spec -> spec.executionContext(MavenExecutionContextView.view(new InMemoryExecutionContext())
            .setMavenSettings(SPRING_MILESTONES_SETTINGS, "repo")),
          pomXml(
            """
              <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <!--~~(https://repo.spring.io/milestone
              https://repo.maven.apache.org/maven2)~~>--><project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void fromMavenSettingsOnAst() {
        rewriteRun(
          spec -> spec
            .executionContext(MavenExecutionContextView.view(new InMemoryExecutionContext())
              .setMavenSettings(SPRING_MILESTONES_SETTINGS, "repo"))
            .recipeExecutionContext(new InMemoryExecutionContext()),
          pomXml(
            """
              <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <!--~~(https://repo.spring.io/milestone
              https://repo.maven.apache.org/maven2)~~>--><project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void producesDataTable() {
        rewriteRun(
          spec -> spec
            .recipe(new EffectiveMavenRepositories(false))
            .executionContext(MavenExecutionContextView.view(new InMemoryExecutionContext())
              .setMavenSettings(SPRING_MILESTONES_SETTINGS, "repo"))
            .dataTableAsCsv(EffectiveMavenRepositoriesTable.class.getName(), """
              pomPath,repositoryUri
              pom.xml,"https://repo.spring.io/milestone"
              pom.xml,"https://repo.maven.apache.org/maven2"
              module/pom.xml,"https://repo.spring.io/milestone"
              module/pom.xml,"https://repo.maven.apache.org/maven2"
              """)
            .recipeExecutionContext(new InMemoryExecutionContext()),
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <modules>
                      <module>module</module>
                  </modules>
              </project>
              """,
            spec -> spec.path("pom.xml")
          ),
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>module</artifactId>
                  <version>1</version>
              </project>
              """,
            spec -> spec.path("module/pom.xml")
          )
        );
    }
}
