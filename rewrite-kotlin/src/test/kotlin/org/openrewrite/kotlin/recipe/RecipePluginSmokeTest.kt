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

import com.tschuchort.compiletesting.KotlinCompilation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Smoke test that the rewrite-kotlin compiler plugin loads under kotlinc and that a
 * source file declaring a `recipe(...)` block compiles without error.
 *
 * This does NOT yet verify that the plugin generated a real Recipe subclass — that
 * comes once the FirDeclarationGenerationExtension is wired. Until then this only
 * proves the plugin registrar is reachable and the FIR extensions (currently empty)
 * don't reject otherwise-valid source.
 */
class RecipePluginSmokeTest {

    @Test
    fun `empty recipe block compiles with the plugin enabled`() {
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.recipe

            val Empty = recipe("Empty") {
                description = "Compiles but does nothing yet."
            }
            """.trimIndent()
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }
}
