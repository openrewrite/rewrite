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

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * FIR-level shape checks for the recipe authoring DSL — runs on every property
 * whose initializer is a call to `org.openrewrite.recipe(...)`. The new
 * [[dsl-redesign-plan-2026-05-16]] surface narrowed v0's rule set substantially
 * because most "ill-formed shape" cases are now caught by Kotlin's type system
 * and `@DslMarker`:
 *
 *  - Nested language scopes (`kotlin { yaml { … } }`) — blocked by `@DslMarker`
 *    on the LanguageScope hierarchy.
 *  - Language scopes at recipe-block level (`recipe { kotlin { … } }`) — the
 *    `kotlin` factory only exists on `LanguageHost`, not `RecipeBuilder`, so
 *    Kotlin's overload resolution rejects it.
 *  - Phase-level helpers inside language scopes (`kotlin { uses<T>() }`) —
 *    `uses<T>()` is declared on `EditScope`, not `LanguageScope`, so it's an
 *    unresolved-reference compile error.
 *
 * What's left for the FIR checker:
 *
 *  1. **Count limits.** At most one bare `edit { }`, one bare `generate { }`,
 *     and one `scan { … }` chain per recipe (single-scan v1). The runtime
 *     builder also `require`s this at recipe-construction time, but a
 *     compile-time error is better UX — authors see the rule when they type,
 *     not when they run.
 *  2. **Scan chain termination.** A `scan<A>(initial) { … }` whose expression
 *     value is discarded — not chained into `.edit { }` or `.generate { }` —
 *     would compute the accumulator and throw it away. Error at compile time.
 *  3. **Orphan rewrite.** `rewrite { … }` without a trailing `to { … }` is the
 *     one orphan-prone shape in the DSL (since `to` is a separate infix call).
 *     The lone `rewrite { }` type-checks (unused `RewriteAdvice` value is
 *     discarded) and produces no transformation. Error so the author fixes it.
 *
 * Rules 1–3 only inspect the top-level statements of the recipe block; the
 * checker intentionally doesn't recurse into nested lambdas so user code
 * inside a pattern lambda can use names like `scan` / `edit` freely.
 *
 * Why diagnostics route through `FirErrors.OTHER_ERROR_WITH_REASON`:
 * registering a custom `KtDiagnosticsContainer` in Kotlin 2.3 requires
 * accessing `KtDiagnosticFactoryToRendererMap`'s internal constructor. The
 * pre-registered string-arg `OTHER_ERROR_WITH_REASON` renders cleanly and is
 * adequate while the rule set is small.
 */
internal object RecipeFirDslCheckers : FirPropertyChecker(MppCheckerKind.Common) {

    private val RECIPE_FQN = CallableId(FqName("org.openrewrite"), Name.identifier("recipe"))

    private val NAME_TO = Name.identifier("to")
    private val NAME_SCAN = Name.identifier("scan")
    private val NAME_EDIT = Name.identifier("edit")
    private val NAME_GENERATE = Name.identifier("generate")
    private val NAME_REWRITE = Name.identifier("rewrite")

    /** Classification of a top-level statement inside the `recipe { … }` block. */
    private sealed interface StatementKind {
        /** Bare `edit { … }` at the recipe-block level (no preceding scan chain). */
        object BareEdit : StatementKind

        /** Bare `generate { … }` at the recipe-block level (no preceding scan chain). */
        object BareGenerate : StatementKind

        /**
         * `scan<A>(initial) { … }` followed by a chain. We consider the
         * outermost call in the chain — i.e., for `scan { … }.edit { … }`
         * the outermost is the `edit` call on the [Scan] receiver, and for
         * `scan { … }.generate { … }.edit { … }` the outermost is the `edit`
         * call on the chained Scan return. The chain MUST end in `edit` or
         * `generate` so the accumulator gets consumed.
         */
        data class ScanChain(val hasEdit: Boolean, val hasGenerate: Boolean) : StatementKind

        /** Naked `scan<A>(initial) { … }` whose expression value is discarded. */
        object OrphanScan : StatementKind

        /** Naked `rewrite { … }` without a trailing `to { … }`. */
        object OrphanRewrite : StatementKind

        /** Anything else (helpers, comments, val declarations the checker doesn't reason about). */
        object Other : StatementKind
    }

    context(@Suppress("unused") context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val initializer = declaration.initializer as? FirFunctionCall ?: return
        if (!initializer.callsRecipeBuilder()) return

        val body = initializer.findTrailingLambdaBody() ?: return

        val classified: List<Pair<FirStatement, StatementKind>> = body.statements.map { it to classify(it) }

        // Rule 3 first — orphan `rewrite(...)` with no `to`. The DSL surface
        // makes `to` a separate infix call, so a recipe body of just
        // `rewrite { ... }` type-checks (the unused `RewriteAdvice` value is
        // discarded) and silently does nothing at runtime. Catch it here
        // before piling on count-related diagnostics.
        for ((stmt, kind) in classified) {
            if (kind === StatementKind.OrphanRewrite) {
                reportError(
                    stmt.source ?: declaration.source ?: return,
                    "`rewrite(...)` without a trailing `to { ... }` produces no " +
                        "transformation. Add `to { /* after */ }` or remove the call.",
                )
            }
        }
        // Orphan rewrite can also appear nested inside an `edit { }` /
        // `scan { }` / `generate { }` block — the canonical home for
        // `rewrite { } to { }` in the new DSL. Walk one level into each
        // phase block and apply the same orphan check.
        for ((stmt, kind) in classified) {
            val phaseLambda = when (kind) {
                StatementKind.BareEdit, StatementKind.BareGenerate, is StatementKind.ScanChain ->
                    phaseLambdaBody(stmt)
                else -> emptyList()
            }
            if (phaseLambda.isEmpty()) continue
            checkOrphanRewriteInsidePhase(phaseLambda, declaration)
        }

        // Rule 2 — orphan `scan { }` (no chained `.edit` / `.generate`). Same
        // failure mode as orphan-rewrite: the lambda runs, the accumulator is
        // computed, the framework never consumes it.
        for ((stmt, kind) in classified) {
            if (kind === StatementKind.OrphanScan) {
                reportError(
                    stmt.source ?: declaration.source ?: return,
                    "`scan { ... }` must be chained with `.edit { ... }` and/or `.generate { ... }`. " +
                        "A bare scan computes the accumulator and discards it.",
                )
            }
        }

        // Rule 1 — count limits. The runtime builder also enforces these via
        // `require(...)`, but a compile-time error reaches the author sooner.
        val bareEdits = classified.filter { it.second === StatementKind.BareEdit }
        val bareGenerates = classified.filter { it.second === StatementKind.BareGenerate }
        val scanChains = classified.filter { it.second is StatementKind.ScanChain }

        if (bareEdits.size > 1) {
            reportError(
                bareEdits[1].first.source ?: declaration.source ?: return,
                "Recipe declares more than one bare `edit { }` block. " +
                    "Compose multi-statement edits inside a single `edit { … }` body — " +
                    "they run sequentially via `composeSequential`.",
            )
        }
        if (bareGenerates.size > 1) {
            reportError(
                bareGenerates[1].first.source ?: declaration.source ?: return,
                "Recipe declares more than one bare `generate { }` block. " +
                    "Merge generation logic into a single `generate { … }` body.",
            )
        }
        if (scanChains.size > 1) {
            reportError(
                scanChains[1].first.source ?: declaration.source ?: return,
                "Recipe declares more than one `scan { … }` chain (single-scan v1). " +
                    "Multi-scan composition is on the deferred runway.",
            )
        }

        // Mixing a scan chain with a bare edit/generate is also a single-scan-v1
        // violation: the bare edit can't see the accumulator, so the author
        // almost certainly meant to chain it.
        if (scanChains.isNotEmpty() && bareEdits.isNotEmpty()) {
            reportError(
                bareEdits.first().first.source ?: declaration.source ?: return,
                "Bare `edit { }` cannot run alongside a `scan { … }` chain — chain " +
                    "the edit onto the scan as `scan { … }.edit { … }` so it can read `acc`.",
            )
        }
        if (scanChains.isNotEmpty() && bareGenerates.isNotEmpty()) {
            reportError(
                bareGenerates.first().first.source ?: declaration.source ?: return,
                "Bare `generate { }` cannot run alongside a `scan { … }` chain — chain " +
                    "the generate onto the scan as `scan { … }.generate { … }` so it can read `acc`.",
            )
        }
    }

    context(@Suppress("unused") context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportError(source: KtSourceElement, message: String) {
        if (source.kind is KtFakeSourceElementKind) return
        reporter.reportOn(source, FirErrors.OTHER_ERROR_WITH_REASON, message)
    }

    /**
     * Classify a top-level statement of the recipe block by walking the
     * outermost call's simple-name chain. Only the structural shape matters;
     * we never recurse into argument lambdas, so user code inside an `edit`
     * body can use any name without false-positive diagnostics.
     */
    private fun classify(statement: FirStatement): StatementKind {
        // A recipe-body statement is either a bare DSL call or a property
        // declaration whose initializer is a DSL call (`val r = scan(...)`).
        // Both shapes need classification.
        val call = statement as? FirFunctionCall
            ?: (statement as? FirProperty)?.initializer as? FirFunctionCall
            ?: return StatementKind.Other

        // Walk the chain receiver-ward to find the head call. For
        // `scan { … }.edit { … }`, the outermost call is `edit`, its dispatch
        // receiver is `scan(…) { … }`. We classify by what the chain DOES.
        val chain = unwindChain(call)
        return when {
            chain.head?.callableSimpleName() == NAME_REWRITE && chain.outermost.callableSimpleName() != NAME_TO ->
                StatementKind.OrphanRewrite
            chain.head?.callableSimpleName() == NAME_SCAN -> {
                val hasEdit = chain.tailNames.contains(NAME_EDIT)
                val hasGenerate = chain.tailNames.contains(NAME_GENERATE)
                if (!hasEdit && !hasGenerate) StatementKind.OrphanScan
                else StatementKind.ScanChain(hasEdit, hasGenerate)
            }
            // Bare-edit / bare-generate / bare-rewrite-with-to. We only treat
            // the call as "bare" when it has NO dispatch-receiver call (i.e.
            // it's a direct method on the recipe block's receiver).
            chain.head === call -> when (call.callableSimpleName()) {
                NAME_EDIT -> StatementKind.BareEdit
                NAME_GENERATE -> StatementKind.BareGenerate
                NAME_TO -> StatementKind.Other // `rewrite { } to { }` at recipe-block level — diagnosed elsewhere or just allowed
                else -> StatementKind.Other
            }
            else -> StatementKind.Other
        }
    }

    /**
     * Result of unwinding a dotted-call chain like `scan { … }.generate { … }.edit { … }`:
     *  - [outermost] is the original call (the `edit` end).
     *  - [head] is the innermost call in the chain (the `scan` end).
     *  - [tailNames] is the set of simple names traversed FROM head TO outermost
     *    (excluding head itself), so for the example above `tailNames = {generate, edit}`.
     *
     * Stops walking when the dispatch receiver isn't another `FirFunctionCall`.
     */
    private data class ChainInfo(
        val outermost: FirFunctionCall,
        val head: FirFunctionCall?,
        val tailNames: Set<Name>,
    )

    private fun unwindChain(call: FirFunctionCall): ChainInfo {
        val tail = mutableSetOf<Name>()
        var cursor: FirFunctionCall = call
        // Tail names traverse from outermost down to (but excluding) the
        // innermost head call.
        while (true) {
            val receiver = cursor.explicitReceiver as? FirFunctionCall ?: break
            tail += cursor.callableSimpleName() ?: Name.special("<unknown>")
            cursor = receiver
        }
        return ChainInfo(outermost = call, head = cursor, tailNames = tail)
    }

    private fun FirFunctionCall.callsRecipeBuilder(): Boolean {
        val symbol = calleeReference.toResolvedCallableSymbol() ?: return false
        return symbol.callableId == RECIPE_FQN
    }

    private fun FirFunctionCall.findTrailingLambdaBody(): FirBlock? {
        val last = argumentList.arguments.lastOrNull() ?: return null
        val lambda = last as? FirAnonymousFunctionExpression ?: return null
        return lambda.anonymousFunction.body
    }

    /**
     * Extracts the trailing-lambda body of a `bareEdit` / `bareGenerate` / outermost
     * `scanChain` call so we can walk INTO the phase block. For chains
     * (`scan { … }.edit { … }`) we want each chained call's trailing lambda —
     * walk the receiver chain and visit every link.
     */
    private fun phaseLambdaBody(statement: FirStatement): List<FirBlock> {
        val call = statement as? FirFunctionCall
            ?: (statement as? FirProperty)?.initializer as? FirFunctionCall
            ?: return emptyList()
        val bodies = mutableListOf<FirBlock>()
        var cursor: FirFunctionCall? = call
        while (cursor != null) {
            cursor.findTrailingLambdaBody()?.let { bodies += it }
            cursor = cursor.explicitReceiver as? FirFunctionCall
        }
        return bodies
    }

    context(@Suppress("unused") context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkOrphanRewriteInsidePhase(phaseBodies: List<FirBlock>, declaration: FirProperty) {
        for (body in phaseBodies) {
            for (stmt in body.statements) {
                val call = stmt as? FirFunctionCall
                    ?: (stmt as? FirProperty)?.initializer as? FirFunctionCall
                    ?: continue
                val chain = unwindChain(call)
                if (chain.head?.callableSimpleName() == NAME_REWRITE &&
                    chain.outermost.callableSimpleName() != NAME_TO
                ) {
                    reportError(
                        stmt.source ?: declaration.source ?: return,
                        "`rewrite(...)` without a trailing `to { ... }` produces no " +
                            "transformation. Add `to { /* after */ }` or remove the call.",
                    )
                }
            }
        }
    }

    private fun FirFunctionCall.callableSimpleName(): Name? =
        calleeReference.toResolvedCallableSymbol()?.callableId?.callableName
}

/**
 * FIR additional-checkers extension that contributes the recipe DSL property
 * checker. Bound to a [FirSession] by the compiler plugin so it runs during
 * the CHECKERS phase on each user source file.
 */
internal class RecipeDslAdditionalCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val propertyCheckers: Set<FirPropertyChecker> = setOf(RecipeFirDslCheckers)
    }
}
