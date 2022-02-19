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

import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.Tree.randomId
import org.openrewrite.java.JavaParser
import org.openrewrite.java.marker.JavaProject
import org.openrewrite.java.tree.J
import org.openrewrite.xml.XmlParser
import java.nio.file.Path

class AddDependencyTest {
    private val xmlParser = XmlParser()
    private val mavenParser = MavenParser.builder().build()
    private val javaParser = JavaParser.fromJavaVersion()
        .classpath("junit-jupiter-api", "guava", "jackson-databind")
        .build()

    private val executionContext: ExecutionContext
        get() {
            val ctx = InMemoryExecutionContext()
            ctx.putMessage(JavaParser.SKIP_SOURCE_SET_TYPE_GENERATION, true)
            return ctx
        }

    private val javaProject = JavaProject(randomId(), "myproject", null)

    private val usingGuavaIntMath = """
        import com.google.common.math.IntMath;
        public class A {
            boolean getMap() {
                return IntMath.isPrime(5);
            }
        }
    """

    private val usingJUnit = """
        class A {
            @org.junit.jupiter.api.Test
            void test() {}
        }
    """

    @ParameterizedTest
    @ValueSource(strings = ["com.google.common.math.*", "com.google.common.math.IntMath"])
    fun onlyIfUsingTestScope(onlyIfUsing: String) {
        assertThat(
            addDependency("com.google.guava:guava:29.0-jre", onlyIfUsing).run(
                javaParser.parseWithProvenance("test", usingGuavaIntMath) + mavenParser.parseWithProvenance(
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
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>29.0-jre</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
            """.trimIndent()
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["com.google.common.math.*", "com.google.common.math.IntMath"])
    fun onlyIfUsingCompileScope(onlyIfUsing: String) {
        assertThat(
            addDependency("com.google.guava:guava:29.0-jre", onlyIfUsing).run(
                javaParser.parseWithProvenance("main", usingGuavaIntMath) + mavenParser.parseWithProvenance(
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
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>29.0-jre</version>
                        </dependency>
                    </dependencies>
                </project>
            """.trimIndent()
        )
    }

    @Test
    fun notUsingType() {
        assertThat(
            addDependency("com.google.guava:guava:29.0-jre", "com.google.common.collect.ImmutableMap").run(
                javaParser.parseWithProvenance("main", usingGuavaIntMath) + mavenParser.parseWithProvenance(
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
    fun addInOrder() {
        assertThat(
            addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath").run(
                javaParser.parseWithProvenance("main", usingGuavaIntMath) + mavenParser.parseWithProvenance(
                    """
                        <project>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1</version>
                            <dependencies>
                                <dependency>
                                    <groupId>commons-lang</groupId>
                                    <artifactId>commons-lang</artifactId>
                                    <version>1.0</version>
                                </dependency>
                            </dependencies>
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
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>29.0-jre</version>
                        </dependency>
                        <dependency>
                            <groupId>commons-lang</groupId>
                            <artifactId>commons-lang</artifactId>
                            <version>1.0</version>
                        </dependency>
                    </dependencies>
                </project>
            """.trimIndent()
        )
    }

    @Test
    fun doNotAddBecauseAlreadyTransitive() {
        assertThat(
            addDependency("org.junit.jupiter:junit-jupiter-api:5.x", "org.junit.jupiter.api.*").run(
                javaParser.parseWithProvenance("test", usingJUnit) + mavenParser.parseWithProvenance(
                    """
                        <project>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1</version>
                            <dependencies>
                                <dependency>
                                    <groupId>org.junit.jupiter</groupId>
                                    <artifactId>junit-jupiter-engine</artifactId>
                                    <version>5.7.1</version>
                                </dependency>
                            </dependencies>
                        </project>
                    """.trimIndent()
                )
            )
        ).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = ["com.google.common.math.*", "com.google.common.math.IntMath"])
    fun semverSelector(onlyIfUsing: String) {
        assertThat(
            AddDependency(
                "com.google.guava", "guava", "29.x", "-jre",
                null, false, onlyIfUsing, null, null, false, null
            ).run(
                javaParser.parseWithProvenance("main", usingGuavaIntMath) + mavenParser.parseWithProvenance(
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
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>29.0-jre</version>
                        </dependency>
                    </dependencies>
                </project>
            """.trimIndent()
        )
    }

    @Test
    fun addTestDependenciesAfterCompile() {
        assertThat(
            addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath").run(
                javaParser.parseWithProvenance("test", usingGuavaIntMath) + mavenParser.parseWithProvenance(
                    """
                        <project>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1</version>
                            <dependencies>
                                <dependency>
                                    <groupId>commons-lang</groupId>
                                    <artifactId>commons-lang</artifactId>
                                    <version>1.0</version>
                                </dependency>
                            </dependencies>
                        </project>
                    """.trimIndent()
                )
            )[0].after!!.printAllTrimmed()
        ).isEqualTo(
            //language=xml
            """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>commons-lang</groupId>
                            <artifactId>commons-lang</artifactId>
                            <version>1.0</version>
                        </dependency>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>29.0-jre</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
            """.trimIndent()
        )
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1392")
    @Test
    fun preserveNonTagContent() {
        assertThat(
            addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath").run(
                javaParser.parseWithProvenance("test", usingGuavaIntMath) + mavenParser.parseWithProvenance(
                    """
                        <project>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1</version>
                            <!-- comment 1 -->
                            <?processing instruction1?>
                            <dependencies>
                                <!-- comment 2 -->
                                <?processing instruction2?>
                                <dependency>
                                    <groupId>commons-lang</groupId>
                                    <artifactId>commons-lang</artifactId>
                                    <version>1.0</version>
                                </dependency>
                            </dependencies>
                        </project>
                    """.trimIndent()
                )
            )[0].after!!.printAllTrimmed()
        ).isEqualTo(
            //language=xml
            """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <!-- comment 1 -->
                    <?processing instruction1?>
                    <dependencies>
                        <!-- comment 2 -->
                        <?processing instruction2?>
                        <dependency>
                            <groupId>commons-lang</groupId>
                            <artifactId>commons-lang</artifactId>
                            <version>1.0</version>
                        </dependency>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>29.0-jre</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
            """.trimIndent()
        )
    }

    @Test
    fun addDependencyDoesntAddWhenExistingDependency() {
        assertThat(
            addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath").run(
                javaParser.parseWithProvenance("main", usingGuavaIntMath) + mavenParser.parseWithProvenance(
                    """
                        <project>
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
                    """.trimIndent()
                )
            )
        ).isEmpty()
    }

    @Test
    fun useManaged(@TempDir tempDir: Path) {
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
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava</artifactId>
                                <version>28.0-jre</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """
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

        assertThat(
            addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath").run(
                javaParser.parseWithProvenance("main", usingGuavaIntMath) +
                        mavenParser.parse(listOf(parent, child), tempDir, InMemoryExecutionContext())
                            .map { j -> j.withMarkers(j.markers.addIfAbsent(javaProject)) }
                            .mapIndexed { n, maven ->
                                if (n == 0) {
                                    // give the parent a different java project
                                    maven.withMarkers(maven.markers.compute(javaProject) { j, _ -> j.withId(randomId()) })
                                } else maven
                            }
            )[0].after!!.printAllTrimmed()
        ).isEqualTo(
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
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                        </dependency>
                    </dependencies>
                </project>
            """.trimIndent()
        )
    }

    @Test
    fun useRequestedVersionInUseByOtherMembersOfTheFamily() {
        assertThat(
            AddDependency(
                "com.fasterxml.jackson.module", "jackson-module-afterburner", "2.10.5",
                null, null, false, "com.fasterxml.jackson.databind.*",
                null, null, null, "com.fasterxml.*"
            ).run(
                javaParser.parseWithProvenance(
                    "main", """
                    public class A {
                        com.fasterxml.jackson.databind.ObjectMapper mapper;
                    }
                """.trimIndent()
                ) + mavenParser.parseWithProvenance(
                    """
                        <project>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1</version>
                            <properties>
                                <jackson.version>2.12.0</jackson.version>
                            </properties>
                            <dependencies>
                                <dependency>
                                    <groupId>com.fasterxml.jackson.core</groupId>
                                    <artifactId>jackson-databind</artifactId>
                                    <version>${'$'}{jackson.version}</version>
                                </dependency>
                            </dependencies>
                        </project>
                    """.trimIndent()
                )
            )[0].after!!.printAllTrimmed()
        ).isEqualTo(
            //language=xml
            """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                        <jackson.version>2.12.0</jackson.version>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>com.fasterxml.jackson.core</groupId>
                            <artifactId>jackson-databind</artifactId>
                            <version>${'$'}{jackson.version}</version>
                        </dependency>
                        <dependency>
                            <groupId>com.fasterxml.jackson.module</groupId>
                            <artifactId>jackson-module-afterburner</artifactId>
                            <version>${'$'}{jackson.version}</version>
                        </dependency>
                    </dependencies>
                </project>
            """.trimIndent()
        )
    }

    private fun JavaParser.parseWithProvenance(sourceSet: String, vararg javaSources: String): List<J.CompilationUnit> {
        setSourceSet(sourceSet)
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

    private fun addDependency(gav: String, onlyIfUsing: String): AddDependency {
        val (group, artifact, version) = gav.split(":")
        return AddDependency(
            group, artifact, version, null, null, true,
            onlyIfUsing, null, null, false, null
        )
    }
}
