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
package org.openrewrite.gradle

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.PathUtils
import org.openrewrite.gradle.util.GradleWrapper.WRAPPER_JAR_LOCATION
import org.openrewrite.remote.Remote
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.text.PlainText
import java.net.URI
import java.nio.file.Paths

@Suppress("UnusedProperty")
class AddGradleWrapperTest : RewriteTest {
    companion object {
        const val GRADLE_WRAPPER_PROPERTIES = """
            distributionBase=GRADLE_USER_HOME
            distributionPath=wrapper/dists
            distributionUrl=https\://services.gradle.org/distributions/gradle-7.4.2-bin.zip
            zipStoreBase=GRADLE_USER_HOME
            zipStorePath=wrapper/dists
        """
    }

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(
            //language=yaml
            """
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.test.AddGradleWrapper
            displayName: Adds a Gradle wrapper
            description: Add wrapper for gradle version 7.4.2
            recipeList:
              - org.openrewrite.gradle.AddGradleWrapper:
                  version: "7.4.2"
        """.trimIndent().byteInputStream(), "org.openrewrite.test.AddGradleWrapper"
        )
    }

    @Test
    fun addWrapper() = rewriteRun(
        { spec ->
            spec.afterRecipe { results ->
                val gradleSh =
                    results.map { it.after }.filterIsInstance<PlainText>().first { it.sourcePath.endsWith("gradlew") }
                assertThat(gradleSh.text).isNotBlank
                assertThat(gradleSh.fileAttributes!!.isExecutable).isTrue

                val gradleBat = results.map { it.after }.filterIsInstance<PlainText>()
                    .first { it.sourcePath.endsWith("gradlew.bat") }
                assertThat(gradleBat.text).isNotBlank
                assertThat(gradleBat.fileAttributes!!.isExecutable).isTrue

                val gradleWrapperJar = results.map { it.after }.filterIsInstance<Remote>()
                    .first { it.sourcePath.endsWith("gradle-wrapper.jar") }
                assertThat(PathUtils.equalIgnoringSeparators(gradleWrapperJar.sourcePath, WRAPPER_JAR_LOCATION))
                assertThat(gradleWrapperJar.uri).isEqualTo(URI.create("https://services.gradle.org/distributions/gradle-7.4.2-bin.zip"))
            }
        },
        buildGradle(""),
        dir(
            "gradle/wrapper",
            properties(null, GRADLE_WRAPPER_PROPERTIES) { spec ->
                spec.path(Paths.get("gradle-wrapper.properties"))
            }
        )
    )

    @Test
    fun addWrapperWhenIncomplete() = rewriteRun(
        { spec ->
            spec.afterRecipe { results ->
                val gradleWrapperJar = results.map { it.after }.filterIsInstance<Remote>()
                    .first { it.sourcePath.endsWith("gradle-wrapper.jar") }
                assertThat(PathUtils.equalIgnoringSeparators(gradleWrapperJar.sourcePath, WRAPPER_JAR_LOCATION))
                assertThat(gradleWrapperJar.uri).isEqualTo(URI.create("https://services.gradle.org/distributions/gradle-7.4.2-bin.zip"))
            }.expectedCyclesThatMakeChanges(1)
        },
        other("") { spec -> spec.path("gradlew") },
        other("") { spec -> spec.path("gradlew.bat") },
        buildGradle(""),
    )

    @Test
    fun addWrapperToGradleKotlin() = rewriteRun(
        { spec ->
            spec.afterRecipe { results -> results.isNotEmpty() }.expectedCyclesThatMakeChanges(1)
        },
        other("") { spec -> spec.path("build.gradle.kts") }
    )

    @Test
    fun dontAddWrapperToMavenProject() = rewriteRun(
        { spec ->
            spec.afterRecipe { results -> results.isEmpty() }
        },
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
}
