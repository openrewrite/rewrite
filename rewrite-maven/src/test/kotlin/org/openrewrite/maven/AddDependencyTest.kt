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

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.openrewrite.Issue
import org.openrewrite.java.JavaParser
import org.openrewrite.test.JavaTestingSupport
import org.openrewrite.test.MavenTestingSupport

class AddDependencyTest : JavaTestingSupport, MavenTestingSupport {

    override val javaParser: JavaParser
        get() = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(true)
        .classpath("junit-jupiter-api", "guava", "jackson-databind", "jackson-core")
        .build()

    private val usingGuavaIntMath = """
        import com.google.common.math.IntMath;
        public class ASimpleExample {
            boolean getMap() {
                //noinspection UnstableApiUsage
                return IntMath.isPrime(5);
            }
        }
    """

    private val usingJUnit = """
        class AnExample {
            @org.junit.jupiter.api.Test
            void test() {}
        }
    """

    @ParameterizedTest
    @ValueSource(strings = ["com.google.common.math.*", "com.google.common.math.IntMath"])
    fun onlyIfUsingTestScope(onlyIfUsing: String) {

        //Parse the maven project (which will have a JavaProject Provenance associated with it)
        val maven = mavenParser.parseMavenProjects(source = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
            </project>
        """)

        //Parse the java with a test source set and associate with the java project.
        val javaSource = javaParser.parse(
            source = usingGuavaIntMath,
            sourceSet = "test",
            markers = listOf(maven.getJavaProject())
        )

        assertChanged(
            recipe = addDependency("com.google.guava:guava:29.0-jre", onlyIfUsing),
            before = maven,
            additionalSources = listOf(javaSource),
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
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
            """
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["com.google.common.math.*", "com.google.common.math.IntMath"])
    fun onlyIfUsingCompileScope(onlyIfUsing: String) {
        //Parse the maven project (which will have a JavaProject Provenance associated with it)
        val maven = mavenParser.parseMavenProjects(
            source = """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                </project>
            """
        )

        //Parse the java with a main source set and associate with the java project.
        val javaSource = javaParser.parse(
            source = usingGuavaIntMath,
            sourceSet = "main",
            markers = listOf(maven.getJavaProject())
        )

        assertChanged(
            recipe = addDependency("com.google.guava:guava:29.0-jre", onlyIfUsing),
            before = maven,
            additionalSources = listOf(javaSource),
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
    fun notUsingType() {
        //Parse the maven project (which will have a JavaProject Provenance associated with it)
        val maven = mavenParser.parseMavenProjects(
            source = """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                </project>
            """
        )

        //Parse the java with a main source set and associate with the java project.
        val javaSource = javaParser.parse(
            source = usingGuavaIntMath,
            sourceSet = "main",
            markers = listOf(maven.getJavaProject())
        )

        assertUnchanged(
            recipe = addDependency("com.google.guava:guava:29.0-jre", "com.google.common.collect.ImmutableMap"),
            before = maven,
            additionalSources = listOf(javaSource)
        )
    }

    @Test
    fun addInOrder() {

        //Parse the maven project (which will have a JavaProject Provenance associated with it)
        val maven = mavenParser.parseMavenProjects(
            source = """
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
            """
        )

        //Parse the java with a main source set and associate with the java project.
        val javaSource = javaParser.parse(
            source = usingGuavaIntMath,
            sourceSet = "main",
            markers = listOf(maven.getJavaProject())
        )

        assertChanged(
            recipe = addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath"),
            before = maven,
            additionalSources = listOf(javaSource),
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
                            <groupId>commons-lang</groupId>
                            <artifactId>commons-lang</artifactId>
                            <version>1.0</version>
                        </dependency>
                    </dependencies>
                </project>
            """
        )
    }

    @Test
    fun doNotAddBecauseAlreadyTransitive() {
        //Parse the maven project (which will have a JavaProject Provenance associated with it)
        val maven = mavenParser.parseMavenProjects(
            source = """
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
            """
        )

        //Parse the java with a main source set and associate with the java project.
        val javaSource = javaParser.parse(
            source = usingJUnit,
            sourceSet = "test",
            markers = listOf(maven.getJavaProject())
        )

        assertUnchanged(
            recipe = addDependency("org.junit.jupiter:junit-jupiter-api:5.x", "org.junit.jupiter.api.*"),
            before = maven,
            additionalSources = listOf(javaSource)
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["com.google.common.math.*", "com.google.common.math.IntMath"])
    fun semverSelector(onlyIfUsing: String) {
        //Parse the maven project (which will have a JavaProject Provenance associated with it)
        val maven = mavenParser.parseMavenProjects(
            source = """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                </project>
            """
        )

        //Parse the java with a main source set and associate with the java project.
        val javaSource = javaParser.parse(
            source = usingGuavaIntMath,
            sourceSet = "main",
            markers = listOf(maven.getJavaProject())
        )

        assertChanged(
            recipe = AddDependency(
                "com.google.guava", "guava", "29.x", "-jre",
                null, false, onlyIfUsing, null, null, false, null
            ),
            before = maven,
            additionalSources = listOf(javaSource),
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
    fun addTestDependenciesAfterCompile() {
        //Parse the maven project (which will have a JavaProject Provenance associated with it)
        val maven = mavenParser.parseMavenProjects(
            source = """
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
            """
        )

        //Parse the java with a test source set and associate with the java project.
        val javaSource = javaParser.parse(
            source = usingGuavaIntMath,
            sourceSet = "test",
            markers = listOf(maven.getJavaProject())
        )

        assertChanged(
            recipe = addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath"),
            before = maven,
            additionalSources = listOf(javaSource),
            after = """
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
            """
        )
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1392")
    @Test
    fun preserveNonTagContent() {

        //Parse the maven project (which will have a JavaProject Provenance associated with it)
        val maven = mavenParser.parseMavenProjects(
            source = """
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
                        <!-- comment 3 -->
                        <?processing instruction3?>
                    </dependencies>
                </project>
            """
        )

        //Parse the java with a test source set and associate with the java project.
        val javaSource = javaParser.parse(
            source = usingGuavaIntMath,
            sourceSet = "test",
            markers = listOf(maven.getJavaProject())
        )

        assertChanged(
            recipe = addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath"),
            before = maven,
            additionalSources = listOf(javaSource),
            after = """
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
                        <!-- comment 3 -->
                        <?processing instruction3?>
                    </dependencies>
                </project>
            """
        )
    }

    @Test
    fun addDependencyDoesntAddWhenExistingDependency() {
        //Parse the maven project (which will have a JavaProject Provenance associated with it)
        val maven = mavenParser.parseMavenProjects(
            source = """
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
            """
        )

        //Parse the java with a main source set and associate with the java project.
        val javaSource = javaParser.parse(
            source = usingGuavaIntMath,
            sourceSet = "main",
            markers = listOf(maven.getJavaProject())
        )

        assertUnchanged(
            recipe = addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath"),
            before = maven,
            additionalSources = listOf(javaSource)
        )
    }

    @Test
    fun useManaged() {

        //Parse the maven project (which will have a JavaProject Provenance associated with it)
        val mavens = mavenParser.parseMavenProjects(
            sources = arrayOf(
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
                """,
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
                """
            )
        )

        //Parse the java with a main source set and associate with the child project.
        val javaSource = javaParser.parse(
            source = usingGuavaIntMath,
            sourceSet = "main",
            markers = listOf(mavens[1].getJavaProject())
        )

        assertChanged(
            recipe = addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath"),
            before = mavens[1],
            additionalSources = listOf(mavens[0], javaSource),
            after = """
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
            """
        )
    }

    @Test
    fun useRequestedVersionInUseByOtherMembersOfTheFamily() {

        //Parse the maven project (which will have a JavaProject Provenance associated with it)
        val maven = mavenParser.parseMavenProjects(
            source = """
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
            """
        )

        //Parse the java with a main source set and associate with the java project.
        val javaSource = javaParser.parse(
            source = """
                public class A {
                    com.fasterxml.jackson.databind.ObjectMapper mapper;
                }
            """,

            sourceSet = "main",
            markers = listOf(maven.getJavaProject())
        )

        assertChanged(
            recipe = AddDependency("com.fasterxml.jackson.module", "jackson-module-afterburner", "2.10.5",
                null, null, false, "com.fasterxml.jackson.databind.*",
                null, null, null, "com.fasterxml.*"
            ),
            before = maven,
            additionalSources = listOf(javaSource),
            after = """
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
            """
        )
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1443")
    @Test
    fun addTransitiveDependencyAsDirect() {

        //Parse the maven project (which will have a JavaProject Provenance associated with it)
        val maven = mavenParser.parseMavenProjects(
            source = """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.fasterxml.jackson.core</groupId>
                            <artifactId>jackson-databind</artifactId>
                            <version>2.12.0</version>
                        </dependency>
                    </dependencies>
                </project>
            """
        )

        //Parse the java with a main source set and associate with the java project.
        val javaSource = javaParser.parse(
            source = """
                public class A {
                    com.fasterxml.jackson.core.Versioned v;
                }
            """,
            sourceSet = "main",
            markers = listOf(maven.getJavaProject())
        )

        assertChanged(
            recipe = addDependency("com.fasterxml.jackson.core:jackson-core:2.12.0", "com.fasterxml.jackson.core.*"),
            before = maven,
            additionalSources = listOf(javaSource),
            after = """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.fasterxml.jackson.core</groupId>
                            <artifactId>jackson-core</artifactId>
                            <version>2.12.0</version>
                        </dependency>
                        <dependency>
                            <groupId>com.fasterxml.jackson.core</groupId>
                            <artifactId>jackson-databind</artifactId>
                            <version>2.12.0</version>
                        </dependency>
                    </dependencies>
                </project>
            """
        )
    }

    private fun addDependency(gav: String, onlyIfUsing: String): AddDependency {
        val (group, artifact, version) = gav.split(":")
        return AddDependency(
            group, artifact, version, null, null, true,
            onlyIfUsing, null, null, false, null
        )
    }
}
