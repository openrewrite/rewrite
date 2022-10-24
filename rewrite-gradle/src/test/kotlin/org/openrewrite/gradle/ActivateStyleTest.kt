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
@file:Suppress("UnusedProperty")

package org.openrewrite.gradle

import org.junit.jupiter.api.Test
import org.openrewrite.gradle.Assertions.buildGradle
import org.openrewrite.properties.Assertions.properties
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.SourceSpecs.other
import org.openrewrite.test.TypeValidation

class ActivateStyleTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(ActivateStyle("org.openrewrite.java.IntelliJ", true))
            .typeValidationOptions(TypeValidation.none())
    }

    @Test
    fun addToRewriteDsl() = rewriteRun(
        buildGradle("""
            plugins {
                id("java")
                id("org.openrewrite.rewrite")
            }

            rewrite {
                activeRecipe("org.openrewrite.java.format.AutoFormat")
            }
        """,
        """
            plugins {
                id("java")
                id("org.openrewrite.rewrite")
            }

            rewrite {
                activeRecipe("org.openrewrite.java.format.AutoFormat")
                activeStyle("org.openrewrite.java.IntelliJ")
            }
        """)
    )

    @Test
    fun addToRewriteDslExistingStyle() = rewriteRun(
        { spec -> spec.recipe(ActivateStyle("org.openrewrite.java.IntelliJ", false)) },
        buildGradle("""
            plugins {
                id("java")
                id("org.openrewrite.rewrite")
            }

            rewrite {
                activeRecipe("org.openrewrite.java.format.AutoFormat")
                activeStyle("otherStyle")
            }
        """,
            """
            plugins {
                id("java")
                id("org.openrewrite.rewrite")
            }

            rewrite {
                activeRecipe("org.openrewrite.java.format.AutoFormat")
                activeStyle("otherStyle", "org.openrewrite.java.IntelliJ")
            }
        """)
    )

    @Test
    fun addToRewriteDslOverwriteStyle() = rewriteRun(
        buildGradle("""
            plugins {
                id("java")
                id("org.openrewrite.rewrite")
            }

            rewrite {
                activeRecipe("org.openrewrite.java.format.AutoFormat")
                activeStyle("com.your.Style")
            }
        """,
            """
            plugins {
                id("java")
                id("org.openrewrite.rewrite")
            }

            rewrite {
                activeRecipe("org.openrewrite.java.format.AutoFormat")
                activeStyle("org.openrewrite.java.IntelliJ")
            }
        """)
    )

    @Test
    fun addToProperties() = rewriteRun(
        other("") { spec -> spec.path("build.gradle.kts") },
        properties("""
            org.gradle.someProp=true
        """,
        """
            org.gradle.someProp=true
            systemProp.rewrite.activeStyles=org.openrewrite.java.IntelliJ
        """) { spec -> spec.path("gradle.properties") }
    )

    @Test
    fun addToPropertiesStyles() = rewriteRun(
        { spec -> spec.recipe(ActivateStyle("org.openrewrite.java.IntelliJ", false)) },
        other("") { spec -> spec.path("build.gradle.kts") },
        properties(
            """
            org.gradle.someProp=true
            systemProp.rewrite.activeStyles=org.openrewrite.java.Other
        """,
            """
            org.gradle.someProp=true
            systemProp.rewrite.activeStyles=org.openrewrite.java.Other,org.openrewrite.java.IntelliJ
        """
        ) { spec -> spec.path("gradle.properties") }
    )

    @Test
    fun overwritePropertiesStyles() = rewriteRun(
        other("") { spec -> spec.path("build.gradle.kts") },
        properties(
            """
            org.gradle.someProp=true
            systemProp.rewrite.activeStyles=org.openrewrite.java.Other
        """,
            """
            org.gradle.someProp=true
            systemProp.rewrite.activeStyles=org.openrewrite.java.IntelliJ
        """
        ) { spec -> spec.path("gradle.properties") }
    )
}
