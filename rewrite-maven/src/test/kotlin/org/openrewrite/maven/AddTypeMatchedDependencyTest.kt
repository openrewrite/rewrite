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

import org.junit.jupiter.api.fail
import org.assertj.core.api.Assertions.*
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions

import org.junit.jupiter.api.Test
import org.openrewrite.*
import org.openrewrite.internal.ListUtils
import org.openrewrite.java.JavaParser
import org.openrewrite.java.marker.JavaProvenance
import org.openrewrite.maven.tree.Maven
import org.openrewrite.scheduling.ForkJoinScheduler
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool

class AddTypeMatchedDependencyTest {

    private val treePrinter: TreePrinter<*>?
        get() = null

    private val mavenParser : MavenParser
        get() = MavenParser.builder().build()

    private val javaParser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .dependsOn(listOf(
                Parser.Input.fromString("""
                    package com.google.common.math;
                    public class IntMath {
                        public static boolean isPrime(int n) {}
                    }
                """),
                    Parser.Input.fromString("""
                    package org.junit.jupiter.api;

                    import java.lang.annotation.ElementType;
                    import java.lang.annotation.Target;

                    @Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
                    public @interface Test {
                    }                    
                """),
                    Parser.Input.fromString("""
                    package com.fasterxml.jackson.databind;
                    public class ObjectMapper {}                    
                """)
            )).build()

    val mainJavaProvenance: JavaProvenance
        get() = javaProvenance(groupId = "com.mycompany.app", artifactId = "my-app", version="1")

    val testJavaProvenance: JavaProvenance
        get() = javaProvenance(groupId = "com.mycompany.app", artifactId = "my-app", version="1", sourceSet= "test")

    @Test
    fun onlyIfUsing() = assertChanged(
        recipe = AddTypeMatchedDependency.builder()
            .groupId("com.google.guava")
            .artifactId("guava")
            .version("29.0-jre")
            .typeMatchExpressions(listOf("com.google.common.math.IntMath")).build(),
        additionalSources = parseJavaFiles(
            javaSources = arrayOf(
                """
                package org.openrewrite.java.testing;
                import com.google.common.math.IntMath;
                public class A {
                    boolean getMap() {
                        return IntMath.isPrime(5);
                    }
                }
                """
            ), javaProvenance = mainJavaProvenance
        ),
        before = """
        <project>
            <groupId>com.mycompany.app</groupId>
            <artifactId>my-app</artifactId>
            <version>1</version>
            <dependencies>
            </dependencies>
        </project>
        """,
        after = """
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
        """
    )

    @Test
    fun onlyIfUsingWildcard() = assertChanged(
        recipe = AddTypeMatchedDependency.builder()
            .groupId("com.google.guava")
            .artifactId("guava")
            .version("29.0-jre")
            .typeMatchExpressions(listOf("com.google.common.math.*")).build(),
        before = """
        <project>
          <groupId>com.mycompany.app</groupId>
          <artifactId>my-app</artifactId>
          <version>1</version>
          <dependencies>
          </dependencies>
        </project>
        """,
        after = """
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
        """,
        additionalSources = parseJavaFiles(
            javaSources = arrayOf(
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
            ), javaProvenance = mainJavaProvenance
        )
    )

    @Test
    fun typeNotFoundNoChange() = assertUnchanged(
        recipe = AddTypeMatchedDependency.builder()
            .groupId("com.google.guava")
            .artifactId("guava")
            .version("29.0-jre")
            .typeMatchExpressions(listOf("com.google.common.collect.ImmutableMap")).build(),
        before = """
        <project>
          <groupId>com.mycompany.app</groupId>
          <artifactId>my-app</artifactId>
          <version>1</version>
          <dependencies>
          </dependencies>
        </project>
        """,
        additionalSources = parseJavaFiles(
            javaSources = arrayOf(
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
            ), javaProvenance = mainJavaProvenance
        )
    )

    @Test
    fun addToExistingDependencies() = assertChanged(
            recipe = AddTypeMatchedDependency.builder()
                .groupId("com.google.guava")
                .artifactId("guava")
                .version("29.0-jre")
                .typeMatchExpressions(listOf("com.google.common.math.IntMath")).build(),
            before = """
                <project>
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
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot</artifactId>
                            <version>1.5.22.RELEASE</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
            additionalSources = parseJavaFiles(
                javaSources = arrayOf(
                    """
                    package org.openrewrite.java.testing;
                    import com.google.common.math.IntMath;
                    public class A {
                        boolean getMap() {
                            return IntMath.isPrime(5);
                        }
                    }
                    """
                ), javaProvenance = mainJavaProvenance
            )
        )

    @Test
    fun doNotAddBecauseAlreadyTransitive() = assertUnchanged(
        recipe = AddTypeMatchedDependency.builder()
            .groupId("org.junit.jupiter")
            .artifactId("junit-jupiter-api")
            .version("5.x")
            .scope("test")
            .typeMatchExpressions(listOf("org.junit.jupiter.api.*")).build(),
        additionalSources = parseJavaFiles(
            javaSources = arrayOf("""
            package org.openrewrite.java.testing;
            import com.google.common.math.IntMath;
            import org.junit.jupiter.api.Test;
            public class A {

                @Test
                boolean aTest() {
                    return IntMath.isPrime(5);
                }
            }
            """
            ), javaProvenance = testJavaProvenance
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
          <groupId>com.mycompany.app</groupId>
          <artifactId>my-app</artifactId>
          <version>1</version>
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
    fun semVersion() {
        assertChanged(
            recipe = AddTypeMatchedDependency.builder()
                .groupId("com.google.guava")
                .artifactId("guava")
                .version("29.x")
                .versionPattern("-jre")
                .typeMatchExpressions(listOf("com.google.common.math.IntMath")).build(),
            additionalSources = parseJavaFiles(
                javaSources = arrayOf(
                    """
                package org.openrewrite.java.testing;
                import com.google.common.math.IntMath;
                public class A {
                    boolean getMap() {
                        return IntMath.isPrime(5);
                    }
                }
                """
                ), javaProvenance = mainJavaProvenance
            ),
            before = """
                <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencies>
                  </dependencies>
                </project>
            """,
            after = """
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
            """
        )
    }

    @Test
    fun addTestDependenciesAfterCompile() = assertChanged(
        recipe = AddTypeMatchedDependency.builder()
            .groupId("org.junit.jupiter")
            .artifactId("junit-jupiter-api")
            .version("5.7.0")
            .scope("test")
            .typeMatchExpressions(listOf("org.junit.jupiter.api.*")).build(),
        additionalSources = parseJavaFiles(
            javaSources = arrayOf(
                """
            package org.openrewrite.java.testing;
            import com.google.common.math.IntMath;
            import org.junit.jupiter.api.Test;
            public class A {

                @Test
                boolean aTest() {
                    return IntMath.isPrime(5);
                }
            }
            """
            ), javaProvenance = testJavaProvenance
        ),
        before = """
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
        """,
        after = """
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
    fun addDependencyDoesntAddWhenExistingDependency() = assertUnchanged(
        recipe = AddTypeMatchedDependency.builder()
            .groupId("org.junit.jupiter")
            .artifactId("junit-jupiter-api")
            .version("5.7.0")
            .scope("test")
            .typeMatchExpressions(listOf("org.junit.jupiter.api.*")).build(),
        additionalSources = parseJavaFiles(
            javaSources = arrayOf("""
            package org.openrewrite.java.testing;
            import com.google.common.math.IntMath;
            import org.junit.jupiter.api.Test;
            public class A {

                @Test
                boolean aTest() {
                    return IntMath.isPrime(5);
                }
            }
            """
            ), javaProvenance = testJavaProvenance
        ),
        before = """
        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
          <modelVersion>4.0.0</modelVersion>
          <groupId>com.mycompany.app</groupId>
          <artifactId>my-app</artifactId>
          <version>1</version>
          <dependencies>
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter</artifactId>
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
    fun useManagedDependency() = assertChanged(
        recipe = AddTypeMatchedDependency.builder()
            .groupId("com.fasterxml.jackson.core")
            .artifactId("jackson-databind")
            .version("2.12.4")
            .typeMatchExpressions(listOf("com.fasterxml.jackson.databind.*")).build(),
        additionalSources = parseJavaFiles(javaSources = arrayOf(
            """
            package org.openrewrite.java.testing;
            import com.fasterxml.jackson.databind.ObjectMapper;
            public class A {
                ObjectMapper mapper;
                void aTest() {
                }
            }
            """
            ), javaProvenance = mainJavaProvenance
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
        recipe = AddTypeMatchedDependency.builder()
            .groupId("com.fasterxml.jackson.module")
            .artifactId("jackson-module-afterburner")
            .version("2.10.5")
            .familyPattern("com.fasterxml.*")
            .typeMatchExpressions(listOf("com.fasterxml.jackson.databind.*")).build(),
        additionalSources = parseJavaFiles(javaSources = arrayOf(
            """
            package org.openrewrite.java.testing;
            import com.fasterxml.jackson.databind.ObjectMapper;
            public class A {
                ObjectMapper mapper;
                void aTest() {
                }
            }
            """
        ), javaProvenance = mainJavaProvenance
        ),
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

    @Test
    fun addOptionalDependency() = assertChanged(
        recipe = AddTypeMatchedDependency.builder()
            .groupId("com.google.guava")
            .artifactId("guava")
            .version("29.0-jre")
            .typeMatchExpressions(listOf("com.google.common.math.IntMath")).build(),
        additionalSources = parseJavaFiles(
            javaSources = arrayOf(
                """
            package org.openrewrite.java.testing;
            import com.google.common.math.IntMath;
            public class A {
                boolean getMap() {
                    return IntMath.isPrime(5);
                }
            }
            """
            ), javaProvenance = mainJavaProvenance
        ),
        before = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <dependencies>
              </dependencies>
            </project>
        """,
        after = """
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
        """
    )

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @Test
    fun checkValidation() {
        var recipe = AddTypeMatchedDependency.builder().build()
        var valid = recipe.validate()
        assertThat(valid.isValid).isFalse
        assertThat(valid.failures()).hasSize(4)
        assertThat(valid.failures()[0].property).isEqualTo("artifactId")
        assertThat(valid.failures()[1].property).isEqualTo("groupId")
        assertThat(valid.failures()[3].property).isEqualTo("version")
        assertThat(valid.failures()[2].property).isEqualTo("typeMatchExpressions")


        recipe = AddTypeMatchedDependency.builder()
            .groupId("org.openrewrite")
            .artifactId("artifact")
            .version("1.0.0")
            .typeMatchExpressions(listOf("com.google.common.math.IntMath"))
            .build()
        valid = recipe.validate()
        assertThat(valid.isValid).isTrue
    }

    fun assertChanged(
        recipe: Recipe,
        @Language("XML") before: String,
        @Language("XML") after: String,
        additionalSources: Array<SourceFile> = emptyArray(),
        cycles: Int = 2,
        expectedCyclesToComplete: Int = cycles - 1,
    ) {

        val source = mavenParser.parse(before.trimIndent()).singleOrNull()
        assertThat(source)
            .`as`("The parser did not return a source file.")
            .isNotNull

        val sources = listOf(source) + additionalSources

        val recipeSchedulerCheckingExpectedCycles = RecipeSchedulerCheckingExpectedCycles(
            ForkJoinScheduler(ForkJoinPool(1)), expectedCyclesToComplete)

        val results = recipe
            .run(
                sources,
                InMemoryExecutionContext { t: Throwable -> fail<Any>("Recipe threw an exception", t) },
                recipeSchedulerCheckingExpectedCycles,
                cycles,
                expectedCyclesToComplete + 1
            )

        if (results.isEmpty()) {
            fail<Any>("The recipe must make changes")
        }

        val result = results.find { s -> source === s.before }

        assertThat(result).`as`("The recipe must make changes").isNotNull
        assertThat(result!!.after).isNotNull
        assertThat(result.after!!.printTrimmed(TreePrinter.identity<Any>()))
            .isEqualTo(after.trimIndent())

        recipeSchedulerCheckingExpectedCycles.verify()
    }

    fun assertUnchanged(
        recipe: Recipe,
        @Language("XML") before: String,
        additionalSources: Array<SourceFile> = emptyArray(),
    ) {
        assertThat(recipe).`as`("A recipe must be specified").isNotNull

        val source = mavenParser.parse(before).singleOrNull()
        assertThat(source)
            .`as`("The parser did not return a source file.")
            .isNotNull

        val sources = listOf(source) + additionalSources

        val recipeSchedulerCheckingExpectedCycles = RecipeSchedulerCheckingExpectedCycles(
            ForkJoinScheduler(ForkJoinPool(1)),0)

        val results = recipe
            .run(
                sources,
                InMemoryExecutionContext { t: Throwable -> fail<Any>("Recipe threw an exception", t) },
                recipeSchedulerCheckingExpectedCycles,
                2,
                2
            )

        results.forEach { result ->
            if (result.diff(treePrinter ?: TreePrinter.identity<Any>()).isEmpty()) {
                fail("An empty diff was generated. The recipe incorrectly changed a reference without changing its contents.")
            }
        }

        for (result in results) {
            assertThat(result.after?.print(treePrinter ?: TreePrinter.identity<Any>(), null))
                .`as`("The recipe must not make changes")
                .isEqualTo(result.before?.print(treePrinter ?: TreePrinter.identity<Any>(), null))
        }

        recipeSchedulerCheckingExpectedCycles.verify()
    }

    fun javaProvenance(groupId: String,
                       artifactId: String,
                       version: String = "1.0.0",
                       sourceSet: String = "main"
    ) : JavaProvenance {

        val javaRuntimeVersion = System.getProperty("java.runtime.version")
        val javaVendor = System.getProperty("java.vm.vendor")

        return JavaProvenance(
            Tree.randomId(),
            "${groupId}:${artifactId}:${version}",
            sourceSet,
            JavaProvenance.BuildTool(JavaProvenance.BuildTool.Type.Maven, ""),
            JavaProvenance.JavaVersion(javaRuntimeVersion, javaVendor,javaRuntimeVersion,javaRuntimeVersion),
            JavaProvenance.Publication(groupId, artifactId, version)
        )
    }

    fun parseMavenFiles(mavenSources: Array<String>, mavenParser: MavenParser = this.mavenParser) : Array<SourceFile> {
        val sources = mavenParser.parse(*mavenSources)
        assertThat(sources.size)
            .`as`("The parser was provided with ${mavenSources.size} inputs which it parsed into ${sources.size} SourceFiles. The parser likely encountered an error.")
            .isEqualTo(mavenSources)
        return sources.toTypedArray()
    }

    fun parseJavaFiles(javaSources: Array<String>, javaProvenance: JavaProvenance) : Array<SourceFile> {
        var sources = javaParser.parse(*javaSources)
        assertThat(sources.size)
            .`as`("The parser was provided with ${javaSources.size} inputs which it parsed into ${sources.size} SourceFiles. The parser likely encountered an error.")
            .isEqualTo(javaSources.size)

        sources = ListUtils.map(sources) { j -> j.withMarkers(j.markers.addIfAbsent(javaProvenance)) }
        return sources.toTypedArray()
    }

    private class RecipeSchedulerCheckingExpectedCycles(
        private val delegate: RecipeScheduler,
        private val expectedCyclesThatMakeChanges: Int,
    ) : RecipeScheduler {
        var cyclesThatResultedInChanges = 0

        override fun <T : Any?> schedule(fn: Callable<T>): CompletableFuture<T> {
            return delegate.schedule(fn)
        }

        override fun <S : SourceFile?> scheduleVisit(
            recipe: Recipe,
            before: MutableList<S>,
            ctx: ExecutionContext,
            recipeThatDeletedSourceFile: MutableMap<UUID, Recipe>,
        ): MutableList<S> {
            ctx.putMessage("cyclesThatResultedInChanges", cyclesThatResultedInChanges)
            val afterList = delegate.scheduleVisit(recipe, before, ctx, recipeThatDeletedSourceFile)
            if (afterList !== before) {
                cyclesThatResultedInChanges = cyclesThatResultedInChanges.inc()
                if (cyclesThatResultedInChanges > expectedCyclesThatMakeChanges &&
                    before.isNotEmpty() && afterList.isNotEmpty()
                ) {
                    assertThat(afterList[0]!!.printTrimmed())
                        .`as`(
                            "Expected recipe to complete in $expectedCyclesThatMakeChanges cycle${if (expectedCyclesThatMakeChanges == 1) "" else "s"}, " +
                                    "but took at least one more cycle. Between the last two executed cycles there were changes."
                        )
                        .isEqualTo(before[0]!!.printTrimmed())
                }
            }
            return afterList
        }

        fun verify() {
            if (cyclesThatResultedInChanges != expectedCyclesThatMakeChanges) {
                fail("Expected recipe to complete in $expectedCyclesThatMakeChanges cycle${if (expectedCyclesThatMakeChanges > 1) "s" else ""}, but took $cyclesThatResultedInChanges cycle${if (cyclesThatResultedInChanges > 1) "s" else ""}.")
            }
        }
    }

}
