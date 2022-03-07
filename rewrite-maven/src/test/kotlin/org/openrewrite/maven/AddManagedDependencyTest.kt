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
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Parser.Input
import org.openrewrite.Tree
import org.openrewrite.java.JavaParser
import org.openrewrite.java.marker.JavaProject
import org.openrewrite.java.tree.J
import org.openrewrite.xml.XmlParser
import java.nio.file.Path

class AddManagedDependencyTest {
    private val xmlParser = XmlParser()
    private val mavenParser = MavenParser.builder().build()
    private val javaParser = JavaParser.fromJavaVersion()
        .dependsOn(
            listOf(Input.fromString("""
                package org.apache.logging.log4j;
                public class Logger {}
            """),Input.fromString("""
                    package org.apache.logging.log4j;
                    public class LogManager {
                        public static Logger getLogger(Class clazz) {
                            return new Logger;
                        }
                    }
            """)
            )
        )
        .build()

    private val executionContext: ExecutionContext
        get() {
            val ctx = InMemoryExecutionContext()
            ctx.putMessage(JavaParser.SKIP_SOURCE_SET_TYPE_GENERATION, true)
            return ctx
        }

    private val javaProject = JavaProject(Tree.randomId(), "myproject", null)

    private val usingLog4J = """
        import org.apache.logging.log4j.Logger;
        import org.apache.logging.log4j.LogManager;

        public class MyExample {

            private static Logger logger = LogManager.getLogger(MyExample.class);
        }
    """

    private val notUsingLog4J = """
        public class SomeClass {

            public static void main(String[] args) {
            }
        }
    """

    @Test
    fun `Does not use type manage dependency not added`() {
        assertThat(
            addManagedDependency(false, "main")
                .run(
                    javaParser.parseWithProvenance(notUsingLog4J) + mavenParser.parseWithProvenance(
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
                )
        ).isEmpty()
    }

    @Test
    fun `Does not use type in classifier source set`() {
        assertThat(
            addManagedDependency(false, "test")
                .run(
                    javaParser.parseWithProvenance(usingLog4J)
                            + mavenParser.parseWithProvenance(
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
                )
        ).isEmpty()
    }

    @Test
    fun `Only added when using and has type`() {
        assertThat(
            addManagedDependency(false, "import")
                .run(
                    javaParser.parseWithProvenance(
                        usingLog4J
                    ) + mavenParser.parseWithProvenance(
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
                )[0].after!!.printAllTrimmed()
        ).isEqualTo(
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
                    </project>
                """.trimIndent()
        )
    }


    @Test
    fun `Added to the root pom`(@TempDir tempDir: Path) {
        val parent = tempDir.resolve("pom.xml")
        val child = tempDir.resolve("server/pom.xml")
        child.toFile().parentFile.mkdirs()

        parent.toFile().writeText(
            //language=xml
            """
                <project>
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
        val results = addManagedDependency(true, "import")
            .run(
                javaParser.parseWithProvenance(
                    usingLog4J
                ) +
                        mavenParser.parse(listOf(parent, child), tempDir, InMemoryExecutionContext())
                            .map { j -> j.withMarkers(j.markers.addIfAbsent(javaProject)) }
                            .mapIndexed { n, maven ->
                                if (n == 0) {
                                    // give the parent a different java project
                                    maven.withMarkers(maven.markers.compute(javaProject) { j, _ -> j.withId(Tree.randomId()) })
                                } else maven
                            }
            )
        assertThat(results).hasSize(1)
        assertThat(results[0].after!!.printAllTrimmed()).isEqualTo(
            """
                <project>
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
        val results = addManagedDependency(true, "import")
            .run(
                javaParser.parseWithProvenance(
                    usingLog4J
                ) +
                        mavenParser.parse(listOf(parent, child), tempDir, InMemoryExecutionContext())
                            .map { j -> j.withMarkers(j.markers.addIfAbsent(javaProject)) }
                            .mapIndexed { n, maven ->
                                if (n == 0) {
                                    // give the parent a different java project
                                    maven.withMarkers(maven.markers.compute(javaProject) { j, _ -> j.withId(Tree.randomId()) })
                                } else maven
                            }
            )
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

    private fun JavaParser.parseWithProvenance(vararg javaSources: String): List<J.CompilationUnit> {
        setSourceSet("import")
        return parse(executionContext, *javaSources).map { j -> j.withMarkers(j.markers.addIfAbsent(javaProject)) }
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
        scope: String
    ): AddManagedDependency {
        return AddManagedDependency(
            "org.apache.logging.log4j", "log4j-bom", "2.17.2", scope, null, "pom",
            true, "org.apache.logging.log4j.*", null, addToRoot
        )
    }
}
