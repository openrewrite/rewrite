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
package org.openrewrite

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Sanity check for the runtime stub. When code that uses `recipe { ... }` is
 * compiled without the rewrite-kotlin K2 compiler plugin loaded, the stub
 * throws a clear setup error rather than silently returning a broken Recipe.
 *
 * End-to-end tests that exercise the plugin's transformations live in
 * `RecipePluginSmokeTest` / `RecipeDslCheckerTest`, which drive kotlinc with
 * the plugin enabled via kotlin-compile-testing.
 */
class RecipeDslRuntimeStubTest {

    @Test
    fun `recipe stub throws a clear plugin setup error when invoked without the plugin`() {
        val ex = assertThrows(IllegalStateException::class.java) {
            recipe(
                displayName = "Stub",
                description = "(this block is never invoked at runtime under the plugin)",
            ) { }
        }
        assertTrue(
            ex.message!!.contains("compiler plugin"),
            "Expected error to point at compiler plugin setup, got: ${ex.message}",
        )
    }
}
