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
import org.junit.jupiter.api.Test

class RecipeDslTest {

    @Test
    fun `recipe() throws a clear plugin setup error when no compiled recipe is registered`() {
        val ex = assertThrows(IllegalStateException::class.java) {
            recipe("nonexistent recipe — no compiler plugin run") {
                description = "(this block is never invoked at runtime; it's metadata for the compiler plugin)"
            }
        }
        assert(ex.message!!.contains("compiler plugin")) {
            "Expected error to point at compiler plugin setup, got: ${ex.message}"
        }
    }

    // End-to-end tests (compiler plugin runs, recipe is registered, transformation
    // applies) use kotlin-compile-testing to drive kotlinc with the plugin enabled.
}
