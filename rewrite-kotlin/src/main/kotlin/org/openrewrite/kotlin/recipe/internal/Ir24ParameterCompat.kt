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

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * Kotlin 2.4 unified `IrFunction.parameters` into a single list (dispatch receiver +
 * context + extension receiver + regular, distinguished by [IrParameterKind]), and
 * dropped the pre-2.4 `valueParameters` / `extensionReceiverParameter` /
 * `getValueArgument` / `putValueArgument` / `valueArgumentsCount` / `extensionReceiver`
 * accessors this generator was written against.
 *
 * Rather than hand-porting every call site to the unified model (and risking an
 * off-by-one against the now-shared dispatch/extension/regular index space), these
 * extensions reconstruct the old, narrower surface in terms of the new one. Since none
 * of these names exist as members anymore, Kotlin resolves the removed-API call sites
 * against these extensions unchanged — the porting risk is concentrated here instead of
 * spread across every use site.
 *
 * `valueParameters` matches Kotlin <2.4's own `IrFunction.getValueParameters()` bytecode
 * (verified by disassembling `kotlin-compiler-embeddable:2.3.20`): it includes both
 * [IrParameterKind.Regular] and [IrParameterKind.Context] parameters, not `Regular` alone —
 * context parameters were already folded into the pre-2.4 `valueParameters` view.
 */

private fun IrValueParameter.isValueParameterKind(): Boolean =
    kind == IrParameterKind.Regular || kind == IrParameterKind.Context

/** The callee's regular and context value parameters, in declaration order (pre-2.4 `valueParameters`). */
internal val IrFunction.valueParameters: List<IrValueParameter>
    get() = parameters.filter { it.isValueParameterKind() }

/** The callee's extension-receiver parameter, or `null` if it has none. */
internal val IrFunction.extensionReceiverParameter: IrValueParameter?
    get() = parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }

/** Number of value-argument slots at this call site. */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal val IrMemberAccessExpression<out IrFunctionSymbol>.valueArgumentsCount: Int
    get() = symbol.owner.parameters.count { it.isValueParameterKind() }

/** The value argument at [index] (matching [IrFunction.valueParameters] order). */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrMemberAccessExpression<out IrFunctionSymbol>.getValueArgument(index: Int): IrExpression? =
    arguments[symbol.owner.valueParameters[index]]

/** Sets the value argument at [index] (matching [IrFunction.valueParameters] order). */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal fun IrMemberAccessExpression<out IrFunctionSymbol>.putValueArgument(index: Int, valueArgument: IrExpression?) {
    arguments[symbol.owner.valueParameters[index]] = valueArgument
}

/** The extension-receiver argument at this call site, or `null` if the callee has none. */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal var IrMemberAccessExpression<out IrFunctionSymbol>.extensionReceiver: IrExpression?
    get() = symbol.owner.extensionReceiverParameter?.let { arguments[it] }
    set(value) {
        symbol.owner.extensionReceiverParameter?.let { arguments[it] = value }
    }
