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

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

// Kotlin compiler plugin entry point for the recipe authoring DSL declared in
// `RecipeDsl.kt`. Registered via `META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar`.
// kotlinc loads this class when rewrite-kotlin is on the user's `kotlinCompilerPluginClasspath`.
//
// Two extensions register here:
//   - `RecipeFirExtensionRegistrar` — FIR shape checks (count limits,
//     orphan-rewrite, orphan-scan). Catches ill-formed DSL at compile time.
//   - `RecipeIrGenerationExtension` — IR pass for `rewrite { } to { }` recipes.
//     Replaces each pattern recipe's `recipe(...)` initializer with a synthetic
//     `<Name>$KtRecipe` constructor call; the synthetic class extends Recipe
//     and `getVisitor()` delegates to GeneratedRecipeSupport.
//
// Recipes with purely imperative shapes (`edit { lang { visitX { } } }` and
// `scan<A>(initial) { … }.edit { … }`) compile + run pure-JVM WITHOUT this
// plugin — the runtime DSL builder in RecipeDsl.kt covers them. The plugin is
// REQUIRED only for `rewrite { } to { }` (whose runtime stub is an `error(...)`)
// and for compile-time FIR validation.
//
// Why a compiler plugin and not KSP: KSP's stable contract exposes declarations and
// types but not expression bodies (property initializers, lambda contents). The DSL
// needs to walk the bodies of the user's `rewrite { p -> p.foo() } to { ... }` lambdas,
// which only the FIR + IR extension surface supports. See project memory
// `lane-e-design-2026-05-14`.

@OptIn(ExperimentalCompilerApi::class)
public class RecipeCompilerPluginRegistrar : CompilerPluginRegistrar() {

    override val pluginId: String = "org.openrewrite.kotlin.recipe"

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(RecipeFirExtensionRegistrar())
        IrGenerationExtension.registerExtension(RecipeIrGenerationExtension())
    }
}
