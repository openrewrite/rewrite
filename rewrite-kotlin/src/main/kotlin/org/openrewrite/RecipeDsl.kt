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

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.openrewrite.dsl.scopes.LanguageScope
import org.openrewrite.java.search.UsesField
import org.openrewrite.java.search.UsesJavaVersion
import org.openrewrite.java.search.UsesMethod
import org.openrewrite.java.search.UsesType
import org.openrewrite.kotlin.recipe.GeneratedRecipeSupport
import java.time.Duration

// Author-facing surface for the Kotlin recipe-authoring DSL.
//
// Three layers (see plan `you-should-read-the-compressed-crayon.md`):
//   1. recipe(displayName, description, recipeTags, effort) { ... }  — metadata + scope
//   2. edit { } / generate { } / scan<A>(initial) { }.edit { }.generate { }
//   3. kotlin { } / java { } / yaml { } / ... { visitX { node -> ... } } — per-language visitor builders
//
// The imperative path (`edit { kotlin { visitX { ... } } }`) compiles and runs
// pure-JVM without the rewrite-kotlin K2 compiler plugin. The `rewrite { } to { }`
// declarative path REQUIRES the plugin: the MethodMatcher spec is synthesized at
// FIR time from the resolved before-lambda symbol. Without the plugin, `rewrite`
// is an `error(...)` stub.

/** Marker that pins receiver scopes inside the recipe DSL. */
@DslMarker
public annotation class RecipeDsl

/**
 * Top-level entry point for a recipe declaration. The trailing lambda runs once
 * at recipe-construction time, populating a [RecipeBuilder] with the captured
 * phase blocks (`edit`/`scan`/`generate`). The returned [Recipe] re-evaluates
 * those blocks on each lifecycle call (`getVisitor()`, `getScanner(acc)`, etc.)
 * to produce fresh visitors per cycle — matching the framework's expectation
 * that visitors carry per-run state via cursor.
 *
 * `estimatedEffortPerOccurrence` is a [java.time.Duration]; `null` (the default)
 * leaves the framework's Recipe-level default in place.
 */
public fun recipe(
    displayName: String,
    description: String,
    tags: Set<String> = emptySet(),
    estimatedEffortPerOccurrence: Duration? = null,
    block: RecipeBuilder.() -> Unit,
): Recipe {
    val builder = RecipeBuilder().apply(block)
    return builder.build(displayName, description, tags, estimatedEffortPerOccurrence)
}

/**
 * Group a fixed list of recipes under a single named composite. The returned
 * [Recipe] has no edit phase of its own — its only effect is to run each
 * element of [recipes] in order via [Recipe.getRecipeList]. Useful for
 * assembling several fine-grained recipes into a higher-level migration
 * target.
 *
 * Unlike [recipe], this builder requires no K2 compiler-plugin support — the
 * returned [Recipe] is a plain JVM class ([KotlinCompositeRecipe]) and
 * round-trips cleanly through the standard recipe serializer.
 */
public fun recipes(
    displayName: String,
    description: String,
    vararg recipes: Recipe,
): Recipe = KotlinCompositeRecipe(displayName, description, recipes.toList())

/**
 * Backing class for [recipes]. Holds a fixed display name, description, and
 * recipe list, exposing them through the [Recipe] API. Public so Jackson can
 * round-trip it via the standard recipe serializer (`@c` polymorphic type tag
 * + property-based constructor detection).
 *
 * The constructor uses `@JsonCreator` + `@JsonProperty` directly so the class
 * deserializes without the optional `jackson-module-kotlin` runtime — only the
 * always-present `ParameterNamesModule` is required. Parameters are nullable
 * because [org.openrewrite.internal.RecipeLoader] validates instantiability by
 * calling the constructor with empty arguments; the public [recipes] entry
 * point always passes real, non-null values.
 *
 * Marked [AbstractRecipe] so the classpath scanner doesn't enumerate it as a
 * self-standing recipe — its only meaningful uses are concrete deserialized
 * instances and (eventually) compiler-synthesized `<Name>$KtRecipe` subclasses.
 */
@AbstractRecipe
public class KotlinCompositeRecipe @JsonCreator constructor(
    @JsonProperty("displayName") private val displayName: String?,
    @JsonProperty("description") private val description: String?,
    @JsonProperty("recipeList") private val recipeList: List<Recipe>?,
) : Recipe() {
    init {
        // Refuse the no-arg Jackson construction that ClasspathScanningLoader
        // uses to probe instantiability. The [AbstractRecipe] annotation is the
        // canonical filter, but older scanners (pre-8.83.0, or any version where
        // recipe-JAR classloader delegation routes the annotation type away from
        // the scanner's own classloader) can't see it. Throwing here trips the
        // scanner's catch-Throwable in `configureRecipe`, so the class is silently
        // skipped rather than enumerated as a phantom empty composite.
        //
        // FIXME: remove this `require` once 8.83.0+ (which parent-delegates
        //  `org.openrewrite.AbstractRecipe` in `RecipeClassLoader`) is the
        //  minimum supported version of build-plugin/CLI scanners in the wild.
        //  At that point the annotation alone is sufficient.
        require(displayName != null) { "KotlinCompositeRecipe requires a non-null displayName" }
    }

    override fun getDisplayName(): String = displayName!!
    override fun getDescription(): String = description ?: "A composite recipe."
    override fun getRecipeList(): List<Recipe> = recipeList ?: emptyList()
}

/**
 * Receiver scope of the [recipe] lambda. Carries the captured phase blocks
 * (`edit`/`scan`/`generate`) and produces a [Recipe] or [ScanningRecipe] from
 * them on [build].
 */
@RecipeDsl
public class RecipeBuilder internal constructor() {

    private var editBlock: (EditScope.() -> Unit)? = null
    private var generateBlock: (GenerateScope.() -> Collection<SourceFile>)? = null
    private var scanBuilder: ScanBuilder<*>? = null

    /** Bare edit phase, no accumulator. */
    public fun edit(block: EditScope.() -> Unit) {
        require(editBlock == null) { "recipe { } already declares an edit block" }
        require(scanBuilder == null) { "use scan<A>(initial) { }.edit { } when a scan accumulator is in play" }
        editBlock = block
    }

    /**
     * Bare generate phase, no accumulator. Rare — generation without scan.
     * The block's last expression is a `Collection<SourceFile>` to emit.
     */
    public fun generate(block: GenerateScope.() -> Collection<SourceFile>) {
        require(generateBlock == null) { "recipe { } already declares a generate block" }
        require(scanBuilder == null) { "use scan<A>(initial) { }.generate { } when a scan accumulator is in play" }
        generateBlock = block
    }

    /**
     * Scan phase that produces an accumulator of type [A]. Chain with `.edit { }`
     * and/or `.generate { }` to consume the accumulator. A bare `scan` with no
     * chained consumer would compute and discard the accumulator — Phase 3's FIR
     * checker rejects that shape at compile time; at runtime the scan still runs
     * but contributes no visitor.
     *
     * The accumulator [A] is passed as a lambda parameter to scan / edit /
     * generate blocks — `scan<A>(initial) { acc -> … }.edit { acc -> … }`. The
     * DSL is agnostic to mutability: authors picking a mutable container type
     * (e.g. `MutableList<String>`) mutate `acc` in place; authors picking a
     * value type wrap in `AtomicReference<A>` themselves. Matches the Java
     * [ScanningRecipe<A>] convention — `A` is whatever fits the recipe.
     */
    public fun <A : Any> scan(initial: A, block: ScanScope.(A) -> Unit): Scan<A> {
        require(scanBuilder == null) { "recipe { } already declares a scan block (single-scan v1)" }
        require(editBlock == null && generateBlock == null) {
            "place scan { } before edit { } / generate { } (chain with scan { }.edit { })"
        }
        val sb = ScanBuilder(initial, block)
        scanBuilder = sb
        return Scan(sb)
    }

    internal fun build(
        displayName: String,
        description: String,
        recipeTags: Set<String>,
        effort: Duration?,
    ): Recipe {
        val scan = scanBuilder
        if (scan != null) {
            return buildScanningRecipe(scan, displayName, description, recipeTags, effort)
        }
        return buildSimpleRecipe(displayName, description, recipeTags, effort)
    }

    private fun buildSimpleRecipe(
        displayName: String,
        description: String,
        recipeTags: Set<String>,
        effort: Duration?,
    ): Recipe {
        val capturedEdit = editBlock
        val capturedGen = generateBlock
        // A bare generate { } without scan is a SourceFile generator that runs
        // outside the scan accumulator lifecycle. Without scan the framework
        // has no natural lifecycle hook for generation; we synthesize a
        // ScanningRecipe<Unit> whose getInitialValue returns Unit and whose
        // generate runs the captured block.
        if (capturedGen != null && capturedEdit == null) {
            return buildBareGenerateRecipe(displayName, description, recipeTags, effort, capturedGen)
        }
        return object : Recipe() {
            override fun getDisplayName(): String = displayName
            override fun getDescription(): String = description
            override fun getTags(): Set<String> = recipeTags
            override fun getEstimatedEffortPerOccurrence(): Duration? = effort
            override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
                val edit = capturedEdit ?: return TreeVisitor.noop<Tree, ExecutionContext>()
                val scope = EditScope().apply(edit)
                return scope.buildVisitor()
            }
        }
    }

    private fun <A : Any> buildScanningRecipe(
        scan: ScanBuilder<A>,
        displayName: String,
        description: String,
        recipeTags: Set<String>,
        effort: Duration?,
    ): Recipe {
        return object : ScanningRecipe<A>() {
            override fun getDisplayName(): String = displayName
            override fun getDescription(): String = description
            override fun getTags(): Set<String> = recipeTags
            override fun getEstimatedEffortPerOccurrence(): Duration? = effort

            override fun getInitialValue(ctx: ExecutionContext): A = scan.initial

            override fun getScanner(acc: A): TreeVisitor<*, ExecutionContext> {
                val scope = ScanScope()
                scan.scanBlock(scope, acc)
                return scope.buildVisitor()
            }

            override fun getVisitor(acc: A): TreeVisitor<*, ExecutionContext> {
                val editBlock = scan.editBlock ?: return TreeVisitor.noop<Tree, ExecutionContext>()
                val scope = EditScope()
                editBlock(scope, acc)
                return scope.buildVisitor()
            }

            override fun generate(
                acc: A,
                generatedInThisCycle: MutableCollection<SourceFile>,
                ctx: ExecutionContext,
            ): MutableCollection<SourceFile> {
                val genBlock = scan.generateBlock ?: return mutableListOf()
                val scope = GenerateScope(ctx)
                val produced = genBlock(scope, acc).toMutableList()
                // 3-arg form: filter against already-generated files for cycle-2 stability.
                val alreadyByPath = generatedInThisCycle.mapNotNull { it.sourcePath?.toString() }.toSet()
                produced.removeAll { (it.sourcePath?.toString() ?: "") in alreadyByPath }
                return produced
            }
        }
    }

    private fun buildBareGenerateRecipe(
        displayName: String,
        description: String,
        recipeTags: Set<String>,
        effort: Duration?,
        genBlock: GenerateScope.() -> Collection<SourceFile>,
    ): Recipe {
        return object : ScanningRecipe<Unit>() {
            override fun getDisplayName(): String = displayName
            override fun getDescription(): String = description
            override fun getTags(): Set<String> = recipeTags
            override fun getEstimatedEffortPerOccurrence(): Duration? = effort
            override fun getInitialValue(ctx: ExecutionContext) = Unit
            override fun getScanner(acc: Unit): TreeVisitor<*, ExecutionContext> = TreeVisitor.noop<Tree, ExecutionContext>()
            override fun generate(
                acc: Unit,
                generatedInThisCycle: MutableCollection<SourceFile>,
                ctx: ExecutionContext,
            ): MutableCollection<SourceFile> {
                val scope = GenerateScope(ctx)
                val produced = scope.genBlock().toMutableList()
                val alreadyByPath = generatedInThisCycle.mapNotNull { it.sourcePath?.toString() }.toSet()
                produced.removeAll { (it.sourcePath?.toString() ?: "") in alreadyByPath }
                return produced
            }
        }
    }
}

/** Internal carrier for a scan phase's captured blocks. */
internal class ScanBuilder<A : Any>(
    val initial: A,
    val scanBlock: ScanScope.(A) -> Unit,
) {
    var editBlock: (EditScope.(A) -> Unit)? = null
    var generateBlock: (GenerateScope.(A) -> Collection<SourceFile>)? = null
}

/** Chain builder returned by [RecipeBuilder.scan]. */
public class Scan<A : Any> internal constructor(internal val builder: ScanBuilder<A>) {
    /** Consume the scan accumulator with an edit phase. */
    public fun edit(block: EditScope.(A) -> Unit) {
        require(builder.editBlock == null) { "scan { }.edit { } already declared" }
        builder.editBlock = block
    }

    /**
     * Consume the scan accumulator with a generate phase. Returns this same
     * [Scan] so a trailing `.edit { }` can follow.
     */
    public fun generate(block: GenerateScope.(A) -> Collection<SourceFile>): Scan<A> {
        require(builder.generateBlock == null) { "scan { }.generate { } already declared" }
        builder.generateBlock = block
        return this
    }
}

// ---------------------------------------------------------------------------
// Scope receivers
// ---------------------------------------------------------------------------

/**
 * Shared base for [EditScope] and [ScanScope]: holds the registered visitors
 * and exposes one language-scope factory per supported language. The 15 factory
 * methods (`kotlin`, `java`, `yaml`, ...) auto-register their built visitor
 * with the enclosing scope AND return it, so authors can also pass them to
 * [check] / [and] / [or] / [not] as ordinary visitor expressions.
 */
@RecipeDsl
public abstract class LanguageHost internal constructor() {

    internal val visitors: MutableList<TreeVisitor<*, ExecutionContext>> = mutableListOf()

    /**
     * Build the composed visitor for this scope. Single-visitor scopes return
     * that visitor directly; multi-visitor scopes wrap via
     * [GeneratedRecipeSupport.composeSequential].
     */
    internal fun buildVisitor(): TreeVisitor<*, ExecutionContext> = when (visitors.size) {
        0 -> TreeVisitor.noop<Tree, ExecutionContext>()
        1 -> visitors[0]
        else -> GeneratedRecipeSupport.composeSequential(visitors.toTypedArray())
    }

    internal fun register(visitor: TreeVisitor<*, ExecutionContext>): TreeVisitor<*, ExecutionContext> {
        visitors.add(visitor)
        return visitor
    }

    /**
     * Take a visitor that was just auto-registered by a sibling factory call
     * and remove it from the visitor list. Used by precondition wrappers
     * ([check]) to "claim" the inner visitor before wrapping it, so the
     * inner visitor doesn't run twice.
     */
    internal fun unregister(visitor: TreeVisitor<*, ExecutionContext>): TreeVisitor<*, ExecutionContext> {
        visitors.remove(visitor)
        return visitor
    }

    // === Language scope factories ===
    //
    // Each one builds a fresh `<Lang>Scope`, runs the user's block on it, then
    // emits the resulting language visitor. The visitor is auto-registered with
    // this enclosing scope (so it contributes to the recipe's visitor pipeline)
    // AND returned (so authors can compose it with [check]).
    //
    // Missing-language dep behavior: when a language module is not on the
    // runtime classpath, the JVM raises `NoClassDefFoundError` when first
    // resolving `<Lang>Visitor` here. Phase 2 accepts that native error; a
    // friendlier pre-flight check is an open implementation note (plan §Phase 2).

    public fun kotlin(block: org.openrewrite.dsl.scopes.KotlinScope.() -> Unit): TreeVisitor<*, ExecutionContext> =
        register(org.openrewrite.dsl.scopes.KotlinScope().apply(block).build())

    public fun java(block: org.openrewrite.dsl.scopes.JavaScope.() -> Unit): TreeVisitor<*, ExecutionContext> =
        register(org.openrewrite.dsl.scopes.JavaScope().apply(block).build())

    public fun yaml(block: org.openrewrite.dsl.scopes.YamlScope.() -> Unit): TreeVisitor<*, ExecutionContext> =
        register(org.openrewrite.dsl.scopes.YamlScope().apply(block).build())

    public fun xml(block: org.openrewrite.dsl.scopes.XmlScope.() -> Unit): TreeVisitor<*, ExecutionContext> =
        register(org.openrewrite.dsl.scopes.XmlScope().apply(block).build())

    public fun maven(block: org.openrewrite.dsl.scopes.MavenScope.() -> Unit): TreeVisitor<*, ExecutionContext> =
        register(org.openrewrite.dsl.scopes.MavenScope().apply(block).build())

    public fun json(block: org.openrewrite.dsl.scopes.JsonScope.() -> Unit): TreeVisitor<*, ExecutionContext> =
        register(org.openrewrite.dsl.scopes.JsonScope().apply(block).build())

    public fun properties(block: org.openrewrite.dsl.scopes.PropertiesScope.() -> Unit): TreeVisitor<*, ExecutionContext> =
        register(org.openrewrite.dsl.scopes.PropertiesScope().apply(block).build())

    public fun toml(block: org.openrewrite.dsl.scopes.TomlScope.() -> Unit): TreeVisitor<*, ExecutionContext> =
        register(org.openrewrite.dsl.scopes.TomlScope().apply(block).build())

    public fun hcl(block: org.openrewrite.dsl.scopes.HclScope.() -> Unit): TreeVisitor<*, ExecutionContext> =
        register(org.openrewrite.dsl.scopes.HclScope().apply(block).build())

    // `gradle { … }` is intentionally omitted: rewrite-gradle depends on
    // rewrite-kotlin (Gradle .kts scripts are parsed via the Kotlin LST), so
    // adding rewrite-gradle as a compileOnly dep here would create a project
    // cycle. Authors targeting Gradle DSL use `groovy { … }` for .gradle and
    // `kotlin { … }` for .gradle.kts; a dedicated GradleScope can land once
    // rewrite-gradle is split into a leaf module.

    public fun groovy(block: org.openrewrite.dsl.scopes.GroovyScope.() -> Unit): TreeVisitor<*, ExecutionContext> =
        register(org.openrewrite.dsl.scopes.GroovyScope().apply(block).build())

    public fun python(block: org.openrewrite.dsl.scopes.PythonScope.() -> Unit): TreeVisitor<*, ExecutionContext> =
        register(org.openrewrite.dsl.scopes.PythonScope().apply(block).build())

    public fun csharp(block: org.openrewrite.dsl.scopes.CSharpScope.() -> Unit): TreeVisitor<*, ExecutionContext> =
        register(org.openrewrite.dsl.scopes.CSharpScope().apply(block).build())

    public fun scala(block: org.openrewrite.dsl.scopes.ScalaScope.() -> Unit): TreeVisitor<*, ExecutionContext> =
        register(org.openrewrite.dsl.scopes.ScalaScope().apply(block).build())

    public fun javascript(block: org.openrewrite.dsl.scopes.JavaScriptScope.() -> Unit): TreeVisitor<*, ExecutionContext> =
        register(org.openrewrite.dsl.scopes.JavaScriptScope().apply(block).build())
}

/**
 * Receiver for the `edit { }` phase block. Hosts language scopes, precondition
 * wrappers, the phase-level `uses<T>() / usesMethod / usesField / usesJavaVersion`
 * helpers, and the declarative `rewrite { } to { }` surface.
 */
@RecipeDsl
public open class EditScope internal constructor() : LanguageHost() {

    // === Precondition wrappers ===
    //
    // `check(condition, visitor)` "claims" the visitor from the auto-registered
    // list, wraps it with the precondition, and registers the wrapper instead.
    // This lets authors write `check(uses<T>(), kotlin { ... })` without the
    // inner kotlin visitor running twice.

    public fun check(condition: TreeVisitor<*, ExecutionContext>?, visitor: TreeVisitor<*, ExecutionContext>): TreeVisitor<*, ExecutionContext> {
        unregister(visitor)
        return register(Preconditions.check(condition, visitor))
    }

    public fun check(condition: Recipe, visitor: TreeVisitor<*, ExecutionContext>): TreeVisitor<*, ExecutionContext> {
        unregister(visitor)
        return register(Preconditions.check(condition, visitor))
    }

    public fun check(condition: Boolean, visitor: TreeVisitor<*, ExecutionContext>): TreeVisitor<*, ExecutionContext> {
        unregister(visitor)
        return register(Preconditions.check(condition, visitor))
    }

    public fun and(vararg vs: TreeVisitor<*, ExecutionContext>): TreeVisitor<*, ExecutionContext> =
        Preconditions.and(*vs)

    public fun or(vararg vs: TreeVisitor<*, ExecutionContext>): TreeVisitor<*, ExecutionContext> =
        Preconditions.or(*vs)

    public fun not(v: TreeVisitor<*, ExecutionContext>): TreeVisitor<*, ExecutionContext> =
        Preconditions.not(v)

    // === Phase-level usage probes ===
    //
    // These return visitors NOT auto-registered with the enclosing scope —
    // they're meant to be consumed by `check(...)` as the precondition arg.

    public inline fun <reified T : Any> uses(): TreeVisitor<*, ExecutionContext> =
        UsesType<ExecutionContext>(T::class.java.name, false)

    public fun usesMethod(spec: String): TreeVisitor<*, ExecutionContext> =
        UsesMethod<ExecutionContext>(spec)

    public fun usesField(owner: String, field: String): TreeVisitor<*, ExecutionContext> =
        UsesField<ExecutionContext>(owner, field)

    public fun usesJavaVersion(min: Int, max: Int = Int.MAX_VALUE): TreeVisitor<*, ExecutionContext> =
        UsesJavaVersion<ExecutionContext>(min, max)

    // === Declarative pattern shape ===
    //
    // `rewrite { } to { }` requires the rewrite-kotlin K2 compiler plugin: the
    // MethodMatcher spec is synthesized at FIR time from the resolved before-
    // lambda symbol. Without the plugin, the surface compiles (it's just a
    // function call) but throws at runtime.

    @Suppress("UNUSED_PARAMETER")
    public fun <R> rewrite(before: () -> R): RewriteAdvice0<R> = pluginRequired()
    @Suppress("UNUSED_PARAMETER")
    public fun <R> rewrite(first: () -> R, vararg rest: () -> R): RewriteAdvice0<R> = pluginRequired()

    @Suppress("UNUSED_PARAMETER")
    public fun <P, R> rewrite(before: (P) -> R): RewriteAdvice1<P, R> = pluginRequired()
    @Suppress("UNUSED_PARAMETER")
    public fun <P, R> rewrite(first: (P) -> R, vararg rest: (P) -> R): RewriteAdvice1<P, R> = pluginRequired()

    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, R> rewrite(before: (P1, P2) -> R): RewriteAdvice2<P1, P2, R> = pluginRequired()
    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, R> rewrite(first: (P1, P2) -> R, vararg rest: (P1, P2) -> R): RewriteAdvice2<P1, P2, R> = pluginRequired()

    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, R> rewrite(before: (P1, P2, P3) -> R): RewriteAdvice3<P1, P2, P3, R> = pluginRequired()
    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, R> rewrite(first: (P1, P2, P3) -> R, vararg rest: (P1, P2, P3) -> R): RewriteAdvice3<P1, P2, P3, R> = pluginRequired()

    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, P4, R> rewrite(before: (P1, P2, P3, P4) -> R): RewriteAdvice4<P1, P2, P3, P4, R> = pluginRequired()
    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, P4, R> rewrite(first: (P1, P2, P3, P4) -> R, vararg rest: (P1, P2, P3, P4) -> R): RewriteAdvice4<P1, P2, P3, P4, R> = pluginRequired()

    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, P4, P5, R> rewrite(before: (P1, P2, P3, P4, P5) -> R): RewriteAdvice5<P1, P2, P3, P4, P5, R> = pluginRequired()
    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, P4, P5, R> rewrite(first: (P1, P2, P3, P4, P5) -> R, vararg rest: (P1, P2, P3, P4, P5) -> R): RewriteAdvice5<P1, P2, P3, P4, P5, R> = pluginRequired()

    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, P4, P5, P6, R> rewrite(before: (P1, P2, P3, P4, P5, P6) -> R): RewriteAdvice6<P1, P2, P3, P4, P5, P6, R> = pluginRequired()
    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, P4, P5, P6, R> rewrite(first: (P1, P2, P3, P4, P5, P6) -> R, vararg rest: (P1, P2, P3, P4, P5, P6) -> R): RewriteAdvice6<P1, P2, P3, P4, P5, P6, R> = pluginRequired()

    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, P4, P5, P6, P7, R> rewrite(before: (P1, P2, P3, P4, P5, P6, P7) -> R): RewriteAdvice7<P1, P2, P3, P4, P5, P6, P7, R> = pluginRequired()
    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, P4, P5, P6, P7, R> rewrite(first: (P1, P2, P3, P4, P5, P6, P7) -> R, vararg rest: (P1, P2, P3, P4, P5, P6, P7) -> R): RewriteAdvice7<P1, P2, P3, P4, P5, P6, P7, R> = pluginRequired()

    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, P4, P5, P6, P7, P8, R> rewrite(before: (P1, P2, P3, P4, P5, P6, P7, P8) -> R): RewriteAdvice8<P1, P2, P3, P4, P5, P6, P7, P8, R> = pluginRequired()
    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, P4, P5, P6, P7, P8, R> rewrite(first: (P1, P2, P3, P4, P5, P6, P7, P8) -> R, vararg rest: (P1, P2, P3, P4, P5, P6, P7, P8) -> R): RewriteAdvice8<P1, P2, P3, P4, P5, P6, P7, P8, R> = pluginRequired()

    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R> rewrite(before: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R): RewriteAdvice9<P1, P2, P3, P4, P5, P6, P7, P8, P9, R> = pluginRequired()
    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R> rewrite(first: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R, vararg rest: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R): RewriteAdvice9<P1, P2, P3, P4, P5, P6, P7, P8, P9, R> = pluginRequired()

    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> rewrite(before: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R): RewriteAdvice10<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> = pluginRequired()
    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> rewrite(first: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R, vararg rest: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R): RewriteAdvice10<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> = pluginRequired()

    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R> rewrite(before: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R): RewriteAdvice11<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R> = pluginRequired()
    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R> rewrite(first: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R, vararg rest: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R): RewriteAdvice11<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R> = pluginRequired()

    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R> rewrite(before: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R): RewriteAdvice12<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R> = pluginRequired()
    @Suppress("UNUSED_PARAMETER")
    public fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R> rewrite(first: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R, vararg rest: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R): RewriteAdvice12<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R> = pluginRequired()

    private fun <T> pluginRequired(): T = error(
        "Recipe DSL: `rewrite { } to { }` requires the rewrite-kotlin K2 compiler plugin. " +
            "Add rewrite-kotlin to `kotlinCompilerPluginClasspath` (plugin id `org.openrewrite.kotlin.recipe`)."
    )
}

/**
 * Receiver for the `scan { }` phase block. Same language-scope surface as
 * [EditScope] but no preconditions / no rewrite{}to{} — scanners only observe.
 * The accumulator is passed to the block as a lambda parameter, not exposed
 * on the receiver.
 */
@RecipeDsl
public open class ScanScope internal constructor() : LanguageHost()

/** Receiver for the `generate { }` block; access [ctx] only. */
@RecipeDsl
public open class GenerateScope internal constructor(public val ctx: ExecutionContext)

// ---------------------------------------------------------------------------
// rewrite { } to { } type stubs — populated at IR time by the K2 plugin.
// ---------------------------------------------------------------------------

/**
 * The result of a `rewrite { } to { }` clause. Exists so the optional
 * `.strictArity()` opt-out can chain off it: by default a clause whose before
 * targets a varargs method matches call sites with any number of trailing
 * arguments (variadic-by-default); `(rewrite { } to { }).strictArity()` pins it
 * to the exact arity the author wrote. Like the rest of the declarative shape
 * this is an `error(...)` stub — the K2 plugin rewrites the whole clause at IR
 * time and this body never runs.
 */
@RecipeDsl public class RewriteRule internal constructor() {
    @Suppress("UNUSED_PARAMETER")
    public fun strictArity(): Unit = error(
        "Recipe DSL: `rewrite { } to { }` requires the rewrite-kotlin K2 compiler plugin.",
    )
}

@RecipeDsl public class RewriteAdvice0<R> internal constructor() {
    @Suppress("UNUSED_PARAMETER")
    public infix fun <R2> to(after: () -> R2): RewriteRule = error(
        "Recipe DSL: `rewrite { } to { }` requires the rewrite-kotlin K2 compiler plugin.",
    )
}

@RecipeDsl public class RewriteAdvice1<P, R> internal constructor() {
    @Suppress("UNUSED_PARAMETER")
    public infix fun <R2> to(after: (P) -> R2): RewriteRule = error(
        "Recipe DSL: `rewrite { } to { }` requires the rewrite-kotlin K2 compiler plugin.",
    )
}

@RecipeDsl public class RewriteAdvice2<P1, P2, R> internal constructor() {
    @Suppress("UNUSED_PARAMETER")
    public infix fun <R2> to(after: (P1, P2) -> R2): RewriteRule = error(
        "Recipe DSL: `rewrite { } to { }` requires the rewrite-kotlin K2 compiler plugin.",
    )
}

@RecipeDsl public class RewriteAdvice3<P1, P2, P3, R> internal constructor() {
    @Suppress("UNUSED_PARAMETER")
    public infix fun <R2> to(after: (P1, P2, P3) -> R2): RewriteRule = error(
        "Recipe DSL: `rewrite { } to { }` requires the rewrite-kotlin K2 compiler plugin.",
    )
}

@RecipeDsl public class RewriteAdvice4<P1, P2, P3, P4, R> internal constructor() {
    @Suppress("UNUSED_PARAMETER")
    public infix fun <R2> to(after: (P1, P2, P3, P4) -> R2): RewriteRule = error(
        "Recipe DSL: `rewrite { } to { }` requires the rewrite-kotlin K2 compiler plugin.",
    )
}

@RecipeDsl public class RewriteAdvice5<P1, P2, P3, P4, P5, R> internal constructor() {
    @Suppress("UNUSED_PARAMETER")
    public infix fun <R2> to(after: (P1, P2, P3, P4, P5) -> R2): RewriteRule = error(
        "Recipe DSL: `rewrite { } to { }` requires the rewrite-kotlin K2 compiler plugin.",
    )
}

@RecipeDsl public class RewriteAdvice6<P1, P2, P3, P4, P5, P6, R> internal constructor() {
    @Suppress("UNUSED_PARAMETER")
    public infix fun <R2> to(after: (P1, P2, P3, P4, P5, P6) -> R2): RewriteRule = error(
        "Recipe DSL: `rewrite { } to { }` requires the rewrite-kotlin K2 compiler plugin.",
    )
}

@RecipeDsl public class RewriteAdvice7<P1, P2, P3, P4, P5, P6, P7, R> internal constructor() {
    @Suppress("UNUSED_PARAMETER")
    public infix fun <R2> to(after: (P1, P2, P3, P4, P5, P6, P7) -> R2): RewriteRule = error(
        "Recipe DSL: `rewrite { } to { }` requires the rewrite-kotlin K2 compiler plugin.",
    )
}

@RecipeDsl public class RewriteAdvice8<P1, P2, P3, P4, P5, P6, P7, P8, R> internal constructor() {
    @Suppress("UNUSED_PARAMETER")
    public infix fun <R2> to(after: (P1, P2, P3, P4, P5, P6, P7, P8) -> R2): RewriteRule = error(
        "Recipe DSL: `rewrite { } to { }` requires the rewrite-kotlin K2 compiler plugin.",
    )
}

@RecipeDsl public class RewriteAdvice9<P1, P2, P3, P4, P5, P6, P7, P8, P9, R> internal constructor() {
    @Suppress("UNUSED_PARAMETER")
    public infix fun <R2> to(after: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R2): RewriteRule = error(
        "Recipe DSL: `rewrite { } to { }` requires the rewrite-kotlin K2 compiler plugin.",
    )
}

@RecipeDsl public class RewriteAdvice10<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> internal constructor() {
    @Suppress("UNUSED_PARAMETER")
    public infix fun <R2> to(after: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R2): RewriteRule = error(
        "Recipe DSL: `rewrite { } to { }` requires the rewrite-kotlin K2 compiler plugin.",
    )
}

@RecipeDsl public class RewriteAdvice11<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R> internal constructor() {
    @Suppress("UNUSED_PARAMETER")
    public infix fun <R2> to(after: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R2): RewriteRule = error(
        "Recipe DSL: `rewrite { } to { }` requires the rewrite-kotlin K2 compiler plugin.",
    )
}

@RecipeDsl public class RewriteAdvice12<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R> internal constructor() {
    @Suppress("UNUSED_PARAMETER")
    public infix fun <R2> to(after: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R2): RewriteRule = error(
        "Recipe DSL: `rewrite { } to { }` requires the rewrite-kotlin K2 compiler plugin.",
    )
}

// ---------------------------------------------------------------------------
// Runtime support for the K2 plugin's imperative-recipe synthesis.
// ---------------------------------------------------------------------------

/**
 * Rebuild the visitor pipeline for an imperative
 * `recipe(...) { edit { lang { visitX { ... } } } }` recipe on each call.
 *
 * The K2 plugin replaces the original `recipe(...)` call site with a
 * synthetic `<Name>$KtRecipe()` constructor; the generated class is a
 * field-less top-level `Recipe` whose `getVisitor()` body calls this
 * helper, threading the user's original trailing lambda back through a
 * fresh [RecipeBuilder]. Because the generated class has no instance
 * state, Jackson roundtrip (`RecipeSerializer.read(write(r))`) succeeds —
 * the workaround `validateRecipeSerialization(false)` is no longer needed
 * for imperative recipes that route through this path.
 *
 * The temporary [Recipe] built here exists only long enough to extract
 * its visitor; metadata (displayName, description, tags, effort) is the
 * generated class's responsibility (it overrides the corresponding Recipe
 * methods with constants), so passing empty placeholders is intentional.
 */
public fun buildImperativeVisitor(
    block: RecipeBuilder.() -> Unit,
): TreeVisitor<*, ExecutionContext> {
    val builder = RecipeBuilder()
    builder.block()
    return builder.build("", "", emptySet(), null).getVisitor()
}

private fun buildImperativeRecipe(block: RecipeBuilder.() -> Unit): Recipe {
    val builder = RecipeBuilder()
    builder.block()
    return builder.build("", "", emptySet(), null)
}

/**
 * Scanning counterparts to [buildImperativeVisitor], one per [ScanningRecipe]
 * lifecycle method. The K2 plugin's synthesized `<Name>$KtRecipe` extends
 * [ScanningRecipe] and routes each call here so all phases survive synthesis.
 * The accumulator is threaded by the framework, so the generated class stays
 * field-less; an edit-only block leaves [buildImperativeRecipe] returning a
 * plain [Recipe], for which the scanner/generate helpers no-op.
 */
public fun buildImperativeInitialValue(
    ctx: ExecutionContext,
    block: RecipeBuilder.() -> Unit,
): Any {
    val recipe = buildImperativeRecipe(block)
    @Suppress("UNCHECKED_CAST")
    return if (recipe is ScanningRecipe<*>) (recipe as ScanningRecipe<Any>).getInitialValue(ctx) else Unit
}

public fun buildImperativeScanner(
    acc: Any,
    block: RecipeBuilder.() -> Unit,
): TreeVisitor<*, ExecutionContext> {
    val recipe = buildImperativeRecipe(block)
    @Suppress("UNCHECKED_CAST")
    return if (recipe is ScanningRecipe<*>) (recipe as ScanningRecipe<Any>).getScanner(acc)
    else TreeVisitor.noop<Tree, ExecutionContext>()
}

public fun buildImperativeEditVisitor(
    acc: Any,
    block: RecipeBuilder.() -> Unit,
): TreeVisitor<*, ExecutionContext> {
    val recipe = buildImperativeRecipe(block)
    @Suppress("UNCHECKED_CAST")
    return if (recipe is ScanningRecipe<*>) (recipe as ScanningRecipe<Any>).getVisitor(acc)
    else recipe.getVisitor()
}

public fun buildImperativeGenerate(
    acc: Any,
    generatedInThisCycle: Collection<SourceFile>,
    ctx: ExecutionContext,
    block: RecipeBuilder.() -> Unit,
): Collection<SourceFile> {
    val recipe = buildImperativeRecipe(block)
    @Suppress("UNCHECKED_CAST")
    return if (recipe is ScanningRecipe<*>) {
        (recipe as ScanningRecipe<Any>).generate(acc, generatedInThisCycle, ctx).toList()
    } else {
        emptyList()
    }
}
