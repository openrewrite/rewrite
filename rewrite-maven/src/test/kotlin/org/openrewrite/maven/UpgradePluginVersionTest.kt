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

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.Issue
import java.nio.file.Path

class UpgradePluginVersionTest : MavenRecipeTest {
    @Test
    fun upgradeToExactVersion() = assertChanged(
        recipe = UpgradePluginVersion(
            "io.quarkus",
            "quarkus-maven-plugin",
            "1.13.5.Final",
            null,
            null
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>

              <groupId>org.openrewrite.example</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>

              <build>
                <plugins>
                  <plugin>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-maven-plugin</artifactId>
                    <version>1.13.3.Final</version>
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

              <build>
                <plugins>
                  <plugin>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-maven-plugin</artifactId>
                    <version>1.13.5.Final</version>
                  </plugin>
                </plugins>
              </build>
            </project>
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/565")
    fun handlesPropertyResolution() = assertChanged(
        recipe = UpgradePluginVersion(
            "io.quarkus",
            "quarkus-maven-plugin",
            "1.13.5.Final",
            null,
            null
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>

              <groupId>org.openrewrite.example</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>

              <properties>
                <quarkus-plugin.group-id>io.quarkus</quarkus-plugin.group-id>
                <quarkus-plugin.artifact-id>quarkus-maven-plugin</quarkus-plugin.artifact-id>
                <quarkus-plugin.version>1.13.3.Final</quarkus-plugin.version>
              </properties>

              <build>
                <plugins>
                  <plugin>
                    <groupId>${"$"}{quarkus-plugin.group-id}</groupId>
                    <artifactId>${"$"}{quarkus-plugin.artifact-id}</artifactId>
                    <version>${"$"}{quarkus-plugin.version}</version>
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
                <quarkus-plugin.group-id>io.quarkus</quarkus-plugin.group-id>
                <quarkus-plugin.artifact-id>quarkus-maven-plugin</quarkus-plugin.artifact-id>
                <quarkus-plugin.version>1.13.5.Final</quarkus-plugin.version>
              </properties>

              <build>
                <plugins>
                  <plugin>
                    <groupId>${"$"}{quarkus-plugin.group-id}</groupId>
                    <artifactId>${"$"}{quarkus-plugin.artifact-id}</artifactId>
                    <version>${"$"}{quarkus-plugin.version}</version>
                  </plugin>
                </plugins>
              </build>
            </project>
        """
    )

    @Test
    @Issue("Should be changed/removed when this recipe supports dynamic version resolution") // todo
    fun ignorePluginWithoutExplicitVersionDeclared() = assertUnchanged(
        recipe = UpgradePluginVersion(
            "io.quarkus",
            "quarkus-maven-plugin",
            "1.13.5.Final",
            null,
            null
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>

              <groupId>org.openrewrite.example</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>

              <build>
                <plugins>
                  <plugin>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-maven-plugin</artifactId>
                  </plugin>
                </plugins>
              </build>
            </project>
        """
    )

    @Test
    fun upgradeVersionDynamicallyUsingPattern() = assertChanged(
        recipe = UpgradePluginVersion(
            "org.openrewrite.maven",
            "rewrite-maven-plugin",
            "~4.2",
            null,
            null
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>

              <groupId>org.openrewrite.example</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>

              <build>
                <plugins>
                  <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>4.2.0</version>
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

              <build>
                <plugins>
                  <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>4.2.3</version>
                  </plugin>
                </plugins>
              </build>
            </project>
        """
    )

    @Test
    fun upgradeVersionIgnoringParent(@TempDir tempDir: Path) {
        val parent = tempDir.resolve("pom.xml")
        val server = tempDir.resolve("server/pom.xml")
        server.toFile().parentFile.mkdirs()

        parent.toFile().writeText(
            """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <packaging>pom</packaging>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app-bom</artifactId>
                  <version>1</version>

                  <build>
                    <pluginManagement>
                      <plugins>
                        <plugin>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>rewrite-maven-plugin</artifactId>
                          <version>4.2.2</version>
                        </plugin>
                      </plugins>
                    </pluginManagement>
                  </build>
                </project>
            """.trimIndent()
        )

        server.toFile().writeText(
            """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <parent>
                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app-bom</artifactId>
                    <version>1</version>
                  </parent>

                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app-server</artifactId>
                  <version>1</version>

                  <build>
                    <plugins>
                      <plugin>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>rewrite-maven-plugin</artifactId>
                        <version>4.2.1</version>
                      </plugin>
                    </plugins>
                  </build>
                </project>
            """.trimIndent()
        )

        assertChanged(
            recipe = UpgradePluginVersion(
                "org.openrewrite.maven",
                "rewrite-maven-plugin",
                "4.2.x",
                null,
                null
            ),
            dependsOn = arrayOf(parent.toFile()),
            before = server.toFile(),
            after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <parent>
                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app-bom</artifactId>
                    <version>1</version>
                  </parent>

                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app-server</artifactId>
                  <version>1</version>

                  <build>
                    <plugins>
                      <plugin>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>rewrite-maven-plugin</artifactId>
                        <version>4.2.3</version>
                      </plugin>
                    </plugins>
                  </build>
                </project>
            """
        )
    }

    @Test
    @Disabled
    fun trustParent(@TempDir tempDir: Path) {
        val parent = tempDir.resolve("pom.xml")
        val server = tempDir.resolve("server/pom.xml")
        server.toFile().parentFile.mkdirs()

        parent.toFile().writeText(
            """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <packaging>pom</packaging>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app-bom</artifactId>
                  <version>1</version>

                  <build>
                    <pluginManagement>
                      <plugins>
                        <plugin>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>rewrite-maven-plugin</artifactId>
                          <version>4.2.2</version>
                        </plugin>
                      </plugins>
                    </pluginManagement>
                  </build>
                </project>
            """.trimIndent()
        )

        server.toFile().writeText(
            """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <parent>
                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app-bom</artifactId>
                    <version>1</version>
                  </parent>

                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app-server</artifactId>
                  <version>1</version>

                  <build>
                    <plugins>
                      <plugin>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>rewrite-maven-plugin</artifactId>
                        <version>4.2.1</version>
                      </plugin>
                    </plugins>
                  </build>
                </project>
            """.trimIndent()
        )

        assertChanged(
            recipe = UpgradePluginVersion(
                "org.openrewrite.maven",
                "rewrite-maven-plugin",
                "4.2.x",
                null,
                true
            ),
            dependsOn = arrayOf(parent.toFile()),
            before = server.toFile(),
            after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <parent>
                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app-bom</artifactId>
                    <version>1</version>
                  </parent>

                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app-server</artifactId>
                  <version>1</version>

                  <build>
                    <plugins>
                      <plugin>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>rewrite-maven-plugin</artifactId>
                        <version>4.2.2</version>
                      </plugin>
                    </plugins>
                  </build>
                </project>
            """
        )
    }

    @Test
    @Disabled
    fun upgradePluginInParent(@TempDir tempDir: Path) {
        val parent = tempDir.resolve("pom.xml")
        val server = tempDir.resolve("server/pom.xml")
        server.toFile().parentFile.mkdirs()

        parent.toFile().writeText(
            """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <packaging>pom</packaging>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app-bom</artifactId>
                  <version>1</version>

                  <properties>
                    <rewrite-maven-plugin.version>4.2.2</rewrite-maven-plugin.version>
                  </properties>
                </project>
            """.trimIndent()
        )

        server.toFile().writeText(
            """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <parent>
                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app-bom</artifactId>
                    <version>1</version>
                  </parent>

                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app-server</artifactId>
                  <version>1</version>

                  <build>
                    <plugins>
                      <plugin>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>rewrite-maven-plugin</artifactId>
                        <version>${"$"}{rewrite-maven-plugin.version}</version>
                      </plugin>
                    </plugins>
                  </build>
                </project>
            """.trimIndent()
        )

        assertChanged(
            recipe = UpgradePluginVersion(
                "org.openrewrite.maven",
                "rewrite-maven-plugin",
                "4.2.3",
                null,
                null
            ),
            dependsOn = arrayOf(server.toFile()),
            before = parent.toFile(),
            after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <packaging>pom</packaging>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app-bom</artifactId>
                  <version>1</version>

                  <properties>
                    <rewrite-maven-plugin.version>4.2.3</rewrite-maven-plugin.version>
                  </properties>
                </project>
            """
        )
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = UpgradePluginVersion(null, null, null, null, null)
        var valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse
        Assertions.assertThat(valid.failures()).hasSize(3)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("artifactId")
        Assertions.assertThat(valid.failures()[1].property).isEqualTo("groupId")
        Assertions.assertThat(valid.failures()[2].property).isEqualTo("newVersion")

        recipe = UpgradePluginVersion(null, "rewrite-maven-plugin", "latest.release", null, null)
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse
        Assertions.assertThat(valid.failures()).hasSize(1)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("groupId")

        recipe = UpgradePluginVersion("org.openrewrite.maven", null, null, null, null)
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse
        Assertions.assertThat(valid.failures()).hasSize(2)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("artifactId")
        Assertions.assertThat(valid.failures()[1].property).isEqualTo("newVersion")

        recipe = UpgradePluginVersion("org.openrewrite.maven", "rewrite-maven-plugin", null, null, null)
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse
        Assertions.assertThat(valid.failures()).hasSize(1)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("newVersion")

        recipe = UpgradePluginVersion("org.openrewrite.maven", "rewrite-maven-plugin", "latest.release", null, null)
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isTrue

        recipe = UpgradePluginVersion("org.openrewrite.maven", "rewrite-maven-plugin", "latest.release", null, null)
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isTrue

        recipe = UpgradePluginVersion("org.openrewrite.maven", "rewrite-maven-plugin", "1.0.0", null, null)
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isTrue
    }
}
