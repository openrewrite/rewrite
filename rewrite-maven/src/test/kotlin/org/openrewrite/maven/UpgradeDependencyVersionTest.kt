/*
 * Copyright 2021 the original author or authors.
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.openrewrite.Issue

class UpgradeDependencyVersionTest : MavenRecipeTest {

    @Test
    fun updateManagedDependencyVersion() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "org.junit.jupiter",
            "junit-jupiter-api",
            "5.7.2", null, null),
        before = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter-api</artifactId>
                            <version>5.6.2</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """,
        after = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter-api</artifactId>
                            <version>5.7.2</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """
    )

    @ParameterizedTest
    @CsvSource(value = ["com.google.guava:guava", "*:*"], delimiter = ':')
    fun upgradeVersion(groupId: String, artifactId: String) = assertChanged(
        recipe = UpgradeDependencyVersion(
            groupId,
            artifactId,
            "latest.patch",
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
                  <groupId>com.google.guava</groupId>
                  <artifactId>guava</artifactId>
                  <version>13.0</version>
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
                  <version>13.0.1</version>
                </dependency>
              </dependencies>
            </project>
        """
    )

    @Test
    fun overrideManagedDependency() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "com.google.guava",
            "guava",
            "14.0",
            "",
            true
        ),
        dependsOn = arrayOf("""
            <project>
                <packaging>pom</packaging>
                <groupId>com.mycompany</groupId>
                <artifactId>my-parent</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>13.0.1</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """
        ),
        before = """
            <project>
                <parent>
                    <groupId>com.mycompany</groupId>
                    <artifactId>my-parent</artifactId>
                    <version>1</version>
                </parent>
                <groupId>com.mycompany</groupId>
                <artifactId>my-child</artifactId>
                <version>1</version>
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                    </dependency>
                </dependencies>
            </project>
            """,
        after = """
            <project>
                <parent>
                    <groupId>com.mycompany</groupId>
                    <artifactId>my-parent</artifactId>
                    <version>1</version>
                </parent>
                <groupId>com.mycompany</groupId>
                <artifactId>my-child</artifactId>
                <version>1</version>
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>14.0</version>
                    </dependency>
                </dependencies>
            </project>
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/739")
    fun upgradeVersionWithGroupIdAndArtifactIdDefinedAsProperty() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "io.quarkus",
            "quarkus-universe-bom",
            "1.13.7.Final",
            null,
            null
        ),
        before = """
            <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                    <quarkus.platform.artifact-id>quarkus-universe-bom</quarkus.platform.artifact-id>
                    <quarkus.platform.group-id>io.quarkus</quarkus.platform.group-id>
                    <quarkus.platform.version>1.11.7.Final</quarkus.platform.version>
                </properties>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>${'$'}{quarkus.platform.group-id}</groupId>
                            <artifactId>${'$'}{quarkus.platform.artifact-id}</artifactId>
                            <version>${'$'}{quarkus.platform.version}</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """,
        after = """
            <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                    <quarkus.platform.artifact-id>quarkus-universe-bom</quarkus.platform.artifact-id>
                    <quarkus.platform.group-id>io.quarkus</quarkus.platform.group-id>
                    <quarkus.platform.version>1.13.7.Final</quarkus.platform.version>
                </properties>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>${'$'}{quarkus.platform.group-id}</groupId>
                            <artifactId>${'$'}{quarkus.platform.artifact-id}</artifactId>
                            <version>${'$'}{quarkus.platform.version}</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """
    )

    @Test
    fun upgradeVersionSuccessively() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "com.google.guava",
            "*",
            "28.x",
            "-jre",
            null
        ).doNext(
            UpgradeDependencyVersion(
                "com.google.guava",
                "*",
                "29.x",
                "-jre",
                null
            )
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
                  <version>27.0-jre</version>
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
                </dependency>
              </dependencies>
            </project>
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/565")
    fun propertiesInDependencyGroupIdAndArtifactId() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "com.google.guava",
            "*",
            "latest.patch",
            null,
            null
        ),
        before = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                    <dependency.group-id>com.google.guava</dependency.group-id>
                    <dependency.artifact-id>guava</dependency.artifact-id>
                    <dependency.version>13.0</dependency.version>
                </properties>
                <dependencies>
                    <dependency>
                        <groupId>${'$'}{dependency.group-id}</groupId>
                        <artifactId>${'$'}{dependency.artifact-id}</artifactId>
                        <version>${'$'}{dependency.version}</version>
                    </dependency>
                </dependencies>
            </project>
        """,
        after = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                    <dependency.group-id>com.google.guava</dependency.group-id>
                    <dependency.artifact-id>guava</dependency.artifact-id>
                    <dependency.version>13.0.1</dependency.version>
                </properties>
                <dependencies>
                    <dependency>
                        <groupId>${'$'}{dependency.group-id}</groupId>
                        <artifactId>${'$'}{dependency.artifact-id}</artifactId>
                        <version>${'$'}{dependency.version}</version>
                    </dependency>
                </dependencies>
            </project>
        """
    )

    @Test
    fun upgradeGuava() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "com.google.guava",
            "*",
            "25-28",
            "-android",
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
                  <version>28.0-android</version>
                </dependency>
              </dependencies>
            </project>
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1334")
    fun upgradeGuavaWithExplicitBlankVersionPattern() = assertChanged(
            recipe = UpgradeDependencyVersion(
                    "com.google.guava",
                    "*",
                    "latest.release",
                    "",
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
                  <version>22.0</version>
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
                      <version>23.0</version>
                    </dependency>
                  </dependencies>
                </project>
            """
    )

    @Test
    fun upgradeManagedInParent() {
        val upgradeRecipe = UpgradeDependencyVersion(
            "com.google.guava",
            "*",
            "25-28",
            "-jre",
            false
        )
        val parent = """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <packaging>pom</packaging>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                        <guava.version>25.0-jre</guava.version>
                    </properties>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava</artifactId>
                                <version>${'$'}{guava.version}</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """.trimIndent()
        val child = """
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
                        </dependency>
                    </dependencies>
                </project>
        """.trimIndent()

        //Parent should be upgraded
        assertChanged(
            recipe = upgradeRecipe,
            dependsOn = arrayOf(child),
            before = parent,
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
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava</artifactId>
                                <version>${'$'}{guava.version}</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """
        )
        //Child should be left alone.
        assertUnchanged(
            recipe = upgradeRecipe,
            dependsOn = arrayOf(parent),
            before = child,
        )
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/891")
    fun upgradeDependencyOnlyTargetsSpecificDependencyProperty() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "com.google.guava",
            "*",
            "25-28",
            "-jre",
            null
        ),
        before = """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <packaging>pom</packaging>
                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                        <guava.version>25.0-jre</guava.version>
                        <spring.version>5.3.9</spring.version>
                        <spring.artifact-id>spring-jdbc</spring.artifact-id>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>${'$'}{guava.version}</version>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework</groupId>
                            <artifactId>${'$'}{spring.artifact-id}</artifactId>
                            <version>${'$'}{spring.version}</version>
                        </dependency>
                    </dependencies>
                </project>
            """,
        after = """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <packaging>pom</packaging>
                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                        <guava.version>28.0-jre</guava.version>
                        <spring.version>5.3.9</spring.version>
                        <spring.artifact-id>spring-jdbc</spring.artifact-id>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>${'$'}{guava.version}</version>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework</groupId>
                            <artifactId>${'$'}{spring.artifact-id}</artifactId>
                            <version>${'$'}{spring.version}</version>
                        </dependency>
                    </dependencies>
                </project>
        """
    )

    @Test
    fun upgradeBomImport() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "io.micronaut",
            "micronaut-bom",
            "3.0.0-M5",
            null,
            null
        ),
        before = """
            <project>
              <packaging>pom</packaging>
              <groupId>org.openrewrite.example</groupId>
              <artifactId>my-app-server</artifactId>
              <version>1</version>
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>io.micronaut</groupId>
                    <artifactId>micronaut-bom</artifactId>
                    <version>2.5.10</version>
                    <type>pom</type>
                    <scope>import</scope>
                  </dependency>
                </dependencies>
              </dependencyManagement>
            </project>
        """,
        after = """
            <project>
              <packaging>pom</packaging>
              <groupId>org.openrewrite.example</groupId>
              <artifactId>my-app-server</artifactId>
              <version>1</version>
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>io.micronaut</groupId>
                    <artifactId>micronaut-bom</artifactId>
                    <version>3.0.0-M5</version>
                    <type>pom</type>
                    <scope>import</scope>
                  </dependency>
                </dependencies>
              </dependencyManagement>
            </project>
        """
    )

    @Test
    fun upgradeAllManagedDependenciesToPatchReleases() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "*",
            "*",
            "latest.patch",
            null,
            null
        ),
        before = """
            <project>
                <packaging>pom</packaging>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app-server</artifactId>
                <version>1</version>
                <properties>
                    <micronaut.version>2.5.10</micronaut.version>
                </properties>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>io.micronaut</groupId>
                            <artifactId>micronaut-bom</artifactId>
                            <version>${'$'}{micronaut.version}</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                        <dependency>
                            <groupId>javax.servlet</groupId>
                            <artifactId>javax.servlet-api</artifactId>
                            <version>4.0.0</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """,
        after = """
            <project>
                <packaging>pom</packaging>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app-server</artifactId>
                <version>1</version>
                <properties>
                    <micronaut.version>2.5.13</micronaut.version>
                </properties>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>io.micronaut</groupId>
                            <artifactId>micronaut-bom</artifactId>
                            <version>${'$'}{micronaut.version}</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                        <dependency>
                            <groupId>javax.servlet</groupId>
                            <artifactId>javax.servlet-api</artifactId>
                            <version>4.0.1</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """
    )

    @Test
    fun upgradeAllDependenciesToPatchReleases() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "*",
            "*",
            "latest.patch",
            null,
            null
        ),
        before = """
            <project>
                <packaging>pom</packaging>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app-server</artifactId>
                <version>1</version>
                <properties>
                    <guava.version>28.0-jre</guava.version>
                    <spring.version>5.3.4</spring.version>
                    <spring.artifact-id>spring-jdbc</spring.artifact-id>
                </properties>
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>${'$'}{guava.version}</version>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>${'$'}{spring.artifact-id}</artifactId>
                        <version>${'$'}{spring.version}</version>
                    </dependency>
                    <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-core</artifactId>
                        <version>2.13.1</version>
                    </dependency>
                </dependencies>
            </project>
        """,
        after = """
            <project>
                <packaging>pom</packaging>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app-server</artifactId>
                <version>1</version>
                <properties>
                    <guava.version>28.0-jre</guava.version>
                    <spring.version>5.3.22</spring.version>
                    <spring.artifact-id>spring-jdbc</spring.artifact-id>
                </properties>
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>${'$'}{guava.version}</version>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>${'$'}{spring.artifact-id}</artifactId>
                        <version>${'$'}{spring.version}</version>
                    </dependency>
                    <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-core</artifactId>
                        <version>2.13.4</version>
                    </dependency>
                </dependencies>
            </project>
        """
    )

    @Test
    fun dependencyManagementResolvedFromProperty() = assertChanged(
            recipe = UpgradeDependencyVersion(
                "io.micronaut",
                "micronaut-bom",
                "3.0.0-M5",
                null,
                null
            ),
            before = """
                <project>
                  <artifactId>my-app-server</artifactId>

                  <properties>
                    <micronaut.version>2.5.11</micronaut.version>
                  </properties>

                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>io.micronaut</groupId>
                        <artifactId>micronaut-bom</artifactId>
                        <version>${'$'}{micronaut.version}</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
            """.trimIndent(),
            after = """
                <project>
                  <artifactId>my-app-server</artifactId>

                  <properties>
                    <micronaut.version>3.0.0-M5</micronaut.version>
                  </properties>

                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>io.micronaut</groupId>
                        <artifactId>micronaut-bom</artifactId>
                        <version>${'$'}{micronaut.version}</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
            """
    )

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

    @Issue("https://github.com/openrewrite/rewrite/issues/1248")
    @Test
    fun updateWithExactVersionRange() = assertChanged(
        recipe = UpgradeDependencyVersion(
            "junit",
            "junit",
            "4.13",
            null,
            false
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            
              <dependencies>
                <dependency>
                  <groupId>junit</groupId>
                  <artifactId>junit</artifactId>
                  <version>[4.11]</version>
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
                  <groupId>junit</groupId>
                  <artifactId>junit</artifactId>
                  <version>4.13</version>
                </dependency>
              </dependencies>
            </project>
        """
    )
}
