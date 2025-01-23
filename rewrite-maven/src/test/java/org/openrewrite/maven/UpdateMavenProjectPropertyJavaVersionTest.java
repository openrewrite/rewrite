/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
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
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

@Deprecated(forRemoval = true)
class UpdateMavenProjectPropertyJavaVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpdateMavenProjectPropertyJavaVersion(17));
    }

    @DocumentExample
    @Test
    void basic() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0.0</version>
                  <modelVersion>4.0</modelVersion>
                  <properties>
                      <java.version>11</java.version>
                      <jdk.version>11</jdk.version>
                      <javaVersion>11</javaVersion>
                      <jdkVersion>11</jdkVersion>
                      <maven.compiler.source>11</maven.compiler.source>
                      <maven.compiler.target>11</maven.compiler.target>
                      <maven.compiler.release>11</maven.compiler.release>
                      <release.version>11</release.version>
                  </properties>
              </project>
              """,
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0.0</version>
                  <modelVersion>4.0</modelVersion>
                  <properties>
                      <java.version>17</java.version>
                      <jdk.version>17</jdk.version>
                      <javaVersion>17</javaVersion>
                      <jdkVersion>17</jdkVersion>
                      <maven.compiler.source>17</maven.compiler.source>
                      <maven.compiler.target>17</maven.compiler.target>
                      <maven.compiler.release>17</maven.compiler.release>
                      <release.version>17</release.version>
                  </properties>
              </project>
              """
          )
        );
    }

    @Test
    void basicWithVariables() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0.0</version>
                  <modelVersion>4.0</modelVersion>
                  <properties>
                      <java.version>${release.version}</java.version>
                      <jdk.version>11</jdk.version>
                      <javaVersion>${release.version}</javaVersion>
                      <jdkVersion>${jdk.version}</jdkVersion>
                      <maven.compiler.source>${maven.compiler.release}</maven.compiler.source>
                      <maven.compiler.target>${maven.compiler.release}</maven.compiler.target>
                      <maven.compiler.release>11</maven.compiler.release>
                      <release.version>11</release.version>
                  </properties>
              </project>
              """,
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0.0</version>
                  <modelVersion>4.0</modelVersion>
                  <properties>
                      <java.version>${release.version}</java.version>
                      <jdk.version>17</jdk.version>
                      <javaVersion>${release.version}</javaVersion>
                      <jdkVersion>${jdk.version}</jdkVersion>
                      <maven.compiler.source>${maven.compiler.release}</maven.compiler.source>
                      <maven.compiler.target>${maven.compiler.release}</maven.compiler.target>
                      <maven.compiler.release>17</maven.compiler.release>
                      <release.version>17</release.version>
                  </properties>
              </project>
              """)
        );
    }

    @Test
    void updateLocalParent() {
        rewriteRun(
            //language=xml
            pomXml(
                """
                  <project>
                      <groupId>com.example</groupId>
                      <artifactId>example-parent</artifactId>
                      <version>1.0.0</version>
                      <modelVersion>4.0</modelVersion>
                      <properties>
                          <java.version>11</java.version>
                          <jdk.version>11</jdk.version>
                          <javaVersion>11</javaVersion>
                          <jdkVersion>11</jdkVersion>
                          <maven.compiler.source>11</maven.compiler.source>
                          <maven.compiler.target>11</maven.compiler.target>
                          <maven.compiler.release>11</maven.compiler.release>
                          <release.version>11</release.version>
                      </properties>
                  </project>
                  """,
                """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>example-parent</artifactId>
                    <version>1.0.0</version>
                    <modelVersion>4.0</modelVersion>
                    <properties>
                        <java.version>17</java.version>
                        <jdk.version>17</jdk.version>
                        <javaVersion>17</javaVersion>
                        <jdkVersion>17</jdkVersion>
                        <maven.compiler.source>17</maven.compiler.source>
                        <maven.compiler.target>17</maven.compiler.target>
                        <maven.compiler.release>17</maven.compiler.release>
                        <release.version>17</release.version>
                    </properties>
                </project>
                """),
            mavenProject("example-child",
                //language=xml
                pomXml(
                    """
                      <project>
                          <parent>
                              <groupId>com.example</groupId>
                              <artifactId>example-parent</artifactId>
                              <version>1.0.0</version>
                          </parent>
                          <groupId>com.example</groupId>
                          <artifactId>example-child</artifactId>
                          <version>1.0.0</version>
                          <modelVersion>4.0</modelVersion>
                      </project>
                      """
                )
            )
        );
    }

    @Test
    void doNothingForExplicitPluginConfiguration() {
        // Use UseMavenCompilerPluginReleaseConfiguration for this case
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>example-child</artifactId>
                <version>1.0.0</version>
                <modelVersion>4.0</modelVersion>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.8.0</version>
                      <configuration>
                        <release>11</release>
                        <source>11</source>
                        <target>11</target>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/514")
    void addReleaseIfNoOtherChangeIsMade() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>example-child</artifactId>
                  <version>1.0.0</version>
                  <modelVersion>4.0</modelVersion>
              </project>
              """,
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>example-child</artifactId>
                  <version>1.0.0</version>
                  <modelVersion>4.0</modelVersion>
                  <properties>
                      <maven.compiler.release>17</maven.compiler.release>
                  </properties>
              </project>
              """
          )
        );
    }

    @Test
    void springBoot3ParentToJava17() {
        // Spring Boot Starter Parent already enforces Java 17
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>3.3.3</version>
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
    void springBoot3ParentToJava21() {
        rewriteRun(
          spec -> spec.recipe(new UpdateMavenProjectPropertyJavaVersion(21)),
          pomXml(
            //language=xml
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>3.3.3</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            //language=xml
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>3.3.3</version>
                  <relativePath/> <!-- lookup parent from repository -->
                </parent>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <java.version>21</java.version>
                </properties>
              </project>
              """
          )
        );
    }
}
