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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.RecipeTest
import java.io.File
import java.nio.file.Path

class ChangeDependencyVersionTest : RecipeTest {
    override val parser: MavenParser = MavenParser.builder().resolveOptional(false).build()

    override val recipe = ChangeDependencyVersion("com.google.guava", "guava", "29.0-jre")

    @Test
    fun fixedVersion() = assertChanged(
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
                  <version>28.2-jre</version>
                  <scope>test</scope>
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
                  <version>29.0-jre</version>
                  <scope>test</scope>
                </dependency>
              </dependencies>
            </project>
        """
    )

    @Test
    fun propertyVersion() = assertChanged(
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
               
              <properties>
                <guava.version>28.2-jre</guava.version>
              </properties>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                  <groupId>com.google.guava</groupId>
                  <artifactId>guava</artifactId>
                  <version>${"$"}{guava.version}</version>
                  <scope>test</scope>
                </dependency>
              </dependencies>
            </project>
        """,
        after = """
            <project>
              <modelVersion>4.0.0</modelVersion>
               
              <properties>
                <guava.version>29.0-jre</guava.version>
              </properties>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              
              <dependencies>
                <dependency>
                  <groupId>com.google.guava</groupId>
                  <artifactId>guava</artifactId>
                  <version>${"$"}{guava.version}</version>
                  <scope>test</scope>
                </dependency>
              </dependencies>
            </project>
        """
    )

    @Test
    fun inDependencyManagementSection(@TempDir tempDir: Path) {
        val myModuleProject = File(tempDir.toFile(), "my-module")
        myModuleProject.mkdirs()

        assertChanged(
            dependsOn = arrayOf(
                File(myModuleProject, "pom.xml").apply {
                    writeText(
                        """
                            <project>
                              <modelVersion>4.0.0</modelVersion>
                             
                              <parent>
                                <groupId>com.mycompany.app</groupId>
                                <artifactId>my-app</artifactId>
                                <version>1</version>
                              </parent>
                            
                              <artifactId>my-module</artifactId>
                            
                              <dependencies>
                                <dependency>
                                  <groupId>com.google.guava</groupId>
                                  <artifactId>guava</artifactId>
                                </dependency>
                              </dependencies>
                            </project>
                        """.trimIndent().trim()
                    )
                }
            ),
            before = File(tempDir.toFile(), "pom.xml").apply {
                writeText(
                    """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                         
                          <packaging>pom</packaging>
                          <groupId>com.mycompany.app</groupId>
                          <artifactId>my-app</artifactId>
                          <version>1</version>
                          
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava</artifactId>
                                <version>28.2-jre</version>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
                        </project>
                    """.trimIndent().trim()
                )
            },
            after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                 
                  <packaging>pom</packaging>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>29.0-jre</version>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
            """
        )
    }

    @Test
    fun inParentProperty(@TempDir tempDir: Path) {
        val myModuleProject = File(tempDir.toFile(), "my-module")
        myModuleProject.mkdirs()

        assertChanged(
            dependsOn = arrayOf(
                File(myModuleProject, "pom.xml").apply {
                    writeText(
                        """
                            <project>
                              <modelVersion>4.0.0</modelVersion>
                             
                              <parent>
                                <groupId>com.mycompany.app</groupId>
                                <artifactId>my-app</artifactId>
                                <version>1</version>
                              </parent>
                            
                              <artifactId>my-module</artifactId>
                            
                              <dependencies>
                                <dependency>
                                  <groupId>com.google.guava</groupId>
                                  <artifactId>guava</artifactId>
                                  <version>${"$"}{guava.version}</version>
                                </dependency>
                              </dependencies>
                            </project>
                        """.trimIndent().trim()
                    )
                }
            ),
            before = File(tempDir.toFile(), "pom.xml").apply {
                writeText(
                    """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                         
                          <packaging>pom</packaging>
                          <groupId>com.mycompany.app</groupId>
                          <artifactId>my-app</artifactId>
                          <version>1</version>
                          
                          <properties>
                            <guava.version>28.2-jre</guava.version>
                          </properties>
                        </project>
                    """.trimIndent().trim()
                )
            },
            after = """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                 
                  <packaging>pom</packaging>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <properties>
                    <guava.version>29.0-jre</guava.version>
                  </properties>
                </project>
            """
        )
    }

    @Test
    fun propertyInDependencyManagementSection() = assertChanged(
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
             
              <packaging>pom</packaging>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            
              <properties>
                <guava.version>28.2-jre</guava.version>
              </properties>
              
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>${"$"}{guava.version}</version>
                  </dependency>
                </dependencies>
              </dependencyManagement>
            </project>
        """,
        after = """
            <project>
              <modelVersion>4.0.0</modelVersion>
             
              <packaging>pom</packaging>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            
              <properties>
                <guava.version>29.0-jre</guava.version>
              </properties>
              
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>${"$"}{guava.version}</version>
                  </dependency>
                </dependencies>
              </dependencyManagement>
            </project>
        """
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = ChangeDependencyVersion(null, null, null)
        var valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse()
        Assertions.assertThat(valid.failures()).hasSize(3)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("artifactId")
        Assertions.assertThat(valid.failures()[1].property).isEqualTo("groupId")
        Assertions.assertThat(valid.failures()[2].property).isEqualTo("newVersion")

        recipe = ChangeDependencyVersion(null, "rewrite-maven", "1.0.1")
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse()
        Assertions.assertThat(valid.failures()).hasSize(1)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("groupId")

        recipe = ChangeDependencyVersion("org.openrewrite", null, "1.0.1")
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse()
        Assertions.assertThat(valid.failures()).hasSize(1)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("artifactId")

        recipe = ChangeDependencyVersion("org.openrewrite", "rewrite-maven", null)
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isFalse()
        Assertions.assertThat(valid.failures()).hasSize(1)
        Assertions.assertThat(valid.failures()[0].property).isEqualTo("newVersion")

        recipe = ChangeDependencyVersion("org.openrewrite", "rewrite-maven", "1.0.1")
        valid = recipe.validate()
        Assertions.assertThat(valid.isValid).isTrue()
    }
}
