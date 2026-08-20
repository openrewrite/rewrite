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

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.junit.jupiter.api.Test

/**
 * Direct unit tests for the IR-parameter compat shim, built against real (if minimal)
 * `IrFunction`/`IrValueParameter` instances constructed via [IrFactoryImpl] rather than
 * a full compilation -- no compiler frontend or module context is needed since only
 * [IrValueParameter.getKind] is ever inspected.
 *
 * These pin the bug this file was written to fix: `valueParameters` originally filtered
 * to [IrParameterKind.Regular] only, silently dropping [IrParameterKind.Context]
 * parameters that Kotlin's own pre-2.4 `IrFunction.getValueParameters()` included
 * (verified by disassembling `kotlin-compiler-embeddable:2.3.20`).
 */
class Ir24ParameterCompatTest {

    private val dummyType: IrType = IrDynamicTypeImpl(emptyList(), Variance.INVARIANT)

    private fun param(name: String, kind: IrParameterKind): IrValueParameter =
        IrFactoryImpl.createValueParameter(
            startOffset = -1,
            endOffset = -1,
            origin = IrDeclarationOrigin.DEFINED,
            kind = kind,
            name = Name.identifier(name),
            type = dummyType,
            isAssignable = false,
            symbol = IrValueParameterSymbolImpl(),
            varargElementType = null,
            isCrossinline = false,
            isNoinline = false,
            isHidden = false,
        )

    @Test
    fun `valueParameters includes both Regular and Context parameters`() {
        val dispatchReceiver = param("this", IrParameterKind.DispatchReceiver)
        val context = param("ctx", IrParameterKind.Context)
        val extensionReceiver = param("recv", IrParameterKind.ExtensionReceiver)
        val regular = param("arg", IrParameterKind.Regular)

        val function = IrFactoryImpl.createSimpleFunction(
            startOffset = -1,
            endOffset = -1,
            origin = IrDeclarationOrigin.DEFINED,
            name = Name.identifier("test"),
            visibility = DescriptorVisibilities.PUBLIC,
            isInline = false,
            isExpect = false,
            returnType = dummyType,
            modality = Modality.FINAL,
            symbol = IrSimpleFunctionSymbolImpl(),
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
            isExternal = false,
        ).apply {
            parameters = listOf(dispatchReceiver, context, extensionReceiver, regular)
        }

        assertThat(function.valueParameters).containsExactly(context, regular)
    }

    @Test
    fun `valueParameters on a function with no context parameter is unaffected`() {
        val dispatchReceiver = param("this", IrParameterKind.DispatchReceiver)
        val a = param("a", IrParameterKind.Regular)
        val b = param("b", IrParameterKind.Regular)

        val function = IrFactoryImpl.createSimpleFunction(
            startOffset = -1,
            endOffset = -1,
            origin = IrDeclarationOrigin.DEFINED,
            name = Name.identifier("test"),
            visibility = DescriptorVisibilities.PUBLIC,
            isInline = false,
            isExpect = false,
            returnType = dummyType,
            modality = Modality.FINAL,
            symbol = IrSimpleFunctionSymbolImpl(),
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
            isExternal = false,
        ).apply {
            parameters = listOf(dispatchReceiver, a, b)
        }

        assertThat(function.valueParameters).containsExactly(a, b)
    }

    @Test
    fun `extensionReceiverParameter finds only the ExtensionReceiver-kind parameter`() {
        val dispatchReceiver = param("this", IrParameterKind.DispatchReceiver)
        val extensionReceiver = param("recv", IrParameterKind.ExtensionReceiver)
        val regular = param("arg", IrParameterKind.Regular)

        val function = IrFactoryImpl.createSimpleFunction(
            startOffset = -1,
            endOffset = -1,
            origin = IrDeclarationOrigin.DEFINED,
            name = Name.identifier("test"),
            visibility = DescriptorVisibilities.PUBLIC,
            isInline = false,
            isExpect = false,
            returnType = dummyType,
            modality = Modality.FINAL,
            symbol = IrSimpleFunctionSymbolImpl(),
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
            isExternal = false,
        ).apply {
            parameters = listOf(dispatchReceiver, extensionReceiver, regular)
        }

        assertThat(function.extensionReceiverParameter).isEqualTo(extensionReceiver)
    }

    @Test
    fun `extensionReceiverParameter is null when the function has none`() {
        val regular = param("arg", IrParameterKind.Regular)

        val function = IrFactoryImpl.createSimpleFunction(
            startOffset = -1,
            endOffset = -1,
            origin = IrDeclarationOrigin.DEFINED,
            name = Name.identifier("test"),
            visibility = DescriptorVisibilities.PUBLIC,
            isInline = false,
            isExpect = false,
            returnType = dummyType,
            modality = Modality.FINAL,
            symbol = IrSimpleFunctionSymbolImpl(),
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
            isExternal = false,
        ).apply {
            parameters = listOf(regular)
        }

        assertThat(function.extensionReceiverParameter).isNull()
    }
}
