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
package org.openrewrite.maven

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe

class MigrateQuarkusMavenPluginNativeImageGoalTest : MavenRecipeTest {
    override val recipe: Recipe
        get() = MigrateQuarkusMavenPluginNativeImageGoal()

    @Test
    fun removeQuarkusMavenPluginNativeImageGoalTest() = assertChanged(
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.openrewrite.example</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <properties>
                <quarkus-plugin.version>1.13.5.Final</quarkus-plugin.version>
              </properties>
              <build>
                <plugins>
                  <plugin>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-maven-plugin</artifactId>
                    <version>${"$"}{quarkus-plugin.version}</version>
                    <extensions>true</extensions>
                    <executions>
                      <execution>
                        <goals>
                          <goal>build</goal>
                          <goal>native-image</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                </plugins>
              </build>
            </project>
        """,
        after = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.openrewrite.example</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <properties>
                <quarkus-plugin.version>1.13.5.Final</quarkus-plugin.version>
              </properties>
              <build>
                <plugins>
                  <plugin>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-maven-plugin</artifactId>
                    <version>${"$"}{quarkus-plugin.version}</version>
                    <extensions>true</extensions>
                    <executions>
                      <execution>
                        <goals>
                          <goal>build</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                </plugins>
              </build>
            </project>
        """
    )

    @Test
    fun addPropertyToNativeProfile() = assertChanged(
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.openrewrite.example</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <properties>
                <quarkus-plugin.version>1.13.5.Final</quarkus-plugin.version>
              </properties>
              <build>
                <plugins>
                  <plugin>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-maven-plugin</artifactId>
                    <version>${"$"}{quarkus-plugin.version}</version>
                    <extensions>true</extensions>
                    <executions>
                      <execution>
                        <goals>
                          <goal>build</goal>
                          <goal>native-image</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                </plugins>
              </build>
              <profiles>
                <profile>
                  <id>native</id>
                  <activation>
                    <property>
                      <name>native</name>
                    </property>
                  </activation>
                </profile>
              </profiles>
            </project>
        """,
        after = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.openrewrite.example</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <properties>
                <quarkus-plugin.version>1.13.5.Final</quarkus-plugin.version>
              </properties>
              <build>
                <plugins>
                  <plugin>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-maven-plugin</artifactId>
                    <version>${"$"}{quarkus-plugin.version}</version>
                    <extensions>true</extensions>
                    <executions>
                      <execution>
                        <goals>
                          <goal>build</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                </plugins>
              </build>
              <profiles>
                <profile>
                  <id>native</id>
                  <activation>
                    <property>
                      <name>native</name>
                    </property>
                  </activation>
                  <properties>
                    <quarkus.package.type>native</quarkus.package.type>
                  </properties>
                </profile>
              </profiles>
            </project>
        """
    )
}
