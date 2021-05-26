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

class UpgradeDependencyVersionTest : MavenRecipeTest {
    @Test
    fun trustParent() = assertUnchanged(
        recipe = UpgradeDependencyVersion(
            "junit",
            "junit",
            "4.x",
            null,
            true
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
                <groupId>com.fasterxml.jackson</groupId>
                <artifactId>jackson-parent</artifactId>
                <version>2.12</version>
              </parent>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                  <groupId>junit</groupId>
                  <artifactId>junit</artifactId>
                </dependency>
              </dependencies>
            </project>
        """
    )

    @Test
    fun upgradeVersion() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "org.springframework.boot",
            null,
            "~1.5",
            null,
            null
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot</artifactId>
                  <version>1.5.1.RELEASE</version>
                </dependency>
              </dependencies>
            </project>
        """,
        after = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot</artifactId>
                  <version>1.5.22.RELEASE</version>
                </dependency>
              </dependencies>
            </project>
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/565")
    @Disabled
    fun handlesPropertiesInDependencyGroupIdAndArtifactId() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "org.springframework.boot",
            null,
            "~1.5",
            null,
            null
        ),
        before = """
            <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <properties>
                    <dependency.group-id>org.springframework.boot</dependency.group-id>
                    <dependency.artifact-id>spring-boot</dependency.artifact-id>
                    <dependency.version>1.5.1.RELEASE</dependency.version>
                </properties>

                <dependencies>
                    <dependency>
                        <groupId>${"$"}{dependency.group-id}</groupId>
                        <artifactId>${"$"}{dependency.artifact-id}</artifactId>
                        <version>${"$"}{dependency.version}</version>
                    </dependency>
                </dependencies>
            </project>
        """,
        after = """
            <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <properties>
                    <dependency.group-id>org.springframework.boot</dependency.group-id>
                    <dependency.artifact-id>spring-boot</dependency.artifact-id>
                    <dependency.version>1.5.22.RELEASE</dependency.version>
                </properties>

                <dependencies>
                    <dependency>
                        <groupId>${"$"}{dependency.group-id}</groupId>
                        <artifactId>${"$"}{dependency.artifact-id}</artifactId>
                        <version>${"$"}{dependency.version}</version>
                    </dependency>
                </dependencies>
            </project>
        """
    )

    @Test
    fun upgradeGuava() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "com.google.guava",
            null as String?,
            "25-28",
            "-jre",
            null
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                  <groupId>com.google.guava</groupId>
                  <artifactId>guava</artifactId>
                  <version>25.0-android</version>
                </dependency>
              </dependencies>
            </project>
        """,
        after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>28.0-jre</version>
                    </dependency>
                  </dependencies>
                </project>
            """
    )

    @Test
    fun upgradeGuavaInParent(@TempDir tempDir: Path) {
        val parent = tempDir.resolve("pom.xml")
        val server = tempDir.resolve("server/pom.xml")
        server.toFile().parentFile.mkdirs()

        parent.toFile().writeText(
            """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                
                  <packaging>pom</packaging>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <properties>
                    <guava.version>25.0-jre</guava.version>
                  </properties>
                </project>
            """.trimIndent()
        )

        server.toFile().writeText(
            """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  
                  <parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </parent>
                
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app-server</artifactId>
                  <version>1</version>
                  
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>${"$"}{guava.version}</version>
                    </dependency>
                  </dependencies>
                </project>
            """.trimIndent()
        )

        assertChanged(
            recipe = UpgradeDependencyVersion(
                "com.google.guava",
                null,
                "25-28",
                "-jre",
                null
            ),
            dependsOn = arrayOf(server.toFile()),
            before = parent.toFile(),
            after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                
                  <packaging>pom</packaging>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <properties>
                    <guava.version>28.0-jre</guava.version>
                  </properties>
                </project>
            """
        )
    }

    @Test
    fun upgradeToExactVersion() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "org.thymeleaf",
            "thymeleaf-spring5",
            "3.0.12.RELEASE",
            null,
            null
        ),
        before = """
            <project>
            <modelVersion>4.0.0</modelVersion>
            
                <packaging>pom</packaging>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
    
                <dependencies>
                    <dependency>
                        <groupId>org.thymeleaf</groupId>
                        <artifactId>thymeleaf-spring5</artifactId>
                        <version>3.0.8.RELEASE</version>
                    </dependency>
                </dependencies>
            </project>
        """,
        after = """
            <project>
            <modelVersion>4.0.0</modelVersion>
            
                <packaging>pom</packaging>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
            
                <dependencies>
                    <dependency>
                        <groupId>org.thymeleaf</groupId>
                        <artifactId>thymeleaf-spring5</artifactId>
                        <version>3.0.12.RELEASE</version>
                    </dependency>
                </dependencies>
            </project>
        """
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = UpgradeDependencyVersion(null, null, null, null, null)
        var valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse()
        Assertions.assertThat(valid.failures()).hasSize(2)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("groupId")
        Assertions.assertThat(valid.failures()[1].property).isEqualTo("newVersion")

        recipe = UpgradeDependencyVersion(null, "rewrite-maven", "latest.release", null, null)
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse()
        Assertions.assertThat(valid.failures()).hasSize(1)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("groupId")

        recipe = UpgradeDependencyVersion("org.openrewrite", null, null, null, null)
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse()
        Assertions.assertThat(valid.failures()).hasSize(1)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("newVersion")

        recipe = UpgradeDependencyVersion("org.openrewrite", "rewrite-maven", "latest.release", null, null)
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isTrue()

        recipe = UpgradeDependencyVersion("org.openrewrite", "rewrite-maven", "latest.release", "123", null)
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isTrue()

        recipe = UpgradeDependencyVersion("org.openrewrite", "rewrite-maven", "1.0.0", null, null)
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isTrue()
    }
}
