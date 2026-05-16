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
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
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
 * FIR-level checker over property declarations whose initializer is a call to
 * `org.openrewrite.recipe(...)`. Walks the trailing lambda body and validates
 * recipe-DSL well-formedness rules at compile time.
 *
 * Rules currently enforced:
 *  - The declarative `rewrite ... to ...` shape and the imperative
 *    `scan` / `edit` / `generate` blocks are mutually exclusive within one
 *    recipe block.
 *  - At most one `rewrite ... to ...` clause per recipe.
 *  - In a scan + edit / scan + generate recipe, every `scan { … }` precedes
 *    every `edit { … }` / `generate { … }` call. `edit`-without-scan is allowed
 *    (the recipe lowers to a plain Recipe with a stateless visitor).
 *
 * The checker walks only the top-level statements of the recipe block; it
 * intentionally does NOT recurse into nested lambdas, otherwise user code like
 * `rewrite { scan -> scan.size }` inside a pattern lambda would false-positive.
 *
 * Why route diagnostics through `FirErrors.OTHER_ERROR_WITH_REASON`: registering
 * a custom `KtDiagnosticsContainer` in Kotlin 2.3 requires accessing
 * `KtDiagnosticFactoryToRendererMap`'s internal constructor.
 * `OTHER_ERROR_WITH_REASON` is a pre-registered string-arg diagnostic that
 * renders cleanly — adequate while the rule set is small.
 */
internal object RecipeDslPropertyChecker : FirPropertyChecker(MppCheckerKind.Common) {

    private val RECIPE_FQN = CallableId(FqName("org.openrewrite"), Name.identifier("recipe"))

    // Names of the RecipeBuilder / RewriteAdvice members the checker recognises.
    // Matching is by simple name because all of these are unambiguous in the
    // DSL block's receiver-scoped position.
    private val NAME_TO = Name.identifier("to")
    private val NAME_SCAN = Name.identifier("scan")
    private val NAME_EDIT = Name.identifier("edit")
    private val NAME_GENERATE = Name.identifier("generate")
    private val NAME_REWRITE = Name.identifier("rewrite")

    /**
     * Classification of a top-level statement inside the recipe block.
     */
    private sealed interface StatementKind {
        object Pattern : StatementKind         // `rewrite(...) to {...}`
        object OrphanRewrite : StatementKind   // `rewrite(...)` with no trailing `to {...}` — silently no-ops today; we error.
        object Scan : StatementKind            // `scan(...) { ... }`
        object Edit : StatementKind            // `edit { ... }` or `edit(scanRef) { ... }`
        object Generate : StatementKind        // `generate(scanRef) { ... }`
        object Other : StatementKind
    }

    context(@Suppress("unused") context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val initializer = declaration.initializer as? FirFunctionCall ?: return
        if (!initializer.callsRecipeBuilder()) return

        val body = initializer.findTrailingLambdaBody() ?: return

        val classified: List<Pair<FirStatement, StatementKind>> = body.statements.map { it to classify(it) }

        val patterns = classified.filter { it.second is StatementKind.Pattern }
        val orphanRewrites = classified.filter { it.second is StatementKind.OrphanRewrite }
        val scans = classified.filter { it.second is StatementKind.Scan }
        val edits = classified.filter { it.second is StatementKind.Edit || it.second is StatementKind.Generate }

        // Rule 0 — orphan `rewrite(...)` with no `to`. The DSL surface makes
        // `to` a separate infix call, so a recipe body of just
        // `rewrite { ... }` type-checks (the unused `RewriteAdvice` value is
        // discarded) and silently does nothing at runtime. Catch it here
        // before other rules so the author fixes the orphan first.
        for ((orphan, _) in orphanRewrites) {
            reportError(
                orphan.source ?: declaration.source ?: return,
                "`rewrite(...)` without a trailing `to { ... }` produces no " +
                    "transformation. Add `to { /* after */ }` or remove the call.",
            )
        }
        if (orphanRewrites.isNotEmpty()) return

        val hasPattern = patterns.isNotEmpty()
        val hasImperative = scans.isNotEmpty() || edits.isNotEmpty()

        // Rule 1 — mixing the declarative pattern shape with the imperative blocks.
        if (hasPattern && hasImperative) {
            val imperativeAnchor = (scans + edits).first().first
            reportError(
                imperativeAnchor.source ?: declaration.source ?: return,
                "Recipe block mixes the `rewrite ... to ...` pattern shape with " +
                    "imperative `scan` / `edit` / `generate` blocks. Split into two recipes.",
            )
            // Don't pile on further diagnostics once the block is structurally invalid.
            return
        }

        // Rule 2 — at most one `rewrite ... to ...` per pattern recipe.
        if (patterns.size > 1) {
            val extra = patterns[1].first
            reportError(
                extra.source ?: declaration.source ?: return,
                "Recipe block declares more than one `rewrite ... to ...` clause. " +
                    "A recipe may carry exactly one pattern; split additional patterns " +
                    "into separate `recipe(...)` declarations.",
            )
        }

        // Rule 3 — every `scan` must precede every `edit` / `generate`.
        if (scans.isNotEmpty() && edits.isNotEmpty()) {
            val lastScanIdx = classified.indexOfLast { it.second is StatementKind.Scan }
            val firstEditIdx = classified.indexOfFirst {
                it.second is StatementKind.Edit || it.second is StatementKind.Generate
            }
            if (firstEditIdx < lastScanIdx) {
                val offending = classified[firstEditIdx].first
                reportError(
                    offending.source ?: declaration.source ?: return,
                    "Recipe places `edit` / `generate` before a `scan` declared " +
                        "later in the block. Move all `scan { ... }` calls above any " +
                        "`edit` / `generate` to keep accumulator wiring lexical.",
                )
            }
        }
    }

    context(@Suppress("unused") context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportError(source: KtSourceElement, message: String) {
        if (source.kind is KtFakeSourceElementKind) return
        reporter.reportOn(source, FirErrors.OTHER_ERROR_WITH_REASON, message)
    }

    /**
     * Classify a top-level statement of the recipe block. Only the outermost call
     * shape matters — we don't recurse into argument lambdas so user code inside
     * a pattern lambda can use names like `scan` / `edit` freely.
     */
    private fun classify(statement: FirStatement): StatementKind {
        // Recipe-body statements are either bare DSL calls (`edit { ... }`)
        // or property declarations binding a scan handle
        // (`val seen = scan(...) { ... }`). Both shapes need classification —
        // missing the FirProperty case let `val seen = scan(...)` register
        // as Other, which let the mode-mixing rules skip seeing the scan
        // and was a latent footgun.
        val call = statement as? FirFunctionCall
            ?: (statement as? FirProperty)?.initializer as? FirFunctionCall
            ?: return StatementKind.Other
        return when (call.callableSimpleName()) {
            NAME_TO -> StatementKind.Pattern
            NAME_REWRITE -> StatementKind.OrphanRewrite
            NAME_SCAN -> StatementKind.Scan
            NAME_EDIT -> StatementKind.Edit
            NAME_GENERATE -> StatementKind.Generate
            else -> StatementKind.Other
        }
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

    private fun FirFunctionCall.callableSimpleName(): Name? =
        calleeReference.toResolvedCallableSymbol()?.callableId?.callableName
}
