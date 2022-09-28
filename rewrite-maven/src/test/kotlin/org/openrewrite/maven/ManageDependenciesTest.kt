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
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Tree
import org.openrewrite.java.marker.JavaProject
import java.nio.file.Path

class ManageDependenciesTest : MavenRecipeTest {
    private val javaProject = JavaProject(Tree.randomId(), "myproject", null)

    @Test
    fun createDependencyManagementWithDependencyWhenNoneExists() = assertChanged(
        recipe = ManageDependencies(
            "org.junit.jupiter",
            "*",
            false, false),
        before = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                    <dependency>
                        <groupId>org.junit.jupiter</groupId>
                        <artifactId>junit-jupiter-api</artifactId>
                        <version>5.6.2</version>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
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
                            <version>5.6.2</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                <dependencies>
                    <dependency>
                        <groupId>org.junit.jupiter</groupId>
                        <artifactId>junit-jupiter-api</artifactId>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
            </project>
        """
    )

    @Test
    fun deferToDependencyManagementWhenDependencyIsAlreadyManaged() = assertChanged(
        recipe = ManageDependencies(
            "junit",
            "junit",
            false, false),
        dependsOn = arrayOf(
            """
                <project>
                <groupId>com.othercompany.app</groupId>
                <artifactId>my-parent-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>junit</groupId>
                            <artifactId>junit</artifactId>
                            <version>4.13.2</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                </project>
            """.trimIndent()
        ),
        before = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <parent>
                    <groupId>com.othercompany.app</groupId>
                    <artifactId>my-parent-app</artifactId>
                    <version>1</version>
                </parent>
                <dependencies>
                    <dependency>
                        <groupId>junit</groupId>
                        <artifactId>junit</artifactId>
                        <version>4.13.2</version>
                    </dependency>
                </dependencies>
            </project>
        """,
        after = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <parent>
                    <groupId>com.othercompany.app</groupId>
                    <artifactId>my-parent-app</artifactId>
                    <version>1</version>
                </parent>
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
    fun manageToSpecefiedVersion() = assertChanged(
        recipe = ManageDependencies(
            "junit",
            "junit",
            false, false),
        before = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                    <dependency>
                        <groupId>junit</groupId>
                        <artifactId>junit</artifactId>
                        <version>4.13.2</version>
                    </dependency>
                </dependencies>
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
                            <groupId>junit</groupId>
                            <artifactId>junit</artifactId>
                            <version>4.13.2</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
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
    fun addedToTheRootPom(@TempDir tempDir: Path) {
        val project = tempDir.resolve("pom.xml")
        val serviceApi = tempDir.resolve("api/pom.xml")
        val service = tempDir.resolve("service/pom.xml")
        val core = tempDir.resolve("core/pom.xml")

        serviceApi.toFile().parentFile.mkdirs()
        service.toFile().parentFile.mkdirs()
        core.toFile().parentFile.mkdirs()

        project.toFile().writeText(
            //language=xml
            """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>project</artifactId>
                    <version>1</version>
                    <modules>
                        <module>core</module>
                        <module>service-api</module>
                        <module>service</module>
                    </modules>
                </project>
            """.trimIndent()
        )

        serviceApi.toFile().writeText(
            //language=xml
            """
                <project>
                    <parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>project</artifactId>
                        <version>1</version>
                    </parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>service-api</artifactId>
                    <version>1</version>
                </project>
            """.trimIndent()
        )

        service.toFile().writeText(
            //language=xml
            """
                <project>
                    <parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>service-api</artifactId>
                        <version>1</version>
                    </parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>service</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>junit</groupId>
                            <artifactId>junit</artifactId>
                            <version>4.13.2</version>
                        </dependency>
                    </dependencies>
                </project>
            """.trimIndent()
        )

        core.toFile().writeText(
            //language=xml
            """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>core</artifactId>
                    <version>1</version>
                </project>
            """.trimIndent()
        )
        val results = ManageDependencies("junit", "junit", true, false)
            .run(parser.parse(listOf(project, serviceApi, service, core), tempDir, InMemoryExecutionContext())
                .map { j -> j.withMarkers(j.markers.addIfAbsent(javaProject)) }
                .mapIndexed { n, maven ->
                    if (n == 0) {
                        // give the parent a different java project
                        maven.withMarkers(maven.markers.compute(javaProject) { j, _ -> j.withId(Tree.randomId()) })
                    } else maven
                }
            ).results
        Assertions.assertThat(results).hasSize(2)
        Assertions.assertThat(results[0].after!!.printAllTrimmed()).isEqualTo(
            """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>project</artifactId>
                    <version>1</version>
                    <modules>
                        <module>core</module>
                        <module>service-api</module>
                        <module>service</module>
                    </modules>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>junit</groupId>
                                <artifactId>junit</artifactId>
                                <version>4.13.2</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """.trimIndent()
        )
        Assertions.assertThat(results[1].after!!.printAllTrimmed()).isEqualTo(
            """
                <project>
                    <parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>service-api</artifactId>
                        <version>1</version>
                    </parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>service</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>junit</groupId>
                            <artifactId>junit</artifactId>
                        </dependency>
                    </dependencies>
                </project>
            """.trimIndent()
        )
    }

    @Test
    fun externalParent() = assertChanged(
        recipe = ManageDependencies(
            "org.apache.logging.log4j",
            "log4j-*",
            true, false),
        before = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.managed.test</groupId>
                <artifactId>a</artifactId>
                <version>1.0.0</version>
                <parent>
                    <groupId>com.fasterxml.jackson</groupId>
                    <artifactId>jackson-parent</artifactId>
                    <version>2.9.1</version>
                </parent>
            </project>
        """,
        dependsOn= arrayOf(
            """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.managed.test</groupId>
                    <artifactId>b</artifactId>
                    <version>1.0.0</version>
                    <parent>
                        <groupId>com.managed.test</groupId>
                        <artifactId>a</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <dependencies>
                        <dependency>
                            <groupId>junit</groupId>
                            <artifactId>junit</artifactId>
                            <version>4.11</version>
                        </dependency>
                        <dependency>
                            <groupId>org.apache.logging.log4j</groupId>
                            <artifactId>log4j-api</artifactId>
                            <version>2.17.2</version>
                        </dependency>
                    </dependencies>
                </project>            
            """
        ),
        after="""
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.managed.test</groupId>
                <artifactId>a</artifactId>
                <version>1.0.0</version>
                <parent>
                    <groupId>com.fasterxml.jackson</groupId>
                    <artifactId>jackson-parent</artifactId>
                    <version>2.9.1</version>
                </parent>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.apache.logging.log4j</groupId>
                            <artifactId>log4j-api</artifactId>
                            <version>2.17.2</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """
    )

    @Test
    fun externalManagedDependencyOverride() = assertChanged(
        recipe = ManageDependencies(
            "junit",
            "junit",
            true, false),
        before = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.managed.test</groupId>
                <artifactId>a</artifactId>
                <version>1.0.0</version>
                <parent>
                    <groupId>com.fasterxml.jackson</groupId>
                    <artifactId>jackson-parent</artifactId>
                    <version>2.9.1</version>
                </parent>
                <dependencies>
                    <dependency>
                        <groupId>junit</groupId>
                        <artifactId>junit</artifactId>
                        <version>4.11</version>
                    </dependency>
                </dependencies>
            </project>
        """,
        after = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.managed.test</groupId>
                <artifactId>a</artifactId>
                <version>1.0.0</version>
                <parent>
                    <groupId>com.fasterxml.jackson</groupId>
                    <artifactId>jackson-parent</artifactId>
                    <version>2.9.1</version>
                </parent>
                <dependencies>
                    <dependency>
                        <groupId>junit</groupId>
                        <artifactId>junit</artifactId>
                    </dependency>
                </dependencies>
            </project>
        """
    )
}
