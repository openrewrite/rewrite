/*
 * Copyright 2026 the original author or authors.
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
@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)
package org.openrewrite.kotlin.recipe

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.intellij.lang.annotations.Language
import org.openrewrite.kotlin.recipe.internal.RecipeCompilerPluginRegistrar

/**
 * Test fixture for compiling Kotlin source with the rewrite-kotlin recipe DSL compiler
 * plugin loaded via kotlin-compile-testing. Used by checker tests, declaration
 * generation tests, and end-to-end DSL compile/run tests.
 *
 * Why a shared fixture: every test in this area needs the same kotlinc-with-plugin
 * setup, and the right configuration knobs (K2 on, plugin registrar wired, classpath
 * carrying the runtime DSL surface) are non-trivial. Centralizing prevents drift.
 */
internal object RecipePluginCompileFixture {

    /**
     * Compile a single Kotlin source file with the rewrite-kotlin recipe DSL plugin
     * enabled. The runtime DSL surface (RecipeDsl.kt) is on the inheriting classpath,
     * so authors can `import org.openrewrite.recipe`.
     */
    fun compile(@Language("kotlin") source: String, fileName: String = "Recipes.kt"): JvmCompilationResult {
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin(fileName, source))
            compilerPluginRegistrars = listOf(RecipeCompilerPluginRegistrar())
            // Inherit the test classpath so `org.openrewrite.recipe` resolves to the
            // RecipeDsl.kt symbols in rewrite-kotlin's main source set.
            inheritClassPath = true
            messageOutputStream = System.out
            verbose = false
        }
        return compilation.compile()
    }
}

internal fun JvmCompilationResult.exitOk(): Boolean = exitCode == KotlinCompilation.ExitCode.OK

