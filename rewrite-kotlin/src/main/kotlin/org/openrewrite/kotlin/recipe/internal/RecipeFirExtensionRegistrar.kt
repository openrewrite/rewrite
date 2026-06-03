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
package org.openrewrite.kotlin.recipe.internal

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

// Bundles all FIR-level extensions that the recipe DSL plugin contributes:
//   - `RecipeDslAdditionalCheckers` runs `RecipeFirDslCheckers` on each user
//     property declaration whose initializer is `org.openrewrite.recipe(...)`.
//   - `RecipeFirMappedTypeFallbackExtension` synthesizes top-level extension
//     functions on Kotlin mapped types (`kotlin.String`, `kotlin.Int`, …) that
//     re-expose Java instance methods Kotlin's mapped-type member scope hides
//     (e.g. JDK 15+ `String.formatted`/`transform`/`stripIndent`/`translateEscapes`).
//     See KT-52378.
internal class RecipeFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::RecipeDslAdditionalCheckers
        +::RecipeFirMappedTypeFallbackExtension
    }
}
