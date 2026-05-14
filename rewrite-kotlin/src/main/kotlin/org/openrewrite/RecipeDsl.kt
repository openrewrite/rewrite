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
@file:JvmName("RecipeDsl")
package org.openrewrite

import org.openrewrite.kotlin.recipe.internal.RecipeRegistry
import java.time.Duration

// Author-facing surface for the Kotlin recipe authoring DSL. The trailing block of
// [recipe] is consumed by the rewrite-kotlin K2 compiler plugin at COMPILE TIME — the
// plugin reads the metadata setters, the `rewrite ... to ...` clause, and the
// `scan/edit/generate` clauses, then generates a real `Recipe` subclass per
// declaration. The block's runtime invocation is intentionally a no-op; the lambdas
// inside it exist for IDE auto-complete, type-checking, and human readability only.
//
// At runtime, [recipe] looks up the generated `Recipe` instance by name and returns
// it. If the compiler plugin did not run for the enclosing module, [recipe] throws
// with a clear message pointing at the plugin setup.

/** Marker that pins receiver scopes inside the recipe DSL. */
@DslMarker
public annotation class RecipeDslMarker

/**
 * Top-level entry point. The author writes:
 *
 *     val UseUppercase = recipe("Use uppercase") {
 *         description = "..."
 *         rewrite { s: String -> s.lowercase() } to { s -> s.uppercase() }
 *     }
 *
 * The rewrite-kotlin compiler plugin processes the trailing block at compile time
 * and generates a real `Recipe` subclass; this runtime function looks up that
 * subclass by name.
 */
public fun recipe(name: String, @Suppress("UNUSED_PARAMETER") block: RecipeBuilder.() -> Unit): Recipe =
    RecipeRegistry.lookup(name)
        ?: error(
            "Recipe DSL: no compiled recipe found for '$name'. The rewrite-kotlin " +
                "compiler plugin must run on this module — add rewrite-kotlin to " +
                "`kotlinCompilerPluginClasspath` (plugin id `org.openrewrite.kotlin.recipe`)."
        )

/**
 * Receiver scope inside the [recipe] block. None of these methods do real work at
 * runtime — they exist as type stubs for the compiler plugin to walk.
 */
@RecipeDslMarker
public class RecipeBuilder internal constructor() {
    public var displayName: String = ""
    public var description: String = ""
    public var estimatedEffortPerOccurrence: Duration? = null
    public var tags: Set<String> = emptySet()

    // === Pattern mode ===
    // One `rewrite ... to ...` clause per recipe. Multiple BEFORE lambdas pair with a
    // single AFTER lambda. The `to` infix returns Unit — chaining a second rewrite is
    // not a type error here but the compiler plugin rejects it at compile time.

    public fun <P, R> rewrite(before: (P) -> R): RewriteAdvice1<P, R> = RewriteAdvice1()
    public fun <P, R> rewrite(first: (P) -> R, vararg rest: (P) -> R): RewriteAdvice1<P, R> = RewriteAdvice1()

    public fun <P1, P2, R> rewrite(before: (P1, P2) -> R): RewriteAdvice2<P1, P2, R> = RewriteAdvice2()
    public fun <P1, P2, R> rewrite(first: (P1, P2) -> R, vararg rest: (P1, P2) -> R): RewriteAdvice2<P1, P2, R> = RewriteAdvice2()

    // === Phase mode ===
    // `scan / edit / generate` map to the standard ScanningRecipe lifecycle. The
    // compiler plugin rejects mixing phase calls with a `rewrite ... to ...` clause
    // in the same recipe.

    public fun <A : Any> scan(initial: A, block: ScanScope<A>.() -> Unit): ScanRef<A> = ScanRef()
    public fun edit(block: EditScope.() -> Unit): Unit = Unit
    public fun <A : Any> edit(scan: ScanRef<A>, block: EditScopeWithAcc<A>.() -> Unit): Unit = Unit
    public fun <A : Any> generate(scan: ScanRef<A>, block: GenerateScope<A>.() -> Unit): Unit = Unit
}

@RecipeDslMarker public class RewriteAdvice1<P, R> internal constructor() {
    public infix fun to(after: (P) -> R): Unit = Unit
}

@RecipeDslMarker public class RewriteAdvice2<P1, P2, R> internal constructor() {
    public infix fun to(after: (P1, P2) -> R): Unit = Unit
}

/**
 * Reference to the accumulator created by a [RecipeBuilder.scan] call, used to bind
 * `edit`/`generate` to the same accumulator.
 */
public class ScanRef<A> internal constructor()

@RecipeDslMarker public class ScanScope<A> internal constructor() {
    public val acc: A get() = error("Stub — the compiler plugin processes the body; runtime is never invoked.")
}

@RecipeDslMarker public class EditScope internal constructor()

@RecipeDslMarker public class EditScopeWithAcc<A> internal constructor() {
    public val acc: A get() = error("Stub — the compiler plugin processes the body; runtime is never invoked.")
}

@RecipeDslMarker public class GenerateScope<A> internal constructor() {
    public val acc: A get() = error("Stub — the compiler plugin processes the body; runtime is never invoked.")
}
