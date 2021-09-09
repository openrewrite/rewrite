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

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Issue
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.marker.JavaSearchResult
import org.openrewrite.java.marker.JavaVersion
import org.openrewrite.java.tree.J
import java.util.*

interface UsesJavaVersionTest : JavaRecipeTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/983")
    @Test
    fun invalidJavaVersion(jp: JavaParser) {
        val sourceFiles = sourceFilesWithJavaVersion()
        val recipe = toRecipe { UsesJavaVersion(-1, Int.MAX_VALUE) }
        val result = recipe.run(sourceFiles, executionContext)
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result.isEmpty()).isTrue
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/983")
    @Test
    fun findJavaVersion(jp: JavaParser) {
        val sourceFiles = sourceFilesWithJavaVersion()
        val recipe = toRecipe { UsesJavaVersion(getJavaVersion().majorVersion) }
        val result = recipe.run(sourceFiles, executionContext)
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result.isNotEmpty())
        Assertions.assertThat(result[0].after!!.markers.markers.map {
            j -> j is JavaSearchResult
        }.any()).isTrue
    }

    @Test
    fun usesJavaVersion(jp: JavaParser) {
        val usesJavaVersion = UsesJavaVersion<ExecutionContext>(8, 11)
        Assertions.assertThat(usesJavaVersion.majorVersionMin).isEqualTo(8)
        Assertions.assertThat(usesJavaVersion.majorVersionMax).isEqualTo(11)
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
