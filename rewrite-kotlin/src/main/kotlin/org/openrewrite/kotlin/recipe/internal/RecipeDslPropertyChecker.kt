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
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.ConstantValueKind
import java.time.Duration
import java.time.format.DateTimeParseException

/**
 * FIR-level checker over property declarations whose initializer is a call to
 * `org.openrewrite.recipe(...)`. Walks the trailing lambda body and validates
 * recipe-DSL well-formedness rules at compile time.
 *
 * Rules currently enforced:
 *  - Pattern mode (`rewrite ... to ...`) and phase mode (`scan/edit/generate`)
 *    are mutually exclusive within one recipe block.
 *  - At most one `rewrite ... to ...` clause per pattern-mode recipe.
 *  - Within a phase-mode recipe, every `scan { … }` precedes every `edit { … }` /
 *    `generate { … }` call. `edit`-without-scan is allowed (the framework
 *    treats it as a stateless edit phase).
 *  - `estimatedEffortPerOccurrence`, when supplied as a string literal, must
 *    parse as an ISO-8601 duration. The IR pass silently falls back to the
 *    Recipe-default 5min on bad input — this checker promotes that to an
 *    error so the author finds out at compile time, not runtime.
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

    /**
     * Classification of a top-level statement inside the recipe block.
     */
    private sealed interface StatementKind {
        object Pattern : StatementKind         // `rewrite(...) to {...}`
        object Scan : StatementKind            // `scan(...) { ... }`
        object Edit : StatementKind            // `edit { ... }` or `edit(scanRef) { ... }`
        object Generate : StatementKind        // `generate(scanRef) { ... }`
        object Other : StatementKind
    }

    context(@Suppress("unused") context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val initializer = declaration.initializer as? FirFunctionCall ?: return
        if (!initializer.callsRecipeBuilder()) return

        validateEffortLiteral(initializer, declaration)

        val body = initializer.findTrailingLambdaBody() ?: return

        val classified: List<Pair<FirStatement, StatementKind>> = body.statements.map { it to classify(it) }

        val patterns = classified.filter { it.second is StatementKind.Pattern }
        val scans = classified.filter { it.second is StatementKind.Scan }
        val edits = classified.filter { it.second is StatementKind.Edit || it.second is StatementKind.Generate }

        val hasPattern = patterns.isNotEmpty()
        val hasPhase = scans.isNotEmpty() || edits.isNotEmpty()

        // Rule 1 — mode mixing.
        if (hasPattern && hasPhase) {
            val phaseAnchor = (scans + edits).first().first
            reportError(
                phaseAnchor.source ?: declaration.source ?: return,
                "Recipe block mixes pattern mode (`rewrite ... to ...`) with phase " +
                    "mode (`scan`/`edit`/`generate`). Split into two recipes.",
            )
            // Don't pile on further diagnostics once the block is structurally invalid.
            return
        }

        // Rule 2 — at most one `rewrite ... to ...` per pattern-mode recipe.
        if (patterns.size > 1) {
            val extra = patterns[1].first
            reportError(
                extra.source ?: declaration.source ?: return,
                "Recipe block declares more than one `rewrite ... to ...` clause. " +
                    "A recipe may carry exactly one pattern; split additional patterns " +
                    "into separate `recipe(...)` declarations.",
            )
        }

        // Rule 3 — within phase mode, every scan must precede every edit/generate.
        if (scans.isNotEmpty() && edits.isNotEmpty()) {
            val lastScanIdx = classified.indexOfLast { it.second is StatementKind.Scan }
            val firstEditIdx = classified.indexOfFirst {
                it.second is StatementKind.Edit || it.second is StatementKind.Generate
            }
            if (firstEditIdx < lastScanIdx) {
                val offending = classified[firstEditIdx].first
                reportError(
                    offending.source ?: declaration.source ?: return,
                    "Phase-mode recipe places `edit`/`generate` before a `scan` " +
                        "declared later in the block. Move all `scan { ... }` calls " +
                        "above any `edit` / `generate` to keep accumulator wiring " +
                        "lexical.",
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
     * Validates the `estimatedEffortPerOccurrence` arg's string-literal value
     * against ISO-8601 duration parsing. Only fires when the value is a string
     * literal — variable references, expression args, and the default empty
     * string are out of scope (the IR pass treats empty as "not specified").
     */
    context(@Suppress("unused") context: CheckerContext, reporter: DiagnosticReporter)
    private fun validateEffortLiteral(call: FirFunctionCall, declaration: FirProperty) {
        val literal = call.findStringLiteralArg("estimatedEffortPerOccurrence") ?: return
        val value = literal.value as? String ?: return
        if (value.isEmpty()) return
        try {
            Duration.parse(value)
        } catch (_: DateTimeParseException) {
            reportError(
                literal.source ?: declaration.source ?: return,
                "`estimatedEffortPerOccurrence` must be an ISO-8601 duration " +
                    "(e.g. \"PT5M\", \"PT30S\"); got \"$value\".",
            )
        }
    }

    /**
     * Looks up the named argument [paramName] on a resolved recipe call and
     * returns its FirLiteralExpression if the user supplied a string literal.
     * Returns null for variable references, function calls, missing args,
     * non-string literals, or any other expression shape — those aren't
     * statically checkable.
     *
     * Handles both pre-resolution shapes (FirNamedArgumentExpression wrappers
     * still present) and post-resolution shapes (mapping in
     * FirResolvedArgumentList, args reordered to parameter order).
     */
    private fun FirFunctionCall.findStringLiteralArg(paramName: String): FirLiteralExpression? {
        val resolved = argumentList as? FirResolvedArgumentList
        if (resolved != null) {
            for ((argExpr, param) in resolved.mapping) {
                if (param.name.asString() != paramName) continue
                return argExpr.unwrapNamed().asStringLiteralOrNull()
            }
            return null
        }
        // Fallback for the unresolved shape — find a wrapper by source-side name.
        for (arg in argumentList.arguments) {
            if (arg is FirNamedArgumentExpression && arg.name.asString() == paramName) {
                return arg.expression.asStringLiteralOrNull()
            }
        }
        return null
    }

    private fun FirExpression.unwrapNamed(): FirExpression =
        if (this is FirNamedArgumentExpression) expression else this

    private fun FirExpression.asStringLiteralOrNull(): FirLiteralExpression? {
        val lit = this as? FirLiteralExpression ?: return null
        return if (lit.kind == ConstantValueKind.String) lit else null
    }

    /**
     * Classify a top-level statement of the recipe block. Only the outermost call
     * shape matters — we don't recurse into argument lambdas so user code inside
     * a pattern lambda can use names like `scan` / `edit` freely.
     */
    private fun classify(statement: FirStatement): StatementKind {
        val call = statement as? FirFunctionCall ?: return StatementKind.Other
        return when (call.callableSimpleName()) {
            NAME_TO -> StatementKind.Pattern
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
