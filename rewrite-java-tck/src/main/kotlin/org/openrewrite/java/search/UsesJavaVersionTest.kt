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
package org.openrewrite.java.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Issue
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.marker.JavaVersion
import org.openrewrite.java.tree.J
import org.openrewrite.marker.SearchResult
import java.util.*

interface UsesJavaVersionTest : JavaRecipeTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/2035")
    @Test
    fun mavenCompilerSources() {
        val java8 = getMajorVersion("8")
        assertThat(java8).isEqualTo(8)

        val java1dot8 = getMajorVersion("1.8")
        assertThat(java1dot8).isEqualTo(8)

        val java11 = getMajorVersion("11")
        assertThat(java11).isEqualTo(11)

        val java17 = getMajorVersion("17")
        assertThat(java17).isEqualTo(17)
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2035")
    @Test
    fun javaRuntimeVersions() {
        val java8 = getMajorVersion("1.8.0.332")
        assertThat(java8).isEqualTo(8)

        val java11 = getMajorVersion("11.0.15")
        assertThat(java11).isEqualTo(11)

        val java17 = getMajorVersion("17.0.3")
        assertThat(java17).isEqualTo(17)
    }

    fun getMajorVersion(versionNumber: String): Int {
        return JavaVersion(UUID.randomUUID(), "", "", versionNumber, versionNumber).majorVersion
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/983")
    @Test
    fun invalidJavaVersion(jp: JavaParser) {
        val sourceFiles = sourceFilesWithJavaVersion()
        val recipe = toRecipe { UsesJavaVersion(-1, Int.MAX_VALUE) }
        val result = recipe.run(sourceFiles, executionContext)
        assertThat(result).isNotNull
        assertThat(result.isEmpty()).isTrue
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/983")
    @Test
    fun findJavaVersion(jp: JavaParser) {
        val sourceFiles = sourceFilesWithJavaVersion()
        val recipe = toRecipe { UsesJavaVersion(getJavaVersion().majorVersion) }
        val result = recipe.run(sourceFiles, executionContext)
        assertThat(result).isNotNull
        assertThat(result.isNotEmpty())
        assertThat(result[0].after!!.markers.markers.map {
            j -> j is SearchResult
        }.any()).isTrue
    }

    @Test
    fun usesJavaVersion(jp: JavaParser) {
        val usesJavaVersion = UsesJavaVersion<ExecutionContext>(8, 11)
        assertThat(usesJavaVersion.majorVersionMin).isEqualTo(8)
        assertThat(usesJavaVersion.majorVersionMax).isEqualTo(11)
    }

    private fun sourceFilesWithJavaVersion(): List<J.CompilationUnit> {
        val input = arrayOf("""class Test {}""")
        return parser.parse(executionContext, *input).map { j ->
            j.withMarkers(
                j.markers.addIfAbsent(getJavaVersion())
            )
        }
    }

    private fun getJavaVersion(): JavaVersion {
        val javaRuntimeVersion = System.getProperty("java.runtime.version")
        val javaVendor = System.getProperty("java.vm.vendor")
        return JavaVersion(UUID.randomUUID(), javaRuntimeVersion, javaVendor, javaRuntimeVersion, javaRuntimeVersion)
    }
}
