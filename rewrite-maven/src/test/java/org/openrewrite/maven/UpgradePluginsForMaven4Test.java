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
import org.openrewrite.semver.Semver;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradePluginsForMaven4Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/maven.yml", "org.openrewrite.maven.UpgradePluginsForMaven4");
    }

    /**
     * The selected version tracks the latest release, so tests capture whatever was chosen and assert only that it
     * satisfies the range the recipe asks for.
     */
    private static String selectedVersion(String actual, String regex, String expectedRange) {
        Matcher matcher = Pattern.compile(regex).matcher(actual);
        assertThat(matcher.find()).as("no version matched %s in:%n%s", regex, actual).isTrue();
        String version = matcher.group(1);
        assertThat(requireNonNull(Semver.validate(expectedRange, null).getValue()).isValid(null, version))
          .as("%s does not satisfy %s", version, expectedRange)
          .isTrue();
        return version;
    }

    @DocumentExample
    @Test
    void upgradeIncompatiblePlugin() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.8.1</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            spec -> spec.after(actual -> """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>%s</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """.formatted(selectedVersion(actual, "<version>(3\\.\\d+\\.\\d+)</version>", "[3.11.0,4.0.0)")))
          )
        );
    }

    @Test
    void upgradePluginAlreadyAtTheFloorToTheLatestRelease() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.11.0</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            spec -> spec.after(actual -> """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>%s</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """.formatted(selectedVersion(actual, "<version>(3\\.\\d+\\.\\d+)</version>", "(3.11.0,4.0.0)")))
          )
        );
    }

    @Test
    void retainPluginWithoutVersion() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeInPluginManagement() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <pluginManagement>
                          <plugins>
                              <plugin>
                                  <groupId>org.apache.maven.plugins</groupId>
                                  <artifactId>maven-surefire-plugin</artifactId>
                                  <version>3.0.0</version>
                              </plugin>
                          </plugins>
                      </pluginManagement>
                  </build>
              </project>
              """,
            spec -> spec.after(actual -> """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <pluginManagement>
                          <plugins>
                              <plugin>
                                  <groupId>org.apache.maven.plugins</groupId>
                                  <artifactId>maven-surefire-plugin</artifactId>
                                  <version>%s</version>
                              </plugin>
                          </plugins>
                      </pluginManagement>
                  </build>
              </project>
              """.formatted(selectedVersion(actual, "<version>(3\\.\\d+\\.\\d+)</version>", "[3.5.2,4.0.0)")))
          )
        );
    }

    @Test
    void upgradeVersionHeldInProperty() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1.0.0</version>
                  <properties>
                      <enforcer.version>3.0.0</enforcer.version>
                  </properties>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-enforcer-plugin</artifactId>
                              <version>${enforcer.version}</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            spec -> spec.after(actual -> """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1.0.0</version>
                  <properties>
                      <enforcer.version>%s</enforcer.version>
                  </properties>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-enforcer-plugin</artifactId>
                              <version>${enforcer.version}</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """.formatted(selectedVersion(actual, "<enforcer.version>(3\\.\\d+\\.\\d+)</enforcer.version>", "[3.5.0,4.0.0)")))
          )
        );
    }

    @Test
    void migrateUnmaintainedScalaPlugin() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.scala-tools</groupId>
                              <artifactId>maven-scala-plugin</artifactId>
                              <version>2.15.2</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            spec -> spec.after(actual -> """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>net.alchim31.maven</groupId>
                              <artifactId>scala-maven-plugin</artifactId>
                              <version>%s</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """.formatted(selectedVersion(actual, "<version>(4\\.\\d+\\.\\d+)</version>", "[4.9.5,5.0.0)")))
          )
        );
    }

    @Test
    void upgradeQuarkusPluginOnlyToItsFloor() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>io.quarkus</groupId>
                              <artifactId>quarkus-maven-plugin</artifactId>
                              <version>3.20.0</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>io.quarkus</groupId>
                              <artifactId>quarkus-maven-plugin</artifactId>
                              <version>3.26.0</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );
    }

    @Test
    void retainQuarkusPluginAboveItsFloor() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>io.quarkus</groupId>
                              <artifactId>quarkus-maven-plugin</artifactId>
                              <version>3.28.0</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );
    }
}
