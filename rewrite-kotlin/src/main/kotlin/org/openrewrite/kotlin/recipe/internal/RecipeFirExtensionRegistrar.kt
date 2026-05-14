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

// Bundles all FIR-level extensions that the recipe DSL plugin contributes.
//
// Currently wired:
//   - [RecipeDslAdditionalCheckers] — validates recipe declarations (mutual exclusion
//     between pattern mode and phase mode today; more rules to come).
//
// Future:
//   - A `FirDeclarationGenerationExtension` that emits a synthetic `Recipe` subclass
//     per top-level `recipe(name) { ... }` val, with metadata + KotlinTemplate
//     strings extracted from the user's lambda bodies.

internal class RecipeFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::RecipeDslAdditionalCheckers
    }
}
