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
@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)
package org.openrewrite.kotlin.recipe

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Preconditions
import org.openrewrite.Recipe
import org.openrewrite.java.search.UsesField
import org.openrewrite.java.search.UsesMethod
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.TypeValidation
import org.openrewrite.kotlin.Assertions.kotlin
import org.openrewrite.java.Assertions.java

/**
 * End-to-end recipe execution for the canonical Phase 3 shape:
 *
 *     val UseFoo = recipe("d", "desc") {
 *         edit { rewrite { p -> p.foo() } to { p -> p.bar() } }
 *     }
 *
 * Each test compiles a recipe via the K2 plugin, instantiates the synthesized
 * `<Name>$KtRecipe` class, then runs it through `RewriteTest` against a
 * Kotlin source fixture. This proves the full IR-pass pipeline — metadata
 * extraction, MethodMatcher spec computation, after-template synthesis,
 * Java-vs-Kotlin classifier, helper dispatch — works against real recipes.
 *
 * The pure-runtime DSL surface (no plugin) is covered separately by
 * `RecipeDslSurfaceTest` in `org.openrewrite`.
 */
class RecipePluginRewriteTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseUppercase = recipe(
                    displayName = "Replace lowercase with uppercase",
                    description = "..."
                ) {
                    edit {
                        rewrite { s: String -> s.lowercase() } to { s -> s.uppercase() }
                    }
                }
            """.trimIndent(),
            propertyName = "UseUppercase",
        ))
        // The synthesized `<Name>$KtRecipe` class lives in the
        // kotlin-compile-testing classloader, not the test's. Jackson's
        // class-id deserializer can't resolve it. The recipe still executes
        // correctly via direct invocation; the round-trip is what fails.
        spec.validateRecipeSerialization(false)
    }

    @Test
    fun `member-call rewrite — lowercase to uppercase`() = rewriteRun(
        kotlin(
            """
            fun example() {
                val s = "hello"
                println(s.lowercase())
            }
            """.trimIndent(),
            """
            fun example() {
                val s = "hello"
                println(s.uppercase())
            }
            """.trimIndent(),
        ),
    )

    @Test
    fun `not-null-asserted before pattern rewrites the whole expression`() {
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseReadln = recipe(
                    displayName = "Use readln() instead of readLine()!!",
                    description = "..."
                ) {
                    edit {
                        rewrite { -> readLine()!! } to { -> readln() }
                    }
                }
            """.trimIndent(),
            propertyName = "UseReadln",
        )
        rewriteRun(
            { spec -> spec.recipe(r) },
            kotlin(
                """
                fun first(): String = readLine()!!
                """.trimIndent(),
                """
                fun first(): String = readln()
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `after-template preserves source-level FQN qualifier`() {
        // `Math.abs(x: Double)` -> `kotlin.math.abs(x)`. K2 IR resolves
        // `kotlin.math.abs(x)` to a single `IrCall(abs)` whose offsets cover
        // only `abs(x)`; the package qualifier has no IR node. The IR pass
        // extends the source slice backward through any `id(.id)*` chain so
        // the synthesized template emits the same FQN the recipe author wrote,
        // keeping the rewrite single-cycle stable without needing an
        // import-add post-visit.
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseKotlinMathAbs = recipe(
                    displayName = "Use kotlin.math.abs",
                    description = "..."
                ) {
                    edit {
                        rewrite { x: Double -> Math.abs(x) } to { x -> kotlin.math.abs(x) }
                    }
                }
            """.trimIndent(),
            propertyName = "UseKotlinMathAbs",
        )
        rewriteRun(
            { spec -> spec.recipe(r) },
            kotlin(
                """
                fun a(x: Double): Double = Math.abs(x)
                """.trimIndent(),
                """
                fun a(x: Double): Double = kotlin.math.abs(x)
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `property-access rewrite — old getter to new getter`() {
        // Defines a stub class in the recipe source alongside the recipe so
        // the BEFORE lambda's `f.oldProp` resolves at recipe-compile time.
        // The IR pass detects that `oldProp` is a property accessor and
        // routes to `GeneratedRecipeSupport.propertyAccessRewrite`, which
        // walks `J.FieldAccess` instead of `J.MethodInvocation`. Both
        // before and after sides are property reads — the matcher fires on
        // a `J.FieldAccess` whose target type is `Foo` and whose selector
        // is `oldProp`, and the template emits the same shape with
        // `newProp` as the selector name.
        val r = loadCompiledRecipe(
            source = """
                package demo
                import org.openrewrite.recipe
                class Foo {
                    val oldProp: Int get() = 1
                    val newProp: Int get() = 2
                }
                val UseNewProp = recipe(
                    displayName = "Use Foo.newProp",
                    description = "..."
                ) {
                    edit {
                        rewrite { f: Foo -> f.oldProp } to { f -> f.newProp }
                    }
                }
            """.trimIndent(),
            propertyName = "UseNewProp",
            packageName = "demo",
        )
        rewriteRun(
            { spec -> spec.recipe(r) },
            kotlin(
                """
                package demo
                class Foo {
                    val oldProp: Int get() = 1
                    val newProp: Int get() = 2
                }
                fun use(f: Foo): Int = f.oldProp
                """,
                """
                package demo
                class Foo {
                    val oldProp: Int get() = 1
                    val newProp: Int get() = 2
                }
                fun use(f: Foo): Int = f.newProp
                """,
            ),
        )
    }

    @Test
    fun `chain with Java-static inner segment — Optional_of_x_get to x`() {
        // The chain validator must accept an inner segment that is a Java
        // static call (`Optional.of(x)`). The inner has no dispatch receiver
        // (statics carry the class via the symbol's parent IrClass) but does
        // bind the lambda param to its value arg.
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                import java.util.Optional
                val UseValueForOptionalOfGet = recipe(
                    displayName = "Optional.of(x).get() -> x",
                    description = "..."
                ) {
                    edit {
                        rewrite { x: String -> Optional.of(x).get() } to { x -> x }
                    }
                }
            """.trimIndent(),
            propertyName = "UseValueForOptionalOfGet",
        )
        rewriteRun(
            { spec -> spec.recipe(r) },
            kotlin(
                """
                import java.util.Optional
                fun example(): String = Optional.of("hi").get()
                """.trimIndent(),
                """
                import java.util.Optional
                fun example(): String = "hi"
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `chain collapse preserves dot-on-its-own-line layout`() {
        // Real-world Kotlin chains are formatted with each `.member(...)` on its
        // own indented line. The chain-collapse rewrite synthesizes a single
        // fused call (`xs.firstOrNull(p)`), whose select.after defaults to ""
        // — jamming the trailing `.firstOrNull` onto the previous line and
        // clobbering the author's formatting. `preserveSelectAfter` copies the
        // matched outer call's select.after onto the result so the new outer
        // sits on its own dot-prefixed line, matching where the matched outer
        // originally was.
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseFirstOrNullWithPredicate = recipe(
                    displayName = "Use firstOrNull(p) instead of filter(p).firstOrNull()",
                    description = "..."
                ) {
                    edit {
                        rewrite { xs: List<Any>, p: (Any) -> Boolean -> xs.filter(p).firstOrNull() } to { xs, p -> xs.firstOrNull(p) }
                    }
                }
            """.trimIndent(),
            propertyName = "UseFirstOrNullWithPredicate",
        )
        rewriteRun(
            { spec ->
                spec.recipe(r)
                // The synthesized `firstOrNull(p)` outer call doesn't get a
                // resolved JavaType.Method on the template-substituted MethodInvocation;
                // the formatting check is what this test is about, not type
                // attribution.
                spec.typeValidationOptions(org.openrewrite.test.TypeValidation.none())
            },
            kotlin(
                """
                fun first(xs: List<Int>): Int? = xs
                    .filter { it > 0 }
                    .firstOrNull()
                """,
                """
                fun first(xs: List<Int>): Int? = xs
                    .firstOrNull { it > 0 }
                """,
            ),
        )
    }

    @Test
    fun `chain collapse — single-line input stays single-line (no false-positive newline)`() {
        // Negative case for `preserveSelectAfter`: if the matched outer call's
        // select.after has no whitespace (the chain was on one line), we must
        // NOT add any newline to the result. The helper's empty-whitespace
        // guard short-circuits, leaving the result's select.after at the
        // template-substituted default (also empty).
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseFirstOrNullWithPredicate = recipe(
                    displayName = "...",
                    description = "..."
                ) {
                    edit {
                        rewrite { xs: List<Any>, p: (Any) -> Boolean -> xs.filter(p).firstOrNull() } to { xs, p -> xs.firstOrNull(p) }
                    }
                }
            """.trimIndent(),
            propertyName = "UseFirstOrNullWithPredicate",
        )
        rewriteRun(
            { spec ->
                spec.recipe(r)
                spec.typeValidationOptions(org.openrewrite.test.TypeValidation.none())
            },
            kotlin(
                """
                fun first(xs: List<Int>): Int? = xs.filter { it > 0 }.firstOrNull()
                """,
                """
                fun first(xs: List<Int>): Int? = xs.firstOrNull { it > 0 }
                """,
            ),
        )
    }

    @Test
    fun `chain collapse — alternate shape (map then filterNotNull) also preserves layout`() {
        // Second chain-shape variant to prove the fix isn't specific to
        // filter/firstOrNull. The `map { f }.filterNotNull()` collapse is the
        // pattern that surfaced the formatting bug in moderneinc/recipes-kotlin
        // corpus runs.
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseMapNotNull = recipe(
                    displayName = "...",
                    description = "..."
                ) {
                    edit {
                        rewrite { xs: List<Any>, f: (Any) -> Int? -> xs.map(f).filterNotNull() } to { xs, f -> xs.mapNotNull(f) }
                    }
                }
            """.trimIndent(),
            propertyName = "UseMapNotNull",
        )
        rewriteRun(
            { spec ->
                spec.recipe(r)
                spec.typeValidationOptions(org.openrewrite.test.TypeValidation.none())
            },
            kotlin(
                """
                fun nonNull(xs: List<Int>): List<Int> = xs
                    .map { it.takeIf { v -> v > 0 } }
                    .filterNotNull()
                """,
                """
                fun nonNull(xs: List<Int>): List<Int> = xs
                    .mapNotNull { it.takeIf { v -> v > 0 } }
                """,
            ),
        )
    }

    @Test
    fun `chain collapse — three-deep chain preserves inner select layout`() {
        // Real-world case from commercetools/rmf-codegen: the matched chain is
        // three deep — `responses.filter(p1).filter(p2).firstOrNull()`. The
        // recipe `filter(p).firstOrNull() -> firstOrNull(p)` fires on the
        // outermost call. `xs` binds to `responses.filter(p1)` — itself a
        // J.MethodInvocation whose `paddingSelect.after` carries the inner
        // dot-on-newline whitespace. The bug we're guarding against: after
        // substitution, the new outer's `.firstOrNull` line keeps its
        // indentation (preserveSelectAfter handles that), but the SUBSTITUTED
        // inner `.filter` loses its own `paddingSelect.after` and gets
        // jammed up against `responses` (or onto a less-indented line).
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseFirstOrNullWithPredicate = recipe(
                    displayName = "...",
                    description = "..."
                ) {
                    edit {
                        rewrite { xs: List<Any>, p: (Any) -> Boolean -> xs.filter(p).firstOrNull() } to { xs, p -> xs.firstOrNull(p) }
                    }
                }
            """.trimIndent(),
            propertyName = "UseFirstOrNullWithPredicate",
        )
        rewriteRun(
            { spec ->
                spec.recipe(r)
                spec.typeValidationOptions(org.openrewrite.test.TypeValidation.none())
            },
            kotlin(
                // Byte-for-byte from commercetools/rmf-codegen
                // codegen-renderers/.../MethodExtensions.kt#Method.returnType:
                // four-space indent for the function body and an eight-space
                // continuation indent on the chain dots (so `.filter` sits at
                // column 12).
                """
                interface Body
                interface Response { val statusCode: Int; val bodies: List<Body>? }
                class NilType : Body
                object TypesFactory { fun createNilType(): Body = NilType() }
                class Method { val responses: List<Response> = emptyList() }
                fun Response.isSuccessfull(): Boolean = statusCode in (200..299)
                fun Method.returnType(): Body {
                    return this.responses
                            .filter { it.isSuccessfull() }
                            .filter { it.bodies?.isNotEmpty() ?: false }
                            .firstOrNull()
                            ?.let { it.bodies!![0] }
                            ?: TypesFactory.createNilType()
                }
                """,
                """
                interface Body
                interface Response { val statusCode: Int; val bodies: List<Body>? }
                class NilType : Body
                object TypesFactory { fun createNilType(): Body = NilType() }
                class Method { val responses: List<Response> = emptyList() }
                fun Response.isSuccessfull(): Boolean = statusCode in (200..299)
                fun Method.returnType(): Body {
                    return this.responses
                            .filter { it.isSuccessfull() }
                            .firstOrNull { it.bodies?.isNotEmpty() ?: false }
                            ?.let { it.bodies!![0] }
                            ?: TypesFactory.createNilType()
                }
                """,
            ),
        )
    }

    @Test
    fun `chain collapse — multi-line lambda body keeps internal indentation`() {
        // Real-world case from aws/aws-toolkit-jetbrains: a `map { ... }` whose
        // lambda body spans multiple statements gets rewritten to `mapNotNull
        // { ... }`. The lambda body lines (4-space indented inside the lambda)
        // must keep their indentation. The bug: the multi-statement lambda body
        // collapses to flat / left-aligned text in the rewritten output.
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseMapNotNull = recipe(
                    displayName = "...",
                    description = "..."
                ) {
                    edit {
                        rewrite { xs: List<Any>, f: (Any) -> Int? -> xs.map(f).filterNotNull() } to { xs, f -> xs.mapNotNull(f) }
                    }
                }
            """.trimIndent(),
            propertyName = "UseMapNotNull",
        )
        rewriteRun(
            { spec ->
                spec.recipe(r)
                spec.typeValidationOptions(org.openrewrite.test.TypeValidation.none())
            },
            kotlin(
                // Byte-for-byte from aws/aws-toolkit-jetbrains
                // plugins/toolkit/jetbrains-rider/.../DotNetRuntimeUtils.kt.
                // Chain at 12-space indent (function body 8 + 4-space chain),
                // lambda body at 16-space indent (12 + 4), closing `}` at 12.
                """
                fun versions(runtimeList: List<String>): List<String?> {
                    val versionRegex = Regex("(\\d+.\\d+.\\d+)")
                    val versions = runtimeList
                        .filter { it.startsWith("Microsoft.NETCore.App") }
                        .map { runtimeString ->
                            val match = versionRegex.find(runtimeString) ?: return@map null
                            match.groups[1]?.value ?: return@map null
                        }
                        .filterNotNull()
                    return versions
                }
                """,
                """
                fun versions(runtimeList: List<String>): List<String?> {
                    val versionRegex = Regex("(\\d+.\\d+.\\d+)")
                    val versions = runtimeList
                        .filter { it.startsWith("Microsoft.NETCore.App") }
                        .mapNotNull { runtimeString ->
                            val match = versionRegex.find(runtimeString) ?: return@map null
                            match.groups[1]?.value ?: return@map null
                        }
                    return versions
                }
                """,
            ),
        )
    }

    @Test
    fun `chain collapse — long chain with intermediate plus calls preserves layout`() {
        // Real-world case from commercetools/rmf-codegen
        // (CsharpObjectTypeExtensions.kt). The matched chain has a long pre-chain
        // (`map → plus → plus → filterNotNull → map → map`) before the
        // collapsing `map(f).filterNotNull()` 2-segment match at the tail. The
        // captured `xs` substitution is the entire long pre-chain; its interior
        // newlines must survive intact when it's plugged back in as the new
        // `mapNotNull` outer's select.
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseMapNotNull = recipe(
                    displayName = "...",
                    description = "..."
                ) {
                    edit {
                        rewrite { xs: List<Any>, f: (Any) -> Int? -> xs.map(f).filterNotNull() } to { xs, f -> xs.mapNotNull(f) }
                    }
                }
            """.trimIndent(),
            propertyName = "UseMapNotNull",
        )
        rewriteRun(
            { spec ->
                spec.recipe(r)
                spec.typeValidationOptions(org.openrewrite.test.TypeValidation.none())
            },
            kotlin(
                // Byte-for-byte from commercetools/rmf-codegen
                // languages/csharp/.../CsharpObjectTypeExtensions.kt#getUsings.
                // 16-space chain indent inside an interface method body. The
                // chain has comment lines mid-stream and a double-space after
                // `=` — both should pass through unchanged.
                """
                interface VrapType { val name: String }
                interface VrapTypeProvider { fun doSwitch(t: Any): VrapType }
                interface Property { val type: Any? }
                interface ObjectType {
                    val allProperties: List<Property>
                    val type: Any?
                    fun discriminatorProperty(): Property?
                    val vrapTypeProvider: VrapTypeProvider
                    fun getUsingsForType(t: VrapType): String?
                }

                interface CsharpObjectTypeExtensions {
                    fun ObjectType.getUsings(): List<String> {
                        var usingsList =  this.allProperties
                                .map { it.type }
                                //If the subtypes are in the same package they should be imported
                                //.plus(this.namedSubTypes())
                                .plus(this.type)
                                .plus(discriminatorProperty()?.type)
                                .filterNotNull()
                                .map { vrapTypeProvider.doSwitch(it) }
                                .map { getUsingsForType(it) }
                                .filterNotNull()
                                .sortedBy { it }
                                .distinct()
                                .toList()
                        return usingsList
                    }
                }
                """,
                """
                interface VrapType { val name: String }
                interface VrapTypeProvider { fun doSwitch(t: Any): VrapType }
                interface Property { val type: Any? }
                interface ObjectType {
                    val allProperties: List<Property>
                    val type: Any?
                    fun discriminatorProperty(): Property?
                    val vrapTypeProvider: VrapTypeProvider
                    fun getUsingsForType(t: VrapType): String?
                }

                interface CsharpObjectTypeExtensions {
                    fun ObjectType.getUsings(): List<String> {
                        var usingsList =  this.allProperties
                                .map { it.type }
                                //If the subtypes are in the same package they should be imported
                                //.plus(this.namedSubTypes())
                                .plus(this.type)
                                .plus(discriminatorProperty()?.type)
                                .filterNotNull()
                                .map { vrapTypeProvider.doSwitch(it) }
                                .mapNotNull { getUsingsForType(it) }
                                .sortedBy { it }
                                .distinct()
                                .toList()
                        return usingsList
                    }
                }
                """,
            ),
        )
    }

    @Test
    fun `bare single-call rewrite — closing brace stays inline with lambda body end`() {
        // Real-world case from ybonjour/test-store: rewrite
        // `results.map { it.x }.sum()` to `results.sumOf { it.x }`. The lambda
        // body is a single inline expression, so the closing `}` must stay on
        // the same line as the body. The bug: the closing `}` ends up on its
        // own line after the rewrite.
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseSumOfWithSelector = recipe(
                    displayName = "...",
                    description = "..."
                ) {
                    edit {
                        rewrite { xs: List<Any>, f: (Any) -> Int -> xs.map(f).sum() } to { xs, f -> xs.sumOf(f) }
                    }
                }
            """.trimIndent(),
            propertyName = "UseSumOfWithSelector",
        )
        rewriteRun(
            { spec ->
                spec.recipe(r)
                spec.typeValidationOptions(org.openrewrite.test.TypeValidation.none())
            },
            kotlin(
                """
                data class Result(val durationMillis: Long?, val testName: String)
                data class Run(val id: java.util.UUID?, val testSuite: java.util.UUID?, val time: java.util.Date?)
                class RunOverview(val run: Run, val total: Long)
                class Repo { fun findAllByRunId(id: java.util.UUID): List<Result> = emptyList() }
                class Svc(val resultRepository: Repo) {
                    private fun getRunOverview(run: Run): RunOverview {
                        val results = resultRepository.findAllByRunId(run.id!!)
                        val totalDuration = results.map { it.durationMillis!! }.sum()

                        run.testSuite?.let { testSuite ->
                            run.time?.let { time ->
                                run.id?.let { runId ->
                                    return RunOverview(run, totalDuration)
                                }
                            }
                        }

                        return RunOverview(run, totalDuration)
                    }
                }
                """,
                """
                data class Result(val durationMillis: Long?, val testName: String)
                data class Run(val id: java.util.UUID?, val testSuite: java.util.UUID?, val time: java.util.Date?)
                class RunOverview(val run: Run, val total: Long)
                class Repo { fun findAllByRunId(id: java.util.UUID): List<Result> = emptyList() }
                class Svc(val resultRepository: Repo) {
                    private fun getRunOverview(run: Run): RunOverview {
                        val results = resultRepository.findAllByRunId(run.id!!)
                        val totalDuration = results.sumOf { it.durationMillis!! }

                        run.testSuite?.let { testSuite ->
                            run.time?.let { time ->
                                run.id?.let { runId ->
                                    return RunOverview(run, totalDuration)
                                }
                            }
                        }

                        return RunOverview(run, totalDuration)
                    }
                }
                """,
            ),
        )
    }

    @Test
    fun `multi-param selector rewrite attaches method type — sumBy to sumOf`() {
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseSumOf = recipe(
                    displayName = "Use sumOf",
                    description = "..."
                ) {
                    edit {
                        rewrite { xs: Iterable<Int>, selector: (Int) -> Int -> xs.sumBy(selector) } to { xs, selector -> xs.sumOf(selector) }
                    }
                }
            """.trimIndent(),
            propertyName = "UseSumOf",
        )
        rewriteRun(
            { spec -> spec.recipe(r) },
            kotlin(
                """
                fun total(xs: List<Int>): Int = xs.sumBy { it * 2 }
                """,
                """
                fun total(xs: List<Int>): Int = xs.sumOf { it * 2 }
                """,
            ),
        )
    }

    @Test
    fun `nested generic type argument in selector attaches method type`() {
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseFlatMap = recipe(
                    displayName = "Use flatMap",
                    description = "..."
                ) {
                    edit {
                        rewrite { xs: Iterable<Int>, f: (Int) -> List<Int> -> xs.map(f).flatten() } to { xs, f -> xs.flatMap(f) }
                    }
                }
            """.trimIndent(),
            propertyName = "UseFlatMap",
        )
        rewriteRun(
            { spec -> spec.recipe(r) },
            kotlin(
                """
                fun expand(xs: List<Int>): List<Int> = xs.map { listOf(it) }.flatten()
                """,
                """
                fun expand(xs: List<Int>): List<Int> = xs.flatMap { listOf(it) }
                """,
            ),
        )
    }

    @Test
    fun `use-site variance argument degrades gracefully to raw`() {
        // Use-site variance can't be rendered concretely, so the type falls back
        // to raw: the rewrite still applies but the method type is absent.
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseToMutableList = recipe(
                    displayName = "Use toMutableList",
                    description = "..."
                ) {
                    edit {
                        rewrite { xs: Iterable<out Number> -> xs.toList() } to { xs -> xs.toMutableList() }
                    }
                }
            """.trimIndent(),
            propertyName = "UseToMutableList",
        )
        rewriteRun(
            { spec ->
                spec.recipe(r)
                spec.typeValidationOptions(TypeValidation.builder().methodInvocations(false).build())
            },
            kotlin(
                """
                fun copy(xs: List<Int>): List<Int> = xs.toList()
                """,
                """
                fun copy(xs: List<Int>): List<Int> = xs.toMutableList()
                """,
            ),
        )
    }

    @Test
    fun `bare single-call rewrite preserves dot-on-its-own-line layout`() {
        // Same fix, different rewrite path: `methodInvocationRewrite` (the
        // non-chain bare path) also runs the template through
        // `KotlinTemplate.builder(...).apply(...)`, which loses the matched
        // outer's `select.after`. The fix is the same shared helper, but the
        // wiring is per-path — exercise the bare path explicitly so we don't
        // regress one branch while fixing another. The recipe pair here
        // (`lowercase` → `uppercase`) is semantically nonsensical; the test
        // is about formatting only. `toUpperCase`-style stdlib deprecations
        // are `@Deprecated(level=ERROR)` in modern Kotlin and won't compile
        // inside `RecipePluginCompileFixture`.
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseUppercase = recipe(
                    displayName = "Use uppercase()",
                    description = "..."
                ) {
                    edit {
                        rewrite { s: String -> s.lowercase() } to { s -> s.uppercase() }
                    }
                }
            """.trimIndent(),
            propertyName = "UseUppercase",
        )
        rewriteRun(
            { spec ->
                spec.recipe(r)
                spec.typeValidationOptions(org.openrewrite.test.TypeValidation.none())
            },
            kotlin(
                """
                fun upper(s: String): String = s
                    .lowercase()
                """,
                """
                fun upper(s: String): String = s
                    .uppercase()
                """,
            ),
        )
    }

    @Test
    fun `recipe targeting no-arg overload does not match the with-predicate overload`() {
        // Regression: with a `(..)` wildcard arg pattern, the recipe
        // `xs.filter(p).any() -> xs.any(p)` also matched the *predicate*
        // overload `xs.filter(p1).any { p2 }` and silently dropped `p2` (along
        // with the entire `.any { ... }` body the author wrote). `(..)` was
        // tightened to `(*,*)` / `(*)` / `()` — the precise JVM arg count —
        // so overloaded names no longer cross-match.
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseAnyWithPredicateInsteadOfFilterAny = recipe(
                    displayName = "...",
                    description = "..."
                ) {
                    edit {
                        rewrite { xs: List<Any>, p: (Any) -> Boolean -> xs.filter(p).any() } to { xs, p -> xs.any(p) }
                    }
                }
            """.trimIndent(),
            propertyName = "UseAnyWithPredicateInsteadOfFilterAny",
        )
        rewriteRun(
            { spec ->
                spec.recipe(r)
                spec.typeValidationOptions(org.openrewrite.test.TypeValidation.none())
            },
            kotlin(
                // `filter { ... }.any { ... }` — the WITH-predicate any. The
                // recipe targets the NO-arg any. Without the arg-count guard,
                // the rewrite drops the inner `.any { p2 }` body. With the
                // guard it leaves the source untouched (no expected diff).
                """
                fun hasMatch(xs: List<Int>): Boolean = xs
                    .filter { it > 0 }
                    .any { it > 10 }
                """,
            ),
        )
    }

    @Test
    fun `multi-before mixed shapes — receiver in one, arg in the other`() {
        // Two before lambdas with the same param count but different
        // canonical signatures: `s.toInt()` binds `s` to the (extension)
        // receiver, while `Integer.parseInt(s)` binds `s` to arg 0. The IR
        // pass produces a per-matcher substitution-source CSV (`-1` for
        // the receiver form, `0` for the arg form), joined by `\n`. The
        // helper detects the multi-CSV shape and dispatches per matcher to
        // fill the after template's `#{any(kotlin.String)}` placeholder
        // with the right substitution.
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val ParseIntToZero = recipe(
                    displayName = "Unify int parsing to literal 0",
                    description = "..."
                ) {
                    edit {
                        rewrite(
                            { s: String -> s.toInt() },
                            { s: String -> Integer.parseInt(s) }
                        ) to { s -> 0 }
                    }
                }
            """.trimIndent(),
            propertyName = "ParseIntToZero",
        )
        rewriteRun(
            { spec -> spec.recipe(r) },
            kotlin(
                """
                fun viaExt(s: String): Int = s.toInt()
                fun viaStatic(s: String): Int = Integer.parseInt(s)
                """,
                """
                fun viaExt(s: String): Int = 0
                fun viaStatic(s: String): Int = 0
                """,
            ),
        )
    }

    @Test
    fun `inlined Java static constant — Math_PI to kotlin_math_PI`() {
        // `Math.PI` is a primitive `public static final double`; K2 FIR2IR
        // compile-time-folds it to a bare `IrConst(3.141592...)` in the
        // recipe source, erasing the `Math` qualifier symbol entirely. The
        // IR pass falls back to parsing the source slice (`Math.PI`) to
        // recover the matcher spec `java.lang.Math#PI` and routes to the
        // `propertyAccessRewrite` helper — the same one that handles
        // Kotlin property-access patterns.
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseKotlinMathPi = recipe(
                    displayName = "Use kotlin.math.PI",
                    description = "..."
                ) {
                    edit {
                        rewrite { -> Math.PI } to { -> kotlin.math.PI }
                    }
                }
            """.trimIndent(),
            propertyName = "UseKotlinMathPi",
        )
        rewriteRun(
            { spec -> spec.recipe(r) },
            kotlin(
                """
                fun circumference(r: Double): Double = 2 * Math.PI * r
                """.trimIndent(),
                """
                fun circumference(r: Double): Double = 2 * kotlin.math.PI * r
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `inlined Java static constant — Integer_MAX_VALUE to Int_MAX_VALUE`() {
        // Cross-classpath sanity: `Integer.MAX_VALUE` resolves through the
        // `java.lang.Integer` candidate, `Int.MAX_VALUE` (the after template's
        // top-level `kotlin.Int.Companion.MAX_VALUE`) is also IrConst-inlined
        // and recovered via the after-template's qualifier extension.
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseKotlinIntMax = recipe(
                    displayName = "Use Int.MAX_VALUE",
                    description = "..."
                ) {
                    edit {
                        rewrite { -> Integer.MAX_VALUE } to { -> Int.MAX_VALUE }
                    }
                }
            """.trimIndent(),
            propertyName = "UseKotlinIntMax",
        )
        rewriteRun(
            { spec -> spec.recipe(r) },
            kotlin(
                """
                fun cap(): Int = Integer.MAX_VALUE
                """.trimIndent(),
                """
                fun cap(): Int = Int.MAX_VALUE
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `imperative recipe — edit kotlin visitClassDeclaration rewrites class shape`() {
        // Exercises the imperative-shape fallback: the K2 plugin can't extract
        // an `edit { rewrite { } to { } }` template, so it instead synthesizes
        // `<Name>$KtRecipe` whose `getVisitor()` body delegates back to
        // `buildImperativeVisitor(<original recipe block>)`. The generated
        // class is field-less — see `imperative recipe — generated class is
        // serializable (no instance fields)` for the Jackson-roundtrip proof.
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val AppendBangToClass = recipe(
                    displayName = "Suffix class names with !",
                    description = "..."
                ) {
                    edit {
                        kotlin {
                            visitClassDeclaration { cls ->
                                if (cls.simpleName.endsWith("!")) cls
                                else cls.withName(cls.name.withSimpleName(cls.simpleName + "!"))
                            }
                        }
                    }
                }
            """.trimIndent(),
            propertyName = "AppendBangToClass",
        )
        rewriteRun(
            { spec -> spec.recipe(r) },
            kotlin(
                """
                class Foo
                """.trimIndent(),
                """
                class Foo!
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `imperative recipe — generated class is field-less`() {
        // Jackson roundtrip is gated on the recipe class having no instance
        // fields beyond declared `@Option` ones (the runtime DSL's anonymous
        // Recipe captures the `block` lambda as a synthetic field — that's
        // what breaks serialization). The plugin's imperative-recipe path
        // produces a top-level `<Name>$KtRecipe` whose state is reconstructed
        // fresh on each `getVisitor()` call, so the class itself has zero
        // fields. Verify by reflection.
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val Noop = recipe(
                    displayName = "Noop",
                    description = "..."
                ) {
                    edit { kotlin { visitClassDeclaration { it } } }
                }
            """.trimIndent(),
            propertyName = "Noop",
        )
        assertThat(r::class.java.simpleName).isEqualTo("Noop\$KtRecipe")
        assertThat(r::class.java.declaredFields).isEmpty()
    }

    @Test
    fun `recipes composite — generated class exposes children`() {
        // The K2 plugin synthesizes a `<Name>$KtRecipe` class for `recipes(...)`
        // properties too, so composites are discoverable by classpath scanning
        // (and end up in `recipes.csv`). The synthesized class extends Recipe
        // directly and overrides getRecipeList() to return `listOf(<children>)`.
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                import org.openrewrite.recipes
                val A = recipe("A", "first") { edit { kotlin { visitClassDeclaration { it } } } }
                val B = recipe("B", "second") { edit { kotlin { visitClassDeclaration { it } } } }
                val Combo = recipes("Combo", "A then B", A, B)
            """.trimIndent(),
            propertyName = "Combo",
        )
        assertThat(r::class.java.simpleName).isEqualTo("Combo\$KtRecipe")
        assertThat(r::class.java.declaredFields).isEmpty()
        assertThat(r.displayName).isEqualTo("Combo")
        assertThat(r.description).isEqualTo("A then B")
        assertThat(r.recipeList).hasSize(2)
        assertThat(r.recipeList.map { it::class.java.simpleName })
            .containsExactly("A\$KtRecipe", "B\$KtRecipe")
    }

    @Test
    fun `generated visitor is wrapped with UsesMethod precondition`() {
        // Mirrors what Refaster does in its generated Java recipes: the
        // synthesized `getVisitor()` returns a Preconditions.Check whose
        // inner `check` visitor is a UsesMethod for the recipe's matcher
        // spec. The framework uses this to skip files that don't reference
        // the targeted member at all.
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseUppercase = recipe("d", "desc") {
                    edit {
                        rewrite { s: String -> s.lowercase() } to { s -> s.uppercase() }
                    }
                }
            """.trimIndent(),
            propertyName = "UseUppercase",
        )
        val visitor = r.getVisitor()
        assertThat(visitor).isInstanceOf(Preconditions.Check::class.java)
        val check = (visitor as Preconditions.Check).check
        assertThat(check).isInstanceOf(UsesMethod::class.java)
        assertThat((check as UsesMethod<*>).methodMatcher.toString())
            .contains("lowercase")
    }

    @Test
    fun `property-access recipe is wrapped with UsesField precondition`() {
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseKotlinMathPi = recipe("d", "desc") {
                    edit {
                        rewrite { -> Math.PI } to { -> kotlin.math.PI }
                    }
                }
            """.trimIndent(),
            propertyName = "UseKotlinMathPi",
        )
        val visitor = r.getVisitor()
        assertThat(visitor).isInstanceOf(Preconditions.Check::class.java)
        assertThat((visitor as Preconditions.Check).check).isInstanceOf(UsesField::class.java)
    }

    @Test
    fun `precondition skips files that don't use the targeted method`() {
        // A source that doesn't call `lowercase()` anywhere must be left
        // untouched even though the walker, if invoked, would visit every
        // method invocation in the file. RewriteTest's single-arg `kotlin(...)`
        // form asserts no change.
        rewriteRun(
            kotlin(
                """
                fun unrelated() {
                    val n = 1 + 2
                    println(n)
                }
                """,
            ),
        )
    }

    @Test
    fun `metadata accessors populate correctly on generated recipe`() {
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                import java.time.Duration
                val MyRecipe = recipe(
                    displayName = "My recipe",
                    description = "Test description",
                    tags = setOf("test", "smoke"),
                    estimatedEffortPerOccurrence = Duration.ofMinutes(3),
                ) {
                    edit {
                        rewrite { s: String -> s.lowercase() } to { s -> s.uppercase() }
                    }
                }
            """.trimIndent(),
            propertyName = "MyRecipe",
        )
        assertThat(r.displayName).isEqualTo("My recipe")
        assertThat(r.description).isEqualTo("Test description")
        assertThat(r.getTags()).containsExactlyInAnyOrder("test", "smoke")
        assertThat(r.estimatedEffortPerOccurrence?.toMinutes()).isEqualTo(3L)
    }

    @Test
    fun `concatenated displayName and description fold to constants`() {
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val MyRecipe = recipe(
                    displayName = "Replace " + "lowercase",
                    description = "Uses " + "uppercase" + " instead",
                ) {
                    edit {
                        rewrite { s: String -> s.lowercase() } to { s -> s.uppercase() }
                    }
                }
            """.trimIndent(),
            propertyName = "MyRecipe",
        )
        assertThat(r.displayName).isEqualTo("Replace lowercase")
        assertThat(r.description).isEqualTo("Uses uppercase instead")
    }

    @Test
    fun `text-block description with trimIndent folds to a constant`() {
        val tq = "\"\"\""
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val MyRecipe = recipe(
                    displayName = "d",
                    description = $tq
                        Alpha
                        Beta
                    $tq.trimIndent(),
                ) {
                    edit {
                        rewrite { s: String -> s.lowercase() } to { s -> s.uppercase() }
                    }
                }
            """.trimIndent(),
            propertyName = "MyRecipe",
        )
        assertThat(r.displayName).isEqualTo("d")
        assertThat(r.description).isEqualTo("Alpha\nBeta")
    }

    @Test
    fun `composite recipes metadata folds concatenation`() {
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                import org.openrewrite.recipes
                val A = recipe("A", "first") { edit { kotlin { visitClassDeclaration { it } } } }
                val Combo = recipes("Co" + "mbo", "A" + " only", A)
            """.trimIndent(),
            propertyName = "Combo",
        )
        assertThat(r.displayName).isEqualTo("Combo")
        assertThat(r.description).isEqualTo("A only")
        assertThat(r.recipeList).hasSize(1)
    }

    @Test
    fun `variadic by-example — asList to List_of matches any arity`() {
        // The author writes a representative 3-arg shape; because `asList` is a
        // varargs method the recipe generalizes to ANY arity (2, 4, even 0).
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseListOf = recipe(
                    displayName = "Use List.of",
                    description = "..."
                ) {
                    edit {
                        rewrite { a: Any, b: Any, c: Any -> java.util.Arrays.asList(a, b, c) } to { a, b, c -> java.util.List.of(a, b, c) }
                    }
                }
            """.trimIndent(),
            propertyName = "UseListOf",
        )
        rewriteRun(
            { spec -> spec.recipe(r) },
            java(
                """
                import java.util.Arrays;
                import java.util.List;
                class A {
                    List<Object> two = Arrays.asList(1, 2);
                    List<Object> four = Arrays.asList(1, 2, 3, 4);
                    List<Object> none = Arrays.asList();
                }
                """.trimIndent(),
                """
                import java.util.Arrays;
                import java.util.List;
                class A {
                    List<Object> two = java.util.List.of(1, 2);
                    List<Object> four = java.util.List.of(1, 2, 3, 4);
                    List<Object> none = java.util.List.of();
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `variadic by-example — Kotlin source fixture`() {
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseListOf = recipe(
                    displayName = "Use List.of",
                    description = "..."
                ) {
                    edit {
                        rewrite { a: Any, b: Any, c: Any -> java.util.Arrays.asList(a, b, c) } to { a, b, c -> java.util.List.of(a, b, c) }
                    }
                }
            """.trimIndent(),
            propertyName = "UseListOf",
        )
        rewriteRun(
            { spec -> spec.recipe(r) },
            kotlin(
                """
                fun use() {
                    java.util.Arrays.asList(1, 2, 3, 4, 5)
                }
                """.trimIndent(),
                """
                fun use() {
                    java.util.List.of(1, 2, 3, 4, 5)
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `variadic spread — asList(star args) to List_of(star args)`() {
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseListOf = recipe(
                    displayName = "Use List.of",
                    description = "..."
                ) {
                    edit {
                        rewrite { args: Array<Any> -> java.util.Arrays.asList(*args) } to { args -> java.util.List.of(*args) }
                    }
                }
            """.trimIndent(),
            propertyName = "UseListOf",
        )
        rewriteRun(
            { spec -> spec.recipe(r) },
            java(
                """
                import java.util.Arrays;
                import java.util.List;
                class A {
                    List<Object> xs = Arrays.asList(1, 2, 3);
                }
                """.trimIndent(),
                """
                import java.util.Arrays;
                import java.util.List;
                class A {
                    List<Object> xs = java.util.List.of(1, 2, 3);
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `prefix plus variadic — String_format to formatted`() {
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseFormatted = recipe(
                    displayName = "Use String.formatted",
                    description = "..."
                ) {
                    edit {
                        rewrite { fmt: String, a: Any, b: Any -> java.lang.String.format(fmt, a, b) } to { fmt, a, b -> fmt.formatted(a, b) }
                    }
                }
            """.trimIndent(),
            propertyName = "UseFormatted",
        )
        rewriteRun(
            { spec -> spec.recipe(r) },
            java(
                """
                class A {
                    String s = String.format("%s %s %s", 1, 2, 3);
                }
                """.trimIndent(),
                """
                class A {
                    String s = "%s %s %s".formatted(1, 2, 3);
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `reorder — move trailing exception ahead of the varargs run`() {
        // The slf4j-style migration: internal `log(msg, throwable, params...)`
        // becomes `error(msg, params..., throwable)` — the fixed Throwable
        // jumps from before the varargs run to after it.
        val r = loadCompiledRecipe(
            source = """
                package demo
                import org.openrewrite.recipe
                class Log {
                    fun internal(msg: String, t: Throwable, vararg params: Any) {}
                    fun error(msg: String, vararg params: Any) {}
                }
                val ReorderLog = recipe(
                    displayName = "Reorder log args",
                    description = "..."
                ) {
                    edit {
                        rewrite { log: Log, msg: String, t: Throwable, a: Any, b: Any -> log.internal(msg, t, a, b) } to { log, msg, t, a, b -> log.error(msg, a, b, t) }
                    }
                }
            """.trimIndent(),
            propertyName = "ReorderLog",
            packageName = "demo",
        )
        rewriteRun(
            // `Log` is a full source so the before is properly typed and matched.
            // The rewritten `log.error(...)` still needs method-invocation type
            // validation relaxed: the generated recipe's JavaTemplate parses the
            // after template with a JDK-only classpath, so it can't re-attribute
            // a method on the test's own `demo.Log` (a real recipe targeting a
            // classpath type like org.slf4j.Logger resolves fine). Same reason
            // ~9 other tests in this file use TypeValidation.none(); the reorder
            // itself is asserted by the before/after text.
            { spec -> spec.recipe(r).typeValidationOptions(TypeValidation.builder().methodInvocations(false).build()) },
            java(
                """
                package demo;
                class Log {
                    void internal(String msg, Throwable t, Object... params) {}
                    void error(String msg, Object... params) {}
                }
                """.trimIndent(),
            ),
            java(
                """
                package demo;
                class Use {
                    void m(Log log, RuntimeException ex) {
                        log.internal("boom", ex, 1, 2, 3);
                    }
                }
                """.trimIndent(),
                """
                package demo;
                class Use {
                    void m(Log log, RuntimeException ex) {
                        log.error("boom", 1, 2, 3, ex);
                    }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `strictArity — opt out of variadic matching`() {
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseListOfExactly2 = recipe(
                    displayName = "Use List.of for exactly 2 args",
                    description = "..."
                ) {
                    edit {
                        (rewrite { a: Any, b: Any -> java.util.Arrays.asList(a, b) } to { a, b -> java.util.List.of(a, b) }).strictArity()
                    }
                }
            """.trimIndent(),
            propertyName = "UseListOfExactly2",
        )
        rewriteRun(
            { spec -> spec.recipe(r) },
            java(
                """
                import java.util.Arrays;
                import java.util.List;
                class A {
                    List<Object> two = Arrays.asList(1, 2);
                    List<Object> three = Arrays.asList(1, 2, 3);
                }
                """.trimIndent(),
                """
                import java.util.Arrays;
                import java.util.List;
                class A {
                    List<Object> two = java.util.List.of(1, 2);
                    List<Object> three = Arrays.asList(1, 2, 3);
                }
                """.trimIndent(),
            ),
        )
    }

    private fun loadCompiledRecipe(source: String, propertyName: String, packageName: String = ""): Recipe {
        val result = RecipePluginCompileFixture.compile(source)
        check(result.exitOk()) { "compile failed:\n${result.messages}" }
        val topLevelClass = result.classLoader.loadClass(
            if (packageName.isEmpty()) "RecipesKt" else "$packageName.RecipesKt"
        )
        val getter = topLevelClass.getDeclaredMethod("get" + propertyName.replaceFirstChar { it.uppercase() })
        getter.isAccessible = true
        return getter.invoke(null) as Recipe
    }
}
