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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.InMemoryExecutionContext
import java.nio.file.Path

class ChangeDependencyGroupIdAndArtifactIdTest : MavenRecipeTest {

    override val recipe: ChangeDependencyGroupIdAndArtifactId
        get() = ChangeDependencyGroupIdAndArtifactId(
            "org.openrewrite.recipe",
            "rewrite-testing-frameworks",
            "org.openrewrite.recipe",
            "rewrite-migrate-java",
            null
        )

    @Test
    fun changeDependencyGroupIdAndArtifactId() = assertChanged(
        recipe =  ChangeDependencyGroupIdAndArtifactId(
            "javax.activation",
            "javax.activation-api",
            "jakarta.activation",
            "jakarta.activation-api",
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
                        <groupId>javax.activation</groupId>
                        <artifactId>javax.activation-api</artifactId>
                    </dependency>
                </dependencies>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>javax.activation</groupId>
                            <artifactId>javax.activation-api</artifactId>
                            <version>1.2.0</version>
                        </dependency>
                        <dependency>
                            <groupId>jakarta.activation</groupId>
                            <artifactId>jakarta.activation-api</artifactId>
                            <version>1.2.1</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
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
                        <groupId>jakarta.activation</groupId>
                        <artifactId>jakarta.activation-api</artifactId>
                    </dependency>
                </dependencies>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>javax.activation</groupId>
                            <artifactId>javax.activation-api</artifactId>
                            <version>1.2.0</version>
                        </dependency>
                        <dependency>
                            <groupId>jakarta.activation</groupId>
                            <artifactId>jakarta.activation-api</artifactId>
                            <version>1.2.1</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
            </project>
        """
    )

    @Test
    fun changeOnlyArtifactId() = assertChanged(
        recipe = ChangeDependencyGroupIdAndArtifactId(
            "org.openrewrite",
            "rewrite-java-8",
            "org.openrewrite",
            "rewrite-java-11",
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
                        <groupId>org.openrewrite</groupId>
                        <artifactId>rewrite-java-8</artifactId>
                        <version>7.20.0</version>
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
                        <groupId>org.openrewrite</groupId>
                        <artifactId>rewrite-java-11</artifactId>
                        <version>7.20.0</version>
                    </dependency>
                </dependencies>
            </project>
        """
    )

    @Test
    fun doNotChangeUnlessBothGroupIdAndArtifactIdMatch() = assertUnchanged(
        before = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                    <dependency>
                        <groupId>org.openrewrite.recipe</groupId>
                        <artifactId>rewrite-spring</artifactId>
                        <version>4.12.0</version>
                    </dependency>
                </dependencies>
            </project>
        """
    )

    @Test
    fun changeDependencyGroupIdAndArtifactIdAndVersion() = assertChanged(
        recipe =  ChangeDependencyGroupIdAndArtifactId(
            "javax.activation",
            "javax.activation-api",
            "jakarta.activation",
            "jakarta.activation-api",
            "2.1.0"
        ),
        before = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                    <dependency>
                        <groupId>javax.activation</groupId>
                        <artifactId>javax.activation-api</artifactId>
                        <version>1.2.0</version>
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
                        <groupId>jakarta.activation</groupId>
                        <artifactId>jakarta.activation-api</artifactId>
                        <version>2.1.0</version>
                    </dependency>
                </dependencies>
            </project>
        """
    )

    @Test
    fun changeDependencyGroupIdAndArtifactIdWithDeepHierarchy(@TempDir tempDir: Path) {
        val parent = tempDir.resolve("pom.xml")
        val child = tempDir.resolve("child/pom.xml")
        val subchild = tempDir.resolve("child/subchild/pom.xml")

        subchild.toFile().parentFile.mkdirs()

        parent.toFile().writeText(
            //language=xml
            """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>parent</artifactId>
                    <version>1</version>
                    <modules>
                        <module>child</module>
                    </modules>
                </project>
            """.trimIndent()
        )

        child.toFile().writeText(
            //language=xml
            """
                <project>
                    <parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>parent</artifactId>
                        <version>1</version>
                    </parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>child</artifactId>
                    <version>1</version>
                    <modules>
                        <module>subchild</module>
                    </modules>
                </project>
            """.trimIndent()
        )

        subchild.toFile().writeText(
            //language=xml
            """
                <project>
                    <parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>child</artifactId>
                        <version>1</version>
                    </parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>subchild</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.quarkus</groupId>
                            <artifactId>quarkus-core</artifactId>
                            <version>2.8.0.Final</version>
                        </dependency>
                    </dependencies>
                </project>
            """.trimIndent()
        )

        val results = ChangeDependencyGroupIdAndArtifactId("io.quarkus", "quarkus-core", "io.quarkus", "quarkus-arc", null)
            .run(parser.parse(listOf(subchild, child, parent), tempDir, InMemoryExecutionContext()))

        assertThat(results).hasSize(1)
        assertThat(results[0].after!!.printAllTrimmed()).isEqualTo(
            """
                <project>
                    <parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>child</artifactId>
                        <version>1</version>
                    </parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>subchild</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.quarkus</groupId>
                            <artifactId>quarkus-arc</artifactId>
                            <version>2.8.0.Final</version>
                        </dependency>
                    </dependencies>
                </project>
            """.trimIndent()
        )
    }
}
