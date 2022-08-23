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
package org.openrewrite.java.tree

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Parser
import org.openrewrite.java.JavaParser
import org.openrewrite.java.search.FindTypes
import java.nio.file.Paths
import java.util.Collections.singletonList
import java.util.zip.ZipFile
import kotlin.io.path.ExperimentalPathApi

interface JavaParserTest {
    @Test
    fun dependenciesFromClasspathDoesntExist(jp: JavaParser.Builder<*, *>) {
        assertThatThrownBy { JavaParser.dependenciesFromClasspath("dne", "another") }
            .hasMessageStartingWith("Unable to find runtime dependencies beginning with: 'another', 'dne'")
    }

    @Test
    fun matchMultipleDependenciesWithOneName(jp: JavaParser.Builder<*, *>) {
        assertThat(JavaParser.dependenciesFromClasspath("junit").size).isGreaterThan(1)
    }

    @ExperimentalPathApi
    @Test
    fun byteArrayAsClasspathElement(jp: JavaParser.Builder<*, *>) {
        val zipFile = ZipFile(JavaParser.dependenciesFromClasspath("junit-jupiter-api").first().toFile())

        zipFile.use { zip ->
            val classpath = zip.entries().asSequence()
                .filter { entry -> entry.name.endsWith(".class") }
                .map { entry ->
                    zip.getInputStream(entry).use { input ->
                        input.readBytes()
                    }
                }
                .toList()

            val context = InMemoryExecutionContext { t -> println(t.printStackTrace()) }
            val cus = jp.classpath(*classpath.toTypedArray())
                .logCompilationWarningsAndErrors(true)
                .build()
                .parse(context,
                    """
                    class Test {
                        @org.junit.jupiter.api.Test
                        void test() {
                        }
                    }
                """.trimIndent()
                )

            assertThat(FindTypes("org.junit.jupiter.api.Test", false).run(cus).results).isNotEmpty
        }
    }

    @Test
    fun relativeSourcePath(jp: JavaParser) {
        val projectDir = Paths.get("/Users/jon/test")
        val sourcePath =
            Paths.get("/Users/jon/test/src/main/java/Test.java")
        val cu = jp.parseInputs(
            listOf(Parser.Input(sourcePath) { "class Test {}".byteInputStream() }),
            projectDir,
            InMemoryExecutionContext()
        )[0]

        assertThat(cu.sourcePath).isEqualTo(Paths.get("src/main/java/Test.java"))
    }

    @Test
    fun dependsOn(jp: JavaParser.Builder<*, *>) {
        val cu = jp
            .dependsOn(singletonList(Parser.Input.fromString("class A {}")))
            .build()
            .parse(
                """
                    class Test {
                        A a;
                    }
                """.trimIndent()
            )

        assertThat(
            (cu[0].classes[0].body.statements[0] as J.VariableDeclarations).typeAsFullyQualified?.fullyQualifiedName
        ).isEqualTo("A")
    }
}
