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

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ChangeJavaCompatibilityTest : GradleRecipeTest {
    @Nested
    @DisplayName("When requested version is an integer")
    class NewInteger : BaseTest("8")

    @Nested
    @DisplayName("When requested version is a double")
    class NewDouble : BaseTest("1.8")

    @Nested
    @DisplayName("When requested version is a string")
    class NewString : BaseTest("\"1.8\"")

    @Nested
    @DisplayName("When requested version is an enum (shorthand)")
    class NewEnumShorthand : BaseTest("VERSION_1_8")

    @Nested
    @DisplayName("When requested version is an enum")
    class NewEnum : BaseTest("JavaVersion.VERSION_1_8")

    abstract class BaseTest(val newVersion: String) : GradleRecipeTest {
        @Test
        fun changeSourceCompatibility() = assertChanged(
            recipe = ChangeJavaCompatibility(newVersion, "source"),
            before = """
                plugins {
                    java
                }
                
                sourceCompatibility = 7
            """,
            after = """
                plugins {
                    java
                }
                
                sourceCompatibility = ${coerce(newVersion)}
            """
        )

        @Test
        fun changeTargetCompatibility() = assertChanged(
            recipe = ChangeJavaCompatibility(newVersion, "target"),
            before = """
                plugins {
                    java
                }
                
                targetCompatibility = 7
            """,
            after = """
                plugins {
                    java
                }
                
                targetCompatibility = ${coerce(newVersion)}
            """
        )

        @Test
        fun changeSetSourceCompatibility() = assertChanged(
            recipe = ChangeJavaCompatibility(newVersion, "source"),
            before = """
                plugins {
                    java
                }
                
                setSourceCompatibility 7
            """,
            after = """
                plugins {
                    java
                }
                
                setSourceCompatibility ${coerce(newVersion)}
            """
        )

        @Test
        fun changeSetTargetCompatibility() = assertChanged(
            recipe = ChangeJavaCompatibility(newVersion, "target"),
            before = """
                plugins {
                    java
                }
                
                setTargetCompatibility 7
            """,
            after = """
                plugins {
                    java
                }
                
                setTargetCompatibility ${coerce(newVersion)}
            """
        )

        @Test
        fun changeSourceCompatibility_JavaPluginExtension() = assertChanged(
            recipe = ChangeJavaCompatibility(newVersion, "source"),
            before = """
                plugins {
                    java
                }
                
                java {
                    sourceCompatibility = "1.7"
                }
            """,
            after = """
                plugins {
                    java
                }
                
                java {
                    sourceCompatibility = ${coerce(newVersion)}
                }
            """
        )

        @Test
        fun changeTargetCompatibility_JavaPluginExtension() = assertChanged(
            recipe = ChangeJavaCompatibility(newVersion, "target"),
            before = """
                plugins {
                    java
                }
                
                java {
                    targetCompatibility = "1.7"
                }
            """,
            after = """
                plugins {
                    java
                }
                
                java {
                    targetCompatibility = ${coerce(newVersion)}
                }
            """
        )

        @Test
        fun changeSourceCompatibility_CompileJava() = assertChanged(
            recipe = ChangeJavaCompatibility(newVersion, "source"),
            before = """
                plugins {
                    java
                }
                
                compileJava {
                    sourceCompatibility = "1.7"
                }
            """,
            after = """
                plugins {
                    java
                }
                
                compileJava {
                    sourceCompatibility = ${coerce(newVersion)}
                }
            """
        )

        @Test
        fun changeTargetCompatibility_CompileJava() = assertChanged(
            recipe = ChangeJavaCompatibility(newVersion, "target"),
            before = """
                plugins {
                    java
                }
                
                compileJava {
                    targetCompatibility = "1.7"
                }
            """,
            after = """
                plugins {
                    java
                }
                
                compileJava {
                    targetCompatibility = ${coerce(newVersion)}
                }
            """
        )

        @Test
        fun changeSourceCompatibility_TasksNamedCompileJava() = assertChanged(
            recipe = ChangeJavaCompatibility(newVersion, "source"),
            before = """
                plugins {
                    java
                }
                
                tasks.named("compileJava") {
                    sourceCompatibility = "1.7"
                }
            """,
            after = """
                plugins {
                    java
                }
                
                tasks.named("compileJava") {
                    sourceCompatibility = ${coerce(newVersion)}
                }
            """
        )

        @Test
        fun changeTargetCompatibility_TasksNamedCompileJava() = assertChanged(
            recipe = ChangeJavaCompatibility(newVersion, "target"),
            before = """
                plugins {
                    java
                }
                
                tasks.named("compileJava") {
                    targetCompatibility = "1.7"
                }
            """,
            after = """
                plugins {
                    java
                }
                
                tasks.named("compileJava") {
                    targetCompatibility = ${coerce(newVersion)}
                }
            """
        )

        @Test
        fun changeSourceCompatibility_TasksWithTypeJavaCompile() = assertChanged(
            recipe = ChangeJavaCompatibility(newVersion, "source"),
            before = """
                plugins {
                    java
                }
                
                tasks.withType(JavaCompile).configureEach {
                    sourceCompatibility = "1.7"
                }
            """,
            after = """
                plugins {
                    java
                }
                
                tasks.withType(JavaCompile).configureEach {
                    sourceCompatibility = ${coerce(newVersion)}
                }
            """
        )

        @Test
        fun changeTargetCompatibility_TasksWithTypeJavaCompile() = assertChanged(
            recipe = ChangeJavaCompatibility(newVersion, "target"),
            before = """
                plugins {
                    java
                }
                
                tasks.withType(JavaCompile).configureEach {
                    targetCompatibility = "1.7"
                }
            """,
            after = """
                plugins {
                    java
                }
                
                tasks.withType(JavaCompile).configureEach {
                    targetCompatibility = ${coerce(newVersion)}
                }
            """
        )

        private fun coerce(version: String): String {
            return when {
                version.startsWith("JavaVersion.") -> version
                version.startsWith("VERSION_") -> "JavaVersion.$version"
                else -> version
            }
        }
    }
}