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
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * FIR-level checker over property declarations that initialize a top-level recipe
 * via `org.openrewrite.recipe(name) { ... }`. Walks the trailing lambda body and
 * validates the recipe-DSL well-formedness rules at compile time.
 *
 * Currently implemented rules:
 *  - Pattern mode (`rewrite ... to ...`) and phase mode (`scan/edit/generate`) are
 *    mutually exclusive within one recipe block.
 *
 * Rules still to add (tracked in `lane-e-design-2026-05-14` project memory):
 *  - At most one `rewrite ... to ...` per pattern-mode recipe.
 *  - `scan` must precede `edit`/`generate` when phase mode is used.
 *
 * Why route through `FirErrors.OTHER_ERROR_WITH_REASON`: setting up a custom
 * `KtDiagnosticsContainer` in Kotlin 2.3 requires accessing
 * `KtDiagnosticFactoryToRendererMap`'s internal constructor. `OTHER_ERROR_WITH_REASON`
 * is a pre-registered string-arg diagnostic that renders cleanly — adequate while
 * the rule set is small.
 */
internal object RecipeDslPropertyChecker : FirPropertyChecker(MppCheckerKind.Common) {

    private val RECIPE_FQN = CallableId(FqName("org.openrewrite"), Name.identifier("recipe"))

    // Receiver-scope members of org.openrewrite.RecipeBuilder. We match by simple name
    // because all of these are unambiguous in the DSL block's receiver-scoped position.
    private val PATTERN_MODE_NAMES = setOf(Name.identifier("rewrite"), Name.identifier("to"))
    private val PHASE_MODE_NAMES = setOf(
        Name.identifier("scan"),
        Name.identifier("edit"),
        Name.identifier("generate"),
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val initializer = declaration.initializer as? FirFunctionCall ?: return
        if (!initializer.callsRecipeBuilder()) return

        val trailingLambda = initializer.findTrailingLambdaBody() ?: return

        var patternCall: FirFunctionCall? = null
        var phaseCall: FirFunctionCall? = null
        trailingLambda.accept(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                if (element is FirFunctionCall) {
                    val name = element.callableSimpleName()
                    if (name in PATTERN_MODE_NAMES && patternCall == null) patternCall = element
                    if (name in PHASE_MODE_NAMES && phaseCall == null) phaseCall = element
                }
                element.acceptChildren(this)
            }
        })

        if (patternCall != null && phaseCall != null) {
            val source = phaseCall!!.source ?: declaration.source ?: return
            if (source.kind is KtFakeSourceElementKind) return
            reporter.reportOn(
                source,
                FirErrors.OTHER_ERROR_WITH_REASON,
                "Recipe block mixes pattern mode (`rewrite ... to ...`) with phase " +
                    "mode (`scan`/`edit`/`generate`). Split into two recipes.",
            )
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
