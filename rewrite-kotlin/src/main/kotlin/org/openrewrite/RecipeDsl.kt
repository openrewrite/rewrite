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

// Author-facing surface for the Kotlin recipe authoring DSL.
//
//     val UseUppercase: Recipe = recipe(
//         displayName = "Use uppercase",
//         description = "Replace lowercase() with uppercase() (illustrative).",
//     ) {
//         rewrite({ s: String -> s.lowercase() }) to { s -> s.uppercase() }
//     }
//
// The rewrite-kotlin K2 compiler plugin
//   1. walks the [recipe] call's metadata arguments and trailing lambda at
//      compile time,
//   2. generates a synthetic `Recipe` subclass per declaration (metadata from
//      the function arguments, behavior translated from the
//      rewrite/scan/edit/generate calls inside the block),
//   3. replaces the [recipe] call's IR with a constructor invocation of the
//      generated subclass.
//
// The lambda body is intentionally never invoked at runtime. Without the
// plugin loaded, [recipe] throws a setup error — the wrapper exists only as
// a syntactic anchor that gives the val a `Recipe` type and gives the plugin
// a well-defined IR call to replace.

/** Marker that pins receiver scopes inside the recipe DSL. */
@DslMarker
public annotation class RecipeDslMarker

/**
 * Top-level anchor for a recipe declaration. Metadata is carried as named
 * arguments; behavior is carried in the trailing lambda. The val initializer
 * wraps the behavior block in this call so that
 *  - the val's declared type is `Recipe` (Java abstract class from rewrite-core),
 *  - the K2 compiler plugin has a single well-defined IR call to replace
 *    with a constructor invocation of the generated `Recipe` subclass.
 *
 * `estimatedEffortPerOccurrence` is an ISO-8601 duration string (e.g. "PT5M");
 * the plugin parses it at compile time. Empty string means "not specified".
 *
 * Without the rewrite-kotlin compiler plugin loaded on the consuming module,
 * this function throws — the DSL requires the plugin.
 */
public fun recipe(
    @Suppress("UNUSED_PARAMETER") displayName: String,
    @Suppress("UNUSED_PARAMETER") description: String,
    @Suppress("UNUSED_PARAMETER") tags: Set<String> = emptySet(),
    @Suppress("UNUSED_PARAMETER") estimatedEffortPerOccurrence: String = "",
    @Suppress("UNUSED_PARAMETER") block: RecipeBuilder.() -> Unit,
): Recipe =
    error(
        "Recipe DSL: the rewrite-kotlin K2 compiler plugin is not loaded. " +
            "Add rewrite-kotlin to `kotlinCompilerPluginClasspath` " +
            "(plugin id `org.openrewrite.kotlin.recipe`)."
    )

/**
 * Receiver scope of the recipe lambda. None of these methods do real work at
 * runtime — they exist as type stubs for the compiler plugin to walk.
 */
@RecipeDslMarker
public class RecipeBuilder internal constructor() {

    // === Pattern mode ===
    // One `rewrite ... to ...` clause per recipe. Multiple BEFORE lambdas pair
    // with a single AFTER lambda. The `to` infix returns Unit — chaining a
    // second rewrite is not a type error here but the compiler plugin rejects
    // it at compile time.

    public fun <P, R> rewrite(before: (P) -> R): RewriteAdvice1<P, R> = RewriteAdvice1()
    public fun <P, R> rewrite(first: (P) -> R, vararg rest: (P) -> R): RewriteAdvice1<P, R> = RewriteAdvice1()

    public fun <P1, P2, R> rewrite(before: (P1, P2) -> R): RewriteAdvice2<P1, P2, R> = RewriteAdvice2()
    public fun <P1, P2, R> rewrite(first: (P1, P2) -> R, vararg rest: (P1, P2) -> R): RewriteAdvice2<P1, P2, R> = RewriteAdvice2()

    // === Phase mode ===
    // `scan / edit / generate` map to the standard ScanningRecipe lifecycle.
    // The compiler plugin rejects mixing phase calls with a `rewrite ... to ...`
    // clause in the same recipe.

    public fun <A : Any> scan(initial: A, block: ScanScope<A>.() -> Unit): ScanRef<A> = ScanRef()
    public fun edit(block: EditScope.() -> Unit): Unit = Unit
    public fun <A : Any> edit(scan: ScanRef<A>, block: EditScopeWithAcc<A>.() -> Unit): Unit = Unit

    /**
     * Phase-mode file generation. The block runs once after scanning is
     * complete and returns the new source files to add to the working
     * source set. Inside the lambda, `acc` exposes the scan accumulator and
     * `ctx` the active [ExecutionContext][org.openrewrite.ExecutionContext].
     *
     * Returning an empty collection is a no-op — the recipe still passes
     * through the framework's generate phase, just adding nothing.
     */
    public fun <A : Any> generate(
        scan: ScanRef<A>,
        block: GenerateScope<A>.() -> kotlin.collections.Collection<org.openrewrite.SourceFile>,
    ): Unit = Unit
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

    @Suppress("UNUSED_PARAMETER")
    public fun visitMethodInvocation(action: (org.openrewrite.java.tree.J.MethodInvocation) -> Unit): Unit = Unit

    @Suppress("UNUSED_PARAMETER")
    public fun visitClassDeclaration(action: (org.openrewrite.java.tree.J.ClassDeclaration) -> Unit): Unit = Unit

    @Suppress("UNUSED_PARAMETER")
    public fun visitMethodDeclaration(action: (org.openrewrite.java.tree.J.MethodDeclaration) -> Unit): Unit = Unit

    @Suppress("UNUSED_PARAMETER")
    public fun visitVariableDeclarations(action: (org.openrewrite.java.tree.J.VariableDeclarations) -> Unit): Unit = Unit

    @Suppress("UNUSED_PARAMETER")
    public fun visitImport(action: (org.openrewrite.java.tree.J.Import) -> Unit): Unit = Unit

    @Suppress("UNUSED_PARAMETER")
    public fun visitProperty(action: (org.openrewrite.kotlin.tree.K.Property) -> Unit): Unit = Unit
}

@RecipeDslMarker public class EditScope internal constructor() {
    @Suppress("UNUSED_PARAMETER")
    public fun visitMethodInvocation(
        action: (org.openrewrite.java.tree.J.MethodInvocation) -> org.openrewrite.java.tree.J.MethodInvocation,
    ): Unit = Unit

    @Suppress("UNUSED_PARAMETER")
    public fun visitClassDeclaration(
        action: (org.openrewrite.java.tree.J.ClassDeclaration) -> org.openrewrite.java.tree.J.ClassDeclaration,
    ): Unit = Unit

    @Suppress("UNUSED_PARAMETER")
    public fun visitMethodDeclaration(
        action: (org.openrewrite.java.tree.J.MethodDeclaration) -> org.openrewrite.java.tree.J.MethodDeclaration,
    ): Unit = Unit

    @Suppress("UNUSED_PARAMETER")
    public fun visitVariableDeclarations(
        action: (org.openrewrite.java.tree.J.VariableDeclarations) -> org.openrewrite.java.tree.J.VariableDeclarations,
    ): Unit = Unit

    @Suppress("UNUSED_PARAMETER")
    public fun visitImport(
        action: (org.openrewrite.java.tree.J.Import) -> org.openrewrite.java.tree.J.Import,
    ): Unit = Unit

    @Suppress("UNUSED_PARAMETER")
    public fun visitProperty(
        action: (org.openrewrite.kotlin.tree.K.Property) -> org.openrewrite.kotlin.tree.K.Property,
    ): Unit = Unit
}

@RecipeDslMarker public class EditScopeWithAcc<A> internal constructor() {
    public val acc: A get() = error("Stub — the compiler plugin processes the body; runtime is never invoked.")

    @Suppress("UNUSED_PARAMETER")
    public fun visitMethodInvocation(
        action: (org.openrewrite.java.tree.J.MethodInvocation) -> org.openrewrite.java.tree.J.MethodInvocation,
    ): Unit = Unit

    @Suppress("UNUSED_PARAMETER")
    public fun visitClassDeclaration(
        action: (org.openrewrite.java.tree.J.ClassDeclaration) -> org.openrewrite.java.tree.J.ClassDeclaration,
    ): Unit = Unit

    @Suppress("UNUSED_PARAMETER")
    public fun visitMethodDeclaration(
        action: (org.openrewrite.java.tree.J.MethodDeclaration) -> org.openrewrite.java.tree.J.MethodDeclaration,
    ): Unit = Unit

    @Suppress("UNUSED_PARAMETER")
    public fun visitVariableDeclarations(
        action: (org.openrewrite.java.tree.J.VariableDeclarations) -> org.openrewrite.java.tree.J.VariableDeclarations,
    ): Unit = Unit

    @Suppress("UNUSED_PARAMETER")
    public fun visitImport(
        action: (org.openrewrite.java.tree.J.Import) -> org.openrewrite.java.tree.J.Import,
    ): Unit = Unit

    @Suppress("UNUSED_PARAMETER")
    public fun visitProperty(
        action: (org.openrewrite.kotlin.tree.K.Property) -> org.openrewrite.kotlin.tree.K.Property,
    ): Unit = Unit
}

@RecipeDslMarker public class GenerateScope<A> internal constructor() {
    public val acc: A get() = error("Stub — the compiler plugin processes the body; runtime is never invoked.")
    public val ctx: org.openrewrite.ExecutionContext
        get() = error("Stub — the compiler plugin processes the body; runtime is never invoked.")
}
