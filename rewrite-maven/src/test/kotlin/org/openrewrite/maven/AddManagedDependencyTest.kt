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

import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Tree
import org.openrewrite.Validated
import org.openrewrite.java.marker.JavaProject
import org.openrewrite.xml.XmlParser
import java.nio.file.Path

class AddManagedDependencyTest {
    private val xmlParser = XmlParser()
    private val mavenParser = MavenParser.builder().build()

    private val javaProject = JavaProject(Tree.randomId(), "myproject", null)

    @Test
    fun `Recipe validation`()  {
        val recipe = AddManagedDependency("org.apache.logging.log4j", "log4j-bom", "latest.release", "import",
            "pom", null, null, null, "org.apache.logging", true)
        val validated : Validated = recipe.validate()
        assertThat(validated).anyMatch { v ->
            !v.elementAt(0).isValid
        }
    }

    @Test
    fun `Does not use type manage dependency not added`() {
        assertThat(
            addManagedDependency(false,"org.apache.logging.log4j:*")
                .run(
                    mavenParser.parseWithProvenance(
                        """
                        <project>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1</version>
                        </project>
                    """.trimIndent()
                    ) + xmlParser.parseWithProvenance(
                        """
                        <project>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1</version>
                        </project>
                    """.trimIndent()
                    )
                ).results
        ).isEmpty()
    }

    @Test
    fun `Only added when using is defined and has dependency`() {
        val results = addManagedDependency(false, "org.apache.logging.log4j:*")
                .run(mavenParser.parseWithProvenance(
                        """
                        <project>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1</version>
                            <dependencies>
                                <dependency>
                                    <groupId>org.apache.logging.log4j</groupId>
                                    <artifactId>log4j-core</artifactId>
                                    <version>2.17.2</version>
                                </dependency>
                            </dependencies>
                        </project>
                    """.trimIndent()
                    ) + xmlParser.parseWithProvenance(
                        """
                        <project>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1</version>
                            <dependencies>
                                <dependency>
                                    <groupId>org.apache.logging.log4j</groupId>
                                    <artifactId>log4j-core</artifactId>
                                    <version>2.17.2</version>
                                </dependency>
                            </dependencies>
                        </project>
                    """.trimIndent()
                    )
                ).results
        assertThat(results).hasSize(1)
        assertThat(results[0].after!!.printAllTrimmed()).isEqualTo(
            """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <dependencyManagement>
                            <dependencies>
                                <dependency>
                                    <groupId>org.apache.logging.log4j</groupId>
                                    <artifactId>log4j-bom</artifactId>
                                    <version>2.17.2</version>
                                    <type>pom</type>
                                    <scope>import</scope>
                                </dependency>
                            </dependencies>
                        </dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.apache.logging.log4j</groupId>
                                <artifactId>log4j-core</artifactId>
                                <version>2.17.2</version>
                            </dependency>
                        </dependencies>
                    </project>
                """.trimIndent()
        )
    }


    @Test
    fun `Added to the root pom`(@TempDir tempDir: Path) {
        val project = tempDir.resolve("pom.xml")
        val service = tempDir.resolve("service/pom.xml")
        val core = tempDir.resolve("core/pom.xml")


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
                        <module>service</module>
                    </modules>
                </project>
            """.trimIndent()
        )

        service.toFile().writeText(
            //language=xml
            """
                <project>
                    <parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>project</artifactId>
                        <version>1</version>
                    </parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>service</artifactId>
                    <version>1</version>
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
        val results = addManagedDependency(true, null)
            .run(mavenParser.parse(listOf(project, service, core), tempDir, InMemoryExecutionContext())
                            .map { j -> j.withMarkers(j.markers.addIfAbsent(javaProject)) }
                            .mapIndexed { n, maven ->
                                if (n == 0) {
                                    // give the parent a different java project
                                    maven.withMarkers(maven.markers.compute(javaProject) { j, _ -> j.withId(Tree.randomId()) })
                                } else maven
                            }
            ).results
        assertThat(results).hasSize(2)
        assertThat(results[0].after!!.printAllTrimmed()).isEqualTo(
            """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>project</artifactId>
                    <version>1</version>
                    <modules>
                        <module>core</module>
                        <module>service</module>
                    </modules>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.apache.logging.log4j</groupId>
                                <artifactId>log4j-bom</artifactId>
                                <version>2.17.2</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """.trimIndent()
        )
        assertThat(results[1].after!!.printAllTrimmed()).isEqualTo(
            """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>core</artifactId>
                    <version>1</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.apache.logging.log4j</groupId>
                                <artifactId>log4j-bom</artifactId>
                                <version>2.17.2</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """.trimIndent()
        )
    }

    @Test
    fun `Added to the root pom having parent not within the source set`(@TempDir tempDir: Path) {
        val parent = tempDir.resolve("pom.xml")
        val child = tempDir.resolve("server/pom.xml")
        child.toFile().parentFile.mkdirs()

        parent.toFile().writeText(
            //language=xml
            """
                <project>
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>2.3.6.RELEASE</version>
                    </parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-parent</artifactId>
                    <version>1</version>
                </project>
            """.trimIndent()
        )

        child.toFile().writeText(
            //language=xml
            """
                <project>
                    <parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-parent</artifactId>
                        <version>1</version>
                    </parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                </project>
            """.trimIndent()
        )
        val results = addManagedDependency(true, null)
            .run(mavenParser.parse(listOf(parent, child), tempDir, InMemoryExecutionContext())
                            .map { j -> j.withMarkers(j.markers.addIfAbsent(javaProject)) }
                            .mapIndexed { n, maven ->
                                if (n == 0) {
                                    // give the parent a different java project
                                    maven.withMarkers(maven.markers.compute(javaProject) { j, _ -> j.withId(Tree.randomId()) })
                                } else maven
                            }
            ).results
        assertThat(results).hasSize(1)
        assertThat(results[0].after!!.printAllTrimmed()).isEqualTo(
            """
                <project>
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>2.3.6.RELEASE</version>
                    </parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-parent</artifactId>
                    <version>1</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.apache.logging.log4j</groupId>
                                <artifactId>log4j-bom</artifactId>
                                <version>2.17.2</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """.trimIndent()
        )
    }

    private fun MavenParser.parseWithProvenance(@Language("xml") vararg pomSources: String) =
        parse(*pomSources).map { j ->
            j.withMarkers(j.markers.addIfAbsent(javaProject))
        }

    private fun XmlParser.parseWithProvenance(@Language("xml") vararg pomSources: String) =
        parse(*pomSources).map { j ->
            j.withMarkers(j.markers.addIfAbsent(javaProject))
        }

    private fun addManagedDependency(
        addToRoot: Boolean,
        onlyIfUsing: String?
    ): AddManagedDependency {
        return AddManagedDependency(
            "org.apache.logging.log4j", "log4j-bom", "2.17.2", "import",
            "pom", null,null, null, onlyIfUsing, addToRoot
        )
    }
}
