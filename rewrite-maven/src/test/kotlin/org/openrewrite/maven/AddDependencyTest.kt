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
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Parser
import org.openrewrite.SourceFile
import org.openrewrite.java.JavaParser
import org.openrewrite.maven.tree.Maven

import java.util.*

class AddDependencyTest : MavenRecipeTest {
    override val recipe: AddDependency
        get() = AddDependency(
            "org.springframework.boot",
            "spring-boot",
            "1.5.22.RELEASE"
        )

    val jp: JavaParser
        get() = JavaParser.fromJavaVersion()
            .dependsOn(Collections.singletonList(Parser.Input.fromString(
                    """
                    package com.google.common.math;
                    public class IntMath {
                        public static boolean isPrime(int n) {}
                    }
                """)))
            .build()

    @Test
    fun onlyIfUsing() {
        val recipe = AddDependency(
            "com.google.guava",
            "guava",
            "29.0-jre",
            null,
            true,
            null,
            null,
            null,
            null,
            listOf("com.google.common.math.IntMath")
        )
        val javaSource = jp.parse(
            """
            package org.openrewrite.java.testing;
            import com.google.common.math.IntMath;
            public class A {
                boolean getMap() {
                    return IntMath.isPrime(5);
                }
            }
        """
        )[0]
        val mavenSource = MavenParser.builder().build().parse(
            """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <dependencies>
              </dependencies>
            </project>
        """.trimIndent()
        )[0]

        val sources: List<SourceFile> = listOf(javaSource, mavenSource)
        val results = recipe.run(sources, InMemoryExecutionContext { error: Throwable -> throw error })
        val mavenResult = results.find { it.before === mavenSource }
        assertThat(mavenResult).isNotNull

        assertThat(mavenResult?.after?.print()).isEqualTo(
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
    fun onlyIfUsingWildcard() {
        val recipe = AddDependency(
            "com.google.guava",
            "guava",
            "29.0-jre",
            null,
            true,
            null,
            null,
            null,
            null,
            listOf("com.google.common.math.*", "org.springframework.boot")
        )
        val javaSource = jp.parse(
            """
            package org.openrewrite.java.testing;
            import com.google.common.math.IntMath;
            public class A {
                boolean getMap() {
                    return IntMath.isPrime(5);
                }
                String getName() {
                    return "bla";
                }
            }
        """
        )[0]
        val mavenSource = MavenParser.builder().build().parse(
            """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <dependencies>
              </dependencies>
            </project>
        """.trimIndent()
        )[0]

        val sources: List<SourceFile> = listOf(javaSource, mavenSource)
        val results = recipe.run(sources, InMemoryExecutionContext { error: Throwable -> throw error })
        val mavenResult = results.find { it.before === mavenSource }
        assertThat(mavenResult).isNotNull

        assertThat(mavenResult?.after?.print()).isEqualTo(
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
    fun onlyIfUsingTypeNotFoundNoChange() = assertUnchanged(
        recipe = AddDependency(
            "com.google.guava",
            "guava",
            "29.0-jre",
            null,
            true,
            null,
            null,
            null,
            null,
            listOf("com.google.common.collect.ImmutableMap")
        ),
        before = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <dependencies>
              </dependencies>
            </project>
        """
    )

    @Test
    fun addToExistingDependencies() = assertChanged(
        recipe = AddDependency(
            "org.springframework.boot",
            "spring-boot-starter-actuator",
            "1.5.22.RELEASE"
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
                  <version>1.5.22.RELEASE</version>
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
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-actuator</artifactId>
                  <version>1.5.22.RELEASE</version>
                </dependency>
              </dependencies>
            </project>
        """
    )

    @Test
    fun doNotAddBecauseAlreadyTransitive() = assertUnchanged(
        recipe = AddDependency(
            "org.junit.jupiter",
            "junit-jupiter-api",
            "5.x",
            null,
            true,
            null,
            "test",
            null,
            null,
            null
        ),
        before = """
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>2.5.1</version>
                <relativePath/> <!-- lookup parent from repository -->
              </parent>
              <groupId>com.example</groupId>
              <artifactId>demo</artifactId>
              <version>0.0.1-SNAPSHOT</version>
              <name>demo</name>
              <description>Demo project for Spring Boot</description>
              <properties>
                <java.version>11</java.version>
              </properties>
              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter</artifactId>
                </dependency>
            
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
            
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                  </plugin>
                </plugins>
              </build>
            
            </project>
        """
    )

    @Test
    fun addWhenNoDependencies() = assertChanged(
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
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
    fun addBySemver() = assertChanged(
        recipe = AddDependency(
            "org.springframework.boot",
            "spring-boot",
            "1.4.X"
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
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
                  <version>1.4.7.RELEASE</version>
                </dependency>
              </dependencies>
            </project>
        """,
        afterConditions = { m : Maven ->
            assertThat(m.model.dependencies.first().version).isEqualTo("1.4.7.RELEASE")
        }
    )

    @Test
    fun addTestDependenciesAfterCompile() = assertChanged(
        recipe = AddDependency("org.junit.jupiter", "junit-jupiter-api", "5.7.0")
            .withScope("test"),
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
                  <version>1.5.22.RELEASE</version>
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
                <dependency>
                  <groupId>org.junit.jupiter</groupId>
                  <artifactId>junit-jupiter-api</artifactId>
                  <version>5.7.0</version>
                  <scope>test</scope>
                </dependency>
              </dependencies>
            </project>
        """
    )

    @Test
    fun maybeAddDependencyDoesntAddWhenExistingDependency() = assertUnchanged(
        recipe = recipe,
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
                  <version>1.5.22.RELEASE</version>
                </dependency>
              </dependencies>
            </project>
        """
    )

    @Test
    fun useManagedDependency() = assertChanged(
        recipe = AddDependency(
            "com.fasterxml.jackson.core",
            "jackson-databind",
            "2.12.0"
        ),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>com.fasterxml.jackson.core</groupId>
                    <artifactId>jackson-databind</artifactId>
                    <version>2.12.0</version>
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
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>com.fasterxml.jackson.core</groupId>
                    <artifactId>jackson-databind</artifactId>
                    <version>2.12.0</version>
                  </dependency>
                </dependencies>
              </dependencyManagement>
              <dependencies>
                <dependency>
                  <groupId>com.fasterxml.jackson.core</groupId>
                  <artifactId>jackson-databind</artifactId>
                </dependency>
              </dependencies>
            </project>
        """
    )

    @Test
    fun useRequestedVersionInUseByOtherMembersOfTheFamily() = assertChanged(
        recipe = AddDependency(
            "com.fasterxml.jackson.module",
            "jackson-module-afterburner",
            "2.12.0"
        ).withFamilyPattern("com.fasterxml.jackson*"),
        before = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
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
        """,
        after = """
            <project>
              <modelVersion>4.0.0</modelVersion>
              
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
        """
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = AddDependency(null, null, null)
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(3)
        assertThat(valid.failures()[0].property).isEqualTo("artifactId")
        assertThat(valid.failures()[1].property).isEqualTo("groupId")
        assertThat(valid.failures()[2].property).isEqualTo("version")

        recipe = AddDependency(null, "rewrite-maven", null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("groupId")
        assertThat(valid.failures()[1].property).isEqualTo("version")

        recipe = AddDependency("org.openrewrite", null, null)
        valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(2)
        assertThat(valid.failures()[0].property).isEqualTo("artifactId")
        assertThat(valid.failures()[1].property).isEqualTo("version")

        recipe = AddDependency("org.openrewrite", "rewrite-maven", "1.0.0")
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue
    }
}
