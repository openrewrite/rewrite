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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class RemovePluginGoalTest implements RewriteTest {

    @DocumentExample
    @Test
    void removeGoalLeavingOtherGoals() {
        rewriteRun(
          spec -> spec.recipe(new RemovePluginGoal("org.apache.maven.plugins", "maven-surefire-plugin", "verify")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <executions>
                        <execution>
                          <id>default</id>
                          <goals>
                            <goal>verify</goal>
                            <goal>test</goal>
                          </goals>
                        </execution>
                      </executions>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <executions>
                        <execution>
                          <id>default</id>
                          <goals>
                            <goal>test</goal>
                          </goals>
                        </execution>
                      </executions>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void removeGoalRemovesEmptyExecution() {
        rewriteRun(
          spec -> spec.recipe(new RemovePluginGoal("org.apache.maven.plugins", "maven-surefire-plugin", "verify")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <executions>
                        <execution>
                          <id>verify-only</id>
                          <goals>
                            <goal>verify</goal>
                          </goals>
                        </execution>
                        <execution>
                          <id>test-only</id>
                          <goals>
                            <goal>test</goal>
                          </goals>
                        </execution>
                      </executions>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <executions>
                        <execution>
                          <id>test-only</id>
                          <goals>
                            <goal>test</goal>
                          </goals>
                        </execution>
                      </executions>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void removeLastGoalRemovesExecutionsElement() {
        rewriteRun(
          spec -> spec.recipe(new RemovePluginGoal("org.apache.maven.plugins", "maven-surefire-plugin", "verify")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <executions>
                        <execution>
                          <id>verify-only</id>
                          <goals>
                            <goal>verify</goal>
                          </goals>
                        </execution>
                      </executions>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    // Rare in practice, but exercises the snapshot-iteration invariant:
    // the loop must handle mutations across multiple executions without stale references.
    void removeGoalFromMultipleExecutions() {
        rewriteRun(
          spec -> spec.recipe(new RemovePluginGoal("org.apache.maven.plugins", "maven-surefire-plugin", "verify")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <executions>
                        <execution>
                          <id>verify-and-test</id>
                          <goals>
                            <goal>verify</goal>
                            <goal>test</goal>
                          </goals>
                        </execution>
                        <execution>
                          <id>verify-only</id>
                          <goals>
                            <goal>verify</goal>
                          </goals>
                        </execution>
                      </executions>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <executions>
                        <execution>
                          <id>verify-and-test</id>
                          <goals>
                            <goal>test</goal>
                          </goals>
                        </execution>
                      </executions>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void noChangeWhenGoalNotPresent() {
        rewriteRun(
          spec -> spec.recipe(new RemovePluginGoal("org.apache.maven.plugins", "maven-surefire-plugin", "verify")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <executions>
                        <execution>
                          <id>default</id>
                          <goals>
                            <goal>test</goal>
                          </goals>
                        </execution>
                      </executions>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void noChangeWhenPluginNotPresent() {
        rewriteRun(
          spec -> spec.recipe(new RemovePluginGoal("org.apache.maven.plugins", "maven-surefire-plugin", "verify")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <executions>
                        <execution>
                          <goals>
                            <goal>compile</goal>
                          </goals>
                        </execution>
                      </executions>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void noChangeWhenExecutionHasNoGoals() {
        rewriteRun(
          spec -> spec.recipe(new RemovePluginGoal("org.apache.maven.plugins", "maven-surefire-plugin", "verify")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <executions>
                        <execution>
                          <id>default</id>
                          <phase>test</phase>
                        </execution>
                      </executions>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void globMatchOnGoal() {
        rewriteRun(
          spec -> spec.recipe(new RemovePluginGoal("org.apache.maven.plugins", "maven-surefire-plugin", "veri*")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <executions>
                        <execution>
                          <id>verify-only</id>
                          <goals>
                            <goal>verify</goal>
                          </goals>
                        </execution>
                      </executions>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void globMatchOnPluginCoordinates() {
        rewriteRun(
          spec -> spec.recipe(new RemovePluginGoal("org.apache.maven.*", "*-surefire-plugin", "verify")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <executions>
                        <execution>
                          <id>verify-only</id>
                          <goals>
                            <goal>verify</goal>
                          </goals>
                        </execution>
                      </executions>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }
}
