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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.properties.Assertions.properties;

class MigrateToMaven3_10Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/maven.yml", "org.openrewrite.maven.MigrateToMaven3_10");
    }

    @DocumentExample
    @Test
    void removesDuplicateAndUpdatesWrapper() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-jar-plugin</artifactId>
                              <version>3.4.2</version>
                          </plugin>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-jar-plugin</artifactId>
                              <version>3.4.2</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-jar-plugin</artifactId>
                              <version>3.4.2</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          ),
          properties(
            """
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip
              """,
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
              .after(after -> assertThat(after)
                .containsPattern("/apache-maven/3\\.10\\.\\d+/apache-maven-3\\.10\\.\\d+-bin\\.zip")
                .actual())
          )
        );
    }

    @Test
    void removesPropertiesEqualToSuperPomDefaultsOnceWrapperIsUpdated() {
        rewriteRun(
          spec -> spec.allSources(source -> source.markers(new BuildTool(randomId(), BuildTool.Type.Maven, "3.9.11"))),
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0.0</version>
                  <properties>
                      <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                      <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
                      <project.build.outputTimestamp>1980-01-01T00:00:02Z</project.build.outputTimestamp>
                      <maven.compiler.release>17</maven.compiler.release>
                  </properties>
              </project>
              """,
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0.0</version>
                  <properties>
                      <project.build.outputTimestamp>1980-01-01T00:00:02Z</project.build.outputTimestamp>
                      <maven.compiler.release>17</maven.compiler.release>
                  </properties>
              </project>
              """
          ),
          properties(
            """
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip
              """,
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
              .after(after -> assertThat(after)
                .containsPattern("/apache-maven/3\\.10\\.\\d+/apache-maven-3\\.10\\.\\d+-bin\\.zip")
                .actual())
          )
        );
    }

    @Test
    void keepsPropertiesEqualToSuperPomDefaultsWithoutWrapper() {
        rewriteRun(
          spec -> spec.allSources(source -> source.markers(new BuildTool(randomId(), BuildTool.Type.Maven, "3.9.11"))),
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0.0</version>
                  <properties>
                      <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                  </properties>
              </project>
              """
          )
        );
    }
}
