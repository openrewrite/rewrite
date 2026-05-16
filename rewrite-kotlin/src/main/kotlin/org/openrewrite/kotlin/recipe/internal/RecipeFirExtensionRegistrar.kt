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
// Phase 3 of the DSL rewrite re-populates this with `RecipeFirDslCheckers` —
// currently empty so the plugin is well-formed but contributes no FIR checks.
internal class RecipeFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        // intentionally empty until RecipeFirDslCheckers lands in Phase 3
    }
}
