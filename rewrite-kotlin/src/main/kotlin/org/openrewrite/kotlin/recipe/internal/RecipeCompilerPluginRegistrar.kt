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
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion
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
        registerRecipeExtensions(configuration) {
            FirExtensionRegistrarAdapter.registerExtension(RecipeFirExtensionRegistrar())
            IrGenerationExtension.registerExtension(RecipeIrGenerationExtension())
        }
    }
}

// A compiler whose version differs from the one we were built against fails registration
// with a raw ClassCastException that Gradle surfaces only as "Internal compiler error".
// See https://github.com/openrewrite/rewrite/issues/8270.
internal fun registerRecipeExtensions(configuration: CompilerConfiguration, register: () -> Unit) {
    try {
        register()
    } catch (e: LinkageError) {
        // Also catches AbstractMethodError.
        reportKotlinVersionMismatch(configuration, e)
    } catch (e: ClassCastException) {
        reportKotlinVersionMismatch(configuration, e)
    }
}

private fun reportKotlinVersionMismatch(configuration: CompilerConfiguration, cause: Throwable) {
    val message = kotlinVersionMismatchMessage(cause)
    val messageCollector = configuration.get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        ?: throw IllegalStateException(message, cause)
    messageCollector.report(CompilerMessageSeverity.ERROR, message)
}

internal fun kotlinVersionMismatchMessage(cause: Throwable): String =
    "The rewrite-kotlin recipe DSL compiler plugin could not register its extensions with the Kotlin " +
            "compiler running this build (Kotlin ${KotlinCompilerVersion.getVersion() ?: "unknown"}). " +
            "rewrite-kotlin was built against Kotlin $BUILT_AGAINST_KOTLIN_VERSION; align your " +
            "kotlin(\"jvm\") version with $BUILT_AGAINST_KOTLIN_VERSION, or use a rewrite-kotlin release " +
            "built against the Kotlin version you are compiling with. " +
            "Underlying failure: ${cause::class.java.name}: ${cause.message}"

// KotlinCompilerVersion reports the compiler that is running, never the one we compiled
// against, so rewrite-kotlin's build emits its own version into this resource.
private val BUILT_AGAINST_KOTLIN_VERSION: String by lazy {
    RecipeCompilerPluginRegistrar::class.java.getResourceAsStream("/META-INF/rewrite-kotlin-compiler.version")
        ?.use { it.readBytes().toString(Charsets.UTF_8).trim() }
        ?.takeIf { it.isNotEmpty() }
        ?: "unknown"
}
