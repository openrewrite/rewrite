/*
 * Copyright 2022 the original author or authors.
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.openrewrite.Issue
import org.openrewrite.java.JavaParser
import org.openrewrite.maven.tree.Scope
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

class AddDependencyTest2 : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        // example of configuring a JavaVersion marker on every source file parsed by every test method
        spec.allSources().java().version(8)
    }

    @ParameterizedTest
    @ValueSource(strings = ["com.google.common.math.*", "com.google.common.math.IntMath"])
    fun addDependency(onlyIfUsing: String) = rewriteRun(
        { spec ->
            spec
                .recipe(addDependency("com.google.guava:guava:29.0-jre", onlyIfUsing))
                .parser(JavaParser.fromJavaVersion().classpath("guava").build())
        },
        mavenProject("server",
            srcMainJava(
                java(
                    """
                        import com.google.common.math.IntMath;
                        public class A {
                            boolean getMap() {
                                //noinspection UnstableApiUsage
                                return IntMath.isPrime(5);
                            }
                        }
                    """,
                )
            ),
            pomXml(
                """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app-server</artifactId>
                        <version>1</version>
                    </project>
                """,
                """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app-server</artifactId>
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
            ) { spec ->
                spec.afterRecipe { maven ->
                    assertThat(
                        maven.mavenResolutionResult()
                            .findDependencies("com.google.guava", "guava", Scope.Compile)
                    ).isNotEmpty()
                }
            }
        ),
        pomXml(
            """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                </project>
            """
        )
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1765")
    @Test
    fun `dependency added with test scope`() = rewriteRun(
        { spec ->
            spec
                .recipe(addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath"))
                .parser(JavaParser.fromJavaVersion().classpath("guava").logCompilationWarningsAndErrors(true).build())
        },
        mavenProject("server",
            srcMainJava(
                java(
                    """
                        public class A {
                            public int asInt(String num){
                                return Integer.parseInt(num);
                            }
                        }
                    """,
                )
            ),
            srcTestJava(
                java(
                    """
                        import com.google.common.math.IntMath;
                        public class ATest {
                            boolean isPrime() {
                                A a = new A();
                                //noinspection UnstableApiUsage
                                assert(IntMath.isPrime(a.asInt("1")));
                            }
                        }
                    """,
                )
            ),
            pomXml(
                """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app-server</artifactId>
                        <version>1</version>
                    </project>
                """,
                """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app-server</artifactId>
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
            ) { spec ->
                spec.afterRecipe { maven ->
                    assertThat(
                        maven.mavenResolutionResult()
                            .findDependencies("com.google.guava", "guava", Scope.Test)
                    ).isNotEmpty()
                }
            }
        )
    )

    private fun addDependency(gav: String, onlyIfUsing: String): AddDependency {
        val (group, artifact, version) = gav.split(":")
        return AddDependency(
            group, artifact, version, null, null, true,
            onlyIfUsing, null, null, false, null
        )
    }
}
